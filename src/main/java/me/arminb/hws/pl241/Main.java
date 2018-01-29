package me.arminb.hws.pl241;

import java.nio.charset.StandardCharsets;

public class Main {
    public static void main(String[] args) {
        new Parser(new Scanner(new FileReader("test.pl241", StandardCharsets.US_ASCII))).parse();
    }
}
