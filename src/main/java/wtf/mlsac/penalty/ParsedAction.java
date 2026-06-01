/*
 * MLSAC is a GPLv3 licensed fork of a Minecraft anti-cheat system.
 * This project is community-maintained and not affiliated with any single upstream repository.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * This file is based on GPLv3 licensed work and includes modifications.
 * Derived from:
 *   - SlothAC (© 2025 KaelusMC, https://github.com/KaelusMC/SlothAC)
 *   - Grim (© 2025 GrimAnticheat, https://github.com/GrimAnticheat/Grim)
 *   - Client-side project (GPLv3: https://github.com/MLSAC/client-side)
 */


package wtf.mlsac.penalty;
public class ParsedAction {
    private final ActionType type;
    private final String command;
    public ParsedAction(ActionType type, String command) {
        this.type = type != null ? type : ActionType.RAW;
        this.command = command != null ? command : "";
    }
    public ActionType getType() {
        return type;
    }
    public String getCommand() {
        return command;
    }
    public boolean hasCommand() {
        return command != null && !command.isEmpty();
    }
    @Override
    public String toString() {
        return "ParsedAction{type=" + type + ", command='" + command + "'}";
    }
}