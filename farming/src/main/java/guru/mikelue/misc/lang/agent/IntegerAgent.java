package guru.mikelue.misc.lang.agent;

import java.util.Map;

/**
 * Types implement this interface provide specific value of Integer type.
 */
public interface IntegerAgent extends ValueAgent<Integer> {
	public interface EnumMate<E extends Enum<E> & IntegerAgent> extends ValueAgent.EnumMate<Integer, E> {}

	/**
	 * Gets service of {@link IntegerAgent.EnumMate}, which is backed by {@link Map}.
	 *
	 * The instance of service would be cached in static scope of JVM.
	 *
	 * @param typeOfEnum Type of enum implements {@link IntegerAgent}
	 *
	 * @return instance of service for getting enum object by int value
	 */
	static <E extends Enum<E> & IntegerAgent> IntegerAgent.EnumMate<E> asEnumMate(Class<E> typeOfEnum)
	{
		IntegerAgent.EnumMate<E> funcImpl = MappingCacheImpl.getEnumMateOfValueAgent(typeOfEnum)::getEnum;
		return funcImpl;
	}

	/**
	 * Builds new instance of map between {@link Integer} and instance of enum.
	 *
	 * @param typeOfEnum Type of enum implements {@link IntegerAgent}
	 *
	 * @return map could be used for resovle enum instance from Integer value
	 */
	static <E extends Enum<E> & IntegerAgent> Map<Integer, E> asNumericCodeMap(Class<E> typeOfEnum)
	{
		return MappingCacheImpl.buildMap(typeOfEnum);
	}

	/**
	 *  The default value would be:
	 *  <ul>
	 *  	<li>{@link Enum#ordinal()} for enum</li>
	 *  	<li>{@link Object#hashCode} otherwise</li>
	 *  </ul>
	 */
	@Override
	public default Integer value()
	{
		if (Enum.class.isInstance(this)) {
			return ((Enum<?>)this).ordinal();
		}

		return hashCode();
	}
	public default int asInteger()
	{
		return value();
	}
}
