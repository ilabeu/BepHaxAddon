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

### Original/Enhanced Modules (BEPHAX Category)
These modules were created or enhanced specifically for this addon:
- **AutoSmithing**: Automates smithing table upgrades
- **YawLock**: Locks player yaw for precise movement
- **UnfocusedFpsLimiter**: Limits FPS when the game window is unfocused
- **ShulkerOverviewModule**: Shows the most abundant item as a mini icon on shulkers in inventory
- **BepMine**: Speedmine optimized for 2b2t with queue system
- **MineESP**: ESP for blocks being mined by other players
- **ItemSearchBar**: Search through inventories for specific items
- **Aura**: Kill aura module for combat (requires ViaFabricPlus + 1.20.4-1.20.6 protocol)
- **Velo**: Velocity/knockback control (requires ViaFabricPlus + 1.20.4-1.20.6 protocol)
- **Phase**: Phase through blocks (requires ViaFabricPlus + 1.20.4-1.20.6 protocol)
- **Criticals**: Forces critical hits (requires ViaFabricPlus + 1.20.4-1.20.6 protocol)
- **PVPModule**: Base class for PVP modules with rotation management
- **ElytraSwap**: Automatically swaps elytras when they reach low durability and restores items to original positions
- **PearlOwner**: Displays the name of the player who threw an ender pearl
- **SignRender**: Renders sign text through walls with advanced clustering
- **NoHurtCam**: Removes the hurt camera tilt and shake effect when taking damage
- **IgnoreSync**: Advanced 2b2t ignore list manager with offline player queuing
- **InvFix**: Fixes inventory issues specific to 2b2t server(Thanks to [EnderKill98](https://github.com/EnderKill98/Fix2b2tGhostItems))
- **WebChat**: Displays Minecraft chat in web browser interface
- **AutoCraft**: Automates crafting recipes (from Meteor Rejects)
- **DisconnectSound**: Plays sound on disconnect (from Meteorist)

### HUD Elements
- **BlockCounterHud**: Displays selected blocks and their inventory counts with customizable layout
- **EntityList**: Advanced entity tracker with projectile support
- **SpeedKMH**: Shows current speed in kilometers per hour
- **DimensionCoords**: Shows coordinates in both Overworld and Nether simultaneously
- **DubCounterHud**: Comprehensive container counter for 2b2t looting
- **MobRateHud**: Advanced mob farm performance analyzer

### Stash Hunting Modules (STASH Category)
From JEFF addon (Credit: [miles352](https://github.com/miles352/meteor-stashhunting-addon)):
- **ElytraFlyPlusPlus**: Enhanced elytra flying controls
- **NoJumpDelay**: Removes the delay between jumps
- **AFKVanillaFly**: AFK flying without mods
- **AutoEXPPlus**: Automates experience mending
- **AutoLogPlus**: Auto-logout on low health or threats
- **AutoPortal**: Automatic portal creation
- **ChestIndex**: Indexes and searches chest contents
- **GotoPosition**: Navigate to specified coordinates
- **HighlightOldLava**: Highlights ancient lava sources
- **Pitch40Util**: Utility for 40-degree pitch mining
- **GrimAirPlace**: Grim-compatible air placement
- **GrimSilentRotations**: Silent rotations compatible with Grim
- **TrailFollower**: Follows player trails
- **VanityESP**: Custom entity ESP for vanity items
- **BetterStashFinder**: Improved stash location finder
- **OldChunkNotifier**: Notifies of old chunks
- **SearchArea**: Defines search areas for resources



### Utility Modules (STARDUST Category)
From Stardust addon (Credit: [0xTas](https://github.com/0xTas/stardust)):

**Commands**:
- **Center**: Center positioning command
- **Coordinates**: Coordinate utilities
- **FirstSeen2b2t**: Checks first seen date on 2b2t
- **LastSeen2b2t**: Checks last seen date on 2b2t
- **Playtime2b2t**: Checks playtime on 2b2t
- **Stats2b2t**: Displays player stats on 2b2t
- **Panorama**: Custom panorama utilities
- **Loadout**: Manages equipment loadouts

**Modules**:
- **Loadouts**: Saves and loads player loadouts
- **AntiToS**: Bypasses certain restrictions
- **ChatSigns**: Signs in chat
- **Archaeology**: Archaeology utilities
- **AutoDrawDistance**: Adjusts draw distance automatically
- **AutoDyeShulkers**: Dyes shulkers automatically
- **AxolotlTools**: Axolotl-related tools
- **BannerData**: Displays banner data
- **BookTools**: Book editing tools
- **Honker**: Custom honking sounds
- **LoreLocator**: Locates lore items
- **MusicTweaks**: Music system tweaks
- **PagePirate**: Page utilities
- **RapidFire**: Fast firing mechanics
- **RoadTrip**: Travel utilities
- **RocketJump**: Rocket jumping
- **RocketMan**: Rocket enhancements
- **SignHistorian**: Sign history tracking
- **SignatureSign**: Signs signatures
- **StashBrander**: Brands stashes
- **WaxAura**: Waxing utilities
- **TreasureESP**: ESP for treasures
- **Updraft**: Updraft flying
- **Grinder**: Automated grinding utilities
- 
### INDICA Modules (BepHax Category)
From INDICA addon (Credit: [Faye-One](https://github.com/Faye-one/INDICA)):
- **InventoryNotif**: Plays sound when inventory becomes full
- **KillEffects**: Visual and audio effects when entities die
- **MapDuplicator**: Automatically duplicates filled maps
- **OminousVaultESP**: Highlights Ominous Vaults with rendering
- **RespawnPointBlocker**: Prevents setting respawn points at beds/anchors
- **ShulkerFrameESP**: Highlights item frames containing shulker boxes

## Credits
This addon aggregates modules from the following projects—huge thanks to their creators for open-sourcing their work:
- [JEFF Stash Hunting](https://github.com/miles352/meteor-stashhunting-addon) by miles352
- [Meteor Rejects](https://github.com/AntiCope) by AntiCope
- [Stardust](https://github.com/0xTas/stardust) by 0xTas
- [Meteorist](https://github.com/Zgoly/Meteorist/) by Zgoly
- [INDICA](https://github.com/Faye-one/INDICA) by Faye-One

Original BepHax modules were developed specifically for 2b2t optimization.

## Contributing
BepHax is community-driven! If you'd like to suggest features, report bugs, or contribute code, open an issue or pull request. Everyone can request changes to make this addon better for the 2b2t community.

## License
Licensed under [GNU GPLv3](LICENSE). Feel free to fork and modify!
