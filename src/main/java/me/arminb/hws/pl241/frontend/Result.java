package me.arminb.hws.pl241.frontend;

import me.arminb.hws.pl241.ssa.Instruction;
import me.arminb.hws.pl241.ssa.OpCode;
import me.arminb.hws.pl241.symbol.Symbol;
import me.arminb.hws.pl241.symbol.SymbolTable;

import java.util.HashMap;
import java.util.List;

public class Result {

    public enum Type {
        CONSTANT,
        VALUE, // var, func and array
        PROCEDURE,
        SELECTOR,
        ADDRESS
    }

    public enum Address {
        DF(0, new Result(Type.ADDRESS, 0)),
        SP(1,new Result(Type.ADDRESS, 1)),
        FP(2, new Result(Type.ADDRESS, 2));

        private int value;
        private Result result;

        Address(int value, Result result) {
            this.value = value;
            this.result = result;
        }

        public Result getResult() {
            return result;
        }

        public int getValue() {
            return value;
        }

        public static Address fromInteger(int value) {
            switch (value) {
                case 0: return DF;
                case 1: return SP;
                default: return FP;
            }
        }
    }

    private final Type type;
    private final Integer value; // number for constant, null for proc, ssaIndex for func, var, rel, and symbolId for selector and address
    private List<Result> arrayIndices; // arrayIndices expression's ssaIndex or number
    private HashMap<Token, OpCode> tokenToOpCodeMapper;

    public Result(Type type, Integer value) {
        this.type = type;
        this.value = new Integer(value);
        arrayIndices = null;
        prePopulateTokenToOpCodeMapper();
    }

    private void prePopulateTokenToOpCodeMapper() {
        tokenToOpCodeMapper = new HashMap<>();
        tokenToOpCodeMapper.put(Token.EQUAL, OpCode.BNE);
        tokenToOpCodeMapper.put(Token.NOT_EQUAL, OpCode.BEQ);
        tokenToOpCodeMapper.put(Token.LESS_THAN, OpCode.BGE);
        tokenToOpCodeMapper.put(Token.LESS_THAN_OR_EQUAL, OpCode.BGT);
        tokenToOpCodeMapper.put(Token.GREATER_THAN, OpCode.BLE);
        tokenToOpCodeMapper.put(Token.GREATER_THAN_OR_EQUAL, OpCode.BLT);
    }

    public void setArrayIndices(List<Result> arrayIndices) {
        this.arrayIndices = arrayIndices;
    }

    // this can be only used for arrays
    public Result getArrayRelativeAddress() {
        if (type != Type.SELECTOR || arrayIndices.isEmpty()) {
            throw new RuntimeException("This method should only be used for arrays!");
        }

        Symbol arraySymbol = SymbolTable.getInstance().get(getValue());
        Integer lastDimensionIndex = Math.min(arrayIndices.size(), arraySymbol.getDimensions().size()) - 1;

        Result lastAddResult = new Result(Type.CONSTANT, 0).plus(arrayIndices.get(lastDimensionIndex));
        for (int i = 0; i < lastDimensionIndex; i++) {
            Result mulResult = new Result(Type.CONSTANT, arraySymbol.getDimensions().get(i + 1)).times(arrayIndices.get(i));
            lastAddResult = lastAddResult.plus(mulResult);
        }

        // word to byte
        return lastAddResult.times(new Result(Type.CONSTANT, 4));
    }

    public Type getType() {
        return type;
    }

    public Integer getValue() {
        return value;
    }

    public Integer getAddress() {
        if (type == Type.ADDRESS) {
            return SymbolTable.getInstance().get(value).getRelativeBaseAddress();
        }
        return null;
    }

    public List<Result> getArrayIndices() {
        return arrayIndices;
    }

    // we wont get types selector and procedure here
    public Result plus(Result result) {
        if (this.getType() == Type.CONSTANT && result.getType() == Type.CONSTANT) {
            return new Result(Type.CONSTANT, this.getValue() + result.getValue());
        }
        Instruction instruction = Instruction.add(this, result);
        return new Result(Type.VALUE, instruction.getIndex());
    }

    // we wont get types selector and procedure here
    public Result minus(Result result) {
        if (this.getType() == Type.CONSTANT && result.getType() == Type.CONSTANT) {
            return new Result(Type.CONSTANT, this.getValue() - result.getValue());
        }
        Instruction instruction = Instruction.sub(this, result);
        return new Result(Type.VALUE, instruction.getIndex());
    }

    // we wont get types selector and procedure here
    public Result times(Result result) {
        if (this.getType() == Type.CONSTANT && result.getType() == Type.CONSTANT) {
            return new Result(Type.CONSTANT, this.getValue() * result.getValue());
        }
        Instruction instruction = Instruction.mul(this, result);
        return new Result(Type.VALUE, instruction.getIndex());
    }

    // we wont get types selector and procedure here
    public Result div(Result result) {
        if (this.getType() == Type.CONSTANT && result.getType() == Type.CONSTANT) {
            return new Result(Type.CONSTANT, this.getValue() / result.getValue());
        }
        Instruction instruction = Instruction.div(this, result);
        return new Result(Type.VALUE, instruction.getIndex());
    }

    // we wont get types selector and procedure here
    public Result relation(Result result, Token relationToken) {
        Result cmpResult;
        if (this.getType() == Type.CONSTANT && result.getType() == Type.CONSTANT) {
            cmpResult = new Result(Type.CONSTANT, this.getValue() - result.getValue());
        } else {
            cmpResult = new Result(Type.VALUE, Instruction.cmp(this, result).getIndex());
        }

        return new Result(Type.VALUE, Instruction.conditionalBranch(tokenToOpCodeMapper.get(relationToken), cmpResult).getIndex());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!Result.class.isAssignableFrom(obj.getClass())) {
            return false;
        }
        final Result other = (Result) obj;
        if (this.type != other.type) {
            return false;
        }
        if (this.value != other.value) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        if (type == Type.CONSTANT) {
            return "#" + value;
        } else if (value == null) {
            return "null";
        } else if (type == Type.ADDRESS) {
            return "*" + Address.fromInteger(getValue());
        } else {
            return value.toString();
        }
    }
}
