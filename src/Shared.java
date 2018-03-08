import java.io.File;

public class Shared {

    // Converts a string to a positive integer
    // Returns -1 if this could not be done
    public static int stringToPosInt(String s, String errMsg) {
        int val;

        try {
            val = Integer.parseInt(s);

            if (val < 1) {
                throw new NumberFormatException("Less than 1");
            }
        } catch (NumberFormatException e) {
            System.out.println(errMsg);
            return -1;
        }

        return val;
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