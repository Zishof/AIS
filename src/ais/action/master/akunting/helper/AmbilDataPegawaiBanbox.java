package ais.action.master.akunting.helper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
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
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.RabUtil;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Pegawai;
import ais.database.model.StatusPegawai;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.rab.Pejabat;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Implementasi pola "Bandbox picker" AIS untuk entity {@link ais.database.model.Pegawai} — lihat
 * {@link ais.ui.util.GetEventListener} untuk arsitektur kerangka umum
 * (constructor/display/onSearchDefault/renderer/callback). {@code Pegawai} adalah master data
 * karyawan/staf kampus (bisa merangkap dosen atau guru lewat relasi {@code dosen}/{@code guru}),
 * dipakai luas di modul akunting untuk memilih penanggung jawab transaksi (mis. pemohon,
 * pemegang uang muka, atasan pemberi persetujuan).
 * <p>
 * Popup menampilkan grid pilih-tunggal (via {@link Radiogroup}) dengan filter "Kode/NIP" (kolom
 * {@code code}/{@code mycode}), "Nama", status pegawai ({@link Combobox} dari {@link StatusPegawai}
 * aktif), dan satuan kerja/unit lewat sub-picker {@code searchparent}
 * ({@link AmbilDataSatuanKerjaBanbox}, mencakup satuan kerja terpilih beserta turunannya). Hasil
 * selalu dibatasi ke pegawai aktif yang tipe pegawainya "masuk presensi" (atau tanpa tipe pegawai).
 * <p>
 * <b>Scoping "atasan-bawahan" (bukan bagian kerangka standar):</b> kecuali pemanggil berstatus
 * admin ({@code admin=true}, lihat {@link Common#getApakahAdminBolehLihatSemuaPegawai()}), hasil
 * dibatasi hanya ke pegawai yang merupakan bawahan langsung/tidak langsung (hingga 3 level:
 * {@code atasanlangsung}, {@code atasanlangsung2}, {@code atasanlangsung3}) dari user login, atau
 * bawahan menurut relasi jabatan ({@code atasan}/{@code atasanPendukung}/
 * {@code atasanPendukungCadangan} pada {@link Pejabat} sesuai {@code jenisJabatan} hak akses user),
 * dihitung sekali di {@link #initAtasan()} saat popup pertama dibuka dan disimpan di
 * {@link #punyaBawahan}/{@link #punyaBawahanDosen}. Bila daftar bawahan ditemukan, filter satuan
 * kerja disembunyikan (scoping bawahan menggantikannya). Bila user tidak boleh melihat data
 * pegawai lain sama sekali ({@code !hakAkses().getMelihatDataPegawaiLain()}), constructor otomatis
 * memilih pegawai milik user sendiri sebagai default lewat {@code RabUtil.setDefaultPegawai}.
 *
 * @see Bandbox
 */
public class AmbilDataPegawaiBanbox extends Bandbox implements GetEventListener {

	/**
	 * Serial version UID standar untuk kompatibilitas serialisasi komponen ZK.
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;


	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;
	private List<Long> punyaBawahan = null;
	private List<Long> punyaBawahanDosen = null;
	private Tbmuser tbmuser;
	private boolean admin;

	/**
	 * Constructor default: mengaktifkan pemilihan pegawai default milik user sendiri
	 * ({@code notDeafault=true}) kecuali user berhak melihat data pegawai lain
	 * ({@link Common#getApakahAdminBolehLihatSemuaPegawai()}).
	 */
	public AmbilDataPegawaiBanbox() {
		this(!Common.getApakahAdminBolehLihatSemuaPegawai());
	}

	/**
	 * Menghitung sekali (saat popup pertama dibuka) daftar id pegawai yang merupakan bawahan
	 * user login, lalu menyimpannya ke {@link #punyaBawahan} (bawahan biasa/jabatan) dan
	 * {@link #punyaBawahanDosen} (bawahan lewat relasi dosen). Tidak melakukan apa pun bila
	 * {@link #admin} bernilai {@code true}. Bila hasil scoping ditemukan, filter satuan
	 * kerja/unit ({@link #searchparent}) disembunyikan karena sudah tidak relevan.
	 */
	@SuppressWarnings("unchecked")
	private void initAtasan() {
		if (!admin) {
			if (tbmuser != null && tbmuser.getPegawai() != null) {
				Session session = HibernateUtil.currentSession();

				Criterion criterion = Restrictions.or(Restrictions.eq("atasanlangsung", tbmuser.getPegawai()),
						Restrictions.or(Restrictions.eq("atasanlangsung3", tbmuser.getPegawai()),
								Restrictions.eq("atasanlangsung2", tbmuser.getPegawai())));

				Criteria criteria = session.createCriteria(Pegawai.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

				if (tbmuser.getDosen() != null) {
					criteria.createAlias("atasanlangsung", "atasanlangsung", Criteria.LEFT_JOIN)
							.createAlias("atasanlangsung2", "atasanlangsung2", Criteria.LEFT_JOIN)
							.createAlias("atasanlangsung3", "atasanlangsung3", Criteria.LEFT_JOIN);

					criterion = Restrictions.or(criterion,
							Restrictions.or(Restrictions.eq("atasanlangsung.dosen", tbmuser.getDosen()),
									Restrictions.or(Restrictions.eq("atasanlangsung3.dosen", tbmuser.getDosen()),
											Restrictions.eq("atasanlangsung2.dosen", tbmuser.getDosen()))));
				} else if (tbmuser.getGuru() != null) {
					criteria.createAlias("atasanlangsung", "atasanlangsung", Criteria.LEFT_JOIN)
							.createAlias("atasanlangsung2", "atasanlangsung2", Criteria.LEFT_JOIN)
							.createAlias("atasanlangsung3", "atasanlangsung3", Criteria.LEFT_JOIN);

					criterion = Restrictions.or(criterion,
							Restrictions.or(Restrictions.eq("atasanlangsung.guru", tbmuser.getGuru()),
									Restrictions.or(Restrictions.eq("atasanlangsung3.guru", tbmuser.getGuru()),
											Restrictions.eq("atasanlangsung2.guru", tbmuser.getGuru()))));
				}

				punyaBawahan = criteria.add(criterion).setProjection(Projections.property("id")).list();

				System.out.println(
						"tbmuser.getPegawai() -> " + tbmuser.getPegawai() + " -> punyaBawahan -> " + punyaBawahan);

				if (tbmuser.hakAkses() != null && tbmuser.hakAkses().getJenisJabatan() != null) {
					List<Long> bawahanJabatan = session.createCriteria(Pegawai.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(Restrictions.or(Restrictions.eq("atasan.id", tbmuser.hakAkses().getJenisJabatan().getId()), Restrictions.or(Restrictions.eq("atasanPendukung.id", tbmuser.hakAkses().getJenisJabatan().getId()), Restrictions.eq("atasanPendukungCadangan.id", tbmuser.hakAkses().getJenisJabatan().getId())) )    )
							.setProjection(Projections.property("id")).list();
					System.out.println(
							"bawahanJabatan -> " + bawahanJabatan + " " + tbmuser.hakAkses().getJenisJabatan());
					if (!bawahanJabatan.isEmpty()) {
						punyaBawahan.addAll(bawahanJabatan);
					}
				}

				else {

					List<Tbmrole> roles = tbmuser.ambilRoles();
					boolean ada = false;
					for (Tbmrole tbmrole : roles) {
						if (tbmrole != null && tbmrole.getJenisJabatan() != null) {
							ada = true;
							break;
						}
					}

					if (!ada) {
						List<Long> pejabats = session.createCriteria(Pejabat.class)

								.setProjection(Projections.groupProperty("jenisJabatan.id"))

								.add(Restrictions.or(
										Restrictions.or(
												Restrictions.ilike("jenisPengguna",
														"," + tbmuser.hakAkses().getRoleId() + ",", MatchMode.ANYWHERE),
												Restrictions.ilike("usernamePengguna", "," + tbmuser.getUserId() + ",",
														MatchMode.ANYWHERE)),
										Restrictions.and(
												Restrictions.or(Restrictions.isNotNull("pegawai"),
														Restrictions.or(Restrictions.isNotNull("guru"),
																Restrictions.isNotNull("dosen"))),
												Restrictions.or(Restrictions.eq("pegawai", tbmuser.getPegawai()),
														Restrictions.or(Restrictions.eq("dosen", tbmuser.getDosen()),
																Restrictions.eq("guru", tbmuser.getGuru()))))))

								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.list();

						System.out.println("pejabats -> " + pejabats);
						if (!pejabats.isEmpty()) {
							List<Long> bawahanJabatan = session.createCriteria(Pegawai.class)
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.add(Restrictions.or(Restrictions.in("atasan.id", pejabats), Restrictions.or( Restrictions.in("atasanPendukung.id", pejabats),  Restrictions.in("atasanPendukungCadangan.id", pejabats))))
									.setProjection(Projections.property("id")).list();
							System.out.println("bawahanJabatan -> " + bawahanJabatan);
							if (!bawahanJabatan.isEmpty()) {
								punyaBawahan.addAll(bawahanJabatan);
							}
						}
					}
				}

				if (!tbmuser.hakAkses().getMelihatDataPegawaiLain()) {
					punyaBawahan.add(tbmuser.getPegawai().getId());
				}
				System.out.println("punyaBawahan -> " + punyaBawahan);

			}

			if (tbmuser != null && tbmuser.getDosen() != null) {
				punyaBawahanDosen = HibernateUtil.currentSession().createCriteria(Pegawai.class)
						.createAlias("dosen", "dosen")
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.or(Restrictions.eq("atasanlangsung", tbmuser.getPegawai()),
								Restrictions.or(Restrictions.eq("atasanlangsung3", tbmuser.getPegawai()),
										Restrictions.eq("atasanlangsung2", tbmuser.getPegawai()))))
						.setProjection(Projections.groupProperty("dosen.id")).list();

				punyaBawahanDosen.add(tbmuser.getDosen().getId());
				System.out.println("punyaBawahanDosen -> " + punyaBawahanDosen);
			}

			if ((punyaBawahan != null && !punyaBawahan.isEmpty())
					|| (punyaBawahanDosen != null && !punyaBawahanDosen.isEmpty())) {
				searchparent.setValue("");
				searchparent.setAttribute("satuanKerja", null);
				searchparent.setAttribute("myValue", null);

				searchparent.setVisible(false);
				labelSatker.setVisible(false);
			}
		}
	}

	/**
	 * @param notDeafault {@code true} untuk otomatis memilih pegawai milik user sendiri sebagai
	 *                    nilai default saat komponen dibuat
	 */
	public AmbilDataPegawaiBanbox(Boolean notDeafault) {
		this(notDeafault, Common.getApakahAdminBolehLihatSemuaPegawai());
	}

	/**
	 * Membuat komponen dan memasang listener {@code onOpen} yang, pada pembukaan pertama,
	 * membangun popup ({@link #display()}) lalu menghitung scoping atasan-bawahan
	 * ({@link #initAtasan()}). Bila {@code notDeafault} atau user tidak boleh melihat data
	 * pegawai lain, pegawai milik user sendiri langsung dipasang sebagai nilai default lewat
	 * {@code RabUtil.setDefaultPegawai}.
	 *
	 * @param notDeafault {@code true} untuk otomatis memilih pegawai milik user sendiri
	 * @param admin       {@code true} untuk menonaktifkan scoping atasan-bawahan (lihat semua
	 *                    pegawai tanpa batas hierarki)
	 */
	public AmbilDataPegawaiBanbox(Boolean notDeafault, Boolean admin) {
		super();
		tbmuser = Common.getCurrentUser();
		this.admin = admin;
		addEventListener("onOpen", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (getChildren().isEmpty()) {
					display();

					initAtasan();

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							setOpen(true);
						}
					});
				}
			}
		});

		boolean tidakBoleh = false;

		if (tbmuser != null && tbmuser.getPegawai() != null && !tbmuser.hakAkses().getMelihatDataPegawaiLain()) {
			tidakBoleh = true;
		}

		if (tbmuser != null && tbmuser.getPegawai() != null && tbmuser.hakAkses().getMelihatDataPegawaiLain()) {
			tidakBoleh = false;
		}

		try {
			if (notDeafault || tidakBoleh) {
				RabUtil.setDefaultPegawai(AmbilDataPegawaiBanbox.this);
			}
		}catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/AmbilDataPegawaiBanbox.java:249");
			// TODO: handle exception
		}

		if (!notDeafault) {
			AmbilDataPegawaiBanbox.this.setDisabled(false);
		}

		setReadonly(true);

	}

	private Textbox kodePegawaian;
	private Textbox nama;
	private Combobox searchstatus;

	/**
	 * Merender satu baris grid pegawai: radio pilih, foto kecil ({@link CommonMedia}), kode+NPWP
	 * (digabung {@link Vbox}), nama, dan satuan kerja. Memilih baris menutup popup, menyimpan
	 * entity {@link Pegawai} terpilih ke attribute {@code "pegawai"} dan {@code "myValue"} pada
	 * Bandbox, mengisi teks tampilan dengan nama pegawai, lalu memicu {@link #eventListener} bila
	 * terpasang — mengikuti kerangka callback standar di {@link ais.ui.util.GetEventListener}.
	 *
	 * @see AmbilDataPegawaiBanbox
	 */
	class PegawaiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Pegawai pegawai = (Pegawai) arg1;

			MyRadioConfig checkbox = new MyRadioConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);

			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataPegawaiBanbox.this.setOpen(false);
					AmbilDataPegawaiBanbox.this.setAttribute("pegawai", pegawai);
					AmbilDataPegawaiBanbox.this.setAttribute("myValue", pegawai);
					AmbilDataPegawaiBanbox.this.setValue(pegawai.getNama());
					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});
			CommonMedia.tampilkanGambarKecil(pegawai).setParent(arg0);

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			new Label(pegawai.getCode()).setParent(vbox);
			new Label(pegawai.getMycode()).setParent(vbox);
			new Label(pegawai.getNpwp()).setParent(vbox);

			new Label(pegawai.getNama()).setParent(arg0);
			new Label(pegawai.getSatuanKerja() == null ? "" : pegawai.getSatuanKerja().getNama()).setParent(arg0);

		}

	}

	private AmbilDataSatuanKerjaBanbox searchparent;
	private SatuanKerjaTreeModel satuanKerjaTreeModel;
	private MyLabelConfig labelSatker;

	/**
	 * Membangun popup pencarian (dipanggil sekali saat pertama dibuka): form filter Kode/NIP,
	 * Nama, Status, dan Satker/Unit, grid hasil bermold "paging" client-side, lalu memuat data
	 * awal lewat timer default (setelah {@link #initAtasan()} sempat dijalankan oleh pemanggil).
	 */
	public void display() throws Exception {
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("950px");
		bandpopup.setHeight("600px");

		Radiogroup radiogroup = new Radiogroup();
		radiogroup.setWidth("100%");
		radiogroup.setHeight("100%");
		radiogroup.setParent(bandpopup);

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(radiogroup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Pegawai");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode/NIP"));
		row.appendChild(kodePegawaian = new Textbox());
		kodePegawaian.setWidth("90%");
		kodePegawaian.addEventListener("onOK", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama = new Textbox());
		nama.setWidth("90%");
		nama.addEventListener("onOK", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status"));
		row.appendChild(searchstatus = new Combobox());
		Common.insertComboDanSemua(searchstatus, "nama", StatusPegawai.class, Restrictions.eq("aktif", true));
		searchstatus.setWidth("90%");

		searchstatus.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});

		row.setParent(rows);
		row.appendChild(labelSatker = new ais.ui.util.MyLabelConfig("Satker/Unit"));
		row.appendChild(searchparent = new AmbilDataSatuanKerjaBanbox());
		searchparent.setWidth("90%");

		searchparent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
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
		column.setLabel("");
		column.setWidth("70px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kode Pegawai");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama Pegawai");
		column.setWidth("35%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Satuan Kerja");

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	/**
	 * Menyusun ulang dan menjalankan kriteria pencarian {@link Pegawai}: menerapkan scoping
	 * atasan-bawahan ({@link #punyaBawahan}/{@link #punyaBawahanDosen}) atau, bila kosong,
	 * scoping satuan kerja dari {@link #searchparent} (kecuali {@link #admin}); lalu menambahkan
	 * filter aktif, tipe pegawai "masuk presensi", nama, kode/NIP, dan status pegawai. Mengisi
	 * ulang grid dengan hasil (maksimum {@link Common#MAX_RESULT_500}) beserta
	 * {@link PegawaiRenderer}.
	 *
	 * @param event tidak dipakai, hanya mengikuti signature standar listener pencarian
	 */
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Pegawai.class);

		if (tbmuser != null && tbmuser.getBiodataCalonMahasiswa() == null) {
			if (punyaBawahan != null && !punyaBawahan.isEmpty() && punyaBawahanDosen != null
					&& !punyaBawahanDosen.isEmpty()) {
				criteria.createAlias("dosen", "dosen", Criteria.LEFT_JOIN)
						.add(Restrictions.or(
								Restrictions.or(Restrictions.in("id", punyaBawahan),
										Restrictions.in("dosen.id", punyaBawahanDosen)),
								Restrictions.or(Restrictions.in("id", punyaBawahan),
										Restrictions.in("dosen.id", punyaBawahanDosen)))

						);

				searchparent.setDisabled(true);

			} else if (punyaBawahan != null && !punyaBawahan.isEmpty()) {
				criteria.add(Restrictions.in("id", punyaBawahan));
				searchparent.setDisabled(true);
			} else if (punyaBawahanDosen != null && !punyaBawahanDosen.isEmpty()) {
				criteria.createAlias("dosen", "dosen", Criteria.LEFT_JOIN)
						.add(Restrictions.in("dosen.id", punyaBawahanDosen));
				searchparent.setDisabled(true);
			} else {

				SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
				Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
				if (parent != null) {
					satuanKerjas.clear();
					satuanKerjas.add(parent);
					satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
				}

				Criterion c = Restrictions.sqlRestriction("false");

				if (!admin) {
					if (tbmuser != null && tbmuser.getDosen() != null && tbmuser.getPegawai() != null) {
						punyaBawahan = new ArrayList<Long>();
						punyaBawahanDosen = new ArrayList<Long>();
						punyaBawahan.add(tbmuser.getPegawai().getId());
						punyaBawahanDosen.add(tbmuser.getDosen().getId());

						c = Restrictions.or(
								Restrictions.or(Restrictions.in("id", punyaBawahan),
										Restrictions.in("dosen.id", punyaBawahanDosen)),
								Restrictions.or(Restrictions.in("id", punyaBawahan),
										Restrictions.in("dosen.id", punyaBawahanDosen)));
					} else if (tbmuser != null && tbmuser.getPegawai() != null) {
						punyaBawahan = new ArrayList<Long>();
						punyaBawahanDosen = new ArrayList<Long>();
						punyaBawahan.add(tbmuser.getPegawai().getId());
						punyaBawahanDosen.add(-1L);

						c = Restrictions.or(
								Restrictions.or(Restrictions.in("id", punyaBawahan),
										Restrictions.in("dosen.id", punyaBawahanDosen)),
								Restrictions.or(Restrictions.in("id", punyaBawahan),
										Restrictions.in("dosen.id", punyaBawahanDosen)));
					} else if (tbmuser != null && tbmuser.getDosen() != null) {
						punyaBawahan = new ArrayList<Long>();
						punyaBawahanDosen = new ArrayList<Long>();
						punyaBawahan.add(-1L);
						punyaBawahanDosen.add(tbmuser.getDosen().getId());

						c = Restrictions.or(
								Restrictions.or(Restrictions.in("id", punyaBawahan),
										Restrictions.in("dosen.id", punyaBawahanDosen)),
								Restrictions.or(Restrictions.in("id", punyaBawahan),
										Restrictions.in("dosen.id", punyaBawahanDosen)));
					}
				}

				System.out.println("parent -> " + parent + " satuanKerjas -> " + satuanKerjas);

				criteria.createAlias("dosen", "dosen", Criteria.LEFT_JOIN)
						.add(satuanKerjas.size() == 0 ? (admin ? Restrictions.sqlRestriction("1=1") : c)
								: Restrictions.or(c, Restrictions.or(Restrictions.isNull("satuanKerja"),
										Restrictions.in("satuanKerja", satuanKerjas))));
			}
		}

		System.out.println("punyaBawahan -> " + punyaBawahan);
		System.out.println("punyaBawahanDosen -> " + punyaBawahanDosen);

		List<Pegawai> pegawai =

				

						criteria.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))

								.createAlias("tipePegawai", "tipePegawai", Criteria.LEFT_JOIN)
								.add(Restrictions.or(Restrictions.isNull("tipePegawai.masukPresensi"),
										Restrictions.eq("tipePegawai.masukPresensi", true)))

								.addOrder(Order.asc("nama"))
								.add(nama.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
										: Restrictions.ilike("nama", nama.getText().trim(), MatchMode.ANYWHERE))
								.add(kodePegawaian.getText().trim().equals("") ? Restrictions.sqlRestriction("1=1")
										: Restrictions.or(
												Restrictions.ilike("code", kodePegawaian.getText().trim(),
														MatchMode.ANYWHERE),
												Restrictions.ilike("mycode", kodePegawaian.getText().trim(),
														MatchMode.ANYWHERE)))
								.add(searchstatus.getSelectedItem() == null
										|| searchstatus.getSelectedItem().getValue() == null
												? Restrictions.sqlRestriction("1=1")
												: Restrictions.eq("statusPegawai",
														searchstatus.getSelectedItem().getValue()))
								.setMaxResults(Common.MAX_RESULT_500).list();

//		System.out.println(pegawai);
		ListModel strset = new SimpleListModel(pegawai);
		grid.setRowRenderer(new PegawaiRenderer());
		grid.setModelCheckMobile(strset);

	}

	/** @param eventListener dipanggil setiap kali user memilih satu pegawai */
	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	/** @return listener pemilihan pegawai yang sedang terpasang, boleh {@code null} */
	public EventListener getEventListener() {
		return eventListener;
	}
}
