package io.debezium.demo.cryptoapp;

import io.debezium.demo.cryptoapp.client.CoinCapClient;
import io.debezium.demo.cryptoapp.model.CryptoAssets;
import io.debezium.demo.cryptoapp.model.CryptoEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class CoinCapService {

    @Inject
    CryptoRepository repository;

    @RestClient
    CoinCapClient client;

    public void fetchAndUpdate() {
        CryptoAssets assets = client.getAll();
        assets.getData().forEach(this::update);
    }

    @Transactional
    public void update(CryptoEntity entity) {
        repository.upsert(entity);
    }
}
