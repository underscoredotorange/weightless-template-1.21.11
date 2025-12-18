package com.weightless;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import static net.minecraft.commands.Commands.literal;

// Imports
import net.minecraft.server.MinecraftServer;;

public class Weightless implements ModInitializer {
	public static final String MOD_ID = "weightless";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public int nextTick = 0;

	float[] slotWeights = {0.4f, 1.8f, 1.3f, 0.4f};
	float[] materialMultipliers = {1.0f, 3.0f, 5.0f, 2.7f, 30.0f, 3.6f, 10.0f};
	float universal_multiplier = 3.0f;

	public void log(String message) { LOGGER.info(message); }
	public MinecraftServer SERVER_INSTANCE;

	public void runCommand(String COMMAND) {
		//log("[Threecore] Running command: " + COMMAND);
		if (SERVER_INSTANCE != null) {
			var DISPATCHER = SERVER_INSTANCE.getCommands().getDispatcher();
			var SOURCE = SERVER_INSTANCE.createCommandSourceStack().withSuppressedOutput();
			try {
				DISPATCHER.execute(COMMAND, SOURCE);
			} catch(com.mojang.brigadier.exceptions.CommandSyntaxException e){
				log("[Threecore] Error running '" + COMMAND + "' -> " + e);
			} catch(Exception e) {
				log("[Threecore] Error running: " + COMMAND + "' -> " + e);
			}
		} else {
			log("[Threecore] Error running any commands at all! SERVER_INSTANCE is null!");
		}
	}

	public float getWeight(ItemStack stack, String slot) {
		String string = stack + "";

		float weight = 0;

		if (slot.contains("HEAD")) {
			weight+=slotWeights[0];
		} else if (slot.contains("CHEST")) {
			weight+=slotWeights[1];
		} else if (slot.contains("LEGS")) {
			weight+=slotWeights[2];
		} else if (slot.contains("FEET")) {
			weight+=slotWeights[3];
		} 

		if (string.contains("air")) {
			return 0;
		} else if (string.contains("leather")) {
			weight*=materialMultipliers[0];
		} else if (string.contains("copper")) {
			weight*=materialMultipliers[1]; 
		} else if (string.contains("iron")) {
			weight*=materialMultipliers[2];
		} else if (string.contains("chainmail")) {
			weight*=materialMultipliers[3];
		} else if (string.contains("gold")) {
			weight*=materialMultipliers[4];
		} else if (string.contains("diamond")) {
			weight*=materialMultipliers[5];
		} else if (string.contains("netherite")) {
			weight*=materialMultipliers[6];
		} else {
			return 0;
		}

		return weight;
	}

	private void refreshConfig(java.io.File config) {
		try {
			if (!config.exists()) {
				try {
					configFile.createNewFile();
					try (java.io.FileWriter w = new java.io.FileWriter(config)) {
							w.write("Slots weight: Head, Chest, Legs, Feet\n");
						for (float weight : slotWeights) {
							w.write(weight +  "\n");
						}
						w.write("Weight Multiplier: Leather, Copper, Iron, Chainmail, Gold, Diamond, Netherite\n");
						for (float multiplier : materialMultipliers) {
							w.write(multiplier +  "\n");
						}
						w.write("Universal Weight Multiplier (Higher/Lower = 1kg faster/slower)\n");
						w.write("2.25\n");
					}
				} catch (SecurityException e) {
					LOGGER.error("[Weightless] Major error encountered while trying to write to config file!\n" + e);
				}
			} else {
				try {
					java.util.List<String> lines = Files.readAllLines(configFile.toPath());{
						for (int x = 0; x < slotWeights.length; x++) {
							slotWeights[x] = Float.parseFloat(lines.get(x + 1));
						}
						for (int x = 0; x < materialMultipliers.length; x++) {
							materialMultipliers[x] = Float.parseFloat(lines.get(x + 6));
						}
						universal_multiplier = Float.parseFloat(lines.get(14));
					}
				}
				catch (Exception e) {
					LOGGER.error("[Weightless] Minor error encountered while trying to read config file!\n" + e);
				}
			}
		} catch (Exception e) {
			LOGGER.error("[Weightless] Severe error encountered while trying to create a config file!\n" + e);
		}
		
	}

	private void registerRefreshConfig(String argument1) {
		CommandRegistrationCallback.EVENT.register((DISPATCHER, REGISTRYACCESS, ENVIIRONMENT) -> {
			DISPATCHER.register(
				literal(argument1)
					.executes(context -> {
						refreshConfig(configFile);
						return 1;
					})
			);
		});
	}

	private String configPath;
	private java.io.File configFile;
	private void initialize() {
		var config = System.getProperty("user.dir") + "\\config\\weightless";
		var configFolder = new java.io.File(config);
		configPath = configFolder.getAbsolutePath() + "\\config.txt";
		configFile = new java.io.File(configPath);

		try {
			if (!configFolder.exists()) {
				log("[Weightless] Creating config...");
				try {
					configFolder.mkdir();
					log("[Weightless] Created config folder");
				} catch (SecurityException e) {
					LOGGER.error("[Weightless] Major error encountered while creating a config folder!\n" + e);
				}
				refreshConfig(configFile);

			}
		} catch (Exception e) {
			LOGGER.error("[Weightless] Severe error encountered while creating a config file and folder!\n" + e);
		}
		
	}

	@Override
	public void onInitialize() {
		LOGGER.info("[Weightless] I feel weightless :(");
		initialize();

		refreshConfig(configFile);

		ServerTickEvents.START_SERVER_TICK.register((INSTANCE) -> {
			SERVER_INSTANCE = INSTANCE;
			if (nextTick == 20) {
				for (ServerPlayer player : INSTANCE.getPlayerList().getPlayers()) {

					String name = player.getName().getString();

					float base_weight = 40;
					

					float head_weight = 0;
					float chest_weight = 0;
					float leg_weight = 0;
					float feet_weight = 0;

					for (EquipmentSlot slot : EquipmentSlot.values()) {
						//log("Slot: " + slot);
						if (slot == EquipmentSlot.HEAD) {
							head_weight=getWeight(player.getItemBySlot(slot), "HEAD");
						} else if (slot == EquipmentSlot.CHEST) {
							chest_weight=getWeight(player.getItemBySlot(slot), "CHEST");
						} else if (slot == EquipmentSlot.LEGS) {
							leg_weight=getWeight(player.getItemBySlot(slot), "LEGS");
						} else if (slot == EquipmentSlot.FEET) {
							feet_weight=getWeight(player.getItemBySlot(slot), "FEET");
						} else { /* pass */ }
					}
					
					float equipment_weight = head_weight + chest_weight + leg_weight + feet_weight;
					//log (equipment_weight + " FROM " + head_weight + " + " + chest_weight + " + " + leg_weight + " + " + feet_weight);
					String new_weight = String.format("%.6f", ((float)1 / (float)(base_weight + equipment_weight)) * universal_multiplier);
					runCommand("attribute " + name + " minecraft:movement_speed base set " + new_weight);
				}
				nextTick = 0;
			}
			
			nextTick+=1;
		});

		registerRefreshConfig("weight_refresh_config");
	}
}