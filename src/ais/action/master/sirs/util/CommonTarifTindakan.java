package ais.action.master.sirs.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.sirs.Asuransi;
import ais.database.model.sirs.Biaya;
import ais.database.model.sirs.BiayaTindakanPerKelas;
import ais.database.model.sirs.Dokter;
import ais.database.model.sirs.JenisBiaya;
import ais.database.model.sirs.KelasPerawatan;
import ais.database.model.sirs.Komunitas;
import ais.database.model.sirs.Pasien;
import ais.database.model.sirs.TarifKhususPunyaTindakan;
import ais.database.model.sirs.Tindakan;

public class CommonTarifTindakan {

	public static BiayaTindakanPerKelas getBiayaTindakanPerKelas(Tindakan tindakan, KelasPerawatan kelasPerawatan) {
		TarifKhususPunyaTindakan tarifKhususPunyaTindakan = null;
		return getBiayaTindakanPerKelas(tindakan, kelasPerawatan, tarifKhususPunyaTindakan);
	}

	public static BiayaTindakanPerKelas getBiayaTindakanPerKelas(Tindakan tindakan, KelasPerawatan kelasPerawatan,
			Dokter dokter, Asuransi asuransi, Set<Komunitas> komunitas, Pasien pasien) {
		TarifKhususPunyaTindakan tarifKhususPunyaTindakan = getTarifKhususPunyaTindakan(tindakan, dokter, asuransi,
				komunitas, pasien);
		return getBiayaTindakanPerKelas(tindakan, kelasPerawatan, tarifKhususPunyaTindakan);
	}

	public static BiayaTindakanPerKelas getBiayaTindakanPerKelas(TarifKhususPunyaTindakan tarifKhususPunyaTindakan,
			KelasPerawatan kelasPerawatan) {
		return getBiayaTindakanPerKelas(tarifKhususPunyaTindakan.getTindakan(), kelasPerawatan,
				tarifKhususPunyaTindakan);
	}

	public static TarifKhususPunyaTindakan getTarifKhususPunyaTindakan(Tindakan tindakan, Dokter dokter,
			Asuransi asuransi, Set<Komunitas> komunitas, Pasien pasien) {
		return (TarifKhususPunyaTindakan) CommonTarif.getTarif(TarifKhususPunyaTindakan.class,
				Restrictions.eq("tindakan", tindakan), dokter, asuransi, komunitas, pasien);
	}

	public static BiayaTindakanPerKelas getBiayaTindakanPerKelas(Tindakan tindakan, KelasPerawatan kelasPerawatan,
			TarifKhususPunyaTindakan tarifKhususPunyaTindakan) {

		Session session = HibernateUtil.currentSession();

		BiayaTindakanPerKelas b;
		if (tarifKhususPunyaTindakan != null) {
			b = (BiayaTindakanPerKelas) session.createCriteria(BiayaTindakanPerKelas.class)
					.add(Restrictions.isNull("tindakan")).add(Restrictions.eq("kelasPerawatan", kelasPerawatan))
					.add(Restrictions.eq("tarifKhususPunyaTindakan", tarifKhususPunyaTindakan)).setMaxResults(1)
					.uniqueResult();
			if (b == null) {

				Number biaya = (Number) session.createCriteria(BiayaTindakanPerKelas.class)
						.setProjection(Projections.property("biaya")).add(Restrictions.eq("tindakan", tindakan))
						.add(Restrictions.eq("kelasPerawatan", kelasPerawatan))
						.add(Restrictions.isNull("tarifKhususPunyaTindakan")).setMaxResults(1).uniqueResult();

				b = new BiayaTindakanPerKelas();
				b.setBiaya(biaya == null ? 0.0 : biaya.doubleValue());
				b.setTarifKhususPunyaTindakan(tarifKhususPunyaTindakan);
				b.setKelasPerawatan(kelasPerawatan);
				b.setTindakan(null);
				session.save(b);
			}
		} else {
			b = (BiayaTindakanPerKelas) session.createCriteria(BiayaTindakanPerKelas.class)
					.add(Restrictions.eq("tindakan", tindakan)).add(Restrictions.eq("kelasPerawatan", kelasPerawatan))
					.add(Restrictions.isNull("tarifKhususPunyaTindakan")).setMaxResults(1).uniqueResult();
			if (b == null) {
				b = new BiayaTindakanPerKelas();
				b.setBiaya(0.0);
				b.setTarifKhususPunyaTindakan(null);
				b.setKelasPerawatan(kelasPerawatan);
				b.setTindakan(tindakan);
				session.save(b);
			}
		}
		return b;
	}

	public static BiayaTindakanPerKelas getBiayaTindakanPerKelas(Tindakan tindakan, KelasPerawatan kelasPerawatan,
			TarifKhususPunyaTindakan tarifKhususPunyaTindakan, Session session) {
		BiayaTindakanPerKelas b;
		if (tarifKhususPunyaTindakan != null) {
			b = (BiayaTindakanPerKelas) session.createCriteria(BiayaTindakanPerKelas.class)
					.add(Restrictions.isNull("tindakan")).add(Restrictions.eq("kelasPerawatan", kelasPerawatan))
					.add(Restrictions.eq("tarifKhususPunyaTindakan", tarifKhususPunyaTindakan)).setMaxResults(1)
					.uniqueResult();
			if (b == null) {

				Number biaya = (Number) session.createCriteria(BiayaTindakanPerKelas.class)
						.setProjection(Projections.property("biaya")).add(Restrictions.eq("tindakan", tindakan))
						.add(Restrictions.eq("kelasPerawatan", kelasPerawatan))
						.add(Restrictions.isNull("tarifKhususPunyaTindakan")).setMaxResults(1).uniqueResult();

				b = new BiayaTindakanPerKelas();
				b.setBiaya(biaya == null ? 0.0 : biaya.doubleValue());
				b.setTarifKhususPunyaTindakan(tarifKhususPunyaTindakan);
				b.setKelasPerawatan(kelasPerawatan);
				b.setTindakan(null);
				session.getTransaction().begin();
				session.save(b);
				session.getTransaction().commit();
			}
		} else {
			b = (BiayaTindakanPerKelas) session.createCriteria(BiayaTindakanPerKelas.class)
					.add(Restrictions.eq("tindakan", tindakan)).add(Restrictions.eq("kelasPerawatan", kelasPerawatan))
					.add(Restrictions.isNull("tarifKhususPunyaTindakan")).setMaxResults(1).uniqueResult();
			if (b == null) {
				b = new BiayaTindakanPerKelas();
				b.setBiaya(0.0);
				b.setTarifKhususPunyaTindakan(null);
				b.setKelasPerawatan(kelasPerawatan);
				b.setTindakan(tindakan);
				session.getTransaction().begin();
				session.save(b);
				session.getTransaction().commit();
			}
		}
		return b;
	}

	@SuppressWarnings("unchecked")
	public static List<JenisBiaya> getJenisBiayas(Tindakan tindakan, String jenisPaket,
			TarifKhususPunyaTindakan tarifKhususPunyaTindakan) {
		Session session = HibernateUtil.currentSession();
		List<JenisBiaya> tempJenisBiayas = new ArrayList<JenisBiaya>();
		if (tindakan != null && tindakan.getId() != null) {
			tempJenisBiayas = session.createCriteria(Biaya.class).add(Restrictions.isNull("detailTransaksiLayanan"))
					.add(Restrictions.isNull("detailTransaksi")).setProjection(Projections.groupProperty("jenisBiaya"))
					.createAlias("biayaTindakanPerKelas", "biayaTindakanPerKelas")
					.createAlias("jenisBiaya", "jenisBiaya").add(Restrictions.eq("jenisBiaya.aktif", true))
					.add(Restrictions.eq("jenisBiaya.tipe",
							jenisPaket.equals(Tindakan.JENIS_PERAWATAN_PAKET) ? JenisBiaya.TIPE_PAKET
									: JenisBiaya.TIPE_TINDAKAAN))

					.add(tarifKhususPunyaTindakan == null
							? Restrictions.and(Restrictions.eq("biayaTindakanPerKelas.tindakan", tindakan),
									Restrictions.isNull("biayaTindakanPerKelas.tarifKhususPunyaTindakan"))
							: Restrictions.and(
									Restrictions.eq("biayaTindakanPerKelas.tarifKhususPunyaTindakan",
											tarifKhususPunyaTindakan),
									Restrictions.isNull("biayaTindakanPerKelas.tindakan")))

					.list();
		}

		if (tempJenisBiayas.isEmpty()) {
			tempJenisBiayas.addAll(session.createCriteria(JenisBiaya.class).add(Restrictions.eq("defaultAktif", true))
					.add(Restrictions.eq("tipe",
							jenisPaket.equals(Tindakan.JENIS_PERAWATAN_PAKET) ? JenisBiaya.TIPE_PAKET
									: JenisBiaya.TIPE_TINDAKAAN))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.asc("nama")).list());
		}

		return tempJenisBiayas;
	}

	@SuppressWarnings("unchecked")
	public static List<Biaya> getBiayaPerJenis(Tindakan tindakan, TarifKhususPunyaTindakan tarifKhususPunyaTindakan,
			JenisBiaya jenisBiaya) {
		Session session = HibernateUtil.currentSession();
		List<Biaya> biayas = session.createCriteria(Biaya.class).add(Restrictions.isNull("detailTransaksiLayanan"))
				.add(Restrictions.isNull("detailTransaksi"))
				.createAlias("biayaTindakanPerKelas", "biayaTindakanPerKelas")
				.add(Restrictions.eq("jenisBiaya", jenisBiaya))

				.add(tarifKhususPunyaTindakan == null
						? Restrictions.and(Restrictions.eq("biayaTindakanPerKelas.tindakan", tindakan),
								Restrictions.isNull("biayaTindakanPerKelas.tarifKhususPunyaTindakan"))
						: Restrictions.and(
								Restrictions.eq("biayaTindakanPerKelas.tarifKhususPunyaTindakan",
										tarifKhususPunyaTindakan),
								Restrictions.isNull("biayaTindakanPerKelas.tindakan")))

				.list();

		return biayas;
	}

}
