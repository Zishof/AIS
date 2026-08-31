package ais.action.master.pmb;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;

import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

import ais.action.master.helper.DetailPaketJurusanHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.JurusanSekolahMahasiswaBaruDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.JenisSekolahMahasiswaBaru;
import ais.database.model.JurusanSekolahMahasiswaBaru;

/**
 * Controller/action ZK untuk jurusan sekolah mahasiswa baru. Tipe ini merupakan titik masuk UI
 * yang menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus
 * oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code JurusanSekolahMahasiswaBaru
 * jurusanSekolahMahasiswaBaru}, {@code MyWindow addWindow}, {@code MyGrid grid}, {@code Combobox
 * searchjenissekolah}, {@code Textbox nama}, {@code Textbox keterangan}, {@code Combobox jenisSekolah}, {@code
 * boolean edit}; inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code init()});
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
public class JurusanSekolahMahasiswaBaruAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7896939206824822505L;
	private JurusanSekolahMahasiswaBaru jurusanSekolahMahasiswaBaru;
	private MyWindow addWindow;
	private MyGrid grid;

	private Combobox searchjenissekolah;
	private Textbox nama;
	private Textbox keterangan;
	private Combobox jenisSekolah;

	private boolean edit = false;
	private boolean delete = false;
	private MyToolbarbuttonConfig add;
	private Textbox kode;

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

		Common.insertCombo(jenisSekolah = new Combobox(), "nama", JenisSekolahMahasiswaBaru.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.insertCombo(searchjenissekolah, "nama", JenisSekolahMahasiswaBaru.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		onSearchDefault(null);
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link JurusanSekolahMahasiswaBaruAction}. Kelas ini menerjemahkan satu
	 * item data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link JurusanSekolahMahasiswaBaruAction} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see JurusanSekolahMahasiswaBaruAction
	 */
	class JurusanSekolahMahasiswaBaruRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final JurusanSekolahMahasiswaBaru jurusanSekolahMahasiswaBaru = (JurusanSekolahMahasiswaBaru) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {
						// DetailSemesterKurikulumHelper
						// detailSemesterKurikulumHelper = new
						// DetailSemesterKurikulumHelper();
						// detailSemesterKurikulumHelper.displayDetailPA(
						// kurikulum, detail, addWindow);

						DetailPaketJurusanHelper detailPaketJurusanHelper = new DetailPaketJurusanHelper();
						detailPaketJurusanHelper.displayDetailPaketJurusan(jurusanSekolahMahasiswaBaru, detail,
								addWindow);
					}
				}
			});
			new Label(jurusanSekolahMahasiswaBaru.getKode()).setParent(arg0);
			new Label(jurusanSekolahMahasiswaBaru.getJenisSekolahMahasiswaBaru().getNama()).setParent(arg0);
			new Label(jurusanSekolahMahasiswaBaru.getNama()).setParent(arg0);
			new Label(jurusanSekolahMahasiswaBaru.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(jurusanSekolahMahasiswaBaru.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jurusanSekolahMahasiswaBaru.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(jurusanSekolahMahasiswaBaru);
				}
			});

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(jurusanSekolahMahasiswaBaru);
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
											JurusanSekolahMahasiswaBaruDao jurusanSekolahMahasiswaBaruDao = DaoFactory
													.getInstance().getJurusanSekolahMahasiswaBaruDao();
											// jurusanSekolahMahasiswaBaruDao.beginTransaction();
											jurusanSekolahMahasiswaBaruDao.delete(
													jurusanSekolahMahasiswaBaruDao.merge(jurusanSekolahMahasiswaBaru));
											// jurusanSekolahMahasiswaBaruDao.commitTransaction();
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
		init(new JurusanSekolahMahasiswaBaru());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(JurusanSekolahMahasiswaBaru jurusanSekolahMahasiswaBaru) {
		this.jurusanSekolahMahasiswaBaru = jurusanSekolahMahasiswaBaru;
		addWindow.setTitle(jurusanSekolahMahasiswaBaru.getId() == null ? "Tambah JurusanSekolahMahasiswaBaru" : "Ubah JurusanSekolahMahasiswaBaru");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode"));
		row.appendChild(kode = new Textbox(jurusanSekolahMahasiswaBaru.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jurusan Sekolah"));
		row.appendChild(nama = new Textbox(
				jurusanSekolahMahasiswaBaru.getNama() == null ? "" : jurusanSekolahMahasiswaBaru.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Sekolah"));
		Common.selectComboItem(jenisSekolah, jurusanSekolahMahasiswaBaru.getJenisSekolahMahasiswaBaru());
		row.appendChild(jenisSekolah);
		jenisSekolah.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(jurusanSekolahMahasiswaBaru.getKeterangan() == null ? ""
				: jurusanSekolahMahasiswaBaru.getKeterangan()));
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
			MyMessageboxConfig.show("Nama Jurusan Sekolah Mahasiswa Baru harus diisi", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		JurusanSekolahMahasiswaBaruDao jurusanSekolahMahasiswaBaruDao = DaoFactory.getInstance()
				.getJurusanSekolahMahasiswaBaruDao();
		if (jurusanSekolahMahasiswaBaru.getId() != null) {
			jurusanSekolahMahasiswaBaru = jurusanSekolahMahasiswaBaruDao.load(jurusanSekolahMahasiswaBaru.getId());

		}

		jurusanSekolahMahasiswaBaru.setNama(nama.getValue());
		jurusanSekolahMahasiswaBaru.setKeterangan(keterangan.getValue());
		jurusanSekolahMahasiswaBaru
				.setJenisSekolahMahasiswaBaru((JenisSekolahMahasiswaBaru) jenisSekolah.getSelectedItem().getValue());
		jurusanSekolahMahasiswaBaru.setKode(kode.getValue().trim());

		// jurusanSekolahMahasiswaBaruDao.beginTransaction();
		if (jurusanSekolahMahasiswaBaru.getId() != null) {
			jurusanSekolahMahasiswaBaruDao.update(jurusanSekolahMahasiswaBaru);
		} else {
			jurusanSekolahMahasiswaBaruDao.save(jurusanSekolahMahasiswaBaru);
		}
		// jurusanSekolahMahasiswaBaruDao.commitTransaction();
		return true;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Session session = HibernateUtil.currentSession();
		List<JurusanSekolahMahasiswaBaru> jurusanSekolahMahasiswaBaru = session
				.createCriteria(JurusanSekolahMahasiswaBaru.class).addOrder(Order.asc("jenisSekolahMahasiswaBaru"))
				.add(searchjenissekolah.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("jenisSekolahMahasiswaBaru", searchjenissekolah.getSelectedItem().getValue()))
				.setMaxResults(Common.MAX_RESULT).list();
		ListModel strset = new SimpleListModel(jurusanSekolahMahasiswaBaru);
		grid.setRowRenderer(new JurusanSekolahMahasiswaBaruRenderer());
		grid.setModelCheckMobile(strset);

	}

	// public Boolean checkNamaAgama() {
	//
	// Integer kotaCount = null;
	// Session session = HibernateUtil.currentSession();
	// kotaCount = ((Number) session.createCriteria(
	// JurusanSekolahMahasiswaBaru.class).setProjection(
	// Projections.rowCount()).add(
	// Restrictions.eq("nama", nama.getValue().trim())).add(
	// this.jurusanSekolahMahasiswaBaru.getId() == null ? Restrictions
	// .sqlRestriction("1=1") : Restrictions.ne("id",
	// this.jurusanSekolahMahasiswaBaru.getId()))
	// .uniqueResult()).intValue();
	//
	// return !kotaCount.equals(0);
	// }

}
