package ml.mypals.persona.roster;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BookMarkManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Path CACHE_DIR = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("persona")
            .resolve("client_bookmarks");

    private static BookMarkSet bookMarkSet;

    private Path getCacheFilePath(String characterId) {
        String safeFileName = "bookmarks" + "_" + characterId;
        return CACHE_DIR.resolve(safeFileName);
    }

    public void loadFromCache(String characterId) {
        Path path = getCacheFilePath(characterId);
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path.getParent());
                Files.createFile(path);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            BookMarkSet data = GSON.fromJson(reader, BookMarkSet.class);
            if (data == null || data.getOwnerCharacter() == null) {
                bookMarkSet = new BookMarkSet(characterId);
            } else {
                bookMarkSet = data;
            }
        } catch (Exception e) {
            e.printStackTrace();
            try { Files.deleteIfExists(path); } catch (Exception ignored) {}
        }
    }

    public List<String> getActiveMarks() {
        return bookMarkSet.getActiveMarks();
    }

    public List<String> getAvailableMarks() {
        return bookMarkSet.getAvailableMarks();
    }

    public void add(String mark) {
        bookMarkSet.addMark(mark);
        saveToCache();
    }

    public boolean replace(String oldMark, String newMark) {
        boolean result = bookMarkSet.replace(oldMark, newMark);
        if (result) saveToCache();
        return result;
    }

    public boolean canReplace(String oldMark, String newMark) {
        return bookMarkSet.canReplace(oldMark, newMark);
    }

    public void remove(String mark) {
        bookMarkSet.removeMark(mark);
        saveToCache();
    }

    public void activate(String mark) {
        bookMarkSet.activate(mark);
        saveToCache();
    }

    public void deactivate(String mark) {
        bookMarkSet.deactivate(mark);
        saveToCache();
    }

    private void saveToCache() {
        if (bookMarkSet == null) {
            return;
        }

        Path path = getCacheFilePath(bookMarkSet.getOwnerCharacter());
        try (Writer writer = Files.newBufferedWriter(path)) {
            GSON.toJson(bookMarkSet, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class BookMarkSet {

        public BookMarkSet(String ownerCharacter, ArrayList<String> activeMarks, ArrayList<String> availableMarks) {
            this.ownerCharacter = ownerCharacter;
            this.activeMarks = activeMarks;
            this.availableMarks = availableMarks;
        }

        public BookMarkSet(String ownerCharacter) {
            this.ownerCharacter = ownerCharacter;
            this.activeMarks = new ArrayList<>();
            this.availableMarks = new ArrayList<>();
        }

        @SerializedName("owner")
        private String ownerCharacter;

        @SerializedName("active")
        private ArrayList<String> activeMarks;

        @SerializedName("available")
        private ArrayList<String> availableMarks;

        public String getOwnerCharacter() {
            return ownerCharacter;
        }

        public ArrayList<String> getActiveMarks() {
            return activeMarks;
        }

        public ArrayList<String> getAvailableMarks() {
            return availableMarks;
        }

        public void addMark(String newMark) {
            if (!availableMarks.contains(newMark)) {
                availableMarks.add(newMark);
            }
        }

        public boolean replace(String oldMark, String newMark) {
            if (availableMarks.contains(oldMark)) {
                int index = availableMarks.indexOf(oldMark);
                availableMarks.set(index, newMark);

                if (activeMarks.contains(oldMark)) {
                    int activeIndex = activeMarks.indexOf(oldMark);
                    activeMarks.set(activeIndex, newMark);
                }

                return true;
            }
            return false;
        }

        public boolean canReplace(String oldMark, String newMark) {
            return Objects.equals(oldMark, newMark) || !availableMarks.contains(newMark);
        }

        public void removeMark(String mark) {
            availableMarks.remove(mark);
            activeMarks.remove(mark);
        }

        public void activate(String mark) {
            if (!activeMarks.contains(mark)) {
                activeMarks.add(mark);
            }
        }

        public void deactivate(String mark) {
            activeMarks.remove(mark);
        }
    }
}