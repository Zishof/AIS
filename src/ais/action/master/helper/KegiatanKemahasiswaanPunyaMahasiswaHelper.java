package ais.action.master.helper;
import ais.common.PesanFormalHelper;

import java.awt.Color;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.poi.ss.usermodel.Hyperlink;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFColor;
import org.zkoss.poi.xssf.usermodel.XSSFFont;
import org.zkoss.poi.xssf.usermodel.XSSFHyperlink;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.JurusanAction;
import ais.action.master.LogLoginAction;
import ais.action.master.SertifikatAction;
import ais.common.Common;
import ais.common.CommonSearchFilterHelper;
import ais.common.CommonPrivilages;
import ais.common.Html2Text;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.DetailKelompokKegiatanKemahasiswaan;
import ais.database.model.DspaceInformation;
import ais.database.model.Fakultas;
import ais.database.model.JabatanKegiatanKemahasiswaan;
import ais.database.model.Jurusan;
import ais.database.model.KegiatanKemahasiswaan;
import ais.database.model.KegiatanKemahasiswaanPunyaMahasiswa;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.PrestasiMahasiswa;
import ais.database.model.SkalaKegiatanKemahasiswaan;
import ais.database.model.StatusMahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.ui.dspace.DspaceCommon;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Layar/komponen ZK (bukan Action, dipanggil dari {@code KegiatanKemahasiswaanAction} lewat
 * {@link #display}) yang mengelola DAFTAR PESERTA (baris {@link KegiatanKemahasiswaanPunyaMahasiswa})
 * dari satu {@link KegiatanKemahasiswaan} (organisasi/kegiatan kemahasiswaan) — mis. mahasiswa
 * yang mengikuti UKM/organisasi tertentu, dengan jabatan &amp; skala kegiatan masing-masing.
 * Mengimplementasikan kontrak generik {@link DataLoader}/{@link DataCriteria}/
 * {@link DataSearchDefault} agar bisa dipakai lewat helper cetak/upload Excel generik
 * ({@code Common.cetakDataCustomButton}/{@code Common.uploadData}).
 *
 * <p><b>Alur utama:</b> grid berpaging menampilkan peserta dengan filter (nama/NIM, angkatan,
 * fakultas, jurusan, {@link #searchmahasiswa Bandbox mahasiswa}, dan status persetujuan peserta
 * lewat {@link #searchPersetujuan}/{@link #persetujuanTerpilih()}). Tiap baris dirender oleh
 * {@link DetailKegiatanKemahasiswaanRenderer}: field jabatan/skala/keterangan/tanggal mulai-sampai
 * bisa diedit inline (auto-save {@code onChange}) selama peserta belum {@code persetujuan}
 * (disetujui); admin (bukan {@code tbmuser.getMahasiswa()}) bisa mencentang persetujuan langsung
 * dari grid bila status {@link KegiatanKemahasiswaan} sudah {@code DISETUJUI}. Peserta yang belum
 * disetujui bisa dihapus satu-per-satu atau massal ("Bersihkan" — DELETE SQL langsung untuk semua
 * baris {@code persetujuan IS NULL OR FALSE} pada kegiatan ini).</p>
 *
 * <p><b>Integrasi repository (DSpace/OJS).</b> Bila konfigurasi
 * {@code kegiatan_mahasiswa_terhubung_ke_dspace} aktif, peserta yang sudah disetujui (dan punya
 * jurusan) bisa diekspor/dibatalkan-ekspor ke repository DSpace sebagai item (lewat
 * {@link #getDspace}), di bawah hierarki community-per-jurusan
 * ({@link #getDspaceTipeKegiatanKemahasiswaanPunyaMahasiswaJurusan}) dan collection-per-kegiatan
 * ({@link #getDspaceTipeKegiatanKemahasiswaanPunyaMahasiswa}) yang dibuat otomatis via API DSpace
 * bila belum ada (di-cache id-nya di {@link Konfigurasi} per kombinasi kegiatan+jurusan).</p>
 *
 * <p><b>Bukan tanggung jawab kelas ini:</b> pencarian/pengambilan mahasiswa BARU sebagai calon
 * peserta (didelegasikan ke {@code AmbilDataMahasiswaForKegiatanKemahasiswaanHelper} lewat tombol
 * "Ambil Mahasiswa"), maupun pembuatan sertifikat (didelegasikan ke
 * {@code SertifikatAction.cetakSertifikat}).</p>
 */
public class KegiatanKemahasiswaanPunyaMahasiswaHelper implements DataLoader, DataCriteria, DataSearchDefault {

	/** Grid ZK yang menampilkan daftar peserta; baris dirender oleh {@link DetailKegiatanKemahasiswaanRenderer}, diisi ulang oleh {@link #loadData}. */
	private MyGrid grid;
	/** Kegiatan/organisasi kemahasiswaan yang pesertanya sedang dikelola layar ini; diset sekali di {@link #display}. */
	private KegiatanKemahasiswaan kegiatanKemahasiswaan;
	/** Kotak isian filter nama/NIM peserta. */
	private Textbox nama;
	/** Kotak isian filter tahun angkatan peserta. */
	private Intbox angkatan;

	/** Filter fakultas peserta (default "Semua" — lihat catatan di {@link #display} kenapa TIDAK dikunci ke fakultas kegiatan). */
	private Combobox searchfakultas = new Combobox();
	/** Filter jurusan/prodi peserta (default "Semua", diisi SELURUH jurusan bukan hanya jurusan kegiatan). */
	private Combobox searchjurusan = new Combobox();
	/** Filter status persetujuan PESERTA (Semua / Disetujui / Belum) — melengkapi filter status kegiatan. */
	private Combobox searchPersetujuan = new Combobox();

	/** Komponen paging grid; event pindah halaman memicu {@link #loadData}. */
	private Paging paging;
	/** Pengguna yang sedang login — dipakai membedakan tampilan admin (bisa menyetujui/menghapus) vs. mahasiswa sendiri. */
	private Tbmuser tbmuser;
	/** Bandbox pencarian satu mahasiswa spesifik sebagai filter tambahan. */
	private AmbilDataMahasiswaBanbox searchmahasiswa;

	/** Menyiapkan combobox fakultas/jurusan (opsi "Semua" + seluruh data), menentukan pengguna login, dan memasang komponen {@link #paging} beserta listener pindah halamannya (memicu {@link #loadData}). Belum memuat data apa pun — pemuatan data terjadi saat {@link #display} dipanggil. */
	public KegiatanKemahasiswaanPunyaMahasiswaHelper() {

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		tbmuser = Common.getCurrentUser();

		paging = new Paging();
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

	}

	/**
	 * Renderer baris grid peserta — satu instance dibuat per pemuatan data ({@link #loadData}),
	 * dipakai ulang untuk SEMUA baris dalam halaman tsb. Konstruktor mengambil daftar pilihan
	 * {@link JabatanKegiatanKemahasiswaan}/{@link SkalaKegiatanKemahasiswaan} SATU KALI dari
	 * {@link DetailKelompokKegiatanKemahasiswaan} induk kegiatan (bukan per-baris, untuk efisiensi
	 * — combo pilihan sama untuk semua peserta kegiatan yang sama), dan mengambil hak akses hapus
	 * ({@link CommonPrivilages#DELETE}) sekali di awal.
	 * <p>
	 * {@link #render} menghasilkan per baris: link "Revisi" (riwayat Envers, lewat
	 * {@link RevisiHelper#createNewRevisi}), upload/download bukti kegiatan
	 * ({@link LampiranLain#createDownloadUploadFileLain}), nama/angkatan/jurusan mahasiswa,
	 * STATUS MAHASISWA TERKINI (dihitung on-the-fly via
	 * {@link HistoryStatusMahasiswaUtil#currentStatus(Mahasiswa)} — bukan disimpan di baris
	 * peserta), field editable (keterangan, tanggal mulai/sampai, jabatan, skala — auto-save
	 * {@code onChange} lewat {@code Common.refreshUpdate}, DINONAKTIFKAN begitu peserta sudah
	 * {@code persetujuan}), checkbox persetujuan (HANYA untuk pengguna non-mahasiswa/admin DAN
	 * hanya bila status {@link KegiatanKemahasiswaan} sudah {@code DISETUJUI} — selain itu label
	 * teks read-only "Ya"/"Belum"), tombol Sertifikat (tampil bila sudah disetujui DAN kegiatan
	 * punya template sertifikat), dan tombol Hapus (tampil bila peserta BELUM disetujui DAN
	 * pengguna punya hak {@code DELETE}, dengan konfirmasi dan pesan error ramah bila gagal karena
	 * relasi data lain).
	 *
	 * @see KegiatanKemahasiswaanPunyaMahasiswaHelper
	 */
	class DetailKegiatanKemahasiswaanRenderer extends ais.ui.util.MyRowRenderer {

		/** {@code true} bila pengguna login punya hak hapus — dicek sekali di constructor, dipakai untuk visibilitas tombol Hapus di setiap baris. */
		private boolean delete = false;
		/** Pilihan jabatan yang berlaku untuk kelompok kegiatan induk (diurutkan), dipakai mengisi combo jabatan tiap baris. */
		private List<JabatanKegiatanKemahasiswaan> jabatanKegiatanKemahasiswaans;
		/** Pilihan skala kegiatan yang berlaku untuk kelompok kegiatan induk (diurutkan), dipakai mengisi combo skala tiap baris. */
		private List<SkalaKegiatanKemahasiswaan> skalaKegiatanKemahasiswaans;

		/** Memuat sekali daftar pilihan jabatan &amp; skala dari {@link DetailKelompokKegiatanKemahasiswaan} induk kegiatan, dan hak hapus pengguna login — dipanggil sekali per pemuatan grid, bukan per baris. */
		public DetailKegiatanKemahasiswaanRenderer() {
			DetailKelompokKegiatanKemahasiswaan detailKelompokKegiatanKemahasiswaan = (DetailKelompokKegiatanKemahasiswaan) HibernateUtil
					.currentSession().createCriteria(DetailKelompokKegiatanKemahasiswaan.class)
					.add(Restrictions.idEq(kegiatanKemahasiswaan.getDetailKelompokKegiatanKemahasiswaan().getId()))
					.uniqueResult();
			jabatanKegiatanKemahasiswaans = new ArrayList<JabatanKegiatanKemahasiswaan>(
					detailKelompokKegiatanKemahasiswaan.getJabatanKegiatanKemahasiswaans());
			skalaKegiatanKemahasiswaans = new ArrayList<SkalaKegiatanKemahasiswaan>(
					detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans());

			Collections.sort(jabatanKegiatanKemahasiswaans);
			Collections.sort(skalaKegiatanKemahasiswaans);
			delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		}

		/**
		 * Merender satu baris {@link KegiatanKemahasiswaanPunyaMahasiswa} sesuai deskripsi
		 * lengkap di Javadoc kelas {@link DetailKegiatanKemahasiswaanRenderer} — lihat di sana
		 * untuk rincian kolom dan aturan visibilitas/edit.
		 *
		 * @param row  baris grid ZK tujuan
		 * @param data instance {@link KegiatanKemahasiswaanPunyaMahasiswa} yang dirender
		 */
		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final KegiatanKemahasiswaanPunyaMahasiswa kegiatanKemahasiswaanPunyaMahasiswa = (KegiatanKemahasiswaanPunyaMahasiswa) data;

			MyDetail detail = new MyDetail();
			detail.setParent(row);
			detail.setOpen(true);

			RevisiHelper.createNewRevisi(KegiatanKemahasiswaanPunyaMahasiswa.class, kegiatanKemahasiswaanPunyaMahasiswa,
					kegiatanKemahasiswaanPunyaMahasiswa.getMahasiswa().getNim()).setParent(row);

			new Label(kegiatanKemahasiswaanPunyaMahasiswa.getMahasiswa().getNama()).setParent(row);

			Vbox vbox = new Vbox();
			vbox.setParent(detail);
			Hbox hbox = new Hbox();

			LampiranLain.createDownloadUploadFileLain(hbox, kegiatanKemahasiswaanPunyaMahasiswa.getId(),
					KegiatanKemahasiswaanPunyaMahasiswa.class.getName(), "Bukti Kegiatan Kemahasiswaan", false,
					new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

						}
					}, null, false, false, false, true);

			hbox.setParent(vbox);

			new Label(kegiatanKemahasiswaanPunyaMahasiswa.getMahasiswa().getTahunangkatan() + "").setParent(row);

			StatusMahasiswa statusMahasiswa = ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(kegiatanKemahasiswaanPunyaMahasiswa.getMahasiswa())
					.getStatusMahasiswa();
			new Label(statusMahasiswa.getNama()).setParent(row);

			new Label(kegiatanKemahasiswaanPunyaMahasiswa.getMahasiswa().getJurusan() == null ? ""
					: kegiatanKemahasiswaanPunyaMahasiswa.getMahasiswa().getJurusan().getNama() + "").setParent(row);

			final MyTextbox keterangan = new MyTextbox(kegiatanKemahasiswaanPunyaMahasiswa.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setRows(2);

			final MyDatebox mulai = new MyDatebox(kegiatanKemahasiswaanPunyaMahasiswa.getMulai());
			mulai.setWidth("90%");
			final MyDatebox sampai = new MyDatebox(kegiatanKemahasiswaanPunyaMahasiswa.getSampai());
			sampai.setWidth("90%");

			mulai.setParent(row);
			sampai.setParent(row);

			final Combobox jabatanKegiatanKemahasiswaan = new Combobox();
			jabatanKegiatanKemahasiswaan.setVisible(!jabatanKegiatanKemahasiswaans.isEmpty());
			Common.insertComboItems(jabatanKegiatanKemahasiswaan, "nama", jabatanKegiatanKemahasiswaans);
			Common.selectComboItem(true, jabatanKegiatanKemahasiswaan,
					kegiatanKemahasiswaanPunyaMahasiswa.getJabatanKegiatanKemahasiswaan());
			jabatanKegiatanKemahasiswaan.setParent(row);
			jabatanKegiatanKemahasiswaan.setReadonly(true);
			jabatanKegiatanKemahasiswaan.setWidth("97%");

			final Combobox skalaKegiatanKemahasiswaan = new Combobox();
			skalaKegiatanKemahasiswaan.setVisible(!skalaKegiatanKemahasiswaans.isEmpty());
			Common.insertComboItems(skalaKegiatanKemahasiswaan, "nama", skalaKegiatanKemahasiswaans);
			Common.selectComboItem(true, skalaKegiatanKemahasiswaan,
					kegiatanKemahasiswaanPunyaMahasiswa.getSkalaKegiatanKemahasiswaan());
			skalaKegiatanKemahasiswaan.setParent(row);
			skalaKegiatanKemahasiswaan.setReadonly(true);
			skalaKegiatanKemahasiswaan.setWidth("97%");

			EventListener eventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					kegiatanKemahasiswaanPunyaMahasiswa.setMulai(mulai.getValue());
					kegiatanKemahasiswaanPunyaMahasiswa.setSampai(sampai.getValue());
					kegiatanKemahasiswaanPunyaMahasiswa.setSkalaKegiatanKemahasiswaan(
							(SkalaKegiatanKemahasiswaan) (skalaKegiatanKemahasiswaan.getSelectedItem() == null ? null
									: skalaKegiatanKemahasiswaan.getSelectedItem().getValue()));
					kegiatanKemahasiswaanPunyaMahasiswa.setKeterangan(keterangan.getValue());
					kegiatanKemahasiswaanPunyaMahasiswa.setJabatanKegiatanKemahasiswaan(
							((JabatanKegiatanKemahasiswaan) (jabatanKegiatanKemahasiswaan.getSelectedItem() == null
									? null
									: jabatanKegiatanKemahasiswaan.getSelectedItem().getValue())));
					Common.refreshUpdate(kegiatanKemahasiswaanPunyaMahasiswa);

				}
			};

			skalaKegiatanKemahasiswaan.addEventListener("onChange", eventListener);
			jabatanKegiatanKemahasiswaan.addEventListener("onChange", eventListener);
			keterangan.addEventListener("onChange", eventListener);
			mulai.addEventListener("onChange", eventListener);
			sampai.addEventListener("onChange", eventListener);
			keterangan.setParent(row);

			jabatanKegiatanKemahasiswaan.setDisabled(kegiatanKemahasiswaanPunyaMahasiswa.getPersetujuan());
			skalaKegiatanKemahasiswaan.setDisabled(kegiatanKemahasiswaanPunyaMahasiswa.getPersetujuan());
			keterangan.setDisabled(kegiatanKemahasiswaanPunyaMahasiswa.getPersetujuan());
			mulai.setDisabled(kegiatanKemahasiswaanPunyaMahasiswa.getPersetujuan());
			sampai.setDisabled(kegiatanKemahasiswaanPunyaMahasiswa.getPersetujuan());

			final MyToolbarbuttonConfig cetakToolbarbuttonSertifikat = new MyToolbarbuttonConfig("Sertifikat",
					"/img/certificate-icon.png");
			cetakToolbarbuttonSertifikat.setOrient("vertical");
			final MyToolbarbuttonConfig deleteButton = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
			deleteButton.setOrient("vertical");

			cetakToolbarbuttonSertifikat.setVisible(kegiatanKemahasiswaanPunyaMahasiswa.getPersetujuan()
					&& kegiatanKemahasiswaanPunyaMahasiswa.getKegiatanKemahasiswaan().getSertifikat() != null);

			Hbox toolbar = new Hbox();
			deleteButton.setVisible(!kegiatanKemahasiswaanPunyaMahasiswa.getPersetujuan());
			if (tbmuser.getMahasiswa() == null
					&& kegiatanKemahasiswaan.getStatus().equals(PrestasiMahasiswa.DISETUJUI)) {
				final MyCheckboxConfig checkbox = new MyCheckboxConfig("Setujui");
				checkbox.setChecked(kegiatanKemahasiswaanPunyaMahasiswa.getPersetujuan());
				checkbox.setParent(row);
				row.setValign("top");row.setAttribute("checkbox", checkbox);
				checkbox.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						kegiatanKemahasiswaanPunyaMahasiswa.setPersetujuan(checkbox.isChecked());
						Common.refreshSaveOrUpdate(kegiatanKemahasiswaanPunyaMahasiswa);
						deleteButton.setVisible(!kegiatanKemahasiswaanPunyaMahasiswa.getPersetujuan());

						cetakToolbarbuttonSertifikat.setVisible(kegiatanKemahasiswaanPunyaMahasiswa.getPersetujuan()
								&& kegiatanKemahasiswaanPunyaMahasiswa.getKegiatanKemahasiswaan()
										.getSertifikat() != null);

						jabatanKegiatanKemahasiswaan.setDisabled(kegiatanKemahasiswaanPunyaMahasiswa.getPersetujuan());
						skalaKegiatanKemahasiswaan.setDisabled(kegiatanKemahasiswaanPunyaMahasiswa.getPersetujuan());
						keterangan.setDisabled(kegiatanKemahasiswaanPunyaMahasiswa.getPersetujuan());
						mulai.setDisabled(kegiatanKemahasiswaanPunyaMahasiswa.getPersetujuan());
						sampai.setDisabled(kegiatanKemahasiswaanPunyaMahasiswa.getPersetujuan());
					}
				});
			} else {
				Label label;
				(label = new Label(kegiatanKemahasiswaanPunyaMahasiswa.getPersetujuan() == null
						|| kegiatanKemahasiswaanPunyaMahasiswa.getPersetujuan() ? "Ya" : "Belum")).setParent(row);
				label.setStyle(label.getValue().equals("Belum") ? "color:red;" : "color:blue");
				label.setParent(row);
			}

			cetakToolbarbuttonSertifikat.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					SertifikatAction.cetakSertifikat(kegiatanKemahasiswaanPunyaMahasiswa);
				}
			});
			cetakToolbarbuttonSertifikat.setParent(toolbar);

			deleteButton.setOrient("vertical");
			deleteButton.setVisible(delete);
			deleteButton.setTooltiptext("Hapus Data");
			deleteButton.addEventListener("onClick", new EventListener() {
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

											Common.refreshDelete(kegiatanKemahasiswaanPunyaMahasiswa);
											loadData(null);

										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											PesanFormalHelper.tampilkanGagalException("Menghapus data", "Data yang Bapak/Ibu coba hapus kemungkinan besar masih memiliki keterkaitan/relasi dengan data lain pada tabel terkait (misalnya digunakan sebagai referensi oleh transaksi, detail, atau riwayat lain), sehingga sistem basis data menolak proses penghapusan ini demi menjaga integritas data secara keseluruhan.", e, new String[]{"Periksa kembali apakah data ini masih digunakan atau direferensikan oleh data lain yang berelasi.", "Hapus atau lepaskan terlebih dahulu keterkaitan/relasi data tersebut sebelum mencoba menghapus data ini kembali.", "Jika Bapak/Ibu yakin data ini seharusnya sudah tidak digunakan lagi, hubungi Administrator untuk pengecekan lebih lanjut."});
										}

									}

								}
							});

				}

			});
			deleteButton.setParent(toolbar);
			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(row);

		}

	}

	/** Nilai filter persetujuan peserta yang dipilih: {@code null}=Semua, TRUE=Disetujui, FALSE=Belum. */
	private Boolean persetujuanTerpilih() {
		if (searchPersetujuan == null || searchPersetujuan.getSelectedItem() == null) {
			return null;
		}
		Object v = searchPersetujuan.getSelectedItem().getValue();
		return v instanceof Boolean ? (Boolean) v : null;
	}

	/**
	 * Implementasi kontrak {@link DataCriteria}: membangun {@link Criteria} Hibernate untuk
	 * {@link KegiatanKemahasiswaanPunyaMahasiswa} milik {@link #kegiatanKemahasiswaan} ini,
	 * digabung filter dari toolbar (mahasiswa spesifik via {@link #searchmahasiswa}, jurusan,
	 * fakultas, angkatan, nama/NIM {@code ilike}, dan {@link #persetujuanTerpilih()} — "Belum"
	 * mencakup {@code NULL} maupun {@code FALSE}, lihat komentar inline). Dipakai baik untuk
	 * hitung total ({@link #loadData}, {@code order=false}) maupun ambil data terurut NIM
	 * ({@code order=true}), dan dipakai ulang oleh fitur cetak/upload Excel generik serta ekspor
	 * DSpace.
	 *
	 * @param order {@code true} untuk menambahkan {@code ORDER BY mahasiswa.nim ASC}
	 * @return Criteria siap dieksekusi/di-paging oleh pemanggil
	 */
	public Criteria initCriteria(boolean order) {

		Mahasiswa mahasiswa = (Mahasiswa) searchmahasiswa.getAttribute("mahasiswa");

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(KegiatanKemahasiswaanPunyaMahasiswa.class);

		criteria.createAlias("mahasiswa", "mahasiswa")

				.add(mahasiswa != null ? Restrictions.eq("mahasiswa.id", mahasiswa.getId())
						: Restrictions.sqlRestriction("1=1"))

				.createAlias("mahasiswa.jurusan", "jurusan")

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: CommonSearchFilterHelper.eqSelectedWithId("mahasiswa.jurusan", searchjurusan, false))

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", searchfakultas, false))

				.add(angkatan.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("mahasiswa.tahunangkatan", angkatan.getValue()))

				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(
								Restrictions.ilike("mahasiswa.nim", nama.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("mahasiswa.nama", nama.getValue().trim(), MatchMode.ANYWHERE)))
				// Filter status persetujuan PESERTA. "Belum" mencakup nilai null (belum diproses).
				.add(persetujuanTerpilih() == null ? Restrictions.sqlRestriction("1=1")
						: (Boolean.TRUE.equals(persetujuanTerpilih()) ? Restrictions.eq("persetujuan", Boolean.TRUE)
								: Restrictions.or(Restrictions.isNull("persetujuan"),
										Restrictions.eq("persetujuan", Boolean.FALSE))))
				.add(Restrictions.eq("kegiatanKemahasiswaan", kegiatanKemahasiswaan));

		if (order)
			criteria.addOrder(Order.asc("mahasiswa.nim"));

		return criteria;
	}

	/**
	 * Implementasi kontrak {@link DataLoader}: menghitung ulang total baris ({@link #paging} lewat
	 * {@code Common.initPaging}) dan memuat SATU HALAMAN peserta ({@code Common.ROWS_COUNT_ON_PAGE}
	 * baris, offset dari {@code paging.getActivePage()}) sesuai {@link #initCriteria}, lalu
	 * memasang {@link DetailKegiatanKemahasiswaanRenderer} baru pada {@link #grid} dan mengganti
	 * model datanya. Dibungkus {@code Common.createDefaultTimer} agar dijalankan sebagai event ZK
	 * terjadwal (bukan langsung inline) — pola standar AIS untuk refresh grid dari listener lain.
	 * Dipanggil ulang dari SEMUA listener perubahan filter, tombol Cari, event pindah halaman, dan
	 * setelah operasi hapus.
	 *
	 * @param value tidak dipakai (parameter kontrak {@link DataLoader}, disediakan untuk kompatibilitas signature)
	 */
	@SuppressWarnings("unchecked")
	public void loadData(Object value) {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.initPaging(initCriteria(false), paging);
				List<KegiatanKemahasiswaanPunyaMahasiswa> myKegiatanKemahasiswaanPunyaMahasiswas = initCriteria(true)
						.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
						.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage()))
						.list();
				ListModel strset = new SimpleListModel(myKegiatanKemahasiswaanPunyaMahasiswas);
				grid.setRowRenderer(new DetailKegiatanKemahasiswaanRenderer());
				grid.setModelCheckMobile(strset);
			}
		});

	}

	/** Mengembalikan {@code this} sebagai {@link DataLoader} — dipakai saat memanggil {@code AmbilDataMahasiswaForKegiatanKemahasiswaanHelper.display} (tombol "Ambil Mahasiswa") agar helper penambah peserta baru bisa memicu {@link #loadData} milik layar ini setelah selesai menambah. */
	private DataLoader getDataloader() {
		return this;
	}

	/**
	 * Titik masuk utama: membangun seluruh UI layar daftar peserta (toolbar filter, grid
	 * berpaging, tombol Cari/Ambil Mahasiswa/Bersihkan/Download/Upload, dan bila dikonfigurasi
	 * tombol Ekspor/Batalkan Ekspor DSpace) ke dalam {@code component} yang dioper pemanggil
	 * (dibersihkan lebih dulu lewat {@code Common.clear}). Menyimpan {@code kegiatanKemahasiswaan}
	 * ke field instance untuk dipakai seluruh method lain (renderer, {@link #initCriteria}, dsb).
	 * <p>
	 * Efek samping penting yang didokumentasikan lewat komentar inline kode: filter
	 * fakultas/jurusan SENGAJA default "Semua" (bukan dikunci ke fakultas/jurusan kegiatan) karena
	 * peserta suatu kegiatan bisa berasal dari prodi manapun; grid dibungkus
	 * {@code Borderlayout}+{@code Center} bertinggi tetap ({@code 60vh}) dengan {@code autoscroll}
	 * agar kolom kanan (Persetujuan/Hapus) tidak terpotong pada tabel lebar/panjang. Tombol
	 * "Bersihkan" menjalankan {@code DELETE} SQL langsung (bukan lewat Hibernate) untuk seluruh
	 * peserta yang belum disetujui pada kegiatan ini. Tombol Ekspor/Batalkan Ekspor DSpace berjalan
	 * di {@link Thread} terpisah dengan {@link Label} progres, memproses hanya peserta yang sudah
	 * {@code persetujuan=true} dan punya jurusan.
	 *
	 * @param kegiatanKemahasiswaan kegiatan/organisasi yang pesertanya ditampilkan
	 * @param component             container ZK tujuan (dibersihkan lalu diisi UI layar ini)
	 * @param window                window induk (diteruskan ke helper "Ambil Mahasiswa" agar bisa menutup diri sendiri bila perlu)
	 */
	public void display(final KegiatanKemahasiswaan kegiatanKemahasiswaan, final Component component,
			final MyWindow window) {
		this.kegiatanKemahasiswaan = kegiatanKemahasiswaan;
		Common.clear(component);

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		// FIX (scroll tidak ada): beri tinggi maksimum + gulir (vertikal & horizontal) supaya kolom
		// paling kanan (Persetujuan/Hapus) tidak terpotong dan daftar panjang tetap bisa digulir,
		// baik di layar lebar (desktop) maupun sempit (mobile).
		groupbox.setStyle("min-height:200px;box-sizing:border-box;");
		groupbox.setParent(component);
		groupbox.appendChild(
				new MyCaptionStyled("Daftar mahasiswa yang mengikuti organisasi " + kegiatanKemahasiswaan.getNama()));

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Mahasiswa : ")));
		toolbar.appendChild(nama = new Textbox());
		nama.setCols(10);
		nama.addEventListener(Events.ON_OK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Angkatan : ")));
		toolbar.appendChild(angkatan = new Intbox());
		angkatan.setCols(4);
		angkatan.addEventListener(Events.ON_OK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		toolbar.appendChild(new Label(Common.getBahasaConfig("Fakultas") + " : "));
		toolbar.appendChild(searchfakultas);
		searchfakultas.setCols(10);
		searchfakultas.addEventListener(Events.ON_CHANGE, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		// FIX (mahasiswa dari prodi lain tidak muncul sebagai peserta): JANGAN memfilter otomatis ke
		// fakultas/prodi kegiatan. Peserta suatu kegiatan bisa berasal dari prodi mana pun; jika
		// penyaring dikunci ke prodi kegiatan, peserta lintas-prodi tersembunyi. Default "Semua" agar
		// SEMUA peserta tampil; pengguna tetap dapat menyaring manual bila memang diperlukan.
		Common.selectComboItem(searchfakultas, null);

		toolbar.appendChild(new Label(Common.getBahasaConfig("Jurusan") + " : "));
		toolbar.appendChild(searchjurusan);
		searchjurusan.setCols(10);
		searchjurusan.addEventListener(Events.ON_CHANGE, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		// Isi pilihan prodi dengan SELURUH prodi (bukan hanya prodi kegiatan) + opsi "Semua", lalu
		// default ke "Semua" agar peserta dari prodi mana pun tetap tampil.
		Common.insertComboDanSemua(searchjurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
				Restrictions.sqlRestriction("1=1"));
		Common.selectComboItem(searchjurusan, null);

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Mahasiswa : ")));
		toolbar.appendChild(searchmahasiswa = new AmbilDataMahasiswaBanbox());
		searchmahasiswa.setCols(10);
		searchmahasiswa.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		// FIX (tidak bisa melihat status persetujuan sebagai peserta): filter status persetujuan
		// PESERTA — melengkapi filter Status yang di layar utama hanya menyaring status KEGIATAN.
		toolbar.appendChild(new Label(Common.getBahasaConfig("Persetujuan") + " : "));
		toolbar.appendChild(searchPersetujuan);
		searchPersetujuan.setCols(8);
		searchPersetujuan.setReadonly(true);
		searchPersetujuan.getChildren().clear();
		org.zkoss.zul.Comboitem ci = new org.zkoss.zul.Comboitem("Semua");
		ci.setValue(null);
		searchPersetujuan.appendChild(ci);
		org.zkoss.zul.Comboitem ciYa = new org.zkoss.zul.Comboitem("Disetujui");
		ciYa.setValue(Boolean.TRUE);
		searchPersetujuan.appendChild(ciYa);
		org.zkoss.zul.Comboitem ciTidak = new org.zkoss.zul.Comboitem("Belum disetujui");
		ciTidak.setValue(Boolean.FALSE);
		searchPersetujuan.appendChild(ciTidak);
		searchPersetujuan.setSelectedItem(ci);
		searchPersetujuan.addEventListener(Events.ON_CHANGE, new EventListener() {
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

		button = new MyToolbarbuttonConfig("Ambil Mahasiswa", "/img/new.gif");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				AmbilDataMahasiswaForKegiatanKemahasiswaanHelper dataMahasiswaHelper = new AmbilDataMahasiswaForKegiatanKemahasiswaanHelper(
						kegiatanKemahasiswaan);
				dataMahasiswaHelper.display(getDataloader(), window);
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Bersihkan", "/img/svg/trash.svg");
		button.setVisible(tbmuser.getMahasiswa() == null && tbmuser.getMahasiswa() == null);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				MyMessageboxConfig.show("Apakah yakin ingin menghapus semua data ini ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									try {

										Session session = HibernateUtil.currentSession();

										session.createSQLQuery(
												"delete from kegiatan_kemahasiswaan_punya_mahasiswa where (persetujuan is null or persetujuan = false) and kegiatan_kemahasiswaan = "
														+ kegiatanKemahasiswaan.getId())
												.executeUpdate();

										loadData(null);

									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
										PesanFormalHelper.tampilkanGagalException("Menghapus data", "Data yang Bapak/Ibu coba hapus kemungkinan besar masih memiliki keterkaitan/relasi dengan data lain pada tabel terkait (misalnya digunakan sebagai referensi oleh transaksi, detail, atau riwayat lain), sehingga sistem basis data menolak proses penghapusan ini demi menjaga integritas data secara keseluruhan.", e, new String[]{"Periksa kembali apakah data ini masih digunakan atau direferensikan oleh data lain yang berelasi.", "Hapus atau lepaskan terlebih dahulu keterkaitan/relasi data tersebut sebelum mencoba menghapus data ini kembali.", "Jika Bapak/Ibu yakin data ini seharusnya sudah tidak digunakan lagi, hubungi Administrator untuk pengecekan lebih lanjut."});
									}

								}

							}
						});

			}

		});
		button.setParent(toolbar);

		List<String> columnHeadersAdding = new ArrayList<String>();
		columnHeadersAdding.add("Bukti");

		EventListener dataAdding = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Object[] objects = (Object[]) arg0.getData();
				KegiatanKemahasiswaanPunyaMahasiswa kegiatanKemahasiswaanPunyaMahasiswa = (KegiatanKemahasiswaanPunyaMahasiswa) objects[0];

				XSSFRow row = (XSSFRow) objects[2];
				XSSFWorkbook workbook = (XSSFWorkbook) objects[3];
				XSSFFont hlink_font = workbook.createFont();
				hlink_font.setUnderline(XSSFFont.U_SINGLE);
				hlink_font.setColor(new XSSFColor(Color.BLUE));

				final XSSFCellStyle hlink_style = workbook.createCellStyle();
				hlink_style.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				hlink_style.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));
				hlink_style.setFont(hlink_font);

				/**
				 * Helper lokal sekali-pakai (didefinisikan di dalam listener {@code dataAdding}) untuk
				 * menambahkan kolom EXTRA "Bukti" (di luar {@code contents} standar) ke baris Excel hasil
				 * cetak/download data peserta ({@code Common.cetakDataCustomButton}). Menangkap
				 * {@code hlink_style} (gaya sel: latar abu-abu, font biru bergaris bawah) dari lingkup
				 * pemanggil untuk konsistensi tampilan link di seluruh workbook.
				 */
				class DataAddingHelper {
					/**
					 * Mengisi satu sel Excel pada kolom {@code index} dengan HYPERLINK ke lampiran
					 * {@link LampiranLain} milik peserta (dicari via {@link LampiranLain#ambil} berdasar
					 * id entity + nama kelas {@code jenis}) — nama file sebagai teks tampilan, URL dari
					 * {@link LampiranLain#createLinkUri()}. Bila peserta tidak punya lampiran, sel
					 * dibiarkan kosong tanpa gaya khusus.
					 *
					 * @param row                                sheet row Excel tujuan
					 * @param index                              indeks kolom (0-based) untuk sel bukti
					 * @param kegiatanKemahasiswaanPunyaMahasiswa peserta yang lampirannya dicari
					 * @param jenis                               nama kelas entity pemilik lampiran (dipakai kunci pencarian {@link LampiranLain})
					 */
					public void process(XSSFRow row, int index,
							KegiatanKemahasiswaanPunyaMahasiswa kegiatanKemahasiswaanPunyaMahasiswa, String jenis)
							throws Exception {
						LampiranLain lam = LampiranLain.ambil(kegiatanKemahasiswaanPunyaMahasiswa.getId(), jenis);

						XSSFCell cell = row.createCell(index);

						if (lam != null) {

							String nama = lam.getNama();

							cell.setCellStyle(hlink_style);
							cell.setCellValue(nama);
							String url = lam.createLinkUri();
							XSSFHyperlink link = row.getSheet().getWorkbook().getCreationHelper().createHyperlink(Hyperlink.LINK_URL);
							link.setAddress(url);
							cell.setHyperlink(link);
						}
					}
				}

				DataAddingHelper dataAddingHelper = new DataAddingHelper();

				dataAddingHelper.process(row, 9, kegiatanKemahasiswaanPunyaMahasiswa,
						KegiatanKemahasiswaanPunyaMahasiswa.class.getName());

			}
		};

		String[] contents = new String[] { "id", "kegiatanKemahasiswaan", "mahasiswa", "mahasiswa.jurusan.nama", "mulai", "sampai",
				"jabatanKegiatanKemahasiswaan", "skalaKegiatanKemahasiswaan", "persetujuan", "keterangan" };

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(
				KegiatanKemahasiswaanPunyaMahasiswa.class, this, "Download", "/img/print.png", columnHeadersAdding,
				dataAdding, contents);

		toolbar.appendChild(cetakToolbarbutton);

		MyToolbarbuttonConfig upload = Common.uploadData(this, KegiatanKemahasiswaanPunyaMahasiswa.class, contents);
		upload.setVisible(Common.getApakahAdmin() || Common.getApakahAdminLain());
		toolbar.appendChild(upload);

		// SCROLL (permintaan user): bungkus grid dalam Borderlayout -> Center(autoscroll) -> Grid ->
		// Rows -> Row. Center diberi TINGGI TERIKAT (via Borderlayout height) sehingga bila baris
		// peserta banyak atau tabel lebar, muncul scrollbar (menegak & mendatar). Caption + toolbar
		// sengaja DILUAR borderlayout agar tidak kena jebakan North-collapse ZK 5.5.
		ais.ui.util.MyBorderlayout blScroll = new ais.ui.util.MyBorderlayout();
		blScroll.setHeight("60vh");
		blScroll.setWidth("100%");
		blScroll.setStyle("min-height:280px;");
		blScroll.setParent(groupbox);
		org.zkoss.zul.Center centerScroll = new org.zkoss.zul.Center();
		centerScroll.setBorder("none");
		centerScroll.setAutoscroll(true);
		centerScroll.setParent(blScroll);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);grid.getPagingChild().setMold("os");
		grid.setParent(centerScroll);

		paging.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("0%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("NIM");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Angkatan");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Status");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jurusan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Mulai");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Sampai");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jabatan/Status");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Skala");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Persetujuan");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("7%");

		loadData(null);
		MyToolbarbuttonConfig exportKeOjs = new MyToolbarbuttonConfig("Ekspor", "/img/corner.gif");
		toolbar.appendChild(exportKeOjs);
		exportKeOjs.setVisible(Common.bolehKonfigurasi("terhubung_ke_dspace", Konfigurasi.TIDAK_AKTIF)
				&& Common.bolehKonfigurasi("kegiatan_mahasiswa_terhubung_ke_dspace"));
		exportKeOjs.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				final Label label = Common.displayLoadBar(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						onSearchDefault(arg0);
						LogLoginAction.tampilDpsaceLog();
					}
				});

				new Thread(new Runnable() {

					@SuppressWarnings("unchecked")
					@Override
					public void run() {
						try {
							String cookie = DspaceCommon.login();
							List<KegiatanKemahasiswaanPunyaMahasiswa> kegiatanKemahasiswaanPunyaMahasiswas = initCriteria(
									true).add(Restrictions.isNotNull("mahasiswa.jurusan"))
											.add(Restrictions.eq("persetujuan", true)).list();

							int rowIndex = 1;
							for (KegiatanKemahasiswaanPunyaMahasiswa kegiatanKemahasiswaanPunyaMahasiswa : kegiatanKemahasiswaanPunyaMahasiswas) {
								label.setValue("Sedang memproses data " + kegiatanKemahasiswaanPunyaMahasiswa.toString()
										+ " ("
										+ Common.numberFormat.get().format(
												(rowIndex++) * 100.0 / kegiatanKemahasiswaanPunyaMahasiswas.size())
										+ " %)");
								KegiatanKemahasiswaanPunyaMahasiswaHelper.getDspace(cookie,
										kegiatanKemahasiswaanPunyaMahasiswa, true);
							}
						} catch (Exception e) {
							// TODO Auto-generated catch block
							Common.tampilErrorJikaAdmin(e);
						}
						label.setValue("");
					}
				}).start();
			}
		});

		MyToolbarbuttonConfig batalExport = new MyToolbarbuttonConfig("Batalkan Ekspor", "/img/svg/trash.svg");
		toolbar.appendChild(batalExport);
		batalExport.setVisible(Common.bolehKonfigurasi("terhubung_ke_dspace", Konfigurasi.TIDAK_AKTIF)
				&& Common.bolehKonfigurasi("kegiatan_mahasiswa_terhubung_ke_dspace"));
		batalExport.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				MyMessageboxConfig.show("Apakah yakin ingin membatalkan ekspor data ini ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {

									final Label label = Common.displayLoadBar(new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											onSearchDefault(arg0);
											LogLoginAction.tampilDpsaceLog();
										}
									});

									new Thread(new Runnable() {

										@SuppressWarnings("unchecked")
										@Override
										public void run() {
											try {
											try {
												String cookie = DspaceCommon.login();
												List<KegiatanKemahasiswaanPunyaMahasiswa> kegiatanKemahasiswaanPunyaMahasiswas = initCriteria(
														true).add(Restrictions.isNotNull("mahasiswa.jurusan"))
																.add(Restrictions.eq("persetujuan", true)).list();

												int rowIndex = 1;
												for (KegiatanKemahasiswaanPunyaMahasiswa kegiatanKemahasiswaanPunyaMahasiswa : kegiatanKemahasiswaanPunyaMahasiswas) {
													label.setValue("Sedang memproses data "
															+ kegiatanKemahasiswaanPunyaMahasiswa.toString() + " ("
															+ Common.numberFormat.get().format((rowIndex++) * 100.0
																	/ kegiatanKemahasiswaanPunyaMahasiswas.size())
															+ " %)");
													DspaceInformation dspaceInformation = DspaceInformation
															.getDspaceInformation(
																	KegiatanKemahasiswaanPunyaMahasiswa.class.getName(),
																	kegiatanKemahasiswaanPunyaMahasiswa.getId());
													if (dspaceInformation != null) {
														int i = DspaceInformation.delete(cookie,
																"items/" + dspaceInformation.getUuid(),
																dspaceInformation.getPostInfo());
														if (i == 200) {

															Session session = HibernateUtil.currentNativeSession();
															session.getTransaction().begin();
															session.delete(dspaceInformation);
															session.getTransaction().commit();
															HibernateUtil.closeSession();
														}
													}
												}
											} catch (Exception e) {
												// TODO Auto-generated catch
												// block
												Common.tampilErrorJikaAdmin(e);
											}
											label.setValue("");
																					} finally {
												ais.database.hibernate.HibernateUtil.closeSession();
											}
										}
									}).start();

								}

							}
						});
			}
		});
	}

	/**
	 * Membangun payload metadata Dublin Core (JSON, skema {@code dc.*} standar DSpace) untuk satu
	 * peserta {@link KegiatanKemahasiswaanPunyaMahasiswa} dan mengirimkannya sebagai item repository
	 * lewat {@link DspaceInformation#dspaceProcess} — dipetakan: {@code dc.contributor.author/editor}
	 * = nama mahasiswa, {@code dc.date.copyright} = label universitas dari konfigurasi,
	 * {@code dc.description.abstract} = {@code keterangan} peserta (di-strip HTML lewat
	 * {@link Html2Text}), {@code dc.type} = nama jabatan (bila ada), {@code dc.title} = nama
	 * kegiatan, {@code dc.subject} = nama skala kegiatan (bila ada), {@code dc.publisher} = tempat
	 * kegiatan, {@code dc.identifier.uri} = URL kegiatan (ditimpa dengan URL lampiran bila ada
	 * berkas bukti), {@code dc.identifier.issn} = kode kegiatan, {@code dc.language} = bahasa
	 * mahasiswa, {@code dc.date.issued} = tanggal mulai. Item dibuat/diupdate di dalam collection
	 * yang di-resolve lewat {@link #getDspaceTipeKegiatanKemahasiswaanPunyaMahasiswa}. Bila peserta
	 * punya {@link LampiranLain} (berkas bukti/sertifikat), file tsb turut di-upload ke item DSpace
	 * setelah item berhasil dibuat/diupdate.
	 *
	 * @param cookie                               token sesi DSpace hasil {@code DspaceCommon.login()}
	 * @param kegiatanKemahasiswaanPunyaMahasiswa  peserta yang akan diekspor
	 * @param update                                {@code true} untuk memperbarui item existing, {@code false} untuk membuat baru
	 * @return {@link DspaceInformation} hasil proses (berisi uuid item DSpace, dsb.)
	 * @throws Exception dilempar apa adanya dari kegagalan panggilan API DSpace
	 */
	public static DspaceInformation getDspace(String cookie,
			KegiatanKemahasiswaanPunyaMahasiswa kegiatanKemahasiswaanPunyaMahasiswa, boolean update) throws Exception {

		JSONArray jsonArray = new JSONArray();

		String nama = "";
		if (kegiatanKemahasiswaanPunyaMahasiswa.getMahasiswa() != null) {
			nama = kegiatanKemahasiswaanPunyaMahasiswa.getMahasiswa().getNama();
		}

		JSONObject jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.contributor.author");
		jsonMetadata.put("value", nama);
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.contributor.editor");
		jsonMetadata.put("value", nama);
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.date.copyright");
		jsonMetadata.put("value",
				"Semua hak cipta dilindungi oleh " + Common.getKonfigurasi("label_universitas", "").getNilai());
		jsonArray.put(jsonMetadata);

		Html2Text parser = new Html2Text();
		parser.parse(new StringReader(kegiatanKemahasiswaanPunyaMahasiswa.getKeterangan()));

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.description.abstract");
		jsonMetadata.put("value", parser.getText());
		jsonArray.put(jsonMetadata);

		if (kegiatanKemahasiswaanPunyaMahasiswa.getJabatanKegiatanKemahasiswaan() != null) {
			jsonMetadata = new JSONObject();
			jsonMetadata.put("key", "dc.type");
			jsonMetadata.put("value", kegiatanKemahasiswaanPunyaMahasiswa.getJabatanKegiatanKemahasiswaan().getNama());
			jsonArray.put(jsonMetadata);
		}

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.title");
		jsonMetadata.put("value", kegiatanKemahasiswaanPunyaMahasiswa.getKegiatanKemahasiswaan().getNama());
		jsonArray.put(jsonMetadata);

		if (kegiatanKemahasiswaanPunyaMahasiswa.getSkalaKegiatanKemahasiswaan() != null) {
			jsonMetadata = new JSONObject();
			jsonMetadata.put("key", "dc.subject");
			jsonMetadata.put("value", kegiatanKemahasiswaanPunyaMahasiswa.getSkalaKegiatanKemahasiswaan().getNama());
			jsonArray.put(jsonMetadata);
		}

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.publisher");
		jsonMetadata.put("value", kegiatanKemahasiswaanPunyaMahasiswa.getKegiatanKemahasiswaan().getTempat());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.identifier.uri");
		jsonMetadata.put("value", kegiatanKemahasiswaanPunyaMahasiswa.getKegiatanKemahasiswaan().getUrl());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.identifier.issn");
		jsonMetadata.put("value", kegiatanKemahasiswaanPunyaMahasiswa.getKegiatanKemahasiswaan().getKode());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.language");
		jsonMetadata.put("value", kegiatanKemahasiswaanPunyaMahasiswa.getMahasiswa().getBahasa());
		jsonArray.put(jsonMetadata);

		if (kegiatanKemahasiswaanPunyaMahasiswa.getMulai() != null) {
			jsonMetadata = new JSONObject();
			jsonMetadata.put("key", "dc.date.issued");
			jsonMetadata.put("value", Common.databaseDateFormat.get().format(kegiatanKemahasiswaanPunyaMahasiswa.getMulai()));
			jsonArray.put(jsonMetadata);
		}

		LampiranLain lampiranLain = LampiranLain.ambil(kegiatanKemahasiswaanPunyaMahasiswa.getId(),
				KegiatanKemahasiswaanPunyaMahasiswa.class.getName());
		if (lampiranLain != null) {
			String uri = lampiranLain.createLinkUri(false);
			if (uri != null && !uri.trim().isEmpty()) {
				jsonMetadata = new JSONObject();
				jsonMetadata.put("key", "dc.identifier.uri");
				jsonMetadata.put("value", uri);
				jsonArray.put(jsonMetadata);
			}
		}

		JSONObject jsonPost = new JSONObject();
		jsonPost.put("metadata", jsonArray);

		DspaceInformation dspaceInformation = DspaceInformation.dspaceProcess(cookie,
				kegiatanKemahasiswaanPunyaMahasiswa, jsonPost.toString(), jsonArray.toString(), update, "items",
				"collections/"
						+ getDspaceTipeKegiatanKemahasiswaanPunyaMahasiswa(cookie, kegiatanKemahasiswaanPunyaMahasiswa)
						+ "/items",
				"items/{uuid}/metadata");

		if (lampiranLain != null) {
			DspaceInformation.upload(cookie, dspaceInformation.getUuid(), lampiranLain,
					"Sertifikat / Lampiran Bukti Ikut Kegiatan");
		}

		return dspaceInformation;
	}

	/**
	 * Me-resolve (atau membuat, via {@link DspaceInformation#dspaceProcess}) COLLECTION DSpace
	 * untuk kombinasi {@link KegiatanKemahasiswaan}+{@link Jurusan} peserta — satu tingkat di
	 * bawah community jurusan yang diambil dari
	 * {@link #getDspaceTipeKegiatanKemahasiswaanPunyaMahasiswaJurusan}. Id collection yang sudah
	 * pernah dibuat di-cache lewat {@link Konfigurasi} berkunci
	 * {@code dspace_label_collection_kegiatanKemahasiswaanPunyaMahasiswa_jurusan_<idKegiatan>_<idJurusan>}
	 * agar panggilan berikutnya untuk kombinasi yang sama tidak membuat collection duplikat.
	 *
	 * @param cookie                               token sesi DSpace
	 * @param kegiatanKemahasiswaanPunyaMahasiswa  sumber kegiatan+jurusan yang collection-nya di-resolve
	 * @return {@link DspaceInformation} collection tsb (baru dibuat atau existing)
	 * @throws Exception dilempar apa adanya dari kegagalan panggilan API DSpace
	 */
	public static DspaceInformation getDspaceTipeKegiatanKemahasiswaanPunyaMahasiswa(String cookie,
			KegiatanKemahasiswaanPunyaMahasiswa kegiatanKemahasiswaanPunyaMahasiswa) throws Exception {

		String description = "Kegiatan mahasiswa yang berupa "
				+ kegiatanKemahasiswaanPunyaMahasiswa.getKegiatanKemahasiswaan().getNama() + " pada kelompok "
				+ kegiatanKemahasiswaanPunyaMahasiswa.getKegiatanKemahasiswaan()
						.getDetailKelompokKegiatanKemahasiswaan().getNama();

		JSONObject jsonPost = new JSONObject();
		jsonPost.put("name", kegiatanKemahasiswaanPunyaMahasiswa.getKegiatanKemahasiswaan().getNama());
		jsonPost.put("copyrightText",
				"Semua hak cipta dilindungi oleh " + Common.getKonfigurasi("label_universitas", "").getNilai());
		jsonPost.put("introductoryText", description);
		jsonPost.put("shortDescription", "Kegiatan Mahasiswa "
				+ kegiatanKemahasiswaanPunyaMahasiswa.getKegiatanKemahasiswaan().getNama() + " Repository");
		jsonPost.put("sidebarText", description);

		Konfigurasi uuidKonfigurasi = Common
				.getKonfigurasi("dspace_label_collection_kegiatanKemahasiswaanPunyaMahasiswa_jurusan_"
						+ kegiatanKemahasiswaanPunyaMahasiswa.getKegiatanKemahasiswaan().getId() + "_"
						+ kegiatanKemahasiswaanPunyaMahasiswa.getMahasiswa().getJurusan().getId(), "");
		return DspaceInformation.dspaceProcess(cookie, uuidKonfigurasi, jsonPost.toString(), true, "collections",
				"communities/" + getDspaceTipeKegiatanKemahasiswaanPunyaMahasiswaJurusan(cookie,
						kegiatanKemahasiswaanPunyaMahasiswa) + "/collections");
	}

	/**
	 * Me-resolve (atau membuat) COMMUNITY DSpace level "Prestasi Mahasiswa" untuk satu
	 * {@link Jurusan} — level TERTINGGI hierarki DSpace yang dipakai kelas ini, di bawah community
	 * jurusan itu sendiri yang di-resolve lewat {@link JurusanAction#getDspace}. Id community
	 * di-cache lewat {@link Konfigurasi} berkunci
	 * {@code dspace_label_collection_kegiatanKemahasiswaanPunyaMahasiswa_<idJurusan>}.
	 *
	 * @param cookie                               token sesi DSpace
	 * @param kegiatanKemahasiswaanPunyaMahasiswa  sumber jurusan (lewat {@code getMahasiswa().getJurusan()}) yang community-nya di-resolve
	 * @return {@link DspaceInformation} community tsb (baru dibuat atau existing)
	 * @throws Exception dilempar apa adanya dari kegagalan panggilan API DSpace
	 */
	public static DspaceInformation getDspaceTipeKegiatanKemahasiswaanPunyaMahasiswaJurusan(String cookie,
			KegiatanKemahasiswaanPunyaMahasiswa kegiatanKemahasiswaanPunyaMahasiswa) throws Exception {
		Jurusan jurusan = kegiatanKemahasiswaanPunyaMahasiswa.getMahasiswa().getJurusan();

		String description = "Prestasi mahasiswa untuk " + Common.getBahasaConfig("Jurusan") + " "
				+ kegiatanKemahasiswaanPunyaMahasiswa.getMahasiswa().getJurusan().getNama();

		JSONObject jsonPost = new JSONObject();
		jsonPost.put("name", "Prestasi Mahasiswa");
		jsonPost.put("copyrightText",
				"Semua hak cipta dilindungi oleh " + Common.getKonfigurasi("label_universitas", "").getNilai());
		jsonPost.put("introductoryText", description);
		jsonPost.put("shortDescription",
				"Prestasi Mahasiswa "
						+ kegiatanKemahasiswaanPunyaMahasiswa.getMahasiswa().getJurusan().getJenjang().getNama()
						+ " Repository");
		jsonPost.put("sidebarText", description);

		Konfigurasi uuidKonfigurasi = Common
				.getKonfigurasi("dspace_label_collection_kegiatanKemahasiswaanPunyaMahasiswa_" + jurusan.getId(), "");
		return DspaceInformation.dspaceProcess(cookie, uuidKonfigurasi, jsonPost.toString(), false, "collections",
				"communities/" + JurusanAction.getDspace(cookie, jurusan, false) + "/collections");

	}

	/** Implementasi kontrak {@link DataSearchDefault}: memuat ulang grid dengan filter saat ini — dipanggil sebagai callback setelah operasi ekspor/batalkan-ekspor DSpace selesai untuk menyegarkan tampilan. */
	@Override
	public void onSearchDefault(Event event) {
		loadData(null);
	}

}
