package ais.common;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * <h3>Katalog tunggal semua menu POS/e-Kantin yang bisa diatur per Grup Pengguna (Tbmrole).</h3>
 *
 * <p>Sumber kebenaran TUNGGAL utk apa saja yang bisa ditampil/disembunyikan lewat kolom JSON
 * {@link ais.database.model.Tbmrole#getEbisnisMenu()} -- MENGGANTIKAN 26 kolom Boolean {@code akses*}
 * terpisah yang sebelumnya ada langsung di {@code Tbmrole} (tidak pernah terpakai UI apa pun sebelum
 * dihapus, lihat migrasi {@code migrasi_ebisnis_menu_konsolidasi.sql}).</p>
 *
 * <p><b>Kenapa satu kolom JSON, bukan kolom per menu:</b> platform ini sedang bertransformasi dari
 * sekadar POS jadi ERP eBisnis penuh (lihat dokumen strategi "MASTER_PROMPT_CODEX_CLAUDE_EBISNIS_ID.md"
 * -- modul Finance/Inventory/HR/Payroll/CRM/dll direncanakan menyusul secara bertahap). Menambah kolom
 * Boolean baru tiap kali ada menu baru tidak scalable (tiap kolom baru = ALTER TABLE + migrasi tabel
 * audit Envers manual). Katalog ini di-hardcode di kode (bukan tabel DB terpisah) krn daftar menu
 * berubah mengikuti rilis kode, bukan data yang diedit runtime -- konsisten dgn pola {@code
 * webapp/WEB-INF/baru/include/menu.jsp}'s {@code subEL}/{@code subSubEL}.</p>
 *
 * <p><b>Struktur JSON tersimpan</b> di {@code Tbmrole.ebisnisMenu} (contoh):</p>
 * <pre>{@code
 * {
 *   "supervisor": false,
 *   "berandaKantin": false,
 *   "landingKantin": false,
 *   "landingInventory": false,
 *   "menu": {
 *     "kasir": true,
 *     "ringkasan": true,
 *     "pembayaran": false,
 *     ...
 *   }
 * }
 * }</pre>
 * <p>{@code menu} adalah map kunci-&gt;boolean (bukan array of object) supaya lookup satu menu O(1) dan
 * baris tak dikenal (kunci lama yang sudah dihapus dari katalog, atau kunci masa depan yang belum
 * dikenal versi kode ini) tidak pernah menyebabkan error parse -- cukup diabaikan.</p>
 */
public final class EbisnisMenuKatalog {

	private EbisnisMenuKatalog() {
	}

	/** Satu baris menu di katalog: kunci JSON, label tampilan, grup/modul, dan platform yang memuatnya. */
	public static final class Entri {
		public final String modul;
		public final String kunci;
		public final String label;
		/** Subset dari {@code "desktop"}, {@code "android"}, {@code "jsp"}. */
		public final String[] platform;

		public Entri(String modul, String kunci, String label, String... platform) {
			this.modul = modul;
			this.kunci = kunci;
			this.label = label;
			this.platform = platform;
		}
	}

	/** Label grup ditampilkan di UI, urut sesuai kemunculan pertama pada {@link #DAFTAR}. */
	public static final String MODUL_POS = "Menu POS (Kasir Desktop / Android)";
	public static final String MODUL_KANTIN_JSP = "Menu Pengaturan e-Kantin (versi JSP)";
	/** Varian "eBisnis Inventory &amp; Sales" (48 layar legacy, lihat docs/pos-inventory-sales di repo zishof-platform). */
	public static final String MODUL_INVENTORY_SALES = "Menu Inventory & Sales (varian eBisnis Inventory & Sales)";
	/** Varian "POS Apotik" (eFarmasi) -- penjualan obat resep/bebas, batch-kedaluwarsa, pengadaan PBF.
	 *  Layar datanya MENUMPANG modul SIRS existing ({@code ais.action.master.sirs}, 191 berkas) --
	 *  lihat komentar padanan per baris {@link #DAFTAR}; JANGAN membangun ulang layar SIRS. */
	public static final String MODUL_APOTIK = "Menu POS Apotik (varian eFarmasi)";
	/** Varian "POS eMedik" -- kasir layanan fasilitas kesehatan (pendaftaran/tagihan/deposit/penjamin),
	 *  BUKAN penjual obat: pemisahan wewenang apoteker vs tenaga medis disengaja (lihat seed role). */
	public static final String MODUL_EMEDIK = "Menu POS eMedik (varian layanan medis)";
	/** Varian "MitraInap" (hotel/penginapan) -- mengisi slot HOTEL_PENGINAPAN JenisUsahaTenantSeedService. */
	public static final String MODUL_MITRAINAP = "Menu MitraInap (varian hotel/penginapan)";

	/**
	 * Daftar lengkap menu yang dikenal versi kode ini. Menambah menu baru = tambah baris di sini --
	 * TIDAK perlu ALTER TABLE apa pun (lihat JavaDoc kelas). Kunci HARUS persis sama dgn yang dipakai
	 * {@code akses-menu.js} (Desktop), {@code PETA_LAYAR_AKSES} (Android), &amp; {@code menu.jsp}
	 * {@code subSubEL} (JSP) supaya satu katalog ini benar-benar jadi satu-satunya sumber kebenaran.
	 */
	public static final List<Entri> DAFTAR = new ArrayList<Entri>();
	static {
		// -- 13 menu POS (Desktop Electron + Android, kunci sama persis dgn akses-menu.js/app.js) --
		DAFTAR.add(new Entri(MODUL_POS, "kasir", "Kasir (POS)", "desktop", "android"));
		DAFTAR.add(new Entri(MODUL_POS, "ringkasan", "Ringkasan", "desktop", "android"));
		DAFTAR.add(new Entri(MODUL_POS, "pesanan", "Pesanan Online", "desktop", "android"));
		DAFTAR.add(new Entri(MODUL_POS, "anggota", "Anggota / Member", "desktop", "android"));
		DAFTAR.add(new Entri(MODUL_POS, "produk", "Produk / Barang", "desktop", "android"));
		// Kendali HPP/harga jual terpusat lintas outlet (lihat GrupProduk/GrupProdukAction).
		// Fail-closed via KUNCI_DEFAULT_NONAKTIF: perubahan harga massal lintas outlet TIDAK
		// boleh mendadak tersedia utk role existing -- nyala hanya lewat grid CRUD TbmroleAction.
		DAFTAR.add(new Entri(MODUL_POS, "grup_produk", "Grup Produk (Harga Terpusat)", "desktop", "android", "jsp"));
		DAFTAR.add(new Entri(MODUL_POS, "stokopname", "Stok Opname", "desktop", "android"));
		DAFTAR.add(new Entri(MODUL_POS, "kulakan", "Kulakan", "desktop", "android"));
		// Modul Pengadaan POS (PR -> PO -> BAST -> Tagihan -> Bayar -> Kulakan). Lingkup TOKO,
		// padanan alur ZKoss versi umum. Fail-closed via KUNCI_DEFAULT_NONAKTIF: menyangkut
		// komitmen pembelian & pembayaran vendor, tidak boleh mendadak muncul utk role existing.
		DAFTAR.add(new Entri(MODUL_POS, "pengadaan_pr", "Pengadaan: Permintaan Pembelian (PR)", "desktop", "android", "jsp"));
		DAFTAR.add(new Entri(MODUL_POS, "diskon", "Aturan Diskon", "desktop", "android"));
		DAFTAR.add(new Entri(MODUL_POS, "returpenjualan", "Retur Penjualan", "desktop", "android"));
		DAFTAR.add(new Entri(MODUL_POS, "riwayatpenjualan", "Riwayat Penjualan", "desktop", "android"));
		DAFTAR.add(new Entri(MODUL_POS, "laporantransaksi", "Laporan Transaksi", "desktop", "android"));
		DAFTAR.add(new Entri(MODUL_POS, "laporan", "Laporan Katalog", "desktop", "android"));
		DAFTAR.add(new Entri(MODUL_POS, "laporankeuangan", "Laporan Keuangan", "desktop", "android", "jsp"));
		DAFTAR.add(new Entri(MODUL_POS, "riwayatsinkronisasi", "Riwayat Sinkronisasi", "desktop", "android"));
		DAFTAR.add(new Entri(MODUL_POS, "logerror", "Log Error", "desktop", "android"));
		DAFTAR.add(new Entri(MODUL_POS, "konfigurasi", "Konfigurasi (Desktop/Android)", "desktop", "android"));

		// -- 12 menu JSP e-Kantin di bawah "Pengaturan" (menu.jsp subSubEL), belum ada padanan Desktop/Android --
		DAFTAR.add(new Entri(MODUL_KANTIN_JSP, "pembayaran", "Pembayaran", "jsp"));
		DAFTAR.add(new Entri(MODUL_KANTIN_JSP, "pedagang", "Pedagang (Kelola Akun)", "jsp"));
		DAFTAR.add(new Entri(MODUL_KANTIN_JSP, "meja", "Meja", "jsp"));
		DAFTAR.add(new Entri(MODUL_KANTIN_JSP, "penyedia", "Penyedia (Vendor)", "jsp"));
		DAFTAR.add(new Entri(MODUL_KANTIN_JSP, "kaskasir", "Kas Kasir", "jsp"));
		DAFTAR.add(new Entri(MODUL_KANTIN_JSP, "setorantenant", "Setoran Tenant", "jsp"));
		DAFTAR.add(new Entri(MODUL_KANTIN_JSP, "jadwalopname", "Jadwal Opname", "jsp"));
		DAFTAR.add(new Entri(MODUL_KANTIN_JSP, "stokexpired", "Stok Min & Expired", "jsp"));
		DAFTAR.add(new Entri(MODUL_KANTIN_JSP, "limitkredit", "Limit Kredit Anggota", "jsp"));
		DAFTAR.add(new Entri(MODUL_KANTIN_JSP, "mutasirekening", "Rekening Koran (Rekonsiliasi)", "jsp"));
		DAFTAR.add(new Entri(MODUL_KANTIN_JSP, "produksi", "Produksi Kantin", "jsp"));
		DAFTAR.add(new Entri(MODUL_KANTIN_JSP, "pengaturanlaporan", "Konfigurasi Laporan (Admin Kantin)", "jsp"));

		// -- 16 menu varian "eBisnis Inventory & Sales" (48 layar legacy; layar yang reuse layar POS
		// existing TETAP memakai kunci lamanya: Stok Opname->stokopname, Kulakan->kulakan,
		// Sales Order digabung ke penjualan_sales [layar 30 = satu layar]) --
		DAFTAR.add(new Entri(MODUL_INVENTORY_SALES, "master_supplier", "Master Supplier", "desktop", "android", "jsp"));
		DAFTAR.add(new Entri(MODUL_INVENTORY_SALES, "master_customer", "Master Customer", "desktop", "android", "jsp"));
		DAFTAR.add(new Entri(MODUL_INVENTORY_SALES, "master_sales", "Master Sales", "desktop", "android", "jsp"));
		DAFTAR.add(new Entri(MODUL_INVENTORY_SALES, "persediaan", "Persediaan & Kartu Stok", "desktop", "android", "jsp"));
		DAFTAR.add(new Entri(MODUL_INVENTORY_SALES, "harga", "Master & Analisis Harga", "desktop", "android", "jsp"));
		DAFTAR.add(new Entri(MODUL_INVENTORY_SALES, "hutang", "Hutang Supplier (AP)", "desktop", "android", "jsp"));
		DAFTAR.add(new Entri(MODUL_INVENTORY_SALES, "penjualan_sales", "Penjualan Sales / Sales Order", "desktop", "android", "jsp"));
		DAFTAR.add(new Entri(MODUL_INVENTORY_SALES, "piutang", "Piutang Customer (AR)", "desktop", "android", "jsp"));
		DAFTAR.add(new Entri(MODUL_INVENTORY_SALES, "surat_perintah_sales", "Surat Perintah Sales Jalan", "desktop", "android", "jsp"));
		DAFTAR.add(new Entri(MODUL_INVENTORY_SALES, "nota_sales", "Nota Sales (Sesi)", "desktop", "android", "jsp"));
		DAFTAR.add(new Entri(MODUL_INVENTORY_SALES, "biaya_sales", "Biaya Sales", "desktop", "android", "jsp"));
		DAFTAR.add(new Entri(MODUL_INVENTORY_SALES, "pembelian_sales", "Pembelian dalam Sesi Sales", "desktop", "android", "jsp"));
		DAFTAR.add(new Entri(MODUL_INVENTORY_SALES, "rekonsiliasi_sales", "Rekonsiliasi Sesi Sales", "desktop", "android", "jsp"));
		DAFTAR.add(new Entri(MODUL_INVENTORY_SALES, "kas_jurnal", "Kas & Jurnal", "desktop", "android", "jsp"));
		DAFTAR.add(new Entri(MODUL_INVENTORY_SALES, "laba_rugi", "Laba / Rugi", "desktop", "android", "jsp"));
		DAFTAR.add(new Entri(MODUL_INVENTORY_SALES, "laporan_inventory_sales", "Laporan Inventory & Sales", "desktop", "android", "jsp"));

		// -- Varian POS Apotik (eFarmasi) -- padanan layar SIRS hasil survei sirs/ dicatat per baris;
		// hanya "apotik_narkotika" yang TIDAK punya padanan (pekerjaan baru di fase layar apotik).
		DAFTAR.add(new Entri(MODUL_APOTIK, "apotik_kasir", "Kasir Apotik", "desktop", "android")); // sirs/TransaksiAction + detail/TransaksiItemDetailHelper
		DAFTAR.add(new Entri(MODUL_APOTIK, "apotik_resep", "Tebus Resep Dokter", "desktop", "android")); // sirs/detail/ResepHelper + helper/AmbilDataResepBanbox
		DAFTAR.add(new Entri(MODUL_APOTIK, "apotik_racikan", "Racikan", "desktop", "android")); // sirs/RacikanAction + helper/BuatRacikanBaruHelper
		DAFTAR.add(new Entri(MODUL_APOTIK, "apotik_formularium", "Formularium & Obat", "desktop", "android")); // sirs/ItemMedisAction + GenerikItemAction
		DAFTAR.add(new Entri(MODUL_APOTIK, "apotik_batch", "Batch & Kedaluwarsa", "desktop", "android")); // sirs/detail/KadaluarsaAction + MonitorKadaluarsaItemAction
		DAFTAR.add(new Entri(MODUL_APOTIK, "apotik_pengadaan", "Pengadaan / PBF", "desktop", "android")); // sirs/PermintaanPembelian->PesananPembelian->PenerimaanOrder(Kembali)
		DAFTAR.add(new Entri(MODUL_APOTIK, "apotik_stok_opname", "Stok Opname Apotik", "desktop", "android")); // sirs/KoreksiItemAction + MonitorStokItemAction + SaldoAwalAction
		DAFTAR.add(new Entri(MODUL_APOTIK, "apotik_retur", "Retur Obat", "desktop", "android")); // sirs/TransaksiReturAction + PemakaianReturItemAction
		DAFTAR.add(new Entri(MODUL_APOTIK, "apotik_narkotika", "Obat Terkendali (Narkotika/Psikotropika)", "desktop", "android")); // BARU -- tanpa padanan SIRS
		DAFTAR.add(new Entri(MODUL_APOTIK, "apotik_laporan", "Laporan Apotik", "desktop", "android")); // sirs/chart/KadaluarsaFarmasiDashboard + PendapatanDashboard

		// -- Varian POS eMedik (kasir layanan medis, non-obat) --
		DAFTAR.add(new Entri(MODUL_EMEDIK, "emedik_kasir", "Kasir Layanan Medis", "desktop", "android")); // sirs/TransaksiAction + PembayaranAction
		DAFTAR.add(new Entri(MODUL_EMEDIK, "emedik_pendaftaran", "Pendaftaran Pasien", "desktop", "android")); // sirs/BookingRegistrasiAction + PendaftaranRawatJalan/Inap/Ugd
		DAFTAR.add(new Entri(MODUL_EMEDIK, "emedik_tagihan", "Tagihan Kunjungan", "desktop", "android")); // sirs/TransaksiAction + StatusPembayaranAction
		DAFTAR.add(new Entri(MODUL_EMEDIK, "emedik_deposit", "Deposit Pasien", "desktop", "android")); // sirs/DepositAction
		DAFTAR.add(new Entri(MODUL_EMEDIK, "emedik_penjamin", "Penjamin & Asuransi", "desktop", "android")); // sirs/AsuransiAction + util/PenjaminResolver
		DAFTAR.add(new Entri(MODUL_EMEDIK, "emedik_laporan", "Laporan Kasir Medis", "desktop", "android")); // sirs/chart/PendapatanDashboard + PendaftaranOverviewDashboard
		// MitraInap MVP langkah 2 (master); reservasi/checkin/folio menyusul fase berikutnya.
		DAFTAR.add(new Entri(MODUL_MITRAINAP, "hotel_properti", "Properti Hotel", "desktop", "android"));
		DAFTAR.add(new Entri(MODUL_MITRAINAP, "hotel_kamar", "Tipe Kamar & Kamar", "desktop", "android"));
		DAFTAR.add(new Entri(MODUL_MITRAINAP, "hotel_reservasi", "Tamu & Reservasi", "desktop", "android"));
		DAFTAR.add(new Entri(MODUL_MITRAINAP, "hotel_checkin", "Check-in / Check-out", "desktop", "android"));
		DAFTAR.add(new Entri(MODUL_MITRAINAP, "hotel_folio", "Folio Tamu", "desktop", "android"));
		// MitraInap langkah 5: dapur outlet + kontrak/statement pemilik kamar (kondotel).
		DAFTAR.add(new Entri(MODUL_MITRAINAP, "hotel_tiket_dapur", "Tiket Dapur", "desktop", "android"));
		DAFTAR.add(new Entri(MODUL_MITRAINAP, "hotel_kontrak_pemilik", "Kontrak Pemilik", "desktop", "android"));
		DAFTAR.add(new Entri(MODUL_MITRAINAP, "hotel_laporan_pemilik", "Laporan Pemilik", "desktop", "android"));
	}

	/**
	 * Kunci yang DEFAULT-nya NONAKTIF (kebalikan konvensi katalog lama yang default {@code true}):
	 * seluruh kunci {@link #MODUL_INVENTORY_SALES}. Alasan fail-closed: menu varian baru TIDAK
	 * boleh bocor otomatis ke role POS existing yang belum pernah menyimpan kunci ini (aturan
	 * "gerbang baru wajib opt-in" -- role lama tidak berubah perilakunya, admin mengaktifkan
	 * eksplisit lewat editor Grup Pengguna; role seed {@code pemilik_sales_inventory}/
	 * {@code sales_keliling} sudah menyimpan nilai eksplisit sendiri).
	 */
	public static final java.util.Set<String> KUNCI_DEFAULT_NONAKTIF = new java.util.LinkedHashSet<String>(java.util.Arrays.asList(
			// Grup Produk: perubahan harga massal lintas outlet -- fail-closed, nyala hanya via admin.
			"grup_produk",
			// Pengadaan POS: komitmen pembelian & pembayaran vendor -- fail-closed.
			"pengadaan_pr",
			// MitraInap: vertikal baru -- role POS existing tidak boleh mendadak melihatnya.
			"hotel_properti", "hotel_kamar", "hotel_reservasi", "hotel_checkin", "hotel_folio",
			"hotel_tiket_dapur", "hotel_kontrak_pemilik", "hotel_laporan_pemilik",
			"master_supplier", "master_customer", "master_sales", "persediaan", "harga", "hutang",
			"penjualan_sales", "piutang", "surat_perintah_sales", "nota_sales", "biaya_sales",
			"pembelian_sales", "rekonsiliasi_sales", "kas_jurnal", "laba_rugi", "laporan_inventory_sales",
			// varian POS Apotik + POS eMedik: alasan fail-closed yang sama -- role POS existing
			// TIDAK boleh mendadak melihat menu apotek/medis; nyala hanya via seed 1.5 / admin.
			"apotik_kasir", "apotik_resep", "apotik_racikan", "apotik_formularium", "apotik_batch",
			"apotik_pengadaan", "apotik_stok_opname", "apotik_retur", "apotik_narkotika", "apotik_laporan",
			"emedik_kasir", "emedik_pendaftaran", "emedik_tagihan", "emedik_deposit", "emedik_penjamin",
			"emedik_laporan"));

	/**
	 * Subset {@link #DAFTAR} kunci yang punya record/CRUD sungguhan (bukan sekadar layar lihat-saja spt
	 * Ringkasan/Log Error/Konfigurasi) -- menu inilah yang dapat kontrol granular per {@link #AKSI_CRUD}
	 * ("Read"-nya sendiri TETAP diwakili {@code menu.<kunci>} yang sudah ada, tidak diduplikasi di sini).
	 * Dipilih dari 16 menu yang punya aksi tambah/ubah/hapus/setujui-tolak record nyata (Produk, Anggota,
	 * Diskon, Kulakan, Retur Penjualan, Stok Opname, Pesanan [approve/reject pesanan online], Pembayaran
	 * [metode pembayaran], Pedagang, Penyedia/Vendor, Limit Kredit, Kas Kasir, Setoran Tenant, Jadwal
	 * Opname, Rekening Koran, Produksi) -- menu murni laporan/status/pengaturan toggle SENGAJA tidak
	 * disertakan (Approve/Reject/Create/Update/Delete tidak berarti apa pun di sana).
	 */
	public static final java.util.Set<String> KUNCI_CRUD = new java.util.LinkedHashSet<String>(java.util.Arrays.asList(
			"produk", "grup_produk", "hotel_properti", "hotel_kamar", "hotel_reservasi", "hotel_checkin", "hotel_folio",
			"hotel_tiket_dapur", "hotel_kontrak_pemilik", "hotel_laporan_pemilik", "anggota", "diskon", "kulakan", "returpenjualan", "riwayatpenjualan", "stokopname", "pesanan",
			"pembayaran", "pedagang", "penyedia", "limitkredit", "kaskasir", "setorantenant",
			"jadwalopname", "mutasirekening", "produksi",
			// varian Inventory & Sales (default aksi ikut KUNCI_DEFAULT_NONAKTIF: false)
			"master_supplier", "master_customer", "master_sales", "harga", "hutang",
			"penjualan_sales", "piutang", "surat_perintah_sales", "nota_sales", "biaya_sales",
			"pembelian_sales", "kas_jurnal",
			// varian POS Apotik/eMedik: menu ber-record nyata (laporan & monitor batch sengaja
			// tidak disertakan -- tidak ada create/update/delete yang berarti di sana)
			"apotik_kasir", "apotik_resep", "apotik_racikan", "apotik_formularium",
			"apotik_pengadaan", "apotik_stok_opname", "apotik_retur", "apotik_narkotika",
			"emedik_kasir", "emedik_pendaftaran", "emedik_tagihan", "emedik_deposit", "emedik_penjamin"));

	/** Aksi granular yg bisa diatur per {@link #KUNCI_CRUD}, di luar Read (sudah diwakili {@code menu}). */
	public static final String[] AKSI_CRUD = { "create", "update", "delete", "approve", "reject" };

	/**
	 * Bungkus JSON default: {@code supervisor}/{@code berandaKantin}/{@code landingKantin} = false,
	 * SEMUA baris {@link #DAFTAR} = true (perilaku "semua menu tampil" spt sebelum fitur hak-akses-per-
	 * grup ini ada -- akun lama/role lama yang belum pernah menyimpan {@code ebisnisMenu} tidak boleh
	 * mendadak kehilangan akses menu begitu kolom ini dibaca pertama kali), SEMUA aksi {@link #AKSI_CRUD}
	 * per {@link #KUNCI_CRUD} = true juga (alasan sama -- role lama yg blm pernah simpan grid CRUD tidak
	 * boleh mendadak kehilangan Create/Update/Delete/Approve/Reject yg sebelumnya implisit boleh semua).
	 */
	public static JSONObject defaultObj() {
		JSONObject obj = new JSONObject();
		try {
			obj.put("supervisor", false);
			obj.put("berandaKantin", false);
			obj.put("landingKantin", false);
			obj.put("landingInventory", false);
			JSONObject menu = new JSONObject();
			for (Entri e : DAFTAR) {
				// Kunci lama default TAMPIL (backward-compat, lihat JavaDoc method); kunci varian
				// Inventory & Sales default TERSEMBUNYI (fail-closed, lihat KUNCI_DEFAULT_NONAKTIF).
				menu.put(e.kunci, !KUNCI_DEFAULT_NONAKTIF.contains(e.kunci));
			}
			obj.put("menu", menu);
			JSONObject crud = new JSONObject();
			for (String kunci : KUNCI_CRUD) {
				JSONObject aksiMenu = new JSONObject();
				boolean defaultAksi = !KUNCI_DEFAULT_NONAKTIF.contains(kunci);
				for (String aksi : AKSI_CRUD) {
					aksiMenu.put(aksi, defaultAksi);
				}
				crud.put(kunci, aksiMenu);
			}
			obj.put("crud", crud);
		} catch (JSONException ex) {
			// put(String, boolean) tidak pernah benar-benar melempar ini dlm praktik (hanya utk
			// nilai numerik tak valid spt NaN) -- ditangkap murni krn org.json versi ini mendeklarasikan
			// checked exception pada put(), bukan krn skenario ini bisa terjadi.
			ais.common.ErrorAuditUtil.record(ex, "EbisnisMenuKatalog.defaultObj: tidak seharusnya terjadi");
		}
		return obj;
	}

	/**
	 * Parse {@code ebisnisMenu} mentah dgn fallback aman ke {@link #defaultObj()} bila {@code null},
	 * kosong, atau JSON rusak -- JANGAN pernah panggil {@code new JSONObject(raw)} langsung di
	 * pemanggil, gunakan method ini supaya satu tempat saja yang menangani nilai kosong/lama/rusak.
	 * Baris {@code menu} yang tidak disebut di JSON tersimpan (mis. role lama yang disimpan sebelum
	 * menu baru ditambahkan ke {@link #DAFTAR}) otomatis dianggap {@code true} (default tampil). Sama
	 * halnya {@code crud.<kunci>.<aksi>} yang tidak disebut (role lama sblm grid CRUD ada, atau aksi baru
	 * yg belum dikenal versi lama) otomatis dianggap {@code true} (default boleh).
	 */
	public static JSONObject urai(String raw) {
		JSONObject hasil = defaultObj();
		if (raw == null || raw.trim().isEmpty()) {
			return hasil;
		}
		try {
			JSONObject tersimpan = new JSONObject(raw);
			if (tersimpan.has("supervisor")) {
				hasil.put("supervisor", tersimpan.optBoolean("supervisor", false));
			}
			if (tersimpan.has("berandaKantin")) {
				hasil.put("berandaKantin", tersimpan.optBoolean("berandaKantin", false));
			}
			if (tersimpan.has("landingKantin")) {
				hasil.put("landingKantin", tersimpan.optBoolean("landingKantin", false));
			}
			if (tersimpan.has("landingInventory")) {
				hasil.put("landingInventory", tersimpan.optBoolean("landingInventory", false));
			}
			JSONObject menuTersimpan = tersimpan.optJSONObject("menu");
			if (menuTersimpan != null) {
				JSONObject menu = hasil.getJSONObject("menu");
				for (Entri e : DAFTAR) {
					if (menuTersimpan.has(e.kunci)) {
						menu.put(e.kunci, menuTersimpan.optBoolean(e.kunci, true));
					}
				}
			}
			JSONObject crudTersimpan = tersimpan.optJSONObject("crud");
			if (crudTersimpan != null) {
				JSONObject crud = hasil.getJSONObject("crud");
				for (String kunci : KUNCI_CRUD) {
					JSONObject aksiTersimpan = crudTersimpan.optJSONObject(kunci);
					if (aksiTersimpan == null) {
						continue;
					}
					JSONObject aksiMenu = crud.getJSONObject(kunci);
					for (String aksi : AKSI_CRUD) {
						if (aksiTersimpan.has(aksi)) {
							aksiMenu.put(aksi, aksiTersimpan.optBoolean(aksi, true));
						}
					}
				}
			}
		} catch (Exception ex) {
			ais.common.ErrorAuditUtil.record(ex, "EbisnisMenuKatalog.urai: JSON ebisnisMenu rusak, pakai default");
			return defaultObj();
		}
		return hasil;
	}

	/**
	 * Cek satu aksi granular ({@link #AKSI_CRUD}) utk satu menu {@link #KUNCI_CRUD} -- panggil ini di
	 * SETIAP endpoint create/update/delete/approve/reject utk menu ber-CRUD (server-side, BUKAN cuma
	 * gerbang UI kosmetik). Supervisor ({@code supervisor} flag hasil {@link #urai(String)}) SELALU
	 * boleh apa pun -- bypass total, satu toggle blanket ("Supervisor = ALL Checked Akses"), BUKAN baris
	 * grid CRUD tersendiri. Menu di luar {@link #KUNCI_CRUD}, aksi tak dikenal, atau role/aksi yg belum
	 * pernah disimpan (role lama) selalu {@code true} (tidak digerbang granular / default boleh -- lihat
	 * catatan backward-compat {@link #urai(String)}). Pemanggil TETAP wajib cek {@code menu.<kunci>}
	 * (visibilitas) terpisah spt biasa -- method ini hanya utk aksi mutasi di dalam menu yg sudah tampil.
	 */
	public static boolean bolehAksi(JSONObject roleEbisnisMenu, String kunciMenu, String aksi) {
		if (roleEbisnisMenu == null) {
			return true;
		}
		if (roleEbisnisMenu.optBoolean("supervisor", false)) {
			return true;
		}
		if (kunciMenu == null || !KUNCI_CRUD.contains(kunciMenu)) {
			return true;
		}
		JSONObject crud = roleEbisnisMenu.optJSONObject("crud");
		if (crud == null) {
			return true;
		}
		JSONObject aksiMenu = crud.optJSONObject(kunciMenu);
		if (aksiMenu == null) {
			return true;
		}
		return aksiMenu.optBoolean(aksi, true);
	}

	/**
	 * Kunci {@link #DAFTAR} yang aktif SECARA DEFAULT utk role dasar "Kantin" (kasir e-Kantin biasa) --
	 * hanya kemampuan kasir sehari-hari (Kasir/Ringkasan/Pesanan/Anggota/Produk/Stok Opname/Kulakan/
	 * Aturan Diskon/Riwayat Sinkronisasi/Log Error/Konfigurasi). Menu lain (Retur Penjualan, Riwayat
	 * Penjualan, Laporan Transaksi, Laporan, dan seluruh menu {@link #MODUL_KANTIN_JSP} spt Pembayaran/
	 * Pedagang/Vendor/Kas Kasir/dll) TIDAK aktif secara default -- admin toko yg butuh boleh
	 * mengaktifkannya sendiri lewat checkbox "Tambah Grup Pengguna" (Tbmrole), tidak perlu ubah kode.
	 */
	private static final java.util.Set<String> KUNCI_DEFAULT_KANTIN = new java.util.HashSet<String>(java.util.Arrays.asList(
			"kasir", "ringkasan", "pesanan", "anggota", "produk", "stokopname", "kulakan", "diskon",
			"riwayatsinkronisasi", "logerror", "konfigurasi"));

	/**
	 * Bungkus JSON {@code ebisnisMenu} default utk role dasar "Kantin" (lihat {@link #KUNCI_DEFAULT_KANTIN}) --
	 * dipakai sekali saat role "Kantin" pertama kali dibuat ({@code InitDataHelper}) &amp; migrasi backfill
	 * satu-kali utk instalasi lama ({@code InitIndex}). HANYA berlaku sbg nilai AWAL -- begitu admin
	 * mengedit lewat "Tambah Grup Pengguna", hasil edit itu yang dipakai seterusnya (tidak pernah ditimpa
	 * ulang oleh migrasi startup, lihat guard {@code ebisnis_menu IS NULL} di {@code InitIndex}).
	 */
	public static String defaultMenuKantinJson() {
		JSONObject obj = defaultObj();
		try {
			JSONObject menu = obj.getJSONObject("menu");
			for (Entri e : DAFTAR) {
				menu.put(e.kunci, KUNCI_DEFAULT_KANTIN.contains(e.kunci));
			}
		} catch (JSONException ex) {
			ais.common.ErrorAuditUtil.record(ex, "EbisnisMenuKatalog.defaultMenuKantinJson: tidak seharusnya terjadi");
		}
		return obj.toString();
	}

	/**
	 * Susun JSON grid (array baris {@code {modul,kunci,label,platform,tampil}}) siap dipakai frontend
	 * merender checkbox -- menggabungkan {@link #DAFTAR} (definisi tetap dari kode) dgn nilai
	 * {@code tampil} aktual dari {@code roleEbisnisMenu} (hasil {@link #urai(String)}).
	 */
	public static JSONArray gridUntukUi(JSONObject roleEbisnisMenu) {
		JSONArray arr = new JSONArray();
		JSONObject menu = roleEbisnisMenu.optJSONObject("menu");
		try {
			for (Entri e : DAFTAR) {
				JSONObject row = new JSONObject();
				row.put("modul", e.modul);
				row.put("kunci", e.kunci);
				row.put("label", e.label);
				row.put("platform", new JSONArray(e.platform));
				row.put("tampil", menu == null || !menu.has(e.kunci) ? true : menu.optBoolean(e.kunci, true));
				arr.put(row);
			}
		} catch (JSONException ex) {
			// Lihat catatan sama di defaultObj() -- put() di sini praktis tidak pernah gagal.
			ais.common.ErrorAuditUtil.record(ex, "EbisnisMenuKatalog.gridUntukUi: tidak seharusnya terjadi");
		}
		return arr;
	}

	// ============================================================================================
	// TAKSONOMI POHON LENGKAP ("Master Menu eBisnis") -- gap-closure dokumen
	// STRUKTUR_MENU_LENGKAP_EBISNIS_ID.md (21 modul akar, ~770 node). BERBEDA dari DAFTAR/urai() di
	// atas: DAFTAR adalah ~27 kunci DATAR yang SUDAH benar-benar berfungsi (dipakai 8 titik kode utk
	// hak akses show/hide, TIDAK diubah supaya tidak mengulang insiden breaking-change sesi
	// sebelumnya). Bagian di bawah ini adalah lapisan STRUKTUR/NAVIGASI POHON tambahan di atasnya,
	// dipakai KHUSUS utk merender sidebar/drawer berbentuk tree di POS Desktop &amp; Android --
	// SENGAJA disimpan sbg SATU BERKAS JSON GLOBAL ({@code ebisnis_menu_master.json}, resource
	// classpath, pola sama berkas .sql di ImportFromEpsbedHelper), BUKAN tabel database baru (per
	// arahan eksplisit: taksonomi/struktur tidak perlu diedit runtime, cukup 1 sumber file; yang
	// PERLU disimpan per-role di database hanya status aktif/nonaktifnya, lewat
	// {@code Tbmrole.ebisnisMenu} field {@code menu} yang SAMA dipakai {@link #urai(String)} di atas
	// -- kuncinya sekarang BOLEH berupa {@code kode} node pohon ini, bukan cuma 27 {@code kunci} lama).
	// ============================================================================================

	private static volatile JSONObject masterTreeCache = null;

	/** Muat &amp; cache {@code ebisnis_menu_master.json} sekali (thread-safe, idempoten kalau gagal parse). */
	private static JSONObject masterTree() throws JSONException {
		JSONObject cached = masterTreeCache;
		if (cached != null) {
			return cached;
		}
		synchronized (EbisnisMenuKatalog.class) {
			if (masterTreeCache != null) {
				return masterTreeCache;
			}
			JSONObject hasil = new JSONObject();
			hasil.put("menu", new JSONArray());
			java.io.InputStream in = null;
			try {
				in = EbisnisMenuKatalog.class.getResourceAsStream("ebisnis_menu_master.json");
				if (in != null) {
					java.util.Scanner scanner = new java.util.Scanner(in, "UTF-8").useDelimiter("\\A");
					String teks = scanner.hasNext() ? scanner.next() : "{}";
					hasil = new JSONObject(teks);
				}
			} catch (Exception ex) {
				ais.common.ErrorAuditUtil.record(ex, "EbisnisMenuKatalog.masterTree: gagal muat ebisnis_menu_master.json");
			} finally {
				if (in != null) {
					try {
						in.close();
					} catch (Exception ignore) {
					}
				}
			}
			masterTreeCache = hasil;
			return hasil;
		}
	}

	/**
	 * Susun POHON menu (nested, bukan array datar) siap dipakai render sidebar/drawer Desktop/Android
	 * -- HANYA menyertakan node yang {@code tersedia=true} (sudah ada layar sungguhan) sesuai
	 * {@code platform} ({@code "desktop"}/{@code "android"}/{@code "jsp"}), BESERTA seluruh leluhurnya
	 * (folder pengelompok, walau leluhur itu sendiri belum "tersedia") supaya konteks pengelompokan
	 * tetap masuk akal -- node yang tidak {@code tersedia} &amp; tidak punya keturunan tersedia SAMA
	 * SEKALI tidak muncul (bukan ditampilkan abu-abu/terkunci, benar-benar disembunyikan, sesuai
	 * catatan desain di {@code ebisnis_menu_master.json}).
	 *
	 * <p>Status aktif/nonaktif PER GRUP PENGGUNA ({@code roleEbisnisMenu}, hasil {@link #urai(String)})
	 * diterapkan HANYA pada node {@code tersedia=true} (leaf sungguhan) -- kalau kode node ada di
	 * {@code roleEbisnisMenu.menu} &amp; bernilai {@code false}, node itu (dan cabangnya jika ia jadi
	 * kosong) ikut disembunyikan. Node folder murni (tidak {@code tersedia}) tidak pernah digerbang
	 * langsung -- ia otomatis hilang kalau SEMUA anaknya hilang.</p>
	 *
	 * @param platform        {@code "desktop"}, {@code "android"}, atau {@code "jsp"}.
	 * @param roleEbisnisMenu hasil {@link #urai(String)} utk role pemanggil (boleh {@code null} -> semua tersedia tampil).
	 * @return JSONArray node akar, tiap node {@code {kode,label,ikon,rute,children:[...]}}.
	 */
	public static JSONArray treeUntukKlien(String platform, JSONObject roleEbisnisMenu) {
		JSONArray hasil = new JSONArray();
		try {
			JSONArray flat = masterTree().optJSONArray("menu");
			if (flat == null) {
				return hasil;
			}
			JSONObject menuAktif = roleEbisnisMenu == null ? null : roleEbisnisMenu.optJSONObject("menu");

			// 1. Indeks node by kode, & peta anak-per-induk.
			java.util.Map<String, JSONObject> byKode = new java.util.LinkedHashMap<String, JSONObject>();
			for (int i = 0; i < flat.length(); i++) {
				JSONObject n = flat.getJSONObject(i);
				byKode.put(n.getString("kode"), n);
			}

			// 2. Tentukan node "tersedia utk platform ini & aktif utk role ini" -> set node yg WAJIB ada.
			java.util.Set<String> wajibAda = new java.util.LinkedHashSet<String>();
			for (JSONObject n : byKode.values()) {
				if (!n.optBoolean("tersedia", false)) continue;
				JSONArray plat = n.optJSONArray("platform");
				boolean cocokPlatform = false;
				if (plat != null) {
					for (int p = 0; p < plat.length(); p++) {
						if (platform.equals(plat.optString(p))) { cocokPlatform = true; break; }
					}
				}
				if (!cocokPlatform) continue;
				String kode = n.getString("kode");
				// Status aktif/nonaktif per-role SEBENARNYA tersimpan berkunci `kunci` LAMA (spt "kasir",
				// "returpenjualan") -- itulah yg ditulis checkbox "Tambah Grup Pengguna" (TbmroleAction)
				// via buildEbisnisMenuJson(), BUKAN kunci `kode` pohon baru ini ("kasir_pos", dst). Cek
				// `kunciLama` node dulu (kalau ada pemetaannya) baru fallback ke `kode` langsung -- supaya
				// node pohon yg blm py padanan lama pun tetap bisa digerbang lewat kode-nya sendiri kelak.
				String kunciLama = n.has("kunciLama") ? n.optString("kunciLama", null) : null;
				boolean aktifUtkRole = true;
				if (menuAktif != null) {
					if (kunciLama != null && menuAktif.has(kunciLama)) {
						aktifUtkRole = menuAktif.optBoolean(kunciLama, true);
					} else if (menuAktif.has(kode)) {
						aktifUtkRole = menuAktif.optBoolean(kode, true);
					}
				}
				if (!aktifUtkRole) continue;
				// Tambahkan node ini + SELURUH leluhurnya (folder pengelompok).
				String jalan = kode;
				while (jalan != null && wajibAda.add(jalan)) {
					JSONObject node = byKode.get(jalan);
					jalan = node == null ? null : node.optString("kodeInduk", null);
					if (jalan != null && jalan.length() == 0) jalan = null;
				}
			}

			// 3. Bangun anak-per-induk HANYA dari node yg wajibAda, urut by "urutan".
			java.util.Map<String, java.util.List<JSONObject>> anakPerInduk = new java.util.LinkedHashMap<String, java.util.List<JSONObject>>();
			for (String kode : wajibAda) {
				JSONObject n = byKode.get(kode);
				if (n == null) continue;
				String induk = n.isNull("kodeInduk") ? null : n.optString("kodeInduk", null);
				if (induk != null && induk.length() == 0) induk = null;
				java.util.List<JSONObject> list = anakPerInduk.get(induk);
				if (list == null) { list = new java.util.ArrayList<JSONObject>(); anakPerInduk.put(induk, list); }
				list.add(n);
			}
			for (java.util.List<JSONObject> list : anakPerInduk.values()) {
				java.util.Collections.sort(list, new java.util.Comparator<JSONObject>() {
					@Override
					public int compare(JSONObject a, JSONObject b) {
						return a.optInt("urutan", 0) - b.optInt("urutan", 0);
					}
				});
			}

			// 4. Rekursif bangun tree mulai dari akar (kodeInduk == null).
			java.util.List<JSONObject> akarList = anakPerInduk.get(null);
			if (akarList != null) {
				for (JSONObject n : akarList) {
					hasil.put(bangunNodeTree(n, anakPerInduk, platform));
				}
			}
		} catch (Exception ex) {
			ais.common.ErrorAuditUtil.record(ex, "EbisnisMenuKatalog.treeUntukKlien: gagal menyusun tree");
		}
		return hasil;
	}

	private static JSONObject bangunNodeTree(JSONObject n, java.util.Map<String, java.util.List<JSONObject>> anakPerInduk, String platform) throws JSONException {
		JSONObject out = new JSONObject();
		String kode = n.getString("kode");
		out.put("kode", kode);
		out.put("label", n.optString("label", kode));
		out.put("tersedia", n.optBoolean("tersedia", false));
		JSONObject rute = n.optJSONObject("rute");
		out.put("rute", rute != null && rute.has(platform) ? rute.optString(platform) : JSONObject.NULL);
		JSONArray children = new JSONArray();
		java.util.List<JSONObject> anak = anakPerInduk.get(kode);
		if (anak != null) {
			for (JSONObject a : anak) {
				children.put(bangunNodeTree(a, anakPerInduk, platform));
			}
		}
		out.put("children", children);
		return out;
	}
}
