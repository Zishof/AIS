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
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import ais.ui.util.MyGrid;
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
import ais.action.master.sekolah.helper.DetailAsramaSiswaHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.AsramaSiswa;
import ais.database.model.sekolah.AsramaSiswaPunyaSiswa;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk asrama siswa. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Checkbox searchaktif}, {@code Combobox
 * searchyayasan}, {@code Textbox nama}, {@code Textbox keterangan}; inisialisasi/lifecycle ({@code
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
public class AsramaSiswaAction extends GenericAutowireComposer
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
	private Combobox searchyayasan;

	private Textbox nama;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private AsramaSiswa asramaSiswa;
	private MyToolbarbuttonConfig add;
	private Combobox yayasan;

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

		Common.insertComboDanSemua(searchyayasan, "nama", Yayasan.class,
				Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.ambilYayasan() != null) {
			Common.selectComboItem(searchyayasan, tbmuser.ambilYayasan());
			searchyayasan.setDisabled(true);
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

		String[] contents = new String[] { "id", "nama", "yayasan", "aktif", "keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, AsramaSiswa.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Singkronkan", "/img/new.gif");
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				final ais.common.LaporanUpload laporan = new ais.common.LaporanUpload("Sinkronisasi Asrama Siswa");

				final Label label = Common.displayLoadBar(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						laporan.selesaikan(new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								onSearchDefault(null);
							}
						});
					}
				});

				final List<AsramaSiswa> asramaes = HibernateUtil.currentSession().createCriteria(AsramaSiswa.class)
						.list();
				new Thread(new Runnable() {

					@Override
					public void run() {
						try {

						Session session = HibernateUtil.currentNativeSession();
						int baris = 1;
						for (AsramaSiswa asramaSiswa : asramaes) {
							try {
								Criteria criteria = session.createCriteria(AsramaSiswaPunyaSiswa.class)
										.setProjection(Projections.groupProperty("siswa"))
										.add(Restrictions.eq("asramaSiswa", asramaSiswa));

								List<Siswa> siswas = criteria.list();

								int rowIndex = 1;
								for (Siswa siswa : siswas) {
									label.setValue("Sedang memproses data " + siswa.toString() + " ("
											+ Common.numberFormat.get().format(rowIndex * 100.0 / siswas.size()) + " %)");

									String kunci = asramaSiswa.getNama() + " - " + siswa.toString();
									try {
										siswa.setAsrama(asramaSiswa);

										session.getTransaction().begin();
										Common.refreshUpdate(session, siswa);
										session.getTransaction().commit();
										laporan.catatBerhasil(baris - 1, kunci, "Sinkronisasi berhasil");
									} catch (Exception ePerSiswa) {
										Common.tampilErrorJikaAdmin(ePerSiswa);
										laporan.catatGagalDetail(baris - 1, kunci, ePerSiswa);
									}

									rowIndex++;
									baris++;
								}
								siswas.clear();
							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);
								laporan.tambahCatatan("Asrama " + asramaSiswa.getNama() + " gagal diproses: "
										+ ais.common.LaporanUpload.detailTeknisException(e));
							}
						}
						HibernateUtil.closeSession();
											} finally {
							label.setValue("");
							ais.database.hibernate.HibernateUtil.closeSession();
						}
					}
				}).start();

			}

		});
		Common.appendKeToolbar(button, add, comp);
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link AsramaSiswaAction}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link AsramaSiswaAction} dan dapat mengakses state
	 * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code DetailAsramaSiswaHelper
	 * detailAsramaSiswaHelper}; operasi lokal: {@code render}(). Aturan bisnis bersama tetap berada pada kelas
	 * induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see AsramaSiswaAction
	 */
	class AsramaSiswaRenderer extends ais.ui.util.MyRowRenderer {

		private DetailAsramaSiswaHelper detailAsramaSiswaHelper = new DetailAsramaSiswaHelper();

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final AsramaSiswa asramaSiswa = (AsramaSiswa) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					if (detail.getChildren().isEmpty() && detail.isOpen()) {
						detailAsramaSiswaHelper.displayDetailPA(asramaSiswa, detail, addWindow);
					}

				}

			});

			RevisiHelper.createNewRevisi(AsramaSiswa.class, asramaSiswa, asramaSiswa.getNama()).setParent(arg0);
			new Label(asramaSiswa.getYayasan().getNama()).setParent(arg0);
			new Label(asramaSiswa.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(asramaSiswa.getAktif());
			checkbox.setParent(arg0);arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					asramaSiswa.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(asramaSiswa);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, asramaSiswa, AsramaSiswaAction.this).setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new AsramaSiswa());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		asramaSiswa = (AsramaSiswa) obj;
		init(asramaSiswa);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(AsramaSiswa asramaSiswa) {
		this.asramaSiswa = asramaSiswa;
		addWindow.setTitle(asramaSiswa.getId() == null ? "Tambah Asrama Siswa" : "Ubah Asrama Siswa");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Asrama *"));
		row.appendChild(nama = new Textbox(asramaSiswa.getNama()));
		nama.setWidth("90%");

		yayasan = new Combobox();
		Common.insertComboDanSemua(yayasan, "nama", Yayasan.class,
				Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan *"));
		row.appendChild(yayasan);
		Common.selectComboItem(yayasan, asramaSiswa.getYayasan());
		yayasan.setWidth("90%");
		yayasan.setReadonly(true);

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.ambilYayasan() != null) {
			Common.selectComboItem(yayasan, tbmuser.ambilYayasan());
			yayasan.setDisabled(true);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(asramaSiswa.getKeterangan()));
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
			MyMessageboxConfig.show("Nama Asrama harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (yayasan.getSelectedItem() == null || yayasan.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Yayasan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (asramaSiswa.getId() != null) {
			asramaSiswa = (AsramaSiswa) session.load(AsramaSiswa.class, asramaSiswa.getId());

		}

		asramaSiswa.setNama(nama.getValue());
		asramaSiswa.setYayasan((Yayasan) yayasan.getSelectedItem().getValue());
		asramaSiswa.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, asramaSiswa);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(AsramaSiswa.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
						|| searchyayasan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<AsramaSiswa> asramaSiswa = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(asramaSiswa);
		grid.setRowRenderer(new AsramaSiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

}
