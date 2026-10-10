// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.json;

final class CompactLayout implements JsonLayout {

    @Override
    public void open(StringBuilder out, char bracket, int depth, boolean empty) {
        out.append(bracket);
    }

    @Override
    public void separator(StringBuilder out, int depth, boolean first) {
        if (!first) {
            out.append(',');
        }
    }

    @Override
    public void nameSeparator(StringBuilder out) {
        out.append(':');
    }

    @Override
    public void close(StringBuilder out, char bracket, int depth, boolean empty) {
        out.append(bracket);
    }
}
