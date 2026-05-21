package com.proyectofinal.libreriacultural.Repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.proyectofinal.libreriacultural.domain.Content;

public interface ContentRepository extends JpaRepository<Content, Long> {

	Optional<Content> findFirstByTitleIgnoreCaseAndTypeIgnoreCase(String title, String type);

    Optional<Content> findByExternalId(String externalId);
}
