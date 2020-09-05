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

import com.google.common.collect.ImmutableList;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

public class FoodTransformer implements Transformer {
    private final Integer foodLevel;
    private final Float saturation;
    private final Boolean wolfEatable;
    private final List<FoodEffect> effects;

    public FoodTransformer(Integer foodLevel, Float saturation, Boolean wolfEatable, List<FoodEffect> effects) {
        this.foodLevel = foodLevel;
        this.saturation = saturation;
        this.wolfEatable = wolfEatable;
        this.effects = effects == null ? null : ImmutableList.copyOf(effects);
    }

    public Integer getFoodLevel() {
        return this.foodLevel;
    }

    public Float getSaturation() {
        return this.saturation;
    }

    public Boolean getWolfEatable() {
        return this.wolfEatable;
    }

    public List<FoodEffect> getEffects() {
        return this.effects;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", FoodTransformer.class.getSimpleName() + "[", "]")
                .add("foodLevel=" + this.foodLevel)
                .add("saturation=" + this.saturation)
                .add("wolfEatable=" + this.wolfEatable)
                .add("effects=" + this.effects)
                .toString();
    }

    public static FoodTransformer deserialize(Map<String, Object> map) throws InvalidConfigurationException {
        Object foodLevelElement = map.get("food-level");
        Object saturationElement = map.get("saturation");
        Object wolfEatableElement = map.get("wolf-eatable");
        Object effectsElement = map.get("effects");

        if (foodLevelElement == null && saturationElement == null &&
                wolfEatableElement == null && effectsElement == null) {
            return null;
        }

        Integer foodLevel = null;
        if (foodLevelElement instanceof Integer) {
            foodLevel = (int) foodLevelElement;
        }

        Float saturation = null;
        if (saturationElement instanceof Number) {
            saturation = ((Number) saturationElement).floatValue();
        }

        Boolean wolfEatable = null;
        if (wolfEatableElement instanceof Boolean) {
            wolfEatable = (boolean) wolfEatableElement;
        }

        List<FoodEffect> effects = null;
        if (effectsElement instanceof List) {
            effects = new ArrayList<>();

            for (Object effect : ((List<?>) effectsElement)) {
                if (effect instanceof Map<?, ?>) {
                    effects.add(FoodEffect.deserialize((Map<String, Object>) effect));
                } else {
                    throw new InvalidConfigurationException("Invalid effect: " + effect);
                }
            }
        }

        return new FoodTransformer(foodLevel, saturation, wolfEatable, effects);
    }

    public static class FoodEffect {
        private final PotionEffect effect;
        private final float chance;

        public FoodEffect(PotionEffect effect, float chance) {
            this.effect = Objects.requireNonNull(effect, "effect");
            this.chance = chance;
        }

        public PotionEffect getEffect() {
            return this.effect;
        }

        public float getChance() {
            return this.chance;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", FoodEffect.class.getSimpleName() + "[", "]")
                    .add("effect=" + this.effect)
                    .add("chance=" + this.chance)
                    .toString();
        }

        @SuppressWarnings("unchecked")
        public static FoodEffect deserialize(Map<String, Object> map) throws InvalidConfigurationException {
            Object effect = map.get("effect");
            if (!(effect instanceof Map)) {
                throw new InvalidConfigurationException("Missing effect element.");
            }

            Object chance = map.get("chance");
            if (!(chance instanceof Number)) {
                throw new InvalidConfigurationException("Missing chance element.");
            }

            Map<String, Object> effectValue = (Map<String, Object>) effect;
            float chanceValue = ((Number) chance).floatValue();
            return new FoodEffect(new PotionEffect(effectValue), chanceValue);
        }
    }
}
