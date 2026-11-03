package com.deepspace.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.LootModifier;
import noobanidus.mods.lootr.block.entities.LootrBarrelBlockEntity;
import noobanidus.mods.lootr.block.entities.LootrChestBlockEntity;
import org.jetbrains.annotations.NotNull;

/** Removes vanilla TNT only from Lootr chest/barrel generated loot. */
public final class TntLootModifier extends LootModifier {
    public static final Codec<TntLootModifier> CODEC = RecordCodecBuilder.create(instance ->
        LootModifier.codecStart(instance).apply(instance, TntLootModifier::new));

    public TntLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(
            ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        BlockEntity blockEntity = context.getParamOrNull(LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof LootrChestBlockEntity || blockEntity instanceof LootrBarrelBlockEntity) {
            generatedLoot.removeIf(stack -> stack.is(Items.TNT));
        }
        return generatedLoot;
    }

    @Override
    public Codec<? extends net.minecraftforge.common.loot.IGlobalLootModifier> codec() {
        return CODEC;
    }
}
