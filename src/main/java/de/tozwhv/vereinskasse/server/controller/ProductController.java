package de.tozwhv.vereinskasse.server.controller;

import de.tozwhv.vereinskasse.server.dto.product.ProductResponseDTO;
import de.tozwhv.vereinskasse.server.dto.product.ProductRequestDTO;
import de.tozwhv.vereinskasse.server.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor // Erzeugt den Konstruktor für den ProductService automatisch
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @PreAuthorize("hasAuthority('READ_PRODUCT')")
    public List<ProductResponseDTO> getAllProducts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "false") boolean onlyActive) {

        if (categoryId != null) {
            return productService.getProductsByCategory(categoryId);
        }
        return productService.getAllProducts(onlyActive);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('READ_PRODUCT')")
    public ResponseEntity<ProductResponseDTO> getProductById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(productService.getProductById(id));
        } catch (Exception _) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @PreAuthorize("hasAuthority('WRITE_PRODUCT')")
    public ResponseEntity<ProductResponseDTO> createProduct(@Valid @RequestBody ProductRequestDTO dto) {
        return ResponseEntity.ok(productService.createProduct(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('WRITE_PRODUCT')")
    public ResponseEntity<ProductResponseDTO> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequestDTO dto) {
        try {
            return ResponseEntity.ok(productService.updateProduct(id, dto));
        } catch (Exception _) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DELETE_PRODUCT')")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        try {
            productService.deleteProduct(id);
            return ResponseEntity.noContent().build();
        } catch (Exception _) {
            return ResponseEntity.notFound().build();
        }
    }
}