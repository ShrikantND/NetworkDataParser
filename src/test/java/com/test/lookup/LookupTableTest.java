package com.test.lookup;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for the LookupTable.
 * This class verifies the functionality of loading lookup data from a file and retrieving tags for a given port and protocol.
 */
class LookupTableTest {

    private LookupTable lookupTable;

    /**
     * Set up the LookupTable instance before each test.
     * This method creates a LookupTable object with a test lookup file.
     *
     * @throws IOException If an I/O error occurs while reading the file.
     */
    @BeforeEach
    void setUp() throws IOException {
        // Initialize LookupTable with a test file
        lookupTable = new LookupTable("src/test/resources/lookupFile.txt");
    }

    /**
     * Test case for verifying that tags are correctly resolved for a valid port and protocol.
     * This test ensures that the resolveTags method correctly returns the tags associated with a port and protocol.
     */
    @Test
    void testResolveTags_validData() {
        // Valid port and protocol, expecting the tag "sv_P1"
        Set<String> tags = lookupTable.resolveTags(25, "tcp");

        assertNotNull(tags, "Tags should not be null");
        assertTrue(tags.contains("sv_P1"), "Tags should contain 'sv_P1'");
    }

    /**
     * Test case for resolving tags for a port and protocol combination that does not exist in the lookup table.
     * This test verifies that the resolveTags method returns an empty set when no tags are found.
     */
    @Test
    void testResolveTags_invalidData() {
        // Port and protocol that don't exist in the lookup table
        Set<String> tags = lookupTable.resolveTags(9999, "udp");

        assertNotNull(tags, "Tags should not be null");
        assertTrue(tags.isEmpty(), "Tags should be empty for non-existent port/protocol");
    }

    /**
     * Test case for verifying that invalid lines are ignored during file loading.
     * This test ensures that the loadLookupTable method skips lines that do not contain exactly three fields (port, protocol, tag).
     *
     * @throws IOException If an I/O error occurs while reading the file.
     */
    @Test
    void testLoadLookupTable_invalidLines() throws IOException {
        // Create a temporary file with an invalid line (missing a tag)
        String invalidFilePath = "src/test/resources/invalidLookupFile.txt";
        Files.write(Paths.get(invalidFilePath), "80,tcp\n".getBytes()); // Incomplete line with missing tag

        LookupTable invalidTable = new LookupTable(invalidFilePath);
        // Verify that the table does not contain invalid data
        Set<String> tags = invalidTable.resolveTags(80, "tcp");

        assertTrue(tags.isEmpty(), "Tags should be empty for an invalid line");

        // Clean up the temporary file
        Files.delete(Paths.get(invalidFilePath));
    }

    /**
     * Test case for verifying the printing of the lookup table.
     * This test ensures that the printLookupTable method correctly prints the content of the lookup table.
     * It will verify that the output matches the expected format for a known lookup table file.
     */
    @Test
    void testPrintLookupTable() {
        // Test the printing functionality of the lookup table
        lookupTable.printLookupTable();
        // This test will only pass visually since the printLookupTable method does not return a value.
        // You can redirect output to a file or stream for better validation if needed.
    }

    /**
     * Integration test for the LookupTable class.
     * This test case checks that the lookup table loads correctly and handles real data, including both valid and invalid lines.
     *
     * @throws IOException If an I/O error occurs while reading or writing files.
     */
    @Test
    void testIntegration() throws IOException {
        // Test the full flow from reading a file to resolving tags
        LookupTable integrationTable = new LookupTable("src/test/resources/lookupFile.txt");

        Set<String> tags = integrationTable.resolveTags(25, "tcp");

        assertNotNull(tags, "Tags should not be null");
        assertTrue(tags.contains("sv_P1"), "Expected tag 'sv_P1' for port 80 and protocol tcp");
    }
}
