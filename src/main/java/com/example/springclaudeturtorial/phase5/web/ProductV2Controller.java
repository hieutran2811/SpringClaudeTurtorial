package com.example.springclaudeturtorial.phase5.web;

import com.example.springclaudeturtorial.phase3.product.Product;
import com.example.springclaudeturtorial.phase3.product.ProductService;
import com.example.springclaudeturtorial.phase5.web.dto.ProductRequest;
import com.example.springclaudeturtorial.phase5.web.dto.ProductRequest.OnCreate;
import com.example.springclaudeturtorial.phase5.web.dto.ProductRequest.OnUpdate;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * ============================================================
 * PHASE 5 — REST Design & HTTP Conventions
 * ============================================================
 *
 * Điểm khác biệt với ProductController (Phase 3):
 *
 * 1. Dùng DTO (ProductRequest) thay vì trực tiếp Entity
 * 2. @Validated(OnCreate.class) thay vì @Valid → kích hoạt Validation Groups
 * 3. Location header khi tạo resource mới (chuẩn RFC 7231)
 * 4. Pagination với Pageable thay vì trả List toàn bộ
 * 5. PATCH cho partial update (chỉ update field được gửi)
 *
 * ── HTTP Method Conventions ───────────────────────────────
 *   POST   /products        → tạo mới     → 201 Created + Location header
 *   GET    /products        → danh sách   → 200 OK (có pagination)
 *   GET    /products/{id}   → chi tiết    → 200 OK
 *   PUT    /products/{id}   → thay toàn bộ → 200 OK
 *   PATCH  /products/{id}   → cập nhật một phần → 200 OK
 *   DELETE /products/{id}   → xóa         → 204 No Content
 * ============================================================
 */
@RestController
@RequestMapping("/api/v2/products")
public class ProductV2Controller {

    private final ProductService productService;

    public ProductV2Controller(ProductService productService) {
        this.productService = productService;
    }

    // ── GET /api/v2/products?page=0&size=10&sort=name,asc ─────────────────
    // Pageable được Spring tự inject từ query params nhờ PageableHandlerMethodArgumentResolver
    @GetMapping
    public Page<Product> findAll(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
        return productService.findAllPaged(pageable);
    }

    // ── GET /api/v2/products/{id} ──────────────────────────────────────────
    @GetMapping("/{id}")
    public Product findById(@PathVariable Long id) {
        return productService.findById(id);  // throws ResourceNotFoundException → 404
    }

    // ── POST /api/v2/products → 201 Created + Location header ─────────────
    //
    // @PreAuthorize: method-level security — kiểm tra role TRƯỚC khi vào method.
    // Kích hoạt bởi @EnableMethodSecurity trong SecurityConfig.
    // hasAnyRole("USER","ADMIN") = hasAuthority("ROLE_USER") || hasAuthority("ROLE_ADMIN")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PostMapping
    public ResponseEntity<Product> create(
            @Validated(OnCreate.class) @RequestBody ProductRequest request) {

        Product product = toProduct(request);
        Product created = productService.create(product);

        // Location header = URI của resource vừa tạo (chuẩn REST)
        // Ví dụ: Location: http://localhost:8080/api/v2/products/5
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();

        return ResponseEntity.created(location).body(created);
    }

    // ── PUT /api/v2/products/{id} → thay toàn bộ resource ─────────────────
    //
    // @Validated(OnUpdate.class): group OnUpdate — không có constraint OnUpdate.class
    // nào thêm hiện tại, nhưng về sau có thể thêm (vd: @NotNull(groups=OnUpdate.class))
    @PutMapping("/{id}")
    public Product update(
            @PathVariable Long id,
            @Validated(OnUpdate.class) @RequestBody ProductRequest request) {

        Product updated = toProduct(request);
        return productService.update(id, updated);
    }

    // ── PATCH /api/v2/products/{id} → partial update ──────────────────────
    //
    // Khác PUT: client chỉ gửi field muốn thay đổi, null = giữ nguyên giá trị cũ.
    // Không dùng @Validated vì không bắt buộc tất cả field phải có.
    @PatchMapping("/{id}")
    public Product patch(@PathVariable Long id, @RequestBody ProductRequest request) {
        return productService.patch(id, request);
    }

    // ── DELETE /api/v2/products/{id} → 204 No Content ─────────────────────
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();  // 204 — không có body
    }

    // ── Helper: DTO → Entity ───────────────────────────────────────────────
    private Product toProduct(ProductRequest r) {
        return new Product(
                r.name(),
                r.category(),
                r.price(),
                r.stock() != null ? r.stock() : 0
        );
    }
}
