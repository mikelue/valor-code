package guru.mikelue.farming.service;

import guru.mikelue.farming.model.Crop;
import guru.mikelue.farming.model.Land;

public class UnsuitableCropException extends RuntimeException {
	private final Land land;
	private final Crop targetCrop;

	public UnsuitableCropException(Land newLand, Crop newTargetCrop)
	{
		super(String.format(
			"Unable to grown crop[%s] in land: [%s](%s). Climate: [%s].",
			newTargetCrop,
			newLand.getName(), newLand.getId(), newLand.getClimate()
		));

		land = newLand;
		targetCrop = newTargetCrop;
	}

	public Land getLand()
	{
		return land;
	}

	public Crop getTargetCrop()
	{
		return targetCrop;
	}
}
