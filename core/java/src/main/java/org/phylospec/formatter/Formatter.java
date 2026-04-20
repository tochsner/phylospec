package org.phylospec.formatter;

import org.phylospec.ast.Stmt;

import java.util.List;

public class Formatter {

    public String format(List<Stmt> statements) {
        FormatVisitor formatVisitor = new FormatVisitor();

        StringBuilder formattedString = new StringBuilder();
        for (Stmt stmt : statements) {
            FormatToken formatToken = stmt.accept(formatVisitor);
            formatToken.format(formattedString, 35, 0, 0, 0, false);
            formattedString.append("\n");
        }

        return formattedString.toString();
    }

}
