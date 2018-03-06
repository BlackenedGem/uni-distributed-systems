import java.net.InetAddress;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

public class FrontEnd extends UnicastRemoteObject implements FrontEndInterface {
    private static final int MAX_SERVERS = 3;
    private static final String SERVER_RMI_NAME = "FileServer";

    private Registry register;
    private List<ServerInterface> fileServers = new ArrayList<>();

    public static void main(String[] args) {
        try {
            FrontEnd obj = new FrontEnd();

            // System.out.println("Detected Local IP: " + InetAddress.getLocalHost().toString());
            // Bind the remote object's stub in the registry
            Registry register = LocateRegistry.getRegistry(1099);
            register.rebind("FrontEnd", obj);

            System.out.println("Server ready");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }

    private FrontEnd() throws RemoteException {
        log("Retrieving registry and file server stubs");

        // Initialize the registry
        register = LocateRegistry.getRegistry(1099);

        // Attempt to initialize servers
        for (int i = 0; i < MAX_SERVERS; i++) {
            fileServers.add(i, getServerStub(i));
        }

        log("Front End initialised");
    }

    // Attempts to retrieve a file server from the registry
    // Returns the stub if successful, otherwise null
    private ServerInterface getServerStub(int id) {
        try {
            ServerInterface stub = (ServerInterface) register.lookup(SERVER_RMI_NAME + id);
            return stub;
        } catch (RemoteException | NotBoundException e) {
            log("Could not retrieve file server " + id + " stub. " + e.getMessage());
            return null;
        }
    }

    @Override
    public String[] list() {
        return new String[]{"Hello", " ", "World", "!"};
    }

    private void log(String msg) {
        System.out.println(msg);
    }
}
