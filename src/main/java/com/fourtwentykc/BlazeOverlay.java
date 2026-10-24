package com.fourtwentykc;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.LongSupplier;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Smoke and confetti over the whole game screen. Each piece lives in fractions of the screen, so a resize mid-blaze
 * keeps it where it was, and each effect fades in and out over its own duration.
 */
class BlazeOverlay extends Overlay
{
	static final int SMOKE_PUFFS = 56;
	static final int MOST_CONFETTI = 320;
	// The kc greens, from lime to leaf.
	static final Color[] GREENS = {
		new Color(0x4E, 0xF0, 0x15), new Color(0xCA, 0xFF, 0x00), new Color(0x2E, 0xB8, 0x2E),
		new Color(0x7C, 0xFC, 0x00), new Color(0x1F, 0x8A, 0x1F),
	};
	// Smoke is soft, so it draws at a quarter of the screen's width and height and is scaled up once: a sixteenth of
	// the blending, the same look.
	static final int SMOKE_SCALE = 4;
	static final float SMOKE_PACE = 3;
	// Smoke drifts slowly, so its layer is redrawn twenty times a second and shown as it is between.
	static final long SMOKE_REDRAW_NANOS = 50_000_000L;
	private static final BufferedImage PUFF = puff(128);

	private final Client client;
	private final LongSupplier nanos;
	private final Random random = new Random();
	private final List<Puff> puffs = new ArrayList<>();
	private final List<Flake> flakes = new ArrayList<>();
	private Effect smoke;
	private Effect confetti;
	private BufferedImage smokeLayer;
	private long smokeLayerAt;

	BlazeOverlay(Client client)
	{
		this(client, System::nanoTime);
	}

	BlazeOverlay(Client client, LongSupplier nanos)
	{
		this.client = client;
		this.nanos = nanos;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(PRIORITY_HIGHEST);
	}

	/** One effect's run: when it began, how long it lasts and how strong it is at its peak (0 to 1). */
	static final class Effect
	{
		final long start;
		final long length;
		final float strength;

		Effect(long start, long length, float strength)
		{
			this.start = start;
			this.length = length;
			this.strength = strength;
		}

		/** Seconds since it began. */
		float age(long now)
		{
			return (now - start) / 1e9f;
		}

		boolean over(long now)
		{
			return now - start >= length;
		}

		/** Its strength now: up over the first second, down over the last third, never past its peak. */
		float level(long now)
		{
			float t = age(now);
			float total = length / 1e9f;
			float in = Math.min(1f, t / Math.min(1f, total / 4));
			float out = Math.min(1f, (total - t) / (total / 3));
			return strength * Math.max(0f, Math.min(in, out));
		}
	}

	/** A puff of smoke: where it starts and drifts, how big it grows, how much of the smoke it carries. */
	private static final class Puff
	{
		float x;
		float y;
		float dx;
		float dy;
		float size;
		float grow;
		float alpha;
	}

	/** A piece of confetti: when it drops, where, how it falls, sways and spins, and its green. */
	private static final class Flake
	{
		float delay;
		float x;
		float fall;
		float sway;
		float swaySpeed;
		float phase;
		float spin;
		float width;
		float height;
		Color color;
	}

	/** Smoke for this many seconds at this density (percent of full); smoke already rolling plays on. */
	void smoke(int seconds, int density)
	{
		if (running(smoke))
		{
			return;
		}
		puffs.clear();
		for (int i = 0; i < SMOKE_PUFFS; i++)
		{
			Puff puff = new Puff();
			puff.x = random.nextFloat();
			// Most of it rises from the lower half of the screen.
			puff.y = 0.35f + random.nextFloat() * 0.75f;
			puff.dx = (random.nextFloat() - 0.5f) * 0.03f;
			puff.dy = -(0.015f + random.nextFloat() * 0.035f);
			puff.size = 0.12f + random.nextFloat() * 0.22f;
			puff.grow = 0.03f + random.nextFloat() * 0.05f;
			puff.alpha = 0.3f + random.nextFloat() * 0.6f;
			puffs.add(puff);
		}
		smoke = new Effect(nanos.getAsLong(), seconds * 1_000_000_000L, density / 100f);
	}

	/** Confetti for this many seconds, at this density (percent of the most there can be); confetti falling plays on. */
	void confetti(int seconds, int density)
	{
		if (running(confetti))
		{
			return;
		}
		flakes.clear();
		int count = Math.max(1, MOST_CONFETTI * density / 100);
		for (int i = 0; i < count; i++)
		{
			Flake flake = new Flake();
			// Drops come through the first two thirds of the run, so it rains the whole time.
			flake.delay = random.nextFloat() * seconds * 2 / 3;
			flake.x = random.nextFloat();
			flake.fall = 0.18f + random.nextFloat() * 0.22f;
			flake.sway = 0.004f + random.nextFloat() * 0.012f;
			flake.swaySpeed = 1.5f + random.nextFloat() * 2.5f;
			flake.phase = random.nextFloat() * 6.283f;
			flake.spin = (random.nextFloat() - 0.5f) * 10f;
			flake.width = 4 + random.nextFloat() * 4;
			flake.height = 7 + random.nextFloat() * 6;
			flake.color = GREENS[random.nextInt(GREENS.length)];
			flakes.add(flake);
		}
		confetti = new Effect(nanos.getAsLong(), seconds * 1_000_000_000L, 1f);
	}

	/** Everything stops at once. */
	void stop()
	{
		smoke = null;
		confetti = null;
		puffs.clear();
		flakes.clear();
		smokeLayer = null;
	}

	private boolean running(Effect effect)
	{
		return effect != null && !effect.over(nanos.getAsLong());
	}

	boolean active()
	{
		return running(smoke) || running(confetti);
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		long now = nanos.getAsLong();
		if (smoke != null && smoke.over(now))
		{
			smoke = null;
		}
		if (confetti != null && confetti.over(now))
		{
			confetti = null;
		}
		if (!active())
		{
			return null;
		}
		int w = client.getCanvasWidth();
		int h = client.getCanvasHeight();
		Composite composite = g.getComposite();
		AffineTransform transform = g.getTransform();
		RenderingHints hints = g.getRenderingHints();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		if (smoke != null)
		{
			paintSmoke(g, w, h, now);
		}
		else
		{
			smokeLayer = null;
		}
		if (confetti != null)
		{
			paintConfetti(g, w, h, now);
		}
		g.setComposite(composite);
		g.setTransform(transform);
		g.setRenderingHints(hints);
		return null;
	}

	private void paintSmoke(Graphics2D g, int w, int h, long now)
	{
		float level = smoke.level(now);
		if (level <= 0)
		{
			return;
		}
		int lw = Math.max(1, w / SMOKE_SCALE);
		int lh = Math.max(1, h / SMOKE_SCALE);
		boolean resized = smokeLayer == null || smokeLayer.getWidth() != lw || smokeLayer.getHeight() != lh;
		if (resized)
		{
			smokeLayer = new BufferedImage(lw, lh, BufferedImage.TYPE_INT_ARGB_PRE);
		}
		if (resized || now - smokeLayerAt >= SMOKE_REDRAW_NANOS)
		{
			smokeLayerAt = now;
			drawSmokeLayer(lw, lh, level, now);
		}
		// Smoke changes so gently from pixel to pixel that the plain upscale looks the same as a smooth one, at a
		// fraction of the cost.
		g.setComposite(AlphaComposite.SrcOver);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		g.drawImage(smokeLayer, 0, 0, w, h, null);
	}

	/** The puffs into the quarter-size layer, as they stand now. */
	private void drawSmokeLayer(int lw, int lh, float level, long now)
	{
		Graphics2D layer = smokeLayer.createGraphics();
		layer.setComposite(AlphaComposite.Clear);
		layer.fillRect(0, 0, lw, lh);
		layer.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		// The smoke moves through its run at the pace of a three-second one, so a long run rolls slowly instead of
		// swelling past the screen.
		float t = smoke.age(now) * SMOKE_PACE / (smoke.length / 1e9f);
		int span = Math.max(lw, lh);
		for (Puff puff : puffs)
		{
			int size = (int) (span * (puff.size + puff.grow * t));
			int x = (int) (lw * (puff.x + puff.dx * t)) - size / 2;
			int y = (int) (lh * (puff.y + puff.dy * t)) - size / 2;
			layer.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1f, level * puff.alpha)));
			layer.drawImage(PUFF, x, y, size, size, null);
		}
		layer.dispose();
	}

	private void paintConfetti(Graphics2D g, int w, int h, long now)
	{
		float level = confetti.level(now);
		float t = confetti.age(now);
		AffineTransform base = g.getTransform();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, level))));
		for (Flake flake : flakes)
		{
			float fell = t - flake.delay;
			if (fell < 0)
			{
				continue;
			}
			float y = -0.05f + flake.fall * fell;
			if (y > 1.05f)
			{
				continue;
			}
			float x = flake.x + flake.sway * (float) Math.sin(flake.phase + flake.swaySpeed * fell) * 8;
			g.setTransform(base);
			g.translate(x * w, y * h);
			g.rotate(flake.phase + flake.spin * fell);
			// A flat piece turning in the air: its width swings through its edge.
			g.scale(Math.cos(flake.phase + fell * flake.swaySpeed * 1.7), 1);
			g.setColor(flake.color);
			g.fillRect((int) (-flake.width / 2), (int) (-flake.height / 2), (int) flake.width, (int) flake.height);
		}
		g.setTransform(base);
	}

	/** One soft puff: a pale, faintly green cloud that fades to nothing at its edge. */
	static BufferedImage puff(int size)
	{
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		float r = size / 2f;
		g.setPaint(new RadialGradientPaint(r, r, r, new float[]{0f, 0.45f, 1f},
			new Color[]{new Color(225, 235, 225, 200), new Color(205, 220, 205, 110), new Color(190, 205, 190, 0)},
			MultipleGradientPaint.CycleMethod.NO_CYCLE));
		g.fillOval(0, 0, size, size);
		g.dispose();
		return image;
	}
}
