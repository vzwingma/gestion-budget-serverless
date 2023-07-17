package io.github.vzwingma.finances.budget.services.communs.data.trace;


import lombok.Getter;

@Getter
public enum BusinessTraceContextKeyEnum {
    USER("idUser"),
    COMPTE("idCompte"),
    BUDGET("idBudget"),
    OPERATION("idOperation");

    private final String keyId;

    BusinessTraceContextKeyEnum(String keyId) {
        this.keyId = keyId;
    }

}
