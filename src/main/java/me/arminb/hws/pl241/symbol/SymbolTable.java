package me.arminb.hws.pl241.symbol;

import me.arminb.hws.pl241.cfg.ControlFlowGraph;
import me.arminb.hws.pl241.frontend.Parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SymbolTable {
    private static SymbolTable instance;

    private Map<String, Map<Integer, Symbol>> symbols;

    public static SymbolTable getInstance() {
        if (instance == null) {
            instance = new SymbolTable();
            prePopulateWithBuiltinFunctions();
        }
        return instance;
    }

    private static void prePopulateWithBuiltinFunctions() {
        instance.add(Symbol.function(0, new ArrayList<>())); // InputNum()
        List<Symbol> params = new ArrayList<>();
        Symbol xParam = Symbol.variable(1, "OutputNum");
        instance.add(xParam); // x param for OutputNum(x)
        params.add(xParam);
        instance.add(Symbol.procedure(2, params)); // OutputNum(x)
        instance.add(Symbol.procedure(3, new ArrayList<>())); // OutputNewLine()
    }

    private SymbolTable() {
        symbols = new HashMap<>();
    }

    public void add(Symbol symbol) {
        if (contains(symbol.getIdentifier(), symbol.getScope())) {
            Parser.getInstance().exitError(symbol.getName()
                    + " is already defined in scope " + symbol.getScope() + "!");
        }
        if (!symbols.containsKey(symbol.getScope())) {
            symbols.put(symbol.getScope(), new HashMap<>());
        }
        symbols.get(symbol.getScope()).put(symbol.getIdentifier(), symbol);
    }

    // returns the symbol with the same identifier in the same scope. If there is not such a symbol, returns the global one
    public Symbol get(Integer identifier, String scope) {
        if (!contains(identifier, scope)) {
            return getGlobal(identifier);
        }
        return symbols.get(scope).get(identifier);
    }

    public boolean contains(Integer identifier, String scope) {
        if (symbols.containsKey(scope) && symbols.get(scope).containsKey(identifier))
            return true;
        return false;
    }

    // get with current scope
    public Symbol get(Integer identifier) {
        return get(identifier, ControlFlowGraph.getCurrent().getName());
    }


    private Symbol getGlobal(Integer identifier) {
        if (!symbols.containsKey(ControlFlowGraph.getMain().getName())) {
            return null;
        }
        return symbols.get(ControlFlowGraph.getMain().getName()).get(identifier);
    }

    // contains with current scope
    public boolean contains(Integer identifier) {
        return contains(identifier, ControlFlowGraph.getCurrent().getName());
    }

    public void resetValueLists() {
        for (String scope: symbols.keySet()) {
            for (Symbol symbol: symbols.get(scope).values()) {
                symbol.resetValueList();
            }
        }
    }

    public void print() {
        for (String scope : symbols.keySet()) {
            for (Integer identifier : symbols.get(scope).keySet()) {
                System.out.println(symbols.get(scope).get(identifier));
            }
        }
    }
}
