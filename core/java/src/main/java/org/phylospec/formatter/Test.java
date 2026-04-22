package org.phylospec.formatter;

import org.phylospec.ast.Stmt;
import org.phylospec.lexer.Lexer;
import org.phylospec.lexer.Token;
import org.phylospec.parser.Parser;

import java.util.List;

public class Test {

    static void main() {
        String text = """
                 // comment 1
                 Real x = 5 // comment 2
                  // comment 3
                 data {
                 // comment 4
                     Real x = 5  // comment 5
                     Real x = 100
                 } 
                 
                 // comment 6
                 data {
                     Real x = 5 
                     Real x = 100
                 } 
                 
                 Real x = 5
                 
                 data { // comment 7
                     Real x = 5 
                     Real x = 100
                 } 
              """;

        // run lexer

        Lexer lexer = new Lexer(text);
        List<Token> tokens = lexer.scanTokens();

        // run parser

        Parser parser = new Parser(tokens);
        List<Stmt> statements = parser.parse();

        Formatter formatter = new Formatter();

        System.out.println(formatter.format(statements, lexer, parser));
    }

}
