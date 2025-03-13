package yoshikihigo.tinypdg.pe.var;

import yoshikihigo.tinypdg.pe.StatementInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Collection;
import java.util.Set;

/**
 * Record the information of defs of the variables in the ProgramElement.
 */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Data
public class VarDef extends Var {

    protected final Type type;

    /**
     * The relevant stmt of this def. Set by StatementInfo.
     */
    protected final StatementInfo relevantStmt;

    /**
     * Def types.
     */
    public enum Type {
        // Levels:
        // - UNKNOWN < NO_DEF < MAY_DEF < DEF < DECLARE < DECLARE_AND_DEF
        UNKNOWN(0), NO_DEF(1), MAY_DEF(2),
        DEF(3), DECLARE(4), DECLARE_AND_DEF(5);

        Type(int level) {
            this.level = level;
        }

        public final int level;

        /**
         * Return whether this is at least MAY_DEF.
         * @return True if it is, otherwise return false
         */
        public boolean isAtLeastMayDef() {
            return this.level >= MAY_DEF.level;
        }

        /**
         * Return whether this is at least a DECLARE.
         * @return True if it is, otherwise return false
         */
        public boolean isAtLeastDeclare() {
            return this.level >= DECLARE.level;
        }

    }

    public VarDef(Scope scope, String variableName, Type type) {
        this(scope, variableName, Set.of(variableName), type, null);
    }

    public VarDef(Scope scope, String mainVariableName, Collection<String> variableNameAliases, Type type) {
        this(scope, mainVariableName, variableNameAliases, type, null);
    }

    public VarDef(Scope scope, String mainVariableName, Collection<String> variableNameAliases,
                  Type type, StatementInfo relevantStmt) {
        super(scope, mainVariableName, variableNameAliases);
        this.type = type;
        this.relevantStmt = relevantStmt;
    }

    public VarDef(Scope scope, VarDef o) {
        this(scope, o.mainVariableName, o.variableNameAliases, o.type, o.relevantStmt);
    }

    public VarDef(VarDef o) {
        this(o.scope, o.mainVariableName, o.variableNameAliases, o.type, o.relevantStmt);
    }

    /**
     * Return a promoted var def with at least the specified type.
     * @param type Type
     * @return Cloned var def with at least the specified type
     */
    public VarDef promote(Type type) {
        if (this.type.level < type.level) {
            return new VarDef(this.scope, this.mainVariableName, this.variableNameAliases, type, this.relevantStmt);
        }
        return new VarDef(this);
    }

}
