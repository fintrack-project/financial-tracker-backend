package com.fintrack.model.market;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fintrack.constants.finance.AssetType;

@Entity
@Table(name = "market_data", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"symbol", "asset_type"})
})
@Data
@NoArgsConstructor
public class MarketData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "symbol", nullable = false, length = 10, unique = true)
    private String symbol;

    @Column(name = "price", precision = 19, scale = 4)
    private BigDecimal price;

    @Column(name = "percent_change", precision = 19, scale = 4)
    private BigDecimal percentChange;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false)
    private AssetType assetType;

    @Column(name = "change", precision = 15, scale = 6)
    private BigDecimal change;

    @Column(name = "high", precision = 15, scale = 6)
    private BigDecimal high;

    @Column(name = "low", precision = 15, scale = 6)
    private BigDecimal low;
}