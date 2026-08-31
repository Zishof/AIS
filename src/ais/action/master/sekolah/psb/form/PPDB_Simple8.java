package ais.action.master.sekolah.psb.form;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Box;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.maintenance.PSBAction;
import ais.action.master.pmb.BiodataCalonMahasiswaAction;
import ais.action.master.sekolah.CalonSiswaAction;
import ais.action.master.sekolah.helper.AmbilDataSiswaBanbox;
import ais.action.master.sekolah.psb.ParameterTambahanPsbListener;
import ais.action.master.sekolah.psb.VerifikasiMatapelajaranPSBHelper;
import ais.action.master.sekolah.psb.VerifikasiPSBHelper;
import ais.action.master.sekolah.psb.VerifikasiParameterPSBHelper;
import ais.action.master.sekolah.psb.nis.DefaultNisGenerator;
import ais.action.master.sekolah.psb.nis.NisGenerator;
import ais.common.Common;
import ais.common.CommonPSB;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Pegawai;
import ais.database.model.Tbmuser;
import ais.database.model.employ.Keluarga;
import ais.database.model.file.FotoCalonSiswa;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.GelombangPendaftaranPsb;
import ais.database.model.sekolah.PaketPsb;
import ais.database.model.sekolah.PaketPsbPunyaGelombangPendaftaranPsb;
import ais.database.model.sekolah.PenjurusanSekolah;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyGroupConfig;
import ais.ui.util.MyLabelAgakKecilBoldBiru;
import ais.ui.util.MyLabelBolder;
import ais.ui.util.MyMessageboxConfig;

/**
 * Varian formulir pendaftaran PSB (Penerimaan Siswa Baru) "Simple8" — versi ringkas yang hanya
 * meminta data inti calon siswa (nama, tempat/tanggal lahir, kontak orang tua, email) beserta
 * parameter tambahan dinamis (lewat {@link ParameterTambahanPsbListener}), info asal informasi
 * pendaftaran, opsi siswa pindahan/alumni, foto, dan pernyataan persetujuan — dibandingkan varian
 * PPDB lain di paket ini yang meminta data lebih lengkap. Memperluas {@code PPDB} untuk mewarisi
 * kerangka baku formulir PSB (kop gelombang pendaftaran, info gelombang, dsb.). Alur simpan: validasi
 * ({@link #check()}) lalu {@link #onSave(Event)} menyimpan {@link CalonSiswa} (nomor registrasi
 * dibangkitkan otomatis untuk data baru lewat {@code CommonPSB.generateNoRegistrasi}), menautkan
 * foto yang sudah diunggah sebelum entitas punya id, dan menyimpan penghubung berkas verifikasi
 * kelengkapan.
 */
public class PPDB_Simple8 extends PPDB {

	private static final long serialVersionUID = 1L;

	private Textbox nama;
	private Textbox tempatLahir;
	private MyDatebox tanggalLahir;
	private Textbox telpOrangTua;
	private Textbox email;

	private MyCheckboxConfig pernyataan;

	protected boolean baru;

	private Row rowParameterTambahan;
	private ArrayList<Row> parameterRows;
	private ParameterTambahanPsbListener parameterTambahanListener;
	private Rows subRowsVerifikasiKelengkapanCalonSiswa;
	private Rows subRowsVerifikasiNilaiRapor;
	private List<Rows> subRowsVerifikasiNilaiParameter;
	private Map<String, LampiranLain> lampiranLains = new HashMap<String, LampiranLain>();
	protected TreeSet<Long> hapusKelasLesSiswa = null;
	protected TreeSet<Long> selectedKelasLesSiswa;
	private Combobox paketPsb;
	private Box infoKampusDariMana;
	private Textbox keteranganInfoKampusDariMana;
	private Textbox namaTemanInfoKampusDariMana;
	private MyCheckboxConfig merupakanPindahan;
	private Textbox pindahanDariSekolah;
	private Textbox alamatSekolahPindahan;
	private Textbox keteranganPindah;
	private MyDatebox tanggalPindah;
	private Textbox kelasSekolahPindahan;
	private AmbilDataSiswaBanbox siswaAlumni;
	private Combobox keluarga;
	private FotoCalonSiswa fotoCalonSiswa;
	private Vbox vboxfotoCalonSiswa;
	private Combobox penjurusanSekolah;
	private Textbox namaIbu;

	private EventListener checkKesamaan = new EventListener() {
		@Override
		public void onEvent(Event arg0) throws Exception {
			CalonSiswaAction.CheckKesamaan checkKesamaan = new CalonSiswaAction.CheckKesamaan(calonSiswa,
					gelombangPendaftaranPsb, tanggalLahir, nama, namaIbu, PPDB_Simple8.this, new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							CalonSiswa calonSiswa1 = (CalonSiswa) arg0.getData();
							PPDB window = new PPDB_Simple8();
							window.setCalonSiswa(calonSiswa1);
							window.setGelombangPendaftaranPsb(gelombangPendaftaranPsb);
							window.setEventListener(eventListener);
							window.init();
							ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
							window.setHeight("95%");
							window.setWidth("750px");
							window.setVisible(true);
							window.onModal();
						}
					});
			checkKesamaan.onEvent(arg0);
		}
	};

	/** Konstruktor kosong (dipakai framework/refleksi); tidak langsung membangun form. */
	public PPDB_Simple8() {
		super();
	}

	/** Konstruktor utama; langsung membangun form lewat {@link #init()} untuk calon siswa dan gelombang pendaftaran yang diberikan. */
	public PPDB_Simple8(CalonSiswa calonSiswa, GelombangPendaftaranPsb gelombangPendaftaranPsb,
			EventListener eventListener) {
		super(calonSiswa, gelombangPendaftaranPsb, eventListener);
		init();
	}

	/**
	 * Membangun seluruh tampilan formulir pendaftaran: kop gelombang (gambar kustom bila diunggah,
	 * jika tidak header baku {@code PSBAction.headerBox()}), judul dan informasi gelombang, tautan
	 * lampiran info PPDB bila ada, lalu field data inti calon siswa, parameter tambahan dinamis,
	 * opsi siswa pindahan/alumni, unggah foto, dan pernyataan persetujuan.
	 */
	@SuppressWarnings({ "deprecation", "unchecked" })
	public void init() {

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("40%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		try {
			if ((calonSiswa != null && calonSiswa.getGelombangPendaftaranPsb() != null)) {
				GelombangPendaftaranPsb gel = calonSiswa.getGelombangPendaftaranPsb();
				LampiranLain kop = LampiranLain.ambil(gel.getId(), LampiranLain.KOP_GELOMBANG_PSB);
				if (kop != null && kop.getId() != null) {
					Image image = new Image(kop.createLinkUri());
					image.setWidth("100%");
					MyFormRow rowUtama1 = new MyFormRow();
					rowUtama1.setSclass("headerHbox");
					rowUtama1.appendChild(image);
					ais.ui.util.ZkCompat.setSpans(rowUtama1, "2");
					rowUtama1.setValign("top");
					rowUtama1.setParent(rows);
				} else {
					Hbox hbox = PSBAction.headerBox();
					MyFormRow rowUtama1 = new MyFormRow();
					rowUtama1.setSclass("headerHbox");
					rowUtama1.appendChild(hbox);
					ais.ui.util.ZkCompat.setSpans(rowUtama1, "2");
					rowUtama1.setValign("top");
					rowUtama1.setParent(rows);
				}
			} else {
				Hbox hbox = PSBAction.headerBox();
				MyFormRow rowUtama1 = new MyFormRow();
				rowUtama1.setSclass("headerHbox");
				rowUtama1.appendChild(hbox);
				ais.ui.util.ZkCompat.setSpans(rowUtama1, "2");
				rowUtama1.setValign("top");
				rowUtama1.setParent(rows);
			}
		} catch (Exception e) {
			Hbox hbox = PSBAction.headerBox();
			MyFormRow rowUtama1 = new MyFormRow();
			rowUtama1.setSclass("headerHbox");
			rowUtama1.appendChild(hbox);
			ais.ui.util.ZkCompat.setSpans(rowUtama1, "2");
			rowUtama1.setValign("top");
			rowUtama1.setParent(rows);
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/psb/form/PPDB_Simple8.java:init-header");
		}

		CalonSiswaAction.initBg(center, gelombangPendaftaranPsb);

		final Tbmuser tbmuser = Common.getCurrentUser();

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new MyLabelAgakKecilBoldBiru(
				Common.getBahasaConfig("FORMULIR PENDAFTARAN") + " "
						+ gelombangPendaftaranPsb.getSekolah().getNama().toUpperCase()
						+ Common.getBahasaConfig(" TAHUN PELAJARAN ") + gelombangPendaftaranPsb.getTahunAjaran()));

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new Html(gelombangPendaftaranPsb.getInformasi().replaceAll("\n", "<br>")));

		final LampiranLain lampiranLain = LampiranLain.ambil(gelombangPendaftaranPsb.getId(), "INFO_PPDB");
		if (lampiranLain != null && lampiranLain.getId() != null) {
			row = new MyFormRow();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");
			A a = new A(lampiranLain.getNama());
			a.setParent(row);
			a.setWidth("95%");
			a.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.display(lampiranLain);
				}
			});
		}

		// ===== DATA CALON SISWA =====

		MyGroupConfig myRowStyled = new MyGroupConfig("DATA CALON SISWA");
		myRowStyled.setParent(rows);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("1. Nama Lengkap Calon Siswa *"));
		row.appendChild(nama = new Textbox(calonSiswa.getNamaSiswa() == null ? "" : calonSiswa.getNamaSiswa()));
		nama.setWidth("90%");
		nama.addEventListener("onChange", checkKesamaan);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("2. Tempat Lahir *"));
		row.appendChild(tempatLahir = new Textbox(
				calonSiswa.getTempatLahir() == null ? "" : calonSiswa.getTempatLahir()));
		tempatLahir.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("3. Tanggal Lahir *"));
		row.appendChild(tanggalLahir = new MyDatebox());
		tanggalLahir.setValue(calonSiswa.getTanggalLahir());
		tanggalLahir.setWidth("90%");
		tanggalLahir.addEventListener("onChange", checkKesamaan);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("4. No. Telepon / HP Orang Tua *"));
		row.appendChild(telpOrangTua = new Textbox(
				calonSiswa.getHp1ayah() == null ? "" : calonSiswa.getHp1ayah()));
		telpOrangTua.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("5. Email Orang Tua"));
		row.appendChild(email = new Textbox(
				calonSiswa.getAlamatEmail() == null ? "" : calonSiswa.getAlamatEmail()));
		email.setWidth("90%");

		// Nama Ibu (tersembunyi, diperlukan checkKesamaan)
		namaIbu = new Textbox(calonSiswa.getNamaIbu() == null ? "" : calonSiswa.getNamaIbu());

		// Parameter Tambahan
		rowParameterTambahan = new MyFormRow();
		rowParameterTambahan.setVisible(false);
		rowParameterTambahan.setStyle("border:0px;background: transparent;");
		rowParameterTambahan.setParent(rows);
		rowParameterTambahan.appendChild(new MyLabelBolder("Form Tambahan"));

		parameterRows = new ArrayList<Row>();

		parameterTambahanListener = new ParameterTambahanPsbListener(calonSiswa, parameterRows, lampiranLains,
				gelombangPendaftaranPsb, false, rows);

		/*
		 * Verifikasi Kelengkapan Berkas ikut tampil pada PENDAFTARAN BARU supaya calon
		 * siswa bisa mengunggah berkas sekaligus. Penautan berkas ke baris penghubung
		 * diselesaikan VerifikasiPSBHelper.simpanVerifikasi() setelah data tersimpan.
		 */
		try {
			subRowsVerifikasiKelengkapanCalonSiswa = VerifikasiPSBHelper.tampilkanVerifikasi(calonSiswa, rows, null,
					calonSiswa.getId() != null ? calonSiswa.getGelombangPendaftaranPsb() : gelombangPendaftaranPsb);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "PPDB_Simple8.tampilkanVerifikasiBerkas");
		}

		if (calonSiswa.getId() != null) {
			try {
				subRowsVerifikasiNilaiRapor = VerifikasiMatapelajaranPSBHelper.tampilkanVerifikasi(calonSiswa, rows,
						gelombangPendaftaranPsb, null);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/psb/form/PPDB_Simple8.java:verif2");
			}
			try {
				subRowsVerifikasiNilaiParameter = VerifikasiParameterPSBHelper.tampilkanVerifikasi(calonSiswa, rows,
						null, calonSiswa.getGelombangPendaftaranPsb());
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/psb/form/PPDB_Simple8.java:verif3");
			}
		}

		try {
			parameterTambahanListener.onEvent(null);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/psb/form/PPDB_Simple8.java:param");
		}

		// Info Dari Mana
		try {
			Component[] cs = CalonSiswaAction.infoDariMana(rows, calonSiswa);
			infoKampusDariMana = (Box) cs[0];
			namaTemanInfoKampusDariMana = (Textbox) cs[1];
			keteranganInfoKampusDariMana = (Textbox) cs[2];
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/psb/form/PPDB_Simple8.java:infoDariMana");
		}

		// Pernyataan
		pernyataan = Common.tambahKeteranganRowHtml(rows,
				"Dengan ini saya menyatakan bahwa data yang saya masukkan benar adanya, dan jika ternyata dikemudian hari ditemukan kesalahan pada data ini baik yang disengaja ataupun tidak disengaja maka saya bersedia menerima sanksi dan resiko yang ditimbulkan karenanya");
		pernyataan.setChecked(calonSiswa.getPernyataan());

		if (calonSiswa.getId() != null) {
			pernyataan.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					session.refresh(calonSiswa);
					calonSiswa.setPernyataan(pernyataan.isChecked());
					Common.refreshUpdate(session, calonSiswa);
					session.flush();
				}
			});
		}

		// South: BATAL / DAFTAR|SIMPAN
		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setAlign("center");
		toolbar.setParent(south);

		MyButtonConfig cancel = new MyButtonConfig("B A T A L", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				PPDB_Simple8.this.detach();
			}
		});
		cancel.setParent(toolbar);

		final boolean daftar = tbmuser == null;
		final MyButtonConfig save = new MyButtonConfig(daftar ? "D A F T A R" : "S I M P A N   D A T A",
				"/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				baru = false;
				if (onSave(event)) {
					PPDB_Simple8.this.detach();
					if (PPDB_Simple8.this.eventListener != null) {
						PPDB_Simple8.this.eventListener.onEvent(new Event("", save, PPDB_Simple8.this.calonSiswa));
					}
					if (baru) {
						String informasi = Common.getKonfigurasi("informasi_registrasi_psb_berhasil_login",
								"Proses pendaftaran peserta didik baru berhasil dilakukan dengan nomor pendaftaran : [no_reg]. Silahkan catat nomor pendaftaran tersebut dan selanjutnya akan diarahkan ke Login.")
								.getNilai();
						informasi = org.apache.commons.lang3.StringUtils.replace(informasi, "[no_reg]",
								PPDB_Simple8.this.calonSiswa.getNoRegistrasi());
						MyMessageboxConfig.show(informasi, "Informasi", MyMessageboxConfig.OK,
								MyMessageboxConfig.INFORMATION, new EventListener() {
									@Override
									public void onEvent(Event arg0) throws Exception {
										Common.createDefaultTimer(new EventListener() {
											@Override
											public void onEvent(Event arg0) throws Exception {
												if (baru && PPDB_Simple8.this.calonSiswa
														.getGelombangPendaftaranPsb()
														.getOtomatisLoginSetelahDaftar()) {
													PPDB_Simple8.this.calonSiswa.setTelahLogin(true);
													PPDB_Simple8.this.calonSiswa
															.setWaktuLogin(ais.ui.util.WaktuUtil.getDate());
													Common.refreshUpdate(PPDB_Simple8.this.calonSiswa);
													Common.setLogin(PPDB_Simple8.this.calonSiswa);
													Sessions.getCurrent(true).setAttribute("cetak", true);
													Common.createDefaultTimer(new EventListener() {
														@Override
														public void onEvent(Event arg0) throws Exception {
															Executions.getCurrent().sendRedirect("");
														}
													});
												} else {
													CalonSiswaAction.onCetakKartu(
															PPDB_Simple8.this.calonSiswa, daftar);
												}
											}
										});
									}
								});
					}
				}
			}
		});
		save.setParent(toolbar);

		Common.masukkanListener(rows, masukkanPerubahan);
	}

	/**
	 * Memvalidasi field wajib formulir sebelum disimpan: nama, tempat lahir, tanggal lahir, telepon
	 * orang tua, batas umur sesuai aturan gelombang pendaftaran ({@link
	 * GelombangPendaftaranPsb#chekUmur}), persetujuan pernyataan dicentang, dan kelengkapan info asal
	 * pendaftaran ({@link CalonSiswaAction#checkInfoDariMana}). Setiap kegagalan menampilkan pesan
	 * dan memfokuskan/menggulir ke field yang bermasalah.
	 *
	 * @return {@code true} bila seluruh validasi lolos, {@code false} sebaliknya
	 * @throws Exception diteruskan apa adanya dari kegagalan pemeriksaan
	 */
	public boolean check() throws Exception {

		if (nama.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show(
					"Nama Calon Siswa harus diisi. Langkah yang dapat dilakukan: (1) lengkapi kolom Nama Calon Siswa; (2) pastikan data telah benar; (3) simpan kembali formulir.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							nama.focus();
							Clients.scrollIntoView(nama);
						}
					});
			return false;
		}

		if (tempatLahir.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show(
					"Tempat Lahir harus diisi. Langkah yang dapat dilakukan: (1) lengkapi kolom Tempat Lahir; (2) pastikan data telah benar; (3) simpan kembali formulir.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							tempatLahir.focus();
							Clients.scrollIntoView(tempatLahir);
						}
					});
			return false;
		}

		if (tanggalLahir.getValue() == null) {
			MyMessageboxConfig.show(
					"Tanggal Lahir harus diisi. Langkah yang dapat dilakukan: (1) lengkapi kolom Tanggal Lahir; (2) pastikan data telah benar; (3) simpan kembali formulir.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							tanggalLahir.focus();
							Clients.scrollIntoView(tanggalLahir);
						}
					});
			return false;
		}

		if (telpOrangTua.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show(
					"No. Telepon/HP Orang Tua harus diisi. Langkah yang dapat dilakukan: (1) lengkapi kolom No. Telepon/HP; (2) pastikan data telah benar; (3) simpan kembali formulir.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							telpOrangTua.focus();
							Clients.scrollIntoView(telpOrangTua);
						}
					});
			return false;
		}

		if (gelombangPendaftaranPsb != null && tanggalLahir.getValue() != null) {
			if (!GelombangPendaftaranPsb.chekUmur(gelombangPendaftaranPsb, tanggalLahir)) {
				return false;
			}
		}

		if (!pernyataan.isChecked()) {
			MyMessageboxConfig.show(
					"Pernyataan persetujuan harus dicentang. Langkah yang dapat dilakukan: (1) baca pernyataan yang tersedia; (2) centang kolom pernyataan; (3) simpan kembali formulir.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							pernyataan.focus();
							Clients.scrollIntoView(pernyataan);
						}
					});
			return false;
		}

		if (!CalonSiswaAction.checkInfoDariMana(infoKampusDariMana, namaTemanInfoKampusDariMana,
				keteranganInfoKampusDariMana)) {
			return false;
		}

		return true;
	}

	private EventListener masukkanPerubahan = new EventListener() {
		@Override
		public void onEvent(Event arg0) throws Exception {
			setdata();
			if (calonSiswa.getId() != null) {
				Common.refreshUpdate(calonSiswa);
			}
		}
	};

	/**
	 * Memvalidasi (lewat {@link #check()}) lalu menyimpan data calon siswa: menuliskan field form ke
	 * entitas {@link CalonSiswa} (termasuk nomor registrasi baru bila data baru), menyimpan/memperbarui
	 * entitas, menautkan foto yang sudah diunggah sebelum entitas punya id, menyimpan penghubung
	 * berkas verifikasi kelengkapan, dan membersihkan cache sesi {@link CalonSiswa}.
	 *
	 * @param event event ZK pemicu penyimpanan (tombol simpan)
	 * @return {@code true} bila berhasil disimpan, {@code false} bila validasi gagal
	 * @throws Exception diteruskan apa adanya dari kegagalan Hibernate saat menyimpan
	 */
	public boolean onSave(Event event) throws Exception {
		if (!check()) {
			return false;
		}

		Session session = HibernateUtil.currentSession();

		if (calonSiswa.getId() != null && calonSiswa.getId() > 0L) {
			calonSiswa = (CalonSiswa) session.load(CalonSiswa.class, calonSiswa.getId());
		}
		if (calonSiswa.getId() != null && calonSiswa.getId() < 0L) {
			calonSiswa.setId(null);
		}

		setdata();

		baru = false;
		if (calonSiswa.getId() == null) {
			session.save(calonSiswa);
			baru = true;
		} else {
			Common.refreshSaveOrUpdate(session, calonSiswa);
		}

		if (fotoCalonSiswa != null) {
			Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
			streamingSession.refresh(fotoCalonSiswa);
			fotoCalonSiswa.setCalonSiswa(calonSiswa.getId());
			streamingSession.getTransaction().begin();
			streamingSession.update(fotoCalonSiswa);
			streamingSession.getTransaction().commit();
			StreamingHibernateUtil.getInstance().closeSession();
		}

		/*
		 * Simpan penghubung berkas verifikasi sekaligus menautkan berkas yang diunggah
		 * sebelum calon siswa punya id (penautan tertunda).
		 */
		VerifikasiPSBHelper.simpanVerifikasi(calonSiswa, subRowsVerifikasiKelengkapanCalonSiswa);

		Common.hapusSession(CalonSiswa.class);

		return true;
	}

	private void setdata() {
		try {
			String info = BiodataCalonMahasiswaAction.infoMahasiswaBaru(infoKampusDariMana);

			calonSiswa.setGelombangPendaftaranPsb(gelombangPendaftaranPsb);
			calonSiswa.setNamaSiswa(nama.getValue());
			calonSiswa.setTempatLahir(tempatLahir.getValue());
			calonSiswa.setTanggalLahir(tanggalLahir.getValue());
			calonSiswa.setHp1ayah(telpOrangTua.getValue());
			calonSiswa.setWaAyah(telpOrangTua.getValue());
			calonSiswa.setAlamatEmail(email.getValue());
			calonSiswa.setPernyataan(pernyataan.isChecked());
			calonSiswa.setInfoKampusDariMana(info);
			calonSiswa.setKeteranganInfoKampusDariMana(
					keteranganInfoKampusDariMana == null ? "" : keteranganInfoKampusDariMana.getValue());
			calonSiswa.setNamaTemanInfoKampusDariMana(
					namaTemanInfoKampusDariMana == null ? "" : namaTemanInfoKampusDariMana.getValue());

			if (calonSiswa.getId() == null) {
				calonSiswa.setNoRegistrasi(CommonPSB.generateNoRegistrasi(calonSiswa));
			}

			parameterTambahanListener.onSave(calonSiswa);

			if (calonSiswa.getGelombangPendaftaranPsb() != null
					&& calonSiswa.getGelombangPendaftaranPsb().getHanyaUntukAnakPegawai()
					&& calonSiswa.getOrangTuaPegawai() == null) {
				Tbmuser tbmuserCur = Common.getCurrentUser();
				if (tbmuserCur != null && tbmuserCur.getPegawai() != null) {
					calonSiswa.setOrangTuaPegawai(tbmuserCur.getPegawai());
				}
			}

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/psb/form/PPDB_Simple8.java:setdata");
		}
	}
}
