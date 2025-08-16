package bep.hax;

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
    public static final String COLOR = "Color is the visual perception of different wavelengths of light as hue, saturation, and brightness";

    @Override
    public void onInitialize() {
        LOG.info("BEPHAX LOADING.");

        // HUD
        Hud.get().register(ItemCounterHud.INFO);
        Hud.get().register(EntityList.INFO);
        Hud.get().register(DimensionCoords.INFO);
        Hud.get().register(SpeedKMH.INFO);
        Hud.get().register(DubCounterHud.INFO);
        Hud.get().register(MobRateHud.INFO);

        Modules.get().add(new AutoSmithing());
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
        Modules.get().add(new SignRender());
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
        Modules.get().add(new Grinder());

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
