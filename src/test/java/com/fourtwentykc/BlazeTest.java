package com.fourtwentykc;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicLong;
import net.runelite.api.Client;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** The smoke and confetti over their runs. */
public class BlazeTest
{
	private static final long SECOND = 1_000_000_000L;

	@Test
	public void anEffectRisesHoldsAndFallsWithinItsRun()
	{
		BlazeOverlay.Effect effect = new BlazeOverlay.Effect(0, 8 * SECOND, 0.6f);
		assertEquals(0, effect.level(0), 1e-6);
		assertEquals(0.3f, effect.level(SECOND / 2), 1e-4);
		assertEquals(0.6f, effect.level(3 * SECOND), 1e-4);
		assertTrue(effect.level(7 * SECOND) < 0.6f);
		assertEquals(0, effect.level(8 * SECOND), 1e-6);
		assertTrue(effect.over(8 * SECOND));
		assertFalse(effect.over(8 * SECOND - 1));
	}

	@Test
	public void smokeCoversTheScreenThenGoes()
	{
		AtomicLong clock = new AtomicLong();
		BlazeOverlay overlay = new BlazeOverlay(canvas(400, 300), clock::get);
		overlay.smoke(4, 80);
		clock.set(2 * SECOND);
		BufferedImage mid = paint(overlay, 400, 300);
		assertTrue("smoke lies over much of the screen", covered(mid) > 0.4);
		clock.set(4 * SECOND);
		assertEquals(0, covered(paint(overlay, 400, 300)), 0);
		assertFalse(overlay.active());
	}

	@Test
	public void thickerSmokeIsThicker()
	{
		AtomicLong clock = new AtomicLong();
		BlazeOverlay thin = new BlazeOverlay(canvas(200, 200), clock::get);
		BlazeOverlay thick = new BlazeOverlay(canvas(200, 200), clock::get);
		thin.smoke(6, 20);
		thick.smoke(6, 100);
		clock.set(3 * SECOND);
		assertTrue(opacity(paint(thick, 200, 200)) > 2 * opacity(paint(thin, 200, 200)));
	}

	@Test
	public void confettiFallsInGreenAndMoreWithDensity()
	{
		AtomicLong clock = new AtomicLong();
		BlazeOverlay little = new BlazeOverlay(canvas(400, 300), clock::get);
		BlazeOverlay lots = new BlazeOverlay(canvas(400, 300), clock::get);
		little.confetti(6, 10);
		lots.confetti(6, 100);
		clock.set(3 * SECOND);
		BufferedImage few = paint(little, 400, 300);
		BufferedImage many = paint(lots, 400, 300);
		assertTrue(covered(many) > 3 * covered(few));
		for (int y = 0; y < many.getHeight(); y++)
		{
			for (int x = 0; x < many.getWidth(); x++)
			{
				int argb = many.getRGB(x, y);
				if ((argb >>> 24) > 0)
				{
					int r = (argb >> 16) & 0xff;
					int g = (argb >> 8) & 0xff;
					int b = argb & 0xff;
					assertTrue("every piece is green", g >= r && g > b);
				}
			}
		}
		clock.set(6 * SECOND);
		assertEquals(0, covered(paint(lots, 400, 300)), 0);
	}

	@Test
	public void aSecondCelebrationLetsTheFirstPlayOn()
	{
		AtomicLong clock = new AtomicLong();
		BlazeOverlay overlay = new BlazeOverlay(canvas(200, 200), clock::get);
		overlay.smoke(3, 100);
		overlay.confetti(3, 100);
		clock.set(3 * SECOND / 2);
		double before = opacity(paint(overlay, 200, 200));
		overlay.smoke(3, 100);
		overlay.confetti(3, 100);
		assertEquals("no snap back to nothing", before, opacity(paint(overlay, 200, 200)), 1e-9);
		clock.set(3 * SECOND);
		assertFalse("and it ends when the first does", overlay.active());
	}

	@Test
	public void anEarlierStopNeverEndsALaterCelebration()
	{
		// Each start of the plugin draws with its own overlay, so the last stop's cleanup can only reach its own.
		AtomicLong clock = new AtomicLong();
		BlazeOverlay before = new BlazeOverlay(canvas(100, 100), clock::get);
		BlazeOverlay after = new BlazeOverlay(canvas(100, 100), clock::get);
		after.confetti(3, 50);
		before.stop();
		assertTrue(after.active());
	}

	@Test
	public void smokeKeepsRollingBetweenRedraws()
	{
		// Redrawn twenty times a second: frames in one twentieth match, frames further apart move on.
		AtomicLong clock = new AtomicLong();
		BlazeOverlay overlay = new BlazeOverlay(canvas(200, 200), clock::get);
		overlay.smoke(3, 100);
		clock.set(SECOND);
		BufferedImage first = paint(overlay, 200, 200);
		clock.addAndGet(BlazeOverlay.SMOKE_REDRAW_NANOS / 2);
		assertTrue("the layer is reused inside its twentieth", same(first, paint(overlay, 200, 200)));
		clock.addAndGet(BlazeOverlay.SMOKE_REDRAW_NANOS * 6);
		assertFalse("and rolls on past it", same(first, paint(overlay, 200, 200)));
	}

	@Test
	public void stopClearsEverything()
	{
		AtomicLong clock = new AtomicLong();
		BlazeOverlay overlay = new BlazeOverlay(canvas(100, 100), clock::get);
		overlay.smoke(10, 50);
		overlay.confetti(10, 50);
		overlay.stop();
		clock.set(2 * SECOND);
		assertFalse(overlay.active());
		assertEquals(0, covered(paint(overlay, 100, 100)), 0);
	}

	private static Client canvas(int width, int height)
	{
		return (Client) Proxy.newProxyInstance(BlazeTest.class.getClassLoader(), new Class<?>[]{Client.class},
			(proxy, method, args) ->
			{
				switch (method.getName())
				{
					case "getCanvasWidth":
						return width;
					case "getCanvasHeight":
						return height;
					default:
						throw new UnsupportedOperationException(method.getName());
				}
			});
	}

	private static BufferedImage paint(BlazeOverlay overlay, int width, int height)
	{
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		overlay.render(g);
		g.dispose();
		return image;
	}

	/** The share of pixels anything was painted on. */
	private static double covered(BufferedImage image)
	{
		int painted = 0;
		for (int y = 0; y < image.getHeight(); y++)
		{
			for (int x = 0; x < image.getWidth(); x++)
			{
				if ((image.getRGB(x, y) >>> 24) > 0)
				{
					painted++;
				}
			}
		}
		return painted / (double) (image.getWidth() * image.getHeight());
	}

	private static boolean same(BufferedImage a, BufferedImage b)
	{
		for (int y = 0; y < a.getHeight(); y++)
		{
			for (int x = 0; x < a.getWidth(); x++)
			{
				if (a.getRGB(x, y) != b.getRGB(x, y))
				{
					return false;
				}
			}
		}
		return true;
	}

	/** The mean alpha across the image, 0 to 1. */
	private static double opacity(BufferedImage image)
	{
		long sum = 0;
		for (int y = 0; y < image.getHeight(); y++)
		{
			for (int x = 0; x < image.getWidth(); x++)
			{
				sum += image.getRGB(x, y) >>> 24;
			}
		}
		return sum / (255.0 * image.getWidth() * image.getHeight());
	}
}
