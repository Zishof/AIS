package ais.service.tenant;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * <h3>Kewenangan peran di dalam tenant (P8 &sect;16).</h3>
 *
 * <p>Lapisan ini <b>menambah</b>, tidak menggantikan. Setiap aksi {@code si_*} harus lolos
 * seluruhnya: keanggotaan tenant, status tenant, entitlement modul, tipe aktor, <b>peran
 * tenant (di sini)</b>, izin menu, izin CRUD, lalu lingkup toko/gudang/sales. Satu pun gagal
 * berarti ditolak.</p>
 *
 * <h4>Diturunkan dari nama aksi, bukan didaftar satu per satu</h4>
 * <p>Ada 112 aksi {@code si_*} dan jumlahnya bertambah. Mendaftarnya satu per satu berarti
 * setiap aksi baru harus diingat seseorang — dan yang lupa tidak menimbulkan galat, hanya
 * lubang izin yang diam. Karena itu <b>area</b> diturunkan dari awalan nama dan <b>sifat</b>
 * dari akhirannya, sehingga aksi baru tercakup sejak lahir.</p>
 *
 * <p>Konsekuensinya: aksi yang namanya di luar kesepakatan <b>tidak terpetakan</b>, dan
 * fail-closed &sect;12.4 menolaknya. Itu disengaja — lebih baik satu aksi baru berhenti
 * terang-terangan daripada diam-diam terbuka untuk semua peran.
 * {@code TenantRbacSelfTest} menjaga agar tidak ada aksi yang tertinggal.</p>
 *
 * <h4>Pengguna tanpa tenant tidak tersentuh sama sekali</h4>
 * <p>{@link #boleh} mengembalikan {@code true} ketika konteksnya {@code null} — keadaan
 * seluruh pengguna hari ini ({@code tbmuser.pendaftar == null}). Lapisan izin lama
 * ({@code ActorContext.bolehAksi}) tetap satu-satunya penentu bagi mereka. Menjadikan lapisan
 * ini wajib untuk semua orang akan memutus setiap pengguna yang ada.</p>
 */
public final class TenantRbac {

	// ------------------------------------------------------------------ peran

	public static final String OWNER = "OWNER";
	public static final String PEMILIK_SALES_INVENTORY = "PEMILIK_SALES_INVENTORY";
	public static final String ADMIN_TENANT = "ADMIN_TENANT";
	public static final String GUDANG = "GUDANG";
	public static final String PEMBELIAN = "PEMBELIAN";
	public static final String SALES_KELILING = "SALES_KELILING";
	public static final String KEUANGAN = "KEUANGAN";
	public static final String AUDITOR = "AUDITOR";

	/** Delapan peran minimum &sect;16, urut sesuai dokumen. */
	public static final String[] PERAN = { OWNER, PEMILIK_SALES_INVENTORY, ADMIN_TENANT,
			GUDANG, PEMBELIAN, SALES_KELILING, KEUANGAN, AUDITOR };

	// ------------------------------------------------------------------ sifat

	/** Membaca: daftar, rincian, laporan, riwayat. */
	public static final String BACA = "baca";
	/** Menulis: membuat, mengubah, menonaktifkan. */
	public static final String TULIS = "tulis";
	/** Menyetujui, memposting, membalik, mencetak ulang — tindakan berisiko. */
	public static final String SETUJU = "setuju";

	// ------------------------------------------------------------------- area

	public static final String AREA_MITRA = "mitra";        // supplier, customer, sales
	public static final String AREA_PRODUK = "produk";      // produk, kategori, satuan
	public static final String AREA_STOK = "stok";          // persediaan, opname, mutasi
	public static final String AREA_HARGA = "harga";        // harga beli/jual, analisis
	public static final String AREA_BELI = "beli";          // pembelian
	public static final String AREA_HUTANG = "hutang";      // AP
	public static final String AREA_JUAL = "jual";          // penjualan, sales order
	public static final String AREA_PIUTANG = "piutang";    // AR, penagihan
	public static final String AREA_TRIP = "trip";          // sales keliling, SPJ, nota
	public static final String AREA_KEUANGAN = "keuangan";  // kas, jurnal, akun, biaya
	public static final String AREA_LAPORAN = "laporan";    // laba rugi, laba kotor
	public static final String AREA_IMPOR = "impor";        // impor legacy
	public static final String AREA_UMUM = "umum";          // konteks aktor, audit, cetak

	private TenantRbac() {
	}

	// ================================================================ matriks

	/** peran -> area -> sifat yang diizinkan. */
	private static final Map<String, Map<String, Set<String>>> MATRIKS =
			new HashMap<String, Map<String, Set<String>>>();

	/**
	 * Tambahkan {@code sifat} (baca/tulis/setuju) yang diizinkan bagi {@code peran} pada
	 * {@code area} tertentu. Dipanggil berulang kali di blok inisialisasi statis di bawah untuk
	 * membangun {@link #MATRIKS}; peta per-peran dan set per-area dibuat lazy saat pertama
	 * disentuh.
	 *
	 * @param peran kode peran, mis. {@link #OWNER}
	 * @param area  kode area, salah satu konstanta {@code AREA_*}
	 * @param sifat satu atau lebih dari {@link #BACA}, {@link #TULIS}, {@link #SETUJU}
	 */
	private static void beri(String peran, String area, String... sifat) {
		Map<String, Set<String>> perArea = MATRIKS.get(peran);
		if (perArea == null) {
			perArea = new HashMap<String, Set<String>>();
			MATRIKS.put(peran, perArea);
		}
		Set<String> s = perArea.get(area);
		if (s == null) {
			s = new HashSet<String>();
			perArea.put(area, s);
		}
		for (int i = 0; i < sifat.length; i++) {
			s.add(sifat[i]);
		}
	}

	private static final String[] SEMUA_AREA = { AREA_MITRA, AREA_PRODUK, AREA_STOK, AREA_HARGA,
			AREA_BELI, AREA_HUTANG, AREA_JUAL, AREA_PIUTANG, AREA_TRIP, AREA_KEUANGAN,
			AREA_LAPORAN, AREA_IMPOR, AREA_UMUM };

	static {
		// OWNER dan PEMILIK_SALES_INVENTORY: seluruhnya.
		for (int i = 0; i < SEMUA_AREA.length; i++) {
			beri(OWNER, SEMUA_AREA[i], BACA, TULIS, SETUJU);
			beri(PEMILIK_SALES_INVENTORY, SEMUA_AREA[i], BACA, TULIS, SETUJU);
		}

		// ADMIN_TENANT: seluruhnya KECUALI impor legacy.
		//
		// Impor menulis puluhan ribu baris sekaligus dan tidak dapat dibatalkan
		// sebagian; ia milik pemilik usaha, bukan administrator hariannya.
		for (int i = 0; i < SEMUA_AREA.length; i++) {
			if (AREA_IMPOR.equals(SEMUA_AREA[i])) {
				continue;
			}
			beri(ADMIN_TENANT, SEMUA_AREA[i], BACA, TULIS, SETUJU);
		}

		// GUDANG: barang, bukan uang.
		beri(GUDANG, AREA_STOK, BACA, TULIS, SETUJU);
		beri(GUDANG, AREA_PRODUK, BACA, TULIS);
		beri(GUDANG, AREA_BELI, BACA);
		beri(GUDANG, AREA_JUAL, BACA);
		beri(GUDANG, AREA_MITRA, BACA);
		beri(GUDANG, AREA_UMUM, BACA);

		// PEMBELIAN: sisi pemasok, termasuk hutangnya.
		beri(PEMBELIAN, AREA_BELI, BACA, TULIS, SETUJU);
		beri(PEMBELIAN, AREA_HUTANG, BACA, TULIS);
		beri(PEMBELIAN, AREA_MITRA, BACA, TULIS);
		beri(PEMBELIAN, AREA_HARGA, BACA, TULIS);
		beri(PEMBELIAN, AREA_PRODUK, BACA);
		beri(PEMBELIAN, AREA_STOK, BACA);
		beri(PEMBELIAN, AREA_LAPORAN, BACA);
		beri(PEMBELIAN, AREA_UMUM, BACA);

		// SALES_KELILING: perjalanan dan notanya. TIDAK boleh mengubah harga --
		// itu justru celah yang paling mudah disalahgunakan di lapangan.
		beri(SALES_KELILING, AREA_TRIP, BACA, TULIS);
		beri(SALES_KELILING, AREA_JUAL, BACA, TULIS);
		beri(SALES_KELILING, AREA_PIUTANG, BACA, TULIS);
		beri(SALES_KELILING, AREA_MITRA, BACA);
		beri(SALES_KELILING, AREA_PRODUK, BACA);
		beri(SALES_KELILING, AREA_HARGA, BACA);
		beri(SALES_KELILING, AREA_STOK, BACA);
		beri(SALES_KELILING, AREA_UMUM, BACA);

		// KEUANGAN: uang dan buku besar, termasuk memposting.
		beri(KEUANGAN, AREA_KEUANGAN, BACA, TULIS, SETUJU);
		beri(KEUANGAN, AREA_HUTANG, BACA, TULIS, SETUJU);
		beri(KEUANGAN, AREA_PIUTANG, BACA, TULIS, SETUJU);
		beri(KEUANGAN, AREA_LAPORAN, BACA, SETUJU);
		beri(KEUANGAN, AREA_BELI, BACA);
		beri(KEUANGAN, AREA_JUAL, BACA);
		beri(KEUANGAN, AREA_TRIP, BACA);
		beri(KEUANGAN, AREA_MITRA, BACA);
		beri(KEUANGAN, AREA_HARGA, BACA);
		beri(KEUANGAN, AREA_UMUM, BACA);

		// AUDITOR: MEMBACA SAJA, seluruh area. Tidak ada satu pun tulis maupun
		// setuju -- kalau auditor dapat mengubah, auditnya kehilangan artinya.
		for (int i = 0; i < SEMUA_AREA.length; i++) {
			beri(AUDITOR, SEMUA_AREA[i], BACA);
		}
	}

	// ============================================================== pemetaan

	/**
	 * Area sebuah aksi {@code si_*}, atau {@code null} bila namanya belum terpetakan.
	 *
	 * <p>Urutan pemeriksaan penting: awalan yang lebih spesifik didahulukan
	 * ({@code si_supplier_price_} sebelum {@code si_supplier_}), persis seperti gerbang menu
	 * di {@code PosApi}.</p>
	 */
	public static String area(String aksi) {
		if (aksi == null || !aksi.startsWith("si_")) {
			return null;
		}
		String a = aksi.substring(3);

		// --- lebih spesifik lebih dulu ---
		if (a.startsWith("supplier_price") || a.startsWith("customer_price")
				|| a.startsWith("price")) {
			return AREA_HARGA;
		}
		if (a.startsWith("payable")) {
			return AREA_HUTANG;
		}
		if (a.startsWith("receivable") || a.startsWith("collection")) {
			return AREA_PIUTANG;
		}
		if (a.startsWith("sales_order")) {
			return AREA_JUAL;
		}
		if (a.startsWith("supplier") || a.startsWith("customer") || a.startsWith("sales")) {
			return AREA_MITRA;
		}
		if (a.startsWith("trip") || a.startsWith("spj") || a.startsWith("nota")) {
			return AREA_TRIP;
		}
		if (a.startsWith("purchase")) {
			return AREA_BELI;
		}
		if (a.startsWith("inventory") || a.startsWith("stok") || a.startsWith("opname")) {
			return AREA_STOK;
		}
		if (a.startsWith("produk") || a.startsWith("product") || a.startsWith("item")) {
			return AREA_PRODUK;
		}
		if (a.startsWith("cash_journal") || a.startsWith("coa") || a.startsWith("expense")
				|| a.startsWith("journal")) {
			return AREA_KEUANGAN;
		}
		if (a.startsWith("profit_loss") || a.startsWith("gross_profit") || a.startsWith("report")) {
			return AREA_LAPORAN;
		}
		if (a.startsWith("import")) {
			return AREA_IMPOR;
		}
		if (a.startsWith("actor_context") || a.startsWith("audit") || a.startsWith("print_log")) {
			return AREA_UMUM;
		}
		return null;
	}

	/**
	 * Sifat sebuah aksi. Bawaannya {@link #BACA} — akhiran yang tidak dikenal diperlakukan
	 * sebagai membaca, sebab hampir seluruh aksi baca memakai akhiran yang beragam
	 * ({@code _aging_customer}, {@code _balance}, {@code _params}) sedangkan aksi tulis
	 * memakai kata kerja yang sedikit dan jelas.
	 */
	public static String sifat(String aksi) {
		if (aksi == null) {
			return BACA;
		}
		if (aksi.endsWith("_reverse") || aksi.endsWith("_approve") || aksi.endsWith("_posting")
				|| aksi.endsWith("_post") || aksi.endsWith("_reprint") || aksi.endsWith("_export")
				|| aksi.endsWith("_void") || aksi.endsWith("_batal")) {
			return SETUJU;
		}
		if (aksi.endsWith("_create") || aksi.endsWith("_update") || aksi.endsWith("_save")
				|| aksi.endsWith("_delete") || aksi.endsWith("_deactivate")
				|| aksi.endsWith("_import_legacy") || aksi.equals("si_import_legacy")) {
			return TULIS;
		}
		return BACA;
	}

	// ================================================================ gerbang

	/**
	 * Boleh atau tidak aktor menjalankan aksi ini menurut peran tenantnya.
	 *
	 * <p>{@code true} bila {@code ctx} {@code null} — pengguna tanpa tenant tidak melewati
	 * lapisan ini sama sekali; izinnya sepenuhnya ditentukan lapisan lama.</p>
	 */
	public static boolean boleh(TenantContext ctx, String aksi) {
		if (ctx == null) {
			return true;
		}
		String peran = ctx.getMembershipRole();
		if (peran == null || peran.trim().length() == 0) {
			// Anggota tenant tanpa peran tidak punya kewenangan apa pun. Fail-closed
			// §12.4; invariannya: setiap TenantMembership wajib punya roleCode.
			return false;
		}
		Map<String, Set<String>> perArea = MATRIKS.get(peran.trim().toUpperCase());
		if (perArea == null) {
			return false;
		}
		String area = area(aksi);
		if (area == null) {
			// Aksi belum terpetakan. Ditolak, bukan dibiarkan lewat.
			return false;
		}
		Set<String> sifatDiizinkan = perArea.get(area);
		return sifatDiizinkan != null && sifatDiizinkan.contains(sifat(aksi));
	}

	/**
	 * Alasan penolakan yang dapat dibaca pengguna. Sengaja <b>tidak</b> menyebut peran mana
	 * yang boleh — itu memberi tahu penyerang bentuk kewenangan di dalam tenant.
	 */
	public static String alasan(TenantContext ctx, String aksi) {
		String peran = ctx == null ? null : ctx.getMembershipRole();
		if (peran == null || peran.trim().length() == 0) {
			return "Peran Anda pada usaha ini belum ditetapkan. Hubungi admin.";
		}
		if (area(aksi) == null) {
			return "Aksi ini belum tersedia pada usaha ber-tenant.";
		}
		return "Peran Anda tidak berwenang melakukan tindakan ini.";
	}

	/** Benar bila peran itu salah satu dari delapan peran minimum &sect;16. */
	public static boolean peranDikenal(String peran) {
		return peran != null && MATRIKS.containsKey(peran.trim().toUpperCase());
	}
}
