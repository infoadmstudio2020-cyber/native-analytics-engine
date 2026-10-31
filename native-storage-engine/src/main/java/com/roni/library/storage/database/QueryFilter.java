package com.roni.library.storage.database;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class QueryFilter {
    public enum Operator {
        EQUALS,
        NOT_EQUALS,
        GREATER_THAN,
        GREATER_THAN_OR_EQUAL,
        LESS_THAN,
        LESS_THAN_OR_EQUAL,
        CONTAINS
    }

    private static class Condition {
        final String field;
        final Operator op;
        final Object expected;

        Condition(String field, Operator op, Object expected) {
            this.field = field;
            this.op = op;
            this.expected = expected;
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        boolean matches(Document doc) {
            if (doc == null) return false;
            Object actual = "_id".equals(field) ? doc.getId() : doc.get(field);

            switch (op) {
                case EQUALS:
                    return Objects.equals(actual, expected);
                case NOT_EQUALS:
                    return !Objects.equals(actual, expected);
                case CONTAINS:
                    if (actual == null || expected == null) return false;
                    return actual.toString().contains(expected.toString());
                case GREATER_THAN:
                case GREATER_THAN_OR_EQUAL:
                case LESS_THAN:
                case LESS_THAN_OR_EQUAL:
                    if (actual == null || expected == null) return false;
                    if (actual instanceof Number && expected instanceof Number) {
                        double a = ((Number) actual).doubleValue();
                        double b = ((Number) expected).doubleValue();
                        if (op == Operator.GREATER_THAN) return a > b;
                        if (op == Operator.GREATER_THAN_OR_EQUAL) return a >= b;
                        if (op == Operator.LESS_THAN) return a < b;
                        return a <= b;
                    }
                    if (actual instanceof Comparable && actual.getClass().isInstance(expected)) {
                        int comp = ((Comparable) actual).compareTo(expected);
                        if (op == Operator.GREATER_THAN) return comp > 0;
                        if (op == Operator.GREATER_THAN_OR_EQUAL) return comp >= 0;
                        if (op == Operator.LESS_THAN) return comp < 0;
                        return comp <= 0;
                    }
                    return false;
                default:
                    return true;
            }
        }
    }

    private final List<Condition> conditions = new ArrayList<>();
    private final boolean matchAll;

    public QueryFilter() {
        this.matchAll = false;
    }

    private QueryFilter(boolean matchAll) {
        this.matchAll = matchAll;
    }

    public static QueryFilter all() {
        return new QueryFilter(true);
    }

    public static QueryFilter eq(String field, Object value) {
        QueryFilter qf = new QueryFilter();
        qf.where(field, Operator.EQUALS, value);
        return qf;
    }

    public QueryFilter where(String field, Operator op, Object value) {
        conditions.add(new Condition(field, op, value));
        return this;
    }

    public QueryFilter eqField(String field, Object value) {
        return where(field, Operator.EQUALS, value);
    }

    public QueryFilter gt(String field, Object value) {
        return where(field, Operator.GREATER_THAN, value);
    }

    public QueryFilter gte(String field, Object value) {
        return where(field, Operator.GREATER_THAN_OR_EQUAL, value);
    }

    public QueryFilter lt(String field, Object value) {
        return where(field, Operator.LESS_THAN, value);
    }

    public QueryFilter lte(String field, Object value) {
        return where(field, Operator.LESS_THAN_OR_EQUAL, value);
    }

    public QueryFilter contains(String field, String substring) {
        return where(field, Operator.CONTAINS, substring);
    }

    public boolean test(Document document) {
        if (matchAll) return true;
        if (conditions.isEmpty()) return true;
        for (Condition cond : conditions) {
            if (!cond.matches(document)) {
                return false;
            }
        }
        return true;
    }
}
