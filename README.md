# 🎛️ ControlSwap — One Character, Many Players

<div align="center">

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.8%2B-brightgreen?style=for-the-badge&logo=minecraft)
![API](https://img.shields.io/badge/API-Paper%2FSpigot_1.21.8-blue?style=for-the-badge)
![Status](https://img.shields.io/badge/Mode-Runner_Only-9cf?style=for-the-badge)

</div>

---

> Many players, one body. Only one runner controls the character at a time.  
> Control swaps on a timer while everyone shares a single state. No hunters.

---

## 📺 Inspired By

<div align="center">

<a href="https://www.youtube.com/watch?v=GwrAvYlT7xg" target="_blank">Watch Sapnap’s Speedrunner Swap</a>

</div>

---

## 🚀 How It Works

### 🔄 Queue‑Based Swapping
- 2 players: simple alternation; the active runner swaps every interval.
- 3+ players: the first active runner is chosen at random, then the queue rotates each swap.
- Everyone shares inventory, health, XP, effects, and position.

### 🕶 Inactive Runners
- See a blacked‑out, frozen view (freeze mode configurable).
- Actionbar shows “Queued (n)” — runner in position 1 sees “You’re up next”.

### ⏲ Active Runner
- Sees a countdown “Swap in: Ns” in the actionbar.

### 🎯 Objective
- Beat the Ender Dragon. When the dragon dies, a clickable donation link is sent to all players.

---

## ✨ Features

<div align="center">

| 🎛️ Feature | 📝 Description |
|:---|:---|
| **Queue System** | Any number of runners; rotates fairly on every swap |
| **Countdown & Queue HUD** | Active sees timer; others see “Queued (n)”/“You’re up next” |
| **Customizable Swaps** | Fixed or randomized intervals, jitter, grace period, pause on disconnect |
| **Safe Swap** | Avoids lava/fire/cactus etc.; scans for nearby safe blocks |
| **Freeze Modes** | EFFECTS, SPECTATOR, LIMBO, or CAGE (default) |
| **Robust CAGE** | Works in Overworld, Nether, and End (safe Y + chunk preload + cross‑world rebuild) |
| **Broadcasts** | Start/stop notifications and donation link on victory |
| **1.21.8 API** | Built for Paper/Spigot 1.21.8 with fallbacks for older APIs |

</div>

---

## ⚙️ Configuration

Edit `plugins/ControlSwap/config.yml` after first run.

- `teams.runners`: list of runner names (optional when using commands)
- `swap.interval`: base seconds (default 60)
- `swap.randomize`: true/false
- `swap.min_interval` / `swap.max_interval` / `swap.jitter.stddev`
- `swap.grace_period_ticks`: invulnerability for new active runner
- `safe_swap.enabled` + `safe_swap.horizontal_radius` / `vertical_distance` / `dangerous_blocks`
- `freeze_mode`: `EFFECTS` | `SPECTATOR` | `LIMBO` | `CAGE` (default)
- `broadcasts.*`: enable/disable start/stop/team messages
- `donation.url`: link shown when the dragon dies

---

## 📝 Commands

<div align="center">

| Command | Description | Notes |
|:--|:--|:--|
| `/swap start` | Start the game | Requires runners set |
| `/swap stop` | End the current game |  |
| `/swap pause` | Pause the game |  |
| `/swap resume` | Resume the game |  |
| `/swap status` | Show current runner, time to swap, queue |  |
| `/swap shuffle` | Shuffle the queue (active stays active) | Admin |
| `/swap setrunners <p1> [p2] ...` | Set/replace runners and their initial queue order |  |
| `/swap reload` | Reload configuration | Admin |
| `/swap clearteams` | Clear runners | Admin |

Permissions: `controlswap.command` (default: op), `controlswap.admin` (default: op)

</div>

---

## 🛠 Installation

1) Download the latest `controlswap-*.jar`  
2) Place it into your server’s `plugins/` directory  
3) Restart the server  
4) Edit `plugins/ControlSwap/config.yml` as desired  
5) Use `/swap setrunners ...` then `/swap start`

Works best on Paper 1.21.8; compatible with Spigot and older 1.21.x via runtime fallbacks.

---

## 🧱 Notes on CAGE Mode

- Cages are constructed in the player’s current world (not teleported across dimensions).
- Safe Y is chosen per environment (Overworld/Nether/End) and clamped within the world height.
- Neighboring chunks are preloaded to prevent partial generation.
- If a caged runner changes worlds, the old cage is removed and rebuilt in the new world automatically.

---

## 🙌 Credits & Support

**Inspired by:** the “Speedrunner Swap” videos (see Sapnap’s run above)  
**Developed by:** muj3b

[![Donate](https://img.shields.io/badge/💖_Donate-Support_Development-ff69b4?style=for-the-badge)](https://donate.stripe.com/8x29AT0H58K03judnR0Ba01)

Enjoy — and good luck swapping! 🚀

