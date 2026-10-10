// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.ultra;

import italy.mlsac.net.pipeline.rows.ClickRow;
import italy.mlsac.net.pipeline.rows.TickRow;
import java.util.List;

record HandedRow(TickRow row, List<ClickRow> clicks) {
}
