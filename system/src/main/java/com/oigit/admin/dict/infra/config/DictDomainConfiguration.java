package com.oigit.admin.dict.infra.config;

import com.oigit.admin.dict.domain.repository.GlobalDictItemRepository;
import com.oigit.admin.dict.domain.repository.GlobalDictTypeRepository;
import com.oigit.admin.dict.domain.service.GlobalDictDomainService;
import com.oigit.admin.dict.domain.service.EnumDictionaryPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.ObjectProvider;

@Configuration(proxyBeanMethods = false)
public class DictDomainConfiguration {

    @Bean
    GlobalDictDomainService globalDictDomainService(
            GlobalDictTypeRepository globalDictTypeRepository,
            GlobalDictItemRepository globalDictItemRepository,
            ObjectProvider<EnumDictionaryPolicy> enumDictionaryPolicyProvider
    ) {
        return new GlobalDictDomainService(
                globalDictTypeRepository,
                globalDictItemRepository,
                enumDictionaryPolicyProvider.getIfAvailable(EnumDictionaryPolicy::none)
        );
    }
}
