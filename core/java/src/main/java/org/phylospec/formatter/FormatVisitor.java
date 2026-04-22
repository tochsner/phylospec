package org.phylospec.formatter;

import org.phylospec.ast.*;
import org.phylospec.lexer.Range;
import org.phylospec.lexer.TokenType;
import org.phylospec.parser.Parser;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.UnaryOperator;

public class FormatVisitor implements AstVisitor<FormatToken, FormatToken, FormatToken> {
    private final LinkedList<Trivia> trivia;
    private final Parser parser;

    public FormatVisitor(List<Trivia> trivia, Parser parser) {
        this.trivia = new LinkedList<>(trivia);
        this.parser = parser;
    }

    @Override
    public FormatToken visitDecoratedStmt(Stmt.Decorated stmt) {
        UnaryOperator<FormatToken> attachTrivia = this.beginTrivia(stmt);

        FormatToken formatToken = new FormatToken.Nest(
                new FormatToken.Text("@"),
                stmt.decorator.accept(this),
                new FormatToken.MustBreak(),
                stmt.statement.accept(this)
        );

        return attachTrivia.apply(formatToken);
    }

    @Override
    public FormatToken visitAssignment(Stmt.Assignment stmt) {
        UnaryOperator<FormatToken> attachTrivia = this.beginTrivia(stmt);

        FormatToken formatToken = new FormatToken.Nest(
                stmt.type.accept(this),
                new FormatToken.Text(" " + stmt.name + " = "),
                stmt.expression.accept(this)
        );

        return attachTrivia.apply(formatToken);
    }

    @Override
    public FormatToken visitDraw(Stmt.Draw stmt) {
        UnaryOperator<FormatToken> attachTrivia = this.beginTrivia(stmt);

        FormatToken formatToken = new FormatToken.Nest(
                stmt.type.accept(this),
                new FormatToken.Text(" " + stmt.name + " ~ "),
                stmt.expression.accept(this)
        );

        return attachTrivia.apply(formatToken);
    }

    @Override
    public FormatToken visitImport(Stmt.Import stmt) {
        UnaryOperator<FormatToken> attachTrivia = this.beginTrivia(stmt);

        FormatToken formatToken = new FormatToken.Nest(
                new FormatToken.Text("use "),
                new FormatToken.Text(String.join(".", stmt.namespace))
        );

        return attachTrivia.apply(formatToken);
    }

    @Override
    public FormatToken visitIndexedStmt(Stmt.Indexed indexed) {
        UnaryOperator<FormatToken> attachTrivia = this.beginTrivia(indexed);

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

        FormatToken formatToken = new FormatToken.Nest(parts);

        return attachTrivia.apply(formatToken);
    }

    @Override
    public FormatToken visitObservedAsStmt(Stmt.ObservedAs observedAs) {
        UnaryOperator<FormatToken> attachTrivia = this.beginTrivia(observedAs);

        FormatToken formatToken = new FormatToken.Nest(
                observedAs.stmt.accept(this),
                new FormatToken.Text(" observed as "),
                observedAs.observedAs.accept(this)
        );

        return attachTrivia.apply(formatToken);
    }

    @Override
    public FormatToken visitObservedBetweenStmt(Stmt.ObservedBetween observedBetween) {
        UnaryOperator<FormatToken> attachTrivia = this.beginTrivia(observedBetween);

        FormatToken formatToken = new FormatToken.Nest(
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

        return attachTrivia.apply(formatToken);
    }

    @Override
    public FormatToken visitLiteral(Expr.Literal expr) {
        UnaryOperator<FormatToken> attachTrivia = this.beginTrivia(expr);

        FormatToken formatToken;
        if (expr.value instanceof String string) {
            formatToken = new FormatToken.Nest(new FormatToken.Text("\"" + string + "\""));
        } else if (expr.unit == null) {
            formatToken = new FormatToken.Nest(new FormatToken.Text(expr.value.toString()));
        } else {
            formatToken = new FormatToken.Nest(
                    new FormatToken.Text(expr.value.toString()),
                    new FormatToken.Text(expr.unit.toString())
            );
        }

        return attachTrivia.apply(formatToken);
    }

    @Override
    public FormatToken visitStringTemplate(Expr.StringTemplate expr) {
        UnaryOperator<FormatToken> attachTrivia = this.beginTrivia(expr);

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

        FormatToken formatToken = new FormatToken.Nest(parts);

        return attachTrivia.apply(formatToken);
    }

    @Override
    public FormatToken visitVariable(Expr.Variable expr) {
        UnaryOperator<FormatToken> attachTrivia = this.beginTrivia(expr);

        FormatToken formatToken = new FormatToken.Nest(new FormatToken.Text(expr.variableName));

        return attachTrivia.apply(formatToken);
    }

    @Override
    public FormatToken visitTemplateVariable(Expr.TemplateVariable expr) {
        UnaryOperator<FormatToken> attachTrivia = this.beginTrivia(expr);

        FormatToken formatToken = new FormatToken.Nest(new FormatToken.Text(expr.variableName));

        return attachTrivia.apply(formatToken);
    }

    @Override
    public FormatToken visitOptionalTemplateVariable(Expr.OptionalTemplateVariable expr) {
        UnaryOperator<FormatToken> attachTrivia = this.beginTrivia(expr);

        FormatToken formatToken = new FormatToken.Nest(new FormatToken.Text(expr.variableName));

        return attachTrivia.apply(formatToken);
    }

    @Override
    public FormatToken visitUnary(Expr.Unary expr) {
        UnaryOperator<FormatToken> attachTrivia = this.beginTrivia(expr);

        FormatToken formatToken = new FormatToken.Nest(
                new FormatToken.Text(TokenType.getLexeme(expr.operator)),
                expr.right.accept(this)
        );

        return attachTrivia.apply(formatToken);
    }

    @Override
    public FormatToken visitBinary(Expr.Binary expr) {
        UnaryOperator<FormatToken> attachTrivia = this.beginTrivia(expr);

        FormatToken formatToken = new FormatToken.Nest(
                expr.left.accept(this),
                new FormatToken.Text(" " + TokenType.getLexeme(expr.operator) + " "),
                expr.right.accept(this)
        );

        return attachTrivia.apply(formatToken);
    }

    @Override
    public FormatToken visitCall(Expr.Call expr) {
        UnaryOperator<FormatToken> attachTrivia = this.beginTrivia(expr);

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

        FormatToken formatToken = new FormatToken.Nest(parts);

        return attachTrivia.apply(formatToken);
    }

    @Override
    public FormatToken visitAssignedArgument(Expr.AssignedArgument expr) {
        UnaryOperator<FormatToken> attachTrivia = this.beginTrivia(expr);

        FormatToken formatToken;
        if (expr.name == null) {
            formatToken = new FormatToken.Nest(expr.expression.accept(this));
        } else if (expr.expression instanceof Expr.Variable variable && expr.name.equals(variable.variableName)) {
            formatToken = new FormatToken.Nest(expr.expression.accept(this));
        } else {
            formatToken = new FormatToken.Nest(
                    new FormatToken.Text(expr.name),
                    new FormatToken.Text("="),
                    expr.expression.accept(this)
            );
        }

        return attachTrivia.apply(formatToken);
    }

    @Override
    public FormatToken visitDrawnArgument(Expr.DrawnArgument expr) {
        UnaryOperator<FormatToken> attachTrivia = this.beginTrivia(expr);

        FormatToken formatToken = new FormatToken.Nest(
                new FormatToken.Text(expr.name),
                new FormatToken.Text("~"),
                expr.expression.accept(this)
        );

        return attachTrivia.apply(formatToken);
    }

    @Override
    public FormatToken visitGrouping(Expr.Grouping expr) {
        UnaryOperator<FormatToken> attachTrivia = this.beginTrivia(expr);

        FormatToken formatToken = new FormatToken.Nest(
                new FormatToken.Text("("),
                new FormatToken.Nest(
                    new FormatToken.Break(),
                    expr.expression.accept(this),
                    new FormatToken.Break()
                ),
                new FormatToken.Text(")")
        );

        return attachTrivia.apply(formatToken);
    }

    @Override
    public FormatToken visitArray(Expr.Array expr) {
        UnaryOperator<FormatToken> attachTrivia = this.beginTrivia(expr);

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

        FormatToken formatToken = new FormatToken.Nest(parts);

        return attachTrivia.apply(formatToken);
    }

    @Override
    public FormatToken visitIndex(Expr.Index expr) {
        UnaryOperator<FormatToken> attachTrivia = this.beginTrivia(expr);

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

        FormatToken formatToken = new FormatToken.Nest(parts);

        return attachTrivia.apply(formatToken);
    }

    @Override
    public FormatToken visitRange(Expr.Range range) {
        UnaryOperator<FormatToken> attachTrivia = this.beginTrivia(range);

        FormatToken formatToken = new FormatToken.Nest(
                range.from.accept(this),
                new FormatToken.Text(":"),
                range.to.accept(this)
        );

        return attachTrivia.apply(formatToken);
    }

    @Override
    public FormatToken visitAtomicType(AstType.Atomic expr) {
        UnaryOperator<FormatToken> attachTrivia = this.beginTrivia(expr);

        FormatToken formatToken = new FormatToken.Nest(new FormatToken.Text(expr.name));

        return attachTrivia.apply(formatToken);
    }

    @Override
    public FormatToken visitGenericType(AstType.Generic expr) {
        UnaryOperator<FormatToken> attachTrivia = this.beginTrivia(expr);

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

        FormatToken formatToken = new FormatToken.Nest(parts);

        return attachTrivia.apply(formatToken);
    }

    /**
     * Collects any line comments preceding this node and returns a callback that prepends them to the finished token.
     * Call at the top of a visitor (pre-order), then apply the result to the token built by that visitor.
     */
    private UnaryOperator<FormatToken> beginTrivia(AstNode node) {
        Range nodeRange = this.parser.getRangeForAstNode(node);
        if (nodeRange == null) return t -> t;

        List<FormatToken> precedingTriviaTokens = new ArrayList<>();
        List<FormatToken> followingTriviaTokens = new ArrayList<>();

        while (!this.trivia.isEmpty()) {
            Trivia nextTrivia = this.trivia.peekFirst();
            if (isBefore(nextTrivia, nodeRange)) {
                this.trivia.pollFirst();
                if (nextTrivia instanceof Trivia.BlankLine) {
                    continue;
                }

                precedingTriviaTokens.add(nextTrivia.getFormatToken());
                precedingTriviaTokens.add(new FormatToken.MustBreak());

            } else if (isEOL(nextTrivia, nodeRange)) {
                this.trivia.pollFirst();
                if (nextTrivia instanceof Trivia.BlankLine) {
                    continue;
                }

                followingTriviaTokens.add(new FormatToken.Text("\t"));
                followingTriviaTokens.add(nextTrivia.getFormatToken());
            } else {
                break;
            }
        }

        if (precedingTriviaTokens.isEmpty()) return t -> t;

        return token -> {
            List<FormatToken> result = new ArrayList<>(precedingTriviaTokens);
            result.add(token);
            result.addAll(followingTriviaTokens);

            return new FormatToken.Nest(0, result);
        };
    }

    private boolean isBefore(Trivia trivia, Range astNodeRange) {
        return trivia.getRange().endLine < astNodeRange.startLine || trivia.getRange().endLine == astNodeRange.startLine && trivia.getRange().end < astNodeRange.start;
    }

    private boolean isEOL(Trivia trivia, Range astNodeRange) {
        return trivia.getRange().startLine == astNodeRange.endLine && astNodeRange.end < trivia.getRange().start;
    }

}
