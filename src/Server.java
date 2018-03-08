import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

public class Server extends UnicastRemoteObject implements ServerInterface {
    private static final String SERVER_RMI_NAME = "FileServer";
    private static final String DEFAULT_RMI_HOSTNAME = "localhost";
    private static final int DEFAULT_RMI_PORT = 1099;
    private static final String BASE_DIR = "server_files_";

    private String FILES_DIR;

    // Main entry functions
    public static void main(String[] args) {
        // Ensure argument length
        if (args.length == 0) {
            System.out.println("No arguments received");
            System.out.println("Must receive arguments in the form: ServerID [Hostname] [Port]");
            return;
        }

        // Read arguments
        int serverID = Shared.parseCommandLineInteger(args, 0, "Server ID must be a positive integer", -1);
        if (serverID == -1) {
            return;
        }

        String hostname;
        if (args.length >= 2) {
            hostname = args[1];
        } else {
            hostname = DEFAULT_RMI_HOSTNAME;
        }

        int port = Shared.parseCommandLineInteger(args, 2, "Port number must be a positive integer", DEFAULT_RMI_PORT);

        // Initialise server
        System.out.println("Initialising server with ID = " + serverID + " at " + hostname + ":" + port);

        try {
            Server obj = new Server(serverID);

            // System.out.println("Detected Local IP: " + InetAddress.getLocalHost().toString());
            // Bind the remote object's stub in the registry
            Registry register = LocateRegistry.getRegistry(hostname, port);
            register.rebind(SERVER_RMI_NAME + serverID, obj);

            System.out.println("Server ready");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }

    // Object functions
    private Server(int id) throws RemoteException {
        this.FILES_DIR = BASE_DIR + id + "/";
        Shared.ensureDirExists(FILES_DIR);
    }

    @Override
    public int delete(String filename) {
        log("Received request to delete: " + filename);

        // Server returns 1 or -1 based on whether or not the file exists
        File file = new File(FILES_DIR + filename);
        if (!file.exists()) {
            log("File doesn't exist: " + file.toString());
            return -1;
        }

        // Delete file
        if (file.delete()) {
            log("File deleted");
            return 1;
        } else {
            log("Error deleting file");
            return 0;
        }
    }

    @Override
    public byte[] download(String filename) {
        log("Received request to download: " + filename);

        // Check if file exists
        File file = new File(FILES_DIR + filename);
        if (!file.exists()) {
            log("The file \"" + filename + "\" does not exist on the server");
            return null;
        }

        // Read file from disk and return
        log("Reading file from disk");
        try {
            byte[] data = Files.readAllBytes(file.toPath());
            log("Data read from disk and returned");
            return data;
        } catch (IOException e) {
            log("Could not read '" + file.toString() + "' from disk. " + e.getMessage());
            return null;
        }
    }

    @Override
    public boolean fileExists(String filename) {
        File file = new File(FILES_DIR + filename);
        return file.exists();
    }

    @Override
    public List<String> list() {
        log("Received request to obtain listings");
        List<String> listings = new ArrayList<>();

        // Get listings by traversing through source directory
        try {
            Files.walk(Paths.get(FILES_DIR))
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        String listing = path.toString().substring(FILES_DIR.length());
                        listings.add(listing);
                    });
        } catch (IOException e) {
            log("Could not find file listings: " + e.getMessage());
            return new ArrayList<>();
        }

        log("Returned listings");
        return listings;
    }

    @Override
    public boolean upload(String filename, byte[] data) {
        log("Received request to upload a file to: " + filename);

        // Convert filename to full path and make directories
        File outFile = new File(FILES_DIR + filename);
        //noinspection ResultOfMethodCallIgnored
        outFile.getParentFile().mkdirs();

        // Save data
        try (FileOutputStream stream = new FileOutputStream(outFile)) {
            stream.write(data);
            log("File saved to disk");
            return true;
        } catch (IOException e) {
            log("Error writing file to disk");
            log(e.getMessage());
            return false;
        }
    }

    private void log(String msg) {
        System.out.println(msg);
    }
}
