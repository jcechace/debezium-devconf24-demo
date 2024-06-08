package io.debezium.demo.cryptoapp;

import java.util.List;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.debezium.demo.cryptoapp.model.CryptoEntity;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CryptoRepository implements PanacheRepositoryBase<CryptoEntity, String> {

    public void upsertAll(long timestamp, List<CryptoEntity> cryptos) {
        cryptos.stream()
                .map(crypto -> findByIdOptional(crypto.getId())
                        .map(crypto::copyTo)
                        .orElse(crypto))
                .peek(entity -> entity.setTimestamp(timestamp))
                .forEach(this::persist);
    }
}
