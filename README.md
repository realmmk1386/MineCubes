# MineCubes 🧊

[![Paper](https://img.shields.io/badge/Paper%20%2F%20Folia%20%2F%20Spigot-1.17%20--%2026.2-blue?logo=minecraft)](https://papermc.io)
[![Java](https://img.shields.io/badge/Java-21%2B-orange?logo=openjdk)](https://openjdk.org)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)
[![Discord](https://img.shields.io/badge/Discord-Join%20Community-5865F2?logo=discord&logoColor=white)](https://discord.gg/NxvQkMjDnR)
[![Donate](https://img.shields.io/badge/Donate-NOWPayments-yellow)](https://nowpayments.io/donation/mmk1386)

**MineCubes** is a modern, high-performance Minecraft generator plugin designed for **Paper / DivineMC / Leaf / Folia 26.2** while maintaining backward compatibility with **Minecraft 1.17 – 1.20+**.

It allows server administrators and players to create, customize, place, and automate 3D cube generators with weighted block drop chances, virtual Double Chest storage, AutoMine automation, and integrated Auto Smelt.

---

## ✨ Features

- 🎲 **Customizable Templates:** Define custom cube generator templates in `cubegens.yml` or via in-game commands with custom sizes ($3 \times 3$, $5 \times 5$, $7 \times 7$), regeneration intervals, and types (`protect` with a bedrock wireframe or `normal`).
- 🖥️ **Interactive GUI Block Editor (`/mc template edit <name>`):**
  - Intuitive 54-slot inventory editor.
  - Drag and drop any blocks you want to generate.
  - Shift + Left Click on any block to set its spawn chance via chat.
  - Live total percentage display with real-time validation (prevents saving if sum exceeds $100\%$).
  - One-click **Auto-Balance** button that mathematically scales chances to equal exactly $100\%$.
- 🔗 **Custom Item Linking:**
  - Hold any custom item (with custom models, lore, glowing effects, or NBT tags) and run `/mc template linkitem <name>`.
  - Deliver generators to players with `/mc give <player> <template> [amount]`.
  - Placing the item in the world creates the generator; picking it up returns the exact linked item.
- 🌱 **Smart Placement & Terrain Protection:**
  - Automatically detects solid ground beneath grass, tall flowers, ferns, or snow layers.
  - Clears foliage upon placement so the generator rests directly on solid ground without hovering.
  - Safely teleports the player outside the structure upon placement to prevent getting trapped.
  - Sits strictly above ground level (`clickedSurface + 1`); picking it up leaves the underlying terrain completely intact without creating square trenches.
- 🧗 **Climbable Corner Ladder:**
  - A built-in ladder attaches to the outer face of the bedrock corner pillar closest to the player.
  - Runs from ground level all the way to the top of the bedrock frame, allowing easy climbing.
  - Cleans up without physics drops when the generator is picked up.
- 📦 **Virtual Double Chest Storage (54 Slots):**
  - Built-in live-synced virtual storage accessible via the management menu.
  - Automatically deposits drops from AutoMine and manual mining in Storage Mode.
  - Safely drops all stored items at the owner's feet if the generator is picked up.
- 🤖 **AutoMine Automation:**
  - Automatically mines inner blocks one-by-one with realistic block break particles and sounds.
  - Directly stores drops into the generator's 54-slot virtual inventory.
- 🔥 **Auto Smelt:**
  - Directly smelts mined raw ores (Raw Iron, Raw Copper, Raw Gold, Ancient Debris, Cobblestone, Sand, Wood into Charcoal, etc.) into refined ingots and items before placing them into storage.
  - Requires active AutoMine and permission `minecubes.autosmelt`.
- 🧲 **Storage Mode (Manual Mining Redirection):**
  - When enabled, manual mining inside the cube redirects drops directly into the generator's storage instead of player inventory.
- 🛑 **Automatic Full Storage Protection:**
  - Automatically disables AutoMine and Storage Mode when the storage is 100% full to prevent loot loss.
  - Sends a clear notification and sound warning to the owner.
- 🔒 **Strict Owner Protection:**
  - Only the player who placed the cube (or server administrators with `minecubes.admin`) can open the settings GUI, toggle AutoMine/Storage Mode/Auto Smelt, open the storage, or pick it up.
  - Non-owners are locked out with visual/audio feedback.

---

## 🕹️ Commands

| Command | Permission | Description |
|---|---|---|
| `/mc` or `/minecubes` | `minecubes.use` | Displays the help menu. |
| `/mc list` | `minecubes.use` | Lists all your placed CubeGens and their coordinates. |
| `/mc unplace [template/id]` | `minecubes.unplace` | Picks up your CubeGen and returns the item. |
| `/mc give <player> <template> [amount]` | `minecubes.give` | Gives a linked CubeGen item to a player. |
| `/mc template list` | `minecubes.admin` | Lists all created templates. |
| `/mc template create <name> [size] [type] [regen]` | `minecubes.admin` | Creates a new template. |
| `/mc template edit <name>` | `minecubes.admin` | Opens the GUI block and percentage editor. |
| `/mc template linkitem <name>` | `minecubes.admin` | Links your held item to the template. |
| `/mc template setsize <name> <size>` | `minecubes.admin` | Sets the template size (odd number $\ge 3$). |
| `/mc template setregen <name> <seconds>` | `minecubes.admin` | Sets the regeneration interval in seconds. |
| `/mc template settype <name> <normal\|protect>` | `minecubes.admin` | Sets the generator frame type. |
| `/mc template remove <name>` | `minecubes.admin` | Removes an existing template. |
| `/mc reload` | `minecubes.admin` | Reloads configurations and templates. |

*Aliases:* `/mc`, `/minecubes`, `/cg`, `/cubegen`

---

## 🔐 Permissions

| Permission | Default | Description |
|---|---|---|
| `minecubes.*` | `op` | Grants access to all MineCubes features. |
| `minecubes.use` | `true` | Allows using basic commands (`/mc`, `/mc list`). |
| `minecubes.place` | `true` | Allows placing a CubeGen item in the world. |
| `minecubes.unplace` | `true` | Allows picking up your placed CubeGen. |
| `minecubes.storage` | `op` | Allows opening the virtual Storage and toggling Storage Mode. |
| `minecubes.automine` | `op` | Allows enabling AutoMine automation. |
| `minecubes.autosmelt` | `op` | Allows enabling Auto Smelt during AutoMine. |
| `minecubes.give` | `op` | Allows giving CubeGen items to players. |
| `minecubes.admin` | `op` | Administrative permission for templates and management. |

*(Legacy `cubegen.*` permission nodes are also fully supported for backwards compatibility).*

---

## ⚙️ Configuration Example (`cubegens.yml`)

```yaml
templates:
  vip_miner:
    displayName: "&6&lVIP Ore CubeGen"
    size: 5
    type: "protect"
    regenInterval: 8
    blocks:
      DIAMOND_ORE: 10.0
      EMERALD_ORE: 5.0
      ANCIENT_DEBRIS: 2.0
      IRON_ORE: 35.0
      GOLD_ORE: 20.0
      STONE: 28.0
```

---

## 🛠️ Building from Source

Requirements:
- JDK 21 or newer (JDK 26 supported)
- Python 3.x (for automated builds) or Maven 3.9+

To build:
```bash
python build.py
```
The output JAR will be generated in `target/MineCubes-2.0.0.jar`.

---

## 💬 Community & Support

Have questions, suggestions, or need help?
- **Discord:** [Join our Discord Server](https://discord.gg/NxvQkMjDnR)

If you enjoy using MineCubes and want to support ongoing development:
- **Donation (Crypto / Global):** [NOWPayments Donation](https://nowpayments.io/donation/mmk1386)

---

## 💡 Origin & Credits

The original concept and base foundation of this project originated from [m0hammad6335/CubeGen](https://github.com/m0hammad6335/CubeGen). It has been completely redesigned, modernized for Paper 26.2, and expanded by **mmk1386** with full GUI systems, AutoMine, Auto Smelt, virtual double chest storage, and advanced smart placement mechanics into **MineCubes**.

---

## 📜 License

This project is licensed under the [MIT License](LICENSE).
