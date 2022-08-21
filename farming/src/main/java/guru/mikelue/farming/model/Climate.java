package guru.mikelue.farming.model;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import guru.mikelue.misc.lang.agent.IntegerAgent;

import static guru.mikelue.farming.model.Crop.*;

@DefaultJsonConfig
public enum Climate implements IntegerAgent {
	Tropical(1), Dry(2), Mild(3), Continental(4), Polar(5);

	public final static Map<Climate, Set<Crop>> SuitableCrops;

	public final static IntegerAgent.EnumMate<Climate> ENUM_MATE =
		IntegerAgent.asEnumMate(Climate.class);

	static {
		SuitableCrops = Map.of(
			Tropical, Set.of(Manioc, Rice, Yams),
			Dry, Set.of(Grape, Tomato, Pumpkin),
			Mild, Set.of(Crop.values()),
			Continental, Set.of(Crop.values()),
			Polar, Set.of(Kale, Spinach, Lettuce)
		);
	}

	@JsonCreator
	public static Climate fromValue(int value)
	{
		return ENUM_MATE.getEnum(value);
	}

	private final int intValue;
	private Climate(int newIntValue)
	{
		intValue = newIntValue;
	}

	@Override @JsonValue
	public Integer value()
	{
		return intValue;
	}
}
