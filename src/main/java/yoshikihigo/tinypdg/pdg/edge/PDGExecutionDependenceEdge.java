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

package yoshikihigo.tinypdg.pdg.edge;

import yoshikihigo.tinypdg.pdg.node.PDGNode;

/**
 * PDG execution edge.
 */
public class PDGExecutionDependenceEdge extends PDGEdge {

	public PDGExecutionDependenceEdge(final PDGNode<?> fromNode,
			final PDGNode<?> toNode) {
		super(TYPE.EXECUTION, fromNode, toNode);
	}

	@Override
	public PDGEdge replaceFromNode(final PDGNode<?> fromNode) {
		assert null != fromNode : "\"fromNode\" is null.";
		return new PDGExecutionDependenceEdge(fromNode, this.toNode);
	}

	@Override
	public PDGEdge replaceToNode(final PDGNode<?> toNode) {
		assert null != toNode : "\"toNode\" is null.";
		return new PDGExecutionDependenceEdge(this.fromNode, toNode);
	}

	@Override
	public String getDependenceString() {
		return "";
	}
}
