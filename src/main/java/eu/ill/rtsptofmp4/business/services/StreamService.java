package eu.ill.rtsptofmp4.business.services;

import eu.ill.rtsptofmp4.business.streaming.RTSPStreamManager;
import eu.ill.rtsptofmp4.business.streaming.RTSPWorker;
import eu.ill.rtsptofmp4.business.streaming.StreamPublisher;
import eu.ill.rtsptofmp4.business.streaming.StreamRelay;
import eu.ill.rtsptofmp4.models.StreamInfo;
import eu.ill.rtsptofmp4.models.StreamInit;
import eu.ill.rtsptofmp4.models.exceptions.StreamingException;
import io.quarkus.logging.Log;
import org.apache.commons.lang3.RandomStringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class StreamService {

    @Inject
    RTSPStreamManager rtspStreamManager;

    @Inject
    StreamPublisher streamPublisher;

    private final List<StreamRelay> streamRelays = new ArrayList<>();
    private final Map<String, StreamRelay> clients = new HashMap<>();

    public StreamService() {
    }

    @PostConstruct
    void init() {
        this.streamPublisher.start();
    }

    @PreDestroy
    void terminate() {
        this.streamPublisher.stop();
    }

    public List<StreamInfo> getAllStreamInfos() {
        return this.streamRelays.stream().map(StreamRelay::getInfo).collect(Collectors.toList());
    }

    public boolean hasClient(String clientId) {
        return this.clients.containsKey(clientId);
    }

    public StreamInit connect(StreamInfo streamInfo, String clientId) throws StreamingException {
        if (clientId != null && this.hasClient(clientId)) {
            throw new StreamingException("ClientId %s is already connected to a stream", clientId);
        }

        if (clientId == null) {
            clientId = this.generateClientId();
        }

        StreamRelay streamRelay;
        synchronized (this.streamRelays) {
            streamRelay = this.getStreamRelayByStreamInfo(streamInfo);
            if (streamRelay == null) {
                Log.infof("Creating new Stream Relay for stream '%s'", streamInfo.getName());

                // Get the RTSP Stream Worker
                RTSPWorker worker = this.rtspStreamManager.connectToStream(streamInfo);

                streamRelay = new StreamRelay(streamInfo, worker, this.streamPublisher, this::handleError);

                this.streamRelays.add(streamRelay);
            }
        }

        // Get the init data
        try {
            // Add the client to the stream relay (if first one it'll start ffmpeg) and wait for init data
            StreamInit streamInit = streamRelay.addClient(clientId);
            this.clients.put(clientId, streamRelay);

            return streamInit;

        } catch (StreamingException e) {
            this.disconnect(clientId);

            throw e;
        }
    }

    public void disconnect(String clientId) {
        synchronized (this.streamRelays) {
            StreamRelay streamRelay = this.getStreamRelayForClientId(clientId);
            if (streamRelay != null) {
                streamRelay.removeClient(clientId);

                if (!streamRelay.hasClients()) {
                    Log.infof("Removing Stream Relay for stream '%s'", streamRelay.getInfo().getName());
                    this.removeStreamRelay(streamRelay.getInfo());
                }

            } else {
                Log.debugf("Stream Relay for client '%s' does not exist", clientId);
            }
        }
    }

    private String generateClientId() {
        String uuid = RandomStringUtils.randomAlphanumeric(10);

        while (this.clients.containsKey(uuid)) {
            uuid = RandomStringUtils.randomAlphanumeric(10);
        }

        return uuid;
    }

    private StreamRelay getStreamRelayByStreamInfo(StreamInfo streamInfo) {
        synchronized (this.streamRelays) {
            return this.streamRelays.stream()
                    .filter(streamRelay -> streamRelay.getInfo().equals(streamInfo))
                    .findFirst()
                    .orElse(null);
        }
    }

    private StreamRelay getStreamRelayForClientId(String clientId) {
        return this.clients.get(clientId);
    }

    private void removeStreamRelay(StreamInfo streamInfo) {
        synchronized (this.streamRelays) {
            this.streamRelays.removeIf(streamRelay -> Objects.equals(streamRelay.getInfo(), streamInfo));
        }
    }

    private void handleError(StreamInfo streamInfo, String error) {
        Log.errorf("Removing stream '%s' due to errors: %s", streamInfo.getName(), error);

        synchronized (this.streamRelays) {
            StreamRelay streamRelay = this.getStreamRelayByStreamInfo(streamInfo);
            if (streamRelay != null) {
                streamRelay.stop();
                this.removeStreamRelay(streamInfo);
            }
        }
    }

}
