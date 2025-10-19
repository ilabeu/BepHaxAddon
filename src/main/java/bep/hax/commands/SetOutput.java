package bep.hax.commands;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.systems.modules.Modules;
import bep.hax.modules.StashMover;
import net.minecraft.command.CommandSource;
import static meteordevelopment.meteorclient.MeteorClient.mc;
public class SetOutput extends Command {
    public SetOutput() {
        super("setoutput", "Start output area selection for StashMover module");
    }
    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            if (mc.player == null) return 0;
            StashMover module = Modules.get().get(StashMover.class);
            if (module != null) {
                if (module.isSelecting()) {
                    module.cancelSelection();
                    info("Previous selection cancelled");
                }
                module.startOutputSelection();
                info("§bOutput area selection started!");
                info("§eLeft-click the first corner block");
                info("§7Press §cESC §7to cancel selection");
            } else {
                error("StashMover module not found!");
            }
            return SINGLE_SUCCESS;
        });
    }
}
