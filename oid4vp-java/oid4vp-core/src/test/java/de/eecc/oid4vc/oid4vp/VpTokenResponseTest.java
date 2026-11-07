package de.eecc.oid4vc.oid4vp;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VpTokenResponseTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void vpTokenJsonIsTheCredentialQueryMap() throws Exception {
        VpTokenResponse.VpToken token = VpTokenResponse.VpToken.oneEach("eyJhb", List.of("my_credential"));

        String json = mapper.writeValueAsString(token);

        assertThat(json).isEqualTo("{\"my_credential\":[\"eyJhb\"]}");
        VpTokenResponse.VpToken read = mapper.readValue(json, VpTokenResponse.VpToken.class);
        assertThat(read.presentations()).containsEntry("my_credential", List.of("eyJhb"));
    }
}
