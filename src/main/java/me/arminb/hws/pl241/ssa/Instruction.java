package me.arminb.hws.pl241.ssa;

import me.arminb.hws.pl241.cfg.BasicBlock;
import me.arminb.hws.pl241.cfg.ControlFlowGraph;
import me.arminb.hws.pl241.frontend.Result;
import me.arminb.hws.pl241.frontend.Scanner;
import me.arminb.hws.pl241.symbol.SymbolTable;

import java.util.ArrayList;
import java.util.List;

public class Instruction {
    private static Integer instructionCounter = 0;
    private Integer index;
    private OpCode opCode;
    private Result operand1;
    private Result operand2;
    private List<Result> params; // for call instructions
    private Integer affectedVariable; // for phi and move instructions
    private Integer phiBeforeValueListSize; // for phi instructions and resetting value lists
    private Instruction next;
    private Instruction previous;
    private BasicBlock basicBlock;
    private List<Instruction> branchDestinationFor;

    private Instruction(BasicBlock basicBlock, OpCode opCode, Result operand1, Result operand2) {
        this.opCode = opCode;
        this.operand1 = operand1;
        this.operand2 = operand2;
        this.index = instructionCounter++;
        this.params = null;
        this.basicBlock = basicBlock;
        basicBlock.addInstruction(this);
        ControlFlowGraph.getCurrent().addInstruction(this);
        branchDestinationFor = new ArrayList<>();
    }

    public static void resetInstructionCounter() {
        instructionCounter = 0;
    }

    private static Instruction getNewInstruction(BasicBlock basicBlock, OpCode opCode, Result operand1, Result operand2) {
        if (basicBlock.hasEmptyInstructionAtBeginning()) {
            Instruction emptyInstruction = basicBlock.getFirstInstruction();
            emptyInstruction.setOpCode(opCode);
            emptyInstruction.setOperand1(operand1);
            emptyInstruction.setOperand2(operand2);
            return emptyInstruction;
        } else {
            return new Instruction(basicBlock, opCode, operand1, operand2);
        }
    }

    private static Instruction getNewInstruction(OpCode opCode, Result operand1, Result operand2) {
        return getNewInstruction(BasicBlock.getCurrent(), opCode, operand1, operand2);
    }

    public static Instruction move(Result expression, Result designator) {
        Instruction moveInstruction = getNewInstruction(OpCode.MOVE, expression, null);
        moveInstruction.setOperand2(new Result(Result.Type.VALUE, moveInstruction.getIndex()));
        SymbolTable.getInstance().get(designator.getValue()).addValue(moveInstruction.getIndex());
        return moveInstruction;
    }

    public static Instruction add(Result operand1, Result operand2) {
        return getNewInstruction(OpCode.ADD, operand1, operand2);
    }

    public static Instruction sub(Result operand1, Result operand2) {
        return getNewInstruction(OpCode.SUB, operand1, operand2);
    }

    public static Instruction mul(Result operand1, Result operand2) {
        return getNewInstruction(OpCode.MUL, operand1, operand2);
    }

    public static Instruction div(Result operand1, Result operand2) {
        return getNewInstruction(OpCode.DIV, operand1, operand2);
    }

    public static Instruction cmp(Result operand1, Result operand2) {
        return getNewInstruction(OpCode.CMP, operand1, operand2);
    }

    public static Instruction conditionalBranch(OpCode opCode, Result condition) {
        return getNewInstruction(opCode, condition, null);
    }

    public static Instruction empty() {
        return getNewInstruction(null, null, null);
    }

    public static Instruction branch(Result target) {
        Instruction branchInstruction = getNewInstruction(OpCode.BRA, target, null);
        ControlFlowGraph.getInstruction(target.getValue()).getBranchDestinationFor().add(branchInstruction);
        return branchInstruction;
    }

    public static Instruction phi(BasicBlock basicBlock, Result designator, Result operand1, Result operand2) {
        Instruction phiInstruction = getNewInstruction(basicBlock, OpCode.PHI, operand1, operand2);
        phiInstruction.setAffectedVariable(designator.getValue());
        basicBlock.changeEmptyToPhiInstruction(phiInstruction);
        return phiInstruction;
    }

    public static Instruction end() {
        return getNewInstruction(OpCode.END, null, null);
    }

    public static Instruction ret(Result result) {
        return getNewInstruction(OpCode.RET, result, null);
    }

    public static Instruction call(Result selector, List<Result> params) {
        Instruction callInstruction = getNewInstruction(OpCode.CALL, selector, null);
        callInstruction.setParams(params);
        return callInstruction;
    }

    public static Instruction read() {
        return getNewInstruction(OpCode.READ, null, null);
    }

    public static Instruction write(Result param) {
        Instruction callInstruction = getNewInstruction(OpCode.WRITE, param, null);
        return callInstruction;
    }

    public static Instruction writeLine() {
        return getNewInstruction(OpCode.WRITENL, null, null);
    }

    public static Instruction load(Result address) {
        return getNewInstruction(OpCode.LOAD, address, null);
    }

    public static Instruction store(Result value, Result address) {
        return getNewInstruction(OpCode.STORE, value, address);
    }

    public static Instruction adda(Result operand1, Result operand2) {
        return getNewInstruction(OpCode.ADDA, operand1, operand2);
    }

    public boolean isEmpty() {
        return opCode == null && operand1 == null && operand2 == null;
    }

    public Integer getIndex() {
        return index;
    }

    public OpCode getOpCode() {
        return opCode;
    }

    public Result getOperand1() {
        return operand1;
    }

    public Result getOperand2() {
        return operand2;
    }

    public BasicBlock getBasicBlock() {
        return basicBlock;
    }

    public void setOperand1(Result operand1) {
        if (opCode == OpCode.BRA) {
            ControlFlowGraph.getInstruction(operand1.getValue()).getBranchDestinationFor().add(this);
        }
        this.operand1 = operand1;
    }

    public void setOperand2(Result operand2) {
        if (opCode == OpCode.BEQ || opCode == OpCode.BNE || opCode == OpCode.BGE || opCode == OpCode.BGT ||
                opCode == OpCode.BLE || opCode == OpCode.BLT) {
            ControlFlowGraph.getInstruction(operand2.getValue()).getBranchDestinationFor().add(this);
        }
        this.operand2 = operand2;
    }

    public Instruction getNext() {
        return next;
    }

    public void setNext(Instruction next) {
        this.next = next;
    }

    public Instruction getPrevious() {
        return previous;
    }

    public void setPrevious(Instruction previous) {
        this.previous = previous;
    }

    @Override
    public String toString() {
        if (opCode == null) {
            return "null";
        }

        switch (opCode) {
            case CALL: {
                StringBuilder retString = new StringBuilder();
                retString.append(index + ": ");
                retString.append(OpCode.CALL);
                retString.append(" " + Scanner.getInstance().identifierToString(operand1.getValue()));
                for (Result param : params) {

                    retString.append(" " + param.getValue());
                }
                return retString.toString();
            }
            case PHI: case MOVE: {
                return index + ": " + opCode + " " + operand1 + " " + operand2
                        + " (" + Scanner.getInstance().identifierToString(getAffectedVariable()) + ")";
            }
            default: {
                if (operand1 == null && operand2 == null) {
                    return index + ": " + opCode;
                } else if (operand2 == null) {
                    return index + ": " + opCode + " " + operand1;
                } else {
                    return index + ": " + opCode + " " + operand1 + " " + operand2;
                }
            }
        }
    }

    public List<Result> getParams() {
        return params;
    }

    public void setParams(List<Result> params) {
        this.params = params;
    }

    public void setOpCode(OpCode opCode) {
        this.opCode = opCode;
    }

    public Integer getAffectedVariable() {
        return affectedVariable;
    }

    public void setAffectedVariable(Integer affectedVariable) {
        this.affectedVariable = affectedVariable;
    }

    public Integer getPhiBeforeValueListSize() {
        return phiBeforeValueListSize;
    }

    // Here we assume this.next == null and instruction.previous == null
    public void connectTo(Instruction instruction) {
        this.setNext(instruction);
        instruction.setPrevious(this);
    }

    public void setPhiBeforeValueListSize(Integer phiBeforeValueListSize) {
        this.phiBeforeValueListSize = phiBeforeValueListSize;
    }

    public List<Instruction> getBranchDestinationFor() {
        return branchDestinationFor;
    }

    public void resetBranchDestinationFor() {
        this.branchDestinationFor = new ArrayList<>();
    }
}
