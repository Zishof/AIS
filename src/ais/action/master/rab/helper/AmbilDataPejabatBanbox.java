package ais.action.master.rab.helper;

import java.util.List;

import org.hibernate.Criteria;
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

import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.employ.JenisJabatan;
import ais.database.model.rab.Pejabat;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Tipe khusus untuk ambil data pejabat banbox. Kelas ini memberi nama dan batas tanggung jawab
 * yang eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Bandbox}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyGrid grid}, {@code
 * ais.ui.util.AmbilDataPagingHelper pagingHelper}, {@code EventListener eventListener}, {@code JenisJabatan
 * jenisJabatan}, {@code Textbox nama}; pembacaan/pencarian ({@code onSearchDefault()}, {@code
 * setEventListener()}, {@code getEventListener()}); operasi domain lain ({@code display()}). Bagian lain dari
 * kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see Bandbox
 */
public class AmbilDataPejabatBanbox extends Bandbox implements GetEventListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452451056684904810L;
	private MyGrid grid;


	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;
	// private SatuanKerjaTreeModel satuanKerjaTreeModel;
	private JenisJabatan jenisJabatan;

	public AmbilDataPejabatBanbox() {
		this("");
	}

	public AmbilDataPejabatBanbox(String value) {
		super(value);
		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.hakAkses() != null && tbmuser.hakAkses().getJenisJabatan() != null) {
			setAttribute("pejabat", tbmuser.hakAkses().getJenisJabatan());
			setValue(tbmuser.hakAkses().getJenisJabatan().toString());
			setDisabled(true);
			return;
		} else {
			List<Pejabat> pejabats = Common.getCurrentPejabat(true);
			if (pejabats != null && !pejabats.isEmpty()) {
				setAttribute("pejabat", pejabats.get(0));
				setValue(pejabats.get(0).toString());
				setDisabled(true);
				return;
			}
		}
		// satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
		try {
			display();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/rab/helper/AmbilDataPejabatBanbox.java:83");
		}
	}

	public AmbilDataPejabatBanbox(JenisJabatan jenisJabatan) {
		super();
		this.jenisJabatan = jenisJabatan;

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.hakAkses() != null && tbmuser.hakAkses().getJenisJabatan() != null) {
			setAttribute("pejabat", tbmuser.hakAkses().getJenisJabatan());
			setValue(tbmuser.hakAkses().getJenisJabatan().toString());
			setDisabled(true);
			return;
		} else {

			List<Pejabat> pejabats = Common.getCurrentPejabat(true);
			if (pejabats != null && !pejabats.isEmpty()) {
				setAttribute("pejabat", pejabats.get(0));
				setValue(pejabats.get(0).toString());
				setDisabled(true);
				return;
			}
		}

		// satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
		try {
			display();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/rab/helper/AmbilDataPejabatBanbox.java:111");
		}
	}

	// private Textbox kode;
	private Textbox nama;
	// private AmbilDataSatuanKerjaBanbox satuanKerja;

	/**
	 * Renderer lokal untuk layar/komponen {@link AmbilDataPejabatBanbox}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link AmbilDataPejabatBanbox} dan dapat mengakses
	 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see AmbilDataPejabatBanbox
	 */
	class PejabatRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Pejabat pejabat = (Pejabat) arg1;
			MyRadioConfig checkbox = new MyRadioConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			// checkbox.setId(pejabat.getId() + "");

			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataPejabatBanbox.this.setOpen(false);
					AmbilDataPejabatBanbox.this.setAttribute("pejabat", pejabat);
					AmbilDataPejabatBanbox.this.setAttribute("myValue", pejabat);
					AmbilDataPejabatBanbox.this.setValue(pejabat.getNama());
					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			if (pejabat.getPegawai() != null) {
				CommonMedia.tampilkanGambarKecil(pejabat.getPegawai()).setParent(arg0);
			} else if (pejabat.getDosen() != null) {
				CommonMedia.tampilkanGambarKecil(pejabat.getDosen()).setParent(arg0);
			} else if (pejabat.getGuru() != null) {
				CommonMedia.tampilkanGambarKecil(pejabat.getGuru()).setParent(arg0);
			} else {
				new Label().setParent(arg0);
			}

			new Label(pejabat.getPegawai() == null
					? (pejabat.getDosen() == null ? (pejabat.getGuru() == null ? "" : pejabat.getGuru().getNama())
							: pejabat.getDosen().getCode())
					: pejabat.getPegawai().getCode()).setParent(arg0);

			new Label(pejabat.getPegawai() == null
					? (pejabat.getDosen() == null ? (pejabat.getGuru() == null ? "" : pejabat.getGuru().getNama())
							: pejabat.getDosen().getNama())
					: pejabat.getPegawai().getNama()).setParent(arg0);

			new Label(pejabat.getJenisPengguna().isEmpty() ? "Tidak ditentukan" : pejabat.getJenisPengguna())
					.setParent(arg0);
			new Label(pejabat.getUsernamePengguna().isEmpty() ? "Tidak ditentukan" : pejabat.getUsernamePengguna())
					.setParent(arg0);

			new Label(pejabat.getJenisJabatan() == null ? "" : pejabat.getJenisJabatan().getNama()).setParent(arg0);
			// new Label(pejabat.getSatuanKerja() == null ? "" : pejabat
			// .getSatuanKerja().getNama()).setParent(arg0);
		}

	}

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
		panel.setTitle("Daftar Pejabat");
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
		column.setLabel("");
		column.setWidth("30px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Foto");
		column.setWidth("80px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kode/NIP");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jenis Pengguna");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Username");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jabatan");

		// column = new MyColumnConfig();
		// column.setParent(columns);
		// column.setLabel("Satuan Kerja");

		onSearchDefault(null);

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(Pejabat.class);

		criteria.add(jenisJabatan == null ? Restrictions.sqlRestriction("1=1")
				: Restrictions.eq("jenisJabatan", jenisJabatan)).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

				.add(nama.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE));

		List<Pejabat> pejabat = criteria.setMaxResults(Common.MAX_RESULT).list();
		ListModel strset = new SimpleListModel(pejabat);
		grid.setRowRenderer(new PejabatRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}
