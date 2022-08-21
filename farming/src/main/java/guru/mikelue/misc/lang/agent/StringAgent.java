package guru.mikelue.misc.lang.agent;

import java.util.Map;

/**
 * Types implement this interface provide specific value of String type.
 */
public interface StringAgent extends ValueAgent<String> {
	public interface EnumMate<E extends Enum<E> & StringAgent> extends ValueAgent.EnumMate<String, E> {}

	/**
	 * Gets service of {@link StringAgent.EnumMate}, which is backed by {@link Map}.
	 *
	 * The instance of service would be cached in static scope of JVM.
	 *
	 * @param typeOfEnum Type of enum implements {@link StringAgent}
	 *
	 * @return instance of service for getting enum object by String value
	 */
	static <E extends Enum<E> & StringAgent> EnumMate<E> asEnumMate(Class<E> typeOfEnum)
	{
		StringAgent.EnumMate<E> funcImpl = MappingCacheImpl.getEnumMateOfValueAgent(typeOfEnum)::getEnum;
		return funcImpl;
	}

	/**
	 * Builds new instance of map between {@link String} and instance of enum.
	 *
	 * @param typeOfEnum Type of enum implements {@link StringAgent}
	 *
	 * @return map could be used for resovle enum instance from String value
	 */
	static <E extends Enum<E> & StringAgent> Map<String, E> asStringCodeMap(Class<E> typeOfEnum)
	{
		return MappingCacheImpl.buildMap(typeOfEnum);
	}

	@Override
	default String value()
	{
		return toString();
	}
}
