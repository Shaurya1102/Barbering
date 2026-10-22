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

    @Query(value = """
            select distinct p.* from barber_profiles p
            join users u on u.id = p.user_id
            where (:query = '' or lower(u.display_name) like concat('%', :query, '%') or lower(coalesce(p.bio, '')) like concat('%', :query, '%')
                or exists (
                    select 1 from barber_services oq
                    join services sq on oq.service_id = sq.id
                    where oq.barber_profile_id = p.id and oq.enabled = true
                      and lower(coalesce(oq.name_override, sq.name)) like concat('%', :query, '%')))
              and (:location = '' or lower(concat(coalesce(p.street_address, ''), ' ', coalesce(p.city, ''), ' ', coalesce(p.region, ''), ' ', coalesce(p.postal_code, ''), ' ', coalesce(p.country, ''))) like concat('%', :location, '%'))
              and (:service = '' or exists (
                  select 1 from barber_services o
                  join services s on o.service_id = s.id
                  where o.barber_profile_id = p.id and o.enabled = true
                    and lower(coalesce(o.name_override, s.name)) like concat('%', :service, '%')))
            """,
            countQuery = """
            select count(distinct p.id) from barber_profiles p
            join users u on u.id = p.user_id
            where (:query = '' or lower(u.display_name) like concat('%', :query, '%') or lower(coalesce(p.bio, '')) like concat('%', :query, '%')
                or exists (
                    select 1 from barber_services oq
                    join services sq on oq.service_id = sq.id
                    where oq.barber_profile_id = p.id and oq.enabled = true
                      and lower(coalesce(oq.name_override, sq.name)) like concat('%', :query, '%')))
              and (:location = '' or lower(concat(coalesce(p.street_address, ''), ' ', coalesce(p.city, ''), ' ', coalesce(p.region, ''), ' ', coalesce(p.postal_code, ''), ' ', coalesce(p.country, ''))) like concat('%', :location, '%'))
              and (:service = '' or exists (
                  select 1 from barber_services o
                  join services s on o.service_id = s.id
                  where o.barber_profile_id = p.id and o.enabled = true
                    and lower(coalesce(o.name_override, s.name)) like concat('%', :service, '%')))
            """,
            nativeQuery = true)
    Page<BarberProfile> search(@Param("query") String query, @Param("location") String location,
                               @Param("service") String service, Pageable pageable);
}
