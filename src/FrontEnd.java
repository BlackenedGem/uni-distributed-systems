import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class FrontEnd extends UnicastRemoteObject implements FrontEndInterface {
    // Constants
    private static final String DEFAULT_RMI_HOSTNAME = "localhost";
    private static final int DEFAULT_RMI_PORT = 1099;

    private static final int MAX_SERVERS = 3;
    private static final String FRONTEND_RMI_NAME = "FrontEnd";
    private static final String SERVER_RMI_NAME = "FileServer";

    // Object variables
    private Registry register;
    private List<ServerInterface> fileServers = new ArrayList<>();

    public static void main(String[] args) {
        // Read arguments
        String hostname;
        if (args.length >= 1) {
            hostname = args[0];
        } else {
            hostname = DEFAULT_RMI_HOSTNAME;
        }

        int port;
        if (args.length >= 2) {
            port = Shared.stringToPosInt(args[1], "Port number must be a positive integer");

            if (port == -1) {
                System.out.println("Using default port of " + DEFAULT_RMI_PORT);
                port = DEFAULT_RMI_PORT;
            }
        } else {
            port = DEFAULT_RMI_PORT;
        }

        // Initialise front end
        System.out.println("Initialising front end at " + hostname + ":" + port);

        try {
            FrontEnd obj = new FrontEnd(hostname, port);

            // System.out.println("Detected Local IP: " + InetAddress.getLocalHost().toString());
            // Bind the remote object's stub in the registry
            Registry register = LocateRegistry.getRegistry(1099);
            register.rebind(FRONTEND_RMI_NAME, obj);

            System.out.println("Server ready");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }

    private FrontEnd(String hostname, int port) throws RemoteException {
        log("Retrieving registry and file server stubs");

        // Initialize the registry
        register = LocateRegistry.getRegistry(hostname, port);

        // Attempt to initialize servers
        for (int i = 1; i <= MAX_SERVERS; i++) {
            fileServers.add(getServerStub(i));
        }

        log("Front End initialised");
    }

    // Attempts to retrieve a file server from the registry
    // Returns the stub if successful, otherwise null
    private ServerInterface getServerStub(int id) {
        log("Retrieving stub for server" + id);
        try {
            ServerInterface stub = (ServerInterface) register.lookup(SERVER_RMI_NAME + id);
            log("Retrieved stub for server " + id);
            return stub;
        } catch (RemoteException | NotBoundException e) {
            log("Could not retrieve file server " + id + " stub");
            return null;
        }
    }

    // Iterates over the list of servers, and if any are null attempts to reconnect
    // Sets them to null if reconnect fails
    private void reconnectToDeadServers() {
        for (int i = 0; i < MAX_SERVERS; i++) {
            ServerInterface server = fileServers.get(i);

            if (server == null) {
                log("Server " + (i + 1) + " is not connected, attempting to reconnect");
                fileServers.set(i, getServerStub(i + 1));
            }
        }
    }

    @Override
    public String[] list() {
        log("Received operation LIST. Checking server statuses first");
        reconnectToDeadServers();
        log("Retrieving listings from servers");

        Set<String> listings = new HashSet<>();

        int serversUsed = 0;
        for (int i = 0; i < MAX_SERVERS; i++) {
            ServerInterface server = fileServers.get(i);
            if (server == null) { continue; }

            try {
                listings.addAll(server.list());
                serversUsed++;
            } catch (RemoteException e) {
                log(e.getMessage());
                log("Disconnected file server " + (i + 1));
            }
        }

        List<String> sortedListings = new ArrayList<>(listings);
        Collections.sort(sortedListings);
        String[] returnArray = sortedListings.toArray(new String[sortedListings.size()]);

        log("Listings retrieved from " + serversUsed + " servers, sending to client");
        return returnArray;
    }

    private void log(String msg) {
        System.out.println(msg);
    }
}
