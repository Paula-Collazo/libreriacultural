package com.proyectofinal.libreriacultural.Repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.proyectofinal.libreriacultural.domain.UserContent;

public interface UserContentRepository extends JpaRepository<UserContent, Long> {

	List<UserContent> findByUserId(Long userId);

	@Query("SELECT uc FROM UserContent uc WHERE uc.user.id = :userId AND UPPER(uc.content.type) = UPPER(:type)")
	List<UserContent> findByUserIdAndContentTypeIgnoreCase(@org.springframework.data.repository.query.Param("userId") Long userId, 
                                                          @org.springframework.data.repository.query.Param("type") String type);

	List<UserContent> findByUserIdAndContentType(Long userId, String type);

	List<UserContent> findByUserIdAndStatus(Long userId, String status);

	boolean existsByUserIdAndContentId(Long userId, Long contentId);

	long countByUserId(Long userId);

	@Query("SELECT COUNT(uc) FROM UserContent uc WHERE uc.user.id = :userId AND UPPER(uc.content.type) = UPPER(:type)")
	long countByUserIdAndContentTypeIgnoreCase(@org.springframework.data.repository.query.Param("userId") Long userId, 
                                               @org.springframework.data.repository.query.Param("type") String type);

	long countByUserIdAndContentType(Long userId, String type);

	long countByUserIdAndStatus(Long userId, String status);

	@Query("SELECT SUM(uc.bookCurrentPage) FROM UserContent uc WHERE uc.user.id = :userId")
	Long sumTotalPagesByUserId(@org.springframework.data.repository.query.Param("userId") Long userId);

	@Query("SELECT COUNT(uc) FROM UserContent uc WHERE uc.user.id = :userId AND uc.addedDate >= :startDate")
	long countRecentItems(@org.springframework.data.repository.query.Param("userId") Long userId, 
                         @org.springframework.data.repository.query.Param("startDate") java.time.LocalDate startDate);

	java.util.Optional<UserContent> findByUserIdAndContentExternalId(Long userId, String externalId);

    @Query("SELECT uc.completionDate as date, COUNT(uc) as count " +
           "FROM UserContent uc " +
           "WHERE uc.user.id = :userId " +
           "AND uc.completionDate IS NOT NULL " +
           "GROUP BY uc.completionDate " +
           "ORDER BY uc.completionDate ASC")
    List<Object[]> getTimeStats(@org.springframework.data.repository.query.Param("userId") Long userId);
}
