// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.json;

final class PrettyLayout implements JsonLayout {

    @Override
    public void open(StringBuilder out, char bracket, int depth, boolean empty) {
        out.append(bracket);
    }

    @Override
    public void separator(StringBuilder out, int depth, boolean first) {
        if (!first) {
            out.append(',');
        }
        newLine(out, depth + 1);
    }

    @Override
    public void nameSeparator(StringBuilder out) {
        out.append(": ");
    }

    @Override
    public void close(StringBuilder out, char bracket, int depth, boolean empty) {
        if (!empty) {
            newLine(out, depth);
        }
        out.append(bracket);
    }

    private static void newLine(StringBuilder out, int depth) {
        out.append('\n');
        for (int i = 0; i < depth; i++) {
            out.append("  ");
        }
    }
}
