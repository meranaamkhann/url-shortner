package com.urlshortener.domain.entity;

import com.urlshortener.domain.enums.UrlStatus;
import com.urlshortener.domain.enums.Visibility;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "urls")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Url extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "short_code", nullable = false, length = 32)
    private String shortCode;

    @Column(name = "long_url", nullable = false, columnDefinition = "TEXT")
    private String longUrl;

    @Column(name = "long_url_hash", nullable = false, length = 64)
    private String longUrlHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "domain_id")
    private CustomDomain domain;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(name = "is_custom_alias", nullable = false)
    @Builder.Default
    private boolean customAlias = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Visibility visibility = Visibility.PUBLIC;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private UrlStatus status = UrlStatus.ACTIVE;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "max_clicks")
    private Long maxClicks;

    @Column(name = "click_count", nullable = false)
    @Builder.Default
    private long clickCount = 0L;

    @Column(name = "deleted_at")
    private Instant deletedAt;


    public boolean isExpired() {
        if (status == UrlStatus.EXPIRED) return true;
        if (expiresAt != null && Instant.now().isAfter(expiresAt)) return true;
        if (maxClicks != null && clickCount >= maxClicks) return true;
        return false;
    }

    public boolean isRedirectable() {
        return status == UrlStatus.ACTIVE && deletedAt == null && !isExpired();
    }

    public boolean isPasswordProtected() {
        return passwordHash != null && !passwordHash.isBlank();
    }
}
