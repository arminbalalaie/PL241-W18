package me.arminb.hws.pl241.frontend;

import me.arminb.hws.pl241.cfg.ControlFlowGraph;
import me.arminb.hws.pl241.symbol.Symbol;
import me.arminb.hws.pl241.symbol.SymbolTable;
import me.arminb.hws.pl241.symbol.SymbolType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class Parser {
    private static Parser instance;
    private final static Logger logger = LoggerFactory.getLogger(Parser.class);
    private Scanner scanner;
    private int lastSeenNumber;
    private int lastSeenIdentifier;

    public static Parser getInstance() {
        if (instance == null) {
            instance = new Parser();
        }
        return instance;
    }

    private Parser() {
        this.scanner = Scanner.getInstance();
    }

    public void parse() {
        Token currentToken;
        currentToken = scanner.next();
        if (currentToken != Token.END_OF_FILE)
            computation();
    }

    public void exitError(String errorMessage) {
        throw new RuntimeException(errorMessage + " at Line: " + scanner.getCurrentLineNumber() + " Column: " + scanner.getCurrentLinePointer());
    }

    public void error(String errorMessage) {
        logger.error(errorMessage + " at Line: " + scanner.getCurrentLineNumber() + " Column: " + scanner.getCurrentLinePointer());
    }

    public void expectationError(Token expected, Token received) {
        error("Expected " + expected + " - Received " + received + ".");
    }

    public void expectationError(String expected, Token received) {
        error("Expected " + expected + " - Received " + received + ".");
    }

    private boolean matchToken(Token... tokens) {
        Token currentToken = scanner.getCurrentToken();
        String errString = "";
        lastSeenIdentifier = scanner.getLastIdentifier();
        lastSeenNumber = scanner.getLastNumber();
        scanner.next();
        for (Token token: tokens) {
            if (currentToken == token) {
                return true;
            }
            errString += token.toString() + ",";
        }

        expectationError(errString, scanner.getCurrentToken());
        return false;
    }

    private boolean currentTokenIs(Token... tokens) {
        for (Token token: tokens) {
            if (scanner.getCurrentToken() == token) {
                return true;
            }
        }
        return false;
    }

    private void computation() {
        // Inits the main CFG
        ControlFlowGraph.initialize();
        // Sets current CFG to main to be used by symbol and symbol table
        ControlFlowGraph.setCurrentCFG(ControlFlowGraph.getMain());

        matchToken(Token.MAIN);

        while (currentTokenIs(Token.VAR, Token.ARRAY)) {
            variableDeclaration();
        }

        while (currentTokenIs(Token.FUNCTION, Token.PROCEDURE)) {
            functionDeclaration();
        }

        SymbolTable.getInstance().print();

        // Switches back to main CFG
        ControlFlowGraph.setCurrentCFG(ControlFlowGraph.getMain());

        matchToken(Token.BEGIN);
        statementSequence();
        matchToken(Token.END);
        matchToken(Token.PERIOD);

        boolean endOfFileReached;
        do {
            endOfFileReached = matchToken(Token.END_OF_FILE);
        } while (!endOfFileReached);
    }

    private void variableDeclaration() {
        List<Integer> dimensions = typeDeclaration();
        matchToken(Token.IDENTIFIER);

        SymbolTable.getInstance().add(
                dimensions.size() == 0 ? Symbol.variable(lastSeenIdentifier):Symbol.array(lastSeenIdentifier,dimensions)
        );
        while (currentTokenIs(Token.COMMA)) {
            matchToken(Token.COMMA);
            matchToken(Token.IDENTIFIER);
            SymbolTable.getInstance().add(
                    dimensions.size() == 0 ? Symbol.variable(lastSeenIdentifier):Symbol.array(lastSeenIdentifier,dimensions)
            );
        }
        matchToken(Token.SEMICOLON);
    }

    private List<Integer> typeDeclaration() {
        List<Integer> dimensions = new ArrayList<>();

        if (currentTokenIs(Token.VAR)) {
            matchToken(Token.VAR);
            return dimensions;
        }

        matchToken(Token.ARRAY);

        do {
            matchToken(Token.OPEN_BRACKET);
            matchToken(Token.NUMBER);
            dimensions.add(lastSeenNumber);
            matchToken(Token.CLOSE_BRACKET);
        } while (currentTokenIs(Token.OPEN_BRACKET));

        return dimensions;
    }

    private void functionDeclaration() {
        List<Symbol> params = new ArrayList<>();
        Token typeToken = scanner.getCurrentToken(); // function or procedure
        matchToken(Token.FUNCTION, Token.PROCEDURE);
        matchToken(Token.IDENTIFIER);
        Integer functionId = lastSeenIdentifier;

        // Creates a new CFG for the function or procedure and changes the current CFG to be used by symbol and symbol table
        ControlFlowGraph.setCurrentCFG(ControlFlowGraph.create(scanner.identifierToString(lastSeenIdentifier)));

        if (currentTokenIs(Token.OPEN_PARENTHESIS)) {
            params = formalParameters();
        }

        if (typeToken == Token.FUNCTION) {
            SymbolTable.getInstance().add(Symbol.function(functionId, params));
        } else {
            SymbolTable.getInstance().add(Symbol.procedure(functionId, params));
        }

        matchToken(Token.SEMICOLON);
        functionBody();
        matchToken(Token.SEMICOLON);
    }

    private List<Symbol> formalParameters() {
        List<Symbol> params = new ArrayList<>();
        matchToken(Token.OPEN_PARENTHESIS);
        if (currentTokenIs(Token.IDENTIFIER)) {
            matchToken(Token.IDENTIFIER);
            // adds the identifier to the return list and symbol table
            params.add(Symbol.variable(lastSeenIdentifier));
            SymbolTable.getInstance().add(Symbol.variable(lastSeenIdentifier));
            while (currentTokenIs(Token.COMMA)) {
                matchToken(Token.COMMA);
                matchToken(Token.IDENTIFIER);
                params.add(Symbol.variable(lastSeenIdentifier));
                SymbolTable.getInstance().add(Symbol.variable(lastSeenIdentifier));
            }
        }
        matchToken(Token.CLOSE_PARENTHESIS);
        return params;
    }

    private void functionBody() {
        while (currentTokenIs(Token.VAR, Token.ARRAY)) {
            variableDeclaration();
        }
        matchToken(Token.BEGIN);
        if (currentTokenIs(Token.LET, Token.CALL, Token.IF, Token.WHILE, Token.RETURN)) {
            statementSequence();
        }
        matchToken(Token.END);
    }

    private void statementSequence() {
        statement();
        while (currentTokenIs(Token.SEMICOLON)) {
            matchToken(Token.SEMICOLON);
            statement();
        }
    }

    private void statement() {
        switch (scanner.getCurrentToken()) {
            case LET: assignment(); break;
            case CALL: functionCall(); break;
            case IF: ifStatement(); break;
            case WHILE: whileStatement(); break;
            case RETURN: returnStatement(); break;
            // TODO default recovery
        }
    }

    private void assignment() {
        matchToken(Token.LET);
        designator();
        matchToken(Token.BECOMES);
        expression();
    }

    private void functionCall() {
        // TODO take care of built-in functions
        matchToken(Token.CALL);
        matchToken(Token.IDENTIFIER);
        Symbol function = SymbolTable.getInstance().get(lastSeenIdentifier, ControlFlowGraph.getMain().getName());
        if (function == null || (function.getType() != SymbolType.FUNCTION && function.getType() != SymbolType.PROCEDURE)) {
            exitError("Function " + Scanner.getInstance().identifierToString(lastSeenIdentifier) + " is not defined!");
        }
        if (currentTokenIs(Token.OPEN_PARENTHESIS)) {
            matchToken(Token.OPEN_PARENTHESIS);
            if (currentTokenIs(Token.NUMBER, Token.IDENTIFIER, Token.OPEN_PARENTHESIS, Token.CALL)) {
                expression();
                while (currentTokenIs(Token.COMMA)) {
                    matchToken(Token.COMMA);
                    expression();
                }
            }
            matchToken(Token.CLOSE_PARENTHESIS);
        }
    }

    private void ifStatement() {
        matchToken(Token.IF);
        relation();
        matchToken(Token.THEN);
        statementSequence();
        if (currentTokenIs(Token.ELSE)) {
            matchToken(Token.ELSE);
            statementSequence();
        }
        matchToken(Token.FI);
    }

    private void whileStatement() {
        matchToken(Token.WHILE);
        relation();
        matchToken(Token.DO);
        statementSequence();
        matchToken(Token.OD);
    }

    private void returnStatement() {
        matchToken(Token.RETURN);
        if (currentTokenIs(Token.NUMBER, Token.IDENTIFIER, Token.OPEN_PARENTHESIS, Token.CALL)) {
            expression();
        }
    }

    private void designator() {
        matchToken(Token.IDENTIFIER);
        Symbol identifier = SymbolTable.getInstance().get(lastSeenIdentifier);
        if (identifier == null) {
            exitError("Identifier " + identifier.getName() + " is not defined!");
        }
        while (currentTokenIs(Token.OPEN_BRACKET)) {
            matchToken(Token.OPEN_BRACKET);
            expression();
            matchToken(Token.CLOSE_BRACKET);
        }
    }

    private void factor() {
        if (currentTokenIs(Token.NUMBER)) {
            matchToken(Token.NUMBER);
        } else if (currentTokenIs(Token.IDENTIFIER)) {
            designator();
        } else if (currentTokenIs(Token.OPEN_PARENTHESIS)) {
            matchToken(Token.OPEN_PARENTHESIS);
            expression();
            matchToken(Token.CLOSE_PARENTHESIS);
        } else if (currentTokenIs(Token.CALL)) {
            functionCall();
        } else {
            // TODO recovery
        }

    }

    private void term() {
        factor();
        while (currentTokenIs(Token.TIMES, Token.DIV)) {
            matchToken(Token.TIMES, Token.DIV);
            factor();
        }
    }

    private void expression() {
        term();

        while (currentTokenIs(Token.PLUS, Token.MINUS)) {
            matchToken(Token.PLUS, Token.MINUS);
            term();
        }
    }

    private void relation() {
        expression();
        relationOperation();
        expression();
    }

    private void relationOperation() {
        matchToken(Token.EQUAL, Token.NOT_EQUAL, Token.LESS_THAN, Token.LESS_THAN_OR_EQUAL,
                Token.GREATER_THAN, Token.GREATER_THAN_OR_EQUAL);
    }
}
