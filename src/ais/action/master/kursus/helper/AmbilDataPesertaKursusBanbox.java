package ais.action.master.kursus.helper;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Bandbox;
import org.zkoss.zul.Bandpopup;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
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
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.kursus.PesertaKursus;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Implementasi pola "Bandbox picker" AIS untuk entity {@link ais.database.model.kursus.PesertaKursus}
 * — lihat {@link ais.ui.util.GetEventListener} untuk arsitektur kerangka umum
 * (constructor/display/onSearchDefault/renderer/callback). Peserta kursus adalah orang yang terdaftar
 * mengikuti suatu kursus di modul Kursus AIS, dengan atribut kode/no. registrasi, nama, email, dan jenis
 * peserta.
 *
 * <p>Konstruktor melakukan prefill: bila pengguna yang sedang login ({@link Common#getCurrentUser()})
 * memiliki data {@code PesertaKursus} miliknya sendiri, entity itu langsung dipasang sebagai atribut
 * {@code pesertaKursus}/{@code myValue} pada Bandbox sebelum popup dibuka. Pencarian memakai dua
 * {@code Textbox}: {@code nama} (ilike ke kolom {@code nama}) dan {@code noregistrasi} (berlabel "Kode",
 * ilike ke kolom {@code kode}), keduanya opsional dan digabung AND; hanya peserta aktif
 * ({@code aktif} null atau {@code true}) yang disertakan, diurutkan ascending berdasar id, dibatasi
 * {@code Common.MAX_RESULT_20} baris. Hasil ditampilkan dalam {@link Radiogroup} (pilih-tunggal via
 * {@link Radio}, yang di ZK merupakan subclass {@link Checkbox} sehingga kompatibel dengan header
 * "select all" pada kolom Nama — meski kombinasi checkbox-header dan radiogroup-body ini tidak lazim,
 * dampaknya hanya baris terakhir yang dicentang oleh "select all" yang efektif terpilih karena batasan
 * radiogroup). Tidak ada parameter constructor tambahan.</p>
 *
 * @see Bandbox
 */
public class AmbilDataPesertaKursusBanbox extends Bandbox implements GetEventListener {

	/**
	 *
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;


	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();

	/**
	 * Membangun Bandbox read-only, mem-prefill pilihan dari {@code PesertaKursus} milik pengguna yang
	 * sedang login (bila ada), lalu memasang listener {@code onOpen} standar: popup dibangun lazy
	 * (sekali saja) via {@link #display()}, kemudian dibuka lewat {@link Common#createDefaultTimer}.
	 */
	public AmbilDataPesertaKursusBanbox() {
		super();

		setReadonly(true);

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.getPesertaKursus() != null) {
			setAttribute("pesertaKursus", tbmuser.getPesertaKursus());
			setAttribute("myValue", tbmuser.getPesertaKursus());
		}

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
	private EventListener eventListener;

	/**
	 * @return listener aktif saat ini, atau {@code null} bila belum diset
	 */
	public EventListener getEventListener() {
		return eventListener;
	}

	/**
	 * Menetapkan listener yang dipanggil setelah baris peserta kursus dipilih.
	 *
	 * @param eventListener listener baru yang akan dipasang
	 */
	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	/**
	 * Merender satu baris grid hasil pencarian peserta kursus: label Kode, Nama, Email, dan Jenis
	 * Peserta, plus satu {@link Radio} pilihan di kolom pertama. Saat radio dicentang ({@code onCheck}),
	 * popup ditutup, entity {@link PesertaKursus} terpilih disimpan lewat
	 * {@code setAttribute("pesertaKursus"/"myValue", ...)} dan teks tampilan Bandbox diisi nama peserta,
	 * lalu {@link #eventListener} (bila terpasang) diberi tahu — mengikuti pola callback standar yang
	 * dijelaskan di {@link ais.ui.util.GetEventListener}.
	 *
	 * @see AmbilDataPesertaKursusBanbox
	 */
	class PesertaKursusRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PesertaKursus pesertaKursus = (PesertaKursus) arg1;
			Radio checkbox = new Radio(pesertaKursus.getNama());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.setAttribute("mahasiswa", pesertaKursus);
			// checkbox.setId(pesertaKursus.getId() + "");

			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataPesertaKursusBanbox.this.setOpen(false);
					AmbilDataPesertaKursusBanbox.this.setAttribute("pesertaKursus", pesertaKursus);
					AmbilDataPesertaKursusBanbox.this.setAttribute("myValue", pesertaKursus);
					AmbilDataPesertaKursusBanbox.this.setValue(pesertaKursus.getNama());

					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			new Label(pesertaKursus.getKode()).setParent(arg0);
			new Label(pesertaKursus.getNama()).setParent(arg0);

			new Label(pesertaKursus.getEmail()).setParent(arg0);
			new Label(pesertaKursus.getJenisPeserta() == null ? "" : pesertaKursus.getJenisPeserta().getNama())
					.setParent(arg0);
		}

	}

	/**
	 * Membangun isi {@link Bandpopup} sekali: panel judul "Daftar Peserta Kursus" berisi form
	 * pencarian ({@code Textbox nama}, {@code Textbox noregistrasi}) + tombol Cari/Bersihkan, kolom
	 * Nama dilengkapi checkbox header "select all", dan grid hasil dibungkus {@link Radiogroup}
	 * (pilih-tunggal). Diakhiri memanggil {@link #onSearchDefault(Event)} dengan {@code null} agar
	 * grid langsung terisi saat popup pertama dibuka.
	 */
	public void display() {
		setReadonly(true);

		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("900px");
		bandpopup.setHeight("600px");

		final Radiogroup radiogroup = new Radiogroup();
		radiogroup.setWidth("100%");
		radiogroup.setHeight("100%");
		radiogroup.setParent(bandpopup);

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(radiogroup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Peserta Kursus");
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
		//
		//
		//
		//

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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode"));
		row.appendChild(noregistrasi = new Textbox());
		noregistrasi.setWidth("90%");

		// row = new MyFormRow();
		//		// row.setParent(rows);
		// row.appendChild(new ais.ui.util.MyLabelConfig("No Ujian"));
		// row.appendChild(noujian = new Textbox());
		// noujian.setWidth("90%");

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
		final Checkbox checkbox = new Checkbox("Nama");
		column.appendChild(checkbox);
		checkbox.addEventListener(Events.ON_CHECK, new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				List<Row> rows = grid.getRows().getChildren();
				for (Row row : rows) {
					try {
						Checkbox myCheckbox = (Checkbox) row.getAttribute("checkbox");
						myCheckbox.setChecked(!myCheckbox.isDisabled() && checkbox.isChecked());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/kursus/helper/AmbilDataPesertaKursusBanbox.java:256");

					}
				}
			}
		});

		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kode");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Email");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jenis");

		onSearchDefault(null);

	}

	/**
	 * Menjalankan pencarian {@link PesertaKursus} yang aktif ({@code aktif} null atau {@code true}),
	 * difilter opsional lewat {@code Textbox nama} (ilike ke kolom {@code nama}) dan
	 * {@code Textbox noregistrasi} (ilike ke kolom {@code kode}), keduanya digabung AND bila diisi,
	 * diurutkan ascending berdasar id dan dibatasi {@code Common.MAX_RESULT_20} baris. Hasil dipasang
	 * ke {@link #grid} lewat {@link PesertaKursusRenderer} dan {@code SimpleListModel}.
	 *
	 * @param event event pemicu (boleh {@code null}, mis. saat dipanggil dari {@link #display()})
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();

		System.out.println("onSearchDefault");

		List<PesertaKursus> pesertaKursus = session.createCriteria(PesertaKursus.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.asc("id"))
				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))
				.add(noregistrasi.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("kode", noregistrasi.getValue().trim(), MatchMode.ANYWHERE))
				// .add(Restrictions.ilike("noUjian",
				// noujian.getText().trim(),
				// MatchMode.ANYWHERE)).setMaxResults(Common.MAX_RESULT)
				.setMaxResults(Common.MAX_RESULT_20).list();
		ListModel strset = new SimpleListModel(pesertaKursus);
		grid.setRowRenderer(new PesertaKursusRenderer());
		grid.setModelCheckMobile(strset);

	}
}
