package com.example.demo.config;

import javax.sql.DataSource;
import jakarta.persistence.EntityManagerFactory;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.lookup.DataSourceLookupFailureException;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableJpaRepositories(
    basePackages = "com.example.demo.repository.employee",
    entityManagerFactoryRef = "employeeEntityManagerFactory",
    transactionManagerRef = "employeeTransactionManager"
)
public class EmployeeDataSourceConfig {

    @Bean(name = "employeeDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.employee")
    public DataSource employeeDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "employeeJpaProperties")
    @ConfigurationProperties(prefix = "spring.jpa.employee")
    public JpaProperties employeeJpaProperties() {
        return new JpaProperties();
    }

    @Bean(name = "employeeEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean employeeEntityManagerFactory(
            @Qualifier("employeeDataSource") DataSource employeeDataSource,
            @Qualifier("employeeJpaProperties") JpaProperties employeeJpaProperties
    ) {
        if (employeeDataSource == null) {
            throw new DataSourceLookupFailureException("Employee DataSource bean not found");
        }

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setShowSql(employeeJpaProperties.isShowSql());
        vendorAdapter.setDatabasePlatform(employeeJpaProperties.getDatabasePlatform());
        vendorAdapter.setGenerateDdl(employeeJpaProperties.isGenerateDdl());

        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(employeeDataSource);
        // FIX: Scan the entire model package
        emf.setPackagesToScan(
            "com.example.demo.model" // <-- FIXED: now includes User and all sub-entities
        );
        emf.setJpaVendorAdapter(vendorAdapter);
        emf.setJpaPropertyMap(employeeJpaProperties.getProperties());

        return emf;
    }

    @Bean(name = "employeeTransactionManager")
    public PlatformTransactionManager employeeTransactionManager(
            @Qualifier("employeeEntityManagerFactory") EntityManagerFactory employeeEmf
    ) {
        return new JpaTransactionManager(employeeEmf);
    }
}