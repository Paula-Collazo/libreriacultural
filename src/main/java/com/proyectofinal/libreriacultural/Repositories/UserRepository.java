package com.proyectofinal.libreriacultural.Repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.proyectofinal.libreriacultural.domain.User;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findFirstByUsername(String username);

	Optional<User> findByUsername(String username);

	Optional<User> findByEmail(String email);
}
