package ais.action.master.inventory.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import ais.ui.util.MyCaptionStyled;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Button;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.generic.AmbilDataTbmuserBanyak;
import ais.action.servlet.api.KantinHelper;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.inventory.Pedagang;
import ais.database.model.inventory.Toko;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Controller/action ZK untuk pedagang. Tipe ini merupakan titik masuk UI yang menghubungkan event
 * layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyDetail}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Toko toko}, {@code MyGrid grid}, {@code
 * boolean edit}, {@code boolean add}, {@code boolean delete}; pembacaan/pencarian ({@code loadData()}, {@code
 * getAksesPedagang()}, {@code tampilkanPilihanMultiToko()}, {@code cariPedagangToko()}); validasi/perhitungan
 * ({@code bolehTambahPenggunaBaru()}); mutasi data ({@code setAksesPedagang()}); operasi domain lain ({@code
 * display()}, {@code daftarAksesPedagang()}, {@code ringkasanAksesPedagang()}, {@code salinAkunPedagang()},
 * {@code buildInfoHtmlInventoryV1()}, {@code escapeHtmlInventoryV1()}); konfigurasi constructor: {@code add},
 * {@code delete}, {@code edit}. Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut
 * di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see MyDetail
 */
public class PedagangAction extends MyDetail {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private Toko toko;
	private MyGrid grid;

	private boolean edit = false;
	private boolean add = false;
	private boolean delete = false;

	public PedagangAction(Toko toko) {
		super();
		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		this.toko = toko;
		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(PedagangAction.this);
				if (isOpen()) {
					display();
				}
			}
		});
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link PedagangAction}. Kelas ini menerjemahkan satu item data menjadi
	 * baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link PedagangAction} dan dapat mengakses state
	 * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see PedagangAction
	 */
	class PedagangRenderer extends ais.ui.util.MyRowRenderer {

		public PedagangRenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final Pedagang pedagang = (Pedagang) data;

			CommonMedia.tampilkanGambarKecil(pedagang.getTbmuser()).setParent(row);

			// Gap-closure "akun pedagang MANDIRI (userid+pass+nama, TANPA Tbmuser -- dibuat lewat
			// tombol 'Tambah Pengguna Baru' baru di sini ATAU sudah ada sebelumnya lewat Konfigurasi
			// POS Desktop/Android) tampil KOSONG di grid ini" -- SEBELUMNYA kolom ini HANYA membaca
			// pedagang.getTbmuser(), jadi baris tanpa Tbmuser (login POS-only) selalu tampil blank
			// ID/Nama walau datanya ADA di kolom Pedagang.userid/Pedagang.nama sendiri.
			new Label(pedagang.getTbmuser() != null ? pedagang.getTbmuser().getUserId()
					: (pedagang.getUserid() == null ? "" : pedagang.getUserid())).setParent(row);

			RevisiHelper
					.createNewRevisi(Pedagang.class, pedagang,
							pedagang.getTbmuser() != null ? pedagang.getTbmuser().getUserNama()
									: (pedagang.getNama() == null ? "" : pedagang.getNama()))
					.setParent(row);

			Label lblKeterangan = new Label(pedagang.getKeterangan() == null ? "" : pedagang.getKeterangan());
			lblKeterangan.setParent(row);

			new Label(ringkasanAksesPedagang(pedagang)).setParent(row);

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig buttonEdit = new MyToolbarbuttonConfig("", "/img/svg/edit.svg");
			buttonEdit.setVisible(edit);
			buttonEdit.setTooltiptext("Edit hak akses pedagang");
			buttonEdit.addEventListener(Events.ON_CLICK, new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					bukaDialogEditPedagang(pedagang);
				}
			});
			buttonEdit.setParent(toolbar);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setVisible(delete);
			button.setTooltiptext("Hapus Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

						@Override
						public void onEvent(Event event) throws Exception {
							int i = Integer.parseInt(event.getData().toString());
							if (i == MyMessageboxConfig.OK) {
								try {

									Common.refreshDelete(pedagang);

									loadData(null);

								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e); 
									MyMessageboxConfig
											.show("Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
													+ e.getMessage());
								}

							}

						}
					});

				}

			});
			button.setParent(toolbar);
			toolbar.setParent(row);
		}
	}

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Session session = HibernateUtil.currentSession();
		List<Pedagang> pedagangs = session.createCriteria(Pedagang.class).addOrder(Order.desc("id"))
				.add(Restrictions.eq("toko", toko)).list();

		ListModel strset = new SimpleListModel(pedagangs);
		grid.setRowRenderer(new PedagangRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void display() {

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(this);
		groupbox.appendChild(new MyCaptionStyled("Daftar Pedagang " + toko.getNama()));
		// FIX layout: sebelumnya blok info ini dipanggil per-BARIS di dalam kolom aksi grid (lebar 5%),
		// membuat teksnya terpotong satu kata per baris -- dipindah ke sini supaya tampil SEKALI sbg
		// banner lebar-penuh, konsisten dgn pola kartu info di layar Kantin lain.
		buildInfoHtmlInventoryV1("Pedagang Toko", "Daftar ini menunjukkan pengguna yang diberi akses sebagai pedagang pada toko tertentu. Pengaturan yang benar membantu transaksi POS tercatat atas toko dan petugas yang sesuai.").setParent(groupbox);
		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Pengguna", "/img/add_item.png");
		button.setDisabled(!add);
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				Session session = HibernateUtil.currentSession();

				List<Tbmuser> tbmusers = session.createCriteria(Pedagang.class)
						.setProjection(Projections.groupProperty("tbmuser")).add(Restrictions.isNotNull("tbmuser"))
						.add(Restrictions.eq("toko", toko)).list();

				AmbilDataTbmuserBanyak ambilDataTbmuserBanyak = new AmbilDataTbmuserBanyak(tbmusers);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataTbmuserBanyak);
				ambilDataTbmuserBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<Tbmuser> tbmusers = (List<Tbmuser>) arg0.getData();

						Session session = HibernateUtil.currentSession();
						for (Tbmuser tbmuser : tbmusers) {
							Pedagang pedagang = new Pedagang();
							pedagang.setTbmuser(tbmuser);
							pedagang.setKeterangan("");
							pedagang.setToko(toko);
							session.save(pedagang);
						}

						loadData(null);
					}
				});
				ambilDataTbmuserBanyak.setWidth("850px");
				ambilDataTbmuserBanyak.setHeight("97%");
				ambilDataTbmuserBanyak.setVisible(true);
				ambilDataTbmuserBanyak.onModal();
			}

		});
		button.setParent(toolbar);

		// Gap-closure "supervisor toko boleh menambah pengguna Kantin baru (Supervisor lain ATAU
		// Kasir biasa) langsung dari layar ini" -- lihat JavaDoc bukaDialogTambahPenggunaBaru()/
		// bolehTambahPenggunaBaru() di bawah utk detail lengkap gerbang & alasan desainnya.
		MyToolbarbuttonConfig btnTambahBaru = new MyToolbarbuttonConfig("Tambah Pengguna Baru", "/img/add_item.png");
		btnTambahBaru.setDisabled(!(add && bolehTambahPenggunaBaru()));
		btnTambahBaru.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				bukaDialogTambahPenggunaBaru();
			}
		});
		btnTambahBaru.setParent(toolbar);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);grid.getPagingChild().setMold("os");
		grid.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Foto");
		column.setWidth("70px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Id Pengguna");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama Pengguna");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Supervisor");
		column.setWidth("120px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth(ais.ui.util.GridKolomHelper.LEBAR_KOLOM_AKSI);
		column.setAlign("center");

		loadData(null);
	}

	private String[][] daftarAksesPedagang() {
		return new String[][] {
				{ "supervisor", "Supervisor" } };
	}

	private boolean getAksesPedagang(Pedagang pedagang, String field) {
		if ("supervisor".equals(field)) return Boolean.TRUE.equals(pedagang.getSupervisor());
		return true;
	}

	private String ringkasanAksesPedagang(Pedagang pedagang) {
		return Boolean.TRUE.equals(pedagang.getSupervisor()) ? "Ya" : "Tidak";
	}

	private void setAksesPedagang(Pedagang pedagang, String field, boolean checked) {
		if ("supervisor".equals(field)) pedagang.setSupervisor(Boolean.valueOf(checked));
	}

	@SuppressWarnings("unchecked")
	private void tampilkanPilihanMultiToko(Vbox parent, final Pedagang pedagangInduk) {
		Session session = HibernateUtil.currentSession();
		List<Toko> tokos = session.createCriteria(Toko.class)
				.add(Restrictions.or(Restrictions.eq("aktif", Boolean.TRUE), Restrictions.isNull("aktif")))
				.addOrder(Order.asc("nama")).list();
		for (final Toko tokoPilihan : tokos) {
			if (tokoPilihan.getId() != null && pedagangInduk.getToko() != null
					&& tokoPilihan.getId().equals(pedagangInduk.getToko().getId())) {
				continue;
			}

			Hbox hbox = new Hbox();
			hbox.setSpacing("6px");
			hbox.setAlign("center");
			hbox.setParent(parent);

			final ais.ui.util.MyCheckboxConfig checkbox = new ais.ui.util.MyCheckboxConfig("");
			checkbox.setChecked(cariPedagangToko(session, pedagangInduk, tokoPilihan) != null);
			checkbox.setParent(hbox);
			new Label(tokoPilihan.getNama() == null ? ("Toko " + tokoPilihan.getId()) : tokoPilihan.getNama()).setParent(hbox);

			checkbox.addEventListener(Events.ON_CHECK, new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					Session session = HibernateUtil.currentSession();
					Pedagang pedagangToko = cariPedagangToko(session, pedagangInduk, tokoPilihan);
					if (checkbox.isChecked()) {
						if (pedagangToko == null) {
							Pedagang baru = new Pedagang();
							salinAkunPedagang(pedagangInduk, baru);
							baru.setToko(tokoPilihan);
							session.save(baru);
						}
					} else if (pedagangToko != null) {
						Common.refreshDelete(pedagangToko);
					}
				}
			});
		}
	}

	private Pedagang cariPedagangToko(Session session, Pedagang sumber, Toko tokoTarget) {
		org.hibernate.Criteria criteria = session.createCriteria(Pedagang.class)
				.add(Restrictions.eq("toko", tokoTarget));
		if (sumber.getTbmuser() != null) {
			criteria.add(Restrictions.eq("tbmuser", sumber.getTbmuser()));
		} else {
			criteria.add(Restrictions.eq("userid", sumber.getUserid()));
			criteria.add(Restrictions.isNull("tbmuser"));
		}
		criteria.setMaxResults(1);
		return (Pedagang) criteria.uniqueResult();
	}

	private void salinAkunPedagang(Pedagang sumber, Pedagang tujuan) {
		tujuan.setTbmuser(sumber.getTbmuser());
		tujuan.setUserid(sumber.getUserid());
		tujuan.setPass(sumber.getPass());
		tujuan.setNama(sumber.getNama());
		tujuan.setAktif(sumber.getAktif());
		tujuan.setKeterangan(sumber.getKeterangan());
		String[][] akses = daftarAksesPedagang();
		for (int i = 0; i < akses.length; i++) {
			setAksesPedagang(tujuan, akses[i][0], getAksesPedagang(sumber, akses[i][0]));
		}
	}

	private org.zkoss.zul.Html buildInfoHtmlInventoryV1(String judul, String deskripsi) {
		return new org.zkoss.zul.Html("<div style=\"padding:10px 12px;margin:4px 0;border-radius:12px;"
				+ "background:#f8fafc;border:1px solid #e2e8f0;color:#475569;font-size:11.5px;line-height:1.55;\">"
				+ "<b style=\"color:#0f172a;\">" + escapeHtmlInventoryV1(judul) + "</b><br/>"
				+ escapeHtmlInventoryV1(deskripsi) + "</div>");
	}

	private String escapeHtmlInventoryV1(String value) {
		if (value == null) {
			return "";
		}
		String s = value;
		s = s.replace("&", "&amp;");
		s = s.replace("<", "&lt;");
		s = s.replace(">", "&gt;");
		s = s.replace("\"", "&quot;");
		s = s.replace("'", "&#39;");
		return s;
	}

	/**
	 * Gerbang tombol "Tambah Pengguna Baru" -- HANYA admin global (tanpa Pedagang sama sekali) ATAU
	 * supervisor toko INI SENDIRI ({@code toko} field kelas ini, dari constructor) yang boleh
	 * menambah pengguna baru di sini. Supervisor toko LAIN (atau kasir non-supervisor) tetap
	 * ditolak -- konsisten dgn IDOR-safe scoping yg sudah dipakai {@code KantinHelper.tambahAkunKasir}
	 * di sisi POS API, walau layar ZK ini sendiri (menu privilege biasa, lihat {@code
	 * CommonPrivilages.checkPrevilages}) TIDAK toko-scoped -- gerbang method ini yg menutup celahnya
	 * utk tombol SPESIFIK ini (bukan seluruh layar, yg di luar cakupan perbaikan ini).
	 */
	private boolean bolehTambahPenggunaBaru() {
		try {
			Tbmuser current = Common.getCurrentUser();
			if (current == null) return false;
			Pedagang pedagangSaya = current.getPedagang();
			if (pedagangSaya == null) return true; // admin global (bukan pedagang toko mana pun)
			return Boolean.TRUE.equals(pedagangSaya.getSupervisor()) && pedagangSaya.getToko() != null
					&& toko.getId().equals(pedagangSaya.getToko().getId());
		} catch (Exception e) {
			return false;
		}
	}

	private void bukaDialogEditPedagang(final Pedagang pedagang) throws InterruptedException {
		final Window win = new Window();
		win.setTitle("Edit Hak Akses Pedagang");
		win.setBorder("normal");
		win.setWidth("640px");
		win.setHeight("86%");
		win.setClosable(true);
		win.setPosition("center");
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(win);

		Borderlayout layout = new Borderlayout();
		layout.setWidth("100%");
		layout.setHeight("100%");
		layout.setParent(win);

		Center center = new Center();
		center.setBorder("none");
		center.setAutoscroll(true);
		center.setParent(layout);

		Vbox vbox = new Vbox();
		vbox.setSpacing("8px");
		vbox.setWidth("100%");
		vbox.setStyle("padding:14px 18px 18px 14px;box-sizing:border-box;");
		vbox.setParent(center);

		String nama = pedagang.getTbmuser() != null ? pedagang.getTbmuser().getUserNama()
				: (pedagang.getNama() == null ? "" : pedagang.getNama());
		new Label("Pengguna: " + nama).setParent(vbox);

		new Label("Keterangan").setParent(vbox);
		final Textbox txtKeterangan = new Textbox(pedagang.getKeterangan() == null ? "" : pedagang.getKeterangan());
		txtKeterangan.setWidth("96%");
		txtKeterangan.setParent(vbox);

		buildInfoHtmlInventoryV1("Hak Akses Menu",
				"Hak akses menu POS/JSP dan pilihan toko sekarang diatur dari Grup Pengguna. Di form pedagang ini hanya tersisa penanda Supervisor.")
						.setParent(vbox);

		Hbox hboxSupervisor = new Hbox();
		hboxSupervisor.setSpacing("6px");
		hboxSupervisor.setAlign("center");
		hboxSupervisor.setParent(vbox);
		final ais.ui.util.MyCheckboxConfig chkSupervisor = new ais.ui.util.MyCheckboxConfig("");
		chkSupervisor.setChecked(Boolean.TRUE.equals(pedagang.getSupervisor()));
		chkSupervisor.setParent(hboxSupervisor);
		new Label("Supervisor").setParent(hboxSupervisor);

		South south = new South();
		south.setBorder("none");
		south.setSize("48px");
		south.setParent(layout);

		Toolbar toolbarTombol = new Toolbar();
		toolbarTombol.setStyle("height:48px;padding:8px 12px;text-align:right;background:#f8fafc;border-top:1px solid #e2e8f0;");
		toolbarTombol.setParent(south);

		Hbox hboxTombol = new Hbox();
		hboxTombol.setSpacing("8px");
		hboxTombol.setPack("end");
		hboxTombol.setWidth("100%");
		hboxTombol.setParent(toolbarTombol);

		Button btnBatal = new Button("Batal");
		btnBatal.setParent(hboxTombol);
		btnBatal.addEventListener(Events.ON_CLICK, new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				win.detach();
			}
		});

		Button btnSimpan = new Button("Simpan");
		btnSimpan.setParent(hboxTombol);
		btnSimpan.addEventListener(Events.ON_CLICK, new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Session session = HibernateUtil.currentSession();
				pedagang.setKeterangan(txtKeterangan.getValue() == null ? "" : txtKeterangan.getValue());
				pedagang.setSupervisor(Boolean.valueOf(chkSupervisor.isChecked()));
				Common.refreshUpdate(session, pedagang);
				win.detach();
				loadData(null);
			}
		});

		win.doModal();
	}

	/**
	 * Dialog kecil "Tambah Pengguna Baru" -- membuat akun {@link Pedagang} MANDIRI (userid+pass+nama,
	 * TANPA baris {@code Tbmuser}, login POS-only) utk toko ini, SAMA PERSIS spt yg dibuat lewat menu
	 * Konfigurasi POS Desktop/Android. SENGAJA memanggil {@link KantinHelper#tambahAkunKasir} LANGSUNG
	 * (bukan menulis ulang validasinya di sini) supaya gerbang otorisasi (supervisor terkunci ke
	 * tokonya sendiri, cek duplikat userid, panjang minimum sandi) benar-benar SATU implementasi yang
	 * dipakai bersama ZK dan POS API, bukan 2 salinan yang bisa perlahan berbeda seiring waktu.
	 */
	private void bukaDialogTambahPenggunaBaru() throws InterruptedException {
		final Window win = new Window();
		win.setTitle("Tambah Pengguna Baru -- " + toko.getNama());
		win.setBorder("normal");
		win.setWidth("380px");
		win.setClosable(true);
		win.setPosition("center");
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(win);

		Vbox vbox = new Vbox();
		vbox.setSpacing("8px");
		vbox.setStyle("padding:14px;width:100%;");
		vbox.setParent(win);

		new Label("Userid").setParent(vbox);
		final Textbox txtUserid = new Textbox();
		txtUserid.setWidth("100%");
		txtUserid.setParent(vbox);

		new Label("Nama").setParent(vbox);
		final Textbox txtNama = new Textbox();
		txtNama.setWidth("100%");
		txtNama.setParent(vbox);

		new Label("Kata Sandi (min. 6 karakter)").setParent(vbox);
		final Textbox txtPassword = new Textbox();
		txtPassword.setType("password");
		txtPassword.setWidth("100%");
		txtPassword.setParent(vbox);

		Hbox hboxSupervisor = new Hbox();
		hboxSupervisor.setSpacing("6px");
		hboxSupervisor.setAlign("center");
		hboxSupervisor.setParent(vbox);
		final ais.ui.util.MyCheckboxConfig chkSupervisor = new ais.ui.util.MyCheckboxConfig("");
		chkSupervisor.setParent(hboxSupervisor);
		new Label("Jadikan Supervisor toko ini").setParent(hboxSupervisor);

		final Label lblError = new Label("");
		lblError.setStyle("color:#dc2626;font-size:11.5px;");
		lblError.setParent(vbox);

		Hbox hboxTombol = new Hbox();
		hboxTombol.setSpacing("8px");
		hboxTombol.setPack("end");
		hboxTombol.setStyle("width:100%;margin-top:6px;");
		hboxTombol.setParent(vbox);

		Button btnBatal = new Button("Batal");
		btnBatal.setParent(hboxTombol);
		btnBatal.addEventListener(Events.ON_CLICK, new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				win.detach();
			}
		});

		Button btnSimpan = new Button("Simpan");
		btnSimpan.setParent(hboxTombol);
		btnSimpan.addEventListener(Events.ON_CLICK, new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				try {
					JSONObject request = new JSONObject();
					request.put("userid", txtUserid.getValue() == null ? "" : txtUserid.getValue().trim());
					request.put("password", txtPassword.getValue() == null ? "" : txtPassword.getValue());
					request.put("nama", txtNama.getValue() == null ? "" : txtNama.getValue().trim());
					request.put("keterangan", "");
					request.put("supervisor", chkSupervisor.isChecked());
					// Dikirim SELALU (bukan hanya utk admin) -- tambahAkunKasir sendiri yg mengabaikan
					// nilai ini utk pemanggil supervisor (dikunci ke tokonya sendiri, IDOR-safe di sisi
					// server), jadi aman dikirim apa adanya di sini utk kedua jenis pemanggil.
					request.put("toko_id", toko.getId());

					JSONObject hasil = new JSONObject();
					KantinHelper.tambahAkunKasir(Common.getCurrentUser(), request, hasil);
					if ("00".equals(hasil.optString("status"))) {
						win.detach();
						loadData(null);
						MyMessageboxConfig.show("Pengguna baru berhasil ditambahkan.");
					} else {
						lblError.setValue(hasil.optString("description", "Gagal menambahkan pengguna."));
					}
				} catch (Exception e) {
					lblError.setValue("Gagal menambahkan pengguna: " + e.getMessage());
				}
			}
		});

		win.doModal();
	}

}
