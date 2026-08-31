package ais.action.master.helper;


import ais.common.CommonSearchFilterHelper;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
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
import org.zkoss.zul.Vbox;

import ais.action.master.MatakuliahPrasyaratAction;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Kurikulum;
import ais.database.model.Matakuliah;
import ais.database.model.Perkuliahan;
import ais.database.model.Program;
import ais.database.model.Tbmuser;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Tipe khusus untuk ambil data perkuliahan bandbox. Kelas ini memberi nama dan batas tanggung
 * jawab yang eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Bandbox}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyGrid grid}, {@code
 * ais.ui.util.AmbilDataPagingHelper pagingHelper}, {@code Textbox kodeMk}, {@code Textbox namaMk}, {@code
 * Combobox searchfakultas}, {@code Combobox jurusanCombobox}, {@code Combobox programCombobox}, {@code Combobox
 * semesterBox}; pembacaan/pencarian ({@code onSearchDefault()}, {@code getEventListener()}, {@code
 * setEventListener()}); operasi domain lain ({@code display()}). Bagian lain dari kontrak tetap mengikuti kelas
 * induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see Bandbox
 */
public class AmbilDataPerkuliahanBandbox extends Bandbox {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4630526859031545820L;
	private MyGrid grid;


	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private Textbox kodeMk;
	private Textbox namaMk;
	private Combobox searchfakultas = new Combobox();
	private Combobox jurusanCombobox = new Combobox();
	private Combobox programCombobox = new Combobox();
	private Combobox semesterBox;
	private Integer semesterPendek;

	private Combobox tahunAjaran;

	private EventListener eventListener;
	private AmbilDataDosenBanbox dosen1;
	private Combobox hari;

	public AmbilDataPerkuliahanBandbox() {
		super();
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

	public AmbilDataPerkuliahanBandbox(Integer semesterPendek) {
		super();
		this.semesterPendek = semesterPendek;
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

	class MatakuliahRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row row, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final Perkuliahan perkuliahan = (Perkuliahan) arg1;
			final Matakuliah matakuliah = perkuliahan.getMatakuliah();
			if (perkuliahan == null || matakuliah == null)
				return;

			MyRadioConfig checkbox = new MyRadioConfig();
			checkbox.setParent(row);
			row.setValign("top");row.setAttribute("checkbox", checkbox);
			checkbox.setAttribute("perkuliahan", perkuliahan);

			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					AmbilDataPerkuliahanBandbox.this.setOpen(false);
					AmbilDataPerkuliahanBandbox.this.setAttribute("perkuliahan", perkuliahan);
					AmbilDataPerkuliahanBandbox.this.setAttribute("myValue", perkuliahan);
					AmbilDataPerkuliahanBandbox.this.setValue(Common.getDeskripsiPerkuliahan(perkuliahan));

					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			Session session = HibernateUtil.currentSession();
			Integer jumlahUdahMasuk = KrsUtilHelper.ambilJumlahDetailperkuliahan(session, perkuliahan, false);

			new Label(perkuliahan.getMatakuliah().getKode()).setParent(row);
			Vbox vbox = new Vbox();
			vbox.setParent(row);
			new ais.ui.util.MyHtml(perkuliahan.getMatakuliah().getKode() + " - " + perkuliahan.getMatakuliah().getNama()
					+ (perkuliahan.getMerupakan_paralel() != null && perkuliahan.getMerupakan_paralel()
							? " <font style='font-weight:bold;color:blue;'>(Paralel)</font>"
							: ""))
					.setParent(vbox);
			MatakuliahPrasyaratAction.tampilPrasyarat(vbox, perkuliahan.getMatakuliah());
			Kurikulum kurikulum = perkuliahan.getKurikulum();
			new Label(kurikulum == null ? "" : "Kurikulum : " + kurikulum.getNama()).setParent(vbox);

			new Label(perkuliahan.getMatakuliah().getSks() + "").setParent(row);
			ais.action.master.helper.PerkuliahanUIHelper.displayDosenPerkuliahan(row, perkuliahan, false);
			new Label((perkuliahan.getHari() == null ? "" : perkuliahan.getHari())).setParent(row);

			new Label(perkuliahan.getSemester() == null ? "" : perkuliahan.getSemester() + "").setParent(row);

			new Label(perkuliahan.getKelas()).setParent(row);
			new Label((perkuliahan.getWaktuMulai() == null ? "" : perkuliahan.getWaktuMulai()) + "-"
					+ (perkuliahan.getWaktuSelesai() == null ? "" : perkuliahan.getWaktuSelesai())).setParent(row);
			new Label(perkuliahan.getRuang() == null ? "" : perkuliahan.getRuang().getKodeRuangan()).setParent(row);
			new Label(jumlahUdahMasuk + "").setParent(row);

		}

	}

	public void display() {

		setReadonly(true);

		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("850px");
		bandpopup.setHeight("500px");

		final Radiogroup radiogroup = new Radiogroup();
		radiogroup.setWidth("100%");
		radiogroup.setHeight("100%");
		radiogroup.setParent(bandpopup);

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(radiogroup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Perkuliahan");
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

		Columns columns = new Columns();
		columns.setParent(searchgrid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("15%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setWidth("15%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setWidth("15%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		Common.insertCombo(searchfakultas, new String[] { "nama", "kode" }, Fakultas.class,
				Restrictions.eq("aktif", true));

		class SearchFakultasEventListener implements EventListener {

			@Override
			public void onEvent(Event event) throws Exception {
				Common.clear(jurusanCombobox);
				jurusanCombobox.setSelectedItem(null);
				if (searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null) {
					return;
				}
				Common.insertCombo(jurusanCombobox, "nama", Jurusan.class,
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false));
			}

		}

		searchfakultas.addEventListener("onChange", new SearchFakultasEventListener());

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		Tbmuser tbmuser = Common.getCurrentUser();
		Common.selectComboItem(searchfakultas, tbmuser.ambilFakultas());
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.setWidth("90%");

		searchfakultas.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);

			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Matakuliah"));
		row.appendChild(kodeMk = new Textbox());
		kodeMk.setWidth("90%");

		kodeMk.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);

			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dosen"));
		row.appendChild(dosen1 = new AmbilDataDosenBanbox());

		if (Common.getCurrentUser().getDosen() != null) {
			dosen1.setAttribute("dosen", Common.getCurrentUser().getDosen());
			dosen1.setAttribute("myValue", Common.getCurrentUser().getDosen());
			dosen1.setValue(Common.getCurrentUser().getDosen().getNama());
		}

		dosen1.setWidth("90%");
		dosen1.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		Common.insertCombo(jurusanCombobox, "nama", Jurusan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
				Restrictions.eq("fakultas", tbmuser.ambilFakultas()));
		Common.selectComboItem(jurusanCombobox, tbmuser.ambilJurusan());
		row.appendChild(jurusanCombobox);
		jurusanCombobox.setWidth("90%");

		jurusanCombobox.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);

			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		Common.insertCombo(programCombobox, "namaBaru", Program.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Program programselected = tbmuser.ambilProgram();
		Common.selectComboItem(programCombobox, programselected);
		row.appendChild(programCombobox);
		programCombobox.setWidth("90%");
		programCombobox.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);

			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Hari"));
		row.appendChild(hari = new Combobox());
		hari.setWidth("90%");
		hari.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);

			}
		});

		for (String h : Common.haris) {
			MyComboitemConfig comboitem = new MyComboitemConfig();
			comboitem.setLabel(h);
			comboitem.setValue(h);
			hari.appendChild(comboitem);
		}
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel("Semua");
		comboitem.setValue(null);
		hari.appendChild(comboitem);
		hari.setReadonly(true);
		hari.setSelectedItem(comboitem);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Matakuliah"));
		row.appendChild(namaMk = new Textbox());
		namaMk.setWidth("90%");
		namaMk.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);

			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		row.appendChild(semesterBox = new Combobox());

		comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		semesterBox.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		semesterBox.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Semua");
		comboitem.setValue(null);
		semesterBox.appendChild(comboitem);
		semesterBox.setSelectedItem(comboitem);
		semesterBox.setReadonly(true);

		// int maxSemesterPilihan = 25;
		// try {
		// maxSemesterPilihan = Integer
		// .parseInt(Common.getKonfigurasi("max_semester_pilihan",
		// "25").getNilai().trim());
		// } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataPerkuliahanBandbox.java:420");
		//
		// }

		// for (int i = 1; i < maxSemesterPilihan; i++) {
		// comboitem = new org.zkoss.zul.Comboitem();
		// comboitem.setLabel(i + "");
		// comboitem.setValue(i);
		// semesterBox.appendChild(comboitem);
		// }
		semesterBox.setWidth("90%");
		semesterBox.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);

			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(tahunAjaran = new Combobox());
		tahunAjaran = Common.generateTahunAjaran(tahunAjaran);
		tahunAjaran.setWidth("90%");
		tahunAjaran.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);

			}
		});

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

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");grid.setWidth("100%");
		/* Paging server-side (AmbilDataPagingHelper) menggantikan mold "paging"
		 * client-side yang dibatasi MAX_RESULT_100. */
		Row rowKetiga = new Row();
		rowKetiga.setParent(rowsUtama);
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.getPagingChild().setMold("os");
		grid.setParent(rowKetiga);

		columns = new Columns();

		columns.setParent(grid);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.appendChild(new Label());
		column.setWidth("40px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kode MK");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Mata Kuliah");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("SKS");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Dosen");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Hari");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Smt");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kelas");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Waktu");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Ruang");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jml Mhs");
		column.setWidth("5%");

		onSearchDefault(null);

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();

		Criterion criterion = dosen1.getAttribute("myValue") == null ? Restrictions.sqlRestriction("1=1")
				: Restrictions.or(Restrictions.eq("dosen1", dosen1.getAttribute("myValue")),
						Restrictions.eq("dosen2", dosen1.getAttribute("myValue")));

		criterion = Restrictions.or(criterion, Restrictions.eq("dosen3", dosen1.getAttribute("myValue")));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen4", dosen1.getAttribute("myValue")));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen5", dosen1.getAttribute("myValue")));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen6", dosen1.getAttribute("myValue")));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen7", dosen1.getAttribute("myValue")));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen8", dosen1.getAttribute("myValue")));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen9", dosen1.getAttribute("myValue")));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen10", dosen1.getAttribute("myValue")));

		List<Perkuliahan> matakuliah = session.createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

				.add(criterion)

				.add(hari.getSelectedItem() == null || hari.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("hari", hari.getSelectedItem().getValue()))

				.add(semesterPendek == null ? Restrictions.isNull("statusSemesterPendek")
						: Restrictions.eq("statusSemesterPendek", semesterPendek))

				.createAlias("jurusan", "jurusan")
				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", searchfakultas, false))
				.add(jurusanCombobox.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: CommonSearchFilterHelper.eqSelectedWithId("jurusan", jurusanCombobox, false))

				.add(programCombobox.getSelectedItem() == null || programCombobox.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("program", programCombobox.getSelectedItem().getValue()))

				.add(semesterBox.getSelectedItem() == null || semesterBox.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("ganjilGenap", semesterBox.getSelectedItem().getValue()))

				.add(tahunAjaran.getSelectedItem() == null || tahunAjaran.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tahunAjaran", tahunAjaran.getSelectedItem().getValue()))
				.add(Restrictions.or(Restrictions.eq("merupakan_paralel", false),
						Restrictions.isNull("merupakan_paralel")))
				.createAlias("matakuliah", "matakuliah")

				.addOrder(Order.asc("matakuliah.nama"))
				.add(kodeMk.getText().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("matakuliah.kode", kodeMk.getText().trim(), MatchMode.ANYWHERE))
				.add(namaMk.getText().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("matakuliah.nama", namaMk.getText().trim(), MatchMode.ANYWHERE))
				.setMaxResults(Common.MAX_RESULT_20).list();

		ListModel strset = new SimpleListModel(matakuliah);
		grid.setRowRenderer(new MatakuliahRenderer());
		grid.setModelCheckMobile(strset);

	}

	public EventListener getEventListener() {
		return eventListener;
	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}
}
