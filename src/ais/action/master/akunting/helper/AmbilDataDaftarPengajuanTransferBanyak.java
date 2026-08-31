package ais.action.master.akunting.helper;

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
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.A;
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
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.sop.TampilanAlurSopAction;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.akunting.DaftarPengajuanTransfer;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Tipe khusus untuk ambil data daftar pengajuan transfer banyak. Kelas ini memberi nama dan batas
 * tanggung jawab yang eksplisit pada perilaku yang diwarisi atau kontrak yang
 * diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyGrid grid}, {@code
 * ais.ui.util.AmbilDataPagingHelper pagingHelper}, {@code EventListener eventListener}, {@code List
 * daftarPengajuanTransfers}, {@code List daftarPengajuanTransfersHanyaDitampilkan}, {@code MyTextbox nama},
 * {@code Set ids}, {@code MyTextbox keterangan}; pembacaan/pencarian ({@code onSearchDefault()}, {@code
 * setEventListener()}, {@code getEventListener()}); operasi domain lain ({@code display()}). Bagian lain dari
 * kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class AmbilDataDaftarPengajuanTransferBanyak extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;

	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;
	private List<DaftarPengajuanTransfer> daftarPengajuanTransfers;
	private List<DaftarPengajuanTransfer> daftarPengajuanTransfersHanyaDitampilkan;

	private MyTextbox nama;

	private Set<Long> ids = new HashSet<Long>();
	private MyTextbox keterangan;

	public AmbilDataDaftarPengajuanTransferBanyak(List<DaftarPengajuanTransfer> daftarPengajuanTransfers) {
		super();
		this.daftarPengajuanTransfers = daftarPengajuanTransfers;
		display();
		onSearchDefault(null);
	}

	public AmbilDataDaftarPengajuanTransferBanyak(List<DaftarPengajuanTransfer> daftarPengajuanTransfers,
			List<DaftarPengajuanTransfer> daftarPengajuanTransfersHanyaDitampilkan) {
		super();
		this.daftarPengajuanTransfers = daftarPengajuanTransfers;
		this.daftarPengajuanTransfersHanyaDitampilkan = daftarPengajuanTransfersHanyaDitampilkan;

		display();

		onSearchDefault(null);
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link AmbilDataDaftarPengajuanTransferBanyak}. Kelas ini menerjemahkan
	 * satu item data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link AmbilDataDaftarPengajuanTransferBanyak} dan
	 * dapat mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see AmbilDataDaftarPengajuanTransferBanyak
	 */
	class DaftarPengajuanTransferRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final DaftarPengajuanTransfer daftarPengajuanTransfer = (DaftarPengajuanTransfer) arg1;
			arg0.setAttribute("daftarPengajuanTransfer", daftarPengajuanTransfer);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			if (daftarPengajuanTransfers != null) {
				for (DaftarPengajuanTransfer myDaftarPengajuanTransfer : daftarPengajuanTransfers) {
					if (myDaftarPengajuanTransfer.getId().equals(daftarPengajuanTransfer.getId())) {
						checkbox.setChecked(true);
						checkbox.setDisabled(true);
						break;
					}
				}
			}

			checkbox.setChecked(ids.contains(daftarPengajuanTransfer.getId()));

			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						ids.add(daftarPengajuanTransfer.getId());
					} else {
						ids.remove(daftarPengajuanTransfer.getId());
					}
				}
			});

			Vbox a;
			(a = RevisiHelper.createNewRevisi(DaftarPengajuanTransfer.class, daftarPengajuanTransfer,
					daftarPengajuanTransfer.getNama())).setParent(arg0);

			new Label(daftarPengajuanTransfer.getKode()).setParent(a);

			new Label(daftarPengajuanTransfer.getWaktu() == null ? ""
					: Common.dateFormat.get().format(daftarPengajuanTransfer.getWaktu())).setParent(arg0);

			new Label(daftarPengajuanTransfer.getAkun() == null ? ""
					: daftarPengajuanTransfer.getAkun().getKode() + "-" + daftarPengajuanTransfer.getAkun().getNama())
					.setParent(arg0);

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);

			new Label(daftarPengajuanTransfer.getBankSumber() == null ? ""
					: daftarPengajuanTransfer.getBankSumber().getNama()).setParent(vbox);
			new Label(daftarPengajuanTransfer.getAtasNamaSumber()).setParent(vbox);
			new Label(daftarPengajuanTransfer.getNoRekSumber()).setParent(vbox);

			new Label(Common.numberFormat.get().format(daftarPengajuanTransfer.getNominal())).setParent(arg0);

			vbox = new Vbox();
			vbox.setParent(arg0);
			new Label(daftarPengajuanTransfer.getKeterangan()).setParent(vbox);
			if (daftarPengajuanTransfer.getDisposisiSop() != null) {
				A aa;
				(aa = new A("SOP " + daftarPengajuanTransfer.getDisposisiSop().getKeterangan() + " ("
						+ daftarPengajuanTransfer.getDisposisiSop().getSop().getNama() + ")")).setParent(vbox);
				aa.setStyle("font-size:9px;");
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(daftarPengajuanTransfer.getDisposisiSop().getId(), null, null,
								true, arg0.getTarget());
					}
				});
			} else {
				new Label().setParent(vbox);
			}
		}

	}

	public void display() {

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(this);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Pengajuan Transfer");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode / Nama"));
		row.appendChild(nama = new MyTextbox());
		nama.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new MyTextbox());
		keterangan.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
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
		column.setLabel("");
		column.setWidth("40px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kode/Nama Pengajuan");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Atas Nama");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nilai");
		column.setAlign("right");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");

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
				AmbilDataDaftarPengajuanTransferBanyak.this.detach();
			}
		});
		cancel.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		button.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				if (eventListener != null && grid.getRows() != null && grid.getRows().getChildren() != null) {
					List<DaftarPengajuanTransfer> daftarPengajuanTransfers = new ArrayList<DaftarPengajuanTransfer>();
					List<Row> rows = grid.getRows().getChildren();
					for (Row row : rows) {
						try {
							MyCheckboxConfig checkbox = (MyCheckboxConfig) row.getAttribute("checkbox");
							if (checkbox.isChecked() && !checkbox.isDisabled()) {
								DaftarPengajuanTransfer myDaftarPengajuanTransfer = (DaftarPengajuanTransfer) row
										.getAttribute("daftarPengajuanTransfer");
								daftarPengajuanTransfers.add(myDaftarPengajuanTransfer);
							}
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/AmbilDataDaftarPengajuanTransferBanyak.java:310");
							// TODO: handle exception
						}
					}
					Event myEvent = new Event("myEvent", event.getTarget(), daftarPengajuanTransfers);
					eventListener.onEvent(myEvent);
				}
				AmbilDataDaftarPengajuanTransferBanyak.this.detach();
			}
		});
		button.setParent(toolbar);

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();

		List<Long> values = new ArrayList<Long>();
		if (daftarPengajuanTransfersHanyaDitampilkan != null) {
			for (DaftarPengajuanTransfer daftarPengajuanTransfer : daftarPengajuanTransfersHanyaDitampilkan) {
				values.add(daftarPengajuanTransfer.getId());
			}
		}

		List<DaftarPengajuanTransfer> daftarPengajuanTransfer = session.createCriteria(DaftarPengajuanTransfer.class)
				.createAlias("disposisiSop", "disposisiSop", Criteria.LEFT_JOIN).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.or(Restrictions.isNull("disposisiSop.aktif"),
						Restrictions.eq("disposisiSop.aktif", true)))
				.addOrder(Order.asc("nama"))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(ids.size() == 0 ? Restrictions.sqlRestriction("1!=1") : Restrictions.in("id", ids)).list();

		List<Long> notIn = new ArrayList<Long>();
		if (daftarPengajuanTransfers != null) {
			for (DaftarPengajuanTransfer u : daftarPengajuanTransfers) {
				notIn.add(u.getId());
			}
		}

		List<DaftarPengajuanTransfer> myDaftarPengajuanTransfer = session.createCriteria(DaftarPengajuanTransfer.class)
				.createAlias("disposisiSop", "disposisiSop", Criteria.LEFT_JOIN)
				.createAlias("saldoAwalMasterAsset", "vendorSaldo", Criteria.LEFT_JOIN)
				.createAlias("vendorSaldo.penyedia", "vendorPenyedia", Criteria.LEFT_JOIN)
				.createAlias("pajak", "pjkBd", Criteria.LEFT_JOIN)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.or(Restrictions.isNull("disposisiSop.aktif"),
						Restrictions.eq("disposisiSop.aktif", true)))

				.add(Restrictions.isNull("prosesTransfer")).add(Restrictions.isNull("transitoriData"))

				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

				.add(notIn.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.not(Restrictions.in("id", notIn)))

				.addOrder(Order.desc("id"))

				.add(ids.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.not(Restrictions.in("id", ids)))
				.add(daftarPengajuanTransfersHanyaDitampilkan == null || values.size() == 0
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.in("id", values))

				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: DaftarPengajuanTransferSearchHelper.filterKodeJudul(session, nama.getValue().trim()))

				.add(keterangan.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("keterangan", keterangan.getValue().trim(), MatchMode.ANYWHERE))

				.setMaxResults(Common.MAX_RESULT_1000).list();

		// TERMIN yang BELUM DISETUJUI jangan bisa ditarik ke DPC (hanya yang sudah disetujui) —
		// termasuk data LAMA yang DPT-nya terlanjur dibuat sebelum disetujui.
		for (DaftarPengajuanTransfer u : myDaftarPengajuanTransfer) {
			if (DaftarPengajuanTransferSearchHelper.terminBelumDisetujui(session, u)) {
				continue;
			}
			daftarPengajuanTransfer.add(u);
		}

		ListModel strset = new SimpleListModel(daftarPengajuanTransfer);
		grid.setRowRenderer(new DaftarPengajuanTransferRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}
