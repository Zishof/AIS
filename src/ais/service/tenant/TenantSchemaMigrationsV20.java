package ais.service.tenant;

/**
 * <h3>Migrasi tenant v20 — harga jual kedua (tunai), padanan {@code HARGAJUAL2} legacy.</h3>
 *
 * <p>Satu kolom pada {@code produk}: {@code harga_jual_tunai}.</p>
 *
 * <h4>Yang hilang, dan seberapa besar</h4>
 * <p>Aplikasi lama menyimpan <b>dua</b> harga jual per produk — {@code HARGAJUAL} (kredit) dan
 * {@code HARGAJUAL2} (tunai) pada {@code STOK.DBF} — dan menampilkan keduanya berdampingan pada
 * layar "DAFTAR HARGA JUAL" lengkap dengan RL% masing-masing. Model tenant hanya punya
 * {@code produk.harga_jual_standar}, sehingga impor legacy membuang {@code HARGAJUAL2} dan
 * melaporkannya sebagai medan tanpa rumah.</p>
 *
 * <p>Bukan medan yang jarang dipakai. Terukur pada data UAT {@code cmnmedika}:</p>
 * <pre>
 *   626 produk
 *   HARGAJUAL2 kosong        459
 *   keduanya nol              34
 *   kredit == tunai            1
 *   kredit != tunai          132   &lt;-- 21% dari seluruh produk
 * </pre>
 * <p>Contoh: {@code 000301} kredit Rp 217.000 vs tunai Rp 206.000; {@code 000412} kredit Rp 0
 * tetapi tunai Rp 54.000.</p>
 *
 * <h4>Kolom pada produk, bukan daftar harga berjenis</h4>
 * <p>Godaan yang wajar: memodelkannya sebagai {@code price_list} ber-jenis KREDIT/TUNAI, yang
 * lebih umum dan menampung jenis harga berikutnya tanpa migrasi lagi.</p>
 * <p><b>Ditolak, dan alasannya konsistensi dengan yang sudah ada.</b> Harga jual pertama sudah
 * berada di {@code produk.harga_jual_standar} — bukan di daftar harga. Menaruh yang kedua di
 * tempat yang berbeda berarti dua harga sejenis dibaca lewat dua jalur berbeda, dan setiap layar
 * yang menampilkan keduanya harus menggabungkan dua sumber. Daftar harga tetap ada dan tetap
 * berlaku sebagai lapisan di atasnya ({@code price_list_detail}, {@code harga_jual_customer});
 * yang ditambahkan di sini adalah harga DASAR kedua, sederajat dengan yang pertama.</p>
 *
 * <h4>{@code harga_jual_standar} adalah harga KREDIT — dinyatakan, bukan disiratkan</h4>
 * <p>Sejak migrasi ini, pasangannya menjadi eksplisit: {@code harga_jual_standar} memuat
 * {@code HARGAJUAL} (kredit) dan {@code harga_jual_tunai} memuat {@code HARGAJUAL2}. Namanya
 * sengaja TIDAK diubah menjadi {@code harga_jual_kredit}: kolom itu sudah dibaca sebelas helper
 * dan dua puluhan layar, dan mengganti namanya demi kerapian akan menyentuh seluruhnya tanpa
 * mengubah satu pun perilaku. Pasangannya dijelaskan di sini dan di JavaDoc pembacanya.</p>
 *
 * <h4>NULL berarti "tidak ada harga tunai terpisah", bukan nol</h4>
 * <p>Kolomnya boleh NULL dan tidak diberi nilai bawaan. Itu disengaja: 459 dari 626 produk legacy
 * memang tidak punya {@code HARGAJUAL2}, dan menuliskan 0 di sana akan berarti "harga tunainya
 * nol" — yang membuat setiap margin tunai menjadi -100% dan setiap laporan salah. Pembacanya
 * harus jatuh ke {@code harga_jual_standar} ketika kolom ini NULL, dan itulah perilaku yang
 * dipakai layar analisis harga.</p>
 *
 * <h4>Tidak ada pengisian surut</h4>
 * <p>Produk yang sudah ada tetap NULL sampai impor legacy dijalankan ulang atau harganya diisi
 * pengguna. Mengisi surut dari {@code harga_jual_standar} akan mengarang kebijakan dua harga
 * untuk produk yang tidak pernah punya — persis kesalahan yang ingin dihindari kolom ini.</p>
 */
public final class TenantSchemaMigrationsV20 {

	private TenantSchemaMigrationsV20() {
	}

	public static final String[] ERP = {

			"ALTER TABLE {S}.produk ADD COLUMN harga_jual_tunai numeric(18,2)" };
}
