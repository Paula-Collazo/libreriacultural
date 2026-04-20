package com.proyectofinal.libreriacultural.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

@Data
@Entity
@Table(name = "user_song_progress", uniqueConstraints = {
        @UniqueConstraint(name = "uk_song_per_user_content", columnNames = { "user_content_id", "track_number" })
})
public class UserSongProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_content_id", nullable = false)
    private UserContent userContent;

    @Column(name = "track_number", nullable = false)
    private Integer trackNumber;

    @Column(name = "track_title", nullable = false, length = 255)
    private String trackTitle;

    @Column(nullable = false)
    private Boolean listened;
}
