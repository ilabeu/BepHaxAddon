package bep.hax;

import bep.hax.globalsettings.*;
import bep.hax.hud.*;
import bep.hax.modules.*;
import bep.hax.modules.searcharea.SearchArea;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.item.Items;
import org.slf4j.Logger;

public class Bep extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("Bephax");
    public static final Category STASH = new Category("Stash Hunt");
    public static final Category STARDUST= new Category("Stardust");
    public static final HudGroup HUD_GROUP = new HudGroup("Bephax");
    public static final Category SETTINGS = new Category("BOSettings", Items.OBSIDIAN.getDefaultStack());
    public static final Category BLACKOUT = new Category("BlackOut", Items.END_CRYSTAL.getDefaultStack());
    public static final String COLOR = "Color is the visual perception of different wavelengths of light as hue, saturation, and brightness";

    @Override
    public void onInitialize() {
        LOG.info("BEPHAX LOADING.");

        // HUD
        Hud.get().register(BlockCounterHud.INFO);
        Hud.get().register(EntityList.INFO);

        Modules.get().add(new AutoSmithing());
        Modules.get().add(new BepMine());
        Modules.get().add(new GrimSilentRotations());
        Modules.get().add(new YawLock());
        Modules.get().add(new UnfocusedFpsLimiter());
        Modules.get().add(new ShulkerOverviewModule());
        //Modules.get().add(new Miner());
        //Modules.get().add(new Autoduper());


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
        Modules.get().add(new TrailFollower());
        Modules.get().add(new VanityESP());
        Modules.get().add(new BetterStashFinder());
        Modules.get().add(new OldChunkNotifier());
        Modules.get().add(new SearchArea());

        // HIGTools https://github.com/RedCarlos26/HIGTools
        Commands.add(new bep.hax.commands.Coordinates());


        // TrouserStreak https://github.com/etianl/Trouser-Streak
        Modules.get().add(new BetterScaffold());
        Modules.get().add(new SuperInstaMine());
        Modules.get().add(new BetterAutoSign());

        // IKEA https://github.com/Nooniboi/Public-Ikea
        Modules.get().add(new AntiInteract());
        Modules.get().add(new AutoItemMove());
        Modules.get().add(new AntiDrop());
        Modules.get().add(new DubCounter());

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
        Modules.get().add(new Loadouts());
        Modules.get().add(new AntiToS());
        Modules.get().add(new ChatSigns());
        Modules.get().add(new Archaeology());
        Modules.get().add(new AutoDoors());
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
        Modules.get().add(new TreasureESP());
        Modules.get().add(new Updraft());


        // Meteorist https://github.com/Zgoly/Meteorist/
        Modules.get().add(new DisconnectSound());




        // Blackout https://github.com/KassuK1/BlackOut
        Modules.get().add(new FacingSettings());
        Modules.get().add(new RangeSettings());
        Modules.get().add(new RaytraceSettings());
        Modules.get().add(new RotationSettings());
        Modules.get().add(new ServerSettings());
        Modules.get().add(new SwingSettings());

        Modules.get().add(new AnchorAuraPlus());
        Modules.get().add(new AntiAim());
        Modules.get().add(new AntiCrawl());
        Modules.get().add(new AutoCraftingTable());
        Modules.get().add(new AutoMend());
        Modules.get().add(new AutoMine());
        Modules.get().add(new AutoPearl());
        Modules.get().add(new AutoTrapPlus());
        Modules.get().add(new BedAuraPlus());
        Modules.get().add(new Blocker());
        Modules.get().add(new BurrowPlus());
        Modules.get().add(new CustomFOV());
        Modules.get().add(new ElytraFlyPlus());
        Modules.get().add(new FastXP());
        Modules.get().add(new FeetESP());
        Modules.get().add(new FlightPlus());
        Modules.get().add(new ForceSneak());
        Modules.get().add(new HoleFillPlus());
        Modules.get().add(new HoleFillRewrite());
        Modules.get().add(new HoleSnap());
        Modules.get().add(new JesusPlus());
        Modules.get().add(new KillAuraPlus());
        Modules.get().add(new LightsOut());
        Modules.get().add(new MineESP());
        Modules.get().add(new OffHandPlus());
        Modules.get().add(new PacketFly());
        Modules.get().add(new PacketLogger());
        Modules.get().add(new PingSpoof());
        Modules.get().add(new PistonCrystal());
        Modules.get().add(new PistonPush());
        Modules.get().add(new PortalGodMode());
        Modules.get().add(new ScaffoldPlus());
        Modules.get().add(new SelfTrapPlus());
        Modules.get().add(new SoundModifier());
        Modules.get().add(new SpeedPlus());
        Modules.get().add(new SprintPlus());
        Modules.get().add(new StepPlus());
        Modules.get().add(new StrictNoSlow());
        Modules.get().add(new Suicide());
        Modules.get().add(new SurroundPlus());
        Modules.get().add(new SwingModifier());
        Modules.get().add(new TickShift());
        Modules.get().add(new WeakAlert());

    }


    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
        Modules.registerCategory(STASH);
        Modules.registerCategory(STARDUST);
        Modules.registerCategory(BLACKOUT);
        Modules.registerCategory(SETTINGS);
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
