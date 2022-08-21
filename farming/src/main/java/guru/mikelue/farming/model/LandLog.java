package guru.mikelue.farming.model;

import java.time.Instant;
import java.util.UUID;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.mapping.*;

import static org.springframework.data.cassandra.core.cql.PrimaryKeyType.*;

@Table("vc_land_log_by_time")
public class LandLog {
	@PrimaryKeyClass
	public static class PK {
		public static PK from(Block ordinaryBlock)
		{
			return from(ordinaryBlock.getLandId(), null, ordinaryBlock.getId());
		}
		public static PK from(UUID landId, Instant time, Short blockId)
		{
			var newPK = new PK();

			newPK.landId = landId;
			newPK.time = time;
			newPK.blockId = blockId;

			return newPK;
		}

		@PrimaryKeyColumn(name="ll_ld_id", type=PARTITIONED)
		private UUID landId;

		@PrimaryKeyColumn(name="ll_time", ordinal=1, ordering=Ordering.DESCENDING)
		private Instant time;

		@PrimaryKeyColumn(name="ll_bl_id", ordinal=2, ordering=Ordering.ASCENDING)
		private Short blockId;

		@Override
		public boolean equals(Object obj)
		{
			if (obj == null) { return false; }
			if (obj == this) { return true; }
			if (obj.getClass() != getClass()) {
				return false;
			}

			var rhs = (PK)obj;
			return new EqualsBuilder()
				.append(this.landId, rhs.landId)
				.append(this.time, rhs.time)
				.append(this.blockId, rhs.blockId)
				.isEquals();
		}

		@Override
		public int hashCode()
		{
			return new HashCodeBuilder(451873, 21589)
				.append(landId)
				.append(time)
				.append(blockId)
			.toHashCode();
		}

		@Override
		public String toString()
		{
			return new ToStringBuilder(this, ToStringStyle.NO_CLASS_NAME_STYLE)
				.append("BlockId", String.format("(%d)%s", blockId, landId))
				.append("Time", time)
			.build();
		}
	}

	@UserDefinedType("udt_block")
	public static class BlockPayload {
		public static BlockPayload from(Block ordinaryPayload)
		{
			var newPayload = new BlockPayload();

			newPayload.setCrop(ordinaryPayload.getCrop());
			newPayload.setSowTime(ordinaryPayload.getSowTime());
			newPayload.setMatureTime(ordinaryPayload.getMatureTime());
			newPayload.setComment(ordinaryPayload.getComment());
			newPayload.setUpdateTime(ordinaryPayload.getUpdateTime());
			newPayload.setHarvestAmount(ordinaryPayload.getHarvestAmount());

			return newPayload;
		}

		@Column("bl_crop")
		private Byte crop = -1;

		@Column("bl_sow_time")
		private Instant sowTime;

		@Column("bl_mature_time")
		private Instant matureTime;

		@Column("bl_harvest_amount")
		private Short harvestAmount;

		@Column("bl_comment")
		private String comment;

		@Column("bl_update_time")
		private Instant updateTime;

		/**
		 * Gets crop of block.<p>
		 *
		 * @return crop of block
		 */
		public Crop getCrop()
		{
			return Crop.ENUM_MATE.getEnum(crop.intValue());
		}

		/**
		 * Sets crop of block.<p>
		 *
		 * @param newCrop crop of block
		 */
		public void setCrop(Byte newCrop)
		{
			crop = newCrop;
		}

		/**
		 * Sets crop of block.<p>
		 *
		 * @param newCrop crop of block
		 */
		public void setCrop(Crop ordinaryCrop)
		{
			crop = (byte)(ordinaryCrop != null ? ordinaryCrop.value() : -1);
		}

		/**
		 * Gets sow time of block.<p>
		 *
		 * @return sow time of block
		 */
		public Instant getSowTime()
		{
			return sowTime;
		}

		/**
		 * Sets sow time of block.<p>
		 *
		 * @param newSowTime sow time of block
		 */
		public void setSowTime(Instant newSowTime)
		{
			sowTime = newSowTime;
		}

		/**
		 * Gets mature time of block.<p>
		 *
		 * @return mature time of block
		 */
		public Instant getMatureTime()
		{
			return matureTime;
		}

		/**
		 * Sets mature time of block.<p>
		 *
		 * @param newMatureTime mature time of block
		 */
		public void setMatureTime(Instant newMatureTime)
		{
			matureTime = newMatureTime;
		}

		/**
		 * Gets comment of block.<p>
		 *
		 * @return comment of block
		 */
		public String getComment()
		{
			return comment;
		}

		/**
		 * Sets comment of block.<p>
		 *
		 * @param newComment comment of block
		 */
		public void setComment(String newComment)
		{
			comment = newComment;
		}

		/**
		 * Gets update time of block.<p>
		 *
		 * @return update time of block
		 */
		public Instant getUpdateTime()
		{
			return updateTime;
		}

		/**
		 * Sets update time of block.<p>
		 *
		 * @param newUpdateTime update time of block
		 */
		public void setUpdateTime(Instant newUpdateTime)
		{
			updateTime = newUpdateTime;
		}

		/**
		 * Gets amount of harvesting for the block.<p>
		 *
		 * <code>-1</code> would be used as no harvesting information.
		 *
		 * @return amount of harvesting for the block
		 */
		public Short getHarvestAmount()
		{
			return harvestAmount;
		}

		/**
		 * Sets amount of harvesting for the block.<p>
		 *
		 * @param newHarvestAmount amount of harvesting for the block
		 */
		public void setHarvestAmount(Short newHarvestAmount)
		{
			harvestAmount = newHarvestAmount != null ? newHarvestAmount : (short)-1;
		}
	}

	public static LandLog from(Block ordinaryBlock)
	{
		var newLog = new LandLog();
		newLog.pk = PK.from(ordinaryBlock);
		newLog.setPayload(ordinaryBlock);

		return newLog;
	}

	@PrimaryKey
	private PK pk = new PK();

	@Column("ll_activity")
	private Byte activity;

	@Column("ll_used_time_second")
	private Short usedTimeSecond;

	@Column(value="ll_bl_payload", isStatic=true)
	private BlockPayload payload;

	public LandLog() {}

	/**
	 * Gets id of land.<p>
	 *
	 * @return id of land
	 */
	public UUID getLandId()
	{
		return pk.landId;
	}

	/**
	 * Sets id of land.<p>
	 *
	 * @param newId id of land
	 */
	public void setLandId(UUID newId)
	{
		pk.landId = newId;
	}

	/**
	 * Gets time of log.<p>
	 *
	 * @return time of log
	 */
	public Instant getTime()
	{
		return pk.time;
	}

	/**
	 * Sets time of log.<p>
	 *
	 * @param newTime time of log
	 */
	public void setTime(Instant newTime)
	{
		pk.time = newTime;
	}

	/**
	 * Gets id of block for the land.<p>
	 *
	 * @return id of block for the land
	 */
	public Short getBlockId()
	{
		return pk.blockId;
	}

	/**
	 * Gets activity of log.<p>
	 *
	 * @return activity of log
	 */
	public LogActivity getActivity()
	{
		return LogActivity.ENUM_MATE.getEnum((int)activity);
	}

	/**
	 * Sets activity of log.<p>
	 *
	 * @param newActivity activity of log
	 */
	public void setActivity(LogActivity newActivity)
	{
		activity = newActivity.value().byteValue();
	}

	/**
	 * Gets used time(in seconds).<p>
	 *
	 * @return used time(in seconds)
	 */
	public Short getUsedTimeSecond()
	{
		return usedTimeSecond;
	}

	/**
	 * Sets used time(in seconds).<p>
	 *
	 * @param newUsedTimeSecond used time(in seconds)
	 */
	public void setUsedTimeSecond(Short newUsedTimeSecond)
	{
		usedTimeSecond = newUsedTimeSecond;
	}

	/**
	 * Gets payload of block.<p>
	 *
	 * @return payload of block
	 */
	public BlockPayload getPayload()
	{
		return payload;
	}

	/**
	 * Sets payload of block(by block object of JPA).<p>
	 *
	 * @param newPayload payload of block
	 */
	public void setPayload(Block ordinaryPayload)
	{
		payload = BlockPayload.from(ordinaryPayload);
	}

	/**
	 * Sets id of block for the land.<p>
	 *
	 * @param newBlockId id of block for the land
	 */
	public void setBlockId(Short newBlockId)
	{
		pk.blockId = newBlockId;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == null) { return false; }
		if (obj == this) { return true; }
		if (obj.getClass() != getClass()) {
			return false;
		}

		var rhs = (LandLog)obj;
		return new EqualsBuilder()
			.append(this.pk, rhs.pk)
			.isEquals();
	}

	@Override
	public int hashCode()
	{
		return new HashCodeBuilder(451873, 21589)
			.append(pk)
		.toHashCode();
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(this, ToStringStyle.NO_CLASS_NAME_STYLE)
			.append("Log-Id", pk)
			.append("Activity", getActivity())
			.append("Comment", payload.getComment())
		.build();
	}
}
