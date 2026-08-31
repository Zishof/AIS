package ais.action.master.library;


import ais.common.CommonSearchFilterHelper;
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

import ais.action.master.asset.LokasiAction;
import ais.action.master.asset.helper.PustakawanAction;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.library.PerpustakaanDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Tbmuser;
import ais.database.model.asset.Lokasi;
import ais.database.model.library.Perpustakaan;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk perpustakaan. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Textbox searchkode}, {@code Combobox
 * searchfakultas}, {@code Combobox searchjurusan}, {@code Combobox searchlokasi}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code initCriteria()}); pembacaan/pencarian
 * ({@code onSearchDefault()}); validasi/perhitungan ({@code checkKodePerpustakaan()}, {@code
 * checkNamaPerpustakaan()}); mutasi data ({@code onSave()}); operasi domain lain ({@code onAdd()}). Bagian lain
 * dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class PerpustakaanAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchkode;
	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private Combobox searchlokasi;
	private Checkbox searchaktif;

	private Textbox kode;
	private Textbox nama;
	private Combobox lokasi;
	// private Intbox maxPinjam;
	private Textbox keterangan;

	private Lokasi selectedLokasi;

	private Combobox jurusan;
	private Combobox fakultas;
	// private Combobox perguruanTinggi;

	private boolean edit = false;
	private boolean delete = false;

	private Perpustakaan perpustakaan;
	private MyToolbarbuttonConfig add;
	private AmbilDataSatuanKerjaBanbox satuanKerja;
	private boolean pt;
	private boolean ya;
	private Combobox yayasan;
	private Combobox sekolah;
	private Tbmuser tbmuser;

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

		boolean[] ptYa = Common.chekPtAtauSekolah();
		pt = ptYa[0];
		ya = ptYa[1];

		Common.insertCombo(searchlokasi, new String[] { "nama" }, "alamat", Lokasi.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		if (session.getAttribute("LokasiPerpustakaan") != null) {
			selectedLokasi = (Lokasi) session.getAttribute("LokasiPerpustakaan");
			Common.selectComboItem(searchlokasi, session.getAttribute("LokasiPerpustakaan"));
			searchlokasi.setDisabled(true);
			session.removeAttribute("LokasiPerpustakaan");
		}

		LokasiAction.kunciLokasi(searchlokasi);

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

	}

	class PerpustakaanRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Perpustakaan perpustakaan = (Perpustakaan) arg1;

			new PustakawanAction(perpustakaan).setParent(arg0);

			new Label(perpustakaan.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(Perpustakaan.class, perpustakaan, perpustakaan.getNama()).setParent(arg0);
			new Label(perpustakaan.getFakultas() == null
					? (perpustakaan.getYayasan() == null ? "" : perpustakaan.getYayasan().getNama())
					: perpustakaan.getFakultas().getNama()).setParent(arg0);
			new Label(perpustakaan.getJurusan() == null
					? (perpustakaan.getSekolah() == null ? "" : perpustakaan.getSekolah().getNama())
					: perpustakaan.getJurusan().getNama()).setParent(arg0);

			new Label(perpustakaan.getSatuanKerja() == null ? "" : perpustakaan.getSatuanKerja().getNama())
					.setParent(arg0);

			new Label(perpustakaan.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(!Boolean.FALSE.equals(perpustakaan.getAktif()));
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					perpustakaan.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(perpustakaan);
				}
			});

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(perpustakaan);
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

											Common.refreshDelete(perpustakaan);

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

	public void onAdd(Event event) throws Exception {
		init(new Perpustakaan());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(Perpustakaan perpustakaan) throws Exception {
		tbmuser = Common.getCurrentUser();
		fakultas = new Combobox();
		jurusan = new Combobox();
		Common.initFakultasDanJurusan(fakultas, jurusan, searchfakultas, searchjurusan);

		this.perpustakaan = perpustakaan;
		addWindow.setTitle(perpustakaan.getId() == null ? "Tambah Perpustakaan" : "Ubah Perpustakaan");
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

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Perpustakaan"));
		row.appendChild(kode = new Textbox(perpustakaan.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Perpustakaan"));
		row.appendChild(nama = new Textbox(perpustakaan.getNama() == null ? "" : perpustakaan.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Lokasi"));
		row.appendChild(lokasi = new Combobox());
		Common.insertComboDanSemua(lokasi, new String[] { "nama" }, "alamat", Lokasi.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(lokasi, perpustakaan.getLokasi());
		lokasi.setWidth("90%");

		if (selectedLokasi != null) {
			Common.selectComboItem(lokasi, selectedLokasi);
			lokasi.setDisabled(true);
		}

		LokasiAction.kunciLokasi(lokasi);

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		Common.insertCombo(fakultas, new String[] { "nama", "kode" }, Fakultas.class, Restrictions.eq("aktif", true));
		Common.selectComboItem(fakultas,
				perpustakaan.getFakultas() == null || perpustakaan.getFakultas() == null
						? Common.getCurrentUser().ambilFakultas()
						: perpustakaan.getFakultas());
		row.appendChild(fakultas);
		fakultas.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		Common.clear(jurusan);
		Common.insertCombo(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
				Restrictions.eq("fakultas",
						fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? null
								: fakultas.getSelectedItem().getValue()));
		Common.pilihJurusan(jurusan,
				perpustakaan.getJurusan() == null ? Common.getCurrentUser().ambilJurusan() : perpustakaan.getJurusan());
		row.appendChild(jurusan);
		jurusan.setWidth("90%");

		yayasan = new Combobox();
		sekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		row = new MyFormRow();
		row.setVisible(ya);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));

		Common.selectComboItem(yayasan,
				perpustakaan == null || perpustakaan.getYayasan() == null ? tbmuser.ambilYayasan()
						: perpustakaan.getYayasan());
		row.appendChild(yayasan);
		yayasan.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(ya);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah"));

		Common.pilihSekolah(sekolah,
				perpustakaan == null || perpustakaan.getSekolah() == null ? tbmuser.ambilSekolah()
						: perpustakaan.getSekolah());
		row.appendChild(sekolah);
		sekolah.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		row.appendChild(satuanKerja = new AmbilDataSatuanKerjaBanbox(true));
		satuanKerja.setValue(perpustakaan.getSatuanKerja() == null
				? (Common.getCurrentUser().ambilSatuanKerja() == null ? ""
						: Common.getCurrentUser().ambilSatuanKerja().toString())
				: perpustakaan.getSatuanKerja().toString());
		satuanKerja.setAttribute("satuanKerja",
				perpustakaan.getSatuanKerja() == null ? Common.getCurrentUser().ambilSatuanKerja()
						: perpustakaan.getSatuanKerja());
		satuanKerja.setWidth("90%");

		// row = new MyFormRow();
		//		// row.setParent(rows);
		// row.appendChild(new ais.ui.util.MyLabelConfig("Jumlah Maksimal
		// Peminjaman"));
		// row.appendChild(maxPinjam = new Intbox(perpustakaan.getMaxPinjam()));
		// maxPinjam.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(
				keterangan = new Textbox(perpustakaan.getKeterangan() == null ? "" : perpustakaan.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);
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

		if (kode.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Kode Perpustakaan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Nama Perpustakaan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		// if (satuanKerja.getAttribute("satuanKerja") == null) {
		// MyMessageboxConfig.show("Satuan Kerja harus diisi", "Peringatan",
		// MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
		// return false;
		// }

		boolean i = checkKodePerpustakaan();
		if (i) {
			MyMessageboxConfig.show("Kode Perpustakaan sudah ada di database", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		i = checkNamaPerpustakaan();
		if (i) {
			MyMessageboxConfig.show("Nama Perpustakaan sudah ada di database", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		PerpustakaanDao perpustakaanDao = DaoFactory.getInstance().getPerpustakaanDao();
		if (perpustakaan.getId() != null) {
			perpustakaan = perpustakaanDao.load(perpustakaan.getId());

		}
		// perpustakaan.setMaxPinjam(maxPinjam.getValue());

		perpustakaan.setFakultas(
				(Fakultas) (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? null
						: fakultas.getSelectedItem().getValue()));
		perpustakaan.setJurusan(
				(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? null
						: jurusan.getSelectedItem().getValue()));
		perpustakaan.setKode(kode.getValue());
		perpustakaan.setNama(nama.getValue());
		perpustakaan.setKeterangan(keterangan.getValue());
		perpustakaan.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));
		perpustakaan.setLokasi(
				(Lokasi) (lokasi.getSelectedItem() == null || lokasi.getSelectedItem().getValue() == null ? null
						: lokasi.getSelectedItem().getValue()));

		perpustakaan.setYayasan(
				(Yayasan) (yayasan.getSelectedItem() == null ? null : yayasan.getSelectedItem().getValue()));
		perpustakaan.setSekolah(
				(Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue()));
		if (perpustakaan.getAktif() == null) {
			perpustakaan.setAktif(true);
		}

		if (perpustakaan.getId() != null) {
			perpustakaanDao.update(perpustakaan);
		} else {
			perpustakaanDao.save(perpustakaan);
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {

		// SatuanKerja parent = (SatuanKerja) searchparent
		// .getAttribute("satuanKerja");
		// Set<SatuanKerja> satuanKerjas =
		// ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		// if (parent != null) {
		// satuanKerjas.clear(); satuanKerjas.add(parent);
		// satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		// }

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Perpustakaan.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));
		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchlokasi.getSelectedItem() == null || searchlokasi.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("lokasi", searchlokasi.getSelectedItem().getValue()))

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false));

		// .add(satuanKerjas.size() == 0 ? Restrictions
		// .sqlRestriction("1=1") : Restrictions.in("satuanKerja",
		// satuanKerjas));
		criteria.add(searchkode == null || searchkode.getValue().trim().isEmpty()
		        ? Restrictions.sqlRestriction("true")
		        : Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Perpustakaan> perpustakaan = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(perpustakaan);
		grid.setRowRenderer(new PerpustakaanRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkKodePerpustakaan() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(Perpustakaan.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("kode", kode.getValue().trim()))
				.add(this.perpustakaan.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.perpustakaan.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

	public Boolean checkNamaPerpustakaan() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(Perpustakaan.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.perpustakaan.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.perpustakaan.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
