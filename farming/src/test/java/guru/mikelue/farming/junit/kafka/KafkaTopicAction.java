package guru.mikelue.farming.junit.kafka;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented @Inherited
@Repeatable(value=MultiKafkaTopicAction.class)
public @interface KafkaTopicAction {
	/**
	 * The phase to apply Kafka actions.
	 */
	public enum Phase {
		/**
		 * Applies Kafka operations after class or method.
		 */
		AFTER,
		/**
		 * Applies Kafka operations before class or method.
		 */
		BEFORE;
	}

	public String[] topics() default {};

	public Phase phase() default Phase.AFTER;
}
