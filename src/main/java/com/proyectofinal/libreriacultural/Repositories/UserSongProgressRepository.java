package com.proyectofinal.libreriacultural.Repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.proyectofinal.libreriacultural.domain.UserSongProgress;

public interface UserSongProgressRepository extends JpaRepository<UserSongProgress, Long> {

    List<UserSongProgress> findByUserContentIdOrderByTrackNumberAsc(Long userContentId);

    Optional<UserSongProgress> findByUserContentIdAndTrackNumber(Long userContentId, Integer trackNumber);

    long countByUserContentIdAndListenedTrue(Long userContentId);
}
