package ais.service.tenant;

/**
 * <h3>Migrasi tenant v14 — kas fisik pada rekonsiliasi penutupan trip.</h3>
 *
 * <p>Satu kolom: {@code sales_trip_rekonsiliasi.kas_fisik_aktual}. Bundel terkecil pada
 * katalog ini, dan sengaja demikian.</p>
 *
 * <h4>Mengapa hanya satu kolom</h4>
 * <p>Penutupan sesi pada jalur legacy menyimpan sepuluh medan ringkasan pada
 * {@code NotaSalesSession}. Ketika dicocokkan satu per satu terhadap
 * {@code sales_trip_rekonsiliasi}, ternyata hampir semuanya sudah ada tempatnya atau tidak
 * perlu tempat sama sekali:</p>
 *
 * <table border="1">
 * <tr><th>medan legacy</th><th>pada tenant</th></tr>
 * <tr><td>{@code totalBiaya}</td><td>{@code nilai_biaya}, sudah ada</td></tr>
 * <tr><td>{@code totalSetoran}</td><td>{@code nilai_setoran}, sudah ada</td></tr>
 * <tr><td>{@code selisihKas}</td><td>{@code selisih}, sudah ada</td></tr>
 * <tr><td>{@code catatanPenutupan}</td><td>{@code keterangan}, sudah ada</td></tr>
 * <tr><td>{@code waktuTutup}, {@code disetujuiOleh}</td>
 *     <td>{@code disetujui_pada}, {@code disetujui_oleh}, sudah ada</td></tr>
 * <tr><td>{@code totalPenerimaanTunai} / {@code NonTunai}</td>
 *     <td><b>tidak perlu kolom</b> — diturunkan dari {@code penerimaan_piutang} yang menunjuk
 *     trip itu, dipilah {@code cara_bayar}</td></tr>
 * <tr><td>{@code totalPembayaranPembelian}</td>
 *     <td><b>tidak perlu kolom</b> — model tenant belum punya pembelian dalam trip, sehingga
 *     angkanya nol menurut definisi, bukan karena hilang</td></tr>
 * <tr><td>{@code kasFisikAktual}</td><td><b>tidak ada, dan tidak dapat diturunkan</b></td></tr>
 * </table>
 *
 * <h4>Kas fisik adalah MASUKAN, bukan turunan</h4>
 * <p>Angka ini datang dari penghitungan uang di tangan saat sesi ditutup. Tidak ada tabel yang
 * dapat menghasilkannya kembali — buku kas hanya tahu berapa uang <b>seharusnya</b> ada.</p>
 * <p>Menyimpan {@code selisih} saja tidak cukup meskipun selisih itu turunan keduanya: begitu
 * buku kasnya bertambah satu baris, {@code kas_seharusnya} berubah dan kas fisik yang dulu
 * dihitung tidak lagi dapat direkonstruksi dari selisihnya. Justru pada penutupan — saat angka
 * dibekukan dan disetujui — kehilangan masukan aslinya paling merugikan.</p>
 *
 * <h4>Yang TIDAK ditambahkan</h4>
 * <p>Tidak ada kolom status pada {@code sales_trip_barang}. Jalur legacy menandai tiap baris
 * barang RECONCILED saat sesi ditutup; pada model tenant kefinalan itu <b>sudah dinyatakan
 * status tripnya</b> yang menjadi CLOSED. Menyalinnya ke tiap baris hanya melahirkan penanda
 * kedua yang bisa berselisih dengan induknya — persis pola yang ditolak sepanjang pemindahan
 * ini.</p>
 * <p>Nota bawaan berbeda: {@code surat_perintah_sales_nota.status} memang menyimpan hasil
 * kunjungan per nota, jadi RECONCILED di sana adalah keadaan nota itu sendiri, bukan salinan
 * keadaan tripnya.</p>
 */
public final class TenantSchemaMigrationsV14 {

	private TenantSchemaMigrationsV14() {
	}

	public static final String[] ERP = {

			"ALTER TABLE {S}.sales_trip_rekonsiliasi ADD COLUMN kas_fisik_aktual"
					+ " numeric(18,2)" };
}
