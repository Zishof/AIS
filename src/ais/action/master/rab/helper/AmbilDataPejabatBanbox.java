package ais.action.master.rab.helper;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Bandbox;
import org.zkoss.zul.Bandpopup;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
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

import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.employ.JenisJabatan;
import ais.database.model.rab.Pejabat;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Implementasi pola "Bandbox picker" AIS untuk entity {@link ais.database.model.rab.Pejabat} — lihat
 * {@link ais.ui.util.GetEventListener} untuk arsitektur kerangka umum (constructor/display/onSearchDefault/
 * renderer/callback).
 *
 * <p>Pejabat merepresentasikan pihak yang berwenang menandatangani/menyetujui dokumen pada alur RAB
 * (mis. KPA, PPK, bendahara), terikat ke {@code JenisJabatan} (jenis jabatan) dan ke salah satu dari
 * tiga entity orang: Pegawai, Dosen, atau Guru — dari sanalah kode, nama, dan foto diambil. Popup
 * pencarian hanya menyediakan satu kriteria teks: {@code nama} ({@code ilike ANYWHERE}, no-op bila
 * kosong), digabung dua filter bisnis tetap: hanya pejabat dengan {@code aktif} bernilai null/true
 * (menyembunyikan pejabat nonaktif) dan, bila instance dibangun lewat konstruktor
 * {@link #AmbilDataPejabatBanbox(JenisJabatan)}, dibatasi ke {@code jenisJabatan} tersebut saja
 * (no-op bila konstruktor lain yang dipakai). Baris grid menampilkan foto kecil, kode/NIP, nama,
 * jenis pengguna, username, dan jabatan lewat {@code PejabatRenderer}; hasil dibungkus
 * {@link org.zkoss.zul.Radiogroup} untuk pemilihan tunggal.</p>
 *
 * <p><b>Logika non-standar penting — auto-pilih dari user login:</b> SEBELUM merakit popup, kedua
 * constructor ({@code String}) dan ({@code JenisJabatan}) mengecek {@code Common.getCurrentUser()}:
 * bila user yang sedang login punya {@code hakAkses().getJenisJabatan()}, Bandbox langsung diisi
 * dengan jabatan tersebut dan di-{@code setDisabled(true)} (popup TIDAK dibangun, pengguna tidak bisa
 * memilih pejabat lain); bila tidak, dicoba {@code Common.getCurrentPejabat(true)} — bila user punya
 * data Pejabat terkait, Bandbox diisi otomatis dengan Pejabat pertama dan juga dikunci. Popup pencarian
 * (dan {@link #display()}) hanya dibangun ketika kedua pengecekan itu tidak menghasilkan nilai. Field
 * {@code pagingHelper} (paging server-side) dideklarasikan tapi {@link #display()} di file ini masih
 * memakai mold "paging" client-side dengan {@code pageSize(50)}, bukan lewat pagingHelper.</p>
 *
 * @see Bandbox
 */
public class AmbilDataPejabatBanbox extends Bandbox implements GetEventListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452451056684904810L;
	private MyGrid grid;


	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;
	// private SatuanKerjaTreeModel satuanKerjaTreeModel;
	private JenisJabatan jenisJabatan;

	/**
	 * Delegasi ke {@link #AmbilDataPejabatBanbox(String)} dengan teks tampilan awal kosong.
	 */
	public AmbilDataPejabatBanbox() {
		this("");
	}

	/**
	 * Membangun Bandbox picker Pejabat tanpa filter jenis jabatan. Sebelum merakit popup, mencoba
	 * auto-pilih dan mengunci Bandbox dari jabatan/pejabat milik user yang sedang login (lihat
	 * Javadoc kelas — bagian "auto-pilih dari user login"); popup baru dirakit lewat
	 * {@link #display()} (dibungkus try-catch beraudit) bila auto-pilih tidak menghasilkan nilai.
	 *
	 * @param value teks awal yang ditampilkan pada Bandbox sebelum auto-pilih/pemilihan pengguna
	 */
	public AmbilDataPejabatBanbox(String value) {
		super(value);
		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.hakAkses() != null && tbmuser.hakAkses().getJenisJabatan() != null) {
			setAttribute("pejabat", tbmuser.hakAkses().getJenisJabatan());
			setValue(tbmuser.hakAkses().getJenisJabatan().toString());
			setDisabled(true);
			return;
		} else {
			List<Pejabat> pejabats = Common.getCurrentPejabat(true);
			if (pejabats != null && !pejabats.isEmpty()) {
				setAttribute("pejabat", pejabats.get(0));
				setValue(pejabats.get(0).toString());
				setDisabled(true);
				return;
			}
		}
		// satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
		try {
			display();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/rab/helper/AmbilDataPejabatBanbox.java:83");
		}
	}

	/**
	 * Membangun Bandbox picker Pejabat yang dibatasi ke {@code jenisJabatan} tertentu (filter dari
	 * pemanggil, diterapkan di {@link #onSearchDefault(Event)} lewat
	 * {@code Restrictions.eq("jenisJabatan", jenisJabatan)}). Sebelum merakit popup, tetap mencoba
	 * auto-pilih dan mengunci Bandbox dari jabatan/pejabat milik user yang sedang login terlebih
	 * dahulu (lihat Javadoc kelas), TANPA mempertimbangkan parameter {@code jenisJabatan} pada langkah
	 * auto-pilih itu.
	 *
	 * @param jenisJabatan jenis jabatan yang membatasi hasil pencarian; menentukan filter di
	 *     {@link #onSearchDefault(Event)}
	 */
	public AmbilDataPejabatBanbox(JenisJabatan jenisJabatan) {
		super();
		this.jenisJabatan = jenisJabatan;

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.hakAkses() != null && tbmuser.hakAkses().getJenisJabatan() != null) {
			setAttribute("pejabat", tbmuser.hakAkses().getJenisJabatan());
			setValue(tbmuser.hakAkses().getJenisJabatan().toString());
			setDisabled(true);
			return;
		} else {

			List<Pejabat> pejabats = Common.getCurrentPejabat(true);
			if (pejabats != null && !pejabats.isEmpty()) {
				setAttribute("pejabat", pejabats.get(0));
				setValue(pejabats.get(0).toString());
				setDisabled(true);
				return;
			}
		}

		// satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
		try {
			display();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/rab/helper/AmbilDataPejabatBanbox.java:111");
		}
	}

	// private Textbox kode;
	private Textbox nama;
	// private AmbilDataSatuanKerjaBanbox satuanKerja;

	/**
	 * Renderer satu baris grid hasil pencarian Pejabat: menampilkan radio pilihan, foto kecil (diambil
	 * dari Pegawai/Dosen/Guru yang terpasang pada Pejabat, cascading ke label kosong bila tak satu pun
	 * terisi), kode/NIP, nama, jenis pengguna, username, dan jabatan. Listener {@code onCheck} pada
	 * radio adalah satu-satunya titik callback pola ini — lihat penjelasan umum di
	 * {@link ais.ui.util.GetEventListener}.
	 *
	 * @see AmbilDataPejabatBanbox
	 */
	class PejabatRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Pejabat pejabat = (Pejabat) arg1;
			MyRadioConfig checkbox = new MyRadioConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			// checkbox.setId(pejabat.getId() + "");

			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataPejabatBanbox.this.setOpen(false);
					AmbilDataPejabatBanbox.this.setAttribute("pejabat", pejabat);
					AmbilDataPejabatBanbox.this.setAttribute("myValue", pejabat);
					AmbilDataPejabatBanbox.this.setValue(pejabat.getNama());
					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			if (pejabat.getPegawai() != null) {
				CommonMedia.tampilkanGambarKecil(pejabat.getPegawai()).setParent(arg0);
			} else if (pejabat.getDosen() != null) {
				CommonMedia.tampilkanGambarKecil(pejabat.getDosen()).setParent(arg0);
			} else if (pejabat.getGuru() != null) {
				CommonMedia.tampilkanGambarKecil(pejabat.getGuru()).setParent(arg0);
			} else {
				new Label().setParent(arg0);
			}

			new Label(pejabat.getPegawai() == null
					? (pejabat.getDosen() == null ? (pejabat.getGuru() == null ? "" : pejabat.getGuru().getNama())
							: pejabat.getDosen().getCode())
					: pejabat.getPegawai().getCode()).setParent(arg0);

			new Label(pejabat.getPegawai() == null
					? (pejabat.getDosen() == null ? (pejabat.getGuru() == null ? "" : pejabat.getGuru().getNama())
							: pejabat.getDosen().getNama())
					: pejabat.getPegawai().getNama()).setParent(arg0);

			new Label(pejabat.getJenisPengguna().isEmpty() ? "Tidak ditentukan" : pejabat.getJenisPengguna())
					.setParent(arg0);
			new Label(pejabat.getUsernamePengguna().isEmpty() ? "Tidak ditentukan" : pejabat.getUsernamePengguna())
					.setParent(arg0);

			new Label(pejabat.getJenisJabatan() == null ? "" : pejabat.getJenisJabatan().getNama()).setParent(arg0);
			// new Label(pejabat.getSatuanKerja() == null ? "" : pejabat
			// .getSatuanKerja().getNama()).setParent(arg0);
		}

	}

	/**
	 * Merakit popup pencarian Pejabat (form kriteria nama, tombol Cari, grid hasil dalam
	 * {@link org.zkoss.zul.Radiogroup} pilih-tunggal) lalu memanggil {@link #onSearchDefault(Event)}
	 * agar grid terisi saat popup pertama tampil. Hanya dipanggil dari constructor bila langkah
	 * auto-pilih dari user login (lihat Javadoc kelas) tidak menghasilkan nilai.
	 *
	 * @throws Exception diteruskan dari pembangunan komponen ZK
	 * @see ais.ui.util.GetEventListener
	 */
	public void display() throws Exception {
		setReadonly(true);

		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("600px");
		bandpopup.setHeight("600px");

		final Radiogroup radiogroup = new Radiogroup();
		radiogroup.setWidth("100%");
		radiogroup.setHeight("100%");
		radiogroup.setParent(bandpopup);

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(radiogroup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Pejabat");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama = new Textbox());
		nama.setWidth("90%");

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
		column.setLabel("Foto");
		column.setWidth("80px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kode/NIP");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jenis Pengguna");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Username");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jabatan");

		// column = new MyColumnConfig();
		// column.setParent(columns);
		// column.setLabel("Satuan Kerja");

		onSearchDefault(null);

	}

	/**
	 * Menjalankan pencarian Pejabat berdasar {@code nama} ({@code ilike ANYWHERE}, no-op bila kosong),
	 * dibatasi ke {@link #jenisJabatan} bila constructor {@link #AmbilDataPejabatBanbox(JenisJabatan)}
	 * dipakai (no-op bila tidak), dan selalu menyaring pejabat yang {@code aktif} bernilai null/true
	 * (menyembunyikan pejabat nonaktif). Maksimum {@code Common.MAX_RESULT} baris, lalu grid diisi
	 * ulang dengan {@link PejabatRenderer}.
	 *
	 * @param event event pemicu; boleh {@code null} (dipanggil juga dari {@link #display()})
	 * @see ais.ui.util.GetEventListener
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(Pejabat.class);

		criteria.add(jenisJabatan == null ? Restrictions.sqlRestriction("1=1")
				: Restrictions.eq("jenisJabatan", jenisJabatan)).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

				.add(nama.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE));

		List<Pejabat> pejabat = criteria.setMaxResults(Common.MAX_RESULT).list();
		ListModel strset = new SimpleListModel(pejabat);
		grid.setRowRenderer(new PejabatRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * {@inheritDoc}
	 *
	 * @see ais.ui.util.GetEventListener
	 */
	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see ais.ui.util.GetEventListener
	 */
	public EventListener getEventListener() {
		return eventListener;
	}
}
