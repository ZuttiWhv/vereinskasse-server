package de.tozwhv.vereinskasse.server.service;

import de.tozwhv.vereinskasse.server.dto.TransactionDTO;
import de.tozwhv.vereinskasse.server.dto.UserBalanceDTO;
import de.tozwhv.vereinskasse.server.modell.*;
import de.tozwhv.vereinskasse.server.repository.DepositRepository;
import de.tozwhv.vereinskasse.server.repository.FeeRepository;
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
    private final FeeRepository feeRepository;


    public TransactionService(SaleRepository saleRepository, DepositRepository depositRepository, UserRepository userRepository, FeeRepository feeRepository) {
        this.saleRepository = saleRepository;
        this.depositRepository = depositRepository;
        this.userRepository = userRepository;
        this.feeRepository = feeRepository;
    }


    public List<TransactionDTO> getUserHistory(User user, LocalDateTime start, LocalDateTime end) {
        // Null-Safe Handling
        LocalDateTime actualStart = (start == null) ? LocalDateTime.now().minusYears(10) : start;
        LocalDateTime actualEnd = (end == null) ? LocalDateTime.now() : end;

        // 1. Daten laden
        List<Sale> sales = saleRepository.findByUserAndCreatedAtBetweenOrderByCreatedAtDesc(user, actualStart, actualEnd);
        List<Deposit> deposits = depositRepository.findByUserAndCreatedAtBetweenOrderByCreatedAtDesc(user, actualStart, actualEnd);
        List<Fee> fees = feeRepository.findByUserAndCreatedAtBetweenOrderByCreatedAtDesc(user, actualStart, actualEnd);

        List<TransactionDTO> history = new ArrayList<>();

        // 2. Sales konvertieren


        sales.forEach(s -> {
            TransactionType type = TransactionType.SALE;
            if (s.isUseVoucher()) type=TransactionType.REDEEM;
            if (s.isVoucher()) type=TransactionType.VOUCHER;
            history.add(new TransactionDTO(
                    s.getId(),
                    type.name(),
                    s.getProduct().getName(),
                    s.getAmount(),
                    s.getPrice(),
                    s.getCreatedAt()
            ));
        });

        // 3. Deposits konvertieren
        deposits.forEach(d -> history.add(new TransactionDTO(
                d.getId(),
                TransactionType.DEPOSIT.name(),
                TransactionType.DEPOSIT.getDisplayName(),
                1, // Menge bei Einzahlung ist immer 1
                d.getAmount(),
                d.getCreatedAt()
        )));

        // Fees konvertieren
        fees.forEach(f -> history.add(new TransactionDTO(
                f.getId(),
                TransactionType.MONTHLY_CONTRIBUTION.name(),
                TransactionType.MONTHLY_CONTRIBUTION.getDisplayName(),
                1,
                f.getAmount(),
                f.getCreatedAt()
        )));

        // 4. Sortieren: Neueste zuerst
        history.sort((a, b) -> b.date().compareTo(a.date()));

        return history;
    }

    public List<UserBalanceDTO> getALLUsersBalanceAtDate(LocalDateTime start) {
        LocalDateTime actualStart = (start == null) ? LocalDateTime.now().minusYears(10) : start;
        LocalDateTime actualEnd = LocalDateTime.now();

        List<UserBalanceDTO> returnList = new ArrayList<>();

        // WICHTIG: Wir nutzen weiterhin findAll(), um auch archivierte User zu prüfen
        userRepository.findAll().forEach(user -> {
            // 1. Daten laden
            List<Sale> sales = saleRepository.findByUserAndCreatedAtBetweenOrderByCreatedAtDesc(user, actualStart, actualEnd);
            List<Deposit> deposits = depositRepository.findByUserAndCreatedAtBetweenOrderByCreatedAtDesc(user, actualStart, actualEnd);
            List<Fee> fees = feeRepository.findByUserAndCreatedAtBetweenOrderByCreatedAtDesc(user, actualStart, actualEnd);

            int allSales = sales.stream().map(Sale::getPrice).mapToInt(Integer::intValue).sum();
            int allDeposits = deposits.stream().map(Deposit::getAmount).mapToInt(Integer::intValue).sum();
            int allFees = fees.stream().map(Fee::getAmount).mapToInt(Integer::intValue).sum();

            // 2. Kontostand zum Wunschdatum berechnen
            long balanceAtDate = user.getBalance() + allSales - allDeposits +allFees;

            // 3. Filter-Logik für archivierte Benutzer:
            // Ein aktiver User wird IMMER hinzugefügt.
            // Ein inaktiver (archivierter) User wird NUR hinzugefügt, wenn sein Kontostand NICHT 0 ist.
            if (user.isActive() || balanceAtDate != 0) {

                // Optional: Name für die Liste hübsch machen, falls es ein archivierter User ist
                String displayName = user.getUsername();
                if (!user.isActive() && displayName.contains("_archived_")) {
                    displayName = displayName.split("_archived_")[0] + " (Ehemalig)";
                }

                returnList.add(new UserBalanceDTO(
                        user.getId(),
                        displayName,
                        balanceAtDate
                ));
            }
        });

        // 4. Sortieren: Alphabetisch nach Username
        returnList.sort(Comparator.comparing(UserBalanceDTO::username));
        return returnList;
    }

}