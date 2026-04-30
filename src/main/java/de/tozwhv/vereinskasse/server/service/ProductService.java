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

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    /**
     * Holt alle Produkte, die nicht gelöscht sind.
     * @param onlyActive Wenn true, werden nur vorrätige Produkte geliefert.
     */
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

    /**
     * Markiert ein Produkt als gelöscht (Soft-Delete).
     */
    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Produkt nicht gefunden"));
        product.setDeleted(true);
        productRepository.save(product);
    }

    // --- Helper Methoden ---

    private void updateProductFields(Product product, ProductRequestDTO dto, Category category) {
        product.setName(dto.name());
        product.setAnzeigename(dto.anzeigename());
        product.setPrice(dto.price());
        product.setImagePath(dto.imagePath());
        product.setCategory(category);
        product.setActive(dto.active());
    }

    private ProductResponseDTO mapToResponseDTO(Product product) {
        return new ProductResponseDTO(
                product.getId(),
                product.getName(),
                product.getAnzeigename(),
                product.getPrice(),
                product.getPriceString(), // Nutzt deine vorhandene Methode in der Entity
                product.getImagePath(),
                new ProductResponseDTO.CategorySummaryDTO(
                        product.getCategory().getId(),
                        product.getCategory().getName()
                ),
                product.isActive()
        );
    }
}