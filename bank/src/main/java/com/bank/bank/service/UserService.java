package com.bank.bank.service;
import com.bank.bank.model.User;
import com.bank.bank.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class UserService {

    private final UserRepository userRepo;

    public UserService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    /** Zwraca aktualne saldo użytkownika. */
    public BigDecimal getBalance(String email) {
        User u = userRepo.findById(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return u.getBalance();
    }

    /** Dokonuje wpłaty i zwraca nowe saldo. */
    @Transactional
    public BigDecimal deposit(String email, BigDecimal amount) {
        User u = userRepo.findById(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        u.setBalance(u.getBalance().add(amount));
        return u.getBalance();
    }
}


