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

import yoshikihigo.tinypdg.pdg.node.PDGControlNode;
import yoshikihigo.tinypdg.pdg.node.PDGNode;
import lombok.Getter;

/**
 * PDG Control edge.
 */
@Getter
public class PDGControlDependenceEdge extends PDGEdge {

	/**
	 * The value on the control dependency edge, such as "true" for "if-true" and "false" for "if-else".
	 */
	final public boolean trueDependence;

	public PDGControlDependenceEdge(final PDGControlNode fromNode, final PDGNode<?> toNode, final boolean trueDependence) {
		super(TYPE.CONTROL, fromNode, toNode);
		this.trueDependence = trueDependence;
	}

    public boolean isFalseDependence() {
		return !this.trueDependence;
	}

	@Override
	public PDGEdge replaceFromNode(final PDGNode<?> fromNode) {
		assert null != fromNode : "\"fromNode\" is null.";
		assert fromNode instanceof PDGControlNode : "\"fromNode\" must be an instanceof PDGControlNode.";
		return new PDGControlDependenceEdge((PDGControlNode) fromNode, this.toNode, this.trueDependence);
	}

	@Override
	public PDGEdge replaceToNode(final PDGNode<?> toNode) {
		assert null != toNode : "\"toNode\" is null.";
		return new PDGControlDependenceEdge((PDGControlNode) this.fromNode, toNode, this.trueDependence);
	}

	@Override
	public String getDependenceString() {
		return this.trueDependence ? "true" : "false";
	}
}
