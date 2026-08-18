package ais.action.master.sapto;

import java.util.List;

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
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyGrid;
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
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.PerguruanTinggi;
import ais.database.model.file.LampiranLain;
import ais.database.model.sapto.PrasaranaPerguruanTinggiSapto;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class PrasaranaPerguruanTinggiSaptoAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Combobox searchPerguruanTinggi;

	private Textbox nama;
	private Combobox jenisKepemilikan;
	private Combobox kondisi;
	private Textbox luas;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private PrasaranaPerguruanTinggiSapto prasaranaPerguruanTinggiSapto;
	private MyToolbarbuttonConfig add;

	protected LampiranLain lainMahasiswa;
	private Combobox perguruanTinggi;
	private Intbox jumlah;
	private Intbox tahun;
	private MyCheckboxConfig utama;

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

		Common.initComboPerguruanTinggi(searchPerguruanTinggi, null);
		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "id", "perguruanTinggi", "nama", "jumlah", "jenisKepemilikan", "kondisi",
				"luas", "tahun", "keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, PrasaranaPerguruanTinggiSapto.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	class PrasaranaPerguruanTinggiSaptoRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final PrasaranaPerguruanTinggiSapto prasaranaPerguruanTinggiSapto = (PrasaranaPerguruanTinggiSapto) arg1;

			Vbox a;
			(a = RevisiHelper.createNewRevisi(PrasaranaPerguruanTinggiSapto.class, prasaranaPerguruanTinggiSapto,
					prasaranaPerguruanTinggiSapto.getNama())).setParent(arg0);

			Vbox myvbox = new Vbox();
			myvbox.setParent(a);

			Hbox hbox = new Hbox();
			hbox.setParent(myvbox);
			LampiranLain.createDownloadUploadFileLain(hbox, prasaranaPerguruanTinggiSapto.getId(),
					PrasaranaPerguruanTinggiSapto.class.getName(), "Bukti Prasarana", false, null, null, false, false,
					false, false);
			new Label(prasaranaPerguruanTinggiSapto.getUtama() ? "Ya" : "Tidak").setParent(arg0);
			new Label(Common.numberFormat.get().format(prasaranaPerguruanTinggiSapto.getJumlah())).setParent(arg0);
			new Label(prasaranaPerguruanTinggiSapto.getJenisKepemilikan()).setParent(arg0);
			new Label(prasaranaPerguruanTinggiSapto.getKondisi()).setParent(arg0);
			new Label(prasaranaPerguruanTinggiSapto.getLuas()).setParent(arg0);
			new Label(prasaranaPerguruanTinggiSapto.getTahun().toString()).setParent(arg0);
			new Label(prasaranaPerguruanTinggiSapto.getKeterangan()).setParent(arg0);

			Common.copyEditDeleteButtons(edit, delete, prasaranaPerguruanTinggiSapto,
					PrasaranaPerguruanTinggiSaptoAction.this).setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new PrasaranaPerguruanTinggiSapto());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		prasaranaPerguruanTinggiSapto = (PrasaranaPerguruanTinggiSapto) obj;
		init(prasaranaPerguruanTinggiSapto);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(PrasaranaPerguruanTinggiSapto prasaranaPerguruanTinggiSapto) {
		this.prasaranaPerguruanTinggiSapto = prasaranaPerguruanTinggiSapto;
		addWindow.setTitle(prasaranaPerguruanTinggiSapto.getId() == null ? "Tambah Jenis Prasarana" : "Ubah Jenis Prasarana");
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

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Perguruan Tinggi *"));
		row.appendChild(perguruanTinggi = new Combobox());
		Common.initComboPerguruanTinggi(perguruanTinggi, prasaranaPerguruanTinggiSapto.getPerguruanTinggi());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Prasarana *"));
		row.appendChild(nama = new Textbox(prasaranaPerguruanTinggiSapto.getNama()));
		nama.setWidth("90%");
		nama.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(utama = new MyCheckboxConfig("Merupakan prasarana utama *"));
		utama.setChecked(prasaranaPerguruanTinggiSapto.getUtama());
		Common.initKeterangan(rows,
				"- Contoh prasarana utama (misalnya: kantor, ruang kelas, ruang laboratorium, studio, ruang perpustakaan, kebun percobaan, ruang dosen) yang digunakan institusi dalam penyelenggaraan program / kegiatan institusi");
		Common.initKeterangan(rows,
				"- Contoh prasarana lain yang mendukung terwujudnya visi (misalnya: tempat pembinaan minat dan bakat, kesejahteraan, ruang himpunan mahasiswa, asrama mahasiswa)");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jumlah *"));
		row.appendChild(jumlah = new Intbox(prasaranaPerguruanTinggiSapto.getJumlah()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Kepemilikan Prasarana *"));
		row.appendChild(jenisKepemilikan = new Combobox());
		MyComboitemConfig comboitemConfig = new MyComboitemConfig(PrasaranaPerguruanTinggiSapto.KEPEMILIKAN_SENDIRI);
		comboitemConfig.setValue(PrasaranaPerguruanTinggiSapto.KEPEMILIKAN_SENDIRI);
		jenisKepemilikan.appendChild(comboitemConfig);

		comboitemConfig = new MyComboitemConfig(PrasaranaPerguruanTinggiSapto.KEPEMILIKAN_SEWA);
		comboitemConfig.setValue(PrasaranaPerguruanTinggiSapto.KEPEMILIKAN_SEWA);
		jenisKepemilikan.appendChild(comboitemConfig);

		comboitemConfig = new MyComboitemConfig(PrasaranaPerguruanTinggiSapto.KEPEMILIKAN_PINJAMAN);
		comboitemConfig.setValue(PrasaranaPerguruanTinggiSapto.KEPEMILIKAN_PINJAMAN);
		jenisKepemilikan.appendChild(comboitemConfig);

		comboitemConfig = new MyComboitemConfig(PrasaranaPerguruanTinggiSapto.KEPEMILIKAN_KERJASAMA);
		comboitemConfig.setValue(PrasaranaPerguruanTinggiSapto.KEPEMILIKAN_KERJASAMA);
		jenisKepemilikan.appendChild(comboitemConfig);

		jenisKepemilikan.setWidth("90%");
		jenisKepemilikan.setReadonly(true);
		Common.selectComboItem(jenisKepemilikan, prasaranaPerguruanTinggiSapto.getJenisKepemilikan());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kondisi *"));
		row.appendChild(kondisi = new Combobox());

		comboitemConfig = new MyComboitemConfig(PrasaranaPerguruanTinggiSapto.KONDISI_TERAWAT);
		comboitemConfig.setValue(PrasaranaPerguruanTinggiSapto.KONDISI_TERAWAT);
		kondisi.appendChild(comboitemConfig);

		comboitemConfig = new MyComboitemConfig(PrasaranaPerguruanTinggiSapto.KONDISI_TIDAK_TERAWAT);
		comboitemConfig.setValue(PrasaranaPerguruanTinggiSapto.KONDISI_TIDAK_TERAWAT);
		kondisi.appendChild(comboitemConfig);

		Common.selectComboItem(kondisi, prasaranaPerguruanTinggiSapto.getKondisi());

		kondisi.setWidth("90%");
		kondisi.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Luas *"));
		row.appendChild(luas = new Textbox(prasaranaPerguruanTinggiSapto.getLuas()));
		luas.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun *"));
		row.appendChild(tahun = new Intbox(prasaranaPerguruanTinggiSapto.getTahun()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(prasaranaPerguruanTinggiSapto.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		lainMahasiswa = null;

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Lampiran / Bukti Prasarana"));
		Hbox hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, prasaranaPerguruanTinggiSapto.getId(),
				PrasaranaPerguruanTinggiSapto.class.getName(), "Bukti Prasarana", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lainMahasiswa = (LampiranLain) arg0.getData();
					}
				});
		hbox.setParent(row);

		Common.initKeterangan(rows,
				"Jika file \"Lampiran / Bukti Prasarana\" lebih dari satu file, zip dulu semua file tersebut");

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
		if (perguruanTinggi.getSelectedItem() == null) {
			MyMessageboxConfig.show("Perguruan Tinggi harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (nama.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show("Jenis Prasarana harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (jumlah.getValue() == null || jumlah.getValue() < 1) {
			MyMessageboxConfig.show("Jumlah Prasarana harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (jenisKepemilikan.getSelectedItem() == null) {
			MyMessageboxConfig.show("Status Kepemilikan Prasarana harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (kondisi.getSelectedItem() == null) {
			MyMessageboxConfig.show("Kondisi Prasarana harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (luas.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show("Luas Prasarana harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (tahun.getValue() == null || tahun.getValue() < 1) {
			MyMessageboxConfig.show("Tahun Prasarana harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (prasaranaPerguruanTinggiSapto.getId() != null) {
			prasaranaPerguruanTinggiSapto = (PrasaranaPerguruanTinggiSapto) session
					.load(PrasaranaPerguruanTinggiSapto.class, prasaranaPerguruanTinggiSapto.getId());

		}

		prasaranaPerguruanTinggiSapto
				.setPerguruanTinggi((PerguruanTinggi) perguruanTinggi.getSelectedItem().getValue());
		prasaranaPerguruanTinggiSapto.setNama(nama.getValue().trim());
		prasaranaPerguruanTinggiSapto.setUtama(utama.isChecked());
		prasaranaPerguruanTinggiSapto.setJenisKepemilikan((String) jenisKepemilikan.getSelectedItem().getValue());
		prasaranaPerguruanTinggiSapto.setKondisi((String) kondisi.getSelectedItem().getValue());
		prasaranaPerguruanTinggiSapto.setJumlah(jumlah.getValue());
		prasaranaPerguruanTinggiSapto.setTahun(tahun.getValue());
		prasaranaPerguruanTinggiSapto.setLuas(luas.getValue());
		prasaranaPerguruanTinggiSapto.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, prasaranaPerguruanTinggiSapto);

		if (lainMahasiswa != null && lainMahasiswa.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(lainMahasiswa);
				lainMahasiswa.setRef(prasaranaPerguruanTinggiSapto.getId());

				session.getTransaction().begin();
				session.update(lainMahasiswa);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e); 
			}

		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PrasaranaPerguruanTinggiSapto.class);

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchPerguruanTinggi.getSelectedItem() == null
				|| searchPerguruanTinggi.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("perguruanTinggi", searchPerguruanTinggi.getSelectedItem().getValue()))

				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<PrasaranaPerguruanTinggiSapto> prasaranaPerguruanTinggiSapto = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(prasaranaPerguruanTinggiSapto);
		grid.setRowRenderer(new PrasaranaPerguruanTinggiSaptoRenderer());
		grid.setModelCheckMobile(strset);

	}

}
