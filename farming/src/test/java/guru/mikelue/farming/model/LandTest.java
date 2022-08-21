package guru.mikelue.farming.model;

import java.lang.annotation.Annotation;
import java.util.UUID;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import guru.mikelue.farming.base.AbstractJsonTestBase;
import guru.mikelue.farming.validate.Groups;
import guru.mikelue.misc.testlib.validation.ConstraintViolationAssertions;

import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.assertj.core.api.Assertions.*;

public class LandTest extends AbstractJsonTestBase {
	public LandTest() {}

	/**
	 * Tests the JSON representation of land.
	 */
	@Test
	void jsonSerialization()
	{
		var sampleLand = new Land();
		sampleLand.setId(UUID.randomUUID());
		sampleLand.setName("yangmei");
		sampleLand.setSize((short)29);
		sampleLand.setClimate(Climate.Tropical);

		getLogger().info("JSON of Land: {}", getObjectMapper().valueToTree(sampleLand));

		assertAsJsonContent(sampleLand)
			.hasJsonPathValue("[?(@.id == '%s')]", sampleLand.getId().toString())
			.hasJsonPathValue("[?(@.name == '%s')]", sampleLand.getName())
			.hasJsonPathValue("[?(@.climate == %d)]", Climate.Tropical.value())
			.hasJsonPathValue("[?(@.size == %d)]", sampleLand.getSize());
	}
	/**
	 * Tests the JSON representation of land.
	 */
	@Test
	void jsonDeserialzation()
	{
		var sampleJson = getObjectMapper().createObjectNode()
			.put("name", " kangaroo ")
			.put("climate", 3);

		var testedResult = treeToValue(sampleJson, Land.class);

		assertThat(testedResult)
			// Tests the trimming
			.hasFieldOrPropertyWithValue("name", "kangaroo")
			.hasFieldOrPropertyWithValue("climate", Climate.Mild);
	}

	/**
	 * Tests the bean validation.
	 */
	@ParameterizedTest
	@MethodSource
	void validation(
		String property, Object sampleValue,
		Class<? extends Annotation> expectedTypeOfViolation,
		Class<?>[] groups
	) {
		ConstraintViolationAssertions.assertThatAsSingle(
			this.validateValue(Land.class, property, sampleValue, groups)
		)
			.constraintIsTypeOfAnnotation(expectedTypeOfViolation);
	}
	static Arguments[] validation()
	{
		return new Arguments[] {
			arguments("name", null, NotNull.class, new Class<?>[0]),
			arguments("name", null, NotNull.class, new Class<?>[] { Groups.WhenUpdate.class }),
			arguments("size", null, NotNull.class, new Class<?>[0]),
			arguments("size", (short)0, Min.class, new Class<?>[0]),
			arguments("climate", null, NotNull.class, new Class<?>[0]),
		};
	}
}
