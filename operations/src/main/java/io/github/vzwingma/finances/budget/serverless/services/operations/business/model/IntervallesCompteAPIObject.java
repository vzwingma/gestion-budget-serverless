package io.github.vzwingma.finances.budget.serverless.services.operations.business.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.vzwingma.finances.budget.services.communs.data.abstrait.AbstractAPIObjectModel;
import io.github.vzwingma.finances.budget.services.communs.utils.data.BudgetDateTimeUtils;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.io.Serial;
import java.time.LocalDate;

@Getter @Setter
public class IntervallesCompteAPIObject extends AbstractAPIObjectModel {

	@Serial
	private static final long serialVersionUID = -2380780514003066552L;


	/**
	 * Date du premier budget du compte
	 */
	@NonNull
	@Schema(description = "Date du premier budget pour le compte")
	private Long datePremierBudget;

	/**
	 * Date du dernier budget du compte
	 */
	@NonNull
	@Schema(description = "Date du dernier budget pour le compte")
	private Long dateDernierBudget;

	/**
	 * @return the datePremierBudget
	 */
	@JsonIgnore
	public LocalDate getLocalDatePremierBudget() {
		return BudgetDateTimeUtils.getLocalDateFromNbDay(datePremierBudget);
	}
	/**
	 * @return the dateDernierBudget
	 */
	@JsonIgnore
	public LocalDate getLocalDateDernierBudget() {
		return BudgetDateTimeUtils.getLocalDateFromNbDay(dateDernierBudget);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("IntervallesCompteAPIObject [datePremierBudget=%s, dateDernierBudget=%s]", datePremierBudget, dateDernierBudget);
	}


}
