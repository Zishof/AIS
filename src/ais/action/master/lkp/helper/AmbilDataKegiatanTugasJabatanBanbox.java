package ais.action.master.lkp.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
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

import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.lkp.KegiatanTugasJabatan;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Tipe khusus untuk ambil data kegiatan tugas jabatan banbox. Kelas ini memberi nama dan batas
 * tanggung jawab yang eksplisit pada perilaku yang diwarisi atau kontrak yang
 * diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Bandbox}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyGrid grid}, {@code
 * ais.ui.util.AmbilDataPagingHelper pagingHelper}, {@code EventListener eventListener}, {@code boolean
 * bolehPilihParent}, {@code Textbox nama}, {@code AmbilDataSatuanKerjaBanbox satuanKerja}, {@code List
 * tbmroles}; pembacaan/pencarian ({@code onSearchDefault()}, {@code setEventListener()}, {@code
 * getEventListener()}); mutasi data ({@code setSatuanKerja()}); operasi domain lain ({@code display()}). Bagian
 * lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see Bandbox
 */
public class AmbilDataKegiatanTugasJabatanBanbox extends Bandbox {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;


	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;
	private boolean bolehPilihParent;

	public AmbilDataKegiatanTugasJabatanBanbox(boolean bolehPilihParent) {
		super();
		this.bolehPilihParent = bolehPilihParent;
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
	private AmbilDataSatuanKerjaBanbox satuanKerja;
	private List<Tbmrole> tbmroles = null;

	public void setSatuanKerja(SatuanKerja satuanKerja, List<Tbmrole> tbmroles, boolean open) throws Exception {
		this.tbmroles = tbmroles;
		if (getChildren().isEmpty()) {
			display();
		}

		this.satuanKerja.setAttribute("satuanKerja", satuanKerja);
		this.satuanKerja.setAttribute("myValue", satuanKerja);
		this.satuanKerja.setValue(satuanKerja == null ? "" : satuanKerja.getNama());
		this.satuanKerja.setDisabled(satuanKerja != null);

		onSearchDefault(null);

		if (satuanKerja != null && open) {

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					setOpen(true);
				}
			});
		}
	}

	class KegiatanTugasJabatanRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final KegiatanTugasJabatan kegiatanTugasJabatan = (KegiatanTugasJabatan) arg1;

			Radio checkbox = new Radio(kegiatanTugasJabatan.getNama());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);

			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataKegiatanTugasJabatanBanbox.this.setOpen(false);
					AmbilDataKegiatanTugasJabatanBanbox.this.setAttribute("kegiatanTugasJabatan", kegiatanTugasJabatan);
					AmbilDataKegiatanTugasJabatanBanbox.this.setAttribute("myValue", kegiatanTugasJabatan);
					AmbilDataKegiatanTugasJabatanBanbox.this.setValue((kegiatanTugasJabatan.getNama()));
					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			RevisiHelper.createNewRevisi(KegiatanTugasJabatan.class, kegiatanTugasJabatan,
					kegiatanTugasJabatan.getSatuanKerja() == null ? ""
							: kegiatanTugasJabatan.getSatuanKerja().getNama())
					.setParent(arg0);

			new Label(Common.numberFormat.get().format(kegiatanTugasJabatan.getAngkaKredit())).setParent(arg0);

			Hbox hbox = new Hbox();
			hbox.setParent(arg0);
			new Label(Common.numberFormat.get().format(kegiatanTugasJabatan.getKuantitasDefault())).setParent(hbox);

			new Label(kegiatanTugasJabatan.getSatuanKuantitas() == null ? ""
					: kegiatanTugasJabatan.getSatuanKuantitas().getNama()).setParent(hbox);

			new Label(Common.numberFormat.get().format(kegiatanTugasJabatan.getKualitasDefault())).setParent(arg0);

			hbox = new Hbox();
			hbox.setParent(arg0);
			new Label(Common.numberFormat.get().format(kegiatanTugasJabatan.getWaktuDefault())).setParent(hbox);

			new Label(kegiatanTugasJabatan.getSatuanWaktu()).setParent(hbox);
		}

	}

	public void display() throws Exception {
		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("700px");
		bandpopup.setHeight("600px");

		Radiogroup radiogroup = new Radiogroup();
		radiogroup.setWidth("100%");
		radiogroup.setHeight("100%");
		radiogroup.setParent(bandpopup);

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(radiogroup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Tugas jabatan (Jobdesk)");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan / Unit Kerja (*)"));
		row.appendChild(satuanKerja = new AmbilDataSatuanKerjaBanbox());
		satuanKerja.setWidth("90%");
		satuanKerja.setReadonly(true);
		satuanKerja.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Kegiatan"));
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

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");grid.setWidth("100%");
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
		column.setLabel("Nama Kegiatan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Satuan/Unit Kerja");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Angka Kredit");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kuantitas");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kualitas");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Waktu");
		column.setWidth("10%");

		onSearchDefault(null);

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser.ambilPegawai() != null) {
			if (tbmroles == null || tbmroles.isEmpty()) {
				tbmroles = new ArrayList<Tbmrole>();
				tbmroles.add(tbmuser.hakAkses());
			}
		}

		Criterion criterion = satuanKerja.getAttribute("satuanKerja") == null ? Restrictions.sqlRestriction("false")
				: Restrictions.eq("satuanKerja", satuanKerja.getAttribute("satuanKerja"));

		Session session = HibernateUtil.currentSession();

		List<Long> longs = bolehPilihParent ? new ArrayList<Long>()
				: session.createCriteria(KegiatanTugasJabatan.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.isNotNull("induk")).setProjection(Projections.groupProperty("induk.id"))
						.list();

		List<KegiatanTugasJabatan> kegiatanTugasJabatan = session.createCriteria(KegiatanTugasJabatan.class)

				.add(bolehPilihParent || longs.isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.not(Restrictions.in("id", longs)))

				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.asc("nama"))
				.add(nama.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))

				.add(Restrictions.or(criterion,
						tbmroles == null || tbmroles.isEmpty() ? Restrictions.sqlRestriction("false")
								: Restrictions.or(Restrictions.and(criterion, Restrictions.isNull("userRole")),
										Restrictions.in("userRole", tbmroles))))

				.setMaxResults(Common.MAX_RESULT).list();

		System.out.println(kegiatanTugasJabatan);
		ListModel strset = new SimpleListModel(kegiatanTugasJabatan);
		grid.setRowRenderer(new KegiatanTugasJabatanRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}
