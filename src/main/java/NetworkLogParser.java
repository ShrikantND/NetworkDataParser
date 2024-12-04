/**
 * Starter
 */

import com.test.processing.LogLineProcessor;
import com.test.validation.LogFileValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class NetworkLogParser {

    private static final Logger logger = LogManager.getLogger(NetworkLogParser.class);

    /**
     * Tunable parameters. These can be converted in property file or can be accepted as
     * input from user
     */
    private static final int CHUNK_SIZE = 1000; // Adjust chunk size based on memory/CPU
    private static final int THREAD_POOL_SIZE = 10; // Adjust pool size as needed
    private static final int MAX_PROCESSING_TIME_IN_MINUTES = 60;


    public static void main(String[] args) {

        if (args.length < 2) {
            System.err.println("Usage: java Main <logFile> <lookupFile>");
            System.exit(1);
        }

        String logFile = args[0];
        String lookupFile = args[1];

        LogFileValidator fileValidator = new LogFileValidator();
        boolean isLogValid = fileValidator.validateFile(logFile);
        boolean isLookupValid = fileValidator.validateFile(lookupFile);

        if (isLogValid && isLookupValid) {
            String outputFile = "";

            try {
                LogLineProcessor processor = new LogLineProcessor(lookupFile,
                        CHUNK_SIZE, THREAD_POOL_SIZE, MAX_PROCESSING_TIME_IN_MINUTES);
                processor.processLogFile(logFile, outputFile);
            } catch (Exception e) {
                logger.error("Error: " + e.getMessage());
            }
        } else {
            System.err.println("Invalid log or lookup file. Exiting...");
        }
    }

}

//
//
//    // Define the protocol-to-tag map
//    private static Map<String, String> lookupTable = new HashMap<>();
//    private static Map<String, Integer> tagCounts = new ConcurrentHashMap<>();
//    private static Map<String, Integer> portProtocolCounts = new ConcurrentHashMap<>();
//
//    public static void main(String[] args) {
//
//        if (args.length != 2) {
//            System.err.println("Usage: java LogProcessor <input-file> <lookup-file>");
//            return;
//        }
//
//        String inputFile = args[0];
//        String lookupFile = args[1];
//
//        // Validate input and lookup files before proceeding
//        if (!FileUtils.validateFile(inputFile)) {
//            System.err.println("Invalid Network Flow Log file: " + inputFile);
//            return;
//        }
//
//        if (!FileUtils.validateFile(lookupFile)) {
//            System.err.println("Invalid lookup file: " + lookupFile);
//            return;
//        }
//
//        // Load lookup file into a map
//        try {
//            loadLookupTable(lookupFile);
//        } catch (IOException e) {
//            System.err.println("Error loading lookup file: " + e.getMessage());
//            return;
//        }
//
//        // Parse the input file and generate output
//        try {
//            processLogFile(inputFile);
//        } catch (IOException e) {
//            System.err.println("Error processing Network Flow Log file: " + e.getMessage());
//        }
//    }
//
//    private static void loadLookupTable(String lookupFile) throws IOException {
//        try (BufferedReader br = new BufferedReader(new FileReader(lookupFile))) {
//            String line;
//            while ((line = br.readLine()) != null) {
//                String[] fields = line.split(",");
//                if (fields.length == 3) {
//                    String portProtocol = fields[0].trim() + "," + fields[1].trim(); // Keep case-sensitive
//                    String tag = fields[2].trim(); // Keep case-sensitive
//                    lookupTable.put(portProtocol, tag); // Store with case-sensitivity
//                } else {
//                    System.err.println("Ignoring invalid entry in lookup file: " + line);
//                }
//            }
//        } catch (IOException e) {
//            throw new IOException("Error reading lookup file: " + e.getMessage(), e);
//        }
//    }
//
//    private static void processLogFile(String inputFile) throws IOException {
//        ExecutorService executorService = Executors.newFixedThreadPool(4);
//
//        BufferedReader br = null;
//        try {
//            br = new BufferedReader(new FileReader(inputFile));
//            List<String> lines = new ArrayList<>();
//            String line;
//
//            while ((line = br.readLine()) != null) {
//                lines.add(line);
//            }
//
//            // Batch process lines by submitting tasks to ExecutorService
//            int batchSize = 1000; // Batch size for multi-threading
//            for (int i = 0; i < lines.size(); i += batchSize) {
//                int end = Math.min(i + batchSize, lines.size());
//                List<String> batch = lines.subList(i, end);
//
//                executorService.submit(() -> processBatch(batch));
//            }
//        } catch (IOException e) {
//            throw new IOException("Error reading log file: " + e.getMessage(), e);
//        } finally {
//            if (br != null) {
//                try {
//                    br.close();
//                } catch (IOException e) {
//                    System.err.println("Error closing log file: " + e.getMessage());
//                }
//            }
//        }
//
//        executorService.shutdown();
//        try {
//            if (!executorService.awaitTermination(60, TimeUnit.MINUTES)) {
//                executorService.shutdownNow();
//            }
//        } catch (InterruptedException e) {
//            executorService.shutdownNow();
//            Thread.currentThread().interrupt();
//        }
//
//        // Write the results to output
//        try {
//            writeResults(tagCounts, portProtocolCounts);
//        } catch (IOException e) {
//            System.err.println("Error writing results to output file: " + e.getMessage());
//        }
//    }
//
//    private static void processBatch(List<String> batch) {
//        for (String line : batch) {
//            try {
//                // Validate the line
//                String[] fields = line.split(" ");
//                if (fields.length != 13) {
//                    System.out.println("Skipping invalid line: " + line);
//                    continue; // Skip invalid lines
//                }
//
//                String srcPort = fields[5];
//                String dstPort = fields[6];
//                String protocolNumber = fields[7];
//
//                // Process the tags from the lookup table
//                String portProtocolKey = dstPort + "," + resolveProtocol(Integer.parseInt(protocolNumber));
//                String tag = lookupTable.get(portProtocolKey); // Case-sensitive lookup
//
//                if (tag != null) {
//                    tagCounts.merge(tag, 1, Integer::sum); // Safely increment the tag count
//                }
//
//                // Count port/protocol combinations
//                String portProtocolCombo = dstPort + "," + resolveProtocol(Integer.parseInt(protocolNumber));
//                portProtocolCounts.merge(portProtocolCombo, 1, Integer::sum); // Safely increment the port/protocol count
//            } catch (NumberFormatException e) {
//                System.err.println("Invalid protocol number in line: " + line);
//            } catch (Exception e) {
//                System.err.println("Error processing line: " + e.getMessage());
//            }
//        }
//    }
//
//    private static void writeResults(Map<String, Integer> tagCounts, Map<String, Integer> portProtocolCounts) throws IOException {
//        BufferedWriter writer = new BufferedWriter(new FileWriter("output.txt"));
//
//        try {
//            writer.write("Tag Counts:\n");
//            for (Map.Entry<String, Integer> entry : tagCounts.entrySet()) {
//                writer.write(entry.getKey() + "," + entry.getValue() + "\n");
//            }
//
//            writer.write("\nPort/Protocol Combination Counts:\n");
//            for (Map.Entry<String, Integer> entry : portProtocolCounts.entrySet()) {
//                writer.write(entry.getKey() + "," + entry.getValue() + "\n");
//            }
//        } catch (IOException e) {
//            throw new IOException("Error writing to output file: " + e.getMessage(), e);
//        } finally {
//            writer.close();
//        }
//    }
//
//    private static String resolveProtocol(int protocolNumber) {
//        IpNumber ipNumber = IpNumber.getInstance((byte) protocolNumber);
//        return ipNumber != null ? ipNumber.name() : "unknown"; // Do not convert to lowercase
//    }
//}
