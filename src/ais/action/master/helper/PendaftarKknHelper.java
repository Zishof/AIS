package ais.action.master.helper;


import ais.common.CommonSearchFilterHelper;
import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.ss.usermodel.Hyperlink;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFColor;
import org.zkoss.poi.xssf.usermodel.XSSFFont;
import org.zkoss.poi.xssf.usermodel.XSSFHyperlink;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;

import ais.action.master.kkn.KknUntukMahasiswaAction;
import ais.action.report.Report;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.common.PesanFormalHelper;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Kkn;
import ais.database.model.Konfigurasi;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranKknMahasiswa;
import ais.database.model.kkn.KknPunyaPersyaratan;
import ais.database.model.kkn.MahasiswaDaftarKkn;
import ais.database.model.kkn.MahasiswaKknPersyaratan;
import ais.database.model.kkn.PersyaratanKkn;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Composer ZK untuk layar "Daftar Mahasiswa Pendaftar KKN": menampilkan grid pendaftar satu
 * {@link Kkn} beserta SKS/IPS/IPK, total skor seleksi, status memenuhi syarat, dan checkbox
 * penerimaan (dapat diedit hanya bila {@code approve=true}). Menyediakan pencarian/filter
 * (NIM/nama, fakultas, jurusan, angkatan, hanya-yang-belum-diterima), tombol cetak kartu pendaftar
 * per mahasiswa, unggah/unduh data massal via Excel (xlsx), penghitungan ulang skor seleksi
 * berdasarkan komponen bertipe {@code PILIHAN_CUSTOM}, serta tiga laporan cetak (daftar pendaftar,
 * daftar penerima, rekap penerima).
 *
 * <p>
 * Implementasi {@link DataLoader} ({@link #loadData(Object)}) dan {@link DataCriteria}
 * ({@link #initCriteria(boolean)}) memungkinkan kelas ini dipakai ulang sebagai sumber data oleh
 * komponen lain (mis. tombol "Baru" pada {@link AmbilDataMahasiswaSeleksiKknHelper}). Ekspor Excel
 * pada {@link #cetakDataCustomButton} berjalan di thread terpisah dengan progress bar berbasis
 * polling {@link org.zkoss.zul.Timer}, menuliskan kolom dinamis sesuai daftar
 * {@link PersyaratanKkn} milik KKN terkait (termasuk link unduhan lampiran bila persyaratan
 * mewajibkan lampiran). Unggah Excel ({@link #displayPrasyaratKkn}) mem-parsing ulang baris
 * berdasarkan NIM dan kolom "Diterima", membuat baris {@link MahasiswaDaftarKkn} baru bila belum
 * ada.
 *
 * <h3>Posisi dalam alur KKN</h3>
 * <p>
 * Kelas ini adalah layar <b>pendaftaran/seleksi</b>, satu tahap sebelum pembagian kelompok:
 * <ol>
 *   <li>mahasiswa (atau petugas) mendaftar lewat
 *   {@code ais.action.master.kkn.KknUntukMahasiswaAction} — di sanalah kolom
 *   {@code memenuhiSyarat} dihitung dan disimpan;</li>
 *   <li>panitia menyeleksi di layar ini ({@code SeleksiPenerimaKknAction} &rarr; helper ini),
 *   menetapkan {@code totalSkor} dan {@code terima};</li>
 *   <li>mahasiswa yang diterima dibagi ke kelompok lewat {@link KelompokKknHelper} dan
 *   {@link ais.database.model.kkn.MahasiswaDapatKelompokKkn}.</li>
 * </ol>
 * Pengecualian per-mahasiswa (mahasiswa yang dibebaskan dari sebagian syarat) dikelola terpisah
 * lewat {@link PengecualianKknMahasiswaHelper}.
 *
 * <h3>Catatan otorisasi</h3>
 * <p>
 * Hak {@code APPROVE} dari menu diteruskan pemanggil sebagai parameter {@code approve} dan hanya
 * dipakai untuk menonaktifkan checkbox "Terima" pada grid. Tiga jalur lain yang juga mengubah data
 * penerimaan tidak memeriksanya sama sekali: tombol "Upload" (impor Excel menulis kolom
 * {@code terima}), tombol hapus per baris, dan tombol "Hitung Skor" (menulis {@code totalSkor}).
 * Kelas ini juga tidak melakukan pemeriksaan cakupan satuan kerja/tenant atas {@link #kkn} yang
 * diterimanya — lihat catatan pada field {@link #kkn}.
 *
 * <h3>Hubungan dengan kembarannya di modul PKL</h3>
 * <p>
 * {@code PendaftarPklHelper} adalah salinan kelas ini untuk modul PKL. Keduanya sejajar baris demi
 * baris pada hampir seluruh isinya, namun terdapat tiga perbedaan nyata yang <b>bukan</b> sekadar
 * penggantian nama entitas — semuanya membuat sisi PKL lebih lemah:
 * <ul>
 *   <li>kelas ini mengisi label kolom "Memenuhi Syarat" lewat
 *   {@link MahasiswaDaftarKkn#getMemenuhiSyarat()}, sedangkan kembarannya membuat label itu tetapi
 *   tidak pernah mengisinya, sehingga kolom tersebut selalu kosong di layar PKL;</li>
 *   <li>pencarian {@link MahasiswaKknPersyaratan} saat ekspor di sini memakai
 *   {@code addOrder(desc(id))} + {@code setMaxResults(1)}, sedangkan kembarannya memanggil
 *   {@code uniqueResult()} tanpa pembatas sehingga data rangkap memicu pengecualian;</li>
 *   <li>tombol "Rekap" di sini mencetak laporan {@code penerima_kkn} yang tersedia, sedangkan
 *   kembarannya menunjuk nama laporan yang tidak ada di direktori laporan.</li>
 * </ul>
 *
 * <h3>Catatan: syarat akademik tidak dievaluasi di kelas ini</h3>
 * <p>
 * Ambang SKS/IPK KKN — termasuk pasangan syarat kedua {@code minimalSksBolehIkutKkn2} /
 * {@code minimalIpkBolehIkutKkn2} yang dikendalikan sakelar "Aktifkan Syarat Lain" pada
 * {@link Kkn} — <b>tidak</b> dievaluasi di sini. Kelas ini hanya <i>menampilkan</i> hasil evaluasi
 * yang tersimpan pada kolom {@code memenuhiSyarat}. Jebakan konfigurasi pada pasangan syarat kedua
 * (getter mengembalikan 0 / 0.0 saat kolomnya {@code null}, padahal nilai awal field-nya 110 / 2.0,
 * sehingga mengaktifkan "Syarat Lain" tanpa mengisi angkanya justru <i>melonggarkan</i> syarat)
 * terdokumentasi pada {@link Kkn} dan berlaku bagi jalur yang menghitung {@code memenuhiSyarat},
 * bukan bagi kelas ini.
 *
 * @see PendaftarPklHelper
 * @see PengecualianKknMahasiswaHelper
 * @see KelompokKknHelper
 * @see AmbilDataMahasiswaSeleksiKknHelper
 */
public class PendaftarKknHelper implements DataLoader, DataCriteria {

	/**
	 * Grid utama berisi baris {@link MahasiswaDaftarKkn} hasil {@link #initCriteria(boolean)}.
	 * Dibuat sekali di {@link #displayPrasyaratKkn}, lalu di-render ulang setiap
	 * {@link #loadData(Object)} memakai {@link PendaftarKknRenderer}.
	 */
	private MyGrid grid;

	/**
	 * KKN yang sedang dikelola. Menjadi filter wajib pada seluruh query kelas ini
	 * ({@code Restrictions.eq("kkn", kkn)}) sekaligus penentu daftar
	 * {@link PersyaratanKkn} yang dipakai sebagai kolom dinamis ekspor Excel.
	 *
	 * <p>
	 * Objek ini diterima apa adanya dari pemanggil
	 * ({@code ais.action.master.kkn.SeleksiPenerimaKknAction}); kelas ini <b>tidak</b> memeriksa
	 * ulang kepemilikan/cakupan satuan kerja atas KKN tersebut — pembatasan cakupan sepenuhnya
	 * menjadi tanggung jawab Action pemanggil.
	 * </p>
	 */
	private Kkn kkn;

	/**
	 * Kotak isian kata kunci pencarian pendaftar. Nilainya dicocokkan sekaligus ke NIM
	 * <b>atau</b> nama mahasiswa (LIKE {@code MatchMode.ANYWHERE}, case-insensitive) pada
	 * {@link #initCriteria(boolean)}; kosong berarti tanpa penyaringan.
	 */
	private Textbox nim;

	/** Combobox filter fakultas. Bila tidak ada pilihan aktif, kriteria hanya mensyaratkan {@code jurusan.fakultas} tidak null (mahasiswa tanpa fakultas ikut tersaring keluar). */
	private Combobox fakultas;

	/** Combobox filter jurusan/program studi. Bila tidak ada pilihan aktif, kriteria hanya mensyaratkan mahasiswa punya jurusan. */
	private Combobox jurusan;

	/** Kontrol paging 50 baris per halaman untuk {@link #grid}; halaman aktifnya dibaca ulang setiap {@link #loadData(Object)}. */
	private Paging paging;

	/**
	 * Penanda apakah pengguna saat ini boleh mengubah status penerimaan langsung dari grid.
	 * Diisi pemanggil dari hak {@code CommonPrivilages.APPROVE}.
	 *
	 * <p>
	 * <b>Cakupan terbatas:</b> flag ini hanya menonaktifkan checkbox "Terima" pada
	 * {@link PendaftarKknRenderer}. Tombol "Upload" (impor Excel) yang dipasang di
	 * {@link #displayPrasyaratKkn} tetap tampil dan tetap menulis kolom {@code terima}
	 * tanpa membaca flag ini, demikian pula tombol hapus per baris.
	 * </p>
	 */
	private boolean approve;

	/** Filter tahun angkatan mahasiswa; kosong ({@code null}) berarti semua angkatan. */
	private Intbox angkatan;

	/** Checkbox "Belum diterima": bila dicentang, kriteria dipersempit ke {@code terima = 0} ({@link MahasiswaDaftarKkn#BELUM_DIPROSES}) saja. Perubahan centangnya langsung memicu {@link #loadData(Object)}. */
	private MyCheckboxConfig hanyaYgBelumDiterima;

	/**
	 * Perender satu baris grid pendaftar KKN. Setiap baris berisi sembilan sel berurutan sesuai
	 * kolom yang dipasang {@link PendaftarKknHelper#displayPrasyaratKkn}: NIM, nama, jurusan,
	 * SKS/SKSK, IP/IPK, skor seleksi, status "Memenuhi Syarat", checkbox penerimaan, dan
	 * kelompok tombol cetak kartu/ubah/hapus.
	 *
	 * <p>
	 * Nilai SKS dan IP/IPK tidak diambil dari kolom tersimpan, melainkan dihitung ulang setiap
	 * render lewat {@link Common#singkronkanKrsMahasiswa} untuk semester yang diturunkan dari
	 * tahun akademik dan semester {@link Kkn} terkait — sehingga angkanya selalu mencerminkan
	 * KRS terkini, bukan kondisi saat mahasiswa mendaftar.
	 * </p>
	 */
	class PendaftarKknRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Merender satu baris pendaftar KKN ke dalam {@code row}.
		 *
		 * <p>
		 * Beberapa perilaku yang perlu diperhatikan:
		 * </p>
		 * <ul>
		 *   <li><b>Penghitungan semester berbiaya.</b> Semester akademik mahasiswa dihitung dari
		 *   tahun angkatan, semester/tahun akademik KKN, dan riwayat pindah kampus, lalu dipakai
		 *   memanggil {@link Common#singkronkanKrsMahasiswa} — pemanggilan ini dapat menulis ke
		 *   basis data (sinkronisasi KRS), jadi render grid tidak sepenuhnya bebas efek samping.</li>
		 *   <li><b>Kolom "Memenuhi Syarat"</b> hanya menampilkan nilai kolom tersimpan
		 *   {@link MahasiswaDaftarKkn#getMemenuhiSyarat()}; kolom tersebut ditulis di tempat lain
		 *   ({@code ais.action.master.kkn.KknUntukMahasiswaAction#daftar}) dan tidak dihitung ulang
		 *   di sini. Baris yang dibuat lewat impor Excel pada
		 *   {@link PendaftarKknHelper#displayPrasyaratKkn} tidak pernah mengisi kolom ini,
		 *   sehingga selalu tampil "Tidak".</li>
		 *   <li><b>Checkbox "Terima"</b> langsung menyimpan perubahan ke basis data pada tiap klik
		 *   ({@link Common#refreshUpdate}) tanpa dialog konfirmasi maupun jejak audit; ia hanya
		 *   dinonaktifkan (bukan disembunyikan) bila {@link PendaftarKknHelper#approve} bernilai
		 *   {@code false}.</li>
		 *   <li><b>Tombol hapus</b> menghapus baris pendaftaran secara permanen tanpa memeriksa
		 *   {@link PendaftarKknHelper#approve}; kegagalan karena relasi ditangani dengan pesan
		 *   kesalahan, bukan pencegahan.</li>
		 * </ul>
		 *
		 * @param row  baris ZK tujuan; sel ditambahkan berurutan sebagai anak {@code row}
		 * @param data elemen model, harus berupa {@link MahasiswaDaftarKkn}
		 * @throws Exception bila render sel atau pemanggilan sinkronisasi KRS gagal
		 */
		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final MahasiswaDaftarKkn mahasiswaDaftarKkn = (MahasiswaDaftarKkn) data;

			final Mahasiswa mahasiswa = mahasiswaDaftarKkn.getMahasiswa();

			new Label(mahasiswa.getNim()).setParent(row);
			new Label(mahasiswa.getNama()).setParent(row);
			new Label(mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getNama()).setParent(row);

			Integer tahunAngkatanMhs = mahasiswa.getTahunangkatan();
			String semesterMulai = mahasiswaDaftarKkn.getKkn().getSemester();
			String ta = mahasiswaDaftarKkn.getKkn().getTahunAkademik();
			Integer tahun = Integer.parseInt(StringUtils.split(ta, "/")[0]);
			Integer semester = Common.getSemester(tahunAngkatanMhs, semesterMulai,
					mahasiswa.getPindahKeKampusIniMasukSemester(), tahun, mahasiswa.getSemesterMulai());

			KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, semester, null, null);
			new Label(Common.numberFormat.get().format(krsMahasiswa.getSksYangDiambil()) + " / "
					+ Common.numberFormat.get().format(krsMahasiswa.getSksk())).setParent(row);
			new Label(Common.numberFormat.get().format(krsMahasiswa.getIps()) + " / "
					+ Common.numberFormat.get().format(krsMahasiswa.getIpk())).setParent(row);

			new Label(Common.numberFormat.get().format(mahasiswaDaftarKkn.getTotalSkor())).setParent(row);

			Label labelmemenuhiSyarat = new Label();
			labelmemenuhiSyarat.setParent(row);

			labelmemenuhiSyarat.setValue(mahasiswaDaftarKkn.getMemenuhiSyarat() ? "Ya" : "Tidak");

			final MyCheckboxConfig labelTelahTerpenuhi = new MyCheckboxConfig("Terima");
			labelTelahTerpenuhi.setDisabled(!approve);
			labelTelahTerpenuhi.setParent(row);
			labelTelahTerpenuhi.setChecked(mahasiswaDaftarKkn.getTerima().equals(MahasiswaDaftarKkn.DITERIMA));

			labelTelahTerpenuhi.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					mahasiswaDaftarKkn.setTerima(labelTelahTerpenuhi.isChecked() ? MahasiswaDaftarKkn.DITERIMA
							: MahasiswaDaftarKkn.BELUM_DIPROSES);
					Common.refreshUpdate(mahasiswaDaftarKkn);
				}
			});

			Hbox toolbar = new Hbox();
			toolbar.setParent(row);

			MyToolbarbuttonConfig cetak = new MyToolbarbuttonConfig("", "/img/print.png");
			cetak.setOrient("vertical");
			cetak.addEventListener("onClick", new EventListener() {

				@SuppressWarnings({})
				@Override
				public void onEvent(Event arg0) throws Exception {
					// TODO Auto-generated method stub
					Map<String, Long> parameters = new HashMap<String, Long>();
					parameters.put("id_mahasiswa", mahasiswa.getId());
					parameters.put("id_kkn", kkn.getId());
					mahasiswa.putPhoto(parameters);
					Report.generatePDFReport(Report.PDF, parameters, "kartu_daftar_kkn",
							ais.ui.util.WaktuUtil.getDate());
				}
			});
			cetak.setParent(toolbar);

			final MyToolbarbuttonConfig buttonEdit = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			buttonEdit.setTooltiptext("Ubah Data");
			buttonEdit.setParent(toolbar);
			buttonEdit.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					KknUntukMahasiswaAction.onAddExternal(event, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							loadData(null);
						}
					}, mahasiswaDaftarKkn.getKkn(), mahasiswa);
				}

			});

			final MyToolbarbuttonConfig buttonDelete = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			buttonDelete.setTooltiptext("Hapus Data");
			buttonDelete.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Common.refreshDelete(HibernateUtil.currentSession(), mahasiswaDaftarKkn);

											loadData(null);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(
													"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
															+ e.getMessage());
										}

									}

								}
							});

				}
			});
			buttonDelete.setParent(toolbar);

		}

	}

	/**
	 * Membangun kriteria Hibernate {@link MahasiswaDaftarKkn} untuk KKN yang sedang ditampilkan,
	 * memfilter sesuai status penerimaan, angkatan, NIM/nama, dan jurusan/fakultas yang dipilih
	 * pada toolbar.
	 *
	 * <p>
	 * Struktur kriteria: kriteria akar dibatasi {@code kkn = }{@link #kkn} dan (opsional)
	 * {@code terima = 0}, lalu berpindah ke sub-kriteria {@code mahasiswa} tempat filter
	 * angkatan/NIM/nama/jurusan dipasang, dan terakhir membuka sub-kriteria {@code jurusan}
	 * secara {@code LEFT_JOIN} untuk filter fakultas. Karena {@code addOrder} dipanggil setelah
	 * berpindah ke sub-kriteria {@code mahasiswa}, pengurutan berlaku atas kolom mahasiswa
	 * ({@code tahunangkatan}, {@code nim}), bukan kolom pendaftaran.
	 *
	 * <p>
	 * Cabang "tanpa filter" diwujudkan dengan {@code Restrictions.sqlRestriction("true")} —
	 * predikat yang selalu benar — sehingga rantai {@code add(...)} tetap seragam. Perlu dicatat
	 * bahwa cabang default filter jurusan/fakultas bukan "tanpa syarat" melainkan
	 * {@code isNotNull}: mahasiswa yang belum punya jurusan (atau jurusannya belum punya
	 * fakultas) tidak akan pernah muncul di grid ini meski baris pendaftarannya ada.
	 *
	 * <p>
	 * Kriteria ini <b>tidak</b> menambahkan pembatasan cakupan satuan kerja/tenant apa pun di
	 * luar penyaringan per-{@link #kkn}; lihat catatan pada field {@link #kkn}.
	 *
	 * @param order bila {@code true}, menambahkan pengurutan tahun angkatan menurun lalu NIM menaik
	 * @return kriteria siap dieksekusi (belum dibatasi jumlah baris)
	 */
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(MahasiswaDaftarKkn.class).add(Restrictions.eq("kkn", kkn))

				.add(hanyaYgBelumDiterima.isChecked() ? Restrictions.eq("terima", 0)
						: Restrictions.sqlRestriction("true"))

				.createCriteria("mahasiswa")

				.add(angkatan.getValue() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("tahunangkatan", angkatan.getValue()))

				.add(nim.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.ilike("nim", nim.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("nama", nim.getValue().trim(), MatchMode.ANYWHERE)))

				.add(jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null
						? Restrictions.isNotNull("jurusan")
						: CommonSearchFilterHelper.eqSelectedWithId("jurusan", jurusan, false));

		if (order)
			criteria.addOrder(Order.desc("tahunangkatan")).addOrder(Order.asc("nim"));

		criteria.createCriteria("jurusan", Criteria.LEFT_JOIN)
				.add(fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null
						? Restrictions.isNotNull("fakultas")
						: CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));

		return criteria;
	}

	/**
	 * Memuat ulang halaman aktif grid pendaftar: menghitung ulang total baris untuk
	 * {@link #paging}, mengambil maksimum {@code Common.ROWS_COUNT_ON_PAGE_50} baris pada offset
	 * halaman aktif, lalu memasang model beserta {@link PendaftarKknRenderer} yang baru.
	 *
	 * <p>
	 * Implementasi kontrak {@link DataLoader#loadData(Object)} sehingga instance ini dapat
	 * diserahkan sebagai callback penyegaran kepada
	 * {@link AmbilDataMahasiswaSeleksiKknHelper} (lihat {@link #getDataloader()}).
	 * {@link #initCriteria(boolean)} sengaja dipanggil dua kali — sekali untuk pencacahan tanpa
	 * pengurutan, sekali untuk pengambilan data dengan pengurutan — karena satu objek
	 * {@link Criteria} tidak dapat dipakai ulang setelah diberi proyeksi pencacahan.
	 *
	 * @param value tidak dipakai; ada hanya untuk memenuhi tanda tangan {@link DataLoader}
	 */
	@SuppressWarnings("unchecked")
	public void loadData(Object value) {

		Common.initPaging50(initCriteria(false), paging);

		List<MahasiswaDaftarKkn> mahasiswaDaftarKkn = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE_50)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE_50 * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(mahasiswaDaftarKkn);
		grid.setRowRenderer(new PendaftarKknRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Menyediakan instance ini sebagai {@link DataLoader} untuk diserahkan ke dialog pemilihan
	 * mahasiswa ({@link AmbilDataMahasiswaSeleksiKknHelper}), agar dialog tersebut dapat memicu
	 * {@link #loadData(Object)} dan menyegarkan grid setelah pendaftar baru ditambahkan.
	 *
	 * <p>
	 * Method pembungkus ini diperlukan karena {@code this} di dalam kelas anonim
	 * {@code EventListener} merujuk ke listener, bukan ke helper.
	 *
	 * @return objek helper ini sendiri
	 */
	private DataLoader getDataloader() {
		return this;
	}

	/**
	 * Membuat tombol toolbar yang mengekspor hasil {@code dataCriteria} ke berkas Excel (xlsx)
	 * dengan kolom dinamis mengikuti daftar {@link PersyaratanKkn} milik KKN ini (termasuk
	 * hyperlink lampiran bila persyaratan mewajibkannya), lalu membuka pratinjau hasilnya dalam
	 * {@link org.zkoss.zss.ui.Spreadsheet} di window modal dengan opsi unduh.
	 *
	 * <p>
	 * <b>Struktur kolom.</b> Tujuh kolom tetap di depan — ID, NIM, Nama, Jurusan, Fakultas,
	 * Diterima, Skor — diikuti satu kolom per {@link PersyaratanKkn} (diurutkan menurut nama lalu
	 * label inputan). Isi kolom persyaratan mengikuti
	 * {@link PersyaratanKkn#getTipeDataInputan()}: teks/teks-angka dan pilihan custom memakai
	 * {@code nilaiString}, tanggal memakai {@code nilaiTanggal}, angka memakai {@code nilaiNumber},
	 * dan ya/tidak memakai {@code nilaiBoolean}. Tipe yang tidak dikenali menuliskan nama
	 * persyaratannya sendiri sebagai isi sel, bukan nilai jawaban mahasiswa.
	 *
	 * <p>
	 * <b>Efek samping penulisan data.</b> Selama ekspor, setiap kombinasi mahasiswa-persyaratan
	 * yang belum punya baris {@link MahasiswaKknPersyaratan} akan <i>dibuat</i> (kosong) dan
	 * disimpan. Jadi tombol "Download" bukan operasi baca-saja: menekannya dapat menambah baris
	 * jawaban kosong ke basis data. Pencarian baris memakai {@code addOrder(desc(id))} +
	 * {@code setMaxResults(1)} sehingga aman terhadap data rangkap.
	 *
	 * <p>
	 * <b>Model konkurensi.</b> Pembuatan berkas berjalan di {@link Thread} terpisah, sementara
	 * thread ZK memantau kemajuannya lewat {@link org.zkoss.zul.Timer} 200&nbsp;ms yang membaca
	 * teks sebuah {@link Label} sebagai kanal status: teks kosong berarti selesai (buka pratinjau),
	 * teks {@code "-"} berarti gagal. Konsekuensinya, thread latar memakai
	 * {@link HibernateUtil#currentSession()} di luar konteks permintaan ZK dan
	 * {@link StreamingHibernateUtil} untuk pembacaan lampiran; kegagalan per-sel ditelan dan
	 * hanya ditampilkan bila pengguna berstatus admin.
	 *
	 * <p>
	 * Batas ekspor adalah 1.048.576 baris (batas baris satu lembar xlsx), dan berkas ditulis ke
	 * folder {@code /tmp} aplikasi web dengan nama bercap waktu.
	 *
	 * @param dataCriteria sumber data yang diekspor (kriteria diminta dengan pengurutan aktif);
	 *                     boleh berupa {@code this} atau penyedia {@link DataCriteria} lain
	 * @param buttonLabel  label tombol
	 * @param buttonImage  path ikon tombol
	 * @return tombol toolbar siap ditambahkan ke {@link org.zkoss.zul.Toolbar}
	 */
	@SuppressWarnings("unchecked")
	public MyToolbarbuttonConfig cetakDataCustomButton(final DataCriteria dataCriteria, String buttonLabel,
			String buttonImage) {

		Session session = HibernateUtil.currentSession();
		final List<PersyaratanKkn> persyaratanKkns = session.createCriteria(KknPunyaPersyaratan.class)
				.createAlias("persyaratanKkn", "persyaratanKkn").add(Restrictions.eq("kkn", kkn))
				.setProjection(Projections.property("persyaratanKkn")).addOrder(Order.asc("persyaratanKkn.nama"))
				.addOrder(Order.asc("persyaratanKkn.labelInputan")).list();

		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig(buttonLabel, buttonImage);

		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));
				final Intbox intbox = new Intbox(10);
				Clients.showBusy(label.getValue());

				final String filename = Sessions.getCurrent().getWebApp()
						.getRealPath("/tmp/cetak_data_"
								+ URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
								+ ".xlsx");
				final File file;
				(file = new File(filename)).createNewFile();

				final Timer timer = new Timer(200);
				timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				timer.setRepeats(true);
				timer.addEventListener("onTimer", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						try {

							Clients.showBusy(label.getValue());
							System.out.println("label " + label.getValue());

							if (label.getValue().trim().equalsIgnoreCase("-")) {
								Clients.clearBusy();
								timer.detach();
							} else if (label.getValue().isEmpty()) {

								Center center = new Center();
								final MyWindow window = new MyWindow("Cetak Data", "none", true);
								window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
								window.setHeight("97%");
								window.setWidth("90%");

								Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
								borderlayout.setParent(window);

								ais.ui.util.ZkCompat.setFlex(center, true);
								center.setParent(borderlayout);

								System.out.println("loading file " + file.getAbsolutePath());
								Common.clear(center);
								Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
				Common.clear(center);spreadsheet.setParent(center);
								spreadsheet.setWidth("100%");
								spreadsheet.setHeight("100%");
								spreadsheet.setSrc("../../tmp/" + file.getName());
								spreadsheet.setMaxrows(intbox.getValue() + 1);
								spreadsheet.setMaxcolumns(persyaratanKkns.size() + 7);
								ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);

								South south = new South();
								south.setParent(borderlayout);

								Toolbar toolbar = new Toolbar();
								// toolbar.setHeight("25px");
								toolbar.setParent(south);
								MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
								cancel.setTooltiptext("Tutup");
								cancel.addEventListener("onClick", new EventListener() {
									@Override
									public void onEvent(Event event) throws Exception {
										window.detach();
									}
								});
								cancel.setParent(toolbar);

								MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Download Data",
										"/img/excel.png");
								print.addEventListener("onClick", new EventListener() {
									@Override
									public void onEvent(Event event) throws Exception {

										try {
											Filedownload.save(new FileInputStream(file), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
													file.getName());
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PendaftarKknHelper.java:366");

										}
									}
								});
								print.setParent(toolbar);

								window.setVisible(true);
								window.onModal();

								Clients.clearBusy();
								timer.detach();
							}

						} catch (Exception e) {
							Clients.clearBusy();
						}

					}
				});
				timer.start();

				try {

					Clients.showBusy(label.getValue());

					new Thread(new Runnable() {

						@Override
						public void run() {

							try {
								Object d = dataCriteria == null ? null : dataCriteria.initCriteria(true);
								@SuppressWarnings("rawtypes")
								List<MahasiswaDaftarKkn> data = (d != null && d instanceof Criteria)
										? ((Criteria) d).setMaxResults(1048576).list()
										: (List) d;
								intbox.setValue(data.size());
								System.out.println("data = " + data.size());

								XSSFWorkbook workbook = new XSSFWorkbook();

								XSSFCellStyle lockedNumericStyle = workbook.createCellStyle();
								lockedNumericStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
								lockedNumericStyle.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));
								// lockedNumericStyle.setLocked(true);

								XSSFCellStyle hlink_style = workbook.createCellStyle();
								XSSFFont hlink_font = workbook.createFont();
								hlink_font.setUnderline(XSSFFont.U_SINGLE);
								hlink_font.setColor(new XSSFColor(Color.BLUE));
								hlink_style.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
								hlink_style.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));
								hlink_style.setFont(hlink_font);

								XSSFCellStyle notLocked = workbook.createCellStyle();
								notLocked.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
								notLocked.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));
								// notLocked.setLocked(false);

								XSSFSheet sheet = workbook.createSheet("CETAK DATA");
								// sheet.protectSheet("passwordrahasia");
								sheet.setDefaultColumnWidth(20);
								int rowIndex = 0;

								XSSFRow rowhead = sheet.createRow((short) 0);

								rowhead.createCell(0).setCellValue("ID");

								rowhead.createCell(1).setCellValue("NIM");
								rowhead.createCell(2).setCellValue("Nama");
								rowhead.createCell(3).setCellValue("Jurusan");
								rowhead.createCell(4).setCellValue("Fakultas");
								rowhead.createCell(5).setCellValue("Diterima");
								rowhead.createCell(6).setCellValue("Skor");

								for (int i = 7; i < persyaratanKkns.size() + 7; i++) {
									PersyaratanKkn persyaratanKkn = persyaratanKkns.get(i - 7);
									if (persyaratanKkn.getLabelInputan() == null
											|| persyaratanKkn.getLabelInputan().trim().isEmpty()) {
										rowhead.createCell(i).setCellValue(persyaratanKkn.getNama());
									} else {
										rowhead.createCell(i).setCellValue(persyaratanKkn.getLabelInputan());

									}
								}

								for (MahasiswaDaftarKkn o : data) {

									try {
										rowIndex++;
										if (o == null) {
											continue;
										}
										Mahasiswa mahasiswa = o.getMahasiswa();
										label.setValue("Sedang memproses data " + o.toString() + " ("
												+ Common.numberFormat.get().format(rowIndex * 100.0 / data.size()) + " %)");

										XSSFRow row = sheet.createRow(rowIndex);
										XSSFCell cell = row.createCell(0);
										cell.setCellStyle(lockedNumericStyle);
										cell.setCellValue(o.getId());

										cell = row.createCell(1);
										cell.setCellStyle(lockedNumericStyle);
										cell.setCellValue(mahasiswa.getNim());

										cell = row.createCell(2);
										cell.setCellStyle(lockedNumericStyle);
										cell.setCellValue(mahasiswa.getNama());

										cell = row.createCell(3);
										cell.setCellStyle(lockedNumericStyle);
										cell.setCellValue(mahasiswa.getJurusan().getNama());

										cell = row.createCell(4);
										cell.setCellStyle(lockedNumericStyle);
										cell.setCellValue(mahasiswa.getJurusan().getFakultas().getNama());

										cell = row.createCell(5);
										cell.setCellStyle(notLocked);
										cell.setCellValue(o.getTerima().equals(1));

										cell = row.createCell(6);
										cell.setCellStyle(lockedNumericStyle);
										cell.setCellValue(o.getTotalSkor());

										Session session = HibernateUtil.currentSession();
										for (int i = 7; i < persyaratanKkns.size() + 7; i++) {

											try {
												PersyaratanKkn persyaratanKkn = persyaratanKkns.get(i - 7);
												MahasiswaKknPersyaratan mahasiswaKknPersyaratan = (MahasiswaKknPersyaratan) session
														.createCriteria(MahasiswaKknPersyaratan.class)
														.add(Restrictions.eq("mahasiswa", mahasiswa))
														.add(Restrictions.eq("kkn", kkn))
														.addOrder(Order.desc("id")).setMaxResults(1)
														.add(Restrictions.eq("persyaratanKkn", persyaratanKkn))
														.uniqueResult();
												if (mahasiswaKknPersyaratan == null) {
													mahasiswaKknPersyaratan = new MahasiswaKknPersyaratan();
													mahasiswaKknPersyaratan.setMahasiswa(mahasiswa);
													mahasiswaKknPersyaratan.setKkn(kkn);
													mahasiswaKknPersyaratan.setPersyaratanKkn(persyaratanKkn);
													session.save(mahasiswaKknPersyaratan);
												}

												if (persyaratanKkn.getTipeDataInputan().equals(PersyaratanKkn.TEXT)
														|| persyaratanKkn.getTipeDataInputan()
																.equals(PersyaratanKkn.TEXT_ANGKA)) {
													cell = row.createCell(i);
													cell.setCellStyle(lockedNumericStyle);
													if (mahasiswaKknPersyaratan.getNilaiString() != null) {
														cell.setCellValue(mahasiswaKknPersyaratan.getNilaiString());
													}
												} else if (persyaratanKkn.getTipeDataInputan()
														.equals(PersyaratanKkn.TANGGAL)) {
													cell = row.createCell(i);
													cell.setCellStyle(lockedNumericStyle);
													cell.setCellValue(
															mahasiswaKknPersyaratan.getNilaiTanggal() == null ? ""
																	: Common.dateFormat1.get().format(
																			mahasiswaKknPersyaratan.getNilaiTanggal()));

												} else if (persyaratanKkn.getTipeDataInputan()
														.equals(PersyaratanKkn.ANGKA)) {
													cell = row.createCell(i);
													cell.setCellStyle(lockedNumericStyle);
													if (mahasiswaKknPersyaratan.getNilaiNumber() != null) {
														cell.setCellValue(mahasiswaKknPersyaratan.getNilaiNumber());
													}

												} else if (persyaratanKkn.getTipeDataInputan()
														.equals(PersyaratanKkn.PILIHAN_YA_TIDAK)) {
													cell = row.createCell(i);
													cell.setCellStyle(lockedNumericStyle);
													if (mahasiswaKknPersyaratan.getNilaiBoolean() != null) {
														cell.setCellValue(mahasiswaKknPersyaratan.getNilaiBoolean());
													}

												} else if (persyaratanKkn.getTipeDataInputan()
														.equals(PersyaratanKkn.PILIHAN_CUSTOM)) {
													cell = row.createCell(i);
													cell.setCellStyle(lockedNumericStyle);
													if (mahasiswaKknPersyaratan.getNilaiString() != null) {
														cell.setCellValue(mahasiswaKknPersyaratan.getNilaiString());
													}

												} else {
													cell = row.createCell(i);
													if (persyaratanKkn.getLabelInputan() == null
															|| persyaratanKkn.getLabelInputan().trim().isEmpty()) {
														cell.setCellValue(persyaratanKkn.getNama());
													} else {
														cell.setCellValue(persyaratanKkn.getLabelInputan());

													}
												}

												if (persyaratanKkn.getHarusMenyertakanLampiran()) {
													cell.setCellStyle(hlink_style);

													try {
														Session streamingSession = StreamingHibernateUtil.getInstance()
																.currentSession();
														int jumlah = ((Number) streamingSession
																.createCriteria(LampiranKknMahasiswa.class)
																.setProjection(Projections.rowCount())
																.add(Restrictions.eq("persyaratanKkn",
																		mahasiswaKknPersyaratan.getId()))
																.setMaxResults(1).uniqueResult()).intValue();

														Long ids = (Long) (streamingSession
																.createCriteria(LampiranKknMahasiswa.class)
																.setProjection(Projections.property("id"))
																.add(Restrictions.eq("persyaratanKkn",
																		mahasiswaKknPersyaratan.getId()))
																.setMaxResults(1).uniqueResult());

														String url = CommonMedia.getFile(ids,
																LampiranKknMahasiswa.class.getName());

														if (jumlah > 0) {
															XSSFHyperlink link = row.getSheet().getWorkbook().getCreationHelper().createHyperlink(Hyperlink.LINK_URL);
															link.setAddress(url);
															cell.setHyperlink(link);
														}
													} catch (Exception e) {
														StreamingHibernateUtil.getInstance().rollbackTransaction();
													}

													StreamingHibernateUtil.getInstance().closeSession();

												}
											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
											}
										}

									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
									}
								}

								try {
									FileOutputStream fileOut = new FileOutputStream(filename);
									workbook.write(fileOut);
									fileOut.close();
								} catch (IOException e) {
									// TODO Auto-generated catch block
									Common.tampilErrorJikaAdmin(e);
								}
								System.out.println(
										"Your excel file has been generated! " );
								data.clear();
								data = null;
								label.setValue("");
							} catch (Exception e) {
								label.setValue("-");
							}

						}
					}).start();

				} catch (Exception e) {
					// TODO Auto-generated catch block
					Common.tampilErrorJikaAdmin(e);
					PesanFormalHelper.tampilkanGagalException(
							"mencetak/mengekspor data pendaftar KKN ke Excel",
							e, new String[] {
									"Muat ulang (refresh) halaman ini lalu coba cetak data kembali.",
									"Periksa apakah jumlah data yang akan diekspor tidak terlalu besar.",
									"Apabila kendala masih berlanjut, hubungi Admin dengan menyertakan tangkapan layar (screenshot) pesan ini."
							});
				}
			}
		});

		return toolbarbutton;
	}

	/**
	 * Membangun seluruh UI layar pendaftar KKN di dalam {@code component} yang diberikan, lalu
	 * memuat data awal.
	 *
	 * <p>
	 * Isi toolbar yang dipasang, berurutan: kotak cari NIM/nama, combobox fakultas dan jurusan,
	 * isian angkatan, checkbox "Belum diterima", tombol "Cari", tombol "Pengecualian"
	 * ({@link PengecualianKknMahasiswaHelper} — hanya tampil bagi pengguna non-mahasiswa dan bila
	 * konfigurasi {@code tampilkan_pengecualian_kkn_mahasiswa_di_seleksi} aktif), tiga tombol
	 * cetak PDF ("Pendaftar" &rarr; {@code pendaftar_kkn}, "Penerima" &rarr;
	 * {@code pendaftar_kkn_diterima}, "Rekap" &rarr; {@code penerima_kkn}), tombol "Hitung Skor",
	 * tombol "Baru" ({@link AmbilDataMahasiswaSeleksiKknHelper}), tombol "Download"
	 * ({@link #cetakDataCustomButton}), dan tombol "Upload".
	 *
	 * <p>
	 * <b>"Hitung Skor"</b> menghitung ulang {@link MahasiswaDaftarKkn#setTotalSkor} untuk seluruh
	 * baris hasil filter saat ini. Skor dijumlahkan dari jawaban bertipe
	 * {@link PersyaratanKkn#PILIHAN_CUSTOM} yang nilainya berformat {@code "label:skor"}; segmen
	 * setelah titik dua di-parse sebagai bilangan bulat, dan jawaban yang tidak berformat
	 * demikian dihitung nol tanpa peringatan ke pengguna. Karena berbasis
	 * {@link #initCriteria(boolean)}, tombol ini hanya menghitung ulang baris yang <i>sedang
	 * lolos filter</i>, bukan seluruh pendaftar KKN ini.
	 *
	 * <p>
	 * <b>"Upload" (impor Excel)</b> membaca kembali berkas berformat sama dengan hasil ekspor:
	 * kolom 0 = ID baris, kolom 1 = NIM, kolom 5 = status diterima. Baris dicocokkan lewat ID
	 * bila ada; bila tidak, baris {@link MahasiswaDaftarKkn} <i>baru</i> dibuat berdasarkan NIM
	 * lalu {@code terima} diisi dari kolom 5. Perlu diperhatikan:
	 * <ul>
	 *   <li>jalur ini melewati {@code KknUntukMahasiswaAction#daftar}, sehingga
	 *   {@code memenuhiSyarat} tidak pernah diisi dan pemeriksaan kuota/persyaratan akademik
	 *   tidak dijalankan;</li>
	 *   <li>tombol ini dipasang tanpa memeriksa parameter {@code approve}, sehingga status
	 *   penerimaan dapat ditulis lewat unggahan berkas walaupun checkbox "Terima" pada grid
	 *   dinonaktifkan;</li>
	 *   <li>setiap baris di-commit satu per satu di thread terpisah — kegagalan di tengah berkas
	 *   meninggalkan sebagian data sudah tersimpan.</li>
	 * </ul>
	 *
	 * @param kkn        KKN yang pendaftarnya ditampilkan
	 * @param component  container ZK yang akan diisi (dibersihkan lebih dulu)
	 * @param window     window pemanggil (dipakai sebagai parent dialog "Ambil Data" baru)
	 * @param approve    bila {@code true}, checkbox "Terima" pada tiap baris dapat diedit; lihat
	 *                   catatan cakupan pada field {@link #approve}
	 */
	public void displayPrasyaratKkn(final Kkn kkn, final Component component, final MyWindow window, boolean approve) {
		this.kkn = kkn;
		this.approve = approve;
		Common.clear(component);

		paging = new Paging();
		Common.initPaging50(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		Common.initFakultasDanJurusanDanSemua(fakultas = new Combobox(), jurusan = new Combobox(), null, null);

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(component);
		groupbox.appendChild(new MyCaptionStyled("Daftar mahasiswa yang mendaftar kkn"));

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);
		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Mhs : ")));
		toolbar.appendChild(nim = new Textbox());
		nim.setWidth("");
		nim.setWidth("70px");
		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Fakultas")));
		toolbar.appendChild(fakultas);
		fakultas.setWidth("70px");

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Jurusan")));
		toolbar.appendChild(jurusan);
		jurusan.setWidth("70px");

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Angkatan")));
		toolbar.appendChild(angkatan = new Intbox());
		angkatan.setWidth("50px");

		toolbar.appendChild(hanyaYgBelumDiterima = new MyCheckboxConfig("Belum diterima"));
		hanyaYgBelumDiterima.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}

		});
		button.setParent(toolbar);

		Tbmuser tbmuser = Common.getCurrentUser();

		MyToolbarbuttonConfig pengecualian = new MyToolbarbuttonConfig("Pengecualian", "/img/svg/edit-box-line.svg");
		toolbar.appendChild(pengecualian);
		pengecualian.setVisible(tbmuser.getMahasiswa() == null
				&& Common.bolehKonfigurasi("tampilkan_pengecualian_kkn_mahasiswa_di_seleksi"));
		pengecualian.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				PengecualianKknMahasiswaHelper pengecualianKknMahasiswaHelper = new PengecualianKknMahasiswaHelper(kkn);
				pengecualianKknMahasiswaHelper.display();
			}
		});

		button = new MyToolbarbuttonConfig("Pendaftar", "/img/print.png");
		button.addEventListener("onClick", new EventListener() {
			final Map<String, Long> parameters = new HashMap<String, Long>();

			@Override
			public void onEvent(Event arg0) throws Exception {

				Integer countPendaftar = ((Number) HibernateUtil.currentSession()
						.createCriteria(MahasiswaDaftarKkn.class).add(Restrictions.eq("kkn", kkn))
						.setProjection(Projections.rowCount()).uniqueResult()).intValue();

				if (countPendaftar == 0) {
					MyMessageboxConfig.show("Tidak Ada Pendaftar", "Informasi", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}

				parameters.put("id_kkn", kkn.getId());
				// parameters
				// .put("jurusan", fakultas.getSelectedItem().getValue());
				// parameters.put("fakultas", fakultas.getSelectedItem()
				// .getValue());
				Report.generatePDFReport(Report.PDF, parameters, "pendaftar_kkn", ais.ui.util.WaktuUtil.getDate());
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Penerima", "/img/print.png");
		button.addEventListener("onClick", new EventListener() {
			final Map<String, Long> parameters = new HashMap<String, Long>();

			@Override
			public void onEvent(Event arg0) throws Exception {

				Integer countPenerima = ((Number) HibernateUtil.currentSession()
						.createCriteria(MahasiswaDaftarKkn.class).add(Restrictions.eq("kkn", kkn))
						.add(Restrictions.eq("terima", 1)).setProjection(Projections.rowCount()).uniqueResult())
								.intValue();

				if (countPenerima == 0) {
					MyMessageboxConfig.show("Tidak Ada Mahasiswa yang Diterima di Kkn Ini", "Informasi",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return;
				}

				parameters.put("id_kkn", kkn.getId());
				Report.generatePDFReport(Report.PDF, parameters, "pendaftar_kkn_diterima",
						ais.ui.util.WaktuUtil.getDate());
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Rekap", "/img/print.png");
		button.addEventListener("onClick", new EventListener() {
			final HashMap<String, Long> parameters = new HashMap<String, Long>();

			@Override
			public void onEvent(Event arg0) throws Exception {

				Integer countPenerima = ((Number) HibernateUtil.currentSession()
						.createCriteria(MahasiswaDaftarKkn.class).add(Restrictions.eq("kkn", kkn))
						.add(Restrictions.eq("terima", 1)).setProjection(Projections.rowCount()).uniqueResult())
								.intValue();

				if (countPenerima == 0) {
					MyMessageboxConfig.show("Tidak Ada Mahasiswa yang Diterima di Kkn Ini", "Informasi",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return;
				}

				parameters.put("id_kkn", kkn.getId());
				Report.generatePDFReport(Report.PDF, parameters, "penerima_kkn", ais.ui.util.WaktuUtil.getDate());
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Hitung Skor", "/img/excel.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.createDefaultTimer(new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event arg0) throws Exception {
						List<MahasiswaDaftarKkn> mahasiswaDaftarKkns = initCriteria(true).list();
						Session session = HibernateUtil.currentSession();
						for (MahasiswaDaftarKkn mahasiswaDaftarKkn : mahasiswaDaftarKkns) {

							List<MahasiswaKknPersyaratan> mahasiswaKknPersyaratans = session
									.createCriteria(MahasiswaKknPersyaratan.class)
									.add(Restrictions.eq("mahasiswa", mahasiswaDaftarKkn.getMahasiswa()))
									.add(Restrictions.eq("kkn", mahasiswaDaftarKkn.getKkn()))
									.createAlias("persyaratanKkn", "persyaratanKkn").add(Restrictions
											.eq("persyaratanKkn.tipeDataInputan", PersyaratanKkn.PILIHAN_CUSTOM))
									.list();
							Integer totalSkor = 0;
							for (MahasiswaKknPersyaratan mahasiswaKknPersyaratan : mahasiswaKknPersyaratans) {
								String val = mahasiswaKknPersyaratan.getNilaiString() == null ? ""
										: mahasiswaKknPersyaratan.getNilaiString().trim();
								String[] kol = StringUtils.split(val, ":");
								Integer skor = 0;
								try {
									skor = Integer.parseInt(kol[1].trim());
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PendaftarKknHelper.java:824");

								}
								totalSkor += skor;
							}
							mahasiswaDaftarKkn.setTotalSkor(totalSkor);

							Common.refreshSaveOrUpdate(session, mahasiswaDaftarKkn);
						}
						loadData(null);
					}
				});
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Baru", "/img/new.gif");
		button.addEventListener(Events.ON_CLICK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// TODO Auto-generated method stub
				try {
					new AmbilDataMahasiswaSeleksiKknHelper().display(kkn, getDataloader(), window);
				} catch (Exception e) {
					ais.common.ErrorAuditUtil.record(e, "PendaftarKknHelper membuka dialog tambah pendaftar KKN");
					Common.tampilErrorJikaAdmin(e);
					MyMessageboxConfig.show("Form tambah pendaftar KKN belum dapat dibuka. Silakan coba kembali.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				}
			}
		});
		button.setParent(toolbar);

		MyToolbarbuttonConfig cetakToolbarbutton = cetakDataCustomButton(this, "Download", "/img/excel.png");
		toolbar.appendChild(cetakToolbarbutton);

		MyToolbarbuttonConfig upload = new MyToolbarbuttonConfig("Upload" + Common.ukuranLabelFileUpload(),
				"/img/excel.png");
		upload.setUpload(Common.ukuranFileUpload());
		upload.addEventListener("onUpload", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				UploadEvent uploadEvent = (UploadEvent) event;
				Media media = uploadEvent.getMedia();if(!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))return;
				if (media.getName().toLowerCase().endsWith("xlsx")) {

					InputStream inputStream = media.getStreamData();
					// System.out.println("media = " + media);
					final File file = new File(
							Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
					// System.out.println("file = " + file.getAbsolutePath());
					file.getParentFile().mkdirs();
					FileOutputStream fileOutputStream = new FileOutputStream(file);
					int c;
					while ((c = inputStream.read()) != -1) {
						fileOutputStream.write(c);
					}
					fileOutputStream.close();
					inputStream.close();

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							final Label peringatan = new Label("");

							final Label label = new Label(ais.common.Common.getBahasaConfig("Proses upload data data .."));
							Clients.showBusy(label.getValue());
							final Timer timer = new Timer(200);
							timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
							timer.setRepeats(true);
							timer.addEventListener("onTimer", new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									Clients.showBusy(label.getValue());
									if (label.getValue().isEmpty()) {
										System.out.println("loading file " + file.getAbsolutePath());
										MyMessageboxConfig.show("Upload data berhasil dilakukan."
												+ (peringatan.getValue().isEmpty() ? "" : "\n" + peringatan.getValue()),
												"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
												new EventListener() {

													@Override
													public void onEvent(Event arg0) throws Exception {
														loadData(null);
													}
												});
										Clients.clearBusy();
										timer.detach();
									}

								}
							});
							timer.start();

							new Thread(new Runnable() {

								@Override
								public void run() {
									try {

									try {

										XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
										XSSFSheet sheet = workbook.getSheetAt(0);

										Session session = HibernateUtil.currentNativeSession();
										int rowCount = (sheet.getLastRowNum() + 1);
										for (int i = 1; i < rowCount; i++) {
											try {

												Long id = Common.getSheetContentAsLong(sheet, 0, i);
												String nim = Common.getSheetContentAsString(sheet, 1, i);
												Boolean diterima = Common.getSheetContentAsBoolean(sheet, 5, i);
												if (nim == null || nim.trim().isEmpty()
														|| diterima == null) {
													continue;
												}

												Mahasiswa mahasiswa = ConstantValues.ambilByNim(nim);
												if (mahasiswa == null) {
													continue;
												}

												MahasiswaDaftarKkn biodataCalonMahasiswa = (MahasiswaDaftarKkn) (id == null
														? null
														: session.createCriteria(MahasiswaDaftarKkn.class)
																.add(Restrictions.idEq(id)).uniqueResult());
												if (biodataCalonMahasiswa == null) {
													biodataCalonMahasiswa = new MahasiswaDaftarKkn();
													biodataCalonMahasiswa.setNama(nim); 
													biodataCalonMahasiswa.setMahasiswa(mahasiswa);
													biodataCalonMahasiswa.setKkn(kkn);
													biodataCalonMahasiswa.setTanggalDaftar(new Date());
												}

												biodataCalonMahasiswa.setTerima(diterima ? 1 : 0);

												session.getTransaction().begin();
												session.saveOrUpdate(biodataCalonMahasiswa);
												session.getTransaction().commit();

												label.setValue("Upload data \"" + biodataCalonMahasiswa.getNama()
														+ "\" (" + Common.numberFormat.get().format(i * 100.0 / rowCount)
														+ " %)");

											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
											}

										}
									} catch (Exception e1) {
										// TODO Auto-generated catch block
										e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/helper/PendaftarKknHelper.java:975");
									}

									HibernateUtil.closeSession();

									label.setValue("");
																	} finally {
										ais.database.hibernate.HibernateUtil.closeSession();
									}
								}
							}).start();

						}
					}, "Harap tunggu.. sedang melakukan proses upload data..");

				} else {
					MyMessageboxConfig.show(
							"File yang anda upload harus ber-format Excel Open XML Spreadsheet (xlsx). Jika masih menggunakan format lain, buka file excel tersebut, kemudian Save As Excel Open XML Spreadsheet (xlsx). "
									+ media,
							"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
				}
			}
		});
		toolbar.appendChild(upload);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);grid.getPagingChild().setMold("os");
		grid.setParent(groupbox);

		paging.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("NIM");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jurusan");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("SKS/SKSK");
		column.setWidth("7%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("IP/IPk");
		column.setWidth("7%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Skor");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Memenuhi Syarat");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Terima/Tidak");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Ubah/Hapus");
		column.setWidth("10%");

		loadData(null);
		// borderlayout.setParent(component);

	}

	/**
	 * Mengubah status penerimaan ({@code terima}) baris pendaftaran mahasiswa pada KKN tertentu
	 * (mengambil baris terbaru bila ada lebih dari satu) dan menampilkan pesan konfirmasi.
	 *
	 * <p>
	 * <b>Tidak ada pemanggil di dalam basis kode saat ini</b> — method ini tersedia sebagai API
	 * bantu bagi layar lain, dan sengaja dipertahankan apa adanya.
	 *
	 * <p>
	 * Perilaku yang perlu diketahui bila hendak dipakai: method ini <i>tidak</i> memeriksa
	 * {@link #approve} maupun hak apa pun, dan hasil query tidak diperiksa {@code null} sehingga
	 * mahasiswa yang belum pernah mendaftar KKN ini menyebabkan
	 * {@link NullPointerException}, bukan pesan kesalahan yang ramah. Parameter {@code kkn}
	 * bersifat lokal dan tidak mengubah field {@link #kkn}.
	 *
	 * @param mahasiswa mahasiswa yang statusnya diubah
	 * @param kkn       KKN terkait
	 * @param checked   {@code true} untuk menandai diterima, {@code false} untuk ditolak
	 * @throws Exception bila penyimpanan gagal
	 */
	public void terimaKkn(Mahasiswa mahasiswa, Kkn kkn, boolean checked) throws Exception {
		Session session = HibernateUtil.currentSession();
		MahasiswaDaftarKkn mahasiswaDiterimaKknIni = (MahasiswaDaftarKkn) session
				.createCriteria(MahasiswaDaftarKkn.class).add(Restrictions.eq("mahasiswa", mahasiswa))
				.add(Restrictions.eq("kkn", kkn)).setMaxResults(1).addOrder(Order.desc("id")).uniqueResult();

		if (checked) {

			mahasiswaDiterimaKknIni.setTerima(1);
			Common.refreshUpdate(session, mahasiswaDiterimaKknIni);
			MyMessageboxConfig.show("Mahasiswa " + mahasiswa.getNama() + " / " + mahasiswa.getNim()
					+ " diterima untuk kkn " + kkn.getNama(), "INFORMASI", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return;

		}

		if (!checked) {
			mahasiswaDiterimaKknIni.setTerima(0);
			Common.refreshUpdate(session, mahasiswaDiterimaKknIni);
			MyMessageboxConfig.show("Mahasiswa " + mahasiswa.getNama() + " / " + mahasiswa.getNim()
					+ " ditolak untuk kkn " + kkn.getNama(), "INFORMASI", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return;
		}

	}

}
