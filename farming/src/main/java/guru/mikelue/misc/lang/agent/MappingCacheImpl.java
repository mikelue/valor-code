package guru.mikelue.misc.lang.agent;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("unchecked")
class MappingCacheImpl {
	private final static ConcurrentHashMap<String, Map<?, ? extends Enum<?>>> valueAndEnumCache = new ConcurrentHashMap<>(8);
	private final static ConcurrentHashMap<String, ValueAgent.EnumMate<?, ? extends Enum<?>>> valueAndMateCache = new ConcurrentHashMap<>(8);

	static <T, E extends Enum<E> & ValueAgent<T>> ValueAgent.EnumMate<T, E> getEnumMateOfValueAgent(Class<E> typeOfEnum)
	{
		return (ValueAgent.EnumMate<T, E>)valueAndMateCache.computeIfAbsent(
			typeOfEnum.getTypeName(),
			key -> new ValueAgent.EnumMate<T, E>() {
				private Map<T, E> map = getMapOfValueAgent(typeOfEnum);

				@Override
				public E getEnum(T agentValue)
				{
					return map.getOrDefault(agentValue, null);
				}
			}
		);

	}
	static <T, E extends Enum<E> & ValueAgent<T>> Map<T, E> getMapOfValueAgent(Class<E> typeOfEnum)
	{
		return (Map<T, E>)valueAndEnumCache.computeIfAbsent(
			typeOfEnum.getTypeName(), k -> buildMap(typeOfEnum)
		);
	}

	static <T, E extends Enum<E> & ValueAgent<T>> Map<T, E> buildMap(Class<E> typeOfEnum)
	{
		var newMap = new HashMap<T, E>();

		for (E enumValue: EnumSet.allOf(typeOfEnum)) {
			newMap.put(enumValue.value(), enumValue);
		}

		return Collections.unmodifiableMap(newMap);
	}
}
