# MLSAC 2.0 — AI anticheat for Minecraft servers

**MLSAC** is a machine-learning anticheat. The plugin watches how players fight — aim, clicks, movement
around the target — and sends compact feature windows to the MLSAC cloud, where neural networks score
them for KillAura, TriggerBot, AimAssist and Reach. What happens to a cheater (alerts, damage
reduction, kicks, bans) is configured in presets on [mlsac.net](https://mlsac.net).

🇷🇺 [Русский ниже](#mlsac-20--ии-античит-для-серверов-minecraft)

## Download

Get the ready plugin from **[mlsac.net](https://mlsac.net/wiki/start/install)**:

| Server Java | Jar |
|---|---|
| Java 21 or newer | `MLSAC-2.0.x-java21.jar` |
| Java 17 | `MLSAC-2.0.x-java17.jar` |

## Requirements

- Paper and forks (Purpur, Pufferfish, Leaf…), Spigot, Folia
- Minecraft 1.16.5 – 1.21.x
- Java 17+
- A licence key from mlsac.net (there is a free plan)

PacketEvents is bundled — nothing else to install. WorldGuard is optional (regions without checks).

## Install

1. Stop the server; remove an old MLSAC 1.x jar if there is one.
2. Put the jar into `plugins/` and start the server.
3. Paste your licence key into `plugins/MLSAC/config.yml` → `api-key`, then `/mlsac reload`.
4. Set up models, punishments and menus in **Configs** on mlsac.net.

Full guide: [mlsac.net/wiki](https://mlsac.net/wiki/start/install).

## Building from source

```bash
./gradlew build
```

Jars land in `build/dist/`. Modules: `core` (backend link, config, packets), `pipeline` (feature
extraction and windows), `features` (alerts, menus, nametags, punishments), `suppression` (suppression
ladder), `app` (bootstrap).

> The public source does not contain the protected transport used by the official build for the Ultra
> model. A build from this repository talks to the backend over the plain protocol; Ultra on the
> newest protocol needs the official jar from mlsac.net.

## Licence

Source-available — see [LICENSE](LICENSE). You may read, build and run it on your own servers; you may
not redistribute it, sell it or use it to build other anticheats or cheats. Third-party components:
[THIRD-PARTY-NOTICES](THIRD-PARTY-NOTICES).

---

# MLSAC 2.0 — ИИ-античит для серверов Minecraft

**MLSAC** — античит на машинном обучении. Плагин смотрит, как игрок дерётся: прицел, клики, движение
вокруг цели, — и отправляет компактные окна признаков в облако MLSAC. Там нейросети оценивают их на
KillAura, TriggerBot, AimAssist и Reach. Что делать с читером (алерты, снижение урона, кики, баны),
настраивается в пресетах на [mlsac.net](https://mlsac.net).

## Скачать

Готовый плагин — на **[mlsac.net](https://mlsac.net/wiki/start/install)**:

| Java сервера | Jar |
|---|---|
| Java 21 и новее | `MLSAC-2.0.x-java21.jar` |
| Java 17 | `MLSAC-2.0.x-java17.jar` |

## Требования

- Paper и форки (Purpur, Pufferfish, Leaf…), Spigot, Folia
- Minecraft 1.16.5 – 1.21.x
- Java 17+
- Ключ лицензии с mlsac.net (есть бесплатный тариф)

PacketEvents уже внутри — больше ничего ставить не нужно. WorldGuard по желанию (регионы без проверки).

## Установка

1. Останови сервер; если стоял MLSAC 1.x, удали старый jar.
2. Положи jar в `plugins/` и запусти сервер.
3. Вставь ключ лицензии в `plugins/MLSAC/config.yml` → `api-key`, затем `/mlsac reload`.
4. Модели, наказания и меню настраиваются во вкладке **«Конфиги»** на mlsac.net.

Подробно: [mlsac.net/wiki](https://mlsac.net/wiki/start/install).

## Сборка из исходников

```bash
./gradlew build
```

Jar появятся в `build/dist/`.

> В открытых исходниках нет защищённого транспорта, которым официальная сборка общается с моделью
> Ultra. Сборка из этого репозитория работает по обычному протоколу; Ultra на новейшем протоколе —
> только с официальным jar с mlsac.net.

## Лицензия

Код открыт для чтения — см. [LICENSE](LICENSE). Можно читать, собирать и запускать на своих серверах;
нельзя распространять, продавать и использовать для создания других античитов или читов.
