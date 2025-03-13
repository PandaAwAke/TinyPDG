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

package yoshikihigo.tinypdg.cfg;

import yoshikihigo.tinypdg.cfg.edge.CFGControlEdge;
import yoshikihigo.tinypdg.cfg.edge.CFGEdge;
import yoshikihigo.tinypdg.cfg.node.*;
import yoshikihigo.tinypdg.pe.*;
import lombok.Getter;

import java.util.*;

/**
 * The Control Flow Graph of a block (usually a method).
 */
public class CFG {

	/**
	 * The ProgramElementInfo of this CFG.
	 */
	final public ProgramElementInfo core;

	/**
	 * The factory to generate CFG nodes.
	 */
	final private CFGNodeFactory nodeFactory;

	/**
	 * All nodes in the CFG.
	 */
	final protected SortedSet<CFGNode<? extends ProgramElementInfo>> nodes;

	/**
	 * The enter node of the CFG, usually the first statement in this block.
	 */
	@Getter
    protected CFGNode<? extends ProgramElementInfo> enterNode;

	/**
	 * The exit nodes of the CFG.
	 */
	final protected Set<CFGNode<? extends ProgramElementInfo>> exitNodes;

	/**
	 * Temporarily record the unhandled break statements (used for building CFG of a loop block).
	 * It will be handled when leaving the loop CFG building.
	 */
	final protected LinkedList<CFGBreakStatementNode> unhandledBreakStatementNodes;

	/**
	 * Temporarily record the unhandled continue statements (used for building CFG of a loop block).
	 * It will be handled when leaving the loop CFG building.
	 */
	final protected LinkedList<CFGContinueStatementNode> unhandledContinueStatementNodes;

	/**
	 * Mark whether the CFG was already built.
	 */
	protected boolean built;

	public CFG(final ProgramElementInfo core, final CFGNodeFactory nodeFactory) {
		assert null != nodeFactory : "\"nodeFactory\" is null.";
		this.core = core;
		this.nodeFactory = nodeFactory;
		this.nodes = new TreeSet<>();
		this.enterNode = null;
		this.exitNodes = new TreeSet<>();
		this.built = false;

		this.unhandledBreakStatementNodes = new LinkedList<>();
		this.unhandledContinueStatementNodes = new LinkedList<>();
	}

	public boolean isEmpty() {
		return this.nodes.isEmpty();
	}

    public SortedSet<CFGNode<? extends ProgramElementInfo>> getExitNodes() {
        return new TreeSet<>(this.exitNodes);
	}

	public SortedSet<CFGNode<? extends ProgramElementInfo>> getAllNodes() {
		return new TreeSet<>(this.nodes);
	}

	/**
	 * Remove all CFGSwitchCaseNodes in this CFG.
	 */
	public void removeSwitchCases() {
		final Iterator<CFGNode<? extends ProgramElementInfo>> iterator = this.nodes.iterator();
		while (iterator.hasNext()) {
			final CFGNode<? extends ProgramElementInfo> node = iterator.next();
			if (node instanceof CFGSwitchCaseNode) {
				for (final CFGEdge edge : node.getBackwardEdges()) {
					final CFGNode<?> fromNode = edge.fromNode;
					for (final CFGNode<?> toNode : node.getForwardNodes()) {
						final CFGEdge newEdge;
						if (edge instanceof CFGControlEdge) {
							newEdge = CFGEdge.makeControlEdge(fromNode, toNode, ((CFGControlEdge) edge).control);
						} else {
							newEdge = CFGEdge.makeEdge(fromNode, toNode);
						}
						fromNode.addForwardEdge(newEdge);
						toNode.addBackwardEdge(newEdge);
					}
				}
				node.remove();
				iterator.remove();
			}
		}
	}

	/**
	 * Remove all CFGJumpStatementNodes in this CFG.
	 */
	public void removeJumpStatements() {
		final Iterator<CFGNode<? extends ProgramElementInfo>> iterator = this.nodes.iterator();
		while (iterator.hasNext()) {
			final CFGNode<? extends ProgramElementInfo> node = iterator.next();
			if (node instanceof CFGJumpStatementNode) {
				for (final CFGNode<?> fromNode : node.getBackwardNodes()) {
					for (final CFGNode<?> toNode : node.getForwardNodes()) {
						final CFGEdge newEdge = CFGEdge.makeJumpEdge(fromNode, toNode);
						fromNode.addForwardEdge(newEdge);
						toNode.addBackwardEdge(newEdge);
					}
				}
				node.remove();
				iterator.remove();
			}
		}
	}

	/**
	 * Build the CFG by the core.
	 */
	public void build() {
		assert !this.built : "this CFG has already built.";
		this.built = true;
		if (null == this.core) {
			final CFGNode<? extends ProgramElementInfo> node = nodeFactory.makeNormalNode(null);
			this.nodes.add(node);
			this.enterNode = node;
			this.exitNodes.add(node);
		} else if (this.core instanceof StatementInfo coreStatement) {
            switch (coreStatement.getCategory()) {
                case Catch, Synchronized -> this.buildConditionalBlockCFG(coreStatement, false);
                case Do -> this.buildDoBlockCFG(coreStatement);
                case For -> this.buildForBlockCFG(coreStatement);
                case Foreach, While -> this.buildConditionalBlockCFG(coreStatement, true);
                case If -> this.buildIfBlockCFG(coreStatement);
                case Switch -> this.buildSwitchBlockCFG(coreStatement);
                case TypeDeclaration -> {}
                case Try -> this.buildTryBlockCFG(coreStatement);
                default -> {
                    final CFGNode<? extends ProgramElementInfo> node = this.nodeFactory.makeNormalNode(coreStatement);
                    this.enterNode = node;	// This will be handled by SequentialCFGs to select the first enterNode.
                    if (StatementInfo.CATEGORY.Break == coreStatement.getCategory()) {
                        this.unhandledBreakStatementNodes.addFirst((CFGBreakStatementNode) node);
                    } else if (StatementInfo.CATEGORY.Continue == coreStatement.getCategory()) {
                        this.unhandledContinueStatementNodes.addFirst((CFGContinueStatementNode) node);
                    } else {
                        this.exitNodes.add(node);
                    }
                    this.nodes.add(node);
                }
            }
		} else if (this.core instanceof ExpressionInfo coreExpression) {
			final CFGNode<? extends ProgramElementInfo> node = this.nodeFactory.makeNormalNode(coreExpression);
			this.enterNode = node;
			this.exitNodes.add(node);
			this.nodes.add(node);
		} else if (this.core instanceof MethodInfo coreMethod) {
			if (!coreMethod.isLambda()) {
				this.buildSimpleBlockCFG(coreMethod);
			}
		} else {
			assert false : "unexpected state.";
		}

		if (null != this.core) {
			this.removePseudoNodes();//处理错误的结点
		}
	}

	private void buildDoBlockCFG(final StatementInfo statement) {
		final SequentialCFGs sequentialCFGs = new SequentialCFGs(statement.getStatements());
		sequentialCFGs.build();
		final ProgramElementInfo condition = statement.getCondition();
		final CFGNode<? extends ProgramElementInfo> conditionNode = this.nodeFactory.makeControlNode(condition);

		this.enterNode = sequentialCFGs.enterNode;
		this.nodes.addAll(sequentialCFGs.nodes);
		this.nodes.add(conditionNode);
		this.exitNodes.add(conditionNode);
		this.unhandledBreakStatementNodes.addAll(sequentialCFGs.unhandledBreakStatementNodes);
		this.unhandledContinueStatementNodes.addAll(sequentialCFGs.unhandledContinueStatementNodes);

		for (final CFGNode<?> exitNode : sequentialCFGs.exitNodes) {
			final CFGEdge edge = CFGEdge.makeEdge(exitNode, conditionNode);
			exitNode.addForwardEdge(edge);
			conditionNode.addBackwardEdge(edge);
		}

		final CFGEdge edge = CFGEdge.makeControlEdge(conditionNode, sequentialCFGs.enterNode, true);
		conditionNode.addForwardEdge(edge);
		sequentialCFGs.enterNode.addBackwardEdge(edge);

		this.connectCFGBreakStatementNode(statement);
		this.connectCFGContinueStatementNode(statement, this.enterNode);
	}

	private void buildForBlockCFG(final StatementInfo statement) {
		final SequentialCFGs sequentialCFGs = new SequentialCFGs(statement.getStatements());
		sequentialCFGs.build();

		final List<ProgramElementInfo> initializers = statement.getInitializers();
		final ProgramElementInfo condition = statement.getCondition();
		final List<ProgramElementInfo> updaters = statement.getUpdaters();

		final SequentialCFGs initializerCFGs = new SequentialCFGs(initializers);
		initializerCFGs.build();
		final CFGNode<? extends ProgramElementInfo> conditionNode = this.nodeFactory.makeControlNode(condition);
		final SequentialCFGs updaterCFGs = new SequentialCFGs(updaters);
		updaterCFGs.build();

		this.enterNode = initializerCFGs.enterNode;
		this.exitNodes.add(conditionNode);
		this.nodes.addAll(sequentialCFGs.nodes);
		this.nodes.addAll(initializerCFGs.nodes);
		this.nodes.add(conditionNode);
		this.nodes.addAll(updaterCFGs.nodes);
		this.unhandledBreakStatementNodes.addAll(sequentialCFGs.unhandledBreakStatementNodes);
		this.unhandledContinueStatementNodes.addAll(sequentialCFGs.unhandledContinueStatementNodes);

		for (final CFGNode<? extends ProgramElementInfo> initializerExitNode : initializerCFGs.exitNodes) {
			final CFGEdge edge = CFGEdge.makeEdge(initializerExitNode, conditionNode);
			initializerExitNode.addForwardEdge(edge);
			conditionNode.addBackwardEdge(edge);
		}

		{
			final CFGEdge controlEdge = CFGEdge.makeControlEdge(conditionNode, sequentialCFGs.enterNode, true);
			conditionNode.addForwardEdge(controlEdge);
			sequentialCFGs.enterNode.addBackwardEdge(controlEdge);
		}

		for (final CFGNode<? extends ProgramElementInfo> sequentialExitNode : sequentialCFGs.exitNodes) {
			final CFGEdge edge = CFGEdge.makeEdge(sequentialExitNode, updaterCFGs.enterNode);
			sequentialExitNode.addForwardEdge(edge);
			updaterCFGs.enterNode.addBackwardEdge(edge);
		}

		for (final CFGNode<? extends ProgramElementInfo> updaterExitNode : updaterCFGs.exitNodes) {
			final CFGEdge edge = CFGEdge.makeEdge(updaterExitNode, conditionNode);
			updaterExitNode.addForwardEdge(edge);
			conditionNode.addBackwardEdge(edge);
		}

		this.connectCFGBreakStatementNode(statement);
		this.connectCFGContinueStatementNode(statement, conditionNode);
	}

	private void buildConditionalBlockCFG(final StatementInfo statement, final boolean loop) {
		final List<StatementInfo> subStatements = statement.getStatements();
		final SequentialCFGs sequentialCFGs = new SequentialCFGs(subStatements);
		sequentialCFGs.build();
		final ProgramElementInfo condition = statement.getCondition();
		final CFGNode<? extends ProgramElementInfo> conditionNode = this.nodeFactory.makeControlNode(condition);

		this.enterNode = conditionNode;
		this.nodes.addAll(sequentialCFGs.nodes);
		this.nodes.add(conditionNode);
		if (loop) {
			this.exitNodes.add(conditionNode);
		} else {
			this.exitNodes.addAll(sequentialCFGs.exitNodes);
			if (subStatements.isEmpty()) {
				this.exitNodes.add(conditionNode);
			}
		}
		this.unhandledBreakStatementNodes.addAll(sequentialCFGs.unhandledBreakStatementNodes);
		this.unhandledContinueStatementNodes.addAll(sequentialCFGs.unhandledContinueStatementNodes);

		{
			final CFGEdge edge = CFGEdge.makeControlEdge(conditionNode, sequentialCFGs.enterNode, true);
			conditionNode.addForwardEdge(edge);
			sequentialCFGs.enterNode.addBackwardEdge(edge);
		}

		if (loop) {
			for (final CFGNode<?> exitNode : sequentialCFGs.exitNodes) {
				if (exitNode instanceof CFGBreakStatementNode) {
					this.exitNodes.add(exitNode);
				} else {
					final CFGEdge edge = CFGEdge.makeEdge(exitNode, conditionNode);
					exitNode.addForwardEdge(edge);
					conditionNode.addBackwardEdge(edge);
				}
			}

			this.connectCFGBreakStatementNode(statement);
			this.connectCFGContinueStatementNode(statement, conditionNode);
		}
	}

	private void buildIfBlockCFG(final StatementInfo statement) {
		this.buildConditionalBlockCFG(statement, false);

		final ProgramElementInfo condition = statement.getCondition();
		final CFGNode<? extends ProgramElementInfo> conditionNode = this.nodeFactory.makeControlNode(condition);

		if (null != statement.getElseStatements()) {
			final List<StatementInfo> elseStatements = statement.getElseStatements();
			final SequentialCFGs elseCFG = new SequentialCFGs(elseStatements);
			elseCFG.build();

			this.nodes.addAll(elseCFG.nodes);
			this.exitNodes.addAll(elseCFG.exitNodes);
			if (elseStatements.isEmpty()) {
				this.exitNodes.add(conditionNode);
			}

			{
				final CFGEdge edge = CFGEdge.makeControlEdge(conditionNode, elseCFG.enterNode, false);
				conditionNode.addForwardEdge(edge);
				elseCFG.enterNode.addBackwardEdge(edge);
			}
			this.unhandledBreakStatementNodes.addAll(elseCFG.unhandledBreakStatementNodes);
			this.unhandledContinueStatementNodes.addAll(elseCFG.unhandledContinueStatementNodes);
		} else {
			this.exitNodes.add(conditionNode);
		}
	}

	private void buildSimpleBlockCFG(final BlockInfo statement) {
		final List<StatementInfo> subStatements = statement.getStatements();//得到所有的块信息
		final SequentialCFGs sequentialCFGs = new SequentialCFGs(subStatements);
		sequentialCFGs.build();//主要函数调用 创造 结点与边之间

		this.enterNode = sequentialCFGs.enterNode;
		this.exitNodes.addAll(sequentialCFGs.exitNodes);
		this.nodes.addAll(sequentialCFGs.nodes);
		this.unhandledBreakStatementNodes.addAll(sequentialCFGs.unhandledBreakStatementNodes);
		this.unhandledContinueStatementNodes.addAll(sequentialCFGs.unhandledContinueStatementNodes);
	}

	private void buildSwitchBlockCFG(final StatementInfo statement) {
		final ProgramElementInfo condition = statement.getCondition();
		final CFGNode<? extends ProgramElementInfo> conditionNode = this.nodeFactory.makeControlNode(condition);
		this.enterNode = conditionNode;
		this.nodes.add(conditionNode);

		final List<StatementInfo> subStatements = statement.getStatements();
		final List<CFG> sequentialCFGs = new ArrayList<>();
		for (final StatementInfo subStatement : subStatements) {
			final CFG subCFG = new CFG(subStatement, this.nodeFactory);
			subCFG.build();
			sequentialCFGs.add(subCFG);
			this.nodes.addAll(subCFG.nodes);
			this.unhandledBreakStatementNodes.addAll(subCFG.unhandledBreakStatementNodes);
			this.unhandledContinueStatementNodes.addAll(subCFG.unhandledContinueStatementNodes);

            switch (subStatement.getCategory()) {
                case Case -> {
                    final CFGEdge edge = CFGEdge.makeControlEdge(conditionNode, subCFG.enterNode, true);
                    conditionNode.addForwardEdge(edge);
                    subCFG.enterNode.addBackwardEdge(edge);
                }
                case Break, Continue -> this.exitNodes.addAll(subCFG.exitNodes);
                default -> {}
            }
		}

		CFG: for (int index = 1; index < sequentialCFGs.size(); index++) {
			final CFG anteriorCFG = sequentialCFGs.get(index - 1);
			final CFG posteriorCFG = sequentialCFGs.get(index);

			final ProgramElementInfo anteriorCore = anteriorCFG.core;
			if (anteriorCore instanceof StatementInfo) {
                switch (((StatementInfo) anteriorCore).getCategory()) {
                    case Break, Continue -> {
                        continue CFG;
                    }
                    default -> {}
                }
			}

			for (final CFGNode<? extends ProgramElementInfo> anteriorExitNode : anteriorCFG.exitNodes) {
				final CFGEdge edge = CFGEdge.makeEdge(anteriorExitNode, posteriorCFG.enterNode);
				anteriorExitNode.addForwardEdge(edge);
				posteriorCFG.enterNode.addBackwardEdge(edge);
			}
		}

		this.exitNodes.addAll(sequentialCFGs.get(sequentialCFGs.size() - 1).exitNodes);

		this.connectCFGBreakStatementNode(statement);
	}

	private void buildTryBlockCFG(final StatementInfo statement) {
		final List<StatementInfo> statements = statement.getStatements();
		final SequentialCFGs sequentialCFGs = new SequentialCFGs(statements);
		sequentialCFGs.build();

		final StatementInfo finallyBlock = statement.getFinallyStatement();
		final CFG finallyCFG = new CFG(finallyBlock, this.nodeFactory);
		finallyCFG.build();

		this.enterNode = sequentialCFGs.enterNode;
		this.nodes.addAll(sequentialCFGs.nodes);
		this.nodes.addAll(finallyCFG.exitNodes);
		this.exitNodes.addAll(finallyCFG.exitNodes);
		this.unhandledBreakStatementNodes.addAll(sequentialCFGs.unhandledBreakStatementNodes);
		this.unhandledContinueStatementNodes.addAll(sequentialCFGs.unhandledContinueStatementNodes);

		for (final CFGNode<? extends ProgramElementInfo> sequentialExitNode : sequentialCFGs.exitNodes) {
			final CFGEdge edge = CFGEdge.makeEdge(sequentialExitNode, finallyCFG.enterNode);
			sequentialExitNode.addForwardEdge(edge);
			finallyCFG.enterNode.addBackwardEdge(edge);
		}

		for (final StatementInfo catchStatement : statement.getCatchStatements()) {
			final CFG catchCFG = new CFG(catchStatement, this.nodeFactory);
			catchCFG.build();

			this.nodes.addAll(catchCFG.nodes);
			for (final CFGNode<? extends ProgramElementInfo> catchExitNode : catchCFG.exitNodes) {
				final CFGEdge edge = CFGEdge.makeEdge(catchExitNode, finallyCFG.enterNode);
				catchExitNode.addForwardEdge(edge);
				finallyCFG.enterNode.addBackwardEdge(edge);
			}
		}
	}

	/**
     * Remove all pseudo nodes (usually occurs when this.core == null) in the CFG.
	 */
	private void removePseudoNodes() {
		final Iterator<CFGNode<? extends ProgramElementInfo>> iterator = this.nodes.iterator();
		while (iterator.hasNext()) {
			final CFGNode<? extends ProgramElementInfo> node = iterator.next();
			if (node instanceof CFGPseudoNode) {
				iterator.remove();

				if (0 == node.compareTo(this.enterNode)) {
					if (!this.enterNode.getForwardEdges().isEmpty()) {
						this.enterNode = this.enterNode.getForwardNodes().first();
					} else {
						this.enterNode = null;
					}
				}

				if (this.exitNodes.contains(node)) {
					this.exitNodes.addAll(node.getBackwardNodes());
					this.exitNodes.remove(node);
				}

				final SortedSet<CFGNode<? extends ProgramElementInfo>> backwardNodes = node.getBackwardNodes();
				final SortedSet<CFGNode<? extends ProgramElementInfo>> forwardNodes = node.getForwardNodes();
				for (final CFGNode<? extends ProgramElementInfo> backwardNode : backwardNodes) {
					backwardNode.removeForwardNode(node);
				}
				for (final CFGNode<? extends ProgramElementInfo> forwardNode : forwardNodes) {
					forwardNode.removeBackwardNode(node);
				}
				for (final CFGNode<? extends ProgramElementInfo> backwardNode : backwardNodes) {
					for (final CFGNode<? extends ProgramElementInfo> forwardNode : forwardNodes) {
						final CFGEdge edge = CFGEdge.makeEdge(backwardNode, forwardNode);
						backwardNode.addForwardEdge(edge);
						forwardNode.addBackwardEdge(edge);
					}
				}
			}
		}
	}

	/**
     * Handle unhandledBreakStatementNodes to add CFG edges for loops.
	 * @param statement The loop statement
	 */
	private void connectCFGBreakStatementNode(final StatementInfo statement) {
		final Iterator<CFGBreakStatementNode> iterator = this.unhandledBreakStatementNodes.iterator();
		while (iterator.hasNext()) {
			final CFGBreakStatementNode node = iterator.next();
			final StatementInfo breakStatement = node.core;
			final String label = breakStatement.getJumpToLabel();

			if (null == label) {
				this.exitNodes.add(node);
				iterator.remove();
			} else {
				if (label.equals(statement.getLabel())) {
					this.exitNodes.add(node);
					iterator.remove();
				}
			}
		}
	}

	/**
	 * Handle unhandledContinueStatementNodes to add CFG edges for loops.
	 * @param statement The loop statement
	 */
	private void connectCFGContinueStatementNode(final StatementInfo statement,
												 final CFGNode<? extends ProgramElementInfo> destinationNode) {
		final Iterator<CFGContinueStatementNode> iterator = this.unhandledContinueStatementNodes.iterator();
		while (iterator.hasNext()) {
			final CFGContinueStatementNode node = iterator.next();
			final StatementInfo continueStatement = node.core;
			final String label = continueStatement.getJumpToLabel();

			if (null == label) {
				final CFGEdge edge = CFGEdge.makeEdge(node, destinationNode);
				node.addForwardEdge(edge);
				destinationNode.addBackwardEdge(edge);
				iterator.remove();
			} else if (label.equals(statement.getLabel())) {
                final CFGEdge edge = CFGEdge.makeEdge(node, destinationNode);
                node.addForwardEdge(edge);
                destinationNode.addBackwardEdge(edge);
                iterator.remove();
            }
		}

	}


    /**
     * A series of CFG. Usually represents CFGs of multiple statements.
	 * This SequentialCFGs can connect these sequential CFGs together.
	 */
	private class SequentialCFGs extends CFG {

		final List<? extends ProgramElementInfo> elements;

		SequentialCFGs(final List<? extends ProgramElementInfo> elements) {
			super(null, CFG.this.nodeFactory);
			this.elements = elements;
		}

		@Override
		public void build() {
			assert !this.built : "this CFG has already built.";
			this.built = true;

			final LinkedList<CFG> sequentialCFGs = new LinkedList<>();
			for (final ProgramElementInfo element : this.elements) {
				final CFG blockCFG = new CFG(element, CFG.this.nodeFactory);
				blockCFG.build(); // TODO 分析每个块的信息
				if (!blockCFG.isEmpty()) {
					sequentialCFGs.add(blockCFG);
				}
			}
			for (int index = 1; index < sequentialCFGs.size(); index++) {  //建立边之间的关系
				final CFG anteriorCFG = sequentialCFGs.get(index - 1);//前向
				final CFG posteriorCFG = sequentialCFGs.get(index);//后向
				for (final CFGNode<?> exitNode : anteriorCFG.exitNodes) { //以结束的边为基点 因为exitNodes中存储的是所有的边（好像是除了continue和break）
					final CFGEdge edge = CFGEdge.makeEdge(exitNode, posteriorCFG.enterNode);//前向的边与后向的边建立 边之间的联系
					exitNode.addForwardEdge(edge);
					posteriorCFG.enterNode.addBackwardEdge(edge);//后向边
				}
			}
			if (sequentialCFGs.isEmpty()) {
				final CFG pseudoCFG = new CFG(null, CFG.this.nodeFactory);
				pseudoCFG.build();
				sequentialCFGs.add(pseudoCFG);
			}

			this.enterNode = sequentialCFGs.getFirst().enterNode;
			this.exitNodes.addAll(sequentialCFGs.getLast().exitNodes);
			for (final CFG cfg : sequentialCFGs) {
				this.nodes.addAll(cfg.nodes);
				this.unhandledBreakStatementNodes.addAll(cfg.unhandledBreakStatementNodes);
				this.unhandledContinueStatementNodes.addAll(cfg.unhandledContinueStatementNodes);
			}
		}
	}

	/**
     * Get reachable nodes in this CFG.
	 * @return The reachable nodes in this CFG
	 */
	public final SortedSet<CFGNode<? extends ProgramElementInfo>> getReachableNodes() {
		return getReachableNodes(this.enterNode);
	}

	/**
	 * Get reachable nodes in this CFG with a startNode.
	 * @param startNode The start node
	 * @return The reachable nodes in this CFG
	 */
	public final SortedSet<CFGNode<? extends ProgramElementInfo>> getReachableNodes(final CFGNode<? extends ProgramElementInfo> startNode) {
		if (startNode == null) {
			return new TreeSet<>();
		}
		final SortedSet<CFGNode<? extends ProgramElementInfo>> nodes = new TreeSet<>();
		this.getReachableNodes(startNode, nodes);
		return nodes;
	}

	/**
     * Real function to calculate reachable nodes.
	 * @param startNode The start node
	 * @param nodes A set to record reachable nodes
	 */
	private void getReachableNodes(final CFGNode<? extends ProgramElementInfo> startNode,
								   final SortedSet<CFGNode<? extends ProgramElementInfo>> nodes) {
		assert null != startNode : "\"startNode\" is null.";
		assert null != nodes : "\"nodes\" is null.";

		if (nodes.contains(startNode)) {
			return;
		}

		nodes.add(startNode);
		for (final CFGNode<? extends ProgramElementInfo> node : startNode.getForwardNodes()) {
			this.getReachableNodes(node, nodes);
		}
	}

}
