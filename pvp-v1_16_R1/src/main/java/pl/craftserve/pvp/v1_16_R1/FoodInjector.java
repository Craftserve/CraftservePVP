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

import com.mojang.datafixers.util.Pair;
import net.minecraft.server.v1_16_R1.FoodInfo;
import net.minecraft.server.v1_16_R1.Item;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.v1_16_R1.potion.CraftPotionUtil;
import pl.craftserve.pvp.FoodTransformer;
import pl.craftserve.pvp.Injector;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class FoodInjector implements Injector<FoodTransformer> {
    static final Logger logger = Logger.getLogger(FoodInjector.class.getName());

    private final Field foodLevelField;
    private final Field saturationField;
    private final Field wolfEatableField;
    private final Field effectsField;

    public FoodInjector() throws NoSuchFieldException {
        this.foodLevelField = V1_16_R1.install(FoodInfo.class, "a");
        this.saturationField = V1_16_R1.install(FoodInfo.class, "b");
        this.wolfEatableField = V1_16_R1.install(FoodInfo.class, "c");
        this.effectsField = V1_16_R1.install(FoodInfo.class, "f");
    }

    @Override
    public FoodTransformer inject(Material material, FoodTransformer food) throws InjectException {
        Objects.requireNonNull(material, "material");
        Objects.requireNonNull(food, "food");

        NamespacedKey materialKey = material.getKey();
        Item item = V1_16_R1.getItem(materialKey);

        FoodInfo foodInfo = item.getFoodInfo();
        if (foodInfo == null) {
            return null;
        }

        logger.log(Level.FINE, "Injecting food properties for " + materialKey + ": " + food);

        FoodTransformer prev = this.convertFood(foodInfo);

        Integer foodLevel = food.getFoodLevel();
        if (foodLevel != null) {
            try {
                this.foodLevelField.set(foodInfo, foodLevel);
            } catch (IllegalAccessException e) {
                throw new InjectException("Food level field for " + materialKey + " is inaccessible.", e);
            }
        }

        Float saturation = food.getSaturation();
        if (saturation != null) {
            try {
                this.saturationField.set(foodInfo, saturation);
            } catch (IllegalAccessException e) {
                throw new InjectException("Saturation level field for " + materialKey + " is inaccessible.", e);
            }
        }

        Boolean wolfEatable = food.getWolfEatable();
        if (wolfEatable != null) {
            try {
                this.wolfEatableField.set(foodInfo, wolfEatable);
            } catch (IllegalAccessException e) {
                throw new InjectException("Wolf eatable field for " + materialKey + " is inaccessible.", e);
            }
        }

        List<FoodTransformer.FoodEffect> effects = food.getEffects();
        if (effects != null) {
            try {
                this.effectsField.set(foodInfo, effects.stream()
                        .map(effect -> Pair.of(CraftPotionUtil.fromBukkit(effect.getEffect()), effect.getChance()))
                        .collect(Collectors.toList()));
            } catch (IllegalAccessException e) {
                throw new InjectException("Effects field for " + materialKey + " is inaccessible.", e);
            }
        }

        return prev;
    }

    @Override
    public Set<FoodTransformer> eject(Material material) throws InjectException {
        Objects.requireNonNull(material, "material");

        FoodInfo foodInfo = V1_16_R1.getItem(material.getKey()).getFoodInfo();
        if (foodInfo == null) {
            return Collections.emptySet();
        }

        return Collections.singleton(this.convertFood( foodInfo));
    }

    private FoodTransformer convertFood(FoodInfo foodInfo) {
        Objects.requireNonNull(foodInfo, "foodInfo");

        int foodLevel = foodInfo.getNutrition();
        float saturation = foodInfo.getSaturationModifier();
        boolean wolfEatable = foodInfo.c();

        List<FoodTransformer.FoodEffect> effects = foodInfo.f().stream()
                .map(pair -> new FoodTransformer.FoodEffect(CraftPotionUtil.toBukkit(pair.getFirst()), pair.getSecond()))
                .collect(Collectors.toList());

        return new FoodTransformer(foodLevel, saturation, wolfEatable, effects);
    }
}
