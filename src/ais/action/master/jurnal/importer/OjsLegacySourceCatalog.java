package ais.action.master.jurnal.importer;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Minimal OJS 2.x catalog used by the six-model AIS legacy integration. */
public final class OjsLegacySourceCatalog {
    private OjsLegacySourceCatalog(){}
    public static final List<String> TABLES=Collections.unmodifiableList(Arrays.asList(
            "journals","journal_settings","articles","article_settings","issues","published_articles","users"));
    public static final Set<String> TABLE_SET=Collections.unmodifiableSet(new LinkedHashSet<String>(TABLES));
    static OjsTransformCatalog.Outcome outcome(String table,String field){
        if(!TABLE_SET.contains(table)||field==null||field.trim().length()==0)throw new IllegalArgumentException("Tabel/field legacy OJS tidak dikenal.");
        String target=("journals".equals(table)||"journal_settings".equals(table))?"JurnalPenelitian/RepoCollection":("issues".equals(table)?"RepoItem(JOURNAL_ISSUE)":("users".equals(table)?"RepoAuthorAuthority":"RepoItem/RepoItemMetadata"));
        return new OjsTransformCatalog.Outcome("ALTER_EXISTING",target,"legacy."+table+"."+field.trim().toLowerCase());
    }
}
