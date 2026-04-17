package com.myfinance.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "feature_flags")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatureFlag {

    @Id
    @Column(name = "flag_key", length = 64)
    private String key;

    @Column(nullable = false)
    private Boolean enabled;

    @Column(nullable = false, length = 128)
    private String label;

    @Column(length = 500)
    private String description;

    @Column(length = 32)
    private String category;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(length = 128)
    private String updatedBy;
}
