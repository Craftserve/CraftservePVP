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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import org.bukkit.Material;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class ModifierSession {
    private final AtomicBoolean modified = new AtomicBoolean();
    private Multimap<Material, Transformer> image = ImmutableMultimap.of();

    private final Injector<Transformer> injector;
    private final Multimap<Material, Transformer> transformers;

    public ModifierSession(Injector<Transformer> injector, Multimap<Material, Transformer> transformers) {
        Objects.requireNonNull(injector, "injector");
        Objects.requireNonNull(transformers, "transformers");

        this.injector = injector;
        this.transformers = ImmutableMultimap.copyOf(transformers);
    }

    public void modify() throws Injector.InjectException {
        if (!this.modified.compareAndSet(false, true)) {
            throw new IllegalStateException("Already modified!");
        }

        ImmutableMultimap.Builder<Material, Transformer> image = ImmutableMultimap.builder();
        try {
            for (Map.Entry<Material, Transformer> entry : this.transformers.entries()) {
                image.put(entry.getKey(), this.injector.inject(entry.getKey(), entry.getValue()));
            }
        } finally {
            this.image = image.build();
        }
    }

    public Multimap<Material, Transformer> getImage() {
        return this.image;
    }

    public Multimap<Material, Transformer> getTransformers() {
        return this.transformers;
    }

    public void restore() throws Injector.InjectException {
        if (!this.modified.compareAndSet(true, false)) {
            throw new IllegalStateException("Not modified!");
        }

        Multimap<Material, Transformer> image = ArrayListMultimap.create(this.image);
        try {
            Iterator<Map.Entry<Material, Transformer>> it = image.entries().iterator();
            while (it.hasNext()) {
                Map.Entry<Material, Transformer> entry = it.next();
                Material material = entry.getKey();
                Transformer transformer = entry.getValue();

                this.injector.inject(material, transformer);
                it.remove();
            }
        } finally {
            this.image = ImmutableMultimap.copyOf(image);
        }
    }
}
