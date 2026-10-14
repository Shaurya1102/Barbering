package com.Barbering.backend.repositories;

import com.Barbering.backend.entities.ServiceCatalogItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ServiceCatalogRepository extends JpaRepository<ServiceCatalogItem, Long> {
    Optional<ServiceCatalogItem> findByNameIgnoreCase(String name);
}
