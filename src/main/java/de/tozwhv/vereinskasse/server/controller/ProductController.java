package de.tozwhv.vereinskasse.server.controller;

import de.tozwhv.vereinskasse.server.modell.Product;
import de.tozwhv.vereinskasse.server.repository.ProductRepository;
import de.tozwhv.vereinskasse.server.service.FileStorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductRepository productRepository;
    private final FileStorageService storageService;

    public ProductController(ProductRepository productRepository, FileStorageService storageService) {
        this.productRepository = productRepository;
        this.storageService = storageService;
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

    @PostMapping("/{id}/image")
    @PreAuthorize("hasAuthority('WRITE_PRODUCT') and hasAuthority('UPLOAD_IMAGES')")
    public ResponseEntity<String> uploadImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) {

        String path = storageService.storeFile(file);
        if (path != null) {
            Product p = productRepository.findById(id).orElseThrow();
            p.setImagePath("/media/" + path);
            productRepository.save(p);
            return ResponseEntity.ok(p.getImagePath());
        } else {
            return ResponseEntity.internalServerError().build();
        }
    }


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
