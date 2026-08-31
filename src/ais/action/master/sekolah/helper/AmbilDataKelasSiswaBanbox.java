package ais.action.master.sekolah.helper;


import ais.common.CommonSearchFilterHelper;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Bandbox;
import org.zkoss.zul.Bandpopup;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.Sekolah;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Tipe khusus untuk ambil data kelas siswa banbox. Kelas ini memberi nama dan batas tanggung jawab
 * yang eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Bandbox}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyGrid grid}, {@code
 * ais.ui.util.AmbilDataPagingHelper pagingHelper}, {@code EventListener eventListener}, {@code boolean
 * semuaTampil}, {@code boolean waliKelasDanGuru}, {@code String ta}, {@code Textbox nama}, {@code Combobox
 * searchsekolah}; pembacaan/pencarian ({@code onSearchDefault()}, {@code setEventListener()}, {@code
 * getEventListener()}); operasi domain lain ({@code display()}). Bagian lain dari kontrak tetap mengikuti kelas
 * induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see Bandbox
 */
public class AmbilDataKelasSiswaBanbox extends Bandbox implements GetEventListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;

	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;
	private boolean semuaTampil;
	private boolean waliKelasDanGuru;

	public AmbilDataKelasSiswaBanbox() {
		this(true, false);
	}

	public String ta = Common.getCurrentTahunAkademik();

	public AmbilDataKelasSiswaBanbox(boolean semuaTampil, boolean waliKelasDanGuru) {
		super();
		this.semuaTampil = semuaTampil;
		this.waliKelasDanGuru = waliKelasDanGuru;
		setReadonly(true);

		addEventListener("onOpen", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (getChildren().isEmpty()) {

					display();

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							setOpen(true);
						}
					});
				}
			}
		});

	}

	private Textbox nama;
	private Combobox searchsekolah = new Combobox();
	public Combobox tahunAkademik;
	private AmbilDataGuruBanbox wali;

	/**
	 * Renderer lokal untuk layar/komponen {@link AmbilDataKelasSiswaBanbox}. Kelas ini menerjemahkan satu item
	 * data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link AmbilDataKelasSiswaBanbox} dan dapat mengakses
	 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see AmbilDataKelasSiswaBanbox
	 */
	class KelasSiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final KelasSiswa kelas = (KelasSiswa) arg1;
			MyRadioConfig checkbox = new MyRadioConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);

			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataKelasSiswaBanbox.this.setOpen(false);
					AmbilDataKelasSiswaBanbox.this.setAttribute("kelasSiswa", kelas);
					AmbilDataKelasSiswaBanbox.this.setAttribute("kelas", kelas);
					AmbilDataKelasSiswaBanbox.this.setValue(kelas.toString());

					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			new Label(kelas.getNama()).setParent(arg0);
			new Label(kelas.getRuang() == null ? "" : kelas.getRuang().getNama()).setParent(arg0);
			new Label(kelas.getTingkat() + "").setParent(arg0);
			new Label(kelas.getSekolah() == null ? "Semua" : kelas.getSekolah().getNama() + "").setParent(arg0);
			new Label(kelas.getKurikulumSekolah() == null ? "" : kelas.getKurikulumSekolah().getNama()).setParent(arg0);
			new Label(kelas.getGuruPembina() == null ? "" : kelas.getGuruPembina().getNama()).setParent(arg0);

		}

	}

	public void display() {
		setReadonly(true);
		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("950px");
		bandpopup.setHeight("600px");

		final Radiogroup radiogroup = new Radiogroup();
		radiogroup.setWidth("100%");
		radiogroup.setHeight("100%");
		radiogroup.setParent(bandpopup);

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(radiogroup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Kelas Siswa");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(panelchildren);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		// SATU SCROLL: filter, tombol cari, dan tabel data ditaruh dalam SATU grid utama di dalam
		// Center (tanpa North terpisah) sehingga seluruh isi popup menggulir bersama (sesuai pola
		// 1-grid). Setiap bagian menjadi satu baris pada grid utama.
		org.zkoss.zul.Grid gridUtama = new org.zkoss.zul.Grid();
		gridUtama.setWidth("100%");
		ais.ui.util.ZkCompat.setFlex(gridUtama, true);
		gridUtama.setParent(center);
		Rows rowsUtama = new Rows();
		rowsUtama.setParent(gridUtama);

		Row rowUtama = new Row();
		rowUtama.setParent(rowsUtama);

		MyGrid searchgrid = new MyGrid();
		searchgrid.setWidth("100%");
		searchgrid.setParent(rowUtama);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama = new Textbox());
		nama.setWidth("90%");
		nama.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		row.appendChild(new MyLabelConfig("TA : "));
		tahunAkademik = new Combobox();
		Common.generateTahunAjaran(tahunAkademik);
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");
		Common.selectComboItem(tahunAkademik, ta);
		tahunAkademik.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah"));
		row.appendChild(searchsekolah);
		Common.insertComboDanSemua(searchsekolah, "nama", Sekolah.class);

		Sekolah sekolah = SekolahUtil.getSekolah();
		if (sekolah != null) {
			Common.selectComboItem(searchsekolah, sekolah);
			searchsekolah.setDisabled(true);
		}

		searchsekolah.setWidth("90%");
		searchsekolah.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		row.appendChild(new ais.ui.util.MyLabelConfig(semuaTampil ? "Wali" : "Guru/Wali"));
		row.appendChild(wali = new AmbilDataGuruBanbox());
		wali.setWidth("90%");
		wali.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

//		if (semuaTampil) {
//			wali.setAttribute("myValue", null);
//			wali.setAttribute("guru", null);
//			wali.setDisabled(false);
//			wali.setValue("");
//		}

		Row rowKedua = new Row();
		rowKedua.setParent(rowsUtama);
		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("32px");
		toolbar.setParent(rowKedua);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});
		button.setParent(toolbar);

		toolbar.appendChild(Common.createCleanButton(this, this));

		Row rowKetiga = new Row();
		rowKetiga.setParent(rowsUtama);
		grid = new MyGrid();
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.getPagingChild().setMold("os");
		grid.setParent(rowKetiga);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("30px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Ruang");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tingkat");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Sekolah");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kurikulum");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Wali Kelas");

		onSearchDefault(null);

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();

		List<Long> ids = new ArrayList<Long>();
		Tbmuser tbmuser = Common.getCurrentUser();
		Guru guru = tbmuser.ambilGuru();
		if (!semuaTampil || waliKelasDanGuru) {

			if (guru != null) {
				ids = session.createCriteria(JadwalPelajaran.class).add(Restrictions.isNotNull("kelas"))
						.setProjection(Projections.groupProperty("kelas.id"))

						.add(tahunAkademik.getSelectedItem() == null
								|| tahunAkademik.getSelectedItem().getValue() == null
								|| tahunAkademik.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("tahunAjaran", tahunAkademik.getSelectedItem().getValue()))

						.add(searchsekolah.getSelectedItem() == null
								|| searchsekolah.getSelectedItem().getValue() == null
								|| searchsekolah.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false))

						.add(guru == null ? Restrictions.sqlRestriction("1=1") :

								Restrictions.or(CommonSearchFilterHelper.eqValue("guru12", guru, false), Restrictions.or(
										CommonSearchFilterHelper.eqValue("guru11", guru, false),
										Restrictions.or(CommonSearchFilterHelper.eqValue("guru10", guru, false), Restrictions.or(
												CommonSearchFilterHelper.eqValue("guru9", guru, false),
												Restrictions.or(CommonSearchFilterHelper.eqValue("guru8", guru, false), Restrictions.or(
														CommonSearchFilterHelper.eqValue("guru7", guru, false),
														Restrictions.or(CommonSearchFilterHelper.eqValue("guru6", guru, false), Restrictions.or(
																CommonSearchFilterHelper.eqValue("guru5", guru, false),
																Restrictions.or(CommonSearchFilterHelper.eqValue("guru4", guru, false),
																		Restrictions.or(CommonSearchFilterHelper.eqValue("guru3", guru, false),
																				Restrictions.or(
																						CommonSearchFilterHelper.eqValue("guru", guru, false),
																						CommonSearchFilterHelper.eqValue("guru2", guru, false))))))))))))

						).list();

			}
		}

		// wali (AmbilDataGuruBanbox) bisa NULL saat banbox dipakai lintas-konteks (mis. dipanggil
		// dari LaporanRekapAbsenPiketHarian) tanpa init UI penuh → NPE di wali.getAttribute("guru").
		Object waliGuru = (wali == null ? null : wali.getAttribute("guru"));
		List<KelasSiswa> kelas = session.createCriteria(KelasSiswa.class)

				.add(waliKelasDanGuru
						? (guru == null
								? (waliGuru == null ? Restrictions.sqlRestriction("true")
										: CommonSearchFilterHelper.eqValue("guruPembina", waliGuru, false))
								: ids.isEmpty()
										? Restrictions.sqlRestriction("false")
										: ids.isEmpty() ? Restrictions.sqlRestriction("true")
												: Restrictions.in("id", ids))
						:

						semuaTampil
								? (waliGuru == null ? Restrictions.sqlRestriction("true")
										: CommonSearchFilterHelper.eqValue("guruPembina", waliGuru, false))
								: Restrictions
										.or(
												// BK-restricted: tampilkan HANYA ke Guru BK kelas ini
												guru != null
														? Restrictions.and(Restrictions.eq("absensiharusGuruBk", true),
																CommonSearchFilterHelper.eqValue("guruBk", guru, false))
														: Restrictions.sqlRestriction("false"),
												Restrictions.or(
														// Wali-restricted: tampilkan HANYA ke Wali Kelas kelas ini
														guru != null
																? Restrictions.and(
																		Restrictions.eq("absensiharusGuruPembina", true),
																		CommonSearchFilterHelper.eqValue("guruPembina", guru, false))
																: Restrictions.sqlRestriction("false"),
														// Tidak ada restriksi: tampilkan ke guru yang mengajar di kelas ini
														Restrictions.and(
																Restrictions.and(
																		Restrictions.or(Restrictions.isNull("absensiharusGuruPembina"),
																				Restrictions.eq("absensiharusGuruPembina", false)),
																		Restrictions.or(Restrictions.isNull("absensiharusGuruBk"),
																				Restrictions.eq("absensiharusGuruBk", false))),
																guru != null && ids.isEmpty()
																		? Restrictions.sqlRestriction("false")
																		: ids.isEmpty() ? Restrictions.sqlRestriction("true")
																				: Restrictions.in("id", ids)))))

				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.asc("sekolah")).addOrder(Order.asc("tingkat")).addOrder(Order.asc("nama"))
				.add(nama.getText() == null || nama.getText().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("nama", nama.getText().trim(), MatchMode.ANYWHERE))

				.add(tahunAkademik.getSelectedItem() == null || tahunAkademik.getSelectedItem().getValue() == null
						|| tahunAkademik.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("tahunAjaran", tahunAkademik.getSelectedItem().getValue()))

				.add(searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
						|| searchsekolah.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false))

				.setMaxResults(Common.MAX_RESULT_1000)

				.list();

		ListModel strset = new SimpleListModel(kelas);
		grid.setRowRenderer(new KelasSiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}
