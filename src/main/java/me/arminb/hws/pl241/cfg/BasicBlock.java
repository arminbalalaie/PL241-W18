package me.arminb.hws.pl241.cfg;

import me.arminb.hws.pl241.frontend.Result;
import me.arminb.hws.pl241.ssa.Instruction;
import me.arminb.hws.pl241.ssa.OpCode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class BasicBlock {
    private static BasicBlock current;

    private Integer number;
    private BasicBlock fallThroughBlock;
    private BasicBlock branchBlock;
    private BasicBlock joinBlock;
    private Boolean joiningFromLeft;
    private HashMap<Integer, Instruction> phiInstructions;
    private Instruction firstInstruction;
    private Instruction lastInstruction;

    private BasicBlock() {
        fallThroughBlock = null;
        branchBlock = null;
        joinBlock = null;
        lastInstruction = null;
        firstInstruction = null;
        phiInstructions = new HashMap<>();
        joiningFromLeft = true;
    }

    public static BasicBlock create() {
        BasicBlock basicBlock = new BasicBlock();
        ControlFlowGraph.getCurrent().addBasicBlock(basicBlock);
        basicBlock.addEmptyInstruction();
        return basicBlock;
    }

    public static void setCurrent(BasicBlock basicBlock) {
        current = basicBlock;
    }

    public static BasicBlock getCurrent() {
        return current;
    }

    public void addInstruction(Instruction instruction) {
        if (getFirstInstruction() == null) {
            firstInstruction = instruction;
            lastInstruction = instruction;
        } else {
            if (instruction.getOpCode() == OpCode.PHI) {
                if (firstInstruction.getOpCode() != OpCode.PHI) {
                    // updating branch instructions that have this instruction as operand
                    for (Instruction branchInstruction: firstInstruction.getBranchDestinationFor()) {
                        if (branchInstruction.getOpCode() == OpCode.BRA) {
                            branchInstruction.setOperand1(new Result(Result.Type.VALUE, instruction.getIndex()));
                        } else {
                            branchInstruction.setOperand2(new Result(Result.Type.VALUE, instruction.getIndex()));
                        }
                    }

                    firstInstruction.resetBranchDestinationFor();

                    instruction.setNext(firstInstruction);
                    instruction.setPrevious(firstInstruction.getPrevious());
                    if (firstInstruction.getPrevious() != null) {
                        firstInstruction.getPrevious().setNext(instruction);
                    }
                    firstInstruction.setPrevious(instruction);
                    firstInstruction = instruction;
                } else {
                    Instruction insertAfter = firstInstruction;
                    while (insertAfter != lastInstruction && insertAfter.getNext() != null &&
                            insertAfter.getNext().getOpCode() == OpCode.PHI) {
                        insertAfter = insertAfter.getNext();
                    }
                    if (insertAfter == lastInstruction || insertAfter.getNext() == null) {
                        lastInstruction = instruction;
                    }

                    if (insertAfter.getNext() != null){
                        instruction.setNext(insertAfter.getNext());
                        insertAfter.getNext().setPrevious(instruction);
                    }
                    insertAfter.setNext(instruction);
                    instruction.setPrevious(insertAfter);
                }
            } else {
                if (lastInstruction.getNext() != null) {
                    instruction.setNext(lastInstruction.getNext());
                    lastInstruction.getNext().setPrevious(instruction);
                }
                getLastInstruction().setNext(instruction);
                instruction.setPrevious(getLastInstruction());
                lastInstruction = instruction;
            }
        }
    }

    public void changeEmptyToPhiInstruction(Instruction instruction) {
        phiInstructions.put(instruction.getAffectedVariable(), instruction);
    }

    public BasicBlock getFallThroughBlock() {
        return fallThroughBlock;
    }

    public void setFallThroughBlock(BasicBlock fallThroughBlock) {
        this.fallThroughBlock = fallThroughBlock;
    }

    public BasicBlock getBranchBlock() {
        return branchBlock;
    }

    public void setBranchBlock(BasicBlock branchBlock) {
        this.branchBlock = branchBlock;
    }

    public BasicBlock getJoinBlock() {
        return joinBlock;
    }

    public void setJoinBlock(BasicBlock joinBlock) {
        this.joinBlock = joinBlock;
    }

    public boolean isEmpty() {
        return firstInstruction == null;
    }

    // adds empty instruction to the block if there is no instruction in the block. Useful for empty join blocks
    public void addEmptyInstruction() {
        if (isEmpty()) {
            BasicBlock currentBlock = BasicBlock.getCurrent();
            BasicBlock.setCurrent(this);
            Instruction.empty();
            BasicBlock.setCurrent(currentBlock);
        }
    }

    public boolean hasEmptyInstructionAtBeginning() {
        if (getFirstInstruction() != null && getFirstInstruction().isEmpty()) {
            return true;
        }
        return false;
    }

    public Instruction getLastInstruction() {
        return lastInstruction;
    }

    public Instruction getFirstInstruction() {
        return firstInstruction;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public Instruction getPhiInstruction(Integer identifier) {
        return phiInstructions.get(identifier);
    }

    public List<Instruction> getPhiInstructions() {
        List<Instruction> retList = new ArrayList<>();
        Instruction currentInstruction = getFirstInstruction();
        while (currentInstruction != null && currentInstruction.getBasicBlock() == this &&
                currentInstruction.getOpCode() == OpCode.PHI) {
            retList.add(currentInstruction);
            currentInstruction = currentInstruction.getNext();
        }
        return retList;
    }

    public void joinFromRight() {
        joiningFromLeft = false;
    }

    public boolean isJoiningFromLeft() {
        return joiningFromLeft;
    }

    public boolean isJoiningFromRight() {
        return !joiningFromLeft;
    }

    @Override
    public String toString() {
        return "BB" + number;
    }
}
