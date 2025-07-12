import java.security.Key;
import java.security.MessageDigest;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;

public class Message {

    private byte[] plainText;
    private byte[] cipherText;
    private Key key;
    private Cipher cipher;
    private byte[] iv;
    private boolean isReceived;
    private static final int BLOCK_LENGTH = 16;
    private static final int HASH_LENGTH = 20;

    public Message() {
        key = null;
        cipherText = null;
        plainText = null;
        cipher = null;
    }

    /**
     * Constructs a Message object, which stores the message's plaintext and
     * ciphertext for use in encrypted messaging.
     * 
     * @param text      the message as a byte array
     * @param k         the encryption/decryption key
     * @param initVect  the initialization vector (iv)
     * @param encrypted if true, then the byte array being fed to the constructor is
     *                  an encrypted message, and the constructor will decrypt it.
     *                  if false, the byte array is unencrypted and the constructor
     *                  will decrypt it.
     */
    public Message(byte[] text, Key k, Cipher c, byte[] initVect, boolean encrypted) {

        this.isReceived = encrypted;
        this.key = k;
        this.cipher = c;
        this.iv = initVect;

        /*
         * This branch stores the given encrypted message in cipherText, decrypts the
         * message, and stores the decrypted message in plainText
         */
        if (encrypted) {
            cipherText = text;

            try {
                cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));

                if (!compareHash(sha1(unappendHash(cipherText)), getAppendedHash(cipherText))) {
                    throw new Exception("ALERT: This message has been modified.");
                }
                byte[] cipherTextWithHashUnAppended = unappendHash(cipherText);
                plainText = cipher.doFinal(cipherTextWithHashUnAppended); // Decryption happens here.
            } catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        }
        /*
         * This branch stores the given unencrypted message in plainText, encrypts it,
         * and stores the encrypted message in cipherText
         */
        else {
            plainText = text;

            try {
                cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));

                cipherText = cipher.doFinal(plainText); // Encryption happens here.
                cipherText = appendHash(cipherText); // Hash is appended to cipherText, completing
                                                     // Encrypt-then-Authenticate scheme

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * Used for creating the initialization vector for the message immediately
     * following this one.
     * 
     * @return byte array of length 16 - the last block of this message's cipherText
     */
    public byte[] getLastBlock() {
        byte[] lastBlock = new byte[BLOCK_LENGTH];
        byte[] withoutHash = unappendHash(cipherText);
        for (int i = 0; i < BLOCK_LENGTH; i++) {
            lastBlock[i] = withoutHash[withoutHash.length + (i - BLOCK_LENGTH)];
        }
        return lastBlock;
    }

    /**
     * Takes a byte array as input, hashes it using sha1, and returns a new array
     * containing the original text with the hash appended to the end of it.
     * 
     * @param text byte array that will have its sha1 hash appended to it
     * @return new byte array composed of the original text and its hash appended to
     *         the end.
     */
    public static byte[] appendHash(byte[] text) {
        byte[] hash = sha1(text);
        byte[] appended = new byte[text.length + hash.length];
        for (int i = 0; i < text.length; i++) {
            appended[i] = text[i];
        }
        for (int i = text.length; i < appended.length; i++) {
            appended[i] = hash[i - text.length];
        }
        return appended;
    }

    /**
     * Takes a byte array composed of a message text and its hash appended to the
     * end, and unappends the hash
     * 
     * @param text byte array of text with hash appended
     * @return byte array containing original text without hash appended
     */
    public static byte[] unappendHash(byte[] text) {
        byte[] original = new byte[text.length - HASH_LENGTH];
        for (int i = 0; i < original.length; i++) {
            original[i] = text[i];
        }
        return original;
    }

    /**
     * Given a byte array composed of message text and its hash appended to the end,
     * returns only the hash appended to the end
     * 
     * @param text byte array containing message text with hash appended to the end
     * @return byte array of the hash appended to the end of the given text
     */
    public static byte[] getAppendedHash(byte[] text) {
        byte[] hash = new byte[HASH_LENGTH];
        int msgLength = text.length - hash.length;
        for (int i = 0; i < hash.length; i++) {
            hash[i] = text[msgLength + i];
        }
        return hash;
    }

    /**
     * Compares two hashes and returns TRUE if they match element for element
     * exactly.
     * Generally used to compare an appended hash and the hash produced by the
     * received message itself.
     * 
     * @param hash1 byte array containing hash for a message
     * @param hash2 byte array containing hash for a message
     * @return TRUE if the two hash byte arrays are the same length AND match
     *         element for element exactly.
     */
    public static boolean compareHash(byte[] hash1, byte[] hash2) {
        boolean match = false;
        if (hash1.length != hash2.length) {
            return match;
        } else {
            for (int i = 0; i < hash1.length; i++) {
                if (hash1[i] != hash2[i]) {
                    return match;
                }
            }
            match = true;
        }
        return match;
    }

    /**
     * Generates and returns a hash of the given text using the SHA-1 algorithm.
     * 
     * @param text byte array of the text to be hashed
     * @return hash as byte array
     */
    public static byte[] sha1(byte[] text) {
        MessageDigest digest = null;
        byte[] hash = null;
        try {
            digest = MessageDigest.getInstance("SHA-1");
            hash = digest.digest(text);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hash;
    }

    /**
     * Returns this message's ciphertext - the encrypted version of the message.
     * 
     * @return byte array of ciphertext
     */
    public byte[] getCipherText() {
        return cipherText;
    }

    /**
     * Returns this message's plaintext - the unencrypted version of the message
     * 
     * @return byte array of plaintext
     */
    public byte[] getPlainText() {
        return plainText;
    }

    /**
     * Used to determine if message was received or sent by the user.
     * 
     * @return TRUE if the message was constructed as an encrypted message, FALSE if
     *         constructed as an unencrypted message (based on encrypted boolean in
     *         constructor)
     */
    public boolean isReceived() {
        return isReceived;
    }
}
