package com.fourtwentykc;

import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.WidgetNode;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetModalMode;

/**
 * The 420 in the game's own popup, the one a new collection log slot raises. It waits its turn behind the game's
 * own popups (collection log slots, combat achievement tasks), one at a time. Everything here runs on the client
 * thread, a game tick at a time.
 */
final class BlazePopup
{
	static final String TITLE = "420";
	static final String BODY = "Blaze it!";
	// The game's notification_display_init: title, body, title colour.
	private static final int NOTIFICATION_DISPLAY_INIT = 3343;
	private static final int TITLE_COLOUR = 0x00ff00;
	// The game queues its own popups back to back: the slot must stand empty this many ticks before ours shows, so it
	// comes after the whole series and never between two of them.
	static final int QUIET_TICKS = 3;

	// Each window layout keeps its own place for the game's notifications; any other layout shows none.
	private static final int[][] SLOTS = {
		{InterfaceID.TOPLEVEL, InterfaceID.Toplevel.NOTIFICATIONS},
		{InterfaceID.TOPLEVEL_PRE_EOC, InterfaceID.ToplevelPreEoc.NOTIFICATIONS},
		{InterfaceID.TOPLEVEL_OSRS_STRETCH, InterfaceID.ToplevelOsrsStretch.NOTIFICATIONS},
	};

	private final Client client;
	private int waiting;
	private int quiet;
	private WidgetNode showing;

	BlazePopup(Client client)
	{
		this.client = client;
	}

	/** Queues a popup, shown once the game's own have played. */
	void show()
	{
		waiting++;
	}

	/** Each game tick: a popup that has played closes, and the next one shows when the slot has been quiet. */
	void tick()
	{
		GameState state = client.getGameState();
		if (state == GameState.LOADING)
		{
			// A region loading mid-session keeps the game's interfaces: everything waits for it.
			return;
		}
		if (state != GameState.LOGGED_IN)
		{
			// Game ticks stop with a logout or hop; any other state leaves our popup to the game.
			showing = null;
			quiet = 0;
			return;
		}
		if (showing != null)
		{
			if (slotOf(showing) >= 0 && playing())
			{
				return;
			}
			close();
			return;
		}
		int slot = notificationSlot();
		if (waiting == 0 || slot < 0 || client.getWidget(slot) == null
			|| client.getComponentTable().get(slot) != null && playing())
		{
			quiet = 0;
			return;
		}
		if (++quiet < QUIET_TICKS)
		{
			return;
		}
		quiet = 0;
		showing = client.openInterface(slot, InterfaceID.NOTIFICATION_DISPLAY, WidgetModalMode.MODAL_CLICKTHROUGH);
		// Taken from the queue only once it is open.
		waiting--;
		client.runScript(NOTIFICATION_DISPLAY_INIT, TITLE, "<col=ffffff>" + BODY + "</col>", TITLE_COLOUR);
	}

	/** Everything goes with the plugin: the queue, and our popup if it is still up. */
	void clear()
	{
		waiting = 0;
		quiet = 0;
		close();
	}

	/** A popup is on screen: the game shrinks its container to nothing once it has played. */
	private boolean playing()
	{
		Widget container = client.getWidget(InterfaceID.NotificationDisplay.CONTAINER);
		return container != null && container.getWidth() > 0;
	}

	/** The notification slot holding this popup, in whichever layout it now sits (a layout change moves it); or -1. */
	private int slotOf(WidgetNode node)
	{
		for (int[] layout : SLOTS)
		{
			if (client.getComponentTable().get(layout[1]) == node)
			{
				return layout[1];
			}
		}
		return -1;
	}

	/** Our popup closes where it sits; one the game has already taken away is only forgotten. */
	private void close()
	{
		if (showing != null && slotOf(showing) >= 0)
		{
			client.closeInterface(showing, true);
		}
		showing = null;
	}

	/** This layout's notification slot, or -1 for a layout that shows none. */
	private int notificationSlot()
	{
		int toplevel = client.getTopLevelInterfaceId();
		for (int[] layout : SLOTS)
		{
			if (layout[0] == toplevel)
			{
				return layout[1];
			}
		}
		return -1;
	}
}
