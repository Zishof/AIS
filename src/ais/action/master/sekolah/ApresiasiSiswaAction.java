package ais.action.master.sekolah;


import ais.action.master.apresiasi.DasbordApresiasi;
import ais.common.CommonSearchFilterHelper;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

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
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
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
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.sekolah.helper.AmbilDataSiswaBanbox;
import ais.action.report.Report;
import ais.action.report.format1.sekolah.LaporanApresiasiSiswa;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Apresiasi;
import ais.database.model.sekolah.ApresiasiDanPenghargaan;
import ais.database.model.sekolah.ApresiasiSiswa;
import ais.database.model.sekolah.Penghargaan;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk apresiasi siswa. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Tabpanel
 * tabDasbor}, {@code Paging paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Combobox
 * searchyayasan}, {@code Combobox searchsekolah}, {@code Textbox keterangan}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code init()}, {@code initCriteria()});
 * pembacaan/pencarian ({@code loadPenghargaan()}, {@code loadApresiasi()}, {@code onSearchDefault()}); mutasi
 * data ({@code onSave()}); pelaporan/ekspor ({@code cetak()}); operasi domain lain ({@code onDasbor()}, {@code
 * onAdd()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class ApresiasiSiswaAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Tabpanel tabDasbor;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Combobox searchyayasan;
	private Combobox searchsekolah;

	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private ApresiasiSiswa apresiasiSiswa;
	private MyToolbarbuttonConfig add;
	private Set<Apresiasi> selectedApresiasi;
	private Set<Penghargaan> selectedPenghargaan;
	private Combobox apresiasiDanPenghargaan;
	private AmbilDataSiswaBanbox siswa;
	private MyDatebox waktu;
	private Combobox ta;

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

		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);

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

		String[] contents = new String[] { "id", "nama", "ta", "sekolah", "keterangan", "aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, ApresiasiSiswa.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
		onDasbor(null);
	}

	public void onDasbor(org.zkoss.zk.ui.event.Event event) {
		if (tabDasbor.getChildren().size() == 0) {
			DasbordApresiasi dasbord = new DasbordApresiasi(DasbordApresiasi.Lingkup.SISWA);
			ais.ui.util.BaseDasbordPortal.mountWrapped(dasbord, tabDasbor,
				"Apresiasi Siswa",
				"Apresiasi dan penghargaan yang diperoleh siswa.");
		}
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link ApresiasiSiswaAction}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link ApresiasiSiswaAction} dan dapat mengakses
	 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see ApresiasiSiswaAction
	 */
	class ApresiasiSiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final ApresiasiSiswa apresiasiSiswa = (ApresiasiSiswa) arg1;

			Hbox hbox = new Hbox();
			hbox.setParent(arg0);
			CommonMedia.tampilkanGambarKecil(apresiasiSiswa.getSiswa()).setParent(hbox);
			Vbox vbox = new Vbox();
			vbox.setParent(hbox);
			vbox.appendChild(new Label(apresiasiSiswa.getSiswa().getNomorInduk()));
			vbox.appendChild(new Label(apresiasiSiswa.getSiswa().getNamaSiswa()));
			vbox.appendChild(new Label(apresiasiSiswa.getSiswa().getSekolah().getNama()));

			RevisiHelper.createNewRevisi(ApresiasiSiswa.class, apresiasiSiswa,
					apresiasiSiswa.getApresiasiDanPenghargaan().getNama()).setParent(arg0);
			new Label(Common.dateFormat5.get().format(apresiasiSiswa.getWaktu())).setParent(arg0);

			new Label(apresiasiSiswa.getTa()).setParent(arg0);
			new Label(apresiasiSiswa.getKeterangan()).setParent(arg0);

			vbox = new Vbox();
			vbox.setParent(arg0);
			int i = 1;
			for (Apresiasi apresiasi : new TreeSet<Apresiasi>(apresiasiSiswa.getApresiasis())) {
				vbox.appendChild(new MyLabelAgakKecil(i + ". " + apresiasi.getNama()));
				i++;
			}

			vbox = new Vbox();
			vbox.setParent(arg0);
			i = 1;
			for (Penghargaan penghargaan : new TreeSet<Penghargaan>(apresiasiSiswa.getPenghargaans())) {
				vbox.appendChild(new MyLabelAgakKecil(i + ". " + penghargaan.getNama()));
				i++;
			}

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(apresiasiSiswa.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					apresiasiSiswa.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(apresiasiSiswa);
				}
			});

			Hbox aa;
			(aa = Common.copyEditDeleteButtons(edit, delete, apresiasiSiswa, ApresiasiSiswaAction.this))
					.setParent(arg0);
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak Apresiasi Siswa");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {
					cetak(apresiasiSiswa);
				}

			});
			button.setParent(aa);

		}

	}

	@SuppressWarnings({})
	public static void cetak(ApresiasiSiswa apresiasiSiswa) throws Exception {
		Report.generatePDFReport(Report.PDF, LaporanApresiasiSiswa.generateParameter(apresiasiSiswa),
				"sekolah/kartu_apresiasi", apresiasiSiswa.getWaktu());
	}

	public void onAdd(Event event) throws Exception {
		init(new ApresiasiSiswa());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		apresiasiSiswa = (ApresiasiSiswa) obj;
		init(apresiasiSiswa);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings({ "deprecation" })
	private void init(ApresiasiSiswa apresiasiSiswa) {
		this.apresiasiSiswa = apresiasiSiswa;
		addWindow.setTitle(apresiasiSiswa.getId() == null ? "Tambah Apresiasi Siswa" : "Ubah Apresiasi Siswa");
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

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pilih Siswa *"));
		row.appendChild(siswa = new AmbilDataSiswaBanbox());
		siswa.setAttribute("siswa", apresiasiSiswa.getSiswa());
		siswa.setValue(apresiasiSiswa.getSiswa() == null ? "" : apresiasiSiswa.getSiswa().getNamaSiswa());
		siswa.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal dan Waktu Apresiasi *"));
		row.appendChild(waktu = new MyDatebox(apresiasiSiswa.getWaktu()));
		waktu.setFormat(Common.dateFormat3.get().toPattern());
		waktu.setWidth("90%");
		waktu.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Ajaran *"));
		row.appendChild(ta = new Combobox());
		ta.setWidth("90%");
		ta.setReadonly(true);
		Common.generateTahunAjaran(ta);
		Common.selectComboItem(ta, apresiasiSiswa.getTa());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(apresiasiSiswa.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Apresiasi *"));
		row.appendChild(apresiasiDanPenghargaan = new Combobox());
		Common.insertCombo(apresiasiDanPenghargaan, "nama", ApresiasiDanPenghargaan.class);
		Common.selectComboItem(apresiasiDanPenghargaan, apresiasiSiswa.getApresiasiDanPenghargaan());
		apresiasiDanPenghargaan.setWidth("90%");
		apresiasiDanPenghargaan.setReadonly(true);

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);
		final MyGrid subGrid = new MyGrid();
		row.appendChild(subGrid);

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);
		final MyGrid subGridH = new MyGrid();
		row.appendChild(subGridH);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadApresiasi(subGrid);
				loadPenghargaan(subGridH);
			}
		};

		apresiasiDanPenghargaan.addEventListener("onChange", eventListener);

		Common.createDefaultTimer(eventListener);

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

	private void loadPenghargaan(MyGrid subGrid) {

		Common.clear(subGrid);

		Columns subColumns = new Columns();
		subColumns.setParent(subGrid);
		subColumns.appendChild(new Column("Penghargaan"));

		ApresiasiDanPenghargaan apresiasiDanPenghargaan = (ApresiasiDanPenghargaan) (this.apresiasiDanPenghargaan
				.getSelectedItem() == null ? null : this.apresiasiDanPenghargaan.getSelectedItem().getValue());

		if (apresiasiDanPenghargaan == null) {

			Rows subRows = new Rows();
			subRows.setParent(subGrid);

			Common.initKeteranganSatuKolom(subRows, "* Jenis apresiasi harus dipilih");

			return;
		}

		Rows subRows = new Rows();
		subRows.setParent(subGrid);

		MyFormRow subRow = new MyFormRow();
		subRow.setStyle("border:0px;background: transparent;");
		subRow.setParent(subRows);
		subRow.setValign("top");

		HibernateUtil.currentSession().refresh(apresiasiDanPenghargaan);

		Set<Penghargaan> penghargaans = apresiasiDanPenghargaan.getPenghargaans();

		if (apresiasiSiswa.getId() != null) {
			HibernateUtil.currentSession().refresh(this.apresiasiSiswa);
		}
		selectedPenghargaan = this.apresiasiSiswa.getPenghargaans();

		Vbox vboxSkala = new Vbox();
		vboxSkala.setPack("top");
		vboxSkala.setParent(subRow);
		for (final Penghargaan penghargaan : penghargaans) {
			final Checkbox checkbox = new Checkbox(penghargaan.getNama() + (penghargaan.getPoin() > 0.1
					? ", pengurangan poin : " + Common.numberFormat.get().format(penghargaan.getPoin())
					: ""));
			checkbox.setParent(vboxSkala);
			checkbox.setChecked(selectedPenghargaan.contains(penghargaan));
			checkbox.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						selectedPenghargaan.add(penghargaan);
					} else {
						selectedPenghargaan.remove(penghargaan);
					}
				}
			});
		}

	}

	private void loadApresiasi(MyGrid subGrid) {
		Common.clear(subGrid);

		Columns subColumns = new Columns();
		subColumns.setParent(subGrid);
		subColumns.appendChild(new Column("Apresiasi"));

		ApresiasiDanPenghargaan apresiasiDanPenghargaan = (ApresiasiDanPenghargaan) (this.apresiasiDanPenghargaan
				.getSelectedItem() == null ? null : this.apresiasiDanPenghargaan.getSelectedItem().getValue());

		if (apresiasiDanPenghargaan == null) {

			Rows subRows = new Rows();
			subRows.setParent(subGrid);

			Common.initKeteranganSatuKolom(subRows, "* Jenis apresiasi harus dipilih");

			return;
		}

		Rows subRows = new Rows();
		subRows.setParent(subGrid);

		MyFormRow subRow = new MyFormRow();
		subRow.setStyle("border:0px;background: transparent;");
		subRow.setParent(subRows);
		subRow.setValign("top");

		HibernateUtil.currentSession().refresh(apresiasiDanPenghargaan);

		Set<Apresiasi> apresiasis = apresiasiDanPenghargaan.getApresiasis();

		if (apresiasiSiswa.getId() != null) {
			HibernateUtil.currentSession().refresh(this.apresiasiSiswa);
		}
		selectedApresiasi = this.apresiasiSiswa.getApresiasis();

		Vbox vboxSkala = new Vbox();
		vboxSkala.setPack("top");
		vboxSkala.setParent(subRow);
		for (final Apresiasi apresiasi : apresiasis) {
			final Checkbox checkbox = new Checkbox(apresiasi.getNama());
			checkbox.setParent(vboxSkala);
			checkbox.setChecked(selectedApresiasi.contains(apresiasi));
			checkbox.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						selectedApresiasi.add(apresiasi);
					} else {
						selectedApresiasi.remove(apresiasi);
					}
				}
			});
		}

	}

	public boolean onSave(Event event) throws Exception {
		if (siswa.getAttribute("siswa") == null) {
			MyMessageboxConfig.show("Siswa harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (apresiasiDanPenghargaan.getSelectedItem() == null
				|| apresiasiDanPenghargaan.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Jenis apresiasi harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (apresiasiSiswa.getId() != null) {
			apresiasiSiswa = (ApresiasiSiswa) session.load(ApresiasiSiswa.class, apresiasiSiswa.getId());

		}

		apresiasiSiswa.setSiswa((Siswa) siswa.getAttribute("siswa"));
		apresiasiSiswa.setApresiasiDanPenghargaan(
				(ApresiasiDanPenghargaan) apresiasiDanPenghargaan.getSelectedItem().getValue());
		apresiasiSiswa.setKeterangan(keterangan.getValue());
		apresiasiSiswa.setApresiasis(selectedApresiasi);
		apresiasiSiswa.setPenghargaans(selectedPenghargaan);

		apresiasiSiswa.setTa((String) ta.getSelectedItem().getValue());

		Common.refreshSaveOrUpdate(session, apresiasiSiswa);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(ApresiasiSiswa.class).createAlias("siswa", "siswa");
		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.getOrangTua() != null && !tbmuser.getOrangTua().ambilAnakSiswa().isEmpty()) {
			criteria.add(Restrictions.in("siswa.id", tbmuser.getOrangTua().ambilAnakSiswa()));
		}
		if (order)
			criteria.addOrder(Order.desc("waktu"));

		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("siswa.nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
						|| searchsekolah.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false))

				.add(searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
						|| searchyayasan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<ApresiasiSiswa> apresiasiSiswa = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(apresiasiSiswa);
		grid.setRowRenderer(new ApresiasiSiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

}
