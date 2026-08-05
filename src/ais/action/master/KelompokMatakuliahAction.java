package ais.action.master;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
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

import ais.action.master.helper.KelompokMatakuliahPunyaMatakuliahHelper;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.KelompokMatakuliahDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Jenjang;
import ais.database.model.KelompokMatakuliah;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class KelompokMatakuliahAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Combobox searchjenjang;

	private Checkbox searchaktif;

	private Textbox kode;
	private Textbox nama;
	private Intbox nomorUrut;
	private Intbox berlakuMulaiTahunAngkatan;
	private Combobox jenjang;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private KelompokMatakuliah kelompokMatakuliah;
	private MyToolbarbuttonConfig add;
	private Textbox namaen;
	private Textbox feeder;

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

		Common.insertComboDanSemua(searchjenjang, "nama", "keterangan", Jenjang.class,Restrictions.or(Restrictions.eq("aktifDipilih", true), Restrictions.isNull("aktifDipilih")));

		Session session = HibernateUtil.currentSession();

		int count = ((Number) session.createCriteria(KelompokMatakuliah.class).setProjection(Projections.rowCount())
				.uniqueResult()).intValue();

		if (count == 0) {
			KelompokMatakuliah kelompokMatakuliah = new KelompokMatakuliah();
			kelompokMatakuliah.setNama("Mata Kuliah Pengembangan Kepribadian (MPK)");
			kelompokMatakuliah.setKeterangan("Mata Kuliah Pengembangan Kepribadian (MPK)");
			kelompokMatakuliah.setNomorUrut(1);
			session.save(kelompokMatakuliah);
			kelompokMatakuliah = new KelompokMatakuliah();
			kelompokMatakuliah.setNama("Mata Kuliah Keilmuan dan Ketrampilan (MKK)");
			kelompokMatakuliah.setKeterangan("Mata Kuliah Keilmuan dan Ketrampilan (MKK)");
			kelompokMatakuliah.setNomorUrut(2);
			session.save(kelompokMatakuliah);
			kelompokMatakuliah = new KelompokMatakuliah();
			kelompokMatakuliah.setNama("Mata Kuliah Keahlian dan Berkarya (MKB)");
			kelompokMatakuliah.setKeterangan("Mata Kuliah Keahlian dan Berkarya (MKB)");
			kelompokMatakuliah.setNomorUrut(3);
			session.save(kelompokMatakuliah);
			kelompokMatakuliah = new KelompokMatakuliah();
			kelompokMatakuliah.setNama("Mata Kuliah Perilaku dan Berkarya (MPB)");
			kelompokMatakuliah.setKeterangan("Mata Kuliah Perilaku dan Berkarya (MPB)");
			kelompokMatakuliah.setNomorUrut(4);
			session.save(kelompokMatakuliah);
			kelompokMatakuliah = new KelompokMatakuliah();
			kelompokMatakuliah.setNama("Mata Kuliah Berkehidupan Bermasyarakat (MBB)");
			kelompokMatakuliah.setKeterangan("Mata Kuliah Berkehidupan Bermasyarakat (MBB)");
			kelompokMatakuliah.setNomorUrut(5);
			session.save(kelompokMatakuliah);

		}

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		String[] contents = new String[] { "id", "kode", "nama", "namaen", "keterangan", "aktif", "feeder" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(KelompokMatakuliah.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, KelompokMatakuliah.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
	}

	class KelompokMatakuliahRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final KelompokMatakuliah kelompokMatakuliah = (KelompokMatakuliah) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					Common.clear(detail);
					if (detail.isOpen()) {
						KelompokMatakuliahPunyaMatakuliahHelper detailperkuliahanHelper = new KelompokMatakuliahPunyaMatakuliahHelper();
						detailperkuliahanHelper.display(kelompokMatakuliah, detail, addWindow);
					}
				}
			});

			new Label(kelompokMatakuliah.getKode()).setParent(arg0);

			RevisiHelper.createNewRevisi(KelompokMatakuliah.class, kelompokMatakuliah, kelompokMatakuliah.getNama())
					.setParent(arg0);
			new Label(kelompokMatakuliah.getNamaen()).setParent(arg0);
			new Label(kelompokMatakuliah.getNomorUrut() + "").setParent(arg0);
			new Label(kelompokMatakuliah.getBerlakuMulaiTahunAngkatan() + "").setParent(arg0);
			new Label(kelompokMatakuliah.getJenjang() == null ? "Semua Jenjang"
					: kelompokMatakuliah.getJenjang().getNama()).setParent(arg0);

			new Label(kelompokMatakuliah.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(kelompokMatakuliah.getAktif());
			checkbox.setParent(arg0);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					kelompokMatakuliah.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(kelompokMatakuliah);
				}
			});

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(kelompokMatakuliah);
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
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Common.refreshDelete(kelompokMatakuliah);

											onSearchDefault(event);
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
		init(new KelompokMatakuliah());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(KelompokMatakuliah kelompokMatakuliah) {
		this.kelompokMatakuliah = kelompokMatakuliah;
		addWindow.setTitle(kelompokMatakuliah.getId() == null ? "Tambah Kelompok Matakuliah" : "Ubah Kelompok Matakuliah");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Kelompok Matakuliah"));
		row.appendChild(kode = new Textbox(kelompokMatakuliah.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Kelompok Matakuliah"));
		row.appendChild(nama = new Textbox(kelompokMatakuliah.getNama() == null ? "" : kelompokMatakuliah.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Kelompok Matakuliah (English)"));
		row.appendChild(namaen = new Textbox(kelompokMatakuliah.getNamaen()));
		namaen.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nomor Urut"));
		row.appendChild(nomorUrut = new Intbox(kelompokMatakuliah.getNomorUrut()));
		nomorUrut.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Berlaku mulai tahun angkatan"));
		row.appendChild(berlakuMulaiTahunAngkatan = new Intbox(kelompokMatakuliah.getBerlakuMulaiTahunAngkatan()));
		berlakuMulaiTahunAngkatan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Berlaku untuk jenjang"));
		row.appendChild(jenjang = new Combobox());
		Common.insertCombo(jenjang, "nama", "keterangan", Jenjang.class,Restrictions.or(Restrictions.eq("aktifDipilih", true), Restrictions.isNull("aktifDipilih")));
		Common.selectComboItem(jenjang, kelompokMatakuliah.getJenjang());
		jenjang.setWidth("90%");

		Common.initKeterangan(rows, "(kosongkan jenjang jika berlaku semua)");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(
				kelompokMatakuliah.getKeterangan() == null ? "" : kelompokMatakuliah.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		row = new MyFormRow();
		row.setVisible(Common.getApakahAdminBolehAksesFeeder());
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode FEEDER"));
		row.appendChild(feeder = new Textbox(kelompokMatakuliah.getFeeder()));
		feeder.setWidth("90%");

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
		if (kode.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Kode Kelompok Matakuliah",
					"Kolom Kode Kelompok Matakuliah belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Kode Kelompok Matakuliah.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (nama.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Kelompok Matakuliah",
					"Kolom Nama Kelompok Matakuliah belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama Kelompok Matakuliah.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (berlakuMulaiTahunAngkatan.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Mulai berlaku",
					"Kolom Mulai berlaku belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Mulai berlaku.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (nomorUrut.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Nomor urut",
					"Kolom Nomor urut belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nomor urut.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		boolean i = checkKodeKelompokMatakuliah();
		if (i) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Kode Kelompok Matakuliah",
					"Kode Kelompok Matakuliah sudah terdaftar sebelumnya di database, sehingga tidak dapat disimpan kembali untuk menghindari duplikasi data.",
					new String[] {
							"Gunakan Kode Kelompok Matakuliah yang berbeda dari data yang sudah ada.",
							"Periksa kembali daftar data yang sudah tersimpan apabila Bapak/Ibu ragu."
					});
			return false;
		}

		// i = checkNamaKelompokMatakuliah();
		// if (i) {
		// MyMessageboxConfig.show("Nama Kelompok Matakuliah sudah ada di
		// database",
		// "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
		// return false;
		// }

		KelompokMatakuliahDao kelompokMatakuliahDao = DaoFactory.getInstance().getKelompokMatakuliahDao();
		if (kelompokMatakuliah.getId() != null) {
			kelompokMatakuliah = kelompokMatakuliahDao.load(kelompokMatakuliah.getId());

		}
		kelompokMatakuliah.setKode(kode.getValue().trim());
		kelompokMatakuliah.setNomorUrut(nomorUrut.getValue());
		kelompokMatakuliah.setBerlakuMulaiTahunAngkatan(berlakuMulaiTahunAngkatan.getValue());
		kelompokMatakuliah.setNama(nama.getValue());
		kelompokMatakuliah.setKeterangan(keterangan.getValue());
		kelompokMatakuliah.setJenjang(
				(Jenjang) (jenjang.getSelectedItem() == null ? null : jenjang.getSelectedItem().getValue()));
		kelompokMatakuliah.setNamaen(namaen.getValue());
		kelompokMatakuliah.setFeeder(feeder.getValue().trim());

		if (kelompokMatakuliah.getId() != null) {
			kelompokMatakuliahDao.update(kelompokMatakuliah);
		} else {
			kelompokMatakuliahDao.save(kelompokMatakuliah);
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(KelompokMatakuliah.class)
				.add(searchaktif == null || searchaktif.isChecked() ? Restrictions.eq("aktif", true) : Restrictions.sqlRestriction("true"));

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchjenjang.getSelectedItem() == null || searchjenjang.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("jenjang", searchjenjang.getSelectedItem().getValue()));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<KelompokMatakuliah> kelompokMatakuliah = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(kelompokMatakuliah);
		grid.setRowRenderer(new KelompokMatakuliahRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkKodeKelompokMatakuliah() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(KelompokMatakuliah.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("kode", kode.getValue().trim()))
				.add(this.kelompokMatakuliah.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.kelompokMatakuliah.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

	public Boolean checkNamaKelompokMatakuliah() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(KelompokMatakuliah.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.kelompokMatakuliah.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.kelompokMatakuliah.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
