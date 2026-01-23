package ml.mypals.persona.mixin.client;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import ml.mypals.persona.PersonaClient;
import ml.mypals.persona.roster.ClientRosterDataManager;
import net.minecraft.client.entity.ClientAvatarState;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.concurrent.atomic.AtomicReference;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin extends Player {
    @Shadow @Final private ClientAvatarState clientAvatarState;

    public AbstractClientPlayerMixin(Level level, GameProfile gameProfile) {
        super(level, gameProfile);
    }
    @Override
    public @NotNull Component getName() {
        if(this.isCustomNameVisible()){
            Component component = this.getCustomName();
            return component!= null? component:Component.literal(this.getGameProfile().name());
        }
        return Component.literal(getNameIfRecorded());
    }
    @Unique
    public String getNameIfRecorded(){
        ClientRosterDataManager clientRosterDataManager = PersonaClient.getRosterDataManager();
        Property property = this.getGameProfile().properties().get("persona_character_id").stream().findFirst().orElse(null);

        AtomicReference<String> name = new AtomicReference<>("???");
        if(property != null){
            clientRosterDataManager.getEntry(this.stringUUID,property.value()).ifPresent(rosterEntry -> {
                name.set(rosterEntry.getNickname());
            });
        }
        return name.get();
    }
}
