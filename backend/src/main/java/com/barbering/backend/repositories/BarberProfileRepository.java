package com.Barbering.backend.repositories;

import com.Barbering.backend.entities.BarberProfile;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BarberProfileRepository extends JpaRepository<BarberProfile, Long> {
    Optional<BarberProfile> findByUserId(Long userId);

    @Query("select p from BarberProfile p where p.latitude is not null and p.longitude is not null")
    List<BarberProfile> findAllWithCoordinates();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from BarberProfile p where p.id = :id")
    Optional<BarberProfile> lockById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from BarberProfile p where p.userId = :userId")
    Optional<BarberProfile> lockByUserId(@Param("userId") Long userId);

    @Query("""
            select distinct p from BarberProfile p, User u
            where u.id = p.userId
              and (:query = '' or lower(u.displayName) like concat('%', :query, '%') or lower(coalesce(p.bio, '')) like concat('%', :query, '%')
                  or exists (
                      select oq.id from BarberServiceOffering oq, ServiceCatalogItem sq
                      where oq.barberProfileId = p.id and oq.serviceId = sq.id and oq.enabled = true
                        and lower(coalesce(oq.nameOverride, sq.name)) like concat('%', :query, '%')))
              and (:location = '' or lower(concat(coalesce(p.streetAddress, ''), ' ', coalesce(p.city, ''), ' ', coalesce(p.region, ''), ' ', coalesce(p.postalCode, ''), ' ', coalesce(p.country, ''))) like concat('%', :location, '%'))
              and (:service = '' or exists (
                  select o.id from BarberServiceOffering o, ServiceCatalogItem s
                  where o.barberProfileId = p.id and o.serviceId = s.id and o.enabled = true
                    and lower(coalesce(o.nameOverride, s.name)) like concat('%', :service, '%')))
            """)
    Page<BarberProfile> search(@Param("query") String query, @Param("location") String location,
                               @Param("service") String service, Pageable pageable);
}
