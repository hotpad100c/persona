package ml.mypals.persona.mixin;

import ml.mypals.persona.Persona;
import ml.mypals.persona.characterData.PlayerCharacterStorage;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public class PlayerMixin {
	@Shadow public int experienceLevel;

	@Inject(at = @At("HEAD"), method = "getName", cancellable = true)
	private void persona$getName(CallbackInfoReturnable<Component> cir) {
		if (!((Object)this instanceof ServerPlayer player)) {
			return;
		}
		PlayerCharacterStorage data = Persona.getCharacterManager().getPlayerCharacters(player,player.getUUID());
		if(data != null) {
			data.getCurrentCharacter().ifPresent(character -> {
				cir.setReturnValue(Component.literal(character.getCustomName()));
			});
		}
	}
}