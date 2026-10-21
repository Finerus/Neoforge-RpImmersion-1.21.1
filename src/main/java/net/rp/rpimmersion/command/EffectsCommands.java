package net.rp.rpimmersion.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.rp.rpimmersion.compat.StaffChecker;
import net.rp.rpimmersion.config.EffectsConfig;
import net.rp.rpimmersion.effects.PlayerStateManager;

public class EffectsCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
                                LiteralCommandNode<CommandSourceStack> root) {
        var node = Commands.literal("effects").requires(StaffChecker::isStaffSource);

        node.then(Commands.literal("status").executes(ctx -> {
            boolean enabled;
            boolean hidden;
            try {
                enabled = EffectsConfig.ENABLE_EFFECTS.get();
                hidden = EffectsConfig.HIDE_POTION_PARTICLES.get();
            } catch (IllegalStateException e) {
                enabled = false;
                hidden = false;
            }
            final boolean fEnabled = enabled;
            final boolean fHidden = hidden;
            int tracked = PlayerStateManager.get().trackedCount();
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "§6[RPImmersion] Effects: §e" + (fEnabled ? "enabled" : "disabled")
                            + "\n§6Potion particles hidden: §e" + fHidden
                            + "\n§6Tracked players: §e" + tracked), false);
            return 1;
        }));

        node.then(Commands.literal("reload").executes(ctx -> {
            PlayerStateManager.get().syncSettingsToAll(ctx.getSource().getServer());
            ctx.getSource().sendSuccess(() -> Component.literal("§a[RPImmersion] Effects config synced."), true);
            return 1;
        }));

        node.then(Commands.literal("enable")
                .then(Commands.argument("value", BoolArgumentType.bool()).executes(ctx -> {
                    boolean v = BoolArgumentType.getBool(ctx, "value");
                    EffectsConfig.ENABLE_EFFECTS.set(v);
                    EffectsConfig.ENABLE_EFFECTS.save();
                    PlayerStateManager.get().syncSettingsToAll(ctx.getSource().getServer());
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "§a[RPImmersion] Effects " + (v ? "§eenabled" : "§edisabled") + "§a."), true);
                    return 1;
                })));

        node.then(Commands.literal("particles")
                .then(Commands.argument("hidden", BoolArgumentType.bool()).executes(ctx -> {
                    boolean v = BoolArgumentType.getBool(ctx, "hidden");
                    EffectsConfig.HIDE_POTION_PARTICLES.set(v);
                    EffectsConfig.HIDE_POTION_PARTICLES.save();
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "§a[RPImmersion] Potion particles " + (v ? "§ehidden" : "§evisible")
                                    + "§a for everyone (applies as effects are applied or refreshed)."), true);
                    return 1;
                })));

        node.then(Commands.literal("reset")
                .then(Commands.argument("target", EntityArgument.player()).executes(ctx -> {
                    ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                    PlayerStateManager.get().resetPlayer(target);
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "§a[RPImmersion] State reset for §e" + target.getName().getString()), true);
                    return 1;
                })));

        root.addChild(node.build());
    }
}