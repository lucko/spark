package me.lucko.spark.api.profiler;

import java.util.Objects;
import java.util.Set;

/**
 * TODO: implement this class, see {@link ProfilerConfiguration} for the purpose.
 */
public final class Dumper {
    private final Method method;
    private final Set<String> criteria;

    /**
     *
     */
    public Dumper(Method method, Set<String> criteria) {
        this.method = method;
        this.criteria = criteria;
    }

    public static Dumper pattern(Set<String> criteria) {
        return new Dumper(Method.PATTERN, criteria);
    }

    public static Dumper specific(Set<String> criteria) {
        return new Dumper(Method.SPECIFIC, criteria);
    }

    public Method method() {
        return method;
    }

    public Set<String> criteria() {
        return criteria;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        Dumper that = (Dumper) obj;
        return Objects.equals(this.method, that.method) &&
                Objects.equals(this.criteria, that.criteria);
    }

    @Override
    public int hashCode() {
        return Objects.hash(method, criteria);
    }

    @Override
    public String toString() {
        return "Dumper[" +
                "method=" + method + ", " +
                "criteria=" + criteria + ']';
    }

    public enum Method {
        PATTERN,
        SPECIFIC
    }
}
