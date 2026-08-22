package ais.action.master.surat;


import ais.common.CommonSearchFilterHelper;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.pdfbox.util.PDFMergerUtility;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Caption;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Doublebox;
import org.zkoss.zul.East;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Iframe;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.akunting.helper.AmbilDataPegawaiBanbox;
import ais.action.master.dashboard.surat.DasboardSuratKeluar;
import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.action.master.helper.BroadcastHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.generic.AmbilDataMahasiswaBanyak;
import ais.action.master.helper.generic.AmbilDataSiswaBanyak;
import ais.action.master.helper.generic.AmbilDataTbmuserBanyak;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.master.sekolah.helper.AmbilDataGuruBanbox;
import ais.action.master.sekolah.helper.AmbilDataSiswaBanbox;
import ais.action.master.sop.TampilanAlurSopAction;
import ais.action.master.sop.helper.SopUtil;
import ais.action.master.surat.helper.AmbilDataAlurPersetujuanSuratKeluarBanbox;
import ais.action.master.surat.helper.AmbilDataKlasifikasiSuratKeluarBanbox;
import ais.action.master.surat.helper.AmbilDataSuratKeluarBanbox;
import ais.action.master.surat.helper.SuratKeluarPunyaGambarFotoHelper;
import ais.action.master.surat.helper.SuratKeluarPunyaSuratMasukHelper;
import ais.action.master.surat.util.SuratUtil;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.UIClassHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.CicilanPembayaran;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.HistoryStatusMahasiswa;
import ais.database.model.ItemBiaya;
import ais.database.model.Jurusan;
import ais.database.model.Kegiatan;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.Pegawai;
import ais.database.model.PendaftaranCutiMahasiswa;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.employ.JenisJabatan;
import ais.database.model.file.FotoGambarSuratKeluar;
import ais.database.model.file.LampiranLain;
import ais.database.model.rab.Pejabat;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Yayasan;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;
import ais.database.model.surat.AlurPersetujuanSuratKeluar;
import ais.database.model.surat.AlurPersetujuanSuratKeluarStatus;
import ais.database.model.surat.KelompokNomorSurat;
import ais.database.model.surat.KlasifikasiSuratKeluar;
import ais.database.model.surat.KlasifikasiSuratKeluarParemeter;
import ais.database.model.surat.KlasifikasiSuratKeluarParemeterValue;
import ais.database.model.surat.NomorSurat;
import ais.database.model.surat.OpsiSuratKeluar;
import ais.database.model.surat.OpsiSuratKeluarValue;
import ais.database.model.surat.SuratKeluar;
import ais.database.model.surat.SuratMasuk;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.FormSop;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyCkEditor;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyGroupboxStyled;
import ais.ui.util.MyHtml;
import ais.ui.util.MyLabelAgakKecilBoldBiru;
import ais.ui.util.MyLabelAgakKecilBoldMerah;
import ais.ui.util.MyLabelBoldConfig;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyLabelStyled;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;
import ais.action.master.helper.FilterLanjutHelper;

public class SuratKeluarAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault, FormSop {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

//	private West west = null;

	private Textbox searchnama;
	private Textbox searchkode;
	private Textbox searchperihal;
	private Textbox searchket;
	private MyDatebox searchmulai;
	private MyDatebox searchsampai;

	protected Combobox searchfakultas;
	protected Combobox searchjurusan;
	private Combobox searchyayasan;
	private Combobox searchsekolah;
	protected Combobox searchkelompokNomorSurat;
	private AmbilDataKlasifikasiSuratKeluarBanbox searchklasifikasiSuratKeluar;

	private AmbilDataSatuanKerjaBanbox searchparent;
	// private AmbilDataSatuanKerjaBanbox searchparent;

	// private AmbilDataSatuanKerjaBanbox satuanKerja;
	private AmbilDataKlasifikasiSuratKeluarBanbox klasifikasiSuratKeluar;
	private Textbox kode;
	// Wadah pratinjau surat (berisi bar toggle HTML/PDF + area iframe yang dibangun Report.tampil).
	private org.zkoss.zul.Div template;
	private AmbilDataMahasiswaBanbox mahasiswa;
	private AmbilDataDosenBanbox dosen;

	private AmbilDataSiswaBanbox siswa;
	private AmbilDataGuruBanbox guru;
	private AmbilDataPegawaiBanbox pegawai;
	private AmbilDataAlurPersetujuanSuratKeluarBanbox alurPersetujuanSuratKeluar;
	private Combobox fakultas;
	private Combobox jurusan;

	private boolean edit = false;
	private boolean delete = false;

	private SuratKeluar suratKeluar;
	private MyToolbarbuttonConfig add;
	private Row rowmhs;
	private Row rowdsn;
	private Row rowpeg;
	private Rows rows;
	// protected String hasil;

	// private SatuanKerjaTreeModel satuanKerjaTreeModel;

	protected Map<String, Object> parameters;
	private Tbmuser tbmuser;

	private Boolean ubahLangsungA = false;
	private SatuanKerjaTreeModel satuanKerjaTreeModel;
	private boolean pt = false;
	private boolean ya = false;
	private Row hbFakultasLabel;
	private Row hbYayasan;
	private Combobox yayasan;
	private Combobox sekolah;
	private AmbilDataSatuanKerjaBanbox satuanKerja;
	private EventListener eventListener = null;

	protected Tabpanel statistik;

	public SuratKeluarAction() {
		super();
	}

	public SuratKeluarAction(String tipe) {
		super();
		this.tipe = tipe;
	}

	public void onStatistik(Event event) {

		if (statistik.getChildren().size() == 0) {
			DasboardSuratKeluar include = new DasboardSuratKeluar();
			include.setHeight("100%");
			include.setWidth("100%");
			include.setParent(statistik);
		}
	}

	public static String[] contents = new String[] { "id", "kode", "agenda", "nama", "klasifikasiSuratKeluar",
			"tanggal", "lampiran", "perihal", "kepada", "konseptor", "ttd", "alurPersetujuanSuratKeluar", "fakultas",
			"jurusan", "yayasan", "sekolah", "satuanKerja", "mahasiswa", "dosen", "siswa", "guru", "pegawai",
			"keterangan" };

	private String tipe = "surat";

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
		tbmuser = Common.getCurrentUser();
		if (execution.getParameter("ubahLangsung") != null) {
			ubahLangsungA = true;
		}
		if (!ubahLangsungA) {
			if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
				session.removeAttribute("usersTemp");
				Common.goLogoff();
				return;
			}
		}

		if (execution.getParameter("tipe") != null && !execution.getParameter("tipe").trim().isEmpty()) {
			tipe = execution.getParameter("tipe").trim();
		}

		if (searchklasifikasiSuratKeluar != null) { searchklasifikasiSuratKeluar.setTipe(tipe); }

		tbmuser = Common.getCurrentUser();

		if (searchklasifikasiSuratKeluar != null) {
			searchklasifikasiSuratKeluar.setEventListener(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(null);
				}
			});

			if (execution.getParameter("klasifikasiSuratKeluar") != null) {
				KlasifikasiSuratKeluar klasifikasiSuratKeluar = (KlasifikasiSuratKeluar) ConstantValues.ambil(
						KlasifikasiSuratKeluar.class.getName(),
						Long.parseLong(execution.getParameter("klasifikasiSuratKeluar")));
				if (klasifikasiSuratKeluar != null) {
					searchklasifikasiSuratKeluar.setAttribute("klasifikasiSuratKeluar", klasifikasiSuratKeluar);
					searchklasifikasiSuratKeluar
							.setValue(klasifikasiSuratKeluar.getKode() + (klasifikasiSuratKeluar.getNama() == null
									|| klasifikasiSuratKeluar.getNama().trim().isEmpty() ? ""
											: "-" + klasifikasiSuratKeluar.getNama()));
					searchklasifikasiSuratKeluar.setDisabled(true);
				}
			}
		}

		searchparent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		boolean[] ptYa = Common.chekPtAtauSekolah();
		pt = ptYa[0];
		ya = ptYa[1];

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah, true, false);

		if (hbFakultasLabel != null) { hbFakultasLabel.setVisible(pt && searchfakultas.getChildren().size() > 1); }
		if (hbYayasan != null) { hbYayasan.setVisible(ya); }

		@SuppressWarnings("unused")
		OpsiSuratKeluar balasan = SuratUtil.balasan;

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		Common.selectComboItem(searchfakultas, null);
		Common.selectComboItem(searchjurusan, null);

		if (searchfakultas != null) { searchfakultas.setDisabled(false); }
		if (searchjurusan != null) { searchjurusan.setDisabled(false); }

		Common.insertComboDanSemua(searchkelompokNomorSurat, "nama", KelompokNomorSurat.class);
		if (searchkelompokNomorSurat != null) { searchkelompokNomorSurat.setReadonly(true); }

		tbmuser = Common.getCurrentUser();
		KelompokNomorSuratAction.checkKelompok(searchkelompokNomorSurat);

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, SuratKeluar.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

		MyToolbarbuttonConfig sinkronNomorButton = new MyToolbarbuttonConfig("Sinkronkan Nomor Surat",
				"/img/svg/list.svg");
		if (sinkronNomorButton != null) {
			sinkronNomorButton.setVisible(Common.getApakahAdmin());
		}
		sinkronNomorButton.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				ais.action.master.surat.SinkronNomorSuratHelper.buka(new EventListener() {
					@Override
					public void onEvent(Event ev) throws Exception {
						onSearchDefault(null);
					}
				});
			}
		});
		Common.appendKeToolbar(sinkronNomorButton, add, comp);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	        FilterLanjutHelper.setup(comp);
}

	class SuratKeluarRenderer extends ais.ui.util.MyRowRenderer {

		@SuppressWarnings("unchecked")
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			try {
				arg0.setValign("top");
				// TODO Auto-generated method stub
				final SuratKeluar suratKeluar = (SuratKeluar) arg1;

				// "Alur Disposisi & Tindak Lanjut" dipindah dari kolom sempit "Status Persetujuan"
				// ke baris DETAIL full-width di bawah row (open=true), agar kartu alur mengalir
				// horizontal dan tidak menumpuk. Detail = anak PERTAMA row (kolom pemandu di ZUL).
				org.zkoss.zul.Detail detailAlur = new org.zkoss.zul.Detail();
				detailAlur.setOpen(true);
				detailAlur.setParent(arg0);

				if (suratKeluar.getTipe() == null) {
					suratKeluar.setTipe(tipe);
					Common.refreshUpdate(suratKeluar);
				}

				Component parent = arg0;
				if (ubahLangsungA) {
					parent = new Vbox();
					parent.setParent(arg0);
				}

				Vbox a;
				(a = RevisiHelper.createNewRevisi(SuratKeluar.class, suratKeluar, suratKeluar.getKode()))
						.setParent(parent);

				new Label(suratKeluar.getTanggal() == null ? "" : Common.dateFormat6.get().format(suratKeluar.getTanggal()))
						.setParent(a);

				if (suratKeluar.getSuratSebelumnya() != null) {
					new MyLabelAgakKecilBoldBiru(suratKeluar.getSuratSebelumnya().getKode()).setParent(a);
					new MyLabelAgakKecilBoldBiru(suratKeluar.getSuratSebelumnya().getNama()).setParent(a);
				}

				new Label(suratKeluar.getNama()).setParent(parent);

				String nama = "Umum";
				if (suratKeluar.getMahasiswa() != null) {
					nama = suratKeluar.getMahasiswa().getNim() + " - " + suratKeluar.getMahasiswa().getNama();
				} else if (suratKeluar.getDosen() != null) {
					nama = suratKeluar.getDosen().getCode() + " - " + suratKeluar.getDosen().getNama();
				} else if (suratKeluar.getSiswa() != null) {
					nama = suratKeluar.getSiswa().getNomorInduk() + " - " + suratKeluar.getSiswa().getNama();
				} else if (suratKeluar.getGuru() != null) {
					nama = suratKeluar.getGuru().getNama();
				} else if (suratKeluar.getPegawai() != null) {
					nama = suratKeluar.getPegawai().getCode() + " - " + suratKeluar.getPegawai().getNama();
				}

				new Label(nama).setParent(parent);

				Session session = HibernateUtil.currentSession();
				String html = "";
				List<String> suratKeluarValues = session.createCriteria(OpsiSuratKeluarValue.class)
						.setProjection(Projections.groupProperty("nama"))
						.add(Restrictions.eq("suratKeluar", suratKeluar)).list();
				for (String opsiSuratKeluarValue : suratKeluarValues) {
					html += "<li>" + opsiSuratKeluarValue + "</li>";
				}
				String safeInputhtml = MyHtml.bersihkan(html);
				new ais.ui.util.MyHtml("<font style=\"font-size: x-small;\"><ul>" + safeInputhtml + "</ul></font>")
						.setParent(parent);

				Vbox myVbox = new Vbox();
				myVbox.setParent(parent);
				html = "";
				List<KlasifikasiSuratKeluarParemeterValue> klasifikasiSuratKeluarParemeterValues = session
						.createCriteria(KlasifikasiSuratKeluarParemeterValue.class)
						.createAlias("klasifikasiSuratKeluarParemeter", "klasifikasiSuratKeluarParemeter")
						.addOrder(Order.asc("klasifikasiSuratKeluarParemeter.nama"))
						.add(Restrictions.eq("suratKeluar", suratKeluar)).list();
				int size = 0;
				boolean tampilSemua = false;

				for (KlasifikasiSuratKeluarParemeterValue klasifikasiSuratKeluarParemeterValue : klasifikasiSuratKeluarParemeterValues) {
					if (size == 1) {
						tampilSemua = true;
						break;
					}
					html += "<li>" + klasifikasiSuratKeluarParemeterValue.getKlasifikasiSuratKeluarParemeter().getNama()
							+ " : " + klasifikasiSuratKeluarParemeterValue.getNama() + "</li>";
					size++;

				}

				safeInputhtml = MyHtml.bersihkan(html);

				final Html isi;
				(isi = new ais.ui.util.MyHtml("<font style=\"font-size: x-small;\"><ul>" + safeInputhtml + "</ul>"
						+ MyHtml.bersihkan(suratKeluar.getKeterangan()) + "</font>")).setParent(myVbox);

				if (tampilSemua) {
					MyToolbarbuttonConfig tdpOnline = new MyToolbarbuttonConfig("Tampilkan semua isi",
							"/img/received.png");
					tdpOnline.setParent(myVbox);
					tdpOnline.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							arg0.getTarget().setVisible(false);
							String html = "";
							Session session = HibernateUtil.currentSession();
							List<KlasifikasiSuratKeluarParemeterValue> klasifikasiSuratKeluarParemeterValues = session
									.createCriteria(KlasifikasiSuratKeluarParemeterValue.class)
									.createAlias("klasifikasiSuratKeluarParemeter", "klasifikasiSuratKeluarParemeter")
									.addOrder(Order.asc("klasifikasiSuratKeluarParemeter.nama"))
									.add(Restrictions.eq("suratKeluar", suratKeluar)).list();

							for (KlasifikasiSuratKeluarParemeterValue klasifikasiSuratKeluarParemeterValue : klasifikasiSuratKeluarParemeterValues) {

								String safeInput = MyHtml.bersihkan(
										klasifikasiSuratKeluarParemeterValue.getKlasifikasiSuratKeluarParemeter()
												.getNama() + " : " + klasifikasiSuratKeluarParemeterValue.getNama());

								html += "<li>" + safeInput + "</li>";
							}

							String safeInput = MyHtml.bersihkan(suratKeluar.getKeterangan());
							isi.setContent("<font style=\"font-size: x-small;\"><ul>" + html + "</ul>" + safeInput
									+ "</font>");
						}
					});
				}

				new Label(suratKeluar.getPerihal()).setParent(parent);

				// Kolom "Status Persetujuan" disembunyikan (visible=false di ZUL). Sel kosong tetap
				// dibuat agar jumlah sel row cocok dgn jumlah kolom; bagan ditaruh di detailAlur.
				new Label().setParent(parent);
				html = SuratKeluarAction.infoDisposisiBagan(suratKeluar);
				new ais.ui.util.MyHtml(html).setParent(detailAlur);

				if (suratKeluar.getDisposisiSop() != null) {

					new Label(Common.simpleString(suratKeluar.getKeterangan())).setParent(a);
					A aa;
					(aa = new A()).setParent(a);
					aa.setStyle("font-size:9px;");
					UIClassHelper.applyReadMore(aa, "SOP " + suratKeluar.getDisposisiSop().getKeterangan() + " ("
							+ suratKeluar.getDisposisiSop().getSop().getNama() + ")");
					aa.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							TampilanAlurSopAction.prosess(suratKeluar.getDisposisiSop().getId(), null, null, true,
									arg0.getTarget());
						}
					});
				} else {
					new Label(suratKeluar.getKeterangan()).setParent(a);
				}

				if (suratKeluar.getDisposisiSop() != null && !suratKeluar.getDisposisiSop().getAktif()) {
					new Label(ais.common.Common.getBahasaConfig("Tidak aktif")).setParent(arg0);
				} else if (edit && tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
						&& tbmuser.ambilDosen() == null && tbmuser.ambilGuru() == null) {
					final MyCheckboxConfig aktif = new MyCheckboxConfig("Aktif");
					aktif.setChecked(suratKeluar.getAktif());
					aktif.setParent(arg0);
					aktif.addEventListener("onCheck", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							suratKeluar.setAktif(aktif.isChecked());
							Common.refreshSaveOrUpdate(suratKeluar);
						}
					});
				} else {
					new Label(suratKeluar.getAktif() ? "Ya" : "Tidak").setParent(arg0);
				}

				Hbox toolbar = new Hbox();

				LampiranLain lainMahasiswa = LampiranLain.ambil(suratKeluar.getKlasifikasiSuratKeluar().getId(),
						LampiranLain.FILE_JRXML_LAYOUT_SURAT);
				if (lainMahasiswa != null && lainMahasiswa.getId() != null) {
					MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
					button.setTooltiptext("Cetak Data");
					button.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {

							suratKeluar.cetak(tbmuser);
						}

					});
					button.setParent(toolbar);
				}

				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
				button.setTooltiptext("Ubah Data");
				button.setVisible(edit);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						init(suratKeluar);
						addWindow.setVisible(true);
						addWindow.onModal();
					}

				});
				button.setParent(toolbar);

				button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
				button.setTooltiptext("Hapus Data");
				button.setVisible(delete);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						MyMessageboxConfig.show(
								"Apakah Bapak/Ibu yakin ingin menghapus data ini? Perlu diperhatikan bahwa tindakan ini bersifat permanen dan data yang telah dihapus tidak dapat dikembalikan.",
								"Pertanyaan",
								MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
								new EventListener() {

									@Override
									public void onEvent(Event event) throws Exception {
										int i = Integer.parseInt(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											try {

												Session session = HibernateUtil.currentSession();
												if (SopUtil.hapusDisposisi(session, suratKeluar.getDisposisiSop())) {

													String sql = "delete from surat.klasifikasi_surat_keluar_paremeter_value where surat_keluar = "
															+ suratKeluar.getId();

													session.createSQLQuery(sql).executeUpdate();

													Common.refreshDelete(session, suratKeluar);
													onSearchDefault(event);
												}
											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
												MyMessageboxConfig.showFormat(
														"Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lainnya. Rincian teknis: {V1}. Langkah yang dapat dilakukan: (1) periksa data lain yang masih terkait dengan data ini; (2) hapus atau lepaskan keterkaitan tersebut terlebih dahulu; (3) hubungi admin apabila memerlukan bantuan.",
														"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
														e.getMessage());
											}

										}

									}
								});

					}
				});
				button.setParent(toolbar);
				ais.ui.util.MenuAksiBaris.pasang(toolbar);
				toolbar.setParent(arg0);
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}
		}

	}

	@SuppressWarnings("rawtypes")
	public static File cetakDisposisi(Map parameters, AlurPersetujuanSuratKeluarStatus masukStatus, Tbmuser tbmuser)
			throws Exception {
		return cetakDisposisi(parameters, masukStatus, true, tbmuser);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static File cetakDisposisi(Map parameters, final AlurPersetujuanSuratKeluarStatus keluarStatus,
			boolean cetak, Tbmuser tbmuser) throws Exception {
		SuratKeluar suratKeluar = keluarStatus.getSuratKeluar();
		parameters.put("qr.surat", suratKeluar.ttdQr());
		parameters.put("disposisi.qr.surat", keluarStatus.ttdQr());

		SuratUtil.initDefaultKop(parameters, tbmuser, suratKeluar.getSatuanKerja());

		if (suratKeluar.getAlurPersetujuanSuratKeluar() != null
				&& suratKeluar.getAlurPersetujuanSuratKeluar().getSatuanKerja() != null) {
			PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
			SuratUtil.initDefaultKopAja(suratKeluar.getAlurPersetujuanSuratKeluar().getSatuanKerja(), perguruanTinggi,
					parameters, "kop_alur");
		} else if (suratKeluar.getSatuanKerja() != null) {
			PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
			SuratUtil.initDefaultKopAja(suratKeluar.getSatuanKerja(), perguruanTinggi, parameters, "kop_alur");
		} else {
			parameters.put("kop_alur", "");
		}

		parameters.put("index", suratKeluar.getKlasifikasiSuratKeluar() == null ? ""
				: suratKeluar.getKlasifikasiSuratKeluar().getNama());
		parameters.put("kode", suratKeluar.getKlasifikasiSuratKeluar() == null ? ""
				: suratKeluar.getKlasifikasiSuratKeluar().getKode());
		parameters.put("berkas", suratKeluar.getLampiran());
		Session session = HibernateUtil.currentSession();
		List<String> opsi = session.createCriteria(OpsiSuratKeluarValue.class)
				.add(Restrictions.eq("suratKeluar", suratKeluar)).setProjection(Projections.property("nama")).list();
		String oo = "";
		int i = 1;
		for (String s : opsi) {
			oo += (i + ". " + s + "\n");
			i++;
		}
		parameters.put("opsi", oo);

		String tgl_no = Common.dateFormat4.get().format(suratKeluar.getTanggal());
		tgl_no += ", " + suratKeluar.getKode();
		parameters.put("tgl_no", tgl_no);
		parameters.put("nomor", suratKeluar.getKode());
		parameters.put("asal", suratKeluar.getKepada());
		parameters.put("agenda", suratKeluar.getAgenda());
		parameters.put("ringkasan", suratKeluar.getKeterangan());

		String tgl_diterima = Common.dateFormat4.get().format(suratKeluar.getTanggal());
		parameters.put("tgl_diterima", tgl_diterima);
		parameters.put("tgl", Common.dateFormat4.get().format(suratKeluar.getTanggal_dirubah()));
		parameters.put("isi_disposisi", keluarStatus.getKeterangan());
		String diteruskan_kepada = "";
		int ii = 1;
		List<AlurPersetujuanSuratKeluarStatus> alurPersetujuanSuratKeluarStatuss = session
				.createCriteria(AlurPersetujuanSuratKeluarStatus.class).add(Restrictions.isNotNull("kodeUnik"))
				.add(Restrictions.eq("suratKeluar", suratKeluar)).addOrder(Order.asc("id")).list();

		for (AlurPersetujuanSuratKeluarStatus myAlurPersetujuanSuratKeluarStatus : alurPersetujuanSuratKeluarStatuss) {

			AlurPersetujuanSuratKeluar alurPersetujuanSuratKeluar = myAlurPersetujuanSuratKeluarStatus
					.getAlurPersetujuanSuratKeluar();

			JenisJabatan jenisJabatan = myAlurPersetujuanSuratKeluarStatus.getJenisJabatan() != null
					? myAlurPersetujuanSuratKeluarStatus.getJenisJabatan()
					: alurPersetujuanSuratKeluar.getJenisJabatan();

			String n = ". " + (jenisJabatan == null ? "" : jenisJabatan.getNama());
			if (!diteruskan_kepada.contains(n)) {
				diteruskan_kepada += (ii + n + "\n");
				ii++;
			}
		}

		List<Long> pejabatas = new ArrayList<Long>();

		Pejabat pejabat = keluarStatus.getPejabat();

		String tgl_diteruskan = Common.dateFormat4.get().format(keluarStatus.getTanggal_dirubah());
		parameters.put("tgl_diteruskan", tgl_diteruskan);

		if (pejabat != null) {
			pejabatas.add(pejabat.getId());
		}

		List<File> files = new ArrayList<File>();

		String disposisi = "";
		i = 1;
		for (AlurPersetujuanSuratKeluarStatus s : alurPersetujuanSuratKeluarStatuss) {

			String nama = "";
			try {
				LampiranLain lampiranLain = LampiranLain.ambil(s.getId(),
						AlurPersetujuanSuratKeluarStatus.class.getName());
				if (lampiranLain != null) {
					nama = lampiranLain.getNama();

					if (nama.toLowerCase().endsWith(".pdf")) {
						files.add(lampiranLain.ambilFile());
					}
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				ais.common.Common.tampilErrorJikaAdmin(e);
			}

			AlurPersetujuanSuratKeluar alurPersetujuanSuratKeluar = s.getAlurPersetujuanSuratKeluar();

			JenisJabatan jenisJabatan = s.getJenisJabatan() != null ? s.getJenisJabatan()
					: alurPersetujuanSuratKeluar.getJenisJabatan();
			pejabat = s.getPejabat();
			if (s.getPejabat() != null) {
				pejabatas.add(s.getPejabat().getId());

				if (s.getDisetujui()) {
					SuratUtil.ttdpejabat(s.getPejabat(), parameters, "setujui.");
				} else {
					SuratUtil.ttdpejabat(s.getPejabat(), parameters, "belum.");
				}
			}

			if (s.getKonseptor() != null) {
				SuratUtil.ttdpejabat(parameters, s.getKonseptor(), "disposisi.oleh.", 1);
			}

			disposisi += (i + ". "
					+ (s.getPejabat() == null ? (jenisJabatan == null ? "" : jenisJabatan.getNama())
							: s.getPejabat().getNama()
									+ (" (" + (jenisJabatan == null ? "" : jenisJabatan.getNama()) + ")"))
					+ "\n\tCatatan : " + s.getKeterangan().replaceAll("\n", " ")
					+ (nama.isEmpty() ? "" : "\n\tLampiran : " + nama) + "\n\tStatus : "

					+ (s.getDisetujui() ? "Disetujui" : (s.getDitolak() ? "Ditolak" : "")) + "\tHari/tgl : "
					+ (s.getDitolak()
							? (s.getWaktuDitolak() == null ? "" : (Common.dateFormat4.get().format(s.getWaktuDitolak())))
							: (s.getWaktuPersetujuan() == null ? ""
									: (Common.dateFormat4.get().format(s.getWaktuPersetujuan()))))
					+ "\n\n");

			String n = ". " + (jenisJabatan == null ? "" : jenisJabatan.getNama());
			if (!diteruskan_kepada.contains(n)) {
				diteruskan_kepada += (ii + n + "\n");
				ii++;
			}
			i++;
		}

		List<String> grups = session.createCriteria(JenisJabatan.class)
				.add(Restrictions.and(
						Restrictions.or(Restrictions.isNull("usernamePengguna"),
								Restrictions.ilike("usernamePengguna", "," + tbmuser.getUserId() + ",",
										MatchMode.ANYWHERE)),
						Restrictions.or(Restrictions.isNull("jenisPengguna"),
								Restrictions.ilike("jenisPengguna", "," + tbmuser.hakAkses().getRoleId() + ",",
										MatchMode.ANYWHERE))))

				.setProjection(Projections.groupProperty("grup")).addOrder(Order.asc("grup"))
				.add(Restrictions.isNotNull("grup"))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();
		if (grups.isEmpty()) {
			grups.add("Pejabat");
		}
		for (String grup : grups) {
			List<JenisJabatan> jenisJabatans = ConstantValues.simpleList(session.createCriteria(JenisJabatan.class)

					.add(Restrictions.and(
							Restrictions.or(Restrictions.isNull("usernamePengguna"),
									Restrictions.ilike("usernamePengguna", "," + tbmuser.getUserId() + ",",
											MatchMode.ANYWHERE)),
							Restrictions.or(Restrictions.isNull("jenisPengguna"),
									Restrictions.ilike("jenisPengguna", "," + tbmuser.hakAkses().getRoleId() + ",",
											MatchMode.ANYWHERE))))

					.addOrder(Order.asc("nomorUrut"))
					.add(Restrictions.or(Restrictions.isNull("grup"), Restrictions.eq("grup", grup)))
					.addOrder(Order.asc("nama"))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
					JenisJabatan.class);

			if (!jenisJabatans.isEmpty()) {

				for (JenisJabatan jenisJabatan : jenisJabatans) {

					List<Pejabat> pejabats = ConstantValues.simpleList(
							session.createCriteria(Pejabat.class)
									.add(pejabatas.isEmpty() ? Restrictions.sqlRestriction("true")
											: Restrictions.not(Restrictions.in("id", pejabatas)))
									.add(Restrictions.eq("jenisJabatan", jenisJabatan))
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
							Pejabat.class);

					for (Pejabat aa : pejabats) {
						if (aa != null) {
							int count = suratKeluar == null || suratKeluar.getId() == null ? 0
									: ((Number) session.createCriteria(AlurPersetujuanSuratKeluarStatus.class)
											.add(Restrictions.isNotNull("kodeUnik"))
											.add(Restrictions.eq("suratKeluar", suratKeluar))
											.add(Restrictions.eq("pejabat", aa))
											.add(Restrictions.eq("jenisJabatan", aa.getJenisJabatan()))
											.setProjection(Projections.rowCount()).uniqueResult()).intValue();

							if (count > 0) {

								disposisi += (i + ". " + aa.getNama() + (" ("
										+ (aa.getJenisJabatan() == null ? "" : aa.getJenisJabatan().getNama()) + ")"));

								String n = ". " + (jenisJabatan == null ? "" : jenisJabatan.getNama());
								if (!diteruskan_kepada.contains(n)) {
									diteruskan_kepada += (ii + n + "\n");
									ii++;
								}

								i++;
							}

						}
					}
				}
			}
		}

		parameters.put("disposisi", disposisi);
		parameters.put("diteruskan_kepada", diteruskan_kepada);

		LampiranLain lainMahasiswa = LampiranLain.ambil(suratKeluar.getKlasifikasiSuratKeluar().getId(),
				LampiranLain.FILE_JRXML_LAYOUT_DISPOSISI);

		if (!files.isEmpty()) {
			File fileHasil;

			if (lainMahasiswa != null && lainMahasiswa.getId() != null) {
				fileHasil = Report.generateCompileFileReport(Report.PDF, parameters,
						lainMahasiswa.ambilFile().getAbsolutePath(), ais.ui.util.WaktuUtil.getDate(), false);
			} else {
				fileHasil = Report.generateFileReport(Report.PDF, parameters, "surat/disposisi_keluar",
						suratKeluar.getTanggal_dirubah(), new Toolbar());
			}

			PDFMergerUtility ut = new PDFMergerUtility();
			ut.addSource(fileHasil);
			for (File f : files) {
				ut.addSource(f);
			}
			File filePdfBaru = new File(
					fileHasil.getParentFile().getAbsolutePath() + "/" + Common.getGeneratedBarCode() + ".pdf");
			ut.setDestinationStream(new FileOutputStream(filePdfBaru));
			ut.mergeDocuments();
			// Salin HTML companion agar toggle pratinjau HTML/PDF muncul setelah merge
			File htmlSrcD = new File(fileHasil.getAbsolutePath() + ".html");
			if (htmlSrcD.exists() && htmlSrcD.length() > 0) {
				File htmlDstD = new File(filePdfBaru.getAbsolutePath() + ".html");
				java.io.FileInputStream isCopyD = null;
				java.io.FileOutputStream osCopyD = null;
				try {
					isCopyD = new java.io.FileInputStream(htmlSrcD);
					osCopyD = new java.io.FileOutputStream(htmlDstD);
					byte[] buf = new byte[8192];
					int n;
					while ((n = isCopyD.read(buf)) > 0) osCopyD.write(buf, 0, n);
				} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/surat/SuratKeluarAction.java:1056");
				} finally {
					if (isCopyD != null) try { isCopyD.close(); } catch (Exception ig2) { ais.common.ErrorAuditUtil.record(ig2, "auto-audit(empty-catch) src/ais/action/master/surat/SuratKeluarAction.java:1058");}
					if (osCopyD != null) try { osCopyD.close(); } catch (Exception ig2) { ais.common.ErrorAuditUtil.record(ig2, "auto-audit(empty-catch) src/ais/action/master/surat/SuratKeluarAction.java:1059");}
				}
			}

			if (cetak) {
				Report.tampil(filePdfBaru);
			}

			return filePdfBaru;
		} else {

			File fileHasil = null;

			try {
				if (cetak) {

					if (lainMahasiswa != null && lainMahasiswa.getId() != null) {
						fileHasil = Report.generateCompileFileReport(Report.PDF, parameters,
								lainMahasiswa.ambilFile().getAbsolutePath(), ais.ui.util.WaktuUtil.getDate());
						Report.tampil(fileHasil);
					} else {
						Report.generatePDFReport(Report.PDF, parameters, "surat/disposisi_keluar",
								suratKeluar.getTanggal_dirubah());
					}
				} else {
					if (lainMahasiswa != null && lainMahasiswa.getId() != null) {
						fileHasil = Report.generateCompileFileReport(Report.PDF, parameters,
								lainMahasiswa.ambilFile().getAbsolutePath(), ais.ui.util.WaktuUtil.getDate());
					} else {
						fileHasil = Report.generateFileReport(Report.PDF, parameters, "surat/disposisi_keluar",
								suratKeluar.getTanggal_dirubah(), new Toolbar());
					}
				}
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}

			return fileHasil;
		}
	}

	@SuppressWarnings("unchecked")
	public static String infoDisposisi(SuratKeluar suratKeluar) {
		String html = "";
		Session session = HibernateUtil.currentSession();
		List<AlurPersetujuanSuratKeluarStatus> alurPersetujuanSuratKeluarStatuss = session
				.createCriteria(AlurPersetujuanSuratKeluarStatus.class).add(Restrictions.isNotNull("kodeUnik"))
				.add(Restrictions.eq("suratKeluar", suratKeluar)).addOrder(Order.asc("id")).list();

		for (AlurPersetujuanSuratKeluarStatus myAlurPersetujuanSuratKeluarStatus : alurPersetujuanSuratKeluarStatuss) {

			String url = "";
			String nama = "";
			try {
				LampiranLain lampiranLain = LampiranLain.ambil(myAlurPersetujuanSuratKeluarStatus.getId(),
						AlurPersetujuanSuratKeluarStatus.class.getName());
				if (lampiranLain != null) {
					nama = lampiranLain.getNama();
					url = lampiranLain.createLinkUri();

				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				ais.common.Common.tampilErrorJikaAdmin(e);
			}

			if (myAlurPersetujuanSuratKeluarStatus.getJenisJabatan() == null
					&& myAlurPersetujuanSuratKeluarStatus.getAlurPersetujuanSuratKeluar() != null
					&& myAlurPersetujuanSuratKeluarStatus.getAlurPersetujuanSuratKeluar().getJenisJabatan() != null) {

				html += "<li>" + myAlurPersetujuanSuratKeluarStatus.getAlurPersetujuanSuratKeluar() + " : "
						+ (myAlurPersetujuanSuratKeluarStatus.getDisetujui()
								? ("<font style=\"font-size: x-small;color:blue;font-weight: bolder;font-family: Poppins, Helvetica, 'sans-serif';\">Sudah ditindak-lanjuti "
										+ (myAlurPersetujuanSuratKeluarStatus.getPejabat() == null
												|| myAlurPersetujuanSuratKeluarStatus.getPejabat().getPegawai() == null
														? (myAlurPersetujuanSuratKeluarStatus.getPejabat()
																.getDosen() == null
																		? ""
																		: " " + myAlurPersetujuanSuratKeluarStatus
																				.getPejabat().getDosen().getNama())
														: " " + myAlurPersetujuanSuratKeluarStatus.getPejabat()
																.getPegawai().getNama())
										+ (!myAlurPersetujuanSuratKeluarStatus.getKeterangan().trim().isEmpty()
												? " dengan catatan \""
														+ myAlurPersetujuanSuratKeluarStatus.getKeterangan() + "\""
												: "")
										+ (myAlurPersetujuanSuratKeluarStatus.getWaktuPersetujuan() == null ? ""
												: " pada waktu " + Common.dateFormat3.get().format(
														myAlurPersetujuanSuratKeluarStatus.getWaktuPersetujuan()))
										+ "</font>")
								: myAlurPersetujuanSuratKeluarStatus.getDitolak()
										? ("<font style=\"font-size: x-small;color:red;font-weight: bolder;font-family: Poppins, Helvetica, 'sans-serif';\">Ditolak "
												+ (myAlurPersetujuanSuratKeluarStatus.getPejabat() == null
														|| myAlurPersetujuanSuratKeluarStatus.getPejabat()
																.getPegawai() == null
																		? (myAlurPersetujuanSuratKeluarStatus
																				.getPejabat().getDosen() == null
																						? ""
																						: " " + myAlurPersetujuanSuratKeluarStatus
																								.getPejabat().getDosen()
																								.getNama())
																		: " " + myAlurPersetujuanSuratKeluarStatus
																				.getPejabat().getPegawai().getNama())
												+ (!myAlurPersetujuanSuratKeluarStatus.getKeterangan().trim().isEmpty()
														? " dengan catatan \""
																+ myAlurPersetujuanSuratKeluarStatus.getKeterangan()
																+ "\""
														: "")
												+ (myAlurPersetujuanSuratKeluarStatus.getWaktuDitolak() == null ? ""
														: " pada waktu " + Common.dateFormat3.get().format(
																myAlurPersetujuanSuratKeluarStatus.getWaktuDitolak()))
												+ "</font>")
										: "<font style=\"font-size: x-small;color:red;font-weight: bolder;font-family: Poppins, Helvetica, 'sans-serif';\">Belum ditindak lanjuti"
												+ (myAlurPersetujuanSuratKeluarStatus.getPejabat() == null ? ""
														: " " + myAlurPersetujuanSuratKeluarStatus.getPejabat()
																.getNama())
												+ "</font>")

						+ (url.isEmpty() ? "" : ", <a href='" + url + "' target='_blank'>" + nama + "</a>")

						+ "</li>";

			} else if (myAlurPersetujuanSuratKeluarStatus.getJenisJabatan() != null) {
				html += "<li>" + myAlurPersetujuanSuratKeluarStatus.getJenisJabatan().getNama() + " : "
						+ (myAlurPersetujuanSuratKeluarStatus.getDisetujui()
								? ("<font style=\"font-size: x-small;color:blue;font-weight: bolder;font-family: Poppins, Helvetica, 'sans-serif';\">Sudah ditindak-lanjuti "
										+ (myAlurPersetujuanSuratKeluarStatus.getPejabat() == null
												|| myAlurPersetujuanSuratKeluarStatus.getPejabat().getPegawai() == null
														? (myAlurPersetujuanSuratKeluarStatus.getPejabat()
																.getDosen() == null
																		? ""
																		: " " + myAlurPersetujuanSuratKeluarStatus
																				.getPejabat().getDosen().getNama())
														: " " + myAlurPersetujuanSuratKeluarStatus.getPejabat()
																.getPegawai().getNama())
										+ (!myAlurPersetujuanSuratKeluarStatus.getKeterangan().trim().isEmpty()
												? " dengan catatan \""
														+ myAlurPersetujuanSuratKeluarStatus.getKeterangan() + "\""
												: "")
										+ (myAlurPersetujuanSuratKeluarStatus.getWaktuPersetujuan() == null ? ""
												: " pada waktu " + Common.dateFormat3.get().format(
														myAlurPersetujuanSuratKeluarStatus.getWaktuPersetujuan()))
										+ "</font>")
								: myAlurPersetujuanSuratKeluarStatus.getDitolak()
										? ("<font style=\"font-size: x-small;color:red;font-weight: bolder;font-family: Poppins, Helvetica, 'sans-serif';\">Ditolak "
												+ (myAlurPersetujuanSuratKeluarStatus.getPejabat() == null
														|| myAlurPersetujuanSuratKeluarStatus.getPejabat()
																.getPegawai() == null
																		? (myAlurPersetujuanSuratKeluarStatus
																				.getPejabat().getDosen() == null
																						? ""
																						: " " + myAlurPersetujuanSuratKeluarStatus
																								.getPejabat().getDosen()
																								.getNama())
																		: " " + myAlurPersetujuanSuratKeluarStatus
																				.getPejabat().getPegawai().getNama())
												+ (!myAlurPersetujuanSuratKeluarStatus.getKeterangan().trim().isEmpty()
														? " dengan catatan \""
																+ myAlurPersetujuanSuratKeluarStatus.getKeterangan()
																+ "\""
														: "")
												+ (myAlurPersetujuanSuratKeluarStatus.getWaktuDitolak() == null ? ""
														: " pada waktu " + Common.dateFormat3.get().format(
																myAlurPersetujuanSuratKeluarStatus.getWaktuDitolak()))
												+ "</font>")
										: "<font style=\"font-size: x-small;color:red;font-weight: bolder;font-family: Poppins, Helvetica, 'sans-serif';\">Belum ditindak lanjuti"
												+ (myAlurPersetujuanSuratKeluarStatus.getPejabat() == null ? ""
														: " " + myAlurPersetujuanSuratKeluarStatus.getPejabat()
																.getNama())
												+ "</font>")

						+ (url.isEmpty() ? "" : ", <a href='" + url + "' target='_blank'>" + nama + "</a>") + "</li>";
			}
		}

		if (suratKeluar.getAlurDitolak() != null && suratKeluar.getAlurDitolak().getTelahDirevisi()) {
			html += "<li style=\"font-size: x-small;color:blue;font-weight: bolder;font-family: Poppins, Helvetica, 'sans-serif';\">Direvisi dengan catatan : "
					+ suratKeluar.getAlurDitolak().getCatatanRevisi() + "</li>";

			html += "<li style=\"font-size: x-small;color:red;font-weight: bolder;font-family: Poppins, Helvetica, 'sans-serif';\">Sebelumnya ditolak dengan catatan : "
					+ suratKeluar.getAlurDitolak().getKeterangan() + "</li>";

		} else if (suratKeluar.getAlurDitolak() != null && suratKeluar.getAlurDitolak().getDitolak()) {
			html += "<li style=\"font-size: x-small;color:red;font-weight: bolder;font-family: Poppins, Helvetica, 'sans-serif';\">Ditolak dengan catatan : "
					+ suratKeluar.getAlurDitolak().getKeterangan() + "</li>";
		}
		String safeInputhtml = MyHtml.bersihkan(html);
		html = "<font style=\"font-size: x-small;\"><ul>" + safeInputhtml + "</ul></font>";
		return html;
	}

	private static final class DisposisiKeluarChip {
		int nomor;
		String label;
		String status;
		String warna;
		String latar;
		String tooltip;
	}

	/**
	 * Informasi disposisi surat keluar untuk dashboard: dikelompokkan mengikuti
	 * kolom Grup pada setup Daftar Pegawai Pada Disposisi Surat.
	 */
	public static String infoDisposisiBagan(SuratKeluar suratKeluar) {
		Session session = HibernateUtil.currentSession();
		List<AlurPersetujuanSuratKeluarStatus> list = session.createCriteria(AlurPersetujuanSuratKeluarStatus.class)
				.add(Restrictions.isNotNull("kodeUnik")).add(Restrictions.eq("suratKeluar", suratKeluar))
				.addOrder(Order.asc("id")).list();

		Map<String, List<DisposisiKeluarChip>> perGrup = new LinkedHashMap<String, List<DisposisiKeluarChip>>();
		int nomor = 1;
		String prevLabel = null;
		Date prevWaktu = null;
		for (AlurPersetujuanSuratKeluarStatus s : list) {
			JenisJabatan jenisJabatan = jenisJabatanDariStatusKeluar(s);
			if (jenisJabatan == null && (s == null || s.getAlurPersetujuanSuratKeluar() == null)) {
				continue;
			}

			String grup = normalisasiGrupDisposisi(jenisJabatan == null ? null : jenisJabatan.getGrup());
			List<DisposisiKeluarChip> chips = perGrup.get(grup);
			if (chips == null) {
				chips = new ArrayList<DisposisiKeluarChip>();
				perGrup.put(grup, chips);
			}

			DisposisiKeluarChip chip = new DisposisiKeluarChip();
			chip.nomor = nomor;
			chip.label = labelDisposisiKeluar(s, jenisJabatan);
			isiStatusChipKeluar(chip, s);
			chip.tooltip = tooltipDisposisiKeluar(s, chip.label, chip.status, prevLabel, prevWaktu);
			chips.add(chip);
			nomor++;
			prevLabel = chip.label;
			prevWaktu = waktuDisposisiKeluar(s);
		}

		String html = buatHtmlDisposisiKeluarBergrup(perGrup);

		if (suratKeluar.getAlurDitolak() != null && Boolean.TRUE.equals(suratKeluar.getAlurDitolak().getTelahDirevisi())) {
			html += "<div style='font-size:11px;color:#1d4ed8;font-weight:800;margin-top:6px;'>Direvisi dengan catatan : "
					+ ais.ui.util.DashboardUiKit.esc(suratKeluar.getAlurDitolak().getCatatanRevisi()) + "</div>";
			html += "<div style='font-size:11px;color:#dc2626;font-weight:800;margin-top:3px;'>Sebelumnya ditolak dengan catatan : "
					+ ais.ui.util.DashboardUiKit.esc(suratKeluar.getAlurDitolak().getKeterangan()) + "</div>";
		} else if (suratKeluar.getAlurDitolak() != null && Boolean.TRUE.equals(suratKeluar.getAlurDitolak().getDitolak())) {
			html += "<div style='font-size:11px;color:#dc2626;font-weight:800;margin-top:6px;'>Ditolak dengan catatan : "
					+ ais.ui.util.DashboardUiKit.esc(suratKeluar.getAlurDitolak().getKeterangan()) + "</div>";
		}

		return html;
	}

	private static String buatHtmlDisposisiKeluarBergrup(Map<String, List<DisposisiKeluarChip>> perGrup) {
		if (perGrup == null || perGrup.isEmpty()) {
			return "<div style='font-size:11px;color:#64748b;background:#f8fafc;border:1px dashed #cbd5e1;"
					+ "border-radius:8px;padding:8px 10px;'>Surat ini belum memiliki disposisi.</div>";
		}

		StringBuilder sb = new StringBuilder();
		sb.append("<div style='margin:8px 0 6px 0;font-size:11px;color:#0f172a;background:#fff;box-sizing:border-box;'>");
		List<String> urutanGrup = urutanGrupDisposisiDariSetup(perGrup);
		for (String grup : urutanGrup) {
			List<DisposisiKeluarChip> chips = perGrup.get(grup);
			if (chips == null || chips.isEmpty()) {
				continue;
			}
			sb.append("<div style='position:relative;margin:12px 0 14px 0;border:1px solid #d7dce7;"
					+ "border-radius:8px;background:#fff;padding:22px 18px 13px 18px;"
					+ "box-shadow:0 1px 4px rgba(15,23,42,.06);box-sizing:border-box;'>");
			sb.append("<span style='position:absolute;top:-10px;left:12px;background:#1f4b99;color:#fff;"
					+ "border-radius:4px;padding:4px 12px;font-size:10px;font-weight:800;line-height:1;'>")
					.append(ais.ui.util.DashboardUiKit.esc(labelGrupDisposisi(grup))).append("</span>");
			// auto-fill + minmax responsif: jumlah kolom mengikuti lebar panel "Informasi Disposisi"
			// (di modal Ubah panelnya sempit) sehingga kartu chip TIDAK melebihi border. Sebelumnya
			// dipaksa 3 kolom min 180px + gap 28px (≈596px) sehingga meluber keluar border.
			sb.append("<div style='display:grid;grid-template-columns:repeat(auto-fill,minmax(150px,1fr));"
					+ "gap:10px 14px;align-items:center;box-sizing:border-box;width:100%;'>");
			for (DisposisiKeluarChip chip : chips) {
				// Kartu chip 2 BARIS: baris-1 = nomor + NAMA PENGGUNA (lengkap, tidak dipotong/ditutupi),
				// baris-2 = KETERANGAN/STATUS. Sebelumnya 1 baris (inline-flex nowrap) sehingga nama
				// ter-ellipsis / tertutup label status. Contoh: "Ketua Umum" (atas) / "Menunggu persetujuan" (bawah).
				sb.append("<span title='").append(ais.ui.util.DashboardUiKit.esc(chip.tooltip))
						.append("' style='display:flex;flex-direction:column;gap:3px;max-width:100%;"
								+ "padding:5px 10px;border-radius:12px;background:")
						.append(chip.latar).append(";border:1px solid ").append(chip.warna)
						.append("33;color:#0f172a;box-sizing:border-box;'>");
				// baris 1: nomor + nama pengguna disposisi
				sb.append("<span style='display:flex;align-items:center;gap:6px;'>");
				sb.append("<span style='flex:0 0 auto;width:18px;height:18px;border-radius:999px;background:")
						.append(chip.warna)
						.append(";color:#fff;font-size:10px;font-weight:900;display:inline-flex;align-items:center;"
								+ "justify-content:center;'>")
						.append(chip.nomor).append("</span>");
				sb.append("<span style='font-size:10px;font-weight:800;color:#0f172a;line-height:1.25;"
						+ "word-break:break-word;'>").append(ais.ui.util.DashboardUiKit.esc(chip.label))
						.append("</span>");
				sb.append("</span>");
				// baris 2: keterangan/status disposisi
				sb.append("<span style='align-self:flex-start;font-size:9px;font-weight:800;color:").append(chip.warna)
						.append(";background:#fff;border:1px solid ").append(chip.warna)
						.append("33;border-radius:999px;padding:1px 7px;'>")
						.append(ais.ui.util.DashboardUiKit.esc(chip.status)).append("</span>");
				sb.append("</span>");
			}
			sb.append("</div></div>");
		}
		sb.append("</div>");
		return sb.toString();
	}

	private static List<String> urutanGrupDisposisiDariSetup(Map<String, List<DisposisiKeluarChip>> perGrup) {
		List<String> urutan = new ArrayList<String>();
		if (perGrup == null || perGrup.isEmpty()) {
			return urutan;
		}

		try {
			List<JenisJabatan> setup = HibernateUtil.currentSession().createCriteria(JenisJabatan.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.addOrder(Order.asc("nomorUrut")).addOrder(Order.asc("nama")).list();
			for (JenisJabatan jenisJabatan : setup) {
				if (jenisJabatan == null) {
					continue;
				}
				String grup = normalisasiGrupDisposisi(jenisJabatan.getGrup());
				if (perGrup.containsKey(grup) && !urutan.contains(grup)) {
					urutan.add(grup);
				}
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit src/ais/action/master/surat/SuratKeluarAction.java:urutanGrupDisposisiDariSetup");
		}

		for (String grup : perGrup.keySet()) {
			if (!urutan.contains(grup)) {
				urutan.add(grup);
			}
		}
		urutkanGrupDisposisiBerdasarkanNomor(urutan);
		return urutan;
	}

	private static void urutkanGrupDisposisiBerdasarkanNomor(List<String> urutan) {
		java.util.Collections.sort(urutan, new java.util.Comparator<String>() {
			@Override
			public int compare(String a, String b) {
				int na = nomorAwalGrupDisposisi(a);
				int nb = nomorAwalGrupDisposisi(b);
				if (na != nb) {
					return na < nb ? -1 : 1;
				}
				return normalisasiGrupDisposisi(a).compareToIgnoreCase(normalisasiGrupDisposisi(b));
			}
		});
	}

	private static int nomorAwalGrupDisposisi(String grup) {
		String nilai = normalisasiGrupDisposisi(grup);
		int mulai = 0;
		while (mulai < nilai.length() && Character.isWhitespace(nilai.charAt(mulai))) {
			mulai++;
		}
		int selesai = mulai;
		while (selesai < nilai.length() && Character.isDigit(nilai.charAt(selesai))) {
			selesai++;
		}
		if (selesai == mulai) {
			return Integer.MAX_VALUE;
		}
		try {
			return Integer.parseInt(nilai.substring(mulai, selesai));
		} catch (Exception e) {
			return Integer.MAX_VALUE;
		}
	}

	private static JenisJabatan jenisJabatanDariStatusKeluar(AlurPersetujuanSuratKeluarStatus status) {
		if (status == null) {
			return null;
		}
		if (status.getJenisJabatan() != null) {
			return status.getJenisJabatan();
		}
		if (status.getAlurPersetujuanSuratKeluar() != null) {
			return status.getAlurPersetujuanSuratKeluar().getJenisJabatan();
		}
		return null;
	}

	private static String normalisasiGrupDisposisi(String grup) {
		if (grup == null || grup.trim().isEmpty()) {
			return "Pejabat";
		}
		return grup.trim();
	}

	private static String labelGrupDisposisi(String grup) {
		return normalisasiGrupDisposisi(grup);
	}

	private static String labelDisposisiKeluar(AlurPersetujuanSuratKeluarStatus status, JenisJabatan jenisJabatan) {
		if (jenisJabatan != null && jenisJabatan.getNama() != null && !jenisJabatan.getNama().trim().isEmpty()) {
			return jenisJabatan.getNama();
		}
		if (status != null && status.getAlurPersetujuanSuratKeluar() != null) {
			return String.valueOf(status.getAlurPersetujuanSuratKeluar());
		}
		return "Disposisi";
	}

	private static void isiStatusChipKeluar(DisposisiKeluarChip chip, AlurPersetujuanSuratKeluarStatus status) {
		if (status != null && Boolean.TRUE.equals(status.getDisetujui())) {
			chip.status = "Disetujui";
			chip.warna = "#16a34a";
			chip.latar = "#dcfce7";
		} else if (status != null && Boolean.TRUE.equals(status.getDitolak())) {
			chip.status = "Ditolak";
			chip.warna = "#dc2626";
			chip.latar = "#fee2e2";
		} else {
			chip.status = "Menunggu Persetujuan";
			chip.warna = "#f59e0b";
			chip.latar = "#fef3c7";
		}
	}

	private static Date waktuDisposisiKeluar(AlurPersetujuanSuratKeluarStatus status) {
		if (status == null) {
			return null;
		}
		if (Boolean.TRUE.equals(status.getDisetujui()) && status.getWaktuPersetujuan() != null) {
			return status.getWaktuPersetujuan();
		}
		if (Boolean.TRUE.equals(status.getDitolak()) && status.getWaktuDitolak() != null) {
			return status.getWaktuDitolak();
		}
		return status.getTanggal_dirubah();
	}

	private static String tooltipDisposisiKeluar(AlurPersetujuanSuratKeluarStatus status, String label,
			String statusText, String prevLabel, Date prevWaktu) {
		StringBuilder sb = new StringBuilder();
		sb.append(label).append(" - ").append(statusText);
		if (status == null) {
			return sb.toString();
		}
		String pejabat = status.getPejabat() == null ? "" : status.getPejabat().getNama();
		if (pejabat != null && pejabat.trim().length() > 0) {
			sb.append(" - ").append(pejabat);
		}
		Date waktu = null;
		if (Boolean.TRUE.equals(status.getDisetujui())) {
			waktu = status.getWaktuPersetujuan();
		} else if (Boolean.TRUE.equals(status.getDitolak())) {
			waktu = status.getWaktuDitolak();
		}
		if (waktu != null) {
			sb.append(" - ").append(Common.dateFormat3.get().format(waktu));
		}
		if (status.getKeterangan() != null && status.getKeterangan().trim().length() > 0) {
			sb.append(" - ").append(status.getKeterangan().replace('\n', ' '));
		}
		String konseptor = ringkasanKonseptorKeluar(status.getKonseptor());
		if (konseptor != null && konseptor.trim().length() > 0) {
			sb.append("\nJabatan yang mendisposisikan : ").append(konseptor);
			Date waktuDisposisi = waktuDisposisiKeluar(status);
			if (waktuDisposisi != null) {
				sb.append("\nTanggal & Waktu : ").append(Common.dateFormat3.get().format(waktuDisposisi));
			}
		}
		return sb.toString();
	}

	private static String ringkasanKonseptorKeluar(Tbmuser konseptor) {
		if (konseptor == null) {
			return "";
		}
		String ringkasan = String.valueOf(konseptor);
		return bersihkanKeteranganKurungKonseptorKeluar(ringkasan);
	}

	private static String bersihkanKeteranganKurungKonseptorKeluar(String ringkasan) {
		if (ringkasan == null || "null".equalsIgnoreCase(ringkasan.trim())) {
			return "";
		}
		return ringkasan.trim().replaceAll("\\s*\\([^)]*\\)\\s*$", "").trim();
	}

	private static String labelJabatanKonseptorKeluar(Tbmuser konseptor) {
		JenisJabatan jenisJabatan = jenisJabatanKonseptorKeluar(konseptor);
		if (jenisJabatan != null && jenisJabatan.getNama() != null && jenisJabatan.getNama().trim().length() > 0) {
			return jenisJabatan.getNama();
		}
		return "";
	}

	private static JenisJabatan jenisJabatanKonseptorKeluar(Tbmuser konseptor) {
		if (konseptor == null) {
			return null;
		}
		try {
			Tbmrole role = konseptor.hakAkses();
			if (role != null && role.getJenisJabatan() != null) {
				return role.getJenisJabatan();
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit src/ais/action/master/surat/SuratKeluarAction.java:jenisJabatanKonseptorKeluar-role");
		}
		try {
			List<Pejabat> pejabats = ConstantValues.simpleList(
					HibernateUtil.currentSession().createCriteria(Pejabat.class)
							.add(Restrictions.or(
									Restrictions.ilike("usernamePengguna", "," + konseptor.getUserId() + ",",
											MatchMode.ANYWHERE),
									Restrictions.or(Restrictions.eq("pegawai", konseptor.getPegawai()),
											Restrictions.or(Restrictions.eq("dosen", konseptor.getDosen()),
													Restrictions.eq("guru", konseptor.getGuru())))))
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.setMaxResults(1),
					Pejabat.class);
			if (!pejabats.isEmpty() && pejabats.get(0).getJenisJabatan() != null) {
				return pejabats.get(0).getJenisJabatan();
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit src/ais/action/master/surat/SuratKeluarAction.java:jenisJabatanKonseptorKeluar-pejabat");
		}
		return null;
	}

	private static String namaKonseptorKeluar(Tbmuser konseptor) {
		if (konseptor == null) {
			return "";
		}
		if (konseptor.getUserNama() != null && konseptor.getUserNama().trim().length() > 0) {
			return konseptor.getUserNama();
		}
		return konseptor.getUserId() == null ? "" : konseptor.getUserId();
	}

	public void onAdd(Event event) throws Exception {
		init(new SuratKeluar());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	public static void onAddExternal(EventListener eventListener, SuratKeluar suratKeluar) throws Exception {
		onAddExternal(eventListener, suratKeluar, false, Common.getCurrentTahunAkademik(),
				Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);
	}

	public static void onAddExternal(EventListener eventListener, SuratKeluar suratKeluar, boolean kunciKlasifikasi,
			String ta, String smt) throws Exception {
		SuratKeluarAction skripsiAction = new SuratKeluarAction();

		skripsiAction.eventListener = eventListener;
		skripsiAction.addWindow = new MyWindow();
		skripsiAction.tbmuser = Common.getCurrentUser();
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(skripsiAction.addWindow);
		skripsiAction.addWindow.setHeight("95%");
		skripsiAction.addWindow.setWidth("90%");

		suratKeluar.setTahunAkademik(ta);
		suratKeluar.setSemester(smt);

		skripsiAction.init(suratKeluar);

		if (kunciKlasifikasi) {
			skripsiAction.klasifikasiSuratKeluar.setDisabled(true);
			skripsiAction.mahasiswa.setDisabled(true);

		}

		if (ta != null && !ta.isEmpty()) {
			skripsiAction.tahunAkademik.setDisabled(true);
		}
		if (smt != null && !smt.isEmpty()) {
			skripsiAction.ganjilGenap.setDisabled(true);
		}

		skripsiAction.addWindow.setVisible(true);
		skripsiAction.addWindow.setClosable(true);
		skripsiAction.addWindow.onModal();
	}

	public String generateCode(boolean tambah) {
		KlasifikasiSuratKeluar klasifikasiSuratKeluar = (KlasifikasiSuratKeluar) (this.klasifikasiSuratKeluar
				.getAttribute("klasifikasiSuratKeluar"));
		return SuratKeluarAction.generateCode(tambah, klasifikasiSuratKeluar, tanggal.getValue());
	}

	public synchronized static String generateCode(boolean tambah, KlasifikasiSuratKeluar klasifikasiSuratKeluar,
			Date tanggal) {

		if (klasifikasiSuratKeluar == null || klasifikasiSuratKeluar.getNomorSurat() == null) {
			return "";
		}

		Long index = klasifikasiSuratKeluar.getNomorSurat().getGunakanIndexUrut()
				? klasifikasiSuratKeluar.getNomorSurat().getNomorIndex()
				: getindex(klasifikasiSuratKeluar);
		if (tambah && klasifikasiSuratKeluar.getNomorSurat().getGunakanIndexUrut()) {
			NomorSurat.tambahIndexNomorSurat(klasifikasiSuratKeluar.getNomorSurat());
		}
		String noAgenda = klasifikasiSuratKeluar.getNomorSurat().format(index, tanggal);
		try {
			noAgenda = org.apache.commons.lang3.StringUtils.replaceIgnoreCase(noAgenda, "KODE_KLASIFIKASI",
					klasifikasiSuratKeluar.getKode());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/SuratKeluarAction.java:1412");
			// TODO: handle exception
		}

		if (tambah && klasifikasiSuratKeluar.getNomorSurat().getGunakanIndexUrut()) {
			int count = 0;
			try {
				Session session = HibernateUtil.currentNativeSession();

				count = ((Number) session.createCriteria(SuratKeluar.class).add(Restrictions.eq("kode", noAgenda))
						.setProjection(Projections.rowCount()).uniqueResult()).intValue();

				// session.disconnect();
				if (session.isOpen()) {
					session.disconnect();
					session.close();
				}

			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}
			HibernateUtil.closeSession();

			if (count > 0) {
				NomorSurat.tambahIndexNomorSurat(klasifikasiSuratKeluar.getNomorSurat());
				noAgenda = generateCode(tambah, klasifikasiSuratKeluar, tanggal);
			}
		}

		// Guard anti-duplikat untuk jalur non-index-urut (rowCount tidak selalu monoton, mis. setelah
		// penghapusan): naikkan urutan sampai kode BENAR-BENAR belum dipakai surat lain.
		if (!klasifikasiSuratKeluar.getNomorSurat().getGunakanIndexUrut()) {
			int guard = 0;
			while (kodeSudahDipakai(noAgenda, null) && guard++ < 10000) {
				index = index + 1;
				noAgenda = klasifikasiSuratKeluar.getNomorSurat().format(index, tanggal);
				try {
					noAgenda = org.apache.commons.lang3.StringUtils.replaceIgnoreCase(noAgenda, "KODE_KLASIFIKASI",
							klasifikasiSuratKeluar.getKode());
				} catch (Exception e) {
					ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) SuratKeluarAction.generateCode-unik");
				}
			}
		}

		return noAgenda;
	}

	/**
	 * Apakah sebuah nomor surat (kolom {@code kode}) sudah dipakai surat lain. {@code kecualiId}
	 * boleh diisi untuk mengabaikan surat yang sedang diedit (agar update tak dianggap bentrok
	 * dengan dirinya sendiri).
	 */
	public static boolean kodeSudahDipakai(String kode, Long kecualiId) {
		if (kode == null || kode.trim().isEmpty()) {
			return false;
		}
		try {
			org.hibernate.Criteria c = HibernateUtil.currentSession().createCriteria(SuratKeluar.class)
					.add(Restrictions.eq("kode", kode));
			if (kecualiId != null) {
				c.add(Restrictions.ne("id", kecualiId));
			}
			Number n = (Number) c.setProjection(Projections.rowCount()).uniqueResult();
			return n != null && n.intValue() > 0;
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) SuratKeluarAction.kodeSudahDipakai");
			return false;
		}
	}

	public String generateCodeAgenda(boolean tambah) {
		KlasifikasiSuratKeluar klasifikasiSuratKeluar = (KlasifikasiSuratKeluar) (this.klasifikasiSuratKeluar
				.getAttribute("klasifikasiSuratKeluar"));
		return SuratKeluarAction.generateCodeAgenda(tambah, klasifikasiSuratKeluar, tanggal.getValue());
	}

	public synchronized static String generateCodeAgenda(boolean tambah, KlasifikasiSuratKeluar klasifikasiSuratKeluar,
			Date tanggal) {

		if (klasifikasiSuratKeluar == null || klasifikasiSuratKeluar.getNomorAgenda() == null) {
			return "";
		}
		Long index = klasifikasiSuratKeluar.getNomorAgenda().getGunakanIndexUrut()
				? klasifikasiSuratKeluar.getNomorAgenda().getNomorIndex()
				: getindex(klasifikasiSuratKeluar);
		if (tambah && klasifikasiSuratKeluar.getNomorAgenda().getGunakanIndexUrut()) {
			NomorSurat.tambahIndexNomorSurat(klasifikasiSuratKeluar.getNomorAgenda());
		}
		String noAgenda = klasifikasiSuratKeluar.getNomorAgenda().format(index, tanggal);

		try {
			noAgenda = org.apache.commons.lang3.StringUtils.replace(noAgenda, "KODE_KLASIFIKASI",
					klasifikasiSuratKeluar.getKode());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/SuratKeluarAction.java:1467");
			// TODO: handle exception
		}

		if (tambah && klasifikasiSuratKeluar.getNomorAgenda().getGunakanIndexUrut()) {
			int count = 0;
			try {
				Session session = HibernateUtil.currentNativeSession();

				count = ((Number) session.createCriteria(SuratKeluar.class).add(Restrictions.eq("agenda", noAgenda))
						.setProjection(Projections.rowCount()).uniqueResult()).intValue();

				// session.disconnect();
				if (session.isOpen()) {
					session.disconnect();
					session.close();
				}

			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}
			HibernateUtil.closeSession();

			if (count > 0) {
				NomorSurat.tambahIndexNomorSurat(klasifikasiSuratKeluar.getNomorAgenda());
				noAgenda = generateCodeAgenda(tambah, klasifikasiSuratKeluar, tanggal);
			}
		}

		return noAgenda;
	}

	public static Long getindex(KlasifikasiSuratKeluar klasifikasiSuratKeluar) {
		if (klasifikasiSuratKeluar.getNomorSurat() == null) {
			return 0L;
		}
		Session session = HibernateUtil.currentSession();
		int tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		int bulan = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1;
		Date sekarang = WaktuUtil.getDate();
		Number indexO = (Number) session.createCriteria(SuratKeluar.class)
				.createAlias("klasifikasiSuratKeluar", "klasifikasiSuratKeluar", Criteria.LEFT_JOIN)
				.createAlias("klasifikasiSuratKeluar.nomorSurat", "nomorSurat", Criteria.LEFT_JOIN)

				.add(klasifikasiSuratKeluar.getNomorSurat().getUrutBerdasarkanNomor()
						? Restrictions.eq("klasifikasiSuratKeluar.nomorSurat", klasifikasiSuratKeluar.getNomorSurat())

						: (klasifikasiSuratKeluar.getNomorSurat().getUrutBerdasarkanKelompok()
								&& klasifikasiSuratKeluar.getNomorSurat().getKelompokNomorSurat() != null
										? Restrictions.eq("nomorSurat.kelompokNomorSurat",
												klasifikasiSuratKeluar.getNomorSurat().getKelompokNomorSurat())
										: Restrictions.sqlRestriction("true")))

				.add(klasifikasiSuratKeluar.getNomorSurat().getResetUrutanTiapTahun() ? Restrictions.eq("tahun", tahun)
						: Restrictions.sqlRestriction("true"))

				.add(klasifikasiSuratKeluar.getNomorSurat().getResetUrutanTiapBulan()
						? Restrictions.and(Restrictions.eq("tahun", tahun), Restrictions.eq("bulan", bulan))
						: Restrictions.sqlRestriction("true"))

				.add(klasifikasiSuratKeluar.getNomorSurat().getResetTiap() != null
						&& (Common.dateFormat8.get().format(klasifikasiSuratKeluar.getNomorSurat().getResetTiap())
								.equals(Common.dateFormat8.get().format(sekarang))
								|| klasifikasiSuratKeluar.getNomorSurat().getResetTiap().before(sekarang))
										? Restrictions.ge("tanggal",
												klasifikasiSuratKeluar.getNomorSurat().getResetTiap())
										: Restrictions.sqlRestriction("true"))

				.setProjection(Projections.rowCount()).uniqueResult();

		Long index = indexO == null ? null : indexO.longValue();
		if (index == null) {
			index = 0L;
		}
		return ++index;
	}

	private final EventListener listenerUtama = new EventListener() {

		@Override
		public void onEvent(Event arg0) throws Exception {

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					KlasifikasiSuratKeluar thisKlasifikasiSuratKeluar = suratKeluar != null
							&& suratKeluar.getKlasifikasiSuratKeluar() != null ? suratKeluar.getKlasifikasiSuratKeluar()
									: ((KlasifikasiSuratKeluar) (klasifikasiSuratKeluar == null ? null
											: klasifikasiSuratKeluar.getAttribute("klasifikasiSuratKeluar")));
					if (thisKlasifikasiSuratKeluar != null) {

						parameters = SuratUtil.ubahIsiSuratKeluar(rows, thisKlasifikasiSuratKeluar,
								(Mahasiswa) mahasiswa.getAttribute("mahasiswa"), (Dosen) dosen.getAttribute("dosen"),
								(Pegawai) pegawai.getAttribute("pegawai"), Common.getCurrentUser(), kode.getValue(),
								suratKeluar, groupboxParemeter);

						// sekolah.nipKepalaSekolah = MYCODE Pegawai kepala sekolah (resolve dari CODE tersimpan).
						tambahParamNipKepalaSekolah(parameters, suratKeluar);

						generateReport(parameters, thisKlasifikasiSuratKeluar);

						if (thisKlasifikasiSuratKeluar.getAlurPersetujuanSuratKeluar() != null) {
							alurPersetujuanSuratKeluar.setAttribute("alurPersetujuanSuratKeluar",
									thisKlasifikasiSuratKeluar.getAlurPersetujuanSuratKeluar());
							alurPersetujuanSuratKeluar
									.setValue(thisKlasifikasiSuratKeluar.getAlurPersetujuanSuratKeluar() == null ? ""
											: thisKlasifikasiSuratKeluar.getAlurPersetujuanSuratKeluar().toString());

							alurPersetujuanSuratKeluar.setDisabled(true);
							alurEventListener.onEvent(null);
						}

					}
				}
			});

		}
	};

	/**
	 * Sisipkan parameter <code>sekolah.nipKepalaSekolah</code> ke Map surat.
	 *
	 * <p>Field "Kode Kepala Sekolah" ({@code Sekolah.getNipKepalaSekolah()}) menyimpan CODE Pegawai
	 * kepala sekolah (mis. {@code 21023L0130260122240812}), BUKAN nomor NIP. Parameter surat harus
	 * menampilkan MYCODE Pegawai tersebut (mis. {@code 062137}). Karena itu nilai code di-resolve ke
	 * Pegawai lalu diambil {@code getMycode()}. Kosong bila code kosong / Pegawai tidak ditemukan.
	 * Memakai {@code currentSession()} (tidak ditutup manual); error ditelan agar cetak tetap jalan.</p>
	 */
	private void tambahParamNipKepalaSekolah(Map<String, Object> parameters, SuratKeluar suratKeluar) {
		if (parameters == null || suratKeluar == null || suratKeluar.getSekolah() == null) {
			return;
		}
		String kodeKepsek = suratKeluar.getSekolah().getNipKepalaSekolah();
		String mycodeKepsek = "";
		if (kodeKepsek != null && !kodeKepsek.trim().isEmpty()) {
			try {
				Pegawai pegKepsek = (Pegawai) HibernateUtil.currentSession().createCriteria(Pegawai.class)
						.add(Restrictions.eq("code", kodeKepsek.trim())).setMaxResults(1).uniqueResult();
				if (pegKepsek != null && pegKepsek.getMycode() != null) {
					mycodeKepsek = pegKepsek.getMycode();
				}
			} catch (Exception eKepsek) {
				Common.tampilErrorJikaAdmin(eKepsek);
			}
		}
		parameters.put("sekolah.nipKepalaSekolah", mycodeKepsek);
	}

	private void generateReport(Map<String, Object> parameters, KlasifikasiSuratKeluar thisKlasifikasiSuratKeluar) {
		if (template != null) {
			Common.clear(template);
			PDFMergerUtility ut = new PDFMergerUtility();
			// Kumpulan pendamping HTML tiap bagian (otomatis dibuat di sebelah tiap PDF),
			// nanti disatukan agar pratinjau bisa tampil HTML (mirip PDF) via toggle.
			java.util.List<File> htmlParts = new java.util.ArrayList<File>();

			for (int index = 1; index <= 15; index++) {
				try {
					LampiranLain lampiranLain = LampiranLain.ambil(thisKlasifikasiSuratKeluar.getId(),
							LampiranLain.FILE_JRXML_LAYOUT_SURAT + (index == 1 ? "" : "_" + index));
					if (lampiranLain != null && lampiranLain.getId() != null) {
						try {

							File jrxmlFile = lampiranLain.ambilFile();
							// KE-1/KE-2: sebagian template surat (jrxml diunggah admin per Klasifikasi
							// Surat, BUKAN berkas repo) menulis ekspresi
							// "NIP. "+$P{sekolah.nipKepalaSekolah}.split("/")[1].trim() -- mengasumsikan
							// nilai parameter memuat "/", padahal isinya HANYA mycode Pegawai polos (mis.
							// "062137", lihat tambahParamNipKepalaSekolah di atas) tanpa "/" sama sekali ->
							// ArrayIndexOutOfBoundsException saat fill (laporan GAGAL TOTAL utk lampiran
							// ini). Template LAIN mungkin memakai nilai parameter ini APA ADANYA (tanpa
							// split) sehingga nilainya TAK BOLEH diubah scr global utk semua lampiran --
							// deteksi pola berisiko ini HANYA pada isi berkas jrxml yang sedang diproses,
							// lalu sisipkan "-/" di depan HANYA utk lampiran ini (split("/")[1] tetap
							// menghasilkan nilai/​teks akhir yang sama seperti sebelum diubah).
							Object nipAsli = parameters.get("sekolah.nipKepalaSekolah");
							boolean nipDiubahSementara = false;
							if (jrxmlFile != null && jrxmlFile.exists() && nipAsli instanceof String
									&& !((String) nipAsli).startsWith("-/")) {
								try {
									String isiJrxml = new String(
											java.nio.file.Files.readAllBytes(jrxmlFile.toPath()), "UTF-8");
									if (isiJrxml.indexOf("nipKepalaSekolah}.split(") >= 0) {
										String nilaiAsli = (String) nipAsli;
										parameters.put("sekolah.nipKepalaSekolah",
												nilaiAsli.trim().isEmpty() ? "-/-" : "-/" + nilaiAsli);
										nipDiubahSementara = true;
									}
								} catch (Exception eScan) { ais.common.ErrorAuditUtil.record(eScan, "auto-audit(empty-catch) src/ais/action/master/surat/SuratKeluarAction.java:1657");
									// Gagal baca/cek isi jrxml -- lanjut pakai nilai asli, jangan halangi cetak.
								}
							}

							try {
								File file = Report.generateCompileFileReport(Report.PDF, parameters,
										jrxmlFile.getAbsolutePath(), ais.ui.util.WaktuUtil.getDate(), false);

								if (file != null && file.exists()) {
									ut.addSource(file);
									File sib = new File(file.getAbsolutePath() + ".html");
									if (sib.exists() && sib.length() > 0) {
										htmlParts.add(sib);
									}
								}
							} finally {
								if (nipDiubahSementara) {
									parameters.put("sekolah.nipKepalaSekolah", nipAsli);
								}
							}

						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
						}
					}
				} catch (Exception e) {
					ais.common.Common.tampilErrorJikaAdmin(e);
				}

			}
			try {
				File filePdfBaru = new File(
						Common.ambilREAL_PATH_REPORT() + "/" + Common.getGeneratedBarCode() + ".pdf");
				ut.setDestinationStream(new FileOutputStream(filePdfBaru));
				ut.mergeDocuments();
				// Satukan pendamping HTML semua bagian → sibling utk berkas merge (best-effort).
				try {
					Report.gabungHtmlMandiri(htmlParts, new File(filePdfBaru.getAbsolutePath() + ".html"));
				} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/action/master/surat/SuratKeluarAction.java:1696");
				}
				CommonReport.tampilkanReportPDF(template, filePdfBaru);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}
	}

	private Rows rowsOpsiSuratKeluar;
	private MyDatebox tanggal;
	private Textbox keterangan;
	private Textbox lampiran;
	private Textbox perihal;
	private Textbox kepada;
	private MyGrid gridGambar;
	private Set<JenisJabatan> selectedJenisJabatan = null;
	private Set<JenisJabatan> removedJenisJabatan = null;
	private Vbox vboxAlur;
	private EventListener alurEventListener;
	private MyCheckboxConfig tampilkanPreview;
	private MyCheckboxConfig nomorSuratBolehDibahManual;
	private Textbox usernamePengguna;
	private MyCheckboxConfig broadcast;
	private Groupbox groupboxParemeter = null;
	private Label agenda;
	private JSONObject jenisSurats;
	private DisposisiSop disposisiSop;
	private boolean ubah = true;
	private Row rowssw;
	private Row rowgr;
	private MyTabConfig tabDisposisi;
	private MyTabConfig tabIsi = null;
	private MyTabConfig tabHasilScan = null;
	private Textbox catatanRevisi;
	private Combobox tahunAkademik;
	private Combobox ganjilGenap;
	private MyTabConfig tabHasilScanMasuk;
	private MyGrid gridGambarMasuk;

	@SuppressWarnings("unchecked")
	private Borderlayout initOptional(final SuratKeluar suratKeluar) {
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		boolean opsi_surat_hanya_satu = Common.bolehKonfigurasi("opsi_surat_hanya_satu", Konfigurasi.TIDAK_AKTIF);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");

		if (opsi_surat_hanya_satu) {

			Radiogroup radiogroup = new Radiogroup();
			radiogroup.setParent(center);
			grid.setParent(radiogroup);
		} else {
			grid.setParent(center);
		}
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);

		Session session = HibernateUtil.currentSession();
		List<OpsiSuratKeluar> opsiSuratKeluars = session.createCriteria(OpsiSuratKeluar.class)
				.add(Restrictions.and(
						Restrictions.or(Restrictions.isNull("usernamePengguna"),
								Restrictions.ilike("usernamePengguna", "," + tbmuser.getUserId() + ",",
										MatchMode.ANYWHERE)),
						Restrictions.or(Restrictions.isNull("jenisPengguna"),
								Restrictions.ilike("jenisPengguna", "," + tbmuser.hakAkses().getRoleId() + ",",
										MatchMode.ANYWHERE))))

				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.asc("nama")).list();

		rowsOpsiSuratKeluar = new Rows();
		rowsOpsiSuratKeluar.setParent(grid);

		int index = 0;
		MyFormRow row = new MyFormRow();
		row.setValign("top");
		for (OpsiSuratKeluar opsiSuratKeluar : opsiSuratKeluars) {

			OpsiSuratKeluarValue opsiSuratKeluarValue = null;
			if (suratKeluar.getId() != null) {
				opsiSuratKeluarValue = (OpsiSuratKeluarValue) session.createCriteria(OpsiSuratKeluarValue.class)
						.add(Restrictions.eq("opsiSuratKeluar", opsiSuratKeluar))
						.add(Restrictions.eq("suratKeluar", suratKeluar)).setMaxResults(1).uniqueResult();
			}

			if (index % 3 == 0) {
				row = new MyFormRow();
				row.setParent(rowsOpsiSuratKeluar);
			}

			if (opsi_surat_hanya_satu) {

				final MyRadioConfig checkbox = new MyRadioConfig(opsiSuratKeluar.getNama());
				row.setValign("top");
				row.setAttribute("checkbox_" + index, checkbox);

				checkbox.setAttribute("opsiSuratKeluar", opsiSuratKeluar);
				checkbox.setAttribute("opsiSuratKeluarValue", opsiSuratKeluarValue);

				checkbox.setChecked(opsiSuratKeluarValue != null);

				if (opsiSuratKeluar.getNama() != null && opsiSuratKeluar.getNama().equals("Lain-lain")) {

					Vbox hbox = new Vbox();
					hbox.setParent(row);

					hbox.appendChild(checkbox);

					final Textbox textboxket = new Textbox(
							opsiSuratKeluarValue == null ? "" : opsiSuratKeluarValue.getKeterangan());
					textboxket.setCols(20);
					textboxket.setRows(2);
					hbox.appendChild(textboxket);
					EventListener eventListenerLainlain = new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							textboxket.setVisible(checkbox.isChecked());
						}
					};
					checkbox.setAttribute("textboxket", textboxket);
					checkbox.addEventListener("onClick", eventListenerLainlain);
					try {
						eventListenerLainlain.onEvent(null);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						ais.common.Common.tampilErrorJikaAdmin(e);
					}
				} else {
					row.appendChild(checkbox);

				}

			} else {

				final MyCheckboxConfig checkbox = new MyCheckboxConfig(opsiSuratKeluar.getNama());
				row.setValign("top");
				row.setAttribute("checkbox_" + index, checkbox);

				checkbox.setAttribute("opsiSuratKeluar", opsiSuratKeluar);
				checkbox.setAttribute("opsiSuratKeluarValue", opsiSuratKeluarValue);

				checkbox.setChecked(opsiSuratKeluarValue != null);

				if (opsiSuratKeluar.getNama() != null && opsiSuratKeluar.getNama().equals("Lain-lain")) {

					Vbox hbox = new Vbox();
					hbox.setParent(row);

					hbox.appendChild(checkbox);

					final Textbox textboxket = new Textbox(
							opsiSuratKeluarValue == null ? "" : opsiSuratKeluarValue.getKeterangan());
					textboxket.setCols(20);
					textboxket.setRows(2);
					hbox.appendChild(textboxket);
					EventListener eventListenerLainlain = new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							textboxket.setVisible(checkbox.isChecked());
						}
					};
					checkbox.setAttribute("textboxket", textboxket);
					checkbox.addEventListener("onClick", eventListenerLainlain);
					try {
						eventListenerLainlain.onEvent(null);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						ais.common.Common.tampilErrorJikaAdmin(e);
					}

				} else {
					row.appendChild(checkbox);
				}
			}
			index++;
		}

		return borderlayout;
	}

	@SuppressWarnings("unchecked")
	public static Borderlayout initJenisJabatan(final SuratKeluar suratKeluar, final JSONObject jenisSurats) {
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Tbmuser tbmuser = Common.getCurrentUser();

		Session session = HibernateUtil.currentSession();

		// Pakai-ulang satu AktorLookup utk SEMUA pejabat + ambil sekaligus pejabat yang sudah
		// didisposisi (hindari pemindaian pengguna & COUNT berulang per pejabat → tab "Disposisi
		// ke" tidak lagi lambat saat form dibuka).
		final SopUtil.AktorLookup aktorLookup = new SopUtil.AktorLookup();
		final java.util.Set<String> pejabatSudahDisposisi = new java.util.HashSet<String>();
		if (suratKeluar != null && suratKeluar.getId() != null) {
			List<Object[]> sudahRows = session.createCriteria(AlurPersetujuanSuratKeluarStatus.class)
					.add(Restrictions.isNotNull("kodeUnik")).add(Restrictions.eq("suratKeluar", suratKeluar))
					.createAlias("pejabat", "pjBatch").createAlias("jenisJabatan", "jjBatch")
					.setProjection(Projections.projectionList().add(Projections.property("pjBatch.id"))
							.add(Projections.property("jjBatch.id")))
					.list();
			for (Object[] r : sudahRows) {
				if (r != null && r[0] != null && r[1] != null) {
					pejabatSudahDisposisi.add(((Number) r[0]).longValue() + "_" + ((Number) r[1]).longValue());
				}
			}
		}

		List<String> grups = session.createCriteria(JenisJabatan.class)

				.add(Restrictions.and(
						Restrictions.or(Restrictions.isNull("usernamePengguna"),
								Restrictions.ilike("usernamePengguna", "," + tbmuser.getUserId() + ",",
										MatchMode.ANYWHERE)),
						Restrictions.or(Restrictions.isNull("jenisPengguna"),
								Restrictions.ilike("jenisPengguna", "," + tbmuser.hakAkses().getRoleId() + ",",
										MatchMode.ANYWHERE))))

				.setProjection(Projections.groupProperty("grup")).addOrder(Order.asc("grup"))
				.add(Restrictions.isNotNull("grup"))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();
		if (grups.isEmpty()) {
			grups.add("Pejabat");
		}

		MyGrid gridUtama = new MyGrid();
		gridUtama.setWidth("100%");
		gridUtama.setParent(center);
		gridUtama.setWidth("100%");
		gridUtama.setHeight("100%");

		Rows rowsUtama = new Rows();
		rowsUtama.setParent(gridUtama);

		for (String grup : grups) {

			List<JenisJabatan> jenisJabatans = ConstantValues.simpleList(
					session.createCriteria(JenisJabatan.class)
							.add(Restrictions.and(
									Restrictions.or(Restrictions.isNull("usernamePengguna"),
											Restrictions.ilike("usernamePengguna", "," + tbmuser.getUserId() + ",",
													MatchMode.ANYWHERE)),
									Restrictions.or(Restrictions.isNull("jenisPengguna"),
											Restrictions.ilike("jenisPengguna",
													"," + tbmuser.hakAkses().getRoleId() + ",", MatchMode.ANYWHERE))))
							.addOrder(Order.asc("nomorUrut"))
							.add(Restrictions.or(Restrictions.isNull("grup"), Restrictions.eq("grup", grup)))
							.addOrder(Order.asc("nama"))
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
					JenisJabatan.class);

			if (!jenisJabatans.isEmpty()) {

				MyFormRow rowUtama = new MyFormRow();
				rowUtama.setParent(rowsUtama);

				MyGroupboxStyled myGroupboxStyled = new MyGroupboxStyled();
				myGroupboxStyled.appendChild(new MyCaptionStyled(grup));
				myGroupboxStyled.setParent(rowUtama);

				MyGrid grid = new MyGrid();
				grid.setWidth("100%");
				grid.setParent(myGroupboxStyled);
				grid.setWidth("100%");
				grid.setHeight("100%");

				Columns columns = new Columns();
				columns.setParent(grid);

				MyColumnConfig column = new MyColumnConfig();
				column.setParent(columns);

				column = new MyColumnConfig();
				column.setParent(columns);

				column = new MyColumnConfig();
				column.setParent(columns);

				Rows rowsOpsiSuratKeluar = new Rows();
				rowsOpsiSuratKeluar.setParent(grid);

				int index = 0;
				MyFormRow row = new MyFormRow();
				row.setValign("top");
				for (JenisJabatan jenisJabatan : jenisJabatans) {

					List<Pejabat> pejabats = ConstantValues
							.simpleList(
									session.createCriteria(Pejabat.class)
											.add(Restrictions.eq("jenisJabatan", jenisJabatan)).add(Restrictions
													.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
									Pejabat.class);

					for (final Pejabat pejabat : pejabats) {

						boolean sudahDisposisi = suratKeluar != null && suratKeluar.getId() != null
								&& pejabat.getJenisJabatan() != null && pejabatSudahDisposisi
										.contains(pejabat.getId() + "_" + pejabat.getJenisJabatan().getId());

						if (index % 3 == 0) {
							row = new MyFormRow();
							row.setParent(rowsOpsiSuratKeluar);
						}

						String username = pejabat.getNama();

						if (!pejabat.getUsernamePengguna().isEmpty() || !pejabat.getJenisPengguna().isEmpty()) {
							String u = SopUtil.ambilNama(pejabat.getUsernamePengguna(), pejabat.getJenisPengguna(),
									aktorLookup);
							username += username.isEmpty() ? u : ", " + u;
						}

						final Checkbox checkbox;
						row.appendChild(checkbox = new Checkbox(
								jenisJabatan.getNama() + (username.trim().isEmpty() ? "" : " (" + username + ")")));
						checkbox.setDisabled(sudahDisposisi);
						checkbox.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								Long id = pejabat.getId();
								if (checkbox.isChecked()) {
									jenisSurats.put(id.toString(), id);
								} else {
									jenisSurats.remove(id.toString());
								}

							}
						});

						if (jenisSurats != null) {
							Iterator<String> enumeration = jenisSurats.keys();

							while (enumeration.hasNext()) {
								try {
									Long idJenis = Long.parseLong(enumeration.next());
									if (idJenis.equals(pejabat.getId())) {
										checkbox.setChecked(true);
										break;
									}
								} catch (Exception e) {
									ais.common.Common.tampilErrorJikaAdmin(e);
								}
							}
						}
						index++;
					}
				}
			}
		}
		return borderlayout;
	}

	protected void initDetail(final SuratKeluar suratKeluar, Component component) throws Exception {
		Tabbox tabbox = new Tabbox();
		tabbox.setParent(component);
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		tabIsi = new MyTabConfig("Isi Surat");
		tabIsi.setParent(tabs);

		tabHasilScan = new MyTabConfig("Lampiran Surat");
		tabHasilScan.setParent(tabs);

		tabHasilScanMasuk = new MyTabConfig("Lampiran surat masuk");
		tabHasilScanMasuk.setParent(tabs);

		MyTabConfig tabOpsi = new MyTabConfig("Petunjuk / Opsi Surat Keluar");
		tabOpsi.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.getSiswa() == null);
		tabOpsi.setParent(tabs);

		tabDisposisi = new MyTabConfig("Disposisi ke");
		tabDisposisi.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.getSiswa() == null);
		tabDisposisi.setParent(tabs);

		MyTabConfig tabparameter = new MyTabConfig("Parameter");
		tabparameter.setParent(tabs);
		tabparameter.setVisible(Common.getApakahAdmin());

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanelIsi = new ais.ui.util.MyTabpanel();
		tabpanelIsi.setParent(tabpanels);

		final Tabpanel tabpanelGambar = new ais.ui.util.MyTabpanel();
		tabpanelGambar.setParent(tabpanels);
		tabpanelGambar.appendChild(
				new SuratKeluarPunyaGambarFotoHelper(gridGambar = new MyGrid()).initDetail(suratKeluar, true));

		final Tabpanel tabpanelGambarMasuk = new ais.ui.util.MyTabpanel();
		tabpanelGambarMasuk.setParent(tabpanels);
		tabpanelGambarMasuk.appendChild(new SuratKeluarPunyaSuratMasukHelper(gridGambarMasuk = new MyGrid())
				.initDetail(suratKeluar, true, tipe));

		Tabpanel tabpanelOpsi = new ais.ui.util.MyTabpanel();
		tabpanelOpsi.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.getSiswa() == null);
		tabpanelOpsi.setParent(tabpanels);

		Tabpanel tabpanelDisposisi = new ais.ui.util.MyTabpanel();
		tabpanelDisposisi.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.getSiswa() == null);
		tabpanelDisposisi.setParent(tabpanels);

		Borderlayout layout = new ais.ui.util.MyBorderlayout();
		tabpanelIsi.appendChild(layout);

		North north = new North();
		north.setParent(layout);

		tampilkanPreview = new MyCheckboxConfig("Tampilkan preview isi surat keluar");
		north.appendChild(tampilkanPreview);
		tampilkanPreview.setChecked(suratKeluar.getKlasifikasiSuratKeluar() != null);

		final Center center = new Center();
		center.setParent(layout);

		if (tampilkanPreview.isChecked()) {
			center.appendChild(template = new org.zkoss.zul.Div());
			template.setHeight("95%");
			template.setWidth("100%");
			template.setStyle("overflow:auto;");
		} else {
			template = null;
		}

		tampilkanPreview.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(center);

				if (!chekBiaya()) {
					return;
				}

				if (tampilkanPreview.isChecked()) {
					center.appendChild(template = new org.zkoss.zul.Div());
					template.setHeight("95%");
					template.setWidth("100%");
					template.setStyle("overflow:auto;");
					listenerUtama.onEvent(new Event("", center, suratKeluar));
				} else {
					template = null;
				}
			}
		});

		if (suratKeluar.getId() == null) {
			South south = new South();
			south.setParent(layout);
			south.setHeight("60px");
			MyLabelBoldConfig lbl;
			south.appendChild(lbl = new MyLabelBoldConfig(
					"Catatan : Nomor surat akan tercetak otomatis setelah Anda menekan tombol simpan di form pengisian surat ini."));
			lbl.setStyle("font-size:14px;font-weight: bolder;font-family: Poppins, Helvetica, 'sans-serif';color:red");
		}

		tabpanelOpsi.appendChild(initOptional(suratKeluar));

		tabpanelDisposisi.appendChild(SuratKeluarAction.initJenisJabatan(suratKeluar, jenisSurats));

		groupboxParemeter = new Groupbox();
		groupboxParemeter.appendChild(new Caption("Parameter Surat Keluar"));
		Tabpanel tabpanelParameter = new ais.ui.util.MyTabpanel();
		tabpanelParameter.setVisible(tabparameter.isVisible());
		tabpanelParameter.setParent(tabpanels);
		tabpanelParameter.appendChild(groupboxParemeter);
	}

	private void init(final SuratKeluar suratKeluar) throws Exception {

		Tbmuser tbmuser = Common.getCurrentUser();
		if (suratKeluar.getFakultas() == null && tbmuser.ambilFakultas() != null) {
			suratKeluar.setFakultas(tbmuser.ambilFakultas());
		}

		if (suratKeluar.getSatuanKerja() == null && tbmuser.ambilSatuanKerja() != null) {
			suratKeluar.setSatuanKerja(tbmuser.ambilSatuanKerja());
		}

		if (suratKeluar.getJurusan() == null && tbmuser.ambilJurusan() != null) {
			suratKeluar.setJurusan(tbmuser.ambilJurusan());
		}

		if (suratKeluar.getYayasan() == null && tbmuser.ambilYayasan() != null) {
			suratKeluar.setYayasan(tbmuser.ambilYayasan());
		}

		if (suratKeluar.getSekolah() == null && tbmuser.ambilSekolah() != null) {
			suratKeluar.setSekolah(tbmuser.ambilSekolah());
		}

		addWindow.setTitle(suratKeluar.getId() == null ? "Tambah Surat Keluar" : "Ubah Surat Keluar");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		disposisiSop = null;
		center.appendChild(form(suratKeluar, null, save, null));

		East east = new East();
		east.setWidth("70%");
		east.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(east, true);
		east.setAutoscroll(true);
		initDetail(suratKeluar, east);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);

		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					addWindow.setVisible(false);

					if (eventListener != null) {
						eventListener.onEvent(new Event("", event.getTarget(), suratKeluar));
					}
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

		if (suratKeluar.getAlurDitolak() != null) {
			save.setVisible(true);
			cancel.setLabel("Batal");
		}

		else if (!ubah) {
			save.setVisible(false);
			cancel.setLabel("Tutup");
		}
	}

	@SuppressWarnings("unchecked")
	public void initParameter(Rows rows, KlasifikasiSuratKeluar klasifikasiSuratKeluar) {
		List<Row> myRows = rows.getChildren();
		Session session = HibernateUtil.currentSession();
		List<KlasifikasiSuratKeluarParemeter> klasifikasiSuratKeluarParemeters = ConstantValues.simpleList(
				session.createCriteria(KlasifikasiSuratKeluarParemeter.class).addOrder(Order.asc("nomorUrut"))
						.add(Restrictions.eq("klasifikasiSuratKeluar", klasifikasiSuratKeluar)),
				KlasifikasiSuratKeluarParemeter.class);
		for (final KlasifikasiSuratKeluarParemeter klasifikasiSuratKeluarParemeter : klasifikasiSuratKeluarParemeters) {

			KlasifikasiSuratKeluarParemeterValue klasifikasiSuratKeluarParemeterValue = null;
			if (suratKeluar != null && suratKeluar.getId() != null) {
				klasifikasiSuratKeluarParemeterValue = (KlasifikasiSuratKeluarParemeterValue) session
						.createCriteria(KlasifikasiSuratKeluarParemeterValue.class)
						.add(Restrictions.eq("klasifikasiSuratKeluarParemeter", klasifikasiSuratKeluarParemeter))
						.add(Restrictions.eq("suratKeluar", suratKeluar)).setMaxResults(1).uniqueResult();
			}

			Row row = null;
			for (Row myrow : myRows) {
				if (myrow.getAttribute("klasifikasiSuratKeluarParemeter") != null) {
					KlasifikasiSuratKeluarParemeter myklasifikasiSuratKeluarParemeter = (KlasifikasiSuratKeluarParemeter) myrow
							.getAttribute("klasifikasiSuratKeluarParemeter");
					if (myklasifikasiSuratKeluarParemeter.getId().equals(klasifikasiSuratKeluarParemeter.getId())) {
						row = myrow;
					}
					myrow.setVisible(myklasifikasiSuratKeluarParemeter.getKlasifikasiSuratKeluar().getId()
							.equals(klasifikasiSuratKeluar.getId()));

				}
			}
			if (row == null) {
				row = new MyFormRow();
			} else {
				Common.clear(row);
			}

			row.setVisible(klasifikasiSuratKeluarParemeter.getTampil());
			row.setValign("top");
			row.setAttribute("klasifikasiSuratKeluarParemeter", klasifikasiSuratKeluarParemeter);
			row.setParent(rows);

			MyLabelConfig config = new ais.ui.util.MyLabelConfig(klasifikasiSuratKeluarParemeter.getNama());
			row.appendChild(config);
			config.setVisible(klasifikasiSuratKeluarParemeter.getTampil());

			if (klasifikasiSuratKeluarParemeter.getTipe().equals(String.class.getName())) {
				final Textbox isi;
				row.appendChild(isi = new Textbox(
						klasifikasiSuratKeluarParemeterValue == null ? klasifikasiSuratKeluarParemeter.getNilai()
								: klasifikasiSuratKeluarParemeterValue.getNama()));
				row.setValign("top");
				row.setAttribute("komponen", isi);
				isi.setWidth("90%");
				isi.setRows(2);
				isi.setVisible(klasifikasiSuratKeluarParemeter.getTampil());
				isi.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (suratKeluar != null && suratKeluar.getId() != null) {
							Session session = HibernateUtil.currentSession();
							KlasifikasiSuratKeluarParemeterValue klasifikasiSuratKeluarParemeterValue = (KlasifikasiSuratKeluarParemeterValue) session
									.createCriteria(KlasifikasiSuratKeluarParemeterValue.class)
									.add(Restrictions.eq("klasifikasiSuratKeluarParemeter",
											klasifikasiSuratKeluarParemeter))
									.add(Restrictions.eq("suratKeluar", suratKeluar)).setMaxResults(1).uniqueResult();

							if (klasifikasiSuratKeluarParemeterValue == null) {
								klasifikasiSuratKeluarParemeterValue = new KlasifikasiSuratKeluarParemeterValue();
							}
							klasifikasiSuratKeluarParemeterValue.setNama(isi.getValue().trim());
							klasifikasiSuratKeluarParemeterValue
									.setKlasifikasiSuratKeluarParemeter(klasifikasiSuratKeluarParemeter);
							klasifikasiSuratKeluarParemeterValue.setSuratKeluar(suratKeluar);
							session.saveOrUpdate(klasifikasiSuratKeluarParemeterValue);
						}
						listenerUtama.onEvent(arg0);
					}
				});
			} else if (klasifikasiSuratKeluarParemeter.getTipe().equals(KlasifikasiSuratKeluarParemeter.COMBO)) {

				final Combobox isi;
				row.appendChild(isi = new Combobox());

				for (String s : klasifikasiSuratKeluarParemeter.getPilihan().split(";")) {
					Comboitem comboitem = new Comboitem(s);
					comboitem.setValue(s);
					isi.appendChild(comboitem);
				}
				isi.setReadonly(true);

				Common.selectComboItem(true, isi,
						klasifikasiSuratKeluarParemeterValue == null ? klasifikasiSuratKeluarParemeter.getNilai()
								: klasifikasiSuratKeluarParemeterValue.getNama());

				row.setValign("top");
				row.setAttribute("komponen", isi);
				isi.setWidth("90%");
				isi.setVisible(klasifikasiSuratKeluarParemeter.getTampil());
				isi.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (suratKeluar != null && suratKeluar.getId() != null) {
							Session session = HibernateUtil.currentSession();
							KlasifikasiSuratKeluarParemeterValue klasifikasiSuratKeluarParemeterValue = (KlasifikasiSuratKeluarParemeterValue) session
									.createCriteria(KlasifikasiSuratKeluarParemeterValue.class)
									.add(Restrictions.eq("klasifikasiSuratKeluarParemeter",
											klasifikasiSuratKeluarParemeter))
									.add(Restrictions.eq("suratKeluar", suratKeluar)).setMaxResults(1).uniqueResult();

							if (klasifikasiSuratKeluarParemeterValue == null) {
								klasifikasiSuratKeluarParemeterValue = new KlasifikasiSuratKeluarParemeterValue();
							}
							klasifikasiSuratKeluarParemeterValue.setNama(isi.getValue().trim());
							klasifikasiSuratKeluarParemeterValue
									.setKlasifikasiSuratKeluarParemeter(klasifikasiSuratKeluarParemeter);
							klasifikasiSuratKeluarParemeterValue.setSuratKeluar(suratKeluar);
							session.saveOrUpdate(klasifikasiSuratKeluarParemeterValue);
							session.flush();
						}
						listenerUtama.onEvent(arg0);
					}
				});
			} else if (klasifikasiSuratKeluarParemeter.getTipe().equals(Integer.class.getName())) {
				Integer nilai = 0;
				try {
					nilai = Integer.parseInt(klasifikasiSuratKeluarParemeterValue == null
							? klasifikasiSuratKeluarParemeter.getNilai().trim()
							: klasifikasiSuratKeluarParemeterValue.getNama().trim());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/SuratKeluarAction.java:2414");
					/* Nilai default parameter bisa berupa placeholder ("-", dst);
					 * itu bukan error - cukup pakai 0 tanpa membanjiri log admin. */
				}
				final Intbox isi;
				row.appendChild(isi = new Intbox(nilai));
				row.setValign("top");
				row.setAttribute("komponen", isi);
				isi.setWidth("90%");
				isi.setVisible(klasifikasiSuratKeluarParemeter.getTampil());
				isi.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (suratKeluar != null && suratKeluar.getId() != null) {
							Session session = HibernateUtil.currentSession();
							KlasifikasiSuratKeluarParemeterValue klasifikasiSuratKeluarParemeterValue = (KlasifikasiSuratKeluarParemeterValue) session
									.createCriteria(KlasifikasiSuratKeluarParemeterValue.class)
									.add(Restrictions.eq("klasifikasiSuratKeluarParemeter",
											klasifikasiSuratKeluarParemeter))
									.add(Restrictions.eq("suratKeluar", suratKeluar)).setMaxResults(1).uniqueResult();

							if (klasifikasiSuratKeluarParemeterValue == null) {
								klasifikasiSuratKeluarParemeterValue = new KlasifikasiSuratKeluarParemeterValue();
							}
							klasifikasiSuratKeluarParemeterValue.setNama(isi.getValue() + "");
							klasifikasiSuratKeluarParemeterValue
									.setKlasifikasiSuratKeluarParemeter(klasifikasiSuratKeluarParemeter);
							klasifikasiSuratKeluarParemeterValue.setSuratKeluar(suratKeluar);
							session.saveOrUpdate(klasifikasiSuratKeluarParemeterValue);
						}
						listenerUtama.onEvent(arg0);
					}
				});
			} else if (klasifikasiSuratKeluarParemeter.getTipe().equals(Double.class.getName())) {
				Double nilai = 0.0;
				try {
					nilai = Double.parseDouble(klasifikasiSuratKeluarParemeterValue == null
							? klasifikasiSuratKeluarParemeter.getNilai().trim()
							: klasifikasiSuratKeluarParemeterValue.getNama().trim());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/SuratKeluarAction.java:2454");
					/* Placeholder ("-", dst) bukan error - pakai 0.0 senyap. */
				}
				final Doublebox isi;
				row.appendChild(isi = new Doublebox(nilai));
				row.setValign("top");
				row.setAttribute("komponen", isi);
				isi.setWidth("90%");
				isi.setVisible(klasifikasiSuratKeluarParemeter.getTampil());
				isi.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (suratKeluar != null && suratKeluar.getId() != null) {
							Session session = HibernateUtil.currentSession();
							KlasifikasiSuratKeluarParemeterValue klasifikasiSuratKeluarParemeterValue = (KlasifikasiSuratKeluarParemeterValue) session
									.createCriteria(KlasifikasiSuratKeluarParemeterValue.class)
									.add(Restrictions.eq("klasifikasiSuratKeluarParemeter",
											klasifikasiSuratKeluarParemeter))
									.add(Restrictions.eq("suratKeluar", suratKeluar)).setMaxResults(1).uniqueResult();

							if (klasifikasiSuratKeluarParemeterValue == null) {
								klasifikasiSuratKeluarParemeterValue = new KlasifikasiSuratKeluarParemeterValue();
							}
							klasifikasiSuratKeluarParemeterValue.setNama(isi.getValue() + "");
							klasifikasiSuratKeluarParemeterValue
									.setKlasifikasiSuratKeluarParemeter(klasifikasiSuratKeluarParemeter);
							klasifikasiSuratKeluarParemeterValue.setSuratKeluar(suratKeluar);
							session.saveOrUpdate(klasifikasiSuratKeluarParemeterValue);
						}
						listenerUtama.onEvent(arg0);
					}
				});
			} else if (klasifikasiSuratKeluarParemeter.getTipe().equals(Date.class.getName())) {
				Date nilai = ais.ui.util.WaktuUtil.getDate();
				try {
					nilai = Common.dateFormat2.get().parse(klasifikasiSuratKeluarParemeterValue == null
							? klasifikasiSuratKeluarParemeter.getNilai().trim()
							: klasifikasiSuratKeluarParemeterValue.getNama().trim());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/SuratKeluarAction.java:2493");
					/* Nilai default sering berupa placeholder seperti
					 * "tanggal_sekarang"/"Tanggal Sekarang"/"-" yang memang
					 * berarti "pakai tanggal hari ini" - bukan error. */
				}
				final Datebox isi;
				row.appendChild(isi = new MyDatebox(nilai));
				row.setValign("top");
				row.setAttribute("komponen", isi);
				isi.setWidth("90%");
				isi.setVisible(klasifikasiSuratKeluarParemeter.getTampil());
				isi.setFormat(Common.dateFormat2.get().toPattern());
				isi.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (suratKeluar != null && suratKeluar.getId() != null) {
							Session session = HibernateUtil.currentSession();
							KlasifikasiSuratKeluarParemeterValue klasifikasiSuratKeluarParemeterValue = (KlasifikasiSuratKeluarParemeterValue) session
									.createCriteria(KlasifikasiSuratKeluarParemeterValue.class)
									.add(Restrictions.eq("klasifikasiSuratKeluarParemeter",
											klasifikasiSuratKeluarParemeter))
									.add(Restrictions.eq("suratKeluar", suratKeluar)).setMaxResults(1).uniqueResult();

							if (klasifikasiSuratKeluarParemeterValue == null) {
								klasifikasiSuratKeluarParemeterValue = new KlasifikasiSuratKeluarParemeterValue();
							}
							klasifikasiSuratKeluarParemeterValue.setNama(Common.dateFormat2.get()
									.format(isi.getValue() == null ? ais.ui.util.WaktuUtil.getDate() : isi.getValue()));
							klasifikasiSuratKeluarParemeterValue
									.setKlasifikasiSuratKeluarParemeter(klasifikasiSuratKeluarParemeter);
							klasifikasiSuratKeluarParemeterValue.setSuratKeluar(suratKeluar);
							session.saveOrUpdate(klasifikasiSuratKeluarParemeterValue);
						}
						listenerUtama.onEvent(arg0);
					}
				});
			} else if (klasifikasiSuratKeluarParemeter.getTipe().equals(KlasifikasiSuratKeluarParemeter.GAMBAR)) {
				String nilai = klasifikasiSuratKeluarParemeterValue == null ? klasifikasiSuratKeluarParemeter.getNilai()
						: klasifikasiSuratKeluarParemeterValue.getNama();
				final Hbox gambarHbox = new Hbox();
				gambarHbox.setAttribute("nilai", nilai);
				gambarHbox.setParent(row);
				if (klasifikasiSuratKeluarParemeter.getTampil()) {

					LampiranLain.createDownloadUploadFileLain(gambarHbox, klasifikasiSuratKeluarParemeter.getId(),
							KlasifikasiSuratKeluarParemeter.class.getName(), "Gambar", false, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									LampiranLain lainMahasiswa = (LampiranLain) arg0.getData();
									gambarHbox.setAttribute("nilai", lainMahasiswa.ambilFile());
									gambarHbox.setAttribute("nilai_id", lainMahasiswa.getId());

									if (suratKeluar != null && suratKeluar.getId() != null) {
										Session session = HibernateUtil.currentSession();
										KlasifikasiSuratKeluarParemeterValue klasifikasiSuratKeluarParemeterValue = (KlasifikasiSuratKeluarParemeterValue) session
												.createCriteria(KlasifikasiSuratKeluarParemeterValue.class)
												.add(Restrictions.eq("klasifikasiSuratKeluarParemeter",
														klasifikasiSuratKeluarParemeter))
												.add(Restrictions.eq("suratKeluar", suratKeluar)).setMaxResults(1)
												.uniqueResult();

										if (klasifikasiSuratKeluarParemeterValue == null) {
											klasifikasiSuratKeluarParemeterValue = new KlasifikasiSuratKeluarParemeterValue();
										}
										klasifikasiSuratKeluarParemeterValue
												.setNama(lainMahasiswa.ambilFile().getAbsolutePath());
										klasifikasiSuratKeluarParemeterValue
												.setKlasifikasiSuratKeluarParemeter(klasifikasiSuratKeluarParemeter);
										klasifikasiSuratKeluarParemeterValue.setSuratKeluar(suratKeluar);
										session.saveOrUpdate(klasifikasiSuratKeluarParemeterValue);
									}

									listenerUtama.onEvent(arg0);
								}
							});
				}

			} else if (klasifikasiSuratKeluarParemeter.getTipe().equals(KlasifikasiSuratKeluarParemeter.TEXT)) {
				final String n = klasifikasiSuratKeluarParemeterValue == null
						? klasifikasiSuratKeluarParemeter.getNilai()
						: klasifikasiSuratKeluarParemeterValue.getNama();

				final MyButtonConfig tombol = new MyButtonConfig(
						"Ubah " + klasifikasiSuratKeluarParemeter.getKey().toUpperCase());
				tombol.setWidth("90%");
				tombol.setParent(row);
				tombol.setAttribute("nilai", n);
				tombol.setVisible(klasifikasiSuratKeluarParemeter.getTampil());
				tombol.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						KlasifikasiSuratKeluarParemeterValue klasifikasiSuratKeluarParemeterValue = null;
						String n;
						if (suratKeluar != null && suratKeluar.getId() != null) {
							Session session = HibernateUtil.currentSession();
							klasifikasiSuratKeluarParemeterValue = (KlasifikasiSuratKeluarParemeterValue) session
									.createCriteria(KlasifikasiSuratKeluarParemeterValue.class)
									.add(Restrictions.eq("klasifikasiSuratKeluarParemeter",
											klasifikasiSuratKeluarParemeter))
									.add(Restrictions.eq("suratKeluar", suratKeluar)).setMaxResults(1).uniqueResult();

							if (klasifikasiSuratKeluarParemeterValue == null) {
								klasifikasiSuratKeluarParemeterValue = new KlasifikasiSuratKeluarParemeterValue();
							}
							n = klasifikasiSuratKeluarParemeterValue == null
									? klasifikasiSuratKeluarParemeter.getNilai()
									: klasifikasiSuratKeluarParemeterValue.getNama();
							tombol.setAttribute("nilai", n);
						} else {
							n = tombol.getAttribute("nilai") + "";
						}

						final MyWindow window = new MyWindow(
								"Nilai " + klasifikasiSuratKeluarParemeter.getKey().toUpperCase(), "none", true);
						window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
						window.setHeight("95%");
						window.setWidth("95%");

						Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
						borderlayout.setParent(window);
						Center center = new Center();
						center.setParent(borderlayout);
						ais.ui.util.ZkCompat.setFlex(center, true);

						final MyCkEditor nilai = new MyCkEditor();
						nilai.setValue(n);
						nilai.setParent(center);

						South south = new South();
						ais.ui.util.ZkCompat.setFlex(south, true);
						south.setParent(borderlayout);

						Toolbar toolbar = new Toolbar();
						// toolbar.setHeight("25px");
						toolbar.setParent(south);
						MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
						cancel.setTooltiptext("Tutup");
						cancel.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								window.detach();
							}
						});
						cancel.setParent(toolbar);
						MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
						save.setTooltiptext("Simpan");
						save.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								if (nilai.getValue().trim().equals("")) {
									MyMessageboxConfig.show("Mohon Bapak/Ibu terlebih dahulu mengisi Nilai Parameter. Langkah yang dapat dilakukan: (1) klik kolom Nilai Parameter; (2) isikan nilai yang sesuai; (3) tekan tombol Simpan kembali.", "Peringatan",
											MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
									return;
								}

								if (suratKeluar != null && suratKeluar.getId() != null) {
									Session session = HibernateUtil.currentSession();
									KlasifikasiSuratKeluarParemeterValue klasifikasiSuratKeluarParemeterValue = (KlasifikasiSuratKeluarParemeterValue) session
											.createCriteria(KlasifikasiSuratKeluarParemeterValue.class)
											.add(Restrictions.eq("klasifikasiSuratKeluarParemeter",
													klasifikasiSuratKeluarParemeter))
											.add(Restrictions.eq("suratKeluar", suratKeluar)).setMaxResults(1)
											.uniqueResult();

									if (klasifikasiSuratKeluarParemeterValue == null) {
										klasifikasiSuratKeluarParemeterValue = new KlasifikasiSuratKeluarParemeterValue();
									}

									String s = nilai.getValue().trim();

									klasifikasiSuratKeluarParemeterValue.setNama(s);
									klasifikasiSuratKeluarParemeterValue
											.setKlasifikasiSuratKeluarParemeter(klasifikasiSuratKeluarParemeter);
									klasifikasiSuratKeluarParemeterValue.setSuratKeluar(suratKeluar);
									session.saveOrUpdate(klasifikasiSuratKeluarParemeterValue);
								}

								tombol.setAttribute("nilai", nilai.getValue().trim());

								listenerUtama.onEvent(event);

								window.detach();
							}
						});
						save.setParent(toolbar);

						window.onModal();

					}
				});

			} else if (klasifikasiSuratKeluarParemeter.getTipe().equals(KlasifikasiSuratKeluarParemeter.DAFTAR_PENGGUNA)
					|| klasifikasiSuratKeluarParemeter.getTipe()
							.equals(KlasifikasiSuratKeluarParemeter.DAFTAR_MAHASISWA)
					|| klasifikasiSuratKeluarParemeter.getTipe().equals(KlasifikasiSuratKeluarParemeter.DAFTAR_SISWA)) {

				final Textbox isi;

				final Vbox vbox = new Vbox();
				row.appendChild(vbox);
				vbox.setWidth("100%");

				vbox.appendChild(isi = new Textbox(
						klasifikasiSuratKeluarParemeterValue == null ? klasifikasiSuratKeluarParemeter.getNilai()
								: klasifikasiSuratKeluarParemeterValue.getNama()));
				vbox.setAttribute("nilai", isi.getValue().trim());
				row.setValign("top");
				row.setAttribute("komponen", isi);
				isi.setWidth("90%");
				isi.setRows(3);
				isi.setVisible(klasifikasiSuratKeluarParemeter.getTampil());

				final EventListener eventListenerData = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (suratKeluar != null && suratKeluar.getId() != null) {
							Session session = HibernateUtil.currentSession();
							KlasifikasiSuratKeluarParemeterValue klasifikasiSuratKeluarParemeterValue = (KlasifikasiSuratKeluarParemeterValue) session
									.createCriteria(KlasifikasiSuratKeluarParemeterValue.class)
									.add(Restrictions.eq("klasifikasiSuratKeluarParemeter",
											klasifikasiSuratKeluarParemeter))
									.add(Restrictions.eq("suratKeluar", suratKeluar)).setMaxResults(1).uniqueResult();

							if (klasifikasiSuratKeluarParemeterValue == null) {
								klasifikasiSuratKeluarParemeterValue = new KlasifikasiSuratKeluarParemeterValue();
							}
							klasifikasiSuratKeluarParemeterValue.setNama(isi.getValue().trim() + "");
							klasifikasiSuratKeluarParemeterValue
									.setKlasifikasiSuratKeluarParemeter(klasifikasiSuratKeluarParemeter);
							klasifikasiSuratKeluarParemeterValue.setSuratKeluar(suratKeluar);
							session.saveOrUpdate(klasifikasiSuratKeluarParemeterValue);
						}

						vbox.setAttribute("nilai", isi.getValue().trim());
						listenerUtama.onEvent(arg0);
					}
				};

				isi.addEventListener("onChange", eventListenerData);

				if (klasifikasiSuratKeluarParemeter.getTipe().equals(KlasifikasiSuratKeluarParemeter.DAFTAR_PENGGUNA)) {
					final MyButtonConfig toolbarbutton = new MyButtonConfig("Ambil Pegawai/Guru/Dosen",
							"/img/user_male_add.png");

					toolbarbutton.setVisible(klasifikasiSuratKeluarParemeter.getTampil());
					vbox.appendChild(toolbarbutton);
					toolbarbutton.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							AmbilDataTbmuserBanyak ambil = new AmbilDataTbmuserBanyak(new ArrayList<Tbmuser>());
							ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambil);
							ambil.setEventListener(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									// TODO Auto-generated method stub
									List<Tbmuser> tbmusers = (List<Tbmuser>) arg0.getData();
									if (tbmusers != null && tbmusers.size() != 0) {
										for (Tbmuser tbmuser : tbmusers) {
											isi.setValue(
													isi.getValue() + (isi.getValue().isEmpty() ? tbmuser.getUserId()
															: "," + tbmuser.getUserId()));
										}
									}
									toolbarbutton.setAttribute("nilai", isi.getValue().trim());
									eventListenerData.onEvent(arg0);
								}
							});
							ambil.setWidth("850px");
							ambil.setHeight("97%");
							ambil.setVisible(true);
							ambil.onModal();
						}
					});

					if (pt) {

						final MyButtonConfig toolbarbuttonD = new MyButtonConfig("Ambil Daftar Mahasiswa",
								"/img/user_male_add.png");

						toolbarbuttonD.setVisible(klasifikasiSuratKeluarParemeter.getTampil());
						vbox.appendChild(toolbarbuttonD);
						toolbarbuttonD.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								AmbilDataMahasiswaBanyak ambil = new AmbilDataMahasiswaBanyak(
										new ArrayList<Mahasiswa>());
								ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambil);
								ambil.setEventListener(new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										// TODO Auto-generated method stub
										List<Mahasiswa> mahasiswas = (List<Mahasiswa>) arg0.getData();
										if (mahasiswas != null && mahasiswas.size() != 0) {
											for (Mahasiswa mahasiswa : mahasiswas) {
												isi.setValue(
														isi.getValue() + (isi.getValue().isEmpty() ? mahasiswa.getNim()
																: "," + mahasiswa.getNim()));
											}
										}
										toolbarbuttonD.setAttribute("nilai", isi.getValue().trim());
										eventListenerData.onEvent(arg0);
									}
								});
								ambil.setWidth("850px");
								ambil.setHeight("97%");
								ambil.setVisible(true);
								ambil.onModal();
							}
						});

					}

					if (ya) {
						final MyButtonConfig toolbarbuttonF = new MyButtonConfig("Ambil Daftar Siswa",
								"/img/user_male_add.png");

						toolbarbuttonF.setVisible(klasifikasiSuratKeluarParemeter.getTampil());
						vbox.appendChild(toolbarbuttonF);
						toolbarbuttonF.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								AmbilDataSiswaBanyak ambil = new AmbilDataSiswaBanyak(new ArrayList<Siswa>());
								ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambil);
								ambil.setEventListener(new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										// TODO Auto-generated method stub
										List<Siswa> siswas = (List<Siswa>) arg0.getData();
										if (siswas != null && siswas.size() != 0) {
											for (Siswa siswa : siswas) {

												String nisn = siswa.getNomorIndukNasional();
												if (nisn == null || nisn.trim().isEmpty()) {
													nisn = siswa.getNomorInduk();
												}

												isi.setValue(isi.getValue()
														+ (isi.getValue().isEmpty() ? nisn : "," + nisn));
											}
										}
										toolbarbuttonF.setAttribute("nilai", isi.getValue().trim());
										eventListenerData.onEvent(arg0);
									}
								});
								ambil.setWidth("850px");
								ambil.setHeight("97%");
								ambil.setVisible(true);
								ambil.onModal();
							}
						});
					}

				} else if (klasifikasiSuratKeluarParemeter.getTipe()
						.equals(KlasifikasiSuratKeluarParemeter.DAFTAR_MAHASISWA)) {
					final MyButtonConfig toolbarbutton = new MyButtonConfig("Ambil Daftar Mahasiswa",
							"/img/user_male_add.png");

					toolbarbutton.setVisible(klasifikasiSuratKeluarParemeter.getTampil());
					vbox.appendChild(toolbarbutton);
					toolbarbutton.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							AmbilDataMahasiswaBanyak ambil = new AmbilDataMahasiswaBanyak(new ArrayList<Mahasiswa>());
							ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambil);
							ambil.setEventListener(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									// TODO Auto-generated method stub
									List<Mahasiswa> mahasiswas = (List<Mahasiswa>) arg0.getData();
									if (mahasiswas != null && mahasiswas.size() != 0) {
										for (Mahasiswa mahasiswa : mahasiswas) {
											isi.setValue(isi.getValue() + (isi.getValue().isEmpty() ? mahasiswa.getNim()
													: "," + mahasiswa.getNim()));
										}
									}
									toolbarbutton.setAttribute("nilai", isi.getValue().trim());
									eventListenerData.onEvent(arg0);
								}
							});
							ambil.setWidth("850px");
							ambil.setHeight("97%");
							ambil.setVisible(true);
							ambil.onModal();
						}
					});

				} else if (klasifikasiSuratKeluarParemeter.getTipe()
						.equals(KlasifikasiSuratKeluarParemeter.DAFTAR_SISWA)) {
					final MyButtonConfig toolbarbutton = new MyButtonConfig("Ambil Daftar Siswa",
							"/img/user_male_add.png");

					toolbarbutton.setVisible(klasifikasiSuratKeluarParemeter.getTampil());
					vbox.appendChild(toolbarbutton);
					toolbarbutton.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							AmbilDataSiswaBanyak ambil = new AmbilDataSiswaBanyak(new ArrayList<Siswa>());
							ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambil);
							ambil.setEventListener(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									// TODO Auto-generated method stub
									List<Siswa> siswas = (List<Siswa>) arg0.getData();
									if (siswas != null && siswas.size() != 0) {
										for (Siswa siswa : siswas) {

											String nisn = siswa.getNomorIndukNasional();
											if (nisn == null || nisn.trim().isEmpty()) {
												nisn = siswa.getNomorInduk();
											}

											isi.setValue(
													isi.getValue() + (isi.getValue().isEmpty() ? nisn : "," + nisn));
										}
									}
									toolbarbutton.setAttribute("nilai", isi.getValue().trim());
									eventListenerData.onEvent(arg0);
								}
							});
							ambil.setWidth("850px");
							ambil.setHeight("97%");
							ambil.setVisible(true);
							ambil.onModal();
						}
					});

				}
			}

			else if (klasifikasiSuratKeluarParemeter.getTipe().equals(KlasifikasiSuratKeluarParemeter.DATA)) {
				final String n = klasifikasiSuratKeluarParemeterValue == null
						? klasifikasiSuratKeluarParemeter.getNilai()
						: klasifikasiSuratKeluarParemeterValue.getNama();

				final MyButtonConfig tombol = new MyButtonConfig(
						"Ubah " + klasifikasiSuratKeluarParemeter.getKey().toUpperCase());
				tombol.setWidth("90%");
				tombol.setParent(row);
				tombol.setAttribute("nilai", n);
				tombol.setVisible(klasifikasiSuratKeluarParemeter.getTampil());
				tombol.addEventListener("onClick", new EventListener() {

					private MyGrid grid = new MyGrid();
					private Rows rows = null;

					private void reloadData(String n) {
						Common.clear(grid);
						String nilai = n;
						String[] rrr = new String[500];
						if (nilai != null && !nilai.trim().isEmpty()) {
							String[] s = StringUtils.split(nilai, "||");
							for (int i = 0; i < s.length; i++) {
								try {
									rrr[i] = s[i];
								} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
							}
						}

						rows = new Rows();
						rows.setParent(grid);
						for (int r = 0; r < 500; r++) {
							MyFormRow row = new MyFormRow();
							row.setValign("top");
							row.setParent(rows);
							String nil = rrr[r];
							String[] val = nil == null || nil.trim().isEmpty() ? new String[15]
									: StringUtils.split(nil, "<->");
							for (int col = 0; col < 15; col++) {
								String v = val.length > col ? val[col] : "";
								Textbox textbox = new Textbox(v);
								textbox.setWidth("90%");
								textbox.setParent(row);
							}
						}
					}

					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								if (onSave(arg0)) {

									final MyWindow window = new MyWindow(
											"Nilai " + klasifikasiSuratKeluarParemeter.getKey().toUpperCase(), "none",
											true);
									window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
									window.setHeight("440px");
									window.setWidth("850px");

									Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
									borderlayout.setParent(window);

									North north = new North();
									north.setParent(borderlayout);

									Hbox hbox = new Hbox();
									final Long ref = suratKeluar.getId();
									LampiranLain.createDownloadUploadFileLain(hbox, ref,
											LampiranLain.FILE_XLS_DATA_SURAT, "Upload *.xlsx", false,
											new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													LampiranLain lainMahasiswa = (LampiranLain) arg0.getData();

													if (lainMahasiswa != null && lainMahasiswa.getId() != null) {
														try {
															Session session = StreamingHibernateUtil.getInstance()
																	.currentSession();

															session.refresh(lainMahasiswa);
															lainMahasiswa.setRef(ref);

															session.getTransaction().begin();
															session.update(lainMahasiswa);
															session.getTransaction().commit();

															StreamingHibernateUtil.getInstance().closeSession();

															if (lainMahasiswa.ambilFile().getAbsolutePath()
																	.toLowerCase().endsWith("xlsx")) {

																XSSFWorkbook workbook = new XSSFWorkbook(
																		lainMahasiswa.ambilFile().getAbsolutePath());
																XSSFSheet sheet = workbook.getSheetAt(0);
																String hasil = "";
																for (int i = 0; i < (sheet.getLastRowNum() + 1); i++) {
																	String ss = "";

																	for (int j = 0; j < sheet.getRow(0)
																			.getLastCellNum(); j++) {
																		String d = Common.getCellContent(
																				Common.getCell(sheet, j, i));
																		ss += ss.isEmpty() ? d : "<->" + d;
																	}
																	hasil += hasil.isEmpty() ? ss : "||" + ss;
																}

																reloadData(hasil);

																if (suratKeluar != null
																		&& suratKeluar.getId() != null) {
																	session = HibernateUtil.currentSession();
																	KlasifikasiSuratKeluarParemeterValue klasifikasiSuratKeluarParemeterValue = (KlasifikasiSuratKeluarParemeterValue) session
																			.createCriteria(
																					KlasifikasiSuratKeluarParemeterValue.class)
																			.add(Restrictions.eq(
																					"klasifikasiSuratKeluarParemeter",
																					klasifikasiSuratKeluarParemeter))
																			.add(Restrictions.eq("suratKeluar",
																					suratKeluar))
																			.setMaxResults(1).uniqueResult();

																	if (klasifikasiSuratKeluarParemeterValue == null) {
																		klasifikasiSuratKeluarParemeterValue = new KlasifikasiSuratKeluarParemeterValue();
																	}
																	klasifikasiSuratKeluarParemeterValue.setNama(hasil);
																	klasifikasiSuratKeluarParemeterValue
																			.setKlasifikasiSuratKeluarParemeter(
																					klasifikasiSuratKeluarParemeter);
																	klasifikasiSuratKeluarParemeterValue
																			.setSuratKeluar(suratKeluar);
																	session.saveOrUpdate(
																			klasifikasiSuratKeluarParemeterValue);
																}
																tombol.setAttribute("nilai", hasil);
																listenerUtama.onEvent(null);
															} else {
																MyMessageboxConfig.show(
																		"Mohon maaf, berkas yang Bapak/Ibu unggah harus berformat Excel Open XML Spreadsheet (xlsx). Langkah yang dapat dilakukan: (1) buka berkas Excel tersebut; (2) pilih menu Save As dan simpan sebagai Excel Open XML Spreadsheet (xlsx); (3) unggah kembali berkas dengan format tersebut.",
																		"Error", MyMessageboxConfig.OK,
																		MyMessageboxConfig.ERROR);
															}

														} catch (Exception e) {
															StreamingHibernateUtil.getInstance().rollbackTransaction();
															Common.tampilErrorJikaAdmin(e);
														}

													}

												}

											});
									hbox.setParent(north);

									Center center = new Center();
									center.setParent(borderlayout);
									ais.ui.util.ZkCompat.setFlex(center, true);

									grid.setParent(center);
									reloadData(n);

									South south = new South();
									ais.ui.util.ZkCompat.setFlex(south, true);
									south.setParent(borderlayout);

									Toolbar toolbar = new Toolbar();
									// toolbar.setHeight("25px");
									toolbar.setParent(south);
									MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal",
											"/img/cancel.gif");
									cancel.setTooltiptext("Tutup");
									cancel.addEventListener("onClick", new EventListener() {
										@Override
										public void onEvent(Event event) throws Exception {
											window.detach();
										}
									});
									cancel.setParent(toolbar);
									MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
									save.setTooltiptext("Simpan");
									save.addEventListener("onClick", new EventListener() {
										@Override
										public void onEvent(Event event) throws Exception {

											String hasil = "";
											for (Object o : rows.getChildren()) {
												if (o instanceof Row) {
													Row row = (Row) o;
													String ss = "";
													for (Object c : row.getChildren()) {
														if (c instanceof Textbox) {
															Textbox d = (Textbox) c;
															ss += ss.isEmpty() ? d.getValue() : "<->" + d.getValue();
														}
													}
													hasil += hasil.isEmpty() ? ss : "||" + ss;
												}
											}

											if (suratKeluar != null && suratKeluar.getId() != null) {
												Session session = HibernateUtil.currentSession();
												KlasifikasiSuratKeluarParemeterValue klasifikasiSuratKeluarParemeterValue = (KlasifikasiSuratKeluarParemeterValue) session
														.createCriteria(KlasifikasiSuratKeluarParemeterValue.class)
														.add(Restrictions.eq("klasifikasiSuratKeluarParemeter",
																klasifikasiSuratKeluarParemeter))
														.add(Restrictions.eq("suratKeluar", suratKeluar))
														.setMaxResults(1).uniqueResult();

												if (klasifikasiSuratKeluarParemeterValue == null) {
													klasifikasiSuratKeluarParemeterValue = new KlasifikasiSuratKeluarParemeterValue();
												}
												klasifikasiSuratKeluarParemeterValue.setNama(hasil);
												klasifikasiSuratKeluarParemeterValue.setKlasifikasiSuratKeluarParemeter(
														klasifikasiSuratKeluarParemeter);
												klasifikasiSuratKeluarParemeterValue.setSuratKeluar(suratKeluar);
												session.saveOrUpdate(klasifikasiSuratKeluarParemeterValue);
											}

											tombol.setAttribute("nilai", hasil);

											listenerUtama.onEvent(event);

											window.detach();
										}
									});
									save.setParent(toolbar);

									window.onModal();
								}

							}
						});

					}

				});
			}

		}

	}

	private void initKelengkapanBerkas(Vbox vbox, AlurPersetujuanSuratKeluar alurPersetujuanSuratKeluar) {
		Common.clear(vbox);
		if (alurPersetujuanSuratKeluar == null || alurPersetujuanSuratKeluar.getId() == null) {
			return;
		}

		final MyGrid subGrid = new MyGrid();
		vbox.appendChild(subGrid);

		Columns subColumns = new Columns();
		subColumns.setParent(subGrid);
		Column c = new Column("Kepada Yth :");
		subColumns.appendChild(c);

		Rows subRows = new Rows();
		subRows.setParent(subGrid);

		MyFormRow subRow = new MyFormRow();
		subRow.setStyle("border:0px;background: transparent;");
		subRow.setParent(subRows);
		subRow.setValign("top");
		Session session = HibernateUtil.currentSession();
		if (alurPersetujuanSuratKeluar.getId() != null) {
			alurPersetujuanSuratKeluar = (AlurPersetujuanSuratKeluar) session
					.createCriteria(AlurPersetujuanSuratKeluar.class)
					.add(Restrictions.idEq(alurPersetujuanSuratKeluar.getId())).uniqueResult();
		}
		selectedJenisJabatan = new HashSet<JenisJabatan>();
		removedJenisJabatan = new HashSet<JenisJabatan>();

		Vbox vboxSkala = new Vbox();
		vboxSkala.setPack("top");
		vboxSkala.setParent(subRow);

		new Label(alurPersetujuanSuratKeluar.getJenisJabatan().getNama()).setParent(vboxSkala);

		TreeMap<String, JenisJabatan> data = new TreeMap<String, JenisJabatan>();
		for (JenisJabatan jenisJabatan : alurPersetujuanSuratKeluar.getJenisJabatans()) {
			data.put(jenisJabatan.getNama(), jenisJabatan);
		}

		for (final JenisJabatan jenisJabatan : data.values()) {

			AlurPersetujuanSuratKeluarStatus alurPersetujuanSuratKeluarStatus = suratKeluar == null
					|| suratKeluar.getId() == null
							? null
							: (AlurPersetujuanSuratKeluarStatus) session
									.createCriteria(AlurPersetujuanSuratKeluarStatus.class)
									.add(Restrictions.isNotNull("kodeUnik"))
									.add(Restrictions.eq("alurPersetujuanSuratKeluar", alurPersetujuanSuratKeluar))
									.add(Restrictions.eq("suratKeluar", suratKeluar))
									.add(Restrictions.eq("jenisJabatan", jenisJabatan)).setMaxResults(1).uniqueResult();

//			System.out.println("alurPersetujuanSuratKeluarStatus => " + alurPersetujuanSuratKeluarStatus);

			if (alurPersetujuanSuratKeluarStatus != null) {
				selectedJenisJabatan.add(jenisJabatan);
				removedJenisJabatan.remove(jenisJabatan);
			}

			final Checkbox checkbox = new Checkbox(jenisJabatan.getNama());
			checkbox.setParent(vboxSkala);
			checkbox.setChecked(alurPersetujuanSuratKeluarStatus != null);
			checkbox.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						selectedJenisJabatan.add(jenisJabatan);
						removedJenisJabatan.remove(jenisJabatan);
					} else {
						selectedJenisJabatan.remove(jenisJabatan);
						removedJenisJabatan.add(jenisJabatan);
					}
				}
			});
		}
		data = null;

		try {
			tabDisposisi.setVisible(!alurPersetujuanSuratKeluar.getHarusMengikutiAlur());
			tabDisposisi.getLinkedPanel().setVisible(!alurPersetujuanSuratKeluar.getHarusMengikutiAlur());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/SuratKeluarAction.java:3267");
			// TODO: handle exception
		}

	}

	private boolean chekBiaya() {

		KlasifikasiSuratKeluar myKlasifikasiSuratKeluar = (KlasifikasiSuratKeluar) (klasifikasiSuratKeluar == null
				? null
				: klasifikasiSuratKeluar.getAttribute("klasifikasiSuratKeluar"));

		Mahasiswa myMahasiswa = mahasiswa != null && mahasiswa.getParent() != null && mahasiswa.getParent().isVisible()
				? (Mahasiswa) mahasiswa.getAttribute("mahasiswa")
				: null;
		if (myKlasifikasiSuratKeluar == null || myMahasiswa == null) {
			return true;
		}

		try {

			if (myMahasiswa != null && !myKlasifikasiSuratKeluar.getKodeItemBiaya().trim().isEmpty()) {

				List<String> warnings = new ArrayList<String>();

				Integer tahunAngkatanMhs = myMahasiswa.getTahunangkatan();
				String semesterMulai = Common.isNowSemensterGanjil(
						tanggal.getValue() == null ? new Date() : tanggal.getValue()) ? Perkuliahan.GANJIL
								: Perkuliahan.GENAP;
				String ta = Common
						.getCurrentTahunAkademik(tanggal.getValue() == null ? new Date() : tanggal.getValue());
				Integer tahun = Integer.parseInt(StringUtils.split(ta, "/")[0]);
				Integer smt = Common.getSemester(tahunAngkatanMhs, semesterMulai,
						myMahasiswa.getPindahKeKampusIniMasukSemester(), tahun, myMahasiswa.getSemesterMulai());

				Session session = HibernateUtil.currentSession();
				for (String kode : myKlasifikasiSuratKeluar.getKodeItemBiaya().trim().split(",")) {
					if (!kode.trim().isEmpty()) {
						ItemBiaya itemBiaya = (ItemBiaya) session.createCriteria(ItemBiaya.class)
								.add(Restrictions.eq("kode", kode.trim())).setMaxResults(1).uniqueResult();
						if (itemBiaya != null) {
							int jumlah = ((Number) session.createCriteria(CicilanPembayaran.class)
									.createAlias("kegiatan", "kegiatan")
									.add(myKlasifikasiSuratKeluar.getSekaliBayar() ? Restrictions.sqlRestriction("true")
											: Restrictions.eq("kegiatan.semster", smt))
									.add(Restrictions.eq("itemBiaya", itemBiaya))
									.add(Restrictions.eq("kegiatan.mahasiswa", myMahasiswa))
									.setProjection(Projections.rowCount()).uniqueResult()).intValue();
							if (jumlah == 0) {
								warnings.add("Mahasiswa dengan NIM " + myMahasiswa.getNim() + " dan nama "
										+ myMahasiswa.getNama() + " belum membayar biaya " + itemBiaya.getKode() + "-"
										+ itemBiaya.getNama()
										+ (myKlasifikasiSuratKeluar.getSekaliBayar() ? "" : " di semester " + smt)
										+ ". Harap menghubungi bagian keuangan untuk melakukan pembayaran.");
								continue;
							}
						}
					}
				}

				if (!warnings.isEmpty()) {
					String w = "";
					for (String s : warnings) {
						w += w.isEmpty() ? s : "\n\n" + s;
					}
					MyMessageboxConfig.show(w, "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}

			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		try {
			if (myMahasiswa != null) {

				String semesterMulai = Common.isNowSemensterGanjil(
						tanggal.getValue() == null ? new Date() : tanggal.getValue()) ? Perkuliahan.GANJIL
								: Perkuliahan.GENAP;
				String ta = Common
						.getCurrentTahunAkademik(tanggal.getValue() == null ? new Date() : tanggal.getValue());
				Integer tahun = Integer.parseInt(StringUtils.split(ta, "/")[0]);
				Integer semesterInt = Common.getSemester(myMahasiswa.getTahunangkatan(), semesterMulai,
						myMahasiswa.getPindahKeKampusIniMasukSemester(), tahun, myMahasiswa.getSemesterMulai());
				if (myKlasifikasiSuratKeluar.getHarusBayarLunasSmtSaatIni()) {
					Integer tahap = null;

					Double harusLunas = 99.0;

					if (ais.common.CommonHelperClass.jenisKegiatansUntukKrs == null) {
						Common.reloadJenisKegiatans();
					}

					PendaftaranCutiMahasiswa pendaftaranCutiMahasiswa = myMahasiswa.ambilCuti(semesterInt, tahap,
							false);
					int countCuti = pendaftaranCutiMahasiswa != null && pendaftaranCutiMahasiswa.getPersetujuan() ? 1
							: 0;

					if (countCuti == 0) {

						// if (tahap == null || tahap.equals(0)) {
						boolean hasil = true;
						if (!Common.checkBaypassStatusPembayaranMahasiswa(semesterInt, tahap, myMahasiswa,
								ais.common.CommonHelperClass.jenisKegiatansUntukKrs)) {
							List<Kegiatan> kegiatanDibayars = myMahasiswa.ambilKegiatans(semesterInt,
									ais.common.CommonHelperClass.jenisKegiatansUntukKrs);
							for (Kegiatan kegiatanDibayar : kegiatanDibayars) {
								hasil &= (kegiatanDibayar != null
										&& kegiatanDibayar.getPersentaseLunas() >= harusLunas);
							}
						}

						if (!hasil) {
							try {
								MyMessageboxConfig.showFormat(
										"Mohon maaf, mahasiswa dengan NIM {V1} atas nama {V2} harus melunasi biaya semester {V3} terlebih dahulu. Langkah yang dapat dilakukan: (1) periksa status pembayaran mahasiswa yang bersangkutan; (2) selesaikan pelunasan biaya semester tersebut; (3) ulangi proses ini setelah pelunasan.",
										"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
										myMahasiswa.getNim(), myMahasiswa.getNama(), semesterInt);
								return false;
							} catch (Exception e) {

								Common.tampilErrorJikaAdmin(e);
							}
						}

					}

				}

				if (myKlasifikasiSuratKeluar.getHarusBayarLunasSmtLalu()) {

					semesterInt = semesterInt - 1;
					if (semesterInt > 0) {
						Integer tahap = null;

						Double harusLunas = 99.0;

						if (ais.common.CommonHelperClass.jenisKegiatansUntukKrs == null) {
							Common.reloadJenisKegiatans();
						}

						PendaftaranCutiMahasiswa pendaftaranCutiMahasiswa = myMahasiswa.ambilCuti(semesterInt, tahap,
								false);
						int countCuti = pendaftaranCutiMahasiswa != null && pendaftaranCutiMahasiswa.getPersetujuan()
								? 1
								: 0;

						if (countCuti == 0) {

							// if (tahap == null || tahap.equals(0)) {
							boolean hasil = true;
							if (!Common.checkBaypassStatusPembayaranMahasiswa(semesterInt, tahap, myMahasiswa,
									ais.common.CommonHelperClass.jenisKegiatansUntukKrs)) {
								List<Kegiatan> kegiatanDibayars = myMahasiswa.ambilKegiatans(semesterInt,
										ais.common.CommonHelperClass.jenisKegiatansUntukKrs);
								for (Kegiatan kegiatanDibayar : kegiatanDibayars) {
									hasil &= (kegiatanDibayar != null
											&& kegiatanDibayar.getPersentaseLunas() >= harusLunas);
								}
							}

							if (!hasil) {
								try {
									MyMessageboxConfig.showFormat(
											"Mohon maaf, mahasiswa dengan NIM {V1} atas nama {V2} harus melunasi biaya semester {V3} terlebih dahulu. Langkah yang dapat dilakukan: (1) periksa status pembayaran mahasiswa yang bersangkutan; (2) selesaikan pelunasan biaya semester tersebut; (3) ulangi proses ini setelah pelunasan.",
											"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
											myMahasiswa.getNim(), myMahasiswa.getNama(), semesterInt);
									return false;
								} catch (Exception e) {

									Common.tampilErrorJikaAdmin(e);
								}
							}
						}
					}

				}

			}

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	public boolean onSave(Event event) throws Exception {

		if (klasifikasiSuratKeluar.getAttribute("klasifikasiSuratKeluar") == null) {
			MyMessageboxConfig.show("Mohon Bapak/Ibu terlebih dahulu memilih Klasifikasi Surat Keluar. Langkah yang dapat dilakukan: (1) buka pilihan Klasifikasi Surat Keluar; (2) pilih klasifikasi yang sesuai; (3) lanjutkan menyimpan surat.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (perihal.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show("Mohon Bapak/Ibu terlebih dahulu mengisi Perihal surat. Langkah yang dapat dilakukan: (1) klik kolom Perihal; (2) isikan perihal surat secara jelas dan ringkas; (3) lanjutkan menyimpan surat.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		KlasifikasiSuratKeluar myKlasifikasiSuratKeluar = (KlasifikasiSuratKeluar) klasifikasiSuratKeluar
				.getAttribute("klasifikasiSuratKeluar");

		if (myKlasifikasiSuratKeluar != null && myKlasifikasiSuratKeluar.getKaitkanDenganSuratLain()
				&& suratSebelumnya.getAttribute("suratKeluar") == null) {
			MyMessageboxConfig.showFormat(
					"Mohon Bapak/Ibu terlebih dahulu memilih {V1}. Langkah yang dapat dilakukan: (1) buka pilihan {V1}; (2) pilih data yang sesuai; (3) lanjutkan menyimpan surat.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
					myKlasifikasiSuratKeluar.getIstilahSuratLain());
			return false;
		}

		Date wkt = WaktuUtil.getDate();
		if (myKlasifikasiSuratKeluar != null && myKlasifikasiSuratKeluar.getBisaDicetakMulai() != null
				&& Double.parseDouble(
						Common.dateFormat84.get().format(myKlasifikasiSuratKeluar.getBisaDicetakMulai())) < Double
								.parseDouble(Common.dateFormat84.get().format(wkt))) {
			MyMessageboxConfig.showFormat(
					"Mohon maaf, Surat {V1} baru dapat dicetak setelah tanggal {V2}. Langkah yang dapat dilakukan: (1) periksa kembali ketentuan waktu pencetakan surat; (2) tunggu hingga tanggal tersebut; (3) lakukan pencetakan setelah waktu yang ditentukan.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
					myKlasifikasiSuratKeluar.getNama(),
					Common.dateFormat61.get().format(myKlasifikasiSuratKeluar.getBisaDicetakMulai()));
			return false;
		}
		if (myKlasifikasiSuratKeluar != null && myKlasifikasiSuratKeluar.getBisaDicetakSampai() != null
				&& Double.parseDouble(
						Common.dateFormat84.get().format(myKlasifikasiSuratKeluar.getBisaDicetakSampai())) > Double
								.parseDouble(Common.dateFormat84.get().format(wkt))) {
			MyMessageboxConfig.showFormat(
					"Mohon maaf, Surat {V1} hanya dapat dicetak sampai dengan tanggal {V2}. Langkah yang dapat dilakukan: (1) periksa kembali ketentuan batas waktu pencetakan surat; (2) pastikan pencetakan dilakukan sebelum tanggal tersebut; (3) hubungi admin apabila memerlukan bantuan.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
					myKlasifikasiSuratKeluar.getNama(),
					Common.dateFormat61.get().format(myKlasifikasiSuratKeluar.getBisaDicetakSampai()));
			return false;
		}

		Mahasiswa myMahasiswa = mahasiswa != null && mahasiswa.getParent() != null && mahasiswa.getParent().isVisible()
				? (Mahasiswa) mahasiswa.getAttribute("mahasiswa")
				: null;
		if (mahasiswa != null && mahasiswa.getParent() != null && mahasiswa.getParent().isVisible()
				&& myMahasiswa == null) {
			MyMessageboxConfig.show(
					"Mohon Bapak/Ibu terlebih dahulu memilih data Mahasiswa. Langkah yang dapat dilakukan: (1) klik kolom Mahasiswa; (2) pilih mahasiswa yang sesuai; (3) lanjutkan menyimpan surat.",
					"Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Dosen myDosen = dosen != null && dosen.getParent() != null && dosen.getParent().isVisible()
				? (Dosen) dosen.getAttribute("dosen")
				: null;
		if (dosen != null && dosen.getParent() != null && dosen.getParent().isVisible() && myDosen == null) {
			MyMessageboxConfig.show(
					"Mohon Bapak/Ibu terlebih dahulu memilih data Dosen. Langkah yang dapat dilakukan: (1) klik kolom Dosen; (2) pilih dosen yang sesuai; (3) lanjutkan menyimpan surat.",
					"Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Siswa mySiswa = siswa != null && siswa.getParent() != null && siswa.getParent().isVisible()
				? (Siswa) siswa.getAttribute("siswa")
				: null;
		if (siswa != null && siswa.getParent() != null && siswa.getParent().isVisible() && mySiswa == null) {
			MyMessageboxConfig.show(
					"Mohon Bapak/Ibu terlebih dahulu memilih data Siswa. Langkah yang dapat dilakukan: (1) klik kolom Siswa; (2) pilih siswa yang sesuai; (3) lanjutkan menyimpan surat.",
					"Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Guru myGuru = guru != null && guru.getParent() != null && guru.getParent().isVisible()
				? (Guru) guru.getAttribute("guru")
				: null;
		if (guru != null && guru.getParent() != null && guru.getParent().isVisible() && myGuru == null) {
			MyMessageboxConfig.show(
					"Mohon Bapak/Ibu terlebih dahulu memilih data Guru. Langkah yang dapat dilakukan: (1) klik kolom Guru; (2) pilih guru yang sesuai; (3) lanjutkan menyimpan surat.",
					"Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (!chekBiaya()) {
			return false;
		}

		if (myKlasifikasiSuratKeluar != null && myMahasiswa != null
				&& myKlasifikasiSuratKeluar.getKlasifikasiSuratKeluarUntuk() != null
				&& myKlasifikasiSuratKeluar.getKlasifikasiSuratKeluarUntuk().getStatusMahasiswa() != null) {
			HistoryStatusMahasiswa historyStatusMahasiswa = ais.action.master.helper.HistoryStatusMahasiswaUtil
					.currentStatus(myMahasiswa);
			if (!historyStatusMahasiswa.getStatusMahasiswa().getId()
					.equals(myKlasifikasiSuratKeluar.getKlasifikasiSuratKeluarUntuk().getStatusMahasiswa().getId())) {
				MyMessageboxConfig.showFormat(
						"Mohon maaf, status mahasiswa untuk surat ini harus \"{V1}\", sedangkan status mahasiswa {V2} saat ini adalah \"{V3}\". Langkah yang dapat dilakukan: (1) periksa kembali status mahasiswa yang bersangkutan; (2) pastikan status telah sesuai dengan ketentuan surat; (3) hubungi admin apabila memerlukan bantuan.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
						myKlasifikasiSuratKeluar.getKlasifikasiSuratKeluarUntuk().getStatusMahasiswa().getNama(),
						myMahasiswa.getNama(), historyStatusMahasiswa.getStatusMahasiswa().getNama());
				return false;
			}
		}

		if (myKlasifikasiSuratKeluar != null && myMahasiswa != null
				&& myKlasifikasiSuratKeluar.getKlasifikasiSuratKeluarUntuk() != null
				&& myKlasifikasiSuratKeluar.getKlasifikasiSuratKeluarUntuk().getStatusAwalMahasiswa() != null) {

			if (!myMahasiswa.getStatusAwalMahasiswa().getId().equals(
					myKlasifikasiSuratKeluar.getKlasifikasiSuratKeluarUntuk().getStatusAwalMahasiswa().getId())) {
				MyMessageboxConfig.showFormat(
						"Mohon maaf, status awal mahasiswa untuk surat ini harus \"{V1}\", sedangkan status awal mahasiswa {V2} saat ini adalah \"{V3}\". Langkah yang dapat dilakukan: (1) periksa kembali status awal mahasiswa yang bersangkutan; (2) pastikan status telah sesuai dengan ketentuan surat; (3) hubungi admin apabila memerlukan bantuan.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
						myKlasifikasiSuratKeluar.getKlasifikasiSuratKeluarUntuk().getStatusMahasiswa().getNama(),
						myMahasiswa.getNama(), myMahasiswa.getStatusAwalMahasiswa().getNama());
				return false;
			}
		}

		List<Row> rowsFotoGambar = gridGambar.getRows().getChildren();
		for (Row row : rowsFotoGambar) {
			FotoGambarSuratKeluar fotoGambarSuratKeluar = (FotoGambarSuratKeluar) row
					.getAttribute("fotoGambarSuratKeluar");
			if (fotoGambarSuratKeluar.getSuratKeluar() == null) {
				MyMessageboxConfig.show(
						"Mohon Bapak/Ibu terlebih dahulu melengkapi Gambar. Langkah yang dapat dilakukan: (1) klik tombol unggah Gambar; (2) pilih berkas gambar yang sesuai; (3) lanjutkan menyimpan surat.",
						"Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		Session session = HibernateUtil.currentSession();
		if (suratKeluar.getId() != null) {
			// KE-FIX (NullPointerException / ObjectNotFoundException): session.load() mengembalikan
			// proxy TANPA query ke DB; bila baris ini sudah dihapus pengguna lain, dereference
			// pertama (getAlurDitolak() di bawah) baru melempar ObjectNotFoundException. Pakai
			// session.get() (langsung query, null bila tak ada) supaya bisa ditangani dgn pesan jelas.
			SuratKeluar suratKeluarTerbaru = (SuratKeluar) session.get(SuratKeluar.class, suratKeluar.getId());
			if (suratKeluarTerbaru == null) {
				MyMessageboxConfig.show(
						"Mohon maaf, data surat ini sudah tidak ditemukan (kemungkinan telah dihapus pihak lain). Silakan muat ulang halaman.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				return false;
			}
			suratKeluar = suratKeluarTerbaru;

		}

		if (suratKeluar.getAlurDitolak() != null && catatanRevisi.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show(
					"Mohon Bapak/Ibu terlebih dahulu mengisi Catatan Revisi Perbaikan karena surat ini sebelumnya ditolak. Langkah yang dapat dilakukan: (1) klik kolom Catatan Revisi Perbaikan; (2) isikan catatan perbaikan secara jelas; (3) lanjutkan menyimpan surat.",
					"Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		String indata = "";
		List<Row> rowsFotoGambarMasuk = gridGambarMasuk.getRows().getChildren();
		for (Row row : rowsFotoGambarMasuk) {
			SuratMasuk suratMasuk = (SuratMasuk) row.getAttribute("suratMasuk");
			if (suratMasuk != null) {
				indata += indata.trim().isEmpty() ? suratMasuk.getId().toString() : "," + suratMasuk.getId();
			}
		}
		suratKeluar.setSuratMasuks(indata);
		suratKeluar.setSuratSebelumnya((SuratKeluar) suratSebelumnya.getAttribute("suratKeluar"));

		suratKeluar.setJurusan(
				(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? null
						: jurusan.getSelectedItem().getValue()));
		suratKeluar.setFakultas(
				(Fakultas) (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? null
						: fakultas.getSelectedItem().getValue()));

		suratKeluar.setPerihal(perihal.getValue());
		suratKeluar.setKepada(kepada.getValue());
		suratKeluar.setAlurPersetujuanSuratKeluar(
				(AlurPersetujuanSuratKeluar) alurPersetujuanSuratKeluar.getAttribute("alurPersetujuanSuratKeluar"));
		suratKeluar.setLampiran(lampiran.getValue());
		// suratKeluar.setSatuanKerja((SatuanKerja)
		// satuanKerja.getAttribute("satuanKerja"));
		suratKeluar.setKode(kode.getValue());
		if (myMahasiswa != null)
			suratKeluar.setMahasiswa(myMahasiswa);

		if (myDosen != null)
			suratKeluar.setDosen(myDosen);

		if (mySiswa != null)
			suratKeluar.setSiswa(mySiswa);

		if (myGuru != null)
			suratKeluar.setGuru(myGuru);

		Pegawai myPegawai = (Pegawai) (pegawai != null && pegawai.getParent() != null && pegawai.getParent().isVisible()
				&& pegawai.getAttribute("pegawai") != null ? pegawai.getAttribute("pegawai") : tbmuser.getPegawai());

		if (myPegawai != null)
			suratKeluar.setPegawai(myPegawai);

		suratKeluar.setTahunAkademik(
				(String) (tahunAkademik.getSelectedItem() == null ? null : tahunAkademik.getSelectedItem().getValue()));
		suratKeluar.setSemester(
				(String) (ganjilGenap.getSelectedItem() == null ? null : ganjilGenap.getSelectedItem().getValue()));

		suratKeluar.setNama(klasifikasiSuratKeluar.getValue());
		suratKeluar.setKlasifikasiSuratKeluar(
				(KlasifikasiSuratKeluar) klasifikasiSuratKeluar.getAttribute("klasifikasiSuratKeluar"));
		suratKeluar.setTanggal(tanggal.getValue());
		suratKeluar.setKeterangan(keterangan.getValue());
		suratKeluar.setUsernamePengguna(usernamePengguna.getValue());
		suratKeluar.setBroadcast(broadcast.isChecked());

		suratKeluar.setYayasan(
				(Yayasan) (yayasan.getSelectedItem() == null ? null : yayasan.getSelectedItem().getValue()));
		suratKeluar.setSekolah(
				(Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue()));

		suratKeluar.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));

		if (disposisiSop != null && disposisiSop.getId() != null) {
			suratKeluar.setDisposisiSop(disposisiSop);
		}
		suratKeluar.setJenisSurats(jenisSurats.toString());
		suratKeluar.setCatatanRevisi(catatanRevisi.getValue());
		suratKeluar.setTipe(tipe);
		if (tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.getSiswa() == null) {
			suratKeluar.setKonseptor(tbmuser);
		}

		if (suratKeluar.getId() != null) {

			if (suratKeluar.getIndex() == null) {
				String noAgenda = generateCode(true);
				kode.setValue(noAgenda);
				suratKeluar.setKode(noAgenda);

				String noAgendaData = generateCodeAgenda(true);
				agenda.setValue(noAgendaData);
				suratKeluar.setAgenda(noAgendaData);

				agenda.getParent().setVisible(suratKeluar.getAgenda() != null);

				Long currentIndex = getindex(
						(KlasifikasiSuratKeluar) klasifikasiSuratKeluar.getAttribute("klasifikasiSuratKeluar"));
				suratKeluar.setIndex(++currentIndex);
			}

			Common.refreshUpdate(session, suratKeluar);
		} else {
			if (nomorSuratBolehDibahManual.isChecked()) {
				String kodeManual = kode.getValue() == null ? "" : kode.getValue().trim();
				if (kodeSudahDipakai(kodeManual, suratKeluar.getId())) {
					MyMessageboxConfig.show(
							"Nomor surat \"" + kodeManual + "\" sudah dipakai surat lain. Gunakan nomor lain, "
									+ "atau jalankan \"Sinkronkan Nomor Surat\" untuk menata ulang penomoran.",
							"Nomor Surat Ganda", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return false;
				}
				suratKeluar.setKode(kodeManual);
			} else if (suratKeluar.getKode() == null) {
				String noAgenda = generateCode(true);
				kode.setValue(noAgenda);
				suratKeluar.setKode(noAgenda);
			}

			if (suratKeluar.getAgenda() == null) {
				String noAgendaData = generateCodeAgenda(true);
				agenda.setValue(noAgendaData);
				suratKeluar.setAgenda(noAgendaData);
			}

			agenda.getParent().setVisible(suratKeluar.getAgenda() != null);

			Long currentIndex = getindex(
					(KlasifikasiSuratKeluar) klasifikasiSuratKeluar.getAttribute("klasifikasiSuratKeluar"));
			suratKeluar.setIndex(++currentIndex);
			session.save(suratKeluar);

		}
		session.flush();

		AlurPersetujuanSuratKeluarStatus alurDitolak = suratKeluar.getAlurDitolak();
		if (alurDitolak != null) {
			session.refresh(alurDitolak);
			alurDitolak.setCatatanRevisi(catatanRevisi.getValue());
			alurDitolak.setTelahDirevisi(true);
			alurDitolak.setDitolak(false);
			alurDitolak.setDisetujui(false);
			Common.refreshUpdate(session, alurDitolak);
			session.flush();
		}

		if (suratKeluar.getAlurPersetujuanSuratKeluar() != null
				&& suratKeluar.getAlurPersetujuanSuratKeluar().getJenisJabatan() != null) {
			selectedJenisJabatan.add(suratKeluar.getAlurPersetujuanSuratKeluar().getJenisJabatan());
		}

//		if (suratKeluar.getBroadcast()) {
//
//			for (Object o : ConstantValues.ambilBerdasarClass(Tbmuser.class).values()) {
//				Tbmuser tbmuser = (Tbmuser) o;
//				if (tbmuser.getUserId() != null && tbmuser.getEmail() != null && !tbmuser.getEmail().isEmpty()
//						&& (tbmuser.getPegawai() != null || tbmuser.getGuru() != null || tbmuser.getDosen() != null)
//						&& suratKeluar.getUsernamePengguna().contains("," + tbmuser.getUserId() + ",")) {
//
//					Pejabat pejabat = (Pejabat) ConstantValues.simpleObject(session.createCriteria(Pejabat.class).add(
//
//							Restrictions.or(Restrictions.eq("guru", tbmuser.getGuru()),
//									Restrictions.or(Restrictions.eq("pegawai", tbmuser.getPegawai()),
//											Restrictions.eq("dosen", tbmuser.getDosen()))
//
//							)).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setMaxResults(1), Pejabat.class);
//
//					if (pejabat == null && tbmuser.getGuru() != null) {
//						JenisJabatan jenisJabatan = (JenisJabatan) ConstantValues
//								.simpleObject(session.createCriteria(JenisJabatan.class).add(
//
//										Restrictions.ilike("nama", "Guru")
//
//								).setMaxResults(1), JenisJabatan.class);
//						if (jenisJabatan == null) {
//							jenisJabatan = new JenisJabatan();
//							jenisJabatan.setNama("Guru");
//							jenisJabatan.setGrup("Guru");
//							session.save(jenisJabatan);
//							session.flush();
//						}
//
//						pejabat = new Pejabat();
//						pejabat.setAktif(true);
//						pejabat.setGuru(tbmuser.getGuru());
//						pejabat.setPegawai(tbmuser.getPegawai());
//						pejabat.setJenisJabatan(jenisJabatan);
//						session.save(pejabat);
//						session.flush();
//					}
//
//					else if (pejabat == null && tbmuser.getDosen() != null) {
//						JenisJabatan jenisJabatan = (JenisJabatan) ConstantValues
//								.simpleObject(session.createCriteria(JenisJabatan.class).add(
//
//										Restrictions.ilike("nama", "Dosen")
//
//								).setMaxResults(1), JenisJabatan.class);
//						if (jenisJabatan == null) {
//							jenisJabatan = new JenisJabatan();
//							jenisJabatan.setNama("Dosen");
//							jenisJabatan.setGrup("Dosen");
//							session.save(jenisJabatan);
//							session.flush();
//						}
//
//						pejabat = new Pejabat();
//						pejabat.setAktif(true);
//						pejabat.setDosen(tbmuser.getDosen());
//						pejabat.setPegawai(tbmuser.getPegawai());
//						pejabat.setJenisJabatan(jenisJabatan);
//						session.save(pejabat);
//						session.flush();
//					} else if (pejabat == null && tbmuser.getPegawai() != null) {
//						JenisJabatan jenisJabatan = (JenisJabatan) ConstantValues
//								.simpleObject(session.createCriteria(JenisJabatan.class).add(
//
//										Restrictions.ilike("nama", "Pejabat")
//
//								).setMaxResults(1), JenisJabatan.class);
//						if (jenisJabatan == null) {
//							jenisJabatan = new JenisJabatan();
//							jenisJabatan.setNama("Pejabat");
//							jenisJabatan.setGrup("Pejabat");
//							session.save(jenisJabatan);
//							session.flush();
//						}
//
//						pejabat = new Pejabat();
//						pejabat.setAktif(true);
//						pejabat.setPegawai(tbmuser.getPegawai());
//						pejabat.setJenisJabatan(jenisJabatan);
//						session.save(pejabat);
//						session.flush();
//					}
//
//					if (pejabat != null) {
//
//						AlurPersetujuanSuratKeluarStatus alurPersetujuanSuratKeluarStatus = (AlurPersetujuanSuratKeluarStatus) session
//								.createCriteria(AlurPersetujuanSuratKeluarStatus.class)
//								.add(Restrictions.isNotNull("kodeUnik"))
//								.add(Restrictions.eq("suratKeluar", suratKeluar))
//								.add(Restrictions.eq("pejabat", pejabat))
//								.add(Restrictions.eq("jenisJabatan", pejabat.getJenisJabatan())).setMaxResults(1)
//								.uniqueResult();
//
//						if (alurPersetujuanSuratKeluarStatus == null) {
//							String kodeUnik = AlurPersetujuanSuratKeluarStatus.kodeUnik(pejabat, suratKeluar,
//									pejabat.getJenisJabatan(), tbmuser, tbmuser == null ? null : tbmuser.getMahasiswa(),
//									tbmuser == null ? null : tbmuser.getSiswa());
//							if (kodeUnik != null) {
//								alurPersetujuanSuratKeluarStatus = (AlurPersetujuanSuratKeluarStatus) session
//										.createCriteria(AlurPersetujuanSuratKeluarStatus.class)
//										.add(Restrictions.isNotNull("kodeUnik"))
//										.add(Restrictions.eq("kodeUnik", kodeUnik)).setMaxResults(1).uniqueResult();
//							}
//						}
//
//						if (alurPersetujuanSuratKeluarStatus == null) {
//							alurPersetujuanSuratKeluarStatus = new AlurPersetujuanSuratKeluarStatus();
//
//							alurPersetujuanSuratKeluarStatus.setKonseptor(tbmuser);
//							alurPersetujuanSuratKeluarStatus
//									.setMahasiswa(tbmuser == null ? null : tbmuser.getMahasiswa());
//							alurPersetujuanSuratKeluarStatus.setSiswa(tbmuser == null ? null : tbmuser.getSiswa());
//
//							alurPersetujuanSuratKeluarStatus.setMasihLanjut(pejabat.getJenisJabatan() != null
//									&& suratKeluar.getAlurPersetujuanSuratKeluar() != null
//									&& suratKeluar.getAlurPersetujuanSuratKeluar().getJenisJabatan() != null
//									&& suratKeluar.getAlurPersetujuanSuratKeluar().getJenisJabatan().getId()
//											.equals(pejabat.getJenisJabatan().getId()));
//
//							alurPersetujuanSuratKeluarStatus
//									.setAlurPersetujuanSuratKeluar(suratKeluar.getAlurPersetujuanSuratKeluar());
//							alurPersetujuanSuratKeluarStatus.setSuratKeluar(suratKeluar);
//							alurPersetujuanSuratKeluarStatus.setJenisJabatan(pejabat.getJenisJabatan());
//							alurPersetujuanSuratKeluarStatus.setPejabat(pejabat);
//							alurPersetujuanSuratKeluarStatus.setJenisSurats(jenisSurats.toString());
//							session.save(alurPersetujuanSuratKeluarStatus);
//							session.flush();
//						}
//
//					}
//
//				}
//			}
//
//		}

		Iterator<String> enumeration = jenisSurats.keys();
		while (enumeration.hasNext()) {
			try {
				Long idJenis = Long.parseLong(enumeration.next());
				if (idJenis != null && !idJenis.equals(-1L)) {
					Pejabat pejabat = (Pejabat) ConstantValues.ambil(Pejabat.class.getName(), idJenis);
					if (pejabat != null) {
						AlurPersetujuanSuratKeluarStatus alurPersetujuanSuratKeluarStatus = (AlurPersetujuanSuratKeluarStatus) session
								.createCriteria(AlurPersetujuanSuratKeluarStatus.class)
								.add(Restrictions.isNotNull("kodeUnik"))
								.add(Restrictions.eq("suratKeluar", suratKeluar))
								.add(Restrictions.eq("pejabat", pejabat))
								.add(Restrictions.eq("jenisJabatan", pejabat.getJenisJabatan())).setMaxResults(1)
								.uniqueResult();

						if (alurPersetujuanSuratKeluarStatus == null) {
							String kodeUnik = AlurPersetujuanSuratKeluarStatus.kodeUnik(pejabat, suratKeluar,
									pejabat.getJenisJabatan(), tbmuser, tbmuser == null ? null : tbmuser.getMahasiswa(),
									tbmuser == null ? null : tbmuser.getSiswa());
							if (kodeUnik != null) {
								alurPersetujuanSuratKeluarStatus = (AlurPersetujuanSuratKeluarStatus) session
										.createCriteria(AlurPersetujuanSuratKeluarStatus.class)
										.add(Restrictions.isNotNull("kodeUnik"))
										.add(Restrictions.eq("kodeUnik", kodeUnik)).setMaxResults(1).uniqueResult();
							}
						}

						if (alurPersetujuanSuratKeluarStatus == null) {
							alurPersetujuanSuratKeluarStatus = new AlurPersetujuanSuratKeluarStatus();

							alurPersetujuanSuratKeluarStatus.setKonseptor(tbmuser);
							alurPersetujuanSuratKeluarStatus
									.setMahasiswa(tbmuser == null ? null : tbmuser.getMahasiswa());
							alurPersetujuanSuratKeluarStatus.setSiswa(tbmuser == null ? null : tbmuser.getSiswa());

							alurPersetujuanSuratKeluarStatus.setMasihLanjut(pejabat.getJenisJabatan() != null
									&& suratKeluar.getAlurPersetujuanSuratKeluar() != null
									&& suratKeluar.getAlurPersetujuanSuratKeluar().getJenisJabatan() != null
									&& suratKeluar.getAlurPersetujuanSuratKeluar().getJenisJabatan().getId()
											.equals(pejabat.getJenisJabatan().getId()));

							alurPersetujuanSuratKeluarStatus
									.setAlurPersetujuanSuratKeluar(suratKeluar.getAlurPersetujuanSuratKeluar());
							alurPersetujuanSuratKeluarStatus.setSuratKeluar(suratKeluar);
							alurPersetujuanSuratKeluarStatus.setJenisJabatan(pejabat.getJenisJabatan());
							alurPersetujuanSuratKeluarStatus.setPejabat(pejabat);
							alurPersetujuanSuratKeluarStatus.setJenisSurats(jenisSurats.toString());
							session.save(alurPersetujuanSuratKeluarStatus);
							session.flush();
						}
					}

				}
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}
		}

		if (selectedJenisJabatan != null && suratKeluar.getAlurPersetujuanSuratKeluar() != null) {

			if (suratKeluar.getAlurPersetujuanSuratKeluar().getJenisJabatan() != null) {
				selectedJenisJabatan.add(suratKeluar.getAlurPersetujuanSuratKeluar().getJenisJabatan());
			}

			for (JenisJabatan jenisJabatan : selectedJenisJabatan) {

				List<Pejabat> pejabats = ConstantValues.simpleList(session.createCriteria(Pejabat.class)

						.add(Restrictions.or(
								Restrictions.or(
										Restrictions.ilike("jenisPengguna", "," + tbmuser.hakAkses().getRoleId() + ",",
												MatchMode.ANYWHERE),
										Restrictions.ilike("usernamePengguna", "," + tbmuser.getUserId() + ",",
												MatchMode.ANYWHERE)),
								Restrictions.and(
										Restrictions.or(Restrictions.isNotNull("pegawai"),
												Restrictions.or(Restrictions.isNotNull("guru"),
														Restrictions.isNotNull("dosen"))),
										Restrictions.or(Restrictions.eq("pegawai", tbmuser.getPegawai()),
												Restrictions.or(Restrictions.eq("dosen", tbmuser.getDosen()),
														Restrictions.eq("guru", tbmuser.getGuru()))))))

						.add(Restrictions.eq("jenisJabatan", jenisJabatan))
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
						Pejabat.class);

				if (pejabats.isEmpty()) {
					pejabats = ConstantValues
							.simpleList(
									session.createCriteria(Pejabat.class)
											.add(Restrictions.eq("jenisJabatan", jenisJabatan)).add(Restrictions
													.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
									Pejabat.class);
				}

				for (Pejabat pejabat : pejabats) {
					AlurPersetujuanSuratKeluarStatus alurPersetujuanSuratKeluarStatus = (AlurPersetujuanSuratKeluarStatus) session
							.createCriteria(AlurPersetujuanSuratKeluarStatus.class)
							.add(Restrictions.isNotNull("kodeUnik"))
							.add(Restrictions.eq("alurPersetujuanSuratKeluar",
									suratKeluar.getAlurPersetujuanSuratKeluar()))
							.add(Restrictions.eq("suratKeluar", suratKeluar)).add(Restrictions.eq("pejabat", pejabat))
							.add(Restrictions.eq("jenisJabatan", jenisJabatan)).setMaxResults(1).uniqueResult();

					if (alurPersetujuanSuratKeluarStatus == null) {
						String kodeUnik = AlurPersetujuanSuratKeluarStatus.kodeUnik(pejabat, suratKeluar, jenisJabatan,
								tbmuser, tbmuser == null ? null : tbmuser.getMahasiswa(),
								tbmuser == null ? null : tbmuser.getSiswa());
						if (kodeUnik != null) {
							alurPersetujuanSuratKeluarStatus = (AlurPersetujuanSuratKeluarStatus) session
									.createCriteria(AlurPersetujuanSuratKeluarStatus.class)
									.add(Restrictions.isNotNull("kodeUnik")).add(Restrictions.eq("kodeUnik", kodeUnik))
									.setMaxResults(1).uniqueResult();
						}
					}

					if (alurPersetujuanSuratKeluarStatus == null) {
						alurPersetujuanSuratKeluarStatus = new AlurPersetujuanSuratKeluarStatus();
						alurPersetujuanSuratKeluarStatus.setKonseptor(tbmuser);
						alurPersetujuanSuratKeluarStatus.setMahasiswa(tbmuser == null ? null : tbmuser.getMahasiswa());
						alurPersetujuanSuratKeluarStatus.setSiswa(tbmuser == null ? null : tbmuser.getSiswa());
						alurPersetujuanSuratKeluarStatus
								.setMasihLanjut(suratKeluar.getAlurPersetujuanSuratKeluar() != null
										&& suratKeluar.getAlurPersetujuanSuratKeluar().getJenisJabatan() != null
										&& suratKeluar.getAlurPersetujuanSuratKeluar().getJenisJabatan().getId()
												.equals(jenisJabatan.getId()));

						alurPersetujuanSuratKeluarStatus
								.setAlurPersetujuanSuratKeluar(suratKeluar.getAlurPersetujuanSuratKeluar());
						alurPersetujuanSuratKeluarStatus.setSuratKeluar(suratKeluar);
						alurPersetujuanSuratKeluarStatus.setJenisJabatan(jenisJabatan);
						alurPersetujuanSuratKeluarStatus.setPejabat(pejabat);
						session.save(alurPersetujuanSuratKeluarStatus);
						session.flush();
					}
				}
			}
		}

		if (removedJenisJabatan != null) {
			for (JenisJabatan jenisJabatan : removedJenisJabatan) {
				AlurPersetujuanSuratKeluarStatus alurPersetujuanSuratKeluarStatus = (AlurPersetujuanSuratKeluarStatus) session
						.createCriteria(AlurPersetujuanSuratKeluarStatus.class).add(Restrictions.isNotNull("kodeUnik"))
						.add(Restrictions.eq("alurPersetujuanSuratKeluar", suratKeluar.getAlurPersetujuanSuratKeluar()))
						.add(Restrictions.eq("suratKeluar", suratKeluar))
						.add(Restrictions.eq("jenisJabatan", jenisJabatan)).setMaxResults(1).uniqueResult();
				if (alurPersetujuanSuratKeluarStatus != null) {
					session.delete(alurPersetujuanSuratKeluarStatus);
					session.flush();
				}

			}
		}

		List<Row> rowsOpsi = rowsOpsiSuratKeluar.getChildren();
		for (Row row : rowsOpsi) {
			Set<String> keys = row.getAttributes().keySet();
			for (String key : keys) {
				if (key.contains("checkbox")) {

					Checkbox checkbox = (Checkbox) row.getAttribute(key);
					OpsiSuratKeluar opsiSuratKeluar = (OpsiSuratKeluar) checkbox.getAttribute("opsiSuratKeluar");
					OpsiSuratKeluarValue opsiSuratKeluarValue = (OpsiSuratKeluarValue) checkbox
							.getAttribute("opsiSuratKeluarValue");

					if (checkbox.isChecked()) {
						if (opsiSuratKeluarValue == null) {
							opsiSuratKeluarValue = new OpsiSuratKeluarValue();
						}

						Textbox textboxket = (Textbox) checkbox.getAttribute("textboxket");
						opsiSuratKeluarValue.setKeterangan(
								textboxket == null || textboxket.getValue().trim().isEmpty() ? opsiSuratKeluar.getNama()
										: textboxket.getValue());
						opsiSuratKeluarValue.setNama(
								textboxket == null || textboxket.getValue().trim().isEmpty() ? opsiSuratKeluar.getNama()
										: textboxket.getValue());
						opsiSuratKeluarValue.setSuratKeluar(suratKeluar);
						opsiSuratKeluarValue.setOpsiSuratKeluar(opsiSuratKeluar);
						session.saveOrUpdate(opsiSuratKeluarValue);
					}

					if (!checkbox.isChecked() && opsiSuratKeluarValue != null) {
						session.delete(opsiSuratKeluarValue);
					}
				}
			}
		}

		Session mysession = StreamingHibernateUtil.getInstance().currentSession();
		try {
			mysession.getTransaction().begin();
			for (Row row : rowsFotoGambar) {
				FotoGambarSuratKeluar fotoGambarSuratKeluar = (FotoGambarSuratKeluar) row
						.getAttribute("fotoGambarSuratKeluar");
				if (fotoGambarSuratKeluar.getId() == null || fotoGambarSuratKeluar.getSuratKeluar() == null
						|| !fotoGambarSuratKeluar.getSuratKeluar().equals(suratKeluar.getId())) {
					fotoGambarSuratKeluar.setSuratKeluar(suratKeluar.getId());
					mysession.saveOrUpdate(fotoGambarSuratKeluar);
					System.out.println("Simpan lampiran " + fotoGambarSuratKeluar.getNama());
				}
			}
			mysession.getTransaction().commit();
		} catch (Exception e) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			Common.tampilErrorJikaAdmin(e);
		}

		StreamingHibernateUtil.getInstance().closeSession();

		if (rows != null) {
			List<Row> myRows = rows.getChildren();

			for (Row row : myRows) {
				KlasifikasiSuratKeluarParemeter klasifikasiSuratKeluarParemeter = (KlasifikasiSuratKeluarParemeter) row
						.getAttribute("klasifikasiSuratKeluarParemeter");
				if (klasifikasiSuratKeluarParemeter == null || !row.isVisible()) {
					continue;
				}

				try {
					Object coponent = row.getChildren().get(1);

					if (coponent instanceof Textbox) {
						Textbox isi = (Textbox) coponent;
						KlasifikasiSuratKeluarParemeterValue klasifikasiSuratKeluarParemeterValue = (KlasifikasiSuratKeluarParemeterValue) session
								.createCriteria(KlasifikasiSuratKeluarParemeterValue.class)
								.add(Restrictions.eq("klasifikasiSuratKeluarParemeter",
										klasifikasiSuratKeluarParemeter))
								.add(Restrictions.eq("suratKeluar", suratKeluar)).setMaxResults(1).uniqueResult();

						if (klasifikasiSuratKeluarParemeterValue == null) {
							klasifikasiSuratKeluarParemeterValue = new KlasifikasiSuratKeluarParemeterValue();
						}
						klasifikasiSuratKeluarParemeterValue.setNama(isi.getValue().trim());
						klasifikasiSuratKeluarParemeterValue
								.setKlasifikasiSuratKeluarParemeter(klasifikasiSuratKeluarParemeter);
						klasifikasiSuratKeluarParemeterValue.setSuratKeluar(suratKeluar);
						session.saveOrUpdate(klasifikasiSuratKeluarParemeterValue);
					} else if (coponent instanceof Intbox) {
						Intbox isi = (Intbox) coponent;
						KlasifikasiSuratKeluarParemeterValue klasifikasiSuratKeluarParemeterValue = (KlasifikasiSuratKeluarParemeterValue) session
								.createCriteria(KlasifikasiSuratKeluarParemeterValue.class)
								.add(Restrictions.eq("klasifikasiSuratKeluarParemeter",
										klasifikasiSuratKeluarParemeter))
								.add(Restrictions.eq("suratKeluar", suratKeluar)).setMaxResults(1).uniqueResult();

						if (klasifikasiSuratKeluarParemeterValue == null) {
							klasifikasiSuratKeluarParemeterValue = new KlasifikasiSuratKeluarParemeterValue();
						}
						klasifikasiSuratKeluarParemeterValue.setNama(isi.getValue() + "");
						klasifikasiSuratKeluarParemeterValue
								.setKlasifikasiSuratKeluarParemeter(klasifikasiSuratKeluarParemeter);
						klasifikasiSuratKeluarParemeterValue.setSuratKeluar(suratKeluar);
						session.saveOrUpdate(klasifikasiSuratKeluarParemeterValue);
					} else if (coponent instanceof Doublebox) {
						Doublebox isi = (Doublebox) coponent;
						KlasifikasiSuratKeluarParemeterValue klasifikasiSuratKeluarParemeterValue = (KlasifikasiSuratKeluarParemeterValue) session
								.createCriteria(KlasifikasiSuratKeluarParemeterValue.class)
								.add(Restrictions.eq("klasifikasiSuratKeluarParemeter",
										klasifikasiSuratKeluarParemeter))
								.add(Restrictions.eq("suratKeluar", suratKeluar)).setMaxResults(1).uniqueResult();

						if (klasifikasiSuratKeluarParemeterValue == null) {
							klasifikasiSuratKeluarParemeterValue = new KlasifikasiSuratKeluarParemeterValue();
						}
						klasifikasiSuratKeluarParemeterValue.setNama(isi.getValue() + "");
						klasifikasiSuratKeluarParemeterValue
								.setKlasifikasiSuratKeluarParemeter(klasifikasiSuratKeluarParemeter);
						klasifikasiSuratKeluarParemeterValue.setSuratKeluar(suratKeluar);
						session.saveOrUpdate(klasifikasiSuratKeluarParemeterValue);
					} else if (coponent instanceof Datebox) {
						Datebox isi = (Datebox) coponent;
						Date nilaiTanggalIsi;
						try {
							nilaiTanggalIsi = isi.getValue();
						} catch (org.zkoss.zk.ui.WrongValueException wve) {
							/* Teks yang diketik user di Datebox tidak dapat diparse (format tanggal
							 * tidak lengkap/tidak valid) -> getValue() melempar WrongValueException.
							 * Beri tahu user secara ramah, lalu lewati parameter ini (bukan crash). */
							MyMessageboxConfig.show("Mohon Bapak/Ibu memeriksa kembali isian tanggal pada parameter \""
									+ klasifikasiSuratKeluarParemeter.getNama()
									+ "\". Format tanggal yang diisi tidak valid/lengkap. Langkah yang dapat dilakukan: (1) klik kolom tanggal tersebut; (2) pilih ulang tanggal yang benar melalui kalender; (3) lanjutkan menyimpan surat.",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							continue;
						}
						KlasifikasiSuratKeluarParemeterValue klasifikasiSuratKeluarParemeterValue = (KlasifikasiSuratKeluarParemeterValue) session
								.createCriteria(KlasifikasiSuratKeluarParemeterValue.class)
								.add(Restrictions.eq("klasifikasiSuratKeluarParemeter",
										klasifikasiSuratKeluarParemeter))
								.add(Restrictions.eq("suratKeluar", suratKeluar)).setMaxResults(1).uniqueResult();

						if (klasifikasiSuratKeluarParemeterValue == null) {
							klasifikasiSuratKeluarParemeterValue = new KlasifikasiSuratKeluarParemeterValue();
						}
						klasifikasiSuratKeluarParemeterValue.setNama(Common.dateFormat2.get()
								.format(nilaiTanggalIsi == null ? ais.ui.util.WaktuUtil.getDate() : nilaiTanggalIsi));
						klasifikasiSuratKeluarParemeterValue
								.setKlasifikasiSuratKeluarParemeter(klasifikasiSuratKeluarParemeter);
						klasifikasiSuratKeluarParemeterValue.setSuratKeluar(suratKeluar);
						session.saveOrUpdate(klasifikasiSuratKeluarParemeterValue);
					} else if (coponent instanceof Hbox) {
						/* attribute "nilai" bisa berisi java.io.File (parameter
						 * gambar) -> konversi aman, jangan cast langsung. */
						String h = ais.action.master.surat.util.SuratUtilHelper
								.nilaiParameterAman(((Hbox) coponent).getAttribute("nilai"));
						KlasifikasiSuratKeluarParemeterValue klasifikasiSuratKeluarParemeterValue = (KlasifikasiSuratKeluarParemeterValue) session
								.createCriteria(KlasifikasiSuratKeluarParemeterValue.class)
								.add(Restrictions.eq("klasifikasiSuratKeluarParemeter",
										klasifikasiSuratKeluarParemeter))
								.add(Restrictions.eq("suratKeluar", suratKeluar)).setMaxResults(1).uniqueResult();

						if (klasifikasiSuratKeluarParemeterValue == null) {
							klasifikasiSuratKeluarParemeterValue = new KlasifikasiSuratKeluarParemeterValue();
						}

						klasifikasiSuratKeluarParemeterValue.setNama(h);
						klasifikasiSuratKeluarParemeterValue
								.setKlasifikasiSuratKeluarParemeter(klasifikasiSuratKeluarParemeter);
						klasifikasiSuratKeluarParemeterValue.setSuratKeluar(suratKeluar);
						session.saveOrUpdate(klasifikasiSuratKeluarParemeterValue);
					} else if (coponent instanceof Vbox) {
						String h = ais.action.master.surat.util.SuratUtilHelper
								.nilaiParameterAman(((Vbox) coponent).getAttribute("nilai"));
						KlasifikasiSuratKeluarParemeterValue klasifikasiSuratKeluarParemeterValue = (KlasifikasiSuratKeluarParemeterValue) session
								.createCriteria(KlasifikasiSuratKeluarParemeterValue.class)
								.add(Restrictions.eq("klasifikasiSuratKeluarParemeter",
										klasifikasiSuratKeluarParemeter))
								.add(Restrictions.eq("suratKeluar", suratKeluar)).setMaxResults(1).uniqueResult();

						if (klasifikasiSuratKeluarParemeterValue == null) {
							klasifikasiSuratKeluarParemeterValue = new KlasifikasiSuratKeluarParemeterValue();
						}

						klasifikasiSuratKeluarParemeterValue.setNama(h);
						klasifikasiSuratKeluarParemeterValue
								.setKlasifikasiSuratKeluarParemeter(klasifikasiSuratKeluarParemeter);
						klasifikasiSuratKeluarParemeterValue.setSuratKeluar(suratKeluar);
						session.saveOrUpdate(klasifikasiSuratKeluarParemeterValue);
					} else if (coponent instanceof MyButtonConfig) {
						String h = (String) ((MyButtonConfig) coponent).getAttribute("nilai");
						KlasifikasiSuratKeluarParemeterValue klasifikasiSuratKeluarParemeterValue = (KlasifikasiSuratKeluarParemeterValue) session
								.createCriteria(KlasifikasiSuratKeluarParemeterValue.class)
								.add(Restrictions.eq("klasifikasiSuratKeluarParemeter",
										klasifikasiSuratKeluarParemeter))
								.add(Restrictions.eq("suratKeluar", suratKeluar)).setMaxResults(1).uniqueResult();

						if (klasifikasiSuratKeluarParemeterValue == null) {
							klasifikasiSuratKeluarParemeterValue = new KlasifikasiSuratKeluarParemeterValue();
						}
						klasifikasiSuratKeluarParemeterValue.setNama(h);
						klasifikasiSuratKeluarParemeterValue
								.setKlasifikasiSuratKeluarParemeter(klasifikasiSuratKeluarParemeter);
						klasifikasiSuratKeluarParemeterValue.setSuratKeluar(suratKeluar);
						session.saveOrUpdate(klasifikasiSuratKeluarParemeterValue);
					}
				} catch (Exception e) {
					ais.common.Common.tampilErrorJikaAdmin(e);
				}
			}
		}

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				parameters = SuratUtil.ubahIsiSuratKeluar(suratKeluar, groupboxParemeter);

				// sekolah.nipKepalaSekolah = MYCODE Pegawai kepala sekolah (di-resolve dari CODE yang
				// tersimpan di field "Kode Kepala Sekolah"). Disisipkan manual ke Map untuk template jrxml.
				tambahParamNipKepalaSekolah(parameters, suratKeluar);

				suratKeluar.cetak(tbmuser, parameters);

				Common.createDefaultTimerNoBusy((new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						AlurPersetujuanSuratKeluarStatus alurPersetujuanSuratKeluarStatus = checkAlurPersetujuanSuratKeluarStatus(
								parameters, suratKeluar);
						BroadcastHelper.kirimEmailSuratKeluar(suratKeluar, alurPersetujuanSuratKeluarStatus, tbmuser);
					}
				}));

			}
		});

		return true;
	}

	@SuppressWarnings("rawtypes")
	private AlurPersetujuanSuratKeluarStatus checkAlurPersetujuanSuratKeluarStatus(Map parameters,
			SuratKeluar suratKeluar) throws Exception {
		Session session = HibernateUtil.currentSession();
		Integer count = ((Number) session.createCriteria(AlurPersetujuanSuratKeluarStatus.class)
				.add(Restrictions.isNotNull("kodeUnik"))
				.createAlias("alurPersetujuanSuratKeluar", "alurPersetujuanSuratKeluar")
				.add(Restrictions.isNull("alurPersetujuanSuratKeluar.parent"))
				.add(Restrictions.eq("suratKeluar", suratKeluar)).setProjection(Projections.rowCount()).uniqueResult())
				.intValue();

		AlurPersetujuanSuratKeluarStatus alurPersetujuanSuratKeluarStatus = (AlurPersetujuanSuratKeluarStatus) session
				.createCriteria(AlurPersetujuanSuratKeluarStatus.class).add(Restrictions.isNotNull("kodeUnik"))
				.add(Restrictions.eq("suratKeluar", suratKeluar))
				.createAlias("alurPersetujuanSuratKeluar", "alurPersetujuanSuratKeluar")
				.add(Restrictions.isNull("alurPersetujuanSuratKeluar.parent")).setMaxResults(1).uniqueResult();

		if (count.equals(0) && suratKeluar.getAlurPersetujuanSuratKeluar() != null) {

			// Cegah "duplicate key value violates unique constraint kodeunik": untuk surat
			// mahasiswa/siswa getKonseptor() DIPAKSA null (lihat entity), sehingga kodeUnik =
			// "M_/S_<id>_<suratId>" yang SAMA untuk SEMUA level alur (kodeUnik tak memuat level).
			// Insert level ke-2 melanggar unique constraint. Karena itu simpan hanya bila kodeUnik
			// belum ada; bila sudah ada, pakai baris yang sudah tersimpan. Kasus pejabat/jenis
			// jabatan/konseptor (P_/J_/K_) tetap tersimpan penuh karena kodeUnik-nya berbeda.
			java.util.Set<String> kodeUnikTerpakai = new java.util.HashSet<String>();

			AlurPersetujuanSuratKeluar alurPersetujuanSuratKeluar = suratKeluar.getAlurPersetujuanSuratKeluar();

			alurPersetujuanSuratKeluarStatus = new AlurPersetujuanSuratKeluarStatus();
			alurPersetujuanSuratKeluarStatus.setAlurPersetujuanSuratKeluar(alurPersetujuanSuratKeluar);
			alurPersetujuanSuratKeluarStatus.setSuratKeluar(suratKeluar);

			alurPersetujuanSuratKeluarStatus.setKonseptor(tbmuser);
			alurPersetujuanSuratKeluarStatus.setMahasiswa(tbmuser == null ? null : tbmuser.getMahasiswa());
			alurPersetujuanSuratKeluarStatus.setSiswa(tbmuser == null ? null : tbmuser.getSiswa());

			alurPersetujuanSuratKeluarStatus = simpanStatusAlurJikaBelumAda(session,
					alurPersetujuanSuratKeluarStatus, kodeUnikTerpakai);

			while (alurPersetujuanSuratKeluar.getParent() != null) {
				alurPersetujuanSuratKeluar = alurPersetujuanSuratKeluar.getParent();

				AlurPersetujuanSuratKeluarStatus status = new AlurPersetujuanSuratKeluarStatus();
				status.setKonseptor(tbmuser);
				status.setMahasiswa(tbmuser == null ? null : tbmuser.getMahasiswa());
				status.setSiswa(tbmuser == null ? null : tbmuser.getSiswa());
				status.setAlurPersetujuanSuratKeluar(alurPersetujuanSuratKeluar);
				status.setSuratKeluar(suratKeluar);
				simpanStatusAlurJikaBelumAda(session, status, kodeUnikTerpakai);
			}

		}

		if (alurPersetujuanSuratKeluarStatus != null) {
			SuratKeluarAction.cetakDisposisi(parameters, alurPersetujuanSuratKeluarStatus, tbmuser);
		}

		return alurPersetujuanSuratKeluarStatus;
	}

	/**
	 * Simpan satu status alur bila kodeUnik-nya BELUM ada (di proses ini maupun di DB). Mengembalikan
	 * baris yang efektif: yang baru disimpan, atau yang sudah ada bila kodeUnik bentrok. Mencegah
	 * ConstraintViolationException pada unique constraint "kodeunik" untuk surat mahasiswa/siswa yang
	 * alur-nya berlapis (kodeUnik sama di tiap level). Tidak mematikan logika lama: level pertama tetap
	 * tersimpan, dan untuk P_/J_/K_ (kodeUnik unik) semua level tetap tersimpan.
	 */
	private AlurPersetujuanSuratKeluarStatus simpanStatusAlurJikaBelumAda(Session session,
			AlurPersetujuanSuratKeluarStatus status, java.util.Set<String> kodeUnikTerpakai) {
		String kodeUnik = null;
		try {
			kodeUnik = status.getKodeUnik();
		} catch (Exception e) {
			kodeUnik = null;
		}
		if (kodeUnik != null && kodeUnik.trim().length() > 0) {
			if (kodeUnikTerpakai.contains(kodeUnik)) {
				AlurPersetujuanSuratKeluarStatus ada = cariStatusAlurByKodeUnik(session, kodeUnik);
				return ada != null ? ada : status;
			}
			AlurPersetujuanSuratKeluarStatus ada = cariStatusAlurByKodeUnik(session, kodeUnik);
			if (ada != null) {
				kodeUnikTerpakai.add(kodeUnik);
				return ada;
			}
			kodeUnikTerpakai.add(kodeUnik);
		}
		session.save(status);
		return status;
	}

	private AlurPersetujuanSuratKeluarStatus cariStatusAlurByKodeUnik(Session session, String kodeUnik) {
		try {
			return (AlurPersetujuanSuratKeluarStatus) session
					.createCriteria(AlurPersetujuanSuratKeluarStatus.class)
					.add(Restrictions.eq("kodeUnik", kodeUnik)).setMaxResults(1).uniqueResult();
		} catch (Exception e) {
			return null;
		}
	}

	private Checkbox searchaktif;
	private MyLabelConfig istilahSuratLain;
	private AmbilDataSuratKeluarBanbox suratSebelumnya;

	/**
	 * FIX WrongValueException "You must specify a date" (KE-13): initCriteria() ini juga dipanggil
	 * dari Thread latar (CommonDownloadUpload, lihat Common$9.initCriteria) di luar konteks event ZK
	 * normal -- getValue() Datebox melempar exception mentah bila user sempat mengetik teks tanggal
	 * tidak valid, dan di thread latar itu TIDAK tertangkap oleh mekanisme pesan error ZK biasa
	 * sehingga menggagalkan seluruh proses unduhan. Tangkap di sini & anggap filter tanggal kosong
	 * (perilaku sama seperti field yang memang tidak diisi) daripada menggagalkan seluruh pencarian.
	 */
	private java.util.Date ambilTanggalDateboxAman(MyDatebox box) {
		if (box == null) {
			return null;
		}
		try {
			return box.getValue();
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/SuratKeluarAction.java:ambilTanggalDateboxAman");
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public Criteria initCriteria(boolean order) {

		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		KlasifikasiSuratKeluar klasifikasiSuratKeluar = (KlasifikasiSuratKeluar) (searchklasifikasiSuratKeluar != null
				? searchklasifikasiSuratKeluar.getAttribute("klasifikasiSuratKeluar")
				: null);

		Jurusan jurusan = tbmuser.getMahasiswa() != null ? tbmuser.getMahasiswa().getJurusan() : tbmuser.ambilJurusan();
		Fakultas fakultas = tbmuser.getMahasiswa() != null ? tbmuser.getMahasiswa().getJurusan().getFakultas()
				: tbmuser.ambilFakultas();

		Criterion criterion1 = Restrictions.or(
				fakultas == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("klasifikasiSuratKeluar.fakultas", fakultas),
				Restrictions.isNull("klasifikasiSuratKeluar.fakultas"));
		Criterion criterion2 = Restrictions.or(
				jurusan == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("klasifikasiSuratKeluar.jurusan", jurusan),
				Restrictions.isNull("klasifikasiSuratKeluar.jurusan"));

		Criterion criterion = Restrictions.and(criterion1, criterion2);

		Tbmrole tbmrole = tbmuser.hakAkses();

		if (tbmrole != null && tbmrole.getRoleId() != null && tbmrole.getMelihatSemuaSurat()) {

		} else if (tbmrole != null && tbmrole.getRoleId() != null && !tbmrole.getMelihatSemuaSurat()
				&& tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null) {

			boolean ada = false;
			String[] daftarUsernameYgBisaLihatSemua = Common
					.getKonfigurasi("daftar_username_yg_bisa_lihat_semua_surat", "").getNilai().split(",");
			for (String s : daftarUsernameYgBisaLihatSemua) {
				if (tbmuser.getUserId() != null && s.trim().equalsIgnoreCase(tbmuser.getUserId())) {
					ada = true;
				}
			}
			if (!ada) {

				Criterion c = Restrictions.eq("dosen", tbmuser.getDosen());

				c = Restrictions.or(c, Restrictions.eq("guru", tbmuser.getGuru()));

				c = Restrictions.or(c, Restrictions.eq("konseptor", tbmuser));

				c = Restrictions.or(c, Restrictions.ilike("klasifikasiSuratKeluar.kodeGrupPengguna",
						";" + tbmrole.getRoleId() + ";", MatchMode.ANYWHERE));

				criterion = Restrictions.and(criterion, c);
			}
		} else if (tbmuser.getMahasiswa() != null) {
			criterion = Restrictions.and(criterion, Restrictions.eq("mahasiswa", tbmuser.getMahasiswa()));
		} else if (tbmuser.ambilDosen() != null) {
			criterion = Restrictions.and(criterion, Restrictions.eq("dosen", tbmuser.ambilDosen()));
		} else if (tbmuser.getSiswa() != null) {
			criterion = Restrictions.and(criterion, Restrictions.eq("siswa", tbmuser.getSiswa()));
		} else if (tbmuser.ambilGuru() != null) {
			criterion = Restrictions.and(criterion, Restrictions.eq("guru", tbmuser.ambilGuru()));
		}

		Session session = HibernateUtil.currentSession();
		// KE-FIX (HibernateException "createCriteria is not valid without active transaction"):
		// dipanggil dari callback tombol dialog konfirmasi (MessageboxDlg onClick) yang bisa
		// berjalan setelah transaksi sebelumnya (mis. hapus) sudah commit/tidak aktif lagi.
		if (session.getTransaction() == null || !session.getTransaction().isActive()) {
			session.beginTransaction();
		}
		Criteria criteria = session.createCriteria(SuratKeluar.class)
				.createAlias("klasifikasiSuratKeluar", "klasifikasiSuratKeluar")
				.add(Restrictions.or(Restrictions.isNull("tipe"), Restrictions.eq("tipe", tipe)))

				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"))

				.add(Restrictions.or(Restrictions.isNull("satuanKerja"),
						satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(
										parent == null ? Restrictions.isNull("satuanKerja")
												: Restrictions.sqlRestriction("false"),
										Restrictions.in("satuanKerja", satuanKerjas))))

		;

		if (searchkelompokNomorSurat != null && searchkelompokNomorSurat.getSelectedItem() != null
				&& searchkelompokNomorSurat.getSelectedItem().getValue() != null) {
			criteria.createAlias("klasifikasiSuratKeluar.nomorSurat", "nomorSurat").add(Restrictions
					.eq("nomorSurat.kelompokNomorSurat", searchkelompokNomorSurat.getSelectedItem().getValue()));
		}

		List<Long> ids = null;
		if (!searchnama.getValue().trim().isEmpty()) {
			ids = session.createCriteria(KlasifikasiSuratKeluarParemeterValue.class)
					.setProjection(Projections.groupProperty("suratKeluar.id"))
					.add(Restrictions.isNotNull("suratKeluar"))
					.add(Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE)).list();
		}

		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria

				.add(ids == null ? Restrictions.sqlRestriction("true")

						: (ids.isEmpty() ? Restrictions.sqlRestriction("false") : Restrictions.in("id", ids)))

				.add(searchperihal == null || searchperihal.getValue().trim().isEmpty()
						? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("perihal", searchperihal.getValue(), MatchMode.ANYWHERE))

				.add(searchket == null || searchket.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("keterangan", searchket.getValue(), MatchMode.ANYWHERE))

				.add(criterion)

				.add(klasifikasiSuratKeluar == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("klasifikasiSuratKeluar", klasifikasiSuratKeluar))

				.add(CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

				.add(CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false))

				.add(searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
						|| searchsekolah.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false))

				.add(searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
						|| searchyayasan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false))

				.add(searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.ilike("agenda", searchkode.getValue(), MatchMode.ANYWHERE),
								Restrictions.ilike("kode", searchkode.getValue(), MatchMode.ANYWHERE)))
				.add((searchmulai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (ambilTanggalDateboxAman(searchmulai) == null ? Restrictions.sqlRestriction("true")
						: Restrictions.ge("tanggal", ambilTanggalDateboxAman(searchmulai))))
				.add((searchsampai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (ambilTanggalDateboxAman(searchsampai) == null ? Restrictions.sqlRestriction("true")
						: Restrictions.le("tanggal", ambilTanggalDateboxAman(searchsampai))));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		if (searchjurusan == null) {
			return;
		}

		Common.initPaging(initCriteria(false), paging);

		List<SuratKeluar> SuratKeluar = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(SuratKeluar);
		grid.setRowRenderer(new SuratKeluarRenderer());
		if (ubahLangsungA) {
			grid.setModel(strset);
		} else {
			grid.setModelCheckMobile(strset);
		}

	}

	@SuppressWarnings("deprecation")
	@Override
	public MyGrid form(GeneralValueObject generalValueObject, DisposisiSop disposisiSop,
			final MyToolbarbuttonConfig save, EventListener setujui) throws Exception {
		tbmuser = Common.getCurrentUser();
		this.suratKeluar = (SuratKeluar) generalValueObject;
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null))
				? this.disposisiSop
				: disposisiSop;
		selectedJenisJabatan = new HashSet<JenisJabatan>();
		removedJenisJabatan = new HashSet<JenisJabatan>();

		try {
			jenisSurats = new JSONObject(suratKeluar.getJenisSurats());
		} catch (Exception e) {
			jenisSurats = new JSONObject();
		}

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("40%");

		column = new MyColumnConfig();
		column.setParent(columns);

		rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Surat Keluar *"));
		row.appendChild(klasifikasiSuratKeluar = new AmbilDataKlasifikasiSuratKeluarBanbox(tipe));
		klasifikasiSuratKeluar.setAttribute("klasifikasiSuratKeluar", suratKeluar.getKlasifikasiSuratKeluar());
		klasifikasiSuratKeluar.setValue(suratKeluar.getKlasifikasiSuratKeluar() == null ? ""
				: suratKeluar.getKlasifikasiSuratKeluar().getKode()
						+ (suratKeluar.getKlasifikasiSuratKeluar().getNama() == null
								|| suratKeluar.getKlasifikasiSuratKeluar().getNama().trim().isEmpty() ? ""
										: "-" + suratKeluar.getKlasifikasiSuratKeluar().getNama()));
		klasifikasiSuratKeluar.setReadonly(true);

		klasifikasiSuratKeluar.setWidth("90%");

		EventListener eventListenerGantiNomor = new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {

				if (!chekBiaya()) {
					return;
				}

				KlasifikasiSuratKeluar thisKlasifikasiSuratKeluar = (KlasifikasiSuratKeluar) (klasifikasiSuratKeluar
						.getAttribute("klasifikasiSuratKeluar"));

				if (thisKlasifikasiSuratKeluar != null) {
					istilahSuratLain.setValue(thisKlasifikasiSuratKeluar.getIstilahSuratLain() + " *");

					if (istilahSuratLain.getParent() != null)
						istilahSuratLain.getParent().setVisible(thisKlasifikasiSuratKeluar.getKaitkanDenganSuratLain());

					if (suratSebelumnya.getParent() != null)
						suratSebelumnya.getParent().setVisible(thisKlasifikasiSuratKeluar.getKaitkanDenganSuratLain());
				}

				if (alurPersetujuanSuratKeluar != null && alurPersetujuanSuratKeluar.getParent() != null) {
					alurPersetujuanSuratKeluar.getParent().setVisible(
							!(thisKlasifikasiSuratKeluar != null && thisKlasifikasiSuratKeluar.getTanpaAlur()));
				}

				if (tahunAkademik != null && tahunAkademik.getParent() != null) {
					tahunAkademik.getParent().setVisible(
							thisKlasifikasiSuratKeluar != null && thisKlasifikasiSuratKeluar.getTampilkanSemester());
				}

				if (ganjilGenap != null && ganjilGenap.getParent() != null) {
					ganjilGenap.getParent().setVisible(
							thisKlasifikasiSuratKeluar != null && thisKlasifikasiSuratKeluar.getTampilkanSemester());
				}

				if (tabIsi != null && tabHasilScan != null) {
					tabIsi.setVisible(
							!(thisKlasifikasiSuratKeluar != null && thisKlasifikasiSuratKeluar.getTanpaTemplate()));

					tabIsi.setSelected(tabIsi.isVisible());
					tabHasilScan.setSelected(!tabIsi.isVisible());

					List<Row> myRows = rows.getChildren();

					for (Row myrow : myRows) {
						if (myrow.getAttribute("klasifikasiSuratKeluarParemeter") != null) {
							KlasifikasiSuratKeluarParemeter myklasifikasiSuratKeluarParemeter = (KlasifikasiSuratKeluarParemeter) myrow
									.getAttribute("klasifikasiSuratKeluarParemeter");
							myrow.setVisible(tabIsi.isVisible() && myklasifikasiSuratKeluarParemeter.getTampil());
						}
					}

				}

				if (thisKlasifikasiSuratKeluar != null && perihal != null && perihal.getValue().trim().isEmpty()
						&& !thisKlasifikasiSuratKeluar.getPerihalDefault().trim().isEmpty()) {
					perihal.setValue(thisKlasifikasiSuratKeluar.getPerihalDefault());
				}

				if (thisKlasifikasiSuratKeluar != null
						&& thisKlasifikasiSuratKeluar.getKlasifikasiSuratKeluarUntuk() != null) {

					if (thisKlasifikasiSuratKeluar.getSatuanKerja() != null) {
						satuanKerja.setAttribute("satuanKerja", thisKlasifikasiSuratKeluar.getSatuanKerja());
						satuanKerja.setValue(thisKlasifikasiSuratKeluar.getSatuanKerja() == null ? ""
								: thisKlasifikasiSuratKeluar.getSatuanKerja().getNama());
						satuanKerja.setDisabled(true);
					} else {
						satuanKerja.setAttribute("satuanKerja", null);
						satuanKerja.setValue("");
						satuanKerja.setDisabled(false);
					}

					initParameter(rows, thisKlasifikasiSuratKeluar);
					listenerUtama.onEvent(arg0);

					rowmhs.setVisible(false);
					rowdsn.setVisible(false);
					rowpeg.setVisible(false);

					rowssw.setVisible(false);
					rowgr.setVisible(false);

					if (thisKlasifikasiSuratKeluar.getKlasifikasiSuratKeluarUntuk().getId()
							.equals(SuratUtil.MAHASISWA.getId())) {
						rowmhs.setVisible(true);
					} else if (thisKlasifikasiSuratKeluar.getKlasifikasiSuratKeluarUntuk().getId()
							.equals(SuratUtil.SISWA.getId())) {
						rowssw.setVisible(true);
					} else if (thisKlasifikasiSuratKeluar.getKlasifikasiSuratKeluarUntuk().getId()
							.equals(SuratUtil.DOSEN.getId())) {
						rowdsn.setVisible(true);
					} else if (thisKlasifikasiSuratKeluar.getKlasifikasiSuratKeluarUntuk().getId()
							.equals(SuratUtil.GURU.getId())) {
						rowgr.setVisible(true);
					} else if (thisKlasifikasiSuratKeluar.getKlasifikasiSuratKeluarUntuk().getId()
							.equals(SuratUtil.PEGAWAI.getId())) {
						rowpeg.setVisible(true);
					}

					tanggal.setDisabled(thisKlasifikasiSuratKeluar.getKlasifikasiSuratKeluarUntuk()
							.getTanggalSuratTidakBisaDiubah());

					Mahasiswa mhs = (Mahasiswa) mahasiswa.getAttribute("mahasiswa");

					if (thisKlasifikasiSuratKeluar.getKlasifikasiSuratKeluarUntuk().getStatusMahasiswa() != null
							&& mhs != null) {
						HistoryStatusMahasiswa historyStatusMahasiswa = ais.action.master.helper.HistoryStatusMahasiswaUtil
								.currentStatus(mhs);
						if (!historyStatusMahasiswa.getStatusMahasiswa().getId().equals(thisKlasifikasiSuratKeluar
								.getKlasifikasiSuratKeluarUntuk().getStatusMahasiswa().getId())) {
							MyMessageboxConfig.showFormatCb(
									"Mohon maaf, status mahasiswa untuk surat ini harus \"{V1}\", sedangkan status mahasiswa {V2} saat ini adalah \"{V3}\". Langkah yang dapat dilakukan: (1) periksa kembali status mahasiswa yang bersangkutan; (2) pastikan status telah sesuai dengan ketentuan surat; (3) pilih kembali klasifikasi surat yang sesuai.",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
									new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											klasifikasiSuratKeluar.setAttribute("klasifikasiSuratKeluar", null);
											klasifikasiSuratKeluar.setValue("");
										}
									},
									thisKlasifikasiSuratKeluar.getKlasifikasiSuratKeluarUntuk().getStatusMahasiswa()
											.getNama(),
									mhs.getNama(), historyStatusMahasiswa.getStatusMahasiswa().getNama());
							return;
						}
					}

					if (thisKlasifikasiSuratKeluar.getKlasifikasiSuratKeluarUntuk().getStatusAwalMahasiswa() != null
							&& mhs != null) {
						if (!mhs.getStatusAwalMahasiswa().getId().equals(thisKlasifikasiSuratKeluar
								.getKlasifikasiSuratKeluarUntuk().getStatusAwalMahasiswa().getId())) {
							MyMessageboxConfig.showFormatCb(
									"Mohon maaf, status awal mahasiswa untuk surat ini harus \"{V1}\", sedangkan status awal mahasiswa {V2} saat ini adalah \"{V3}\". Langkah yang dapat dilakukan: (1) periksa kembali status awal mahasiswa yang bersangkutan; (2) pastikan status telah sesuai dengan ketentuan surat; (3) pilih kembali klasifikasi surat yang sesuai.",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
									new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											klasifikasiSuratKeluar.setAttribute("klasifikasiSuratKeluar", null);
											klasifikasiSuratKeluar.setValue("");
										}
									},
									thisKlasifikasiSuratKeluar.getKlasifikasiSuratKeluarUntuk().getStatusAwalMahasiswa()
											.getNama(),
									mhs.getNama(), mhs.getStatusAwalMahasiswa().getNama());
							return;
						}
					}

					if (thisKlasifikasiSuratKeluar.getKlasifikasiSuratKeluarUntuk().getMahasiswaHarusTelahMembayar()
							&& mhs != null) {
						if (!Common.checkStatusPembayaranMahasiswa(mhs.currentSemester(), mhs.currentTahapan(), mhs,
								false, false)) {
							MyMessageboxConfig.showFormatCb(
									"Mohon maaf, mahasiswa yang bersangkutan belum melunasi biaya perkuliahan pada semester {V1}. Langkah yang dapat dilakukan: (1) periksa status pembayaran mahasiswa; (2) selesaikan pelunasan biaya perkuliahan tersebut; (3) hubungi bagian keuangan untuk informasi lebih lanjut.",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION,
									new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											klasifikasiSuratKeluar.setAttribute("klasifikasiSuratKeluar", null);
											klasifikasiSuratKeluar.setValue("");
										}
									}, mhs.currentSemester());
							return;
						}
					}

					if (suratKeluar.getId() == null) {
						try {
							kode.setDisabled(!nomorSuratBolehDibahManual.isChecked());
							kode.setReadonly(!nomorSuratBolehDibahManual.isChecked());
							if (nomorSuratBolehDibahManual.isChecked()) {
								String noAgenda = generateCode(false);
								kode.setValue(noAgenda);
							} else {
								kode.setValue("");
							}

							String noAgendaData = generateCodeAgenda(false);
							agenda.setValue(noAgendaData);
							agenda.getParent().setVisible(suratKeluar.getAgenda() != null);
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/SuratKeluarAction.java:4768");
							// TODO: handle exception
						}

					}
				}
			}
		};

		klasifikasiSuratKeluar.setEventListener(eventListenerGantiNomor);

		tbmuser = Common.getCurrentUser();
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nomor Surat"));
		row.appendChild(kode = new Textbox(suratKeluar.getKode()));
		kode.setReadonly(SuratKeluarAction.this.suratKeluar.getId() == null || tbmuser.getMahasiswa() != null
				|| tbmuser.ambilDosen() != null);
		kode.setWidth("90%");
		kode.addEventListener("onChange", listenerUtama);

		if (suratKeluar.getId() != null) {
			kode.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					Session session = HibernateUtil.currentNativeSession();
					SuratKeluar mySuratKeluar = (SuratKeluar) session.createCriteria(SuratKeluar.class)
							.add(Restrictions.idEq(suratKeluar.getId())).uniqueResult();

					mySuratKeluar.setKode(kode.getValue().trim());
					session.getTransaction().begin();
					Common.refreshUpdate(session, mySuratKeluar);
					session.getTransaction().commit();
					// session.disconnect();
					ais.common.Common.closeOpenedSession(session);

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							onSearchDefault(arg0);
						}
					});
				}
			});
		}

		if (SuratKeluarAction.this.suratKeluar.getId() == null) {
			Common.initKeterangan(rows, "Nomor surat akan ter-cetak ketika Anda klik tombol Simpan di sebelah bawah");
			kode.setDisabled(true);
		}

		kode.setAttribute("janganDisabled", true);

		row = new MyFormRow();
		row.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.getSiswa() == null && tbmuser.ambilDosen() == null && tbmuser.ambilGuru() == null);
		row.setVisible(suratKeluar.getId() == null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		nomorSuratBolehDibahManual = new MyCheckboxConfig("Nomor surat boleh diubah manual");
		row.appendChild(nomorSuratBolehDibahManual);
		nomorSuratBolehDibahManual.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				kode.setDisabled(!nomorSuratBolehDibahManual.isChecked());
				kode.setReadonly(!nomorSuratBolehDibahManual.isChecked());
				if (nomorSuratBolehDibahManual.isChecked()) {
					String noAgenda = generateCode(false);
					kode.setValue(noAgenda);
				} else {
					kode.setValue("");
				}

				String noAgendaData = generateCodeAgenda(false);
				agenda.setValue(noAgendaData);
				agenda.getParent().setVisible(suratKeluar.getAgenda() != null);
			}
		});

		if (Common.getKonfigurasi("nomor_surat_boleh_diubah_manual", Konfigurasi.AKTIF).getNilai()
				.equals(Konfigurasi.TIDAK_AKTIF)) {
			row.setVisible(false);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nomor Agenda"));
		row.appendChild(agenda = new Label(suratKeluar.getAgenda()));
		agenda.getParent().setVisible(suratKeluar.getAgenda() != null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Perihal *"));
		row.appendChild(perihal = new Textbox(suratKeluar.getPerihal()));
		perihal.setWidth("90%");

		if (suratKeluar.getId() == null) {
			suratKeluar.setSatuanKerja(Common.getSatuanKerja());
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(istilahSuratLain = new ais.ui.util.MyLabelConfig("Surat Sebelumnya *"));
		row.appendChild(suratSebelumnya = new AmbilDataSuratKeluarBanbox());
		suratSebelumnya.setAttribute("suratKeluar", suratKeluar.getSuratSebelumnya());
		suratSebelumnya
				.setValue(suratKeluar.getSuratSebelumnya() == null ? "" : suratKeluar.getSuratSebelumnya().getKode());
		suratSebelumnya.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		satuanKerja = new AmbilDataSatuanKerjaBanbox(true);
		satuanKerja.setValue(suratKeluar.getSatuanKerja() == null ? "" : suratKeluar.getSatuanKerja().getNama());
		satuanKerja.setAttribute("satuanKerja", suratKeluar.getSatuanKerja());
		row.appendChild(satuanKerja);
		satuanKerja.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Surat"));
		row.appendChild(tanggal = new MyDatebox(suratKeluar.getTanggal()));
		tanggal.setReadonly(true);
		tanggal.setDisabled(true);
		tanggal.setWidth("90%");
		tanggal.addEventListener("onChange", eventListenerGantiNomor);

		KlasifikasiSuratKeluar thisKlasifikasiSuratKeluar = (KlasifikasiSuratKeluar) (klasifikasiSuratKeluar
				.getAttribute("klasifikasiSuratKeluar"));
		if (thisKlasifikasiSuratKeluar != null && thisKlasifikasiSuratKeluar.getKlasifikasiSuratKeluarUntuk() != null) {
			tanggal.setDisabled(
					thisKlasifikasiSuratKeluar.getKlasifikasiSuratKeluarUntuk().getTanggalSuratTidakBisaDiubah());
		}

		if (suratKeluar.getFakultas() == null && tbmuser.ambilFakultas() != null) {
			suratKeluar.setFakultas(tbmuser.ambilFakultas());
		}
		if (suratKeluar.getJurusan() == null && tbmuser.ambilJurusan() != null) {
			suratKeluar.setJurusan(tbmuser.ambilJurusan());
		}

		if (suratKeluar.getId() != null) {
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Buat/Ubah"));

			row.appendChild(RevisiHelper.createNewRevisi(SuratKeluar.class, suratKeluar,
					Common.dateFormat3.get().format(suratKeluar.getTanggal_dirubah())));
		}

		Common.initFakultasDanJurusanDanSemua(fakultas = new Combobox(), jurusan = new Combobox(), null, null);
		MyFormRow rowFakultas = new MyFormRow();
		rowFakultas.setVisible(pt && false);
		rowFakultas.setStyle("border:0px;background: transparent;");
		rowFakultas.setParent(rows);
		rowFakultas.appendChild(new Label(ais.common.Common.getBahasaConfig("Fakultas")));
		rowFakultas.appendChild(fakultas);

		Common.selectComboItem(fakultas, suratKeluar.getFakultas());
		fakultas.setWidth("90%");

		Common.insertComboDanSemua(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
				Restrictions.eq("fakultas",
						suratKeluar.getFakultas() == null ? tbmuser.ambilFakultas() : suratKeluar.getFakultas()));

		MyFormRow rowJurusan = new MyFormRow();
		rowJurusan.setVisible(pt && false);
		rowJurusan.setStyle("border:0px;background: transparent;");
		rowJurusan.setParent(rows);
		rowJurusan.appendChild(new Label(ais.common.Common.getBahasaConfig("Jurusan")));
		rowJurusan.appendChild(jurusan);
		jurusan.setWidth("90%");
		Common.pilihJurusan(jurusan, suratKeluar.getJurusan());

		Tbmuser tbmuser1 = Common.getCurrentUser();

		yayasan = new Combobox();
		sekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		row = new MyFormRow();
		row.setVisible(ya);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));

		Common.selectComboItem(yayasan,
				suratKeluar == null || suratKeluar.getYayasan() == null ? tbmuser1.ambilYayasan()
						: suratKeluar.getYayasan());
		row.appendChild(yayasan);
		yayasan.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(ya);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah"));

		Common.pilihSekolah(sekolah,
				suratKeluar == null || suratKeluar.getSekolah() == null ? tbmuser1.ambilSekolah()
						: suratKeluar.getSekolah());
		row.appendChild(sekolah);
		sekolah.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Berkas / Lampiran"));
		row.appendChild(lampiran = new Textbox(suratKeluar.getLampiran()));
		lampiran.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tujuan Surat"));
		row.appendChild(kepada = new Textbox(suratKeluar.getKepada()));
		kepada.setWidth("90%");

		rowmhs = new MyFormRow();
		rowmhs.setVisible(false);
		rowmhs.setStyle("border:0px;background: transparent;");
		rowmhs.setParent(rows);
		rowmhs.appendChild(new Label(ais.common.Common.getBahasaConfig("Mahasiswa")));
		rowmhs.appendChild(mahasiswa = new AmbilDataMahasiswaBanbox());
		mahasiswa.setAttribute("mahasiswa", suratKeluar.getMahasiswa());
		mahasiswa.setValue(suratKeluar.getMahasiswa() == null ? "" : suratKeluar.getMahasiswa().toString());
		mahasiswa.setEventListener(eventListenerGantiNomor);

		if (Common.bolehKonfigurasi("hanya_admin_yg_bisa_ubah_pilihan_mahasiswa_pada_surat_keluar", Konfigurasi.TIDAK_AKTIF)) {
			mahasiswa.setDisabled(!Common.getApakahAdmin());
		}

		if (suratKeluar.getId() == null && tbmuser != null && tbmuser.ambilDosen() != null) {
			suratKeluar.setDosen(tbmuser.ambilDosen());
		}
		if (suratKeluar.getId() == null && tbmuser != null && tbmuser.ambilGuru() != null) {
			suratKeluar.setGuru(tbmuser.ambilGuru());
		}

		rowdsn = new MyFormRow();
		rowdsn.setVisible(false);
		rowdsn.setStyle("border:0px;background: transparent;");
		rowdsn.setParent(rows);
		rowdsn.appendChild(new Label(ais.common.Common.getBahasaConfig("Dosen")));
		rowdsn.appendChild(dosen = new AmbilDataDosenBanbox(false));
		dosen.setAttribute("dosen", suratKeluar.getDosen());
		dosen.setValue(suratKeluar.getDosen() == null ? "" : suratKeluar.getDosen().toString());
		dosen.setEventListener(listenerUtama);

//		if (Common.getKonfigurasi("hanya_admin_yg_bisa_ubah_pilihan_dosen_pada_surat_keluar", Konfigurasi.TIDAK_AKTIF)
//				.getNilai().equals(Konfigurasi.AKTIF)) {
//			dosen.setDisabled(!Common.getApakahAdmin());
//		}

		rowssw = new MyFormRow();
		rowssw.setVisible(false);
		rowssw.setStyle("border:0px;background: transparent;");
		rowssw.setParent(rows);
		rowssw.appendChild(new Label(ais.common.Common.getBahasaConfig("Siswa")));
		rowssw.appendChild(siswa = new AmbilDataSiswaBanbox());
		siswa.setAttribute("siswa", suratKeluar.getSiswa());
		siswa.setValue(suratKeluar.getSiswa() == null ? "" : suratKeluar.getSiswa().toString());
		siswa.setEventListener(eventListenerGantiNomor);

		if (Common.bolehKonfigurasi("hanya_admin_yg_bisa_ubah_pilihan_siswa_pada_surat_keluar", Konfigurasi.TIDAK_AKTIF)) {
			siswa.setDisabled(!Common.getApakahAdmin());
		}

		rowgr = new MyFormRow();
		rowgr.setVisible(false);
		rowgr.setStyle("border:0px;background: transparent;");
		rowgr.setParent(rows);
		rowgr.appendChild(new Label(ais.common.Common.getBahasaConfig("Guru")));
		rowgr.appendChild(guru = new AmbilDataGuruBanbox(false));
		guru.setAttribute("guru", suratKeluar.getGuru());
		guru.setValue(suratKeluar.getGuru() == null ? "" : suratKeluar.getGuru().toString());
		guru.setEventListener(listenerUtama);

//		if (Common.getKonfigurasi("hanya_admin_yg_bisa_ubah_pilihan_guru_pada_surat_keluar", Konfigurasi.TIDAK_AKTIF)
//				.getNilai().equals(Konfigurasi.AKTIF)) {
//			guru.setDisabled(!Common.getApakahAdmin());
//		}

		rowpeg = new MyFormRow();
		rowpeg.setVisible(false);
		rowpeg.setStyle("border:0px;background: transparent;");
		rowpeg.setParent(rows);
		rowpeg.appendChild(new Label(ais.common.Common.getBahasaConfig("Pegawai")));
		rowpeg.appendChild(pegawai = new AmbilDataPegawaiBanbox(true));
		pegawai.setAttribute("pegawai", suratKeluar.getPegawai());
		pegawai.setValue(suratKeluar.getPegawai() == null ? "" : suratKeluar.getPegawai().getNama());
		pegawai.setEventListener(listenerUtama);

		KlasifikasiSuratKeluar myKlasifikasiSuratKeluar = suratKeluar.getKlasifikasiSuratKeluar();
		if (suratKeluar.getId() != null && myKlasifikasiSuratKeluar != null) {
			rowmhs.setVisible(false);
			rowdsn.setVisible(false);
			rowpeg.setVisible(false);

			rowssw.setVisible(false);
			rowgr.setVisible(false);

			if (myKlasifikasiSuratKeluar.getKlasifikasiSuratKeluarUntuk().getStatusAwalMahasiswa() != null
					|| myKlasifikasiSuratKeluar.getKlasifikasiSuratKeluarUntuk().getStatusMahasiswa() != null
					|| myKlasifikasiSuratKeluar.getKlasifikasiSuratKeluarUntuk().getId()
							.equals(SuratUtil.MAHASISWA.getId())) {
				rowmhs.setVisible(true);
			} else if (myKlasifikasiSuratKeluar.getKlasifikasiSuratKeluarUntuk().getId()
					.equals(SuratUtil.SISWA.getId())) {
				rowssw.setVisible(true);
			} else if (myKlasifikasiSuratKeluar.getKlasifikasiSuratKeluarUntuk().getId()
					.equals(SuratUtil.DOSEN.getId())) {
				rowdsn.setVisible(true);
			} else if (myKlasifikasiSuratKeluar.getKlasifikasiSuratKeluarUntuk().getId()
					.equals(SuratUtil.GURU.getId())) {
				rowgr.setVisible(true);
			} else if (myKlasifikasiSuratKeluar.getKlasifikasiSuratKeluarUntuk().getId()
					.equals(SuratUtil.PEGAWAI.getId())) {
				rowpeg.setVisible(true);
			}

		}

		if (Common.bolehKonfigurasi("hanya_admin_yg_bisa_ubah_pilihan_pegawai_pada_surat_keluar", Konfigurasi.TIDAK_AKTIF)) {
			pegawai.setDisabled(!Common.getApakahAdmin());
		}

		tahunAkademik = new Combobox();
		Common.generateTahunAjaran(tahunAkademik);
		ganjilGenap = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		ganjilGenap.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		ganjilGenap.appendChild(comboitem);

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik *"));
		row.appendChild(tahunAkademik);
		Common.selectComboItem(tahunAkademik, suratKeluar.getTahunAkademik());
		tahunAkademik.setWidth("90%");

		tahunAkademik.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (suratKeluar != null) {
					suratKeluar.setSemester((String) ganjilGenap.getSelectedItem().getValue());
					suratKeluar.setTahunAkademik((String) tahunAkademik.getSelectedItem().getValue());
				}
				listenerUtama.onEvent(arg0);
			}
		});

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester *"));
		row.appendChild(ganjilGenap);
		Common.selectComboItem(ganjilGenap, suratKeluar.getSemester());
		ganjilGenap.setWidth("90%");
		ganjilGenap.setReadonly(true);

		ganjilGenap.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (suratKeluar != null) {
					suratKeluar.setSemester((String) ganjilGenap.getSelectedItem().getValue());
					suratKeluar.setTahunAkademik((String) tahunAkademik.getSelectedItem().getValue());
				}
				listenerUtama.onEvent(arg0);
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Alur Persetujuan"));
		row.appendChild(alurPersetujuanSuratKeluar = new AmbilDataAlurPersetujuanSuratKeluarBanbox(true, true, tipe));
		alurPersetujuanSuratKeluar.setAttribute("alurPersetujuanSuratKeluar",
				suratKeluar.getAlurPersetujuanSuratKeluar());
		alurPersetujuanSuratKeluar.setValue(suratKeluar.getAlurPersetujuanSuratKeluar() == null ? ""
				: suratKeluar.getAlurPersetujuanSuratKeluar().toString());
		alurPersetujuanSuratKeluar.setWidth("90%");

		alurPersetujuanSuratKeluar.setDisabled(suratKeluar.getKlasifikasiSuratKeluar() != null
				&& suratKeluar.getKlasifikasiSuratKeluar().getAlurPersetujuanSuratKeluar() != null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(vboxAlur = new Vbox());

		alurEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initKelengkapanBerkas(vboxAlur, (AlurPersetujuanSuratKeluar) alurPersetujuanSuratKeluar
						.getAttribute("alurPersetujuanSuratKeluar"));
			}
		};

		alurPersetujuanSuratKeluar.setEventListener(alurEventListener);
		Common.createDefaultTimer(alurEventListener);

		row = new MyFormRow();
		row.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.getSiswa() == null);
		row.setParent(rows);
		broadcast = new MyCheckboxConfig("Sebarkan / broadcast surat ini");
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(broadcast);
		broadcast.setChecked(suratKeluar.getBroadcast());

		row = new MyFormRow();
		row.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.getSiswa() == null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sebarkan ke username pengguna"));
		row.appendChild(usernamePengguna = new Textbox(suratKeluar.getUsernamePengguna()));
		usernamePengguna.setWidth("90%");
		usernamePengguna.setRows(2);

		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Ambil Username Pengguna",
				"/img/user_male_add.png");

		final MyFormRow rowAmbilPengguna = new MyFormRow();
		rowAmbilPengguna.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.getSiswa() == null);
		rowAmbilPengguna.setParent(rows);
		rowAmbilPengguna.appendChild(new ais.ui.util.MyLabelConfig(""));
		rowAmbilPengguna.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				AmbilDataTbmuserBanyak ambil = new AmbilDataTbmuserBanyak(new ArrayList<Tbmuser>());
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambil);
				ambil.setEventListener(new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event arg0) throws Exception {
						// TODO Auto-generated method stub
						List<Tbmuser> tbmusers = (List<Tbmuser>) arg0.getData();
						if (tbmusers != null && tbmusers.size() != 0) {
							for (Tbmuser tbmuser : tbmusers) {
								usernamePengguna.setValue(usernamePengguna.getValue()
										+ (usernamePengguna.getValue().isEmpty() ? tbmuser.getUserId()
												: "," + tbmuser.getUserId()));
							}
						}
					}
				});
				ambil.setWidth("850px");
				ambil.setHeight("97%");
				ambil.setVisible(true);
				ambil.onModal();
			}
		});

		final Row a2 = Common.initKeterangan(rows,
				"Jika lebih dari satu, pisahkan dengan tanda koma (,). Kosongkan apabila boleh diajukan oleh semua username pengguna");
		a2.setVisible(tbmuser != null && tbmuser.ambilDosen() == null && tbmuser.getMahasiswa() == null
				&& tbmuser.ambilGuru() == null && tbmuser.getSiswa() == null);

		EventListener startEvent = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				a2.setVisible(broadcast.isChecked() && tbmuser != null && tbmuser.getMahasiswa() == null
						&& tbmuser.getSiswa() == null && tbmuser.getSiswa() == null);
				rowAmbilPengguna.setVisible(broadcast.isChecked() && tbmuser != null && tbmuser.ambilDosen() == null
						&& tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null);
				usernamePengguna.getParent().setVisible(broadcast.isChecked() && tbmuser != null
						&& tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null);
			}

		};

		broadcast.addEventListener("onClick", startEvent);
		startEvent.onEvent(null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(suratKeluar.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		if (tbmuser.getMahasiswa() != null) {
			mahasiswa.setAttribute("mahasiswa", tbmuser.getMahasiswa());
			mahasiswa.setValue(tbmuser.getMahasiswa().getNim() + "-" + tbmuser.getMahasiswa().getNama());
		}

		if (tbmuser.ambilDosen() != null) {
			dosen.setAttribute("dosen", tbmuser.ambilDosen());
			dosen.setValue(tbmuser.ambilDosen().getNama());
		}

		if (disposisiSop != null) {
			row = new MyFormRow();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");

			MyGroupboxStyled groupboxStyled = new MyGroupboxStyled();
			groupboxStyled.setParent(row);
			groupboxStyled.setHeight("800px");
			groupboxStyled.appendChild(new MyCaptionStyled("Surat Keluar"));

			initDetail(suratKeluar, groupboxStyled);
		}

		ubah = true;
		if (suratKeluar.getId() != null) {

			Session session = HibernateUtil.currentSession();
			AlurPersetujuanSuratKeluarStatus alurPersetujuanSuratKeluarStatus = (AlurPersetujuanSuratKeluarStatus) session
					.createCriteria(AlurPersetujuanSuratKeluarStatus.class).add(Restrictions.isNotNull("kodeUnik"))
					.setMaxResults(1).add(Restrictions.eq("suratKeluar", suratKeluar)).addOrder(Order.asc("id"))
					.uniqueResult();

			if (alurPersetujuanSuratKeluarStatus != null && alurPersetujuanSuratKeluarStatus.getDisetujui()) {
				Common.freezeGanti(grid, true);
				ubah = false;
			}
		}

		if (suratKeluar.getId() != null && myKlasifikasiSuratKeluar != null) {
			initParameter(rows, myKlasifikasiSuratKeluar);
		}

		if (searchklasifikasiSuratKeluar != null
				&& searchklasifikasiSuratKeluar.getAttribute("klasifikasiSuratKeluar") != null) {
			KlasifikasiSuratKeluar k = (KlasifikasiSuratKeluar) searchklasifikasiSuratKeluar
					.getAttribute("klasifikasiSuratKeluar");
			klasifikasiSuratKeluar.setValue(k.getNama());
			klasifikasiSuratKeluar.setAttribute("klasifikasiSuratKeluar", k);

			if (searchklasifikasiSuratKeluar.isDisabled()) {
				klasifikasiSuratKeluar.setDisabled(true);
			}

			addWindow.setTitle("Pendataan " + (k == null ? "" : k.getNama()));

		}

		row = new MyFormRow();
		row.setVisible(suratKeluar.getAlurDitolak() != null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Catatan Ditolak"));
		row.appendChild(new MyLabelAgakKecilBoldMerah(
				suratKeluar.getAlurDitolak() == null ? "" : suratKeluar.getAlurDitolak().getKeterangan()));

		row = new MyFormRow();
		row.setVisible(suratKeluar.getAlurDitolak() != null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Catatan Revisi Perbaikan *"));
		row.appendChild(catatanRevisi = new Textbox(suratKeluar.getCatatanRevisi()));
		catatanRevisi.setWidth("90%");
		catatanRevisi.setRows(5);

		if (suratKeluar != null && suratKeluar.getId() != null) {
			row = new MyFormRow();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");
			row.appendChild(new MyLabelStyled("Informasi Disposisi"));

			row = new MyFormRow();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");
			String html = SuratKeluarAction.infoDisposisiBagan(suratKeluar);
			new ais.ui.util.MyHtml(html).setParent(row);
		}

		if (klasifikasiSuratKeluar.getAttribute("klasifikasiSuratKeluar") != null) {
			eventListenerGantiNomor.onEvent(null);
		}

		return grid;
	}

	@Override
	public String istilah() throws Exception {
		// TODO Auto-generated method stub
		return "Surat Keluar";
	}

	@Override
	public DataSop ambil() throws Exception {
		// TODO Auto-generated method stub
		return suratKeluar;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Class ambilClass() throws Exception {
		// TODO Auto-generated method stub
		return SuratKeluar.class;
	}

	@Override
	public void setPersetujuan(boolean persetujuan) {
//		this.persetujuan = persetujuan;
	}

	@Override
	public File cetakData(GeneralValueObject generalValueObject) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
}
