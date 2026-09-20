package rs.serbianelection2026.backend.predictionmarket.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.serbianelection2026.backend.common.entity.AuditableEntity;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "prediction_market_outcome")
public class PredictionMarketOutcome extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prediction_market_id", nullable = false)
    private PredictionMarket predictionMarket;

    @Column(name = "external_id", nullable = false, length = 255)
    private String externalId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, precision = 6, scale = 4)
    private BigDecimal price;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "image_url", length = 2048)
    private String imageUrl;

    @Column(precision = 18, scale = 2)
    private BigDecimal volume;

    @Column(name = "one_day_price_change", precision = 6, scale = 4)
    private BigDecimal oneDayPriceChange;

    @Column(name = "best_ask", precision = 6, scale = 4)
    private BigDecimal bestAsk;

    @Column(name = "best_bid", precision = 6, scale = 4)
    private BigDecimal bestBid;

    /** Compact JSON of the recent price history, kept only for the leading outcomes shown as a chart. */
    @Column(name = "price_history")
    private String priceHistory;
}
