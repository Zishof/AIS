package ais.action.master.helper;

import java.util.List;

import org.hibernate.Criteria;
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
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.kkn.KelompokKknAction;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.MahasiswaDapatKelompokKkn;
import ais.database.model.Tbmuser;
import ais.database.model.kkn.KelompokKkn;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Implementasi pola "Bandbox picker" AIS untuk entity
 * {@link ais.database.model.kkn.KelompokKkn} — lihat {@link ais.ui.util.GetEventListener} untuk
 * arsitektur kerangka umum (constructor/display/onSearchDefault/renderer/callback).
 * <p>
 * {@code KelompokKkn} adalah kelompok mahasiswa untuk program KKN (Kuliah Kerja Nyata), masing-
 * masing dinaungi satu periode/kegiatan {@code Kkn} (diakses lewat alias {@code kkn}) yang boleh
 * dibatasi fakultas/jurusan tertentu. Popup pencarian menyediakan field {@code nama} dan
 * {@code alamat} (keduanya ilike substring). BERBEDA dari kebanyakan subclass sejenis, hasil
 * SELALU di-scope otomatis (tanpa kontrol pengguna) ke fakultas/jurusan mahasiswa/pengguna yang
 * sedang login ({@link ais.common.Common#getCurrentUser()} — lewat {@code Tbmuser.getMahasiswa()}
 * bila pengguna adalah mahasiswa, atau {@code Tbmuser.ambilFakultas()/ambilJurusan()} untuk
 * pengguna staf), digabung {@code Restrictions.or(eq(...), isNull(...))} sehingga kelompok KKN
 * lintas-fakultas/jurusan tetap ikut tampil; hanya kelompok dengan {@code mahasiswaBisaMemilih ==
 * true} yang muncul. Renderer juga menghitung jumlah anggota terdaftar
 * ({@link ais.database.model.MahasiswaDapatKelompokKkn}) dan menonaktifkan radio button bila
 * kuota kelompok sudah penuh — mencegah mahasiswa memilih kelompok yang sudah tidak menerima
 * anggota baru. Pemilihan bersifat TUNGGAL (Radio biasa dalam Radiogroup, bukan
 * {@code MyRadioConfig}). Constructor tanpa argumen mendelegasikan ke constructor boolean (nilai
 * argumennya {@code notDeafault} sebenarnya tidak dipakai — kedua constructor berperilaku sama).
 * </p>
 *
 * @see Bandbox
 */
public class AmbilDataKelompokKknBanbox extends Bandbox implements GetEventListener {

	/**
	 *
	 */
	private static final long serialVersionUID = 6452451056684904810L;
	private MyGrid grid;

	private EventListener eventListener;

	// private Mahasiswa mahasiswa = null;

	/**
	 * Konstruktor default, mendelegasikan ke {@link #AmbilDataKelompokKknBanbox(Boolean)}.
	 * Parameter delegasi tidak memengaruhi perilaku (lihat catatan di Javadoc class).
	 */
	public AmbilDataKelompokKknBanbox() {
		this(true);
	}

	/**
	 * Konstruktor standar: memasang listener {@code onOpen} yang membangun popup pencarian secara
	 * lazy pada pembukaan pertama. Mengikuti kerangka standar di
	 * {@link ais.ui.util.GetEventListener}. Parameter {@code notDeafault} (typo historis pada nama
	 * parameter, dipertahankan apa adanya) TIDAK dipakai di badan constructor.
	 *
	 * @param notDeafault tidak dipakai; ada untuk membedakan signature dari constructor default
	 */
	public AmbilDataKelompokKknBanbox(Boolean notDeafault) {
		super();
		setReadonly(true);

		// mahasiswa = Common.getCurrentUser().getMahasiswa();

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

	/** Kriteria pencarian: alamat lokasi kelompok KKN (ilike, substring). */
	private Textbox alamat;
	/** Kriteria pencarian: nama kelompok KKN (ilike, substring). */
	private Textbox nama;

	/**
	 * Renderer baris grid hasil pencarian {@link KelompokKkn}: menampilkan info dosen pembimbing
	 * lewat {@link KelompokKknAction#tampilkanInfoDosen}, alamat, dan rasio kuota terisi
	 * ({@code kuota / jumlahAnggotaTerdaftar}) dihitung langsung dari
	 * {@link ais.database.model.MahasiswaDapatKelompokKkn}. KHUSUS renderer ini: radio button
	 * pilihan dinonaktifkan ({@code setDisabled}) bila jumlah anggota terdaftar sudah mencapai
	 * kuota, mencegah mahasiswa memilih kelompok yang penuh. Selebihnya mengikuti kerangka
	 * renderer standar di {@link ais.ui.util.GetEventListener} — listener {@code onCheck} menutup
	 * popup, menyimpan entity terpilih ke atribut {@code "kelompokKkn"}/{@code "myValue"} dan teks
	 * tampilan {@code kelompokKkn.getNama()}, lalu meneruskan event ke {@link #eventListener} bila
	 * terpasang.
	 *
	 * @see AmbilDataKelompokKknBanbox
	 */
	class KelompokKknRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final KelompokKkn kelompokKkn = (KelompokKkn) arg1;
			Radio checkbox = new Radio(kelompokKkn.getNama());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);

			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataKelompokKknBanbox.this.setOpen(false);
					AmbilDataKelompokKknBanbox.this.setAttribute("kelompokKkn", kelompokKkn);
					AmbilDataKelompokKknBanbox.this.setAttribute("myValue", kelompokKkn);
					AmbilDataKelompokKknBanbox.this.setValue(kelompokKkn.getNama());
					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			KelompokKknAction.tampilkanInfoDosen(kelompokKkn, false, true).setParent(arg0);

			new Label(kelompokKkn.getAlamat()).setParent(arg0);

			int count = ((Number) HibernateUtil.currentSession().createCriteria(MahasiswaDapatKelompokKkn.class)
					.add(Restrictions.eq("kelompokKkn", kelompokKkn)).setProjection(Projections.rowCount())
					.uniqueResult()).intValue();

			new Label(kelompokKkn.getKuota() + " / " + count).setParent(arg0);

			checkbox.setDisabled(count >= kelompokKkn.getKuota());
		}

	}

	/**
	 * Membangun popup pencarian {@link KelompokKkn} sekali (dipanggil lazy dari listener
	 * {@code onOpen}): form dengan field nama dan alamat, tombol Cari, dan grid hasil dibungkus
	 * {@link org.zkoss.zul.Radiogroup} (pilih tunggal). Mengikuti kerangka {@code display()}
	 * standar — lihat {@link ais.ui.util.GetEventListener}. Memanggil
	 * {@link #onSearchDefault(Event)} di akhir agar grid terisi saat popup pertama dibuka.
	 */
	public void display() {

		setReadonly(true);

		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("750px");
		bandpopup.setHeight("600px");

		final Radiogroup radiogroup = new Radiogroup();
		radiogroup.setWidth("100%");
		radiogroup.setHeight("100%");
		radiogroup.setParent(bandpopup);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(radiogroup);
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

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Alamat"));
		row.appendChild(alamat = new Textbox());
		alamat.setWidth("90%");

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
		grid.setMold("paging");
		grid.setPageSize(10);grid.getPagingChild().setMold("os");
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
		column.setLabel("Nama");
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Pembimbing");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("30%");
		column.setLabel("Alamat");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kuota");
		column.setWidth("10%");

		onSearchDefault(null);

	}

	/**
	 * Mengeksekusi pencarian {@link KelompokKkn}: menentukan dulu fakultas/jurusan pengguna login
	 * (mahasiswa atau staf), lalu memfilter kelompok yang periode KKN-nya cocok dengan
	 * fakultas/jurusan tersebut (digabung {@code or(eq, isNull)} agar kelompok lintas-fakultas/
	 * jurusan tetap tampil) dan {@code mahasiswaBisaMemilih == true}, ditambah filter {@code nama}
	 * dan {@code alamat} (ilike substring). Diurutkan menaik berdasar nama, dibatasi
	 * {@link ais.common.Common#MAX_RESULT}, lalu memasang {@link KelompokKknRenderer} dan model
	 * hasil ke {@link #grid}. Mengikuti kerangka {@code onSearchDefault} standar — lihat
	 * {@link ais.ui.util.GetEventListener}.
	 *
	 * @param event event pemicu (klik tombol Cari); boleh {@code null} saat dipanggil dari
	 *              {@link #display()} untuk mengisi grid pertama kali
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Tbmuser tbmuser = Common.getCurrentUser();
		Fakultas fakultas = null;
		Jurusan jurusan = null;
		if (tbmuser != null && tbmuser.getMahasiswa() != null) {
			fakultas = tbmuser.getMahasiswa().getJurusan().getFakultas();
			jurusan = tbmuser.getMahasiswa().getJurusan();
		} else if (tbmuser != null) {
			fakultas = tbmuser.ambilFakultas();
			jurusan = tbmuser.ambilJurusan();
		}

		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(KelompokKkn.class).createAlias("kkn", "kkn")
				
				.add(fakultas == null ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.eq("kkn.fakultas", fakultas),
								Restrictions.isNull("kkn.fakultas")))
				.add(jurusan == null ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.eq("kkn.jurusan", jurusan),
								Restrictions.isNull("kkn.jurusan")))
				
				.add(Restrictions.eq("mahasiswaBisaMemilih", true));

		criteria.addOrder(Order.asc("nama"))
				.add(nama.getText().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", nama.getText().trim(), MatchMode.ANYWHERE))
				.add(alamat.getText().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("alamat", alamat.getText().trim(), MatchMode.ANYWHERE));

		List<KelompokKkn> kelompokKkn = criteria.setMaxResults(Common.MAX_RESULT).list();

		ListModel strset = new SimpleListModel(kelompokKkn);
		grid.setRowRenderer(new KelompokKknRenderer());
		grid.setModelCheckMobile(strset);

	}

	/** {@inheritDoc} Implementasi setter polos standar — lihat {@link ais.ui.util.GetEventListener}. */
	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	/** {@inheritDoc} Implementasi getter polos standar — lihat {@link ais.ui.util.GetEventListener}. */
	public EventListener getEventListener() {
		return eventListener;
	}
}
