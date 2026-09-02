package ais.service.tenant;

/**
 * <h3>Migrasi tenant v12 — buku kas trip.</h3>
 *
 * <p>Bundel ini menutup celah <b>C-11</b>: model tenant tidak punya tempat untuk mencatat uang
 * tunai yang berpindah selama satu perjalanan sales. Akibatnya bukan satu aksi yang tertahan,
 * melainkan seluruh sisi kas lapangan — termasuk satu angka yang <b>sudah terkirim dan masih
 * salah</b>.</p>
 *
 * <h4>Mengapa ketiadaannya berakibat, bukan sekadar kurang lengkap</h4>
 * <p>Jalur legacy menyimpan satu buku kas per sesi, {@code nota_sales_kas}: baris bertanda
 * dengan sembilan jenis, dan saldo kas adalah <b>penjumlahan bertandanya</b>. Rumus itu dipakai
 * daftar trip, layar rinci, dan penutupan sesi.</p>
 * <p>Tanpa tabel itu, kueri tenant yang sudah terkirim menghitung saldo kas sebagai
 * {@code Σ nota.tunai − Σ setoran} — mencakup penjualan tunai bernota dan setoran, tetapi
 * <b>tidak</b> uang muka operasional, <b>tidak</b> penagihan tunai piutang lama, dan
 * <b>tidak</b> biaya tunai. Untuk trip dengan panjar 500.000, penjualan tunai 300.000,
 * penagihan tunai 200.000, biaya 100.000, dan setoran 400.000: legacy menghasilkan 500.000,
 * rumus tanpa buku kas menghasilkan −100.000. Bukan selisih kecil — beda tanda.</p>
 *
 * <h4>Yang TIDAK ditambahkan: kolom saldo kas awal</h4>
 * <p>Entitas legacy punya {@code NotaSalesSession.saldoKasAwal}, disalin dari
 * {@code spj.uangMukaOperasional} saat trip dimulai. Godaannya adalah menirunya sebagai kolom
 * {@code sales_trip.saldo_kas_awal}.</p>
 * <p>Sengaja tidak. Pada jalur legacy, uang muka itu <b>juga</b> dibukukan sebagai baris
 * {@code OPENING_ADVANCE}, dan saldo kasnya menjumlahkan baris — sehingga kolomnya hanyalah
 * salinan yang kebetulan sama. Dua sumber untuk satu angka adalah persis bentuk cacat yang
 * sudah ditemukan berkali-kali pada pemindahan ini: kolom ringkasan yang menjadi basi diam-diam.
 * Di sini uang muka awal <b>diturunkan</b> dari bukunya sendiri, dan tidak ada yang bisa
 * berselisih.</p>
 * <p>Yang memang perlu ditambahkan adalah <b>sumbernya</b>:
 * {@code surat_perintah_sales.uang_muka_operasional}. Tanpa itu, tidak ada angka yang bisa
 * dibukukan sebagai baris pembuka saat trip dimulai.</p>
 *
 * <h4>Nominal BERTANDA, bukan kuantitas + arah</h4>
 * <p>Katalog ini memakai pola "kuantitas selalu positif + kolom arah" pada {@code mutasi_stok}.
 * Buku kas <b>sengaja tidak mengikutinya</b>.</p>
 * <p>Seluruh makna buku kas legacy adalah penjumlahan bertanda, dan setiap pembacanya —
 * saldo berjalan, rekap penutupan, kas fisik seharusnya — menjumlahkan langsung. Menyimpan
 * besaran positif berikut arah terpisah memaksa <b>setiap</b> pembaca menyusun ulang tandanya,
 * dan satu pembaca yang lupa menghasilkan angka uang yang salah tanpa gagal. Kekeliruan
 * persis jenis itu sudah pernah terkirim sekali pada rumus saldo kas; tandanya disimpan sekali
 * di sini supaya tidak ada yang perlu menyusunnya ulang.</p>
 * <p>Kontrak jenis dan tandanya didefinisikan pada {@link ais.service.tenant.TenantKasTrip},
 * bukan sebagai batasan {@code CHECK}. Batasan itu akan menolak baris yang jalur legacy terima
 * saat impor, dan menjadikan penambahan jenis kesepuluh sebagai migrasi tersendiri.</p>
 *
 * <h4>Biaya trip: dua kolom supaya kas dan pembalikannya utuh</h4>
 * <p>{@code sales_trip_biaya} selama ini tidak punya cara membedakan biaya tunai dari biaya
 * non-tunai, sehingga tidak ada dasar untuk memutuskan apakah suatu biaya menyentuh kas.
 * {@code cara_bayar} menutup itu.</p>
 * <p>Ia juga satu-satunya tabel dokumen tanpa {@code pembalik_dari_id} dan tanpa {@code status}.
 * Membalik biaya dengan baris bernilai negatif tanpa penunjuk asalnya menghasilkan dua baris
 * yang tidak dapat dipasangkan kembali: totalnya benar, tetapi tidak ada yang tahu baris mana
 * membalik baris mana. Keduanya ditambahkan di sini.</p>
 *
 * <h4>Apa yang bundel ini BUKA, dan apa yang tetap tertutup</h4>
 * <p>Sebelumnya dicatat bahwa bundel ini "membuka tujuh aksi". Setelah ditelusuri satu per satu,
 * <b>angka itu terlalu optimistis</b> — beberapa aksi menunggu lebih dari satu hal:</p>
 * <ul>
 * <li><b>Terbuka:</b> {@code tripCashSale}, {@code expenseReverse}, dan penuntasan rumus
 * {@code saldoKas} pada {@code tripList}.</li>
 * <li><b>Masih menunggu tabel nota bawaan bernilai tertagih:</b> {@code collectionCreate},
 * {@code collectionReverse}, {@code tripClose}, {@code spjNotaAssign},
 * {@code tripNotaResult}.</li>
 * <li><b>Masih menunggu master kategori biaya:</b> {@code expenseCreate}.</li>
 * <li><b>Masih menunggu tabel pembelian dalam trip:</b> {@code tripPurchaseLink}, dan sisi
 * pembelian pada {@code tripDetail}.</li>
 * </ul>
 * <p>Ketiga penghalang sisa itu bundel tersendiri, dan tidak dicampurkan ke sini: masing-masing
 * menyangkut konsep berbeda, dan menggabungkannya membuat satu bundel yang gagal seluruhnya bila
 * satu bagiannya keliru.</p>
 *
 * <h4>Apa yang TIDAK dilakukan bundel ini</h4>
 * <p>Tidak ada pengisian data. Trip yang sudah berjalan tidak memperoleh baris pembuka secara
 * surut, dan biaya lama tetap {@code NULL} pada {@code cara_bayar}. Menebak berapa uang muka
 * yang dulu dibawa, atau biaya mana yang dulu tunai, hanya melahirkan angka uang yang tampak
 * sahih tanpa dasar.</p>
 */
public final class TenantSchemaMigrationsV12 {

	private TenantSchemaMigrationsV12() {
	}

	public static final String[] ERP = {

			// ---------- sumber uang muka operasional ----------
			"ALTER TABLE {S}.surat_perintah_sales ADD COLUMN uang_muka_operasional"
					+ " numeric(18,2) DEFAULT 0",

			// ---------- buku kas trip ----------
			"CREATE TABLE {S}.sales_trip_kas ("
					+ "id bigserial PRIMARY KEY, "
					+ "sales_trip_id bigint NOT NULL REFERENCES {S}.sales_trip(id), "
					// BERTANDA: masuk positif, keluar negatif. Lihat TenantKasTrip.
					+ "jenis varchar(32) NOT NULL, "
					+ "nominal numeric(18,2) NOT NULL, "
					+ "referensi varchar(64), "
					+ "keterangan text, "
					+ "waktu timestamp NOT NULL, "
					+ "idempotency_key varchar(128), "
					+ "correlation_id varchar(64), "
					+ "dibuat_pada timestamp, "
					+ "tanggal_dirubah timestamp, "
					+ "oleh varchar(255), "
					+ "olehid varchar(255), "
					+ "legacy_source_file varchar(128), "
					+ "legacy_source_record_no integer, "
					+ "legacy_row_hash varchar(64), "
					+ "legacy_import_run_id bigint, "
					+ "legacy_deleted boolean DEFAULT false, "
					+ "legacy_tafsir varchar(64))",

			"CREATE INDEX idx_{SU}_sales_trip_kas_trip ON {S}.sales_trip_kas (sales_trip_id)",
			"CREATE INDEX idx_{SU}_sales_trip_kas_jenis ON {S}.sales_trip_kas (jenis)",
			"CREATE INDEX idx_{SU}_sales_trip_kas_waktu ON {S}.sales_trip_kas (waktu)",

			// Parsial, sebentuk dengan sebelas indeks idempotensi yang dipasang v11.
			"CREATE UNIQUE INDEX uq_{SU}_sales_trip_kas_idem ON {S}.sales_trip_kas"
					+ " (idempotency_key) WHERE idempotency_key IS NOT NULL",

			// ---------- biaya trip: kas dan pembalikannya ----------
			"ALTER TABLE {S}.sales_trip_biaya ADD COLUMN cara_bayar varchar(32)",

			"ALTER TABLE {S}.sales_trip_biaya ADD COLUMN status varchar(32) DEFAULT 'AKTIF'",

			"ALTER TABLE {S}.sales_trip_biaya ADD COLUMN pembalik_dari_id bigint"
					+ " REFERENCES {S}.sales_trip_biaya(id)",

			"CREATE INDEX idx_{SU}_sales_trip_biaya_status ON {S}.sales_trip_biaya (status)" };
}
