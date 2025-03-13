package yoshikihigo.tinypdg.pe.var;

import yoshikihigo.tinypdg.pe.ProgramElementInfo;
import yoshikihigo.tinypdg.pe.StatementInfo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The scope manager.
 */
public class ScopeManager {

    /**
     * The cache map to store all blocks and scopes.
     */
    protected Map<ProgramElementInfo, Scope> blockAndScopes = new ConcurrentHashMap<>();

    /**
     * Clear all scopes to reset this cache.
     */
    public void clear() {
        blockAndScopes.clear();
    }

    /**
     * Get the scope of a block. If it does not exist in our cache, this will add it.
     * In other words, for the same "block", this will return the same "scope".
     * This will also recursively create the full scope stack.
     * @param block The block PE. Usually comes from "StatementInfo.ownerBlock"
     * @return The scope of the block
     */
    public Scope getScope(ProgramElementInfo block) {
        if (blockAndScopes.containsKey(block)) {
            return blockAndScopes.get(block);
        }
        Scope scope = new Scope(block);
        blockAndScopes.put(block, scope);
        if (block instanceof StatementInfo blockStmt) {
            ProgramElementInfo parentBlock = blockStmt.getOwnerBlock();
            if (parentBlock != null && !parentBlock.equals(block)) {
                scope.setParent(getScope(parentBlock));
            }
        }
        return scope;
    }

}
