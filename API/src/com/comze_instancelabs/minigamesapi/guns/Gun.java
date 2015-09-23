package com.comze_instancelabs.minigamesapi.guns;

import org.bukkit.Bukkit;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;

public class Gun {

    public double speed = 1D; // the higher the faster
    public int shoot_amount = 1;
    public int max_durability = 50;
    public int durability = 50;
    public Class<? extends Projectile> bullet = Egg.class;
    public JavaPlugin plugin;
    public double knockback_multiplier = 1.1D;
    public String name = "Gun";
    public HashMap<String, Boolean> canshoot_ = new HashMap<String, Boolean>();
    boolean canshoot = true;
    ArrayList<ItemStack> items;
    ArrayList<ItemStack> icon;

    public Gun(JavaPlugin plugin, String name, double speed, int shoot_amount, int durability, double knockback_multiplier, Class<? extends Projectile> bullet, ArrayList<ItemStack> items, ArrayList<ItemStack> icon) {
        this.plugin = plugin;
        this.name = name;
        this.speed = speed;
        this.shoot_amount = shoot_amount;
        this.durability = durability;
        this.max_durability = durability;
        this.bullet = bullet;
        this.knockback_multiplier = knockback_multiplier;
        this.items = items;
        this.icon = icon;
        if (name.equalsIgnoreCase("grenade")) {
            this.bullet = Snowball.class;
        }
    }

    public Gun(JavaPlugin plugin, ArrayList<ItemStack> items, ArrayList<ItemStack> icon) {
        this(plugin, "Gun", 1D, 1, 50, 1.1D, Egg.class, items, icon);
    }

    public void shoot(Player p) {
        if (canshoot) {
            for (int i = 0; i < shoot_amount; i++) {
                p.launchProjectile(bullet);
                this.durability -= 1;
            }
            canshoot = false;
            Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                public void run() {
                    canshoot = true;
                }
            }, (long) (20D / speed));
        }
    }

    public void shoot(final Player p, int shoot_amount, int durability, int speed) {
        if (!canshoot_.containsKey(p.getName())) {
            canshoot_.put(p.getName(), true);
        }
        if (canshoot_.get(p.getName())) {
            for (int i = 0; i < shoot_amount + 1; i++) {
                p.launchProjectile(bullet);
                this.durability -= (int) (10D / durability);
            }
            canshoot_.put(p.getName(), false);
            Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                public void run() {
                    canshoot_.put(p.getName(), true);
                }
            }, (long) (60D / speed));
        }
    }

    public void onHit(Entity ent, int knockback_multiplier) {
        if (this.name.equalsIgnoreCase("freeze")) {
            final Player p = (Player) ent;
            p.setWalkSpeed(0.0F);
            Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                public void run() {
                    p.setWalkSpeed(0.2F);
                }
            }, 20L + 20L * knockback_multiplier);
        } else {
            ent.setVelocity(ent.getLocation().getDirection().multiply((-1D) * knockback_multiplier));
        }
    }

    public void reloadGun() {
        this.durability = this.max_durability;
        this.canshoot = true;
    }

}
