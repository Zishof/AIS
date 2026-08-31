package ais.action.master.payroll;

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
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Window;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.payroll.detail.CabangPunyaPegawaiAction;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.payroll.Cabang;
import ais.ui.util.DataInitDefault;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Controller/action ZK untuk cabang. Tipe ini merupakan titik masuk UI yang menghubungkan event
 * layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Window addWindow}, {@code MyGrid grid},
 * {@code Paging paging}, {@code MyTextbox searchnama}, {@code MyTextbox nama}, {@code MyTextbox keterangan},
 * {@code boolean edit}, {@code boolean delete}; inisialisasi/lifecycle ({@code doAfterCompose()}, {@code
 * init()}, {@code init()}, {@code initCriteria()}); pembacaan/pencarian ({@code onSearchDefault()});
 * validasi/perhitungan ({@code checkNamaCabang()}); mutasi data ({@code onSave()}); operasi domain lain ({@code
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
public class CabangAction extends GenericAutowireComposer implements DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private Window addWindow;
	private MyGrid grid;
	private Paging paging;

	private MyTextbox searchnama;
	private MyTextbox nama;
	private MyTextbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private Cabang cabang;
	private MyToolbarbuttonConfig add;

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
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

	}

	class CabangRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final Cabang cabang = (Cabang) arg1;

			new CabangPunyaPegawaiAction(cabang).setParent(arg0);

			RevisiHelper.createNewRevisi(Cabang.class, cabang, cabang.getNama()).setParent(arg0);
			new Label(cabang.getKeterangan()).setParent(arg0);

			Common.copyEditDeleteButtons(edit, delete, cabang, CabangAction.this).setParent(arg0);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new Cabang());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		Cabang cabang = (Cabang) obj;
		init(cabang);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(Cabang cabang) {
		this.cabang = cabang;
		addWindow.setTitle(cabang.getId() == null ? "Tambah Cabang" : "Ubah Cabang");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama Cabang")));
		row.appendChild(nama = new MyTextbox(cabang.getNama() == null ? "" : cabang.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Keterangan")));
		row.appendChild(keterangan = new MyTextbox(cabang.getKeterangan() == null ? "" : cabang.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(south);
		Toolbarbutton cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);
		Toolbarbutton save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					Common.initPaging(paging, new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							onSearchDefault(null);
						}
					});
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show(
					"Mohon maaf, kolom Nama Cabang belum diisi. Nama Cabang wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) mohon Bapak/Ibu mengisi kolom Nama Cabang; (2) pastikan kolom tersebut tidak dikosongkan; (3) kemudian tekan tombol Simpan kembali.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		/*
		 * if (keterangan.getValue().trim().equals("")) { Messagebox.show(
		 * "Keterangan harus diisi", "Peringatan", Messagebox.OK,
		 * Messagebox.EXCLAMATION); return false; }
		 */

		boolean i = checkNamaCabang();
		if (i) {
			MyMessageboxConfig.show(
					"Mohon maaf, Nama Cabang yang Bapak/Ibu masukkan sudah terdaftar di dalam basis data. Langkah yang dapat dilakukan: (1) mohon gunakan Nama Cabang yang berbeda; (2) periksa kembali daftar data cabang yang telah ada; (3) kemudian tekan tombol Simpan kembali.",
					"Peringatan", 1, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (cabang.getId() != null) {
			cabang = (Cabang) session.load(Cabang.class, cabang.getId());

		}

		cabang.setNama(nama.getValue());
		cabang.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, cabang);
		return true;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);
		List<Cabang> cabang = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(cabang);
		grid.setRowRenderer(new CabangRenderer());
		grid.setModelCheckMobile(strset);

		grid.renderAll();

	}

	private Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Cabang.class);
		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add((searchnama == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true") : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE)));
		return criteria;
	}

	public Boolean checkNamaCabang() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(Cabang.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("nama", nama.getValue().trim())).add(this.cabang.getId() == null
						? Restrictions.sqlRestriction("1=1") : Restrictions.ne("id", this.cabang.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
