package co.xenastudios.xlimbo.command;

import co.xenastudios.xlimbo.XLimboPlugin;
import co.xenastudios.xlimbo.config.Settings;
import co.xenastudios.xlimbo.util.BuildInfo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;

/**
 * {@code /xlimbo} admin command. Subcommands:
 * <ul>
 *   <li>{@code reload} — re-read + re-validate config and re-apply live.</li>
 *   <li>{@code info}   — build/version + runtime status.</li>
 * </ul>
 * Gated behind {@code xlimbo.admin}.
 */
public final class XLimboCommand implements CommandExecutor, TabCompleter {

    private static final String PERMISSION = "xlimbo.admin";
    private static final List<String> SUBCOMMANDS = List.of("reload", "info");

    private final XLimboPlugin plugin;

    public XLimboCommand(XLimboPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(Component.text("You don't have permission to do that.", NamedTextColor.RED));
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /xlimbo <reload|info>", NamedTextColor.YELLOW));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                boolean ok = plugin.reload();
                sender.sendMessage(ok
                        ? Component.text("xLimbo config reloaded.", NamedTextColor.GREEN)
                        : Component.text("Reload failed — see console. Previous config kept.", NamedTextColor.RED));
            }
            case "info" -> sendInfo(sender);
            default -> sender.sendMessage(Component.text("Unknown subcommand. Usage: /xlimbo <reload|info>", NamedTextColor.YELLOW));
        }
        return true;
    }

    private void sendInfo(CommandSender sender) {
        BuildInfo build = BuildInfo.load(plugin);
        Settings s = plugin.settings();
        sender.sendMessage(Component.text("xLimbo", NamedTextColor.AQUA)
                .append(Component.text(" v" + build.version(), NamedTextColor.GRAY)));
        sender.sendMessage(line("Commit", build.shortCommit()));
        sender.sendMessage(line("Built", build.buildTimestamp()));
        sender.sendMessage(line("World", s.world().name()));
        sender.sendMessage(line("Border", (long) s.world().borderSize() + " blocks"));
        sender.sendMessage(line("Spawn radius", "±" + s.world().spawnRadius()));
        sender.sendMessage(line("Main server", s.mainServer()));
        sender.sendMessage(line("Online", Integer.toString(plugin.getServer().getOnlinePlayers().size())));
    }

    private Component line(String key, String value) {
        return Component.text(key + ": ", NamedTextColor.GRAY)
                .append(Component.text(value, NamedTextColor.WHITE));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            return List.of();
        }
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            return SUBCOMMANDS.stream().filter(c -> c.startsWith(prefix)).toList();
        }
        return List.of();
    }
}
