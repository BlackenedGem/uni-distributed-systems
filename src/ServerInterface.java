import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface ServerInterface extends Remote {
    // Returns a status code indicating if the file was deleted
    // -1 - File does not exist
    // 0 - Internal error (ie. IOException)
    // 1 - File was deleted
    int delete(String filename) throws RemoteException;

    // Response is null if file couldn't be found/another error occurred
    byte[] download(String filename) throws RemoteException;

    // Returns true if the file exists on the server
    boolean fileExists(String filename) throws RemoteException;

    // Returns a list of listings
    // List is blank if no listings exist/any other errors occurred
    List<String> list() throws RemoteException;

    // Returns true or false if upload was succesful
    boolean upload(String filename, byte[] data) throws RemoteException;
}
