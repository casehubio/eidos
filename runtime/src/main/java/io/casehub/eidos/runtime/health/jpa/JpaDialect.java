package io.casehub.eidos.runtime.health.jpa;

import jakarta.persistence.EntityManager;
import org.hibernate.Session;

final class JpaDialect {

    private static final java.util.logging.Logger LOG =
            java.util.logging.Logger.getLogger(JpaDialect.class.getName());

    private JpaDialect() {}

    static boolean isPostgres(EntityManager em) {
        try {
            return em.unwrap(Session.class).doReturningWork(connection -> {
                var product = connection.getMetaData().getDatabaseProductName();
                return product != null && product.equalsIgnoreCase("PostgreSQL");
            });
        } catch (Exception e) {
            LOG.warning("Could not determine database dialect via connection metadata; defaulting to H2: " + e.getMessage());
            return false;
        }
    }
}
