package guru.mikelue.farming.model;

import java.time.Instant;
import java.util.UUID;
import javax.persistence.*;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.annotations.Type;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import guru.mikelue.farming.validate.Groups;

@Entity
@Table(name="vc_land")
@DefaultJsonConfig
@JsonPropertyOrder({ "id", "name", "climate", "size" })
public class Land {
	@Id
	@GeneratedValue
	@Column(name="ld_id")
	@JsonProperty("id")
	private UUID id;

	@JsonProperty("name")
	@Column(name="ld_name", unique=true, nullable=false)
	@NotNull(groups={Default.class, Groups.WhenUpdate.class})
	private String name;

	@JsonProperty("size")
	@Column(name="ld_size", nullable=false, updatable=false)
	@NotNull @Min(1)
	private Short size;

	@JsonProperty("climate")
	@Type(type="pgsql_enum") @Enumerated(EnumType.STRING)
	@Column(name="ld_climate", nullable=false, updatable=false)
	@NotNull
	private Climate climate;

	@Column(name="ld_creation_time", updatable=false)
	private Instant creationTime;

	public Land() {}
	public Land(UUID id)
	{
		this.id = id;
	}

	/**
	 * Sets id of land.<p>
	 *
	 * @param newId The id of
	 *
	 * @see #getId()
	 */
	public void setId(UUID newId) { this.id = newId; }
	/**
	 * Gets id of land.<p>
	 *
	 * @return id of
	 *
	 * @see #setId()
	 */
	public UUID getId() { return this.id; }

	/**
	 * Gets name of land.<p>
	 *
	 * @return name of land
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Sets name of land.<p>
	 *
	 * @param newName name of land
	 */
	@JsonSetter("name")
	public void setName(String newName)
	{
		name = StringUtils.trimToNull(newName);
	}

	/**
	 * Gets size of land.<p>
	 *
	 * @return size of land
	 */
	public Short getSize()
	{
		return size;
	}

	/**
	 * Sets size of land.<p>
	 *
	 * @param newSize size of land
	 */
	public void setSize(Short newSize)
	{
		size = newSize;
	}

	/**
	 * Gets climate of land.<p>
	 *
	 * @return climate of land
	 */
	public Climate getClimate()
	{
		return climate;
	}

	/**
	 * Sets climate of land.<p>
	 *
	 * @param newClimate climate of land
	 */
	public void setClimate(Climate newClimate)
	{
		climate = newClimate;
	}

	/**
	 * Gets time of creation for this record.<p>
	 *
	 * @return time of creation for this record
	 */
	public Instant getCreationTime()
	{
		return creationTime;
	}

	/**
	 * Sets time of creation for this record.<p>
	 *
	 * @param newCreationTime time of creation for this record
	 */
	public void setCreationTime(Instant newCreationTime)
	{
		creationTime = newCreationTime;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == null) { return false; }
		if (obj == this) { return true; }

		if (!getClass().isInstance(obj)) {
			return false;
		}

		var another = (Land)obj;
		return new EqualsBuilder()
			.append(this.getId(), another.getId())
			.isEquals();
	}
	@Override
	public int hashCode()
	{
		return new HashCodeBuilder(17449, 7951247)
			.append(this.getId())
			.toHashCode();
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(this)
			.append("name", getName())
			.append("climate", getClimate())
			.append("size", getSize())
		.build();
	}
}
