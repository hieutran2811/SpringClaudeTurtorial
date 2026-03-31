package com.example.springclaudeturtorial.phase3.product;

import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller — sẽ dùng để viết test @WebMvcTest
 */
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // GET /api/v1/products
    @GetMapping
    public List<Product> findAll() {
        return productService.findAll();
    }

    // GET /api/v1/products/{id}
    @GetMapping("/{id}")
    public Product findById(@PathVariable Long id) {
        return productService.findById(id);
    }

    // GET /api/v1/products?category=Electronics
    @GetMapping(params = "category")
    public List<Product> findByCategory(@RequestParam String category) {
        return productService.findByCategory(category);
    }

    // POST /api/v1/products
    @PostMapping
    public ResponseEntity<Product> create(@Valid @RequestBody Product product) {
        Product created = productService.create(product);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(created);
    }

    // PUT /api/v1/products/{id}
    @PutMapping("/{id}")
    public Product update(@PathVariable Long id, @Valid @RequestBody Product product) {
        return productService.update(id, product);
    }

    // DELETE /api/v1/products/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // Exception handling đã được chuyển sang GlobalExceptionHandler (Phase 5)
}
