// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.notice;

public record StaffTemplate(String prefix, String body) {

    public static StaffTemplate bare(String body) {
        return new StaffTemplate("", body);
    }

    public String render(Placeholders placeholders) {
        return prefix + placeholders.applyTo(body);
    }
}
