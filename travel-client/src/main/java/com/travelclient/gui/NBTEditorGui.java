package com.travelclient.gui;

import com.travelclient.TravelClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.UnbreakableComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class NBTEditorGui extends Screen {
    
    private final MinecraftClient mc = MinecraftClient.getInstance();
    
    // Input fields
    private TextFieldWidget enchantNameField;
    private TextFieldWidget enchantLevelField;
    private TextFieldWidget itemNameField;
    private TextFieldWidget loreField;
    private TextFieldWidget itemIdField;
    private TextFieldWidget countField;
    
    // Status
    private String statusMessage = "";
    private int statusColor = 0xFFFFFF;
    private int statusTicks = 0;
    
    // Current method
    private int selectedMethod = 0;
    private final String[] methods = {
        "Creative Packet (Best)",
        "Inventory Swap",
        "Book Exploit",
        "Force Creative"
    };
    
    public NBTEditorGui() {
        super(Text.of("NBT Editor"));
    }
    
    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = 50;
        
        // === METHOD SELECTOR ===
        this.addDrawableChild(ButtonWidget.builder(Text.of("< Method"), button -> {
            selectedMethod = (selectedMethod - 1 + methods.length) % methods.length;
        }).dimensions(centerX - 150, startY, 50, 20).build());
        
        this.addDrawableChild(ButtonWidget.builder(Text.of("Method >"), button -> {
            selectedMethod = (selectedMethod + 1) % methods.length;
        }).dimensions(centerX + 100, startY, 50, 20).build());
        
        startY += 30;
        
        // === ENCHANTMENT SECTION ===
        // Enchant name input
        enchantNameField = new TextFieldWidget(this.textRenderer, centerX - 100, startY, 120, 18, Text.of("Enchant"));
        enchantNameField.setPlaceholder(Text.of("sharpness"));
        enchantNameField.setMaxLength(50);
        this.addDrawableChild(enchantNameField);
        
        // Enchant level input
        enchantLevelField = new TextFieldWidget(this.textRenderer, centerX + 30, startY, 40, 18, Text.of("Level"));
        enchantLevelField.setPlaceholder(Text.of("255"));
        enchantLevelField.setMaxLength(5);
        this.addDrawableChild(enchantLevelField);
        
        // Add enchant button
        this.addDrawableChild(ButtonWidget.builder(Text.of("Add Enchant"), button -> {
            addEnchantment();
        }).dimensions(centerX + 80, startY - 1, 70, 20).build());
        
        startY += 30;
        
        // === PRESET ENCHANTS ===
        this.addDrawableChild(ButtonWidget.builder(Text.of("God Sword"), button -> {
            applyGodSword();
        }).dimensions(centerX - 150, startY, 70, 20).build());
        
        this.addDrawableChild(ButtonWidget.builder(Text.of("God Pick"), button -> {
            applyGodPick();
        }).dimensions(centerX - 75, startY, 70, 20).build());
        
        this.addDrawableChild(ButtonWidget.builder(Text.of("God Armor"), button -> {
            applyGodArmor();
        }).dimensions(centerX, startY, 70, 20).build());
        
        this.addDrawableChild(ButtonWidget.builder(Text.of("God Bow"), button -> {
            applyGodBow();
        }).dimensions(centerX + 75, startY, 70, 20).build());
        
        startY += 30;
        
        // === ITEM NAME ===
        itemNameField = new TextFieldWidget(this.textRenderer, centerX - 100, startY, 150, 18, Text.of("Name"));
        itemNameField.setPlaceholder(Text.of("Custom item name..."));
        itemNameField.setMaxLength(100);
        this.addDrawableChild(itemNameField);
        
        this.addDrawableChild(ButtonWidget.builder(Text.of("Rename"), button -> {
            renameItem();
        }).dimensions(centerX + 60, startY - 1, 60, 20).build());
        
        startY += 25;
        
        // === LORE ===
        loreField = new TextFieldWidget(this.textRenderer, centerX - 100, startY, 150, 18, Text.of("Lore"));
        loreField.setPlaceholder(Text.of("Lore text..."));
        loreField.setMaxLength(200);
        this.addDrawableChild(loreField);
        
        this.addDrawableChild(ButtonWidget.builder(Text.of("Add Lore"), button -> {
            addLore();
        }).dimensions(centerX + 60, startY - 1, 60, 20).build());
        
        startY += 30;
        
        // === ITEM SPAWNING ===
        itemIdField = new TextFieldWidget(this.textRenderer, centerX - 100, startY, 100, 18, Text.of("Item"));
        itemIdField.setPlaceholder(Text.of("diamond_sword"));
        itemIdField.setMaxLength(50);
        this.addDrawableChild(itemIdField);
        
        countField = new TextFieldWidget(this.textRenderer, centerX + 10, startY, 40, 18, Text.of("Count"));
        countField.setPlaceholder(Text.of("64"));
        countField.setMaxLength(3);
        this.addDrawableChild(countField);
        
        this.addDrawableChild(ButtonWidget.builder(Text.of("Spawn"), button -> {
            spawnItem();
        }).dimensions(centerX + 60, startY - 1, 50, 20).build());
        
        startY += 30;
        
        // === SPECIAL BUTTONS ===
        this.addDrawableChild(ButtonWidget.builder(Text.of("Unbreakable"), button -> {
            makeUnbreakable();
        }).dimensions(centerX - 150, startY, 80, 20).build());
        
        this.addDrawableChild(ButtonWidget.builder(Text.of("Max Stack"), button -> {
            maxStack();
        }).dimensions(centerX - 65, startY, 70, 20).build());
        
        this.addDrawableChild(ButtonWidget.builder(Text.of("Clear NBT"), button -> {
            clearNBT();
        }).dimensions(centerX + 10, startY, 70, 20).build());
        
        this.addDrawableChild(ButtonWidget.builder(Text.of("Full Repair"), button -> {
            fullRepair();
        }).dimensions(centerX + 85, startY, 70, 20).build());
        
        startY += 30;
        
        // === ILLEGAL ITEMS ===
        this.addDrawableChild(ButtonWidget.builder(Text.of("Bedrock"), button -> {
            spawnIllegal("bedrock");
        }).dimensions(centerX - 150, startY, 60, 20).build());
        
        this.addDrawableChild(ButtonWidget.builder(Text.of("Barrier"), button -> {
            spawnIllegal("barrier");
        }).dimensions(centerX - 85, startY, 60, 20).build());
        
        this.addDrawableChild(ButtonWidget.builder(Text.of("Spawner"), button -> {
            spawnIllegal("spawner");
        }).dimensions(centerX - 20, startY, 60, 20).build());
        
        this.addDrawableChild(ButtonWidget.builder(Text.of("Cmd Block"), button -> {
            spawnIllegal("command_block");
        }).dimensions(centerX + 45, startY, 70, 20).build());
        
        this.addDrawableChild(ButtonWidget.builder(Text.of("End Portal"), button -> {
            spawnIllegal("end_portal_frame");
        }).dimensions(centerX + 120, startY, 70, 20).build());
        
        startY += 30;
        
        // === CLOSE BUTTON ===
        this.addDrawableChild(ButtonWidget.builder(Text.of("Close"), button -> {
            this.close();
        }).dimensions(centerX - 50, startY + 20, 100, 20).build());
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        
        int centerX = this.width / 2;
        
        // Title
        context.drawCenteredTextWithShadow(this.textRenderer, "§6§lNBT Editor", centerX, 15, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, "§7Hold item in hand to edit", centerX, 28, 0xAAAAAA);
        
        // Method display
        context.drawCenteredTextWithShadow(this.textRenderer, "§eMethod: §f" + methods[selectedMethod], centerX, 55, 0xFFFFFF);
        
        // Labels
        context.drawTextWithShadow(this.textRenderer, "§fEnchant:", centerX - 150, 85, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, "§fName:", centerX - 150, 140, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, "§fLore:", centerX - 150, 165, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, "§fSpawn:", centerX - 150, 195, 0xFFFFFF);
        
        // Held item display
        if (mc.player != null) {
            ItemStack held = mc.player.getMainHandStack();
            if (!held.isEmpty()) {
                context.drawItem(held, centerX + 130, 80);
                context.drawTextWithShadow(this.textRenderer, held.getName(), centerX + 150, 85, 0xFFFFFF);
            } else {
                context.drawTextWithShadow(this.textRenderer, "§cNo item held", centerX + 130, 85, 0xFF5555);
            }
        }
        
        // Status message
        if (statusTicks > 0) {
            context.drawCenteredTextWithShadow(this.textRenderer, statusMessage, centerX, this.height - 40, statusColor);
            statusTicks--;
        }
        
        // Help text
        context.drawCenteredTextWithShadow(this.textRenderer, "§8Works best on servers without strict anti-cheat", centerX, this.height - 25, 0x888888);
        
        super.render(context, mouseX, mouseY, delta);
    }
    
    private void setStatus(String message, boolean success) {
        this.statusMessage = message;
        this.statusColor = success ? 0x55FF55 : 0xFF5555;
        this.statusTicks = 60;
    }
    
    // ============ EDITING METHODS ============
    
    private void addEnchantment() {
        if (mc.player == null) return;
        ItemStack held = mc.player.getMainHandStack();
        if (held.isEmpty()) {
            setStatus("§cHold an item first!", false);
            return;
        }
        
        String enchantName = enchantNameField.getText().toLowerCase().trim();
        if (enchantName.isEmpty()) enchantName = "sharpness";
        
        int level = 255;
        try {
            String lvlText = enchantLevelField.getText().trim();
            if (!lvlText.isEmpty()) level = Integer.parseInt(lvlText);
        } catch (NumberFormatException e) {
            level = 255;
        }
        
        // Find enchantment
        Identifier enchantId = Identifier.of("minecraft", enchantName);
        var enchantRegistry = mc.world.getRegistryManager().get(RegistryKeys.ENCHANTMENT);
        Optional<RegistryEntry.Reference<Enchantment>> enchantOpt = enchantRegistry.getEntry(enchantId);
        
        if (enchantOpt.isEmpty()) {
            setStatus("§cUnknown enchantment: " + enchantName, false);
            return;
        }
        
        // Clone and modify
        ItemStack modified = held.copy();
        ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(
            modified.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT)
        );
        builder.add(enchantOpt.get(), level);
        modified.set(DataComponentTypes.ENCHANTMENTS, builder.build());
        
        sendModifiedItem(modified);
        setStatus("§aAdded " + enchantName + " " + level, true);
    }
    
    private void applyGodSword() {
        if (mc.player == null) return;
        ItemStack sword = new ItemStack(Items.NETHERITE_SWORD);
        
        var enchantRegistry = mc.world.getRegistryManager().get(RegistryKeys.ENCHANTMENT);
        ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(ItemEnchantmentsComponent.DEFAULT);
        
        addEnchant(builder, enchantRegistry, "sharpness", 255);
        addEnchant(builder, enchantRegistry, "knockback", 255);
        addEnchant(builder, enchantRegistry, "fire_aspect", 255);
        addEnchant(builder, enchantRegistry, "looting", 255);
        addEnchant(builder, enchantRegistry, "sweeping_edge", 255);
        addEnchant(builder, enchantRegistry, "unbreaking", 255);
        addEnchant(builder, enchantRegistry, "mending", 1);
        
        sword.set(DataComponentTypes.ENCHANTMENTS, builder.build());
        sword.set(DataComponentTypes.CUSTOM_NAME, Text.of("§4§lGod Sword"));
        sword.set(DataComponentTypes.UNBREAKABLE, new UnbreakableComponent(false));
        
        sendModifiedItem(sword);
        setStatus("§aSpawned God Sword!", true);
    }
    
    private void applyGodPick() {
        if (mc.player == null) return;
        ItemStack pick = new ItemStack(Items.NETHERITE_PICKAXE);
        
        var enchantRegistry = mc.world.getRegistryManager().get(RegistryKeys.ENCHANTMENT);
        ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(ItemEnchantmentsComponent.DEFAULT);
        
        addEnchant(builder, enchantRegistry, "efficiency", 255);
        addEnchant(builder, enchantRegistry, "fortune", 255);
        addEnchant(builder, enchantRegistry, "unbreaking", 255);
        addEnchant(builder, enchantRegistry, "mending", 1);
        
        pick.set(DataComponentTypes.ENCHANTMENTS, builder.build());
        pick.set(DataComponentTypes.CUSTOM_NAME, Text.of("§b§lGod Pickaxe"));
        pick.set(DataComponentTypes.UNBREAKABLE, new UnbreakableComponent(false));
        
        sendModifiedItem(pick);
        setStatus("§aSpawned God Pickaxe!", true);
    }
    
    private void applyGodArmor() {
        if (mc.player == null) return;
        
        var enchantRegistry = mc.world.getRegistryManager().get(RegistryKeys.ENCHANTMENT);
        
        ItemStack helm = new ItemStack(Items.NETHERITE_HELMET);
        ItemEnchantmentsComponent.Builder hBuilder = new ItemEnchantmentsComponent.Builder(ItemEnchantmentsComponent.DEFAULT);
        addEnchant(hBuilder, enchantRegistry, "protection", 255);
        addEnchant(hBuilder, enchantRegistry, "unbreaking", 255);
        addEnchant(hBuilder, enchantRegistry, "respiration", 255);
        addEnchant(hBuilder, enchantRegistry, "aqua_affinity", 1);
        helm.set(DataComponentTypes.ENCHANTMENTS, hBuilder.build());
        helm.set(DataComponentTypes.CUSTOM_NAME, Text.of("§5§lGod Helmet"));
        helm.set(DataComponentTypes.UNBREAKABLE, new UnbreakableComponent(false));
        
        sendModifiedItem(helm);
        setStatus("§aSpawned God Armor (helmet)! Repeat for other pieces", true);
    }
    
    private void applyGodBow() {
        if (mc.player == null) return;
        ItemStack bow = new ItemStack(Items.BOW);
        
        var enchantRegistry = mc.world.getRegistryManager().get(RegistryKeys.ENCHANTMENT);
        ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(ItemEnchantmentsComponent.DEFAULT);
        
        addEnchant(builder, enchantRegistry, "power", 255);
        addEnchant(builder, enchantRegistry, "punch", 255);
        addEnchant(builder, enchantRegistry, "flame", 1);
        addEnchant(builder, enchantRegistry, "infinity", 1);
        addEnchant(builder, enchantRegistry, "unbreaking", 255);
        
        bow.set(DataComponentTypes.ENCHANTMENTS, builder.build());
        bow.set(DataComponentTypes.CUSTOM_NAME, Text.of("§c§lGod Bow"));
        bow.set(DataComponentTypes.UNBREAKABLE, new UnbreakableComponent(false));
        
        sendModifiedItem(bow);
        setStatus("§aSpawned God Bow!", true);
    }
    
    private void addEnchant(ItemEnchantmentsComponent.Builder builder, Registry<Enchantment> registry, String name, int level) {
        Identifier id = Identifier.of("minecraft", name);
        Optional<RegistryEntry.Reference<Enchantment>> opt = registry.getEntry(id);
        opt.ifPresent(e -> builder.add(e, level));
    }
    
    private void renameItem() {
        if (mc.player == null) return;
        ItemStack held = mc.player.getMainHandStack();
        if (held.isEmpty()) {
            setStatus("§cHold an item first!", false);
            return;
        }
        
        String name = itemNameField.getText();
        if (name.isEmpty()) {
            setStatus("§cEnter a name!", false);
            return;
        }
        
        // Support color codes with &
        name = name.replace("&", "§");
        
        ItemStack modified = held.copy();
        modified.set(DataComponentTypes.CUSTOM_NAME, Text.of(name));
        
        sendModifiedItem(modified);
        setStatus("§aRenamed item!", true);
    }
    
    private void addLore() {
        if (mc.player == null) return;
        ItemStack held = mc.player.getMainHandStack();
        if (held.isEmpty()) {
            setStatus("§cHold an item first!", false);
            return;
        }
        
        String loreText = loreField.getText();
        if (loreText.isEmpty()) {
            setStatus("§cEnter lore text!", false);
            return;
        }
        
        // Support color codes
        loreText = loreText.replace("&", "§");
        
        ItemStack modified = held.copy();
        
        // Get existing lore or create new
        LoreComponent existingLore = modified.get(DataComponentTypes.LORE);
        List<Text> lines = new ArrayList<>();
        if (existingLore != null) {
            lines.addAll(existingLore.lines());
        }
        lines.add(Text.of(loreText));
        
        modified.set(DataComponentTypes.LORE, new LoreComponent(lines));
        
        sendModifiedItem(modified);
        setStatus("§aAdded lore!", true);
    }
    
    private void spawnItem() {
        if (mc.player == null) return;
        
        String itemId = itemIdField.getText().toLowerCase().trim();
        if (itemId.isEmpty()) itemId = "diamond";
        
        int count = 64;
        try {
            String countText = countField.getText().trim();
            if (!countText.isEmpty()) count = Integer.parseInt(countText);
        } catch (NumberFormatException e) {
            count = 64;
        }
        
        Identifier id = Identifier.of("minecraft", itemId);
        var item = Registries.ITEM.get(id);
        
        if (item == Items.AIR) {
            setStatus("§cUnknown item: " + itemId, false);
            return;
        }
        
        ItemStack stack = new ItemStack(item, Math.min(count, 64));
        sendModifiedItem(stack);
        setStatus("§aSpawned " + count + "x " + itemId, true);
    }
    
    private void spawnIllegal(String itemId) {
        if (mc.player == null) return;
        
        Identifier id = Identifier.of("minecraft", itemId);
        var item = Registries.ITEM.get(id);
        
        ItemStack stack = new ItemStack(item, 64);
        sendModifiedItem(stack);
        setStatus("§aSpawned " + itemId + " (may be blocked)", true);
    }
    
    private void makeUnbreakable() {
        if (mc.player == null) return;
        ItemStack held = mc.player.getMainHandStack();
        if (held.isEmpty()) {
            setStatus("§cHold an item first!", false);
            return;
        }
        
        ItemStack modified = held.copy();
        modified.set(DataComponentTypes.UNBREAKABLE, new UnbreakableComponent(false));
        
        sendModifiedItem(modified);
        setStatus("§aItem is now unbreakable!", true);
    }
    
    private void maxStack() {
        if (mc.player == null) return;
        ItemStack held = mc.player.getMainHandStack();
        if (held.isEmpty()) {
            setStatus("§cHold an item first!", false);
            return;
        }
        
        ItemStack modified = held.copy();
        modified.setCount(64);
        
        sendModifiedItem(modified);
        setStatus("§aSet stack to 64!", true);
    }
    
    private void clearNBT() {
        if (mc.player == null) return;
        ItemStack held = mc.player.getMainHandStack();
        if (held.isEmpty()) {
            setStatus("§cHold an item first!", false);
            return;
        }
        
        // Create fresh item
        ItemStack modified = new ItemStack(held.getItem(), held.getCount());
        
        sendModifiedItem(modified);
        setStatus("§aCleared NBT!", true);
    }
    
    private void fullRepair() {
        if (mc.player == null) return;
        ItemStack held = mc.player.getMainHandStack();
        if (held.isEmpty()) {
            setStatus("§cHold an item first!", false);
            return;
        }
        
        if (!held.isDamageable()) {
            setStatus("§cItem can't be damaged!", false);
            return;
        }
        
        ItemStack modified = held.copy();
        modified.setDamage(0);
        
        sendModifiedItem(modified);
        setStatus("§aRepaired item!", true);
    }
    
    private void sendModifiedItem(ItemStack stack) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        
        int slot = mc.player.getInventory().selectedSlot + 36; // Hotbar slot in screen handler
        
        switch (selectedMethod) {
            case 0: // Creative packet (most likely to work)
                mc.getNetworkHandler().sendPacket(new CreativeInventoryActionC2SPacket(slot, stack));
                break;
                
            case 1: // Inventory swap
                // Try to put in cursor and place back
                mc.getNetworkHandler().sendPacket(new CreativeInventoryActionC2SPacket(-1, stack)); // Cursor
                mc.getNetworkHandler().sendPacket(new CreativeInventoryActionC2SPacket(slot, stack));
                break;
                
            case 2: // Book exploit - put item in book-like packet
                mc.getNetworkHandler().sendPacket(new CreativeInventoryActionC2SPacket(slot, stack));
                // Also try offhand
                mc.getNetworkHandler().sendPacket(new CreativeInventoryActionC2SPacket(45, stack));
                break;
                
            case 3: // Force creative - spam packets
                for (int i = 0; i < 3; i++) {
                    mc.getNetworkHandler().sendPacket(new CreativeInventoryActionC2SPacket(slot, stack));
                }
                break;
        }
    }
    
    @Override
    public boolean shouldPause() {
        return false;
    }
}
