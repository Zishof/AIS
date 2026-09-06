package ais.service.tenant;

/**
 * <h3>Migrasi tenant v21 — wilayah mitra: satu kolom pada profil supplier dan customer.</h3>
 *
 * <p>Dua kolom: {@code supplier_profile.wilayah} dan {@code customer_profile.wilayah}.</p>
 *
 * <h4>Yang hilang</h4>
 * <p>Aplikasi lama membagi mitra menurut wilayah, menampilkannya sebagai kolom "Wilayah" pada
 * layar Data Supplier (01-03) dan Data Customer (04-06), dan menyediakan tombol
 * <b>"Urut Wilayah"</b> pada keduanya. Model tenant tidak punya kolomnya, sehingga
 * {@link SalesInventoryMasterTenant#dukungWilayahMitra()} — pembaca yang dipanggil helper master —
 * mengembalikan {@code false}, saringan kata kunci melewatkan wilayah, dan pengurutan
 * {@code sort=wilayah} jatuh ke urutan nama.</p>
 *
 * <p>Layar barunya SUDAH punya kolom WILAYAH; ia kosong bukan karena tidak dirancang, melainkan
 * karena datanya tidak pernah sampai.</p>
 *
 * <h4>Sumbernya BERBEDA antara kedua berkas — dan itu jebakannya</h4>
 * <p>Godaan yang wajar: mengambil kolom {@code WILAYAH} dari kedua berkas legacy. Itu benar untuk
 * supplier dan <b>salah total</b> untuk customer.</p>
 * <table border="1">
 * <caption>Terukur pada data UAT cmnmedika</caption>
 * <tr><th>berkas</th><th>alamat jalan</th><th>wilayah</th></tr>
 * <tr><td>{@code SUPPLIER.DBF} (101)</td><td>{@code ALAMAT} — 97 terisi ("CRBN", "BANDUNG")</td>
 *     <td>{@code WILAYAH} — <b>72 terisi</b> ("CIREBON", "CRB", "JT 7")</td></tr>
 * <tr><td>{@code CUSTOMER.DBF} (334)</td><td>{@code ALAMAT1} — 334 terisi ("BOBOS", "BODE")</td>
 *     <td>{@code ALAMAT} — <b>334 terisi</b>, tujuh nilai saja: C1..C7</td></tr>
 * </table>
 * <p>Pada berkas customer, kolom yang <b>bernama</b> {@code WILAYAH} justru kosong seluruhnya
 * (0 dari 334), dan wilayahnya menumpang di {@code ALAMAT}. Karena itu ekstraktor memetakan
 * sumber yang berbeda untuk masing-masing, dan itu dinyatakan di sini supaya orang berikutnya
 * tidak "merapikannya" menjadi seragam.</p>
 *
 * <h4>Mengapa panjangnya 100, dan mengapa varchar bukan referensi tabel</h4>
 * <p>{@code varchar(100)} menyamai {@code kota} pada tabel yang sama — nilai terpanjang pada data
 * legacy hanya tujuh aksara, jadi 100 memberi ruang tanpa menebak-nebak.</p>
 * <p>Wilayah TIDAK dijadikan tabel referensi ber-FK meski nilainya sedikit dan berulang. Nilainya
 * belum bersih: supplier memakai "CIREBON", "CRB", dan "CBR" untuk hal yang tampaknya sama,
 * sedangkan customer memakai kode C1..C7. Memasang FK sekarang menuntut penyeragaman lebih dulu —
 * yaitu keputusan bisnis tentang wilayah mana yang sama dengan mana, bukan keputusan migrasi.
 * Menyimpan teks apa adanya membawa data legacy masuk utuh dan menyisakan penyeragaman itu
 * sebagai langkah tersendiri yang dapat diambil kapan saja.</p>
 *
 * <h4>NULL berarti "wilayahnya tidak dicatat legacy"</h4>
 * <p>29 dari 101 supplier tidak punya wilayah pada berkasnya. Kolomnya boleh NULL dan tidak
 * diberi nilai bawaan; mengisinya dengan string kosong akan membuat "tidak dicatat" tidak dapat
 * dibedakan dari "sengaja dikosongkan".</p>
 */
public final class TenantSchemaMigrationsV21 {

	private TenantSchemaMigrationsV21() {
	}

	public static final String[] ERP = {

			"ALTER TABLE {S}.supplier_profile ADD COLUMN wilayah varchar(100)",

			"ALTER TABLE {S}.customer_profile ADD COLUMN wilayah varchar(100)",

			// Pengurutan dan penyaringan wilayah adalah alasan kolom ini ada; tanpa indeks,
			// "Urut Wilayah" pada 334 pelanggan menjadi pemindaian penuh setiap kali.
			"CREATE INDEX idx_{SU}_supplier_profile_wilayah ON {S}.supplier_profile (wilayah)",

			"CREATE INDEX idx_{SU}_customer_profile_wilayah ON {S}.customer_profile (wilayah)" };
}
