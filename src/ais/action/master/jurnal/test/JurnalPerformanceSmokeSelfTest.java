package ais.action.master.jurnal.test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;

import org.hibernate.Session;

import ais.database.hibernate.HibernateUtil;

/**
 * Repeatable read-only performance characterization on the SIT demo dataset.
 * It records measurements but deliberately has no pass threshold until the
 * product/SRE owners approve one.
 */
public final class JurnalPerformanceSmokeSelfTest {
    private JurnalPerformanceSmokeSelfTest(){}
    public static void main(String[]args)throws Exception{
        if(!"ais_jurnal_sit".equals(System.getenv("AIS_JURNAL_DB_NAME")))
            throw new IllegalStateException("Performance smoke hanya boleh memakai ais_jurnal_sit.");
        System.setProperty("javax.persistence.validation.mode","none");
        String source=args.length>0?args[0]:"AIS_JOURNAL_DEMO:demo-500x100";
        int loops=args.length>1?Integer.parseInt(args[1]):60;
        if(loops<20||loops>500)throw new IllegalArgumentException("Loop harus 20..500.");
        try{
            Session session=HibernateUtil.currentSession();Connection c=session.connection();
            long rows=scalar(c,"select count(*) from public.repo_item where source_class=? and aktif=true",source);
            if(rows<50000L)throw new IllegalStateException("Dataset performance kurang dari 50.000 artikel: "+rows);
            String[]sql={
                "select workflow_status,count(*) from public.repo_item where source_class=? and aktif=true group by workflow_status",
                "select id,title,workflow_status,published_at from public.repo_item where source_class=? and aktif=true and workflow_status='PUBLISHED' order by published_at desc nulls last,id desc limit 25",
                "select id,title from public.repo_item where source_class=? and aktif=true and lower(title) like '%article 99%' order by id desc limit 25"
            };
            for(int i=0;i<3;i++)execute(c,sql[i],source);
            long[] samples=new long[loops*sql.length];int at=0;long started=System.nanoTime();
            for(int loop=0;loop<loops;loop++)for(int q=0;q<sql.length;q++){
                long one=System.nanoTime();execute(c,sql[q],source);samples[at++]=System.nanoTime()-one;
            }
            long elapsed=System.nanoTime()-started;Arrays.sort(samples);
            double p50=millis(samples[(int)Math.ceil(samples.length*.50)-1]);
            double p95=millis(samples[(int)Math.ceil(samples.length*.95)-1]);
            double throughput=samples.length/(elapsed/1000000000.0d);
            long heap=Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();
            System.out.println("JurnalPerformanceSmokeSelfTest OK characterization-only rows="+rows+" samples="+samples.length
                +" queriesPerLoop=3 p50Ms="+round(p50)+" p95Ms="+round(p95)+" throughputOpsPerSec="+round(throughput)
                +" heapUsedBytes="+heap+" threshold=UNAPPROVED");
        }finally{HibernateUtil.closeSession();}
        System.exit(0);
    }
    private static long scalar(Connection c,String sql,String source)throws Exception{PreparedStatement p=c.prepareStatement(sql);try{p.setString(1,source);ResultSet r=p.executeQuery();try{return r.next()?r.getLong(1):0L;}finally{r.close();}}finally{p.close();}}
    private static void execute(Connection c,String sql,String source)throws Exception{PreparedStatement p=c.prepareStatement(sql);try{p.setString(1,source);ResultSet r=p.executeQuery();try{while(r.next()){} }finally{r.close();}}finally{p.close();}}
    private static double millis(long nanos){return nanos/1000000.0d;}
    private static double round(double value){return Math.round(value*100.0d)/100.0d;}
}
