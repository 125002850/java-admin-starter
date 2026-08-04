package com.oigit.admin.boot.config;

import com.oigit.admin.core.translation.TranslationEngine;
import com.oigit.admin.core.translation.TranslationProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class TranslationConfiguration {

    @Bean
    public TranslationEngine translationEngine(List<TranslationProvider> providers) {
        return new TranslationEngine(providers);
    }
}
