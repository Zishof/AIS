package ais.action.master.akunting.helper;

import java.util.List;
import java.util.Set;

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

import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.UangMuka;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.AmbilDataPagingHelper;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Implementasi pola "Bandbox picker" AIS untuk entity
 * {@link ais.database.model.akunting.UangMuka} — lihat {@link ais.ui.util.GetEventListener} untuk
 * arsitektur kerangka umum (constructor/display/onSearchDefault/renderer/callback). {@code
 * UangMuka} adalah master data pengajuan uang muka akunting: dana tunai yang diberikan lebih
 * dahulu kepada satuan kerja/pegawai, disetujui lewat alur SOP, lalu wajib dipertanggungjawabkan
 * (kecuali sedang dipakai sebagai sumber dana pengadaan, lihat mode {@code hanyPr}).
 * <p>
 * Popup menampilkan satu grid pilih-tunggal (via {@link Radiogroup}) dengan filter teks bebas
 * "Kode/Nama" (cocok kolom {@code kode} atau {@code keterangan}, ILIKE ANYWHERE) dan satuan kerja
 * lewat sub-picker {@link AmbilDataSatuanKerjaBanbox}. Hasil selalu dibatasi ke uang muka yang
 * aktif, berstatus {@link UangMuka#DISETUJU} dengan {@code disetujuiOleh} terisi, sudah
 * direalisasikan/ditransfer (join {@code daftarPengajuanTransfer} — transfer biasa lewat
 * {@code prosesTransfer.realisasikanOleh}, atau transitori lewat {@code transitoriData.transfer}),
 * dan berada dalam cakupan satuan kerja terpilih beserta turunannya (atau satuan kerja user login
 * bila kosong).
 * <p>
 * <b>Mode {@code hanyPr}</b> (diaktifkan lewat constructor {@link #AmbilDataUangMukaBanbox(boolean)}):
 * bila {@code true}, hanya menampilkan uang muka yang sudah tertaut ke pengadaan
 * ({@code permintaanPengadaanMasterAssets} tidak kosong) dan BELUM diterima
 * ({@code penerimaanPengadaanMasterAsset} masih null), serta MENGABAIKAN syarat "belum ada
 * pertangungjawaban" (uang muka yang sudah dipertanggungjawabkan tetap boleh dipakai lagi untuk
 * pengadaan). Bila {@code false} (default), justru sebaliknya: uang muka harus BELUM punya
 * {@code pertangungjawaban}. Pencarian dijalankan lewat paginasi server-side
 * {@link AmbilDataPagingHelper}.
 *
 * @see Bandbox
 */
public class AmbilDataUangMukaBanbox extends Bandbox implements GetEventListener {

	/**
	 * Serial version UID standar untuk kompatibilitas serialisasi komponen ZK.
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;
	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private SatuanKerjaTreeModel satuanKerjaTreeModel;
	private EventListener eventListener;
	private boolean hanyPr = false;

	/** Constructor default: mode {@link #hanyPr} dinonaktifkan (syarat "belum dipertanggungjawabkan" berlaku). */
	public AmbilDataUangMukaBanbox() {
		this(false);
	}

	/**
	 * Membangun komponen: memasang mode read-only standar dan listener {@code onOpen} yang
	 * membangun popup ({@link #display()}) hanya pada pembukaan pertama, mengikuti kerangka umum
	 * di {@link ais.ui.util.GetEventListener}.
	 *
	 * @param hanyPr {@code true} untuk membatasi hasil ke uang muka yang tertaut pengadaan dan
	 *               belum diterima (lihat penjelasan mode {@code hanyPr} di Javadoc kelas)
	 */
	public AmbilDataUangMukaBanbox(boolean hanyPr) {
		super();
		this.hanyPr = hanyPr;
		setReadonly(true);
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
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

	/**
	 * Merender satu baris grid uang muka: radio pilih, kode+nama (digabung {@link Vbox}), saldo,
	 * nilai, sisa (saldo dikurangi nilai, kosong bila saldo &le; 0), dan keterangan. Memilih baris
	 * menutup popup, menyimpan entity {@link UangMuka} terpilih ke attribute {@code "uangMuka"}
	 * pada Bandbox, mengisi teks tampilan dengan kodenya, lalu memicu {@link #eventListener} bila
	 * terpasang — mengikuti kerangka callback standar di {@link ais.ui.util.GetEventListener}.
	 *
	 * @see AmbilDataUangMukaBanbox
	 */
	class UangMukaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final UangMuka uangMuka = (UangMuka) arg1;
			MyRadioConfig checkbox = new MyRadioConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			// checkbox.setId(uangMuka.getId() + "");

			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataUangMukaBanbox.this.setOpen(false);
					AmbilDataUangMukaBanbox.this.setAttribute("uangMuka", uangMuka);
					AmbilDataUangMukaBanbox.this.setValue(uangMuka.getKode());

					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});
			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			new Label(uangMuka.getKode()).setParent(vbox);
			new Label(uangMuka.getNama()).setParent(vbox);
			new Label(Common.numberFormat.get().format(uangMuka.getSaldo())).setParent(arg0);
			new Label(Common.numberFormat.get().format(uangMuka.getNilai())).setParent(arg0);
			new Label(uangMuka.getSaldo() <= 0.0 ? ""
					: Common.numberFormat.get().format(uangMuka.getSaldo() - uangMuka.getNilai())).setParent(arg0);
			new Label(uangMuka.getKeterangan()).setParent(arg0);

		}

	}

	/**
	 * Membangun popup pencarian (dipanggil sekali saat pertama dibuka): form "Kode/Nama" +
	 * sub-picker satuan kerja, grid hasil berpaginasi server-side ({@link AmbilDataPagingHelper}),
	 * lalu memuat data awal lewat {@link #onSearchDefault(Event)}.
	 */
	public void display() throws Exception {
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
		panel.setTitle("Daftar Uang Muka");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode/Nama"));
		row.appendChild(nama = new Textbox());
		nama.setWidth("90%");
		nama.addEventListener("onOK", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});

		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		row.appendChild(satuanKerja = new AmbilDataSatuanKerjaBanbox(true));
		satuanKerja.setWidth("90%");
		satuanKerja.setEventListener(new EventListener() {
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

		toolbar.appendChild(Common.createCleanButton(this, this));

		/* Paging server-side (AmbilDataPagingHelper) menggantikan mold
		 * "paging" client-side yang dibatasi MAX_RESULT_100. */
		grid = new MyGrid();
		pagingHelper.pasangOnPaging(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
		pagingHelper.pasangGridDanPaging(center, grid);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("30px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kode");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Saldo");
		column.setWidth("10%");
		column.setAlign("right");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nilai");
		column.setWidth("10%");
		column.setAlign("right");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Sisa");
		column.setWidth("10%");
		column.setAlign("right");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");
		column.setWidth("40%");

		onSearchDefault(null);

	}

	/**
	 * Menyusun kriteria pencarian {@link UangMuka}: filter mode {@link #hanyPr} (tertaut
	 * pengadaan dan belum diterima, mengabaikan syarat pertanggungjawaban), aktif, cocok teks
	 * "Kode/Nama", sudah direalisasikan (transfer biasa atau transitori), disetujui dengan
	 * {@code disetujuiOleh} terisi, dan (bila bukan mode {@code hanyPr}) belum punya
	 * {@code pertangungjawaban}; dibatasi ke satuan kerja terpilih beserta turunannya (atau
	 * satuan kerja user login bila kosong).
	 *
	 * @param session sesi Hibernate untuk membangun kriteria
	 * @param isOrder tambahkan pengurutan menaik berdasarkan kode bila {@code true}
	 * @return kriteria pencarian siap dieksekusi
	 */
	public Criteria initCriteria(Session session, boolean isOrder) {
		SatuanKerja parent = (SatuanKerja) satuanKerja.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Tbmuser tbmuser = Common.getCurrentUser();

		Criteria criteria = session.createCriteria(UangMuka.class)

				.add(hanyPr ?

						Restrictions.and(Restrictions.isNull("penerimaanPengadaanMasterAsset"),
								Restrictions.ne("permintaanPengadaanMasterAssets", ""))

						: Restrictions.sqlRestriction("true"))

				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(nama.getText().trim().isEmpty() ? Restrictions.sqlRestriction("1=1") :

						Restrictions.or(Restrictions.ilike("kode", nama.getText().trim(), MatchMode.ANYWHERE),
								Restrictions.or(Restrictions.ilike("kode", nama.getText().trim(), MatchMode.ANYWHERE),
										Restrictions.ilike("keterangan", nama.getText().trim(), MatchMode.ANYWHERE)))

				)

				.createAlias("daftarPengajuanTransfer", "daftarPengajuanTransfer")
				.createAlias("daftarPengajuanTransfer.prosesTransfer", "prosesTransfer", Criteria.LEFT_JOIN)
				.createAlias("daftarPengajuanTransfer.transitoriData", "transitoriData", Criteria.LEFT_JOIN)
				.createAlias("transitoriData.prosesTransitori", "prosesTransitori", Criteria.LEFT_JOIN)

				.add(Restrictions.or(
						Restrictions.and(Restrictions.eq("daftarPengajuanTransfer.transfer", true),
								Restrictions.isNotNull("prosesTransfer.realisasikanOleh")),
						Restrictions.and(Restrictions.eq("daftarPengajuanTransfer.transitori", true),
								Restrictions.isNotNull("transitoriData.transfer"))))

				.add(Restrictions.and(Restrictions.isNotNull("disetujuiOleh"),

						Restrictions.and(Restrictions.eq("status", UangMuka.DISETUJU),
								Restrictions.and(
										Restrictions.or(Restrictions.isNull("satuanKerja"),
												Restrictions.or(
														satuanKerjas.isEmpty() ? Restrictions.sqlRestriction("true")
																: Restrictions.in("satuanKerja", satuanKerjas),
														Restrictions.eq("satuanKerja", tbmuser.ambilSatuanKerja()))),

										Restrictions.and(
												hanyPr ? Restrictions.sqlRestriction("true")
														: Restrictions.isNull("pertangungjawaban"),
												Restrictions.eq("aktif", true))))));

		if (isOrder) {
			criteria.addOrder(Order.asc("kode"));
		}
		return criteria;
	}

	/**
	 * Mengeksekusi ulang {@link #initCriteria(Session, boolean)} lewat {@link #pagingHelper}
	 * (paginasi server-side) dan mengisi ulang grid dengan hasilnya beserta
	 * {@link UangMukaRenderer}.
	 *
	 * @param event tidak dipakai, hanya mengikuti signature standar listener pencarian
	 */
	public void onSearchDefault(Event event) {
		List<UangMuka> uangMuka = pagingHelper.cari(new AmbilDataPagingHelper.CriteriaFactory() {
			@Override
			public Criteria initCriteria(Session session, boolean isOrder) {
				return AmbilDataUangMukaBanbox.this.initCriteria(session, isOrder);
			}
		}, UangMuka.class);

		ListModel strset = new SimpleListModel(uangMuka);
		grid.setRowRenderer(new UangMukaRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Mengunci filter satuan kerja dari luar (mis. diisi otomatis dari konteks entity induk):
	 * mengisi teks dan attribute satuan kerja pada sub-picker, menonaktifkannya bila
	 * {@code satuanKerja} tidak null, lalu memuat ulang hasil pencarian. Tidak melakukan apa pun
	 * bila popup belum pernah dibuka (sub-picker satuan kerja belum terpasang).
	 *
	 * @param satuanKerja satuan kerja yang dipaksakan sebagai filter, atau {@code null} untuk
	 *                    mengaktifkan kembali pemilihan bebas
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {

		try {
			if (this.satuanKerja == null) {
				// Popup Bandbox belum pernah dibuka (display() belum jalan lewat onOpen),
				// child widget "satuanKerja" & grid masih null. Lewati saja, tidak ada
				// UI untuk disinkronkan.
				return;
			}
			this.satuanKerja.setValue(satuanKerja == null ? "" : satuanKerja.getNama());
			this.satuanKerja.setAttribute("satuanKerja", satuanKerja);
			this.satuanKerja.setDisabled(satuanKerja != null);
			onSearchDefault(null);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/AmbilDataUangMukaBanbox.java:349");
			// TODO: handle exception
		}
	}

	/** @param eventListener dipanggil setiap kali user memilih satu uang muka */
	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	/** @return listener pemilihan uang muka yang sedang terpasang, boleh {@code null} */
	public EventListener getEventListener() {
		return eventListener;
	}
}
