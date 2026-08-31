package ais.action.master.helper;

import java.util.Calendar;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
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

import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.GelombangPendaftaran;
import ais.database.model.JenisSeleksi;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Tipe khusus untuk ambil data calon mahasiswa daftar ulang baru banbox. Kelas ini memberi nama
 * dan batas tanggung jawab yang eksplisit pada perilaku yang diwarisi atau kontrak yang
 * diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Bandbox}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyGrid grid}, {@code
 * ais.ui.util.AmbilDataPagingHelper pagingHelper}, {@code ais.ui.util.AmbilDataSortHelper sortHelper}, {@code
 * Textbox nama}, {@code Textbox noregistrasi}, {@code Textbox noujian}, {@code Textbox nim}, {@code Combobox
 * jenisSeleksi}; pembacaan/pencarian ({@code getEventListener()}, {@code setEventListener()}, {@code
 * onSearchDefault()}); operasi domain lain ({@code display()}). Bagian lain dari kontrak tetap mengikuti kelas
 * induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see Bandbox
 */
public class AmbilDataCalonMahasiswaDaftarUlangBaruBanbox extends Bandbox implements GetEventListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;


	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private final ais.ui.util.AmbilDataSortHelper sortHelper = new ais.ui.util.AmbilDataSortHelper();
	public AmbilDataCalonMahasiswaDaftarUlangBaruBanbox() {
		super();
		setWidth("95%");
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
	private Textbox noregistrasi;
	private Textbox noujian;
	private Textbox nim;

	private Combobox jenisSeleksi;
	private Combobox tahun;

	private EventListener eventListener;
	private MyCheckboxConfig lulus;
	private Combobox gelombang;

	public EventListener getEventListener() {
		return eventListener;
	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	class CalonMahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final BiodataCalonMahasiswa calonMahasiswa = (BiodataCalonMahasiswa) arg1;
			CommonMedia.tampilkanGambarKecil(calonMahasiswa).setParent(arg0);
			Radio checkbox = new Radio(calonMahasiswa.getNama());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.setAttribute("mahasiswa", calonMahasiswa);
			// checkbox.setId(calonMahasiswa.getId() + "");

			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataCalonMahasiswaDaftarUlangBaruBanbox.this.setOpen(false);
					AmbilDataCalonMahasiswaDaftarUlangBaruBanbox.this.setAttribute("calonMahasiswa", calonMahasiswa);
					AmbilDataCalonMahasiswaDaftarUlangBaruBanbox.this
							.setValue(calonMahasiswa.getNoRegistrasi() + " - " + calonMahasiswa.getNama());
					AmbilDataCalonMahasiswaDaftarUlangBaruBanbox.this.setId("calonmhs_" + calonMahasiswa.getId());

					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			new Label(calonMahasiswa.getNoRegistrasi()).setParent(arg0);
			new Label(calonMahasiswa.getNoUjian()).setParent(arg0);
			new Label(calonMahasiswa.getNim()).setParent(arg0);

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
			new Label(calonMahasiswa.getProdiLulus() == null ? "" : calonMahasiswa.getProdiLulus().getNama())
					.setParent(arg0);
			new Label(calonMahasiswa.getProgram()).setParent(arg0);
		}

	}

	private MyCheckboxConfig tampilkanYgSudahBayar;
	private MyCheckboxConfig tampilkanYgSudahBayarDaftarUlang;
	private MyCheckboxConfig tampilkanYgSudahLunasDaftarUlang;

	private MyCheckboxConfig tampilkanYgSudahdapatNIM;

	public void display() {
		setReadonly(true);
		setReadonly(true);

		Common.insertCombo(jenisSeleksi = new Combobox(), "nama", JenisSeleksi.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		tahun = new Combobox();
		int tahunCurrent = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		MyComboitemConfig comboitem;
		for (int i = tahunCurrent - 5; i <= tahunCurrent; i++) {
			comboitem = new MyComboitemConfig(i + "");
			comboitem.setValue(i);
			tahun.appendChild(comboitem);
		}

		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("1200px");
		bandpopup.setHeight("600px");
		bandpopup.setSclass("ecampus-zul-picker-popup");

		Radiogroup radiogroup = new Radiogroup();
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

		MyFormRow row = new MyFormRow();
		row.setValign("top");
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

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("No Registrasi"));
		row.appendChild(noregistrasi = new Textbox());
		noregistrasi.setWidth("90%");
		noregistrasi.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("No Ujian"));
		row.appendChild(noujian = new Textbox());
		noujian.setWidth("90%");
		noujian.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("NIM"));
		row.appendChild(nim = new Textbox());
		nim.setWidth("90%");
		nim.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Seleksi"));
		row.appendChild(jenisSeleksi);
		jenisSeleksi.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun"));
		row.appendChild(tahun);
		tahun.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Lulus Seleksi"));
		row.appendChild(lulus = new MyCheckboxConfig(""));
		lulus.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Gelombang"));
		row.appendChild(gelombang = new Combobox());
		Common.insertComboDanSemua(gelombang, new String[] { "nama", "mulai", "sampai", "jenisSeleksi" },
				"tahunAkademik", GelombangPendaftaran.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		gelombang.setWidth("90%");

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

		toolbar.appendChild(tampilkanYgSudahBayar = new MyCheckboxConfig("Bayar Reg."));
		toolbar.appendChild(tampilkanYgSudahBayarDaftarUlang = new MyCheckboxConfig("Bayar Daftar Ulang"));
		toolbar.appendChild(tampilkanYgSudahLunasDaftarUlang = new MyCheckboxConfig("Lunas Daftar Ulang"));
		toolbar.appendChild(tampilkanYgSudahdapatNIM = new MyCheckboxConfig("Dapat NIM"));

		tampilkanYgSudahBayar.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});
		tampilkanYgSudahBayarDaftarUlang.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});
		tampilkanYgSudahLunasDaftarUlang.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});
		tampilkanYgSudahdapatNIM.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");grid.setWidth("100%");
		/* Paging server-side (AmbilDataPagingHelper) menggantikan mold "paging"
		 * client-side yang dibatasi MAX_RESULT_100. */
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
		column.setWidth("70px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("No Registrasi");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("No Ujian");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("NIM");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Prodi Pilihan");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Prodi Lulus");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Program");
		column.setWidth("10%");

		sortHelper.pasang(grid, new EventListener() {
			public void onEvent(Event e) throws Exception {
				onSearchDefault(null);
			}
		}, new String[] { null, "nama", "noRegistrasi", "noUjian", "nim", null, null, "program" });

		onSearchDefault(null);

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(BiodataCalonMahasiswa.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

				.add(gelombang.getSelectedItem() == null || gelombang.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("gelombangPendaftaran", gelombang.getSelectedItem().getValue()))

				.add(lulus.isChecked() ? Restrictions.isNotNull("prodiLulus") : Restrictions.sqlRestriction("true"))

				.add(nama.getText().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", nama.getText().trim(), MatchMode.ANYWHERE))
				.add(nim.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("nim", nim.getValue().trim(), MatchMode.ANYWHERE))
				.add(noregistrasi.getText().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("noRegistrasi", noregistrasi.getValue().trim(), MatchMode.ANYWHERE))

				.add(noujian.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("noUjian", noujian.getValue().trim(), MatchMode.ANYWHERE))

				.add(jenisSeleksi.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("jenisSeleksi", jenisSeleksi.getSelectedItem().getValue()))
				.add(tahun.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tahun", tahun.getSelectedItem().getValue()))

				.add(tampilkanYgSudahdapatNIM.isChecked()
						? Restrictions.and(Restrictions.isNotNull("nim"), Restrictions.ne("nim", ""))
						: Restrictions.sqlRestriction("true"))

				.setMaxResults(Common.MAX_RESULT);

		if (tampilkanYgSudahBayar.isChecked()) {
			criteria.createAlias("pembayaranRegistrasi", "pembayaranRegistrasi", Criteria.LEFT_JOIN)
					.add(tampilkanYgSudahBayar.isChecked() ? Restrictions.gt("pembayaranRegistrasi.amount", 0.1)
							: Restrictions.sqlRestriction("true"));
		}

		if (tampilkanYgSudahBayarDaftarUlang.isChecked() || tampilkanYgSudahLunasDaftarUlang.isChecked()) {
			criteria.createAlias("pembayaranDaftarUlang", "pembayaranDaftarUlang", Criteria.LEFT_JOIN)

					.add(tampilkanYgSudahLunasDaftarUlang.isChecked()
							? Restrictions.eq("pembayaranDaftarUlang.lunas", true)
							: Restrictions.sqlRestriction("true"))

					.add(tampilkanYgSudahBayarDaftarUlang.isChecked()
							? Restrictions.gt("pembayaranDaftarUlang.amount", 0.1)

							: Restrictions.sqlRestriction("true"));
		}

		// ORDER BY dinamis (server-side) sesuai kolom yang diklik; default id desc.
		List<BiodataCalonMahasiswa> biodataCalonMahasiswa = pagingHelper.cariDenganCriteriaUrut(criteria,
				BiodataCalonMahasiswa.class, sortHelper.field("id"), sortHelper.asc(false));

		ListModel strset = new SimpleListModel(biodataCalonMahasiswa);
		grid.setRowRenderer(new CalonMahasiswaRenderer());
		grid.setModelCheckMobile(strset);

	}
}
