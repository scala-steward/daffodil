/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
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

package org.apache.daffodil.grammar
import org.apache.daffodil.dsom.DFDLNewVariableInstance
import org.apache.daffodil.dsom.Term
import org.apache.daffodil.grammar.primitives._

/////////////////////////////////////////////////////////////////
// Common to all Terms (Elements and ModelGroups)
/////////////////////////////////////////////////////////////////

trait TermGrammarMixin
  extends AlignedMixin
  with BitOrderMixin { self: Term =>

  override protected final def grammarContext = this

  def termContentBody: Gram

  private lazy val newVars = this.annotationObjs.filter { st =>
    st.isInstanceOf[DFDLNewVariableInstance]
  }.asInstanceOf[Seq[DFDLNewVariableInstance]]

  private lazy val newVarStarts = newVars.map { _.gram(self) }
  private lazy val newVarEnds = newVars.map { _.endGram(self) }

  protected lazy val hasEncoding = optionEncodingRaw.isDefined

  // TODO: replace dfdlScopeBegin and dfdlScopeEnd with a single Combinator.
  protected final lazy val dfdlScopeBegin = prod("dfdlScopeBegin", newVarStarts.length > 0) {
    newVarStarts.fold(mt) { _ ~ _ }
  }

  protected final lazy val dfdlScopeEnd = prod("dfdlScopeEnd", newVarEnds.length > 0) {
    newVarEnds.fold(mt) { _ ~ _ }
  }

  /**
   * public for unit testing use.
   */
  final lazy val Some(es) = {
    //
    // Not sure how to assert this,
    // but an invariant we're assuming here is that we are NOT the
    // root element, which has no enclosing sequence at all.
    //
    // The grammar rules shouldn't be asking for separated stuff
    // in that situation, so we shouldn't be here.
    //
    subset(nearestEnclosingSequence != None, "(Current restriction) There must be an enclosing sequence.")
    nearestEnclosingSequence
  }

  /**
   * Mandatory text alignment or mta
   *
   * mta can only apply to things with encodings. No encoding, no MTA.
   *
   * In addition, it has to be textual data. Just because there's an encoding
   * in the property environment shouldn't get you an MTA region. It has
   * to be textual.
   */
  protected final lazy val mtaBase = prod("mandatoryTextAlignment", hasEncoding) {
    MandatoryTextAlignment(this, knownEncodingAlignmentInBits, false)
  }

  /**
   * Mandatory text alignment for delimiters
   */
  protected final lazy val delimMTA = prod(
    "delimMTA",
    {
      hasDelimiters
    }) {
      // This is different from mtaBase because it passes in 'true' for the
      // last parameter to signify that it is MTA for a delimiter. mtaBase
      // passes in 'false'
      MandatoryTextAlignment(this, knownEncodingAlignmentInBits, true)
    }

}