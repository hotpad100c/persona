package ml.mypals.persona.items;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.function.Function;

import static ml.mypals.persona.Persona.MOD_ID;

public class ModItems {
    public static final Item ROSTER = register("roster", RosterItem::new, (new Item.Properties()).stacksTo(1));
    private static Item register(String id, Function<Item.Properties, Item> function, Item.Properties properties) {
        return Items.registerItem(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID,id)),function,properties);
    }

    public static void initialize() {
    }
}
