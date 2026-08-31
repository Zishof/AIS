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
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import ais.ui.util.MyDetail;
import ais.ui.util.MyGrid;
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

import ais.action.master.helper.DetailKelompokKegiatanKedosenanHelper;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.JenisKelompokKegiatanKedosenan;
import ais.database.model.KelompokKegiatanKedosenan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk kelompok kegiatan kedosenan. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Textbox nama}, {@code Textbox keterangan},
 * {@code Combobox jenisKelompokKegiatanKedosenan}, {@code KelompokKegiatanKedosenan kelompokKegiatanKedosenan};
 * inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code
 * initCriteria()}); pembacaan/pencarian ({@code onSearchDefault()}); validasi/perhitungan ({@code
 * checkNamaKelompokKegiatanKedosenan()}); mutasi data ({@code onSave()}); operasi domain lain ({@code
 * onKelompokAspek()}, {@code onJabatanKegiatanKedosenan()}, {@code onSkalaKegiatanKedosenan()}, {@code
 * onNilaiKegiatanKedosenan()}, {@code onAdd()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau
 * interface yang disebut di atas.</p>
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
public class KelompokKegiatanKedosenanAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox nama;
	private Textbox keterangan;
	private Combobox jenisKelompokKegiatanKedosenan;

	// private boolean edit = false;
	// private boolean delete = false;

	private KelompokKegiatanKedosenan kelompokKegiatanKedosenan;
	private MyToolbarbuttonConfig add;

	private Tabpanel jenisKelompokKegiatanKedosenanTab;

	public void onKelompokAspek(Event event) {
		if (jenisKelompokKegiatanKedosenanTab.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(jenisKelompokKegiatanKedosenanTab);
			MyInclude iframe = new MyInclude("/pages/master/jenis_kelompok_kegiatan_kedosenan.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel jabatanKegiatanKedosenan;

	public void onJabatanKegiatanKedosenan(Event event) {
		if (jabatanKegiatanKedosenan.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(jabatanKegiatanKedosenan);
			MyInclude iframe = new MyInclude("/pages/master/jabatan_kegiatan_kedosenan.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel skalaKegiatanKedosenanTab;
	private Combobox jenis;

	public void onSkalaKegiatanKedosenan(Event event) {
		if (skalaKegiatanKedosenanTab.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(skalaKegiatanKedosenanTab);
			MyInclude iframe = new MyInclude("/pages/master/skala_kegiatan_kedosenan.zul");
			iframe.setParent(window);
		}
	}
	
	private Tabpanel nilaiKegiatanKedosenanTab;

	public void onNilaiKegiatanKedosenan(Event event) {
		if (nilaiKegiatanKedosenanTab.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(nilaiKegiatanKedosenanTab);
			MyInclude iframe = new MyInclude("/pages/master/nilai_kegiatan_kedosenan.zul");
			iframe.setParent(window);
		}
	}

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

		// add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		// add.setTooltiptext("Tambah");

		// edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		// delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "id", "nama", "jenisKelompokKegiatanKedosenan", "nomorUrut",
				"bisaDipilihDosen", "aktif", "keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, KelompokKegiatanKedosenan.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible())); }
		Common.appendKeToolbar(upload, add, comp);
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link KelompokKegiatanKedosenanAction}. Kelas ini menerjemahkan satu
	 * item data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link KelompokKegiatanKedosenanAction} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code DetailKelompokKegiatanKedosenanHelper
	 * detailKelompokKegiatanKedosenanHelper}; operasi lokal: {@code render}(). Aturan bisnis bersama tetap berada
	 * pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see KelompokKegiatanKedosenanAction
	 */
	class KelompokKegiatanKedosenanRenderer extends ais.ui.util.MyRowRenderer {

		private DetailKelompokKegiatanKedosenanHelper detailKelompokKegiatanKedosenanHelper = new DetailKelompokKegiatanKedosenanHelper();

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final KelompokKegiatanKedosenan kelompokKegiatanKedosenan = (KelompokKegiatanKedosenan) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					// detailJenisKegiatanHelper.displayJenisKegiatanDetail(
					// jenisKegiatan, detail, addWindow);
					Common.clear(detail);
					if (detail.isOpen())
						detailKelompokKegiatanKedosenanHelper
								.displayDetailKelompokKegiatanKedosenan(kelompokKegiatanKedosenan, detail);
				}
			});

			RevisiHelper.createNewRevisi(KelompokKegiatanKedosenan.class, kelompokKegiatanKedosenan,
					kelompokKegiatanKedosenan.getNama()).setParent(arg0);
			new Label(kelompokKegiatanKedosenan.getJenisKelompokKegiatanKedosenan() == null ? ""
					: kelompokKegiatanKedosenan.getJenisKelompokKegiatanKedosenan().getNama()).setParent(arg0);

			new Label(kelompokKegiatanKedosenan.getJenis()).setParent(arg0);
			final Intbox nomorUrut = new Intbox(kelompokKegiatanKedosenan.getNomorUrut());
			nomorUrut.setParent(arg0);
			nomorUrut.setWidth("90%");

			nomorUrut.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					kelompokKegiatanKedosenan.setNomorUrut(nomorUrut.getValue());
					Common.refreshUpdate(kelompokKegiatanKedosenan);
				}
			});

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			// checkbox.setDisabled(!edit);
			checkbox.setChecked(kelompokKegiatanKedosenan.getAktif());
			checkbox.setParent(arg0);arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					kelompokKegiatanKedosenan.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(kelompokKegiatanKedosenan);
				}
			});

			final MyCheckboxConfig bisaDipilihDosen = new MyCheckboxConfig("Bisa Dipilih Dosen");
			bisaDipilihDosen.setChecked(kelompokKegiatanKedosenan.getBisaDipilihDosen());
			bisaDipilihDosen.setParent(arg0);
			bisaDipilihDosen.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					kelompokKegiatanKedosenan.setBisaDipilihDosen(bisaDipilihDosen.isChecked());
					Common.refreshSaveOrUpdate(kelompokKegiatanKedosenan);
				}
			});

			new Label(kelompokKegiatanKedosenan.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			// button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(kelompokKegiatanKedosenan);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			// button.setVisible(delete);
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
											Common.refreshDelete(kelompokKegiatanKedosenan);
											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e); 
											MyMessageboxConfig
													.show("Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
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
		init(new KelompokKegiatanKedosenan());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(KelompokKegiatanKedosenan kelompokKegiatanKedosenan) {
		this.kelompokKegiatanKedosenan = kelompokKegiatanKedosenan;
		addWindow.setTitle(kelompokKegiatanKedosenan.getId() == null ? "Tambah Aspek Kegiatan" : "Ubah Aspek Kegiatan");
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
		column.setWidth("40%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Aspek Kegiatan"));
		row.appendChild(nama = new Textbox(kelompokKegiatanKedosenan.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelompok Kegiatan"));
		row.appendChild(jenisKelompokKegiatanKedosenan = new Combobox());
		jenisKelompokKegiatanKedosenan.setWidth("90%");
		Common.insertCombo(jenisKelompokKegiatanKedosenan, "nama", "keterangan", JenisKelompokKegiatanKedosenan.class);
		Common.selectComboItem(jenisKelompokKegiatanKedosenan,
				kelompokKegiatanKedosenan.getJenisKelompokKegiatanKedosenan());
		jenisKelompokKegiatanKedosenan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Bidang Kegiatan Dosen"));
		row.appendChild(jenis = new Combobox());
		jenis.setWidth("90%");

		Comboitem comboitem = new Comboitem(KelompokKegiatanKedosenan.BIDANG_PENDIDIKAN);
		comboitem.setValue(KelompokKegiatanKedosenan.BIDANG_PENDIDIKAN);
		jenis.appendChild(comboitem);

		comboitem = new Comboitem(KelompokKegiatanKedosenan.BIDANG_PENELITIAN);
		comboitem.setValue(KelompokKegiatanKedosenan.BIDANG_PENELITIAN);
		jenis.appendChild(comboitem);

		comboitem = new Comboitem(KelompokKegiatanKedosenan.BIDANG_PENGABDIAN);
		comboitem.setValue(KelompokKegiatanKedosenan.BIDANG_PENGABDIAN);
		jenis.appendChild(comboitem);

		comboitem = new Comboitem(KelompokKegiatanKedosenan.BIDANG_PENUNJANG);
		comboitem.setValue(KelompokKegiatanKedosenan.BIDANG_PENUNJANG);
		jenis.appendChild(comboitem);

		Common.selectComboItem(jenis, kelompokKegiatanKedosenan.getJenis());
		jenis.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(kelompokKegiatanKedosenan.getKeterangan()));
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
			PesanFormalHelper.tampilkanGagal("penyimpanan data Aspek Kegiatan",
					"Kolom Nama Aspek Kegiatan belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama Aspek Kegiatan.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (jenisKelompokKegiatanKedosenan.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Kelompok Aspek",
					"Kolom Kelompok Aspek belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Kelompok Aspek.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		boolean i = checkNamaKelompokKegiatanKedosenan();
		if (i) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Aspek Kegiatan",
					"Nama Aspek Kegiatan sudah terdaftar sebelumnya di database, sehingga tidak dapat disimpan kembali untuk menghindari duplikasi data.",
					new String[] {
							"Gunakan nama aspek kegiatan yang berbeda dari data yang sudah ada.",
							"Periksa kembali daftar data yang sudah tersimpan apabila Bapak/Ibu ragu."
					});
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (kelompokKegiatanKedosenan.getId() != null) {
			kelompokKegiatanKedosenan = (KelompokKegiatanKedosenan) session.load(KelompokKegiatanKedosenan.class,
					kelompokKegiatanKedosenan.getId());

		}

		kelompokKegiatanKedosenan
				.setJenis((String) (jenis.getSelectedItem() == null ? null : jenis.getSelectedItem().getValue()));
		kelompokKegiatanKedosenan.setNama(nama.getValue());
		kelompokKegiatanKedosenan.setKeterangan(keterangan.getValue());
		kelompokKegiatanKedosenan.setJenisKelompokKegiatanKedosenan(
				(JenisKelompokKegiatanKedosenan) jenisKelompokKegiatanKedosenan.getSelectedItem().getValue());

		Common.refreshSaveOrUpdate(session, kelompokKegiatanKedosenan);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(KelompokKegiatanKedosenan.class);

		if (order)
			criteria.addOrder(Order.asc("nomorUrut")).addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<KelompokKegiatanKedosenan> kelompokKegiatanKedosenan = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(kelompokKegiatanKedosenan);
		grid.setRowRenderer(new KelompokKegiatanKedosenanRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkNamaKelompokKegiatanKedosenan() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(KelompokKegiatanKedosenan.class)
				.setProjection(Projections.rowCount()).add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.kelompokKegiatanKedosenan.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.kelompokKegiatanKedosenan.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
