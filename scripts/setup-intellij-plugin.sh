#!/usr/bin/env sh
set -eu

plugin_id="dev.riege.buildmycommand.intellij"
plugin_version="0.1.0"
skip_build="false"

while [ "$#" -gt 0 ]; do
  case "$1" in
    --skip-build)
      skip_build="true"
      shift
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
      echo "Usage: $0 [--skip-build] [--plugin-id ID] [--plugin-version VERSION]" >&2
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
  echo "Install it from IntelliJ: Settings > Plugins > gear > Install Plugin from Disk..."
else
  echo "No plugin ZIP found yet. Run ./gradlew :intellij-plugin:buildPlugin"
fi
