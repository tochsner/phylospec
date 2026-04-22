package org.phylospec.formatter;

import java.util.Arrays;
import java.util.List;

public abstract class FormatToken {

    public abstract int format(StringBuilder stringBuilder, int maxWidth, int widthBefore, int widthAfter, int indent, boolean applyBreaks);

    public abstract int getBaseWidth();

    protected abstract boolean mustBreak();

    protected abstract boolean canBreak();

    public static class Nest extends FormatToken {
        private FormatToken[] inner;
        private int indentOnBreak = 4;

        public Nest(FormatToken... inner) {
            this.inner = inner;
        }

        public Nest(int indentOnBreak, List<FormatToken> inner) {
            this(inner.toArray(new FormatToken[0]));
            this.indentOnBreak = indentOnBreak;
        }

        public Nest(List<FormatToken> inner) {
            this(inner.toArray(new FormatToken[0]));
        }

        public FormatToken[] getInner() {
            return inner;
        }

        public void setInner(FormatToken... inner) {
            this.inner = inner;
        }

        @Override
        public int format(StringBuilder stringBuilder, int maxWidth, int widthBefore, int widthAfter, int indent, boolean applyBreaks) {
            int baseWidth = this.getBaseWidth();

            boolean applyBreaksHere = this.mustBreak() || maxWidth < widthBefore + baseWidth + widthAfter;

            int oldIndent = indent;

            int numLineBreaks = 0;
            for (FormatToken token : this.inner) {
                if (token.canBreak()) {
                    numLineBreaks++;
                }
            }

            if (applyBreaksHere && 0 < numLineBreaks) {
                indent += this.indentOnBreak;
            }

            int numBreaksAdded = 0;
            for (int i = 0; i < this.inner.length; i++) {
                FormatToken formatToken = this.inner[i];

                if (formatToken.canBreak()) numBreaksAdded++;

                if (numBreaksAdded == numLineBreaks)
                    indent = oldIndent;

                int widthUntilBreak = 0;
                for (int j = i + 1; j < this.inner.length; j++) {
                    if (this.inner[j].canBreak()) break;
                    widthUntilBreak += this.inner[j].getBaseWidth();
                }

                widthBefore = formatToken.format(
                        stringBuilder,
                        maxWidth,
                        widthBefore,
                        widthAfter + widthUntilBreak,
                        indent,
                        applyBreaksHere
                );
            }

            return widthBefore;
        }

        @Override
        public int getBaseWidth() {
            int currBaseWidth = 0;
            int maxBaseWidth = 0;

            for (FormatToken token : this.inner) {
                currBaseWidth += token.getBaseWidth();

                if (token instanceof MustBreak) {
                    maxBaseWidth = Math.max(maxBaseWidth, currBaseWidth);
                    currBaseWidth = 0;
                }
            }

            return Math.max(maxBaseWidth, currBaseWidth);
        }

        @Override
        protected boolean mustBreak() {
            return Arrays.stream(this.inner).anyMatch(FormatToken::mustBreak);
        }

        @Override
        protected boolean canBreak() {
            return false;
        }
    }

    public static class Text extends FormatToken {
        private String text;

        public Text(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        @Override
        public int format(StringBuilder stringBuilder, int maxWidth, int widthBefore, int widthAfter, int indent, boolean applyBreaks) {
            stringBuilder.append(this.text);
            return widthBefore + this.text.length();
        }

        @Override
        public int getBaseWidth() {
            return this.text.length();
        }

        @Override
        protected boolean mustBreak() {
            return false;
        }

        @Override
        protected boolean canBreak() {
            return false;
        }
    }

    public static class Break extends FormatToken {

        private final String delimiter;

        public Break() {
            this.delimiter = "";
        }

        public Break(String delimiter) {
            this.delimiter = delimiter;
        }

        @Override
        public int format(StringBuilder stringBuilder, int maxWidth, int widthBefore, int widthAfter, int indent, boolean applyBreaks) {
            if (applyBreaks) {
                stringBuilder.append("\n");
                for (int i = 0; i < indent; i++) {
                    stringBuilder.append(" ");
                }
                return indent;
            } else {
                stringBuilder.append(delimiter);
                return widthBefore + delimiter.length();
            }
        }

        @Override
        public int getBaseWidth() {
            return this.delimiter.length();
        }

        @Override
        protected boolean mustBreak() {
            return false;
        }

        @Override
        protected boolean canBreak() {
            return true;
        }
    }

    public static class MustBreak extends FormatToken {

        @Override
        public int format(StringBuilder stringBuilder, int maxWidth, int widthBefore, int widthAfter, int indent, boolean applyBreaks) {
            stringBuilder.append("\n");

            for (int i = 0; i < indent; i++) {
                stringBuilder.append(" ");
            }

            return indent;
        }

        @Override
        public int getBaseWidth() {
            return 0;
        }

        @Override
        protected boolean mustBreak() {
            return true;
        }

        @Override
        protected boolean canBreak() {
            return true;
        }
    }

}
