package ais.action.master;

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

import ais.action.master.helper.AmbilDataCalonMahasiswaBanbox;
import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.sekolah.helper.AmbilDataCalonSiswaBanbox;
import ais.action.master.sekolah.helper.AmbilDataSiswaBanbox;
import ais.action.report.format1.keuangan.LaporanPengeluaranMahasiswa;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Deposit;
import ais.database.model.GeneralValueObject;
import ais.database.model.JenisPembayaran;
import ais.database.model.JenisPengeluaranMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.PengeluaranMahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class PengeluaranMahasiswaAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnamaPenabung;
	private Textbox searchnama;

	private AmbilDataMahasiswaBanbox mahasiswa;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private PengeluaranMahasiswa pengeluaranMahasiswa;
	private MyToolbarbuttonConfig add;
	private Tbmuser tbmuser;
	private MyDoublebox nominal;
	private MyDatebox waktu;

	private Tabpanel tabLaporanPengeluaranMahasiswa;
	private Combobox jenisPembayaran;

	public void onLaporanPengeluaranMahasiswa(Event event) {

		if (tabLaporanPengeluaranMahasiswa.getChildren().size() == 0) {
			LaporanPengeluaranMahasiswa laporan = new LaporanPengeluaranMahasiswa();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(tabLaporanPengeluaranMahasiswa);
		}
	}

	private Tabpanel tabJenisPengeluaranMahasiswa;

	public void onJenis(Event event) {

		if (tabJenisPengeluaranMahasiswa.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tabJenisPengeluaranMahasiswa);
			MyInclude iframe = new MyInclude("/pages/master/jenis_pengeluaran_mahasiswa.zul");
			iframe.setParent(window);
		}
	}

	private Combobox jenisPengeluaranMahasiswa;
	private AmbilDataCalonMahasiswaBanbox calonMahasiswa;
	private boolean pt;
	private boolean ya;
	private AmbilDataSiswaBanbox siswa;
	private AmbilDataCalonSiswaBanbox calonSiswa;

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
		boolean[] ptYa = Common.chekPtAtauSekolah();
		pt = ptYa[0];
		ya = ptYa[1];

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE) && tbmuser != null
				&& tbmuser.getMahasiswa() == null);
		add.setTooltiptext("Tambah");
		}

		if (tabJenisPengeluaranMahasiswa != null) { tabJenisPengeluaranMahasiswa.setVisible((add != null && add.isVisible())); }
		tabJenisPengeluaranMahasiswa.getLinkedTab().setVisible((add != null && add.isVisible()));

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "id", "mahasiswa", "biodataCalonMahasiswa", "siswa", "calonSiswa", "nominal",
				"tanggal", "jenisPembayaran", "jenisPengeluaranMahasiswa", "keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(PengeluaranMahasiswa.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, PengeluaranMahasiswa.class, contents);
		upload.setVisible((add != null && add.isVisible()) && edit && delete && tbmuser != null && tbmuser.getMahasiswa() == null
				&& tbmuser.getSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null);
		Common.appendKeToolbar(upload, add, comp);
	}

	class PengeluaranMahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PengeluaranMahasiswa pengeluaranMahasiswa = (PengeluaranMahasiswa) arg1;
			if (pengeluaranMahasiswa.getSiswa() != null) {

				CommonMedia.tampilkanGambarKecil(pengeluaranMahasiswa.getSiswa()).setParent(arg0);
				RevisiHelper.createNewRevisi(Deposit.class, pengeluaranMahasiswa,
						pengeluaranMahasiswa.getSiswa().getNomorInduk() + "-"
								+ pengeluaranMahasiswa.getSiswa().getNama())
						.setParent(arg0);

			} else if (pengeluaranMahasiswa.getCalonSiswa() != null) {

				CommonMedia.tampilkanGambarKecil(pengeluaranMahasiswa.getCalonSiswa()).setParent(arg0);
				RevisiHelper.createNewRevisi(Deposit.class, pengeluaranMahasiswa,
						pengeluaranMahasiswa.getCalonSiswa().getNoRegistrasi() + "-"
								+ pengeluaranMahasiswa.getCalonSiswa().getNama())
						.setParent(arg0);

			} else if (pengeluaranMahasiswa.getMahasiswa() != null) {

				CommonMedia.tampilkanGambarKecil(pengeluaranMahasiswa.getMahasiswa()).setParent(arg0);
				RevisiHelper.createNewRevisi(PengeluaranMahasiswa.class, pengeluaranMahasiswa,
						pengeluaranMahasiswa.getMahasiswa().getNim() + "-"
								+ pengeluaranMahasiswa.getMahasiswa().getNama())
						.setParent(arg0);

			} else if (pengeluaranMahasiswa.getCalonMahasiswa() != null) {
				CommonMedia.tampilkanGambarKecil(pengeluaranMahasiswa.getCalonMahasiswa()).setParent(arg0);
				RevisiHelper.createNewRevisi(PengeluaranMahasiswa.class, pengeluaranMahasiswa,
						pengeluaranMahasiswa.getCalonMahasiswa().getNoRegistrasi() + "-"
								+ pengeluaranMahasiswa.getCalonMahasiswa().getNama())
						.setParent(arg0);
			}
			new Label(Common.numberFormat.get().format(pengeluaranMahasiswa.getNominal())).setParent(arg0);
			new Label(Common.dateFormat.get().format(pengeluaranMahasiswa.getWaktu())).setParent(arg0);
			new Label(pengeluaranMahasiswa.getJenisPembayaran() == null ? ""
					: pengeluaranMahasiswa.getJenisPembayaran().getNama()).setParent(arg0);
			new Label(pengeluaranMahasiswa.getJenisPengeluaranMahasiswa() == null ? ""
					: pengeluaranMahasiswa.getJenisPengeluaranMahasiswa().getNama()).setParent(arg0);
			new Label(pengeluaranMahasiswa.getKeterangan()).setParent(arg0);

			if (tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null) {
				Common.copyEditDeleteButtons(edit, delete, pengeluaranMahasiswa, PengeluaranMahasiswaAction.this)
						.setParent(arg0);
			}
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new PengeluaranMahasiswa());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		pengeluaranMahasiswa = (PengeluaranMahasiswa) obj;
		init(pengeluaranMahasiswa);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(PengeluaranMahasiswa pengeluaranMahasiswa) throws Exception {
		this.pengeluaranMahasiswa = pengeluaranMahasiswa;
		addWindow.setTitle(pengeluaranMahasiswa.getId() == null ? "Tambah Pengeluaran" : "Ubah Pengeluaran");
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

		boolean[] ptYa = Common.chekPtAtauSekolah();
		pt = ptYa[0];
		ya = ptYa[1];

		MyFormRow row = new MyFormRow();
		row.setVisible(pt);
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mahasiswa *"));
		row.appendChild(mahasiswa = new AmbilDataMahasiswaBanbox());
		mahasiswa.setValue(
				pengeluaranMahasiswa.getMahasiswa() == null ? "" : pengeluaranMahasiswa.getMahasiswa().getNama());
		mahasiswa.setAttribute("mahasiswa", pengeluaranMahasiswa.getMahasiswa());
		mahasiswa.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("(atau) Calon Mahasiswa *"));
		row.appendChild(calonMahasiswa = new AmbilDataCalonMahasiswaBanbox());
		calonMahasiswa.setValue(pengeluaranMahasiswa.getCalonMahasiswa() == null ? ""
				: pengeluaranMahasiswa.getCalonMahasiswa().getNama());
		calonMahasiswa.setAttribute("calonMahasiswa", pengeluaranMahasiswa.getCalonMahasiswa());
		calonMahasiswa.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(ya);
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Siswa *"));
		row.appendChild(siswa = new AmbilDataSiswaBanbox());
		siswa.setValue(pengeluaranMahasiswa.getSiswa() == null ? "" : pengeluaranMahasiswa.getSiswa().getNama());
		siswa.setAttribute("siswa", pengeluaranMahasiswa.getSiswa());
		siswa.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(ya);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("(atau) Calon Siswa *"));
		row.appendChild(calonSiswa = new AmbilDataCalonSiswaBanbox());
		calonSiswa.setValue(
				pengeluaranMahasiswa.getCalonSiswa() == null ? "" : pengeluaranMahasiswa.getCalonSiswa().getNama());
		calonSiswa.setAttribute("calonSiswa", pengeluaranMahasiswa.getCalonSiswa());
		calonSiswa.setWidth("90%");

		EventListener a = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				mahasiswa.getParent().setVisible(false);
				calonMahasiswa.getParent().setVisible(false);

				siswa.getParent().setVisible(false);
				calonSiswa.getParent().setVisible(false);

				if (calonMahasiswa.getAttribute("calonMahasiswa") != null) {
					calonMahasiswa.getParent().setVisible(true);
				} else if (mahasiswa.getAttribute("mahasiswa") != null) {
					mahasiswa.getParent().setVisible(true);
				} else if (siswa.getAttribute("siswa") != null) {
					siswa.getParent().setVisible(true);
				} else if (calonSiswa.getAttribute("calonSiswa") != null) {
					calonSiswa.getParent().setVisible(true);
				} else {
					mahasiswa.getParent().setVisible(pt);
					calonMahasiswa.getParent().setVisible(pt);

					siswa.getParent().setVisible(ya);
					calonSiswa.getParent().setVisible(ya);
				}

			}
		};

		mahasiswa.setEventListener(a);
		calonMahasiswa.setEventListener(a);
		siswa.setEventListener(a);
		calonSiswa.setEventListener(a);
		a.onEvent(null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nominal *"));
		row.appendChild(nominal = new MyDoublebox(pengeluaranMahasiswa.getNominal()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal/Waktu *"));
		row.appendChild(waktu = new MyDatebox(pengeluaranMahasiswa.getWaktu()));
		waktu.setFormat(Common.dateFormat.get().toPattern());
		waktu.setReadonly(true);
		waktu.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Cara Pembayaran *"));

		if (pengeluaranMahasiswa != null && pengeluaranMahasiswa.getJenisPembayaran() == null) {
			JenisPembayaran jenisPembayaranDefault = (JenisPembayaran) HibernateUtil.currentSession()
					.createCriteria(JenisPembayaran.class).add(Restrictions.eq("defaultPembayaran", true))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setMaxResults(1)
					.uniqueResult();
			pengeluaranMahasiswa.setJenisPembayaran(jenisPembayaranDefault);
		}
		Common.insertCombo(jenisPembayaran = new Combobox(), "nama", "akun", JenisPembayaran.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		row.appendChild(jenisPembayaran);
		Common.selectComboItem(jenisPembayaran, pengeluaranMahasiswa.getJenisPembayaran());
		jenisPembayaran.setWidth("90%");
		jenisPembayaran.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pengeluaran *"));

		Common.insertCombo(jenisPengeluaranMahasiswa = new Combobox(), "nama", "akun", JenisPengeluaranMahasiswa.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		row.appendChild(jenisPengeluaranMahasiswa);
		Common.selectComboItem(jenisPengeluaranMahasiswa, pengeluaranMahasiswa.getJenisPengeluaranMahasiswa());
		jenisPengeluaranMahasiswa.setWidth("90%");
		jenisPengeluaranMahasiswa.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(pengeluaranMahasiswa.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Selesai", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setVisible((add != null && add.isVisible()) && edit && delete && tbmuser != null && tbmuser.getMahasiswa() == null
				&& tbmuser.getSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null);
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
		if (mahasiswa.getAttribute("mahasiswa") == null && calonMahasiswa.getAttribute("calonMahasiswa") == null
				&& siswa.getAttribute("siswa") == null && calonSiswa.getAttribute("calonSiswa") == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Pengeluaran oleh",
					"Kolom Pengeluaran oleh belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Pengeluaran oleh.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (nominal.getValue() == null || nominal.getValue().intValue() == 0) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Nominal",
					"Kolom Nominal belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nominal.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (jenisPembayaran.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Cara pembayaran",
					"Kolom Cara pembayaran belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Cara pembayaran.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (jenisPengeluaranMahasiswa.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Jenis tabungan",
					"Kolom Jenis tabungan belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Jenis tabungan.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (pengeluaranMahasiswa.getId() != null) {
			pengeluaranMahasiswa = (PengeluaranMahasiswa) session.load(PengeluaranMahasiswa.class,
					pengeluaranMahasiswa.getId());
		}

		pengeluaranMahasiswa.setCalonMahasiswa((BiodataCalonMahasiswa) calonMahasiswa.getAttribute("calonMahasiswa"));
		pengeluaranMahasiswa.setSiswa((Siswa) siswa.getAttribute("siswa"));
		pengeluaranMahasiswa.setCalonSiswa((CalonSiswa) calonSiswa.getAttribute("calonSiswa"));
		pengeluaranMahasiswa.setMahasiswa((Mahasiswa) mahasiswa.getAttribute("mahasiswa"));
		pengeluaranMahasiswa.setJenisPembayaran((JenisPembayaran) jenisPembayaran.getSelectedItem().getValue());
		pengeluaranMahasiswa.setJenisPengeluaranMahasiswa(
				(JenisPengeluaranMahasiswa) jenisPengeluaranMahasiswa.getSelectedItem().getValue());
		pengeluaranMahasiswa.setNominal(nominal.getValue());
		pengeluaranMahasiswa.setWaktu(waktu.getValue());
		pengeluaranMahasiswa.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, pengeluaranMahasiswa);

		return true;
	}

	public Criteria initCriteria(boolean order) {

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PengeluaranMahasiswa.class);

		if (searchnamaPenabung != null && !searchnamaPenabung.getValue().trim().isEmpty()) {
			criteria.createAlias("mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)
					.createAlias("biodataCalonMahasiswa", "biodataCalonMahasiswa", Criteria.LEFT_JOIN)
					.createAlias("calonSiswa", "calonSiswa", Criteria.LEFT_JOIN)
					.createAlias("siswa", "siswa", Criteria.LEFT_JOIN)

					.add(Restrictions.or(
							Restrictions.ilike("calonSiswa.namaSiswa", searchnamaPenabung.getValue().trim(),
									MatchMode.ANYWHERE),
							Restrictions.or(
									Restrictions.ilike("siswa.namaSiswa", searchnamaPenabung.getValue().trim(),
											MatchMode.ANYWHERE),
									Restrictions.or(
											Restrictions.ilike("mahasiswa.nama", searchnamaPenabung.getValue().trim(),
													MatchMode.ANYWHERE),
											Restrictions.ilike("biodataCalonMahasiswa.nama",
													searchnamaPenabung.getValue().trim(), MatchMode.ANYWHERE)))));
		}

		if (order)
			criteria.addOrder(Order.desc("waktu"));
		criteria

				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("keterangan", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<PengeluaranMahasiswa> pengeluaranMahasiswa = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(pengeluaranMahasiswa);
		grid.setRowRenderer(new PengeluaranMahasiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

}
