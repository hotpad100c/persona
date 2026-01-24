package ml.mypals.persona.management;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import ml.mypals.persona.Persona;
import ml.mypals.persona.network.packets.category.PlayerCategoryDataPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class MemberCategoryManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path DATA_DIR = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("persona")
            .resolve("categories");

    private static final String CATEGORIES_FILE = "member_categories.json";
    private static final String DEFAULT_CATEGORY = "default";

    private final Map<String, MemberEntry> categories = new HashMap<>();

    public MemberCategoryManager() {
        try {
            Files.createDirectories(DATA_DIR);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create categories data directory", e);
        }

        loadCategories();
        ensureDefaultCategory();
    }

    private void ensureDefaultCategory() {
        if (!categories.containsKey(DEFAULT_CATEGORY)) {
            MemberEntry defaultEntry = new MemberEntry(
                    DEFAULT_CATEGORY,
                    "DEFAULT",
                    3,
                    false,
                    true,
                    0,
                    1
            );
            categories.put(DEFAULT_CATEGORY, defaultEntry);
            saveCategories();
        }
    }

    private void loadCategories() {
        File file = DATA_DIR.resolve(CATEGORIES_FILE).toFile();
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                Map<String, MemberEntry> loaded = GSON.fromJson(
                        reader,
                        new TypeToken<Map<String, MemberEntry>>(){}.getType()
                );
                if (loaded != null) {
                    categories.putAll(loaded);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveCategories() {
        File file = DATA_DIR.resolve(CATEGORIES_FILE).toFile();
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(categories, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean createOrUpdateCategory(String categoryId, String categoryName,
                                          int maxCharacters, boolean shownOnJoin, boolean canUseRoster,
                                          int priority, int rosterLevel) {
        MemberEntry entry = categories.get(categoryId);
        if (entry != null) {
            entry.setCategoryName(categoryName);
            entry.setMaxCharacters(maxCharacters);
            entry.setShowOnJoin(shownOnJoin);
            entry.setCanUseRoster(canUseRoster);
            entry.setPriority(priority);
            entry.setRosterLevel(rosterLevel);
        } else {
            entry = new MemberEntry(categoryId, categoryName, maxCharacters, shownOnJoin, canUseRoster, priority, rosterLevel);
            categories.put(categoryId, entry);
        }
        saveCategories();
        return true;
    }

    public boolean deleteCategory(String categoryId) {
        if (DEFAULT_CATEGORY.equals(categoryId)) {
            return false;
        }

        MemberEntry category = categories.remove(categoryId);
        if (category != null) {
            MemberEntry defaultCategory = categories.get(DEFAULT_CATEGORY);
            for (UUID playerId : category.getMemberUUIDs()) {
                defaultCategory.addMember(playerId);
            }
            saveCategories();
            return true;
        }
        return false;
    }

    public Optional<MemberEntry> getCategory(String categoryId) {
        return Optional.ofNullable(categories.get(categoryId));
    }

    public Map<String, MemberEntry> getAllCategories() {
        return new HashMap<>(categories);
    }

    public boolean setPlayerCategory(ServerPlayer serverPlayer, UUID playerId, String categoryId) {
        MemberEntry targetCategory = categories.get(categoryId);
        if (targetCategory == null) {
            return false;
        }

        for (MemberEntry category : categories.values()) {
            category.removeMember(playerId);
        }

        targetCategory.addMember(playerId);
        saveCategories();
        PlayerCategoryData playerCategoryData = createPlayerCategoryData(playerId);
        if(playerCategoryData!=null)ServerPlayNetworking.send(serverPlayer,new PlayerCategoryDataPayload(playerCategoryData));
        return true;
    }

    public MemberEntry getPlayerCategory(UUID playerId) {
        return categories.values().stream()
                .filter(category -> category.hasMember(playerId))
                .max(Comparator.comparingInt(MemberEntry::getPriority))
                .orElse(categories.get(DEFAULT_CATEGORY));
    }

    public PlayerCategoryData createPlayerCategoryData(UUID playerId) {
        MemberEntry category = getPlayerCategory(playerId);
        return new PlayerCategoryData(playerId, category);
    }

    public int getPlayerMaxCharacters(UUID playerId) {
        return getPlayerCategory(playerId).getMaxCharacters();
    }

    public boolean canPlayerUseRoster(UUID playerId) {
        return getPlayerCategory(playerId).canUseRoster();
    }
    public boolean shouldPlayerShouJoinMessage(UUID playerId) {
        return getPlayerCategory(playerId).showOnJoin();
    }

    public boolean canCreateCharacter(UUID playerId, int currentCharacterCount) {
        int maxCharacters = getPlayerMaxCharacters(playerId);
        return currentCharacterCount < maxCharacters;
    }

    public void addPlayerToDefault(ServerPlayer serverPlayer, UUID playerId) {
        boolean inAnyCategory = categories.values().stream()
                .anyMatch(category -> category.hasMember(playerId));

        if (!inAnyCategory) {
            MemberEntry defaultCategory = categories.get(DEFAULT_CATEGORY);
            defaultCategory.addMember(playerId);
            saveCategories();
        }
        PlayerCategoryData playerCategoryData = createPlayerCategoryData(playerId);
        if(playerCategoryData!=null)ServerPlayNetworking.send(serverPlayer,new PlayerCategoryDataPayload(playerCategoryData));

    }

    public void removePlayer(UUID playerId) {
        boolean modified = false;
        for (MemberEntry category : categories.values()) {
            if (category.removeMember(playerId)) {
                modified = true;
            }
        }
        if (modified) {
            saveCategories();
        }
    }
}