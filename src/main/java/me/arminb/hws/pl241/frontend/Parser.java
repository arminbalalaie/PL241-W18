package me.arminb.hws.pl241.frontend;

import me.arminb.hws.pl241.cfg.BasicBlock;
import me.arminb.hws.pl241.cfg.ControlFlowGraph;
import me.arminb.hws.pl241.ssa.Instruction;
import me.arminb.hws.pl241.ssa.OpCode;
import me.arminb.hws.pl241.symbol.Symbol;
import me.arminb.hws.pl241.symbol.SymbolTable;
import me.arminb.hws.pl241.symbol.SymbolType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
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

        ControlFlowGraph.generateGraphFiles();
    }

    public void exitError(String errorMessage) {
        throw new RuntimeException(errorMessage + " at Line: " + scanner.getCurrentLineNumber() + " Column: " + scanner.getCurrentLinePointer());
    }

    public void error(String errorMessage) {
        logger.error(errorMessage + " at Line: " + scanner.getCurrentLineNumber() + " Column: " + scanner.getCurrentLinePointer());
    }

    public void warning(String warningMessage) {
        logger.warn(warningMessage + " at Line: " + scanner.getCurrentLineNumber() + " Column: " + scanner.getCurrentLinePointer());
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

        // Switches back to main CFG
        ControlFlowGraph.setCurrentCFG(ControlFlowGraph.getMain());
        // Resets instructionCounter and value lists
        SymbolTable.getInstance().resetValueLists();
        Instruction.resetInstructionCounter();
        // Creates the entry basic block for the main CFG and sets it as current basic block
        BasicBlock.setCurrent(BasicBlock.create());

        matchToken(Token.BEGIN);
        statementSequence();
        matchToken(Token.END);
        matchToken(Token.PERIOD);
        Instruction.end();

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
        // Resets instructionCounter and value lists
        SymbolTable.getInstance().resetValueLists();
        Instruction.resetInstructionCounter();
        // Creates the entry basic block for this CFG and sets it as current basic block
        BasicBlock.setCurrent(BasicBlock.create());

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
            Symbol paramSymbol = Symbol.variable(lastSeenIdentifier, true);
            params.add(paramSymbol);
            SymbolTable.getInstance().add(paramSymbol);
            while (currentTokenIs(Token.COMMA)) {
                matchToken(Token.COMMA);
                matchToken(Token.IDENTIFIER);
                paramSymbol = Symbol.variable(lastSeenIdentifier, true);
                params.add(paramSymbol);
                SymbolTable.getInstance().add(paramSymbol);
            }
        }
        matchToken(Token.CLOSE_PARENTHESIS);
        return params;
    }

    private void functionBody() {
        Instruction lastStatement = null;
        while (currentTokenIs(Token.VAR, Token.ARRAY)) {
            variableDeclaration();
        }
        matchToken(Token.BEGIN);
        if (currentTokenIs(Token.LET, Token.CALL, Token.IF, Token.WHILE, Token.RETURN)) {
            lastStatement = statementSequence();
        }
        matchToken(Token.END);

        if (lastStatement == null || lastStatement.getOpCode() != OpCode.RET) {
            Instruction.ret(null);
        }
    }

    private Instruction statementSequence() {
        Instruction retInstruction = BasicBlock.getCurrent().getLastInstruction();
        Instruction statementInstruction = statement();
        if (statementInstruction != null) {
            retInstruction = statementInstruction;
        }
        while (currentTokenIs(Token.SEMICOLON)) {
            matchToken(Token.SEMICOLON);
            statementInstruction = statement();
            if (statementInstruction != null) {
                retInstruction = statementInstruction;
            }
        }
        return retInstruction;
    }

    private Instruction statement() {
        switch (scanner.getCurrentToken()) {
            case LET: return assignment();
            case CALL: return ControlFlowGraph.getCurrent().getInstruction(functionCall().getValue());
            case IF: return ifStatement();
            case WHILE: return whileStatement();
            case RETURN: return returnStatement();
            // TODO default recovery
        }

        return null;
    }

    private Instruction assignment() {
        matchToken(Token.LET);
        Result designatorResult = designator();
        matchToken(Token.BECOMES);
        Result expressionResult = expression();
        if (expressionResult.getType() == Result.Type.PROCEDURE) {
            error("cannot assign procedure to a variable!");
        }

        if (designatorResult.getArrayIndices().isEmpty()) { // variable
            Symbol variableSymbol = SymbolTable.getInstance().get(designatorResult.getValue());
            Result valueBeforeMove = variableSymbol.getLastValue();
            Integer valueListSizeBeforeMove = variableSymbol.getValueList().size();

            if (variableSymbol.isGlobal()) { // global
                Result addResult;
                if (expressionResult.getType() == Result.Type.CONSTANT) {
                    Instruction addInstruction = Instruction.add(expressionResult, new Result(Result.Type.CONSTANT, 0));
                    addResult = new Result(Result.Type.VALUE, addInstruction.getIndex());
                } else {
                    addResult = expressionResult;
                }
                Instruction storeInstruction = Instruction.store(addResult, variableSymbol.getAbsoluteAddress());
                storeInstruction.setAffectedVariable(variableSymbol.getIdentifier());
                variableSymbol.addValue(addResult);
                addPhiInstruction(designatorResult, valueListSizeBeforeMove, valueBeforeMove, addResult);
                return storeInstruction;
            } else { // local
                // deliberately skip this for copy propagation
                //Instruction moveInstruction = Instruction.move(expressionResult, designatorResult);

                if (expressionResult.getType() == Result.Type.CONSTANT) { // constant assignment is handled by add by zero
                    Instruction addInstruction = Instruction.add(expressionResult, new Result(Result.Type.CONSTANT, 0));
                    Result addResult = new Result(Result.Type.VALUE, addInstruction.getIndex());
                    variableSymbol.addValue(addResult);
                    addPhiInstruction(designatorResult, valueListSizeBeforeMove, valueBeforeMove, addResult);
                    return addInstruction;
                } else { // value to value assignment
                    if (valueBeforeMove == null || !valueBeforeMove.equals(expressionResult)) {
                        variableSymbol.addValue(expressionResult);
                        addPhiInstruction(designatorResult, valueListSizeBeforeMove, valueBeforeMove, expressionResult);
                    }
                    return null;
                }
            }
        } else { // array
            Result absoluteAddress = SymbolTable.getInstance().get(designatorResult.getValue()).getAbsoluteAddress();
            Instruction addAInstruction = Instruction.adda(absoluteAddress, designatorResult.getArrayRelativeAddress());
            Instruction storeInstruction = Instruction.store(expressionResult, new Result(Result.Type.VALUE, addAInstruction.getIndex()));
            storeInstruction.setAffectedVariable(designatorResult.getValue());
            return storeInstruction;
        }
    }

    private Instruction addPhiInstruction(Result designatorResult,Integer valueListSizeBeforeMove,
                                          Result beforeValue, Result newValue) {
        if (BasicBlock.getCurrent().getJoinBlock() != null) { // We need to create phi instruction
            Instruction existingPhi = BasicBlock.getCurrent().getJoinBlock().getPhiInstruction(designatorResult.getValue());
            if (existingPhi == null) {
                // there is not any phi instruction for this variable in the join block
                Instruction phiInstruction;
                if (BasicBlock.getCurrent().isJoiningFromLeft()) {
                    phiInstruction = Instruction.phi(BasicBlock.getCurrent().getJoinBlock(), designatorResult,
                            newValue, beforeValue);
                } else {
                    phiInstruction = Instruction.phi(BasicBlock.getCurrent().getJoinBlock(), designatorResult,
                            beforeValue, newValue);
                }
                // set this for resetting value list
                phiInstruction.setPhiBeforeValueListSize(valueListSizeBeforeMove);
                return phiInstruction;
            } else {
                if (BasicBlock.getCurrent().isJoiningFromLeft()) {
                    existingPhi.setOperand1(newValue);
                } else {
                    existingPhi.setOperand2(newValue);
                }
                return existingPhi;
            }
        }
        return null;
    }

    private Result functionCall() {
        Result callResult;
        List<Result> params = new ArrayList<>();
        matchToken(Token.CALL);
        matchToken(Token.IDENTIFIER);
        Integer functionIdentifier = lastSeenIdentifier;
        Symbol functionSymbol = SymbolTable.getInstance().get(functionIdentifier, ControlFlowGraph.getMain().getName());
        if (functionSymbol == null || (functionSymbol.getType() != SymbolType.FUNCTION && functionSymbol.getType() != SymbolType.PROCEDURE)) {
            error("Function " + Scanner.getInstance().identifierToString(functionIdentifier) + " is not defined!");
        }
        if (currentTokenIs(Token.OPEN_PARENTHESIS)) {
            matchToken(Token.OPEN_PARENTHESIS);
            if (currentTokenIs(Token.NUMBER, Token.IDENTIFIER, Token.OPEN_PARENTHESIS, Token.CALL)) {
                params.add(expression());
                while (currentTokenIs(Token.COMMA)) {
                    matchToken(Token.COMMA);
                    params.add(expression());
                }
            }
            matchToken(Token.CLOSE_PARENTHESIS);

        }

        if (functionSymbol != null) {
            if (functionSymbol.getParameters().size() != params.size()) {
                error("The " + functionSymbol.getType().toString().toLowerCase() + " "
                        + functionSymbol.getName() + "takes " + functionSymbol.getParameters().size() + " parameters!");
            }
        }

        Result.Type functionType;
        if (functionSymbol != null && functionSymbol.getType() == SymbolType.PROCEDURE) {
            functionType = Result.Type.PROCEDURE;
        } else {
            functionType = Result.Type.VALUE;
        }

        // predefined functions and procedures
        if (functionSymbol.getName().equals("InputNum")) {
            callResult = new Result(functionType, Instruction.read().getIndex());
        } else if (functionSymbol.getName().equals("OutputNum")) {
            callResult = new Result(functionType, Instruction.write(params.get(0)).getIndex());
        } else if (functionSymbol.getName().equals("OutputNewLine")) {
            callResult = new Result(functionType, Instruction.writeLine().getIndex());
        } else {
            callResult = new Result(functionType, Instruction.call(new Result(Result.Type.SELECTOR, functionIdentifier), params).getIndex());
        }

        // TODO here we are assuming a store in the called function so we need to add a kill in load anchor for CSE
        // reset value list of global vars in order to avoid possible side effects of global variables in the called function
        SymbolTable.getInstance().resetGlobalVariablesValueList();

        return callResult;
    }



    private Instruction ifStatement() {
        BasicBlock entryBlock = BasicBlock.getCurrent();
        // Join block
        BasicBlock joinBlock = BasicBlock.create();
        // setting join block for nested if's join blocks. Necessary for proper phi instruction generation
        joinBlock.setJoinBlock(entryBlock.getJoinBlock());
        if (entryBlock.isJoiningFromRight()) {
            joinBlock.joinFromRight();
        }
        // Fall through block
        BasicBlock fallThroughBlock = BasicBlock.create();
        fallThroughBlock.setJoinBlock(joinBlock);
        // Connect entry block to fall through block
        entryBlock.setFallThroughBlock(fallThroughBlock);
        entryBlock.getLastInstruction().connectTo(fallThroughBlock.getFirstInstruction());
        // set up domination information
        entryBlock.addImmediateDomination(fallThroughBlock);
        entryBlock.addImmediateDomination(joinBlock);

        matchToken(Token.IF);
        Result relationResult = relation();
        matchToken(Token.THEN);

        Instruction fixupBranchInstruction = ControlFlowGraph.getCurrent().getInstruction(relationResult.getValue());

        BasicBlock.setCurrent(fallThroughBlock);

        Instruction lastThenInstruction = statementSequence();

        if (currentTokenIs(Token.ELSE)) {
            matchToken(Token.ELSE);

            BasicBlock branchBlock = BasicBlock.create();
            branchBlock.setJoinBlock(joinBlock);
            branchBlock.joinFromRight();
            BasicBlock.setCurrent(branchBlock);
            // connect entry block to branch block
            entryBlock.setBranchBlock(branchBlock);
            lastThenInstruction.connectTo(branchBlock.getFirstInstruction());
            // set up domination information
            entryBlock.addImmediateDomination(branchBlock);

            // resetting value list for variables with phi instructions in join block
            resetValueListBasedOnPhiInstructions(joinBlock.getPhiInstructions());

            Instruction lastElseInstruction = statementSequence();

            // Fixup entry block branch - this needs to be done after statementSequence since we don't know if the first
            // instruction of branch block is gonna change or not
            fixupBranchInstruction.setOperand2(new Result(Result.Type.VALUE, branchBlock.getFirstInstruction().getIndex()));

            // finds the last block in fall through side to be connected to join block
            fallThroughBlock = lastThenInstruction.getBasicBlock();
            fallThroughBlock.setBranchBlock(joinBlock);
            // adds corresponding branch instruction to the new fall through block
            BasicBlock.setCurrent(fallThroughBlock);
            Instruction.branch(new Result(Result.Type.VALUE, joinBlock.getFirstInstruction().getIndex()));

            // finds the last block in branch side to be connected to join block
            branchBlock = lastElseInstruction.getBasicBlock();
            // connests branch block to join block
            branchBlock.setFallThroughBlock(joinBlock);
            lastElseInstruction.connectTo(joinBlock.getFirstInstruction());
        } else {
            // Setting up edges for if-then case
            // We don't need a branch in fallThrough block
            entryBlock.setBranchBlock(joinBlock);
            // Fixup entry block branch
            fixupBranchInstruction.setOperand2(new Result(Result.Type.VALUE, joinBlock.getFirstInstruction().getIndex()));

            // finds the last block in fall through side to be connected to join block
            fallThroughBlock = lastThenInstruction.getBasicBlock();
            // connects fall through block to join block
            fallThroughBlock.setFallThroughBlock(joinBlock);
            lastThenInstruction.connectTo(joinBlock.getFirstInstruction());
        }

        matchToken(Token.FI);

        // resetting value list for variables with phi instructions in join block
        resetValueListBasedOnPhiInstructions(joinBlock.getPhiInstructions());

        // propagating phi instructions to the outer join block
        BasicBlock.setCurrent(joinBlock);
        for (Instruction phiInstruction : joinBlock.getPhiInstructions()) {
            addPhiInstruction(new Result(Result.Type.SELECTOR, phiInstruction.getAffectedVariable()),
                    phiInstruction.getPhiBeforeValueListSize(),
                    SymbolTable.getInstance().get(phiInstruction.getAffectedVariable()).getLastValue(),
                    new Result(Result.Type.VALUE, phiInstruction.getIndex()));
        }

        // updating value list for variables with phi instructions
        for (Instruction phiInstruction : joinBlock.getPhiInstructions()) {
            SymbolTable.getInstance().get(phiInstruction.getAffectedVariable()).addValue(
                    new Result(Result.Type.VALUE, phiInstruction.getIndex()));
        }

        return joinBlock.getLastInstruction();
    }

    private void resetValueListBasedOnPhiInstructions(List<Instruction> phiInstructions) {
        for (Instruction phiInstruction: phiInstructions) {
            SymbolTable.getInstance().get(phiInstruction.getAffectedVariable()).resetValueListTo(phiInstruction.getPhiBeforeValueListSize());
        }
    }

    private Instruction whileStatement() {
        BasicBlock currentBlock = BasicBlock.getCurrent();
        BasicBlock loopBodyBlock = BasicBlock.create();
        BasicBlock followBlock = BasicBlock.create();
        // we don't have pre-header block. detect loop header block
        BasicBlock joinBlock;
        if (currentBlock.isEmpty() || currentBlock.hasEmptyInstructionAtBeginning()) {
            joinBlock = currentBlock;
        } else {
            joinBlock = BasicBlock.create(); // Loop header
            // connects current block to loop header
            currentBlock.setFallThroughBlock(joinBlock);
            currentBlock.getLastInstruction().connectTo(joinBlock.getFirstInstruction());
            // set up domination information
            currentBlock.addImmediateDomination(joinBlock);
        }
        // set outer join block for nesting and phi propagation
        followBlock.setJoinBlock(currentBlock.getJoinBlock());
        // connects loop header to loop body
        joinBlock.setFallThroughBlock(loopBodyBlock);
        joinBlock.getLastInstruction().connectTo(loopBodyBlock.getFirstInstruction());
        // connects loop header to follow block
        joinBlock.setBranchBlock(followBlock);
        // sets loop body join block for phi instructions
        loopBodyBlock.setJoinBlock(joinBlock);
        loopBodyBlock.joinFromRight();
        // set up domination information
        joinBlock.addImmediateDomination(loopBodyBlock);
        joinBlock.addImmediateDomination(followBlock);

        matchToken(Token.WHILE);

        BasicBlock.setCurrent(joinBlock);
        Result relationResult = relation();
        // sets the branch instruction number for the loop header
        Instruction fixupBranchInstruction = ControlFlowGraph.getCurrent().getInstruction(relationResult.getValue());

        matchToken(Token.DO);

        BasicBlock.setCurrent(loopBodyBlock);
        Instruction lastWhileStatement = statementSequence();

        // fixes the branch instruction in loop header.
        fixupBranchInstruction.setOperand2(new Result(Result.Type.VALUE, followBlock.getFirstInstruction().getIndex()));

        // finds the last block in the loop body side
        loopBodyBlock = lastWhileStatement.getBasicBlock();
        // connects last block in the loop body side to loop header
        loopBodyBlock.setBranchBlock(joinBlock);
        // adds corresponding branch instruction to loop body side
        BasicBlock.setCurrent(loopBodyBlock);
        Instruction loopBodyBranchInstruction = Instruction.branch(new Result(Result.Type.VALUE,
                joinBlock.getFirstInstruction().getIndex()));
        // connects last instruction in the loop body side to first instruction in follow block
        loopBodyBranchInstruction.connectTo(followBlock.getFirstInstruction());

        matchToken(Token.OD);

        // rename phi occurrences - update loop header and body based on the new phi instructions
        renameWhileInstructionsOperandsBasedonPhiInstructions(joinBlock);

        // resetting value list for variables with phi instructions in join block
        resetValueListBasedOnPhiInstructions(joinBlock.getPhiInstructions());

        // propagating phi instructions to the outer join block
        BasicBlock.setCurrent(joinBlock);
        for (Instruction phiInstruction : joinBlock.getPhiInstructions()) {
            addPhiInstruction(new Result(Result.Type.SELECTOR, phiInstruction.getAffectedVariable()),
                    phiInstruction.getPhiBeforeValueListSize(),
                    SymbolTable.getInstance().get(phiInstruction.getAffectedVariable()).getLastValue(),
                    new Result(Result.Type.VALUE, phiInstruction.getIndex()));
        }

        // rename phi occurrences again using the outer join block
        if (joinBlock.getJoinBlock() != null) {
            renameWhileInstructionsOperandsBasedonPhiInstructions(joinBlock.getJoinBlock());
        }

        // updating value list for variables with phi instructions
        for (Instruction phiInstruction : joinBlock.getPhiInstructions()) {
            SymbolTable.getInstance().get(phiInstruction.getAffectedVariable()).addValue(
                    new Result(Result.Type.VALUE, phiInstruction.getIndex()));
        }

        BasicBlock.setCurrent(followBlock);

        return followBlock.getLastInstruction();
    }

    private void renameWhileInstructionsOperandsBasedonPhiInstructions(BasicBlock joinBlock) {
        List<Instruction> phiInstructions = joinBlock.getPhiInstructions();
        HashMap<Result, Result> oldToNewMapping = new HashMap<>();

        Instruction currentInstruction = joinBlock.getFirstInstruction();
        while (currentInstruction != null &&
                (currentInstruction.getBasicBlock() == joinBlock || currentInstruction.getBasicBlock() == joinBlock.getFallThroughBlock())) {

            if (currentInstruction.getOpCode() != OpCode.PHI) {
                if (currentInstruction.getOperand1() != null && currentInstruction.getOperand1().getType() != Result.Type.CONSTANT &&
                        oldToNewMapping.containsKey(currentInstruction.getOperand1())) {
                    currentInstruction.setOperand1(oldToNewMapping.get(currentInstruction.getOperand1()));
                }
                if (currentInstruction.getOperand2() != null && currentInstruction.getOperand2().getType() != Result.Type.CONSTANT &&
                        oldToNewMapping.containsKey(currentInstruction.getOperand2())) {
                    currentInstruction.setOperand2(oldToNewMapping.get(currentInstruction.getOperand2()));
                }

                if (currentInstruction.getParams() != null) {
                    List<Result> newParams = new ArrayList<>();
                    for (Result param : currentInstruction.getParams()) {
                        if (param.getType() != Result.Type.CONSTANT && oldToNewMapping.containsKey(param)) {
                            newParams.add(oldToNewMapping.get(param));
                        } else {
                            newParams.add(param);
                        }
                    }
                    currentInstruction.setParams(newParams);
                }

            } else {
                if (currentInstruction.getBasicBlock() == joinBlock) {
                    if (currentInstruction.getOperand1() != null) {
                        oldToNewMapping.put(currentInstruction.getOperand1(), new Result(Result.Type.VALUE, currentInstruction.getIndex()));
                    }
                    if (currentInstruction.getOperand2() != null) {
                        oldToNewMapping.put(currentInstruction.getOperand2(), new Result(Result.Type.VALUE, currentInstruction.getIndex()));
                    }
                }
            }

            currentInstruction = currentInstruction.getNext();
        }
    }

    private Instruction returnStatement() {
        matchToken(Token.RETURN);
        if (currentTokenIs(Token.NUMBER, Token.IDENTIFIER, Token.OPEN_PARENTHESIS, Token.CALL)) {
            Result expressionResult = expression();
            return Instruction.ret(expressionResult);
        }
        return Instruction.ret(null);
    }

    private Result designator() {
        matchToken(Token.IDENTIFIER);

        Symbol identifier = SymbolTable.getInstance().get(lastSeenIdentifier);
        if (identifier == null) {
            error("Identifier " + scanner.identifierToString(lastSeenIdentifier) + " is not defined!");
        }
        Result designatorResult = new Result(Result.Type.SELECTOR, lastSeenIdentifier);

        List<Result> arrayIndices = new ArrayList<>();
        while (currentTokenIs(Token.OPEN_BRACKET)) {
            matchToken(Token.OPEN_BRACKET);
            arrayIndices.add(expression());
            matchToken(Token.CLOSE_BRACKET);
        }

        designatorResult.setArrayIndices(arrayIndices);

        return designatorResult;
    }

    private Result factor() {
        Result factorResult = null;
        if (currentTokenIs(Token.NUMBER)) {
            matchToken(Token.NUMBER);
            factorResult = new Result(Result.Type.CONSTANT, lastSeenNumber);
        } else if (currentTokenIs(Token.IDENTIFIER)) {
            Result designatorResult = designator();
            // designator is rvalue in factor. changes type from selector to variable
            if (designatorResult.getArrayIndices().isEmpty()) { // variable
                Symbol variableSymbol = SymbolTable.getInstance().get(designatorResult.getValue());
                if (variableSymbol == null || variableSymbol.getLastValue() == null) {
                    if (variableSymbol == null) {
                        factorResult = new Result(Result.Type.VALUE, null);
                    } else if (variableSymbol.isParam()) {
                        Instruction loadInstruction = Instruction.load(variableSymbol.getAbsoluteAddress());
                        loadInstruction.setAffectedVariable(variableSymbol.getIdentifier());
                        factorResult = new Result(Result.Type.VALUE, loadInstruction.getIndex());
                        variableSymbol.addValue(factorResult);
                    } else if (variableSymbol.isGlobal()) {
                        warning("Global variable " + scanner.identifierToString(designatorResult.getValue())
                                + " might not be initialized!");
                        Instruction loadInstruction = Instruction.load(variableSymbol.getAbsoluteAddress());
                        loadInstruction.setAffectedVariable(variableSymbol.getIdentifier());
                        factorResult = new Result(Result.Type.VALUE, loadInstruction.getIndex());
                        variableSymbol.addValue(factorResult);
                    } else {
                        error("Variable " + scanner.identifierToString(designatorResult.getValue())
                                + " first needs to be initialized!");
                        factorResult = new Result(Result.Type.VALUE, null);
                    }
                } else {
                    Result lastValue = variableSymbol.getLastValue();
                    if (lastValue.getType() != Result.Type.CONSTANT) {
                        Instruction lastValueInstruction = ControlFlowGraph.getCurrent().getInstruction(lastValue.getValue());
                        if (lastValueInstruction.getOpCode() == OpCode.PHI && lastValueInstruction.hasNullOperands()) {
                            error("Variable " + scanner.identifierToString(designatorResult.getValue())
                                    + " may not be initialized!");
                        }
                    }
                    factorResult = lastValue;
                }
            } else { // array
                Result absoluteAddress = SymbolTable.getInstance().get(designatorResult.getValue()).getAbsoluteAddress();
                Instruction addAInstruction = Instruction.adda(absoluteAddress, designatorResult.getArrayRelativeAddress());
                Instruction loadInstruction = Instruction.load(new Result(Result.Type.VALUE, addAInstruction.getIndex()));
                loadInstruction.setAffectedVariable(designatorResult.getValue());
                factorResult = new Result(Result.Type.VALUE, loadInstruction.getIndex());
            }
        } else if (currentTokenIs(Token.OPEN_PARENTHESIS)) {
            matchToken(Token.OPEN_PARENTHESIS);
            factorResult = expression();
            matchToken(Token.CLOSE_PARENTHESIS);
        } else if (currentTokenIs(Token.CALL)) {
            factorResult = functionCall();
        } else {
            // TODO recovery
        }

        return factorResult;
    }

    private Result term() {
        Result factorReturn = factor();
        Result factorResult2 = null;

        while (currentTokenIs(Token.TIMES, Token.DIV)) {
            Token operationToken = scanner.getCurrentToken();
            matchToken(Token.TIMES, Token.DIV);
            factorResult2 = factor();
            if (operationToken == Token.DIV) {
                factorReturn = factorReturn.div(factorResult2);
            } else { // TIMES
                factorReturn = factorReturn.times(factorResult2);
            }
        }

        return factorReturn;
    }

    private Result expression() {
        Result expressionReturn = term();
        Result expressionResult2 = null;

        while (currentTokenIs(Token.PLUS, Token.MINUS)) {
            Token operationToken = scanner.getCurrentToken();
            matchToken(Token.PLUS, Token.MINUS);
            expressionResult2 = term();
            if (operationToken == Token.PLUS) {
                expressionReturn = expressionReturn.plus(expressionResult2);
            } else { // MINUS
                expressionReturn = expressionReturn.minus(expressionResult2);
            }
        }

        return expressionReturn;
    }

    private Result relation() {
        Result expressionResult1 = expression();
        Token relationToken = relationOperation();
        Result expressionResult2 = expression();

        Result relationResult = expressionResult1.relation(expressionResult2, relationToken);
        return relationResult;
    }

    private Token relationOperation() {
        Token relationToken = scanner.getCurrentToken();
        if(!matchToken(Token.EQUAL, Token.NOT_EQUAL, Token.LESS_THAN, Token.LESS_THAN_OR_EQUAL,
                Token.GREATER_THAN, Token.GREATER_THAN_OR_EQUAL)) {
            relationToken = Token.EQUAL;
        }
        return relationToken;
    }
}
