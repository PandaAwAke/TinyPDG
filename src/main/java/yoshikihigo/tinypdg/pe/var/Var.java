package yoshikihigo.tinypdg.pe.var;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.*;

@ToString(exclude = "scope")
@NoArgsConstructor
@Getter
public class Var {

    /**
     * The scope of this var.
     * (Currently we only consider vars in methods, so if this var is a field, the scope will be null)
     * This can change, so don't include it in hashCode()
     */
    protected Scope scope = null;

    /**
     * The main variable name.
     */
    protected String mainVariableName = null;

    /**
     * Aliases of the same variable. Such as "this.source" and "source".
     * Note that the mainVariableName should also in it.
     */
    protected Set<String> variableNameAliases = new TreeSet<>();

    public Var(Scope scope, String variableName) {
        this(scope, variableName, Set.of(variableName));
    }

    public Var(Scope scope, String mainVariableName, Collection<String> variableNameAliases) {
        this.scope = scope;
        this.mainVariableName = mainVariableName;
        this.variableNameAliases.addAll(variableNameAliases);
    }

    public Set<String> getVariableNameAliases() {
        return Collections.unmodifiableSet(variableNameAliases);
    }

    public void updateScope() {
        if (scope != null) {
            scope.addVariable(this);
        }
    }

    /**
     * Judge whether a variable name matches this Var.
     * @return True if the name matches this var.
     */
    public boolean matchName(String name) {
        return variableNameAliases.contains(name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Var var = (Var) o;
        return Objects.equals(scope, var.scope) && Objects.equals(mainVariableName, var.mainVariableName) && Objects.equals(variableNameAliases, var.variableNameAliases);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mainVariableName, variableNameAliases);
    }

}
