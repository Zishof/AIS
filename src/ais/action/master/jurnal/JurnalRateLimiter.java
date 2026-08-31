package ais.action.master.jurnal;

import java.util.LinkedHashMap;
import java.util.Map;

/** Small bounded per-node fixed-window guard; reverse proxy limits remain additive. */
public final class JurnalRateLimiter {
    private static final int MAX_KEYS=10000;
    private static final LinkedHashMap<String,Bucket> BUCKETS=new LinkedHashMap<String,Bucket>(1024,0.75f,true);
    private JurnalRateLimiter(){}
    public static synchronized boolean allow(String namespace,String remote,int limit,long windowMillis){if(namespace==null||!namespace.matches("[a-zA-Z0-9_.-]{1,40}")||remote==null||remote.length()>100||limit<1||limit>10000||windowMillis<1000||windowMillis>3600000L)return false;long now=System.currentTimeMillis();String key=namespace+"|"+remote;Bucket b=BUCKETS.get(key);if(b==null||now-b.start>=windowMillis||now<b.start){b=new Bucket(now);BUCKETS.put(key,b);}if(BUCKETS.size()>MAX_KEYS){java.util.Iterator<Map.Entry<String,Bucket>>i=BUCKETS.entrySet().iterator();while(BUCKETS.size()>MAX_KEYS&&i.hasNext()){i.next();i.remove();}}if(b.count>=limit)return false;b.count++;return true;}
    static synchronized void clearForTest(){BUCKETS.clear();}
    /**
     * Tipe implementasi bersarang {@link Bucket} milik {@link JurnalRateLimiter}. Kelas ini memberi nama pada
     * state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link JurnalRateLimiter}.
     * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p> Tipe ini
     * merupakan detail implementasi privat; pemanggil luar harus memakai API kelas induk.
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code long start}, {@code int count}.
     * Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
     *
     * @see JurnalRateLimiter
     */
    private static final class Bucket{final long start;int count;Bucket(long start){this.start=start;}}
}
