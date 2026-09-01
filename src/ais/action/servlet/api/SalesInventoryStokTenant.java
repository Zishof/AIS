package ais.action.servlet.api;

import ais.service.tenant.TenantAccessException;
import ais.service.tenant.TenantContext;
import ais.service.tenant.TenantMutasiStok;
import ais.service.tenant.TenantSchemaService;

/**
 * <h3>Jalur schema tenant untuk Persediaan &amp; Kartu Stok (P4, helper pertama).</h3>
 *
 * <p>Jalur legacy di {@link SalesInventoryStokHelper} <b>tidak diubah sama sekali</b>. Kelas ini
 * menyediakan SQL pengganti untuk aktor yang punya schema tenant, dan hanya bagian
 * {@code SELECT}-nya yang berbeda -- pembungkus, penghitungan total, paginasi, dan perakitan
 * JSON di helper itu dipakai bersama. Karena kolom keluarannya sama persis dan berurutan sama,
 * tidak ada satu baris pun perakitan hasil yang perlu digandakan.</p>
 *
 * <h4>Mengapa bukan sekadar mengganti prefiks schema</h4>
 * <p>Schema tenant bukan cermin {@code koperasi}. Legacy menjumlahkan delapan tabel terpisah;
 * tenant memakai satu buku besar {@code mutasi_stok} ({@link TenantMutasiStok}). Lima dari
 * sepuluh tabel yang dipakai jalur legacy tidak ada di sisi tenant, dan empat dari lima kolom
 * {@code produk} berganti nama. Rinciannya di
 * {@code docs/tenant-inventory-sales/04-refactor-si.md}.</p>
 *
 * <h4>Satu perbedaan hasil yang DISENGAJA</h4>
 * <p>Kondisi rentang legacy adalah {@code BETWEEN dari AND (sampai + INTERVAL 1 day)} atas
 * kolom {@code timestamp}, sehingga peristiwa tepat pukul 00:00:00 pada H+1 <b>ikut terhitung
 * ke rentang sebelumnya</b>. Itu salah-hitung batas; sudah diperagakan pada uji kesetaraan
 * (selisih 1.000 unit untuk satu baris).</p>
 * <p>{@code mutasi_stok.tanggal} bertipe {@code date}, jadi jalur tenant tidak dapat -- dan
 * tidak boleh -- meniru cacat itu. Ia memakai {@code BETWEEN dari AND sampai}. Meniru cacat
 * legacy demi angka yang sama berarti mengabadikannya di model baru.</p>
 *
 * <h4>Filter toko: gagal-tertutup, bukan diabaikan</h4>
 * <p>Model tenant tidak punya {@code produk.toko}; lingkupnya {@code gudang}/{@code lokasi_stok}
 * dan penegakannya bagian &sect;16 yang belum dikerjakan. Permintaan bersaring toko karena itu
 * <b>ditolak</b>, bukan dijalankan tanpa saringan -- mengabaikan saringan lingkup berarti
 * menyajikan data di luar wewenang peminta.</p>
 */
final class SalesInventoryStokTenant {

	private SalesInventoryStokTenant() {
	}

	/** Benar bila aktor ini dilayani schema tenant, bukan schema bersama {@code koperasi}. */
	static boolean aktif(EbisnisActorContextResolver.ActorContext aktor) {
		return aktor != null && aktor.tenant != null
				&& aktor.tenant.getSchemaName() != null
				&& aktor.tenant.getSchemaName().trim().length() > 0;
	}

	/**
	 * Prefiks schema berikut titiknya. Divalidasi {@link TenantSchemaService#pastikanAman} --
	 * validator yang sama dengan jalur {@code {S}}, bukan pengutipan sendiri.
	 */
	static String skema(TenantContext tenant) {
		String s = tenant == null ? null : tenant.getSchemaName();
		if (s == null || s.trim().length() == 0) {
			throw new TenantAccessException(TenantAccessException.TENANT_SCHEMA_INVALID,
					"Tenant ini tidak memiliki schema.");
		}
		try {
			return "\"" + TenantSchemaService.pastikanAman(s.trim()) + "\".";
		} catch (IllegalArgumentException e) {
			throw new TenantAccessException(TenantAccessException.TENANT_SCHEMA_INVALID,
					"Konfigurasi schema tenant tidak sah.", e);
		}
	}

	/** Penjumlah satu ember dari buku besar, sebagai subkueri terkorelasi ke {@code p.id}. */
	private static String ember(String skema, String pilih, String syarat, String dari,
			String sampai) {
		return "COALESCE((SELECT SUM(" + pilih + ") FROM " + skema + "mutasi_stok m"
				+ " WHERE m.produk_id = p.id" + syarat
				+ " AND m.tanggal BETWEEN DATE '" + dari + "' AND DATE '" + sampai + "'),0)";
	}

	/**
	 * {@code SELECT} pengganti untuk {@code si_inventory_balance}, berkolom sama dan berurutan
	 * sama dengan jalur legacy: id, kode, barcode, nama, satuan, harga_beli, harga_jual,
	 * stok_minimum, awal, masuk, keluar, opname.
	 *
	 * <p>{@code dari}/{@code sampai} sudah divalidasi {@code yyyy-MM-dd} oleh pemanggil; ia
	 * disambung sebagai literal DATE, bukan parameter, karena berada di dalam subkueri
	 * terkorelasi yang urutan parameternya harus tetap cocok dengan {@code where}.</p>
	 */
	static String selectSaldo(String skema, String dari, String sampai, String where) {
		String bukanOpname = " AND m.jenis <> '" + TenantMutasiStok.OPNAME + "'";
		String hanyaOpname = " AND m.jenis = '" + TenantMutasiStok.OPNAME + "'";
		// Awal = saldo bersih SELURUH riwayat sebelum tanggal mulai -- termasuk opname,
		// sebab saldo pembuka memang sudah memuat penyesuaian sebelumnya.
		String awal = "COALESCE((SELECT SUM(m.arah * m.kuantitas) FROM " + skema + "mutasi_stok m"
				+ " WHERE m.produk_id = p.id AND m.tanggal < DATE '" + dari + "'),0)";
		String masuk = ember(skema, "m.kuantitas",
				" AND m.arah = " + TenantMutasiStok.MASUK + bukanOpname, dari, sampai);
		String keluar = ember(skema, "m.kuantitas",
				" AND m.arah = " + TenantMutasiStok.KELUAR + bukanOpname, dari, sampai);
		String opname = ember(skema, "m.arah * m.kuantitas", hanyaOpname, dari, sampai);
		return "SELECT p.id AS id, p.kode AS kode, COALESCE(p.barcode,'') AS barcode, "
				+ "p.nama AS nama, COALESCE(NULLIF(TRIM(s.nama),''),'(Belum diatur)') AS satuan, "
				+ "COALESCE(p.harga_beli_terakhir,0) AS harga_beli, "
				+ "COALESCE(p.harga_jual_standar,0) AS harga_jual, "
				+ "COALESCE(p.stok_minimum,0) AS stok_minimum, "
				+ awal + " AS awal, " + masuk + " AS masuk, " + keluar + " AS keluar, "
				+ opname + " AS opname "
				+ "FROM " + skema + "produk p LEFT JOIN " + skema + "satuan s ON p.satuan_id = s.id"
				+ where;
	}

	/**
	 * Kartu stok per produk. Legacy menyatukan sembilan sumber lewat {@code UNION ALL}; di sisi
	 * tenant seluruhnya sudah satu tabel, sehingga tidak ada penyatuan sama sekali.
	 *
	 * <p>Label {@code jenis} diterjemahkan ke teks yang sama dengan kartu legacy supaya
	 * tampilannya tidak berubah bagi pengguna yang pindah ke tenant.</p>
	 */
	static String sqlKartu(String skema, long produkId, String dari, String sampai) {
		return "SELECT m.tanggal AS waktu, CASE m.jenis"
				+ " WHEN '" + TenantMutasiStok.PENGADAAN + "' THEN 'Kulakan/Pengadaan'"
				+ " WHEN '" + TenantMutasiStok.PENJUALAN + "' THEN 'Penjualan'"
				+ " WHEN '" + TenantMutasiStok.PEMAKAIAN_BAHAN + "' THEN 'Pemakaian Bahan Baku'"
				+ " WHEN '" + TenantMutasiStok.RETUR_PENJUALAN
				+ "' THEN 'Retur Penjualan (kembali ke stok)'"
				+ " WHEN '" + TenantMutasiStok.RETUR_PEMBELIAN + "' THEN 'Retur Pembelian'"
				+ " WHEN '" + TenantMutasiStok.MUTASI_MASUK + "' THEN 'Mutasi Masuk (antar toko)'"
				+ " WHEN '" + TenantMutasiStok.MUTASI_KELUAR + "' THEN 'Mutasi Keluar (antar toko)'"
				+ " WHEN '" + TenantMutasiStok.OPNAME + "' THEN 'Penyesuaian Opname'"
				+ " WHEN '" + TenantMutasiStok.PRODUKSI + "' THEN 'Produksi'"
				+ " ELSE m.jenis END AS jenis,"
				+ " COALESCE(NULLIF(m.nomor_dokumen,''), COALESCE('#' || m.dokumen_id, '')) AS referensi,"
				+ " CASE WHEN m.arah = " + TenantMutasiStok.MASUK
				+ " THEN m.kuantitas ELSE 0 END AS masuk,"
				+ " CASE WHEN m.arah = " + TenantMutasiStok.KELUAR
				+ " THEN m.kuantitas ELSE 0 END AS keluar"
				+ " FROM " + skema + "mutasi_stok m WHERE m.produk_id = " + produkId
				+ " AND m.tanggal BETWEEN DATE '" + dari + "' AND DATE '" + sampai + "'"
				+ " ORDER BY 1 ASC, 2 ASC";
	}

	/** Saldo pembuka sebelum {@code dari}, untuk baris "Saldo Awal" pada kartu stok. */
	static String sqlSaldoAwal(String skema, long produkId, String dari) {
		return "SELECT COALESCE(SUM(m.arah * m.kuantitas),0) FROM " + skema + "mutasi_stok m"
				+ " WHERE m.produk_id = " + produkId + " AND m.tanggal < DATE '" + dari + "'";
	}
}
