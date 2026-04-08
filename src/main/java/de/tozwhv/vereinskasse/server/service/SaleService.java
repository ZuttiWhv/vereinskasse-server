package de.tozwhv.vereinskasse.server.service;

import de.tozwhv.vereinskasse.server.modell.Sale;
import de.tozwhv.vereinskasse.server.modell.User;
import de.tozwhv.vereinskasse.server.repository.SaleRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SaleService {

    private final SaleRepository saleRepository;

    public SaleService(SaleRepository saleRepository) {
        this.saleRepository = saleRepository;
    }

    public List<Sale> getSalesFiltered(User currentUser, LocalDateTime start, LocalDateTime end, boolean isAdmin) {
        // Falls kein Datum geliefert wurde, setzen wir einen extrem weiten Bereich oder Standard
        if (start == null) start = LocalDateTime.now().minusYears(100);
        if (end == null) end = LocalDateTime.now();

        if (isAdmin) {
            return saleRepository.findAllByCreatedAtBetweenOrderByCreatedAtDesc(start, end);
        } else {
            return saleRepository.findByUserAndCreatedAtBetweenOrderByCreatedAtDesc(currentUser, start, end);
        }
    }
}