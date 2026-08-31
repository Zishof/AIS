package ais.service.tenant.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ais.service.tenant.TenantContext;
import ais.service.tenant.TenantRbac;

/**
 * <h3>Penjaga kewenangan peran tenant (P8).</h3>
 *
 * <p>Tiga hal yang dijaga di sini, dan yang pertama adalah yang paling mudah membusuk.</p>
 *
 * <ol>
 * <li><b>Tidak ada aksi {@code si_*} yang tertinggal tanpa pemetaan area.</b> Daftarnya
 *     <b>dipindai dari sumber</b>, bukan ditulis ulang di sini — daftar salinan akan basi
 *     diam-diam begitu seseorang menambah aksi baru, dan itu persis kegagalan yang hendak
 *     dicegah.</li>
 * <li><b>AUDITOR tidak dapat menulis apa pun.</b> Auditor yang dapat mengubah kehilangan
 *     artinya sebagai auditor.</li>
 * <li><b>Pengguna tanpa tenant tidak tersentuh.</b> Itu keadaan seluruh pengguna hari ini.</li>
 * </ol>
 *
 * <p>Jalankan dari {@code C:\opt\AIS\ais\src\main}:
 * {@code java ais.service.tenant.test.TenantRbacSelfTest}</p>
 */
public final class TenantRbacSelfTest {

	/** Kelas utilitas murni statis — tidak pernah diinstansiasi. */
	private TenantRbacSelfTest() {
	}

	/** Penghitung kegagalan lintas kelima blok uji; bukan JUnit sehingga dikelola manual. */
	private static int gagal;

	/**
	 * Catat satu kegagalan: cetak baris {@code GAGAL: <pesan>} ke {@code System.out} dan
	 * naikkan {@link #gagal}. Dipanggil langsung untuk kegagalan yang tidak berbentuk
	 * pemeriksaan boolean sederhana, dan secara internal oleh {@link #benar(boolean, String)}.
	 *
	 * @param pesan penjelasan kegagalan, ditulis apa adanya ke keluaran konsol
	 */
	private static void salah(String pesan) {
		System.out.println("  GAGAL: " + pesan);
		gagal++;
	}

	/**
	 * Assersi manual: catat kegagalan lewat {@link #salah(String)} bila {@code syarat}
	 * bernilai {@code false}. Pengganti {@code assertTrue} JUnit pada harness ini — tidak
	 * menghentikan eksekusi saat gagal, hanya menambah hitungan {@link #gagal}.
	 *
	 * @param syarat kondisi yang diharapkan benar
	 * @param pesan  penjelasan yang dicetak bila {@code syarat} salah
	 */
	private static void benar(boolean syarat, String pesan) {
		if (!syarat) {
			salah(pesan);
		}
	}

	/**
	 * Titik masuk harness manual (bukan JUnit) untuk penjaga RBAC tenant. Jalankan dari
	 * {@code C:\opt\AIS\ais\src\main} dengan {@code java ais.service.tenant.test.TenantRbacSelfTest}
	 * — direktori kerja itu WAJIB karena {@link #ujiSeluruhAksiTerpetakan()} memindai berkas
	 * sumber dispatcher lewat path relatif (lihat {@link #cariSumber()}). Menjalankan kelima
	 * blok uji berurutan ({@link #ujiSeluruhAksiTerpetakan()}, {@link #ujiAuditorHanyaMembaca()},
	 * {@link #ujiTanpaTenantTidakTersentuh()}, {@link #ujiPeranKosongDitolak()},
	 * {@link #ujiPemisahanKewenangan()}), lalu bila {@link #gagal} &gt; 0 melempar
	 * {@link IllegalStateException} berisi jumlah masalah; bila lolos, mencetak
	 * {@code "TenantRbacSelfTest OK"} dan memanggil {@code System.exit(0)} eksplisit.
	 *
	 * @param a tidak dipakai
	 * @throws Exception diteruskan dari {@link #ujiSeluruhAksiTerpetakan()} (mis. kegagalan
	 *                    membaca berkas sumber); {@link IllegalStateException} bila ada satu
	 *                    atau lebih pemeriksaan yang gagal
	 */
	public static void main(String[] a) throws Exception {
		ujiSeluruhAksiTerpetakan();
		ujiAuditorHanyaMembaca();
		ujiTanpaTenantTidakTersentuh();
		ujiPeranKosongDitolak();
		ujiPemisahanKewenangan();
		if (gagal > 0) {
			throw new IllegalStateException(gagal + " masalah pada RBAC tenant.");
		}
		System.out.println("TenantRbacSelfTest OK");
		System.exit(0);
	}

	// ------------------------------------------------------------------ (1)

	/**
	 * Pindai seluruh literal {@code "si_..."} dari sumber dispatcher, lalu pastikan setiap
	 * aksi utuh punya area. Awalan penyaring (berakhir garis bawah, mis. {@code "si_audit_"})
	 * dilewati — itu pola pencocokan menu, bukan nama aksi.
	 */
	private static void ujiSeluruhAksiTerpetakan() throws Exception {
		File dir = cariSumber();
		if (dir == null) {
			salah("direktori sumber tidak ditemukan -- jalankan dari C:\\opt\\AIS\\ais\\src\\main"
					+ " supaya pemindaian aksi dapat berjalan. Uji ini TIDAK boleh dilewati diam-diam.");
			return;
		}
		TreeSet<String> aksi = new TreeSet<String>();
		kumpulkan(dir, aksi);
		File servlet = new File(dir.getParentFile(), "servlet");
		if (servlet.isDirectory()) {
			kumpulkan(servlet, aksi);
		}

		int utuh = 0;
		List<String> tanpaArea = new ArrayList<String>();
		for (String s : aksi) {
			if (s.endsWith("_")) {
				continue; // awalan penyaring menu
			}
			utuh++;
			if (TenantRbac.area(s) == null) {
				tanpaArea.add(s);
			}
		}
		System.out.println("  aksi si_* utuh dipindai: " + utuh);
		benar(utuh >= 80, "aksi terpindai hanya " + utuh + " -- pemindaian tampaknya gagal,"
				+ " bukan berarti aksinya berkurang");
		for (int i = 0; i < tanpaArea.size(); i++) {
			salah("aksi TIDAK terpetakan ke area RBAC: " + tanpaArea.get(i)
					+ " -- fail-closed akan menolaknya untuk SEMUA peran."
					+ " Tambahkan awalannya di TenantRbac.area().");
		}
	}

	/**
	 * Cari direktori sumber dispatcher aksi {@code si_*}, dicoba dari beberapa path relatif
	 * kandidat agar toleran terhadap variasi direktori kerja saat harness dijalankan (mis.
	 * dari {@code src/main} langsung atau dari satu tingkat di bawahnya).
	 *
	 * @return direktori kandidat pertama yang benar-benar ada, atau {@code null} bila tidak
	 *         satu pun ditemukan (pemanggil melaporkannya sebagai kegagalan keras, bukan
	 *         dilewati diam-diam)
	 */
	private static File cariSumber() {
		String[] kandidat = { "java/ais/action/servlet/api", "src/ais/action/servlet/api",
				"../java/ais/action/servlet/api" };
		for (int i = 0; i < kandidat.length; i++) {
			File f = new File(kandidat[i]);
			if (f.isDirectory()) {
				return f;
			}
		}
		return null;
	}

	/** Pola literal string {@code "si_..."} pada kode sumber — dasar pemindaian {@link #kumpulkan}. */
	private static final Pattern POLA_AKSI = Pattern.compile("\"(si_[a-z_]+)\"");

	/**
	 * Pindai seluruh berkas {@code .java} langsung di {@code dir} (tidak rekursif ke
	 * subdirektori) baris demi baris, kumpulkan setiap literal yang cocok {@link #POLA_AKSI}
	 * ke {@code keluar}. Dipakai {@link #ujiSeluruhAksiTerpetakan()} untuk membangun daftar
	 * aksi {@code si_*} secara langsung dari sumber, bukan salinan yang ditulis ulang di sini.
	 *
	 * @param dir    direktori yang dipindai; bila bukan direktori (atau tidak dapat
	 *               dilistir), method kembali tanpa efek
	 * @param keluar himpunan terurut tempat aksi yang ditemukan diakumulasikan (dipanggil
	 *               berulang untuk beberapa direktori sehingga bersifat akumulatif)
	 * @throws Exception diteruskan dari kegagalan membaca berkas (mis. {@code IOException})
	 */
	private static void kumpulkan(File dir, TreeSet<String> keluar) throws Exception {
		File[] isi = dir.listFiles();
		if (isi == null) {
			return;
		}
		for (int i = 0; i < isi.length; i++) {
			if (!isi[i].isFile() || !isi[i].getName().endsWith(".java")) {
				continue;
			}
			BufferedReader r = new BufferedReader(
					new InputStreamReader(new FileInputStream(isi[i]), "UTF-8"));
			try {
				String baris;
				while ((baris = r.readLine()) != null) {
					Matcher m = POLA_AKSI.matcher(baris);
					while (m.find()) {
						keluar.add(m.group(1));
					}
				}
			} finally {
				r.close();
			}
		}
	}

	// ------------------------------------------------------------------ (2)

	/**
	 * Blok (2): pastikan peran {@link TenantRbac#AUDITOR} dapat membaca daftar/laporan/riwayat
	 * audit, tetapi ditolak untuk SEMUA aksi tulis (create/update/deactivate/save/reverse)
	 * yang dicontohkan di sini — auditor yang dapat mengubah kehilangan artinya sebagai
	 * auditor (lihat javadoc kelas).
	 */
	private static void ujiAuditorHanyaMembaca() {
		TenantContext auditor = konteks(TenantRbac.AUDITOR);
		benar(TenantRbac.boleh(auditor, "si_customer_list"), "AUDITOR harus dapat membaca");
		benar(TenantRbac.boleh(auditor, "si_profit_loss_report"),
				"AUDITOR harus dapat membaca laporan");
		benar(TenantRbac.boleh(auditor, "si_audit_history"), "AUDITOR harus dapat membaca audit");

		String[] tulis = { "si_customer_create", "si_customer_update", "si_supplier_deactivate",
				"si_expense_create", "si_coa_save", "si_collection_create", "si_purchase_terms_save",
				"si_customer_price_save", "si_import_legacy" };
		for (int i = 0; i < tulis.length; i++) {
			benar(!TenantRbac.boleh(auditor, tulis[i]),
					"AUDITOR TIDAK boleh menulis: " + tulis[i]
							+ " -- auditor yang dapat mengubah kehilangan artinya");
		}
		String[] setuju = { "si_collection_reverse", "si_expense_reverse",
				"si_payable_payment_reverse" };
		for (int i = 0; i < setuju.length; i++) {
			benar(!TenantRbac.boleh(auditor, setuju[i]),
					"AUDITOR TIDAK boleh membalik: " + setuju[i]);
		}
	}

	// ------------------------------------------------------------------ (3)

	/**
	 * Blok (3): pastikan konteks {@code null} (pengguna tanpa tenant, keadaan seluruh
	 * pengguna hari ini) tetap LEWAT untuk aksi apa pun — RBAC tenant ini adalah lapisan
	 * TAMBAHAN yang hanya aktif untuk anggota tenant; izin pengguna non-tenant tetap
	 * ditentukan lapisan lama yang sudah ada, tidak boleh tiba-tiba diblokir oleh lapisan ini.
	 */
	private static void ujiTanpaTenantTidakTersentuh() {
		benar(TenantRbac.boleh(null, "si_customer_create"),
				"konteks null (pengguna tanpa tenant) harus LEWAT -- itu keadaan seluruh"
						+ " pengguna hari ini, dan izinnya ditentukan lapisan lama");
		benar(TenantRbac.boleh(null, "si_import_legacy"),
				"konteks null harus lewat untuk aksi apa pun pada lapisan ini");
	}

	/**
	 * Blok (3, lanjutan): anggota tenant TANPA peran (string kosong) atau dengan peran yang
	 * tidak dikenal harus ditolak fail-closed (§12.4) — bukan diperlakukan sebagai "boleh
	 * segalanya" atau "abaikan saja". Juga memeriksa {@code peranDikenal} abai huruf
	 * besar-kecil dan jumlah peran minimum ({@link TenantRbac#PERAN} — delapan peran §16).
	 */
	private static void ujiPeranKosongDitolak() {
		benar(!TenantRbac.boleh(konteks(""), "si_customer_list"),
				"anggota tenant TANPA peran harus ditolak (fail-closed §12.4)");
		benar(!TenantRbac.boleh(konteks("PERAN_KARANGAN"), "si_customer_list"),
				"peran yang tidak dikenal harus ditolak, bukan diterima");
		benar(TenantRbac.peranDikenal("owner"), "peranDikenal harus abai huruf besar-kecil");
		benar(!TenantRbac.peranDikenal("PERAN_KARANGAN"), "peran karangan tidak boleh dikenal");
		benar(TenantRbac.PERAN.length == 8, "delapan peran minimum §16");
	}

	// ------------------------------------------------------------------ (4)

	/** Pemisahan kewenangan yang paling mudah disalahgunakan bila longgar. */
	private static void ujiPemisahanKewenangan() {
		TenantContext sales = konteks(TenantRbac.SALES_KELILING);
		benar(TenantRbac.boleh(sales, "si_trip_start"), "SALES_KELILING harus dapat memulai trip");
		benar(!TenantRbac.boleh(sales, "si_customer_price_save"),
				"SALES_KELILING TIDAK boleh mengubah harga -- celah yang paling mudah"
						+ " disalahgunakan di lapangan");
		benar(!TenantRbac.boleh(sales, "si_coa_save"),
				"SALES_KELILING tidak berurusan dengan bagan akun");

		TenantContext gudang = konteks(TenantRbac.GUDANG);
		benar(TenantRbac.boleh(gudang, "si_inventory_balance"), "GUDANG membaca stok");
		benar(!TenantRbac.boleh(gudang, "si_payable_payment_create"),
				"GUDANG mengurus barang, bukan uang");
		benar(!TenantRbac.boleh(gudang, "si_customer_price_save"),
				"GUDANG tidak menentukan harga");

		TenantContext beli = konteks(TenantRbac.PEMBELIAN);
		benar(TenantRbac.boleh(beli, "si_supplier_create"), "PEMBELIAN mengelola pemasok");
		benar(!TenantRbac.boleh(beli, "si_collection_create"),
				"PEMBELIAN tidak menagih piutang");

		TenantContext keuangan = konteks(TenantRbac.KEUANGAN);
		benar(TenantRbac.boleh(keuangan, "si_coa_save"), "KEUANGAN mengelola bagan akun");
		benar(TenantRbac.boleh(keuangan, "si_payable_payment_reverse"),
				"KEUANGAN dapat membalik pembayaran");
		benar(!TenantRbac.boleh(keuangan, "si_import_legacy"),
				"impor legacy bukan wewenang keuangan");

		TenantContext admin = konteks(TenantRbac.ADMIN_TENANT);
		benar(TenantRbac.boleh(admin, "si_customer_create"), "ADMIN_TENANT mengelola master");
		benar(!TenantRbac.boleh(admin, "si_import_legacy"),
				"impor menulis puluhan ribu baris dan tidak dapat dibatalkan sebagian --"
						+ " milik pemilik usaha, bukan administrator hariannya");

		TenantContext owner = konteks(TenantRbac.OWNER);
		benar(TenantRbac.boleh(owner, "si_import_legacy"), "OWNER boleh mengimpor");
		benar(TenantRbac.boleh(owner, "si_coa_save"), "OWNER boleh seluruhnya");
	}

	/**
	 * Bentuk {@link TenantContext} minimal ber-schema {@code tenant_uji}/
	 * {@code tenant_uji__audit} dengan satu peran keanggotaan tertentu — dipakai berulang
	 * oleh {@link #ujiAuditorHanyaMembaca()} dan {@link #ujiPemisahanKewenangan()} untuk
	 * menghindari duplikasi konstruksi builder di setiap kasus uji.
	 *
	 * @param peran nilai {@code membershipRole} yang disisipkan ke konteks, mis. salah satu
	 *              konstanta {@link TenantRbac} ({@code AUDITOR}, {@code OWNER}, dst.) atau
	 *              string sembarang untuk menguji peran yang tidak dikenal
	 * @return konteks tenant TENANT_ONLY siap pakai untuk {@link TenantRbac#boleh}
	 */
	private static TenantContext konteks(String peran) {
		return TenantContext.builder().tenantId(Long.valueOf(1)).tenantMode("TENANT_ONLY")
				.membershipRole(peran).schemaName("tenant_uji")
				.auditSchemaName("tenant_uji__audit").build();
	}
}
