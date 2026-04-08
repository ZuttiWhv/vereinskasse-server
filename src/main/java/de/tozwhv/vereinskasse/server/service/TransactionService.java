package de.tozwhv.vereinskasse.server.service;

import de.tozwhv.vereinskasse.server.dto.TransactionDTO;
import de.tozwhv.vereinskasse.server.modell.Deposit;
import de.tozwhv.vereinskasse.server.modell.Sale;
import de.tozwhv.vereinskasse.server.modell.User;
import de.tozwhv.vereinskasse.server.repository.DepositRepository;
import de.tozwhv.vereinskasse.server.repository.SaleRepository;
import de.tozwhv.vereinskasse.server.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class TransactionService {
    private final SaleRepository saleRepository;
    private final DepositRepository depositRepository;
    private final UserRepository userRepository;

    public TransactionService(SaleRepository saleRepository, DepositRepository depositRepository, UserRepository userRepository) {
        this.saleRepository = saleRepository;
        this.depositRepository = depositRepository;
        this.userRepository = userRepository;
    }


    public List<TransactionDTO> getUserHistory(Long userId,LocalDateTime start, LocalDateTime end) {

        if (start == null) start = LocalDateTime.now().minusYears(100);
        if (end == null) end = LocalDateTime.now();
        User user = userRepository.getReferenceById(userId);

    List<Sale> sales = saleRepository.findByUserAndCreatedAtBetweenOrderByCreatedAtDesc(user,start, end);
    // 2. Deposits holen
    List<Deposit> deposits = depositRepository.findByUserAndCreatedAtBetweenOrderByCreatedAtDesc(user,start,end);

    List<TransactionDTO> history = new ArrayList<>();

    // Sales konvertieren
    sales.forEach(s -> history.add(new TransactionDTO(
            s.getId(), "SALE", s.getProduct().getName(), s.getPrice(), s.getCreatedAt()
    )));

    // Deposits konvertieren
    deposits.forEach(d -> history.add(new TransactionDTO(
            d.getId(), "DEPOSIT", "Guthaben aufgeladen", d.getAmount(), d.getCreatedAt()
    )));

    // Nach Datum sortieren (Neueste zuerst)
    history.sort((a, b) -> b.date().compareTo(a.date()));
    return history;
}}