package eu.ill.rtsptofmp4.business.streaming;

import eu.ill.rtsptofmp4.models.exceptions.StreamingException;
import eu.ill.rtsptofmp4.models.StreamInfo;
import eu.ill.rtsptofmp4.models.StreamInit;
import io.quarkus.logging.Log;

import java.util.HashSet;
import java.util.Set;

public class StreamRelay implements RTSPStreamClient {

    private final StreamInfo streamInfo;
    private final RTSPWorker rtspWorker;
    private final StreamPublisher streamPublisher;
    private final StreamErrorHandler errorHandler;

    private final Set<String> clientIds = new HashSet<>();

    public StreamRelay(final StreamInfo streamInfo, final RTSPWorker rtspWorker, final StreamPublisher streamPublisher, final StreamErrorHandler errorHandler) {
        this.streamInfo = streamInfo;
        this.rtspWorker = rtspWorker;
        this.streamPublisher = streamPublisher;
        this.errorHandler = errorHandler;
    }

    public String getId() {
        return this.streamInfo.getId();
    }

    public StreamInfo getInfo() {
        return this.streamInfo;
    }

    public StreamInit addClient(String clientId) throws StreamingException {
        if (!this.hasClient(clientId)) {
            this.clientIds.add(clientId);

            Log.infof("Client '%s' added to stream '%s'. %d client(s) are now attached.", clientId, this.streamInfo.getName(), this.clientIds.size());

            if (this.clientIds.size() == 1) {
                this.rtspWorker.start(this);
            }

        } else {
            Log.debugf("Client '%s' is already attached to stream '%s'", clientId, this.streamInfo.getName());
        }

        return this.getInitData(clientId);
    }

    public void removeClient(String clientId) {
        if (this.hasClient(clientId)) {
            this.clientIds.remove(clientId);

            Log.infof("Client '%s' removed from stream '%s'. %d client(s) are now attached.", clientId, this.streamInfo.getName(), this.clientIds.size());

            if (this.clientIds.size() == 0) {
                this.rtspWorker.stop();
            }

        } else {
            Log.debugf("Client '%s' is not attached to stream '%s'", clientId, this.streamInfo.getName());
        }
    }

    public boolean hasClients() {
        return this.clientIds.size() > 0;
    }

    public boolean hasClient(String clientId) {
        return this.clientIds.contains(clientId);
    }

    public void stop() {
        this.rtspWorker.stop();
        this.clientIds.clear();
    }

    public StreamInit getInitData(String clientId) throws StreamingException {

        try {
            String mime = this.rtspWorker.getMime();
            Log.debugf("Got mime '%s' from ffmpeg for stream '%s'", mime, this.streamInfo.getName());

            byte[] initialisation = this.rtspWorker.getInitialisation();
            Log.debugf("Got initialisation of length %d from ffmpeg for stream '%s'", initialisation.length, this.streamInfo.getName());

            return new StreamInit(clientId, mime, initialisation);

        } catch (StreamingException e) {
            throw new StreamingException("Failed to get stream init data: %s", e.getMessage());
        }
    }

    @Override
    public void onSegment(byte[] segment) {
        Log.debugf("Got segment of length %d from ffmpeg for stream '%s'", segment.length, this.streamInfo.getName());
        this.streamPublisher.publish(this.streamInfo.getId(), segment);
    }

    @Override
    public void onExit(int code) {
        if (code == 1) {
            this.errorHandler.handleError(this.streamInfo, "ffmpeg crashed for stream '" + this.streamInfo.getName() + "'");
        }
    }

    public interface StreamErrorHandler {
        void handleError(StreamInfo streamInfo, String error);
    }
}
