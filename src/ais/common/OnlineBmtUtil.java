package ais.common;

import java.util.Locale;

import org.json.JSONObject;

import ais.database.model.Konfigurasi;
import ais.database.model.VirtualAccountBank;
import ais.database.model.sekolah.KanalPembayaran;
import ais.database.model.sekolah.Sekolah;

/**
 * Kontrak bersama kanal pembayaran Online BMT.
 *
 * <p>Online BMT berbeda dari payment-link seperti e-Smartlink. AIS tidak mengirim
 * permintaan pembuatan tagihan ke BMT. AIS menerbitkan satu nomor invoice lokal
 * pada {@link VirtualAccountBank}; kemudian server BMT memanggil endpoint
 * {@code /OnlineBmt} untuk INQUIRY, PAYMENT, atau CHECK_STATUS_PAYMENT. Karena itu
 * pembuatan invoice tidak boleh dianggap sebagai pembayaran dan tidak boleh
 * mengubah saldo, cicilan, maupun status mahasiswa.</p>
 *
 * <p>Aktivasi selalu fail-closed. Sakelar global dan sakelar tenant harus aktif.
 * Instalasi lama yang belum memiliki kolom sekolah/kanal akan dibaca sebagai
 * {@code false} oleh getter model. Kunci rahasia hanya dibaca oleh servlet dan
 * tidak pernah ditaruh pada request/response {@link VirtualAccountBank}.</p>
 */
public final class OnlineBmtUtil {

	public static final String BANK_NAME = "Online BMT";
	public static final String PARAM_KEY = "online_bmt";
	public static final String MARKER = "online_bmt:true";

	private OnlineBmtUtil() {
	}

	public static boolean isGlobalEnabled() {
		return Common.bolehKonfigurasi(Konfigurasi.ONLINE_BMT_AKTIF, Konfigurasi.TIDAK_AKTIF);
	}

	public static boolean isPerguruanTinggiEnabled(Long ptId) {
		if (!isGlobalEnabled() || ptId == null) {
			return false;
		}
		return Common.bolehKonfigurasi(Konfigurasi.ONLINE_BMT_AKTIF_PT_PREFIX + ptId,
				Konfigurasi.TIDAK_AKTIF);
	}

	public static boolean isSekolahEnabled(Sekolah sekolah, KanalPembayaran kanal) {
		if (!isGlobalEnabled() || sekolah == null
				|| !Boolean.TRUE.equals(sekolah.getAktfkanPembayaranViaOnlineBmt())) {
			return false;
		}
		return kanal == null || Boolean.TRUE.equals(kanal.getAktfkanPembayaranViaOnlineBmt());
	}

	/** Menyiapkan invoice lokal; method ini tidak melakukan posting finansial. */
	public static void prepareInvoice(VirtualAccountBank invoice) {
		if (invoice == null) {
			throw new IllegalArgumentException("Invoice Online BMT tidak boleh null");
		}
		String prefix = Common.getKonfigurasi(Konfigurasi.ONLINE_BMT_PREFIX_INVOICE, "BMT").getNilai();
		prefix = prefix == null ? "BMT" : prefix.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ENGLISH);
		if (prefix.length() == 0) {
			prefix = "BMT";
		}
		if (prefix.length() > 8) {
			prefix = prefix.substring(0, 8);
		}
		String kode = prefix + Common.getGeneratedBarCode(30 - prefix.length());
		invoice.setKode(kode);
		invoice.setBank(BANK_NAME);
		invoice.setChannel("ONLINE_BMT");
		invoice.setLink("");

		JSONObject audit = new JSONObject();
		audit.put("provider", "ONLINE_BMT");
		audit.put("NO_INVOICE", kode);
		audit.put("state", "WAITING_PAYMENT");
		invoice.setRequest(audit.toString());
	}

	public static double payableAmount(VirtualAccountBank invoice) {
		if (invoice == null) {
			return 0.0;
		}
		return (invoice.getTotal() == null ? 0.0 : invoice.getTotal().doubleValue())
				+ (invoice.getBiayaAdmin() == null ? 0.0 : invoice.getBiayaAdmin().doubleValue());
	}

	/** Menambahkan nama kanal pada daftar CSV tanpa duplikasi; OFF tidak mengubah konfigurasi lama. */
	public static String appendToConfiguredBanks(String configured, boolean enabled) {
		String value = configured == null ? "" : configured.trim();
		if (!enabled) return value;
		String[] banks = value.split(",");
		for (int i = 0; i < banks.length; i++) {
			if (BANK_NAME.equalsIgnoreCase(banks[i].trim())) return value;
		}
		return value.length() == 0 ? BANK_NAME : value + "," + BANK_NAME;
	}
}
