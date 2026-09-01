package ais.action.master.rab.helper;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import ais.ui.util.MyColumnConfig;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import ais.ui.util.MyRadioConfig;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import ais.ui.util.MyToolbarbuttonConfig;

import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.rab.Mitra;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.GetEventListener;

/**
 * Implementasi pola "Bandbox picker" AIS untuk entity {@link ais.database.model.rab.Mitra} — lihat
 * {@link ais.ui.util.GetEventListener} untuk arsitektur kerangka umum (constructor/display/onSearchDefault/
 * renderer/callback).
 *
 * <p>Mitra adalah rekanan/vendor yang menjadi pihak ketiga dalam transaksi pengadaan pada modul RAB
 * (mis. penerima pembayaran/rekening tujuan). Popup pencarian menyediakan kriteria {@code kode} dan
 * {@code nama} ({@code ilike ANYWHERE}) ditambah filter non-trivial berupa {@link
 * AmbilDataSatuanKerjaBanbox} bertindak sebagai sub-picker Satuan Kerja: bila satker dipilih,
 * {@code onSearchDefault(Event)} memakai {@link SatuanKerjaTreeModel#getChildsSet} untuk mengumpulkan
 * satker terpilih beserta seluruh unit turunannya, lalu membatasi hasil Mitra dengan
 * {@code Restrictions.in("satuanKerja", satuanKerjas)}; bila tidak ada satker terpilih, filter ini
 * no-op lewat idiom {@code Restrictions.sqlRestriction("1=1")}. Baris grid menampilkan kode, nama,
 * alamat, dan nomor rekening lewat {@code MitraRenderer}; hasil dibungkus {@link
 * org.zkoss.zul.Radiogroup} sehingga pemilihan bersifat tunggal.</p>
 *
 * <p><b>Constructor:</b> {@code AmbilDataMitraBanbox()} mendelegasikan ke
 * {@code AmbilDataMitraBanbox(String value)} dengan nilai awal kosong; overload ini meneruskan
 * {@code value} ke {@code super(Bandbox)} sebagai teks tampilan awal (bukan filter dari entity induk)
 * dan menginisialisasi {@code satuanKerjaTreeModel}. Pemanggilan {@link #display()} dibungkus
 * try-catch dengan audit otomatis ({@code ErrorAuditUtil.record}) pada catch kosong, bukan bagian dari
 * kerangka standar (kebanyakan subclass lain tidak membungkus {@code display()} sama sekali).</p>
 *
 * @see Bandbox
 */
public class AmbilDataMitraBanbox extends Bandbox implements GetEventListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452451056684904810L;
	private MyGrid grid;

	private EventListener eventListener;
	private SatuanKerjaTreeModel satuanKerjaTreeModel;

	/**
	 * Delegasi ke {@link #AmbilDataMitraBanbox(String)} dengan teks tampilan awal kosong.
	 */
	public AmbilDataMitraBanbox() {
		this("");
	}

	/**
	 * Membangun Bandbox picker Mitra dengan teks tampilan awal {@code value}, menyiapkan
	 * {@code satuanKerjaTreeModel} untuk penelusuran hierarki satker, lalu memanggil
	 * {@link #display()} (dibungkus try-catch beraudit — lihat Javadoc kelas).
	 *
	 * @param value teks awal yang ditampilkan pada Bandbox sebelum pengguna memilih Mitra
	 */
	public AmbilDataMitraBanbox(String value) {
		super(value);
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
		try {
			display();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/rab/helper/AmbilDataMitraBanbox.java:65");
		}
	}

	private Textbox kode;
	private Textbox nama;
	private AmbilDataSatuanKerjaBanbox satuanKerja;

	/**
	 * Renderer satu baris grid hasil pencarian Mitra: menampilkan radio pilihan, kode, nama, alamat,
	 * dan nomor rekening. Listener {@code onCheck} pada radio adalah satu-satunya titik callback pola
	 * ini — lihat penjelasan umum di {@link ais.ui.util.GetEventListener}.
	 *
	 * @see AmbilDataMitraBanbox
	 */
	class MitraRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final Mitra mitra = (Mitra) arg1;
			MyRadioConfig checkbox = new MyRadioConfig();
			checkbox.setParent(arg0);arg0.setAttribute("checkbox", checkbox);
			// checkbox.setId(mitra.getId() + "");

			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataMitraBanbox.this.setOpen(false);
					AmbilDataMitraBanbox.this.setAttribute("mitra", mitra);
					AmbilDataMitraBanbox.this.setAttribute("myValue", mitra);
					AmbilDataMitraBanbox.this.setValue(mitra.getNama());
					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			new Label(mitra.getKode()).setParent(arg0);
			new Label(mitra.getNama()).setParent(arg0);
			new Label(mitra.getAlamat()).setParent(arg0);
			new Label(mitra.getRek()).setParent(arg0);
		}

	}

	/**
	 * Merakit popup pencarian Mitra: form kriteria kode/nama plus sub-picker Satuan Kerja
	 * ({@link AmbilDataSatuanKerjaBanbox}, dengan listener yang memicu {@link #onSearchDefault(Event)}
	 * ulang saat satker dipilih), tombol Cari, dan grid hasil dalam {@link org.zkoss.zul.Radiogroup}
	 * pilih-tunggal; diakhiri memanggil {@link #onSearchDefault(Event)} agar grid terisi saat popup
	 * pertama tampil.
	 *
	 * @throws Exception diteruskan dari pembangunan komponen ZK
	 * @see ais.ui.util.GetEventListener
	 */
	public void display() throws Exception {
		setReadonly(true);

		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("600px");
		bandpopup.setHeight("600px");

		final Radiogroup radiogroup = new Radiogroup();
		radiogroup.setWidth("100%");
		radiogroup.setHeight("100%");
		radiogroup.setParent(bandpopup);

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(radiogroup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Mitra");
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

		MyGrid searchgrid = new MyGrid();searchgrid.setWidth("100%");
		searchgrid.setParent(rowUtama);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode"));
		row.appendChild(kode = new Textbox());
		kode.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama = new Textbox());
		nama.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		row.appendChild(this.satuanKerja = new AmbilDataSatuanKerjaBanbox());
		satuanKerja.setWidth("90%");
		satuanKerja.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
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

		toolbar.appendChild(Common.createCleanButton(this, this));

		grid = new MyGrid();//grid.setOddRowSclass("non-odd");grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);grid.getPagingChild().setMold("os");
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
		column.setLabel("Kode");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Alamat");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Rek.");

		onSearchDefault(null);

	}

	/**
	 * Menjalankan pencarian Mitra berdasar {@code kode} dan {@code nama} ({@code ilike ANYWHERE}),
	 * ditambah filter scoping satker: bila sub-picker {@link #satuanKerja} memiliki nilai terpilih,
	 * hasil dibatasi ke satker tersebut beserta seluruh unit turunannya (lewat
	 * {@link SatuanKerjaTreeModel#getChildsSet}); bila kosong, filter satker no-op. Maksimum
	 * {@code Common.MAX_RESULT} baris, urut nama menaik, lalu grid diisi ulang dengan
	 * {@link MitraRenderer}.
	 *
	 * @param event event pemicu; boleh {@code null} (dipanggil juga dari {@link #display()} dan dari
	 *     listener sub-picker satker)
	 * @see ais.ui.util.GetEventListener
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		SatuanKerja parent = (SatuanKerja) satuanKerja
				.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear(); satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(Mitra.class);

		criteria.addOrder(Order.asc("nama"))
				.add(Restrictions.ilike("nama", nama.getText().trim(),
						MatchMode.ANYWHERE))
				.add(Restrictions.ilike("kode", kode.getText().trim(),
						MatchMode.ANYWHERE))
				.add(satuanKerjas.size() == 0 ? Restrictions
						.sqlRestriction("1=1") : Restrictions.in("satuanKerja",
						satuanKerjas));
		List<Mitra> mitra = criteria.setMaxResults(Common.MAX_RESULT).list();

		// // System.out.println(mitra);
		ListModel strset = new SimpleListModel(mitra);
		grid.setRowRenderer(new MitraRenderer());
		grid.setModelCheckMobile(strset);

		

	}

	/**
	 * {@inheritDoc}
	 *
	 * @see ais.ui.util.GetEventListener
	 */
	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see ais.ui.util.GetEventListener
	 */
	public EventListener getEventListener() {
		return eventListener;
	}
}
