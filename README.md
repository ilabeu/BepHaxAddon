# BepHax Meteor Addon

[![GitHub Release](https://img.shields.io/github/v/release/dekrom/BepHaxAddon?include_prereleases&label=Latest%20Release)](https://github.com/dekrom/BepHaxAddon/releases)
[![GitHub License](https://img.shields.io/github/license/dekrom/BepHaxAddon)](https://github.com/dekrom/BepHaxAddon/blob/main/LICENSE)
[![GitHub Issues](https://img.shields.io/github/issues/dekrom/BepHaxAddon)](https://github.com/dekrom/BepHaxAddon/issues)

[![Discord](https://img.shields.io/discord/658535415548084245?color=7289da&label=Discord&logo=discord&logoColor=white)](https://discord.gg/EGEhHNSkV8)
[![Twitch](https://img.shields.io/twitch/status/dekrom?color=9146FF&label=Twitch&logo=twitch&logoColor=white)](https://www.twitch.tv/dekrom)
[![YouTube](https://img.shields.io/badge/YouTube-dekrom-red?logo=youtube&logoColor=white)](https://www.youtube.com/@dekrom)

## What is BepHax?

BepHax is a comprehensive Meteor Client addon specifically designed for 2b2t.org players. It combines the best utility modules from various community addons into one optimized package, with custom enhancements and original modules built for the 2b2t anarchy server environment.

**Key Focus Areas:**
- Advanced combat with silent rotations
- Automated stash management and hunting
- Quality of life improvements for 2b2t
- Enhanced ESP and visual features
- Extensive automation capabilities

## Installation

1. Download the latest JAR from the [Releases](https://github.com/dekrom/BepHaxAddon/releases) page.
2. Place the JAR file in your Minecraft mods folder (typically `~/.minecraft/mods` on Fabric Loader).
3. Launch Minecraft with Fabric and Meteor Client installed.
4. Activate modules via the Meteor Client GUI (default key: Right Shift).

### Requirements

- [Minecraft 1.21.4](https://minecraft.net/)
- [Fabric Loader 0.16.10 or compatible](https://fabricmc.net/)
- [Meteor Client (latest snapshot for 1.21.4)](https://github.com/MeteorDevelopment/meteor-client/tree/a96efdcdd60ed226650f6fc7f952ba65371bfc4d)
- [Baritone API](https://github.com/cabaletta/baritone/tree/1.21.4)
- [ViaFabricPlus](https://modrinth.com/mod/viafabricplus) - **Required for silent rotation modules**
- [XaeroMinimap](https://modrinth.com/mod/xaeros-minimap)
- [XaeroWorldMap](https://modrinth.com/mod/xaeros-world-map)
- [XaeroPlus](https://github.com/rfresh2/XaeroPlus)

**Important Note for 2b2t:** For modules with silent rotations (Phase, Criticals, Surround, GrimScaffold) and enhanced Meteor modules (CrystalAura, AutoTrap, KillAura via mixins) to work properly, you must connect to 2b2t.org using **Minecraft protocol 1.20.4 - 1.20.6** through ViaFabricPlus. This ensures proper compatibility with the server's anti-cheat systems.

---

## Features

BepHax organizes modules into custom categories optimized for different gameplay aspects:

### 🎯 Original BepHax Modules (CATEGORY)

#### Combat & PVP
- **Criticals** - Forces critical hits with multiple bypass modes (requires ViaFabricPlus with protocol 1.20.4-1.20.6)
  - PACKET, GRIM, GRIM_V3 modes for anti-cheat bypass
  - Jump height customization
  - Silent rotations for server-side compatibility
- **Phase** - Phase through blocks with anti-cheat bypass (requires ViaFabricPlus)
  - Pearl phasing mode for teleporting through blocks
  - Clip mode for precise positioning
  - Normal, Sand, and Climb modes
  - Self-fill and attack crystal options
- **Surround** - Automatically surrounds feet with obsidian
  - Head level and cover head options
  - Mine extend for damaged blocks
  - Attack crystals in the way
  - GrimAirPlace exploit support
- **PVPModule** - Base framework for PVP modules with centralized rotation management

#### Mining & Resources
- **BepMine** - Advanced speedmine optimized for 2b2t with queue system
  - Toggleable instant mining keybind
  - Visual and chat notifications
  - Anti-cheat compatible timing
  - Smart tool selection
- **MineESP** - Highlights blocks being mined by other players

#### Inventory & Items
- **ShulkerOverviewModule** - Shows most common item as mini icon on shulkers
  - Works in inventory, containers, and hotbar
  - Configurable icon size and position
  - Multiple item indicator
- **ItemSearchBar** - Search through inventories for specific items
- **InvFix** - Fixes inventory issues specific to 2b2t server
- **Replenish** - Auto-refills items from inventory
- **ElytraSwap** - Auto-swaps elytras at low durability
  - Configurable durability threshold
  - Only while flying option
  - Combat protection mode (swap to chestplate when hit)
  - Auto swap back after protection duration
  - Remembers original item positions

#### Automation & Utilities
- **StashMover** - Advanced item transfer system with pearl loading
  - Input/Output area selection
  - Only Shulkers mode - filters non-shulker items
  - Break Empty - breaks empty containers
  - Fill Enderchest - efficient enderchest usage
  - Pearl loading between areas
  - Multi-axis pearl throwing support
  - Automatic container detection
  - Commands: `.stashmover input`, `.stashmover output`, `.stashmover status`, `.stashmover clear`
- **PearlLoader** - Manages ender pearl loading/unloading
- **WheelPicker** - Random selection wheel for various actions
- **AutoRegear** - Automated regearing system
  - Automatically creates platform
  - Restocks rockets and elytras from ender chest
  - Smart inventory management
- **AutoBreed** - Automated animal breeding
- **Stripper** - Automatically strips logs

#### Visual & ESP
- **SignRender** - Renders sign text through walls with clustering
  - Wall rendering with opacity
  - Sign clustering for performance
  - Distance-based rendering

#### Utilities
- **GhostMode** - Continue playing after death
  - Maintains configurable health value
  - Blocks death packets option
  - Prevents phasing through world
- **NoHurtCam** - Removes hurt camera shake
- **WebChat** - Displays chat in web browser

#### From Other Addons (CATEGORY)
- **KillEffects** - Visual and audio effects on entity death (from INDICA)
- **MapDuplicator** - Automatically duplicates filled maps (from INDICA)
- **RespawnPointBlocker** - Prevents setting respawn points (from INDICA)
- **AutoCraft** - Automates crafting recipes (from Meteor Rejects)
- **BookTools** - Advanced book editing tools (from Stardust)

---

### 🔍 Stash Hunting Modules (STASH)

*Modules from [JEFF Stash Hunting](https://github.com/miles352/meteor-stashhunting-addon) by miles352 and BepHax originals*

#### Search & Detection
- **BetterStashFinder** - Improved stash location finder
- **OldChunkNotifier** - Notifies when entering old chunks
- **ChestIndex** - Indexes and searches chest contents
- **SearchArea** - Automated chunk loading system for map scanning
  - Rectangle and spiral modes
  - Configurable path gaps
  - Save/load support
  - Position memory for resuming

#### Movement & Navigation
- **ElytraFlyPlusPlus** - Enhanced elytra flight controls
- **AFKVanillaFly** - AFK flying without mods
- **NoJumpDelay** - Removes delay between jumps
- **GotoPosition** - Navigate to specified coordinates
- **Pitch40Util** - Utility for 40-degree pitch mining
- **GrimAirPlace** - Grim-compatible air placement
- **TrailFollower** - Follows player trails
- **TrailMaker** - Plot and follow chunk highlights on Xaero's map for navigation
  - Record waypoints on map
  - Automatic pathfinding between highlights
  - Removes highlights as you reach them
- **YawLock** - Locks player yaw for precise movement
  - Jitter option for randomization
  - Configurable lock angle
- **GrimScaffold** - Advanced scaffold with Grim anti-cheat compatibility
  - Tower mode for vertical building
  - Customizable block placement
  - Speed settings
  - Rotation support

#### Automation
- **AutoPortal** - Automatic portal creation
- **AutoEXPPlus** - Automates experience mending
- **UnfocusedFpsLimiter** - Limits FPS when window unfocused
  - Saves resources during AFK
  - Configurable FPS limit

#### Visual & ESP
- **HighlightOldLava** - Highlights ancient lava sources
- **VanityESP** - Custom entity ESP for vanity items

#### Communication & Integration
- **DiscordNotifs** - Discord webhook integration for game events
  - Message logging
  - Player tracking
  - Death notifications
  - Configurable event filters

---

### ⭐ Utility Modules (STARDUST)

*Modules from [Stardust](https://github.com/0xTas/stardust) by 0xTas*

#### Commands
- **Stats2b2t** - Display player stats on 2b2t.org
- **LastSeen2b2t** - Check last seen date on 2b2t
- **FirstSeen2b2t** - Check first seen date on 2b2t
- **Playtime2b2t** - Check playtime on 2b2t
- **Panorama** - Custom panorama utilities
- **Loadout** - Manage equipment loadouts
- **Center** - Center positioning command (from HIGTools)
- **Coordinates** - Coordinate utilities (from HIGTools)

#### Creative Tools
- **ChatSigns** - Create signs in chat
- **SignHistorian** - Track sign history
- **SignatureSign** - Sign signatures
- **BannerData** - Display banner data
- **PagePirate** - Page copying utilities

#### Automation & Crafting
- **AutoSmith** - Automates smithing table upgrades
  - Automatic netherite upgrades
  - Item filtering
  - Batch processing support
- **AutoDoors** - Automatically interact with doors
  - Classic mode for single interactions
  - Spammer mode for rapid opening/closing
- **AutoMason** - Automates stonecutter/masonry operations
  - Batch processing support
  - Configurable item filters
- **AutoDrawDistance** - Adjusts render distance automatically based on FPS
  - FPS target configuration (30-240)
  - Minimum/maximum render distance limits
  - Sweet spot detection for optimal performance
  - Verbose debug mode with FPS reporting
- **AutoDyeShulkers** - Automatically dyes shulkers
- **Loadouts** - Equipment loadout management

#### Combat & Movement
- **RocketJump** - Rocket jumping mechanics with fireworks
- **RocketMan** - Rocket enhancements
- **RapidFire** - Fast firing mechanics for projectiles
- **Updraft** - Updraft flying mechanics

#### Tools & Utilities
- **Archaeology** - Archaeology automation for suspicious sand/gravel
- **AxolotlTools** - Axolotl-related tools
- **Grinder** - Automated grinding utilities
- **LoreLocator** - Locates items with specific lore
- **StashBrander** - Brand stashes with custom messages
- **WaxAura** - Automatic waxing of copper blocks
- **RoadTrip** - Travel utilities
- **AntiToS** - Bypasses certain restrictions
- **AdBlocker** - Blocks advertisements

#### Fun & Misc
- **Honker** - Custom honking sounds with goat horns
  - Multiple sound options
  - Configurable volume and pitch
- **MusicTweaks** - Music system tweaks
- **DisconnectSound** - Plays sound on disconnect (from Meteorist)

---

### 📊 HUD Elements

- **ItemCounterHud** - Displays selected blocks and inventory counts
- **EntityList** - Advanced entity tracker with projectile support
  - Tracks players, mobs, items, projectiles
  - Configurable distance calculation (2D/3D)
  - Sort by distance with custom colors
- **SpeedKMH** - Shows current speed in km/h
- **DimensionCoords** - Shows coordinates in both dimensions
- **DubCounterHud** - Container counter for looting
- **MobRateHud** - Mob farm performance analyzer

---

## 🔧 Enhanced Meteor Client Integration

BepHax includes custom mixins that enhance existing Meteor Client modules with advanced features.

### Meteor Module Enhancements

The addon includes specialized mixins (`bep-meteor.mixins.json`) that improve functionality of core Meteor modules:

#### Combat Enhancements
- **CrystalAuraMixin** - Adds rotation system with yaw stepping for anti-cheat bypass
  - Yaw stepping modes: OFF, SEMI, FULL
  - Configurable yaw step limit (45-90° for GrimAC)
  - Silent rotations using priority-based system
  - Instant rotate option
  - Coordinated with InventoryManager for silent swaps
- **AutoTrapMixin** - Adds rotation system with yaw stepping
  - Similar rotation features as CrystalAura
  - Priority-based rotation coordination
- **KillAuraMixin** - Enhanced kill aura functionality
- **VelocityMixin** - Advanced velocity modes for anti-cheat bypass
  - NORMAL, WALLS, GRIM, GRIM_V3 modes
  - Configurable horizontal and vertical multipliers
  - Advanced velocity modification patterns
- **ClientPlayerEntityGrimV3Mixin** - GrimAC v3 compatibility layer

#### Inventory & Items
- **AutoEatMixin** - Enhanced eating coordination with InventoryManager
  - Silent slot swapping during eating
  - Server-side slot tracking
- **ExpThrowerMixin** - Silent slot swapping for XP bottle throwing
- **BetterTooltipsMixin** - Enhanced item information display

#### Utilities
- **AutoLogMixin** - Enhanced disconnect handling with custom logging
- **FreecamMixin** - Extended features for camera manipulation
- **AutoMendMixin** - Improved experience management
- **TimerMixin** - Better game speed control
- **NametagsMixin** - Improved player name rendering
- **NoRenderMixin** - Extended render blocking options
- **NotifierMixin** - Enhanced notification system
- **PacketCancellerMixin** - Advanced packet manipulation
- **OnlinePlayersMixin** - Better player list management
- **NoSlowMixin** - Movement speed enhancements

#### GUI Enhancements
- **PeekScreenMixin** - Enhanced GUI peek functionality
- **ModuleScreenMixin** - Module screen improvements
- **ModulesScreenMixin** - Modules list improvements

### Why Mixins Matter

BepHax's mixin system allows seamless integration with Meteor Client without modifying the base mod. These enhancements:
- Add rotation systems to combat modules for anti-cheat bypass
- Integrate InventoryManager's silent slot swapping into existing modules
- Add advanced velocity modes for better movement control
- Are optional (`defaultRequire: 0`) - addon works even if mixins fail

---

## Usage Examples

### StashMover Setup
```
1. Select input area: `.stashmover input`
2. Select output area: `.stashmover output`
3. Configure pearl positions in module settings
4. Enable "Only Shulkers" to filter items
5. Enable "Fill Enderchest" for efficient transfer
6. Start the module to begin automated transfer
```

### GhostMode
```
1. Enable before dying or when death screen appears
2. Adjust "health-value" to maintain specific health
3. Enable "block-death-packets" if having issues
4. Toggle off to respawn normally
```

### BepMine (Speedmine)
```
1. Set keybind for instant mine toggle
2. Look at block and start mining
3. Toggle instant mine when needed
4. Visual indicators show mining progress
```

### TrailMaker
```
1. Enable module and toggle recording on
2. Add chunk highlights on Xaero's map at desired waypoints
3. Toggle recording off when done plotting
4. Click "Start Following" to navigate the trail automatically
5. Module removes highlights as you reach each point
```

### SearchArea
```
1. Select mode (Rectangle or Spiral)
2. For Rectangle mode: Set start and end positions
3. Configure path gap (chunks between each path)
4. Optional: Set save-name for data persistence
5. Enable module to begin automated chunk loading
```

### DiscordNotifs
```
1. Create a Discord webhook in your server
2. Enter webhook URL in module settings
3. Configure which events to log (messages, players, deaths, etc.)
4. Enable module to start sending notifications
```

---

## Credits & Attribution

BepHax is a community-driven project that aggregates and enhances modules from various talented developers:

### Core Contributors
- **[JEFF Stash Hunting](https://github.com/miles352/meteor-stashhunting-addon)** by [miles352](https://github.com/miles352)
  - Stash hunting tools, navigation, ESP modules

- **[Stardust](https://github.com/0xTas/stardust)** by [0xTas](https://github.com/0xTas)
  - Utility modules, 2b2t commands, automation tools

- **[Meteor Rejects](https://github.com/AntiCope/meteor-rejects)** by [AntiCope](https://github.com/AntiCope)
  - AutoCraft module

- **[INDICA](https://github.com/Faye-one/INDICA)** by [Faye-one](https://github.com/Faye-one)
  - Kill effects, map duplicator, respawn blocker

- **[Meteorist](https://github.com/Zgoly/Meteorist)** by [Zgoly](https://github.com/Zgoly)
  - DisconnectSound module

- **[HIGTools](https://github.com/RedCarlos26/HIGTools)** by [RedCarlos26](https://github.com/RedCarlos26)
  - Center and Coordinates commands

### BepHax Development
- **[dekrom](https://github.com/dekrom)** - Project maintainer, original modules, integration work

---

## Contributing

BepHax is community-driven! We welcome contributions from the 2b2t community:

1. Open an issue on [GitHub Issues](https://github.com/dekrom/BepHaxAddon/issues)
2. Submit a pull request with your improvements
3. Join discussions to suggest features or report bugs

Everyone can request changes to make this addon better for the 2b2t community.

---

## Community & Support

### Join the Community
- **Discord**: [Join our server](https://discord.gg/EGEhHNSkV8)
- **Twitch**: [Watch dekrom stream](https://www.twitch.tv/dekrom)
- **YouTube**: [dekrom's channel](https://www.youtube.com/@dekrom)

---

## License

Licensed under [GNU GPLv3](LICENSE). Feel free to fork and modify!

---

## Disclaimer

This addon is designed for use on anarchy servers like 2b2t.org. Use responsibly and be aware of server rules and policies.
