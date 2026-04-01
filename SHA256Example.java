import java.security.MessageDigest;

public class SHA256Example {
    public static void main(String[] args) throws Exception {
        String input = "hello";

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(input.getBytes());

        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            hex.append(String.format("%02x", b));
        }

        System.out.println(hex.toString().length());
    }
}