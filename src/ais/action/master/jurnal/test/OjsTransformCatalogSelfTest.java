package ais.action.master.jurnal.test;

import ais.action.master.jurnal.importer.OjsSourceCatalog;
import ais.action.master.jurnal.importer.OjsTransformCatalog;

/**
 * Harness uji manual (bukan JUnit, dijalankan lewat {@code main}) yang memverifikasi konsistensi
 * katalog transformasi impor OJS: {@link OjsSourceCatalog#TABLES} harus berisi 134 tabel sumber,
 * dan setiap tabel tersebut harus terklasifikasi tepat satu kali di {@link OjsTransformCatalog}
 * dengan total disposisi 69 {@code MERGED} + 37 {@code ALTER_EXISTING} + 11 {@code NEW_MODEL} + 4
 * {@code DERIVED} + 13 {@code NOT_APPLICABLE_WITH_RATIONALE} = 134 tabel (habis terklasifikasi,
 * tidak ada yang terlewat). Untuk setiap tabel sumber juga diverifikasi bahwa
 * {@link OjsTransformCatalog#outcome} mengembalikan {@code targetType}/{@code targetField} yang
 * terisi. Terakhir memastikan katalog bersifat <i>fail-closed</i>: memanggil {@code outcome} untuk
 * tabel yang tidak dikenal harus melempar {@link IllegalArgumentException}, bukan diam-diam
 * mengembalikan hasil kosong/null.
 */
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
