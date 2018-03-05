import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class Server {
    private static int PORT_NUMBER = 1234;
    public static String BASE_DIR = "server_files/";
    public static int TIMEOUT = 5000;

    private void run() throws Exception {
        ServerSocket serverSocket = new ServerSocket(PORT_NUMBER);
        System.out.println("Server started on port " + PORT_NUMBER);

        int curID = 1;

        //noinspection InfiniteLoopStatement
        while(true){
            try {
                Socket clientSocket = serverSocket.accept();
                clientSocket.setSoTimeout(TIMEOUT);
                new Thread(new ServerConnection(clientSocket, curID)).start();
                curID++;
            } catch (IOException e) {
                System.out.println("Error accepting client connection: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) throws Exception {
        // Create base dir if it doesn't exist
        File bd = new File(BASE_DIR);
        if (!bd.exists()) {
            System.out.println("Base directory '" + BASE_DIR + "' does not exist");
            if (bd.mkdir()) {
                System.out.println("Directory created");
            } else {
                System.out.println("Could not create directory. Errors will probably occur from now on");
            }
        }

        // Get user input for configurable options
        Scanner scanner = new Scanner(System.in);

        // Enter timeout
        System.out.print("Enter socket timeout in ms (Leave empty for default): ");
        String timeoutStr = scanner.nextLine();

        try {
            TIMEOUT = Integer.valueOf(timeoutStr);
            System.out.println(String.format("Set timeout to %,dms", TIMEOUT));
        } catch (NumberFormatException e) {
            System.out.println(String.format("Timeout set to default value of %,dms", TIMEOUT));
        }

        // Enter port
        System.out.print("Enter port (Leave empty for default): ");
        String portStr = scanner.nextLine();

        try {
            PORT_NUMBER= Integer.valueOf(portStr);
            System.out.println(String.format("Set port to %,d", PORT_NUMBER));
        } catch (NumberFormatException e) {
            System.out.println("Set port to default value of " + PORT_NUMBER);
        }

        // Run server
        scanner.close();
        new Server().run();
    }
}
