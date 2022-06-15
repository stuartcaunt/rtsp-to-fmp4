package eu.ill.rtsptofmp4;

import io.quarkus.runtime.annotations.StaticInitSafe;
import io.smallrye.config.ConfigMapping;

import java.util.List;

@StaticInitSafe
@ConfigMapping(prefix = "server")
public interface ServerConfig {
    String name();
    List<StreamConfig> streams();
    WorkerConfig worker();
    PublisherConfig publisher();

    interface StreamConfig {
        String id();
        String name();
        String url();
    }

    interface WorkerConfig {
        int intialisationTimoutMs();
        String ffmpegPath();
    }

    interface PublisherConfig {
        int port();
    }

}

