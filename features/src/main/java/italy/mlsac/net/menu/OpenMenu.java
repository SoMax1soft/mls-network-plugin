// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.menu;

public interface OpenMenu {

    MenuFrame render();

    MenuReaction click(int slot, ClickKind click);
}
