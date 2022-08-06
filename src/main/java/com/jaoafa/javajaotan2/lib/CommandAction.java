/*
 * jaoLicense
 *
 * Copyright (c) 2022 jao Minecraft Server
 *
 * The following license applies to this project: jaoLicense
 *
 * Japanese: https://github.com/jaoafa/jao-Minecraft-Server/blob/master/jaoLICENSE.md
 * English: https://github.com/jaoafa/jao-Minecraft-Server/blob/master/jaoLICENSE-en.md
 */

package com.jaoafa.javajaotan2.lib;

import com.jagrosh.jdautilities.command.CommandEvent;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class CommandAction {
    private final String name;
    private final Consumer<CommandEvent> action;
    private final BiConsumer<CommandEvent, List<String>> namedAction;
    private final List<CommandAction> subActions;
    private final List<String> argNames;

    public CommandAction(String name, Consumer<CommandEvent> action) {
        this.name = name;
        this.action = action;
        this.namedAction = null;
        this.subActions = null;
        this.argNames = null;
    }

    public CommandAction(String name, List<CommandAction> subActions) {
        this.name = name;
        this.action = null;
        this.namedAction = null;
        this.subActions = subActions;
        this.argNames = null;
    }

    public CommandAction(String name, BiConsumer<CommandEvent, List<String>> action, List<String> argNames) {
        this.name = name;
        this.action = null;
        this.namedAction = action;
        this.subActions = null;
        this.argNames = argNames;
    }

    public String getName() {
        return name;
    }

    public Consumer<CommandEvent> getAction() {
        return action;
    }

    public BiConsumer<CommandEvent, List<String>> getNamedAction() {
        return namedAction;
    }

    public List<CommandAction> getSubActions() {
        return subActions;
    }

    public List<String> getArgumentNames() {
        return argNames;
    }

    public static boolean execute(CommandEvent event, List<CommandAction> actions) {
        return execute(event, 0, actions);
    }

    private static boolean execute(CommandEvent event, int index, List<CommandAction> actions) {
        CommandArgument args = new CommandArgument(event.getArgs());
        String name = args.getString(index);
        Optional<CommandAction> match = actions
            .stream()
            .filter(action -> action.getName().equalsIgnoreCase(name))
            .findFirst();
        if (match.isEmpty()) {
            return false;
        }
        CommandAction action = match.get();
        if (action.getAction() != null) {
            action.getAction().accept(event);
        } else if (action.getNamedAction() != null) {
            action.getNamedAction().accept(event, action.getArgumentNames());
        } else {
            execute(event, index + 1, action.getSubActions());
        }
        return true;
    }
}
