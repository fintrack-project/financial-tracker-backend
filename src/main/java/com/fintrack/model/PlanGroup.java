package com.fintrack.model;

import jakarta.persistence.*;

@Entity
@Table(name = "plan_groups")
public class PlanGroup {

    @Id
    private String id;

    @Column(name = "lvl", nullable = false, unique = true)
    private Short level;

    @Column(name = "name", nullable = false)
    private String name;

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Short getLevel() {
        return level;
    }

    public void setLevel(Short level) {
        this.level = level;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
} 