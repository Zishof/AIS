package ais.action.master.helper;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.DosenAction;
import ais.common.Common;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.KegiatanKedosenan;
import ais.database.model.KegiatanKedosenanPunyaDosen;
import ais.database.model.Tbmuser;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Helper "pilih dari daftar" untuk menugaskan satu {@link Dosen} ke satu atau lebih
 * {@link KegiatanKedosenan} (kegiatan tridarma dosen yang sudah berstatus
 * {@link KegiatanKedosenan#DISETUJUI}), lewat relasi {@link KegiatanKedosenanPunyaDosen}.
 * Menampilkan jendela modal pencarian berdasarkan nama kegiatan, dengan grid berpaging server-side
 * (50 baris per halaman via {@link Common#initPaging50}) dan checkbox per baris.
 *
 * <p>
 * Kegiatan yang sudah tertaut ke dosen ditampilkan dengan checkbox tercentang sekaligus
 * dinonaktifkan ({@code disabled}) — sehingga hanya kegiatan yang belum tertaut yang bisa
 * dipilih/disimpan; {@link #save()} hanya memproses baris yang tercentang DAN tidak dinonaktifkan.
 * Hanya kegiatan yang boleh dipilih dosen yang muncul di daftar (filter
 * {@code bolehDipilih}/{@code kelompokKegiatanKedosenan.bisaDipilihDosen}/{@code aktif} pada
 * {@link #initCriteria(boolean)}).
 * </p>
 */
public class AmbilDataKegiatanForKegiatanKedosenanHelper {

	private Dosen dosen;
	private MyGrid grid;

	private Textbox nama;

	private Paging paging;

	/**
	 * Membuat helper untuk satu {@link Dosen} dan menyiapkan komponen paging server-side (50 baris
	 * per halaman) yang memicu {@link #onSearchDefault(Event)} saat halaman berganti.
	 *
	 * @param dosen dosen yang akan ditugaskan ke kegiatan-kegiatan terpilih
	 */
	public AmbilDataKegiatanForKegiatanKedosenanHelper(Dosen dosen) {
		this.dosen = dosen;

		paging = new Paging();
		Common.initPaging50(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

	}

	/** Perender baris grid: checkbox status tertaut (tercentang+nonaktif bila relasi sudah ada) plus label nama kegiatan, fakultas, jurusan, kelompok aspek, detail aspek, dan keterangan. */
	class DosenRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final KegiatanKedosenan kegiatanKedosenan = (KegiatanKedosenan) arg1;
			Session session = HibernateUtil.currentSession();
			int count = ((Number) session.createCriteria(KegiatanKedosenanPunyaDosen.class)
					.add(Restrictions.eq("dosen", dosen)).add(Restrictions.eq("kegiatanKedosenan", kegiatanKedosenan))
					.setProjection(Projections.rowCount()).uniqueResult()).intValue();
			final MyCheckboxConfig checkbox = new MyCheckboxConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.setAttribute("kegiatanKedosenan", kegiatanKedosenan);
			checkbox.setChecked(count != 0);
			checkbox.setDisabled(count != 0);

			new Label(kegiatanKedosenan.getNama()).setParent(arg0);
			new Label(kegiatanKedosenan.getFakultas() == null ? "Semua" : kegiatanKedosenan.getFakultas().getNama())
					.setParent(arg0);
			new Label(kegiatanKedosenan.getJurusan() == null ? "Semua" : kegiatanKedosenan.getJurusan().getNama())
					.setParent(arg0);
			new Label(kegiatanKedosenan.getKelompokKegiatanKedosenan().getNama()).setParent(arg0);
			new Label(kegiatanKedosenan.getDetailKelompokKegiatanKedosenan().getNama()).setParent(arg0);
			new Label(kegiatanKedosenan.getKeterangan()).setParent(arg0);

		}
	}

	/**
	 * Membuat relasi {@link KegiatanKedosenanPunyaDosen} baru (mencatat {@code oleh}/{@code tbmuser}
	 * dari user yang sedang login dan {@code diubahDari = DosenAction}) untuk setiap baris grid
	 * yang tercentang dan tidak dinonaktifkan (belum tertaut sebelumnya). Kegagalan per baris
	 * ditelan.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void save() throws InterruptedException {
		Session session = HibernateUtil.currentSession();
		final Tbmuser tbmuser = Common.getCurrentUser();

		Rows rows = grid.getRows();
		List<Row> list = rows.getChildren();
		for (Row row : list) {
			List data = row.getChildren();
			try {
				MyCheckboxConfig checkbox = (MyCheckboxConfig) data.get(0);
				if (checkbox.isChecked() && !checkbox.isDisabled()) {
					KegiatanKedosenan kegiatanKedosenan = (KegiatanKedosenan) checkbox
							.getAttribute("kegiatanKedosenan");
					KegiatanKedosenanPunyaDosen kegiatanKedosenanPunyaDosen = (KegiatanKedosenanPunyaDosen) session
							.createCriteria(KegiatanKedosenanPunyaDosen.class)
							.add(Restrictions.eq("kegiatanKedosenan", kegiatanKedosenan))
							.add(Restrictions.eq("dosen", dosen)).setMaxResults(1).uniqueResult();
					if (kegiatanKedosenanPunyaDosen == null) {
						kegiatanKedosenanPunyaDosen = new KegiatanKedosenanPunyaDosen();
						kegiatanKedosenanPunyaDosen.setKegiatanKedosenan(kegiatanKedosenan);
						kegiatanKedosenanPunyaDosen.setOleh(tbmuser.getUserId());
						kegiatanKedosenanPunyaDosen.setTbmuser(tbmuser);
						kegiatanKedosenanPunyaDosen.setDosen(dosen);
						kegiatanKedosenanPunyaDosen.setDiubahDari(DosenAction.class.getSimpleName());
						Common.refreshSaveOrUpdate(session, kegiatanKedosenanPunyaDosen);
					}

				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataKegiatanForKegiatanKedosenanHelper.java:127");
				// TODO: handle exception
			}
		}

	}

	/**
	 * Membangun dan menampilkan jendela modal pemilihan kegiatan kedosenan: form pencarian nama
	 * kegiatan, grid ber-paging server-side dengan checkbox per baris, dan tombol Simpan/Batal.
	 *
	 * @param dataLoader callback penyegar tampilan pemanggil setelah simpan
	 * @param window     jendela modal yang akan dibangun isinya (dibersihkan lebih dulu)
	 */
	public void display(final DataLoader dataLoader, final MyWindow window) {

		Common.clear(window);
		window.setTitle("Ambil Data Kegiatan Dosen");
		window.setWidth("90%");
		window.setHeight("90%");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		North north = new North();
		north.setParent(borderlayout);
		// FIX toolbar/tombol tidak tampil: pada ZK5 region North memakai tinggi bawaan
		// (+-100px); dengan flex=true isinya diregangkan ke tinggi tersebut sehingga
		// Toolbar yang diletakkan DI BAWAH grid filter ikut terpotong. Disamakan dengan
		// layar sejenis yang sudah benar (DownloadMahasiswa, DownloadKrs, DownloadNilai):
		// flex dimatikan + tinggi eksplisit. Autoscroll sebagai pengaman bila isi bertambah.
		ais.ui.util.ZkCompat.setFlex(north, false);
		north.setHeight("160px");
		north.setAutoscroll(true);

		Div div = new Div();
		div.setParent(north);

		MyGrid searchgrid = new MyGrid();
		searchgrid.setWidth("100%");
		searchgrid.setParent(div);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Kegiatan"));
		row.appendChild(nama = new Textbox());
		nama.setWidth("90%");

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(div);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});
		button.setParent(toolbar);

		Borderlayout myBorderlayout1 = new ais.ui.util.MyBorderlayout();
		myBorderlayout1.setParent(center);

		Center myCenter1 = new Center();
		ais.ui.util.ZkCompat.setFlex(myCenter1, true);
		myCenter1.setParent(myBorderlayout1);

		South mySouth = new South();
		mySouth.setParent(myBorderlayout1);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setParent(myCenter1);

		paging.setParent(mySouth);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		final MyCheckboxConfig checkbox = new MyCheckboxConfig();
		column.appendChild(checkbox);
		checkbox.addEventListener(Events.ON_CHECK, new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				List<Row> rows = grid.getRows().getChildren();
				for (Row row : rows) {
					try {
						MyCheckboxConfig myCheckbox = (MyCheckboxConfig) row.getAttribute("checkbox");
						myCheckbox.setChecked(!myCheckbox.isDisabled() && checkbox.isChecked());

						if (myCheckbox.isDisabled()) {
							continue;
						}

						myCheckbox.setChecked(checkbox.isChecked());
						if (!checkbox.isChecked()) {
							continue;
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataKegiatanForKegiatanKedosenanHelper.java:226");

					}

				}
			}
		});

		column.setWidth("50px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Fakultas");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jurusan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Aspek");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Rincian Aspek");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");
		column.setWidth("20%");

		onSearchDefault(null);

		South south = new South();
		south.setParent(borderlayout);

		toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);

		button = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		button.setTooltiptext("Simpan");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				save();
				dataLoader.loadData(null);
				window.setVisible(false);
			}
		});
		button.setParent(toolbar);

		// button = new MyToolbarbuttonConfig("Ambil Semua", "/img/save.gif");
		// button.setTooltiptext("Simpan");
		// button.addEventListener("onClick", new EventListener() {
		// @Override
		// public void onEvent(Event event) throws Exception {
		// saveSemua();
		// dataLoader.loadData(null);
		// window.setVisible(false);
		// }
		// });
		// button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		button.setTooltiptext("Tutup");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.setVisible(false);
			}
		});
		button.setParent(toolbar);

		window.setVisible(true);
		try {
			window.onModal();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Membangun kriteria pencarian dasar {@link KegiatanKedosenan} yang boleh dipilih dosen: status
	 * {@link KegiatanKedosenan#DISETUJUI}, {@code bolehDipilih} tidak {@code false}, dan kelompok
	 * kegiatannya {@code bisaDipilihDosen} serta {@code aktif} tidak {@code false}, disaring pula
	 * dengan nama kegiatan (ilike) dari kotak pencarian bila diisi.
	 *
	 * @param order bila {@code true}, tambahkan pengurutan menurun berdasarkan tanggal mulai lalu id
	 *              (dipakai saat mengambil data untuk ditampilkan); bila {@code false}, kriteria
	 *              dipakai untuk menghitung total baris pada {@link Common#initPaging50}
	 * @return kriteria Hibernate siap dieksekusi/diberi batas hasil
	 */
	public Criteria initCriteria(boolean order) {

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(KegiatanKedosenan.class)
				.add(Restrictions.or(Restrictions.isNull("bolehDipilih"), Restrictions.eq("bolehDipilih", true)))
				.createAlias("kelompokKegiatanKedosenan", "kelompokKegiatanKedosenan")
				.add(Restrictions.or(Restrictions.isNull("kelompokKegiatanKedosenan.bisaDipilihDosen"),
						Restrictions.eq("kelompokKegiatanKedosenan.bisaDipilihDosen", true)))
				.add(Restrictions.or(Restrictions.isNull("kelompokKegiatanKedosenan.aktif"),
						Restrictions.eq("kelompokKegiatanKedosenan.aktif", true)))
				.add(Restrictions.eq("status", KegiatanKedosenan.DISETUJUI));

		if (order)
			criteria.addOrder(Order.desc("mulai")).addOrder(Order.desc("id"));

		criteria.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
				: Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE));

		return criteria;
	}

	/**
	 * Menghitung total baris untuk komponen paging ({@link Common#initPaging50}) lalu mengambil satu
	 * halaman {@link KegiatanKedosenan} (50 baris) sesuai halaman aktif, dan memuat ulang grid
	 * dengan hasilnya.
	 *
	 * @param event event pemicu (pencarian/paging), tidak dipakai langsung
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Common.initPaging50(initCriteria(false), paging);

		List<Dosen> dosen = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE_50)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE_50 * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(dosen);
		grid.setRowRenderer(new DosenRenderer());
		grid.setModelCheckMobile(strset);

	}

}
