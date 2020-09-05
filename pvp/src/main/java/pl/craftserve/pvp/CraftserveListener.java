/*
 * Copyright 2020 Aleksander Jagiełło <themolkapl@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pl.craftserve.pvp;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.awt.Color;
import java.net.URL;
import java.time.Duration;
import java.util.Objects;

public class CraftserveListener implements Listener {
    private static final ChatColor COLOR = ChatColor.of(new Color(92, 184, 92));
    private static final Duration DELAY = Duration.ofSeconds(3);

    private final BukkitScheduler scheduler;
    private final Plugin plugin;
    private final URL url;

    private final BaseComponent[] message;

    public CraftserveListener(BukkitScheduler scheduler, Plugin plugin, URL url) {
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.url = Objects.requireNonNull(url, "url");

        this.message = this.createMessage();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void sendMessage(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        this.scheduler.runTaskLater(this.plugin, () -> {
            if (player.isOnline()) {
                player.spigot().sendMessage(ChatMessageType.SYSTEM, this.message);
            }
        }, DELAY.toMillis() / 50L);
    }

    private BaseComponent[] createMessage() {
        return new ComponentBuilder()
                .append(new ComponentBuilder("Ten serwer korzysta z darmowego pluginu ")
                        .color(COLOR)
                        .create())
                .append(new ComponentBuilder(this.plugin.getName())
                        .color(COLOR)
                        .underlined(true)
                        .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(new ComponentBuilder()
                                .append("Otwórz repozytorium projektu")
                                .color(ChatColor.GRAY)
                                .create())))
                        .event(new ClickEvent(ClickEvent.Action.OPEN_URL, this.url.toString()))
                        .create())
                .append(this.reset()) // this shouldn't even exist...
                .append(new ComponentBuilder(" i ma poprawioną walkę na wersjach gry 1.12+")
                        .color(COLOR)
                        .create())
                .create();
    }

    private BaseComponent[] reset() {
        return new ComponentBuilder("")
                .underlined(false)
                .event(new ClickEvent(ClickEvent.Action.OPEN_URL, ""))
                .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(new BaseComponent[0])))
                .create();
    }
}
