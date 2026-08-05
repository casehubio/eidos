# io.casehub.eidos.api.spi.VocabularyRegistrar

**Package:** `io.casehub.eidos.api.spi`

**Kind:** `interface`

CDI SPI for vocabulary registration. Implement as an `@ApplicationScoped` bean
to auto-register a vocabulary enum with `io.casehub.eidos.api.VocabularyRegistry`
at startup. The enum class must carry `io.casehub.eidos.api.VocabularyMetadata`.

## Methods

### `public abstract java.lang.Class<? extends java.lang.Enum<? extends io.casehub.eidos.api.VocabularyTerm>> vocabulary()`
