package ais.action.servlet.api;

import org.json.JSONObject;

import ais.database.model.Tbmuser;

/**
 * <h3>Dispatcher aksi {@code apotik_*} -- varian "POS Apotik" (FASE A).</h3>
 *
 * <p>Dipanggil dari {@code ApiEBisnis.prosesAksiTambahan} SETELAH dispatcher {@code si_}
 * (Inventory &amp; Sales) menolak aksinya -- pola dan kontrak return SAMA: {@code true} = sudah
 * ditangani (termasuk ditangani-dengan-error), {@code false} = bukan milik dispatcher ini.
 * Gerbang menu per-aksi sudah dijalankan LEBIH DULU oleh {@code PosApi.bolehAksesActionKantin}
 * (kunci {@code apotik_*} default FALSE -- fail-closed); di sini tinggal rute + normalisasi.</p>
 */
public final class ApotikApiDispatcher {

	private ApotikApiDispatcher() {
	}

	/**
	 * Kunci menu Apotik yang punya gerbang CRUD sungguhan di peladen. Dikirim ke klien
	 * supaya tombol Simpan pada tiap formulir dapat dipadamkan sebelum ditekan.
	 */
	private static final String[] KUNCI_HAK = {
		"apotik_kasir", "apotik_resep", "apotik_racikan", "apotik_pengadaan",
		"apotik_stok_opname", "apotik_retur", "apotik_formularium"
	};

	/**
	 * Hak per menu Apotik: {@code hak: {apotik_retur: {create, update, delete}, ...}}.
	 *
	 * <p>Bentuknya bersarang per menu -- berbeda dari modul lain yang satu layarnya satu
	 * menu -- karena SATU layar Persediaan memuat tiga formulir dengan tiga kunci
	 * berbeda. Mengirim satu peta datar akan memaksa klien menebak kunci mana yang
	 * berlaku untuk formulir mana.</p>
	 *
	 * <p>Ini bukan gerbang: gerbang sebenarnya tetap pemeriksaan di tiap metode helper.</p>
	 */
	private static JSONObject hakAksesJson(Tbmuser tbmuser) throws Exception {
		JSONObject semua = new JSONObject();
		for (int i = 0; i < KUNCI_HAK.length; i++) {
			String kunci = KUNCI_HAK[i];
			JSONObject j = new JSONObject();
			j.put("create", ApotikApiHelper.bolehAksiMenu(tbmuser, kunci, "create"));
			j.put("update", ApotikApiHelper.bolehAksiMenu(tbmuser, kunci, "update"));
			j.put("delete", ApotikApiHelper.bolehAksiMenu(tbmuser, kunci, "delete"));
			semua.put(kunci, j);
		}
		return semua;
	}

	public static boolean dispatch(String action, Tbmuser tbmuser, JSONObject payload, JSONObject hasil)
			throws Exception {
		if (action == null || !action.startsWith("apotik_")) {
			return false;
		}
		if ("apotik_item_cari".equals(action)) {
			ApotikApiHelper.itemCari(payload, hasil);
			// Daftar ini SELALU dimuat layar Persediaan saat dibuka, sebelum formulir
			// mana pun disentuh -- tempat paling awal memberi tahu tombol mana yang
			// akan ditolak.
			hasil.put("hak", hakAksesJson(tbmuser));
		} else if ("apotik_item_batch".equals(action)) {
			ApotikApiHelper.itemBatch(payload, hasil);
		} else if ("apotik_resep_list".equals(action)) {
			ApotikApiHelper.resepList(payload, hasil);
		} else if ("apotik_resep_detail".equals(action)) {
			ApotikApiHelper.resepDetail(payload, hasil);
		} else if ("apotik_antrean_farmasi_list".equals(action)) {
			ApotikApiHelper.antreanFarmasiList(tbmuser, payload, hasil);
		} else if ("apotik_antrean_farmasi_simpan".equals(action)) {
			ApotikApiHelper.antreanFarmasiSimpan(tbmuser, payload, hasil);
		} else if ("apotik_antrean_farmasi_status".equals(action)) {
			ApotikApiHelper.antreanFarmasiStatus(tbmuser, payload, hasil);
		} else if ("apotik_antrean_farmasi_hapus".equals(action)) {
			ApotikApiHelper.antreanFarmasiHapus(tbmuser, payload, hasil);
		} else if ("apotik_item_profil_simpan".equals(action)) {
			ApotikApiHelper.itemProfilSimpan(tbmuser, payload, hasil);
		} else if (action.startsWith("apotik_dispensing_")) {
			ApotikDispensingHelper.proses(action, tbmuser, payload, hasil);
		} else if ("apotik_cara_bayar_list".equals(action)) {
			ApotikApiHelper.caraBayarList(payload, hasil);
		} else if ("apotik_bayar".equals(action)) {
			ApotikApiHelper.bayar(tbmuser, payload, hasil);
		} else if ("apotik_racikan_list".equals(action)) {
			ApotikRacikanProduksiHelper.racikanList(payload, hasil);
		} else if ("apotik_bayar_racikan".equals(action)) {
			ApotikRacikanProduksiHelper.bayarRacikan(tbmuser, payload, hasil);
		} else if ("apotik_produksi_katalog".equals(action)) {
			ApotikRacikanProduksiHelper.produksiKatalog(payload, hasil);
		} else if ("apotik_produksi_proses".equals(action)) {
			ApotikRacikanProduksiHelper.prosesProduksi(tbmuser, payload, hasil);
		} else if ("apotik_terima_barang".equals(action)) {
			ApotikPersediaanHelper.terimaBarang(tbmuser, payload, hasil);
		} else if ("apotik_opname_simpan".equals(action)) {
			ApotikPersediaanHelper.opnameSimpan(tbmuser, payload, hasil);
		} else if ("apotik_retur_simpan".equals(action)) {
			ApotikPersediaanHelper.returSimpan(tbmuser, payload, hasil);
		} else if ("apotik_batch_status_ubah".equals(action)) {
			ApotikPersediaanHelper.batchStatusUbah(tbmuser, payload, hasil);
		} else if ("apotik_batch_monitor".equals(action)) {
			ApotikPersediaanHelper.batchMonitor(payload, hasil);
		} else if ("apotik_laporan_penjualan".equals(action)) {
			ApotikLaporanHelper.laporanPenjualan(payload, hasil);
		} else if ("apotik_sesi_kas_status".equals(action)) {
			ApotikSesiKasHelper.status(tbmuser, payload, hasil);
		} else if ("apotik_sesi_kas_buka".equals(action)) {
			ApotikSesiKasHelper.buka(tbmuser, payload, hasil);
		} else if ("apotik_sesi_kas_tutup".equals(action)) {
			ApotikSesiKasHelper.tutup(tbmuser, payload, hasil);
		} else if ("apotik_sesi_kas_list".equals(action)) {
			ApotikSesiKasHelper.daftar(tbmuser, payload, hasil);
		} else if ("apotik_metrik_operasional".equals(action)) {
			ApotikMetrikHelper.metrikOperasional(payload, hasil);
		} else if ("apotik_laporan_pembayaran".equals(action)) {
			ApotikLaporanHelper.laporanPembayaran(payload, hasil);
		} else if ("apotik_laporan_terkendali".equals(action)) {
			ApotikLaporanHelper.laporanTerkendali(payload, hasil);
		} else if ("apotik_laporan_kedaluwarsa".equals(action)) {
			ApotikLaporanHelper.laporanKedaluwarsa(payload, hasil);
		} else if ("apotik_provision_demo".equals(action)) {
			// Provisioning UAT: admin + token konfirmasi + hanya server tanpa data SIRS.
			ApotikDemoProvisionHelper.provisionDemo(tbmuser, payload, hasil);
		} else {
			hasil.put("status", "error");
			hasil.put("message", "Aksi POS Apotik belum tersedia di server ini: " + action);
			return true;
		}
		normalisasi(hasil);
		return true;
	}

	/** Seragamkan konvensi "00"/"91" ke status:"success"/"error" (paritas dispatcher si_). */
	private static void normalisasi(JSONObject hasil) throws Exception {
		String status = hasil.optString("status", "");
		if ("00".equals(status)) {
			hasil.put("status", "success");
		} else if (!"success".equals(status) && !"error".equals(status)) {
			hasil.put("statusAsli", status);
			hasil.put("status", "error");
			if (!hasil.has("message")) {
				hasil.put("message", hasil.optString("description", "Permintaan tidak dapat diproses."));
			}
		}
	}
}
