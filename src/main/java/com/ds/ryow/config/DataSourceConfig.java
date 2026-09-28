package com.ds.ryow.config;

import javax.sql.DataSource;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class DataSourceConfig {

	@Bean(name = "readWriteDataSource")
	@ConfigurationProperties("app.datasource.read-write")
	DataSource readWriteDataSource() {
		return DataSourceBuilder.create().build();
	}

	@Bean(name = "readOnlyFirstDataSource")
	@ConfigurationProperties("app.datasource.read-only-first")
	DataSource readOnlyFirstDataSource() {
		return DataSourceBuilder.create().build();
	}

	@Bean(name = "readOnlySecondDataSource")
	@ConfigurationProperties("app.datasource.read-only-second")
	DataSource readOnlySecondDataSource() {
		return DataSourceBuilder.create().build();
	}

	@Bean(name = "readWriteJdbcTemplate")
	JdbcTemplate readWriteJdbcTemplate(@Qualifier("readWriteDataSource") DataSource readWriteDataSource) {
		return new JdbcTemplate(readWriteDataSource);
	}

	@Bean(name = "readOnlyFirstJdbcTemplate")
	JdbcTemplate readOnlyFirstJdbcTemplate(@Qualifier("readOnlyFirstDataSource") DataSource readOnlyFirstDataSource) {
		return new JdbcTemplate(readOnlyFirstDataSource);
	}

	@Bean(name = "readOnlySecondJdbcTemplate")
	JdbcTemplate readOnlySecondJdbcTemplate(@Qualifier("readOnlySecondDataSource") DataSource readOnlySecondDataSource) {
		return new JdbcTemplate(readOnlySecondDataSource);
	}
}