package se.mickelus.tetra.items.forged;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ObjectHolder;
import se.mickelus.tetra.TetraMod;
import se.mickelus.tetra.blocks.forged.ForgedBlockCommon;
import se.mickelus.tetra.items.TetraItem;
import se.mickelus.tetra.items.TetraItemGroup;

import javax.annotation.Nullable;
import java.util.List;

public class ItemMesh extends TetraItem {
    private static final String unlocalizedName = "forged_mesh";
    @ObjectHolder(TetraMod.MOD_ID + ":" + unlocalizedName)
    public static ItemMesh instance;

    public ItemMesh() {
        super(new Properties().group(TetraItemGroup.instance));
        setRegistryName(unlocalizedName);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void addInformation(ItemStack itemStack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
        tooltip.add(ForgedBlockCommon.hintTooltip);
    }
}
