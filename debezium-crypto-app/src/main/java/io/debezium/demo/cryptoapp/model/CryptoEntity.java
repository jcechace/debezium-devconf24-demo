package io.debezium.demo.cryptoapp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

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

    private Double supply;
    private Double maxSupply;
    private Double marketCapUsd;

    @Column(name = "volumeDay")
    private Double volumeUsd24Hr;

    private Double priceUsd;

    @Column(name = "changePercent")
    private Double changePercent24Hr;

    @Column(name = "vwmap")
    private Double vwap24Hr;


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

        return target;
    }
}
