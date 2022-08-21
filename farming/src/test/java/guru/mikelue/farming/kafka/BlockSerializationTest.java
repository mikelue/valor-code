package guru.mikelue.farming.kafka;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.junit.jupiter.api.*;

import guru.mikelue.farming.model.RandomModels;
import guru.mikelue.farming.model.Block.BlockId;
import guru.mikelue.misc.testlib.AbstractTestBase;

import static org.assertj.core.api.Assertions.*;
import static guru.mikelue.farming.kafka.BlockSerialization.*;

public class BlockSerializationTest extends AbstractTestBase {
	private Serializer<BlockId> testedSerializer =
		new KeySerializerImpl();
	private Deserializer<BlockId> testedDeserializer =
		new KeyDeserializerImpl();

	public BlockSerializationTest() {}

	/**
	 * Tests the serialization for {@link BlockId}.
	 */
	@Test
	void serializeKey()
	{
		var sampleBlockId = randomTestId();

		byte[] testedResult = testedSerializer.serialize(
			null, sampleBlockId
		);

		assertThat(
			bytesToLong(testedResult, 0, 8)
		)
			.isEqualTo(sampleBlockId.getLandId()
				.getMostSignificantBits());

		assertThat(
			bytesToLong(testedResult, 8, 16)
		)
			.isEqualTo(sampleBlockId.getLandId()
				.getLeastSignificantBits());

		assertThat(
			bytesToShort(testedResult, 16, 18)
		)
			.isEqualTo(sampleBlockId.getId());
	}

	/**
	 * Tests the deserialization for {@link BlockId}.
	 */
	@Test
	void deserializeKey()
	{
		var sampleBlockId = randomTestId();
		var sampleBytes = testedSerializer.serialize(null, sampleBlockId);

		var testedResult = testedDeserializer.deserialize(null, sampleBytes);

		assertThat(testedResult)
			.isEqualTo(sampleBlockId);
	}

	private static BlockId randomTestId()
	{
		return RandomModels.randomBlock()
			.getBlockId();
	}

	private static long bytesToLong(byte[] source, int from, int to)
	{
		long value = 0l;

		for (var i = from; i < to; i++) {
			value <<= 8;
			value += source[i] & 0xFF;
		}

		return value;
	}

	private static short bytesToShort(byte[] source, int from, int to)
	{
		short value = (short)0;

		for (var i = from; i < to; i++) {
			value <<= 8;
			value += source[i] & 0xFF;
		}

		return value;
	}
}
