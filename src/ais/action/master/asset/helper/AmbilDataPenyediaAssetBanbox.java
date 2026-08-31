package ais.action.master.asset.helper;

import java.util.List;

import org.hibernate.Criteria;
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
import org.zkoss.zul.Columns;
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

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.PenyediaAsset;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Tipe khusus untuk ambil data penyedia asset banbox. Kelas ini memberi nama dan batas tanggung
 * jawab yang eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Bandbox}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyGrid grid}, {@code
 * ais.ui.util.AmbilDataPagingHelper pagingHelper}, {@code EventListener eventListener}, {@code MyTextbox nama},
 * {@code MyTextbox jenis}, {@code MyTextbox kategori}; pembacaan/pencarian ({@code onSearchDefault()}, {@code
 * setEventListener()}, {@code getEventListener()}); operasi domain lain ({@code display()}, {@code
 * buildCriteria()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see Bandbox
 */
public class AmbilDataPenyediaAssetBanbox extends Bandbox {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;
	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;

	public AmbilDataPenyediaAssetBanbox() {
		super();
		this.addEventListener(Events.ON_OK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (getValue().trim().equals("")) {
					setAttribute("penyediaAsset", null);
					setValue("");
					return;
				}

				PenyediaAsset penyediaAsset = (PenyediaAsset)

				ConstantValues.simpleObject(HibernateUtil.currentSession().createCriteria(PenyediaAsset.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

						.add(Restrictions.ilike("kode", AmbilDataPenyediaAssetBanbox.this.getValue().trim(),
								MatchMode.EXACT))
						.setMaxResults(1), PenyediaAsset.class);
				if (penyediaAsset == null) {
					MyMessageboxConfig.show(
							"PenyediaAsset dengan kode = " + AmbilDataPenyediaAssetBanbox.this.getValue().trim()
									+ " tidak dpenyediaAssetukan",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}
				AmbilDataPenyediaAssetBanbox.this.setOpen(false);
				AmbilDataPenyediaAssetBanbox.this.setAttribute("penyediaAsset", penyediaAsset);
				AmbilDataPenyediaAssetBanbox.this.setValue(penyediaAsset.getNama());
				if (eventListener != null) {
					eventListener.onEvent(arg0);
				}
			}
		});

		display();

		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (grid == null || grid.getRows() == null || grid.getRows().getChildren() == null
						|| grid.getRows().getChildren().size() == 0) {
					onSearchDefault(null);
				}
			}
		});
	}

	private MyTextbox nama;
	private MyTextbox jenis;
	private MyTextbox kategori;

	class PenyediaAssetRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PenyediaAsset penyediaAsset = (PenyediaAsset) arg1;
			new Label(penyediaAsset.getNama()).setParent(arg0);
			arg0.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataPenyediaAssetBanbox.this.setOpen(false);
					AmbilDataPenyediaAssetBanbox.this.setAttribute("penyediaAsset", penyediaAsset);
					AmbilDataPenyediaAssetBanbox.this.setValue(penyediaAsset.getNama());
					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			new Label(penyediaAsset.getJenisPenyediaAsset() == null ? ""
					: penyediaAsset.getJenisPenyediaAsset().getNama()).setParent(arg0);
			new Label(penyediaAsset.getKategoriPenyediaAsset() == null ? ""
					: penyediaAsset.getKategoriPenyediaAsset().getNama()).setParent(arg0);

			String alamat = penyediaAsset.getAlamat();
			if (penyediaAsset.getKecamatan() != null) {
				String c = "Kec." + penyediaAsset.getKecamatan().getNama();
				alamat += alamat.isEmpty() ? c : ", " + c;
			}
			if (penyediaAsset.getKota() != null) {
				String c = "Kab/Kota." + penyediaAsset.getKota().getNama();
				alamat += alamat.isEmpty() ? c : ", " + c;
			}
			if (penyediaAsset.getPropinsi() != null) {
				String c = "Prop." + penyediaAsset.getPropinsi().getNama();
				alamat += alamat.isEmpty() ? c : ", " + c;
			}
			if (!penyediaAsset.getKodePos().trim().isEmpty()) {
				String c = "Kode Pos " + penyediaAsset.getKodePos();
				alamat += alamat.isEmpty() ? c : ", " + c;
			}
			if (!penyediaAsset.getTelp().trim().isEmpty()) {
				String c = "Telp. " + penyediaAsset.getTelp();
				alamat += alamat.isEmpty() ? c : ", " + c;
			}
			if (!penyediaAsset.getFax().trim().isEmpty()) {
				String c = "Fax. " + penyediaAsset.getFax();
				alamat += alamat.isEmpty() ? c : ", " + c;
			}
			if (!penyediaAsset.getKontak().trim().isEmpty()) {
				String c = "Kontak. " + penyediaAsset.getKontak();
				alamat += alamat.isEmpty() ? c : ", " + c;
			}
			if (!penyediaAsset.getEmail().trim().isEmpty()) {
				String c = "Email. " + penyediaAsset.getEmail();
				alamat += alamat.isEmpty() ? c : ", " + c;
			}

			new MyLabelKecil(alamat).setParent(arg0);

			String jenis = "";
			if (penyediaAsset.getJenisPekerjaanPenyedia1() != null) {
				jenis += jenis.isEmpty() ? penyediaAsset.getJenisPekerjaanPenyedia1().getNama()
						: ", " + penyediaAsset.getJenisPekerjaanPenyedia1().getNama();
			}
			if (penyediaAsset.getJenisPekerjaanPenyedia2() != null) {
				jenis += jenis.isEmpty() ? penyediaAsset.getJenisPekerjaanPenyedia2().getNama()
						: ", " + penyediaAsset.getJenisPekerjaanPenyedia2().getNama();
			}
			if (penyediaAsset.getJenisPekerjaanPenyedia3() != null) {
				jenis += jenis.isEmpty() ? penyediaAsset.getJenisPekerjaanPenyedia3().getNama()
						: ", " + penyediaAsset.getJenisPekerjaanPenyedia3().getNama();
			}
			if (penyediaAsset.getJenisPekerjaanPenyedia4() != null) {
				jenis += jenis.isEmpty() ? penyediaAsset.getJenisPekerjaanPenyedia4().getNama()
						: ", " + penyediaAsset.getJenisPekerjaanPenyedia4().getNama();
			}
			if (penyediaAsset.getJenisPekerjaanPenyedia5() != null) {
				jenis += jenis.isEmpty() ? penyediaAsset.getJenisPekerjaanPenyedia5().getNama()
						: ", " + penyediaAsset.getJenisPekerjaanPenyedia5().getNama();
			}
			new MyLabelKecil(jenis).setParent(arg0);

		}

	}

	public void display() {
		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("1100px");
		bandpopup.setHeight("600px");

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(bandpopup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Penyedia / Perusahaan");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(panelchildren);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, false);
		north.setHeight("130px");
		north.setAutoscroll(true);

		Div div = new Div();
		div.setParent(north);

		MyGrid searchgrid = new MyGrid();
		searchgrid.setWidth("100%");
		searchgrid.setParent(div);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode/Nama/Pemilik"));
		row.appendChild(nama = new MyTextbox());
		nama.setWidth("90%");
		nama.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis"));
		row.appendChild(jenis = new MyTextbox());
		jenis.setWidth("90%");
		jenis.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kategori"));
		row.appendChild(kategori = new MyTextbox());
		kategori.setWidth("90%");
		kategori.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		Toolbar toolbar = new Toolbar();
		ais.ui.util.BanboxFilterToggle.pasang(north, searchgrid, toolbar);
		// toolbar.setHeight("25px");
		toolbar.setParent(div);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
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

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");grid.setWidth("100%");
		/* Paging server-side (AmbilDataPagingHelper) menggantikan mold "paging"
		 * client-side yang dibatasi MAX_RESULT_100. */
		pagingHelper.pasangOnPaging(new EventListener() {
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
		pagingHelper.pasangGridDanPaging(center, grid);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jenis");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kategori");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Alamat");
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Pekerjaan");
		column.setWidth("15%");

	}

	/** Bangun Criteria filter (tanpa ORDER BY) agar bisa dipakai untuk count maupun data. */
	private Criteria buildCriteria(Session session) {
		Criteria criteria = session.createCriteria(PenyediaAsset.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.ilike("kode", nama.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.or(
										Restrictions.ilike("pemilik", nama.getValue().trim(), MatchMode.ANYWHERE),
										Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))));
		if (!jenis.getValue().trim().isEmpty()) {
			criteria.createAlias("jenisPenyediaAsset", "jenisPenyediaAsset")
					.add(Restrictions.ilike("jenisPenyediaAsset.nama", jenis.getValue().trim(), MatchMode.ANYWHERE));
		}
		if (!kategori.getValue().trim().isEmpty()) {
			criteria.createAlias("kategoriPenyediaAsset", "kategoriPenyediaAsset")
					.add(Restrictions.ilike("kategoriPenyediaAsset.nama", kategori.getValue().trim(), MatchMode.ANYWHERE));
		}
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Session session = HibernateUtil.currentSession();

		// Hitung total untuk kontrol paging
		Object totalObj = buildCriteria(session).setProjection(Projections.rowCount()).uniqueResult();
		int totalData = totalObj == null ? 0 : ((Number) totalObj).intValue();
		pagingHelper.getPaging().setTotalSize(totalData);

		int activePage = pagingHelper.getPaging().getActivePage();
		if (activePage * pagingHelper.getPageSize() >= totalData) {
			activePage = 0;
			pagingHelper.getPaging().setActivePage(0);
		}

		// Ambil satu halaman data langsung dengan ORDER BY nama — aman tanpa DISTINCT
		// karena PenyediaAsset tidak punya LEFT JOIN yang menghasilkan duplikat baris.
		List<PenyediaAsset> penyediaAsset = (List<PenyediaAsset>) buildCriteria(session)
				.addOrder(Order.asc("nama"))
				.setFirstResult(activePage * pagingHelper.getPageSize())
				.setMaxResults(pagingHelper.getPageSize())
				.list();

		ListModel strset = new SimpleListModel(penyediaAsset);
		grid.setRowRenderer(new PenyediaAssetRenderer());
		grid.setModelCheckMobile(strset);
	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}
