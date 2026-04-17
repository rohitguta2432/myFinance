package com.myfinance.dto;

import com.myfinance.model.FeatureFlag;
import java.time.LocalDateTime;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureFlagDTO {
    private String key;
    private Boolean enabled;
    private String label;
    private String description;
    private String category;
    private LocalDateTime updatedAt;
    private String updatedBy;

    public static FeatureFlagDTO from(FeatureFlag f) {
        return FeatureFlagDTO.builder()
                .key(f.getKey())
                .enabled(f.getEnabled())
                .label(f.getLabel())
                .description(f.getDescription())
                .category(f.getCategory())
                .updatedAt(f.getUpdatedAt())
                .updatedBy(f.getUpdatedBy())
                .build();
    }
}
