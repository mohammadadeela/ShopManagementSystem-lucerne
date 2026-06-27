package com.lucerne.ui;

import java.util.List;

public record ModuleDefinition(
        String key, String title, String description, String permission,
        String selectSql, String idColumn, List<String> searchColumns,
        String dateColumn, String statusColumn, String amountColumn,
        String branchColumn, String customerColumn, String defaultOrder,
        List<String> statuses
) {
    public static Builder builder(String key, String title) { return new Builder(key, title); }
    public static final class Builder {
        private final String key, title;
        private String description="Manage records, filters, exports and operational details.";
        private String permission="VIEW_DASHBOARD";
        private String selectSql, idColumn="ID", dateColumn, statusColumn, amountColumn, branchColumn, customerColumn, defaultOrder="1 DESC";
        private List<String> searchColumns=List.of(), statuses=List.of();
        private Builder(String key, String title) { this.key=key; this.title=title; }
        public Builder description(String v){description=v; return this;}
        public Builder permission(String v){permission=v; return this;}
        public Builder sql(String v){selectSql=v; return this;}
        public Builder id(String v){idColumn=v; return this;}
        public Builder search(String... v){searchColumns=List.of(v); return this;}
        public Builder date(String v){dateColumn=v; return this;}
        public Builder status(String v, String... s){statusColumn=v; statuses=List.of(s); return this;}
        public Builder amount(String v){amountColumn=v; return this;}
        public Builder branch(String v){branchColumn=v; return this;}
        public Builder customer(String v){customerColumn=v; return this;}
        public Builder order(String v){defaultOrder=v; return this;}
        public ModuleDefinition build(){return new ModuleDefinition(key,title,description,permission,selectSql,idColumn,searchColumns,dateColumn,statusColumn,amountColumn,branchColumn,customerColumn,defaultOrder,statuses);}
    }
}
