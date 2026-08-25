package ais.action.master.sosial.test;

import java.math.BigDecimal;
import org.json.JSONObject;
import ais.action.master.sosial.helper.ZakatCalculationResult;
import ais.action.master.sosial.helper.ZakatCalculatorService;
import ais.database.model.sosial.KebijakanPerhitunganZakat;

public final class ZakatCalculatorGoldenSelfTest {
    private static int failures;
    public static void main(String[] args)throws Exception{
        ZakatCalculatorService service=new ZakatCalculatorService();
        KebijakanPerhitunganZakat gold=policy("GOLD","0.025","85","1000000","HALF_UP",2);ZakatCalculationResult at=service.calculate(gold,new JSONObject().put("grams","85"));ok(at.isReached(),"gold at nisab");eq(at.getAmount(),"2125000.00","gold amount");ok(!service.calculate(gold,new JSONObject().put("grams","84.999")).isReached(),"gold below nisab");
        KebijakanPerhitunganZakat income=policy("INCOME_ANNUAL","0.025","85","1000000","HALF_UP",2);ZakatCalculationResult annual=service.calculate(income,new JSONObject().put("grossIncome","100000000").put("deductions","10000000"));eq(annual.getAmount(),"2250000.00","annual income");
        KebijakanPerhitunganZakat monthly=policy("INCOME_MONTHLY","0.025","85","1000000","HALF_UP",2);monthly.setNisabBasis("ANNUAL");eq(service.calculate(monthly,new JSONObject().put("grossIncome","8000000")).getAmount(),"200000.00","monthly income");
        KebijakanPerhitunganZakat fitrah=policy("FITRAH","1","1",null,"HALF_UP",2);fitrah.setFitrahCash(new BigDecimal("45000"));eq(service.calculate(fitrah,new JSONObject().put("people",4)).getAmount(),"180000.00","fitrah");deniedZeroRate(service,gold);
        if(failures>0)throw new IllegalStateException(failures+" zakat golden failures.");System.out.println("ZakatCalculatorGoldenSelfTest OK");
    }
    private static KebijakanPerhitunganZakat policy(String key,String rate,String quantity,String price,String rounding,int scale){KebijakanPerhitunganZakat p=new KebijakanPerhitunganZakat();p.setFormulaKey(key);p.setRate(new BigDecimal(rate));p.setNisabQuantity(new BigDecimal(quantity));if(price!=null)p.setReferencePrice(new BigDecimal(price));p.setRoundingMode(rounding);p.setResultScale(Integer.valueOf(scale));return p;}
    private static void deniedZeroRate(ZakatCalculatorService s,KebijakanPerhitunganZakat p)throws Exception{p.setRate(BigDecimal.ZERO);try{s.calculate(p,new JSONObject().put("grams","85"));ok(false,"zero rate rejected");}catch(IllegalStateException expected){}}
    private static void eq(BigDecimal a,String b,String label){ok(a.compareTo(new BigDecimal(b))==0,label+": "+a);}private static void ok(boolean yes,String label){if(!yes){System.out.println("GAGAL: "+label);failures++;}}
}
