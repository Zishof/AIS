package ais.action.master.helper.generic;


import ais.common.CommonSearchFilterHelper;
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
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GrupParameterTambahan;
import ais.database.model.ParameterTambahan;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Tipe khusus untuk ambil data parameter tambahan banyak. Kelas ini memberi nama dan batas
 * tanggung jawab yang eksplisit pada perilaku yang diwarisi atau kontrak yang
 * diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyGrid grid}, {@code
 * ais.ui.util.AmbilDataPagingHelper pagingHelper}, {@code EventListener eventListener}, {@code List
 * parameterTambahans}, {@code List parameterTambahansHanyaDitampilkan}, {@code Set ids}, {@code MyTextbox nama},
 * {@code MyTextbox nilai}; pembacaan/pencarian ({@code onSearchDefault()}, {@code setEventListener()}, {@code
 * getEventListener()}); operasi domain lain ({@code display()}). Bagian lain dari kontrak tetap mengikuti kelas
 * induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class AmbilDataParameterTambahanBanyak extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;

	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;
	private List<ParameterTambahan> parameterTambahans;
	private List<ParameterTambahan> parameterTambahansHanyaDitampilkan;

	private Set<Long> ids = new HashSet<Long>();

	public AmbilDataParameterTambahanBanyak(List<ParameterTambahan> parameterTambahans) {
		super();
		this.parameterTambahans = parameterTambahans;
		display();
		onSearchDefault(null);
	}

	public AmbilDataParameterTambahanBanyak(List<ParameterTambahan> parameterTambahans,
			List<ParameterTambahan> parameterTambahansHanyaDitampilkan) {
		super();
		this.parameterTambahans = parameterTambahans;
		this.parameterTambahansHanyaDitampilkan = parameterTambahansHanyaDitampilkan;

		display();

		onSearchDefault(null);
	}

	private MyTextbox nama;
	private MyTextbox nilai;
	private Combobox tipeDataInputan;
	private Combobox searchgrup;
	private Textbox searchketerangan;

	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private Combobox searchyayasan;
	private Combobox searchsekolah;

	/**
	 * Renderer lokal untuk layar/komponen {@link AmbilDataParameterTambahanBanyak}. Kelas ini menerjemahkan satu
	 * item data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link AmbilDataParameterTambahanBanyak} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see AmbilDataParameterTambahanBanyak
	 */
	class ParameterTambahanRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final ParameterTambahan parameterTambahan = (ParameterTambahan) arg1;
			arg0.setAttribute("parameterTambahan", parameterTambahan);

			final Checkbox checkbox = new Checkbox(parameterTambahan.getLabelInputan());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			for (ParameterTambahan myParameterTambahan : parameterTambahans) {
				if (myParameterTambahan.getId().equals(parameterTambahan.getId())) {
					checkbox.setChecked(true);
					checkbox.setDisabled(true);
					break;
				}
			}

			checkbox.setChecked(ids.contains(parameterTambahan.getId()));

			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						ids.add(parameterTambahan.getId());
					} else {
						ids.remove(parameterTambahan.getId());
					}
				}
			});

			new Label(parameterTambahan.getHarusMenyertakanLampiran() ? "Ya" : "Tidak").setParent(arg0);

			new Label(parameterTambahan.getTipeDataInputan()).setParent(arg0);
			new Label(parameterTambahan.getNilaiDataInputan()).setParent(arg0);
			new Label(parameterTambahan.getGrupParameterTambahan() == null ? ""
					: parameterTambahan.getGrupParameterTambahan().getNama()).setParent(arg0);
			new Label(parameterTambahan.getNomorUrut() + "").setParent(arg0);
			new Label(parameterTambahan.getKeterangan()).setParent(arg0);
		}

	}

	public void display() {
		boolean[] ptYa = Common.chekPtAtauSekolah();
		boolean pt = ptYa[0];
		boolean ya = ptYa[1]; 

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);
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
		row.appendChild(nama = new MyTextbox());
		nama.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.appendChild(new ais.ui.util.MyLabelConfig("Nilai"));
		row.appendChild(nilai = new MyTextbox());
		nilai.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.appendChild(new ais.ui.util.MyLabelConfig("Tipe"));
		row.appendChild(tipeDataInputan = new Combobox());
		tipeDataInputan.setWidth("90%");
		MyComboitemConfig comboitem = new MyComboitemConfig(ParameterTambahan.TIDAK_ADA);
		comboitem.setValue(ParameterTambahan.TIDAK_ADA);
		tipeDataInputan.appendChild(comboitem);

		comboitem = new MyComboitemConfig(ParameterTambahan.TEXT);
		comboitem.setValue(ParameterTambahan.TEXT);
		tipeDataInputan.appendChild(comboitem);

		comboitem = new MyComboitemConfig(ParameterTambahan.ANGKA);
		comboitem.setValue(ParameterTambahan.ANGKA);
		tipeDataInputan.appendChild(comboitem);

		comboitem = new MyComboitemConfig(ParameterTambahan.TEXT_ANGKA);
		comboitem.setValue(ParameterTambahan.TEXT_ANGKA);
		tipeDataInputan.appendChild(comboitem);

		comboitem = new MyComboitemConfig(ParameterTambahan.TANGGAL);
		comboitem.setValue(ParameterTambahan.TANGGAL);
		tipeDataInputan.appendChild(comboitem);
		
		comboitem = new MyComboitemConfig(ParameterTambahan.TANGGAL_DAN_WAKTU);
		comboitem.setValue(ParameterTambahan.TANGGAL_DAN_WAKTU);
		tipeDataInputan.appendChild(comboitem);

		comboitem = new MyComboitemConfig(ParameterTambahan.WAKTU);
		comboitem.setValue(ParameterTambahan.WAKTU);
		tipeDataInputan.appendChild(comboitem);

		comboitem = new MyComboitemConfig(ParameterTambahan.PILIHAN_YA_TIDAK);
		comboitem.setValue(ParameterTambahan.PILIHAN_YA_TIDAK);
		tipeDataInputan.appendChild(comboitem);

		comboitem = new MyComboitemConfig(ParameterTambahan.PILIHAN_CUSTOM);
		comboitem.setValue(ParameterTambahan.PILIHAN_CUSTOM);
		tipeDataInputan.appendChild(comboitem);

		comboitem = new MyComboitemConfig(ParameterTambahan.PILIHAN_BANYAK);
		comboitem.setValue(ParameterTambahan.PILIHAN_BANYAK);
		tipeDataInputan.appendChild(comboitem);

		comboitem = new MyComboitemConfig("Semua");
		comboitem.setValue(null);
		tipeDataInputan.appendChild(comboitem);
		tipeDataInputan.setSelectedItem(comboitem);
		tipeDataInputan.setReadonly(true);

		tipeDataInputan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(searchketerangan = new MyTextbox());
		searchketerangan.setWidth("90%");
		searchketerangan.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.appendChild(new ais.ui.util.MyLabelConfig("Grup"));

		Common.insertComboDanSemua(searchgrup = new Combobox(), "nama", GrupParameterTambahan.class);
		row.appendChild(searchgrup);
		searchgrup.setWidth("90%");
		searchgrup.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		searchfakultas = new Combobox();
		searchjurusan = new Combobox();
		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		searchyayasan = new Combobox();
		searchsekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah, true, false);

		row = new MyFormRow();
		row.setParent(rows);

		if (pt) {
			row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
			row.appendChild(searchfakultas);
			searchfakultas.setWidth("90%");

			row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
			row.appendChild(searchjurusan);
			searchjurusan.setWidth("90%");
		}

		if (ya) {
			row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));
			row.appendChild(searchyayasan);
			searchyayasan.setWidth("90%");

			row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah"));
			row.appendChild(searchsekolah);
			searchsekolah.setWidth("90%");
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

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
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
		column.setLabel("Nama Parameter");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Lampiran");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tipe Data");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nilai");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Grup");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("No. Urt");
		column.setWidth("5%");

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
				AmbilDataParameterTambahanBanyak.this.detach();
			}
		});
		cancel.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		button.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				if (eventListener != null && grid.getRows() != null && grid.getRows().getChildren() != null) {
					List<ParameterTambahan> parameterTambahans = new ArrayList<ParameterTambahan>();
					List<Row> rows = grid.getRows().getChildren();
					for (Row row : rows) {
						try {
							Checkbox checkbox = (Checkbox) row.getAttribute("checkbox");
							if (checkbox.isChecked() && !checkbox.isDisabled()) {
								ParameterTambahan myParameterTambahan = (ParameterTambahan) row
										.getAttribute("parameterTambahan");
								parameterTambahans.add(myParameterTambahan);
							}
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/generic/AmbilDataParameterTambahanBanyak.java:392");
							// TODO: handle exception
						}
					}
					Event myEvent = new Event("myEvent", event.getTarget(), parameterTambahans);
					eventListener.onEvent(myEvent);
				}
				AmbilDataParameterTambahanBanyak.this.detach();
			}
		});
		button.setParent(toolbar);

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();

		List<Long> values = new ArrayList<Long>();
		if (parameterTambahansHanyaDitampilkan != null) {
			for (ParameterTambahan parameterTambahan : parameterTambahansHanyaDitampilkan) {
				values.add(parameterTambahan.getId());
			}
		}

		List<ParameterTambahan> parameterTambahan = session.createCriteria(ParameterTambahan.class)

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false))

				.add(searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
						|| searchsekolah.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false))

				.add(searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
						|| searchyayasan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false))

				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.asc("nomorUrut")).addOrder(Order.asc("nama"))
				.add(ids.size() == 0 ? Restrictions.sqlRestriction("1!=1") : Restrictions.in("id", ids)).list();

		List<ParameterTambahan> myParameterTambahan = session.createCriteria(ParameterTambahan.class)

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false))

				.add(searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
						|| searchsekolah.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false))

				.add(searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
						|| searchyayasan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false))

				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.asc("nomorUrut")).addOrder(Order.asc("nama"))
				.add(ids.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.not(Restrictions.in("id", ids)))
				.add(parameterTambahansHanyaDitampilkan == null || values.size() == 0
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.in("id", values))
				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchketerangan.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("keterangan", searchketerangan.getValue().trim(), MatchMode.ANYWHERE))

				.add(nilai.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("nilaiDataInputan", nilai.getValue().trim(), MatchMode.ANYWHERE))

				.add(tipeDataInputan.getSelectedItem() == null || tipeDataInputan.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tipeDataInputan", tipeDataInputan.getSelectedItem().getValue()))

				.add(searchgrup.getSelectedItem() == null || searchgrup.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("grupParameterTambahan", searchgrup.getSelectedItem().getValue()))

				.setMaxResults(Common.MAX_RESULT).list();

		parameterTambahan.addAll(myParameterTambahan);

		ListModel strset = new SimpleListModel(parameterTambahan);
		grid.setRowRenderer(new ParameterTambahanRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}
