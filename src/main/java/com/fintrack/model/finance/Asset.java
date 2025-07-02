package com.fintrack.model.finance;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fintrack.constants.finance.AssetType;

@Entity
@Table(name = "asset", uniqueConstraints = @UniqueConstraint(columnNames = {"account_id", "asset_name"}))
@Data
@NoArgsConstructor
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "asset_name", nullable = false)
    private String assetName;

    @Column(name = "symbol", nullable = false)
    private String symbol;

    @Column(name = "unit", nullable = false)
    private String unit;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false)
    private AssetType assetType;
}