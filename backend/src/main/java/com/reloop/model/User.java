package com.reloop.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A person using the system - either a citizen registering e-waste, or a
 * recycler operator managing pickups. This is a plain domain entity, not
 * an account: there is no password field and no login. "Which user is
 * acting" is decided by the frontend's demo-mode user switcher and passed
 * as a plain userId on requests, not by any authentication mechanism.
 *
 * Real authentication (passwords/sessions/JWT) was explicitly out of scope
 * for this project - see docs/INTERVIEW_QA.md for how it would be added.
 */
@Entity
@Table(name = "app_user") // "user" is a reserved word in both H2 and PostgreSQL
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    protected User() {
    }

    public User(String name, String email, UserRole role) {
        this.name = name;
        this.email = email;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }
}
