package io.casehub.eidos.deployment;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "casehub.eidos")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface EidosBuildTimeConfig {

    /** Reactive service tier configuration. */
    ReactiveConfig reactive();

    interface ReactiveConfig {
        /**
         * Whether to activate the reactive service tier.
         *
         * Set to {@code true} in deployments providing a reactive datasource
         * (Hibernate Reactive + reactive PostgreSQL client). JDBC-only consumers
         * must leave this unset — default {@code false} excludes all reactive beans.
         */
        @WithDefault("false")
        boolean enabled();
    }
}
