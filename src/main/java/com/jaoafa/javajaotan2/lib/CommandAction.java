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

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.entities.Message;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class CommandAction {
    private final String name;
    private final Consumer<CommandEvent> action;
    private final BiConsumer<CommandEvent, List<String>> namedAction;
    private final List<CommandAction> subActions;
    private final List<String> argNames;
    private final String description;

    /**
     * CommandAction クラスを作成します。<br>
     * このクラスを利用する場合、{@link Command#execute(CommandEvent)} で {@link CommandAction#execute(CommandWithActions, CommandEvent)} を呼び出してください。<br>
     * <br>
     * このコンストラクタは、<b>名前付きの引数</b>を持たず、このアクションの下にサブコマンド（グループ）がない場合に利用します。<br>
     * （名前付きの引数を用いず、位置引数で値を取得する場合はこのメソッドを使います。）<br>
     * 例えば、次のようなコマンドを実装する場合です。<br>
     * <ul>
     *     <li><code>/play air-horn</code>: エアーホーンを再生する</li>
     *     <li><code>/notify clear</code>: すべてのNotifyを削除する</li>
     *     <li><code>/notify new &lt;Text...&gt;</code>: Notifyとして <i>Text...</i> を追加する。<br>
     *     （{@link CommandArgument#getString(int)} で <i>Text...</i> の値を取得する場合に限る）</li>
     * </ul>
     *
     * @param name   アクション名
     * @param action 当該コマンド実行時の実行関数
     *
     * @see Command#execute(CommandEvent)
     */
    public CommandAction(String name, Consumer<CommandEvent> action, String description) {
        this.name = name;
        this.action = action;
        this.namedAction = null;
        this.subActions = null;
        this.argNames = null;
        this.description = description;
    }

    /**
     * CommandAction クラスを作成します。<br>
     * このクラスを利用する場合、{@link Command#execute(CommandEvent)} で {@link CommandAction#execute(CommandWithActions, CommandEvent)} を呼び出してください。<br>
     * <br>
     * このコンストラクタは、このアクションの下にサブコマンド（グループ）がある場合に利用します。このアクションの下には入れ子でグループを追加することもできます。<br>
     * 例えば、次のようなコマンドを実装する場合です。<br>
     * <ul>
     *     <li><code>/messages management new &lt;Text...&gt;</code>: メッセージとして <i>Text...</i> を追加する</li>
     * </ul>
     *
     * @param name       アクション名
     * @param subActions サブコマンド（グループ）
     *
     * @see Command#execute(CommandEvent)
     */
    public CommandAction(String name, List<CommandAction> subActions) {
        this.name = name;
        this.action = null;
        this.namedAction = null;
        this.subActions = subActions;
        this.argNames = null;
        this.description = null;
    }

    /**
     * CommandAction クラスを作成します。<br>
     * このクラスを利用する場合、{@link Command#execute(CommandEvent)} で {@link CommandAction#execute(CommandWithActions, CommandEvent)} を呼び出してください。<br>
     * <br>
     * このコンストラクタは、<b>名前付きの引数</b>を持ち、このアクションの下にサブコマンド（グループ）がない場合に利用します。<br>
     * 例えば、次のようなコマンドを実装する場合です。<br>
     * <ul>
     *     <li><code>/notify new &lt;Text...&gt;</code>: Notifyとして <i>Text...</i> を追加する。<br>
     *     （{@link CommandArgument#getString(String)} で <i>Text...</i> の値を取得する場合に限る）</li>
     *     <li><code>/ban add &lt;UserId&gt;</code>: <i>UserId</i> のユーザーをBanする。<br>
     *     （{@link CommandArgument#getString(String)} で <i>UserId</i> の値を取得する場合に限る）</li>
     * </ul>
     *
     * @param name     アクション名
     * @param action   当該コマンド実行時の実行関数
     * @param argNames 引数の名前リスト
     *
     * @see Command#execute(CommandEvent)
     */
    public CommandAction(String name, BiConsumer<CommandEvent, List<String>> action, List<String> argNames, String description) {
        this.name = name;
        this.action = null;
        this.namedAction = action;
        this.subActions = null;
        this.argNames = argNames;
        this.description = description;
    }

    /**
     * このアクションの名前を取得します。
     *
     * @return このアクションの名前
     */
    public String getName() {
        return name;
    }

    /**
     * このアクションの実行関数を取得します。<br>
     * このアクションに名前付き引数がある場合や、サブコマンド（グループ）を持つ場合は null を返します。
     *
     * @return このアクションの実行関数
     */
    public Consumer<CommandEvent> getAction() {
        return action;
    }

    /**
     * このアクションの実行関数を取得します。<br>
     * このアクションに名前付き引数がない場合や、サブコマンド（グループ）を持つ場合は null を返します。
     *
     * @return このアクションの実行関数
     */
    public BiConsumer<CommandEvent, List<String>> getNamedAction() {
        return namedAction;
    }

    /**
     * このアクションの下にあるサブコマンド（グループ）を取得します。<br>
     * このアクションに名前付き引数がない場合や、サブコマンド（グループ）を持たない場合は null を返します。
     *
     * @return このアクションの下にあるサブコマンド（グループ）
     */
    public List<CommandAction> getSubActions() {
        return subActions;
    }

    /**
     * このアクションの名前付き引数名リストを取得します。<br>
     * このアクションに名前付き引数がない場合は null を返します。
     *
     * @return このアクションの名前付き引数名リスト
     */
    public List<String> getArgumentNames() {
        return argNames;
    }

    /**
     * このアクションの説明を取得します。<br>
     * このアクションに説明がない場合は null を返します。
     *
     * @return このアクションの説明
     */
    public String getDescription() {
        return description;
    }

    /**
     * このアクションを実行します。<br>
     * このメソッドは、{@link Command#execute(CommandEvent)} で呼び出す必要があります。
     *
     * @param cmd   呼び出し元のコマンド定義（this）
     * @param event CommandEvent
     */
    public static void execute(CommandWithActions cmd, CommandEvent event) {
        Message message = event.getMessage();
        if (!execute(event, 0, cmd.getActions())) {
            message.reply(("""
                指定されたコマンドのアクションが見つかりませんでした。
                利用可能なアクションは次の通りです：```
                - /%s %s
                ```""").formatted(
                cmd.getName(),
                String.join("\n- /%s ".formatted(cmd.getName()), getActionNames(cmd.getActions()))
            )).queue();
        }
    }

    private static boolean execute(CommandEvent event, int index, List<CommandAction> actions) {
        CommandArgument args = new CommandArgument(event.getArgs());
        if (!args.has(index)) {
            return false;
        }
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

    public static String getArguments(List<CommandAction> actions) {
        List<String> commands = actions
            .stream()
            .map(action -> {
                if (action.getSubActions() != null) {
                    return List.of(getArguments(action.getSubActions()));
                } else if (action.getNamedAction() != null) {
                    return List.of("%s %s: %s".formatted(
                        action.getName(),
                        action
                            .getArgumentNames()
                            .stream()
                            .map("<%s>"::formatted)
                            .collect(Collectors.joining(" ")),
                        action.getDescription()
                    ));
                } else {
                    return List.of("%s: %s".formatted(action.getName(), action.getDescription()));
                }
            })
            .flatMap(Collection::stream)
            .toList();

        return String.join("\n", commands);
    }

    private static List<String> getActionNames(List<CommandAction> actions) {
        return actions
            .stream()
            .map(action -> {
                if (action.getSubActions() != null) {
                    return getActionNames(action.getSubActions()).stream().map(name -> action.getName() + " " + name).toList();
                } else {
                    return List.of(action.getName());
                }
            })
            .flatMap(Collection::stream)
            .toList();
    }
}
