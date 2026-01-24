package ml.mypals.persona.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import ml.mypals.persona.management.MemberCategoryManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import static ml.mypals.persona.Persona.getMemberCategoryManager;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
    @Shadow public abstract ServerPlayer getPlayer();

    @WrapOperation(method = "removePlayerFromWorld", at = @At(target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V", value = "INVOKE"))
    public void removePlayerFromWorld(PlayerList instance, Component component, boolean bl, Operation<Void> original) {
        MemberCategoryManager memberCategoryManager = getMemberCategoryManager();
        ServerPlayer serverPlayer = this.getPlayer();
        memberCategoryManager.addPlayerToDefault(serverPlayer, serverPlayer.getUUID());
        if(memberCategoryManager.shouldPlayerShouJoinMessage(serverPlayer.getUUID())){
            original.call(instance, component, bl);
        };

    }
}