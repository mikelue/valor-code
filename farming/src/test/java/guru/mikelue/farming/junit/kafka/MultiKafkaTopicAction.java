package guru.mikelue.farming.junit.kafka;

import java.lang.annotation.*;

@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented @Inherited
public @interface MultiKafkaTopicAction {
	KafkaTopicAction[] value();
}
