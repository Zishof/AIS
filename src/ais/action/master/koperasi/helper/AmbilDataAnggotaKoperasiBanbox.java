package ais.action.master.koperasi.helper;

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

import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.koperasi.AnggotaKoperasi;
import ais.database.model.koperasi.Koperasi;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Implementasi pola "Bandbox picker" AIS untuk entity
 * {@link ais.database.model.koperasi.AnggotaKoperasi} — lihat {@link ais.ui.util.GetEventListener}
 * untuk arsitektur kerangka umum (constructor/display/onSearchDefault/renderer/callback).
 * {@code AnggotaKoperasi} adalah anggota koperasi pegawai (bisa juga berperan sebagai pelanggan
 * dalam transaksi koperasi) pada modul koperasi kampus.
 *
 * <p>
 * Pencarian memakai dua kotak teks — {@code kode} dan {@code nama} (masing-masing ILIKE ke kolom
 * sejenisnya bila diisi) — serta combobox {@code koperasi} untuk membatasi ke koperasi tertentu
 * (daftar koperasi pada combobox sendiri sudah disaring {@code aktif = true} atau {@code aktif}
 * null). Hasil pencarian selalu disaring ke anggota dengan {@code aktif = true} atau {@code aktif}
 * null, diurutkan berdasarkan nama, dan dibatasi {@link Common#MAX_RESULT_50} baris. Setiap baris
 * grid menampilkan foto kecil anggota ({@link CommonMedia#tampilkanGambarKecil}), nama, nama
 * koperasi, telepon, dan jenis anggota. Pemilihan bersifat tunggal (baris dibungkus
 * {@link org.zkoss.zul.Radiogroup}). Constructor dua-argumen menerima parameter {@code Boolean}
 * yang saat ini tidak dipakai isinya di badan constructor — hanya menyediakan overload terpisah
 * dari constructor tanpa argumen.
 * </p>
 *
 * @see Bandbox
 */
public class AmbilDataAnggotaKoperasiBanbox extends Bandbox implements GetEventListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452451056684904810L;
	private MyGrid grid;


	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;

	/**
	 * Constructor default — sama dengan memanggil {@link #AmbilDataAnggotaKoperasiBanbox(Boolean)}
	 * dengan {@code true} (nilai parameter tidak dipakai isinya di badan constructor).
	 */
	public AmbilDataAnggotaKoperasiBanbox() {
		this(true);
	}

	/**
	 * Mengikuti kerangka standar {@link ais.ui.util.GetEventListener}: {@code setReadonly(true)},
	 * atur tooltip/style tampilan Bandbox, lalu memasang listener {@code onOpen} yang lazy-build
	 * popup ({@link #display()}) pada pembukaan pertama.
	 *
	 * @param notDeafault parameter pembeda overload; nilainya tidak dipakai di badan constructor ini
	 */
	public AmbilDataAnggotaKoperasiBanbox(Boolean notDeafault) {
		super();
		setReadonly(true);
		setTooltiptext("Klik untuk memilih anggota koperasi");
		setStyle("border-radius:8px;border:1px solid #cbd5e1;padding:4px 8px;background:white;");

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

	private Textbox kode;
	private Textbox nama;
	private Combobox koperasi;

	/**
	 * Renderer baris grid hasil pencarian anggota koperasi. Mengikuti pola standar
	 * {@link ais.ui.util.GetEventListener}: tiap baris menampilkan foto kecil, nama anggota, nama
	 * koperasi, telepon, jenis anggota, plus satu radio button; memilih radio menutup popup,
	 * menyimpan {@link AnggotaKoperasi} terpilih ke atribut {@code "anggotaKoperasi"} dan
	 * {@code "myValue"} pada Bandbox, mengisi teks tampilan Bandbox dengan nama anggota, lalu
	 * meneruskan event ke {@link #eventListener} bila terpasang.
	 *
	 * @see AmbilDataAnggotaKoperasiBanbox
	 */
	class AnggotaKoperasiRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Merender satu baris grid untuk satu {@link AnggotaKoperasi}: kolom checkbox/radio pilihan,
		 * foto kecil, nama anggota beserta nama koperasinya, telepon, dan jenis anggota.
		 */
		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final AnggotaKoperasi anggotaKoperasi = (AnggotaKoperasi) arg1;
			MyRadioConfig checkbox = new MyRadioConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);

			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataAnggotaKoperasiBanbox.this.setOpen(false);
					AmbilDataAnggotaKoperasiBanbox.this.setAttribute("anggotaKoperasi", anggotaKoperasi);
					AmbilDataAnggotaKoperasiBanbox.this.setAttribute("myValue", anggotaKoperasi);
					AmbilDataAnggotaKoperasiBanbox.this.setValue(anggotaKoperasi.getNama());
					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});
			CommonMedia.tampilkanGambarKecil(anggotaKoperasi).setParent(arg0);

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
//			new Label(anggotaKoperasi.getKode()).setParent(vbox);
			new Label(anggotaKoperasi.getNama()).setParent(vbox);
			new Label(anggotaKoperasi.getKoperasi() == null ? "" : anggotaKoperasi.getKoperasi().getNama())
					.setParent(vbox);
			new Label(anggotaKoperasi.getTelp()).setParent(arg0);
			new Label(anggotaKoperasi.getJenisAnggotaKoperasi() == null ? ""
					: anggotaKoperasi.getJenisAnggotaKoperasi().getNama()).setParent(arg0);

		}

	}

	/**
	 * Membangun popup pencarian (form kriteria kode/nama/koperasi + tombol Cari + grid hasil
	 * dibungkus {@link org.zkoss.zul.Radiogroup}) sekali saat pertama dibuka, lalu memanggil
	 * {@link #onSearchDefault(Event)} agar grid langsung terisi. Mengikuti kerangka standar
	 * {@link ais.ui.util.GetEventListener}.
	 */
	public void display() {

		setReadonly(true);

		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth(Common.isMobile() ? "96%" : "850px");
		bandpopup.setHeight(Common.isMobile() ? "520px" : "430px");

		Radiogroup radiogroup = new Radiogroup();
		radiogroup.setWidth("100%");
		radiogroup.setHeight("100%");
		radiogroup.setParent(bandpopup);

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(radiogroup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Pilih Anggota Koperasi / Pelanggan");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode"));
		row.appendChild(kode = new Textbox());
		kode.setWidth("90%");

		kode.addEventListener("onOK", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama = new Textbox());
		nama.setWidth("90%");

		nama.addEventListener("onOK", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Koperasi"));
		row.appendChild(koperasi = new Combobox());
		koperasi.setWidth("90%");
		Common.insertComboDanSemua(koperasi, "nama", Koperasi.class,
				Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));
		koperasi.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
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

		grid = new MyGrid();
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
		column.setLabel("");
		column.setWidth("30px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("70px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kode/Nama");
		column.setWidth("50%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Telp");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jenis");

		onSearchDefault(null);

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(AnggotaKoperasi.class)
				.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));

		if (koperasi != null && koperasi.getSelectedItem() != null && koperasi.getSelectedItem().getValue() != null) {
			criteria.add(Restrictions.eq("koperasi", koperasi.getSelectedItem().getValue()));
		}
		if (nama != null && nama.getValue() != null && !nama.getValue().trim().isEmpty()) {
			criteria.add(Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE));
		}
		if (kode != null && kode.getValue() != null && !kode.getValue().trim().isEmpty()) {
			criteria.add(Restrictions.ilike("kode", kode.getValue().trim(), MatchMode.ANYWHERE));
		}
		criteria.addOrder(Order.asc("nama"));

		List<AnggotaKoperasi> anggotaKoperasi = criteria.setMaxResults(Common.MAX_RESULT_50).list();
		ListModel strset = new SimpleListModel(anggotaKoperasi);
		grid.setRowRenderer(new AnggotaKoperasiRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}
