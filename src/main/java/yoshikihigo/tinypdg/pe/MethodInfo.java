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

import yoshikihigo.tinypdg.pe.var.ScopeManager;
import yoshikihigo.tinypdg.pe.var.VarDef;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Describe the information of a method (in the ast).
 */
@Getter
public class MethodInfo extends ProgramElementInfo implements BlockInfo {

	/**
     * The scope manager used for creating scopes.
     */
    private final ScopeManager scopeManager;

	/**
	 * Used for lambda expression. Mark whether this is a lambda method.
	 */
	final public boolean lambda;

	/**
	 * Used for lambda expression.
	 * If lambda expression only consists of 1 expression, then this is it.
	 */
	private ExpressionInfo lambdaExpression;

	/**
	 * The name of the method.
	 */
	final public String name;

	/**
	 * The parameter VariableInfos.
	 */
	final private List<VariableDeclarationInfo> parameters = new ArrayList<>();

	public List<VariableDeclarationInfo> getParameters() {
		return Collections.unmodifiableList(parameters);
	}

	/**
	 * The statements inside the block.
	 */
	final private List<StatementInfo> statements = new ArrayList<>();

	public List<StatementInfo> getStatements() {
		return Collections.unmodifiableList(statements);
	}

	public MethodInfo(final ScopeManager scopeManager, final boolean lambda, final String name,
					  final Object node, final int startLine, final int endLine) {
		super(node, startLine, endLine);
		this.scopeManager = scopeManager;
		this.lambda = lambda;
		this.name = name;
	}

	/**
	 * Add a parameter VariableInfo.
	 * @param parameter The parameter info to add
	 */
	public void addParameter(final VariableDeclarationInfo parameter) {
		assert null != parameter : "\"variable\" is null.";
		this.parameters.add(parameter);
	}

	@Override
	public void setStatement(final StatementInfo statement) {
		assert null != statement : "\"statement\" is null.";
		this.statements.clear();
		if (StatementInfo.CATEGORY.SimpleBlock == statement.getCategory()) {
			this.statements.addAll(statement.getStatements());
		} else {
			this.statements.add(statement);
		}
	}

	@Override
	public void addStatement(final StatementInfo statement) {
		assert null != statement : "\"statement\" is null.";
		this.statements.add(statement);
	}

	/**
	 * Set the lambda expression if this is a single-expression lambda function.
	 * @param lambdaExpression The single expression in this lambda function
	 */
	public void setLambdaExpression(ExpressionInfo lambdaExpression) {
		assert lambda : "\"lambda\" is false.";
		this.lambdaExpression = lambdaExpression;
	}

	@Override
	protected void doCalcDefVariables() {
		// Make sure that the parameters are calculated firstly
		for (final VariableDeclarationInfo parameter : this.parameters) {
			parameter.getDefVariables().forEach(paramDef -> {
				VarDef def = new VarDef(scopeManager.getScope(this),
						paramDef.getMainVariableName(), paramDef.getVariableNameAliases(), paramDef.getType());
				def.updateScope();
				this.addVarDef(def);
			});
		}
		for (final StatementInfo statement : this.statements) {
			statement.getDefVariables().forEach(this::addVarDef);
		}
		if (lambda && lambdaExpression != null) {
			lambdaExpression.getDefVariables().forEach(this::addVarDef);
		}
	}

	@Override
	protected void doCalcUseVariables() {
		for (final StatementInfo statement : this.statements) {
			statement.getUseVariables().forEach(this::addVarUse);
		}
		if (lambda && lambdaExpression != null) {
			lambdaExpression.getUseVariables().forEach(this::addVarUse);
		}
	}

}
