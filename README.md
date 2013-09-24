<!-- -*- markdown -*- -->
# `dbunitunit`

## DbUnit Unit Testing Utilities

### September 24, 2013

### [Laird Nelson][1]

`dbunitunit` is a small project that provides [JUnit][2] utilities
that make it easy to use [DbUnit][3] in your unit tests.  This project
is best used with the [`h2unit`][4] project as well.

### Sample Code

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
        assertNotNull(c);
        final IDatabaseTester tester = this.dbUnitRule.getIDatabaseTester(this.h2);
        assertNotNull(tester);
      }

    }

[1]: http://about.me/lairdnelson
[2]: http://junit.org
[3]: http://www.dbunit.org
[4]: http://github.com/ljnelson/h2unit
