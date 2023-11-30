# Procédure d'administration

## Ajouter un nouveau compte

Pour ajouter un nouveau compte, injecter un document dans la collection `v12-app.comptesbancaires`

Le document doit avoir la forme suivante :

        {
            "_id": "<<id_du_compte>>",
            "libelle": "Nom du compte",
            "proprietaire": {
                "_id": {
                    "$oid": "5484268384b7ff1e5f26b692"
                },
                "login": "vzwingma",
                "libelle": "Vincent"
            },
            "itemIcon": "img/<<nom de la banque>>.png",
            "ordre": {
                "$numberInt": "<<ordre d'affichage>>"
            },
            "actif": true
        }

## Initier un nouveau budget

Pour initier un nouveau budget pour un nouveau compte, injecter un document dans la collection `v12-app.budgets
` dans la bdd.

Le document doit avoir la forme suivante :

        {
            "_id": "<<id_du_budget>>",
            "actif": true,
            "idCompteBancaire": "<<id_du_compte>>",
            "mois": "NOVEMBER",
            "annee": {
                "$numberInt": "2022"
            },
            "listeOperations": [],
            "dateMiseAJour": {
                "$date": {
                    "$numberLong": "1665304684286"
                }
            },	
            "soldes": {
                "soldeAtFinMoisCourant": {
                    "$numberDouble": "0"
                },
                "soldeAtFinMoisPrecedent": {
                    "$numberDouble": "0"
                },
                "soldeAtMaintenant": {
                    "$numberDouble": "0"
                }
            },
            "totauxParCategories": { },
            "totauxParSSCategories": { }
        }

où

- `id_du_compte` : est l'identifiant du compte, créé précédemment
- `id_du_budget` : est la concaténation de : "`id_du_compte`\_`annee`\_`n° du mois`"
