/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hu.agnos.report.server.service.query.generator.sql;

import hu.agnos.report.server.service.query.generator.AbstractQueryGenerator;
import hu.agnos.report.server.util.CubeQueryLocator;
import hu.mi.agnos.report.entity.Hierarchy;
import hu.mi.agnos.report.entity.Level;
import hu.mi.agnos.report.entity.Indicator;
import hu.mi.agnos.report.entity.Report;

import java.util.ArrayList;


/**
 *
 * @author parisek
 */
public class SimpleSQLQueryGenerator implements AbstractQueryGenerator{
    
    /**
     *
     * @param cql
     * @param report
     * @return
     */
    @Override
     public String getQuery(CubeQueryLocator cql, Report report) {
        String cubeName = report.getName().split(":")[0];
        int dimSize = cql.getBaseLevelString().length;
        int[] baseLevels = new int[dimSize];
        String[] baseValues = new String[dimSize];
        for (int i = 0; i < dimSize; i++) {
            baseLevels[i] = cql.getBaseLevelString()[i].length;
            baseValues[i] = (baseLevels[i] == 0) ? null : cql.getBaseLevelString()[i][baseLevels[i] - 1];
        }
        int[] isInAxis = cql.getDrillVector();

        StringBuilder sql = new StringBuilder();
        StringBuilder sql_select = new StringBuilder();
        StringBuilder sql_where = new StringBuilder();
        StringBuilder sql_groupby = new StringBuilder();
        StringBuilder sql_orderby = new StringBuilder();

        for (int h = 0; h < report.getHierarchies().size(); h++) {
            
            Hierarchy hierarchy = report.getHierarchies().get(h);
            baseLevels[h] = Math.min(baseLevels[h], hierarchy.getLevels().size() - 1);

            int l = baseLevels[h] + isInAxis[h];
            if (l >= hierarchy.getLevels().size()) {
                l = hierarchy.getLevels().size() - 1;
            }
            Level level = hierarchy.getLevels().get(l);

            if (isInAxis[h] == 1) {
//                System.out.println("h==1");
                // ha az id és a known_id megeggyezik, akkor sincs baj az as miatt 
                sql_select.append(" \"").append(level.getIdColumnName()).append("\" as Z").append(h).append("O1,");
                sql_select.append(" \"").append(level.getCodeColumnName()).append("\" as Z").append(h).append("O2,");
                sql_select.append(" \"").append(level.getNameColumnName()).append("\" as Z").append(h).append("O3,");

                sql_groupby.append(" \"").append(level.getIdColumnName()).append("\",");
                sql_groupby.append(" \"").append(level.getCodeColumnName()).append("\",");
                sql_groupby.append(" \"").append(level.getNameColumnName()).append("\",");

                sql_orderby.append(" \"").append(level.getIdColumnName()).append("\" ASC,");
            }

            if (baseLevels[h] != 0) {
                sql_where.append(" \"").append(hierarchy.getLevels().get(baseLevels[h]).getIdColumnName()).append("\"='").append(baseValues[h]).append("',");
            }
        }

        for (Indicator indicator : report.getIndicators()) {
//            System.out.println("debug: " + indicator.getJSONFull());
            sql_select.append(" SUM(\"").append(indicator.getValue().getMeasureUniqueName()).append("\") AS \"").append(indicator.getValue().getMeasureUniqueName()).append("\",");
            sql_select.append(" SUM(\"").append(indicator.getDenominator().getMeasureUniqueName()).append("\") AS \"").append(indicator.getDenominator().getMeasureUniqueName()).append("\",");
        }

        if (sql_select.length() > 0) {
            sql_select.deleteCharAt(sql_select.length() - 1);
            sql.append("SELECT").append(sql_select);
            sql.append("\nFROM \"").append(cubeName).append("\"");
            if (sql_where.length() > 0) {
                sql_where.deleteCharAt(sql_where.length() - 1);
                String sql_where_string = sql_where.toString().replaceAll(",", " AND");
                sql.append("\nWHERE").append(sql_where_string);
            }
            if (sql_groupby.length() > 0) {
                sql_groupby.deleteCharAt(sql_groupby.length() - 1);
                sql.append("\nGROUP BY").append(checkDuplicate(sql_groupby.toString()));
            }
            if (sql_orderby.length() > 0) {
                sql_orderby.deleteCharAt(sql_orderby.length() - 1);
                sql.append("\nORDER BY").append(sql_orderby).append("");
            }
        }
        return sql.toString();
    }
     
       public String checkDuplicate (String subQuery){
        ArrayList<String> differentWords = new ArrayList<>();
        for(String word : subQuery.split(",")){
            if (! differentWords.contains(word.trim())){
                differentWords.add(word.trim());
            }
            
//            System.err.println(word.trim()+", "+differentWords.size());
//            System.err.println(word.trim()+", "+differentWords.get(0)+ " " + word.trim().equals(differentWords.get(0))+ " "+differentWords.contains(word.trim()));
            
        }
        String result = " ";
        for(String word : differentWords){
            result += word+", ";
        }
        
        return result.substring(0, result.length()-2);
    }

}
