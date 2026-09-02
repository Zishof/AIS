package ais.action.master;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import ais.ui.util.MyCaptionStyled;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Space;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.action.master.helper.PembayaranUtilHelper;
import ais.action.master.helper.TunggakanMahasiswaHelper;
import ais.action.master.helper.generic.AmbilDataItemBiayaBanyak;
import ais.action.master.helper.virtualaccount.DownloadTagihanMahasiswaBankNtt;
import ais.action.report.CommonReportHelper;
import ais.action.ws.util.ConstantUtil;
import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.dao.DaoFactory;
import ais.database.dao.KegiatanDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.CicilanPembayaran;
import ais.database.model.DetailBiaya;
import ais.database.model.DetailKegiatan;
import ais.database.model.ItemBiaya;
import ais.database.model.JenisKegiatan;
import ais.database.model.JenisPembayaran;
import ais.database.model.Kegiatan;
import ais.database.model.Konfigurasi;
import ais.database.model.LogPembayaran;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusMahasiswa;
import ais.database.model.TunggakanMahasiswa;
import ais.database.model.file.LampiranLain;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Controller/action ZK untuk pembayaran mahasiswa. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Combobox semester}, {@code Combobox
 * akun}, {@code MyDatebox tanggalValidasi}, {@code Row rowInfoTunggakan}, {@code Row rowNim}, {@code Row
 * rowNama}, {@code Row rowKewarganegaraan}, {@code Row rowJenisKuliah}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}); pembacaan/pencarian ({@code onCariMahasiswa()}, {@code
 * listBiaya()}, {@code loadData()}); validasi/perhitungan ({@code hitungJumlahBiayaSeharusnya()}); mutasi data
 * ({@code reset()}, {@code onSave()}); operasi domain lain ({@code formKomponenLengkap()}). Bagian lain dari
 * kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see GenericAutowireComposer
 */
public class PembayaranMahasiswaAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4681108885695239730L;

	private Combobox semester;
	private Combobox akun;
	private MyDatebox tanggalValidasi;

	private Row rowInfoTunggakan;
	private Row rowNim;
	private Row rowNama;
	private Row rowKewarganegaraan;
	private Row rowJenisKuliah;
	private Row rowProdi;
	private Row rowLampiran;
	private Row rowSemester;
	private Row rowTahunMasuk;
	private Row rowTahunAkademik;
	private Row rowTanggalValidasi;
	private Row rowValidator;
	private Row rowPengurangan;
	private Row rowKeterangan;
	private Row rowListBiaya;
	private Row rowButtonSave;

	private AmbilDataMahasiswaBanbox nim;
	private Label kewarganegaraan;
	private Label jenisKuliah;
	private Label prodi;
	private Label labelNimMahasiswa;
	private Label labelNamaMahasiswa;
	private Label labelTahunMasuk;
	private Label labelTahunAkademik;
	private Label validator;
	private MyDoublebox pengurangan;
	private Textbox keterangan;
	private MyGrid gridss;
	private MyButtonConfig save;

	private Hbox spaceBayar;

	Label labelFooter1;
	Label labelFooter2;

	private Combobox jenisPembayaran;

	private Mahasiswa mahasiswa;
	private Kegiatan kegiatan;
	private PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

	private Double nilaiBiayaHarusDiBayars = 0.0;

	private List<DetailBiaya> detailBiayas = new ArrayList<DetailBiaya>();
	private Center center;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	@SuppressWarnings("unchecked")
	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		boolean aktifkan_pembayaran_via_bank_ntt = Common.bolehKonfigurasi("aktifkan_pembayaran_via_bank_ntt", Konfigurasi.TIDAK_AKTIF);
		if (aktifkan_pembayaran_via_bank_ntt && spaceBayar != null) {

			final MyButtonConfig bayarBankNTT = new MyButtonConfig("BAYAR VIA BANK NTT");
			bayarBankNTT.setWidth("300px");
			bayarBankNTT.setHeight("55px");
			spaceBayar.appendChild(bayarBankNTT);

			bayarBankNTT.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							JenisKegiatan jenisKegiatan = (JenisKegiatan) (jenisPembayaran.getSelectedItem() == null
									? null
									: jenisPembayaran.getSelectedItem().getValue());
							Rows rows = (Rows) gridss.getRows();
							Map<Long, Object[]> biayas = new HashMap<Long, Object[]>();
							List<Row> myRows = rows.getChildren();
							for (Row row : myRows) {
								DetailBiaya detailBiaya = (DetailBiaya) row.getAttribute("myValue");
								Double biaya = detailBiaya.getNilaiBiayaBaru() == null ? detailBiaya.getNilaiBiaya()
										: detailBiaya.getNilaiBiayaBaru();
								biayas.put(detailBiaya.getId(), new Object[] { detailBiaya, biaya });
							}

							if (!biayas.isEmpty()) {
								DownloadTagihanMahasiswaBankNtt.downloadData(mahasiswa, labelTahunAkademik.getValue(),
										(Integer) semester.getSelectedItem().getValue(), jenisKegiatan, biayas);
							} else {
								MyMessageboxConfig.show("Masukkan tagihan yang akan dibayarkan", "Peringatan",
										MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
							}

						}
					}, "Proses pembayaran ..");
				}
			});
		}

		if (save != null) {
			String labelBayar = Common.getBahasa("label_bayar");
			save.setLabel(labelBayar);
		}

		nim.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onCariMahasiswa();
			}
		});

		// jenisPembayaran =
		// Common.initJenisPembayaranMahasiswa(jenisPembayaran);
		Session session = HibernateUtil.currentSession();
		List<JenisKegiatan> jenisKegiatans = session.createCriteria(JenisKegiatan.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.or(Restrictions.isNull("defaultKegiatan"), Restrictions.eq("defaultKegiatan", false)))
				.list();
		for (JenisKegiatan jenisKegiatan : jenisKegiatans) {
			MyComboitemConfig comboitem = new MyComboitemConfig(jenisKegiatan.getNamaKegiatan());
			comboitem.setValue(jenisKegiatan);
			jenisPembayaran.appendChild(comboitem);
		}

		int maxSemesterPilihan = 25;
		try {
			maxSemesterPilihan = Integer
					.parseInt(Common.getKonfigurasi("max_semester_pilihan", "25").getNilai().trim());
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		if (semester != null) {
			for (int i = 1; i < maxSemesterPilihan; i++) {
				org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
				comboitem.setLabel(i + "");
				comboitem.setValue(i);
				semester.appendChild(comboitem);
			}
		}

		if (save == null) {
			// Komponen "save" tidak ada pada template halaman ini (mis. zul
			// belum/tidak menyediakan tombol simpan) - lewati pemasangan
			// listener agar tidak NPE saat doAfterCompose dijalankan.
			return;
		}

		save.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				Integer selectedSemester = (Integer) (semester.getSelectedItem() == null ? null
						: semester.getSelectedItem().getValue());
				if (selectedSemester == null) {
					MyMessageboxConfig.show("Semester harus dipilih", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}
				// JenisKegiatan kegiatanDaftarUlangMahasiswaBaru =
				// pembayaranUtil
				// .generateJenisKegiatan(ConstantUtil.PENDAFTARAN_ULANG_MAHASISWA_BARU);
				// List<TunggakanMahasiswa> tunggakanMahasiswas =
				// pembayaranUtil.getTunggakanMahasiswa(
				// new JenisKegiatan[] { kegiatanDaftarUlangMahasiswaBaru,
				// jenisKegiatan }, mahasiswa,
				// HibernateUtil.currentSession());
				// for (TunggakanMahasiswa tunggakanMahasiswa :
				// tunggakanMahasiswas) {
				// if (tunggakanMahasiswa.getSemester() < selectedSemester) {
				// MyMessageboxConfig.show(
				// "Mahasiswa ini masih mempunyai tunggakan di semester "
				// + tunggakanMahasiswa.getSemester(),
				// "Peringatan", MyMessageboxConfig.OK,
				// MyMessageboxConfig.EXCLAMATION);
				// return;
				// }
				// }

				MyMessageboxConfig.show(
						"Apakah yakin ingin melakukan pembayaran untuk:\nMahasiswa : " + mahasiswa.getNama()
								+ "\nJumlah : " + labelFooter2.getValue() + " ",
						"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {

									if (onSave(kegiatan, mahasiswa, event)) {
										Common.freeze(center, true);
									}

								}

							}
						});

			}
		});

	}

	/**
	 * Sebagian template zul untuk halaman ini bisa saja tidak menyediakan
	 * seluruh komponen form detail (mis. rowNim/rowNama/semester/save dst)
	 * misalnya karena di-embed pada halaman lain via Include. Cek ini
	 * dipakai sebagai guard defensif sebelum memakai komponen-komponen
	 * tersebut agar tidak NullPointerException - BUKAN untuk mengubah
	 * perilaku pencarian/reset saat form lengkap dan normal.
	 */
	private boolean formKomponenLengkap() {
		return rowNim != null && rowNama != null && rowKewarganegaraan != null && rowJenisKuliah != null
				&& rowProdi != null && rowSemester != null && rowTahunMasuk != null && rowTahunAkademik != null
				&& rowTanggalValidasi != null && rowValidator != null && rowPengurangan != null
				&& rowKeterangan != null && rowListBiaya != null && rowButtonSave != null && rowLampiran != null
				&& semester != null && labelNimMahasiswa != null && labelNamaMahasiswa != null
				&& kewarganegaraan != null && jenisKuliah != null && prodi != null && labelTahunMasuk != null
				&& labelTahunAkademik != null && tanggalValidasi != null && validator != null
				&& keterangan != null;
	}

	private void reset() {
		if (!formKomponenLengkap()) {
			return;
		}
		rowNim.setVisible(false);
		rowNama.setVisible(false);
		rowKewarganegaraan.setVisible(false);
		rowJenisKuliah.setVisible(false);
		rowProdi.setVisible(false);
		rowSemester.setVisible(false);
		rowTahunMasuk.setVisible(false);
		rowTahunAkademik.setVisible(false);
		rowTanggalValidasi.setVisible(false);
		rowValidator.setVisible(false);
		rowPengurangan.setVisible(false);
		rowKeterangan.setVisible(false);
		rowListBiaya.setVisible(false);
		rowButtonSave.setVisible(false);
	}

	private EventListener eventListener = new EventListener() {

		@Override
		public void onEvent(Event event) throws Exception {

			Common.clear(rowListBiaya);
			if (semester.getSelectedItem() == null || semester.getSelectedItem().getValue() == null) {
				MyMessageboxConfig.show("Semester harus dipilih", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return;
			}
			Integer tahunAngkatanMhs = mahasiswa.getTahunangkatan();
			Integer semesterMulai = mahasiswa.getPindahKeKampusIniMasukSemester();
			Integer tahunAkademikMulai = Common.getTahunAkademik((Integer) semester.getSelectedItem().getValue(),
					tahunAngkatanMhs, semesterMulai, mahasiswa.getSemesterMulai());

			String tahunAkademik = tahunAkademikMulai + "/" + (tahunAkademikMulai + 1);

			JenisKegiatan jenisKegiatan = (JenisKegiatan) (jenisPembayaran.getSelectedItem() == null ? null
					: jenisPembayaran.getSelectedItem().getValue());

			labelTahunAkademik.setValue(tahunAkademik);
			rowListBiaya.setVisible(true);

			kegiatan = mahasiswa.ambilKegiatans((Integer) semester.getSelectedItem().getValue(), jenisKegiatan);

			final Integer selectedSemester = (Integer) (semester.getSelectedItem() == null ? null
					: semester.getSelectedItem().getValue());
			if (selectedSemester == null) {
				MyMessageboxConfig.show("Semester harus dipilih", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return;
			}

			listBiaya(rowListBiaya, mahasiswa, kegiatan);
		}
	};

	private Hbox hboxJenisPembayaran;

	protected LampiranLain lainMahasiswa;

	public void onCariMahasiswa() throws Exception {
		kegiatan = null;
		if (nim.getValue().equals("")) {
			MyMessageboxConfig.show("Masukkan NIM Mahasiswa", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			reset();
			return;
		}
		if (jenisPembayaran.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Jenis Pembayaran",
					"Kolom Jenis Pembayaran belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Jenis Pembayaran.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			reset();
			return;
		}
		// JenisKegiatan jenisKegiatan = (JenisKegiatan)
		// (jenisPembayaran.getSelectedItem() == null ? null
		// : jenisPembayaran.getSelectedItem().getValue());

		try {
			mahasiswa = (Mahasiswa) nim.getAttribute("mahasiswa");

			if (mahasiswa == null) {
				reset();
				return;
			}

			Session session = HibernateUtil.currentSession();
			session.refresh(mahasiswa);

			{

				int countMahasiswaPindahan = ((Number) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.eq("alihProdiMahasiswa", mahasiswa)).setProjection(Projections.rowCount())
						.uniqueResult()).intValue();

				if (countMahasiswaPindahan > 0) {
					MyMessageboxConfig.show("Mahasiswa dengan NIM " + mahasiswa.getNim() + " telah pindah prodi",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					reset();
					return;
				}

				// StatusMahasiswa statusMahasiswa =
				// ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(mahasiswa).getStatusMahasiswa();
				//
				// if
				// (statusMahasiswa.getId().equals(ConstantValues.LULUS.getId()))
				// {
				// MyMessageboxConfig.show("Mahasiswa tersebut sudah lulus",
				// "Peringatan", MyMessageboxConfig.OK,
				// MyMessageboxConfig.EXCLAMATION);
				// reset();
				// return;
				// }

				// if (mahasiswa.getStatus().getId()
				// .equals(ConstantValues.DROP_OUT.getId())) {
				// MyMessageboxConfig
				// .show("Mahasiswa tersebut sudah di - DROP OUT",
				// "Peringatan", MyMessageboxConfig.OK,
				// MyMessageboxConfig.EXCLAMATION);
				// reset();
				// return;
				// }

				// JenisKegiatan kegiatanDaftarUlangMahasiswaBaru =
				// pembayaranUtil
				// .generateJenisKegiatan(ConstantUtil.PENDAFTARAN_ULANG_MAHASISWA_BARU);
				// List<TunggakanMahasiswa> tunggakanMahasiswas =
				// pembayaranUtil.getTunggakanMahasiswa(
				// new JenisKegiatan[] { kegiatanDaftarUlangMahasiswaBaru,
				// jenisKegiatan }, mahasiswa,
				// HibernateUtil.currentSession());
				//
				// Common.clear(rowInfoTunggakan);
				// if (tunggakanMahasiswas.size() != 0) {
				// new TunggakanMahasiswaHelper().display(rowInfoTunggakan,
				// tunggakanMahasiswas);
				// }

				// Integer bulan = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH);
				final Integer tahunAngkatanMhs = mahasiswa.getTahunangkatan();
				String semesterMulai = Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP;

				Integer smt = Common.getSemester(tahunAngkatanMhs, semesterMulai,
						mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai());

				// kegiatan = pembayaranUtil.checkKegiatanMahasiswa(mahasiswa,
				// smt, jenisKegiatan);

				kegiatan = null;

				if (!formKomponenLengkap()) {
					// Komponen form detail (rowNim/rowNama/semester/dll) tidak
					// tersedia pada template halaman ini - lewati render detail
					// mahasiswa agar tidak NullPointerException.
					reset();
					return;
				}

				rowNim.setVisible(true);
				labelNimMahasiswa.setValue(mahasiswa.getNim());
				rowNama.setVisible(true);
				labelNamaMahasiswa.setValue(mahasiswa.getNama());
				rowKewarganegaraan.setVisible(true);
				kewarganegaraan.setValue(mahasiswa.getWarganegara());

				Common.clear(rowLampiran);

				rowLampiran.appendChild(new Label(ais.common.Common.getBahasaConfig("File Bukti Pembayaran")));
				Hbox hbox = new Hbox();
				LampiranLain.createDownloadUploadFileLain(hbox, kegiatan == null ? null : kegiatan.getId(),
						"Bukti Pembayaran", "Bukti Pembayaran", false, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								lainMahasiswa = (LampiranLain) arg0.getData();
							}
						});
				hbox.setParent(rowLampiran);

				rowLampiran.setVisible(true);

				rowJenisKuliah.setVisible(true);
				// Common.selectComboItem(jenisKuliah, "Reguler");
				jenisKuliah.setValue("Reguler");
				rowProdi.setVisible(true);
				prodi.setValue(mahasiswa.getJurusan().getNama());
				// Common.selectComboItem(prodi, mahasiswa.getJurusan());
				rowSemester.setVisible(true);
				semester.setDisabled(false);
				Common.selectComboItem(semester, smt);

				semester.addEventListener(Events.ON_CHANGE, eventListener);

				rowTahunMasuk.setVisible(true);
				labelTahunMasuk.setValue(mahasiswa.getTahunangkatan().toString());
				rowTahunAkademik.setVisible(true);

				tanggalValidasi.setDisabled(false);
				rowTanggalValidasi.setVisible(true);
				rowValidator.setVisible(true);
				rowPengurangan.setVisible(true);
				rowKeterangan.setVisible(true);

				/*
				 * if (jenisKegiatanDetail != null)
				 * tanggalValidasi.setValue(kegiatan.getTanggal()); else
				 */
				if (kegiatan != null) {
					// Common.freeze(center, true);
					tanggalValidasi.setValue(kegiatan.getTanggal());
					validator.setValue(kegiatan.getValidator() == null ? "" : kegiatan.getValidator());

					keterangan.setValue(kegiatan.getKeterangan() == null ? "" : kegiatan.getKeterangan());
				} else
					tanggalValidasi.setValue(ais.ui.util.WaktuUtil.getDate());
				// rowButton.setVisible(true);

				Common.clear(rowListBiaya);
				rowListBiaya.setVisible(true);
				eventListener.onEvent(null);

			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

	}

	@SuppressWarnings({ "unchecked" })
	public void listBiaya(final Component comp, final Mahasiswa mahasiswa, final Kegiatan keg) throws Exception {
		kegiatan = keg;
		final Integer selectedSemester = (Integer) (semester.getSelectedItem() == null ? null
				: semester.getSelectedItem().getValue());

		detailBiayas = new ArrayList<DetailBiaya>();
		final JenisKegiatan jenisKegiatan = (JenisKegiatan) (jenisPembayaran.getSelectedItem() == null ? null
				: jenisPembayaran.getSelectedItem().getValue());

		this.mahasiswa = mahasiswa;

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.appendChild(new MyCaptionStyled("Daftar Biaya"));

		groupbox.setStyle("min-height: 200px;");
		groupbox.setWidth("95%");
		groupbox.setParent(comp);
		SatuanKerja satuanKerja = Common.getSatuanKerja();
		Common.insertCombo(akun = new Combobox(), "nama", "akun", JenisPembayaran.class,
				Restrictions.and(
						satuanKerja == null ? Restrictions.sqlRestriction("true")
								: Restrictions.or(Restrictions.isNull("satuanKerja"),
										Restrictions.eq("satuanKerja", satuanKerja)),
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));
		hboxJenisPembayaran = new Hbox(
				new Component[] { new Label(ais.common.Common.getBahasaConfig("Cara Pembayaran : ")), new Space(), new Space(), new Space(), akun });
		akun.setCols(50);
		hboxJenisPembayaran.setParent(groupbox);

		if (kegiatan != null && kegiatan.getId() != null) {
			CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) HibernateUtil.currentSession()
					.createCriteria(CicilanPembayaran.class).add(Restrictions.eq("kegiatan", kegiatan))
					.addOrder(Order.asc("id")).setMaxResults(1).uniqueResult();
			if (cicilanPembayaran != null) {
				Common.selectComboItem(akun, cicilanPembayaran.getJenisPembayaran());
			} else {
				Common.selectComboItem(akun, ConstantValues.TUNAI);
			}
		} else {
			Common.selectComboItem(akun, ConstantValues.TUNAI);
		}

		if (akun != null && akun.getSelectedItem() == null) {
			JenisPembayaran jenisPembayaranDefault = (JenisPembayaran) HibernateUtil.currentSession()
					.createCriteria(JenisPembayaran.class).add(Restrictions.eq("defaultPembayaran", true))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setMaxResults(1)
					.uniqueResult();
			Common.selectComboItem(akun, jenisPembayaranDefault);
		}

		hboxJenisPembayaran.setVisible(Common.bolehKonfigurasi("integrasi_modul_akuntansi", Konfigurasi.TIDAK_AKTIF));

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Item Biaya", "/img/new.gif");

		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				List<ItemBiaya> itemBiayas = new ArrayList<ItemBiaya>();
				for (DetailBiaya detailBiaya : detailBiayas) {
					itemBiayas.add(detailBiaya.getItemBiaya());
				}

				AmbilDataItemBiayaBanyak window = new AmbilDataItemBiayaBanyak(itemBiayas);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
				window.setWidth("700px");
				window.setHeight("90%");

				window.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<ItemBiaya> itemBiayas = (List<ItemBiaya>) arg0.getData();
						if (itemBiayas != null) {
							rowButtonSave.setVisible(itemBiayas.size() != 0);

							Session session = HibernateUtil.currentSession();
							StatusMahasiswa statusMahasiswa = ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(mahasiswa).getStatusMahasiswa();

							for (ItemBiaya itemBiaya : itemBiayas) {
								DetailBiaya detailBiaya = (DetailBiaya) session.createCriteria(DetailKegiatan.class)
										.createAlias("detailBiaya", "detailBiaya")
										.add(Restrictions.eq("detailBiaya.itemBiaya", itemBiaya))
										.createAlias("kegiatan", "kegiatan")
										.setProjection(Projections.property("detailBiaya"))
										.add(Restrictions.eq("kegiatan.mahasiswa", mahasiswa))
										.add(Restrictions.eq("kegiatan.jenisKegiatan", jenisKegiatan))
										.add(Restrictions.eq("kegiatan.semster", selectedSemester))
										.add(Restrictions.eq("kegiatan.tahunAkademik", labelTahunAkademik.getValue()))
										.setMaxResults(1).uniqueResult();

								if (detailBiaya == null) {
									detailBiaya = new DetailBiaya();
									detailBiaya.setAngkatan(mahasiswa.getTahunangkatan());
									detailBiaya.setFakultas(mahasiswa.getJurusan().getFakultas());
									detailBiaya.setItemBiaya(itemBiaya);
									detailBiaya.setJenisKegiatan(jenisKegiatan);
									detailBiaya.setJenisSeleksi(null);
									detailBiaya.setJenjang(mahasiswa.getJenjang());
									detailBiaya.setJurusan(mahasiswa.getJurusan());
									detailBiaya.setMerupakanPembayaran(true);
									detailBiaya.setNama("Pembayaran Mahasiswa");
									detailBiaya.setNilaiBiaya(0.0);
									detailBiaya.setProgram(mahasiswa.getProgram());
									detailBiaya.setSemester(selectedSemester);
									detailBiaya.setStatusMahasiswa(statusMahasiswa);
									detailBiaya.setTahunAkademik(labelTahunAkademik.getValue());
									detailBiaya.setWnaAtauWni(mahasiswa.getWarganegara());
									HibernateUtil.currentSession().save(detailBiaya);
								}

								detailBiayas.add(detailBiaya);

							}
							loadData(detailBiayas);

						}
					}
				});

				window.onModal();

			}

		});
		button.setParent(toolbar);

		gridss = new MyGrid();
		gridss.setMold("paging");
		gridss.setPageSize(1000);
		gridss.setParent(groupbox);
		gridss.setWidth("100%");

		Columns columns = new Columns();
		columns.setParent(gridss);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Item Biaya");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Biaya");
		column.setWidth("25%");
		column.setAlign("right");

		Foot foot = new Foot();
		foot.setParent(gridss);

		Footer footer = new Footer();
		footer.setParent(foot);
		labelFooter1 = new Label();
		labelFooter1.setParent(footer);
		labelFooter1.setValue("Jumlah Biaya");

		footer = new Footer();
		footer.setAlign("right");
		footer.setParent(foot);
		labelFooter2 = new Label();
		labelFooter2.setParent(footer);

		if (kegiatan != null && kegiatan.getId() != null && kegiatan.getAmount() > 0.1) {
			save.setDisabled(true);
			save.setLabel("Mahasiswa ini sudah melakukan pembayaran");
			MyMessageboxConfig.show("Mahasiswa ini sudah melakukan pembayaran", "Pemberitahuan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							CommonReportHelper.cetakBuktipembayaranMahasiswa(kegiatan, true);

							pembayaranUtil.updateTunggakan(kegiatan, HibernateUtil.currentSession());

						}
					});

			Session session = HibernateUtil.currentSession();
			detailBiayas = session.createCriteria(DetailKegiatan.class).createAlias("kegiatan", "kegiatan")
					.setProjection(Projections.property("detailBiaya"))
					.add(Restrictions.eq("kegiatan.mahasiswa", mahasiswa))
					.add(Restrictions.eq("kegiatan.jenisKegiatan", jenisKegiatan))
					.add(Restrictions.eq("kegiatan.semster", selectedSemester))
					.add(Restrictions.eq("kegiatan.tahunAkademik", labelTahunAkademik.getValue())).list();

			loadData(detailBiayas);

		} else {
			save.setDisabled(false);
			if (save != null) {
				String labelBayar = Common.getBahasa("label_bayar");
				save.setLabel(labelBayar);
			}
		}

	}

	public void loadData(List<DetailBiaya> detailBiayas) throws Exception {
		this.detailBiayas = detailBiayas;
		ListModel strset = null;
		strset = new SimpleListModel(detailBiayas);
		gridss.setRowRenderer(new DetailBiayaMahasiswaRenderer());
		gridss.setModelCheckMobile(strset);
		gridss.renderAll();

		hitungJumlahBiayaSeharusnya();
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link PembayaranMahasiswaAction}. Kelas ini menerjemahkan satu item
	 * data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link PembayaranMahasiswaAction} dan dapat mengakses
	 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see PembayaranMahasiswaAction
	 */
	class DetailBiayaMahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final DetailBiaya detailBiaya = (DetailBiaya) arg1;
			arg0.setAttribute("myValue", detailBiaya);

			Session session = HibernateUtil.currentSession();

			DetailKegiatan detailKegiatan = detailBiaya == null || kegiatan == null || detailBiaya.getId() == null
					|| kegiatan.getId() == null
							? null
							: (DetailKegiatan) session.createCriteria(DetailKegiatan.class)
									.add(Restrictions.eq("detailBiaya", detailBiaya))
									.add(Restrictions.eq("kegiatan", kegiatan)).setMaxResults(1).uniqueResult();

			new Label(detailBiaya.getItemBiaya().getNama()).setParent(arg0);

			final MyDoublebox harusDiBayar = new MyDoublebox(detailKegiatan == null ? 0.0 : detailKegiatan.getBiaya());
			harusDiBayar.setWidth("90%");
			harusDiBayar.setParent(arg0);

			harusDiBayar.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					detailBiaya.setNilaiBiaya(harusDiBayar.getValue());
					hitungJumlahBiayaSeharusnya();
				}
			});
		}
	}

	@SuppressWarnings("unchecked")
	public boolean onSave(Kegiatan keg, Mahasiswa mahasiswa, Event event) throws Exception {
		kegiatan = keg;
		JenisKegiatan jenisKegiatan = (JenisKegiatan) (jenisPembayaran.getSelectedItem() == null ? null
				: jenisPembayaran.getSelectedItem().getValue());

		if (jenisKegiatan == null) {
			MyMessageboxConfig.show("Jenis pembayaran harus dipilih", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (hboxJenisPembayaran.isVisible() && akun.getSelectedItem() == null) {
			MyMessageboxConfig.show("Cara pembayaran harus dipilih", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (Common.bolehKonfigurasi("harus_menyertakan_bukti_pembayaran", Konfigurasi.TIDAK_AKTIF) && lainMahasiswa == null) {
			MyMessageboxConfig.show("Bukti pembayaran harus dilengkapi, harap upload bukti pembayaran.", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		try {
			KegiatanDao kegiatanDao = DaoFactory.getInstance().getKegiatanDao();

			Rows rows = (Rows) gridss.getRows();

			if (kegiatan != null && kegiatan.getId() != null) {
				kegiatan = kegiatanDao.load(kegiatan.getId());
			} else {
				kegiatan = new Kegiatan();
			}

			this.mahasiswa = mahasiswa;
			StatusMahasiswa statusMahasiswa = ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(mahasiswa).getStatusMahasiswa();
			statusMahasiswa = PembayaranUtilHelper.statusMahasiswaPembayaranEfektif(statusMahasiswa);

			kegiatan.setStatusMahasiswa(statusMahasiswa);
			kegiatan.setJenisKegiatan(jenisKegiatan);
			kegiatan.setMahasiswa(mahasiswa);
			// kegiatan.setProgram(mahasiswa.getProgram());
			kegiatan.setSemster(
					semester.getSelectedItem() == null ? 1 : (Integer) semester.getSelectedItem().getValue());
			kegiatan.setTahunAkademik(labelTahunAkademik.getValue() == null ? "" : labelTahunAkademik.getValue());
			kegiatan.setTanggal(
					tanggalValidasi.getValue() == null ? ais.ui.util.WaktuUtil.getDate() : tanggalValidasi.getValue());
			kegiatan.setValidated(1);
			kegiatan.setJenisKegiatan(jenisKegiatan);
			kegiatan.setValidator(Common.getCurrentUser().getUserNama());
			kegiatan.setPengurangan(pengurangan.getValue() == null ? 0.0 : pengurangan.getValue());
			kegiatan.setKeterangan(keterangan.getValue().trim());
			kegiatan.setAmount(nilaiBiayaHarusDiBayars);
			kegiatan.setKodeUnikLain(true);
			validator.setValue(kegiatan.getValidator());

			keterangan.setValue(kegiatan.getKeterangan() == null ? "" : kegiatan.getKeterangan());

			Session session = kegiatanDao.getCurrentSession();

			if (kegiatan.getId() != null) {
				kegiatanDao.update(kegiatan);
			} else {
				kegiatanDao.save(kegiatan);
			}

			if (nilaiBiayaHarusDiBayars != null && nilaiBiayaHarusDiBayars > 0.1) {
				LogPembayaran logPembayaran = new LogPembayaran();
				logPembayaran.setKegiatan(kegiatan);
				logPembayaran.setNominal(nilaiBiayaHarusDiBayars);
				Common.refreshSaveOrUpdate(logPembayaran);
			}

			if (rows != null && rows.getChildren() != null) {
				List<Row> myRows = rows.getChildren();
				for (Row row : myRows) {
					DetailBiaya detailBiaya = (DetailBiaya) row.getAttribute("myValue");

					DetailKegiatan detailKegiatan = kegiatan.ambilSatuDetailKegiatan(detailBiaya, true);
					if (detailKegiatan == null) {
						detailKegiatan = new DetailKegiatan();
					}
					Double biaya = detailBiaya.getNilaiBiayaBaru() == null ? detailBiaya.getNilaiBiaya()
							: detailBiaya.getNilaiBiayaBaru();
					try {
						MyDoublebox myLabel = (MyDoublebox) row.getChildren().get(1);
						// System.out.println("myLabel = " + myLabel.getValue()
						// + ", detailBiaya = " + detailBiaya);
						Double nilaiBiayas = myLabel.getValue();
						biaya = (myLabel.getValue() == null ? 0.0 : nilaiBiayas);
						detailBiaya.setNilaiBiaya(biaya);
						detailBiaya.setNilaiBiayaBaru(biaya);
						Common.refreshUpdate(session, detailBiaya);
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
					detailKegiatan.setBiaya(biaya);
					detailKegiatan.setDetailBiaya(detailBiaya);
					detailKegiatan.setKeterangan(detailBiaya.getKeterangan());
					detailKegiatan.setKegiatan(kegiatan);
					Common.refreshSaveOrUpdate(session, detailKegiatan);

				}
			}

			CicilanPembayaran cicilanPembayaran = Common.simpanCicilanTanpaMencicil(kegiatan, nilaiBiayaHarusDiBayars,
					tanggalValidasi.getValue(), keterangan.getValue(),
					(JenisPembayaran) (akun.getSelectedItem() == null ? null : akun.getSelectedItem().getValue()),
					detailBiayas, session);

			if (lainMahasiswa != null) {
				cicilanPembayaran.setIdLampiran(lainMahasiswa.getId());
				Common.refreshUpdate(cicilanPembayaran);

				try {
					Session sessionStream = StreamingHibernateUtil.getInstance().currentSession();

					sessionStream.refresh(lainMahasiswa);
					lainMahasiswa.setRef(kegiatan.getId());

					sessionStream.getTransaction().begin();
					sessionStream.update(lainMahasiswa);
					sessionStream.getTransaction().commit();

					StreamingHibernateUtil.getInstance().closeSession();
				} catch (Exception e) {
					StreamingHibernateUtil.getInstance().rollbackTransaction();
					Common.tampilErrorJikaAdmin(e);
				}
			}

			MyMessageboxConfig.show("Pembayaran Berhasil Dilakukan", "Pemberitahuan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							if (Common.bolehKonfigurasi("cetak_bukti_pembayaran_setelah_proses_pembayaran")) {
								CommonReportHelper.cetakBuktipembayaranMahasiswa(kegiatan, true);

							}
						}
					});

			session.flush();
			pembayaranUtil.updateTunggakan(kegiatan, HibernateUtil.currentSession());

			Thread.sleep(1000);
			JenisKegiatan kegiatanDaftarUlangMahasiswaBaru = pembayaranUtil
					.generateJenisKegiatan(ConstantUtil.PENDAFTARAN_ULANG_MAHASISWA_BARU);
			List<TunggakanMahasiswa> tunggakanMahasiswas = pembayaranUtil.getTunggakanMahasiswa(
					new JenisKegiatan[] { kegiatanDaftarUlangMahasiswaBaru, kegiatan.getJenisKegiatan() },
					kegiatan.getMahasiswa(), HibernateUtil.currentSession());

			Common.clear(rowInfoTunggakan);
			if (tunggakanMahasiswas.size() != 0) {
				new TunggakanMahasiswaHelper().display(rowInfoTunggakan, tunggakanMahasiswas);
			}

			return true;
		} catch (Exception e) {
			MyMessageboxConfig.show("Pembayaran Gagal Dilakukan", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			Common.tampilErrorJikaAdmin(e);
			return false;
		}

	}

	public void hitungJumlahBiayaSeharusnya() throws ParseException {

		Rows rows = (Rows) gridss.getRows();
		nilaiBiayaHarusDiBayars = 0.0;
		if (rows != null && rows.getChildren() != null) {
			for (int i = 0; i < rows.getChildren().size(); i++) {
				Row myRow = (Row) rows.getChildren().get(i);
				if (myRow.getChildren().get(1) instanceof MyDoublebox) {
					MyDoublebox myLabel = (MyDoublebox) myRow.getChildren().get(1);
					// System.out.println("myLabel = " + myLabel.getValue());
					Double nilaiBiayas = myLabel.getValue();
					nilaiBiayaHarusDiBayars += (myLabel.getValue() == null ? 0.0 : nilaiBiayas);

				}
			}
			labelFooter2.setStyle("text-align: right;");
			labelFooter2.setValue(Common.numberFormat.get().format(nilaiBiayaHarusDiBayars));
		}
	}

}
