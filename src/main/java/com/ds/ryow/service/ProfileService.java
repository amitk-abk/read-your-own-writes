package com.ds.ryow.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import com.ds.ryow.api.ProfileRequest;
import com.ds.ryow.model.UserSocialProfile;
import com.ds.ryow.repository.ProfileRepository;
import com.ds.ryow.ryow.RyowDecision;
import com.ds.ryow.ryow.RyowRouter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ProfileService {
	private final ProfileRepository repository;
	private final RyowRouter router;
	private final JdbcTemplate readWrite;
	private final JdbcTemplate readOnlyFirst;
	private final JdbcTemplate readOnlySecond;

	public ProfileService(ProfileRepository repository, RyowRouter router,
			@Qualifier("readWriteJdbcTemplate") JdbcTemplate readWrite,
			@Qualifier("readOnlyFirstJdbcTemplate") JdbcTemplate readOnlyFirst,
			@Qualifier("readOnlySecondJdbcTemplate") JdbcTemplate readOnlySecond) {
		this.repository = repository;
		this.router = router;
		this.readWrite = readWrite;
		this.readOnlyFirst = readOnlyFirst;
		this.readOnlySecond = readOnlySecond;
	}

	public WriteResult create(ProfileRequest request) {
		UUID id = UUID.randomUUID();
		OffsetDateTime updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
		repository.insert(id, request, updatedAt);
		long token = router.nowEpochMillis();
		return new WriteResult(id, token);
	}

	public Optional<WriteResult> update(UUID id, ProfileRequest request) {
		OffsetDateTime updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
		boolean updated = repository.update(id, request, updatedAt);
		long token = router.nowEpochMillis();
		return updated ? Optional.of(new WriteResult(id, token)) : Optional.empty();
	}

	public ReadResult find(UUID id, String ryowHeader) {
		RyowDecision decision = router.decide(ryowHeader);
		JdbcTemplate template = decision.primary() ? null
				: "readOnlyFirst".equals(decision.source()) ? readOnlyFirst : readOnlySecond;
		Optional<UserSocialProfile> profile = decision.primary()
				? repository.findById(id, readWrite)
				: repository.findById(id, template);
		return new ReadResult(profile, decision.source());
	}

	public record WriteResult(UUID id, long ryowToken) { }
	public record ReadResult(Optional<UserSocialProfile> profile, String source) { }
}