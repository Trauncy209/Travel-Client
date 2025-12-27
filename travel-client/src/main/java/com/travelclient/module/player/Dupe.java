package com.travelclient.module.player;

import com.travelclient.module.Module;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.UnbreakableComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.DonkeyEntity;
import net.minecraft.entity.passive.LlamaEntity;
import net.minecraft.entity.vehicle.ChestBoatEntity;
import net.minecraft.entity.vehicle.ChestMinecartEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

public class Dupe extends Module {
    
    private int dupeMode = 0; // 0=auto, 1=packet, 2=desync, 3=container, 4=book
    private int cooldown = 0;
    private boolean attemptingDupe = false;
    private ItemStack toDupe = null;
    
    public Dupe() {
        super("Dupe", "Attempts various dupe methods", Category.PLAYER);
    }
    
    @Override
    public void onEnable() {
        if (mc.player == null) return;
        
        mc.player.sendMessage(Text.of("§a[Dupe] §fExploit Module Active"), false);
        mc.player.sendMessage(Text.of("§e  #dupe §7- Try to dupe held item"), false);
        mc.player.sendMessage(Text.of("§e  #dupe packet §7- Packet spam method"), false);
        mc.player.sendMessage(Text.of("§e  #dupe desync §7- Inventory desync method"), false);
        mc.player.sendMessage(Text.of("§e  #dupe container §7- Container dupe (need chest)"), false);
        mc.player.sendMessage(Text.of("§e  #dupe drop §7- Drop desync method"), false);
        mc.player.sendMessage(Text.of("§e  #give <item> [count] §7- Creative packet attempt"), false);
        mc.player.sendMessage(Text.of("§c[!] §7Most methods patched on vanilla - works on some plugins"), false);
    }
    
    @Override
    public void onTick() {
        if (cooldown > 0) cooldown--;
    }
    
    public void tryDupe(String method) {
        if (mc.player == null) return;
        if (cooldown > 0) {
            mc.player.sendMessage(Text.of("§c[Dupe] Cooldown... wait " + cooldown + " ticks"), false);
            return;
        }
        
        ItemStack held = mc.player.getMainHandStack();
        if (held.isEmpty()) {
            mc.player.sendMessage(Text.of("§c[Dupe] Hold an item to dupe!"), false);
            return;
        }
        
        toDupe = held.copy();
        
        switch (method.toLowerCase()) {
            case "packet":
                packetDupe();
                break;
            case "desync":
                desyncDupe();
                break;
            case "container":
                containerDupe();
                break;
            case "drop":
                dropDupe();
                break;
            case "creative":
                creativeDupe();
                break;
            default:
                // Try all methods
                mc.player.sendMessage(Text.of("§e[Dupe] Trying all methods..."), false);
                packetDupe();
                desyncDupe();
                dropDupe();
                break;
        }
        
        cooldown = 20;
    }
    
    // Method 1: Packet spam - send conflicting inventory packets
    private void packetDupe() {
        if (mc.player == null || mc.interactionManager == null) return;
        
        mc.player.sendMessage(Text.of("§e[Dupe] Trying packet spam..."), false);
        
        int heldSlot = mc.player.getInventory().selectedSlot;
        int emptySlot = findEmptySlot();
        
        if (emptySlot == -1) {
            mc.player.sendMessage(Text.of("§c[Dupe] Need empty slot!"), false);
            return;
        }
        
        // Send rapid conflicting packets
        for (int i = 0; i < 5; i++) {
            // Pick up item
            mc.player.networkHandler.sendPacket(new ClickSlotC2SPacket(
                0, 0, heldSlot + 36, 0, SlotActionType.PICKUP, 
                toDupe.copy(), new it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<>()
            ));
            
            // Put down in empty slot
            mc.player.networkHandler.sendPacket(new ClickSlotC2SPacket(
                0, 0, emptySlot + (emptySlot < 9 ? 36 : 0), 0, SlotActionType.PICKUP,
                ItemStack.EMPTY, new it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<>()
            ));
            
            // Pick up again 
            mc.player.networkHandler.sendPacket(new ClickSlotC2SPacket(
                0, 0, heldSlot + 36, 0, SlotActionType.PICKUP,
                toDupe.copy(), new it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<>()
            ));
        }
        
        mc.player.sendMessage(Text.of("§7[Dupe] Packet spam sent - check inventory"), false);
    }
    
    // Method 2: Inventory desync - confuse server about inventory state
    private void desyncDupe() {
        if (mc.player == null) return;
        
        mc.player.sendMessage(Text.of("§e[Dupe] Trying desync..."), false);
        
        int slot = mc.player.getInventory().selectedSlot;
        
        // Send position packet while "moving" item
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(
            mc.player.getX(), mc.player.getY(), mc.player.getZ(),
            mc.player.getYaw(), mc.player.getPitch(), mc.player.isOnGround()
        ));
        
        // Quick slot swap packets
        for (int i = 0; i < 3; i++) {
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket((slot + 1) % 9));
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
        }
        
        // Throw and cancel rapidly
        mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(
            PlayerActionC2SPacket.Action.DROP_ITEM, BlockPos.ORIGIN, net.minecraft.util.math.Direction.DOWN
        ));
        
        // Swap to offhand and back
        mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(
            PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, net.minecraft.util.math.Direction.DOWN
        ));
        mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(
            PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, net.minecraft.util.math.Direction.DOWN
        ));
        
        mc.player.sendMessage(Text.of("§7[Dupe] Desync attempted - check for duped items"), false);
    }
    
    // Method 3: Container dupe - exploit chest/container interactions
    private void containerDupe() {
        if (mc.player == null) return;
        
        mc.player.sendMessage(Text.of("§e[Dupe] Container method requires:"), false);
        mc.player.sendMessage(Text.of("§71. Open a chest/container"), false);
        mc.player.sendMessage(Text.of("§72. Put item in container"), false);
        mc.player.sendMessage(Text.of("§73. Spam shift-click while closing"), false);
        mc.player.sendMessage(Text.of("§7Or find a donkey/llama and kill it while in inventory"), false);
        
        // Check if in container
        if (mc.player.currentScreenHandler != mc.player.playerScreenHandler) {
            // In a container - try rapid transfer
            int slot = mc.player.getInventory().selectedSlot;
            
            for (int i = 0; i < 10; i++) {
                mc.player.networkHandler.sendPacket(new ClickSlotC2SPacket(
                    mc.player.currentScreenHandler.syncId, 0,
                    slot + 36, 0, SlotActionType.QUICK_MOVE,
                    toDupe.copy(), new it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<>()
                ));
            }
            
            // Close and reopen rapidly
            mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(
                mc.player.currentScreenHandler.syncId
            ));
            
            mc.player.sendMessage(Text.of("§7[Dupe] Container exploit attempted"), false);
        }
    }
    
    // Method 4: Drop desync - drop item and cause desync
    private void dropDupe() {
        if (mc.player == null) return;
        
        mc.player.sendMessage(Text.of("§e[Dupe] Trying drop desync..."), false);
        
        // Drop item
        mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(
            PlayerActionC2SPacket.Action.DROP_ALL_ITEMS, BlockPos.ORIGIN, net.minecraft.util.math.Direction.DOWN
        ));
        
        // Immediately send position update
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
            mc.player.getX(), mc.player.getY() + 0.1, mc.player.getZ(), false
        ));
        
        // Send conflicting held item packet
        mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(
            mc.player.getInventory().selectedSlot
        ));
        
        // Interact packet
        mc.player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(
            Hand.MAIN_HAND, 0, mc.player.getYaw(), mc.player.getPitch()
        ));
        
        mc.player.sendMessage(Text.of("§7[Dupe] Drop desync sent - pick up dropped items quickly!"), false);
    }
    
    // Method 5: Creative packet in survival (rarely works)
    private void creativeDupe() {
        if (mc.player == null) return;
        
        mc.player.sendMessage(Text.of("§e[Dupe] Sending creative packets..."), false);
        
        int emptySlot = findEmptySlot();
        if (emptySlot == -1) {
            mc.player.sendMessage(Text.of("§c[Dupe] Need empty slot!"), false);
            return;
        }
        
        ItemStack stack = toDupe.copy();
        stack.setCount(stack.getMaxCount());
        
        int networkSlot = emptySlot < 9 ? 36 + emptySlot : emptySlot;
        
        // Send creative inventory packet
        mc.player.networkHandler.sendPacket(new CreativeInventoryActionC2SPacket(networkSlot, stack));
        
        mc.player.sendMessage(Text.of("§7[Dupe] Creative packet sent (usually rejected)"), false);
    }
    
    // Give item using creative packets
    public void giveItem(String itemName, int count) {
        if (mc.player == null) return;
        
        Item item = findItem(itemName);
        if (item == null || item == Items.AIR) {
            mc.player.sendMessage(Text.of("§c[Dupe] Item not found: " + itemName), false);
            return;
        }
        
        int emptySlot = findEmptySlot();
        if (emptySlot == -1) {
            mc.player.sendMessage(Text.of("§c[Dupe] No empty slot!"), false);
            return;
        }
        
        ItemStack stack = new ItemStack(item, Math.min(count, item.getMaxCount()));
        int networkSlot = emptySlot < 9 ? 36 + emptySlot : emptySlot;
        
        // Try creative packet
        mc.player.networkHandler.sendPacket(new CreativeInventoryActionC2SPacket(networkSlot, stack));
        
        // Also try click packet
        mc.player.networkHandler.sendPacket(new ClickSlotC2SPacket(
            0, 0, networkSlot, 0, SlotActionType.PICKUP,
            stack, new it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<>()
        ));
        
        String name = Registries.ITEM.getId(item).getPath();
        mc.player.sendMessage(Text.of("§e[Dupe] Attempted to give " + count + "x " + name), false);
    }
    
    // Spawn with enchants
    public void spawnWithEnchants(String itemName, String enchantString) {
        if (mc.player == null || mc.world == null) return;
        
        Item item = findItem(itemName);
        if (item == null || item == Items.AIR) {
            mc.player.sendMessage(Text.of("§c[Dupe] Item not found: " + itemName), false);
            return;
        }
        
        int emptySlot = findEmptySlot();
        if (emptySlot == -1) {
            mc.player.sendMessage(Text.of("§c[Dupe] No empty slot!"), false);
            return;
        }
        
        ItemStack stack = new ItemStack(item, 1);
        
        // Apply enchantments
        if (enchantString != null && !enchantString.isEmpty()) {
            Registry<Enchantment> registry = mc.world.getRegistryManager().get(RegistryKeys.ENCHANTMENT);
            ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(stack.getEnchantments());
            
            for (String part : enchantString.split(",")) {
                String[] kv = part.trim().split(":");
                if (kv.length != 2) continue;
                
                String name = kv[0].toLowerCase().trim();
                int level;
                try { level = Integer.parseInt(kv[1].trim()); } 
                catch (NumberFormatException e) { continue; }
                
                if (name.equals("unbreakable")) {
                    stack.set(DataComponentTypes.UNBREAKABLE, new UnbreakableComponent(true));
                    continue;
                }
                
                Identifier id = Identifier.tryParse("minecraft:" + name);
                if (id == null) continue;
                
                Optional<RegistryEntry.Reference<Enchantment>> opt = registry.getEntry(
                    RegistryKey.of(RegistryKeys.ENCHANTMENT, id)
                );
                if (opt.isPresent()) builder.add(opt.get(), level);
            }
            stack.set(DataComponentTypes.ENCHANTMENTS, builder.build());
        }
        
        int networkSlot = emptySlot < 9 ? 36 + emptySlot : emptySlot;
        mc.player.networkHandler.sendPacket(new CreativeInventoryActionC2SPacket(networkSlot, stack));
        
        mc.player.sendMessage(Text.of("§e[Dupe] Attempted spawn with enchants"), false);
    }
    
    public void showNBT() {
        if (mc.player == null) return;
        
        ItemStack held = mc.player.getMainHandStack();
        if (held.isEmpty()) {
            mc.player.sendMessage(Text.of("§c[NBT] Hold an item!"), false);
            return;
        }
        
        mc.player.sendMessage(Text.of("§6=== Item Info ==="), false);
        mc.player.sendMessage(Text.of("§eName: §f" + held.getName().getString()), false);
        mc.player.sendMessage(Text.of("§eID: §f" + Registries.ITEM.getId(held.getItem())), false);
        mc.player.sendMessage(Text.of("§eCount: §f" + held.getCount() + "/" + held.getMaxCount()), false);
        
        if (held.isDamageable()) {
            mc.player.sendMessage(Text.of("§eDurability: §f" + (held.getMaxDamage() - held.getDamage()) + "/" + held.getMaxDamage()), false);
        }
        
        ItemEnchantmentsComponent enchants = held.get(DataComponentTypes.ENCHANTMENTS);
        if (enchants != null && !enchants.isEmpty()) {
            mc.player.sendMessage(Text.of("§eEnchantments:"), false);
            enchants.getEnchantmentEntries().forEach(e -> {
                mc.player.sendMessage(Text.of("§7  - " + e.getKey().getIdAsString().replace("minecraft:", "") + ": " + e.getIntValue()), false);
            });
        }
    }
    
    public void enchantHeldItem(String enchantName, int level) {
        // This only works client-side, server will reject
        mc.player.sendMessage(Text.of("§c[Dupe] Enchant requires creative/singleplayer"), false);
    }
    
    public void repairHeldItem() {
        mc.player.sendMessage(Text.of("§c[Dupe] Repair requires creative/singleplayer"), false);
    }
    
    private Item findItem(String name) {
        String search = name.toLowerCase().replace(" ", "_");
        Identifier id = Identifier.tryParse("minecraft:" + search);
        if (id != null) {
            Item item = Registries.ITEM.get(id);
            if (item != Items.AIR) return item;
        }
        for (Item item : Registries.ITEM) {
            if (Registries.ITEM.getId(item).getPath().contains(search)) return item;
        }
        return null;
    }
    
    private int findEmptySlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isEmpty()) return i;
        }
        for (int i = 9; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).isEmpty()) return i;
        }
        return -1;
    }
}
