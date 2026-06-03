#!/usr/bin/env sh
set -eu

username=""
password=""
token_base64=""
signing_key_file=""
signing_key_password=""
gradle_user_home="${GRADLE_USER_HOME:-$HOME/.gradle}"

while [ "$#" -gt 0 ]; do
  case "$1" in
    --username)
      username="${2:?Missing value for --username}"
      shift 2
      ;;
    --password)
      password="${2:?Missing value for --password}"
      shift 2
      ;;
    --token-base64)
      token_base64="${2:?Missing value for --token-base64}"
      shift 2
      ;;
    --signing-in-memory-key-file)
      signing_key_file="${2:?Missing value for --signing-in-memory-key-file}"
      shift 2
      ;;
    --signing-in-memory-key-password)
      signing_key_password="${2:?Missing value for --signing-in-memory-key-password}"
      shift 2
      ;;
    --gradle-user-home)
      gradle_user_home="${2:?Missing value for --gradle-user-home}"
      shift 2
      ;;
    *)
      echo "Unknown argument: $1" >&2
      exit 2
      ;;
  esac
done

decode_base64() {
  if command -v base64 >/dev/null 2>&1; then
    printf '%s' "$1" | base64 --decode 2>/dev/null || printf '%s' "$1" | base64 -d
  else
    python3 -c 'import base64,sys; print(base64.b64decode(sys.argv[1]).decode())' "$1"
  fi
}

if [ -n "$token_base64" ]; then
  case "$token_base64" in
    *:*)
      echo "token-base64 must be the base64-encoded value of username:password, not the raw username:password text." >&2
      echo "Either encode it first or use --username and --password." >&2
      exit 2
      ;;
  esac
  decoded="$(decode_base64 "$token_base64")"
  case "$decoded" in
    *:*)
      username="${decoded%%:*}"
      password="${decoded#*:}"
      ;;
    *)
      echo "token-base64 must decode to username:password" >&2
      exit 2
      ;;
  esac
fi

if [ -z "$username" ] || [ -z "$password" ]; then
  echo "Provide either --username and --password, or --token-base64." >&2
  exit 2
fi

case "$gradle_user_home" in
  [A-Za-z]:/*)
    drive="$(printf '%s' "${gradle_user_home%%:*}" | tr '[:upper:]' '[:lower:]')"
    rest="${gradle_user_home#?:}"
    if [ -d "/mnt/$drive" ]; then
      gradle_user_home="/mnt/$drive$rest"
    else
      gradle_user_home="/$drive$rest"
    fi
    ;;
esac

properties_file="$gradle_user_home/gradle.properties"
mkdir -p "$gradle_user_home"
touch "$properties_file"

escape_value() {
  awk 'BEGIN { ORS="" } { gsub(/\\/,"\\\\"); printf "%s", $0; if (!eof) printf "\\n" }' "$1"
}

set_property() {
  key="$1"
  value="$2"
  tmp="${properties_file}.tmp"
  if grep -q "^[[:space:]]*$key[[:space:]]*=" "$properties_file"; then
    awk -v key="$key" -v value="$value" '
      $0 ~ "^[[:space:]]*" key "[[:space:]]*=" { print key "=" value; next }
      { print }
    ' "$properties_file" > "$tmp"
  else
    cat "$properties_file" > "$tmp"
    printf '%s=%s\n' "$key" "$value" >> "$tmp"
  fi
  mv "$tmp" "$properties_file"
}

set_property "mavenCentralUsername" "$username"
set_property "mavenCentralPassword" "$password"

stored_keys="mavenCentralUsername, mavenCentralPassword"

if [ -n "$signing_key_file" ]; then
  key_value="$(awk 'BEGIN { ORS="" } { gsub(/\\/,"\\\\"); printf "%s", $0; printf "\\n" }' "$signing_key_file")"
  set_property "signingInMemoryKey" "$key_value"
  stored_keys="$stored_keys, signingInMemoryKey"
fi

if [ -n "$signing_key_password" ]; then
  set_property "signingInMemoryKeyPassword" "$signing_key_password"
  stored_keys="$stored_keys, signingInMemoryKeyPassword"
fi

chmod 600 "$properties_file" 2>/dev/null || true
echo "Maven Central Gradle properties updated at $properties_file"
echo "Stored keys: $stored_keys"
