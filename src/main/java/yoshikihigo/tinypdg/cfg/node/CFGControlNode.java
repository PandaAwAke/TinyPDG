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

import yoshikihigo.tinypdg.pe.ProgramElementInfo;

/**
 * CFG Control Node.
 * Usually including: Try, Catch, Synchronized, Loops, TypeDeclaration, If, Switch.
 */
public class CFGControlNode extends CFGNode<ProgramElementInfo> {

	public CFGControlNode(final ProgramElementInfo expression) {
		super(expression);
	}
}
