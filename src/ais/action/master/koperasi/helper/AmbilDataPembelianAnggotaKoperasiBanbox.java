package ais.action.master.koperasi.helper;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Bandbox;
import org.zkoss.zul.Bandpopup;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.koperasi.AnggotaKoperasi;
import ais.database.model.koperasi.PembelianAnggotaKoperasi;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;

/**
 * Tipe khusus untuk ambil data pembelian anggota koperasi banbox. Kelas ini memberi nama dan batas
 * tanggung jawab yang eksplisit pada perilaku yang diwarisi atau kontrak yang
 * diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Bandbox}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Grid grid}, {@code
 * ais.ui.util.AmbilDataPagingHelper pagingHelper}, {@code EventListener eventListener}, {@code MyTextbox
 * kodePembelianAnggotaKoperasian}, {@code MyTextbox mr}, {@code MyTextbox nama}; pembacaan/pencarian ({@code
 * onSearchDefault()}, {@code setEventListener()}, {@code getEventListener()}); operasi domain lain ({@code
 * display()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see Bandbox
 */
public class AmbilDataPembelianAnggotaKoperasiBanbox extends Bandbox {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private Grid grid;


	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;

	public AmbilDataPembelianAnggotaKoperasiBanbox() {
		super();
		setReadonly(true);
		this.addEventListener(Events.ON_OK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (getValue().trim().equals("")) {
					setAttribute("pembelianAnggotaKoperasi", null);
					setValue("");
					return;
				}

				PembelianAnggotaKoperasi pembelianAnggotaKoperasi = (PembelianAnggotaKoperasi) HibernateUtil
						.currentSession().createCriteria(PembelianAnggotaKoperasi.class).add(Restrictions.ilike("kode",
								AmbilDataPembelianAnggotaKoperasiBanbox.this.getValue().trim(), MatchMode.EXACT))
						.setMaxResults(1).uniqueResult();
				if (pembelianAnggotaKoperasi == null) {
					MyMessageboxConfig.showFormat(
							"Mohon maaf, data Pembelian Anggota Koperasi dengan kode \"{V1}\" tidak ditemukan. Langkah yang dapat dilakukan: (1) periksa kembali penulisan kode; (2) gunakan tombol pencarian untuk memilih data dari daftar; (3) ulangi pencarian.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION,
							AmbilDataPembelianAnggotaKoperasiBanbox.this.getValue().trim());
					return;
				}
				AmbilDataPembelianAnggotaKoperasiBanbox.this.setOpen(false);
				AmbilDataPembelianAnggotaKoperasiBanbox.this.setAttribute("pembelianAnggotaKoperasi",
						pembelianAnggotaKoperasi);
				AmbilDataPembelianAnggotaKoperasiBanbox.this.setValue(pembelianAnggotaKoperasi.getKode() + " - "
						+ (pembelianAnggotaKoperasi.getAnggotaKoperasi() == null ? ""
								: pembelianAnggotaKoperasi.getAnggotaKoperasi().getNama()));
				if (eventListener != null) {
					eventListener.onEvent(arg0);
				}
			}
		});

		display();

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

	private MyTextbox kodePembelianAnggotaKoperasian;
	private MyTextbox mr;
	private MyTextbox nama;

	/**
	 * Renderer lokal untuk layar/komponen {@link AmbilDataPembelianAnggotaKoperasiBanbox}. Kelas ini menerjemahkan
	 * satu item data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link AmbilDataPembelianAnggotaKoperasiBanbox} dan
	 * dapat mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see AmbilDataPembelianAnggotaKoperasiBanbox
	 */
	class PembelianAnggotaKoperasiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final PembelianAnggotaKoperasi pembelianAnggotaKoperasi = (PembelianAnggotaKoperasi) arg1;

			arg0.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataPembelianAnggotaKoperasiBanbox.this.setOpen(false);
					AmbilDataPembelianAnggotaKoperasiBanbox.this.setAttribute("pembelianAnggotaKoperasi",
							pembelianAnggotaKoperasi);
					AmbilDataPembelianAnggotaKoperasiBanbox.this.setValue(pembelianAnggotaKoperasi.getKode() + " - "
							+ (pembelianAnggotaKoperasi.getAnggotaKoperasi() == null ? ""
									: pembelianAnggotaKoperasi.getAnggotaKoperasi().getNama()));
					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			final AnggotaKoperasi anggotaKoperasi = pembelianAnggotaKoperasi.getAnggotaKoperasi();

			new Label(pembelianAnggotaKoperasi.getKode()).setParent(arg0);
			new Label(anggotaKoperasi == null ? "Bukan Anggota" : anggotaKoperasi.getKode()).setParent(arg0);
			new Label(anggotaKoperasi == null ? "Bukan Anggota" : anggotaKoperasi.getNama()).setParent(arg0);

			new Label(anggotaKoperasi.getAlamat()).setParent(arg0);
			new Label(pembelianAnggotaKoperasi.getTanggalPembayaran() == null ? ""
					: Common.dateFormat3.get().format(pembelianAnggotaKoperasi.getTanggalPembayaran())).setParent(arg0);

		}

	}

	public void display() {
		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("95%");
		bandpopup.setHeight("600px");

		Panel panel = new Panel();
		panel.setParent(bandpopup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Pembelian Anggota Koperasi");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new Borderlayout();
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

		Grid searchgrid = new Grid();
		searchgrid.setParent(rowUtama);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		Row row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("No. Pemb.")));
		row.appendChild(kodePembelianAnggotaKoperasian = new MyTextbox());
		kodePembelianAnggotaKoperasian.setWidth("90%");
		kodePembelianAnggotaKoperasian.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode Anggota")));
		row.appendChild(mr = new MyTextbox());
		mr.setWidth("90%");
		mr.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama")));
		row.appendChild(nama = new MyTextbox());
		nama.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("25px");
		Row rowKedua = new Row();
		rowKedua.setParent(rowsUtama);
		toolbar.setHeight("32px");
		toolbar.setParent(rowKedua);

		Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("Cari", "/img/search.gif");
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
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/koperasi/helper/AmbilDataPembelianAnggotaKoperasiBanbox.java:237");
					}
				}
				onSearchDefault(event);
			}
		}));

		grid = new Grid();
		/* Paging server-side (AmbilDataPagingHelper) menggantikan mold "paging"
		 * client-side yang dibatasi MAX_RESULT. */
		Row rowKetiga = new Row();
		rowKetiga.setParent(rowsUtama);
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.getPagingChild().setMold("os");
		grid.setParent(rowKetiga);

		Columns columns = new Columns();

		columns.setParent(grid);

		Column column = new Column();
		column.setParent(columns);
		column.setLabel("No. Pemb");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Kode Anggota");
		column.setWidth("12%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Nama Anggota");
		column.setWidth("15%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Alamat");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Wkt. Pemb.");
		column.setWidth("15%");

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

//		kodeIdentitas

		Session session = HibernateUtil.currentSession();
		List<PembelianAnggotaKoperasi> pembelianAnggotaKoperasi = session.createCriteria(PembelianAnggotaKoperasi.class)
				.createAlias("anggotaKoperasi", "anggotaKoperasi", Criteria.LEFT_JOIN)
				.add(mr.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(
								Restrictions.ilike("anggotaKoperasi.kode", mr.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("anggotaKoperasi.kodeIdentitas", mr.getValue().trim(),
										MatchMode.ANYWHERE))

				)
				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("anggotaKoperasi.nama", nama.getValue().trim(), MatchMode.ANYWHERE))
				.add(kodePembelianAnggotaKoperasian.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("kode", kodePembelianAnggotaKoperasian.getValue().trim(),
								MatchMode.ANYWHERE))

				.setMaxResults(Common.MAX_RESULT).list();

		System.out.println(pembelianAnggotaKoperasi);
		ListModel strset = new SimpleListModel(pembelianAnggotaKoperasi);
		grid.setRowRenderer(new PembelianAnggotaKoperasiRenderer());
		grid.setModel(strset);
		grid.renderAll();

	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}
