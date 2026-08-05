package ais.action.master.helper;
import ais.common.PesanFormalHelper;


import ais.common.CommonSearchFilterHelper;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.listener.DataLoader;
import ais.database.dao.DaoFactory;
import ais.database.dao.PaketRegistrasiMahasiswaDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.PaketRegistrasiMahasiswa;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class DetailPaketRegistrasiMahasiswaHelper implements DataLoader {

	private MyGrid grid;
	private PaketRegistrasiMahasiswa paketRegistrasiMahasiswa;
	private MyWindow addWindow;
	private Boolean readonly = false;

	private Combobox namajurusan;
	private Combobox namafakultas;

	private Set<Jurusan> jurusans;

	class DetailJurusanRenderer extends ais.ui.util.MyRowRenderer {

		public DetailJurusanRenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			// final PaketRegistrasiMahasiswa paketRegistrasiMahasiswa =
			// (PaketRegistrasiMahasiswa) data;
			final Jurusan jurusan = (Jurusan) data;

			// final Jurusan dataJurusan = (Jurusan)
			// paketRegistrasiMahasiswa.getJurusans();

			// new
			// Label(paketRegistrasiMahasiswa.getJurusans().toString()).setParent(row);
			new Label(jurusan.getNama()).setParent(row);

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
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

											for (Jurusan a : paketRegistrasiMahasiswa.getJurusans()) {
												if (jurusan.getId().equals(a.getId())) {
													paketRegistrasiMahasiswa.getJurusans().remove(a);
													break;
												}
											}

											PaketRegistrasiMahasiswaDao paketRegistrasiMahasiswaDao = DaoFactory
													.getInstance().getPaketRegistrasiMahasiswaDao();
											// paketRegistrasiMahasiswaDao.beginTransaction();
											paketRegistrasiMahasiswaDao.update((paketRegistrasiMahasiswa));
											// paketRegistrasiMahasiswaDao.commitTransaction();

											loadData(null);

										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											PesanFormalHelper.tampilkanGagalException("Menghapus data", "Data yang Bapak/Ibu coba hapus kemungkinan besar masih memiliki keterkaitan/relasi dengan data lain pada tabel terkait (misalnya digunakan sebagai referensi oleh transaksi, detail, atau riwayat lain), sehingga sistem basis data menolak proses penghapusan ini demi menjaga integritas data secara keseluruhan.", e, new String[]{"Periksa kembali apakah data ini masih digunakan atau direferensikan oleh data lain yang berelasi.", "Hapus atau lepaskan terlebih dahulu keterkaitan/relasi data tersebut sebelum mencoba menghapus data ini kembali.", "Jika Bapak/Ibu yakin data ini seharusnya sudah tidak digunakan lagi, hubungi Administrator untuk pengecekan lebih lanjut."});
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

	public void loadData(Object value) {
		Session session = HibernateUtil.currentSession();

		paketRegistrasiMahasiswa = (PaketRegistrasiMahasiswa) session.load(PaketRegistrasiMahasiswa.class,
				paketRegistrasiMahasiswa.getId());

		jurusans = paketRegistrasiMahasiswa.getJurusans();

		ListModel strset = new SimpleListModel(jurusans.toArray());
		grid.setRowRenderer(new DetailJurusanRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void displayDetailJurusan(final PaketRegistrasiMahasiswa paketRegistrasiMahasiswa, final Component component,
			final MyWindow window) {

		this.paketRegistrasiMahasiswa = paketRegistrasiMahasiswa;
		Common.clear(component);
		this.addWindow = window;
		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(component);
		panel.setWidth("100%");
		panel.setHeight("230px");
		panel.setTitle("Detail Jurusan");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(panel);
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah Data", "/img/new.gif");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				init(paketRegistrasiMahasiswa);
				window.setVisible(true);
				window.onModal();
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

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jurusan");
		column.setWidth("50%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("5%");

		loadData(paketRegistrasiMahasiswa);
		// borderlayout.setParent(component);

	}

	private void init(PaketRegistrasiMahasiswa paketRegistrasiMahasiswa) {
		this.paketRegistrasiMahasiswa = paketRegistrasiMahasiswa;

		Common.insertCombo(namafakultas = new Combobox(), "nama", Fakultas.class, Restrictions.eq("aktif", true));
		Common.insertCombo(namajurusan = new Combobox(), "nama", Jurusan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		class FakultasEventListener implements EventListener {

			@Override
			public void onEvent(Event event) throws Exception {
				// TODO Auto-generated method stub
				Common.clear(namajurusan);
				namajurusan.setSelectedItem(null);
				if (namafakultas.getSelectedItem() == null) {
					return;
				}
				Common.insertCombo(namajurusan, "nama", Jurusan.class,
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						CommonSearchFilterHelper.eqSelectedWithId("fakultas", namafakultas, false));

			}

		}

		namafakultas.addEventListener("onChange", new FakultasEventListener());

		addWindow.setTitle("Form Jurusan");
		addWindow.setWidth("500px");
		addWindow.setHeight("500px");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(addWindow);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(namafakultas);
		// Common.selectComboItem(namafakultas, null);
		namafakultas.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(namajurusan);
		// Common.selectComboItem(namajurusan,
		// paketRegistrasiMahasiswa.getJurusans());
		namajurusan.setWidth("90%");

		// row = new MyFormRow();
		//		// row.setParent(rows);
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
					// onSearchDefault(null);
					loadData(null);
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);

		borderlayout.setParent(addWindow);

	}

	public boolean onSave(Event event) throws Exception {
		if (namajurusan.getSelectedItem() == null) {
			MyMessageboxConfig.show(Common.getBahasaConfig("Jurusan") + " harus diisi", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		PaketRegistrasiMahasiswaDao paketRegistrasiMahasiswaDao = DaoFactory.getInstance()
				.getPaketRegistrasiMahasiswaDao();
		if (paketRegistrasiMahasiswa.getId() != null) {
			paketRegistrasiMahasiswa = paketRegistrasiMahasiswaDao.load(paketRegistrasiMahasiswa.getId());
		}

		jurusans.add((Jurusan) namajurusan.getSelectedItem().getValue());
		paketRegistrasiMahasiswa.setJurusans(jurusans);

		// paketRegistrasiMahasiswaDao.beginTransaction();
		if (paketRegistrasiMahasiswa.getId() != null) {
			paketRegistrasiMahasiswaDao.update(paketRegistrasiMahasiswa);
		} else {
			paketRegistrasiMahasiswaDao.save(paketRegistrasiMahasiswa);
		}
		// paketRegistrasiMahasiswaDao.commitTransaction();
		return true;
	}

	public void setReadonly(Boolean readonly) {
		this.readonly = readonly;
	}

	public Boolean getReadonly() {
		return readonly;
	}

}
