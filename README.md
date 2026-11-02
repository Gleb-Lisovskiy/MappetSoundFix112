# Mappet Sound Fix

A small client-side addon for **Mappet 0.9 on Minecraft 1.12.2 (Forge)** that
fixes custom sounds not loading on case-sensitive file systems such as Linux
and macOS.

* **Minecraft:** 1.12.2
* **Loader:** Forge
* **Mappet:** 0.9.x
* **MixinBooter:** 8.x
* **Side:** client only

## The problem

Mappet turns every `.ogg` file in `config/mappet/sounds` into a sound event.
The generated `sounds.json` keeps the real file name, for example:

```json
"trs.loops.loop_serverMid": {
  "sounds": ["mp.sounds:trs/loops/loop_serverMid"]
}
```

Minecraft stores resource paths in lower case, so when the sound is loaded the
game asks for:

```
sounds/trs/loops/loop_servermid.ogg
```

Mappet 0.9 resolves that path with:

```java
private File getFile(String path)
{
    return new File(this.folder, path.substring(7));
}
```

On Windows NTFS is case-insensitive, so `loop_servermid.ogg` still finds
`loop_serverMid.ogg`. On Linux ext4 it does not, and the sound is skipped:

```
File mp.sounds:sounds/trs/loops/loop_servermid.ogg does not exist, cannot add it to event mp.sounds:trs.loops.loop_servermid
```

This is why some sound packs lose exactly the files that contain upper-case
letters, while every lower-case file keeps working.

## How it works

`SoundPackMixin` injects into `mchorse.mappet.client.SoundPack.getFile` at
`RETURN`:

1. If Mappet's exact lookup found the file, nothing changes.
2. If the file does not exist, the path is resolved inside
   `config/mappet/sounds` **case-insensitively, component by component**.
3. If several files differ only by case, the lexicographically smallest name is
   chosen so the result is stable between launches.

Files on disk are **not** renamed. Sound event names in scripts stay the same,
because Minecraft lower-cases them anyway.

The project deliberately does not reference any Minecraft class from the
mixin. The target class is addressed by name, and the handler only uses
`String`, `File` and Mixin types. That keeps the mixin independent of MCP
mappings and makes it work with different Mappet 0.9 builds.

## Installation

1. Install Forge 1.12.2, Mappet 0.9 and MixinBooter 8.x.
2. Put `MappetSoundFix-1.0.0.jar` into the `mods` folder.
3. Start the game.

No other configuration is required.

## Configuration

The first launch creates `config/mappetsoundfix.cfg`:

| Option                  | Default | Description |
|-------------------------|---------|-------------|
| `caseInsensitiveLookup` | `true`  | Enable the case-insensitive fallback. Disable to get Mappet's original behaviour. |
| `logResolvedFiles`      | `false` | Log every sound found through the fallback (useful for diagnostics). |

The config is read during `preInit`; restart the game after changing it.

## Verifying

`logs/latest.log` should contain:

```
[MixinBooter]: Instantiating class kerilom.mappetsoundfix.LateMixinLoader for its mixins.
[MixinBooter]: Adding mixins.mappetsoundfix.json mixin configuration.
[MappetSoundFix] Case-insensitive lookup: enabled, resolved-file logging: disabled.
```

After the resource reload (game start or `F3+T`) there should be no more
`does not exist, cannot add it to event` warnings for sounds that exist on
disk. With `logResolvedFiles=true` the mapping is printed explicitly:

```
[MappetSoundFix] sounds/trs/loops/loop_servermid.ogg -> trs/loops/loop_serverMid.ogg
```

## Building

The project uses [RetroFuturaGradle](https://github.com/GTNewHorizons/RetroFuturaGradle),
the modern Gradle toolchain for legacy Minecraft versions. RFG provisions a
Java 8 toolchain automatically, so the host can run any recent JDK.

```bash
./gradlew build
```

The mod jar is written to `build/libs/MappetSoundFix-1.0.0.jar`.

Useful tasks:

```bash
./gradlew test      # resolver unit tests
```

`./gradlew runClient` starts a bare dev client. Mappet and MixinBooter are not
declared as runtime dependencies, add them to the `mods` folder of the generated
`run/` directory if you want to test the mixin in a dev client.

### Offline build

If the Gradle toolchain cannot download dependencies, `tools/build-offline.sh`
compiles the project with `javac --release 8` against jars from an existing
Forge 1.12.2 instance. It is a fallback, not the primary build:

```bash
INSTANCE=/path/to/instance ./tools/build-offline.sh
```

### Java 8 toolchain

RFG downloads a Java 8 toolchain through the Foojay resolver. If that is not
possible, install any JDK 8 and tell Gradle where it is, either in
`~/.gradle/gradle.properties`:

```properties
org.gradle.java.installations.paths=/path/to/jdk8
```

or by running `JAVA_HOME=/path/to/jdk8 ./gradlew build`.

## Project layout

```
src/main/java/kerilom/mappetsoundfix/
├── MappetSoundFix.java          entry point, Forge config
├── LateMixinLoader.java         MixinBooter ILateMixinLoader
├── CaseInsensitiveFiles.java    pure-Java path resolver
└── mixin/SoundPackMixin.java    the actual fix
src/main/resources/
├── mcmod.info
└── mixins.mappetsoundfix.json
src/test/java/...                JUnit tests for the resolver
```

## Compatibility

* Mappet 0.9.x is supported. The mixin needs the private
  `SoundPack.getFile(String)` method; if a future Mappet release changes it,
  the mixin will refuse to apply instead of silently doing nothing.
* MixinBooter 8.x or newer. The addon uses `ILateMixinLoader`, which is
  supported by all MixinBooter 8 releases.
* Client only: dedicated servers do not use the custom sound pack.

### Known limitations

* Two files that differ only by case (`A.ogg` and `a.ogg`) collapse into one
  sound event. This is a `ResourceLocation` limitation, not an addon bug.
* Files with an upper-case extension (`.OGG`) are ignored by Mappet before the
  addon sees them.

## License

MIT. See [LICENSE](LICENSE).

Русская версия: [README.ru.md](README.ru.md).
