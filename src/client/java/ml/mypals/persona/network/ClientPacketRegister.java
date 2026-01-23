package ml.mypals.persona.network;

import ml.mypals.persona.PersonaClient;
import ml.mypals.persona.network.packets.category.PlayerCategoryDataPayload;
import ml.mypals.persona.network.packets.roster.*;
import ml.mypals.persona.screen.RosterRecordScreen;
import ml.mypals.persona.screen.RosterViewScreen;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

public class ClientPacketRegister {
    public static void initialize(){
        ClientPlayNetworking.registerGlobalReceiver(PlayerCategoryDataPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                PersonaClient.playerCategoryData = payload.playerCategoryData();
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(AddToRosterS2CPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                if(PersonaClient.playerCategoryData != null && PersonaClient.playerCategoryData.canUseRoster()){
                    context.client().setScreen(new RosterRecordScreen(payload.data()));
                }
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(CharacterSyncS2CPayload.TYPE,(payload, context)-> {
            context.client().execute(() -> {
                PersonaClient.getCharacterManager().handleSync(payload);
                ClientPlayNetworking.send(new RosterRequestC2SPayload(PersonaClient.getRosterDataManager().getRecorded()));
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(RosterDeltaSyncS2CPayload.TYPE,(payload, context)-> {
            context.client().execute(() -> {
                PersonaClient.getRosterDataManager().handleSync(payload);
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(OpenRosterViewScreenPayload.TYPE,(payload, context)-> {
            context.client().execute(() -> {
                PersonaClient.getRosterDataManager().getCurrentRoster().ifPresent(rosterData ->{
                    if(PersonaClient.playerCategoryData.canUseRoster()){
                        Minecraft.getInstance().setScreen(new RosterViewScreen(rosterData));
                    }
                });
            });
        });
    }
}
