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
 * Tipe khusus untuk ambil data guru banbox. Kelas ini memberi nama dan batas tanggung jawab yang
 * eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Bandbox}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyGrid grid}, {@code
 * ais.ui.util.AmbilDataPagingHelper pagingHelper}, {@code EventListener eventListener}, {@code Textbox kode},
 * {@code Textbox nama}, {@code Combobox searchyayasan}, {@code Combobox searchsekolah}; pembacaan/pencarian
 * ({@code onSearchDefault()}, {@code setEventListener()}, {@code getEventListener()}); operasi domain lain
 * ({@code display()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
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

	public AmbilDataGuruBanbox() {
		this(true);
	}

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
	 * Renderer lokal untuk layar/komponen {@link AmbilDataGuruBanbox}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link AmbilDataGuruBanbox} dan dapat mengakses state
	 * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
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

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}
