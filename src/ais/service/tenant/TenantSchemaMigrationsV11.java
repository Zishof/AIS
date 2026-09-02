package ais.service.tenant;

/**
 * <h3>Migrasi tenant v11 — menjadikan kunci idempotensi benar-benar mengikat.</h3>
 *
 * <p>Bundel ini tidak menambah tabel dan tidak menambah kolom. Ia menambahkan <b>indeks unik
 * parsial</b> pada sebelas kolom {@code idempotency_key} yang selama ini ada tetapi tidak
 * dijaga apa pun.</p>
 *
 * <h4>Apa yang salah</h4>
 * <p>Katalog menyediakan {@code idempotency_key} pada dua belas tabel. Satu di antaranya,
 * {@code idempotency_record}, memang dijaga {@code UNIQUE (idempotency_key, aksi)} sejak awal.
 * Sebelas sisanya <b>hanya berindeks biasa, atau tanpa indeks sama sekali</b> — kolomnya dapat
 * diisi nilai yang sama berkali-kali tanpa ada yang menolak.</p>
 * <p>Akibatnya kunci idempotensi pada schema tenant selama ini bersifat hiasan: ia mencatat
 * niat, tetapi tidak menegakkan apa pun.</p>
 *
 * <h4>Mengapa itu berbahaya, dan seberapa</h4>
 * <p>Jalur tulis tenant yang sudah berjalan — misalnya pencatatan pembayaran hutang —
 * memeriksa kuncinya lebih dulu ({@code SELECT ... WHERE idempotency_key = ?}) dan
 * mengembalikan {@code idempotentReplay} bila sudah ada. Pemeriksaan itu <b>menyelamatkan
 * kasus yang lazim</b>: klik ganda, atau klien mengulang permintaan setelah waktu habis. Itu
 * berurutan, dan pemeriksaannya bekerja.</p>
 * <p>Yang tidak terlindungi adalah dua permintaan yang <b>benar-benar bersamaan</b>: keduanya
 * lolos pemeriksaan sebelum salah satunya sempat menyisipkan, lalu keduanya menyisipkan.
 * Hasilnya satu supplier dibayar dua kali dari satu perintah bayar.</p>
 * <p>Jalur legacy tidak punya celah ini karena di sana penjaganya adalah batasan basis data —
 * kodenya bahkan menangkap {@code ConstraintViolationException} dan memperlakukannya sebagai
 * pengulangan. Penjaga itulah yang hilang saat kueri dipindahkan ke schema tenant.</p>
 * <p>Pola "periksa lalu sisipkan" tanpa batasan basis data memang tidak pernah cukup: satu
 * baris tetap dapat menyelinap di antara keduanya. Yang menutupnya hanya indeks unik.</p>
 *
 * <h4>Mengapa parsial</h4>
 * <p>Semua kolomnya boleh {@code NULL}, dan baris hasil impor legacy umumnya memang tidak
 * punya kunci. {@code WHERE idempotency_key IS NOT NULL} membuat baris-baris itu tetap sah dan
 * tidak saling bentrok sebagai NULL berulang. Bentuknya sama persis dengan indeks yang sudah
 * dipasang v10 untuk {@code sales_trip_biaya}.</p>
 *
 * <h4>Bundel ini dapat GAGAL, dan itu disengaja</h4>
 * <p>Bila suatu tenant terlanjur menyimpan dua baris dengan {@code idempotency_key} sama pada
 * tabel yang sama, pembuatan indeksnya akan ditolak dan migrasi berhenti dengan galat.</p>
 * <p>Itu perilaku yang diinginkan. Baris kembar semacam itu <b>adalah</b> dokumen ganda yang
 * hendak dicegah kolom ini; menemukannya saat migrasi jauh lebih baik daripada membiarkannya
 * diam di dalam data. Penanganannya adalah memeriksa pasangan baris itu dan membatalkan yang
 * bukan asli, bukan melonggarkan indeksnya.</p>
 *
 * <h4>Apa yang TIDAK dilakukan bundel ini</h4>
 * <p>Tidak ada penghapusan atau penggabungan baris kembar secara otomatis. Menebak mana di
 * antara dua dokumen uang yang "asli" bukan pekerjaan migrasi.</p>
 */
public final class TenantSchemaMigrationsV11 {

	private TenantSchemaMigrationsV11() {
	}

	public static final String[] ERP = {

			// ---------- dokumen pembelian & hutang ----------
			"CREATE UNIQUE INDEX uq_{SU}_pembelian_idem ON {S}.pembelian"
					+ " (idempotency_key) WHERE idempotency_key IS NOT NULL",

			"CREATE UNIQUE INDEX uq_{SU}_pembayaran_hutang_idem ON {S}.pembayaran_hutang"
					+ " (idempotency_key) WHERE idempotency_key IS NOT NULL",

			// ---------- dokumen penjualan & piutang ----------
			"CREATE UNIQUE INDEX uq_{SU}_sales_order_idem ON {S}.sales_order"
					+ " (idempotency_key) WHERE idempotency_key IS NOT NULL",

			"CREATE UNIQUE INDEX uq_{SU}_faktur_penjualan_idem ON {S}.faktur_penjualan"
					+ " (idempotency_key) WHERE idempotency_key IS NOT NULL",

			"CREATE UNIQUE INDEX uq_{SU}_penerimaan_piutang_idem ON {S}.penerimaan_piutang"
					+ " (idempotency_key) WHERE idempotency_key IS NOT NULL",

			// ---------- trip sales ----------
			"CREATE UNIQUE INDEX uq_{SU}_sales_trip_idem ON {S}.sales_trip"
					+ " (idempotency_key) WHERE idempotency_key IS NOT NULL",

			"CREATE UNIQUE INDEX uq_{SU}_sales_trip_nota_idem ON {S}.sales_trip_nota"
					+ " (idempotency_key) WHERE idempotency_key IS NOT NULL",

			"CREATE UNIQUE INDEX uq_{SU}_sales_trip_setoran_idem ON {S}.sales_trip_setoran"
					+ " (idempotency_key) WHERE idempotency_key IS NOT NULL",

			// ---------- persediaan & akuntansi ----------
			"CREATE UNIQUE INDEX uq_{SU}_mutasi_stok_idem ON {S}.mutasi_stok"
					+ " (idempotency_key) WHERE idempotency_key IS NOT NULL",

			"CREATE UNIQUE INDEX uq_{SU}_jurnal_idem ON {S}.jurnal"
					+ " (idempotency_key) WHERE idempotency_key IS NOT NULL",

			"CREATE UNIQUE INDEX uq_{SU}_posting_log_idem ON {S}.posting_log"
					+ " (idempotency_key) WHERE idempotency_key IS NOT NULL" };
}
