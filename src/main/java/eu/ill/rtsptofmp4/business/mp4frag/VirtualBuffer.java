package eu.ill.rtsptofmp4.business.mp4frag;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class VirtualBuffer {

    private final List<byte[]> byteArrays = new ArrayList<>();
    private final ByteBuffer uint32Storage = ByteBuffer.allocate(8).put(new byte[]{0, 0, 0, 0});
    private final byte[] fourByteArray = new byte[4];

    private int offset = 0;
    private int indexInFirstArray = 0;

    private int length = 0;

    public void addBuffer(byte[] byteArray) {
        this.byteArrays.add(byteArray);
        this.length += byteArray.length;
    }

    public long length() {
        return this.length - this.offset;
    }

    public int indexOf(byte[] data) {
        if (this.length < this.offset + data.length) {
            return -1;
        }

        // Get position of first byte
        int currentBufferIndex = this.offset;
        int currentArrayIndex = this.indexInFirstArray;
        int currentDataIndex = 0;

        byte[] byteArray = this.byteArrays.get(0);

        int counter = 0;
        while (counter < data.length && currentBufferIndex < this.length) {
            if (data[currentDataIndex] == byteArray[currentArrayIndex]) {
                currentDataIndex++;
                currentArrayIndex++;
                currentBufferIndex++;
                counter++;

            } else {
                currentDataIndex = 0;
                currentBufferIndex = currentBufferIndex - counter + 1;
                currentArrayIndex = currentArrayIndex - counter + 1;
                counter = 0;
            }
        }

        if (currentDataIndex == data.length) {
            return currentBufferIndex - data.length;
        } else {
            return -1;
        }

    }

    public long readUInt32(int userIndex) throws IndexOutOfBoundsException {
        if (this.length < this.offset + userIndex + 4) {
            throw new IndexOutOfBoundsException("Cannot read uint32 from byte arrays of length " + this.length + " at index " + userIndex + " with offset " + this.offset);
        }

        this.uint32Storage.position(4);
        this.uint32Storage.put(this.getFourBytes(this.offset + userIndex));
        this.uint32Storage.position(0);

        this.incOffset(4);

        return this.uint32Storage.getLong();
    }

    private byte[] getFourBytes(int internalIndex) {
        if (this.length < internalIndex + 4) {
            throw new IndexOutOfBoundsException("Cannot read 4 bytes from byte arrays of length " + this.length + " at index " + internalIndex);
        }

        // Get position of first byte
        int iArray = 0;
        int combinedArrayLengths = this.byteArrays.get(0).length;
        int previousCombinedArrayLengths = 0;
        while (combinedArrayLengths <= internalIndex) {
            iArray++;
            previousCombinedArrayLengths = combinedArrayLengths;
            combinedArrayLengths += this.byteArrays.get(iArray).length;
        }

        byte[] byteArray = this.byteArrays.get(iArray);
        int indexInArray = internalIndex - previousCombinedArrayLengths;

        for (int i = 0; i < 4; i++) {
            this.fourByteArray[i] = byteArray[indexInArray];

            if (i < 3) {
                indexInArray++;
                if (indexInArray >= byteArray.length) {
                    iArray++;
                    byteArray = this.byteArrays.get(iArray);
                    indexInArray = 0;
                }
            }
        }

        return this.fourByteArray;
    }

    private byte getByte(int index) {
        if (this.length < index) {
            throw new IndexOutOfBoundsException("Cannot read byte from byte arrays of length " + this.length + " at index " + index);
        }

        int arrayIndex = 0;
        int combinedArrayLengths = this.byteArrays.get(0).length;
        int indexAtArrayStart = 0;
        while (combinedArrayLengths <= index) {
            arrayIndex++;
            indexAtArrayStart = combinedArrayLengths;
            combinedArrayLengths += this.byteArrays.get(arrayIndex).length;
        }

        int indexInArray = index - indexAtArrayStart;
        return this.byteArrays.get(arrayIndex)[indexInArray];
    }

    private void incOffset(int length) {
        if (this.length < this.offset + length) {
            throw new IndexOutOfBoundsException("Cannot increase array offset by " + length + " virtual buffer has length " + this.length + " with offset " + this.offset);
        }

        this.offset += length;
        this.indexInFirstArray += length;
        this.cleanArrays();
    }

    private void cleanArrays() {
        while (this.offset > 0 && this.offset > this.byteArrays.get(0).length) {
            this.popFront();
        }
    }

    private void popFront() {
        if (this.byteArrays.size() > 0) {
            byte[] popped = this.byteArrays.remove(0);
            this.length -= popped.length;
            this.offset -= popped.length;
            this.indexInFirstArray -= popped.length;
        }
    }
}
