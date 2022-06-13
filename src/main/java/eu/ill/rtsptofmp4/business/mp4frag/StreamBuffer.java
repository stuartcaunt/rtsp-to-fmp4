package eu.ill.rtsptofmp4.business.mp4frag;

import eu.ill.rtsptofmp4.models.exceptions.MP4FragException;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class StreamBuffer {

    private static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);

    private byte[] data = {};
    private final byte[] unsignedIntArray = {0, 0, 0, 0, 0, 0, 0, 0};
    private int offset = 0;
    private int capacity = 0;
    private int limit = 0;

    public StreamBuffer(byte[] data) {
        this.data = data;
        this.offset = 0;
        this.capacity  = data.length;
        this.limit = data.length;
    }

    public StreamBuffer(byte[] data, int offset, int length) {
        this.data = data;
        this.offset = offset;
        this.capacity = length;
        this.limit = offset + length;
    }

    public int length() {
        return this.capacity;
    }

    public int readUInt32BE(int index) throws MP4FragException {
        if (this.limit < this.offset + index + 4) {
            throw new IndexOutOfBoundsException("Cannot read uint32 from StreamBuffer of length " + this.capacity + " at index " + index + " with offset " + this.offset);
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(this.data, this.offset + index, 4);
//        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        int value = byteBuffer.getInt();
        if (value < 0) {
            throw new MP4FragException("Overflow of unit32 value stored in int32");
        }
        return value;
    }

    public byte get(int index) {
        if (this.limit < this.offset + index) {
            throw new IndexOutOfBoundsException("Cannot read byte from StreamBuffer of length " + this.capacity + " at index " + index + " with offset " + this.offset);
        }

        return this.data[this.offset + index];
    }

    public byte[] getBytes() {
        byte[] bytes = new byte[this.capacity];
        System.arraycopy(this.data, this.offset, bytes, 0, this.capacity);

        return bytes;
    }

    public int indexOf(byte[] data) {
        return this.indexOf(data, 0);
    }

    public int indexOf(byte[] data, int byteOffset) {
        if (this.limit < this.offset + byteOffset) {
            return -1;
        }
        if (this.limit < this.offset + data.length) {
            return -1;
        }

        int indexOf = this.offset + byteOffset;
        int streamBufferIndex = indexOf;
        int dataIndex = 0;

        while (dataIndex < data.length && streamBufferIndex < this.limit) {
            if (data[dataIndex] == this.data[streamBufferIndex]) {
                dataIndex++;
                streamBufferIndex++;

            } else {
                dataIndex = 0;
                indexOf++;
                streamBufferIndex = indexOf;
            }
        }

        if (dataIndex == data.length) {
            return indexOf - this.offset;

        } else {
            return -1;
        }
    }

    public boolean includes(byte[] data) {
        return this.indexOf(data) != -1;
    }

    public StreamBuffer slice(int startIndex) {
        return this.slice(startIndex, this.limit);
    }

    public StreamBuffer slice(int startIndex, int endIndex) {
        if (this.capacity <= startIndex) {
            throw new IndexOutOfBoundsException("Cannot perform slice from Stream Buffer of length " + this.capacity + " at index " + startIndex);
        }
        if (this.limit < endIndex) {
            throw new IndexOutOfBoundsException("Cannot perform slice from Stream Buffer of length " + this.capacity + " from index " + startIndex + " to index " + endIndex);
        }

        return new StreamBuffer(this.data, this.offset + startIndex, endIndex - startIndex);
    }

    public StreamBuffer filterNonZeroBytes() {
        int nonZeroByteCount = 0;
        for (int i = this.offset; i < this.limit; i++) {
            if (this.data[i] != 0) {
                nonZeroByteCount++;
            }
        }

        byte[] filtered = new byte[nonZeroByteCount];
        for (int i = this.offset, j = 0; i < this.limit; i++) {
            if (this.data[i] != 0) {
                filtered[j++] = this.data[i];
            }
        }

        return new StreamBuffer(filtered);
    }

    public static StreamBuffer concat(StreamBuffer[] buffers) {
        return StreamBuffer.concat(buffers, -1);
    }

    public static StreamBuffer concat(StreamBuffer[] buffers, int bufferLength) {
        int totalLength = 0;
        for (StreamBuffer buffer : buffers) {
            totalLength += buffer.capacity;
        }

        byte[] concat = new byte[totalLength];
        int concatPosition = 0;
        for (StreamBuffer buffer : buffers) {
            System.arraycopy(buffer.data, buffer.offset, concat, concatPosition, buffer.capacity);
            concatPosition += buffer.capacity;
        }

        return new StreamBuffer(concat, 0, bufferLength == -1 ? totalLength : bufferLength);
    }

    public String toString(String format) {
        if (format.equals("hex")) {
            return this.toHex();
        }

        return new String(this.data, this.offset, this.capacity, StandardCharsets.UTF_8);
    }

    private String toHex() {
        byte[] hexChars = new byte[this.capacity * 2];
        for (int j = 0; j < this.capacity; j++) {
            int index = j + this.offset;
            int v = this.data[index] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars, StandardCharsets.UTF_8);
    }

    public static String toHex(byte value) {
        byte[] hexChars = new byte[2];
        hexChars[0] = HEX_ARRAY[value >>> 4];
        hexChars[1] = HEX_ARRAY[value & 0x0F];
        return new String(hexChars, StandardCharsets.UTF_8);
    }

}
