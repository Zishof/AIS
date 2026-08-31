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
 * Tipe khusus untuk ambil data calon mahasiswa cek kesehatan banbox. Kelas ini memberi nama dan
 * batas tanggung jawab yang eksplisit pada perilaku yang diwarisi atau kontrak yang
 * diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Bandbox}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyGrid grid}, {@code
 * ais.ui.util.AmbilDataPagingHelper pagingHelper}, {@code Textbox nama}, {@code Textbox noregistrasi}, {@code
 * Textbox noujian}, {@code EventListener eventListener}; pembacaan/pencarian ({@code getEventListener()}, {@code
 * setEventListener()}, {@code onSearchDefault()}); operasi domain lain ({@code display()}). Bagian lain dari
 * kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see Bandbox
 */
public class AmbilDataCalonMahasiswaCekKesehatanBanbox extends Bandbox implements GetEventListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;

	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	public AmbilDataCalonMahasiswaCekKesehatanBanbox() {
		super();
		display();
	}

	private Textbox nama;
	private Textbox noregistrasi;
	private Textbox noujian;

	private EventListener eventListener;

	public EventListener getEventListener() {
		return eventListener;
	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

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

