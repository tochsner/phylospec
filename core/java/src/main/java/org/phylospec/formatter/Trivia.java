package org.phylospec.formatter;

import org.phylospec.lexer.Range;
import org.phylospec.lexer.Token;

public abstract class Trivia {

    public abstract Range getRange();
    public abstract FormatToken getFormatToken();

    public static class Comment extends Trivia {

        private final Token comment;

        public Comment(Token comment) {
            this.comment = comment;
        }

        @Override
        public Range getRange() {
            return this.comment.range;
        }

        @Override
        public FormatToken getFormatToken() {
            return new FormatToken.Text(this.comment.lexeme);
        }

    }

    public static class BlankLine extends Trivia {

        private final int lineNumber;

        public BlankLine(int lineNumber) {
            this.lineNumber = lineNumber;
        }

        @Override
        public Range getRange() {
            return new Range(lineNumber, 0, 1);
        }

        @Override
        public FormatToken getFormatToken() {
            return new FormatToken.MustBreak();
        }

    }

    public static class InvalidLine extends Trivia {

        private final Range range;
        private final String text;

        public InvalidLine(Range range, String text) {
            this.range = range;
            this.text = text;
        }

        @Override
        public Range getRange() {
            return this.range;
        }

        @Override
        public FormatToken getFormatToken() {
            return new FormatToken.Text(this.text);
        }

    }

}
