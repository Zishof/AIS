package ais.action.master.sirs.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.sirs.AlatMedis;
import ais.database.model.sirs.Asuransi;
import ais.database.model.sirs.Biaya;
import ais.database.model.sirs.BiayaAlatMedisPerKelas;
import ais.database.model.sirs.Dokter;
import ais.database.model.sirs.JenisBiaya;
import ais.database.model.sirs.KelasPerawatan;
import ais.database.model.sirs.Komunitas;
import ais.database.model.sirs.Pasien;
import ais.database.model.sirs.TarifKhususPunyaAlatMedis;

public class CommonTarifAlatMedis {

	public static BiayaAlatMedisPerKelas getBiayaAlatMedisPerKelas(AlatMedis alatMedis, KelasPerawatan kelasPerawatan) {
		TarifKhususPunyaAlatMedis tarifKhususPunyaAlatMedis = null;
		return getBiayaAlatMedisPerKelas(alatMedis, kelasPerawatan, tarifKhususPunyaAlatMedis);
	}

	public static BiayaAlatMedisPerKelas getBiayaAlatMedisPerKelas(AlatMedis alatMedis, KelasPerawatan kelasPerawatan,
			Dokter dokter, Asuransi asuransi, Set<Komunitas> komunitas, Pasien pasien) {
		TarifKhususPunyaAlatMedis tarifKhususPunyaAlatMedis = getTarifKhususPunyaAlatMedis(alatMedis, dokter, asuransi,
				komunitas, pasien);
		return getBiayaAlatMedisPerKelas(alatMedis, kelasPerawatan, tarifKhususPunyaAlatMedis);
	}

	public static BiayaAlatMedisPerKelas getBiayaAlatMedisPerKelas(TarifKhususPunyaAlatMedis tarifKhususPunyaAlatMedis,
			KelasPerawatan kelasPerawatan) {
		return getBiayaAlatMedisPerKelas(tarifKhususPunyaAlatMedis.getAlatMedis(), kelasPerawatan,
				tarifKhususPunyaAlatMedis);
	}

	public static TarifKhususPunyaAlatMedis getTarifKhususPunyaAlatMedis(AlatMedis alatMedis, Dokter dokter,
			Asuransi asuransi, Set<Komunitas> komunitas, Pasien pasien) {

		return (TarifKhususPunyaAlatMedis) CommonTarif.getTarif(TarifKhususPunyaAlatMedis.class,
				Restrictions.eq("alatMedis", alatMedis), dokter, asuransi, komunitas, pasien);

	}

	public static BiayaAlatMedisPerKelas getBiayaAlatMedisPerKelas(AlatMedis alatMedis, KelasPerawatan kelasPerawatan,
			TarifKhususPunyaAlatMedis tarifKhususPunyaAlatMedis) {

		Session session = HibernateUtil.currentSession();

		BiayaAlatMedisPerKelas b;
		if (tarifKhususPunyaAlatMedis != null) {
			b = (BiayaAlatMedisPerKelas) session.createCriteria(BiayaAlatMedisPerKelas.class)
					.add(Restrictions.isNull("alatMedis")).add(Restrictions.eq("kelasPerawatan", kelasPerawatan))
					.add(Restrictions.eq("tarifKhususPunyaAlatMedis", tarifKhususPunyaAlatMedis)).setMaxResults(1)
					.uniqueResult();
			if (b == null) {

				Number biaya = (Number) session.createCriteria(BiayaAlatMedisPerKelas.class)
						.setProjection(Projections.property("biaya")).add(Restrictions.eq("alatMedis", alatMedis))
						.add(Restrictions.eq("kelasPerawatan", kelasPerawatan))
						.add(Restrictions.isNull("tarifKhususPunyaAlatMedis")).setMaxResults(1).uniqueResult();

				b = new BiayaAlatMedisPerKelas();
				b.setBiaya(biaya == null ? 0.0 : biaya.doubleValue());
				b.setTarifKhususPunyaAlatMedis(tarifKhususPunyaAlatMedis);
				b.setKelasPerawatan(kelasPerawatan);
				b.setAlatMedis(null);
				session.save(b);
			}
		} else {
			b = (BiayaAlatMedisPerKelas) session.createCriteria(BiayaAlatMedisPerKelas.class)
					.add(Restrictions.eq("alatMedis", alatMedis)).add(Restrictions.eq("kelasPerawatan", kelasPerawatan))
					.add(Restrictions.isNull("tarifKhususPunyaAlatMedis")).setMaxResults(1).uniqueResult();
			if (b == null) {
				b = new BiayaAlatMedisPerKelas();
				b.setBiaya(0.0);
				b.setTarifKhususPunyaAlatMedis(null);
				b.setKelasPerawatan(kelasPerawatan);
				b.setAlatMedis(alatMedis);
				session.save(b);
			}
		}
		return b;
	}

	public static BiayaAlatMedisPerKelas getBiayaAlatMedisPerKelas(AlatMedis alatMedis, KelasPerawatan kelasPerawatan,
			TarifKhususPunyaAlatMedis tarifKhususPunyaAlatMedis, Session session) {
		BiayaAlatMedisPerKelas b;
		if (tarifKhususPunyaAlatMedis != null) {
			b = (BiayaAlatMedisPerKelas) session.createCriteria(BiayaAlatMedisPerKelas.class)
					.add(Restrictions.isNull("alatMedis")).add(Restrictions.eq("kelasPerawatan", kelasPerawatan))
					.add(Restrictions.eq("tarifKhususPunyaAlatMedis", tarifKhususPunyaAlatMedis)).setMaxResults(1)
					.uniqueResult();
			if (b == null) {

				Number biaya = (Number) session.createCriteria(BiayaAlatMedisPerKelas.class)
						.setProjection(Projections.property("biaya")).add(Restrictions.eq("alatMedis", alatMedis))
						.add(Restrictions.eq("kelasPerawatan", kelasPerawatan))
						.add(Restrictions.isNull("tarifKhususPunyaAlatMedis")).setMaxResults(1).uniqueResult();

				b = new BiayaAlatMedisPerKelas();
				b.setBiaya(biaya == null ? 0.0 : biaya.doubleValue());
				b.setTarifKhususPunyaAlatMedis(tarifKhususPunyaAlatMedis);
				b.setKelasPerawatan(kelasPerawatan);
				b.setAlatMedis(null);
				session.getTransaction().begin();
				session.save(b);
				session.getTransaction().commit();
			}
		} else {
			b = (BiayaAlatMedisPerKelas) session.createCriteria(BiayaAlatMedisPerKelas.class)
					.add(Restrictions.eq("alatMedis", alatMedis)).add(Restrictions.eq("kelasPerawatan", kelasPerawatan))
					.add(Restrictions.isNull("tarifKhususPunyaAlatMedis")).setMaxResults(1).uniqueResult();
			if (b == null) {
				b = new BiayaAlatMedisPerKelas();
				b.setBiaya(0.0);
				b.setTarifKhususPunyaAlatMedis(null);
				b.setKelasPerawatan(kelasPerawatan);
				b.setAlatMedis(alatMedis);
				session.getTransaction().begin();
				session.save(b);
				session.getTransaction().commit();
			}
		}
		return b;
	}

	@SuppressWarnings("unchecked")
	public static List<JenisBiaya> getJenisBiayas(AlatMedis alatMedis,
			TarifKhususPunyaAlatMedis tarifKhususPunyaAlatMedis) {
		Session session = HibernateUtil.currentSession();
		List<JenisBiaya> tempJenisBiayas = new ArrayList<JenisBiaya>();
		if (alatMedis != null && alatMedis.getId() != null) {
			tempJenisBiayas = session.createCriteria(Biaya.class).add(Restrictions.isNull("detailTransaksiLayanan"))
					.add(Restrictions.isNull("detailTransaksi")).setProjection(Projections.groupProperty("jenisBiaya"))
					.createAlias("biayaAlatMedisPerKelas", "biayaAlatMedisPerKelas")
					.createAlias("jenisBiaya", "jenisBiaya").add(Restrictions.eq("jenisBiaya.aktif", true))
					.add(Restrictions.eq("jenisBiaya.tipe", JenisBiaya.TIPE_ALAT_MEDIS))

					.add(tarifKhususPunyaAlatMedis == null
							? Restrictions.and(Restrictions.eq("biayaAlatMedisPerKelas.alatMedis", alatMedis),
									Restrictions.isNull("biayaAlatMedisPerKelas.tarifKhususPunyaAlatMedis"))
							: Restrictions.and(
									Restrictions.eq("biayaAlatMedisPerKelas.tarifKhususPunyaAlatMedis",
											tarifKhususPunyaAlatMedis),
									Restrictions.isNull("biayaAlatMedisPerKelas.alatMedis")))

					.list();
		}

		if (tempJenisBiayas.isEmpty()) {
			tempJenisBiayas.addAll(session.createCriteria(JenisBiaya.class).add(Restrictions.eq("defaultAktif", true))
					.add(Restrictions.eq("tipe", JenisBiaya.TIPE_ALAT_MEDIS)).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.addOrder(Order.asc("nama")).list());
		}

		return tempJenisBiayas;
	}

	@SuppressWarnings("unchecked")
	public static List<Biaya> getBiayaPerJenis(AlatMedis alatMedis, TarifKhususPunyaAlatMedis tarifKhususPunyaAlatMedis,
			JenisBiaya jenisBiaya) {
		Session session = HibernateUtil.currentSession();
		List<Biaya> biayas = session.createCriteria(Biaya.class).add(Restrictions.isNull("detailTransaksiLayanan"))
				.add(Restrictions.isNull("detailTransaksi"))
				.createAlias("biayaAlatMedisPerKelas", "biayaAlatMedisPerKelas")
				.add(Restrictions.eq("jenisBiaya", jenisBiaya))

				.add(tarifKhususPunyaAlatMedis == null
						? Restrictions.and(Restrictions.eq("biayaAlatMedisPerKelas.alatMedis", alatMedis),
								Restrictions.isNull("biayaAlatMedisPerKelas.tarifKhususPunyaAlatMedis"))
						: Restrictions.and(
								Restrictions.eq("biayaAlatMedisPerKelas.tarifKhususPunyaAlatMedis",
										tarifKhususPunyaAlatMedis),
								Restrictions.isNull("biayaAlatMedisPerKelas.alatMedis")))

				.list();

		return biayas;
	}
}
