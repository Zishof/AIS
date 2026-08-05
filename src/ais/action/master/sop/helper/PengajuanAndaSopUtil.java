package ais.action.master.sop.helper;

import java.util.Date;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.database.model.Tbmuser;
import ais.database.model.sop.AktorSop;
import ais.database.model.sop.DisposisiAlurSop;
import ais.database.model.sop.DisposisiSop;

/**
 * Utilitas bersama untuk dasbor <b>"Pengajuan Anda" (Workflow / Proses SOP)</b> versi <b>JSP</b>.
 *
 * <h2>Untuk apa kelas ini?</h2>
 * <p>
 * Kelas ini menyediakan kriteria/penghitung yang dipakai oleh halaman JSP
 * {@code modul/pagesmastersoppengajuananda/index.jsp} (+ {@code pengajuan_anda_service.jsp}) agar
 * pengguna kantin dapat memantau alur SOP miliknya — mulai dari pengajuan PR, PO, BAST, dan
 * seterusnya — persis seperti dasbor versi ZK ({@code DasboardSop} / {@code TampilanAlurSopAction}).
 * </p>
 *
 * <h2>Kenapa logika ditaruh di sini (reuse)?</h2>
 * <p>
 * Inti perhitungan dasbor SOP di versi ZK ({@code DasboardSop}) memakai method instance yang sulit
 * dipanggil dari JSP. Namun bagian tersulitnya — pencocokan "aktor" (siapa yang berhak/menunggu
 * disposisi) — sebenarnya berada di method <b>statik</b> {@link AktorSop#buatCriterion(Tbmuser,
 * boolean, Criteria)} dan {@link AktorSop#bolehMelihatSemuaSop(Tbmuser)}. Kelas ini me-<i>reuse</i>
 * method statik itu lalu menyusun ulang kriteria yang <b>identik</b> dengan panel ZK, sehingga angka
 * di JSP konsisten dengan ZK tanpa menduplikasi logika aktor. Bila aturan aktor berubah, cukup ubah
 * di {@code AktorSop} dan kedua tampilan ikut.
 * </p>
 *
 * <h2>Enam metrik dasbor (sama dengan ZK)</h2>
 * <ol>
 * <li><b>Menunggu Saya</b> — langkah yang menunggu disposisi Anda
 *     ({@link #criteriaMenungguSaya(Session, Tbmuser, boolean)}).</li>
 * <li><b>Sudah Disposisi</b> — langkah yang sudah pernah Anda proses
 *     ({@link #criteriaSudahDisposisi(Session, Tbmuser)}).</li>
 * <li><b>Jumlah Pengajuan Anda</b> — dokumen SOP yang Anda ajukan
 *     ({@link #criteriaPengajuanAnda(Session, Tbmuser)}).</li>
 * <li><b>Selesai</b> — pengajuan yang sudah tuntas ({@link #criteriaSelesai(Session, Tbmuser)}).</li>
 * <li><b>Menunggu Petugas</b> — masih menunggu aktor lain
 *     ({@link #criteriaMenungguAktor(Session, Tbmuser)}).</li>
 * <li><b>Lewat Batas Waktu</b> — subset "Menunggu Saya" yang sudah melewati tenggat (parameter
 *     {@code hanyaLewatDeadline=true}).</li>
 * </ol>
 *
 * <h2>Aturan sesi</h2>
 * <p>
 * Semua method menerima {@link Session} dari pemanggil (JSP memberi {@code currentSession()} yang
 * <b>tidak</b> ditutup manual karena dikelola kerangka kerja). Kelas ini tidak pernah membuka
 * {@code openSession()}/{@code currentNativeSession()} sendiri sehingga tidak ada sesi menggantung.
 * </p>
 *
 * <h2>Kompatibilitas</h2>
 * <p>Java 1.7 (tanpa lambda/stream), {@code try/catch} gaya 1.6 (tanpa multi-catch).</p>
 *
 * @author AIS
 */
public final class PengajuanAndaSopUtil {

	private PengajuanAndaSopUtil() {
	}

	/**
	 * Pembatas "milik saya" — port dari {@code DasboardSop.getUserRestriction}. Bila pengguna boleh
	 * melihat semua SOP → tanpa batas; mahasiswa/siswa dibatasi ke dirinya; selain itu dibatasi ke
	 * {@code diajukanOleh = pengguna}.
	 *
	 * @param user   pengguna aktif.
	 * @param prefix awalan alias (mis. {@code "disposisiSop"}); kosong untuk entitas utama.
	 * @return {@link Criterion} pembatas; tidak pernah {@code null}.
	 */
	public static Criterion userRestriction(Tbmuser user, String prefix) {
		if (AktorSop.bolehMelihatSemuaSop(user)) {
			return Restrictions.sqlRestriction("true");
		}
		String p = (prefix == null || prefix.length() == 0) ? "" : prefix + ".";
		if (user != null && (user.getMahasiswa() != null || user.getSiswa() != null)) {
			return Restrictions.or(Restrictions.eq(p + "mahasiswa", user.getMahasiswa()),
					Restrictions.eq(p + "siswa", user.getSiswa()));
		}
		return user == null ? Restrictions.sqlRestriction("false") : Restrictions.eq(p + "diajukanOleh", user);
	}

	/** Kriteria "Jumlah Pengajuan Anda" — DisposisiSop yang Anda ajukan. */
	public static Criteria criteriaPengajuanAnda(Session session, Tbmuser user) {
		return session.createCriteria(DisposisiSop.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(userRestriction(user, ""));
	}

	/** Kriteria "Sudah Disposisi" — DisposisiAlurSop (langkah non-start) yang sudah Anda proses. */
	public static Criteria criteriaSudahDisposisi(Session session, Tbmuser user) {
		return session.createCriteria(DisposisiAlurSop.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.isNotNull("alurSop")).createAlias("alurSop", "alurSop")
				.add(Restrictions.isNotNull("alurSop.sop")).add(Restrictions.eq("alurSop.start", false))
				.add(Restrictions.isNotNull("waktu")).createAlias("disposisiSop", "disposisiSop")
				.add(userRestriction(user, "")).add(Restrictions.or(Restrictions.isNull("disposisiSop.aktif"),
						Restrictions.eq("disposisiSop.aktif", true)));
	}

	/**
	 * Kriteria "Menunggu Saya" — langkah yang menunggu disposisi Anda (memakai pencocokan aktor
	 * {@link AktorSop#buatCriterion(Tbmuser, boolean, Criteria)}).
	 *
	 * @param hanyaLewatDeadline bila {@code true} hanya yang sudah melewati {@code waktuMaksimal}
	 *                           (untuk metrik "Lewat Batas Waktu").
	 */
	public static Criteria criteriaMenungguSaya(Session session, Tbmuser user, boolean hanyaLewatDeadline) {
		Criteria criteria = session.createCriteria(DisposisiAlurSop.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.or(Restrictions.isNull("selesai"), Restrictions.eq("selesai", false)))
				.add(Restrictions.isNotNull("alurSop")).createAlias("alurSop", "alurSop")
				.add(Restrictions.isNotNull("alurSop.sop"))
				.createAlias("alurSop.aktorSop", "aktorSop", Criteria.LEFT_JOIN)
				.createAlias("disposisiSop", "disposisiSop")
				.createAlias("sebelumnya", "sebelumnya", Criteria.LEFT_JOIN);

		criteria.add(AktorSop.buatCriterion(user, true, criteria))
				.add(Restrictions.and(Restrictions.isNull("diajukanOleh"),
						Restrictions.and(Restrictions.isNull("siswa"), Restrictions.isNull("mahasiswa"))))
				.add(Restrictions.isNull("setelahnya")).add(Restrictions.isNotNull("sebelumnya"))
				.add(Restrictions.or(Restrictions.isNull("disposisiSop.aktif"),
						Restrictions.eq("disposisiSop.aktif", true)));

		if (hanyaLewatDeadline) {
			criteria.add(Restrictions.isNotNull("waktuMaksimal"));
			criteria.add(Restrictions.lt("waktuMaksimal", new Date()));
		}
		return criteria;
	}

	/** Kriteria "Selesai" — DisposisiAlurSop langkah akhir/selesai untuk pengajuan milik Anda. */
	public static Criteria criteriaSelesai(Session session, Tbmuser user) {
		Criterion c1 = Restrictions.isNull("alurSop.setelahnya");
		c1 = Restrictions.and(c1, Restrictions.isNull("alurSop.setelahnya2"));
		c1 = Restrictions.and(c1, Restrictions.isNull("alurSop.setelahnya3"));
		c1 = Restrictions.and(c1, Restrictions.isNull("alurSop.setelahnya4"));
		c1 = Restrictions.and(c1, Restrictions.isNull("alurSop.setelahnya5"));
		c1 = Restrictions.or(c1, Restrictions.eq("selesai", true));

		return session.createCriteria(DisposisiAlurSop.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.isNotNull("alurSop")).createAlias("alurSop", "alurSop")
				.add(Restrictions.isNotNull("alurSop.sop")).add(Restrictions.eq("alurSop.start", false))
				.add(Restrictions.or(Restrictions.eq("selesai", true), Restrictions.isNull("setelahnya"))).add(c1)
				.add(Restrictions.isNotNull("waktu")).createAlias("disposisiSop", "disposisiSop")
				.add(userRestriction(user, "disposisiSop")).add(Restrictions.or(
						Restrictions.isNull("disposisiSop.aktif"), Restrictions.eq("disposisiSop.aktif", true)));
	}

	/** Kriteria "Menunggu Petugas" — langkah pengajuan Anda yang masih menunggu aktor lain. */
	public static Criteria criteriaMenungguAktor(Session session, Tbmuser user) {
		return session.createCriteria(DisposisiAlurSop.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.isNotNull("alurSop")).createAlias("alurSop", "alurSop")
				.add(Restrictions.isNotNull("alurSop.sop")).add(Restrictions.isNull("diajukanOleh"))
				.add(Restrictions.isNull("mahasiswa")).add(Restrictions.isNull("siswa"))
				.createAlias("disposisiSop", "disposisiSop").add(userRestriction(user, "disposisiSop"))
				.add(Restrictions.or(Restrictions.isNull("disposisiSop.aktif"),
						Restrictions.eq("disposisiSop.aktif", true)));
	}

	/** Hitung jumlah baris suatu kriteria (memasang projeksi rowCount). */
	public static int hitung(Criteria criteria) {
		if (criteria == null) {
			return 0;
		}
		Object r = criteria.setProjection(Projections.rowCount()).uniqueResult();
		return r == null ? 0 : ((Number) r).intValue();
	}

	/** Ringkasan 6 metrik dasbor dalam sekali hitung. Tidak pernah {@code null}. */
	public static Ringkasan hitungRingkasan(Session session, Tbmuser user) {
		Ringkasan r = new Ringkasan();
		try { r.menungguSaya = hitung(criteriaMenungguSaya(session, user, false)); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/helper/PengajuanAndaSopUtil.java:183"); }
		try { r.sudahDisposisi = hitung(criteriaSudahDisposisi(session, user)); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/helper/PengajuanAndaSopUtil.java:184"); }
		try { r.pengajuanAnda = hitung(criteriaPengajuanAnda(session, user)); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/helper/PengajuanAndaSopUtil.java:185"); }
		try { r.selesai = hitung(criteriaSelesai(session, user)); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/helper/PengajuanAndaSopUtil.java:186"); }
		try { r.menungguPetugas = hitung(criteriaMenungguAktor(session, user)); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/helper/PengajuanAndaSopUtil.java:187"); }
		try { r.lewatBatas = hitung(criteriaMenungguSaya(session, user, true)); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/helper/PengajuanAndaSopUtil.java:188"); }
		r.prosesDipantau = r.pengajuanAnda;
		r.totalAntrian = r.menungguSaya + r.menungguPetugas;
		return r;
	}

	/** Wadah ringkasan 6 metrik + 2 angka banner (Proses Dipantau & Total Antrian). */
	public static final class Ringkasan {
		public int menungguSaya;
		public int sudahDisposisi;
		public int pengajuanAnda;
		public int selesai;
		public int menungguPetugas;
		public int lewatBatas;
		public int prosesDipantau;
		public int totalAntrian;
	}
}
