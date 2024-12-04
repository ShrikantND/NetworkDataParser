package com.test.processing;

import com.test.lookup.LookupTable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pcap4j.packet.namednumber.IpNumber;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * LogLineProcessor class is responsible for processing a log file, extracting relevant details from each line,
 * resolving tags based on lookup, and providing counts for tags, protocol-port combinations, and untagged entries.
 * It uses multithreading to process chunks of lines concurrently for performance optimization.
 */
public class LogLineProcessor {

    private static final Logger logger = LogManager.getLogger(LogLineProcessor.class);
    private final LookupTable lookupTable; // LookupTable instance to resolve tags
    private final int chunkSize; // Size of the chunks in which lines are processed concurrently
    private final int maxProcessingTime; // Maximum time allowed for processing
    private final ExecutorService executorService; // Executor for multithreading
    private final Map<Integer, String> protocolCache = new HashMap<>(); // Cache for protocol numbers
    private final Map<String, Integer> tagCount = new ConcurrentHashMap<>(); // Tag count map
    private final Map<String, Integer> portProtocolCount = new ConcurrentHashMap<>(); // Port-Protocol combination count map
    private final AtomicInteger untaggedCount = new AtomicInteger(0); // Counter for untagged entries using AtomicInteger for thread-safety

    /**
     * Constructor initializes the LogLineProcessor with necessary parameters.
     *
     * @param lookupFile Path to the file containing lookup data for tags.
     * @param chunkSize Number of lines to process concurrently in each thread.
     * @param threadPoolSize Number of threads in the pool for parallel processing.
     * @param maxProcessingTime Maximum time (in minutes) to allow for processing before shutting down threads.
     * @throws IOException If there is an error while reading the lookup file.
     */
    public LogLineProcessor(String lookupFile, int chunkSize, int threadPoolSize, int maxProcessingTime) throws IOException {
        this.chunkSize = chunkSize;
        this.maxProcessingTime = maxProcessingTime;
        this.lookupTable = new LookupTable(lookupFile);
        this.executorService = Executors.newFixedThreadPool(threadPoolSize); // Create a thread pool with a fixed number of threads
    }

    /**
     * Main method to process the log file, split into chunks for parallel processing.
     *
     * @param inputFile Path to the input log file to process.
     * @param outputFile Path to the output file where results will be written.
     */
    public void processLogFile(String inputFile, String outputFile) {
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
            List<Future<Void>> tasks = new ArrayList<>(); // List to store futures of tasks
            List<String> linesBuffer = new ArrayList<>(chunkSize); // Buffer to hold lines for each chunk
            String line;

            // Read the input file line by line and create chunks for processing
            while ((line = reader.readLine()) != null) {
                linesBuffer.add(line);

                // If buffer is full, process the chunk
                if (linesBuffer.size() == chunkSize) {
                    tasks.add(processChunk(new ArrayList<>(linesBuffer)));
                    linesBuffer.clear(); // Clear buffer for the next chunk
                }
            }

            // Process the remaining lines if any
            if (!linesBuffer.isEmpty()) {
                tasks.add(processChunk(new ArrayList<>(linesBuffer)));
            }

            // Wait for all tasks to complete
            for (Future<Void> task : tasks) {
                try {
                    task.get(); // Blocks until the task completes
                } catch (ExecutionException | InterruptedException e) {
                    logger.error("Error in processing task: " + e.getMessage());
                }
            }

            // Log the count of untagged entries
            logger.info("Total untagged entries: " + untaggedCount.get());

            // Write the results to the output file
            writeResultsToFile(outputFile);

        } catch (IOException e) {
            logger.error("Error processing log file: " + e.getMessage());
        } finally {
            shutdownExecutor(); // Ensure the executor service is properly shut down
        }
    }

    /**
     * Processes a chunk of log lines in a separate thread.
     *
     * @param lines List of log lines to process.
     * @return Future object representing the result of the asynchronous task.
     */
    Future<Void> processChunk(List<String> lines) {
        return executorService.submit(() -> {
            Set<Integer> protocolNumbers = new HashSet<>(); // Set to collect protocol numbers from the chunk
            for (String line : lines) {
                processLine(line, protocolNumbers); // Process each line
            }
            resolveAndCacheProtocols(protocolNumbers); // Resolve and cache protocols for the chunk
            return null; // Callable requires a return value
        });
    }

    /**
     * Processes an individual log line, extracts relevant data, and updates the counts.
     *
     * @param line Log line to process.
     * @param protocolNumbers Set to collect protocol numbers for later caching.
     */
    void processLine(String line, Set<Integer> protocolNumbers) {
        try {
            String[] fields = line.split(" "); // Split the line by spaces
            if (fields.length >= 13) { // Ensure the line has enough data
                int destPort = Integer.parseInt(fields[6]); // Extract destination port
                int protocolNum = Integer.parseInt(fields[7]); // Extract protocol number
                protocolNumbers.add(protocolNum); // Add protocol number to the set

                // Resolve tags based on the destination port and protocol
                Set<String> tags = lookupTable.resolveTags(destPort, resolveProtocol(protocolNum));

                // If no tags are found, increment the untagged count
                if (tags.isEmpty()) {
                    untaggedCount.incrementAndGet(); // Thread-safe increment of untagged count
                } else {
                    // Otherwise, update the tag count
                    for (String tag : tags) {
                        tagCount.merge(tag, 1, Integer::sum); // Increment count of each tag
                    }
                }

                // Track port-protocol combinations
                String portProtocolKey = destPort + "," + resolveProtocol(protocolNum);
                portProtocolCount.merge(portProtocolKey, 1, Integer::sum); // Increment port-protocol combination count
            }
        } catch (Exception e) {
            logger.error("Error processing log line: " + e.getMessage());
        }
    }

    /**
     * Resolves and caches protocol names based on protocol numbers.
     *
     * @param protocolNumbers Set of protocol numbers to resolve.
     */
    private void resolveAndCacheProtocols(Set<Integer> protocolNumbers) {
        for (Integer protocolNum : protocolNumbers) {
            protocolCache.computeIfAbsent(protocolNum, this::resolveProtocol); // Resolve and cache protocol if not already present
        }
    }

    /**
     * Resolves the protocol name based on the protocol number.
     *
     * @param protocolNum The protocol number.
     * @return The protocol name.
     */
    String resolveProtocol(int protocolNum) {
        IpNumber ipNumber = IpNumber.getInstance((byte) protocolNum);
        return (ipNumber != null) ? ipNumber.name().toLowerCase() : "unknown"; // Return protocol name or "unknown" if not found
    }

    /**
     * Writes the processed results (tag counts, port/protocol combinations, and untagged count) to the output file.
     *
     * @param outputFile Path to the output file.
     */
    private void writeResultsToFile(String outputFile) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            writer.write("Tag Counts:\n");
            for (Map.Entry<String, Integer> entry : tagCount.entrySet()) {
                writer.write(entry.getKey() + "," + entry.getValue() + "\n");
            }

            // Write the count of untagged entries to the output file
            writer.write("Untagged,"+String.valueOf(untaggedCount.get()) + "\n");

            writer.write("\nPort/Protocol Combination Counts:\n");
            for (Map.Entry<String, Integer> entry : portProtocolCount.entrySet()) {
                writer.write(entry.getKey() + "," + entry.getValue() + "\n");
            }

        } catch (IOException e) {
            logger.error("Error writing to output file: " + e.getMessage());
        }
    }

    /**
     * Shuts down the executor service and waits for all tasks to complete.
     */
    private void shutdownExecutor() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(maxProcessingTime, TimeUnit.MINUTES)) {
                logger.error("Executor did not terminate in the specified time.");
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            logger.error("Interrupted while waiting for executor termination: " + e.getMessage());
            executorService.shutdownNow();
        }
    }
}
