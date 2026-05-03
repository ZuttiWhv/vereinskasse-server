package de.tozwhv.vereinskasse.server.repository;

import de.tozwhv.vereinskasse.server.modell.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByCategoryId(Long categoryId);
        List<Product> findAllByDeletedFalse();

        List<Product> findByCategoryIdAndDeletedFalse(Long categoryId);

        Optional<Product> findByBarcodeAndDeletedFalse(String barcode);
    }

