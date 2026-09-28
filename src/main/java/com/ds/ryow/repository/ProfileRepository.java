package com.ds.ryow.repository;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import com.ds.ryow.api.ProfileRequest;
import com.ds.ryow.model.UserSocialProfile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ProfileRepository {
	private static final String SELECT = """
			SELECT id, username, email, first_name, last_name, date_of_birth, phone_number,
			country, city, bio, avatar_url, website_url, interests, occupation, employer,
			education, relationship_status, privacy_level, preferred_language, timezone, updated_at
			FROM user_social_profile WHERE id = ?""";

	private final JdbcTemplate readWrite;

	public ProfileRepository(JdbcTemplate readWriteJdbcTemplate) {
		this.readWrite = readWriteJdbcTemplate;
	}

	public void insert(UUID id, ProfileRequest request, OffsetDateTime updatedAt) {
		readWrite.update("""
				INSERT INTO user_social_profile (id, username, email, first_name, last_name,
				date_of_birth, phone_number, country, city, bio, avatar_url, website_url, interests,
				occupation, employer, education, relationship_status, privacy_level,
				preferred_language, timezone, updated_at)
				VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
				id, request.username(), request.email(), request.firstName(), request.lastName(),
				request.dateOfBirth(), request.phoneNumber(), request.country(), request.city(),
				request.bio(), request.avatarUrl(), request.websiteUrl(), request.interests(),
				request.occupation(), request.employer(), request.education(),
				request.relationshipStatus(), request.privacyLevel(), request.preferredLanguage(),
				request.timezone(), updatedAt);
	}

	public boolean update(UUID id, ProfileRequest request, OffsetDateTime updatedAt) {
		return readWrite.update("""
				UPDATE user_social_profile SET username = ?, email = ?, first_name = ?, last_name = ?,
				date_of_birth = ?, phone_number = ?, country = ?, city = ?, bio = ?, avatar_url = ?,
				website_url = ?, interests = ?, occupation = ?, employer = ?, education = ?,
				relationship_status = ?, privacy_level = ?, preferred_language = ?, timezone = ?,
				updated_at = ? WHERE id = ?""",
				request.username(), request.email(), request.firstName(), request.lastName(),
				request.dateOfBirth(), request.phoneNumber(), request.country(), request.city(),
				request.bio(), request.avatarUrl(), request.websiteUrl(), request.interests(),
				request.occupation(), request.employer(), request.education(),
				request.relationshipStatus(), request.privacyLevel(), request.preferredLanguage(),
				request.timezone(), updatedAt, id) == 1;
	}

	public Optional<UserSocialProfile> findById(UUID id, JdbcTemplate jdbcTemplate) {
		return jdbcTemplate.query(SELECT, this::mapProfile, id).stream().findFirst();
	}

	private UserSocialProfile mapProfile(ResultSet resultSet, int rowNumber) throws SQLException {
		return new UserSocialProfile(
				resultSet.getObject("id", UUID.class), resultSet.getString("username"),
				resultSet.getString("email"), resultSet.getString("first_name"),
				resultSet.getString("last_name"), resultSet.getObject("date_of_birth", Date.class) == null
						? null : resultSet.getObject("date_of_birth", Date.class).toLocalDate(),
				resultSet.getString("phone_number"), resultSet.getString("country"),
				resultSet.getString("city"), resultSet.getString("bio"), resultSet.getString("avatar_url"),
				resultSet.getString("website_url"), resultSet.getString("interests"),
				resultSet.getString("occupation"), resultSet.getString("employer"),
				resultSet.getString("education"), resultSet.getString("relationship_status"),
				resultSet.getString("privacy_level"), resultSet.getString("preferred_language"),
				resultSet.getString("timezone"), resultSet.getObject("updated_at", OffsetDateTime.class));
	}
}