package ill.eu.rtsptofmp4.business.streaming;

public interface RTSPStreamClient {
    void onSegment(byte[] segment);
    void onExit(int code);
}
