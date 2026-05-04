package ianlegaria.personalknowledgeengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PersonalKnowledgeEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(PersonalKnowledgeEngineApplication.class, args);
    }

}
