---
name: conventions-qa
description: "Conventions de test du projet [NOM_DU_PROJET] pour 🟢 QALvin : framework, commandes, emplacement des tests, mocks, seuil de couverture, cas à couvrir. Prioritaire sur le générique."
user-invocable: false
---

# Conventions projet — [NOM_DU_PROJET] (tests)

> **Template** — placeholders `[...]` à remplir via skill `init-harness`. Tant qu'un placeholder subsiste,
> appliquer le comportement générique de l'agent pour la valeur concernée.
> Projet : `[NOM_DU_PROJET]` — [DESCRIPTION_COURTE_DU_PROJET].

## Stack de test

- **[FRAMEWORK_TEST] [VERSION]** + [BIBLIOTHÈQUES_TEST, ex. utilitaires de rendu, assertions, mocks]
- Fichiers de test : `[MOTIF_FICHIERS_TEST]`, [EMPLACEMENT, ex. co-localisés avec le fichier testé]

## Commandes

```bash
[COMMANDE_TEST]                       # tous les tests
[COMMANDE_TEST_CI]                    # mode CI avec couverture
[COMMANDE_TEST_FICHIER] [CHEMIN]      # un fichier
[COMMANDE_TEST_NOM] "[NOM_DU_TEST]"   # un test par nom
```

Rapport de couverture : `[CHEMIN_RAPPORT_COUVERTURE]` (lu par [OUTIL_QUALITÉ, ex. SonarQube]).
**Seuil de couverture** : [SEUIL, défaut ≥ 80 %] sur le code modifié.

## Mocks

- Dépendances externes (réseau, stockage, horloge) : [MÉCANISME_MOCK]
- État partagé injecté : [COMMENT_MOCKER_L_ÉTAT]

<!-- Exemple illustratif (à remplacer par un test réel du projet) :
```typescript
test('affiche le libellé', () => { render(<MonComposant />); expect(screen.getByText('Libellé')).toBeInTheDocument(); });
```
-->

## Cas à couvrir systématiquement

- **Nominal** : comportement correct avec données valides
- **Vide / null** : données absentes ou partielles
- **Erreurs** : [CODES_ERREUR_PERTINENTS, ex. 401/403, 404, 500] et comportement attendu
- **Interactions** : actions utilisateur ou appels d'API selon le type de composant
- **Régression** : casser volontairement le code doit faire échouer au moins un test

## Ce que QALvin ne fait PAS

- Modifier le code de production (hors fichiers de test).
- Mettre à jour la documentation (rôle 🟣 DOCly).
- Changer la stratégie de test globale sans validation 🟠 ARCos + 👤.
