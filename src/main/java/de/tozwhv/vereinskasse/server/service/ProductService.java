package de.tozwhv.vereinskasse.server.service;

import de.tozwhv.vereinskasse.server.dto.product.ProductRequestDTO;
import de.tozwhv.vereinskasse.server.dto.product.ProductResponseDTO;
import de.tozwhv.vereinskasse.server.modell.Category;
import de.tozwhv.vereinskasse.server.modell.Product;
import de.tozwhv.vereinskasse.server.repository.CategoryRepository;
import de.tozwhv.vereinskasse.server.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public List<ProductResponseDTO> getAllProducts(boolean onlyActive) {
        return productRepository.findAllByDeletedFalse().stream()
                .filter(p -> !onlyActive || p.isActive())
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public List<ProductResponseDTO> getProductsByCategory(Long categoryId) {
        return productRepository.findByCategoryIdAndDeletedFalse(categoryId).stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public ProductResponseDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new EntityNotFoundException("Produkt nicht gefunden"));
        return mapToResponseDTO(product);
    }

    @Transactional
    public ProductResponseDTO createProduct(ProductRequestDTO dto) {
        Category category = categoryRepository.findById(dto.categoryId())
                .orElseThrow(() -> new EntityNotFoundException("Kategorie existiert nicht"));

        Product product = new Product();
        updateProductFields(product, dto, category);

        Product savedProduct = productRepository.save(product);
        return mapToResponseDTO(savedProduct);
    }

    @Transactional
    public ProductResponseDTO updateProduct(Long id, ProductRequestDTO dto) {
        Product product = productRepository.findById(id)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new EntityNotFoundException("Produkt nicht gefunden"));

        Category category = categoryRepository.findById(dto.categoryId())
                .orElseThrow(() -> new EntityNotFoundException("Kategorie existiert nicht"));

        updateProductFields(product, dto, category);

        return mapToResponseDTO(productRepository.save(product));
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Produkt nicht gefunden"));
        product.setDeleted(true);
        productRepository.save(product);
    }

    public ProductResponseDTO getProductByBarcode(String barcode) {
        Product product = productRepository.findByBarcodesContainingAndDeletedFalse(barcode)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new EntityNotFoundException("Kein produkt zum Barcode gefunden"));
        return mapToResponseDTO(product);
    }

    // --- Helper Methoden ---

    private void updateProductFields(Product product, ProductRequestDTO dto, Category category) {
        // NEU: Validierung triggern, bevor irgendwas in die Entity geschrieben wird!
        validateBarcodes(dto.barcodes());

        product.setName(dto.name());
        product.setAnzeigename(dto.anzeigename());
        product.setPrice(dto.price());
        product.setImagePath(dto.imagePath());
        product.setCategory(category);
        product.setActive(dto.active());
        product.setBarcodes(dto.barcodes());
    }

    /**
     * NEU: Überprüft die übergebenen Barcodes auf korrekte EAN-Ziffern und Längen (8 oder 13).
     */
    private void validateBarcodes(Collection<String> barcodes) {
        if (barcodes != null) {
            for (String barcode : barcodes) {
                String trimmed = barcode.trim();

                // Besteht der Code nur aus Zahlen?
                if (!trimmed.matches("\\d+")) {
                    throw new IllegalArgumentException("Barcode '" + barcode + "' ist ungültig! Nur Ziffern (0-9) erlaubt.");
                }

                // Entspricht die Länge der EAN-Norm?
                if (trimmed.length() != 8 && trimmed.length() != 13) {
                    throw new IllegalArgumentException("Barcode '" + barcode + "' hat eine ungültige Länge (" + trimmed.length() + " Zeichen). Erlaubt sind nur 8 oder 13 Stellen!");
                }
            }
        }
    }

    private ProductResponseDTO mapToResponseDTO(Product product) {
        return new ProductResponseDTO(
                product.getId(),
                product.getName(),
                product.getAnzeigename(),
                product.getPrice(),
                product.getPriceString(),
                product.getImagePath(),
                new ProductResponseDTO.CategorySummaryDTO(
                        product.getCategory().getId(),
                        product.getCategory().getName()
                ),
                product.isActive(),
                product.getBarcodes()
        );
    }
}