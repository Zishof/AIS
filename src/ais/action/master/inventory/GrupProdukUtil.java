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
	@SuppressWarnings("unchecked")
	public static int terapkanHargaKeAnggota(Session session, GrupProduk grup) {
		if (grup == null || (grup.getHargaBeli() == null && grup.getHargaJual() == null)) {
			return 0;
		}
		java.util.List<Produk> anggota = session.createCriteria(Produk.class)
				.add(Restrictions.eq("grupProduk", grup)).list();
		int diubah = 0;
		int diproses = 0;
		for (Produk p : anggota) {
			boolean berubah = false;
			if (grup.getHargaBeli() != null && !grup.getHargaBeli().equals(p.getHargaBeli())) {
				p.setHargaBeli(grup.getHargaBeli());
				berubah = true;
			}
			if (grup.getHargaJual() != null && !grup.getHargaJual().equals(p.getHargaJual())) {
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
		return diubah;
	}

	/** Jumlah produk anggota satu grup (dipakai daftar/list di semua platform). */
	public static int jumlahAnggota(Session session, GrupProduk grup) {
		Number n = (Number) session.createCriteria(Produk.class)
				.setProjection(org.hibernate.criterion.Projections.rowCount())
				.add(Restrictions.eq("grupProduk", grup)).uniqueResult();
		return n == null ? 0 : n.intValue();
	}
}
