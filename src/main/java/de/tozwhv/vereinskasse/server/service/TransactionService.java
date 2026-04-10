package de.tozwhv.vereinskasse.server.service;

import de.tozwhv.vereinskasse.server.dto.TransactionDTO;
import de.tozwhv.vereinskasse.server.modell.Deposit;
import de.tozwhv.vereinskasse.server.modell.Sale;
import de.tozwhv.vereinskasse.server.modell.User;
import de.tozwhv.vereinskasse.server.repository.DepositRepository;
import de.tozwhv.vereinskasse.server.repository.SaleRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class TransactionService {
    private final SaleRepository saleRepository;
    private final DepositRepository depositRepository;


    public TransactionService(SaleRepository saleRepository, DepositRepository depositRepository) {
        this.saleRepository = saleRepository;
        this.depositRepository = depositRepository;
    }


    public List<TransactionDTO> getUserHistory(User user, LocalDateTime start, LocalDateTime end) {
        // Null-Safe Handling
        LocalDateTime actualStart = (start == null) ? LocalDateTime.now().minusYears(10) : start;
        LocalDateTime actualEnd = (end == null) ? LocalDateTime.now() : end;

        // 1. Daten laden
        List<Sale> sales = saleRepository.findByUserAndCreatedAtBetweenOrderByCreatedAtDesc(user, actualStart, actualEnd);
        List<Deposit> deposits = depositRepository.findByUserAndCreatedAtBetweenOrderByCreatedAtDesc(user, actualStart, actualEnd);

        List<TransactionDTO> history = new ArrayList<>();

        // 2. Sales konvertieren (Multipliziere Menge * Preis für totalPrice)
        sales.forEach(s -> history.add(new TransactionDTO(
                s.getId(),
                "SALE",
                s.getProduct().getName(),
                s.getAmount(),
                s.getPrice(), // Gesammtsumme der Transaktion liegt bereits vor
                s.getCreatedAt()
        )));

        // 3. Deposits konvertieren
        deposits.forEach(d -> history.add(new TransactionDTO(
                d.getId(),
                "DEPOSIT",
                "Guthaben aufgeladen",
                1, // Menge bei Einzahlung ist immer 1
                d.getAmount(),
                d.getCreatedAt()
        )));

        // 4. Sortieren: Neueste zuerst
        history.sort((a, b) -> b.date().compareTo(a.date()));

        return history;
    }
}