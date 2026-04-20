package com.proyectofinal.libreriacultural.Repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.proyectofinal.libreriacultural.domain.UserSeriesEpisodeProgress;

public interface UserSeriesEpisodeProgressRepository extends JpaRepository<UserSeriesEpisodeProgress, Long> {

    List<UserSeriesEpisodeProgress> findByUserContentIdOrderBySeasonNumberAscEpisodeNumberAsc(Long userContentId);

    Optional<UserSeriesEpisodeProgress> findByUserContentIdAndSeasonNumberAndEpisodeNumber(Long userContentId,
            Integer seasonNumber, Integer episodeNumber);

    long countByUserContentIdAndWatchedTrue(Long userContentId);
}
