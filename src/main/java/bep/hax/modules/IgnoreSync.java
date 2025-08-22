package bep.hax.modules;

import bep.hax.Bep;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.PlayerListEntry;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IgnoreSync extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgAutoSync = settings.createGroup("Auto Sync");
    private final SettingGroup sgDisplay = settings.createGroup("Display");
    private final SettingGroup sgQueue = settings.createGroup("Queue Management");

    private final Setting<Boolean> autoSync = sgGeneral.add(new BoolSetting.Builder()
        .name("Auto Sync")
        .description("Automatically sync ignore list when joining 2b2t.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> syncDelay = sgGeneral.add(new IntSetting.Builder()
        .name("Sync Delay")
        .description("Delay in seconds after joining before syncing.")
        .defaultValue(10)
        .min(5)
        .max(60)
        .sliderRange(5, 60)
        .visible(autoSync::get)
        .build()
    );

    private final Setting<Boolean> verboseMode = sgGeneral.add(new BoolSetting.Builder()
        .name("Verbose Mode")
        .description("Show detailed sync information in chat.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> hideMessages = sgGeneral.add(new BoolSetting.Builder()
        .name("Hide Server Messages")
        .description("Hide ignore/unignore messages from chat.")
        .defaultValue(true)
        .build()
    );


    private final Setting<Integer> pageDelay = sgAutoSync.add(new IntSetting.Builder()
        .name("Page Delay")
        .description("Delay in ticks between requesting pages.")
        .defaultValue(60)
        .min(40)
        .max(200)
        .sliderRange(40, 200)
        .build()
    );

    private final Setting<Boolean> autoIgnoreOnline = sgAutoSync.add(new BoolSetting.Builder()
        .name("Auto Ignore Online")
        .description("Automatically ignore online players from your local list.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> queueOffline = sgAutoSync.add(new BoolSetting.Builder()
        .name("Queue Offline Players")
        .description("Queue offline players to be ignored when they come online.")
        .defaultValue(true)
        .build()
    );


    private final Setting<Boolean> showOnlineStatus = sgDisplay.add(new BoolSetting.Builder()
        .name("Show Online Status")
        .description("Show if players are currently online.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> checkQueueDelay = sgQueue.add(new IntSetting.Builder()
        .name("Queue Check Delay")
        .description("How often to check queued players (seconds).")
        .defaultValue(30)
        .min(10)
        .max(120)
        .sliderRange(10, 120)
        .build()
    );

    private final Setting<Integer> actionDelay = sgQueue.add(new IntSetting.Builder()
        .name("Action Delay")
        .description("Delay between ignore/unignore actions (ms).")
        .defaultValue(1500)
        .min(1000)
        .max(5000)
        .sliderRange(1000, 5000)
        .build()
    );

    private final Setting<String> storageFile = sgGeneral.add(new StringSetting.Builder()
        .name("Storage File")
        .description("File name for storing ignore lists.")
        .defaultValue("2b2t_ignore_list.json")
        .build()
    );

    // State management
    private boolean isSyncing = false;
    private boolean isCollectingList = false;
    private int syncTimer = 0;
    private int pageTimer = 0;
    private int queueCheckTimer = 0;
    private boolean waitingForResponse = false;
    private int nextPageToRequest = -1;

    // Data storage
    private final Set<String> serverIgnoreList = ConcurrentHashMap.newKeySet();
    private final Set<String> localIgnoreList = ConcurrentHashMap.newKeySet();
    private final Set<String> tempIgnoreList = ConcurrentHashMap.newKeySet();
    private final Map<String, IgnoreEntry> ignoreDatabase = new ConcurrentHashMap<>();
    private final Set<String> queuedToIgnore = ConcurrentHashMap.newKeySet();
    private final Set<String> queuedToUnignore = ConcurrentHashMap.newKeySet();
    private final Set<String> recentlySeenPlayers = ConcurrentHashMap.newKeySet();

    // Action queue for rate limiting
    private final Queue<IgnoreAction> actionQueue = new LinkedList<>();
    private long lastActionTime = 0;

    // Patterns for parsing - Updated for 2b2t format
    private static final Pattern IGNORE_LIST_HEADER = Pattern.compile(".*\\[ Ignored players \\[<\\] (\\d+)/(\\d+) \\[>\\] \\].*");
    private static final Pattern IGNORE_LIST_PLAYER = Pattern.compile("^([a-zA-Z0-9_]{3,16})(?:\\s+\\[hard\\])?$");
    private static final Pattern IGNORE_SUCCESS = Pattern.compile(".*Permanently ignoring ([a-zA-Z0-9_]{3,16})\\. This is saved in /ignorelist\\.");
    private static final Pattern UNIGNORE_SUCCESS = Pattern.compile(".*No longer ignoring ([a-zA-Z0-9_]{3,16}).*");
    private static final Pattern PLAYER_OFFLINE = Pattern.compile(".*This player is not online.*");
    private static final Pattern PLAYER_MESSAGE = Pattern.compile("^<([a-zA-Z0-9_]{3,16})>");
    private static final Pattern NO_IGNORED_PLAYERS = Pattern.compile(".*You have not ignored any players.*");
    private static final Pattern BAD_COMMAND = Pattern.compile(".*Bad command.*");

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private File dataFile;
    private String lastAttemptedPlayer = null;

    public IgnoreSync() {
        super(
            Bep.CATEGORY,
            "IgnoreSync",
            "Advanced 2b2t ignore list manager with offline player queuing."
        );
    }

    @Override
    public void onActivate() {
        initializeDataFile();
        loadLocalList();
    }

    @Override
    public void onDeactivate() {
        saveLocalList();
        resetSyncState();
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WTable table = theme.table();
        fillTable(theme, table);
        return table;
    }

    private void fillTable(GuiTheme theme, WTable table) {
        // Clear the table first
        table.clear();

        // Status section
        table.add(theme.label("Local List:")).expandX();
        table.add(theme.label(String.valueOf(localIgnoreList.size()) + " players")).expandX();
        table.add(theme.label("")).expandX();
        table.row();

        table.add(theme.label("Server List:")).expandX();
        table.add(theme.label(String.valueOf(serverIgnoreList.size()) + " players")).expandX();
        table.add(theme.label("")).expandX();
        table.row();

        if (!queuedToIgnore.isEmpty()) {
            table.add(theme.label("Queued:")).expandX();
            table.add(theme.label(String.valueOf(queuedToIgnore.size()) + " players")).expandX();
            table.add(theme.label("")).expandX();
            table.row();
        }

        table.add(theme.horizontalSeparator()).expandX().expandCellX();
        table.row();

        // Header
        table.add(theme.label("Player")).expandX();
        table.add(theme.label("Status")).expandX();
        table.add(theme.label("Action")).expandX();
        table.row();

        // Sort players alphabetically
        List<String> sortedPlayers = new ArrayList<>(localIgnoreList);
        sortedPlayers.sort(String.CASE_INSENSITIVE_ORDER);

        // Limit display to prevent lag
        int displayCount = Math.min(sortedPlayers.size(), 50);
        if (sortedPlayers.size() > 50) {
            table.add(theme.label("Showing first 50 of " + sortedPlayers.size())).expandX().expandCellX();
            table.row();
        }

        // Add each player (limited)
        for (int i = 0; i < displayCount; i++) {
            String player = sortedPlayers.get(i);

            // Player name
            table.add(theme.label(player)).expandX();

            // Status
            if (showOnlineStatus.get()) {
                boolean isOnline = isPlayerOnline(player);
                boolean isQueued = queuedToIgnore.contains(player);
                boolean isOnServer = serverIgnoreList.contains(player);

                String statusText;
                if (isQueued) {
                    statusText = "Queued";
                } else if (isOnline) {
                    statusText = "Online";
                } else if (isOnServer) {
                    statusText = "Synced";
                } else {
                    statusText = "Local";
                }

                table.add(theme.label(statusText)).expandX();
            } else {
                table.add(theme.label("")).expandX();
            }

            // Remove button
            WMinus remove = table.add(theme.minus()).expandCellX().widget();
            remove.action = () -> {
                removeFromLocalList(player);
                // Don't call fillTable here, it will be refreshed on next GUI update
            };

            table.row();
        }

        // Add queued players section if any (limited)
        if (!queuedToIgnore.isEmpty()) {
            table.add(theme.horizontalSeparator()).expandX().expandCellX();
            table.row();
            table.add(theme.label("Queued Players")).expandX().expandCellX();
            table.row();

            List<String> queuedList = new ArrayList<>(queuedToIgnore);
            int queueDisplayCount = Math.min(queuedList.size(), 10);

            for (int i = 0; i < queueDisplayCount; i++) {
                String player = queuedList.get(i);
                table.add(theme.label(player)).expandX();
                table.add(theme.label("Waiting")).expandX();

                WMinus cancel = table.add(theme.minus()).expandCellX().widget();
                cancel.action = () -> {
                    queuedToIgnore.remove(player);
                    // Don't call fillTable here
                };

                table.row();
            }

            if (queuedList.size() > 10) {
                table.add(theme.label("... and " + (queuedList.size() - 10) + " more")).expandX().expandCellX();
                table.row();
            }
        }

        // Control buttons section
        table.add(theme.horizontalSeparator()).expandX().expandCellX();
        table.row();

        // First row of buttons
        WButton syncButton = table.add(theme.button("Sync Now")).expandX().widget();
        syncButton.action = this::startSync;

        WButton statusButton = table.add(theme.button("Status")).expandX().widget();
        statusButton.action = this::showStatus;

        WButton listButton = table.add(theme.button("List All")).expandX().widget();
        listButton.action = this::listLocal;

        table.row();

        // Second row of buttons
        WButton processQueueButton = table.add(theme.button("Process Queue")).expandX().widget();
        processQueueButton.action = () -> {
            processQueue();
        };

        WButton clearQueueButton = table.add(theme.button("Clear Queue")).expandX().widget();
        clearQueueButton.action = () -> {
            queuedToIgnore.clear();
            queuedToUnignore.clear();
            actionQueue.clear();
            info("Cleared all queues");
        };

        WButton backupButton = table.add(theme.button("Backup")).expandX().widget();
        backupButton.action = () -> {
            createBackup();
            info("Created backup of ignore list");
        };

        table.row();
    }

    @EventHandler
    private void onGameJoined(GameJoinedEvent event) {
        if (!autoSync.get()) return;

        // Check if we're on 2b2t
        if (mc.player != null && mc.getNetworkHandler() != null) {
            String serverAddress = mc.getNetworkHandler().getConnection().getAddress().toString();
            if (serverAddress.toLowerCase().contains("2b2t")) {
                syncTimer = syncDelay.get() * 20; // Convert seconds to ticks
                info("Will sync ignore list in " + syncDelay.get() + " seconds...");

                // Clear recent players on join
                recentlySeenPlayers.clear();
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        // Handle auto-sync timer
        if (syncTimer > 0) {
            syncTimer--;
            if (syncTimer == 0) {
                startSync();
            }
        }

        // Handle page request timing - finish collection after timeout
        if (isCollectingList && pageTimer > 0) {
            pageTimer--;
            if (pageTimer == 0) {
                if (nextPageToRequest > 0) {
                    // Request next page
                    ChatUtils.sendPlayerMsg("/ignorelist " + nextPageToRequest);
                    nextPageToRequest = -1;
                    waitingForResponse = true;
                    pageTimer = pageDelay.get();
                } else {
                    // No more messages received, finish collection
                    finishCollection();
                }
            }
        }

        // Check queued players periodically
        queueCheckTimer++;
        if (queueCheckTimer >= checkQueueDelay.get() * 20) {
            queueCheckTimer = 0;
            processQueuedPlayers();
        }

        // Process action queue
        processActionQueue();
    }

    @EventHandler
    private void onReceiveMessage(ReceiveMessageEvent event) {
        String message = event.getMessage().getString();

        // Track players who send messages (they're online)
        Matcher playerMessageMatcher = PLAYER_MESSAGE.matcher(message);
        if (playerMessageMatcher.find()) {
            String playerName = playerMessageMatcher.group(1);
            recentlySeenPlayers.add(playerName);

            // Check if this player is queued
            if (queuedToIgnore.contains(playerName)) {
                queuedToIgnore.remove(playerName);
                addAction(new IgnoreAction(playerName, true));
                if (verboseMode.get()) {
                    info("Player " + playerName + " is now online, queueing ignore action");
                }
            }
        }

        // Parse ignore list
        if (isCollectingList) {
            // Check for empty list
            Matcher noPlayersMatcher = NO_IGNORED_PLAYERS.matcher(message);
            if (noPlayersMatcher.find()) {
                if (hideMessages.get()) event.cancel();
                finishCollection();
                return;
            }

            // Check for the ignore list header with pages
            Matcher headerMatcher = IGNORE_LIST_HEADER.matcher(message);
            if (headerMatcher.find()) {
                waitingForResponse = false;
                int currentPage = Integer.parseInt(headerMatcher.group(1));
                int totalPages = Integer.parseInt(headerMatcher.group(2));

                if (verboseMode.get()) {
                    info("Parsing page " + currentPage + "/" + totalPages);
                }

                // The message contains newlines with player names
                String[] lines = message.split("\\n");
                for (String line : lines) {
                    // Skip the header line
                    if (line.contains("Ignored players")) continue;

                    // Try to extract player name from each line
                    line = line.trim();
                    if (!line.isEmpty()) {
                        // Remove [hard] tag if present
                        String playerName = line.replaceAll("\\s*\\[hard\\]\\s*$", "").trim();

                        // Validate player name
                        if (playerName.matches("[a-zA-Z0-9_]{3,16}")) {
                            tempIgnoreList.add(playerName);
                            if (verboseMode.get()) {
                                info("Found ignored player: " + playerName);
                            }
                        }
                    }
                }

                // Check if we need to get more pages
                if (currentPage < totalPages) {
                    // Schedule next page request after delay
                    waitingForResponse = true;
                    pageTimer = pageDelay.get();
                    nextPageToRequest = currentPage + 1;
                } else {
                    // All pages collected
                    pageTimer = 20; // Small delay before finishing
                }

                if (hideMessages.get()) event.cancel();
                return;
            }
        }

        // Handle ignore/unignore responses
        if (isSyncing || !actionQueue.isEmpty()) {
            Matcher ignoreSuccess = IGNORE_SUCCESS.matcher(message);
            if (ignoreSuccess.find()) {
                String player = ignoreSuccess.group(1);
                serverIgnoreList.add(player);
                if (verboseMode.get()) {
                    info("Successfully ignored: " + player);
                }
                if (hideMessages.get()) event.cancel();
                return;
            }

            Matcher unignoreSuccess = UNIGNORE_SUCCESS.matcher(message);
            if (unignoreSuccess.find()) {
                String player = unignoreSuccess.group(1);
                serverIgnoreList.remove(player);
                if (verboseMode.get()) {
                    info("Successfully unignored: " + player);
                }
                if (hideMessages.get()) event.cancel();
                return;
            }

            // Handle bad command error (means we tried to unignore with wrong syntax)
            Matcher badCommand = BAD_COMMAND.matcher(message);
            if (badCommand.find() && lastAttemptedPlayer != null) {
                error("Failed to process " + lastAttemptedPlayer + " - Bad command");
                lastAttemptedPlayer = null;
                if (hideMessages.get()) event.cancel();
                return;
            }

            Matcher playerOffline = PLAYER_OFFLINE.matcher(message);
            if (playerOffline.find() && lastAttemptedPlayer != null) {
                if (queueOffline.get()) {
                    queuedToIgnore.add(lastAttemptedPlayer);
                    warning(lastAttemptedPlayer + " is offline, added to queue");
                } else {
                    warning(lastAttemptedPlayer + " is offline, skipping");
                }
                lastAttemptedPlayer = null;
                if (hideMessages.get()) event.cancel();
                return;
            }
        }
    }

    public void startSync() {
        if (isSyncing || isCollectingList) {
            error("Already syncing!");
            return;
        }

        info("Starting ignore list sync...");

        // Always create backup before sync
        createBackup();

        resetSyncState();
        isSyncing = true;
        isCollectingList = true;
        tempIgnoreList.clear();

        // Request ignore list
        ChatUtils.sendPlayerMsg("/ignorelist");
        waitingForResponse = true;
        pageTimer = pageDelay.get() * 2; // Give extra time for first response
    }

    private void finishCollection() {
        isCollectingList = false;
        serverIgnoreList.clear();
        serverIgnoreList.addAll(tempIgnoreList);

        info("Collected " + serverIgnoreList.size() + " ignored players from server");

        if (verboseMode.get() && !serverIgnoreList.isEmpty()) {
            info("Server ignore list: " + String.join(", ", serverIgnoreList));
        }

        // Process the differences
        processSyncDifferences();
    }

    private void processSyncDifferences() {
        // First, merge server list into local list (these are already ignored on server)
        for (String player : serverIgnoreList) {
            if (!localIgnoreList.contains(player)) {
                localIgnoreList.add(player);
                if (verboseMode.get()) {
                    info("Added " + player + " from server to local list");
                }
            }
        }

        // Find players in local list but not on server (need to be ignored)
        Set<String> toIgnore = new HashSet<>(localIgnoreList);
        toIgnore.removeAll(serverIgnoreList);

        // Find players on server but not in local list (for optional removal)
        Set<String> onServerOnly = new HashSet<>(serverIgnoreList);
        onServerOnly.removeAll(localIgnoreList);

        int onlineCount = 0;
        int queuedCount = 0;

        // Process players that need to be ignored on server
        if (!toIgnore.isEmpty()) {
            info("Found " + toIgnore.size() + " players to ignore on server");

            if (autoIgnoreOnline.get()) {
                for (String player : toIgnore) {
                    if (isPlayerOnline(player) || recentlySeenPlayers.contains(player)) {
                        addAction(new IgnoreAction(player, true));
                        onlineCount++;
                        if (verboseMode.get()) {
                            info("Will ignore " + player + " (online)");
                        }
                    } else if (queueOffline.get()) {
                        queuedToIgnore.add(player);
                        queuedCount++;
                        if (verboseMode.get()) {
                            info("Queued " + player + " (offline)");
                        }
                    }
                }
            } else {
                info("Auto-ignore disabled, add manually or enable setting");
            }

            if (onlineCount > 0) {
                info("Processing " + onlineCount + " online players");
            }
            if (queuedCount > 0) {
                info("Queued " + queuedCount + " offline players");
            }
        }

        // Report if there are players on server not in local list
        if (!onServerOnly.isEmpty() && verboseMode.get()) {
            info("Server has " + onServerOnly.size() + " players not in local list");
        }

        // Save the updated local list
        saveLocalList();

        info("Sync complete! Local list: " + localIgnoreList.size() + " players, Queued: " + queuedToIgnore.size());
        isSyncing = false;
    }

    private void processQueuedPlayers() {
        if (queuedToIgnore.isEmpty()) return;

        List<String> toProcess = new ArrayList<>(queuedToIgnore);
        for (String player : toProcess) {
            if (isPlayerOnline(player) || recentlySeenPlayers.contains(player)) {
                queuedToIgnore.remove(player);
                addAction(new IgnoreAction(player, true));
                if (verboseMode.get()) {
                    info("Processing queued player: " + player);
                }
            }
        }
    }

    private void processActionQueue() {
        if (actionQueue.isEmpty()) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastActionTime < actionDelay.get()) return;

        IgnoreAction action = actionQueue.poll();
        if (action != null) {
            lastAttemptedPlayer = action.player;
            if (action.ignore) {
                // Use /ignorehard to permanently ignore
                ChatUtils.sendPlayerMsg("/ignorehard " + action.player);
                if (verboseMode.get()) {
                    info("Sending: /ignorehard " + action.player);
                }
            } else {
                // Use /ignorehard to remove from ignore list (same command for both)
                ChatUtils.sendPlayerMsg("/ignorehard " + action.player);
                if (verboseMode.get()) {
                    info("Sending: /ignorehard " + action.player + " (to unignore)");
                }
            }
            lastActionTime = currentTime;
        }
    }

    private void addAction(IgnoreAction action) {
        actionQueue.offer(action);
    }

    private boolean isPlayerOnline(String playerName) {
        if (mc.getNetworkHandler() == null) return false;

        for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
            if (entry.getProfile().getName().equalsIgnoreCase(playerName)) {
                return true;
            }
        }
        return false;
    }

    private void resetSyncState() {
        isSyncing = false;
        isCollectingList = false;
        syncTimer = 0;
        pageTimer = 0;
        waitingForResponse = false;
        tempIgnoreList.clear();
        lastAttemptedPlayer = null;
        nextPageToRequest = -1;
    }

    private void initializeDataFile() {
        try {
            Path meteorFolder = Path.of("meteor-client");
            if (!Files.exists(meteorFolder)) {
                Files.createDirectory(meteorFolder);
            }

            Path dataFolder = meteorFolder.resolve("ignore-sync");
            if (!Files.exists(dataFolder)) {
                Files.createDirectory(dataFolder);
            }

            dataFile = dataFolder.resolve(storageFile.get()).toFile();

            if (!dataFile.exists()) {
                dataFile.createNewFile();
                saveLocalList();
            }
        } catch (IOException e) {
            error("Failed to initialize data file: " + e.getMessage());
        }
    }

    private void loadLocalList() {
        if (dataFile == null || !dataFile.exists()) {
            info("No existing ignore list found, starting fresh");
            return;
        }

        try (FileReader reader = new FileReader(dataFile)) {
            Type type = new TypeToken<IgnoreData>(){}.getType();
            IgnoreData data = gson.fromJson(reader, type);

            if (data != null) {
                localIgnoreList.clear();
                if (data.ignoredPlayers != null && !data.ignoredPlayers.isEmpty()) {
                    localIgnoreList.addAll(data.ignoredPlayers);
                }

                ignoreDatabase.clear();
                if (data.playerDatabase != null) {
                    ignoreDatabase.putAll(data.playerDatabase);
                }

                if (data.queuedPlayers != null && !data.queuedPlayers.isEmpty()) {
                    queuedToIgnore.clear();
                    queuedToIgnore.addAll(data.queuedPlayers);
                }

                info("Loaded " + localIgnoreList.size() + " ignored players, " +
                     queuedToIgnore.size() + " queued");

                if (verboseMode.get() && !localIgnoreList.isEmpty()) {
                    info("Local list: " + String.join(", ", localIgnoreList));
                }
            } else {
                info("Data file exists but is empty, starting fresh");
            }
        } catch (Exception e) {
            error("Failed to load ignore list: " + e.getMessage());
            error("Starting with empty list");
        }
    }

    private void saveLocalList() {
        if (dataFile == null) return;

        try (FileWriter writer = new FileWriter(dataFile)) {
            IgnoreData data = new IgnoreData();
            data.ignoredPlayers = new ArrayList<>(localIgnoreList);
            data.queuedPlayers = new ArrayList<>(queuedToIgnore);
            data.lastUpdated = System.currentTimeMillis();
            data.playerDatabase = new HashMap<>(ignoreDatabase);

            gson.toJson(data, writer);

            if (verboseMode.get()) {
                info("Saved " + localIgnoreList.size() + " ignored players");
            }
        } catch (IOException e) {
            error("Failed to save ignore list: " + e.getMessage());
        }
    }

    private void createBackup() {
        if (dataFile == null || !dataFile.exists()) return;

        try {
            // Single backup file that gets overwritten
            String backupName = storageFile.get().replace(".json", "_backup.json");
            Path backupPath = dataFile.toPath().getParent().resolve(backupName);
            Files.copy(dataFile.toPath(), backupPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            if (verboseMode.get()) {
                info("Created backup: " + backupName);
            }
        } catch (IOException e) {
            error("Failed to create backup: " + e.getMessage());
        }
    }

    // Public methods for commands
    public void addToLocalList(String playerName) {
        if (localIgnoreList.contains(playerName)) {
            warning(playerName + " is already in the ignore list");
            return;
        }

        localIgnoreList.add(playerName);

        IgnoreEntry entry = ignoreDatabase.getOrDefault(playerName, new IgnoreEntry());
        entry.username = playerName;
        entry.addedDate = System.currentTimeMillis();
        entry.addedBy = mc.player != null ? mc.player.getName().getString() : "Unknown";
        ignoreDatabase.put(playerName, entry);

        // Check if already ignored on server
        if (serverIgnoreList.contains(playerName)) {
            info("Added " + playerName + " to local list (already ignored on server)");
        }
        // Check if online and add to action queue
        else if (isPlayerOnline(playerName) || recentlySeenPlayers.contains(playerName)) {
            addAction(new IgnoreAction(playerName, true));
            info("Added " + playerName + " to ignore list (online, will ignore)");
        } else if (queueOffline.get()) {
            queuedToIgnore.add(playerName);
            info("Added " + playerName + " to ignore list (offline, queued)");
        } else {
            info("Added " + playerName + " to local ignore list");
        }

        saveLocalList();
    }

    public void removeFromLocalList(String playerName) {
        if (localIgnoreList.remove(playerName)) {
            queuedToIgnore.remove(playerName);

            // Add unignore action if on server list
            if (serverIgnoreList.contains(playerName)) {
                addAction(new IgnoreAction(playerName, false));
                info("Removing " + playerName + " from server ignore list...");
            }

            saveLocalList();
            info("Removed " + playerName + " from local ignore list");
        } else {
            error(playerName + " not found in ignore list");
        }
    }

    public void listLocal() {
        if (localIgnoreList.isEmpty() && queuedToIgnore.isEmpty()) {
            info("Ignore lists are empty");
            return;
        }

        if (!localIgnoreList.isEmpty()) {
            info("Ignored players (" + localIgnoreList.size() + "):");
            List<String> sorted = new ArrayList<>(localIgnoreList);
            sorted.sort(String.CASE_INSENSITIVE_ORDER);

            // Limit output to prevent spam
            int count = 0;
            for (String player : sorted) {
                if (count >= 20) {
                    info("... and " + (sorted.size() - 20) + " more players");
                    break;
                }
                boolean online = isPlayerOnline(player);
                boolean onServer = serverIgnoreList.contains(player);
                String status = online ? " [Online]" : (onServer ? " [Synced]" : " [Local]");
                info("- " + player + status);
                count++;
            }
        }

        if (!queuedToIgnore.isEmpty()) {
            info("Queued to ignore (" + queuedToIgnore.size() + "):");
            int count = 0;
            for (String player : queuedToIgnore) {
                if (count >= 10) {
                    info("... and " + (queuedToIgnore.size() - 10) + " more");
                    break;
                }
                info("- " + player + " [Queued]");
                count++;
            }
        }
    }

    public void clearLocal() {
        createBackup();
        localIgnoreList.clear();
        ignoreDatabase.clear();
        queuedToIgnore.clear();
        queuedToUnignore.clear();
        actionQueue.clear();
        saveLocalList();
        info("Cleared all ignore lists and queues");
    }

    public void processQueue() {
        if (queuedToIgnore.isEmpty()) {
            info("No players in queue");
            return;
        }

        info("Processing " + queuedToIgnore.size() + " queued players...");
        processQueuedPlayers();
    }

    public void showStatus() {
        info("=== IgnoreSync Status ===");
        info("Local List: " + localIgnoreList.size() + " players");
        info("Server List: " + serverIgnoreList.size() + " players (last sync)");
        info("Queued: " + queuedToIgnore.size() + " players");
        info("Action Queue: " + actionQueue.size() + " pending");

        // Show differences
        Set<String> notOnServer = new HashSet<>(localIgnoreList);
        notOnServer.removeAll(serverIgnoreList);

        Set<String> notInLocal = new HashSet<>(serverIgnoreList);
        notInLocal.removeAll(localIgnoreList);

        if (!notOnServer.isEmpty()) {
            info("Not on server: " + notOnServer.size() + " players");
            if (verboseMode.get() && notOnServer.size() <= 10) {
                info("  " + String.join(", ", notOnServer));
            }
        }

        if (!notInLocal.isEmpty()) {
            info("On server but not local: " + notInLocal.size() + " players");
            if (verboseMode.get() && notInLocal.size() <= 10) {
                info("  " + String.join(", ", notInLocal));
            }
        }

        // Show online players from list
        int onlineCount = 0;
        List<String> onlinePlayers = new ArrayList<>();
        for (String player : localIgnoreList) {
            if (isPlayerOnline(player)) {
                onlineCount++;
                onlinePlayers.add(player);
            }
        }
        info("Currently online: " + onlineCount + " ignored players");
        if (verboseMode.get() && onlineCount > 0 && onlineCount <= 5) {
            info("  " + String.join(", ", onlinePlayers));
        }
    }

    // Data classes
    private static class IgnoreData {
        List<String> ignoredPlayers = new ArrayList<>();
        List<String> queuedPlayers = new ArrayList<>();
        long lastUpdated;
        Map<String, IgnoreEntry> playerDatabase = new HashMap<>();
    }

    private static class IgnoreEntry {
        String username;
        long addedDate;
        String addedBy;
        String reason;
        List<String> aliases = new ArrayList<>();
    }

    private static class IgnoreAction {
        final String player;
        final boolean ignore;

        IgnoreAction(String player, boolean ignore) {
            this.player = player;
            this.ignore = ignore;
        }
    }
}
