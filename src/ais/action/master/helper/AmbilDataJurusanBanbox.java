package ais.action.master.helper;


import ais.common.CommonSearchFilterHelper;
import java.util.List;

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
import org.zkoss.zul.Combobox;
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
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Jurusan;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Implementasi pola "Bandbox picker" AIS untuk entity {@link ais.database.model.Jurusan} — lihat
 * {@link ais.ui.util.GetEventListener} untuk arsitektur kerangka umum
 * (constructor/display/onSearchDefault/renderer/callback).
 * <p>
 * {@code Jurusan} adalah master data program studi/jurusan akademik, masing-masing opsional
 * dikaitkan ke satu {@code Fakultas} induk (bila {@code fakultas} kosong, jurusan tersebut berlaku
 * lintas fakultas dan ditampilkan sebagai "Semua"). Popup pencarian menyediakan field {@code nama}
 * (ilike substring) dan Combobox {@code searchfakultas}; hanya baris {@code aktif == true} atau
 * {@code aktif} kosong yang tampil, diurutkan menaik berdasar nama lalu id. Filter fakultas
 * memakai {@code Restrictions.or(isNull("fakultas"), eq(...))} sehingga jurusan lintas-fakultas
 * selalu ikut muncul terlepas dari fakultas yang dipilih pengguna di form pencarian. Pemilihan
 * bersifat TUNGGAL (Radiogroup). Tidak ada constructor dengan parameter tambahan; kelas ini
 * memakai paging {@code grid.setMold("paging")} client-side lama (dibatasi
 * {@link ais.common.Common#MAX_RESULT_50}), bukan {@code AmbilDataPagingHelper} server-side —
 * field {@code pagingHelper} dideklarasikan tapi TIDAK dipakai di {@link #onSearchDefault(Event)}.
 * </p>
 *
 * @see Bandbox
 */
public class AmbilDataJurusanBanbox extends Bandbox implements GetEventListener {

	/**
	 *
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;


	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;

	/**
	 * Konstruktor standar: mempersiapkan pilihan Combobox fakultas (termasuk opsi "Semua") lewat
	 * {@link ais.common.Common#initFakultasDanJurusanDanSemua} dan memasang listener {@code onOpen}
	 * yang membangun popup pencarian secara lazy pada pembukaan pertama. Mengikuti kerangka standar
	 * di {@link ais.ui.util.GetEventListener}, tidak ada logika tambahan khusus entity ini.
	 */
	public AmbilDataJurusanBanbox() {
		super();
		setReadonly(true);

		addEventListener("onOpen", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (getChildren().isEmpty()) {

					Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, null);

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

	/** Kriteria pencarian: nama jurusan (ilike, substring). */
	private Textbox nama;
	/** Kriteria pencarian: fakultas induk jurusan (termasuk opsi "Semua"). */
	private Combobox searchfakultas = new Combobox();

	/**
	 * Renderer baris grid hasil pencarian {@link Jurusan}: kolom nama, fakultas (tampil "Semua"
	 * bila kosong), keterangan, dan satu radio button pilihan. Mengikuti kerangka renderer standar
	 * di {@link ais.ui.util.GetEventListener} — listener {@code onCheck} menutup popup, menyimpan
	 * entity terpilih ke atribut {@code "jurusan"} dan teks tampilan {@code jurusan.toString()},
	 * lalu meneruskan event ke {@link #eventListener} bila terpasang.
	 *
	 * @see AmbilDataJurusanBanbox
	 */
	class JurusanRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Jurusan jurusan = (Jurusan) arg1;
			MyRadioConfig checkbox = new MyRadioConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			// checkbox.setId(jurusan.getId() + "");

			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataJurusanBanbox.this.setOpen(false);
					AmbilDataJurusanBanbox.this.setAttribute("jurusan", jurusan);
					AmbilDataJurusanBanbox.this.setValue(jurusan.toString());

					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			new Label(jurusan.getNama()).setParent(arg0);
			new Label(jurusan.getFakultas() == null ? "Semua" : jurusan.getFakultas().getNama()).setParent(arg0);
			new Label(jurusan.getKeterangan()).setParent(arg0);

		}

	}

	/**
	 * Membangun popup pencarian {@link Jurusan} sekali (dipanggil lazy dari listener
	 * {@code onOpen}): form dengan field nama dan fakultas, tombol Cari, dan grid hasil dibungkus
	 * {@link org.zkoss.zul.Radiogroup} (pilih tunggal). Mengikuti kerangka {@code display()}
	 * standar — lihat {@link ais.ui.util.GetEventListener}. Memanggil
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
		panel.setTitle("Daftar Jurusan");
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

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");

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
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Fakultas");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");

		onSearchDefault(null);

	}

	/**
	 * Mengeksekusi pencarian {@link Jurusan} dengan filter {@code aktif} (baris nonaktif
	 * disembunyikan kecuali kolomnya {@code null}), {@code nama} (ilike substring), dan fakultas
	 * (eq bila dipilih, tapi digabung {@code Restrictions.or(isNull("fakultas"), ...)} sehingga
	 * jurusan lintas-fakultas selalu ikut tampil). Diurutkan menaik berdasar nama lalu id, dibatasi
	 * {@link ais.common.Common#MAX_RESULT_50}, lalu memasang {@link JurusanRenderer} dan model
	 * hasil ke {@link #grid}. Mengikuti kerangka {@code onSearchDefault} standar — lihat
	 * {@link ais.ui.util.GetEventListener}.
	 *
	 * @param event event pemicu (klik tombol Cari); boleh {@code null} saat dipanggil dari
	 *              {@link #display()} untuk mengisi grid pertama kali
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();
		List<Jurusan> jurusan = session.createCriteria(Jurusan.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.asc("nama")).addOrder(Order.asc("id"))
				.add(nama.getText().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("nama", nama.getText().trim(), MatchMode.ANYWHERE))

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.isNull("fakultas"),
										CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false)))

				.setMaxResults(Common.MAX_RESULT_50)

				.list();

		ListModel strset = new SimpleListModel(jurusan);
		grid.setRowRenderer(new JurusanRenderer());
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
