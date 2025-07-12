package com.example.demo.config;

import javax.sql.DataSource;
import jakarta.persistence.EntityManagerFactory;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableJpaRepositories(
    basePackages = "com.example.demo.repository.customer",
    entityManagerFactoryRef = "customerEntityManagerFactory",
    transactionManagerRef = "customerTransactionManager"
)
public class CustomerDataSourceConfig {

    @Primary
    @Bean(name = "customerDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.customer")
    public DataSource customerDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Primary
    @Bean(name = "customerJpaProperties")
    @ConfigurationProperties(prefix = "spring.jpa.customer")
    public JpaProperties customerJpaProperties() {
        return new JpaProperties();
    }

    @Primary
    @Bean(name = "customerEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean customerEntityManagerFactory(
            @Qualifier("customerDataSource") DataSource customerDataSource,
            @Qualifier("customerJpaProperties") JpaProperties customerJpaProperties
    ) {
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setShowSql(customerJpaProperties.isShowSql());
        vendorAdapter.setDatabasePlatform(customerJpaProperties.getDatabasePlatform());
        vendorAdapter.setGenerateDdl(customerJpaProperties.isGenerateDdl());

        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(customerDataSource);
        emf.setPackagesToScan("com.example.demo.model.customer");
        emf.setJpaVendorAdapter(vendorAdapter);
        emf.setJpaPropertyMap(customerJpaProperties.getProperties());
        return emf;
    }

    @Primary
    @Bean(name = "customerTransactionManager")
    public PlatformTransactionManager customerTransactionManager(
            @Qualifier("customerEntityManagerFactory") EntityManagerFactory customerEmf
    ) {
        return new JpaTransactionManager(customerEmf);
    }
}
