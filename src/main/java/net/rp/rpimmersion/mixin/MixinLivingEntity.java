package net.rp.rpimmersion.mixin;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.rp.rpimmersion.config.EffectsConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity {

    @Shadow @Final
    private static EntityDataAccessor<List<ParticleOptions>> DATA_EFFECT_PARTICLES;

    @Inject(method = "updateSynchronizedMobEffectParticles", at = @At("RETURN"), remap = false)
    private void rpimmersion$hideParticles(CallbackInfo ci) {
        try {
            if (EffectsConfig.HIDE_POTION_PARTICLES.get())
                ((LivingEntity) (Object) this).getEntityData().set(DATA_EFFECT_PARTICLES, List.of());
        } catch (IllegalStateException ignored) {}
    }
}