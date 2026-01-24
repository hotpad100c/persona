package ml.mypals.persona;

import ml.mypals.persona.characterData.CharacterData;
import ml.mypals.persona.characterData.CharacterManager;
import ml.mypals.persona.commands.CategoryCommand;
import ml.mypals.persona.commands.CharacterCommand;
import ml.mypals.persona.items.ModItems;
import ml.mypals.persona.items.rosterData.RosterDataManager;
import ml.mypals.persona.management.MemberCategoryManager;
import ml.mypals.persona.network.ServerPacketRegister;
import ml.mypals.persona.network.packets.roster.CharacterSyncS2CPayload;
import ml.mypals.persona.skinHandler.SimpleSkinChanger;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class Persona implements ModInitializer {
	public static final String MOD_ID = "persona";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static CharacterManager characterManager;
	private static RosterDataManager rosterDataManager;
	private static MemberCategoryManager memberCategoryManager;
	@Override
	public void onInitialize() {
		characterManager = new CharacterManager();
		rosterDataManager = new RosterDataManager();
		memberCategoryManager = new MemberCategoryManager();
		SimpleSkinChanger.initialize();
		ModItems.initialize();
		ServerPacketRegister.initialize();
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			new CharacterCommand(characterManager).register(dispatcher);
			new CategoryCommand(memberCategoryManager).register(dispatcher);
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->{
			characterManager.unloadPlayer(handler.getPlayer().getUUID());
			rosterDataManager.unloadPlayer(handler.getPlayer().getUUID());
			}
		);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->{
				characterManager.loadPlayer(handler.getPlayer(),handler.getPlayer().getUUID());
				rosterDataManager.loadPlayer(handler.getPlayer(), handler.getPlayer().getUUID());
				Optional<CharacterData> characterData = characterManager.getPlayerCharacters(handler.getPlayer(), handler.getPlayer().getUUID()).getCurrentCharacter();
				characterData.ifPresent(data -> ServerPlayNetworking.send(handler.getPlayer(),new CharacterSyncS2CPayload(data)));
			}
		);

	}
	public static CharacterManager getCharacterManager(){
		return characterManager;
	}
	public static RosterDataManager getRosterDataManager(){
		return rosterDataManager;
	}
	public static MemberCategoryManager getMemberCategoryManager(){
		return memberCategoryManager;
	}


}