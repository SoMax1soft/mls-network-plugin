// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.menu;

import italy.mlsac.net.text.TemplateValues;
import java.util.Optional;

record PageSpan(int page, int pages, int total, boolean hasMore) {

    boolean hasPrevious() {
        return page > 0;
    }

    Optional<TemplateValues> buttonValues(ButtonAction action, TemplateValues shared) {
        return switch (action) {
            case PREV_PAGE -> hasPrevious() ? Optional.of(shared.and("{PAGE}", page)) : Optional.empty();
            case NEXT_PAGE -> hasMore ? Optional.of(shared.and("{PAGE}", page + 2)) : Optional.empty();
            default -> Optional.of(shared.and("{CURRENT}", page + 1)
                    .and("{TOTAL}", Math.max(1, pages))
                    .and("{PAGE}", page + 1)
                    .and("{COUNT}", total));
        };
    }
}
