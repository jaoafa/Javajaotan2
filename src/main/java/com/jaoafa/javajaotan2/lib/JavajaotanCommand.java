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

import cloud.commandframework.Command;
import cloud.commandframework.jda.JDACommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class JavajaotanCommand {
    public static String permRoles(List<Roles> roles) {
        if(roles == null) return null;
        return roles.stream()
            .map(Roles::getRoleId)
            .map(Object::toString).collect(Collectors.joining("|"));
    }

    public static class Detail {
        private final String name;
        private final List<String> aliases;
        private final String description;
        private final List<Roles> allowRoles;

        public Detail(@NotNull String name, @NotNull String description) {
            this.name = name;
            this.aliases = new ArrayList<>();
            this.description = description;
            this.allowRoles = null;
        }

        public Detail(@NotNull String name, @NotNull String description, @Nullable List<Roles> allowRoles) {
            this.name = name;
            this.aliases = new ArrayList<>();
            this.description = description;
            this.allowRoles = allowRoles;
        }

        public Detail(@NotNull String name, @NotNull List<String> aliases, @NotNull String description) {
            this.name = name;
            this.aliases = aliases;
            this.description = description;
            this.allowRoles = null;
        }

        public Detail(@NotNull String name, @NotNull List<String> aliases, @NotNull String description, @Nullable List<Roles> allowRoles) {
            this.name = name;
            this.aliases = aliases;
            this.description = description;
            this.allowRoles = allowRoles;
        }

        /**
         * ??????????????????????????????
         *
         * @return ???????????????
         */
        public String getName() {
            return name;
        }

        /**
         * ?????????????????????????????????????????????
         *
         * @return ??????????????????????????????
         */
        public List<String> getAliases() {
            return aliases;
        }

        /**
         * ???????????????????????????????????????
         *
         * @return ?????????????????????
         */
        public String getDescription() {
            return description;
        }

        /**
         * ???????????????????????????????????????????????????????????????
         *
         * @return ???????????????????????????????????????
         */
        public List<Roles> getAllowRoles() {
            return allowRoles;
        }
    }

    public static class Cmd {
        Command<JDACommandSender>[] commands;

        @SafeVarargs
        public Cmd(@NonNull Command<JDACommandSender>... commands) {
            this.commands = commands;
        }

        /**
         * Command????????????????????????
         *
         * @return Command?????????
         */
        public List<Command<JDACommandSender>> getCommands() {
            return List.of(this.commands);
        }
    }
}
