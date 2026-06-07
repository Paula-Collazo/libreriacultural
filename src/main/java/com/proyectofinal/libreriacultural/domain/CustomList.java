package com.proyectofinal.libreriacultural.domain;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "custom_lists")
public class CustomList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToMany
    @JoinTable(
        name = "list_contents",
        joinColumns = @JoinColumn(name = "list_id"),
        inverseJoinColumns = @JoinColumn(name = "user_content_id")
    )
    private List<UserContent> items = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String coverUrl;

    public CustomList() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public List<UserContent> getItems() { return items; }
    public void setItems(List<UserContent> items) { this.items = items; }
}
