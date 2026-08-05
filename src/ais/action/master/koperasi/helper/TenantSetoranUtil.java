package ais.action.master.koperasi.helper;

import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.database.model.inventory.SetoranTenant;
import ais.database.model.inventory.Toko;

/**
 * <h2>TenantSetoranUtil — mesin pusat Setoran &amp; Bagi Hasil Tenant.</h2>
 *
 * <p>
 * Menyediakan operasi baca/simpan/hapus {@link SetoranTenant} yang dipakai BERSAMA oleh versi
 * <b>JSP</b> dan <b>ZKoss</b>, sehingga aturan perhitungan (bagi hasil, total kewajiban, status
 * lunas/kurang) tidak diduplikasi. Perhitungan: bila {@code nilaiBagiHasil} belum diisi namun
 * {@code persen} &gt; 0 maka {@code nilaiBagiHasil = omzet × persen / 100}; total kewajiban =
 * {@code nilaiBagiHasil + sewa + biaya}; {@code status} = LUNAS bila {@code setoran ≥ kewajiban},
 * selain itu KURANG.
 * </p>
 *
 * <p>
 * <b>Sesi:</b> memakai {@link Session} milik pemanggil dan menyimpan lewat
 * {@link Common#refreshSaveOrUpdate(Session, ais.database.model.GeneralValueObject)} /
 * {@link Common#refreshDelete(Session, ais.database.model.GeneralValueObject)}; tidak membuka/menutup
 * sesi sendiri. Kompatibel Java 1.7.
 * </p>
 *
 * @author AIS e-Kantin (modul tenant)
 * @see SetoranTenant
 */
public final class TenantSetoranUtil {

	private TenantSetoranUtil() {
	}

	/** Nilai aman-null untuk Double (mengembalikan 0.0 bila null). Reusable lintas layar tenant. */
	public static double nz(Double d) {
		return d == null ? 0.0 : d.doubleValue();
	}

	/** Total kewajiban tenant = bagi hasil + sewa + biaya layanan. Aman terhadap field null. */
	public static double kewajiban(SetoranTenant s) {
		if (s == null) {
			return 0.0;
		}
		return nz(s.getNilaiBagiHasil()) + nz(s.getSewa()) + nz(s.getBiayaLayanan());
	}

	/** Daftar setoran (opsional dibatasi toko), terbaru dahulu. */
	@SuppressWarnings("unchecked")
	public static List<SetoranTenant> daftar(Session session, Long tokoId, int max) {
		Criteria c = session.createCriteria(SetoranTenant.class);
		if (tokoId != null) {
			c.add(Restrictions.eq("toko.id", tokoId));
		}
		c.addOrder(Order.desc("tanggal")).addOrder(Order.desc("id"));
		if (max > 0) {
			c.setMaxResults(max);
		}
		return c.list();
	}

	/**
	 * Menyimpan (tambah/ubah) satu baris setoran tenant, sekaligus menghitung nilai bagi hasil (bila
	 * memakai persentase), dan menetapkan status LUNAS/KURANG.
	 *
	 * @param id null untuk tambah, atau id yang diubah.
	 * @return entity tersimpan.
	 */
	public static SetoranTenant simpan(Session session, Long id, Toko toko, Date tanggal, String periode, double omzet,
			double persen, double nilaiBagiHasil, double sewa, double biaya, double setoran, String keterangan,
			String oleh, String olehId) {
		SetoranTenant s = (id == null) ? new SetoranTenant() : (SetoranTenant) session.get(SetoranTenant.class, id);
		if (s == null) {
			s = new SetoranTenant();
		}
		if (nilaiBagiHasil <= 0 && persen > 0) {
			nilaiBagiHasil = omzet * persen / 100.0;
		}
		s.setToko(toko);
		s.setTanggal(tanggal == null ? new Date() : tanggal);
		s.setPeriode(periode);
		s.setOmzet(Double.valueOf(omzet));
		s.setPersenBagiHasil(Double.valueOf(persen));
		s.setNilaiBagiHasil(Double.valueOf(nilaiBagiHasil));
		s.setSewa(Double.valueOf(sewa));
		s.setBiayaLayanan(Double.valueOf(biaya));
		s.setSetoran(Double.valueOf(setoran));
		double keharusan = nilaiBagiHasil + sewa + biaya;
		s.setStatus(setoran >= keharusan ? SetoranTenant.STATUS_LUNAS : SetoranTenant.STATUS_KURANG);
		s.setKeterangan(keterangan);
		if (id == null) {
			s.setOleh(oleh);
			s.setOlehId(olehId);
		}
		Common.refreshSaveOrUpdate(session, s);
		return s;
	}

	/** Menghapus satu baris setoran tenant. */
	public static void hapus(Session session, Long id) {
		if (id == null) {
			return;
		}
		SetoranTenant s = (SetoranTenant) session.get(SetoranTenant.class, id);
		if (s != null) {
			Common.refreshDelete(session, s);
		}
	}
}
