import java.io.Serializable;

/**
 * Wrapper class for byte[].
 * Used for sending/receiving byte[] through socket via ObjectOutputStream and
 * ObjectInputStream
 */
public class ByteWrapper implements Serializable {

    private byte[] arr;

    public ByteWrapper(byte[] bytes) {
        arr = bytes;
    }

    public byte[] getBytes() {
        return arr;
    }
}
