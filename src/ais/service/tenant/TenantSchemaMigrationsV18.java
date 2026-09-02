package ais.service.tenant;

/**
 * <h3>Migrasi tenant v18 — jenis pembayaran dan termin per hutang.</h3>
 *
 * <p>Dua kolom pada {@code hutang_supplier}: {@code jenis_pembayaran} dan {@code termin_hari}.</p>
 *
 * <h4>Legacy memakai tabel terpisah; tenant tidak memerlukannya</h4>
 * <p>Jalur legacy menyimpan syarat pembayaran pada tabel tersendiri,
 * {@code payable_faktur_info}, yang menggantung satu-ke-satu pada faktur pengadaan. Tabel itu
 * memuat empat hal: jenis pembayaran, termin, jatuh tempo, dan uang muka.</p>
 * <p>Pada model tenant tiga dari empat sudah punya rumah. {@code jatuh_tempo} memang sudah ada
 * pada {@code hutang_supplier} sejak awal, dan uang muka — lihat bawah — bukan kolom melainkan
 * dokumen. Yang benar-benar kurang hanya dua, dan keduanya adalah keterangan tentang hutang
 * yang sama. Membuat tabel satu-ke-satu untuk dua kolom hanya menambah join tanpa menambah
 * apa pun.</p>
 *
 * <h4>{@code dibayarAwal} sengaja TIDAK dibuatkan kolom</h4>
 * <p>Ini keputusan yang sama dengan yang sudah diambil di sisi piutang, dan alasannya sama.</p>
 * <p>Sisa hutang pada model tenant dihitung {@code nilai − Σalokasi} — satu sumber, dan setiap
 * pembayaran adalah dokumen dengan alokasinya sendiri. Menambahkan kolom uang muka berarti
 * memperkenalkan <b>pengurang kedua</b> yang tidak berasal dari dokumen mana pun: sisa hutang
 * akan berkurang tanpa ada pembayaran yang dapat ditunjuk, dan rekonsiliasi terhadap kas
 * kehilangan pasangannya.</p>
 * <p>Pada model tenant, uang muka <b>adalah pembayaran</b> — dicatat lewat aksi pembayaran
 * hutang, yang menerbitkan dokumennya berikut alokasinya. Jalur tenant karena itu menolak
 * {@code dibayar_awal} pada aksi ini dan menunjuk ke aksi yang tepat, alih-alih menyimpannya di
 * tempat yang membuat angkanya bercabang.</p>
 *
 * <h4>Jatuh tempo diturunkan dari termin, dan itu tetap ditulis</h4>
 * <p>{@code jatuh_tempo} bisa saja dihitung dari {@code tanggal + termin_hari}. Ia tetap
 * disimpan karena termin dapat diubah belakangan sementara jatuh tempo yang sudah disepakati
 * dengan pemasok tidak ikut berubah begitu saja — dan karena seluruh kueri umur hutang membacanya
 * langsung. Aksi yang mengubah termin memperbaruinya bersama-sama, dalam satu transaksi.</p>
 *
 * <h4>Apa yang TIDAK dilakukan bundel ini</h4>
 * <p>Tidak ada pengisian surut. Hutang yang sudah ada tetap {@code NULL} pada kedua kolom, yang
 * berarti syaratnya tidak tercatat — bukan CREDIT dengan termin nol. Menebak bahwa hutang lama
 * pasti kredit akan memberi mereka syarat yang tidak pernah disepakati.</p>
 */
public final class TenantSchemaMigrationsV18 {

	private TenantSchemaMigrationsV18() {
	}

	public static final String[] ERP = {

			"ALTER TABLE {S}.hutang_supplier ADD COLUMN jenis_pembayaran varchar(32)",

			"ALTER TABLE {S}.hutang_supplier ADD COLUMN termin_hari integer",

			"CREATE INDEX idx_{SU}_hutang_supplier_jenis ON {S}.hutang_supplier"
					+ " (jenis_pembayaran)" };
}
