package guru.mikelue.misc.testlib.validation;

import java.lang.annotation.Annotation;
import java.util.List;

import javax.validation.ConstraintViolation;

import org.assertj.core.api.AbstractListAssert;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ObjectAssert;

/**
 * Provides some convenient methods to assert the content of {@link ConstraintViolation}.
 */
public class ConstraintViolationAssert<T> extends AbstractObjectAssert<ConstraintViolationAssert<T>, ConstraintViolation<T>> {
	public ConstraintViolationAssert(
		ConstraintViolation<T> violation
	) {
		super(violation, ConstraintViolationAssert.class);
	}

	public ConstraintViolationAssert<T> constraintIsTypeOfAnnotation(Class<? extends Annotation> typeOfAnnotation)
	{
		extracting("constraintDescriptor")
			.extracting("annotation")
			.as(
				"Checks violation for property path: [%s/%s]. Invalid value: [%s]",
				actual.getPropertyPath(), actual.getRootBeanClass().getSimpleName(),
				actual.getInvalidValue()
			)
			.isInstanceOf(typeOfAnnotation);

		return this;
	}

	public AbstractListAssert<
		?, List<? extends String>,
		String, ObjectAssert<String>
	>
	extractNameOfPropertyPath()
	{
		return Assertions.assertThat(actual.getPropertyPath())
			.extracting("name", String.class);
	}

	/**
	 * Gets the object of actual violation.
	 */
	public ConstraintViolation<T> getViolation()
	{
		return actual;
	}
}
