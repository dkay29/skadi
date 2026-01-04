package com.dkay229.skadi.jdbc;

import com.dkay229.skadi.jdbc.spi.DefaultDriverManagerJdbcConnectionProvider;
import com.dkay229.skadi.jdbc.spi.JdbcClientFactory;
import com.dkay229.skadi.jdbc.spi.JdbcConnectionProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class JdbcConfig {

    /**
     * Default provider (DriverManager). Corporate deployments can override by providing their own beans.
     */
    @Bean
    public JdbcConnectionProvider defaultJdbcConnectionProvider() {
        return new DefaultDriverManagerJdbcConnectionProvider();
    }

    @Bean
    public JdbcClientFactory jdbcClientFactory(SkadiJdbcProperties props, List<JdbcConnectionProvider> providers) {
        return new JdbcClientFactory(props, providers);
    }
}
