package ais.action.master.koperasi;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.akunting.ProsesTransferAction;
import ais.action.master.dashboard.koperasi.DasboardTransaksiKoperasi;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.koperasi.helper.AmbilDataAnggotaKoperasiBanbox;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.master.sop.TampilanAlurSopAction;
import ais.action.report.Report;
import ais.action.report.format1.koperasi.LaporanTransaksiKoperasi;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.UIClassHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.DaftarPengajuanTransfer;
import ais.database.model.akunting.DanaTalangan;
import ais.database.model.akunting.NomorSuratAlurKeuangan;
import ais.database.model.akunting.UangMuka;
import ais.database.model.file.LampiranLain;
import ais.database.model.koperasi.AnggotaKoperasi;
import ais.database.model.koperasi.CaraPembayaranKoperasi;
import ais.database.model.koperasi.JenisTransaksiKoperasi;
import ais.database.model.koperasi.ProdukKoperasi;
import ais.database.model.koperasi.TransaksiKoperasi;
import ais.database.model.koperasi.TransaksiKoperasiDetail;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;
import ais.database.model.surat.NomorSurat;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.FormSop;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.DashboardUiKit;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;
import ais.action.master.helper.FilterLanjutHelper;

public class TransaksiKoperasiAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault, FormSop {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4124140285573733292L;

	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox serachnama;
	private Textbox serachjenis;
	private Textbox serachkode;
	private Combobox searchstatus;
	private Checkbox searchaktif;
	private MyDatebox start;
	private MyDatebox end;
	private Label kode;
	private Textbox keterangan;

	public TransaksiKoperasi transaksiKoperasi;
	private MyToolbarbuttonConfig add;

	private boolean edit;
	private boolean delete;

	private Combobox produkKoperasi;

	private Double nilai;

	private MyDatebox tanggal;

	private DisposisiSop disposisiSop;

	private JSONArray array;

	private Row rowFormula;

	private boolean persetujuan = false;

	private Tbmuser tbmuser;

	private SatuanKerjaTreeModel satuanKerjaTreeModel;
	private AmbilDataSatuanKerjaBanbox searchparent;

	private AmbilDataAnggotaKoperasiBanbox anggotaKoperasi;

	private Radiogroup status;

	private boolean setujui = false;
	protected LampiranLain lainMahasiswa;

	private boolean viewOnly = false;

	protected Tabpanel statistik;

	private MyDatebox tanggalPersetujuanManual;

	private Row rowDetail;

	private Combobox caraPembayaranKoperasi;

	private Row rowDetailAngsuran;

	protected Row rowFormulaAngsuran;

	private EventListener eventListenerDetailAngsuran;

	private Label pokokPinjaman;
	private Label totalPinjaman;
	private Label jumlahMargin;

	private Double yangDiterima = 0.0;

	private Label durasiPinjaman;

	private Label tenorPinjaman;

	private MyDatebox tanggalMulaiDiangsur;

	private Label tanggalTerakhirDiangsur;

	private EventListener eventListenerDetail;

	private Label nilaiYangDiterima;

	private Row rowpokokPinjaman;

	private Row rownilaiYangDiterima;

	private Row rownilaiYangDiterimaDa;

	private Row rowtotalPinjaman;

	private Row rowdurasiPinjaman;

	private Row rowtenorPinjaman;

	private Row rowtanggalMulaiDiangsur;

	private Row rowtanggalTerakhirDiangsur;

	private MyLabelConfig tanggalMulaiDiangsurLabel;

	private Row rowCaraPembayaranKoperasi;

	public void onStatistik(Event event) {

		if (statistik.getChildren().size() == 0) {
			DasboardTransaksiKoperasi include = new DasboardTransaksiKoperasi();
			include.setHeight("100%");
			include.setWidth("100%");
			include.setParent(statistik);
		}
	}

	public static String[] contents = new String[] { "id", "kode", "nama", "keterangan", "caraPembayaranKoperasi",
			"anggotaKoperasi", "produkKoperasi", "formula", "satuanKerja", "tanggal", "nilai", "yangDiterima", "margin",
			"status", "daftarPengajuanTransfer.prosesTransfer.kode", "daftarPengajuanTransfer.prosesTransfer.nama",
			"daftarPengajuanTransfer.prosesTransfer.tanggalPembuatan",
			"daftarPengajuanTransfer.prosesTransfer.disetujuiOleh",
			"daftarPengajuanTransfer.prosesTransfer.tanggalPersetujuan",
			"daftarPengajuanTransfer.prosesTransfer.realisasikanOleh",
			"daftarPengajuanTransfer.prosesTransfer.tanggalRealisasikan", "aktif" };

	public TransaksiKoperasiAction() {
		tbmuser = Common.getCurrentUser();
	}

	public TransaksiKoperasiAction(boolean persetujuan) {
		this.persetujuan = persetujuan;
		tbmuser = Common.getCurrentUser();
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
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		searchparent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		if (start != null) start.setReadonly(true);
		if (end != null) end.setReadonly(true);

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 6);
		if (start != null) start.setValue(calendar.getTime());
		calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
		if (end != null) end.setValue(calendar.getTime());

		Comboitem comboitemSemua = new Comboitem("Semua");
		if (comboitemSemua != null) { comboitemSemua.setValue(null); }
		searchstatus.appendChild(comboitemSemua);

		Comboitem comboitem = new Comboitem(TransaksiKoperasi.PENGAJUAN);
		if (comboitem != null) { comboitem.setValue(TransaksiKoperasi.PENGAJUAN); }
		searchstatus.appendChild(comboitem);
		comboitem = new Comboitem(TransaksiKoperasi.DISETUJU);
		if (comboitem != null) { comboitem.setValue(TransaksiKoperasi.DISETUJU); }
		searchstatus.appendChild(comboitem);
		comboitem = new Comboitem(TransaksiKoperasi.DITOLAK);
		if (comboitem != null) { comboitem.setValue(TransaksiKoperasi.DITOLAK); }
		searchstatus.appendChild(comboitem);

		if (searchstatus != null) { searchstatus.setSelectedItem(comboitemSemua); }
		if (searchstatus != null) { searchstatus.setReadonly(true); }

		if (execution.getParameter("persetujuan") != null) {
			persetujuan = Boolean.parseBoolean(execution.getParameter("persetujuan"));
		}

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE) && !persetujuan);
		}

		if (add != null) { add.setTooltiptext("Tambah"); }
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(TransaksiKoperasi.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, TransaksiKoperasi.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

	        FilterLanjutHelper.setup(comp);
}

	class TransaksiKoperasiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final TransaksiKoperasi transaksiKoperasi = (TransaksiKoperasi) arg1;

			if (transaksiKoperasi.getDibuatOleh() == null) {
				transaksiKoperasi.setDibuatOleh(tbmuser);
			}

			if (transaksiKoperasi.getStatus().equals(UangMuka.DISETUJU)
					&& transaksiKoperasi.getDaftarPengajuanTransfer() == null) {
				DaftarPengajuanTransfer.simpanTransaksiKoperasi(transaksiKoperasi);
			}

			Vbox a;
			(a = RevisiHelper.createNewRevisi(TransaksiKoperasi.class, transaksiKoperasi,
					transaksiKoperasi.getKode() == null ? "" : transaksiKoperasi.getKode().trim().toString()))
					.setParent(arg0);

			if (transaksiKoperasi.getDaftarPengajuanTransfer() != null
					&& transaksiKoperasi.getDaftarPengajuanTransfer().getProsesTransfer() != null) {

				A aaa = new A(transaksiKoperasi.getDaftarPengajuanTransfer().getProsesTransfer().getKode());
				aaa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						ProsesTransferAction.onAddExternal(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								onSearchDefault(arg0);
							}
						}, transaksiKoperasi.getDaftarPengajuanTransfer().getProsesTransfer());

					}
				});
				aaa.setStyle("font-size:12px;");
				aaa.setParent(a);
			}

			Vbox myvbox = new Vbox();
			myvbox.setParent(a);

			Hbox hbox = new Hbox();
			hbox.setParent(myvbox);
			LampiranLain.createDownloadUploadFileLain(hbox, transaksiKoperasi.getId(),
					TransaksiKoperasi.class.getName(), "Bukti", false, null, null, false, false, false, true);

			myvbox = new Vbox();
			myvbox.setParent(arg0);
//			new Label(transaksiKoperasi.getAnggotaKoperasi() == null ? ""
//					: transaksiKoperasi.getAnggotaKoperasi().getKode()).setParent(myvbox);
			new Label(transaksiKoperasi.getAnggotaKoperasi() == null ? ""
					: transaksiKoperasi.getAnggotaKoperasi().getNama()).setParent(myvbox);

			myvbox = new Vbox();
			myvbox.setParent(arg0);
			new Label(transaksiKoperasi.getProdukKoperasi() == null ? ""
					: transaksiKoperasi.getProdukKoperasi().getKode() + "-"
							+ transaksiKoperasi.getProdukKoperasi().getNama())
					.setParent(myvbox);

			new Label(transaksiKoperasi.getCaraPembayaranKoperasi() == null ? ""
					: transaksiKoperasi.getCaraPembayaranKoperasi().getNama()).setParent(myvbox);

			new Label(Common.numberFormat.get().format(transaksiKoperasi.getNilai())).setParent(arg0);

			a = new Vbox();
			a.setParent(arg0);
			new Label(transaksiKoperasi.getTanggal() == null ? ""
					: Common.dateFormat3.get().format(transaksiKoperasi.getTanggal())).setParent(a);
			new Label(transaksiKoperasi.getSatuanKerja() == null ? "" : transaksiKoperasi.getSatuanKerja().getNama())
					.setParent(a);

			new MyLabelAgakKecil(
					transaksiKoperasi.getDibuatOleh() == null ? "" : transaksiKoperasi.getDibuatOleh().getUserNama())
					.setParent(a);

			a = new Vbox();
			a.setParent(arg0);
			new Label(transaksiKoperasi.getStatus()).setParent(a);
			(new MyLabelAgakKecil(transaksiKoperasi.getDisetujuiOleh() == null ? ""
					: transaksiKoperasi.getDisetujuiOleh().getUserNama())).setParent(a);
			(new MyLabelAgakKecil(transaksiKoperasi.getTanggalPersetujuan() == null ? ""
					: Common.dateFormat3.get().format(transaksiKoperasi.getTanggalPersetujuan()))).setParent(a);

			Vbox vbox1 = new Vbox();
			vbox1.setParent(arg0);
			new MyLabelAgakKecil(Common.simpleString(transaksiKoperasi.getKeterangan())).setParent(vbox1);
			if (transaksiKoperasi.getDisposisiSop() != null) {
				A aa;
				(aa = new A()).setParent(vbox1);

				aa.setStyle("font-size:9px;");
				UIClassHelper.applyReadMore(aa, "SOP " + transaksiKoperasi.getDisposisiSop().getKeterangan() + " ("
						+ transaksiKoperasi.getDisposisiSop().getSop().getNama() + ")");
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(transaksiKoperasi.getDisposisiSop().getId(), null, null, true,
								arg0.getTarget());
					}
				});
			}

			if (transaksiKoperasi.getDisposisiSop() != null && !transaksiKoperasi.getDisposisiSop().getAktif()) {
				new Label(ais.common.Common.getBahasaConfig("Tidak aktif")).setParent(arg0);
			} else if (persetujuan && !transaksiKoperasi.getStatus().equals(TransaksiKoperasi.DISETUJU)) {
				final MyCheckboxConfig aktif = new MyCheckboxConfig("Aktif");
				aktif.setChecked(transaksiKoperasi.getAktif());
				aktif.setParent(arg0);
				aktif.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						transaksiKoperasi.setAktif(aktif.isChecked());
						Common.refreshSaveOrUpdate(transaksiKoperasi);
					}
				});
			} else {
				new Label(transaksiKoperasi.getAktif() ? "Ya" : "Tidak").setParent(arg0);
			}

			vbox1 = new Vbox();
			vbox1.setParent(arg0);

			DaftarPengajuanTransfer.tampilStatus(transaksiKoperasi.getDaftarPengajuanTransfer(), vbox1);

			Session session = HibernateUtil.currentSession();
			int count = ((Number) session.createCriteria(TransaksiKoperasiDetail.class)
					.add(Restrictions.eq("transaksiKoperasi", transaksiKoperasi))
					.add(Restrictions.isNotNull("pembayaranAnggotaKoperasiDetail"))
					.setProjection(Projections.rowCount()).uniqueResult()).intValue();

			Hbox hbx;
			(hbx = Common.copyEditDeleteButtons(edit,
					!persetujuan && !transaksiKoperasi.getStatus().equals(TransaksiKoperasi.DISETUJU),
					count == 0 && delete && !persetujuan
							&& !transaksiKoperasi.getStatus().equals(TransaksiKoperasi.DISETUJU),
					transaksiKoperasi, TransaksiKoperasiAction.this)).setParent(arg0);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak");
			button.setOrient("vertical");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {
					cetak(transaksiKoperasi);
				}

			});
			button.setParent(hbx);
		}

	}

	@Override
	public File cetakData(GeneralValueObject generalValueObject) throws Exception {
		TransaksiKoperasi transaksiKoperasi = (TransaksiKoperasi) generalValueObject;
		LaporanTransaksiKoperasi buktiPengeluaranKas = new LaporanTransaksiKoperasi(transaksiKoperasi);
		buktiPengeluaranKas.setTitle("Laporan");
		buktiPengeluaranKas.setClosable(true);
		buktiPengeluaranKas.setHeight("90%");
		buktiPengeluaranKas.setWidth("900px");
		buktiPengeluaranKas.setVisible(false);
		File file = Report.generateFileReport(Report.PDF, buktiPengeluaranKas.generateParameter(),
				"koperasi/transaksiKoperasi", ais.ui.util.WaktuUtil.getDate(), null, new Toolbar());
		return file;
	}

	public static void cetak(TransaksiKoperasi transaksiKoperasi) throws Exception {
		LaporanTransaksiKoperasi buktiPengeluaranKas = new LaporanTransaksiKoperasi(transaksiKoperasi);
		buktiPengeluaranKas.setTitle("Laporan");
		buktiPengeluaranKas.setClosable(true);
		buktiPengeluaranKas.setHeight("90%");
		buktiPengeluaranKas.setWidth("900px");
		buktiPengeluaranKas.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		buktiPengeluaranKas.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		transaksiKoperasi = (TransaksiKoperasi) obj;
		init(transaksiKoperasi);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	public void onAdd(Event event) throws Exception {
		viewOnly = false;
		persetujuan = false;
		init(new TransaksiKoperasi());
		addWindow.setVisible(true);
		try {
			addWindow.onModal();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Common.tampilErrorJikaAdmin(e);
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public MyGrid form(GeneralValueObject generalValueObject, DisposisiSop disposisiSop,
			final MyToolbarbuttonConfig save, final EventListener setujuiData) throws Exception {
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null)) ? this.disposisiSop : disposisiSop;
		transaksiKoperasi = (TransaksiKoperasi) generalValueObject;
		if (satuanKerjaTreeModel == null) {
			satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
		}
		setujui = false;
		if (!persetujuan) {
			if (transaksiKoperasi != null && transaksiKoperasi.getStatus().equals(TransaksiKoperasi.DISETUJU)) {
				setujui = true;
			} else {
				setujui = false;
			}
		}

		if (transaksiKoperasi != null && transaksiKoperasi.getStatus().equals(TransaksiKoperasi.DISETUJU)) {
			setujui = true;
		}

		if (transaksiKoperasi.getDisposisiSop() != null
				&& transaksiKoperasi.getDisposisiSop().getDisposisiSetuju() != null
				&& transaksiKoperasi.getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() != null
				&& transaksiKoperasi.getDisposisiSop().getDisposisiSetuju().getSelesai()) {
			viewOnly = true;
		}

		try {
			if (transaksiKoperasi.getSatuanKerja() == null) {
				transaksiKoperasi.setSatuanKerja(Common.getSatuanKerja());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/koperasi/TransaksiKoperasiAction.java:569");
			// TODO: handle exception
		}

		Session session = HibernateUtil.currentSession();
		int count = ((Number) (transaksiKoperasi == null || transaksiKoperasi.getId() == null ? 0
				: session.createCriteria(TransaksiKoperasiDetail.class)
						.add(Restrictions.eq("transaksiKoperasi", transaksiKoperasi))
						.add(Restrictions.isNotNull("pembayaranAnggotaKoperasiDetail"))
						.setProjection(Projections.rowCount()).uniqueResult()))
				.intValue();

		if (count > 0) {
			persetujuan = true;
			viewOnly = true;

			if (save != null) {
				save.setVisible(false);
			}
		}

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Anggota Koperasi *"));
		anggotaKoperasi = new AmbilDataAnggotaKoperasiBanbox();
		anggotaKoperasi.setValue(
				transaksiKoperasi.getAnggotaKoperasi() == null ? "" : transaksiKoperasi.getAnggotaKoperasi().getNama());
		anggotaKoperasi.setAttribute("anggotaKoperasi", transaksiKoperasi.getAnggotaKoperasi());
		if (persetujuan || setujui || viewOnly || transaksiKoperasi.getId() != null) {
			row.appendChild(new Label(transaksiKoperasi.getAnggotaKoperasi() == null ? ""
					: transaksiKoperasi.getAnggotaKoperasi().getNama()));
		} else {
			row.appendChild(anggotaKoperasi);
		}
		anggotaKoperasi.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode"));
		if (transaksiKoperasi.getKode() == null) {
			String noAgenda = generateCode(false, transaksiKoperasi);
			transaksiKoperasi.setKode(noAgenda);
		}

		kode = new Label(transaksiKoperasi.getKode());
		if (persetujuan) {
			row.appendChild(new Label(transaksiKoperasi.getKode()));
		} else {
			row.appendChild(kode);
		}
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Produk Koperasi *"));
		produkKoperasi = new Combobox();
		produkKoperasi.setWidth("90%");
		if (produkKoperasi.getChildren().size() == 1 && transaksiKoperasi.getProdukKoperasi() == null) {
			produkKoperasi.setSelectedIndex(0);
		}

		if (persetujuan || setujui || viewOnly || transaksiKoperasi.getId() != null) {
			row.appendChild(new Label(transaksiKoperasi.getProdukKoperasi() == null ? ""
					: transaksiKoperasi.getProdukKoperasi().getNama()));
		} else {
			row.appendChild(produkKoperasi);
		}
		Common.selectComboItem(true, produkKoperasi, transaksiKoperasi.getProdukKoperasi());
		produkKoperasi.setReadonly(true);

		rowCaraPembayaranKoperasi = new MyFormRow();
		rowCaraPembayaranKoperasi.setParent(rows);
		rowCaraPembayaranKoperasi.appendChild(new ais.ui.util.MyLabelConfig("Cara Transaksi *"));
		caraPembayaranKoperasi = new Combobox();
		caraPembayaranKoperasi.setWidth("90%");

		if (caraPembayaranKoperasi.getChildren().size() == 1 && transaksiKoperasi.getCaraPembayaranKoperasi() == null) {
			caraPembayaranKoperasi.setSelectedIndex(0);
		}

		if (persetujuan || setujui || viewOnly) {
			rowCaraPembayaranKoperasi.appendChild(new Label(transaksiKoperasi.getCaraPembayaranKoperasi() == null ? ""
					: transaksiKoperasi.getCaraPembayaranKoperasi().getNama()));
		} else {
			rowCaraPembayaranKoperasi.appendChild(caraPembayaranKoperasi);
		}
		Common.selectComboItem(true, caraPembayaranKoperasi, transaksiKoperasi.getCaraPembayaranKoperasi());
		caraPembayaranKoperasi.setReadonly(true);

		final EventListener eventListenerKas = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				AnggotaKoperasi anggotaKoperasi = (AnggotaKoperasi) TransaksiKoperasiAction.this.anggotaKoperasi
						.getAttribute("anggotaKoperasi");

				if (anggotaKoperasi != null) {
					Common.insertCombo(produkKoperasi, new String[] { "kode", "nama" }, "keterangan",
							ProdukKoperasi.class, Restrictions.and(Restrictions.eq("aktif", true),
									Restrictions.eq("koperasi", anggotaKoperasi.getKoperasi())));

					Common.insertCombo(caraPembayaranKoperasi, new String[] { "kode", "nama" }, "keterangan",
							CaraPembayaranKoperasi.class,
							Restrictions.and(Restrictions.eq("aktif", true),
									Restrictions.or(Restrictions.isNull("koperasi"),
											Restrictions.eq("koperasi", anggotaKoperasi.getKoperasi()))));

					transaksiKoperasi.setSatuanKerja(anggotaKoperasi.getSatuanKerja());

					Common.selectComboItem(true, produkKoperasi, transaksiKoperasi.getProdukKoperasi());
					Common.selectComboItem(true, caraPembayaranKoperasi, transaksiKoperasi.getCaraPembayaranKoperasi());

					if (produkKoperasi.getChildren().size() == 1 && transaksiKoperasi.getProdukKoperasi() == null) {
						produkKoperasi.setSelectedIndex(0);
					}

					if (caraPembayaranKoperasi.getChildren().size() == 1
							&& transaksiKoperasi.getCaraPembayaranKoperasi() == null) {
						caraPembayaranKoperasi.setSelectedIndex(0);
					}
				}

			}
		};

		anggotaKoperasi.setEventListener(eventListenerKas);

		Common.createDefaultTimer(eventListenerKas);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Unit"));
		final Label unit = new Label();
		row.appendChild(unit);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Pengajuan Transaksi *"));
		Hbox hbox = new Hbox();
		row.appendChild(hbox);
		tanggal = new MyDatebox(transaksiKoperasi.getTanggal());
		tanggal.setFormat(Common.dateFormat3.get().toPattern());
		if (persetujuan || setujui || viewOnly) {
			hbox.appendChild(new Label(Common.dateFormat6.get().format(transaksiKoperasi.getTanggal())));
		} else {
			tanggal.setParent(hbox);
		}

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				ProdukKoperasi work = (ProdukKoperasi) (produkKoperasi.getSelectedItem() == null ? null
						: produkKoperasi.getSelectedItem().getValue());
				AnggotaKoperasi anggotaKoperasi = (AnggotaKoperasi) TransaksiKoperasiAction.this.anggotaKoperasi
						.getAttribute("anggotaKoperasi");

				transaksiKoperasi.setProdukKoperasi(work);
				transaksiKoperasi.setKode(kode.getValue());
				transaksiKoperasi.setNilai(nilai);
				transaksiKoperasi.setKeterangan(keterangan.getValue());
				transaksiKoperasi.setTanggal(tanggal.getValue());
				transaksiKoperasi.setSatuanKerja(anggotaKoperasi == null ? null : anggotaKoperasi.getSatuanKerja());
				transaksiKoperasi.setAnggotaKoperasi(anggotaKoperasi);
				transaksiKoperasi.setFormula(array.toString());

				String sts = (String) (status.getSelectedItem() == null ? null : status.getSelectedItem().getValue());
				if (sts != null && sts.equals(DanaTalangan.DISETUJU)) {
					transaksiKoperasi.setDisetujuiOleh(tbmuser);
					transaksiKoperasi.setTanggalPersetujuan(tanggalPersetujuanManual.getValue());
				} else {
					transaksiKoperasi.setDisetujuiOleh(null);
					transaksiKoperasi.setTanggalPersetujuan(null);
				}

				transaksiKoperasi.setStatus(sts);

				unit.setValue(transaksiKoperasi == null || transaksiKoperasi.getSatuanKerja() == null ? ""
						: transaksiKoperasi.getSatuanKerja().getNama());

				if (transaksiKoperasi.getId() == null) {
					String noAgenda = generateCode(false, transaksiKoperasi);
					transaksiKoperasi.setKode(noAgenda);
					kode.setValue(noAgenda);
				}

				rowCaraPembayaranKoperasi.setVisible(
						work != null && ConstantValues.PINJAMAN != null && work.getTipeProdukKoperasi() != null
								&& work.getTipeProdukKoperasi().getId().equals(ConstantValues.PINJAMAN.getId()));
			}
		};

		produkKoperasi.addEventListener("onChange", eventListener);

		lainMahasiswa = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Bukti Transaksi"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, transaksiKoperasi.getId(), TransaksiKoperasi.class.getName(),
				"Bukti Transaksi", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lainMahasiswa = (LampiranLain) arg0.getData();
					}
				});
		hbox.setParent(row);

		Common.initKeterangan(rows, "Jika file bukti transaksi lebih dari satu file, zip dulu semua file tersebut");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		keterangan = new Textbox(transaksiKoperasi.getKeterangan() == null ? "" : transaksiKoperasi.getKeterangan());

		if (setujui) {
			row.appendChild(
					new Label(transaksiKoperasi.getKeterangan() == null ? "" : transaksiKoperasi.getKeterangan()));
		} else {
			row.appendChild(keterangan);
		}

		keterangan.setWidth("90%");
		keterangan.setRows(2);

		nilai = 0.0;
		rowDetail = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(rowDetail, "2");
		rowDetail.setParent(rows);

		eventListenerDetail = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				ProdukKoperasi work = (ProdukKoperasi) (produkKoperasi.getSelectedItem() == null ? null
						: produkKoperasi.getSelectedItem().getValue());

				Common.clear(rowDetail);
				array = new JSONArray(transaksiKoperasi.getFormula());
				rowFormula = Common.tampilanScroll1(rowDetail);

				transaksiKoperasi.setProdukKoperasi(work);

				reloadFormula(rowFormula, array, persetujuan, setujui, viewOnly, transaksiKoperasi);
			}
		};

		rownilaiYangDiterimaDa = new MyFormRow();
		rownilaiYangDiterimaDa.setParent(rows);
		rownilaiYangDiterimaDa.appendChild(new ais.ui.util.MyLabelConfig("Nilai yang diterima"));
		nilaiYangDiterima = new Label();
		rownilaiYangDiterimaDa.appendChild(nilaiYangDiterima);

		rowpokokPinjaman = new MyFormRow();
		rowpokokPinjaman.setParent(rows);
		rowpokokPinjaman.appendChild(new ais.ui.util.MyLabelConfig("Pokok Pinjaman"));
		pokokPinjaman = new Label();
		pokokPinjaman.setStyle("font-weight:700;color:#0f172a;");
		rowpokokPinjaman.appendChild(pokokPinjaman);

		rownilaiYangDiterima = new MyFormRow();
		rownilaiYangDiterima.setParent(rows);
		rownilaiYangDiterima.appendChild(new ais.ui.util.MyLabelConfig("Jumlah Margin"));
		jumlahMargin = new Label();
		jumlahMargin.setStyle("font-weight:700;color:#0f172a;");
		rownilaiYangDiterima.appendChild(jumlahMargin);

		rowtotalPinjaman = new MyFormRow();
		rowtotalPinjaman.setParent(rows);
		rowtotalPinjaman.appendChild(new ais.ui.util.MyLabelConfig("Total Pinjaman"));
		totalPinjaman = new Label();
		totalPinjaman.setStyle("font-weight:800;color:#2563eb;font-size:15px;");
		rowtotalPinjaman.appendChild(totalPinjaman);

		rowdurasiPinjaman = new MyFormRow();
		rowdurasiPinjaman.setParent(rows);
		rowdurasiPinjaman.appendChild(new ais.ui.util.MyLabelConfig("Durasi Pinjaman"));
		durasiPinjaman = new Label();
		rowdurasiPinjaman.appendChild(durasiPinjaman);

		rowtenorPinjaman = new MyFormRow();
		rowtenorPinjaman.setParent(rows);
		rowtenorPinjaman.appendChild(new ais.ui.util.MyLabelConfig("Tenor Pinjaman"));
		tenorPinjaman = new Label();
		rowtenorPinjaman.appendChild(tenorPinjaman);

		rowtanggalMulaiDiangsur = new MyFormRow();
		rowtanggalMulaiDiangsur.setParent(rows);
		rowtanggalMulaiDiangsur
				.appendChild(tanggalMulaiDiangsurLabel = new ais.ui.util.MyLabelConfig("Tanggal Awal Angsuran *"));
		tanggalMulaiDiangsur = new MyDatebox(transaksiKoperasi.getTanggalMulaiDiangsur());

		if (setujui) {
			rowtanggalMulaiDiangsur.appendChild(new Label(transaksiKoperasi.getTanggalMulaiDiangsur() == null ? ""
					: Common.dateFormat6.get().format(transaksiKoperasi.getTanggalMulaiDiangsur())));
		} else {
			rowtanggalMulaiDiangsur.appendChild(tanggalMulaiDiangsur);
		}

		rowtanggalTerakhirDiangsur = new MyFormRow();
		rowtanggalTerakhirDiangsur.setParent(rows);
		rowtanggalTerakhirDiangsur.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Akhir Angsuran *"));
		tanggalTerakhirDiangsur = new Label(transaksiKoperasi.getTanggalTerakhirDiangsur() == null ? ""
				: Common.dateFormat6.get().format(transaksiKoperasi.getTanggalTerakhirDiangsur()));
		rowtanggalTerakhirDiangsur.appendChild(tanggalTerakhirDiangsur);

		rowDetailAngsuran = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(rowDetailAngsuran, "2");
		rowDetailAngsuran.setParent(rows);

		eventListenerDetailAngsuran = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				eventListenerDetail.onEvent(arg0);

				ProdukKoperasi work = (ProdukKoperasi) (produkKoperasi.getSelectedItem() == null ? null
						: produkKoperasi.getSelectedItem().getValue());
				transaksiKoperasi.setProdukKoperasi(work);
				transaksiKoperasi.setTanggalMulaiDiangsur(tanggalMulaiDiangsur.getValue());
				tanggalTerakhirDiangsur.setValue(transaksiKoperasi.getTanggalTerakhirDiangsur() == null ? ""
						: Common.dateFormat6.get().format(transaksiKoperasi.getTanggalTerakhirDiangsur()));

				durasiPinjaman.setValue(work == null ? "" : work.getDurasi());

				if (work != null && work.getTipeProdukKoperasi() != null && ConstantValues.SIMPANAN != null
						&& work.getTipeProdukKoperasi().getId().equals(ConstantValues.SIMPANAN.getId())) {
					tanggalMulaiDiangsurLabel.setValue("Tanggal Jatuh Tempo Dibayar *");
				} else {
					tanggalMulaiDiangsurLabel.setValue("Tanggal Awal Angsuran *");
				}

				boolean tampilPinjaman = work != null && work.getTipeProdukKoperasi() != null
						&& ConstantValues.PINJAMAN != null
						&& work.getTipeProdukKoperasi().getId().equals(ConstantValues.PINJAMAN.getId());

				rowpokokPinjaman.setVisible(tampilPinjaman);
				rownilaiYangDiterima.setVisible(tampilPinjaman);
				rownilaiYangDiterimaDa.setVisible(tampilPinjaman);

				rowtotalPinjaman.setVisible(tampilPinjaman);
				rowdurasiPinjaman.setVisible(tampilPinjaman);

				rowtenorPinjaman.setVisible(tampilPinjaman);
				rowtanggalTerakhirDiangsur.setVisible(tampilPinjaman);

				tenorPinjaman.setValue(
						work == null ? "" : Common.numberFormat.get().format(work.getJangkaWaktuBulan()) + " Bulan");

				Common.clear(rowDetailAngsuran);
				rowFormulaAngsuran = Common.tampilanScroll1(rowDetailAngsuran);

				reloadAngsuran(rowFormulaAngsuran, persetujuan, setujui, viewOnly, transaksiKoperasi);
			}
		};

		eventListenerDetailAngsuran.onEvent(null);
		produkKoperasi.addEventListener("onChange", eventListenerDetailAngsuran);
		tanggalMulaiDiangsur.addEventListener("onChange", eventListenerDetailAngsuran);

		row = new MyFormRow();
		row.setVisible(persetujuan && !viewOnly && (disposisiSop == null || disposisiSop.getId() == null));
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Pengajuan"));
		status = new Radiogroup();
		Radio comboitem = new Radio(UangMuka.PENGAJUAN);
		comboitem.setAttribute("value", UangMuka.PENGAJUAN);
		comboitem.setValue(UangMuka.PENGAJUAN);
		comboitem.setVisible(false);
		status.appendChild(comboitem);
		comboitem = new Radio(UangMuka.DISETUJU);
		comboitem.setAttribute("value", UangMuka.DISETUJU);
		comboitem.setValue(UangMuka.DISETUJU);
		status.appendChild(comboitem);
		comboitem = new Radio(UangMuka.DITOLAK);
		comboitem.setAttribute("value", UangMuka.DITOLAK);
		comboitem.setValue(UangMuka.DITOLAK);
		status.appendChild(comboitem);
		status.setWidth("90%");
		Common.selectRadioItem(status, transaksiKoperasi.getStatus());
		row.appendChild(status);

		grid.setAttribute("eventListenerSetuju", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (arg0 != null && arg0.getTarget() instanceof Checkbox) {
					Checkbox checkbox = (Checkbox) arg0.getTarget();
					Boolean selesai = (Boolean) checkbox.getAttribute("checkbox");
					if (selesai != null && selesai) {
						Common.selectRadioItem(status, UangMuka.DISETUJU);
						Common.freeze(status, true);
					} else {
						status.setSelectedItem(null);
						Common.freeze(status, false);
					}
				}
			}
		});

		if (setujuiData != null) {
			status.addEventListener("onClick", setujuiData);

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					setujuiData.onEvent(new Event("", null, transaksiKoperasi.getStatus().equals(UangMuka.DISETUJU)));
				}
			});
		}

		if (setujui) {
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Status Pengajuan"));
			row.appendChild(new ais.ui.util.MyLabelConfig(transaksiKoperasi.getStatus()));
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Persetujuan"));
		tanggalPersetujuanManual = new MyDatebox(transaksiKoperasi.getTanggalPersetujuanManual());

		if (transaksiKoperasi.getPostingHistory() == null) {
			row.appendChild(tanggalPersetujuanManual);
		} else {
			row.appendChild(new Label(Common.dateFormat1.get()
					.format(transaksiKoperasi.getTanggalPersetujuanManual() == null ? WaktuUtil.getDate()
							: transaksiKoperasi.getTanggalPersetujuanManual())));
		}

		tanggalPersetujuanManual.setReadonly(true);
		tanggalPersetujuanManual.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (transaksiKoperasi != null && transaksiKoperasi.getId() != null) {
					transaksiKoperasi.setTanggalPersetujuanManual(tanggalPersetujuanManual.getValue());
					Common.refreshUpdate(transaksiKoperasi);
				}
			}
		});

		Common.createDefaultTimer(eventListener);

		EventListener s = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				boolean setujui = status.getSelectedItem() == null ? false
						: status.getSelectedItem().getValue().equals(TransaksiKoperasi.DISETUJU);

				if (tanggalPersetujuanManual != null && tanggalPersetujuanManual.getParent() != null) {
					if (tanggalPersetujuanManual.getValue() == null) {
						tanggalPersetujuanManual.setValue(WaktuUtil.getDate());
					}
					tanggalPersetujuanManual.getParent().setVisible(setujui);
				}

				if (setujui) {
					save.setLabel("Selesaikan dan Setujui Transaksi Koperasi");
				} else {
					save.setLabel(!persetujuan ? "Simpan dan Cetak" : "Ubah Status Persetujuan dan Cetak");
				}
			}
		};

		status.addEventListener("onClick", s);
		Common.createDefaultTimer(s);

		return grid;
	}

	public void reloadAngsuran(final Row rowFormula, final boolean persetujuan, final boolean setujui,
			final boolean viewOnly, final TransaksiKoperasi transaksiKoperasi) throws Exception {
		Common.clear(rowFormula);

		ProdukKoperasi work = transaksiKoperasi.getProdukKoperasi();

		if (work != null && work.getTipeProdukKoperasi() != null && ConstantValues.SIMPANAN != null
				&& work.getTipeProdukKoperasi().getId().equals(ConstantValues.SIMPANAN.getId())
				&& transaksiKoperasi.getTanggalMulaiDiangsur() != null) {

			Grid grid = new Grid();
			grid.setSclass("dgrid");
			grid.setWidth("100%");
			grid.setParent(rowFormula);
			grid.setWidth("100%");
			grid.setHeight("100%");

			Columns columns = new Columns();
			columns.setParent(grid);

			MyColumnConfig column = new MyColumnConfig("Pembayaran ke");
			column.setParent(columns);

			column = new MyColumnConfig("Tanggal Jatuh Tempo");
			column.setParent(columns);
			column.setWidth("25%");

			column = new MyColumnConfig("Nilai Dibayar");
			column.setAlign("right");
			column.setParent(columns);
			column.setWidth("12%");

			Foot foot = new Foot();
			foot.setParent(grid);

			Footer footer = new Footer("Total");
			foot.appendChild(footer);

			footer = new Footer("");
			foot.appendChild(footer);

			Footer footerTotalPokok = new Footer("0");
			foot.appendChild(footerTotalPokok);

			Rows rows = new Rows();
			rows.setParent(grid);

			Integer jumlahAngsur = work.getJumlahTransaksiTerbentuk();

			if (jumlahAngsur != null) {
				Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
				calendar.setTime(transaksiKoperasi.getTanggalMulaiDiangsur());

				Double totalPokok = 0.0;

				for (int i = 1; i <= work.getJumlahTransaksiTerbentuk(); i++) {
					Date tanggal = calendar.getTime();

					totalPokok += nilai;

					MyFormRow row = new MyFormRow();
					row.setValign("top");
					row.setParent(rows);
					row.appendChild(new Label("Pembayaran ke-" + (i)));
					row.appendChild(new Label(Common.dateFormat6.get().format(tanggal)));
					row.appendChild(new Label(Common.numberFormat.get().format(nilai)));

					if (work.getDurasi().equals(ProdukKoperasi.HARIAN)) {
						calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
					} else if (work.getDurasi().equals(ProdukKoperasi.MINGGUAN)) {
						calendar.set(Calendar.WEEK_OF_YEAR, calendar.get(Calendar.WEEK_OF_YEAR) + 1);
					} else if (work.getDurasi().equals(ProdukKoperasi.BULANAN)) {
						calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 1);
					} else if (work.getDurasi().equals(ProdukKoperasi.TAHUNAN)) {
						calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) + 1);
					}
				}
				footerTotalPokok.setLabel(Common.numberFormat.get().format(totalPokok));
			}

		}

		else if (work != null && work.getTipeProdukKoperasi() != null && ConstantValues.PINJAMAN != null
				&& work.getTipeProdukKoperasi().getId().equals(ConstantValues.PINJAMAN.getId())
				&& transaksiKoperasi.getTanggalMulaiDiangsur() != null) {
			Grid grid = new Grid();
			grid.setSclass("dgrid");
			grid.setWidth("100%");
			grid.setParent(rowFormula);
			grid.setWidth("100%");
			grid.setHeight("100%");

			Columns columns = new Columns();
			columns.setParent(grid);

			MyColumnConfig column = new MyColumnConfig("Angsuran ke");
			column.setParent(columns);

			column = new MyColumnConfig("Tanggal Jatuh Tempo");
			column.setParent(columns);
			column.setWidth("25%");

			column = new MyColumnConfig("Pokok");
			column.setAlign("right");
			column.setParent(columns);
			column.setWidth("12%");

			column = new MyColumnConfig("Margin");
			column.setAlign("right");
			column.setParent(columns);
			column.setWidth("12%");

			column = new MyColumnConfig("Sisa Pokok");
			column.setAlign("right");
			column.setParent(columns);
			column.setWidth("12%");

			Foot foot = new Foot();
			foot.setParent(grid);

			Footer footer = new Footer("Total");
			foot.appendChild(footer);

			footer = new Footer("");
			foot.appendChild(footer);

			Footer footerTotalPokok = new Footer("0");
			foot.appendChild(footerTotalPokok);

			Footer footerTotalMargin = new Footer("0");
			foot.appendChild(footerTotalMargin);

			footer = new Footer("");
			foot.appendChild(footer);

			Rows rows = new Rows();
			rows.setParent(grid);

			Integer jumlahAngsur = transaksiKoperasi.getJumlahAngsur();

			if (jumlahAngsur != null) {
				Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
				calendar.setTime(transaksiKoperasi.getTanggalMulaiDiangsur());
				Calendar s = ais.ui.util.WaktuUtil.getCalendar();
				s.setTime(transaksiKoperasi.getTanggalTerakhirDiangsur());
				s.set(Calendar.DATE, s.get(Calendar.DATE) - 1);

				int i = 1;
				Double sisa = nilai;

				Double totalPokok = 0.0;
				Double totalMargin = 0.0;

				while (calendar.getTime().before(s.getTime())) {
					Date tanggal = calendar.getTime();
					Double pokok = nilai / jumlahAngsur.doubleValue();
					Double m = transaksiKoperasi.getMargin() / jumlahAngsur.doubleValue();

					totalPokok += pokok;
					totalMargin += m;

					sisa = sisa - pokok;

					MyFormRow row = new MyFormRow();
					row.setValign("top");
					row.setParent(rows);
					row.appendChild(new Label("Angsuran ke-" + (i)));
					row.appendChild(new Label(Common.dateFormat6.get().format(tanggal)));
					row.appendChild(new Label(Common.numberFormat.get().format(pokok)));
					row.appendChild(new Label(Common.numberFormat.get().format(m)));
					row.appendChild(new Label(Common.numberFormat.get().format(sisa)));
					i++;

					if (work.getDurasi().equals(ProdukKoperasi.HARIAN)) {
						calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
					} else if (work.getDurasi().equals(ProdukKoperasi.MINGGUAN)) {
						calendar.set(Calendar.WEEK_OF_YEAR, calendar.get(Calendar.WEEK_OF_YEAR) + 1);
					} else if (work.getDurasi().equals(ProdukKoperasi.BULANAN)) {
						calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 1);
					} else if (work.getDurasi().equals(ProdukKoperasi.TAHUNAN)) {
						calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) + 1);
					}
				}
				footerTotalPokok.setLabel(Common.numberFormat.get().format(totalPokok));
				footerTotalMargin.setLabel(Common.numberFormat.get().format(totalMargin));
			}

		}
	}

	public void reloadFormula(final Row rowFormula, final JSONArray array, final boolean persetujuan,
			final boolean setujui, final boolean viewOnly, final TransaksiKoperasi transaksiKoperasi) throws Exception {
		final MyFormRow rowU = new MyFormRow();

		if (transaksiKoperasi != null && transaksiKoperasi.getId() == null
				&& transaksiKoperasi.getProdukKoperasi() != null) {

			for (int i = 0; i < array.length(); i++) {
				JSONObject jsonObject = array.getJSONObject(i);
				if (!jsonObject.isNull("defaultProduk")) {
					jsonObject.remove("key");
				}
			}

			JSONArray arrayProduk = new JSONArray(transaksiKoperasi.getProdukKoperasi().getFormula());
			for (int i = 0; i < arrayProduk.length(); i++) {
				JSONObject jsonObject = arrayProduk.getJSONObject(i);
				if (!jsonObject.isNull("key")) {
					jsonObject.put("defaultProduk", true);

					array.put(jsonObject);
				}
			}
		}

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah Transaksi", "/img/svg/addthis.svg");
		button.setTooltiptext("Hapus Data");
		button.setVisible(!persetujuan && !setujui && !viewOnly);
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				JSONObject jsonObject = new JSONObject();
				jsonObject.put("nama", "");
				jsonObject.put("qty", 1.0);
				jsonObject.put("harga", 0.0);
				jsonObject.put("jumlah", 0.0);

				jsonObject.put("bolehJns", true);
				jsonObject.put("bolehQty", true);
				jsonObject.put("bolehNilai", true);

				Long key = Math.abs(Common.randLong());
				jsonObject.put("key", key);

				array.put(jsonObject);

				reloadDataFormula(rowU, array, persetujuan, setujui, viewOnly, transaksiKoperasi.getProdukKoperasi());
			}
		});
		button.setParent(rowFormula);

		rowU.setParent(rowFormula.getParent());

		reloadDataFormula(rowU, array, persetujuan, setujui, viewOnly, transaksiKoperasi.getProdukKoperasi());

	}

	public void reloadDataFormula(final Row rowU, final JSONArray array, final boolean persetujuan,
			final boolean setujui, final boolean viewOnly, final ProdukKoperasi work) throws Exception {
		Common.clear(rowU);

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(rowU);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig("Keterangan Transaksi");
		column.setParent(columns);

		column = new MyColumnConfig("Jenis Transaksi");
		column.setParent(columns);
		column.setWidth("25%");

		column = new MyColumnConfig("Qty");
		column.setAlign("right");
		column.setParent(columns);
		column.setWidth("8%");

		column = new MyColumnConfig("Nilai");
		column.setAlign("right");
		column.setParent(columns);
		column.setWidth("12%");

		column = new MyColumnConfig("Total");
		column.setAlign("right");
		column.setParent(columns);
		column.setWidth("12%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("8%");

		Foot foot = new Foot();
		foot.setParent(grid);

		Footer footer = new Footer("Total");
		foot.appendChild(footer);

		footer = new Footer("");
		foot.appendChild(footer);

		footer = new Footer("");
		foot.appendChild(footer);

		footer = new Footer("");
		foot.appendChild(footer);

		final Footer footerTotal = new Footer("");
		foot.appendChild(footerTotal);

		footer = new Footer("");
		foot.appendChild(footer);

		Rows rows = new Rows();
		rows.setParent(grid);

		final EventListener hitungTotal = new EventListener() {

			private double nilaiSemua;
			private double nilaiAdmin;

			@Override
			public void onEvent(Event arg0) throws Exception {
				nilai = 0.0;
				nilaiSemua = 0.0;

				nilaiAdmin = 0.0;
				yangDiterima = 0.0;
				for (int i = 0; i < array.length(); i++) {

					JSONObject jsonObject = array.getJSONObject(i);
					Long key = null;
					if (!jsonObject.isNull("key")) {
						key = ais.common.CommonJSONUtil.ambilLong(jsonObject,"key");
					}

					if (key != null) {

						JenisTransaksiKoperasi jenisTransaksiKoperasi = (JenisTransaksiKoperasi) (jsonObject
								.isNull("jenisTransaksiKoperasi") ? null
										: ConstantValues.ambil(JenisTransaksiKoperasi.class.getName(),
												ais.common.CommonJSONUtil.ambilLong(jsonObject,"jenisTransaksiKoperasi")));

						if (!jsonObject.isNull("jumlah") && jenisTransaksiKoperasi != null) {
							if (jenisTransaksiKoperasi.getMenghitungTotal()) {
								nilai += jsonObject.getDouble("jumlah");
							} else {
								nilaiAdmin += jsonObject.getDouble("jumlah");
							}
						}

						if (!jsonObject.isNull("jumlah")) {
							nilaiSemua += jsonObject.getDouble("jumlah");
						}
					}
				}

				yangDiterima = (nilai - nilaiAdmin);

				transaksiKoperasi.setProdukKoperasi(work);
				transaksiKoperasi.setNilai(nilai);
				footerTotal.setLabel(Common.numberFormat.get().format(nilaiSemua));
				nilaiYangDiterima.setValue(Common.numberFormat.get().format(yangDiterima));
				pokokPinjaman.setValue(Common.numberFormat.get().format(nilai));

				if (work != null) {

					jumlahMargin.setValue(Common.numberFormat.get().format(transaksiKoperasi.getMargin()));

					totalPinjaman.setValue(Common.numberFormat.get().format(transaksiKoperasi.getMargin() + nilai));
				}
			}

		};

		hitungTotal.onEvent(null);
		for (int i = 0; i < array.length(); i++) {
			final int index = i;
			final JSONObject jsonObject = array.getJSONObject(i);
			Long key = null;
			if (!jsonObject.isNull("key")) {
				key = ais.common.CommonJSONUtil.ambilLong(jsonObject,"key");
			}

			if (key != null) {

				JenisTransaksiKoperasi jenisTransaksiKoperasi = (JenisTransaksiKoperasi) (jsonObject
						.isNull("jenisTransaksiKoperasi") ? null
								: ConstantValues.ambil(JenisTransaksiKoperasi.class.getName(),
										ais.common.CommonJSONUtil.ambilLong(jsonObject,"jenisTransaksiKoperasi")));

				String nama = "";

				if (!jsonObject.isNull("nama")) {
					nama = jsonObject.get("nama") + "";
				}

				Double qty = 1.0;
				if (!jsonObject.isNull("qty")) {
					qty = jsonObject.getDouble("qty");
				}

				Double harga = 0.0;
				if (!jsonObject.isNull("harga")) {
					harga = jsonObject.getDouble("harga");
				}

				Double jumlah = 0.0;
				if (!jsonObject.isNull("jumlah")) {
					jumlah = jsonObject.getDouble("jumlah");
				}

				String nama_file = "";

				if (!jsonObject.isNull("nama_file")) {
					nama_file = jsonObject.get("nama_file") + "";
				}

				String link = "";

				if (!jsonObject.isNull("link")) {
					link = jsonObject.get("link") + "";
				}

				Boolean boleh = jenisTransaksiKoperasi == null ? false : jenisTransaksiKoperasi.getBolehJns();

				Boolean bolehQty = jenisTransaksiKoperasi == null ? false : jenisTransaksiKoperasi.getBolehQty();

				Boolean bolehNilai = jenisTransaksiKoperasi == null ? false : jenisTransaksiKoperasi.getBolehNilai();

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);

				final Combobox jenisTranskasi = new Combobox();
				Common.insertCombo(jenisTranskasi, "nama", "keterangan", JenisTransaksiKoperasi.class, Restrictions.and(
						work == null || work.getTipeProdukKoperasi() == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("tipeProdukKoperasi", work.getTipeProdukKoperasi()),
						Restrictions.eq("aktif", true)));
				Common.selectComboItem(true, jenisTranskasi, jenisTransaksiKoperasi);
				jenisTranskasi.setReadonly(true);
				jenisTranskasi.setWidth("95%");

				final Label nilai = new Label(Common.numberFormat.get().format(jumlah));

				final MyTextbox targetText = new MyTextbox(nama);

				Vbox myvbox = new Vbox();
				myvbox.setParent(row);
				myvbox.setWidth("95%");

				final MyDoublebox qtyBox = new MyDoublebox(qty);
				final MyDoublebox hargaBox = new MyDoublebox(harga);

				targetText.setWidth("95%");
				qtyBox.setWidth("95%");
				hargaBox.setWidth("95%");

				if (persetujuan || setujui || viewOnly) {
					row.appendChild(new Label(jenisTransaksiKoperasi == null ? "" : jenisTransaksiKoperasi.getNama()));
					myvbox.appendChild(new Label(nama));
					row.appendChild(new Label(Common.numberFormat.get().format(qty)));
					row.appendChild(new Label(Common.numberFormat.get().format(harga)));
				} else {
					// Bila jenis transaksi kosong/tak dikenali (mis. baris pokok pinjaman belum diberi
					// jenis), tampilkan combo yang dapat dipilih agar bisa diperbaiki langsung — bukan
					// label kosong yang membingungkan. Nilai combo tetap dibaca saat simpan (lihat di
					// bawah), sehingga pilihan pengguna tersimpan ke formula.
					row.appendChild((jenisTransaksiKoperasi == null || Boolean.TRUE.equals(boleh)) ? jenisTranskasi
							: new Label(jenisTransaksiKoperasi.getNama()));

					myvbox.appendChild(targetText);
					row.appendChild(bolehQty ? qtyBox : new Label(Common.numberFormat.get().format(qty)));
					row.appendChild(bolehNilai ? hargaBox : new Label(Common.numberFormat.get().format(harga)));
				}
				final LampiranLain lampiranLain = LampiranLain.ambil(key, "Dokumen Transaksi Koperasi");

				if (lampiranLain != null) {

					A a = new A(lampiranLain.getNama());
					a.setParent(myvbox);
					a.setWidth("95%");

					a.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							Common.display(lampiranLain);
						}
					});

				}

				else if (!nama_file.isEmpty() && !link.isEmpty()) {

					A a = new A(nama_file);
					a.setParent(myvbox);
					a.setWidth("95%");
					final String url = link;
					a.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							Clients.evalJavaScript(
									"popupCenter({url: '" + url + "', title: 'Data', w: 1200, h: 600});");
						}
					});

				} else {

					Hbox hbox = new Hbox();
					hbox.setParent(myvbox);
					LampiranLain.createDownloadUploadFileLain(hbox, key, "Dokumen Transaksi Koperasi", "Bukti", false,
							new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									LampiranLain lampiranLain = (LampiranLain) arg0.getData();
									jsonObject.put("link", lampiranLain.createLinkUri(false));
									jsonObject.put("nama_file", lampiranLain.getNama());
									jsonObject.put("id_file", lampiranLain.getId());
								}
							}, null, false, false, false, !(persetujuan || setujui || viewOnly));
				}

				EventListener eventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						JenisTransaksiKoperasi jenisTransaksiKoperasi = (JenisTransaksiKoperasi) (jenisTranskasi
								.getSelectedItem() == null ? null : jenisTranskasi.getSelectedItem().getValue());

						jsonObject.put("jenisTransaksiKoperasi",
								jenisTransaksiKoperasi == null ? null : jenisTransaksiKoperasi.getId());
						jsonObject.put("nama", targetText.getValue());
						jsonObject.put("qty", qtyBox.getValue());
						jsonObject.put("harga", hargaBox.getValue());

						Double jumlah = (qtyBox.getValue() == null ? 0.0 : qtyBox.getValue())
								* (hargaBox.getValue() == null ? 0.0 : hargaBox.getValue());
						jsonObject.put("jumlah", jumlah);
						nilai.setValue(Common.numberFormat.get().format(jumlah));

						Common.clear(rowDetailAngsuran);
						rowFormulaAngsuran = Common.tampilanScroll1(rowDetailAngsuran);

						reloadAngsuran(rowFormulaAngsuran, persetujuan, setujui, viewOnly, transaksiKoperasi);

						hitungTotal.onEvent(null);
					}
				};

				targetText.setRows(2);

				jenisTranskasi.addEventListener("onChange", eventListener);

				qtyBox.addEventListener("onChange", eventListener);
				targetText.addEventListener("onChange", eventListener);
				hargaBox.addEventListener("onChange", eventListener);
				nilai.setParent(row);

				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
				button.setTooltiptext("Hapus Data");
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menghapus data ini? Data yang telah dihapus tidak dapat dikembalikan.", "Pertanyaan",
								MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
								new EventListener() {

									@Override
									public void onEvent(Event event) throws Exception {
										int i = Integer.parseInt(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											try {
												array.put(index, new JSONObject());

												reloadDataFormula(rowU, array, persetujuan, setujui, viewOnly, work);

											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
												MyMessageboxConfig.show(MyMessageboxConfig.format(
														"Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lainnya. Rincian: {V1}. Langkah yang dapat dilakukan: (1) hapus atau lepaskan data terkait terlebih dahulu; (2) ulangi penghapusan; (3) apabila masih gagal, mohon hubungi administrator sistem.",
														e.getMessage()));
											}

										}

									}
								});

					}
				});

				if (persetujuan || setujui || viewOnly) {
					new Label().setParent(row);
				} else {
					button.setParent(row);
				}
			}
		}
	}

	private void init(final TransaksiKoperasi transaksiKoperasi) throws Exception {
		addWindow.setTitle("Transaksi");

		if (transaksiKoperasi.getDibuatOleh() == null) {
			transaksiKoperasi.setDibuatOleh(tbmuser);
			transaksiKoperasi.setTanggalPembuatan(new Date());
		}

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan dan Cetak", "/img/save.gif");

		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(addWindow);

		// Header banner modern (HTML/CSS, responsif) agar form transaksi lebih enak dipandang.
		North header = new North();
		header.setBorder("none");
		header.setParent(borderlayout);
		header.appendChild(DashboardUiKit.html(DashboardUiKit.headerModul("Transaksi Koperasi",
				"Catat pengajuan pinjaman atau simpanan anggota: pilih produk, isi rincian transaksi, "
						+ "lalu Simpan & Cetak bukti.")));

		Center center = new Center();
		center.setParent(borderlayout);
		disposisiSop=null;center.appendChild(form(transaksiKoperasi, disposisiSop, save, null));
		ais.ui.util.ZkCompat.setFlex(center, true);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
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

					addWindow.setVisible(false);

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							onSearchDefault(null);
						}
					});
				}
			}
		});
		save.setParent(toolbar);

		if (!persetujuan && setujui) {
			save.setVisible(false);
			cancel.setLabel("Tutup");
		}

	}

	public boolean onSave(Event event) throws Exception {

		AnggotaKoperasi anggotaKoperasi = (AnggotaKoperasi) TransaksiKoperasiAction.this.anggotaKoperasi
				.getAttribute("anggotaKoperasi");

		if (anggotaKoperasi == null) {
			MyMessageboxConfig.show("Mohon maaf, anggota koperasi belum diisi. Langkah yang dapat dilakukan: (1) pilih anggota koperasi terlebih dahulu; (2) ulangi penyimpanan transaksi.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		transaksiKoperasi.setFormula(array.toString());
		JSONArray array = new JSONArray(transaksiKoperasi.getFormula());
		for (int i = 0; i < array.length(); i++) {

			JSONObject jsonObject = array.getJSONObject(i);
			Long key = null;
			if (!jsonObject.isNull("key")) {
				key = ais.common.CommonJSONUtil.ambilLong(jsonObject,"key");
			}

			if (key != null) {
				JenisTransaksiKoperasi jenisTransaksiKoperasi = (JenisTransaksiKoperasi) (jsonObject
						.isNull("jenisTransaksiKoperasi") ? null
								: ConstantValues.ambil(JenisTransaksiKoperasi.class.getName(),
										ais.common.CommonJSONUtil.ambilLong(jsonObject,"jenisTransaksiKoperasi")));

				Double jumlah = 0.0;
				if (!jsonObject.isNull("jumlah")) {
					jumlah = jsonObject.getDouble("jumlah");
				}

				if (jenisTransaksiKoperasi == null || !jenisTransaksiKoperasi.getAktif()) {
					MyMessageboxConfig.show("Mohon maaf, masih terdapat jenis transaksi yang belum lengkap atau tidak aktif. Langkah yang dapat dilakukan: (1) lengkapi seluruh jenis transaksi pada rincian; (2) pastikan jenis transaksi yang dipilih masih aktif; (3) ulangi penyimpanan.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return false;
				}

				if (jumlah.intValue() == 0) {
					MyMessageboxConfig.show("Mohon maaf, nilai biaya pengeluaran tidak boleh 0. Langkah yang dapat dilakukan: (1) isi nominal biaya dengan angka lebih dari 0; (2) ulangi penyimpanan.", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return false;
				}
			}
		}

		nilai = 0.0;
		for (int i = 0; i < array.length(); i++) {

			JSONObject jsonObject = array.getJSONObject(i);
			Long key = null;
			if (!jsonObject.isNull("key")) {
				key = ais.common.CommonJSONUtil.ambilLong(jsonObject,"key");
			}

			if (key != null) {
				JenisTransaksiKoperasi jenisTransaksiKoperasi = (JenisTransaksiKoperasi) (jsonObject
						.isNull("jenisTransaksiKoperasi") ? null
								: ConstantValues.ambil(JenisTransaksiKoperasi.class.getName(),
										ais.common.CommonJSONUtil.ambilLong(jsonObject,"jenisTransaksiKoperasi")));
				if (jenisTransaksiKoperasi != null && jenisTransaksiKoperasi.getMenghitungTotal()) {
					nilai += jsonObject.getDouble("jumlah");
				}
			}
		}

		System.out.println("array -> " + array + " nilai " + nilai);

		ProdukKoperasi produkKoperasi = (ProdukKoperasi) (this.produkKoperasi.getSelectedItem() == null ? null
				: this.produkKoperasi.getSelectedItem().getValue());

		if (produkKoperasi == null) {
			MyMessageboxConfig.show("Mohon maaf, produk koperasi belum dipilih. Langkah yang dapat dilakukan: (1) pilih salah satu produk koperasi; (2) ulangi penyimpanan transaksi.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (produkKoperasi.getHanyaBolehSekaliTransaksi()) {
			if (checkTransaksi()) {
				MyMessageboxConfig.showFormat(
						"Mohon maaf, transaksi \"{V1}\" atas nama \"{V2}\" sudah pernah dilakukan sebelumnya dan hanya boleh dilakukan satu kali. Langkah yang dapat dilakukan: (1) periksa kembali riwayat transaksi anggota; (2) apabila memang berbeda, gunakan produk koperasi yang sesuai.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION,
						produkKoperasi.getNama(), anggotaKoperasi.getNama());
				return false;
			}
		}

		if (nilai > produkKoperasi.getNilaiMaksimal()) {
			MyMessageboxConfig.showFormat(
					"Mohon maaf, nilai transaksi melebihi batas maksimal yang diperbolehkan, yaitu {V1}. Langkah yang dapat dilakukan: (1) sesuaikan nilai transaksi agar tidak melebihi batas maksimal; (2) ulangi penyimpanan.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION,
					Common.numberFormat.get().format(produkKoperasi.getNilaiMaksimal()));
			return false;
		}
		if (nilai < produkKoperasi.getNilaiMinimal()) {
			MyMessageboxConfig.showFormat(
					"Mohon maaf, nilai transaksi kurang dari batas minimal yang diperbolehkan, yaitu {V1}. Langkah yang dapat dilakukan: (1) sesuaikan nilai transaksi agar tidak kurang dari batas minimal; (2) ulangi penyimpanan.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION,
					Common.numberFormat.get().format(produkKoperasi.getNilaiMinimal()));
			return false;
		}

		// BMPP (Batas Maksimum Pemberian Pinjaman) SOM USPK — peringatan LUNAK (tidak memblokir):
		// pinjaman ke pihak tidak terkait maks 15% Modal Sendiri; pengurus/pengawas 10%.
		if (produkKoperasi.getTipeProdukKoperasi() != null && ConstantValues.PINJAMAN != null
				&& produkKoperasi.getTipeProdukKoperasi().getId() != null
				&& produkKoperasi.getTipeProdukKoperasi().getId().equals(ConstantValues.PINJAMAN.getId())) {
			try {
				double modalSendiri = 0.0;
				Session sesiModal = HibernateUtil.currentSession();
				Long tipeSimpanan = ConstantValues.SIMPANAN != null ? ConstantValues.SIMPANAN.getId() : null;
				if (tipeSimpanan != null) {
					@SuppressWarnings("unchecked")
					List<TransaksiKoperasi> simpananList = sesiModal.createQuery(
							"select distinct t from TransaksiKoperasi t left join fetch t.produkKoperasi p "
									+ "where p.tipeProdukKoperasi.id = :tipe").setParameter("tipe", tipeSimpanan).list();
					for (TransaksiKoperasi ts : simpananList) {
						try {
							String nmProduk = ts.getProdukKoperasi() == null || ts.getProdukKoperasi().getNama() == null
									? ""
									: ts.getProdukKoperasi().getNama().toLowerCase();
							if (nmProduk.contains("pokok") || nmProduk.contains("wajib")) {
								modalSendiri += ts.getNilai();
							}
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/koperasi/TransaksiKoperasiAction.java:1860");
						}
					}
				}
				boolean pihakTerkait = anggotaKoperasi != null && anggotaKoperasi.getPihakTerkait();
				double persenBmpp = pihakTerkait ? 0.10 : 0.15;
				double batasBmpp = modalSendiri * persenBmpp;
				if (batasBmpp > 0 && nilai > batasBmpp) {
					MyMessageboxConfig.showFormat(
							"Perhatian: nilai pinjaman melebihi Batas Maksimum Pemberian Pinjaman (BMPP) sebesar {V1} dari Modal Sendiri (~Rp {V2}). Mohon dipastikan memperoleh persetujuan sesuai SOM sebelum melanjutkan proses.",
							"Peringatan BMPP", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION,
							(pihakTerkait ? "10% (pihak terkait)" : "15% (pihak tidak terkait)"),
							Common.numberFormat.get().format(batasBmpp));
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}

		CaraPembayaranKoperasi caraPembayaranKoperasi = (CaraPembayaranKoperasi) (this.caraPembayaranKoperasi
				.getSelectedItem() == null ? null : this.caraPembayaranKoperasi.getSelectedItem().getValue());

		if (caraPembayaranKoperasi == null && produkKoperasi.getTipeProdukKoperasi() != null
				&& ConstantValues.PINJAMAN != null
				&& produkKoperasi.getTipeProdukKoperasi().getId().equals(ConstantValues.PINJAMAN.getId())) {
			MyMessageboxConfig.show("Mohon maaf, cara transaksi belum dipilih. Langkah yang dapat dilakukan: (1) pilih salah satu cara transaksi; (2) ulangi penyimpanan.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (tanggal.getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, tanggal pengajuan transaksi belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Tanggal Pengajuan Transaksi; (2) ulangi penyimpanan.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();

		if (transaksiKoperasi.getId() != null) {
			transaksiKoperasi = (TransaksiKoperasi) session.load(TransaksiKoperasi.class, transaksiKoperasi.getId());
		}

		if (transaksiKoperasi.getDibuatOleh() == null) {
			transaksiKoperasi.setDibuatOleh(tbmuser);
			transaksiKoperasi.setTanggalPembuatan(new Date());
		}
		if (disposisiSop != null && disposisiSop.getId() != null) {
			transaksiKoperasi.setDisposisiSop(disposisiSop);
		}

		transaksiKoperasi.setProdukKoperasi(produkKoperasi);
		transaksiKoperasi.setKode(kode.getValue());
		transaksiKoperasi.setNilai(nilai);
		transaksiKoperasi.setKeterangan(keterangan.getValue());
		transaksiKoperasi.setTanggal(tanggal.getValue());
		transaksiKoperasi.setSatuanKerja(anggotaKoperasi == null ? null : anggotaKoperasi.getSatuanKerja());
		transaksiKoperasi.setAnggotaKoperasi(anggotaKoperasi);
		transaksiKoperasi.setFormula(array.toString());
		transaksiKoperasi.setCaraPembayaranKoperasi(caraPembayaranKoperasi);
		transaksiKoperasi.setYangDiterima(yangDiterima);
		String sts = (String) (status.getSelectedItem() == null ? null : status.getSelectedItem().getValue());
		if (sts != null && sts.equals(DanaTalangan.DISETUJU)) {
			transaksiKoperasi.setDisetujuiOleh(tbmuser);
			transaksiKoperasi.setTanggalPersetujuan(tanggalPersetujuanManual.getValue());
		} else {
			transaksiKoperasi.setDisetujuiOleh(null);
			transaksiKoperasi.setTanggalPersetujuan(null);
		}
		transaksiKoperasi.setTanggalPersetujuanManual(tanggalPersetujuanManual.getValue());
		transaksiKoperasi.setStatus(sts);

		if (transaksiKoperasi.getId() != null) {
			session.update(transaksiKoperasi);
		} else {
			transaksiKoperasi.setDibuatOleh(tbmuser);
			String noAgenda = generateCode(true, transaksiKoperasi);
			kode.setValue(noAgenda);
			transaksiKoperasi.setKode(kode.getValue());
			session.save(transaksiKoperasi);
		}
		TransaksiKoperasiAction.this.populateTransaksi(session, transaksiKoperasi);
		session.flush();

		if (lainMahasiswa != null && lainMahasiswa.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(lainMahasiswa);
				lainMahasiswa.setRef(transaksiKoperasi.getId());

				session.getTransaction().begin();
				session.update(lainMahasiswa);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}
		}

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (transaksiKoperasi.getStatus().equals(DanaTalangan.DISETUJU)) {
					DaftarPengajuanTransfer.simpanTransaksiKoperasi(TransaksiKoperasiAction.this.transaksiKoperasi);
				}

				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						cetak(TransaksiKoperasiAction.this.transaksiKoperasi);
					}
				}, "Proses cetak", false, 2500);

			}
		});

		return true;
	}

	public Boolean checkTransaksi() {

		ProdukKoperasi produkKoperasi = (ProdukKoperasi) TransaksiKoperasiAction.this.produkKoperasi
				.getAttribute("produkKoperasi");

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(TransaksiKoperasi.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("produkKoperasi", produkKoperasi))
				.add(this.transaksiKoperasi.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.transaksiKoperasi.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

	public Criteria initCriteria(boolean order) {

		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(TransaksiKoperasi.class)

				.add((start == null || end == null || start.getValue() == null || end.getValue() == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction("date(this_.tanggal_pembuatan) between date('"
						+ Common.databaseDateFormat.get().format(start.getValue()) + "') and date('"
						+ Common.databaseDateFormat.get().format(end.getValue()) + "')")))

				.add(Restrictions.or(Restrictions.isNull("satuanKerja"),
						satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(
										parent == null ? Restrictions.isNull("satuanKerja")
												: Restrictions.sqlRestriction("false"),
										Restrictions.in("satuanKerja", satuanKerjas))))

				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"))
				.add(serachkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("kode", serachkode.getValue().trim(), MatchMode.ANYWHERE))
				.add(serachnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", serachnama.getValue().trim(), MatchMode.ANYWHERE));

		if (serachjenis != null && !serachjenis.getValue().trim().isEmpty()) {
			criteria.createAlias("produkKoperasi", "produkKoperasi")
					.add(serachjenis.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.or(
									Restrictions.ilike("produkKoperasi.nama", serachjenis.getValue().trim(),
											MatchMode.ANYWHERE),
									Restrictions.ilike("produkKoperasi.kode", serachjenis.getValue().trim(),
											MatchMode.ANYWHERE)));
		}

		if (order)
			criteria.addOrder(Order.desc("id"));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<TransaksiKoperasi> transaksiKoperasi = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(transaksiKoperasi);
		grid.setRowRenderer(new TransaksiKoperasiRenderer());
		grid.setModelCheckMobile(strset);

	}

	public String istilah() throws Exception {
		// TODO Auto-generated method stub
		return "Transaksi Koperasi";
	}

	@Override
	public DataSop ambil() throws Exception {
		// TODO Auto-generated method stub
		return transaksiKoperasi;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Class ambilClass() throws Exception {
		// TODO Auto-generated method stub
		return TransaksiKoperasi.class;
	}

	private String generateCode(boolean tambah, TransaksiKoperasi transaksiKoperasi) {

		if (transaksiKoperasi != null && transaksiKoperasi.getProdukKoperasi() != null
				&& transaksiKoperasi.getProdukKoperasi().getNomorSurat() != null) {
			Long index = transaksiKoperasi.getProdukKoperasi().getNomorSurat().getGunakanIndexUrut()
					? transaksiKoperasi.getProdukKoperasi().getNomorSurat().getNomorIndex()
					: getindex(transaksiKoperasi.getProdukKoperasi().getNomorSurat());
			if (tambah) {
				NomorSurat.tambahIndexNomorSurat(transaksiKoperasi.getProdukKoperasi().getNomorSurat());
			}
			String noAgenda = transaksiKoperasi.getProdukKoperasi().getNomorSurat().format(index, WaktuUtil.getDate());
			return noAgenda;
		} else {

			if (NomorSuratAlurKeuangan.TRANSAKSI_KOPERASI_DATA == null
					|| NomorSuratAlurKeuangan.TRANSAKSI_KOPERASI_DATA.getNomorSurat() == null) {
				return Common.getGeneratedBarCode();
			}

			Long index = NomorSuratAlurKeuangan.TRANSAKSI_KOPERASI_DATA.getNomorSurat().getGunakanIndexUrut()
					? NomorSuratAlurKeuangan.TRANSAKSI_KOPERASI_DATA.getNomorSurat().getNomorIndex()
					: getindex(NomorSuratAlurKeuangan.TRANSAKSI_KOPERASI_DATA.getNomorSurat());
			if (tambah) {
				NomorSurat.tambahIndexNomorSurat(NomorSuratAlurKeuangan.TRANSAKSI_KOPERASI_DATA.getNomorSurat());
			}
			String noAgenda = NomorSuratAlurKeuangan.TRANSAKSI_KOPERASI_DATA.getNomorSurat().format(index,
					WaktuUtil.getDate());
			return noAgenda;
		}
	}

	private Long getindex(NomorSurat nomorSurat) {
		if (nomorSurat == null) {
			return 0L;
		}
		Session session = HibernateUtil.currentSession();
		int tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		int bulan = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1;
		Date sekarang = WaktuUtil.getDate();
		Number indexO = (Number) session.createCriteria(TransaksiKoperasi.class)
				.add(searchstatus.getSelectedItem() == null || searchstatus.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("status", searchstatus.getSelectedItem().getValue()))

				.createAlias("nomorSuratAlurKeuangan", "nomorSuratAlurKeuangan", Criteria.LEFT_JOIN)
				.createAlias("nomorSuratAlurKeuangan.nomorSurat", "nomorSurat", Criteria.LEFT_JOIN)

				.add(nomorSurat.getUrutBerdasarkanNomor()
						? Restrictions.eq("nomorSuratAlurKeuangan.nomorSurat", nomorSurat)

						: (nomorSurat.getUrutBerdasarkanKelompok() && nomorSurat.getKelompokNomorSurat() != null
								? Restrictions.eq("nomorSurat.kelompokNomorSurat", nomorSurat.getKelompokNomorSurat())
								: Restrictions.sqlRestriction("true")))

				.add(nomorSurat.getResetUrutanTiapTahun() ? Restrictions.eq("tahun", tahun)
						: Restrictions.sqlRestriction("true"))

				.add(nomorSurat.getResetUrutanTiapBulan()
						? Restrictions.and(Restrictions.eq("tahun", tahun), Restrictions.eq("bulan", bulan))
						: Restrictions.sqlRestriction("true"))

				.add(nomorSurat.getResetTiap() != null && (Common.dateFormat8.get().format(nomorSurat.getResetTiap())
						.equals(Common.dateFormat8.get().format(sekarang)) || nomorSurat.getResetTiap().before(sekarang))
								? Restrictions.ge("tanggalPembuatan", nomorSurat.getResetTiap())
								: Restrictions.sqlRestriction("true"))

				.setProjection(Projections.rowCount()).uniqueResult();

		Long index = indexO == null ? null : indexO.longValue();
		if (index == null) {
			index = 0L;
		}
		return ++index;
	}

	@Override
	public void setPersetujuan(boolean persetujuan) {
		this.persetujuan = persetujuan;
	}

	private void populateTransaksi(Session session, TransaksiKoperasi transaksiKoperasi) {

		int count = ((Number) session.createCriteria(TransaksiKoperasiDetail.class)
				.add(Restrictions.eq("transaksiKoperasi", transaksiKoperasi))
				.add(Restrictions.isNotNull("pembayaranAnggotaKoperasiDetail")).setProjection(Projections.rowCount())
				.uniqueResult()).intValue();

		if (count == 0) {
			session.createSQLQuery("delete from koperasi.transaksi_koperasi_detail where transaksi_koperasi="
					+ transaksiKoperasi.getId()).executeUpdate();

			if (transaksiKoperasi.getStatus().equalsIgnoreCase(TransaksiKoperasi.DISETUJU)) {

				ProdukKoperasi produkKoperasi = transaksiKoperasi.getProdukKoperasi();
				if (produkKoperasi != null && produkKoperasi.getTipeProdukKoperasi() != null
						&& ConstantValues.SIMPANAN != null
						&& produkKoperasi.getTipeProdukKoperasi().getId().equals(ConstantValues.SIMPANAN.getId())) {

					Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
					calendar.setTime(transaksiKoperasi.getTanggalMulaiDiangsur());

					for (int i = 1; i <= produkKoperasi.getJumlahTransaksiTerbentuk(); i++) {

						Date tanggal = calendar.getTime();
						Double pokok = transaksiKoperasi.getNilai();

						TransaksiKoperasiDetail transaksiKoperasiDetail = new TransaksiKoperasiDetail();
						transaksiKoperasiDetail.setTransaksiKoperasi(transaksiKoperasi);
						transaksiKoperasiDetail.setKe(i);
						transaksiKoperasiDetail.setTanggal(tanggal);
						transaksiKoperasiDetail.setPokok(pokok);
						transaksiKoperasiDetail.setMargin(0.0);
						transaksiKoperasiDetail.setSisa(0.0);
						session.save(transaksiKoperasiDetail);
						session.flush();

						if (transaksiKoperasi.getProdukKoperasi().getDurasi().equals(ProdukKoperasi.HARIAN)) {
							calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
						} else if (transaksiKoperasi.getProdukKoperasi().getDurasi().equals(ProdukKoperasi.MINGGUAN)) {
							calendar.set(Calendar.WEEK_OF_YEAR, calendar.get(Calendar.WEEK_OF_YEAR) + 1);
						} else if (transaksiKoperasi.getProdukKoperasi().getDurasi().equals(ProdukKoperasi.BULANAN)) {
							calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 1);
						} else if (transaksiKoperasi.getProdukKoperasi().getDurasi().equals(ProdukKoperasi.TAHUNAN)) {
							calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) + 1);
						}
					}

				} else {

					Integer jumlahAngsur = transaksiKoperasi.getJumlahAngsur();

					if (jumlahAngsur != null) {

						Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
						calendar.setTime(transaksiKoperasi.getTanggalMulaiDiangsur());
						Calendar s = ais.ui.util.WaktuUtil.getCalendar();
						s.setTime(transaksiKoperasi.getTanggalTerakhirDiangsur());
						s.set(Calendar.DATE, s.get(Calendar.DATE) - 1);

						int i = 1;
						Double sisa = transaksiKoperasi.getNilai();

						// Jadwal angsuran sesuai metode bunga produk. FLAT (default) = perilaku lama;
						// MENURUN/ANUITAS = opsi baru, seluruhnya lewat PerhitunganPinjamanUtil (reuse).
						// Bunga produk = rate per-BULAN (getMargin = nilai×bunga%×jangkaWaktuBulan). Untuk
							// MENURUN/ANUITAS ubah ke rate per-ANGSURAN = bunga × (bulan tenor / jumlah angsuran)
							// agar benar utk durasi non-bulanan; produk bulanan tetap identik (faktor≈1). FLAT
							// mengabaikan nilai ini (memakai marginTotalFlat).
							ProdukKoperasi produkAkad = transaksiKoperasi.getProdukKoperasi();
							double bungaBulanan = produkAkad == null || produkAkad.getBunga() == null ? 0.0
									: produkAkad.getBunga();
							double bulanTenor = produkAkad == null || produkAkad.getJangkaWaktuBulan() == null ? 0.0
									: produkAkad.getJangkaWaktuBulan();
							double bungaPerAngsuran = jumlahAngsur.intValue() > 0
									? bungaBulanan * bulanTenor / jumlahAngsur.intValue()
									: bungaBulanan;
							double[][] jadwalAngsur = ais.action.master.koperasi.helper.PerhitunganPinjamanUtil.hitungJadwal(
								transaksiKoperasi.getNilai(), transaksiKoperasi.getMargin(),
								bungaPerAngsuran, jumlahAngsur.intValue(),
								produkAkad == null ? ProdukKoperasi.METODE_FLAT : produkAkad.getMetodeBunga());

						while (calendar.getTime().before(s.getTime())) {
							Date tanggal = calendar.getTime();
							int idxAngsur = i - 1;
							Double pokok = idxAngsur < jadwalAngsur.length ? jadwalAngsur[idxAngsur][0]
									: transaksiKoperasi.getNilai() / jumlahAngsur.doubleValue();
							Double m = idxAngsur < jadwalAngsur.length ? jadwalAngsur[idxAngsur][1]
									: transaksiKoperasi.getMargin() / jumlahAngsur.doubleValue();
							sisa = sisa - pokok;

							TransaksiKoperasiDetail transaksiKoperasiDetail = new TransaksiKoperasiDetail();
							transaksiKoperasiDetail.setTransaksiKoperasi(transaksiKoperasi);
							transaksiKoperasiDetail.setKe(i);
							transaksiKoperasiDetail.setTanggal(tanggal);
							transaksiKoperasiDetail.setPokok(pokok);
							transaksiKoperasiDetail.setMargin(m);
							transaksiKoperasiDetail.setSisa(sisa);
							session.save(transaksiKoperasiDetail);
							session.flush();

							i++;

							if (transaksiKoperasi.getProdukKoperasi().getDurasi().equals(ProdukKoperasi.HARIAN)) {
								calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
							} else if (transaksiKoperasi.getProdukKoperasi().getDurasi()
									.equals(ProdukKoperasi.MINGGUAN)) {
								calendar.set(Calendar.WEEK_OF_YEAR, calendar.get(Calendar.WEEK_OF_YEAR) + 1);
							} else if (transaksiKoperasi.getProdukKoperasi().getDurasi()
									.equals(ProdukKoperasi.BULANAN)) {
								calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 1);
							} else if (transaksiKoperasi.getProdukKoperasi().getDurasi()
									.equals(ProdukKoperasi.TAHUNAN)) {
								calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) + 1);
							}
						}

					}
				}
			}
		}
	}
}
