package ais.action.master.employ;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

import ais.common.UploadReportHelper;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.employ.util.GapokImporter;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.employ.GajiPokokDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.employ.GajiPokok;
import ais.database.model.employ.Golongan;
import ais.database.model.employ.Peraturan;
import ais.database.model.file.LampiranLain;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class GajiPokokAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Combobox searchgolongan;
	private Combobox searchperaturan;
	private Intbox searchmasakerja;

	private Combobox golongan;
	private Combobox masaKerja;
	private MyDoublebox gaji;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private GajiPokok gajiPokok;
	private MyToolbarbuttonConfig add;

	private MyDatebox tanggalEfektif;
	private MyDoublebox lain;

	private Tabpanel manajemenKodeTunjangan;

	public void onKodeTunjangan(Event event) {
		if (manajemenKodeTunjangan.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(manajemenKodeTunjangan);
			MyInclude iframe = new MyInclude("/pages/master/payroll/kode_tunjangan.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel manajemenInsentif;

	public void onInsentif(Event event) {
		if (manajemenInsentif.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(manajemenInsentif);
			MyInclude iframe = new MyInclude("/pages/master/employ/insentif.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel manajemenMakan;

	public void onMakan(Event event) {
		if (manajemenMakan.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(manajemenMakan);
			MyInclude iframe = new MyInclude("/pages/master/employ/makan.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel manajemenTransport;

	public void onTransport(Event event) {
		if (manajemenTransport.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(manajemenTransport);
			MyInclude iframe = new MyInclude("/pages/master/employ/transport.zul");
			iframe.setParent(window);
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
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		Common.insertComboDanSemua(searchgolongan, "nama", Golongan.class, Restrictions.eq("aktif", true));
		Common.insertComboDanSemua(searchperaturan, "nama", Peraturan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		masaKerja = new Combobox();
		MyComboitemConfig comboitem;
		for (int i = 0; i < 63; i++) {
			comboitem = new MyComboitemConfig(i + "");
			comboitem.setValue(i);
			comboitem.setParent(masaKerja);
		}

		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "id", "keterangan", "golongan", "peraturan", "masaKerja", "gaji", "lain",
				"tanggalEfektif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(GajiPokok.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, GajiPokok.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	class GajiPokokRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final GajiPokok gajiPokok = (GajiPokok) arg1;

			new Label(gajiPokok.getGolongan() == null ? "" : gajiPokok.getGolongan().getNama()).setParent(arg0);

			Vbox a;
			(a = RevisiHelper.createNewRevisi(GajiPokok.class, gajiPokok,
					gajiPokok.getPeraturan() == null ? "-" : gajiPokok.getPeraturan().getNama())).setParent(arg0);
			if (gajiPokok.getPeraturan() != null) {
				Vbox myvbox = new Vbox();
				myvbox.setParent(a);

				Hbox hbox = new Hbox();
				hbox.setParent(myvbox);
				LampiranLain.createDownloadUploadFileLain(hbox, gajiPokok.getPeraturan().getId(),
						Peraturan.class.getName(), "Peraturan Dokumen", false, null, null, false, false, false, false);
			}

			new Label(gajiPokok.getMasaKerja() == null ? "0"
					: Common.numberFormat.get().format(gajiPokok.getMasaKerja()) + " th").setParent(arg0);

			new Label(gajiPokok.getGaji() == null ? "0" : Common.numberFormat.get().format(gajiPokok.getGaji()))
					.setParent(arg0);

			new Label(gajiPokok.getLain() == null ? "0" : Common.numberFormat.get().format(gajiPokok.getLain()))
					.setParent(arg0);

			new Label(gajiPokok.getTanggalEfektif() == null ? "0"
					: Common.dateFormat1.get().format(gajiPokok.getTanggalEfektif())).setParent(arg0);
			new Label(gajiPokok.getKeterangan()).setParent(arg0);

			Common.copyEditDeleteButtons(edit, delete, gajiPokok, GajiPokokAction.this).setParent(arg0);

		}

	}

	public void onDownload(Event event) throws Exception {
		Filedownload.save(new File(application.getRealPath("/img/Contoh_Daftar_Gapok_PNS.xlsx")),
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
	}

	public void onUploadGapok(ForwardEvent event) throws Exception {
		UploadEvent uploadEvent = (UploadEvent) event.getOrigin();
		Media media = uploadEvent.getMedia();if(!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))return;
		if (media.getName().toLowerCase().endsWith("xlsx")) {
			InputStream inputStream = media.getStreamData();
			final File file = new File(Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
			file.getParentFile().mkdirs();
			FileOutputStream fileOutputStream = new FileOutputStream(file);
			int c;
			while ((c = inputStream.read()) != -1) {
				fileOutputStream.write(c);
			}
			fileOutputStream.close();
			inputStream.close();
			final UploadReportHelper report = new UploadReportHelper("Upload Gaji Pokok");
			try {
				GapokImporter.doImport(file);
				report.sukses(1, "gaji-pokok", "Import berhasil");
			} catch (RuntimeException rex) {
				report.gagal(1, "gaji-pokok", rex, "Periksa format file Excel gaji pokok");
			}
			try { Filedownload.save(report.simpanLaporan(), "text/plain"); } catch (Exception ignored) {}
			MyMessageboxConfig.show("Upload gaji pokok berhasil dilakukan. " + report.getRingkasan(), "Informasi", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							onSearchDefault(arg0);
						}
					});

		} else {
			MyMessageboxConfig.show(
					"File yang anda upload harus ber-format Excel Open XML Spreadsheet (xlsx). Jika masih menggunakan format lain, buka file excel tersebut, kemudian Save As Excel Open XML Spreadsheet (xlsx). "
							+ media,
					"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
		}
	}

	public void onAdd(Event event) throws Exception {
		init(new GajiPokok());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(GajiPokok gajiPokok) throws Exception {
		this.gajiPokok = gajiPokok;
		addWindow.setTitle(gajiPokok.getId() == null ? "Tambah Gaji Pokok" : "Ubah Gaji Pokok");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Golongan *"));
		row.appendChild(golongan = new Combobox());
		Common.insertCombo(golongan, "nama", Golongan.class, Restrictions.eq("aktif", true));
		Common.selectComboItem(golongan, gajiPokok.getGolongan());
		golongan.setWidth("90%");
		golongan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Masa Kerja (MK) *"));
		row.appendChild(masaKerja);
		Common.selectComboItem(masaKerja, gajiPokok.getMasaKerja() == null ? null : gajiPokok.getMasaKerja());
		masaKerja.setWidth("90%");
		masaKerja.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Gaji Pokok (GAPOK) *"));
		row.appendChild(gaji = new MyDoublebox(gajiPokok.getGaji()));
		gaji.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Lain-lain (LAIN_LAIN) *"));
		row.appendChild(lain = new MyDoublebox(gajiPokok.getLain()));
		lain.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Efektif *"));
		row.appendChild(tanggalEfektif = new MyDatebox(gajiPokok.getTanggalEfektif()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(gajiPokok.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

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
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
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

	}

	public boolean onSave(Event event) throws Exception {

		if (golongan.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, Golongan belum dipilih. Langkah yang dapat dilakukan: (1) pilih Golongan dari dropdown pada form; (2) pastikan data golongan sudah tersedia di master; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (masaKerja.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, Masa Kerja belum dipilih. Langkah yang dapat dilakukan: (1) pilih Masa Kerja dari dropdown pada form; (2) pastikan data masa kerja sudah tersedia di master; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (gaji.getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Gaji belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Gaji pada form; (2) pastikan nilai gaji berupa angka yang valid; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (lain.getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, kolom Lain-lain belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Lain-lain pada form; (2) pastikan nilai berupa angka yang valid atau nol; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		GajiPokokDao gajiPokokDao = DaoFactory.getInstance().getGajiPokokDao();
		if (gajiPokok.getId() != null) {
			gajiPokok = gajiPokokDao.load(gajiPokok.getId());

		}

		gajiPokok.setGolongan(
				(Golongan) (golongan.getSelectedItem() == null ? null : golongan.getSelectedItem().getValue()));
		// gajiPokok.setMasaKerja(masaKerja.getValue());

		gajiPokok.setGaji(gaji.getValue());
		gajiPokok.setMasaKerja(
				(Integer) (masaKerja.getSelectedItem() == null ? null : masaKerja.getSelectedItem().getValue()));
		gajiPokok.setKeterangan(keterangan.getValue());

		gajiPokok.setTanggalEfektif(tanggalEfektif.getValue());
		gajiPokok.setLain(lain.getValue());

		if (gajiPokok.getId() != null) {
			gajiPokokDao.update(gajiPokok);
		} else {
			gajiPokokDao.save(gajiPokok);
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(GajiPokok.class).createAlias("golongan", "golongan")
				.add(Restrictions.eq("golongan.aktif", true));
		if (order)
			criteria.addOrder(Order.asc("golongan.id")).addOrder(Order.asc("masaKerja"));
		criteria.add(searchgolongan.getSelectedItem() == null || searchgolongan.getSelectedItem().getValue() == null
				? Restrictions.sqlRestriction("1=1")
				: Restrictions.eq("golongan", searchgolongan.getSelectedItem().getValue()))
				.add(searchperaturan.getSelectedItem() == null || searchperaturan.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("peraturan", searchperaturan.getSelectedItem().getValue()))
				.add(searchmasakerja.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("masaKerja", searchmasakerja.getValue()))

		;
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<GajiPokok> gajiPokok = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(gajiPokok);
		grid.setRowRenderer(new GajiPokokRenderer());
		grid.setModelCheckMobile(strset);

	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		gajiPokok = (GajiPokok) obj;
		init(gajiPokok);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

}
