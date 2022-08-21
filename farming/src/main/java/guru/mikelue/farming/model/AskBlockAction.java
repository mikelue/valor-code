package guru.mikelue.farming.model;

import java.util.UUID;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

import guru.mikelue.farming.validate.Groups.ForCleaningBlock;

@DefaultJsonConfig
public class AskBlockAction {
	private UUID landId;

	@JsonProperty("crop")
	@NotNull
	private Crop crop;

	@JsonProperty("asked_blocks")
	@NotNull(groups={Default.class, ForCleaningBlock.class})
	@Min(value=1, groups={Default.class, ForCleaningBlock.class})
	private Short askedBlocks;

	private String comment;

	public AskBlockAction() {}
	public AskBlockAction(AskBlockAction another)
	{
		landId = another.landId;
		crop = another.crop;
		askedBlocks = another.askedBlocks;
		comment = another.comment;
	}

	/**
	 * Gets id of land to be sowed.<p>
	 *
	 * @return id of land to be sowed
	 */
	public UUID getLandId()
	{
		return landId;
	}

	/**
	 * Sets id of land to be sowed.<p>
	 *
	 * @param newLandId id of land to be sowed
	 */
	public void setLandId(UUID newLandId)
	{
		landId = newLandId;
	}

	/**
	 * Gets crop to be sowed.<p>
	 *
	 * @return crop to be sowed
	 */
	public Crop getCrop()
	{
		return crop;
	}

	/**
	 * Sets crop to be sowed.<p>
	 *
	 * @param newCrop crop to be sowed
	 */
	public void setCrop(Crop newCrop)
	{
		crop = newCrop;
	}

	/**
	 * Gets number of blocks to be sowed.<p>
	 *
	 * @return number of blocks to be sowed
	 */
	public Short getAskedBlocks()
	{
		return askedBlocks;
	}

	/**
	 * Sets number of blocks to be sowed.<p>
	 *
	 * @param newAskedBlocks number of blocks to be sowed
	 */
	public void setAskedBlocks(Short newAskedBlocks)
	{
		askedBlocks = newAskedBlocks;
	}

	/**
	 * Gets comment for sowing.<p>
	 *
	 * @return comment for sowing
	 */
	public String getComment()
	{
		return comment;
	}

	/**
	 * Sets comment for sowing.<p>
	 *
	 * @param newComment comment for sowing
	 */
	@JsonSetter("comment")
	public void setComment(String newComment)
	{
		comment = StringUtils.trimToNull(newComment);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == null) { return false; }
		if (obj == this) { return true; }
		if (obj.getClass() != getClass()) {
			return false;
		}

		var rhs = (AskBlockAction)obj;
		return new EqualsBuilder()
			.append(this.getLandId(), rhs.getLandId())
			.append(this.getCrop(), rhs.getCrop())
			.append(this.getAskedBlocks(), rhs.getAskedBlocks())
			.append(this.getComment(), rhs.getComment())
			.isEquals();
	}

	@Override
	public int hashCode()
	{
		return new HashCodeBuilder(23609, 29761)
			.append(getLandId())
			.append(getCrop())
			.append(getAskedBlocks())
			.append(getComment())
		.toHashCode();
	}
}
