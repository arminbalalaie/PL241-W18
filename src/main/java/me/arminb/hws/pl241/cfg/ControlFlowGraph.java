package me.arminb.hws.pl241.cfg;

import me.arminb.hws.pl241.frontend.Scanner;
import me.arminb.hws.pl241.symbol.Symbol;
import me.arminb.hws.pl241.symbol.SymbolTable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ControlFlowGraph {
    // Static
    public static final String MAIN = "main";
    private static Map<String, ControlFlowGraph> controlFlowGraphs;
    private static ControlFlowGraph current;

    // Instance
    private List<BasicBlock> basicBlocks;
    private String name;

    private ControlFlowGraph(String name) {
        this.name = name;
        basicBlocks = new ArrayList<>();
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

    public List<BasicBlock> getBasicBlocks() {
        return basicBlocks;
    }

    public String getName() {
        return name;
    }
}
