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
import ais.database.model.MahasiswaDapatKelompokKkn;
import ais.database.model.ParameterUmum;
import ais.database.model.Pegawai;
import ais.database.model.PenilaianAsesor;
import ais.database.model.PenunjangKinerjaDosen;

/**
 * Helper penghitung dan penyimpan penilaian beban kinerja dosen (BKD) untuk peran
 * <b>pembimbing KKN</b>: mengonversi jumlah mahasiswa bimbingan KKN seorang dosen (per jenjang,
 * tahun akademik, semester) menjadi angka SKS penunjang kinerja lewat tabel konfigurasi
 * ({@code jumlah_sks_pembimbing_kkn[_jenjangId]}), lalu menyimpannya sebagai
 * {@link AsesemenPenilaian} dan memicu pemeriksaan status penilaian asesor terkait lewat
 * {@link PenilaianAsesorAction#checkPenilaian}. Tiga overload {@code populate} membentuk
 * hierarki cakupan: seluruh jenjang -> satu jenjang (untuk satu pegawai atau seluruh dosen
 * pembimbing KKN pada jenjang itu) -> satu dosen tertentu.
 */
public class BkdKknHelper {

	/**
	 * Memproses seluruh {@link Jenjang} aktif untuk {@code pegawai} (atau, bila {@code pegawai}
	 * bukan dosen, untuk seluruh dosen pembimbing KKN) pada {@code tahunAkademik}/{@code semester}
	 * yang diberikan, mendelegasikan ke {@link #populate(Session, Pegawai, Jenjang, String, String, Label)}
	 * per jenjang.
	 *
	 * @param session       sesi Hibernate aktif
	 * @param pegawai       pegawai yang diproses; bila bukan dosen, method memproses seluruh dosen
	 *                      pembimbing KKN yang ditemukan
	 * @param tahunAkademik tahun akademik yang dinilai
	 * @param semester      semester yang dinilai
	 * @param label         komponen label UI untuk menampilkan progres (boleh {@code null})
	 */
	@SuppressWarnings("unchecked")
	public static void populate(Session session, final Pegawai pegawai, String tahunAkademik, String semester,
			Label label) {
		List<Jenjang> jenjangs = HibernateUtil.currentSession().createCriteria(Jenjang.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();
		for (Jenjang jenjang : jenjangs) {
			BkdKknHelper.populate(session, pegawai, jenjang, tahunAkademik, semester, label);
		}
	}

	/**
	 * Memproses satu {@code jenjang} untuk {@code pegawai}: bila {@code pegawai} memiliki data
	 * dosen, mendelegasikan langsung ke {@link #populate(Session, Dosen, Jenjang, String, String, Label)};
	 * bila tidak (mis. dipanggil dengan {@code pegawai=null} untuk memproses semua), method
	 * mencari seluruh dosen yang tercatat sebagai salah satu dari lima pembimbing kelompok KKN
	 * pada jenjang/tahun akademik/semester tersebut lalu memproses masing-masing satu per satu,
	 * memperbarui {@code label} dengan persentase progres di setiap iterasi.
	 *
	 * @param session       sesi Hibernate aktif
	 * @param pegawai       pegawai yang diproses, atau {@code null}/non-dosen untuk memproses seluruh dosen pembimbing KKN
	 * @param jenjang       jenjang pendidikan yang diproses
	 * @param tahunAkademik tahun akademik yang dinilai
	 * @param semester      semester yang dinilai
	 * @param label         komponen label UI untuk menampilkan progres (boleh {@code null})
	 */
	@SuppressWarnings("unchecked")
	public static void populate(Session session, final Pegawai pegawai, Jenjang jenjang, String tahunAkademik,
			String semester, Label label) {

		if (pegawai != null && pegawai.getDosen() != null) {
			BkdKknHelper.populate(session, pegawai.getDosen(), jenjang, tahunAkademik, semester, label);
		} else {

			String[] cols = new String[] { "dosen_pembimbing1", "dosen_pembimbing2", "dosen_pembimbing3",
					"dosen_pembimbing4", "dosen_pembimbing5" };

			TreeSet<Dosen> dosens = new TreeSet<Dosen>();
			for (String col : cols) {
				dosens.addAll(session.createCriteria(MahasiswaDapatKelompokKkn.class)
						.add(Restrictions.eq("diterima", true)).createAlias("kelompokKkn", "kelompokKkn")
						.createAlias("kelompokKkn.kkn", "kkn").createAlias("mahasiswa", "mahasiswa")
						.createAlias("mahasiswa.jurusan", "jurusan").add(Restrictions.eq("jurusan.jenjang", jenjang))
						.setProjection(Projections.groupProperty("kelompokKkn." + col))
						.add(Restrictions.isNotNull("kelompokKkn." + col))
						.add(Restrictions.eq("kkn.tahunAkademik", tahunAkademik))
						.add(Restrictions.eq("kkn.semester", semester)).list());
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
				BkdKknHelper.populate(session, dosen, jenjang, tahunAkademik, semester, label);
			}
		}
	}

	/**
	 * Menghitung dan menyimpan penilaian pembimbing KKN untuk satu {@code dosen} tertentu. Tidak
	 * melakukan apa pun bila {@code dosen} tidak memiliki asesor aktif atau tidak membimbing
	 * mahasiswa KKN pada jenjang/tahun akademik/semester tersebut. Bila memenuhi syarat: jumlah
	 * mahasiswa bimbingan (di posisi pembimbing 1-5 kelompok KKN) dipetakan ke nilai SKS lewat
	 * rentang konfigurasi {@code jumlah_sks_pembimbing_kkn[_jenjangId]}
	 * ({@link KonfigurasiBkdAction#terjemahkanNilai}), lalu disimpan/diperbarui sebagai baris
	 * {@link AsesemenPenilaian} (spesifikasi {@link PenilaianAsesor#PEMBIMBING_KKN}, bidang
	 * {@link PenunjangKinerjaDosen#PENDIDIKAN}) dalam transaksi Hibernate sendiri, dan status
	 * penilaian asesor terkait diperbarui lewat {@link PenilaianAsesorAction#checkPenilaian}.
	 *
	 * @param session       sesi Hibernate aktif
	 * @param dosen         dosen yang dinilai
	 * @param jenjang       jenjang pendidikan yang diproses
	 * @param tahunAkademik tahun akademik yang dinilai
	 * @param semester      semester yang dinilai
	 * @param label         komponen label UI untuk progres (tidak dipakai pada overload ini)
	 */
	@SuppressWarnings("unchecked")
	public static void populate(Session session, Dosen dosen, Jenjang jenjang, String tahunAkademik, String semester,
			Label label) {

		if (dosen == null || dosen.getPegawaiId() == null) {
			return;
		}

		List<Asesor> asesors = session.createCriteria(AsesorPegawai.class).createAlias("pegawai", "pegawai")
				.createAlias("asesor", "asesor")
				.add(Restrictions.or(Restrictions.isNull("asesor.aktif"), Restrictions.eq("asesor.aktif", true)))
				.add(Restrictions.eq("pegawai.dosen", dosen)).setProjection(Projections.groupProperty("asesor")).list();

		System.out.println("dosen => " + dosen + ", asesors => " + asesors);
		if (!asesors.isEmpty()) {

			String key = "jumlah_sks_pembimbing_kkn";
			String newKey = key + "_" + jenjang.getId();

			ParameterUmum konfigurasi = Common.getParameterUmum(newKey, "1-25=1;26-50=2;51-75=3;76-100=4");

			List<CommonVO> commonVOs = KonfigurasiBkdAction.terjemahkanNilai(konfigurasi.getNilai().trim(),
					konfigurasi.getInfo1().trim());

			System.out.println("commonVOs " + konfigurasi.getNilai().trim() + " => " + commonVOs);

			Criterion criterion = Restrictions.eq("kelompokKkn.dosen_pembimbing1", dosen);

			String[] cols = new String[] { "dosen_pembimbing2", "dosen_pembimbing3", "dosen_pembimbing4",
					"dosen_pembimbing5" };
			for (String c : cols) {
				criterion = Restrictions.or(criterion, Restrictions.eq("kelompokKkn." + c, dosen));
			}

			double jumlahMhs = ((Number) session.createCriteria(MahasiswaDapatKelompokKkn.class)
					.add(Restrictions.eq("diterima", true)).createAlias("kelompokKkn", "kelompokKkn")
					.createAlias("kelompokKkn.kkn", "kkn").createAlias("mahasiswa", "mahasiswa")
					.createAlias("mahasiswa.jurusan", "jurusan").add(Restrictions.eq("jurusan.jenjang", jenjang))
					.setProjection(Projections.rowCount()).add(criterion)
					.add(Restrictions.eq("kkn.tahunAkademik", tahunAkademik))
					.add(Restrictions.eq("kkn.semester", semester)).uniqueResult()).doubleValue();

			if (jumlahMhs > 0.01) {

				double sks = 0.0;
				String keterangan = dosen.getNama() + " membimbing KKN sebanyak "
						+ Common.numberFormat.get().format(jumlahMhs) + " mahasiswa " + jenjang.getNama() + ".";

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
									: "nilai yang di dapat adalah "
											+ Common.numberFormat.get().format(commonVOPilihan.getNilai()) + " SKS.")
							+ ", sehingga jumlah SKS akhir yang diperoleh adalah " + Common.numberFormat.get().format(sks);
				}

				keterangan += (" " + keteranganTambahan);

				AsesemenPenilaian asesemenPenilaian = (AsesemenPenilaian) session
						.createCriteria(AsesemenPenilaian.class)
						.add(Restrictions.eq("pegawai.id", dosen.getPegawaiId()))
						.add(Restrictions.eq("spesifikasi", PenilaianAsesor.PEMBIMBING_KKN))
						.add(Restrictions.eq("tahunAkademik", tahunAkademik)).add(Restrictions.eq("jenjang", jenjang))
						.add(Restrictions.eq("semester", semester)).setMaxResults(1).uniqueResult();
				if (asesemenPenilaian == null) {
					asesemenPenilaian = new AsesemenPenilaian();
				}

				asesemenPenilaian.setSpesifikasi(PenilaianAsesor.PEMBIMBING_KKN);
				asesemenPenilaian.setJenjang(jenjang);
				asesemenPenilaian.setBidang(PenunjangKinerjaDosen.PENDIDIKAN);
				asesemenPenilaian.setKeterangan(keterangan);
				asesemenPenilaian.setTahunAkademik(tahunAkademik);
				asesemenPenilaian.setSemester(semester);
				asesemenPenilaian.setPegawai(new Pegawai(dosen));
				asesemenPenilaian.setSks(sks);

				session.getTransaction().begin();
				session.saveOrUpdate(asesemenPenilaian);
				session.getTransaction().commit();

				PenilaianAsesorAction.checkPenilaian(session, asesors, asesemenPenilaian);
			}
		}
	}

}
