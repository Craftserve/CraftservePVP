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

import com.google.common.collect.ImmutableSet;
import org.bukkit.Material;

import java.util.Objects;
import java.util.Set;

public interface Injector<T extends Transformer> {
    T inject(Material material, T t) throws InjectException;

    Set<T> eject(Material material) throws InjectException;

    class InjectException extends Exception {
        public InjectException() {
        }

        public InjectException(String message) {
            super(message);
        }

        public InjectException(String message, Throwable cause) {
            super(message, cause);
        }

        public InjectException(Throwable cause) {
            super(cause);
        }
    }

    class Impl implements Injector<Transformer> {
        private final Injector<AttributeTransformer> attributeInjector;
        private final Injector<FoodTransformer> foodInjector;

        public Impl(Injector<AttributeTransformer> attributeInjector,
                    Injector<FoodTransformer> foodInjector) {
            this.attributeInjector = Objects.requireNonNull(attributeInjector, "attributeInjector");
            this.foodInjector = Objects.requireNonNull(foodInjector, "foodInjector");
        }

        @Override
        public Transformer inject(Material material, Transformer transformer) throws InjectException {
            Objects.requireNonNull(material, "material");
            Objects.requireNonNull(transformer, "transformer");

            Transformer prev;
            if (transformer instanceof AttributeTransformer) {
                prev = this.attributeInjector.inject(material, (AttributeTransformer) transformer);
            } else if (transformer instanceof FoodTransformer) {
                prev = this.foodInjector.inject(material, (FoodTransformer) transformer);
            } else {
                throw new InjectException("Unsupported transformer: " + transformer.getClass());
            }

            return prev;
        }

        @Override
        public Set<Transformer> eject(Material material) throws InjectException {
            Objects.requireNonNull(material, "material");

            return ImmutableSet.<Transformer>builder()
                    .addAll(this.attributeInjector.eject(material))
                    .addAll(this.foodInjector.eject(material))
                    .build();
        }
    }
}
