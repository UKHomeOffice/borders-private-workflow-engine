package uk.gov.homeoffice.borders.workflow.config;


import org.camunda.bpm.engine.impl.cfg.ProcessEnginePlugin;
import org.camunda.spin.plugin.impl.SpinProcessEnginePlugin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ApplicationConfiguration {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public ProcessEnginePlugin spinProcessEnginePlugin() {
        return new SpinProcessEnginePlugin();
    }
}
