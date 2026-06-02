# Suggestions

Use `suggest` for plain strings:

```java
List<String> values = framework.suggest(source, "g", 1);
```

Use `suggestRich` for adapter-friendly completion:

```java
List<Suggestion> values = framework.suggestRich(CommandInput.normalized(source, "g"));
```

Rich suggestions carry replacement text, replacement range, tooltip, type, and priority. Adapters can render those into Brigadier suggestions, terminal completions, Discord autocomplete choices, or IDE completions.

Custom parsers can provide suggestions through the public parser registry:

```java
CommandFramework framework = CommandFramework.builder()
    .suggestionProvider(Rank.class, ctx -> List.of(Suggestion.text("admin")))
    .build();
```
