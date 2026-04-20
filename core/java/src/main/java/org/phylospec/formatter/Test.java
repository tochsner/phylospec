package org.phylospec.formatter;

import org.phylospec.ast.Stmt;
import org.phylospec.lexer.Lexer;
import org.phylospec.lexer.Token;
import org.phylospec.parser.Parser;

import java.util.List;

public class Test {

    static void main() {
        String text = """
                // per-statement
                Alignment filtered = subset(
                    // per-func
                    alignment=data, start=10, end=898  // end-of-line-2
                ) // end-of-line-3
              """;

        // run lexer

        Lexer lexer = new Lexer(text);
        List<Token> tokens = lexer.scanTokens();

        // run parser

        Parser parser = new Parser(tokens);
        List<Stmt> statements = parser.parse();

        Formatter formatter = new Formatter();

        System.out.println(formatter.format(statements));
    }

}
