package com.barofit.routine.config;
import com.barofit.common.migration.ModuleSchema;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration(proxyBeanMethods = false)
class RoutineSchemaConfig {
    @Bean ModuleSchema routineSchema() { return new ModuleSchema("routine"); }
}
