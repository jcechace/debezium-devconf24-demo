package io.debezium.demo.cryptoapp.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
@Getter
@Setter
public class CryptoAssets {
    private List<CryptoEntity> data;
    private Double timestamp;
}
