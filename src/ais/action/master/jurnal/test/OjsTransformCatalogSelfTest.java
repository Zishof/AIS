package ais.action.master.jurnal.test;

import ais.action.master.jurnal.importer.OjsSourceCatalog;
import ais.action.master.jurnal.importer.OjsTransformCatalog;

public final class OjsTransformCatalogSelfTest {
    private OjsTransformCatalogSelfTest() {}
    public static void main(String[] args) {
        check(OjsSourceCatalog.TABLES.size()==134,"134 tables");
        check(OjsTransformCatalog.count("MERGED")==69,"69 merged");
        check(OjsTransformCatalog.count("ALTER_EXISTING")==37,"37 alter");
        check(OjsTransformCatalog.count("NEW_MODEL")==11,"11 new model");
        check(OjsTransformCatalog.count("DERIVED")==4,"4 derived");
        check(OjsTransformCatalog.count("NOT_APPLICABLE_WITH_RATIONALE")==13,"13 N/A");
        for(String table:OjsSourceCatalog.TABLES){OjsTransformCatalog.Outcome o=OjsTransformCatalog.outcome(table,"fixture_field");check(o.targetType!=null&&o.targetField!=null,"outcome "+table);}
        boolean denied=false;try{OjsTransformCatalog.outcome("unknown_table","x");}catch(IllegalArgumentException e){denied=true;}check(denied,"unknown deny");
        System.out.println("OjsTransformCatalogSelfTest OK tables=134 dispositions=134 fail-closed");
    }
    private static void check(boolean v,String m){if(!v)throw new IllegalStateException(m);}
}
