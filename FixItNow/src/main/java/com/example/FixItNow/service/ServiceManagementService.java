package com.example.FixItNow.service;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Manages the service catalog — CRUD for individual services under each category (SRS FR4). */
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ServiceManagementService {

    private final ServiceRepository serviceRepository;
    private final CategoryRepository categoryRepository;

    public List<Service> getActiveServices() {
        return serviceRepository.findByIsActiveTrue();
    }

    public List<Service> getByCategory(Long categoryId) {
        return serviceRepository.findByCategoryIdAndIsActiveTrue(categoryId);
    }

    public Service getById(Long id) {
        return serviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found: " + id));
    }

    @Transactional
    public Service create(Long categoryId, Service service) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));
        service.setCategory(category);
        return serviceRepository.save(service);
    }

    @Transactional
    public Service update(Long id, Service updates) {
        Service existing = getById(id);
        if (updates.getName()        != null) existing.setName(updates.getName());
        if (updates.getDescription() != null) existing.setDescription(updates.getDescription());
        if (updates.getDayPayment()  != null) existing.setDayPayment(updates.getDayPayment());
        return serviceRepository.save(existing);
    }

    @Transactional
    public void deactivate(Long id) {
        Service service = getById(id);
        service.setActive(false);
        serviceRepository.save(service);
    }
}
