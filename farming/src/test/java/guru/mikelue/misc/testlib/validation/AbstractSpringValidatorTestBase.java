package guru.mikelue.misc.testlib.validation;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.executable.ExecutableValidator;
import javax.validation.metadata.BeanDescriptor;

import org.springframework.beans.factory.annotation.Autowired;

import guru.mikelue.misc.testlib.AbstractTestBase;

/**
 * Provides accessor for {@link Validator} and some convenient methods for validation.
 */
public abstract class AbstractSpringValidatorTestBase extends AbstractTestBase {
	@Autowired
    private Validator validator;

	protected AbstractSpringValidatorTestBase() {}

	public Validator getValidator()
	{
		return validator;
	}

	public <T> Set<ConstraintViolation<T>> validate(T object, Class<?>... groups)
	{
		return getValidator().validate(object, groups);
	}
	public <T> Set<ConstraintViolation<T>> validateProperty(T object, String propertyName, Class<?>... groups)
	{
		return getValidator().validateProperty(object, propertyName, groups);
	}
	public <T> Set<ConstraintViolation<T>> validateValue(Class<T> beanType, String propertyName, Object value, Class<?>... groups)
	{
		return getValidator().validateValue(beanType, propertyName, value, groups);
	}
	public ExecutableValidator forExecutables()
	{
		return getValidator().forExecutables();
	}
	public BeanDescriptor getConstraintsForClass(Class<?> clazz)
	{
		return getValidator().getConstraintsForClass(clazz);
	}
}
