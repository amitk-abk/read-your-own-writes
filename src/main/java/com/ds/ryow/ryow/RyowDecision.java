package com.ds.ryow.ryow;

public record RyowDecision(String source, boolean primary) {
	public static RyowDecision primaryDecision() {
		return new RyowDecision("readWrite", true);
	}

	public static RyowDecision replica(String source) {
		return new RyowDecision(source, false);
	}
}