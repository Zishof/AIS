package ais.action.servlet.api;

/**
 * <h3>Jalur schema tenant untuk Hutang Supplier (P4, helper ketiga).</h3>
 *
 * <p>Jalur legacy di {@link SalesInventoryPayableHelper} tidak diubah; kelas ini menyediakan
 * potongan SQL penggantinya dengan kolom keluaran yang sama dan berurutan sama, sehingga
 * perakitan JSON dipakai bersama.</p>
 *
 * <h4>Legacy menyimpan faktur, tenant menyimpan HUTANG</h4>
 * <p>Perbedaan bentuknya lebih dalam daripada penggantian nama. Legacy menaruh seluruh faktur
 * kulakan di {@code pengadaan_faktur}, lalu menempelkan {@code payable_faktur_info} 1:1 untuk
 * medan yang khusus hutang (termin, jatuh tempo, DP), dan menyaring mana yang berhutang lewat
 * {@code i.jenis_pembayaran IN ('DP','CREDIT')}.</p>
 * <p>Model tenant memisahkannya: {@code pembelian} adalah dokumen pembeliannya, sedangkan
 * {@code hutang_supplier} <b>hanya berisi yang benar-benar berhutang</b>. Tabel sisi
 * {@code payable_faktur_info} lenyap -- medannya sudah menyatu.</p>
 * <p>Akibatnya saringan jenis pembayaran <b>tidak diperlukan</b> di jalur tenant: setiap baris
 * di {@code hutang_supplier} sudah pasti hutang. Menyalin saringan itu ke sini justru salah,
 * sebab kolomnya tidak ada dan konsepnya sudah terwujud sebagai keberadaan barisnya.</p>
 *
 * <h4>Pemetaan</h4>
 * <table border="1">
 * <tr><th>Legacy</th><th>Tenant</th></tr>
 * <tr><td>{@code pengadaan_faktur f} + {@code payable_faktur_info i}</td><td>{@code hutang_supplier h}</td></tr>
 * <tr><td>{@code alokasi_pembayaran_hutang_supplier} ({@code a.nominal}, {@code a.pengadaan_faktur})</td>
 *     <td>{@code alokasi_pembayaran_hutang} ({@code a.nilai}, {@code a.hutang_supplier_id})</td></tr>
 * <tr><td>{@code pembayaran_hutang_supplier}</td><td>{@code pembayaran_hutang}</td></tr>
 * <tr><td>{@code library.penyedia}</td><td>{@code <schema>.supplier}</td></tr>
 * <tr><td>{@code f.total_faktur_manual} / {@code f.total_hitung_saat_simpan}</td><td>{@code h.nilai}</td></tr>
 * <tr><td>{@code i.dibayar_awal}</td><td>alokasi pembayaran biasa</td></tr>
 * <tr><td>{@code f.tanggal_faktur}</td><td>{@code h.tanggal}</td></tr>
 * <tr><td>{@code i.jatuh_tempo}</td><td>{@code h.jatuh_tempo}</td></tr>
 * </table>
 *
 * <h4>Sisa hutang dihitung dari alokasi, bukan dibaca dari kolom</h4>
 * <p>{@code hutang_supplier} menyediakan {@code terbayar} dan {@code sisa}. Jalur ini
 * <b>sengaja tidak memakainya</b> dan menghitung ulang dari {@code alokasi_pembayaran_hutang},
 * persis seperti legacy menghitung dari alokasinya.</p>
 * <p>Alasannya sama dengan {@code saldo_stok} pada model stok: kolom ringkasan adalah turunan
 * yang bisa basi, dan angka hutang yang basi berarti membayar dua kali atau menagih yang sudah
 * lunas. Menghitung dari alokasi membuat hasilnya selalu sesuai dengan pembayaran yang benar-benar
 * tercatat.</p>
 */
final class SalesInventoryPayableTenant {

	private SalesInventoryPayableTenant() {
	}

	/** Benar bila aktor ini dilayani schema tenant. */
	static boolean aktif(EbisnisActorContextResolver.ActorContext aktor) {
		return SalesInventoryTenantSchema.aktif(aktor);
	}

	/** Prefiks schema berikut titiknya. */
	static String skema(EbisnisActorContextResolver.ActorContext aktor) {
		return SalesInventoryTenantSchema.skema(aktor.tenant);
	}

	/** Nilai hutang menurut dokumennya. */
	static String total() {
		return "COALESCE(h.nilai,0)";
	}

	/** Jumlah seluruh alokasi pembayaran atas satu hutang. */
	static String alokasi(String skema) {
		return "COALESCE((SELECT SUM(a.nilai) FROM " + skema + "alokasi_pembayaran_hutang a"
				+ " WHERE a.hutang_supplier_id = h.id),0)";
	}

	/** Sisa yang belum dibayar. Dihitung, bukan dibaca dari {@code h.sisa}. */
	static String outstanding(String skema) {
		return "(" + total() + " - " + alokasi(skema) + ")";
	}

	// ------------------------------------------------------------------ daftar hutang

	/**
	 * Awal klausa {@code WHERE}. Legacy menyaring {@code jenis_pembayaran IN ('DP','CREDIT')};
	 * di sini tidak perlu, sebab tabelnya memang hanya memuat hutang.
	 */
	static String whereAwal() {
		return " WHERE 1=1 ";
	}

	/**
	 * Alamat pemasok berada di {@code supplier_profile}, bukan di {@code supplier}. {@code LEFT
	 * JOIN} karena profil boleh belum diisi -- pemasok tanpa profil tetap harus muncul di daftar
	 * hutang, bukan hilang begitu saja.
	 */
	static String dasarDaftar(String skema, String where) {
		return " FROM " + skema + "hutang_supplier h JOIN " + skema + "supplier s"
				+ " ON h.supplier_id = s.id LEFT JOIN " + skema + "supplier_profile sp"
				+ " ON sp.supplier_id = s.id " + where;
	}

	/**
	 * Kolom daftar hutang, berurutan sama dengan jalur legacy: id, nomorFaktur, tanggal,
	 * supplierId, supplierKode, supplierNama, alamat, jenisPembayaran, terminHari, jatuhTempo,
	 * total, dibayarAwal, alokasi, outstanding.
	 *
	 * <p>{@code jenisPembayaran} dan {@code terminHari} tidak ada pada model tenant. Kolomnya
	 * dipertahankan supaya bentuk JSON tidak berubah: jenis diisi {@code 'CREDIT'} -- itulah
	 * arti keberadaan baris hutang -- dan termin diturunkan dari selisih tanggal, yang memang
	 * definisinya.</p>
	 */
	static String selectDaftar(String skema) {
		return "SELECT h.id, COALESCE(h.nomor_faktur,''), h.tanggal, h.supplier_id, "
				+ "COALESCE(s.kode,''), COALESCE(s.nama,''), COALESCE(sp.alamat1,''), "
				+ "'CREDIT', "
				+ "CASE WHEN h.jatuh_tempo IS NULL OR h.tanggal IS NULL THEN NULL "
				+ "ELSE (h.jatuh_tempo - h.tanggal) END, h.jatuh_tempo, "
				+ total() + ", 0, " + alokasi(skema) + ", " + outstanding(skema);
	}

	/** Urutan sama dengan legacy: kode pemasok, lalu tanggal, lalu id. */
	static String urutDaftar() {
		return " ORDER BY s.kode ASC, h.tanggal ASC, h.id ASC LIMIT ? OFFSET ?";
	}

	/** Sisa satu hutang, untuk memeriksa kelebihan bayar sebelum alokasi baru. */
	static String outstandingSatu(String skema) {
		return "SELECT " + outstanding(skema) + " FROM " + skema + "hutang_supplier h WHERE h.id = ?";
	}

	// ------------------------------------------------------------------ riwayat pembayaran

	/**
	 * Rincian alokasi satu dokumen pembayaran. Legacy menautkan alokasi ke faktur lewat
	 * {@code a.pengadaan_faktur}; tenant menautkannya ke hutangnya lewat
	 * {@code a.hutang_supplier_id}.
	 */
	static String sqlRincianAlokasi(String skema) {
		return "SELECT COALESCE(h.nomor_faktur,'#' || h.id), h.tanggal, a.nilai, "
				+ total() + ", h.jatuh_tempo"
				+ " FROM " + skema + "alokasi_pembayaran_hutang a"
				+ " JOIN " + skema + "hutang_supplier h ON a.hutang_supplier_id = h.id"
				+ " WHERE a.pembayaran_hutang_id = ? ORDER BY h.tanggal ASC";
	}

	// ------------------------------------------------------------------ umur hutang

	/**
	 * Ember umur, memakai tanggal acuan {@code asOf} dan label yang <b>sama persis</b> dengan
	 * jalur legacy (BELUM / B1_30 / B31_60 / B61_90 / B90). Label yang berbeda akan membuat
	 * ringkasan di klien jatuh ke ember yang salah tanpa satu pun galat muncul.
	 */
	static String bucketAging(String asOf) {
		String umur = "(DATE '" + asOf + "' - h.jatuh_tempo)";
		return "CASE WHEN h.jatuh_tempo IS NULL OR h.jatuh_tempo >= DATE '" + asOf + "' THEN 'BELUM' "
				+ "WHEN " + umur + " <= 30 THEN 'B1_30' "
				+ "WHEN " + umur + " <= 60 THEN 'B31_60' "
				+ "WHEN " + umur + " <= 90 THEN 'B61_90' ELSE 'B90' END";
	}

	/**
	 * Umur hutang. Saringan {@code jenis_pembayaran} milik legacy dijatuhkan dengan sengaja --
	 * lihat catatan kelas: setiap baris {@code hutang_supplier} sudah pasti hutang, sehingga
	 * saringan itu tidak punya makna di sini.
	 *
	 * <p>Kolom keluarannya berurutan sama dengan legacy: supplierId, kode, nama, nomorFaktur,
	 * tanggal, jatuhTempo, outstanding, bucket.</p>
	 */
	static String sqlAging(String skema, String asOf) {
		return "SELECT h.supplier_id, COALESCE(s.kode,''), COALESCE(s.nama,''), "
				+ "COALESCE(h.nomor_faktur,'#' || h.id), h.tanggal, h.jatuh_tempo, "
				+ outstanding(skema) + " AS outstanding, " + bucketAging(asOf) + " AS bucket"
				+ " FROM " + skema + "hutang_supplier h"
				+ " JOIN " + skema + "supplier s ON h.supplier_id = s.id"
				+ " WHERE " + outstanding(skema) + " > 0.009"
				+ " ORDER BY s.kode ASC, h.jatuh_tempo ASC NULLS FIRST LIMIT 2000";
	}

	// ------------------------------------------------------------------ laporan pembelian

	/**
	 * <h4>Laporan pembelian bertumpu pada {@code pembelian}, BUKAN {@code hutang_supplier}</h4>
	 *
	 * <p>Laporan ini mencakup <b>seluruh</b> pembelian, termasuk yang tunai -- terlihat dari
	 * {@code LEFT JOIN} dan {@code COALESCE(i.jenis_pembayaran,'CASH')} pada jalur legacy.</p>
	 *
	 * <p>Di model tenant, pembelian tunai <b>tidak melahirkan baris {@code hutang_supplier}</b>
	 * sama sekali. Menyusun laporan dari tabel hutang karena itu akan menghilangkan seluruh
	 * pembelian tunai dari laporan tanpa satu pun galat muncul -- angka yang salah, terlihat
	 * benar. Dasarnya harus dokumen pembeliannya.</p>
	 *
	 * <p>Hutangnya di-{@code LEFT JOIN}: ada berarti kredit, tidak ada berarti tunai.</p>
	 */
	static String dasarLaporan(String skema, String where) {
		return " FROM " + skema + "pembelian b"
				+ " LEFT JOIN " + skema + "supplier s ON b.supplier_id = s.id"
				+ " LEFT JOIN " + skema + "hutang_supplier h ON h.pembelian_id = b.id " + where;
	}

	/**
	 * Kolom laporan, berurutan sama dengan legacy: id, nomorFaktur, tanggal, supplierKode,
	 * supplierNama, total, jenisPembayaran, dibayarAwal, alokasi, diskon.
	 *
	 * <p>{@code dibayarAwal} pada legacy adalah {@code COALESCE(i.dibayar_awal, TOTAL)} -- yakni
	 * seluruh nilai dianggap terbayar bila fakturnya tunai. Perilaku itu dipertahankan: tanpa
	 * baris hutang, seluruh totalnya dianggap lunas.</p>
	 */
	static String selectLaporan(String skema) {
		String alokasiHutang = "COALESCE((SELECT SUM(a.nilai) FROM " + skema
				+ "alokasi_pembayaran_hutang a WHERE a.hutang_supplier_id = h.id),0)";
		return "SELECT b.id, COALESCE(b.nomor_faktur,'#' || b.id), b.tanggal, "
				+ "COALESCE(s.kode,''), COALESCE(s.nama,'(tanpa supplier)'), COALESCE(b.total,0), "
				+ "CASE WHEN h.id IS NULL THEN 'CASH' ELSE 'CREDIT' END, "
				+ "CASE WHEN h.id IS NULL THEN COALESCE(b.total,0) ELSE 0 END, "
				+ alokasiHutang + ", COALESCE(b.diskon,0)";
	}

	static String urutLaporan() {
		return " ORDER BY b.tanggal ASC, b.id ASC LIMIT 3000";
	}

	/** Awal {@code WHERE} laporan, dengan nama kolom tenant. */
	static String whereLaporan() {
		return " WHERE b.tanggal BETWEEN CAST(? AS date) AND (CAST(? AS date) + interval '1 day') ";
	}

	// ------------------------------------------------------------------ riwayat pembayaran

	static String dasarRiwayat(String skema, String where) {
		return " FROM " + skema + "pembayaran_hutang b JOIN " + skema + "supplier s"
				+ " ON b.supplier_id = s.id " + where;
	}

	/**
	 * Kolom riwayat pembayaran, berurutan sama dengan legacy: id, tanggal, supplierId, kode,
	 * nama, nominal, metode, noBg, namaBank, tanggalBg, keterangan, oleh, kodeUnik, ringkasan
	 * alokasi, statusDok, statusBg.
	 *
	 * <p>{@code status_bg} ada sejak bundel v17. Sebelumnya kolom ini dikembalikan
	 * {@code NULL} sebab model tenant memang belum menyimpannya — dan sengaja tidak disamakan
	 * dengan status dokumen, karena menyamakannya membuat giro yang belum cair tampak sudah
	 * beres.</p>
	 */
	static String selectRiwayat(String skema) {
		return "SELECT b.id, b.tanggal, b.supplier_id, COALESCE(s.kode,''), COALESCE(s.nama,''), "
				+ "COALESCE(b.nilai,0), COALESCE(b.cara_bayar,''), COALESCE(b.nomor_bg,''), "
				+ "COALESCE(b.nama_bank,''), b.tanggal_bg, COALESCE(b.keterangan,''), "
				+ "COALESCE(b.oleh,''), COALESCE(b.nomor_dokumen,''), "
				+ "(SELECT COALESCE(string_agg(COALESCE(h2.nomor_faktur,'#' || h2.id)"
				+ " || ' (' || a2.nilai || ')', ', '),'')"
				+ " FROM " + skema + "alokasi_pembayaran_hutang a2"
				+ " JOIN " + skema + "hutang_supplier h2 ON a2.hutang_supplier_id = h2.id"
				+ " WHERE a2.pembayaran_hutang_id = b.id), "
				+ "COALESCE(b.status,'AKTIF'), COALESCE(b.status_bg,'')";
	}

	static String urutRiwayat() {
		return " ORDER BY b.tanggal DESC, b.id DESC LIMIT ? OFFSET ?";
	}

	/** Nama kolom metode pembayaran pada model tenant, untuk saringan. */
	static String kolomMetode() {
		return "b.cara_bayar";
	}

	// ------------------------------------------------------------------ kuitansi pembayaran

	/**
	 * Kepala kuitansi. Kolomnya berurutan sama dengan legacy: id, tanggal, supplierKode,
	 * supplierNama, supplierAlamat, nominal, metode, noBg, namaBank, tanggalBg, keterangan,
	 * oleh, kodeUnik.
	 */
	static String sqlKuitansi(String skema) {
		return "SELECT b.id, b.tanggal, COALESCE(s.kode,''), COALESCE(s.nama,''), "
				+ "COALESCE(sp.alamat1,''), COALESCE(b.nilai,0), COALESCE(b.cara_bayar,''), "
				+ "COALESCE(b.nomor_bg,''), COALESCE(b.nama_bank,''), b.tanggal_bg, "
				+ "COALESCE(b.keterangan,''), COALESCE(b.oleh,''), COALESCE(b.nomor_dokumen,'')"
				+ " FROM " + skema + "pembayaran_hutang b"
				+ " JOIN " + skema + "supplier s ON b.supplier_id = s.id"
				+ " LEFT JOIN " + skema + "supplier_profile sp ON sp.supplier_id = s.id"
				+ " WHERE b.id = ?";
	}

	// ------------------------------------------------------------------ simpan (SQL asli)

	/**
	 * <h4>Mengapa jalur tulis memakai SQL asli</h4>
	 * <p>Entitas Hibernate mematok {@code @Table(schema = "koperasi")}, dan pemetaan itu statis
	 * per SessionFactory. {@code session.save()} karena itu selalu menulis ke schema bersama,
	 * berapa pun tenant yang aktif. Lihat catatan yang sama pada {@code SalesInventoryHargaTenant}.</p>
	 */
	static String cariPembayaranByKodeUnik(String skema) {
		return "SELECT id FROM " + skema + "pembayaran_hutang WHERE idempotency_key = ? LIMIT 1";
	}

	/** Keberadaan pemasok DI DALAM schema tenant. */
	static String adaSupplier(String skema) {
		return "SELECT COUNT(*) FROM " + skema + "supplier WHERE id = ?";
	}

	/**
	 * Mengunci baris hutang milik pemasok tertentu. {@code FOR UPDATE} menahan dua pembayaran
	 * bersamaan agar tidak sama-sama lolos pemeriksaan sisa -- tanpa ini, satu hutang bisa
	 * terbayar dua kali.
	 */
	static String kunciHutang(String skema) {
		return "SELECT h.id FROM " + skema + "hutang_supplier h"
				+ " WHERE h.id = ? AND h.supplier_id = ? FOR UPDATE";
	}

	/**
	 * <p>{@code nomor_dokumen} wajib pada model tenant, sedangkan jalur legacy tidak
	 * mengenalnya -- ia hanya punya {@code kode_unik} sebagai kunci idempotensi. Sampai ada
	 * skema penomoran dokumen per tenant, kunci unik itu dipakai sekaligus sebagai nomor
	 * dokumen: nilainya sudah dijamin unik dan dapat ditelusuri balik ke permintaan yang
	 * membuatnya.</p>
	 *
	 * <p>Ini <b>bukan</b> pengganti penomoran yang sebenarnya. Bila nanti ada skema penomoran,
	 * ganti di sini -- bukan di pemanggilnya.</p>
	 */
	static String sisipPembayaran(String skema) {
		return "INSERT INTO " + skema + "pembayaran_hutang"
				+ " (nomor_dokumen, tanggal, supplier_id, cara_bayar, nomor_bg, nama_bank,"
				+ " tanggal_bg, nilai, keterangan, idempotency_key, status, dibuat_pada, oleh)"
				+ " VALUES (?, CURRENT_DATE, ?, ?, ?, ?, ?, ?, ?, ?, 'AKTIF', now(), ?)";
	}

	static String sisipAlokasi(String skema) {
		return "INSERT INTO " + skema + "alokasi_pembayaran_hutang"
				+ " (pembayaran_hutang_id, hutang_supplier_id, nilai, dibuat_pada, oleh)"
				+ " VALUES (?, ?, ?, now(), ?)";
	}

	/**
	 * <h4>Kait ke Daftar Pengajuan Transfer sengaja TIDAK dipanggil pada jalur tenant</h4>
	 *
	 * <p>Jalur legacy menautkan tiap pembayaran hutang ke
	 * {@code akunting.DaftarPengajuanTransfer} supaya muncul di layar Pembayaran Transfer.
	 * Modul itu <b>bersama</b>, bukan per-tenant.</p>
	 *
	 * <p>Menautkan pembayaran milik satu tenant ke sana berarti menaruh datanya di tabel yang
	 * dibaca seluruh instalasi -- persis kebocoran yang dicegah pekerjaan ini. Karena itu kaitnya
	 * dilewati, dan itu <b>mengubah perilaku</b>: pembayaran hutang tenant tidak akan muncul di
	 * layar transfer keuangan bersama.</p>
	 *
	 * <p>Padanan per-tenantnya belum ada dan bukan keputusan yang boleh diambil sebagai efek
	 * samping pemindahan kueri. Ditandai di sini supaya terlihat, bukan tersembunyi.</p>
	 */
	static boolean tautkanKeDaftarTransfer() {
		return false;
	}

	/**
	 * Termin/jenis pembayaran per faktur tidak ada pada model tenant: {@code payable_faktur_info}
	 * lenyap, dan {@code hutang_supplier} hanya menyimpan {@code jatuh_tempo} yang ditetapkan saat
	 * hutangnya lahir. Aksi penyimpanan termin karena itu ditolak, bukan dijalankan sebagian --
	 * menyimpan jatuh tempo saja lalu melaporkan sukses akan menyesatkan pengguna yang mengira
	 * jenis dan terminnya ikut tersimpan.
	 */
	static boolean dukungSimpanTermin() {
		return false;
	}
}
