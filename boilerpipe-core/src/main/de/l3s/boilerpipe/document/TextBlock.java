// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * boilerpipe
 *
 * Copyright (c) 2009 Christian Kohlschütter
 *
 * The author licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.l3s.boilerpipe.document;

import com.dom_distiller.client.LogUtil;
import com.google.gwt.dom.client.Node;

import de.l3s.boilerpipe.labels.DefaultLabels;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Describes a block of text.
 *
 * A block can be an "atomic" text element (i.e., a sequence of text that is not
 * interrupted by any HTML markup) or a compound of such atomic elements.
 *
 * @author Christian Kohlschütter
 */
public class TextBlock implements Cloneable {
    boolean isContent = false;
    private CharSequence text;
    Set<String> labels = null;

    int offsetBlocksStart;
    int offsetBlocksEnd;

    int numWords;
    int numWordsInAnchorText;
    int numWordsInWrappedLines;
    int numWrappedLines;
    float textDensity;
    float linkDensity;

    List<Node> nonWhitespaceTextElements;
    List<Node> allTextElements;

    private int numFullTextWords = 0;
	private int tagLevel;

    public static final List<Node> EMPTY_NODE_LIST = new LinkedList<Node>();
    public static final TextBlock EMPTY_START = new TextBlock("", EMPTY_NODE_LIST, EMPTY_NODE_LIST,
            0, 0, 0, 0, -1);
    public static final TextBlock EMPTY_END = new TextBlock("", EMPTY_NODE_LIST, EMPTY_NODE_LIST,
            0, 0, 0, 0, Integer.MAX_VALUE);

    public TextBlock(final String text) {
        this(text, EMPTY_NODE_LIST, EMPTY_NODE_LIST, 0,0,0,0,0);
    }

    public TextBlock(final String text, final List<Node> containedTextElements,
            List<Node> allTextElements, final int numWords, final int numWordsInAnchorText,
            final int numWordsInWrappedLines, final int numWrappedLines,
            final int offsetBlocks) {
        this.text = text;
        this.nonWhitespaceTextElements = new LinkedList<Node>(containedTextElements);
        this.allTextElements = new LinkedList<Node>(allTextElements);
        this.numWords = numWords;
        this.numWordsInAnchorText = numWordsInAnchorText;
        this.numWordsInWrappedLines = numWordsInWrappedLines;
        this.numWrappedLines = numWrappedLines;
        this.offsetBlocksStart = offsetBlocks;
        this.offsetBlocksEnd = offsetBlocks;
        initDensities();
    }

    public boolean isContent() {
        return isContent;
    }

    public boolean setIsContent(boolean isContent) {
        if (isContent != this.isContent) {
            this.isContent = isContent;
            return true;
        } else {
            return false;
        }
    }

    public String getText() {
        return text.toString();
    }

    public int getNumWords() {
        return numWords;
    }

    public int getNumWordsInAnchorText() {
        return numWordsInAnchorText;
    }

    public float getTextDensity() {
        return textDensity;
    }

    public float getLinkDensity() {
        return linkDensity;
    }

    public void mergeNext(final TextBlock other) {
        if (!(text instanceof StringBuilder)) {
            text = new StringBuilder(text);
        }
        StringBuilder sb = (StringBuilder) text;
        sb.append('\n');
        sb.append(other.text);

        numWords += other.numWords;
        numWordsInAnchorText += other.numWordsInAnchorText;

        numWordsInWrappedLines += other.numWordsInWrappedLines;
        numWrappedLines += other.numWrappedLines;

        offsetBlocksStart = Math
                .min(offsetBlocksStart, other.offsetBlocksStart);
        offsetBlocksEnd = Math.max(offsetBlocksEnd, other.offsetBlocksEnd);

        initDensities();

        this.isContent |= other.isContent;

        if (nonWhitespaceTextElements == null) {
            nonWhitespaceTextElements = new LinkedList<Node>();
        }
        nonWhitespaceTextElements.addAll(other.nonWhitespaceTextElements);
        if (allTextElements == null) {
            allTextElements = new LinkedList<Node>();
        }
        allTextElements.addAll(other.allTextElements);

        numFullTextWords += other.numFullTextWords;

        if (other.labels != null) {
            if (labels == null) {
                labels = new HashSet<String>(other.labels);
            } else {
                labels.addAll(other.labels);
            }
        }

        tagLevel = Math.min(tagLevel, other.tagLevel);
    }

    private void initDensities() {
        if (numWordsInWrappedLines == 0) {
            numWordsInWrappedLines = numWords;
            numWrappedLines = 1;
        }
        textDensity = numWordsInWrappedLines / (float) numWrappedLines;
        linkDensity = numWords == 0 ? 0 : numWordsInAnchorText / (float) numWords;
    }

    public int getOffsetBlocksStart() {
        return offsetBlocksStart;
    }
    public int getOffsetBlocksEnd() {
        return offsetBlocksEnd;
    }

    @Override
    public String toString() {
        return "[" + offsetBlocksStart + "-" + offsetBlocksEnd + ";tl="+tagLevel+"; nw="+numWords+";nwl="+numWrappedLines+";ld="+linkDensity+"]\t" +
                (isContent ? LogUtil.kGreen + "CONTENT" : LogUtil.kPurple + "boilerplate") + LogUtil.kReset +
                "," + LogUtil.kDarkGray + labels + LogUtil.kReset + "\n" + getText();
    }

    /**
     * Adds an arbitrary String label to this {@link TextBlock}.
     *
     * @param label The label
     * @see DefaultLabels
     */
    public void addLabel(final String label) {
        if (labels == null) {
            labels = new HashSet<String>(2);
        }
        labels.add(label);
    }

    /**
     * Checks whether this TextBlock has the given label.
     *
     * @param label The label
     * @return <code>true</code> if this block is marked by the given label.
     */
    public boolean hasLabel(final String label) {
        return labels != null && labels.contains(label);
    }

    public boolean removeLabel(final String label) {
    	return labels != null && labels.remove(label);
    }

    /**
     * Returns the labels associated to this TextBlock, or <code>null</code> if no such labels
     * exist.
     *
     * NOTE: The returned instance is the one used directly in TextBlock. You have full access
     * to the data structure. However it is recommended to use the label-specific methods in {@link TextBlock}
     * whenever possible.
     *
     * @return Returns the set of labels, or <code>null</code> if no labels was added yet.
     */
    public Set<String> getLabels() {
        return labels;
    }

    /**
     * Adds a set of labels to this {@link TextBlock}.
     * <code>null</code>-references are silently ignored.
     *
     * @param l The labels to be added.
     */
    public void addLabels(final Set<String> l) {
        if(l == null) {
            return;
        }
        if(this.labels == null) {
            this.labels = new HashSet<String>(l);
        } else {
            this.labels.addAll(l);
        }
    }

    /**
     * Adds a set of labels to this {@link TextBlock}.
     * <code>null</code>-references are silently ignored.
     *
     * @param l The labels to be added.
     */
    public void addLabels(final String... l) {
        if(l == null) {
            return;
        }
        if(this.labels == null) {
            this.labels = new HashSet<String>();
        }
        for(final String label : l) {
            this.labels.add(label);
        }
    }

    /**
     * @return a list of the non whitespace Text nodes, or <code>null</code>.
     */
    public List<Node> getNonWhitespaceTextElements() {
        return nonWhitespaceTextElements;
    }

    /**
     * @return a list of all Text nodes (including whitespace-only ones), or <code>null</code>.
     */
    public List<Node> getAllTextElements() {
        return allTextElements;
    }

    /**
     * @return the first non-whitespace Text node, or <code>null</code>.
     */
    public Node getFirstNonWhitespaceTextElement() {
        if (nonWhitespaceTextElements.size() > 0) {
            return nonWhitespaceTextElements.get(0);
        }
        return null;
    }

    /**
     * @return the first non-whitespace Text node, or <code>null</code>.
     */
    public Node getLastNonWhitespaceTextElement() {
        if (nonWhitespaceTextElements.size() > 0) {
            return nonWhitespaceTextElements.get(nonWhitespaceTextElements.size() - 1);
        }
        return null;
    }

	public int getTagLevel() {
		return tagLevel;
	}

	public void setTagLevel(int tagLevel) {
		this.tagLevel = tagLevel;
	}
}
