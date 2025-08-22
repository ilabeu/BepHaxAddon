package bep.hax.commands;

import bep.hax.modules.IgnoreSync;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.PlayerArgumentType;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.command.CommandSource;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static meteordevelopment.meteorclient.MeteorClient.mc;

public class IgnoreSyncCommand extends Command {
    public IgnoreSyncCommand() {
        super("ignoresync", "Manage 2b2t ignore list synchronization.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("sync")
            .executes(ctx -> {
                IgnoreSync module = Modules.get().get(IgnoreSync.class);
                if (module == null) {
                    error("IgnoreSync module not found!");
                    return SINGLE_SUCCESS;
                }
                
                if (!module.isActive()) {
                    error("IgnoreSync module must be enabled!");
                    return SINGLE_SUCCESS;
                }
                
                module.startSync();
                return SINGLE_SUCCESS;
            })
        );

        builder.then(literal("add")
            .then(argument("player", PlayerArgumentType.create())
                .executes(ctx -> {
                    IgnoreSync module = Modules.get().get(IgnoreSync.class);
                    if (module == null) {
                        error("IgnoreSync module not found!");
                        return SINGLE_SUCCESS;
                    }
                    
                    String player = ctx.getArgument("player", String.class);
                    module.addToLocalList(player);
                    return SINGLE_SUCCESS;
                })
            )
        );

        builder.then(literal("remove")
            .then(argument("player", PlayerArgumentType.create())
                .executes(ctx -> {
                    IgnoreSync module = Modules.get().get(IgnoreSync.class);
                    if (module == null) {
                        error("IgnoreSync module not found!");
                        return SINGLE_SUCCESS;
                    }
                    
                    String player = ctx.getArgument("player", String.class);
                    module.removeFromLocalList(player);
                    return SINGLE_SUCCESS;
                })
            )
        );

        builder.then(literal("list")
            .executes(ctx -> {
                IgnoreSync module = Modules.get().get(IgnoreSync.class);
                if (module == null) {
                    error("IgnoreSync module not found!");
                    return SINGLE_SUCCESS;
                }
                
                module.listLocal();
                return SINGLE_SUCCESS;
            })
        );

        builder.then(literal("clear")
            .then(literal("confirm")
                .executes(ctx -> {
                    IgnoreSync module = Modules.get().get(IgnoreSync.class);
                    if (module == null) {
                        error("IgnoreSync module not found!");
                        return SINGLE_SUCCESS;
                    }
                    
                    module.clearLocal();
                    return SINGLE_SUCCESS;
                })
            )
            .executes(ctx -> {
                warning("This will clear your entire local ignore list!");
                warning("Use '.ignoresync clear confirm' to confirm.");
                return SINGLE_SUCCESS;
            })
        );

        builder.then(literal("import")
            .then(argument("players", PlayerArgumentType.create())
                .executes(ctx -> {
                    IgnoreSync module = Modules.get().get(IgnoreSync.class);
                    if (module == null) {
                        error("IgnoreSync module not found!");
                        return SINGLE_SUCCESS;
                    }
                    
                    String playersRaw = ctx.getArgument("players", String.class);
                    String[] players = playersRaw.split(",");
                    
                    int added = 0;
                    for (String player : players) {
                        String trimmed = player.trim();
                        if (!trimmed.isEmpty()) {
                            module.addToLocalList(trimmed);
                            added++;
                        }
                    }
                    
                    info("Added " + added + " players to local ignore list");
                    return SINGLE_SUCCESS;
                })
            )
        );

        builder.then(literal("queue")
            .executes(ctx -> {
                IgnoreSync module = Modules.get().get(IgnoreSync.class);
                if (module == null) {
                    error("IgnoreSync module not found!");
                    return SINGLE_SUCCESS;
                }
                
                module.processQueue();
                return SINGLE_SUCCESS;
            })
        );

        builder.then(literal("status")
            .executes(ctx -> {
                IgnoreSync module = Modules.get().get(IgnoreSync.class);
                if (module == null) {
                    error("IgnoreSync module not found!");
                    return SINGLE_SUCCESS;
                }
                
                module.showStatus();
                return SINGLE_SUCCESS;
            })
        );

        builder.then(literal("help")
            .executes(ctx -> {
                info("IgnoreSync Commands:");
                info("  .ignoresync sync - Sync with server ignore list");
                info("  .ignoresync status - Show sync status and differences");
                info("  .ignoresync add <player> - Add player to local list");
                info("  .ignoresync remove <player> - Remove player from local list");
                info("  .ignoresync list - Show local ignore list");
                info("  .ignoresync clear - Clear local ignore list");
                info("  .ignoresync import <player1,player2,...> - Import multiple players");
                info("  .ignoresync queue - Process queued offline players");
                return SINGLE_SUCCESS;
            })
        );
    }
}