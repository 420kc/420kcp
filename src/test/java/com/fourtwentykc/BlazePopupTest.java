package com.fourtwentykc;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.HashTable;
import net.runelite.api.WidgetNode;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** The 420 popup waits its turn behind the game's own and never strands, blocks or closes what is not its own. */
public class BlazePopupTest
{
	private static final int SLOT = InterfaceID.Toplevel.NOTIFICATIONS;

	private static final int STRETCH_SLOT = InterfaceID.ToplevelOsrsStretch.NOTIFICATIONS;

	private GameState state = GameState.LOGGED_IN;
	private int toplevel = InterfaceID.TOPLEVEL;
	private boolean openFails;
	private boolean slotBuilt = true;
	private int containerWidth;
	private final Map<Long, WidgetNode> table = new HashMap<>();
	private final List<WidgetNode> opened = new ArrayList<>();
	private final List<WidgetNode> closed = new ArrayList<>();
	private BlazePopup popup;

	@Before
	public void setUp()
	{
		popup = new BlazePopup(client());
	}

	@Test
	public void itWaitsForTheSlotToStandQuiet()
	{
		popup.show();
		for (int i = 1; i < BlazePopup.QUIET_TICKS; i++)
		{
			popup.tick();
			assertEquals(0, opened.size());
		}
		popup.tick();
		assertEquals(1, opened.size());
	}

	@Test
	public void itComesAfterTheGamesOwnSeries()
	{
		popup.show();
		WidgetNode game = gamePopup();
		for (int i = 0; i < 10; i++)
		{
			popup.tick();
		}
		assertEquals("never over a game popup that is playing", 0, opened.size());
		// One ends and the next of the game's own follows a tick later: ours waits through the gap.
		containerWidth = 0;
		popup.tick();
		containerWidth = 200;
		table.put((long) SLOT, gamePopup());
		popup.tick();
		popup.tick();
		assertEquals(0, opened.size());
		containerWidth = 0;
		for (int i = 0; i < BlazePopup.QUIET_TICKS; i++)
		{
			popup.tick();
		}
		assertEquals(1, opened.size());
		assertTrue(game != opened.get(0));
	}

	@Test
	public void aPlayedPopupClosesAndTheNextShows()
	{
		popup.show();
		popup.show();
		quietTicks();
		assertEquals(1, opened.size());
		containerWidth = 200;
		popup.tick();
		assertEquals(0, closed.size());
		containerWidth = 0;
		popup.tick();
		assertEquals(1, closed.size());
		quietTicks();
		assertEquals(2, opened.size());
	}

	@Test
	public void aHopNeverBlocksOrStrandsTheQueue()
	{
		popup.show();
		popup.show();
		quietTicks();
		containerWidth = 200;
		// The hop takes the game's interfaces: our popup is gone without a close.
		state = GameState.HOPPING;
		table.clear();
		popup.tick();
		state = GameState.LOGGED_IN;
		containerWidth = 0;
		quietTicks();
		assertEquals("the queued one shows after the hop", 2, opened.size());
		assertEquals("nothing closed that was no longer there", 0, closed.size());
	}

	@Test
	public void aPopupWaitsForTheSlotAfterLogin()
	{
		slotBuilt = false;
		popup.show();
		for (int i = 0; i < 10; i++)
		{
			popup.tick();
		}
		assertEquals(0, opened.size());
		slotBuilt = true;
		quietTicks();
		assertEquals(1, opened.size());
	}

	@Test
	public void aLayoutChangeKeepsOurPopupOurs()
	{
		popup.show();
		quietTicks();
		WidgetNode ours = opened.get(0);
		containerWidth = 200;
		// Fixed to resizable: the game moves the playing popup into the new layout's slot.
		table.clear();
		table.put((long) STRETCH_SLOT, ours);
		toplevel = InterfaceID.TOPLEVEL_OSRS_STRETCH;
		popup.tick();
		assertEquals("still playing, still ours", 0, closed.size());
		containerWidth = 0;
		popup.tick();
		assertEquals("closed where it now sits", 1, closed.size());
		assertTrue(closed.get(0) == ours);
		popup.show();
		quietTicks();
		assertEquals(2, opened.size());
		containerWidth = 200;
		popup.clear();
		assertEquals("and the plugin's end closes it too", 2, closed.size());
	}

	@Test
	public void aLayoutWithNoNotificationsWaitsWithoutLosingTheQueue()
	{
		toplevel = InterfaceID.TOPLEVEL_DISPLAY;
		popup.show();
		for (int i = 0; i < 10; i++)
		{
			popup.tick();
		}
		assertEquals(0, opened.size());
		toplevel = InterfaceID.TOPLEVEL;
		quietTicks();
		assertEquals(1, opened.size());
	}

	@Test
	public void aFailedOpenKeepsTheCelebrationQueued()
	{
		popup.show();
		openFails = true;
		try
		{
			quietTicks();
		}
		catch (IllegalStateException expected)
		{
			// The event bus logs it; the queue keeps the popup.
		}
		openFails = false;
		quietTicks();
		assertEquals(1, opened.size());
	}

	@Test
	public void clearingClosesOnlyItsOwn()
	{
		popup.show();
		quietTicks();
		containerWidth = 200;
		// A layout change moved the slot's contents on: the node is no longer ours to close.
		table.put((long) SLOT, gamePopup());
		popup.clear();
		assertEquals(0, closed.size());
		popup.show();
		popup.clear();
		table.clear();
		containerWidth = 0;
		quietTicks();
		assertEquals("cleared means nothing waits", 1, opened.size());
	}

	private void quietTicks()
	{
		for (int i = 0; i < BlazePopup.QUIET_TICKS; i++)
		{
			popup.tick();
		}
	}

	private WidgetNode gamePopup()
	{
		WidgetNode node = node();
		table.put((long) SLOT, node);
		containerWidth = 200;
		return node;
	}

	private static WidgetNode node()
	{
		return (WidgetNode) Proxy.newProxyInstance(BlazePopupTest.class.getClassLoader(), new Class<?>[]{WidgetNode.class},
			(proxy, method, args) -> method.getName().equals("equals") ? proxy == args[0]
				: method.getName().equals("hashCode") ? System.identityHashCode(proxy) : null);
	}

	@SuppressWarnings("unchecked")
	private Client client()
	{
		HashTable<WidgetNode> components = (HashTable<WidgetNode>) Proxy.newProxyInstance(getClass().getClassLoader(),
			new Class<?>[]{HashTable.class}, (proxy, method, args) -> table.get((Long) args[0]));
		Widget slot = (Widget) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Widget.class},
			(proxy, method, args) -> null);
		Widget container = (Widget) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Widget.class},
			(proxy, method, args) -> method.getName().equals("getWidth") ? containerWidth : null);
		return (Client) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Client.class},
			(proxy, method, args) ->
			{
				switch (method.getName())
				{
					case "getGameState":
						return state;
					case "getTopLevelInterfaceId":
						return toplevel;
					case "getComponentTable":
						return components;
					case "getWidget":
						int id = (Integer) args[0];
						return id == SLOT || id == STRETCH_SLOT ? (slotBuilt ? slot : null)
							: id == InterfaceID.NotificationDisplay.CONTAINER && !table.isEmpty() ? container : null;
					case "openInterface":
						if (openFails)
						{
							throw new IllegalStateException("component does not exist");
						}
						WidgetNode mine = node();
						table.put((long) (Integer) args[0], mine);
						opened.add(mine);
						containerWidth = 0;
						return mine;
					case "closeInterface":
						// The real client refuses a node no longer in the component table.
						if (!table.containsValue(args[0]))
						{
							throw new IllegalArgumentException("WidgetNode is no longer valid");
						}
						closed.add((WidgetNode) args[0]);
						table.values().remove(args[0]);
						return null;
					case "runScript":
						return null;
					default:
						throw new UnsupportedOperationException(method.getName());
				}
			});
	}
}
