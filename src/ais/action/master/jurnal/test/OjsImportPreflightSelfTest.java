package ais.action.master.jurnal.test;

import ais.action.master.jurnal.importer.OjsImportPreflightService;

public final class OjsImportPreflightSelfTest {
    private OjsImportPreflightSelfTest() {}
    public static void main(String[] args) throws Exception {
        String jdbc=System.getenv("AIS_JURNAL_OJS_FIXTURE_JDBC"),user=System.getenv("AIS_JURNAL_OJS_FIXTURE_USER"),password=System.getenv("AIS_JURNAL_OJS_FIXTURE_PASSWORD");
        if(jdbc==null||!jdbc.contains("ojs_jurnal_fixture_3505")||user==null||password==null)throw new IllegalStateException("Fixture environment tidak aman/lengkap.");
        OjsImportPreflightService.Config c=new OjsImportPreflightService.Config();c.jdbcUrl=jdbc;c.user=user;c.password=password;c.schema="public";
        OjsImportPreflightService.Result r=new OjsImportPreflightService().inspect(c);
        if(r.expectedTables!=134||r.foundTables!=134||r.foundFields!=905||!r.missing.isEmpty()||!"3.5.0-5".equals(r.version))throw new IllegalStateException("Preflight bukan 134/905 OJS 3.5.0-5.");
        System.out.println("OjsImportPreflightSelfTest OK version=3.5.0-5 tables=134 fields=905 read-only");
    }
}
