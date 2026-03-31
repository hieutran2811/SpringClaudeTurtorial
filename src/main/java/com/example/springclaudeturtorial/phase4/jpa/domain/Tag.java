package com.example.springclaudeturtorial.phase4.jpa.domain;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Tag — Many side của @ManyToMany với Item (inverse side)
 */
@Entity
@Table(name = "tags")
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    // mappedBy = "tags": Item là owning side (Item định nghĩa @JoinTable)
    @ManyToMany(mappedBy = "tags")
    private List<Item> items = new ArrayList<>();

    protected Tag() {}

    public Tag(String name) { this.name = name; }

    public Long       getId()    { return id; }
    public String     getName()  { return name; }
    public List<Item> getItems() { return items; }

    @Override
    public String toString() { return "Tag{'" + name + "'}"; }
}
