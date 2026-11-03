package com.deepspace.compat.superchat;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

import java.util.List;
import java.util.Set;

/**
 * Early Mixin 0.8.5 gate for optional Super Chat 3.1.0 targets.
 * This class intentionally references no Minecraft, Forge, BFCR, or DI types.
 *
 * IMPORTANT: this plugin must never call the Mixin class provider's findClass()
 * for an optional target. Doing so loads the target while Mixin is preparing
 * its configuration and causes MixinTargetAlreadyLoadedException. Resource
 * lookup is used instead because it tests for the class without loading it.
 */
public final class SuperChatCompatMixinPlugin implements IMixinConfigPlugin {
    private static final String TARGET = "net.mcreator.superchat.command.ChatInteractionHandler";
    private static final String GENETIC_ANIMALS_TARGET = "mokiyoki.enhancedanimals.GeneticAnimals";
    private static final String GENETIC_ANIMALS_RESOURCE = "mokiyoki/enhancedanimals/GeneticAnimals.class";
    private static final String TARGET_RESOURCE = "net/mcreator/superchat/command/ChatInteractionHandler.class";
    private static final String DISCORD = "com.deepspace.mixin.SuperChatDiscordIntegrationMixin";
    private static final String TIMESTAMP = "com.deepspace.mixin.SuperChatTimestampMixin";
    private static final String DISCORD_TARGET_RESOURCE = "de/erdbeerbaerlp/dcintegration/forge/DiscordIntegrationMod.class";

    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (GENETIC_ANIMALS_TARGET.equals(targetClassName)) {
            return classResourceExists(GENETIC_ANIMALS_RESOURCE);
        }
        if (!TARGET.equals(targetClassName)) return true;

        // Never use ClassProvider.findClass() here: it loads ChatInteractionHandler
        // during Mixin preparation and makes Mixin 0.8.5 reject the target as
        // already loaded. getResource() performs a non-loading classpath lookup.
        if (!classResourceExists(TARGET_RESOURCE)) return false;

        // Timestamp support only needs Super Chat. The Discord bridge additionally
        // needs Discord Integration; don't prepare that mixin when DI is absent.
        if (DISCORD.equals(mixinClassName)) {
            return classResourceExists(DISCORD_TARGET_RESOURCE);
        }
        if (TIMESTAMP.equals(mixinClassName)) return true;

        return true;
    }

    private static boolean classResourceExists(String resource) {
        try {
            ClassLoader loader = MixinService.getService().getClassProvider().getClass().getClassLoader();
            if (loader != null && loader.getResource(resource) != null) return true;
        } catch (Throwable ignored) {
            // Fall through to the plugin's own loader.
        }
        try {
            return SuperChatCompatMixinPlugin.class.getClassLoader().getResource(resource) != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass,
                                   String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass,
                                    String mixinClassName, IMixinInfo mixinInfo) {}
}


