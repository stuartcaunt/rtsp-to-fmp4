package eu.ill.rtsptofmp4.business.mp4frag;

import java.nio.ByteBuffer;

public class CopyingStreamBuffer {

    private byte[] data = {};
    private final byte[] unsignedIntArray = {0, 0, 0, 0, 0, 0, 0, 0};

    public CopyingStreamBuffer() {

    }

    public CopyingStreamBuffer(byte[] data) {
        this.data = data;
    }

    public void addBuffer(byte[] data) {
        if (this.data == null) {
            this.data = data;

        } else {
            byte[] old = this.data;
            this.data = new byte[old.length + data.length];
            System.arraycopy(old, 0, this.data, 0, old.length);
            System.arraycopy(data, 0, this.data, old.length, data.length);
        }
    }

    public int length() {
        return this.data.length;
    }

    public long readUInt32BE(int index) {
        if (this.data.length < index + 4) {
            throw new IndexOutOfBoundsException("Cannot read uint32 from byte arrays of length " + data.length + " at index " + index);
        }
        System.arraycopy(this.data, index, this.unsignedIntArray, 4, 4);

        ByteBuffer byteBuffer = ByteBuffer.wrap(this.unsignedIntArray);
//        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        return byteBuffer.getLong();
    }

    public int indexOf(byte[] data) {
        if (this.data.length < data.length) {
            return -1;
        }

        int indexOf = 0;
        int streamBufferIndex = indexOf;
        int dataIndex = 0;

        while (dataIndex < data.length && streamBufferIndex < this.data.length) {
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
            return indexOf;

        } else {
            return -1;
        }
    }

    public CopyingStreamBuffer slice(int startIndex) {
        if (this.data.length <= startIndex) {
            throw new IndexOutOfBoundsException("Cannot perform slice from byte arrays of length " + data.length + " at index " + startIndex);
        }

        return this.slice(startIndex, this.data.length - startIndex);
    }

    public CopyingStreamBuffer slice(int startIndex, int length) {
        if (this.data.length < startIndex + length) {
            throw new IndexOutOfBoundsException("Cannot perform slice from byte arrays of length " + data.length + " at index " + startIndex + " of length " + length);
        }

        byte[] sliced = new byte[length];
        System.arraycopy(this.data, startIndex, sliced, 0, length);

        return new CopyingStreamBuffer(sliced);
    }
}
