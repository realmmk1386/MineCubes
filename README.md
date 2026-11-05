# MineCubes 🧊

[![Minecraft Versions](https://img.shields.io/badge/Minecraft-1.17%20--%201.21.x%20--%2026.2-blue?logo=minecraft)](https://papermc.io)
[![Paper / Folia](https://img.shields.io/badge/Platform-Paper%20%7C%20Folia%20%7C%20Spigot-brightgreen)](https://papermc.io)
[![Java](https://img.shields.io/badge/Java-21%2B-orange?logo=openjdk)](https://openjdk.org)
[![Version](https://img.shields.io/badge/Version-2.1.0-blueviolet)](https://github.com/realmmk1386/MineCubes/releases)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)
[![Discord](https://img.shields.io/badge/Discord-Join%20Community-5865F2?logo=discord&logoColor=white)](https://discord.gg/NxvQkMjDnR)
[![Donate](https://img.shields.io/badge/Donate-NOWPayments-yellow)](https://nowpayments.io/donation/mmk1386)

**MineCubes** is a modern, high-performance Minecraft 3D cube generator plugin designed for **Minecraft 1.17 through 1.21.x (1.21.0, 1.21.1, 1.21.2, 1.21.3, 1.21.4)** and modern server engines like **Paper, DivineMC, Leaf, and Folia 26.2**.

It allows server administrators and players to create, customize, place, and automate 3D cube generators with weighted block drop chances, virtual Double Chest storage, AutoMine automation, and integrated Auto Smelt.

---

## ✨ Features

- 🎲 **Customizable Templates:** Define custom cube generator templates in `cubegens.yml` or via in-game commands with custom sizes ($3 \times 3$, $5 \times 5$, $7 \times 7$), regeneration intervals, and types (`protect` with a bedrock wireframe or `normal`).
- 🖥️ **Interactive GUI Block Editor (`/mc template edit <name>`):**
  - Intuitive 54-slot inventory editor.
  - Drag and drop any blocks you want to generate (fully supports all blocks including modern 1.21 Tuff & Copper variants).
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
  - Sits strictly above ground level (`clickedSurface + 1`); picking it up leaves the underlying terrain completely intact without creating trenches.
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
| `/mc` or `/minecube` | `minecubes.use` | Displays the help menu. |
| `/mc list` | `minecubes.use` | Lists all your placed MineCubes and their coordinates. |
| `/mc unplace [template/id]` | `minecubes.unplace` | Picks up your MineCube and returns the item. |
| `/mc give <player> <template> [amount]` | `minecubes.give` | Gives a linked MineCube item to a player. |
| `/mc template list` | `minecubes.admin` | Lists all created templates. |
| `/mc template create <name> [size] [type] [regen]` | `minecubes.admin` | Creates a new template. |
| `/mc template edit <name>` | `minecubes.admin` | Opens the GUI block and percentage editor. |
| `/mc template linkitem <name>` | `minecubes.admin` | Links your held item to the template. |
| `/mc template setsize <name> <size>` | `minecubes.admin` | Sets the template size (odd number $\ge 3$). |
| `/mc template setregen <name> <seconds>` | `minecubes.admin` | Sets the regeneration interval in seconds. |
| `/mc template settype <name> <normal\|protect>` | `minecubes.admin` | Sets the generator frame type. |
| `/mc template remove <name>` | `minecubes.admin` | Removes an existing template. |
| `/mc reload` | `minecubes.admin` | Reloads configurations and templates. |

*Aliases:* `/mc`, `/minecubes`, `/minecube`, `/cg`, `/cubegen`

---

## 🔐 Permissions

| Permission | Default | Description |
|---|---|---|
| `minecubes.*` | `op` | Grants access to all MineCubes features. |
| `minecubes.use` | `true` | Allows using basic commands (`/mc`, `/mc list`). |
| `minecubes.place` | `true` | Allows placing a MineCube item in the world. |
| `minecubes.unplace` | `true` | Allows picking up your placed MineCube. |
| `minecubes.storage` | `op` | Allows opening the virtual Storage and toggling Storage Mode. |
| `minecubes.automine` | `op` | Allows enabling AutoMine automation. |
| `minecubes.autosmelt` | `op` | Allows enabling Auto Smelt during AutoMine. |
| `minecubes.give` | `op` | Allows giving MineCube items to players. |
| `minecubes.admin` | `op` | Administrative permission for templates and management. |

*(Legacy `cubegen.*` permission nodes are also fully supported for backwards compatibility).*

---

## ⚙️ Configuration Example (`cubegens.yml`)

```yaml
templates:
  default:
    name: "Default MineCube"
    size: 5
    type: "protect"
    regen: 10
    blocks:
      STONE: 70.0
      COBBLESTONE: 20.0
      COAL_ORE: 7.0
      IRON_ORE: 3.0

  diamond:
    name: "Diamond MineCube"
    size: 5
    type: "protect"
    regen: 8
    blocks:
      STONE: 50.0
      DEEPSLATE: 30.0
      DIAMOND_ORE: 15.0
      DEEPSLATE_DIAMOND_ORE: 5.0
```

---

## 💬 Community & Support

Join our official Discord community for support, updates, and feature requests:
- **Discord:** [https://discord.gg/NxvQkMjDnR](https://discord.gg/NxvQkMjDnR)

---

## 💡 Origin & Credits

The original concept of this plugin originated from [m0hammad6335/CubeGen](https://github.com/m0hammad6335/CubeGen). It has been completely rebuilt, modernized for modern Minecraft and Folia engines, and expanded by **mmk1386** with interactive GUI tools, AutoMine, Auto Smelt, virtual 54-slot storage, smart placement mechanics, and ladder climbing.

---

## ☕ Support & Donations

If you appreciate MineCubes and would like to support development:
- **NOWPayments (Crypto):** [https://nowpayments.io/donation/mmk1386](https://nowpayments.io/donation/mmk1386)

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).
