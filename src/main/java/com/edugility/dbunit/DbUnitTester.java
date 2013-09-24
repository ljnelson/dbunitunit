/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright (c) 2013 Edugility LLC.
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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import java.sql.Connection; // for javadoc only

import org.dbunit.IDatabaseTester; // for javadoc only
import org.dbunit.JdbcDatabaseTester; // for javadoc only

import org.dbunit.dataset.IDataSet; // for javadoc only

/**
 * An annotation that describes an {@link IDatabaseTester}.
 *
 * <p>In typical usage, you may annotate {@link Connection} fields or
 * {@link IDatabaseTester} fields with this annotation.  In the former
 * case, a {@link DbUnitRule} will construct an {@link
 * IDatabaseTester} that wraps your {@link Connection}, creating the
 * {@link Connection} itself if necessary (the {@link IDatabaseTester}
 * can be acquired by using the {@link
 * DbUnitRule#getIDatabaseTester(Connection)} method).  In the latter
 * case, a {@link DbUnitRule} will create a new {@link
 * JdbcDatabaseTester} initialized with the values taken from this
 * annotation's attributes, and will inject the new {@link
 * IDatabaseTester} into the field for you.</p>
 *
 * <h4>Design Notes</h4>
 *
 * <p>Because annotation attributes cannot default to {@code null},
 * and because in some cases an empty attribute value is valid, some
 * of the attributes in this annotation default to the literal {@link
 * String} "{@code null}".  Code that processes annotations of this
 * type should treat such values as indicating {@code null}.</p>
 *
 * @author <a href="http://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 *
 * @see IDatabaseTester
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DbUnitTester {

  /**
   * A JDBC-compliant connection URL describing how to connect to a
   * database.
   */
  String connectionUrl() default "";

  /**
   * The name of a classpath resource identifying an {@link IDataSet}
   * representation.
   */
  String dataSetResource() default "";

  /**
   * A password to use when connecting to the database.  If its value
   * is "{@code null}", then {@code null} will be used instead.
   */
  String password() default "null";

  /**
   * The schema under test.  If its value is "{@code null}", then
   * {@code null} will be used instead.
   */
  String schema() default "null";

  /**
   * The username to use when connecting to the database.  If its
   * value is "{@code null}", then {@code null} will be used instead.
   */
  String username() default "null";

  /**
   * Whether or not DbUnit should attempt to validate the schema.
   */
  boolean validateSchema() default false;

}
