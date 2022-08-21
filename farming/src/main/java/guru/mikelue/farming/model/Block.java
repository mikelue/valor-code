package guru.mikelue.farming.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

import javax.persistence.*;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.hibernate.annotations.Type;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;

import guru.mikelue.misc.lang.agent.IntegerAgent;

@Entity
@Table(name="vc_block")
@IdClass(Block.BlockId.class)
@NamedQueries({
	@NamedQuery(
		name="Block.findByLandIdAndStatus",
		query="""
			SELECT b FROM Block b
			WHERE b.status = :status
				AND b.landId = :land_id
			ORDER BY b.id ASC
		"""
	),
})
@DefaultJsonConfig
@JsonPropertyOrder({ "id", "crop", "sow_time", "mature_time", "harvest_amount", "status", "comment", "update_time" })
public class Block {
	public static class BlockId implements Serializable {
		private UUID landId;
		private Short id;

		public BlockId() {}
		public BlockId(UUID newLandId, Short newId)
		{
			landId = newLandId;
			id = newId;
		}
		public BlockId(Land newLand, Short newId)
		{
			this(newLand.getId(), newId);
		}

		/**
		 * Gets land id of block.<p>
		 *
		 * @return land id of block
		 */
		public UUID getLandId()
		{
			return landId;
		}

		/**
		 * Sets land id of block.<p>
		 *
		 * @param newLandId land id of block
		 */
		public void setLandId(UUID newLandId)
		{
			landId = newLandId;
		}

		/**
		 * Gets id of block(contained in land).<p>
		 *
		 * @return id of block(contained in land)
		 */
		public Short getId()
		{
			return id;
		}

		/**
		 * Sets id of block(contained in land).<p>
		 *
		 * @param newId id of block(contained in land)
		 */
		public void setId(Short newId)
		{
			id = newId;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (obj == null) { return false; }
			if (obj == this) { return true; }

			if (!getClass().isInstance(obj)) {
				return false;
			}

			var another = (BlockId)obj;
			return new EqualsBuilder()
				.append(this.landId, another.landId)
				.append(this.id, another.id)
				.isEquals();
		}
		@Override
		public int hashCode()
		{
			return new HashCodeBuilder(22027, 17449)
				.append(this.landId)
				.append(this.id)
				.toHashCode();
		}

		@Override
		public String toString()
		{
			return String.format("(%d)%s", id, landId);
		}
	}

	@DefaultJsonConfig
	public enum Status implements IntegerAgent {
		Available(0), ScheduledSow(1), Occupied(2),
		ScheduledHarvest(3), ScheduledClean(4);

		public final static IntegerAgent.EnumMate<Status> ENUM_MATE =
			IntegerAgent.asEnumMate(Status.class);

		@JsonCreator
		public static Status fromValue(int value)
		{
			return ENUM_MATE.getEnum(value);
		}

		private final int intValue;
		private Status(int newIntValue)
		{
			intValue = newIntValue;
		}

		@Override @JsonValue
		public Integer value()
		{
			return intValue;
		}
	}

	public Block() {}

	@Id
	@Column(name="bl_id", updatable=false)
	@JsonProperty("id")
	private Short id;

	@Id
	@Column(name="bl_ld_id", updatable=false)
	@JsonProperty("uuid")
	private UUID landId;

	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="bl_ld_id", updatable=false, insertable=false)
	private Land land;

	@Enumerated(EnumType.STRING)
	@Column(name="bl_crop")
	private Crop crop;

	@Column(name="bl_sow_time")
	private Instant sowTime;

	@Column(name="bl_mature_time")
	private Instant matureTime;

	@Column(name="bl_harvest_amount")
	private Short harvestAmount;

	@Enumerated(EnumType.STRING)
	@Column(name="bl_status", nullable=false)
	@Type(type="pgsql_enum")
	private Status status;

	@Column(name="bl_comment")
	private String comment;

	@Column(name="bl_update_time", nullable=false)
	private Instant updateTime;

	/**
	 * Gets enclosing land of block.<p>
	 *
	 * @return enclosing land of block
	 */
	public Land getLand()
	{
		return land;
	}

	/**
	 * Sets enclosing land of block.<p>
	 *
	 * @param newLand enclosing land of block
	 */
	public void setLand(Land newLand)
	{
		setLandId(newLand.getId());
	}

	/**
	 * Gets id of land.<p>
	 *
	 * @return id of land
	 */
	public UUID getLandId()
	{
		return landId;
	}

	/**
	 * Sets id of land.<p>
	 *
	 * @param newLandId id of land
	 */
	public void setLandId(UUID newLandId)
	{
		landId = newLandId;
	}

	/**
	 * Gets the id of block globally(includes land id and block's one).
	 */
	public BlockId getBlockId()
	{
		return new BlockId(landId, id);
	}

	/**
	 * Gets id of this block(enclosed by a land).<p>
	 *
	 * @return
	 */
	public Short getId()
	{
		return id;
	}

	/**
	 * Sets .<p>
	 *
	 * @param newId
	 */
	public void setId(Short newId)
	{
		id = newId;
	}

	/**
	 * Gets status of block.<p>
	 *
	 * @return status of block
	 */
	@JsonGetter("status")
	public Status getStatus()
	{
		return status;
	}

	/**
	 * Sets status of block.<p>
	 *
	 * @param newStatus status of block
	 */
	public void setStatus(Status newStatus)
	{
		status = newStatus;
	}

	/**
	 * Gets crop of block.<p>
	 *
	 * @return crop of block
	 */
	@JsonGetter("crop")
	public Crop getCrop()
	{
		return crop;
	}

	/**
	 * Sets crop of block.<p>
	 *
	 * @param newCrop crop of block
	 */
	public void setCrop(Crop newCrop)
	{
		crop = newCrop;
	}

	/**
	 * Gets time of sowing of block.<p>
	 *
	 * @return time of sowing of block
	 */
	@JsonGetter("sow_time")
	public Instant getSowTime()
	{
		return sowTime;
	}

	/**
	 * Sets time of sowing of block.<p>
	 *
	 * @param newSowTime time of sowing of block
	 */
	public void setSowTime(Instant newSowTime)
	{
		sowTime = newSowTime;
	}

	/**
	 * Gets time of mature of block.<p>
	 *
	 * @return time of mature of block
	 */
	@JsonGetter("mature_time")
	public Instant getMatureTime()
	{
		return matureTime;
	}

	/**
	 * Sets time of mature of block.<p>
	 *
	 * @param newMatureTime time of mature of block
	 */
	public void setMatureTime(Instant newMatureTime)
	{
		matureTime = newMatureTime;
	}

	/**
	 * Gets amount of harvest for the block.<p>
	 *
	 * @return amount of harvest for the block
	 */
	@JsonGetter("harvest_amount")
	public Short getHarvestAmount()
	{
		return harvestAmount;
	}

	/**
	 * Sets amount of harvest for the block.<p>
	 *
	 * @param newHarvestAmount amount of harvest for the block
	 */
	public void setHarvestAmount(Short newHarvestAmount)
	{
		harvestAmount = newHarvestAmount;
	}

	/**
	 * Gets comment of block.<p>
	 *
	 * @return comment of block
	 */
	@JsonGetter("comment")
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
	 * Gets udpate time of block.<p>
	 *
	 * @return udpate time of block
	 */
	@JsonGetter("update_time")
	public Instant getUpdateTime()
	{
		return updateTime;
	}

	/**
	 * Sets udpate time of block.<p>
	 *
	 * @param newUpdateTime udpate time of block
	 */
	public void setUpdateTime(Instant newUpdateTime)
	{
		updateTime = newUpdateTime;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == null) { return false; }
		if (obj == this) { return true; }

		if (!getClass().isInstance(obj)) {
			return false;
		}

		var another = (Block)obj;

		return new EqualsBuilder()
			.append(this.getBlockId(), another.getBlockId())
			.isEquals();
	}
	@Override
	public int hashCode()
	{
		return this.getBlockId().hashCode();
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(this, ToStringStyle.NO_CLASS_NAME_STYLE)
			.append("block_id", getBlockId())
			.append("crop", getCrop())
			.append("status", getStatus())
			.append("sow-time", getSowTime())
			.append("mature-time", getMatureTime())
			.append("harvest-amount", getHarvestAmount())
			.append("comment", getComment())
		.build();
	}
}
