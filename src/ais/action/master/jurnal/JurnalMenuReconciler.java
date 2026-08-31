package ais.action.master.jurnal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;

import ais.common.JurnalAksesKatalog;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Menu;

/**
 * Reconciles the reserved journal menu hierarchy without touching unrelated
 * menu rows. Inspection is read-only; mutation is separately opt-in and is
 * blocked when any id/root/child/url/label collision exists.
 */
public final class JurnalMenuReconciler {
    public static final long PARENT_ID = 2000460500L;
    public static final long CHILD_ID_BASE = 2000000000L;

    /**
     * Tipe implementasi bersarang {@link Desired} milik {@link JurnalMenuReconciler}. Kelas ini memberi nama pada
     * state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link JurnalMenuReconciler}.
     * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code long id}, {@code long root}, {@code
     * long child}, {@code String label}, {@code String url}, {@code int order}. Aturan bisnis bersama tetap berada
     * pada kelas induk atau service yang dipanggilnya.</p>
     *
     * @see JurnalMenuReconciler
     */
    public static final class Desired {
        public final long id;
        public final long root;
        public final long child;
        public final String label;
        public final String url;
        public final int order;

        private Desired(long id,long root,long child,String label,String url,int order){
            this.id=id;this.root=root;this.child=child;this.label=label;this.url=url;this.order=order;
        }
    }

    /**
     * Pembawa data/helper lokal milik {@link JurnalMenuReconciler} untuk result. Tipe ini mengelompokkan nilai
     * antara agar perhitungan atau rendering tidak memakai array/map tanpa kontrak yang jelas.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link JurnalMenuReconciler}.
     * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code List inserts}, {@code List updates},
     * {@code List unchanged}, {@code List conflicts}, {@code boolean applied}; operasi lokal: {@code safe}().
     * Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
     * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
     * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
     * tambahkan perilaku lintas domain pada service bersama.</p>
     *
     * @see JurnalMenuReconciler
     */
    public static final class Result {
        public final List<String> inserts=new ArrayList<String>();
        public final List<String> updates=new ArrayList<String>();
        public final List<String> unchanged=new ArrayList<String>();
        public final List<String> conflicts=new ArrayList<String>();
        public boolean applied;
        public boolean safe(){return conflicts.isEmpty();}
    }

    public List<Desired> desired(){
        List<Desired> rows=new ArrayList<Desired>();
        rows.add(new Desired(PARENT_ID,46L,4605L,"Jurnal","/jurnal/admin",0));
        int order=1;
        for(JurnalAksesKatalog.Entri entry:JurnalAksesKatalog.DAFTAR){
            rows.add(new Desired(CHILD_ID_BASE+entry.child,4605L,entry.child,entry.label,"/jurnal/admin/"+entry.kunci,order++));
        }
        return Collections.unmodifiableList(rows);
    }

    @SuppressWarnings("unchecked")
    public Result inspect(){
        Session session=HibernateUtil.currentSession();
        Query query=session.createQuery("from Menu");
        return inspectRows((List<Menu>)query.list());
    }

    /** Visible for deterministic collision tests without a database mutation. */
    public Result inspectRows(List<Menu> existing){
        Result result=new Result();
        Map<Long,Menu> byId=new LinkedHashMap<Long,Menu>();
        Map<Long,Desired> desiredById=new LinkedHashMap<Long,Desired>();
        for(Desired wanted:desired())desiredById.put(Long.valueOf(wanted.id),wanted);
        for(Menu row:existing)if(row!=null&&row.getId()!=null)byId.put(row.getId(),row);
        for(Desired wanted:desired()){
            Menu managed=byId.get(Long.valueOf(wanted.id));
            if(managed!=null&&(managed.getRoot().longValue()!=wanted.root||managed.getChild().longValue()!=wanted.child)){
                result.conflicts.add("reserved id "+wanted.id+" belongs to "+managed.getRoot()+"/"+managed.getChild());
            }
            for(Menu row:existing){
                if(row==null||row.getId()==null||row.getId().longValue()==wanted.id)continue;
                Desired managedWanted=desiredById.get(row.getId());
                boolean managedOwned=managedWanted!=null&&row.getRoot().longValue()==managedWanted.root
                    &&row.getChild().longValue()==managedWanted.child;
                if(row.getRoot().longValue()==wanted.root&&row.getChild().longValue()==wanted.child)
                    result.conflicts.add("root/child "+wanted.root+"/"+wanted.child+" already uses id "+row.getId());
                /* A legacy journal row with the canonical reserved PK and tuple is
                   owned by this reconciler. Its old label/URL may safely move in
                   the same transaction; unrelated rows still fail closed. */
                if(!managedOwned&&equal(row.getUrl(),wanted.url))result.conflicts.add("url "+wanted.url+" already uses id "+row.getId());
                if(!managedOwned&&equal(row.getLabel(),wanted.label))result.conflicts.add("label "+wanted.label+" already uses id "+row.getId());
            }
            if(managed==null)result.inserts.add(String.valueOf(wanted.id));
            else if(matches(managed,wanted))result.unchanged.add(String.valueOf(wanted.id));
            else result.updates.add(String.valueOf(wanted.id));
        }
        return result;
    }

    public Result reconcile(String actorId){
        requireMutationOptIn();
        Session session=HibernateUtil.currentSession();
        Transaction tx=session.getTransaction();
        if(tx.isActive())throw new IllegalStateException("Reconciler menu harus memiliki transaksi sendiri.");
        Result before=inspect();
        if(!before.safe())throw new IllegalStateException("Rekonsiliasi menu diblokir; collision: "+before.conflicts);
        try{
            tx.begin();
            for(Desired wanted:desired()){
                Menu row=(Menu)session.get(Menu.class,Long.valueOf(wanted.id));
                if(row==null){row=new Menu();row.setId(Long.valueOf(wanted.id));session.save(row);}
                row.setRoot(Long.valueOf(wanted.root));row.setChild(Long.valueOf(wanted.child));
                row.setLabel(wanted.label);row.setUrl(wanted.url);row.setNomorUrut(Integer.valueOf(wanted.order));
                row.setAktif(Boolean.TRUE);row.setTampilDiPt(Boolean.TRUE);row.setTampilDiSekolah(Boolean.TRUE);
                row.setBukaHalamanBaru(Boolean.FALSE);row.setOleh("JurnalMenuReconciler");row.setOlehId(actorId);
                row.setTanggal_dirubah(new Date());session.saveOrUpdate(row);
            }
            session.flush();tx.commit();
            Result after=inspect();
            if(!after.safe()||after.unchanged.size()!=desired().size())
                throw new IllegalStateException("Verifikasi pasca-reconcile menu gagal.");
            before.applied=true;return before;
        }catch(RuntimeException e){if(tx.isActive())tx.rollback();throw e;}
    }

    private static boolean matches(Menu row,Desired wanted){
        return row.getRoot().longValue()==wanted.root&&row.getChild().longValue()==wanted.child
            &&equal(row.getLabel(),wanted.label)&&equal(row.getUrl(),wanted.url)
            &&Boolean.TRUE.equals(row.getAktif())&&row.getNomorUrut().intValue()==wanted.order;
    }
    private static boolean equal(String left,String right){
        return normalize(left).equals(normalize(right));
    }
    private static String normalize(String value){
        return value==null?"":value.trim().toLowerCase(Locale.ENGLISH);
    }
    private static void requireMutationOptIn(){
        if(!"true".equalsIgnoreCase(System.getenv("AIS_JURNAL_MENU_RECONCILE")))
            throw new IllegalStateException("Set AIS_JURNAL_MENU_RECONCILE=true untuk mengizinkan mutation menu.");
        String db=normalize(System.getenv("AIS_JURNAL_DB_NAME"));
        boolean disposable=db.endsWith("_sit")||db.endsWith("_uat")||db.endsWith("_demo")||db.endsWith("_fixture")||db.endsWith("_test");
        if(!disposable&&!"true".equalsIgnoreCase(System.getenv("AIS_JURNAL_ALLOW_PRODUCTION_MENU_RECONCILE")))
            throw new IllegalStateException("Target menu non-disposable memerlukan opt-in production terpisah.");
    }
}
