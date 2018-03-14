package me.arminb.hws.pl241.frontend;

import me.arminb.hws.pl241.ssa.Instruction;
import me.arminb.hws.pl241.ssa.OpCode;
import me.arminb.hws.pl241.symbol.Symbol;
import me.arminb.hws.pl241.symbol.SymbolTable;

import java.util.HashMap;
import java.util.List;

public class Result {

    public static enum Type {
        CONSTANT,
        VALUE, // var, func and array
        PROCEDURE,
        SELECTOR,
        ADDRESS
    }

    public static enum Address {
        DF(0),
        SP(1),
        FP(2);

        int value;

        Address(int value) {
            this.value = value;
        }

        public Result getResult() {
            return new Result(Type.ADDRESS, value);
        }

        public static Address fromInteger(int value) {
            switch (value) {
                case 0: return DF;
                case 1: return SP;
                default: return FP;
            }
        }
    }

    private Type type;
    private Integer value; // number for constant, null for proc, ssaIndex for func, var, rel, and symbolId for selector and address
    private List<Result> arrayIndices; // arrayIndices expression's ssaIndex or number
    private HashMap<Token, OpCode> tokenToOpCodeMapper;

    public Result(Type type, Integer value) {
        this.type = type;
        this.value = value;
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

    public void setType(Type type) {
        this.type = type;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    public void setArrayIndices(List<Result> arrayIndices) {
        this.arrayIndices = arrayIndices;
    }

    public boolean isArrayIndicesConstant() {
        for (Result index: arrayIndices) {
            if (index.getType() != Type.CONSTANT) {
                return false;
            }
        }
        return true;
    }

    public Result getArrayRelativeAddress() {
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
