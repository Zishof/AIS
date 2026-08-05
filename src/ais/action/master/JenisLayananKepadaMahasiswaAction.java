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

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.JenisLayananKepadaMahasiswa;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class JenisLayananKepadaMahasiswaAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Checkbox searchaktif;

	private Textbox nama;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private JenisLayananKepadaMahasiswa jenisLayananKepadaMahasiswa;
	private MyToolbarbuttonConfig add;
	private Intbox nomorUrut;

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

		Session session = HibernateUtil.currentSession();

		int count = ((Number) session.createCriteria(JenisLayananKepadaMahasiswa.class)
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();
		if (count == 0) {

			int i = 1;
			String s = "Bimbingan dan konseling\n" + "Minat dan bakat (ekstra kurikuler)\n" + "Pembinaan soft skills\n"
					+ "Beasiswa\n" + "Kesehatan\n" + "Lainnya";
			for (String ss : s.split("\n")) {
				JenisLayananKepadaMahasiswa jenisLayananKepadaMahasiswa = new JenisLayananKepadaMahasiswa();
				jenisLayananKepadaMahasiswa.setNama(ss.trim());
				jenisLayananKepadaMahasiswa.setNomorUrut(i);
				jenisLayananKepadaMahasiswa
						.setKeterangan("Layanan kepada mahasiswa yang berupa pembinaan dan pemahanan " + ss);
				session.save(jenisLayananKepadaMahasiswa);
				i++;
			}

		}

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

		String[] contents = new String[] { "id", "nama", "nomorUrut", "aktif", "pa", "bimbingan", "revisi", "umum",
				"keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(JenisLayananKepadaMahasiswa.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, JenisLayananKepadaMahasiswa.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	class JenisLayananKepadaMahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final JenisLayananKepadaMahasiswa jenisLayananKepadaMahasiswa = (JenisLayananKepadaMahasiswa) arg1;

			RevisiHelper.createNewRevisi(JenisLayananKepadaMahasiswa.class, jenisLayananKepadaMahasiswa,
					jenisLayananKepadaMahasiswa.getNama()).setParent(arg0);
			new Label(Common.numberFormat.get().format(jenisLayananKepadaMahasiswa.getNomorUrut())).setParent(arg0);

			Hbox hbox = new Hbox();
			hbox.setParent(arg0);

			final MyCheckboxConfig aktif = new MyCheckboxConfig("Aktif");
			aktif.setChecked(jenisLayananKepadaMahasiswa.getAktif());
			aktif.setParent(hbox);
			aktif.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenisLayananKepadaMahasiswa.setAktif(aktif.isChecked());
					Common.refreshSaveOrUpdate(jenisLayananKepadaMahasiswa);
				}
			});

			final MyCheckboxConfig pa = new MyCheckboxConfig("Pembimbing Akdemik");
			pa.setChecked(jenisLayananKepadaMahasiswa.getPa());
			pa.setParent(hbox);
			pa.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenisLayananKepadaMahasiswa.setPa(pa.isChecked());
					Common.refreshSaveOrUpdate(jenisLayananKepadaMahasiswa);
				}
			});

			final MyCheckboxConfig bimbingan = new MyCheckboxConfig("Bimbingan Skripsi/TA");
			bimbingan.setChecked(jenisLayananKepadaMahasiswa.getBimbingan());
			bimbingan.setParent(hbox);
			bimbingan.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenisLayananKepadaMahasiswa.setBimbingan(bimbingan.isChecked());
					Common.refreshSaveOrUpdate(jenisLayananKepadaMahasiswa);
				}
			});

			final MyCheckboxConfig revisi = new MyCheckboxConfig("Revisi Skripsi/TA");
			revisi.setChecked(jenisLayananKepadaMahasiswa.getRevisi());
			revisi.setParent(hbox);
			revisi.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenisLayananKepadaMahasiswa.setRevisi(revisi.isChecked());
					Common.refreshSaveOrUpdate(jenisLayananKepadaMahasiswa);
				}
			});

			final MyCheckboxConfig umum = new MyCheckboxConfig("Umum");
			umum.setChecked(jenisLayananKepadaMahasiswa.getUmum());
			umum.setParent(hbox);
			umum.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenisLayananKepadaMahasiswa.setUmum(umum.isChecked());
					Common.refreshSaveOrUpdate(jenisLayananKepadaMahasiswa);
				}
			});

			new Label(jenisLayananKepadaMahasiswa.getKeterangan()).setParent(arg0);

			Common.copyEditDeleteButtons(edit, delete, jenisLayananKepadaMahasiswa,
					JenisLayananKepadaMahasiswaAction.this).setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new JenisLayananKepadaMahasiswa());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		jenisLayananKepadaMahasiswa = (JenisLayananKepadaMahasiswa) obj;
		init(jenisLayananKepadaMahasiswa);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(JenisLayananKepadaMahasiswa jenisLayananKepadaMahasiswa) {
		this.jenisLayananKepadaMahasiswa = jenisLayananKepadaMahasiswa;
		addWindow.setTitle(jenisLayananKepadaMahasiswa.getId() == null ? "Tambah Jenis Layanan Kepada Mahasiswa" : "Ubah Jenis Layanan Kepada Mahasiswa");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Jenis Layanan"));
		row.appendChild(nama = new Textbox(jenisLayananKepadaMahasiswa.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nomor Urut"));
		row.appendChild(nomorUrut = new Intbox(jenisLayananKepadaMahasiswa.getNomorUrut()));
		nomorUrut.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(jenisLayananKepadaMahasiswa.getKeterangan()));
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
		if (nama.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Jenis Layanan Kepada Mahasiswa",
					"Kolom Nama Jenis Layanan Kepada Mahasiswa belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama Jenis Layanan Kepada Mahasiswa.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		boolean i = checkNamaJenisLayananKepadaMahasiswa();
		if (i) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Jenis Layanan Kepada Mahasiswa",
					"Nama Jenis Layanan Kepada Mahasiswa sudah terdaftar sebelumnya di database, sehingga tidak dapat disimpan kembali untuk menghindari duplikasi data.",
					new String[] {
							"Gunakan nama jenis layanan kepada mahasiswa yang berbeda dari data yang sudah ada.",
							"Periksa kembali daftar data yang sudah tersimpan apabila Bapak/Ibu ragu."
					});
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (jenisLayananKepadaMahasiswa.getId() != null) {
			jenisLayananKepadaMahasiswa = (JenisLayananKepadaMahasiswa) session.load(JenisLayananKepadaMahasiswa.class,
					jenisLayananKepadaMahasiswa.getId());

		}

		jenisLayananKepadaMahasiswa.setNama(nama.getValue());
		jenisLayananKepadaMahasiswa.setNomorUrut(nomorUrut.getValue());
		jenisLayananKepadaMahasiswa.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, jenisLayananKepadaMahasiswa);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(JenisLayananKepadaMahasiswa.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));

		if (order)
			criteria.addOrder(Order.asc("nomorUrut"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<JenisLayananKepadaMahasiswa> jenisLayananKepadaMahasiswa = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(jenisLayananKepadaMahasiswa);
		grid.setRowRenderer(new JenisLayananKepadaMahasiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkNamaJenisLayananKepadaMahasiswa() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(JenisLayananKepadaMahasiswa.class)
				.setProjection(Projections.rowCount()).add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.jenisLayananKepadaMahasiswa.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.jenisLayananKepadaMahasiswa.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
