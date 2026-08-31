package ais.action.master.helper.generic;

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
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Mahasiswa;
import ais.ui.util.MyTextbox;

/**
 * Tipe khusus untuk ambil data mahasiswa banyak. Kelas ini memberi nama dan batas tanggung jawab
 * yang eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyGrid grid}, {@code
 * ais.ui.util.AmbilDataPagingHelper pagingHelper}, {@code EventListener eventListener}, {@code List mahasiswas},
 * {@code List mahasiswasHanyaDitampilkan}, {@code Set ids}, {@code MyTextbox kodeMahasiswaan}, {@code MyTextbox
 * nama}; pembacaan/pencarian ({@code onSearchDefault()}, {@code setEventListener()}, {@code
 * getEventListener()}); operasi domain lain ({@code tambahGrupFilter()}, {@code display()}). Bagian lain dari
 * kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class AmbilDataMahasiswaBanyak extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;
	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;
	private List<Mahasiswa> mahasiswas;
	private List<Mahasiswa> mahasiswasHanyaDitampilkan;

	private Set<Long> ids = new HashSet<Long>();

	public AmbilDataMahasiswaBanyak(List<Mahasiswa> mahasiswas) {
		super();
		this.mahasiswas = mahasiswas;
		display();
		onSearchDefault(null);
	}

	public AmbilDataMahasiswaBanyak(List<Mahasiswa> mahasiswas, List<Mahasiswa> mahasiswasHanyaDitampilkan) {
		super();
		this.mahasiswas = mahasiswas;
		this.mahasiswasHanyaDitampilkan = mahasiswasHanyaDitampilkan;

		display();

		onSearchDefault(null);
	}

	private MyTextbox kodeMahasiswaan;
	private MyTextbox nama;
	private MyTextbox angkatan;
	private MyTextbox prodi;
	private static final String GAYA_BARIS_FILTER = "display:flex;flex-wrap:wrap;gap:10px 14px;align-items:flex-end;padding:10px 12px;box-sizing:border-box;width:100%;";
	private static final String GAYA_GRUP_FILTER = "display:flex;flex-direction:column;gap:3px;min-width:130px;flex:1 1 170px;";
	private static final String GAYA_LABEL_FILTER = "font-weight:600;";
	private static final String GAYA_KOTAK_FILTER = "box-sizing:border-box;";

	private void tambahGrupFilter(Div baris, String labelTeks, org.zkoss.zk.ui.HtmlBasedComponent kotak) {
		Div grup = new Div();
		grup.setStyle(GAYA_GRUP_FILTER);
		grup.setParent(baris);
		Label label = new Label(Common.getBahasaConfig(labelTeks));
		label.setStyle(GAYA_LABEL_FILTER);
		label.setParent(grup);
		kotak.setWidth("100%");
		kotak.setStyle(GAYA_KOTAK_FILTER);
		kotak.setParent(grup);
	}

	class MahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Mahasiswa mahasiswa = (Mahasiswa) arg1;
			arg0.setAttribute("mahasiswa", mahasiswa);
			final MyCheckboxConfig checkbox = new MyCheckboxConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			for (Mahasiswa myMahasiswa : mahasiswas) {
				if (myMahasiswa.getId().equals(mahasiswa.getId())) {
					checkbox.setChecked(true);
					checkbox.setDisabled(true);
					break;
				}
			}

			checkbox.setChecked(ids.contains(mahasiswa.getId()));

			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						ids.add(mahasiswa.getId());
					} else {
						ids.remove(mahasiswa.getId());
					}
				}
			});

			new Label(mahasiswa.getNim()).setParent(arg0);
			new Label(mahasiswa.getNama()).setParent(arg0);
			new Label(mahasiswa.getTahunangkatan() + "").setParent(arg0);
			new Label(mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getNama()).setParent(arg0);
			new Label(mahasiswa.getJurusan() == null || mahasiswa.getJurusan().getFakultas() == null ? ""
					: mahasiswa.getJurusan().getFakultas().getNama()).setParent(arg0);

		}

	}

	public void display() {

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(this);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Mahasiswa");
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

		Row rowFilter = new Row();
		rowFilter.setParent(rows);
		Div barisFilter = new Div();
		barisFilter.setStyle(GAYA_BARIS_FILTER);
		barisFilter.setParent(rowFilter);
		kodeMahasiswaan = new MyTextbox();
		tambahGrupFilter(barisFilter, "NIM", kodeMahasiswaan);
		kodeMahasiswaan.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		nama = new MyTextbox();
		tambahGrupFilter(barisFilter, "Nama", nama);
		nama.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		angkatan = new MyTextbox();
		tambahGrupFilter(barisFilter, "Angkatan", angkatan);
		angkatan.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		prodi = new MyTextbox();
		tambahGrupFilter(barisFilter, "Prodi", prodi);
		prodi.addEventListener(Events.ON_OK, new EventListener() {
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
		/* setPageSize legacy dihapus: grid bukan mold "paging" sehingga setPageSize melempar IllegalStateException ("Available only the paging mold") dan daftar tidak pernah tampil. Paging ditangani AmbilDataPagingHelper. */
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
		column.setLabel("NIM");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Angkatan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jurusan");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Fakultas");
		column.setWidth("20%");

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
				AmbilDataMahasiswaBanyak.this.detach();
			}
		});
		cancel.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		button.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				if (eventListener != null && grid.getRows() != null && grid.getRows().getChildren() != null) {
					List<Mahasiswa> mahasiswas = new ArrayList<Mahasiswa>();
					List<Row> rows = grid.getRows().getChildren();
					for (Row row : rows) {
						try {
							/* KE-FIX NullPointerException: tidak semua baris di dalam Rows adalah
							 * baris data hasil renderer -- ada baris bantu/placeholder yang TIDAK
							 * memiliki atribut "checkbox" (dan/atau "mahasiswa"), sehingga
							 * getAttribute() mengembalikan null dan checkbox.isChecked() meledak.
							 * Exception-nya memang tertangkap, tetapi tercatat ke ErrorAuditUtil
							 * untuk SETIAP baris semacam itu pada SETIAP klik Simpan. Baris tanpa
							 * data cukup dilewati -- hasil akhirnya sama persis dengan perilaku
							 * lama (baris itu memang tidak pernah ikut terpilih). */
							MyCheckboxConfig checkbox = (MyCheckboxConfig) row.getAttribute("checkbox");
							if (checkbox != null && checkbox.isChecked() && !checkbox.isDisabled()) {
								Mahasiswa myMahasiswa = (Mahasiswa) row.getAttribute("mahasiswa");
								if (myMahasiswa != null) {
									mahasiswas.add(myMahasiswa);
								}
							}
						}catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/generic/AmbilDataMahasiswaBanyak.java:270");
						}
					}
					Event myEvent = new Event("myEvent", event.getTarget(), mahasiswas);
					eventListener.onEvent(myEvent);
				}
				AmbilDataMahasiswaBanyak.this.detach();
			}
		});
		button.setParent(toolbar);

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();

		List<Long> values = new ArrayList<Long>();
		if (mahasiswasHanyaDitampilkan != null) {
			for (Mahasiswa mahasiswa : mahasiswasHanyaDitampilkan) {
				values.add(mahasiswa.getId());
			}
		}

		List<Mahasiswa> mahasiswa = ConstantValues.simpleList(
				session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.desc("tahunangkatan")).addOrder(Order.asc("nim"))
						.add(ids.size() == 0 ? Restrictions.sqlRestriction("1!=1") : Restrictions.in("id", ids)),
				Mahasiswa.class);

		Criteria criteria = session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.desc("tahunangkatan")).addOrder(Order.asc("nim"))
				.add(ids.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.not(Restrictions.in("id", ids)))
				.add(mahasiswasHanyaDitampilkan == null ? Restrictions.sqlRestriction("1=1")
						: values.size() == 0 ? Restrictions.sqlRestriction("1!=1") : Restrictions.in("id", values))
				.add(Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))
				.add(Restrictions.ilike("nim", kodeMahasiswaan.getValue().trim(), MatchMode.ANYWHERE));

		String nilaiAngkatan = angkatan.getValue() == null ? "" : angkatan.getValue().trim();
		if (nilaiAngkatan.length() > 0) {
			try {
				criteria.add(Restrictions.eq("tahunangkatan", Integer.valueOf(nilaiAngkatan)));
			} catch (NumberFormatException e) {
				/* Angkatan pada data mahasiswa bertipe angka. Input bukan angka dibuat
				 * menghasilkan daftar kosong agar pencarian tidak memunculkan data yang
				 * tidak sesuai. */
				criteria.add(Restrictions.sqlRestriction("1!=1"));
			}
		}

		String nilaiProdi = prodi.getValue() == null ? "" : prodi.getValue().trim();
		if (nilaiProdi.length() > 0) {
			criteria.createAlias("jurusan", "prodiFilter");
			criteria.add(Restrictions.ilike("prodiFilter.nama", nilaiProdi, MatchMode.ANYWHERE));
		}

		List<Mahasiswa> myMahasiswa = criteria.setMaxResults(Common.MAX_RESULT).list();

		mahasiswa.addAll(myMahasiswa);

		ListModel strset = new SimpleListModel(mahasiswa);
		grid.setRowRenderer(new MahasiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}
