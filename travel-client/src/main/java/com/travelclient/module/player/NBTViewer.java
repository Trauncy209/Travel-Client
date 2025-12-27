package com.travelclient.module.player;

import com.travelclient.module.Module;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.*;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NBTViewer extends Module {
    
    private List<String> nbtLines = new ArrayList<>();
    private int scrollOffset = 0;
    private boolean showDetailed = true;
    
    public NBTViewer() {
        super("NBTViewer", "View item NBT data (F4 to toggle detailed)", Category.PLAYER);
    }
    
    @Override
    public void onTick() {
        if (mc.player == null) return;
        
        ItemStack held = mc.player.getMainHandStack();
        if (held.isEmpty()) {
            nbtLines.clear();
            nbtLines.add("§7Hold an item to view NBT data");
            return;
        }
        
        nbtLines.clear();
        nbtLines.add("§6§l=== " + held.getName().getString() + " ===");
        nbtLines.add("§fItem: §7" + held.getItem().toString());
        nbtLines.add("§fCount: §7" + held.getCount());
        nbtLines.add("§fMax Stack: §7" + held.getMaxCount());
        
        if (held.isDamageable()) {
            nbtLines.add("§fDurability: §7" + (held.getMaxDamage() - held.getDamage()) + "/" + held.getMaxDamage());
        }
        
        nbtLines.add("");
        nbtLines.add("§e§l--- Components (1.21+) ---");
        
        // Get all components
        ComponentMap components = held.getComponents();
        
        // Custom name
        if (held.contains(DataComponentTypes.CUSTOM_NAME)) {
            Text name = held.get(DataComponentTypes.CUSTOM_NAME);
            nbtLines.add("§fCustom Name: §b" + (name != null ? name.getString() : "null"));
        }
        
        // Lore
        if (held.contains(DataComponentTypes.LORE)) {
            LoreComponent lore = held.get(DataComponentTypes.LORE);
            if (lore != null && !lore.lines().isEmpty()) {
                nbtLines.add("§fLore:");
                for (Text line : lore.lines()) {
                    nbtLines.add("  §7- §f" + line.getString());
                }
            }
        }
        
        // Enchantments
        if (held.contains(DataComponentTypes.ENCHANTMENTS)) {
            ItemEnchantmentsComponent enchants = held.get(DataComponentTypes.ENCHANTMENTS);
            if (enchants != null && !enchants.isEmpty()) {
                nbtLines.add("§fEnchantments:");
                for (var entry : enchants.getEnchantments()) {
                    int level = enchants.getLevel(entry);
                    String name = entry.getIdAsString();
                    nbtLines.add("  §d" + name + " §7Lv." + level);
                }
            }
        }
        
        // Stored Enchantments (books)
        if (held.contains(DataComponentTypes.STORED_ENCHANTMENTS)) {
            ItemEnchantmentsComponent stored = held.get(DataComponentTypes.STORED_ENCHANTMENTS);
            if (stored != null && !stored.isEmpty()) {
                nbtLines.add("§fStored Enchantments:");
                for (var entry : stored.getEnchantments()) {
                    int level = stored.getLevel(entry);
                    String name = entry.getIdAsString();
                    nbtLines.add("  §d" + name + " §7Lv." + level);
                }
            }
        }
        
        // Unbreakable
        if (held.contains(DataComponentTypes.UNBREAKABLE)) {
            nbtLines.add("§fUnbreakable: §atrue");
        }
        
        // Custom Model Data
        if (held.contains(DataComponentTypes.CUSTOM_MODEL_DATA)) {
            CustomModelDataComponent cmd = held.get(DataComponentTypes.CUSTOM_MODEL_DATA);
            if (cmd != null) {
                nbtLines.add("§fCustom Model Data: §7" + cmd.toString());
            }
        }
        
        // Attribute Modifiers
        if (held.contains(DataComponentTypes.ATTRIBUTE_MODIFIERS)) {
            AttributeModifiersComponent attrs = held.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
            if (attrs != null) {
                nbtLines.add("§fAttribute Modifiers:");
                for (var entry : attrs.modifiers()) {
                    nbtLines.add("  §7" + entry.attribute().getIdAsString());
                    nbtLines.add("    §8" + entry.modifier().id() + ": " + entry.modifier().value());
                }
            }
        }
        
        // Potion Contents
        if (held.contains(DataComponentTypes.POTION_CONTENTS)) {
            PotionContentsComponent potion = held.get(DataComponentTypes.POTION_CONTENTS);
            if (potion != null) {
                nbtLines.add("§fPotion:");
                if (potion.potion().isPresent()) {
                    nbtLines.add("  §7Base: " + potion.potion().get().getIdAsString());
                }
                for (var effect : potion.customEffects()) {
                    nbtLines.add("  §c" + effect.getEffectType().getIdAsString() + 
                                " §7Lv." + (effect.getAmplifier() + 1) + 
                                " §8(" + effect.getDuration()/20 + "s)");
                }
            }
        }
        
        // Container contents (shulkers, bundles)
        if (held.contains(DataComponentTypes.CONTAINER)) {
            ContainerComponent container = held.get(DataComponentTypes.CONTAINER);
            if (container != null) {
                nbtLines.add("§fContainer Contents:");
                int slot = 0;
                for (ItemStack stack : container.iterateNonEmpty()) {
                    nbtLines.add("  §7[" + slot + "] §f" + stack.getCount() + "x " + stack.getName().getString());
                    slot++;
                }
            }
        }
        
        // Firework
        if (held.contains(DataComponentTypes.FIREWORKS)) {
            FireworksComponent fw = held.get(DataComponentTypes.FIREWORKS);
            if (fw != null) {
                nbtLines.add("§fFirework:");
                nbtLines.add("  §7Flight: " + fw.flightDuration());
                nbtLines.add("  §7Explosions: " + fw.explosions().size());
            }
        }
        
        // Written Book
        if (held.contains(DataComponentTypes.WRITTEN_BOOK_CONTENT)) {
            WrittenBookContentComponent book = held.get(DataComponentTypes.WRITTEN_BOOK_CONTENT);
            if (book != null) {
                nbtLines.add("§fWritten Book:");
                nbtLines.add("  §7Title: " + book.title().raw());
                nbtLines.add("  §7Author: " + book.author());
                nbtLines.add("  §7Pages: " + book.pages().size());
                nbtLines.add("  §7Generation: " + book.generation());
            }
        }
        
        // Show all component types present
        if (showDetailed) {
            nbtLines.add("");
            nbtLines.add("§e§l--- All Components ---");
            for (var type : components.getTypes()) {
                Object value = components.get(type);
                String typeName = type.toString();
                if (typeName.length() > 50) typeName = typeName.substring(0, 47) + "...";
                nbtLines.add("§8" + typeName);
            }
        }
        
        nbtLines.add("");
        nbtLines.add("§7§oNote: Editing NBT requires creative mode");
        nbtLines.add("§7§oor operator permissions on the server.");
    }
    
    public void render(DrawContext context) {
        if (!isEnabled() || mc.player == null) return;
        
        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();
        
        // Draw on right side of screen
        int boxWidth = 250;
        int x = screenWidth - boxWidth - 10;
        int y = 30;
        int lineHeight = 10;
        int maxLines = (screenHeight - 60) / lineHeight;
        
        // Background
        int boxHeight = Math.min(nbtLines.size(), maxLines) * lineHeight + 10;
        context.fill(x - 5, y - 5, x + boxWidth + 5, y + boxHeight, 0xA0000000);
        
        // Title
        context.drawTextWithShadow(mc.textRenderer, "§6§lNBT Viewer §7(Scroll: Up/Down)", x, y - 15, 0xFFFFFF);
        
        // Lines with scroll
        int startLine = Math.max(0, Math.min(scrollOffset, nbtLines.size() - maxLines));
        int endLine = Math.min(startLine + maxLines, nbtLines.size());
        
        for (int i = startLine; i < endLine; i++) {
            String line = nbtLines.get(i);
            // Truncate long lines
            if (mc.textRenderer.getWidth(line.replaceAll("§.", "")) > boxWidth) {
                while (mc.textRenderer.getWidth(line.replaceAll("§.", "") + "...") > boxWidth && line.length() > 10) {
                    line = line.substring(0, line.length() - 1);
                }
                line = line + "...";
            }
            context.drawTextWithShadow(mc.textRenderer, line, x, y + (i - startLine) * lineHeight, 0xFFFFFF);
        }
        
        // Scroll indicator
        if (nbtLines.size() > maxLines) {
            context.drawTextWithShadow(mc.textRenderer, 
                "§7[" + (startLine + 1) + "-" + endLine + "/" + nbtLines.size() + "]", 
                x, y + boxHeight - 5, 0xAAAAAA);
        }
    }
    
    public void scroll(int amount) {
        scrollOffset += amount;
        if (scrollOffset < 0) scrollOffset = 0;
    }
    
    public void toggleDetailed() {
        showDetailed = !showDetailed;
        if (mc.player != null) {
            mc.player.sendMessage(Text.of("§e[NBTViewer] Detailed mode: " + (showDetailed ? "§aON" : "§cOFF")), false);
        }
    }
    
    public boolean isShowingDetailed() {
        return showDetailed;
    }
}
