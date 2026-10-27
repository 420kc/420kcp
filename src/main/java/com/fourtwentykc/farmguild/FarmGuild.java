package com.fourtwentykc.farmguild;

import com.fourtwentykc.FourTwentyKcConfig;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.MenuEntry;
import net.runelite.api.Model;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.WorldView;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;

/**
 * The Farming Guild's trees and bushes grow hemp instead, and Guildmaster Jane is Mary Jane, on worlds 420 to 422
 * unless the setting says everywhere. Each tree is taken out of the scene as it loads and the hemp plant stands
 * in its place, facing its way; the game's own scene comes back with a reload when the plants go. Part of the 420 kc
 * plugin since 2.0.0 (it was the w420 Farm Guild plugin, never published): the plugin starts and stops it.
 */
@Slf4j
public class FarmGuild
{
	static final int FIRST_WORLD = 420;
	static final int LAST_WORLD = 422;
	// The settings that change which plants stand, so the scene reloads for them alone.
	private static final Set<String> PLANT_KEYS = Set.of("hempPlants", "moreHemp", "growthStage", "only420Worlds");
	// The settings that change her text and the vault's, so the game's text comes back for them alone.
	private static final Set<String> TEXT_KEYS = Set.of("maryJane", "seedStash", "only420Worlds");

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private FourTwentyKcConfig config;

	private final List<Plant> plants = new ArrayList<>();
	// Trees whose hemp model the client is still fetching: each grows hemp the first tick its model is there.
	private final List<Waiting> waiting = new ArrayList<>();
	// Her lines not rewritten, logged once each so a line the transcript words differently can be added.
	private final Set<String> unheard = new HashSet<>();
	// The stages' models, each loaded once.
	private final Map<Integer, Model> models = new HashMap<>();
	// Her dialogue line last read, hers or the game's kept, so each page is read once.
	private String said;
	// Each widget renamed, by its component and child index, so a window drawn afresh replaces its own entry: the widget,
	// the game's text and ours, so turning a setting or the plugin off puts the game's back.
	private final Map<Long, Shown> renamed = new HashMap<>();
	// The day the plants last grew at 4:20, so they grow once in that minute.
	private LocalDate grewOn;

	/** A hemp plant in a tree's place, and its stage. */
	private static final class Plant
	{
		final RuneLiteObject object;
		HempStage stage;

		Plant(RuneLiteObject object, HempStage stage)
		{
			this.object = object;
			this.stage = stage;
		}
	}

	/** A widget showing our text, and the game's to put back. */
	private static final class Shown
	{
		final Widget widget;
		final String game;
		final String ours;

		Shown(Widget widget, String game, String ours)
		{
			this.widget = widget;
			this.game = game;
			this.ours = ours;
		}
	}

	/** A tree waiting on its stage's model. */
	private static final class Waiting
	{
		final GameObject tree;
		final HempStage stage;

		Waiting(GameObject tree, HempStage stage)
		{
			this.tree = tree;
			this.stage = stage;
		}
	}

	// No start: the plugin has RuneLite send this every object already in the scene as it starts in game, so a guild
	// already loaded grows hemp from those, and a reload as well would place a second set over the first.

	/** The plugin stopping: the plants go, and the game's own text and scene come back. */
	public void stop()
	{
		clientThread.invoke(() ->
		{
			clearPlants();
			models.clear();
			restoreText();
			reloadScene();
		});
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!FourTwentyKcConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}
		if (TEXT_KEYS.contains(event.getKey()))
		{
			// The game's text comes back at once, and the next tick renames whatever the settings still rename.
			clientThread.invoke(this::restoreText);
		}
		if (PLANT_KEYS.contains(event.getKey()))
		{
			// The plants come or go with the scene: a reload places them, or brings the game's trees back.
			clientThread.invoke(this::reloadScene);
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		// The scene is rebuilt (a world hop too): its objects spawn again, and the plants placed for the old one go.
		if (event.getGameState() == GameState.LOADING || event.getGameState() == GameState.LOGIN_SCREEN
			|| event.getGameState() == GameState.HOPPING)
		{
			clearPlants();
		}
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		GameObject object = event.getGameObject();
		// Every object of every scene load passes here, so the guild's own ids go first and the settings after.
		if (!FarmGuildScenery.replaces(object.getId(), object.getWorldLocation(), true)
			|| !config.hempPlants() || !onThisWorld()
			|| !FarmGuildScenery.replaces(object.getId(), object.getWorldLocation(), config.moreHemp()))
		{
			return;
		}
		// Random draws each plant's stage as its tree spawns, so every load of the guild grows a new mix.
		HempStage stage = config.growthStage().forPlant(n -> ThreadLocalRandom.current().nextInt(n));
		if (!grow(object, stage))
		{
			waiting.add(new Waiting(object, stage));
		}
	}

	/** Puts a hemp plant in a tree's place, or false while the client is still fetching its model. */
	private boolean grow(GameObject tree, HempStage stage)
	{
		Model model = model(stage.getModel());
		if (model == null)
		{
			return false;
		}
		RuneLiteObject plant = client.createRuneLiteObject();
		plant.setModel(model);
		plant.setLocation(tree.getLocalLocation(), tree.getPlane());
		plant.setOrientation(tree.getOrientation());
		plant.setActive(true);
		plants.add(new Plant(plant, stage));
		client.getTopLevelWorldView().getScene().removeGameObject(tree);
		return true;
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (plants.isEmpty() || !config.growAtFourTwenty() || !HempStage.isFourTwenty(LocalTime.now()))
		{
			return;
		}
		LocalDate today = LocalDate.now();
		if (today.equals(grewOn))
		{
			return;
		}
		grewOn = today;
		for (Plant plant : plants)
		{
			HempStage next = plant.stage.next();
			Model model = next == plant.stage ? null : model(next.getModel());
			if (model != null)
			{
				plant.object.setModel(model);
				plant.stage = next;
			}
		}
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (!onThisWorld())
		{
			return;
		}
		MenuEntry entry = event.getMenuEntry();
		String target = entry.getTarget();
		String hers = maryJane() ? MaryJane.rename(target) : target;
		String renamed = seedStash() ? SeedStash.rename(hers) : hers;
		if (renamed != null && !renamed.equals(target))
		{
			entry.setTarget(renamed);
		}
	}

	@Subscribe
	public void onClientTick(ClientTick event)
	{
		// A model the cache didn't have yet (a player who has never grown hemp) arrives within a few ticks.
		if (!waiting.isEmpty())
		{
			// Each missing model is asked for once a tick, however many trees wait on it.
			Set<Integer> missing = new HashSet<>();
			waiting.removeIf(w -> !missing.contains(w.stage.getModel()) && (grow(w.tree, w.stage)
				|| !missing.add(w.stage.getModel())));
		}
		if (seedStash())
		{
			// The vault's window title is a child its frame script draws.
			renameTree(client.getWidget(InterfaceID.SeedVault.FRAME), 2);
		}
		if (!maryJane())
		{
			return;
		}
		// Her dialogue writes its name and line afresh with each page, so each frame's are checked; a frame without
		// her dialogue open finds no widget.
		rename(client.getWidget(InterfaceID.Objectbox.TEXT));
		Widget name = client.getWidget(InterfaceID.ChatLeft.NAME);
		if (name == null || name.isHidden() || !MaryJane.isHer(name.getText()))
		{
			return;
		}
		rename(name);
		Widget line = client.getWidget(InterfaceID.ChatLeft.TEXT);
		if (line == null || line.isHidden())
		{
			return;
		}
		String text = line.getText();
		if (text == null || text.equals(said))
		{
			return;
		}
		String hers = MaryJane.say(text, n -> ThreadLocalRandom.current().nextInt(n));
		if (hers != null)
		{
			said = hers;
			show(line, hers);
			return;
		}
		said = text;
		if (unheard.add(text))
		{
			log.debug("Mary Jane: a line of Guildmaster Jane's kept as the game's: {}", text);
		}
	}

	/** On worlds 420 to 422, or anywhere when the setting allows. */
	private boolean onThisWorld()
	{
		int world = client.getWorld();
		return !config.only420Worlds() || world >= FIRST_WORLD && world <= LAST_WORLD;
	}

	private boolean maryJane()
	{
		return config.maryJane() && onThisWorld();
	}

	private boolean seedStash()
	{
		return config.seedStash() && onThisWorld();
	}

	/** The Seed Stash's name in a widget and its children, this many levels down. */
	private void renameTree(Widget widget, int depth)
	{
		if (widget == null || widget.isHidden())
		{
			return;
		}
		String text = widget.getText();
		String stash = SeedStash.rename(text);
		if (stash != null && !stash.equals(text))
		{
			show(widget, stash);
		}
		if (depth == 0)
		{
			return;
		}
		for (Widget[] children : new Widget[][]{widget.getDynamicChildren(), widget.getStaticChildren(), widget.getNestedChildren()})
		{
			for (Widget child : children == null ? new Widget[0] : children)
			{
				renameTree(child, depth - 1);
			}
		}
	}

	private void rename(Widget widget)
	{
		if (widget == null || widget.isHidden())
		{
			return;
		}
		String text = widget.getText();
		String hers = MaryJane.rename(text);
		if (hers != null && !hers.equals(text))
		{
			show(widget, hers);
		}
	}

	/** Shows our text in a widget, keeping the game's to put back. */
	private void show(Widget widget, String text)
	{
		renamed.put((long) widget.getId() << 32 | (widget.getIndex() & 0xffffffffL), new Shown(widget, widget.getText(), text));
		widget.setText(text);
	}

	/** Puts the game's text back in every widget still showing ours. */
	private void restoreText()
	{
		for (Shown shown : renamed.values())
		{
			// A widget the game has since written over is the game's again: nothing of ours to put back.
			if (shown.ours.equals(shown.widget.getText()))
			{
				shown.widget.setText(shown.game);
			}
		}
		renamed.clear();
		said = null;
	}

	private Model model(int id)
	{
		Model model = models.get(id);
		if (model == null)
		{
			model = client.loadModel(id);
			if (model == null)
			{
				log.debug("hemp model {} did not load", id);
				return null;
			}
			models.put(id, model);
		}
		return model;
	}

	private void clearPlants()
	{
		for (Plant plant : plants)
		{
			plant.object.setActive(false);
		}
		plants.clear();
		waiting.clear();
	}

	/** Reloads the scene when it holds the guild, the only place a reload changes anything. */
	private void reloadScene()
	{
		WorldView view = client.getTopLevelWorldView();
		int[] regions = view == null ? null : view.getMapRegions();
		if (client.getGameState() == GameState.LOGGED_IN && regions != null
			&& IntStream.of(regions).anyMatch(r -> r == FarmGuildScenery.REGION))
		{
			client.setGameState(GameState.LOADING);
		}
	}
}
