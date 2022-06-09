package ill.eu.rtsptofmp4.business.streaming;

import ill.eu.rtsptofmp4.business.mp4frag.MP4Frag;
import ill.eu.rtsptofmp4.models.StreamInfo;
import ill.eu.rtsptofmp4.models.exceptions.StreamingException;
import io.quarkus.logging.Log;

import java.io.IOException;

public class RTSPWorker {

    private final StreamInfo streamInfo;
    private final String ffmpegPath;
    private final int initialisationTimeoutMs;

    private RTSPStreamClient client;
    private Process process;
    private MP4Frag mp4Frag;
    private Thread thread;

    public RTSPWorker(final StreamInfo streamInfo, final String ffmpegPath, final int initialisationTimeoutMs) {
        this.streamInfo = streamInfo;
        this.ffmpegPath = ffmpegPath;
        this.initialisationTimeoutMs = initialisationTimeoutMs;
    }

    public String getStreamId() {
        return this.streamInfo.getId();
    }

    public synchronized void start(RTSPStreamClient client) {
        if (this.thread == null) {
            this.client = client;

            this.mp4Frag = new MP4Frag();

            this.thread = new Thread(this::threadMain);
            this.thread.start();

        } else {
            Log.warnf("Cannot start an RTSP Worker that is already started: stream '%s'", this.streamInfo.getName());
        }
    }

    public synchronized void stop() {
        if (this.thread != null) {
            this.client = null;

            this.mp4Frag.stop();

            if (this.process != null) {
                Log.infof("Killing ffmpeg for RTSP stream '%s'", this.streamInfo.getName());
                this.process.destroy();
            }

            this.thread.interrupt();
            try {
                this.thread.join();

            } catch (InterruptedException e) {
                Log.errorf("Failed to join worker thread for stream '%s", this.streamInfo.getName());
            }

            this.mp4Frag = null;
            this.thread = null;
        }
    }

    public synchronized String getMime() throws StreamingException {
        if (this.mp4Frag != null) {
            return this.mp4Frag.getMime(this.initialisationTimeoutMs);

        } else {
            throw new StreamingException("ffmpeg process for RTSP stream '%s' does not exist", this.streamInfo.getName());
        }
    }

    public synchronized byte[] getInitialisation() throws StreamingException {
        if (this.mp4Frag != null) {
            return this.mp4Frag.getInitialisation(this.initialisationTimeoutMs);

        } else {
            throw new StreamingException("ffmpeg process for RTSP stream '%s' does not exist", this.streamInfo.getName());
        }
    }

    private synchronized void sendExitCode(int exitCode) {
        if (this.client != null) {
            this.client.onExit(exitCode);
        }
    }

    private void threadMain() {
        Log.infof("Starting ffmpeg for stream '%s'", this.streamInfo.getName());

        String[] params = {
                this.ffmpegPath,
                "-re",
                "-rtsp_transport",
                "tcp",
                "-i",
                this.streamInfo.getUrl(),
                "-reset_timestamps",
                "1",
                "-an",
                "-c:v",
                "copy",
                "-f",
                "mp4",
                "-movflags",
                "+frag_every_frame+empty_moov+default_base_moof",
                "pipe:1"
        };

        Log.infof("Spawning ffmpeg for RTSP stream '%s' at %s", this.streamInfo.getName(), this.streamInfo.getUrl());
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(params);

//        processBuilder.redirectInput(ProcessBuilder.Redirect.DISCARD);
//        processBuilder.redirectError(ProcessBuilder.Redirect.DISCARD);

        int exitCode = 0;
        try {
            this.process = processBuilder.start();
            this.mp4Frag.process(this.process.getInputStream(), this.process.getErrorStream());

            exitCode = this.process.waitFor();
            this.process = null;

        } catch (IOException e) {
            Log.errorf("IOException received while piping ffmpeg output: %s", e.getMessage());
            exitCode = 1;

        } catch (InterruptedException e) {
            Log.errorf("InterruptedException received while piping ffmpeg output: %s", e.getMessage());
            exitCode = 1;
        }

        if (!this.mp4Frag.stopped()) {
            this.sendExitCode(exitCode);
        }
    }
}
