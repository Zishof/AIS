package ais.action.master.sekolah;


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
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import ais.ui.util.MyInclude;
import org.zkoss.zul.Intbox;
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

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.AngketPenilaianGuru;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk angket penilaian guru. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchkode}, {@code Textbox searchnama}, {@code Combobox
 * searchsekolah}, {@code Combobox searchprogram}, {@code Combobox searchyayasan}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code initCriteria()}); pembacaan/pencarian
 * ({@code onSearchDefault()}); validasi/perhitungan ({@code checkNamaAngket()}); mutasi data ({@code onSave()});
 * operasi domain lain ({@code onAngketAngketUmum()}, {@code onAdd()}). Bagian lain dari kontrak tetap mengikuti
 * kelas induk atau interface yang disebut di atas.</p>
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
public class AngketPenilaianGuruAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchkode;
	private Textbox searchnama;
	private Combobox searchsekolah;
	private Combobox searchprogram;
	private Combobox searchyayasan;

	private Textbox kode;
	private Textbox isi;
	private Textbox petunjuk;
	private Intbox jumlahPilihan;
	private Combobox sekolah;
	private Combobox yayasan;
	private Combobox program;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private AngketPenilaianGuru angketPenilaianGuru;
	private MyToolbarbuttonConfig add;

	private Tabpanel grupAngketUmum;
	private Textbox angkatan;

	public void onAngketAngketUmum(Event event) {
		if (grupAngketUmum.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(grupAngketUmum);
			MyInclude iframe = new MyInclude("/pages/master/angket_penilaian_umum.zul");
			iframe.setParent(window);
		}
	}

	public static String[] contents = new String[] { "id", "kode", "petunjuk", "isi", "jumlahPilihan", "yayasan",
			"sekolah", "program", "untukSiswa", "untukGuru", "keterangan" };

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

		Session session = HibernateUtil.currentSession();
		int count = ((Number) session.createCriteria(AngketPenilaianGuru.class).setProjection(Projections.rowCount())
				.uniqueResult()).intValue();
		if (count == 0) {
			AngketPenilaianGuru angket = new AngketPenilaianGuru();
			angket.setKode("001.000");
			angket.setIsi("EVALUASI PENILAIAN PEMBELAJARAN");
			Common.refreshSaveOrUpdate(session, angket);
		}

		Common.initPrograms(searchprogram);

		yayasan = new Combobox();
		sekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, searchyayasan, searchsekolah);

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

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(AngketPenilaianGuru.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, AngketPenilaianGuru.class, contents);
		Common.appendKeToolbar(upload, add, comp);
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link AngketPenilaianGuruAction}. Kelas ini menerjemahkan satu item
	 * data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link AngketPenilaianGuruAction} dan dapat mengakses
	 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see AngketPenilaianGuruAction
	 */
	class AngketPenilaianGuruRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final AngketPenilaianGuru angketPenilaianGuru = (AngketPenilaianGuru) arg1;

			new Label(angketPenilaianGuru.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(AngketPenilaianGuru.class, angketPenilaianGuru, angketPenilaianGuru.getIsi())
					.setParent(arg0);
			new Label(Common.numberFormat.get().format(angketPenilaianGuru.getJumlahPilihan())).setParent(arg0);

			new Label(angketPenilaianGuru.getYayasan() == null ? "Semua" : angketPenilaianGuru.getYayasan().getNama())
					.setParent(arg0);
			new Label(angketPenilaianGuru.getSekolah() == null ? "Semua" : angketPenilaianGuru.getSekolah().getNama())
					.setParent(arg0);

			new Label(angketPenilaianGuru.getProgram() == null || angketPenilaianGuru.getProgram().trim().isEmpty()
					? "Semua"
					: angketPenilaianGuru.getProgram()).setParent(arg0);

			new Label(angketPenilaianGuru.getAngkatan() == null || angketPenilaianGuru.getAngkatan().trim().isEmpty()
					? "Semua"
					: angketPenilaianGuru.getAngkatan()).setParent(arg0);

			final MyCheckboxConfig untukSiswa = new MyCheckboxConfig("Untuk Siswa");
			untukSiswa.setDisabled(!edit);
			untukSiswa.setChecked(angketPenilaianGuru.getUntukSiswa());
			untukSiswa.setParent(arg0);
			untukSiswa.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					angketPenilaianGuru.setUntukSiswa(untukSiswa.isChecked());
					Common.refreshSaveOrUpdate(angketPenilaianGuru);
				}
			});

			final MyCheckboxConfig untukGuru = new MyCheckboxConfig("Untuk Guru");
			untukGuru.setDisabled(!edit);
			untukGuru.setChecked(angketPenilaianGuru.getUntukGuru());
			untukGuru.setParent(arg0);
			untukGuru.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					angketPenilaianGuru.setUntukGuru(untukGuru.isChecked());
					Common.refreshSaveOrUpdate(angketPenilaianGuru);
				}
			});

			new Label(angketPenilaianGuru.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(angketPenilaianGuru);
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

											Common.refreshDelete(angketPenilaianGuru);

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
		init(new AngketPenilaianGuru());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(AngketPenilaianGuru angketPenilaianGuru) {
		this.angketPenilaianGuru = angketPenilaianGuru;
		addWindow.setTitle(angketPenilaianGuru.getId() == null ? "Tambah Angket Penilaian Guru" : "Ubah Angket Penilaian Guru");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Angket"));
		row.appendChild(kode = new Textbox(angketPenilaianGuru.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Angket"));
		row.appendChild(isi = new Textbox(angketPenilaianGuru.getIsi() == null ? "" : angketPenilaianGuru.getIsi()));
		isi.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Petunjuk"));
		row.appendChild(petunjuk = new Textbox(angketPenilaianGuru.getPetunjuk()));
		petunjuk.setWidth("90%");
		petunjuk.setRows(7);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jumlah Pilihan"));
		row.appendChild(jumlahPilihan = new Intbox(angketPenilaianGuru.getJumlahPilihan()));
		jumlahPilihan.setWidth("90%");

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.ambilSekolah() != null) {
			angketPenilaianGuru.setSekolah(tbmuser.ambilSekolah());
		}
		if (tbmuser != null && tbmuser.ambilYayasan() != null) {
			angketPenilaianGuru.setYayasan(tbmuser.ambilYayasan());
		}
		if (tbmuser != null && tbmuser.ambilYayasan() != null) {
			angketPenilaianGuru.setProgram(tbmuser.ambilProgram() == null ? "" : tbmuser.ambilProgram().getNama());
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));
		Common.insertCombo(yayasan = new Combobox(), new String[] { "nama", "kode" }, Yayasan.class,
				Restrictions.eq("aktif", true));
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel("semua");
		comboitem.setValue(null);
		yayasan.appendChild(comboitem);
		Common.selectComboItem(yayasan, angketPenilaianGuru.getYayasan());
		row.appendChild(yayasan);
		yayasan.setWidth("90%");

		Common.initKeterangan(rows, "(Kosongkan " + Common.getBahasaConfig("Yayasan")
				+ " jika angket ini berlaku untuk semua " + Common.getBahasaConfig("Yayasan") + ")");

		if (yayasan.getSelectedItem() != null && yayasan.getSelectedItem().getValue() != null) {
			Common.insertComboDanSemua(sekolah, new String[] { "nama", "kodeEpsbed" }, "jenjang", Sekolah.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					CommonSearchFilterHelper.eqSelectedWithId("yayasan", yayasan, false));
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		Common.selectComboItem(sekolah = new Combobox(),
				angketPenilaianGuru.getSekolah() == null ? tbmuser.ambilSekolah() : angketPenilaianGuru.getSekolah());
		row.appendChild(sekolah);
		sekolah.setWidth("90%");

		Common.initKeterangan(rows, "(Kosongkan " + Common.getBahasaConfig("Prodi")
				+ " jika angket ini berlaku untuk semua " + Common.getBahasaConfig("Prodi") + ")");

		program = Common.initPrograms(program);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		Common.selectComboItem(program, angketPenilaianGuru.getProgram());
		row.appendChild(program);
		program.setWidth("90%");

		Common.initKeterangan(rows, "(Kosongkan program jika angket ini berlaku untuk semua program)");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Angkatan"));
		row.appendChild(angkatan = new Textbox(angketPenilaianGuru.getAngkatan()));
		angkatan.setWidth("90%");

		Common.initKeterangan(rows,
				"(Kosongkan tahun angkatan jika angket ini berlaku untuk semua tahun angkatan, jika terdapat banyak tahun angkatan, masukkan tahun angkatan yang dipisahkan koma, contoh 2017,2018,2019");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(
				angketPenilaianGuru.getKeterangan() == null ? "" : angketPenilaianGuru.getKeterangan()));
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
		if (kode.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Kode Angket harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (isi.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Nama Angket harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		boolean i = checkNamaAngket();
		if (i) {
			MyMessageboxConfig.show("Kode Angket sudah ada di database", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (angketPenilaianGuru.getId() != null) {
			angketPenilaianGuru = (AngketPenilaianGuru) session.load(AngketPenilaianGuru.class,
					angketPenilaianGuru.getId());

		}

		angketPenilaianGuru.setAngkatan(angkatan.getValue().trim());
		angketPenilaianGuru.setKode(kode.getValue());
		angketPenilaianGuru.setIsi(isi.getValue());
		angketPenilaianGuru.setKeterangan(keterangan.getValue());
		angketPenilaianGuru.setPetunjuk(petunjuk.getValue());
		angketPenilaianGuru.setJumlahPilihan(jumlahPilihan.getValue());

		angketPenilaianGuru.setSekolah(
				(Sekolah) (sekolah.getSelectedItem() == null || sekolah.getSelectedItem().getValue() == null ? null
						: sekolah.getSelectedItem().getValue()));
		angketPenilaianGuru.setYayasan(
				(Yayasan) (yayasan.getSelectedItem() == null || yayasan.getSelectedItem().getValue() == null ? null
						: yayasan.getSelectedItem().getValue()));
		angketPenilaianGuru.setProgram(
				(String) (program.getSelectedItem() == null || program.getSelectedItem().getValue() == null ? null
						: program.getSelectedItem().getValue()));

		Common.refreshSaveOrUpdate(session, angketPenilaianGuru);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(AngketPenilaianGuru.class);

		if (order)
			criteria.addOrder(Order.asc("kode"));

		criteria.add(searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
				? Restrictions.sqlRestriction("1=1")
				: Restrictions.or(Restrictions.isNull("sekolah"),
						CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false)))

				.add(searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("yayasan"),
								CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false)))

				.add(searchprogram.getSelectedItem() == null || searchprogram.getSelectedItem().getValue() == null
						|| searchprogram.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.isNull("program"),
										Restrictions.eq("program", searchprogram.getSelectedItem().getValue())))

				.add(searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("isi", searchnama.getValue().trim(), MatchMode.ANYWHERE));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<AngketPenilaianGuru> angketPenilaianGuru = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(angketPenilaianGuru);
		grid.setRowRenderer(new AngketPenilaianGuruRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkNamaAngket() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(AngketPenilaianGuru.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("kode", kode.getValue().trim()))
				.add(this.angketPenilaianGuru.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.angketPenilaianGuru.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
