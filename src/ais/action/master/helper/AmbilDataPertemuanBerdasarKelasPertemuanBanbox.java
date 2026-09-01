package ais.action.master.helper;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
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
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Div;
import ais.ui.util.MyGrid;
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
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.CommonSearchFilterHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.KelasPertemuan;
import ais.database.model.Pertemuan;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Implementasi pola "Bandbox picker" AIS untuk entity {@link ais.database.model.Pertemuan} —
 * lihat {@link ais.ui.util.GetEventListener} untuk arsitektur kerangka umum
 * (constructor/display/onSearchDefault/renderer/callback).
 * <p>
 * Berbeda dari picker Pertemuan lain, kelas ini mencari Pertemuan (mis. topik/kemampuan akhir
 * pembelajaran suatu sesi kuliah) secara TIDAK LANGSUNG lewat entity penghubung
 * {@link ais.database.model.KelasPertemuan}: query di {@code onSearchDefault()} melakukan
 * {@code Projections.groupProperty("pertemuan")} atas {@code KelasPertemuan} sehingga hasilnya
 * adalah Pertemuan-Pertemuan berbeda yang sudah pernah dipakai pada minimal satu baris
 * KelasPertemuan yang cocok kriteria — cara mengambil "daftar topik pertemuan yang sudah ada"
 * untuk digunakan ulang, bukan pencarian langsung ke tabel Pertemuan. Kriteria pencarian: dosen
 * pengampu ({@code AmbilDataDosenBanbox searchdosen}, dicocokkan ke salah satu dari 10 slot dosen
 * perkuliahan {@code perkuliahan.dosen1}..{@code dosen10} lewat rangkaian OR), fakultas/prodi
 * ({@code searchfakultas}/{@code searchjurusan}, filter opsional yang tetap meloloskan baris
 * dengan jurusan/fakultas kosong), dan tahun angkatan ({@code Decimalbox searchtahun}, idem
 * meloloskan baris tanpa tahun angkatan). Berbeda dari kebanyakan subclass lain, kombo
 * fakultas/jurusan diinisialisasi lewat {@link ais.common.Common#initFakultasDanJurusanDanSemua}
 * di listener {@code onOpen} SEBELUM {@code display()} dipanggil, bukan di dalam {@code display()}
 * itu sendiri. Pemilihan bersifat tunggal lewat {@link org.zkoss.zul.Radiogroup}; nilai Bandbox
 * diisi lewat {@code pertemuan.toString()}. Grid hasil memakai mold "paging" client-side dibatasi
 * {@link ais.common.Common#MAX_RESULT}.
 * </p>
 *
 * @see Bandbox
 */
public class AmbilDataPertemuanBerdasarKelasPertemuanBanbox extends Bandbox implements GetEventListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;

	/* Catatan: field ini dideklarasikan tapi tidak dipakai secara aktif di file ini — grid hasil
	 * pencarian di display() memakai mold "paging" client-side, bukan AmbilDataPagingHelper. */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;

	/**
	 * Konstruktor pola Bandbox picker: kunci input jadi read-only, lalu pasang listener
	 * {@code onOpen} yang — berbeda dari kebanyakan subclass lain — lebih dulu menginisialisasi
	 * pilihan {@link #searchfakultas}/{@link #searchjurusan} lewat
	 * {@link Common#initFakultasDanJurusanDanSemua} sebelum membangun popup lewat
	 * {@link #display()} secara lazy pada pembukaan pertama, lalu membuka popup lewat
	 * {@link Common#createDefaultTimer}. Lihat {@link ais.ui.util.GetEventListener} untuk
	 * penjelasan lengkap kerangka ini.
	 */
	public AmbilDataPertemuanBerdasarKelasPertemuanBanbox() {
		super();
		setReadonly(true);

		addEventListener("onOpen", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (getChildren().isEmpty()) {

					Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

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

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private AmbilDataDosenBanbox searchdosen = new AmbilDataDosenBanbox();
	private Decimalbox searchtahun = new Decimalbox();

	/**
	 * Renderer baris grid hasil pencarian: menampilkan radio button pilihan diikuti kolom jenis
	 * (nama {@code statusPertemuan}, kosong bila tidak diisi), topik (label kolom "Kemampuan akhir
	 * pembelajaran"), dan {@code pertemuan.info()} (label kolom "Perkuliahan"). Saat radio
	 * dicentang ({@code onCheck}), popup ditutup, entity {@link Pertemuan} terpilih disimpan
	 * sebagai attribute {@code "pertemuan"} pada Bandbox, teks Bandbox diisi
	 * {@code pertemuan.toString()}, lalu {@link #eventListener} (bila terpasang) diberi tahu —
	 * lihat pola callback selengkapnya di {@link ais.ui.util.GetEventListener}.
	 *
	 * @see AmbilDataPertemuanBerdasarKelasPertemuanBanbox
	 */
	class KelasPertemuanRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final Pertemuan pertemuan = (Pertemuan) arg1;
			MyRadioConfig checkbox = new MyRadioConfig();
			checkbox.setParent(arg0);arg0.setAttribute("checkbox", checkbox);
			// checkbox.setId(pertemuan.getId() + "");

			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataPertemuanBerdasarKelasPertemuanBanbox.this.setOpen(false);
					AmbilDataPertemuanBerdasarKelasPertemuanBanbox.this.setAttribute("pertemuan", pertemuan);
					AmbilDataPertemuanBerdasarKelasPertemuanBanbox.this.setValue(pertemuan.toString());

					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			new Label(pertemuan.getStatusPertemuan() == null ? ""
					: pertemuan.getStatusPertemuan().getNama()).setParent(arg0);
			new Label(pertemuan.getTopik()).setParent(arg0);
			new Label(pertemuan.info()).setParent(arg0);

		}

	}

	/**
	 * Membangun popup pencarian (form kriteria dosen/fakultas/prodi/angkatan + tombol Cari + grid
	 * hasil berbungkus {@link org.zkoss.zul.Radiogroup}) sekali saat popup pertama kali dibuka,
	 * lalu memanggil {@link #onSearchDefault(Event)} agar grid langsung terisi.
	 */
	public void display() {
		setReadonly(true);
		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("800px");
		bandpopup.setHeight("600px");

		final Radiogroup radiogroup = new Radiogroup();
		radiogroup.setWidth("100%");
		radiogroup.setHeight("100%");
		radiogroup.setParent(bandpopup);

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(radiogroup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Kelas Pertemuan");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(panelchildren);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

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
		row.appendChild(new ais.ui.util.MyLabelConfig("Dosen"));
		row.appendChild(searchdosen);
		searchdosen.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Angkatan"));
		row.appendChild(searchtahun);
		searchtahun.setWidth("90%");

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		Row rowKedua = new Row();
		rowKedua.setParent(rowsUtama);
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

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		/* Grid memakai mold "paging" client-side dengan setPageSize(50); dibatasi juga di query
		 * lewat Common.MAX_RESULT (lihat onSearchDefault()). */
		Row rowKetiga = new Row();
		rowKetiga.setParent(rowsUtama);
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
		column.setLabel("Jenis");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kemampuan akhir pembelajaran");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Perkuliahan");
		column.setWidth("70%");

		onSearchDefault(null);

	}

	/**
	 * Menjalankan pencarian {@link Pertemuan} secara tidak langsung lewat
	 * {@link ais.database.model.KelasPertemuan}: query {@code createCriteria(KelasPertemuan.class)}
	 * dengan {@code Projections.groupProperty("pertemuan")} sehingga hasil berupa Pertemuan unik
	 * yang muncul di baris KelasPertemuan yang cocok kriteria dosen (salah satu dari 10 slot
	 * {@code perkuliahan.dosen1}..{@code dosen10}), fakultas/prodi (opsional, baris tanpa
	 * fakultas/jurusan tetap lolos), dan tahun angkatan (opsional, baris tanpa tahun angkatan
	 * tetap lolos). Hasil dipasang ke {@link #grid} lewat {@link KelasPertemuanRenderer} dan
	 * dibatasi {@link Common#MAX_RESULT} baris.
	 *
	 * @param event event pemicu (boleh {@code null}, dipakai juga sebagai pengisi awal grid saat
	 *              popup pertama dibuka)
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Criterion criterion = searchdosen.getAttribute("myValue") == null ? Restrictions.sqlRestriction("1=1")
				: Restrictions.or(Restrictions.eq("perkuliahan.dosen1", searchdosen.getAttribute("myValue")),
						Restrictions.eq("perkuliahan.dosen2", searchdosen.getAttribute("myValue")));

		criterion = Restrictions.or(criterion,
				Restrictions.eq("perkuliahan.dosen3", searchdosen.getAttribute("myValue")));
		criterion = Restrictions.or(criterion,
				Restrictions.eq("perkuliahan.dosen4", searchdosen.getAttribute("myValue")));
		criterion = Restrictions.or(criterion,
				Restrictions.eq("perkuliahan.dosen5", searchdosen.getAttribute("myValue")));
		criterion = Restrictions.or(criterion,
				Restrictions.eq("perkuliahan.dosen6", searchdosen.getAttribute("myValue")));
		criterion = Restrictions.or(criterion,
				Restrictions.eq("perkuliahan.dosen7", searchdosen.getAttribute("myValue")));
		criterion = Restrictions.or(criterion,
				Restrictions.eq("perkuliahan.dosen8", searchdosen.getAttribute("myValue")));
		criterion = Restrictions.or(criterion,
				Restrictions.eq("perkuliahan.dosen9", searchdosen.getAttribute("myValue")));
		criterion = Restrictions.or(criterion,
				Restrictions.eq("perkuliahan.dosen10", searchdosen.getAttribute("myValue")));

		Session session = HibernateUtil.currentSession();
		List<Pertemuan> kelasPertemuan = session.createCriteria(KelasPertemuan.class)

				.setProjection(Projections.groupProperty("pertemuan"))

				.createAlias("pertemuan", "pertemuan").createAlias("pertemuan.perkuliahan", "perkuliahan")
				.createAlias("perkuliahan.jurusan", "jurusan")

				.add(searchdosen.getAttribute("myValue") == null ? Restrictions.sqlRestriction("1=1") : criterion)

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.isNull("perkuliahan.jurusan"),
										CommonSearchFilterHelper.eqSelectedWithId("perkuliahan.jurusan", searchjurusan, false)))

				.add(searchtahun.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("perkuliahan.tahunAngkatan"),
								Restrictions.eq("perkuliahan.tahunAngkatan", searchtahun.getValue().intValue())))

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.isNull("jurusan.fakultas"),
										CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", searchfakultas, false)))

				.setMaxResults(Common.MAX_RESULT)

				.list();

		System.out.println(kelasPertemuan);
		ListModel strset = new SimpleListModel(kelasPertemuan);
		grid.setRowRenderer(new KelasPertemuanRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Menetapkan listener yang dipanggil setelah pengguna memilih satu baris pertemuan.
	 *
	 * @param eventListener listener baru yang akan dipasang
	 */
	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	/**
	 * Mengambil listener yang sedang terpasang.
	 *
	 * @return listener aktif saat ini, atau {@code null} bila belum diset
	 */
	public EventListener getEventListener() {
		return eventListener;
	}
}

