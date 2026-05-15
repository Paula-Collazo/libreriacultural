package com.proyectofinal.libreriacultural.Repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.proyectofinal.libreriacultural.domain.UserContent;

public interface UserContentRepository extends JpaRepository<UserContent, Long> {

	List<UserContent> findByUserId(Long userId);

	List<UserContent> findByUserIdAndContentType(Long userId, String type);

	List<UserContent> findByUserIdAndStatus(Long userId, String status);

	boolean existsByUserIdAndContentId(Long userId, Long contentId);

	long countByUserId(Long userId);

	long countByUserIdAndContentType(Long userId, String type);

	long countByUserIdAndStatus(Long userId, String status);

	@Query("SELECT SUM(uc.bookCurrentPage) FROM UserContent uc WHERE uc.user.id = :userId")
	Long sumTotalPagesByUserId(@org.springframework.data.repository.query.Param("userId") Long userId);

	@Query("SELECT COUNT(uc) FROM UserContent uc WHERE uc.user.id = :userId AND uc.addedDate >= :startDate")
	long countRecentItems(@org.springframework.data.repository.query.Param("userId") Long userId, 
                         @org.springframework.data.repository.query.Param("startDate") java.time.LocalDate startDate);
}
