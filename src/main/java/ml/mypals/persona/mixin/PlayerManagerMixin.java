package ml.mypals.persona.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import ml.mypals.persona.management.MemberCategoryManager;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import static ml.mypals.persona.Persona.getMemberCategoryManager;

@Mixin(PlayerList.class)
public class PlayerManagerMixin {
    @WrapOperation(method = "placeNewPlayer", at = @At(target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V", value = "INVOKE"))
    public void placeNewPlayer(PlayerList instance, Component component, boolean bl, Operation<Void> original, @Local(argsOnly = true) ServerPlayer serverPlayer) {
        MemberCategoryManager memberCategoryManager = getMemberCategoryManager();
        memberCategoryManager.addPlayerToDefault(serverPlayer, serverPlayer.getUUID());
        if(memberCategoryManager.shouldPlayerShouJoinMessage(serverPlayer.getUUID())){
            original.call(instance, component, bl);
        };

    }
}
