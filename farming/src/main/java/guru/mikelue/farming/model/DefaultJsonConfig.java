package guru.mikelue.farming.model;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.annotation.JsonAutoDetect;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@JsonAutoDetect(
	creatorVisibility=NONE,
	setterVisibility=NONE,
	isGetterVisibility=NONE,
	getterVisibility=NONE,
	fieldVisibility=NONE
)
@JacksonAnnotationsInside
public @interface DefaultJsonConfig {}

