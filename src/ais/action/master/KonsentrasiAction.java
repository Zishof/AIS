package ais.action.master;


import ais.common.CommonSearchFilterHelper;
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

import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.KonsentrasiDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Jurusan;
import ais.database.model.Konsentrasi;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Composer ZK untuk pengelolaan data master Konsentrasi ({@link Konsentrasi}) — bidang
 * peminatan/spesialisasi di bawah suatu {@link Jurusan} (mis. konsentrasi dalam satu program
 * studi). Mengikuti pola CRUD standar action-layer AIS: pencarian dengan filter nama dan jurusan
 * ({@link #initCriteria(boolean)}/{@link #onSearchDefault(Event)}), form tambah/ubah
 * ({@link #onAdd(Event)}) dengan validasi field wajib (nama, jurusan) pada
 * {@link #onSave(Event)}, disimpan lewat {@link KonsentrasiDao}. {@link #doBeforeCompose} menjaga
 * keamanan halaman lebih dulu; {@link #doAfterCompose(Component)} memvalidasi sesi login dan hak
 * baca sebelum melanjutkan inisialisasi (redirect logoff bila tidak valid).
 */
public class KonsentrasiAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {
	private static final long serialVersionUID = 3786091220301468178L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;
	private Textbox searchnama;
	private Textbox nama;
	private Textbox namaEnglish;
	private Konsentrasi konsentrasi;
	private Combobox searchjurusan;
	private Combobox jurusan;
	private Combobox dibukaUntukPMB;
	private MyToolbarbuttonConfig add;
	private boolean edit;
	private boolean delete;

	@Override
	/** Menjalankan pemeriksaan keamanan halaman ({@code Common.doCheckSecurity()}) sebelum komponen ZK di-compose. */
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/** Inisialisasi composer setelah komponen ZK ter-wiring: memvalidasi sesi login dan hak baca (redirect logoff bila tidak valid), lalu menyiapkan toolbar dan pencarian. */
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		dibukaUntukPMB = new Combobox();
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		if (comboitem != null) { comboitem.setLabel("Ya"); }
		if (comboitem != null) { comboitem.setValue(1); }
		dibukaUntukPMB.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel("Tidak"); }
		if (comboitem != null) { comboitem.setValue(0); }
		dibukaUntukPMB.appendChild(comboitem);

		Common.insertCombo(searchjurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "id", "nama", "namaEnglish", "jurusan" };

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, Konsentrasi.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	class KonsentrasiRenderer extends ais.ui.util.MyRowRenderer {
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final Konsentrasi konsentrasi = (Konsentrasi) arg1;

			new Label(konsentrasi.getNama()).setParent(arg0);
			new Label(konsentrasi.getNamaEnglish()).setParent(arg0);
			new Label(konsentrasi.getJurusan().getNama()).setParent(arg0);
			// new Label(konsentrasi.getDibukaUntukPMB() == null ? "tidak"
			// : konsentrasi.getDibukaUntukPMB().equals(1) ? "ya" :
			// "tidak").setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(konsentrasi);
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
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Common.refreshDelete(konsentrasi);
											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show("Data ini tidak dapat dihapus");
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

	/** Handler tombol tambah: membuka form dengan entitas {@link Konsentrasi} baru (kosong). */
	public void onAdd(Event event) throws Exception {
		init(new Konsentrasi());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(Konsentrasi konsentrasi) {
		this.konsentrasi = konsentrasi;
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

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama *"));
		row.appendChild(nama = new Textbox(konsentrasi.getNama() == null ? "" : konsentrasi.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasaConfig("Jurusan") + " *"));
		Common.insertCombo(jurusan = new Combobox(), new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.pilihJurusan(jurusan, konsentrasi.getJurusan());
		row.appendChild(jurusan);
		jurusan.setWidth("90%");
		jurusan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Name In English"));
		row.appendChild(
				namaEnglish = new Textbox(konsentrasi.getNamaEnglish() == null ? "" : konsentrasi.getNamaEnglish()));
		namaEnglish.setWidth("90%");

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

	/**
	 * Memvalidasi (nama dan jurusan wajib isi) dan menyimpan data konsentrasi lewat
	 * {@link KonsentrasiDao}.
	 *
	 * @param event event ZK asal aksi simpan
	 * @return {@code true} bila data berhasil disimpan
	 */
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
		if (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show(Common.getBahasaConfig("Jurusan") + " harus diisi", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		KonsentrasiDao konsentrasiDao = DaoFactory.getInstance().getKonsentrasiDao();
		if (konsentrasi.getId() != null) {
			konsentrasi = konsentrasiDao.load(konsentrasi.getId());
		}
		konsentrasi.setNama(nama.getValue());
		konsentrasi.setJurusan((Jurusan) jurusan.getSelectedItem().getValue());
		konsentrasi.setNamaEnglish(namaEnglish.getValue());
		// konsentrasi.setDibukaUntukPMB((Integer)
		// dibukaUntukPMB.getSelectedItem().getValue());

		// konsentrasiDao.beginTransaction();
		if (konsentrasi.getId() != null) {
			konsentrasiDao.update(konsentrasi);
			// MyMessageboxConfig.show("Konsentrasi berhasil di-update",
			// "Informasi", 1,
			// MyMessageboxConfig.INFORMATION);
		} else {
			konsentrasiDao.save(konsentrasi);
			// MyMessageboxConfig.show("Konsentrasi berhasil disimpan",
			// "Informasi", 1,
			// MyMessageboxConfig.INFORMATION);
		}
		// konsentrasiDao.commitTransaction();
		return true;
	}

	/**
	 * Membangun kueri pencarian konsentrasi, difilter nama dan jurusan.
	 *
	 * @param order {@code true} untuk menyertakan pengurutan hasil
	 * @return kriteria Hibernate siap dieksekusi/dipaginasi
	 */
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Konsentrasi.class);
		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	/** Mengeksekusi ulang pencarian ({@link #initCriteria(boolean)}) untuk halaman aktif dan merender hasilnya ke grid daftar konsentrasi. */
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Konsentrasi> konsentrasi = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(konsentrasi);
		grid.setRowRenderer(new KonsentrasiRenderer());
		grid.setModelCheckMobile(strset);

	}

}
