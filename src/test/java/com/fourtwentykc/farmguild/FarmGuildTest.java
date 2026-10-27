package com.fourtwentykc.farmguild;

import com.fourtwentykc.FourTwentyKcConfig;
import java.lang.reflect.Field;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.Model;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.Scene;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.ProfileChanged;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The Farm Guild's rules a player meets first, driven through its own handlers against a mocked client. */
public class FarmGuildTest
{
	// A tile in the guild's map square, 4922.
	private static final WorldPoint IN_GUILD = new WorldPoint(1250, 3740, 0);

	private Client client;
	private FourTwentyKcConfig config;
	private Scene scene;
	private FarmGuild guild;

	@Before
	public void setUp() throws Exception
	{
		client = mock(Client.class);
		config = mock(FourTwentyKcConfig.class);
		ClientThread clientThread = mock(ClientThread.class);
		doAnswer(call ->
		{
			((Runnable) call.getArgument(0)).run();
			return null;
		}).when(clientThread).invoke(any(Runnable.class));
		WorldView view = mock(WorldView.class);
		scene = mock(Scene.class);
		when(client.getTopLevelWorldView()).thenReturn(view);
		when(view.getScene()).thenReturn(scene);
		when(view.getMapRegions()).thenReturn(new int[]{FarmGuildScenery.REGION});
		when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
		when(client.getWorld()).thenReturn(420);
		when(client.loadModel(anyInt())).thenReturn(mock(Model.class));
		when(client.createRuneLiteObject()).thenAnswer(call -> mock(RuneLiteObject.class));
		when(config.hempPlants()).thenReturn(true);
		when(config.moreHemp()).thenReturn(true);
		when(config.growthStage()).thenReturn(HempStage.GROWN);
		when(config.growAtFourTwenty()).thenReturn(true);
		when(config.maryJane()).thenReturn(true);
		when(config.seedStash()).thenReturn(true);
		when(config.only420Worlds()).thenReturn(true);

		guild = new FarmGuild();
		inject("client", client);
		inject("clientThread", clientThread);
		inject("config", config);
	}

	private void inject(String name, Object value) throws Exception
	{
		Field field = FarmGuild.class.getDeclaredField(name);
		field.setAccessible(true);
		field.set(guild, value);
	}

	private GameObject spawn(int id, WorldPoint at)
	{
		GameObject tree = mock(GameObject.class);
		when(tree.getId()).thenReturn(id);
		when(tree.getWorldLocation()).thenReturn(at);
		when(tree.getLocalLocation()).thenReturn(new LocalPoint(0, 0, 0));
		GameObjectSpawned event = new GameObjectSpawned();
		event.setGameObject(tree);
		guild.onGameObjectSpawned(event);
		return tree;
	}

	private static ConfigChanged changed(String key)
	{
		ConfigChanged event = new ConfigChanged();
		event.setGroup(FourTwentyKcConfig.GROUP);
		event.setKey(key);
		return event;
	}

	@Test
	public void hempGrowsOnWorlds420To422AloneUnlessTheSwitchSaysEverywhere()
	{
		for (int world : new int[]{419, 420, 421, 422, 423, 301})
		{
			when(client.getWorld()).thenReturn(world);
			GameObject tree = spawn(34486, new WorldPoint(1250, 3740 + world - 400, 0));
			verify(scene, times(world >= 420 && world <= 422 ? 1 : 0)).removeGameObject(tree);
		}
		when(config.only420Worlds()).thenReturn(false);
		when(client.getWorld()).thenReturn(301);
		GameObject anywhere = spawn(34486, new WorldPoint(1251, 3741, 0));
		verify(scene).removeGameObject(anywhere);
	}

	@Test
	public void aTreeGetsOnePlantHoweverOftenItSpawns()
	{
		spawn(34486, IN_GUILD);
		spawn(34486, IN_GUILD);
		verify(client, times(1)).createRuneLiteObject();
	}

	@Test
	public void aNewSceneTakesThePlantsAway()
	{
		for (GameState state : new GameState[]{GameState.LOADING, GameState.HOPPING, GameState.LOGIN_SCREEN})
		{
			RuneLiteObject plant = mock(RuneLiteObject.class);
			when(client.createRuneLiteObject()).thenReturn(plant);
			spawn(34486, IN_GUILD);
			GameStateChanged event = new GameStateChanged();
			event.setGameState(state);
			guild.onGameStateChanged(event);
			verify(plant).setActive(false);
		}
	}

	@Test
	public void aNewGrowthStageChangesThePlantsInPlace()
	{
		RuneLiteObject plant = mock(RuneLiteObject.class);
		when(client.createRuneLiteObject()).thenReturn(plant);
		spawn(34486, IN_GUILD);
		Model sapling = mock(Model.class);
		when(client.loadModel(HempStage.STAGE_1.getModel())).thenReturn(null, sapling);
		when(config.growthStage()).thenReturn(HempStage.STAGE_1);

		guild.onConfigChanged(changed("growthStage"));
		verify(plant, never()).setModel(sapling);
		guild.onClientTick(new ClientTick());
		verify(plant).setModel(sapling);
		verify(client, never()).setGameState(any());
	}

	@Test
	public void atFourTwentyEachPlantGrowsOnceAndOneWaitingOnItsModelGrowsWhenItArrives()
	{
		when(config.growthStage()).thenReturn(HempStage.STAGE_1);
		RuneLiteObject plant = mock(RuneLiteObject.class);
		when(client.createRuneLiteObject()).thenReturn(plant);
		spawn(34486, IN_GUILD);
		HempStage next = HempStage.STAGE_1.next();
		Model grown = mock(Model.class);
		when(client.loadModel(next.getModel())).thenReturn(null, grown);
		guild.clock = Clock.fixed(LocalDateTime.of(2026, 11, 4, 16, 20, 5).atZone(ZoneId.systemDefault()).toInstant(),
			ZoneId.systemDefault());

		guild.onGameTick(new GameTick());
		verify(plant, never()).setModel(grown);
		guild.onGameTick(new GameTick());
		guild.onGameTick(new GameTick());
		verify(plant, times(1)).setModel(grown);
	}

	@Test
	public void aPlantWaitingOnANewStageGrowsFromItAtFourTwenty()
	{
		when(config.growthStage()).thenReturn(HempStage.STAGE_1);
		RuneLiteObject plant = mock(RuneLiteObject.class);
		when(client.createRuneLiteObject()).thenReturn(plant);
		spawn(34486, IN_GUILD);
		// Stage 3's model is still on its way when the setting changes, just before 4:20.
		Model stage3 = mock(Model.class);
		Model stage4 = mock(Model.class);
		when(client.loadModel(HempStage.STAGE_3.getModel())).thenReturn(null, stage3);
		when(client.loadModel(HempStage.STAGE_4.getModel())).thenReturn(stage4);
		when(config.growthStage()).thenReturn(HempStage.STAGE_3);
		guild.onConfigChanged(changed("growthStage"));
		guild.clock = Clock.fixed(LocalDateTime.of(2026, 11, 4, 16, 20, 5).atZone(ZoneId.systemDefault()).toInstant(),
			ZoneId.systemDefault());

		guild.onGameTick(new GameTick());
		verify(plant, never()).setModel(stage4);
		guild.onClientTick(new ClientTick());
		verify(plant).setModel(stage3);
		guild.onGameTick(new GameTick());
		verify(plant).setModel(stage4);
	}

	@Test
	public void aProfileSwitchReloadsTheGuild()
	{
		guild.onProfileChanged(new ProfileChanged());
		verify(client).setGameState(GameState.LOADING);
	}

	@Test
	public void turningItOffPutsTheVaultsTitleBack()
	{
		AtomicReference<String> text = new AtomicReference<>("Seed Vault");
		Widget title = mock(Widget.class);
		when(title.getText()).thenAnswer(call -> text.get());
		doAnswer(call ->
		{
			text.set(call.getArgument(0));
			return null;
		}).when(title).setText(anyString());
		when(client.getWidget(InterfaceID.SeedVault.FRAME)).thenReturn(title);

		guild.onClientTick(new ClientTick());
		org.junit.Assert.assertEquals("Seed Stash", text.get());
		guild.stop();
		org.junit.Assert.assertEquals("Seed Vault", text.get());
		verify(client, atLeastOnce()).setGameState(GameState.LOADING);
	}
}
