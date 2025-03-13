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

import yoshikihigo.tinypdg.cfg.edge.CFGEdge;
import yoshikihigo.tinypdg.pe.ProgramElementInfo;

import java.util.Collection;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * CFG Node.
 * @param <T> The ProgramElementInfo type associated with this node.
 */
public abstract class CFGNode<T extends ProgramElementInfo> implements Comparable<CFGNode<? extends ProgramElementInfo>> {

    protected final SortedSet<CFGEdge> forwardEdges;
    protected final SortedSet<CFGEdge> backwardEdges;

    public final T core;

    protected CFGNode(final T core) {
        this.forwardEdges = new TreeSet<>();
        this.backwardEdges = new TreeSet<>();
        this.core = core;
    }

    public boolean addForwardEdge(final CFGEdge forwardEdge) {
        if (null == forwardEdge) {
            throw new IllegalArgumentException();
        }

        if (!this.equals(forwardEdge.fromNode)) {
            throw new IllegalArgumentException();
        }

        return this.forwardEdges.add(forwardEdge);
    }

    public boolean addBackwardEdge(final CFGEdge backwardEdge) {
        if (null == backwardEdge) {
            throw new IllegalArgumentException();
        }

        if (!this.equals(backwardEdge.toNode)) {
            throw new IllegalArgumentException();
        }

        return this.backwardEdges.add(backwardEdge);
    }

    public boolean removeForwardEdge(final CFGEdge forwardEdge) {
        assert null != forwardEdge : "\"forwardEdge\" is null.";
        return this.forwardEdges.remove(forwardEdge);
    }

    public boolean removeForwardEdges(final Collection<CFGEdge> forwardEdges) {
        if (null == forwardEdges) {
            throw new IllegalArgumentException();
        }

        return this.forwardEdges.removeAll(forwardEdges);
    }

    public boolean removeBackwardEdge(final CFGEdge backwardEdge) {
        assert null != backwardEdge : "\"backwardEdge\" is null.";
        return this.backwardEdges.remove(backwardEdge);
    }

    public boolean removeBackwardEdges(final Collection<CFGEdge> backwardEdges) {
        if (null == backwardEdges) {
            throw new IllegalArgumentException();
        }

        return this.backwardEdges.removeAll(backwardEdges);
    }

    public boolean removeForwardNode(final CFGNode<? extends ProgramElementInfo> node) {
        assert null != node : "\"node\" is null.";
        final Iterator<CFGEdge> iterator = this.forwardEdges.iterator();
        while (iterator.hasNext()) {
            final CFGEdge edge = iterator.next();
            if (0 == edge.toNode.compareTo(node)) {
                iterator.remove();
                return true;
            }
        }
        return false;
    }

    public boolean removeBackwardNode(final CFGNode<? extends ProgramElementInfo> node) {
        assert null != node : "\"node\" is null.";
        final Iterator<CFGEdge> iterator = this.backwardEdges.iterator();
        while (iterator.hasNext()) {
            final CFGEdge edge = iterator.next();
            if (0 == edge.fromNode.compareTo(node)) {
                iterator.remove();
                return true;
            }
        }
        return false;
    }

    /**
     * Remove this CFG node.
     * This will also remove all backward and forward edges for the neighbors of this node.
     */
    public void remove() {
        final SortedSet<CFGEdge> backwardEdges = this.getBackwardEdges();
        final SortedSet<CFGEdge> forwardEdges = this.getForwardEdges();

        for (final CFGEdge edge : backwardEdges) {
            final boolean b = edge.fromNode.removeForwardEdge(edge);
            assert b : "invalid status.";
        }

        for (final CFGEdge edge : forwardEdges) {
            final boolean b = edge.toNode.removeBackwardEdge(edge);
            assert b : "invalid status.";
        }

        this.backwardEdges.clear();
        this.forwardEdges.clear();
    }

    public SortedSet<CFGNode<? extends ProgramElementInfo>> getForwardNodes() {
        final SortedSet<CFGNode<? extends ProgramElementInfo>> forwardNodes = new TreeSet<CFGNode<? extends ProgramElementInfo>>();
        for (final CFGEdge forwardEdge : this.getForwardEdges()) {
            forwardNodes.add(forwardEdge.toNode);
        }
        return forwardNodes;
    }

    public SortedSet<CFGEdge> getForwardEdges() {
        final SortedSet<CFGEdge> forwardEdges = new TreeSet<CFGEdge>();
        forwardEdges.addAll(this.forwardEdges);
        return forwardEdges;
    }

    public SortedSet<CFGNode<? extends ProgramElementInfo>> getBackwardNodes() {
        final SortedSet<CFGNode<? extends ProgramElementInfo>> backwardNodes = new TreeSet<CFGNode<? extends ProgramElementInfo>>();
        for (final CFGEdge backwardEdge : this.getBackwardEdges()) {
            backwardNodes.add(backwardEdge.fromNode);
        }
        return backwardNodes;
    }

    public SortedSet<CFGEdge> getBackwardEdges() {
        final SortedSet<CFGEdge> backwardEdges = new TreeSet<CFGEdge>();
        backwardEdges.addAll(this.backwardEdges);
        return backwardEdges;
    }

    @Override
    public int compareTo(final CFGNode<? extends ProgramElementInfo> node) {
        if (null == node) {
            throw new IllegalArgumentException();
        }

        return Integer.compare(this.core.id, node.core.id);
    }

    public final String getText() {
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
