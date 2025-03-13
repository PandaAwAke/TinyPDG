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

package yoshikihigo.tinypdg.cfg.node;

import yoshikihigo.tinypdg.cfg.node.CFGPseudoNode.PseudoElement;
import yoshikihigo.tinypdg.pe.ProgramElementInfo;

/**
 * The pseudo (fake) node. This can be used to represent null values.
 */
public class CFGPseudoNode extends CFGNode<PseudoElement> {

	/**
	 * The pseudo (fake) program element.
	 */
	public static class PseudoElement extends ProgramElementInfo {
		PseudoElement() {
			super(null, 0, 0);
		}
	}

	public CFGPseudoNode() {
		super(new PseudoElement());
	}

}
