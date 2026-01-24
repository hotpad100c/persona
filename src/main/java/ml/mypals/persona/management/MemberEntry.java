package ml.mypals.persona.management;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MemberEntry {
    @SerializedName("category_id")
    private String categoryId;

    @SerializedName("category_name")
    private String categoryName;

    @SerializedName("max_characters")
    private int maxCharacters;//Max character number

    @SerializedName("shou_join_message")
    private boolean displayOnJoin;

    @SerializedName("can_use_roster")
    private boolean canUseRoster;

    @SerializedName("roster_level")
    private int rosterLevel;//0 : NO UI; 1:UI, NO DESC; 2:FULL

    @SerializedName("priority")
    private int priority;

    @SerializedName("members")
    private List<String> members;

    public MemberEntry(String categoryId, String categoryName, int maxCharacters,  boolean displayOnJoin, boolean canUseRoster, int rosterLevel) {
        this(categoryId, categoryName, maxCharacters, displayOnJoin, canUseRoster, 0, rosterLevel);
    }

    public MemberEntry(String categoryId, String categoryName, int maxCharacters, boolean displayOnJoin, boolean canUseRoster, int priority, int rosterLevel) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.maxCharacters = maxCharacters;
        this.displayOnJoin = displayOnJoin;
        this.canUseRoster = canUseRoster;
        this.priority = priority;
        this.rosterLevel = rosterLevel;
        this.members = new ArrayList<>();
    }

    public String getCategoryId() {
        return categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public int getMaxCharacters() {
        return maxCharacters;
    }

    public boolean canUseRoster() {
        return canUseRoster;
    }

    public int getPriority() {
        return priority;
    }
    public int getRosterLevel(){
        return rosterLevel;
    }
    public void setMaxCharacters(int maxCharacters) {
        this.maxCharacters = maxCharacters;
    }
    public void setShowOnJoin(boolean showOnJoin) {
        this.displayOnJoin = showOnJoin;
    }
    public void setCanUseRoster(boolean canUseRoster) {
        this.canUseRoster = canUseRoster;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public void setRosterLevel(int rosterLevel){
        this.rosterLevel = rosterLevel;
    }
    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }
    public boolean showOnJoin() {
        return displayOnJoin;
    }
    public boolean addMember(UUID playerId) {
        String id = playerId.toString();
        if (!members.contains(id)) {
            members.add(id);
            return true;
        }
        return false;
    }

    public boolean removeMember(UUID playerId) {
        return members.remove(playerId.toString());
    }

    public boolean hasMember(UUID playerId) {
        return members.contains(playerId.toString());
    }

    public List<UUID> getMemberUUIDs() {
        List<UUID> uuids = new ArrayList<>();
        for (String id : members) {
            try {
                uuids.add(UUID.fromString(id));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return uuids;
    }

    public int getMemberCount() {
        return members.size();
    }



    @Override
    public String toString() {
        return String.format("%s{id='%s', members=%d, maxChars=%d, canRoster=%b, priority=%d, rosterLevel=%d}",
                categoryName, categoryId, members.size(), maxCharacters, canUseRoster, priority,rosterLevel);
    }
}