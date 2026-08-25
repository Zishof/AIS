package ais.action.master.sosial.helper;

import java.math.BigDecimal; import java.math.RoundingMode; import java.util.Date; import org.hibernate.Session; import org.hibernate.criterion.Order; import org.hibernate.criterion.Restrictions; import org.json.JSONObject;
import ais.database.model.sosial.*;

public final class ZakatCalculatorService {
    private static final BigDecimal ZERO=BigDecimal.ZERO, TWELVE=new BigDecimal("12");
    public PerhitunganZakat calculateAndStore(Session session,SocialRequestContext context,Long jenisZakatId,JSONObject input){
        if(!SocialFeatureFlags.enabled(session,context,SocialFeatureFlags.CALCULATOR))throw new IllegalStateException("Kalkulator zakat belum diaktifkan.");
        JenisZakat type=(JenisZakat)session.get(JenisZakat.class,jenisZakatId);if(type==null||!context.getTenantKey().equals(type.getTenantKey())||!type.getActive())throw new IllegalArgumentException("Jenis zakat tidak tersedia.");
        Date now=new Date();KebijakanPerhitunganZakat policy=(KebijakanPerhitunganZakat)session.createCriteria(KebijakanPerhitunganZakat.class).add(Restrictions.eq("tenantKey",context.getTenantKey())).add(Restrictions.eq("jenisZakat",type)).add(Restrictions.eq("status","APPROVED")).add(Restrictions.le("effectiveFrom",now)).add(Restrictions.or(Restrictions.isNull("effectiveUntil"),Restrictions.ge("effectiveUntil",now))).addOrder(Order.desc("effectiveFrom")).setMaxResults(1).uniqueResult();
        if(policy==null)throw new IllegalStateException("Kebijakan zakat aktif belum tersedia.");
        ZakatCalculationResult result=calculate(policy,input);PerhitunganZakat snapshot=new PerhitunganZakat();snapshot.setTenantKey(context.getTenantKey());snapshot.setJenisZakat(type);snapshot.setPolicy(policy);snapshot.setPolicyVersion(policy.getVersion());snapshot.setInputJson(input.toString());snapshot.setResultJson(result.toJson().toString());snapshot.setNisabValueSnapshot(result.getNisab());snapshot.setRateSnapshot(result.getRate());snapshot.setAmount(result.getAmount());snapshot.setReachedNisab(result.isReached());snapshot.setConverted(Boolean.FALSE);snapshot.setCurrency("IDR");snapshot.setRequestId(context.getRequestId());snapshot.setStatus("CALCULATED");snapshot.setCreatedBy(context.getActorId());if(context.isAuthenticated())snapshot.setDonorIdentity(new SocialIdentityService().resolveOrCreate(session,context));session.save(snapshot);return snapshot;
    }
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
