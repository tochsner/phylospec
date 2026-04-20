package org.phylospec.formatter;

import org.phylospec.ast.Stmt;
import org.phylospec.lexer.Lexer;
import org.phylospec.lexer.Range;
import org.phylospec.parser.Parser;

import java.util.*;

public class Formatter {

    int MAX_WIDTH = 55;
    int MAX_LINE_GAP = 2;
    int INDENT = 4;

    public String format(List<Stmt> statements, Lexer lexer, Parser parser) {
        FormatVisitor formatVisitor = new FormatVisitor();
        StringBuilder formattedString = new StringBuilder();

        if (statements.isEmpty()) return "";

        // for each statement, we record the gap to the previous one

        Map<Stmt, Integer> stmtGaps = new IdentityHashMap<>();
        int lastLineEnd = 1;

        for (Stmt stmt : statements) {
            Range range = parser.getRangeForAstNode(stmt);
            int lineGap = range.startLine - lastLineEnd;
            lineGap = Math.min(MAX_LINE_GAP, lineGap);
            stmtGaps.put(stmt, lineGap);
            lastLineEnd = range.endLine;
        }

        // we group the statements by block

        List<List<Stmt>> stmtsPerBlock = new ArrayList<>();

        for (Stmt stmt : statements) {
            if (stmtsPerBlock.isEmpty()) {
                stmtsPerBlock.add(new ArrayList<>());
            } else if (stmtsPerBlock.getLast().getFirst().block != stmt.block) {
                stmtGaps.put(stmt, 1);
                stmtsPerBlock.add(new ArrayList<>());
            }
            stmtsPerBlock.getLast().add(stmt);
        }

        // we print the statements per block

        for (List<Stmt> stmts : stmtsPerBlock) {
            Stmt.Block currentBlock = stmts.getFirst().block;

            // add block header if needed

            switch (currentBlock) {
                case Stmt.Block.NoBlock none: {
                    break;
                }
                case Stmt.Block.Data data: {
                    formattedString.append("\n\ndata {");
                    break;
                }
                case Stmt.Block.Model model: {
                    formattedString.append("\n\nmodel {");
                    break;
                }
                case Stmt.Block.Mcmc mcmc: {
                    formattedString.append("\n\nmcmc {");
                    break;
                }
                case Stmt.Block.Custom custom: {
                    formattedString.append("\n\n").append(custom.blockName()).append(" {");
                    break;
                }
            }

            // set indent
            int indent = currentBlock instanceof Stmt.Block.NoBlock ? 0 : INDENT;

            // print statements

            for (Stmt stmt : stmts) {
                formattedString.append("\n".repeat(Math.max(0, stmtGaps.get(stmt))));
                formattedString.append(" ".repeat(Math.max(0, indent)));

                FormatToken formatToken = stmt.accept(formatVisitor);
                formatToken.format(formattedString, MAX_WIDTH, 0, 0, indent, false);
            }

            if (!(currentBlock instanceof Stmt.Block.NoBlock)) {
                formattedString.append("\n}\n");
            }
        }

        return formattedString.toString();
    }

}
