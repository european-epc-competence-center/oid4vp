package de.eecc.oid4vc.oid4vp.spring;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.eecc.oid4vc.oid4vp.api.Oid4Vp;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(Oid4Vp.class)
@EnableConfigurationProperties(Oid4VpProperties.class)
public class Oid4VpAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public Oid4Vp oid4Vp(Oid4VpProperties properties, ObjectProvider<ObjectMapper> objectMapper) {
        var builder = Oid4Vp.builder().options(properties.toOptions());
        objectMapper.ifAvailable(builder::objectMapper);
        return builder.build();
    }

    @Bean
    @ConditionalOnWebApplication
    @ConditionalOnClass(name = "org.springframework.web.bind.annotation.RestController")
    @ConditionalOnMissingBean
    public Oid4VpExceptionHandler oid4VpExceptionHandler() {
        return new Oid4VpExceptionHandler();
    }
}
