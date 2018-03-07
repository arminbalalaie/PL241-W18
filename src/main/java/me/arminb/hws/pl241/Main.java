package me.arminb.hws.pl241;

import me.arminb.hws.pl241.frontend.FileReader;
import me.arminb.hws.pl241.frontend.Parser;

import java.nio.charset.StandardCharsets;

public class Main {
    public static void main(String[] args) {
        FileReader.initialize("test.pl241", StandardCharsets.US_ASCII);
        Parser.getInstance().parse();
    }
}
