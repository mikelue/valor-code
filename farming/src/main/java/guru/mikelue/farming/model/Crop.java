package guru.mikelue.farming.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import guru.mikelue.misc.lang.agent.IntegerAgent;

@DefaultJsonConfig
public enum Crop implements IntegerAgent {
	Manioc(1), Rice(2), Yams(3), Grape(4), Tomato(5),
	Pumpkin(6), Kale(7), Spinach(8), Lettuce(9);

	public final static IntegerAgent.EnumMate<Crop> ENUM_MATE =
		IntegerAgent.asEnumMate(Crop.class);

	@JsonCreator
	public static Crop fromValue(int value)
	{
		return ENUM_MATE.getEnum(value);
	}

	private final int intValue;
	private Crop(int newIntValue)
	{
		intValue = newIntValue;
	}

	@Override @JsonValue
	public Integer value()
	{
		return intValue;
	}
}
