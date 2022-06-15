package eu.ill.rtsptofmp4.models;

public class StreamInit {

    private final String clientId;
    private final String mime;
    private final byte[] initSegment;

    public StreamInit(final String clientId, final String mime, final byte[] initSegment) {
        this.clientId = clientId;
        this.mime = mime;
        this.initSegment = initSegment;
    }

    public String getClientId() {
        return clientId;
    }

    public String getMime() {
        return mime;
    }

    public byte[] getInitSegment() {
        return initSegment;
    }
}
