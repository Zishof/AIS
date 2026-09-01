package ais.action.master.helper;


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

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Tbmuser;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Implementasi pola "Bandbox picker" AIS untuk entity {@link ais.database.model.Dosen} — lihat
 * {@link ais.ui.util.GetEventListener} untuk arsitektur kerangka umum
 * (constructor/display/onSearchDefault/renderer/callback). {@code Dosen} adalah master data
 * pengajar/dosen kampus, boleh terikat ke satu fakultas/jurusan (home-base) atau ditandai
 * {@code milikUniversitas} (dosen "lintas prodi" yang tidak terikat satu jurusan saja, mis. dosen
 * mata kuliah umum/MKU).
 * <p>
 * Popup menampilkan grid pilih-tunggal (via {@link Radiogroup}) dengan filter "NIDN/Kode" (cocok
 * {@code mycode}, {@code nidn}, atau {@code code}), "Nama", "Fakultas" dan "Prodi" (combobox
 * berjenjang, diinisialisasi lewat {@code Common.initFakultasDanJurusanDanSemua}), ditambah
 * toggle "Lintas prodi" (default AKTIF) yang bila dicentang menampilkan dosen
 * {@code milikUniversitas=true} SEBAGAI PENGGANTI filter fakultas/jurusan biasa (union logika:
 * cocok jurusan ATAU milik universitas). Hasil selalu dibatasi ke dosen berstatus kepegawaian
 * "aktif" (prefiks nama status, atau tanpa status) dan {@code aktif=true}/null; bila
 * {@link #hanyaDosenTetap} diaktifkan lewat {@link #setHanyaDosenTetap(boolean)}, ditambah syarat
 * punya {@code ikatanKerjaDosen.tetap=true} (dosen tetap, bukan honorer/luar biasa). Bila komponen
 * dibuat dalam konteks satu {@link PerguruanTinggi}, hasil juga dibatasi ke dosen milik perguruan
 * tinggi tersebut (atau tanpa perguruan tinggi).
 * <p>
 * <b>Empat constructor dengan perilaku tidak simetris</b> (catat sebagai kekhasan, bukan
 * dianggap error dokumentasi): hanya constructor 4-parameter
 * {@link #AmbilDataDosenBanbox(Boolean, Boolean, Boolean)} yang benar-benar memakai nilai
 * parameter yang diberikan; constructor 1 dan 2 parameter mendelegasikan ke overload lain dengan
 * nilai HARDCODE, mengabaikan sebagian argumen yang diterima dari pemanggil — perilaku ini sudah
 * ada di kode asli dan TIDAK diubah di sini. Bila {@code semua=false} dan user login adalah dosen
 * itu sendiri, constructor otomatis memasang dosen tersebut sebagai nilai default dan
 * menonaktifkan komponen ({@code setDisabled(true)}).
 *
 * @see Bandbox
 */
public class AmbilDataDosenBanbox extends Bandbox implements GetEventListener {

	/**
	 * Serial version UID standar untuk kompatibilitas serialisasi komponen ZK.
	 */
	private static final long serialVersionUID = 6452451056684904810L;
	private MyGrid grid;


	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;
	private Tbmuser tbmuser;
	private PerguruanTinggi perguruanTinggi;
	private Boolean tanpaLihatPt;
	private boolean hanyaDosenTetap;

	/** Constructor default: mendelegasikan ke {@link #AmbilDataDosenBanbox(Boolean)} dengan {@code notDeafault=true}. */
	public AmbilDataDosenBanbox() {
		this(true);
	}

	/**
	 * Catatan: parameter {@code notDeafault} TIDAK dipakai — constructor ini selalu
	 * mendelegasikan ke {@link #AmbilDataDosenBanbox(Boolean, Boolean)} dengan
	 * {@code (true, false)}, mengabaikan nilai argumen yang diberikan pemanggil. Perilaku ini
	 * sudah ada di kode asli.
	 *
	 * @param notDeafault diabaikan, lihat catatan di atas
	 */
	public AmbilDataDosenBanbox(Boolean notDeafault) {
		this(true, false);
	}

	/**
	 * Catatan: KEDUA parameter TIDAK dipakai — constructor ini selalu mendelegasikan ke
	 * {@link #AmbilDataDosenBanbox(Boolean, Boolean, Boolean)} dengan {@code (false, true, false)},
	 * mengabaikan nilai argumen yang diberikan pemanggil. Perilaku ini sudah ada di kode asli.
	 *
	 * @param notDeafault diabaikan, lihat catatan di atas
	 * @param tanpaLihatPt diabaikan, lihat catatan di atas
	 */
	public AmbilDataDosenBanbox(Boolean notDeafault, Boolean tanpaLihatPt) {
		this(false, true, false);
	}

	/**
	 * Membangun komponen (satu-satunya constructor yang benar-benar memakai seluruh
	 * parameternya): menginisialisasi filter fakultas/prodi mengikuti fakultas user login (bila
	 * user bukan mahasiswa/siswa), memasang dosen milik user login sebagai nilai default dan
	 * menonaktifkan komponen bila {@code semua=false} dan user adalah dosen, lalu memasang
	 * listener {@code onOpen} yang membangun popup ({@link #display()}) pada pembukaan pertama.
	 *
	 * @param semua        {@code false} untuk otomatis memilih dosen milik user login sendiri
	 *                     (bila user adalah dosen) dan menonaktifkan komponen
	 * @param notDeafault  disimpan tapi tidak dipakai langsung di sini (dipakai lewat kombinasi
	 *                     parameter lain pada overload di atas)
	 * @param tanpaLihatPt {@code true} untuk mengabaikan scoping {@link PerguruanTinggi} pada
	 *                     pencarian
	 */
	public AmbilDataDosenBanbox(Boolean semua, Boolean notDeafault, Boolean tanpaLihatPt) {
		super();
		this.tanpaLihatPt = tanpaLihatPt;
		setReadonly(true);
		perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
		tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null) {
			if (tbmuser.ambilFakultas() != null) {
				Common.selectComboItem(searchfakultas, tbmuser.ambilFakultas());
				Common.clear(searchjurusan);
				Common.insertCombo(searchjurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						Restrictions.eq("fakultas", tbmuser.ambilFakultas()));
				// searchfakultas.setDisabled(true);
			} else {
				// searchfakultas.setDisabled(false);
			}
		}

		try {
			if (!semua && tbmuser != null && tbmuser.ambilDosen() != null) {

				Dosen dosen = tbmuser == null ? null : tbmuser.ambilDosen();
				setValue(dosen.getNama());
				setAttribute("myValue", dosen);
				setAttribute("dosen", dosen);
				setDisabled(true);

			} else {
				setValue("=Dosen=");
			}

		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataDosenBanbox.java:113");
			// TODO: handle exception
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

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private MyCheckboxConfig milikUniversitas;

	/**
	 * Merender satu baris grid: radio pilih, foto kecil ({@link CommonMedia}), kode+NIDN
	 * (digabung {@link Vbox}), nama, dan nama jurusan. Memilih baris menutup popup, menyimpan
	 * entity {@link Dosen} terpilih ke attribute {@code "dosen"}/{@code "myValue"} pada Bandbox,
	 * mengisi teks tampilan dengan namanya, lalu memicu {@link #eventListener} bila terpasang —
	 * mengikuti kerangka callback standar di {@link ais.ui.util.GetEventListener}.
	 *
	 * @see AmbilDataDosenBanbox
	 */
	class DosenRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Dosen dosen = (Dosen) arg1;
			MyRadioConfig checkbox = new MyRadioConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);

			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataDosenBanbox.this.setOpen(false);
					AmbilDataDosenBanbox.this.setAttribute("dosen", dosen);
					AmbilDataDosenBanbox.this.setAttribute("myValue", dosen);
					AmbilDataDosenBanbox.this.setValue(dosen.getNama());
					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});
			CommonMedia.tampilkanGambarKecil(dosen).setParent(arg0);

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			new Label(dosen.getCode()).setParent(vbox);
			new Label(dosen.getMycode()).setParent(vbox);
			new Label(dosen.getNidn()).setParent(vbox);
			new Label(dosen.getNama()).setParent(arg0);
			new Label(dosen.getJurusan() == null ? "" : dosen.getJurusan().getNama()).setParent(arg0);

		}

	}

	/**
	 * Membangun popup pencarian (dipanggil sekali saat pertama dibuka): form filter NIDN/Kode,
	 * Nama, Fakultas, Prodi, toggle "Lintas prodi", grid hasil bermold "paging", lalu memuat
	 * data awal lewat {@link #onSearchDefault(Event)}. Setelah render, filter fakultas/prodi
	 * dikunci/dibuka tergantung apakah user login adalah mahasiswa dan konfigurasi
	 * {@code bisa_pilih_prodi_lain_saat_pilih_dosen_pa}.
	 */
	public void display() {

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		setReadonly(true);

		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("850px");
		bandpopup.setHeight("600px");

		Radiogroup radiogroup = new Radiogroup();
		radiogroup.setWidth("100%");
		radiogroup.setHeight("100%");
		radiogroup.setParent(bandpopup);

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(radiogroup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Dosen");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("NIDN/Kode"));
		row.appendChild(kode = new Textbox());
		kode.setWidth("90%");

		kode.addEventListener("onOK", new EventListener() {
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");

		searchfakultas.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");

		searchjurusan.addEventListener("onChange", new EventListener() {
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

		milikUniversitas = new MyCheckboxConfig("Lintas prodi");
		toolbar.appendChild(milikUniversitas);
		milikUniversitas.setChecked(true);

		milikUniversitas.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});

		grid = new MyGrid();
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
		column.setLabel("Kode/NIDN/NUPN");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Homebase");

		onSearchDefault(null);
		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Tbmuser tbmuser = Common.getCurrentUser();
				if (tbmuser != null && tbmuser.getMahasiswa() != null) {
					boolean bisa_pilih_prodi_lain_saat_pilih_dosen_pa = Common.bolehKonfigurasi("bisa_pilih_prodi_lain_saat_pilih_dosen_pa");
					searchfakultas.setDisabled(!bisa_pilih_prodi_lain_saat_pilih_dosen_pa);
					searchjurusan.setDisabled(!bisa_pilih_prodi_lain_saat_pilih_dosen_pa);
				} else {
					searchfakultas.setDisabled(false);
					searchjurusan.setDisabled(false);
				}

			}
		});
	}

	/**
	 * Menyusun dan menjalankan kriteria pencarian {@link Dosen}: status kepegawaian aktif, (bila
	 * {@link #hanyaDosenTetap}) punya ikatan kerja tetap, cocok nama dan kode/NIDN (ILIKE
	 * ANYWHERE), cocok jurusan ATAU {@code milikUniversitas} (toggle "Lintas prodi"), cocok
	 * fakultas ATAU {@code milikUniversitas} (dilewati sepenuhnya bila
	 * {@link #tanpaLihatPt} — nama parameter menyesatkan, sebenarnya berarti "tanpa filter
	 * fakultas", bukan terkait {@link PerguruanTinggi}), dan scoping {@link #perguruanTinggi}
	 * bila diset. Dibatasi {@link Common#MAX_RESULT_50} baris. Mengisi ulang grid dengan hasilnya
	 * beserta {@link DosenRenderer}.
	 *
	 * @param event tidak dipakai, hanya mengikuti signature standar listener pencarian
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(Dosen.class)
				.createAlias("statusPegawai", "statusPegawai", Criteria.LEFT_JOIN)
				.add(Restrictions.or(Restrictions.ilike("statusPegawai.nama", "aktif", MatchMode.START),
						Restrictions.isNull("statusPegawai.nama")))
				.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));
		if (hanyaDosenTetap) {
			criteria.createAlias("ikatanKerjaDosen", "ikatanKerjaDosen", Criteria.INNER_JOIN)
					.add(Restrictions.eq("ikatanKerjaDosen.tetap", true));
		}

		criteria.addOrder(Order.asc("nama")).add(

				nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1") :

						Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))

				.add(kode.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1") :

						Restrictions.or(Restrictions.ilike("mycode", kode.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.or(Restrictions.ilike("nidn", kode.getValue().trim(), MatchMode.ANYWHERE),
										Restrictions.ilike("code", kode.getValue().trim(), MatchMode.ANYWHERE))))

				.add(Restrictions.or(
						searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
								|| searchjurusan.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false),

						milikUniversitas.isChecked() ? Restrictions.eq("milikUniversitas", true)
								: Restrictions.sqlRestriction("false")))

				.add(tanpaLihatPt ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(
								milikUniversitas.isChecked() ? Restrictions.eq("milikUniversitas", true)
										: Restrictions.sqlRestriction("false"),
								searchfakultas.getSelectedItem() == null
										|| searchfakultas.getSelectedItem().getValue() == null
										|| searchfakultas.getSelectedItem().getValue() == null
												? Restrictions.sqlRestriction("1=1")
												: CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false)));

		if (perguruanTinggi != null) {
			criteria.add(Restrictions.or(Restrictions.isNull("perguruanTinggi"),
					Restrictions.eq("perguruanTinggi", perguruanTinggi)));
		}

		List<Dosen> dosen = criteria.setMaxResults(Common.MAX_RESULT_50).list();

		ListModel strset = new SimpleListModel(dosen);
		grid.setRowRenderer(new DosenRenderer());
		grid.setModelCheckMobile(strset);

	}

	/** @param eventListener dipanggil setiap kali user memilih satu dosen */
	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	/** @return listener pemilihan dosen yang sedang terpasang, boleh {@code null} */
	public EventListener getEventListener() {
		return eventListener;
	}

	/** @return {@code true} bila pencarian dibatasi hanya ke dosen dengan ikatan kerja tetap */
	public boolean isHanyaDosenTetap() {
		return hanyaDosenTetap;
	}

	/**
	 * Mengaktifkan/menonaktifkan pembatasan hasil pencarian hanya ke dosen dengan
	 * {@code ikatanKerjaDosen.tetap=true}. Tidak memuat ulang grid secara otomatis — panggil
	 * {@link #onSearchDefault(Event)} setelahnya bila popup sudah terbuka.
	 *
	 * @param hanyaDosenTetap {@code true} untuk membatasi hanya ke dosen tetap
	 */
	public void setHanyaDosenTetap(boolean hanyaDosenTetap) {
		this.hanyaDosenTetap = hanyaDosenTetap;
	}
}
