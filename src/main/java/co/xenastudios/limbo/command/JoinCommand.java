package co.xenastudios.limbo.command;

import co.xenastudios.limbo.LimboPlugin;
import co.xenastudios.limbo.config.Settings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.logging.Level;

/**
 * {@code /join} — sends the player back to the main server via the proxy.
 * Player-only, permission-gated, with a per-player cooldown to stop proxy spam.
 *
 * <p>The base command is declared in {@code plugin.yml}; configured aliases are
 * registered at enable via the server {@link CommandMap} (fail-safe — if that
 * fails the base command still works).
 */
public final class JoinCommand implements CommandExecutor {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final LimboPlugin plugin;

    public JoinCommand(LimboPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Settings settings = plugin.settings();
        Settings.JoinCommand cfg = settings.joinCommand();

        if (!cfg.enabled()) {
            sender.sendMessage(Component.text("This command is disabled."));
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command."));
            return true;
        }
        if (!cfg.permission().isBlank() && !player.hasPermission(cfg.permission())) {
            sender.sendMessage(Component.text("You don't have permission to do that."));
            return true;
        }

        long remaining = plugin.cooldownManager().tryUse(player.getUniqueId(), cfg.cooldownSeconds());
        if (remaining > 0) {
            int seconds = CooldownManager.toRemainingSeconds(remaining);
            try {
                player.sendMessage(MM.deserialize(cfg.messageCooldownTemplate(),
                        Placeholder.unparsed("seconds", Integer.toString(seconds))));
            } catch (Exception ignored) {
                // Malformed template shouldn't block the cooldown itself.
            }
            return true;
        }

        player.sendMessage(cfg.messageSending());
        plugin.proxyConnector().send(player, settings.mainServer());
        return true;
    }

    /** Register configured aliases into the server command map. Fail-safe. */
    public void registerAliases() {
        try {
            Settings.JoinCommand cfg = plugin.settings().joinCommand();
            CommandMap map = plugin.getServer().getCommandMap();
            for (String alias : cfg.aliases()) {
                if (alias == null || alias.isBlank() || alias.equalsIgnoreCase("join")) {
                    continue;
                }
                if (map.getCommand(alias) != null) {
                    continue; // don't clobber an existing command
                }
                Command dynamic = new Command(alias) {
                    @Override
                    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
                        return onCommand(sender, this, commandLabel, args);
                    }
                };
                dynamic.setDescription("Send yourself back to the main server.");
                map.register("limbo", dynamic);
            }
        } catch (Throwable t) {
            plugin.getLogger().log(Level.WARNING, "Failed to register /join aliases (base command still works).", t);
        }
    }
}
