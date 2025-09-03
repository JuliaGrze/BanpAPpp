package com.bank.bank;

import com.bank.bank.model.User;
import com.bank.bank.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

@SpringBootApplication
public class BankApplication {

	public static void main(String[] args) {
		SpringApplication.run(BankApplication.class, args);
	}

	@Bean
	CommandLineRunner initData(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		return args -> {
			User anna = new User("anna@example.com", passwordEncoder.encode("123"), "Anna Kowalska", "1234567890123456", "123", "12/26", BigDecimal.valueOf(500));
			userRepository.save(anna);

			User jan = new User("jan@example.com", passwordEncoder.encode("2004"), "Jan Nowak", "9876543210987654", "456", "11/25", BigDecimal.valueOf(50));
			userRepository.save(jan);
		};
	}

}
