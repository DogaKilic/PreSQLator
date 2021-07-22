package antlr;

import content.TableBank;
import content.TableContent;

import java.util.ArrayList;
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
        ArrayList<String> currentResults = new ArrayList<>();
        String tableName = "";
        //String selectQuery = "";
        for(int i = 0; i < selectCores.stream().count(); i++){
            tableName = selectCores.get(i).table_or_subquery().get(0).getText();
            System.out.println(selectCores.get(i).result_column().get(0).getText());
            for(int k = 0; k < selectCores.get(i).result_column().stream().count(); k++) {
                currentResults.add(selectCores.get(i).result_column().get(k).getText());
            }
            TableBank.addPreparedSelectQuery(tableName, currentResults);
            currentResults.clear();
        }
    }
}
