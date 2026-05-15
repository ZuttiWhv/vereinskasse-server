package de.tozwhv.vereinskasse.server.service;

import de.tozwhv.vereinskasse.server.dto.TransactionDTO;
import de.tozwhv.vereinskasse.server.dto.UserBalanceDTO;
import de.tozwhv.vereinskasse.server.modell.Deposit;
import de.tozwhv.vereinskasse.server.modell.Sale;
import de.tozwhv.vereinskasse.server.modell.User;
import de.tozwhv.vereinskasse.server.repository.DepositRepository;
import de.tozwhv.vereinskasse.server.repository.SaleRepository;
import de.tozwhv.vereinskasse.server.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
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


    public List<TransactionDTO> getUserHistory(User user, LocalDateTime start, LocalDateTime end) {
        // Null-Safe Handling
        LocalDateTime actualStart = (start == null) ? LocalDateTime.now().minusYears(10) : start;
        LocalDateTime actualEnd = (end == null) ? LocalDateTime.now() : end;

        // 1. Daten laden
        List<Sale> sales = saleRepository.findByUserAndCreatedAtBetweenOrderByCreatedAtDesc(user, actualStart, actualEnd);
        List<Deposit> deposits = depositRepository.findByUserAndCreatedAtBetweenOrderByCreatedAtDesc(user, actualStart, actualEnd);

        List<TransactionDTO> history = new ArrayList<>();

        // 2. Sales konvertieren (Multipliziere Menge * Preis für totalPrice)


        sales.forEach(s -> {
            String type = "SALE";
            if (s.isUseVoucher()) type="REDEEM";
            if (s.isVoucher()) type="VOUCHER";
            history.add(new TransactionDTO(
                    s.getId(),
                    type,
                    s.getProduct().getName(),
                    s.getAmount(),
                    s.getPrice(), // Gesammtsumme der Transaktion liegt bereits vor
                    s.getCreatedAt()
            ));
        });

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

    public List<UserBalanceDTO> getALLUsersBalanceAtDate(LocalDateTime start) {
        LocalDateTime actualStart = (start == null) ? LocalDateTime.now().minusYears(10) : start;
        LocalDateTime actualEnd = LocalDateTime.now();

        List<UserBalanceDTO> returnList = new ArrayList<>();

        userRepository.findAll().forEach(user -> {
            // 1. Daten laden
            List<Sale> sales = saleRepository.findByUserAndCreatedAtBetweenOrderByCreatedAtDesc(user, actualStart, actualEnd);
            List<Deposit> deposits = depositRepository.findByUserAndCreatedAtBetweenOrderByCreatedAtDesc(user, actualStart, actualEnd);

            int allSales = sales.stream().map(Sale::getPrice).mapToInt(Integer::intValue).sum();
            int allDeposits = deposits.stream().map(Deposit::getAmount).mapToInt(Integer::intValue).sum();

            // Vom aktuellen Kontostand zurück zu einem Wunschdatum =

            returnList.add(
                    new UserBalanceDTO(user.getId(), user.getUsername(),
                            user.getBalance() + allSales - allDeposits));

        });


        // 4. Sortieren: Neueste zuerst
        returnList.sort(Comparator.comparing(UserBalanceDTO::username));
        return returnList;
    }


}