package ais.action.master;

import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Tabpanel;

import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.MahasiswaRequestTugasAkhir;
import ais.database.model.PertemuanPunyaGrupPertemuan;
import ais.database.model.Skripsi;
import ais.database.model.Tbmuser;
import ais.database.model.kkn.KelompokKkn;
import ais.database.model.pkl.KelompokPkl;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelBoldMerah;
import ais.ui.util.MyWindow;

public class KonsultasiAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	protected static final long serialVersionUID = 3786091220301468178L;
	protected MyWindow addWindow;
	protected MyGrid grid;

	protected Mahasiswa mahasiswa;

	protected MyColumnConfig colSemester;
	protected MyColumnConfig colTahapan;

	protected Integer semesterPendek;
	protected boolean remedial = false;

	protected Combobox semesterMulai;
	protected Combobox semesterSampai;

	protected String tahunAkademik = null;
	protected String jenisSemester = null;
	protected String hr = null;
	protected String keyword = "";
	protected boolean merupakanPraPerkuliahan = false;
	protected Integer ekstrakurikuler = null;
	protected boolean merupakanRemedial = false;

	protected Tabpanel kknTab;

	@SuppressWarnings("unchecked")
	public void onKKN(Event event) {

		if (kknTab.getChildren().size() == 0) {

			Borderlayout borderlayout = new Borderlayout();
			borderlayout.setParent(kknTab);

			borderlayout.setWidth("100%");
			borderlayout.setHeight("100%");

			Center center = new Center();
			center.setBorder("none");
			center.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);

			Object[] objects = mahasiswa.ambilPerkuliahanDanParalel(tahunAkademik, jenisSemester, hr, keyword.trim(),
					"", merupakanPraPerkuliahan, ekstrakurikuler, true, merupakanRemedial, false,
					TampilanELearningAction.KKN, 0, 100, true);
			List<KelompokKkn> kelompokKkns = (List<KelompokKkn>) objects[0];

			if (kelompokKkns.size() == 1) {
				try {
					TampilanELearningAction.prosess(kelompokKkns.get(0), true, center, false);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					ais.common.Common.tampilErrorJikaAdmin(e);
				}
			}

			else if (kelompokKkns.isEmpty()) {
				new MyLabelBoldMerah(Common.getBahasaConfig("Anda belum terdaftar sebagai anggota kelompok KKN"))
						.setParent(center);
			} else {
				// GANTI TAB -> BUTTON GROUP (kelas reusable ais.ui.util.MyButtonTabbox): tab
				// per kelompok KKN ini data-driven, sama seperti pola "Ke-1".."Ke-N" di
				// SetingBiayaAction yang sebelumnya bermasalah blank/scroll pakai
				// Tab/Tabpanel bawaan ZK.
				ais.ui.util.MyButtonTabbox tabboxKkn = ais.ui.util.MyButtonTabbox.buat(center, "100%", null);
				int i = 0;
				for (final KelompokKkn data : kelompokKkns) {
					final int index = i + 1;
					tabboxKkn.tambahTabLazy(index, data.getNama_kelompok(),
							new ais.ui.util.MyButtonTabbox.PemuatTab() {
								@Override
								public void muat(org.zkoss.zul.Div panel) throws Exception {
									TampilanELearningAction.prosess(data, true, panel, false);
								}
							});
					i++;
				}
				tabboxKkn.pilih(1);
			}

		}
	}

	protected Tabpanel pklTab;

	@SuppressWarnings("unchecked")
	public void onPKL(Event event) {

		if (pklTab.getChildren().size() == 0) {

			Borderlayout borderlayout = new Borderlayout();
			borderlayout.setParent(pklTab);

			borderlayout.setWidth("100%");
			borderlayout.setHeight("100%");

			Center center = new Center();
			center.setBorder("none");
			center.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);

			Object[] objects = mahasiswa.ambilPerkuliahanDanParalel(tahunAkademik, jenisSemester, hr, keyword.trim(),
					"", merupakanPraPerkuliahan, ekstrakurikuler, true, merupakanRemedial, false,
					TampilanELearningAction.PKL, 0, 100, true);
			List<KelompokPkl> kelompokPkls = (List<KelompokPkl>) objects[0];

			if (kelompokPkls.size() == 1) {
				try {
					TampilanELearningAction.prosess(kelompokPkls.get(0), true, center, false);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					ais.common.Common.tampilErrorJikaAdmin(e);
				}
			}

			else if (kelompokPkls.isEmpty()) {
				new MyLabelBoldMerah(Common.getBahasaConfig("Anda belum terdaftar sebagai anggota kelompok PKL"))
						.setParent(center);
			} else {
				// GANTI TAB -> BUTTON GROUP (kelas reusable ais.ui.util.MyButtonTabbox): tab
				// per kelompok PKL ini data-driven, sama seperti pola "Ke-1".."Ke-N" di
				// SetingBiayaAction yang sebelumnya bermasalah blank/scroll pakai
				// Tab/Tabpanel bawaan ZK.
				ais.ui.util.MyButtonTabbox tabboxPkl = ais.ui.util.MyButtonTabbox.buat(center, "100%", null);
				int i = 0;
				for (final KelompokPkl data : kelompokPkls) {
					final int index = i + 1;
					tabboxPkl.tambahTabLazy(index, data.getNama_kelompok(),
							new ais.ui.util.MyButtonTabbox.PemuatTab() {
								@Override
								public void muat(org.zkoss.zul.Div panel) throws Exception {
									TampilanELearningAction.prosess(data, true, panel, false);
								}
							});
					i++;
				}
				tabboxPkl.pilih(1);
			}

		}
	}

	protected Tabpanel pembimbingTab;

	@SuppressWarnings("unchecked")
	public void onSkripsi(Event event) {

		if (pembimbingTab.getChildren().size() == 0) {

			Borderlayout borderlayout = new Borderlayout();
			borderlayout.setParent(pembimbingTab);

			borderlayout.setWidth("100%");
			borderlayout.setHeight("100%");

			Center center = new Center();
			center.setBorder("none");
			center.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);

			Object[] objects = mahasiswa.ambilPerkuliahanDanParalel(tahunAkademik, jenisSemester, hr, keyword.trim(),
					"", merupakanPraPerkuliahan, ekstrakurikuler, true, merupakanRemedial, false,
					TampilanELearningAction.BIMBINGAN, 0, 100, true);
			List<MahasiswaRequestTugasAkhir> mahasiswaRequestTugasAkhirs = (List<MahasiswaRequestTugasAkhir>) objects[0];

			if (mahasiswaRequestTugasAkhirs.size() == 1) {
				try {
					TampilanELearningAction.prosess(mahasiswaRequestTugasAkhirs.get(0), true, center, false);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					ais.common.Common.tampilErrorJikaAdmin(e);
				}
			}

			else if (mahasiswaRequestTugasAkhirs.isEmpty()) {
				new MyLabelBoldMerah(
						Common.getBahasaConfig("Anda belum mengajukan bimbingan skripsi/tugas akhir/tesis"))
								.setParent(center);
			} else {
				// GANTI TAB -> BUTTON GROUP (kelas reusable ais.ui.util.MyButtonTabbox): tab
				// per pengajuan skripsi/TA ini data-driven, sama seperti pola "Ke-1".."Ke-N"
				// di SetingBiayaAction yang sebelumnya bermasalah blank/scroll pakai
				// Tab/Tabpanel bawaan ZK.
				ais.ui.util.MyButtonTabbox tabboxSkripsi = ais.ui.util.MyButtonTabbox.buat(center, "100%", null);
				int i = 0;
				for (final MahasiswaRequestTugasAkhir data : mahasiswaRequestTugasAkhirs) {
					final int index = i + 1;
					tabboxSkripsi.tambahTabLazy(index, data.getJudul(),
							new ais.ui.util.MyButtonTabbox.PemuatTab() {
								@Override
								public void muat(org.zkoss.zul.Div panel) throws Exception {
									TampilanELearningAction.prosess(data, true, panel, false);
								}
							});
					i++;
				}
				tabboxSkripsi.pilih(1);
			}

		}
	}

	protected Tabpanel pengujiTab;

	@SuppressWarnings("unchecked")
	public void onPenguji(Event event) {

		if (pengujiTab.getChildren().size() == 0) {

			Borderlayout borderlayout = new Borderlayout();
			borderlayout.setParent(pengujiTab);

			borderlayout.setWidth("100%");
			borderlayout.setHeight("100%");

			Center center = new Center();
			center.setBorder("none");
			center.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);

			Object[] objects = mahasiswa.ambilPerkuliahanDanParalel(tahunAkademik, jenisSemester, hr, keyword.trim(),
					"", merupakanPraPerkuliahan, ekstrakurikuler, true, merupakanRemedial, false,
					TampilanELearningAction.SKRIPSI, 0, 100, true);
			List<Skripsi> skripsis = (List<Skripsi>) objects[0];

			if (skripsis.size() == 1) {
				try {
					TampilanELearningAction.prosess(skripsis.get(0), true, center, false);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					ais.common.Common.tampilErrorJikaAdmin(e);
				}
			}

			else if (skripsis.isEmpty()) {
				new MyLabelBoldMerah(Common.getBahasaConfig("Anda belum mengajukan sidang skripsi/tugas akhir/tesis"))
						.setParent(center);
			} else {
				// GANTI TAB -> BUTTON GROUP (kelas reusable ais.ui.util.MyButtonTabbox): tab
				// per pengajuan sidang ini data-driven, sama seperti pola "Ke-1".."Ke-N" di
				// SetingBiayaAction yang sebelumnya bermasalah blank/scroll pakai
				// Tab/Tabpanel bawaan ZK.
				ais.ui.util.MyButtonTabbox tabboxPenguji = ais.ui.util.MyButtonTabbox.buat(center, "100%", null);
				int i = 0;
				for (final Skripsi data : skripsis) {
					final int index = i + 1;
					tabboxPenguji.tambahTabLazy(index, data.getJudul(),
							new ais.ui.util.MyButtonTabbox.PemuatTab() {
								@Override
								public void muat(org.zkoss.zul.Div panel) throws Exception {
									TampilanELearningAction.prosess(data, true, panel, false);
								}
							});
					i++;
				}
				tabboxPenguji.pilih(1);
			}

		}
	}

	protected Tabpanel lainTab;

	@SuppressWarnings("unchecked")
	public void onKonsultasi(Event event) {

		if (lainTab.getChildren().size() == 0) {

			Borderlayout borderlayout = new Borderlayout();
			borderlayout.setParent(lainTab);

			borderlayout.setWidth("100%");
			borderlayout.setHeight("100%");

			Center center = new Center();
			center.setBorder("none");
			center.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);

			Object[] objects = mahasiswa.ambilPerkuliahanDanParalel(tahunAkademik, jenisSemester, hr, keyword.trim(),
					"", merupakanPraPerkuliahan, ekstrakurikuler, true, merupakanRemedial, false,
					TampilanELearningAction.KONSULTASI, 0, 100, true);
			List<PertemuanPunyaGrupPertemuan> pertemuanPunyaGrupPertemuans = (List<PertemuanPunyaGrupPertemuan>) objects[0];

			if (pertemuanPunyaGrupPertemuans.size() == 1) {
				try {
					TampilanELearningAction.prosess(pertemuanPunyaGrupPertemuans.get(0), true, center, false);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					ais.common.Common.tampilErrorJikaAdmin(e);
				}
			}

			else if (pertemuanPunyaGrupPertemuans.isEmpty()) {
				new MyLabelBoldMerah(Common.getBahasaConfig("Anda belum mengikuti konsultasi")).setParent(center);
			} else {
				// GANTI TAB -> BUTTON GROUP (kelas reusable ais.ui.util.MyButtonTabbox): tab
				// per grup pertemuan ini data-driven, sama seperti pola "Ke-1".."Ke-N" di
				// SetingBiayaAction yang sebelumnya bermasalah blank/scroll pakai
				// Tab/Tabpanel bawaan ZK.
				ais.ui.util.MyButtonTabbox tabboxKonsultasi = ais.ui.util.MyButtonTabbox.buat(center, "100%", null);
				int i = 0;
				for (final PertemuanPunyaGrupPertemuan data : pertemuanPunyaGrupPertemuans) {
					final int index = i + 1;
					tabboxKonsultasi.tambahTabLazy(index, data.getGrupPertemuan().getNama(),
							new ais.ui.util.MyButtonTabbox.PemuatTab() {
								@Override
								public void muat(org.zkoss.zul.Div panel) throws Exception {
									TampilanELearningAction.prosess(data, true, panel, false);
								}
							});
					i++;
				}
				tabboxKonsultasi.pilih(1);
			}

		}
	}

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();
		boolean cek = execution.getParameter("pass") == null;
		if (cek) {
			if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
				session.removeAttribute("usersTemp");
				Common.goLogoff();
				return;
			}
		}
		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser == null || tbmuser.getMahasiswa() == null) {
			alert("Anda harus login sebagai mahasiswa");
			return;
		}

		mahasiswa = tbmuser.getMahasiswa();

		if (ConstantValues.jumlahTahapan.isEmpty()) {
			ConstantValues.initJumlahTahapan();
		}
		if (ConstantValues.aktifkanTahapan
				&& ConstantValues.getJumlahTahapan(mahasiswa.getProgram(), mahasiswa.getJurusan()) > 2) {
			colTahapan.setWidth("10%");
		}

		for (Integer i = 1; i <= (mahasiswa.getSemesterLulus() != null && mahasiswa.getSemesterLulus()>0? mahasiswa.getSemesterLulus() : 40); i++) {
			MyComboitemConfig comboitem = new MyComboitemConfig(i + "");
			comboitem.setValue(i);
			semesterMulai.appendChild(comboitem);
			comboitem = new MyComboitemConfig(i + "");
			comboitem.setValue(i);
			semesterSampai.appendChild(comboitem);
		}

		int defaultPemilihanSemesterMulai = Common
				.getKonfigurasi("default_pemilihan_semester_mulai", mahasiswa.currentSemester() + "").niliaInteger();
		int defaultPemilihanSemesterSampai = Common
				.getKonfigurasi("default_pemilihan_semester_sampai", mahasiswa.currentSemester() + "").niliaInteger();
		
		
		if (mahasiswa.getSemesterLulus() != null && defaultPemilihanSemesterMulai > mahasiswa.getSemesterLulus()) {
			defaultPemilihanSemesterMulai = mahasiswa.getSemesterLulus();
		}
		if (mahasiswa.getSemesterLulus() != null && defaultPemilihanSemesterSampai > mahasiswa.getSemesterLulus()) {
			defaultPemilihanSemesterSampai = mahasiswa.getSemesterLulus();
		}

		if (defaultPemilihanSemesterMulai != defaultPemilihanSemesterSampai) {
			Common.selectComboItem(semesterMulai, defaultPemilihanSemesterMulai);
			Common.selectComboItem(semesterSampai, defaultPemilihanSemesterSampai);
		} else {
			Common.selectComboItem(semesterMulai,
					mahasiswa.getSemesterLulus() != null && mahasiswa.currentSemester()>mahasiswa.getSemesterLulus() ? mahasiswa.getSemesterLulus() : mahasiswa.currentSemester());
			Common.selectComboItem(semesterSampai,
					mahasiswa.getSemesterLulus() != null && mahasiswa.currentSemester()>mahasiswa.getSemesterLulus() ? mahasiswa.getSemesterLulus() : mahasiswa.currentSemester());
		}

		if (semesterMulai != null) { semesterMulai.setReadonly(true); }
		if (semesterSampai != null) { semesterSampai.setReadonly(true); }

		onSearchDefault(null);
	}

	public void onSearchDefaultKeDatabase(Event event) {
		load(true);
	}

	private Center rowData;

	public void onSearchDefault(Event event) {
		load(false);
	}

	private void load(final boolean keDatabase) {

		Integer mulai = (Integer) (semesterMulai.getSelectedItem() == null ? 0
				: semesterMulai.getSelectedItem().getValue());
		if (semesterSampai.getSelectedItem() == null || semesterSampai.getSelectedItem().getValue() == null) {
			Integer smt = mahasiswa.currentSemester();
			System.out.println("smt -> " + smt);
			Common.selectComboItem(true, semesterSampai, smt);
		}
		Integer sampai = (Integer) (semesterSampai.getSelectedItem() == null ? 0
				: semesterSampai.getSelectedItem().getValue());

		Common.clear(rowData);

		if (mulai.equals(sampai)) {
			List<String[]> datas = Common.generateSemestersForGrid(mahasiswa, mulai, sampai, semesterPendek);
			for (String[] data : datas) {
				Integer smt;
				try {
					smt = Integer.parseInt(data[1].split(",")[0]);
				} catch (Exception e) {
					smt = 0;
				}
				Integer semester = smt;
				if (semester > 0) {
					Integer tahap;
					try {
						tahap = Integer.parseInt(data[3]);
					} catch (Exception e) {
						tahap = 0;
					}
					Integer tahapan = tahap;

					KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, semester, tahapan,
							semesterPendek, keDatabase);
					try {
						TampilanELearningAction.prosess(krsMahasiswa, true, rowData, false);
					} catch (Exception e) {
						ais.common.Common.tampilErrorJikaAdmin(e);
					}
				}
			}
		} else {

			// GANTI TAB -> BUTTON GROUP (kelas reusable ais.ui.util.MyButtonTabbox): tab
			// per tahun ajaran/semester ini data-driven, sama seperti pola "Ke-1".."Ke-N"
			// di SetingBiayaAction yang sebelumnya bermasalah blank/scroll pakai
			// Tab/Tabpanel bawaan ZK.
			ais.ui.util.MyButtonTabbox tabboxKrs = ais.ui.util.MyButtonTabbox.buat(rowData, "100%", null);

			List<String[]> datas = Common.generateSemestersForGrid(mahasiswa, mulai, sampai, semesterPendek);
			int i = 0;
			for (String[] data : datas) {
				Integer smt;
				try {
					smt = Integer.parseInt(data[1].split(",")[0]);
				} catch (Exception e) {
					smt = 0;
				}
				final Integer semester = smt;

				Integer tahap;
				try {
					tahap = Integer.parseInt(data[3]);
				} catch (Exception e) {
					tahap = 0;
				}
				final Integer tahapan = tahap;

				final String tahunAjaran = data[0];
				final int index = i + 1;

				tabboxKrs.tambahTabLazy(index, tahunAjaran + "/" + semester,
						new ais.ui.util.MyButtonTabbox.PemuatTab() {
							@Override
							public void muat(org.zkoss.zul.Div panel) throws Exception {
								KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, semester,
										tahapan, semesterPendek, keDatabase);
								TampilanELearningAction.prosess(krsMahasiswa, true, panel, false);
							}
						});
				i++;
			}
			tabboxKrs.pilih(1);
		}
	}

}
