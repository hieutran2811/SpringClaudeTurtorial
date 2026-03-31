package com.example.springclaudeturtorial.phase4.jpa.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Item — Many side của @ManyToOne với Category
 *       Many side của @ManyToMany với Tag
 */
@Entity
@Table(name = "items")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @DecimalMin("0.0")
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Min(0)
    private int stock = 0;

    /**
     * @ManyToOne: nhiều Item → 1 Category
     *
     * fetch = EAGER: load Category ngay khi load Item (default cho @ManyToOne)
     * LAZY thường tốt hơn cho performance — sẽ thấy rõ ở N+1 demo
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    /**
     * @ManyToMany: nhiều Item ↔ nhiều Tag
     *
     * @JoinTable: tạo bảng trung gian item_tags
     * joinColumns:        FK từ item_tags → items
     * inverseJoinColumns: FK từ item_tags → tags
     */
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name               = "item_tags",
        joinColumns        = @JoinColumn(name = "item_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private List<Tag> tags = new ArrayList<>();

    /**
     * @Version: Optimistic Locking
     * Mỗi lần update → version tăng 1
     * Nếu 2 transaction update cùng lúc → OptimisticLockException
     */
    @Version
    private Long version;

    protected Item() {}

    public Item(String name, BigDecimal price, int stock) {
        this.name  = name;
        this.price = price;
        this.stock = stock;
    }

    public void addTag(Tag tag) {
        tags.add(tag);
        tag.getItems().add(this);
    }

    // Getters & Setters
    public Long       getId()       { return id; }
    public String     getName()     { return name; }
    public BigDecimal getPrice()    { return price; }
    public int        getStock()    { return stock; }
    public Category   getCategory() { return category; }
    public List<Tag>  getTags()     { return tags; }
    public Long       getVersion()  { return version; }

    public void setName(String name)         { this.name = name; }
    public void setPrice(BigDecimal price)   { this.price = price; }
    public void setStock(int stock)          { this.stock = stock; }
    public void setCategory(Category cat)    { this.category = cat; }

    @Override
    public String toString() {
        return "Item{id=%d, name='%s', price=%s, stock=%d}".formatted(id, name, price, stock);
    }
}
