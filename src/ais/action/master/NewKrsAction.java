package ais.action.master;

import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Html;
import ais.ui.util.MyInclude;
import org.zkoss.zul.Row;
import org.zkoss.zul.Tabpanel;

import ais.action.master.helper.KrsHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyWindow;

public class NewKrsAction extends GenericAutowireComposer {

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

	protected Tabpanel krsSp;

	protected Combobox semesterMulai;
	protected Combobox semesterSampai;

	protected MyCheckboxConfig cariSemesterPendek;

	public void onKrsSp(Event event) {

		if (krsSp.getChildren().size() == 0) {
			MyInclude include = new MyInclude();
			include.setHeight("100%");
			include.setWidth("100%");
			include.setParent(krsSp);
			include.setSrc("/pages/master/krs_sp.zul?pass=123");
		}
	}

	protected Tabpanel mkRemedial;

	public void onMkRemedial(Event event) {

		if (mkRemedial.getChildren().size() == 0) {
			MyInclude include = new MyInclude();
			include.setHeight("100%");
			include.setWidth("100%");
			include.setParent(mkRemedial);
			include.setSrc("/pages/master/krs_remedial.zul?pass=123");
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

		Common.selectComboItem(semesterMulai,
				mahasiswa.getSemesterLulus() != null && mahasiswa.currentSemester()>mahasiswa.getSemesterLulus() ? mahasiswa.getSemesterLulus() : mahasiswa.currentSemester());
		Common.selectComboItem(semesterSampai,
				mahasiswa.getSemesterLulus() != null && mahasiswa.currentSemester()>mahasiswa.getSemesterLulus() ? mahasiswa.getSemesterLulus() : mahasiswa.currentSemester());

		if (semesterMulai != null) { semesterMulai.setReadonly(true); }
		if (semesterSampai != null) { semesterSampai.setReadonly(true); }

		if (cariSemesterPendek != null) {
			cariSemesterPendek.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					onSearchDefault(event);
				}
			});
		}

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

		final Integer semesterPendekEfektif = (cariSemesterPendek != null && cariSemesterPendek.isChecked())
				? Perkuliahan.SEMESTER_PENDEK
				: semesterPendek;

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

					Row rowUtama = Common.tampilanScroll1(rowData);

					Integer tahapan = tahap;
					String tahunAjaran = data[0];
					Boolean editable = true;
					Html html = new Html();
					Html komentarshtml = new Html();
					KrsHelper krsHelper = new KrsHelper(semesterPendekEfektif, remedial);
					krsHelper.display(editable, mahasiswa, tahunAjaran, semester, tahapan, rowUtama, html,
							komentarshtml, keDatabase);
				}
			}
		} else {

			// GANTI TAB -> BUTTON GROUP (kelas reusable ais.ui.util.MyButtonTabbox): tab
			// per tahun ajaran/semester ini data-driven (jumlahnya tergantung riwayat KRS
			// mahasiswa), sama seperti pola "Ke-1".."Ke-N" di SetingBiayaAction yang
			// sebelumnya bermasalah blank/scroll pakai Tab/Tabpanel bawaan ZK.
			ais.ui.util.MyButtonTabbox tabboxKrs = ais.ui.util.MyButtonTabbox.buat(rowData, "100%", null);

			final Html html = new Html();
			final Html komentarshtml = new Html();

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
								Boolean editable = true;
								KrsHelper krsHelper = new KrsHelper(semesterPendekEfektif, remedial);
								krsHelper.display(editable, mahasiswa, tahunAjaran, semester, tahapan, panel, html,
										komentarshtml, keDatabase);
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
