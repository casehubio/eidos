package io.casehub.eidos.runtime.health.jpa;

import jakarta.persistence.EntityManager;

final class JpaDialect {

    private static final java.util.logging.Logger LOG =
            java.util.logging.Logger.getLogger(JpaDialect.class.getName());

    private JpaDialect() {}

    static boolean isPostgres(EntityManager em) {
        var props = em.getEntityManagerFactory().getProperties();
        var url   = (String) props.get("jakarta.persistence.jdbc.url");
        if (url == null) {
            url = (String) props.get("javax.persistence.jdbc.url");
        }
        if (url == null) {
            LOG.warning("JDBC URL not found in EntityManagerFactory properties; defaulting to H2 dialect");
            return false;
        }
        return url.startsWith("jdbc:postgresql");
    }
}
