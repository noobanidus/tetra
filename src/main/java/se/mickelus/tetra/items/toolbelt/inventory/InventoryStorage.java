package se.mickelus.tetra.items.toolbelt.inventory;

import net.minecraft.item.ItemStack;
import se.mickelus.tetra.NBTHelper;
import se.mickelus.tetra.items.toolbelt.ItemToolbeltModular;
import se.mickelus.tetra.items.toolbelt.SlotType;
import se.mickelus.tetra.module.ItemEffect;

import java.util.Collection;
import java.util.List;

public class InventoryStorage extends InventoryToolbelt {

    private static final String inventoryKey = "storageInventory";
    public static int maxSize = 30; // 24;

    public InventoryStorage(ItemStack stack) {
        super(inventoryKey, stack, maxSize, SlotType.storage);
        ItemToolbeltModular item = (ItemToolbeltModular) stack.getItem();
        numSlots = item.getNumSlots(stack, SlotType.storage);

        readFromNBT(NBTHelper.getTag(stack));
    }

    @Override
    public boolean storeItemInInventory(ItemStack itemStack) {
        List<Collection<ItemEffect>> effects = getSlotEffects();
        // attempt to merge the itemstack with itemstacks in the toolbelt
        for (int i = 0; i < getSizeInventory(); i++) {
            ItemStack storedStack = getStackInSlot(i);
            if (effects.get(i).contains(ItemEffect.quickAccess)
                    && storedStack.isItemEqual(itemStack)
                    && storedStack.getCount() < storedStack.getMaxStackSize()) {

                int moveCount = Math.min(itemStack.getCount(), storedStack.getMaxStackSize() - storedStack.getCount());
                storedStack.grow(moveCount);
                setInventorySlotContents(i, storedStack);
                itemStack.shrink(moveCount);

                if (itemStack.isEmpty()) {
                    return true;
                }
            }
        }

        // put item in the first empty slot
        for (int i = 0; i < getSizeInventory(); i++) {
            if (effects.get(i).contains(ItemEffect.quickAccess) && getStackInSlot(i).isEmpty()) {
                setInventorySlotContents(i, itemStack);
                return true;
            }
        }
        return false;
    }
}
