package bep.hax;

import bep.hax.hud.*;
import bep.hax.modules.*;
import bep.hax.modules.searcharea.SearchArea;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import bep.hax.managers.PacketManager;


public class Bep extends MeteorAddon {
    public static final Logger LOG = LoggerFactory.getLogger("BepHax");
    public static final Category CATEGORY = new Category("Bephax");
    public static final Category STASH = new Category("Stash Hunt");
    public static final Category STARDUST= new Category("Stardust");
    public static final HudGroup HUD_GROUP = new HudGroup("Bephax");

    private PacketManager packetManager;

    @Override
    public void onInitialize() {
        LOG.info("BEPHAX LOADING.");

        // Initialize StashMover selection handler
        bep.hax.modules.StashMoverSelectionHandler.init();

        // HUD
        Hud.get().register(ItemCounterHud.INFO);
        Hud.get().register(EntityList.INFO);
        Hud.get().register(DimensionCoords.INFO);
        Hud.get().register(SpeedKMH.INFO);
        Hud.get().register(DubCounterHud.INFO);
        Hud.get().register(MobRateHud.INFO);
        //Hud.get().register(BlockCounterHud.INFO);

        Commands.add(new bep.hax.commands.IgnoreSyncCommand());

        Modules.get().add(new AutoSmith());
        Modules.get().add(new BepMine());
        Modules.get().add(new YawLock());
        Modules.get().add(new UnfocusedFpsLimiter());
        Modules.get().add(new ShulkerOverviewModule());
        Modules.get().add(new ItemSearchBar());
        Modules.get().add(new MineESP());
        Modules.get().add(new Aura());
        Modules.get().add(new Velo());
        Modules.get().add(new Phase());
        Modules.get().add(new Criticals());
        Modules.get().add(new PearlOwner());
        Modules.get().add(new PearlLoader());
        Modules.get().add(new SignRender());
        Modules.get().add(new WheelPicker());
        Modules.get().add(new NoHurtCam());
        Modules.get().add(new ElytraSwap());
        Modules.get().add(new IgnoreSync());
        Modules.get().add(new InvFix());
        Modules.get().add(new WebChat());
        Modules.get().add(new Replenish());
        Modules.get().add(new GhostMode());

        bep.hax.util.CapeManager.getInstance();


        // StashMover - Automated shulker transfer system
        Modules.get().add(new StashMover());
        Commands.add(new bep.hax.commands.SetInput());
        Commands.add(new bep.hax.commands.SetOutput());
        Commands.add(new bep.hax.commands.StashStatus());
        Commands.add(new bep.hax.commands.SetClear());

        // INDICA MOD https://github.com/Faye-one/INDICA
        Modules.get().add(new KillEffects());
        Modules.get().add(new RespawnPointBlocker());
        Modules.get().add(new MapDuplicator());


        // JEFF STASH HUNTING https://github.com/miles352/meteor-stashhunting-addon
        Modules.get().add(new ElytraFlyPlusPlus());

        Modules.get().add(new AFKVanillaFly());
        Modules.get().add(new NoJumpDelay());
        Modules.get().add(new AutoEXPPlus());
        Modules.get().add(new AutoLogPlus());
        Modules.get().add(new AutoPortal());
        Modules.get().add(new ChestIndex());
        Modules.get().add(new GotoPosition());
        Modules.get().add(new HighlightOldLava());
        Modules.get().add(new Pitch40Util());
        Modules.get().add(new GrimAirPlace());
        Modules.get().add(new GrimScaffold());
        Modules.get().add(new TrailFollower());
        Modules.get().add(new Stripper());
        Modules.get().add(new VanityESP());
        Modules.get().add(new BetterStashFinder());
        Modules.get().add(new OldChunkNotifier());
        Modules.get().add(new SearchArea());
        Modules.get().add(new DiscordNotifs());
        Modules.get().add(new TrailMaker());


        // HIGTools https://github.com/RedCarlos26/HIGTools
        Commands.add(new bep.hax.commands.Coordinates());
        Commands.add(new bep.hax.commands.Center());



        // Meteor Rejects https://github.com/AntiCope
        Modules.get().add(new AutoCraft());

        // Stardust https://github.com/0xTas/stardust
        Commands.add(new bep.hax.commands.FirstSeen2b2t());
        Commands.add(new bep.hax.commands.LastSeen2b2t());
        Commands.add(new bep.hax.commands.Playtime2b2t());
        Commands.add(new bep.hax.commands.Stats2b2t());
        Commands.add(new bep.hax.commands.Panorama());
        Commands.add(new bep.hax.commands.Loadout());

        // Stardust Modules https://github.com/0xTas/stardust
        Modules.get().add(new AdBlocker());
        Modules.get().add(new Loadouts());
        Modules.get().add(new AntiToS());
        Modules.get().add(new ChatSigns());
        Modules.get().add(new Archaeology());
        Modules.get().add(new AutoDrawDistance());
        Modules.get().add(new AutoDyeShulkers());
        Modules.get().add(new AxolotlTools());
        Modules.get().add(new BannerData());
        Modules.get().add(new BookTools());
        Modules.get().add(new Honker());
        Modules.get().add(new LoreLocator());
        Modules.get().add(new MusicTweaks());
        Modules.get().add(new PagePirate());
        Modules.get().add(new RapidFire());
        Modules.get().add(new RoadTrip());
        Modules.get().add(new RocketJump());
        Modules.get().add(new RocketMan());
        Modules.get().add(new SignHistorian());
        Modules.get().add(new SignatureSign());
        Modules.get().add(new StashBrander());
        Modules.get().add(new WaxAura());
        Modules.get().add(new Updraft());
        Modules.get().add(new Grinder());
        Modules.get().add(new AutoDoors());
        Modules.get().add(new AutoMason());


        // Meteorist https://github.com/Zgoly/Meteorist/
        Modules.get().add(new DisconnectSound());





    }


    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
        Modules.registerCategory(STASH);
        Modules.registerCategory(STARDUST);
    }


    @Override
    public String getPackage() {
        return "bep.hax";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("BepHaxAddon", "bephaxaddon");
    }
}
