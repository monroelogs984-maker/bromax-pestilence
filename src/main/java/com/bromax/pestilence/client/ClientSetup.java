package com.bromax.pestilence.client;

import com.bromax.pestilence.ModRegistry;
import com.bromax.pestilence.Pestilence;
import com.bromax.pestilence.item.GarbItem;
import com.bromax.pestilence.item.PlagueMaskItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import net.minecraft.world.entity.Entity;

@EventBusSubscriber(modid = Pestilence.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        geo(event, ModRegistry.PLAGUE_RAT.get(), "plague_rat", 1f);
        // Humanoids are authored at 1px = 1/16 block; 0.9375 is the vanilla humanoid render scale.
        geo(event, ModRegistry.PLAGUE_DOCTOR.get(), "plague_doctor", 0.9375f);
        geo(event, ModRegistry.AFFLICTED_COW.get(), "afflicted_cow", 1f);
        geo(event, ModRegistry.AFFLICTED_PIG.get(), "afflicted_pig", 1f);
        geo(event, ModRegistry.AFFLICTED_SHEEP.get(), "afflicted_sheep", 1f);
        geo(event, ModRegistry.AFFLICTED_CHICKEN.get(), "afflicted_chicken", 1f);
        geo(event, ModRegistry.AFFLICTED_HORSE.get(), "afflicted_horse", 1f);
        geo(event, ModRegistry.AFFLICTED_WOLF.get(), "afflicted_wolf", 1f);
        geo(event, ModRegistry.AFFLICTED_VILLAGER.get(), "afflicted_villager", 0.9375f);
        geo(event, ModRegistry.PLAGUE_BEARER.get(), "plague_bearer", 1.05f);
        geo(event, ModRegistry.GRAVEDIGGER.get(), "gravedigger", 0.9375f);
        geo(event, ModRegistry.FLAGELLANT.get(), "flagellant", 0.9375f);
        geo(event, ModRegistry.CENSER_MONK.get(), "censer_monk", 0.9375f);
        geo(event, ModRegistry.RATCATCHER.get(), "ratcatcher", 0.9375f);
        geo(event, ModRegistry.MOURNER.get(), "mourner", 0.9375f);
        geo(event, ModRegistry.BONE_COLLECTOR.get(), "bone_collector", 0.9375f);
        geo(event, ModRegistry.PUSTULE.get(), "pustule", 1f);
        geo(event, ModRegistry.SEWER_RAT.get(), "sewer_rat", 1.5f);
        geo(event, ModRegistry.BLOATED_RAT.get(), "bloated_rat", 1f);
        geo(event, ModRegistry.RAT_SWARM.get(), "rat_swarm", 1f);
        geo(event, ModRegistry.CARRION_CROW.get(), "carrion_crow", 1f);
        event.registerEntityRenderer(ModRegistry.MIASMA_WRAITH.get(), PlagueRenderers.Wraith::new);
        event.registerEntityRenderer(ModRegistry.BURROWER_RAT.get(), PlagueRenderers.Burrower::new);
        event.registerEntityRenderer(ModRegistry.FLEA_CLOUD.get(), NoopRenderer::new);
        event.registerEntityRenderer(ModRegistry.FLY_SWARM.get(), NoopRenderer::new);
        event.registerEntityRenderer(ModRegistry.CORPSE.get(), CorpseRenderer::new);
        event.registerEntityRenderer(ModRegistry.MIASMA_CLOUD.get(), NoopRenderer::new);
        event.registerBlockEntityRenderer(ModRegistry.APOTHECARY_TABLE_BE.get(), ItemHolderRenderer::new);
        event.registerBlockEntityRenderer(ModRegistry.DRYING_RACK_BE.get(), ItemHolderRenderer::new);
    }

    private static <T extends Entity & GeoEntity> void geo(EntityRenderersEvent.RegisterRenderers event, EntityType<T> type, String model, float scale) {
        EntityRendererProvider<T> provider = ctx -> new GeoEntityRenderer<>(ctx, new DefaultedEntityGeoModel<T>(Pestilence.id(model), true)).withScale(scale);
        event.registerEntityRenderer(type, provider);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModRegistry.FLY.get(), FlyParticle.Provider::new);
    }

    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            private GeoArmorRenderer<PlagueMaskItem> renderer;

            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
                if (renderer == null) renderer = new GeoArmorRenderer<>(new DefaultedItemGeoModel<>(Pestilence.id("armor/plague_mask")));
                renderer.prepForRender(entity, stack, slot, original);
                return renderer;
            }
        }, ModRegistry.PLAGUE_MASK.get());
        // The coat, trousers and boots share one model; GeckoLib shows the bones for the slot being drawn.
        for (var garb : java.util.List.of(ModRegistry.DOCTORS_COAT, ModRegistry.DOCTORS_TROUSERS, ModRegistry.DOCTORS_BOOTS)) {
            event.registerItem(new IClientItemExtensions() {
                private GeoArmorRenderer<GarbItem> renderer;

                @Override
                public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
                    if (renderer == null) renderer = new GeoArmorRenderer<>(new DefaultedItemGeoModel<>(Pestilence.id("armor/doctors_garb")));
                    renderer.prepForRender(entity, stack, slot, original);
                    return renderer;
                }
            }, garb.get());
        }
    }
}
