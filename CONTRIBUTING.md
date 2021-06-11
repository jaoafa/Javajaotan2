# Contributing

開発に参加する方は必ずこの文書を読んでください

## Development Environment

開発に関する環境情報です。

- IDE: IntelliJ IDEA (お勧め)
- Language: Java 11
- Package Manager: Maven

Eclipse などでも開発できますが、開発のサポートは IntelliJ IDEA のみ対応します。

## Talk Language

原則、コミットのメッセージやプルリクエストのメッセージなどは日本語で記述してください。

## Project board

プロジェクトのタスク管理ボードとして [GitHub の Project 機能](https://github.com/jaoafa/Javajaotan2/projects/1) を使用しています。  
タスクが多い際は `Ctrl+F` や `F3` でページ内検索をすることをお勧めします。

## Development Process

開発に参加する手順は次のとおりです。

1. `jaoafa/Javajaotan2` の `master` ブランチを自分のユーザーへフォークする
2. コードを書く (後述する [Precautions for development](#precautions-for-development) もお読みください)
3. 期待通りに動作するかどうかテストする
4. コミットする
5. プッシュする
6. `jaoafa/Javajaotan2` にプルリクエストを送信する

プログラミングを始める前に、`jaoafa/Javajaotan2` (一般的に `upstream` と呼ばれます) から**リベースプルを実施することを強くお勧めします**。(これをしないとコンフリクトする場合があります)

`upstream` の登録は以下の方法で行えます。

![リモート定義イメージ画像](https://i.imgur.com/w1CValK.png)

1. プロジェクトを右クリックし、 `Git` -> `リモート管理` と進む
2. `+` を押し、名前に `upstream` 、 URL に `https://github.com/jaoafa/Javajaotan2.git` と打ち込む
3. `OK` を押し登録する

リベースプルは以下の方法で行えます。

![リベースプルイメージ画像](https://i.imgur.com/MqCXMrq.png)

1. 右下のブランチ名が表示されている欄をクリックする
2. 表示されるウィンドウの右上 ↙️(フェッチボタン) を押し、最新の情報に更新する
3. `upstream/master` をクリックし、「リベースを使用して現在のブランチにプル」を押す
4. 成功したことが表示されれば完了

## Test Process

Javajaotan2 の動作確認（テスト）を行うためには、以下の手順を実施してください。

**執筆中**

## Precautions for development

開発にあたり、次の注意事項をご確認ください。

### General

- コマンド・機能の開発を始める前に、次の作業を実施してください。
  - **`upstream/master` からリベースプルを行い、最新の状態に更新する**
  - **[Projects](https://github.com/jaoafa/Javajaotan2/projects/1) で、該当する看板があれば `In Progress` に移動する**
  - 必要に応じて、ブランチを分ける  
    （ブランチを分けることで同時に複数のコマンド・機能を開発できます。この際 `upstream/master` を元としてブランチを作成してください）
  - 必要に応じて、該当する Issue の `Assignees` に自分を追加する
- 将来的に追加・修正などを行わなければならない項目がある場合は、 `// TODO <Message>` で TODO を登録してください。
- ローカル変数はなにかしらの理由がある場合を除き小文字で始めてください。
- `config.json` で設定される設定情報は `JavajaotanConfig` にあり、 `Main.getJavajaotanConfig()` から取得できます。
- 複数のクラスにわたって使用される変数は `JavajaotanData` に変数を作成し、 Getter と Setter を使用して管理してください。
- 複数のクラスにわたって多く使用される関数は `JavajaotanLibrary` に関数を作成し、Javadoc を書いたうえで `extends JavajaotanLibrary` して利用してください。
- データベースは jaoMain と ZakuroHat の二つがありますが、原則 jaoMain が使用されます。それぞれ `JavajaotanData.getMainMySQLDBManager` `JavajaotanData.getZKRHatMySQLDBManager` で取得できます。
- コマンドによって Bot が送信しなければならないメッセージは原則 `Message.reply` によるリプライ形式でのメッセージ送信をお願いします。

### Command

- 使用しているコマンドフレームワークは [Incendo/cloud](https://github.com/Incendo/cloud) です。
  - ドキュメントは [こちら](https://incendo.github.io/cloud) です。
- 全てのコマンドは [`src/main/java/com/jaoafa/javajaotan2/command/Cmd_<CommandName>.java`](src/main/java/com/jaoafa/javajaotan2/command) に配置され、これらが自動で読み込まれます。
- 同時に、クラス名は `Cmd_<CommandName>` でなければなりません。`<CommandName>` は大文字・小文字を問いません。
- また、ここに配置されるコマンドクラスは `com.jaoafa.javajaotan2.lib.CommandPremise` インターフェースを実装する必要があります。（`implements CommandPremise`）
- コマンドの情報（コマンド名・説明）は `details()` で定義します。
- コマンドの内容は `register()` で定義します。このメソッドは Main クラスの `registerCommand` から呼び出され、コマンドが追加されます。

### Event

- 全てのイベント駆動の機能は [`src/main/java/com/jaoafa/javajaotan2/event/Event_<FuncName>.java`](src/main/java/com/jaoafa/javajaotan2/event) に配置され、これらが自動で読み込まれます。
- 同時に、クラス名は `Event_<FuncName>` でなければなりません。
- また、ここに配置されるコマンドクラスは `net.dv8tion.jda.api.hooks.ListenerAdapter` インターフェースを継承する必要があります。（`extends ListenerAdapter`）
- イベントを受け取るためには、各イベントクラスを引数に取り、また関数名を `on<各イベントクラス名>` にしなければなりません。また、 `@Override` でオーバーライドしなければなりません。 (例: `@Override public void onGuildMessageReceivedEvent(GuildMessageReceivedEvent event)`)
- `<FuncName>` は自由で構いません

## Git

### Commit

- 発生しているエラーなどはコミット・プルリクエスト前にすべて修正してください。
- コミットメッセージは **[CommitLint のルール](https://github.com/conventional-changelog/commitlint/tree/master/%40commitlint/config-conventional#rules) である以下に沿っていることを期待しますが、必須ではありません。**
- 次の形式でコミットメッセージを指定してください: `type(scope): subject` (e.g. `fix(home): message`)
  - `type`, `subject` は必須、 `scope` は必須ではありません
- `type-enum`: `type` は必ず次のいずれかにしなければなりません
  - `build`: ビルド関連
  - `ci`: CI 関連
  - `chore`: いろいろ
  - `docs`: ドキュメント関連
  - `feat`: 新機能
  - `fix`: 修正
  - `perf`: パフォーマンス改善
  - `refactor`: リファクタリング
  - `revert`: コミットの取り消し
  - `style`: コードスタイルの修正
  - `test`: テストコミット
- `type-case`: `type` は必ず小文字でなければなりません (NG: `FIX` / OK: `fix`)
- `type-empty`: `type` は必ず含めなければなりません (NG: `test message` / OK: `test: message`)
- `scope-case`: `scope` は必ず小文字でなければなりません (NG: `fix(HOME): message` / OK: `fix:(home): message`)
- `subject-case`: `subject` は必ず次のいずれかの書式でなければなりません `sentence-case`, `start-case`, `pascal-case`, `upper-case`
- `subject-empty`: `subject` は必ず含めなければなりません (NG: `fix:` / OK: `fix: message`)
- `subject-full-stop`: `subject` は `.` 以外で終えてください (NG: `fix: message.` / OK: `fix: message`)

#### Branch rule

- 基本的にはフォークして開発してください
- 必要がある場合、ブランチは機能追加・修正などに応じて分けて作成してください
- ブランチ名は機能追加・修正の内容を示す言葉で構成することをお勧めします（例: `add-test-command`, `fix-test-command-api-url`）
- master ブランチへの直接コミットはできません
- 全てのコード追加はプルリクエストを必要とします
- Tomachi に限りセルフマージを可能とします
- レビューはほぼすべてを Tomachi が行います

### Publish

master ブランチ = 本番環境動作ソースコード です。

- コミットされると、GitHub Actions によってビルドが実施されます。失敗した場合、jMS Gamers Club `#github-notice` に通知が飛びます。
- コミットされると、ZakuroHat でビルドされ DiscordBot jaotan にて本番環境下で動作します。
- バージョン表記は本番環境でのビルド処理によって、`yyyy.mm.dd_hh.mm_最終コミットsha8桁` に変更されます。

## Other

- 不明な点は jMS Gamers Club の `#development` チャンネルで質問してください。
