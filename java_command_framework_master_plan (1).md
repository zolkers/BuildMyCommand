# Plan complet — Framework de commandes universel en Java

## 0. Vision du projet

Le but est de créer un framework de commandes Java capable de fonctionner dans presque n'importe quel contexte :

- terminal
- console serveur
- chat de jeu
- bots Discord
- outils développeur
- REPL
- panneaux d'administration
- plugins
- systèmes embarqués
- scripts internes
- interfaces custom

Le framework doit permettre de définir une commande une seule fois, puis de l'exécuter depuis plusieurs plateformes grâce à des adapters.

L'objectif principal :

> Offrir une API ultra simple pour les cas basiques, mais assez puissante pour gérer des cas avancés sans devoir contourner le framework.

Le framework doit être :

- propre
- extensible
- testable
- platform-agnostic
- autocomplete-ready
- compatible annotations
- compatible builder
- compatible manual API publique
- adapté aux flags
- adapté aux subcommands
- adapté aux arguments typés
- adapté aux commandes dynamiques
- adapté aux plugins

---

# 1. Principe fondamental

Le framework ne doit pas être centré sur les annotations.

Il doit être centré sur un modèle interne universel :

```text
CommandGraph / CommandTree
```

Les annotations, le builder et la manual API doivent tous compiler vers ce même modèle interne.

```text
Annotation API
       ↓
Builder API
       ↓
Manual API
       ↓
CommandGraph
       ↓
Dispatcher
```

Ou plus précisément :

```text
@Command methods ───────┐
Builder DSL ────────────┼──> CommandNode tree ──> Dispatcher
Manual CommandNode API ─┘
```

Cela évite de bloquer le framework dans une seule façon de déclarer les commandes.

---

# 2. APIs publiques à proposer

Le framework doit exposer 3 APIs publiques principales.

## 2.1 Annotation API

Pour les utilisateurs qui veulent écrire peu de code.

Exemple :

```java
public final class ModerationCommands {

    @Command("ban")
    @Description("Ban a user")
    @Permission("mod.ban")
    public CommandResult ban(
        CommandContext ctx,
        @Arg("target") User target,
        @Greedy @OptionalArg @Arg("reason") String reason,
        @Flag("silent") boolean silent
    ) {
        return Results.success("Banned " + target.name());
    }
}
```

Avantages :

- très rapide
- très lisible
- excellent pour 80% des commandes
- parfait pour des plugins classiques
- proche des frameworks modernes

Inconvénients :

- moins dynamique
- dépend de la réflexion
- plus difficile pour des commandes générées depuis config ou runtime

---

## 2.2 Builder API

Pour les commandes flexibles et dynamiques.

Exemple :

```java
registry.command("ban", command -> command
    .description("Ban a user")
    .permission("mod.ban")
    .argument("target", User.class)
    .greedyOptional("reason", String.class)
    .flag("silent", Boolean.class)
    .executes(ctx -> {
        User target = ctx.arg("target");
        String reason = ctx.argOr("reason", "No reason");
        boolean silent = ctx.flag("silent");

        return Results.success("Banned " + target.name());
    })
);
```

Avantages :

- dynamique
- propre
- parfait pour les plugins
- parfait pour les commandes générées
- parfait pour les tests
- aucun besoin de réflexion

---

## 2.3 Manual API publique

Important : la Manual API ne doit pas être une API interne sale exposée aux utilisateurs.

Elle doit être officielle, documentée et stable.

Elle sert aux utilisateurs avancés, aux adapters, aux extensions, aux moteurs de plugins et aux frameworks qui veulent générer des commandes directement.

Exemple :

```java
CommandNode ban = Commands.literal("ban")
    .description("Ban a user")
    .permission("mod.ban")
    .aliases("block", "punish")
    .argument(Arguments.required("target", User.class))
    .argument(Arguments.greedyOptional("reason", String.class))
    .flag(Flags.bool("silent")
        .alias("s")
        .description("Do not broadcast the ban")
    )
    .handler(ctx -> {
        User target = ctx.arg("target");
        String reason = ctx.argOr("reason", "No reason");

        return Results.success("Banned " + target.name());
    })
    .build();

registry.register(ban);
```

Pourquoi cette API est nécessaire :

- intégration avec d'autres frameworks
- commandes générées par fichiers YAML/JSON
- plugins externes
- systèmes complexes
- optimisation sans réflexion
- tests avancés
- contrôle total du command tree

---

# 3. Architecture globale

Architecture recommandée :

```text
command-core
command-annotations
command-builder
command-terminal-adapter
command-gamechat-adapter
command-discord-adapter
command-testkit
command-examples
```

## 3.1 command-core

Le coeur du framework.

Contient :

- `CommandFramework`
- `CommandRegistry`
- `CommandDispatcher`
- `CommandNode`
- `CommandGraph`
- `CommandContext`
- `CommandSource`
- `CommandInput`
- `CommandOutput`
- `CommandResult`
- `ArgumentParser<T>`
- `ArgumentRegistry`
- `SuggestionEngine`
- `SuggestionProvider`
- `CommandRouter`
- `CommandTokenizer`
- `FlagParser`
- `HelpGenerator`
- `Middleware`
- `PermissionChecker`
- `CooldownManager`
- `ErrorHandler`

Le core ne doit dépendre d'aucune plateforme externe.

Il ne doit pas connaître Minecraft, Discord, JLine ou autre.

---

## 3.2 command-annotations

Module optionnel qui ajoute les annotations.

Contient :

- `@Command`
- `@CommandGroup`
- `@Subcommand`
- `@Arg`
- `@Flag`
- `@Permission`
- `@Description`
- `@Alias`
- `@OptionalArg`
- `@Greedy`
- `@Cooldown`
- `AnnotationCommandScanner`
- `MethodCommandBinder`

Il transforme les classes annotées en `CommandNode`.

---

## 3.3 command-builder

Peut être dans le core ou dans un module séparé.

Contient le DSL fluide :

```java
registry.command("user", user -> user
    .subcommand("rank", rank -> rank
        .subcommand("set", set -> set
            .argument("target", User.class)
            .argument("rank", Rank.class)
            .executes(...)
        )
    )
);
```

---

## 3.4 command-terminal-adapter

Adapter terminal.

Objectif :

```java
TerminalAdapter.attach(framework)
    .prompt("> ")
    .prefix("")
    .autocomplete(true)
    .start();
```

Optionnellement basé sur JLine.

---

## 3.5 command-gamechat-adapter

Adapter générique pour chat de jeu.

Objectif :

```java
GameChatAdapter.attach(framework)
    .prefix("/")
    .sourceMapper(player -> Sources.of(player.getName(), player))
    .messageSender((player, message) -> player.sendMessage(message.text()))
    .autocomplete(true)
    .register();
```

Ne doit pas dépendre d'un jeu spécifique dans le core.

On peut faire ensuite :

- `command-minecraft-paper`
- `command-velocity`
- `command-bukkit`
- `command-custom-game`

---

## 3.6 command-discord-adapter

Adapter Discord.

Peut supporter :

- text commands
- slash commands
- autocomplete Discord
- permissions Discord
- ephemeral replies

---

## 3.7 command-testkit

Très important.

Permet de tester sans plateforme.

```java
CommandTestKit kit = CommandTestKit.create(framework);

kit.dispatch("ban Steve griefing")
    .assertSuccess()
    .assertMessageContains("Banned Steve");

kit.suggest("ban St")
    .assertContains("Steve");
```

---

# 4. Objets génériques principaux

## 4.1 CommandFramework

Objet principal.

```java
CommandFramework framework = CommandFramework.builder()
    .registry(registry)
    .arguments(arguments -> arguments
        .register(User.class, new UserParser(userService))
        .register(Rank.class, Arguments.enumParser(Rank.class))
    )
    .middleware(middleware -> middleware
        .use(Permissions.middleware())
        .use(Logging.middleware())
    )
    .errors(errors -> errors
        .handler(new DefaultErrorHandler())
    )
    .suggestions(suggestions -> suggestions
        .caseInsensitive(true)
        .maxResults(20)
    )
    .build();
```

Responsabilités :

- contient registry
- contient dispatcher
- contient parsers
- contient middleware
- contient suggestion engine
- contient help generator
- expose dispatch/suggest/help

---

## 4.2 CommandSource

Représente l'origine d'une commande.

Peut être :

- joueur
- terminal
- console
- utilisateur Discord
- système
- test fake source

Interface proposée :

```java
public interface CommandSource {
    String id();

    String name();

    void reply(CommandMessage message);

    default boolean hasPermission(String permission) {
        return true;
    }

    default Locale locale() {
        return Locale.getDefault();
    }

    default <T> Optional<T> unwrap(Class<T> type) {
        return Optional.empty();
    }

    default Map<String, Object> metadata() {
        return Map.of();
    }
}
```

Le `unwrap` est crucial.

Exemple Minecraft :

```java
ctx.source().unwrap(Player.class)
```

Exemple terminal :

```java
ctx.source().unwrap(TerminalSession.class)
```

Cela permet de garder le core générique.

---

## 4.3 CommandInput

Représente l'entrée brute.

```java
public record CommandInput(
    CommandSource source,
    String raw,
    int cursor,
    String prefix,
    CommandPlatform platform
) {}
```

Exemples :

```text
raw = "/ban Steve griefing"
prefix = "/"
cursor = 19
```

Pour un dispatch normal, le cursor peut être `raw.length()`.

Pour l'autocomplete, le cursor est la position actuelle du curseur.

---

## 4.4 CommandPlatform

Représente la plateforme.

```java
public interface CommandPlatform {
    String id();

    String displayName();

    default boolean supportsRichMessages() {
        return false;
    }

    default boolean supportsAutocomplete() {
        return false;
    }

    default boolean supportsPermissions() {
        return true;
    }
}
```

Exemples :

```java
Platforms.TERMINAL
Platforms.GAME_CHAT
Platforms.DISCORD
Platforms.TEST
```

---

## 4.5 CommandMessage

Objet de réponse générique.

```java
public record CommandMessage(
    String text,
    MessageLevel level,
    Map<String, Object> metadata
) {}
```

Niveaux :

```java
INFO
SUCCESS
WARNING
ERROR
DEBUG
```

Pourquoi ne pas juste envoyer une String ?

Parce que certains adapters peuvent rendre les messages différemment.

Terminal :

```text
[ERROR] Missing permission.
```

Game chat :

```text
red text
```

Discord :

```text
embed
```

Le core reste neutre.

---

## 4.6 CommandContext

Objet donné au handler.

```java
public final class CommandContext {
    public CommandSource source();

    public CommandInput input();

    public CommandPath path();

    public <T> T arg(String name);

    public <T> T arg(String name, Class<T> type);

    public <T> Optional<T> optionalArg(String name, Class<T> type);

    public <T> T argOr(String name, T fallback);

    public boolean flag(String name);

    public <T> Optional<T> option(String name, Class<T> type);

    public void reply(String message);

    public void reply(CommandMessage message);

    public <T> Optional<T> platformObject(Class<T> type);
}
```

---

## 4.7 CommandResult

Retour standardisé.

```java
public sealed interface CommandResult permits
    CommandResult.Success,
    CommandResult.Failure,
    CommandResult.Silent {

    record Success(CommandMessage message) implements CommandResult {}

    record Failure(CommandMessage message) implements CommandResult {}

    record Silent() implements CommandResult {}
}
```

Helpers :

```java
Results.success("Done");
Results.error("Invalid target.");
Results.silent();
```

---

# 5. CommandGraph et CommandNode

Le coeur technique du framework.

## 5.1 CommandGraph

```java
public final class CommandGraph {
    private final CommandNode root;
}
```

Le graph contient toutes les commandes.

---

## 5.2 CommandNode

```java
public final class CommandNode {
    private final String name;
    private final Set<String> aliases;
    private final String description;
    private final String permission;
    private final List<CommandNode> children;
    private final List<ArgumentSpec<?>> arguments;
    private final List<FlagSpec<?>> flags;
    private final List<CommandMiddleware> middleware;
    private final CommandExecutor executor;
    private final SuggestionProvider suggestionProvider;
    private final Map<String, Object> metadata;
}
```

Un node peut être :

- literal
- argument
- executable
- group
- hidden
- alias

Mais pour garder une API simple, on peut commencer avec :

```java
Commands.literal("ban")
```

---

# 6. Command routing

Le router sert à trouver quelle commande correspond à l'input.

Input :

```text
user rank set Victor admin --silent
```

Le router doit reconnaître :

```text
user -> rank -> set
```

Puis laisser :

```text
Victor admin --silent
```

au parser d'arguments et flags.

Étapes :

```text
1. tokenizer
2. prefix stripping
3. literal matching
4. alias matching
5. subcommand matching
6. argument parsing
7. flag parsing
8. validation
9. middleware
10. handler execution
```

Pseudo-code :

```java
public CompletionStage<CommandResult> dispatch(CommandInput input) {
    TokenizedInput tokenized = tokenizer.tokenize(input.raw(), input.cursor());

    RouteMatch route = router.match(graph.root(), tokenized);

    ParsedFlags flags = flagParser.parse(route.node(), route.remainingTokens());

    ParsedArguments arguments = argumentParser.parse(route.node(), route.remainingTokens(), flags);

    CommandContext context = contextFactory.create(input, route, arguments, flags);

    return middlewareChain.execute(context, () -> route.node().executor().execute(context));
}
```

---

# 7. Tokenizer

Le tokenizer doit être solide.

Il doit gérer :

- espaces
- guillemets doubles
- guillemets simples
- escaping
- tokens vides
- positions start/end
- curseur pour autocomplete

Exemples :

```text
say "hello world"
```

Tokens :

```text
say
hello world
```

```text
msg Victor "yo \"bro\""
```

Tokens :

```text
msg
Victor
yo "bro"
```

Modèle :

```java
public record Token(
    String value,
    int start,
    int end,
    boolean quoted
) {}
```

Pour l'autocomplete, les positions sont essentielles.

---

# 8. Arguments typés

Le framework doit convertir automatiquement les strings en objets Java.

Interface :

```java
public interface ArgumentParser<T> {
    T parse(ParseContext context, String input) throws ArgumentParseException;

    default List<Suggestion> suggest(SuggestionContext context) {
        return List.of();
    }

    Class<T> type();
}
```

Parsers built-in :

- `String`
- `Integer`
- `Long`
- `Double`
- `Float`
- `Boolean`
- `Enum`
- `UUID`
- `Duration`
- `LocalDate`
- `LocalDateTime`
- `Path`
- `URI`
- `URL`

Parsers custom :

```java
argumentRegistry.register(User.class, new UserParser(userService));
argumentRegistry.register(World.class, new WorldParser(worldService));
argumentRegistry.register(Item.class, new ItemParser(itemRegistry));
```

---

# 9. ArgumentSpec

```java
public record ArgumentSpec<T>(
    String name,
    Class<T> type,
    boolean required,
    boolean greedy,
    String description,
    Optional<T> defaultValue,
    SuggestionProvider suggestionProvider
) {}
```

Types d'arguments :

```java
Arguments.required("target", User.class)
Arguments.optional("reason", String.class)
Arguments.greedy("message", String.class)
Arguments.greedyOptional("reason", String.class)
```

Règle :

- les required doivent être avant les optional
- un greedy argument doit être dernier
- un seul greedy argument par commande

---

# 10. Flags et options

Le framework doit supporter :

## Boolean flags

```text
reload --force
ban Steve --silent
```

## Options avec valeur

```text
give Steve diamond --amount 64
teleport Victor --world nether
```

## Short aliases

```text
give Steve diamond -a 64
reload -f
```

## Grouped short flags

Optionnel mais cool :

```text
tar -xvf archive.tar
```

Peut être ajouté plus tard.

---

## 10.1 FlagSpec

```java
public record FlagSpec<T>(
    String name,
    Set<String> aliases,
    Class<T> type,
    boolean required,
    Optional<T> defaultValue,
    String description,
    SuggestionProvider suggestions
) {}
```

Examples :

```java
Flags.bool("silent").alias("s")
Flags.option("amount", Integer.class).alias("a").defaultValue(1)
Flags.option("world", World.class)
```

---

# 11. Subcommands

Les subcommands doivent être natives.

Exemple :

```java
registry.command("user", user -> user
    .subcommand("rank", rank -> rank
        .subcommand("set", set -> set
            .argument("target", User.class)
            .argument("rank", Rank.class)
            .executes(...)
        )
        .subcommand("remove", remove -> remove
            .argument("target", User.class)
            .executes(...)
        )
    )
);
```

Inputs :

```text
user rank set Victor admin
user rank remove Victor
```

Le framework doit gérer :

- aliases
- nested subcommands
- permissions héritées
- middleware hérité
- help par niveau
- suggestions par niveau

---

# 12. Autocomplete natif

C'est une feature centrale.

L'utilisateur ne doit pas devoir coder l'autocomplete à la main pour chaque commande.

Le framework doit générer automatiquement les suggestions depuis :

- command names
- aliases
- subcommands
- flags
- options
- enum values
- boolean values
- parsers custom
- suggestion providers custom

API :

```java
List<Suggestion> suggestions = framework.suggest(source, "/user r", 7);
```

---

## 12.1 Suggestion

```java
public record Suggestion(
    String value,
    String tooltip,
    int replacementStart,
    int replacementEnd,
    SuggestionType type,
    int priority
) {}
```

SuggestionType :

```java
COMMAND
SUBCOMMAND
ARGUMENT
FLAG
OPTION_VALUE
ALIAS
```

---

## 12.2 SuggestionContext

```java
public record SuggestionContext(
    CommandSource source,
    CommandInput input,
    List<Token> tokens,
    int cursor,
    CommandNode currentNode,
    Optional<ArgumentSpec<?>> currentArgument,
    Optional<FlagSpec<?>> currentFlag,
    String currentToken
) {}
```

---

## 12.3 Autocomplete flow

```text
1. tokenize with cursor
2. partial route matching
3. detect what cursor edits:
   - command
   - subcommand
   - argument
   - flag name
   - flag value
4. call relevant suggestion provider
5. filter by prefix
6. sort by priority
7. return replacement ranges
```

Pseudo-code :

```java
public List<Suggestion> suggest(CommandSource source, String raw, int cursor) {
    TokenizedInput input = tokenizer.tokenize(raw, cursor);

    PartialRoute route = router.matchPartial(graph.root(), input);

    if (route.isEditingFlagName()) {
        return suggestionEngine.flags(route.node(), input.currentToken());
    }

    if (route.isEditingFlagValue()) {
        return suggestionEngine.flagValues(route.currentFlag(), input.currentToken());
    }

    if (route.isEditingArgument()) {
        return suggestionEngine.argument(route.currentArgument(), input.currentToken());
    }

    return suggestionEngine.children(route.node(), input.currentToken());
}
```

---

## 12.4 Custom autocomplete via parser

Exemple :

```java
public final class UserParser implements ArgumentParser<User> {
    public User parse(ParseContext ctx, String input) {
        return userService.find(input)
            .orElseThrow(() -> new ArgumentParseException("Unknown user: " + input));
    }

    public List<Suggestion> suggest(SuggestionContext ctx) {
        return userService.onlineUsers().stream()
            .map(User::name)
            .filter(name -> name.startsWith(ctx.currentToken()))
            .map(name -> Suggestions.argument(name, "Online user", ctx))
            .toList();
    }

    public Class<User> type() {
        return User.class;
    }
}
```

Avec ça, toute commande qui utilise `User` obtient l'autocomplete automatiquement.

---

# 13. Help generation automatique

Comme tout est structuré, le framework peut générer l'aide.

Input :

```text
help server reload
```

Output :

```text
Usage:
  server reload [--force]

Description:
  Reloads the server.

Permission:
  server.reload

Flags:
  --force, -f    Force reload

Examples:
  server reload
  server reload --force
```

API :

```java
framework.help().usage("server reload");
framework.help().list(source);
framework.help().describe("ban");
```

Les commandes peuvent avoir :

- description
- usage override
- examples
- flag descriptions
- argument descriptions
- visibility
- permission-aware help

Important :

> L'aide doit respecter les permissions du source.

Un utilisateur sans permission ne doit pas forcément voir les commandes admin.

---

# 14. Middleware

Le middleware doit permettre d'ajouter de la logique autour des commandes.

Interface :

```java
public interface CommandMiddleware {
    CompletionStage<Void> before(CommandContext context);

    default CompletionStage<Void> after(CommandContext context, CommandResult result) {
        return CompletableFuture.completedFuture(null);
    }
}
```

Exemples :

- permission middleware
- cooldown middleware
- logging middleware
- metrics middleware
- async database middleware
- game-only middleware
- console-only middleware
- audit middleware

Global :

```java
framework.middleware().use(new LoggingMiddleware());
```

Local :

```java
registry.command("admin", admin -> admin
    .middleware(new AdminOnlyMiddleware())
    .subcommand("reload", ...)
);
```

---

# 15. Permissions

Permissions simples :

```java
.permission("server.reload")
```

Checker :

```java
public interface PermissionChecker {
    boolean hasPermission(CommandSource source, String permission);
}
```

Par défaut :

```java
source.hasPermission(permission)
```

Support avancé :

- permission héritée depuis parent
- wildcard
- roles
- platform permissions
- permission-aware autocomplete
- permission-aware help

Exemple :

```java
Permissions.wildcard()
Permissions.platform()
Permissions.custom(checker)
```

---

# 16. Errors propres

Le framework doit avoir des erreurs propres.

Types :

```java
UnknownCommandException
MissingArgumentException
InvalidArgumentException
UnknownFlagException
MissingFlagValueException
MissingPermissionException
CommandExecutionException
AmbiguousCommandException
```

Messages exemples :

```text
Unknown command: /serber
Did you mean: /server?

Missing argument: target
Usage: /ban <target> [reason]

Invalid value for amount: abc
Expected an integer.

Unknown flag: --silnt
Did you mean: --silent?
```

ErrorHandler :

```java
public interface CommandErrorHandler {
    CommandResult handle(CommandContext context, Throwable error);
}
```

Adapter-specific rendering :

- terminal prints plain text
- game chat can colorize
- Discord can use embeds

---

# 17. Async support

Le framework doit être async-ready dès le début.

Executor :

```java
public interface CommandExecutor {
    CompletionStage<CommandResult> execute(CommandContext context);
}
```

Builder helper :

```java
.executes(ctx -> Results.success("Done"))
.executesAsync(ctx -> database.ban(...))
```

Pourquoi :

- database
- HTTP
- file IO
- distributed servers
- Discord APIs
- game server tasks

---

# 18. Adapters faciles à configurer

Un adapter doit être très simple à brancher.

## 18.1 Interface générique

```java
public interface CommandAdapter {
    void start();

    void stop();

    CommandPlatform platform();
}
```

## 18.2 Adapter config

```java
public interface AdapterConfig {
    String prefix();

    boolean autocomplete();

    boolean stripPrefix();

    boolean caseInsensitive();
}
```

---

## 18.3 Terminal adapter

API idéale :

```java
TerminalAdapter adapter = TerminalAdapter.attach(framework)
    .prefix("")
    .prompt("> ")
    .autocomplete(true)
    .history(true)
    .source(Sources.console())
    .start();
```

Il doit gérer :

- lecture input
- dispatch
- affichage output
- autocomplete via JLine
- history
- Ctrl+C clean stop

---

## 18.4 Game chat adapter générique

API :

```java
GameChatAdapter.<Player>attach(framework)
    .prefix("/")
    .sourceMapper(player -> Sources.generic(
        player.getUniqueId().toString(),
        player.getName(),
        player
    ))
    .permissionChecker((player, permission) -> player.hasPermission(permission))
    .messageSender((player, message) -> player.sendMessage(message.text()))
    .autocomplete(true)
    .register();
```

L'idée :

- l'utilisateur donne comment transformer son Player en CommandSource
- l'utilisateur donne comment envoyer un message
- l'utilisateur donne comment vérifier les permissions
- l'adapter fait le reste

---

## 18.5 Discord adapter

API :

```java
DiscordAdapter.attach(framework, jda)
    .prefix("!")
    .slashCommands(true)
    .textCommands(true)
    .autocomplete(true)
    .ephemeralErrors(true)
    .register();
```

---

# 19. Sources utilitaires

Pour faciliter la config :

```java
Sources.console()
Sources.system()
Sources.generic(id, name)
Sources.generic(id, name, nativeObject)
Sources.wrapping(nativeObject, idExtractor, nameExtractor)
```

Exemple :

```java
CommandSource source = Sources.generic(
    player.getUniqueId().toString(),
    player.getName(),
    player
);
```

---

# 20. Messages utilitaires

```java
Messages.info("Hello")
Messages.success("Done")
Messages.warning("Careful")
Messages.error("No permission")
```

Adapters peuvent mapper :

- `ERROR` vers rouge
- `SUCCESS` vers vert
- `WARNING` vers jaune

---

# 21. Metadata générique

Tout objet important peut avoir une metadata map.

Utile pour :

- platform-specific data
- command categories
- analytics
- rich rendering
- Discord embed options
- game chat colors
- hidden/debug commands

Exemple :

```java
Commands.literal("reload")
    .metadata("category", "admin")
    .metadata("color", "red")
```

---

# 22. Annotation API détaillée

Annotations recommandées :

```java
@Command("ban")
@Description("Ban a user")
@Permission("mod.ban")
@Alias({"block", "punish"})
@Example("ban Steve griefing")
@Example("ban Steve --silent")
public CommandResult ban(...)
```

Paramètres :

```java
@Arg("target")
@Description("The user to ban")
User target
```

```java
@OptionalArg
@Greedy
@Arg("reason")
String reason
```

```java
@Flag(value = "silent", aliases = {"s"})
boolean silent
```

Groupes :

```java
@CommandGroup("user")
@Permission("user")
public final class UserCommands {

    @Subcommand("rank set")
    @Permission("user.rank.set")
    public CommandResult setRank(...) {}

    @Subcommand("rank remove")
    @Permission("user.rank.remove")
    public CommandResult removeRank(...) {}
}
```

---

# 23. Inférence intelligente

Pour rendre l'API clean, autoriser l'inférence.

Exemple minimal :

```java
@Command("ban")
public CommandResult ban(User target, String reason) {
    ...
}
```

Le scanner peut inférer :

```text
arg0 -> target
arg1 -> reason
```

Mais attention : en Java, les noms des paramètres ne sont disponibles que si compilé avec `-parameters`.

Donc stratégie :

1. si `@Arg` existe, l'utiliser
2. sinon si parameter names disponibles, les utiliser
3. sinon fallback vers `arg0`, `arg1`
4. recommander `-parameters`

---

# 24. Manual API détaillée

Manual API officielle :

```java
CommandNode command = Commands.literal("user")
    .description("User commands")
    .child(Commands.literal("rank")
        .child(Commands.literal("set")
            .argument(Arguments.required("target", User.class))
            .argument(Arguments.required("rank", Rank.class))
            .flag(Flags.bool("silent").alias("s"))
            .handler(ctx -> {
                User target = ctx.arg("target");
                Rank rank = ctx.arg("rank");
                return Results.success("Rank set.");
            })
        )
    )
    .build();

registry.register(command);
```

Cette API doit être :

- stable
- complète
- documentée
- utilisable par des libraries tierces
- pas dépendante de réflexion
- équivalente au builder

---

# 25. Builder API détaillée

```java
registry.command("user", user -> user
    .description("User commands")
    .subcommand("rank", rank -> rank
        .subcommand("set", set -> set
            .description("Set user rank")
            .permission("user.rank.set")
            .argument("target", User.class)
            .argument("rank", Rank.class)
            .flag("silent", Boolean.class)
            .executes(ctx -> {
                return Results.success("Rank set.");
            })
        )
    )
);
```

Le builder doit être plus agréable que la Manual API, mais moins bas niveau.

---

# 26. Registry

```java
public final class CommandRegistry {
    public void register(CommandNode node);

    public void unregister(String path);

    public Optional<CommandNode> find(String path);

    public List<CommandNode> commands();

    public CommandBuilder command(String name);

    public void registerAnnotated(Object object);
}
```

Fonctions utiles :

```java
registry.unregister("admin reload");
registry.reloadFrom(config);
registry.clear();
```

---

# 27. Command lifecycle

Possibilité d'avoir des hooks :

```java
onRegister
onUnregister
onExecute
onError
onSuggest
```

Utile pour :

- plugins
- reload dynamique
- logs
- metrics
- debugging

---

# 28. Configuration globale

```java
CommandFramework framework = CommandFramework.builder()
    .caseInsensitive(true)
    .allowUnknownFlags(false)
    .allowFlagPositionAnywhere(true)
    .defaultPrefix("/")
    .maxSuggestions(20)
    .locale(Locale.ENGLISH)
    .build();
```

Options :

- case insensitive
- aliases enabled
- unknown flag behavior
- max suggestion count
- permission-aware suggestions
- hidden commands
- default locale
- strict parsing mode
- lenient parsing mode

---

# 29. Parsing strict vs lenient

## Strict mode

```text
ban Steve --unknown
```

Erreur :

```text
Unknown flag: --unknown
```

## Lenient mode

Ignore ou met dans metadata.

Utile pour certains adapters.

---

# 30. Flags position anywhere

Le framework devrait supporter :

```text
ban Steve griefing --silent
ban --silent Steve griefing
```

Mais attention : ça complexifie le parsing.

Recommandation :

- v1 : flags après arguments ou n'importe où si facile
- v2 : full flexible parsing

---

# 31. Command examples

Chaque commande peut avoir des exemples.

```java
.example("ban Steve griefing")
.example("ban Steve --silent")
```

Utilisé dans :

- help
- docs
- autocomplete tooltips
- generated documentation

---

# 32. Documentation generation

Gros bonus.

Vu que les commandes sont structurées, on peut générer :

- Markdown docs
- HTML docs
- JSON schema
- OpenAPI-like command docs

API :

```java
framework.docs().markdown();
framework.docs().json();
```

---

# 33. JSON/YAML command definitions

Grâce à la Manual API, on peut ajouter plus tard des commandes définies par config.

Exemple YAML :

```yaml
commands:
  - name: rules
    description: Show server rules
    response: "Read the rules at example.com"
```

Le loader compile ça vers `CommandNode`.

---

# 34. Internationalisation

Prévoir une base simple :

```java
CommandMessage.translatable("error.no_permission")
```

Source locale :

```java
ctx.source().locale()
```

Error messages traduisibles :

```text
error.unknown_command
error.missing_argument
error.invalid_argument
```

---

# 35. Sécurité et validation

Le framework doit permettre :

- validation d'arguments
- permissions
- cooldowns
- rate limits
- source type restrictions
- audit logs

Exemple :

```java
.argument(Arguments.required("amount", Integer.class)
    .validate(value -> value > 0, "Amount must be positive")
)
```

Restrictions :

```java
.requires(SourcePredicates.consoleOnly())
.requires(SourcePredicates.playerOnly())
```

---

# 36. Cooldowns

```java
.cooldown(Duration.ofSeconds(5))
```

Middleware :

```java
CooldownMiddleware
```

Support :

- per source
- per command
- global
- bypass permission

---

# 37. Command aliases

Aliases pour commandes :

```java
Commands.literal("teleport")
    .aliases("tp")
```

Aliases pour flags :

```java
Flags.option("amount", Integer.class)
    .alias("a")
```

Autocomplete peut suggérer soit :

- nom principal
- aliases
- les deux selon config

---

# 38. Hidden commands

```java
.hidden(true)
```

Utile pour :

- debug
- admin
- internal
- experimental

Hidden signifie :

- pas dans help
- pas dans autocomplete
- exécutable si connu, selon config

---

# 39. Testing complet

Le testkit doit être excellent.

Exemples :

```java
CommandTestKit kit = CommandTestKit.create(framework);

kit.dispatch("ban Steve")
    .assertSuccess();

kit.dispatch("ban")
    .assertFails(MissingArgumentException.class);

kit.suggest("user r")
    .assertContains("rank");

kit.help("ban")
    .assertContains("Usage");
```

Fake source :

```java
TestSource source = TestSource.named("Victor")
    .permission("mod.ban");
```

---

# 40. Performance

Réflexion uniquement au registration time.

Pas pendant chaque dispatch.

Les annotations doivent être scannées une fois :

```text
scan annotations
    ↓
create CommandNode
    ↓
runtime uses CommandNode only
```

Optimisations :

- trie pour command names
- maps pour child lookup
- cached lowercase names si case-insensitive
- immutable command tree
- parser registry lookup cache

---

# 41. Thread safety

Recommandation :

- registry mutable pendant setup
- command graph immutable pendant runtime
- reload crée un nouveau graph
- dispatcher lit un snapshot

```java
framework.reload(registry -> {
    registry.register(...);
});
```

Cela évite les problèmes en async.

---

# 42. Versioning API

Comme c'est un framework, garder une API stable.

Packages publics :

```text
api
core
builder
annotation
adapter
testkit
```

Éviter d'exposer des classes internes.

Utiliser :

```text
internal
```

pour les détails non stables.

---

# 43. Roadmap de développement

## Phase 1 — Core minimal

Objectif :

```text
hello
ban Steve
```

À faire :

- CommandSource
- CommandInput
- CommandContext
- CommandResult
- CommandNode
- CommandRegistry
- CommandDispatcher
- simple tokenizer
- string argument parser

---

## Phase 2 — Arguments typés

Ajouter :

- ArgumentParser<T>
- ArgumentRegistry
- built-in parsers
- parse errors propres

Support :

```text
give Steve diamond 64
```

---

## Phase 3 — Subcommands

Ajouter :

- command tree
- nested routing
- aliases
- path matching

Support :

```text
user rank set Victor admin
```

---

## Phase 4 — Flags

Ajouter :

- boolean flags
- value options
- aliases
- unknown flag errors

Support :

```text
reload --force
give Steve diamond --amount 64
```

---

## Phase 5 — Autocomplete

Ajouter :

- partial routing
- SuggestionEngine
- SuggestionProvider
- enum suggestions
- flag suggestions
- argument parser suggestions

Support :

```java
framework.suggest(source, raw, cursor);
```

---

## Phase 6 — Help generation

Ajouter :

- usage formatter
- command list
- command details
- examples
- permission-aware help

---

## Phase 7 — Builder API

Ajouter DSL propre :

```java
registry.command("ban", c -> c.argument(...).executes(...));
```

---

## Phase 8 — Manual API publique

Stabiliser :

```java
Commands.literal(...)
Arguments.required(...)
Flags.bool(...)
```

---

## Phase 9 — Annotation API

Ajouter :

- annotations
- scanner
- method binder
- parameter binding
- annotation-to-node compiler

---

## Phase 10 — Middleware

Ajouter :

- permission middleware
- cooldown middleware
- logging middleware
- custom middleware chain

---

## Phase 11 — Async

Rendre execution basée sur :

```java
CompletionStage<CommandResult>
```

---

## Phase 12 — Adapters

Ajouter :

- terminal adapter
- gamechat adapter
- Discord adapter optionnel
- testkit

---

# 44. API finale idéale

Création :

```java
CommandFramework framework = CommandFramework.builder()
    .caseInsensitive(true)
    .arguments(args -> args
        .register(User.class, new UserParser(userService))
        .register(Rank.class, Arguments.enumParser(Rank.class))
    )
    .middleware(m -> m
        .use(Permissions.middleware())
        .use(Cooldowns.middleware())
    )
    .build();
```

Annotation :

```java
framework.registry().registerAnnotated(new ModerationCommands());
```

Builder :

```java
framework.registry().command("reload", reload -> reload
    .permission("server.reload")
    .flag("force", Boolean.class)
    .executes(ctx -> Results.success("Reloaded"))
);
```

Manual :

```java
framework.registry().register(
    Commands.literal("ping")
        .description("Ping command")
        .handler(ctx -> Results.success("Pong"))
        .build()
);
```

Terminal :

```java
TerminalAdapter.attach(framework)
    .prompt("> ")
    .autocomplete(true)
    .start();
```

Game chat :

```java
GameChatAdapter.<Player>attach(framework)
    .prefix("/")
    .sourceMapper(player -> Sources.generic(
        player.getUniqueId().toString(),
        player.getName(),
        player
    ))
    .messageSender((player, msg) -> player.sendMessage(msg.text()))
    .autocomplete(true)
    .register();
```

Dispatch direct :

```java
framework.dispatch(Sources.console(), "reload --force");
```

Autocomplete direct :

```java
framework.suggest(source, "/user r", 7);
```

---

# 45. Règles de design à respecter

## Règle 1

Le core ne dépend d'aucune plateforme.

## Règle 2

Annotations, builder et manual API produisent tous un `CommandNode`.

## Règle 3

L'autocomplete utilise le même routing que l'exécution.

## Règle 4

Les parsers typés fournissent aussi les suggestions.

## Règle 5

Les errors sont typées et propres.

## Règle 6

Les adapters sont petits et configurables.

## Règle 7

Les objets génériques ont toujours un moyen de récupérer l'objet natif avec `unwrap`.

## Règle 8

La réflexion ne doit jamais être nécessaire pendant l'exécution.

## Règle 9

La help est générée automatiquement depuis les metadata.

## Règle 10

Le framework doit être testable sans terminal, sans jeu, sans Discord.

---

# 46. Design final résumé

Architecture finale :

```text
User command declarations
    ├── Annotation API
    ├── Builder API
    └── Manual API
            ↓
        CommandNode tree
            ↓
        CommandGraph
            ↓
        Dispatcher
            ↓
  Tokenizer + Router + Parser + FlagParser
            ↓
        Middleware chain
            ↓
        CommandExecutor
            ↓
        CommandResult
            ↓
        Adapter output
```

Autocomplete :

```text
Raw input + cursor
        ↓
Tokenizer with positions
        ↓
Partial router
        ↓
Detect editing target
        ↓
Suggestion provider
        ↓
Suggestions with replacement ranges
```

Adapters :

```text
Platform event
    ↓
Adapter maps native user to CommandSource
    ↓
Adapter creates CommandInput
    ↓
Framework dispatches
    ↓
Adapter renders CommandResult
```

---

# 47. Conclusion

Le meilleur design est :

```text
Command-tree-based core
+ public Manual API
+ fluent Builder API
+ optional Annotation API
+ generic objects
+ configurable adapters
+ automatic autocomplete
+ typed parsers
+ flags/options
+ subcommand router
+ middleware
+ help generation
+ testkit
```

Ce design est clean parce que chaque partie a un rôle clair.

Ce design est puissant parce qu'il ne dépend pas d'une seule manière de déclarer les commandes.

Ce design est extensible parce que les adapters et parsers sont plug-and-play.

Ce design est agréable parce qu'un utilisateur peut commencer avec une simple annotation, puis passer au builder ou à la manual API quand il a besoin de plus de contrôle.

La phrase clé du projet :

> Déclare une commande une fois, exécute-la partout, avec parsing, flags, permissions, help et autocomplete automatiquement.


---

# 48. Case Sensitivity & Match Strategies

Le framework doit permettre un contrôle fin de la sensibilité à la casse.

Éviter un simple :

```java
boolean caseSensitive;
```

Préférer :

```java
public enum CaseMode {
    SENSITIVE,
    INSENSITIVE,
    INHERIT
}
```

## 48.1 Niveau framework

```java
CommandFramework framework = CommandFramework.builder()
    .caseMode(CaseMode.INSENSITIVE)
    .build();
```

Comportement :

```text
/ban
/Ban
/BAN
```

sont considérés identiques.

---

## 48.2 Niveau commande

```java
registry.command("git")
    .caseMode(CaseMode.SENSITIVE);
```

ou

```java
@Command("git")
@CaseSensitive
public final class GitCommands {}
```

Permet d'avoir des commandes spécifiques qui imposent la casse.

---

## 48.3 Niveau argument

```java
.argument("username", User.class)
.caseMode(CaseMode.INSENSITIVE)
```

```java
.argument("permission", String.class)
.caseMode(CaseMode.SENSITIVE)
```

Exemple :

```text
Victor
victor
VICTOR
```

peuvent représenter le même utilisateur alors que :

```text
Admin.Read
admin.read
```

peuvent être différents.

---

## 48.4 Niveau flag

```java
Flags.option("world", String.class)
    .caseMode(CaseMode.INSENSITIVE);
```

ou

```java
Flags.option("Environment", String.class)
    .caseMode(CaseMode.SENSITIVE);
```

---

## 48.5 Héritage

Ordre de résolution :

```text
Argument
    ↓
Commande
    ↓
Framework
```

Avec `INHERIT`, l'élément récupère automatiquement le comportement du niveau supérieur.

---

## 48.6 MatchStrategy

Prévoir dès le départ une abstraction plus puissante :

```java
public interface MatchStrategy {

    boolean matches(String input, String target);

    MatchScore score(String input, String target);
}
```

Implémentations fournies :

```java
CaseSensitiveMatchStrategy
CaseInsensitiveMatchStrategy
FuzzyMatchStrategy
```

---

## 48.7 Fuzzy Matching

Permet :

```text
/telport
```

de suggérer :

```text
teleport
```

ou

```text
/serber
```

de suggérer :

```text
server
```

Le fuzzy matching doit être configurable :

```java
framework.suggestions()
    .matchStrategy(MatchStrategies.fuzzy());
```

---

## 48.8 Autocomplete

L'autocomplete doit utiliser exactement la même MatchStrategy que le dispatcher.

Principe :

```text
Même logique de matching
Même logique de casse
Même logique de scoring
```

Ainsi le comportement reste cohérent partout.

---

## 48.9 Recommandation finale

Ajouter dans l'API publique :

```java
.caseMode(CaseMode.SENSITIVE)
.caseMode(CaseMode.INSENSITIVE)
.caseMode(CaseMode.INHERIT)
```

et

```java
.matchStrategy(MatchStrategies.caseSensitive())
.matchStrategy(MatchStrategies.caseInsensitive())
.matchStrategy(MatchStrategies.fuzzy())
```

sur :

- framework
- commandes
- arguments
- flags
- moteur de suggestions

Cela évite de casser l'API plus tard si le framework évolue vers du fuzzy matching avancé ou des suggestions intelligentes.


---

# 49. Compatibilité universelle et différenciation face à Imperat

Cette section fixe une règle importante : le framework doit être réellement compatible avec n'importe quelle plateforme, mais sans copier les faiblesses de frameworks existants comme Imperat.

Imperat se présente comme un framework de commandes générique et multi-plateforme. Son repository contient notamment des modules pour Bukkit, Bungee, CLI, JDA, Minestom, Paper et Velocity. L'objectif n'est donc pas seulement de dire "nous aussi on supporte plusieurs plateformes", mais de proposer une architecture plus propre, plus prévisible et plus agréable à maintenir.

Le framework doit être différent sur trois points majeurs :

1. modèle de plateforme vraiment générique
2. subcommands explicites et propres
3. aucune hiérarchie magique basée sur des annotations difficiles à lire

---

## 49.1 Compatibilité avec n'importe quelle plateforme

Le core ne doit dépendre d'aucune plateforme.

Le framework ne doit jamais avoir dans le core :

```java
Player
CommandSender
JDA
SlashCommandInteractionEvent
ConsoleCommandSender
MinecraftServer
```

À la place, le core ne connaît que :

```java
CommandSource
CommandInput
CommandOutput
CommandPlatform
CommandMessage
CommandAdapter
```

Chaque plateforme est branchée via un adapter.

```text
Minecraft PlayerCommandEvent
        ↓
MinecraftAdapter
        ↓
CommandInput + CommandSource
        ↓
CommandFramework
```

```text
Terminal line input
        ↓
TerminalAdapter
        ↓
CommandInput + CommandSource
        ↓
CommandFramework
```

```text
Discord SlashCommandInteraction
        ↓
DiscordAdapter
        ↓
CommandInput + CommandSource
        ↓
CommandFramework
```

Règle :

> Une plateforme doit être un plugin autour du framework, jamais une dépendance dans le coeur.

---

## 49.2 Adapter SDK

Le framework doit fournir un vrai SDK d'adapter.

Interface :

```java
public interface CommandAdapter<NATIVE_EVENT, NATIVE_SOURCE> {

    CommandPlatform platform();

    boolean accepts(NATIVE_EVENT event);

    CommandInput mapInput(NATIVE_EVENT event);

    CommandSource mapSource(NATIVE_SOURCE source);

    void render(CommandSource source, CommandResult result);

    default List<Suggestion> suggest(NATIVE_EVENT event) {
        return List.of();
    }
}
```

Mais pour les utilisateurs, l'API doit rester simple :

```java
GameChatAdapter.<Player>attach(framework)
    .platform("my-game")
    .prefix("/")
    .sourceMapper(player -> Sources.generic(
        player.getUniqueId().toString(),
        player.getName(),
        player
    ))
    .permissionChecker((player, permission) -> player.hasPermission(permission))
    .messageSender((player, message) -> player.sendMessage(message.text()))
    .autocomplete(true)
    .register();
```

Pour un terminal :

```java
TerminalAdapter.attach(framework)
    .prompt("> ")
    .prefix("")
    .autocomplete(true)
    .start();
```

Pour Discord :

```java
DiscordAdapter.attach(framework, jda)
    .slashCommands(true)
    .textCommands(true)
    .autocomplete(true)
    .register();
```

---

## 49.3 Platform Contract

Chaque adapter doit déclarer ses capacités.

```java
public interface PlatformCapabilities {

    boolean supportsAutocomplete();

    boolean supportsRichMessages();

    boolean supportsPermissions();

    boolean supportsAsyncReplies();

    boolean supportsEphemeralMessages();

    boolean supportsCommandRegistration();

    boolean supportsNestedSubcommands();

    boolean supportsFlags();
}
```

Pourquoi : certaines plateformes ne supportent pas tout.

Exemples :

- terminal : autocomplete possible, rich messages limitées
- Minecraft chat : autocomplete possible, rich messages selon API
- Discord slash commands : structure très stricte
- custom game chat : dépend du moteur
- remote admin panel : peut supporter JSON complet

Le framework doit adapter le rendu et les fonctionnalités selon les capacités.

---

## 49.4 Ne pas faire comme Imperat sur les subcommands

Dans Imperat, les subcommands avancées peuvent devenir difficiles à lire parce que la structure peut dépendre de :

- classes imbriquées
- `@SubCommand`
- `attachTo = "<argument>"`
- `@InheritedArg`
- méthodes `@Execute`
- arguments hérités depuis des niveaux précédents

Exemple de style à éviter :

```java
@SubCommand(value = "sub1", attachTo = "<otherText2>")
public static class Sub1 {

    @Execute
    public void run(
        @InheritedArg @Named("otherText") String otherText,
        @InheritedArg @Named("otherText2") String otherText2
    ) {}
}
```

Pourquoi c'est problématique :

- la route réelle n'est pas immédiatement visible
- une subcommand peut être attachée à un argument au lieu d'être un enfant clair
- les arguments hérités rendent la signature longue
- la logique de routing devient implicite
- l'autocomplete devient plus difficile à raisonner
- les erreurs deviennent moins prévisibles
- les grosses commandes deviennent vite illisibles

Notre framework doit faire l'inverse.

---

## 49.5 Subcommands propres : route tree explicite

Chaque subcommand doit être un vrai node dans l'arbre.

Exemple recommandé :

```java
Commands.literal("test")
    .argument(Arguments.required("otherText", String.class))
    .argument(Arguments.required("otherText2", String.class))
    .child(Commands.literal("sub1")
        .argument(Arguments.optional("a", String.class))
        .child(Commands.literal("sub2")
            .argument(Arguments.optional("b", String.class))
            .child(Commands.literal("sub3")
                .argument(Arguments.optional("c", String.class))
                .handler(ctx -> {
                    String otherText = ctx.arg("otherText");
                    String otherText2 = ctx.arg("otherText2");
                    String a = ctx.argOr("a", "");
                    String b = ctx.argOr("b", "");
                    String c = ctx.argOr("c", "");

                    return Results.success("done");
                })
            )
        )
    );
```

La route est visible :

```text
test <otherText> <otherText2> sub1 [a] sub2 [b] sub3 [c]
```

Pas besoin de :

```java
@InheritedArg
attachTo = "<argument>"
```

Le contexte connaît déjà les arguments parents.

---

## 49.6 Parent arguments accessibles automatiquement

Si un argument est défini sur un node parent, les enfants doivent pouvoir y accéder automatiquement.

Exemple :

```java
Commands.literal("user")
    .argument(Arguments.required("target", User.class))
    .child(Commands.literal("rank")
        .child(Commands.literal("set")
            .argument(Arguments.required("rank", Rank.class))
            .handler(ctx -> {
                User target = ctx.arg("target");
                Rank rank = ctx.arg("rank");

                return Results.success("Rank updated");
            })
        )
    );
```

Commande :

```text
user Victor rank set admin
```

Le handler de `rank set` peut lire :

```java
ctx.arg("target");
ctx.arg("rank");
```

Donc pas besoin d'annotations d'héritage.

L'héritage est structurel, pas décoratif.

---

## 49.7 RoutePattern lisible

Ajouter une API alternative pour déclarer des routes longues proprement.

```java
registry.route("user <target:User> rank set <rank:Rank>")
    .permission("user.rank.set")
    .handler(ctx -> {
        User target = ctx.arg("target");
        Rank rank = ctx.arg("rank");
        return Results.success("Rank updated");
    });
```

Autres exemples :

```java
registry.route("server reload [--force]")
    .handler(...);

registry.route("ban <target:User> [reason:String...] [--silent]")
    .handler(...);

registry.route("perm group add <permission:String> <group:String>")
    .handler(...);
```

Cette API est très importante pour se différencier.

Elle donne une lisibilité proche d'une syntaxe CLI, tout en compilant vers le même `CommandNode tree`.

---

## 49.8 Interdiction des subcommands attachées à des arguments

Règle de design :

> Une subcommand ne doit jamais être attachée à un argument par une string magique.

Interdit :

```java
@SubCommand(value = "sub1", attachTo = "<name>")
```

Autorisé :

```java
.argument("name", String.class)
.child(Commands.literal("sub1"))
```

Pourquoi :

- moins magique
- plus typé
- plus lisible
- plus simple à documenter
- plus simple à autocomplete
- plus simple à tester

---

## 49.9 Trois façons propres de déclarer la même commande

### Manual API

```java
registry.register(
    Commands.literal("user")
        .argument(Arguments.required("target", User.class))
        .child(Commands.literal("rank")
            .child(Commands.literal("set")
                .argument(Arguments.required("rank", Rank.class))
                .handler(ctx -> Results.success("Rank updated"))
            )
        )
        .build()
);
```

### Builder API

```java
registry.command("user", user -> user
    .argument("target", User.class)
    .subcommand("rank", rank -> rank
        .subcommand("set", set -> set
            .argument("rank", Rank.class)
            .executes(ctx -> Results.success("Rank updated"))
        )
    )
);
```

### RoutePattern API

```java
registry.route("user <target:User> rank set <rank:Rank>")
    .handler(ctx -> Results.success("Rank updated"));
```

Les trois produisent exactement le même arbre interne.

---

## 49.10 Annotation API, mais sans magie dangereuse

Les annotations restent utiles, mais elles ne doivent pas créer de routing invisible.

Bon :

```java
@Command("user <target:User> rank set <rank:Rank>")
public CommandResult setRank(CommandContext ctx) {
    User target = ctx.arg("target");
    Rank rank = ctx.arg("rank");

    return Results.success("Rank updated");
}
```

Ou :

```java
@Command("user rank set")
public CommandResult setRank(
    @Arg("target") User target,
    @Arg("rank") Rank rank
) {
    return Results.success("Rank updated");
}
```

À éviter dans notre framework :

```java
@SubCommand(attachTo = "<target>")
@InheritedArg
```

Règle :

> Les annotations doivent déclarer une route, pas cacher une route.

---

## 49.11 Subcommand Router robuste

Le router doit être construit comme un vrai parseur de route.

Il doit gérer proprement :

- literals
- aliases
- typed arguments
- optional arguments
- greedy arguments
- flags
- inherited parent args
- fallback handlers
- default handlers
- ambiguous routes
- partial routes for autocomplete

Exemple d'ambiguïté :

```text
user <target>
user list
```

Input :

```text
user list
```

Le router doit choisir le literal `list` avant l'argument `<target>`.

Priorité recommandée :

```text
1. literal exact
2. literal alias
3. typed argument constrained
4. typed argument generic
5. greedy argument
6. fallback
```

Cela évite que `list` soit avalé comme un username.

---

## 49.12 Subcommand autocomplete fiable

Comme les subcommands sont des vrais nodes, l'autocomplete devient simple.

Input :

```text
user Victor r
```

Le router sait :

```text
user <target> [cursor here]
```

Suggestions :

```text
rank
remove
rename
```

Input :

```text
user Victor rank set a
```

Le router sait :

```text
rank argument is being edited
```

Suggestions :

```text
admin
architect
assistant
```

Pas besoin de réimplémenter une logique spéciale pour les subcommands.

---

## 49.13 Objectif de différenciation

Le framework ne doit pas être :

```text
Imperat but with another name
```

Il doit être :

```text
A platform-neutral command routing engine
with clean explicit command trees,
first-class autocomplete,
clear route patterns,
and small configurable adapters.
```

Différences clés :

| Sujet | Imperat-style à éviter | Notre design |
|---|---|---|
| Core | multi-platform mais modules très orientés plateformes connues | core totalement platform-agnostic |
| Subcommands | annotations, classes imbriquées, attachTo, inherited args | command tree explicite |
| Routing | structure parfois implicite | route visible et inspectable |
| Autocomplete | dépend beaucoup des annotations/suggestions | dérivé du router + parsers |
| Adapters | modules par plateformes | SDK d'adapter générique |
| API avancée | annotation-heavy | manual + builder + route pattern + annotations |
| Debug | difficile si la route est implicite | `framework.inspectRoute(input)` possible |

---

## 49.14 API d'inspection/debug

Ajouter une API de debug.

```java
RouteDebug debug = framework.inspectRoute("user Victor rank set admin");
```

Output :

```text
Matched route:
  user <target:User> rank set <rank:Rank>

Tokens:
  user      -> literal
  Victor    -> argument target = User("Victor")
  rank      -> literal
  set       -> literal
  admin     -> argument rank = ADMIN

Handler:
  UserCommands#setRank
```

Très utile pour éviter les problèmes de frameworks où les subcommands sont impossibles à comprendre.

---

## 49.15 Règle finale

Le framework doit être universel, mais surtout prévisible.

La priorité n'est pas seulement :

```text
supporter beaucoup de plateformes
```

La priorité est :

```text
avoir un coeur tellement propre que n'importe quelle plateforme peut s'y brancher sans casser le modèle mental.
```

Phrase de design :

> No magic subcommands. No platform leakage. No hidden routing. Everything is a node, every route is inspectable, every adapter is replaceable.


---

# 50. Route Pattern DSL officiel

Le DSL ne doit pas être une petite feature secondaire.

Il doit devenir une des APIs principales du framework, au même niveau que :

- Manual API
- Builder API
- Annotation API

Objectif :

> Pouvoir déclarer une commande complète avec une string lisible, typée, documentable et compilable vers le CommandNode tree.

Exemple :

```java
registry.route("user <target:User> rank set <rank:Rank> [--silent]")
    .description("Set a user's rank")
    .permission("user.rank.set")
    .handler(ctx -> {
        User target = ctx.arg("target");
        Rank rank = ctx.arg("rank");
        boolean silent = ctx.flag("silent");

        return Results.success("Rank updated");
    });
```

Cette route compile vers exactement le même arbre que :

```java
Commands.literal("user")
    .argument(Arguments.required("target", User.class))
    .child(Commands.literal("rank")
        .child(Commands.literal("set")
            .argument(Arguments.required("rank", Rank.class))
            .flag(Flags.bool("silent"))
        )
    );
```

---

## 50.1 Pourquoi le DSL est important

Le DSL donne une seule source de vérité.

Une route DSL peut servir à :

- parser la commande
- générer le CommandNode tree
- générer l'aide
- générer l'autocomplete
- exporter un schema JSON
- générer une UI admin
- générer de la documentation Markdown
- détecter les conflits
- synchroniser Discord slash commands
- tester les routes avec snapshots

Le DSL rend les commandes lisibles sans avoir à suivre 5 classes imbriquées.

---

## 50.2 Syntaxe de base

### Literal

```text
server reload
```

```java
registry.route("server reload")
    .handler(...);
```

---

### Argument typé obligatoire

```text
ban <target:User>
```

```java
registry.route("ban <target:User>")
    .handler(ctx -> {
        User target = ctx.arg("target");
        return Results.success("Banned " + target.name());
    });
```

---

### Argument optionnel

```text
ban <target:User> [reason:String]
```

---

### Argument greedy

```text
broadcast <message:String...>
```

Le `...` signifie : consomme le reste de l'input.

---

### Argument optionnel greedy

```text
ban <target:User> [reason:String...]
```

---

### Boolean flag

```text
server reload [--force]
```

---

### Option flag avec valeur

```text
give <target:User> <item:Item> [--amount:Int]
```

---

### Alias de flag

```text
give <target:User> <item:Item> [--amount:Int|-a]
```

---

### Alias de commande

```text
teleport|tp <target:User>
```

---

### Plusieurs literals possibles

```text
user rank set <target:User> <rank:Rank>
```

---

## 50.3 Types DSL built-in

Alias de types recommandés :

```text
String
Str
Int
Integer
Long
Double
Float
Bool
Boolean
UUID
Duration
Path
Url
Enum(...)
```

Exemples :

```text
wait <duration:Duration>
user find <id:UUID>
download <url:Url>
```

Pour les types custom :

```text
User
Rank
World
Item
Permission
```

Ils sont résolus par l'`ArgumentRegistry`.

```java
framework.arguments()
    .register(User.class, new UserParser(userService))
    .register(World.class, new WorldParser(worldService));
```

---

## 50.4 Enum inline

Support possible :

```text
weather set <type:Enum(SUN,RAIN,STORM)>
```

Ou enum Java :

```text
weather set <type:WeatherType>
```

Le deuxième est meilleur si l'enum existe en Java.

---

## 50.5 Contraintes inline

Le DSL peut supporter des contraintes simples.

```text
give <target:User> <item:Item> <amount:Int{1..64}>
```

```text
ban <target:User> [reason:String{max=120}...]
```

```text
speed set <value:Double{0.0..10.0}>
```

Ces contraintes compilent vers des validators.

Exemple équivalent :

```java
Arguments.required("amount", Integer.class)
    .validate(value -> value >= 1 && value <= 64)
```

---

## 50.6 Metadata inline

Optionnel, pour plus tard.

```text
server reload [--force:Bool @desc("Force reload")]
```

Mais recommandation :

> Garder le DSL lisible. Mettre les descriptions longues dans l'API fluide.

Exemple préféré :

```java
registry.route("server reload [--force]")
    .flag("force", flag -> flag.description("Force reload even if warnings exist"))
    .description("Reloads the server");
```

---

## 50.7 DSL + fluent customisation

Le DSL définit la structure.

L'API fluide ajoute les métadonnées.

```java
registry.route("ban <target:User> [reason:String...] [--silent|-s]")
    .description("Ban a user")
    .permission("mod.ban")
    .arg("target", arg -> arg
        .description("User to ban")
        .suggestions(UserSuggestions.onlineUsers())
    )
    .arg("reason", arg -> arg
        .description("Ban reason")
        .defaultValue("No reason")
    )
    .flag("silent", flag -> flag
        .description("Do not broadcast the ban")
    )
    .handler(ctx -> {
        User target = ctx.arg("target");
        String reason = ctx.argOr("reason", "No reason");
        boolean silent = ctx.flag("silent");

        return Results.success("Banned " + target.name());
    });
```

---

## 50.8 Grammaire proposée

Version simplifiée :

```text
route        ::= segment+
segment      ::= literal
               | argument
               | optional
               | flag

literal      ::= NAME ("|" NAME)*

argument     ::= "<" ARG_NAME ":" TYPE modifiers? constraints? ">"

optional     ::= "[" (argument_inner | flag_inner) "]"

flag         ::= "["? "--" FLAG_NAME flag_type? flag_aliases? "]"?

flag_type    ::= ":" TYPE

flag_aliases ::= "|" "-" SHORT_NAME
               | "|" "--" LONG_ALIAS

modifiers    ::= "..."

constraints  ::= "{" constraint_body "}"
```

Exemples valides :

```text
ban <target:User>
ban <target:User> [reason:String...]
server reload [--force]
give <target:User> <item:Item> [--amount:Int|-a]
teleport|tp <target:User> [world:World]
speed set <value:Double{0.0..10.0}>
```

---

## 50.9 Compilation DSL vers CommandNode

Pipeline :

```text
Route string
    ↓
RouteTokenizer
    ↓
RoutePatternParser
    ↓
RouteAst
    ↓
RouteCompiler
    ↓
CommandNode tree
```

Classes :

```java
RouteTokenizer
RoutePatternParser
RouteAst
RouteCompiler
RouteCompileException
```

AST exemple :

```text
Route(
  Literal("ban"),
  Argument(name="target", type="User", required=true),
  Argument(name="reason", type="String", required=false, greedy=true),
  Flag(name="silent", type="Boolean", required=false)
)
```

---

## 50.10 Erreurs DSL propres

Le DSL doit avoir ses propres erreurs au startup.

Exemples :

```text
Invalid route pattern:
ban <target:User [reason:String]

Reason:
Missing closing '>' for argument target.
```

```text
Invalid route pattern:
give <amount:Int{64..1}>

Reason:
Invalid range. Minimum cannot be greater than maximum.
```

```text
Invalid route pattern:
ban [reason:String] <target:User>

Reason:
Required argument cannot appear after optional positional argument.
```

Ces erreurs doivent arriver au register, pas au runtime.

---

## 50.11 Validation au registration time

Quand une route DSL est enregistrée, le framework doit vérifier :

- syntaxe valide
- types connus
- pas de required après optional
- greedy argument en dernier
- pas de double nom d'argument
- pas de double flag
- pas de conflit évident avec une route existante
- handler présent si la route est exécutable
- permissions cohérentes
- aliases non ambigus

---

## 50.12 DSL et autocomplete

L'autocomplete doit être automatiquement générée depuis le DSL.

Route :

```text
user <target:User> rank set <rank:Rank> [--silent]
```

Input :

```text
user V
```

Suggestions :

```text
Victor
Vincent
Valkyrie
```

Input :

```text
user Victor r
```

Suggestions :

```text
rank
```

Input :

```text
user Victor rank set a
```

Suggestions :

```text
admin
assistant
architect
```

Input :

```text
user Victor rank set admin --
```

Suggestions :

```text
--silent
```

Tout vient du même arbre compilé depuis le DSL.

---

## 50.13 DSL et help generation

Route :

```text
ban <target:User> [reason:String...] [--silent|-s]
```

Help générée :

```text
Usage:
  ban <target> [reason...] [--silent|-s]

Arguments:
  target    User to ban
  reason    Optional ban reason

Flags:
  --silent, -s    Do not broadcast the ban
```

---

## 50.14 DSL et schema export

Le DSL doit être exportable en schema.

```java
CommandSchema schema = framework.schema();
```

Exemple JSON :

```json
{
  "route": "ban <target:User> [reason:String...] [--silent|-s]",
  "name": "ban",
  "arguments": [
    {
      "name": "target",
      "type": "User",
      "required": true,
      "greedy": false
    },
    {
      "name": "reason",
      "type": "String",
      "required": false,
      "greedy": true
    }
  ],
  "flags": [
    {
      "name": "silent",
      "type": "Boolean",
      "aliases": ["s"],
      "required": false
    }
  ]
}
```

Ce schema peut alimenter :

- UI web
- Discord slash commands
- docs Markdown
- tests
- remote autocomplete
- admin panel

---

## 50.15 DSL et annotations

Les annotations peuvent utiliser le DSL directement.

```java
@Command("ban <target:User> [reason:String...] [--silent|-s]")
@Permission("mod.ban")
public CommandResult ban(CommandContext ctx) {
    User target = ctx.arg("target");
    String reason = ctx.argOr("reason", "No reason");
    boolean silent = ctx.flag("silent");

    return Results.success("Banned " + target.name());
}
```

Ou version paramètres :

```java
@Command("ban")
public CommandResult ban(
    @Arg("target") User target,
    @Greedy @OptionalArg @Arg("reason") String reason,
    @Flag("silent") boolean silent
) {
    return Results.success("Banned " + target.name());
}
```

Mais la version DSL est plus explicite.

---

## 50.16 DSL et Manual API

La Manual API reste utile pour les cas ultra dynamiques.

Mais le DSL peut être utilisé dans la Manual API :

```java
CommandNode node = Routes.compile(
    "ban <target:User> [reason:String...] [--silent|-s]"
)
.description("Ban a user")
.permission("mod.ban")
.handler(...)
.build();

registry.register(node);
```

---

## 50.17 DSL et Builder API

Le builder peut accepter des routes :

```java
registry.route("server reload [--force]")
    .description("Reload server")
    .handler(...);
```

Mais aussi garder la forme classique :

```java
registry.command("server", server -> server
    .subcommand("reload", reload -> reload
        .flag("force", Boolean.class)
        .executes(...)
    )
);
```

Les deux sont valides.

---

## 50.18 DSL et conflict analyzer

Le DSL rend l'analyse de conflits plus facile.

Exemple :

```text
user <target:User>
user list
```

Le framework peut afficher :

```text
Potential conflict:
  user <target:User>
  user list

Reason:
  "list" may be parsed as User if the User parser accepts arbitrary strings.

Resolution:
  literal routes are prioritized before argument routes.
```

Autre exemple :

```text
tp <target:User>
tp <world:World>
```

Conflit :

```text
Ambiguous argument route:
  both User and World parsers may accept the same token.
```

---

## 50.19 DSL comme identité officielle d'une commande

Chaque commande doit pouvoir retourner sa route canonique.

```java
command.routePattern();
```

Exemple :

```text
ban <target:User> [reason:String...] [--silent|-s]
```

Cette route canonique sert pour :

- logs
- docs
- debug
- metrics
- schema
- studio
- command replay

---

## 50.20 DSL versioning

Prévoir une version du DSL.

```java
@CommandDslVersion("1")
```

Ou dans le framework :

```java
CommandFramework.builder()
    .dslVersion(DslVersion.V1)
    .build();
```

Pourquoi :

- ajouter des features plus tard sans casser les anciennes routes
- permettre des warnings de dépréciation
- maintenir une API stable

---

## 50.21 DSL design rule

Le DSL doit rester lisible.

Bon :

```text
ban <target:User> [reason:String...] [--silent|-s]
```

Mauvais :

```text
ban <target:User{online=true,permission=mod.target,case=insensitive}> [reason:String{max=120,greedy=true,optional=true}]
```

Règle :

> Le DSL décrit la structure. L'API fluide décrit les détails riches.

Donc :

```java
registry.route("ban <target:User> [reason:String...] [--silent|-s]")
    .arg("target", arg -> arg.onlineOnly())
    .arg("reason", arg -> arg.maxLength(120))
    .flag("silent", flag -> flag.description("Do not broadcast"));
```

---

## 50.22 Feature unique : Route DSL Playground

Avec le Command Studio, ajouter un playground DSL.

L'utilisateur tape :

```text
ban <target:User> [reason:String...] [--silent|-s]
```

Le studio affiche :

```text
Command tree
Arguments
Flags
Generated help
Example inputs
Autocomplete simulation
Conflict warnings
JSON schema
```

C'est une feature très différenciante.

---

## 50.23 Résumé

Le DSL officiel donne au framework une identité forte.

Il permet de dire :

> This framework is not annotation-first. It is route-first.

Différence fondamentale :

```text
Imperat-style:
  annotations describe methods and subcommands are inferred

Notre style:
  route patterns describe commands and everything compiles into an inspectable tree
```

Phrase clé :

> Write the route once. Get parsing, autocomplete, help, docs, schema, validation, debugging and adapters for free.


---

# 51. Positionnement final : choix libre des APIs

Le framework ne doit forcer aucun style.

Le DSL est une feature puissante, mais pas obligatoire.

Le coeur officiel reste :

```text
CommandNode Tree
```

Toutes les APIs publiques compilent vers ce même modèle.

```text
DSL Route API ─────┐
Annotation API ────┤
Builder API ───────┤──> CommandNode Tree ──> Dispatcher
Manual API ────────┘
```

Donc le framework est :

```text
command-tree-first
```

et non :

```text
DSL-first
annotation-first
builder-first
```

Phrase de design :

> Le DSL est un langage optionnel de déclaration de routes, pas une contrainte imposée à l'utilisateur.

---

## 51.1 Annotation sans DSL

```java
@Command("ban")
@Permission("mod.ban")
public CommandResult ban(
    @Arg("target") User target,
    @OptionalArg @Greedy @Arg("reason") String reason,
    @Flag("silent") boolean silent
) {
    return Results.success("Banned " + target.name());
}
```

---

## 51.2 Annotation avec DSL

```java
@Command("ban <target:User> [reason:String...] [--silent|-s]")
@Permission("mod.ban")
public CommandResult ban(CommandContext ctx) {
    User target = ctx.arg("target");
    String reason = ctx.argOr("reason", "No reason");
    boolean silent = ctx.flag("silent");

    return Results.success("Banned " + target.name());
}
```

---

## 51.3 Builder sans DSL

```java
registry.command("ban", ban -> ban
    .permission("mod.ban")
    .argument("target", User.class)
    .greedyOptional("reason", String.class)
    .flag("silent", Boolean.class)
    .executes(ctx -> Results.success("Banned"))
);
```

---

## 51.4 Manual API sans DSL

```java
registry.register(
    Commands.literal("ban")
        .permission("mod.ban")
        .argument(Arguments.required("target", User.class))
        .argument(Arguments.greedyOptional("reason", String.class))
        .flag(Flags.bool("silent").alias("s"))
        .handler(ctx -> Results.success("Banned"))
        .build()
);
```

---

## 51.5 Route DSL direct

```java
registry.route("ban <target:User> [reason:String...] [--silent|-s]")
    .permission("mod.ban")
    .handler(ctx -> Results.success("Banned"));
```

Toutes ces formes doivent produire le même arbre interne.

---

# 52. Philosophie projet

Le framework doit être pensé comme un moteur universel de commandes, pas comme une simple librairie Minecraft ou CLI.

## 52.1 Principes

1. Le core ne dépend d'aucune plateforme.
2. Les plateformes se branchent via adapters.
3. Les commandes sont des routes inspectables.
4. L'autocomplete vient du même modèle que le parsing.
5. Les erreurs sont typées, propres et utiles.
6. Les APIs sont multiples mais convergent vers un seul modèle.
7. Les subcommands sont explicites, jamais magiques.
8. La réflexion est autorisée au registration time, jamais nécessaire au runtime.
9. La documentation, le schema, l'aide et l'autocomplete viennent des metadata.
10. La qualité du code doit être stricte dès le début.

---

## 52.2 Ce qu'on ne veut pas

Éviter :

- subcommands attachées à des arguments par string magique
- héritage implicite illisible
- annotations obligatoires
- core dépendant de Minecraft
- adapters énormes contenant la logique de parsing
- autocomplete séparé du dispatcher
- erreurs génériques inutiles
- API qui casse dès qu'on ajoute une nouvelle plateforme
- code non testé dans le core

---

## 52.3 Ce qu'on veut

Créer un framework où :

```text
une route = parsing + autocomplete + help + docs + schema + debug
```

Et où chaque plateforme ne fait que :

```text
native event -> CommandInput -> dispatcher -> CommandResult -> native output
```

---

# 53. Architecture Maven/Gradle par modules

Recommandation : Gradle multi-module avec Kotlin DSL.

Structure :

```text
command-framework/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle/
├── docs/
├── examples/
├── modules/
│   ├── core/
│   ├── api/
│   ├── annotations/
│   ├── dsl/
│   ├── builder/
│   ├── testkit/
│   ├── adapters/
│   │   ├── terminal/
│   │   ├── gamechat/
│   │   ├── discord/
│   │   └── minecraft/
│   │       ├── common/
│   │       ├── bukkit/
│   │       ├── paper/
│   │       ├── velocity/
│   │       ├── minestom/
│   │       └── sponge/
│   ├── studio/
│   ├── schema/
│   └── docs-generator/
└── README.md
```

---

## 53.1 Module `api`

Contient uniquement les interfaces stables publiques.

```text
CommandSource
CommandInput
CommandResult
CommandContext
CommandMessage
CommandPlatform
CommandAdapter
ArgumentParser
SuggestionProvider
PermissionChecker
```

Ce module doit être léger et très stable.

Règle :

> Les adapters et plugins externes doivent pouvoir dépendre de `api` sans tirer tout le framework.

---

## 53.2 Module `core`

Contient l'implémentation principale.

```text
CommandDispatcher
CommandRegistry
CommandGraph
CommandNode
CommandRouter
CommandTokenizer
FlagParser
ArgumentResolver
SuggestionEngine
MiddlewareChain
ErrorHandler
HelpGenerator
```

Dépend de :

```text
api
```

Ne dépend pas de :

```text
Minecraft
Discord
JLine
Spring
```

---

## 53.3 Module `dsl`

Contient :

```text
RouteTokenizer
RoutePatternParser
RouteAst
RouteCompiler
RouteCompileException
DslVersion
```

Dépend de :

```text
api
core
```

Le DSL est optionnel.

Un utilisateur peut ne jamais importer ce module.

---

## 53.4 Module `annotations`

Contient :

```text
@Command
@Route
@CommandGroup
@Arg
@Flag
@Permission
@Description
@Cooldown
@Confirm
@Hidden
AnnotationScanner
MethodBinder
```

Dépend de :

```text
api
core
dsl optionnel
```

Important :

- une annotation peut utiliser le DSL
- une annotation peut aussi utiliser une déclaration classique sans DSL
- le scanner compile tout vers `CommandNode`

---

## 53.5 Module `builder`

Peut être intégré au core ou séparé.

Contient :

```text
CommandBuilder
ArgumentBuilder
FlagBuilder
RouteBuilder
```

Si séparé, il dépend de :

```text
api
core
```

---

## 53.6 Module `testkit`

Contient :

```text
CommandTestKit
TestSource
TestPlatform
DispatchAssertions
SuggestionAssertions
RouteAssertions
FakePermissionChecker
```

Objectif :

```java
kit.dispatch("ban Steve")
    .assertSuccess();

kit.suggest("ban St")
    .assertContains("Steve");

kit.inspect("user Victor rank set admin")
    .assertMatched("user <target:User> rank set <rank:Rank>");
```

---

## 53.7 Module `schema`

Contient :

```text
CommandSchema
SchemaExporter
JsonSchemaExporter
YamlSchemaExporter
MarkdownSchemaExporter
```

Permet :

- docs auto
- web studio
- Discord slash sync
- admin panel
- tests snapshot

---

## 53.8 Module `studio`

Module optionnel.

Contient un mini dashboard web :

```java
framework.studio()
    .port(8080)
    .start();
```

Affiche :

- command tree
- routes
- permissions
- conflicts
- autocomplete playground
- route inspector
- schema export
- telemetry

Ne doit jamais être une dépendance obligatoire.

---

# 54. Architecture Minecraft multi-plateforme

Minecraft doit être supporté, mais ne doit pas polluer le core.

Il faut séparer :

```text
minecraft-common
minecraft-bukkit
minecraft-paper
minecraft-velocity
minecraft-minestom
minecraft-sponge
```

---

## 54.1 Module `minecraft-common`

Contient les abstractions communes Minecraft.

```text
MinecraftCommandSource
MinecraftAudience
MinecraftPlatform
MinecraftMessageRenderer
MinecraftPermissionBridge
MinecraftSuggestionBridge
MinecraftAdapterConfig
```

Mais attention : même `minecraft-common` ne doit pas imposer Bukkit/Paper.

Il peut définir ses propres interfaces :

```java
public interface MinecraftActor {
    UUID uniqueId();

    String name();

    boolean hasPermission(String permission);

    void sendMessage(CommandMessage message);

    <T> Optional<T> nativeHandle(Class<T> type);
}
```

Puis chaque plateforme mappe son objet natif.

---

## 54.2 Bukkit adapter

Module :

```text
adapters/minecraft/bukkit
```

Dépendances :

```text
api
core
minecraft-common
Bukkit API
```

Exemple :

```java
BukkitCommandAdapter.attach(framework, plugin)
    .prefix("/")
    .registerCommands(true)
    .tabComplete(true)
    .sourceMapper(sender -> Sources.generic(
        sender.getName(),
        sender.getName(),
        sender
    ))
    .messageSender((sender, message) -> sender.sendMessage(message.text()))
    .register();
```

Support :

- `CommandSender`
- `Player`
- console
- tab completion
- permissions Bukkit
- plugin lifecycle
- unregister on disable

---

## 54.3 Paper adapter

Paper peut être séparé de Bukkit pour profiter de fonctionnalités modernes.

Module :

```text
adapters/minecraft/paper
```

Support possible :

- Brigadier integration Paper
- Adventure components
- async completions si disponible
- meilleure gestion des audiences
- Paper command registration
- lifecycle events

API :

```java
PaperCommandAdapter.attach(framework, plugin)
    .useAdventure(true)
    .registerBrigadier(true)
    .tabComplete(true)
    .register();
```

---

## 54.4 Velocity adapter

Module :

```text
adapters/minecraft/velocity
```

Support :

- `CommandSource`
- `Player`
- console
- Brigadier-like command API Velocity
- suggestions
- Adventure components
- permissions Velocity

API :

```java
VelocityCommandAdapter.attach(framework, proxy, plugin)
    .registerCommands(true)
    .suggestions(true)
    .register();
```

---

## 54.5 Minestom adapter

Module :

```text
adapters/minecraft/minestom
```

Support :

- Minestom command system
- players
- console
- suggestions
- component messages

API :

```java
MinestomCommandAdapter.attach(framework)
    .registerCommands(true)
    .register();
```

---

## 54.6 Sponge adapter

Module optionnel.

Même logique :

```java
SpongeCommandAdapter.attach(framework, plugin)
    .register();
```

---

# 55. Minecraft multi-version strategy

Minecraft change souvent.

Le framework doit éviter de compiler un seul module contre trop de versions incompatibles.

## 55.1 Stratégie recommandée

Séparer par plateforme et, si nécessaire, par version majeure.

Exemple :

```text
minecraft-paper-common
minecraft-paper-1-20
minecraft-paper-1-21
minecraft-paper-latest
```

Mais ne pas faire ça dès le jour 1 si ce n'est pas nécessaire.

Démarrage recommandé :

```text
minecraft-bukkit
minecraft-paper
minecraft-velocity
minecraft-minestom
```

Puis ajouter des modules versionnés seulement si une API casse.

---

## 55.2 Version bridge

Créer une interface interne :

```java
public interface MinecraftVersionBridge {

    boolean supportsNativeCommandTree();

    boolean supportsAsyncSuggestions();

    boolean supportsRichTooltips();

    void registerCommand(MinecraftRegisteredCommand command);

    void unregisterCommand(String name);
}
```

Implémentations :

```text
PaperModernBridge
PaperLegacyBridge
BukkitLegacyBridge
VelocityBridge
```

L'adapter choisit le bridge au runtime ou au setup.

---

## 55.3 Ne jamais faire ça dans le core

Interdit dans `core` :

```java
if (serverVersion.startsWith("1.20")) ...
```

Toute logique version doit rester dans :

```text
minecraft adapter modules
```

---

## 55.4 Mapping des messages

Minecraft moderne utilise souvent Adventure.

Mais Bukkit legacy utilise des strings.

Créer :

```java
public interface MinecraftMessageRenderer {
    Object render(CommandMessage message, MinecraftPlatform platform);
}
```

Implémentations :

```text
PlainTextRenderer
AdventureRenderer
LegacyColorRenderer
MiniMessageRenderer
```

Config :

```java
PaperCommandAdapter.attach(framework, plugin)
    .messages(Messages.adventure())
    .register();
```

---

## 55.5 Suggestions Minecraft

Le framework produit :

```java
List<Suggestion>
```

L'adapter convertit vers :

- Bukkit TabCompleter
- Paper Brigadier suggestions
- Velocity suggestions
- Minestom suggestions

Le core ne connaît jamais ces types.

---

## 55.6 Permissions Minecraft

Le core appelle :

```java
source.hasPermission("mod.ban")
```

L'adapter mappe vers :

```java
sender.hasPermission("mod.ban")
player.hasPermission("mod.ban")
```

Pour les plateformes plus avancées, brancher LuckPerms via une extension optionnelle :

```text
minecraft-luckperms
```

Mais ne pas en faire une dépendance obligatoire.

---

# 56. Architecture adapters universels

Un adapter doit être petit.

Responsabilités d'un adapter :

```text
1. écouter l'événement natif
2. construire CommandSource
3. construire CommandInput
4. appeler framework.dispatch ou framework.suggest
5. rendre CommandResult
```

Un adapter ne doit pas :

- parser les arguments
- router les subcommands
- gérer les flags
- générer l'aide
- faire l'autocomplete manuellement
- contenir la logique métier

---

## 56.1 Adapter checklist

Chaque adapter doit documenter :

```text
supported platforms
supported versions
autocomplete support
rich messages support
permission support
async support
native command registration support
known limitations
```

---

# 57. API publique souhaitée

## 57.1 Setup minimal

```java
CommandFramework framework = CommandFramework.create();

framework.registry().command("ping", ping -> ping
    .executes(ctx -> Results.success("Pong"))
);
```

---

## 57.2 Setup complet

```java
CommandFramework framework = CommandFramework.builder()
    .caseMode(CaseMode.INSENSITIVE)
    .matchStrategy(MatchStrategies.caseInsensitive())
    .arguments(args -> args
        .register(User.class, new UserParser(userService))
        .register(Rank.class, Arguments.enumParser(Rank.class))
    )
    .middleware(m -> m
        .use(Permissions.middleware())
        .use(Cooldowns.middleware())
        .use(Logging.middleware())
    )
    .errors(errors -> errors
        .handler(new FriendlyErrorHandler())
    )
    .suggestions(s -> s
        .maxResults(20)
        .permissionAware(true)
    )
    .build();
```

---

## 57.3 Registration par annotations

```java
framework.registry().registerAnnotated(new UserCommands(userService));
```

---

## 57.4 Registration par DSL

```java
framework.registry()
    .route("ban <target:User> [reason:String...] [--silent|-s]")
    .permission("mod.ban")
    .description("Ban a user")
    .handler(ctx -> Results.success("Banned"));
```

---

## 57.5 Registration par builder

```java
framework.registry().command("ban", ban -> ban
    .permission("mod.ban")
    .argument("target", User.class)
    .greedyOptional("reason", String.class)
    .flag("silent", Boolean.class)
    .executes(ctx -> Results.success("Banned"))
);
```

---

## 57.6 Registration par manual API

```java
CommandNode node = Commands.literal("ban")
    .permission("mod.ban")
    .argument(Arguments.required("target", User.class))
    .argument(Arguments.greedyOptional("reason", String.class))
    .flag(Flags.bool("silent").alias("s"))
    .handler(ctx -> Results.success("Banned"))
    .build();

framework.registry().register(node);
```

---

# 58. Conventions Git

## 58.1 Branches

Branches principales :

```text
main
develop
```

Branches de travail :

```text
feature/<short-name>
fix/<short-name>
refactor/<short-name>
docs/<short-name>
test/<short-name>
release/<version>
hotfix/<short-name>
```

Exemples :

```text
feature/route-dsl-parser
feature/minecraft-paper-adapter
fix/greedy-argument-parsing
docs/adapter-sdk
```

---

## 58.2 Conventional Commits

Format :

```text
type(scope): message
```

Types :

```text
feat
fix
docs
style
refactor
perf
test
build
ci
chore
revert
```

Exemples :

```text
feat(core): add command graph dispatcher
feat(dsl): implement route pattern parser
feat(minecraft-paper): add Paper adapter
fix(core): resolve literal priority over argument nodes
test(dsl): add invalid pattern cases
docs(api): document CommandSource capabilities
```

---

## 58.3 Pull requests

Chaque PR doit contenir :

```text
Summary
Motivation
Changes
Tests
Breaking changes
Checklist
```

Checklist :

```text
- [ ] Tests added or updated
- [ ] Documentation updated
- [ ] No platform dependency leaked into core
- [ ] Public API reviewed
- [ ] Sonar issues fixed
- [ ] Coverage maintained
```

---

## 58.4 Merge strategy

Recommandation :

```text
Squash merge
```

Pourquoi :

- historique propre
- release notes plus faciles
- commits atomiques par PR

---

## 58.5 Tags

Format :

```text
vMAJOR.MINOR.PATCH
```

Exemples :

```text
v0.1.0
v0.2.0
v1.0.0
```

---

# 59. Qualité Sonar et conventions de code

## 59.1 Objectif qualité

Objectif strict :

```text
Sonar Quality Gate: PASSED
Coverage core: 100%
Coverage global: >= 90%
Duplications: < 3%
Bugs: 0
Vulnerabilities: 0
Code smells: reviewed
Security hotspots: reviewed
```

---

## 59.2 100% coverage

Le 100% absolu doit être exigé au minimum sur :

```text
api
core
dsl
schema
```

Pour les adapters, viser :

```text
>= 80-90%
```

car certaines parties nécessitent des mocks de plateforme.

Mais les adapters doivent quand même avoir :

- tests unitaires de mapping
- tests de rendering
- tests de suggestions
- tests lifecycle si possible

---

## 59.3 JaCoCo

Configurer JaCoCo par module.

Exemple :

```text
core: 100%
dsl: 100%
annotations: 95%+
builder: 100%
schema: 100%
testkit: 95%+
adapters: 80%+
```

Build doit échouer si le seuil n'est pas atteint.

---

## 59.4 Types de tests

### Unit tests

Pour :

- tokenizer
- parser
- router
- flag parser
- argument resolver
- suggestion engine
- DSL parser
- conflict analyzer

### Integration tests

Pour :

- annotation registration
- builder registration
- DSL registration
- manual registration
- dispatch end-to-end
- suggestions end-to-end
- help generation

### Contract tests

Pour les adapters.

Un adapter doit passer une suite commune :

```text
AdapterContractTest
```

Vérifie :

- source mapping
- input mapping
- result rendering
- autocomplete bridge
- permission bridge

### Snapshot tests

Pour :

- generated help
- schema export
- route debug output
- docs generation

---

## 59.5 Mutation testing

Optionnel mais très fort.

Utiliser PIT mutation testing pour le core.

Objectif :

```text
Mutation score core: >= 90%
```

Surtout pour :

- router
- parser
- conflict analyzer
- DSL compiler

---

## 59.6 Style Java

Recommandations :

- Java 17 minimum
- Java 21 possible si projet moderne
- `record` pour data objects
- sealed interfaces pour résultats/erreurs si utile
- pas de Lombok dans le core public
- nulls interdits dans API publique
- utiliser `Optional` pour absence explicite
- immutability par défaut
- classes finales sauf extension voulue

---

## 59.7 Static analysis

Outils :

```text
SonarCloud
Checkstyle
SpotBugs
Error Prone
NullAway ou Checker Framework
JaCoCo
PIT
```

---

## 59.8 Formatting

Choisir un formatter unique :

```text
google-java-format
```

ou Checkstyle custom.

Règle :

> Pas de débat de style en PR. Le formatter décide.

---

# 60. CI/CD

## 60.1 GitHub Actions

Workflows :

```text
ci.yml
release.yml
docs.yml
sonar.yml
publish-snapshot.yml
```

---

## 60.2 CI main

À chaque push/PR :

```text
checkout
setup Java
cache Gradle
compile
unit tests
integration tests
jacoco
sonar
spotbugs
checkstyle
build examples
```

---

## 60.3 Matrix Java

Tester :

```text
Java 17
Java 21
```

Si le projet choisit Java 17 comme baseline, Java 21 sert à vérifier compatibilité moderne.

---

## 60.4 Publication

Publier :

```text
Maven Central
GitHub Packages optionnel
```

Artefacts :

```text
command-api
command-core
command-dsl
command-annotations
command-testkit
command-adapter-terminal
command-adapter-minecraft-paper
...
```

---

# 61. Conventions d'API

## 61.1 Compatibilité binaire

À partir de `1.0.0`, respecter SemVer.

```text
MAJOR = breaking changes
MINOR = features compatibles
PATCH = fixes
```

---

## 61.2 Packages

Proposition :

```text
dev.yourname.commands.api
dev.yourname.commands.core
dev.yourname.commands.dsl
dev.yourname.commands.annotation
dev.yourname.commands.builder
dev.yourname.commands.adapter
dev.yourname.commands.minecraft
```

Éviter d'exposer :

```text
internal
```

Exemple :

```text
dev.yourname.commands.core.internal
```

Tout ce qui est `internal` peut changer sans SemVer.

---

## 61.3 Naming

Objets principaux :

```text
CommandFramework
CommandRegistry
CommandDispatcher
CommandGraph
CommandNode
CommandSource
CommandInput
CommandContext
CommandResult
CommandMessage
CommandAdapter
ArgumentParser
SuggestionProvider
```

Factories :

```text
Commands
Arguments
Flags
Results
Messages
Sources
Suggestions
MatchStrategies
```

---

# 62. Roadmap complète

## Phase 0 — Design freeze initial

Livrables :

- README vision
- architecture decision records
- module structure
- public API draft
- examples draft

---

## Phase 1 — API + Core minimal

Livrables :

- `api`
- `core`
- `CommandSource`
- `CommandInput`
- `CommandResult`
- `CommandNode`
- `CommandRegistry`
- `CommandDispatcher`
- simple tokenizer
- literal commands
- simple handlers

Exemple supporté :

```text
ping
```

---

## Phase 2 — Arguments typés

Livrables :

- `ArgumentParser<T>`
- `ArgumentRegistry`
- built-in parsers
- required args
- optional args
- greedy args
- typed parse errors

Exemples :

```text
ban Steve
broadcast hello world
```

---

## Phase 3 — Router subcommands propre

Livrables :

- tree router
- nested commands
- parent args
- route priority
- ambiguity handling
- aliases

Exemple :

```text
user Victor rank set admin
```

---

## Phase 4 — Flags/options

Livrables :

- boolean flags
- value options
- aliases
- defaults
- required flags
- flag autocomplete

Exemple :

```text
give Steve diamond --amount 64 -s
```

---

## Phase 5 — Suggestion engine

Livrables :

- partial route matching
- suggestions from literals
- suggestions from args
- suggestions from flags
- replacement ranges
- ranking

---

## Phase 6 — DSL

Livrables :

- route tokenizer
- parser
- AST
- compiler to CommandNode
- DSL errors
- DSL tests 100%
- route canonicalization

---

## Phase 7 — Builder + Manual API stable

Livrables :

- `Commands.literal`
- `Arguments.required`
- `Flags.bool`
- fluent builder
- route builder
- public API docs

---

## Phase 8 — Annotation API

Livrables :

- annotations
- scanner
- method binder
- parameter injection
- annotation + DSL support
- no magic subcommands

---

## Phase 9 — Help/schema/debug

Livrables :

- help generator
- schema exporter
- route inspector
- conflict analyzer
- mermaid graph export

---

## Phase 10 — Testkit

Livrables :

- dispatch assertions
- suggestion assertions
- route assertions
- fake source
- fake platform

---

## Phase 11 — Terminal adapter

Livrables :

- terminal dispatch
- prompt
- autocomplete
- history
- examples

---

## Phase 12 — Minecraft common + Bukkit/Paper

Livrables :

- minecraft-common
- Bukkit adapter
- Paper adapter
- permissions bridge
- tab completion
- Adventure renderer optional

---

## Phase 13 — Velocity/Minestom

Livrables :

- Velocity adapter
- Minestom adapter
- adapter contract tests

---

## Phase 14 — Studio alpha

Livrables :

- command tree viewer
- route inspector
- DSL playground
- schema viewer
- conflict reports

---

## Phase 15 — 1.0 release

Critères :

- API stable
- docs complètes
- examples complets
- Maven Central
- Sonar green
- core/dsl 100% coverage
- adapters documentés

---

# 63. Plan d'implémentation détaillé

## 63.1 Première semaine

Faire :

- repo Gradle multi-module
- api module
- core module
- testkit minimal
- CI minimal
- formatter
- JaCoCo
- README initial

Ne pas faire encore :

- Minecraft
- Discord
- Studio
- annotations avancées

---

## 63.2 Deuxième étape

Implémenter le core pur.

Ordre :

```text
CommandSource
CommandResult
CommandMessage
CommandNode
CommandRegistry
Tokenizer
Dispatcher
Basic tests
```

Objectif :

```java
framework.registry().command("ping", c -> c.executes(ctx -> Results.success("Pong")));
framework.dispatch(source, "ping");
```

---

## 63.3 Troisième étape

Ajouter parsing arguments.

Ordre :

```text
Token
TokenizedInput
ArgumentSpec
ArgumentParser
ArgumentRegistry
ArgumentResolver
Parse errors
```

Tests :

```text
required arg
optional arg
greedy arg
quoted strings
invalid integer
missing arg
```

---

## 63.4 Quatrième étape

Ajouter subcommand router.

Tests importants :

```text
literal beats argument
alias route
parent argument visible in child
unknown subcommand
ambiguous route warning
```

---

## 63.5 Cinquième étape

Ajouter flags.

Tests :

```text
--silent
-s
--amount 64
-a 64
unknown flag
missing flag value
flag default
```

---

## 63.6 Sixième étape

Ajouter suggestions.

Tests :

```text
root command suggestion
subcommand suggestion
argument suggestion
flag suggestion
replacement range
case insensitive suggestion
```

---

## 63.7 Septième étape

Ajouter DSL.

Ne pas mélanger au core au début.

Créer module séparé :

```text
dsl
```

Implémenter :

```text
RouteTokenizer
RoutePatternParser
RouteAst
RouteCompiler
```

Tests DSL nombreux.

---

## 63.8 Huitième étape

Ajouter annotations.

Important :

- scanner uniquement au registration time
- compile vers CommandNode
- pas de routing spécial annotation

---

## 63.9 Neuvième étape

Ajouter Minecraft Paper/Bukkit.

Seulement après core stable.

Sinon on risque de designer le framework autour de Minecraft.

---

# 64. Documentation à produire

Docs minimum :

```text
Getting Started
Core Concepts
CommandSource
Arguments
Flags
Subcommands
Autocomplete
DSL
Annotations
Builder API
Manual API
Adapters
Minecraft
Testing
Advanced Routing
Error Handling
Schema Export
```

Exemples :

```text
terminal-basic
minecraft-paper-basic
minecraft-bukkit-basic
velocity-basic
annotation-example
dsl-example
builder-example
manual-example
```

---

# 65. Critères de réussite

Le projet est réussi si :

1. On peut écrire une commande simple en moins de 5 lignes.
2. On peut écrire une commande complexe sans magie.
3. Les subcommands restent lisibles même à 5 niveaux.
4. L'autocomplete fonctionne sans code spécifique dans 80% des cas.
5. Le core fonctionne sans aucune plateforme.
6. Minecraft est un adapter, pas le centre du design.
7. Les conflits de routes sont détectés.
8. Les erreurs aident vraiment l'utilisateur.
9. Les docs peuvent être générées depuis le schema.
10. Le framework est testable sans lancer Minecraft/Discord/terminal.

---

# 66. Phrase finale du projet

> A universal, platform-neutral command routing engine for Java, with optional DSL, annotations, builder and manual APIs, clean subcommands, automatic autocomplete, schema export, strict quality, and adapters that never leak platform complexity into the core.
