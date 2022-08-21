package guru.mikelue.misc.lang.agent;

import java.util.Optional;

/**
 * Defines objects(like {@link Enum}, normally) could be represented as a certain type of value.
 *
 * @param <T> - The type of value to which the object represent.
 */
public interface ValueAgent<T> {
	/**
	 * Defines the interface to get {@Enum} instance from the value of defined type.
	 *
	 * @param <T> - The type of value from which the enum instance gets.
	 * @param <E> - The {@link Enum} type implements {@link ValueAgent}
	 */
	@FunctionalInterface
	public interface EnumMate<T, E extends Enum<E> & ValueAgent<T>> {

		/**
		 * Gets instance of enum instance.
		 *
		 * @param value The value used to look for enum instance
		 *
		 * @return The value(may be null if nothing matched)
		 */
		E getEnum(T value);

		/**
		 * Gets {@link Optional} instance of {@link #getEnum(T)}.
		 *
		 * @param value The value used to look for enum instance
		 *
		 * @return Optional instance in case of null value from {@link #getEnum(T)}
		 */
		default Optional<E> getEnumOptional(T value)
		{
			return Optional.ofNullable(getEnum(value));
		}
	};

	/**
	 * Gets the value represented by this object.
	 *
	 * @return The value(must not be null).
	 */
	public T value();
}
