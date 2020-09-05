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

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.io.Closer;
import com.google.gson.JsonParser;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Server;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import pl.craftserve.metrics.pluginmetricslite.MetricsLite;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class PvpPlugin extends JavaPlugin {
    static final Logger logger = Logger.getLogger(PvpPlugin.class.getName());

    private static final long DAT_PROTOCOL_VERSION = 2721351624263755569L;

    private static final String REPOSITORY_OWNER = "Craftserve";
    private static final String REPOSITORY_NAME = "CraftservePVP";
    private static final URL REPOSITORY_URL;

    static {
        try {
            REPOSITORY_URL = new URL("https://github.com/" + REPOSITORY_OWNER + "/" + REPOSITORY_NAME);
        } catch (MalformedURLException e) {
            throw new Error(e);
        }
    }

    private Injector<Transformer> injector;
    private ModifierSession session;
    private Updater updater;

    @Override
    public void onEnable() {
        Server server = this.getServer();
        String serverVersion = this.getServerVersion(server);
        String fullServerVersion = server.getVersion();

        PluginManager pluginManager = server.getPluginManager();
        BukkitScheduler scheduler = server.getScheduler();

        try {
            this.injector = this.createInjector(serverVersion, this.getClass().getPackage());
        } catch (ClassNotFoundException e) {
            logger.log(Level.SEVERE, "Your server version or implementation (" + fullServerVersion + ") is unsupported.", e);
            this.setEnabled(false);
            return;
        } catch (ReflectiveOperationException e) {
            logger.log(Level.SEVERE, "Could not create injector for " + fullServerVersion, e);
            this.setEnabled(false);
            return;
        }

        logger.log(Level.INFO, "Using " + serverVersion + " injector.");

        Instant loadInstant = Instant.now();
        Multimap<Material, Transformer> transformers;

        try {
            transformers = this.readJarDatFile(serverVersion + ".dat");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Could not load transformers.", e);
            transformers = ImmutableMultimap.of();
        }

        Duration loadDuration = Duration.between(loadInstant, Instant.now());
        logger.info("Loaded " + transformers.size() + " transformer(s) for " + transformers.keySet().size() +
                " material(s), took " + loadDuration.toMillis() / 1000F + "s.");

        if (!transformers.isEmpty()) {
            Instant injectInstant = Instant.now();
            try {
                this.modifyServer(transformers);
            } catch (Injector.InjectException e) {
                logger.log(Level.SEVERE, "Could not inject transformers.", e);
                this.setEnabled(false);
                return;
            }

            Duration injectDuration = Duration.between(injectInstant, Instant.now());
            logger.info("Injected, took " + injectDuration.toMillis() / 1000F + "s.");

            pluginManager.registerEvents(new CraftserveListener(scheduler, this, REPOSITORY_URL), this);
        }

        JsonParser jsonParser = new JsonParser();

        this.updater = new Updater(scheduler, this, jsonParser, REPOSITORY_OWNER, REPOSITORY_NAME);
        this.updater.start();
        pluginManager.registerEvents(this.updater, this);

        MetricsLite.start(this);
    }

    @Override
    public void onDisable() {
        MetricsLite.stopIfRunning(this);

        if (this.updater != null) {
            this.updater.stop();
            this.updater = null;
        }

        if (this.isModified()) {
            try {
                this.restoreServer();
            } catch (Injector.InjectException e) {
                logger.log(Level.SEVERE, "Could not restore the server to previous values.", e);
            }
        }
    }

    public Injector<Transformer> getInjector() {
        return this.injector;
    }

    public Optional<ModifierSession> getSession() {
        return Optional.ofNullable(this.session);
    }

    public void modifyServer(Multimap<Material, Transformer> transformers) throws Injector.InjectException {
        Objects.requireNonNull(transformers, "transformers");

        if (this.session != null) {
            throw new IllegalStateException("Server is already modified.");
        }

        ModifierSession session = new ModifierSession(this.injector, transformers);
        try {
            session.modify();
        } finally {
            this.session = session;
        }
    }

    public boolean isModified() {
        return this.session != null;
    }

    public void restoreServer() throws Injector.InjectException {
        if (this.session == null) {
            throw new IllegalStateException("Server is not modified.");
        }

        try {
            this.session.restore();
        } finally {
            this.session = null;
        }
    }

    public Multimap<Material, Transformer> deserialize(Map<String, Object> transformers) throws InvalidConfigurationException {
        Objects.requireNonNull(transformers, "transformers");

        ImmutableMultimap.Builder<Material, Transformer> builder = ImmutableMultimap.builder();
        for (Map.Entry<String, Object> entry : transformers.entrySet()) {
            NamespacedKey key = parseKey(entry.getKey());

            Material material = Registry.MATERIAL.get(key);
            if (material == null) {
                throw new InvalidConfigurationException("Invalid material: " + key);
            }

            Object value = entry.getValue();
            if (!(value instanceof List<?>)) {
                throw new InvalidConfigurationException("Transformers must be a list");
            }

            for (Object transformer : (List<?>) value) {
                if (transformer instanceof Map<?, ?>) {
                    Map<String, Object> map = (Map<String, Object>) transformer;

                    AttributeTransformer attributeTransformer = AttributeTransformer.deserialize(map);
                    if (attributeTransformer != null) {
                        builder.put(material, attributeTransformer);
                    }

                    FoodTransformer foodTransformer = FoodTransformer.deserialize(map);
                    if (foodTransformer != null) {
                        builder.put(material, foodTransformer);
                    }
                } else {
                    throw new InvalidConfigurationException("Transformer must be a map.");
                }
            }
        }

        return builder.build();
    }

    private String getServerVersion(Server server) {
        Objects.requireNonNull(server, "server");

        Package rootPackage = server.getClass().getPackage();
        String[] parts = rootPackage.getName().split("\\.");
        return parts[parts.length - 1];
    }

    private Injector<Transformer> createInjector(String serverVersion, Package from) throws ReflectiveOperationException {
        Objects.requireNonNull(serverVersion, "serverVersion");
        Objects.requireNonNull(from, "from");

        Class<?> clazz = Class.forName(String.format("%s.%s.%s",
                from.getName(),
                serverVersion,
                serverVersion.toUpperCase()
        ));

        if (!Injector.class.isAssignableFrom(clazz)) {
            throw new ReflectiveOperationException(clazz + " is not an instance of " + Injector.class);
        }

        Constructor<?> constructor = clazz.getDeclaredConstructor();
        constructor.setAccessible(true);

        return (Injector<Transformer>) constructor.newInstance();
    }

    private Multimap<Material, Transformer> readJarDatFile(String filename) throws IOException {
        Objects.requireNonNull(filename, "filename");
        ClassLoader classLoader = this.getClass().getClassLoader();

        Object transformers;
        try (Closer closer = Closer.create()) {
            InputStream inputStream = closer.register(classLoader.getResourceAsStream(filename));
            if (inputStream == null) {
                throw new FileNotFoundException("Missing " + filename + " file.");
            }

            BufferedInputStream bufferedInputStream = closer.register(new BufferedInputStream(inputStream));
            ObjectInputStream objectInputStream = closer.register(new ObjectInputStream(bufferedInputStream));

            long protocolVersion = objectInputStream.readLong();
            if (protocolVersion != DAT_PROTOCOL_VERSION) {
                throw new IOException("Unsupported protocol version: " + protocolVersion);
            }

            try {
                transformers = objectInputStream.readObject();
            } catch (ClassNotFoundException e) {
                throw new IOException("Required class wasn't found.", e);
            }
        }

        if (!(transformers instanceof Map<?, ?>)) {
            throw new IOException("Root transformers object is not a map.");
        }

        try {
            return this.deserialize((Map<String, Object>) transformers);
        } catch (InvalidConfigurationException e) {
            throw new IOException("Could not deserialize transformers.", e);
        }
    }

    public static NamespacedKey parseKey(String input) throws InvalidConfigurationException {
        Objects.requireNonNull(input, "input");

        int separator = input.indexOf(':');
        if (separator == -1) {
            throw new InvalidConfigurationException("Missing namespace and key separator in: " + input);
        }

        String namespace = input.substring(0, separator);
        String key = input.substring(separator + 1);
        return new NamespacedKey(namespace, key);
    }
}
