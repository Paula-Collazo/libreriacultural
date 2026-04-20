package com.proyectofinal.libreriacultural.domain;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@Entity
@Table(name = "user_content", uniqueConstraints = {
    @UniqueConstraint(name = "uk_user_content_user_content", columnNames = { "user_id", "content_id" })
})
public class UserContent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "content_id", nullable = false)
    private Content content;

    @Column(length = 20)
    private String status;

    @Column(name = "movie_watched")
    private Boolean movieWatched;

    @Column(name = "book_current_page")
    private Integer bookCurrentPage;

    @Column(name = "book_total_pages")
    private Integer bookTotalPages;

    @Column(name = "added_date")
    private LocalDate addedDate;

    @OneToMany(mappedBy = "userContent")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private List<UserSeriesEpisodeProgress> episodeProgresses;

    @OneToMany(mappedBy = "userContent")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private List<UserSongProgress> songProgresses;
}
