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
import com.bromax.pestilence.entity.afflicted.AfflictedVillager;
import com.bromax.pestilence.block.PlagueCrossBlock;
import com.google.common.collect.ImmutableSet;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import com.bromax.pestilence.item.PlagueMaskItem;
import com.bromax.pestilence.item.ShroudedCorpseItem;
import com.bromax.pestilence.apothecary.ApothecaryRecipe;
import com.bromax.pestilence.block.ApothecaryTableBlock;
import com.bromax.pestilence.block.ApothecaryTableBlockEntity;
import com.bromax.pestilence.block.DryingRackBlock;
import com.bromax.pestilence.block.DryingRackBlockEntity;
import com.bromax.pestilence.block.HerbBlock;
import com.bromax.pestilence.disease.Ailment;
import com.bromax.pestilence.disease.Disease;
import com.bromax.pestilence.item.CenserItem;
import com.bromax.pestilence.item.GarbItem;
import com.bromax.pestilence.item.PhysiciansKitItem;
import com.bromax.pestilence.item.RemedyItem;
import com.bromax.pestilence.item.TreatmentItem;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
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
    public static final DeferredRegister<PoiType> POI_TYPES = DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, MODID);
    public static final DeferredRegister<VillagerProfession> PROFESSIONS = DeferredRegister.create(Registries.VILLAGER_PROFESSION, MODID);
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MODID);
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, MODID);

    // Disease state (which sickness, and whether it has been diagnosed)
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Ailment>> AILMENT = ATTACHMENTS.register("ailment",
            () -> AttachmentType.builder(() -> Ailment.NONE).serialize(Ailment.CODEC).build());

    public static final DeferredHolder<RecipeType<?>, RecipeType<ApothecaryRecipe>> APOTHECARY_TYPE = RECIPE_TYPES.register("apothecary",
            () -> new RecipeType<ApothecaryRecipe>() {
                @Override
                public String toString() {
                    return MODID + ":apothecary";
                }
            });
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ApothecaryRecipe>> APOTHECARY_SERIALIZER =
            RECIPE_SERIALIZERS.register("apothecary", ApothecaryRecipe.Serializer::new);

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
    public static final DeferredHolder<EntityType<?>, EntityType<AfflictedVillager>> AFFLICTED_VILLAGER = mob("afflicted_villager", AfflictedVillager::new, MobCategory.MONSTER, 0.6f, 1.95f);
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

    public static final DeferredBlock<PlagueCrossBlock> PLAGUE_CROSS = BLOCKS.register("plague_cross", () -> new PlagueCrossBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(0.5f).sound(SoundType.WOOD).noCollission().ignitedByLava()));

    public static final TagKey<Item> BRAZIER_FUEL = TagKey.create(Registries.ITEM, Pestilence.id("brazier_fuel"));
    public static final TagKey<Item> CENSER_FUEL = TagKey.create(Registries.ITEM, Pestilence.id("censer_fuel"));
    public static final TagKey<Item> DOCTORS_GARB = TagKey.create(Registries.ITEM, Pestilence.id("doctors_garb"));

    // Apothecary blocks
    public static final DeferredBlock<ApothecaryTableBlock> APOTHECARY_TABLE = BLOCKS.register("apothecary_table", () -> new ApothecaryTableBlock(
            BlockBehaviour.Properties.ofFullCopy(Blocks.CRAFTING_TABLE).noOcclusion()));
    public static final DeferredBlock<DryingRackBlock> DRYING_RACK = BLOCKS.register("drying_rack", () -> new DryingRackBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.5f).sound(SoundType.WOOD).noOcclusion().ignitedByLava()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ApothecaryTableBlockEntity>> APOTHECARY_TABLE_BE = BLOCK_ENTITIES.register(
            "apothecary_table", () -> BlockEntityType.Builder.of(ApothecaryTableBlockEntity::new, APOTHECARY_TABLE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DryingRackBlockEntity>> DRYING_RACK_BE = BLOCK_ENTITIES.register(
            "drying_rack", () -> BlockEntityType.Builder.of(DryingRackBlockEntity::new, DRYING_RACK.get()).build(null));

    // Herbs: the plant, the herb (which is also its seed) and the dried herb.
    public static final String[] HERBS = {"garlic", "rue", "sage", "wormwood", "rosemary"};
    public static final Map<String, DeferredBlock<HerbBlock>> HERB_PLANTS = new LinkedHashMap<>();
    public static final Map<String, DeferredItem<Item>> HERB_ITEMS = new LinkedHashMap<>();
    public static final Map<String, DeferredItem<Item>> DRIED_HERBS = new LinkedHashMap<>();

    static {
        for (String herb : HERBS) {
            DeferredBlock<HerbBlock> plant = BLOCKS.register(herb + "_plant", () -> new HerbBlock(HERB_ITEMS.get(herb),
                    BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT)));
            HERB_PLANTS.put(herb, plant);
            HERB_ITEMS.put(herb, ITEMS.register(herb, () -> new ItemNameBlockItem(plant.get(), new Item.Properties())));
            DRIED_HERBS.put(herb, ITEMS.registerSimpleItem("dried_" + herb));
        }
    }

    private static Map<Item, Item> dried;

    /** Fresh herb -> dried herb, for the Drying Rack. */
    public static Map<Item, Item> driedForms() {
        if (dried == null) {
            dried = new java.util.HashMap<>();
            for (String herb : HERBS) dried.put(HERB_ITEMS.get(herb).get(), DRIED_HERBS.get(herb).get());
        }
        return dried;
    }

    // The village physician works at an Apothecary Table.
    public static final DeferredHolder<PoiType, PoiType> APOTHECARY_POI = POI_TYPES.register("apothecary",
            () -> new PoiType(ImmutableSet.copyOf(APOTHECARY_TABLE.get().getStateDefinition().getPossibleStates()), 1, 1));
    public static final DeferredHolder<VillagerProfession, VillagerProfession> PHYSICIAN = PROFESSIONS.register("physician",
            () -> new VillagerProfession("physician", h -> h.is(APOTHECARY_POI.getKey()), h -> h.is(APOTHECARY_POI.getKey()),
                    ImmutableSet.of(), ImmutableSet.of(), SoundEvents.VILLAGER_WORK_CLERIC));

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> GARB_MATERIAL = ARMOR_MATERIALS.register("doctors_garb", () -> {
        EnumMap<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        defense.put(ArmorItem.Type.HELMET, 1);
        defense.put(ArmorItem.Type.CHESTPLATE, 3);
        defense.put(ArmorItem.Type.LEGGINGS, 2);
        defense.put(ArmorItem.Type.BOOTS, 1);
        defense.put(ArmorItem.Type.BODY, 3);
        return new ArmorMaterial(defense, 15, SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.of(Items.LEATHER),
                List.of(new ArmorMaterial.Layer(Pestilence.id("doctors_garb"))), 0f, 0f);
    });

    // Items
    public static final DeferredItem<Item> PLAGUE_MASK = ITEMS.register("plague_mask",
            () -> new PlagueMaskItem(PLAGUE_MASK_MATERIAL, new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(13))));
    public static final DeferredItem<Item> FOUR_THIEVES_VINEGAR = remedy("four_thieves_vinegar", UseAnim.DRINK, Items.GLASS_BOTTLE, Disease.PNEUMONIC);
    public static final DeferredItem<Item> PLAGUE_POULTICE = remedy("plague_poultice", UseAnim.BRUSH, null, Disease.BUBONIC);
    public static final DeferredItem<Item> WORMWOOD_TINCTURE = remedy("wormwood_tincture", UseAnim.DRINK, Items.GLASS_BOTTLE, Disease.SEPTICEMIC);
    public static final DeferredItem<Item> HERBAL_BROTH = remedy("herbal_broth", UseAnim.EAT, Items.BOWL, Disease.DYSENTERY);
    public static final DeferredItem<Item> THERIAC = remedy("theriac", UseAnim.DRINK, Items.GLASS_BOTTLE);
    public static final DeferredItem<Item> MORTAR_AND_PESTLE = ITEMS.register("mortar_and_pestle", () -> new Item(new Item.Properties().durability(128)));
    public static final DeferredItem<Item> PHYSICIANS_KIT = ITEMS.register("physicians_kit", () -> new PhysiciansKitItem(new Item.Properties().durability(64)));
    public static final DeferredItem<Item> LANCET = ITEMS.register("lancet", () -> new TreatmentItem(new Item.Properties().durability(32), TreatmentItem.Kind.LANCET));
    public static final DeferredItem<Item> LEECH_JAR = ITEMS.register("leech_jar", () -> new TreatmentItem(new Item.Properties().stacksTo(16), TreatmentItem.Kind.LEECHES));
    public static final DeferredItem<Item> CENSER = ITEMS.register("censer", () -> new CenserItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> DOCTORS_COAT = ITEMS.register("doctors_coat", () -> new GarbItem(GARB_MATERIAL, ArmorItem.Type.CHESTPLATE,
            new Item.Properties().durability(ArmorItem.Type.CHESTPLATE.getDurability(13))));
    public static final DeferredItem<Item> DOCTORS_TROUSERS = ITEMS.register("doctors_trousers", () -> new GarbItem(GARB_MATERIAL, ArmorItem.Type.LEGGINGS,
            new Item.Properties().durability(ArmorItem.Type.LEGGINGS.getDurability(13))));
    public static final DeferredItem<Item> DOCTORS_BOOTS = ITEMS.register("doctors_boots", () -> new GarbItem(GARB_MATERIAL, ArmorItem.Type.BOOTS,
            new Item.Properties().durability(ArmorItem.Type.BOOTS.getDurability(13))));
    public static final DeferredItem<?> APOTHECARY_TABLE_ITEM = ITEMS.registerSimpleBlockItem(APOTHECARY_TABLE);
    public static final DeferredItem<?> DRYING_RACK_ITEM = ITEMS.registerSimpleBlockItem(DRYING_RACK);
    public static final DeferredItem<?> PLAGUE_CROSS_ITEM = ITEMS.registerSimpleBlockItem(PLAGUE_CROSS);
    public static final DeferredItem<Item> AFFLICTED_VILLAGER_SPAWN_EGG = egg("afflicted_villager", AFFLICTED_VILLAGER, 0x7a6a5a, 0x8c8a3a);
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

    private static DeferredItem<Item> remedy(String name, UseAnim anim, @org.jetbrains.annotations.Nullable Item container, Disease... cures) {
        return ITEMS.register(name, () -> new RemedyItem(new Item.Properties().stacksTo(16), Set.of(cures), anim, container));
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
        ATTACHMENTS.register(bus);
        POI_TYPES.register(bus);
        PROFESSIONS.register(bus);
        RECIPE_TYPES.register(bus);
        RECIPE_SERIALIZERS.register(bus);
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
        event.put(AFFLICTED_VILLAGER.get(), AfflictedVillager.createAttributes().build());
    }

    private static void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(PLAGUE_RAT.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(PLAGUE_DOCTOR.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mob::checkMobSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        for (var type : List.of(AFFLICTED_COW, AFFLICTED_PIG, AFFLICTED_SHEEP, AFFLICTED_CHICKEN, AFFLICTED_HORSE, AFFLICTED_WOLF, AFFLICTED_VILLAGER)) {
            registerAfflicted(event, type.get());
        }
    }

    private static <T extends AfflictedMob> void registerAfflicted(RegisterSpawnPlacementsEvent event, EntityType<T> type) {
        event.register(type, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}
