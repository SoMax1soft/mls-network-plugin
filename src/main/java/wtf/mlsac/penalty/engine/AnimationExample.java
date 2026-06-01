/*
 * This file is part of MLSAC - AI powered Anti-Cheat
 * Copyright (C) 2026 MLSAC Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package wtf.mlsac.penalty.engine;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import wtf.mlsac.penalty.BanAnimationEngine;

/**
 * Примеры использования Animation Engine.
 * Этот класс демонстрирует различные способы создания и запуска анимаций.
 */
public class AnimationExample {

    /**
     * Пример 1: Использование готового пресета
     */
    public static void example1_UsePreset(JavaPlugin plugin, Player player) {
        // Создаем конфигурацию из пресета
        BanAnimationConfig config = AnimationPresets.createClassicBanAnimation();
        
        // Создаем движок
        BanAnimationEngine engine = new BanAnimationEngine(plugin, config);
        
        // Запускаем анимацию
        String banCommand = "ban " + player.getName() + " Cheating detected";
        engine.playAnimation(player, banCommand);
    }

    /**
     * Пример 2: Создание простой анимации программно
     */
    public static void example2_SimpleAnimation(JavaPlugin plugin, Player player) {
        BanAnimationConfig config = new BanAnimationConfig();
        config.totalTicks = 60;
        config.freezePlayer = true;
        config.strikeLightningAtEnd = true;

        // Один стейдж с огненной спиралью
        BanAnimationConfig.StageConfig stage = new BanAnimationConfig.StageConfig();
        stage.name = "Fire Spiral";
        stage.startTick = 1;
        stage.endTick = 60;

        BanAnimationConfig.ParticleEffectConfig particle = new BanAnimationConfig.ParticleEffectConfig();
        particle.particleType = Particle.FLAME;
        particle.shape = "RISING_SPIRAL";
        particle.intervalTicks = 1;
        particle.count = 3;
        particle.radiusStart = 2.0;
        particle.radiusEnd = 1.0;
        particle.heightOffsetStart = 0.0;
        particle.heightOffsetEnd = 3.0;
        particle.speed = 0.02;

        stage.particles.add(particle);
        config.stages.add(stage);

        BanAnimationEngine engine = new BanAnimationEngine(plugin, config);
        engine.playAnimation(player, "ban " + player.getName() + " Hacking");
    }

    /**
     * Пример 3: Параллельные стейджи с цветными частицами
     */
    public static void example3_ParallelStages(JavaPlugin plugin, Player player) {
        BanAnimationConfig config = new BanAnimationConfig();
        config.totalTicks = 80;
        config.freezePlayer = true;
        config.strikeLightningAtEnd = true;

        // Стейдж 1: Красная спираль (тики 1-40)
        BanAnimationConfig.StageConfig stage1 = new BanAnimationConfig.StageConfig();
        stage1.name = "Red Spiral";
        stage1.startTick = 1;
        stage1.endTick = 40;

        BanAnimationConfig.ParticleEffectConfig redSpiral = new BanAnimationConfig.ParticleEffectConfig();
        redSpiral.particleType = Particle.REDSTONE;
        redSpiral.shape = "RISING_SPIRAL";
        redSpiral.intervalTicks = 1;
        redSpiral.count = 2;
        redSpiral.radiusStart = 1.5;
        redSpiral.radiusEnd = 1.0;
        redSpiral.heightOffsetStart = 0.0;
        redSpiral.heightOffsetEnd = 2.0;
        redSpiral.isColored = true;
        redSpiral.red = 255;
        redSpiral.green = 0;
        redSpiral.blue = 0;
        redSpiral.particleSize = 1.5f;

        stage1.particles.add(redSpiral);
        config.stages.add(stage1);

        // Стейдж 2: Синяя сфера (тики 20-80, параллельно с красной спиралью на 20-40)
        BanAnimationConfig.StageConfig stage2 = new BanAnimationConfig.StageConfig();
        stage2.name = "Blue Sphere";
        stage2.startTick = 20;
        stage2.endTick = 80;

        BanAnimationConfig.ParticleEffectConfig blueSphere = new BanAnimationConfig.ParticleEffectConfig();
        blueSphere.particleType = Particle.REDSTONE;
        blueSphere.shape = "SPHERE";
        blueSphere.intervalTicks = 1;
        blueSphere.count = 25;
        blueSphere.radiusStart = 3.0;
        blueSphere.radiusEnd = 0.5;
        blueSphere.heightOffsetStart = 1.0;
        blueSphere.heightOffsetEnd = 1.0;
        blueSphere.isColored = true;
        blueSphere.red = 0;
        blueSphere.green = 0;
        blueSphere.blue = 255;
        blueSphere.particleSize = 1.2f;

        stage2.particles.add(blueSphere);
        config.stages.add(stage2);

        BanAnimationEngine engine = new BanAnimationEngine(plugin, config);
        engine.playAnimation(player, "ban " + player.getName() + " Unfair advantage");
    }

    /**
     * Пример 4: Анимация со звуками
     */
    public static void example4_WithSounds(JavaPlugin plugin, Player player) {
        BanAnimationConfig config = new BanAnimationConfig();
        config.totalTicks = 100;
        config.freezePlayer = true;
        config.strikeLightningAtEnd = true;

        // Стейдж с частицами и звуками
        BanAnimationConfig.StageConfig stage = new BanAnimationConfig.StageConfig();
        stage.name = "Warning";
        stage.startTick = 1;
        stage.endTick = 100;

        // Частицы
        BanAnimationConfig.ParticleEffectConfig particle = new BanAnimationConfig.ParticleEffectConfig();
        particle.particleType = Particle.REDSTONE;
        particle.shape = "CIRCLE";
        particle.intervalTicks = 2;
        particle.count = 16;
        particle.radiusStart = 1.0;
        particle.radiusEnd = 2.5;
        particle.heightOffsetStart = 0.0;
        particle.heightOffsetEnd = 0.0;
        particle.isColored = true;
        particle.red = 255;
        particle.green = 255;
        particle.blue = 0;
        particle.particleSize = 1.5f;

        stage.particles.add(particle);

        // Зацикленный звук предупреждения
        BanAnimationConfig.SoundEffectConfig loopSound = new BanAnimationConfig.SoundEffectConfig();
        loopSound.soundType = Sound.BLOCK_NOTE_BLOCK_PLING;
        loopSound.playAtTick = 0;
        loopSound.volume = 0.8f;
        loopSound.pitch = 1.5f;
        loopSound.loop = true;
        loopSound.loopIntervalTicks = 10;

        stage.sounds.add(loopSound);

        // Финальный звук взрыва
        BanAnimationConfig.SoundEffectConfig finalSound = new BanAnimationConfig.SoundEffectConfig();
        finalSound.soundType = Sound.ENTITY_GENERIC_EXPLODE;
        finalSound.playAtTick = 95;
        finalSound.volume = 2.0f;
        finalSound.pitch = 0.8f;
        finalSound.loop = false;

        stage.sounds.add(finalSound);

        config.stages.add(stage);

        BanAnimationEngine engine = new BanAnimationEngine(plugin, config);
        engine.playAnimation(player, "ban " + player.getName() + " Cheating");
    }

    /**
     * Пример 5: Использование AnimationManager
     */
    public static void example5_UseManager(JavaPlugin plugin, Player player) {
        // Создаем и инициализируем менеджер
        AnimationManager manager = new AnimationManager(plugin);
        manager.initialize();

        // Получаем список доступных анимаций
        plugin.getLogger().info("Available animations: " + manager.getAvailableAnimations());

        // Запускаем анимацию по имени
        manager.playAnimation(player, "classic", "ban " + player.getName() + " Hacking");

        // Или используем анимацию по умолчанию
        manager.playDefaultAnimation(player, "ban " + player.getName() + " Cheating");

        // Проверяем, анимируется ли игрок
        if (manager.isAnimating(player)) {
            plugin.getLogger().info(player.getName() + " is currently being animated");
        }

        // Перезагрузка анимаций
        manager.reload();

        // Очистка при выключении
        manager.shutdown();
    }

    /**
     * Пример 6: Загрузка из YAML файла
     */
    public static void example6_LoadFromYAML(JavaPlugin plugin, Player player) {
        AnimationConfigLoader loader = new AnimationConfigLoader(plugin);

        // Загрузка из файла
        BanAnimationConfig config = loader.loadFromFile("rainbow_ban.yml");

        if (config != null) {
            BanAnimationEngine engine = new BanAnimationEngine(plugin, config);
            engine.playAnimation(player, "ban " + player.getName() + " Cheating");
        } else {
            plugin.getLogger().warning("Failed to load animation, using default");
            BanAnimationEngine engine = new BanAnimationEngine(plugin, 
                AnimationPresets.createClassicBanAnimation());
            engine.playAnimation(player, "ban " + player.getName() + " Cheating");
        }
    }

    /**
     * Пример 7: Сложная многостадийная анимация
     */
    public static void example7_ComplexAnimation(JavaPlugin plugin, Player player) {
        BanAnimationConfig config = new BanAnimationConfig();
        config.totalTicks = 120;
        config.freezePlayer = true;
        config.strikeLightningAtEnd = true;

        // Стадия 1: Предупреждение (тики 1-40)
        BanAnimationConfig.StageConfig warning = new BanAnimationConfig.StageConfig();
        warning.name = "Warning";
        warning.startTick = 1;
        warning.endTick = 40;

        BanAnimationConfig.ParticleEffectConfig yellowCircle = new BanAnimationConfig.ParticleEffectConfig();
        yellowCircle.particleType = Particle.REDSTONE;
        yellowCircle.shape = "CIRCLE";
        yellowCircle.intervalTicks = 2;
        yellowCircle.count = 12;
        yellowCircle.radiusStart = 1.0;
        yellowCircle.radiusEnd = 1.5;
        yellowCircle.isColored = true;
        yellowCircle.red = 255;
        yellowCircle.green = 255;
        yellowCircle.blue = 0;
        yellowCircle.particleSize = 1.0f;

        warning.particles.add(yellowCircle);
        config.stages.add(warning);

        // Стадия 2: Нарастание (тики 30-80, параллельно с предупреждением)
        BanAnimationConfig.StageConfig buildup = new BanAnimationConfig.StageConfig();
        buildup.name = "Build Up";
        buildup.startTick = 30;
        buildup.endTick = 80;

        BanAnimationConfig.ParticleEffectConfig helix = new BanAnimationConfig.ParticleEffectConfig();
        helix.particleType = Particle.FLAME;
        helix.shape = "HELIX";
        helix.intervalTicks = 1;
        helix.count = 2;
        helix.radiusStart = 0.5;
        helix.radiusEnd = 1.5;
        helix.heightOffsetStart = 0.0;
        helix.heightOffsetEnd = 2.5;
        helix.speed = 0.02;

        buildup.particles.add(helix);
        config.stages.add(buildup);

        // Стадия 3: Кульминация (тики 70-110)
        BanAnimationConfig.StageConfig climax = new BanAnimationConfig.StageConfig();
        climax.name = "Climax";
        climax.startTick = 70;
        climax.endTick = 110;

        BanAnimationConfig.ParticleEffectConfig redSphere = new BanAnimationConfig.ParticleEffectConfig();
        redSphere.particleType = Particle.REDSTONE;
        redSphere.shape = "SPHERE";
        redSphere.intervalTicks = 1;
        redSphere.count = 30;
        redSphere.radiusStart = 3.0;
        redSphere.radiusEnd = 0.3;
        redSphere.isColored = true;
        redSphere.red = 255;
        redSphere.green = 0;
        redSphere.blue = 0;
        redSphere.particleSize = 2.0f;

        climax.particles.add(redSphere);
        config.stages.add(climax);

        // Стадия 4: Взрыв (тики 110-120)
        BanAnimationConfig.StageConfig explosion = new BanAnimationConfig.StageConfig();
        explosion.name = "Explosion";
        explosion.startTick = 110;
        explosion.endTick = 120;

        BanAnimationConfig.ParticleEffectConfig explode = new BanAnimationConfig.ParticleEffectConfig();
        explode.particleType = Particle.EXPLOSION_LARGE;
        explode.shape = "EXPLOSION";
        explode.intervalTicks = 3;
        explode.count = 30;
        explode.radiusStart = 3.0;
        explode.radiusEnd = 3.0;
        explode.speed = 0.1;

        explosion.particles.add(explode);

        BanAnimationConfig.SoundEffectConfig explosionSound = new BanAnimationConfig.SoundEffectConfig();
        explosionSound.soundType = Sound.ENTITY_GENERIC_EXPLODE;
        explosionSound.playAtTick = 0;
        explosionSound.volume = 2.0f;
        explosionSound.pitch = 0.8f;

        explosion.sounds.add(explosionSound);
        config.stages.add(explosion);

        BanAnimationEngine engine = new BanAnimationEngine(plugin, config);
        engine.playAnimation(player, "ban " + player.getName() + " Severe cheating");
    }
}
