
package ais.action.master.rab;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.East;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
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

import ais.action.master.akunting.helper.AmbilDataPegawaiBanbox;
import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.generic.AmbilDataTbmuserBanyak;
import ais.action.master.sekolah.helper.AmbilDataGuruBanbox;
import ais.action.master.surat.helper.FotoGambarTandaTanganPejabatHelper;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.rab.PejabatDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Pegawai;
import ais.database.model.Tbmuser;
import ais.database.model.employ.JenisJabatan;
import ais.database.model.file.FotoGambarTandaTanganPejabat;
import ais.database.model.rab.Pejabat;
import ais.database.model.sekolah.Guru;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk pejabat. Tipe ini merupakan titik masuk UI yang menghubungkan event
 * layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Textbox searchjenis}, {@code Checkbox
 * searchaktif}, {@code AmbilDataPegawaiBanbox pegawai}, {@code Combobox jenisJabatan}; inisialisasi/lifecycle
 * ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code initDetail()}, {@code init()}, {@code
 * initCriteria()}); pembacaan/pencarian ({@code onSearchDefault()}); mutasi data ({@code onSave()}); operasi
 * domain lain ({@code onAdd()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang
 * disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see GenericAutowireComposer
 */
public class PejabatAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchjenis;
	private Checkbox searchaktif;
	// private AmbilDataSatuanKerjaBanbox searchparent;

	private AmbilDataPegawaiBanbox pegawai;
	// private AmbilDataSatuanKerjaBanbox satuanKerja;
	private Combobox jenisJabatan;
	private MyCheckboxConfig aktif;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private Pejabat pejabat;
	private MyToolbarbuttonConfig add;

	private MyGrid gridGambar;
	private AmbilDataDosenBanbox dosen;
	private AmbilDataGuruBanbox guru;
	private Textbox jenisPengguna;
	private Textbox usernamePengguna;

	// private SatuanKerjaTreeModel satuanKerjaTreeModel;

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

		// satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

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

		// searchparent.setEventListener(new EventListener() {
		//
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		// onSearchDefault(null);
		// }
		// });
	}

	class PejabatRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Pejabat pejabat = (Pejabat) arg1;

			if (pejabat.getPegawai() != null) {
				CommonMedia.tampilkanGambarKecil(pejabat.getPegawai()).setParent(arg0);
				RevisiHelper.createNewRevisi(Pejabat.class, pejabat,
						pejabat.getPegawai() == null ? "" : pejabat.getPegawai().getNama()).setParent(arg0);
			} else if (pejabat.getDosen() != null) {
				CommonMedia.tampilkanGambarKecil(pejabat.getDosen()).setParent(arg0);
				RevisiHelper.createNewRevisi(Dosen.class, pejabat,
						pejabat.getDosen() == null ? "" : pejabat.getDosen().getNama()).setParent(arg0);
			} else if (pejabat.getGuru() != null) {
				CommonMedia.tampilkanGambarKecil(pejabat.getGuru()).setParent(arg0);
				RevisiHelper.createNewRevisi(Guru.class, pejabat,
						pejabat.getGuru() == null ? "" : pejabat.getGuru().getNama()).setParent(arg0);
			} else {
				new Label().setParent(arg0);
				new Label(pejabat.getNama()).setParent(arg0);
			}

			// new Label(pejabat.getSatuanKerja() == null ? "" :
			// pejabat.getSatuanKerja().getNama()).setParent(arg0);

			new Label(pejabat.getJenisPengguna().isEmpty() ? "Tidak ditentukan" : pejabat.getJenisPengguna())
					.setParent(arg0);
			new Label(pejabat.getUsernamePengguna().isEmpty() ? "Tidak ditentukan" : pejabat.getUsernamePengguna())
					.setParent(arg0);

			new Label(pejabat.getJenisJabatan() == null ? "" : pejabat.getJenisJabatan().getNama()).setParent(arg0);
			new Label(pejabat.getAktif() == null || !pejabat.getAktif() ? "Tidak" : "Aktif").setParent(arg0);
			new Label(pejabat.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(pejabat);
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
											Common.refreshDeleteFlush(pejabat);
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
			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(arg0);
		}

	}

	protected void initDetail(final Pejabat pejabat, Component component) throws Exception {
		Tabbox tabbox = new Tabbox();
		tabbox.setParent(component);
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		final MyTabConfig tabGambar = new MyTabConfig("Gambar Tanda Tangan");
		tabGambar.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		final Tabpanel tabpanelGambar = new ais.ui.util.MyTabpanel();
		tabpanelGambar.setParent(tabpanels);

		tabpanelGambar
				.appendChild(new FotoGambarTandaTanganPejabatHelper(gridGambar = new MyGrid()).initDetail(pejabat));
	}

	public void onAdd(Event event) throws Exception {
		init(new Pejabat());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(Pejabat pejabat) throws Exception {
		this.pejabat = pejabat;
		addWindow.setTitle(pejabat.getId() == null ? "Tambah Pejabat" : "Ubah Pejabat");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		
		boolean[] ptYa = Common.chekPtAtauSekolah();
		boolean pt = ptYa[0];
		boolean ya = ptYa[1]; 

		East east = new East();
		east.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(east, true);
		east.setWidth("60%");

		initDetail(pejabat, east);

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
		column.setWidth("40%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Jabatan *"));
		row.appendChild(jenisJabatan = new Combobox());
		Common.insertCombo(jenisJabatan, "nama", JenisJabatan.class, Restrictions.eq("aktif", true));
		Common.selectComboItem(jenisJabatan, pejabat.getJenisJabatan());
		jenisJabatan.setWidth("90%");
		jenisJabatan.setReadonly(true);

		
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis pengguna (jika menggunakan grup pengguna)"));
		row.appendChild(jenisPengguna = new Textbox(pejabat.getJenisPengguna()));
		jenisPengguna.setWidth("90%");
		jenisPengguna.setRows(2);

		Common.initKeterangan(rows,
				"Jika lebih dari satu, pisahkan dengan tanda koma (,). Kosongkan apabila boleh diajukan oleh semua aktor pengguna");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Username pengguna (jika menggunakan userid)"));
		row.appendChild(usernamePengguna = new Textbox(pejabat.getUsernamePengguna()));
		usernamePengguna.setWidth("90%");
		usernamePengguna.setRows(2);

		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Ambil Username Pengguna",
				"/img/user_male_add.png");

		final MyFormRow rowAmbilPengguna = new MyFormRow();
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

		Common.initKeterangan(rows,
				"Jika lebih dari satu, pisahkan dengan tanda koma (,). Kosongkan apabila boleh diajukan oleh semua username pengguna");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pegawai (jika Pegawai)"));
		row.appendChild(pegawai = new AmbilDataPegawaiBanbox());
		pegawai.setAttribute("pegawai",
				pejabat.getPegawai() == null ? Common.getCurrentUser().ambilPegawai() : pejabat.getPegawai());
		pegawai.setValue(pejabat.getPegawai() == null
				? (Common.getCurrentUser().ambilPegawai() == null ? ""
						: (Common.getCurrentUser().ambilPegawai().getCode() + " - "
								+ Common.getCurrentUser().ambilPegawai().getNama()))
				: pejabat.getPegawai().getCode() + " - " + pejabat.getPegawai().getNama());
		pegawai.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dosen (jika Dosen)"));
		row.appendChild(dosen = new AmbilDataDosenBanbox());
		dosen.setAttribute("dosen",
				pejabat.getDosen() == null ? Common.getCurrentUser().getDosen() : pejabat.getDosen());
		dosen.setValue(pejabat.getDosen() == null
				? (Common.getCurrentUser().getDosen() == null ? ""
						: (Common.getCurrentUser().getDosen().getCode() + " - "
								+ Common.getCurrentUser().getDosen().getNama()))
				: pejabat.getDosen().getCode() + " - " + pejabat.getDosen().getNama());
		dosen.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(ya);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Guru (jika Guru)"));
		row.appendChild(guru = new AmbilDataGuruBanbox());
		guru.setAttribute("guru", pejabat.getGuru() == null ? Common.getCurrentUser().getGuru() : pejabat.getGuru());
		guru.setValue(pejabat.getGuru() == null
				? (Common.getCurrentUser().getGuru() == null ? ""
						: (Common.getCurrentUser().getGuru().getKode() + " - "
								+ Common.getCurrentUser().getGuru().getNama()))
				: pejabat.getGuru().getKode() + " - " + pejabat.getGuru().getNama());
		guru.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Aktif"));
		row.appendChild(aktif = new MyCheckboxConfig());
		aktif.setChecked(pejabat.getAktif() != null && pejabat.getAktif());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(pejabat.getKeterangan() == null ? "" : pejabat.getKeterangan()));
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

	@SuppressWarnings("unchecked")
	public boolean onSave(Event event) throws Exception {
		// if (pegawai.getAttribute("pegawai") == null) {
		// MyMessageboxConfig.show("Nama Pejabat harus diisi", "Peringatan",
		// MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
		// return false;
		// }
		// if (satuanKerja.getAttribute("satuanKerja") == null) {
		// MyMessageboxConfig.show("Satuan Kerja harus diisi", "Peringatan",
		// MyMessageboxConfig.OK,
		// MyMessageboxConfig.INFORMATION);
		// return false;
		// }
		if (jenisJabatan.getSelectedItem() == null) {
			MyMessageboxConfig.show("Jenis Jabatan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		List<Row> rowsFotoGambar = gridGambar.getRows().getChildren();
		for (Row row : rowsFotoGambar) {
			FotoGambarTandaTanganPejabat fotoGambarTandaTanganPejabat = (FotoGambarTandaTanganPejabat) row
					.getAttribute("fotoGambarTandaTanganPejabat");
			if (fotoGambarTandaTanganPejabat.getPejabat() == null) {
				MyMessageboxConfig.show("Gambar harus diisi", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		PejabatDao pejabatDao = DaoFactory.getInstance().getPejabatDao();
		if (pejabat.getId() != null) {
			pejabat = pejabatDao.load(pejabat.getId());

		}

		pejabat.setAktif(aktif.isChecked());
		pejabat.setKeterangan(keterangan.getValue());
		pejabat.setJenisJabatan((JenisJabatan) (jenisJabatan.getSelectedItem() == null ? null
				: jenisJabatan.getSelectedItem().getValue()));
		pejabat.setPegawai((Pegawai) pegawai.getAttribute("pegawai"));
		pejabat.setDosen((Dosen) dosen.getAttribute("dosen"));

		pejabat.setGuru((Guru) guru.getAttribute("guru"));
		pejabat.setJenisPengguna(jenisPengguna.getValue());
		pejabat.setUsernamePengguna(usernamePengguna.getValue());

		// pejabat.setSatuanKerja((SatuanKerja) satuanKerja
		// .getAttribute("satuanKerja"));

		if (pejabat.getId() != null) {
			pejabatDao.update(pejabat);
		} else {
			pejabatDao.save(pejabat);
		}

		Session mysession = StreamingHibernateUtil.getInstance().currentSession();
		try {
			mysession.getTransaction().begin();
			for (Row row : rowsFotoGambar) {
				FotoGambarTandaTanganPejabat fotoGambarTandaTanganPejabat = (FotoGambarTandaTanganPejabat) row
						.getAttribute("fotoGambarTandaTanganPejabat");
				fotoGambarTandaTanganPejabat.setPejabat(pejabat.getId());
				mysession.saveOrUpdate(fotoGambarTandaTanganPejabat);
			}
			mysession.getTransaction().commit();
		} catch (Exception e) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			Common.tampilErrorJikaAdmin(e);
		}

		StreamingHibernateUtil.getInstance().closeSession();

		return true;
	}

	public Criteria initCriteria(boolean order) {

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Pejabat.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));
		if (order)
			criteria.addOrder(Order.desc("id"));

		if (!searchjenis.getValue().trim().isEmpty()) {
			criteria.createAlias("jenisJabatan", "jenisJabatan", Criteria.LEFT_JOIN)
					.add(Restrictions.ilike("jenisJabatan.nama", searchjenis.getValue().trim(), MatchMode.ANYWHERE));
		}

		if (!searchnama.getValue().trim().isEmpty()) {
			criteria.createAlias("pegawai", "pegawai", Criteria.LEFT_JOIN)
					.createAlias("dosen", "dosen", Criteria.LEFT_JOIN).createAlias("guru", "guru", Criteria.LEFT_JOIN)
					.add(Restrictions.or(
							Restrictions.ilike("usernamePengguna", searchnama.getValue().trim(), MatchMode.ANYWHERE),
							Restrictions.or(
									Restrictions.ilike("jenisPengguna", searchnama.getValue().trim(),
											MatchMode.ANYWHERE),
									Restrictions.or(
											Restrictions.ilike("pegawai.nama", searchnama.getValue().trim(),
													MatchMode.ANYWHERE),
											Restrictions.or(
													Restrictions.ilike("guru.nama", searchnama.getValue().trim(),
															MatchMode.ANYWHERE),
													Restrictions.ilike("dosen.nama", searchnama.getValue().trim(),
															MatchMode.ANYWHERE))))));
		}

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Pejabat> pejabat = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(pejabat);
		grid.setRowRenderer(new PejabatRenderer());
		grid.setModelCheckMobile(strset);

	}

}
