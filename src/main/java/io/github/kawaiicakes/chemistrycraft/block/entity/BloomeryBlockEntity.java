package io.github.kawaiicakes.chemistrycraft.block.entity;

import io.github.kawaiicakes.chemistrycraft.recipe.BloomeryRecipe;
import io.github.kawaiicakes.chemistrycraft.screen.BloomeryBlockMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import static io.github.kawaiicakes.chemistrycraft.registry.BlockEntityRegistry.BLOOMERY_ENTITY;
import static io.github.kawaiicakes.chemistrycraft.registry.ItemRegistry.BLOOMERY_ITEM;

public class BloomeryBlockEntity extends BlockEntity implements MenuProvider {
    //  Inventory of the block entity
    private final ItemStackHandler itemHandler = new ItemStackHandler(3) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged(); //    Reloads chunk/block on change of contents
        }
    };

    //  Makes inventory available via capabilities
    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    protected final ContainerData data; //  This field is responsible for carrying data generated via ticking for display on the GUI later.
    private int progress = 0;
    private int maxProgress = 78;

    public BloomeryBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(BLOOMERY_ENTITY.get(), blockPos, blockState);
        this.data = new ContainerData() {
            @Override
            public int get(int index) { //  'Saves' these values into our ContainerData
                return switch (index) {
                    case 0 -> BloomeryBlockEntity.this.progress;
                    case 1 -> BloomeryBlockEntity.this.maxProgress;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) { //  'Saves' these values into our ContainerData
                switch (index) {
                    case 0 -> BloomeryBlockEntity.this.progress = value;
                    case 1 -> BloomeryBlockEntity.this.maxProgress = value;
                }
            }

            @Override
            public int getCount() { //  number of saved variables in ContainerData
                return 2;
            }
        };
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("BALLS!!");
    }

    @Nullable
    @Override //    renders GUI when called
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new BloomeryBlockMenu(id, inventory, this, this.data);
    }

    @Override //    Allows import/export to inventory
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        }

        return super.getCapability(cap, side);
    }

    @Override //    ?
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
    }

    @Override //    ?
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
    }

    @Override   //  Saves inventory
    protected void saveAdditional(CompoundTag nbt) {
        nbt.put("inventory", itemHandler.serializeNBT());
        nbt.putInt("bloomery_progress", this.progress);

        super.saveAdditional(nbt);
    }

    @Override   //  Loads inventory
    public void load(CompoundTag nbt) {
        super.load(nbt);
        itemHandler.deserializeNBT(nbt.getCompound("inventory"));
        this.progress = nbt.getInt("bloomery_progress");
    }

    //  Called inside of block class when destroyed so that inventory drops
    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }

        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

    public static void tick(Level level, BlockPos blockPos, BlockState blockState, BloomeryBlockEntity bloomeryBlockEntity) {
        if (!level.isClientSide()) {
            if (hasRecipe(bloomeryBlockEntity)) {
                bloomeryBlockEntity.progress++;
                setChanged(level, blockPos, blockState); // Causes reload when necessary

                if (bloomeryBlockEntity.progress >= bloomeryBlockEntity.maxProgress) {
                    craftItem(bloomeryBlockEntity);
                }
            } else {
                bloomeryBlockEntity.resetProgress();
                setChanged(level, blockPos, blockState);
            }
        }
    }

    private void resetProgress() {
        this.progress = 0;
    }
    private static void craftItem(BloomeryBlockEntity entity) { // Called when item is crafted
        Level level = entity.getLevel();
        SimpleContainer inventory = new SimpleContainer(entity.itemHandler.getSlots()); //  makes inventory to make life easier
        for (int i = 0; i < entity.itemHandler.getSlots(); ++i) {
            inventory.setItem(i, entity.itemHandler.getStackInSlot(i));
        }

        Optional<BloomeryRecipe> recipe = level.getRecipeManager().getRecipeFor(BloomeryRecipe.Type.INSTANCE, inventory, level);

        if (hasRecipe(entity)) {
            entity.itemHandler.extractItem(1, 1, false); // recipe.get().getResultItem().getItem(); ignores count!
            entity.itemHandler.setStackInSlot(2, new ItemStack(recipe.get().getResultItem().getItem(), entity.itemHandler.getStackInSlot(2).getCount() + 1));

            entity.resetProgress();
        }
    }

    private static boolean hasRecipe(BloomeryBlockEntity entity) { // Checks if a valid recipe exists
        Level level = entity.getLevel();
        SimpleContainer inventory = new SimpleContainer(entity.itemHandler.getSlots()); //  makes inventory to make life easier
        for (int i = 0; i < entity.itemHandler.getSlots(); ++i) {
            inventory.setItem(i, entity.itemHandler.getStackInSlot(i));
        }

        Optional<BloomeryRecipe> recipe = level.getRecipeManager().getRecipeFor(BloomeryRecipe.Type.INSTANCE, inventory, level);

        return recipe.isPresent() && canInsertAmountIntoOutputSlot(inventory) && canInsertItemIntoOutputSlot(inventory, recipe.get().getResultItem());
    }

    private static boolean canInsertItemIntoOutputSlot(SimpleContainer inventory, ItemStack itemStack) {
        return inventory.getItem(2).getItem() == itemStack.getItem() || inventory.getItem(2).isEmpty();
    }

    private static boolean canInsertAmountIntoOutputSlot(SimpleContainer inventory) {
        return inventory.getItem(2).getMaxStackSize() > inventory.getItem(2).getCount();
    }


}
