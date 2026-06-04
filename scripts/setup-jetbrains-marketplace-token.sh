#!/usr/bin/env sh
set -eu

skip_github_secret=false

while [ "$#" -gt 0 ]; do
  case "$1" in
    --skip-github-secret)
      skip_github_secret=true
      shift
      ;;
    *)
      echo "Unknown argument: $1" >&2
      exit 1
      ;;
  esac
done

token="${JETBRAINS_MARKETPLACE_TOKEN:-}"

if [ -z "$token" ]; then
  printf "JetBrains Marketplace token: " >&2
  stty -echo
  IFS= read -r token
  stty echo
  printf "\n" >&2
fi

if [ -z "$token" ]; then
  echo "JETBRAINS_MARKETPLACE_TOKEN is empty." >&2
  exit 1
fi

case "$token" in
  perm:*) ;;
  *) echo "Warning: JetBrains Marketplace permanent tokens usually start with 'perm:'." >&2 ;;
esac

if [ "$skip_github_secret" = false ]; then
  if ! command -v gh >/dev/null 2>&1; then
    echo "GitHub CLI 'gh' was not found. Install gh or rerun with --skip-github-secret." >&2
    exit 1
  fi

  gh secret set JETBRAINS_MARKETPLACE_TOKEN --body "$token"
  echo "Stored JETBRAINS_MARKETPLACE_TOKEN as a GitHub Actions repository secret."
fi

echo "For this shell session, run:"
echo "export JETBRAINS_MARKETPLACE_TOKEN='<token hidden>'"
echo "For long-term shell persistence, add that export to your shell profile manually."
echo "JetBrains Marketplace token setup complete."
