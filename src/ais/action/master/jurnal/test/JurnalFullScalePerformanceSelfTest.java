package ais.action.master.jurnal.test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/** Full-scale read/load/soak gate over the dedicated jurnal_perf schema. */
public final class JurnalFullScalePerformanceSelfTest {
    private static final int THREADS=8;
    private static final double OLTP_P95_LIMIT_MS=250.0d;
    private static final double LOAD_P95_LIMIT_MS=500.0d;
    private static final double ANALYTIC_P95_LIMIT_MS=3000.0d;
    private static final double THROUGHPUT_MIN=50.0d;
    private static final long HEAP_LIMIT=1610612736L;
    private static final String[] OLTP={
        "select workflow_status,count(*) from jurnal_perf.article where journal_id=? and aktif=true group by workflow_status",
        "select id,title,published_at from jurnal_perf.article where journal_id=? and workflow_status='PUBLISHED' and aktif=true order by published_at desc nulls last,id desc limit 25",
        "select id,bundle_name,mime_type,ukuran_byte from jurnal_perf.file_metadata where item_id=? and aktif=true order by bundle_name,file_version desc",
        "select event_type,count(*) from jurnal_perf.usage_event where item_id=? and occurred_at>=? group by event_type",
        "select role_key,count(*) from jurnal_perf.perf_user where journal_id=? and aktif=true group by role_key"
    };
    private static final String ANALYTIC="select event_type,country_code,count(*) from jurnal_perf.usage_event where occurred_at>=? and user_agent_class<>'BOT' group by event_type,country_code";

    public static void main(String[]args)throws Exception{
        String db=env("AIS_JURNAL_DB_NAME"),host=env("AIS_JURNAL_DB_HOST"),port=env("AIS_JURNAL_DB_PORT");
        final String user=env("AIS_JURNAL_DB_USER"),password=env("AIS_JURNAL_DB_PASSWORD");
        if(!"ais_jurnal_sit".equals(db))throw new IllegalStateException("Full performance test hanya boleh pada ais_jurnal_sit.");
        int seconds=args.length>0?Integer.parseInt(args[0]):300;if(seconds<60||seconds>3600)throw new IllegalArgumentException("Soak harus 60..3600 detik.");
        Class.forName("org.postgresql.Driver");final String url="jdbc:postgresql://"+host+":"+port+"/"+db+"?ApplicationName=JurnalFullScalePerformanceSelfTest";
        Connection c=open(url,user,password);try{
            checkDatabase(c);verifyCounts(c);
            long coldStart=System.nanoTime();runOltp(c,0,1);double coldMs=ms(System.nanoTime()-coldStart);
            List<Long>warm=new ArrayList<Long>();for(int round=0;round<40;round++)for(int q=0;q<OLTP.length;q++){long n=System.nanoTime();runOltp(c,q,round);warm.add(Long.valueOf(System.nanoTime()-n));}
            List<Long>analytics=new ArrayList<Long>();for(int i=0;i<10;i++){long n=System.nanoTime();runAnalytic(c);analytics.add(Long.valueOf(System.nanoTime()-n));}
            final AtomicBoolean stop=new AtomicBoolean(false);final AtomicLong operations=new AtomicLong(),errors=new AtomicLong(),heapPeak=new AtomicLong();
            final List<Long>loadSamples=Collections.synchronizedList(new ArrayList<Long>());final CountDownLatch ready=new CountDownLatch(THREADS),done=new CountDownLatch(THREADS);
            for(int i=0;i<THREADS;i++){final int worker=i;new Thread(new Runnable(){public void run(){Connection x=null;try{x=open(url,user,password);Random random=new Random(91919L+worker);ready.countDown();while(!stop.get()){int q=random.nextInt(OLTP.length),key=1+random.nextInt(q==4?100:100000);long n=System.nanoTime();try{runOltp(x,q,key);loadSamples.add(Long.valueOf(System.nanoTime()-n));operations.incrementAndGet();}catch(Exception e){errors.incrementAndGet();}}}catch(Exception e){errors.incrementAndGet();ready.countDown();}finally{close(x);done.countDown();}}},"jurnal-perf-"+i).start();}
            ready.await();long loadStarted=System.nanoTime(),deadline=System.currentTimeMillis()+seconds*1000L;while(System.currentTimeMillis()<deadline){long used=Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();raise(heapPeak,used);Thread.sleep(1000L);}stop.set(true);done.await();double elapsedSeconds=(System.nanoTime()-loadStarted)/1000000000.0d;
            double warmP50=pct(warm,.50),warmP95=pct(warm,.95),analyticP95=pct(analytics,.95),loadP95=pct(loadSamples,.95),throughput=operations.get()/elapsedSeconds;
            boolean pass=warmP95<=OLTP_P95_LIMIT_MS&&analyticP95<=ANALYTIC_P95_LIMIT_MS&&loadP95<=LOAD_P95_LIMIT_MS&&throughput>=THROUGHPUT_MIN&&errors.get()==0L&&heapPeak.get()<=HEAP_LIMIT;
            System.out.println("JurnalFullScalePerformanceSelfTest "+(pass?"OK":"FAIL")+" dataset=100000/1000000/10000000/10000 threads="+THREADS+" soakSeconds="+seconds
                +" coldProcessMs="+round(coldMs)+" warmSamples="+warm.size()+" warmP50Ms="+round(warmP50)+" warmP95Ms="+round(warmP95)
                +" analyticSamples="+analytics.size()+" analyticP95Ms="+round(analyticP95)+" loadOperations="+operations.get()+" loadP95Ms="+round(loadP95)
                +" throughputOpsPerSec="+round(throughput)+" errors="+errors.get()+" heapPeakBytes="+heapPeak.get()
                +" thresholds=warmP95<=250,analyticP95<=3000,loadP95<=500,throughput>=50,errors=0,heap<=1610612736");
            if(!pass)throw new IllegalStateException("Threshold performance lokal tidak terpenuhi.");
        }finally{close(c);}
    }
    private static Connection open(String url,String user,String password)throws Exception{Connection c=DriverManager.getConnection(url,user,password);c.setReadOnly(true);return c;}
    private static void checkDatabase(Connection c)throws Exception{Statement s=c.createStatement();ResultSet r=s.executeQuery("select current_database(),to_regclass('jurnal_perf.article'),to_regclass('jurnal_perf.file_metadata'),to_regclass('jurnal_perf.usage_event'),to_regclass('jurnal_perf.perf_user')");try{if(!r.next()||!"ais_jurnal_sit".equals(r.getString(1))||r.getString(2)==null||r.getString(3)==null||r.getString(4)==null||r.getString(5)==null)throw new IllegalStateException("Schema performance tidak lengkap.");}finally{r.close();s.close();}}
    private static void verifyCounts(Connection c)throws Exception{long[]expected={100000L,1000000L,10000000L,10000L};String[]tables={"article","file_metadata","usage_event","perf_user"};for(int i=0;i<tables.length;i++){Statement s=c.createStatement();ResultSet r=s.executeQuery("select count(*) from jurnal_perf."+tables[i]);try{if(!r.next()||r.getLong(1)!=expected[i])throw new IllegalStateException("Cardinality salah: "+tables[i]);}finally{r.close();s.close();}}}
    private static void runOltp(Connection c,int q,int key)throws Exception{PreparedStatement p=c.prepareStatement(OLTP[q]);try{if(q==0||q==1||q==4)p.setLong(1,1L+(key%100));else p.setLong(1,1L+(key%100000));if(q==3)p.setTimestamp(2,Timestamp.valueOf("2026-07-24 00:00:00"));consume(p);}finally{p.close();}}
    private static void runAnalytic(Connection c)throws Exception{PreparedStatement p=c.prepareStatement(ANALYTIC);try{p.setTimestamp(1,Timestamp.valueOf("2026-07-24 00:00:00"));consume(p);}finally{p.close();}}
    private static void consume(PreparedStatement p)throws Exception{ResultSet r=p.executeQuery();try{while(r.next()){} }finally{r.close();}}
    private static double pct(List<Long>values,double percentile){if(values.isEmpty())return Double.POSITIVE_INFINITY;List<Long>x=new ArrayList<Long>(values);Collections.sort(x);return ms(x.get((int)Math.ceil(x.size()*percentile)-1).longValue());}
    private static double ms(long nanos){return nanos/1000000.0d;}private static double round(double x){return Math.round(x*100.0d)/100.0d;}
    private static void raise(AtomicLong target,long value){long old;do{old=target.get();if(value<=old)return;}while(!target.compareAndSet(old,value));}
    private static String env(String key){String value=System.getenv(key);if(value==null||value.trim().length()==0)throw new IllegalStateException("Environment wajib: "+key);return value.trim();}
    private static void close(Connection c){if(c!=null)try{c.close();}catch(Exception ignored){}}
}
