package ill.eu.rtsptofmp4.business.mp4frag;

import ill.eu.rtsptofmp4.models.exceptions.StreamingException;
import io.quarkus.logging.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class MP4Frag {

    private String mime;
    private byte[] initialisation;

    private final Object mimeCondition = new Object();
    private final Object initialisationCondition = new Object();

    private boolean stopped = false;

    public void process(InputStream stdout, InputStream stderr) throws IOException, InterruptedException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stderr));

        int counter = 0;
        String line;
        try {
            while (!this.stopped && (line = reader.readLine()) != null) {
                System.out.println(line);
                counter++;
                if (counter == 2) {
                    this.setMime("mp4/doodaa");
                } else if (counter == 3) {
                    this.setInitialisation("hello".getBytes(StandardCharsets.UTF_8));
                }
            }

        } catch (Exception e) {
            if (!this.stopped) {
                throw e;
            }
        }

//        InputStream inputStream = process.getInputStream();
//        while (process.isAlive()) {
//            while (inputStream.available() > 0) {
//                int available = inputStream.available();
//                byte[] chunk = inputStream.readNBytes(available);
//                this.processChunk(chunk);
//            }
//
//            Thread.sleep(20);
//        }
    }

    public void stop() {
        this.stopped = true;
    }

    public boolean stopped() {
        return this.stopped;
    }

    public String getMime(int timoutMs) throws StreamingException {
        synchronized (mimeCondition) {
            if (this.mime != null) {
                return this.mime;
            }

            try {
                this.mimeCondition.wait(timoutMs);
                if (this.mime != null) {
                    return this.mime;

                } else {
                    throw new StreamingException("Timeout while waiting to receive mime");
                }

            } catch (InterruptedException e) {
                throw new StreamingException("Interrupted while waiting to receive mime");
            }
        }
    }

    public byte[] getInitialisation(int timoutMs) throws StreamingException {
        synchronized (initialisationCondition) {
            if (this.initialisation != null) {
                return this.initialisation;
            }

            try {
                this.initialisationCondition.wait(timoutMs);
                if (this.initialisation != null) {
                    return this.initialisation;

                } else {
                    throw new StreamingException("Timeout while waiting to receive initialisation segment");
                }

            } catch (InterruptedException e) {
                throw new StreamingException("Interrupted while waiting to receive initialisation segment");
            }
        }
    }

    private void processChunk(byte[] chunk) {
        Log.debugf("Processing chunk of length %d", chunk.length);
    }


    private void setMime(String mime) {
        synchronized (mimeCondition) {
            this.mime = mime;
            this.mimeCondition.notify();
        }
    }

    private void setInitialisation(byte[] initialisation) {
        synchronized (initialisationCondition) {
            this.initialisation = initialisation;
            this.initialisationCondition.notify();
        }
    }
}
