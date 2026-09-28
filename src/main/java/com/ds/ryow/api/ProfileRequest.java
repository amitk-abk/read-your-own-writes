package com.ds.ryow.api;

import java.time.LocalDate;

public record ProfileRequest(
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
		String timezone) {
}