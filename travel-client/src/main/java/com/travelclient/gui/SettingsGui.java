package com.travelclient.gui;

import com.travelclient.TravelClient;
import com.travelclient.module.Module;
import com.travelclient.module.combat.*;
import com.travelclient.module.movement.*;
import com.travelclient.module.player.*;
import com.travelclient.module.render.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class SettingsGui extends Screen {
    
    private final Module module;
    private final Screen parent;
    private boolean waitingForKey = false;
    
    // Search for BlockESP
    private TextFieldWidget searchBox;
    private boolean searchMode = false;
    
    public SettingsGui(Module module, Screen parent) {
        super(Text.literal(module.getName() + " Settings"));
        this.module = module;
        this.parent = parent;
    }
    
    @Override
    protected void init() {
        searchBox = new TextFieldWidget(textRenderer, width/2 - 60, height/2 + 20, 120, 16, Text.literal(""));
        searchBox.setMaxLength(50);
    }
    
    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        // Full dark background
        ctx.fill(0, 0, width, height, 0xFF101010);
        
        // Title bar
        ctx.fill(0, 0, width, 24, 0xFF1a1a2e);
        ctx.drawCenteredTextWithShadow(textRenderer, module.getName() + " Settings", width/2, 8, 0xFFe94560);
        
        // Back hint
        ctx.drawTextWithShadow(textRenderer, "< ESC to go back", 5, 8, 0xFF666666);
        
        // Settings panel
        int pw = 200;
        int ph = 200;
        int px = width/2 - pw/2;
        int py = 40;
        
        ctx.fill(px, py, px + pw, py + ph, 0xFF1a1a1a);
        ctx.fill(px, py, px + pw, py + 1, 0xFF333333);
        ctx.fill(px, py + ph - 1, px + pw, py + ph, 0xFF333333);
        ctx.fill(px, py, px + 1, py + ph, 0xFF333333);
        ctx.fill(px + pw - 1, py, px + pw, py + ph, 0xFF333333);
        
        int sy = py + 10;
        
        // Module enabled status
        String status = module.isEnabled() ? "§aENABLED" : "§cDISABLED";
        ctx.drawTextWithShadow(textRenderer, "Status: " + status, px + 10, sy, 0xFFFFFFFF);
        sy += 18;
        
        // Keybind
        String keyText;
        if (waitingForKey) {
            keyText = "§e[Press any key...]";
        } else if (module.getKeybind() > 0) {
            keyText = "§a[" + getKeyName(module.getKeybind()) + "]";
        } else {
            keyText = "§7[None]";
        }
        ctx.drawTextWithShadow(textRenderer, "Keybind: " + keyText, px + 10, sy, 0xFFFFFFFF);
        ctx.drawTextWithShadow(textRenderer, "§8(click to set, right-click clear)", px + 10, sy + 10, 0xFFFFFFFF);
        sy += 26;
        
        // Separator
        ctx.fill(px + 10, sy, px + pw - 10, sy + 1, 0xFF333333);
        sy += 8;
        
        // Module-specific settings
        List<Setting> settings = getSettings();
        for (Setting s : settings) {
            boolean hover = mx >= px + 10 && mx <= px + pw - 10 && my >= sy && my <= sy + 14;
            
            if (hover) {
                ctx.fill(px + 5, sy - 2, px + pw - 5, sy + 14, 0xFF2a2a2a);
            }
            
            if (s.type == 0) { // Boolean
                String val = s.getBool() ? "§aON" : "§cOFF";
                ctx.drawTextWithShadow(textRenderer, s.name + ": " + val, px + 10, sy + 2, 0xFFCCCCCC);
            } else if (s.type == 1) { // Number  
                String val = String.format("%.1f", s.getNum());
                ctx.drawTextWithShadow(textRenderer, s.name + ": §e" + val, px + 10, sy + 2, 0xFFCCCCCC);
                ctx.drawTextWithShadow(textRenderer, "§a[+]", px + pw - 40, sy + 2, 0xFFFFFFFF);
                ctx.drawTextWithShadow(textRenderer, "§c[-]", px + pw - 22, sy + 2, 0xFFFFFFFF);
            } else if (s.type == 2) { // Search
                ctx.drawTextWithShadow(textRenderer, "§e> Click to Search Blocks", px + 10, sy + 2, 0xFFFFFFFF);
            }
            
            sy += 18;
        }
        
        // Description at bottom
        ctx.drawTextWithShadow(textRenderer, "§7" + module.getDescription(), px + 10, py + ph - 16, 0xFF888888);
        
        // Search mode overlay
        if (searchMode) {
            ctx.fill(0, 0, width, height, 0xCC000000);
            ctx.fill(width/2 - 80, height/2 - 30, width/2 + 80, height/2 + 50, 0xFF1a1a1a);
            ctx.drawCenteredTextWithShadow(textRenderer, "Search Block", width/2, height/2 - 20, 0xFFe94560);
            searchBox.render(ctx, mx, my, 0);
            ctx.drawCenteredTextWithShadow(textRenderer, "§7Press Enter to search", width/2, height/2 + 42, 0xFFFFFFFF);
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int mx = (int) mouseX;
        int my = (int) mouseY;
        
        if (searchMode) {
            searchBox.mouseClicked(mouseX, mouseY, button);
            // Click outside closes search
            if (mx < width/2 - 80 || mx > width/2 + 80 || my < height/2 - 30 || my > height/2 + 50) {
                searchMode = false;
            }
            return true;
        }
        
        int pw = 200;
        int px = width/2 - pw/2;
        int py = 40;
        int sy = py + 10 + 18; // Start at keybind row
        
        // Keybind click
        if (mx >= px + 10 && mx <= px + pw - 10 && my >= sy && my <= sy + 20) {
            if (button == 1) {
                module.setKeybind(0);
                waitingForKey = false;
            } else {
                waitingForKey = true;
            }
            return true;
        }
        sy += 26 + 8; // Skip separator
        
        // Settings clicks
        List<Setting> settings = getSettings();
        for (Setting s : settings) {
            if (mx >= px + 10 && mx <= px + pw - 10 && my >= sy && my <= sy + 14) {
                if (s.type == 0) { // Boolean - toggle
                    s.setBool(!s.getBool());
                } else if (s.type == 1) { // Number
                    if (mx >= px + pw - 45 && mx <= px + pw - 25) {
                        s.setNum(Math.min(s.max, s.getNum() + s.step));
                    } else if (mx >= px + pw - 25) {
                        s.setNum(Math.max(s.min, s.getNum() - s.step));
                    }
                } else if (s.type == 2) { // Search
                    searchMode = true;
                    searchBox.setText("");
                    searchBox.setFocused(true);
                }
                return true;
            }
            sy += 18;
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Waiting for keybind
        if (waitingForKey) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                module.setKeybind(0);
            } else {
                module.setKeybind(keyCode);
            }
            waitingForKey = false;
            return true;
        }
        
        // Search mode
        if (searchMode) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                searchMode = false;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER) {
                String txt = searchBox.getText().trim();
                if (!txt.isEmpty()) {
                    BlockESP esp = TravelClient.moduleManager.getModule(BlockESP.class);
                    if (esp != null) esp.setSearchBlock(txt);
                }
                searchMode = false;
                return true;
            }
            searchBox.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        
        // ESC goes back to main GUI
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            MinecraftClient.getInstance().setScreen(parent);
            return true;
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (searchMode) {
            searchBox.charTyped(chr, modifiers);
            return true;
        }
        return super.charTyped(chr, modifiers);
    }
    
    @Override
    public boolean shouldPause() {
        return false;
    }
    
    private String getKeyName(int k) {
        if (k <= 0) return "None";
        String n = GLFW.glfwGetKeyName(k, 0);
        if (n != null) return n.toUpperCase();
        return switch (k) {
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "LSHIFT";
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RSHIFT";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "LCTRL";
            case GLFW.GLFW_KEY_LEFT_ALT -> "LALT";
            case GLFW.GLFW_KEY_TAB -> "TAB";
            case GLFW.GLFW_KEY_SPACE -> "SPACE";
            case GLFW.GLFW_KEY_CAPS_LOCK -> "CAPS";
            default -> "KEY" + k;
        };
    }
    
    // ===== SETTINGS =====
    
    private List<Setting> getSettings() {
        List<Setting> list = new ArrayList<>();
        
        if (module instanceof KillAura m) {
            list.add(new Setting("Range", m::getRange, m::setRange, 1, 6, 0.5));
            list.add(new Setting("APS", () -> (double)m.getAPS(), v -> m.setAPS(v.intValue()), 1, 20, 1));
            list.add(new Setting("Players", m::getPlayers, m::setPlayers));
            list.add(new Setting("Mobs", m::getMobs, m::setMobs));
            list.add(new Setting("Animals", m::getAnimals, m::setAnimals));
        }
        else if (module instanceof AutoCrystal m) {
            list.add(new Setting("Range", m::getRange, m::setRange, 1, 6, 0.5));
            list.add(new Setting("Players", m::getPlayers, m::setPlayers));
            list.add(new Setting("Mobs", m::getMobs, m::setMobs));
        }
        else if (module instanceof Aimbot m) {
            list.add(new Setting("Range", m::getRange, m::setRange, 1, 6, 0.5));
            list.add(new Setting("Speed", m::getAimSpeed, m::setAimSpeed, 0.1, 1, 0.1));
            list.add(new Setting("Players", m::getPlayers, m::setPlayers));
            list.add(new Setting("Mobs", m::getMobs, m::setMobs));
        }
        else if (module instanceof ElytraFly m) {
            list.add(new Setting("TurnSpeed", m::getTurnSpeed, m::setTurnSpeed, 5, 100, 5));
        }
        else if (module instanceof AutoFirework m) {
            list.add(new Setting("MinSpeed", m::getMinSpeed, m::setMinSpeed, 0.1, 2, 0.1));
        }
        else if (module instanceof FakeElytra m) {
            list.add(new Setting("Speed", m::getSpeed, m::setSpeed, 0.5, 10, 0.5));
            list.add(new Setting("VertSpeed", m::getVertSpeed, m::setVertSpeed, 0.5, 5, 0.5));
        }
        else if (module instanceof Freecam m) {
            list.add(new Setting("Speed", () -> (double)m.getSpeed(), v -> m.setSpeed(v.floatValue()), 0.5, 5, 0.5));
        }
        else if (module instanceof BlockESP m) {
            list.add(new Setting("Search", 2));
            list.add(new Setting("Range", m::getRange, m::setRange, 8, 64, 4));
        }
        else if (module instanceof PlayerESP m) {
            list.add(new Setting("Range", m::getRange, m::setRange, 50, 500, 25));
            list.add(new Setting("Tracers", m::hasTracers, m::setTracers));
            list.add(new Setting("Box", m::hasBox, m::setBox));
        }
        else if (module instanceof ItemESP m) {
            list.add(new Setting("Range", m::getRange, m::setRange, 20, 200, 10));
            list.add(new Setting("Nametags", m::hasNametags, m::setNametags));
        }
        else if (module instanceof ChestESP m) {
            list.add(new Setting("Range", m::getRange, m::setRange, 20, 200, 10));
            list.add(new Setting("Alerts", m::getAlertValuable, m::setAlertValuable));
        }
        else if (module instanceof Spammer m) {
            list.add(new Setting("Delay", () -> (double)m.getDelay(), v -> m.setDelay(v.intValue()), 20, 200, 10));
            list.add(new Setting("RandomSuffix", m::getRandomSuffix, m::setRandomSuffix));
        }
        else if (module instanceof AutoEat m) {
            list.add(new Setting("HealthThr", () -> (double)m.getHealthThreshold(), v -> m.setHealthThreshold(v.floatValue()), 2, 18, 1));
            list.add(new Setting("HungerThr", () -> (double)m.getHungerThreshold(), v -> m.setHungerThreshold(v.floatValue()), 6, 18, 1));
        }
        
        return list;
    }
    
    private static class Setting {
        String name;
        int type; // 0=bool, 1=number, 2=search
        double min, max, step;
        java.util.function.Supplier<Double> numGet;
        java.util.function.Consumer<Double> numSet;
        java.util.function.Supplier<Boolean> boolGet;
        java.util.function.Consumer<Boolean> boolSet;
        
        Setting(String name, java.util.function.Supplier<Double> get, java.util.function.Consumer<Double> set, double min, double max, double step) {
            this.name = name; this.type = 1;
            this.numGet = get; this.numSet = set;
            this.min = min; this.max = max; this.step = step;
        }
        
        Setting(String name, java.util.function.Supplier<Boolean> get, java.util.function.Consumer<Boolean> set) {
            this.name = name; this.type = 0;
            this.boolGet = get; this.boolSet = set;
        }
        
        Setting(String name, int searchType) {
            this.name = name; this.type = 2;
        }
        
        double getNum() { return numGet != null ? numGet.get() : 0; }
        void setNum(double v) { if (numSet != null) numSet.accept(v); }
        boolean getBool() { return boolGet != null && boolGet.get(); }
        void setBool(boolean v) { if (boolSet != null) boolSet.accept(v); }
    }
}
