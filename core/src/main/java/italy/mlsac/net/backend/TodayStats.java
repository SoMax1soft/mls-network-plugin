// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend;

public record TodayStats(String date, long requestsUsed, long requestsLimit, long requestsRemaining,
                         long detections) {
}
