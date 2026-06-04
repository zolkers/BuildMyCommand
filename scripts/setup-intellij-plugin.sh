#!/usr/bin/env sh
# Copyright (c) 2026 Zolkers
#
# Licensed under the MIT License.
# SPDX-License-Identifier: MIT

set -eu

plugin_id="dev.riege.buildmycommand.dsl"
plugin_version="0.2.1"
skip_build="false"
install="false"
ide_config_dir=""

while [ "$#" -gt 0 ]; do
  case "$1" in
    --skip-build)
      skip_build="true"
      shift
      ;;
    --install)
      install="true"
      shift
      ;;
    --ide-config-dir)
      ide_config_dir="$2"
      shift 2
      ;;
    --plugin-id)
      plugin_id="$2"
      shift 2
      ;;
    --plugin-version)
      plugin_version="$2"
      shift 2
      ;;
    *)
      echo "Unknown argument: $1" >&2
      echo "Usage: $0 [--skip-build] [--install] [--ide-config-dir DIR] [--plugin-id ID] [--plugin-version VERSION]" >&2
      exit 2
      ;;
  esac
done

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
root=$(CDPATH= cd -- "$script_dir/.." && pwd)
idea_dir="$root/.idea"
external_dependencies="$idea_dir/externalDependencies.xml"

mkdir -p "$idea_dir"

cat > "$external_dependencies" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<project version="4">
  <component name="ExternalDependencies">
    <plugin id="$plugin_id" min-version="$plugin_version" />
  </component>
</project>
EOF

echo "Declared required IntelliJ plugin in $external_dependencies"

if [ "$skip_build" != "true" ]; then
  "$root/gradlew" ":intellij-plugin:buildPlugin"
fi

distribution_dir="$root/modules/intellij-plugin/build/distributions"
zip=""
if [ -d "$distribution_dir" ]; then
  zip=$(find "$distribution_dir" -maxdepth 1 -name "*.zip" -type f -print | sort | tail -n 1)
fi

if [ -n "$zip" ]; then
  echo "Plugin ZIP ready: $zip"
else
  echo "No plugin ZIP found yet. Run ./gradlew :intellij-plugin:buildPlugin"
fi

if [ "$install" = "true" ]; then
  if [ -z "$zip" ]; then
    echo "Cannot install the IntelliJ plugin because no ZIP was found." >&2
    exit 1
  fi

  if [ -z "$ide_config_dir" ]; then
    jetbrains_dir="${XDG_CONFIG_HOME:-$HOME/.config}/JetBrains"
    if [ -d "$jetbrains_dir" ]; then
      ide_config_dir=$(find "$jetbrains_dir" -maxdepth 1 -type d \( -name "IntelliJIdea*" -o -name "IdeaIC*" \) -print | sort | tail -n 1)
    fi
  fi

  if [ -z "$ide_config_dir" ]; then
    echo "No IntelliJ config directory found. Pass --ide-config-dir explicitly." >&2
    exit 1
  fi

  plugins_dir="$ide_config_dir/plugins"
  install_dir="$plugins_dir/intellij-plugin"
  mkdir -p "$plugins_dir"

  case "$(CDPATH= cd -- "$plugins_dir" && pwd)/intellij-plugin" in
    "$(CDPATH= cd -- "$plugins_dir" && pwd)"/*) ;;
    *)
      echo "Refusing to install outside IntelliJ plugins directory: $install_dir" >&2
      exit 1
      ;;
  esac

  rm -rf "$install_dir"
  unzip -q "$zip" -d "$plugins_dir"
  echo "Installed BuildMyCommand plugin into $install_dir"
  echo "Restart IntelliJ IDEA to load it."
elif [ -n "$zip" ]; then
  echo "Install it from IntelliJ: Settings > Plugins > gear > Install Plugin from Disk..."
  echo "Or run: ./scripts/setup-intellij-plugin.sh --skip-build --install"
fi
