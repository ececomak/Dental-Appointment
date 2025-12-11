package com.ece.dental_clinic.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "treatment")
public class Treatment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "default_price", nullable = false)
    private Double defaultPrice;

    @Column(name = "default_duration_minutes")
    private Integer defaultDurationMinutes;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public Treatment() {
    }

    // GETTER / SETTER

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getDefaultPrice() {
        return defaultPrice;
    }

    public void setDefaultPrice(Double defaultPrice) {
        this.defaultPrice = defaultPrice;
    }

    public Integer getDefaultDurationMinutes() {
        return defaultDurationMinutes;
    }

    public void setDefaultDurationMinutes(Integer defaultDurationMinutes) {
        this.defaultDurationMinutes = defaultDurationMinutes;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}