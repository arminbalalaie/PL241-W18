package me.arminb.hws.pl241;

public enum Token {
    ERROR("", 0),
    TIMES("*", 1),
    DIV("/", 2),
    PLUS("+", 11),
    MINUS("-", 22),
    EQUAL("==", 20),
    NOT_EQUAL("!=", 21),
    LESS_THAN("<", 22),
    GREATER_THAN_OR_EQUAL(">=", 23),
    LESS_THAN_OR_EQUAL("<=", 24),
    GREATER_THAN(">", 25),
    PERIOD(".", 30),
    COMMA(",", 31),
    OPEN_BRACKET("[", 32),
    CLOSE_BRACKET("]", 33),
    CLOSE_PARENTHESIS(")", 34),
    BECOMES("<-", 40),
    THEN("then", 41),
    OD("od", 42),
    OPEN_PARENTHESIS("(", 50),
    NUMBER("number", 60),
    IDENTIFIER("identifier", 61),
    SEMICOLON(";", 70),
    END("}", 80),
    DO("do", 81),
    FI("fi", 82),
    ELSE("else", 90),
    LET("let", 100),
    CALL("call", 101),
    IF("if", 102),
    WHILE("while", 103),
    RETURN("return", 104),
    VAR("var", 110),
    ARRAY("array", 111),
    FUNCTION("function",112),
    PROCEDURE("procedure", 113),
    BEGIN("{", 150),
    MAIN("main", 200),
    END_OF_FILE("end of file", 255);

    private String strValue;
    private Integer intValue;

    Token(String strValue, Integer intValue) {
        this.strValue = strValue;
        this.intValue = intValue;
    }

    public String getStrValue() {
        return strValue;
    }

    public Integer getIntValue() {
        return intValue;
    }
}
