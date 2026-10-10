// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.menu;

import italy.mlsac.net.command.CommandActor;
import italy.mlsac.net.command.Subcommand;
import italy.mlsac.net.command.SubcommandName;
import italy.mlsac.net.text.MessageBook;
import java.util.List;
import java.util.function.BooleanSupplier;

public final class SuspectsSubcommand implements Subcommand {

    private final MenuDesk menus;
    private final MessageBook messages;
    private final BooleanSupplier guiSwitchedOn;

    public SuspectsSubcommand(BooleanSupplier guiSwitchedOn, MenuDesk menus, MessageBook messages) {
        this.guiSwitchedOn = guiSwitchedOn;
        this.messages = messages;
        this.menus = menus;
    }

    @Override
    public void execute(CommandActor invoker, List<String> args) {
        boolean open = guiSwitchedOn.getAsBoolean();
        invoker.actorUuid().filter(viewer -> open).ifPresentOrElse(
                viewer -> menus.openSuspects(viewer, invoker.name()),
                () -> refuseUnless(open, invoker));
    }

    @Override
    public SubcommandName name() {
        return SubcommandName.SUSPECTS;
    }

    private void refuseUnless(boolean open, CommandActor invoker) {
        if (!open) {
            invoker.send(messages.prefixed("module-disabled"));
        }
    }
}
