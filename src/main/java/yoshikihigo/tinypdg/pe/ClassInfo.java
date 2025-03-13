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

import lombok.Getter;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Describe the information of a Class / AnonymousClass (in the ast).
 */
public class ClassInfo extends ProgramElementInfo {

	/**
	 * The name of the class. For anonymous class, this is null.
	 */
	final public String name;

	/**
	 * The MethodInfo of methods in this class.
	 */
	@Getter
	final private List<MethodInfo> methods;

	public ClassInfo(final String name, final ASTNode node, final int startLine, final int endLine) {
		super(node, startLine, endLine);
		this.name = name;
		this.methods = new ArrayList<>();
	}

	/**
	 * Check whether this is an anonymous class.
	 * @return true if this is an anonymous class
	 */
	public boolean isAnonymous() {
		return null == this.name;
	}

	/**
	 * Add a MethodInfo.
	 * @param method MethodInfo
	 */
	public void addMethod(final MethodInfo method) {
		assert null != method : "\"method\" is null.";
		this.methods.add(method);
	}

}
