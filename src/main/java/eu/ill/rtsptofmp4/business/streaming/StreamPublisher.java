package eu.ill.rtsptofmp4.business.streaming;

import eu.ill.rtsptofmp4.ServerConfig;
import io.quarkus.logging.Log;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.concurrent.LinkedBlockingDeque;

@ApplicationScoped
public class StreamPublisher {

    @Inject
    ServerConfig serverConfig;

    private final LinkedBlockingDeque<StreamMessage> messageQueue = new LinkedBlockingDeque<>();
    private ZMQ.Socket socket;

    private boolean running;
    private Thread thread;


    public StreamPublisher() {

    }

    public synchronized void start() {
        if (this.thread == null) {
            this.running = true;
            this.thread = new Thread(this::mainLoop);
            this.thread.start();
        }

        this.connect();
    }

    public synchronized void stop() {
        this.disconnect();

        try {
            this.running = false;
            if (this.thread != null) {
                this.thread.interrupt();
                this.thread.join();
                this.thread = null;
            }

        } catch (InterruptedException e) {
            Log.errorf("Stop of Stream Publisher thread interrupted: %s", e.getMessage());
        }
    }

    public void publish(String streamId, byte[] segment) {
        try {
            this.messageQueue.put(new StreamMessage(streamId, segment));

        } catch (InterruptedException exception) {
            Log.error("Interrupted when adding message to publisher message queue");
        }
    }

    private void connect() {
        if (this.socket == null) {
            ZContext context = new ZContext();
            this.socket = context.createSocket(SocketType.PUB);
            this.socket.setLinger(0);

            String address = "tcp://*:" + this.serverConfig.publisher().port();

            try {
                this.socket.bind(address);
                Log.debugf("Stream Publisher bound to %s", address);

            } catch (Exception e) {
                Log.errorf("Failed to bind Stream Publisher socket to %s: %s", address, e.getMessage());
            }
        }
    }

    private void disconnect() {
        if (this.socket != null) {
            this.socket.close();
            this.socket = null;
            Log.debug("Steam Publisher socket closed");
        }

    }

    private void mainLoop() {
        while (this.running) {
            try {
                StreamMessage message = this.messageQueue.take();
                Log.tracef("Sending segment data of length %d for stream %s", message.getData().length, message.getStreamId());

                // Create multipart message
                ZMsg zMessage = new ZMsg();
                zMessage.add(message.getStreamId());
                zMessage.add(message.getData());

                zMessage.send(this.socket);

            } catch (InterruptedException exception) {
                if (this.running) {
                    Log.info("Publisher message sender thread interrupted");
                }
            }
        }

    }

    private static final class StreamMessage {
        private final String streamId;
        private final byte[] data;

        public StreamMessage(final String streamId, final byte[] data) {
            this.streamId = streamId;
            this.data = data;
        }

        public String getStreamId() {
            return streamId;
        }

        public byte[] getData() {
            return data;
        }
    }

}
