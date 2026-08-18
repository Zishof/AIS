package ais.action.master.surat;


import ais.common.CommonSearchFilterHelper;
import java.io.File;
import java.io.FileOutputStream;
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

import org.apache.pdfbox.util.PDFMergerUtility;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Doublebox;
import org.zkoss.zul.East;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
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

import ais.action.master.dashboard.surat.DasboardSuratMasuk;
import ais.action.master.helper.BroadcastHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.generic.AmbilDataTbmuserBanyak;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.master.sop.TampilanAlurSopAction;
import ais.action.master.sop.helper.SopUtil;
import ais.action.master.surat.helper.AmbilDataAlurPersetujuanSuratMasukBanbox;
import ais.action.master.surat.helper.AmbilDataKlasifikasiSuratMasukBanbox;
import ais.action.master.surat.helper.SuratMasukPunyaGambarFotoHelper;
import ais.action.master.surat.util.SuratUtil;
import ais.action.report.Report;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.UIClassHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.employ.JenisJabatan;
import ais.database.model.file.FotoGambarSuratMasuk;
import ais.database.model.file.LampiranLain;
import ais.database.model.rab.Pejabat;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;
import ais.database.model.surat.AlurPersetujuanSuratMasuk;
import ais.database.model.surat.AlurPersetujuanSuratMasukStatus;
import ais.database.model.surat.KlasifikasiSuratMasuk;
import ais.database.model.surat.KlasifikasiSuratMasukParemeter;
import ais.database.model.surat.KlasifikasiSuratMasukParemeterValue;
import ais.database.model.surat.LokerSurat;
import ais.database.model.surat.NomorSurat;
import ais.database.model.surat.OpsiSuratMasuk;
import ais.database.model.surat.OpsiSuratMasukValue;
import ais.database.model.surat.SifatSurat;
import ais.database.model.surat.SuratKeluar;
import ais.database.model.surat.SuratMasuk;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.FormSop;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyCkEditor;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyGroupboxStyled;
import ais.ui.util.MyHtml;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelAgakKecilBoldMerah;
import ais.ui.util.MyLabelStyled;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

public class SuratMasukAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault, FormSop {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchnamaIsi;
	private Textbox searchkode;
	private Textbox searchjenis;
	private Textbox searchasal;
	private Combobox searchloker;
	private MyDatebox searchmulai;
	private MyDatebox searchsampai;
	protected Combobox searchfakultas;
	protected Combobox searchjurusan;
	private Combobox searchyayasan;
	private Combobox searchsekolah;
	private AmbilDataSatuanKerjaBanbox searchparent;
	private AmbilDataKlasifikasiSuratMasukBanbox klasifikasiSuratMasuk;
	private Textbox kode;
	private AmbilDataAlurPersetujuanSuratMasukBanbox alurPersetujuanSuratMasuk;

	private MyDatebox tanggal;
	private MyDatebox tanggalSurat;
	private Textbox asal;
	private Textbox noSurat;
	private Combobox loker;
	private Textbox perihal;
	private Textbox lampiran;
	// private Textbox catatanDisposisi;
	private MyCkEditor ringkasan;

	private boolean edit = false;
	private boolean delete = false;

	private SuratMasuk suratMasuk;
	private MyToolbarbuttonConfig add;
	private Rows rows;

	// private SatuanKerjaTreeModel satuanKerjaTreeModel;
	private MyGrid gridGambar;
	private Rows rowsOpsiSuratMasuk;
	// private MyCkEditor keterangan;
	private Tabpanel tabpanelRingkasan;

	private SatuanKerjaTreeModel satuanKerjaTreeModel;
	private boolean pt = false;
	private boolean ya = false;
	private Row hbFakultasLabel;
	private Row hbYayasan;
	private Combobox yayasan;
	private Combobox sekolah;
	private AmbilDataSatuanKerjaBanbox satuanKerja;

	protected Tabpanel statistik;

	public SuratMasukAction() {
		super();
	}

	public SuratMasukAction(String tipe) {
		super();
		this.tipe = tipe;
	}

	public void onStatistik(Event event) {

		if (statistik.getChildren().size() == 0) {
			DasboardSuratMasuk include = new DasboardSuratMasuk(tipe);
			include.setHeight("100%");
			include.setWidth("100%");
			include.setParent(statistik);
		}
	}

	public static String[] contents = new String[] { "id", "noSurat", "kode", "nama", "klasifikasiSuratMasuk",
			"tanggalSurat", "tanggal", "loker", "status", "sifat", "kerahasiaan", "asal", "tujuan", "lampiran",
			"perihal", "tanggalDiteruskan", "alurPersetujuanSuratMasuk", "fakultas", "jurusan", "yayasan", "sekolah",
			"satuanKerja", "simpan", "balas", "perbanyak", "teliti", "ikutiPerkembangan", "harapPenjelasanMasalah",
			"untukDiproses", "saranSaran", "pakaiSebagaiPedoman", "bicarakanDenganSaya", "fotocopyUntukSaya",
			"ringkasan", "koreksi", "pejabatBerwenang", "namaPejabatBerwenang", "isi", "isiDisposisiPimpinan",
			"jawabanPenerimaDisposisi", "keterangan" };
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
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		if (execution.getParameter("tipe") != null && !execution.getParameter("tipe").trim().isEmpty()) {
			tipe = execution.getParameter("tipe").trim();
		}

		tbmuser = Common.getCurrentUser();

		EventListener eventListenerD = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
				Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
				if (parent != null) {
					satuanKerjas.clear();
					satuanKerjas.add(parent);
					satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
				}

				Common.insertComboDanSemua(searchloker, new String[] { "kode", "nama" }, "keterangan", LokerSurat.class,

						Restrictions.and(
								Restrictions.or(Restrictions.isNull("satuanKerja"),
										satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
												: Restrictions.or(Restrictions.isNull("satuanKerja"),
														Restrictions.in("satuanKerja", satuanKerjas))),

								Restrictions.and(Restrictions.eq("tipe", tipe), Restrictions
										.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))));

				Common.createDefaultTimerNoBusy(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						onSearchDefault(null);
					}
				});
			}
		};

		if (searchparent != null) { searchparent.setEventListener(eventListenerD); }
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		boolean[] ptYa = Common.chekPtAtauSekolah();
		pt = ptYa[0];
		ya = ptYa[1];

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah, true, false);

		if (hbFakultasLabel != null) { hbFakultasLabel.setVisible(pt && searchfakultas.getChildren().size() > 1); }
		if (hbYayasan != null) { hbYayasan.setVisible(ya); }

		@SuppressWarnings("unused")
		OpsiSuratMasuk balas = SuratUtil.balas;

		// satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

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

		MyToolbarbuttonConfig upload = Common.uploadData(this, SuratMasuk.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

		Common.createDefaultTimer(eventListenerD);
	        FilterLanjutHelper.setup(comp);
}

	class SuratMasukRenderer extends ais.ui.util.MyRowRenderer {

		@SuppressWarnings("unchecked")
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final SuratMasuk suratMasuk = (SuratMasuk) arg1;

			// "Alur Disposisi & Tindak Lanjut" dipindah dari kolom sempit "Status Disposisi"
			// ke baris DETAIL full-width di bawah row (open=true) agar kartu alur mengalir
			// horizontal dan tidak menumpuk. Detail = anak PERTAMA row (kolom pemandu di ZUL).
			org.zkoss.zul.Detail detailAlur = new org.zkoss.zul.Detail();
			detailAlur.setOpen(true);
			detailAlur.setParent(arg0);

			if (suratMasuk.getTipe() == null) {
				suratMasuk.setTipe(tipe);
				Common.refreshUpdate(suratMasuk);
			}

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			new MyLabelAgakKecil(
					suratMasuk.getTanggal() == null ? "" : Common.dateFormat6.get().format(suratMasuk.getTanggal()))
					.setParent(vbox);
			new MyLabelAgakKecil(
					suratMasuk.getTanggalSurat() == null ? "" : Common.dateFormat6.get().format(suratMasuk.getTanggalSurat()))
					.setParent(vbox);
			Vbox a;
			(a = RevisiHelper.createNewRevisi(SuratMasuk.class, suratMasuk, suratMasuk.getKode())).setParent(vbox);
			a.appendChild(new Label(suratMasuk.getNoSurat()));
			new MyLabelAgakKecil(suratMasuk.getNama()).setParent(arg0);

			vbox = new Vbox();
			vbox.setParent(arg0);
			new MyLabelAgakKecil(suratMasuk.getLoker() == null ? "" : suratMasuk.getLoker().getNama()).setParent(vbox);
			new MyLabelAgakKecil(suratMasuk.getStatus()).setParent(vbox);
			new MyLabelAgakKecil(suratMasuk.getSifat()).setParent(vbox);
			new MyLabelAgakKecil(suratMasuk.getKerahasiaan()).setParent(vbox);

			new Label(suratMasuk.getPerihal()).setParent(arg0);

			Session session = HibernateUtil.currentSession();
			String html = "";
			List<OpsiSuratMasukValue> suratMasukValues = session.createCriteria(OpsiSuratMasukValue.class)
					.add(Restrictions.eq("suratMasuk", suratMasuk)).list();
			for (OpsiSuratMasukValue opsiSuratMasukValue : suratMasukValues) {
				html += "<li>" + opsiSuratMasukValue.getNama() + "</li>";
			}
			
			String safeInputhtml = MyHtml.bersihkan(html);

			new ais.ui.util.MyHtml("<font style=\"font-size: x-small;\"><ul>" + safeInputhtml + "</ul></font>").setParent(arg0);

			html = "";
			List<KlasifikasiSuratMasukParemeterValue> klasifikasiSuratMasukParemeterValues = session
					.createCriteria(KlasifikasiSuratMasukParemeterValue.class)
					.createAlias("klasifikasiSuratMasukParemeter", "klasifikasiSuratMasukParemeter")
					.addOrder(Order.asc("klasifikasiSuratMasukParemeter.nama"))
					.add(Restrictions.eq("suratMasuk", suratMasuk)).list();
			for (KlasifikasiSuratMasukParemeterValue klasifikasiSuratMasukParemeterValue : klasifikasiSuratMasukParemeterValues) {
				html += "<li>" + klasifikasiSuratMasukParemeterValue.getKlasifikasiSuratMasukParemeter().getNama()
						+ " : " + klasifikasiSuratMasukParemeterValue.getNama() + "</li>";
			}

			if (suratMasuk.getRingkasan() != null && !suratMasuk.getRingkasan().trim().isEmpty()) {
				html += "<hr>" + suratMasuk.getRingkasan();
			}
			safeInputhtml = MyHtml.bersihkan(html);
			new ais.ui.util.MyHtml("<font style=\"font-size: x-small;\"><ul>" + safeInputhtml + "</ul></font>").setParent(arg0);

			html = SuratMasukAction.infoDisposisiBagan(suratMasuk);
			new Label().setParent(arg0);
			new ais.ui.util.MyHtml(html).setParent(detailAlur);

			if (suratMasuk.getDisposisiSop() != null) {

				new Label(Common.simpleString(suratMasuk.getKeterangan())).setParent(a);
				A aa;
				(aa = new A()).setParent(a);
				aa.setStyle("font-size:9px;");
				UIClassHelper.applyReadMore(aa, "SOP " + suratMasuk.getDisposisiSop().getKeterangan() + " ("
						+ suratMasuk.getDisposisiSop().getSop().getNama() + ")");
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(suratMasuk.getDisposisiSop().getId(), null, null, true,
								arg0.getTarget());
					}
				});
			} else {
				new Label(suratMasuk.getKeterangan()).setParent(a);
			}

			if (suratMasuk.getDisposisiSop() != null && !suratMasuk.getDisposisiSop().getAktif()) {
				new Label(ais.common.Common.getBahasaConfig("Tidak aktif")).setParent(arg0);
			} else if (edit && tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
					&& tbmuser.ambilDosen() == null && tbmuser.ambilGuru() == null) {
				final MyCheckboxConfig aktif = new MyCheckboxConfig("Aktif");
				aktif.setChecked(suratMasuk.getAktif());
				aktif.setParent(arg0);
				aktif.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						suratMasuk.setAktif(aktif.isChecked());
						Common.refreshSaveOrUpdate(suratMasuk);
					}
				});
			} else {
				new Label(suratMasuk.getAktif() ? "Ya" : "Tidak").setParent(arg0);
			}

			Hbox toolbar = new Hbox();
			GeneralValueObject.tampilKunci(toolbar, suratMasuk, tbmuser, new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					onSearchDefault(event);
				}

			}, false);

			MyToolbarbuttonConfig catatanDisposisi = new MyToolbarbuttonConfig("", "/img/print.png");
			catatanDisposisi.setTooltiptext("Catatan / Cetak / Simpan Disposisi");
			catatanDisposisi.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null
					&& tbmuser.getSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null);
			catatanDisposisi.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AlurPersetujuanSuratMasukStatus status = (AlurPersetujuanSuratMasukStatus) HibernateUtil
							.currentSession().createCriteria(AlurPersetujuanSuratMasukStatus.class)
							.add(Restrictions.isNotNull("kodeUnik")).add(Restrictions.eq("suratMasuk", suratMasuk))
							.addOrder(Order.asc("id")).setMaxResults(1).uniqueResult();
					if (status == null) {
						MyMessageboxConfig.show("Surat ini belum memiliki data disposisi.");
						return;
					}
					ais.action.master.surat.helper.CatatanDisposisiPopupHelper.showMasuk(status, tbmuser,
							(org.zkoss.zk.ui.Component) event.getTarget());
				}
			});
			catatanDisposisi.setParent(toolbar);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("",
					suratMasuk.getDikunci() == null ? "/img/svg/edit-box-line.svg" : "/img/svg/eye.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(suratMasuk);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete && suratMasuk.getDikunci() == null);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Session session = HibernateUtil.currentSession();
											if (SopUtil.hapusDisposisi(session, suratMasuk.getDisposisiSop())) {

												String sql = "delete from surat.klasifikasi_surat_masuk_paremeter_value where surat_masuk = "
														+ suratMasuk.getId();

												session.createSQLQuery(sql).executeUpdate();

												Common.refreshDelete(session, suratMasuk);

												onSearchDefault(event);
											}
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(
													"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
															+ e.getMessage());
										}

									}

								}
							});

				}
			});
			button.setParent(toolbar);
			toolbar.setParent(arg0);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new SuratMasuk());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	protected void initDetail(final SuratMasuk suratMasuk, Component component) throws Exception {
		Tabbox tabbox = new Tabbox();
		tabbox.setParent(component);
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		MyTabConfig tabOpsi = new MyTabConfig("Petunjuk / Opsi Surat Masuk");
		tabOpsi.setParent(tabs);

		MyTabConfig tabDisposisi = new MyTabConfig("Disposisi ke");
		tabDisposisi.setParent(tabs);

		MyTabConfig tabRingkasan = new MyTabConfig("Catatan/Ringkasan");
		tabRingkasan.setParent(tabs);

		MyTabConfig tabGambar = new MyTabConfig("Lampiran Surat");
		tabGambar.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanelOpsi = new ais.ui.util.MyTabpanel();
		tabpanelOpsi.setParent(tabpanels);

		Tabpanel tabpanelDisposisi = new ais.ui.util.MyTabpanel();
		tabpanelDisposisi.setParent(tabpanels);

		tabpanelRingkasan = new ais.ui.util.MyTabpanel();
		tabpanelRingkasan.setParent(tabpanels);

		Tabpanel tabpanelGambar = new ais.ui.util.MyTabpanel();
		tabpanelGambar.setParent(tabpanels);

		tabpanelOpsi.appendChild(initOptional(suratMasuk));

		tabpanelDisposisi.appendChild(SuratMasukAction.initJenisJabatan(suratMasuk, jenisSurats));

		tabpanelGambar.appendChild(
				new SuratMasukPunyaGambarFotoHelper(gridGambar = new MyGrid()).initDetail(suratMasuk, true));
		ringkasan = new MyCkEditor();
		if (suratMasuk.getDikunci() == null) {
			tabpanelRingkasan.appendChild(ringkasan);
		} else {
			tabpanelRingkasan.appendChild(new Html(suratMasuk.getRingkasan() == null ? "" : suratMasuk.getRingkasan()));
		}
		ringkasan.setValue(suratMasuk.getRingkasan() == null ? "" : suratMasuk.getRingkasan());
		ringkasan.setWidth("90%");
		ringkasan.setHeight("100%");

	}

	@SuppressWarnings("unchecked")
	public static Borderlayout initJenisJabatan(final SuratMasuk suratMasuk, final JSONObject jenisSurats) {
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		Tbmuser tbmuser = Common.getCurrentUser();
		Session session = HibernateUtil.currentSession();

		// Pakai-ulang satu AktorLookup utk SEMUA pejabat (jangan pindai daftar pengguna berulang
		// tiap pejabat) + ambil sekaligus pejabat yang sudah didisposisi (hindari COUNT per pejabat)
		// — dua hal inilah yang dulu membuat tab "Disposisi ke" lambat saat form/preview dibuka.
		final SopUtil.AktorLookup aktorLookup = new SopUtil.AktorLookup();
		final java.util.Set<String> pejabatSudahDisposisi = new java.util.HashSet<String>();
		if (suratMasuk != null && suratMasuk.getId() != null) {
			List<Object[]> sudahRows = session.createCriteria(AlurPersetujuanSuratMasukStatus.class)
					.add(Restrictions.isNotNull("kodeUnik")).add(Restrictions.eq("suratMasuk", suratMasuk))
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

				Rows rowsOpsiSuratMasuk = new Rows();
				rowsOpsiSuratMasuk.setParent(grid);

				int index = 0;
				MyFormRow row = new MyFormRow();
				row.setValign("top");
				for (final JenisJabatan jenisJabatan : jenisJabatans) {

					List<Pejabat> pejabats = ConstantValues
							.simpleList(
									session.createCriteria(Pejabat.class)
											.add(Restrictions.eq("jenisJabatan", jenisJabatan)).add(Restrictions
													.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
									Pejabat.class);

					for (final Pejabat pejabat : pejabats) {

						boolean sudahDisposisi = suratMasuk != null && suratMasuk.getId() != null
								&& pejabat.getJenisJabatan() != null && pejabatSudahDisposisi
										.contains(pejabat.getId() + "_" + pejabat.getJenisJabatan().getId());

						if (index % 3 == 0) {
							row = new MyFormRow();
							row.setParent(rowsOpsiSuratMasuk);
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
						checkbox.setDisabled(sudahDisposisi || suratMasuk.getDikunci() != null);
						checkbox.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Long id = new Long(pejabat.getId());
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

	@SuppressWarnings("unchecked")
	private Borderlayout initOptional(final SuratMasuk suratMasuk) {
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
		List<OpsiSuratMasuk> opsiSuratMasuks = session.createCriteria(OpsiSuratMasuk.class)
				.add(Restrictions.and(
						Restrictions.or(Restrictions.isNull("usernamePengguna"),
								Restrictions.ilike("usernamePengguna", "," + tbmuser.getUserId() + ",",
										MatchMode.ANYWHERE)),
						Restrictions.or(Restrictions.isNull("jenisPengguna"),
								Restrictions.ilike("jenisPengguna", "," + tbmuser.hakAkses().getRoleId() + ",",
										MatchMode.ANYWHERE))))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.asc("nama")).list();

		rowsOpsiSuratMasuk = new Rows();
		rowsOpsiSuratMasuk.setParent(grid);

		int index = 0;
		MyFormRow row = new MyFormRow();
		row.setValign("top");
		for (OpsiSuratMasuk opsiSuratMasuk : opsiSuratMasuks) {

			OpsiSuratMasukValue opsiSuratMasukValue = null;
			if (suratMasuk.getId() != null) {
				opsiSuratMasukValue = (OpsiSuratMasukValue) session.createCriteria(OpsiSuratMasukValue.class)
						.add(Restrictions.eq("opsiSuratMasuk", opsiSuratMasuk))
						.add(Restrictions.eq("suratMasuk", suratMasuk)).setMaxResults(1).uniqueResult();
			}

			if (index % 3 == 0) {
				row = new MyFormRow();
				row.setParent(rowsOpsiSuratMasuk);
			}

			if (opsi_surat_hanya_satu) {

				final MyRadioConfig checkbox = new MyRadioConfig(opsiSuratMasuk.getNama());
				row.setValign("top");
				row.setAttribute("checkbox_" + index, checkbox);

				checkbox.setAttribute("opsiSuratMasuk", opsiSuratMasuk);
				checkbox.setAttribute("opsiSuratMasukValue", opsiSuratMasukValue);
				checkbox.setDisabled(suratMasuk.getDikunci() != null);
				checkbox.setChecked(opsiSuratMasukValue != null);

				if (opsiSuratMasuk.getNama() != null && opsiSuratMasuk.getNama().equals("Lain-lain")) {

					Vbox hbox = new Vbox();
					hbox.setParent(row);

					hbox.appendChild(checkbox);

					final Textbox textboxket = new Textbox(
							opsiSuratMasukValue == null ? "" : opsiSuratMasukValue.getKeterangan());
					textboxket.setCols(20);
					textboxket.setRows(2);
					textboxket.setDisabled(suratMasuk.getDikunci() != null);
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
				final MyCheckboxConfig checkbox = new MyCheckboxConfig(opsiSuratMasuk.getNama());
				row.setValign("top");
				row.setAttribute("checkbox_" + index, checkbox);
				checkbox.setDisabled(suratMasuk.getDikunci() != null);
				checkbox.setAttribute("opsiSuratMasuk", opsiSuratMasuk);
				checkbox.setAttribute("opsiSuratMasukValue", opsiSuratMasukValue);

				checkbox.setChecked(opsiSuratMasukValue != null);

				if (opsiSuratMasuk.getNama() != null && opsiSuratMasuk.getNama().equals("Lain-lain")) {

					Vbox hbox = new Vbox();
					hbox.setParent(row);

					hbox.appendChild(checkbox);

					final Textbox textboxket = new Textbox(
							opsiSuratMasukValue == null ? "" : opsiSuratMasukValue.getKeterangan());
					textboxket.setCols(20);
					textboxket.setRows(2);
					textboxket.setDisabled(suratMasuk.getDikunci() != null);
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

	public synchronized static Long getindexSurat(KlasifikasiSuratMasuk klasifikasiSuratMasuk) {
		if (klasifikasiSuratMasuk.getNomorSurat() == null) {
			return 0L;
		}
		Session session = HibernateUtil.currentSession();
		int tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		Number indexO = (Number) session.createCriteria(SuratMasuk.class)
				.createAlias("klasifikasiSuratMasuk", "klasifikasiSuratMasuk", Criteria.LEFT_JOIN)
				.createAlias("klasifikasiSuratMasuk.nomorSurat", "nomorSurat", Criteria.LEFT_JOIN)

				.add(klasifikasiSuratMasuk.getNomorSurat().getUrutBerdasarkanNomor()
						? Restrictions.eq("klasifikasiSuratMasuk.nomorSurat", klasifikasiSuratMasuk.getNomorSurat())

						: (klasifikasiSuratMasuk.getNomorSurat().getUrutBerdasarkanKelompok()
								&& klasifikasiSuratMasuk.getNomorSurat().getKelompokNomorSurat() != null
										? Restrictions.eq("nomorSurat.kelompokNomorSurat",
												klasifikasiSuratMasuk.getNomorSurat().getKelompokNomorSurat())
										: Restrictions.sqlRestriction("true")))

				.add(klasifikasiSuratMasuk.getNomorSurat().getResetUrutanTiapTahun() ? Restrictions.eq("tahun", tahun)
						: Restrictions.sqlRestriction("true"))
				.setProjection(Projections.rowCount()).uniqueResult();

		Long index = indexO == null ? null : indexO.longValue();
		if (index == null) {
			index = 0L;
		}
		return ++index;
	}

	private String generateCode(boolean tambah) {
		KlasifikasiSuratMasuk klasifikasiSuratMasuk = (KlasifikasiSuratMasuk) (this.klasifikasiSuratMasuk
				.getAttribute("klasifikasiSuratMasuk"));
		return SuratMasukAction.generateCode(tambah, klasifikasiSuratMasuk, tanggal.getValue());

	}

	public synchronized static String generateCode(boolean tambah, KlasifikasiSuratMasuk klasifikasiSuratMasuk,
			Date tanggal) {

		if (klasifikasiSuratMasuk == null) {
			return "";
		}

		if (klasifikasiSuratMasuk.getNomorSurat() != null) {
			Long index = klasifikasiSuratMasuk.getNomorSurat().getGunakanIndexUrut()
					? klasifikasiSuratMasuk.getNomorSurat().getNomorIndex()
					: getindexSurat(klasifikasiSuratMasuk);
			if (tambah && klasifikasiSuratMasuk.getNomorSurat().getGunakanIndexUrut()) {
				NomorSurat.tambahIndexNomorSurat(klasifikasiSuratMasuk.getNomorSurat());
			}
			String noAgenda = klasifikasiSuratMasuk.getNomorSurat().format(index, tanggal);
			try {
				noAgenda = org.apache.commons.lang3.StringUtils.replaceIgnoreCase(noAgenda, "KODE_KLASIFIKASI",
						klasifikasiSuratMasuk.getKode());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/SuratMasukAction.java:960");
				// TODO: handle exception
			}
			return noAgenda;
		} else {
			Long index = getindex(klasifikasiSuratMasuk);

			String kodeIndex = "000000" + index;
			kodeIndex = kodeIndex.substring(kodeIndex.length() - 4, kodeIndex.length());

			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			int bulan = calendar.get(Calendar.MONTH);
			int tahun = calendar.get(Calendar.YEAR);

			String noAgenda = (klasifikasiSuratMasuk.getPrefix().equals("") ? ""
					: klasifikasiSuratMasuk.getPrefix() + "/") + kodeIndex + "/" + Common.binaryToRoman(bulan + 1) + "/"
					+ tahun
					+ (klasifikasiSuratMasuk.getPostfix().equals("") ? "" : "/" + klasifikasiSuratMasuk.getPostfix());
			try {
				noAgenda = org.apache.commons.lang3.StringUtils.replaceIgnoreCase(noAgenda, "KODE_KLASIFIKASI",
						klasifikasiSuratMasuk.getKode());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/SuratMasukAction.java:981");
				// TODO: handle exception
			}
			return noAgenda;
		}
	}

	public synchronized static Long getindex(KlasifikasiSuratMasuk klasifikasiSuratMasuk) {
		Session session = HibernateUtil.currentSession();
		int tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		Long index = (Long) session.createCriteria(SuratMasuk.class).add(Restrictions.eq("tahun", tahun))
				.setProjection(Projections.max("index"))
				.add(Restrictions.eq("klasifikasiSuratMasuk", klasifikasiSuratMasuk)).uniqueResult();
		if (index == null) {
			index = 0L;
		}
		return index;
	}

	private final EventListener listener = new EventListener() {

		@Override
		public void onEvent(Event arg0) throws Exception {
			KlasifikasiSuratMasuk thisKlasifikasiSuratMasuk = (KlasifikasiSuratMasuk) (klasifikasiSuratMasuk
					.getAttribute("klasifikasiSuratMasuk"));
			if (thisKlasifikasiSuratMasuk != null) {
				String code = suratMasuk.getKode() == null || suratMasuk.getKode().trim().equals("")
						? generateCode(false)
						: suratMasuk.getKode().trim();
				kode.setValue(code);

				if (thisKlasifikasiSuratMasuk.getAlurPersetujuanSuratMasuk() != null) {
					alurPersetujuanSuratMasuk.setAttribute("alurPersetujuanSuratMasuk",
							thisKlasifikasiSuratMasuk.getAlurPersetujuanSuratMasuk());
					alurPersetujuanSuratMasuk
							.setValue(thisKlasifikasiSuratMasuk.getAlurPersetujuanSuratMasuk() == null ? ""
									: thisKlasifikasiSuratMasuk.getAlurPersetujuanSuratMasuk().toString());
					alurEventListener.onEvent(null);
					alurPersetujuanSuratMasuk.setDisabled(true);
				}

			}
		}
	};
	private Combobox status;
	private Combobox kerahasiaan;
	private Combobox fakultas;
	private Combobox jurusan;
	private EventListener alurEventListener;
	private Vbox vboxAlur;
	private HashSet<JenisJabatan> selectedJenisJabatan;
	private HashSet<JenisJabatan> removedJenisJabatan;
	private Textbox usernamePengguna;
	private MyCheckboxConfig broadcast;
	private Tbmuser tbmuser;
	private JSONObject jenisSurats;
	private DisposisiSop disposisiSop = null;
	private boolean ubah = true;
	private Textbox catatanRevisi;

	private void init(SuratMasuk suratMasuk) throws Exception {

		Tbmuser tbmuser = Common.getCurrentUser();
		if (suratMasuk.getFakultas() == null && tbmuser.ambilFakultas() != null) {
			suratMasuk.setFakultas(tbmuser.ambilFakultas());
		}

		if (suratMasuk.getSatuanKerja() == null && tbmuser.ambilSatuanKerja() != null) {
			suratMasuk.setSatuanKerja(tbmuser.ambilSatuanKerja());
		}

		if (suratMasuk.getJurusan() == null && tbmuser.ambilJurusan() != null) {
			suratMasuk.setJurusan(tbmuser.ambilJurusan());
		}

		if (suratMasuk.getYayasan() == null && tbmuser.ambilYayasan() != null) {
			suratMasuk.setYayasan(tbmuser.ambilYayasan());
		}

		if (suratMasuk.getSekolah() == null && tbmuser.ambilSekolah() != null) {
			suratMasuk.setSekolah(tbmuser.ambilSekolah());
		}

		addWindow.setTitle(suratMasuk.getId() == null ? "Tambah Surat Masuk" : "Ubah Surat Masuk");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		disposisiSop = null;
		center.appendChild(form(suratMasuk, null, save, null));

		East east = new East();
		east.setWidth("60%");
		east.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(east, true);
		east.setAutoscroll(true);

		initDetail(suratMasuk, east);

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
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

		if (suratMasuk.getAlurDitolak() != null) {
			save.setVisible(true);
			cancel.setLabel("Batal");
		}

		else if (!ubah || suratMasuk.getDikunci() != null) {
			save.setVisible(false);
			cancel.setLabel("Tutup");
		}
	}

	private void initKelengkapanBerkas(Vbox vbox, AlurPersetujuanSuratMasuk alurPersetujuanSuratMasuk) {
		Common.clear(vbox);
		if (alurPersetujuanSuratMasuk == null || alurPersetujuanSuratMasuk.getId() == null) {
			return;
		}

		final MyGrid subGrid = new MyGrid();
		vbox.appendChild(subGrid);

		Columns subColumns = new Columns();
		subColumns.setParent(subGrid);
		Column c = new Column("Kepada Yth. :");
		subColumns.appendChild(c);

		Rows subRows = new Rows();
		subRows.setParent(subGrid);

		MyFormRow subRow = new MyFormRow();
		subRow.setStyle("border:0px;background: transparent;");
		subRow.setParent(subRows);
		subRow.setValign("top");
		Session session = HibernateUtil.currentSession();
		session.refresh(alurPersetujuanSuratMasuk);
		selectedJenisJabatan = new HashSet<JenisJabatan>();
		removedJenisJabatan = new HashSet<JenisJabatan>();

		Vbox vboxSkala = new Vbox();
		vboxSkala.setPack("top");
		vboxSkala.setParent(subRow);

		new Label(alurPersetujuanSuratMasuk.getJenisJabatan().getNama()).setParent(vboxSkala);

		session.refresh(alurPersetujuanSuratMasuk);
		TreeMap<String, JenisJabatan> data = new TreeMap<String, JenisJabatan>();
		try {
			for (JenisJabatan jenisJabatan : alurPersetujuanSuratMasuk.getJenisJabatans()) {
				data.put(jenisJabatan.getNama(), jenisJabatan);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/SuratMasukAction.java:1162");
//			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		for (final JenisJabatan jenisJabatan : data.values()) {

			AlurPersetujuanSuratMasukStatus alurPersetujuanSuratMasukStatus = suratMasuk == null
					|| suratMasuk.getId() == null
							? null
							: (AlurPersetujuanSuratMasukStatus) session
									.createCriteria(AlurPersetujuanSuratMasukStatus.class)
									.add(Restrictions.isNotNull("kodeUnik"))
									.add(Restrictions.eq("alurPersetujuanSuratMasuk", alurPersetujuanSuratMasuk))
									.add(Restrictions.eq("suratMasuk", suratMasuk))
									.add(Restrictions.eq("jenisJabatan", jenisJabatan)).setMaxResults(1).uniqueResult();

			System.out.println("alurPersetujuanSuratMasukStatus => " + alurPersetujuanSuratMasukStatus);

			if (alurPersetujuanSuratMasukStatus != null) {
				selectedJenisJabatan.add(jenisJabatan);
				removedJenisJabatan.remove(jenisJabatan);
			}

			final Checkbox checkbox = new Checkbox(jenisJabatan.getNama());
			checkbox.setParent(vboxSkala);
			checkbox.setChecked(alurPersetujuanSuratMasukStatus != null);
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
	}

	@SuppressWarnings("unchecked")
	public void initParameter(Rows rows, KlasifikasiSuratMasuk klasifikasiSuratMasuk) {
		List<Row> myRows = rows.getChildren();
		Session session = HibernateUtil.currentSession();
		List<KlasifikasiSuratMasukParemeter> klasifikasiSuratMasukParemeters = session
				.createCriteria(KlasifikasiSuratMasukParemeter.class)
				.add(Restrictions.eq("klasifikasiSuratMasuk", klasifikasiSuratMasuk)).list();
		for (final KlasifikasiSuratMasukParemeter klasifikasiSuratMasukParemeter : klasifikasiSuratMasukParemeters) {

			KlasifikasiSuratMasukParemeterValue klasifikasiSuratMasukParemeterValue = null;
			if (suratMasuk != null && suratMasuk.getId() != null) {
				klasifikasiSuratMasukParemeterValue = (KlasifikasiSuratMasukParemeterValue) session
						.createCriteria(KlasifikasiSuratMasukParemeterValue.class)
						.add(Restrictions.eq("klasifikasiSuratMasukParemeter", klasifikasiSuratMasukParemeter))
						.add(Restrictions.eq("suratMasuk", suratMasuk)).setMaxResults(1).uniqueResult();
			}

			Row row = null;
			for (Row myrow : myRows) {
				if (myrow.getAttribute("klasifikasiSuratMasukParemeter") != null) {
					KlasifikasiSuratMasukParemeter myklasifikasiSuratMasukParemeter = (KlasifikasiSuratMasukParemeter) myrow
							.getAttribute("klasifikasiSuratMasukParemeter");
					if (myklasifikasiSuratMasukParemeter.getId().equals(klasifikasiSuratMasukParemeter.getId())) {
						row = myrow;
					}
					myrow.setVisible(myklasifikasiSuratMasukParemeter.getKlasifikasiSuratMasuk().getId()
							.equals(klasifikasiSuratMasuk.getId()));

				}
			}
			if (row == null) {
				row = new MyFormRow();
			} else {
				Common.clear(row);
			}

			row.setValign("top");
			row.setAttribute("klasifikasiSuratMasukParemeter", klasifikasiSuratMasukParemeter);
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig(klasifikasiSuratMasukParemeter.getNama()));

			if (klasifikasiSuratMasukParemeter.getTipe().equals(String.class.getName())) {
				final Textbox isi;
				String nilai = klasifikasiSuratMasukParemeter.getNilai();
				row.appendChild(isi = new Textbox(klasifikasiSuratMasukParemeterValue == null ? nilai
						: klasifikasiSuratMasukParemeterValue.getNama()));
				isi.setWidth("90%");
				isi.setRows(2);
				isi.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (suratMasuk != null && suratMasuk.getId() != null) {
							Session session = HibernateUtil.currentSession();
							KlasifikasiSuratMasukParemeterValue klasifikasiSuratMasukParemeterValue = (KlasifikasiSuratMasukParemeterValue) session
									.createCriteria(KlasifikasiSuratMasukParemeterValue.class)
									.add(Restrictions.eq("klasifikasiSuratMasukParemeter",
											klasifikasiSuratMasukParemeter))
									.add(Restrictions.eq("suratMasuk", suratMasuk)).setMaxResults(1).uniqueResult();

							if (klasifikasiSuratMasukParemeterValue == null) {
								klasifikasiSuratMasukParemeterValue = new KlasifikasiSuratMasukParemeterValue();
							}
							klasifikasiSuratMasukParemeterValue.setNama(isi.getValue().trim());
							klasifikasiSuratMasukParemeterValue
									.setKlasifikasiSuratMasukParemeter(klasifikasiSuratMasukParemeter);
							klasifikasiSuratMasukParemeterValue.setSuratMasuk(suratMasuk);
							session.saveOrUpdate(klasifikasiSuratMasukParemeterValue);
						}
						listener.onEvent(arg0);
					}
				});
			} else if (klasifikasiSuratMasukParemeter.getTipe().equals(Integer.class.getName())) {
				Integer nilai = 0;
				try {
					nilai = Integer.parseInt(klasifikasiSuratMasukParemeterValue == null
							? klasifikasiSuratMasukParemeter.getNilai().trim()
							: klasifikasiSuratMasukParemeterValue.getNama().trim());
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
				final Intbox isi;
				row.appendChild(isi = new Intbox(nilai));
				isi.setWidth("90%");
				isi.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (suratMasuk != null && suratMasuk.getId() != null) {
							Session session = HibernateUtil.currentSession();
							KlasifikasiSuratMasukParemeterValue klasifikasiSuratMasukParemeterValue = (KlasifikasiSuratMasukParemeterValue) session
									.createCriteria(KlasifikasiSuratMasukParemeterValue.class)
									.add(Restrictions.eq("klasifikasiSuratMasukParemeter",
											klasifikasiSuratMasukParemeter))
									.add(Restrictions.eq("suratMasuk", suratMasuk)).setMaxResults(1).uniqueResult();

							if (klasifikasiSuratMasukParemeterValue == null) {
								klasifikasiSuratMasukParemeterValue = new KlasifikasiSuratMasukParemeterValue();
							}
							klasifikasiSuratMasukParemeterValue.setNama(isi.getValue() + "");
							klasifikasiSuratMasukParemeterValue
									.setKlasifikasiSuratMasukParemeter(klasifikasiSuratMasukParemeter);
							klasifikasiSuratMasukParemeterValue.setSuratMasuk(suratMasuk);
							session.saveOrUpdate(klasifikasiSuratMasukParemeterValue);
						}
						listener.onEvent(arg0);
					}
				});
			} else if (klasifikasiSuratMasukParemeter.getTipe().equals(Double.class.getName())) {
				Double nilai = 0.0;
				try {
					nilai = Double.parseDouble(klasifikasiSuratMasukParemeterValue == null
							? klasifikasiSuratMasukParemeter.getNilai().trim()
							: klasifikasiSuratMasukParemeterValue.getNama().trim());
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
				final Doublebox isi;
				row.appendChild(isi = new Doublebox(nilai));
				isi.setWidth("90%");
				isi.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (suratMasuk != null && suratMasuk.getId() != null) {
							Session session = HibernateUtil.currentSession();
							KlasifikasiSuratMasukParemeterValue klasifikasiSuratMasukParemeterValue = (KlasifikasiSuratMasukParemeterValue) session
									.createCriteria(KlasifikasiSuratMasukParemeterValue.class)
									.add(Restrictions.eq("klasifikasiSuratMasukParemeter",
											klasifikasiSuratMasukParemeter))
									.add(Restrictions.eq("suratMasuk", suratMasuk)).setMaxResults(1).uniqueResult();

							if (klasifikasiSuratMasukParemeterValue == null) {
								klasifikasiSuratMasukParemeterValue = new KlasifikasiSuratMasukParemeterValue();
							}
							klasifikasiSuratMasukParemeterValue.setNama(isi.getValue() + "");
							klasifikasiSuratMasukParemeterValue
									.setKlasifikasiSuratMasukParemeter(klasifikasiSuratMasukParemeter);
							klasifikasiSuratMasukParemeterValue.setSuratMasuk(suratMasuk);
							session.saveOrUpdate(klasifikasiSuratMasukParemeterValue);
						}
						listener.onEvent(arg0);
					}
				});
			} else if (klasifikasiSuratMasukParemeter.getTipe().equals(Date.class.getName())) {
				Date nilai = ais.ui.util.WaktuUtil.getDate();
				try {
					nilai = Common.dateFormat2.get().parse(klasifikasiSuratMasukParemeterValue == null
							? klasifikasiSuratMasukParemeter.getNilai().trim()
							: klasifikasiSuratMasukParemeterValue.getNama().trim());
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
				final Datebox isi;
				row.appendChild(isi = new MyDatebox(nilai));
				isi.setWidth("90%");
				isi.setFormat(Common.dateFormat2.get().toPattern());
				isi.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (suratMasuk != null && suratMasuk.getId() != null) {
							Session session = HibernateUtil.currentSession();
							KlasifikasiSuratMasukParemeterValue klasifikasiSuratMasukParemeterValue = (KlasifikasiSuratMasukParemeterValue) session
									.createCriteria(KlasifikasiSuratMasukParemeterValue.class)
									.add(Restrictions.eq("klasifikasiSuratMasukParemeter",
											klasifikasiSuratMasukParemeter))
									.add(Restrictions.eq("suratMasuk", suratMasuk)).setMaxResults(1).uniqueResult();

							if (klasifikasiSuratMasukParemeterValue == null) {
								klasifikasiSuratMasukParemeterValue = new KlasifikasiSuratMasukParemeterValue();
							}
							klasifikasiSuratMasukParemeterValue.setNama(Common.dateFormat2.get()
									.format(isi.getValue() == null ? ais.ui.util.WaktuUtil.getDate() : isi.getValue()));
							klasifikasiSuratMasukParemeterValue
									.setKlasifikasiSuratMasukParemeter(klasifikasiSuratMasukParemeter);
							klasifikasiSuratMasukParemeterValue.setSuratMasuk(suratMasuk);
							session.saveOrUpdate(klasifikasiSuratMasukParemeterValue);
						}
						listener.onEvent(arg0);
					}
				});
			}
		}
	}

	@SuppressWarnings("unchecked")
	public boolean onSave(Event event) throws Exception {

		if (noSurat.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show("Mohon maaf, Nomor Surat belum diisi. Langkah yang dapat dilakukan: (1) klik kolom No. Surat pada formulir; (2) ketikkan nomor surat yang sesuai; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (klasifikasiSuratMasuk.getAttribute("klasifikasiSuratMasuk") == null) {
			MyMessageboxConfig.show("Mohon maaf, Klasifikasi Surat Masuk belum dipilih. Langkah yang dapat dilakukan: (1) klik kolom Klasifikasi Surat Masuk; (2) pilih klasifikasi yang sesuai dari daftar; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (kode.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show("Mohon maaf, Nomor Agenda belum diisi. Langkah yang dapat dilakukan: (1) klik kolom No. Agenda pada formulir; (2) ketikkan nomor agenda yang sesuai; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		KlasifikasiSuratMasuk kl = (KlasifikasiSuratMasuk) klasifikasiSuratMasuk.getAttribute("klasifikasiSuratMasuk");

		if (!kl.getTanpaAlur() && alurPersetujuanSuratMasuk.getAttribute("alurPersetujuanSuratMasuk") == null) {
			MyMessageboxConfig.show("Mohon maaf, Alur Surat Masuk belum dipilih. Langkah yang dapat dilakukan: (1) klik kolom Alur Surat Masuk; (2) pilih alur yang sesuai dari daftar; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (asal.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show("Mohon maaf, Asal Surat belum diisi. Langkah yang dapat dilakukan: (1) klik kolom Asal Surat pada formulir; (2) ketikkan asal surat yang sesuai; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (perihal.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show("Mohon maaf, Perihal Surat belum diisi. Langkah yang dapat dilakukan: (1) klik kolom Perihal pada formulir; (2) ketikkan perihal surat secara jelas dan ringkas; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (loker.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, Loker Surat belum dipilih. Langkah yang dapat dilakukan: (1) klik pilihan Loker pada formulir; (2) pilih loker yang sesuai dari daftar; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if ((sifatSurat.getSelectedItem() == null ? null : sifatSurat.getSelectedItem().getValue()) == null) {
			MyMessageboxConfig.show("Mohon maaf, Sifat Surat belum dipilih. Langkah yang dapat dilakukan: (1) klik pilihan Sifat Surat pada formulir; (2) pilih sifat surat yang sesuai dari daftar; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (ringkasan.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show("Mohon maaf, Catatan atau Ringkasan Surat belum diisi. Langkah yang dapat dilakukan: (1) buka tab Ringkasan; (2) isi catatan atau ringkasan surat secara singkat dan jelas; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			tabpanelRingkasan.getLinkedTab().setSelected(true);
			return false;
		}

		if (lampiran.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show("Mohon maaf, Berkas atau Lampiran Surat belum diisi. Langkah yang dapat dilakukan: (1) klik kolom Berkas/Lampiran; (2) isikan nama atau referensi berkas yang disertakan; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		List<Row> rowsFotoGambar = null;
		if (gridGambar != null && gridGambar.getRows() != null) {
			rowsFotoGambar = gridGambar.getRows().getChildren();
			for (Row row : rowsFotoGambar) {
				FotoGambarSuratMasuk fotoGambarSuratMasuk = (FotoGambarSuratMasuk) row
						.getAttribute("fotoGambarSuratMasuk");
				if (fotoGambarSuratMasuk.getSuratMasuk() == null) {
					MyMessageboxConfig.show("Mohon maaf, terdapat baris gambar yang belum diunggah. Langkah yang dapat dilakukan: (1) periksa daftar gambar pada tab Foto; (2) hapus baris yang kosong atau unggah file gambar yang sesuai; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			}
		}

		Session session = HibernateUtil.currentSession();
		try {
			if (suratMasuk.getId() != null) {
				suratMasuk = (SuratMasuk) session.load(SuratMasuk.class, suratMasuk.getId());
			}
		} catch (Exception e) {
			suratMasuk = new SuratMasuk();
		}

		if (suratMasuk.getAlurDitolak() != null && catatanRevisi.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show("Mohon maaf, Catatan Revisi Perbaikan belum diisi. Langkah yang dapat dilakukan: (1) klik kolom Catatan Revisi Perbaikan; (2) tuliskan catatan perbaikan berdasarkan masukan yang diberikan; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		suratMasuk.setJurusan(
				(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? null
						: jurusan.getSelectedItem().getValue()));
		suratMasuk.setFakultas(
				(Fakultas) (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? null
						: fakultas.getSelectedItem().getValue()));

		suratMasuk.setAlurPersetujuanSuratMasuk(
				(AlurPersetujuanSuratMasuk) alurPersetujuanSuratMasuk.getAttribute("alurPersetujuanSuratMasuk"));

		// suratMasuk.setSatuanKerja((SatuanKerja)
		// satuanKerja.getAttribute("satuanKerja"));
		suratMasuk.setKode(kode.getValue());
		suratMasuk.setNama(klasifikasiSuratMasuk.getValue());
		suratMasuk.setKlasifikasiSuratMasuk(
				(KlasifikasiSuratMasuk) klasifikasiSuratMasuk.getAttribute("klasifikasiSuratMasuk"));

		// private MyDatebox tanggalSurat;
		suratMasuk.setTanggalSurat(tanggalSurat.getValue());
		// private Textbox asal;
		suratMasuk.setAsal(asal.getValue());
		// private Textbox noSurat;
		suratMasuk.setNoSurat(noSurat.getValue());

		suratMasuk.setLoker((LokerSurat) (loker.getSelectedItem() == null ? null : loker.getSelectedItem().getValue()));
		// private Textbox perihal;
		suratMasuk.setPerihal(perihal.getValue());
		// private Textbox lampiran;
		suratMasuk.setLampiran(lampiran.getValue());
		// suratMasuk.setKeterangan(keterangan.getValue());
		//
		// private Textbox ringkasan;
		suratMasuk.setRingkasan(ringkasan.getValue());
		// private Textbox koreksi;
		// suratMasuk.setCatatanDisposisi(catatanDisposisi.getValue());

		suratMasuk.setKerahasiaan((String) kerahasiaan.getSelectedItem().getValue());

		suratMasuk.setSifatSurat(
				(SifatSurat) (sifatSurat.getSelectedItem() == null ? null : sifatSurat.getSelectedItem().getValue()));
		suratMasuk.setStatus((String) status.getSelectedItem().getValue());

		suratMasuk.setYayasan(
				(Yayasan) (yayasan.getSelectedItem() == null ? null : yayasan.getSelectedItem().getValue()));
		suratMasuk.setSekolah(
				(Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue()));

		suratMasuk.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));
		suratMasuk.setUsernamePengguna(usernamePengguna.getValue());
		suratMasuk.setBroadcast(broadcast.isChecked());
		suratMasuk.setJenisSurats(jenisSurats.toString());
		if (disposisiSop != null && disposisiSop.getId() != null) {
			suratMasuk.setDisposisiSop(disposisiSop);
		}

		suratMasuk.setCatatanRevisi(catatanRevisi.getValue());

		suratMasuk.setTipe(tipe);

		if (tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.getSiswa() == null) {
			suratMasuk.setKonseptor(tbmuser);
		}

		if (suratMasuk.getId() != null) {

			if (suratMasuk.getIndex() == null) {
				String noAgenda = generateCode(true);
				kode.setValue(noAgenda);
				suratMasuk.setKode(noAgenda);
				Long currentIndex = getindex(
						(KlasifikasiSuratMasuk) klasifikasiSuratMasuk.getAttribute("klasifikasiSuratMasuk"));
				suratMasuk.setIndex(++currentIndex);
			}

			Common.refreshUpdate(session, suratMasuk);
		} else {
			if (suratMasuk.getKode() == null) {
				String noAgenda = generateCode(true);
				kode.setValue(noAgenda);
				suratMasuk.setKode(noAgenda);
			}

			Long currentIndex = getindex(
					(KlasifikasiSuratMasuk) klasifikasiSuratMasuk.getAttribute("klasifikasiSuratMasuk"));
			suratMasuk.setIndex(++currentIndex);
			session.save(suratMasuk);
		}
		session.flush();

		AlurPersetujuanSuratMasukStatus alurDitolak = suratMasuk.getAlurDitolak();
		if (alurDitolak != null) {
			session.refresh(alurDitolak);
			alurDitolak.setCatatanRevisi(catatanRevisi.getValue());
			alurDitolak.setTelahDirevisi(true);
			alurDitolak.setDitolak(false);
			alurDitolak.setDisetujui(false);
			Common.refreshUpdate(session, alurDitolak);
			session.flush();
		}

		if (suratMasuk.getAlurPersetujuanSuratMasuk() != null
				&& suratMasuk.getAlurPersetujuanSuratMasuk().getJenisJabatan() != null) {
			selectedJenisJabatan.add(suratMasuk.getAlurPersetujuanSuratMasuk().getJenisJabatan());
		}

//		if (suratMasuk.getBroadcast()) {
//
//			for (Object o : ConstantValues.ambilBerdasarClass(Tbmuser.class).values()) {
//				Tbmuser tbmuser = (Tbmuser) o;
//				if (tbmuser.getUserId() != null && tbmuser.getEmail() != null && !tbmuser.getEmail().isEmpty()
//						&& (tbmuser.getPegawai() != null || tbmuser.getGuru() != null || tbmuser.getDosen() != null)
//						&& suratMasuk.getUsernamePengguna().contains("," + tbmuser.getUserId() + ",")) {
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
//						AlurPersetujuanSuratMasukStatus alurPersetujuanSuratMasukStatus = (AlurPersetujuanSuratMasukStatus) session
//								.createCriteria(AlurPersetujuanSuratMasukStatus.class)
//								.add(Restrictions.isNotNull("kodeUnik")).add(Restrictions.eq("suratMasuk", suratMasuk))
//								.add(Restrictions.eq("pejabat", pejabat))
//								.add(Restrictions.eq("jenisJabatan", pejabat.getJenisJabatan())).setMaxResults(1)
//								.uniqueResult();
//
//						if (alurPersetujuanSuratMasukStatus == null) {
//							String kodeUnik = AlurPersetujuanSuratMasukStatus.kodeUnik(pejabat, suratMasuk,
//									pejabat.getJenisJabatan(), tbmuser, tbmuser == null ? null : tbmuser.getMahasiswa(),
//									tbmuser == null ? null : tbmuser.getSiswa());
//							if (kodeUnik != null) {
//								alurPersetujuanSuratMasukStatus = (AlurPersetujuanSuratMasukStatus) session
//										.createCriteria(AlurPersetujuanSuratMasukStatus.class)
//										.add(Restrictions.isNotNull("kodeUnik"))
//										.add(Restrictions.eq("kodeUnik", kodeUnik)).setMaxResults(1).uniqueResult();
//							}
//						}
//
//						if (alurPersetujuanSuratMasukStatus == null) {
//							alurPersetujuanSuratMasukStatus = new AlurPersetujuanSuratMasukStatus();
//
//							alurPersetujuanSuratMasukStatus.setKonseptor(tbmuser);
//							alurPersetujuanSuratMasukStatus
//									.setMahasiswa(tbmuser == null ? null : tbmuser.getMahasiswa());
//							alurPersetujuanSuratMasukStatus.setSiswa(tbmuser == null ? null : tbmuser.getSiswa());
//
//							alurPersetujuanSuratMasukStatus.setMasihLanjut(pejabat.getJenisJabatan() != null
//									&& suratMasuk.getAlurPersetujuanSuratMasuk() != null
//									&& suratMasuk.getAlurPersetujuanSuratMasuk().getJenisJabatan() != null
//									&& suratMasuk.getAlurPersetujuanSuratMasuk().getJenisJabatan().getId()
//											.equals(pejabat.getJenisJabatan().getId()));
//
//							alurPersetujuanSuratMasukStatus
//									.setAlurPersetujuanSuratMasuk(suratMasuk.getAlurPersetujuanSuratMasuk());
//							alurPersetujuanSuratMasukStatus.setSuratMasuk(suratMasuk);
//							alurPersetujuanSuratMasukStatus.setJenisJabatan(pejabat.getJenisJabatan());
//							alurPersetujuanSuratMasukStatus.setPejabat(pejabat);
//							alurPersetujuanSuratMasukStatus.setJenisSurats(jenisSurats.toString());
//							session.save(alurPersetujuanSuratMasukStatus);
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

						AlurPersetujuanSuratMasukStatus alurPersetujuanSuratMasukStatus = (AlurPersetujuanSuratMasukStatus) session
								.createCriteria(AlurPersetujuanSuratMasukStatus.class)
								.add(Restrictions.isNotNull("kodeUnik")).add(Restrictions.eq("suratMasuk", suratMasuk))
								.add(Restrictions.eq("pejabat", pejabat))
								.add(Restrictions.eq("jenisJabatan", pejabat.getJenisJabatan())).setMaxResults(1)
								.uniqueResult();

						if (alurPersetujuanSuratMasukStatus == null) {
							String kodeUnik = AlurPersetujuanSuratMasukStatus.kodeUnik(pejabat, suratMasuk,
									pejabat.getJenisJabatan(), tbmuser, tbmuser == null ? null : tbmuser.getMahasiswa(),
									tbmuser == null ? null : tbmuser.getSiswa());
							if (kodeUnik != null) {
								alurPersetujuanSuratMasukStatus = (AlurPersetujuanSuratMasukStatus) session
										.createCriteria(AlurPersetujuanSuratMasukStatus.class)
										.add(Restrictions.isNotNull("kodeUnik"))
										.add(Restrictions.eq("kodeUnik", kodeUnik)).setMaxResults(1).uniqueResult();
							}
						}

						if (alurPersetujuanSuratMasukStatus == null) {
							alurPersetujuanSuratMasukStatus = new AlurPersetujuanSuratMasukStatus();

							alurPersetujuanSuratMasukStatus.setKonseptor(tbmuser);
							alurPersetujuanSuratMasukStatus
									.setMahasiswa(tbmuser == null ? null : tbmuser.getMahasiswa());
							alurPersetujuanSuratMasukStatus.setSiswa(tbmuser == null ? null : tbmuser.getSiswa());

							alurPersetujuanSuratMasukStatus.setMasihLanjut(pejabat.getJenisJabatan() != null
									&& suratMasuk.getAlurPersetujuanSuratMasuk() != null
									&& suratMasuk.getAlurPersetujuanSuratMasuk().getJenisJabatan() != null
									&& suratMasuk.getAlurPersetujuanSuratMasuk().getJenisJabatan().getId()
											.equals(pejabat.getJenisJabatan().getId()));

							alurPersetujuanSuratMasukStatus
									.setAlurPersetujuanSuratMasuk(suratMasuk.getAlurPersetujuanSuratMasuk());
							alurPersetujuanSuratMasukStatus.setSuratMasuk(suratMasuk);
							alurPersetujuanSuratMasukStatus.setJenisJabatan(pejabat.getJenisJabatan());
							alurPersetujuanSuratMasukStatus.setPejabat(pejabat);
							alurPersetujuanSuratMasukStatus.setJenisSurats(jenisSurats.toString());
							session.save(alurPersetujuanSuratMasukStatus);
							session.flush();
						}

					}
				}
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}
		}

		if (selectedJenisJabatan != null && suratMasuk.getAlurPersetujuanSuratMasuk() != null) {

			if (suratMasuk.getAlurPersetujuanSuratMasuk().getJenisJabatan() != null) {
				selectedJenisJabatan.add(suratMasuk.getAlurPersetujuanSuratMasuk().getJenisJabatan());
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
					AlurPersetujuanSuratMasukStatus alurPersetujuanSuratMasukStatus = (AlurPersetujuanSuratMasukStatus) session
							.createCriteria(AlurPersetujuanSuratMasukStatus.class)
							.add(Restrictions.isNotNull("kodeUnik"))
							.add(Restrictions.eq("alurPersetujuanSuratMasuk",
									suratMasuk.getAlurPersetujuanSuratMasuk()))
							.add(Restrictions.eq("suratMasuk", suratMasuk)).add(Restrictions.eq("pejabat", pejabat))
							.add(Restrictions.eq("jenisJabatan", jenisJabatan)).setMaxResults(1).uniqueResult();

					if (alurPersetujuanSuratMasukStatus == null) {
						String kodeUnik = AlurPersetujuanSuratMasukStatus.kodeUnik(pejabat, suratMasuk,
								pejabat.getJenisJabatan(), tbmuser, tbmuser == null ? null : tbmuser.getMahasiswa(),
								tbmuser == null ? null : tbmuser.getSiswa());
						if (kodeUnik != null) {
							alurPersetujuanSuratMasukStatus = (AlurPersetujuanSuratMasukStatus) session
									.createCriteria(AlurPersetujuanSuratMasukStatus.class)
									.add(Restrictions.isNotNull("kodeUnik")).add(Restrictions.eq("kodeUnik", kodeUnik))
									.setMaxResults(1).uniqueResult();
						}
					}

					if (alurPersetujuanSuratMasukStatus == null) {
						alurPersetujuanSuratMasukStatus = new AlurPersetujuanSuratMasukStatus();
						alurPersetujuanSuratMasukStatus.setKonseptor(tbmuser);
						alurPersetujuanSuratMasukStatus.setMahasiswa(tbmuser == null ? null : tbmuser.getMahasiswa());
						alurPersetujuanSuratMasukStatus.setSiswa(tbmuser == null ? null : tbmuser.getSiswa());
						alurPersetujuanSuratMasukStatus
								.setMasihLanjut(suratMasuk.getAlurPersetujuanSuratMasuk().getJenisJabatan() != null
										&& suratMasuk.getAlurPersetujuanSuratMasuk().getJenisJabatan().getId()
												.equals(jenisJabatan.getId()));

						alurPersetujuanSuratMasukStatus
								.setAlurPersetujuanSuratMasuk(suratMasuk.getAlurPersetujuanSuratMasuk());
						alurPersetujuanSuratMasukStatus.setSuratMasuk(suratMasuk);
						alurPersetujuanSuratMasukStatus.setJenisJabatan(jenisJabatan);
						alurPersetujuanSuratMasukStatus.setPejabat(pejabat);
						session.save(alurPersetujuanSuratMasukStatus);
						session.flush();
					}
				}
			}
		}

		if (removedJenisJabatan != null) {
			for (JenisJabatan jenisJabatan : removedJenisJabatan) {
				AlurPersetujuanSuratMasukStatus alurPersetujuanSuratMasukStatus = (AlurPersetujuanSuratMasukStatus) session
						.createCriteria(AlurPersetujuanSuratMasukStatus.class).add(Restrictions.isNotNull("kodeUnik"))
						.add(Restrictions.eq("alurPersetujuanSuratMasuk", suratMasuk.getAlurPersetujuanSuratMasuk()))
						.add(Restrictions.eq("suratMasuk", suratMasuk))
						.add(Restrictions.eq("jenisJabatan", jenisJabatan)).setMaxResults(1).uniqueResult();
				if (alurPersetujuanSuratMasukStatus != null) {
					session.delete(alurPersetujuanSuratMasukStatus);
					session.flush();
				}

			}
		}

		List<Row> rowsOpsi = rowsOpsiSuratMasuk.getChildren();
		for (Row row : rowsOpsi) {
			Set<String> keys = row.getAttributes().keySet();
			for (String key : keys) {
				if (key.contains("checkbox")) {
					Checkbox checkbox = (Checkbox) row.getAttribute(key);

					OpsiSuratMasuk opsiSuratMasuk = (OpsiSuratMasuk) checkbox.getAttribute("opsiSuratMasuk");
					OpsiSuratMasukValue opsiSuratMasukValue = (OpsiSuratMasukValue) checkbox
							.getAttribute("opsiSuratMasukValue");

					if (checkbox.isChecked()) {
						if (opsiSuratMasukValue == null) {
							opsiSuratMasukValue = new OpsiSuratMasukValue();
						}

						Textbox textboxket = (Textbox) checkbox.getAttribute("textboxket");
						opsiSuratMasukValue.setKeterangan(
								textboxket == null || textboxket.getValue().trim().isEmpty() ? opsiSuratMasuk.getNama()
										: textboxket.getValue());
						opsiSuratMasukValue.setNama(
								textboxket == null || textboxket.getValue().trim().isEmpty() ? opsiSuratMasuk.getNama()
										: textboxket.getValue());
						opsiSuratMasukValue.setSuratMasuk(suratMasuk);
						opsiSuratMasukValue.setOpsiSuratMasuk(opsiSuratMasuk);
						session.saveOrUpdate(opsiSuratMasukValue);
					}

					if (!checkbox.isChecked() && opsiSuratMasukValue != null) {
						session.delete(opsiSuratMasukValue);
					}
				}
			}
		}

		if (rowsFotoGambar != null) {
			Session mysession = StreamingHibernateUtil.getInstance().currentSession();
			try {
				mysession.getTransaction().begin();
				for (Row row : rowsFotoGambar) {
					FotoGambarSuratMasuk fotoGambarSuratMasuk = (FotoGambarSuratMasuk) row
							.getAttribute("fotoGambarSuratMasuk");
					if (fotoGambarSuratMasuk.getId() == null || fotoGambarSuratMasuk.getSuratMasuk() == null
							|| !fotoGambarSuratMasuk.getSuratMasuk().equals(suratMasuk.getId())) {
						fotoGambarSuratMasuk.setSuratMasuk(suratMasuk.getId());
						mysession.saveOrUpdate(fotoGambarSuratMasuk);
						System.out.println("Simpan lampiran " + fotoGambarSuratMasuk.getNama());
					}
				}
				mysession.getTransaction().commit();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}

			StreamingHibernateUtil.getInstance().closeSession();
		}

		if (rows != null) {
			List<Row> myRows = rows.getChildren();

			for (Row row : myRows) {
				KlasifikasiSuratMasukParemeter klasifikasiSuratMasukParemeter = (KlasifikasiSuratMasukParemeter) row
						.getAttribute("klasifikasiSuratMasukParemeter");
				if (klasifikasiSuratMasukParemeter == null || !row.isVisible()) {
					continue;
				}

				Object coponent = row.getChildren().get(1);

				if (coponent instanceof Textbox) {
					Textbox isi = (Textbox) coponent;
					KlasifikasiSuratMasukParemeterValue klasifikasiSuratMasukParemeterValue = (KlasifikasiSuratMasukParemeterValue) session
							.createCriteria(KlasifikasiSuratMasukParemeterValue.class)
							.add(Restrictions.eq("klasifikasiSuratMasukParemeter", klasifikasiSuratMasukParemeter))
							.add(Restrictions.eq("suratMasuk", suratMasuk)).setMaxResults(1).uniqueResult();

					if (klasifikasiSuratMasukParemeterValue == null) {
						klasifikasiSuratMasukParemeterValue = new KlasifikasiSuratMasukParemeterValue();
					}
					klasifikasiSuratMasukParemeterValue.setNama(isi.getValue().trim());
					klasifikasiSuratMasukParemeterValue
							.setKlasifikasiSuratMasukParemeter(klasifikasiSuratMasukParemeter);
					klasifikasiSuratMasukParemeterValue.setSuratMasuk(suratMasuk);
					session.saveOrUpdate(klasifikasiSuratMasukParemeterValue);
				} else if (coponent instanceof Intbox) {
					Intbox isi = (Intbox) coponent;
					KlasifikasiSuratMasukParemeterValue klasifikasiSuratMasukParemeterValue = (KlasifikasiSuratMasukParemeterValue) session
							.createCriteria(KlasifikasiSuratMasukParemeterValue.class)
							.add(Restrictions.eq("klasifikasiSuratMasukParemeter", klasifikasiSuratMasukParemeter))
							.add(Restrictions.eq("suratMasuk", suratMasuk)).setMaxResults(1).uniqueResult();

					if (klasifikasiSuratMasukParemeterValue == null) {
						klasifikasiSuratMasukParemeterValue = new KlasifikasiSuratMasukParemeterValue();
					}
					klasifikasiSuratMasukParemeterValue.setNama(isi.getValue() + "");
					klasifikasiSuratMasukParemeterValue
							.setKlasifikasiSuratMasukParemeter(klasifikasiSuratMasukParemeter);
					klasifikasiSuratMasukParemeterValue.setSuratMasuk(suratMasuk);
					session.saveOrUpdate(klasifikasiSuratMasukParemeterValue);
				} else if (coponent instanceof Doublebox) {
					Doublebox isi = (Doublebox) coponent;
					KlasifikasiSuratMasukParemeterValue klasifikasiSuratMasukParemeterValue = (KlasifikasiSuratMasukParemeterValue) session
							.createCriteria(KlasifikasiSuratMasukParemeterValue.class)
							.add(Restrictions.eq("klasifikasiSuratMasukParemeter", klasifikasiSuratMasukParemeter))
							.add(Restrictions.eq("suratMasuk", suratMasuk)).setMaxResults(1).uniqueResult();

					if (klasifikasiSuratMasukParemeterValue == null) {
						klasifikasiSuratMasukParemeterValue = new KlasifikasiSuratMasukParemeterValue();
					}
					klasifikasiSuratMasukParemeterValue.setNama(isi.getValue() + "");
					klasifikasiSuratMasukParemeterValue
							.setKlasifikasiSuratMasukParemeter(klasifikasiSuratMasukParemeter);
					klasifikasiSuratMasukParemeterValue.setSuratMasuk(suratMasuk);
					session.saveOrUpdate(klasifikasiSuratMasukParemeterValue);
				} else if (coponent instanceof Datebox) {
					Datebox isi = (Datebox) coponent;
					KlasifikasiSuratMasukParemeterValue klasifikasiSuratMasukParemeterValue = (KlasifikasiSuratMasukParemeterValue) session
							.createCriteria(KlasifikasiSuratMasukParemeterValue.class)
							.add(Restrictions.eq("klasifikasiSuratMasukParemeter", klasifikasiSuratMasukParemeter))
							.add(Restrictions.eq("suratMasuk", suratMasuk)).setMaxResults(1).uniqueResult();

					if (klasifikasiSuratMasukParemeterValue == null) {
						klasifikasiSuratMasukParemeterValue = new KlasifikasiSuratMasukParemeterValue();
					}
					klasifikasiSuratMasukParemeterValue.setNama(Common.dateFormat2.get()
							.format(isi.getValue() == null ? ais.ui.util.WaktuUtil.getDate() : isi.getValue()));
					klasifikasiSuratMasukParemeterValue
							.setKlasifikasiSuratMasukParemeter(klasifikasiSuratMasukParemeter);
					klasifikasiSuratMasukParemeterValue.setSuratMasuk(suratMasuk);
					session.saveOrUpdate(klasifikasiSuratMasukParemeterValue);
				}
			}
		}

		final AlurPersetujuanSuratMasukStatus alurPersetujuanSuratMasukStatus = checkAlurPersetujuanSuratMasukStatus(
				suratMasuk);

		Common.createDefaultTimerNoBusy((new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				BroadcastHelper.kirimEmailSuratMasuk(suratMasuk, alurPersetujuanSuratMasukStatus,
						tbmuser);
			}
		}));

		return true;
	}

	private AlurPersetujuanSuratMasukStatus checkAlurPersetujuanSuratMasukStatus(final SuratMasuk suratMasuk)
			throws Exception {
		Session session = HibernateUtil.currentSession();
		Integer count = ((Number) session.createCriteria(AlurPersetujuanSuratMasukStatus.class)
				.add(Restrictions.isNotNull("kodeUnik"))
				.createAlias("alurPersetujuanSuratMasuk", "alurPersetujuanSuratMasuk")
				.add(Restrictions.isNull("alurPersetujuanSuratMasuk.parent"))
				.add(Restrictions.eq("suratMasuk", suratMasuk)).setProjection(Projections.rowCount()).uniqueResult())
				.intValue();

		AlurPersetujuanSuratMasukStatus alurPersetujuanSuratMasukStatus = (AlurPersetujuanSuratMasukStatus) session
				.createCriteria(AlurPersetujuanSuratMasukStatus.class).add(Restrictions.isNotNull("kodeUnik"))
				.add(Restrictions.eq("suratMasuk", suratMasuk))
				.createAlias("alurPersetujuanSuratMasuk", "alurPersetujuanSuratMasuk")
				.add(Restrictions.isNull("alurPersetujuanSuratMasuk.parent")).setMaxResults(1).uniqueResult();
		if (count.equals(0) && suratMasuk.getAlurPersetujuanSuratMasuk() != null) {

			AlurPersetujuanSuratMasuk alurPersetujuanSuratMasuk = suratMasuk.getAlurPersetujuanSuratMasuk();
			alurPersetujuanSuratMasukStatus = new AlurPersetujuanSuratMasukStatus();

			alurPersetujuanSuratMasukStatus.setKonseptor(tbmuser);
			alurPersetujuanSuratMasukStatus.setMahasiswa(tbmuser == null ? null : tbmuser.getMahasiswa());
			alurPersetujuanSuratMasukStatus.setSiswa(tbmuser == null ? null : tbmuser.getSiswa());

			alurPersetujuanSuratMasukStatus.setAlurPersetujuanSuratMasuk(alurPersetujuanSuratMasuk);
			alurPersetujuanSuratMasukStatus.setSuratMasuk(suratMasuk);
			alurPersetujuanSuratMasukStatus.setKeterangan(suratMasuk.getCatatanDisposisi());
			session.save(alurPersetujuanSuratMasukStatus);

			while (alurPersetujuanSuratMasuk.getParent() != null) {
				alurPersetujuanSuratMasuk = alurPersetujuanSuratMasuk.getParent();
				alurPersetujuanSuratMasukStatus = new AlurPersetujuanSuratMasukStatus();

				alurPersetujuanSuratMasukStatus.setKonseptor(tbmuser);
				alurPersetujuanSuratMasukStatus.setMahasiswa(tbmuser == null ? null : tbmuser.getMahasiswa());
				alurPersetujuanSuratMasukStatus.setSiswa(tbmuser == null ? null : tbmuser.getSiswa());

				alurPersetujuanSuratMasukStatus.setAlurPersetujuanSuratMasuk(alurPersetujuanSuratMasuk);
				alurPersetujuanSuratMasukStatus.setSuratMasuk(suratMasuk);
				session.save(alurPersetujuanSuratMasukStatus);
			}

		}

		if (alurPersetujuanSuratMasukStatus != null) {
			SuratMasukAction.cetakDisposisi(alurPersetujuanSuratMasukStatus, tbmuser);
		}

		return alurPersetujuanSuratMasukStatus;
	}

	public static File cetakDisposisi(AlurPersetujuanSuratMasukStatus masukStatus, Tbmuser tbmuser) throws Exception {
		return cetakDisposisi(masukStatus, true, tbmuser);
	}

	@SuppressWarnings("unchecked")
	public static File cetakDisposisi(AlurPersetujuanSuratMasukStatus masukStatus, boolean cetak, Tbmuser tbmuser)
			throws Exception {
		SuratMasuk suratMasuk = masukStatus.getSuratMasuk();
		@SuppressWarnings("rawtypes")
		Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("qr.surat", suratMasuk.ttdQr());
		parameters.put("disposisi.qr.surat", masukStatus.ttdQr());

		SuratUtil.initDefaultKop(parameters, tbmuser, suratMasuk.getSatuanKerja());

		if (suratMasuk.getAlurPersetujuanSuratMasuk() != null
				&& suratMasuk.getAlurPersetujuanSuratMasuk().getSatuanKerja() != null) {
			PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
			SuratUtil.initDefaultKopAja(masukStatus.getAlurPersetujuanSuratMasuk().getSatuanKerja(), perguruanTinggi,
					parameters, "kop_alur");
		} else if (suratMasuk.getSatuanKerja() != null) {
			PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
			SuratUtil.initDefaultKopAja(suratMasuk.getSatuanKerja(), perguruanTinggi, parameters, "kop_alur");
		} else {
			parameters.put("kop_alur", "");
		}

		parameters.put("index",
				suratMasuk.getKlasifikasiSuratMasuk() == null ? "" : suratMasuk.getKlasifikasiSuratMasuk().getNama());
		parameters.put("kode",
				suratMasuk.getKlasifikasiSuratMasuk() == null ? "" : suratMasuk.getKlasifikasiSuratMasuk().getKode());
		parameters.put("berkas", suratMasuk.getLampiran());

		String tgl_no = Common.dateFormat4.get().format(suratMasuk.getTanggalSurat());
		tgl_no += ", " + suratMasuk.getKode();
		tgl_no += ", " + suratMasuk.getNoSurat();
		parameters.put("no", suratMasuk.getNoSurat());
		parameters.put("agenda", suratMasuk.getKode());
		parameters.put("tgl_no", tgl_no);
		parameters.put("nomor", suratMasuk.getNoSurat());
		parameters.put("asal", suratMasuk.getAsal());
		parameters.put("ringkasan", suratMasuk.getRingkasan());
		parameters.put("status", suratMasuk.getStatus());
		parameters.put("sifat", suratMasuk.getSifat());
		parameters.put("kerahasiaan", suratMasuk.getKerahasiaan());
		parameters.put("lampiran", suratMasuk.getLampiran());
		parameters.put("perihal", suratMasuk.getPerihal());

		List<String> opsi = HibernateUtil.currentSession().createCriteria(OpsiSuratMasukValue.class)
				.add(Restrictions.eq("suratMasuk", suratMasuk)).setProjection(Projections.property("nama")).list();
		String oo = "";
		int i = 1;
		for (String s : opsi) {
			oo += (i + ". " + s + "\n");
			i++;
		}
		parameters.put("opsi", oo);

		String tgl_diterima = Common.dateFormat4.get().format(suratMasuk.getTanggal());
		parameters.put("tgl_diterima", tgl_diterima);
		parameters.put("tgl", Common.dateFormat4.get().format(suratMasuk.getTanggalSurat()));
		parameters.put("isi_disposisi", masukStatus.getKeterangan());

		Session session = HibernateUtil.currentSession();
		String diteruskan_kepada = "";
		String disposisi = "";
		i = 1;
		int ii = 1;
		List<Long> pejabatas = new ArrayList<Long>();
		List<AlurPersetujuanSuratMasukStatus> alurPersetujuanSuratMasukStatuses = session
				.createCriteria(AlurPersetujuanSuratMasukStatus.class).add(Restrictions.isNotNull("kodeUnik"))
				.add(Restrictions.eq("suratMasuk", masukStatus.getSuratMasuk())).addOrder(Order.asc("id")).list();
		List<File> files = new ArrayList<File>();
		for (AlurPersetujuanSuratMasukStatus alurPersetujuanSuratMasukStatus : alurPersetujuanSuratMasukStatuses) {

			String nama = "";
			try {
				LampiranLain lampiranLain = LampiranLain.ambil(alurPersetujuanSuratMasukStatus.getId(),
						AlurPersetujuanSuratMasukStatus.class.getName());
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

			JenisJabatan jenisJabatan = alurPersetujuanSuratMasukStatus.getJenisJabatan();

			String n = ". " + (jenisJabatan == null ? "" : jenisJabatan.getNama());
			if (!diteruskan_kepada.contains(n)) {
				diteruskan_kepada += (ii + n + "\n");
				ii++;
			}

			AlurPersetujuanSuratMasukStatus s = alurPersetujuanSuratMasukStatus;

			if (s.getPejabat() != null) {
				pejabatas.add(s.getPejabat().getId());

				if (alurPersetujuanSuratMasukStatus.getDisetujui()) {
					try {
						SuratUtil.ttdpejabat(s.getPejabat(), parameters, "setujui.");
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/SuratMasukAction.java:2187");
						// TODO: handle exception
					}
				} else {
					try {
						SuratUtil.ttdpejabat(s.getPejabat(), parameters, "belum.");
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/SuratMasukAction.java:2193");
						// TODO: handle exception
					}
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
					+ (s.getDisetujui() ? "Disetujui" : (s.getDitolak() ? "Ditolak" : ""))

					+ "\tHari/tgl : "
					+ (s.getWaktuPersetujuan() == null ? "" : (Common.dateFormat4.get().format(s.getWaktuPersetujuan())))
					+ "\n\n");

			i++;
		}

		String tgl_diteruskan = Common.dateFormat4.get().format(masukStatus.getTanggal_dirubah());
		parameters.put("tgl_diteruskan", tgl_diteruskan);

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

				for (JenisJabatan jenisJabatan : jenisJabatans) {

					List<Pejabat> pejabats = ConstantValues.simpleList(
							session.createCriteria(Pejabat.class)
									.add(Restrictions.or(Restrictions.isNotNull("pegawai"),
											Restrictions.or(Restrictions.isNotNull("guru"),
													Restrictions.isNotNull("dosen"))))
									.add(pejabatas.isEmpty() ? Restrictions.sqlRestriction("true")
											: Restrictions.not(Restrictions.in("id", pejabatas)))
									.add(Restrictions.eq("jenisJabatan", jenisJabatan))
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
							Pejabat.class);

					for (Pejabat aa : pejabats) {
						if (aa != null) {
							int count = suratMasuk == null || suratMasuk.getId() == null ? 0
									: ((Number) session.createCriteria(AlurPersetujuanSuratMasukStatus.class)
											.add(Restrictions.isNotNull("kodeUnik"))
											.add(Restrictions.eq("suratMasuk", suratMasuk))
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

		LampiranLain lainMahasiswa = LampiranLain.ambil(suratMasuk.getKlasifikasiSuratMasuk().getId(),
				LampiranLain.FILE_JRXML_LAYOUT_DISPOSISI_MASUK);

		if (!files.isEmpty()) {

			File fileHasil;

			if (lainMahasiswa != null && lainMahasiswa.getId() != null) {
				fileHasil = Report.generateCompileFileReport(Report.PDF, parameters,
						lainMahasiswa.ambilFile().getAbsolutePath(), ais.ui.util.WaktuUtil.getDate(), false);
			} else {
				fileHasil = Report.generateFileReport(Report.PDF, parameters, "surat/disposisi",
						suratMasuk.getTanggal_dirubah(), new Toolbar());
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
						Report.generatePDFReport(Report.PDF, parameters, "surat/disposisi",
								suratMasuk.getTanggal_dirubah());
					}
				} else {
					if (lainMahasiswa != null && lainMahasiswa.getId() != null) {
						fileHasil = Report.generateCompileFileReport(Report.PDF, parameters,
								lainMahasiswa.ambilFile().getAbsolutePath(), ais.ui.util.WaktuUtil.getDate());
					} else {
						fileHasil = Report.generateFileReport(Report.PDF, parameters, "surat/disposisi",
								suratMasuk.getTanggal_dirubah(), new Toolbar());
					}
				}
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}
			return fileHasil;
		}

	}

	@SuppressWarnings("unchecked")
	public static String infoDisposisi(SuratMasuk suratMasuk) {
		String html = "";
		Session session = HibernateUtil.currentSession();
		List<AlurPersetujuanSuratMasukStatus> alurPersetujuanSuratMasukStatuss = session
				.createCriteria(AlurPersetujuanSuratMasukStatus.class).add(Restrictions.isNotNull("kodeUnik"))
				.add(Restrictions.eq("suratMasuk", suratMasuk)).addOrder(Order.asc("id")).list();

		for (AlurPersetujuanSuratMasukStatus myAlurPersetujuanSuratMasukStatus : alurPersetujuanSuratMasukStatuss) {

			String url = "";
			String nama = "";
			try {
				LampiranLain lampiranLain = LampiranLain.ambil(myAlurPersetujuanSuratMasukStatus.getId(),
						AlurPersetujuanSuratMasukStatus.class.getName());
				if (lampiranLain != null) {
					nama = lampiranLain.getNama();
					url = lampiranLain.createLinkUri(false);
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				ais.common.Common.tampilErrorJikaAdmin(e);
			}

			if (myAlurPersetujuanSuratMasukStatus.getJenisJabatan() == null
					&& myAlurPersetujuanSuratMasukStatus.getAlurPersetujuanSuratMasuk() != null
					&& myAlurPersetujuanSuratMasukStatus.getAlurPersetujuanSuratMasuk().getJenisJabatan() != null) {
				html += "<li>" + myAlurPersetujuanSuratMasukStatus.getAlurPersetujuanSuratMasuk() + " : "
						+ (myAlurPersetujuanSuratMasukStatus.getDisetujui()
								? ("<font style=\"font-size: x-small;color:blue;font-weight: bolder;font-family: Poppins, Helvetica, 'sans-serif';\">Sudah ditindak-lanjuti "
										+ (myAlurPersetujuanSuratMasukStatus.getPejabat() == null
												|| myAlurPersetujuanSuratMasukStatus.getPejabat().getPegawai() == null
														? (myAlurPersetujuanSuratMasukStatus.getPejabat()
																.getDosen() == null
																		? ""
																		: " " + myAlurPersetujuanSuratMasukStatus
																				.getPejabat().getDosen().getNama())
														: " " + myAlurPersetujuanSuratMasukStatus.getPejabat()
																.getPegawai().getNama())
										+ (!myAlurPersetujuanSuratMasukStatus.getKeterangan().trim().isEmpty()
												? " dengan catatan \""
														+ myAlurPersetujuanSuratMasukStatus.getKeterangan() + "\""
												: "")
										+ (myAlurPersetujuanSuratMasukStatus.getWaktuPersetujuan() == null ? ""
												: " pada waktu " + Common.dateFormat3.get().format(
														myAlurPersetujuanSuratMasukStatus.getWaktuPersetujuan()))
										+ "</font>")

								:

								myAlurPersetujuanSuratMasukStatus.getDitolak()
										? ("<font style=\"font-size: x-small;color:red;font-weight: bolder;font-family: Poppins, Helvetica, 'sans-serif';\">Ditolak "
												+ (myAlurPersetujuanSuratMasukStatus.getPejabat() == null
														|| myAlurPersetujuanSuratMasukStatus.getPejabat()
																.getPegawai() == null
																		? (myAlurPersetujuanSuratMasukStatus
																				.getPejabat().getDosen() == null
																						? ""
																						: " " + myAlurPersetujuanSuratMasukStatus
																								.getPejabat().getDosen()
																								.getNama())
																		: " " + myAlurPersetujuanSuratMasukStatus
																				.getPejabat().getPegawai().getNama())
												+ (!myAlurPersetujuanSuratMasukStatus.getKeterangan().trim().isEmpty()
														? " dengan catatan \""
																+ myAlurPersetujuanSuratMasukStatus.getKeterangan()
																+ "\""
														: "")
												+ (myAlurPersetujuanSuratMasukStatus.getWaktuDitolak() == null ? ""
														: " pada waktu " + Common.dateFormat3.get().format(
																myAlurPersetujuanSuratMasukStatus.getWaktuDitolak()))
												+ "</font>")

										: "<font style=\"font-size: x-small;color:red;font-weight: bolder;font-family: Poppins, Helvetica, 'sans-serif';\">Belum ditindak lanjuti"
												+ (myAlurPersetujuanSuratMasukStatus.getPejabat() == null ? ""
														: " " + myAlurPersetujuanSuratMasukStatus.getPejabat()
																.getNama())
												+ "</font>")

						+ (url.isEmpty() ? "" : ", <a href='" + url + "' target='_blank'>" + nama + "</a>") + "</li>";

			} else if (myAlurPersetujuanSuratMasukStatus.getJenisJabatan() != null) {
				html += "<li>" + myAlurPersetujuanSuratMasukStatus.getJenisJabatan().getNama() + " : "
						+ (myAlurPersetujuanSuratMasukStatus.getDisetujui()
								? ("<font style=\"font-size: x-small;color:blue;font-weight: bolder;font-family: Poppins, Helvetica, 'sans-serif';\">Sudah ditindak-lanjuti "
										+ (myAlurPersetujuanSuratMasukStatus.getPejabat() == null
												|| myAlurPersetujuanSuratMasukStatus.getPejabat().getPegawai() == null
														? (myAlurPersetujuanSuratMasukStatus.getPejabat()
																.getDosen() == null
																		? ""
																		: " " + myAlurPersetujuanSuratMasukStatus
																				.getPejabat().getDosen().getNama())
														: " " + myAlurPersetujuanSuratMasukStatus.getPejabat()
																.getPegawai().getNama())
										+ (!myAlurPersetujuanSuratMasukStatus.getKeterangan().trim().isEmpty()
												? " dengan catatan \""
														+ myAlurPersetujuanSuratMasukStatus.getKeterangan() + "\""
												: "")
										+ (myAlurPersetujuanSuratMasukStatus.getWaktuPersetujuan() == null ? ""
												: " pada waktu " + Common.dateFormat3.get().format(
														myAlurPersetujuanSuratMasukStatus.getWaktuPersetujuan()))
										+ "</font>")

								:

								myAlurPersetujuanSuratMasukStatus.getDitolak()
										? ("<font style=\"font-size: x-small;color:red;font-weight: bolder;font-family: Poppins, Helvetica, 'sans-serif';\">Ditolak "
												+ (myAlurPersetujuanSuratMasukStatus.getPejabat() == null
														|| myAlurPersetujuanSuratMasukStatus.getPejabat()
																.getPegawai() == null
																		? (myAlurPersetujuanSuratMasukStatus
																				.getPejabat().getDosen() == null
																						? ""
																						: " " + myAlurPersetujuanSuratMasukStatus
																								.getPejabat().getDosen()
																								.getNama())
																		: " " + myAlurPersetujuanSuratMasukStatus
																				.getPejabat().getPegawai().getNama())
												+ (!myAlurPersetujuanSuratMasukStatus.getKeterangan().trim().isEmpty()
														? " dengan catatan \""
																+ myAlurPersetujuanSuratMasukStatus.getKeterangan()
																+ "\""
														: "")
												+ (myAlurPersetujuanSuratMasukStatus.getWaktuDitolak() == null ? ""
														: " pada waktu " + Common.dateFormat3.get().format(
																myAlurPersetujuanSuratMasukStatus.getWaktuDitolak()))
												+ "</font>")

										: "<font style=\"font-size: x-small;color:red;font-weight: bolder;font-family: Poppins, Helvetica, 'sans-serif';\">Belum ditindak lanjuti"
												+ (myAlurPersetujuanSuratMasukStatus.getPejabat() == null ? ""
														: " " + myAlurPersetujuanSuratMasukStatus.getPejabat()
																.getNama())
												+ "</font>")

						+ (url.isEmpty() ? "" : ", <a href='" + url + "' target='_blank'>" + nama + "</a>") + "</li>";
			}
		}

//		if (suratMasuk.getAlurDitolak() != null && suratMasuk.getAlurDitolak().getTelahDirevisi()) {
//			html += "<li style=\"font-size: x-small;color:blue;font-weight: bolder;font-family: Poppins, Helvetica, 'sans-serif';\">Direvisi dengan catatan : "
//					+ suratMasuk.getAlurDitolak().getCatatanRevisi() + "</li>";
//
//			html += "<li style=\"font-size: x-small;color:red;font-weight: bolder;font-family: Poppins, Helvetica, 'sans-serif';\">Sebelumnya ditolak dengan catatan : "
//					+ suratMasuk.getAlurDitolak().getKeterangan() + "</li>";
//
//		} else if (suratMasuk.getAlurDitolak() != null && suratMasuk.getAlurDitolak().getDitolak()) {
//			html += "<li style=\"font-size: x-small;color:red;font-weight: bolder;font-family: Poppins, Helvetica, 'sans-serif';\">Ditolak dengan catatan : "
//					+ suratMasuk.getAlurDitolak().getKeterangan() + "</li>";
//		}
		
		
		String safeInputhtml = MyHtml.bersihkan(html); 

		html = "<font style=\"font-size: x-small;\"><ul>" + safeInputhtml + "</ul></font>";
		return html;
	}

	private static final class DisposisiMasukChip {
		int nomor;
		String label;
		String penerima;
		String catatan;
		String status;
		String warna;
		String latar;
		String tooltip;
	}

	/**
	 * Informasi disposisi surat masuk dalam format kartu per grup sesuai setup
	 * Daftar Pegawai Pada Disposisi Surat.
	 */
	public static String infoDisposisiBagan(SuratMasuk suratMasuk) {
		Session session = HibernateUtil.currentSession();
		List<AlurPersetujuanSuratMasukStatus> list = session.createCriteria(AlurPersetujuanSuratMasukStatus.class)
				.add(Restrictions.isNotNull("kodeUnik")).add(Restrictions.eq("suratMasuk", suratMasuk))
				.addOrder(Order.asc("id")).list();

		Map<String, List<DisposisiMasukChip>> perGrup = new LinkedHashMap<String, List<DisposisiMasukChip>>();
		int nomor = 1;
		String prevLabel = null;
		Date prevWaktu = null;
		for (AlurPersetujuanSuratMasukStatus s : list) {
			JenisJabatan jenisJabatan = jenisJabatanDariStatusMasuk(s);
			if (jenisJabatan == null && (s == null || s.getAlurPersetujuanSuratMasuk() == null)) {
				continue;
			}
			String grup = normalisasiGrupDisposisi(jenisJabatan == null ? null : jenisJabatan.getGrup());
			List<DisposisiMasukChip> chips = perGrup.get(grup);
			if (chips == null) {
				chips = new ArrayList<DisposisiMasukChip>();
				perGrup.put(grup, chips);
			}
			DisposisiMasukChip chip = new DisposisiMasukChip();
			chip.nomor = nomor;
			chip.label = labelDisposisiMasuk(s, jenisJabatan);
			chip.penerima = namaPenerimaDisposisiMasuk(s);
			chip.catatan = s == null ? "" : s.getKeterangan();
			isiStatusChipMasuk(chip, s);
			chip.tooltip = tooltipDisposisiMasuk(s, chip.label, chip.status, prevLabel, prevWaktu);
			chips.add(chip);
			nomor++;
			prevLabel = chip.label;
			prevWaktu = waktuDisposisiMasuk(s);
		}

		return buatHtmlDisposisiMasukBergrup(perGrup);
	}

	private static String buatHtmlDisposisiMasukBergrup(Map<String, List<DisposisiMasukChip>> perGrup) {
		if (perGrup == null || perGrup.isEmpty()) {
			return "<div style='font-size:11px;color:#64748b;background:#f8fafc;border:1px dashed #cbd5e1;"
					+ "border-radius:8px;padding:8px 10px;'>Surat ini belum memiliki disposisi.</div>";
		}

		StringBuilder sb = new StringBuilder();
		sb.append("<div style='margin:8px 0 6px 0;font-size:11px;color:#0f172a;background:#fff;box-sizing:border-box;'>");
		List<String> urutanGrup = urutanGrupDisposisiMasukDariSetup(perGrup);
		for (String grup : urutanGrup) {
			List<DisposisiMasukChip> chips = perGrup.get(grup);
			if (chips == null || chips.isEmpty()) {
				continue;
			}
			sb.append("<div style='position:relative;margin:12px 0 14px 0;border:1px solid #d7dce7;"
					+ "border-radius:8px;background:#fff;padding:22px 18px 13px 18px;"
					+ "box-shadow:0 1px 4px rgba(15,23,42,.06);box-sizing:border-box;'>");
			sb.append("<span style='position:absolute;top:-10px;left:12px;background:#1f4b99;color:#fff;"
					+ "border-radius:4px;padding:4px 12px;font-size:10px;font-weight:800;line-height:1;'>")
					.append(ais.ui.util.DashboardUiKit.esc(labelGrupDisposisi(grup))).append("</span>");
			// auto-fill + minmax responsif: kolom mengikuti lebar panel "Informasi Disposisi" (sempit di
			// modal Ubah) agar kartu chip TIDAK melebihi border. Sebelumnya dipaksa 3 kolom min 180px.
			sb.append("<div style='display:grid;grid-template-columns:repeat(auto-fill,minmax(150px,1fr));"
					+ "gap:10px 14px;align-items:center;box-sizing:border-box;width:100%;'>");
			for (DisposisiMasukChip chip : chips) {
				// Kartu chip 2 BARIS: baris-1 = nomor + NAMA PENGGUNA (lengkap), baris-2 = KETERANGAN/STATUS.
				// Sebelumnya 1 baris (inline-flex nowrap) sehingga nama ter-ellipsis / tertutup label status.
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
				if (chip.penerima != null && !chip.penerima.trim().isEmpty()) {
					sb.append("<span style='font-size:9px;font-weight:700;color:#334155;line-height:1.35;"
							+ "word-break:break-word;'>Tujuan: ")
							.append(ais.ui.util.DashboardUiKit.esc(chip.penerima)).append("</span>");
				}
				if (chip.catatan != null && !chip.catatan.trim().isEmpty()) {
					sb.append("<span style='font-size:9px;color:#475569;line-height:1.4;word-break:break-word;"
							+ "border-top:1px dashed #cbd5e1;padding-top:3px;'>Catatan: ")
							.append(ais.ui.util.DashboardUiKit.esc(chip.catatan.trim())).append("</span>");
				}
				sb.append("</span>");
			}
			sb.append("</div></div>");
		}
		sb.append("</div>");
		return sb.toString();
	}

	private static List<String> urutanGrupDisposisiMasukDariSetup(Map<String, List<DisposisiMasukChip>> perGrup) {
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
					"auto-audit src/ais/action/master/surat/SuratMasukAction.java:urutanGrupDisposisiMasukDariSetup");
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

	private static JenisJabatan jenisJabatanDariStatusMasuk(AlurPersetujuanSuratMasukStatus status) {
		if (status == null) {
			return null;
		}
		if (status.getJenisJabatan() != null) {
			return status.getJenisJabatan();
		}
		if (status.getAlurPersetujuanSuratMasuk() != null) {
			return status.getAlurPersetujuanSuratMasuk().getJenisJabatan();
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

	private static String labelDisposisiMasuk(AlurPersetujuanSuratMasukStatus status, JenisJabatan jenisJabatan) {
		if (jenisJabatan != null && jenisJabatan.getNama() != null && !jenisJabatan.getNama().trim().isEmpty()) {
			return jenisJabatan.getNama();
		}
		if (status != null && status.getAlurPersetujuanSuratMasuk() != null) {
			return String.valueOf(status.getAlurPersetujuanSuratMasuk());
		}
		return "Disposisi";
	}

	private static void isiStatusChipMasuk(DisposisiMasukChip chip, AlurPersetujuanSuratMasukStatus status) {
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

	private static Date waktuDisposisiMasuk(AlurPersetujuanSuratMasukStatus status) {
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

	/** Nama orang yang benar-benar dituju, bukan hanya nama grup/jabatannya. */
	public static String namaPenerimaDisposisiMasuk(AlurPersetujuanSuratMasukStatus status) {
		if (status == null || status.getPejabat() == null) {
			return "";
		}
		Pejabat pejabat = status.getPejabat();
		if (pejabat.getDosen() != null && pejabat.getDosen().getNama() != null) {
			return pejabat.getDosen().getNama();
		}
		if (pejabat.getPegawai() != null && pejabat.getPegawai().getNama() != null) {
			return pejabat.getPegawai().getNama();
		}
		if (pejabat.getGuru() != null && pejabat.getGuru().getNama() != null) {
			return pejabat.getGuru().getNama();
		}
		return pejabat.getNama() == null ? "" : pejabat.getNama();
	}

	/** Catatan/instruksi pimpinan terakhir sebelum langkah penerima saat ini. */
	@SuppressWarnings("unchecked")
	public static String catatanPimpinanDisposisiMasuk(AlurPersetujuanSuratMasukStatus status) {
		if (status == null || status.getSuratMasuk() == null) {
			return "";
		}
		String catatanSurat = status.getSuratMasuk().getCatatanDisposisi();
		List<AlurPersetujuanSuratMasukStatus> sebelumnya = HibernateUtil.currentSession()
				.createCriteria(AlurPersetujuanSuratMasukStatus.class)
				.add(Restrictions.isNotNull("kodeUnik"))
				.add(Restrictions.eq("suratMasuk", status.getSuratMasuk()))
				.add(status.getId() == null ? Restrictions.sqlRestriction("1=1") : Restrictions.lt("id", status.getId()))
				.addOrder(Order.desc("id")).list();
		for (AlurPersetujuanSuratMasukStatus sebelum : sebelumnya) {
			String catatan = sebelum.getKeterangan();
			if (catatan != null && !catatan.trim().isEmpty()) {
				return catatan.trim();
			}
		}
		return catatanSurat == null ? "" : catatanSurat.trim();
	}

	private static String tooltipDisposisiMasuk(AlurPersetujuanSuratMasukStatus status, String label,
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
		String konseptor = ringkasanKonseptorMasuk(status.getKonseptor());
		if (konseptor != null && konseptor.trim().length() > 0) {
			sb.append("\nJabatan yang mendisposisikan : ").append(konseptor);
			Date waktuDisposisi = waktuDisposisiMasuk(status);
			if (waktuDisposisi != null) {
				sb.append("\nTanggal & Waktu : ").append(Common.dateFormat3.get().format(waktuDisposisi));
			}
		}
		return sb.toString();
	}

	private static String ringkasanKonseptorMasuk(Tbmuser konseptor) {
		if (konseptor == null) {
			return "";
		}
		String ringkasan = String.valueOf(konseptor);
		return bersihkanKeteranganKurungKonseptorMasuk(ringkasan);
	}

	private static String bersihkanKeteranganKurungKonseptorMasuk(String ringkasan) {
		if (ringkasan == null || "null".equalsIgnoreCase(ringkasan.trim())) {
			return "";
		}
		return ringkasan.trim().replaceAll("\\s*\\([^)]*\\)\\s*$", "").trim();
	}

	private static String labelJabatanKonseptorMasuk(Tbmuser konseptor) {
		JenisJabatan jenisJabatan = jenisJabatanKonseptorMasuk(konseptor);
		if (jenisJabatan != null && jenisJabatan.getNama() != null && jenisJabatan.getNama().trim().length() > 0) {
			return jenisJabatan.getNama();
		}
		return "";
	}

	private static JenisJabatan jenisJabatanKonseptorMasuk(Tbmuser konseptor) {
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
					"auto-audit src/ais/action/master/surat/SuratMasukAction.java:jenisJabatanKonseptorMasuk-role");
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
					"auto-audit src/ais/action/master/surat/SuratMasukAction.java:jenisJabatanKonseptorMasuk-pejabat");
		}
		return null;
	}

	private static String namaKonseptorMasuk(Tbmuser konseptor) {
		if (konseptor == null) {
			return "";
		}
		if (konseptor.getUserNama() != null && konseptor.getUserNama().trim().length() > 0) {
			return konseptor.getUserNama();
		}
		return konseptor.getUserId() == null ? "" : konseptor.getUserId();
	}

	private Checkbox searchaktif;
	private Combobox sifatSurat;

	@SuppressWarnings("unchecked")
	public Criteria initCriteria(boolean order) {

		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(SuratMasuk.class)

				.add(Restrictions.or(Restrictions.isNull("tipe"), Restrictions.eq("tipe", tipe)))

				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"))

				.add(Restrictions.or(Restrictions.isNull("satuanKerja"),
						satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(
										parent == null ? Restrictions.isNull("satuanKerja")
												: Restrictions.sqlRestriction("false"),
										Restrictions.in("satuanKerja", satuanKerjas))));

		Jurusan jurusan = tbmuser.getMahasiswa() != null ? tbmuser.getMahasiswa().getJurusan() : tbmuser.ambilJurusan();
		Fakultas fakultas = tbmuser.getMahasiswa() != null ? tbmuser.getMahasiswa().getJurusan().getFakultas()
				: tbmuser.ambilFakultas();

		Criterion criterion1 = Restrictions.or(
				fakultas == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("klasifikasiSuratMasuk.fakultas", fakultas),
				Restrictions.isNull("klasifikasiSuratMasuk.fakultas"));
		Criterion criterion2 = Restrictions.or(
				jurusan == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("klasifikasiSuratMasuk.jurusan", jurusan),
				Restrictions.isNull("klasifikasiSuratMasuk.jurusan"));

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

				Criterion c = Restrictions.eq("konseptor", tbmuser);

				c = Restrictions.or(c, Restrictions.ilike("klasifikasiSuratMasuk.kodeGrupPengguna",
						";" + tbmrole.getRoleId() + ";", MatchMode.ANYWHERE));

				criterion = Restrictions.and(criterion, c);
			}
		}
		List<Long> ids = null;
		if (!searchnamaIsi.getValue().trim().isEmpty()) {
			ids = session.createCriteria(KlasifikasiSuratMasukParemeterValue.class)
					.setProjection(Projections.groupProperty("suratMasuk.id")).add(Restrictions.isNotNull("suratMasuk"))
					.add(Restrictions.ilike("nama", searchnamaIsi.getValue().trim(), MatchMode.ANYWHERE)).list();
		}

		if (order)
			criteria.addOrder(Order.desc("tanggal"));
		criteria.createAlias("klasifikasiSuratMasuk", "klasifikasiSuratMasuk")
				.add(ids == null ? Restrictions.sqlRestriction("true")
						: (ids.isEmpty() ? Restrictions.sqlRestriction("false") : Restrictions.in("id", ids)))

				.add(criterion)
				.add(searchjenis == null || searchjenis.getValue().trim().isEmpty()
						? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", searchjenis.getValue(), MatchMode.ANYWHERE))

				.add(searchasal == null || searchasal.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("asal", searchasal.getValue(), MatchMode.ANYWHERE))

				.add(CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))
				.add(CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false))

				.add(searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
						|| searchsekolah.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false))

				.add(searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
						|| searchyayasan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false))

				.add(searchloker.getSelectedItem() == null || searchloker.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("loker", searchloker.getSelectedItem().getValue()))

				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("perihal", searchnama.getValue().trim(), MatchMode.ANYWHERE)))
				.add(searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("kode", searchkode.getValue(), MatchMode.ANYWHERE))
				.add((searchmulai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchmulai.getValue() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.ge("tanggal", searchmulai.getValue())))
				.add((searchsampai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchsampai.getValue() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.le("tanggal", searchsampai.getValue())));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<SuratMasuk> SuratMasuk = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(SuratMasuk);
		grid.setRowRenderer(new SuratMasukRenderer());
		grid.setModelCheckMobile(strset);

	}

	@SuppressWarnings("deprecation")
	@Override
	public MyGrid form(GeneralValueObject generalValueObject, DisposisiSop disposisiSop,
			final MyToolbarbuttonConfig save, EventListener setujui) throws Exception {
		tbmuser = Common.getCurrentUser();
		this.suratMasuk = (SuratMasuk) generalValueObject;
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null))
				? this.disposisiSop
				: disposisiSop;
		selectedJenisJabatan = new HashSet<JenisJabatan>();
		removedJenisJabatan = new HashSet<JenisJabatan>();
		try {
			jenisSurats = new JSONObject(suratMasuk.getJenisSurats());
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
		row.appendChild(new ais.ui.util.MyLabelConfig("No Surat *"));
		row.appendChild(noSurat = new Textbox(suratMasuk.getNoSurat() == null ? "" : suratMasuk.getNoSurat()));
		noSurat.setWidth("90%");
		noSurat.setAttribute("janganDisabled", true);
		if (suratMasuk.getId() != null) {
			noSurat.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					Session session = HibernateUtil.currentNativeSession();
					SuratMasuk mySuratMasuk = (SuratMasuk) session.createCriteria(SuratMasuk.class)
							.add(Restrictions.idEq(suratMasuk.getId())).uniqueResult();

					mySuratMasuk.setNoSurat(noSurat.getValue().trim());
					session.getTransaction().begin();
					Common.refreshUpdate(session, mySuratMasuk);
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

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Klasifikasi Surat Masuk *"));
		row.appendChild(klasifikasiSuratMasuk = new AmbilDataKlasifikasiSuratMasukBanbox(tipe));
		klasifikasiSuratMasuk.setAttribute("klasifikasiSuratMasuk", suratMasuk.getKlasifikasiSuratMasuk());
		klasifikasiSuratMasuk.setValue(suratMasuk.getKlasifikasiSuratMasuk() == null ? ""
				: suratMasuk.getKlasifikasiSuratMasuk().getKode()
						+ (suratMasuk.getKlasifikasiSuratMasuk().getNama() == null
								|| suratMasuk.getKlasifikasiSuratMasuk().getNama().trim().isEmpty() ? ""
										: "-" + suratMasuk.getKlasifikasiSuratMasuk().getNama()));

		klasifikasiSuratMasuk.setWidth("90%");
		klasifikasiSuratMasuk.setReadonly(true);

		EventListener eventListenermasuk = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				KlasifikasiSuratMasuk thisKlasifikasiSuratMasuk = (KlasifikasiSuratMasuk) (klasifikasiSuratMasuk
						.getAttribute("klasifikasiSuratMasuk"));
				if (thisKlasifikasiSuratMasuk != null) {

					if (alurPersetujuanSuratMasuk != null && alurPersetujuanSuratMasuk.getParent() != null) {
						alurPersetujuanSuratMasuk.getParent().setVisible(
								!(thisKlasifikasiSuratMasuk != null && thisKlasifikasiSuratMasuk.getTanpaAlur()));
					}

					if (thisKlasifikasiSuratMasuk.getSatuanKerja() != null) {
						satuanKerja.setAttribute("satuanKerja", thisKlasifikasiSuratMasuk.getSatuanKerja());
						satuanKerja.setValue(thisKlasifikasiSuratMasuk.getSatuanKerja() == null ? ""
								: thisKlasifikasiSuratMasuk.getSatuanKerja().getNama());
						satuanKerja.setDisabled(true);
					} else {
						satuanKerja.setAttribute("satuanKerja", null);
						satuanKerja.setValue("");
						satuanKerja.setDisabled(false);
					}

					if (perihal != null && perihal.getValue().trim().isEmpty()
							&& !thisKlasifikasiSuratMasuk.getPerihalDefault().trim().isEmpty()) {
						perihal.setValue(thisKlasifikasiSuratMasuk.getPerihalDefault());
					}

					if (thisKlasifikasiSuratMasuk.getSifatSurat() != null) {
						Common.selectComboItem(true, sifatSurat, thisKlasifikasiSuratMasuk.getSifatSurat());
						sifatSurat.setDisabled(true);
					} else {
						Common.selectComboItem(sifatSurat, null);
						sifatSurat.setDisabled(false);
					}

					initParameter(rows, thisKlasifikasiSuratMasuk);
					listener.onEvent(arg0);

				}
			}
		};

		klasifikasiSuratMasuk.setEventListener(eventListenermasuk);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("No. Agenda Surat *"));
		row.appendChild(kode = new Textbox(suratMasuk.getKode()));
		kode.setWidth("90%");
		kode.setAttribute("janganDisabled", true);
		if (suratMasuk.getId() != null) {
			kode.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					Session session = HibernateUtil.currentNativeSession();
					SuratMasuk mySuratMasuk = (SuratMasuk) session.createCriteria(SuratMasuk.class)
							.add(Restrictions.idEq(suratMasuk.getId())).uniqueResult();

					mySuratMasuk.setKode(kode.getValue().trim());
					session.getTransaction().begin();
					Common.refreshUpdate(session, mySuratMasuk);
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

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Alur Disposisi *"));
		row.appendChild(alurPersetujuanSuratMasuk = new AmbilDataAlurPersetujuanSuratMasukBanbox(true, true, tipe));
		alurPersetujuanSuratMasuk.setAttribute("alurPersetujuanSuratMasuk", suratMasuk.getAlurPersetujuanSuratMasuk());
		alurPersetujuanSuratMasuk.setValue(suratMasuk.getAlurPersetujuanSuratMasuk() == null ? ""
				: suratMasuk.getAlurPersetujuanSuratMasuk().toString());
		alurPersetujuanSuratMasuk.setWidth("90%");
		alurPersetujuanSuratMasuk.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(vboxAlur = new Vbox());

		alurEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initKelengkapanBerkas(vboxAlur, (AlurPersetujuanSuratMasuk) alurPersetujuanSuratMasuk
						.getAttribute("alurPersetujuanSuratMasuk"));
			}
		};

		alurPersetujuanSuratMasuk.setEventListener(alurEventListener);
		Common.createDefaultTimer(alurEventListener);

		if (suratMasuk.getId() == null) {
			suratMasuk.setSatuanKerja(Common.getSatuanKerja());
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja *"));
		satuanKerja = new AmbilDataSatuanKerjaBanbox(true);
		satuanKerja.setValue(suratMasuk.getSatuanKerja() == null ? "" : suratMasuk.getSatuanKerja().getNama());
		satuanKerja.setAttribute("satuanKerja", suratMasuk.getSatuanKerja());
		row.appendChild(satuanKerja);
		satuanKerja.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Diterima Tanggal *"));
		row.appendChild(tanggal = new MyDatebox(
				suratMasuk.getTanggal() == null ? ais.ui.util.WaktuUtil.getDate() : suratMasuk.getTanggal()));
		tanggal.setWidth("90%");
		tanggal.setReadonly(true);
		tanggal.setDisabled(!Common.bolehKonfigurasi("tanggal_diterima_di_surat_masuk_boleh_diubah"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Surat *"));
		row.appendChild(tanggalSurat = new MyDatebox(
				suratMasuk.getTanggalSurat() == null ? ais.ui.util.WaktuUtil.getDate() : suratMasuk.getTanggalSurat()));
		tanggalSurat.setWidth("90%");
		tanggalSurat.setReadonly(true);

		if (suratMasuk.getId() != null) {
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Buat/Ubah"));

			row.appendChild(RevisiHelper.createNewRevisi(SuratKeluar.class, suratMasuk,
					Common.dateFormat3.get().format(suratMasuk.getTanggal_dirubah())));
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Asal Surat *"));
		row.appendChild(asal = new Textbox(suratMasuk.getAsal()));
		asal.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Perihal *"));
		row.appendChild(perihal = new Textbox(suratMasuk.getPerihal()));
		perihal.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Lokasi Loker *"));
		row.appendChild(loker = new Combobox());
		loker.setWidth("90%");
		loker.setReadonly(true);

		EventListener satkerEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				SatuanKerja parent = (SatuanKerja) satuanKerja.getAttribute("satuanKerja");
				Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
				if (parent != null) {
					satuanKerjas.clear();
					satuanKerjas.add(parent);
					satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
				}

				Common.insertCombo(loker, new String[] { "kode", "nama" }, "keterangan", LokerSurat.class,
						Restrictions.and(
								Restrictions.or(Restrictions.isNull("satuanKerja"),
										satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
												: Restrictions.or(Restrictions.isNull("satuanKerja"),
														Restrictions.in("satuanKerja", satuanKerjas))),
								Restrictions.and(Restrictions.eq("tipe", tipe), Restrictions
										.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))));

				Common.selectComboItem(true, loker, suratMasuk.getLoker());
			}
		};

		satuanKerja.setEventListener(satkerEventListener);
		Common.createDefaultTimer(satkerEventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status *"));
		row.appendChild(status = new Combobox());
		String[] ss = new String[] { "Asli", "Tembusan" };
		for (String s : ss) {
			MyComboitemConfig comboitem = new MyComboitemConfig(s);
			comboitem.setValue(s);
			status.appendChild(comboitem);
		}
		Common.selectComboItem(status, suratMasuk.getStatus());
		status.setWidth("90%");
		status.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sifat Surat"));
		Common.insertCombo(sifatSurat = new Combobox(), new String[] { "nama" }, "keterangan", SifatSurat.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		Common.selectComboItem(sifatSurat, suratMasuk.getSifatSurat());
		row.appendChild(sifatSurat);
		sifatSurat.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kerahasiaan"));
		row.appendChild(kerahasiaan = new Combobox());
		ss = new String[] { "Sangat rahasia", "Rahasia", "Biasa" };
		for (String s : ss) {
			MyComboitemConfig comboitem = new MyComboitemConfig(s);
			comboitem.setValue(s);
			kerahasiaan.appendChild(comboitem);
		}
		Common.selectComboItem(kerahasiaan, suratMasuk.getKerahasiaan());
		kerahasiaan.setWidth("90%");
		kerahasiaan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Berkas / Lampiran *"));
		row.appendChild(lampiran = new Textbox(suratMasuk.getLampiran()));
		lampiran.setWidth("90%");

		if (suratMasuk.getKlasifikasiSuratMasuk() != null) {
			initParameter(rows, suratMasuk.getKlasifikasiSuratMasuk());
		}

		tbmuser = Common.getCurrentUser();
		if (suratMasuk.getFakultas() == null && tbmuser.ambilFakultas() != null) {
			suratMasuk.setFakultas(tbmuser.ambilFakultas());
		}
		if (suratMasuk.getJurusan() == null && tbmuser.ambilJurusan() != null) {
			suratMasuk.setJurusan(tbmuser.ambilJurusan());
		}

		Common.initFakultasDanJurusanDanSemua(fakultas = new Combobox(), jurusan = new Combobox(), null, null);
		MyFormRow rowFakultas = new MyFormRow();
		rowFakultas.setVisible(pt && false);
		rowFakultas.setStyle("border:0px;background: transparent;");
		rowFakultas.setParent(rows);
		rowFakultas.appendChild(new Label(ais.common.Common.getBahasaConfig("Fakultas")));
		rowFakultas.appendChild(fakultas);
		Common.selectComboItem(true, fakultas, suratMasuk.getFakultas());
		fakultas.setWidth("90%");

		Common.insertComboDanSemua(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
				Restrictions.eq("fakultas",
						suratMasuk.getFakultas() == null ? tbmuser.ambilFakultas() : suratMasuk.getFakultas()));

		MyFormRow rowJurusan = new MyFormRow();
		rowJurusan.setVisible(pt && false);
		rowJurusan.setStyle("border:0px;background: transparent;");
		rowJurusan.setParent(rows);
		rowJurusan.appendChild(new Label(ais.common.Common.getBahasaConfig("Jurusan")));
		rowJurusan.appendChild(jurusan);
		jurusan.setWidth("90%");
		Common.pilihJurusan(jurusan, suratMasuk.getJurusan());

		Tbmuser tbmuser1 = Common.getCurrentUser();

		yayasan = new Combobox();
		sekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		row = new MyFormRow();
		row.setVisible(ya);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));

		Common.selectComboItem(yayasan, suratMasuk == null || suratMasuk.getYayasan() == null ? tbmuser1.ambilYayasan()
				: suratMasuk.getYayasan());
		row.appendChild(yayasan);
		yayasan.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(ya);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah"));

		Common.pilihSekolah(sekolah, suratMasuk == null || suratMasuk.getSekolah() == null ? tbmuser1.ambilSekolah()
				: suratMasuk.getSekolah());
		row.appendChild(sekolah);
		sekolah.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.getSiswa() == null);
		row.setParent(rows);
		broadcast = new MyCheckboxConfig("Sebarkan / broadcast surat ini");
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(broadcast);
		broadcast.setChecked(suratMasuk.getBroadcast());

		row = new MyFormRow();
		row.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.getSiswa() == null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sebarkan ke username pengguna"));
		row.appendChild(usernamePengguna = new Textbox(suratMasuk.getUsernamePengguna()));
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
				rowAmbilPengguna.setVisible(broadcast.isChecked() && tbmuser != null && tbmuser.getMahasiswa() == null
						&& tbmuser.getSiswa() == null && tbmuser.getSiswa() == null);
				usernamePengguna.getParent().setVisible(broadcast.isChecked() && tbmuser != null
						&& tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null);
			}

		};

		broadcast.addEventListener("onClick", startEvent);
		startEvent.onEvent(null);

		if (disposisiSop != null) {
			row = new MyFormRow();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");

			MyGroupboxStyled groupboxStyled = new MyGroupboxStyled();
			groupboxStyled.setParent(row);
			groupboxStyled.setHeight("800px");
			groupboxStyled.appendChild(new MyCaptionStyled("Surat Masuk"));

			initDetail(suratMasuk, groupboxStyled);
		}

		if (suratMasuk.getId() != null && suratMasuk.getAlurPersetujuanSuratMasuk() != null) {
			Common.createDefaultTimer(listener);
			Common.createDefaultTimer(alurEventListener);
		}

		ubah = true;
		if (suratMasuk.getDikunci() == null && suratMasuk.getId() != null) {

			Session session = HibernateUtil.currentSession();
			AlurPersetujuanSuratMasukStatus alurPersetujuanSuratMasukStatus = (AlurPersetujuanSuratMasukStatus) session
					.createCriteria(AlurPersetujuanSuratMasukStatus.class).add(Restrictions.isNotNull("kodeUnik"))
					.setMaxResults(1).add(Restrictions.eq("suratMasuk", suratMasuk)).addOrder(Order.asc("id"))
					.uniqueResult();

			if (alurPersetujuanSuratMasukStatus != null && alurPersetujuanSuratMasukStatus.getDisetujui()) {
				Common.freezeGanti(grid, true);
				ubah = false;
			}
		}

		eventListenermasuk.onEvent(null);

		row = new MyFormRow();
		row.setVisible(suratMasuk.getAlurDitolak() != null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Catatan Ditolak"));
		row.appendChild(new MyLabelAgakKecilBoldMerah(
				suratMasuk.getAlurDitolak() == null ? "" : suratMasuk.getAlurDitolak().getKeterangan()));

		row = new MyFormRow();
		row.setVisible(suratMasuk.getAlurDitolak() != null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Catatan Revisi Perbaikan *"));
		row.appendChild(catatanRevisi = new Textbox(suratMasuk.getCatatanRevisi()));
		catatanRevisi.setWidth("90%");
		catatanRevisi.setRows(5);

		if (suratMasuk != null && suratMasuk.getId() != null) {
			row = new MyFormRow();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");
			row.appendChild(new MyLabelStyled("Informasi Disposisi"));

			row = new MyFormRow();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");
			String html = SuratMasukAction.infoDisposisiBagan(suratMasuk);
			new ais.ui.util.MyHtml(html).setParent(row);
		}

		if (suratMasuk.getDikunci() != null) {
			Common.freezeGanti(grid, true);
			kode.setDisabled(true);
			noSurat.setDisabled(true);
		}

		return grid;
	}

	@Override
	public String istilah() throws Exception {
		// TODO Auto-generated method stub
		return "Surat Masuk";
	}

	@Override
	public DataSop ambil() throws Exception {
		// TODO Auto-generated method stub
		return suratMasuk;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Class ambilClass() throws Exception {
		// TODO Auto-generated method stub
		return SuratMasuk.class;
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
