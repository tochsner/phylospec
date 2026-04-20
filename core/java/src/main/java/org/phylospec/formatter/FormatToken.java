package org.phylospec.formatter;

import java.util.List;

public abstract class FormatToken {

    public abstract void format(StringBuilder stringBuilder, int lw);

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
        public void format(StringBuilder stringBuilder, int lw) {
            stringBuilder.append(this.text);
        }
    }

    public static class Nest extends FormatToken {
        private FormatToken[] inner;

        public Nest(FormatToken inner) {
            this.inner = new FormatToken[] {inner};
        }

        public Nest(FormatToken... inner) {
            this.inner = inner;
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
        public void format(StringBuilder stringBuilder, int lw) {
            for (FormatToken formatToken : this.inner) {
                formatToken.format(stringBuilder, lw);
            }
        }
    }

    public static class MustBreak extends FormatToken {

        @Override
        public void format(StringBuilder stringBuilder, int lw) {
            stringBuilder.append("\n");
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
        public void format(StringBuilder stringBuilder, int lw) {
            stringBuilder.append(delimiter);
        }
    }

}
