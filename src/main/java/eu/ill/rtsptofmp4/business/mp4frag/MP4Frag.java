package eu.ill.rtsptofmp4.business.mp4frag;

import eu.ill.rtsptofmp4.models.exceptions.MP4FragException;
import eu.ill.rtsptofmp4.models.exceptions.StreamingException;
import io.quarkus.logging.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class MP4Frag {

    private final static byte[] FTYP = {0x66, 0x74, 0x79, 0x70}; // ftyp
    private final static byte[] MOOV = {0x6d, 0x6f, 0x6f, 0x76}; // moov
    private final static byte[] MOOF = {0x6d, 0x6f, 0x6f, 0x66}; // moof
    private final static byte[] MDAT = {0x6d, 0x64, 0x61, 0x74}; // mdat
    private final static byte[] MFRA = {0x6d, 0x66, 0x72, 0x61}; // mfra
    private final static byte[] HVCC = {0x68, 0x76, 0x63, 0x43}; // hvcC
    private final static byte[] HEV1 = {0x68, 0x65, 0x76, 0x31}; // hev1
    private final static byte[] HVC1 = {0x68, 0x76, 0x63, 0x31}; // hvc1
    private final static byte[] AVCC = {0x61, 0x76, 0x63, 0x43}; // avcC
    private final static byte[] AVC1 = {0x61, 0x76, 0x63, 0x31}; // avc1
    private final static byte[] AVC2 = {0x61, 0x76, 0x63, 0x32}; // avc2
    private final static byte[] AVC3 = {0x61, 0x76, 0x63, 0x33}; // avc3
    private final static byte[] AVC4 = {0x61, 0x76, 0x63, 0x34}; // avc4
    private final static byte[] MP4A = {0x6d, 0x70, 0x34, 0x61}; // mp4a
    private final static byte[] ESDS = {0x65, 0x73, 0x64, 0x73}; // esds
    private final static int MOOF_SEARCH_LIMIT = 50; // number of allowed attempts to find missing moof atom

    private enum ProcessStage {
        FIND_FTYP,
        FIND_MOOV,
        FIND_MOOF,
        FIND_MDAT,
        MOOF_SEARCH,
    }

    private final SegmentListener segmentListener;

    private String mime;
    private byte[] initialization;

    private final Object mimeCondition = new Object();
    private final Object initializationCondition = new Object();

    private ProcessStage currentProcessStage = ProcessStage.FIND_FTYP;

    private int ftypLength = -1;
    private StreamBuffer ftyp;

    private int moofLength = -1;
    private StreamBuffer moof;
    private int moofBufferSize = - 1;
    private List<StreamBuffer> moofBuffer;
    private int moofSearches = -1;

    private int mdatLength = -1;
    private int mdatBufferSize = - 1;
    private List<StreamBuffer> mdatBuffer;

    private String videoCodec;
    private String audioCodec;

    private boolean stopped = false;

    public MP4Frag(final SegmentListener segmentListener) {
        this.segmentListener = segmentListener;
    }

    public void process(Process process) throws IOException, InterruptedException, MP4FragException {
        InputStream inputStream = process.getInputStream();
        while (process.isAlive()) {
            while (inputStream.available() > 0) {
                int available = inputStream.available();
                byte[] chunk = inputStream.readNBytes(available);
                Log.debugf("Received %d bytes from ffmpeg for processing", available);

                this.parseChunk(new StreamBuffer(chunk));
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

    public byte[] getInitialization(int timoutMs) throws StreamingException {
        synchronized (initializationCondition) {
            if (this.initialization != null) {
                return this.initialization;
            }

            long startTimeMs = new Date().getTime();
            long currentTimeMs;
            long timeoutDeltaMs = 100;
            boolean waiting;

            do {
                try {
                    this.initializationCondition.wait(timeoutDeltaMs);

                    if (this.stopped) {
                        throw new StreamingException("MP4Frag stopped while waiting to receive mime");
                    }

                    if (this.initialization != null) {
                        return this.initialization;

                    } else {
                        // Check if timeout reached
                        currentTimeMs = new Date().getTime();
                        waiting = (currentTimeMs - startTimeMs) < timoutMs || timoutMs == 0;

                        if (!waiting) {
                            throw new StreamingException("MP4Frag timeout while waiting to receive initialization segment");
                        }
                    }

                } catch (InterruptedException e) {
                    throw new StreamingException("MP4Frag interrupted while waiting to receive initialization segment");
                }

            } while (waiting);
        }
        throw new StreamingException("MP4Frag initialization has not been set");
    }

    public void parseChunk(StreamBuffer chunk) throws MP4FragException {
        if (this.currentProcessStage.equals(ProcessStage.FIND_FTYP)) {
            this.findFtyp(chunk);

        } else if (this.currentProcessStage.equals(ProcessStage.FIND_MOOV)) {
            this.findMoov(chunk);

        } else if (this.currentProcessStage.equals(ProcessStage.FIND_MOOF)) {
            this.findMoof(chunk);

        } else if (this.currentProcessStage.equals(ProcessStage.FIND_MDAT)) {
            this.findMdat(chunk);

        } else if (this.currentProcessStage.equals(ProcessStage.MOOF_SEARCH)) {
            this.moofSearch(chunk);
        }
    }

    private void findFtyp(StreamBuffer chunk) throws MP4FragException {
        long chunkLength = chunk.length();
        if (chunkLength < 8 || chunk.indexOf(FTYP) != 4) {
            throw new MP4FragException("FTYP %s not found", Arrays.toString(FTYP));
        }

        this.ftypLength = chunk.readUInt32BE(0);

        if (this.ftypLength < chunkLength) {
            this.ftyp = chunk.slice(0, this.ftypLength);
            this.currentProcessStage = ProcessStage.FIND_MOOV;
            this.parseChunk(chunk.slice(this.ftypLength));

        } else if (this.ftypLength == chunkLength) {
            this.ftyp = chunk;
            this.currentProcessStage = ProcessStage.FIND_MOOV;

        } else {
            //should not be possible to get here because ftyp is approximately 24 bytes
            //will have to buffer this chunk and wait for rest of it on next pass
            throw new MP4FragException("ftypLength: %d > chunkLength: %d", this.ftypLength, chunkLength);
        }
    }

    private void findMoov(StreamBuffer chunk) throws MP4FragException {
        int chunkLength = chunk.length();
        if (chunkLength < 8 || chunk.indexOf(MOOV) != 4) {
            throw new MP4FragException("MOOV %s not found", Arrays.toString(MOOV));
        }

        int moovLength = chunk.readUInt32BE(0);
        if (moovLength < chunkLength) {
            this.initialize(StreamBuffer.concat(this.ftyp, chunk, this.ftypLength + moovLength));
            this.ftyp = null;
            this.ftypLength = -1;
            this.currentProcessStage = ProcessStage.FIND_MOOF;

            this.parseChunk(chunk.slice(moovLength));

        } else if (moovLength == chunkLength) {
            this.initialize(StreamBuffer.concat(this.ftyp, chunk, this.ftypLength + moovLength));
            this.ftyp = null;
            this.ftypLength = -1;
            this.currentProcessStage = ProcessStage.FIND_MOOF;

        } else {
            //probably should not arrive here here because moov is typically < 800 bytes
            //will have to store chunk until size is big enough to have entire moov piece
            //ffmpeg may have crashed before it could output moov and got us here
            throw new MP4FragException("moovLength: %d > chunkLength: %d", moovLength, chunkLength);
        }
    }

    private void initialize(StreamBuffer chunk) throws MP4FragException {
        this.setInitialisation(chunk.getBytes());

//        int mdhdIndex = chunk.indexOf(MDHD);
//        byte mdhdVersion = chunk.get(mdhdIndex + 4);
//        int timescale = chunk.readUInt32BE(mdhdIndex + (mdhdVersion == 0 ? 16 : 24));

        List<String> codecs = new ArrayList<>();
        String mp4Type = "";

        if (this.parseCodecAVCC(chunk) || this.parseCodecHVCC(chunk)) {
            codecs.add(this.videoCodec);
            mp4Type = "video";
        }
        if (this.parseCodecMP4A(chunk)) {
            codecs.add(this.audioCodec);
            if (this.videoCodec == null) {
                mp4Type = "audio";
            }
        }
        if (codecs.size() == 0) {
            throw new MP4FragException("codecs not found.");
        }

        this.setMime(mp4Type + "/mp4; codecs=\"" + String.join(", ", codecs) + "\"");
    }

    private void moofSearch(StreamBuffer chunk) throws MP4FragException {
        if (this.moofSearches < MOOF_SEARCH_LIMIT) {
            this.moofSearches++;

            //console.warn(`MOOF search attempt number ${this._moofSearches}.`);
            int index = chunk.indexOf(MOOF);
            if (index > 3 && chunk.length() > index + 3) {
                this.moofSearches = -1;
                this.currentProcessStage = ProcessStage.FIND_MOOF;
                this.parseChunk(chunk.slice(index - 4));
            }
        } else {
            throw new MP4FragException("MOOF %s search failed after %d attempts.", Arrays.toString(MOOF), this.moofSearches);
        }
    }

    private void findMoof(StreamBuffer chunk) throws MP4FragException {
        if (this.moofBuffer != null) {
            this.moofBuffer.add(chunk);

            int chunkLength = chunk.length();
            this.moofBufferSize += chunkLength;
            if (this.moofLength == this.moofBufferSize) {
                //todo verify this works
                this.moof = StreamBuffer.concat(this.moofBuffer, this.moofLength);
                this.moofBuffer = null;
                this.moofBufferSize = -1;
                this.currentProcessStage = ProcessStage.FIND_MDAT;

            } else if (this.moofLength < this.moofBufferSize) {
                this.moof = StreamBuffer.concat(this.moofBuffer, this.moofLength);
                int sliceIndex = chunkLength - (this.moofBufferSize - this.moofLength);
                this.moofBuffer = null;
                this.moofBufferSize = -1;
                this.currentProcessStage = ProcessStage.FIND_MDAT;

                this.parseChunk(chunk.slice(sliceIndex));
            }

        } else {
            int chunkLength = chunk.length();
            if (chunkLength < 8 || chunk.indexOf(MOOF) != 4) {
                // ffmpeg occasionally pipes corrupt data, lets try to get back to normal if we can find next MOOF box before attempts run out
                int mfraIndex = chunk.indexOf(MFRA);
                if (mfraIndex != -1) {
                    // console.log(`MFRA was found at ${mfraIndex}. This is expected at the end of stream.`);
                    return;
                }
                // console.warn('Failed to find MOOF. Starting MOOF search. Ignore this if your file stream input has ended.');
                this.moofSearches = 0;
                this.currentProcessStage = ProcessStage.MOOF_SEARCH;

                this.parseChunk(chunk);
                return;
            }

            this.moofLength = chunk.readUInt32BE(0);
            if (this.moofLength == 0) {
                throw new MP4FragException("Bad data from input stream reports %s length of 0.", Arrays.toString(MOOF));
            }

            if (this.moofLength < chunkLength) {
                this.moof = chunk.slice(0, this.moofLength);
                this.currentProcessStage = ProcessStage.FIND_MDAT;
                this.parseChunk(chunk.slice(this.moofLength));

            } else if (this.moofLength == chunkLength) {
                // todo verify this works
                this.moof = chunk;
                this.currentProcessStage = ProcessStage.FIND_MDAT;

            } else {
                this.moofBuffer = new ArrayList<>(Arrays.asList(chunk));
                this.moofBufferSize = chunkLength;
            }
        }
    }

    private void findMdat(StreamBuffer chunk) throws MP4FragException {
        if (this.mdatBuffer != null) {
            this.mdatBuffer.add(chunk);
            int chunkLength = chunk.length();
            this.mdatBufferSize += chunkLength;
            if (this.mdatLength == this.mdatBufferSize) {
                this.setSegment(this.moof, this.mdatBuffer, this.moofLength + this.mdatLength);

                this.moof = null;
                this.mdatBuffer = null;
                this.mdatBufferSize = -1;
                this.mdatLength = -1;
                this.moofLength = -1;
                this.currentProcessStage = ProcessStage.FIND_MOOF;

            } else if (this.mdatLength < this.mdatBufferSize) {
                this.setSegment(this.moof, this.mdatBuffer, this.moofLength + this.mdatLength);

                int sliceIndex = chunkLength - (this.mdatBufferSize - this.mdatLength);
                this.moof = null;
                this.mdatBuffer = null;
                this.mdatBufferSize = -1;
                this.mdatLength = -1;
                this.moofLength = -1;
                this.currentProcessStage = ProcessStage.FIND_MOOF;

                this.parseChunk(chunk.slice(sliceIndex));
            }
        } else {
            int chunkLength = chunk.length();
            if (chunkLength < 8 || chunk.indexOf(MDAT) != 4) {
                throw new MP4FragException("MDAT %s not found", Arrays.toString(MDAT));
            }
            this.mdatLength = chunk.readUInt32BE(0);
            if (this.mdatLength > chunkLength) {
                this.mdatBuffer = new ArrayList<>();
                this.mdatBuffer.add(chunk);
                this.mdatBufferSize = chunkLength;

            } else if (this.mdatLength == chunkLength) {
                this.setSegment(this.moof, chunk, this.moofLength + chunkLength);
                this.moof = null;
                this.moofLength = -1;
                this.mdatLength = -1;

                this.currentProcessStage = ProcessStage.FIND_MOOF;

            } else {
                this.setSegment(this.moof, chunk, this.moofLength + this.mdatLength);
                int sliceIndex = this.mdatLength;
                this.moof = null;
                this.moofLength = -1;
                this.mdatLength = -1;
                this.currentProcessStage = ProcessStage.FIND_MOOF;

                this.parseChunk(chunk.slice(sliceIndex));
            }
        }
    }

    private void setSegment(StreamBuffer moof, StreamBuffer mdat, int segmentLength) {
        StreamBuffer segment = StreamBuffer.concat(moof, mdat, segmentLength);

        this.segmentListener.onSegment(segment);
    }

    private void setSegment(StreamBuffer moof, List<StreamBuffer> mdatBuffer, int segmentLength) {
        List<StreamBuffer> segmentBuffer = new ArrayList<>();
        segmentBuffer.add(moof);
        segmentBuffer.addAll(mdatBuffer);
        StreamBuffer segment = StreamBuffer.concat(segmentBuffer, segmentLength);

        this.segmentListener.onSegment(segment);
    }

    private boolean parseCodecMP4A(StreamBuffer chunk) {
        int index = chunk.indexOf(MP4A);
        if (index != -1) {
            List<String> codecs =  new ArrayList<>(Arrays.asList("mp4a"));
            int esdsIndex = chunk.indexOf(ESDS, index);
            // verify tags 3, 4, 5 to be in expected positions
            if (esdsIndex != -1 && chunk.get(esdsIndex + 8) == 0x03 && chunk.get(esdsIndex + 16) == 0x04 && chunk.get(esdsIndex + 34) == 0x05) {
                codecs.add(StreamBuffer.toHex(chunk.get(esdsIndex + 21)));
                codecs.add(Integer.toString(((chunk.get(esdsIndex + 39) & 0xf8) >> 3)));
                this.audioCodec = String.join(".", codecs);

                return true;
            }
            // console.warn('unexpected mp4a esds structure');
        }

        return false;
    }

    private boolean parseCodecAVCC(StreamBuffer chunk) {
        int index = chunk.indexOf(AVCC);
        if (index != -1) {
            List<String> codecs = new ArrayList<>();
            if (chunk.includes(AVC1)) {
                codecs.add("avc1");

            } else if (chunk.includes(AVC2)) {
                codecs.add("avc2");

            } else if (chunk.includes(AVC3)) {
                codecs.add("avc3");

            } else if (chunk.includes(AVC4)) {
                codecs.add("avc4");

            } else {
                return false;
            }

            codecs.add(chunk.slice(index + 5, index + 8).toString("hex").toUpperCase());

            this.videoCodec = String.join(".", codecs);

            return true;
        }

        return false;
    }

    private boolean parseCodecHVCC(StreamBuffer chunk) throws MP4FragException {
        int index = chunk.indexOf(HVCC);
        if (index != -1) {
            List<String> codecs = new ArrayList<>();
            if (chunk.includes(HVC1)) {
                codecs.add("hvc1");

            } else if (chunk.includes(HEV1)) {
                codecs.add("hev1");

            } else {
                return false;
            }

            byte tmpByte = chunk.get(index + 5);
            int generalProfileSpace = (tmpByte >> 6) & 0x03; // get 1st 2 bits (11000000)
            String generalTierFlag = ((tmpByte & 0x20) != 0) ? "H" : "L"; // get next bit (00100000)
            String generalProfileIdc = Integer.toString(tmpByte & 0x1f); // get last 5 bits (00011111)
            String generalProfileCompatibility = MP4Frag.reverseBitsToHex(chunk.readUInt32BE(index + 6));
            String generalConstraintIndicator = chunk.slice(index + 10, index + 16).filterNonZeroBytes().toString("hex");
            String generalLevelIdc = Byte.toString(chunk.get(index + 16));

            switch (generalProfileSpace) {
                case 0 -> codecs.add(generalProfileIdc);
                case 1 -> codecs.add("A" + generalProfileIdc);
                case 2 -> codecs.add("B" + generalProfileIdc);
                case 3 -> codecs.add("C" + generalProfileIdc);
            }
            codecs.add(generalProfileCompatibility);
            codecs.add(generalTierFlag + generalLevelIdc);
            if (generalConstraintIndicator.length() > 0) {
                codecs.add(generalConstraintIndicator);
            }

            this.videoCodec = String.join(".", codecs);

            return true;
        }
        return false;
    }

    private static String reverseBitsToHex(int n) {
        n = ((n >> 1) & 0x55555555) | ((n & 0x55555555) << 1);
        n = ((n >> 2) & 0x33333333) | ((n & 0x33333333) << 2);
        n = ((n >> 4) & 0x0f0f0f0f) | ((n & 0x0f0f0f0f) << 4);
        n = ((n >> 8) & 0x00ff00ff) | ((n & 0x00ff00ff) << 8);
        return Integer.toHexString ((n >> 16) | (n << 16));
    }

    private void setMime(String mime) {
        synchronized (mimeCondition) {
            this.mime = mime;
            this.mimeCondition.notify();
        }
    }

    private void setInitialisation(byte[] initialization) {
        synchronized (initializationCondition) {
            this.initialization = initialization;
            this.initializationCondition.notify();
        }
    }

    public interface SegmentListener {
        void onSegment(StreamBuffer segment);
    }
}
