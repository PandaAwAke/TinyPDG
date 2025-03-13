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

package yoshikihigo.tinypdg.pe;

/**
 * Describe the information of an operator in an expression (in the ast).
 */
public class OperatorInfo extends ProgramElementInfo {

	/**
	 * The token of the operator, such as "++", "="
	 */
	final public String token;

	public OperatorInfo(final String token, final Object node, final int startLine, final int endLine) {
		super(node, startLine, endLine);
		this.token = token;
		this.setText(token);
	}

}
