package me.arminb.hws.pl241.symbol;

import me.arminb.hws.pl241.cfg.ControlFlowGraph;
import me.arminb.hws.pl241.frontend.Scanner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Symbol {
    private final Integer identifier;
    private final String name;
    private final SymbolType type;
    private final String scope;
    private final List<Integer> dimensions;
    private final List<Symbol> parameters;

    public static Symbol variable(Integer identifier) {
        return new Symbol(identifier,
                Scanner.getInstance().identifierToString(identifier),
                SymbolType.VARIABLE,
                ControlFlowGraph.getCurrent().getName(),
                new ArrayList<>(),
                new ArrayList<>());
    }

    public static Symbol variable(Integer identifier, String scope) {
        return new Symbol(identifier,
                Scanner.getInstance().identifierToString(identifier),
                SymbolType.VARIABLE,
                scope,
                new ArrayList<>(),
                new ArrayList<>());
    }

    public static Symbol array(Integer identifier, List<Integer> dimensions) {
        return new Symbol(identifier,
                Scanner.getInstance().identifierToString(identifier),
                SymbolType.ARRAY,
                ControlFlowGraph.getCurrent().getName(),
                dimensions,
                new ArrayList<>());
    }

    public static Symbol function(Integer identifier, List<Symbol> parameters) {
        return new Symbol(identifier,
                Scanner.getInstance().identifierToString(identifier),
                SymbolType.FUNCTION,
                ControlFlowGraph.MAIN,
                new ArrayList<>(),
                parameters);
    }

    public static Symbol procedure(Integer identifier, List<Symbol> parameters) {
        return new Symbol(identifier,
                Scanner.getInstance().identifierToString(identifier),
                SymbolType.PROCEDURE,
                ControlFlowGraph.MAIN,
                new ArrayList<>(),
                parameters);
    }

    private Symbol(Integer identifier, String name, SymbolType type, String scope, List<Integer> dimensions, List<Symbol> parameters) {
        this.identifier = new Integer(identifier);
        this.name = new String(name);
        this.type = type;
        this.scope = scope;
        this.dimensions = Collections.unmodifiableList(dimensions);
        this.parameters = Collections.unmodifiableList(parameters);
    }

    public Integer getIdentifier() {
        return identifier;
    }

    public String getName() {
        return name;
    }

    public SymbolType getType() {
        return type;
    }

    public String getScope() {
        return scope;
    }

    public List<Integer> getDimensions() {
        return dimensions;
    }

    public List<Symbol> getParameters() {
        return parameters;
    }

    public boolean isGlobal() {
        return scope == ControlFlowGraph.getMain().getName();
    }

    @Override
    public String toString() {
        return this.getType() + "\t" + this.getIdentifier() + "\t"
                + this.getName() + "\t" + this.getDimensions().size() + "\t"
                + this.getScope() + "\t" + this.getParameters().size();
    }
}

