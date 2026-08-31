package ais.action.master.helper;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import ais.ui.util.MyColumnConfig;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.PesanFormalHelper;
import ais.common.listener.DataLoader;
import ais.database.dao.DaoFactory;
import ais.database.dao.TemplateSuratParameterDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.TemplateSurat;
import ais.database.model.TemplateSuratParameter;
import ais.ui.util.MyPanel;

/**
 * Helper ZK CRUD sederhana untuk mengelola daftar {@link TemplateSuratParameter} (parameter/variabel
 * yang dapat disisipkan ke dalam isi satu {@link TemplateSurat}, mis. placeholder bertipe
 * {@link String}/{@link Integer}/{@link Long} yang nantinya diganti dengan nilai sesungguhnya saat
 * surat dicetak). Menampilkan grid berpaginasi dengan tombol tambah/ubah/hapus per baris, hak akses
 * masing-masing dicek terhadap {@link CommonPrivilages} (CREATE/UPDATE/DELETE) dan disembunyikan
 * total dari pengguna mahasiswa.
 */
public class TemplateSuratParameterHelper implements DataLoader {

	private MyGrid grid;
	private TemplateSurat templateSurat;
	private boolean delete = false;
	private boolean add = false;
	private TemplateSuratParameter templateSuratParameter;
	private MyWindow addWindow;
	private Textbox nama;
	private Textbox keterangan;
	private Combobox tipe;
	private boolean edit = false;

	/** Menyiapkan hak akses (CREATE/UPDATE/DELETE) pengguna saat ini dan kombo pilihan tipe parameter (String/Integer/Long). */
	public TemplateSuratParameterHelper() {
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);

		tipe = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig(String.class.getName());
		comboitem.setValue(String.class.getName());
		tipe.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Integer.class.getName());
		comboitem.setValue(Integer.class.getName());
		tipe.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Long.class.getName());
		comboitem.setValue(Long.class.getName());
		tipe.appendChild(comboitem);

	}

	class DetailTemplateSuratRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final TemplateSuratParameter templateSuratParameter = (TemplateSuratParameter) data;

			RevisiHelper.createNewRevisi(TemplateSuratParameter.class, templateSuratParameter,
					templateSuratParameter.getNama()).setParent(row);
			new Label(templateSuratParameter.getTipe()).setParent(row);
			new Label(templateSuratParameter.getKeterangan()).setParent(row);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setVisible(Common.getCurrentUser().getMahasiswa() == null && edit);
			button.setTooltiptext("Edit Data");
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					init(templateSuratParameter);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setVisible(Common.getCurrentUser().getMahasiswa() == null && delete);
			button.setTooltiptext("Hapus Data");
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
											TemplateSuratParameterDao templateSuratDao = DaoFactory.getInstance()
													.getTemplateSuratParameterDao();

											templateSuratDao.delete((templateSuratParameter));

											loadData(null);

										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											PesanFormalHelper.tampilkanGagalException(
													"menghapus data parameter template surat ini",
													e,
													new String[] {
															"Periksa apakah parameter ini masih berelasi dengan data lain (misalnya sedang digunakan oleh template surat) sehingga tidak dapat dihapus.",
															"Hapus atau lepaskan terlebih dahulu data terkait yang masih berelasi, lalu ulangi proses penghapusan.",
															"Jika data tetap tidak dapat dihapus, konfirmasikan kebutuhan penghapusan ini kepada Administrator." });
										}

									}

								}
							});

				}

			});
			button.setParent(toolbar);
			toolbar.setParent(row);

		}

	}

	/**
	 * Implementasi {@link DataLoader}: memuat ulang seluruh {@link TemplateSuratParameter} milik
	 * {@link #templateSurat} ke {@link #grid}.
	 *
	 * @param value tidak digunakan
	 */
	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Session session = HibernateUtil.currentSession();
		List<TemplateSuratParameter> templateSuratParameter = session.createCriteria(TemplateSuratParameter.class)
				.addOrder(Order.asc("id")).add(Restrictions.eq("templateSurat", templateSurat)).list();

		ListModel strset = new SimpleListModel(templateSuratParameter);
		grid.setRowRenderer(new DetailTemplateSuratRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Menampilkan panel daftar parameter milik {@code templateSurat} dengan tombol "Tambah Parameter"
	 * (tampil hanya bagi non-mahasiswa yang punya hak CREATE).
	 *
	 * @param templateSurat template surat yang parameternya akan dikelola
	 * @param component     komponen ZK induk tempat panel dirender (dibersihkan lebih dulu)
	 */
	public void display(final TemplateSurat templateSurat, final Component component) {
		this.templateSurat = templateSurat;
		Common.clear(component);

		MyPanel panel = new MyPanel();
		panel.setParent(component);
		panel.setWidth("100%");
		panel.setHeight("450px");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Toolbar toolbar = new Toolbar();
		toolbar.setVisible(Common.getCurrentUser().getMahasiswa() == null && add);
		// toolbar.setHeight("25px");
		toolbar.setParent(panel);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah Parameter", "/img/new.gif");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onAdd(event);
			}

		});
		button.setParent(toolbar);

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(panelchildren);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);grid.getPagingChild().setMold("os");
		grid.setParent(center);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tipe");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("10%");

		loadData(null);

	}

	/** Membuka jendela form untuk menambah {@link TemplateSuratParameter} baru. */
	public void onAdd(Event event) throws Exception {
		init(new TemplateSuratParameter());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(TemplateSuratParameter templateSuratParameter) {
		addWindow = new MyWindow();
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(addWindow);
		addWindow.setWidth("500px");
		addWindow.setHeight("400px");
		this.templateSuratParameter = templateSuratParameter;
		addWindow.setTitle(templateSuratParameter.getId() == null ? "Tambah Format Template Surat" : "Ubah Format Template Surat");
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
		column.setWidth("35%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Parameter"));
		row.appendChild(
				nama = new Textbox(templateSuratParameter.getNama() == null ? "" : templateSuratParameter.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tipe"));
		row.appendChild(tipe);
		Common.selectComboItem(tipe, templateSuratParameter.getTipe());
		tipe.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(
				templateSuratParameter.getKeterangan() == null ? "" : templateSuratParameter.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);
		keterangan.setRows(4);

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
				addWindow.detach();
			}
		});
		cancel.setParent(toolbar);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					loadData(null);
					addWindow.detach();
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	/**
	 * Memvalidasi (Nama dan Tipe wajib diisi) dan menyimpan {@link #templateSuratParameter} yang
	 * sedang diedit, terikat ke {@link #templateSurat}.
	 *
	 * @param event event ZK pemicu (mis. klik tombol Simpan)
	 * @return {@code true} bila validasi lolos dan data tersimpan; {@code false} bila ada field wajib
	 *         yang belum terisi
	 * @throws Exception diteruskan dari kegagalan akses database
	 */
	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Nama Parameter harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (tipe.getSelectedItem() == null) {
			MyMessageboxConfig.show("Tipe Parameter harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		TemplateSuratParameterDao templateSuratParameterDao = DaoFactory.getInstance().getTemplateSuratParameterDao();
		if (templateSuratParameter.getId() != null) {
			templateSuratParameter = templateSuratParameterDao.load(templateSuratParameter.getId());

		}

		templateSuratParameter.setTipe((String) tipe.getSelectedItem().getValue());
		templateSuratParameter.setNama(nama.getValue());
		templateSuratParameter.setKeterangan(keterangan.getValue());
		templateSuratParameter.setTemplateSurat(templateSurat);

		if (templateSuratParameter.getId() != null) {
			templateSuratParameterDao.update(templateSuratParameter);
		} else {
			templateSuratParameterDao.save(templateSuratParameter);
		}

		return true;
	}

}
