package com.test.processing;

import com.test.lookup.LookupTable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the LogLineProcessor class.
 * Tests include validating log line processing, file processing, asynchronous chunk processing,
 * and integration of the processor with expected output.
 */
class LogLineProcessorTest {

    private static final Logger logger = LogManager.getLogger(LogLineProcessorTest.class);
    private LogLineProcessor logLineProcessor;
    private LookupTable mockLookupTable;

    /**
     * Set up the test environment.
     * Creates a mock LookupTable and initializes LogLineProcessor with necessary configuration.
     */
    @BeforeEach
    void setUp() throws IOException {
        // Mock LookupTable for unit testing
        mockLookupTable = mock(LookupTable.class);
        Set<String> tags = new HashSet<>();
        tags.add("sv_P1");
        // Stub the resolveTags method to return predefined tags for specific port/protocol combination
        when(mockLookupTable.resolveTags(80, "tcp")).thenReturn(tags);

        // Initialize LogLineProcessor with mock LookupTable and test configurations
        logLineProcessor = new LogLineProcessor("src/test/resources/lookupFile.txt", 100, 4, 2);
    }

    /**
     * Test processing of a single log line with valid data.
     * This ensures that the protocol is correctly processed and resolved.
     */
    @Test
    void testProcessLogLine_validData() {
        String logLine = "2 123456789012 eni-0a1b2c3d 10.0.1.201 198.51.100.2 443 49153 6 25 20000 1620140761 1620140821 ACCEPT OK";

        Set<Integer> protocolNumbers = new HashSet<>();
        logLineProcessor.processLine(logLine, protocolNumbers);

        // Verify protocol is processed correctly
        assertTrue(protocolNumbers.contains(6)); // Protocol 6 (TCP) should be processed
        // Verify protocol resolution works as expected
        assertEquals("tcp", logLineProcessor.resolveProtocol(6)); // TCP should be resolved for protocol number 6
    }

    /**
     * Test processing a log file and comparing the output with an expected result.
     * This ensures that the entire log file is processed correctly and output matches expected results.
     */
    @Test
    void testProcessLogFile_withOutputValidation() throws IOException {
        String logFilePath = "src/test/resources/logFile.txt";
        String outputFilePath = "src/test/resources/outputFile.txt";
        String expectedOutputFile = "src/test/resources/expectedOutput.txt";

        logLineProcessor.processLogFile(logFilePath, outputFilePath);

        // Compare the actual output file with the expected output
        String expectedOutput = new String(Files.readAllBytes(Paths.get(expectedOutputFile)), StandardCharsets.UTF_8);
        String actualOutput = new String(Files.readAllBytes(Paths.get(outputFilePath)), StandardCharsets.UTF_8);
        assertEquals(expectedOutput, actualOutput); // Ensure that both output files are identical
    }

    /**
     * Test processing a chunk of log lines asynchronously.
     * This checks that asynchronous processing works and verifies protocol caching.
     * The test waits briefly to allow asynchronous tasks to complete.
     */
    @Test
    void testProcessChunk_asynchronousValidation() throws Exception {
        List<String> chunk = Arrays.asList(
                "2 123456789012 eni-0a1b2c3d 10.0.1.201 198.51.100.2 443 49153 6 25 20000 1620140761 1620140821 ACCEPT OK",
                "2 123456789012 eni-0a1b2c3d 10.0.1.202 198.51.100.3 80 49154 6 30 25000 1620140821 1620140881 ACCEPT OK"
        );

        logLineProcessor.processChunk(chunk);

        // Allow asynchronous tasks to complete
        Thread.sleep(1000);

        // Validate cached protocol and check that it's correctly resolved
        assertEquals("tcp", logLineProcessor.resolveProtocol(6));
        // Additional assertions can be added based on processed data
    }

    /**
     * Integration test to validate the entire flow of the LogLineProcessor.
     * This test runs the processor with input log and lookup files and compares the output to expected results.
     */
    @Test
    public void testLogLineProcessorIntegration() throws IOException {
        String LOOKUP_FILE = "src/test/resources/lookupFile.txt";
        String LOG_FILE = "src/test/resources/logFile.txt";
        String EXPECTED_OUTPUT_FILE = "src/test/resources/expectedOutput.txt";
        String ACTUAL_OUTPUT_FILE = "src/test/resources/outputFile.txt";

        // Create a new LogLineProcessor and run the processing
        LogLineProcessor processor = new LogLineProcessor(LOOKUP_FILE, 5, 2, 1);
        processor.processLogFile(LOG_FILE, ACTUAL_OUTPUT_FILE);

        // Compare the actual output file with the expected output
        String expectedOutput = new String(Files.readAllBytes(Paths.get(EXPECTED_OUTPUT_FILE)), StandardCharsets.UTF_8);
        String actualOutput = new String(Files.readAllBytes(Paths.get(ACTUAL_OUTPUT_FILE)), StandardCharsets.UTF_8);

        // Ensure that both files have identical content
        assertEquals(expectedOutput, actualOutput);
    }
}
