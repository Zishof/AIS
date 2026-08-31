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

/**
 * Utilitas resolusi tarif khusus (modul SIRS/rumah sakit): mencari baris "tarif khusus" (relasi
 * {@code tarifKhusus} pada entitas {@code clazz}, mis. tarif tindakan/kamar/obat) yang berlaku
 * pada tanggal berjalan (rentang {@code mulai}-{@code sampai}, aktif) dan paling spesifik cocok
 * dengan kombinasi {@link Dokter}, {@link Asuransi}, {@link Komunitas}, dan {@link Pasien} yang
 * diberikan.
 */
public class CommonTarif {

	/**
	 * Mencari tarif khusus yang berlaku dengan strategi generalisasi bertingkat: dimulai dari
	 * kecocokan paling spesifik (seluruh dimensi dokter/asuransi/komunitas/pasien dicocokkan persis
	 * dengan parameter yang diberikan, atau {@code null} bila parameter kosong), lalu bila tidak
	 * ditemukan, dilonggarkan langkah demi langkah — melepas syarat pasien, lalu komunitas, lalu
	 * asuransi, lalu dokter (dan kombinasinya) — hingga akhirnya jatuh ke tarif umum yang tidak
	 * terikat dimensi apa pun sama sekali. Di setiap langkah, hanya baris teraktif dan terbaru
	 * (diurutkan {@code mulai} menurun) yang diambil.
	 *
	 * @param clazz          kelas entitas tarif yang memiliki relasi {@code tarifKhusus}
	 * @param mainCriterion  kriteria tambahan pada entitas utama (mis. jenis tindakan/item spesifik)
	 * @param dokter         dokter terkait, boleh {@code null}
	 * @param asuransi       asuransi pasien, boleh {@code null}
	 * @param komunitas      kumpulan komunitas pasien, boleh {@code null}/kosong
	 * @param pasien         pasien spesifik (untuk tarif personal), boleh {@code null}
	 * @return baris tarif khusus paling spesifik yang cocok dan berlaku, atau {@code null} bila tidak ada sama sekali
	 */
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
