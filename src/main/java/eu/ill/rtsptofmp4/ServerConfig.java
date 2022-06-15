package eu.ill.rtsptofmp4;

import io.quarkus.runtime.annotations.StaticInitSafe;
import io.smallrye.config.ConfigMapping;

@StaticInitSafe
@ConfigMapping(prefix = "server")
public interface ServerConfig {
    WorkerConfig worker();
    PublisherConfig publisher();

    interface WorkerConfig {
        int intialisationTimoutMs();
        String ffmpegPath();
    }

    interface PublisherConfig {
        int port();
    }

}

