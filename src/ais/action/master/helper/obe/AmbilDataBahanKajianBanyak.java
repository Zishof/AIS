package ais.action.master.helper.obe;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
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
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Matakuliah;
import ais.database.model.obe.BahanKajian;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Tipe khusus untuk ambil data bahan kajian banyak. Kelas ini memberi nama dan batas tanggung
 * jawab yang eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyGrid grid}, {@code
 * ais.ui.util.AmbilDataPagingHelper pagingHelper}, {@code EventListener eventListener}, {@code List
 * bahanKajians}, {@code List bahanKajiansHanyaDitampilkan}, {@code MyTextbox nama}, {@code Combobox
 * searchfakultas}, {@code Combobox searchjurusan}; pembacaan/pencarian ({@code onSearchDefault()}, {@code
 * setEventListener()}, {@code getEventListener()}); operasi domain lain ({@code display()}). Bagian lain dari
 * kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class AmbilDataBahanKajianBanyak extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;

	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;
	private List<BahanKajian> bahanKajians;
	private List<BahanKajian> bahanKajiansHanyaDitampilkan;

	private MyTextbox nama;
	private Combobox searchfakultas;
	private Combobox searchjurusan;

	private Set<Long> ids = new HashSet<Long>();
	private MyTextbox keterangan;
	private Jurusan jurusan = null;
	private Matakuliah matakuliah = null;

	public AmbilDataBahanKajianBanyak(List<BahanKajian> bahanKajians) {
		super();
		this.bahanKajians = bahanKajians;
		display();
		onSearchDefault(null);
	}

	public AmbilDataBahanKajianBanyak(List<BahanKajian> bahanKajians, Jurusan j, Matakuliah matakuliah) {
		super();
		this.bahanKajians = bahanKajians;
		this.jurusan = j;
		this.matakuliah = matakuliah;
		display();
		onSearchDefault(null);
	}

	public AmbilDataBahanKajianBanyak(List<BahanKajian> bahanKajians, List<BahanKajian> bahanKajiansHanyaDitampilkan) {
		super();
		this.bahanKajians = bahanKajians;
		this.bahanKajiansHanyaDitampilkan = bahanKajiansHanyaDitampilkan;

		display();

		onSearchDefault(null);
	}

	class BahanKajianRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final BahanKajian bahanKajian = (BahanKajian) arg1;
			arg0.setAttribute("bahanKajian", bahanKajian);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			for (BahanKajian myBahanKajian : bahanKajians) {
				if (myBahanKajian.getId().equals(bahanKajian.getId())) {
					checkbox.setChecked(true);
					checkbox.setDisabled(true);
					break;
				}
			}

			checkbox.setChecked(ids.contains(bahanKajian.getId()));

			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						ids.add(bahanKajian.getId());
					} else {
						ids.remove(bahanKajian.getId());
					}
				}
			});
			new Label(bahanKajian.getKode()).setParent(arg0);
			new Label(bahanKajian.getNama()).setParent(arg0);
			new Label(bahanKajian.getKeterangan()).setParent(arg0);
		}

	}

	public void display() {

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(this);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Capaian Lulusan");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode/Isi"));
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

		searchfakultas = new Combobox();
		searchjurusan = new Combobox();
		if (jurusan == null) {
			row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
			row.appendChild(searchfakultas);
			searchfakultas.setWidth("90%");

			row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Jurusan"));
			row.appendChild(searchjurusan);
			searchjurusan.setWidth("90%");

			Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
		} else {
			row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Jurusan"));
			row.appendChild(new Label(jurusan.getNama()));
		}

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
		column.setWidth("40px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kode");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Isi");
		column.setWidth("60%");

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
				AmbilDataBahanKajianBanyak.this.detach();
			}
		});
		cancel.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		button.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				if (eventListener != null && grid.getRows() != null && grid.getRows().getChildren() != null) {
					List<BahanKajian> bahanKajians = new ArrayList<BahanKajian>();
					List<Row> rows = grid.getRows().getChildren();
					for (Row row : rows) {
						try {
							MyCheckboxConfig checkbox = (MyCheckboxConfig) row.getAttribute("checkbox");
							if (checkbox.isChecked() && !checkbox.isDisabled()) {
								BahanKajian myBahanKajian = (BahanKajian) row.getAttribute("bahanKajian");
								bahanKajians.add(myBahanKajian);
							}
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/obe/AmbilDataBahanKajianBanyak.java:299");
							// TODO: handle exception
						}
					}
					Event myEvent = new Event("myEvent", event.getTarget(), bahanKajians);
					eventListener.onEvent(myEvent);
				}
				AmbilDataBahanKajianBanyak.this.detach();
			}
		});
		button.setParent(toolbar);

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();

		List<Long> values = new ArrayList<Long>();
		if (bahanKajiansHanyaDitampilkan != null) {
			for (BahanKajian bahanKajian : bahanKajiansHanyaDitampilkan) {
				values.add(bahanKajian.getId());
			}
		}

		List<BahanKajian> bahanKajian = ConstantValues.simpleList(
				session.createCriteria(BahanKajian.class).addOrder(Order.asc("kode"))
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(ids.size() == 0 ? Restrictions.sqlRestriction("1!=1") : Restrictions.in("id", ids)),
				BahanKajian.class);

		List<Long> notIn = new ArrayList<Long>();
		if (bahanKajians != null) {
			for (BahanKajian u : bahanKajians) {
				notIn.add(u.getId());
			}
		}

		Jurusan s = (Jurusan) (searchjurusan.getSelectedItem() == null ? null
				: searchjurusan.getSelectedItem().getValue());
		Fakultas f = (Fakultas) (searchfakultas.getSelectedItem() == null ? null
				: searchfakultas.getSelectedItem().getValue());

		List<BahanKajian> myBahanKajian = session.createCriteria(BahanKajian.class)

				.add(matakuliah != null
						? Restrictions.or(Restrictions.isNull("khususBuatMk"),
								Restrictions.eq("khususBuatMk", matakuliah))
						: Restrictions.isNull("khususBuatMk"))

				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

				.add(notIn.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.not(Restrictions.in("id", notIn)))

				.addOrder(Order.asc("kode"))
				.add(ids.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.not(Restrictions.in("id", ids)))
				.add(bahanKajiansHanyaDitampilkan == null || values.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.in("id", values))
				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("kode", nama.getValue().trim(), MatchMode.ANYWHERE)))

				.add(keterangan.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("keterangan", keterangan.getValue().trim(), MatchMode.ANYWHERE))

				.createAlias("jurusan", "jurusan")

				.add(jurusan == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("jurusan", jurusan))
				.add(s == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("jurusan", s))
				.add(f == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("jurusan.fakultas", f))

				.setMaxResults(Common.MAX_RESULT).list();

		bahanKajian.addAll(myBahanKajian);

		ListModel strset = new SimpleListModel(bahanKajian);
		grid.setRowRenderer(new BahanKajianRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}
