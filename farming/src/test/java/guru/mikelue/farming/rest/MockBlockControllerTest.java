package guru.mikelue.farming.rest;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import guru.mikelue.farming.model.AskBlockAction;
import guru.mikelue.farming.model.Block;
import guru.mikelue.farming.model.Climate;
import guru.mikelue.farming.model.Crop;
import guru.mikelue.farming.model.Land;
import guru.mikelue.farming.repos.jpa.BlockRepos;
import guru.mikelue.farming.service.FarmingService;
import guru.mikelue.farming.service.UnsuitableCropException;
import guru.mikelue.farming.web.CodeAndDetailException;
import guru.mikelue.misc.testlib.AbstractTestBase;

import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.*;

public class MockBlockControllerTest extends AbstractTestBase {
	@Injectable
	private FarmingService mockFarmingService;
	@Injectable
	private BlockRepos mockBlockRepos;

	@Tested
	private BlockController testedController;

	public MockBlockControllerTest() {}

	/**
	 * Tests exception for improper crops for climate.
	 */
	@Test
	void askSowingWithImproperCrop()
	{
		final var sampleLand = new Land();
		sampleLand.setClimate(Climate.Polar);
		final var sampleCrop = Crop.Spinach;

		new Expectations() {{
			mockFarmingService.askSow((AskBlockAction)any);
			result = Flux.<Block>error(
				new UnsuitableCropException(
					sampleLand, sampleCrop
				)
			);
		}};

		/**
		 * Tests not existing land
		 */
		var testedResult = testedController.askSowing(
			UUID.randomUUID(), Mono.just(new AskBlockAction())
		);

		StepVerifier.create(testedResult)
			.expectErrorSatisfies(e -> assertThat(e)
				.isInstanceOf(CodeAndDetailException.class)
				.hasCauseInstanceOf(UnsuitableCropException.class)
				.hasFieldOrPropertyWithValue("codeAndDetail.code", 1)
				.extracting("codeAndDetail.detail", MAP)
				.hasFieldOrPropertyWithValue("climate", sampleLand.getClimate())
				.hasFieldOrPropertyWithValue("crop", sampleCrop)
			)
			.verify();
		// :~)
	}
}
