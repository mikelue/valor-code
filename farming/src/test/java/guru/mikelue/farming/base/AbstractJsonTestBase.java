package guru.mikelue.farming.base;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContentAssert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import guru.mikelue.misc.testlib.AbstractTestBase;

@JsonTest
@ImportAutoConfiguration(ValidationAutoConfiguration.class)
public abstract class AbstractJsonTestBase extends AbstractTestBase {
	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JacksonTester<JsonNode> jsonTester;

	@Autowired
	private Validator validator;

	protected AbstractJsonTestBase() {}

	public ObjectMapper getObjectMapper()
	{
		return objectMapper;
	}

	public JacksonTester<JsonNode> jacksonTester;

	public JsonNode valueToTree(Object value)
	{
		return objectMapper.valueToTree(value);
	}

	public <T> T treeToValue(JsonNode jsonNode, Class<T> typeOfValue)
	{
		try {
			return objectMapper.treeToValue(jsonNode, typeOfValue);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	public JacksonTester<JsonNode> getJacksonTester()
	{
		return jsonTester;
	}

	public JsonContentAssert assertAsJsonContent(Object value)
	{
		try {
			return assertThat(jsonTester.write(valueToTree(value)));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public ObjectNode createObjectNode()
	{
		return getObjectMapper().createObjectNode();
	}

	public ArrayNode createArrayNode()
	{
		return getObjectMapper().createArrayNode();
	}

	public Validator getValidator()
	{
		return validator;
	}

	public <T> Set<ConstraintViolation<T>> validateValue(Class<T> beanType, String propertyName, Object value, Class<?>... groups)
	{
		return validator.validateValue(beanType, propertyName, value, groups);
	}
}
