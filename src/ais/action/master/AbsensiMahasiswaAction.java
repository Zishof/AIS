package ais.action.master;

import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyInclude;
import org.zkoss.zul.Row;
import org.zkoss.zul.Tabpanel;

import ais.action.master.dashboard.admin.DashboardRekapAbsensiMahasiswa;
import ais.action.master.dashboard.admin.DashboardRekapAbsensiPerMahasiswa;
import ais.action.master.helper.AbsensiMahasiswaHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyWindow;

public class AbsensiMahasiswaAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	protected static final long serialVersionUID = 3786091220401469178L;
	protected MyWindow addWindow;
	protected MyGrid grid;

	protected Mahasiswa mahasiswa;
	protected MyColumnConfig colIpk;
	protected MyColumnConfig colSks;
	protected Integer semesterPendek;

	protected Tabpanel absensiSp;
	protected Tabpanel asisten;

	protected MyColumnConfig colSemester;
	protected MyColumnConfig colTahapan;

	public void onAsisten(Event event) {

		if (asisten.getChildren().size() == 0) {
			MyInclude include = new MyInclude();
			include.setHeight("100%");
			include.setWidth("100%");
			include.setParent(asisten);
			include.setSrc("/pages/master/absensi.zul");
		}
	}

	protected Combobox semesterMulai;
	protected Combobox semesterSampai;
	protected ais.ui.util.MyCheckboxConfig cariSemesterPendek;

	public void onAbsensiSp(Event event) {

		if (absensiSp.getChildren().size() == 0) {
			MyInclude include = new MyInclude();
			include.setHeight("100%");
			include.setWidth("100%");
			include.setParent(absensiSp);
			include.setSrc("/pages/master/absensi_mahasiswa_sp.zul");
		}
	}

	protected Tabpanel rekapitulasiAbsensiMahasiswa;

	public void onRekapitulasiAbsensiMahasiswa(Event event) {

		if (rekapitulasiAbsensiMahasiswa.getChildren().size() == 0) {
			DashboardRekapAbsensiMahasiswa laporan = new DashboardRekapAbsensiMahasiswa(mahasiswa);
			ais.ui.util.BaseDasbordPortal.mountWrapped(laporan, rekapitulasiAbsensiMahasiswa,
				"Rekap Absensi", "Ringkasan kehadiran mahasiswa ini di seluruh mata kuliah.");
		}
	}

	protected Tabpanel rekapitulasiAbsensiPerMahasiswa;

	public void onRekapitulasiAbsensiPerMahasiswa(Event event) {

		if (rekapitulasiAbsensiPerMahasiswa.getChildren().size() == 0) {
			DashboardRekapAbsensiPerMahasiswa laporan = new DashboardRekapAbsensiPerMahasiswa(mahasiswa);
			ais.ui.util.BaseDasbordPortal.mountWrapped(laporan, rekapitulasiAbsensiPerMahasiswa,
				"Absensi per Mahasiswa", "Detail kehadiran mahasiswa ini di tiap mata kuliah.");
		}
	}

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
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

		for (int i = 1; i <= 20; i++) {
			MyComboitemConfig comboitem = new MyComboitemConfig(i + "");
			comboitem.setValue(i);
			semesterMulai.appendChild(comboitem);
			comboitem = new MyComboitemConfig(i + "");
			comboitem.setValue(i);
			semesterSampai.appendChild(comboitem);
		}

		Common.selectComboItem(semesterMulai, mahasiswa.currentSemester());
		Common.selectComboItem(semesterSampai, mahasiswa.currentSemester());

		if (semesterMulai != null) { semesterMulai.setReadonly(true); }
		if (semesterSampai != null) { semesterSampai.setReadonly(true); }

		if (cariSemesterPendek != null) {
			cariSemesterPendek.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					onSearchDefault(null);
				}
			});
		}

		onSearchDefault(null);

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

		final Integer semesterPendekEfektif = (cariSemesterPendek != null && cariSemesterPendek.isChecked())
				? Perkuliahan.SEMESTER_PENDEK : semesterPendek;

		Common.clear(rowData);

		if (mulai.equals(sampai)) {
			List<String[]> datas = Common.generateSemestersForGrid(mahasiswa, mulai, sampai, semesterPendekEfektif);
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
					String tahunAjaran = data[0];

					Row rowUtama = Common.tampilanScroll1(rowData);
					
					AbsensiMahasiswaHelper absensiMahasiswaHelper = new AbsensiMahasiswaHelper(semesterPendekEfektif);
					absensiMahasiswaHelper.display(mahasiswa, tahunAjaran, semester, tahapan, rowUtama, semesterPendekEfektif,
							addWindow, keDatabase);
				}
			}
		} else {

			// GANTI TAB -> BUTTON GROUP (kelas reusable ais.ui.util.MyButtonTabbox): tab
			// per tahun ajaran/semester ini data-driven, sama seperti pola "Ke-1".."Ke-N"
			// di SetingBiayaAction yang sebelumnya bermasalah blank/scroll pakai
			// Tab/Tabpanel bawaan ZK.
			ais.ui.util.MyButtonTabbox tabboxKrs = ais.ui.util.MyButtonTabbox.buat(rowData, "100%", null);

			List<String[]> datas = Common.generateSemestersForGrid(mahasiswa, mulai, sampai, semesterPendekEfektif);
			int i = 0;
			boolean ada = false;
			for (String[] data : datas) {
				Integer smt;
				try {
					smt = Integer.parseInt(data[1].split(",")[0]);
				} catch (Exception e) {
					smt = 0;
				}
				if (mahasiswa.currentSemester() == smt) {
					ada = true;
				}
			}

			int indexTerpilih = 1;
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
								AbsensiMahasiswaHelper absensiMahasiswaHelper = new AbsensiMahasiswaHelper(
										semesterPendekEfektif);
								absensiMahasiswaHelper.display(mahasiswa, tahunAjaran, semester, tahapan, panel,
										semesterPendekEfektif, addWindow, keDatabase);
							}
						});
				if (ada) {
					if (mahasiswa.currentSemester() == semester) {
						indexTerpilih = index;
					}
				} else if (i == 0) {
					indexTerpilih = index;
				}
				i++;
			}
			tabboxKrs.pilih(indexTerpilih);
		}
	}

}
