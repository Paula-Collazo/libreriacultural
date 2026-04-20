package com.proyectofinal.libreriacultural.Repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.proyectofinal.libreriacultural.domain.UserContent;

public interface UserContentRepository extends JpaRepository<UserContent, Long> {

	List<UserContent> findByUserId(Long userId);

	List<UserContent> findByUserIdAndContentType(Long userId, String type);

	List<UserContent> findByUserIdAndStatus(Long userId, String status);

	boolean existsByUserIdAndContentId(Long userId, Long contentId);

	long countByUserId(Long userId);

	long countByUserIdAndContentType(Long userId, String type);

	long countByUserIdAndStatus(Long userId, String status);
}
