package com.sohlman.sql.parser;

import com.liferay.portal.kernel.util.StringBundler;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.JdbcParameter;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.ItemsList;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.statement.select.SubSelect;

import org.junit.Assert;
import org.junit.Test;

public class SQLTest {

	@Test
	public void testInnerJoin() throws Exception {
		verifyQueryFile("/sql-inner-join-select-ok.txt");
		verifyQueryFile("/sql-inner-join-select-error.txt");
	}	

	@Test
	public void testInnerJoinStange() throws Exception {
		verifyQueryFile("/sql-inner-join-select-stange-ok.txt");
	}		
	
	@Test
	public void testSimpleSelect() throws Exception {
		verifyQueryFile("/sql-simple-select-ok.txt");
		verifyQueryFile("/sql-simple-select-error.txt");
	}	
	
	@Test
	public void testSimpleSelectGroupBy() throws Exception {
		verifyQueryFile("/sql-simple-select-group-by-ok.txt");
		verifyQueryFile("/sql-simple-select-group-by-error.txt");
	}		

	public void testSimpleSelectOrderBy() throws Exception {
		verifyQueryFile("/sql-simple-select-order-by-ok.txt");
		verifyQueryFile("/sql-simple-select-order-by-error.txt");
	}		
	
	@Test
	public void testSimpleFunctionSelect() throws Exception {
		verifyQueryFile("/sql-simple-function-select-ok.txt");
		verifyQueryFile("/sql-simple-function-select-error.txt");
	}	
	
	@Test
	public void testUnionSelect() throws Exception {
		verifyQueryFile("/sql-union-ok.txt");
		verifyQueryFile("/sql-union-error.txt");
	}

	@Test
	public void testSimpleSelectWhere() throws Exception {
		verifyQueryFile("/sql-simple-select-where-ok.txt");
		verifyQueryFile("/sql-simple-select-where-error.txt");
	}
	
	@Test
	public void testHibernateSelectWhere() throws Exception {
		verifyQueryFile("/sql-simple-hibernate-select-ok.txt");
		verifyQueryFile("/sql-simple-hibernate-select-error.txt");
	}		


	
	@Test
	public void testStarSelectWhere() throws Exception {
		verifyQueryFile("/sql-simple-star-select-ok.txt");
	}		
	
	@Test
	public void testExampleSocial() throws Exception {
		verifyQueryFile("/sql-example-social-ok.txt");
		verifyQueryFile("/sql-example-social-error.txt");
	}	

	@Test
	public void testExampleDM() throws Exception {
		verifyQueryFile("/sql-example-dm-ok.txt");
		verifyQueryFile("/sql-example-dm-error.txt");
	}	
	
	@Test
	public void testSimpleSelectWhereIn() throws Exception {
		verifyQueryFile("/sql-simple-select-where-in-ok.txt");
	}
	
	public void verifyQueryFile(String file) throws Exception {
		String sql = cleanEntities(readSQL(file));
		Statement statement = CCJSqlParserUtil.parse(sql);
		
		if (statement instanceof Select) {
			try {
				verifySelectBody(((Select)statement).getSelectBody());
				if (file.indexOf("-error.")>0) {
					Assert.fail("sql should produce error\n" + sql);
				}
			}
			catch(NoTableNameAtColumnSQLException e) {
				if (file.indexOf("-ok.")>0) {
					Assert.fail(e.getMessage());
				}
			}
		}
		else {
			// Ignore
		}
	}
	
	protected void verifySelectBody(SelectBody selectBody) throws Exception {
		
		if (selectBody instanceof SetOperationList ) {
			SetOperationList setOperationList = (SetOperationList)selectBody;
			for(SelectBody subSelectBody : setOperationList.getSelects()) {
				verifySelectBody(subSelectBody);
			}
		}
		else if (selectBody instanceof PlainSelect) {

			PlainSelect plainSelect = (PlainSelect)selectBody;
			
			List<SelectItem> list = plainSelect.getSelectItems();

			for ( SelectItem selectItem : list) {
				
				if (selectItem instanceof SelectExpressionItem) {
					SelectExpressionItem selectExpressionItem = (SelectExpressionItem)selectItem;
					verifyExpression(selectExpressionItem.getExpression());	
				}
				else {
					System.out.println("selectItem :" + selectItem.getClass());
				}
			}
			
			FromItem fromItem = plainSelect.getFromItem();
			
			if (fromItem instanceof SubSelect) {
				SubSelect subSelect = (SubSelect)fromItem;
				verifySelectBody(subSelect.getSelectBody());
			}
			else if (fromItem instanceof Table) {
				Table table = (Table)fromItem;
			}
			else {
				System.out.println(" fromItem:"+ fromItem.getClass());
			}
			
			Expression expression = plainSelect.getWhere();
			
			verifyExpression(expression);	
		}
		else {
			System.out.println("selectBody:" +selectBody);
		}
	}
	
	protected void verifyExpression(Expression expression) throws Exception {
		if (expression==null) {
			return;
		}
		if (expression instanceof Parenthesis) {
			Parenthesis parenthesis = (Parenthesis)expression;
			
			verifyExpression(parenthesis.getExpression());
		}
		else if (expression instanceof BinaryExpression) {
			BinaryExpression binaryExpression = (BinaryExpression)expression;
			verifyExpression(binaryExpression.getLeftExpression());
			verifyExpression(binaryExpression.getRightExpression());
		}
		else if (expression instanceof Column) {
			Column column = (Column)expression;
			
			if (column.getTable().getName()==null) {
				throw new NoTableNameAtColumnSQLException("Column name does not have table name prefix " + column.getColumnName());
			}
		}
		else if (expression instanceof Function) {
			Function function = (Function)expression;
			ExpressionList expressionList = function.getParameters();
			for ( Expression functionExpression : expressionList.getExpressions()) {
				verifyExpression(functionExpression);
			}
		}
		else if (expression instanceof InExpression ) {
			InExpression inExpression = (InExpression)expression;
			verifyExpression(inExpression.getLeftExpression());
			ItemsList itemsList = inExpression.getRightItemsList();
			
			if ( itemsList instanceof SubSelect ) {
				SubSelect subSelect = (SubSelect) itemsList;
				verifySelectBody(subSelect.getSelectBody());
			}
			else if (itemsList instanceof ExpressionList) {
				ExpressionList expressionList = (ExpressionList)itemsList;
				for (Expression subExpression : expressionList.getExpressions()) {
					verifyExpression(subExpression);
				}
			}
			else {
				System.out.println(" itemsList:"+ itemsList.getClass());
				
			}
		}
		else if (expression instanceof JdbcParameter) {
			// Ignore
		}
		else {
			System.out.println(" expression:" + expression.getClass().getName());
		}
	}
	
	protected String readSQL(String file) throws IOException, URISyntaxException {

		List<String> list = Files.readAllLines(
				Paths.get(this.getClass().getResource(file).toURI()),
				Charset.defaultCharset());
		
		StringBundler sb = new StringBundler(list.size()*2);
		for (String line : list) {
			sb.append(line);
			sb.append("\n");
		}
		return sb.toString();	
	}
	
	protected String cleanEntities(String sql) {
		return sql.replaceAll("[\\{]|[\\}]", "");
	}
}
