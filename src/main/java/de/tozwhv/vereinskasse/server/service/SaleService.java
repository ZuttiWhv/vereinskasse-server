package de.tozwhv.vereinskasse.server.service;

import de.tozwhv.vereinskasse.server.dto.SaleDTO;
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

    public List<SaleDTO> getSalesFiltered(User currentUser, LocalDateTime start, LocalDateTime end, boolean isAdmin) {
        List<Sale> sales;

        if (start == null) start = LocalDateTime.now().minusYears(100);
        if (end == null) end = LocalDateTime.now();

        if (isAdmin) {
            sales = saleRepository.findAllByCreatedAtBetweenOrderByCreatedAtDesc(start, end);
        } else {
            sales = saleRepository.findByUserAndCreatedAtBetweenOrderByCreatedAtDesc(currentUser, start, end);
        }

        // Mapping von Entity zu DTO
        return sales.stream()
                .map(this::convertToDTO)
                .toList();
    }
    private SaleDTO convertToDTO(Sale sale) {
        return new SaleDTO(
                sale.getId(),
                sale.getPrice(),
                sale.getAmount(),
                sale.getCreatedAt(),
                sale.getProduct().getId(),
                sale.getProduct().getAnzeigename() != null ? sale.getProduct().getAnzeigename() : sale.getProduct().getName(),
                sale.getUser().getId(),
                sale.getUser().getUsername()
        );
    }
}