
# Flow Log Parser and Lookup Table

## Overview
This project contains a Java-based flow log parser that processes network flow logs, resolves tags based on a lookup table, and outputs relevant statistics. The core functionality includes parsing log files, resolving port/protocol combinations, and generating reports based on lookup data.

### Key Features:
- **Flow Log Parsing**: Efficiently processes flow logs to extract information based on protocol, port, and tags.
- **Lookup Table**: Resolves port and protocol combinations to associated tags from a CSV-like file.
- **Tagging and Reporting**: Generates reports on port/protocol counts and associated tags.
- **Threading Optimizations**: Designed to handle large files and process log lines asynchronously for improved performance.
- **Invalidation Handling**: Ensures correct processing of incomplete or invalid data.

## Setup and Requirements

### Prerequisites
- **Java 1.8 or higher**.
- **Gradle 7.x or higher** for building and running tests.

### Building and Running the Project
To build the project, use Gradle:
```bash
./gradlew build
```

To run the application:
```bash
java -jar build/libs/NetworkDataParser-1.0-SNAPSHOT.jar <logFilePath> <lookupFilePath> <outputFilePath>
```

### Directory Structure:
- `src/`: Source code for the application.
- `src/test/resources/`: Test files for unit and integration tests.
- `build.gradle`: Gradle build configuration.

## Core Components

### 1. `LogLineProcessor`
This class is responsible for parsing the flow logs. It processes each line from the input log files, extracts relevant fields such as port, protocol, and tag, and counts occurrences of each tag and port/protocol combination.

- **Threading Optimizations**: The log parsing process is optimized to handle large datasets efficiently. Multiple chunks of the log file can be processed in parallel, ensuring faster processing times without blocking the main thread.
  
- **Asynchronous Task Processing**: The class uses asynchronous processing for large chunks of logs. This reduces the overall runtime of log parsing by distributing the work across available threads.

### 2. `LookupTable`
This class loads the lookup table from a file and allows the resolution of tags for given port/protocol combinations.

- **File Parsing**: It reads a CSV-like file, ignoring invalid lines and entries. Each entry contains a port, protocol, and associated tag.
  
- **Invalidations**: The system ignores any incomplete or incorrectly formatted lines in the lookup file. If any invalid data is detected, the line is skipped to prevent errors from affecting the lookup functionality.

### 3. `Gradle Setup`
Gradle is used for building and managing the project dependencies. The configuration is optimized for compiling the application, running unit tests, and generating JAR artifacts.

- **Dependencies**: The project uses dependencies such as `JUnit 5` for testing and `Log4j` for logging.

## Unit and Integration Testing

### 1. `LookupTableTest`
Unit tests for the `LookupTable` class ensure that the lookup functionality works as expected. Tests include:
- Resolving tags for valid port/protocol combinations.
- Handling invalid entries in the lookup file.
- Printing the contents of the lookup table.

### 2. `LogLineProcessorTest`
Unit tests for the `LogLineProcessor` class validate the log line processing:
- Parsing valid log lines.
- Handling asynchronous log file processing.
- Verifying output against expected results.

### 3. Integration Test
The integration tests ensure that the `LogLineProcessor` and `LookupTable` classes work together correctly to produce the desired output.

## Optimization Details

### Threading and Asynchronous Processing
To handle large log files efficiently:
- **Parallel Chunk Processing**: The `LogLineProcessor` class processes chunks of log lines in parallel using asynchronous tasks. This reduces the overall time required for large files.
  
- **Thread Pool Management**: A custom thread pool is used to manage concurrent processing, allowing for efficient resource utilization while avoiding excessive context switching or thread contention.

- **Logging of Threading Operations**: The application logs key threading events, such as when a chunk is being processed asynchronously or when the main thread waits for threads to complete.

### Invalid Data Handling and Invalidation
- **Line Validation**: During both log parsing and lookup table loading, invalid or incomplete lines are ignored, ensuring that only correctly formatted data is processed. Currently only flow logs and look table with fix lines are considered. This can be easily customized and make extensible using configuration properties.
- **Graceful Error Handling**: The system gracefully handles errors related to invalid data, logging the issues without interrupting the overall flow of the application.

## Example Usage

### Running the Log Parser
1. Prepare a log file (`logFile.txt`) with flow log data.
2. Prepare a lookup file (`lookupFile.txt`) containing port, protocol, and tags in CSV format.
3. Run the parser:
   ```bash
   java -jar build/libs/NetworkDataParser-1.0-SNAPSHOT.jar src/test/resources/logFile.txt src/test/resources/lookupFile.txt src/test/resources/outputFile.txt
   ```

### Sample Output:
The output will contain:
- **Port/Protocol Combinations**: A list of unique combinations with their counts. All the port protocol combinations are considered as there is no defined criteria provided.
- **Tag Counts**: The count of logs matching each tag.

## Troubleshooting

- **File Not Found**: Ensure that the log and lookup files are correctly specified in the command line.
- **Missing Tags**: If tags are missing, verify that the lookup file is properly formatted and contains all required entries for the respective ports and protocols.
