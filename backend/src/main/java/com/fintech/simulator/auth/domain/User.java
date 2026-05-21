package com.fintech.simulator.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "USERS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @Column(name = "user_id", length = 50)
    private String userId;

    @Column(name = "password", length = 100, nullable = false)
    private String password;

    @Column(name = "username", length = 50, nullable = false)
    private String username;

    @Column(name = "email", length = 100, nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 10, nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 10, nullable = false)
    private UserStatus status;

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    private User(String userId, String password, String username, String email, Role role) {
        this.userId = userId;
        this.password = password;
        this.username = username;
        this.email = email;
        this.role = role;
        this.status = UserStatus.ACTIVE;
        this.createdAt = OffsetDateTime.now();
    }

    /** 일반 회원가입용 팩토리 */
    public static User newUser(String username, String email, String encodedPassword) {
        return new User(UUID.randomUUID().toString(), encodedPassword, username, email, Role.USER);
    }

    /** 관리자 시드용 팩토리 (D06에서 사용 예정) */
    public static User newAdmin(String username, String email, String encodedPassword) {
        return new User(UUID.randomUUID().toString(), encodedPassword, username, email, Role.ADMIN);
    }

    public void markLoggedIn() {
        this.lastLoginAt = OffsetDateTime.now();
    }

    public void suspend()  { this.status = UserStatus.SUSPENDED; }
    public void activate() { this.status = UserStatus.ACTIVE; }
    public void changeRole(Role role) { this.role = role; }
}
