package io.github.vzwingma.finances.budget.services.operations.config;


import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Contact;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.info.License;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import jakarta.enterprise.context.ApplicationScoped;

@OpenAPIDefinition(
        tags = {
                @Tag(name="application", description="Gestion de budget"),
                @Tag(name="operations", description="Operations en rapport avec les opérations")
        },
        info = @Info(
                title = "Gestion de Budgets : µService d'opérations",
                description = "API du service de budgets",
                version = "17.0.0",
                contact = @Contact(
                        name = "Vincent Zwingmann",
                        email = "vincent.zwingmann@github.com"),
                license = @License(
                        name = "Apache 2.0",
                        url = "https://www.apache.org/licenses/LICENSE-2.0.html"))
)
@ApplicationScoped
public class OpenAPIConfig {
}
