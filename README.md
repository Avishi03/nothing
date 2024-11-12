# OOPS Project 


## Avishi Agrawal 
## 23BDS074


## Intoduction:
This Java program is designed to handle the compression and decompression of large CSV files using Huffman coding. The program works by dividing the file into smaller chunks, compressing each chunk, and then storing them in a compressed format. It also supports the decompression of these chunks back into their original form. Let me walk you through the main components and functionality of the code:

## Key Components:
### Constants:

CHUNK_SIZE: Defines the size of each chunk for processing (1MB).
MAX_BUFFER_SIZE: Maximum buffer size for reading.
COMPRESSION_EXTENSION: File extension for compressed files.
DECOMPRESSION_EXTENSION: File extension for decompressed files.
CsvParser class: This class contains methods for both compression and decompression of large files. It is a utility class and is not intended to be instantiated.

### Node class:

Represents a node in the Huffman tree, used for encoding and decoding characters.
Each node holds a character (ch), its frequency (freq), and references to left and right child nodes.
The class implements Comparable to allow sorting of nodes based on frequency (used for building the Huffman tree).


### Compression Method (compressLargeFile):

This method reads the input file in chunks (using a BufferedReader), compresses each chunk separately, and writes the compressed data to a binary file.
For each chunk:
A frequency map is created for the characters in that chunk.
A Huffman tree (trie) is built based on the frequencies of the characters.
The tree is used to generate Huffman codes, which are then used to compress the chunk into bits.
The chunk's compressed data, along with metadata like the chunk index and length, is written to the output file.

### Compression Details (compressChunk):

For each chunk, a frequency map is created for the characters.
A Huffman tree is built using a priority queue (PriorityQueue<Node>), where nodes are sorted by frequency.
The tree is then used to generate Huffman codes for each character.
The compressed data is written in the form of bits, and a compression ratio is calculated for the chunk.


### Decompression Method (decompressLargeFile):

This method reads the compressed file, reconstructs the Huffman tree for each chunk, and decompresses the data.
The program reads the total file size and then processes each chunk sequentially.
For each chunk:
The header (which contains the chunk index and size) is read.
The Huffman tree for the chunk is read.
The compressed data is decoded using the Huffman tree, and the decompressed text is written to the output file.

### Decompression Details (decompressChunk):

This method reconstructs the original data from the compressed byte array using the Huffman tree.
It uses a BitSet to handle the binary representation of the compressed data.
The decompressed characters are appended to a StringBuilder and returned.


### Huffman Tree Construction and Serialization:

The Huffman tree is recursively built using the buildTrie and buildCode methods.
The tree is written to the output file using the writeTrie method, which serializes the tree structure.
During decompression, the tree is reconstructed using the readTrie method.

###  File Verification (verifyFiles):

After decompression, the program compares the original file and the decompressed file line by line to ensure the integrity of the decompressed file.
If any differences are found, it throws an IOException.

### Main Method (main):

It sets up paths for the input, compressed, and decompressed files.
It calls the compressLargeFile method to compress the file, followed by the decompressLargeFile method to decompress it.
After decompression, it calls verifyFiles to check if the decompressed file matches the original.

### Detailed Steps:
### Compression:

### Read the input CSV file in 1MB chunks.
For each chunk:
Generate a frequency map for characters in that chunk.
Build the Huffman tree from the frequency map.
Write the Huffman tree and compressed data (in bit format) to the output file.
Write the total file size at the start of the output file.

### Decompression:

Read the compressed file, starting with the total file size.
For each chunk:
Read the chunk header (index and length).
Reconstruct the Huffman tree.
Decode the compressed data using the Huffman tree.
Write the decompressed data to the output file.

### File Verification:

After decompression, compare the original and decompressed files line by line to ensure they match.

## Important Notes:
Huffman Coding: A lossless data compression algorithm where frequent characters are represented with shorter codes. The tree is used to efficiently encode and decode data.


## Chunking:
The large file is processed in chunks to handle memory constraints when dealing with very large files. This ensures that even large files can be compressed and decompressed without running into memory issues.


## Potential Enhancements:
Error handling could be improved in certain sections (for example, when verifying file integrity).
The program could be optimized for parallel processing of chunks for faster compression and decompression of large files.
In summary, the code provides a way to efficiently compress and decompress large CSV files using Huffman coding while ensuring that the decompressed data matches the original. The program is designed to handle large files in manageable chunks, which is especially useful for systems with memory limitations.
