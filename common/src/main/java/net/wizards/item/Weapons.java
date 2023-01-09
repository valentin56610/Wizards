package net.wizards.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.*;
import net.minecraft.recipe.Ingredient;
import net.minecraft.util.Identifier;
import net.minecraft.util.Lazy;
import net.minecraft.util.registry.Registry;
import net.spell_power.api.MagicSchool;
import net.spell_power.api.attributes.Attributes;
import net.wizards.WizardsMod;
import net.wizards.config.ItemConfig;
import net.wizards.item.weapon.StaffItem;

import java.util.ArrayList;
import java.util.UUID;
import java.util.function.Supplier;

public class Weapons {

    public record Entry(String name, Material material, ItemConfig.Weapon defaults) {
        public Identifier id() {
            return new Identifier(WizardsMod.ID, name);
        }
        public Entry add(ItemConfig.SpellAttribute attribute) {
            defaults.add(attribute);
            return this;
        }
    }

    private static final ArrayList<Entry> entries = new ArrayList<>();
    private static Entry entry(String name, Material material, ItemConfig.Weapon defaults) {
        var entry = new Entry(name, material, defaults);
        entries.add(entry);
        return entry;
    }

    // MARK: Material

    public static class Material implements ToolMaterial {
        public static Material matching(ToolMaterials vanillaMaterial, Supplier repairIngredient) {
            var material = new Material();
            material.miningLevel = vanillaMaterial.getMiningLevel();
            material.durability = vanillaMaterial.getDurability();
            material.miningSpeed = vanillaMaterial.getMiningSpeedMultiplier();
            material.enchantability = vanillaMaterial.getEnchantability();
            material.ingredient = new Lazy(repairIngredient);
            return material;
        }

        private int miningLevel = 0;
        private int durability = 0;
        private float miningSpeed = 0;
        private int enchantability = 0;
        private Lazy<Ingredient> ingredient = null;

        @Override
        public int getDurability() {
            return durability;
        }

        @Override
        public float getMiningSpeedMultiplier() {
            return miningSpeed;
        }

        @Override
        public float getAttackDamage() {
            return 0;
        }

        @Override
        public int getMiningLevel() {
            return miningLevel;
        }

        @Override
        public int getEnchantability() {
            return enchantability;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return (Ingredient)this.ingredient.get();
        }
    }

    // MARK: Wands

    private static final float wandAttackDamage = 2;
    private static final float wandAttackSpeed = -2.4F;
    private static Entry wand(String name, Material material) {
        return entry(name, material, new ItemConfig.Weapon(wandAttackDamage, wandAttackSpeed));
    }

    public static final Entry noviceWand = wand("wand_novice",
            Material.matching(ToolMaterials.WOOD, () -> Ingredient.ofItems(Items.STICK)))
            .add(ItemConfig.SpellAttribute.bonus(Attributes.POWER.get(MagicSchool.ARCANE), 1));
    public static final Entry arcaneWand = wand("wand_arcane",
            Material.matching(ToolMaterials.IRON, () -> Ingredient.ofItems(Items.GOLD_INGOT)))
            .add(ItemConfig.SpellAttribute.bonus(Attributes.POWER.get(MagicSchool.ARCANE), 2));
    public static final Entry fireWand = wand("wand_fire",
            Material.matching(ToolMaterials.IRON, () -> Ingredient.ofItems(Items.GOLD_INGOT)))
            .add(ItemConfig.SpellAttribute.bonus(Attributes.POWER.get(MagicSchool.FIRE), 2));
    public static final Entry frostWand = wand("wand_frost",
            Material.matching(ToolMaterials.IRON, () -> Ingredient.ofItems(Items.IRON_INGOT)))
            .add(ItemConfig.SpellAttribute.bonus(Attributes.POWER.get(MagicSchool.FROST), 2));

    // MARK: Staves

    private static final float staffAttackDamage = 4;
    private static final float staffAttackSpeed = -3F;
    private static Entry staff(String name, Material material) {
        return entry(name, material, new ItemConfig.Weapon(staffAttackDamage, staffAttackSpeed));
    }

    public static final Entry arcaneStaff = staff("staff_arcane",
            Material.matching(ToolMaterials.IRON, () -> Ingredient.ofItems(Items.GOLD_INGOT)))
            .add(ItemConfig.SpellAttribute.bonus(Attributes.POWER.get(MagicSchool.ARCANE), 4));
    public static final Entry fireStaff = staff("staff_fire",
            Material.matching(ToolMaterials.IRON, () -> Ingredient.ofItems(Items.GOLD_INGOT)))
            .add(ItemConfig.SpellAttribute.bonus(Attributes.POWER.get(MagicSchool.FIRE), 4));
    public static final Entry frostStaff = staff("staff_frost",
            Material.matching(ToolMaterials.IRON, () -> Ingredient.ofItems(Items.IRON_INGOT)))
            .add(ItemConfig.SpellAttribute.bonus(Attributes.POWER.get(MagicSchool.FROST), 4));


    // MARK: Register

    public static void register() {
        for(var entry: entries) {
            var config = entry.defaults; // TODO get from config
            var settings = new Item.Settings().group(Group.WIZARDS);
            var item = new StaffItem(entry.material(), attributesFrom(config), settings);
            Registry.register(Registry.ITEM, entry.id(), item);
        }
    }

    private static Multimap<EntityAttribute, EntityAttributeModifier> attributesFrom(ItemConfig.Weapon config) {
        ImmutableMultimap.Builder<EntityAttribute, EntityAttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(EntityAttributes.GENERIC_ATTACK_DAMAGE,
                new EntityAttributeModifier(
                        ItemAccessor.ATTACK_DAMAGE_MODIFIER_ID(),
                        "Weapon modifier",
                        config.attack_damage,
                        EntityAttributeModifier.Operation.ADDITION));
        builder.put(EntityAttributes.GENERIC_ATTACK_SPEED,
                new EntityAttributeModifier(
                        ItemAccessor.ATTACK_SPEED_MODIFIER_ID(),
                        "Weapon modifier",
                        config.attack_speed,
                        EntityAttributeModifier.Operation.ADDITION));
        for(var attribute: config.spell_attributes) {
            try {
                builder.put(Attributes.all.get(attribute.name).attribute,
                        new EntityAttributeModifier(
                                "Weapon modifier",
                                attribute.value,
                                attribute.operation));
            } catch (Exception e) {
                System.err.println("Failed to add item attribute modifier: " + e.getMessage());
            }
        }
        return builder.build();
    }

    private static abstract class ItemAccessor extends Item {
        public ItemAccessor(Settings settings) { super(settings); }
        public static final UUID ATTACK_DAMAGE_MODIFIER_ID() { return ATTACK_DAMAGE_MODIFIER_ID; }
        public static final UUID ATTACK_SPEED_MODIFIER_ID() { return ATTACK_SPEED_MODIFIER_ID; }
    }
}