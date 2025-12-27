# 🚀 Travel Client

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-green.svg)](https://minecraft.net)
[![Fabric](https://img.shields.io/badge/Mod%20Loader-Fabric-blue.svg)](https://fabricmc.net)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![GitHub release](https://img.shields.io/github/v/release/Trauncy209/Travel-Client?include_prereleases)](https://github.com/Trauncy209/Travel-Client/releases)

A powerful Minecraft Fabric utility client designed for survival, travel, and PvP. Features extensive AFK-safe automation, elytra navigation, ESP overlays, and combat assistance.

![Travel Client](https://i.imgur.com/placeholder.png)

---

## ✨ Features

### 🛫 Travel & Navigation
| Module | Description |
|--------|-------------|
| **ElytraFly** | Advanced elytra flight with auto pitch correction and speed control |
| **ElytraNav** | Automatically steers toward target coordinates while flying |
| **GroundNav** | Auto-walks/sprints toward coordinates with pathfinding |
| **AutoFirework** | Uses fireworks when elytra speed drops |
| **ElytraReplace** | Swaps to fresh elytra when durability is low |

### ⚔️ Combat
| Module | Description |
|--------|-------------|
| **KillAura** | Automatically attacks nearby entities |
| **AutoCrystal** | End crystal PvP automation |
| **Aimbot** | Locks aim onto targets |
| **Criticals** | Forces critical hits |
| **Reach** | Extended attack range |
| **Surround** | Places obsidian around your feet |
| **AnchorAura** | Respawn anchor automation |

### 🏃 Movement
| Module | Description |
|--------|-------------|
| **Fly** | Creative-style flight |
| **Speed** | Move faster |
| **NoFall** | Prevents fall damage |
| **Jesus** | Walk on water and lava |
| **Freecam** | Detach camera from player |
| **SafeWalk** | Prevents walking off edges |
| **AutoSprint** | Always sprint when moving |
| **AntiVelocity** | Reduces knockback |

### 🎒 Player
| Module | Description |
|--------|-------------|
| **AutoTotem** | Keeps totem in offhand |
| **AutoEat** | Eats when hungry or low health |
| **AutoArmor** | Equips best armor automatically |
| **AutoTool** | Switches to best tool for mining |
| **FastPlace** | Removes block placement delay |
| **AirPlace** | Place blocks in mid-air |
| **Scaffold** | Auto-places blocks under you |
| **AutoDisconnect** | Disconnects when health is critical |
| **DeathCoords** | Saves coordinates on death |
| **AntiAFK** | Prevents AFK kicks |

### 👁️ Render / ESP
| Module | Description |
|--------|-------------|
| **PlayerESP** | Highlight players through walls |
| **ChestESP** | Highlight containers (chests, shulkers, barrels) |
| **MobESP** | Highlight mobs (color-coded hostile/passive) |
| **ItemESP** | Highlight dropped items |
| **BlockESP** | Find spawners, portals, ores |
| **Fullbright** | See in complete darkness |
| **Nametags** | Enhanced nametags with health/distance |
| **Waypoints** | Custom waypoint markers |
| **ChunkBorders** | Show chunk boundaries |
| **HUD** | Coordinates, speed, FPS overlay |
| **NewerChunks** | Detect newly generated chunks |
| **ShulkerPreview** | See shulker box contents on hover |

---

## 📦 Installation

### Requirements
- Minecraft **1.21.1**
- [Fabric Loader](https://fabricmc.net/use/installer/) **0.15.0+**
- [Fabric API](https://modrinth.com/mod/fabric-api)

### Steps
1. Install Fabric Loader for Minecraft 1.21.1
2. Download Fabric API and put it in your `mods` folder
3. Download `travel-client-1.0.0.jar` from [Releases](https://github.com/Trauncy209/Travel-Client/releases)
4. Put the JAR in your `mods` folder
5. Launch Minecraft with the Fabric profile

---

## 🎮 Controls

| Key | Action |
|-----|--------|
| **Right Shift** | Open/close Click GUI |
| **Arrow Keys** | Navigate GUI |
| **Enter** | Toggle selected module |

## 🔧 Building from Source

```bash
# Clone the repository
git clone https://github.com/Trauncy209/Travel-Client.git
cd Travel-Client

# Build with Gradle
./gradlew build

# JAR will be in build/libs/
```

Or on Windows, just run `BUILD.bat`

---

## 📸 Screenshots

*Coming soon!*

<!-- 
Add screenshots like this:
![GUI](screenshots/gui.png)
![ESP](screenshots/esp.png)
-->

---

## ⚠️ Disclaimer

This mod is for **educational purposes** and **single-player/private server use**. 

- Using utility clients on public servers may violate their rules
- You may be banned for using certain features
- Use at your own risk

---

## 🤝 Contributing

Contributions are welcome! Feel free to:
- Open issues for bugs or feature requests
- Submit pull requests
- Fork and make your own version

---

## 📜 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 💬 Credits

- **Developer:** (Trauncy209)
- Built with [Fabric](https://fabricmc.net/)
- Inspired by clients like Meteor, Wurst, and Aristois

---

<p align="center">
  <b>⭐ Star this repo if you find it useful! ⭐</b>
</p>
