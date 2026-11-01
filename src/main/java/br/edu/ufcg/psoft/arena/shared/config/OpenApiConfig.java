package br.edu.ufcg.psoft.arena.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI arenaOpenApi() {
        return new OpenAPI().info(new Info()
                .title("PSoft Arena API")
                .version("v1")
                .description("""
                        API da plataforma PSoft Arena.

                        Recursos sob /api/v1 com JSON em portugues. Criacoes respondem 201 com Location,
                        remocoes respondem 204 e erros usam ProblemDetail (400 validacao, 404 inexistente,
                        409 regra de negocio, duplicata ou FK).
                        """));
    }
}
