package ais.action.master.sosial.helper;

import java.math.BigDecimal;
import org.hibernate.Session;

/**
 * Satu-satunya definisi saldo finansial dashboard Sosial.
 * Nominal kontribusi platform dan fee gateway tidak pernah dianggap dana sosial.
 */
public final class SocialFinancialInvariantService {
    public static final String POSTED_REFUND_TYPES="'REFUND','REVERSAL'";

    public Snapshot snapshot(Session session,String tenantKey){
        if(session==null)throw new IllegalArgumentException("Session wajib tersedia.");
        if(tenantKey==null||tenantKey.trim().isEmpty())throw new IllegalArgumentException("Tenant wajib tersedia.");
        BigDecimal settled=sum(session,"select sum(p.requestAmount) from PembayaranDonasi p where p.tenantKey=:tenant and p.paymentStatus='PAID'",tenantKey);
        BigDecimal returned=sum(session,"select sum(e.amount) from SocialCorrectionEvent e where e.tenantKey=:tenant and e.status='POSTED' and e.correctionType in ("+POSTED_REFUND_TYPES+")",tenantKey);
        BigDecimal allocated=sum(session,"select sum(a.amount) from AlokasiDonasi a where a.tenantKey=:tenant and a.status='POSTED'",tenantKey);
        BigDecimal distributed=sum(session,"select sum(d.amount) from DetailPenyaluranDonasi d where d.tenantKey=:tenant and d.status='POSTED'",tenantKey);
        return calculate(settled,returned,allocated,distributed);
    }

    public static Snapshot calculate(BigDecimal settled,BigDecimal returned,BigDecimal allocated,BigDecimal distributed){
        settled=money(settled);returned=money(returned);allocated=money(allocated);distributed=money(distributed);
        if(settled.signum()<0||returned.signum()<0||allocated.signum()<0||distributed.signum()<0)throw new IllegalArgumentException("Komponen saldo tidak boleh negatif.");
        BigDecimal netSettled=settled.subtract(returned);
        BigDecimal unallocated=netSettled.subtract(allocated);
        BigDecimal allocationAvailable=allocated.subtract(distributed);
        return new Snapshot(settled,returned,allocated,distributed,netSettled,unallocated,allocationAvailable,
                unallocated.signum()<0||allocationAvailable.signum()<0||returned.compareTo(settled)>0);
    }

    public static BigDecimal totalCharged(BigDecimal donation,BigDecimal contribution,BigDecimal fee){
        donation=money(donation);contribution=money(contribution);fee=money(fee);
        if(donation.signum()<=0||contribution.signum()<0||fee.signum()<0)throw new IllegalArgumentException("Komponen tagihan tidak valid.");
        return donation.add(contribution).add(fee).setScale(2);
    }

    private BigDecimal sum(Session s,String hql,String tenant){Object value=s.createQuery(hql).setString("tenant",tenant).uniqueResult();return value instanceof BigDecimal?money((BigDecimal)value):BigDecimal.ZERO.setScale(2);}
    private static BigDecimal money(BigDecimal value){return (value==null?BigDecimal.ZERO:value).setScale(2,BigDecimal.ROUND_HALF_UP);}

    /**
     * Pembawa data/helper lokal milik {@link SocialFinancialInvariantService} untuk snapshot. Tipe ini
     * mengelompokkan nilai antara agar perhitungan atau rendering tidak memakai array/map tanpa kontrak yang
     * jelas.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
     * SocialFinancialInvariantService}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
     * digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code BigDecimal settled}, {@code
     * BigDecimal returned}, {@code BigDecimal allocated}, {@code BigDecimal distributed}, {@code BigDecimal
     * netSettled}, {@code BigDecimal unallocated}, {@code BigDecimal allocationAvailable}, {@code boolean
     * exception}; operasi lokal: {@code getSettled()}, {@code getReturned()}, {@code getAllocated()}, {@code
     * getDistributed()}, {@code getNetSettled()}, {@code getUnallocated()}, {@code getAllocationAvailable()},
     * {@code hasException}(). Aturan bisnis bersama tetap berada pada kelas induk atau service yang
     * dipanggilnya.</p>
     * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
     * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
     * tambahkan perilaku lintas domain pada service bersama.</p>
     *
     * @see SocialFinancialInvariantService
     */
    public static final class Snapshot {
        private final BigDecimal settled,returned,allocated,distributed,netSettled,unallocated,allocationAvailable;
        private final boolean exception;
        private Snapshot(BigDecimal s,BigDecimal r,BigDecimal a,BigDecimal d,BigDecimal n,BigDecimal u,BigDecimal v,boolean e){settled=s;returned=r;allocated=a;distributed=d;netSettled=n;unallocated=u;allocationAvailable=v;exception=e;}
        public BigDecimal getSettled(){return settled;} public BigDecimal getReturned(){return returned;} public BigDecimal getAllocated(){return allocated;} public BigDecimal getDistributed(){return distributed;}
        public BigDecimal getNetSettled(){return netSettled;} public BigDecimal getUnallocated(){return unallocated;} public BigDecimal getAllocationAvailable(){return allocationAvailable;} public boolean hasException(){return exception;}
    }
}
