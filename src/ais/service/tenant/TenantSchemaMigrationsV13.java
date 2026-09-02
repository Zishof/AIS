package ais.service.tenant;

/**
 * <h3>Migrasi tenant v13 — nota piutang yang dibawa sales.</h3>
 *
 * <p>Bundel ini menambah satu tabel: {@code surat_perintah_sales_nota}, penugasan dokumen
 * piutang kepada satu Surat Perintah Sales untuk ditagih di lapangan. Ia menutup penghalang
 * kedua dari tiga yang tersisa pada P4, dan penghalang yang paling banyak menahan aksi.</p>
 *
 * <h4>Apa yang ditahannya</h4>
 * <p>Lima aksi menunggu tabel ini: {@code spjNotaAssign} dan {@code tripNotaResult} pada helper
 * Trip, {@code tripClose} yang merekap hasil penagihannya, serta {@code collectionCreate} dan
 * {@code collectionReverse} pada helper Piutang — keduanya memundurkan nilai tertagih nota yang
 * dibawa saat penagihan dicatat atau dibatalkan.</p>
 * <p>Tabel tenant yang namanya mirip, {@code sales_trip_nota}, bukan padanannya: itu nota
 * <b>penjualan yang diterbitkan</b> dalam trip. Yang dibutuhkan di sini adalah kebalikannya —
 * piutang <b>lama</b> yang dibawa untuk ditagih.</p>
 *
 * <h4>Melekat pada SPJ, bukan pada trip</h4>
 * <p>Penugasan terjadi <b>sebelum berangkat</b>: jalur legacy menolaknya begitu SPJ meninggalkan
 * status DRAFT/SUBMITTED/APPROVED. Karena itu tabelnya menggantung pada
 * {@code surat_perintah_sales}, sebentuk dengan {@code surat_perintah_sales_detail} yang memuat
 * barang bawaannya. SPJ membawa dua hal — barang dan tagihan — dan keduanya direncanakan di
 * tempat yang sama.</p>
 *
 * <h4>EMPAT medan legacy yang sengaja TIDAK dibuatkan kolom</h4>
 *
 * <p>Entitas legacy {@code SpjSalesNota} punya sebelas medan. Empat di antaranya adalah salinan
 * atau turunan, dan menyalinnya berarti membuat sumber kedua yang bisa berselisih:</p>
 * <ul>
 * <li><b>{@code nilaiAwal}</b> — salinan {@code piutang_customer.nilai}. Ditarik lewat join.</li>
 * <li><b>{@code jatuhTempo}</b> — salinan {@code piutang_customer.jatuh_tempo}. Ditarik lewat
 * join.</li>
 * <li><b>{@code customer}</b> — salinan {@code piutang_customer.customer_id}. Ditarik lewat
 * join.</li>
 * <li><b>{@code nilaiTertagih}</b> — lihat bagian berikut; ini yang paling penting.</li>
 * </ul>
 *
 * <h4>{@code nilaiTertagih} DITURUNKAN, dan itu menghapus satu kelas cacat</h4>
 * <p>Pada jalur legacy, nilai tertagih adalah kolom yang <b>dinaikkan</b> saat penagihan dicatat
 * dan <b>diturunkan</b> saat penagihan dibalik. Dua penulis untuk satu angka, dan salah satunya —
 * pembalikan — mudah terlewat. Legacy bahkan harus menjepitnya ke nol secara eksplisit supaya
 * tidak menjadi negatif.</p>
 * <p>Model tenant tidak menyimpannya. Berapa yang tertagih selama satu perjalanan diturunkan dari
 * alokasi penerimaan: penerimaan yang menunjuk trip tersebut ({@code penerimaan_piutang
 * .sales_trip_id}, kolom dari bundel v10) dan teralokasi ke dokumen piutang nota itu.</p>
 * <p>Akibatnya, pembalikan penagihan <b>tidak perlu mengingat apa pun</b>: alokasi pembaliknya
 * bernilai negatif, sehingga jumlahnya turun dengan sendirinya. Angka yang mustahil basi lebih
 * baik daripada angka yang diperbarui dua penulis.</p>
 *
 * <h4>{@code saldoSaatAssign} TETAP disimpan, dan itu bukan inkonsistensi</h4>
 * <p>Kolom ini terlihat seperti ringkasan yang bisa basi — persis jenis yang ditolak di atas.
 * Bedanya menentukan: ia <b>bukan</b> ringkasan keadaan sekarang, melainkan <b>potret satu
 * saat</b>. Berapa sisa tagihan ketika nota diserahkan tidak dapat dihitung ulang belakangan,
 * sebab alokasi sesudahnya sudah mengubah sisanya.</p>
 * <p>Potret memang harus disimpan; ringkasan tidak. Yang berbahaya adalah kolom yang mengaku
 * mewakili keadaan sekarang padahal berhenti diperbarui.</p>
 *
 * <h4>Satu nota, satu penugasan</h4>
 * <p>{@code UNIQUE (surat_perintah_sales_id, piutang_customer_id)} menjaga satu dokumen piutang
 * tidak ditugaskan dua kali pada SPJ yang sama. Jalur legacy mengandalkan penggantian menyeluruh
 * ("hapus semua lalu tulis ulang") tanpa batasan basis data; di sini batasannya dipasang, sebab
 * penugasan ganda berarti satu tagihan dihitung dua kali pada rekap penutupan.</p>
 *
 * <h4>Apa yang TIDAK dilakukan bundel ini</h4>
 * <p>Tidak ada pengisian data, dan tidak ada penugasan yang dibentuk surut untuk SPJ yang sudah
 * berjalan. Menebak nota mana yang dulu dibawa hanya melahirkan riwayat penagihan yang tampak
 * sahih tanpa dasar.</p>
 */
public final class TenantSchemaMigrationsV13 {

	private TenantSchemaMigrationsV13() {
	}

	public static final String[] ERP = {

			"CREATE TABLE {S}.surat_perintah_sales_nota ("
					+ "id bigserial PRIMARY KEY, "
					+ "surat_perintah_sales_id bigint NOT NULL"
					+ " REFERENCES {S}.surat_perintah_sales(id), "
					+ "piutang_customer_id bigint NOT NULL REFERENCES {S}.piutang_customer(id), "
					// Potret saat penyerahan; TIDAK dapat dihitung ulang belakangan.
					+ "saldo_saat_assign numeric(18,2) NOT NULL DEFAULT 0, "
					+ "status varchar(32) DEFAULT 'ASSIGNED', "
					+ "hasil_kunjungan text, "
					+ "janji_bayar date, "
					+ "alasan_gagal text, "
					+ "idempotency_key varchar(128), "
					+ "dibuat_pada timestamp, "
					+ "tanggal_dirubah timestamp, "
					+ "oleh varchar(255), "
					+ "olehid varchar(255), "
					+ "legacy_source_file varchar(128), "
					+ "legacy_source_record_no integer, "
					+ "legacy_row_hash varchar(64), "
					+ "legacy_import_run_id bigint, "
					+ "legacy_deleted boolean DEFAULT false, "
					+ "legacy_tafsir varchar(64), "
					+ "CONSTRAINT uq_{SU}_sps_nota UNIQUE (surat_perintah_sales_id,"
					+ " piutang_customer_id))",

			"CREATE INDEX idx_{SU}_sps_nota_spj ON {S}.surat_perintah_sales_nota"
					+ " (surat_perintah_sales_id)",

			"CREATE INDEX idx_{SU}_sps_nota_piutang ON {S}.surat_perintah_sales_nota"
					+ " (piutang_customer_id)",

			"CREATE INDEX idx_{SU}_sps_nota_status ON {S}.surat_perintah_sales_nota (status)",

			// Sebentuk dengan dua belas indeks idempotensi yang sudah dipasang v10-v12.
			"CREATE UNIQUE INDEX uq_{SU}_sps_nota_idem ON {S}.surat_perintah_sales_nota"
					+ " (idempotency_key) WHERE idempotency_key IS NOT NULL" };
}
