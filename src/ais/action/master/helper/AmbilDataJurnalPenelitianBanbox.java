package ais.action.master.helper;

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

import ais.action.master.penelitiandanpengabdian.JurnalPenelitianAction;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.penelitiandanpengabdian.JurnalPenelitian;
import ais.ui.util.AmbilDataPagingHelper;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Implementasi pola "Bandbox picker" AIS untuk entity
 * {@link ais.database.model.penelitiandanpengabdian.JurnalPenelitian} — lihat
 * {@link ais.ui.util.GetEventListener} untuk arsitektur kerangka umum
 * (constructor/display/onSearchDefault/renderer/callback).
 * <p>
 * {@code JurnalPenelitian} adalah master data jurnal ilmiah (mis. tempat publikasi hasil
 * penelitian dosen) pada modul Penelitian dan Pengabdian. Popup pencarian hanya menyediakan satu
 * kriteria {@code Textbox nama} yang dicocokkan ilike-substring ke kolom {@code judul} jurnal;
 * hanya baris {@code aktif == true} atau {@code aktif} kosong yang ditampilkan. Kelas ini memakai
 * pola paging SERVER-SIDE terbaru via {@link AmbilDataPagingHelper} (bukan
 * {@code grid.setMold("paging")} client-side lama) — lihat {@link #initCriteria} dan
 * {@link #onSearchDefault}, serta filter toggle pencarian lewat
 * {@link ais.ui.util.BanboxFilterToggle}. Toolbar popup juga menyediakan tombol "Tambah Jurnal
 * Baru" yang langsung memanggil {@link JurnalPenelitianAction#onAddExternal} dan otomatis memilih
 * jurnal yang baru dibuat begitu tersimpan. Pemilihan bersifat TUNGGAL (Radiogroup). Tidak ada
 * constructor dengan parameter tambahan.
 * </p>
 *
 * @see Bandbox
 */
public class AmbilDataJurnalPenelitianBanbox extends Bandbox implements GetEventListener {

	/**
	 *
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;
	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final AmbilDataPagingHelper pagingHelper = new AmbilDataPagingHelper();

	private EventListener eventListener;

	/**
	 * Konstruktor standar: memasang listener {@code onOpen} yang membangun popup pencarian secara
	 * lazy pada pembukaan pertama. Mengikuti kerangka standar di
	 * {@link ais.ui.util.GetEventListener}, tidak ada logika tambahan khusus entity ini.
	 */
	public AmbilDataJurnalPenelitianBanbox() {
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

	/** Kriteria pencarian: judul jurnal (ilike, substring). */
	private Textbox nama;

	/**
	 * Renderer baris grid hasil pencarian {@link JurnalPenelitian}: menampilkan kolom judul dan
	 * satu radio button pilihan. Mengikuti kerangka renderer standar di
	 * {@link ais.ui.util.GetEventListener} — listener {@code onCheck} menutup popup, menyimpan
	 * entity terpilih ke atribut {@code "jurnalPenelitian"} dan teks tampilan
	 * {@code jurnalPenelitian.toString()}, lalu meneruskan event ke {@link #eventListener} bila
	 * terpasang.
	 *
	 * @see AmbilDataJurnalPenelitianBanbox
	 */
	class JurnalPenelitianRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final JurnalPenelitian jurnalPenelitian = (JurnalPenelitian) arg1;
			MyRadioConfig checkbox = new MyRadioConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			// checkbox.setId(jurnalPenelitian.getId() + "");

			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataJurnalPenelitianBanbox.this.setOpen(false);
					AmbilDataJurnalPenelitianBanbox.this.setAttribute("jurnalPenelitian", jurnalPenelitian);
					AmbilDataJurnalPenelitianBanbox.this.setValue(jurnalPenelitian.toString());

					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			new Label(jurnalPenelitian.getJudul()).setParent(arg0);

		}

	}

	/**
	 * Membangun popup pencarian {@link JurnalPenelitian} sekali (dipanggil lazy dari listener
	 * {@code onOpen}): form dengan field {@code nama} (judul), tombol Cari, tombol Tambah Jurnal
	 * Baru, filter toggle {@link ais.ui.util.BanboxFilterToggle}, dan grid hasil ber-paging
	 * server-side dibungkus {@link org.zkoss.zul.Radiogroup} (pilih tunggal). Mengikuti kerangka
	 * {@code display()} standar — lihat {@link ais.ui.util.GetEventListener}. Memanggil
	 * {@link #onSearchDefault(Event)} di akhir agar grid terisi saat popup pertama dibuka.
	 */
	public void display() {
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
		panel.setTitle("Daftar Jurnal");
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

		Columns columns = new Columns();
		columns.setParent(searchgrid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Jurnal"));
		row.appendChild(nama = new Textbox());
		nama.setWidth("90%");

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

		MyToolbarbuttonConfig add = new MyToolbarbuttonConfig("Tambah Jurnal Baru", "/img/new.gif");
		add.setParent(toolbar);
		add.setTooltiptext("Tambah");
		add.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				JurnalPenelitianAction.onAddExternal(event, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						JurnalPenelitian jurnalPenelitian = (JurnalPenelitian) arg0.getData();
						if (jurnalPenelitian != null) {
							AmbilDataJurnalPenelitianBanbox.this.setOpen(false);
							AmbilDataJurnalPenelitianBanbox.this.setAttribute("jurnalPenelitian", jurnalPenelitian);
							AmbilDataJurnalPenelitianBanbox.this.setValue(jurnalPenelitian.toString());

							if (eventListener != null) {
								eventListener.onEvent(arg0);
							}
						}
					}
				}, new JurnalPenelitian());

			}
		});

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

		columns = new Columns();

		columns.setParent(grid);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("30px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		onSearchDefault(null);

	}

	/**
	 * Membangun {@link Criteria} pencarian {@link JurnalPenelitian}: hanya baris {@code aktif}
	 * (atau {@code null}), dicocokkan ke {@code judul} lewat ilike-substring bila field
	 * {@link #nama} diisi. Dipanggil oleh {@link AmbilDataPagingHelper} baik untuk menghitung total
	 * baris maupun mengambil satu halaman data — parameter {@code isOrder} mengontrol apakah
	 * pengurutan ({@code Order.asc("judul")}) ikut dipasang (hanya perlu saat mengambil data,
	 * bukan saat menghitung count).
	 *
	 * @param session  sesi Hibernate aktif
	 * @param isOrder  {@code true} untuk memasang {@code ORDER BY judul}, {@code false} bila
	 *                 criteria hanya dipakai menghitung jumlah baris
	 * @return criteria siap dieksekusi oleh {@link AmbilDataPagingHelper}
	 */
	public Criteria initCriteria(Session session, boolean isOrder) {
		Criteria criteria = session.createCriteria(JurnalPenelitian.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(nama.getText().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("judul", nama.getText().trim(), MatchMode.ANYWHERE));
		if (isOrder) {
			criteria.addOrder(Order.asc("judul"));
		}
		return criteria;
	}

	/**
	 * Mengeksekusi pencarian {@link JurnalPenelitian} lewat {@link AmbilDataPagingHelper#cari},
	 * yang memanggil balik {@link #initCriteria(Session, boolean)} untuk membangun query per
	 * halaman (paging server-side, bukan {@code MAX_RESULT} client-side), lalu memasang
	 * {@link JurnalPenelitianRenderer} dan model hasil ke {@link #grid}. Mengikuti kerangka
	 * {@code onSearchDefault} standar — lihat {@link ais.ui.util.GetEventListener}.
	 *
	 * @param event event pemicu (klik tombol Cari); boleh {@code null} saat dipanggil dari
	 *              {@link #display()} untuk mengisi grid pertama kali
	 */
	public void onSearchDefault(Event event) {
		List<JurnalPenelitian> jurnalPenelitian = pagingHelper.cari(new AmbilDataPagingHelper.CriteriaFactory() {
			@Override
			public Criteria initCriteria(Session session, boolean isOrder) {
				return AmbilDataJurnalPenelitianBanbox.this.initCriteria(session, isOrder);
			}
		}, JurnalPenelitian.class);

		ListModel strset = new SimpleListModel(jurnalPenelitian);
		grid.setRowRenderer(new JurnalPenelitianRenderer());
		grid.setModelCheckMobile(strset);

	}

	/** {@inheritDoc} Implementasi setter polos standar — lihat {@link ais.ui.util.GetEventListener}. */
	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	/** {@inheritDoc} Implementasi getter polos standar — lihat {@link ais.ui.util.GetEventListener}. */
	public EventListener getEventListener() {
		return eventListener;
	}
}
