package com.bank.bank.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class User {
    @Id
    private String email;  // e-mail jako klucz główny
    private String password;
    private String name;

    private String cardNumber;

    private String csv;

    private String expiryDate;
    private BigDecimal balance;


}

