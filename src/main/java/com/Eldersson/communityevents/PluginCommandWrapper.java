package com.Eldersson.communityevents;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;

public class PluginCommandWrapper extends Command {
    private final CommandExecutor executor;
    private final TabCompleter completer;

    public PluginCommandWrapper(String name, CommandExecutor executor) {
        super(name);
        this.executor = executor;
        this.completer = executor instanceof TabCompleter ? (TabCompleter) executor : null;
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        return executor.onCommand(sender, this, label, args);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        if (completer != null) {
            return completer.onTabComplete(sender, this, alias, args);
        }
        return super.tabComplete(sender, alias, args);
    }
}
