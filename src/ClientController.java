import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Optional;
import java.util.function.UnaryOperator;

public class ClientController {
    private static final String FRONTEND_RMI_NAME = "FrontEnd";
    private static final String DEFAULT_IP = "localhost";
    private static final int DEFAULT_PORT = 1099;
    public static final String BASE_DIR = "client_files/";

    // Connection UI
    @FXML private TextField textIP;
    @FXML private TextField textPort;
    @FXML private Button connect;
    @FXML private Button quit;

    // Operations
    @FXML private Button delf;
    @FXML private Button dwld;
    @FXML private Button list;
    @FXML private Button upld;

    // Listview
    @FXML private ListView<String> listView;

    // FrontEnd connection
    private FrontEndInterface frontEnd;

    // Formatter to restrict inputs to only numbers
    // https://stackoverflow.com/q/40472668
    private UnaryOperator<TextFormatter.Change> integerFilter = textField -> {
        String input = textField.getText();
        if (input.matches("[0-9]*")) {
            return textField;
        }
        return null;
    };

    @FXML
    public void initialize() {
        setUIState(false);

        textIP.setText(DEFAULT_IP);
        textPort.setText(String.valueOf(DEFAULT_PORT));
        textPort.setTextFormatter(new TextFormatter<String>(integerFilter));

        Log.init(listView);
    }

    private void setUIState() {
        setUIState(frontEnd != null);
    }

    private void setUIState(boolean connected) {
        disableConnectionGUI(connected);
        disableOperations(!connected);
    }

    private void disableAllUI() {
        disableOperations(true);
        disableConnectionGUI(true);
    }

    private void disableConnectionGUI(boolean disable) {
        textIP.setDisable(disable);
        textPort.setDisable(disable);
        connect.setDisable(disable);
    }

    private void disableOperations(boolean disable) {
        quit.setDisable(disable);
        delf.setDisable(disable);
        dwld.setDisable(disable);
        list.setDisable(disable);
        upld.setDisable(disable);
    }

    @FXML
    private void connect() {
        // Get IP/Port
        String hostname = textIP.getText();
        int port = Integer.parseInt(textPort.getText());

        Log.log("Retrieving front end stub at " + hostname + ":" + port);

        Task<Boolean> task = new Task<Boolean>() {
            @Override protected Boolean call() {
                // Attempt to connect to registry and retrieve stub
                try {
                    Registry registry = LocateRegistry.getRegistry(hostname, port);
                    frontEnd = (FrontEndInterface) registry.lookup("FrontEnd");
                    Log.log("Succesfully retrieved stub");
                    return true;
                } catch (RemoteException | NotBoundException e) {
                    frontEnd = null;
                    Log.log(e.getMessage());
                    return false;
                }
            }
        };

        updateOnTaskEnd(task);
        startTask(task);
    }

    @FXML
    private void delete() {
    }

    @FXML
    private void download() {
    }

    @FXML private void list() {
    }

    @FXML private void quit() {
    }

    @FXML private void upload() {
    }

    private void saveFile(String suggestedName, byte[] data) {
        // Get file
        FileChooser fc = new FileChooser();
        fc.setTitle("Save file");
        fc.setInitialDirectory(new File(BASE_DIR));
        fc.setInitialFileName(new File(suggestedName).getName());
        File outFile = fc.showSaveDialog(getStage());

        if (outFile == null) {
            return;
        }

        // Make directory if it doesn't exist (for some reason)
        // Write data out
        //noinspection ResultOfMethodCallIgnored
        outFile.getParentFile().mkdirs();
        try (FileOutputStream stream = new FileOutputStream(outFile)) {
            stream.write(data);
            Log.log("File saved to disk");
        } catch (IOException e) {
            Log.log("Error writing file to disk");
            Log.log(e.getMessage());
        }
    }

    private Stage getStage() {
        return (Stage) listView.getScene().getWindow();
    }

    private void updateOnTaskEnd(Task<Boolean> task) {
        task.setOnSucceeded(event -> {
            if (!task.getValue()) {
                quit();
            } else {
                setUIState();
            }
        });
    }

    private void startTask(Task task) {
        disableAllUI();
        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
    }

    private Optional<String> getInput(String header, String content, String defaultValue) {
        TextInputDialog dialog = new TextInputDialog(defaultValue);
        dialog.initStyle(StageStyle.UTILITY);
        dialog.setTitle("");
        dialog.setHeaderText(header);
        dialog.setContentText(content);
        dialog.initOwner(getStage());
        dialog.initModality(Modality.WINDOW_MODAL);

        return dialog.showAndWait();

    }
}

class Log {
    private static ListView<String> list;

    public static void init(ListView<String> list) {
        Log.list = list;
    }

    public static void log(String msg) {
        System.out.println(msg);
        Platform.runLater(() -> {
            list.getItems().add(msg);
            list.scrollTo(list.getItems().size() - 1);
        });
    }
}

class DownloadedFile {
    private byte[] data;
    private boolean socketError;

    DownloadedFile(boolean socketError, byte[] data) {
        this.socketError = socketError;
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }

    public boolean hadSocketError() {
        return socketError;
    }

    public boolean containsData() {
        return data != null;
    }
}
