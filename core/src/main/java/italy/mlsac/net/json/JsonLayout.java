// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.json;

public interface JsonLayout {

    JsonLayout COMPACT = new CompactLayout();

    JsonLayout PRETTY = new PrettyLayout();

    void open(StringBuilder out, char bracket, int depth, boolean empty);

    void separator(StringBuilder out, int depth, boolean first);

    void nameSeparator(StringBuilder out);

    void close(StringBuilder out, char bracket, int depth, boolean empty);
}
