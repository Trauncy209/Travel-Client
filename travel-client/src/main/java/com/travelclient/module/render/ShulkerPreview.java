package com.travelclient.module.render;

import com.travelclient.module.Module;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.collection.DefaultedList;

import java.util.List;

public class ShulkerPreview extends Module {
    
    public ShulkerPreview() {
        super("ShulkerPreview", "Hold ALT to preview shulker contents", Category.RENDER);
        setEnabled(true); // Enabled by default
    }
    
    @Override
    public void onTick() {
        // Nothing needed here - rendering is done via mixin
    }
    
    /**
     * Check if an item is a shulker box
     */
    public static boolean isShulkerBox(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!(stack.getItem() instanceof BlockItem blockItem)) return false;
        return blockItem.getBlock() instanceof ShulkerBoxBlock;
    }
    
    /**
     * Get the contents of a shulker box
     */
    public static DefaultedList<ItemStack> getShulkerContents(ItemStack shulker) {
        DefaultedList<ItemStack> items = DefaultedList.ofSize(27, ItemStack.EMPTY);
        
        if (!isShulkerBox(shulker)) return items;
        
        // Get container component (1.21+ way)
        ContainerComponent container = shulker.get(DataComponentTypes.CONTAINER);
        if (container != null) {
            int i = 0;
            for (ItemStack stack : container.iterateNonEmpty()) {
                if (i < 27) {
                    items.set(i, stack);
                    i++;
                }
            }
        }
        
        return items;
    }
    
    /**
     * Render the shulker preview box
     */
    public static void renderPreview(DrawContext context, ItemStack shulker, int mouseX, int mouseY) {
        if (!isShulkerBox(shulker)) return;
        
        MinecraftClient mc = MinecraftClient.getInstance();
        
        DefaultedList<ItemStack> contents = getShulkerContents(shulker);
        
        // Check if shulker has any items
        boolean hasItems = false;
        for (ItemStack stack : contents) {
            if (!stack.isEmpty()) {
                hasItems = true;
                break;
            }
        }
        
        if (!hasItems) return;
        
        // Calculate preview box dimensions
        int boxWidth = 9 * 18 + 8;  // 9 slots wide + padding
        int boxHeight = 3 * 18 + 8; // 3 rows + padding
        
        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();
        
        // Position the preview box - ALWAYS on the LEFT side of cursor to avoid tooltip
        // Tooltip always appears to the right, so we go left
        int x = mouseX - boxWidth - 20;
        int y = mouseY - boxHeight / 2; // Center vertically on cursor
        
        // If too far left, put it at the top of screen instead
        if (x < 5) {
            x = 5;
            y = 5; // Top left corner
        }
        
        // Keep on screen vertically
        if (y < 5) {
            y = 5;
        }
        if (y + boxHeight > screenHeight - 5) {
            y = screenHeight - boxHeight - 5;
        }
        
        // Draw background with thicker border for visibility
        context.fill(x - 4, y - 4, x + boxWidth + 4, y + boxHeight + 4, 0xFF000000); // Black outer border
        context.fill(x - 3, y - 3, x + boxWidth + 3, y + boxHeight + 3, 0xFF555555); // Dark gray border
        context.fill(x - 2, y - 2, x + boxWidth + 2, y + boxHeight + 2, 0xFF8B8B8B); // Gray border
        context.fill(x, y, x + boxWidth, y + boxHeight, 0xFFC6C6C6); // Light gray background
        
        // Draw "Shulker Contents" title
        String title = "§8Shulker Contents";
        context.drawTextWithShadow(mc.textRenderer, title, x + 4, y - 12, 0xFFFFFF);
        
        // Draw slot backgrounds
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotX = x + 4 + col * 18;
                int slotY = y + 4 + row * 18;
                context.fill(slotX, slotY, slotX + 16, slotY + 16, 0xFF8B8B8B); // Slot background
            }
        }
        
        // Draw items
        for (int i = 0; i < 27; i++) {
            ItemStack stack = contents.get(i);
            if (stack.isEmpty()) continue;
            
            int row = i / 9;
            int col = i % 9;
            int itemX = x + 4 + col * 18;
            int itemY = y + 4 + row * 18;
            
            context.drawItem(stack, itemX, itemY);
            context.drawItemInSlot(mc.textRenderer, stack, itemX, itemY);
        }
    }
    
    /**
     * Get the slot the mouse is hovering over
     */
    public static Slot getHoveredSlot(HandledScreen<?> screen, double mouseX, double mouseY) {
        try {
            // Use reflection to get the focusedSlot field
            for (var field : HandledScreen.class.getDeclaredFields()) {
                if (field.getType() == Slot.class) {
                    field.setAccessible(true);
                    return (Slot) field.get(screen);
                }
            }
        } catch (Exception e) {
            // Fallback - ignore
        }
        return null;
    }
}
