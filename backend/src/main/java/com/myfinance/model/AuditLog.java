package com.myfinance.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    private String userName;
    private String userEmail;

    private String action; // LOGIN, SAVE_PROFILE, ADD_INCOME, DELETE_GOAL, etc.
    private String entity; // profile, income, expense, asset, liability, goal, insurance, tax
    private Long entityId;

    private String details; // Optional extra info

    @CreationTimestamp
    private LocalDateTime createdAt;
}
