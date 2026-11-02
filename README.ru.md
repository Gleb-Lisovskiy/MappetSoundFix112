# Mappet Sound Fix

Клиентский аддон для **Mappet 0.9 на Minecraft 1.12.2 (Forge)**, который
исправляет неполную загрузку пользовательских звуков на регистрозависимых
файловых системах (Linux, macOS).

* **Minecraft:** 1.12.2
* **Загрузчик:** Forge
* **Mappet:** 0.9.x
* **MixinBooter:** 8.x
* **Сторона:** только клиент

## В чём проблема

Mappet превращает каждый `.ogg` из `config/mappet/sounds` в звуковое событие.
В сгенерированном `sounds.json` сохраняется настоящее имя файла:

```json
"trs.loops.loop_serverMid": {
  "sounds": ["mp.sounds:trs/loops/loop_serverMid"]
}
```

Minecraft хранит пути ресурсов в нижнем регистре, поэтому при загрузке звука
игра запрашивает:

```
sounds/trs/loops/loop_servermid.ogg
```

Mappet 0.9 ищет файл так:

```java
private File getFile(String path)
{
    return new File(this.folder, path.substring(7));
}
```

На Windows NTFS регистронезависима, и `loop_servermid.ogg` находит
`loop_serverMid.ogg`. На Linux ext4 — нет, и звук пропускается:

```
File mp.sounds:sounds/trs/loops/loop_servermid.ogg does not exist, cannot add it to event mp.sounds:trs.loops.loop_servermid
```

Поэтому из набора пропадают ровно те файлы, в именах которых есть заглавные
буквы, а все файлы в нижнем регистре продолжают работать.

## Как это работает

`SoundPackMixin` встраивается в `mchorse.mappet.client.SoundPack.getFile` в
точке `RETURN`:

1. Если Mappet нашёл файл по точному пути — ничего не меняется.
2. Если файла нет, путь ищется внутри `config/mappet/sounds`
   **без учёта регистра, по каждому компоненту пути**.
3. Если несколько файлов различаются только регистром, выбирается
   лексикографически меньший — результат стабилен между запусками.

Файлы на диске **не переименовываются**. Имена событий в скриптах остаются
прежними, потому что Minecraft всё равно приводит их к нижнему регистру.

Миксин намеренно не ссылается ни на один класс Minecraft: целевой класс
указан строкой, а обработчик использует только `String`, `File` и типы Mixin.
Поэтому фикс не зависит от MCP-маппингов и переживает разные сборки Mappet 0.9.

## Установка

1. Установить Forge 1.12.2, Mappet 0.9 и MixinBooter 8.x.
2. Положить `MappetSoundFix-1.0.0.jar` в папку `mods`.
3. Запустить игру.

Больше ничего настраивать не нужно.

## Конфигурация

При первом запуске создаётся `config/mappetsoundfix.cfg`:

| Опция                   | По умолчанию | Описание |
|-------------------------|--------------|----------|
| `caseInsensitiveLookup` | `true`       | Включает регистронезависимый поиск. Отключите, чтобы вернуть оригинальное поведение Mappet. |
| `logResolvedFiles`      | `false`      | Логировать каждый звук, найденный через fallback (для диагностики). |

Конфиг читается в `preInit`; после изменения перезапустите игру.

## Проверка

В `logs/latest.log` должны появиться строки:

```
[MixinBooter]: Instantiating class kerilom.mappetsoundfix.LateMixinLoader for its mixins.
[MixinBooter]: Adding mixins.mappetsoundfix.json mixin configuration.
[MappetSoundFix] Case-insensitive lookup: enabled, resolved-file logging: disabled.
```

После перезагрузки ресурсов (старт мира или `F3+T`) предупреждений
`does not exist, cannot add it to event` для существующих на диске звуков быть
не должно. При `logResolvedFiles=true` в лог пишется точное соответствие:

```
[MappetSoundFix] sounds/trs/loops/loop_servermid.ogg -> trs/loops/loop_serverMid.ogg
```

## Сборка

Проект использует
[RetroFuturaGradle](https://github.com/GTNewHorizons/RetroFuturaGradle) —
современный Gradle-тулчейн для старых версий Minecraft. RFG сам скачивает
Java 8 toolchain, поэтому хост может работать на любой свежей JDK.

```bash
./gradlew build
```

Готовый jar: `build/libs/MappetSoundFix-1.0.0.jar`.

Полезные задачи:

```bash
./gradlew test      # юнит-тесты резолвера
```

`./gradlew runClient` запускает пустой dev-клиент. Mappet и MixinBooter не
объявлены как runtime-зависимости — положите их в папку `mods` созданного
каталога `run/`, если хотите проверить миксин в dev-клиенте.

### Сборка без сети

Если Gradle не может скачать зависимости, `tools/build-offline.sh` компилирует
проект через `javac --release 8` против jar'ов из уже установленного инстанса
Forge 1.12.2. Это запасной вариант, а не основной билд:

```bash
INSTANCE=/path/to/instance ./tools/build-offline.sh
```

### Java 8 toolchain

RFG скачивает Java 8 toolchain через резолвер Foojay. Если это невозможно,
установите любой JDK 8 и укажите его Gradle — либо в
`~/.gradle/gradle.properties`:

```properties
org.gradle.java.installations.paths=/path/to/jdk8
```

либо запуском `JAVA_HOME=/path/to/jdk8 ./gradlew build`.

## Структура

```
src/main/java/kerilom/mappetsoundfix/
├── MappetSoundFix.java          точка входа, конфиг Forge
├── LateMixinLoader.java         MixinBooter ILateMixinLoader
├── CaseInsensitiveFiles.java    чистый Java-резолвер путей
└── mixin/SoundPackMixin.java    сам фикс
src/main/resources/
├── mcmod.info
└── mixins.mappetsoundfix.json
src/test/java/...                JUnit-тесты резолвера
```

## Совместимость

* Поддерживается Mappet 0.9.x. Миксину нужен приватный метод
  `SoundPack.getFile(String)`; если в будущем Mappet его изменит, миксин
  откажется применяться, а не будет молча ничего делать.
* MixinBooter 8.x или новее: используется `ILateMixinLoader`, который есть во
  всех релизах MixinBooter 8.
* Только клиент: на выделенном сервере пользовательский sound pack не нужен.

### Ограничения

* Два файла, различающиеся только регистром (`A.ogg` и `a.ogg`), схлопнутся в
  одно событие. Это ограничение `ResourceLocation`, а не аддона.
* Файлы с расширением в верхнем регистре (`.OGG`) Mappet игнорирует ещё до
  того, как их увидит аддон.

## Лицензия

MIT. См. [LICENSE](LICENSE).

English version: [README.md](README.md).
