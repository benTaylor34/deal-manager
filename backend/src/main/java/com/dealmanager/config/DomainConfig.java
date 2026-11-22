package com.dealmanager.config;

import com.dealmanager.domain.ApprovalPolicy;
import com.dealmanager.domain.DealStateMachine;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Clock;

@Configuration
public class DomainConfig {

    @Bean
    ApprovalPolicy approvalPolicy(@Value("${deals.approval.rm-discount-limit}") BigDecimal rmDiscountLimit) {
        return new ApprovalPolicy(rmDiscountLimit);
    }

    @Bean
    DealStateMachine dealStateMachine() {
        return new DealStateMachine();
    }

    /** Injected everywhere "now" is needed, so tests can pin time. */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    /**
     * Write BigDecimal as a JSON string ("1234.5000"), never a number. JavaScript parses
     * JSON numbers into doubles, which would quietly round money in the browser.
     */
    @Bean
    Jackson2ObjectMapperBuilderCustomizer bigDecimalAsString() {
        return builder -> builder.serializerByType(BigDecimal.class, new JsonSerializer<BigDecimal>() {
            @Override
            public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider serializers)
                    throws IOException {
                gen.writeString(value.toPlainString());
            }
        });
    }
}
