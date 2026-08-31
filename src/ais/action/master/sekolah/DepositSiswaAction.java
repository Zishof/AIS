package ais.action.master.sekolah;


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
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
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
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.sekolah.helper.AmbilDataSiswaBanbox;
import ais.action.master.sekolah.util.PembayaranSiswaUtil;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.AkunPembayaranSiswa;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.DepositSiswa;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk deposit siswa. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Combobox searchjenis}, {@code Textbox searchsiswa}, {@code Combobox
 * searchyayasan}, {@code Combobox searchsekolah}, {@code Textbox keterangan}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code init()}, {@code initCriteria()});
 * pembacaan/pencarian ({@code onSearchDefault()}); mutasi data ({@code onSave()}); operasi domain lain ({@code
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
public class DepositSiswaAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Combobox searchjenis;
	private Textbox searchsiswa;

	private Combobox searchyayasan;
	private Combobox searchsekolah;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private DepositSiswa depositSiswa;
	private MyToolbarbuttonConfig add;
	private AmbilDataSiswaBanbox siswa;
	private Combobox akunPembayaranSiswa;
	private MyDoublebox nominal;

	private Siswa selectedSiswa = null;
	private CalonSiswa selectedCalonSiswa = null;

	private Label siswatampil, nimsiswatampil, kelassiswatampil;
	private MyDatebox waktu;

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

		if (ExecutionsCtrl.getCurrent().getParameter("siswa") != null) {
			selectedSiswa = (Siswa) HibernateUtil.currentSession().createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa","")).add(Restrictions.isNotNull("sekolah"))
					.add(Restrictions.idEq(Long.parseLong(ExecutionsCtrl.getCurrent().getParameter("siswa"))))
					.uniqueResult();
		} else if (ExecutionsCtrl.getCurrent().getParameter("calon_siswa") != null) {
			selectedCalonSiswa = (CalonSiswa) HibernateUtil.currentSession().createCriteria(CalonSiswa.class).add(Restrictions.isNotNull("gelombangPendaftaranPsb"))
					.add(Restrictions.idEq(Long.parseLong(ExecutionsCtrl.getCurrent().getParameter("calon_siswa"))))
					.uniqueResult();
		}

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.getSiswa() != null) {
			selectedSiswa = tbmuser.getSiswa();
		} else if (tbmuser != null && tbmuser.getCalonSiswa() != null) {
			selectedCalonSiswa = tbmuser.getCalonSiswa();
		}

		if (selectedSiswa != null) {
			if (siswatampil != null)
				siswatampil.setValue("Nama Siswa : " + (selectedSiswa == null ? "" : selectedSiswa.getNama()));
			if (nimsiswatampil != null)
				nimsiswatampil.setValue("NIS Siswa : " + (selectedSiswa == null ? "" : selectedSiswa.getNim()));
			if (kelassiswatampil != null)
				kelassiswatampil.setValue("Kelas Siswa : " + (selectedSiswa == null ? ""
						: (selectedSiswa.getKelas() == null ? "" : selectedSiswa.getKelas().getNama())));
		}

		Sekolah curr = tbmuser == null ? null : tbmuser.ambilSekolah();
		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);
		Common.insertComboDanSemua(searchjenis, new String[] { "nama", "sekolah" }, "keterangan", AkunPembayaranSiswa.class,

				Restrictions.and(
						curr == null ? Restrictions.sqlRestriction("true")
								: Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", curr)),
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

		);

		add.setVisible(selectedSiswa == null && selectedCalonSiswa == null
				&& CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		if (add != null) { add.setTooltiptext("Tambah"); }

		edit = selectedSiswa == null && selectedCalonSiswa == null
				&& CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = selectedSiswa == null && selectedCalonSiswa == null
				&& CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "id", "siswa", "tanggalBayar", "waktu", "nominal", "pembayaranSiswa",
				"akunPembayaranSiswa", "keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, DepositSiswa.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	        FilterLanjutHelper.setup(comp);
}

	/**
	 * Renderer lokal untuk layar/komponen {@link DepositSiswaAction}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link DepositSiswaAction} dan dapat mengakses state
	 * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see DepositSiswaAction
	 */
	class DepositSiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final DepositSiswa depositSiswa = (DepositSiswa) arg1;
			CommonMedia.tampilkanGambarKecil(depositSiswa.getSiswa()).setParent(arg0);
			new Label(depositSiswa.getSiswa().getNomorInduk()).setParent(arg0);
			Vbox a;
			(a = RevisiHelper.createNewRevisi(DepositSiswa.class, depositSiswa, depositSiswa.getSiswa().getNama()))
					.setParent(arg0);

			if (Common.isMobile()) {
				new Label(("Waktu : ")
						+ (depositSiswa.getWaktu() == null ? "" : Common.dateFormat5.get().format(depositSiswa.getWaktu())))
						.setParent(a);
				new Label(("Nilai : ") + (Common.numberFormat.get().format(depositSiswa.getNominal()))).setParent(a);
				new Label(("Cara : ") + (depositSiswa.getAkunPembayaranSiswa() == null ? ""
						: depositSiswa.getAkunPembayaranSiswa().getNama())).setParent(a);
				new Label(("Keterangan : ") + (depositSiswa.getKeterangan())).setParent(a);
			}

			Vbox hbox = new Vbox();
			hbox.setParent(arg0);

			new Label(depositSiswa.getWaktu() == null ? "" : Common.dateFormat5.get().format(depositSiswa.getWaktu()))
					.setParent(hbox);

			new Label(depositSiswa.getValidator()).setParent(hbox);

			new Label(depositSiswa.getAkunPembayaranSiswa() == null ? ""
					: depositSiswa.getAkunPembayaranSiswa().getNama()).setParent(arg0);

			new Label(depositSiswa.getPembayaranSiswa() == null ? "" : depositSiswa.getPembayaranSiswa().getNama())
					.setParent(arg0);

			new Label(Common.numberFormat.get().format(depositSiswa.getNominal())).setParent(arg0);
			new Label(depositSiswa.getKeterangan()).setParent(arg0);

			if (depositSiswa.getPembayaranSiswa() == null) {

				Hbox toolbar = new Hbox();
				toolbar.setParent(arg0);
				Common.copyEditDeleteButtons(edit, delete, depositSiswa, DepositSiswaAction.this).setParent(toolbar);

				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
				button.setTooltiptext("Cetak Data");
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						PembayaranSiswaUtil.cetakDeposit(depositSiswa);
					}

				});
				button.setParent(toolbar);

			} else {
				Hbox toolbar = new Hbox();
				toolbar.setParent(arg0);

				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
				button.setTooltiptext("Cetak Data");
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						PembayaranSiswaUtil.cetakDeposit(depositSiswa);
					}

				});
				button.setParent(toolbar);
			}
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new DepositSiswa());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		depositSiswa = (DepositSiswa) obj;
		init(depositSiswa);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(final DepositSiswa depositSiswa) throws Exception {
		this.depositSiswa = depositSiswa;
		addWindow.setTitle(depositSiswa.getId() == null ? "Tambah Deposit Siswa" : "Ubah Deposit Siswa");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Pilih Siswa *"));
		row.appendChild(siswa = new AmbilDataSiswaBanbox());
		siswa.setAttribute("siswa", depositSiswa.getSiswa());
		siswa.setValue(depositSiswa.getSiswa() == null ? "" : depositSiswa.getSiswa().getNamaSiswa());
		siswa.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Cara Pembayaran *"));
		row.appendChild(akunPembayaranSiswa = new Combobox());
		akunPembayaranSiswa.setWidth("90%");
		akunPembayaranSiswa.setReadonly(true);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Sekolah s = null;

				Siswa ss = (Siswa) siswa.getAttribute("siswa");
				s = ss == null ? null : ss.getSekolah();

				Common.insertCombo(akunPembayaranSiswa, new String[] { "nama", "akun", "bank" },
						AkunPembayaranSiswa.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));
				Common.selectComboItem(akunPembayaranSiswa, depositSiswa.getAkunPembayaranSiswa());

			}
		};

		siswa.setEventListener(eventListener);
		eventListener.onEvent(null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal dan Waktu Pembayaran *"));
		row.appendChild(waktu = new MyDatebox(depositSiswa.getWaktu()));
		waktu.setFormat(Common.dateFormat3.get().toPattern());
		waktu.setWidth("90%");
		waktu.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nominal Deposit"));
		row.appendChild(nominal = new MyDoublebox(depositSiswa.getNominal()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(depositSiswa.getKeterangan()));
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
		if (siswa.getAttribute("siswa") == null) {
			MyMessageboxConfig.show("Siswa harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (akunPembayaranSiswa.getSelectedItem() == null || akunPembayaranSiswa.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Cara pembayaran harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (depositSiswa.getId() != null) {
			depositSiswa = (DepositSiswa) session.load(DepositSiswa.class, depositSiswa.getId());
		}
		depositSiswa.setTanggalBayar(waktu.getValue());
		depositSiswa.setWaktu(waktu.getValue());
		depositSiswa.setSiswa((Siswa) siswa.getAttribute("siswa"));
		depositSiswa.setNominal(nominal.getValue());
		depositSiswa.setAkunPembayaranSiswa((AkunPembayaranSiswa) akunPembayaranSiswa.getSelectedItem().getValue());
		depositSiswa.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, depositSiswa);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(DepositSiswa.class)
				.add(selectedSiswa == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("siswa", selectedSiswa))
				.add(selectedCalonSiswa == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("calonSiswa", selectedCalonSiswa));

		if (order)
			criteria.addOrder(Order.desc("tanggalBayar"));

		if (!searchsiswa.getValue().trim().isEmpty()) {
			criteria.createAlias("siswa", "siswa", Criteria.LEFT_JOIN)
					.createAlias("calonSiswa", "calonSiswa", Criteria.LEFT_JOIN)

					.add(Restrictions.or(
							Restrictions.ilike("siswa.nomorIndukNasional", searchsiswa.getValue().trim(),
									MatchMode.ANYWHERE),
							Restrictions.or(
									Restrictions.ilike("calonSiswa.nomorIndukNasional", searchsiswa.getValue().trim(),
											MatchMode.ANYWHERE),
									Restrictions.or(
											Restrictions.ilike("calonSiswa.namaSiswa", searchsiswa.getValue().trim(),
													MatchMode.ANYWHERE),
											Restrictions.or(
													Restrictions.ilike("siswa.namaSiswa", searchsiswa.getValue().trim(),
															MatchMode.ANYWHERE),
													Restrictions.or(
															Restrictions.ilike("siswa.nomorInduk",
																	searchsiswa.getValue().trim(), MatchMode.ANYWHERE),
															Restrictions.ilike("calonSiswa.nomorInduk",
																	searchsiswa.getValue().trim(),
																	MatchMode.ANYWHERE)))))));

		}

		criteria.add(searchjenis.getSelectedItem() == null || searchjenis.getSelectedItem().getValue() == null
				? Restrictions.sqlRestriction("true")
				: Restrictions.eq("akunPembayaranSiswa", searchjenis.getSelectedItem().getValue()))

				.add(searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
						|| searchsekolah.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false))

				.add(searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
						|| searchyayasan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false));
		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.getOrangTua() != null && !tbmuser.getOrangTua().ambilAnakSiswa().isEmpty()) {
			criteria.add(Restrictions.in("siswa.id", tbmuser.getOrangTua().ambilAnakSiswa()));
		}
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<DepositSiswa> depositSiswa = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(depositSiswa);
		grid.setRowRenderer(new DepositSiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

}
