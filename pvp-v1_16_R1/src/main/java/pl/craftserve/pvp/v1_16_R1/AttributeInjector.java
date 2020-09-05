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

package pl.craftserve.pvp.v1_16_R1;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.server.v1_16_R1.AttributeBase;
import net.minecraft.server.v1_16_R1.AttributeModifier;
import net.minecraft.server.v1_16_R1.IRegistry;
import net.minecraft.server.v1_16_R1.Item;
import net.minecraft.server.v1_16_R1.ItemArmor;
import net.minecraft.server.v1_16_R1.ItemSword;
import net.minecraft.server.v1_16_R1.ItemTool;
import net.minecraft.server.v1_16_R1.ItemTrident;
import net.minecraft.server.v1_16_R1.MinecraftKey;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Tag;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.v1_16_R1.attribute.CraftAttributeInstance;
import org.bukkit.craftbukkit.v1_16_R1.util.CraftNamespacedKey;
import pl.craftserve.pvp.AttributeTransformer;
import pl.craftserve.pvp.Injector;
import pl.craftserve.pvp.ItemTags;

import java.lang.reflect.Field;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class AttributeInjector implements Injector<AttributeTransformer> {
    static final Logger logger = Logger.getLogger(AttributeInjector.class.getName());

    private final Map<Material, Field> attributeFields;

    private final Field itemArmorArmor;
    private final Field itemArmorArmorToughness;
    private final Field itemArmorKnockbackResistance;
    private final Field itemSwordAttackDamage;
    private final Field itemToolAttackDamage;

    public AttributeInjector() throws NoSuchFieldException {
        this.attributeFields = ImmutableMap.<Material, Field>builder()
                .putAll(install(ItemTags.ARMOR, ItemArmor.class, "m")) // ItemArmor.m
                .putAll(install(ItemTags.AXES, ItemTool.class, "d")) // ItemTool.d
                .putAll(install(ItemTags.HOES, ItemTool.class, "d")) // ItemTool.d
                .putAll(install(ItemTags.PICKAXES, ItemTool.class, "d")) // ItemTool.d
                .putAll(install(ItemTags.SHOVELS, ItemTool.class, "d")) // ItemTool.d
                .putAll(install(ItemTags.SWORDS, ItemSword.class, "b")) //  // ItemSword.b
                .putAll(install(ItemTags.TRIDENTS, ItemTrident.class, "a")) // ItemTrident.a
                .build();

        this.itemArmorArmor = V1_16_R1.install(ItemArmor.class, "k");
        this.itemArmorArmorToughness = V1_16_R1.install(ItemArmor.class, "l");
        this.itemArmorKnockbackResistance = V1_16_R1.install(ItemArmor.class, "c");
        this.itemSwordAttackDamage = V1_16_R1.install(ItemSword.class, "a");
        this.itemToolAttackDamage = V1_16_R1.install(ItemTool.class, "c");
    }

    private static Iterable<Map.Entry<Material, Field>> install(
            Tag<Material> tag, Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Objects.requireNonNull(tag, "tag");
        Objects.requireNonNull(clazz, "clazz");
        Objects.requireNonNull(fieldName, "fieldName");

        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);

        return tag.getValues().stream()
                .map(material -> new AbstractMap.SimpleImmutableEntry<>(material, field))
                .collect(Collectors.toList());
    }

    @Override
    public AttributeTransformer inject(Material material, AttributeTransformer attribute) throws InjectException {
        Objects.requireNonNull(material, "material");
        Objects.requireNonNull(attribute, "attribute");

        NamespacedKey materialKey = material.getKey();
        Item item = V1_16_R1.getItem(materialKey);

        Field field = this.attributeFields.get(material);
        if (field == null) {
            return null;
        }

        logger.log(Level.FINE, "Injecting attribute for " + materialKey + ": " + attribute);

        AttributeTransformer prev = this.ejectSingle(material);
        try {
            field.set(item, this.convertAttributes(attribute));
        } catch (IllegalAccessException e) {
            throw new InjectException("Attribute field for " + materialKey + " is inaccessible.", e);
        }

        Collection<Map.Entry<Attribute, org.bukkit.attribute.AttributeModifier>> entries =
                attribute.getAttributeModifiers().entries();
        double totalAttackDamage = entries.stream()
                .filter(entry -> entry.getKey().equals(Attribute.GENERIC_ATTACK_DAMAGE))
                .mapToDouble(entry -> entry.getValue().getAmount())
                .sum();

        // Special case for ItemArmor, ItemTool and ItemSword.
        // attack_damage is also stored in a local field.
        if (item instanceof ItemArmor) {
            try {
                this.itemArmorArmor.set(item, (int) entries.stream()
                        .filter(entry -> entry.getKey().equals(Attribute.GENERIC_ARMOR))
                        .mapToDouble(entry -> entry.getValue().getAmount())
                        .sum());
                this.itemArmorArmorToughness.set(item, (float) entries.stream()
                        .filter(entry -> entry.getKey().equals(Attribute.GENERIC_ARMOR_TOUGHNESS))
                        .mapToDouble(entry -> entry.getValue().getAmount())
                        .sum());
                this.itemArmorKnockbackResistance.set(item, (float) entries.stream()
                        .filter(entry -> entry.getKey().equals(Attribute.GENERIC_KNOCKBACK_RESISTANCE))
                        .mapToDouble(entry -> entry.getValue().getAmount())
                        .sum());
            } catch (IllegalAccessException e) {
                throw new InjectException("Fields in " + ItemArmor.class.getSimpleName() + " are inaccessible.", e);
            }
        } else if (item instanceof ItemSword) {
            try {
                this.itemSwordAttackDamage.set(item, (float) totalAttackDamage);
            } catch (IllegalAccessException e) {
                throw new InjectException(this.itemSwordAttackDamage.getName() + " field in " +
                        ItemSword.class.getSimpleName() + " is inaccessible.", e);
            }
        } else if (item instanceof ItemTool) {
            try {
                this.itemToolAttackDamage.set(item, (float) totalAttackDamage);
            } catch (IllegalAccessException e) {
                throw new InjectException(this.itemToolAttackDamage.getName() + " field in " +
                        ItemTool.class.getSimpleName() + " is inaccessible.", e);
            }
        }

        return prev;
    }

    @Override
    public Set<AttributeTransformer> eject(Material material) throws InjectException {
        Objects.requireNonNull(material, "material");

        AttributeTransformer attribute = this.ejectSingle(material);
        if (attribute == null) {
            return Collections.emptySet();
        }

        return Collections.singleton(attribute);
    }

    private AttributeTransformer ejectSingle(Material material) throws InjectException {
        Objects.requireNonNull(material, "material");

        NamespacedKey materialKey = material.getKey();
        Item item = V1_16_R1.getItem(materialKey);

        Field field = this.attributeFields.get(material);
        if (field == null) {
            return null;
        }

        Object value;
        try {
            value = field.get(item);
        } catch (IllegalAccessException e) {
            throw new InjectException("Attribute field for " + materialKey + " is inaccessible.", e);
        }

        if (value == null) {
            throw new InjectException("Attribute field for " + materialKey + " is undefined.");
        } else if (!(value instanceof Multimap)) {
            throw new InjectException("Attribute field for " + materialKey + " is not a multimap.");
        }

        return this.convertAttributes((Multimap<AttributeBase, AttributeModifier>) value);
    }

    private Multimap<AttributeBase, AttributeModifier> convertAttributes(AttributeTransformer attribute) {
        Objects.requireNonNull(attribute, "attribute");

        ImmutableMultimap.Builder<AttributeBase, AttributeModifier> builder = ImmutableMultimap.builder();
        attribute.getAttributeModifiers().forEach((bukkitAttribute, bukkitModifier) -> {
            MinecraftKey minecraftKey = CraftNamespacedKey.toMinecraft(bukkitAttribute.getKey());
            AttributeBase nmsAttribute = Objects.requireNonNull(IRegistry.ATTRIBUTE.get(minecraftKey));

            builder.put(nmsAttribute, CraftAttributeInstance.convert(bukkitModifier));
        });

        return builder.build();
    }

    private AttributeTransformer convertAttributes(Multimap<AttributeBase, AttributeModifier> multimap) {
        Objects.requireNonNull(multimap, "multimap");

        ImmutableMultimap.Builder<Attribute, org.bukkit.attribute.AttributeModifier> builder = ImmutableMultimap.builder();
        multimap.forEach((nmsAttribute, nmsModifier) -> {
            MinecraftKey minecraftKey = Objects.requireNonNull(IRegistry.ATTRIBUTE.getKey(nmsAttribute));
            NamespacedKey namespacedKey = CraftNamespacedKey.fromMinecraft(minecraftKey);
            Attribute bukkitAttribute = Objects.requireNonNull(Registry.ATTRIBUTE.get(namespacedKey));

            builder.put(bukkitAttribute, CraftAttributeInstance.convert(nmsModifier));
        });

        return new AttributeTransformer(builder.build());
    }
}
