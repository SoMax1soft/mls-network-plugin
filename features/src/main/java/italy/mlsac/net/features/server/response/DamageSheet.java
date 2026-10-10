// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.server.response;

import java.util.List;

public interface DamageSheet {

    List<String> applicableParts();

    double part(String name);

    void rewritePart(String name, double value);

    double baseDamage();

    void rewriteBase(double value);

    double finalDamage();
}
