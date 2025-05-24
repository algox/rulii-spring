package org.rulii.spring.test.rules.setb;

import org.rulii.annotation.*;
import org.rulii.validation.RuleViolation;
import org.rulii.validation.RuleViolations;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDate;

@Rule
@Description("This Rule will validate that the from date is before the to date.")
public class ConsistentDateRule {

    // We are leveraging Spring to externalize the message
    @Value("${errorCode.100}")
    private String message;

    public ConsistentDateRule() {
        super();
    }

    @PreCondition // Don't run the rule if there are nulls
    public boolean check(LocalDate fromDate, LocalDate toDate) {
        return toDate != null && fromDate != null;
    }

    @Given // Condition
    public boolean isValid(LocalDate fromDate, LocalDate toDate) {
        return fromDate.isBefore(toDate);
    }

    @Otherwise() // Else Action
    public void otherwise(LocalDate fromDate, LocalDate toDate, RuleViolations violations) {
        violations.add(RuleViolation.builder().build("consistentDateRule", "errorCode.100", message));
    }
}
