package ml.mypals.persona.items;

import ml.mypals.persona.Persona;
import ml.mypals.persona.characterData.CharacterData;
import ml.mypals.persona.characterData.CharacterManager;
import ml.mypals.persona.characterData.PlayerCharacterStorage;
import ml.mypals.persona.items.rosterData.AddCharacterToRosterData;
import ml.mypals.persona.items.rosterData.PlayerRosterData;
import ml.mypals.persona.items.rosterData.RosterDataManager;
import ml.mypals.persona.network.packets.roster.AddToRosterS2CPayload;
import ml.mypals.persona.network.packets.roster.OpenRosterViewScreenPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.ChatFormatting;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class RosterItem extends Item {

    public RosterItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult use(Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.SUCCESS; // 客户端消费事件

        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

        ItemStack stack = player.getItemInHand(hand);

        double reachDistance = 4.5;
        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 lookVec = player.getViewVector(1.0F);
        Vec3 endPos = eyePos.add(lookVec.scale(reachDistance));

        AABB searchBox = player.getBoundingBox()
                .expandTowards(lookVec.scale(reachDistance))
                .inflate(1.0);

        EntityHitResult entityHit = null;
        double closestDistance = reachDistance;

        for (Entity entity : level.getEntities(player, searchBox, e -> !e.isSpectator() && e.isPickable())) {
            AABB entityBox = entity.getBoundingBox().inflate(entity.getPickRadius());
            Optional<Vec3> hitPos = entityBox.clip(eyePos, endPos);

            if (hitPos.isPresent()) {
                double distance = eyePos.distanceTo(hitPos.get());
                if (distance < closestDistance) {
                    closestDistance = distance;
                    entityHit = new EntityHitResult(entity, hitPos.get());
                }
            }
        }

        if (entityHit != null && entityHit.getEntity() instanceof LivingEntity target) {
            InteractionResult result = this.interactPlayer(stack, player, target, hand);
            if (result != InteractionResult.PASS) {
                return result;
            }
        }

        ServerPlayNetworking.send(serverPlayer, new OpenRosterViewScreenPayload());
        return InteractionResult.SUCCESS;
    }


    public @NotNull InteractionResult interactPlayer(@NotNull ItemStack stack, Player user, @NotNull LivingEntity target, @NotNull InteractionHand hand) {
        if (user.level().isClientSide()) return InteractionResult.PASS;

        if (user.equals(target) || !(target instanceof ServerPlayer targetPlayer)) return InteractionResult.FAIL;

        ServerPlayer serverUser = (ServerPlayer) user;

        RosterDataManager rosterDataManager = Persona.getRosterDataManager();
        CharacterManager characterManager = Persona.getCharacterManager();
        PlayerCharacterStorage targetData = characterManager.getPlayerCharacters(targetPlayer, targetPlayer.getUUID());

        PlayerCharacterStorage userData = characterManager.getPlayerCharacters(targetPlayer, serverUser.getUUID());

        if (targetData.getCurrentCharacter().isEmpty()) {
            serverUser.sendSystemMessage(Component.literal("The player has no characters yet")
                    .withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }

        if (userData.getCurrentCharacter().isEmpty()) {
            serverUser.sendSystemMessage(Component.literal("You have no characters yet")
                    .withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        PlayerRosterData rosterData = rosterDataManager.getPlayerRoster(userData.getCurrentCharacter().get());
        if(rosterData.hasEntry(targetPlayer.getUUID().toString(), targetData.getCurrentCharacterId())){
            serverUser.sendSystemMessage(
                    Component.translatable("persona.roster.already")
                            .withStyle(ChatFormatting.YELLOW)
            );
            return InteractionResult.FAIL;
        }

        CharacterData targetCharacter = targetData.getCurrentCharacter().get();

        ServerPlayNetworking.send(serverUser, new AddToRosterS2CPayload(
                new AddCharacterToRosterData(targetCharacter.getCorrespondingPlayer(), targetCharacter.getCustomName()))
        );

        return InteractionResult.SUCCESS;
    }
}