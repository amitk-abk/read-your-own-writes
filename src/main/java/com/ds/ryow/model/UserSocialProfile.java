package com.ds.ryow.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record UserSocialProfile(
		UUID id,
		String username,
		String email,
		String firstName,
		String lastName,
		LocalDate dateOfBirth,
		String phoneNumber,
		String country,
		String city,
		String bio,
		String avatarUrl,
		String websiteUrl,
		String interests,
		String occupation,
		String employer,
		String education,
		String relationshipStatus,
		String privacyLevel,
		String preferredLanguage,
		String timezone,
		OffsetDateTime updatedAt) {
}