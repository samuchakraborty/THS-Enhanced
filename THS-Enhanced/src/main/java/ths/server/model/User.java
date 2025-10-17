package ths.server.model;

import java.time.LocalDateTime;

public class User {
  private Long id;
  private String name;
  private String email;
  private String passwordHash;
  private String role;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
   public User() {
        var now = java.time.LocalDateTime.now(java.time.ZoneOffset.UTC);
        this.createdAt = now;
        this.updatedAt = now;
    }

  // getters & setters
  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }

  public String getName() { return name; }
  public void setName(String name) { this.name = name; }

  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email; }

  public String getPasswordHash() { return passwordHash; }
  public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

  public String getRole() { return role; }
  public void setRole(String role) { this.role = role; }

  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
