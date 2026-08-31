package ais.action.master.sekolah.psb.form;

import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.sekolah.helper.AmbilDataSiswaBanbox;
import ais.common.Common;
import ais.database.model.Pegawai;
import ais.database.model.employ.Keluarga;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.GelombangPendaftaranPsb;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Tipe khusus untuk ppdb. Kelas ini memberi nama dan batas tanggung jawab yang eksplisit pada
 * perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code CalonSiswa calonSiswa}, {@code
 * GelombangPendaftaranPsb gelombangPendaftaranPsb}, {@code EventListener eventListener}; inisialisasi/lifecycle
 * ({@code init()}); pembacaan/pencarian ({@code getCalonSiswa()}, {@code getGelombangPendaftaranPsb()}, {@code
 * getEventListener()}, {@code setEventListener()}); mutasi data ({@code setCalonSiswa()}, {@code
 * setGelombangPendaftaranPsb()}); operasi domain lain ({@code tandaiFormPpdb()}, {@code anakPegawai()}, {@code
 * anakPegawai()}, {@code alumni()}, {@code alumni()}, {@code sibling()}). Bagian lain dari kontrak tetap
 * mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public abstract class PPDB extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected CalonSiswa calonSiswa;
	protected GelombangPendaftaranPsb gelombangPendaftaranPsb;
	protected EventListener eventListener;

	public PPDB() {
		super();
		tandaiFormPpdb();
	}

	public PPDB(CalonSiswa calonSiswa, GelombangPendaftaranPsb gelombangPendaftaranPsb, EventListener eventListener) {
		super();
		this.calonSiswa = calonSiswa;
		this.gelombangPendaftaranPsb = gelombangPendaftaranPsb;
		this.eventListener = eventListener;
		tandaiFormPpdb();
	}

	/* Semua turunan (PPDB1/2/Alumni/Simple..6) otomatis mendapat styling
	 * modern + responsif dari css_utama.css blok "FORM PPDB MODERN". */
	private void tandaiFormPpdb() {
		try {
			String s = getSclass();
			if (s == null || s.indexOf("ppdb-form-window") < 0) {
				setSclass((s == null || s.trim().isEmpty() ? "" : s + " ") + "ppdb-form-window");
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/psb/form/PPDB.java:62");
		}
	}

	public CalonSiswa getCalonSiswa() {
		return calonSiswa;
	}

	public void setCalonSiswa(CalonSiswa calonSiswa) {
		this.calonSiswa = calonSiswa;
	}

	public GelombangPendaftaranPsb getGelombangPendaftaranPsb() {
		return gelombangPendaftaranPsb;
	}

	public void setGelombangPendaftaranPsb(GelombangPendaftaranPsb gelombangPendaftaranPsb) {
		this.gelombangPendaftaranPsb = gelombangPendaftaranPsb;
	}

	public EventListener getEventListener() {
		return eventListener;
	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public abstract void init();

	public static Combobox anakPegawai(CalonSiswa calonSiswa, GelombangPendaftaranPsb gelombangPendaftaranPsb,
			Pegawai pe, Rows rows, EventListener setelah) {
		return anakPegawai(calonSiswa, gelombangPendaftaranPsb, pe, rows, null, setelah);
	}

	public static Combobox anakPegawai(CalonSiswa calonSiswa, final GelombangPendaftaranPsb gelombangPendaftaranPsb,
			Pegawai pe, Rows rows, final Row rowData, final EventListener setelah) {

		if (calonSiswa != null && calonSiswa.getOrangTuaPegawai() != null) {
			pe = calonSiswa.getOrangTuaPegawai();
		}
		final Pegawai pegawai = pe;

		final Combobox keluarga = new Combobox();
		keluarga.setReadonly(true);
		if (gelombangPendaftaranPsb.getHanyaUntukAnakPegawai() && pegawai != null && pegawai.getId() != null) {
			Common.insertCombo(keluarga, "nama", "keteranganTambahan", Keluarga.class,
					Restrictions.and(Restrictions.eq("pegawai", pegawai), Restrictions.eq("hubungan", Keluarga.ANAK)));
			HelperFormPpdb.barisForm(rows, rowData, "Pilih anak pegawai *", keluarga);
			keluarga.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Keluarga k = (Keluarga) (keluarga.getSelectedItem() == null ? null
							: keluarga.getSelectedItem().getValue());

					if (k == null) {
						MyMessageboxConfig.show("Pilih salah satu anak", "Peringatan", MyMessageboxConfig.OK,
								MyMessageboxConfig.INFORMATION);
						return;
					}

					setelah.onEvent(new Event("", keluarga, k));
				}
			});

		}
		Common.selectComboItem(true, keluarga, calonSiswa.getKeluarga());

		return keluarga;
	}

	public static AmbilDataSiswaBanbox alumni(CalonSiswa calonSiswa, GelombangPendaftaranPsb gelombangPendaftaranPsb,
			Rows rows, EventListener setelah) {
		return alumni(calonSiswa, gelombangPendaftaranPsb, rows, null, setelah);
	}

	public static AmbilDataSiswaBanbox alumni(CalonSiswa calonSiswa,
			final GelombangPendaftaranPsb gelombangPendaftaranPsb, Rows rows, final Row rowData,
			final EventListener setelah) {

		/* Bug lama: getTingkatDariAlumni() terkirim dua kali sehingga nilai
		 * tingkat (mis. "9") dipakai sebagai filter NAMA kelas dan daftar
		 * alumni selalu kosong. Argumen ke-5 yang benar: kelasDariAlumni. */
		final AmbilDataSiswaBanbox siswaAlumni = new AmbilDataSiswaBanbox(true, true,
				gelombangPendaftaranPsb.getAlumniDari(), gelombangPendaftaranPsb.getTingkatDariAlumni(),
				gelombangPendaftaranPsb.getKelasDariAlumni(), gelombangPendaftaranPsb.getTahunAkademikAlumni());
		siswaAlumni.setAttribute("myValue", calonSiswa.getSiswaAlumni());
		siswaAlumni.setAttribute("siswa", calonSiswa.getSiswaAlumni());
		siswaAlumni.setValue(calonSiswa.getSiswaAlumni() == null ? "" : calonSiswa.getSiswaAlumni().getNama());
		siswaAlumni.setWidth("90%");

		if (gelombangPendaftaranPsb.getHarusSebagaiAlumni() && calonSiswa.getId() == null) {

			HelperFormPpdb.barisForm(rows, rowData, "Pilih alumni dari siswa *", siswaAlumni);

			EventListener siswaAlumniEventListener = new EventListener() {

				private void doProseALumni(Siswa s) {
					try {
						setelah.onEvent(new Event("", siswaAlumni, s));
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/psb/form/PPDB.java:164");
					}
				}

				@Override
				public void onEvent(Event arg0) throws Exception {
					final Siswa s = (Siswa) siswaAlumni.getAttribute("siswa");
					if (s != null) {

						if (gelombangPendaftaranPsb.getTerdapatVerifikasiDenganNikAlumni()) {

							final MyWindow window = new MyWindow("Masukkan NIK Alumni", "none", true);
							window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
							window.setHeight("300px");
							window.setWidth("500px");

							Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
							borderlayout.setParent(window);

							Center center = new Center();
							ais.ui.util.ZkCompat.setFlex(center, true);
							center.setParent(borderlayout);

							MyGrid grid = new MyGrid();
							grid.setWidth("100%");
							grid.setParent(center);
							grid.setWidth("100%");
							grid.setHeight("100%");

							Columns columns = new Columns();
							columns.setParent(grid);

							MyColumnConfig column = new MyColumnConfig();
							column.setParent(columns);
							column.setWidth("30%");

							column = new MyColumnConfig();
							column.setParent(columns);

							Rows rows = new Rows();
							rows.setParent(grid);

							MyFormRow row = new MyFormRow();
							row.setValign("top");
							row.setParent(rows);
							row.appendChild(new ais.ui.util.MyLabelConfig("NIK Alumni"));
							final Textbox nik;
							row.appendChild(nik = new Textbox());
							nik.setWidth("90%");

							Common.initKeterangan(rows,
									"Guna melakukan verifikasi bahwa data Alumni yang Anda pilih benar, masukkan NIK Alumni tersebut");

							South south = new South();
							south.setParent(borderlayout);

							Toolbar toolbar = new Toolbar();
							// toolbar.setHeight("25px");
							toolbar.setParent(south);
							MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
							cancel.setTooltiptext("Tutup");
							cancel.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {

									siswaAlumni.setValue("");
									siswaAlumni.setAttribute("myValue", null);
									siswaAlumni.setAttribute("siswa", null);

									window.detach();
								}
							});
							cancel.setParent(toolbar);

							MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Proses Verifikasi NIK",
									"/img/excel.png");
							print.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {

									if (s.getNik().trim().equalsIgnoreCase(nik.getValue().trim())) {
										doProseALumni(s);
										window.detach();
									} else {
										MyMessageboxConfig.show("NIK yang Anda masukkan tidak sesuai", "Peringatan",
												MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
									}

								}
							});
							print.setParent(toolbar);

							window.setVisible(true);
							window.onModal();

						} else {
							doProseALumni(s);
						}

					}
				}
			};
			Common.createDefaultTimer(siswaAlumniEventListener);
			siswaAlumni.setEventListener(siswaAlumniEventListener);
		}

		return siswaAlumni;
	}

	public static AmbilDataSiswaBanbox sibling(CalonSiswa calonSiswa, GelombangPendaftaranPsb gelombangPendaftaranPsb,
			Rows rows, EventListener setelah) {
		return sibling(calonSiswa, gelombangPendaftaranPsb, rows, null, setelah);
	}

	public static AmbilDataSiswaBanbox sibling(CalonSiswa calonSiswa,
			final GelombangPendaftaranPsb gelombangPendaftaranPsb, Rows rows, final Row rowData,
			final EventListener setelah) {

		final AmbilDataSiswaBanbox siswaSibling = new AmbilDataSiswaBanbox(true);
		siswaSibling.setAttribute("myValue", calonSiswa.getSiswaSibling());
		siswaSibling.setAttribute("siswa", calonSiswa.getSiswaSibling());
		siswaSibling.setValue(calonSiswa.getSiswaSibling() == null ? "" : calonSiswa.getSiswaSibling().getNama());
		siswaSibling.setWidth("90%");

		if (gelombangPendaftaranPsb.getHarusSebagaiSaudara() && calonSiswa.getId() == null) {

			HelperFormPpdb.barisForm(rows, rowData, "Pilih sibling dari siswa *", siswaSibling);

			EventListener siswaSiblingEventListener = new EventListener() {

				private void doProseALumni(Siswa s) {
					try {
						setelah.onEvent(new Event("", siswaSibling, s));
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/psb/form/PPDB.java:298");
					}
				}

				@Override
				public void onEvent(Event arg0) throws Exception {
					final Siswa s = (Siswa) siswaSibling.getAttribute("siswa");
					if (s != null) {

						if (gelombangPendaftaranPsb.getTerdapatVerifikasiDenganNikSibling()) {

							final MyWindow window = new MyWindow("Masukkan NIK Sibling", "none", true);
							window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
							window.setHeight("300px");
							window.setWidth("500px");

							Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
							borderlayout.setParent(window);

							Center center = new Center();
							ais.ui.util.ZkCompat.setFlex(center, true);
							center.setParent(borderlayout);

							MyGrid grid = new MyGrid();
							grid.setWidth("100%");
							grid.setParent(center);
							grid.setWidth("100%");
							grid.setHeight("100%");

							Columns columns = new Columns();
							columns.setParent(grid);

							MyColumnConfig column = new MyColumnConfig();
							column.setParent(columns);
							column.setWidth("30%");

							column = new MyColumnConfig();
							column.setParent(columns);

							Rows rows = new Rows();
							rows.setParent(grid);

							MyFormRow row = new MyFormRow();
							row.setValign("top");
							row.setParent(rows);
							row.appendChild(new ais.ui.util.MyLabelConfig("NIK Sibling"));
							final Textbox nik;
							row.appendChild(nik = new Textbox());
							nik.setWidth("90%");

							Common.initKeterangan(rows,
									"Guna melakukan verifikasi bahwa data Sibling yang Anda pilih benar, masukkan NIK Sibling tersebut");

							South south = new South();
							south.setParent(borderlayout);

							Toolbar toolbar = new Toolbar();
							// toolbar.setHeight("25px");
							toolbar.setParent(south);
							MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
							cancel.setTooltiptext("Tutup");
							cancel.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {

									siswaSibling.setValue("");
									siswaSibling.setAttribute("myValue", null);
									siswaSibling.setAttribute("siswa", null);

									window.detach();
								}
							});
							cancel.setParent(toolbar);

							MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Proses Verifikasi NIK",
									"/img/excel.png");
							print.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {

									if (s.getNik().trim().equalsIgnoreCase(nik.getValue().trim())) {
										doProseALumni(s);
										window.detach();
									} else {
										MyMessageboxConfig.show("NIK yang Anda masukkan tidak sesuai", "Peringatan",
												MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
									}

								}
							});
							print.setParent(toolbar);

							window.setVisible(true);
							window.onModal();

						} else {
							doProseALumni(s);
						}

					}
				}
			};
			Common.createDefaultTimer(siswaSiblingEventListener);
			siswaSibling.setEventListener(siswaSiblingEventListener);
		}

		return siswaSibling;
	}
}
