package com.inventory.vehicle.common.config;

import java.util.Arrays;
import java.util.LinkedHashSet;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class DatabaseMigrationConfig {

    private static final String FLYWAY_BEAN_NAME = "flyway";
    private static final String ENTITY_MANAGER_FACTORY_BEAN_NAME = "entityManagerFactory";

    @Bean(initMethod = "migrate")
    Flyway flyway(DataSource dataSource, Environment environment) {
        String[] locations = environment
                .getProperty("spring.flyway.locations", "classpath:db/migration")
                .split(",");
        return Flyway.configure()
                .dataSource(dataSource)
                .locations(Arrays.stream(locations).map(String::trim).toArray(String[]::new))
                .baselineOnMigrate(true)
                .baselineVersion("8")
                .load();
    }

    @Bean
    static BeanFactoryPostProcessor flywayBeforeJpa() {
        return beanFactory -> {
            if (!beanFactory.containsBeanDefinition(ENTITY_MANAGER_FACTORY_BEAN_NAME)) {
                return;
            }
            BeanDefinition entityManagerFactory = beanFactory.getBeanDefinition(ENTITY_MANAGER_FACTORY_BEAN_NAME);
            LinkedHashSet<String> dependencies = new LinkedHashSet<>();
            String[] existingDependencies = entityManagerFactory.getDependsOn();
            if (existingDependencies != null) {
                dependencies.addAll(Arrays.asList(existingDependencies));
            }
            dependencies.add(FLYWAY_BEAN_NAME);
            entityManagerFactory.setDependsOn(dependencies.toArray(String[]::new));
        };
    }
}
