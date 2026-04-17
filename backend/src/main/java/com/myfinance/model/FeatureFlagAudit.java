package com.myfinance.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "feature_flag_audit")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatureFlagAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "flag_key", nullable = false, length = 64)
    private String flagKey;

    private Boolean oldValue;
    private Boolean newValue;

    @Column(length = 128)
    private String changedBy;

    @CreationTimestamp
    private LocalDateTime changedAt;
}
