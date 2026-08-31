package ais.action.master;

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
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import ais.ui.util.MyGrid;
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

import ais.action.master.helper.BerkasHasilAkreditasiPunyaNamaHelper;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BerkasHasilAkreditasi;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.PerguruanTinggi;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk berkas hasil akreditasi. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Textbox nama}, {@code Textbox asesor1},
 * {@code Textbox asesor2}, {@code MyDatebox tanggal}; inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code
 * doAfterCompose()}, {@code init()}, {@code initCriteria()}); pembacaan/pencarian ({@code onSearchDefault()});
 * mutasi data ({@code onSave()}); operasi domain lain ({@code onAdd()}). Bagian lain dari kontrak tetap
 * mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class BerkasHasilAkreditasiAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox nama;
	private Textbox asesor1;
	private Textbox asesor2;
	private MyDatebox tanggal;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private BerkasHasilAkreditasi berkasHasilAkreditasi;
	private MyToolbarbuttonConfig add;

	private Jurusan jurusan;
	private Fakultas fakultas;
	private PerguruanTinggi perguruanTinggi;

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

		if (Common.isNumber(execution.getParameter("jurusan"))) {
			jurusan = (Jurusan) HibernateUtil.currentSession().createCriteria(Jurusan.class)
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("jurusan").trim()))).uniqueResult();
		}

		if (Common.isNumber(execution.getParameter("fakultas"))) {
			fakultas = (Fakultas) HibernateUtil.currentSession().createCriteria(Fakultas.class)
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("fakultas").trim()))).uniqueResult();
		}

		if (Common.isNumber(execution.getParameter("perguruanTinggi"))) {
			perguruanTinggi = (PerguruanTinggi) HibernateUtil.currentSession().createCriteria(PerguruanTinggi.class)
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("perguruanTinggi").trim())))
					.uniqueResult();
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

		String[] contents = new String[] { "id", "nama", "asesor1", "asesor2", "tanggal", "keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(BerkasHasilAkreditasi.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, BerkasHasilAkreditasi.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

	}

	/**
	 * Renderer lokal untuk layar/komponen {@link BerkasHasilAkreditasiAction}. Kelas ini menerjemahkan satu item
	 * data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link BerkasHasilAkreditasiAction} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see BerkasHasilAkreditasiAction
	 */
	class BerkasHasilAkreditasiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final BerkasHasilAkreditasi berkasHasilAkreditasi = (BerkasHasilAkreditasi) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					Common.clear(detail);
					if (detail.isOpen()) {
						BerkasHasilAkreditasiPunyaNamaHelper detailperkuliahanHelper = new BerkasHasilAkreditasiPunyaNamaHelper();
						detailperkuliahanHelper.display(berkasHasilAkreditasi, detail, addWindow);
					}
				}
			});

			RevisiHelper.createNewRevisi(BerkasHasilAkreditasi.class, berkasHasilAkreditasi,
					berkasHasilAkreditasi.getNama()).setParent(arg0);
			new Label(berkasHasilAkreditasi.getAsesor1()).setParent(arg0);
			new Label(berkasHasilAkreditasi.getAsesor2()).setParent(arg0);
			new Label(berkasHasilAkreditasi.getTanggal() == null ? ""
					: Common.dateFormat4.get().format(berkasHasilAkreditasi.getTanggal())).setParent(arg0);
			new Label(berkasHasilAkreditasi.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(berkasHasilAkreditasi);
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
											Common.refreshDelete(berkasHasilAkreditasi);
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
		init(new BerkasHasilAkreditasi());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(BerkasHasilAkreditasi berkasHasilAkreditasi) {
		this.berkasHasilAkreditasi = berkasHasilAkreditasi;
		addWindow.setTitle(berkasHasilAkreditasi.getId() == null ? "Tambah Berkas Akreditasi" : "Ubah Berkas Akreditasi");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Akreditasi"));
		row.appendChild(nama = new Textbox(berkasHasilAkreditasi.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Asesor I"));
		row.appendChild(asesor1 = new Textbox(berkasHasilAkreditasi.getAsesor1()));
		asesor1.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Asesor II"));
		row.appendChild(asesor2 = new Textbox(berkasHasilAkreditasi.getAsesor2()));
		asesor2.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal"));
		row.appendChild(tanggal = new MyDatebox(berkasHasilAkreditasi.getTanggal()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(berkasHasilAkreditasi.getKeterangan()));
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
			PesanFormalHelper.tampilkanGagal("penyimpanan data Akreditasi",
					"Kolom Nama Akreditasi belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama Akreditasi.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (berkasHasilAkreditasi.getId() != null) {
			berkasHasilAkreditasi = (BerkasHasilAkreditasi) session.load(BerkasHasilAkreditasi.class,
					berkasHasilAkreditasi.getId());

		}

		berkasHasilAkreditasi.setNama(nama.getValue());
		berkasHasilAkreditasi.setAsesor1(asesor1.getValue());
		berkasHasilAkreditasi.setAsesor2(asesor2.getValue());
		berkasHasilAkreditasi.setTanggal(tanggal.getValue());
		berkasHasilAkreditasi.setKeterangan(keterangan.getValue());
		berkasHasilAkreditasi.setJurusan(jurusan);
		berkasHasilAkreditasi.setFakultas(fakultas);
		berkasHasilAkreditasi.setPerguruanTinggi(perguruanTinggi);

		Common.refreshUpdate(session, berkasHasilAkreditasi);

		return true;
	}

	// private Jurusan jurusan;
	// private Fakultas fakultas;
	// private PerguruanTinggi perguruanTinggi;

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(BerkasHasilAkreditasi.class);

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(jurusan == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("jurusan", jurusan))
				.add(fakultas == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("fakultas", fakultas))
				.add(perguruanTinggi == null || perguruanTinggi.getId() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("perguruanTinggi", perguruanTinggi))
				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<BerkasHasilAkreditasi> berkasHasilAkreditasi = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(berkasHasilAkreditasi);
		grid.setRowRenderer(new BerkasHasilAkreditasiRenderer());
		grid.setModelCheckMobile(strset);

	}

}
