package io.casehub.eidos.api;

public record VocabularyInfo(String uri, String name, String version, String description) {
    public VocabularyInfo {
        if (uri == null || uri.isBlank())
            throw new IllegalArgumentException("VocabularyInfo.uri must not be blank");
        if (name == null) name = "";
        if (version == null) version = "";
        if (description == null) description = "";
    }
}
