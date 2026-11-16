# CourtCompanion

An app for basketball coaches to divide the players at practice into groups, quickly and fairly. Keep a player
database (skill 1–5, player types such as *Quick*, *Physical*, *Tall*, and pairs to keep apart), tick who actually
turned up, pick how to divide them and press **Generate**. It runs on Android and as a desktop app on Linux, built
from the same Kotlin code. Claude (or any other MCP client or script) can build the player database and try
divisions through the built-in MCP server and command line tool.

## Features

- **Attendance**: everyone starts as present; tap the few who are missing. Attendance is kept for the rest of the
  day, so the next round is one tap away.
- **Divide** into groups of *N* players or into *N* groups. When the players don't divide evenly, choose
  *One smaller group* (19 in groups of 4: 4·4·4·4·3) or *Bigger groups* (5·5·5·4). The resulting sizes are shown
  above the Generate button.
- **Goals**:
  - *Similar level*: players of the same skill together (drills). With *Consider player types*, also the same types
    together; skill always comes first.
  - *Even teams*: every group equally strong (games against each other). With *Consider player types*, the types
    are spread over the teams (e.g. one quick, one physical and one tall per team).
  - *Random*: just shuffle.
- **Randomness**: among equally good divisions one is picked at random, so equal players are mixed differently
  every time.
- **Mix partners**: pairs who were already together in earlier rounds today are avoided (can be switched off).
- **Keep apart**: saved pairs (under *Players*) and one-off pairs for today never share a group, if at all possible.
- **Results** as coloured group cards with average skill and type counts. *Re-roll* replaces the round with another
  equally good one; *Next round* keeps it and makes a new one; *Change* goes back to the request.
- **English and Swedish**, switchable in *Settings*.
- **Player file**: export the database as JSON and import it on another device (*Settings*). Claude can create
  this file for you; see [Using CourtCompanion from Claude](#using-courtcompanion-from-claude).

Planned: importing today's attendance from Svenskalag.

## How it works

The project uses Kotlin Multiplatform and Compose Multiplatform:

| Module | Contents |
| --- | --- |
| `shared` | Everything except the entry points. `src/commonMain` holds the logic and the UI; `src/androidMain` and `src/desktopMain` hold the few platform-specific pieces. |
| `app` | The Android app: just `MainActivity`, the manifest and resources. |
| `desktop` | The desktop app: just `main()`, which opens a window, or runs the command line tool when given arguments. |
| `cli` | The command line tool and MCP server (`courtcompanion-cli`), which work without a window. |

Packages in `shared/src/commonMain`:

| Package | Contents |
| --- | --- |
| `model` | Plain data classes: `Player`, `KeepApart` and `Roster` (also the export file format). |
| `grouping` | `GroupRequest`, `GroupSizes` (how many players per group) and `GroupingEngine`, the division algorithm. |
| `data` | `AppRepository`, storing the roster, settings and today's session as JSON files, and `RosterJson` for export/import. |
| `ui` | Compose screens and view models: practice (home), result, players, settings; `Strings` holds the English and Swedish texts. |

`GroupingEngine` gives every candidate division a cost: skill spread within groups (*Similar level*) or differences
in team strength (*Even teams*), type differences or type imbalance, a large penalty per keep-apart pair in a group,
and a smaller one per pair that was together earlier today. A local search (swapping players between groups) from
many random and sorted starts finds good divisions, and one of the near-best is picked at random.

`Platform` (in `shared/src/commonMain/kotlin/com/robert/courtcompanion/Platform.kt`) is the compatibility layer. It
provides storage locations and how to hand over an exported file (share sheet on Android, *Save as* on desktop), and
is implemented by `AndroidPlatform` and `DesktopPlatform`. `SystemBackHandler` and `rememberJsonFilePicker` are
`expect` declarations with one `actual` per platform.

Data is stored in app storage on Android and in `~/.local/share/courtcompanion` on desktop (shared with the command
line tool and the MCP server).

## Using CourtCompanion from Claude

CourtCompanion includes an MCP server, so Claude can build your player database and try divisions. It offers these
tools:

| Tool | What it does |
| --- | --- |
| `list_players` | Lists players, skill, types and keep-apart pairs. |
| `upsert_players`, `remove_players` | Add, change (by name) or remove players. |
| `set_keep_apart` | Add or remove saved keep-apart pairs. |
| `generate_groups` | Divides the players with the app's algorithm and returns the groups as text and an image. Changes nothing. |
| `export_roster` | Writes the player file (default `~/Documents/CourtCompanion/courtcompanion-players.json`) to import on the phone. |

Changes Claude makes show up in the desktop app. To get them onto the phone, let Claude export the player file,
send it to the phone (e.g. by email or Drive) and import it under *Settings → Import players*.

To connect it to the Claude desktop app, add a `courtcompanion` entry under `mcpServers` in
`~/.config/Claude/claude_desktop_config.json` and restart Claude. With the `.deb` package installed:

```json
{
  "mcpServers": {
    "courtcompanion": { "command": "/opt/courtcompanion/bin/courtcompanion", "args": ["mcp"] }
  }
}
```

Without installing, build the command line tool with `./gradlew :cli:installDist` and point Claude at it, with the
JDK Gradle uses:

```json
{
  "mcpServers": {
    "courtcompanion": {
      "command": "/path/to/CourtCompanion/cli/build/install/courtcompanion-cli/bin/courtcompanion-cli",
      "args": ["mcp"],
      "env": { "JAVA_HOME": "/path/to/jdk-21" }
    }
  }
}
```

Then ask Claude something like *"Here's my U13 team with skill levels from 1 to 5 … add them to CourtCompanion and
make the file for my phone"*.

### Command line

The same features are available from a terminal (`courtcompanion-cli` from `:cli:installDist`, or the installed
`/opt/courtcompanion/bin/courtcompanion` with the same arguments):

```bash
courtcompanion-cli list
```

```bash
courtcompanion-cli generate --size 4 --absent "Vera,Saga" -o groups.png
```

```bash
courtcompanion-cli generate --groups 3 --goal balanced --types
```

```bash
courtcompanion-cli export -o players.json
```

Run `courtcompanion-cli help` for all options.

## Building

Requirements: a JDK 17–23 for Gradle (JDK 21 recommended), plus Android Studio (Ladybug or newer) with Android
SDK 35 for the Android app. The Android app runs on Android 10 (API 29) and newer.

> Gradle 8.10 cannot run on Java 25, which newer Android Studio versions bundle. If sync fails with just
> `25.0.4` as the error, set *Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK* to a
> JDK 21. On the command line, point `JAVA_HOME` at a JDK 21.

1. Open the project folder in Android Studio and let Gradle sync.
2. Run the `app` configuration on a device or emulator.

From the command line:

```bash
./gradlew :app:assembleDebug
```

Unit tests run on the desktop JVM, without Android:

```bash
./gradlew :shared:desktopTest :cli:test :desktop:test
```

### Desktop

Run the desktop app:

```bash
./gradlew :desktop:run
```

Build an installable Ubuntu package (needs `sudo apt install binutils fakeroot` once):

```bash
./gradlew :desktop:packageDeb
```

The package is written to `desktop/build/compose/binaries/main/deb/`. Install it with
`sudo apt install ./desktop/build/compose/binaries/main/deb/courtcompanion_0.1.0-1_amd64.deb`; CourtCompanion then
appears in the app launcher.

## Releasing

Release builds are signed with the key described in `keystore.properties` in the project root. That file is
git-ignored; `keystore.properties.example` shows its format. Without it, release builds are unsigned.

> Back up the keystore file and `keystore.properties` somewhere safe (e.g. a password manager). Updates must be
> signed with the same key, or phones will refuse to install them over the previous version.

To publish a new version:

1. Increase `versionCode` (and update `versionName`) in `app/build.gradle.kts`, and `packageVersion` in
   `desktop/build.gradle.kts`.
2. Build the signed APK:

   ```bash
   ./gradlew assembleRelease
   ```

3. Share `app/build/outputs/apk/release/app-release.apk`, e.g. as a Google Drive link. Recipients open it on
   their phone and allow installing from that app when Android asks.
