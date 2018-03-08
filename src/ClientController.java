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
import java.nio.file.Files;
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
                    frontEnd = (FrontEndInterface) registry.lookup(FRONTEND_RMI_NAME);
                    Log.log("Succesfully retrieved stub");
                    return true;

                } catch (RemoteException | NotBoundException e) {
                    // Output message and disconnect if a failure occurs
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
        // Get the filename to delete
        Optional<String> result = getInput("Choose file to delete", "Enter filename:", "");
        if (!result.isPresent()) {
            return;
        }

        // Task returns 1 if file exists, -1 if it doesn't, and 0 if a remote exception occurred
        Task<Integer> task1 = new Task<Integer>() {
            @Override protected Integer call() {
                try {
                    if (frontEnd.fileExists(result.get())) {
                        return 1;
                    } else {
                        return -1;
                    }
                } catch (RemoteException e) {
                    Log.log(e.getMessage());
                    return 0;
                }
            }
        };

        task1.setOnSucceeded(event -> {
            // If response is 0 then a remote exception error occurred
            if (task1.getValue() == 0) {
                quit();
                return;
            } else if (task1.getValue() == -1) {
                setUIState();
                return;
            }

            // If the file exists then prompt user to delete
            Alert a = new Alert(Alert.AlertType.CONFIRMATION);
            a.setTitle("Confirm");
            a.setHeaderText("Delete file?");
            Optional<ButtonType> result2 = a.showAndWait();

            // Return if user does not confirm
            if (!(result2.isPresent() && result2.get() == ButtonType.OK)) {
                setUIState();
                return;
            }

            // Create a new task to send the confirmation
            Task<Boolean> task2 = new Task<Boolean>() {
                @Override protected Boolean call() {
                    try {
                        Log.log(frontEnd.delete(result.get()));
                        return true;
                    } catch (RemoteException e) {
                        Log.log(e.getMessage());
                        return false;
                    }
                }
            };

            // Start the task
            updateOnTaskEnd(task2);
            startTask(task2);
        });

        startTask(task1);
    }

    @FXML
    private void download() {
        // Get filename
        Optional<String> result = getInput("Enter filename", "File to download from server:", "");
        if (!result.isPresent()) {
            return;
        }

        Task<DownloadedFile> task = new Task<DownloadedFile>() {
            @Override protected DownloadedFile call() {
                try {
                    byte[] data = frontEnd.download(result.get());
                    return new DownloadedFile(false, data);
                } catch (RemoteException e) {
                    // Output message if a failure occurs
                    // Disconnect is done by setting the rmiError flag of DownloadedFile to true
                    Log.log(e.getMessage());
                    return new DownloadedFile(true, null);
                }
            }
        };

        task.setOnSucceeded(event -> {
            DownloadedFile df = task.getValue();
            if (df.hadRMIError()) {
                quit();
                return;
            } else if (df.containsData()) {
                saveFile(result.get(), df.getData());
            } else {
                Log.log("File does not exist on the available servers (or an internal server error occurred)");
            }

            setUIState();
        });
        startTask(task);
    }

    @FXML
    private void list() {
        Log.log("Retrieving listings");

        Task<Boolean> task = new Task<Boolean>() {
            @Override protected Boolean call() {
                try {
                    // Fetch listings and output
                    String[] listings = frontEnd.list();

                    if (listings.length == 0) {
                        Log.log("Server contains no listings");
                    } else {
                        Log.log("Listings:");
                        for (String listing : listings) {
                            Log.log(listing);
                        }
                    }
                    return true;

                } catch (RemoteException e) {
                    // Output message and disconnect if a failure occurs
                    Log.log(e.getMessage());
                    return false;
                }
            }
        };

        updateOnTaskEnd(task);
        startTask(task);
    }

    @FXML
    private void quit() {
        frontEnd = null;
        Log.log("Discarded stub reference from memory");
        setUIState();
    }

    @FXML
    private void upload() {
        // Get file
        FileChooser fc = new FileChooser();
        fc.setTitle("Select file");
        fc.setInitialDirectory(new File(BASE_DIR));
        File file = fc.showOpenDialog(getStage());

        if (file == null) {
            return;
        }

        // Get filename
        Optional<String> resultName = getInput("Enter the filename to save on the server", "Filename:", file.getName());
        if (!resultName.isPresent()) {
            return;
        }

        // Get high priority or not?
        ButtonType yes = new ButtonType("Yes");
        ButtonType no = new ButtonType("No");

        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("File reliability");
        a.setHeaderText("Upload file with high reliability?");
        a.getButtonTypes().setAll(yes, no);
        a.initOwner(getStage());
        a.initModality(Modality.WINDOW_MODAL);
        Optional<ButtonType> resultMode = a.showAndWait();
        boolean highReliability = resultMode.isPresent() && resultMode.get() == yes;

        Task<Boolean> task = new Task<Boolean>() {
            @Override protected Boolean call() {
                // Read file before sending
                Log.log("Reading file from disk");
                byte[] bytes;
                try {
                    bytes = Files.readAllBytes(file.toPath());
                } catch (IOException e) {
                    // Handle error
                    // Note that we still return true because a client error has occurred, not an RMI error
                    Log.log("Error reading file from disk. " + e.getMessage());
                    return true;
                }

                // Send file through front end
                try {
                    Log.log("Uploading file");
                    String response = frontEnd.upload(resultName.get(), bytes, highReliability);
                    Log.log(response);
                } catch (RemoteException e) {
                    // Output message and disconnect if a failure occurs
                    Log.log(e.getMessage());
                    return false;
                }

                return true;
            }
        };

        updateOnTaskEnd(task);
        startTask(task);
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
    private boolean rmiError;

    DownloadedFile(boolean rmiError, byte[] data) {
        this.rmiError = rmiError;
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }

    public boolean hadRMIError() {
        return rmiError;
    }

    public boolean containsData() {
        return data != null;
    }
}
