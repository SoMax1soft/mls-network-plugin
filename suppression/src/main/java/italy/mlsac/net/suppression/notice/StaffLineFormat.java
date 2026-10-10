// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.notice;

public interface StaffLineFormat {

    NoticeKey key();

    Placeholders placeholders(String actions);
}
