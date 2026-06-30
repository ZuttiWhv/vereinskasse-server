package de.tozwhv.vereinskasse.server.service;

import de.tozwhv.vereinskasse.server.modell.*;
import de.tozwhv.vereinskasse.server.repository.FeeRepository;
import de.tozwhv.vereinskasse.server.repository.SaleRepository;
import de.tozwhv.vereinskasse.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AccountingService {

    private final UserRepository userRepository;
    private final SaleRepository saleRepository;
    private final FeeRepository feeRepository; // Neu hinzugefügt

    /**
     * Bucht eine Gebühr (automatisch oder manuell).
     * Diese Methode ist der zentrale Punkt für alle 'Fee'-Transaktionen.
     */
    @Transactional
    public Fee bookFee(User user, int amount, TransactionType type, String description) {
        user.setBalance(user.getBalance() - amount);
        userRepository.save(user);

        // Fee-Eintrag erstellen
        Fee fee = new Fee();
        fee.setUser(user);
        fee.setAmount(amount);
        fee.setFeeType(type);
        fee.setDescription(description);

        return feeRepository.save(fee);
    }


    /**
     * Bucht einen regulären Verkauf vom Guthaben ab.
     */
    @Transactional
    public Sale bookRegularSale(User user, Product product, int amount) {
        int totalPrice = product.getPrice() * amount;
        validateBalance(user, totalPrice);

        // Guthaben abziehen
        user.setBalance(user.getBalance() - totalPrice);
        userRepository.save(user);

        // Sale-Eintrag erstellen
        Sale sale = createBaseSale(user, product, amount);
        sale.setPrice(totalPrice);
        sale.setUseVoucher(false);
        sale.setVoucher(false);

        return saleRepository.save(sale);
    }

    /**
     * Bucht die Einlösung eines Gutscheins (0€ in der Historie).
     */
    @Transactional
    public Sale bookVoucherRedemption(User user, Product product, int amount) {
        Sale sale = createBaseSale(user, product, amount);
        sale.setPrice(0);
        sale.setUseVoucher(true);
        sale.setVoucher(false);

        return saleRepository.save(sale);
    }

    /**
     * Bucht den Kauf eines Gutschein-Kontingents (Spende).
     */
    @Transactional
    public Sale bookVoucherPurchase(User user, Product product, int amount) {
        int totalPrice = product.getPrice() * amount;
        validateBalance(user, totalPrice);

        user.setBalance(user.getBalance() - totalPrice);
        userRepository.save(user);

        Sale sale = createBaseSale(user, product, amount);
        sale.setPrice(totalPrice);
        sale.setUseVoucher(false);
        sale.setVoucher(true); // Markiert, dass dies ein Gutscheinkauf war

        return saleRepository.save(sale);
    }

    private void validateBalance(User user, double requiredAmount) {
        long creditLimit = user.getBillingGroup().isAllowNegativeBalance()
                ? user.getBillingGroup().getCreditLimit()
                : 0;

        if ((user.getBalance() + creditLimit) < requiredAmount) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "Guthaben nicht ausreichend!");
        }
    }

    private Sale createBaseSale(User user, Product product, int amount) {
        Sale sale = new Sale();
        sale.setUser(user);
        sale.setProduct(product);
        sale.setAmount(amount);
        return sale;
    }
}