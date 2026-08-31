package ais.action.master.bkd.helper;

import java.util.List;
import java.util.TreeSet;

import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zul.Label;

import ais.action.master.bkd.PenilaianAsesorAction;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.AsesemenPenilaian;
import ais.database.model.Asesor;
import ais.database.model.AsesorPegawai;
import ais.database.model.Dosen;
import ais.database.model.Jenjang;
import ais.database.model.ParameterUmum;
import ais.database.model.Pegawai;
import ais.database.model.PenilaianAsesor;
import ais.database.model.PenunjangKinerjaDosen;
import ais.database.model.Perkuliahan;
import ais.database.model.Skripsi;

/**
 * Helper penghitung dan penyimpan komponen BKD (Beban Kerja Dosen) untuk peran dosen sebagai
 * <b>penguji tugas akhir/skripsi</b> ({@code PenilaianAsesor.PENGUJI_TA}), pada bidang "Pendidikan".
 * Untuk satu tahun akademik + semester (dan opsional satu {@link Jenjang} tertentu), kelas ini
 * mencari mahasiswa yang skripsinya telah disidang ({@code telahSidang=1}) di mana dosen menjadi
 * pembimbing atau salah satu dari tiga penguji ({@code penguji1/2/3}), menghitung SKS beban
 * mengajar yang didapat (dibatasi maksimum konfigurasi per jenjang lewat {@link ParameterUmum}
 * kunci {@code jumlah_sks_ujian_tugas_akhir_<idJenjang>}), lalu menyimpan/memperbarui satu baris
 * {@link AsesemenPenilaian} berisi rincian mahasiswa yang diuji sebagai keterangan. Setelah
 * tersimpan, penilaian tersebut diteruskan ke {@link PenilaianAsesorAction#checkPenilaian} agar
 * asesor terkait (dosen dengan relasi {@link AsesorPegawai} aktif) dapat menilainya.
 *
 * <p>
 * Tiga overload {@code populate} membentuk rantai pemrosesan bertingkat: (1) varian tanpa
 * {@code jenjang} memproses SEMUA jenjang aktif; (2) varian dengan {@code Pegawai} + {@code jenjang}
 * memproses satu dosen (bila {@code pegawai} punya relasi dosen) atau, bila {@code pegawai} null,
 * SEMUA dosen yang muncul sebagai pembimbing/penguji skripsi pada jenjang tersebut (dengan progres
 * dilaporkan lewat {@code label} bila diberikan); (3) varian inti dengan {@link Dosen} eksplisit
 * yang benar-benar melakukan perhitungan dan penyimpanan.
 * </p>
 */
public class BkdPengujiSkripsiHelper {

	/**
	 * Memproses BKD penguji skripsi untuk {@code pegawai} pada SEMUA {@link Jenjang} aktif,
	 * mendelegasikan tiap jenjang ke {@link #populate(Session, Pegawai, Jenjang, String, String, Label)}.
	 *
	 * @param session       sesi Hibernate aktif
	 * @param pegawai       pegawai target; bila relasinya ke {@link Dosen} ada, hanya dosen
	 *                      tersebut yang diproses — bila {@code null} pada overload berikutnya,
	 *                      seluruh dosen relevan diproses
	 * @param tahunAkademik tahun akademik target
	 * @param semester      semester target ({@link Perkuliahan#GANJIL} atau genap)
	 * @param label         komponen ZK opsional untuk menampilkan progres pemrosesan ke pengguna
	 */
	@SuppressWarnings("unchecked")
	public static void populate(Session session, final Pegawai pegawai, String tahunAkademik, String semester,
			Label label) {
		List<Jenjang> jenjangs = HibernateUtil.currentSession().createCriteria(Jenjang.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();
		for (Jenjang jenjang : jenjangs) {
			BkdPengujiSkripsiHelper.populate(session, pegawai, jenjang, tahunAkademik, semester, label);
		}
	}

	/**
	 * Memproses BKD penguji skripsi pada satu {@code jenjang}. Bila {@code pegawai} relasinya ke
	 * {@link Dosen} ada, mendelegasikan langsung ke overload {@link Dosen} untuk dosen tersebut
	 * saja. Bila {@code pegawai} {@code null}, mencari SEMUA dosen (pembimbing atau salah satu dari
	 * {@code penguji1/2/3}) pada skripsi jenjang/tahun/semester tersebut yang telah disidang, lalu
	 * memproses masing-masing satu per satu (progres dilaporkan lewat {@code label} bila diberikan).
	 *
	 * @param session       sesi Hibernate aktif
	 * @param pegawai       pegawai target, atau {@code null} untuk memproses semua dosen relevan
	 * @param jenjang       jenjang pendidikan yang menjadi cakupan pencarian skripsi
	 * @param tahunAkademik tahun akademik target
	 * @param semester      semester target
	 * @param label         komponen ZK opsional untuk menampilkan progres
	 */
	@SuppressWarnings("unchecked")
	public static void populate(Session session, final Pegawai pegawai, Jenjang jenjang, String tahunAkademik,
			String semester, Label label) {

		if (pegawai != null && pegawai.getDosen() != null) {
			BkdPengujiSkripsiHelper.populate(session, pegawai.getDosen(), jenjang, tahunAkademik, semester, label);
		} else {

			String[] cols = new String[] { "penguji1", "penguji2", "penguji3" };

			TreeSet<Dosen> dosens = new TreeSet<Dosen>();
			for (String col : cols) {
				dosens.addAll(session.createCriteria(Skripsi.class).add(Restrictions.eq("telahSidang", 1))
						.createAlias("mahasiswa", "mahasiswa").createAlias("mahasiswa.jurusan", "jurusan")
						.add(Restrictions.eq("jurusan.jenjang", jenjang)).setProjection(Projections.groupProperty(col))
						.add(Restrictions.isNotNull(col)).add(Restrictions.eq("tahunAkademik", tahunAkademik))
						.add(Restrictions.in("semester",
								semester.equals(Perkuliahan.GANJIL) ? Common.ganjil : Common.genap))
						.list());
			}

			System.out.println("jenjang => " + jenjang + ", banyak " + dosens.size());

			int i = 0;
			int rowCount = dosens.size();
			for (Dosen dosen : dosens) {
				if (label != null) {
					label.setValue("Memproses data penguji skripsi \"" + dosen.getNama() + "\" ("
							+ Common.numberFormat.get().format(i * 100.0 / rowCount) + " %)");
				}
				i++;
				BkdPengujiSkripsiHelper.populate(session, dosen, jenjang, tahunAkademik, semester, label);
			}
		}
	}

	/**
	 * Implementasi inti: menghitung dan menyimpan komponen BKD penguji skripsi untuk satu
	 * {@code dosen} tertentu. Tidak melakukan apa pun bila dosen tidak memiliki asesor aktif
	 * ({@link AsesorPegawai}) atau tidak muncul sebagai pembimbing/penguji pada skripsi yang telah
	 * disidang di jenjang/tahun/semester tersebut. Jumlah mahasiswa yang diuji dibatasi maksimum
	 * konfigurasi ({@code jumlah_sks_ujian_tugas_akhir_<idJenjang>}, default maksimum 8 mahasiswa)
	 * sebelum dikalikan bobot SKS per mahasiswa dari konfigurasi yang sama. Baris
	 * {@link AsesemenPenilaian} yang sudah ada untuk kombinasi
	 * dosen+spesifikasi+jenjang+tahun+semester yang sama diperbarui (bukan dibuat baru berulang).
	 * Setelah tersimpan, memicu {@link PenilaianAsesorAction#checkPenilaian} untuk seluruh asesor
	 * aktif dosen tersebut.
	 *
	 * @param session       sesi Hibernate aktif
	 * @param dosen         dosen yang dinilai sebagai penguji tugas akhir
	 * @param jenjang       jenjang pendidikan cakupan skripsi
	 * @param tahunAkademik tahun akademik target
	 * @param semester      semester target
	 * @param label         tidak dipakai langsung pada overload ini (diteruskan untuk konsistensi
	 *                      tanda tangan pemanggilan berantai)
	 */
	@SuppressWarnings("unchecked")
	public static void populate(Session session, Dosen dosen, Jenjang jenjang, String tahunAkademik, String semester,
			Label label) {

		if (dosen == null || dosen.getPegawaiId() == null) {
			return;
		}

		List<Asesor> asesors = session.createCriteria(AsesorPegawai.class).createAlias("pegawai", "pegawai")
				.createAlias("asesor", "asesor")
				.add(Restrictions.or(Restrictions.isNull("asesor.aktif"), Restrictions.eq("asesor.aktif", true)) )
				.add(Restrictions.eq("pegawai.dosen", dosen)).setProjection(Projections.groupProperty("asesor")).list();

		System.out.println("dosen => " + dosen + ", asesors => " + asesors);
		if (!asesors.isEmpty()) {

			Criterion criterion = Restrictions.eq("pembimbing", dosen);

			String[] cols = new String[] { "penguji1", "penguji2", "penguji3" };
			for (String c : cols) {
				criterion = Restrictions.or(criterion, Restrictions.eq(c, dosen));
			}

			List<Skripsi> skripsis = session.createCriteria(Skripsi.class).add(Restrictions.eq("telahSidang", 1))
					.createAlias("mahasiswa", "mahasiswa").createAlias("mahasiswa.jurusan", "jurusan")
					.add(Restrictions.eq("jurusan.jenjang", jenjang)).add(criterion)
					.add(Restrictions.eq("tahunAkademik", tahunAkademik)).add(Restrictions.in("semester",
							semester.equals(Perkuliahan.GANJIL) ? Common.ganjil : Common.genap))
					.list();

			Integer qtyBimbinganSkripis = skripsis.size();

			if (qtyBimbinganSkripis > 0) {

				double jml = qtyBimbinganSkripis.doubleValue();

				Double sks = 0.0;

				String key = "jumlah_sks_ujian_tugas_akhir";
				String newKey = key + "_" + jenjang.getId(); 

				ParameterUmum konfigurasi = Common.getParameterUmum(newKey, "0.0");

				double maks = 8.0;
				try {
					maks = Double.parseDouble(konfigurasi.getInfo1());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/bkd/helper/BkdPengujiSkripsiHelper.java:121");

				}

				if (jml > maks) {
					jml = maks;
				}

				try {
					sks = Double.parseDouble(konfigurasi.getNilai()) * jml;
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e); 
				}

				String keterangan = dosen.getNama() + " menguji tugas akhir sebanyak " + qtyBimbinganSkripis
						+ " mahasiswa " + jenjang.getNama() + ". Jumlah total sks yang di dapat sebanyak "
						+ Common.numberFormat.get().format(sks) + ". Maksimal mahasiswa yang bisa diuji adalah "
						+ Common.numberFormat.get().format(maks) + ". ";

				for (Skripsi skripsi : skripsis) {
					keterangan += "Mahasiswa \"" + skripsi.getMahasiswa().getNim() + "-"
							+ skripsi.getMahasiswa().getNama() + "\" dengan judul \"" + skripsi.getJudul() + "\". ";
				}

				AsesemenPenilaian asesemenPenilaian = (AsesemenPenilaian) session
						.createCriteria(AsesemenPenilaian.class).add(Restrictions.eq("pegawai.id", dosen.getPegawaiId()))
						.add(Restrictions.eq("spesifikasi", PenilaianAsesor.PENGUJI_TA))
						.add(Restrictions.eq("jenjang", jenjang)).add(Restrictions.eq("tahunAkademik", tahunAkademik))
						.add(Restrictions.eq("semester", semester)).setMaxResults(1).uniqueResult();
				if (asesemenPenilaian == null) {
					asesemenPenilaian = new AsesemenPenilaian();
				}
				asesemenPenilaian.setBidang(PenunjangKinerjaDosen.PENDIDIKAN);
				asesemenPenilaian.setKeterangan(keterangan);
				asesemenPenilaian.setSpesifikasi(PenilaianAsesor.PENGUJI_TA);
				asesemenPenilaian.setJenjang(jenjang);
				asesemenPenilaian.setTahunAkademik(tahunAkademik);
				asesemenPenilaian.setSemester(semester);
				asesemenPenilaian.setPegawai( new Pegawai(dosen));
				asesemenPenilaian.setSks(sks);

				session.getTransaction().begin();
				session.saveOrUpdate(asesemenPenilaian);
				session.getTransaction().commit();

				PenilaianAsesorAction.checkPenilaian(session, asesors, asesemenPenilaian);

			}
		}
	}

}
