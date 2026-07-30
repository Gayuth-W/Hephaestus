package com.gayuth.hephaestus.dto;

/** A caller -> callee dependency, by span id. */
public record GraphEdge(String from, String to) {
}
