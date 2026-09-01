package ais.action.master.sekolah.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
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
import org.zkoss.zul.Paging;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonSearchFilterHelper;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.KelasSiswaPunyaSiswa;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Implementasi pola "Bandbox picker" AIS untuk entity {@link ais.database.model.sekolah.Siswa} —
 * lihat {@link ais.ui.util.GetEventListener} untuk arsitektur kerangka umum (constructor/display/
 * onSearchDefault/renderer/callback). {@code Siswa} adalah entity murid pada modul sekolah.
 *
 * <p>
 * Berbeda dari picker sejenis yang hanya memakai constructor default, kelas ini punya beberapa
 * constructor dengan parameter tambahan yang mengubah scope pencarian: mendukung pencarian siswa
 * aktif biasa (default, filter status aktif diterapkan), atau <b>mode alumni</b> (aktif bila
 * {@code sekolahDari}/{@code tingkatDariAlumni}/{@code kelasDariAlumni}/{@code tahunAkademikAlumni}
 * diisi — lihat {@link #modeAlumni()} — filter status aktif dilewati karena alumni umumnya sudah
 * tidak aktif, difilter berdasarkan riwayat kelas/tingkat/tahun ajaran via
 * {@link KelasSiswaPunyaSiswa}), atau dibatasi ke daftar id tertentu ({@code
 * indsMhsPerkuliahan}). Bila user yang login adalah siswa itu sendiri, komponen otomatis terisi
 * dan terkunci ke datanya sendiri. Popup memakai paging server-side kustom 5 baris/halaman
 * ({@link Paging}, bukan {@code AmbilDataPagingHelper} seperti picker lain di modul ini) dan
 * membuka sesi Hibernate sendiri per pencarian ({@link #onSearchDefault(Event)}). Memilih satu
 * baris pada popup (via radio button) menutup popup, mengisi nilai komponen, dan memicu
 * {@link #eventListener} pemanggil.
 * </p>
 */
public class AmbilDataSiswaBanbox extends Bandbox implements GetEventListener {

	private static final long serialVersionUID = 6452451056684904810L;
	private MyGrid grid;
	private Paging paging;

	private EventListener eventListener;
	private List<Long> indsMhsPerkuliahan = null;
	private Boolean semuaSekolah = false;
	private Sekolah sekolahDari = null;
	private String tingkatDariAlumni = "";
	private String tahunAkademikAlumni = "";
	private boolean sibling = false;
	private String kelasDariAlumni = "";

	private Textbox kode;
	private Textbox nama;
	private Combobox searchyayasan = new Combobox();
	private Combobox searchsekolah = new Combobox();
	private AmbilDataKelasSiswaSemuaBanbox kelas;
	private AmbilDataGuruBanbox wali;

	// Request: Paging per 5 baris
	private static final int PAGE_SIZE_LIMA = 5;

	/** Seperti {@link #AmbilDataSiswaBanbox(Boolean, Boolean)} dengan {@code notDefault=true}, {@code semuaSekolah=false} (mode pencarian siswa aktif biasa, otomatis terisi bila user login adalah siswa). */
	public AmbilDataSiswaBanbox() {
		this(true, false);
	}

	/** Seperti konstruktor default, dengan flag {@code sibling} (dipakai pemanggil untuk menandai konteks "pilih saudara kandung", tidak memengaruhi logika pencarian internal). */
	public AmbilDataSiswaBanbox(boolean sibling) {
		this(true, false);
		this.sibling = sibling;
	}

	/** Seperti {@link #AmbilDataSiswaBanbox(Boolean, Boolean, Sekolah, String, String, String)} tanpa filter mode alumni. */
	public AmbilDataSiswaBanbox(Boolean notDeafault, Boolean semuaSekolah) {
		this(notDeafault, semuaSekolah, null, null, null, null);
	}

	/**
	 * Konstruktor paling lengkap: {@code notDeafault} menentukan apakah user login yang berperan
	 * siswa otomatis mengisi komponen ini (lihat {@link #initSiswaDefault}); parameter
	 * alumni ({@code sekolahDari}/{@code tingkatDariAlumni}/{@code kelasDariAlumni}/
	 * {@code tahunAkademikAlumni}, boleh berisi banyak nilai dipisah koma untuk tingkat/kelas/
	 * tahun) mengaktifkan mode alumni bila salah satunya diisi — lihat {@link #modeAlumni()}.
	 */
	public AmbilDataSiswaBanbox(Boolean notDeafault, Boolean semuaSekolah, Sekolah sekolahDari,
			String tingkatDariAlumni, String kelasDariAlumni, String tahunAkademikAlumni) {
		super();
		this.kelasDariAlumni = kelasDariAlumni;
		this.semuaSekolah = semuaSekolah;
		this.tingkatDariAlumni = tingkatDariAlumni;
		this.tahunAkademikAlumni = tahunAkademikAlumni;
		this.sekolahDari = sekolahDari;
		setReadonly(true);

		initSiswaDefault(notDeafault);
		initOnOpenEvent();
	}

	/** Konstruktor yang membatasi pencarian hanya ke siswa dengan id pada {@code indsMhsPerkuliahan}. */
	public AmbilDataSiswaBanbox(List<Long> indsMhsPerkuliahan) {
		super();
		this.indsMhsPerkuliahan = indsMhsPerkuliahan;

		initSiswaDefault(true);
		initOnOpenEvent();
	}

	/** Mengisi otomatis dan mengunci komponen ke data siswa milik user yang sedang login, bila {@code notDeafault} true dan user tersebut memang berperan siswa. */
	private void initSiswaDefault(Boolean notDeafault) {
		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.getSiswa() != null && notDeafault) {
			Siswa siswa = tbmuser.getSiswa();
			setValue(siswa.getNama());
			setAttribute("myValue", siswa);
			setAttribute("siswa", siswa);
			setDisabled(true);
		}
	}

	/** Mendaftarkan pembangunan popup pencarian ({@link #display()}) secara lazy pada pembukaan pertama komponen (bukan saat konstruksi), lalu memaksa popup tetap terbuka lewat timer singkat. */
	private void initOnOpenEvent() {
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

	/** Renderer baris popup untuk {@link Siswa}: radio pilih (tampil sebagai nama saja bila {@code sekolahDari} diisi, atau lengkap dengan foto/nomor induk/nama/kelas+penjurusan/guru pembina/sekolah pada mode biasa); memilih radio menutup popup dan mengisi komponen. */
	class SiswaRenderer extends ais.ui.util.MyRowRenderer {
		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final Siswa siswa = (Siswa) arg1;

			if (sekolahDari != null && sekolahDari.getId() != null) {
				Radio checkbox = new Radio(siswa.getNamaSiswa());
				checkbox.setParent(arg0);
				arg0.setAttribute("checkbox", checkbox);
				checkbox.addEventListener("onCheck", createCheckListener(siswa));
			} else {
				MyRadioConfig checkbox = new MyRadioConfig();
				checkbox.setParent(arg0);
				arg0.setAttribute("checkbox", checkbox);
				checkbox.addEventListener("onCheck", createCheckListener(siswa));

				CommonMedia.tampilkanGambarKecil(siswa).setParent(arg0);
				new Label(siswa.getNomorInduk()).setParent(arg0);
				new Label(siswa.getNamaSiswa()).setParent(arg0);

				try {
					KelasSiswa kelasSiswa = siswa.getKelas();
					String kelasNama = (kelasSiswa == null ? "" : kelasSiswa.getNama());
					String penjurusan = (siswa.getPenjurusanSekolah() == null ? "" : " " + siswa.getPenjurusanSekolah().getNama());
					new Label(kelasNama + penjurusan).setParent(arg0);
				} catch (Exception e) {
					new Label(siswa.getPenjurusanSekolah() == null ? "" : " " + siswa.getPenjurusanSekolah().getNama()).setParent(arg0);
				}

				new Label(siswa.getGuruPembina() == null ? "" : siswa.getGuruPembina().getNama()).setParent(arg0);
				new Label(siswa.getSekolah() == null ? "" : siswa.getSekolah().getNama()).setParent(arg0);
			}
		}
		
		/** Membuat listener yang menetapkan {@code siswa} sebagai pilihan komponen ini (nilai tampil, atribut {@code myValue}/{@code siswa}), menutup popup, dan memicu {@link #eventListener} pemanggil. */
		private EventListener createCheckListener(final Siswa siswa) {
			return new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataSiswaBanbox.this.setOpen(false);
					AmbilDataSiswaBanbox.this.setAttribute("siswa", siswa);
					AmbilDataSiswaBanbox.this.setAttribute("myValue", siswa);
					AmbilDataSiswaBanbox.this.setValue(siswa.getNama());
					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			};
		}
	}

	/** Membangun kerangka popup pencarian (filter kode/nama/yayasan/sekolah/kelas/wali di utara, grid hasil berpaging di tengah) dan langsung memuat data awal. */
	public void display() {
		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);
		setReadonly(true);

		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("1250px");
		bandpopup.setHeight("600px");

		if (sekolahDari != null && sekolahDari.getId() != null) {
			bandpopup.setWidth("350px");
			bandpopup.setHeight("600px");
		}

		final Radiogroup radiogroup = new Radiogroup();
		radiogroup.setWidth("100%");
		radiogroup.setHeight("100%");
		radiogroup.setParent(bandpopup);

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(radiogroup);
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

		kode = new Textbox();
		nama = new Textbox();
		kelas = new AmbilDataKelasSiswaSemuaBanbox();
		wali = new AmbilDataGuruBanbox();

		// Inisialisasi Paging
		paging = new Paging();
		paging.setPageSize(PAGE_SIZE_LIMA);
		paging.addEventListener("onPaging", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		EventListener searchEvent = new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				paging.setActivePage(0); // Reset Paging ke Hal-1 saat cari ulang
				onSearchDefault(null);
			}
		};

		if (sekolahDari != null && sekolahDari.getId() != null || sibling) {
			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
			row.appendChild(nama);
			nama.setWidth("90%");
			nama.addEventListener("onOK", searchEvent);
		} else if (semuaSekolah) {
			MyFormRow row = new MyFormRow();
			row.setValign("top");
			
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Kode"));
			row.appendChild(kode);
			kode.setWidth("90%");
			kode.addEventListener("onOK", searchEvent);

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
			row.appendChild(nama);
			nama.setWidth("90%");
			nama.addEventListener("onOK", searchEvent);

			row.appendChild(new ais.ui.util.MyLabelConfig("Kelas"));
			row.appendChild(kelas);
			kelas.setWidth("90%");
			kelas.setEventListener(searchEvent);
		} else {
			MyFormRow row = new MyFormRow();
			row.setValign("top");
			
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Kode"));
			row.appendChild(kode);
			kode.setWidth("90%");
			kode.addEventListener("onOK", searchEvent);

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));
			row.appendChild(searchyayasan);
			searchyayasan.setWidth("90%");

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah"));
			row.appendChild(searchsekolah);
			searchsekolah.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
			row.appendChild(nama);
			nama.setWidth("90%");
			nama.addEventListener("onOK", searchEvent);

			row.appendChild(new ais.ui.util.MyLabelConfig("Kelas"));
			row.appendChild(kelas);
			kelas.setWidth("90%");
			kelas.setEventListener(searchEvent);

			row.appendChild(new ais.ui.util.MyLabelConfig("Wali"));
			row.appendChild(wali);
			wali.setWidth("90%");
			wali.setEventListener(searchEvent);
		}

		Toolbar toolbar = new Toolbar();
		ais.ui.util.BanboxFilterToggle.pasang(north, searchgrid, toolbar);
		toolbar.setParent(div);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", searchEvent);
		button.setParent(toolbar);

		toolbar.appendChild(Common.createCleanButton(this, this));

		// Layout grid + Paging diletakkan pada South Container
		Borderlayout gridLayout = new ais.ui.util.MyBorderlayout();
		gridLayout.setParent(center);
		
		Center centerGrid = new Center();
		ais.ui.util.ZkCompat.setFlex(centerGrid, true);
		centerGrid.setParent(gridLayout);
		
		South southGrid = new South();
		southGrid.setParent(gridLayout);

		grid = new MyGrid();
		grid.setWidth("100%");
		// Hapus setting paging internal dari MyGrid karena dikontrol eksternal
		// grid.setMold("paging"); 
		grid.setParent(centerGrid);
		
		paging.setParent(southGrid); // Menempelkan Paging control di bagian bawah tabel

		Columns columns = new Columns();
		columns.setParent(grid);

		if (sekolahDari != null && sekolahDari.getId() != null || sibling) {
			MyColumnConfig column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Nama");
		} else {
			MyColumnConfig column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("");
			column.setWidth("30px");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("");
			column.setWidth("70px");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("NIS");
			column.setWidth("20%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Nama");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Kelas");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Wali Kelas");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Sekolah");
		}

		onSearchDefault(null);
	}

	/** Menentukan apakah komponen ini sedang dalam mode pencarian alumni (salah satu dari {@code sekolahDari}/{@code tingkatDariAlumni}/{@code kelasDariAlumni}/{@code tahunAkademikAlumni} diisi) — memengaruhi apakah filter status aktif diterapkan pada {@link #initCriteria}. */
	private boolean modeAlumni() {
		return (sekolahDari != null && sekolahDari.getId() != null)
				|| (tingkatDariAlumni != null && !tingkatDariAlumni.trim().isEmpty())
				|| (kelasDariAlumni != null && !kelasDariAlumni.trim().isEmpty())
				|| (tahunAkademikAlumni != null && !tahunAkademikAlumni.trim().isEmpty());
	}

	/**
	 * Menyusun kriteria pencarian {@link Siswa}: dibatasi ke {@code sekolahDari} (mode alumni),
	 * kelas ({@link KelasSiswaPunyaSiswa}) dan guru wali terpilih, atau (khusus mode alumni tanpa
	 * wali terpilih) kombinasi tingkat/kelas/tahun ajaran alumni; difilter status aktif kecuali
	 * mode alumni ({@link #modeAlumni()}); difilter ilike nama dan nomor induk/NISN; dibatasi ke
	 * {@code indsMhsPerkuliahan} bila diisi; dan dibatasi ke anak dari user orang tua yang login
	 * bila berlaku. Terurut nama bila {@code isOrder} true.
	 */
	@SuppressWarnings("unchecked")
	public Criteria initCriteria(Session session, boolean isOrder) {
		List<Long> longs = null;
		if (kelas != null && kelas.getAttribute("kelasSiswa") != null) {
			longs = session.createCriteria(KelasSiswaPunyaSiswa.class)
					.setProjection(Projections.property("siswa.id"))
					.add(CommonSearchFilterHelper.eqValue("kelasSiswa", kelas.getAttribute("kelasSiswa"), false)).list();
		}

		List<Long> longsWali = null;
		if (wali != null && wali.getAttribute("guru") != null) {
			longsWali = session.createCriteria(KelasSiswaPunyaSiswa.class)
					.setProjection(Projections.property("siswa.id")).createAlias("kelasSiswa", "kelasSiswa")
					.add(CommonSearchFilterHelper.eqValue("kelasSiswa.guruPembina", wali.getAttribute("guru"), false)).list();
		} else if ((tingkatDariAlumni != null && !tingkatDariAlumni.trim().isEmpty())
				|| (kelasDariAlumni != null && !kelasDariAlumni.trim().isEmpty())) {

			List<Integer> listTingkat = new ArrayList<Integer>();
			for (String s : tingkatDariAlumni.split(",")) {
				try {
					if (!s.trim().isEmpty()) {
						listTingkat.add(Integer.parseInt(s.trim()));
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/AmbilDataSiswaBanbox.java:439");}
			}

			List<String> listKelas = new ArrayList<String>();
			for (String s : kelasDariAlumni.split(",")) {
				try {
					if (!s.trim().isEmpty()) {
						listKelas.add(s.trim());
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/AmbilDataSiswaBanbox.java:448");}
			}

			if (!listTingkat.isEmpty() || !listKelas.isEmpty()) {
				List<String> tas = new ArrayList<String>();
				if (tahunAkademikAlumni != null && !tahunAkademikAlumni.trim().isEmpty()) {
					for (String s : tahunAkademikAlumni.split(",")) {
						if (!s.trim().isEmpty()) {
							tas.add(s.trim());
						}
					}
				}

				longs = session.createCriteria(KelasSiswaPunyaSiswa.class)
						.setProjection(Projections.property("siswa.id"))
						.createAlias("kelasSiswa", "kelasSiswa")
						.add(listTingkat.isEmpty() ? Restrictions.sqlRestriction("1=1") : Restrictions.in("kelasSiswa.tingkat", listTingkat))
						.add(listKelas.isEmpty() ? Restrictions.sqlRestriction("1=1") : Restrictions.in("kelasSiswa.nama", listKelas))
						.add(tas.isEmpty() ? Restrictions.sqlRestriction("1=1") : Restrictions.in("kelasSiswa.tahunAjaran", tas))
						.add(CommonSearchFilterHelper.eqValue("kelasSiswa.sekolah", sekolahDari, false))
						.add(sekolahDari != null && sekolahDari.getId() != null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("kelasSiswa.sekolah", searchsekolah, false))
						.add(sekolahDari != null && sekolahDari.getId() != null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("kelasSiswa.yayasan", searchyayasan, false))
						.list();
			}
		}

		Criteria criteria = session.createCriteria(Siswa.class)
				.add(CommonSearchFilterHelper.eqValue("sekolah", sekolahDari, false))
				.add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa", ""))
				.add(Restrictions.isNotNull("nama")).add(Restrictions.ne("nama", ""))
				.add(Restrictions.isNotNull("sekolah"))
				.add(longs == null ? Restrictions.sqlRestriction("1=1") : longs.isEmpty() ? Restrictions.sqlRestriction("1=0") : Restrictions.in("id", longs))
				.add(longsWali == null ? Restrictions.sqlRestriction("1=1") : longsWali.isEmpty() ? Restrictions.sqlRestriction("1=0") : Restrictions.in("id", longsWali))
				/* Mode alumni (sekolahDari/tingkat/kelas/tahun alumni diisi):
				 * siswa lulusan umumnya sudah berstatus tidak aktif, jadi filter
				 * aktif hanya dipakai pada mode pencarian siswa biasa. */
				.add(modeAlumni() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(indsMhsPerkuliahan == null ? Restrictions.sqlRestriction("1=1") : indsMhsPerkuliahan.isEmpty() ? Restrictions.sqlRestriction("1=0") : Restrictions.in("id", indsMhsPerkuliahan));

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.getOrangTua() != null && !tbmuser.getOrangTua().ambilAnakSiswa().isEmpty()) {
			criteria.add(Restrictions.in("id", tbmuser.getOrangTua().ambilAnakSiswa()));
		}

		if (isOrder) {
			criteria.addOrder(Order.asc("namaSiswa"));
		}

		criteria.add(nama == null || nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1") : Restrictions.ilike("namaSiswa", nama.getValue().trim(), MatchMode.ANYWHERE))
				.add(kode == null || kode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1") : 
						Restrictions.or(Restrictions.ilike("nomorIndukSantri", kode.getValue().trim(), MatchMode.ANYWHERE),
						Restrictions.or(Restrictions.ilike("nomorInduk", kode.getValue().trim(), MatchMode.ANYWHERE),
										Restrictions.ilike("nomorIndukNasional", kode.getValue().trim(), MatchMode.ANYWHERE))))
				.add(sekolahDari != null && sekolahDari.getId() != null ? Restrictions.sqlRestriction("1=1")
						: CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false))
				.add(sekolahDari != null && sekolahDari.getId() != null ? Restrictions.sqlRestriction("1=1")
						: CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false));

		return criteria;
	}

	/** Memuat ulang daftar siswa sesuai filter aktif ke grid popup, membuka sesi Hibernate baru sendiri dan menutupnya di {@code finally} (terpisah dari sesi request agar aman dipanggil dari konteks event popup). */
	@SuppressWarnings({ })
	public void onSearchDefault(Event event) {
		Session session = null;
		try {
			// WAJIB buka session baru dan tutup di finally
			session = HibernateUtil.getSessionFactory().openSession();

			// 1. Dapatkan Total Data untuk Setup Pagination
			Criteria countCriteria = initCriteria(session, false);
			countCriteria.setProjection(Projections.rowCount());
			Long totalData = (Long) countCriteria.uniqueResult();
			
			paging.setTotalSize(totalData != null ? totalData.intValue() : 0);
			paging.setMold("os");
			paging.setDetailed(true);
			// 2. Tentukan Offset Paging dan Tarik Hanya 5 Data Per Halaman
			int activePage = paging.getActivePage() < 0 ? 0 : paging.getActivePage();
			int startOffset = activePage * PAGE_SIZE_LIMA;

			Criteria listCriteria = initCriteria(session, true);
			List<Siswa> siswa = ConstantValues.simpleList( listCriteria
					.setFirstResult(startOffset)
					.setMaxResults(PAGE_SIZE_LIMA)
					,Siswa.class);
			initializeSiswaForRender(siswa);

			ListModel strset = new SimpleListModel(siswa);
			grid.setRowRenderer(new SiswaRenderer());
			grid.setModelCheckMobile(strset);

		} catch (Exception e) {
			/* Error koneksi sementara (mis. DB restart / "terminating connection due to
			 * administrator command") bukan bug aplikasi — jangan dicatat sebagai error keras
			 * agar audit tidak penuh. Error lain tetap dilaporkan. */
			if (!Common.isTransientKoneksiError(e)) {
				Common.tampilErrorJikaAdmin(e);
			}
		} finally {
			/* Rollback transaksi implisit dulu; close() saja meninggalkan koneksi
			 * pool "idle in transaction" yang menahan lock berjam-jam. */
			ais.ui.util.AmbilDataPagingHelper.tutupSessionQuietly(session);
		}
	}

	/** Meng-inisialisasi properti lazy-load ({@link Hibernate#initialize}) yang dibutuhkan renderer pada tiap {@link Siswa} hasil pencarian sebelum sesi Hibernate ditutup, mencegah {@code LazyInitializationException} saat grid dirender. */
	private void initializeSiswaForRender(List<Siswa> siswas) {
		if (siswas == null || siswas.isEmpty()) {
			return;
		}
		for (Siswa siswa : siswas) {
			if (siswa == null) {
				continue;
			}
			try { Hibernate.initialize(siswa.getSekolah()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/AmbilDataSiswaBanbox.java:564");}
			try { Hibernate.initialize(siswa.getKelas()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/AmbilDataSiswaBanbox.java:565");}
			try { Hibernate.initialize(siswa.getPenjurusanSekolah()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/AmbilDataSiswaBanbox.java:566");}
			try { Hibernate.initialize(siswa.getGuruPembina()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/AmbilDataSiswaBanbox.java:567");}
		}
	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}
