package ais.action.master;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Rows;

import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.CommonVO;
import ais.database.model.DetailKelompokKegiatanKedosenan;
import ais.database.model.JabatanFungsionalDosen;
import ais.database.model.JabatanKegiatanKedosenan;
import ais.database.model.JenisPeredaranBuku;
import ais.database.model.Jenjang;
import ais.database.model.KelompokKegiatanKedosenan;
import ais.database.model.SkalaKegiatanKedosenan;
import ais.database.model.TahapanAtauCapaianPembelajaran;
import ais.database.model.TahapanPenyusunanBuku;
import ais.database.model.penelitiandanpengabdian.PenelitianDanPengabdian;
import ais.database.model.penelitiandanpengabdian.TahapanPelaporanPenelitianDanPengabdian;
import ais.database.model.penelitiandanpengabdian.TahapanPenyusunanArtikel;
import ais.database.model.penelitiandanpengabdian.TingkatArtikel;
import ais.ui.util.MyComboitemConfig;

public class KonfigurasiBkdAction extends ParameterUmumAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterComposeOri(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		onTampil();

	}

	public static List<CommonVO> terjemahkanNilai(String konfig, String info1) {
		List<CommonVO> commonVOs = new ArrayList<CommonVO>();

		if (konfig.trim().toLowerCase().startsWith("@=")) {
			try {
				CommonVO commonVO = new CommonVO();
				commonVO.setName(konfig);
				commonVO.setNilai(
						Double.parseDouble(org.apache.commons.lang3.StringUtils.replace(konfig.trim().toLowerCase(), "@=", "").trim()));
				commonVO.setMasing(true);
				if (Common.isNumber(info1.trim())) {
					commonVO.setMaksimal(Double.parseDouble(info1.trim()));
				}
				commonVO.setDibagi(konfig.trim().endsWith("$"));
				commonVOs.add(commonVO);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		} else if (konfig.trim().toLowerCase().contains(";")) {
			for (String s : StringUtils.split(konfig, ";")) {
				try {
					CommonVO commonVO = new CommonVO();
					commonVO.setName(s);
					commonVO.setPersen(StringUtils.split(s.trim().toLowerCase(), "=")[1].trim().endsWith("%"));
					commonVO.setNilai(Double.parseDouble(
							org.apache.commons.lang3.StringUtils.replace(StringUtils.split(s.trim().toLowerCase(), "=")[1].trim(), "%", "")));
					commonVO.setMasing(false);
					commonVO.setMulai(Double.parseDouble(StringUtils.split(s.trim(), "-")[0].trim()));
					commonVO.setSampai(Double
							.parseDouble(StringUtils.split(StringUtils.split(s.trim(), "-")[1].trim(), "=")[0].trim()));
					if (Common.isNumber(info1.trim())) {
						commonVO.setMaksimal(Double.parseDouble(info1.trim()));
					}
					commonVOs.add(commonVO);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		} else if (konfig.trim().toLowerCase().contains("=")) {
			try {
				CommonVO commonVO = new CommonVO();
				commonVO.setName(konfig);
				commonVO.setPersen(StringUtils.split(konfig.trim().toLowerCase(), "=")[1].trim().endsWith("%"));
				commonVO.setNilai(Double.parseDouble(
						org.apache.commons.lang3.StringUtils.replace(StringUtils.split(konfig.trim().toLowerCase(), "=")[1].trim(), "%", "")));
				try {
					commonVO.setMulai(Double.parseDouble(StringUtils.split(konfig.trim(), "-")[0].trim()));
				} catch (Exception e) {
					commonVO.setMulai(Double.parseDouble(StringUtils.split(konfig.trim(), "=")[0].trim()));
				}
				try {
					commonVO.setSampai(Double.parseDouble(
							StringUtils.split(StringUtils.split(konfig.trim(), "-")[1].trim(), "=")[0].trim()));
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/KonfigurasiBkdAction.java:115");

				}
				commonVO.setMasing(false);
				commonVO.setDibagi(konfig.trim().endsWith("$"));
				if (Common.isNumber(info1.trim())) {
					commonVO.setMaksimal(Double.parseDouble(info1.trim()));
				}
				commonVOs.add(commonVO);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		} else if (konfig.trim().toLowerCase().contains("x")) {
			try {

				String[] spl = StringUtils.split(konfig.trim(), "x");
				Double nilai = 1.0;
				for (String s : spl) {
					try {
						Double n = Double.parseDouble(s.trim().replaceAll("\\D+", ""));
						if (s.trim().endsWith("%")) {
							n = n / 100.0;
						}
						nilai *= n;
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/KonfigurasiBkdAction.java:139");

					}
				}

				CommonVO commonVO = new CommonVO();
				commonVO.setName(konfig);
				commonVO.setNilai(nilai);
				commonVO.setMulai(0.0);
				commonVO.setMasing(false);
				commonVO.setDibagi(konfig.trim().endsWith("$"));
				if (Common.isNumber(info1.trim())) {
					commonVO.setMaksimal(Double.parseDouble(info1.trim()));
				}
				commonVOs.add(commonVO);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}
		return commonVOs;
	}

	private void initPengajaran(Rows rows) {
		Combobox combo = new Combobox();
		String[] data = new String[] { "Dinilai sama", "Dibagi rata", "50%" };
		for (String d : data) {
			MyComboitemConfig comboitem = new MyComboitemConfig(d);
			comboitem.setValue(d);
			combo.appendChild(comboitem);
		}
		combo.setReadonly(true);
		combo.setCols(2);

		Combobox comboSertifikasi = new Combobox();
		data = new String[] { "Telah Sertifikasi", "Belum Sertifikasi" };
		for (String d : data) {
			MyComboitemConfig comboitem = new MyComboitemConfig(d);
			comboitem.setValue(d);
			comboSertifikasi.appendChild(comboitem);
		}
		comboSertifikasi.setReadonly(true);
		comboSertifikasi.setCols(4);

		Combobox jabatanFungsionalDosen = new Combobox();
		Common.insertComboDanSemua(jabatanFungsionalDosen, "nama", JabatanFungsionalDosen.class,
				Restrictions.and(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						Restrictions.ne("nama", "")));
		jabatanFungsionalDosen.setReadonly(true);
		if (!jabatanFungsionalDosen.getChildren().isEmpty()) {
			jabatanFungsionalDosen.setSelectedIndex(jabatanFungsionalDosen.getChildren().size() - 1);
		}
		jabatanFungsionalDosen.setCols(4);

		rows.appendChild(createRowActive(
				"Penghitungan BKD untuk pengajaran menggunakan penghitungan per perkuliahan, artinya hanya melihat kelas tanpa melihat jumlah mahasiswa",
				"penghitungan_bkd_pengajaran_menggunakan_per_perkuliahan"));

		rows.appendChild(createRowActiveWithTreeCombo(
				"Pengaturan jumlah SKS beban kerja pengajaran jika dosen lebih dari satu", "pengaturan_juml_sks_beban",
				"", "Telah Sertifikasi", "50%", jabatanFungsionalDosen, comboSertifikasi, combo, null));

		jabatanFungsionalDosen = new Combobox();
		Common.insertComboDanSemua(jabatanFungsionalDosen, "nama", JabatanFungsionalDosen.class,
				Restrictions.and(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						Restrictions.ne("nama", "")));
		jabatanFungsionalDosen.setReadonly(true);
		if (!jabatanFungsionalDosen.getChildren().isEmpty()) {
			jabatanFungsionalDosen.setSelectedIndex(jabatanFungsionalDosen.getChildren().size() - 1);
		}
		jabatanFungsionalDosen.setCols(4);

		Combobox jenjang = new Combobox();
		Common.insertCombo(jenjang, "nama", Jenjang.class,
				Restrictions.and(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						Restrictions.ne("nama", "")));
		jenjang.setReadonly(true);
		Common.selectComboItem(jenjang, ConstantValues.s1);
		jenjang.setCols(2);

		rows.appendChild(createRowNilai("Pengaturan jumlah SKS untuk pengajaran", "jumlah_sks_pengajaran",
				"1-40=100%;41-80=150%;81-120=200%;121-160=250%", 1, jabatanFungsionalDosen, jenjang, null));

	}

	private void initBimbinganTugasAkhir(Rows rows) throws Exception {
		final Combobox jenjangTahapan = new Combobox();
		Common.insertCombo(jenjangTahapan, "nama", Jenjang.class,
				Restrictions.and(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						Restrictions.ne("nama", "")));
		jenjangTahapan.setReadonly(true);
		Common.selectComboItem(jenjangTahapan, ConstantValues.s1);
		jenjangTahapan.setCols(2);

		Combobox comboJenisPembimbing = new Combobox();
		String[] data = new String[] { "Pembimbing Utama", "Pembimbing Pendamping" };
		for (String d : data) {
			MyComboitemConfig comboitem = new MyComboitemConfig(d);
			comboitem.setValue(d);
			comboJenisPembimbing.appendChild(comboitem);
		}
		comboJenisPembimbing.setReadonly(true);
		comboJenisPembimbing.setSelectedIndex(0);
		comboJenisPembimbing.setCols(4);

		final Combobox jabatanTahapanAtauCapaianPembelajaran = new Combobox();
		jabatanTahapanAtauCapaianPembelajaran.setCols(4);

		EventListener eventListenerJenjang = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.insertCombo(jabatanTahapanAtauCapaianPembelajaran, "nama", TahapanAtauCapaianPembelajaran.class,
						Restrictions.and(Restrictions.eq("jenjang", jenjangTahapan.getSelectedItem().getValue()),
								Restrictions.and(
										Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
										Restrictions.eq("jenis", TahapanAtauCapaianPembelajaran.TATAPAN_BIMBINGAN))));
				jabatanTahapanAtauCapaianPembelajaran.setReadonly(true);
				if (!jabatanTahapanAtauCapaianPembelajaran.getChildren().isEmpty()) {
					jabatanTahapanAtauCapaianPembelajaran.setSelectedIndex(0);
				}
			}
		};
		jenjangTahapan.addEventListener("onChange", eventListenerJenjang);
		eventListenerJenjang.onEvent(null);

		rows.appendChild(
				createRowNilaiDariVo("Pengaturan jumlah SKS untuk bimbingan Tugas Akhir/Skripsi/Thesis/Disertasi",
						"jumlah_sks_bimbingan_tugas_akhir", "0.0", "6", null, 1, jenjangTahapan, comboJenisPembimbing,
						jabatanTahapanAtauCapaianPembelajaran, null, null));
	}

	private void initUjianTugasAkhir(Rows rows) throws Exception {
		final Combobox jenjangTahapan = new Combobox();
		Common.insertCombo(jenjangTahapan, "nama", Jenjang.class,
				Restrictions.and(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						Restrictions.ne("nama", "")));
		jenjangTahapan.setReadonly(true);
		Common.selectComboItem(jenjangTahapan, ConstantValues.s1);
		jenjangTahapan.setCols(2);

		rows.appendChild(
				createRowNilaiDariVo("Pengaturan jumlah SKS untuk penguji Tugas Akhir/Skripsi/Thesis/Disertasi",
						"jumlah_sks_ujian_tugas_akhir", "0.0", "8", null, 1, jenjangTahapan, null, null, null, null));
	}

	private void initUjianPrposalTugasAkhir(Rows rows) throws Exception {
		final Combobox jenjangTahapan = new Combobox();
		Common.insertCombo(jenjangTahapan, "nama", Jenjang.class,
				Restrictions.and(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						Restrictions.ne("nama", "")));
		jenjangTahapan.setReadonly(true);
		Common.selectComboItem(jenjangTahapan, ConstantValues.s1);
		jenjangTahapan.setCols(2);

		rows.appendChild(createRowNilaiDariVo(
				"Pengaturan jumlah SKS untuk penguji proposal Tugas Akhir/Skripsi/Thesis/Disertasi",
				"jumlah_sks_proposal_ujian_tugas_akhir", "0.0", "8", null, 1, jenjangTahapan, null, null, null, null));
	}

	private void initPembimbingAkademikTugasAkhir(Rows rows) throws Exception {
		final Combobox jenjangTahapan = new Combobox();
		Common.insertCombo(jenjangTahapan, "nama", Jenjang.class,
				Restrictions.and(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						Restrictions.ne("nama", "")));
		jenjangTahapan.setReadonly(true);
		Common.selectComboItem(jenjangTahapan, ConstantValues.s1);
		jenjangTahapan.setCols(2);

		rows.appendChild(createRowNilaiDariVo("Pengaturan jumlah SKS untuk pembimbing akademik atau dosen PA",
				"jumlah_sks_pembimbing_akademik_mahasiswa", "0.0", "8", null, 1, jenjangTahapan, null, null, null,
				null));
	}

	private void initKkn(Rows rows) {

		Combobox jenjang = new Combobox();
		Common.insertCombo(jenjang, "nama", Jenjang.class,
				Restrictions.and(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						Restrictions.ne("nama", "")));
		jenjang.setReadonly(true);
		Common.selectComboItem(jenjang, ConstantValues.s1);
		jenjang.setCols(2);

		rows.appendChild(createRowNilai("Pengaturan jumlah SKS untuk bimbingan KKN", "jumlah_sks_pembimbing_kkn",
				"1-25=1;26-50=2;51-75=3;76-100=4", 1, jenjang, null, null));

	}

	private void initPkl(Rows rows) {

		Combobox jenjang = new Combobox();
		Common.insertCombo(jenjang, "nama", Jenjang.class,
				Restrictions.and(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						Restrictions.ne("nama", "")));
		jenjang.setReadonly(true);
		Common.selectComboItem(jenjang, ConstantValues.s1);
		jenjang.setCols(2);

		rows.appendChild(createRowNilai("Pengaturan jumlah SKS untuk bimbingan PKL", "jumlah_sks_pembimbing_pkl",
				"1-25=1;26-50=2;51-75=3;76-100=4", 1, jenjang, null, null));

	}

	private void initBuku(Rows rows) {

		Combobox combo = new Combobox();
		String[] data = new String[] { "Dinilai sama", "Dibagi rata", "50%",
				"Ketua = 60% dan 1 Anggota = 40%, jika Anggota > 1, maka Ketua = 40% dan Anggota = 60%" };
		for (String d : data) {
			MyComboitemConfig comboitem = new MyComboitemConfig(d);
			comboitem.setValue(d);
			combo.appendChild(comboitem);
		}
		combo.setReadonly(true);
		rows.appendChild(createRowActiveWithDefault(
				"Pengaturan jumlah SKS beban kerja penulisan buku jika dosen lebih dari satu",
				"pengaturan_pembagian_beban_sks_buku", "", "Dinilai sama", combo));

		Combobox tahapan = new Combobox();
		Common.insertComboDanSemua(tahapan, new String[] { "nama", "prosentase" }, "keterangan",
				TahapanPenyusunanBuku.class,
				Restrictions.and(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						Restrictions.ne("nama", "")));
		tahapan.setReadonly(true);
		if (!tahapan.getChildren().isEmpty()) {
			tahapan.setSelectedIndex(tahapan.getChildren().size() - 1);
		}
		tahapan.setCols(4);

		Combobox jenisPeredaranBuku = new Combobox();
		Common.insertCombo(jenisPeredaranBuku, "nama", JenisPeredaranBuku.class,
				Restrictions.and(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						Restrictions.ne("nama", "")));
		jenisPeredaranBuku.setReadonly(true);
		if (!jenisPeredaranBuku.getChildren().isEmpty()) {
			jenisPeredaranBuku.setSelectedIndex(jenisPeredaranBuku.getChildren().size() - 1);
		}
		jenisPeredaranBuku.setCols(2);

		rows.appendChild(createRowNilai("Pengaturan jumlah SKS untuk penulisan buku", "pengaturan_beban_sks_buku",
				"0.0", 1, tahapan, jenisPeredaranBuku, null));

	}

	private void initArtikel(Rows rows) {

		Combobox combo = new Combobox();
		String[] data = new String[] { "Dinilai sama", "Dibagi rata", "50%",
				"Ketua = 60% dan 1 Anggota = 40%, jika Anggota > 1, maka Ketua = 40% dan Anggota = 60%" };
		for (String d : data) {
			MyComboitemConfig comboitem = new MyComboitemConfig(d);
			comboitem.setValue(d);
			combo.appendChild(comboitem);
		}
		combo.setReadonly(true);
		rows.appendChild(createRowActiveWithDefault(
				"Pengaturan jumlah SKS beban kerja penulisan artikel jika dosen lebih dari satu",
				"pengaturan_pembagian_beban_sks_artikel", "", "Dinilai sama", combo));

		Combobox tahapan = new Combobox();
		Common.insertComboDanSemua(tahapan, new String[] { "nama", "prosentase" }, "keterangan",
				TahapanPenyusunanArtikel.class,
				Restrictions.and(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						Restrictions.ne("nama", "")));
		tahapan.setReadonly(true);
		if (!tahapan.getChildren().isEmpty()) {
			tahapan.setSelectedIndex(tahapan.getChildren().size() - 1);
		}
		tahapan.setCols(4);

		Combobox jenisPeredaranArtikel = new Combobox();
		Common.insertCombo(jenisPeredaranArtikel, "nama", TingkatArtikel.class,
				Restrictions.and(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						Restrictions.ne("nama", "")));
		jenisPeredaranArtikel.setReadonly(true);
		if (!jenisPeredaranArtikel.getChildren().isEmpty()) {
			jenisPeredaranArtikel.setSelectedIndex(jenisPeredaranArtikel.getChildren().size() - 1);
		}
		jenisPeredaranArtikel.setCols(2);

		rows.appendChild(createRowNilai("Pengaturan jumlah SKS untuk penulisan artikel", "pengaturan_beban_sks_artikel",
				"0.0", 1, tahapan, jenisPeredaranArtikel, null));

	}

	private void initPenelitianDanPengabdian(Rows rows) throws Exception {

		Combobox combo = new Combobox();
		String[] data = new String[] { "Dinilai sama", "Dibagi rata", "50%",
				"Ketua = 60% dan 1 Anggota = 40%, jika Anggota > 1, maka Ketua = 40% dan Anggota = 60%" };
		for (String d : data) {
			MyComboitemConfig comboitem = new MyComboitemConfig(d);
			comboitem.setValue(d);
			combo.appendChild(comboitem);
		}
		combo.setReadonly(true);
		rows.appendChild(createRowActiveWithDefault(
				"Pengaturan jumlah SKS beban kerja penelitian dan pengabdian jika dosen lebih dari satu",
				"pengaturan_pembagian_beban_sks_penelitian_dan_pengabdian", "", "Dinilai sama", combo));

		final Combobox jenisPeredaranArtikel = new Combobox();
		Common.insertCombo(jenisPeredaranArtikel, "judul", PenelitianDanPengabdian.class,
				Restrictions.and(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						Restrictions.ne("judul", "")));
		jenisPeredaranArtikel.setReadonly(true);
		if (!jenisPeredaranArtikel.getChildren().isEmpty()) {
			jenisPeredaranArtikel.setSelectedIndex(jenisPeredaranArtikel.getChildren().size() - 1);
		}
		jenisPeredaranArtikel.setCols(2);

		final Combobox tahapan = new Combobox();
		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.insertComboDanSemua(tahapan, new String[] { "nama", "tahapPengajuan" }, "keterangan",
						TahapanPelaporanPenelitianDanPengabdian.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
								jenisPeredaranArtikel.getSelectedItem() == null ? Restrictions.sqlRestriction("false")
										: Restrictions.eq("penelitianDanPengabdian",
												jenisPeredaranArtikel.getSelectedItem().getValue())));
				tahapan.setReadonly(true);
				if (!tahapan.getChildren().isEmpty()) {
					tahapan.setSelectedIndex(tahapan.getChildren().size() - 1);
				}
				tahapan.setCols(4);
			}
		};

		rows.appendChild(createRowNilai("Pengaturan jumlah SKS untuk penulisan artikel",
				"pengaturan_beban_sks_penelitian_dan_pengabdian", "0.0", 1, jenisPeredaranArtikel, tahapan, null));

		eventListener.onEvent(null);
		jenisPeredaranArtikel.addEventListener("onChange", eventListener);

	}

	private void initKegiatanDosen(Rows rows) throws Exception {

		final Combobox kelompokKegiatanKedosenan = new Combobox();
		Common.insertCombo(kelompokKegiatanKedosenan, new String[] { "nama", "jenis" }, "keterangan",
				KelompokKegiatanKedosenan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		if (!kelompokKegiatanKedosenan.getChildren().isEmpty()) {
			kelompokKegiatanKedosenan.setSelectedIndex(0);
		}
		kelompokKegiatanKedosenan.setCols(4);
		kelompokKegiatanKedosenan.setReadonly(true);

		final Combobox detailKelompokKegiatanKedosenan = new Combobox();
		detailKelompokKegiatanKedosenan.setReadonly(true);
		detailKelompokKegiatanKedosenan.setCols(4);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(detailKelompokKegiatanKedosenan);
				if (kelompokKegiatanKedosenan.getSelectedItem() != null
						&& kelompokKegiatanKedosenan.getSelectedItem().getValue() != null) {
					Common.insertCombo(detailKelompokKegiatanKedosenan, "nama", DetailKelompokKegiatanKedosenan.class,

							Restrictions.eq("kelompokKegiatanKedosenan",
									kelompokKegiatanKedosenan.getSelectedItem().getValue()),
							Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
					if (!detailKelompokKegiatanKedosenan.getChildren().isEmpty()) {
						detailKelompokKegiatanKedosenan.setSelectedIndex(0);
					}

				}

			}
		};

		kelompokKegiatanKedosenan.addEventListener("onChange", eventListener);
		eventListener.onEvent(null);

		final Combobox jabatanKegiatanKedosenan = new Combobox();
		final Combobox skalaKegiatanKedosenan = new Combobox();

		EventListener detail = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				DetailKelompokKegiatanKedosenan kedosenan = (DetailKelompokKegiatanKedosenan) (detailKelompokKegiatanKedosenan
						.getSelectedItem() == null ? null
								: detailKelompokKegiatanKedosenan.getSelectedItem().getValue());
				if (kedosenan != null) {
					HibernateUtil.currentSession().refresh(kedosenan);
					List<JabatanKegiatanKedosenan> jabatanKegiatanKedosenans = new ArrayList<JabatanKegiatanKedosenan>(
							kedosenan.getJabatanKegiatanKedosenans());
					List<SkalaKegiatanKedosenan> skalaKegiatanKedosenans = new ArrayList<SkalaKegiatanKedosenan>(
							kedosenan.getSkalaKegiatanKedosenans());

					Common.insertComboItems(jabatanKegiatanKedosenan, "nama", jabatanKegiatanKedosenans);
					Common.insertComboItems(skalaKegiatanKedosenan, "nama", skalaKegiatanKedosenans);

					if (!jabatanKegiatanKedosenan.getChildren().isEmpty()) {
						jabatanKegiatanKedosenan.setSelectedIndex(0);
					}

					if (!skalaKegiatanKedosenan.getChildren().isEmpty()) {
						skalaKegiatanKedosenan.setSelectedIndex(0);
					}
				}

			}
		};

		detailKelompokKegiatanKedosenan.addEventListener("onChange", detail);
		detail.onEvent(null);

		jabatanKegiatanKedosenan.setCols(3);
		skalaKegiatanKedosenan.setCols(3);

		jabatanKegiatanKedosenan.setReadonly(true);
		skalaKegiatanKedosenan.setReadonly(true);

		rows.appendChild(createRowNilai("Pengaturan jumlah SKS untuk kegiatan dosen",
				"pengaturan_beban_sks_kegiatan_dosen", "0.0", 1, kelompokKegiatanKedosenan,
				detailKelompokKegiatanKedosenan, jabatanKegiatanKedosenan, skalaKegiatanKedosenan, null));

	}

	public void onTampil() throws Exception {

		Rows rows = (createSpan("Pengaturan Beban Kinerja Dosen (BKD)"));

		

		initPengajaran(rows);

		initBimbinganTugasAkhir(rows);

		initUjianTugasAkhir(rows);

		initUjianPrposalTugasAkhir(rows);

		initPembimbingAkademikTugasAkhir(rows);

		initKkn(rows);

		initPkl(rows);

		initBuku(rows);

		initArtikel(rows);

		initKegiatanDosen(rows);

		initPenelitianDanPengabdian(rows);

	}
}
