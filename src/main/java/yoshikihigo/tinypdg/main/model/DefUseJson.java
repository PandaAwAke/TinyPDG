package yoshikihigo.tinypdg.main.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import yoshikihigo.tinypdg.pdg.PDG;
import yoshikihigo.tinypdg.pe.ProgramElementInfo;
import yoshikihigo.tinypdg.pe.var.Scope;
import yoshikihigo.tinypdg.pe.var.VarDef;
import yoshikihigo.tinypdg.pe.var.VarUse;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DefUseJson {

    List<VariableJson> variableJsons = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @EqualsAndHashCode(of = {"id", "scope", "name"})
    public static class VariableJson {
        int id;
        ScopeJson scopeJson;
        String name;
        Set<Integer> defStmtLineNumbers = new TreeSet<>();
        Set<Integer> useStmtLineNumbers = new TreeSet<>();

        @JsonIgnore
        Scope scope;

        public VariableJson(int id, Scope scope, ScopeJson scopeJson, String name) {
            this.id = id;
            this.scope = scope;
            this.scopeJson = scopeJson;
            this.name = name;
        }

        public void addDefStmtLineNumber(int lineNumber) {
            defStmtLineNumbers.add(lineNumber);
        }

        public void addUseStmtLineNumber(int lineNumber) {
            useStmtLineNumbers.add(lineNumber);
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ScopeJson {
        String type;
        int lineNumber;
    }

    /**
     * Try to find an existing target VariableJson in a collection (by scope and name).
     * @param collection Collection to search
     * @param scope scope
     * @param name Variable name
     * @return The matched VariableJson (optional)
     */
    public static Optional<VariableJson> findVariableJson(Collection<VariableJson> collection, Scope scope, String name) {
        return collection.stream().filter(vj -> Objects.equals(vj.scope, scope) && Objects.equals(vj.name, name)).findFirst();
    }

    public static DefUseJson createFromPDG(PDG pdg) {
        Map<VariableJson, Integer> nodeIdMap = new LinkedHashMap<>();
        AtomicInteger nodeIdCounter = new AtomicInteger(0);

        pdg.getAllNodesExceptEntryAndParams().forEach(ddgNode -> {
            int stmtStartLine = ddgNode.getCore().getStartLine();

            Stream.concat(
                    ddgNode.getCore().getDefVariablesAtLeastMayDef().stream(),
                    ddgNode.getCore().getUseVariablesAtLeastMayUse().stream()
            ).forEach(var -> {
                Scope scope = var.getScope();
                ScopeJson scopeJson;
                if (scope != null) {
                    ProgramElementInfo scopeBlock = scope.getBlock();
                    scopeJson = new ScopeJson(scopeBlock.getClass().getSimpleName(), scopeBlock.getStartLine());
                } else {
                    scopeJson = null;
                }
                String varName = var.getMainVariableName();

                Optional<VariableJson> matchedExistingVarJsonOpt = findVariableJson(nodeIdMap.keySet(), scope, varName);
                VariableJson matchedExistingVarJson;
                if (matchedExistingVarJsonOpt.isEmpty()) {
                    int id = nodeIdCounter.getAndIncrement();
                    matchedExistingVarJson = new VariableJson(id, scope, scopeJson, varName);
                    nodeIdMap.put(matchedExistingVarJson, id);
                } else {
                    matchedExistingVarJson = matchedExistingVarJsonOpt.get();
                }

                if (var instanceof VarDef) {
                    matchedExistingVarJson.addDefStmtLineNumber(stmtStartLine);
                } else if (var instanceof VarUse) {
                    matchedExistingVarJson.addUseStmtLineNumber(stmtStartLine);
                }
            });
        });

        return new DefUseJson(new ArrayList<>(nodeIdMap.keySet()));
    }


}
