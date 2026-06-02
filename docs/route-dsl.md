# Route DSL

Routes describe command shape in one string:

```java
framework.registry()
    .route("moderation punish <target:String> <reason:String...> [--duration:Integer|-d] [--silent|-s]")
    .executes(ctx -> Results.success("ok"));
```

Supported forms:

- Literals: `moderation punish`
- Root and subcommand aliases: `ban|block <target:String>`
- Required arguments: `<target:String>`
- Optional arguments: `[<amount:Integer>]`
- Greedy arguments: `<reason:String...>`
- Flags: `[--silent|-s]`
- Options: `[--duration:Integer|-d]`
- Enum-like types: `<mode:enum(EASY,NORMAL,HARD)>`
- Ranges: `<amount:int{1..64}>`

The DSL module parses and canonicalizes routes before core registration. Core still owns execution, permissions, suggestions, and middleware.

Aliases are command literals, not argument values. Case-insensitive matching is an explicit framework policy:

```java
CommandFramework.builder().caseInsensitiveLiterals().caseInsensitiveOptions().build();
```
