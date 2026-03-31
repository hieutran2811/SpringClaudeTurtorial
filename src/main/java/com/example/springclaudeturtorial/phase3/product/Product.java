package com.example.springclaudeturtorial.phase3.product;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100)
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "Category is required")
    private String category;

    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be positive")
    @Column(nullable = false)
    private Double price;

    @Min(value = 0, message = "Stock cannot be negative")
    private Integer stock = 0;

    protected Product() {}

    public Product(String name, String category, Double price, Integer stock) {
        this.name     = name;
        this.category = category;
        this.price    = price;
        this.stock    = stock;
    }

    // Getters
    public Long    getId()       { return id; }
    public String  getName()     { return name; }
    public String  getCategory() { return category; }
    public Double  getPrice()    { return price; }
    public Integer getStock()    { return stock; }

    // Setters
    public void setName(String name)         { this.name = name; }
    public void setCategory(String category) { this.category = category; }
    public void setPrice(Double price)       { this.price = price; }
    public void setStock(Integer stock)      { this.stock = stock; }

    @Override
    public String toString() {
        return "Product{id=%d, name='%s', category='%s', price=%.0f, stock=%d}"
            .formatted(id, name, category, price, stock);
    }
}
