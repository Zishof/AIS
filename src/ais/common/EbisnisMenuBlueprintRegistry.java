package ais.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Registry target menu eBisnis lintas Desktop, Android, JSP, dan ZKoss.
 *
 * <p>Registry ini bersifat aditif. {@link EbisnisMenuKatalog} tetap menjadi adapter
 * kompatibilitas selama route dan hak akses legacy masih digunakan. Satu fungsi
 * bisnis hanya mempunyai satu {@code menuKey}; nama lama disimpan sebagai alias
 * agar sidebar tidak menampilkan menu ganda.</p>
 */
public final class EbisnisMenuBlueprintRegistry {

	public static final String[] AKSI_WORKFLOW = new String[] {
			"view", "create", "edit_draft", "submit", "approve", "reject",
			"cancel", "post", "reverse", "export", "view_cost", "view_all_location"
	};

	/**
	 * Tipe implementasi bersarang {@link Entri} milik {@link EbisnisMenuBlueprintRegistry}. Kelas ini memberi nama
	 * pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
	 * EbisnisMenuBlueprintRegistry}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
	 * digunakan dan diuji.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String menuKey}, {@code String
	 * label}, {@code String canonicalRoute}, {@code String parentKey}, {@code int sortOrder}, {@code List
	 * platforms}, {@code List legacyAliases}, {@code List requiredActions}. Aturan bisnis bersama tetap berada
	 * pada kelas induk atau service yang dipanggilnya.</p>
	 *
	 * @see EbisnisMenuBlueprintRegistry
	 */
	public static final class Entri {
		public final String menuKey;
		public final String label;
		public final String canonicalRoute;
		public final String parentKey;
		public final int sortOrder;
		public final List<String> platforms;
		public final List<String> legacyAliases;
		public final List<String> requiredActions;
		public final String featureFlag;
		public final String deprecatedSince;
		public final String replacementKey;

		private Entri(String menuKey, String label, String canonicalRoute,
				String parentKey, int sortOrder, String platforms,
				String aliases, String actions, String featureFlag,
				String deprecatedSince, String replacementKey) {
			this.menuKey = normalisasi(menuKey);
			this.label = label;
			this.canonicalRoute = canonicalRoute;
			this.parentKey = normalisasi(parentKey);
			this.sortOrder = sortOrder;
			this.platforms = daftarNilai(platforms);
			this.legacyAliases = daftarNilaiNormal(aliases);
			this.requiredActions = daftarNilaiNormal(actions);
			this.featureFlag = normalisasi(featureFlag);
			this.deprecatedSince = deprecatedSince == null ? "" : deprecatedSince.trim();
			this.replacementKey = normalisasi(replacementKey);
		}
	}

	private static final Map<String, Entri> ENTRI = new LinkedHashMap<String, Entri>();
	private static final Map<String, String> ALIAS = new LinkedHashMap<String, String>();

	static {
		// Root navigation.
		grup("operasional_penjualan", "Operasional Penjualan", 100);
		menu("kasir_pos", "Kasir/POS", "/ebisnis/kasir", "operasional_penjualan", 110, "kasir,pos", "view,create");
		menu("pesanan_pelanggan", "Pesanan Pelanggan", "/ebisnis/pesanan", "operasional_penjualan", 120, "pesanan,pesanan_online", "view,create,edit_draft,cancel");
		menu("layar_pelanggan", "Layar Pelanggan", "/ebisnis/layar-pelanggan", "operasional_penjualan", 130, "layarpelanggan", "view");
		menu("retur_penjualan", "Retur Penjualan", "/ebisnis/retur-penjualan", "operasional_penjualan", 140, "returpenjualan,sales_return", "view,create,approve,cancel,export");

		grup("dashboard_control_tower", "Dashboard & Control Tower", 200);
		menu("dashboard_bisnis", "Dashboard Bisnis", "/ebisnis/dashboard", "dashboard_control_tower", 210, "ringkasan,dashboard", "view,export");
		menu("control_tower_rantai_pasok", "Control Tower Rantai Pasok", "/ebisnis/control-tower", "dashboard_control_tower", 220, "", "view,view_all_location,export");
		menu("alert_pengecualian", "Alert & Pengecualian", "/ebisnis/alert", "dashboard_control_tower", 230, "", "view,update");
		menu("kpi_sla", "KPI dan SLA", "/ebisnis/kpi-sla", "dashboard_control_tower", 240, "", "view,export");

		grup("master_data", "Master Data", 300);
		menu("pelanggan_member", "Pelanggan/Member", "/ebisnis/pelanggan", "master_data", 310, "anggota,pelanggan", "view,create,update,delete,export");
		menu("produk", "Produk", "/ebisnis/produk", "master_data", 320, "barang", "view,create,update,delete,export,view_cost");
		menu("kategori_produk_akun", "Kategori Produk & Akun", "/ebisnis/kategori-produk", "master_data", 330, "jenis_produk,jenisproduk", "view,create,update,delete");
		menu("grup_harga_hpp_resep", "Grup Harga, HPP & Resep", "/ebisnis/grup-produk", "master_data", 340, "grup_produk", "view,create,update,delete,view_cost");
		menu("uom_konversi", "Satuan/UOM dan Konversi", "/ebisnis/uom", "master_data", 350, "satuan", "view,create,update,delete");
		menu("supplier_vendor", "Supplier/Vendor", "/ebisnis/supplier", "master_data", 360, "supplier", "view,create,update,delete,export");
		menu("lokasi_logistik", "Outlet, Gudang, Zona, dan Bin", "/ebisnis/lokasi-logistik", "master_data", 370, "toko,gudang", "view,create,update,delete,view_all_location");
		menu("armada_ekspedisi", "Armada dan Ekspedisi", "/ebisnis/armada-ekspedisi", "master_data", 380, "", "view,create,update,delete");
		menu("aturan_diskon_promo", "Aturan Diskon/Promo", "/ebisnis/aturan-diskon", "master_data", 390, "aturan_diskon", "view,create,update,delete,approve");
		menu("cara_pembayaran", "Cara Pembayaran", "/ebisnis/cara-pembayaran", "master_data", 400, "pembayaran", "view,create,update,delete");

		grup("perencanaan_replenishment", "Perencanaan & Replenishment", 500);
		menu("kebijakan_reorder", "Kebijakan Min-Max/Reorder Point", "/ebisnis/replenishment/kebijakan", "perencanaan_replenishment", 510, "", "view,create,update,approve");
		menu("permintaan_stok_outlet", "Permintaan Stok Outlet", "/ebisnis/replenishment/permintaan", "perencanaan_replenishment", 520, "", "view,create,edit_draft,submit,approve,reject,cancel");
		menu("konsolidasi_kebutuhan", "Konsolidasi Kebutuhan", "/ebisnis/replenishment/konsolidasi", "perencanaan_replenishment", 530, "", "view,create,approve,export");
		menu("rekomendasi_replenishment", "Rekomendasi Replenishment", "/ebisnis/replenishment/rekomendasi", "perencanaan_replenishment", 540, "", "view,approve,export");
		menu("alokasi_stok", "Alokasi Stok", "/ebisnis/replenishment/alokasi", "perencanaan_replenishment", 550, "", "view,create,approve,cancel,view_all_location");

		grup("pengadaan", "Pengadaan", 600);
		menu("pengadaan_pr", "Permintaan Pembelian (PR)", "/ebisnis/pengadaan/pr", "pengadaan", 610, "permintaan_pembelian,pr", "view,create,edit_draft,submit,approve,reject,cancel,export");
		menu("pengadaan_rfq", "RFQ & Seleksi Vendor", "/ebisnis/pengadaan/rfq", "pengadaan", 620, "", "view,create,edit_draft,submit,approve,reject,cancel,export,view_cost");
		menu("pengadaan_po", "Pemesanan Pembelian (PO)", "/ebisnis/pengadaan/po", "pengadaan", 630, "pemesanan_pembelian,po", "view,create,edit_draft,submit,approve,reject,cancel,export,view_cost");
		menu("kulakan", "Kulakan", "/ebisnis/kulakan", "pengadaan", 640, "pembelian_stok_langsung", "view,create,update,cancel,export,view_cost");
		menu("pengadaan_bast", "Penerimaan Barang (BAST Vendor)", "/ebisnis/pengadaan/bast", "pengadaan", 650, "penerimaan_barang,bast", "view,create,submit,approve,reject,cancel,export");
		menu("monitoring_pengadaan", "Monitoring Pengadaan", "/ebisnis/pengadaan/monitoring", "pengadaan", 660, "pengadaan_sinkron", "view,export");
		menu("retur_pembelian", "Retur Pembelian", "/ebisnis/pengadaan/retur", "pengadaan", 670, "", "view,create,submit,approve,cancel,export");
		menu("status_tagihan_vendor", "Status Tagihan Vendor", "/ebisnis/keuangan/ap-tagihan", "pengadaan", 680, "", "view,export");

		grup("pergudangan", "Pergudangan", 700);
		menu("dashboard_gudang", "Dashboard Gudang", "/ebisnis/gudang/dashboard", "pergudangan", 710, "", "view,view_all_location,export");
		menu("jadwal_inbound", "Jadwal Inbound", "/ebisnis/gudang/inbound", "pergudangan", 720, "", "view,create,update,cancel");
		menu("penerimaan_qc", "Penerimaan Fisik & QC", "/ebisnis/gudang/receipt", "pergudangan", 730, "", "view,create,submit,approve,reject,cancel");
		menu("putaway", "Putaway", "/ebisnis/gudang/putaway", "pergudangan", 740, "", "view,create,submit,cancel");
		menu("persediaan_lokasi", "Persediaan per Gudang/Zona/Bin", "/ebisnis/gudang/persediaan", "pergudangan", 750, "", "view,view_all_location,view_cost,export");
		menu("lot_fefo_karantina", "Batch/FEFO/Kedaluwarsa/Karantina", "/ebisnis/gudang/lot", "pergudangan", 760, "kedaluwarsa", "view,update,approve,export");
		menu("reservasi_alokasi", "Reservasi & Alokasi", "/ebisnis/gudang/reservasi", "pergudangan", 770, "", "view,create,cancel,view_all_location");
		menu("picking", "Picking", "/ebisnis/gudang/picking", "pergudangan", 780, "", "view,create,submit,cancel");
		menu("packing", "Packing", "/ebisnis/gudang/packing", "pergudangan", 790, "", "view,create,submit,cancel");
		menu("stok_opname", "Stok Opname/Cycle Count", "/ebisnis/gudang/stok-opname", "pergudangan", 800, "stokopname", "view,create,submit,approve,reject,post,reverse,export");
		menu("penyesuaian_stok", "Penyesuaian Stok", "/ebisnis/gudang/penyesuaian", "pergudangan", 810, "", "view,create,submit,approve,post,reverse,export");
		menu("retur_gudang", "Retur Gudang", "/ebisnis/gudang/retur", "pergudangan", 820, "", "view,create,submit,approve,cancel,export");

		grup("distribusi_pengiriman", "Distribusi & Pengiriman", 900);
		menu("transfer_antar_lokasi", "Transfer Antar Lokasi", "/ebisnis/distribusi/transfer", "distribusi_pengiriman", 910, "mutasiantaroutlet,mutasi_antar_outlet", "view,create,edit_draft,submit,approve,reject,cancel,export");
		menu("delivery_order", "Delivery Order", "/ebisnis/distribusi/do", "distribusi_pengiriman", 920, "", "view,create,edit_draft,submit,approve,cancel,export");
		menu("freight_order", "Freight Order/Rute/Muatan", "/ebisnis/distribusi/freight", "distribusi_pengiriman", 930, "", "view,create,edit_draft,submit,approve,cancel,export,view_cost");
		menu("shipment_tracking", "Shipment & Tracking", "/ebisnis/distribusi/shipment", "distribusi_pengiriman", 940, "pengiriman_gudang", "view,create,update,cancel,export");
		menu("proof_of_delivery", "Proof of Delivery", "/ebisnis/distribusi/pod", "distribusi_pengiriman", 950, "", "view,create,approve,reject,export");
		menu("penerimaan_transfer_outlet", "Penerimaan Transfer Outlet", "/ebisnis/distribusi/penerimaan-outlet", "distribusi_pengiriman", 960, "", "view,create,submit,approve,reject,export");
		menu("klaim_distribusi", "Selisih/Kerusakan/Klaim", "/ebisnis/distribusi/klaim", "distribusi_pengiriman", 970, "", "view,create,submit,approve,reject,cancel,export");
		menu("reverse_logistics", "Retur & Reverse Logistics", "/ebisnis/distribusi/retur", "distribusi_pengiriman", 980, "", "view,create,submit,approve,cancel,reverse,export");

		grup("produksi", "Produksi", 1000);
		menu("bom_resep", "Formula/BOM/Resep", "/ebisnis/produksi/bom", "produksi", 1010, "", "view,create,update,approve,view_cost,export");
		menu("rencana_produksi", "Rencana Produksi", "/ebisnis/produksi/rencana", "produksi", 1020, "", "view,create,edit_draft,submit,approve,reject,cancel");
		menu("production_order", "Work/Production Order", "/ebisnis/produksi/order", "produksi", 1030, "produksi_outlet", "view,create,edit_draft,submit,approve,reject,cancel,export");
		menu("material_issue", "Material Issue", "/ebisnis/produksi/material-issue", "produksi", 1040, "", "view,create,submit,post,reverse");
		menu("work_in_process", "Barang Dalam Proses (WIP)", "/ebisnis/produksi/wip", "produksi", 1050, "pengadaan_bdp,barang_dalam_proses", "view,export,view_cost");
		menu("finished_goods_receipt", "Hasil Produksi/Finished Goods", "/ebisnis/produksi/hasil", "produksi", 1060, "", "view,create,submit,post,reverse");
		menu("waste_yield_variance", "Waste/Yield/Selisih", "/ebisnis/produksi/variance", "produksi", 1070, "", "view,create,approve,post,reverse,export,view_cost");

		grup("keuangan", "Keuangan", 1100);
		menu("ap_tagihan_vendor", "AP & Tagihan Vendor", "/ebisnis/keuangan/ap", "keuangan", 1110, "pengadaan_tagihan,terima_tagihan_vendor", "view,create,edit_draft,submit,approve,reject,cancel,post,reverse,export,view_cost");
		menu("pembayaran_vendor", "Pembayaran Vendor", "/ebisnis/keuangan/pembayaran-vendor", "keuangan", 1120, "pengadaan_dpc", "view,create,edit_draft,submit,approve,reject,cancel,post,reverse,export");
		menu("piutang_pelanggan", "Piutang Pelanggan (AR)", "/ebisnis/keuangan/ar", "keuangan", 1130, "", "view,create,update,post,reverse,export");
		menu("uang_muka_pj", "Uang Muka & Pertanggungjawaban", "/ebisnis/keuangan/uang-muka", "keuangan", 1140, "", "view,create,submit,approve,reject,cancel,post,reverse,export");
		menu("kas_besar", "Kas Besar", "/ebisnis/keuangan/kas-besar", "keuangan", 1150, "", "view,create,submit,approve,post,reverse,export");
		menu("kas_kecil", "Kas Kecil & Replenishment", "/ebisnis/keuangan/kas-kecil", "keuangan", 1160, "", "view,create,submit,approve,post,reverse,export");
		menu("dana_talangan", "Dana Talangan", "/ebisnis/keuangan/dana-talangan", "keuangan", 1170, "", "view,create,submit,approve,post,reverse,export");
		menu("reimbursement", "Reimbursement", "/ebisnis/keuangan/reimbursement", "keuangan", 1180, "", "view,create,submit,approve,reject,post,reverse,export");
		menu("pembayaran_transfer", "Pembayaran & Transfer", "/ebisnis/keuangan/transfer", "keuangan", 1190, "", "view,create,submit,approve,reject,post,reverse,export");
		menu("pajak", "Pajak", "/ebisnis/keuangan/pajak", "keuangan", 1200, "pengadaan_pajak", "view,create,submit,approve,post,reverse,export");

		grup("akuntansi", "Akuntansi", 1300);
		menu("bagan_akun", "Bagan Akun", "/ebisnis/akuntansi/akun", "akuntansi", 1310, "", "view,create,update,delete,export");
		menu("jenis_transaksi", "Jenis Transaksi", "/ebisnis/akuntansi/jenis-transaksi", "akuntansi", 1320, "", "view,create,update,delete");
		menu("draft_jurnal", "Draft Jurnal", "/ebisnis/akuntansi/draft-jurnal", "akuntansi", 1330, "", "view,create,edit_draft,submit,approve,reject,cancel,export");
		menu("jurnal_umum", "Jurnal Umum", "/ebisnis/akuntansi/jurnal", "akuntansi", 1340, "", "view,create,post,reverse,export");
		menu("posting_otomatis", "Posting Otomatis", "/ebisnis/akuntansi/posting", "akuntansi", 1350, "", "view,post,reverse,export");
		menu("saldo_awal", "Saldo Awal", "/ebisnis/akuntansi/saldo-awal", "akuntansi", 1360, "", "view,create,submit,approve,post,reverse,export");
		menu("penyesuaian_akuntansi", "Penyesuaian", "/ebisnis/akuntansi/penyesuaian", "akuntansi", 1370, "", "view,create,submit,approve,post,reverse,export");
		menu("tutup_buku", "Tutup Buku", "/ebisnis/akuntansi/tutup-buku", "akuntansi", 1380, "", "view,submit,approve,post,reverse,export");

		grup("laporan", "Laporan", 1400);
		menu("laporan_penjualan", "Laporan Penjualan", "/ebisnis/laporan/penjualan", "laporan", 1410, "riwayatpenjualan,laporantransaksi", "view,export");
		menu("laporan_pengadaan", "Laporan Pengadaan", "/ebisnis/laporan/pengadaan", "laporan", 1420, "", "view,export,view_cost");
		menu("laporan_pergudangan", "Laporan Persediaan/Pergudangan", "/ebisnis/laporan/pergudangan", "laporan", 1430, "", "view,export,view_cost,view_all_location");
		menu("laporan_distribusi", "Laporan Distribusi/Pengiriman", "/ebisnis/laporan/distribusi", "laporan", 1440, "", "view,export,view_all_location");
		menu("laporan_produksi", "Laporan Produksi", "/ebisnis/laporan/produksi", "laporan", 1450, "", "view,export,view_cost");
		menu("laporan_keuangan", "Laporan Keuangan", "/ebisnis/laporan/keuangan", "laporan", 1460, "laporankeuangan", "view,export,view_cost,view_all_location");
		menu("laporan_akuntansi", "Laporan Akuntansi", "/ebisnis/laporan/akuntansi", "laporan", 1470, "", "view,export");
		menu("audit_trail", "Audit Trail", "/ebisnis/laporan/audit", "laporan", 1480, "", "view,export,view_all_location");

		grup("sistem", "Sistem", 1500);
		menu("konfigurasi", "Konfigurasi", "/ebisnis/konfigurasi", "sistem", 1510, "", "view,update");
		menu("hak_akses", "Hak Akses", "/ebisnis/hak-akses", "sistem", 1520, "", "view,create,update,delete,export");
		menu("riwayat_sinkronisasi", "Riwayat Sinkronisasi", "/ebisnis/riwayat-sinkronisasi", "sistem", 1530, "riwayatsinkronisasi", "view,export");
		menu("log_error", "Log Error", "/ebisnis/log-error", "sistem", 1540, "", "view,export");
		menu("audit_aktivitas", "Audit Aktivitas", "/ebisnis/audit-aktivitas", "sistem", 1550, "", "view,export,view_all_location");

		validasi();
	}

	private EbisnisMenuBlueprintRegistry() {
	}

	private static void grup(String kunci, String label, int urutan) {
		tambah(new Entri(kunci, label, "", "", urutan,
				"desktop,android,jsp,zkoss", "", "view", "", "", ""));
	}

	private static void menu(String kunci, String label, String route, String parent,
			int urutan, String alias, String aksi) {
		tambah(new Entri(kunci, label, route, parent, urutan,
				"desktop,android,jsp,zkoss", alias, aksi,
				"menu_" + normalisasi(kunci), "", ""));
	}

	private static void tambah(Entri entri) {
		if (ENTRI.containsKey(entri.menuKey)) {
			throw new IllegalStateException("Menu key ganda: " + entri.menuKey);
		}
		ENTRI.put(entri.menuKey, entri);
		simpanAlias(entri.menuKey, entri.menuKey);
		for (int i = 0; i < entri.legacyAliases.size(); i++) {
			simpanAlias(entri.legacyAliases.get(i), entri.menuKey);
		}
	}

	private static void simpanAlias(String alias, String kanonik) {
		String kunci = normalisasi(alias);
		String lama = ALIAS.get(kunci);
		if (lama != null && !lama.equals(kanonik)) {
			throw new IllegalStateException("Alias menu bentrok: " + kunci
					+ " -> " + lama + " / " + kanonik);
		}
		ALIAS.put(kunci, kanonik);
	}

	public static void validasi() {
		Set<String> route = new LinkedHashSet<String>();
		for (Entri entri : ENTRI.values()) {
			if (entri.parentKey.length() > 0 && !ENTRI.containsKey(entri.parentKey)) {
				throw new IllegalStateException("Parent menu tidak dikenal: " + entri.parentKey);
			}
			if (entri.canonicalRoute.length() > 0 && !route.add(entri.canonicalRoute)) {
				throw new IllegalStateException("Canonical route ganda: " + entri.canonicalRoute);
			}
			for (int i = 0; i < entri.requiredActions.size(); i++) {
				String aksi = entri.requiredActions.get(i);
				if (!EbisnisMenuActionRegistry.aksiTerdaftar(aksi)) {
					throw new IllegalStateException("Aksi belum terdaftar: " + aksi
							+ " pada " + entri.menuKey);
				}
			}
		}
	}

	public static String kanonik(String kunciAtauAlias) {
		String hasil = ALIAS.get(normalisasi(kunciAtauAlias));
		return hasil == null ? normalisasi(kunciAtauAlias) : hasil;
	}

	public static Entri dapatkan(String kunciAtauAlias) {
		return ENTRI.get(kanonik(kunciAtauAlias));
	}

	public static List<Entri> semua() {
		return Collections.unmodifiableList(new ArrayList<Entri>(ENTRI.values()));
	}

	public static List<Entri> anak(String parentKey) {
		String parent = kanonik(parentKey);
		List<Entri> hasil = new ArrayList<Entri>();
		for (Entri entri : ENTRI.values()) {
			if (parent.equals(entri.parentKey)) {
				hasil.add(entri);
			}
		}
		return Collections.unmodifiableList(hasil);
	}

	private static List<String> daftarNilai(String nilai) {
		List<String> hasil = new ArrayList<String>();
		if (nilai != null && nilai.trim().length() > 0) {
			String[] bagian = nilai.split(",");
			for (int i = 0; i < bagian.length; i++) {
				String item = bagian[i].trim();
				if (item.length() > 0 && !hasil.contains(item)) {
					hasil.add(item);
				}
			}
		}
		return Collections.unmodifiableList(hasil);
	}

	private static List<String> daftarNilaiNormal(String nilai) {
		List<String> mentah = daftarNilai(nilai);
		List<String> hasil = new ArrayList<String>();
		for (int i = 0; i < mentah.size(); i++) {
			String item = normalisasi(mentah.get(i));
			if (item.length() > 0 && !hasil.contains(item)) {
				hasil.add(item);
			}
		}
		return Collections.unmodifiableList(hasil);
	}

	private static String normalisasi(String nilai) {
		if (nilai == null) {
			return "";
		}
		String hasil = nilai.trim().toLowerCase();
		hasil = hasil.replace('-', '_').replace(' ', '_').replace('/', '_');
		while (hasil.indexOf("__") >= 0) {
			hasil = hasil.replace("__", "_");
		}
		return hasil;
	}
}
