package br.univille.sendhemosc;

import br.univille.sendhemosc.config.SendHemoscProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Ponto de entrada do prototipo Send Hemosc.
 */
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(SendHemoscProperties.class)
public class SendHemoscApplication {

    public static void main(final String[] args) {
        SpringApplication.run(SendHemoscApplication.class, args);
    }
}
