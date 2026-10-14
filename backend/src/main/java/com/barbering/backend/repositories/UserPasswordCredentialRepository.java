package com.Barbering.backend.repositories;

import com.Barbering.backend.entities.UserPasswordCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserPasswordCredentialRepository extends JpaRepository<UserPasswordCredential, Long> {

    Optional<UserPasswordCredential> findByUserId(Long userId);
}
