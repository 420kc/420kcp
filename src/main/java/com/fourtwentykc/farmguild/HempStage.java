package com.fourtwentykc.farmguild;

import java.time.LocalTime;
import java.util.function.IntUnaryOperator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * How grown the guild's hemp is: one of the plant's five growth stages, each the plant model of a hemp patch object
 * (58847 to 58851) without the patch's soil, or each plant a stage of its own, drawn again whenever the guild loads.
 * A plant a plugin places is drawn afresh every frame, not baked like scenery, so the earlier stages cost less: 136
 * fully grown plants cost about 60 fps zoomed out under 117 HD (Dylan, 10/23).
 */
@Getter
@RequiredArgsConstructor
public enum HempStage
{
	STAGE_1("Stage 1", 59767),       // 580 faces
	STAGE_2("Stage 2", 59769),       // 1,356 faces
	STAGE_3("Stage 3", 59771),       // 2,434 faces
	STAGE_4("Stage 4", 59773),       // 2,624 faces
	GROWN("Fully grown", 59774),     // 3,156 faces
	RANDOM("Random", -1);

	private static final HempStage[] GROWING = {STAGE_1, STAGE_2, STAGE_3, STAGE_4, GROWN};

	private final String label;
	private final int model;

	/** The stage for one plant: this one, or for Random, a stage {@code choose} picks (given how many, an index). */
	HempStage forPlant(IntUnaryOperator choose)
	{
		return this == RANDOM ? GROWING[choose.applyAsInt(GROWING.length)] : this;
	}

	/** The stage after this one; fully grown stays fully grown. */
	HempStage next()
	{
		return this == GROWN || this == RANDOM ? this : GROWING[ordinal() + 1];
	}

	/** Whether this minute is 4:20 pm, when every plant grows a stage (Dylan, 10/23). */
	static boolean isFourTwenty(LocalTime now)
	{
		return now.getHour() == 16 && now.getMinute() == 20;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
