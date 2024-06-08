package io.debezium.demo.cryptoapp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "coincap")
public class CryptoEntity {

    @Id
    private String id;
    private Integer rank;
    private String symbol;
    private String name;

    @Column(precision = 60, scale = 20)
    private BigDecimal supply;
    @Column(precision = 60, scale = 20)
    private BigDecimal maxSupply;
    @Column(precision = 60, scale = 20)
    private BigDecimal marketCapUsd;

    @Column(name = "volumeDay", precision = 60, scale = 20)
    private BigDecimal volumeUsd24Hr;

    @Column(precision = 60, scale = 20)
    private BigDecimal priceUsd;

    @Column(name = "changePercent", precision = 60, scale = 20)
    private BigDecimal changePercent24Hr;

    @Column(name = "vwmap", precision = 60, scale = 20)
    private BigDecimal vwap24Hr;

    @Column(name = "lastUpdate")
    private long timestamp;


    public CryptoEntity copyTo(CryptoEntity target) {
        target.rank = this.rank;
        target.symbol = this.symbol;
        target.name = this.name;
        target.supply = this.supply;
        target.maxSupply = this.maxSupply;
        target.marketCapUsd = this.marketCapUsd;
        target.volumeUsd24Hr = this.volumeUsd24Hr;
        target.priceUsd = this.priceUsd;
        target.changePercent24Hr = this.changePercent24Hr;
        target.vwap24Hr = this.vwap24Hr;
        target.timestamp = this.timestamp;

        return target;
    }
}
