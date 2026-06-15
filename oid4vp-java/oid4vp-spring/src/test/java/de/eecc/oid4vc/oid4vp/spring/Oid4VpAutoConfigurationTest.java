package de.eecc.oid4vc.oid4vp.spring;

import de.eecc.oid4vc.oid4vp.api.Oid4Vp;
import de.eecc.oid4vc.oid4vp.api.Oid4VpOptions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class Oid4VpAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(Oid4VpAutoConfiguration.class));

    @Test
    void createsOid4VpBeanFromProperties() {
        contextRunner
                .withPropertyValues(
                        "oid4vp.response-uri=https://example.com/api/auth/oid4vp/response",
                        "oid4vp.verifier-url=http://localhost:3000/api/verifier")
                .run(context -> assertThat(context).hasSingleBean(Oid4Vp.class));
    }

    @Test
    void respectsCustomOid4VpBean() {
        Oid4Vp custom = Oid4Vp.create(Oid4VpOptions.builder()
                .responseUri("https://custom.example/response")
                .build());

        contextRunner
                .withPropertyValues("oid4vp.response-uri=https://example.com/response")
                .withBean("customOid4Vp", Oid4Vp.class, () -> custom)
                .run(context -> assertThat(context.getBean(Oid4Vp.class)).isSameAs(custom));
    }
}
