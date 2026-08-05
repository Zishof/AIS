package ais.action.master.sirs.util;

import java.util.Date;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.sirs.Asuransi;
import ais.database.model.sirs.Dokter;
import ais.database.model.sirs.Komunitas;
import ais.database.model.sirs.Pasien;

public class CommonTarif {

	@SuppressWarnings("rawtypes")
	public static Object getTarif(Class clazz, Criterion mainCriterion, Dokter dokter, Asuransi asuransi,
			Set<Komunitas> komunitas, Pasien pasien) {

		Session session = HibernateUtil.currentSession();

		Object o = session.createCriteria(clazz).add(mainCriterion).createCriteria("tarifKhusus")
				.add(Restrictions.le("mulai", new Date()))
				.add(Restrictions.or(Restrictions.isNull("sampai"), Restrictions.ge("sampai", new Date())))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.desc("mulai")).setMaxResults(1)
				// -- //
				.add(dokter == null ? Restrictions.isNull("dokter") : Restrictions.eq("dokter", dokter))
				.add(asuransi == null ? Restrictions.isNull("asuransi") : Restrictions.eq("asuransi", asuransi))
				.add(komunitas == null || komunitas.isEmpty() ? Restrictions.isNull("komunitas")
						: Restrictions.in("komunitas", komunitas))
				.add(pasien == null ? Restrictions.isNull("pasien") : Restrictions.eq("pasien", pasien)).uniqueResult();

		// -------------- step 1 ----------------//
		System.out.println("-------------- step 1 " + o + " ----------------");

		if (o == null) {
			o = session.createCriteria(clazz).add(mainCriterion).createCriteria("tarifKhusus")
					.add(Restrictions.le("mulai", new Date()))
					.add(Restrictions.or(Restrictions.isNull("sampai"), Restrictions.ge("sampai", new Date())))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.desc("mulai")).setMaxResults(1)
					// -- //
					.add(dokter == null ? Restrictions.isNull("dokter") : Restrictions.eq("dokter", dokter))
					.add(asuransi == null ? Restrictions.isNull("asuransi") : Restrictions.eq("asuransi", asuransi))
					.add(komunitas == null || komunitas.isEmpty() ? Restrictions.isNull("komunitas")
							: Restrictions.in("komunitas", komunitas))
					.add(Restrictions.isNull("pasien")).uniqueResult();
		}

		if (o == null) {
			o = session.createCriteria(clazz).add(mainCriterion).createCriteria("tarifKhusus")
					.add(Restrictions.le("mulai", new Date()))
					.add(Restrictions.or(Restrictions.isNull("sampai"), Restrictions.ge("sampai", new Date())))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.desc("mulai")).setMaxResults(1)
					// -- //
					.add(dokter == null ? Restrictions.isNull("dokter") : Restrictions.eq("dokter", dokter))
					.add(asuransi == null ? Restrictions.isNull("asuransi") : Restrictions.eq("asuransi", asuransi))
					.add(Restrictions.isNull("komunitas"))
					.add(pasien == null ? Restrictions.isNull("pasien") : Restrictions.eq("pasien", pasien))
					.uniqueResult();
		}

		if (o == null) {
			o = session.createCriteria(clazz).add(mainCriterion).createCriteria("tarifKhusus")
					.add(Restrictions.le("mulai", new Date()))
					.add(Restrictions.or(Restrictions.isNull("sampai"), Restrictions.ge("sampai", new Date())))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.desc("mulai")).setMaxResults(1)
					// -- //
					.add(dokter == null ? Restrictions.isNull("dokter") : Restrictions.eq("dokter", dokter))
					.add(Restrictions.isNull("asuransi"))
					.add(komunitas == null || komunitas.isEmpty() ? Restrictions.isNull("komunitas")
							: Restrictions.in("komunitas", komunitas))
					.add(pasien == null ? Restrictions.isNull("pasien") : Restrictions.eq("pasien", pasien))
					.uniqueResult();
		}

		if (o == null) {
			o = session.createCriteria(clazz).add(mainCriterion).createCriteria("tarifKhusus")
					.add(Restrictions.le("mulai", new Date()))
					.add(Restrictions.or(Restrictions.isNull("sampai"), Restrictions.ge("sampai", new Date())))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.desc("mulai")).setMaxResults(1)
					// -- //
					.add(Restrictions.isNull("dokter"))
					.add(asuransi == null ? Restrictions.isNull("asuransi") : Restrictions.eq("asuransi", asuransi))
					.add(komunitas == null || komunitas.isEmpty() ? Restrictions.isNull("komunitas")
							: Restrictions.in("komunitas", komunitas))
					.add(pasien == null ? Restrictions.isNull("pasien") : Restrictions.eq("pasien", pasien))
					.uniqueResult();
		}

		// -------------- step 2 ----------------//
		System.out.println("-------------- step 2 " + o + " ----------------");

		if (o == null) {
			o = session.createCriteria(clazz).add(mainCriterion).createCriteria("tarifKhusus")
					.add(Restrictions.le("mulai", new Date()))
					.add(Restrictions.or(Restrictions.isNull("sampai"), Restrictions.ge("sampai", new Date())))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.desc("mulai")).setMaxResults(1)
					// -- //
					.add(dokter == null ? Restrictions.isNull("dokter") : Restrictions.eq("dokter", dokter))
					.add(asuransi == null ? Restrictions.isNull("asuransi") : Restrictions.eq("asuransi", asuransi))
					.add(Restrictions.isNull("komunitas")).add(Restrictions.isNull("pasien")).uniqueResult();
		}

		if (o == null) {
			o = session.createCriteria(clazz).add(mainCriterion).createCriteria("tarifKhusus")
					.add(Restrictions.le("mulai", new Date()))
					.add(Restrictions.or(Restrictions.isNull("sampai"), Restrictions.ge("sampai", new Date())))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.desc("mulai")).setMaxResults(1)
					// -- //
					.add(dokter == null ? Restrictions.isNull("dokter") : Restrictions.eq("dokter", dokter))
					.add(Restrictions.isNull("asuransi")).add(Restrictions.isNull("komunitas"))
					.add(pasien == null ? Restrictions.isNull("pasien") : Restrictions.eq("pasien", pasien))
					.uniqueResult();
		}

		if (o == null) {
			o = session.createCriteria(clazz).add(mainCriterion).createCriteria("tarifKhusus")
					.add(Restrictions.le("mulai", new Date()))
					.add(Restrictions.or(Restrictions.isNull("sampai"), Restrictions.ge("sampai", new Date())))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.desc("mulai")).setMaxResults(1)
					// -- //
					.add(Restrictions.isNull("dokter")).add(Restrictions.isNull("asuransi"))
					.add(komunitas == null || komunitas.isEmpty() ? Restrictions.isNull("komunitas")
							: Restrictions.in("komunitas", komunitas))
					.add(pasien == null ? Restrictions.isNull("pasien") : Restrictions.eq("pasien", pasien))
					.uniqueResult();
		}

		if (o == null) {
			o = session.createCriteria(clazz).add(mainCriterion).createCriteria("tarifKhusus")
					.add(Restrictions.le("mulai", new Date()))
					.add(Restrictions.or(Restrictions.isNull("sampai"), Restrictions.ge("sampai", new Date())))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.desc("mulai")).setMaxResults(1)
					// -- //
					.add(Restrictions.isNull("dokter"))
					.add(asuransi == null ? Restrictions.isNull("asuransi") : Restrictions.eq("asuransi", asuransi))
					.add(komunitas == null || komunitas.isEmpty() ? Restrictions.isNull("komunitas")
							: Restrictions.in("komunitas", komunitas))
					.add(Restrictions.isNull("pasien")).uniqueResult();
		}

		// -------------- step 3 ----------------//
		System.out.println("-------------- step 3 " + o + " ----------------");

		if (o == null) {
			o = session.createCriteria(clazz).add(mainCriterion).createCriteria("tarifKhusus")
					.add(Restrictions.le("mulai", new Date()))
					.add(Restrictions.or(Restrictions.isNull("sampai"), Restrictions.ge("sampai", new Date())))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.desc("mulai")).setMaxResults(1)
					// -- //
					.add(dokter == null ? Restrictions.isNull("dokter") : Restrictions.eq("dokter", dokter))
					.add(Restrictions.isNull("asuransi")).add(Restrictions.isNull("komunitas"))
					.add(Restrictions.isNull("pasien")).uniqueResult();
		}

		if (o == null) {
			o = session.createCriteria(clazz).add(mainCriterion).createCriteria("tarifKhusus")
					.add(Restrictions.le("mulai", new Date()))
					.add(Restrictions.or(Restrictions.isNull("sampai"), Restrictions.ge("sampai", new Date())))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.desc("mulai")).setMaxResults(1)
					// -- //
					.add(Restrictions.isNull("dokter")).add(Restrictions.isNull("asuransi"))
					.add(Restrictions.isNull("komunitas"))
					.add(pasien == null ? Restrictions.isNull("pasien") : Restrictions.eq("pasien", pasien))
					.uniqueResult();
		}

		if (o == null) {
			o = session.createCriteria(clazz).add(mainCriterion).createCriteria("tarifKhusus")
					.add(Restrictions.le("mulai", new Date()))
					.add(Restrictions.or(Restrictions.isNull("sampai"), Restrictions.ge("sampai", new Date())))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.desc("mulai")).setMaxResults(1)
					// -- //
					.add(Restrictions.isNull("dokter")).add(Restrictions.isNull("asuransi"))
					.add(komunitas == null || komunitas.isEmpty() ? Restrictions.isNull("komunitas")
							: Restrictions.in("komunitas", komunitas))
					.add(Restrictions.isNull("pasien")).uniqueResult();
		}

		if (o == null) {
			o = session.createCriteria(clazz).add(mainCriterion).createCriteria("tarifKhusus")
					.add(Restrictions.le("mulai", new Date()))
					.add(Restrictions.or(Restrictions.isNull("sampai"), Restrictions.ge("sampai", new Date())))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.desc("mulai")).setMaxResults(1)
					// -- //
					.add(Restrictions.isNull("dokter"))
					.add(asuransi == null ? Restrictions.isNull("asuransi") : Restrictions.eq("asuransi", asuransi))
					.add(Restrictions.isNull("komunitas")).add(Restrictions.isNull("pasien")).uniqueResult();
		}

		// -------------- step 4 ----------------//
		System.out.println("-------------- step 4 " + o + " ----------------");
		if (o == null) {
			o = session.createCriteria(clazz).add(mainCriterion).createCriteria("tarifKhusus")
					.add(Restrictions.le("mulai", new Date()))
					.add(Restrictions.or(Restrictions.isNull("sampai"), Restrictions.ge("sampai", new Date())))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.desc("mulai")).setMaxResults(1)
					// -- //
					.add(Restrictions.isNull("dokter")).add(Restrictions.isNull("asuransi"))
					.add(Restrictions.isNull("komunitas")).add(Restrictions.isNull("pasien")).uniqueResult();
		}

		// -------------- step 5 ----------------//
		System.out.println("-------------- step 5 " + o + " ----------------");

		return o;
	}

}
