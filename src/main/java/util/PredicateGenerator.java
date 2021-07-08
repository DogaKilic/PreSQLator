package util;

import java.util.function.Predicate;

public class PredicateGenerator {

    public static Predicate<String> generateBasicPredicate() {
        Predicate<String> statementPredicate = i -> (i.contains("java.sql.Statement"));
        Predicate<String> preparedStatementPredicate = i -> (i.contains("java.sql.PreparedStatement"));
        Predicate<String> dittoPredicate = i -> (i.contains("\""));
        Predicate<String> totalStatementPredicate = statementPredicate.or(preparedStatementPredicate).and(dittoPredicate);
        return totalStatementPredicate;
    }
}
