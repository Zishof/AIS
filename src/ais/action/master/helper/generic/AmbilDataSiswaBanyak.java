package ais.action.master.helper.generic;


import ais.common.CommonSearchFilterHelper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
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
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.KelasSiswaPunyaSiswa;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Tipe khusus untuk ambil data siswa banyak. Kelas ini memberi nama dan batas tanggung jawab yang
 * eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyGrid grid}, {@code
 * ais.ui.util.AmbilDataPagingHelper pagingHelper}, {@code EventListener eventListener}, {@code List siswas},
 * {@code List siswasHanyaDitampilkan}, {@code Set ids}, {@code MyTextbox kodeSiswaan}, {@code MyTextbox
 * namaSiswa}; pembacaan/pencarian ({@code onSearchDefault()}, {@code setEventListener()}, {@code
 * getEventListener()}); operasi domain lain ({@code display()}). Bagian lain dari kontrak tetap mengikuti kelas
 * induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class AmbilDataSiswaBanyak extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;

	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;
	private List<Siswa> siswas;
	private List<Siswa> siswasHanyaDitampilkan;

	private Set<Long> ids = new HashSet<Long>();

	public AmbilDataSiswaBanyak(List<Siswa> siswas) {
		super();
		this.siswas = siswas;
		display();
		onSearchDefault(null);
	}

	public AmbilDataSiswaBanyak(List<Siswa> siswas, List<Siswa> siswasHanyaDitampilkan) {
		super();
		this.siswas = siswas;
		this.siswasHanyaDitampilkan = siswasHanyaDitampilkan;

		display();

		onSearchDefault(null);
	}

	private MyTextbox kodeSiswaan;
	private MyTextbox namaSiswa;
	private MyTextbox kelas;

	class SiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Siswa siswa = (Siswa) arg1;
			arg0.setAttribute("siswa", siswa);
			final MyCheckboxConfig checkbox = new MyCheckboxConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			for (Siswa mySiswa : siswas) {
				if (mySiswa.getId().equals(siswa.getId())) {
					checkbox.setChecked(true);
					checkbox.setDisabled(true);
					break;
				}
			}

			checkbox.setChecked(ids.contains(siswa.getId()));

			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						ids.add(siswa.getId());
					} else {
						ids.remove(siswa.getId());
					}
				}
			});

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			new Label(siswa.getNomorInduk()).setParent(vbox);
			new Label(siswa.getNomorIndukNasional()).setParent(vbox);
			new Label(siswa.getNama()).setParent(arg0);
			new Label(siswa.getSekolah() == null ? "" : siswa.getSekolah().getNama()).setParent(arg0);

			try {
				KelasSiswa kelasSiswa = siswa.getKelas();
				new Label(kelasSiswa == null ? "" : kelasSiswa.getNama()).setParent(arg0);
			} catch (Exception e) {
				new Label().setParent(arg0);
			}

			new Label(siswa.getAsrama() == null ? "" : siswa.getAsrama().getNama()).setParent(arg0);
			new Label(siswa.getTahunMasuk() + "").setParent(arg0);
		}

	}

	private Combobox searchyayasan = new Combobox();
	private Combobox searchsekolah = new Combobox();

	public void display() {

		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(this);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Siswa");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("NIS"));
		row.appendChild(kodeSiswaan = new MyTextbox());
		kodeSiswaan.setWidth("90%");
		kodeSiswaan.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(namaSiswa = new MyTextbox());
		namaSiswa.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelas"));
		row.appendChild(kelas = new MyTextbox());
		kelas.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));
		row.appendChild(searchyayasan);
		searchyayasan.setWidth("90%");
		searchyayasan.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah"));
		row.appendChild(searchsekolah);
		searchsekolah.setWidth("90%");
		searchsekolah.setWidth("90%");

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
		column.setWidth("30px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("NIS");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Sekolah");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kelas");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Asrama");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tahun");
		column.setWidth("10%");

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
				AmbilDataSiswaBanyak.this.detach();
			}
		});
		cancel.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		button.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				if (eventListener != null && grid.getRows() != null && grid.getRows().getChildren() != null) {
					List<Siswa> siswas = new ArrayList<Siswa>();
					List<Row> rows = grid.getRows().getChildren();
					for (Row row : rows) {
						// Baris pesan/paging tidak selalu mempunyai checkbox atau objek siswa.
						// Jangan memakai exception sebagai alur normal karena setiap klik Simpan
						// sebelumnya menghasilkan NPE dan memenuhi Log Error.
						Object nilaiCheckbox = row == null ? null : row.getAttribute("checkbox");
						Object nilaiSiswa = row == null ? null : row.getAttribute("siswa");
						if (nilaiCheckbox instanceof MyCheckboxConfig && nilaiSiswa instanceof Siswa) {
							MyCheckboxConfig checkbox = (MyCheckboxConfig) nilaiCheckbox;
							if (checkbox.isChecked() && !checkbox.isDisabled()) {
								siswas.add((Siswa) nilaiSiswa);
							}
						}
					}
					Event myEvent = new Event("myEvent", event.getTarget(), siswas);
					eventListener.onEvent(myEvent);
				}
				AmbilDataSiswaBanyak.this.detach();
			}
		});
		button.setParent(toolbar);

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();

		List<Long> values = new ArrayList<Long>();
		if (siswasHanyaDitampilkan != null) {
			for (Siswa siswa : siswasHanyaDitampilkan) {
				values.add(siswa.getId());
			}
		}
		Tbmuser tbmuser = Common.getCurrentUser();
		List<Long> anak = tbmuser != null && tbmuser.getOrangTua() != null ? tbmuser.getOrangTua().ambilAnakSiswa()
				: new ArrayList<Long>();

		List<Long> longsKls = null;
		if (kelas != null && !kelas.getValue().trim().isEmpty()) {
			longsKls = session.createCriteria(KelasSiswaPunyaSiswa.class)
					.setProjection(Projections.property("siswa.id")).createAlias("kelasSiswa", "kelasSiswa")
					.add(Restrictions.ilike("kelasSiswa.nama", kelas.getValue().trim(), MatchMode.ANYWHERE)).list();

		}

		System.out.println("longsKls" + longsKls);

		List<Siswa> siswa = ConstantValues.simpleList(
				session.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa"))
						.add(Restrictions.ne("namaSiswa", "")).add(Restrictions.isNotNull("sekolah"))
						.add(anak.isEmpty() ? Restrictions.sqlRestriction("true") : Restrictions.in("id", anak))

						.addOrder(Order.asc("namaSiswa"))
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(ids.size() == 0 ? Restrictions.sqlRestriction("1!=1") : Restrictions.in("id", ids)),
				Siswa.class);

		List<Siswa> mySiswa = session.createCriteria(Siswa.class)
				.add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa", ""))
				.add(Restrictions.isNotNull("sekolah"))
				.add(anak.isEmpty() ? Restrictions.sqlRestriction("true") : Restrictions.in("id", anak))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.desc("tahunMasuk")).addOrder(Order.asc("nomorInduk"))
				.add(ids.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.not(Restrictions.in("id", ids)))
				.add(siswasHanyaDitampilkan == null || values.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.in("id", values))
				.add(namaSiswa.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("namaSiswa", namaSiswa.getValue().trim(), MatchMode.ANYWHERE))
				.add(kodeSiswaan.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nomorInduk", kodeSiswaan.getValue().trim(), MatchMode.ANYWHERE))

				.add(longsKls == null || longsKls.isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.in("id", longsKls))

				.add(searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
						|| searchsekolah.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false))

				.add(searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
						|| searchyayasan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false))

				.setMaxResults(Common.MAX_RESULT).list();

		siswa.addAll(mySiswa);

		ListModel strset = new SimpleListModel(siswa);
		grid.setRowRenderer(new SiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}
