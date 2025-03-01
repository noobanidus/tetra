package se.mickelus.tetra.items;

import com.google.common.collect.*;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RotatedPillarBlock;
import net.minecraft.block.material.Material;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.monster.EndermanEntity;
import net.minecraft.entity.monster.EndermiteEntity;
import net.minecraft.entity.monster.ShulkerEntity;
import net.minecraft.entity.monster.VexEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.particles.BlockParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.Tag;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.event.entity.player.UseHoeEvent;
import net.minecraftforge.eventbus.api.Event;
import se.mickelus.tetra.BleedingEffect;
import se.mickelus.tetra.EarthboundEffect;
import se.mickelus.tetra.NBTHelper;
import se.mickelus.tetra.ToolTypes;
import se.mickelus.tetra.capabilities.Capability;
import se.mickelus.tetra.module.ItemEffect;
import se.mickelus.tetra.module.ItemEffectHandler;
import se.mickelus.tetra.util.CastOptional;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ItemModularHandheld extends ItemModular {

    /**
     * Below are lists of blocks, materials and tags that describe what different tools can harvest and efficiently destroy. Note that these
     * are copies of what the vanilla tool counterparts explicitly state that they can destroy and harvest, some blocks (and required tiers)
     * are not listed here as that's part of that block's implementation.
     */

    // copy of AxeItem.EFFECTIVE_ON, blocks that the axe explicitly state it can efficiently DESTROY
    private static final Set<Block> axeDestroyBlocks = Sets.newHashSet(Blocks.OAK_PLANKS, Blocks.SPRUCE_PLANKS, Blocks.BIRCH_PLANKS, Blocks.JUNGLE_PLANKS, Blocks.ACACIA_PLANKS, Blocks.DARK_OAK_PLANKS, Blocks.BOOKSHELF, Blocks.OAK_WOOD, Blocks.SPRUCE_WOOD, Blocks.BIRCH_WOOD, Blocks.JUNGLE_WOOD, Blocks.ACACIA_WOOD, Blocks.DARK_OAK_WOOD, Blocks.OAK_LOG, Blocks.SPRUCE_LOG, Blocks.BIRCH_LOG, Blocks.JUNGLE_LOG, Blocks.ACACIA_LOG, Blocks.DARK_OAK_LOG, Blocks.CHEST, Blocks.PUMPKIN, Blocks.CARVED_PUMPKIN, Blocks.JACK_O_LANTERN, Blocks.MELON, Blocks.LADDER, Blocks.SCAFFOLDING, Blocks.OAK_BUTTON, Blocks.SPRUCE_BUTTON, Blocks.BIRCH_BUTTON, Blocks.JUNGLE_BUTTON, Blocks.DARK_OAK_BUTTON, Blocks.ACACIA_BUTTON, Blocks.OAK_PRESSURE_PLATE, Blocks.SPRUCE_PRESSURE_PLATE, Blocks.BIRCH_PRESSURE_PLATE, Blocks.JUNGLE_PRESSURE_PLATE, Blocks.DARK_OAK_PRESSURE_PLATE, Blocks.ACACIA_PRESSURE_PLATE);

    // copy of hardcoded values in AxeItem, materials that the axe explicitly state it can efficiently HARVEST
    private static final Set<Material> axeHarvestMaterials = Sets.newHashSet(Material.WOOD, Material.PLANTS, Material.TALL_PLANTS, Material.BAMBOO);

    // copy of PickaxeItem.EFFECTIVE_ON, blocks that the pickaxe explicitly state it can efficiently DESTROY
    private static final Set<Block> pickaxeDestroyBlocks = Sets.newHashSet(Blocks.ACTIVATOR_RAIL, Blocks.COAL_ORE, Blocks.COBBLESTONE, Blocks.DETECTOR_RAIL, Blocks.DIAMOND_BLOCK, Blocks.DIAMOND_ORE, Blocks.POWERED_RAIL, Blocks.GOLD_BLOCK, Blocks.GOLD_ORE, Blocks.ICE, Blocks.IRON_BLOCK, Blocks.IRON_ORE, Blocks.LAPIS_BLOCK, Blocks.LAPIS_ORE, Blocks.MOSSY_COBBLESTONE, Blocks.NETHERRACK, Blocks.PACKED_ICE, Blocks.BLUE_ICE, Blocks.RAIL, Blocks.REDSTONE_ORE, Blocks.SANDSTONE, Blocks.CHISELED_SANDSTONE, Blocks.CUT_SANDSTONE, Blocks.CHISELED_RED_SANDSTONE, Blocks.CUT_RED_SANDSTONE, Blocks.RED_SANDSTONE, Blocks.STONE, Blocks.GRANITE, Blocks.POLISHED_GRANITE, Blocks.DIORITE, Blocks.POLISHED_DIORITE, Blocks.ANDESITE, Blocks.POLISHED_ANDESITE, Blocks.STONE_SLAB, Blocks.SMOOTH_STONE_SLAB, Blocks.SANDSTONE_SLAB, Blocks.PETRIFIED_OAK_SLAB, Blocks.COBBLESTONE_SLAB, Blocks.BRICK_SLAB, Blocks.STONE_BRICK_SLAB, Blocks.NETHER_BRICK_SLAB, Blocks.QUARTZ_SLAB, Blocks.RED_SANDSTONE_SLAB, Blocks.PURPUR_SLAB, Blocks.SMOOTH_QUARTZ, Blocks.SMOOTH_RED_SANDSTONE, Blocks.SMOOTH_SANDSTONE, Blocks.SMOOTH_STONE, Blocks.STONE_BUTTON, Blocks.STONE_PRESSURE_PLATE, Blocks.POLISHED_GRANITE_SLAB, Blocks.SMOOTH_RED_SANDSTONE_SLAB, Blocks.MOSSY_STONE_BRICK_SLAB, Blocks.POLISHED_DIORITE_SLAB, Blocks.MOSSY_COBBLESTONE_SLAB, Blocks.END_STONE_BRICK_SLAB, Blocks.SMOOTH_SANDSTONE_SLAB, Blocks.SMOOTH_QUARTZ_SLAB, Blocks.GRANITE_SLAB, Blocks.ANDESITE_SLAB, Blocks.RED_NETHER_BRICK_SLAB, Blocks.POLISHED_ANDESITE_SLAB, Blocks.DIORITE_SLAB, Blocks.SHULKER_BOX, Blocks.BLACK_SHULKER_BOX, Blocks.BLUE_SHULKER_BOX, Blocks.BROWN_SHULKER_BOX, Blocks.CYAN_SHULKER_BOX, Blocks.GRAY_SHULKER_BOX, Blocks.GREEN_SHULKER_BOX, Blocks.LIGHT_BLUE_SHULKER_BOX, Blocks.LIGHT_GRAY_SHULKER_BOX, Blocks.LIME_SHULKER_BOX, Blocks.MAGENTA_SHULKER_BOX, Blocks.ORANGE_SHULKER_BOX, Blocks.PINK_SHULKER_BOX, Blocks.PURPLE_SHULKER_BOX, Blocks.RED_SHULKER_BOX, Blocks.WHITE_SHULKER_BOX, Blocks.YELLOW_SHULKER_BOX);

    // copy of hardcoded values in PickaxeItem, materials that the pickaxe explicitly state it can efficiently HARVEST
    private static final Set<Material> pickaxeHarvestMaterials = Sets.newHashSet(Material.IRON, Material.ANVIL, Material.ROCK);

    // copy of ShovelItem.EFFECTIVE_ON, blocks that the shovel explicitly state it can efficiently DESTROY
    private static final Set<Block> shovelDestroyBlocks = Sets.newHashSet(Blocks.CLAY, Blocks.DIRT, Blocks.COARSE_DIRT, Blocks.PODZOL, Blocks.FARMLAND, Blocks.GRASS_BLOCK, Blocks.GRAVEL, Blocks.MYCELIUM, Blocks.SAND, Blocks.RED_SAND, Blocks.SNOW_BLOCK, Blocks.SNOW, Blocks.SOUL_SAND, Blocks.GRASS_PATH, Blocks.WHITE_CONCRETE_POWDER, Blocks.ORANGE_CONCRETE_POWDER, Blocks.MAGENTA_CONCRETE_POWDER, Blocks.LIGHT_BLUE_CONCRETE_POWDER, Blocks.YELLOW_CONCRETE_POWDER, Blocks.LIME_CONCRETE_POWDER, Blocks.PINK_CONCRETE_POWDER, Blocks.GRAY_CONCRETE_POWDER, Blocks.LIGHT_GRAY_CONCRETE_POWDER, Blocks.CYAN_CONCRETE_POWDER, Blocks.PURPLE_CONCRETE_POWDER, Blocks.BLUE_CONCRETE_POWDER, Blocks.BROWN_CONCRETE_POWDER, Blocks.GREEN_CONCRETE_POWDER, Blocks.RED_CONCRETE_POWDER, Blocks.BLACK_CONCRETE_POWDER);

    // copy of hardcoded values in ShovelItem, blocks that the shovel explicitly state it can efficiently HARVEST
    private static final Set<Block> shovelHarvestBlocks = Sets.newHashSet(Blocks.SNOW, Blocks.SNOW_BLOCK);

    // copy of hardcoded values in SwordItem, materials & tag that it explicitly state it can efficiently DESTROY
    private static final Set<Material> cuttingDestroyMaterials = Sets.newHashSet(Material.PLANTS, Material.TALL_PLANTS, Material.CORAL, Material.GOURD, Material.WEB);
    private static final Set<Tag<Block>> cuttingDestroyTags = Sets.newHashSet(BlockTags.LEAVES);

    // copy of hardcoded values in SwordItem, blocks that the sword explicitly state it can efficiently HARVEST
    private static final Set<Block> cuttingHarvestBlocks = Sets.newHashSet(Blocks.COBWEB);

    private static final ResourceLocation nailedTag = new ResourceLocation("tetra:nailed");

    protected static final Map<Block, BlockState> tillLookup = Maps.newHashMap(ImmutableMap.of(
            Blocks.GRASS_BLOCK, Blocks.FARMLAND.getDefaultState(),
            Blocks.GRASS_PATH, Blocks.FARMLAND.getDefaultState(),
            Blocks.DIRT, Blocks.FARMLAND.getDefaultState(),
            Blocks.COARSE_DIRT, Blocks.DIRT.getDefaultState()));

    protected static final Map<Block, BlockState> flattenLookup = Maps.newHashMap(ImmutableMap.of(
            Blocks.GRASS_BLOCK, Blocks.GRASS_PATH.getDefaultState()));

    protected static final Map<Block, Block> stripLookup = (new ImmutableMap.Builder<Block, Block>())
            .put(Blocks.OAK_WOOD, Blocks.STRIPPED_OAK_WOOD)
            .put(Blocks.OAK_LOG, Blocks.STRIPPED_OAK_LOG)
            .put(Blocks.DARK_OAK_WOOD, Blocks.STRIPPED_DARK_OAK_WOOD)
            .put(Blocks.DARK_OAK_LOG, Blocks.STRIPPED_DARK_OAK_LOG)
            .put(Blocks.ACACIA_WOOD, Blocks.STRIPPED_ACACIA_WOOD)
            .put(Blocks.ACACIA_LOG, Blocks.STRIPPED_ACACIA_LOG)
            .put(Blocks.BIRCH_WOOD, Blocks.STRIPPED_BIRCH_WOOD)
            .put(Blocks.BIRCH_LOG, Blocks.STRIPPED_BIRCH_LOG)
            .put(Blocks.JUNGLE_WOOD, Blocks.STRIPPED_JUNGLE_WOOD)
            .put(Blocks.JUNGLE_LOG, Blocks.STRIPPED_JUNGLE_LOG)
            .put(Blocks.SPRUCE_WOOD, Blocks.STRIPPED_SPRUCE_WOOD)
            .put(Blocks.SPRUCE_LOG, Blocks.STRIPPED_SPRUCE_LOG)
            .build();



    protected static final UUID ARMOR_MODIFIER = UUID.fromString("D96050BE-6A94-4A27-AA0B-2AF705327BA4");

    // the base amount of damage the item should take after destroying a block
    protected int blockDestroyDamage = 1;

    // the base amount of damage the item should take after hitting an entity
    protected int entityHitDamage = 1;

    public ItemModularHandheld(Properties properties) {
        super(properties);
    }

    public int getBlockDestroyDamage() {
        return blockDestroyDamage;
    }

    public int getEntityHitDamage() {
        return entityHitDamage;
    }

    @Override
    public boolean onBlockDestroyed(ItemStack itemStack, World world, BlockState state, BlockPos pos,
            LivingEntity entity) {
        if (state.getBlockHardness(world, pos) > 0) {
            applyDamage(blockDestroyDamage, itemStack, entity);

            if (!isBroken(itemStack)) {
                tickProgression(entity, itemStack, 1);
            }
        }

        if (!world.isRemote) {
            int intuitLevel = getEffectLevel(itemStack, ItemEffect.intuit);
            if (intuitLevel > 0) {
                int xp = state.getExpDrop(world, pos, EnchantmentHelper.getEnchantmentLevel(Enchantments.FORTUNE, itemStack),
                        EnchantmentHelper.getEnchantmentLevel(Enchantments.SILK_TOUCH, itemStack));
                if (xp > 0) {
                    tickHoningProgression(entity, itemStack, xp);
                }
            }
        }

        causeFierySelfEffect(entity, itemStack, 1);
        causeEnderReverbEffect(entity, itemStack, 1);
        causeHauntEffect(entity, itemStack, 1);

        return true;
    }

    @Override
    public boolean hitEntity(ItemStack itemStack, LivingEntity target, LivingEntity attacker) {
        applyDamage(entityHitDamage, itemStack, attacker);

        if (!isBroken(itemStack)) {
            getAllModules(itemStack).forEach(module -> module.hitEntity(itemStack, target, attacker));

            int sweepingLevel = getEffectLevel(itemStack, ItemEffect.sweeping);
            if (sweepingLevel > 0) {
                sweepAttack(itemStack, target, attacker, sweepingLevel);
            }

            int bleedingLevel = getEffectLevel(itemStack, ItemEffect.bleeding);
            if (bleedingLevel > 0) {
                if (!CreatureAttribute.UNDEAD.equals(target.getCreatureAttribute())
                        && attacker.getRNG().nextFloat() < 0.3f) {
                    target.addPotionEffect(new EffectInstance(BleedingEffect.instance, 40, bleedingLevel));
                }
            }

            // todo: only trigger if target is standing on stone/earth/sand/gravel
            int earthbindLevel = getEffectLevel(itemStack, ItemEffect.earthbind);
            if (earthbindLevel > 0 && attacker.getRNG().nextFloat() < Math.max(0.1, 0.5 * ( 1 - target.posY  / 128 ))) {
                target.addPotionEffect(new EffectInstance(EarthboundEffect.instance, 80, 0, false, true));

                if (target.world instanceof ServerWorld) {
                    BlockState blockState = target.world.getBlockState(new BlockPos(target.posX, target.posY - 1, target.posZ));
                    ((ServerWorld)target.world).spawnParticle(new BlockParticleData(ParticleTypes.BLOCK, blockState),
                            target.posX, target.posY + 0.1, target.posZ,
                            16, 0, target.world.rand.nextGaussian() * 0.2, 0, 0.1);
                }
            }

            causeFierySelfEffect(attacker, itemStack, 1.4);
            causeEnderReverbEffect(attacker, itemStack, 1.5);
            causeHauntEffect(attacker, itemStack, 1.5);

            tickProgression(attacker, itemStack, 1);
        }

        return true;
    }

    @Override
    public ActionResultType onItemUse(ItemUseContext context) {
        PlayerEntity player = context.getPlayer();
        Hand hand = context.getHand();
        World world = context.getWorld();
        BlockPos pos = context.getPos();
        Direction facing = context.getFace();

        ItemStack itemStack = player.getHeldItem(hand);
        int flatteningLevel = getEffectLevel(itemStack, ItemEffect.flattening);
        int strippingLevel = getEffectLevel(itemStack, ItemEffect.stripping);

        causeFierySelfEffect(player, itemStack, 2);
        causeEnderReverbEffect(player, itemStack, 1.7);

        if (flatteningLevel > 0 && (strippingLevel > 0 && player.isSneaking() || strippingLevel == 0)) {
            return flattenPath(player, world, pos, hand, facing);
        } else if (strippingLevel > 0) {
            return stripBlock(context);
        }

        int tillingLevel = getEffectLevel(itemStack, ItemEffect.tilling);
        if (tillingLevel > 0) {
            return tillBlock(context);
        }


        int denailingLevel = getEffectLevel(itemStack, ItemEffect.denailing);
        if (denailingLevel > 0 && player.getCooledAttackStrength(0) > 0.9) {
            ActionResultType result = denailBlock(player, world, pos, hand, facing);

            if (result.equals(ActionResultType.SUCCESS)) {
                player.resetCooldown();
            }

            return result;
        }

        return super.onItemUse(context);
    }

    protected void causeFierySelfEffect(LivingEntity entity, ItemStack itemStack, double multiplier) {
        if (!entity.world.isRemote) {
            double fierySelfEfficiency = getEffectEfficiency(itemStack, ItemEffect.fierySelf);
            if (fierySelfEfficiency > 0) {
                BlockPos pos = entity.getPosition();
                float temperature = entity.world.getBiome(pos).getTemperature(pos);
                if (entity.getRNG().nextDouble() < fierySelfEfficiency * temperature * multiplier) {
                    entity.setFire(getEffectLevel(itemStack, ItemEffect.fierySelf));
                }
            }
        }
    }

    protected void causeEnderReverbEffect(LivingEntity entity, ItemStack itemStack, double multiplier) {
        if (!entity.world.isRemote) {
            double effectProbability = getEffectEfficiency(itemStack, ItemEffect.enderReverb);
            if (effectProbability > 0) {
                if (entity.getRNG().nextDouble() < effectProbability * multiplier) {
                    AxisAlignedBB aabb = new AxisAlignedBB(entity.getPosition()).grow(24);
                    List<LivingEntity> nearbyTargets = entity.world.getEntitiesWithinAABB(LivingEntity.class, aabb,
                            target -> target instanceof EndermanEntity || target instanceof EndermiteEntity
                                    || target instanceof ShulkerEntity || target instanceof EnderDragonEntity);
                    if (nearbyTargets.size() > 0) {
                        nearbyTargets.get(entity.getRNG().nextInt(nearbyTargets.size())).setRevengeTarget(entity);
                    }
                }
            }
        }
    }

    protected void causeHauntEffect(LivingEntity entity, ItemStack itemStack, double multiplier) {
        if (!entity.world.isRemote) {
            double effectProbability = getEffectEfficiency(itemStack, ItemEffect.haunted);
            if (effectProbability > 0) {
                if (entity.getRNG().nextDouble() < effectProbability * multiplier) {
                    int effectLevel = getEffectLevel(itemStack, ItemEffect.haunted);

                    VexEntity vex = EntityType.VEX.create(entity.world);
                    vex.setLimitedLife(effectLevel * 20);
                    vex.setLocationAndAngles(entity.posX, entity.posY + 1, entity.posZ, entity.rotationYaw, 0.0F);
                    vex.setHeldItem(Hand.MAIN_HAND, itemStack.copy());
                    vex.setDropChance(EquipmentSlotType.MAINHAND, 0);
                    vex.addPotionEffect(new EffectInstance(Effects.INVISIBILITY, 2000 + effectLevel * 20));
                    entity.world.addEntity(vex);

                    // todo: use temporary modules for this instead once implemented
                    CastOptional.cast(itemStack.getItem(), ItemModular.class)
                            .map(item -> Arrays.stream(item.getMajorModules(itemStack)))
                            .orElse(Stream.empty())
                            .filter(Objects::nonNull)
                            .filter(module -> module.getImprovement(itemStack, ItemEffect.hauntedKey) != null)
                            .findAny()
                            .ifPresent(module -> {
                                int level = module.getImprovementLevel(itemStack, ItemEffect.hauntedKey);
                                if (level > 0) {
                                    module.addImprovement(itemStack, ItemEffect.hauntedKey, level - 1);
                                } else {
                                    module.removeImprovement(itemStack, ItemEffect.hauntedKey);
                                }
                            });

                    entity.world.playSound(null, entity.getPosition(), SoundEvents.ENTITY_WITCH_AMBIENT, SoundCategory.PLAYERS, 2f, 2);
                }
            }
        }
    }

    /**
     * Applies usage effects and ticks progression based on the given multiplier, should typically be called when the item is used
     * for something.
     *
     * @param entity The using entity
     * @param itemStack The used itemstack
     * @param multiplier A multiplier representing the effort and effect yielded from the use
     */
    public void applyUsageEffects(LivingEntity entity, ItemStack itemStack, double multiplier) {
        tickProgression(entity, itemStack, (int) multiplier);
        causeFierySelfEffect(entity, itemStack, multiplier);
        causeEnderReverbEffect(entity, itemStack, multiplier);
    }

    /**
     * Flattens grass into a path similar to how vanilla shovels does it.
     * @param player the responsible player entity
     * @param world the world in which the action takes place
     * @param pos the position in the world
     * @param hand the hand holding the tool
     * @param facing the clicked face
     * @return ActionResultType.SUCCESS if successful, ActionResultType.FAIL if block cannot be edited by player,
     * otherwise ActionResultType.PASS
     */
    public ActionResultType flattenPath(PlayerEntity player, World world, BlockPos pos, Hand hand, Direction facing) {
        ItemStack itemStack = player.getHeldItem(hand);

        if (facing != Direction.DOWN && world.getBlockState(pos.up()).isAir(world, pos.up())) {
            BlockState blockstate = flattenLookup.get(world.getBlockState(pos).getBlock());
            if (blockstate != null) {
                world.playSound(player, pos, SoundEvents.ITEM_SHOVEL_FLATTEN, SoundCategory.BLOCKS, 1, 1);
                if (!world.isRemote) {
                    world.setBlockState(pos, blockstate, 11);
                    applyDamage(blockDestroyDamage, itemStack, player);
                    tickProgression(player, itemStack, blockDestroyDamage);
                }

                return ActionResultType.SUCCESS;
            }
        }

        return ActionResultType.PASS;
    }

    /**
     * Tills dirt or grass, turning it into farmland. Tilling coarse dirt turns it into dirt.
     * @param context the context of the item's usage
     * otherwise ActionResultType.PASS
     */
    public ActionResultType tillBlock(ItemUseContext context) {
        PlayerEntity player = context.getPlayer();
        Hand hand = context.getHand();
        World world = context.getWorld();
        BlockPos pos = context.getPos();
        Direction facing = context.getFace();

        ItemStack itemStack = player.getHeldItem(hand);

        if (!player.canPlayerEdit(pos.offset(facing), facing, itemStack)) {
            return ActionResultType.FAIL;
        }

        // fire the forge event manually as the helper damages durability
        UseHoeEvent event = new UseHoeEvent(context);
        MinecraftForge.EVENT_BUS.post(event);

        if (event.isCanceled()) {
            return ActionResultType.FAIL;
        }
        if (event.getResult() == Event.Result.ALLOW) {
            applyDamage(blockDestroyDamage, itemStack, player);
            tickProgression(player, itemStack, blockDestroyDamage);
            return ActionResultType.SUCCESS;
        }

        if (facing != Direction.DOWN && world.isAirBlock(pos.up())) {
            BlockState newState = tillLookup.get(world.getBlockState(pos).getBlock());

            if (newState != null) {
                world.playSound(player, pos, SoundEvents.ITEM_HOE_TILL, SoundCategory.BLOCKS, 1, 1);

                if (!world.isRemote) {
                    world.setBlockState(pos, newState, 11);
                    applyDamage(blockDestroyDamage, itemStack, player);
                }

                return ActionResultType.SUCCESS;
            }
        }

        return ActionResultType.PASS;
    }

    public ActionResultType stripBlock(ItemUseContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getPos();
        BlockState blockState = world.getBlockState(pos);
        Block block = stripLookup.get(blockState.getBlock());
        if (block != null) {
            PlayerEntity player = context.getPlayer();
            world.playSound(player, pos, SoundEvents.ITEM_AXE_STRIP, SoundCategory.BLOCKS, 1, 1);

            if (!world.isRemote) {
                world.setBlockState(pos, block.getDefaultState().with(RotatedPillarBlock.AXIS, blockState.get(RotatedPillarBlock.AXIS)), 11);
                applyDamage(blockDestroyDamage, context.getItem(), player);
            }

            return ActionResultType.SUCCESS;
        } else {
            return ActionResultType.PASS;
        }
    }

    /**
     * Instantly break plank based blocks.
     * @param player the responsible player entity
     * @param world the world in which the action takes place
     * @param pos the position in the world
     * @param hand the hand holding the tool
     * @param facing the clicked face
     * @return ActionResultType.SUCCESS if successful, ActionResultType.FAIL if block cannot be edited by player,
     * otherwise ActionResultType.PASS
     */
    public ActionResultType denailBlock(PlayerEntity player, World world, BlockPos pos, Hand hand, Direction facing) {
        ItemStack itemStack = player.getHeldItem(hand);

        if (!player.canPlayerEdit(pos.offset(facing), facing, itemStack)) {
            return ActionResultType.FAIL;
        }

        BlockState blockState = world.getBlockState(pos);
        if (canDenail(blockState, world, pos)) {
            boolean success = ItemEffectHandler.breakBlock(world, player, player.getHeldItem(hand), pos, blockState, true);
            if (success) {
                applyDamage(blockDestroyDamage, itemStack, player);
                tickProgression(player, itemStack, blockDestroyDamage);

                player.resetCooldown();
                return ActionResultType.SUCCESS;
            }
        }

        return ActionResultType.PASS;
    }

    private boolean canDenail(BlockState blockState, World world, BlockPos pos) {
        return blockState.getBlock().getTags().contains(nailedTag);
    }

    /**
     * Perfoms a sweeping attack, dealing damage and playing effects similar to vanilla swords.
     * @param itemStack the itemstack used for the attack
     * @param target the attacking entity
     * @param attacker the attacked entity
     * @param sweepingLevel the level of the sweeping effect of the itemstack
     */
    private void sweepAttack(ItemStack itemStack, LivingEntity target, LivingEntity attacker, int sweepingLevel) {
        float cooldown = 1;
        if (attacker instanceof PlayerEntity) {
            cooldown = ItemModularHandheld.getCooledAttackStrength(itemStack);
        }

        if (cooldown > 0.9) {
            float damage = (float) Math.max((getDamageModifier(itemStack) + 1) * (sweepingLevel * 0.125f), 1);
            float knockback = sweepingLevel > 4 ? (getEnchantmentLevelFromImprovements(itemStack, Enchantments.KNOCKBACK) + 1) * 0.5f : 0.5f;
            double range = 1 + getEffectEfficiency(itemStack, ItemEffect.sweeping);

            // range values set up to mimic vanilla behaviour
            attacker.world.getEntitiesWithinAABB(LivingEntity.class,
                    target.getBoundingBox().grow(range, 0.25d, range)).stream()
                    .filter(entity -> entity != attacker)
                    .filter(entity -> !attacker.isOnSameTeam(entity))
                    .filter(entity -> attacker.getDistanceSq(entity) < (range + 2) * (range + 2))
                    .forEach(entity -> {
                        entity.knockBack(attacker, knockback,
                                MathHelper.sin(attacker.rotationYaw * 0.017453292f),
                                -MathHelper.cos(attacker.rotationYaw * 0.017453292f));
                        if (attacker instanceof PlayerEntity) {
                            entity.attackEntityFrom(DamageSource.causePlayerDamage((PlayerEntity) attacker), damage);
                        } else {
                            entity.attackEntityFrom(DamageSource.causeIndirectDamage(attacker, entity), damage);
                        }
                    });

            attacker.world.playSound(null, attacker.posX, attacker.posY, attacker.posZ,
                    SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, attacker.getSoundCategory(), 1.0F, 1.0F);

            CastOptional.cast(attacker, PlayerEntity.class).ifPresent(PlayerEntity::spawnSweepParticles);
        }
    }

    /**
     * Spawns sweeping particles in the given world at the given coordinates. Similar to the sweeping particle used
     * by vanilla swords.
     * @param world The world in which to spawn the particle
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     * @param xOffset x offset which is later multiplied by a random number (0-1)
     * @param zOffset z offset which is later multiplied by a random number (0-1)
     */
    public static void spawnSweepParticles(World world, double x, double y, double z, double xOffset, double zOffset) {
        if (world instanceof ServerWorld) {
            ((ServerWorld)world).spawnParticle(ParticleTypes.SWEEP_ATTACK, x, y, z,
                    1, xOffset, 0, zOffset, 0);
        }
    }

    public static void spawnSweepParticles(LivingEntity attacker) {
        double xOffset = -MathHelper.sin(attacker.rotationYaw * 0.017453292F);
        double zOffset = MathHelper.cos(attacker.rotationYaw * 0.017453292F);

        spawnSweepParticles(attacker.world, attacker.posX + xOffset, attacker.posY + attacker.getHeight() * 0.5D,
                attacker.posZ + zOffset, xOffset, zOffset);
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, PlayerEntity player, Entity entity) {
        setCooledAttackStrength(stack, player.getCooledAttackStrength(0.5f));
        return false;
    }

    public void setCooledAttackStrength(ItemStack itemStack, float strength) {
        NBTHelper.getTag(itemStack).putFloat(cooledStrengthKey, strength);
    }

    public static float getCooledAttackStrength(ItemStack itemStack) {
        return NBTHelper.getTag(itemStack).getFloat(cooledStrengthKey);
    }

    @Override
    public Multimap<String, AttributeModifier> getAttributeModifiers(EquipmentSlotType slot, ItemStack itemStack) {
        Multimap<String, AttributeModifier> multimap = super.getAttributeModifiers(slot, itemStack);

        if (slot == EquipmentSlotType.MAINHAND) {
            multimap.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(), new AttributeModifier(ATTACK_DAMAGE_MODIFIER,
                    "Weapon modifier", getDamageModifier(itemStack), AttributeModifier.Operation.ADDITION));

            multimap.put(SharedMonsterAttributes.ATTACK_SPEED.getName(), new AttributeModifier(ATTACK_SPEED_MODIFIER,
                    "Weapon modifier", getSpeedModifier(itemStack), AttributeModifier.Operation.ADDITION));
        }

        if (slot == EquipmentSlotType.MAINHAND || slot == EquipmentSlotType.OFFHAND) {
            int armor = getEffectLevel(itemStack, ItemEffect.armor);
            if  (armor > 0) {
                multimap.put(SharedMonsterAttributes.ARMOR.getName(), new AttributeModifier(ARMOR_MODIFIER,
                        "Weapon modifier", armor, AttributeModifier.Operation.ADDITION));
            }
        }

        return multimap;
    }

    public double getDamageModifier(ItemStack itemStack) {
        if (isBroken(itemStack)) {
            return 0;
        }

        double damageModifier = getAllModules(itemStack).stream()
                .mapToDouble(itemModule -> itemModule.getDamageModifier(itemStack))
                .sum();

        damageModifier = Arrays.stream(getSynergyData(itemStack))
                .mapToDouble(synergyData -> synergyData.damage)
                .reduce(damageModifier, Double::sum);

        damageModifier = Arrays.stream(getSynergyData(itemStack))
                .mapToDouble(synergyData -> synergyData.damageMultiplier)
                .reduce(damageModifier, (a, b) -> a * b);

        return getAllModules(itemStack).stream()
                .map(itemModule -> itemModule.getDamageMultiplierModifier(itemStack))
                .reduce(damageModifier, (a, b) -> a * b);
    }

    public static double getDamageModifierStatic(ItemStack itemStack) {
        if (itemStack.getItem() instanceof ItemModularHandheld) {
            return ((ItemModularHandheld) itemStack.getItem()).getDamageModifier(itemStack);
        }
        return 0;
    }

    public double getSpeedModifier(ItemStack itemStack) {
        double speedModifier = getAllModules(itemStack).stream()
                .map(itemModule -> itemModule.getSpeedModifier(itemStack))
                .reduce(-2.4d, Double::sum);

        speedModifier = Arrays.stream(getSynergyData(itemStack))
                .mapToDouble(synergyData -> synergyData.attackSpeed)
                .reduce(speedModifier, Double::sum);

        speedModifier = Arrays.stream(getSynergyData(itemStack))
                .mapToDouble(synergyData -> synergyData.attackSpeedMultiplier)
                .reduce(speedModifier, (a, b) -> a * b);

        speedModifier = getAllModules(itemStack).stream()
                .map(itemModule -> itemModule.getSpeedMultiplierModifier(itemStack))
                .reduce(speedModifier, (a, b) -> a * b);

        speedModifier *= getCounterWeightMultiplier(itemStack);

        if (speedModifier < -4) {
            speedModifier = -3.9d;
        }

        return speedModifier;
    }

    public double getCounterWeightMultiplier(ItemStack itemStack) {
        int counterWeightLevel = getEffectLevel(itemStack, ItemEffect.counterweight);
        if (counterWeightLevel > 0) {
            int integrityCost = getIntegrityCost(itemStack);

            return 0.5 + Math.abs(counterWeightLevel + integrityCost) * 0.2;
        }
        return 1;
    }

    public static double getSpeedModifierStatic(ItemStack itemStack) {
        if (itemStack.getItem() instanceof ItemModularHandheld) {
            return ((ItemModularHandheld) itemStack.getItem()).getSpeedModifier(itemStack);
        }
        return 2;
    }

    @Override
    public Set<ToolType> getToolTypes(ItemStack stack) {
        if (!isBroken(stack)) {
            return getCapabilities(stack).stream()
                    .map(Enum::toString)
                    .map(ToolType::get)
                    .collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }

    @Override
    public int getHarvestLevel(ItemStack stack, ToolType tool, @Nullable PlayerEntity player, @Nullable BlockState blockState) {
        if (!isBroken(stack)) {
            // todo: change tool capabilities to be zero indexed to align with vanilla
            int capabilityLevel = getCapabilityLevel(stack, tool);
            if (capabilityLevel > 0) {
                return capabilityLevel - 1;
            }
        }
        return -1;
    }

    @Override
    public boolean canHarvestBlock(ItemStack stack, BlockState state) {
        if (getHarvestLevel(stack, state.getHarvestTool(), null, state) >= state.getHarvestLevel()) {
            return true;
        } else {
            if (pickaxeHarvestMaterials.contains(state.getMaterial())) {
                return getHarvestLevel(stack, ToolType.PICKAXE, null, null) >= 0;
            } else if (axeHarvestMaterials.contains(state.getMaterial())) {
                return getHarvestLevel(stack, ToolType.AXE, null, null) >= 0;
            } else if (shovelHarvestBlocks.contains(state.getBlock())) {
                return getHarvestLevel(stack, ToolType.SHOVEL, null, null) >= 0;
            } else if (cuttingHarvestBlocks.contains(state.getBlock())) {
                return getHarvestLevel(stack, ToolTypes.cut, null, null) >= 0;
            }
        }

        return false;
    }

    @Override
    public float getDestroySpeed(ItemStack itemStack, BlockState blockState) {
        if (!isBroken(itemStack)) {
            ToolType tool = getEffectiveTool(blockState);
            float speed = (float) (4 + getSpeedModifier(itemStack));

            if (tool != null) {
                speed *= getCapabilityEfficiency(itemStack, tool);
            } else {
                speed *= getToolTypes(itemStack).stream()
                        .filter(blockState::isToolEffective)
                        .map(toolType -> getCapabilityEfficiency(itemStack, toolType))
                        .max(Comparator.naturalOrder())
                        .orElse(0f);
            }

            // todo: need a better way to handle how swords break cobwebs faster
            if (ToolTypes.cut.equals(tool) && blockState.getBlock().equals(Blocks.COBWEB)) {
                speed *= 10;
            }

            if (speed < 1) {
                return 1;
            }
            return speed;
        }
        return 1;
    }

    public static boolean isToolEffective(ToolType toolType, BlockState blockState) {
        if (axeHarvestMaterials.contains(blockState.getMaterial()) && ToolType.AXE.equals(toolType)) {
            return true;
        } else if (pickaxeHarvestMaterials.contains(blockState.getMaterial()) && ToolType.PICKAXE.equals(toolType)) {
            return true;
        } else if (cuttingHarvestBlocks.contains(blockState.getBlock()) && ToolTypes.cut.equals(toolType)) {
            return true;
        } else if (shovelHarvestBlocks.contains(blockState.getBlock()) && ToolType.SHOVEL.equals(toolType)) {
            return true;
        }

        if (axeDestroyBlocks.contains(blockState.getBlock()) && ToolType.AXE.equals(toolType)) {
            return true;
        } else if (pickaxeDestroyBlocks.contains(blockState.getBlock()) && ToolType.PICKAXE.equals(toolType)) {
            return true;
        } else if (cuttingDestroyMaterials.contains(blockState.getMaterial())
                || cuttingDestroyTags.stream().anyMatch(tag -> blockState.getBlock().isIn(tag)) && ToolTypes.cut.equals(toolType)) {
            return true;
        } else if (shovelDestroyBlocks.contains(blockState.getBlock()) && ToolType.SHOVEL.equals(toolType)) {
            return true;
        }

        return toolType.equals(blockState.getHarvestTool());
    }

    public static ToolType getEffectiveTool(BlockState blockState) {
        ToolType tool = blockState.getHarvestTool();

        if (tool != null) {
            return tool;
        }

        if (axeHarvestMaterials.contains(blockState.getMaterial())) {
            return ToolType.AXE;
        } else if (pickaxeHarvestMaterials.contains(blockState.getMaterial())) {
            return ToolType.PICKAXE;
        } else if (cuttingHarvestBlocks.contains(blockState.getBlock())) {
            return ToolTypes.cut;
        } else if (shovelHarvestBlocks.contains(blockState.getBlock())) {
            return ToolType.SHOVEL;
        }

        if (axeDestroyBlocks.contains(blockState.getBlock())) {
            return ToolType.AXE;
        } else if (pickaxeDestroyBlocks.contains(blockState.getBlock())) {
            return ToolType.PICKAXE;
        } else if (cuttingDestroyMaterials.contains(blockState.getMaterial())
                || cuttingDestroyTags.stream().anyMatch(tag -> blockState.getBlock().isIn(tag))) {
            return ToolTypes.cut;
        } else if (shovelDestroyBlocks.contains(blockState.getBlock())) {
            return ToolType.SHOVEL;
        }
        return null;
    }

    @Override
    public boolean isEnchantable(final ItemStack stack) {
        return false;
    }

    @Override
    public boolean isBookEnchantable(final ItemStack stack, final ItemStack book) {
        return false;
    }

    @Override
    public ItemStack onCraftConsumeCapability(ItemStack providerStack, ItemStack targetStack, PlayerEntity player,
            Capability capability, int capabilityLevel, boolean consumeResources) {
        if (consumeResources) {
            applyDamage(capabilityLevel, providerStack, player);

            causeFierySelfEffect(player, providerStack, capabilityLevel * 2);
            causeEnderReverbEffect(player, providerStack, capabilityLevel * 2);
            causeHauntEffect(player, providerStack, capabilityLevel * 2);

            tickProgression(player, providerStack, capabilityLevel * 2);
        }

        return super.onCraftConsumeCapability(providerStack, targetStack, player, capability, capabilityLevel, consumeResources);
    }

    @Override
    public ItemStack onActionConsumeCapability(ItemStack providerStack, ItemStack targetStack, PlayerEntity player,
            Capability capability, int capabilityLevel, boolean consumeResources) {
        if (consumeResources) {
            applyDamage(capabilityLevel, providerStack, player);

            causeFierySelfEffect(player, providerStack, capabilityLevel * 2);
            causeEnderReverbEffect(player, providerStack, capabilityLevel * 2);
            causeHauntEffect(player, providerStack, capabilityLevel * 2);

            tickProgression(player, providerStack, capabilityLevel * 2);
        }

        return super.onCraftConsumeCapability(providerStack, targetStack, player, capability, capabilityLevel, consumeResources);
    }
}
