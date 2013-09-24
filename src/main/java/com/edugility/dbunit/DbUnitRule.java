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

import java.lang.reflect.Field;

import java.net.URL;

import java.sql.Connection;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.dbunit.DatabaseUnitException;
import org.dbunit.DefaultDatabaseTester;
import org.dbunit.IDatabaseTester;
import org.dbunit.JdbcDatabaseTester;

import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;

import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.DefaultDataSet;
import org.dbunit.dataset.IDataSet;

import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;

import org.dbunit.operation.DatabaseOperation;

import org.junit.Assert;

import org.junit.internal.runners.statements.ExpectException;

import org.junit.rules.ExternalResource;

import org.junit.runner.Description;

import org.junit.runners.model.FrameworkField;
import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;

/**
 * An {@link ExternalResource} that sets up and injects {@link
 * IDatabaseTester} instances.
 *
 * @author <a href="http://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 *
 * @see ExternalResource
 *
 * @see IDatabaseTester
 */
public class DbUnitRule extends ExternalResource {

  /**
   * A {@link Map} of {@link TestClass} instances indexed by {@link
   * Class}es containing JUnit tests.
   *
   * <p>This field is never {@code null}.</p>
   */
  private static final Map<Class<?>, TestClass> testClasses = new HashMap<Class<?>, TestClass>();

  /**
   * The JUnit test instance.
   *
   * <p>This field is never {@code null}.</p>
   */
  private final Object testInstance;

  /**
   * The {@link Description} desscribing the current JUnit test.
   *
   * <p>This field may be {@code null} at any point.</p>
   */
  private Description description;

  /**
   * A {@link Map} of {@link IDatabaseTester} instances indexed by the
   * {@link Connection}s to which they are attached.
   *
   * <p>This field is never {@code null}.</p>
   */
  private final Map<Connection, IDatabaseTester> testers;


  /*
   * Constructors.
   */


  /**
   * Creates a new {@link DbUnitRule}.
   *
   * @param testInstance the current JUnit test; must not be {@code
   * null}
   *
   * @exception AssertionError if {@code testInstance} is {@code null}
   */
  public DbUnitRule(final Object testInstance) {
    super();
    this.testInstance = testInstance;
    this.testers = new HashMap<Connection, IDatabaseTester>(7);
    Assert.assertNotNull(testInstance);
  }


  /*
   * Instance methods.
   */


  /**
   * Applies this {@link DbUnitRule} to the supplied {@link Statement}
   * in the context of the supplied {@link Description}.
   *
   * @param base the {@link Statement} to "wrap"; JUnit documentation
   * makes no guarantee as to whether this parameter will ever be
   * {@code null} or not
   *
   * @param description the {@link Description} describing the test;
   * JUnit documentation makes no guarantee as to whether this
   * parameter will ever be {@code null} or not
   *
   * @return a {@link Statement}; JUnit documentation makes no
   * guarantee as to whether this value will ever be {@code null}
   * or not
   */
  @Override
  public Statement apply(final Statement base, final Description description) {
    this.description = description;
    final Statement s = super.apply(base, description);
    return s;
  }

  /**
   * Scans the test class for fields annotated with occurrences of the
   * {@link DbUnitTester} annotation, and when those fields are either
   * of type {@link Connection} or {@link IDatabaseTester} creates an
   * {@link IDatabaseTester} instance to govern them.
   *
   * <p>If the field so processed was of type {@link Connection}, then
   * its associated {@link IDatabaseTester} may be retrieved by use of
   * the {@link #getIDatabaseTester(Connection)} method.</p>
   *
   * @exception Throwable if any of a host of errors occurs
   */
  @Override
  protected void before() throws Throwable {
    Assert.assertNotNull(this.testInstance);
    final TestClass testClass = this.getTestClass();
    if (testClass != null) {
      final Collection<FrameworkField> annotatedFields = testClass.getAnnotatedFields(DbUnitTester.class);
      if (annotatedFields != null && !annotatedFields.isEmpty()) {
        for (final FrameworkField ff : annotatedFields) {
          if (ff != null) {
            final Field f = ff.getField();
            if (f != null) {
              final Class<?> fieldType = f.getType();
              assert fieldType != null;
              if (Connection.class.isAssignableFrom(fieldType)) {
                final DbUnitTester dbUnitTester = f.getAnnotation(DbUnitTester.class);
                Assert.assertNotNull(dbUnitTester);              
                // Connection field annotated with DbUnitTester.
                this.processConnection(f, dbUnitTester);
              } else if (IDatabaseTester.class.isAssignableFrom(fieldType)) {
                final DbUnitTester dbUnitTester = f.getAnnotation(DbUnitTester.class);
                Assert.assertNotNull(dbUnitTester);
                // IDatabaseTester field annotated with DbUnitTester.
                this.processIDatabaseTester(f, dbUnitTester);
              }
            }
          }
        }
      }
    }
  }

  /**
   * Returns a {@link IDatabaseTester} that is associated with the
   * supplied {@link Connection}.
   *
   * @param key the {@link Connection} for which an {@link
   * IDatabaseTester} should be returned; may be {@code null} in which
   * case {@code null} will be returned
   *
   * @return a {@link IDatabaseTester}, or {@code null}
   */
  public IDatabaseTester getIDatabaseTester(final Connection key) {
    IDatabaseTester tester = null;
    if (key != null) {
      tester = this.testers.get(key);
    }
    return tester;
  }

  private final void processIDatabaseTester(final Field f, final DbUnitTester annotation) throws Exception {
    Assert.assertNotNull(f);
    Assert.assertTrue(IDatabaseTester.class.isAssignableFrom(f.getType()));
    Assert.assertNotNull(annotation);
    IDatabaseTester tester = (IDatabaseTester)f.get(this.testInstance);
    Assert.assertNull(tester);
    tester = this.createIDatabaseTester(annotation, null);
    Assert.assertNotNull(tester);
    this.processIDatabaseTester(tester, this.getDataSetResourceName(f, annotation));
  }

  private final void processIDatabaseTester(final IDatabaseTester tester, final String dataSetResourceName) throws Exception {
    Assert.assertNotNull(tester);
    this.configureTester(tester, dataSetResourceName);
    tester.onSetup();
    final IDatabaseConnection idc = tester.getConnection();
    Assert.assertNotNull(idc);
    this.configureIDatabaseConnection(idc);
    final Connection c = idc.getConnection();
    Assert.assertNotNull(c);
    this.testers.put(c, tester);
  }

  private final void processConnection(final Field f, final DbUnitTester annotation) throws Exception {
    Assert.assertNotNull(f);
    Assert.assertTrue(Connection.class.isAssignableFrom(f.getType()));
    Assert.assertNotNull(annotation);
    final boolean accessibility = f.isAccessible();
    try {
      f.setAccessible(true);
      final Connection c = (Connection)f.get(this.testInstance);
      final IDatabaseTester tester = this.createIDatabaseTester(annotation, c);
      Assert.assertNotNull(tester);
      this.processIDatabaseTester(tester, this.getDataSetResourceName(f, annotation));
      if (c == null) {
        final IDatabaseConnection idc = tester.getConnection();
        Assert.assertNotNull(idc);
        f.set(this.testInstance, idc.getConnection());
      }
    } finally {
      f.setAccessible(accessibility);
    }
  }

  /**
   * Returns the name of a classpath resource that picks out a
   * classpath resource from which an {@link IDataSet} may be
   * assembled that is appropriate for the {@link Field} that is
   * annotated with the supplied {@link DbUnitTester} annotation.
   *
   * <p>This method may return {@code null}.</p>
   *
   * <p>This implementation retrieves the value of the {@link
   * DbUnitTester#dataSetResource()} attribute and returns it, with
   * one caveat.  If the value of the {@link
   * DbUnitTester#dataSetResource()} attribute is equal
   * (case-insensitively) to the value "{@code null}", then {@code
   * null} is returned.</p>
   *
   * @param f the {@link Field} that is annotated; provided as a
   * convenience to overriders.  The value of this parameter is never
   * {@code null}.
   *
   * @param annotation the {@link DbUnitTester} that is being looked
   * at.  The value of this parameter is never {@code null}.
   *
   * @return a classpath resource name, or {@code null}
   *
   * @exception Exception if any of a whole host of errors occurs
   */
  protected String getDataSetResourceName(final Field f, final DbUnitTester annotation) throws Exception {
    Assert.assertNotNull(f);
    Assert.assertNotNull(annotation);
    String dataSetResourceName = annotation.dataSetResource();
    if (dataSetResourceName != null && dataSetResourceName.isEmpty()) {
      dataSetResourceName = null;
    }
    return dataSetResourceName;
  }

  /**
   * Creates a {@link IDatabaseTester} given a {@link DbUnitTester}
   * annotation that describes it, and a (possibly {@code null})
   * {@link Connection} and returns it.
   *
   * <p>This method never returns {@code null}.</p>
   *
   * <p>Overrides of this method must ensure that {@code null} is
   * never returned.</p>
   *
   * @param annotation the {@link DbUnitTester} annotation that
   * describes the {@link IDatabaseTester} to be created; must not be
   * {@code null}
   *
   * @param c a {@link Connection} which&mdash;if non-{@code
   * null}&mdash;will be associated with the newly created {@link
   * IDatabaseTester}; may be {@code null}
   *
   * @return a non-{@code null} IDatabaseTester
   *
   * @exception DatabaseUnitException if the supplied {@link
   * Connection} was non-{@code null} and a new {@link
   * DatabaseConnection} could not be created to wrap it
   *
   * @see DatabaseConnection
   *
   * @see IDatabaseTester
   */
  protected IDatabaseTester createIDatabaseTester(final DbUnitTester annotation, final Connection c) throws DatabaseUnitException {
    Assert.assertNotNull(annotation);
    String schema = annotation.schema();
    if (schema != null && schema.equalsIgnoreCase("null")) {
      schema = null;
    }
    final IDatabaseTester tester;
    if (c == null) {
      final String connectionUrl = annotation.connectionUrl();
      if (connectionUrl == null || connectionUrl.isEmpty()) {
        throw new IllegalStateException("no connectionUrl value");
      }
      String username = annotation.username();
      if (username != null && username.equalsIgnoreCase("null")) {
        username = null;
      }
      String password = annotation.username();
      if (password != null && password.equalsIgnoreCase("null")) {
        password = null;
      }
      try {
        tester = new JdbcDatabaseTester("java.lang.Object", connectionUrl, username, password, schema);
      } catch (final ClassNotFoundException willNeverHappen) {
        throw (InternalError)new InternalError().initCause(willNeverHappen);
      }
    } else {
      tester = new DefaultDatabaseTester(new DatabaseConnection(c, schema, annotation.validateSchema()));
    }
    return tester;
  }

  /**
   * Given an {@link IDatabaseTester} and a name that should pick out
   * the raw materials from which to assemble an {@link IDataSet},
   * returns a non-{@code null} {@link IDataSet} instance
   * corresponding to both.
   *
   * <p>This method never returns {@code null} and overrides must
   * ensure that {@code null} is not returned.</p>
   *
   * <p>It is not assumed that the {@link IDataSet} returned by this
   * method is {@linkplain IDatabaseTester#setDataSet(IDataSet) set on
   * the supplied <code>IDatabaseTester</code>} instance.  That
   * association is established by code elsewhere in the {@link
   * DbUnitRule} class.</p>
   *
   * <p>This implementation attempts to return an {@link IDataSet} as
   * returned by the {@link FlatXmlDataSetBuilder#build(URL)} method.
   * If that cannot happen, then a new {@link DefaultDataSet} is
   * returned instead.</p>
   *
   * @param tester the {@link IDatabaseTester} for which a {@link
   * IDataSet} should be produced; supplied as a convenience.  The
   * value of this parameter is never {@code null}.
   *
   * @param dataSetName a name for a {@link IDataSet} as returned by
   * the {@link #getDataSetResourceName(Field, DbUnitTester)} method;
   * may be {@code null}
   *
   * @return a non-{@code null} {@link IDataSet} instance
   *
   * @exception Exception if an error occurs
   */
  protected IDataSet getIDataSet(final IDatabaseTester tester, final String dataSetName) throws Exception {
    Assert.assertNotNull(tester);
    final URL dataSetUrl;
    if (dataSetName != null) {
      final ClassLoader ccl = Thread.currentThread().getContextClassLoader();
      Assert.assertNotNull(ccl);
      dataSetUrl = ccl.getResource(dataSetName);
    } else {
      dataSetUrl = null;
    }
    if (dataSetUrl != null) {
      return new FlatXmlDataSetBuilder().setCaseSensitiveTableNames(false).build(dataSetUrl);
    }
    return new DefaultDataSet();
  }

  /**
   * Configures the supplied {@link IDatabaseTester} for use.
   *
   * <p>This implementation calls the {@link
   * #getIDataSet(IDatabaseTester, String)} method and {@linkplain
   * IDatabaseTester#setDataSet(IDataSet) sets its return value on the
   * supplied <code>IDatabaseTester</code>}.  It then calls {@link
   * IDatabaseTester#setSetUpOperation(DatabaseOperation)} with a
   * value of {@link DatabaseOperation#CLEAN_INSERT}, and calls {@link
   * IDatabaseTester#setTearDownOperation(DatabaseOperation)} with a
   * value of {@link DatabaseOperation#NONE}.</p>
   *
   * @param tester the {@link IDatabaseTester} to configure.  The
   * value of this parameter is never {@code null}.
   *
   * @param dataSetResourceName a name as returned by the {@link
   * #getDataSetResourceName(Field, DbUnitTester)} method; may be
   * {@code null}
   *
   * @exception Exception if an error occurs
   *
   * @see #getIDataSet(IDatabaseTester, String)
   *
   * @see #getDataSetResourceName(Field, DbUnitTester)
   */
  protected void configureTester(final IDatabaseTester tester, final String dataSetResourceName) throws Exception {
    Assert.assertNotNull(tester);
    final IDataSet dataSet = this.getIDataSet(tester, dataSetResourceName);
    Assert.assertNotNull(dataSet);
    tester.setDataSet(dataSet);
    tester.setSetUpOperation(DatabaseOperation.CLEAN_INSERT);
    tester.setTearDownOperation(DatabaseOperation.NONE);
  }

  /**
   * Configures the supplied {@link IDatabaseConnection}.
   *
   * <p>This implementation does nothing.</p>
   *
   * @param idc the {@link IDatabaseConnection} to configure.  The
   * value of this parameter is never {@code null}.
   *
   * @exception AssertionError if {@code idc} is {@code null}
   */
  protected void configureIDatabaseConnection(final IDatabaseConnection idc) {
    Assert.assertNotNull(idc);
  }

  /**
   * Calls the {@link IDatabaseTester#onTearDown()} method on every
   * {@link IDatabaseTester} instance created by this class.
   */
  @Override
  protected void after() {
    this.description = null;
    if (this.testers != null && !this.testers.isEmpty()) {
      final Iterable<Entry<Connection, IDatabaseTester>> entrySet = this.testers.entrySet();
      if (entrySet != null) {
        for (final Entry<Connection, IDatabaseTester> entry : entrySet) {
          if (entry != null) {
            final IDatabaseTester tester = entry.getValue();
            if (tester != null) {
              try {
                tester.onTearDown();
              } catch (final RuntimeException throwMe) {
                throw throwMe;
              } catch (final Exception everythingElse) {
                throw new RuntimeException(everythingElse);
              }
            }
          }
        }
      }
    }
  }

  /**
   * Returns a {@link TestClass} suitable for this {@link DbUnitRule}
   * after indexing it in the {@link #testClasses} {@link Map} under
   * the {@link Class} that is returned by the {@link
   * Description#getTestClass()} method.
   *
   * @return a {@link TestClass}; may be {@code null} since JUnit
   * documentation makes no guarantees otherwise
   */
  private final TestClass getTestClass() {
    TestClass testClass = null;
    final Description description = this.description;
    if (description != null) {
      final Class<?> c = description.getTestClass();
      if (c != null) {
        synchronized (testClasses) {
          testClass = testClasses.get(c);
          if (testClass == null) {
            testClass = new TestClass(c);
            testClasses.put(c, testClass);
          }
        }
      }
    }
    return testClass;
  }

}
