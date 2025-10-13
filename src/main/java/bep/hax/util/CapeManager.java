package bep.hax.util;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import java.util.*;
import static meteordevelopment.meteorclient.MeteorClient.mc;
public class CapeManager {
    private static final CapeManager INSTANCE = new CapeManager();
    public static final Identifier CAPE_TEXTURE = Identifier.of("bephax", "textures/cape/cape.png");
    public static final Identifier FRIEND_CAPE_TEXTURE = Identifier.of("bephax", "textures/cape/bephax.png");
    public static final Identifier TRADING_CAPE_TEXTURE = Identifier.of("bephax", "textures/cape/trading.png");
    private static final Set<UUID> CAPE_UUIDS = new HashSet<>();
    private static final Set<UUID> FRIEND_CAPE_UUIDS = new HashSet<>();
    private static final Set<UUID> TRADING_CAPE_UUIDS = new HashSet<>();
    private final Set<UUID> glowingPlayers = Collections.synchronizedSet(new HashSet<>());
    static {
        CAPE_UUIDS.add(UUID.fromString("2ffa806c-cbad-4274-8175-eb2494b7fd66"));
        CAPE_UUIDS.add(UUID.fromString("d039366f-5879-4de3-b4fa-a496f6e6e12c"));
        CAPE_UUIDS.add(UUID.fromString("fa022f07-1791-4fdd-8aa5-ff9e7cccb515"));
        CAPE_UUIDS.add(UUID.fromString("76e66b60-13e7-4c2e-aa72-a79c112b9d10"));
        CAPE_UUIDS.add(UUID.fromString("9b684b28-8093-4345-8938-e7c3585917af"));
        CAPE_UUIDS.add(UUID.fromString("ab7baa00-7b85-486b-bf73-506139d6f222"));
        CAPE_UUIDS.add(UUID.fromString("47fac000-3c26-4cc8-917a-cfba1fa922d3"));
        CAPE_UUIDS.add(UUID.fromString("9a5e22ed-143d-45a8-90f5-cd5d76448f89"));
        CAPE_UUIDS.add(UUID.fromString("124dc8eb-e3dd-4d95-81af-6cf2dcd3515c"));
        CAPE_UUIDS.add(UUID.fromString("dced3c5a-8ac7-4766-bf4a-ec8b646e0ac1"));
        CAPE_UUIDS.add(UUID.fromString("5b1de9b3-b867-4a42-b21e-5fcc78d01c26"));
        CAPE_UUIDS.add(UUID.fromString("effe2d70-4171-42a1-aedb-fc43ecf14b87"));
        CAPE_UUIDS.add(UUID.fromString("8ac72a70-1334-4841-a9fe-a62e9abb1cdd"));
        CAPE_UUIDS.add(UUID.fromString("2bf5a8f8-ccdf-4841-a781-0ff69bf0b41c"));
        CAPE_UUIDS.add(UUID.fromString("290bcd4a-edd3-4b13-bfce-81121f5f5432"));
        CAPE_UUIDS.add(UUID.fromString("51dcd870-d33b-40e9-9fc1-aecdcff96081"));
        CAPE_UUIDS.add(UUID.fromString("187a435c-31fd-48d0-99d1-9f299a31ec05"));
        CAPE_UUIDS.add(UUID.fromString("5c75988a-7df3-42fe-8ea8-e52e7345dc02"));
        CAPE_UUIDS.add(UUID.fromString("29fa11fd-5452-4db2-9405-22a63658e464"));
        CAPE_UUIDS.add(UUID.fromString("57f9d29f-03a8-4491-8a0c-e391a6986869"));
        CAPE_UUIDS.add(UUID.fromString("8034d01d-bc3b-49e2-a6f6-29455d0a5f24"));
        CAPE_UUIDS.add(UUID.fromString("fdee323e-7f0c-4c15-8d1c-0f277442342a"));
        CAPE_UUIDS.add(UUID.fromString("6146ef23-4498-4ba1-a881-9f34b382c3c4"));
        CAPE_UUIDS.add(UUID.fromString("55636aad-6761-4ecc-90dc-bd1b5d2baf2c"));
        CAPE_UUIDS.add(UUID.fromString("988353dd-8e59-4a5a-9c86-415d4f46eef4"));
        CAPE_UUIDS.add(UUID.fromString("17fe552b-5224-455d-ade6-e7b8bc78edbe"));
        CAPE_UUIDS.add(UUID.fromString("cda8edd9-430e-4f6e-a45a-be4566f39c38"));
        CAPE_UUIDS.add(UUID.fromString("cda8edd9-430e-4f6e-a45a-be4566f39c38"));
        CAPE_UUIDS.add(UUID.fromString("11b822dc-fa0d-4b22-b123-30a6a5b6e781"));
        CAPE_UUIDS.add(UUID.fromString("20a9929a-0833-4103-8a76-3365eb25b089"));
        CAPE_UUIDS.add(UUID.fromString("9ca029bb-2e4c-4d79-95f8-465c2c336e44"));
        CAPE_UUIDS.add(UUID.fromString("0bf55022-5e58-44ec-be17-6d12d6bbb918"));
        CAPE_UUIDS.add(UUID.fromString("6c623aee-e635-410b-b052-ceb4f1e1d914"));
        CAPE_UUIDS.add(UUID.fromString("b95aaeab-a70e-44d1-a363-1f2dd4e1cc57"));
        CAPE_UUIDS.add(UUID.fromString("de8bf2dc-01ab-4859-bfd7-4fd820686522"));
        CAPE_UUIDS.add(UUID.fromString("ecc6ad17-611f-46cc-aa1f-99f5742e8cbc"));
        CAPE_UUIDS.add(UUID.fromString("7e4d4671-eb0b-427c-b94a-64a020daf1fa"));
        CAPE_UUIDS.add(UUID.fromString("faea9d8b-b6de-4855-91ac-9a884fee8836"));
        CAPE_UUIDS.add(UUID.fromString("c941216d-e16b-4019-b965-3570dead2ea3"));
        FRIEND_CAPE_UUIDS.add(UUID.fromString("f5c4da20-b770-4073-a50f-e9d2cd69ae3c"));
        FRIEND_CAPE_UUIDS.add(UUID.fromString("7cbdb0a3-a141-4a2c-a4cc-f9637d392b56"));
        FRIEND_CAPE_UUIDS.add(UUID.fromString("91bdb7ca-ad68-4b92-a78e-898c761aed42"));
        FRIEND_CAPE_UUIDS.add(UUID.fromString("1f68fc6c-b180-41e3-96bf-62e15756d290"));
        FRIEND_CAPE_UUIDS.add(UUID.fromString("2ba6f46e-ef30-4060-a019-5832045e2110"));
        FRIEND_CAPE_UUIDS.add(UUID.fromString("2d3e440e-b764-41b4-9aa5-ebcf1cd87ac4"));
        FRIEND_CAPE_UUIDS.add(UUID.fromString("3062e6ee-e4b3-41af-bf41-ae29921bda6a"));
        FRIEND_CAPE_UUIDS.add(UUID.fromString("bf775b81-1fae-40a1-b967-95ea5da0404e"));
        FRIEND_CAPE_UUIDS.add(UUID.fromString("ad3195a8-1888-44d7-9d61-5bddacfa8b8f"));
        FRIEND_CAPE_UUIDS.add(UUID.fromString("34a67d07-9afc-4620-9708-24254ae0e362"));
        FRIEND_CAPE_UUIDS.add(UUID.fromString("3186e3aa-0030-4ad0-a61e-2ffacb425b86"));
        FRIEND_CAPE_UUIDS.add(UUID.fromString("c52a9f8b-22e0-4cd6-93c3-2c664f2f89b8"));
        FRIEND_CAPE_UUIDS.add(UUID.fromString("c1dad6ec-2454-47fc-b23f-64a6315055e1"));
        FRIEND_CAPE_UUIDS.add(UUID.fromString("0efe3c27-d239-4bca-926f-e85d7527b37b"));
        TRADING_CAPE_UUIDS.add(UUID.fromString("e9bb2116-19f6-4998-90ac-352d128b06a1"));
        TRADING_CAPE_UUIDS.add(UUID.fromString("af6ba3f6-24ab-4eae-a52e-81c7b0097955"));
        TRADING_CAPE_UUIDS.add(UUID.fromString("af371e84-a08c-4eaa-ae03-e45bb60d6a46"));
        TRADING_CAPE_UUIDS.add(UUID.fromString("be8e82dc-95e5-40f8-9c5e-58a9a5058ce7"));
        TRADING_CAPE_UUIDS.add(UUID.fromString("863e5ba9-fb0e-4867-a870-371cf2d86bb3"));
        TRADING_CAPE_UUIDS.add(UUID.fromString("f65f7c2b-070a-4db1-b377-e7743a850c3f"));
    }
    private CapeManager() {
        MeteorClient.EVENT_BUS.subscribe(this);
    }
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        glowingPlayers.clear();
        for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
            if (entry.getProfile() != null && entry.getProfile().getId() != null) {
                UUID uuid = entry.getProfile().getId();
                if (CAPE_UUIDS.contains(uuid) || FRIEND_CAPE_UUIDS.contains(uuid) || TRADING_CAPE_UUIDS.contains(uuid)) {
                    glowingPlayers.add(uuid);
                }
            }
        }
    }
    public static CapeManager getInstance() {
        return INSTANCE;
    }
    public boolean hasCape(UUID uuid) {
        return CAPE_UUIDS.contains(uuid);
    }
    public boolean hasCape(PlayerEntity player) {
        return player != null && hasCape(player.getUuid());
    }
    public boolean hasFriendCape(UUID uuid) {
        return FRIEND_CAPE_UUIDS.contains(uuid);
    }
    public boolean hasFriendCape(PlayerEntity player) {
        return player != null && hasFriendCape(player.getUuid());
    }
    public boolean hasTradingCape(UUID uuid) {
        return TRADING_CAPE_UUIDS.contains(uuid);
    }
    public boolean hasTradingCape(PlayerEntity player) {
        return player != null && hasTradingCape(player.getUuid());
    }
    public boolean hasAnyCape(UUID uuid) {
        return hasCape(uuid) || hasFriendCape(uuid) || hasTradingCape(uuid);
    }
    public boolean hasAnyCape(PlayerEntity player) {
        return player != null && hasAnyCape(player.getUuid());
    }
    public void addGlowingPlayer(UUID uuid) {
        glowingPlayers.add(uuid);
    }
    public void removeGlowingPlayer(UUID uuid) {
        glowingPlayers.remove(uuid);
    }
    public boolean shouldGlow(UUID uuid) {
        return glowingPlayers.contains(uuid);
    }
    public boolean shouldGlow(PlayerEntity player) {
        return player != null && shouldGlow(player.getUuid());
    }
    public void clearGlowingPlayers() {
        glowingPlayers.clear();
    }
    public Set<UUID> getGlowingPlayers() {
        return Collections.unmodifiableSet(glowingPlayers);
    }
    public static void addCapeUUID(UUID uuid) {
        CAPE_UUIDS.add(uuid);
    }
    public static void addCapeUUID(String uuidString) {
        try {
            CAPE_UUIDS.add(UUID.fromString(uuidString));
        } catch (IllegalArgumentException e) {
        }
    }
    public static Set<UUID> getCapeUUIDs() {
        return Collections.unmodifiableSet(CAPE_UUIDS);
    }
    public static void addFriendCapeUUID(UUID uuid) {
        FRIEND_CAPE_UUIDS.add(uuid);
    }
    public static void addFriendCapeUUID(String uuidString) {
        try {
            FRIEND_CAPE_UUIDS.add(UUID.fromString(uuidString));
        } catch (IllegalArgumentException e) {
        }
    }
    public static Set<UUID> getFriendCapeUUIDs() {
        return Collections.unmodifiableSet(FRIEND_CAPE_UUIDS);
    }
    public static void addTradingCapeUUID(UUID uuid) {
        TRADING_CAPE_UUIDS.add(uuid);
    }
    public static void addTradingCapeUUID(String uuidString) {
        try {
            TRADING_CAPE_UUIDS.add(UUID.fromString(uuidString));
        } catch (IllegalArgumentException e) {
        }
    }
    public static Set<UUID> getTradingCapeUUIDs() {
        return Collections.unmodifiableSet(TRADING_CAPE_UUIDS);
    }
}