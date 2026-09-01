package ais.action.master.sekolah.helper;


import ais.common.CommonSearchFilterHelper;
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
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.Sekolah;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Implementasi pola "Bandbox picker" AIS untuk entity {@link ais.database.model.sekolah.Guru}
 * — lihat {@link ais.ui.util.GetEventListener} untuk arsitektur kerangka umum
 * (constructor/display/onSearchDefault/renderer/callback).
 * <p>
 * Memilih satu {@link Guru} (guru/tenaga pendidik sekolah) dari daftar popup, dengan filter
 * kode/NUPTK, nama, yayasan, dan sekolah, serta pemilihan tunggal lewat {@link Radiogroup}.
 * Kriteria wajib di {@link #onSearchDefault(Event)}: guru harus terhubung ke sekolah
 * ({@code sekolah} tidak null), status kepegawaian berawalan "aktif" atau belum diisi, dan flag
 * {@code aktif} bernilai true atau belum diisi. Kolom pencarian "Kode/NUPTK" mencocokkan field
 * {@code kode} ATAU {@code nuptk} (Nomor Unik Pendidik dan Tenaga Kependidikan) sekaligus.
 * Filter sekolah dicek terhadap EMPAT field penugasan guru ({@code sekolah}, {@code sekolah1},
 * {@code sekolah2}, {@code sekolah3} — satu guru bisa mengajar di lebih dari satu unit sekolah),
 * dan filter sekolah maupun yayasan SAMA SEKALI DILEWATI bila {@code guru.milikUniversitas ==
 * true} (guru level yayasan/universitas, bukan terikat ke satu sekolah tertentu).
 * </p>
 * <p>
 * Paginasi memakai mold client-side {@code grid.setMold("paging")} + {@code setPageSize(50)}
 * dengan hasil dibatasi {@code Common.MAX_RESULT_50} — field {@code pagingHelper}
 * ({@link ais.ui.util.AmbilDataPagingHelper}) dideklarasikan namun TIDAK dipakai di file ini
 * (sisa refactor yang belum tuntas, bukan bug fungsional yang perlu diperbaiki di sini).
 * Constructor {@link #AmbilDataGuruBanbox(Boolean)} punya dua inisialisasi khusus non-standar:
 * (1) bila user yang login BUKAN mahasiswa/siswa dan memiliki yayasan, combo yayasan otomatis
 * dipilihkan ke yayasan user tersebut dan combo sekolah dibatasi ke sekolah-sekolah aktif di
 * yayasan itu saja; (2) terpisah dari itu, bila user yang login sendiri tercatat sebagai
 * {@link Guru}, komponen langsung terisi dengan data guru tersebut dan dinonaktifkan — pola
 * sama seperti subclass Banbox lain untuk user yang "adalah" entity yang dicari.
 * </p>
 *
 * @see Bandbox
 */
public class AmbilDataGuruBanbox extends Bandbox implements GetEventListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452451056684904810L;
	private MyGrid grid;


	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;

	/** Membuat komponen dengan inisialisasi default (lihat {@link #AmbilDataGuruBanbox(Boolean)}). */
	public AmbilDataGuruBanbox() {
		this(true);
	}

	/**
	 * @param notDeafault parameter kompatibilitas kerangka umum (lihat
	 *                     {@link ais.ui.util.GetEventListener}); di file ini nilainya tidak
	 *                     dipakai untuk mengubah alur — pengisian otomatis untuk user yang
	 *                     dirinya sendiri seorang {@link Guru} selalu berjalan
	 */
	public AmbilDataGuruBanbox(Boolean notDeafault) {
		super();
		setReadonly(true);
		Tbmuser tbmuser = Common.getCurrentUser();
		try {

			if (tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null) {
				if (tbmuser.ambilYayasan() != null) {
					Common.selectComboItem(searchyayasan, tbmuser.ambilYayasan());
					Common.clear(searchsekolah);
					Common.insertCombo(searchsekolah, new String[] { "nama", "jenisSekolah" }, "yayasan", Sekolah.class,
							Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
							Restrictions.eq("yayasan", tbmuser.ambilYayasan()));
					// searchyayasan.setDisabled(true);
				} else {
					// searchyayasan.setDisabled(false);
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/AmbilDataGuruBanbox.java:84");
			// TODO: handle exception
		}

		if (tbmuser != null && tbmuser.ambilGuru() != null) {
			Guru guru = tbmuser.ambilGuru();
			setValue(guru.getNama());
			setAttribute("myValue", guru);
			setAttribute("guru", guru);
			setDisabled(true);
		}

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

	private Textbox kode;
	private Textbox nama;

	private Combobox searchyayasan = new Combobox();
	private Combobox searchsekolah = new Combobox();

	/**
	 * Renderer baris hasil pencarian guru: radio pilih (memilih guru, menutup popup, dan memicu
	 * {@code eventListener}), foto kecil, kode+NUPTK (ditumpuk dalam {@link Vbox}), nama, status
	 * kepegawaian, dan kolom "kepemilikan". Kolom kepemilikan dihitung: nama yayasan, ditimpa
	 * nama sekolah utama bila terisi, lalu ditambah nama sekolah kedua/ketiga/keempat
	 * ({@code sekolah1}/{@code sekolah2}/{@code sekolah3}) bila diisi — kecuali bila
	 * {@code guru.getMilikUniversitas() == true}, yang menampilkan teks tetap "Milik Yayasan"
	 * (guru level yayasan, bukan milik satu sekolah).
	 *
	 * @see AmbilDataGuruBanbox
	 */
	class GuruRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Guru guru = (Guru) arg1;
			MyRadioConfig checkbox = new MyRadioConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			// checkbox.setId(guru.getId() + "");

			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataGuruBanbox.this.setOpen(false);
					AmbilDataGuruBanbox.this.setAttribute("guru", guru);
					AmbilDataGuruBanbox.this.setAttribute("myValue", guru);
					AmbilDataGuruBanbox.this.setValue(guru.getNama());
					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});
			CommonMedia.tampilkanGambarKecil(guru).setParent(arg0);

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			new Label(guru.getKode()).setParent(vbox);
			new Label(guru.getNuptk()).setParent(vbox);
			new Label(guru.getNamaGuru()).setParent(arg0);
			new Label(guru.getStatusPegawai() == null ? "" : guru.getStatusPegawai().getNama()).setParent(arg0);
			String milik = "";
			if (guru.getYayasan() != null) {
				milik = guru.getYayasan().getNama();
			}
			if (guru.getSekolah() != null) {
				milik = guru.getSekolah().getNama();
			}

			if (guru.getSekolah1() != null) {
				milik += ", " + guru.getSekolah1().getNama();
			}
			if (guru.getSekolah2() != null) {
				milik += ", " + guru.getSekolah2().getNama();
			}
			if (guru.getSekolah3() != null) {
				milik += ", " + guru.getSekolah3().getNama();
			}

			new Label(
					guru.getMilikUniversitas() != null && guru.getMilikUniversitas() == true ? "Milik Yayasan" : milik)
					.setParent(arg0);

		}

	}

	/**
	 * Menyusun konten popup bandbox (dipanggil sekali saat pertama dibuka): form pencarian
	 * (kode/NUPTK, nama, yayasan, sekolah) dengan tombol cari/bersihkan, grid hasil dengan
	 * paginasi client-side (mold "paging", 50 baris/halaman), lalu memuat data awal lewat
	 * {@link #onSearchDefault(Event)}.
	 *
	 * @see ais.ui.util.GetEventListener
	 */
	public void display() {

		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);

		setReadonly(true);

		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("750px");
		bandpopup.setHeight("600px");

		final Radiogroup radiogroup = new Radiogroup();
		radiogroup.setWidth("100%");
		radiogroup.setHeight("100%");
		radiogroup.setParent(bandpopup);

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(radiogroup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Guru");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode/NUPTK"));
		row.appendChild(kode = new Textbox());
		kode.setWidth("90%");
		kode.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama = new Textbox());
		nama.setWidth("90%");
		nama.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));
		row.appendChild(searchyayasan);
		searchyayasan.setWidth("90%");
		searchyayasan.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah"));
		row.appendChild(searchsekolah);
		searchsekolah.setWidth("90%");
		searchsekolah.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
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
		column.setLabel("");
		column.setWidth("70px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kode/NUPTK");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Status");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("20%");
		column.setLabel("Kepemilikan");

		onSearchDefault(null);

	}

	/**
	 * Memuat ulang grid hasil pencarian guru sesuai filter formulir saat ini, memakai sesi
	 * Hibernate thread-local. Kriteria wajib: {@code sekolah} tidak null, status kepegawaian
	 * berawalan "aktif" atau belum diisi, flag {@code aktif} true/belum diisi. Kode/NUPTK
	 * dicocokkan terhadap field {@code kode} ATAU {@code nuptk}. Filter sekolah memeriksa empat
	 * field penugasan ({@code sekolah}/{@code sekolah1}/{@code sekolah2}/{@code sekolah3}), dan
	 * filter sekolah maupun yayasan dilewati sepenuhnya bila {@code milikUniversitas == true}.
	 * Hasil dibatasi {@code Common.MAX_RESULT_50} baris.
	 *
	 * @param event event pemicu (tidak dipakai — pemanggil selalu mengirim {@code null})
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(Guru.class).add(Restrictions.isNotNull("sekolah"))
				.createAlias("statusPegawai", "statusPegawai", Criteria.LEFT_JOIN)
				.add(Restrictions.or(Restrictions.ilike("statusPegawai.nama", "aktif", MatchMode.START),
						Restrictions.isNull("statusPegawai.nama")))
				.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));

		criteria.addOrder(Order.asc("namaGuru"))
				.add(nama.getText().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("namaGuru", nama.getText().trim(), MatchMode.ANYWHERE))

				.add(kode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.ilike("kode", kode.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("nuptk", kode.getValue().trim(), MatchMode.ANYWHERE))

				)

				.add(Restrictions.or(
						searchsekolah.getSelectedItem() == null
								|| searchsekolah.getSelectedItem().getValue() == null || searchsekolah.getSelectedItem()
										.getValue() == null
												? Restrictions.sqlRestriction("1=1")
												: Restrictions
														.or(CommonSearchFilterHelper.eqSelectedWithId("sekolah3", searchsekolah, false),
																Restrictions.or(
																		CommonSearchFilterHelper.eqSelectedWithId("sekolah2", searchsekolah, false),
																		Restrictions.or(
																				CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false),
																				CommonSearchFilterHelper.eqSelectedWithId("sekolah1", searchsekolah, false)))),
						Restrictions.eq("milikUniversitas", true)))

				.add(Restrictions.or(Restrictions.eq("milikUniversitas", true),
						searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
								|| searchyayasan.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false)));

		List<Guru> guru = criteria.setMaxResults(Common.MAX_RESULT_50).list();

		// System.out.println(guru);
		ListModel strset = new SimpleListModel(guru);
		grid.setRowRenderer(new GuruRenderer());
		grid.setModelCheckMobile(strset);

	}

	/** @param eventListener dipanggil setiap kali user memilih satu guru dari daftar */
	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	/** @return listener pemilihan guru yang sedang terpasang, boleh {@code null} */
	public EventListener getEventListener() {
		return eventListener;
	}
}
