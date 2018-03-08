import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ClientGUI extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("client.fxml"));
        stage.setTitle("Client");
        stage.setScene(new Scene(root, 600, 400));
        stage.getScene().getStylesheets().add(getClass().getResource("client.css").toExternalForm());
        stage.setMinWidth(400);
        stage.setMinHeight(300);
        stage.show();
    }

    public static void main(String[] args) {
        Shared.ensureDirExists(ClientController.BASE_DIR);
        System.out.println("Launching GUI");
        launch(args);
    }
}