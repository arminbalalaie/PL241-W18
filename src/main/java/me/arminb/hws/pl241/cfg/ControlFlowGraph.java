package me.arminb.hws.pl241.cfg;

import ch.qos.logback.core.util.FileUtil;
import me.arminb.hws.pl241.frontend.FileReader;
import me.arminb.hws.pl241.frontend.Scanner;
import me.arminb.hws.pl241.ssa.Instruction;
import me.arminb.hws.pl241.symbol.Symbol;
import me.arminb.hws.pl241.symbol.SymbolTable;
import org.apache.commons.io.FileUtils;

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
    private HashMap<Integer, Instruction> instructions;

    // Instance
    private List<BasicBlock> basicBlocks;
    private String name;
    private Integer basicBlockCounter;

    private ControlFlowGraph(String name) {
        this.name = name;
        basicBlocks = new ArrayList<>();
        basicBlockCounter = 0;
        instructions = new HashMap<>();
    }

    public static void initialize() {
        controlFlowGraphs = new HashMap<>();
        controlFlowGraphs.put(MAIN, new ControlFlowGraph(MAIN));
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

    public Instruction getInstruction(Integer index) {
        return instructions.get(index);
    }

    public static void generateGraphFiles() {
        try {
            Path graphDirectory = Paths.get("graphs", FileReader.getInstance().getFileName());
            if (!(Files.isDirectory(graphDirectory))) {
                graphDirectory.toFile().mkdirs();
            }

            FileUtils.cleanDirectory(graphDirectory.toFile());
        } catch (IOException e) {
            throw new RuntimeException("Cannot create graphs directory!");
        }

        for (ControlFlowGraph controlFlowGraph: controlFlowGraphs.values()) {
            controlFlowGraph.generateControlFlowGraphFile();
            controlFlowGraph.generateDominationTreeFile();
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

    public void generateDominationTreeFile() {
        StringBuilder retString = new StringBuilder();

        retString.append("digraph " + getName() + " {\n\n");

        for (BasicBlock basicBlock: basicBlocks) {
            retString.append("\"" + basicBlock.toString() + "\" [shape=box];\n");
        }

        retString.append("\n");

        for (BasicBlock basicBlock: basicBlocks) {
            for (BasicBlock immDom: basicBlock.getImmediateDominations()) {
                retString.append(basicBlock.toString() + " -> " + immDom.toString() + ";\n");
            }
        }

        retString.append("\n}");

        createGraphFile(retString.toString(), "_dom");
    }

    public void generateControlFlowGraphFile() {
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

        createGraphFile(retString.toString());
    }

    private void createGraphFile(String graphDescription) {
        createGraphFile(graphDescription, "");
    }

    private void createGraphFile(String graphDescription, String fileNamePostfix) {
        try {
            Path graphFile = Paths.get("graphs", FileReader.getInstance().getFileName(),
                    getName() + fileNamePostfix + ".gv");
            Path psFile = Paths.get("graphs", FileReader.getInstance().getFileName(),
                    getName() + fileNamePostfix + ".ps");

            Files.write(graphFile, graphDescription.getBytes());
            Runtime.getRuntime().exec("dot -Tps " + graphFile + " -o " + psFile);
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
