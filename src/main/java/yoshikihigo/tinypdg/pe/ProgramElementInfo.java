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

import yoshikihigo.tinypdg.pe.var.Scope;
import yoshikihigo.tinypdg.pe.var.VarDef;
import yoshikihigo.tinypdg.pe.var.VarUse;
import lombok.Getter;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

abstract public class ProgramElementInfo implements Comparable<ProgramElementInfo> {

	/**
	 * The ID generator to generate a unique id for every ProgramElementInfo (thread safely).
	 */
	final static private AtomicInteger ID_GENERATOR = new AtomicInteger(0);

	/**
	 * The unique id of the program element.
	 */
	@Getter
	final public int id;

	/**
	 * The original ast node from the parser.
	 * Usually used for debugging, or getting additional information from the ast.
	 */
	@Getter
	final public Object node;

	/**
	 * The start line number of the program element (ast).
	 */
	@Getter
	final public int startLine;

	/**
	 * The end line number of the program element (ast).
	 */
	@Getter
	final public int endLine;

	/**
	 * One type of the text representation for the program element.
	 */
	@Getter
	private String text;

	/**
	 * The modifiers of the program element (ast) (if exists).
	 */
	@Getter
	final private List<String> modifiers;

	/**
	 * Used to mark whether this program element is the "condition" of another element
	 * (such as the condition of an "If" statement).
	 * If so, this is the entire block of the StatementInfo (such as the whole "If" StatementInfo)
	 */
	@Getter
    protected BlockInfo ownerConditionalBlock;

	/**
	 * Whether we should treat non-local variables as field such as "this.source".
	 */
	protected static boolean TREAT_NON_LOCAL_VARIABLE_AS_FIELD = true;


	/**
	 * Whether we should treat uppercase started variables as field.
	 * This will take effect only when TREAT_NON_LOCAL_VARIABLE_AS_FIELD was enabled.
	 */
	protected static boolean TREAT_FIELD_EXCLUDE_UPPERCASE = true;


	public ProgramElementInfo(final Object node, final int startLine, final int endLine) {
		this.node = node;
		this.startLine = startLine;
		this.endLine = endLine;
		this.id = ID_GENERATOR.getAndIncrement();
		this.text = "";

		this.modifiers = new ArrayList<>();

		this.ownerConditionalBlock = null;
	}

	@Override
	final public int hashCode() {
		return this.id;
	}

	@Override
	final public boolean equals(final Object o) {
		if (!(o instanceof ProgramElementInfo target)) {
			return false;
		}

        return this.id == target.id;
	}

	@Override
	final public int compareTo(final ProgramElementInfo element) {
		assert null != element : "\"element\" is null.";
        return Integer.compare(this.id, element.id);
	}

	final public void setText(final String text) {
		assert null != text : "\"text\" is null.";
		this.text = text;
	}

	final public void addModifier(final String modifier) {
		assert null != modifier : "\"modifier\" is null.";
		this.modifiers.add(modifier);
	}


	// --------------------- Variable Uses & Defs ---------------------

	private Set<VarDef> defVariables = null;
	private Set<VarUse> useVariables = null;

	/**
     * Analyze the defined variables (i.e. defs) in this program element.
	 * May contain NO_DEF defs.
     * @return The variable defs in the program element
     */
	public Set<VarDef> getDefVariables() {
		if (null == defVariables) {
			defVariables = new HashSet<>();
			doCalcDefVariables();
		}
		return Collections.unmodifiableSet(defVariables);
	}

	/**
     * Analyze the defined variables (i.e. defs) in this program element.
	 * Does not contain NO_DEF defs.
     * @return The variable defs in the program element
     */
	public Set<VarDef> getDefVariablesAtLeastMayDef() {
		return getDefVariables().stream()
				.filter(def -> def.getType().isAtLeastMayDef())
				.collect(Collectors.toSet());
	}

	/**
     * Analyze the used variables (i.e. uses) in this program element.
	 * May contain NO_USE uses.
     * @return The variable uses in the program element
     */
	public Set<VarUse> getUseVariables() {
		if (null == useVariables) {
			useVariables = new HashSet<>();
			doCalcUseVariables();
		}
		return Collections.unmodifiableSet(useVariables);
	}

	/**
     * Analyze the used variables (i.e. uses) in this program element.
	 * Does not contain NO_USE uses.
     * @return The variable uses in the program element
     */
	public Set<VarUse> getUseVariablesAtLeastMayUse() {
		return getUseVariables().stream()
				.filter(use -> use.getType().isAtLeastMayUse())
				.collect(Collectors.toSet());
	}

	/**
	 * Add a variable def. This should be the only interface for the subclasses to use.
	 * @param mainVariableName Variable name
	 * @param type Def type, can be one of UNKNOWN, DEF, MAY_DEF
	 */
	protected void addVarDef(Scope scope, String mainVariableName, VarDef.Type type) {
		this.defVariables.add(new VarDef(scope, mainVariableName, Set.of(mainVariableName), type));
	}

	/**
	 * Add a variable def. This should be the only interface for the subclasses to use.
	 * @param varDef Var def
	 */
	protected void addVarDef(VarDef varDef) {
		this.defVariables.add(new VarDef(varDef));
	}

	/**
	 * Add a variable use. This should be the only interface for the subclasses to use.
	 * @param mainVariableName Variable name
	 * @param type Use type, can be one of UNKNOWN, USE, MAY_USE
	 */
	protected void addVarUse(Scope scope, String mainVariableName, VarUse.Type type) {
		this.useVariables.add(new VarUse(scope, mainVariableName, Set.of(mainVariableName), type));
	}

	/**
	 * Add a variable use. This should be the only interface for the subclasses to use.
	 * @param varUse Var use
	 */
	protected void addVarUse(VarUse varUse) {
		this.useVariables.add(new VarUse(varUse));
	}

	/**
	 * The real place to calculate defs.
	 */
	protected void doCalcDefVariables() {
		// Do nothing by default, implement it in your subclass
	}

	/**
	 * The real place to calculate uses.
	 */
	protected void doCalcUseVariables() {
		// Do nothing by default, implement it in your subclass
	}


	public void setOwnerConditionalBlock(final BlockInfo ownerConditionalBlock) {
		assert null != ownerConditionalBlock : "\"ownerConditionalBlock\" is null.";
		this.ownerConditionalBlock = ownerConditionalBlock;
	}

	@Override
	public String toString() {
		return text;
//		return node.toString();
	}


}
