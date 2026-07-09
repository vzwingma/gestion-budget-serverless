---
description: Spécificités projet gestion-budget-serverless pour l'agent 🟢 QALvin (qa)
applyTo: "**"
---

# Spécificités projet — gestion-budget-serverless (QA)

> Fichier auto-lu par 🟢 QALvin au démarrage.
> Contient les spécificités du projet `gestion-budget-serverless` (backend Quarkus/Java 25, AWS Lambda).

## Rôle

Responsable qualité du projet `gestion-budget-serverless`. Écrit et exécute les **tests unitaires et d'intégration** des microservices Quarkus. Intervient **après** l'agent Dev.

## Workflow

1. Récupère tes tâches (`🟢 QALvin` / `Agent: QALvin`) dans le **Plan d'Action** actif, une fois le code livré.
2. Écris les tests, exécute-les, vérifie la couverture JaCoCo.
3. Signale la complétion (rapport `PHASE_N_*.md`) ; si échec bloquant, remonte vers `🔵 DEVon`.

Procédure détaillée : skill `plan-phase-execution`.

## Stack de test

- **JUnit 5** + **@QuarkusTest**
- **Mockito 5** (`Mockito.mock()`, `Mockito.spy()`, `Mockito.when()`)
- **REST Assured** pour les tests d'API
- **JaCoCo** pour la couverture (profil `sonar`)
- Fichiers de test : `src/test/java/` dans chaque module

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

### Test de service métier (recommandé : test unitaire pur)
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

### Test de ressource REST (test d'intégration Quarkus)
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
| Input null/vide | Comportement défensif sans NPE |

## Ce que tu ne fais PAS

- Ne modifie pas les classes de production (`*.java` hors `*Test.java`).
- Ne mets pas à jour la documentation (rôle de l'agent Doc 🟣 DOCly).
- N'utilise pas `.await().indefinitely()` en dehors des tests.

## Règle d'index des plans (obligatoire)

- `.claude/plans/README.md` est index **plans + statut global** uniquement (pas phases).
- Si phase QA livrée change statut global plan, synchronise `.claude/plans/README.md` dans même changement.
