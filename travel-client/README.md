# Travel Client

A Minecraft Fabric mod for survival and travel utilities. Designed for safe elytra and ground navigation with extensive AFK safety features.

## Features

### 🚀 Core Travel (AFK-Safe Priority)
- **ElytraFly** - Auto pitch correction for efficient gliding
- **ElytraNav** - Automatically steers toward target coordinates while flying
- **AutoFirework** - Uses fireworks when elytra speed drops
- **GroundNav** - Auto-walks/sprints toward target coordinates with auto-jump
- **ElytraReplace** - Automatically swaps to fresh elytra when durability is low

### 🛡️ Core Safety (Essential for AFK)
- **AutoTotem** - Keeps totem of undying in offhand
- **AutoEat** - Eats food when health/hunger is low (golden apples for emergencies)
- **AutoDisconnect** - DISCONNECTS when health is critical or totem pops (AFK lifesaver!)
- **AutoRespawn** - Automatically respawns after death
- **AntiAFK** - Prevents being kicked for inactivity
- **DeathCoords** - Saves and displays coordinates when you die
- **HealthWarn** - Screen flash and sound when health is low

### 🏃 Movement
- **SafeWalk** - Prevents walking off edges
- **AutoSprint** - Automatically sprints when moving forward
- **NoFall** - Prevents fall damage
- **Jesus** - Walk on water and lava
- **Freecam** - Fly around without moving your player
- **AntiVelocity** - Reduces knockback from attacks

### 🎒 Player Utility
- **AutoArmor** - Automatically equips best armor
- **AutoTool** - Switches to the best tool for the block you're mining
- **FastPlace** - Removes delay between block placements

### 👁️ ESP / Render
- **HUD** - Displays coordinates, speed, direction, target info, active modules
- **Fullbright** - See in the dark without torches
- **PlayerESP** - Highlights players with tracers and boxes (color-coded by distance)
- **ChestESP** - Highlights containers through walls (chests, shulkers, barrels)
- **MobESP** - Highlights mobs (red = hostile, green = passive)
- **ItemESP** - Highlights dropped items
- **BlockESP** - Find spawners, portals, ancient debris, diamond ore
- **Nametags** - Enhanced nametags with health and distance
- **Waypoints** - Save and display custom waypoint locations
- **ChunkBorders** - Shows chunk boundaries
- **LightLevel** - Spawn-proofing overlay (red = mobs can spawn)

## Installation

### Requirements
- Java Development Kit (JDK) 21
- Minecraft 1.21.1
- Fabric Loader 0.15+

### Building
1. Open terminal in project folder
2. Run: `./gradlew build` (Linux/Mac) or `gradlew.bat build` (Windows)
3. Find the mod jar in `build/libs/travel-client-1.0.0.jar`

### Installing
1. Install Fabric Loader for Minecraft 1.21.1
2. Install Fabric API
3. Copy `travel-client-1.0.0.jar` to your `.minecraft/mods` folder
4. Launch Minecraft with Fabric profile

## Usage

### Opening the GUI
Press **Right Shift** to open the module menu

### Setting a Destination
1. Open GUI with Right Shift
2. Enter X and Z coordinates
3. Click "Set Target"
4. Enable ElytraNav (for flying) or GroundNav (for walking)

### Recommended Module Combos

**🌙 AFK Elytra Travel (Sleep Mode):**
- ElytraFly + ElytraNav + AutoFirework + ElytraReplace
- AutoTotem + AutoEat + AutoDisconnect + DeathCoords
- AntiAFK + HUD

**🌙 AFK Ground Travel:**
- GroundNav + SafeWalk + AutoSprint
- AutoTotem + AutoEat + AutoDisconnect + DeathCoords
- AntiAFK + HUD

**🎮 Active Elytra Travel:**
- ElytraFly + AutoFirework + ElytraReplace
- AutoTotem + AutoEat + HealthWarn
- HUD + PlayerESP + Fullbright

**🔍 Exploration:**
- Fullbright + ChestESP + PlayerESP + MobESP + ItemESP
- HUD + AutoTotem + AutoEat + SafeWalk
- Waypoints + DeathCoords

**⚔️ PvP Awareness:**
- PlayerESP + Nametags + HealthWarn
- AutoTotem + AutoEat + AntiVelocity
- HUD + Fullbright

**🏗️ Building:**
- FastPlace + AutoTool + LightLevel + ChunkBorders
- SafeWalk + Fullbright

## Configuration

Settings are saved to `.minecraft/config/travelclient.json`

Module states and target coordinates persist between sessions.

## Keybinds

- **Right Shift** - Open/close module GUI

## Notes

### AFK Safety System
The mod has a layered safety system for AFK travel:
1. **AutoTotem** - First line of defense, keeps totem ready
2. **AutoEat** - Keeps health/hunger topped up
3. **HealthWarn** - Alerts you if health gets low
4. **AutoDisconnect** - Last resort, disconnects before death (saves coords!)
5. **DeathCoords** - If all else fails, you know where you died

For maximum AFK safety, enable **all** of these modules.

### General Notes
- The mod is client-side only
- Works on any server (but check server rules!)
- AutoFirework requires firework rockets in hotbar
- AutoTotem requires totems in inventory
- AutoEat prioritizes golden apples when health is low
- ElytraReplace will swap elytras when durability drops below 10
- AutoDisconnect triggers at 2 hearts OR when a totem pops
- DeathCoords logs deaths to chat and saves them in memory

## Credits

Built with Fabric API for Minecraft 1.21.1
