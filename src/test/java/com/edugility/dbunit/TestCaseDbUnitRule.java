/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright (c) 2011-2013 Edugility LLC.
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense and/or sell copies
 * of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT.  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *
 * The original copy of this license is available at
 * http://www.opensource.org/license/mit-license.html.
 */
package com.edugility.dbunit;

import java.sql.Connection;

import com.edugility.h2unit.H2Connection;
import com.edugility.h2unit.H2Rule;

import org.dbunit.IDatabaseTester;

import org.junit.Rule;
import org.junit.Test;

import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

import static org.junit.Assert.*;

public class TestCaseDbUnitRule {

  public final DbUnitRule dbUnitRule = new DbUnitRule(this);

  @Rule
  public final TestRule chain = RuleChain.outerRule(new H2Rule(this)).around(this.dbUnitRule);
  
  @DbUnitTester(connectionUrl = "jdbc:h2:mem:test")
  private Connection c;

  @DbUnitTester
  @H2Connection(url = "jdbc:h2:mem:test", user = "SA", password = "", threadSafe = true)
  private Connection h2;

  public TestCaseDbUnitRule() {
    super();
  }

  @Test
  public void testInjection() {
    assertNotNull(h2);
    System.out.println("H2 connection: " + h2);
    assertNotNull(c);
    System.out.println("c connection: " + c);
    final IDatabaseTester tester = this.dbUnitRule.getIDatabaseTester(this.h2);
    assertNotNull(tester);
  }

}
