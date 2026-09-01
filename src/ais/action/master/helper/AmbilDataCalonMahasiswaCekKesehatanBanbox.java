package ais.action.master.helper;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Bandbox;
import org.zkoss.zul.Bandpopup;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.ws.util.ConstantUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.JenisKegiatan;
import ais.database.model.Kegiatan;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Implementasi pola "Bandbox picker" AIS untuk entity
 * {@link ais.database.model.BiodataCalonMahasiswa} (dijangkau lewat {@link Kegiatan} induknya) —
 * lihat {@link ais.ui.util.GetEventListener} untuk arsitektur kerangka umum
 * (constructor/display/onSearchDefault/renderer/callback). Varian khusus alur "cek kesehatan"
 * calon mahasiswa baru: query dasarnya BUKAN langsung ke {@link BiodataCalonMahasiswa}, melainkan
 * ke {@link Kegiatan} — hanya {@code Kegiatan} bertipe {@link JenisKegiatan} dengan nama
 * {@code ConstantUtil.PENDAFTARAN_ULANG_MAHASISWA_BARU} (kegiatan pendaftaran ulang mahasiswa
 * baru), berstatus {@code validated=1} (sudah divalidasi), DAN calon mahasiswa terkait harus sudah
 * punya {@code prodi_lulus} terisi (dinyatakan lulus seleksi/diterima) — yang muncul di hasil
 * pencarian; entity {@link BiodataCalonMahasiswa} dibaca lewat {@code kegiatan.getCalonMahasiswa()}.
 * Ini memastikan hanya calon mahasiswa yang statusnya sudah siap untuk dijadwalkan cek kesehatan
 * yang muncul di popup.
 * <p>
 * Popup menampilkan grid pilih-tunggal (via {@link Radiogroup}/{@link Radio}) dengan filter "Nama",
 * "No Registrasi", dan "No Ujian" (ketiganya ILIKE ANYWHERE). Kolom grid menampilkan no registrasi,
 * no ujian, dan gabungan nama seluruh prodi pilihan. Berbeda dari sebagian besar subclass lain,
 * constructor LANGSUNG memanggil {@link #display()} (bukan menunda ke listener {@code onOpen}).
 *
 * @see Bandbox
 */
public class AmbilDataCalonMahasiswaCekKesehatanBanbox extends Bandbox implements GetEventListener {

	/**
	 * Serial version UID standar untuk kompatibilitas serialisasi komponen ZK.
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;

	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	/** Membangun komponen dan LANGSUNG memanggil {@link #display()} di constructor. */
	public AmbilDataCalonMahasiswaCekKesehatanBanbox() {
		super();
		display();
	}

	private Textbox nama;
	private Textbox noregistrasi;
	private Textbox noujian;

	private EventListener eventListener;

	/** @return listener pemilihan calon mahasiswa yang sedang terpasang, boleh {@code null} */
	public EventListener getEventListener() {
		return eventListener;
	}

	/** @param eventListener dipanggil setiap kali user memilih satu calon mahasiswa */
	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	/**
	 * Merender satu baris grid: radio pilih berlabel nama (dibaca dari
	 * {@code kegiatan.getCalonMahasiswa()}), no registrasi, no ujian, dan gabungan nama seluruh
	 * prodi pilihan. Memilih baris menutup popup, menyimpan entity
	 * {@link BiodataCalonMahasiswa} terpilih ke attribute {@code "calonMahasiswa"} pada Bandbox,
	 * mengisi teks tampilan dengan "no registrasi - nama", mengubah id komponen menjadi
	 * {@code "calonmhs_<id>"}, lalu memicu {@link #eventListener} bila terpasang.
	 *
	 * @see AmbilDataCalonMahasiswaCekKesehatanBanbox
	 */
	class CalonMahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			// final BiodataCalonMahasiswa calonMahasiswa =
			// (BiodataCalonMahasiswa) arg1;
			final Kegiatan kegiatan = (Kegiatan) arg1;
			final BiodataCalonMahasiswa calonMahasiswa = kegiatan.getCalonMahasiswa();
			Radio checkbox = new Radio(calonMahasiswa.getNama());
			checkbox.setParent(arg0);arg0.setAttribute("checkbox", checkbox);
			// checkbox.setId(calonMahasiswa.getId() + "");

			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataCalonMahasiswaCekKesehatanBanbox.this.setOpen(false);
					AmbilDataCalonMahasiswaCekKesehatanBanbox.this.setAttribute("calonMahasiswa", calonMahasiswa);
					AmbilDataCalonMahasiswaCekKesehatanBanbox.this
							.setValue(calonMahasiswa.getNoRegistrasi() + " - " + calonMahasiswa.getNama());
					AmbilDataCalonMahasiswaCekKesehatanBanbox.this.setId("calonmhs_" + calonMahasiswa.getId());

					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			new Label(calonMahasiswa.getNoRegistrasi()).setParent(arg0);
			new Label(calonMahasiswa.getNoUjian()).setParent(arg0);

			String prodiPilihan = "";
			if (calonMahasiswa.getProdi1() != null) {
				prodiPilihan += calonMahasiswa.getProdi1().getNama();
			}
			if (calonMahasiswa.getProdi2() != null) {
				prodiPilihan += prodiPilihan.isEmpty() ? calonMahasiswa.getProdi2().getNama()
						: ", " + calonMahasiswa.getProdi2().getNama();
			}
			if (calonMahasiswa.getProdi3() != null) {
				prodiPilihan += prodiPilihan.isEmpty() ? calonMahasiswa.getProdi3().getNama()
						: ", " + calonMahasiswa.getProdi3().getNama();
			}
			if (calonMahasiswa.getProdi4() != null) {
				prodiPilihan += prodiPilihan.isEmpty() ? calonMahasiswa.getProdi4().getNama()
						: ", " + calonMahasiswa.getProdi4().getNama();
			}
			if (calonMahasiswa.getProdi5() != null) {
				prodiPilihan += prodiPilihan.isEmpty() ? calonMahasiswa.getProdi5().getNama()
						: ", " + calonMahasiswa.getProdi5().getNama();
			}
			new Label(prodiPilihan).setParent(arg0);
		}

	}

	/**
	 * Membangun popup pencarian (dipanggil langsung dari constructor): form filter Nama/No
	 * Registrasi/No Ujian, grid hasil bermold "paging", lalu memuat data awal lewat
	 * {@link #onSearchDefault(Event)}.
	 */
	public void display() {
		setReadonly(true);
		setReadonly(true);
		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("1200px");
		bandpopup.setHeight("600px");

		final Radiogroup radiogroup = new Radiogroup();
		radiogroup.setWidth("100%");
		radiogroup.setHeight("100%");
		radiogroup.setParent(bandpopup);

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(radiogroup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Calon Mahasiswa");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama = new Textbox());
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("No Registrasi"));
		row.appendChild(noregistrasi = new Textbox());
		noregistrasi.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("No Ujian"));
		row.appendChild(noujian = new Textbox());
		noujian.setWidth("90%");

		// row = new MyFormRow();
		//		// row.setParent(rows);
		// South south = new South();
		// ais.ui.util.ZkCompat.setFlex(south, true);
		// south.setParent(div);

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

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");grid.setWidth("100%");
		/* setPageSize legacy dihapus: grid bukan mold "paging" sehingga setPageSize melempar IllegalStateException ("Available only the paging mold") dan daftar tidak pernah tampil. Paging ditangani AmbilDataPagingHelper. */
		/* Paging server-side (AmbilDataPagingHelper) menggantikan mold "paging"
		 * client-side yang dibatasi MAX_RESULT. */
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
		column.setLabel("Nama");
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("No Registrasi");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("No Ujian");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Prodi Pilihan");
		column.setWidth("20%");

		onSearchDefault(null);

	}

	/**
	 * Menyusun dan menjalankan kriteria pencarian: mencari {@link JenisKegiatan} bernama
	 * {@code ConstantUtil.PENDAFTARAN_ULANG_MAHASISWA_BARU}, lalu mengambil {@link Kegiatan}
	 * aktif+tervalidasi ({@code validated=1}) dari jenis tersebut yang calon mahasiswanya sudah
	 * punya {@code prodi_lulus} dan cocok filter Nama/No Ujian/No Registrasi (ILIKE ANYWHERE),
	 * dibatasi 100 baris. Mengisi ulang grid dengan hasilnya beserta
	 * {@link CalonMahasiswaRenderer}.
	 *
	 * @param event tidak dipakai, hanya mengikuti signature standar listener pencarian
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();

		System.out.println("onSearchDefault");

		JenisKegiatan jenisKegiatan = (JenisKegiatan) session.createCriteria(JenisKegiatan.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.eq("namaKegiatan", ConstantUtil.PENDAFTARAN_ULANG_MAHASISWA_BARU)).uniqueResult();
		List<Kegiatan> biodataCalonMahasiswa1 = session.createCriteria(Kegiatan.class).add(Restrictions.eq("aktif", true)).add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
				.add(Restrictions.eq("validated", 1)).add(Restrictions.eq("jenisKegiatan", jenisKegiatan))
				.createCriteria("calonMahasiswa").add(Restrictions.neProperty("prodi_lulus", null))
				.add(Restrictions.ilike("nama", nama.getValue(), MatchMode.ANYWHERE))
				.add(Restrictions.ilike("noUjian", noujian.getValue(), MatchMode.ANYWHERE))
				.add(Restrictions.ilike("noRegistrasi", noregistrasi.getValue(), MatchMode.ANYWHERE))

		.setMaxResults(100).list();

		ListModel strset = new SimpleListModel(biodataCalonMahasiswa1);
		grid.setRowRenderer(new CalonMahasiswaRenderer());
		grid.setModelCheckMobile(strset);

	}
}

