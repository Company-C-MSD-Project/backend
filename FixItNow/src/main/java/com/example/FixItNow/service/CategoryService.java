package com.example.FixItNow.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Service catalog management — categories created/managed by Admin (SRS FR7). */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<Category> getActiveCategories() {
        return categoryRepository.findByIsActiveTrue();
    }

    public Category getById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
    }

    @Transactional
    public Category create(Category category) {
        if (categoryRepository.existsByCategoryType(category.getCategoryType())) {
            throw new BadRequestException("Category already exists: " + category.getCategoryType());
        }
        return categoryRepository.save(category);
    }

    @Transactional
    public Category update(Long id, Category updates) {
        Category existing = getById(id);
        if (updates.getDescription() != null) existing.setDescription(updates.getDescription());
        if (updates.getCategoryType() != null) existing.setCategoryType(updates.getCategoryType());
        return categoryRepository.save(existing);
    }
}
