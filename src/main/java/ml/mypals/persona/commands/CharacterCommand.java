package ml.mypals.persona.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import ml.mypals.persona.Persona;
import ml.mypals.persona.characterData.CharacterData;
import ml.mypals.persona.characterData.CharacterManager;
import ml.mypals.persona.characterData.PlayerCharacterStorage;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public class CharacterCommand {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
    private final CharacterManager manager;
    private final SuggestionProvider<CommandSourceStack> characterIdSuggestionProvider = (context, builder) -> {
        Entity entity = context.getSource().getEntity();
        if (!(entity instanceof ServerPlayer player)) {
            return builder.buildFuture();
        }

        PlayerCharacterStorage data = Persona.getCharacterManager().getPlayerCharacters(player, player.getUUID());
        List<CharacterData> characters = data.getCharacters();

        for (CharacterData character : characters) {
            if (!character.isDiscarded())
                builder.suggest(character.getCustomName(), Component.literal(character.getCharacterId()));
        }
        return builder.buildFuture();
    };

    public CharacterCommand(CharacterManager manager) {
        this.manager = manager;
    }

    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("character")
                .then(Commands.literal("create")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .then(Commands.argument("skinSource", StringArgumentType.string())
                                        .executes(this::createCharacter))))
                .then(Commands.literal("switch")
                        .then(Commands.argument("characterName", StringArgumentType.string())
                                .suggests(characterIdSuggestionProvider)
                                .executes(this::switchCharacter)))
                .then(Commands.literal("list")
                        .executes(this::listCharacters))
                .then(Commands.literal("delete")
                        .then(Commands.argument("characterId", StringArgumentType.string())
                                .suggests(characterIdSuggestionProvider)
                                .executes(this::deleteCharacter)))
        );
    }

    private int createCharacter(CommandContext<CommandSourceStack> context) {
        Entity entity = context.getSource().getEntity();
        if (!(entity instanceof ServerPlayer player)) {
            return 0;
        }
        String name = StringArgumentType.getString(context, "name");
        //Property skinTexture = manager.getPlayerSkinTexture(player);
        String skinTexture = StringArgumentType.getString(context, "skinSource");
        boolean success = manager.createCharacter(player, player.getUUID(), name, skinTexture);

        if (success) {
            context.getSource().sendSuccess(() ->
                            Component.translatable("command.persona.character.create.success")
                                    .withStyle(ChatFormatting.GREEN)
                                    .append(Component.translatable("command.persona.common.colon"))
                                    .append(Component.literal(name).withStyle(ChatFormatting.YELLOW)),
                    false
            );
        } else {
            context.getSource().sendFailure(
                    Component.translatable("command.persona.character.create.failed")
                            .withStyle(ChatFormatting.RED)
            );
        }

        return success ? 1 : 0;
    }

    private int switchCharacter(CommandContext<CommandSourceStack> context) {
        Entity entity = context.getSource().getEntity();
        if (!(entity instanceof ServerPlayer player)) {
            return 0;
        }

        String characterId = StringArgumentType.getString(context, "characterName");
        PlayerCharacterStorage data = manager.getPlayerCharacters(player, player.getUUID());

        Optional<CharacterData> oldCharacter = data.getCurrentCharacter();
        boolean success = manager.switchCharacter(player, player.getUUID(), characterId);
        Optional<CharacterData> newCharacter = data.getCurrentCharacter();
        if (success) {
            newCharacter.ifPresent(character -> {
                oldCharacter.ifPresent(Persona.getRosterDataManager()::unloadCharacterRoster);
                Persona.getRosterDataManager().loadCharacterRoster(player, character);

                context.getSource().sendSuccess(() ->
                                Component.translatable("command.persona.character.switch.success")
                                        .withStyle(ChatFormatting.GREEN)
                                        .append(Component.translatable("command.persona.common.colon"))
                                        .append(Component.literal(character.getCustomName()).withStyle(ChatFormatting.YELLOW)),
                        false
                );
            });
        } else {
            context.getSource().sendFailure(
                    Component.translatable("command.persona.character.switch.failed")
                            .withStyle(ChatFormatting.RED)
            );
        }

        return success ? 1 : 0;
    }

    private int listCharacters(CommandContext<CommandSourceStack> context) {
        Entity entity = context.getSource().getEntity();
        if (!(entity instanceof ServerPlayer player)) {
            return 0;
        }

        PlayerCharacterStorage data = manager.getPlayerCharacters(player, player.getUUID());
        List<CharacterData> characters = data.getCharacters();

        if (characters.isEmpty()) {
            context.getSource().sendFailure(
                    Component.translatable("command.persona.character.list.empty")
                            .withStyle(ChatFormatting.RED)
            );
            return 0;
        }

        context.getSource().sendSuccess(() ->
                        Component.translatable("command.persona.character.list.title")
                                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                false
        );

        String currentId = data.getCurrentCharacterId();
        for (int i = 0; i < characters.size(); i++) {
            CharacterData character = characters.get(i);
            if (character.isDiscarded()) continue;

            boolean isCurrent = character.getCharacterId().equals(currentId);

            Component line = Component.literal((i + 1) + ". ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(
                            Component.literal(character.getCustomName())
                                    .withStyle(isCurrent ? ChatFormatting.GREEN : ChatFormatting.WHITE)
                                    .withStyle(s -> s.withClickEvent(
                                            new ClickEvent.RunCommand("/character switch " + character.getCustomName())
                                    ))
                    )
                    .append(
                            isCurrent ?
                                    Component.translatable("command.persona.character.list.current").withStyle(ChatFormatting.AQUA) :
                                    Component.empty()
                    )
                    .append(
                            Component.translatable("command.persona.character.list.id", character.getCharacterId())
                                    .withStyle(ChatFormatting.GRAY)
                    )
                    .append(
                            Component.translatable("command.persona.character.list.date",
                                            DATE_FORMAT.format(new Date(character.getCreateTime())))
                                    .withStyle(ChatFormatting.GRAY)
                    );

            context.getSource().sendSuccess(() -> line, false);
        }

        return 1;
    }

    private int deleteCharacter(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            return 0;
        }

        String characterId = StringArgumentType.getString(context, "characterId");
        PlayerCharacterStorage data = manager.getPlayerCharacters(player, player.getUUID());

        String characterName = data.getCharacterByName(characterId)
                .map(CharacterData::getCustomName)
                .orElse("Unknown");

        boolean success = manager.deleteCharacter(player, player.getUUID(), characterId);

        if (success) {
            context.getSource().sendSuccess(() ->
                            Component.translatable("command.persona.character.delete.success")
                                    .withStyle(ChatFormatting.GREEN)
                                    .append(Component.translatable("command.persona.common.colon"))
                                    .append(Component.literal(characterName).withStyle(ChatFormatting.YELLOW)),
                    false
            );
        } else {
            context.getSource().sendFailure(
                    Component.translatable("command.persona.character.delete.failed")
                            .withStyle(ChatFormatting.RED)
            );
        }

        return success ? 1 : 0;
    }
}