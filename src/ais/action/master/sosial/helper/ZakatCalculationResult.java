package ais.action.master.sosial.helper;
import java.math.BigDecimal; import org.json.JSONObject;
/**
 * Objek hasil (immutable) satu perhitungan zakat pada modul sosial: menyimpan apakah nisab
 * tercapai, nilai nisab yang dipakai sebagai ambang, dasar perhitungan zakat, tarif (rate), nominal
 * zakat terhutang, serta rincian ({@code breakdown}) langkah perhitungan dalam bentuk JSON bebas
 * untuk keperluan audit/tampilan. Seluruh field diisi sekali lewat konstruktor dan hanya dapat
 * dibaca lewat getter; kelas ini tidak melakukan perhitungan sendiri, hanya membungkus hasil yang
 * sudah dihitung pemanggil.
 */
public final class ZakatCalculationResult { private final boolean reached; private final BigDecimal nisab,base,rate,amount; private final JSONObject breakdown;
	/**
	 * Membungkus hasil perhitungan zakat yang sudah jadi.
	 *
	 * @param r     apakah dasar perhitungan mencapai/melampaui nisab
	 * @param n     nilai nisab yang dipakai
	 * @param b     dasar perhitungan zakat (harta/penghasilan setelah pengurang, sesuai jenis zakat)
	 * @param rate  tarif zakat yang diterapkan
	 * @param a     nominal zakat terhutang hasil akhir
	 * @param j     rincian langkah perhitungan dalam bentuk JSON, boleh {@code null}
	 */
	public ZakatCalculationResult(boolean r,BigDecimal n,BigDecimal b,BigDecimal rate,BigDecimal a,JSONObject j){this.reached=r;this.nisab=n;this.base=b;this.rate=rate;this.amount=a;this.breakdown=j;}
 public boolean isReached(){return reached;} public BigDecimal getNisab(){return nisab;} public BigDecimal getBase(){return base;} public BigDecimal getRate(){return rate;} public BigDecimal getAmount(){return amount;} public JSONObject getBreakdown(){return breakdown;}
	/** Menyusun ulang seluruh hasil perhitungan ini menjadi satu {@link JSONObject} siap kirim ke klien (kunci: {@code reachedNisab}, {@code nisabValue}, {@code zakatBase}, {@code rate}, {@code zakatAmount}, {@code breakdown}). Kegagalan penyusunan JSON diabaikan secara diam-diam sehingga hasil bisa berupa objek JSON parsial. */
	public JSONObject toJson(){JSONObject o=new JSONObject();try{o.put("reachedNisab",reached).put("nisabValue",nisab).put("zakatBase",base).put("rate",rate).put("zakatAmount",amount).put("breakdown",breakdown);}catch(Exception ignored){}return o;}
}
