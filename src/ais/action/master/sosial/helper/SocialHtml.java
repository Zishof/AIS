package ais.action.master.sosial.helper;
import java.math.BigDecimal; import java.text.NumberFormat; import java.util.Locale;
/**
 * Utilitas format tampilan untuk modul sosial (halaman donasi/zakat publik): escaping HTML aman
 * dari XSS, format nilai uang Rupiah, dan perhitungan persentase progres (mis. progres donasi
 * terhadap target) dibatasi ke rentang 0-100.
 */
public final class SocialHtml {
	private SocialHtml(){}

	/** Meng-escape {@code value} (dikonversi ke string dulu) agar aman disisipkan ke markup HTML — meng-escape {@code &<>"'}; mengembalikan string kosong bila {@code null}. */
	public static String e(Object value){if(value==null)return "";String s=String.valueOf(value);return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");}

	/** Memformat {@code value} sebagai mata uang Rupiah (locale {@code id-ID}, tanpa desimal); {@code null} diperlakukan sebagai nol. */
	public static String money(BigDecimal value){if(value==null)value=BigDecimal.ZERO;NumberFormat f=NumberFormat.getCurrencyInstance(new Locale("id","ID"));f.setMaximumFractionDigits(0);return f.format(value);}

	/** Menghitung persentase {@code value} terhadap {@code target} (pembulatan ke bawah, tanpa desimal), dibatasi ke rentang 0-100; mengembalikan 0 bila {@code value}/{@code target} null atau {@code target} tidak positif. */
	public static int progress(BigDecimal value,BigDecimal target){if(value==null||target==null||target.signum()<=0)return 0;int p=value.multiply(new BigDecimal("100")).divide(target,0,BigDecimal.ROUND_DOWN).intValue();return Math.max(0,Math.min(100,p));}
}
