package de.tozwhv.vereinskasse.server.controller;

import de.tozwhv.vereinskasse.server.dto.CategoryDTO;
import de.tozwhv.vereinskasse.server.modell.Category;
import de.tozwhv.vereinskasse.server.repository.CategoryRepository;
import de.tozwhv.vereinskasse.server.service.FileStorageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryRepository categoryRepository;
    private final FileStorageService storageService;

    public CategoryController(CategoryRepository categoryRepository, FileStorageService storageService) {
        this.categoryRepository = categoryRepository;
        this.storageService = storageService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('READ_CATEGORY')")
    public List<Category> getAll() {
        return categoryRepository.findAll();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('WRITE_CATEGORY')")
    public ResponseEntity<Category> update(@PathVariable Long id, @Valid @RequestBody CategoryDTO dto) {
        return categoryRepository.findById(id).map(existingCategory -> {
            existingCategory.setName(dto.getName());
            if (dto.getImagePath() != null && !dto.getImagePath().isBlank()) {
                existingCategory.setImagePath(dto.getImagePath());
            }
            Category updated = categoryRepository.save(existingCategory);
            return ResponseEntity.ok(updated);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Category> create(@Valid @RequestBody CategoryDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(categoryRepository.save(dto.toEntity()));
    }

    @PostMapping("/{id}/image")
    @PreAuthorize("hasAuthority('WRITE_CATEGORY') and hasAuthority('UPLOAD_IMAGES')")
    public ResponseEntity<String> uploadImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        String path = storageService.storeFile(file);
        if (path != null) {
            Category c = categoryRepository.findById(id).orElseThrow();
            c.setImagePath("/media/" + path);
            categoryRepository.save(c);
            return ResponseEntity.ok(c.getImagePath());
        } else {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('READ_CATEGORY')")
    public ResponseEntity<Category> getById(@PathVariable Long id) {
        return categoryRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DELETE_CATEGORY')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (categoryRepository.existsById(id)) {
            categoryRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}