package com.bank.bank.repository;

import com.bank.bank.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByCardNumber(String cardNumber);
}

