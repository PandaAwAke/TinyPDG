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

import yoshikihigo.tinypdg.pe.ExpressionInfo;
import yoshikihigo.tinypdg.pe.MethodInfo;
import yoshikihigo.tinypdg.pe.ProgramElementInfo;
import org.eclipse.jdt.core.dom.ASTNode;

/**
 * The fake "entry" node for a method.
 */
public class PDGMethodEnterNode extends PDGControlNode {

	/**
	 * Create a new PDGMethodEnterNode instance.
	 * @param method The method of the PDG.
	 * @return The fake PDGMethodEnterNode
	 */
	static public PDGMethodEnterNode getInstance(final MethodInfo method) {
		assert null != method : "\"method\" is null.";
		assert method.node instanceof ASTNode : "\"method.node\" is not an ASTNode.";
		final ProgramElementInfo methodEnterExpression = new ExpressionInfo(
				ExpressionInfo.CATEGORY.MethodEnter,
                (ASTNode) method.node,
				method.startLine,
				method.endLine);
		methodEnterExpression.setText("Enter");
		return new PDGMethodEnterNode(methodEnterExpression);
	}

	private PDGMethodEnterNode(final ProgramElementInfo methodEnterExpression) {
		super(methodEnterExpression);
	}
}
