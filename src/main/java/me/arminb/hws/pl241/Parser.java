package me.arminb.hws.pl241;

public class Parser {
    Scanner scanner;

    public Parser(Scanner scanner) {
        this.scanner = scanner;
    }

    public void parse() {
        Token currentToken;
        do {
            currentToken = scanner.next();
            System.out.println(currentToken);
        } while (currentToken != Token.END_OF_FILE);
    }
}
