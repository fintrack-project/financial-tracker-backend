package com.fintrack.model.market;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "market_index_data", uniqueConstraints = @UniqueConstraint(columnNames = "symbol"))
@Data
@NoArgsConstructor
public class MarketIndexData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "symbol", nullable = false, length = 255, unique = true)
    private String symbol;

    @Column(name = "price", nullable = false, length = 255)
    private String price;

    @Column(name = "price_change")
    private Double priceChange;

    @Column(name = "percent_change", length = 255)
    private String percentChange;

    @Column(name = "price_high")
    private Double priceHigh;

    @Column(name = "price_low")
    private Double priceLow;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}