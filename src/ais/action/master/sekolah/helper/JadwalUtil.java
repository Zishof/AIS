package ais.action.master.sekolah.helper;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.database.model.sekolah.KelasSiswa;

/**
 * Kelas utilitas kecil untuk modul jadwal pelajaran sekolah: menemukan mata pelajaran yang
 * diajar seorang guru pada satu kelas siswa tertentu.
 */
public class JadwalUtil {

	/**
	 * Mengambil daftar id mata pelajaran yang diajar {@code guru} pada {@code kelasSiswa}.
	 * Pencarian mencocokkan {@code guru} terhadap salah satu dari 12 kolom pengampu
	 * ({@code guru} s/d {@code guru12}) pada {@link JadwalPelajaran}, karena satu baris jadwal
	 * dapat diampu lebih dari satu guru sekaligus (mis. team teaching).
	 *
	 * @param guru       guru yang dicari jadwalnya
	 * @param kelasSiswa kelas yang menjadi konteks pencarian
	 * @return daftar id (unik per kelompok) mata pelajaran yang diajar {@code guru} di kelas tersebut
	 */
	@SuppressWarnings("unchecked")
	public static List<Long> ambilJadwal(Guru guru, KelasSiswa kelasSiswa) {
		Session session = HibernateUtil.currentNativeSession();

		Criterion criterion = Restrictions.or(Restrictions.eq("guru", guru), Restrictions.eq("guru2", guru));

		criterion = Restrictions.or(criterion, Restrictions.eq("guru3", guru));
		criterion = Restrictions.or(criterion, Restrictions.eq("guru4", guru));
		criterion = Restrictions.or(criterion, Restrictions.eq("guru5", guru));
		criterion = Restrictions.or(criterion, Restrictions.eq("guru6", guru));
		criterion = Restrictions.or(criterion, Restrictions.eq("guru7", guru));
		criterion = Restrictions.or(criterion, Restrictions.eq("guru8", guru));
		criterion = Restrictions.or(criterion, Restrictions.eq("guru9", guru));
		criterion = Restrictions.or(criterion, Restrictions.eq("guru10", guru));
		criterion = Restrictions.or(criterion, Restrictions.eq("guru11", guru));
		criterion = Restrictions.or(criterion, Restrictions.eq("guru12", guru));

		List<Long> mt = session.createCriteria(JadwalPelajaran.class).add(criterion)
				.add(Restrictions.eq("kelas", kelasSiswa)).setProjection(Projections.groupProperty("matapelajaran.id"))
				.list();
		// session.disconnect();
		if (session.isOpen()) {session.disconnect();session.close();}
		HibernateUtil.closeSession();
		return mt;
	}

}
