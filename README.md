# BepHax Meteor Addon

[![GitHub Release](https://img.shields.io/github/v/release/dekrom/BepHaxAddon?include_prereleases&label=Latest%20Release)](https://github.com/dekrom/BepHaxAddon/releases)
[![GitHub License](https://img.shields.io/github/license/dekrom/BepHaxAddon)](https://github.com/dekrom/BepHaxAddon/blob/main/LICENSE)
[![GitHub Issues](https://img.shields.io/github/issues/dekrom/BepHaxAddon)](https://github.com/dekrom/BepHaxAddon/issues)
[![Discord](https://img.shields.io/discord/658535415548084245?color=7289da&label=Discord&logo=discord&logoColor=white)](https://discord.gg/EGEhHNSkV8)

A comprehensive Meteor Client addon with **86+ modules** designed for 2b2t.org and anarchy servers. Combines community favorites with custom enhancements, advanced combat features, automated stash hunting, and extensive quality-of-life improvements.

## Installation

1. Download the latest JAR from [Releases](https://github.com/dekrom/BepHaxAddon/releases)
2. Place in `~/.minecraft/mods` folder
3. Launch Minecraft with Fabric and Meteor Client
4. Access modules via Meteor GUI (Right Shift)

### Requirements

| Dependency | Version | Notes                             |
|------------|---------|-----------------------------------|
| [Minecraft](https://minecraft.net/) | 1.21.4 | Required                          |
| [Fabric Loader](https://fabricmc.net/) | 0.16.10+ | Required                          |
| [Fabric API](https://modrinth.com/mod/fabric-api) | 0.119.3+ | Required                          |
| [Meteor Client](https://meteorclient.com) | 1.21.4-SNAPSHOT | Required                          |
| [Baritone](https://github.com/cabaletta/baritone) | 1.21.4-SNAPSHOT | Required                          |
| [ViaFabricPlus](https://modrinth.com/mod/viafabricplus) | Latest | **Required for silent rotations** |
| [XaeroMinimap](https://modrinth.com/mod/xaeros-minimap) | 25.2.10+ | Required for map features         |
| [XaeroWorldMap](https://modrinth.com/mod/xaeros-world-map) | 1.39.12+ | Required for map features         |
| [XaeroPlus](https://github.com/rfresh2/XaeroPlus) | 2.28.1+ | Required for enhanced map tools   |

**2b2t Anti-Cheat Bypass:** Connect using **protocol 1.20.4-1.20.6** via ViaFabricPlus for silent rotation modules (Phase, Criticals, Surround, GrimScaffold) and enhanced Meteor mixins (CrystalAura, AutoTrap) to work properly.

## Features

**85+ modules** organized into three categories, **13 commands**, **6 HUD elements**, and **56 mixins** enhancing Meteor Client functionality.

### 🎯 BepHax Category (Original Modules)

<details>
<summary><b>Combat & PVP</b> (5 modules)</summary>

- **BepCrystal** - Enhanced crystal aura (cooperation with jaxui)
- **Criticals** - Forces critical hits (PACKET/GRIM/GRIM_V3 modes, silent rotations)
- **Phase** - Phase through blocks (Pearl/Clip/Normal/Sand/Climb modes)
- **Surround** - Auto obsidian surround (head cover, GrimAirPlace support)
- **PVPModule** - Base framework for PVP modules with rotation management
</details>

<details>
<summary><b>Mining & Resources</b> (3 modules)</summary>

- **BepMine** - Advanced speedmine with queue system
- **MineESP** - Highlights blocks being mined by others
</details>

<details>
<summary><b>Inventory & Items</b> (5 modules)</summary>

- **ShulkerOverviewModule** - Mini icons showing common items in shulkers
- **ItemSearchBar** - Search inventory for items
- **InvFix** - Fixes 2b2t inventory issues
- **Replenish** - Auto-refill items from inventory
- **ElytraSwap** - Auto-swap elytras at low durability (combat protection mode)
- **PearlLoader** - Pearl loading/unloading management
</details>

<details>
<summary><b>Stash Management</b> (3 modules)</summary>

- **StashMover** - Advanced item transfer with pearl loading (`.stashmover` commands)
- **StashMoverSelectionHandler** - Area selection for StashMover
- **BetterStashFinder** - Enhanced stash location finder
</details>

<details>
<summary><b>Automation & Utilities</b> (6 modules)</summary>

- **AutoBreed** - Animal breeding automation
- **AutoEXPPlus** - Experience mending automation
- **AutoPortal** - Portal creation automation
- **AutoRespond** - Automated response system
- **UnfocusedFpsLimiter** - Limits FPS when window unfocused
- **WheelPicker** - Random selection wheel
</details>

<details>
<summary><b>Navigation & Movement</b> (6 modules)</summary>

- **GotoPosition** - Navigate to coordinates
- **TrailFollower** - Follow player trails
- **TrailMaker** - Plot and follow chunk highlights on Xaero's map
- **Pitch40Util** - 40-degree pitch mining utility
- **GrimScaffold** - Advanced scaffold with Grim support (tower mode)
- **YawLock** - Locks player yaw for precise movement
</details>

<details>
<summary><b>Visual & Other</b> (4 modules)</summary>

- **SignRender** - Renders sign text through walls with clustering
- **GhostMode** - Continue playing after death
- **NoHurtCam** - Removes hurt camera shake
- **WebChat** - Display chat in browser
</details>

### 🔍 JEFF Stash Hunting Category

*Integrated from [JEFF Stash Hunting](https://github.com/miles352/meteor-stashhunting-addon) by miles352*

<details>
<summary><b>Stash Hunting Modules</b> (8 modules)</summary>

- **SearchArea** - Automated chunk loading system (Rectangle/Spiral modes, save/load support)
- **ChestIndex** - Index and search chest contents (Stash Logger)
- **AFKVanillaFly** - AFK flying without mods using fireworks
- **NoJumpDelay** - Removes jump delay
- **GrimAirPlace** - Grim-compatible air placement
- **HighlightOldLava** - Highlights ancient lava sources
- **VanityESP** - Custom entity ESP for vanity items (item frames with maps, banners)
- **ElytraFlyPlusPlus** - Enhanced elytra flight controls with auto firework
- **OldChunkNotifier** - Notifies when entering old chunks
</details>

### ⭐ Stardust Category

*Integrated from [Stardust](https://github.com/0xTas/stardust) by 0xTas*

<details>
<summary><b>Stardust Modules</b> (25+ modules)</summary>

**Automation & Crafting:**
- **AutoSmith** - Smithing table upgrade automation (netherite upgrades, armor trimming)
- **AutoDoors** - Auto door interaction (classic/spammer modes)
- **AutoMason** - Stonecutter/masonry automation
- **AutoDyeShulkers** - Automated shulker dyeing
- **AutoDrawDistance** - FPS-based render distance adjustment
- **AutoCraft** - Automated crafting recipes (from Meteor Rejects)

**Combat & Movement:**
- **RocketJump** - Firework rocket jumping mechanics
- **RocketMan** - Rocket enhancements
- **RapidFire** - Fast projectile firing
- **Updraft** - Updraft flying mechanics

**Tools & Utilities:**
- **Archaeology** - Suspicious sand/gravel automation with ESP
- **AxolotlTools** - Axolotl-related tools
- **Grinder** - Automated grinding utilities
- **LoreLocator** - Locates items with specific lore
- **StashBrander** - Brand stashes with custom messages
- **WaxAura** - Automatic waxing of copper blocks
- **RoadTrip** - Travel utilities
- **PagePirate** - Pirates books from other players, displays contents
- **Stripper** - Auto log stripping
- **Loadouts** - Equipment loadout management

**Visual & Creative:**
- **ChatSigns** - Create signs in chat
- **SignHistorian** - Track sign history
- **SignatureSign** - Sign signatures
- **BookTools** - Advanced book editing tools
- **BannerData** - Display banner information

**Misc & QoL:**
- **Honker** - Custom goat horn sounds
- **MusicTweaks** - Music system tweaks
- **AntiToS** - Bypass restrictions
- **AdBlocker** - Blocks advertisements
- **DisconnectSound** - Play sound on disconnect (from Meteorist)
</details>

### 📦 Other Integrations

<details>
<summary><b>Community Modules</b> (3 modules)</summary>

- **KillEffects** - Visual/audio effects on entity death (from INDICA)
- **MapDuplicator** - Auto duplicates filled maps (from INDICA)
- **RespawnPointBlocker** - Prevents setting respawn points (from INDICA)
- **DiscordNotifs** - Discord webhook integration for game events
</details>

### 📊 HUD Elements (6 total)

- **ItemCounterHud** - Selected blocks and inventory counts
- **EntityList** - Entity tracker (players, mobs, items, projectiles, 2D/3D distance)
- **SpeedKMH** - Current speed in km/h
- **DimensionCoords** - Coordinates in both dimensions
- **DubCounterHud** - Container counter for looting
- **MobRateHud** - Mob farm performance analyzer

### 🔧 Commands (13 total)

| Command | Description |
|---------|-------------|
| `.center` | Center player positioning |
| `.coords` | Coordinate utilities |
| `.enemy` | Enemy tracking management |
| `.firstseen` | Check first seen on 2b2t.org |
| `.lastseen` | Check last seen on 2b2t.org |
| `.loadout` | Manage equipment loadouts |
| `.panorama` | Custom panorama utilities |
| `.playtime` | Check playtime on 2b2t.org |
| `.stats` | Display player stats on 2b2t.org |
| `.stashmover input` | Set stash input area |
| `.stashmover output` | Set stash output area |
| `.stashmover status` | Display stash mover status |
| `.stashmover clear` | Clear stash area settings |

## 🔧 Meteor Client Enhancements

**56 custom mixins** enhance Meteor Client modules with advanced features:

**Combat Mixins** - CrystalAura, AutoTrap, KillAura (yaw stepping, silent rotations, anti-cheat bypass), Velocity (GRIM/GRIM_V3 modes)

**Inventory Mixins** - AutoEat, ExpThrower (silent slot swapping via InventoryManager)

**Utility Mixins** - AutoLog, FreeCam, AutoMend, Timer, Nametags, NoRender, Notifier, PacketCanceller, OnlinePlayers, NoSlow, PeekScreen

## Quick Start Guide

<details>
<summary><b>StashMover</b> - Automated item transfer</summary>

1. `.stashmover input` - Select input area
2. `.stashmover output` - Select output area
3. Configure pearl positions in settings
4. Enable "Only Shulkers" to filter items
5. Toggle module to start transfer
</details>

<details>
<summary><b>SearchArea</b> - Chunk loading for stash hunting</summary>

1. Select mode (Rectangle/Spiral)
2. Set start/end positions (Rectangle mode)
3. Configure path gap
4. Optional: Set save-name for persistence
5. Enable module to begin scanning
</details>

<details>
<summary><b>TrailMaker</b> - Map-based navigation</summary>

1. Enable module and start recording
2. Add chunk highlights on Xaero's map
3. Stop recording
4. Click "Start Following" to navigate
5. Highlights auto-remove as you reach them
</details>

<details>
<summary><b>DiscordNotifs</b> - Discord integration</summary>

1. Create Discord webhook
2. Enter webhook URL in settings
3. Configure event filters
4. Enable module
</details>

## Credits

BepHax integrates modules from talented community developers:

- **[Stardust](https://github.com/0xTas/stardust)** by [0xTas](https://github.com/0xTas) - Utility modules, 2b2t API commands, automation
- **[JEFF Stash Hunting](https://github.com/miles352/meteor-stashhunting-addon)** by [miles352](https://github.com/miles352) - Stash hunting and navigation
- **[Meteor Rejects](https://github.com/AntiCope/meteor-rejects)** by [AntiCope](https://github.com/AntiCope) - AutoCraft
- **[INDICA](https://github.com/Faye-one/INDICA)** by [Faye-one](https://github.com/Faye-one) - KillEffects, MapDuplicator
- **[Meteorist](https://github.com/Zgoly/Meteorist)** by [Zgoly](https://github.com/Zgoly) - DisconnectSound
- **[HIGTools](https://github.com/RedCarlos26/HIGTools)** by [RedCarlos26](https://github.com/RedCarlos26) - Center, Coordinates commands
- **[jaxui](https://github.com/jaxui)** - BepCrystal module cooperation
- **[dekrom](https://github.com/dekrom)** - Project maintainer, original modules, integration

## Contributing

Open an [issue](https://github.com/dekrom/BepHaxAddon/issues) or submit a pull request. Community contributions welcome!

## Community

- **Discord**: [discord.gg/EGEhHNSkV8](https://discord.gg/EGEhHNSkV8)
- **GitHub**: [github.com/dekrom/BepHaxAddon](https://github.com/dekrom/BepHaxAddon)

## License

[GNU GPLv3](LICENSE) - Free to fork and modify.

## Disclaimer

Designed for anarchy servers like 2b2t.org. Use responsibly.
