package io.casehub.eidos.vocab;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ThomasKilmannTermTest {

    @Test
    void collaborative_alias_on_collaborating() {
        assertThat(ThomasKilmannTerm.COLLABORATING.aliases())
            .contains("collaborative");
    }

    @Test
    void cooperative_alias_still_on_collaborating() {
        assertThat(ThomasKilmannTerm.COLLABORATING.aliases())
            .contains("cooperative");
    }
}
