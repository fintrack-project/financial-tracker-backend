package com.fintrack.model.market;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fintrack.constants.finance.AssetType;

@Entity
@Table(name = "market_data_monthly", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"symbol", "date", "asset_type"})
})
@Data
@NoArgsConstructor
public class MarketDataMonthly {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String symbol;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false)
    private AssetType assetType;
}