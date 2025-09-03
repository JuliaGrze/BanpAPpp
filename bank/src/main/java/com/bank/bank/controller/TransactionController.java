package com.bank.bank.controller;

import com.bank.bank.dto.InitTransactionRequest;
import com.bank.bank.dto.InitTransactionResponse;
import com.bank.bank.dto.TransactionDTO;
import com.bank.bank.mapper.TransactionMapper;
import com.bank.bank.model.Transaction;
import com.bank.bank.model.TransactionStatus;
import com.bank.bank.model.User;
import com.bank.bank.repository.TransactionRepository;
import com.bank.bank.repository.UserRepository;
import com.bank.bank.service.TransactionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "http://localhost:4201")
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final TransactionService transactionService;

    public TransactionController(TransactionRepository transactionRepository,
                                 UserRepository userRepository,
                                 TransactionService transactionService) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.transactionService = transactionService;
    }

    // Pobieranie wszystkich transakcji
    @GetMapping("/all/{email}")
    public List<TransactionDTO> getAllTransactions(@PathVariable String email) {
        // Możesz dodać logikę sprawdzającą token i uprawnienia
        User user = userRepository.findById(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return transactionRepository.findByUser(user)
                .stream()
                .map(TransactionMapper::toDTO)
                .collect(Collectors.toList());
    }

    // Pobieranie potwierdzonych transakcji
    @GetMapping("/confirmed/{email}")
    public List<TransactionDTO> getConfirmedTransactions(@PathVariable String email) {
        User user = userRepository.findById(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return transactionRepository.findByUserAndStatus(user, TransactionStatus.CONFIRMED)
                .stream()
                .map(TransactionMapper::toDTO)
                .collect(Collectors.toList());
    }

    // Przetwarzanie transakcji (np. płatność)
    @PostMapping("/process")
    public String processTransaction(@RequestBody String encryptedData) {
        try {
            return transactionService.processEncryptedTransaction(encryptedData);
        } catch (Exception e) {
            return "Error processing transaction: " + e.getMessage();
        }
    }

    // Pobieranie oczekujących transakcji
    @GetMapping("/pending/{email}")
    public List<TransactionDTO> getPendingTransactions(@PathVariable String email) {
        User user = userRepository.findById(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return transactionRepository.findByUserAndStatus(user, TransactionStatus.PENDING)
                .stream()
                .map(TransactionMapper::toDTO)
                .collect(Collectors.toList());
    }

    // Potwierdzanie transakcji
    @PostMapping("/confirm/{id}")
    public String confirmTransaction(@PathVariable Long id) {
        // Możesz tu dodać sprawdzenie uprawnień użytkownika
        transactionService.confirmTransaction(id);
        return "Transaction confirmed.";
    }

    // Odrzucanie transakcji
    @PostMapping("/reject/{id}")
    public String rejectTransaction(@PathVariable Long id) {
        // Możesz tu dodać sprawdzenie uprawnień użytkownika
        transactionService.rejectTransaction(id);
        return "Transaction rejected.";
    }

    // w com.bank.bank.controller.TransactionController
    @PostMapping("/init")
    public InitTransactionResponse init(@RequestBody InitTransactionRequest req) {
        Transaction tx = new Transaction();
        tx.setOrderId(req.orderId());   // ID zamówienia ze sklepu
        tx.setAmount(req.amount());             // kwota (BigDecimal)
        tx.setCurrency(req.currency());         // np. PLN
        tx.setStatus(TransactionStatus.PENDING); // zawsze startuje jako PENDING
        tx.setDescription(req.description());   // opis np. "Order ORD-..."
        tx.setCallbackUrl(req.callbackUrl());   // gdzie bank ma wysłać wynik
        tx.setCreatedAt(java.time.LocalDateTime.now()); // timestamp

        // >>> PRZYPISANIE UŻYTKOWNIKA <<<
        if (req.customerEmail() != null && !req.customerEmail().isBlank()) {
            User user = userRepository.findById(req.customerEmail())
                    .orElseThrow(() -> new RuntimeException("User not found: " + req.customerEmail()));
            tx.setUser(user);
        }

        tx = transactionRepository.save(tx);


        // zwracamy np. "tx_123" – sklep to zapisze do payment.bank_transaction_id
        return new InitTransactionResponse("tx_" + tx.getId(), tx.getStatus().name(), tx.getOrderId(), tx.getAmount());

    }




}
