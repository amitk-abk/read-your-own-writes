package com.ds.ryow.api;

import java.util.UUID;

import com.ds.ryow.model.UserSocialProfile;
import com.ds.ryow.ryow.RyowRouter;
import com.ds.ryow.service.ProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profiles")
public class ProfileController {
	private final ProfileService service;

	public ProfileController(ProfileService service) {
		this.service = service;
	}

	@PostMapping
	public ResponseEntity<Void> create(@RequestBody ProfileRequest request,
			@RequestHeader(value = RyowRouter.HEADER_NAME, required = false) String ignoredHeader) {
		ProfileService.WriteResult result = service.create(request);
		return ResponseEntity.created(java.net.URI.create("/api/profiles/" + result.id()))
				.header(RyowRouter.HEADER_NAME, Long.toString(result.ryowToken()))
				.build();
	}

	@PutMapping("/{id}")
	public ResponseEntity<Void> update(@PathVariable UUID id, @RequestBody ProfileRequest request,
			@RequestHeader(value = RyowRouter.HEADER_NAME, required = false) String ignoredHeader) {
		return service.update(id, request)
			.map(this::noContentWithToken)
			.orElseGet(() -> ResponseEntity.notFound().build());
	}

	private ResponseEntity<Void> noContentWithToken(ProfileService.WriteResult result) {
		return ResponseEntity.noContent()
				.header(RyowRouter.HEADER_NAME, Long.toString(result.ryowToken())).build();
	}

	@GetMapping("/{id}")
	public ResponseEntity<UserSocialProfile> find(@PathVariable UUID id,
			@RequestHeader(value = RyowRouter.HEADER_NAME, required = false) String ryowHeader) {
		ProfileService.ReadResult result = service.find(id, ryowHeader);
		if (result.profile().isEmpty()) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok().header("X-RYOW-Source", result.source()).body(result.profile().get());
	}
}