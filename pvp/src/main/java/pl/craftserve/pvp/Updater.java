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

import com.google.common.io.Closer;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
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
import org.bukkit.scheduler.BukkitTask;

import javax.net.ssl.HttpsURLConnection;
import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Updater implements Listener {
    static final Logger logger = Logger.getLogger(Updater.class.getName());

    private static final ChatColor COLOR = ChatColor.of(new Color(92, 184, 92));
    private static final Duration INTERVAL = Duration.ofHours(1);
    private static final Duration NOTIFICATION_DELAY = Duration.ofSeconds(15);

    private final BukkitScheduler scheduler;
    private final Plugin plugin;
    private final JsonParser jsonParser;

    private final String repositoryOwner;
    private final String repositoryName;

    private BukkitTask updaterTask;
    private Resource resource;

    public Updater(BukkitScheduler scheduler, Plugin plugin, JsonParser jsonParser,
                   String repositoryOwner, String repositoryName) {
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.jsonParser = Objects.requireNonNull(jsonParser, "jsonParser");

        this.repositoryOwner = Objects.requireNonNull(repositoryOwner, "repositoryOwner");
        this.repositoryName = Objects.requireNonNull(repositoryName, "repositoryName");
    }

    public void start() {
        this.updaterTask = this.scheduler.runTaskTimerAsynchronously(
                this.plugin, this::checkForUpdates, 1L, INTERVAL.toMillis() / 50L);
    }

    public void stop() {
        if (this.updaterTask != null) {
            this.updaterTask.cancel();
            this.updaterTask = null;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void sendNotification(PlayerJoinEvent event) {
        Resource resource = this.resource;
        if (resource == null) {
            return;
        }

        String currentVersion = this.plugin.getDescription().getVersion();
        if (resource.test(currentVersion)) {
            // already up to date
            return;
        }

        Player player = event.getPlayer();
        if (!player.hasPermission("craftservepvp.update")) {
            return;
        }

        this.scheduler.runTaskLater(this.plugin, () -> {
            if (player.isOnline()) {
                Player.Spigot spigot = player.spigot();

                this.createNotification(resource, currentVersion).forEach(components -> {
                    spigot.sendMessage(ChatMessageType.SYSTEM, components);
                });
            }
        }, NOTIFICATION_DELAY.toMillis() / 50L);
    }

    private Iterable<BaseComponent[]> createNotification(Resource resource, String currentVersion) {
        Objects.requireNonNull(resource, "resource");
        String publishedAt = resource.publishedAt.format(DateTimeFormatter.ISO_DATE);

        return Arrays.asList(
                new ComponentBuilder()
                        .append("You are running an outdated version of " + this.plugin.getName())
                        .color(COLOR)
                        .create(),
                new ComponentBuilder()
                        .append("Your version: " + currentVersion)
                        .color(COLOR)
                        .create(),
                new ComponentBuilder()
                        .append("Newest version: " + resource.version + ", published " + publishedAt)
                        .color(COLOR)
                        .create(),
                new ComponentBuilder()
                        .append(new ComponentBuilder("Download: ")
                                .color(COLOR)
                                .create())
                        .append(new ComponentBuilder(resource.htmlUrl.toString())
                                .color(COLOR)
                                .underlined(true)
                                .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(new ComponentBuilder()
                                        .append("Open project download")
                                        .color(ChatColor.GRAY)
                                        .create())))
                                .event(new ClickEvent(ClickEvent.Action.OPEN_URL, resource.htmlUrl.toString()))
                                .create())
                        .create()
        );
    }

    private void checkForUpdates() {
        URL url;
        try {
            url = new URL(String.format("https://api.github.com/repos/%s/%s/releases/latest",
                    this.repositoryOwner, this.repositoryName));
        } catch (MalformedURLException e) {
            logger.log(Level.SEVERE, "Invalid URL for updater, won't check for updates. :(", e);
            return;
        }

        String jsonString;
        try (InputStream inputStream = this.requestReleaseJson(url)) {
            jsonString = this.inputStreamToString(inputStream);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "An I/O exception occurred while checking for updates.", e);
            return;
        }

        JsonElement rootElement;
        try {
            rootElement = this.jsonParser.parse(jsonString);
        } catch (JsonIOException e) {
            logger.log(Level.SEVERE, "An I/O exception occurred while parsing JSON response.", e);
            return;
        } catch (JsonSyntaxException e) {
            logger.log(Level.SEVERE, "Invalid JSON syntax in JSON response.", e);
            return;
        }

        JsonObject rootObject;
        if (rootElement instanceof JsonObject) {
            rootObject = (JsonObject) rootElement;
        } else {
            logger.log(Level.SEVERE, "Root element is not an object.");
            return;
        }

        Resource resource;
        try {
            resource = Resource.create(rootObject);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Could not create resource from JSON response.", e);
            return;
        }

        this.resource = resource;

        if (resource.test(this.plugin.getDescription().getVersion())) {
            logger.info(this.plugin.getName() + " is up to date.");
        } else {
            logger.warning("A new update for " + this.plugin.getName() +
                    " is available. Download it from: " + resource.htmlUrl.toString());
        }
    }

    private InputStream requestReleaseJson(URL url) throws IOException {
        Objects.requireNonNull(url, "url");

        URLConnection urlConnection = url.openConnection();
        if (!(urlConnection instanceof HttpsURLConnection)) {
            throw new IOException("Connection is not an instance of " + HttpsURLConnection.class);
        }

        HttpsURLConnection connection = (HttpsURLConnection) urlConnection;
        connection.setDoInput(true);

        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/vnd.github.v3+json");

        do {
            try {
                connection.connect();
                break;
            } catch (SocketTimeoutException e) {
                // try again
            }
        } while (true);

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpsURLConnection.HTTP_OK) {
            throw new IOException("Request returned " + responseCode + ", " + HttpsURLConnection.HTTP_OK + " was expected.");
        }

        return connection.getInputStream();
    }

    private String inputStreamToString(InputStream inputStream) throws IOException {
        Objects.requireNonNull(inputStream, "inputStream");

        try (Closer closer = Closer.create()) {
            InputStreamReader inputStreamReader = closer.register(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            BufferedReader bufferedReader = closer.register(new BufferedReader(inputStreamReader));

            StringBuilder jsonBuilder = new StringBuilder();
            do {
                String line = bufferedReader.readLine();
                if (line == null) {
                    break;
                }

                jsonBuilder.append(line);
            } while (true);

            return jsonBuilder.toString();
        }
    }

    private static class Resource implements Predicate<String> {
        final String version;
        final LocalDateTime publishedAt;
        final URL htmlUrl;

        private Resource(String version, LocalDateTime publishedAt, URL htmlUrl) {
            this.version = Objects.requireNonNull(version, "version");
            this.publishedAt = Objects.requireNonNull(publishedAt, "publishedAt");
            this.htmlUrl = Objects.requireNonNull(htmlUrl, "htmlUrl");
        }

        @Override
        public boolean test(String string) {
            Objects.requireNonNull(string, "string");
            return this.version.equalsIgnoreCase(string.trim());
        }

        public static Resource create(JsonObject object) throws IOException {
            Objects.requireNonNull(object, "object");

            JsonElement nameElement = object.get("name");
            if (nameElement == null || !nameElement.isJsonPrimitive()) {
                throw new IOException("Missing name string.");
            }
            String name = nameElement.getAsString();

            JsonElement publishedAtElement = object.get("published_at");
            if (publishedAtElement == null || !publishedAtElement.isJsonPrimitive()) {
                throw new IOException("Missing published_at string.");
            }

            JsonElement htmlUrlElement = object.get("html_url");
            if (htmlUrlElement == null || !htmlUrlElement.isJsonPrimitive()) {
                throw new IOException("Missing html_url string.");
            }

            String version;
            if (name.length() >= 1 && name.startsWith("v")) {
                version = name.substring(1);
            } else {
                throw new IOException("Invalid version: " + name);
            }

            LocalDateTime publishedAt;
            try {
                publishedAt = LocalDateTime.parse(publishedAtElement.getAsString(), DateTimeFormatter.ISO_DATE_TIME);
            } catch (DateTimeParseException e) {
                throw new IOException("Invalid date format for published_at.", e);
            }

            URL htmlUrl;
            try {
                htmlUrl = new URL(htmlUrlElement.getAsString());
            } catch (MalformedURLException e) {
                throw new IOException("Invalid URL for html_url.", e);
            }

            return new Resource(version, publishedAt, htmlUrl);
        }
    }
}
