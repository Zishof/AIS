package ais.action.master;

import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Tabpanel;

import ais.action.master.helper.PenilaianMahasiswaHelper;
import ais.action.report.format1.akademik.LaporanKHS;
import ais.action.report.format1.akademik.LaporanRekamanNilai;
import ais.action.report.format1.akademik.LaporanTranskipAkademik;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.MahasiswaJadiAsisten;
import ais.database.model.Tbmuser;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyWindow;

public class NilaiMahasiswaAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	protected static final long serialVersionUID = 3786091220401468178L;
	protected MyWindow addWindow;
	protected MyGrid grid;
	protected Label searchnim;
	protected Label searchjurusan;
	protected Label searchnama;
	protected Label searchfakultas;
	protected Label searchangkatan;
	protected Checkbox searchtampilin;
	protected Mahasiswa mahasiswa;

	protected Tabpanel kartuHasilStudi;
	protected Tabpanel transkripAkademik;
	protected Tabpanel rekamanNilai;
	protected Tabpanel asisten;

	protected Integer semesterPendek = null;
	protected MyColumnConfig colIpk;
	protected MyColumnConfig colKet;
	protected MyColumnConfig colSks;
	protected Tabpanel nilaiSp;

	protected Combobox semesterMulai;
	protected Combobox semesterSampai;

	protected boolean remedial = false;

	public void onNilaiSp(Event event) {
		if (!PenilaianMahasiswaHelper.checkBolehLihatNilai(mahasiswa)) {
			return;
		}
		if (nilaiSp.getChildren().size() == 0) {
			MyInclude include = new MyInclude();
			include.setHeight("100%");
			include.setWidth("100%");
			include.setParent(nilaiSp);
			include.setSrc("/pages/master/nilai_mahasiswa_sp.zul");
		}
	}

	public void onAsisten(Event event) {
		if (asisten.getChildren().size() == 0) {
			MyInclude include = new MyInclude();
			include.setHeight("100%");
			include.setWidth("100%");
			include.setParent(asisten);
			include.setSrc("/pages/master/penilaian.zul");
		}
	}

	public void onTampilKHS(Event event) {

		if (kartuHasilStudi.getChildren().size() == 0) {
			LaporanKHS laporanKHS = new LaporanKHS();
			laporanKHS.setHeight("100%");
			laporanKHS.setWidth("100%");
			laporanKHS.setParent(kartuHasilStudi);
		}
	}

	public void onTampilTranskripAkademik(Event event) {
		if (!PenilaianMahasiswaHelper.checkBolehLihatNilai(mahasiswa)) {
			return;
		}
		if (transkripAkademik.getChildren().size() == 0) {
			LaporanTranskipAkademik laporanTranskipAkademik = new LaporanTranskipAkademik();
			laporanTranskipAkademik.setHeight("100%");
			laporanTranskipAkademik.setWidth("100%");
			laporanTranskipAkademik.setParent(transkripAkademik);
		}
	}

	public void onTampilRekamanNilai(Event event) {
		if (!PenilaianMahasiswaHelper.checkBolehLihatNilai(mahasiswa)) {
			return;
		}
		if (rekamanNilai.getChildren().size() == 0) {
			LaporanRekamanNilai laporanRekamanNilai = new LaporanRekamanNilai();
			laporanRekamanNilai.setHeight("100%");
			laporanRekamanNilai.setWidth("100%");
			laporanRekamanNilai.setParent(rekamanNilai);
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

		if (!PenilaianMahasiswaHelper.checkBolehLihatNilai(mahasiswa)) {
			return;
		}

		if (asisten != null) {
			boolean mahasiswaBolehLihatNilai = ((Number) HibernateUtil.currentSession()
					.createCriteria(MahasiswaJadiAsisten.class)
					.add(Restrictions.eq("mahasiswa", tbmuser.getMahasiswa()))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.setProjection(Projections.rowCount()).uniqueResult()).intValue() > 0;
			asisten.setVisible(mahasiswaBolehLihatNilai);
			asisten.getLinkedTab().setVisible(mahasiswaBolehLihatNilai);
		}

		if (searchnim != null) { searchnim.setValue(mahasiswa.getNim()); }
		if (searchjurusan != null) { searchjurusan.setValue(mahasiswa.getJurusan().getNama()); }
		if (searchnama != null) { searchnama.setValue(mahasiswa.getNama()); }
		if (searchfakultas != null) { searchfakultas.setValue(mahasiswa.getJurusan().getFakultas().getNama()); }
		if (searchangkatan != null) { searchangkatan.setValue(mahasiswa.getTahunangkatan() + " (" + mahasiswa.getSemesterMulai() + ")"); }

		for (Integer i = 1; i <= (mahasiswa.getSemesterLulus() != null && mahasiswa.getSemesterLulus() > 0
				? mahasiswa.getSemesterLulus()
				: 40); i++) {
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
					mahasiswa.getSemesterLulus() != null && mahasiswa.currentSemester() > mahasiswa.getSemesterLulus()
							? mahasiswa.getSemesterLulus()
							: mahasiswa.currentSemester());
			Common.selectComboItem(semesterSampai,
					mahasiswa.getSemesterLulus() != null && mahasiswa.currentSemester() > mahasiswa.getSemesterLulus()
							? mahasiswa.getSemesterLulus()
							: mahasiswa.currentSemester());
		}

		if (semesterMulai != null) { semesterMulai.setReadonly(true); }
		if (semesterSampai != null) { semesterSampai.setReadonly(true); }

		onSearchDefault(null);
	}

	class DataRenderer extends ais.ui.util.MyRowRenderer {

		private boolean keDatabase;

		public DataRenderer(boolean keDatabase) {
			this.keDatabase = keDatabase;
		}

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final String[] data = (String[]) arg1;

			final MyDetail detail = new MyDetail();
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
			detail.setVisible(!semester.equals(1000));
			detail.setParent(arg0);
			EventListener eventListener = new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {
					Common.clear(detail);
					PenilaianMahasiswaHelper penilaianMahasiswaHelper = new PenilaianMahasiswaHelper(semesterPendek,
							remedial);
					penilaianMahasiswaHelper.display(mahasiswa, data[0], semester, tahapan, detail, addWindow,
							keDatabase);
				}
			};
			detail.addEventListener("onOpen", eventListener);

			final Html html = new ais.ui.util.MyHtml("");
			html.setParent(arg0);

			new Label(data[0]).setParent(arg0);
			new Label(tahapan != null && tahapan.equals(-1) ? "" : semester.equals(1000) ? "Lulus" : data[1])
					.setParent(arg0);
			try {
				new Label(data[2]).setParent(arg0);
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

			// String semesterMulai = Common.isNowSemensterGanjil() ?
			// Perkuliahan.GANJIL : Perkuliahan.GENAP;
			// if (Common
			// .getSemester(mahasiswa.getTahunangkatan(), semesterMulai,
			// mahasiswa.getPindahKeKampusIniMasukSemester(),
			// mahasiswa.getSemesterMulai())
			// .equals(semester)) {
			// arg0.setStyle("border:0px;background: #C2FFA3;");
			// detail.setOpen(true);
			// eventListener.onEvent(null);
			// }

			final Label ip = new Label();
			ip.setParent(arg0);
			final Label sks = new Label();
			sks.setParent(arg0);

			if (searchtampilin != null && searchtampilin.isChecked()) {

				if (semester > 0) {
					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event a) throws Exception {

							KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, semester, tahapan,
									semesterPendek, keDatabase);

							Double ipmhs = krsMahasiswa.getIps();
							Double ipkmhs = krsMahasiswa.getIpk();
							ip.setValue(Common.numberFormat.get().format(ipmhs) + " / "
									+ Common.numberFormat.get().format(ipkmhs));

							Integer sksmhss = krsMahasiswa.getSksYangDiambil();
							Integer sksmhs = krsMahasiswa.getSksk();
							sks.setValue(Common.numberFormat.get().format(sksmhss) + " / "
									+ Common.numberFormat.get().format(sksmhs));

							html.setContent(mahasiswa.rubahKeteranganPengambilanKRS(krsMahasiswa.getSemester(),
									krsMahasiswa.getTahapan(), krsMahasiswa.getSemesterPendek(), krsMahasiswa, false));
						}
					});
				}
			}
		}

	}

	public void onSearchDefaultKeDatabase(Event event) {

		if (searchtampilin != null && !searchtampilin.isChecked()) {
			searchtampilin.setChecked(true);
		}
		mahasiswa.reInit();
		colIpk.setVisible(searchtampilin != null && searchtampilin.isChecked());
		colSks.setVisible(searchtampilin != null && searchtampilin.isChecked());
		colKet.setVisible(searchtampilin != null && searchtampilin.isChecked());

		Integer mulai = (Integer) (semesterMulai.getSelectedItem() == null ? 0
				: semesterMulai.getSelectedItem().getValue());
		if (semesterSampai.getSelectedItem() == null || semesterSampai.getSelectedItem().getValue() == null) {
			Integer smt = mahasiswa.currentSemester();
			System.out.println("smt -> " + smt);
			Common.selectComboItem(true, semesterSampai, smt);
		}
		Integer sampai = (Integer) (semesterSampai.getSelectedItem() == null ? 0
				: semesterSampai.getSelectedItem().getValue());
		ListModel strset = new SimpleListModel(
				Common.generateSemestersForGrid(mahasiswa, mulai, sampai, semesterPendek));
		grid.setRowRenderer(new DataRenderer(true));
		grid.setModelCheckMobile(strset);

	}

	public void onSearchDefault(Event event) {

		colIpk.setVisible(searchtampilin != null && searchtampilin.isChecked());
		colSks.setVisible(searchtampilin != null && searchtampilin.isChecked());
		colKet.setVisible(searchtampilin != null && searchtampilin.isChecked());

		Integer mulai = (Integer) (semesterMulai.getSelectedItem() == null ? 0
				: semesterMulai.getSelectedItem().getValue());
		if (semesterSampai.getSelectedItem() == null || semesterSampai.getSelectedItem().getValue() == null) {
			Integer smt = mahasiswa.currentSemester();
			System.out.println("smt -> " + smt);
			Common.selectComboItem(true, semesterSampai, smt);
		}
		Integer sampai = (Integer) (semesterSampai.getSelectedItem() == null ? 0
				: semesterSampai.getSelectedItem().getValue());
		ListModel strset = new SimpleListModel(
				Common.generateSemestersForGrid(mahasiswa, mulai, sampai, semesterPendek));

		grid.setRowRenderer(new DataRenderer(false));
		grid.setModelCheckMobile(strset);
	}

}
