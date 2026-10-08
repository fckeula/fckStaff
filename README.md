# fckStaff

A staff-mode plugin for **Paper** servers (API 26.2+), written in **Java 25** and built with **Gradle**.

fckStaff provides staff mode, vanish, freeze tools, staff/freeze chat, SQLite persistence, and a fully configurable setup.

## Features

- **Staff Mode** — saves the player's state, enables creative/flight, gives a configurable staff toolkit, and enables vanish.
- **Vanish** — independent `/vanish` command with permission-based visibility.
- **Freeze** — prevents movement, block interaction, commands and damage, with private freeze chat.
- **Staff Chat** — toggle or send one-off staff messages.
- **Persistence** — staff mode survives reconnects and server restarts using SQLite.
- **Hot Reload** — reload configuration and language files with `/fckstaff reload`.
- **Fully configurable** — commands, items, GUI, messages, language and behavior can be changed without modifying the code.

## Requirements

- **Paper 26.2+**
- **Java 25**
- No extra libraries need to be installed manually; Paper handles the SQLite dependency.

## Installation

1. Download the latest `.jar` from the [Releases](../../releases) page.
2. Put the JAR into your server's `plugins/` folder.
3. Start or restart the server.
4. Configure `plugins/fckStaff/config.yml` if needed.

## Commands

| Command | Default alias | Permission |
|---|---|---|
| `/staffmode` | `/sm`, `/staff` | `fckstaff.staffmode` |
| `/freeze <player>` | `/f` | `fckstaff.freeze` |
| `/freezechat <player> <message>` | `/fc` | `fckstaff.freezechat` |
| `/staffchat [message]` | `/sc` | `fckstaff.staffchat` |
| `/vanish` | `/v` | `fckstaff.vanish` |
| `/fckstaff reload` | `/zs` | `fckstaff.admin` |

Aliases can be changed or removed in `config.yml`.

## Permissions

| Permission | Description | Default |
|---|---|---|
| `fckstaff.admin` | Reload configuration | `op` |
| `fckstaff.staffmode` | Use staff mode | `op` |
| `fckstaff.freeze` | Freeze/unfreeze players | `op` |
| `fckstaff.freezechat` | Use and see freeze chat | `op` |
| `fckstaff.staffchat` | Use and see staff chat | `op` |
| `fckstaff.vanish` | Use vanish | `op` |
| `fckstaff.vanish.bypass` | Use vanish outside staff mode when restricted | `op` |
| `fckstaff.see` | See vanished players | `op` |

## Configuration

Main configuration is in `config.yml`.

- `language` — active language.
- `commands` — command aliases.
- `vanish.require-staffmode` — restrict `/vanish` to staff mode.
- `actionbar.interval-ticks` — action bar refresh interval.
- `freeze.*` — freeze reminders and disconnect behavior.
- `items` / `menu` — toolkit and GUI materials, slots, amounts, glow and model data.

Languages are stored in `langs/`. English and Spanish are included by default.

## Building

Clone the repository and use the included Gradle wrapper:

```bash
./gradlew build
```

On Windows:

```powershell
.\gradlew.bat build
```

The compiled plugin will be available in:

```text
build/libs/
```

The generated JAR can be copied directly to your server's `plugins/` directory.

## Releases

Releases are automated with GitHub Actions.

Create and push a version tag:

```bash
git tag v1.0.0
git push origin v1.0.0
```

The workflow will build the plugin and automatically create a GitHub Release with the generated JAR attached.

## License

See the repository license file for licensing information.
