package org.apache.lucene.search;

/**
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;

final class BooleanScorer extends Scorer
{

  /** This is a linked list of subclause scorers */
  private SubScorer scorers = null;

  /** This is the number of non-prohibited clauses we encountered, plus one */
  private int maxCoord = 1;

  /** This is an array of the results of the Similarity.coord() function, which
  * will be calculated when the number of non-prohibited clauses is finally known */
  private float[] coordFactors = null;

  /** requiredMask keeps track of which bit positions are "required" */
  private int requiredMask = 0;
  /** prohibitedMask keeps track of which bit positions are "prohibited" */
  private int prohibitedMask = 0;

  /** This is the next bit mask to use for a subscorer for a subclause.
  * This limits the kinds that need masks to 32 */
  private int nextMask = 1;

  BooleanScorer(Similarity similarity)
  {
    super(similarity);
  }


  /** Add a scorer (of any kind) into a Boolean scorer.  The scorer's methods will be used to score
  * the combined result.
  * There are three possible legal combinations of parameters:
  * 1) Required: The corresponding clause MUST be matched by the document (AND)
  * 2) Prohibited: The corresponding clause CANNOT be matched by the document (NOT)
  * 3) Nothing special: The corresponding clause MAY be matched by the document (OR)
  */
  final void add(Scorer scorer, boolean required, boolean prohibited)
    throws IOException {
    int mask = 0;
    // The number of required/prohibited clauses is restricted by this implementation to 32,
    // because a bit mask is used to describe (for a given doc) which of the boolean clauses
    // it meets.  This is only necessary (apparently) when either prohibited or required.
    if (required || prohibited) {
      if (nextMask == 0)
	throw new IndexOutOfBoundsException
	  ("More than 32 required/prohibited clauses in query.");
      mask = nextMask;
      nextMask = nextMask << 1;
    } else
      mask = 0;

    // maxCoord is the "total number of terms in the query".  This gets updated when 
    // the clause is not "prohibited".
    // NOTE that I believe there is a problem here, in that no attempt is made to obtain
    // the "term count" from any of the subqueries that go into the boolean query.  The
    // maxCoord value therefore is only a count of the number of "subqueries that aren't
    // prohibited" in the Boolean query.
    if (!prohibited)
      maxCoord++;

    // prohibitedMask keeps track of which bit positions are "prohibited".
    // requiredMask keeps track of which bit positions are "required".
    if (prohibited)
      prohibitedMask |= mask;			  // update prohibited mask
    else if (required)
      requiredMask |= mask;			  // update required mask

    // Scorers is a tree of the actual scorers.
    // The hit collector also cares about the mask, so pass it in
    SubScorer sub = new SubScorer(scorer, mask, prohibited);
    addToTree(sub);
  }

  /** This method simply precalculates the value of the coord() function from the
  * Similarity implementation we are using.  The array contains the value of the
  * coord function where the index is the number of "terms" that match in a document,
  * measured against the number of subqueries (that aren't "prohibited") in the boolean
  * query.
  * I would hope that coordFactors[] is therefore indexed by the number of subqueries that
  * match for a given document, not the number of terms...
  */
  private final void computeCoordFactors() throws IOException {
    coordFactors = new float[maxCoord];
    for (int i = 0; i < maxCoord; i++)
      coordFactors[i] = getSimilarity().coord(i, maxCoord-1);
  }

  // Local current values - these are the current values for the document being worked on.
  // They are reset whenever another document is started.
  protected int currentDoc;
  protected float currentScore;
  protected int currentCoord;
  protected int currentBits;

  /** Get the current document number for this Boolean scorer.
  */
  public int doc()
  {
	// Use the current bucket, which MUST have been set up when next() was called
	// the first time.
	return currentDoc;
  }

  /** Get the score for the current document for this scorer.
  */
  public float score() throws IOException
  {
    // Do the precalculation of the coord[] array, if needed
    if (coordFactors == null)
      computeCoordFactors();

    // We adjust the current document's score by the number of terms matched in the subscorer,
    // vs. the number of non-prohibited subscorers!!!
    // (Not sure this is in fact correct, unless Bucket.coord represents somehow a number of
    // sub-scorers).
    return currentScore * coordFactors[currentCoord];
  }

  /** Advance to the next document.
  * This method will advance (internally) to the next valid current document bucket.  It also looks like
  * this document fills the hit collector as needed, from the subscorers.
  * 
  *@return false if there are no more documents; true otherwise.
  */
  public boolean next() throws IOException
  {
    // The algorithm here is to first loop through the linked list of scorers, and find the minimum document id #.
    // Then, we reset the current* variables, and collect all the data from the scorers for that document
    // (doing a "next()" for each one, of course).
    // Finally, if the collected data is excluded, we go on to the next one.
    while (true)
    {
	// Reset all collector variables
	currentDoc = -1;
	currentBits = 0;
	currentScore = 0.0f;
	currentCoord = 0;

	// Look for lowest doc id first
	SubScorer list = scorers;
	if (list == null)
	{
		return false;
	}
	SubScorer prev = null;
	while (true)
	{
		if (list.lesser == null)
			break;
		prev = list;
		list = list.lesser;
	}

	// Disconnect from chain
	if (prev == null)
		scorers = list.greater;
	else
		prev.lesser = list.greater;
	list.greater = null;

	currentDoc = list.currentDoc;

	// Now, do the collection phase
	while (list != null)
	{
		SubScorer sub = list;
		list = list.next;
		currentBits |= sub.mask;
		if (!sub.prohibited)
		{
			currentScore += sub.scorer.score();
			currentCoord++;
		}
		if (sub.next())
			addToTree(sub);
	}

        // Now, check if the current document should be excluded or not
	// This document can only be used if it is NOT prohibited in any of the
	// places that matched it, and is present in all the required places.
        if ((currentBits & prohibitedMask) == 0 && 
            (currentBits & requiredMask) == requiredMask)
		break;

	continue;
    }
    return true;
  }

  /** This method's semantics are: set the stream to the specified doc id, then
  * decide if there is anything more on the stream, and return true if so.
  */
  public boolean skipTo(int target) throws IOException
  {
	// Keep getting the lowest, and doing "skip to", until the lowest is >= the
	// target.
	while (true)
	{
		SubScorer list = scorers;
		// If the active tree is empty, it means there are no more matches
		if (list == null)
			return false;
		SubScorer prev = null;
		while (true)
		{
			if (list.lesser == null)
				break;
			prev = list;
			list = list.lesser;
		}

		// If the id >= target, then we are done.
		// Furthermore, we know there are more id's to go.
		if (list.currentDoc >= target)
			break;

		// Disconnect from chain
		if (prev == null)
			scorers = list.greater;
		else
			prev.lesser = list.greater;
		list.greater = null;

		// Walk through the list and do the skip
		while (list != null)
		{
			SubScorer sub = list;
			list = list.next;
			if (sub.skipTo(target))
				addToTree(sub);
		}

		// Loop back around again.
	}

    // Ok, there are other POSSIBLE choices.  We have to assure ourselves that
    // a legal one exists though, before returning true.
    while (true)
    {
	// Reset all collector variables
	int currentDoc = -1;
	int currentBits = 0;

	// Look for lowest doc id first
	SubScorer list = scorers;
	if (list == null)
	{
		return false;
	}
	SubScorer prev = null;
	while (true)
	{
		if (list.lesser == null)
		{
			// Do NOT disconnect from chain; but allow
			// ourselves the option if this turns out to
			// be a valid combo
			break;
		}
		prev = list;
		list = list.lesser;
	}

	// Now, do the collection phase
	SubScorer sub = list;
	while (sub != null)
	{
		currentBits |= sub.mask;
		sub = sub.next;
	}

        // Now, check if the current document should be excluded or not
	// This document can only be used if it is NOT prohibited in any of the
	// places that matched it, and is present in all the required places.
        if ((currentBits & prohibitedMask) == 0 && 
            (currentBits & requiredMask) == requiredMask)
		break;

	// Advance past this document; it doesn't qualify
	// First, disconnect the list from the chain
	if (prev == null)
		scorers = list.greater;
	else
		prev.lesser = list.greater;
	list.greater = null;

	// Now, advance each one - requeue only if there's more stuff.
	while (list != null)
	{
		sub = list;
		list = list.next;
		if (sub.next())
			addToTree(sub);
	}

	continue;
    }

    return true;
  }

  public Explanation explain(int doc) throws IOException {
    throw new UnsupportedOperationException();
  }

  public String toString() {
    StringBuffer buffer = new StringBuffer();
    buffer.append("boolean(");
    if (scorers != null)
	buffer.append(scorers.toString());
    buffer.append(")");
    return buffer.toString();
  }

  /** This method inserts a SubScorer object into the scorers tree, using the document identifier
  * to order it.
  */
  protected void addToTree(SubScorer sub)
  {
	int currentDoc = sub.currentDoc;
	if (currentDoc == -1)
		return;
	SubScorer current = scorers;
	SubScorer prev = null;
	boolean lesser = false;
	while (true)
	{
		if (current == null)
		{
			// Add it here
			sub.next = null;
			if (prev == null)
				scorers = sub;
			else
			{
				if (lesser)
					prev.lesser = sub;
				else
					prev.greater = sub;
			}
			return;
		}
		if (currentDoc == current.currentDoc)
		{
			// Add into current doc chain
			sub.next = current.next;
			current.next = sub;
			return;
		}
		if (currentDoc < current.currentDoc)
		{
			// Go down lesser chain
			lesser = true;
			prev = current;
			current = current.lesser;
		}
		else
		{
			// Go down greater chain
			lesser = false;
			prev = current;
			current = current.greater;
		}
	}
  }

  /** Each subscorer is represented by one of these.
  */
  static final class SubScorer
  {
    public Scorer scorer;
    public boolean done = false;
    public int mask;
    public SubScorer next = null;
    public SubScorer lesser = null;
    public SubScorer greater = null;
    public boolean prohibited;
    public int currentDoc;	// Current document number; will be -1 if end

    public SubScorer(Scorer scorer, int mask, boolean prohibited)
      throws IOException
    {
      this.scorer = scorer;
      this.mask = mask;
      this.prohibited = prohibited;
      // Initialize by doing a next()
      next();
    }

    /** Proceed to the next document for this scorer.
    */
    public boolean next()
	throws IOException
    {
      if (done)
	return false;
      done = !scorer.next();
      if (done == false)
      {
		currentDoc = scorer.doc();
		return true;
      }
      currentDoc = -1;
      return false;
    }

    /** Skip to a target doc id, and return true if there is stuff after that.
    */
    public boolean skipTo(int target)
	throws IOException
    {
	if (done)
		return false;
	done = !scorer.skipTo(target);
      if (done == false)
      {
		currentDoc = scorer.doc();
		return true;
      }
      currentDoc = -1;
      return false;
    }

    /** Get this as a string.
    */
    public String toString()
    {
	StringBuffer sb = new StringBuffer();
	sb.append(scorer.toString());
	if (next != null)
		sb.append(" ").append(next.toString());
	if (lesser != null)
		sb.append(" ").append(lesser.toString());
	if (greater != null)
		sb.append(" ").append(greater.toString());
	return sb.toString();
    }

  }

}

