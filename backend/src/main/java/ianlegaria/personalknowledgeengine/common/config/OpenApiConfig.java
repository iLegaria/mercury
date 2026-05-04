package ianlegaria.personalknowledgeengine.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Personal Knowledge Engine API")
                        .description("""
                                A personal RAG (Retrieval-Augmented Generation) knowledge engine.
                                Upload documents, ask questions answered by your own content, \
                                generate quizzes, and study with spaced-repetition flashcards.

                                Key features:
                                - Semantic search powered by Cohere embeddings + pgvector
                                - RAG pipeline: retrieve relevant chunks → generate answers with Cohere Command-R
                                - LLM-graded quizzes with STRICT and OPEN modes
                                - Flashcard spaced repetition using the SM-2 algorithm
                                - Async document ingestion via RabbitMQ
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Ian Legaria")
                                .email("il.legaria@gmail.com")));
    }
}
