package guru.mikelue.farming.kafka;

import java.nio.ByteBuffer;
import java.util.UUID;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;

import guru.mikelue.farming.model.Block.BlockId;

public class BlockSerialization {
	private BlockSerialization() {}

	// 2 longs + 1 short
	private final static int NEED_BYTES_FOR_KEY = 18;

	public static class KeySerializerImpl implements Serializer<BlockId> {
        @Override
        public byte[] serialize(String topic, BlockId blockId)
		{
			var buffer = ByteBuffer.allocate(NEED_BYTES_FOR_KEY);

			var landId = blockId.getLandId();
			buffer.putLong(landId.getMostSignificantBits());
			buffer.putLong(landId.getLeastSignificantBits());
			buffer.putShort(blockId.getId());

			return buffer.array();
        }
	}

	public static class KeyDeserializerImpl implements Deserializer<BlockId> {
        @Override
        public BlockId deserialize(String topic, byte[] data)
		{
			var sourceBytes = ByteBuffer.wrap(data);

			var leftBitsOfUuid = sourceBytes.getLong();
			var rightBitsOfUuid = sourceBytes.getLong();
			var blockIdInLand = sourceBytes.getShort();

            return new BlockId(
				new UUID(leftBitsOfUuid, rightBitsOfUuid),
				blockIdInLand
			);
        }
	}
}
