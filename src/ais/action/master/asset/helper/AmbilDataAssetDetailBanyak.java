package ais.action.master.asset.helper;

import java.util.ArrayList;
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
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.AssetDetail;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Tipe khusus untuk ambil data asset detail banyak. Kelas ini memberi nama dan batas tanggung
 * jawab yang eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyGrid grid}, {@code
 * ais.ui.util.AmbilDataPagingHelper pagingHelper}, {@code EventListener eventListener}, {@code List
 * assetDetails}, {@code List assetDetailsHanyaDitampilkan}, {@code Set ids}, {@code String tipe}, {@code Boolean
 * bolehDipinjam}; inisialisasi/lifecycle ({@code initCriteria()}); pembacaan/pencarian ({@code
 * onSearchDefault()}, {@code setEventListener()}, {@code getEventListener()}); operasi domain lain ({@code
 * display()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class AmbilDataAssetDetailBanyak extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;
	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;
	private List<AssetDetail> assetDetails;
	private List<AssetDetail> assetDetailsHanyaDitampilkan;

	private Set<Long> ids = new HashSet<Long>();
	private String tipe = null;
	private Boolean bolehDipinjam;

	public AmbilDataAssetDetailBanyak(List<AssetDetail> assetDetails, String tipe) {
		this(assetDetails, tipe, null);
	}

	public AmbilDataAssetDetailBanyak(List<AssetDetail> assetDetails, String tipe, Boolean bolehDipinjam) {
		super();
		this.tipe = tipe;
		this.assetDetails = assetDetails;
		this.bolehDipinjam = bolehDipinjam;

		display();

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	public AmbilDataAssetDetailBanyak(List<AssetDetail> assetDetails, List<AssetDetail> assetDetailsHanyaDitampilkan,
			String tipe, Boolean bolehDipinjam) {
		super();
		this.tipe = tipe;
		this.assetDetails = assetDetails;
		this.assetDetailsHanyaDitampilkan = assetDetailsHanyaDitampilkan;
		this.bolehDipinjam = bolehDipinjam;

		display();

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	private MyTextbox kode;
	private MyTextbox nama;
	private MyTextbox merk;

	class AssetDetailRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {

			arg0.setValign("top");
			// TODO Auto-generated method stub
			final AssetDetail assetDetail = (AssetDetail) arg1;
			arg0.setAttribute("assetDetail", assetDetail);
			final Checkbox checkbox = new Checkbox(assetDetail.getBarcode());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			for (AssetDetail myAssetDetail : assetDetails) {
				if (myAssetDetail.getId().equals(assetDetail.getId())) {
					checkbox.setChecked(true);
					checkbox.setDisabled(true);
					break;
				}
			}

			checkbox.setChecked(ids.contains(assetDetail.getId()));

			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						ids.add(assetDetail.getId());
					} else {
						ids.remove(assetDetail.getId());
					}
				}
			});
			new Label(assetDetail.getNama()).setParent(arg0);
			new Label(assetDetail.getAsset().getMasterAsset().getMerk()).setParent(arg0);
			new Label(assetDetail.getAsset().getMasterAsset().getJenisAsset() == null ? ""
					: assetDetail.getAsset().getMasterAsset().getJenisAsset().getNama()).setParent(arg0);
			new Label(assetDetail.getAsset().getMasterAsset().getTipe()).setParent(arg0);
			new Label(assetDetail.getAsset().getMasterAsset().getKelompokAsset() == null ? ""
					: assetDetail.getAsset().getMasterAsset().getKelompokAsset().getNama()).setParent(arg0);

			new Label(assetDetail.getAsset().getMasterAsset().getSpesifikasi()).setParent(arg0);
			new Label(assetDetail.getStatusAsset() == null ? "" : assetDetail.getStatusAsset().getNama())
					.setParent(arg0);
		}

	}

	public void display() {

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(this);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Barcode Sarpras/Fasilitas");
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
		// FIX toolbar/tombol tidak tampil: pada ZK5 region North memakai tinggi bawaan
		// (+-100px); dengan flex=true isinya diregangkan ke tinggi tersebut sehingga
		// Toolbar yang diletakkan DI BAWAH grid filter ikut terpotong. Disamakan dengan
		// layar sejenis yang sudah benar (DownloadMahasiswa, DownloadKrs, DownloadNilai):
		// flex dimatikan + tinggi eksplisit. Autoscroll sebagai pengaman bila isi bertambah.
		ais.ui.util.ZkCompat.setFlex(north, false);
		north.setHeight("160px");
		north.setAutoscroll(true);

		Div div = new Div();
		div.setParent(north);

		MyGrid searchgrid = new MyGrid();
		searchgrid.setWidth("100%");
		searchgrid.setParent(div);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);

		row.appendChild(new ais.ui.util.MyLabelConfig("Barcode"));
		row.appendChild(kode = new MyTextbox());
		kode.setWidth("90%");

		kode.addEventListener("onOK", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});

		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama = new MyTextbox());
		nama.setWidth("90%");

		nama.addEventListener("onOK", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});

		row.appendChild(new ais.ui.util.MyLabelConfig("Merk"));
		row.appendChild(merk = new MyTextbox());
		merk.setWidth("90%");

		merk.addEventListener("onOK", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
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

		Borderlayout myBorderlayout1 = new ais.ui.util.MyBorderlayout();
		myBorderlayout1.setParent(center);

		Center myCenter1 = new Center();
		ais.ui.util.ZkCompat.setFlex(myCenter1, true);
		myCenter1.setParent(myBorderlayout1);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		/* Paging server-side (AmbilDataPagingHelper) menggantikan mold "paging"
		 * client-side yang dibatasi MAX_RESULT. */
		pagingHelper.pasangOnPaging(new EventListener() {
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
		pagingHelper.pasangGridDanPaging(myCenter1, grid);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Barcode");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Merk");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jenis");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tipe");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kelompok");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Spesifikasi");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Status");

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);

		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				AmbilDataAssetDetailBanyak.this.detach();
			}
		});
		cancel.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		button.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				if (eventListener != null && grid.getRows() != null && grid.getRows().getChildren() != null) {
					List<AssetDetail> assetDetails = new ArrayList<AssetDetail>();
					List<Row> rows = grid.getRows().getChildren();
					for (Row row : rows) {
						try {
							Checkbox checkbox = (Checkbox) row.getAttribute("checkbox");
							if (checkbox != null && checkbox.isChecked() && !checkbox.isDisabled()) {
								AssetDetail myAssetDetail = (AssetDetail) row.getAttribute("assetDetail");
								assetDetails.add(myAssetDetail);
							}
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/AmbilDataAssetDetailBanyak.java:322");
							// TODO: handle exception
						}
					}
					Event myEvent = new Event("myEvent", event.getTarget(), assetDetails);
					eventListener.onEvent(myEvent);
				}
				AmbilDataAssetDetailBanyak.this.detach();
			}
		});
		button.setParent(toolbar);

	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();

		List<Long> values = new ArrayList<Long>();
		if (assetDetailsHanyaDitampilkan != null) {
			for (AssetDetail assetDetail : assetDetailsHanyaDitampilkan) {
				values.add(assetDetail.getId());
			}
		}

		Criteria criteria = session.createCriteria(AssetDetail.class)

				.createAlias("asset", "asset").createAlias("asset.masterAsset", "masterAsset")

				.add(bolehDipinjam == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("masterAsset.bolehDipinjam", bolehDipinjam))

				.add(tipe == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("tipe", tipe))

				.add(ids.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.not(Restrictions.in("id", ids)))
				.add(assetDetailsHanyaDitampilkan == null || values.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.in("id", values))

				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(
								Restrictions.ilike("masterAsset.nama", nama.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE)))

				.add(merk.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("masterAsset.merk", merk.getValue().trim(), MatchMode.ANYWHERE))

				.add(kode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("barcode", kode.getValue().trim(), MatchMode.ANYWHERE))

		;

		if (order) {
			criteria.addOrder(Order.asc("masterAsset.nama"));
		}

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();

		List<AssetDetail> assetDetail = ConstantValues.simpleList(session.createCriteria(AssetDetail.class)
				.createAlias("asset", "asset").createAlias("asset.masterAsset", "masterAsset")
				.add(bolehDipinjam == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("masterAsset.bolehDipinjam", bolehDipinjam))
				.add(tipe == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("masterAsset.tipe", tipe))
				.addOrder(Order.asc("masterAsset.nama"))
				.add(ids.size() == 0 ? Restrictions.sqlRestriction("1!=1") : Restrictions.in("id", ids)),
				AssetDetail.class);

		// Paging server-side ditangani sepenuhnya oleh pagingHelper (hitung total, offset, dan
		// komponen Paging tunggal). Tidak ada lagi paging legacy sehingga tak muncul pager ganda.
		List<AssetDetail> myAssetDetail = pagingHelper.cariDenganCriteria(initCriteria(true), AssetDetail.class);

		assetDetail.addAll(myAssetDetail);

		ListModel strset = new SimpleListModel(assetDetail);
		grid.setRowRenderer(new AssetDetailRenderer());
		grid.setModelCheckMobile(strset);
	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}
