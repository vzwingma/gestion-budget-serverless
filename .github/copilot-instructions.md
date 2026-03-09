# Copilot Instructions – gestion-budget-serverless

Il s'agit du **backend Quarkus/Java 21** de l'application de gestion de budget, déployé sous forme de fonctions AWS Lambda natives. Le frontend se trouve dans le dépôt compagnon [`gestion-budget-ihm`](../gestion-budget-ihm) (React/TypeScript).

## Build, Test et Lint

```bash
# Construire tous les modules (mode JVM)
mvn clean package

# Construire un seul module
mvn clean package -f comptes/pom.xml

# Exécuter tous les tests
mvn test

# Exécuter une classe de test spécifique
mvn test -Dtest=ComptesServiceTest

# Exécuter une méthode de test spécifique
mvn test -Dtest=ComptesServiceTest#testGetComptes

# Exécuter les tests d'un seul module
mvn test -f operations/pom.xml

# Construire l'exécutable Linux natif pour Lambda (nécessite GraalVM/Mandrel)
mvn clean package -Pnative -Dquarkus.native.container-build=true

# Exécuter l'analyse SonarCloud (nécessite sonar.token)
mvn verify -Psonar
```

## Architecture

### Projet Maven multi-modules
```
gestion-budget-serverless/
├── communs/          # Bibliothèque partagée : classes de base, modèles, sécurité, exceptions
├── parametrages/     # Microservice : paramètres système  → /parametres/v2/
├── utilisateurs/     # Microservice : auth/profils utilisateur → /utilisateurs/v2/
├── comptes/          # Microservice : comptes bancaires   → /comptes/v2/
└── operations/       # Microservice : budgets et opérations → /budgets/v2/
```

Tous les microservices (`comptes`, `operations`, `parametrages`, `utilisateurs`) dépendent de `communs` et suivent la même structure interne en couches.

### Couches de l'architecture hexagonale (par microservice)
```
api/          – Contrôleurs REST JAX-RS, enums des chemins d'API, surcharges exception/sécurité
business/     – Logique métier (services @ApplicationScoped), interfaces de ports, modèles métier
spi/          – Adaptateurs base de données (MongoDB Panache), providers REST inter-services
config/       – Classes de configuration Quarkus (OpenAPI, hints de réflexion GraalVM)
utils/        – Classes utilitaires métier
```

### Patterns clés du framework

Les **ressources REST** étendent `AbstractAPIInterceptors` (de `communs`) et utilisent les annotations JAX-RS standard :
```java
@Path(ComptesAPIEnum.COMPTES_BASE)
public class ComptesResource extends AbstractAPIInterceptors {
    @Inject IComptesAppProvider services;

    @GET
    @RolesAllowed({ComptesAPIEnum.COMPTES_ROLE})
    @Operation(description = "...")
    public Uni<List<CompteBancaire>> getComptes() { ... }
}
```

**Programmation réactive** : toutes les méthodes de service et les appels base de données retournent `Uni<T>` (valeur unique) ou `Multi<T>` (flux) de Mutiny. Ne jamais bloquer avec `.await().indefinitely()` en dehors des tests.

**Injection de dépendances** : CDI uniquement (`@Inject`, `@ApplicationScoped`). Aucune annotation Spring.

**Interfaces de ports** : la logique métier est toujours masquée derrière une interface dans `business/ports/` (ex. `IBudgetAppProvider`, `IComptesRepository`). Les ressources REST injectent l'interface, pas l'implémentation.

**Sécurité** : chaque microservice surcharge `AbstractAPISecurityFilter` et `IJwtSecurityContext` de `communs`. Les endpoints déclarent `@RolesAllowed` avec les constantes de rôle de leur propre `*APIEnum`.

**Appels inter-services** : les services qui ont besoin de données d'autres microservices injectent une interface provider dans `spi/` (ex. `IComptesServiceProvider`, `IParametragesServiceProvider`) appuyée par un client REST Quarkus.

### Base de données
- **MongoDB** via Quarkus MongoDB Panache (pattern repository, pas Active Record).
- Chaîne de connexion : variable d'environnement `QUARKUS_MONGODB_CONNECTION_STRING` (par défaut `localhost:27017` en dev).
- Base dev : `v12-app-dev`. Base prod : variable d'environnement `QUARKUS_MONGODB_DATABASE`.
- La configuration se trouve dans `src/main/resources/dev/application.properties` et `src/main/resources/prod/application.properties` pour chaque module.

### `communs` module
Partagé entre tous les microservices :
- `api/AbstractAPIResource` – endpoint de base `/info`
- `api/AbstractAPIInterceptors` – intercepteurs de logs requête/réponse
- `api/security/AbstractAPISecurityFilter` – validation JWT
- `utils/security/JWTUtils`, `SecurityUtils` – parsing JWT, sanitation des entrées
- `utils/exceptions/` – exceptions typées (`DataNotFoundException`, `UserNotAuthorizedException`, etc.)
- `data/trace/BusinessTraceContext` – contexte de traçage style MDC réinitialisé après chaque réponse
- `aws-deploy/` – templates AWS SAM et configuration API Gateway

### Conventions de test
- Utiliser `@QuarkusTest` sur les classes de test.
- Mocker les dépendances avec `Mockito.mock()` / `Mockito.spy()` dans `@BeforeEach`.
- Résoudre les résultats réactifs dans les tests avec `.await().indefinitely()`.
- `communs` est publié sur GitHub Packages ; les POM des microservices le référencent en dépendance.

## Déploiement
- La CI build d'abord `communs`, le publie sur GitHub Packages, puis build chaque microservice en parallèle en image native.
- Les images natives sont déployées sur AWS Lambda via SAM. Les routes d'API sont définies dans `communs/src/aws-deploy/`.
- SonarCloud s'exécute sur `master` une fois tous les builds terminés.
