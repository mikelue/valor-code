package guru.mikelue.farming.json;

import java.io.IOException;
import java.time.Instant;

import org.springframework.boot.jackson.JsonComponent;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

@JsonComponent
public class InstantJsonComponent {
	public InstantJsonComponent() {}

	public static class Serializer extends JsonSerializer<Instant> {
        @Override
        public void serialize(
			Instant value, JsonGenerator jgen, SerializerProvider provider
		) throws IOException
		{
			if (value == null) {
				jgen.writeNull();
			} else {
				jgen.writeNumber(value.getEpochSecond());
			}
        }

    }

    public static class Deserializer extends JsonDeserializer<Instant> {
        @Override
        public Instant deserialize(
			JsonParser p, DeserializationContext ctxt
		) throws IOException
		{
            return Instant.ofEpochSecond(p.getLongValue());
        }
	}
}
