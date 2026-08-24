package ais.action.master.sosial.helper;
import java.math.BigDecimal; import org.json.JSONObject;
public final class ZakatCalculationResult { private final boolean reached; private final BigDecimal nisab,base,rate,amount; private final JSONObject breakdown;
 public ZakatCalculationResult(boolean r,BigDecimal n,BigDecimal b,BigDecimal rate,BigDecimal a,JSONObject j){this.reached=r;this.nisab=n;this.base=b;this.rate=rate;this.amount=a;this.breakdown=j;}
 public boolean isReached(){return reached;} public BigDecimal getNisab(){return nisab;} public BigDecimal getBase(){return base;} public BigDecimal getRate(){return rate;} public BigDecimal getAmount(){return amount;} public JSONObject getBreakdown(){return breakdown;}
 public JSONObject toJson(){JSONObject o=new JSONObject();try{o.put("reachedNisab",reached).put("nisabValue",nisab).put("zakatBase",base).put("rate",rate).put("zakatAmount",amount).put("breakdown",breakdown);}catch(Exception ignored){}return o;}
}
