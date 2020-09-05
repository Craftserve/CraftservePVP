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
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;

import java.util.Objects;
import java.util.Set;

public interface ItemTags {
    Tag<Material> ARMOR = create("armor",
            Material.LEATHER_HELMET,
            Material.LEATHER_CHESTPLATE,
            Material.LEATHER_LEGGINGS,
            Material.LEATHER_BOOTS,

            Material.GOLDEN_HELMET,
            Material.GOLDEN_CHESTPLATE,
            Material.GOLDEN_LEGGINGS,
            Material.GOLDEN_BOOTS,

            Material.CHAINMAIL_HELMET,
            Material.CHAINMAIL_CHESTPLATE,
            Material.CHAINMAIL_LEGGINGS,
            Material.CHAINMAIL_BOOTS,

            Material.IRON_HELMET,
            Material.IRON_CHESTPLATE,
            Material.IRON_LEGGINGS,
            Material.IRON_BOOTS,

            Material.DIAMOND_HELMET,
            Material.DIAMOND_CHESTPLATE,
            Material.DIAMOND_LEGGINGS,
            Material.DIAMOND_BOOTS,

            Material.NETHERITE_HELMET,
            Material.NETHERITE_CHESTPLATE,
            Material.NETHERITE_LEGGINGS,
            Material.NETHERITE_BOOTS,

            Material.TURTLE_HELMET
    );

    Tag<Material> AXES = create("axes",
            Material.WOODEN_AXE,
            Material.GOLDEN_AXE,
            Material.STONE_AXE,
            Material.IRON_AXE,
            Material.DIAMOND_AXE,
            Material.NETHERITE_AXE
    );

    Tag<Material> HOES = create("hoes",
            Material.WOODEN_HOE,
            Material.GOLDEN_HOE,
            Material.STONE_HOE,
            Material.IRON_HOE,
            Material.DIAMOND_HOE,
            Material.NETHERITE_HOE
    );

    Tag<Material> PICKAXES = create("pickaxes",
            Material.WOODEN_PICKAXE,
            Material.GOLDEN_PICKAXE,
            Material.STONE_PICKAXE,
            Material.IRON_PICKAXE,
            Material.DIAMOND_PICKAXE,
            Material.NETHERITE_PICKAXE
    );

    Tag<Material> SHOVELS = create("shovels",
            Material.WOODEN_SHOVEL,
            Material.GOLDEN_SHOVEL,
            Material.STONE_SHOVEL,
            Material.IRON_SHOVEL,
            Material.DIAMOND_SHOVEL,
            Material.NETHERITE_SHOVEL
    );

    Tag<Material> SWORDS = create("swords",
            Material.WOODEN_SWORD,
            Material.GOLDEN_SWORD,
            Material.STONE_SWORD,
            Material.IRON_SWORD,
            Material.DIAMOND_SWORD,
            Material.NETHERITE_SWORD
    );

    Tag<Material> TRIDENTS = create("tridents",
            Material.TRIDENT
    );

    static <T extends Keyed> Tag<T> create(String key, T... values) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(values, "values");

        NamespacedKey namespacedKey = NamespacedKey.minecraft(key);
        ImmutableSet<T> set = ImmutableSet.copyOf(values);

        return new Tag<T>() {
            @Override
            public boolean isTagged(T t) {
                return set.contains(Objects.requireNonNull(t, "t"));
            }

            @Override
            public Set<T> getValues() {
                return set;
            }

            @Override
            public NamespacedKey getKey() {
                return namespacedKey;
            }
        };
    }
}
