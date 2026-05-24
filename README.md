<div align="center">

<!-- Превью-анимация названия -->
<img src="https://readme-typing-svg.demolab.com/?font=Unbounded&weight=700&size=38&pause=1000&color=4C8BF5&center=true&vCenter=true&width=700&lines=MLSAC;Modern+Minecraft+Anti-Cheat" alt="MLSAC Typings" />

<p align="center">
  <b>MLSAC</b> — клиент-серверная система защиты для Minecraft-серверов, построенная на базе удаленной аналитики.
  <br>
  Плагин выполняет роль легковесного коннектора, а вся обработка данных и вычисление нарушений происходят на стороне нашей инфраструктуры.
</p>

<!-- Навигационные ссылки -->
<p align="center">
  <a href="https://discord.gg/ARVNGSTWeg">
    <img src="https://img.shields.io/badge/Discord-Join%20Server-5865F2?style=flat-square&logo=discord&logoColor=white" alt="Discord" />
  </a>
  <a href="https://github.com/SoMax1soft/MLSAC/issues">
    <img src="https://img.shields.io/badge/Bugs-Report-ED4245?style=flat-square&logo=github" alt="Report Bug" />
  </a>
  <a href="https://github.com/SoMax1soft/MLSAC/issues">
    <img src="https://img.shields.io/badge/Features-Request-9B59B6?style=flat-square&logo=github" alt="Request Feature" />
  </a>
</p>

<!-- Статистика репозитория -->
<p align="center">
  <img src="https://img.shields.io/github/license/SoMax1soft/MLSAC?style=flat-square&color=4C8BF5" alt="License" />
  <img src="https://img.shields.io/github/stars/SoMax1soft/MLSAC?style=flat-square&color=4C8BF5" alt="Stars" />
  <img src="https://img.shields.io/github/forks/SoMax1soft/MLSAC?style=flat-square&color=4C8BF5" alt="Forks" />
</p>

</div>

---

## 🌐 Общая информация

MLSAC.NET переносит классическую задачу поиска читов с игрового сервера в облако. Это позволяет полностью исключить нагрузку на основной поток (TPS) сервера, вызванную сложными математическими проверками движения и пакетов. Плагин лишь собирает и отправляет необходимые метрики, получая обратно готовый вердикт.

---

## Ключевые особенности

| Функция | Описание |
| :--- | :--- |
| **Удаленная обработка** | Все вычисления перенесены на наши серверы. Влияние плагина на производительность вашего сервера сведено к нулю. |
| **Модели FAST и PRO** | Два независимых алгоритма детекции. Модель FAST отвечает за моментальную блокировку грубых хаков, а PRO анализирует скрытые модификации на основе поведенческих факторов. |
| **Межсерверный анализ** | Информация об известных нарушителях синхронизируется внутри сети MLSAC, позволяя автоматически брать под наблюдение подозрительных игроков при входе на ваш сервер. |
| **Центральная панель** | Полноценный веб-интерфейс для управления конфигурацией, просмотра графиков активности, логов нарушений и ведения базы банов. |
| **Уведомления** | Прямая интеграция с Discord и Telegram через вебхуки для мгновенного оповещения администрации о срабатываниях системы. |
| **Гибкая кастомизация** | Конфигурация разработана так, чтобы её можно было быстро адаптировать под правила и механику конкретного игрового режима без чтения лишней документации. |

---

## Системные требования

* **Платформы:** Spigot, Paper и любые форки на их основе (Purpur, Pufferfish и др.)
* **Среда выполнения:** Java 17 или новее

---

## Установка и запуск

1. Загрузите последнюю версию плагина со страницы **[GitHub Releases](https://github.com/SoMax1soft/MLSAC/releases)**.
2. Поместите файл `.jar` в директорию `plugins` вашего сервера и запустите его для генерации файлов конфигурации.
3. Откройте файл конфигурации и укажите токен авторизации для привязки сервера к вашей веб-панели.
4. Выберите используемую модель детекции (FAST/PRO) и при необходимости настройте вебхуки для отправки логов в Discord или Telegram.

---

<div align="center">
  <sub>MLSAC.NET — Modern Anti-Cheat Infrastructure</sub>
</div>
