import java.io.File;

public class Shared {
    // Attempts to read a value from the command line as an integer
    // Returns the default value if this cannot be done
    public static int parseCommandLineInteger(String[] args, int index, String errMsg, int defaultVal) {
        // Return default value if argument not specified
        if (index >= args.length) {
            return defaultVal;
        }

        // Attempt to parse and return the int
        try {
            int val = Integer.parseInt(args[index]);

            if (val < 1) {
                System.out.println(errMsg);
                return defaultVal;
            }

            return val;
        } catch (NumberFormatException e) {
            System.out.println(errMsg);
            return defaultVal;
        }
    }

    public static void ensureDirExists(String location) {
        // Create base dir if it doesn't exist
        File bd = new File(location);
        if (!bd.exists()) {
            System.out.println("Base directory '" + location + "' does not exist");
            if (bd.mkdir()) {
                System.out.println("Directory created");
            } else {
                System.out.println("Could not create directory. Errors will probably occur from now on");
            }
        }
    }
}