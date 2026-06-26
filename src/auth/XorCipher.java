package auth;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class XorCipher {

    private static final byte[] KEY = "BiblioSystem-XOR".getBytes(StandardCharsets.UTF_8);

    private XorCipher() {}

    public static String encrypt(String plainText) {
        if (plainText == null) {
            return "";
        }
        byte[] input = plainText.getBytes(StandardCharsets.UTF_8);
        byte[] output = xor(input);
        return Base64.getEncoder().encodeToString(output);
    }

    public static String decrypt(String cipherTextBase64) {
        if (cipherTextBase64 == null || cipherTextBase64.trim().isEmpty()) {
            return "";
        }
        byte[] input = Base64.getDecoder().decode(cipherTextBase64);
        byte[] output = xor(input);
        return new String(output, StandardCharsets.UTF_8);
    }

    private static byte[] xor(byte[] input) {
        byte[] output = new byte[input.length];
        for (int i = 0; i < input.length; i++) {
            output[i] = (byte) (input[i] ^ KEY[i % KEY.length]);
        }
        return output;
    }
}