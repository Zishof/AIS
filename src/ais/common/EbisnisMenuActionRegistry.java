package ais.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Registry kompatibilitas nama menu/aksi eBisnis.
 *
 * <p>Kunci kanonik sengaja tetap memakai kunci yang telah tersimpan di
 * {@code Tbmrole.ebisnisMenu} dan dipakai {@code TbmroleAction}. Istilah proses bisnis baru
 * (PR, PO, penerimaan vendor, tagihan, pembayaran, dan barang dalam proses) hanya menjadi alias.
 * Dengan demikian pembaruan navigasi tidak memutus hak akses role lama dan tidak membuat dua
 * kewenangan berbeda untuk satu fungsi yang sama.</p>
 */
public final class EbisnisMenuActionRegistry {

	private static final Map<String, String> ALIAS_KE_KANONIK = new LinkedHashMap<String, String>();
	private static final Map<String, Set<String>> KANONIK_KE_ALIAS = new LinkedHashMap<String, Set<String>>();
	private static final Map<String, String> ALIAS_AKSI_KE_KANONIK = new LinkedHashMap<String, String>();
	private static final Map<String, Set<String>> AKSI_KANONIK_KE_ALIAS = new LinkedHashMap<String, Set<String>>();

	static {
		daftar("pengadaan_pr", new String[] { "permintaan_pembelian", "purchase_requisition", "pr" });
		daftar("pengadaan_po", new String[] { "pemesanan_pembelian", "purchase_order", "po" });
		daftar("pengadaan_bast", new String[] { "penerimaan_barang", "penerimaan_vendor", "vendor_bast", "goods_receipt", "bast" });
		daftar("pengadaan_tagihan", new String[] { "terima_tagihan_vendor", "tagihan_vendor", "vendor_invoice" });
		daftar("pengadaan_dpc", new String[] { "pembayaran_vendor", "daftar_pengajuan_transfer", "proses_transfer_vendor", "dpc" });
		daftar("pengadaan_bdp", new String[] { "barang_dalam_proses", "work_in_process", "bdp" });
		daftar("pengadaan_pajak", new String[] { "bayar_pajak_pengadaan", "withholding_tax" });
		daftar("pengadaan_sinkron", new String[] { "sinkron_pengadaan", "rekonsiliasi_pengadaan" });
		daftar("kulakan", new String[] { "pembelian_stok_langsung", "pengadaan_toko_langsung" });
		daftar("mutasiantaroutlet", new String[] { "mutasi_antar_outlet", "transfer_stok_outlet" });
		daftar("stokopname", new String[] { "stok_opname", "stock_opname" });
		daftar("returpenjualan", new String[] { "retur_penjualan", "sales_return" });
		daftar("riwayatpenjualan", new String[] { "riwayat_penjualan", "sales_history" });
		daftar("riwayatsinkronisasi", new String[] { "riwayat_sinkronisasi", "sync_history" });
		daftar("laporantransaksi", new String[] { "laporan_transaksi", "transaction_report" });
		daftar("laporankeuangan", new String[] { "laporan_keuangan", "financial_report" });
		daftar("produksi", new String[] { "manufacturing", "produksi_outlet" });

		// Alias hanya menyatukan istilah yang benar-benar ekuivalen. Aksi proses bisnis
		// tetap berdiri sendiri dan tidak pernah dipetakan ke CRUD lain.
		daftarAksi("view", new String[] { "lihat", "read" });
		daftarAksi("create", new String[] { "tambah", "insert", "new" });
		daftarAksi("update", new String[] { "ubah", "edit" });
		daftarAksi("edit_draft", new String[] { "ubah_draft", "edit_draf" });
		daftarAksi("delete", new String[] { "hapus", "remove" });
		daftarAksi("submit", new String[] { "ajukan", "kirim_persetujuan" });
		daftarAksi("approve", new String[] { "setujui", "approval" });
		daftarAksi("reject", new String[] { "tolak", "rejection" });
		daftarAksi("cancel", new String[] { "batalkan", "void" });
		daftarAksi("post", new String[] { "posting" });
		daftarAksi("reverse", new String[] { "pembalikan", "reversal" });
		daftarAksi("export", new String[] { "ekspor", "download" });
		daftarAksi("view_cost", new String[] { "lihat_biaya", "lihat_hpp" });
		daftarAksi("view_all_location", new String[] { "lihat_semua_lokasi", "view_all_locations" });

		validasiTidakBentrok();
	}

	private EbisnisMenuActionRegistry() {
	}

	private static void daftar(String kanonik, String[] alias) {
		String kunciKanonik = normalisasi(kanonik);
		Set<String> semuaAlias = new LinkedHashSet<String>();
		simpanAlias(ALIAS_KE_KANONIK, kunciKanonik, kunciKanonik, "menu");
		if (alias != null) {
			for (int i = 0; i < alias.length; i++) {
				String nilai = normalisasi(alias[i]);
				if (nilai.length() > 0 && !kunciKanonik.equals(nilai)) {
					simpanAlias(ALIAS_KE_KANONIK, nilai, kunciKanonik, "menu");
					semuaAlias.add(nilai);
				}
			}
		}
		KANONIK_KE_ALIAS.put(kunciKanonik, Collections.unmodifiableSet(semuaAlias));
	}

	private static void daftarAksi(String kanonik, String[] alias) {
		String aksiKanonik = normalisasi(kanonik);
		Set<String> semuaAlias = new LinkedHashSet<String>();
		simpanAlias(ALIAS_AKSI_KE_KANONIK, aksiKanonik, aksiKanonik, "aksi");
		if (alias != null) {
			for (int i = 0; i < alias.length; i++) {
				String nilai = normalisasi(alias[i]);
				if (nilai.length() > 0 && !aksiKanonik.equals(nilai)) {
					simpanAlias(ALIAS_AKSI_KE_KANONIK, nilai, aksiKanonik, "aksi");
					semuaAlias.add(nilai);
				}
			}
		}
		AKSI_KANONIK_KE_ALIAS.put(aksiKanonik, Collections.unmodifiableSet(semuaAlias));
	}

	private static void simpanAlias(Map<String, String> indeks, String alias, String kanonik, String jenis) {
		String sebelumnya = indeks.get(alias);
		if (sebelumnya != null && !sebelumnya.equals(kanonik)) {
			throw new IllegalStateException("Alias " + jenis + " '" + alias
					+ "' bentrok antara '" + sebelumnya + "' dan '" + kanonik + "'.");
		}
		indeks.put(alias, kanonik);
	}

	/** Fail-fast bila registry hasil refactor mengandung alias kosong atau pemetaan tidak konsisten. */
	public static void validasiTidakBentrok() {
		validasiIndeks(ALIAS_KE_KANONIK, KANONIK_KE_ALIAS, "menu");
		validasiIndeks(ALIAS_AKSI_KE_KANONIK, AKSI_KANONIK_KE_ALIAS, "aksi");
	}

	private static void validasiIndeks(Map<String, String> indeks,
			Map<String, Set<String>> kebalikan, String jenis) {
		for (Map.Entry<String, String> entri : indeks.entrySet()) {
			if (entri.getKey() == null || entri.getKey().length() == 0
					|| entri.getValue() == null || entri.getValue().length() == 0) {
				throw new IllegalStateException("Registry " + jenis + " berisi kunci kosong.");
			}
			if (!entri.getKey().equals(entri.getValue())) {
				Set<String> alias = kebalikan.get(entri.getValue());
				if (alias == null || !alias.contains(entri.getKey())) {
					throw new IllegalStateException("Registry " + jenis + " tidak konsisten untuk alias '"
							+ entri.getKey() + "'.");
				}
			}
		}
	}

	/** Normalisasi aman untuk nama dari JSON, route, maupun TbmroleAction. */
	public static String normalisasi(String nilai) {
		if (nilai == null) {
			return "";
		}
		String sumber = nilai.trim().toLowerCase();
		StringBuilder hasil = new StringBuilder(sumber.length());
		boolean garisBawah = false;
		for (int i = 0; i < sumber.length(); i++) {
			char c = sumber.charAt(i);
			if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
				hasil.append(c);
				garisBawah = false;
			} else if (!garisBawah && hasil.length() > 0) {
				hasil.append('_');
				garisBawah = true;
			}
		}
		while (hasil.length() > 0 && hasil.charAt(hasil.length() - 1) == '_') {
			hasil.deleteCharAt(hasil.length() - 1);
		}
		return hasil.toString();
	}

	public static String kanonik(String kunci) {
		String normal = normalisasi(kunci);
		String hasil = ALIAS_KE_KANONIK.get(normal);
		return hasil == null ? normal : hasil;
	}

	/** Kandidat lookup berurutan: kanonik lebih dahulu, kemudian seluruh alias lama. */
	public static List<String> kandidat(String kunci) {
		String kanonik = kanonik(kunci);
		List<String> hasil = new ArrayList<String>();
		if (kanonik.length() == 0) {
			return hasil;
		}
		hasil.add(kanonik);
		Set<String> alias = KANONIK_KE_ALIAS.get(kanonik);
		if (alias != null) {
			hasil.addAll(alias);
		}
		return hasil;
	}

	public static String aksiKanonik(String aksi) {
		String normal = normalisasi(aksi);
		String hasil = ALIAS_AKSI_KE_KANONIK.get(normal);
		return hasil == null ? normal : hasil;
	}

	public static boolean aksiTerdaftar(String aksi) {
		return ALIAS_AKSI_KE_KANONIK.containsKey(normalisasi(aksi));
	}

	public static boolean menuTerdaftar(String kunci) {
		return ALIAS_KE_KANONIK.containsKey(normalisasi(kunci));
	}

	/** Kandidat lookup aksi berurutan: aksi kanonik lalu alias historisnya. */
	public static List<String> kandidatAksi(String aksi) {
		String kanonik = aksiKanonik(aksi);
		List<String> hasil = new ArrayList<String>();
		if (kanonik.length() == 0) {
			return hasil;
		}
		hasil.add(kanonik);
		Set<String> alias = AKSI_KANONIK_KE_ALIAS.get(kanonik);
		if (alias != null) {
			hasil.addAll(alias);
		}
		return hasil;
	}

	public static boolean memiliki(Map<?, ?> sumber, String kunci) {
		if (sumber == null) {
			return false;
		}
		List<String> kandidat = kandidat(kunci);
		for (int i = 0; i < kandidat.size(); i++) {
			if (sumber.containsKey(kandidat.get(i))) {
				return true;
			}
		}
		return false;
	}
}
