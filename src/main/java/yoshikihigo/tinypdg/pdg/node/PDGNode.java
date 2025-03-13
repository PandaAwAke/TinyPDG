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

import yoshikihigo.tinypdg.pdg.edge.PDGEdge;
import yoshikihigo.tinypdg.pe.ProgramElementInfo;
import lombok.Getter;

import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Nodes in PDG.
 * @param <T> The ProgramElementInfo type associated with this PDG node.
 */
@Getter
public abstract class PDGNode<T extends ProgramElementInfo> implements Comparable<PDGNode<?>> {

    /**
     * The ProgramElementInfo associated with this PDG node.
     */
    public final T core;
    private final SortedSet<PDGEdge> forwardEdges;
    private final SortedSet<PDGEdge> backwardEdges;

    protected PDGNode(final T core) {
        assert null != core : "\"core\" is null.";
        this.core = core;
        this.forwardEdges = new TreeSet<>();
        this.backwardEdges = new TreeSet<>();
    }

    public boolean addForwardEdge(final PDGEdge edge) {
        assert null != edge : "\"edge\" is null.";
        assert 0 == this.compareTo(edge.fromNode) : "\"edge.fromNode\" must be the same as this object.";
        return this.forwardEdges.add(edge);
    }

    public boolean addBackwardEdge(final PDGEdge edge) {
        assert null != edge : "\"edge\" is null.";
        assert 0 == this.compareTo(edge.toNode) : "\"edge.toNode\" must be the same as this object.";
        return this.backwardEdges.add(edge);
    }

    public boolean removeForwardEdge(final PDGEdge edge) {
        assert null != edge : "\"edge\" is null.";
        assert 0 == this.compareTo(edge.fromNode) : "\"edge.fromNode\" must be the same as this object.";
        return this.forwardEdges.remove(edge);
    }

    public boolean removeBackwardEdge(final PDGEdge edge) {
        assert null != edge : "\"edge\" is null.";
        assert 0 == this.compareTo(edge.toNode) : "\"edge.toNode\" must be the same as this object.";
        return this.backwardEdges.remove(edge);
    }

    public void remove() {
        for (final PDGEdge edge : this.getBackwardEdges()) {
            final PDGNode<?> backwardNode = edge.fromNode;
            backwardNode.removeForwardEdge(edge);
        }

        for (final PDGEdge edge : this.getForwardEdges()) {
            final PDGNode<?> forwardNode = edge.toNode;
            forwardNode.removeBackwardEdge(edge);
        }

        this.backwardEdges.clear();
        this.forwardEdges.clear();
    }

    @Override
    public int compareTo(final PDGNode<?> node) {
        assert null != node : "\"node\" is null.";
        return this.core.compareTo(node.core);
    }

    public String getText() {
        final StringBuilder text = new StringBuilder();
        text.append(this.core.getText());
        text.append(" <");
        if (this.core.startLine == this.core.endLine) {
            text.append(this.core.startLine);
        } else {
            text.append(this.core.startLine);
            text.append("...");
            text.append(this.core.endLine);
        }
        text.append(">");
        return text.toString();
    }

    @Override
    public String toString() {
        return getText();
    }

}
