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
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import org.zkoss.zul.Hbox;
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
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.PenerimaanPengadaanMasterAsset;
import ais.database.model.asset.PenyediaAsset;
import ais.database.model.file.LampiranLain;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Tipe khusus untuk ambil data penerimaan pengadaan master asset banyak. Kelas ini memberi nama
 * dan batas tanggung jawab yang eksplisit pada perilaku yang diwarisi atau kontrak yang
 * diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyGrid grid}, {@code
 * ais.ui.util.AmbilDataPagingHelper pagingHelper}, {@code EventListener eventListener}, {@code List
 * penerimaanPengadaanMasterAssets}, {@code List penerimaanPengadaanMasterAssetsHanyaDitampilkan}, {@code Set
 * ids}, {@code PenyediaAsset penyediaAsset}, {@code MyTextbox kode}; inisialisasi/lifecycle ({@code
 * initCriteria()}); pembacaan/pencarian ({@code onSearchDefault()}, {@code setEventListener()}, {@code
 * getEventListener()}); operasi domain lain ({@code display()}). Bagian lain dari kontrak tetap mengikuti kelas
 * induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class AmbilDataPenerimaanPengadaanMasterAssetBanyak extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;
	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;
	private List<PenerimaanPengadaanMasterAsset> penerimaanPengadaanMasterAssets;
	private List<PenerimaanPengadaanMasterAsset> penerimaanPengadaanMasterAssetsHanyaDitampilkan;

	private Set<Long> ids = new HashSet<Long>();
	private PenyediaAsset penyediaAsset;

	public AmbilDataPenerimaanPengadaanMasterAssetBanyak(
			List<PenerimaanPengadaanMasterAsset> penerimaanPengadaanMasterAssets, PenyediaAsset penyediaAsset) {
		super();
		this.penerimaanPengadaanMasterAssets = penerimaanPengadaanMasterAssets;
		this.penyediaAsset = penyediaAsset;

		display();

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	public AmbilDataPenerimaanPengadaanMasterAssetBanyak(
			List<PenerimaanPengadaanMasterAsset> penerimaanPengadaanMasterAssets,
			List<PenerimaanPengadaanMasterAsset> penerimaanPengadaanMasterAssetsHanyaDitampilkan,
			PenyediaAsset penyediaAsset) {
		super();
		this.penerimaanPengadaanMasterAssets = penerimaanPengadaanMasterAssets;
		this.penerimaanPengadaanMasterAssetsHanyaDitampilkan = penerimaanPengadaanMasterAssetsHanyaDitampilkan;
		this.penyediaAsset = penyediaAsset;

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

	class PenerimaanPengadaanMasterAssetRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PenerimaanPengadaanMasterAsset penerimaanPengadaanMasterAsset = (PenerimaanPengadaanMasterAsset) arg1;
			arg0.setAttribute("penerimaanPengadaanMasterAsset", penerimaanPengadaanMasterAsset);
			final MyCheckboxConfig checkbox = new MyCheckboxConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			for (PenerimaanPengadaanMasterAsset myPenerimaanPengadaanMasterAsset : penerimaanPengadaanMasterAssets) {
				if (myPenerimaanPengadaanMasterAsset.getId().equals(penerimaanPengadaanMasterAsset.getId())) {
					checkbox.setChecked(true);
					checkbox.setDisabled(true);
					break;
				}
			}

			checkbox.setChecked(ids.contains(penerimaanPengadaanMasterAsset.getId()));

			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						ids.add(penerimaanPengadaanMasterAsset.getId());
					} else {
						ids.remove(penerimaanPengadaanMasterAsset.getId());
					}
				}
			});
			new Label(penerimaanPengadaanMasterAsset.getKode()).setParent(arg0);

			Vbox myvbox = new Vbox();
			myvbox.setParent(arg0);

			new Label(penerimaanPengadaanMasterAsset.getKeterangan()).setParent(myvbox);

			Hbox hbox = new Hbox();
			hbox.setParent(myvbox);
			LampiranLain.createDownloadUploadFileLain(hbox, penerimaanPengadaanMasterAsset.getId(),
					PenerimaanPengadaanMasterAsset.class.getName(), "Tagihan", false, null, null, false, false, false,
					false);

			new Label(penerimaanPengadaanMasterAsset.getPenyedia() == null ? ""
					: penerimaanPengadaanMasterAsset.getPenyedia().getNama()).setParent(arg0);
			new Label(penerimaanPengadaanMasterAsset.getTanggalPersetujuan() == null ? ""
					: Common.dateFormat4.get().format(penerimaanPengadaanMasterAsset.getTanggalPersetujuan()))
					.setParent(arg0);
		}

	}

	public void display() {

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(this);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Tagihan");
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

		row.appendChild(new ais.ui.util.MyLabelConfig("Kode"));
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

		Toolbar toolbar = new Toolbar();
		ais.ui.util.BanboxFilterToggle.pasang(north, searchgrid, toolbar);
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

		grid = new MyGrid();
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
		column.setLabel("");
		column.setWidth("40px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kode");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Penyedia");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tanggal");

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		toolbar = new Toolbar();
		toolbar.setParent(south);

		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				AmbilDataPenerimaanPengadaanMasterAssetBanyak.this.detach();
			}
		});
		cancel.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		button.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				if (eventListener != null && grid.getRows() != null && grid.getRows().getChildren() != null) {
					List<PenerimaanPengadaanMasterAsset> penerimaanPengadaanMasterAssets = new ArrayList<PenerimaanPengadaanMasterAsset>();
					List<Row> rows = grid.getRows().getChildren();
					for (Row row : rows) {
						try {
							MyCheckboxConfig checkbox = (MyCheckboxConfig) row.getAttribute("checkbox");
							if (checkbox != null && checkbox.isChecked() && !checkbox.isDisabled()) {
								PenerimaanPengadaanMasterAsset myPenerimaanPengadaanMasterAsset = (PenerimaanPengadaanMasterAsset) row
										.getAttribute("penerimaanPengadaanMasterAsset");
								penerimaanPengadaanMasterAssets.add(myPenerimaanPengadaanMasterAsset);
							}
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/AmbilDataPenerimaanPengadaanMasterAssetBanyak.java:304");
							// TODO: handle exception
						}
					}
					Event myEvent = new Event("myEvent", event.getTarget(), penerimaanPengadaanMasterAssets);
					eventListener.onEvent(myEvent);
				}
				AmbilDataPenerimaanPengadaanMasterAssetBanyak.this.detach();
			}
		});
		button.setParent(toolbar);

	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();

		List<Long> values = new ArrayList<Long>();
		if (penerimaanPengadaanMasterAssetsHanyaDitampilkan != null) {
			for (PenerimaanPengadaanMasterAsset penerimaanPengadaanMasterAsset : penerimaanPengadaanMasterAssetsHanyaDitampilkan) {
				values.add(penerimaanPengadaanMasterAsset.getId());
			}
		}

		Criteria criteria = session.createCriteria(PenerimaanPengadaanMasterAsset.class)

				.add(penyediaAsset != null ? Restrictions.eq("penyedia", penyediaAsset)
						: Restrictions.sqlRestriction("true"))

				.add(ids.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.not(Restrictions.in("id", ids)))

				.add(penerimaanPengadaanMasterAssetsHanyaDitampilkan == null || values.size() == 0
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.in("id", values))

				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("keterangan", nama.getValue().trim(), MatchMode.ANYWHERE))

				.add(kode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("kode", kode.getValue().trim(), MatchMode.ANYWHERE))

		;

		if (order) {
			criteria.addOrder(Order.desc("id"));
		}

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();

		List<PenerimaanPengadaanMasterAsset> penerimaanPengadaanMasterAsset = ConstantValues.simpleList(session
				.createCriteria(PenerimaanPengadaanMasterAsset.class)

				.add(penyediaAsset != null ? Restrictions.eq("penyedia", penyediaAsset)
						: Restrictions.sqlRestriction("true"))

				.addOrder(Order.desc("id"))
				.add(ids.size() == 0 ? Restrictions.sqlRestriction("1!=1") : Restrictions.in("id", ids)),
				PenerimaanPengadaanMasterAsset.class);

		// Paging server-side ditangani sepenuhnya oleh pagingHelper (hitung total, offset, dan
		// komponen Paging tunggal). Tidak ada lagi paging legacy sehingga tak muncul pager ganda.
		List<PenerimaanPengadaanMasterAsset> myPenerimaanPengadaanMasterAsset = pagingHelper
				.cariDenganCriteria(initCriteria(true), PenerimaanPengadaanMasterAsset.class);

		penerimaanPengadaanMasterAsset.addAll(myPenerimaanPengadaanMasterAsset);

		ListModel strset = new SimpleListModel(penerimaanPengadaanMasterAsset);
		grid.setRowRenderer(new PenerimaanPengadaanMasterAssetRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}
