package ais.action.master.inventory;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import ais.database.model.inventory.GrupProduk;
import ais.database.model.inventory.Produk;

/**
 * Logika bersama Grup Produk (harga terpusat lintas toko) -- dipakai layar ZK
 * ({@code GrupProdukAction}) DAN API JSON ({@code GrupProdukApiHelper}, utk JSP e-Kantin
 * serta POS Desktop/Android) supaya aturan penyalinannya SATU, bukan duplikat per platform.
 */
public final class GrupProdukUtil {

	private GrupProdukUtil() {
	}

	/**
	 * Salin harga grup (kolom yang TERISI saja) ke seluruh produk anggota, per-baris lewat
	 * session Hibernate (BUKAN bulk HQL) SENGAJA: {@code Produk} ber-{@code @Audited}, dan bulk
	 * update melewati Envers sehingga perubahan harga massal tidak meninggalkan jejak audit --
	 * untuk perubahan finansial lintas puluhan outlet, jejak audit per-baris justru bagian
	 * terpenting fiturnya. Flush periodik tiap 250 baris (pola commit periodik provisioning demo).
	 *
	 * <p>Berjalan DI DALAM transaksi milik pemanggil (method ini tidak begin/commit sendiri).
	 * Return jumlah baris produk yang benar-benar berubah.</p>
	 */
	/**
	 * "HPP selalu mengikuti Grup Produk" efektif -- kolom baru {@code ikut_hpp} NULL pada
	 * baris lama diperlakukan mengikuti perilaku lama (salin bila hargaBeli terisi) supaya
	 * pelanggan waralaba existing tidak berubah perilakunya tanpa disentuh.
	 */
	public static boolean ikutHpp(GrupProduk grup) {
		return grup.getIkutHpp() != null ? grup.getIkutHpp().booleanValue() : grup.getHargaBeli() != null;
	}

	/** "Harga Jual selalu sama dengan Grup Produk" efektif -- padanan {@link #ikutHpp}. */
	public static boolean ikutHargaJual(GrupProduk grup) {
		return grup.getIkutHargaJual() != null ? grup.getIkutHargaJual().booleanValue()
				: grup.getHargaJual() != null;
	}

	/** Hasil {@link #terapkanHargaKeAnggota}: jumlah anggota yang berubah dan yang dilewati. */
	public static final class HasilTerapkanHarga {
		public final int diubah;
		public final int dilewatiKunciManual;

		HasilTerapkanHarga(int diubah, int dilewatiKunciManual) {
			this.diubah = diubah;
			this.dilewatiKunciManual = dilewatiKunciManual;
		}
	}

	@SuppressWarnings("unchecked")
	public static HasilTerapkanHarga terapkanHargaKeAnggota(Session session, GrupProduk grup) {
		if (grup == null) {
			return new HasilTerapkanHarga(0, 0);
		}
		boolean salinHpp = ikutHpp(grup);
		boolean salinJual = ikutHargaJual(grup);
		if (!salinHpp && !salinJual) {
			// Kedua toggle mati = grup murni pengelompokan; tidak menyentuh produk anggota.
			return new HasilTerapkanHarga(0, 0);
		}
		String bahanGrup = grup.getBahanBaku() == null || grup.getBahanBaku().trim().isEmpty()
				|| "[]".equals(grup.getBahanBaku().trim()) ? null : grup.getBahanBaku();
		java.util.List<Produk> anggota = session.createCriteria(Produk.class)
				.add(Restrictions.eq("grupProduk", grup)).list();
		int diubah = 0;
		int dilewatiKunciManual = 0;
		int diproses = 0;
		for (Produk p : anggota) {
			boolean berubah = false;
			// Kontrak Produk.hargaBeliManual ("kunci harga beli dari faktur"): satu-satunya
			// jalur lain yg menghormatinya adalah KantinHelper.kulakanFakturSimpan -- cakupan
			// yang sama dipakai di sini supaya produk terkunci tidak tertimpa diam-diam saat
			// harga grup diterapkan. bahanBaku/HPP SENGAJA TIDAK ikut dikunci: resep selalu jadi
			// sumber kebenaran HPP di semua jalur lain (ProdukAction, KantinHelper.produkSimpan)
			// terlepas dari hargaBeliManual, jadi menguncinya di sini hanya akan menyimpang dari
			// konvensi yang sudah ada.
			boolean terkunciManual = Boolean.TRUE.equals(p.getHargaBeliManual());
			if (salinHpp && grup.getHargaBeli() != null && !grup.getHargaBeli().equals(p.getHargaBeli())) {
				if (terkunciManual) {
					dilewatiKunciManual++;
				} else {
					p.setHargaBeli(grup.getHargaBeli());
					berubah = true;
				}
			}
			// Resep ikut paket HPP ("HPP termasuk bahan baku"); grup dgn resep kosong TIDAK
			// menghapus resep lokal anggota -- hanya resep terisi yang menyeragamkan.
			if (salinHpp && bahanGrup != null && !bahanGrup.equals(p.getBahanBaku())) {
				p.setBahanBaku(bahanGrup);
				berubah = true;
			}
			if (salinJual && grup.getHargaJual() != null && !grup.getHargaJual().equals(p.getHargaJual())) {
				p.setHargaJual(grup.getHargaJual());
				berubah = true;
			}
			if (berubah) {
				session.saveOrUpdate(p);
				diubah++;
			}
			if (++diproses % 250 == 0) {
				session.flush();
			}
		}
		return new HasilTerapkanHarga(diubah, dilewatiKunciManual);
	}

	/** Jumlah produk anggota satu grup (dipakai daftar/list di semua platform). */
	public static int jumlahAnggota(Session session, GrupProduk grup) {
		Number n = (Number) session.createCriteria(Produk.class)
				.setProjection(org.hibernate.criterion.Projections.rowCount())
				.add(Restrictions.eq("grupProduk", grup)).uniqueResult();
		return n == null ? 0 : n.intValue();
	}
}
