package com.example.springclaudeturtorial.phase4.jpa.repository;

import com.example.springclaudeturtorial.phase4.jpa.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByName(String name);

    // JOIN FETCH — load category kèm items trong 1 query
    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.items WHERE c.id = :id")
    Optional<Category> findByIdWithItems(Long id);
}
