package com.bank.bank.controller;

import com.bank.bank.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/users")
@CrossOrigin("http://localhost:4201")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Pobranie salda
    @GetMapping("/balance")
    public BigDecimal getBalance(Authentication auth) {
        String email = auth.getName();
        return userService.getBalance(email);
    }

    // WPŁACENIE środków
    @PostMapping("/deposit")
    public BigDecimal deposit(
            @RequestParam BigDecimal amount,
            Authentication auth
    ) {
        String email = auth.getName();
        return userService.deposit(email, amount);
    }
}
