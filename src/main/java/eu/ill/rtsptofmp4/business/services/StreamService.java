package eu.ill.rtsptofmp4.business.services;

import eu.ill.rtsptofmp4.ServerConfig;
import eu.ill.rtsptofmp4.business.streaming.StreamPublisher;
import eu.ill.rtsptofmp4.business.streaming.RTSPStreamManager;
import eu.ill.rtsptofmp4.business.streaming.RTSPWorker;
import eu.ill.rtsptofmp4.business.streaming.StreamRelay;
import eu.ill.rtsptofmp4.models.StreamInfo;
import eu.ill.rtsptofmp4.models.StreamInit;
import eu.ill.rtsptofmp4.models.exceptions.StreamingException;
import io.quarkus.logging.Log;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class StreamService {

    @Inject
    ServerConfig serverConfig;

    @Inject
    RTSPStreamManager rtspStreamManager;

    @Inject
    StreamPublisher streamPublisher;

    private List<StreamInfo> streams;
    private final List<StreamRelay> streamRelays = new ArrayList<>();

    public StreamService() {
    }

    @PostConstruct
    void init() {
        this.streams = this.serverConfig.streams().stream().map(stream -> new StreamInfo(stream.id(), stream.name(), stream.url())).collect(Collectors.toList());

        this.streamPublisher.start();
    }

    @PreDestroy
    void terminate() {
        this.streamPublisher.stop();
    }

    public List<StreamInfo> getStreams() {
        return this.streams;
    }

    public StreamInfo getStream(String streamId) {
        Optional<StreamInfo> optionalStreamInfo = this.streams.stream().filter(stream -> Objects.equals(stream.getId(), streamId)).findFirst();
        return optionalStreamInfo.orElse(null);
    }

    public StreamInit connect(String streamId, String clientId) throws StreamingException {
        StreamInfo streamInfo = this.getStream(streamId);
        if (streamInfo == null) {
            throw new NoSuchElementException("Could not find stream details for stream with id " + streamId);
        }

        StreamRelay streamRelay;
        synchronized (this.streamRelays) {
            streamRelay = this.getStreamRelay(streamId);
            if (streamRelay == null) {
                Log.infof("Creating new Stream Relay for stream '%s'", streamInfo.getName());

                // Get the RTSP Stream Worker
                RTSPWorker worker = this.rtspStreamManager.connectToStream(streamInfo);

                streamRelay = new StreamRelay(streamInfo, worker, this.streamPublisher, this::handleError);

                this.streamRelays.add(streamRelay);
            }

            // Add the client to the stream relay (if first one it'll start ffmpeg)
            streamRelay.addClient(clientId);
        }

        // Get the init data
        try {
            return streamRelay.getInitData();

        } catch (StreamingException e) {
            throw e;
        }
    }

    public void disconnect(String streamId, String clientId) {
        StreamInfo streamInfo = this.getStream(streamId);
        if (streamInfo == null) {
            throw new NoSuchElementException("Could not find stream details for stream with id " + streamId);
        }

        synchronized (this.streamRelays) {
            StreamRelay streamRelay = this.getStreamRelay(streamId);
            if (streamRelay != null) {
                streamRelay.removeClient(clientId);

                if (!streamRelay.hasClients()) {
                    Log.infof("Removing Stream Relay for stream '%s'", streamInfo.getName());
                    this.removeStreamRelay(streamId);
                }

            } else {
                Log.debugf("Stream Relay for stream '%s' does not exist", streamId);
            }
        }
    }

    private StreamRelay getStreamRelay(String streamId) {
        Optional<StreamRelay> optionalStreamRelay = this.streamRelays.stream().filter(streamRelay -> Objects.equals(streamRelay.getStreamId(), streamId)).findFirst();
        return optionalStreamRelay.orElse(null);
    }

    private synchronized void removeStreamRelay(String streamId) {
        this.streamRelays.removeIf(streamRelay -> Objects.equals(streamRelay.getStreamId(), streamId));
    }

    private void handleError(StreamInfo streamInfo, String error) {
        Log.errorf("Removing stream '%s' due to errors: %s", streamInfo.getName(), error);

        synchronized (this.streamRelays) {
            StreamRelay streamRelay = this.getStreamRelay(streamInfo.getId());
            if (streamRelay != null) {
                streamRelay.stop();
                this.removeStreamRelay(streamInfo.getId());
            }
        }
    }

}
