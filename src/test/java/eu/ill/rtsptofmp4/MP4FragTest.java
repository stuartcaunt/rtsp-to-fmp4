package eu.ill.rtsptofmp4;

import eu.ill.rtsptofmp4.business.mp4frag.MP4Frag;
import eu.ill.rtsptofmp4.business.mp4frag.StreamBuffer;
import eu.ill.rtsptofmp4.models.exceptions.MP4FragException;
import eu.ill.rtsptofmp4.models.exceptions.StreamingException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Base64;

public class MP4FragTest {


    @Test
    public void testStreamBuffer() throws MP4FragException {
        byte[] mainBuffer = new byte[]{0, 0, 0, 1, 0 ,0, 0, 2, 0, 0, 0, 3, 0 ,0 ,0, 4, 0, 0, 0, 5, 0, 0, 0, 6};
        StreamBuffer streamBuffer = new StreamBuffer(mainBuffer);

        int one = streamBuffer.readUInt32BE(0);
        int two = streamBuffer.readUInt32BE(4);
        int three = streamBuffer.readUInt32BE(8);
        int four = streamBuffer.readUInt32BE(12);

        Assertions.assertEquals(1L, one);
        Assertions.assertEquals(2L, two);
        Assertions.assertEquals(3L, three);
        Assertions.assertEquals(4L, four);
    }

    @Test
    public void testStreamBufferIndexOf() throws MP4FragException {
        byte[] mainBuffer = new byte[]{0, 0, 0, 1, 0 ,0, 0, 2, 0, 0, 0, 3, 0 ,0 ,0, 4, 0, 0, 0, 5, 0, 0, 0, 6};
        StreamBuffer streamBuffer = new StreamBuffer(mainBuffer);

        int one = streamBuffer.readUInt32BE(0);
        Assertions.assertEquals(1L, one);

        byte[] testBuffer = new byte[]{0, 0, 0, 5};
        int indexOf = streamBuffer.indexOf(testBuffer);
        Assertions.assertEquals(16, indexOf);

        int five = streamBuffer.readUInt32BE(indexOf);

        Assertions.assertEquals(5L, five);
    }

    @Test
    public void testPartStreamBufferIndexOf() throws MP4FragException {
        byte[] mainBuffer = new byte[]{0, 0, 0, 1, 0 ,0, 0, 2, 0, 0, 0, 3, 0 ,0 ,0, 4, 0, 0, 0, 5, 0, 0, 0, 6};
        StreamBuffer streamBuffer = new StreamBuffer(mainBuffer, 4, 16);

        int two = streamBuffer.readUInt32BE(0);
        Assertions.assertEquals(2L, two);

        byte[] testBuffer = new byte[]{0, 0, 0, 5};
        int indexOf = streamBuffer.indexOf(testBuffer);
        Assertions.assertEquals(12, indexOf);

        int five = streamBuffer.readUInt32BE(indexOf);

        Assertions.assertEquals(5L, five);
    }

    @Test
    public void testPartStreamBufferNoIndexOf() throws MP4FragException {
        byte[] mainBuffer = new byte[]{0, 0, 0, 1, 0 ,0, 0, 2, 0, 0, 0, 3, 0 ,0 ,0, 4, 0, 0, 0, 5, 0, 0, 0, 6};
        StreamBuffer streamBuffer = new StreamBuffer(mainBuffer, 4, 16);

        int two = streamBuffer.readUInt32BE(0);
        Assertions.assertEquals(2L, two);

        byte[] testBuffer = new byte[]{0, 0, 0, 5, 0, 0, 0, 6};

        int indexOf = streamBuffer.indexOf(testBuffer);
        Assertions.assertEquals(-1, indexOf);
    }

    @Test
    public void testParseCodecAVCC() throws MP4FragException, StreamingException {
        MP4Frag mp4Frag = new MP4Frag((segment) -> {});

        String ftypBase64 = "AAAAHGZ0eXBpc281AAACAGlzbzVpc282bXA0MQ==";
        StreamBuffer ftypeChunk = new StreamBuffer(Base64.getDecoder().decode(ftypBase64));
        mp4Frag.parseChunk(ftypeChunk);

        String initializationBase64 = "AAADJW1vb3YAAABsbXZoZAAAAAAAAAAAAAAAAAAAA+gAAAAAAAEAAAEAAAAAAAAAAAAAAAABAAAAAAAAAAAAAAAAAAAAAQAAAAAAAAAAAAAAAAAAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAIAAAIJdHJhawAAAFx0a2hkAAAAAwAAAAAAAAAAAAAAAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABAAAAAAAAAAAAAAAAAAAAAQAAAAAAAAAAAAAAAAAAQAAAAAPAAAACHAAAAAABpW1kaWEAAAAgbWRoZAAAAAAAAAAAAAAAAAABX5AAAAAAVcQAAAAAAC1oZGxyAAAAAAAAAAB2aWRlAAAAAAAAAAAAAAAAVmlkZW9IYW5kbGVyAAAAAVBtaW5mAAAAFHZtaGQAAAABAAAAAAAAAAAAAAAkZGluZgAAABxkcmVmAAAAAAAAAAEAAAAMdXJsIAAAAAEAAAEQc3RibAAAAMRzdHNkAAAAAAAAAAEAAAC0YXZjMQAAAAAAAAABAAAAAAAAAAAAAAAAAAAAAAPAAhwASAAAAEgAAAAAAAAAAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABj//wAAADthdmNDAWQAH//hAB5nZAAfrNlA8BF+8BagICAoAAADAAgAAAMBgHjBjLABAAZo6+C0siz9+PgAAAAAE2NvbHJuY2x4AAEAAQABAAAAABBwYXNwAAAAAQAAAAEAAAAQc3R0cwAAAAAAAAAAAAAAEHN0c2MAAAAAAAAAAAAAABRzdHN6AAAAAAAAAAAAAAAAAAAAEHN0Y28AAAAAAAAAAAAAAChtdmV4AAAAIHRyZXgAAAAAAAAAAQAAAAEAAAAAAAAAAAAAAAAAAACAdWR0YQAAAHhtZXRhAAAAAAAAACFoZGxyAAAAAAAAAABtZGlyYXBwbAAAAAAAAAAAAAAAAEtpbHN0AAAAHqluYW0AAAAWZGF0YQAAAAEAAAAAU3RyZWFtAAAAJal0b28AAAAdZGF0YQAAAAEAAAAATGF2ZjU5LjE2LjEwMA==";
        StreamBuffer initializationChunk = new StreamBuffer(Base64.getDecoder().decode(initializationBase64));
        mp4Frag.parseChunk(initializationChunk);

        String mime = mp4Frag.getMime(0);
        Assertions.assertNotNull(mime);
        Assertions.assertEquals("video/mp4; codecs=\"avc1.64001F\"", mime);
    }

    @Test
    public void testParseCodecHVCC() throws MP4FragException, StreamingException {
        MP4Frag mp4Frag = new MP4Frag((segment) -> {});

        String ftypBase64 = "AAAAHGZ0eXBpc281AAACAGlzbzVpc282bXA0MQ==";
        StreamBuffer ftypeChunk = new StreamBuffer(Base64.getDecoder().decode(ftypBase64));
        mp4Frag.parseChunk(ftypeChunk);

        String initializationBase64 = "AAAFRW1vb3YAAABsbXZoZAAAAAAAAAAAAAAAAAAAA+gAAAAAAAEAAAEAAAAAAAAAAAAAAAABAAAAAAAAAAAAAAAAAAAAAQAAAAAAAAAAAAAAAAAAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAIAAAJKdHJhawAAAFx0a2hkAAAAAwAAAAAAAAAAAAAAAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABAAAAAAAAAAAAAAAAAAAAAQAAAAAAAAAAAAAAAAAAQAAAAAPAAAACHAAAAAAB5m1kaWEAAAAgbWRoZAAAAAAAAAAAAAAAAAABX5AAAAAAVcQAAAAAAC1oZGxyAAAAAAAAAAB2aWRlAAAAAAAAAAAAAAAAVmlkZW9IYW5kbGVyAAAAAZFtaW5mAAAAFHZtaGQAAAABAAAAAAAAAAAAAAAkZGluZgAAABxkcmVmAAAAAAAAAAEAAAAMdXJsIAAAAAEAAAFRc3RibAAAAQVzdHNkAAAAAAAAAAEAAAD1aGV2MQAAAAAAAAABAAAAAAAAAAAAAAAAAAAAAAPAAhwASAAAAEgAAAAAAAAAAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABj//wAAAHxodmNDAQFgAAAAkAAAAAAAWvAA/P34+AAADwMgAAEAGEABDAH//wFgAAADAJAAAAMAAAMAWpWYCSEAAQAvQgEBAWAAAAMAkAAAAwAAAwBaoAeCAIh95ZWaSTK8BagICA8IAAADAAgAAAMAwEAiAAEAB0QBwXK0YkAAAAATY29scm5jbHgAAQABAAEAAAAAEHBhc3AAAAABAAAAAQAAABBzdHRzAAAAAAAAAAAAAAAQc3RzYwAAAAAAAAAAAAAAFHN0c3oAAAAAAAAAAAAAAAAAAAAQc3RjbwAAAAAAAAAAAAABv3RyYWsAAABcdGtoZAAAAAMAAAAAAAAAAAAAAAIAAAAAAAAAAAAAAAAAAAAAAAAAAQEAAAAAAQAAAAAAAAAAAAAAAAAAAAEAAAAAAAAAAAAAAAAAAEAAAAAAAAAAAAAAAAAAAVttZGlhAAAAIG1kaGQAAAAAAAAAAAAAAAAAALuAAAAAAFXEAAAAAAAtaGRscgAAAAAAAAAAc291bgAAAAAAAAAAAAAAAFNvdW5kSGFuZGxlcgAAAAEGbWluZgAAABBzbWhkAAAAAAAAAAAAAAAkZGluZgAAABxkcmVmAAAAAAAAAAEAAAAMdXJsIAAAAAEAAADKc3RibAAAAH5zdHNkAAAAAAAAAAEAAABubXA0YQAAAAAAAAABAAAAAAAAAAAAAgAQAAAAALuAAAAAAAA2ZXNkcwAAAAADgICAJQACAASAgIAXQBUAAAAAAfQAAAH0AAWAgIAFEZBW5QAGgICAAQIAAAAUYnRydAAAAAAAAfQAAAH0AAAAABBzdHRzAAAAAAAAAAAAAAAQc3RzYwAAAAAAAAAAAAAAFHN0c3oAAAAAAAAAAAAAAAAAAAAQc3RjbwAAAAAAAAAAAAAASG12ZXgAAAAgdHJleAAAAAAAAAABAAAAAQAAAAAAAAAAAAAAAAAAACB0cmV4AAAAAAAAAAIAAAABAAAAAAAAAAAAAAAAAAAAgHVkdGEAAAB4bWV0YQAAAAAAAAAhaGRscgAAAAAAAAAAbWRpcmFwcGwAAAAAAAAAAAAAAABLaWxzdAAAAB6pbmFtAAAAFmRhdGEAAAABAAAAAFN0cmVhbQAAACWpdG9vAAAAHWRhdGEAAAABAAAAAExhdmY1OS4xNi4xMDA=";
        StreamBuffer initializationChunk = new StreamBuffer(Base64.getDecoder().decode(initializationBase64));
        mp4Frag.parseChunk(initializationChunk);

        String mime = mp4Frag.getMime(0);
        Assertions.assertNotNull(mime);
        Assertions.assertEquals("video/mp4; codecs=\"hev1.1.6.L90.90, mp4a.40.2\"", mime);
    }
}
