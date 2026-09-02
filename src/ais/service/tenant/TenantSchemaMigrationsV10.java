package ais.service.tenant;

/**
 * <h3>Migrasi tenant v10 — tiga celah yang menghadang pemindahan P4.</h3>
 *
 * <p>Bundel ini <b>tidak menambah tabel</b>. Ia menutup tiga celah yang masing-masing
 * menghentikan satu bagian pemindahan {@code si_*}, dan ketiganya ditemukan dengan cara yang
 * sama: mencocokkan kueri legacy terhadap schema tenant sungguhan, bukan membaca dokumen.</p>
 *
 * <h4>Mengapa bundel baru, bukan menyunting v1–v9</h4>
 * <p>Katalog migrasi bersifat <b>append-only ber-checksum</b>. Menyunting DDL bundel yang sudah
 * dirilis membuat {@code terapkanMigrasi} gagal keras pada <b>setiap</b> tenant yang sudah
 * di-provision — checksum tersimpan tidak lagi cocok. Perubahan schema karena itu selalu
 * berupa bundel baru berisi {@code ALTER}, dan itulah bundel ini.</p>
 *
 * <h4>J-10.1 — {@code harga_jual_customer.customer_id} boleh kosong</h4>
 * <p>Legacy menyatakan <b>harga umum</b> sebagai baris {@code harga_jual_customer} dengan
 * {@code anggota_koperasi IS NULL}: satu harga yang berlaku bagi semua pelanggan, bersanding
 * dengan harga khusus per pelanggan pada tabel yang sama.</p>
 * <p>Kolom tenant semula {@code NOT NULL}, sehingga baris semacam itu <b>mustahil ada</b>.
 * Akibatnya tiga hal pada helper Harga terpaksa gagal-tertutup: saringan "hanya umum", simpan
 * harga tanpa anggota, dan kolom harga umum pada Analisa Harga.</p>
 * <p>Alternatif yang sempat dipertimbangkan — memakai {@code produk.harga_jual_standar} sebagai
 * harga umum — ditolak karena keduanya berbeda maksud: harga jual standar adalah atribut
 * produk, sedangkan harga umum adalah <b>versi berharga-berlaku</b> yang punya tanggal mulai,
 * tanggal akhir, dan riwayat. Menyamakannya menghilangkan riwayat harga umum.</p>
 *
 * <h4>J-10.2 — {@code sales_trip_biaya.idempotency_key}</h4>
 * <p>Jalur legacy <b>mewajibkan</b> {@code kode_unik} saat mencatat biaya trip; itu bukan
 * pilihan. Tanpa kolom penampungnya, satu permintaan yang diulang — gangguan jaringan, klien
 * mencoba lagi — akan <b>membukukan biaya dua kali</b>, dan langsung merusak total biaya trip
 * pada rekonsiliasi.</p>
 * <p>Indeks uniknya <b>parsial</b> ({@code WHERE idempotency_key IS NOT NULL}) supaya baris
 * lama yang tidak punya kunci tetap sah dan tidak saling bentrok sebagai NULL ganda.</p>
 *
 * <h4>J-10.3 — {@code penerimaan_piutang.sales_trip_id}</h4>
 * <p>Legacy menautkan penerimaan piutang ke sesi trip lewat {@code p.sesi}, sehingga layar
 * rinci trip dapat menjumlahkan berapa yang tertagih selama perjalanan itu.</p>
 * <p>Tanpa kaitan itu, jumlahnya hanya dapat ditempuh lewat rantai
 * {@code sales_trip_nota} &rarr; {@code faktur_penjualan} &rarr; {@code piutang_customer}
 * &rarr; alokasinya — dan rantai itu <b>tidak setara</b>: ia hanya menemukan penagihan atas
 * faktur yang terbit pada trip yang sama, sedangkan sales juga menagih piutang lama saat
 * berkeliling. Justru penagihan piutang lama itulah yang biasanya jadi alasan perjalanannya.</p>
 * <p>Kolomnya sengaja {@code NULL}-able: penerimaan di kantor memang tidak punya trip.</p>
 *
 * <h4>Apa yang TIDAK dilakukan bundel ini</h4>
 * <p>Tidak ada pengisian data. Baris yang sudah ada tetap {@code NULL} pada ketiga kolom baru,
 * dan itu benar: menebak trip mana yang menagih suatu penerimaan lama, atau kunci idempotensi
 * mana yang dulu dipakai, hanya melahirkan data yang tampak sahih tanpa dasar.</p>
 */
public final class TenantSchemaMigrationsV10 {

	private TenantSchemaMigrationsV10() {
	}

	public static final String[] ERP = {

			// ---------- J-10.1: harga umum harus dapat disimpan ----------
			"ALTER TABLE {S}.harga_jual_customer ALTER COLUMN customer_id DROP NOT NULL",

			// Harga umum unik per produk per tanggal berlaku. Parsial, sebab yang dijaga
			// hanya baris tanpa customer -- harga khusus pelanggan punya aturannya sendiri.
			"CREATE UNIQUE INDEX uq_{SU}_harga_jual_umum ON {S}.harga_jual_customer"
					+ " (produk_id, berlaku_dari) WHERE customer_id IS NULL",

			// ---------- J-10.2: biaya trip harus idempoten ----------
			"ALTER TABLE {S}.sales_trip_biaya ADD COLUMN idempotency_key varchar(128)",

			// Parsial: baris lama tanpa kunci tetap sah dan tidak bentrok sesama NULL.
			"CREATE UNIQUE INDEX uq_{SU}_sales_trip_biaya_idem ON {S}.sales_trip_biaya"
					+ " (idempotency_key) WHERE idempotency_key IS NOT NULL",

			// ---------- J-10.3: penerimaan piutang dapat ditautkan ke trip ----------
			"ALTER TABLE {S}.penerimaan_piutang ADD COLUMN sales_trip_id bigint"
					+ " REFERENCES {S}.sales_trip(id)",

			"CREATE INDEX idx_{SU}_penerimaan_piutang_trip ON {S}.penerimaan_piutang"
					+ " (sales_trip_id)" };
}
