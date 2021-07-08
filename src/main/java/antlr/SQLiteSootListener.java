package antlr;

import content.TableBank;
import content.TableContent;

import java.util.LinkedList;
import java.util.List;

public class SQLiteSootListener extends SQLiteParserBaseListener {

    @Override
    public void enterCreate_table_stmt(SQLiteParser.Create_table_stmtContext ctx) {
        String tableName = ctx.table_name().getText();
        if (!TableBank.hasTable(tableName)) {
            TableContent newTable = new TableContent(tableName);
            TableBank.addTable(newTable);
        }

        LinkedList<String> columnList = new LinkedList<>();
        for (SQLiteParser.Column_defContext context : ctx.column_def()) {
            String content = context.column_name().getText() + ":" + context.type_name().getText();
            columnList.add(content);
        }
        TableBank.setColumns(tableName, columnList);
    }

    @Override
    public void enterInsert_stmt(SQLiteParser.Insert_stmtContext ctx) {
        String tableName = ctx.table_name().getText();
        if (TableBank.hasTable(tableName)) {
            TableBank.addPreparedInsertStatement(tableName);
        }
    }

    @Override
    public void enterSelect_stmt(SQLiteParser.Select_stmtContext ctx) {
        List<SQLiteParser.Select_coreContext> selectCores = ctx.select_core();
        String tableName = "";
        String selectQuery = "";
        if (selectCores.stream().count() == 1) {
            tableName = selectCores.get(0).table_or_subquery().get(0).table_name().getText();
            selectQuery = selectCores.get(0).result_column().get(0).getText();
        }
        TableBank.addPreparedSelectQuery(tableName, selectQuery);
    }
}
