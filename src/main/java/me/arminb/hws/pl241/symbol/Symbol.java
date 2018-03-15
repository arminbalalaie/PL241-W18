package me.arminb.hws.pl241.symbol;

import me.arminb.hws.pl241.cfg.ControlFlowGraph;
import me.arminb.hws.pl241.frontend.Result;
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
    private List<Result> valueList;
    private final Boolean isParam;
    private Integer relativeBaseAddress;

    public static Symbol variable(Integer identifier, Boolean isParam) {
        return new Symbol(identifier,
                Scanner.getInstance().identifierToString(identifier),
                SymbolType.VARIABLE,
                ControlFlowGraph.getCurrent().getName(),
                new ArrayList<>(),
                new ArrayList<>(),
                isParam);
    }

    public static Symbol variable(Integer identifier) {
        return variable(identifier,false);
    }

    public static Symbol variable(Integer identifier, String scope, Boolean isParam) {
        return new Symbol(identifier,
                Scanner.getInstance().identifierToString(identifier),
                SymbolType.VARIABLE,
                scope,
                new ArrayList<>(),
                new ArrayList<>(),
                isParam);
    }

    public static Symbol variable(Integer identifier, String scope) {
        return variable(identifier, scope, false);
    }

    public static Symbol array(Integer identifier, List<Integer> dimensions) {
        return new Symbol(identifier,
                Scanner.getInstance().identifierToString(identifier),
                SymbolType.ARRAY,
                ControlFlowGraph.getCurrent().getName(),
                dimensions,
                new ArrayList<>(),
                false);
    }

    public static Symbol function(Integer identifier, List<Symbol> parameters) {
        return new Symbol(identifier,
                Scanner.getInstance().identifierToString(identifier),
                SymbolType.FUNCTION,
                ControlFlowGraph.MAIN,
                new ArrayList<>(),
                parameters,
                false);
    }

    public static Symbol procedure(Integer identifier, List<Symbol> parameters) {
        return new Symbol(identifier,
                Scanner.getInstance().identifierToString(identifier),
                SymbolType.PROCEDURE,
                ControlFlowGraph.MAIN,
                new ArrayList<>(),
                parameters,
                false);
    }

    private Symbol(Integer identifier, String name, SymbolType type, String scope, List<Integer> dimensions,
                   List<Symbol> parameters, Boolean isParam) {
        this.identifier = new Integer(identifier);
        this.name = new String(name);
        this.type = type;
        this.scope = scope;
        this.dimensions = Collections.unmodifiableList(dimensions);
        this.parameters = Collections.unmodifiableList(parameters);
        this.valueList = new ArrayList<>();
        this.isParam = isParam;
        // set up relative base address
        MemoryAllocator.getInstance().allocate(this);
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

    public boolean isVariable() {
        return type == SymbolType.VARIABLE;
    }

    public boolean isFunction() {
        return type == SymbolType.FUNCTION;
    }

    public boolean isProcedure() {
        return type == SymbolType.PROCEDURE;
    }

    public boolean isArray() {
        return type == SymbolType.ARRAY;
    }

    public void addValue(Result value) {
        this.valueList.add(value);
    }

    public void resetValueListTo(Integer size) {
        for (int i=0; i<valueList.size()-size; i++) {
            valueList.remove(valueList.size()-1);
        }
    }

    public void resetValueList() {
        valueList = new ArrayList<>();
    }

    public Result getLastValue() {
        if (this.valueList.isEmpty()) {
            return null;
        }
        return this.valueList.get(this.valueList.size()-1);
    }

    @Override
    public String toString() {
        return this.getType() + "\t" + this.getIdentifier() + "\t"
                + this.getName() + "\t" + this.getDimensions().size() + "\t"
                + this.getScope() + "\t" + this.getParameters().size();
    }

    public Boolean isParam() {
        return isParam;
    }

    public List<Result> getValueList() {
        return valueList;
    }

    public Integer getRelativeBaseAddress() {
        return relativeBaseAddress;
    }

    public void setRelativeBaseAddress(Integer relativeBaseAddress) {
        this.relativeBaseAddress = relativeBaseAddress;
    }

    // this method creates instruction
    public Result getAbsoluteAddress() {
        Result baseAddress;
        if (isGlobal()) {
            baseAddress = Result.Address.DF.getResult();
        } else {
            baseAddress = Result.Address.FP.getResult();
        }
        return baseAddress.plus(new Result(Result.Type.CONSTANT, getRelativeBaseAddress()));
    }
}

