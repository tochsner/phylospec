package org.phylospec.formatter;

import org.phylospec.ast.Stmt;
import org.phylospec.lexer.Lexer;
import org.phylospec.parser.Parser;

import java.util.List;

public class Formatter {

    int MAX_LINE_GAP = 2;

    public String format(List<Stmt> statements, Lexer lexer, Parser parser) {
        FormatVisitor formatVisitor = new FormatVisitor();

        StringBuilder formattedString = new StringBuilder();

        int lastLineEnd = 1;

        for (Stmt stmt : statements) {
            int lineGap = parser.getRangeForAstNode(stmt).startLine - lastLineEnd;
            lineGap = Math.min(MAX_LINE_GAP, lineGap);

            for (int i = 0; i < lineGap; i++) {
                formattedString.append("\n");
            }

            FormatToken formatToken = stmt.accept(formatVisitor);
            formatToken.format(formattedString, 55, 0, 0, 0, false);

            lastLineEnd = parser.getRangeForAstNode(stmt).endLine;
        }

        return formattedString.toString();
    }

}
