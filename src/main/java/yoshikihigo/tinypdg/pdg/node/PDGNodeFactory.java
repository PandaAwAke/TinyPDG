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

package yoshikihigo.tinypdg.pdg.node;

import yoshikihigo.tinypdg.cfg.node.CFGControlNode;
import yoshikihigo.tinypdg.cfg.node.CFGNode;
import yoshikihigo.tinypdg.cfg.node.CFGNormalNode;
import yoshikihigo.tinypdg.pe.*;

import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * The factory to generate PDG nodes.
 */
public class PDGNodeFactory {

	/**
     * A cache for the mapping from ProgramElementInfo to PDGNode.
     */
	private final ConcurrentMap<ProgramElementInfo, PDGNode<? extends ProgramElementInfo>> elementToNodeMap;

	public PDGNodeFactory() {
		this.elementToNodeMap = new ConcurrentHashMap<>();
	}

	/**
	 * Make PDG node by a CFG node.
	 * @param node CFG node
	 * @return PDG node
	 */
	public PDGNode<?> makeNode(final CFGNode<?> node) {
		assert null != node : "\"node\" is null.";

		if (node instanceof CFGControlNode) {
			return this.makeControlNode(node.core);
		} else if (node instanceof CFGNormalNode) {
			return this.makeNormalNode(node.core);
		} else {
			assert false : "\"node\" is an invalid parameter.";
			return null;
		}
	}

	/**
	 * Make PDGControlNode by a ProgramElementInfo.
	 * @param element ProgramElementInfo, usually the "entry" or a condition element.
	 * @return PDGControlNode
	 */
	public synchronized PDGNode<?> makeControlNode(final ProgramElementInfo element) {
		assert null != element : "\"element\" is null.";

		PDGNode<?> node = this.elementToNodeMap.get(element);
		if (null != node) {
			return node;
		}

		if (element instanceof ExpressionInfo) {
			node = new PDGControlNode(element);
		} else if (element instanceof VariableDeclarationInfo) {
			node = new PDGControlNode(element);
		} else if (element instanceof MethodInfo) {
			node = PDGMethodEnterNode.getInstance((MethodInfo) element);
		} else {
			assert false : "\"element\" is an invalid parameter.";
		}

		this.elementToNodeMap.put(element, node);
		return node;
	}

	/**
	 * Make PDGNormalNode by a ProgramElementInfo.
	 * @param element ProgramElementInfo
	 * @return PDGNormalNode
	 */
	public synchronized PDGNode<?> makeNormalNode(final ProgramElementInfo element) {
		assert null != element : "\"element\" is null.";

		PDGNode<?> node = this.elementToNodeMap.get(element);
		if (null != node) {
			return node;
		}

		if (element instanceof ExpressionInfo) {
			node = new PDGExpressionNode((ExpressionInfo) element);
		} else if (element instanceof StatementInfo) {
			node = new PDGStatementNode((StatementInfo) element);
		} else if (element instanceof VariableDeclarationInfo) {
			node = new PDGParameterNode((VariableDeclarationInfo) element);
		} else {
			assert false : "\"element\" is an invalid parameter.";
		}

		this.elementToNodeMap.put(element, node);

		return node;
	}

	public SortedSet<PDGNode<?>> getAllNodes() {
        return new TreeSet<>(this.elementToNodeMap.values());
	}

	public int size() {
		return this.elementToNodeMap.size();
	}

}
