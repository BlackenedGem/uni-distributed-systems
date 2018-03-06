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
    private static String BASE_DIR = "server_files_";

    private int id;
    private String FILES_DIR;

    private Server(int id) throws RemoteException {
        this.id = id;
        this.FILES_DIR = BASE_DIR + id + "/";
    }

    public static void main(String[] args) {
        int serverID = 1;

        try {
            Server obj = new Server(serverID);

            // System.out.println("Detected Local IP: " + InetAddress.getLocalHost().toString());
            // Bind the remote object's stub in the registry
            Registry register = LocateRegistry.getRegistry(1099);
            register.rebind("FileServer" + serverID, obj);

            System.out.println("Server ready");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }

    @Override
    public List<String> list() {
        log("Sending listings to client");
        List<String> listings = new ArrayList<>();

        // Get listings by traversing through source directory
        try {
            Files.walk(Paths.get(FILES_DIR))
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        String listing = path.toString().substring(Server.BASE_DIR.length());
                        listings.add(listing);
                    });
        } catch (IOException e) {
            log("Could not find file listings: " + e.getMessage());
            return new ArrayList<>();
        }

        return listings;
    }

    private void log(String msg) {
        System.out.println(msg);
    }
}
