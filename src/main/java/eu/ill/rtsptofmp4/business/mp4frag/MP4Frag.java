package eu.ill.rtsptofmp4.business.mp4frag;

import eu.ill.rtsptofmp4.models.exceptions.StreamingException;
import io.quarkus.logging.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

public class MP4Frag {

    private String mime;
    private byte[] initialisation;

    private final Object mimeCondition = new Object();
    private final Object initialisationCondition = new Object();

    private boolean stopped = false;

    public void process(Process process) throws IOException, InterruptedException {
//        BufferedReader reader = new BufferedReader(new InputStreamReader(stderr));
//
//        int counter = 0;
//        String line;
//        try {
//            while (!this.stopped && (line = reader.readLine()) != null) {
//                System.out.println(line);
//                counter++;
//                if (counter == 2) {
//                    this.setMime("mp4/doodaa");
//                } else if (counter == 3) {
//                    this.setInitialisation("hello".getBytes(StandardCharsets.UTF_8));
//                }
//            }
//
//        } catch (Exception e) {
//            if (!this.stopped) {
//                throw e;
//            }
//        }

        InputStream inputStream = process.getInputStream();
        while (process.isAlive()) {
            while (inputStream.available() > 0) {
                int available = inputStream.available();
                byte[] chunk = inputStream.readNBytes(available);
                this.processChunk(chunk);
            }

            Thread.sleep(20);
        }
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

            long startTimeMs = new Date().getTime();
            long currentTimeMs;
            long timeoutDeltaMs = 100;
            boolean waiting;
            do {
                try {
                    this.mimeCondition.wait(timeoutDeltaMs);

                    if (this.stopped) {
                        throw new StreamingException("MP4Frag stopped while waiting to receive mime");
                    }

                    if (this.mime != null) {
                        return this.mime;

                    } else {
                        // Check if timeout reached
                        currentTimeMs = new Date().getTime();
                        waiting = (currentTimeMs - startTimeMs) < timoutMs || timoutMs == 0;

                        if (!waiting) {
                            throw new StreamingException("MP4Frag timeout while waiting to receive mime");
                        }
                    }

                } catch (InterruptedException e) {
                    throw new StreamingException("MP4Frag interrupted while waiting to receive mime");
                }

            } while (waiting);

            throw new StreamingException("MP4Frag mime has not been set");
        }
    }

    public byte[] getInitialisation(int timoutMs) throws StreamingException {
        synchronized (initialisationCondition) {
            if (this.initialisation != null) {
                return this.initialisation;
            }

            long startTimeMs = new Date().getTime();
            long currentTimeMs;
            long timeoutDeltaMs = 100;
            boolean waiting;

            do {
                try {
                    this.initialisationCondition.wait(timeoutDeltaMs);

                    if (this.stopped) {
                        throw new StreamingException("MP4Frag stopped while waiting to receive mime");
                    }

                    if (this.initialisation != null) {
                        return this.initialisation;

                    } else {
                        // Check if timeout reached
                        currentTimeMs = new Date().getTime();
                        waiting = (currentTimeMs - startTimeMs) < timoutMs || timoutMs == 0;

                        if (!waiting) {
                            throw new StreamingException("MP4Frag timeout while waiting to receive initialisation segment");
                        }
                    }

                } catch (InterruptedException e) {
                    throw new StreamingException("MP4Frag interrupted while waiting to receive initialisation segment");
                }

            } while (waiting);
        }
        throw new StreamingException("MP4Frag initialisation has not been set");
    }

    private void processChunk(byte[] chunk) {
        Log.debugf("MP4Frag processing chunk of length %d", chunk.length);
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
