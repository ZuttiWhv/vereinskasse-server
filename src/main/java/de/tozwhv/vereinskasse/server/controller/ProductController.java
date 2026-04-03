package de.tozwhv.vereinskasse.server.controller;

import org.springframework.web.bind.annotation.*;
import de.tozwhv.vereinskasse.server.modell.Product;
import de.tozwhv.vereinskasse.server.repository.ProductRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;


import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductRepository productRepository;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('READ_PRODUCT')")
    public List<Product> getAllProducts(@RequestParam(required = false) Long categoryId) {
        if (categoryId != null) {
            return productRepository.findByCategoryId(categoryId);
        }
        return productRepository.findAll();
    }


    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('READ_PRODUCT')")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        return productRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('WRITE_PRODUCT')")
    public Product createProduct(@RequestBody Product product) {
        return productRepository.save(product);
    }

    // Benutzer aktualisieren
    @PreAuthorize("hasAuthority('WRITE_PRODUCT')")
    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @RequestBody Product productDetails) {
        return productRepository.findById(id).map(product -> {
            product.setAnzeigename(productDetails.getAnzeigename());
            product.setCategory(productDetails.getCategory());
            product.setName(productDetails.getName());
            product.setImagePath(productDetails.getImagePath());
            product.setPrice(productDetails.getPrice());
            return ResponseEntity.ok(productRepository.save(product));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DELETE_PRODUCT')")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        if (productRepository.existsById(id)) {
            productRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
