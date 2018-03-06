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
}