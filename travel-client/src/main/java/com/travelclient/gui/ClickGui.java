package com.travelclient.gui;

import com.travelclient.TravelClient;
import com.travelclient.module.Module;
import com.travelclient.module.Module.Category;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class ClickGui extends Screen {
    
    private static final int PANEL_WIDTH = 80;
    private static final int HEADER_HEIGHT = 14;
    private static final int MODULE_HEIGHT = 12;
    private static final int PANEL_GAP = 2;
    
    private final boolean[] expanded = {true, true, true, true, true};
    private int scrollX = 0;
    
    public ClickGui() {
        super(Text.literal("Truancy"));
    }
    
    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        // Dark background
        ctx.fill(0, 0, width, height, 0xAA000000);
        
        // Title bar
        ctx.fill(0, 0, width, 16, 0xFF111111);
        ctx.drawCenteredTextWithShadow(textRenderer, "§c§lTruancy §7| §fDrag or scroll to pan", width / 2, 4, 0xFFFFFF);
        
        // Calculate total width needed
        Category[] cats = Category.values();
        int totalWidth = cats.length * (PANEL_WIDTH + PANEL_GAP);
        
        // Clamp scroll
        int maxScroll = Math.max(0, totalWidth - width + 20);
        if (scrollX < 0) scrollX = 0;
        if (scrollX > maxScroll) scrollX = maxScroll;
        
        // Draw panels
        int x = 10 - scrollX;
        for (int i = 0; i < cats.length; i++) {
            if (x + PANEL_WIDTH > 0 && x < width) {
                drawPanel(ctx, mx, my, x, 22, i, cats[i]);
            }
            x += PANEL_WIDTH + PANEL_GAP;
        }
        
        // Scroll indicators
        if (scrollX > 0) {
            ctx.drawTextWithShadow(textRenderer, "§e◀", 2, height / 2, 0xFFFFFF);
        }
        if (scrollX < maxScroll) {
            ctx.drawTextWithShadow(textRenderer, "§e▶", width - 10, height / 2, 0xFFFFFF);
        }
    }
    
    private void drawPanel(DrawContext ctx, int mx, int my, int x, int y, int idx, Category cat) {
        List<Module> mods = getModules(cat);
        int contentHeight = expanded[idx] ? mods.size() * MODULE_HEIGHT : 0;
        int totalHeight = HEADER_HEIGHT + contentHeight;
        
        // Clamp height to screen
        int maxHeight = height - y - 10;
        if (totalHeight > maxHeight) totalHeight = maxHeight;
        
        // Panel background
        ctx.fill(x, y, x + PANEL_WIDTH, y + totalHeight, 0xEE151520);
        
        // Header
        ctx.fill(x, y, x + PANEL_WIDTH, y + HEADER_HEIGHT, 0xFF252535);
        ctx.drawTextWithShadow(textRenderer, cat.getName(), x + 3, y + 3, 0xFFe94560);
        ctx.drawTextWithShadow(textRenderer, expanded[idx] ? "-" : "+", x + PANEL_WIDTH - 8, y + 3, 0xFF888888);
        
        if (!expanded[idx]) return;
        
        // Modules (only draw what fits)
        int my2 = y + HEADER_HEIGHT;
        for (Module mod : mods) {
            if (my2 + MODULE_HEIGHT > height - 10) break;
            
            boolean hover = mx >= x && mx < x + PANEL_WIDTH && my >= my2 && my < my2 + MODULE_HEIGHT;
            
            if (hover) {
                ctx.fill(x, my2, x + PANEL_WIDTH, my2 + MODULE_HEIGHT, 0xFF333345);
            }
            
            int col = mod.isEnabled() ? 0xFF55FF55 : 0xFFAAAAAA;
            ctx.drawTextWithShadow(textRenderer, mod.getName(), x + 2, my2 + 2, col);
            
            my2 += MODULE_HEIGHT;
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int mx = (int) mouseX;
        int my = (int) mouseY;
        
        Category[] cats = Category.values();
        int x = 10 - scrollX;
        
        for (int i = 0; i < cats.length; i++) {
            int py = 22;
            List<Module> mods = getModules(cats[i]);
            
            // Header click
            if (mx >= x && mx < x + PANEL_WIDTH && my >= py && my < py + HEADER_HEIGHT) {
                expanded[i] = !expanded[i];
                return true;
            }
            
            if (expanded[i]) {
                int my2 = py + HEADER_HEIGHT;
                for (Module mod : mods) {
                    if (my2 + MODULE_HEIGHT > height - 10) break;
                    
                    if (mx >= x && mx < x + PANEL_WIDTH && my >= my2 && my < my2 + MODULE_HEIGHT) {
                        if (button == 0) {
                            mod.toggle();
                        } else if (button == 1) {
                            MinecraftClient.getInstance().setScreen(new SettingsGui(mod, this));
                        }
                        return true;
                    }
                    my2 += MODULE_HEIGHT;
                }
            }
            
            x += PANEL_WIDTH + PANEL_GAP;
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollX -= (int) (verticalAmount * 40);
        return true;
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        scrollX -= (int) deltaX;
        return true;
    }
    
    private List<Module> getModules(Category cat) {
        List<Module> list = new ArrayList<>();
        for (Module m : TravelClient.moduleManager.getModules()) {
            if (m.getCategory() == cat) list.add(m);
        }
        return list;
    }
    
    @Override
    public boolean shouldPause() {
        return false;
    }
}
