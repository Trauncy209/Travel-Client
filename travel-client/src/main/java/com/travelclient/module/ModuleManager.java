package com.travelclient.module;

import com.travelclient.module.combat.*;
import com.travelclient.module.movement.*;
import com.travelclient.module.player.*;
import com.travelclient.module.render.*;
import java.util.ArrayList;
import java.util.List;

public class ModuleManager {
    private final List<Module> modules = new ArrayList<>();
    
    // Navigation target coords
    private double targetX = 0;
    private double targetZ = 0;
    private boolean hasTarget = false;
    
    public ModuleManager() {
        // === COMBAT MODULES ===
        modules.add(new KillAura());
        modules.add(new AutoCrystal());
        modules.add(new Criticals());
        modules.add(new Surround());
        modules.add(new AnchorAura());
        modules.add(new Aimbot());
        modules.add(new Reach());
        modules.add(new AutoPot());
        
        // === MOVEMENT MODULES ===
        modules.add(new ElytraFly(this));
        modules.add(new ElytraNavigate(this));
        modules.add(new AutoFirework(this));
        modules.add(new FakeElytra());
        modules.add(new Speed());
        modules.add(new Fly());
        modules.add(new SafeWalk());
        modules.add(new AutoSprint());
        modules.add(new NoFall());
        modules.add(new Jesus());
        modules.add(new Freecam());
        modules.add(new AntiVelocity());
        modules.add(new GroundNavigate(this));
        modules.add(new ElytraReplace());
        modules.add(new NetherNav(this));
        
        // === PLAYER MODULES ===
        modules.add(new AutoTotem());
        modules.add(new AutoEat());
        modules.add(new AutoDisconnect());
        modules.add(new AutoRespawn());
        modules.add(new AntiAFK());
        modules.add(new DeathCoords());
        modules.add(new LowHealthWarning());
        modules.add(new AntiGhast());
        modules.add(new AutoArmor());
        modules.add(new AutoTool());
        modules.add(new FastPlace());
        modules.add(new AutoMine());
        modules.add(new AutoBridge());
        modules.add(new Scaffold());
        modules.add(new GScaffold());
        modules.add(new AirPlace());
        modules.add(new Spammer());
        modules.add(new Dupe());
        modules.add(new NBTViewer());
        
        // === RENDER / ESP ===
        modules.add(new HudModule(this));
        modules.add(new ElytraDebugRenderer());
        modules.add(new Fullbright());
        modules.add(new PlayerESP());
        modules.add(new ChestESP());
        modules.add(new MobESP());
        modules.add(new ItemESP());
        modules.add(new BlockESP());
        modules.add(new Nametags());
        modules.add(new Waypoints());
        modules.add(new ChunkBorders());
        modules.add(new LightLevel());
        modules.add(new PacketLogger());
        modules.add(new ShulkerPreview());
        modules.add(new NewerChunks());
    }
    
    public void onTick() {
        for (Module module : modules) {
            if (module.isEnabled()) {
                module.onTick();
            }
        }
    }
    
    public List<Module> getModules() {
        return modules;
    }
    
    public Module getModule(String name) {
        for (Module module : modules) {
            if (module.getName().equalsIgnoreCase(name)) {
                return module;
            }
        }
        return null;
    }
    
    public <T extends Module> T getModule(Class<T> clazz) {
        for (Module module : modules) {
            if (clazz.isInstance(module)) {
                return clazz.cast(module);
            }
        }
        return null;
    }
    
    public void setTargetCoords(double x, double z) {
        this.targetX = x;
        this.targetZ = z;
        this.hasTarget = true;
    }
    
    public void clearTarget() {
        this.hasTarget = false;
    }
    
    public double getTargetX() { return targetX; }
    public double getTargetZ() { return targetZ; }
    public boolean hasTarget() { return hasTarget; }
    
    public double getDistanceToTarget(double playerX, double playerZ) {
        if (!hasTarget) return -1;
        double dx = targetX - playerX;
        double dz = targetZ - playerZ;
        return Math.sqrt(dx * dx + dz * dz);
    }
    
    public float getYawToTarget(double playerX, double playerZ) {
        if (!hasTarget) return 0;
        double dx = targetX - playerX;
        double dz = targetZ - playerZ;
        return (float) Math.toDegrees(Math.atan2(-dx, dz));
    }
}
