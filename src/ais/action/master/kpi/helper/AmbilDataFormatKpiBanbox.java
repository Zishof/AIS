package ais.action.master.kpi.helper;


import ais.common.CommonSearchFilterHelper;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Bandbox;
import org.zkoss.zul.Bandpopup;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
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
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;

import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.kpi.FormatKpi;
import ais.database.model.kpi.FormatKpiDetail;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Tipe khusus untuk ambil data format kpi banbox. Kelas ini memberi nama dan batas tanggung jawab
 * yang eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Bandbox}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyGrid grid}, {@code
 * ais.ui.util.AmbilDataPagingHelper pagingHelper}, {@code Combobox searchfakultas}, {@code Combobox
 * searchjurusan}, {@code Combobox searchyayasan}, {@code Combobox searchsekolah}, {@code
 * AmbilDataSatuanKerjaBanbox searchparent}, {@code EventListener eventListener}; pembacaan/pencarian ({@code
 * onSearchDefault()}, {@code setEventListener()}, {@code getEventListener()}); operasi domain lain ({@code
 * display()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see Bandbox
 */
public class AmbilDataFormatKpiBanbox extends Bandbox {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;

	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private Combobox searchyayasan;
	private Combobox searchsekolah;
	private AmbilDataSatuanKerjaBanbox searchparent;
	private EventListener eventListener;

	protected boolean target = false;

	public AmbilDataFormatKpiBanbox() {
		this(false);
	}

	public AmbilDataFormatKpiBanbox(boolean target) {
		super();
		this.target = target;
		this.addEventListener(Events.ON_OK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				FormatKpi formatKpi = (FormatKpi) HibernateUtil
						.currentSession().createCriteria(FormatKpi.class).add(Restrictions.ilike("nama",
								AmbilDataFormatKpiBanbox.this.getValue().trim(), MatchMode.EXACT))
						.setMaxResults(1).uniqueResult();
				if (formatKpi == null) {
					MyMessageboxConfig.showFormat(
							"Mohon maaf, data Format KPI dengan nama \"{V1}\" tidak ditemukan. Langkah yang dapat dilakukan: (1) periksa kembali penulisan nama; (2) gunakan tombol pencarian untuk memilih data dari daftar; (3) ulangi pencarian.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION,
							AmbilDataFormatKpiBanbox.this.getValue().trim());
					return;
				}
				AmbilDataFormatKpiBanbox.this.setOpen(false);
				AmbilDataFormatKpiBanbox.this.setAttribute("formatKpi", formatKpi);
				AmbilDataFormatKpiBanbox.this.setValue(formatKpi.getNama());
				if (eventListener != null) {
					eventListener.onEvent(arg0);
				}
			}
		});
		try {
			display();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/kpi/helper/AmbilDataFormatKpiBanbox.java:102");
		}

		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (grid == null || grid.getRows() == null || grid.getRows().getChildren() == null
						|| grid.getRows().getChildren().size() == 0) {
					onSearchDefault(null);
				}
			}
		});

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.ambilPegawai() != null) {
			FormatKpi formatKpi = (FormatKpi) ConstantValues.simpleObject(HibernateUtil.currentSession()
					.createCriteria(FormatKpiDetail.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.setProjection(Projections.property("formatKpi.id")).setMaxResults(1)
					.createAlias("formatKpi", "formatKpi").add(Restrictions.eq("formatKpi.aktif", true))
					.add(Restrictions.eq("pegawai", tbmuser.ambilPegawai())), FormatKpi.class, false);

			if (formatKpi != null) {
				AmbilDataFormatKpiBanbox.this.setAttribute("formatKpi", formatKpi);
				AmbilDataFormatKpiBanbox.this.setValue(formatKpi.getNama());
				AmbilDataFormatKpiBanbox.this.setDisabled(true);
			}
		}
	}

	private MyTextbox nama;
	private SatuanKerjaTreeModel satuanKerjaTreeModel;

	/**
	 * Renderer lokal untuk layar/komponen {@link AmbilDataFormatKpiBanbox}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link AmbilDataFormatKpiBanbox} dan dapat mengakses
	 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see AmbilDataFormatKpiBanbox
	 */
	public class FormatKpiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final FormatKpi formatKpi = (FormatKpi) arg1;
			arg0.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataFormatKpiBanbox.this.setOpen(false);
					AmbilDataFormatKpiBanbox.this.setAttribute("formatKpi", formatKpi);
					AmbilDataFormatKpiBanbox.this.setValue(formatKpi.getNama());
					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});
			new Label(formatKpi.getKode()).setParent(arg0);
			new Label(formatKpi.getNama()).setParent(arg0);
			new Label(formatKpi.getKeterangan()).setParent(arg0);

		}

	}

	public void display() throws Exception {

		boolean[] ptYa = Common.chekPtAtauSekolah();
		boolean pt = ptYa[0];
		boolean ya = ptYa[1];

		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("750px");
		bandpopup.setHeight("600px");

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(bandpopup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Format KPI");
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
		searchgrid.setParent(rowUtama);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama")));
		row.appendChild(nama = new MyTextbox());
		nama.setWidth("90%");
		nama.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Satuan Kerja")));
		row.appendChild(searchparent = new AmbilDataSatuanKerjaBanbox());
		searchparent.setWidth("90%");
		searchparent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		MyFormRow hbFakultasLabel = new MyFormRow();
		hbFakultasLabel.setParent(rows);

		hbFakultasLabel.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		hbFakultasLabel.appendChild(searchfakultas = new Combobox());
		searchfakultas.setWidth("90%");

		hbFakultasLabel.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		hbFakultasLabel.appendChild(searchjurusan = new Combobox());
		searchjurusan.setWidth("90%");

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		hbFakultasLabel.setVisible(pt && searchfakultas.getChildren().size() > 1);

		MyFormRow hbYayasan = new MyFormRow();
		hbYayasan.setParent(rows);

		hbYayasan.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));
		hbYayasan.appendChild(searchyayasan = new Combobox());
		searchyayasan.setWidth("90%");

		hbYayasan.appendChild(new ais.ui.util.MyLabelConfig("Sekolah"));
		hbYayasan.appendChild(searchsekolah = new Combobox());
		searchsekolah.setWidth("90%");

		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah, true, false);
		hbYayasan.setVisible(ya);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		Row rowKedua = new Row();
		rowKedua.setParent(rowsUtama);
		toolbar.setHeight("32px");
		toolbar.setParent(rowKedua);

		Toolbarbutton button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});
		button.setParent(toolbar);
		toolbar.appendChild(Common.createCleanButton(this, new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (eventListener != null) {
					try {
						eventListener.onEvent(null);
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
				}
				onSearchDefault(event);
			}
		}));

		// final Radiogroup radiogroup = new Radiogroup();

		grid = new MyGrid();
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

		Column column = new Column();
		column.setParent(columns);
		column.setLabel("Kode");
		column.setWidth("15%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("35%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Keterangan");

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Tbmuser tbmuser = Common.getCurrentUser();

		Session session = HibernateUtil.currentSession();
		List<FormatKpi> formatKpi = session.createCriteria(FormatKpi.class)

				.add(target
						? Restrictions.or(
								Restrictions.ilike("usernamePenggunaTarget", "," + tbmuser.getUserId() + ",",
										MatchMode.ANYWHERE),
								Restrictions.or(Restrictions.isNull("usernamePenggunaTarget"),
										Restrictions.eq("usernamePenggunaTarget", "")))
						: Restrictions.sqlRestriction("true"))

				.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("satuanKerja"),
								Restrictions.or(
										parent == null ? Restrictions.isNull("satuanKerja")
												: Restrictions.sqlRestriction("false"),
										Restrictions.in("satuanKerja", satuanKerjas))))

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false))

				.add(searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
						|| searchsekolah.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false))

				.add(searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
						|| searchyayasan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false))

				.addOrder(Order.desc("id"))
				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))
				.setMaxResults(Common.MAX_RESULT).list();

		System.out.println(formatKpi);
		ListModel strset = new SimpleListModel(formatKpi);
		grid.setRowRenderer(new FormatKpiRenderer());
		grid.setModelCheckMobile(strset);
		grid.renderAll();

	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}
