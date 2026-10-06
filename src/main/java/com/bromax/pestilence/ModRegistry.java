package com.bromax.pestilence;

import com.bromax.pestilence.block.BlightedSoilBlock;
import com.bromax.pestilence.block.BrazierBlockEntity;
import com.bromax.pestilence.block.HerbBrazierBlock;
import com.bromax.pestilence.block.NestBlock;
import com.bromax.pestilence.block.NestBlockEntity;
import com.bromax.pestilence.block.NestKind;
import com.bromax.pestilence.block.PyreBlock;
import com.bromax.pestilence.block.WitheredLeavesBlock;
import com.bromax.pestilence.item.QuicklimeItem;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.PushReaction;
import com.bromax.pestilence.effect.ImmunityEffect;
import com.bromax.pestilence.effect.PlagueEffect;
import com.bromax.pestilence.entity.Corpse;
import com.bromax.pestilence.entity.MiasmaCloud;
import com.bromax.pestilence.entity.PlagueDoctor;
import com.bromax.pestilence.entity.PlagueRat;
import com.bromax.pestilence.entity.afflicted.AfflictedChicken;
import com.bromax.pestilence.entity.afflicted.AfflictedCow;
import com.bromax.pestilence.entity.afflicted.AfflictedHorse;
import com.bromax.pestilence.entity.afflicted.AfflictedMob;
import com.bromax.pestilence.entity.afflicted.AfflictedPig;
import com.bromax.pestilence.entity.afflicted.AfflictedSheep;
import com.bromax.pestilence.entity.afflicted.AfflictedWolf;
import com.bromax.pestilence.item.PlagueMaskItem;
import com.bromax.pestilence.item.ShroudedCorpseItem;
import com.bromax.pestilence.item.VinegarItem;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;
import java.util.function.Supplier;

import static com.bromax.pestilence.Pestilence.MODID;

public final class ModRegistry {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, MODID);
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, MODID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // Effects
    public static final DeferredHolder<MobEffect, MobEffect> PLAGUE = EFFECTS.register("plague", PlagueEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> IMMUNITY = EFFECTS.register("plague_immunity", ImmunityEffect::new);

    // Particles
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FLY = PARTICLES.register("fly", () -> new SimpleParticleType(false));

    // Entities
    public static final DeferredHolder<EntityType<?>, EntityType<PlagueRat>> PLAGUE_RAT = mob("plague_rat", PlagueRat::new, MobCategory.MONSTER, 0.6f, 0.45f);
    public static final DeferredHolder<EntityType<?>, EntityType<PlagueDoctor>> PLAGUE_DOCTOR = mob("plague_doctor", PlagueDoctor::new, MobCategory.CREATURE, 0.6f, 1.95f);
    public static final DeferredHolder<EntityType<?>, EntityType<AfflictedCow>> AFFLICTED_COW = mob("afflicted_cow", AfflictedCow::new, MobCategory.MONSTER, 0.9f, 1.4f);
    public static final DeferredHolder<EntityType<?>, EntityType<AfflictedPig>> AFFLICTED_PIG = mob("afflicted_pig", AfflictedPig::new, MobCategory.MONSTER, 0.9f, 0.9f);
    public static final DeferredHolder<EntityType<?>, EntityType<AfflictedSheep>> AFFLICTED_SHEEP = mob("afflicted_sheep", AfflictedSheep::new, MobCategory.MONSTER, 0.9f, 1.3f);
    public static final DeferredHolder<EntityType<?>, EntityType<AfflictedChicken>> AFFLICTED_CHICKEN = mob("afflicted_chicken", AfflictedChicken::new, MobCategory.MONSTER, 0.4f, 0.7f);
    public static final DeferredHolder<EntityType<?>, EntityType<AfflictedHorse>> AFFLICTED_HORSE = mob("afflicted_horse", AfflictedHorse::new, MobCategory.MONSTER, 1.3f, 1.6f);
    public static final DeferredHolder<EntityType<?>, EntityType<AfflictedWolf>> AFFLICTED_WOLF = mob("afflicted_wolf", AfflictedWolf::new, MobCategory.MONSTER, 0.6f, 0.85f);
    public static final DeferredHolder<EntityType<?>, EntityType<Corpse>> CORPSE = ENTITIES.register("corpse",
            () -> EntityType.Builder.<Corpse>of(Corpse::new, MobCategory.MISC).sized(0.9f, 0.5f).clientTrackingRange(8).updateInterval(10).build(MODID + ":corpse"));
    public static final DeferredHolder<EntityType<?>, EntityType<MiasmaCloud>> MIASMA_CLOUD = ENTITIES.register("miasma_cloud",
            () -> EntityType.Builder.<MiasmaCloud>of(MiasmaCloud::new, MobCategory.MISC).sized(1f, 1f).fireImmune().noSave()
                    .clientTrackingRange(8).updateInterval(20).build(MODID + ":miasma_cloud"));

    // Armour
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> PLAGUE_MASK_MATERIAL = ARMOR_MATERIALS.register("plague_mask", () -> {
        EnumMap<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        for (ArmorItem.Type type : ArmorItem.Type.values()) defense.put(type, type == ArmorItem.Type.HELMET ? 1 : 0);
        return new ArmorMaterial(defense, 15, SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.of(Items.LEATHER),
                List.of(new ArmorMaterial.Layer(Pestilence.id("plague_mask"))), 0f, 0f);
    });

    // Blocks
    public static final DeferredBlock<PyreBlock> PYRE = BLOCKS.register("pyre", () -> new PyreBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOD).strength(2f).sound(SoundType.WOOD).noOcclusion().ignitedByLava()
            .lightLevel(state -> state.getValue(PyreBlock.LIT) ? 15 : 0)));

    public static final DeferredBlock<NestBlock> RAT_WARREN = nest(NestKind.WARREN, MapColor.DIRT, SoundType.ROOTED_DIRT, 0);
    public static final DeferredBlock<NestBlock> PLAGUE_PIT = nest(NestKind.PIT, MapColor.TERRACOTTA_BROWN, SoundType.MUD, 0);
    public static final DeferredBlock<NestBlock> MIASMA_WELL = nest(NestKind.WELL, MapColor.COLOR_GREEN, SoundType.STONE, 3);
    public static final DeferredBlock<BlightedSoilBlock> BLIGHTED_SOIL = BLOCKS.register("blighted_soil", () -> new BlightedSoilBlock(
            BlockBehaviour.Properties.ofFullCopy(Blocks.DIRT).mapColor(MapColor.TERRACOTTA_GRAY).sound(SoundType.ROOTED_DIRT).randomTicks()));
    public static final DeferredBlock<WitheredLeavesBlock> WITHERED_LEAVES = BLOCKS.register("withered_leaves", () -> new WitheredLeavesBlock(
            BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES).mapColor(MapColor.TERRACOTTA_BROWN)));
    public static final DeferredBlock<HerbBrazierBlock> HERB_BRAZIER = BLOCKS.register("herb_brazier", () -> new HerbBrazierBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.5f).sound(SoundType.LANTERN).noOcclusion()
                    .lightLevel(state -> state.getValue(HerbBrazierBlock.LIT) ? 13 : 0)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NestBlockEntity>> NEST_BE = BLOCK_ENTITIES.register("nest",
            () -> BlockEntityType.Builder.of(NestBlockEntity::new, RAT_WARREN.get(), PLAGUE_PIT.get(), MIASMA_WELL.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BrazierBlockEntity>> BRAZIER_BE = BLOCK_ENTITIES.register("herb_brazier",
            () -> BlockEntityType.Builder.of(BrazierBlockEntity::new, HERB_BRAZIER.get()).build(null));

    public static final TagKey<Item> BRAZIER_FUEL = TagKey.create(Registries.ITEM, Pestilence.id("brazier_fuel"));

    // Items
    public static final DeferredItem<Item> PLAGUE_MASK = ITEMS.register("plague_mask",
            () -> new PlagueMaskItem(PLAGUE_MASK_MATERIAL, new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(13))));
    public static final DeferredItem<Item> FOUR_THIEVES_VINEGAR = ITEMS.register("four_thieves_vinegar", () -> new VinegarItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> RAT_TAIL = ITEMS.registerSimpleItem("rat_tail");
    public static final DeferredItem<Item> SHROUDED_CORPSE = ITEMS.register("shrouded_corpse", () -> new ShroudedCorpseItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<?> PYRE_ITEM = ITEMS.registerSimpleBlockItem(PYRE);
    public static final DeferredItem<Item> QUICKLIME = ITEMS.register("quicklime", () -> new QuicklimeItem(new Item.Properties()));
    public static final DeferredItem<?> HERB_BRAZIER_ITEM = ITEMS.registerSimpleBlockItem(HERB_BRAZIER);
    public static final DeferredItem<?> BLIGHTED_SOIL_ITEM = ITEMS.registerSimpleBlockItem(BLIGHTED_SOIL);
    public static final DeferredItem<?> WITHERED_LEAVES_ITEM = ITEMS.registerSimpleBlockItem(WITHERED_LEAVES);
    public static final DeferredItem<?> RAT_WARREN_ITEM = ITEMS.registerSimpleBlockItem(RAT_WARREN);
    public static final DeferredItem<?> PLAGUE_PIT_ITEM = ITEMS.registerSimpleBlockItem(PLAGUE_PIT);
    public static final DeferredItem<?> MIASMA_WELL_ITEM = ITEMS.registerSimpleBlockItem(MIASMA_WELL);
    public static final DeferredItem<Item> PLAGUE_RAT_SPAWN_EGG = egg("plague_rat", PLAGUE_RAT, 0x4a3f38, 0xc48b86);
    public static final DeferredItem<Item> PLAGUE_DOCTOR_SPAWN_EGG = egg("plague_doctor", PLAGUE_DOCTOR, 0x1c1a1a, 0xc9b48a);
    public static final DeferredItem<Item> AFFLICTED_COW_SPAWN_EGG = egg("afflicted_cow", AFFLICTED_COW, 0x4f4237, 0x8c8a3a);
    public static final DeferredItem<Item> AFFLICTED_PIG_SPAWN_EGG = egg("afflicted_pig", AFFLICTED_PIG, 0xa87a74, 0x8c8a3a);
    public static final DeferredItem<Item> AFFLICTED_SHEEP_SPAWN_EGG = egg("afflicted_sheep", AFFLICTED_SHEEP, 0x9c957f, 0x8c8a3a);
    public static final DeferredItem<Item> AFFLICTED_CHICKEN_SPAWN_EGG = egg("afflicted_chicken", AFFLICTED_CHICKEN, 0xa59e8f, 0x8c8a3a);
    public static final DeferredItem<Item> AFFLICTED_HORSE_SPAWN_EGG = egg("afflicted_horse", AFFLICTED_HORSE, 0x51463d, 0x8c8a3a);
    public static final DeferredItem<Item> AFFLICTED_WOLF_SPAWN_EGG = egg("afflicted_wolf", AFFLICTED_WOLF, 0x625e58, 0x8c8a3a);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("pestilence", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + MODID))
            .icon(() -> new ItemStack(PLAGUE_MASK.get()))
            .displayItems((params, out) -> ITEMS.getEntries().forEach(item -> out.accept(item.get())))
            .build());

    private ModRegistry() {}

    private static <T extends Mob> DeferredHolder<EntityType<?>, EntityType<T>> mob(String name, EntityType.EntityFactory<T> factory,
                                                                                   MobCategory category, float width, float height) {
        return ENTITIES.register(name, () -> EntityType.Builder.of(factory, category).sized(width, height)
                .clientTrackingRange(10).build(MODID + ":" + name));
    }

    private static DeferredBlock<NestBlock> nest(NestKind kind, MapColor color, SoundType sound, int light) {
        return BLOCKS.register(kind.id, () -> new NestBlock(kind, BlockBehaviour.Properties.of().mapColor(color).sound(sound)
                .strength(kind.hardness, kind.hardness * 2).pushReaction(PushReaction.BLOCK)
                .noLootTable().lightLevel(state -> light == 0 ? 0 : light + state.getValue(NestBlock.STAGE))));
    }

    private static DeferredItem<Item> egg(String name, Supplier<? extends EntityType<? extends Mob>> type, int bg, int fg) {
        return ITEMS.register(name + "_spawn_egg", () -> new DeferredSpawnEggItem(type, bg, fg, new Item.Properties()));
    }

    static void register(IEventBus bus) {
        EFFECTS.register(bus);
        PARTICLES.register(bus);
        ENTITIES.register(bus);
        ARMOR_MATERIALS.register(bus);
        BLOCKS.register(bus);
        BLOCK_ENTITIES.register(bus);
        ITEMS.register(bus);
        TABS.register(bus);
        bus.addListener(ModRegistry::onAttributes);
        bus.addListener(ModRegistry::onSpawnPlacements);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(PLAGUE_RAT.get(), PlagueRat.createAttributes().build());
        event.put(PLAGUE_DOCTOR.get(), PlagueDoctor.createAttributes().build());
        event.put(AFFLICTED_COW.get(), AfflictedCow.createAttributes().build());
        event.put(AFFLICTED_PIG.get(), AfflictedPig.createAttributes().build());
        event.put(AFFLICTED_SHEEP.get(), AfflictedSheep.createAttributes().build());
        event.put(AFFLICTED_CHICKEN.get(), AfflictedChicken.createAttributes().build());
        event.put(AFFLICTED_HORSE.get(), AfflictedHorse.createAttributes().build());
        event.put(AFFLICTED_WOLF.get(), AfflictedWolf.createAttributes().build());
    }

    private static void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(PLAGUE_RAT.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(PLAGUE_DOCTOR.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mob::checkMobSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        for (var type : List.of(AFFLICTED_COW, AFFLICTED_PIG, AFFLICTED_SHEEP, AFFLICTED_CHICKEN, AFFLICTED_HORSE, AFFLICTED_WOLF)) {
            registerAfflicted(event, type.get());
        }
    }

    private static <T extends AfflictedMob> void registerAfflicted(RegisterSpawnPlacementsEvent event, EntityType<T> type) {
        event.register(type, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}
