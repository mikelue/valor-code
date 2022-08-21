package guru.mikelue.farming.repos.jpa;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import guru.mikelue.farming.model.Land;

public interface LandRepos extends JpaRepository<Land, UUID>, CustomizedLandRepos {
	Land findLandByName(String name);
}
