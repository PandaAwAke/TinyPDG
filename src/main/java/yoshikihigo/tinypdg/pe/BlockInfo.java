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

import java.util.Collection;
import java.util.List;

/**
 * Describe a ProgramElement that can lead a block.
 */
public interface BlockInfo {

	/**
	 * Set the statement information of the block.
	 * In detail, it firstly clears the statements,
	 * then if statement is a simple block, then all statements inside will be added.
	 * Otherwise, only add one statement.
	 * @param statement The statement to set
	 */
	void setStatement(StatementInfo statement);

	/**
	 * Add a statement information to the block.
	 * @param statement The statement to add
	 */
	void addStatement(StatementInfo statement);

	/**
	 * Add multiple statement information to the block.
	 * @param statements The statements to add
	 */
	default void addStatements(Collection<StatementInfo> statements) {
		assert null != statements : "\"statements\" is null.";
		for (StatementInfo statement : statements) {
			addStatement(statement);
		}
	}

	/**
	 * Get the statements in the block.
	 * @return The statements in the block
	 */
	List<StatementInfo> getStatements();

}
