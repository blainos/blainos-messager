import java.io.IOException;
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;

/**
 * Used to login to the Messager app. Takes IP address and password and passes
 * it along to Messager.
 */
public class Login extends Application {

    Color darkModeSquare = Color.rgb(51, 51, 51);
    Color darkModeBackground = Color.rgb(27, 27, 27);
    Color textColor = Color.WHITE;
    Messager messager;

    /**
     * Main method for the whole application.
     * 
     * @param args
     */
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage loginStage) {
        loginStage.setTitle("Login");

        /*
         * Build login window
         */
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 20, 20));
        grid.setBackground(new Background(new BackgroundFill(darkModeBackground, CornerRadii.EMPTY, Insets.EMPTY)));

        TextField ipField = new TextField();
        ipField.setBackground(new Background(new BackgroundFill(darkModeSquare, CornerRadii.EMPTY, Insets.EMPTY)));
        ipField.setStyle("-fx-text-fill: white;");
        PasswordField passwordField = new PasswordField();
        passwordField
                .setBackground(new Background(new BackgroundFill(darkModeSquare, CornerRadii.EMPTY, Insets.EMPTY)));
        passwordField.setStyle("-fx-text-fill: white;");
        Button loginButton = new Button("Login");
        loginButton.setBackground(new Background(new BackgroundFill(darkModeSquare, CornerRadii.EMPTY, Insets.EMPTY)));
        loginButton.setTextFill(textColor);

        Label ipLabel = new Label("IP Address:");
        ipLabel.setTextFill(textColor);
        Label passwordLabel = new Label("Password:");
        passwordLabel.setTextFill(textColor);

        grid.add(ipLabel, 0, 0);
        grid.add(ipField, 1, 0);
        grid.add(passwordLabel, 0, 1);
        grid.add(passwordField, 1, 1);
        grid.add(loginButton, 1, 2);

        /*
         * Takes input in IP Address and password fields, creates a new instance of
         * Messager with given input, closes the login window and launches Messager.
         */
        loginButton.setOnAction(e -> {
            String ip = ipField.getText();
            String password = passwordField.getText();

            try {
                loginStage.close();
                messager = new Messager(ip, password);
                messager.start(new Stage());
            } catch (IOException e1) {
                e1.printStackTrace();
            } catch (Exception e2) {
                e2.printStackTrace();
            }

        });

        Scene scene = new Scene(grid, 300, 150);
        scene.setFill(darkModeBackground);
        loginStage.setScene(scene);
        loginStage.show();
    }

}
