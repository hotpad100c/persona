package ml.mypals.persona;

import com.sun.jna.platform.win32.Psapi;
import ml.mypals.persona.fakePlayer.FakePlayerFactory;
import ml.mypals.persona.items.rosterData.AddCharacterToRosterData;
import ml.mypals.persona.items.rosterData.PlayerRosterData;
import ml.mypals.persona.management.PlayerCategoryData;
import ml.mypals.persona.network.ClientPacketRegister;
import ml.mypals.persona.network.packets.roster.AddToRosterC2SPayload;
import ml.mypals.persona.network.packets.roster.RosterRequestC2SPayload;
import ml.mypals.persona.roster.BookMarkManager;
import ml.mypals.persona.roster.ClientCharacterManager;
import ml.mypals.persona.roster.ClientRosterDataManager;
import ml.mypals.persona.screen.CharacterNameHud;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.resources.Identifier;

import static ml.mypals.persona.Persona.MOD_ID;
import static ml.mypals.persona.fakePlayer.FakePlayerFactory.clearCache;

public class PersonaClient implements ClientModInitializer {
	public static PlayerCategoryData playerCategoryData;
	private static ClientCharacterManager characterManager;
	private static ClientRosterDataManager playerRosterData;
	private static BookMarkManager bookMarkManager;
	@Override
	public void onInitializeClient() {
		ClientPacketRegister.initialize();
		characterManager = new ClientCharacterManager();
		playerRosterData = new ClientRosterDataManager();
		bookMarkManager = new BookMarkManager();

		ClientPlayConnectionEvents.DISCONNECT.register((clientPacketListener,minecraft) -> {
			characterManager.clear();
			playerRosterData.clear();

			FakePlayerFactory.clearCache();
		});
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, Identifier.fromNamespaceAndPath(MOD_ID, "character_name"), CharacterNameHud::render);
	}
	public static ClientRosterDataManager getRosterDataManager(){
		return playerRosterData;
	}
	public static ClientCharacterManager getCharacterManager(){
		return characterManager;
	}
	public static BookMarkManager getBookMarkManager(){
		return bookMarkManager;
	}
}