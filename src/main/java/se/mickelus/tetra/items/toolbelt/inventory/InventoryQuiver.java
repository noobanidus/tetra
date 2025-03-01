package se.mickelus.tetra.items.toolbelt.inventory;

import net.minecraft.item.ItemStack;
import se.mickelus.tetra.NBTHelper;
import se.mickelus.tetra.items.toolbelt.ItemToolbeltModular;
import se.mickelus.tetra.items.toolbelt.SlotType;

import java.util.ArrayList;

public class InventoryQuiver extends InventoryToolbelt {

    private static final String inventoryKey = "quiverInventory";
    public static int maxSize = 30; // 27;

    public InventoryQuiver(ItemStack stack) {
        super(inventoryKey, stack, maxSize, SlotType.quiver);
        ItemToolbeltModular item = (ItemToolbeltModular) stack.getItem();
        numSlots = item.getNumSlots(stack, SlotType.quiver);

        predicate = InventoryToolbelt.quiverPredicate;

        readFromNBT(NBTHelper.getTag(stack));
    }

    /**
     * Returns the number of unique items in this inventory.
     * @return
     */
    public ItemStack[] getAggregatedStacks() {
        ArrayList<ItemStack> aggregatedStacks = new ArrayList<>();
        for (ItemStack itemStack : inventoryContents) {
            boolean found = false;
            for (ItemStack aggregatedStack : aggregatedStacks) {
                if (ItemStack.areItemsEqual(itemStack, aggregatedStack) && ItemStack.areItemStackTagsEqual(itemStack, aggregatedStack)) {
                    found = true;
                    aggregatedStack.grow(itemStack.getCount());
                    break;
                }
            }
            if (!found && !itemStack.isEmpty()) {
                aggregatedStacks.add(itemStack.copy());
            }
        }

        return aggregatedStacks.toArray(new ItemStack[aggregatedStacks.size()]);
    }

    public int getFirstIndexForStack(ItemStack itemStack) {
        for (int i = 0; i < inventoryContents.size(); i++) {
            if (ItemStack.areItemsEqual(itemStack, inventoryContents.get(i)) && ItemStack.areItemStackTagsEqual(inventoryContents.get(i), itemStack)) {
                return i;
            }
        }
        return -1;
    }
}
