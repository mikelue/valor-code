package guru.mikelue.farming.model;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import guru.mikelue.farming.base.AbstractJsonTestBase;
import guru.mikelue.farming.validate.Groups.ForCleaningBlock;
import guru.mikelue.misc.testlib.validation.ConstraintViolationAssertions;

import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.lang.annotation.Annotation;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import static org.assertj.core.api.Assertions.*;

public class AskBlockActionTest extends AbstractJsonTestBase {
	public AskBlockActionTest() {}

	/**
	 * Tests the deserialization for model.
	 */
	@Test
	void deserialize()
	{
		var sampleJson = createObjectNode()
			.put("crop", Crop.Manioc.value())
			.put("asked_blocks", 32)
			.put("comment", "chimpanzee");

		var testedAction = treeToValue(sampleJson, AskBlockAction.class);


		assertThat(testedAction)
			.hasFieldOrPropertyWithValue("crop", Crop.Manioc)
			.hasFieldOrPropertyWithValue(
				"askedBlocks",
				sampleJson.get("asked_blocks").shortValue()
			)
			.hasFieldOrPropertyWithValue(
				"comment",
				sampleJson.get("comment").asText()
			);

	}

	/**
	 * Tests the bean validation.
	 */
	@ParameterizedTest
	@MethodSource
	void validation(
		String property, Object sampleValue,
		Class<? extends Annotation> expectedTypeOfViolation,
		Class<?>... groups
	) {
		ConstraintViolationAssertions.assertThatAsSingle(
			this.validateValue(
				AskBlockAction.class, property, sampleValue,
				groups
			))
			.constraintIsTypeOfAnnotation(expectedTypeOfViolation);
	}
	static Arguments[] validation()
	{
		return new Arguments[] {
			arguments("crop", null, NotNull.class, new Class<?>[0]),
			arguments("askedBlocks", null, NotNull.class, new Class<?>[0]),
			arguments("askedBlocks", (short)-1, Min.class, new Class<?>[0]),
			arguments("askedBlocks", (short)-1, Min.class, new Class<?>[] { ForCleaningBlock.class }),
			arguments("askedBlocks", null, NotNull.class, new Class<?>[] { ForCleaningBlock.class }),
		};
	}
}
