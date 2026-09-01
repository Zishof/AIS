package ais.action.master.kursus.helper;

import java.util.List;

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
import org.zkoss.zul.Div;
import org.zkoss.zul.Hbox;
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
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.file.LampiranLain;
import ais.database.model.kursus.KomponenDataProdukKursus;
import ais.database.model.kursus.KomponenProdukKursus;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Implementasi pola "Bandbox picker" AIS untuk entity
 * {@link ais.database.model.kursus.KomponenDataProdukKursus} — lihat
 * {@link ais.ui.util.GetEventListener} untuk arsitektur kerangka umum
 * (constructor/display/onSearchDefault/renderer/callback). Pada modul kursus, satu
 * {@link ais.database.model.kursus.KomponenProdukKursus} (mis. tipe komponen "Pembelajaran Jarak
 * Jauh"/"Pembelajaran Tatap Muka") bisa punya beberapa baris {@code KomponenDataProdukKursus} —
 * varian data (harga, jadwal, jumlah pertemuan) untuk komponen tersebut. Constructor WAJIB diberi
 * parameter {@code komponenProdukKursus} (kode jenis komponen dari entity induk, salah satu
 * konstanta {@link KomponenProdukKursus}) — ini bukan filter opsional seperti pada subclass lain,
 * melainkan syarat wajib karena kelas ini tidak punya constructor tanpa argumen.
 *
 * <p>
 * Pencarian memakai satu kotak teks {@code nama} (dicocokkan ILIKE ke kolom {@code kode} maupun
 * {@code nama} sekaligus, digabung {@code OR}). Hasil selalu disaring persis ke
 * {@code komponenProdukKursus} milik constructor dan hanya baris dengan {@code aktif = true} atau
 * {@code aktif} null. Kolom "Jml.Pertemuan" dan "Mulai" pada grid hanya ditampilkan (lebar > 0) bila
 * {@code komponenProdukKursus} bertipe {@link KomponenProdukKursus#PEMBELAJARAN_JARAK_JAUH} atau
 * {@link KomponenProdukKursus#PEMBELAJARAN_TATAP_MUKA} — untuk tipe lain kolom itu disembunyikan
 * (lebar diset "0px") karena datanya tidak relevan. Baris grid juga menampilkan komponen revisi
 * ({@link RevisiHelper#createNewRevisi}) dan unggah/unduh lampiran
 * ({@link LampiranLain#createDownloadUploadFileLain}) — bukan sekadar label teks seperti kebanyakan
 * subclass lain. Pemilihan bersifat tunggal, memakai {@link org.zkoss.zul.Radio} langsung (bukan
 * {@code MyRadioConfig}) berlabel kode, dibungkus {@link org.zkoss.zul.Radiogroup}.
 * </p>
 *
 * @see Bandbox
 */
public class AmbilDataKomponenDataProdukKursusBanbox extends Bandbox implements GetEventListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;

	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private String komponenProdukKursus;
	private EventListener eventListener;

	/**
	 * Mengikuti kerangka standar {@link ais.ui.util.GetEventListener}: {@code setReadonly(true)},
	 * lalu memasang listener {@code onOpen} yang lazy-build popup ({@link #display()}) pada
	 * pembukaan pertama.
	 *
	 * @param komponenProdukKursus kode jenis {@link KomponenProdukKursus} (dari entity induk) yang
	 *                             menjadi syarat wajib pencarian — bukan filter opsional
	 */
	public AmbilDataKomponenDataProdukKursusBanbox(String komponenProdukKursus) {
		super();
		this.komponenProdukKursus = komponenProdukKursus;
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

	/**
	 * Renderer baris grid hasil pencarian komponen data produk kursus. Mengikuti pola standar
	 * {@link ais.ui.util.GetEventListener}: tiap baris menampilkan {@link org.zkoss.zul.Radio}
	 * berlabel kode; memilih radio menutup popup, menyimpan {@link KomponenDataProdukKursus}
	 * terpilih ke atribut {@code "komponenDataProdukKursus"} pada Bandbox, mengisi teks tampilan
	 * Bandbox lewat {@code toString()}, lalu meneruskan event ke {@link #eventListener} bila
	 * terpasang. Berbeda dari renderer sejenis lainnya, baris ini juga merender komponen revisi
	 * ({@link RevisiHelper#createNewRevisi}) dan unggah/unduh lampiran
	 * ({@link LampiranLain#createDownloadUploadFileLain}), bukan sekadar label.
	 *
	 * @see AmbilDataKomponenDataProdukKursusBanbox
	 */
	class KomponenDataProdukKursusRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Merender satu baris grid untuk satu {@link KomponenDataProdukKursus}: radio pilihan, blok
		 * nama+revisi+lampiran, harga (atau label "Default" bila {@code hargaIkutDefault}), jumlah
		 * pertemuan, tanggal mulai, dan keterangan.
		 */
		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final KomponenDataProdukKursus komponenDataProdukKursus = (KomponenDataProdukKursus) arg1;
			Radio checkbox = new Radio(komponenDataProdukKursus.getKode());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);

			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataKomponenDataProdukKursusBanbox.this.setOpen(false);
					AmbilDataKomponenDataProdukKursusBanbox.this.setAttribute("komponenDataProdukKursus",
							komponenDataProdukKursus);
					AmbilDataKomponenDataProdukKursusBanbox.this.setValue(komponenDataProdukKursus.toString());

					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			Vbox a;
			(a = RevisiHelper.createNewRevisi(KomponenDataProdukKursus.class, komponenDataProdukKursus,
					komponenDataProdukKursus.getNama())).setParent(arg0);
			Hbox hbox = new Hbox();
			hbox.setParent(a);
			LampiranLain.createDownloadUploadFileLain(hbox, komponenDataProdukKursus.getId(),
					KomponenDataProdukKursus.class.getName(), komponenProdukKursus, false, null, null, false, false,
					false, false);
			new Label(komponenDataProdukKursus.getHargaIkutDefault() ? "Default"
					: Common.numberFormat.get().format(komponenDataProdukKursus.getHarga())).setParent(arg0);
			new Label(Common.numberFormat.get().format(komponenDataProdukKursus.getJumlahPertemuan())).setParent(arg0);
			new Label(Common.dateFormat1.get().format(komponenDataProdukKursus.getMulai())).setParent(arg0);
			new Label(komponenDataProdukKursus.getKeterangan()).setParent(arg0);

		}

	}

	/**
	 * Membangun popup pencarian (form kriteria kode/nama + tombol Cari + grid hasil dibungkus
	 * {@link org.zkoss.zul.Radiogroup}) sekali saat pertama dibuka, termasuk menyembunyikan kolom
	 * "Jml.Pertemuan"/"Mulai" bila {@code komponenProdukKursus} bukan tipe jarak jauh/tatap muka,
	 * lalu memanggil {@link #onSearchDefault(Event)} agar grid langsung terisi. Mengikuti kerangka
	 * standar {@link ais.ui.util.GetEventListener}.
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
		panel.setTitle("Daftar Komponen Produk");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode/Nama"));
		row.appendChild(nama = new Textbox());
		nama.setWidth("90%");

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
		column.setLabel("Nama");
		column.setWidth("40%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Harga");
		column.setAlign("right");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jml.Pertemuan");
		column.setWidth(komponenProdukKursus.equals(KomponenProdukKursus.PEMBELAJARAN_JARAK_JAUH)
				|| komponenProdukKursus.equals(KomponenProdukKursus.PEMBELAJARAN_TATAP_MUKA) ? "15%" : "0px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Mulai");
		column.setWidth(komponenProdukKursus.equals(KomponenProdukKursus.PEMBELAJARAN_JARAK_JAUH)
				|| komponenProdukKursus.equals(KomponenProdukKursus.PEMBELAJARAN_TATAP_MUKA) ? "15%" : "0px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");

		onSearchDefault(null);

	}

	/**
	 * Menjalankan pencarian {@link KomponenDataProdukKursus} yang persis milik
	 * {@code komponenProdukKursus} dari constructor, dengan {@code aktif = true} atau
	 * {@code aktif} null, disaring lebih lanjut oleh kecocokan {@code kode}/{@code nama} terhadap
	 * teks {@code nama} pada form. Hasil diurutkan berdasarkan nama dan dipasang ke {@link #grid}
	 * lewat {@link KomponenDataProdukKursusRenderer}, dibatasi {@link Common#MAX_RESULT_1000} baris.
	 *
	 * @param event event pemicu (boleh {@code null}, tidak dipakai isinya — hanya sinyal untuk
	 *              menjalankan ulang pencarian)
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();
		List<KomponenDataProdukKursus> komponenDataProdukKursus = session.createCriteria(KomponenDataProdukKursus.class)

						.add(Restrictions.eq("komponenProdukKursus", komponenProdukKursus))

						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.addOrder(Order.asc("nama"))
						.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(
										Restrictions.ilike("kode", nama.getValue().trim(), MatchMode.ANYWHERE),
										Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))

						)

						.setMaxResults(Common.MAX_RESULT_1000)

						.list();

		System.out.println(komponenDataProdukKursus);
		ListModel strset = new SimpleListModel(komponenDataProdukKursus);
		grid.setRowRenderer(new KomponenDataProdukKursusRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Memasang listener yang dipanggil setelah pengguna memilih satu komponen data produk kursus
	 * di grid.
	 *
	 * @param eventListener listener baru yang akan dipasang
	 */
	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	/**
	 * @return listener yang saat ini terpasang, atau {@code null} bila belum diset
	 */
	public EventListener getEventListener() {
		return eventListener;
	}
}
