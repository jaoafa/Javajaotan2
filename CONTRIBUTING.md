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

プロジェクトのタスク管理ボードとして [GitHub の Issue 機能](https://github.com/jaoafa/Javajaotan2/issues) と [GitHub の Project 機能](https://github.com/jaoafa/Javajaotan2/projects/1) を使用しています。  
メインで使うのは Issue で、Project はほぼサブとしてしか使っていません（割り当てられていない Issue も多いです）。

## How to develop

このプロジェクトでは、以下のプロセスに則り開発を進めます。

- 全ての開発作業は各ユーザーのフォークリポジトリで行います。
- 実施した開発内容は動作テストを行い、期待通りにエラーなく動作することを確認してください。
- 1 つのコマンド・1 つの機能を制作し終え、本番環境に反映しても構わない場合はオリジナルリポジトリである jaoafa/MyMaid4 にプルリクエストを送信してください。
- 送信されたプルリクエストはコードオーナーによってコードをチェックされ、問題がなければマージされます。問題がある場合はプルリクエストのレビュー・コメントにて、その旨を記載しますので応答・修正して下さい。

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

## Precautions for development

開発にあたり、次の注意事項をご確認ください。

### General

- コマンド・機能の開発を始める前に、次の作業を実施してください。
  - **`upstream/master` からリベースプルを行い、最新の状態に更新する**
  - **[Projects](https://github.com/jaoafa/Javajaotan2/projects/1) で、該当する看板があれば `🚧 作業中` に移動する**
  - 必要に応じて、ブランチを分ける  
    （ブランチを分けることで同時に複数のコマンド・機能を開発できます。この際 `upstream/master` を元としてブランチを作成してください）
  - 必要に応じて、該当する Issue の `Assignees` に自分を追加する
- 将来的に追加・修正などを行わなければならない項目がある場合は、 `// TODO <Message>` で TODO を登録してください。
- ローカル変数はなにかしらの理由がある場合を除き小文字で始めてください。
- `config.json` で設定される設定情報は `JavajaotanConfig` にあり、 `Main.getJavajaotanConfig()` から取得できます。
- 複数のクラスにわたって使用される変数は `JavajaotanData` に変数を作成し、 Getter と Setter を使用して管理してください。
- 複数のクラスにわたって多く使用される関数は `JavajaotanLibrary` に関数を作成し、Javadoc を書いたうえで `extends JavajaotanLibrary` して利用してください。
- データベースは jaoMain と ZakuroHat の二つがありますが、原則 jaoMain が使用されます。それぞれ `JavajaotanData.getMainMySQLDBManager()` `JavajaotanData.getZKRHatMySQLDBManager()` で取得できます。
- コマンドによって Bot が送信しなければならないメッセージは原則 `Message.reply` によるリプライ形式でのメッセージ送信をお願いします。

### Command

- 使用しているコマンドフレームワークは [Chew/JDA-Chewtils](https://github.com/Chew/JDA-Chewtils) です。
- 全てのコマンドは [`src/main/java/com/jaoafa/javajaotan2/command/Cmd_<CommandName>.java`](src/main/java/com/jaoafa/javajaotan2/command) に配置され、これらが自動で読み込まれます。
- 同時に、クラス名は `Cmd_<CommandName>` でなければなりません。`<CommandName>` は大文字・小文字を問いません。
- また、ここに配置されるコマンドクラスは `com.jagrosh.jdautilities.command.Command` を継承する必要があります。（`extends Command`）
- コマンドの定義はコンストラクタにて行います。
- Command クラスを継承したことにより、`void execute(CommandEvent)` メソッドを実行しなければなりません。
  - 簡易なコマンド実装であればこの関数内に処理を記述します。複雑な実装の場合は `CommandAction` クラスの利用を検討してください。
- コマンドの引数は独自で処理する必要があります。ダブルクォーテーションによるクォート引数や以降の引数を全部取得するメソッドを備えた `CommandArgument` クラスで処理することをおすすめします。後述する解説を参照ください。

### Event

- 全てのイベント駆動の機能は [`src/main/java/com/jaoafa/javajaotan2/event/Event_<FuncName>.java`](src/main/java/com/jaoafa/javajaotan2/event) に配置され、これらが自動で読み込まれます。
- 同時に、クラス名は `Event_<FuncName>` でなければなりません。
- また、ここに配置されるコマンドクラスは `net.dv8tion.jda.api.hooks.ListenerAdapter` インターフェースを継承する必要があります。（`extends ListenerAdapter`）
- イベントを受け取るためには、各イベントクラスを引数に取り、また関数名を `on<各イベントクラス名>` にしなければなりません。また、 `@Override` でオーバーライドしなければなりません。 (例: `@Override public void onGuildMessageReceived(GuildMessageReceivedEvent event)`)
- `<FuncName>` は自由で構いません

## Git

### General

- `upstream` とは `jaoafa/MyMaid4` のことです（オリジナルリポジトリとも呼びます）。
- フォークしている場合、`origin` とはあなたのアカウント以下にある MyMaid4 のフォークリポジトリのことです（フォークリポジトリとも呼びます）。
- ローカルリポジトリとは、あなたのコンピュータ上にある MyMaid4 のリポジトリのことです。

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

- 必ずフォークして開発してください
- ブランチは機能追加・修正などに応じて分けて作成してください。一つのプルリクエストに複数の変更事項をまとめるとレビュアーの負担が増えます。
- ブランチ名は機能追加・修正の内容を示す言葉で構成することをお勧めします。（例: `feat/test-command`, `fix/test-command-api-url`）
  - 開発者自身が自分で「何のために作ったブランチか」を把握できればどんな名前でも構いません。
- upstream の master ブランチへの直接コミットはできません。
- 全てのコード編集はプルリクエストを必要とします。
- [Tomachi](https://github.com/book000) に限りセルフマージを可能とします（この際、jaotan がレビューします）。
- マージにあたっては [Tomachi](https://github.com/book000) のレビュー承認が必要です。
- レビュアーに指定されていなくても気になることがあればレビューして構いません。

### Publish

master ブランチ = 本番環境動作ソースコード です。

- コミットされると、GitHub Actions によってビルドが実施されます。ビルド成果物は [Release](https://github.com/jaoafa/Javajaotan2/releases/) でリリースされます。
- リリースされると、ZakuroHat で成果物がダウンロードされ、 DiscordBot jaotan にて本番環境下で動作します。
- バージョンは [Semantic Versioning 2.0.0](https://semver.org) に基づきコミットメッセージを元に自動で付与されます。

## Code Quality

コードの品質や安全性、依存パッケージを管理し一定以上に保つため、以下のサービスを利用しています。

- [CodeQL](https://codeql.github.com/): GitHub によるの静的アプリケーションセキュリティテスト (SAST) です。Push / Pull Request 時にチェックされます。
- [GitHub Security Advisories](https://docs.github.com/ja/code-security/repository-security-advisories/about-github-security-advisories-for-repositories):
  GitHub によるリポジトリセキュリティ脆弱性検知ツールです。
- [Dependabot](https://docs.github.com/ja/code-security/supply-chain-security/managing-vulnerabilities-in-your-projects-dependencies):
  GitHub による依存パッケージ脆弱性管理サービスです。
- [Renovate](https://www.whitesourcesoftware.com/free-developer-tools/renovate/): WhiteSource
  による依存パッケージバージョン管理ツールです。自動でアップデートを収集し、Pull Request を作成します。

## Other

- 不明な点は jMS Gamers Club の `#development` チャンネルで質問してください。
