package ais.action.master.jurnal.test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.hibernate.Session;
import org.hibernate.Transaction;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;

/** Read-only, secret-free physical schema fingerprint for isolated SIT/UAT clones. */
public final class JurnalSchemaFingerprintSelfTest {
    private JurnalSchemaFingerprintSelfTest() {}
    public static void main(String[] args) throws Exception {
        requireClone("AIS_JURNAL_DB_NAME","ais");requireClone("AIS_JURNAL_STREAMING_DB_NAME","streaming_ais");
        System.setProperty("javax.persistence.validation.mode","none");
        Session main=null,stream=null;Transaction streamTx=null;
        try{
            main=HibernateUtil.openSession();Connection mc=main.connection();
            String mainDb=scalarText(mc,"select current_database()");
            long mainTables=scalar(mc,"select count(*) from information_schema.tables where table_type='BASE TABLE' and table_schema not in ('pg_catalog','information_schema')");
            long journalTables=scalar(mc,"select count(*) from information_schema.tables where table_schema='penelitiandanpengabdian' and table_name in ('template_email_jurnal','langganan_jurnal','undangan_peran_jurnal','peserta_diskusi_jurnal','penugasan_tahap_jurnal','penugasan_reviewer_jurnal','agregat_penggunaan_jurnal','rentang_ip_langganan_jurnal','import_sumber_ojs','import_job_ojs','import_checkpoint_ojs','import_mapping_ojs')");
            stream=StreamingHibernateUtil.getInstance().currentSession();streamTx=stream.beginTransaction();Connection sc=stream.connection();
            String streamDb=scalarText(sc,"select current_database()");
            long streamTables=scalar(sc,"select count(*) from information_schema.tables where table_type='BASE TABLE' and table_schema not in ('pg_catalog','information_schema')");
            long journalBlobTables=scalar(sc,"select count(*) from information_schema.tables where table_schema='public' and table_name='lampiran_jurnal'");
            long columns=scalar(sc,"select count(*) from information_schema.columns where table_schema='public' and table_name='lampiran_jurnal'");
            long uniqueConstraints=scalar(sc,"select count(*) from information_schema.table_constraints where table_schema='public' and table_name='lampiran_jurnal' and constraint_type='UNIQUE'");
            long rows=scalar(sc,"select count(*) from public.lampiran_jurnal");streamTx.commit();
            if(journalTables!=12||journalBlobTables!=1||columns!=19||uniqueConstraints<2)throw new IllegalStateException("Kontrak schema jurnal tidak lengkap.");
            System.out.println("JurnalSchemaFingerprintSelfTest OK mainDb="+mainDb+" mainTables="+mainTables+" journalMain=12 streamDb="+streamDb+" streamTables="+streamTables+" journalStreaming=1 lampiranColumns="+columns+" uniqueConstraints="+uniqueConstraints+" rows="+rows);
        }finally{if(streamTx!=null&&streamTx.isActive())streamTx.rollback();if(stream!=null)StreamingHibernateUtil.getInstance().closeSession();HibernateUtil.closeSessionQuietly(main);}
        System.exit(0);
    }
    private static long scalar(Connection c,String sql)throws Exception{PreparedStatement p=c.prepareStatement(sql);ResultSet r=null;try{r=p.executeQuery();if(!r.next())throw new IllegalStateException("Fingerprint query kosong.");return r.getLong(1);}finally{if(r!=null)r.close();p.close();}}
    private static String scalarText(Connection c,String sql)throws Exception{PreparedStatement p=c.prepareStatement(sql);ResultSet r=null;try{r=p.executeQuery();if(!r.next())throw new IllegalStateException("Fingerprint query kosong.");return r.getString(1);}finally{if(r!=null)r.close();p.close();}}
    private static void requireClone(String key,String baseline){String value=System.getenv(key);if(value==null||value.trim().length()==0||baseline.equalsIgnoreCase(value.trim()))throw new IllegalStateException("Fingerprint wajib clone: "+key);}
}
