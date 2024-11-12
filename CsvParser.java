import java.io.*;
import java.util.*;
import java.nio.file.*;

public class CsvParser {
    private static final int CHUNK_SIZE = 1024 * 1024; // 1MB chunks
    private static final int MAX_BUFFER_SIZE = 8192;
    private static final String COMPRESSION_EXTENSION = ".bin";
    private static final String DECOMPRESSION_EXTENSION = ".csv";

    private CsvParser() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    private static class Node implements Comparable<Node> {
        private final char ch;
        private final int freq;
        private final Node left, right;

        Node(char ch, int freq, Node left, Node right) {
            this.ch = ch;
            this.freq = freq;
            this.left = left;
            this.right = right;
        }

        private boolean isLeaf() {
            return (left == null) && (right == null);
        }

        public int compareTo(Node that) {
            if (that == null) {
                throw new NullPointerException("Cannot compare with null node");
            }
            return Integer.compare(this.freq, that.freq);
        }
    }

    // Modified compress method to handle chunks
    public static void compressLargeFile(String inputFilePath) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFilePath));
             FileOutputStream outputStream = new FileOutputStream("compressed" + COMPRESSION_EXTENSION);
             DataOutputStream dataOut = new DataOutputStream(new BufferedOutputStream(outputStream))) {

            // Write the total file size first
            long fileSize = Files.size(Paths.get(inputFilePath));
            dataOut.writeLong(fileSize);
            System.out.println("File size written: " + fileSize);

            // Process file in chunks
            char[] buffer = new char[CHUNK_SIZE];
            int bytesRead;
            int chunkIndex = 0;

            while ((bytesRead = reader.read(buffer)) != -1) {
                String chunk = new String(buffer, 0, bytesRead);
                System.out.println("Compressing chunk " + chunkIndex + " with " + bytesRead + " bytes.");
                compressChunk(chunk, dataOut, chunkIndex++);
            }
        }
    }

    private static void compressChunk(String input, DataOutputStream out, int chunkIndex) throws IOException {
        if (input.isEmpty()) {
            return;
        }

        // Create frequency map for this chunk
        Map<Character, Integer> freqMap = new HashMap<>();
        for (char ch : input.toCharArray()) {
            freqMap.merge(ch, 1, Integer::sum);
        }

        // Display frequency distribution for this chunk
        System.out.println("Frequency distribution for chunk " + chunkIndex + ": " + freqMap);

        // Build Huffman trie for this chunk
        Node root = buildTrie(freqMap);
        Map<Character, String> huffmanCodes = new HashMap<>();
        buildCode(huffmanCodes, root, "");

        // Write chunk header
        out.writeInt(chunkIndex);
        out.writeInt(input.length());
        System.out.println("Written chunk header: index=" + chunkIndex + ", length=" + input.length());

        // Write the Huffman tree
        writeTrie(root, out);

        // Convert codes to bits
        BitSet bitSet = new BitSet();
        int bitIndex = 0;

        // Encode the chunk
        for (char ch : input.toCharArray()) {
            String code = huffmanCodes.get(ch);
            for (char bit : code.toCharArray()) {
                bitSet.set(bitIndex++, bit == '1');
            }
        }

        // Write the compressed data
        byte[] bytes = bitSet.toByteArray();
        out.writeInt(bytes.length);
        out.write(bytes);
        out.flush();
        System.out.println("Compressed data written for chunk " + chunkIndex);

        // Calculate and display compression ratio for this chunk
        int uncompressedSizeBytes = input.length();
        int compressedSizeBytes = bytes.length;
        double compressionRatio = (compressedSizeBytes > 0) ? (double) uncompressedSizeBytes / compressedSizeBytes : 0.0;
        System.out.println("Compression ratio for chunk " + chunkIndex + ": " + compressionRatio);
    }

    private static Node buildTrie(Map<Character, Integer> freqMap) {
        PriorityQueue<Node> pq = new PriorityQueue<>();
        for (Map.Entry<Character, Integer> entry : freqMap.entrySet()) {
            pq.add(new Node(entry.getKey(), entry.getValue(), null, null));
        }

        while (pq.size() > 1) {
            Node left = pq.poll();
            Node right = pq.poll();
            pq.add(new Node('\0', left.freq + right.freq, left, right));
        }
        return pq.poll();
    }

    private static void buildCode(Map<Character, String> huffmanCodes, Node x, String s) {
        if (x.isLeaf()) {
            huffmanCodes.put(x.ch, s.isEmpty() ? "0" : s);
        } else {
            buildCode(huffmanCodes, x.left, s + '0');
            buildCode(huffmanCodes, x.right, s + '1');
        }
    }

    private static void writeTrie(Node x, DataOutputStream out) throws IOException {
        if (x.isLeaf()) {
            out.writeBoolean(true);
            out.writeChar(x.ch);
        } else {
            out.writeBoolean(false);
            writeTrie(x.left, out);
            writeTrie(x.right, out);
        }
    }

    // Modified decompress method to handle chunks
    public static void decompressLargeFile(String compressedFilePath, String outputFilePath) throws IOException {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(compressedFilePath)));
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {

            // Read total file size
            long totalSize = in.readLong();
            long bytesProcessed = 0;
            System.out.println("Total size to be decompressed: " + totalSize);

            // Process chunks until we've read the entire file
            while (bytesProcessed < totalSize) {
                // Ensure there's enough data to read the chunk header and compressed data
                if (in.available() < 8) break; // Check if there's enough data for chunk header

                // Read chunk header
                int chunkIndex = in.readInt();
                int chunkLength = in.readInt();
                System.out.println("Reading chunk " + chunkIndex + " with length " + chunkLength);

                // Read and reconstruct Huffman tree for this chunk
                Node root = readTrie(in);

                // Read compressed data length
                if (in.available() < 4) break; // Check if there's enough data for the next length value
                int compressedLength = in.readInt();
                if (in.available() < compressedLength) break; // Ensure enough data for compressed data

                byte[] compressedData = new byte[compressedLength];
                in.readFully(compressedData);

                // Decompress chunk
                String decompressedChunk = decompressChunk(root, compressedData, chunkLength);
                writer.write(decompressedChunk);
                bytesProcessed += decompressedChunk.length();
                System.out.println("Decompressed chunk " + chunkIndex + " with " + decompressedChunk.length() + " bytes.");
            }
        }
    }

    private static Node readTrie(DataInputStream in) throws IOException {
        boolean isLeaf = in.readBoolean();
        if (isLeaf) {
            char ch = in.readChar();
            return new Node(ch, -1, null, null);
        } else {
            Node left = readTrie(in);
            Node right = readTrie(in);
            return new Node('\0', -1, left, right);
        }
    }

    private static String decompressChunk(Node root, byte[] compressedData, int length) {
        StringBuilder result = new StringBuilder(length);
        BitSet bitSet = BitSet.valueOf(compressedData);
        int bitIndex = 0;

        for (int i = 0; i < length; i++) {
            Node x = root;
            while (!x.isLeaf()) {
                boolean bit = bitSet.get(bitIndex++);
                x = bit ? x.right : x.left;
            }
            result.append(x.ch);
        }

        return result.toString();
    }

    public static void main(String[] args) {
        String inputFilePath = "C:\\Users\\avishi\\Downloads\\Dataset.csv";
        String compressedFilePath = "compressed" + COMPRESSION_EXTENSION;
        String decompressedFilePath = "decompressed" + DECOMPRESSION_EXTENSION;

        try {
            System.out.println("Starting file compression...");
            compressLargeFile(inputFilePath);
            System.out.println("Compression completed. Starting decompression...");

            decompressLargeFile(compressedFilePath, decompressedFilePath);
            System.out.println("Decompression completed successfully.");

            // Verify files
            verifyFiles(inputFilePath, decompressedFilePath);

        } catch (IOException e) {
            System.err.println("Error processing file: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void verifyFiles(String originalPath, String decompressedPath) throws IOException {
        try (BufferedReader original = new BufferedReader(new FileReader(originalPath));
             BufferedReader decompressed = new BufferedReader(new FileReader(decompressedPath))) {

            String originalLine, decompressedLine;
            long lineNumber = 1;

            while ((originalLine = original.readLine()) != null &&
                    (decompressedLine = decompressed.readLine()) != null) {
                if (!originalLine.equals(decompressedLine)) {
                    throw new IOException("Verification failed at line " + lineNumber);
                }
                lineNumber++;
            }

            if (original.readLine() != null || decompressed.readLine() != null) {
                throw new IOException("Files have different lengths");
            }

            System.out.println("File verification successful: original and decompressed files match");
        }
    }
}
