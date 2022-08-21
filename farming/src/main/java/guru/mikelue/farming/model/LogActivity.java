package guru.mikelue.farming.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import guru.mikelue.misc.lang.agent.IntegerAgent;

@DefaultJsonConfig
public enum LogActivity implements IntegerAgent {
	Sowing(1), Harvesting(2), Cleaning(3);

	public final static IntegerAgent.EnumMate<LogActivity> ENUM_MATE =
		IntegerAgent.asEnumMate(LogActivity.class);

	@JsonCreator
	public static LogActivity fromValue(int value)
	{
		return ENUM_MATE.getEnum(value);
	}

	private final int intValue;
	private LogActivity(int newValue)
	{
		intValue = newValue;
	}

	@Override @JsonValue
	public Integer value()
	{
		return intValue;
	}
}
