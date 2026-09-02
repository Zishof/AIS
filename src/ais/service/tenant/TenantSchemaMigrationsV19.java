package ais.service.tenant;

/**
 * <h3>Migrasi tenant v19 — satuan jual: konversi UOM dan cuplikannya pada baris order.</h3>
 *
 * <p>Dua kolom pada {@code satuan} (metadata konversinya) dan tiga pada
 * {@code sales_order_detail} (cuplikan yang dipakai satu baris order).</p>
 *
 * <h4>Yang benar-benar hilang bukan tabelnya</h4>
 * <p>Catatan lama menyebut {@code satuan_produk} "tidak ada" pada model tenant. Itu benar sebagai
 * nama tabel dan menyesatkan sebagai kesimpulan: perannya <b>ada</b>, bernama {@code satuan},
 * lengkap dengan {@code produk.satuan_id} yang menunjuk satuan dasar produk. Yang hilang hanya
 * <b>metadata konversinya</b> — tanpa itu satuan tenant cuma label, dan "12 PCS" tidak dapat
 * dihubungkan dengan "1 DUS".</p>
 *
 * <h4>Rasio DAN arahnya, bukan satu bilangan faktor</h4>
 * <p>Godaan yang wajar: gabungkan {@code rasio} dan {@code tipe_konversi} legacy menjadi satu
 * kolom faktor, sebab dua kolom yang menyandikan satu nilai bisa saling bertentangan.</p>
 * <p><b>Itu keliru di sini, dan alasannya aritmetika.</b> Faktor pecahan tidak selalu dapat
 * disimpan tepat: 1/12 pada {@code numeric(18,6)} menjadi {@code 0.083333}, dan
 * {@code 12 × 0.083333 = 0.999996}. Dua belas PCS berubah menjadi 0,999996 DUS — selisih yang
 * tidak pernah cukup besar untuk terlihat, dan tidak pernah hilang. Menyimpan {@code rasio = 12}
 * beserta arahnya menjaga angkanya tetap bulat, dan pembagiannya dilakukan sekali pada saat
 * dipakai, bukan dibekukan sebagai desimal yang sudah rusak.</p>
 * <p>Karena itu bentuk legacy dipertahankan justru ketika sisa model tenant menyederhanakan.
 * Aturan "satu sumber" tetap ditegakkan dengan cara lain: yang menyimpan kebenaran adalah
 * {@code kuantitas} pada barisnya, yang dihitung sekali dan bertipe numerik tepat.</p>
 *
 * <h4>Kategori: supaya konversi yang mustahil DITOLAK</h4>
 * <p>{@code kategori} bukan hiasan pengelompokan. Jalur legacy memakainya untuk menolak konversi
 * antar-kategori — kilogram tidak boleh menjadi liter — dan penolakan itu satu-satunya hal yang
 * mencegah faktor asal-asalan menghasilkan kuantitas yang tampak wajar. Satuan tanpa kategori
 * diperlakukan {@code 'UNIT'}, sama seperti jalur legacy memperlakukan katalog lama.</p>
 *
 * <h4>Cuplikan pada baris order: tiga kolom, dan {@code kuantitas} tetap yang berwenang</h4>
 * <p>{@code satuan_jual_id}, {@code qty_input}, dan {@code faktor_ke_dasar} adalah <b>cuplikan
 * saat transaksi</b>, bukan ringkasan yang bisa diturunkan ulang: rasio satuan boleh berubah
 * besok, sedangkan dokumen yang sudah terbit harus tetap menceritakan apa yang dipakai hari ini.
 * Ini kebalikan dari {@code nilai_tertagih} dan {@code sisa_hutang}, yang justru <b>tidak</b>
 * disimpan karena keduanya ringkasan.</p>
 * <p>{@code faktor_ke_dasar} disimpan sebagai catatan, <b>bukan</b> sebagai masukan hitungan
 * ulang. Kuantitas dasarnya sudah dihitung sekali ke {@code kuantitas}; pembulatan pada kolom
 * cuplikan karena itu tidak pernah bisa merusak angka yang mengikat.</p>
 *
 * <h4>Apa yang TIDAK dilakukan bundel ini</h4>
 * <p>Tidak ada {@code satuan_pembelian} pada {@code produk}. Jalur legacy memakainya hanya
 * sebagai cadangan ketika satuan masukan tidak disebut; pada jalur tenant satuan jualnya selalu
 * disebut ketika cabang ini berjalan, sehingga menambahkannya sekarang berarti menambah kolom
 * yang belum ada pembacanya.</p>
 * <p>Tidak ada {@code presisi_pembulatan}. Legacy menyimpannya tetapi tidak memakainya pada
 * jalur konversi ini; menyalin kolom yang tidak dibaca hanya memindahkan pertanyaan.</p>
 * <p>Tidak ada pengisian surut. Satuan yang sudah ada tetap {@code NULL} pada kedua kolom, yang
 * berarti kategorinya {@code UNIT} dan rasionya 1 — yaitu perlakuan yang sama dengan satuan
 * berkategori tunggal, dan bukan tebakan tentang satuan yang belum pernah dikonversi.</p>
 */
public final class TenantSchemaMigrationsV19 {

	private TenantSchemaMigrationsV19() {
	}

	public static final String[] ERP = {

			"ALTER TABLE {S}.satuan ADD COLUMN kategori varchar(50)",

			"ALTER TABLE {S}.satuan ADD COLUMN rasio numeric(18,6)",

			"ALTER TABLE {S}.satuan ADD COLUMN tipe_konversi varchar(20)",

			"CREATE INDEX idx_{SU}_satuan_kategori ON {S}.satuan (kategori)",

			"ALTER TABLE {S}.sales_order_detail ADD COLUMN satuan_jual_id bigint"
					+ " REFERENCES {S}.satuan(id)",

			"ALTER TABLE {S}.sales_order_detail ADD COLUMN qty_input numeric(18,4)",

			"ALTER TABLE {S}.sales_order_detail ADD COLUMN faktor_ke_dasar numeric(18,6)",

			"CREATE INDEX idx_{SU}_sales_order_detail_satuan ON {S}.sales_order_detail"
					+ " (satuan_jual_id)" };
}
