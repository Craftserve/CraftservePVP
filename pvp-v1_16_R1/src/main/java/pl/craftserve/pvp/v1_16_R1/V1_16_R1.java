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

import net.minecraft.server.v1_16_R1.IRegistry;
import net.minecraft.server.v1_16_R1.Item;
import net.minecraft.server.v1_16_R1.MinecraftKey;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.v1_16_R1.util.CraftNamespacedKey;
import pl.craftserve.pvp.Injector;

import java.lang.reflect.Field;
import java.util.Objects;

public class V1_16_R1 extends Injector.Impl {
    public V1_16_R1() throws NoSuchFieldException {
        super(new AttributeInjector(), new FoodInjector());
    }

    static Item getItem(NamespacedKey namespacedKey) throws InjectException {
        Objects.requireNonNull(namespacedKey, "namespacedKey");

        MinecraftKey minecraftKey = CraftNamespacedKey.toMinecraft(namespacedKey);
        return IRegistry.ITEM.getOptional(minecraftKey).orElseThrow(() -> {
            return new InjectException(namespacedKey + " is not an item.");
        });
    }

    static Field install(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Objects.requireNonNull(clazz, "clazz");
        Objects.requireNonNull(fieldName, "fieldName");

        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field;
    }
}
