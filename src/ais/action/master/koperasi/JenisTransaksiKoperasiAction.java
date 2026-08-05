package ais.action.master.koperasi;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
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

import ais.action.master.akunting.helper.AmbilDataAkunBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.akunting.Akun;
import ais.database.model.file.LampiranLain;
import ais.database.model.koperasi.JenisTransaksiKoperasi;
import ais.database.model.koperasi.KelompokParameterTambahanProdukKoperasi;
import ais.database.model.koperasi.TipeProdukKoperasi;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class JenisTransaksiKoperasiAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Checkbox searchaktif;
	private Textbox searchnama;

	private Textbox nama;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private JenisTransaksiKoperasi jenisTransaksiKoperasi;
	private MyToolbarbuttonConfig add;
	private Set<KelompokParameterTambahanProdukKoperasi> selectedKelompokParameterTambahanProdukKoperasi;
	protected LampiranLain lampiran;

	private Tabpanel tabManajemenParameter;
	private Combobox tipeProdukKoperasi;
	private AmbilDataAkunBanbox akun;
	private MyCheckboxConfig menghitungTotal;
	private MyCheckboxConfig bolehJns;
	private MyCheckboxConfig bolehQty;
	private MyCheckboxConfig bolehNilai;

	public void onManajemenParameter(Event event) {
		if (tabManajemenParameter.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tabManajemenParameter);
			MyInclude iframe = new MyInclude("/pages/master/koperasi/parameter_tambahan_produk_koperasi.zul");
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

		String[] contents = new String[] { "id", "nama", "akun", "keterangan", "menghitungTotal", "bolehJns",
				"bolehQty", "bolehNilai", "aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, JenisTransaksiKoperasi.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	class JenisTransaksiKoperasiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final JenisTransaksiKoperasi jenisTransaksiKoperasi = (JenisTransaksiKoperasi) arg1;

			RevisiHelper.createNewRevisi(JenisTransaksiKoperasi.class, jenisTransaksiKoperasi,
					jenisTransaksiKoperasi.getNama()).setParent(arg0);

			new Label(jenisTransaksiKoperasi.getTipeProdukKoperasi() == null ? ""
					: jenisTransaksiKoperasi.getTipeProdukKoperasi().getNama()).setParent(arg0);

			new Label(jenisTransaksiKoperasi.getAkun() == null ? "" : jenisTransaksiKoperasi.getAkun().getNama())
					.setParent(arg0);

			new Label(jenisTransaksiKoperasi.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(jenisTransaksiKoperasi.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenisTransaksiKoperasi.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(jenisTransaksiKoperasi);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, jenisTransaksiKoperasi, JenisTransaksiKoperasiAction.this)
					.setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new JenisTransaksiKoperasi());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		jenisTransaksiKoperasi = (JenisTransaksiKoperasi) obj;
		init(jenisTransaksiKoperasi);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(JenisTransaksiKoperasi jenisTransaksiKoperasi) {
		this.jenisTransaksiKoperasi = jenisTransaksiKoperasi;
		addWindow.setTitle(jenisTransaksiKoperasi.getId() == null ? "Tambah Jenis Transaksi Koperasi" : "Ubah Jenis Transaksi Koperasi");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Transaksi Koperasi *"));
		row.appendChild(nama = new Textbox(jenisTransaksiKoperasi.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tipe Produk Koperasi *"));
		row.appendChild(tipeProdukKoperasi = new Combobox());
		Common.insertCombo(tipeProdukKoperasi, "nama", TipeProdukKoperasi.class, Restrictions.eq("aktif", true));
		Common.selectComboItem(tipeProdukKoperasi, jenisTransaksiKoperasi.getTipeProdukKoperasi());
		tipeProdukKoperasi.setWidth("90%");
		tipeProdukKoperasi.setReadonly(true);

		row = new MyFormRow();
		row.setStyle("border:0px;background: transakun;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Akun *")));
		row.appendChild(akun = new AmbilDataAkunBanbox());
		akun.setValue(jenisTransaksiKoperasi.getAkun() == null ? "" : jenisTransaksiKoperasi.getAkun().getNama());
		akun.setAttribute("akun", jenisTransaksiKoperasi.getAkun());
		akun.setWidth("90%");

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(menghitungTotal = new MyCheckboxConfig("Terhitung ke total"));
		menghitungTotal.setChecked(jenisTransaksiKoperasi.getMenghitungTotal());

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(bolehJns = new MyCheckboxConfig("Boleh ubah jenis transaksi"));
		bolehJns.setChecked(jenisTransaksiKoperasi.getBolehJns());

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(bolehQty = new MyCheckboxConfig("Boleh ubah jumlah (qty) transaksi"));
		bolehQty.setChecked(jenisTransaksiKoperasi.getBolehQty());

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(bolehNilai = new MyCheckboxConfig("Boleh ubah nilai transaksi"));
		bolehNilai.setChecked(jenisTransaksiKoperasi.getBolehNilai());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(jenisTransaksiKoperasi.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		lampiran = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("File Laporan (jrxml atau jasper)"));
		Hbox hbox = new Hbox();
		hbox.setParent(row);
		LampiranLain.createDownloadUploadFileLain(hbox, jenisTransaksiKoperasi.getId(),
				LampiranLain.FILE_JRXML_LAYOUT_TIPE_PRODUK_KOPERASI, "File Laporan jrxml", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lampiran = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, true);

		initKelompokParameterTambahanProdukKoperasi(rows);

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

	@SuppressWarnings("deprecation")
	private void initKelompokParameterTambahanProdukKoperasi(Rows rows) {
		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		final MyGrid subGrid = new MyGrid();
		row.appendChild(subGrid);

		Columns subColumns = new Columns();
		subColumns.setParent(subGrid);
		Column c = new Column("Parameter Produk Koperasi");
		subColumns.appendChild(c);

		Rows subRows = new Rows();
		subRows.setParent(subGrid);

		MyFormRow subRow = new MyFormRow();
		subRow.setStyle("border:0px;background: transparent;");
		subRow.setParent(subRows);
		subRow.setValign("top");

		@SuppressWarnings("unchecked")
		Map<Long, KelompokParameterTambahanProdukKoperasi> kelompokParameterTambahanProdukKoperasis = ConstantValues
				.ambilBerdasarClass(KelompokParameterTambahanProdukKoperasi.class);

		if (jenisTransaksiKoperasi != null && jenisTransaksiKoperasi.getId() != null) {
			HibernateUtil.currentSession().refresh(jenisTransaksiKoperasi);
		}

		selectedKelompokParameterTambahanProdukKoperasi = this.jenisTransaksiKoperasi
				.getKelompokParameterTambahanProdukKoperasis();
		Set<Long> ids = new HashSet<Long>();
		for (KelompokParameterTambahanProdukKoperasi v : selectedKelompokParameterTambahanProdukKoperasi) {
			ids.add(v.getId());
		}

		System.out.println("ids ->" + ids);

		Vbox vboxSkala = new Vbox();
		vboxSkala.setPack("top");
		vboxSkala.setParent(subRow);
		for (final KelompokParameterTambahanProdukKoperasi kelompokParameterTambahanProdukKoperasi : kelompokParameterTambahanProdukKoperasis
				.values()) {
			final Checkbox checkbox = new Checkbox(kelompokParameterTambahanProdukKoperasi.getNama());
			checkbox.setParent(vboxSkala);
			checkbox.setChecked(ids.contains(kelompokParameterTambahanProdukKoperasi.getId()));
			checkbox.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						selectedKelompokParameterTambahanProdukKoperasi.add(kelompokParameterTambahanProdukKoperasi);
					} else {

						for (KelompokParameterTambahanProdukKoperasi a : selectedKelompokParameterTambahanProdukKoperasi) {
							if (a.getId().equals(kelompokParameterTambahanProdukKoperasi.getId())) {
								selectedKelompokParameterTambahanProdukKoperasi.remove(a);
								break;
							}
						}

					}

					System.out.println("selectedKelompokParameterTambahanProdukKoperasi => "
							+ selectedKelompokParameterTambahanProdukKoperasi);
				}
			});
		}
	}

	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, nama jenis transaksi koperasi belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Nama Jenis Transaksi; (2) gunakan nama yang deskriptif dan belum terpakai; (3) ulangi penyimpanan.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (tipeProdukKoperasi.getSelectedItem() == null || tipeProdukKoperasi.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, tipe produk belum dipilih. Langkah yang dapat dilakukan: (1) pilih tipe produk dari daftar yang tersedia; (2) pastikan tipe produk sudah terdaftar di sistem; (3) ulangi penyimpanan.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (jenisTransaksiKoperasi.getId() != null) {
			jenisTransaksiKoperasi = (JenisTransaksiKoperasi) session.load(JenisTransaksiKoperasi.class,
					jenisTransaksiKoperasi.getId());

		}

		jenisTransaksiKoperasi.setNama(nama.getValue());
		jenisTransaksiKoperasi.setKeterangan(keterangan.getValue());
		jenisTransaksiKoperasi
				.setKelompokParameterTambahanProdukKoperasis(selectedKelompokParameterTambahanProdukKoperasi);
		jenisTransaksiKoperasi.setAkun((Akun) akun.getAttribute("akun"));
		jenisTransaksiKoperasi
				.setTipeProdukKoperasi((TipeProdukKoperasi) tipeProdukKoperasi.getSelectedItem().getValue());
		jenisTransaksiKoperasi.setMenghitungTotal(menghitungTotal.isChecked());

		jenisTransaksiKoperasi.setBolehJns(bolehJns.isChecked());
		jenisTransaksiKoperasi.setBolehQty(bolehQty.isChecked());
		jenisTransaksiKoperasi.setBolehNilai(bolehNilai.isChecked());

		Common.refreshSaveOrUpdate(session, jenisTransaksiKoperasi);

		try {
			session = StreamingHibernateUtil.getInstance().currentSession();

			if (lampiran != null && lampiran.getId() != null) {
				session.refresh(lampiran);
				lampiran.setRef(jenisTransaksiKoperasi.getId());

				session.getTransaction().begin();
				session.update(lampiran);
				session.getTransaction().commit();
			}

			StreamingHibernateUtil.getInstance().closeSession();
		} catch (Exception e) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			Common.tampilErrorJikaAdmin(e);
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(JenisTransaksiKoperasi.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))

		;
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<JenisTransaksiKoperasi> jenisTransaksiKoperasi = ConstantValues.simpleList(
				initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
						.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())),
				JenisTransaksiKoperasi.class);
		ListModel strset = new SimpleListModel(jenisTransaksiKoperasi);
		grid.setRowRenderer(new JenisTransaksiKoperasiRenderer());
		grid.setModelCheckMobile(strset);

	}

}
