package io.github.vzwingma.finances.budget.services.operations.business.model.operation;

import io.github.vzwingma.finances.budget.services.communs.data.abstrait.AbstractAPIObjectModel;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.io.Serial;
import java.util.Set;

/**
 * Object représentant les libellés des opérations pour l'ensemble des budgets de l'année pour un compte
 * @author vzwingma
 *
 */
@Getter @Setter
public class LibellesOperationsAPIObject extends AbstractAPIObjectModel {


	@Serial
	private static final long serialVersionUID = -1515823001772650589L;
	@Schema(description = "Id du compte")
	private String idCompte;
	@Schema(description = "Libelles des opérations courantes")
	private Set<String> libellesOperations;
}
