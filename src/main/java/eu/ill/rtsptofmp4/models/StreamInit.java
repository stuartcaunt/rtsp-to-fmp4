package eu.ill.rtsptofmp4.models;

public class StreamInit {

    private final String mime;
    private final byte[] initSegment;

    public StreamInit(String mime, byte[] initSegment) {
        this.mime = mime;
        this.initSegment = initSegment;
    }

    public String getMime() {
        return mime;
    }

    public byte[] getInitSegment() {
        return initSegment;
    }
}
