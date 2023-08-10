package com.kawaiicakes.chemistrycraft.registry;

import com.kawaiicakes.chemistrycraft.common.blocks.OreBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.kawaiicakes.chemistrycraft.ChemistryCraft.MOD_ID;
import static net.minecraftforge.registries.ForgeRegistries.BLOCKS;

public class BlockRegistry {
    public static final DeferredRegister<Block> ORES = DeferredRegister.create(BLOCKS, MOD_ID);
    public static final List<OreBlock> STONE_ORE_BLOCKS = new ArrayList<>();
    public static final List<OreBlock> DEEPSLATE_ORE_BLOCKS = new ArrayList<>();

    public static final BlockBehaviour.Properties STONE_ORE_PROPERTIES = BlockBehaviour.Properties.of(Material.STONE).sound(SoundType.STONE);
    public static final BlockBehaviour.Properties DEEPSLATE_ORE_PROPERTIES = BlockBehaviour.Properties.of(new Material.Builder(MaterialColor.DEEPSLATE).build()).sound(SoundType.DEEPSLATE);

    public static Optional<RegistryObject<Block>> getRegistryObjectByName(String pName) {
        return ORES.getEntries().stream().filter(blockRegistryObject -> blockRegistryObject.getId().getPath().equals(pName)).findFirst();
    }

    public static void register(IEventBus bus)
    {
        ORES.register(bus);
    }
}
