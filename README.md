# BepHax Meteor Addon

[![GitHub Release](https://img.shields.io/github/v/release/dekrom/BepHaxAddon?include_prereleases&label=Latest%20Release)](https://github.com/dekrom/BepHaxAddon/releases)
[![GitHub License](https://img.shields.io/github/license/dekrom/BepHaxAddon)](https://github.com/dekrom/BepHaxAddon/blob/main/LICENSE)
[![GitHub Issues](https://img.shields.io/github/issues/dekrom/BepHaxAddon)](https://github.com/dekrom/BepHaxAddon/issues)

## What it is?

BepHax is created to simplify 2b2t.org players' lives by combining the best utility modules from various addons into one accessible Meteor Client addon, optimized for compatibility and ease of use on the 2b2t anarchy server while discarding some functionalities that might not be wanted or useful for 2b2t.

## Installation

1. Download the latest JAR from the [Releases](https://github.com/dekrom/BepHaxAddon/releases) page.
2. Place the JAR file in your Minecraft mods folder (typically `~/.minecraft/mods` on Fabric Loader).
3. Launch Minecraft with Fabric and Meteor Client installed.
4. Activate modules via the Meteor Client GUI (default key: Right Shift).

**Requirements:**
- [Minecraft 1.21.4](https://minecraft.net/)
- [Fabric Loader 0.16.10 or compatible](https://fabricmc.net/)
- [Meteor Client (latest snapshot for 1.21.4)](https://github.com/MeteorDevelopment/meteor-client/tree/a96efdcdd60ed226650f6fc7f952ba65371bfc4d)
- [Baritone API](https://github.com/cabaletta/baritone/tree/1.21.4)
- [ViaFabricPlus](https://modrinth.com/mod/viafabricplus) - **Required for silent rotation modules**
- [XaeroMinimap](https://modrinth.com/mod/xaeros-minimap)
- [XaeroWorldMap](https://modrinth.com/mod/xaeros-world-map)
- [XaeroPlus](https://github.com/rfresh2/XaeroPlus)

**Important Note for 2b2t:** For modules with silent rotations (Aura, Velo, Phase, Criticals) to work properly, you must connect to 2b2t.org using **Minecraft protocol 1.20.4 - 1.20.6** through ViaFabricPlus. This ensures proper compatibility with the server's anti-cheat systems.

## Features

BepHax organizes modules into custom categories optimized for 2b2t gameplay:

### Core Modules (BEPHAX Category)

#### Combat & PVP
- **Aura**: Advanced kill aura with smart targeting and rotation management (requires ViaFabricPlus)
- **Criticals**: Forces critical hits with multiple bypass modes (requires ViaFabricPlus)
- **Velo**: Velocity/knockback control for better combat (requires ViaFabricPlus)
- **Phase**: Phase through blocks with anti-cheat bypass (requires ViaFabricPlus)
- **PVPModule**: Base framework for PVP modules with rotation management

#### Mining & Resources
- **BepMine**: Advanced speedmine optimized for 2b2t with queue system
    - Toggleable instant mining keybind
    - Visual and chat notifications
    - Anti-cheat compatible timing
- **MineESP**: Highlights blocks being mined by other players
- **Stripper**: Strips logs automatically

#### Inventory & Items
- **ShulkerOverviewModule**: Shows most common item as mini icon on shulkers
    - Works in inventory, containers, and hotbar
    - Configurable icon size and position
    - Multiple item indicator
- **ItemSearchBar**: Search through inventories for specific items
- **AutoSmithing**: Automates smithing table upgrades
- **AutoCraft**: Automates crafting recipes (from Meteor Rejects)
- **InvFix**: Fixes inventory issues specific to 2b2t server
- **InventoryNotif**: Plays sound when inventory becomes full
- **Replenish**: Auto-refills items from inventory

#### Movement & Travel
- **YawLock**: Locks player yaw for precise movement with jitter option
- **ElytraSwap**: Auto-swaps elytras at low durability 
    - Remembers original item positions
    - Configurable durability threshold
- **ElytraFlyPlusPlus**: Enhanced elytra flight controls
- **NoJumpDelay**: Removes delay between jumps
- **AFKVanillaFly**: AFK flying without mods
- **TrailFollower**: Follows player trails

#### Automation
- **StashMover**: Advanced item transfer system with pearl loading
    - Input/Output area selection
    - Only Shulkers mode - filters non-shulker items
    - Break Empty - breaks empty containers
    - Fill Enderchest - efficient enderchest usage
    - Pearl loading between areas
    - Multi-axis pearl throwing support
    - Automatic container detection
- **AutoLogPlus**: Auto-logout on low health or threats
- **AutoPortal**: Automatic portal creation
- **AutoEXPPlus**: Automates experience mending
- **BetterAutoEat**: Enhanced auto-eat with smart food selection
- **PearlLoader**: Manages ender pearl loading/unloading
- **WheelPicker**: selection wheel for various configurable actions

#### Visual & ESP
- **SignRender**: Renders sign text through walls with clustering
- **PearlOwner**: Shows who threw ender pearls
- **OminousVaultESP**: Highlights Ominous Vaults
- **ShulkerFrameESP**: Highlights item frames with shulkers
- **VanityESP**: Custom entity ESP for vanity items
- **TreasureESP**: ESP for treasure items
- **KillEffects**: Visual and audio effects on entity death
- **HighlightOldLava**: Highlights ancient lava sources

#### Utilities
- **GhostMode**: Continue playing after death
    - Maintains configurable health value
    - Blocks death packets option
    - Prevents phasing through world
- **NoHurtCam**: Removes hurt camera shake
- **UnfocusedFpsLimiter**: Limits FPS when window unfocused
- **DisconnectSound**: Plays sound on disconnect
- **MapDuplicator**: Automatically duplicates filled maps
- **RespawnPointBlocker**: Prevents setting respawn points
- **WebChat**: Displays chat in web browser
- **IgnoreSync**: Advanced ignore list manager

### Stash Hunting Modules (STASH Category)

#### Search & Detection
- **BetterStashFinder**: Improved stash location finder
- **OldChunkNotifier**: Notifies when entering old chunks
- **ChestIndex**: Indexes and searches chest contents
- **LoreLocator**: Locates items with specific lore

#### Movement & Navigation
- **GotoPosition**: Navigate to specified coordinates
- **Pitch40Util**: Utility for 40-degree pitch mining
- **GrimAirPlace**: Grim-compatible air placement

### Utility Modules (STARDUST Category)

#### Commands
- **Center**: Center positioning command
- **Coordinates**: Coordinate utilities
- **FirstSeen2b2t**: Check first seen date on 2b2t
- **LastSeen2b2t**: Check last seen date on 2b2t
- **Playtime2b2t**: Check playtime on 2b2t
- **Stats2b2t**: Display player stats on 2b2t
- **Panorama**: Custom panorama utilities
- **Loadout**: Manage equipment loadouts

#### Creative Tools
- **BookTools**: Advanced book editing tools
- **ChatSigns**: Create signs in chat
- **SignHistorian**: Track sign history
- **SignatureSign**: Sign signatures
- **BannerData**: Display banner data
- **PagePirate**: Page copying utilities

#### Fun & Misc
- **Honker**: Custom honking sounds with goat horns
- **MusicTweaks**: Music system tweaks
- **RocketJump**: Rocket jumping mechanics
- **RocketMan**: Rocket enhancements
- **RapidFire**: Fast firing mechanics
- **Updraft**: Updraft flying mechanics
- **Grinder**: Automated grinding utilities

#### Specialized Tools
- **Archaeology**: Archaeology automation
- **AutoDrawDistance**: Adjusts render distance automatically
- **AutoDyeShulkers**: Automatically dyes shulkers
- **AxolotlTools**: Axolotl-related tools
- **StashBrander**: Brand stashes with custom messages
- **WaxAura**: Automatic waxing of copper blocks
- **RoadTrip**: Travel utilities
- **AntiToS**: Bypasses certain restrictions

### HUD Elements

- **BlockCounterHud**: Displays selected blocks and inventory counts
- **EntityList**: Advanced entity tracker with projectile support
    - Tracks players, mobs, items, projectiles
    - Configurable distance calculation (2D/3D)
    - Sort by distance with custom colors
- **SpeedKMH**: Shows current speed in km/h
- **DimensionCoords**: Shows coordinates in both dimensions
- **DubCounterHud**: Container counter for looting
- **MobRateHud**: Mob farm performance analyzer

## Usage Examples

### StashMover Setup
1. Select input area: `.stashmover input`
2. Select output area: `.stashmover output`
3. Configure pearl positions in module settings
4. Enable "Only Shulkers" to filter items
5. Enable "Fill Enderchest" for efficient transfer
6. Start the module to begin automated transfer

## Credits

This addon aggregates modules from the following projects—huge thanks to their creators:
- [JEFF Stash Hunting](https://github.com/miles352/meteor-stashhunting-addon) by miles352
- [Meteor Rejects](https://github.com/AntiCope) by AntiCope
- [Stardust](https://github.com/0xTas/stardust) by 0xTas
- [Meteorist](https://github.com/Zgoly/Meteorist/) by Zgoly

Original BepHax modules were developed specifically for 2b2t optimization.

## Contributing

BepHax is community-driven! If you'd like to suggest features, report bugs, or contribute code:
1. Open an issue on GitHub
2. Submit a pull request
3. Join discussions in issues

Everyone can request changes to make this addon better for the 2b2t community.

## License

Licensed under [GNU GPLv3](LICENSE). Feel free to fork and modify!

## Support

- **Issues**: [GitHub Issues](https://github.com/dekrom/BepHaxAddon/issues)
- **Discord**: Join the 2b2t community servers
- **Wiki**: Check the [GitHub Wiki](https://github.com/dekrom/BepHaxAddon/wiki) for detailed guides
