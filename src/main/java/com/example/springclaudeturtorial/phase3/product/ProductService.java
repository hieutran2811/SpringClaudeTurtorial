package com.example.springclaudeturtorial.phase3.product;

import com.example.springclaudeturtorial.phase5.web.dto.ProductRequest;
import com.example.springclaudeturtorial.phase5.web.exception.BusinessException;
import com.example.springclaudeturtorial.phase5.web.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)  // default read-only cho toàn service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> findAll() {
        return productRepository.findAll();
    }

    // Phase 5: pagination
    public Page<Product> findAllPaged(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    public Product findById(Long id) {
        return productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }

    public List<Product> findByCategory(String category) {
        return productRepository.findByCategory(category);
    }

    @Transactional  // override → read-write
    public Product create(Product product) {
        if (productRepository.existsByName(product.getName())) {
            throw new BusinessException("DUPLICATE_PRODUCT",
                    "Product already exists: " + product.getName());
        }
        return productRepository.save(product);
    }

    @Transactional
    public Product update(Long id, Product updated) {
        Product existing = findById(id);
        existing.setName(updated.getName());
        existing.setCategory(updated.getCategory());
        existing.setPrice(updated.getPrice());
        existing.setStock(updated.getStock());
        return productRepository.save(existing);
    }

    // Phase 5: partial update — chỉ update field != null
    @Transactional
    public Product patch(Long id, ProductRequest request) {
        Product existing = findById(id);
        if (request.name()     != null) existing.setName(request.name());
        if (request.category() != null) existing.setCategory(request.category());
        if (request.price()    != null) existing.setPrice(request.price());
        if (request.stock()    != null) existing.setStock(request.stock());
        return productRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        Product product = findById(id);
        productRepository.delete(product);
    }
}
