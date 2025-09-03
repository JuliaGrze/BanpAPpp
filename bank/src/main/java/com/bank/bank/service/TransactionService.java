package com.bank.bank.service;

import com.bank.bank.dto.WebhookDTO;
import com.bank.bank.model.Transaction;
import com.bank.bank.model.TransactionStatus;
import com.bank.bank.model.User;
import com.bank.bank.repository.TransactionRepository;
import com.bank.bank.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
            payload.setAmount(tx.getAmount());
            payload.setOrderId(String.valueOf(tx.getOrderId()));

//            payload.setTimestamp(tx.getCreatedAt());
            logger.info("Posting webhook to {}: {}", webhookUrl, payload);
            var resp = restTemplate.postForEntity(webhookUrl, payload, Void.class);
            logger.info("Webhook response: {}", resp.getStatusCode());
        } catch (Exception ex) {
            logger.error("Webhook failed for tx {}: {}", tx.getId(), ex.getMessage(), ex);
        }
    }

}
