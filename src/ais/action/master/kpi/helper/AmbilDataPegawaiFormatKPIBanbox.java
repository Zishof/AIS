package ais.action.master.kpi.helper;

import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
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
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Pegawai;
import ais.database.model.StatusPegawai;
import ais.database.model.Tbmuser;
import ais.database.model.kpi.FormatKpiDetail;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Implementasi pola "Bandbox picker" AIS — dengan variasi query non-trivial — untuk memilih
 * {@link ais.database.model.Pegawai} dalam konteks modul KPI (Key Performance Indicator) pegawai.
 * Lihat {@link ais.ui.util.GetEventListener} untuk arsitektur kerangka umum
 * (constructor/display/onSearchDefault/renderer/callback).
 *
 * <p>
 * Berbeda dari kebanyakan subclass {@code AmbilData*Banbox}, query akar {@code onSearchDefault()}
 * BUKAN {@code Criteria(Pegawai.class)} langsung, melainkan {@code Criteria(FormatKpiDetail.class)}
 * dengan {@link org.hibernate.criterion.Projections#property(String) Projections.property("pegawai")}
 * — hasilnya tetap berupa daftar {@code Pegawai}, tapi hanya pegawai yang punya baris
 * {@link ais.database.model.kpi.FormatKpiDetail} pada format KPI yang boleh direalisasikan oleh
 * pengguna yang sedang login (kolom {@code formatKpi.usernamePenggunaRealisasi} memuat userId
 * pengguna dalam daftar dipisah koma, atau kosong/null berarti terbuka untuk semua). Hasil lebih
 * lanjut disaring berdasarkan satuan kerja (satker/unit): defaultnya dibatasi ke satker yang
 * menjadi wewenang pengguna ({@code SekolahUtil.ambilSatuanKerjas()}); bila pengguna memilih satker
 * tertentu lewat {@code searchparent} (sendiri berupa instance
 * {@link ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox} — Bandbox picker bersarang di
 * dalam popup Bandbox ini), cakupan berpindah ke satker itu beserta seluruh anak-satkernya (lewat
 * {@code SatuanKerjaTreeModel}). Field publik {@code punyaBawahan} (diisi pemanggil sebelum popup
 * dibuka) bila tidak kosong membatasi hasil hanya ke id pegawai dalam daftar tersebut (mis. daftar
 * bawahan langsung). Kriteria tambahan pada popup: {@code kodePegawaian} (cocok ke {@code code} atau
 * {@code mycode}), {@code nama} (ILIKE), dan {@code searchstatus} (status kepegawaian aktif). Hanya
 * pegawai dengan {@code aktif = true} atau {@code aktif} null yang ditampilkan.
 * </p>
 * <p>
 * Pemilihan bersifat tunggal: tiap baris memakai {@code MyRadioConfig}, namun — variasi sah dari
 * kerangka referensi — TIDAK dibungkus dalam {@link org.zkoss.zul.Radiogroup} pada
 * {@link #display()}.
 * </p>
 *
 * @see Bandbox
 */
public class AmbilDataPegawaiFormatKPIBanbox extends Bandbox implements GetEventListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;

	private EventListener eventListener;

	/**
	 * Filter opsional berisi daftar id {@link Pegawai}, diisi pemanggil SEBELUM popup dibuka (lewat
	 * konstruksi lalu akses langsung ke field publik ini). Bila tidak kosong, hasil pencarian
	 * dibatasi hanya ke pegawai dengan id dalam daftar ini (mis. daftar bawahan langsung suatu
	 * atasan) — lihat pemakaiannya di {@link #onSearchDefault(Event)}.
	 */
	public List<Long> punyaBawahan = null;

	/**
	 * Mengikuti kerangka standar {@link ais.ui.util.GetEventListener}: {@code setReadonly(true)},
	 * lalu memasang listener {@code onOpen} yang lazy-build popup ({@link #display()}) pada
	 * pembukaan pertama.
	 */
	public AmbilDataPegawaiFormatKPIBanbox() {
		super();
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

	private Textbox kodePegawaian;
	private Textbox nama;
	private Combobox searchstatus;

	/**
	 * Renderer baris grid hasil pencarian pegawai. Mengikuti pola standar
	 * {@link ais.ui.util.GetEventListener}: tiap baris menampilkan foto kecil, kode/mycode/NPWP,
	 * nama pegawai, dan nama satuan kerjanya, plus satu {@code MyRadioConfig} pilihan; memilih baris
	 * menutup popup, menyimpan {@link Pegawai} terpilih ke atribut {@code "pegawai"} dan
	 * {@code "myValue"} pada Bandbox, mengisi teks tampilan Bandbox dengan nama pegawai, lalu
	 * meneruskan event ke {@link #eventListener} bila terpasang.
	 *
	 * @see AmbilDataPegawaiFormatKPIBanbox
	 */
	class PegawaiRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Merender satu baris grid untuk satu {@link Pegawai}: kolom pilihan, foto kecil,
		 * kode/mycode/NPWP, nama pegawai, dan nama satuan kerja.
		 */
		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Pegawai pegawai = (Pegawai) arg1;

			MyRadioConfig checkbox = new MyRadioConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);

			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataPegawaiFormatKPIBanbox.this.setOpen(false);
					AmbilDataPegawaiFormatKPIBanbox.this.setAttribute("pegawai", pegawai);
					AmbilDataPegawaiFormatKPIBanbox.this.setAttribute("myValue", pegawai);
					AmbilDataPegawaiFormatKPIBanbox.this.setValue(pegawai.getNama());
					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});
			CommonMedia.tampilkanGambarKecil(pegawai).setParent(arg0);

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			new Label(pegawai.getCode()).setParent(vbox);
			new Label(pegawai.getMycode()).setParent(vbox);
			new Label(pegawai.getNpwp()).setParent(vbox);

			new Label(pegawai.getNama()).setParent(arg0);
			new Label(pegawai.getSatuanKerja() == null ? "" : pegawai.getSatuanKerja().getNama()).setParent(arg0);

		}

	}

	private AmbilDataSatuanKerjaBanbox searchparent;
	private SatuanKerjaTreeModel satuanKerjaTreeModel;

	/**
	 * Membangun popup pencarian (form kriteria kode/nama/status/satker + tombol Cari + grid hasil,
	 * TANPA pembungkus {@link org.zkoss.zul.Radiogroup}) sekali saat pertama dibuka. Berbeda dari
	 * kerangka standar, pengisian awal grid dijadwalkan lewat {@link Common#createDefaultTimer}
	 * (bukan pemanggilan langsung {@link #onSearchDefault(Event)}) agar {@code searchparent} (Bandbox
	 * bersarang) sempat selesai dirender lebih dulu.
	 *
	 * @throws Exception diteruskan apa adanya dari inisialisasi komponen ZK
	 */
	public void display() throws Exception {
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("800px");
		bandpopup.setHeight("600px");

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(bandpopup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Pegawai");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode/NIP"));
		row.appendChild(kodePegawaian = new Textbox());
		kodePegawaian.setWidth("90%");
		kodePegawaian.addEventListener("onOK", new EventListener() {
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Status"));
		row.appendChild(searchstatus = new Combobox());
		Common.insertComboDanSemua(searchstatus, "nama", StatusPegawai.class, Restrictions.eq("aktif", true));
		searchstatus.setWidth("90%");

		searchstatus.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satker/Unit"));
		row.appendChild(searchparent = new AmbilDataSatuanKerjaBanbox());
		searchparent.setWidth("90%");

		searchparent.setEventListener(new EventListener() {

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

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);
		grid.getPagingChild().setMold("os");
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
		column.setLabel("Kode Pegawai");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama Pegawai");
		column.setWidth("35%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Satuan Kerja");

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	/**
	 * Menjalankan pencarian {@link Pegawai} lewat query akar {@link FormatKpiDetail} (lihat
	 * penjelasan lengkap di Javadoc kelas): menyaring ke format KPI yang boleh direalisasikan
	 * pengguna login, satuan kerja (default wewenang pengguna, atau satker terpilih di
	 * {@code searchparent} beserta anak-satkernya), daftar id {@link #punyaBawahan} bila diisi,
	 * status aktif, serta kecocokan {@code kodePegawaian}/{@code nama}/{@code searchstatus} pada
	 * form. Hasil dipasang ke {@link #grid} lewat {@link PegawaiRenderer}, dibatasi
	 * {@link Common#MAX_RESULT_1000} baris.
	 *
	 * @param event event pemicu (boleh {@code null}, tidak dipakai isinya — hanya sinyal untuk
	 *              menjalankan ulang pencarian)
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear(); satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Tbmuser tbmuser = Common.getCurrentUser();

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(FormatKpiDetail.class).setProjection(Projections.property("pegawai"))
				.createAlias("formatKpi", "formatKpi")

				.add(Restrictions.or(
						Restrictions.ilike("formatKpi.usernamePenggunaRealisasi", "," + tbmuser.getUserId() + ",",
								MatchMode.ANYWHERE),
						Restrictions.or(Restrictions.isNull("formatKpi.usernamePenggunaRealisasi"),
								Restrictions.eq("formatKpi.usernamePenggunaRealisasi", ""))))

				.createCriteria("pegawai").add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(parent==null ? Restrictions.isNull("satuanKerja") : Restrictions.sqlRestriction("false"), Restrictions.in("satuanKerja", satuanKerjas)));

		if (punyaBawahan != null && !punyaBawahan.isEmpty()) {
			criteria.add(Restrictions.in("id", punyaBawahan));
		}

		List<Pegawai> pegawai = criteria
				.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))

				.addOrder(Order.asc("nama"))
				.add(nama.getText().trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("nama", nama.getText().trim(), MatchMode.ANYWHERE))
				.add(kodePegawaian.getText().trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(
								Restrictions.ilike("code", kodePegawaian.getText().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("mycode", kodePegawaian.getText().trim(), MatchMode.ANYWHERE)))
				.add(searchstatus.getSelectedItem() == null || searchstatus.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("statusPegawai", searchstatus.getSelectedItem().getValue()))
				.setMaxResults(Common.MAX_RESULT_1000).list();

		System.out.println(pegawai);
		ListModel strset = new SimpleListModel(pegawai);
		grid.setRowRenderer(new PegawaiRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Memasang listener yang dipanggil setelah pengguna memilih satu pegawai di grid.
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
