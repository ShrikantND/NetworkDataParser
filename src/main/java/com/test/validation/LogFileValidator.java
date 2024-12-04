package com.test.validation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

public class LogFileValidator {
    private static final Logger logger = LogManager.getLogger(LogFileValidator.class);

    public boolean validateFile(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            logger.error("File does not exist: {}", filePath);
            return false;
        }
        if (!file.canRead()) {
            logger.error("Cannot read file: {}", filePath);
            return false;
        }
        return true;
    }
}
