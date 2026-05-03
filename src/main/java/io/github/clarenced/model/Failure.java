package io.github.clarenced.model;


import com.fasterxml.jackson.annotation.JsonProperty;

public record Failure(@JsonProperty("cause") String cause, @JsonProperty("stackTrace") String stackTrace) {
}
