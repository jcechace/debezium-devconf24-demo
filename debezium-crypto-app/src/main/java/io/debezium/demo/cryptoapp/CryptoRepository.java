package io.debezium.demo.cryptoapp;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.debezium.demo.cryptoapp.model.CryptoEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class CryptoRepository implements PanacheRepositoryBase<CryptoEntity, String> {

    @Inject
    Logger logger;

    public boolean upsert(long timestamp, CryptoEntity crypto) {
        var entity = findById(crypto.getId());

        if (entity == null) {
            logger.debugf("Creating new crypto: %s", crypto.getName());
            crypto.setTimestamp(timestamp);
            persist(crypto);
            return true;
        } else if (!entity.hasSameDataAs(crypto)) {
            logger.debugf("Updating crypto: %s", crypto.getName());
            crypto.copyTo(timestamp, entity);
            persist(entity);
            return true;
        }
        return false;
    }
}
