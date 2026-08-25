package ais.action.master.repository;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import ais.common.Common;
import ais.common.ErrorAuditUtil;

/** Scheduler daemon untuk mengubah pencarian tersimpan menjadi notifikasi in-app. */
public final class RepositoryAlertScheduler {
    private static volatile ScheduledExecutorService scheduler;
    private RepositoryAlertScheduler(){}

    public static synchronized void mulai(){
        if(scheduler!=null)return;String enabled=Common.getKonfigurasi("repository_search_alerts","Aktif").getNilai();
        if(!"Aktif".equalsIgnoreCase(enabled)&&!"true".equalsIgnoreCase(enabled)){System.out.println("[Repository] Search alert dinonaktifkan.");return;}
        final int interval=positive(Common.getKonfigurasi("repository_search_alert_interval_minutes","30").getNilai(),30);
        ScheduledExecutorService service=Executors.newSingleThreadScheduledExecutor(factory("repository-search-alert"));
        service.scheduleWithFixedDelay(new Runnable(){public void run(){try{RepositoryAlertService.Summary summary=jalankanSekali();System.out.println("[Repository] Search alert selesai: "+summary);}catch(Throwable error){ErrorAuditUtil.record(error,"RepositoryAlertScheduler.run");}}},15,interval,TimeUnit.MINUTES);
        scheduler=service;System.out.println("[Repository] Search alert aktif tiap "+interval+" menit.");
    }
    public static synchronized void hentikan(){ScheduledExecutorService service=scheduler;scheduler=null;if(service!=null)service.shutdownNow();}
    public static RepositoryAlertService.Summary jalankanSekali(){return new RepositoryAlertService().process(500,20);}
    private static int positive(String value,int fallback){try{int number=Integer.parseInt(value==null?"":value.trim());return number>0?number:fallback;}catch(Exception e){return fallback;}}
    private static ThreadFactory factory(final String name){return new ThreadFactory(){private final AtomicInteger sequence=new AtomicInteger(1);public Thread newThread(Runnable runnable){Thread thread=new Thread(runnable,name+"-"+sequence.getAndIncrement());thread.setDaemon(true);return thread;}};}
}
