package util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

public final class HuffmanTextCompressor {

    private HuffmanTextCompressor() {}

    public static Result compressAndRestore(String text) throws IOException {
        String source = text == null ? "" : text;
        byte[] originalBytes = source.getBytes(StandardCharsets.UTF_8);
        if (originalBytes.length == 0) {
            return new Result("", "", 0, 0, 0d);
        }

        Map<Character, Integer> frequency = buildFrequency(source);
        Node root = buildTree(frequency);
        Map<Character, String> codes = new HashMap<>();
        buildCodes(root, "", codes);

        StringBuilder bits = new StringBuilder();
        for (int i = 0; i < source.length(); i++) {
            bits.append(codes.get(source.charAt(i)));
        }

        byte[] packed = packBits(bits.toString());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream dos = new DataOutputStream(baos)) {
            dos.writeInt(frequency.size());
            for (Map.Entry<Character, Integer> entry : frequency.entrySet()) {
                dos.writeChar(entry.getKey());
                dos.writeInt(entry.getValue());
            }
            dos.writeInt(bits.length());
            dos.writeInt(packed.length);
            dos.write(packed);
        }

        byte[] compressedBytes = baos.toByteArray();
        String base64 = Base64.getEncoder().encodeToString(compressedBytes);
        String restored = decompress(base64);
        double ratio = originalBytes.length == 0 ? 0d : (double) compressedBytes.length / (double) originalBytes.length;
        return new Result(base64, restored, originalBytes.length, compressedBytes.length, ratio);
    }

    public static String decompress(String base64) throws IOException {
        if (base64 == null || base64.trim().isEmpty()) {
            return "";
        }

        byte[] payload = Base64.getDecoder().decode(base64);
        try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(payload))) {
            int frequencySize = dis.readInt();
            Map<Character, Integer> frequency = new HashMap<>();
            for (int i = 0; i < frequencySize; i++) {
                char character = dis.readChar();
                int count = dis.readInt();
                frequency.put(character, count);
            }

            int bitCount = dis.readInt();
            int packedLength = dis.readInt();
            byte[] packed = new byte[packedLength];
            dis.readFully(packed);

            Node root = buildTree(frequency);
            if (root == null) {
                return "";
            }

            if (root.isLeaf()) {
                StringBuilder single = new StringBuilder();
                for (int i = 0; i < bitCount; i++) {
                    single.append(root.character);
                }
                return single.toString();
            }

            StringBuilder decoded = new StringBuilder();
            Node current = root;
            for (int i = 0; i < bitCount; i++) {
                int bit = (packed[i / 8] >> (7 - (i % 8))) & 1;
                current = bit == 0 ? current.left : current.right;
                if (current.isLeaf()) {
                    decoded.append(current.character);
                    current = root;
                }
            }
            return decoded.toString();
        }
    }

    private static Map<Character, Integer> buildFrequency(String source) {
        Map<Character, Integer> frequency = new HashMap<>();
        for (int i = 0; i < source.length(); i++) {
            char character = source.charAt(i);
            frequency.put(character, frequency.getOrDefault(character, 0) + 1);
        }
        return frequency;
    }

    private static Node buildTree(Map<Character, Integer> frequency) {
        PriorityQueue<Node> queue = new PriorityQueue<>();
        for (Map.Entry<Character, Integer> entry : frequency.entrySet()) {
            queue.add(new Node(entry.getKey(), entry.getValue(), null, null));
        }

        if (queue.isEmpty()) {
            return null;
        }

        while (queue.size() > 1) {
            Node left = queue.poll();
            Node right = queue.poll();
            queue.add(new Node('\0', left.frequency + right.frequency, left, right));
        }
        return queue.poll();
    }

    private static void buildCodes(Node node, String prefix, Map<Character, String> codes) {
        if (node == null) {
            return;
        }
        if (node.isLeaf()) {
            codes.put(node.character, prefix.isEmpty() ? "0" : prefix);
            return;
        }
        buildCodes(node.left, prefix + '0', codes);
        buildCodes(node.right, prefix + '1', codes);
    }

    private static byte[] packBits(String bits) {
        byte[] packed = new byte[(bits.length() + 7) / 8];
        for (int i = 0; i < bits.length(); i++) {
            if (bits.charAt(i) == '1') {
                packed[i / 8] |= (byte) (1 << (7 - (i % 8)));
            }
        }
        return packed;
    }

    public static final class Result {
        public final String compressedBase64;
        public final String restoredText;
        public final int originalBytes;
        public final int compressedBytes;
        public final double ratio;

        public Result(String compressedBase64, String restoredText, int originalBytes, int compressedBytes, double ratio) {
            this.compressedBase64 = compressedBase64;
            this.restoredText = restoredText;
            this.originalBytes = originalBytes;
            this.compressedBytes = compressedBytes;
            this.ratio = ratio;
        }
    }

    private static final class Node implements Comparable<Node> {
        private final char character;
        private final int frequency;
        private final Node left;
        private final Node right;

        private Node(char character, int frequency, Node left, Node right) {
            this.character = character;
            this.frequency = frequency;
            this.left = left;
            this.right = right;
        }

        private boolean isLeaf() {
            return left == null && right == null;
        }

        @Override
        public int compareTo(Node other) {
            return Integer.compare(this.frequency, other.frequency);
        }
    }
}