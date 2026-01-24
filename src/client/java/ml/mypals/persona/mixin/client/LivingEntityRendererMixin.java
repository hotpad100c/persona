package ml.mypals.persona.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {
    @WrapMethod(method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;D)Z")
    private boolean lucidity$shouldShowName(LivingEntity livingEntity, double d, Operation<Boolean> original) {
        if(livingEntity instanceof Player player && Minecraft.getInstance().player != null && !Minecraft.getInstance().player.isSpectator()){
            return false;
        }
        return original.call(livingEntity,d);
    }
}
