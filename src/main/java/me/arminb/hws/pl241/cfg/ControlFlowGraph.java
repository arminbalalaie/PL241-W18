package me.arminb.hws.pl241.cfg;

import me.arminb.hws.pl241.frontend.Scanner;
import me.arminb.hws.pl241.ssa.Instruction;
import me.arminb.hws.pl241.symbol.Symbol;
import me.arminb.hws.pl241.symbol.SymbolTable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ControlFlowGraph {
    // Static
    public static final String MAIN = "main";
    private static Map<String, ControlFlowGraph> controlFlowGraphs;
    private static ControlFlowGraph current;
    private static HashMap<Integer, Instruction> instructions;

    // Instance
    private List<BasicBlock> basicBlocks;
    private String name;
    private Integer basicBlockCounter;

    private ControlFlowGraph(String name) {
        this.name = name;
        basicBlocks = new ArrayList<>();
        basicBlockCounter = 0;
    }

    public static void initialize() {
        controlFlowGraphs = new HashMap<>();
        controlFlowGraphs.put(MAIN, new ControlFlowGraph(MAIN));
        instructions = new HashMap<>();
    }

    public static void setCurrentCFG(ControlFlowGraph controlFlowGraph) {
        current = controlFlowGraph;
    }

    public static ControlFlowGraph getCurrent() {
        return current;
    }

    public static ControlFlowGraph create(String name) {
        if (controlFlowGraphs == null) {
            throw new RuntimeException("ControlFlowGraph should be first initialized!");
        }

        if (controlFlowGraphs.containsKey(name)) {
            Symbol functionSymbol = SymbolTable.getInstance().get(Scanner.getInstance().stringToIdentifier(name));
            throw new RuntimeException("A " + functionSymbol.getType().toString().toLowerCase()
                    + " with name " + name + " already exists!");
        }

        ControlFlowGraph controlFlowGraph = new ControlFlowGraph(name);
        controlFlowGraphs.put(name, controlFlowGraph);
        return controlFlowGraph;
    }

    public static ControlFlowGraph get(String name) {
        return controlFlowGraphs.get(name);
    }

    public static ControlFlowGraph getMain() {
        return controlFlowGraphs.get(MAIN);
    }

    public static Instruction getInstruction(Integer index) {
        return instructions.get(index);
    }

    public static void generateGraphFiles() {
        for (ControlFlowGraph controlFlowGraph: controlFlowGraphs.values()) {
            controlFlowGraph.generateGraphFile();
        }
    }

    public List<BasicBlock> getBasicBlocks() {
        return basicBlocks;
    }

    public void addBasicBlock(BasicBlock basicBlock) {
        basicBlock.setNumber(basicBlockCounter++);
        basicBlocks.add(basicBlock);
    }

    public String getName() {
        return name;
    }

    public void addInstruction(Instruction instruction) {
        this.instructions.put(instruction.getIndex(), instruction);
    }

    public void generateGraphFile() {
        try {
            if (!(Files.exists(Paths.get("graphs")) && new File("graphs").isDirectory())) {
                Files.createDirectory(Paths.get("graphs"));
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot create graphs directory!");
        }

        StringBuilder retString = new StringBuilder();

        retString.append("digraph " + getName() + " {\n\n");

        for (BasicBlock basicBlock: basicBlocks) {
            StringBuilder insString = new StringBuilder();

            Instruction currentInstruction = basicBlock.getFirstInstruction();
            if (currentInstruction != null) {
                do {
                    insString.append(currentInstruction.toString()).append("\n");
                    currentInstruction = currentInstruction.getNext();
                } while (currentInstruction != basicBlock.getLastInstruction().getNext());
                insString.deleteCharAt(insString.length()-1);
            }

            retString.append("\"" + basicBlock.toString() + "\" [shape=box, label=\"" + basicBlock.toString()
                    + "\n=================\n" + insString.toString() + "\"];\n");
        }

        retString.append("\n");

        for (BasicBlock basicBlock: basicBlocks) {
            if (basicBlock.getFallThroughBlock() != null) {
                retString.append(basicBlock.toString() + " -> " + basicBlock.getFallThroughBlock().toString() + ";\n");
            }

            if (basicBlock.getBranchBlock() != null) {
                retString.append(basicBlock.toString() + " -> " + basicBlock.getBranchBlock().toString() + ";\n");
            }
        }

        retString.append("\n}");

        try {
            Path graphFile = Paths.get("graphs", getName() + ".gv");
            Path psFile = Paths.get("graphs", getName() + ".ps");

            Files.write(Paths.get("graphs", getName() + ".gv"), retString.toString().getBytes());
            Runtime.getRuntime().exec("dot -Tps " + graphFile.toString() + " -o " + psFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        StringBuilder retString = new StringBuilder();
        Instruction currentInstruction = basicBlocks.get(0).getFirstInstruction();
        while (currentInstruction != null) {
            retString.append(currentInstruction + "\n");
            currentInstruction = currentInstruction.getNext();
        }
        return retString.toString();
    }
}
