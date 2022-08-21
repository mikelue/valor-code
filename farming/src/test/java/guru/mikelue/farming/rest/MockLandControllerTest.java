package guru.mikelue.farming.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import org.junit.jupiter.api.Test;

import guru.mikelue.farming.model.Land;
import guru.mikelue.farming.repos.cassandra.LandLogRepos;
import guru.mikelue.farming.repos.jpa.LandRepos;
import guru.mikelue.farming.web.CodeAndDetailException;
import guru.mikelue.misc.testlib.AbstractTestBase;

import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

public class MockLandControllerTest extends AbstractTestBase {
	@Injectable
	private LandRepos mockLandRepos;
	@Injectable
	private LandLogRepos mockLandLogRepos;

	@Tested
	private LandController testedController;

	public MockLandControllerTest() {}

	/**
	 * Tests getting of not-existing land by id.
	 */
	@Test
	void getByIdWithMissed()
	{
		final var sampleLand = new Land();
		sampleLand.setId(UUID.randomUUID());
		sampleLand.setName("java apple");

		new Expectations() {{
			mockLandRepos.findById(sampleLand.getId());
			result = Optional.empty();
		}};

		/**
		 * Tests not existing land
		 */
		var testedResult = testedController.getById(
			sampleLand.getId()
		);

		StepVerifier.create(testedResult)
			.expectErrorSatisfies(e -> assertThat(e)
				.isInstanceOf(ResponseStatusException.class)
				.hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
				.hasMessageContainingAll("Unable", sampleLand.getId().toString())
			)
			.verify();
		// :~)
	}

	/**
	 * Tests exception for duplicated name while adding.
	 */
	@Test
	void addNewWithDuplicatedName()
	{
		new Expectations() {{
			mockLandRepos.addNewWithBlocks((Land)any);
			result = new DataIntegrityViolationException("unq_vc_land__ld_name");
		}};

		/**
		 * Tests not existing land
		 */
		var testedResult = testedController.addNew(Mono.just(new Land()));

		StepVerifier.create(testedResult)
			.expectErrorSatisfies(MockLandControllerTest::assertDuplicatedName)
			.verify();
		// :~)
	}

	/**
	 * Tests exception for duplicated name while modifiying.
	 */
	@Test
	void modifyWithDuplicatedName()
	{
		new Expectations() {{
			mockLandRepos.save((Land)any);
			result = new DataIntegrityViolationException("unq_vc_land__ld_name");
		}};

		/**
		 * Tests not existing land
		 */
		var testedResult = testedController.modify(
			UUID.randomUUID(),
			Mono.just(new Land())
		);

		StepVerifier.create(testedResult)
			.expectErrorSatisfies(MockLandControllerTest::assertDuplicatedName)
			.verify();
		// :~)
	}

	private static void assertDuplicatedName(Throwable t)
	{
		assertThat(t)
			.isInstanceOf(CodeAndDetailException.class)
			.hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT)
			.hasFieldOrPropertyWithValue("codeAndDetail.code", 1);
	}
}
