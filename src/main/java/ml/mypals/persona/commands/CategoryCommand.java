package ml.mypals.persona.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import ml.mypals.persona.management.MemberCategoryManager;
import ml.mypals.persona.management.MemberEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

import java.util.Map;

public class CategoryCommand {
    private final MemberCategoryManager manager;

    public CategoryCommand(MemberCategoryManager manager) {
        this.manager = manager;
    }

    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("category")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_ADMIN))

                .then(Commands.literal("create")
                        .then(Commands.argument("categoryId", StringArgumentType.word())
                                .then(Commands.argument("categoryName", StringArgumentType.string())
                                        .then(Commands.argument("maxCharacters", IntegerArgumentType.integer(0, 100))
                                                .then(Commands.argument("canUseRoster", BoolArgumentType.bool())
                                                        .then(Commands.argument("displayJoinLeave", BoolArgumentType.bool())

                                                                .executes(ctx -> createCategory(ctx, 1, 0))

                                                                .then(Commands.argument("rosterLevel", IntegerArgumentType.integer(0, 2))

                                                                        .executes(ctx -> createCategory(
                                                                                ctx,
                                                                                IntegerArgumentType.getInteger(ctx, "rosterLevel"),
                                                                                0))

                                                                        .then(Commands.argument("priority", IntegerArgumentType.integer())

                                                                                .executes(ctx -> createCategory(
                                                                                        ctx,
                                                                                        IntegerArgumentType.getInteger(ctx, "rosterLevel"),
                                                                                        IntegerArgumentType.getInteger(ctx, "priority")
                                                                                )))
                                                                )

                                                                .then(Commands.argument("priority", IntegerArgumentType.integer())

                                                                        .executes(ctx -> createCategory(
                                                                                ctx,
                                                                                1,
                                                                                IntegerArgumentType.getInteger(ctx, "priority")
                                                                        )))
                                                        )
                                                )
                                        )
                                )
                        )
                )

                .then(Commands.literal("delete")
                        .then(Commands.argument("categoryId", StringArgumentType.word())
                                .executes(this::deleteCategory)))

                .then(Commands.literal("list")
                        .executes(this::listCategories))

                .then(Commands.literal("set")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("categoryId", StringArgumentType.word())
                                        .executes(this::setPlayerCategory))))

                .then(Commands.literal("info")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(this::getPlayerInfo)))
        );
    }

    private int createCategory(CommandContext<CommandSourceStack> context, int rosterLevel, int priority) {
        String categoryId = StringArgumentType.getString(context, "categoryId");
        String categoryName = StringArgumentType.getString(context, "categoryName");
        int maxCharacters = IntegerArgumentType.getInteger(context, "maxCharacters");
        boolean canUseRoster = BoolArgumentType.getBool(context, "canUseRoster");
        boolean displayOnJoin = BoolArgumentType.getBool(context, "displayJoinLeave");
        boolean success = manager.createOrUpdateCategory(
                categoryId, categoryName, maxCharacters, displayOnJoin, canUseRoster, priority, rosterLevel
        );

        if (success) {
            context.getSource().sendSystemMessage(
                    Component.translatable("command.persona.category.create.success")
                            .withStyle(ChatFormatting.GREEN)
                            .append(Component.translatable("command.persona.common.colon"))
                            .append(Component.literal(categoryName).withStyle(ChatFormatting.YELLOW))
                            .append(Component.translatable("command.persona.category.id", categoryId))
                            .append(Component.translatable("command.persona.category.max", maxCharacters))
                            .append(Component.translatable("command.persona.category.roster",
                                    canUseRoster ?
                                            Component.translatable("command.persona.common.yes") :
                                            Component.translatable("command.persona.common.no")))
                            .append(Component.translatable("command.persona.category.roster_level", rosterLevel))
                            .append(Component.translatable("command.persona.category.priority", priority))
            );
        } else {
            context.getSource().sendSystemMessage(
                    Component.translatable("command.persona.category.create.failed")
                            .withStyle(ChatFormatting.RED)
            );
        }

        return success ? 1 : 0;
    }

    private int deleteCategory(CommandContext<CommandSourceStack> context) {
        String categoryId = StringArgumentType.getString(context, "categoryId");

        boolean success = manager.deleteCategory(categoryId);

        if (success) {
            context.getSource().sendSystemMessage(
                    Component.translatable("command.persona.category.delete.success")
                            .withStyle(ChatFormatting.GREEN)
                            .append(Component.translatable("command.persona.common.colon"))
                            .append(Component.literal(categoryId).withStyle(ChatFormatting.YELLOW))
            );
        } else {
            context.getSource().sendSystemMessage(
                    Component.translatable("command.persona.category.delete.failed")
                            .withStyle(ChatFormatting.RED)
            );
        }

        return success ? 1 : 0;
    }

    private int listCategories(CommandContext<CommandSourceStack> context) {
        Map<String, MemberEntry> categories = manager.getAllCategories();

        if (categories.isEmpty()) {
            context.getSource().sendSystemMessage(
                    Component.translatable("command.persona.category.list.empty")
                            .withStyle(ChatFormatting.YELLOW)
            );
            return 0;
        }

        context.getSource().sendSystemMessage(
                Component.translatable("command.persona.category.list.title")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
        );

        categories.values().stream()
                .sorted((a, b) -> Integer.compare(b.getPriority(), a.getPriority()))
                .forEach(entry -> {
                    Component line = Component.translatable("command.persona.category.list.entry")
                            .withStyle(ChatFormatting.GRAY)
                            .append(Component.literal(entry.getCategoryName()).withStyle(ChatFormatting.YELLOW))
                            .append(Component.translatable("command.persona.category.list.id", entry.getCategoryId()))
                            .append(Component.translatable("command.persona.category.list.max", entry.getMaxCharacters()))
                            .append(Component.translatable("command.persona.category.list.roster",
                                            entry.canUseRoster() ?
                                                    Component.translatable("command.persona.common.yes") :
                                                    Component.translatable("command.persona.common.no"))
                                    .withStyle(entry.canUseRoster() ? ChatFormatting.GREEN : ChatFormatting.RED))
                            .append(Component.translatable("command.persona.category.list.roster_level", entry.getRosterLevel()))
                            .append(Component.translatable("command.persona.category.list.priority", entry.getPriority()));

                    context.getSource().sendSystemMessage(line);
                });

        return 1;
    }

    private int setPlayerCategory(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
            String categoryId = StringArgumentType.getString(context, "categoryId");

            boolean success = manager.setPlayerCategory(targetPlayer,targetPlayer.getUUID(), categoryId);

            if (success) {
                MemberEntry category = manager.getPlayerCategory(targetPlayer.getUUID());

                context.getSource().sendSystemMessage(
                        Component.translatable("command.persona.category.player.set")
                                .withStyle(ChatFormatting.GREEN)
                                .append(Component.literal(targetPlayer.getName().getString()).withStyle(ChatFormatting.YELLOW))
                                .append(Component.translatable("command.persona.common.to"))
                                .append(Component.literal(category.getCategoryName()).withStyle(ChatFormatting.AQUA))
                );

                targetPlayer.sendSystemMessage(
                        Component.translatable("command.persona.category.player.notify")
                                .withStyle(ChatFormatting.GREEN)
                                .append(Component.literal(category.getCategoryName()).withStyle(ChatFormatting.YELLOW))
                );
            } else {
                context.getSource().sendFailure(
                        Component.translatable("command.persona.category.player.failed")
                                .withStyle(ChatFormatting.RED)
                );
            }

            return success ? 1 : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private int getPlayerInfo(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
            MemberEntry category = manager.getPlayerCategory(targetPlayer.getUUID());

            context.getSource().sendSystemMessage(
                    Component.translatable("command.persona.player.info.title")
                            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
            );

            context.getSource().sendSystemMessage(
                    Component.translatable("command.persona.player.info.name")
                            .append(Component.literal(targetPlayer.getName().getString()).withStyle(ChatFormatting.WHITE))
            );

            context.getSource().sendSystemMessage(
                    Component.translatable("command.persona.player.info.category")
                            .append(Component.literal(category.getCategoryName()).withStyle(ChatFormatting.GRAY))
                            .append(Component.translatable("command.persona.player.info.category_id", category.getCategoryId()))
            );

            context.getSource().sendSystemMessage(
                    Component.translatable("command.persona.player.info.max")
                            .append(Component.literal(String.valueOf(category.getMaxCharacters())).withStyle(ChatFormatting.GRAY))
            );

            context.getSource().sendSystemMessage(
                    Component.translatable("command.persona.player.info.roster")
                            .append(
                                    category.canUseRoster() ?
                                            Component.translatable("command.persona.common.yes").withStyle(ChatFormatting.GREEN) :
                                            Component.translatable("command.persona.common.no").withStyle(ChatFormatting.RED)
                            )
            );

            context.getSource().sendSystemMessage(
                    Component.translatable("command.persona.player.info.roster_level")
                            .append(Component.literal(String.valueOf(category.getRosterLevel())).withStyle(ChatFormatting.GRAY))
            );

            return 1;
        } catch (Exception e) {
            return 0;
        }
    }
}