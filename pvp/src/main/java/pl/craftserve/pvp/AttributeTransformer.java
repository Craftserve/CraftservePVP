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
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.InvalidConfigurationException;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public class AttributeTransformer implements Transformer {
    private final Multimap<Attribute, AttributeModifier> attributeModifiers;

    public AttributeTransformer(Multimap<Attribute, AttributeModifier> attributeModifiers) {
        this.attributeModifiers = ImmutableMultimap.copyOf(attributeModifiers);
    }

    public Multimap<Attribute, AttributeModifier> getAttributeModifiers() {
        return this.attributeModifiers;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", AttributeTransformer.class.getSimpleName() + "[", "]")
                .add("attributeModifiers=" + this.attributeModifiers)
                .toString();
    }

    public static AttributeTransformer deserialize(Map<String, Object> map) throws InvalidConfigurationException {
        Object attributeModifiers = map.get("attribute-modifiers");
        if (attributeModifiers == null) {
            return null;
        } else if (!(attributeModifiers instanceof Map<?, ?>)) {
            throw new InvalidConfigurationException("Attribute-modifiers must be a map.");
        }

        ImmutableMultimap.Builder<org.bukkit.attribute.Attribute, AttributeModifier> builder = ImmutableMultimap.builder();

        for (Map.Entry<String, Object> entry : ((Map<String, Object>) attributeModifiers).entrySet()) {
            NamespacedKey key = PvpPlugin.parseKey(entry.getKey());

            org.bukkit.attribute.Attribute attribute = Registry.ATTRIBUTE.get(key);
            if (attribute == null) {
                throw new InvalidConfigurationException("Unknown attribute: " + key);
            }

            Object value = entry.getValue();
            if (!(value instanceof List<?>)) {
                throw new InvalidConfigurationException("Attribute modifiers must be a list.");
            }

            for (Object attributeModifier : (List<Object>) value) {
                if (attributeModifier instanceof Map<?, ?>) {
                    builder.put(attribute, AttributeModifier.deserialize((Map<String, Object>) attributeModifier));
                } else {
                    throw new InvalidConfigurationException("Attribute modifier must be a map.");
                }
            }
        }

        return new AttributeTransformer(builder.build());
    }
}
