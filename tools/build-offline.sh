#!/usr/bin/env bash
#
# Offline fallback build: no Gradle, no network.
#
# Compiles the mod with javac --release 8 against the jars of an already
# installed Forge 1.12.2 instance. Prefer `./gradlew build`; this script only
# exists for machines where the Gradle toolchain cannot download dependencies.
#
# Required:
#   INSTANCE  path to a Minecraft instance that contains a mods/ folder
#             with Mappet and MixinBooter installed
#
# Usage:
#   INSTANCE=/path/to/instance ./tools/build-offline.sh
#   INSTANCE=/path/to/instance ./tools/build-offline.sh --install
#
set -e

cd "$(dirname "$0")/.."

JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk}"
JAVAC="$JAVA_HOME/bin/javac"
JAR="$JAVA_HOME/bin/jar"

INSTANCE="${INSTANCE:-}"

if [ -z "$INSTANCE" ]; then
    echo "Set INSTANCE to a Forge 1.12.2 instance directory:" >&2
    echo "  INSTANCE=/path/to/instance $0" >&2
    exit 1
fi

MIXINBOOTER_JAR="${MIXINBOOTER_JAR:-$INSTANCE/mods/!mixinbooter-8.9.jar}"

# Looks for a jar in the given directories, newest version first.
find_jar() {
    local pattern="$1"
    shift

    local dir
    for dir in "$@"; do
        [ -d "$dir" ] || continue

        local found
        found="$(find "$dir" -name "$pattern" 2>/dev/null | sort -V | tail -n 1)"

        if [ -n "$found" ]; then
            echo "$found"
            return 0
        fi
    done

    return 0
}

LIB_DIRS=(
    "$INSTANCE/../libraries"
    "$HOME/.local/share/PrismLauncher/libraries"
    "$HOME/.gradle/caches"
)

FORGE_JAR="${FORGE_JAR:-}"
if [ -z "$FORGE_JAR" ] || [ ! -e "$FORGE_JAR" ]; then
    FORGE_JAR="$(find_jar 'forge-1.12.2-*-universal.jar' "${LIB_DIRS[@]}")"
fi

# Runtime log4j (2.8.1 on 1.12.2) is preferred over whatever Gradle cached.
LOG4J_API="${LOG4J_API:-}"
if [ -z "$LOG4J_API" ] || [ ! -e "$LOG4J_API" ]; then
    LOG4J_API="$(find_jar 'log4j-api-*.jar' "${LIB_DIRS[@]}")"
fi

GUAVA="${GUAVA:-}"
if [ -z "$GUAVA" ] || [ ! -e "$GUAVA" ]; then
    GUAVA="$(find_jar 'guava-*.jar' "${LIB_DIRS[@]}")"
fi

GSON="${GSON:-}"
if [ -z "$GSON" ] || [ ! -e "$GSON" ]; then
    GSON="$(find_jar 'gson-*.jar' "${LIB_DIRS[@]}")"
fi

VERSION="1.0.0"
BUILD="build/offline"
OUT="$BUILD/MappetSoundFix-$VERSION.jar"

echo "JAVAC: $JAVAC"
echo "FORGE: ${FORGE_JAR:-MISSING}"
echo "MIXINBOOTER: ${MIXINBOOTER_JAR:-MISSING}"
echo "LOG4J: ${LOG4J_API:-MISSING}"

[ -x "$JAVAC" ] || { echo "javac not found/executable" >&2; exit 1; }
[ -x "$JAR" ] || { echo "jar not found/executable" >&2; exit 1; }
[ -e "$FORGE_JAR" ] || { echo "forge jar missing" >&2; exit 1; }
[ -e "$MIXINBOOTER_JAR" ] || { echo "mixinbooter jar missing" >&2; exit 1; }

CP="$FORGE_JAR:$MIXINBOOTER_JAR"
[ -e "$LOG4J_API" ] && CP="$CP:$LOG4J_API"
[ -e "$GUAVA" ] && CP="$CP:$GUAVA"
[ -e "$GSON" ] && CP="$CP:$GSON"

rm -rf "$BUILD"
mkdir -p "$BUILD/classes"

find src/main/java -name '*.java' | sort > "$BUILD/sources.txt"

"$JAVAC" \
    --release 8 \
    -proc:none \
    -encoding UTF-8 \
    -Xlint:none \
    -cp "$CP" \
    -d "$BUILD/classes" \
    @"$BUILD/sources.txt"

cp -R src/main/resources/. "$BUILD/classes/"

sed -i "s/\${modVersion}/$VERSION/g" "$BUILD/classes/mcmod.info"

mkdir -p "$BUILD"
(cd "$BUILD/classes" && "$JAR" cf "../MappetSoundFix-$VERSION.jar" .)

echo "built: $OUT"

if [ "${1:-}" = "--install" ]; then
    cp "$OUT" "$INSTANCE/mods/"
    echo "installed: $INSTANCE/mods/MappetSoundFix-$VERSION.jar"
fi
