package ais.action.master;

import java.util.List;
import java.util.Set;

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
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.akunting.helper.AmbilDataAkunBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.JenisPembayaranDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Bank;
import ais.database.model.JenisPembayaran;
import ais.database.model.JenisTabungan;
import ais.database.model.Konfigurasi;
import ais.database.model.akunting.Akun;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class JenisPembayaranAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4124140285573733292L;

	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox serachnama;
	private Textbox serachkode;
	private Checkbox searchaktif;
	private AmbilDataSatuanKerjaBanbox searchparent;

	private Textbox nama;
	private Textbox kode;
	private Textbox deskripsi;
	private AmbilDataAkunBanbox akun;

	public JenisPembayaran jenisPembayaran;
	private MyToolbarbuttonConfig add;

	private boolean edit;
	private boolean delete;

	private MyColumnConfig kode_akun;
	private MyColumnConfig nama_akun;

	private Combobox bank;

	private AmbilDataSatuanKerjaBanbox satuanKerja;

	private Combobox jenisTabungan;

	private SatuanKerjaTreeModel satuanKerjaTreeModel;

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

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		}

//		// add.setDisabled((Common.getCurrentUser().getRoot() == null || !Common.getCurrentUser().getRoot()));

		if (!Common.bolehKonfigurasi("integrasi_modul_akuntansi", Konfigurasi.TIDAK_AKTIF)) {
			if (kode_akun != null)
				kode_akun.setWidth("0px");
			if (nama_akun != null)
				nama_akun.setWidth("0px");
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

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	class JenisPembayaranRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final JenisPembayaran jenisPembayaran = (JenisPembayaran) arg1;

			RevisiHelper
					.createNewRevisi(JenisPembayaran.class, jenisPembayaran,
							jenisPembayaran.getKode() == null ? "" : jenisPembayaran.getKode().trim().toString())
					.setParent(arg0);
			new Label(jenisPembayaran.getNama()).setParent(arg0);
			new Label(jenisPembayaran.getDeskripsi()).setParent(arg0);

			new MyLabelAgakKecil(jenisPembayaran.getBank() == null ? "" : jenisPembayaran.getBank().getNama())
					.setParent(arg0);
			new Label(jenisPembayaran.getAkun() == null ? ""
					: jenisPembayaran.getAkun().getKode() + "-" + jenisPembayaran.getAkun().getNama()).setParent(arg0);

			new Label(jenisPembayaran.getJenisTabungan() == null ? "" : jenisPembayaran.getJenisTabungan().getNama())
					.setParent(arg0);

			new Label(jenisPembayaran.getSatuanKerja() == null ? ""
					: jenisPembayaran.getSatuanKerja().getKode() + "-" + jenisPembayaran.getSatuanKerja().getNama())
					.setParent(arg0);

			final MyCheckboxConfig aktif = new MyCheckboxConfig("Aktif");
			aktif.setChecked(jenisPembayaran.getAktif());
			aktif.setParent(arg0);
			aktif.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenisPembayaran.setAktif(aktif.isChecked());
					Common.refreshSaveOrUpdate(jenisPembayaran);
				}
			});

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Default");
			checkbox.setChecked(jenisPembayaran.getDefaultPembayaran());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenisPembayaran.setDefaultPembayaran(checkbox.isChecked());
					Common.refreshSaveOrUpdate(jenisPembayaran);
					
					
					Common.createDefaultTimer(new EventListener() {
						
						@Override
						public void onEvent(Event arg0) throws Exception {
							JenisPembayaran.reloadDefault();
						}
					});
				}
			});

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);

			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) {

					try {
						init(jenisPembayaran);
						addWindow.setVisible(true);
						addWindow.onModal();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						Common.tampilErrorJikaAdmin(e);
					}
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete);
			button.setDisabled((Common.getCurrentUser().getRoot() == null || !Common.getCurrentUser().getRoot()));
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

											Common.refreshDelete(jenisPembayaran);
											
											Common.createDefaultTimer(new EventListener() {
												
												@Override
												public void onEvent(Event arg0) throws Exception {
													JenisPembayaran.reloadDefault();
													
													onSearchDefault(arg0);
												}
											});

											
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
		init(new JenisPembayaran());
		addWindow.setVisible(true);
		try {
			addWindow.onModal();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void init(JenisPembayaran jenisPembayaran) throws Exception {
		addWindow.setTitle(jenisPembayaran.getId() == null ? "Tambah Jenis Pembayaran" : "Ubah Jenis Pembayaran");
		this.jenisPembayaran = jenisPembayaran;
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode"));
		row.appendChild(
				kode = new Textbox((jenisPembayaran.getKode() == null ? "" : jenisPembayaran.getKode().trim())));
		kode.setWidth("90%");
		// kode.setDisabled(Common.getCurrentUser().getRoot() == null || !Common.getCurrentUser().getRoot());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama = new Textbox(jenisPembayaran.getNama() == null ? "" : jenisPembayaran.getNama().trim()));
		nama.setWidth("90%");
		// nama.setDisabled(Common.getCurrentUser().getRoot() == null || !Common.getCurrentUser().getRoot());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Bank"));
		row.appendChild(bank = new Combobox());
		Common.insertComboDanSemua(bank, new String[] { "nama", "nomorRekening", "atasNama" }, "keterangan", Bank.class,
				"== Jenis Pembayaran Bukan Transfer ==", Restrictions.sqlRestriction("true"));
		Common.selectComboItem(bank, jenisPembayaran.getBank());
		bank.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Deskripsi"));
		row.appendChild(
				deskripsi = new Textbox(jenisPembayaran.getDeskripsi() == null ? "" : jenisPembayaran.getDeskripsi()));
		deskripsi.setWidth("90%");
		deskripsi.setRows(5);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Akun"));
		row.appendChild(akun = new AmbilDataAkunBanbox(false));
		akun.setValue(jenisPembayaran.getAkun() == null ? "" : jenisPembayaran.getAkun().getNama());
		akun.setAttribute("akun", jenisPembayaran.getAkun());
		akun.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jika mengambil dari tabungan, pilih jenis tabungan"));

		Common.insertComboDanSemua(jenisTabungan = new Combobox(), new String[] { "nama", "keterangan" }, "akun",
				JenisTabungan.class, "=Bukan Tabungan=",
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		row.appendChild(jenisTabungan);
		Common.selectComboItem(jenisTabungan, jenisPembayaran.getJenisTabungan());
		jenisTabungan.setWidth("90%");
		jenisTabungan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		row.appendChild(satuanKerja = new AmbilDataSatuanKerjaBanbox(false));
		satuanKerja.setValue(jenisPembayaran.getSatuanKerja() == null ? ""
				: jenisPembayaran.getSatuanKerja().getKode() + "-" + jenisPembayaran.getSatuanKerja().getNama());
		satuanKerja.setAttribute("satuanKerja", jenisPembayaran.getSatuanKerja());
		satuanKerja.setWidth("90%");

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

	}

	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Nama",
					"Kolom Nama belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		if (akun.getAttribute("akun") == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Akun",
					"Kolom Akun belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Akun.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		boolean i = checkKode();
		if (i) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Kode Item",
					"Kode Item sudah terdaftar sebelumnya di database, sehingga tidak dapat disimpan kembali untuk menghindari duplikasi data.",
					new String[] {
							"Gunakan Kode Item yang berbeda dari data yang sudah ada.",
							"Periksa kembali daftar data yang sudah tersimpan apabila Bapak/Ibu ragu."
					});
			return false;
		}

		i = checkNama();
		if (i) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Item",
					"Nama Item sudah terdaftar sebelumnya di database, sehingga tidak dapat disimpan kembali untuk menghindari duplikasi data.",
					new String[] {
							"Gunakan nama item yang berbeda dari data yang sudah ada.",
							"Periksa kembali daftar data yang sudah tersimpan apabila Bapak/Ibu ragu."
					});
			return false;
		}

		JenisPembayaranDao jenisPembayaranDao = DaoFactory.getInstance().getJenisPembayaranDao();
		if (jenisPembayaran.getId() != null) {
			jenisPembayaran = jenisPembayaranDao.load(jenisPembayaran.getId());
		}
		jenisPembayaran.setAkun((Akun) akun.getAttribute("akun"));
		jenisPembayaran.setKode(kode.getValue());
		jenisPembayaran.setNama(nama.getValue());
		jenisPembayaran.setDeskripsi(deskripsi.getValue());
		jenisPembayaran.setBank((Bank) (bank.getSelectedItem() == null ? null : bank.getSelectedItem().getValue()));
		jenisPembayaran.setJenisTabungan((JenisTabungan) (jenisTabungan.getSelectedItem() == null ? null
				: jenisTabungan.getSelectedItem().getValue()));
		jenisPembayaran.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));

		if (jenisPembayaran.getId() != null) {
			jenisPembayaranDao.update(jenisPembayaran);
		} else {
			jenisPembayaranDao.save(jenisPembayaran);
		}
	
		Common.createDefaultTimer(new EventListener() {
			
			@Override
			public void onEvent(Event arg0) throws Exception {
				JenisPembayaran.reloadDefault();
			}
		});
		
		return true;
	}

	public Criteria initCriteria(boolean order) {

		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear(); satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(JenisPembayaran.class)

				.add(Restrictions.or(Restrictions.isNull("satuanKerja"),
						satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(parent==null ? Restrictions.isNull("satuanKerja") : Restrictions.sqlRestriction("false"), Restrictions.in("satuanKerja", satuanKerjas))))

				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"))
				.add(serachkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("kode", serachkode.getValue().trim(), MatchMode.ANYWHERE))
				.add(serachnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", serachnama.getValue().trim(), MatchMode.ANYWHERE));
		if (order)
			criteria.addOrder(Order.asc("kode"));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<JenisPembayaran> jenisPembayaran = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(jenisPembayaran);
		grid.setRowRenderer(new JenisPembayaranRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkKode() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(JenisPembayaran.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("kode", kode.getValue().trim()))
				.add(this.jenisPembayaran.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.jenisPembayaran.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

	public Boolean checkNama() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(JenisPembayaran.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.jenisPembayaran.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.jenisPembayaran.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
