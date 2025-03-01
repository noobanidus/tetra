package se.mickelus.tetra.blocks.forged;

import net.minecraft.block.*;
import net.minecraft.block.pattern.BlockStateMatcher;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.IFluidState;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.ObjectHolder;
import se.mickelus.tetra.TetraMod;
import se.mickelus.tetra.blocks.ITetraBlock;
import se.mickelus.tetra.blocks.salvage.BlockInteraction;
import se.mickelus.tetra.blocks.salvage.IBlockCapabilityInteractive;
import se.mickelus.tetra.capabilities.Capability;
import se.mickelus.tetra.items.ItemModular;
import se.mickelus.tetra.module.ItemEffectHandler;
import se.mickelus.tetra.util.CastOptional;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

import static net.minecraft.fluid.Fluids.WATER;
import static net.minecraft.state.properties.BlockStateProperties.WATERLOGGED;


public class BlockForgedCrate extends FallingBlock implements ITetraBlock, IBlockCapabilityInteractive, IWaterLoggable {
    static final String unlocalizedName = "forged_crate";
    @ObjectHolder(TetraMod.MOD_ID + ":" + unlocalizedName)
    public static BlockForgedCrate instance;

    public static final DirectionProperty propFacing = HorizontalBlock.HORIZONTAL_FACING;
    public static final BooleanProperty propStacked = BooleanProperty.create("stacked");
    public static final IntegerProperty propIntegrity = IntegerProperty.create("integrity", 0, 3);

    static final BlockInteraction[] interactions = new BlockInteraction[] {
            new BlockInteraction(Capability.pry, 1, Direction.EAST, 6, 8, 6, 8,
                    BlockStateMatcher.ANY,
                    BlockForgedCrate::attemptBreakPry),
            new BlockInteraction(Capability.hammer, 3, Direction.EAST, 1, 4, 1, 4,
                    BlockStateMatcher.ANY,
                    BlockForgedCrate::attemptBreakHammer),
            new BlockInteraction(Capability.hammer, 3, Direction.EAST, 10, 13, 10, 13,
                    BlockStateMatcher.ANY,
                    BlockForgedCrate::attemptBreakHammer),
    };

    public static final ResourceLocation pryBonusLootTable = new ResourceLocation(TetraMod.MOD_ID, "forged/forged_crate_pry_bonus");
    public static final ResourceLocation hammerbonusLootTable = new ResourceLocation(TetraMod.MOD_ID, "forged/forged_crate_hammer_bonus");

    private static final VoxelShape shape = makeCuboidShape(1, 0, 1, 15, 14, 15);
    private static final VoxelShape[] shapesNormal = new VoxelShape[4];
    private static final VoxelShape[] shapesOffset = new VoxelShape[4];
    static {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            shapesNormal[dir.getHorizontalIndex()] = shape.withOffset(dir.getXOffset() / 16f, dir.getYOffset() / 16f, dir.getZOffset() / 16f);
            shapesOffset[dir.getHorizontalIndex()] = shapesNormal[dir.getHorizontalIndex()].withOffset(0, -1 / 8f, 0);
        }
    }

    public BlockForgedCrate() {
        super(Properties.create(ForgedBlockCommon.material)
                .sound(SoundType.METAL)
                .hardnessAndResistance(7));

        setRegistryName(unlocalizedName);

        this.setDefaultState(getDefaultState()
                .with(propFacing, Direction.EAST)
                .with(propStacked, false)
                .with(propIntegrity, 3)
                .with(WATERLOGGED, false));
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable IBlockReader worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        tooltip.add(ForgedBlockCommon.hintTooltip);
    }

    private static boolean attemptBreakHammer(World world, BlockPos pos, BlockState blockState, PlayerEntity player, Hand hand, Direction facing) {
        return attemptBreak(world, pos, blockState, player, hand, player.getHeldItem(hand), Capability.hammer, 2, 1);
    }

    private static boolean attemptBreakPry(World world, BlockPos pos, BlockState blockState, PlayerEntity player, Hand hand, Direction facing) {
        return attemptBreak(world, pos, blockState, player, hand, player.getHeldItem(hand), Capability.pry, 0, 2);
    }

    private static boolean attemptBreak(World world, BlockPos pos, BlockState blockState, PlayerEntity player, Hand hand,
            ItemStack itemStack, Capability capability, int min, int multiplier) {

        int integrity = blockState.get(propIntegrity);

        int progress = CastOptional.cast(itemStack.getItem(), ItemModular.class)
                .map(item -> item.getCapabilityLevel(itemStack, capability))
                .map(level -> ( level - min ) * multiplier)
                .orElse(1);

        if (integrity - progress >= 0) {
            if (Capability.hammer.equals(capability)) {
                world.playSound(player, pos, SoundEvents.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, SoundCategory.PLAYERS, 1, 0.5f);
            } else {
                world.playSound(player, pos, SoundEvents.BLOCK_LADDER_STEP, SoundCategory.PLAYERS, 0.7f, 2f);
            }

            world.setBlockState(pos, blockState.with(propIntegrity, integrity - progress));
        } else {
            if (world instanceof ServerWorld) {
                ResourceLocation lootTable = Capability.hammer.equals(capability) ? hammerbonusLootTable : pryBonusLootTable;

                BlockInteraction.getLoot(lootTable, player, hand, (ServerWorld) world, blockState)
                        .forEach(lootStack -> spawnAsEntity(world, pos, lootStack));
            }

            world.playEvent(player, 2001, pos, Block.getStateId(blockState));
            ItemEffectHandler.breakBlock(world, player, itemStack, pos, blockState, false);
        }

        return true;
    }

    @Override
    public BlockInteraction[] getPotentialInteractions(BlockState state, Direction face, Collection<Capability> capabilities) {
            return interactions;
    }

    @Override
    public boolean onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
        return BlockInteraction.attemptInteraction(world, state, pos, player, hand, hit);
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        super.fillStateContainer(builder);
        builder.add(propFacing, propStacked, propIntegrity, WATERLOGGED);
    }

    @Override
    public IFluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? WATER.getStillFluidState(false) : super.getFluidState(state);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        return super.getStateForPlacement(context)
                .with(propFacing, context.getPlacementHorizontalFacing())
                .with(propStacked, equals(context.getWorld().getBlockState(context.getPos().down()).getBlock()))
                .with(WATERLOGGED, context.getWorld().getFluidState(context.getPos()).getFluid() == WATER);
    }

    @Override
    public BlockState updatePostPlacement(BlockState state, Direction facing, BlockState facingState, IWorld world, BlockPos currentPos,
            BlockPos facingPos) {
        if (state.get(WATERLOGGED)) {
            world.getPendingFluidTicks().scheduleTick(currentPos, WATER, WATER.getTickRate(world));
        }

        if (Direction.DOWN.equals(facing) && equals(facingState.getBlock())) {
            return super.updatePostPlacement(state, facing, facingState, world, currentPos, facingPos)
                    .with(propStacked, true);
        }

        return super.updatePostPlacement(state, facing, facingState, world, currentPos, facingPos);
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext context) {
        if (!state.get(propStacked)) {
            return shapesNormal[state.get(propFacing).getHorizontalIndex()];
        }
        return shapesOffset[state.get(propFacing).getHorizontalIndex()];
    }

    @Override
    public BlockState rotate(final BlockState state, final Rotation rotation) {
        return state.with(propFacing, rotation.rotate(state.get(propFacing)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.toRotation(state.get(propFacing)));
    }

    @Override
    public boolean hasItem() {
        return true;
    }

    @Override
    public void registerItem(IForgeRegistry<Item> registry) {
        registerItem(registry, this);
    }
}
