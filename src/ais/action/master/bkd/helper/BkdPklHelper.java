package ais.action.master.bkd.helper;

import java.util.List;
import java.util.TreeSet;

import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zul.Label;

import ais.action.master.KonfigurasiBkdAction;
import ais.action.master.bkd.PenilaianAsesorAction;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.AsesemenPenilaian;
import ais.database.model.Asesor;
import ais.database.model.AsesorPegawai;
import ais.database.model.CommonVO;
import ais.database.model.Dosen;
import ais.database.model.Jenjang;
import ais.database.model.MahasiswaDapatKelompokPkl;
import ais.database.model.ParameterUmum;
import ais.database.model.Pegawai;
import ais.database.model.PenilaianAsesor;
import ais.database.model.PenunjangKinerjaDosen;

/**
 * Helper batch modul BKD (Beban Kerja Dosen) yang menghitung SKS pembimbingan PKL (Praktik Kerja
 * Lapangan) dosen dan mencatatnya sebagai {@link AsesemenPenilaian} (bidang
 * {@link PenunjangKinerjaDosen#PENDIDIKAN}, spesifikasi {@link PenilaianAsesor#PEMBIMBING_PKL})
 * untuk satu tahun akademik/semester — pola dan struktur kerjanya identik dengan
 * {@code BkdBimbinganSkripsiHelper}, hanya sumber datanya berupa
 * {@link MahasiswaDapatKelompokPkl} (dosen pembimbing 1-5 pada {@code kelompokPkl}) alih-alih
 * bimbingan tugas akhir.
 *
 * <p>
 * SKS dihitung dari jumlah mahasiswa bimbingan PKL dosen pada jenjang/periode tertentu, dipetakan
 * ke nilai SKS lewat tabel rentang di konfigurasi {@link ParameterUmum} berkunci
 * {@code jumlah_sks_pembimbing_pkl_<idJenjang>} (default {@code "1-25=1;26-50=2;51-75=3;76-100=4"},
 * diterjemahkan lewat {@code KonfigurasiBkdAction.terjemahkanNilai}). Setelah
 * {@link AsesemenPenilaian} disimpan, {@code PenilaianAsesorAction.checkPenilaian} memastikan baris
 * {@link PenilaianAsesor} terkait tersinkron dengan {@link Asesor} yang berlaku.
 * </p>
 */
public class BkdPklHelper {

	/** Memproses seluruh {@link Jenjang} aktif untuk {@code pegawai} (atau seluruh dosen pembimbing PKL bila {@code null}) pada tahun akademik/semester tertentu, mendelegasikan ke overload per-jenjang. */
	@SuppressWarnings("unchecked")
	public static void populate(Session session, final Pegawai pegawai, String tahunAkademik, String semester,
			Label label) {
		List<Jenjang> jenjangs = HibernateUtil.currentSession().createCriteria(Jenjang.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();
		for (Jenjang jenjang : jenjangs) {
			BkdPklHelper.populate(session, pegawai, jenjang, tahunAkademik, semester, label);
		}
	}

	@SuppressWarnings("unchecked")
	/**
	 * Untuk satu {@code jenjang}: bila {@code pegawai} diberikan (dan berelasi ke {@link Dosen}),
	 * memproses dosen tersebut langsung. Bila {@code pegawai} {@code null}, mengumpulkan seluruh
	 * dosen yang tercatat sebagai pembimbing 1-5 pada {@link MahasiswaDapatKelompokPkl} yang
	 * diterima ({@code diterima=true}) pada periode tersebut (dideduplikasi lewat {@link TreeSet}),
	 * lalu memproses masing-masing.
	 */
	public static void populate(Session session, final Pegawai pegawai, Jenjang jenjang, String tahunAkademik,
			String semester, Label label) {

		if (pegawai != null && pegawai.getDosen() != null) {
			BkdPklHelper.populate(session, pegawai.getDosen(), jenjang, tahunAkademik, semester, label);
		} else {

			String[] cols = new String[] { "dosen_pembimbing1", "dosen_pembimbing2", "dosen_pembimbing3",
					"dosen_pembimbing4", "dosen_pembimbing5" };

			TreeSet<Dosen> dosens = new TreeSet<Dosen>();
			for (String col : cols) {
				dosens.addAll(session.createCriteria(MahasiswaDapatKelompokPkl.class)
						.add(Restrictions.eq("diterima", true)).createAlias("kelompokPkl", "kelompokPkl")
						.createAlias("kelompokPkl.pkl", "pkl").createAlias("mahasiswa", "mahasiswa")
						.createAlias("mahasiswa.jurusan", "jurusan").add(Restrictions.eq("jurusan.jenjang", jenjang))
						.setProjection(Projections.groupProperty("kelompokPkl." + col))
						.add(Restrictions.isNotNull("kelompokPkl." + col))
						.add(Restrictions.eq("pkl.tahunAkademik", tahunAkademik))
						.add(Restrictions.eq("pkl.semester", semester)).list());
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
				BkdPklHelper.populate(session, dosen, jenjang, tahunAkademik, semester, label);
			}
		}
	}

	@SuppressWarnings("unchecked")
	/**
	 * Implementasi kanonik: menghitung jumlah mahasiswa bimbingan PKL {@code dosen} (sebagai salah
	 * satu dari pembimbing1-5) pada {@code jenjang}/{@code tahunAkademik}/{@code semester},
	 * memetakannya ke nilai SKS lewat konfigurasi rentang {@code jumlah_sks_pembimbing_pkl}, lalu
	 * menyimpan/memperbarui satu baris {@link AsesemenPenilaian}. Tidak melakukan apa pun bila
	 * {@code dosen} {@code null}/tidak berelasi pegawai, atau bila dosen tidak punya {@link Asesor}
	 * aktif.
	 */
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

			String key = "jumlah_sks_pembimbing_pkl";
			String newKey = key + "_" + jenjang.getId();

			ParameterUmum konfigurasi = Common.getParameterUmum(newKey, "1-25=1;26-50=2;51-75=3;76-100=4"); 

			List<CommonVO> commonVOs = KonfigurasiBkdAction.terjemahkanNilai(konfigurasi.getNilai().trim(),
					konfigurasi.getInfo1().trim());

			System.out.println("commonVOs " + konfigurasi.getNilai().trim() + " => " + commonVOs);

			Criterion criterion = Restrictions.eq("kelompokPkl.dosen_pembimbing1", dosen);

			String[] cols = new String[] { "dosen_pembimbing2", "dosen_pembimbing3", "dosen_pembimbing4",
					"dosen_pembimbing5" };
			for (String c : cols) {
				criterion = Restrictions.or(criterion, Restrictions.eq("kelompokPkl." + c, dosen));
			}

			double jumlahMhs = ((Number) session.createCriteria(MahasiswaDapatKelompokPkl.class)
					.add(Restrictions.eq("diterima", true)).createAlias("kelompokPkl", "kelompokPkl")
					.createAlias("kelompokPkl.pkl", "pkl").createAlias("mahasiswa", "mahasiswa")
					.createAlias("mahasiswa.jurusan", "jurusan").add(Restrictions.eq("jurusan.jenjang", jenjang))
					.setProjection(Projections.rowCount()).add(criterion)
					.add(Restrictions.eq("pkl.tahunAkademik", tahunAkademik))
					.add(Restrictions.eq("pkl.semester", semester)).uniqueResult()).doubleValue();

			double sks = 0.0;
			String keterangan = dosen.getNama() + " membimbing PKL sebanyak " + Common.numberFormat.get().format(jumlahMhs)
					+ " mahasiswa " + jenjang.getNama() + ".";

			String keteranganTambahan = "";
			CommonVO commonVOPilihan = null;
			for (CommonVO commonVO : commonVOs) {
				if (jumlahMhs >= commonVO.getMulai() && jumlahMhs <= commonVO.getSampai()) {
					sks = commonVO.getNilai();
					commonVOPilihan = commonVO;
					break;
				}
			}

			if (commonVOPilihan != null) {
				keteranganTambahan = "Mulai " + Common.numberFormat.get().format(commonVOPilihan.getMulai()) + " s.d "
						+ Common.numberFormat.get().format(commonVOPilihan.getSampai()) + " "
						+ (commonVOPilihan.getPersen()
								? "persen yang di dapat dari SKS "
										+ Common.numberFormat.get().format(commonVOPilihan.getNilai()) + "%"
								: "nilai yang di dapat adalah " + Common.numberFormat.get().format(commonVOPilihan.getNilai())
										+ " SKS.")
						+ ", sehingga jumlah SKS akhir yang diperoleh adalah " + Common.numberFormat.get().format(sks);
			}

			keterangan += (" " + keteranganTambahan);

			AsesemenPenilaian asesemenPenilaian = (AsesemenPenilaian) session.createCriteria(AsesemenPenilaian.class)
					.add(Restrictions.eq("pegawai.id", dosen.getPegawaiId()))
					.add(Restrictions.eq("spesifikasi", PenilaianAsesor.PEMBIMBING_PKL))
					.add(Restrictions.eq("tahunAkademik", tahunAkademik)).add(Restrictions.eq("jenjang", jenjang))
					.add(Restrictions.eq("semester", semester)).setMaxResults(1).uniqueResult();
			if (asesemenPenilaian == null) {
				asesemenPenilaian = new AsesemenPenilaian();
			}

			asesemenPenilaian.setSpesifikasi(PenilaianAsesor.PEMBIMBING_PKL);
			asesemenPenilaian.setJenjang(jenjang);
			asesemenPenilaian.setBidang(PenunjangKinerjaDosen.PENDIDIKAN);
			asesemenPenilaian.setKeterangan(keterangan);
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
