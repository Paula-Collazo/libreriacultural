package com.proyectofinal.libreriacultural.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.proyectofinal.libreriacultural.domain.Content;

public interface ContentRepository extends JpaRepository<Content, Long> {
    
}
