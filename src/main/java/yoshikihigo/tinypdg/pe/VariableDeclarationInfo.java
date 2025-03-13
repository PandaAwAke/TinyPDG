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

import yoshikihigo.tinypdg.pe.var.VarDef;
import lombok.Getter;
import org.eclipse.jdt.core.dom.VariableDeclaration;

/**
 * Describe the information of a variable (in the ast).
 * Including parameters and SingleVariableDeclaration.
 */
@Getter
public class VariableDeclarationInfo extends ProgramElementInfo {

	/**
	 * The category of this variable.
	 */
    public CATEGORY category;

	/**
	 * The TypeInfo of this variable.
	 */
	final public TypeInfo type;

	/**
	 * The name of this variable.
	 */
	final public String name;

	public VariableDeclarationInfo(final CATEGORY category,
								   final TypeInfo type,
								   final String name,
								   final VariableDeclaration node,
								   final int startLine,
								   final int endLine) {
		super(node, startLine, endLine);
		this.category = category;
		this.type = type;
		this.name = name;
	}

	public void setCategory(final CATEGORY category) {
		assert null != category : "\"category\" is null.";
		this.category = category;
	}

	@Override
	protected void doCalcDefVariables() {
		this.addVarDef(null, name, VarDef.Type.DECLARE);
	}

	/**
     * All supported variable types.
     */
    public enum CATEGORY {
		FIELD,
		LOCAL,
		PARAMETER,
	}

}
