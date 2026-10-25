---
name: conventions-qa
description: "Conventions de test du projet gestion-budget-serverless pour 🟢 QALvin : framework, commandes, emplacement des tests, mocks, seuil de couverture, cas à couvrir. Prioritaire sur le générique."
user-invocable: false
---

# Conventions projet — gestion-budget-serverless (tests)

> Projet : `gestion-budget-serverless` — backend Quarkus/Java 25 multi-modules Maven, architecture hexagonale,
> Mutiny + MongoDB Panache, déployé en images natives sur AWS Lambda (SAM).

## Stack de test

- **JUnit 5** (`quarkus-junit`) + `@QuarkusTest`, **Mockito 5** (`mockito-core`), **REST Assured**,
  **JaCoCo** (`quarkus-jacoco` + `jacoco-maven-plugin`, profil `sonar`)
- Fichiers de test : `<module>/src/test/java/...` (miroir du package testé), nommés `Test<Classe>.java` ou
  `<Classe>Test.java` (les deux coexistent : suivre celui du module/package touché) ; ressources dans
  `<module>/src/test/resources/`

## Commandes

```bash
mvn clean test                                   # tous les tests
mvn verify -Psonar -f <module>/pom.xml           # module avec couverture JaCoCo
mvn clean test -f <module>/pom.xml               # un module
mvn clean test -Dtest=BudgetServiceTest          # une classe
mvn clean test -Dtest=BudgetServiceTest#testGetBudget   # une méthode
```

`communs` doit être installé (`mvn clean install -f communs/pom.xml`) avant de tester un µService après modif de `communs`.

Rapport de couverture : `<module>/target/jacoco-report/jacoco.xml` (+ `target/jacoco-aggregate/`), lu par SonarCloud.
**Seuil de couverture** : ≥ 80 % sur le code modifié.

## Mocks

- Dépendances (repositories, providers inter-µServices) : `Mockito.mock()` / `Mockito.spy()` dans `@BeforeEach` ;
  ressources REST : `@InjectMock` sur l'interface de port.
- Réactif : stubs `Uni.createFrom().item(...)` / `Uni.createFrom().failure(...)`, résolution `.await().indefinitely()`.
- Horloge : `Clock.fixed(Instant.parse("..."), ZoneOffset.UTC)` injecté (ADR-004), jamais de date courante.

```java
@BeforeEach
void setup() {
    xxxRepository = Mockito.mock(IXxxRepository.class);
    xxxService = Mockito.spy(new XxxService(xxxRepository));
}

@Test
void testMethodeNotFound() {
    Mockito.when(xxxRepository.chargeXxx("inconnu"))
           .thenReturn(Uni.createFrom().failure(new DataNotFoundException("inconnu")));
    assertThrows(DataNotFoundException.class, () -> xxxService.maMethodeMetier("inconnu").await().indefinitely());
}
```

## Cas à couvrir systématiquement

- **Nominal** : 200, données retournées
- **Vide / null** : input null/vide → comportement défensif, pas de NPE
- **Erreurs** : `DataNotFoundException` → 404, `BadParametersException` → 400, `CompteClosedException` → 405/423,
  non authentifié → 401, non autorisé → 403
- **Interactions** : appels aux ports (`Mockito.verify`) et endpoints REST (REST Assured)
- **Régression** : casser volontairement le code doit faire échouer au moins un test

## Ce que QALvin ne fait PAS

- Modifier le code de production (`src/main/**`).
- Mettre à jour la documentation (rôle 🟣 DOCly).
- Changer la stratégie de test globale sans validation 🟠 ARCos + 👤.
