import java.net.*;
import java.io.*;

/**
 * Used to act as host for communications between users.
 * One user will act as host and the other as client.
 * Thus this class is a companion class to Clienter.
 * Tbh Hoster and Clienter should be merged into a single class with a boolean
 * in the constructor but it's too late for that lol
 */
public class Hoster {
    private ServerSocket host;
    private Socket client;
    private ObjectInputStream inbox;
    private ObjectOutputStream outbox;

    /**
     * Initializes serverSocket (host) and opens for a client socket to connect. The
     * code halts until a connection is made inside this method.
     * 
     * @param port
     * @throws IOException
     */
    public void start(int port) throws IOException {
        host = new ServerSocket(port);
        client = host.accept();
        outbox = new ObjectOutputStream(client.getOutputStream());
        inbox = new ObjectInputStream(client.getInputStream());
    }

    /**
     * Used to close host serverSocket, client socket, and input and output streams.
     * 
     * @throws IOException
     */
    public void stop() throws IOException {
        inbox.close();
        outbox.close();
        client.close();
        host.close();
    }

    /**
     * Writes the given byte array as an object to the client socket.
     * 
     * @param message
     * @throws IOException
     */
    public void send(byte[] message) throws IOException {
        outbox.writeObject(new ByteWrapper(message));
    }

    /**
     * Reads an object from the socket's input stream and casts it to byte[] and
     * returns it.
     * 
     * @return receivedBytes
     * @throws Exception
     */
    public byte[] receive() throws Exception {
        ByteWrapper receivedObject = (ByteWrapper) inbox.readObject();
        byte[] receivedBytes = receivedObject.getBytes();
        return receivedBytes;
    }

    /**
     * Checks if client socket is connected to serverSocket
     * 
     * @return TRUE if client is connected and false otherwise
     */
    public boolean isConnected() {
        return client.isConnected();
    }

    /**
     * Returns the ObjectOutputStream being used for sending the byte arrays through
     * the socket
     * 
     * @return outbox
     */
    public ObjectOutputStream getOutbox() {
        return outbox;
    }

    /**
     * Returns the ObjectInputStream being used for receiving byte arrays through
     * the socket
     * 
     * @return inbox
     */
    public ObjectInputStream getInbox() {
        return inbox;
    }
}
