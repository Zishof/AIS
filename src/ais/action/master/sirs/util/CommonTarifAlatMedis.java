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

/**
 * Utilitas resolusi tarif alat medis pada modul SIRS: menerapkan hierarki tarif <b>khusus lebih
 * diutamakan daripada tarif umum</b>. Alat medis dapat memiliki tarif khusus yang berlaku untuk
 * kombinasi dokter/asuransi/komunitas/pasien tertentu ({@link TarifKhususPunyaAlatMedis}, dicari
 * lewat {@link CommonTarif#getTarif}); bila ada, biaya per kelas perawatan diambil dari baris
 * {@link BiayaAlatMedisPerKelas} yang tertaut tarif khusus tersebut (dibuat otomatis dengan nilai
 * awal disalin dari tarif umum bila belum ada baris untuk kombinasi itu); bila tidak ada tarif
 * khusus yang cocok, biaya diambil langsung dari baris yang tertaut alat medis umum. Kelas ini juga
 * menyediakan pencarian daftar jenis biaya dan rincian biaya terkait alat medis untuk keperluan
 * tampilan tagihan.
 */
public class CommonTarifAlatMedis {

	/** Seperti {@link #getBiayaAlatMedisPerKelas(AlatMedis, KelasPerawatan, TarifKhususPunyaAlatMedis)} tanpa tarif khusus (selalu memakai tarif umum alat medis). */
	public static BiayaAlatMedisPerKelas getBiayaAlatMedisPerKelas(AlatMedis alatMedis, KelasPerawatan kelasPerawatan) {
		TarifKhususPunyaAlatMedis tarifKhususPunyaAlatMedis = null;
		return getBiayaAlatMedisPerKelas(alatMedis, kelasPerawatan, tarifKhususPunyaAlatMedis);
	}

	/**
	 * Mengambil biaya alat medis pada kelas perawatan tertentu, otomatis mengutamakan tarif khusus
	 * ({@link #getTarifKhususPunyaAlatMedis}) yang cocok kombinasi dokter/asuransi/komunitas/pasien
	 * bila ada, atau jatuh ke tarif umum bila tidak ada tarif khusus yang berlaku.
	 */
	public static BiayaAlatMedisPerKelas getBiayaAlatMedisPerKelas(AlatMedis alatMedis, KelasPerawatan kelasPerawatan,
			Dokter dokter, Asuransi asuransi, Set<Komunitas> komunitas, Pasien pasien) {
		TarifKhususPunyaAlatMedis tarifKhususPunyaAlatMedis = getTarifKhususPunyaAlatMedis(alatMedis, dokter, asuransi,
				komunitas, pasien);
		return getBiayaAlatMedisPerKelas(alatMedis, kelasPerawatan, tarifKhususPunyaAlatMedis);
	}

	/** Seperti {@link #getBiayaAlatMedisPerKelas(AlatMedis, KelasPerawatan, TarifKhususPunyaAlatMedis)}, dengan alat medis diambil dari {@code tarifKhususPunyaAlatMedis.getAlatMedis()}. */
	public static BiayaAlatMedisPerKelas getBiayaAlatMedisPerKelas(TarifKhususPunyaAlatMedis tarifKhususPunyaAlatMedis,
			KelasPerawatan kelasPerawatan) {
		return getBiayaAlatMedisPerKelas(tarifKhususPunyaAlatMedis.getAlatMedis(), kelasPerawatan,
				tarifKhususPunyaAlatMedis);
	}

	/** Mencari tarif khusus alat medis yang cocok kombinasi dokter/asuransi/komunitas/pasien lewat mesin resolusi tarif umum {@link CommonTarif#getTarif}; mengembalikan {@code null} bila tidak ada yang berlaku. */
	public static TarifKhususPunyaAlatMedis getTarifKhususPunyaAlatMedis(AlatMedis alatMedis, Dokter dokter,
			Asuransi asuransi, Set<Komunitas> komunitas, Pasien pasien) {

		return (TarifKhususPunyaAlatMedis) CommonTarif.getTarif(TarifKhususPunyaAlatMedis.class,
				Restrictions.eq("alatMedis", alatMedis), dokter, asuransi, komunitas, pasien);

	}

	/**
	 * Implementasi inti resolusi biaya alat medis per kelas perawatan (memakai
	 * {@link HibernateUtil#currentSession()}, transaksi implisit). Bila {@code tarifKhususPunyaAlatMedis}
	 * diberikan, mencari/membuat baris {@link BiayaAlatMedisPerKelas} yang tertaut tarif khusus
	 * tersebut (nilai awal disalin dari biaya tarif umum alat medis yang sama bila baris belum
	 * ada); bila {@code null}, mencari/membuat baris yang tertaut langsung ke {@code alatMedis}
	 * (tarif umum, nilai awal 0). Baris baru langsung disimpan ke database.
	 *
	 * @param alatMedis                    alat medis yang dicari biayanya
	 * @param kelasPerawatan                kelas perawatan pasien
	 * @param tarifKhususPunyaAlatMedis     tarif khusus yang berlaku, atau {@code null} untuk tarif umum
	 * @return baris {@link BiayaAlatMedisPerKelas} yang sudah ada atau baru dibuat, tidak pernah {@code null}
	 */
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

	/** Seperti {@link #getBiayaAlatMedisPerKelas(AlatMedis, KelasPerawatan, TarifKhususPunyaAlatMedis)}, tetapi memakai {@code session} yang diberikan pemanggil dan transaksi eksplisit (begin/commit) saat menyimpan baris baru — dipakai saat pemanggil sudah mengelola sesi/transaksinya sendiri. */
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

	/**
	 * Mengambil daftar jenis biaya yang berlaku untuk alat medis (dan tarif khusus bila diberikan)
	 * berdasarkan rincian biaya ({@link Biaya}) yang belum tertaut ke transaksi layanan/detail
	 * transaksi manapun (baris "template" biaya alat medis). Bila tidak ditemukan baris biaya
	 * spesifik untuk alat medis tersebut, jatuh ke daftar jenis biaya default aktif bertipe
	 * {@link JenisBiaya#TIPE_ALAT_MEDIS} (diurutkan berdasarkan nama) sebagai fallback.
	 *
	 * @param alatMedis                   alat medis yang dicari jenis biayanya, boleh {@code null}
	 *                                    (menghasilkan daftar kosong sebelum fallback)
	 * @param tarifKhususPunyaAlatMedis   tarif khusus yang berlaku, atau {@code null} untuk tarif umum
	 * @return daftar {@link JenisBiaya} yang relevan, tidak pernah {@code null}
	 */
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

	/**
	 * Mengambil daftar rincian biaya ({@link Biaya}, baris "template" yang belum tertaut transaksi
	 * apa pun) untuk satu jenis biaya tertentu pada alat medis (dan tarif khusus bila diberikan).
	 *
	 * @param alatMedis                   alat medis yang dicari rincian biayanya
	 * @param tarifKhususPunyaAlatMedis   tarif khusus yang berlaku, atau {@code null} untuk tarif umum
	 * @param jenisBiaya                  jenis biaya yang dicari
	 * @return daftar {@link Biaya} yang cocok, dapat kosong
	 */
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
