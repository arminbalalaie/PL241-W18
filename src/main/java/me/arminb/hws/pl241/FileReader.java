package me.arminb.hws.pl241;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Paths;

public class FileReader {
    private final static Logger logger = LoggerFactory.getLogger(FileReader.class);
    private java.util.Scanner scanner;
    private String fileName;
    private int currentLinePointer;
    private String currentLine;
    private long currentLineNumber;
    private char currentChar;

    public FileReader(String fileName, Charset charset) {
        this.fileName = Paths.get(fileName).toAbsolutePath().toString();
        currentLineNumber = 0;
        currentLinePointer = 0;
        currentLine = null;
        currentChar = (char) 0;

        try {
            logger.info("Opening " + this.fileName + " for parsing ..");
            scanner = new java.util.Scanner(new InputStreamReader(new FileInputStream(fileName), charset));
        } catch (FileNotFoundException e) {
            throw new RuntimeException("File " + this.fileName + " does not exist!");
        }

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                closeFile();
            }
        }));
    }

    public char next() {
        if (currentLine == null || currentLinePointer == currentLine.length()) {
            if (!scanner.hasNextLine()) {
                currentChar = (char)-1;
                return currentChar;
            }

            currentLine = scanner.nextLine();
            currentLineNumber++;
            if (scanner.ioException() != null) {
                throw new RuntimeException("Error while reading the input file at Line: " + getCurrentLineNumber() +
                        " Column: " + getCurrentLinePointer() + "!");
            }
            currentLinePointer = 0;

            currentChar = ' ';
            return currentChar;
        }

        currentChar = currentLine.charAt(currentLinePointer++);
        return currentChar;
    }

    private void closeFile() {
        logger.info("Closing input file " + this.fileName + " ..");
        scanner.close();
    }

    public int getCurrentLinePointer() {
        return currentLinePointer;
    }

    public long getCurrentLineNumber() {
        return currentLineNumber;
    }

    public char getCurrentChar() {
        return currentChar;
    }
}
