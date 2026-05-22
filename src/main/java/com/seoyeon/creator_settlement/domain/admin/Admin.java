package com.seoyeon.creator_settlement.domain.admin;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
@Entity
@Table(name = "admin")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Admin {
        @Id
        @Column(length = 50)
        private String id;

        @Column(nullable = false, length = 100)
        private String name;

        @Column(name = "created_at", nullable = false, updatable = false)
        private OffsetDateTime createdAt;

        @PrePersist
        void onCreate() {
            if (createdAt == null) createdAt = OffsetDateTime.now();
        }

        public Admin(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }
