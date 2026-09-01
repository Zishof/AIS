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
 * Implementasi pola "Bandbox picker" AIS untuk entity
 * {@link ais.database.model.sekolah.KelasSiswa} — lihat {@link ais.ui.util.GetEventListener}
 * untuk arsitektur kerangka umum (constructor/display/onSearchDefault/renderer/callback).
 * <p>
 * Memilih satu {@link KelasSiswa} — rombongan belajar REGULER (kelas resmi tempat siswa
 * terdaftar per tahun ajaran, beda dari {@code KelasLesSiswa} yang merupakan kelas
 * les/ekstrakurikuler) — dari daftar popup, dengan filter nama, tahun akademik, sekolah, dan
 * guru/wali pembina, serta pemilihan tunggal lewat {@link Radiogroup}. Combo sekolah otomatis
 * terisi dan dinonaktifkan bila konteks sekolah aktif sudah diketahui lewat
 * {@link SekolahUtil#getSekolah()}. Field {@code wali} adalah nested
 * {@link AmbilDataGuruBanbox} — picker guru bersarang di dalam picker ini — yang dipasangi
 * listener sehingga memilih guru di situ langsung memicu pencarian ulang. Field {@code wali}
 * boleh {@code null} bila banbox dipakai lintas konteks tanpa {@link #display()} penuh
 * (mis. dipanggil dari layar laporan rekap absen piket harian) — {@link #onSearchDefault(Event)}
 * menjaga hal ini secara eksplisit sebelum membaca atribut guru dari {@code wali}.
 * </p>
 * <p>
 * <b>Otorisasi guru di {@link #onSearchDefault(Event)} lebih kompleks dari
 * {@code AmbilDataKelasLesSiswaBanbox}</b> karena membedakan peran Guru BK (bimbingan konseling)
 * dan Guru Pembina/Wali Kelas, DAN mengecek langsung ke {@link JadwalPelajaran} (jadwal
 * pelajaran per kelas, dengan hingga 12 slot guru mata pelajaran {@code guru}..{@code guru12})
 * untuk menentukan kelas mana saja yang benar-benar diajar guru yang sedang login pada tahun
 * akademik/sekolah terpilih:
 * <ul>
 * <li>{@code waliKelasDanGuru == true}: bila user login BUKAN guru, hasil difilter ke guru yang
 * dipilih di picker {@code wali} (atau tampil semua bila belum memilih); bila user login ADALAH
 * guru, hasil dibatasi ke kelas yang menurut {@link JadwalPelajaran} benar-benar diajar guru
 * tsb (kosong bila guru itu tidak mengajar kelas apa pun pada TA/sekolah terpilih).</li>
 * <li>{@code waliKelasDanGuru == false} dan {@code semuaTampil == true}: sama seperti kasus
 * non-guru di atas — difilter ke guru yang dipilih di picker {@code wali}, atau tampil semua
 * bila belum dipilih.</li>
 * <li>{@code waliKelasDanGuru == false} dan {@code semuaTampil == false} (mode paling ketat):
 * kelas ditampilkan ke guru login HANYA bila (a) guru adalah Guru BK kelas itu dan kelas
 * mewajibkan absensi lewat Guru BK ({@code absensiharusGuruBk}), ATAU (b) guru adalah Guru
 * Pembina/Wali kelas itu dan kelas mewajibkan absensi lewat Guru Pembina
 * ({@code absensiharusGuruPembina}), ATAU (c) kelas TIDAK mewajibkan absensi lewat BK maupun
 * Pembina dan guru benar-benar mengajar kelas itu menurut {@link JadwalPelajaran} (user
 * non-guru melihat semua kelas tanpa restriksi kategori (c)).</li>
 * </ul>
 * Constructor tanpa argumen memakai {@code semuaTampil=true, waliKelasDanGuru=false}. Berbeda
 * dari {@code AmbilDataKelasLesSiswaBanbox}, field {@code ta} (tahun akademik berjalan) DIPAKAI
 * di sini sebagai nilai awal combo {@code tahunAkademik}. Field {@code pagingHelper}
 * ({@link ais.ui.util.AmbilDataPagingHelper}) dideklarasikan namun TIDAK dipakai (paging aktual
 * memakai mold client-side "paging" + {@code Common.MAX_RESULT_1000}) — sisa refactor yang
 * belum tuntas, bukan bug fungsional yang perlu diperbaiki di sini. Semua hasil juga difilter
 * ke kelas yang {@code aktif} true atau belum diisi.
 * </p>
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

	/** Membuat komponen dengan {@code semuaTampil=true, waliKelasDanGuru=false} (lihat {@link #AmbilDataKelasSiswaBanbox(boolean, boolean)}). */
	public AmbilDataKelasSiswaBanbox() {
		this(true, false);
	}

	/** Tahun akademik berjalan; dipakai sebagai nilai awal terpilih combo {@code tahunAkademik}. */
	public String ta = Common.getCurrentTahunAkademik();

	/**
	 * @param semuaTampil     bila {@code true} (dan {@code waliKelasDanGuru} tidak
	 *                        mengubahnya), hasil difilter ke guru yang dipilih pada picker
	 *                        {@code wali}, atau tampil semua bila belum dipilih
	 * @param waliKelasDanGuru bila {@code true}, batasi hasil untuk user login yang berperan
	 *                        sebagai guru hanya ke kelas yang benar-benar diajarnya menurut
	 *                        {@link JadwalPelajaran}; lihat penjelasan lengkap kombinasi kedua
	 *                        flag ini (termasuk perbedaan peran Guru BK vs Guru Pembina) di
	 *                        Javadoc kelas
	 */
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
	 * Renderer baris hasil pencarian kelas siswa: radio pilih (memilih kelas, menutup popup, dan
	 * memicu {@code eventListener}), nama kelas, ruang, tingkat, sekolah (label "Semua" bila
	 * kelas tidak terikat satu sekolah tertentu), kurikulum sekolah, dan nama guru pembina/wali.
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

	/**
	 * Menyusun konten popup bandbox (dipanggil sekali saat pertama dibuka): form pencarian
	 * (nama, tahun akademik, sekolah, guru/wali — label kolom guru berubah menjadi "Wali" bila
	 * {@code semuaTampil} atau "Guru/Wali" sebaliknya) dan grid hasil digabung dalam SATU grid
	 * utama (pola "1-grid" agar seluruh popup menggulir bersama, tanpa North terpisah), dengan
	 * paginasi client-side (mold "paging", 50 baris/halaman), lalu memuat data awal lewat
	 * {@link #onSearchDefault(Event)}.
	 *
	 * @see ais.ui.util.GetEventListener
	 */
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

	/**
	 * Memuat ulang grid hasil pencarian kelas siswa sesuai filter formulir dan flag
	 * {@code semuaTampil}/{@code waliKelasDanGuru} constructor saat ini (lihat penjelasan
	 * kombinasi lengkap, termasuk peran Guru BK vs Guru Pembina, di Javadoc kelas), memakai sesi
	 * Hibernate thread-local. Bila mode saat ini butuh scoping guru ({@code !semuaTampil ||
	 * waliKelasDanGuru}) dan user login adalah guru, method ini lebih dulu mengumpulkan
	 * {@code ids} — id kelas yang menurut {@link JadwalPelajaran} benar-benar diajar guru
	 * tersebut (dicek di 12 slot {@code guru}..{@code guru12}, difilter tahun
	 * akademik/sekolah terpilih) — sebelum membangun kriteria utama {@link KelasSiswa}. Selalu
	 * menambahkan filter {@code aktif} true/belum diisi, urutan sekolah/tingkat/nama, filter
	 * nama (ilike), tahun akademik, dan sekolah dari formulir, dibatasi
	 * {@code Common.MAX_RESULT_1000} baris.
	 *
	 * @param event event pemicu (tidak dipakai — pemanggil selalu mengirim {@code null})
	 */
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

	/** @param eventListener dipanggil setiap kali user memilih satu kelas siswa dari daftar */
	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	/** @return listener pemilihan kelas siswa yang sedang terpasang, boleh {@code null} */
	public EventListener getEventListener() {
		return eventListener;
	}
}
