package com.bank.bank.service;

import com.bank.bank.dto.InitTransactionRequest;
import com.bank.bank.dto.InitTransactionResponse;
import com.bank.bank.dto.WebhookDTO;
import com.bank.bank.model.Transaction;
import com.bank.bank.model.TransactionStatus;
import com.bank.bank.model.User;
import com.bank.bank.repository.TransactionRepository;
import com.bank.bank.repository.UserRepository;
import com.bank.bank.security.ShopEncryptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Base64;
import java.math.BigDecimal;
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final RSAService rsaService;
    private final RestTemplate restTemplate;
    private final Logger logger = LoggerFactory.getLogger(TransactionService.class);
    private final ObjectMapper om = new ObjectMapper();
    private final ShopEncryptor shopEncryptor;

    @Value("${external.shop.webhook-url}")
    private String webhookUrl;

    @Transactional
    public String processEncryptedTransaction(String encryptedBase64) {
        Transaction tx = new Transaction();
        try {
            // 1) odszyfruj
            byte[] decoded = Base64.getDecoder().decode(encryptedBase64);
            String decrypted = rsaService.decrypt(decoded);
            String[] parts = decrypted.split(";");
            BigDecimal amount = new BigDecimal(parts[0]);
            String cardNumber = parts[1], expiry = parts[2], csv = parts[3];
            String orderId = parts.length > 4 ? parts[4] : null;
            tx.setOrderId(orderId);

            // 2) znajdź użytkownika
            User user = userRepository.findByCardNumber(cardNumber)
                    .orElseThrow(() -> new IllegalArgumentException("INVALID_DATA"));

            // 3) walidacja kart
            if (!user.getExpiryDate().equals(expiry) || !user.getCsv().equals(csv)) {
                tx.setStatus(TransactionStatus.REJECTED);
                tx.setUser(user);
                tx.setAmount(amount);
                tx.setCreatedAt(LocalDateTime.now());
                transactionRepository.save(tx);
                sendWebhook(tx, "INVALID_DATA");
                return "Invalid card details.";
            }

            // 4) brak środków
            if (user.getBalance().compareTo(amount) < 0)  {
                tx.setStatus(TransactionStatus.REJECTED);
                tx.setUser(user);
                tx.setAmount(amount);
                tx.setCreatedAt(LocalDateTime.now());
                transactionRepository.save(tx);
                sendWebhook(tx, "REJECTED");
                return "Transaction rejected: insufficient funds.";
            }

            // 5) dalej normalnie: albo auto-confirm, albo pending
            tx.setUser(user);
            tx.setAmount(amount);
            tx.setCreatedAt(LocalDateTime.now());
            if (amount.compareTo(BigDecimal.valueOf(100)) <= 0) {
                tx.setStatus(TransactionStatus.CONFIRMED);
                user.setBalance(user.getBalance().subtract(amount));
                userRepository.save(user);
                transactionRepository.save(tx);
                sendWebhook(tx, "CONFIRMED");
                return "Transaction auto-confirmed.";
            } else {
                tx.setStatus(TransactionStatus.PENDING);
                transactionRepository.save(tx);
                sendWebhook(tx, "PENDING");
                return "Transaction pending user confirmation.";
            }

        } catch (IllegalArgumentException ex) {
            // nieprawidłowe dane już obsłużone wyżej
            return ex.getMessage();
        } catch (Exception ex) {
            logger.error("Unexpected error in processEncryptedTransaction", ex);
            throw new RuntimeException("Internal error");
        }
    }

    @Transactional
    public String confirmTransaction(Long transactionId) {
        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found."));
        User user = tx.getUser();

        if (tx.getStatus() != TransactionStatus.PENDING) {
            return "Transaction is not pending.";
        }

        if (user.getBalance().compareTo(tx.getAmount()) < 0) {
            tx.setStatus(TransactionStatus.REJECTED);
            transactionRepository.save(tx);
            sendWebhook(tx, "REJECTED");
            return "Transaction rejected: insufficient funds at confirmation.";
        }

        user.setBalance(user.getBalance().subtract(tx.getAmount()));
        tx.setStatus(TransactionStatus.CONFIRMED);
        userRepository.save(user);
        transactionRepository.save(tx);
        sendWebhook(tx, "CONFIRMED");
        return "Transaction confirmed successfully.";
    }

    @Transactional
    public String rejectTransaction(Long transactionId) {
        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found."));
        tx.setStatus(TransactionStatus.REJECTED);
        transactionRepository.save(tx);
        sendWebhook(tx, "REJECTED");
        return "Transaction rejected.";
    }

    private void sendWebhook(Transaction tx, String resultStatus) {
        try {
            WebhookDTO payload = new WebhookDTO();
            payload.setTransactionId(tx.getId());
            payload.setStatus(resultStatus);
            payload.setEmail(tx.getUser().getEmail());
            payload.setAmount(tx.getAmount());           // BigDecimal
            payload.setOrderId(tx.getOrderId());         // nie rób String.valueOf(null)

            // 1) JSON -> String
            String json = om.writeValueAsString(payload);

            // 2) RSA (publiczny klucz SKLEPU) -> base64
            String enc = shopEncryptor.encryptToBase64(json);

            // 3) Wyślij jako text/plain
            HttpHeaders h = new HttpHeaders();
            h.setContentType(MediaType.TEXT_PLAIN);
            HttpEntity<String> req = new HttpEntity<>(enc, h);

            logger.info("Posting webhook to {} (status={}, txId={}, orderId={}, bodyLen={})",
                    webhookUrl, resultStatus, tx.getId(), tx.getOrderId(), enc.length());

            // ⬇️⬇️ KLUCZOWA ZMIANA: wysyłamy 'req', NIE 'payload'
            var resp = restTemplate.postForEntity(webhookUrl, req, Void.class);
            logger.info("Webhook response: {}", resp.getStatusCode());
        } catch (Exception ex) {
            logger.error("Webhook failed for tx {}: {}", tx.getId(), ex.getMessage(), ex);
        }
    }

    @Transactional
    public InitTransactionResponse initTransaction(InitTransactionRequest req) {
        long t0 = System.currentTimeMillis();
        logger.info("[INIT] start orderId={}, amount={}, curr={}, email={}",
                req.orderId(), req.amount(), req.currency(), req.customerEmail());

        try {
            // 1) Znajdź usera (jeśli podano email)
            User user = null;
            if (req.customerEmail() != null && !req.customerEmail().isBlank()) {
                user = userRepository.findById(req.customerEmail())
                        .orElseThrow(() -> new RuntimeException("User not found: " + req.customerEmail()));
                logger.debug("[INIT] user found email={}, balance={}", user.getEmail(), user.getBalance());
            } else {
                logger.debug("[INIT] no customerEmail provided – proceeding without user binding");
            }

            // 2) Przygotuj transakcję (status zdecydujemy przed zapisem)
            Transaction tx = new Transaction();
            tx.setOrderId(req.orderId());
            tx.setAmount(req.amount());
            tx.setCurrency(req.currency());
            tx.setDescription(req.description());
            tx.setCallbackUrl(req.callbackUrl());
            tx.setCreatedAt(java.time.LocalDateTime.now());
            if (user != null) tx.setUser(user);

            // 3) Brak środków -> REJECTED
            if (user != null && user.getBalance().compareTo(req.amount()) < 0) {
                logger.info("[INIT] REJECTED insufficient funds: balance={}, amount={}",
                        user.getBalance(), req.amount());
                tx.setStatus(TransactionStatus.REJECTED);
                tx = transactionRepository.save(tx);
                logger.debug("[INIT] saved REJECTED txId={}", tx.getId());
                sendWebhook(tx, "REJECTED");
                logger.debug("[INIT] webhook sent for txId={} status=REJECTED", tx.getId());

                logger.info("[INIT] done txId={} status=REJECTED took={}ms",
                        tx.getId(), (System.currentTimeMillis() - t0));
                return new InitTransactionResponse("tx_" + tx.getId(), tx.getStatus().name(), tx.getOrderId(), tx.getAmount());
            }

            // 4) Auto-CONFIRMED dla kwot < 100 (jeśli znamy usera)
            if (user != null && req.amount().compareTo(BigDecimal.valueOf(100)) < 0) {
                BigDecimal old = user.getBalance();
                BigDecimal newBal = old.subtract(req.amount());
                logger.info("[INIT] AUTO-CONFIRMED amount<100: oldBalance={} -> newBalance={}", old, newBal);

                tx.setStatus(TransactionStatus.CONFIRMED);
                user.setBalance(newBal);
                userRepository.save(user);
                tx = transactionRepository.save(tx);
                logger.debug("[INIT] saved CONFIRMED txId={}", tx.getId());
                sendWebhook(tx, "CONFIRMED");
                logger.debug("[INIT] webhook sent for txId={} status=CONFIRMED", tx.getId());

                logger.info("[INIT] done txId={} status=CONFIRMED took={}ms",
                        tx.getId(), (System.currentTimeMillis() - t0));
                return new InitTransactionResponse("tx_" + tx.getId(), tx.getStatus().name(), tx.getOrderId(), tx.getAmount());
            }

            // 5) Pozostałe przypadki -> PENDING
            tx.setStatus(TransactionStatus.PENDING);
            tx = transactionRepository.save(tx);
            logger.info("[INIT] PENDING created txId={} (amount>=100 or user unknown)", tx.getId());
            // opcjonalnie: sendWebhook(tx, "PENDING");

            logger.info("[INIT] done txId={} status=PENDING took={}ms",
                    tx.getId(), (System.currentTimeMillis() - t0));
            return new InitTransactionResponse("tx_" + tx.getId(), tx.getStatus().name(), tx.getOrderId(), tx.getAmount());

        } catch (Exception ex) {
            logger.error("[INIT] unexpected error orderId={} amount={} msg={}",
                    req.orderId(), req.amount(), ex.getMessage(), ex);
            throw ex; // pozwól wyżej zadecydować, ale mamy pełny log
        }
    }






}
