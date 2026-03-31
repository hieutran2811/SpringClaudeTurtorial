package com.example.springclaudeturtorial.phase4.jpa.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.util.ArrayList;
import java.util.List;

/**
 * Category — One side của @OneToMany với Item
 */
@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(unique = true, nullable = false)
    private String name;

    private String description;

    /**
     * @OneToMany: 1 Category → nhiều Item
     *
     * mappedBy = "category": FK nằm ở bảng Item (Item là "owning side")
     * cascade = ALL:         save/delete Category → cascade sang Item
     * orphanRemoval = true:  xoá Item khỏi list → tự xoá khỏi DB
     * fetch = LAZY:          KHÔNG load items khi load Category (default cho collection)
     */
    @OneToMany(
        mappedBy    = "category",
        cascade     = CascadeType.ALL,
        orphanRemoval = true,
        fetch       = FetchType.LAZY
    )
    private List<Item> items = new ArrayList<>();

    protected Category() {}

    public Category(String name, String description) {
        this.name        = name;
        this.description = description;
    }

    // Convenience methods — luôn dùng helper method để duy trì bidirectional sync
    public void addItem(Item item) {
        items.add(item);
        item.setCategory(this);
    }

    public void removeItem(Item item) {
        items.remove(item);
        item.setCategory(null);
    }

    public Long   getId()          { return id; }
    public String getName()        { return name; }
    public String getDescription() { return description; }
    public List<Item> getItems()   { return items; }
    public void setName(String name) { this.name = name; }
}
