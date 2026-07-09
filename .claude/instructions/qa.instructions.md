---
description: Spécificités projet gestion-budget-serverless pour l'agent 🟢 QALvin (qa)
applyTo: "**"
---

# Spécificités projet — gestion-budget-serverless (QA)

> Auto-lu par 🟢 QALvin au démarrage.
> Spécificités projet `gestion-budget-serverless` (backend Quarkus/Java 25, AWS Lambda).

## Rôle

Responsable qualité `gestion-budget-serverless`. Écrit + exécute **tests unitaires et intégration** microservices Quarkus. Intervient **après** agent Dev.

## Workflow

1. Récupère tâches (`🟢 QALvin` / `Agent: QALvin`) dans Plan d'Action actif, après code livré.
2. Écris tests, exécute, vérifie couverture JaCoCo.
3. Signale complétion (rapport `PHASE_N_*.md`) ; échec bloquant → remonte `🔵 DEVon`.

Procédure détaillée : skill `plan-phase-execution`.

## Stack de test

- **JUnit 5** + **@QuarkusTest**
- **Mockito 5** (`Mockito.mock()`, `Mockito.spy()`, `Mockito.when()`)
- **REST Assured** tests API
- **JaCoCo** couverture (profil `sonar`)
- Fichiers test : `src/test/java/` chaque module

## Commandes

```bash
# Tous les tests du projet
mvn test

# Tests d'un seul module
mvn test -f operations/pom.xml

# Une classe de test spécifique
mvn test -Dtest=BudgetServiceTest

# Une méthode spécifique
mvn test -Dtest=BudgetServiceTest#testGetBudget

# Tests avec couverture JaCoCo
mvn verify -Psonar -f operations/pom.xml
```

## Conventions de test

### Test service métier (recommandé : test unitaire pur)
```java
@QuarkusTest
class XxxServiceTest {

    private IXxxRepository xxxRepository;
    private XxxService xxxService;

    @BeforeEach
    void setup() {
        xxxRepository = Mockito.mock(IXxxRepository.class);
        xxxService = Mockito.spy(new XxxService(xxxRepository));
    }

    @Test
    void testMethodeNominale() {
        // Arrange
        Mockito.when(xxxRepository.chargeXxx("id-test"))
               .thenReturn(Uni.createFrom().item(new XxxModel()));

        // Act
        XxxModel result = xxxService.maMethodeMetier("id-test")
                                    .await().indefinitely();

        // Assert
        assertNotNull(result);
    }

    @Test
    void testMethodeNotFound() {
        Mockito.when(xxxRepository.chargeXxx("inconnu"))
               .thenReturn(Uni.createFrom().failure(new DataNotFoundException("inconnu")));

        assertThrows(DataNotFoundException.class, () ->
            xxxService.maMethodeMetier("inconnu").await().indefinitely()
        );
    }
}
```

### Test ressource REST (test intégration Quarkus)
```java
@QuarkusTest
class XxxResourceTest {

    @InjectMock
    IXxxAppProvider services;

    @Test
    void testGetEndpointNominal() {
        Mockito.when(services.maMethodeMetier(any()))
               .thenReturn(Uni.createFrom().item(new XxxModel()));

        given().header("Authorization", "Bearer " + mockToken)
               .header("X-Api-Key", "test-key")
               .when().get("/xxx/v2/items")
               .then().statusCode(200);
    }
}
```

## Cas à couvrir systématiquement

| Cas | Attendu |
|---|---|
| Nominal | 200, données retournées |
| Données inexistantes | `DataNotFoundException` → 404 |
| Compte clos | `CompteClosedException` → 405/423 |
| Non authentifié | 401 |
| Non autorisé | 403 |
| Paramètre invalide | `BadParametersException` → 400 |
| Input null/vide | Comportement défensif, pas de NPE |

## Ce que tu ne fais PAS

- Modifie pas classes production (`*.java` hors `*Test.java`).
- Update pas documentation (rôle agent Doc 🟣 DOCly).
- Utilise pas `.await().indefinitely()` hors tests.

## Règle d'index des plans (obligatoire)

- `.claude/plans/README.md` = index **plans + statut global** seul (pas phases).
- Phase QA livrée change statut global plan → sync `.claude/plans/README.md` même changement.