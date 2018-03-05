import java.net.InetAddress;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class FrontEnd extends UnicastRemoteObject implements FrontEndInterface {
     protected FrontEnd() throws RemoteException {
    }

    public static void main(String[] args) {
        try {
            FrontEnd obj = new FrontEnd();

            System.out.println("Detected Local IP: " + InetAddress.getLocalHost().toString());
            // Bind the remote object's stub in the registry
            Registry register = LocateRegistry.getRegistry(1099);
            register.rebind("FrontEnd", obj);

            System.out.println("Server ready");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }

    @Override
    public String[] list() throws RemoteException {
        return new String[0];
    }
}
