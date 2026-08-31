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

/**
 * Utilitas resolusi tarif tindakan medis SIRS per kelas perawatan, dengan dukungan tarif khusus
 * (override berdasarkan dokter/asuransi/komunitas/pasien tertentu — lihat
 * {@link ais.action.master.sirs.util.CommonTarif}). Fungsi inti,
 * {@link #getBiayaTindakanPerKelas}, menerapkan strategi "get-or-create": bila baris
 * {@link BiayaTindakanPerKelas} untuk kombinasi tindakan/kelas/tarif khusus belum ada, baris baru
 * dibuat otomatis (biaya awal 0, atau disalin dari tarif standar bila membuat entri tarif khusus)
 * dan disimpan — method ini memiliki efek samping penulisan database meski namanya berawalan
 * "get". Juga menyediakan {@link #getJenisBiayas}/{@link #getBiayaPerJenis} untuk mengambil
 * rincian komponen biaya (jenis biaya) suatu tindakan/paket, dengan fallback ke jenis biaya default
 * bila tindakan belum memiliki komponen biaya spesifik.
 */
public class CommonTarifTindakan {

	/** Mengambil/membuat {@link BiayaTindakanPerKelas} untuk tindakan dan kelas perawatan tanpa tarif khusus. */
	public static BiayaTindakanPerKelas getBiayaTindakanPerKelas(Tindakan tindakan, KelasPerawatan kelasPerawatan) {
		TarifKhususPunyaTindakan tarifKhususPunyaTindakan = null;
		return getBiayaTindakanPerKelas(tindakan, kelasPerawatan, tarifKhususPunyaTindakan);
	}

	/** Menentukan tarif khusus yang berlaku (bila ada) untuk kombinasi dokter/asuransi/komunitas/pasien, lalu mengambil/membuat {@link BiayaTindakanPerKelas} sesuai kelas perawatan. */
	public static BiayaTindakanPerKelas getBiayaTindakanPerKelas(Tindakan tindakan, KelasPerawatan kelasPerawatan,
			Dokter dokter, Asuransi asuransi, Set<Komunitas> komunitas, Pasien pasien) {
		TarifKhususPunyaTindakan tarifKhususPunyaTindakan = getTarifKhususPunyaTindakan(tindakan, dokter, asuransi,
				komunitas, pasien);
		return getBiayaTindakanPerKelas(tindakan, kelasPerawatan, tarifKhususPunyaTindakan);
	}

	/** Mengambil/membuat {@link BiayaTindakanPerKelas} dari {@code tarifKhususPunyaTindakan} yang sudah diketahui (tindakan diturunkan darinya). */
	public static BiayaTindakanPerKelas getBiayaTindakanPerKelas(TarifKhususPunyaTindakan tarifKhususPunyaTindakan,
			KelasPerawatan kelasPerawatan) {
		return getBiayaTindakanPerKelas(tarifKhususPunyaTindakan.getTindakan(), kelasPerawatan,
				tarifKhususPunyaTindakan);
	}

	/** Mencari {@link TarifKhususPunyaTindakan} yang berlaku untuk tindakan tertentu berdasarkan kriteria dokter/asuransi/komunitas/pasien, lewat {@link CommonTarif#getTarif}. */
	public static TarifKhususPunyaTindakan getTarifKhususPunyaTindakan(Tindakan tindakan, Dokter dokter,
			Asuransi asuransi, Set<Komunitas> komunitas, Pasien pasien) {
		return (TarifKhususPunyaTindakan) CommonTarif.getTarif(TarifKhususPunyaTindakan.class,
				Restrictions.eq("tindakan", tindakan), dokter, asuransi, komunitas, pasien);
	}

	/**
	 * Mengambil baris {@link BiayaTindakanPerKelas} untuk kombinasi tindakan, kelas perawatan, dan
	 * tarif khusus (opsional). Bila belum ada, membuat baris baru: bila {@code tarifKhususPunyaTindakan}
	 * diberikan, biaya awal disalin dari tarif standar (tindakan tanpa tarif khusus) pada kelas yang
	 * sama; bila tidak, biaya awal 0. Baris baru langsung disimpan ke sesi Hibernate saat ini
	 * (tanpa transaksi eksplisit — mengasumsikan transaksi sudah berjalan di pemanggil).
	 *
	 * @param tindakan                 tindakan medis yang tarifnya dicari
	 * @param kelasPerawatan           kelas perawatan (mis. VIP, Kelas I/II/III)
	 * @param tarifKhususPunyaTindakan tarif khusus yang berlaku, atau {@code null} untuk tarif standar
	 * @return baris biaya (tersimpan atau baru dibuat), tidak pernah {@code null}
	 */
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

	/** Sama seperti {@link #getBiayaTindakanPerKelas(Tindakan, KelasPerawatan, TarifKhususPunyaTindakan)}, tetapi memakai {@code session} yang diberikan dan membungkus penyimpanan baris baru dalam transaksi eksplisit (begin/commit) sendiri. */
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

	/**
	 * Mengambil daftar jenis biaya (komponen tarif, mis. jasa dokter/BHP/administrasi) yang sudah
	 * dikonfigurasi untuk tindakan tertentu (dicocokkan dengan tarif khusus bila diberikan). Bila
	 * tindakan belum memiliki komponen biaya spesifik apa pun, fallback ke seluruh
	 * {@link JenisBiaya} bertipe sesuai ({@code jenisPaket}) yang ditandai {@code defaultAktif}.
	 *
	 * @param tindakan                 tindakan yang komponen biayanya dicari, boleh {@code null}/tanpa id (langsung fallback)
	 * @param jenisPaket               {@link Tindakan#JENIS_PERAWATAN_PAKET} atau jenis lain (menentukan tipe {@link JenisBiaya})
	 * @param tarifKhususPunyaTindakan tarif khusus yang berlaku, atau {@code null} untuk tarif standar
	 * @return daftar jenis biaya yang berlaku (spesifik tindakan, atau default bila tidak ada)
	 */
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

	/** Mengambil daftar baris {@link Biaya} template (belum terikat ke transaksi/detail layanan mana pun) untuk kombinasi tindakan/tarif khusus dan satu {@code jenisBiaya} tertentu. */
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
