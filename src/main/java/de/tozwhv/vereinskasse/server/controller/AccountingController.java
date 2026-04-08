package de.tozwhv.vereinskasse.server.controller;

import de.tozwhv.vereinskasse.server.dto.TransactionDTO;
import de.tozwhv.vereinskasse.server.modell.User;
import de.tozwhv.vereinskasse.server.repository.ProductRepository;
import de.tozwhv.vereinskasse.server.repository.SaleRepository;
import de.tozwhv.vereinskasse.server.repository.UserRepository;
import de.tozwhv.vereinskasse.server.service.SaleService;
import de.tozwhv.vereinskasse.server.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/accounting")

public class AccountingController {

        private final TransactionService transactionService;
        private final UserRepository userRepository;


        @Autowired
        public AccountingController(SaleRepository saleRepository, ProductRepository productRepository, UserRepository userRepository, SaleService saleService, TransactionService transactionService, UserRepository userRepository1) {
            this.transactionService = transactionService;
            this.userRepository = userRepository1;
        }



        @GetMapping
        @PreAuthorize("hasAuthority('READ_OWN_SALES')")
        public List<TransactionDTO> getUserTransactions(
                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
                Authentication authentication)
        {
            // 1. Den User anhand des Namens aus dem Security-Kontext laden
            User user = userRepository.findByUsername(authentication.getName())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User nicht gefunden"));
            return transactionService.getUserHistory(user, start, end);
        }
}
