package com.test.lookup;

import java.io.*;
import java.util.*;

/**
 * LookupTable class is responsible for loading a lookup table from a file and providing functionality
 * to retrieve associated tags for a given port and protocol.
 * The table maps port numbers to protocols and associated tags.
 */
public class LookupTable {

    // The lookup table structure: a map where the key is the port number,
    // and the value is another map of protocols and their associated tags.
    private final Map<Integer, Map<String, Set<String>>> lookupTable;

    /**
     * Constructor that initializes the lookup table by loading data from the specified file.
     *
     * @param filePath The file path to the lookup data (CSV format: Port, Protocol, Tag).
     * @throws IOException If an I/O error occurs while reading the file.
     */
    public LookupTable(String filePath) throws IOException {
        lookupTable = new HashMap<>();
        loadLookupTable(filePath);
    }

    /**
     * Loads the lookup table from the given file.
     * The file is expected to have lines in the format: port, protocol, tag
     * Invalid lines (not matching this format) are skipped.
     *
     * @param filePath The file path to the lookup data.
     * @throws IOException If an I/O error occurs while reading the file.
     */
    private void loadLookupTable(String filePath) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            // Read the file line by line
            while ((line = reader.readLine()) != null) {
                // Split each line by commas
                String[] parts = line.split(",");
                if (parts.length != 3) {
                    continue; // Skip invalid lines with incorrect number of columns
                }
                // Parse port, protocol, and tag from the line
                int port = Integer.parseInt(parts[0].trim());  // Port number (integer)
                String protocol = parts[1].trim().toLowerCase();  // Protocol name (converted to lowercase)
                String tag = parts[2].trim();  // Tag associated with the port/protocol

                // Update the lookup table:
                // For each port, if the protocol does not exist, create a new entry for it, and then add the tag.
                lookupTable
                        .computeIfAbsent(port, k -> new HashMap<>())  // Ensure port exists in the map
                        .computeIfAbsent(protocol, k -> new HashSet<>())  // Ensure protocol exists for the port
                        .add(tag);  // Add the tag to the protocol set
            }
        }
    }

    /**
     * Retrieves the set of tags for a given port and protocol from the lookup table.
     * If no tags are found, returns an empty set.
     *
     * @param port     The port number.
     * @param protocol The protocol name (case-insensitive).
     * @return A set of tags associated with the specified port and protocol.
     */
    public Set<String> resolveTags(int port, String protocol) {
        // Fetch tags using port and protocol (case-insensitive). If not found, return an empty set.
        return lookupTable.getOrDefault(port, Collections.emptyMap())
                .getOrDefault(protocol.toLowerCase(), Collections.emptySet());
    }

    /**
     * Only for debugging purposes: prints the contents of the lookup table.
     * This will display the port, protocol, and associated tags for each entry in the table.
     */
    public void printLookupTable() {
        // Iterate over all entries in the lookup table and print them
        for (Map.Entry<Integer, Map<String, Set<String>>> portEntry : lookupTable.entrySet()) {
            int port = portEntry.getKey();
            for (Map.Entry<String, Set<String>> protocolEntry : portEntry.getValue().entrySet()) {
                String protocol = protocolEntry.getKey();
                Set<String> tags = protocolEntry.getValue();
                System.out.println("Port: " + port + ", Protocol: " + protocol + ", Tags: " + tags);
            }
        }
    }
}
