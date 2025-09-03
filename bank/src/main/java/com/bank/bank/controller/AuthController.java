package com.bank.bank.controller;
import com.bank.bank.dto.AuthRequestDTO;
import com.bank.bank.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody AuthRequestDTO authRequestDTO) {
        String jwtToken = authService.login(authRequestDTO.getEmail(), authRequestDTO.getPassword());
        return ResponseEntity.ok(jwtToken);
    }
}

