// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.server;

public interface SuppressionNotices {

    void toAlertSubscribers(String text);

    void toStaffAndOps(String text);

    void toConsole(String text);

    void warn(String line);
}
