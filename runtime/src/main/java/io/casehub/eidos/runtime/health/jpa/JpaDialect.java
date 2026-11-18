package io.casehub.eidos.runtime.health.jpa;

import jakarta.persistence.EntityManager;

final class JpaDialect {

    private JpaDialect() {}

    static boolean isPostgres(EntityManager em) {
        var props = em.getEntityManagerFactory().getProperties();
        var url = (String) props.get("jakarta.persistence.jdbc.url");
        if (url == null) {
            url = (String) props.get("javax.persistence.jdbc.url");
        }
        return url != null && url.startsWith("jdbc:postgresql");
    }
}
