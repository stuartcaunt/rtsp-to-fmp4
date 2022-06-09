package ill.eu.rtsptofmp4.business.streaming;

import ill.eu.rtsptofmp4.ServerConfig;
import ill.eu.rtsptofmp4.models.StreamInfo;
import io.quarkus.logging.Log;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@ApplicationScoped
public class RTSPStreamManager {

    @Inject
    ServerConfig serverConfig;

    private final List<RTSPWorker> rtspWorkers = new ArrayList<>();

    public RTSPWorker connectToStream(StreamInfo streamInfo) {
        // Check if worker exists
        RTSPWorker worker = this.getWorker(streamInfo);
        if (worker == null) {
            worker = new RTSPWorker(streamInfo, this.serverConfig.worker().ffmpegPath(), this.serverConfig.worker().intialisationTimoutMs());
            Log.infof("Created new RTSP Worker for stream '%s'", streamInfo.getName());
            this.rtspWorkers.add(worker);
        }

        return worker;
    }

    private RTSPWorker getWorker(StreamInfo streamInfo) {
        Optional<RTSPWorker> optionalRTSPWorker = this.rtspWorkers.stream().filter(worker -> Objects.equals(worker.getStreamId(), streamInfo.getId())).findFirst();
        return optionalRTSPWorker.orElse(null);
    }
}
