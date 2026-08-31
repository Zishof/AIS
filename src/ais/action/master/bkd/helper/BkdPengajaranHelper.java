package ais.action.master.bkd.helper;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zul.Label;

import ais.action.master.KonfigurasiBkdAction;
import ais.action.master.bkd.PenilaianAsesorAction;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.model.AsesemenPenilaian;
import ais.database.model.Asesor;
import ais.database.model.AsesorPegawai;
import ais.database.model.CommonVO;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Dosen;
import ais.database.model.Jenjang;
import ais.database.model.Konfigurasi;
import ais.database.model.Matakuliah;
import ais.database.model.ParameterUmum;
import ais.database.model.Pegawai;
import ais.database.model.PenilaianAsesor;
import ais.database.model.PenunjangKinerjaDosen;
import ais.database.model.Perkuliahan;

/**
 * Helper modul BKD untuk mempopulasi ulang penilaian asesmen kegiatan <b>pengajaran</b> dosen.
 * Mendukung dua mode penghitungan, dipilih lewat konfigurasi
 * {@code penghitungan_bkd_pengajaran_menggunakan_per_perkuliahan}: mode <i>per perkuliahan</i>
 * (SKS dihitung langsung per baris {@link Perkuliahan} yang diampu, dibagi antar dosen pengampu
 * bila lebih dari satu) dan mode <i>lama</i> berbasis {@link Detailperkuliahan} yang disetujui,
 * dikelompokkan per mata kuliah, dengan SKS disesuaikan tabel skala {@code jumlah_sks_pengajaran_*}
 * berdasarkan jumlah mahasiswa yang diajar serta faktor beban berdasar jabatan
 * fungsional/status sertifikasi dosen. Dosen dicari sebagai pengampu di salah satu dari 10 slot
 * ({@code dosen1}..{@code dosen10}) pada {@link Perkuliahan}, per jenjang pendidikan (S1/S2/S3/D3/D4).
 */
public class BkdPengajaranHelper {

	/**
	 * Mempopulasi ulang penilaian pengajaran untuk seluruh slot dosen pengampu ({@code dosen1}..
	 * {@code dosen10}) — bila mode per-perkuliahan aktif, dijalankan langsung tanpa filter jenjang;
	 * bila tidak, dijalankan per kombinasi jenjang (S1/S2/S3/D3/D4) x slot dosen.
	 *
	 * @param session       sesi Hibernate aktif
	 * @param pegawai       dosen yang diproses, atau {@code null} untuk semua dosen pengajar
	 * @param tahunAkademik tahun akademik penilaian
	 * @param semester      semester penilaian
	 * @param label         komponen label UI tempat progres ditampilkan
	 */
	public static void populate(Session session, final Pegawai pegawai, String tahunAkademik, String semester,
			Label label) {
		Jenjang[] jenjangs = new Jenjang[] { ConstantValues.s1, ConstantValues.s2, ConstantValues.s3, ConstantValues.d3,
				ConstantValues.d4 };
		String[] dosens = new String[] { "dosen1", "dosen2", "dosen3", "dosen4", "dosen5", "dosen6", "dosen7", "dosen8",
				"dosen9", "dosen10" };

		if (Common.bolehKonfigurasi("penghitungan_bkd_pengajaran_menggunakan_per_perkuliahan")) {
			for (String colDosen : dosens) {
				BkdPengajaranHelper.populate(session, pegawai, colDosen, null, tahunAkademik, semester, label);
			}
		} else {
			for (Jenjang jenjang : jenjangs) {
				for (String colDosen : dosens) {
					BkdPengajaranHelper.populate(session, pegawai, colDosen, jenjang, tahunAkademik, semester, label);
				}
			}
		}
	}

	/**
	 * Mempopulasi ulang penilaian pengajaran untuk slot pengampu {@code colDosen} dan
	 * {@code jenjang} tertentu: bila {@code pegawai} adalah dosen, langsung diproses; bila
	 * {@code null}, seluruh dosen yang mengampu perkuliahan aktif pada slot dan jenjang tersebut
	 * dicari lebih dulu lalu diproses satu per satu, dengan progres persentase ditampilkan ke
	 * {@code label}.
	 *
	 * @param session       sesi Hibernate aktif
	 * @param pegawai       dosen yang diproses, atau {@code null} untuk semua dosen pada slot+jenjang ini
	 * @param colDosen      nama kolom slot pengampu ({@code "dosen1"}..{@code "dosen10"})
	 * @param jenjang       jenjang pendidikan (diabaikan bila mode per-perkuliahan aktif)
	 * @param tahunAkademik tahun akademik penilaian
	 * @param semester      semester penilaian
	 * @param label         komponen label UI tempat progres ditampilkan, boleh {@code null}
	 */
	@SuppressWarnings("unchecked")
	public static void populate(Session session, final Pegawai pegawai, String colDosen, Jenjang jenjang,
			String tahunAkademik, String semester, Label label) {

		if (pegawai != null && pegawai.getDosen() != null) {
			BkdPengajaranHelper.populate(session, pegawai.getDosen(), colDosen, jenjang, tahunAkademik, semester,
					label);
		} else {

			List<Dosen> dosens = session.createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).createAlias("jurusan", "jurusan")
					.add(Restrictions.eq("jurusan.jenjang", jenjang)).setProjection(Projections.groupProperty(colDosen))
					.add(Restrictions.isNotNull(colDosen)).add(Restrictions.eq("tahunAjaran", tahunAkademik))
					.add(Restrictions.in("semester",
							semester.equals(Perkuliahan.GANJIL) ? Common.ganjil : Common.genap))
					.list();

			System.out.println("jenjang => " + jenjang + ", colDosen => " + colDosen + ", banyak " + dosens.size());
			int rowCount = dosens.size();
			int i = 0;
			for (Dosen dosen : dosens) {
				if (label != null) {
					label.setValue("Memproses data pengajaran \"" + dosen + "\" ("
							+ Common.numberFormat.get().format(i * 100.0 / rowCount) + " %)");
				}
				i++;
				BkdPengajaranHelper.populate(session, dosen, colDosen, jenjang, tahunAkademik, semester, label);
			}
		}
	}

	/**
	 * Menghitung dan menyimpan/memutakhirkan baris {@link AsesemenPenilaian} pengajaran untuk
	 * {@code dosen} pada slot {@code colDosen} dan {@code jenjang}. Mencari asesor aktif dosen ini
	 * terlebih dahulu (tanpa asesor, method tidak melakukan apa pun). Bila konfigurasi
	 * {@code penghitungan_bkd_pengajaran_menggunakan_per_perkuliahan} aktif, satu baris asesmen
	 * dibuat per {@link Perkuliahan} yang diampu (SKS mata kuliah dibagi antar dosen pengampu bila
	 * lebih dari satu, sesuai {@code pengaturan_juml_sks_beban}: 50% atau dibagi rata). Bila tidak,
	 * satu baris dibuat per mata kuliah (dikelompokkan dari {@link Detailperkuliahan} yang
	 * disetujui), dengan SKS disesuaikan tabel skala {@code jumlah_sks_pengajaran_*} (bergantung
	 * jabatan fungsional dan jenjang) berdasarkan jumlah mahasiswa yang diajar.
	 *
	 * @param session       sesi Hibernate aktif
	 * @param dosen         dosen pengajar; method langsung kembali tanpa efek bila {@code null} atau
	 *                      belum tertaut data pegawai
	 * @param colDosen      nama kolom slot pengampu ({@code "dosen1"}..{@code "dosen10"})
	 * @param jenjang       jenjang pendidikan mata kuliah
	 * @param tahunAkademik tahun akademik penilaian
	 * @param semester      semester penilaian
	 * @param label         tidak dipakai langsung di method ini (parameter diteruskan dari pemanggil)
	 */
	@SuppressWarnings("unchecked")
	public static void populate(Session session, Dosen dosen, String colDosen, Jenjang jenjang, String tahunAkademik,
			String semester, Label label) {

		if (dosen == null || dosen.getPegawaiId() == null) {
			return;
		}

		List<Asesor> asesors = session.createCriteria(AsesorPegawai.class).createAlias("pegawai", "pegawai")
				.createAlias("asesor", "asesor")
				.add(Restrictions.or(Restrictions.isNull("asesor.aktif"), Restrictions.eq("asesor.aktif", true)))
				.add(Restrictions.eq("pegawai.dosen", dosen)).setProjection(Projections.groupProperty("asesor")).list();

		System.out.println("dosen => " + dosen + ", asesors => " + asesors + ", dosen.getJabatanFungsionalDosen() = "
				+ dosen.getJabatanFungsionalDosen());
		if (!asesors.isEmpty()) {

			if (Common.bolehKonfigurasi("penghitungan_bkd_pengajaran_menggunakan_per_perkuliahan")) {

				List<Perkuliahan> perkuliahans = session.createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.eq(colDosen, dosen)).add(Restrictions.eq("tahunAjaran", tahunAkademik))
						.add(Restrictions.in("semester",
								semester.equals(Perkuliahan.GANJIL) ? Common.ganjil : Common.genap))
						.list();

				String pengaturan_juml_sks_beban = Common.getParameterUmum("pengaturan_juml_sks_beban", "50%")
						.getNilai();

				System.out.println("perkuliahans => " + perkuliahans.size() + ", pengaturan_juml_sks_beban = "
						+ pengaturan_juml_sks_beban);

				for (Perkuliahan perkuliahan : perkuliahans) {

					Matakuliah matakuliah = perkuliahan.getMatakuliah();
					Integer jumlahDosen = perkuliahan.getJumlahDosen();

					Double sks = matakuliah == null ? 0.0 : matakuliah.getSks().doubleValue();

					String keterangan = dosen.getNama()
							+ (dosen.getJabatanFungsionalDosen() == null ? ""
									: " (" + dosen.getJabatanFungsionalDosen().getNama() + ")")
							+ "  mengajar matakuliah " + matakuliah.getKode() + "-" + matakuliah.getNama()
							+ " dengan jumlah SKS " + sks + " di kelas " + perkuliahan.getKelas() + " semester "
							+ perkuliahan.getSemester() + ". Jumlah pengajar sebanyak " + jumlahDosen + ".";
					if (jumlahDosen > 1) {
						if (pengaturan_juml_sks_beban.equalsIgnoreCase("50%")) {
							sks = sks / 2.0;
						} else if (pengaturan_juml_sks_beban.equalsIgnoreCase("Dibagi rata")) {
							sks = sks / jumlahDosen.doubleValue();
						}
					}

					AsesemenPenilaian asesemenPenilaian = (AsesemenPenilaian) session
							.createCriteria(AsesemenPenilaian.class)
							.add(Restrictions.eq("pegawai.id", dosen.getPegawaiId()))
							.add(Restrictions.eq("perkuliahan", perkuliahan))
							.add(Restrictions.eq("tahunAkademik", tahunAkademik))
							.add(Restrictions.eq("jenjang", perkuliahan.getJurusan().getJenjang()))
							.add(Restrictions.eq("semester", semester)).setMaxResults(1).uniqueResult();
					if (asesemenPenilaian == null) {
						asesemenPenilaian = new AsesemenPenilaian();
					}

					asesemenPenilaian.setSpesifikasi(PenilaianAsesor.PENGAJARAN);
					asesemenPenilaian.setJenjang(perkuliahan.getJurusan().getJenjang());
					asesemenPenilaian.setBidang(PenunjangKinerjaDosen.PENDIDIKAN);
					asesemenPenilaian.setKeterangan(keterangan);
					asesemenPenilaian.setPerkuliahan(perkuliahan);
					asesemenPenilaian.setTahunAkademik(tahunAkademik);
					asesemenPenilaian.setSemester(semester);
					asesemenPenilaian.setPegawai(new Pegawai(dosen));
					asesemenPenilaian.setSks(sks);

					session.getTransaction().begin();
					session.saveOrUpdate(asesemenPenilaian);
					session.getTransaction().commit();

					PenilaianAsesorAction.checkPenilaian(session, asesors, asesemenPenilaian);
				}

			} else {

				ParameterUmum jumlahSksPengajaran;
				if (dosen.getJabatanFungsionalDosen() == null) {
					jumlahSksPengajaran = Common.getParameterUmum("jumlah_sks_pengajaran_" + jenjang.getId(),
							"1-40=100%;41-80=150%;81-120=200%;121-160=250%");
				} else {
					jumlahSksPengajaran = Common.getParameterUmum("jumlah_sks_pengajaran_"
							+ dosen.getJabatanFungsionalDosen().getId() + "_" + jenjang.getId(),
							"1-40=100%;41-80=150%;81-120=200%;121-160=250%");
				}

				List<CommonVO> commonVOs = KonfigurasiBkdAction.terjemahkanNilai(jumlahSksPengajaran.getNilai().trim(),
						jumlahSksPengajaran.getInfo1().trim());

				String pengaturan_juml_sks_beban = Common.getParameterUmum("pengaturan_juml_sks_beban", "50%")
						.getNilai();

				if (dosen.getJabatanFungsionalDosen() == null) {
					if (dosen.getSertifikasi()) {
						jumlahSksPengajaran = Common.getParameterUmum("pengaturan_juml_sks_bebanTelah Sertifikasi",
								"50%");
					} else {
						jumlahSksPengajaran = Common.getParameterUmum("pengaturan_juml_sks_bebanBelum Sertifikasi",
								"50%");
					}
				} else {
					if (dosen.getSertifikasi()) {
						jumlahSksPengajaran = Common.getParameterUmum(
								"pengaturan_juml_sks_beban" + dosen.getJabatanFungsionalDosen() + "Telah Sertifikasi",
								"50%");
					} else {
						jumlahSksPengajaran = Common.getParameterUmum(
								"pengaturan_juml_sks_beban" + dosen.getJabatanFungsionalDosen() + "Belum Sertifikasi",
								"50%");
					}
				}

				System.out.println("commonVOs " + jumlahSksPengajaran.getNilai().trim() + " => " + commonVOs
						+ ", pengaturan_juml_sks_beban => " + pengaturan_juml_sks_beban);

				List<Object[]> perkuliahans = session.createCriteria(Detailperkuliahan.class)
						.add(Restrictions.eq("persetujuan", Detailperkuliahan.DISETUJUI))
						.createAlias("perkuliahan", "perkuliahan").createAlias("perkuliahan.jurusan", "jurusan")
						.add(Restrictions.eq("jurusan.jenjang", jenjang)).setProjection(Projections.projectionList()

								.add(Projections.groupProperty("perkuliahan.matakuliah")).add(Projections.rowCount())
								.add(Projections.max("perkuliahan.jumlahDosen")))

						.add(Restrictions.eq("perkuliahan." + colDosen, dosen))
						.add(Restrictions.eq("perkuliahan.tahunAjaran", tahunAkademik))
						.add(Restrictions.in("perkuliahan.semester",
								semester.equals(Perkuliahan.GANJIL) ? Common.ganjil : Common.genap))
						.list();
				for (Object[] obj : perkuliahans) {

					Matakuliah matakuliah = (Matakuliah) obj[0];
					Integer jumlahMhs = (Integer) (obj[1] == null ? 0 : obj[1]);
					Integer jumlahDosen = (Integer) (obj[2] == null ? 0 : obj[2]);

					Double sks = matakuliah == null ? 0.0 : matakuliah.getSks().doubleValue();

					String keterangan = dosen.getNama()
							+ (dosen.getJabatanFungsionalDosen() == null ? ""
									: " (" + dosen.getJabatanFungsionalDosen().getNama() + ")")
							+ "  mengajar matakuliah " + matakuliah.getKode() + "-" + matakuliah.getNama()
							+ " dengan jumlah SKS " + sks + ". Jumlah total mahasiswa yang diajar " + jumlahMhs
							+ " dengan jumlah pengajar sebanyak " + jumlahDosen + ".";

					if (jumlahDosen > 1) {
						if (pengaturan_juml_sks_beban.equalsIgnoreCase("50%")) {
							sks = sks / 2.0;
						} else if (pengaturan_juml_sks_beban.equalsIgnoreCase("Dibagi rata")) {
							sks = sks / jumlahDosen.doubleValue();
						}
					}

					String keteranganTambahan = "";
					CommonVO commonVOPilihan = null;
					for (CommonVO commonVO : commonVOs) {
						if (jumlahMhs.doubleValue() >= commonVO.getMulai()
								&& jumlahMhs.doubleValue() <= commonVO.getSampai()) {
							if (commonVO.getPersen()) {
								sks = (sks * commonVO.getNilai()) / 100.0;
							} else {
								sks = commonVO.getNilai();
							}
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
								+ ", sehingga jumlah SKS akhir yang diperoleh adalah "
								+ Common.numberFormat.get().format(sks);
					}

					keterangan += (" " + keteranganTambahan);

					AsesemenPenilaian asesemenPenilaian = (AsesemenPenilaian) session
							.createCriteria(AsesemenPenilaian.class)
							.add(Restrictions.eq("pegawai.id", dosen.getPegawaiId()))
							.add(Restrictions.eq("matakuliah", matakuliah))
							.add(Restrictions.eq("tahunAkademik", tahunAkademik))
							.add(Restrictions.eq("jenjang", jenjang)).add(Restrictions.eq("semester", semester))
							.setMaxResults(1).uniqueResult();
					if (asesemenPenilaian == null) {
						asesemenPenilaian = new AsesemenPenilaian();
					}

					asesemenPenilaian.setSpesifikasi(PenilaianAsesor.PENGAJARAN);
					asesemenPenilaian.setJenjang(jenjang);
					asesemenPenilaian.setBidang(PenunjangKinerjaDosen.PENDIDIKAN);
					asesemenPenilaian.setKeterangan(keterangan);
					asesemenPenilaian.setMatakuliah(matakuliah);
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

}
