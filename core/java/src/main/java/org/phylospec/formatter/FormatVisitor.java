package org.phylospec.formatter;

import org.phylospec.ast.*;
import org.phylospec.lexer.Token;
import org.phylospec.lexer.TokenType;
import org.phylospec.parser.Parser;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class FormatVisitor implements AstVisitor<FormatToken, FormatToken, FormatToken> {
    private final LinkedList<Token> comments;
    private final Parser parser;

    public FormatVisitor(List<Token> comments, Parser parser) {
        this.comments = new LinkedList<>(comments);
        this.parser = parser;
    }

    @Override
    public FormatToken visitDecoratedStmt(Stmt.Decorated stmt) {
        return this.addLineComments(
                stmt,
                new FormatToken.Nest(
                    new FormatToken.Text("@"),
                    stmt.decorator.accept(this),
                    new FormatToken.MustBreak(),
                    stmt.statement.accept(this)
                )
        );
    }

    @Override
    public FormatToken visitAssignment(Stmt.Assignment stmt) {
        return this.addLineComments(
                stmt,
                new FormatToken.Nest(
                    stmt.type.accept(this),
                    new FormatToken.Text(" " + stmt.name + " = "),
                    stmt.expression.accept(this)
                )
        );
    }

    @Override
    public FormatToken visitDraw(Stmt.Draw stmt) {
        return this.addLineComments(
                stmt,
                new FormatToken.Nest(
                    stmt.type.accept(this),
                    new FormatToken.Text(" " + stmt.name + " ~ "),
                    stmt.expression.accept(this)
                )
        );
    }

    @Override
    public FormatToken visitImport(Stmt.Import stmt) {
        return new FormatToken.Nest(
                new FormatToken.Text("use "),
                new FormatToken.Text(String.join(".", stmt.namespace))
        );
    }

    @Override
    public FormatToken visitIndexedStmt(Stmt.Indexed indexed) {
        List<FormatToken> parts = new ArrayList<>();

        if (indexed.statement instanceof Stmt.Assignment assignment) {
            parts.add(assignment.type.accept(this));
            parts.add(new FormatToken.Text(" " + assignment.name));
        } else if (indexed.statement instanceof Stmt.Draw draw) {
            parts.add(draw.type.accept(this));
            parts.add(new FormatToken.Text(" " + draw.name));
        } else throw new RuntimeException();

        parts.add(new FormatToken.Text("["));

        for (int i = 0; i < indexed.indices.size(); i++) {
            parts.add(new FormatToken.Text(indexed.indices.get(i).variableName));

            if (i < indexed.indices.size() - 1) {
                parts.add(new FormatToken.Text(", "));
            }
        }

        parts.add(new FormatToken.Text("] = "));

        if (indexed.statement instanceof Stmt.Assignment assignment) {
            parts.add(assignment.expression.accept(this));
        } else if (indexed.statement instanceof Stmt.Draw draw) {
            parts.add(draw.expression.accept(this));
        } else throw new RuntimeException();

        for (int i = 0; i < indexed.indices.size(); i++) {
            parts.add(new FormatToken.Text(" for " + indexed.indices.get(i).variableName + " in "));
            parts.add(indexed.ranges.get(i).accept(this));
        }

        return new FormatToken.Nest(parts);
    }

    @Override
    public FormatToken visitObservedAsStmt(Stmt.ObservedAs observedAs) {
        return new FormatToken.Nest(
                observedAs.stmt.accept(this),
                new FormatToken.Text(" observed as "),
                observedAs.observedAs.accept(this)
        );
    }

    @Override
    public FormatToken visitObservedBetweenStmt(Stmt.ObservedBetween observedBetween) {
        return new FormatToken.Nest(
                observedBetween.stmt.accept(this),
                new FormatToken.Nest(
                    new FormatToken.Text(" observed between ["),
                    new FormatToken.Break(),
                    observedBetween.observedFrom.accept(this),
                    new FormatToken.Text(", "),
                    new FormatToken.Break(),
                    observedBetween.observedTo.accept(this),
                    new FormatToken.Break(),
                    new FormatToken.Text("]")
                )
        );
    }

    @Override
    public FormatToken visitLiteral(Expr.Literal expr) {
        if (expr.value instanceof String string) {
            return new FormatToken.Text("\"" + string + "\"");
        }
        if (expr.unit == null) {
            return new FormatToken.Text(expr.value.toString());
        } else {
            return new FormatToken.Nest(
                    new FormatToken.Text(expr.value.toString()),
                    new FormatToken.Text(expr.unit.toString())
            );
        }
    }

    @Override
    public FormatToken visitStringTemplate(Expr.StringTemplate expr) {
        List<FormatToken> parts = new ArrayList<>();

        parts.add(new FormatToken.Text("\""));

        for (Expr.StringTemplate.Part part : expr.parts) {
            if (part instanceof Expr.StringTemplate.StringPart(String value)) {
                parts.add(new FormatToken.Text(value));
            } else if (part instanceof Expr.StringTemplate.ExpressionPart(Expr.Variable expression)) {
                parts.add(new FormatToken.Text("${"));
                parts.add(new FormatToken.Text(expression.variableName));
                parts.add(new FormatToken.Text("}"));
            }
        }

        parts.add(new FormatToken.Text("\""));

        return new FormatToken.Nest(parts);
    }

    @Override
    public FormatToken visitVariable(Expr.Variable expr) {
        return new FormatToken.Text(expr.variableName);
    }

    @Override
    public FormatToken visitTemplateVariable(Expr.TemplateVariable expr) {
        return new FormatToken.Text(expr.variableName);
    }

    @Override
    public FormatToken visitOptionalTemplateVariable(Expr.OptionalTemplateVariable expr) {
        return new FormatToken.Text(expr.variableName);
    }

    @Override
    public FormatToken visitUnary(Expr.Unary expr) {
        return new FormatToken.Nest(
                new FormatToken.Text(TokenType.getLexeme(expr.operator)),
                expr.right.accept(this)
        );
    }

    @Override
    public FormatToken visitBinary(Expr.Binary expr) {
        return new FormatToken.Nest(
                expr.left.accept(this),
                new FormatToken.Text(" " + TokenType.getLexeme(expr.operator) + " "),
                expr.right.accept(this)
        );
    }

    @Override
    public FormatToken visitCall(Expr.Call expr) {
        List<FormatToken> parts = new ArrayList<>();
        parts.add(new FormatToken.Text(expr.functionName + "("));
        parts.add(new FormatToken.Break());

        List<FormatToken> argumentParts = new ArrayList<>();
        for (int i = 0; i < expr.arguments.length; i++) {
            argumentParts.add(expr.arguments[i].accept(this));

            if (i < expr.arguments.length - 1) {
                argumentParts.add(new FormatToken.Text(","));
                argumentParts.add(new FormatToken.Break(" "));
            }
        }
        parts.add(new FormatToken.Nest(0, argumentParts));

        parts.add(new FormatToken.Break());
        parts.add(new FormatToken.Text(")"));

        return new FormatToken.Nest(parts);
    }

    @Override
    public FormatToken visitAssignedArgument(Expr.AssignedArgument expr) {
        if (expr.name == null) {
            return expr.expression.accept(this);
        }
        if (expr.expression instanceof Expr.Variable var && expr.name.equals(var.variableName)) {
            return expr.expression.accept(this);
        }
        return new FormatToken.Nest(
                new FormatToken.Text(expr.name),
                new FormatToken.Text("="),
                expr.expression.accept(this)
        );
    }

    @Override
    public FormatToken visitDrawnArgument(Expr.DrawnArgument expr) {
        return new FormatToken.Nest(
                new FormatToken.Text(expr.name),
                new FormatToken.Text("~"),
                expr.expression.accept(this)
        );
    }

    @Override
    public FormatToken visitGrouping(Expr.Grouping expr) {
        return new FormatToken.Nest(
                new FormatToken.Text("("),
                new FormatToken.Nest(
                    new FormatToken.Break(),
                    expr.expression.accept(this),
                    new FormatToken.Break()
                ),
                new FormatToken.Text(")")
        );
    }

    @Override
    public FormatToken visitArray(Expr.Array expr) {
        List<FormatToken> parts = new ArrayList<>();

        parts.add(new FormatToken.Text("["));

        List<FormatToken> elementParts = new ArrayList<>();
        elementParts.add(new FormatToken.Break());

        List<FormatToken> innerElementParts = new ArrayList<>();
        for (int i = 0; i < expr.elements.size(); i++) {
            innerElementParts.add(expr.elements.get(i).accept(this));

            if (i < expr.elements.size() - 1) {
                innerElementParts.add(new FormatToken.Text(","));
                innerElementParts.add(new FormatToken.Break(" "));
            }
        }
        elementParts.add(new FormatToken.Nest(0, innerElementParts));

        elementParts.add(new FormatToken.Break());

        parts.add(new FormatToken.Nest(elementParts));
        parts.add(new FormatToken.Text("]"));

        return new FormatToken.Nest(parts);
    }

    @Override
    public FormatToken visitIndex(Expr.Index expr) {
        List<FormatToken> parts = new ArrayList<>();

        parts.add(expr.object.accept(this));

        parts.add(new FormatToken.Text("["));
        parts.add(new FormatToken.Break());

        for (int i = 0; i < expr.indices.size(); i++) {
            parts.add(expr.indices.get(i).accept(this));

            if (i < expr.indices.size() - 1) {
                parts.add(new FormatToken.Text(","));
                parts.add(new FormatToken.Break(" "));
            }
        }
        parts.add(new FormatToken.Text("]"));

        return new FormatToken.Nest(parts);
    }

    @Override
    public FormatToken visitRange(Expr.Range range) {
        return new FormatToken.Nest(
                range.from.accept(this),
                new FormatToken.Text(":"),
                range.to.accept(this)
        );
    }

    @Override
    public FormatToken visitAtomicType(AstType.Atomic expr) {
        return new FormatToken.Text(expr.name);
    }

    @Override
    public FormatToken visitGenericType(AstType.Generic expr) {
        List<FormatToken> parts = new ArrayList<>();
        parts.add(new FormatToken.Text(expr.name + "<"));
        for (int i = 0; i < expr.typeParameters.length; i++) {
            parts.add(expr.typeParameters[i].accept(this));

            if (i < expr.typeParameters.length - 1) {
                parts.add(new FormatToken.Text(","));
                parts.add(new FormatToken.Break(" "));
            }
        }
        parts.add(new FormatToken.Text(">"));

        return new FormatToken.Nest(parts);
    }

    /**
     * Finds the line comments before that line and adds them to the beginning of the nested DOM expression.
     */
    private FormatToken addLineComments(Stmt node, FormatToken.Nest nest) {
        List<FormatToken> commentLines = new ArrayList<>();

        int nodeStartLine = this.parser.getRangeForAstNode(node).startLine;
        int lastCommentLine = -1;
        while (!this.comments.isEmpty() && this.comments.peekFirst().range.startLine < nodeStartLine) {
            // because we are processing the comments and statements from top to bottom, all comments
            // before the statement are line comments (we would have processed them otherwise)

            Token comment = this.comments.pollFirst();

            if (lastCommentLine != -1 && 1 < comment.range.startLine - lastCommentLine) {
                // add a gap between comments
                commentLines.add(new FormatToken.MustBreak());
            }

            commentLines.add(new FormatToken.Text(comment.lexeme));
            commentLines.add(new FormatToken.MustBreak());

            lastCommentLine = comment.range.startLine;
        }

        if (commentLines.isEmpty()) {
            return nest;
        }

        // add a line break between the comment and the statement if needed
        if (1 < nodeStartLine - lastCommentLine) {
            // add a gap between comments
            commentLines.add(new FormatToken.MustBreak());
        }

        commentLines.add(nest);
        return new FormatToken.Nest(0, commentLines);
    }
}
