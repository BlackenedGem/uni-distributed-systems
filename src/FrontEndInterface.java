import java.rmi.Remote;
import java.rmi.RemoteException;

public interface FrontEndInterface extends Remote {
    // Delete a file from all servers
    // Returns a string denoting the response
    String delete(String filename) throws RemoteException;

    // Download a file from a server
    // Returns the bytes (or null if operation could not be completed)
    byte[] download(String filename) throws RemoteException;

    // Returns an array of strings representing a listing
    // This array will contain 0 items if no listings exist
    String[] list() throws RemoteException;

    // Upload a file to the server
    // Returns a response message that can be displayed to the client
    String upload(String filename, byte[] data, boolean highReliability) throws RemoteException;
}
