package ml.mypals.persona.network;

import net.minecraft.resources.Identifier;

import static ml.mypals.persona.Persona.MOD_ID;

public class PersonaNetworkingConstants {
    public static final Identifier PLAYER_CATEGORY = Identifier.fromNamespaceAndPath(MOD_ID, "player_category");
    public static final Identifier ADD_TO_ROSTER_S2C = Identifier.fromNamespaceAndPath(MOD_ID, "add_to_roster_s2c");
    public static final Identifier ADD_TO_ROSTER_C2S = Identifier.fromNamespaceAndPath(MOD_ID, "add_to_roster_c2s");

    public static final Identifier ROSTER_SYNC_S2C = Identifier.fromNamespaceAndPath(MOD_ID, "roster_sync_s2c");
    public static final Identifier ROSTER_SYNC_C2S = Identifier.fromNamespaceAndPath(MOD_ID, "roster_sync_c2s");
    public static final Identifier ROSTER_REQUEST_C2S = Identifier.fromNamespaceAndPath(MOD_ID, "roster_request_c2s");

    public static final Identifier ROSTER_DELTA_SYNC_S2C = Identifier.fromNamespaceAndPath(MOD_ID, "roster_delta_sync_s2c");
    public static final Identifier CHARACTER_SYNC = Identifier.fromNamespaceAndPath(MOD_ID, "character_sync");

    public static final Identifier OPEN_ROSTER = Identifier.fromNamespaceAndPath(MOD_ID, "open_roster");
}
