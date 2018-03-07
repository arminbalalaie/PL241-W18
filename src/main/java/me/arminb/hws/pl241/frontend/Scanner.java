package me.arminb.hws.pl241.frontend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class Scanner {
    private static Scanner instance;
    private final static Logger logger = LoggerFactory.getLogger(Scanner.class);
    private FileReader fileReader;
    private Token currentToken;
    private int lastNumber;
    private int lastIdentifier;
    private HashMap<Integer, String> id2StringMap;
    private HashMap<String, Integer> string2IdMap;
    private HashMap<String, Token> reservedWordsMap;
    private int identifierCounter;


    public static Scanner getInstance() {
        if (instance == null) {
            instance = new Scanner();
            prePopulateWithBuiltinFunctions();
        }
        return instance;
    }

    private static void prePopulateWithBuiltinFunctions() {
        instance.addIdentifier("InputNum"); // 0
        instance.addIdentifier("x"); // 1 for OutputNum(x)
        instance.addIdentifier("OutputNum"); // 2
        instance.addIdentifier("OutputNewLine"); // 3

    }

    private Scanner() {
        fileReader = FileReader.getInstance();
        id2StringMap = new HashMap<>();
        string2IdMap = new HashMap<>();
        reservedWordsMap = populateReservedWordsMap();
        identifierCounter = 0;

        fileReader.next();
    }

    private HashMap<String, Token> populateReservedWordsMap() {
        HashMap<String, Token> retMap = new HashMap<>();
        Token[] reservedTokens = {
                Token.THEN, Token.DO, Token.OD, Token.IF, Token.FI, Token.ELSE, Token.WHILE, Token.CALL,
                Token.VAR, Token.ARRAY, Token.LET, Token.FUNCTION, Token.PROCEDURE, Token.MAIN, Token.RETURN
        };

        for (Token token: reservedTokens) {
            retMap.put(token.getStrValue(), token);
        }

        return retMap;
    }

    public Token next() {
        currentToken = getNextToken();
        return currentToken;
    }

    private Token getNextToken() {
        char currentChar = fileReader.getCurrentChar();

        // skip whitespace
        while (Character.isWhitespace(currentChar)) {
            currentChar = fileReader.next();
        }

        // check for error, eof and one character tokens that are not the first char of any other tokens
        switch(currentChar) {
            case (char) 0: fileReader.next(); return Token.ERROR;
            case (char) -1: fileReader.next(); return Token.END_OF_FILE;
            case '*': fileReader.next(); return Token.TIMES;
            case '/': fileReader.next(); return Token.DIV;
            case '+': fileReader.next(); return Token.PLUS;
            case '-': fileReader.next(); return Token.MINUS;
            case '.': fileReader.next(); return Token.PERIOD;
            case ',': fileReader.next(); return Token.COMMA;
            case '[': fileReader.next(); return Token.OPEN_BRACKET;
            case ']': fileReader.next(); return Token.CLOSE_BRACKET;
            case ')': fileReader.next(); return Token.CLOSE_PARENTHESIS;
            case '(': fileReader.next(); return Token.OPEN_PARENTHESIS;
            case ';': fileReader.next(); return Token.SEMICOLON;
            case '}': fileReader.next(); return Token.END;
            case '{': fileReader.next(); return Token.BEGIN;
            default: break;
        }

        if (Character.isDigit(currentChar)) {
            // only numbers start with digits
            int scannedNumber = scanNumber();
            lastNumber = scannedNumber;
            return Token.NUMBER;
        } else if (Character.isLetter(currentChar)) {
            // identifiers or reserved words start with letters
            String scannedIdentifier = scanIdentifier();
            Token reservedToken = getReservedToken(scannedIdentifier);
            if (reservedToken != null) {
                return reservedToken;
            }

            // check for existing identifiers
            if (string2IdMap.containsKey(scannedIdentifier)) {
                lastIdentifier = string2IdMap.get(scannedIdentifier);
            } else {
                addIdentifier(scannedIdentifier);
            }
            return Token.IDENTIFIER;
        } else if(currentChar == '>' || currentChar == '<' || currentChar == '=' || currentChar == '!') {
            return scanRelationalOrBecomesToken();
        } else {
            fileReader.next();
            return Token.ERROR;
        }
    }

    private void addIdentifier(String identifier) {
        id2StringMap.put(identifierCounter, identifier);
        string2IdMap.put(identifier, identifierCounter);
        lastIdentifier = identifierCounter++;
    }

    private Token getReservedToken(String token) {
        return reservedWordsMap.get(token);
    }

    private int scanNumber() {
        StringBuilder numberStringBuilder = new StringBuilder();
        char currentChar;
        do {
            currentChar = fileReader.getCurrentChar();
            numberStringBuilder.append(currentChar);
            currentChar = fileReader.next();
        } while(Character.isDigit(currentChar));
        return Integer.parseInt(numberStringBuilder.toString());
    }

    private String scanIdentifier() {
        StringBuilder identifierStringBuilder = new StringBuilder();
        char currentChar;
        do {
            currentChar = fileReader.getCurrentChar();
            identifierStringBuilder.append(currentChar);
            currentChar = fileReader.next();
        } while(Character.isLetter(currentChar) || Character.isDigit(currentChar));
        return identifierStringBuilder.toString();
    }

    private Token scanRelationalOrBecomesToken() {
        char currentChar = fileReader.getCurrentChar();
        if (currentChar == '=') {
            currentChar = fileReader.next();
            if (currentChar == '=') { // ==
                fileReader.next();
                return Token.EQUAL;
            } else {
                expectationError("=", currentChar);
                return Token.ERROR;
            }
        } else if (currentChar == '!') {
            currentChar = fileReader.next();
            if (currentChar == '=') { // !=
                fileReader.next();
                return Token.NOT_EQUAL;
            } else {
                expectationError("=", currentChar);
                return Token.ERROR;
            }
        } else if (currentChar == '>') {
            currentChar = fileReader.next();
            if (currentChar == '=') { // >=
                fileReader.next();
                return Token.GREATER_THAN_OR_EQUAL;
            } else { // >
                return Token.GREATER_THAN;
            }
        } else { // <
            currentChar = fileReader.next();
            if (currentChar == '=') { // <=
                fileReader.next();
                return Token.LESS_THAN_OR_EQUAL;
            } else if (currentChar == '-') { // <-
                fileReader.next();
                return Token.BECOMES;
            } else { // <
                return Token.LESS_THAN;
            }
        }
    }

    private void expectationError(String expected, String received) {
        error("Expected " + expected + " - Received " + received + ".");
    }

    private void expectationError(String expected, char received) {
        expectationError(expected, String.valueOf(received));
    }

    private void error(String errorMessage) {
        logger.error(errorMessage + " at Line: " + fileReader.getCurrentLineNumber() + " Column: " + fileReader.getCurrentLinePointer());
    }

    public String identifierToString(int id) {
        return id2StringMap.get(id);
    }

    public int stringToIdentifier(String id) {
        return string2IdMap.get(id);
    }

    public int getLastNumber() {
        return lastNumber;
    }

    public int getLastIdentifier() {
        return lastIdentifier;
    }

    public Token getCurrentToken() {
        return currentToken;
    }

    public int getCurrentLinePointer() {
        return fileReader.getCurrentLinePointer();
    }

    public long getCurrentLineNumber() {
        return fileReader.getCurrentLineNumber();
    }
}
