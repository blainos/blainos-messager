import java.io.IOException;
import java.security.spec.KeySpec;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

/**
 * The Messager application.
 * Handles GUI.
 * Handles overarching logic, including:
 * Whether the user acts as host or client,
 * sending and receiving of data via Hoster or Clienter,
 * creating and tracking Message objects as they are sent and received.
 */
public class Messager extends Application {

    private char[] password;
    private String ipAddress;
    private Hoster hoster;
    private Clienter clienter;
    private boolean isHost;
    private final int PORT = 5555;

    private Cipher ciph;
    private SecretKey k;
    private byte[] iv;
    private ArrayList<Message> messages;

    private static final ScheduledExecutorService receiver = Executors.newSingleThreadScheduledExecutor();

    private GridPane grid;
    private ScrollPane scroll;
    private TextField textField;
    private GridPane messageField;

    private Color darkModeSquare = Color.rgb(51, 51, 51);
    private Color darkModeBackground = Color.rgb(27, 27, 27);
    private Font consolas = new Font("Consolas", 12);

    /**
     * Constructor initialized by Login, creates instance of Messager with the given
     * IP address of target and password for encryption.
     * 
     * @param ip   IP address of host
     * @param pass password for encryption
     * @throws Exception
     */
    public Messager(String ip, String pass) throws Exception {

        password = pass.toCharArray();

        /*
         * Salt used for generating key (along with password) changes based on the date.
         * This means the same password will generate different keys each day.
         * The only hold-up is if two users' devices do not share the same date, they
         * will not be able to communicate.
         */
        LocalDate date = LocalDate.now();
        byte[] salt = date.toString().getBytes("UTF-8");

        /**
         * Key generation.
         */
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        KeySpec spec = new PBEKeySpec(password, salt, 65536, 256);
        SecretKey temp = factory.generateSecret(spec);
        k = new SecretKeySpec(temp.getEncoded(), "AES");

        /*
         * Cipher generation.
         */
        ciph = Cipher.getInstance("AES/CBC/PKCS5Padding");
        ciph.init(Cipher.ENCRYPT_MODE, k);
        // iv = ciph.getParameters().getParameterSpec(IvParameterSpec.class).getIV();
        /*
         * Initialization vector is currently based on hash of password.
         * Getting the iv using the commented-out method above seemed to be producing
         * inconsistent IVs between two users, inhibiting communication.
         */
        iv = new byte[16];
        for (int i = 0; i < iv.length; i++) {
            iv[i] = Message.sha1(pass.getBytes())[i];
        }

        messages = new ArrayList<Message>(); // currently just used for grabbing lastBlock of previous message for IV of
                                             // current message

        /*
         * initializing various variables and objects
         */
        ipAddress = ip;
        isHost = false;
        clienter = new Clienter();
        hoster = new Hoster();

        grid = new GridPane();
        textField = new TextField();
        messageField = new GridPane();
        scroll = new ScrollPane(messageField);

    }

    /**
     * does all the stuff
     */
    @Override
    public void start(Stage stage) throws IOException {

        /*
         * Determines who acts as host and who acts as client.
         * User will attempt to connect to host as client using Clienter,
         * if there is no host, then will proceed as host.
         * Currently the GUI does not launch until the host accepts a client.
         */
        try {
            System.out.println("pre connect as client");
            clienter.startConnection(ipAddress, PORT);
            System.out.println("post connect as client");
        } catch (Exception e) {
            System.out.println("connect as client failed, into try/catch");
            isHost = true;
        } finally {
            if (isHost) {
                System.out.println("pre start as host");
                hoster.start(PORT); // Halts inside here until client connects.
                System.out.println("post start as host");
            }
        }

        /*
         * close protocol
         */
        stage.setOnCloseRequest(e -> {
            try {
                if (isHost) {
                    hoster.stop();
                } else {
                    clienter.stopConnection();
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            receiver.shutdown();
        });

        /*
         * Build GUI
         */
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10, 10, 10, 10));
        grid.setBackground(new Background(new BackgroundFill(darkModeBackground, CornerRadii.EMPTY, Insets.EMPTY)));

        textField.setBackground(new Background(new BackgroundFill(darkModeSquare, CornerRadii.EMPTY, Insets.EMPTY)));
        textField.setStyle("-fx-text-fill: white;");

        messageField.setHgap(10);
        messageField.setVgap(10);
        messageField.setPadding(new Insets(10, 10, 10, 10));
        messageField
                .setBackground(new Background(new BackgroundFill(darkModeBackground, CornerRadii.EMPTY, Insets.EMPTY)));

        scroll.setPrefHeight(400);
        scroll.setPrefWidth(250);
        scroll.setBackground(new Background(new BackgroundFill(darkModeBackground, CornerRadii.EMPTY, Insets.EMPTY)));
        scroll.setStyle(
                "-fx-background: transparent; -fx-background-color: transparent; -fx-padding: 10; -fx-background-insets: 0;");
        scroll.setHbarPolicy(ScrollBarPolicy.NEVER);
        scroll.vvalueProperty().bind(messageField.heightProperty());

        grid.add(scroll, 0, 0);
        grid.add(textField, 0, 1);

        /*
         * Handles message sending here.
         * Text typed in text field will send on press of enter key
         */
        textField.setOnKeyPressed(e -> {
            if (e.getCode().equals(KeyCode.ENTER)) {
                try {
                    byte[] msgBytes = textField.getText().getBytes("UTF-8"); // String in textField converted to bytes
                    if (messages.size() != 0) {
                        iv = messages.get(messages.size() - 1).getLastBlock(); // sets IV to the last block of the
                                                                               // previous message
                    }
                    Message outgoingMessage = new Message(msgBytes, k, ciph, iv, false); // creates Message object.
                                                                                         // Encryption happens inside
                                                                                         // constructor.
                    messages.add(outgoingMessage);
                    textField.clear();
                    print(outgoingMessage); // outgoing Message is displayed on screen, both ciphertext and plaintext.
                    if (isHost) {
                        hoster.send(outgoingMessage.getCipherText()); // sends as host if host
                    } else {
                        clienter.send(outgoingMessage.getCipherText()); // sends as client if client
                    }

                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        });

        Scene scene = new Scene(grid);
        scene.setFill(darkModeSquare);
        stage.setResizable(false);
        stage.setMaximized(false);
        stage.setScene(scene);
        stage.setTitle("Messager");
        stage.show();

        /*
         * Receiving messages handled here.
         */
        Runnable task = new Runnable() {
            public void run() {
                try {
                    if (isHost && hoster.isConnected()) { // this whole if statement could be consolidated if Hoster and
                                                          // Clienter were consolidated

                        while (hoster.isConnected()) { // receiving loop

                            byte[] msgBytes;
                            if ((msgBytes = hoster.receive()) != null) {

                                if (messages.size() != 0) {
                                    iv = messages.get(messages.size() - 1).getLastBlock(); // creates IV from last block
                                                                                           // of last message
                                }
                                Message incomingMessage = new Message(msgBytes, k, ciph, iv, true); // creates Message
                                                                                                    // object.
                                                                                                    // Decryption
                                                                                                    // happens here.
                                messages.add(incomingMessage);
                                print(incomingMessage); // prints Message to screen, both ciphertext and plain text.
                            }
                        }
                    } else {

                        // same stuff as above but with Clienter instead of Hoster
                        while (clienter.isConnected()) { // receiving loop
                            byte[] msgBytes;
                            if ((msgBytes = clienter.receive()) != null) {

                                if (messages.size() != 0) {
                                    iv = messages.get(messages.size() - 1).getLastBlock();
                                }
                                Message incomingMessage = new Message(msgBytes, k, ciph, iv, true);
                                messages.add(incomingMessage);
                                print(incomingMessage);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        receiver.schedule(task, 1, TimeUnit.SECONDS); // creates thread or whatever

    }

    /**
     * Prints the given message to the screen, ciphertext and then plaintext.
     * Text is white if sent, and green if received.
     * 
     * @param message the message to display
     */
    public void print(Message message) {
        /*
         * Code inside runnable so JFX can run it inside the proper thread, since this
         * method can be called from a separate receiver thread.
         */
        Runnable runnable = new Runnable() {
            public void run() {
                Text text = new Text(new String(message.getCipherText()) + "\n" + new String(message.getPlainText()));
                text.setFont(consolas);
                text.setStyle("-fx-padding: 0;");
                text.setWrappingWidth(200);
                if (message.isReceived()) {
                    text.setFill(Color.GREEN);
                } else {
                    text.setFill(Color.WHITE);
                }
                messageField.add(text, 0, getRowCount(messageField));
            }
        };
        Platform.runLater(runnable);
    }


    private int getRowCount(GridPane pane) {
        int numRows = pane.getRowConstraints().size(); 
        for (int i = 0; i < pane.getChildren().size(); i++) {
            Node child = pane.getChildren().get(i);
            if (child.isManaged()) { // Check if the child is managed, i.e. part of the layout
                Integer rowIndex = GridPane.getRowIndex(child); 
                if(rowIndex != null){
                    numRows = Math.max(numRows,rowIndex+1); 
                }
            }
        }
        return numRows;
    }


}
