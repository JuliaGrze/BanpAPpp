package com.bank.bank.repository;

import com.bank.bank.model.Transaction;
import com.bank.bank.model.TransactionStatus;
import com.bank.bank.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByUserEmailAndStatus(String userEmail, TransactionStatus status);
    List<Transaction> findByUserAndStatus(User user, TransactionStatus status);
    List<Transaction> findByUser(User user);

}
