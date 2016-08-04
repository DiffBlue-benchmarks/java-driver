/*
 *      Copyright (C) 2012-2015 DataStax Inc.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.datastax.driver.core;

import com.datastax.driver.core.utils.Bytes;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.testng.annotations.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.datastax.driver.core.StatementFormatter.StatementFormatVerbosity.*;
import static com.datastax.driver.core.TestUtils.getFixedValue;
import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static com.datastax.driver.core.schemabuilder.SchemaBuilder.createTable;
import static com.google.common.base.Strings.repeat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @jira_ticket JAVA-1257
 */
public class StatementFormatterTest {

    private static final List<DataType> dataTypes = new ArrayList<DataType>(
            Sets.filter(DataType.allPrimitiveTypes(TestUtils.getDesiredProtocolVersion()), new Predicate<DataType>() {
                @Override
                public boolean apply(DataType type) {
                    return type != DataType.counter();
                }
            }));


    private final ProtocolVersion version = TestUtils.getDesiredProtocolVersion();

    private final CodecRegistry codecRegistry = CodecRegistry.DEFAULT_INSTANCE;

    // Basic Tests

    @Test(groups = "unit")
    public void should_format_statement() throws Exception {
        StatementFormatter formatter = StatementFormatter.builder().build();
        Statement statement = new Statement() {
            @Override
            public ByteBuffer getRoutingKey(ProtocolVersion protocolVersion, CodecRegistry codecRegistry) {
                return null;
            }

            @Override
            public String getKeyspace() {
                return null;
            }
        };
        String s = formatter.format(statement, EXTENDED, version, codecRegistry);
        assertThat(s)
                .contains("com.datastax.driver.core.StatementFormatterTest$");
    }

    @Test(groups = "unit")
    public void should_format_regular_statement() throws Exception {
        StatementFormatter formatter = StatementFormatter.builder().build();
        Statement statement = new RegularStatement() {
            @Override
            public ByteBuffer getRoutingKey(ProtocolVersion protocolVersion, CodecRegistry codecRegistry) {
                return null;
            }

            @Override
            public String getKeyspace() {
                return null;
            }

            @Override
            public String getQueryString(CodecRegistry codecRegistry) {
                return "this is the query string";
            }

            @Override
            public ByteBuffer[] getValues(ProtocolVersion protocolVersion, CodecRegistry codecRegistry) {
                return null;
            }

            @Override
            public Map<String, ByteBuffer> getNamedValues(ProtocolVersion protocolVersion, CodecRegistry codecRegistry) {
                return null;
            }

            @Override
            public boolean hasValues(CodecRegistry codecRegistry) {
                return false;
            }

            @Override
            public boolean usesNamedValues() {
                return false;
            }
        };
        String s = formatter.format(statement, EXTENDED, version, codecRegistry);
        assertThat(s)
                .contains("com.datastax.driver.core.StatementFormatterTest")
                .contains("this is the query string");
    }

    @Test(groups = "unit")
    public void should_format_simple_statement() throws Exception {
        StatementFormatter formatter = StatementFormatter.builder().build();
        Statement statement = new SimpleStatement("SELECT * FROM t WHERE c1 = ? AND c2 = ?", "foo", 42);
        String s = formatter.format(statement, EXTENDED, version, codecRegistry);
        assertThat(s)
                .contains("com.datastax.driver.core.SimpleStatement@")
                .contains("[2 bound values]")
                .contains("SELECT * FROM t WHERE c1 = ? AND c2 = ?")
                .contains("{ 0 : 'foo', 1 : 42 }");
    }

    @Test(groups = "unit")
    public void should_format_simple_statement_with_named_values() throws Exception {
        StatementFormatter formatter = StatementFormatter.builder().build();
        Statement statement = new SimpleStatement("SELECT * FROM t WHERE c1 = ? AND c2 = ?",
                ImmutableMap.<String, Object>of("c1", "foo", "c2", 42));
        String s = formatter.format(statement, EXTENDED, version, codecRegistry);
        assertThat(s)
                .contains("com.datastax.driver.core.SimpleStatement@")
                .contains("[2 bound values]")
                .contains("SELECT * FROM t WHERE c1 = ? AND c2 = ?")
                .contains("{ c1 : 'foo', c2 : 42 }");
    }

    @Test(groups = "unit")
    public void should_format_statement_without_values() throws Exception {
        StatementFormatter formatter = StatementFormatter.builder().build();
        Statement statement = new SimpleStatement("SELECT * FROM t WHERE c1 = 42");
        String s = formatter.format(statement, EXTENDED, version, codecRegistry);
        assertThat(s)
                .contains("com.datastax.driver.core.SimpleStatement@")
                .contains("[0 bound values]")
                .contains("SELECT * FROM t WHERE c1 = 42")
                .doesNotContain("{");
    }

    @Test(groups = "unit")
    public void should_format_built_statement() throws Exception {
        StatementFormatter formatter = StatementFormatter.builder().build();
        Statement statement = select().from("t").where(eq("c1", "foo")).and(eq("c2", 42)).and(eq("c3", false));
        String s = formatter.format(statement, EXTENDED, version, codecRegistry);
        assertThat(s)
                .contains("com.datastax.driver.core.querybuilder.Select$Where@")
                .contains("[2 bound values]")
                .contains("SELECT * FROM t WHERE c1=? AND c2=42 AND c3=?")
                .contains("{ 0 : 'foo', 1 : false }");
    }

    @Test(groups = "unit")
    public void should_format_schema_statement() throws Exception {
        StatementFormatter formatter = StatementFormatter.builder().build();
        Statement statement = createTable("t")
                .addPartitionKey("c1", DataType.cint())
                .addClusteringColumn("c2", DataType.varchar())
                .addColumn("c3", DataType.cboolean());
        String s = formatter.format(statement, EXTENDED, version, codecRegistry);
        assertThat(s)
                .contains("com.datastax.driver.core.schemabuilder.Create@")
                .contains("CREATE TABLE t(\n" +
                        "\t\tc1 int,\n" +
                        "\t\tc2 varchar,\n" +
                        "\t\tc3 boolean,\n" +
                        "\t\tPRIMARY KEY(c1, c2))");
    }

    @Test(groups = "unit")
    public void should_format_bound_statement() throws Exception {
        StatementFormatter formatter = StatementFormatter.builder().build();
        BoundStatement statement = newBoundStatementMock("SELECT * FROM t WHERE c1 = ? AND c2 = ? AND c3 = ?");
        String s = formatter.format(statement, EXTENDED, version, codecRegistry);
        assertThat(s)
                .contains("com.datastax.driver.core.BoundStatement")
                .contains("[3 bound values]")
                .contains("SELECT * FROM t WHERE c1 = ? AND c2 = ? AND c3 = ?")
                .contains("{ c1 : 'foo', c2 : <NULL>, c3 : <UNSET> }");
    }

    @Test(groups = "unit")
    public void should_format_batch_statement() throws Exception {
        StatementFormatter formatter = StatementFormatter.builder().build();
        BatchStatement statement = new BatchStatement(BatchStatement.Type.UNLOGGED);
        Statement inner1 = newBoundStatementMock("SELECT * FROM t WHERE c1 = ? AND c2 = ? AND c3 = ?");
        Statement inner2 = new SimpleStatement("SELECT * FROM t WHERE c1 = ? AND c2 = ?", "foo", 42);
        Statement inner3 = select().from("t").where(eq("c1", "foo")).and(eq("c2", 42));
        statement.add(inner1);
        statement.add(inner2);
        statement.add(inner3);
        String s = formatter.format(statement, EXTENDED, version, codecRegistry);
        assertThat(s)
                .contains("com.datastax.driver.core.BatchStatement")
                .contains("[UNLOGGED, 3 inner statements, 6 bound values]")
                .contains(formatter.format(inner1, EXTENDED, version, codecRegistry))
                .contains(formatter.format(inner2, EXTENDED, version, codecRegistry))
                .contains(formatter.format(inner3, EXTENDED, version, codecRegistry));
    }

    @Test(groups = "unit")
    public void should_format_wrapped_statement() throws Exception {
        StatementFormatter formatter = StatementFormatter.builder().build();
        Statement statement = new StatementWrapper(new SimpleStatement("SELECT * FROM t WHERE c1 = 42")) {
        };
        String s = formatter.format(statement, EXTENDED, version, codecRegistry);
        assertThat(s)
                .contains("com.datastax.driver.core.SimpleStatement@")
                .contains("[0 bound values]")
                .contains("SELECT * FROM t WHERE c1 = 42")
                .doesNotContain("{");
    }

    // Verbosity

    @Test(groups = "unit")
    public void should_format_with_abridged_verbosity() throws Exception {
        StatementFormatter formatter = StatementFormatter.builder().build();
        Statement statement = new SimpleStatement("SELECT * FROM t WHERE c1 = ? AND c2 = ?", "foo", 42);
        String s = formatter.format(statement, ABRIDGED, version, codecRegistry);
        assertThat(s)
                .contains("com.datastax.driver.core.SimpleStatement@")
                .contains("[2 bound values]")
                .doesNotContain("SELECT * FROM t WHERE c1 = ? AND c2 = ?")
                .doesNotContain("{ 0 : 'foo', 1 : 42 }");
    }

    @Test(groups = "unit")
    public void should_format_with_normal_verbosity() throws Exception {
        StatementFormatter formatter = StatementFormatter.builder().build();
        Statement statement = new SimpleStatement("SELECT * FROM t WHERE c1 = ? AND c2 = ?", "foo", 42);
        String s = formatter.format(statement, NORMAL, version, codecRegistry);
        assertThat(s)
                .contains("com.datastax.driver.core.SimpleStatement@")
                .contains("[2 bound values]")
                .contains("SELECT * FROM t WHERE c1 = ? AND c2 = ?")
                .doesNotContain("{ 0 : 'foo', 1 : 42 }");
    }

    // Limits

    @Test(groups = "unit")
    public void should_truncate_query_string() throws Exception {
        StatementFormatter formatter = StatementFormatter.builder()
                .withMaxQueryStringLength(7)
                .build();
        SimpleStatement statement = new SimpleStatement("123456789");
        String s = formatter.format(statement, EXTENDED, version, codecRegistry);
        assertThat(s)
                .contains("1234567<TRUNCATED>");
    }

    @Test(groups = "unit")
    public void should_not_truncate_query_string_when_unlimited() throws Exception {
        StatementFormatter formatter = StatementFormatter.builder()
                .withUnlimitedQueryStringLength()
                .build();
        String query = repeat("a", 5000);
        SimpleStatement statement = new SimpleStatement(query);
        String s = formatter.format(statement, EXTENDED, version, codecRegistry);
        assertThat(s)
                .contains(query);
    }

    @Test(groups = "unit")
    public void should_not_print_more_bound_values_than_max() throws Exception {
        StatementFormatter formatter = StatementFormatter.builder()
                .withMaxBoundValues(2)
                .build();
        SimpleStatement statement = new SimpleStatement("query", 0, 1, 2, 3);
        String s = formatter.format(statement, EXTENDED, version, codecRegistry);
        assertThat(s)
                .contains("{ 0 : 0, 1 : 1, <OTHER VALUES OMITTED> }");
    }

    @Test(groups = "unit")
    public void should_truncate_bound_value() throws Exception {
        StatementFormatter formatter = StatementFormatter.builder()
                .withMaxBoundValueLength(4)
                .build();
        SimpleStatement statement = new SimpleStatement("query", "12345");
        String s = formatter.format(statement, EXTENDED, version, codecRegistry);
        assertThat(s)
                .contains("{ 0 : '123<TRUNCATED> }");
    }

    @Test(groups = "unit")
    public void should_truncate_bound_value_byte_buffer() throws Exception {
        StatementFormatter formatter = StatementFormatter.builder()
                .withMaxBoundValueLength(4)
                .build();
        SimpleStatement statement = new SimpleStatement("query", Bytes.fromHexString("0xCAFEBABE"));
        String s = formatter.format(statement, EXTENDED, version, codecRegistry);
        assertThat(s)
                .contains("{ 0 : 0xca<TRUNCATED> }");
    }

    @Test(groups = "unit")
    public void should_truncate_inner_statements() throws Exception {
        StatementFormatter formatter = StatementFormatter.builder()
                .withMaxInnerStatements(2)
                .build();
        BatchStatement statement = new BatchStatement(BatchStatement.Type.UNLOGGED);
        Statement inner1 = newBoundStatementMock("SELECT * FROM t WHERE c1 = ? AND c2 = ? AND c3 = ?");
        Statement inner2 = new SimpleStatement("SELECT * FROM t WHERE c1 = ? AND c2 = ?", "foo", 42);
        Statement inner3 = select().from("t").where(eq("c1", "foo")).and(eq("c2", 42));
        statement.add(inner1);
        statement.add(inner2);
        statement.add(inner3);
        String s = formatter.format(statement, EXTENDED, version, codecRegistry);
        assertThat(s)
                .contains("com.datastax.driver.core.BatchStatement")
                .contains("[UNLOGGED, 3 inner statements, 6 bound values]")
                .contains(formatter.format(inner1, EXTENDED, version, codecRegistry))
                .contains(formatter.format(inner2, EXTENDED, version, codecRegistry))
                .doesNotContain(formatter.format(inner3, EXTENDED, version, codecRegistry))
                .contains("<OTHER STATEMENTS OMITTED>");
    }


    // Custom printers

    private static class CustomStatement extends Statement {

        @Override
        public ByteBuffer getRoutingKey(ProtocolVersion protocolVersion, CodecRegistry codecRegistry) {
            return null;
        }

        @Override
        public String getKeyspace() {
            return null;
        }

    }

    private static class CustomStatementPrinter implements StatementFormatter.StatementPrinter<CustomStatement> {

        @Override
        public Class<CustomStatement> getSupportedStatementClass() {
            return CustomStatement.class;
        }

        @Override
        public void print(CustomStatement statement, StatementFormatter.StatementWriter out, StatementFormatter.StatementFormatVerbosity verbosity) {
            out.getBuffer().append("This is a statement with a ");
            // also incidentally test multiple appends to the query string
            out.appendQueryStringFragment("QUERY");
            out.appendQueryStringFragment("STR");
            out.appendQueryStringFragment("ING");
        }

    }

    @Test(groups = "unit")
    public void should_use_custom_printer() throws Exception {
        CustomStatement statement = new CustomStatement();
        StatementFormatter formatter = StatementFormatter.builder()
                .withMaxQueryStringLength(5)
                .addStatementPrinter(new CustomStatementPrinter()).build();
        String s = formatter.format(statement, EXTENDED, version, codecRegistry);
        assertThat(s)
                .isEqualTo("This is a statement with a QUERY<TRUNCATED>");
    }

    // Data types

    @Test(groups = "short")
    public void should_log_all_parameter_types_simple_statements() throws Exception {
        String query = "UPDATE test SET c1 = ? WHERE pk = 42";
        StatementFormatter formatter = StatementFormatter.builder()
                .withUnlimitedBoundValueLength()
                .build();
        for (DataType type : dataTypes) {
            Object value = getFixedValue(type);
            SimpleStatement statement = new SimpleStatement(query, value);
            String s = formatter.format(statement, EXTENDED, version, codecRegistry);
            // time cannot be used with simple statements
            TypeCodec<Object> codec = codecRegistry.codecFor(type.equals(DataType.time()) ? DataType.bigint() : type, value);
            assertThat(s).contains(codec.format(value));
        }
    }

    @Test(groups = "short")
    public void should_log_all_parameter_types_bound_statements() throws Exception {
        String query = "UPDATE test SET c1 = ? WHERE pk = 42";
        StatementFormatter formatter = StatementFormatter.builder()
                .withUnlimitedBoundValueLength()
                .build();
        for (DataType type : dataTypes) {
            Object value = getFixedValue(type);
            BoundStatement statement = newBoundStatementMock(query, type);
            TypeCodec<Object> codec = codecRegistry.codecFor(type, value);
            statement.set(0, value, codec);
            String s = formatter.format(statement, EXTENDED, version, codecRegistry);
            assertThat(s).contains(codec.format(value));
        }
    }

    private BoundStatement newBoundStatementMock(String queryString) {
        PreparedStatement ps = mock(PreparedStatement.class);
        ColumnDefinitions cd = mock(ColumnDefinitions.class);
        PreparedId pid = new PreparedId(null, cd, null, null, version);
        when(ps.getVariables()).thenReturn(cd);
        when(ps.getPreparedId()).thenReturn(pid);
        when(ps.getCodecRegistry()).thenReturn(codecRegistry);
        when(ps.getQueryString()).thenReturn(queryString);
        when(cd.size()).thenReturn(3);
        when(cd.getName(0)).thenReturn("c1");
        when(cd.getName(1)).thenReturn("c2");
        when(cd.getName(2)).thenReturn("c3");
        when(cd.getType(0)).thenReturn(DataType.varchar());
        when(cd.getType(1)).thenReturn(DataType.cint());
        when(cd.getType(2)).thenReturn(DataType.cboolean());
        BoundStatement statement = new BoundStatement(ps);
        statement.setString(0, "foo");
        statement.setToNull(1);
        return statement;
    }

    private BoundStatement newBoundStatementMock(String queryString, DataType type) {
        PreparedStatement ps = mock(PreparedStatement.class);
        ColumnDefinitions cd = mock(ColumnDefinitions.class);
        PreparedId pid = new PreparedId(null, cd, null, null, version);
        when(ps.getVariables()).thenReturn(cd);
        when(ps.getPreparedId()).thenReturn(pid);
        when(ps.getCodecRegistry()).thenReturn(codecRegistry);
        when(ps.getQueryString()).thenReturn(queryString);
        when(cd.size()).thenReturn(1);
        when(cd.getName(0)).thenReturn("c1");
        when(cd.getType(0)).thenReturn(type);
        return new BoundStatement(ps);
    }



}
