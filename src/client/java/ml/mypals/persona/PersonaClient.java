package ml.mypals.persona;

import ml.mypals.persona.fakePlayer.FakePlayerFactory;
import ml.mypals.persona.items.rosterData.AddCharacterToRosterData;
import ml.mypals.persona.items.rosterData.PlayerRosterData;
import ml.mypals.persona.management.PlayerCategoryData;
import ml.mypals.persona.network.ClientPacketRegister;
import ml.mypals.persona.network.packets.roster.AddToRosterC2SPayload;
import ml.mypals.persona.network.packets.roster.RosterRequestC2SPayload;
import ml.mypals.persona.roster.ClientCharacterManager;
import ml.mypals.persona.roster.ClientRosterDataManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import static ml.mypals.persona.fakePlayer.FakePlayerFactory.clearCache;

public class PersonaClient implements ClientModInitializer {
	public static PlayerCategoryData playerCategoryData;
	private static ClientCharacterManager characterManager;
	private static ClientRosterDataManager playerRosterData;
	@Override
	public void onInitializeClient() {
		ClientPacketRegister.initialize();
		characterManager = new ClientCharacterManager();
		playerRosterData = new ClientRosterDataManager();
		ClientPlayConnectionEvents.JOIN.register((clientPacketListener, packetSender, minecraft) -> {
			//NO OP?
		});
		ClientPlayConnectionEvents.DISCONNECT.register((clientPacketListener,minecraft) -> {
			characterManager.clear();
			playerRosterData.clear();

			FakePlayerFactory.clearCache();
		});
	}
	public static ClientRosterDataManager getRosterDataManager(){
		return playerRosterData;
	}
	public static ClientCharacterManager getCharacterManager(){
		return characterManager;
	}
}