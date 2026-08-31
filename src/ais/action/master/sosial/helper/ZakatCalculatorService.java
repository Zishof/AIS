package ais.action.master.sosial.helper;

import java.math.BigDecimal; import java.math.RoundingMode; import java.util.Date; import org.hibernate.Session; import org.hibernate.criterion.Order; import org.hibernate.criterion.Restrictions; import org.json.JSONObject;
import ais.database.model.sosial.*;

/**
 * Layanan modul sosial untuk menghitung zakat (Fitrah, Maal berbasis emas/GOLD, dan penghasilan
 * bulanan/tahunan INCOME_MONTHLY/INCOME_ANNUAL) berdasarkan {@link KebijakanPerhitunganZakat}
 * (kebijakan/parameter perhitungan) yang berlaku aktif untuk satu {@link JenisZakat} pada tenant
 * tertentu, lalu menyimpan hasilnya sebagai snapshot immutable {@link PerhitunganZakat}.
 *
 * <p>
 * {@link #calculateAndStore} adalah alur lengkap: memvalidasi fitur kalkulator aktif
 * ({@link SocialFeatureFlags#CALCULATOR}), memvalidasi jenis zakat milik tenant yang sama dan
 * aktif, mencari kebijakan berstatus {@code APPROVED} yang rentang keberlakuannya
 * ({@code effectiveFrom}/{@code effectiveUntil}) mencakup waktu sekarang (terbaru bila lebih dari
 * satu cocok), menjalankan {@link #calculate}, lalu menyimpan input mentah, hasil, dan snapshot
 * nisab/rate/policy version sebagai satu baris {@link PerhitunganZakat} baru (audit trail lengkap
 * meski kebijakan berubah di kemudian hari). {@link #calculate} sendiri adalah fungsi murni (tanpa
 * efek samping) yang bisa dipakai terpisah untuk simulasi tanpa menyimpan hasil.
 * </p>
 *
 * <p>
 * Rumus per jenis: FITRAH = jumlah jiwa x nominal fitrah per jiwa (rate dipaksa 1, nisab 0 — zakat
 * fitrah tidak mengenal ambang nisab); GOLD = nisab dan basis dihitung dari gram x harga referensi
 * emas; selain itu (penghasilan) = (penghasilan kotor - pengurang) dibandingkan nisab (dibagi 12
 * bila kebijakan {@code INCOME_MONTHLY} dengan basis nisab {@code ANNUAL}). Zakat hanya dikenakan
 * ({@code reached=true}) bila basis &gt;= nisab.
 * </p>
 */
public final class ZakatCalculatorService {
    private static final BigDecimal ZERO=BigDecimal.ZERO, TWELVE=new BigDecimal("12");
    /**
     * Alur lengkap perhitungan zakat: validasi fitur/jenis zakat/kebijakan aktif, menjalankan
     * {@link #calculate}, lalu menyimpan snapshot hasilnya sebagai {@link PerhitunganZakat} baru
     * (termasuk mengaitkan identitas donatur via {@link SocialIdentityService} bila context
     * terautentikasi).
     *
     * @param jenisZakatId id {@link JenisZakat} yang dihitung
     * @param input        parameter perhitungan mentah dari klien (mis. {@code people}/{@code grams}/{@code grossIncome})
     * @return baris {@link PerhitunganZakat} yang baru tersimpan
     * @throws IllegalStateException  bila fitur kalkulator nonaktif atau belum ada kebijakan aktif yang berlaku
     * @throws IllegalArgumentException bila jenis zakat tidak ditemukan/bukan milik tenant/tidak aktif
     */
    public PerhitunganZakat calculateAndStore(Session session,SocialRequestContext context,Long jenisZakatId,JSONObject input){
        if(!SocialFeatureFlags.enabled(session,context,SocialFeatureFlags.CALCULATOR))throw new IllegalStateException("Kalkulator zakat belum diaktifkan.");
        JenisZakat type=(JenisZakat)session.get(JenisZakat.class,jenisZakatId);if(type==null||!context.getTenantKey().equals(type.getTenantKey())||!type.getActive())throw new IllegalArgumentException("Jenis zakat tidak tersedia.");
        Date now=new Date();KebijakanPerhitunganZakat policy=(KebijakanPerhitunganZakat)session.createCriteria(KebijakanPerhitunganZakat.class).add(Restrictions.eq("tenantKey",context.getTenantKey())).add(Restrictions.eq("jenisZakat",type)).add(Restrictions.eq("status","APPROVED")).add(Restrictions.le("effectiveFrom",now)).add(Restrictions.or(Restrictions.isNull("effectiveUntil"),Restrictions.ge("effectiveUntil",now))).addOrder(Order.desc("effectiveFrom")).setMaxResults(1).uniqueResult();
        if(policy==null)throw new IllegalStateException("Kebijakan zakat aktif belum tersedia.");
        ZakatCalculationResult result=calculate(policy,input);PerhitunganZakat snapshot=new PerhitunganZakat();snapshot.setTenantKey(context.getTenantKey());snapshot.setJenisZakat(type);snapshot.setPolicy(policy);snapshot.setPolicyVersion(policy.getVersion());snapshot.setInputJson(input.toString());snapshot.setResultJson(result.toJson().toString());snapshot.setNisabValueSnapshot(result.getNisab());snapshot.setRateSnapshot(result.getRate());snapshot.setAmount(result.getAmount());snapshot.setReachedNisab(result.isReached());snapshot.setConverted(Boolean.FALSE);snapshot.setCurrency("IDR");snapshot.setRequestId(context.getRequestId());snapshot.setStatus("CALCULATED");snapshot.setCreatedBy(context.getActorId());if(context.isAuthenticated())snapshot.setDonorIdentity(new SocialIdentityService().resolveOrCreate(session,context));session.save(snapshot);return snapshot;
    }
    /**
     * Fungsi murni (tanpa efek samping/query database) yang menghitung hasil zakat dari kebijakan
     * {@code p} dan input {@code in}, bercabang berdasarkan {@code p.getFormulaKey()}
     * ("FITRAH"/"GOLD"/lainnya = berbasis penghasilan). Lihat javadoc kelas untuk rumus tiap jenis.
     *
     * @throws IllegalArgumentException bila input tidak valid (mis. jumlah jiwa bukan bilangan bulat positif, pengurang negatif)
     * @throws IllegalStateException    bila parameter kebijakan yang wajib positif (rate, nominal fitrah, harga referensi, kuantitas nisab) tidak valid
     */
    public ZakatCalculationResult calculate(KebijakanPerhitunganZakat p,JSONObject in){
        String key=p.getFormulaKey();BigDecimal rate=positive(p.getRate(),"Kadar zakat");int scale=p.getResultScale();RoundingMode rounding=rounding(p.getRoundingMode());BigDecimal base,nisab;
        JSONObject breakdown=new JSONObject();
        if("FITRAH".equals(key)){BigDecimal people=value(in,"people");if(people.scale()>0||people.compareTo(BigDecimal.ONE)<0)throw new IllegalArgumentException("Jumlah jiwa tidak valid.");base=positive(p.getFitrahCash(),"Nominal fitrah").multiply(people);nisab=ZERO;rate=BigDecimal.ONE;return new ZakatCalculationResult(true,nisab,base,rate,base.setScale(scale,rounding),put(breakdown,"people",people,"cashPerPerson",p.getFitrahCash()));}
        if("GOLD".equals(key)){BigDecimal grams=value(in,"grams");BigDecimal price=positive(p.getReferencePrice(),"Harga emas");base=grams.multiply(price);nisab=positive(p.getNisabQuantity(),"Nisab").multiply(price);put(breakdown,"grams",grams,"referencePrice",price);}
        else {BigDecimal gross=valueOptional(in,"grossIncome",valueOptional(in,"amount",ZERO));BigDecimal deductions=valueOptional(in,"deductions",ZERO);if(deductions.signum()<0)throw new IllegalArgumentException("Pengurang tidak valid.");base=gross.subtract(deductions);if(base.signum()<0)base=ZERO;nisab=nisabValue(p);if("INCOME_MONTHLY".equals(key)&&"ANNUAL".equalsIgnoreCase(p.getNisabBasis()))nisab=nisab.divide(TWELVE,scale,rounding);put(breakdown,"gross",gross,"deductions",deductions);}
        boolean reached=base.compareTo(nisab)>=0;BigDecimal amount=reached?base.multiply(rate).setScale(scale,rounding):ZERO.setScale(scale,rounding);return new ZakatCalculationResult(reached,nisab.setScale(scale,rounding),base.setScale(scale,rounding),rate,amount,breakdown);
    }
    private BigDecimal nisabValue(KebijakanPerhitunganZakat p){BigDecimal q=positive(p.getNisabQuantity(),"Nisab");return p.getReferencePrice()==null?q:q.multiply(positive(p.getReferencePrice(),"Harga referensi"));}
    private BigDecimal value(JSONObject o,String k){BigDecimal v=valueOptional(o,k,null);if(v==null||v.signum()<0)throw new IllegalArgumentException(k+" wajib berupa angka non-negatif.");if(v.compareTo(new BigDecimal("1000000000000000"))>0)throw new IllegalArgumentException(k+" melampaui batas.");return v;}
    private BigDecimal valueOptional(JSONObject o,String k,BigDecimal d){try{if(!o.has(k)||o.isNull(k))return d;return new BigDecimal(String.valueOf(o.get(k)).replace(",",""));}catch(Exception e){throw new IllegalArgumentException(k+" tidak valid.");}}
    private BigDecimal positive(BigDecimal v,String n){if(v==null||v.signum()<=0)throw new IllegalStateException(n+" harus lebih besar dari nol.");return v;}
    private RoundingMode rounding(String v){try{return v==null?RoundingMode.HALF_UP:RoundingMode.valueOf(v);}catch(Exception e){return RoundingMode.HALF_UP;}}
    private JSONObject put(JSONObject o,String k1,Object v1,String k2,Object v2){try{o.put(k1,v1).put(k2,v2);}catch(Exception ignored){}return o;}
}
