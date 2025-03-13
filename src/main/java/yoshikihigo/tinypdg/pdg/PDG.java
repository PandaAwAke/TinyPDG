/*
 * Copyright 2024 Ma Yingshuo
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package yoshikihigo.tinypdg.pdg;

import yoshikihigo.tinypdg.cfg.CFG;
import yoshikihigo.tinypdg.cfg.node.CFGNode;
import yoshikihigo.tinypdg.cfg.node.CFGNodeFactory;
import yoshikihigo.tinypdg.pdg.edge.PDGControlDependenceEdge;
import yoshikihigo.tinypdg.pdg.edge.PDGDataDependenceEdge;
import yoshikihigo.tinypdg.pdg.edge.PDGEdge;
import yoshikihigo.tinypdg.pdg.edge.PDGExecutionDependenceEdge;
import yoshikihigo.tinypdg.pdg.node.*;
import yoshikihigo.tinypdg.pe.*;
import yoshikihigo.tinypdg.pe.var.VarDef;
import yoshikihigo.tinypdg.pe.var.VarUse;
import lombok.Getter;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The Program Dependency Graph of a method.
 */
public class PDG implements Comparable<PDG> {

    /**
     * The factory to generate PDG nodes.
     */
    final private PDGNodeFactory pdgNodeFactory;

    /**
     * The factory to generate CFG nodes.
     */
    final private CFGNodeFactory cfgNodeFactory;

    /**
	 * All nodes in the CFG.
	 */
    @Getter
	final protected SortedSet<PDGNode<?>> allNodes = new TreeSet<>();

    /**
	 * The enter node of the PDG.
     * Note that unlike CFG, the enterNode here is usually a fake entry node.
	 */
    final public PDGMethodEnterNode enterNode;

    /**
     * The exit nodes of the PDG. This is the same as the CFG.
     */
    final private SortedSet<PDGNode<?>> exitNodes;

    /**
     * The parameter nodes of the PDG. Usually represents parameters of the method.
     */
    final private List<PDGParameterNode> parameterNodes;

    /**
     * The MethodInfo related to this PDG (like CFG.core).
     */
    final public MethodInfo unit;

    // ------------------- Control dependency edges -------------------
    /**
     * Whether PDG should build control dependency edges, such as if-then-else condition edges.
     */
    final public boolean buildControlDependence;
    /**
     * If this is true, then for every node, there'll be a "true" control edge from the enter node to it.
     */
    final public boolean buildControlDependenceFromEnterToAllNodes = false;
    /**
     * If this is true, then there'll be control edges from the enter node to parameter nodes.
     */
    final public boolean buildControlDependenceFromEnterToParameterNodes = false;


    // ------------------- Data dependency edges -------------------
    /**
     * Whether PDG should build data dependency edges.
     */
    final public boolean buildDataDependence;

    /**
     * If this is true, then in the defs of the ProgramElementInfo,
     * MAY_DEF will be treated as DEF, and new-defs will completely replace old-defs,
     * thereby stop the propagation of old-defs.
     * Otherwise, only the surely DEF will stop the propagation of old-defs.
     */
    final public boolean treatMayDefAsDef = false;

    /**
     * If this is true, then in the uses of the ProgramElementInfo,
     * MAY_USE will be treated as USE.
     */
    final public boolean treatMayUseAsUse = true;

    // ------------------- Execution dependency edges -------------------
    /**
     * Whether PDG should build execution dependency edges, such as CFG normal edges.
     */
    final public boolean buildExecutionDependence;


    /**
     * The CFG object used for building PDG.
     */
    @Getter
    private CFG cfg;

    public PDG(final MethodInfo unit,
               final PDGNodeFactory pdgNodeFactory,
               final CFGNodeFactory cfgNodeFactory,
               final boolean buildControlDependence,
               final boolean buildDataDependence,
               final boolean buildExecutionDependence) {

        assert null != unit : "\"unit\" is null";
        assert null != pdgNodeFactory : "\"pdgNodeFactory\" is null";
        assert null != cfgNodeFactory : "\"cfgNodeFactory\" is null";

        this.unit = unit; //一个方法总的信息
        this.pdgNodeFactory = pdgNodeFactory;
        this.cfgNodeFactory = cfgNodeFactory;

        this.enterNode = (PDGMethodEnterNode) this.pdgNodeFactory.makeControlNode(unit);
        this.allNodes.add(enterNode);
        this.exitNodes = new TreeSet<>();
        this.parameterNodes = new ArrayList<>();

        for (final VariableDeclarationInfo variable : unit.getParameters()) {
            final PDGParameterNode parameterNode = (PDGParameterNode) this.pdgNodeFactory.makeNormalNode(variable);
            this.allNodes.add(parameterNode);
            this.parameterNodes.add(parameterNode);
        }

        this.buildControlDependence = buildControlDependence;
        this.buildDataDependence = buildDataDependence;
        this.buildExecutionDependence = buildExecutionDependence;
    }

    public PDG(final MethodInfo unit,
               final PDGNodeFactory pdgNodeFactory,
               final CFGNodeFactory cfgNodeFactory) {
        this(unit, pdgNodeFactory, cfgNodeFactory, true, true, true);
    }

    public PDG(final MethodInfo unit) {
        this(unit, new PDGNodeFactory(), new CFGNodeFactory());
    }

    public PDG(final MethodInfo unit, final boolean buildControlDependency,
               final boolean buildDataDependency,
               final boolean buildExecutionDependency) {
        this(unit, new PDGNodeFactory(), new CFGNodeFactory(),
                buildControlDependency, buildDataDependency,
                buildExecutionDependency);
    }

    @Override
    public int compareTo(final PDG o) {
        assert null != o : "\"o\" is null.";
        return this.unit.compareTo(o.unit);
    }

    public final SortedSet<PDGNode<?>> getExitNodes() {
        final SortedSet<PDGNode<?>> nodes = new TreeSet<>();
        nodes.addAll(this.exitNodes);
        return nodes;
    }

    public final List<PDGParameterNode> getParameterNodes() {
        return new ArrayList<>(this.parameterNodes);
    }

    /**
     * Get all PDGNodes except entry and param nodes.
     * @return All PDGNodes except entry and param nodes.
     */
    public final SortedSet<PDGNode<?>> getAllNodesExceptEntryAndParams() {
        return this.allNodes.stream()
                .filter(node -> !(node instanceof PDGMethodEnterNode) && !(node instanceof PDGParameterNode))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    public final SortedSet<PDGEdge> getAllEdges() {
        final SortedSet<PDGEdge> edges = new TreeSet<>();

        final SortedSet<PDGNode<?>> nodes = this.getAllNodes();
        for (final PDGNode<?> node : nodes) {
            edges.addAll(node.getForwardEdges());
            edges.addAll(node.getBackwardEdges());
        }

        return edges;
    }

    /**
     * Build the PDG.
     */
    public void build() {
        this.cfg = new CFG(this.unit, this.cfgNodeFactory);
        this.cfg.build();
//        this.cfg.removeSwitchCases(); //switch结点
//        this.cfg.removeJumpStatements(); //Jump结点的打扰

        if (this.buildControlDependence) { //控制依赖
            if (this.buildControlDependenceFromEnterToAllNodes) {
                // Add control edges from "entry" to all nodes
                this.buildControlDependence(this.enterNode, unit); // enter与各大结点之间的关系
            }

            if (this.buildControlDependenceFromEnterToParameterNodes) {
                // Add control edges from "entry" to all parameter nodes
    			for (final PDGParameterNode parameterNode : this.parameterNodes) {
    				final PDGControlDependenceEdge edge = new PDGControlDependenceEdge(
    						this.enterNode, parameterNode, true);
    				this.enterNode.addForwardEdge(edge);
    				parameterNode.addBackwardEdge(edge);
    			}
            }
        }

        // ---------------- Build dependency edges for "entry" ----------------
        if (this.buildExecutionDependence) {
            // Execution dependency edge: from "PDG entry" (a fake node) to "CFG entry" (the first statement of the method)
            if (!this.cfg.isEmpty()) {
                final PDGNode<?> node = this.pdgNodeFactory.makeNode(this.cfg.getEnterNode());
                this.allNodes.add(node);
                final PDGExecutionDependenceEdge edge = new PDGExecutionDependenceEdge(this.enterNode, node);
                this.enterNode.addForwardEdge(edge);
                node.addBackwardEdge(edge);
            }
        }

        if (this.buildDataDependence) {
            // Initialization: calculate all defs and uses (for later use)
            this.unit.getDefVariables();
            this.unit.getUseVariables();

            // Data dependency edges: parameters to "CFG entry" (the first statement of the method)
            for (final PDGParameterNode parameterNode : this.parameterNodes) {
                if (!this.cfg.isEmpty()) {
                    this.buildDataDependence(this.cfg.getEnterNode(), parameterNode, parameterNode.core.name, new HashSet<>());
                }
            }

            // Data dependency edges: from "PDG entry" (a fake node) to parameters
            for (final PDGParameterNode parameterNode : this.parameterNodes) {
                final PDGDataDependenceEdge edge = new PDGDataDependenceEdge(this.enterNode, parameterNode, parameterNode.core.name);
                this.enterNode.addForwardEdge(edge);
                parameterNode.addBackwardEdge(edge);
            }
        }

        // ---------------- Build dependency edges by CFG ----------------
        // This set is used to avoid revisiting the same node
        final Set<CFGNode<?>> checkedNodes = new HashSet<>();

        // Build the dependency edges: reachable from the "CFG entry" (the first statement of the method)
        if (!this.cfg.isEmpty()) {
            this.buildDependence(this.cfg.getEnterNode(), checkedNodes);
        }

        // Set PDG exitNodes from CFG exitNodes
        for (final CFGNode<?> cfgExitNode : this.cfg.getExitNodes()) {
            final PDGNode<?> pdgExitNode = this.pdgNodeFactory.makeNode(cfgExitNode);
            this.allNodes.add(pdgExitNode);
            this.exitNodes.add(pdgExitNode);
        }

        // Build the dependency edges: unreachable in the CFG
        if (!this.cfg.isEmpty()) {
            final Set<CFGNode<?>> unreachableNodes = new HashSet<>(this.cfg.getAllNodes());
            unreachableNodes.removeAll(this.cfg.getReachableNodes());
            for (final CFGNode<?> unreachableNode : unreachableNodes) {
                this.buildDependence(unreachableNode, checkedNodes);
            }
        }
    }

    /**
     * Build all dependency edges from a CFG node.
     * @param cfgNode CFGNode
     * @param checkedNodes The visited nodes set used for avoiding revisiting
     */
    private void buildDependence(final CFGNode<?> cfgNode,
                                 final Set<CFGNode<?>> checkedNodes) {
        assert null != cfgNode : "\"cfgNode\" is null.";
        assert null != checkedNodes : "\"checkedNodes\" is null.";

        if (checkedNodes.contains(cfgNode)) {
            return;
        } else {
            checkedNodes.add(cfgNode);
        }

        final PDGNode<?> pdgNode = this.pdgNodeFactory.makeNode(cfgNode); // PDGNode
        this.allNodes.add(pdgNode);
        if (this.buildDataDependence) {
            Set<VarDef> defs = pdgNode.core.getDefVariablesAtLeastMayDef();
            for (final VarDef def : defs) {
                if (!def.getType().isAtLeastMayDef()) {
                    continue;
                }

                // Actually the cfgNode itself should also be considered
                this.buildDataDependence(cfgNode, pdgNode, def.getMainVariableName(), new HashSet<>());
                for (final CFGNode<?> toNode : cfgNode.getForwardNodes()) {
                    this.buildDataDependence(toNode, pdgNode, def.getMainVariableName(), new HashSet<>());
                }
            }
        }
        if (this.buildControlDependence) {
            if (pdgNode instanceof PDGControlNode) {
                final ProgramElementInfo condition = pdgNode.core;
                this.buildControlDependence((PDGControlNode) pdgNode, condition.getOwnerConditionalBlock());
            }
        }

        if (this.buildExecutionDependence) {
            for (final CFGNode<?> toCFGNode : cfgNode.getForwardNodes()) {
                this.buildExecutionDependence(toCFGNode, pdgNode);
            }
        }

        // Recursive: for every forwarding nodes
        for (final CFGNode<?> forwardNode : cfgNode.getForwardNodes()) {
            this.buildDependence(forwardNode, checkedNodes);
        }
    }

    /**
     * Try to build a data dependency edge (associated with a variable) from "fromPDGNode" to PDG node of "cfgNode".
     * If "cfgNode" uses the variable defined by "fromPDGNode", then the edge will be added.
     * @param cfgNode The CFG node which might use the variable
     * @param fromPDGNode The PDG node which defined the variable
     * @param variable Defined variable (originally defined in "fromPDGNode")
     * @param checkedCFGNodes The visited nodes set used for avoiding revisiting
     */
    private void buildDataDependence(final CFGNode<?> cfgNode,
                                     final PDGNode<?> fromPDGNode,
                                     final String variable,
                                     final Set<CFGNode<?>> checkedCFGNodes) {
        assert null != cfgNode : "\"cfgNode\" is null.";
        assert null != fromPDGNode : "\"fromPDGNode\" is null.";
        assert null != variable : "\"variable\" is null.";
        assert null != checkedCFGNodes : "\"checkedCFGnodes\" is null.";

        if (checkedCFGNodes.contains(cfgNode)) {
            return;
        } else {
            checkedCFGNodes.add(cfgNode);
        }

        // The variable was defined in "fromPDGNode"
        // If "cfgNode" uses the variable, add the edge
        Optional<VarUse> matchedUse = cfgNode.core.getUseVariablesAtLeastMayUse().stream()
                .filter(use -> use.matchName(variable))
                .findFirst();

        boolean shouldAddEdge = false;
        if (matchedUse.isPresent()) {
            VarUse.Type useType = matchedUse.get().getType();
            assert useType.level > VarUse.Type.UNKNOWN.level : "Illegal state";

            if (this.treatMayUseAsUse) {
                // MAY_USE is treated as USE
                if (useType.level >= VarUse.Type.MAY_USE.level) {
                    shouldAddEdge = true;
                }
            } else {
                if (useType.level >= VarUse.Type.USE.level) {
                    shouldAddEdge = true;
                }
            }
        }

        // Add edge: fromPDGNode -> cfgNode (because cfgNode used this variable)
        if (shouldAddEdge) {
            final PDGNode<?> toPDGNode = this.pdgNodeFactory.makeNode(cfgNode);
            this.allNodes.add(toPDGNode);
            final PDGDataDependenceEdge edge = new PDGDataDependenceEdge(fromPDGNode, toPDGNode, variable);
            fromPDGNode.addForwardEdge(edge);
            toPDGNode.addBackwardEdge(edge);
        }


        // Find whether this variable was defined in this PE
        // If so, try to stop or continue the propagation...
        Optional<VarDef> matchedDef = cfgNode.core.getDefVariablesAtLeastMayDef().stream()
                .filter(def -> def.matchName(variable))
                .findFirst();

        boolean shouldPropagate = true;
        if (matchedDef.isPresent()) {
            VarDef.Type varDefType = matchedDef.get().getType();

            // Only consider MAY_DEF or above
            if (varDefType.isAtLeastMayDef()) {
                if (this.treatMayDefAsDef) {
                    // [MAY_DEF, DEF] will stop the propagation
                    shouldPropagate = false;
                } else if (varDefType.equals(VarDef.Type.DEF)) {
                    // [DEF] will stop the propagation
                    shouldPropagate = false;
                }
            }
        }

        // Propagate
        if (shouldPropagate) {
            for (final CFGNode<?> forwardNode : cfgNode.getForwardNodes()) {
                this.buildDataDependence(forwardNode, fromPDGNode, variable, checkedCFGNodes);
            }
        }
    }

    /**
     * Try to build a control dependency edge from "fromPDGNode" to statements in the "block".
     * @param fromPDGNode The source PDG node
     * @param block The target block
     */
    private void buildControlDependence(final PDGControlNode fromPDGNode,
                                        final BlockInfo block) {
        for (final StatementInfo statement : block.getStatements()) {
            this.buildControlDependence(fromPDGNode, statement, true);
        }

        if (block instanceof StatementInfo) {
            for (final StatementInfo statement : ((StatementInfo) block).getElseStatements()) {
                this.buildControlDependence(fromPDGNode, statement, false);
            }

            for (final ProgramElementInfo updater : ((StatementInfo) block).getUpdaters()) {
                final PDGNode<?> toPDGNode = this.pdgNodeFactory.makeNormalNode(updater);
                this.allNodes.add(toPDGNode);
                final PDGControlDependenceEdge edge = new PDGControlDependenceEdge(fromPDGNode, toPDGNode, true);
                fromPDGNode.addForwardEdge(edge);
                toPDGNode.addBackwardEdge(edge);
            }
        }
    }

    /**
     * Try to build a control dependency edge from "fromPDGNode" to the statement with a boolean value on the edge.
     * @param fromPDGNode The source PDG node
     * @param statement The target statement
     * @param type The value of the edge, such as "true" for "if-then", "false" for "if-else"
     */
    private void buildControlDependence(final PDGControlNode fromPDGNode,
                                        final StatementInfo statement,
                                        final boolean type) {
        switch (statement.getCategory()) {
            case Catch, Do, For, Foreach, If, SimpleBlock, Synchronized, Switch, Try, While -> {
                final ProgramElementInfo condition = statement.getCondition();
                if (null != condition) {
                    final PDGNode<?> toPDGNode = this.pdgNodeFactory.makeControlNode(condition);
                    this.allNodes.add(toPDGNode);
                    final PDGControlDependenceEdge edge = new PDGControlDependenceEdge(fromPDGNode, toPDGNode, type);
                    fromPDGNode.addForwardEdge(edge);
                    toPDGNode.addBackwardEdge(edge);
                } else {
                    this.buildControlDependence(fromPDGNode, statement);
                }

                for (final ProgramElementInfo initializer : statement.getInitializers()) {
                    final PDGNode<?> toPDGNode = this.pdgNodeFactory.makeNormalNode(initializer);
                    this.allNodes.add(toPDGNode);
                    final PDGControlDependenceEdge edge = new PDGControlDependenceEdge(fromPDGNode, toPDGNode, type);
                    fromPDGNode.addForwardEdge(edge);
                    toPDGNode.addBackwardEdge(edge);
                }
            }
            case Assert, Break, Case, Continue, Expression, Return, Throw, VariableDeclaration -> {
                final CFGNode<?> cfgNode = this.cfgNodeFactory.getNode(statement);
                if ((null != cfgNode) && (this.cfg.getAllNodes().contains(cfgNode))) {
                    final PDGNode<?> toPDGNode = this.pdgNodeFactory.makeNormalNode(statement);
                    this.allNodes.add(toPDGNode);
                    final PDGControlDependenceEdge edge = new PDGControlDependenceEdge(fromPDGNode, toPDGNode, type);
                    fromPDGNode.addForwardEdge(edge);
                    toPDGNode.addBackwardEdge(edge);
                }
            }
            default -> {}
        }
    }


    private void buildExecutionDependence(final CFGNode<?> toCFGNode,
                                          final PDGNode<?> fromPDGNode) {
        final PDGNode<?> toPDGNode = this.pdgNodeFactory.makeNode(toCFGNode);
        this.allNodes.add(toPDGNode);
        final PDGExecutionDependenceEdge edge = new PDGExecutionDependenceEdge(fromPDGNode, toPDGNode);
        fromPDGNode.addForwardEdge(edge);
        toPDGNode.addBackwardEdge(edge);
    }

}
