package ais.action.master.surat.helper;


import ais.common.CommonSearchFilterHelper;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
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
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Tbmuser;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.surat.KlasifikasiSuratKeluar;
import ais.database.model.surat.KlasifikasiSuratKeluarUntuk;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Tipe khusus untuk ambil data klasifikasi surat keluar banbox. Kelas ini memberi nama dan batas
 * tanggung jawab yang eksplisit pada perilaku yang diwarisi atau kontrak yang
 * diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Bandbox}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyGrid grid}, {@code
 * ais.ui.util.AmbilDataPagingHelper pagingHelper}, {@code EventListener eventListener}, {@code
 * SatuanKerjaTreeModel satuanKerjaTreeModel}, {@code String tipe}, {@code MyTextbox
 * kodeKlasifikasiSuratKeluaran}, {@code MyTextbox nama}, {@code MyTextbox nomor}; pembacaan/pencarian ({@code
 * onSearchDefault()}, {@code setEventListener()}, {@code getEventListener()}); mutasi data ({@code setTipe()});
 * operasi domain lain ({@code display()}); konfigurasi constructor: {@code satuanKerjaTreeModel}. Bagian lain
 * dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see Bandbox
 */
public class AmbilDataKlasifikasiSuratKeluarBanbox extends Bandbox {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;

	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;

	private SatuanKerjaTreeModel satuanKerjaTreeModel;
	private String tipe;

	public void setTipe(String tipe) {
		this.tipe = tipe;
	}

	public AmbilDataKlasifikasiSuratKeluarBanbox() {
		this("surat");
	}

	public AmbilDataKlasifikasiSuratKeluarBanbox(String tipe) {
		super();
		this.tipe = tipe;
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
		setReadonly(true);
		this.addEventListener(Events.ON_OK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (getValue().trim().equals("")) {
					setAttribute("klasifikasiSuratKeluar", null);
					setValue("");
					return;
				}

				KlasifikasiSuratKeluar klasifikasiSuratKeluar = (KlasifikasiSuratKeluar) HibernateUtil.currentSession()
						.createCriteria(KlasifikasiSuratKeluar.class).add(Restrictions.ilike("kode",
								AmbilDataKlasifikasiSuratKeluarBanbox.this.getValue().trim(), MatchMode.EXACT))
						.setMaxResults(1).uniqueResult();
				if (klasifikasiSuratKeluar == null) {
					MyMessageboxConfig.show(
							"Klasifikasi Surat Keluar dengan kode = "
									+ AmbilDataKlasifikasiSuratKeluarBanbox.this.getValue().trim() + " tidak ditemukan",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}
				AmbilDataKlasifikasiSuratKeluarBanbox.this.setOpen(false);
				AmbilDataKlasifikasiSuratKeluarBanbox.this.setAttribute("klasifikasiSuratKeluar",
						klasifikasiSuratKeluar);
				AmbilDataKlasifikasiSuratKeluarBanbox.this.setValue(klasifikasiSuratKeluar.getKode()
						+ (klasifikasiSuratKeluar.getNama() == null || klasifikasiSuratKeluar.getNama().trim().isEmpty()
								? ""
								: "-" + klasifikasiSuratKeluar.getNama()));
				if (eventListener != null) {
					eventListener.onEvent(arg0);
				}
			}
		});

		try {
			display();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/surat/helper/AmbilDataKlasifikasiSuratKeluarBanbox.java:123");
		}

		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (grid == null || grid.getRows() == null || grid.getRows().getChildren() == null
						|| grid.getRows().getChildren().size() == 0) {
					onSearchDefault(null);
				}
			}
		});
	}

	private MyTextbox kodeKlasifikasiSuratKeluaran;
	private MyTextbox nama;
	private MyTextbox nomor;
	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private AmbilDataSatuanKerjaBanbox satuanKerja;
	private Combobox yayasan;
	private Combobox sekolah;

	class KlasifikasiSuratKeluarRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final KlasifikasiSuratKeluar klasifikasiSuratKeluar = (KlasifikasiSuratKeluar) arg1;

			EventListener s = new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataKlasifikasiSuratKeluarBanbox.this.setOpen(false);
					AmbilDataKlasifikasiSuratKeluarBanbox.this.setAttribute("klasifikasiSuratKeluar",
							klasifikasiSuratKeluar);
					AmbilDataKlasifikasiSuratKeluarBanbox.this
							.setValue(klasifikasiSuratKeluar.getKode() + (klasifikasiSuratKeluar.getNama() == null
									|| klasifikasiSuratKeluar.getNama().trim().isEmpty() ? ""
											: "-" + klasifikasiSuratKeluar.getNama()));
					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			};

			arg0.addEventListener("onClick", s);

			Radio aa;
			(aa = new Radio(klasifikasiSuratKeluar.getKode())).setParent(arg0);
			aa.addEventListener("onClick", s);
			RevisiHelper.createNewRevisi(KlasifikasiSuratKeluar.class, klasifikasiSuratKeluar,
					klasifikasiSuratKeluar.getNama()).setParent(arg0);
			new Label(klasifikasiSuratKeluar.getSifat()).setParent(arg0);
			new Label(klasifikasiSuratKeluar.getKlasifikasiSuratKeluarUntuk().getNama()).setParent(arg0);
			new Label(klasifikasiSuratKeluar.getNomorSurat() == null ? ""
					: klasifikasiSuratKeluar.getNomorSurat().getContohFormat()).setParent(arg0);
			new Label(klasifikasiSuratKeluar.getAlurPersetujuanSuratKeluar() == null ? ""
					: klasifikasiSuratKeluar.getAlurPersetujuanSuratKeluar().toString()).setParent(arg0);

		}

	}

	public void display() throws Exception {
		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("1000px");
		bandpopup.setHeight("600px");

		Radiogroup radiogroup = new Radiogroup();
		radiogroup.setParent(bandpopup);

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(radiogroup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Klasifikasi Surat Keluar");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode"));
		row.appendChild(kodeKlasifikasiSuratKeluaran = new MyTextbox());
		kodeKlasifikasiSuratKeluaran.setWidth("90%");
		kodeKlasifikasiSuratKeluaran.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama = new MyTextbox());
		nama.setWidth("90%");
		nama.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		row.appendChild(satuanKerja = new AmbilDataSatuanKerjaBanbox());
		satuanKerja.setWidth("90%");
		satuanKerja.setEventListener(new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nomor"));
		row.appendChild(nomor = new MyTextbox());
		nomor.setWidth("90%");
		nomor.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row = new MyFormRow();
		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas = new Combobox(),
				searchjurusan = new Combobox());
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.addEventListener("onChange", new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.addEventListener("onChange", new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});
		Tbmuser tbmuser = Common.getCurrentUser();

		boolean[] ptYa = Common.chekPtAtauSekolah();
		boolean pt = ptYa[0];
		boolean ya = ptYa[1];

		searchfakultas.getParent().setVisible(pt && searchfakultas.getChildren().size() > 1);
		searchjurusan.getParent().setVisible(pt && searchfakultas.getChildren().size() > 1);

		yayasan = new Combobox();
		sekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		row = new MyFormRow();
		row.setVisible(ya);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));
		row.appendChild(yayasan);
		yayasan.setWidth("90%");

		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah"));
		row.appendChild(sekolah);
		sekolah.setWidth("90%");

		if (tbmuser.getMahasiswa() != null) {
			searchjurusan.setSelectedIndex(-1);
			searchfakultas.setSelectedIndex(-1);
			searchjurusan.setDisabled(false);
			searchfakultas.setDisabled(false);

			searchfakultas.getParent().setVisible(false);
			searchjurusan.getParent().setVisible(false);
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
		toolbar.appendChild(Common.createCleanButton(this, new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (eventListener != null) {
					try {
						eventListener.onEvent(null);
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
				}
				onSearchDefault(event);
			}
		}));

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
		column.setLabel("Kode");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Sifat");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Peruntukkan");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nomor Surat");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Alur Persetujuan");

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Tbmuser tbmuser = Common.getCurrentUser();
		Jurusan jurusan = tbmuser.getMahasiswa() != null ? tbmuser.getMahasiswa().getJurusan() : tbmuser.ambilJurusan();
		Fakultas fakultas = tbmuser.getMahasiswa() != null ? tbmuser.getMahasiswa().getJurusan().getFakultas()
				: tbmuser.ambilFakultas();

		Criterion criterion1 = Restrictions.or(
				fakultas == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("fakultas", fakultas),
				Restrictions.isNull("fakultas"));
		Criterion criterion2 = Restrictions.or(
				jurusan == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("jurusan", jurusan),
				Restrictions.isNull("jurusan"));

		Criterion criterion = Restrictions.and(criterion1, criterion2);

		KlasifikasiSuratKeluarUntuk umum = (KlasifikasiSuratKeluarUntuk) ConstantValues.simpleObject(
				HibernateUtil.currentSession().createCriteria(KlasifikasiSuratKeluarUntuk.class)
						.add(Restrictions.ilike("nama", "Umum", MatchMode.EXACT)).setMaxResults(1),
				KlasifikasiSuratKeluarUntuk.class);

		if (tbmuser.getMahasiswa() != null) {
			List<KlasifikasiSuratKeluarUntuk> klasifikasiSuratKeluarUntuk = ConstantValues
					.simpleList(HibernateUtil.currentSession().createCriteria(KlasifikasiSuratKeluarUntuk.class)
							.add(Restrictions.or(Restrictions.isNotNull("statusMahasiswa"), Restrictions.or(

									Restrictions.ilike("nama", "Mahasiswa", MatchMode.EXACT),
									Restrictions.isNotNull("statusAwalMahasiswa"))

							)), KlasifikasiSuratKeluarUntuk.class);
			criterion = Restrictions.and(criterion,
					klasifikasiSuratKeluarUntuk.isEmpty() ? Restrictions.sqlRestriction("false")
							: Restrictions.in("klasifikasiSuratKeluarUntuk", klasifikasiSuratKeluarUntuk));
		}

		if (tbmuser.getSiswa() != null) {
			List<KlasifikasiSuratKeluarUntuk> klasifikasiSuratKeluarUntuk = ConstantValues
					.simpleList(
							HibernateUtil.currentSession().createCriteria(KlasifikasiSuratKeluarUntuk.class)
									.add(Restrictions.ilike("nama", "Siswa", MatchMode.EXACT)),
							KlasifikasiSuratKeluarUntuk.class);
			criterion = Restrictions.and(criterion,
					klasifikasiSuratKeluarUntuk.isEmpty() ? Restrictions.sqlRestriction("false")
							: Restrictions.in("klasifikasiSuratKeluarUntuk", klasifikasiSuratKeluarUntuk));
		}

		if (tbmuser.ambilDosen() != null) {
			KlasifikasiSuratKeluarUntuk klasifikasiSuratKeluarUntuk = (KlasifikasiSuratKeluarUntuk) ConstantValues
					.simpleObject(
							HibernateUtil.currentSession().createCriteria(KlasifikasiSuratKeluarUntuk.class)
									.add(Restrictions.ilike("nama", "Dosen", MatchMode.EXACT)).setMaxResults(1),
							KlasifikasiSuratKeluarUntuk.class);

			criterion = Restrictions.and(criterion,
					Restrictions.or(Restrictions.eq("klasifikasiSuratKeluarUntuk", umum),
							Restrictions.eq("klasifikasiSuratKeluarUntuk", klasifikasiSuratKeluarUntuk)));
		}

		if (tbmuser.ambilGuru() != null) {
			KlasifikasiSuratKeluarUntuk klasifikasiSuratKeluarUntuk = (KlasifikasiSuratKeluarUntuk) ConstantValues
					.simpleObject(
							HibernateUtil.currentSession().createCriteria(KlasifikasiSuratKeluarUntuk.class)
									.add(Restrictions.ilike("nama", "Guru", MatchMode.EXACT)).setMaxResults(1),
							KlasifikasiSuratKeluarUntuk.class);

			criterion = Restrictions.and(criterion,
					Restrictions.or(Restrictions.eq("klasifikasiSuratKeluarUntuk", umum),
							Restrictions.eq("klasifikasiSuratKeluarUntuk", klasifikasiSuratKeluarUntuk)));
		}

		criterion = Restrictions.and(criterion,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		String sql = "(this_.kode_grup_pengguna is null or trim(this_.kode_grup_pengguna)='' or '"
				+ tbmuser.hakAkses().getRoleId() + "' = ANY(string_to_array(this_.kode_grup_pengguna,';')) )";
		criterion = Restrictions.and(criterion, Restrictions.sqlRestriction(sql));

		SatuanKerja parent = (SatuanKerja) satuanKerja.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Session session = HibernateUtil.currentSession();

		List<KlasifikasiSuratKeluar> klasifikasiSuratKeluar =

				
								session.createCriteria(KlasifikasiSuratKeluar.class)
										.add(Restrictions
												.or(Restrictions.isNull("tipe"), Restrictions.eq("tipe", tipe)))
										.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

										.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("true")
												: Restrictions.or(Restrictions.isNull("satuanKerja"),
														Restrictions.in("satuanKerja", satuanKerjas)))

										.createAlias("nomorSurat", "nomorSurat", Criteria.LEFT_JOIN)
										.addOrder(Order.asc("nama"))
										.add(nomor.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
												: Restrictions.ilike("nomorSurat.contohFormat", nomor.getValue().trim(),
														MatchMode.ANYWHERE))
										.add(Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))
										.add(kodeKlasifikasiSuratKeluaran.getValue().trim().equals("")
												? Restrictions.sqlRestriction("1=1")
												: Restrictions.ilike("kode",
														kodeKlasifikasiSuratKeluaran.getValue().trim(),
														MatchMode.ANYWHERE))

										.add(searchjurusan.getSelectedItem() == null
												|| searchjurusan.getSelectedItem().getValue() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.or(Restrictions.isNull("jurusan"),
																CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false)))

										.add(searchfakultas.getSelectedItem() == null
												|| searchfakultas.getSelectedItem().getValue() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.or(Restrictions.isNull("fakultas"),
																CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false)))

										.add(sekolah.getSelectedItem() == null
												|| sekolah.getSelectedItem().getValue() == null
												|| sekolah.getSelectedItem().getValue() == null
														? Restrictions.sqlRestriction("1=1")
														: CommonSearchFilterHelper.eqSelectedWithId("sekolah", sekolah, false))

										.add(yayasan.getSelectedItem() == null
												|| yayasan.getSelectedItem().getValue() == null
												|| yayasan.getSelectedItem().getValue() == null
														? Restrictions.sqlRestriction("1=1")
														: CommonSearchFilterHelper.eqSelectedWithId("yayasan", yayasan, false))

										.add(criterion).setMaxResults(Common.MAX_RESULT_500).list();
		ListModel strset = new SimpleListModel(klasifikasiSuratKeluar);
		grid.setRowRenderer(new KlasifikasiSuratKeluarRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}
