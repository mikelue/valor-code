package guru.mikelue.farming.validate;

/**
 * Used to grouping validation for various circumstances.
 */
public interface Groups {
	/**
	 * Indicates the groups is used when doing insertion for data.
	 */
	public interface WhenInsert {}
	/**
	 * Indicates the groups is used when doing update on data.
	 */
	public interface WhenUpdate {}

	/**
	 * Indicates the groups is used when doing cleaning for blocks.
	 */
	public interface ForCleaningBlock {}
}
