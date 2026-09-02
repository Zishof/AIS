package ais.service.tenant;

/**
 * <h3>Migrasi tenant v17 — status giro pada pembayaran dan penerimaan.</h3>
 *
 * <p>Empat kolom: {@code status_bg} dan {@code tanggal_status_bg} pada
 * {@code pembayaran_hutang} dan {@code penerimaan_piutang}. Penghalang katalog terakhir pada
 * seluruh P4.</p>
 *
 * <h4>Menyimpan nomor giro tanpa statusnya adalah setengah catatan</h4>
 * <p>Model tenant sudah menyimpan {@code nomor_bg}, {@code nama_bank}, dan {@code tanggal_bg}.
 * Yang hilang justru medan yang menentukan: <b>apakah gironya cair atau ditolak</b>.</p>
 * <p>Tanpa itu, giro yang sudah dicairkan bank tidak dapat dibedakan dari giro yang ditolak,
 * dan keduanya tampak sama pada layar — dokumen berisi nomor giro, tanpa keterangan apa pun
 * tentang nasibnya. Untuk pembayaran hutang, itu berarti tidak ada cara mengetahui apakah
 * supplier sudah benar-benar menerima uangnya.</p>
 *
 * <h4>Siklusnya: DITERIMA &rarr; CAIR atau TOLAK</h4>
 * <p>Jalur legacy memperlakukan {@code null} sebagai DITERIMA — giro baru diterima dan belum
 * ada kabarnya. Dari sana ia hanya boleh berpindah <b>sekali</b>: menjadi CAIR atau TOLAK, dan
 * sesudah itu final.</p>
 * <p>Kolomnya karena itu dibiarkan {@code NULL} sebagai bawaan, bukan diisi {@code 'DITERIMA'}.
 * Mengisi bawaan berarti setiap dokumen non-giro yang lampau ikut mengaku punya giro yang sedang
 * ditunggu.</p>
 *
 * <h4>Penolakan giro menerbitkan pembalikan</h4>
 * <p>Ini yang membuat kolom status bukan sekadar keterangan: pada jalur legacy, giro yang ditolak
 * <b>otomatis menerbitkan dokumen pembalik</b>. Uangnya tidak pernah benar-benar berpindah,
 * sehingga hutang atau piutangnya harus hidup kembali.</p>
 * <p>Kedua jalur pembalikan itu sudah tersedia pada schema tenant sejak v11 (idempotensi
 * pembayaran) dan v12–v13 (pembalikan penagihan), sehingga v17 melengkapi bagian terakhirnya.</p>
 *
 * <h4>{@code tanggal_status_bg} terpisah dari {@code tanggal_bg}</h4>
 * <p>Keduanya berbeda maksud dan mudah tertukar. {@code tanggal_bg} adalah tanggal jatuh tempo
 * yang tertulis pada lembar gironya; {@code tanggal_status_bg} adalah kapan nasibnya diketahui.
 * Giro bertanggal 30 Juni yang baru ditolak bank pada 3 Juli punya dua tanggal berbeda, dan
 * menyatukannya menghilangkan salah satunya.</p>
 *
 * <h4>Apa yang TIDAK dilakukan bundel ini</h4>
 * <p>Tidak ada penetapan surut. Dokumen giro yang sudah ada tetap {@code NULL} pada kedua kolom,
 * yang berarti "belum ada kabarnya" — bukan CAIR. Menebak bahwa giro lama pasti sudah cair akan
 * menutup dokumen yang mungkin sebenarnya ditolak dan belum ditindaklanjuti.</p>
 */
public final class TenantSchemaMigrationsV17 {

	private TenantSchemaMigrationsV17() {
	}

	public static final String[] ERP = {

			// ---------- sisi hutang ----------
			"ALTER TABLE {S}.pembayaran_hutang ADD COLUMN status_bg varchar(32)",

			"ALTER TABLE {S}.pembayaran_hutang ADD COLUMN tanggal_status_bg date",

			"CREATE INDEX idx_{SU}_bayar_hutang_status_bg ON {S}.pembayaran_hutang (status_bg)",

			// ---------- sisi piutang ----------
			"ALTER TABLE {S}.penerimaan_piutang ADD COLUMN status_bg varchar(32)",

			"ALTER TABLE {S}.penerimaan_piutang ADD COLUMN tanggal_status_bg date",

			"CREATE INDEX idx_{SU}_terima_piutang_status_bg ON {S}.penerimaan_piutang"
					+ " (status_bg)" };
}
