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

## 📺 Watch the original!

https://www.youtube.com/watch?v=GwrAvYlT7xg

---

## 🚀 How It Works

### 🔄 Queue‑Based Swapping
- 2 players: simple alternation; the active runner swaps every interval.
- 3+ players: the first active runner is chosen at random, then the queue rotates each swap.
- Inventory transfers to the new active runner at swap time; inactive runners have empty inventories. Health, XP, effects, and position still follow the active runner.

### 🕶 Inactive Runners
- See a blacked‑out, frozen view (freeze mode configurable).
- Actionbar shows “Queued (n)” — runner in position 1 sees “You’re up next”.

### ⏲ Active Runner
- By default, sees countdown only during the last 10 seconds (actionbar).
- Toggle to “Full timer” in the GUI if you prefer always-visible.

### 🎯 Objective
- Beat the Ender Dragon. When the dragon dies, a clickable donation link is sent to all players.

---

## ✨ Features

<div align="center">

| 🎛️ Feature | 📝 Description |
|:---|:---|
| **Queue System** | Any number of runners; rotates fairly on every swap |
| **Countdown & Queue HUD** | Active sees timer (last 10s default); waiting see full time |
| **Customizable Swaps** | Fixed or randomized intervals, jitter, grace period, pause on disconnect |
| **Smart Auto‑Resume** | If paused due to disconnects, auto‑resumes when a runner rejoins |
| **Safe Swap** | Avoids lava/fire/cactus etc.; scans for nearby safe blocks |
| **Freeze Modes** | EFFECTS, SPECTATOR, LIMBO, or CAGE (default) |
| **Robust CAGE** | Works in Overworld, Nether, and End (safe Y + chunk preload + cross‑world rebuild) |
| **In‑Game GUI Controls** | Toggle timer visibility, freeze mode, safe swap; adjust interval ±5s |
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
- `swap.auto_resume_on_join`: auto‑resume after disconnect pause when a runner rejoins (default `true`)
- `safe_swap.enabled` + `safe_swap.horizontal_radius` / `vertical_distance` / `dangerous_blocks`
- `freeze_mode`: `EFFECTS` | `SPECTATOR` | `LIMBO` | `CAGE` (default)
- `timer_visibility.runner_visibility`: `last_10` (default) or `always` or `never`
- `timer_visibility.waiting_visibility`: `always` (default) or `last_10` or `never`
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

## 🧭 GUI Quick Guide

- Start/Stop, Pause/Resume, Shuffle, Set Runners.
- Runner Timer: cycles FULL / LAST 10s / HIDDEN.
- Waiting Timer: cycles FULL / LAST 10s / HIDDEN.
- Interval: press `-5s` or `+5s` to adjust base swap interval; shows current value.
- Inactive Runner State: cycles EFFECTS → SPECTATOR → LIMBO → CAGE.
- Safe Swap: toggles hazard‑avoiding teleports.

Changes apply immediately; timer HUD refreshes on the spot.

---

## 🧱 Notes on CAGE Mode

- Cages are constructed in the player’s current world (not teleported across dimensions).
- Safe Y is chosen per environment (Overworld/Nether/End) and clamped within the world height.
- Neighboring chunks are preloaded to prevent partial generation.
- If a caged runner changes worlds, the old cage is removed and rebuilt in the new world automatically.

---

## 🧼 Changes (v1.3)

- Inactive runners no longer mirror the active runner’s inventory. Only the newly active runner receives the inventory at swap.
- Additional guards to keep inactive inventories empty across world changes.

## 🧼 Stability Improvements (v1.2)

- Queue rebuilds from configured runner names on joins/leaves to avoid losing runners.
- World change handling ensures cages/effects/visibility are reapplied correctly per player.
- Proper action bar fallback on Spigot (no chat spam on older servers).
- Inventory sync off‑hand null‑safety.

---

## 🙌 Credits & Support

**Inspired by:** the “Speedrunner Swap” videos (see Sapnap’s run above)  
**Developed by:** muj3b

[![Donate](https://img.shields.io/badge/💖_Donate-Support_Development-ff69b4?style=for-the-badge)](https://donate.stripe.com/8x29AT0H58K03judnR0Ba01)

Enjoy — and good luck swapping! 🚀
