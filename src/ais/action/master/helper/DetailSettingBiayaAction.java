package ais.action.master.helper;
import ais.common.PesanFormalHelper;


import ais.common.CommonSearchFilterHelper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.generic.AmbilDataBiodataCalonMahasiswaBanyak;
import ais.action.master.helper.generic.AmbilDataMahasiswaBanyak;
import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.AfiliasiCalonMahasiswa;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.CicilanPembayaran;
import ais.database.model.DetailBiaya;
import ais.database.model.DetailKegiatan;
import ais.database.model.DetailSettingBiaya;
import ais.database.model.HistoryStatusMahasiswa;
import ais.database.model.ItemBiaya;
import ais.database.model.JenisKegiatan;
import ais.database.model.Jurusan;
import ais.database.model.Kegiatan;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Paket;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.Perkuliahan;
import ais.database.model.SettingBiaya;
import ais.database.model.SettingBiayaDetail;
import ais.database.model.StatusAwalMahasiswa;
import ais.database.model.Tbmuser;
import ais.ui.render.DetailPembayaranMahasiswaRenderer;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyDoubleboxMin;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Layar keuangan "Detail Setting Biaya": menampilkan dan mengelola tagihan seluruh mahasiswa
 * atau calon mahasiswa yang terkena satu {@link SettingBiaya} (aturan biaya per jenis kegiatan,
 * mis. SPP semester, pendaftaran ulang, uang gedung). Dibuka dari layar Setting Biaya via
 * constructor {@link #DetailSettingBiayaAction(SettingBiaya)}. Untuk setiap
 * mahasiswa/calon-mahasiswa dalam cakupan, layar menampilkan nominal "Tagihan Aktif" per
 * {@link ItemBiaya} — yaitu {@link DetailKegiatan} milik {@link Kegiatan} yang SUDAH berjalan —
 * dan (bila {@code nilaiBisaDiubah} pada ItemBiaya-nya) mengizinkan admin mengubah nominal
 * tersebut satu-satu langsung dari grid, contoh nyata: tagihan "Sumbangan Bangunan Tahap II"
 * diturunkan dari plafon Rp4.000.000 menjadi sesuai kesanggupan mahasiswa (mis. Rp1.000.000),
 * yang otomatis membuat kekurangan tagihan tersebut dianggap Lunas.
 *
 * <p><b>Empat mode tampilan grid</b> (ditentukan {@link #modeDaftarMahasiswa()} dan jenis
 * {@link JenisKegiatan} dari {@link #settingBiaya}, lihat percabangan di {@link #initCriteria}
 * dan {@link #loadData(Object, boolean)}):</p>
 * <ul>
 * <li><b>Khusus per calon mahasiswa</b> ({@link CalonMahasiswaSettingRenderer}) — dipakai bila
 * SettingBiaya untuk jenis kegiatan Pendaftaran Calon Mahasiswa/Pendaftaran Ulang Mahasiswa Baru
 * dan ditandai khusus/dibatasi mahasiswa tertentu; sumber baris {@link SettingBiayaDetail}
 * (template kuota custom per orang, kolom JSON {@code biayas} berisi nilai per
 * {@code itemBiaya.id}) plus tampilan tagihan aktif yang sama seperti mode reguler.</li>
 * <li><b>Khusus per mahasiswa</b> ({@link MahasiswaSettingRenderer}) — padanan untuk mahasiswa
 * aktif (bukan calon).</li>
 * <li><b>Daftar mahasiswa reguler</b> ({@link MahasiswaRenderer}) — sumber baris langsung
 * {@link Mahasiswa} disaring kriteria SettingBiaya (program, status awal, jenjang, jenis
 * seleksi, gelombang pendaftaran, fakultas/jurusan, angkatan, pencarian nama/NIM).</li>
 * <li><b>Daftar calon mahasiswa reguler</b> ({@link CalonMahasiswaRenderer}) — padanan untuk
 * {@link BiodataCalonMahasiswa}, dengan filter tambahan replikasi mesin pencocokan tagihan
 * (lihat komentar "PERBAIKAN" di {@link #initCriteria} soal jenjang jenisSeleksi
 * dipilih/mentah/gelombang) agar daftar di layar ini konsisten dengan siapa yang benar-benar
 * kena tagihan.</li>
 * </ul>
 *
 * <p><b>Resolusi tagihan aktif</b> ({@link #resolveKegiatanMahasiswa}/
 * {@link #resolveKegiatanCalonMahasiswa}) memanggil {@code KegiatanHelper.checkKegiatanMahasiswa}/
 * {@code checkKegiatanCalonMahasiswa} yang MEMBUAT {@link Kegiatan} bila belum ada (sama seperti
 * perilaku grid pembayaran lain), lalu daftar {@link DetailBiaya}/{@link PengaturanPembayaranBulanan}
 * dipetakan per {@code itemBiaya.id} untuk dirender oleh {@link #renderSatuItemTagihanAktif}.
 * Kolom "Tanggal Tagihan" ({@link #renderTanggalTagihan}/{@link #ambilTanggalTagihanEfektif})
 * menampilkan tanggal efektif tiap item, dengan fallback ke tanggal default DetailKegiatan
 * placeholder lalu {@code DetailBiaya.getDefaultTanggalTagihan()}.</p>
 *
 * <p><b>Fitur lain:</b> upload/download Excel massal tagihan dan template SettingBiayaDetail
 * ({@link #uploadDataMahasiswa}, {@link #uploadDataCalonMahasiswa}), tombol Reset (kembalikan
	 * tagihan ke default billing, {@code rst=true}) dan Refresh (hitung ulang tanpa reset).
	 * Akses membuat/mengatur Billing sengaja tidak diletakkan di layar detail ini; akses tersebut
	 * berada pada menu Action setiap baris Setting Biaya dengan Tagihan Default = Tidak agar jalur
	 * navigasinya tunggal dan dapat ditemukan tanpa membuka detail lebih dulu.
 * Penghapusan binding SettingBiayaDetail ({@link #bolehHapusSettingBiayaDetail}) tidak pernah
 * menghapus {@link DetailBiaya} yang sudah dipakai sebagai transaksi/tagihan historis — baris
 * di-null-kan referensinya (mahasiswa/calon mahasiswa dilepas) alih-alih dihapus, demi menjaga
 * integritas riwayat.</p>
 *
 * @see MyDetail
 */
public class DetailSettingBiayaAction extends MyDetail implements DataCriteria {
	/**
	 * @return {@code true} bila {@link #settingBiaya} ditandai khusus untuk mahasiswa
	 *         tertentu atau dibatasi mahasiswa tertentu — menentukan apakah grid memakai
	 *         sumber baris {@link SettingBiayaDetail} (mode "khusus per mahasiswa/calon")
	 *         alih-alih daftar {@link Mahasiswa}/{@link BiodataCalonMahasiswa} reguler.
	 */
	private boolean modeDaftarMahasiswa() {
		return settingBiaya != null && (settingBiaya.getKhususBuatMahasiswaTertentu()
				|| settingBiaya.getBatasiMahasiswaTertentu());
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private SettingBiaya settingBiaya;
	private MyGrid grid;

	private Textbox pencarian;

	private Combobox searchtahun;
	private Combobox searchtahunsd;
	private Combobox searchfakultas;
	private Combobox searchjurusan;

	private List<ItemBiaya> selectedItemBiaya;

	private Paging paging;
	private boolean mhs = true;

	private Combobox genapGanjil;
	private Combobox tahunAkademik;

	private Tbmuser tbmuser;

	private EventListener refrsh = new EventListener() {

		@Override
		public void onEvent(Event arg0) throws Exception {
			loadData(true);
		}
	};

	/**
	 * Simpan {@link SettingBiaya} sumber data dan tentukan mode mahasiswa-aktif vs
	 * calon-mahasiswa ({@link #mhs}) dari {@link JenisKegiatan}-nya (Pendaftaran Calon Mahasiswa
	 * atau Pendaftaran Ulang Mahasiswa Baru berarti mode calon mahasiswa). Menyiapkan
	 * {@link Paging} yang memuat ulang grid ({@link #loadData(Object)}) tiap ganti halaman, dan
	 * mendaftarkan listener {@code onOpen} yang membangun UI ({@link #display()}) tepat saat
	 * layar dibuka pertama kali oleh ZK (bukan langsung di constructor) sambil membersihkan
	 * komponen lama ({@link Common#clear}).
	 *
	 * @param settingBiaya aturan biaya yang tagihan-tagihannya ingin dikelola di layar ini.
	 */
	public DetailSettingBiayaAction(SettingBiaya settingBiaya) {
		super();
		tbmuser = Common.getCurrentUser();
		this.settingBiaya = settingBiaya;
		JenisKegiatan j = settingBiaya.getJenisKegiatan();
		if ((ConstantValues.PENDAFTARAN_CALON_MAHASISWA != null
				&& j.getId().equals(ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId()))
				|| (ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU != null
						&& j.getId().equals(ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU.getId()))) {
			mhs = false;
		}

		paging = new Paging();
		Common.initPaging15(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(DetailSettingBiayaAction.this);
				if (isOpen()) {
					display();
				}
			}
		});
	}

	/**
	 * Resolusi Kegiatan+DetailKegiatan seorang Mahasiswa untuk {@link #settingBiaya} ini —
	 * DIEKSTRAK agar dipakai BERSAMA oleh grid mahasiswa reguler ({@link MahasiswaRenderer})
	 * maupun grid "khusus per mahasiswa" ({@link MahasiswaSettingRenderer}), sehingga kolom
	 * "Nominal Tagihan Aktif" identik di kedua mode — termasuk fitur yang diminta: nominal
	 * {@link DetailKegiatan} boleh diubah manual per mahasiswa (mis. tagihan "Sumbangan
	 * Bangunan Tahap II" diturunkan dari plafon Rp4.000.000 menjadi sesuai kesanggupan
	 * mahasiswa, mis. Rp1.000.000, dan otomatis dianggap Lunas karena kekurangan menjadi 0).
	 * <p>
	 * Tidak melakukan side effect UI apa pun — murni resolusi data (KRS sync, default
	 * biaya/tarian bulanan, lalu {@code KegiatanHelper.checkKegiatanMahasiswa} yang MEMBUAT
	 * Kegiatan bila belum ada, sama seperti perilaku grid reguler selama ini).
	 *
	 * @return array 3 elemen: {@code [0]=Kegiatan} (boleh null), {@code [1]=Collection<DetailKegiatan>}
	 *         milik kegiatan tsb, {@code [2]=Map<Long idItemBiaya, Map<Long idDetailBiayaOrPpb, Object[]>>}
	 *         — dipakai {@link #renderSatuItemTagihanAktif} per item biaya.
	 */
	@SuppressWarnings("rawtypes")
	private Object[] resolveKegiatanMahasiswa(Mahasiswa mahasiswa, Integer smt, boolean refresh, boolean rst)
			throws Exception {
		Session session = HibernateUtil.currentNativeSession();
		Collection mydetailBiayas;
		if (settingBiaya != null && settingBiaya.getGunakanBiayaDefault()) {

			KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, smt, null, null, refresh);

			HistoryStatusMahasiswa tempHistoryStatusMahasiswa = ais.action.master.helper.HistoryStatusMahasiswaUtil
					.getHistoryStatusMahasiswa(krsMahasiswa, refresh);

			StatusAwalMahasiswa statusAwalMahasiswa = tempHistoryStatusMahasiswa.getStatusAwalMahasiswa();
			Paket paket = null;
			AfiliasiCalonMahasiswa afiliasiCalonMahasiswa = null;
			try {
				BiodataCalonMahasiswa biodataCalonMahasiswa = mahasiswa.getBiodataCalonMahasiswaData();
				paket = biodataCalonMahasiswa == null ? null : biodataCalonMahasiswa.getPaket();
				afiliasiCalonMahasiswa = biodataCalonMahasiswa == null ? null
						: biodataCalonMahasiswa.getAfiliasiCalonMahasiswa();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailSettingBiayaAction.java:resolveKegiatanMahasiswa.biodata");
			}

			// Helper bersarang di atas (singkronkanKrsMahasiswa/getHistoryStatusMahasiswa) dapat
			// menutup native session ThreadLocal. Ambil ulang session yang DIJAMIN open sebelum dipakai
			// (currentNativeSession mengembalikan session sama bila masih open, atau membuka baru).
			session = HibernateUtil.currentNativeSession();
			mydetailBiayas = SetingBiayaHelper.getDefaultSettingBiaya(session, settingBiaya,
					mahasiswa.getTahunangkatan(), mahasiswa.getJenjang(), smt, settingBiaya.getJenisKegiatan(),
					statusAwalMahasiswa, tempHistoryStatusMahasiswa.getStatusMahasiswa(),
					mahasiswa.getJenisSeleksi(), mahasiswa.getGelombangPendaftaran(), paket, mahasiswa.getJurusan(),
					mahasiswa.getProgram(), mahasiswa.getKelamin(), afiliasiCalonMahasiswa);
		} else {
			mydetailBiayas = PembayaranUtilHelper.getDetailBiayaMahasiswa(mahasiswa, smt,
					settingBiaya.getJenisKegiatan(), refresh);
		}

		if (settingBiaya != null && !settingBiaya.getGunakanBiayaDefault()) {
			// getDetailBiayaMahasiswa/helper di atas dapat menutup native session -> ambil ulang.
			session = HibernateUtil.currentNativeSession();
			int countPengaturanBulanan = PembayaranUtilHelper.countBulanan(session, mahasiswa,
					settingBiaya.getJenisKegiatan(), smt, mydetailBiayas, refresh, true);
			if (countPengaturanBulanan > 0) {
				mydetailBiayas = PembayaranUtilHelper.getDetailBiayaMahasiswa(mahasiswa, smt,
						settingBiaya.getJenisKegiatan(), "-1", refresh);
			}
		}
		JenisKegiatan j = settingBiaya.getJenisKegiatan();
		boolean ulang = refresh;
		ItemBiaya item = null;
		// Helper-helper di atas dapat menutup native session -> ambil ulang sebelum dipakai.
		session = HibernateUtil.currentNativeSession();
		final Kegiatan kegiatan = KegiatanHelper.checkKegiatanMahasiswa(j, mahasiswa, smt,
				tahunAkademik.getSelectedItem().getValue().toString(), ulang, rst, item, session);
		Collection<DetailKegiatan> detailKegiatans = kegiatan == null || kegiatan.getId() == null ? null
				: kegiatan.ambilDetailKegiatan(refresh);
		Map<Long, Map<Long, Object[]>> datas = new HashMap<Long, Map<Long, Object[]>>();
		for (Object o : mydetailBiayas) {
			DetailBiaya detailBiaya = null;
			PengaturanPembayaranBulanan pengaturanPembayaranBulanan = null;
			if (o instanceof DetailBiaya) {
				detailBiaya = (DetailBiaya) o;
				Map<Long, Object[]> map = datas.get(detailBiaya.getItemBiaya().getId());
				if (map == null) {
					map = new HashMap<Long, Object[]>();
					datas.put(detailBiaya.getItemBiaya().getId(), map);
				}
				map.put(detailBiaya.getId(), new Object[] { detailBiaya });
			} else if (o instanceof PengaturanPembayaranBulanan) {
				pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) o;
				detailBiaya = pengaturanPembayaranBulanan.getDetailBiaya();
				Map<Long, Object[]> map = datas.get(detailBiaya.getItemBiaya().getId());
				if (map == null) {
					map = new HashMap<Long, Object[]>();
					datas.put(detailBiaya.getItemBiaya().getId(), map);
				}
				map.put(pengaturanPembayaranBulanan.getId(), new Object[] { pengaturanPembayaranBulanan });
			}
		}
		return new Object[] { kegiatan, detailKegiatans, datas };
	}

	/**
	 * Render SATU kolom item biaya — nominal "Tagihan Aktif" ({@link DetailKegiatan} milik
	 * Kegiatan yang SUDAH berjalan) — ke dalam {@code vbox} yang diberikan. Diekstrak dari
	 * {@link MahasiswaRenderer}/{@link CalonMahasiswaRenderer} agar dipakai ULANG oleh
	 * {@link MahasiswaSettingRenderer}/{@link CalonMahasiswaSettingRenderer} (grid "khusus
	 * per mahasiswa"), sehingga kemampuan ubah nominal manual (menghormati
	 * {@code ItemBiaya.nilaiBisaDiubah} dan penguncian {@code DetailKegiatan.kunci}) identik
	 * di KEDUA mode Setting Biaya — inilah mekanisme "satu-satu" yang diminta user.
	 */
	private void renderSatuItemTagihanAktif(final Vbox vbox, final Kegiatan kegiatan,
			final Collection<DetailKegiatan> detailKegiatans, Map<Long, Object[]> map, final boolean refresh) {
		for (Object[] d : map.values()) {
			DetailBiaya detailBiaya = null;
			PengaturanPembayaranBulanan pengaturanPembayaranBulanan = null;

			if (d[0] instanceof PengaturanPembayaranBulanan) {
				pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) d[0];
				detailBiaya = pengaturanPembayaranBulanan.getDetailBiaya();
			} else if (d[0] instanceof DetailBiaya) {
				detailBiaya = (DetailBiaya) d[0];
			}

			if (pengaturanPembayaranBulanan != null) {
				vbox.appendChild(new Label("Bulan " + pengaturanPembayaranBulanan.getNamaBulan()));
			}
			final DetailBiaya detailBiayaFinal = detailBiaya;
			final PengaturanPembayaranBulanan pengaturanPembayaranBulanan1 = pengaturanPembayaranBulanan;
			final DetailKegiatan detailKegiatan = kegiatan == null || kegiatan.getId() == null ? null
					: (pengaturanPembayaranBulanan1 != null
							? kegiatan.ambilSatuDetailKegiatan(pengaturanPembayaranBulanan1, detailKegiatans)
							: kegiatan.ambilSatuDetailKegiatan(detailBiayaFinal, refresh));
			if (kegiatan != null && (detailBiayaFinal.getItemBiaya().getNilaiBisaDiubah()
					&& (detailKegiatan == null || detailKegiatan.getKunci() == null))) {
				final MyDoublebox doublebox;
				if (detailBiayaFinal.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
					doublebox = new MyDoubleboxMin(-Math.abs(detailKegiatan == null ? 0.0 : detailKegiatan.getBiaya()));
				} else {
					doublebox = new MyDoublebox(detailKegiatan == null ? 0.0 : detailKegiatan.getBiaya());
				}
				doublebox.setParent(vbox);

				doublebox.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						Session session = HibernateUtil.currentSession();
						DetailKegiatan dk = detailKegiatan;
						if (dk == null) {
							dk = new DetailKegiatan();
						}
						dk.setPengaturanPembayaranBulanan(pengaturanPembayaranBulanan1);
						dk.setBiaya(doublebox.getValue());
						dk.setDetailBiaya(detailBiayaFinal);
						dk.setKeterangan(detailBiayaFinal.getKeterangan());
						dk.setKegiatan(kegiatan);
						Common.refreshSaveOrUpdate(session, dk);

					}
				});
				doublebox.setWidth("90%");
				if (detailKegiatan != null && detailKegiatan.getBukanTagihan()) {
					doublebox.setDisabled(true);
				}

				// AKTIF/NONAKTIF per mahasiswa (mirip checkbox aktifkanmanual di sisi siswa
				// PengaturanBiayaAction): admin bisa langsung menonaktifkan tagihan item ini
				// utk mahasiswa ybs dari sini, tanpa pindah ke layar lain. Hanya tersedia bila
				// DetailKegiatan-nya sudah ada (baris belum ada -> belum ada yg bisa dimatikan).
				if (detailKegiatan != null && detailKegiatan.getId() != null) {
					final DetailKegiatan dkUntukToggle = detailKegiatan;
					final MyCheckboxConfig aktifkan = new MyCheckboxConfig("Aktif");
					aktifkan.setChecked(!Boolean.TRUE.equals(dkUntukToggle.getBukanTagihan()));
					aktifkan.setParent(vbox);
					aktifkan.addEventListener("onCheck", new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							Session session = HibernateUtil.currentSession();
							dkUntukToggle.setBukanTagihan(!aktifkan.isChecked());
							Common.refreshSaveOrUpdate(session, dkUntukToggle);
							doublebox.setDisabled(!aktifkan.isChecked());
						}
					});
				}

			} else {
				new Label(Common.numberFormat.get()
						.format(detailKegiatan == null ? 0.0 : detailKegiatan.getBiaya()))
						.setParent(vbox);
			}

			DetailPembayaranMahasiswaRenderer.tampilkanKunci(vbox, detailKegiatan, refrsh, tbmuser);
		}
	}

	/**
	 * Saring peta tagihan aktif ({@code map}, hasil {@link #resolveKegiatanMahasiswa}) agar
	 * hanya menyisakan entri yang {@link DetailBiaya}-nya benar-benar berasal dari
	 * {@link #settingBiaya} INI — perlu karena satu {@link ItemBiaya} bisa punya tagihan dari
	 * beberapa SettingBiaya berbeda (mis. tagihan lama dari setting sebelumnya), sehingga grid
	 * layar ini tidak menampilkan/mengizinkan ubah nominal tagihan milik SettingBiaya lain.
	 *
	 * @param map peta {@code idDetailBiayaAtauPpb -> Object[]{DetailBiaya atau PengaturanPembayaranBulanan}}.
	 * @return sub-peta yang hanya berisi entri milik {@link #settingBiaya}, atau {@code null}
	 *         bila {@code map} null atau tidak ada entri yang cocok.
	 */
	private Map<Long, Object[]> saringMapUntukSettingIni(Map<Long, Object[]> map) {
		if (map == null) {
			return null;
		}
		Map<Long, Object[]> mapUntukSettingIni = null;
		for (Map.Entry<Long, Object[]> entry : map.entrySet()) {
			Object obj = entry.getValue()[0];
			DetailBiaya detailBiayaCek = (obj instanceof PengaturanPembayaranBulanan)
					? ((PengaturanPembayaranBulanan) obj).getDetailBiaya()
					: (obj instanceof DetailBiaya ? (DetailBiaya) obj : null);
			if (detailBiayaCek != null && detailBiayaCek.getSettingBiaya() != null
					&& detailBiayaCek.getSettingBiaya().getId() != null && settingBiaya.getId() != null
					&& detailBiayaCek.getSettingBiaya().getId().equals(settingBiaya.getId())) {
				if (mapUntukSettingIni == null) {
					mapUntukSettingIni = new HashMap<Long, Object[]>();
				}
				mapUntukSettingIni.put(entry.getKey(), entry.getValue());
			}
		}
		return mapUntukSettingIni;
	}

	/**
	 * Tentukan tanggal tagihan efektif satu item ({@code data}), dengan urutan prioritas:
	 * (1) tanggal {@link DetailKegiatan} yang benar-benar sudah tersimpan untuk item ini, bila
	 * ada; (2) bila belum ada Kegiatan/DetailKegiatan tersimpan, tanggal default yang dihitung
	 * dari sebuah {@link DetailKegiatan} placeholder (tidak disimpan, hanya untuk memanggil
	 * getter tanggal defaultnya); (3) fallback terakhir {@code DetailBiaya.getDefaultTanggalTagihan()}.
	 *
	 * @param data             elemen {@code Object[]{DetailBiaya atau PengaturanPembayaranBulanan}}.
	 * @param kegiatan         Kegiatan aktif mahasiswa/calon (boleh null bila belum ada).
	 * @param detailKegiatans  daftar DetailKegiatan milik Kegiatan tsb (boleh null).
	 * @param refresh          diteruskan ke {@link Kegiatan#ambilSatuDetailKegiatan} untuk
	 *                         memaksa baca ulang dari DB bila perlu.
	 * @return tanggal efektif, atau {@code null} bila {@code data} kosong/tidak dikenali.
	 */
	private Date ambilTanggalTagihanEfektif(Object[] data, Kegiatan kegiatan,
			Collection<DetailKegiatan> detailKegiatans, boolean refresh) {
		if (data == null || data.length == 0 || data[0] == null) {
			return null;
		}
		DetailBiaya detailBiaya = null;
		PengaturanPembayaranBulanan pengaturanPembayaranBulanan = null;
		if (data[0] instanceof PengaturanPembayaranBulanan) {
			pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) data[0];
			detailBiaya = pengaturanPembayaranBulanan.getDetailBiaya();
		} else if (data[0] instanceof DetailBiaya) {
			detailBiaya = (DetailBiaya) data[0];
		}
		if (detailBiaya == null) {
			return null;
		}

		DetailKegiatan detailKegiatan = kegiatan == null || kegiatan.getId() == null ? null
				: (pengaturanPembayaranBulanan != null
						? kegiatan.ambilSatuDetailKegiatan(pengaturanPembayaranBulanan, detailKegiatans)
						: kegiatan.ambilSatuDetailKegiatan(detailBiaya, refresh));
		if (detailKegiatan != null && detailKegiatan.getTanggal() != null) {
			return detailKegiatan.getTanggal();
		}

		if (kegiatan != null) {
			DetailKegiatan detailKegiatanDefault = new DetailKegiatan();
			detailKegiatanDefault.setKegiatan(kegiatan);
			detailKegiatanDefault.setDetailBiaya(detailBiaya);
			detailKegiatanDefault.setPengaturanPembayaranBulanan(pengaturanPembayaranBulanan);
			Date tanggal = detailKegiatanDefault.getTanggal();
			if (tanggal != null) {
				return tanggal;
			}
		}
		return detailBiaya.getDefaultTanggalTagihan();
	}

	/**
	 * Render kolom "Tanggal Tagihan" satu baris: kumpulkan tanggal efektif
	 * ({@link #ambilTanggalTagihanEfektif}) tiap {@link ItemBiaya} yang dipilih layar
	 * ({@link #selectedItemBiaya}), di-dedup lewat {@link LinkedHashSet} agar tanggal yang sama
	 * tidak diulang. Bila lebih dari satu ItemBiaya dipilih, tiap baris tanggal diberi prefix
	 * nama item agar jelas tanggal mana milik item mana. Tampilkan "-" bila tidak ada tanggal
	 * sama sekali.
	 *
	 * @param row            baris grid tujuan.
	 * @param kegiatan       Kegiatan aktif mahasiswa/calon (boleh null).
	 * @param detailKegiatans daftar DetailKegiatan milik Kegiatan tsb (boleh null).
	 * @param datas          peta {@code idItemBiaya -> (idDetailBiayaAtauPpb -> Object[])} hasil resolusi.
	 * @param refresh        diteruskan ke {@link #ambilTanggalTagihanEfektif}.
	 * @param saringSetting  bila {@code true}, saring dulu tiap map lewat {@link #saringMapUntukSettingIni}
	 *                       agar hanya tanggal tagihan milik {@link #settingBiaya} ini yang ditampilkan.
	 */
	private void renderTanggalTagihan(Row row, Kegiatan kegiatan, Collection<DetailKegiatan> detailKegiatans,
			Map<Long, Map<Long, Object[]>> datas, boolean refresh, boolean saringSetting) {
		Vbox vbox = new Vbox();
		vbox.setWidth("99%");
		vbox.setParent(row);
		Set<String> tanggalLabels = new LinkedHashSet<String>();
		boolean tampilkanNamaItem = selectedItemBiaya != null && selectedItemBiaya.size() > 1;
		if (selectedItemBiaya != null) {
			for (ItemBiaya itemBiaya : selectedItemBiaya) {
				Map<Long, Object[]> map = datas == null ? null : datas.get(itemBiaya.getId());
				if (saringSetting) {
					map = saringMapUntukSettingIni(map);
				}
				if (map == null || map.isEmpty()) {
					continue;
				}
				for (Object[] value : map.values()) {
					Date tanggal = ambilTanggalTagihanEfektif(value, kegiatan, detailKegiatans, refresh);
					if (tanggal != null) {
						String label = Common.dateFormat1.get().format(tanggal);
						tanggalLabels.add(tampilkanNamaItem ? itemBiaya.getNama() + ": " + label : label);
					}
				}
			}
		}
		if (tanggalLabels.isEmpty()) {
			vbox.appendChild(new Label("-"));
		} else {
			for (String label : tanggalLabels) {
				vbox.appendChild(new Label(label));
			}
		}
	}

	/**
	 * Renderer baris grid mode "daftar mahasiswa reguler": untuk tiap {@link Mahasiswa}, hitung
	 * semester berjalan lalu resolusi Kegiatan+tagihan aktifnya via
	 * {@link #resolveKegiatanMahasiswa}, tampilkan biodata ringkas (foto, NIM/nama, HP, email,
	 * angkatan, jurusan, program, semester), kolom tanggal tagihan
	 * ({@link #renderTanggalTagihan}), lalu satu kolom per {@link ItemBiaya} terpilih berisi
	 * nominal tagihan aktif (disaring khusus milik {@link #settingBiaya} ini lewat
	 * {@link #saringMapUntukSettingIni}) atau label "Tidak/belum ada" bila item tsb tidak
	 * relevan untuk mahasiswa ini. Native Hibernate session dibuka per baris dan
	 * di-disconnect/close di akhir render agar tidak menumpuk koneksi saat grid berisi banyak
	 * baris.
	 *
	 * @see DetailSettingBiayaAction
	 */
	class MahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		/** Bila true, paksa resolusi Kegiatan/tagihan baca ulang dari DB (bukan cache). */
		private boolean refresh;
		/** Bila true, reset tagihan ke default billing saat resolusi (dipakai tombol Reset). */
		private boolean rst;

		public MahasiswaRenderer(boolean refresh, boolean rst) {
			this.refresh = refresh;
			this.rst = rst;
		}

		/**
		 * Render satu baris mahasiswa — lihat Javadoc kelas {@link MahasiswaRenderer} untuk
		 * detail kolom dan pengelolaan session.
		 *
		 * @param arg0 baris grid tujuan.
		 * @param data data baris, di-cast ke {@link Mahasiswa}.
		 */
		@SuppressWarnings("rawtypes")
		@Override
		public void render(final Row arg0, Object data) throws Exception {
			Mahasiswa mahasiswa = (Mahasiswa) data;

			Integer smt = Common.getSemester(mahasiswa.getTahunangkatan(),
					tahunAkademik.getSelectedItem().getValue().toString(),
					genapGanjil.getSelectedItem().getValue().toString(), mahasiswa.getPindahKeKampusIniMasukSemester(),
					mahasiswa.getSemesterMulai());

			Session session = HibernateUtil.currentNativeSession();
			try {
				Object[] resolusi = resolveKegiatanMahasiswa(mahasiswa, smt, refresh, rst);
				final Kegiatan kegiatan = (Kegiatan) resolusi[0];
				@SuppressWarnings("unchecked")
				Collection<DetailKegiatan> detailKegiatans = (Collection<DetailKegiatan>) resolusi[1];
				@SuppressWarnings("unchecked")
				Map<Long, Map<Long, Object[]>> datas = (Map<Long, Map<Long, Object[]>>) resolusi[2];

				Hbox ahbox = new Hbox();
				ahbox.setParent(arg0);
				CommonMedia.tampilkanGambarKecil(mahasiswa).setParent(ahbox);
				Vbox a = new Vbox();
				a.setParent(ahbox);
				new Label(mahasiswa.getNim() + " / " + mahasiswa.getNama()).setParent(a);

				mahasiswa.tampilkanHp(a);
				mahasiswa.tampilkanEmail(a);

				a = new Vbox();
				a.setParent(ahbox);

				new Label("Angkatan:" + mahasiswa.getTahunangkatan() + "").setParent(a);
				new Label(mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getNama()).setParent(a);
				new Label(mahasiswa.getProgram()).setParent(a);
				new Label("Smt:" + smt + "").setParent(a);

				renderTanggalTagihan(arg0, kegiatan, detailKegiatans, datas, refresh, true);

				for (final ItemBiaya itemBiaya : selectedItemBiaya) {

					Map<Long, Object[]> mapUntukSettingIni = saringMapUntukSettingIni(datas.get(itemBiaya.getId()));

					if (mapUntukSettingIni != null) {
						Vbox vbox = new Vbox();
						vbox.setWidth("99%");
						vbox.setParent(arg0);
						renderSatuItemTagihanAktif(vbox, kegiatan, detailKegiatans, mapUntukSettingIni, refresh);
					} else {
						new Label("Tidak/belum ada").setParent(arg0);
					}
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/DetailSettingBiayaAction.java:353");
			}

			// session.disconnect();
			if (session.isOpen()) {session.disconnect();session.close();}
			HibernateUtil.closeSession();
		}
	}

	/**
	 * Renderer baris grid mode "khusus per mahasiswa": sumber baris adalah
	 * {@link SettingBiayaDetail} (binding SettingBiaya-mahasiswa dengan template kuota custom
	 * per orang, kolom JSON {@code biayas}). Untuk tiap baris ditampilkan: biodata ringkas,
	 * editor semester berlaku ({@code minSmt}/{@code maxSmt}, disimpan langsung
	 * {@link Common#refreshUpdate} tiap kali diubah — bukan menunggu tombol Simpan), kolom
	 * tanggal tagihan, lalu PER item biaya terpilih DUA baris: "Kuota Custom (Template)" — nilai
	 * default JSON yang dipakai HANYA saat tagihan pertama kali diterbitkan — dan "Tagihan Aktif"
	 * — nominal {@link DetailKegiatan} yang SUDAH berjalan (resolusi sekali di awal render lewat
	 * {@link #resolveKegiatanMahasiswa}), tempat admin bisa menurunkan nominal tagihan manual
	 * per mahasiswa (lihat Javadoc kelas {@link DetailSettingBiayaAction} untuk contoh kasus
	 * Sumbangan Bangunan). Tombol Hapus baris memanggil {@link #bolehHapusSettingBiayaDetail}
	 * sebelum {@link Common#refreshDelete}.
	 *
	 * @see DetailSettingBiayaAction
	 */
	class MahasiswaSettingRenderer extends ais.ui.util.MyRowRenderer {

		/** Bila true, paksa resolusi Kegiatan/tagihan aktif baca ulang dari DB. */
		private boolean refresh;
		/** Bila true, reset tagihan ke default billing saat resolusi. */
		private boolean rst;

		public MahasiswaSettingRenderer(boolean refresh, boolean rst) {
			this.refresh = refresh;
			this.rst = rst;
		}

		/**
		 * Render satu baris {@link SettingBiayaDetail} — lihat Javadoc kelas
		 * {@link MahasiswaSettingRenderer} untuk detail kolom Kuota Custom vs Tagihan Aktif.
		 *
		 * @param arg0 baris grid tujuan.
		 * @param data data baris, di-cast ke {@link SettingBiayaDetail}.
		 */
		@Override
		public void render(final Row arg0, Object data) throws Exception {
			// TODO Auto-generated method stub
			final SettingBiayaDetail settingBiayaDetail = (SettingBiayaDetail) data;
			final Mahasiswa mahasiswa = settingBiayaDetail.getMahasiswa();

			// FITUR: nominal Tagihan Aktif (DetailKegiatan yang SUDAH berjalan) boleh diubah
			// manual per mahasiswa di sini — resolusi dilakukan SEKALI di awal (dipakai di
			// dalam loop item biaya di bawah) via method yang SAMA dgn grid mahasiswa reguler,
			// sehingga tagihan "custom per mahasiswa" (mis. Sumbangan Bangunan Tahap II) bisa
			// diturunkan sesuai kesanggupan mahasiswa dan otomatis dianggap Lunas.
			Kegiatan kegiatanAktif = null;
			Collection<DetailKegiatan> detailKegiatansAktif = null;
			Map<Long, Map<Long, Object[]>> datasAktif = new HashMap<Long, Map<Long, Object[]>>();
			try {
				Integer smtAktif = Common.getSemester(mahasiswa.getTahunangkatan(),
						tahunAkademik.getSelectedItem().getValue().toString(),
						genapGanjil.getSelectedItem().getValue().toString(),
						mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai());
				Object[] resolusi = resolveKegiatanMahasiswa(mahasiswa, smtAktif, refresh, rst);
				kegiatanAktif = (Kegiatan) resolusi[0];
				@SuppressWarnings("unchecked")
				Collection<DetailKegiatan> dk = (Collection<DetailKegiatan>) resolusi[1];
				detailKegiatansAktif = dk;
				@SuppressWarnings("unchecked")
				Map<Long, Map<Long, Object[]>> ds = (Map<Long, Map<Long, Object[]>>) resolusi[2];
				datasAktif = ds;
			} catch (Exception eAktif) {
				ais.common.ErrorAuditUtil.record(eAktif,
						"DetailSettingBiayaAction.MahasiswaSettingRenderer: gagal resolusi tagihan aktif; mahasiswa="
								+ mahasiswa.getNim());
			}
			final Kegiatan kegiatanAktifFinal = kegiatanAktif;
			final Collection<DetailKegiatan> detailKegiatansAktifFinal = detailKegiatansAktif;
			final Map<Long, Map<Long, Object[]>> datasAktifFinal = datasAktif;

			try {

				Hbox ahbox = new Hbox();
				ahbox.setParent(arg0);
				CommonMedia.tampilkanGambarKecil(mahasiswa).setParent(ahbox);
				Vbox a = new Vbox();
				a.setParent(ahbox);
				new Label(mahasiswa.getNim() + " / " + mahasiswa.getNama()).setParent(a);

				mahasiswa.tampilkanHp(a);
				mahasiswa.tampilkanEmail(a);

				a = new Vbox();
				a.setParent(ahbox);

				new Label("Angkatan:" + mahasiswa.getTahunangkatan() + "").setParent(a);
				new Label(mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getNama()).setParent(a);
				new Label(mahasiswa.getProgram()).setParent(a);

				final Intbox minSmt = new Intbox(settingBiayaDetail.getMinSmt());
				final Intbox maxSmt = new Intbox(settingBiayaDetail.getMaxSmt());
				minSmt.setCols(2);
				maxSmt.setCols(2);
				Hbox hbox = new Hbox();
				hbox.setParent(a);
				new Label(ais.common.Common.getBahasaConfig("Semester :")).setParent(hbox);
				minSmt.setParent(hbox);
				minSmt.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						settingBiayaDetail.setMinSmt(minSmt.getValue());
						Common.refreshUpdate(settingBiayaDetail);
					}
				});
				new Label(ais.common.Common.getBahasaConfig("sd")).setParent(hbox);
				maxSmt.setParent(hbox);
				maxSmt.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						settingBiayaDetail.setMaxSmt(maxSmt.getValue());
						Common.refreshUpdate(settingBiayaDetail);
					}
				});

				final JSONObject jsonObject = new JSONObject(settingBiayaDetail.getBiayas());

				renderTanggalTagihan(arg0, kegiatanAktifFinal, detailKegiatansAktifFinal, datasAktifFinal, refresh,
						true);

				for (final ItemBiaya itemBiaya : selectedItemBiaya) {

					Vbox vboxItem = new Vbox();
					vboxItem.setWidth("99%");
					vboxItem.setParent(arg0);

					vboxItem.appendChild(new Label(ais.common.Common.getBahasaConfig("Kuota Custom (Template)")));
					final MyDoublebox myDoublebox = new MyDoublebox(
							jsonObject.isNull(itemBiaya.getId().toString()) ? 0.0
									: jsonObject.getDouble(itemBiaya.getId().toString()));
					myDoublebox.setWidth("95%");
					myDoublebox.setParent(vboxItem);
					myDoublebox.addEventListener("onChange", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							jsonObject.put(itemBiaya.getId().toString(),
									myDoublebox.getValue() == null ? 0.0 : myDoublebox.getValue());
							settingBiayaDetail.setBiayas(jsonObject.toString());
							Common.refreshUpdate(settingBiayaDetail);
						}
					});

					// NOMINAL TAGIHAN AKTIF: DetailKegiatan yang SUDAH berjalan (bila ada) — di
					// sinilah admin boleh menurunkan nominal tagihan mahasiswa ini secara manual
					// (mis. Sumbangan Bangunan Tahap II dari Rp4.000.000 menjadi Rp1.000.000
					// sesuai kesanggupannya), berbeda dengan "Kuota Custom" di atas yang hanya
					// nilai DEFAULT dipakai SAAT tagihan pertama kali diterbitkan.
					vboxItem.appendChild(new Label(ais.common.Common.getBahasaConfig("Tagihan Aktif")));
					Map<Long, Object[]> mapAktif = datasAktifFinal.get(itemBiaya.getId());
					if (mapAktif != null) {
						renderSatuItemTagihanAktif(vboxItem, kegiatanAktifFinal, detailKegiatansAktifFinal, mapAktif,
								refresh);
					} else {
						new Label(ais.common.Common.getBahasaConfig("Belum ada tagihan aktif")).setParent(vboxItem);
					}
				}

				// Kolom aksi rapi (pola MahasiswaAction): semua tombol dibungkus kebab popup (⋯)
				// via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten antar layar.
				final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
						new java.util.ArrayList<org.zkoss.zk.ui.Component>();
				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
				button.setTooltiptext("Hapus Data");
				button.addEventListener("onClick", new EventListener() {
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

											if (!bolehHapusSettingBiayaDetail(settingBiayaDetail)) {
												loadData(null);
												return;
											}
												Common.refreshDelete(settingBiayaDetail);

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
				aksiButtons.add(button);
				ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);

			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/DetailSettingBiayaAction.java:474");
			}

		}
	}

	/**
	 * Padanan {@link MahasiswaSettingRenderer} untuk {@link BiodataCalonMahasiswa} — sumber
	 * baris tetap {@link SettingBiayaDetail}, tapi relasinya ke
	 * {@code settingBiayaDetail.getBiodataCalonMahasiswa()} dan resolusi tagihan aktif lewat
	 * {@link #resolveKegiatanCalonMahasiswa}. Lihat Javadoc {@link MahasiswaSettingRenderer}
	 * untuk penjelasan lengkap kolom Kuota Custom vs Tagihan Aktif dan alasan penurunan nominal
	 * manual per orang.
	 *
	 * @see DetailSettingBiayaAction
	 */
	class CalonMahasiswaSettingRenderer extends ais.ui.util.MyRowRenderer {

		/** Bila true, paksa resolusi Kegiatan/tagihan aktif baca ulang dari DB. */
		private boolean refresh;
		/** Bila true, reset tagihan ke default billing saat resolusi. */
		private boolean rst;

		public CalonMahasiswaSettingRenderer(boolean refresh, boolean rst) {
			this.refresh = refresh;
			this.rst = rst;
		}

		/**
		 * Render satu baris {@link SettingBiayaDetail} milik calon mahasiswa — lihat Javadoc
		 * kelas {@link CalonMahasiswaSettingRenderer}/{@link MahasiswaSettingRenderer}.
		 *
		 * @param arg0 baris grid tujuan.
		 * @param data data baris, di-cast ke {@link SettingBiayaDetail}.
		 */
		@Override
		public void render(final Row arg0, Object data) throws Exception {
			// TODO Auto-generated method stub
			final SettingBiayaDetail settingBiayaDetail = (SettingBiayaDetail) data;
			final BiodataCalonMahasiswa mahasiswa = settingBiayaDetail.getBiodataCalonMahasiswa();

			// FITUR: nominal Tagihan Aktif (DetailKegiatan yang SUDAH berjalan) boleh diubah
			// manual per calon mahasiswa di sini — lihat penjelasan lengkap di javadoc
			// MahasiswaSettingRenderer (padanan untuk mahasiswa aktif).
			Kegiatan kegiatanAktif = null;
			Collection<DetailKegiatan> detailKegiatansAktif = null;
			Map<Long, Map<Long, Object[]>> datasAktif = new HashMap<Long, Map<Long, Object[]>>();
			try {
				Integer smtAktif = Common.getSemester(mahasiswa.getTahun(),
						tahunAkademik.getSelectedItem().getValue().toString(),
						genapGanjil.getSelectedItem().getValue().toString(),
						mahasiswa.getPindahDariKampusLamaDiSemester(), mahasiswa.getSemesterMulai());
				Object[] resolusi = resolveKegiatanCalonMahasiswa(mahasiswa, smtAktif, refresh, rst);
				kegiatanAktif = (Kegiatan) resolusi[0];
				@SuppressWarnings("unchecked")
				Collection<DetailKegiatan> dk = (Collection<DetailKegiatan>) resolusi[1];
				detailKegiatansAktif = dk;
				@SuppressWarnings("unchecked")
				Map<Long, Map<Long, Object[]>> ds = (Map<Long, Map<Long, Object[]>>) resolusi[2];
				datasAktif = ds;
			} catch (Exception eAktif) {
				ais.common.ErrorAuditUtil.record(eAktif,
						"DetailSettingBiayaAction.CalonMahasiswaSettingRenderer: gagal resolusi tagihan aktif; noRegistrasi="
								+ mahasiswa.getNoRegistrasi());
			}
			final Kegiatan kegiatanAktifFinal = kegiatanAktif;
			final Collection<DetailKegiatan> detailKegiatansAktifFinal = detailKegiatansAktif;
			final Map<Long, Map<Long, Object[]>> datasAktifFinal = datasAktif;

			try {

				Hbox ahbox = new Hbox();
				ahbox.setParent(arg0);
				CommonMedia.tampilkanGambarKecil(mahasiswa).setParent(ahbox);
				Vbox a = new Vbox();
				a.setParent(ahbox);
				new Label(mahasiswa.getNoRegistrasi() + " / " + mahasiswa.getNama()).setParent(a);

				mahasiswa.tampilkanHp(a);
				mahasiswa.tampilkanEmail(a);

				a = new Vbox();
				a.setParent(ahbox);

				final Intbox minSmt = new Intbox(settingBiayaDetail.getMinSmt());
				final Intbox maxSmt = new Intbox(settingBiayaDetail.getMaxSmt());
				minSmt.setCols(2);
				maxSmt.setCols(2);
				new Label("Angkatan:" + mahasiswa.getTahun() + "").setParent(a);
				new Label(mahasiswa.getGelombangPendaftaran() == null ? ""
						: mahasiswa.getGelombangPendaftaran().getNama()).setParent(a);
				new Label(mahasiswa.getProgram()).setParent(a);
				Hbox hbox = new Hbox();
				hbox.setParent(a);
				new Label(ais.common.Common.getBahasaConfig("Semester :")).setParent(hbox);
				minSmt.setParent(hbox);
				minSmt.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						settingBiayaDetail.setMinSmt(minSmt.getValue());
						Common.refreshUpdate(settingBiayaDetail);
					}
				});
				new Label(ais.common.Common.getBahasaConfig("sd")).setParent(hbox);
				maxSmt.setParent(hbox);
				maxSmt.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						settingBiayaDetail.setMaxSmt(maxSmt.getValue());
						Common.refreshUpdate(settingBiayaDetail);
					}
				});

				final JSONObject jsonObject = new JSONObject(settingBiayaDetail.getBiayas());

				renderTanggalTagihan(arg0, kegiatanAktifFinal, detailKegiatansAktifFinal, datasAktifFinal, refresh,
						true);

				for (final ItemBiaya itemBiaya : selectedItemBiaya) {

					Vbox vboxItem = new Vbox();
					vboxItem.setWidth("99%");
					vboxItem.setParent(arg0);

					vboxItem.appendChild(new Label(ais.common.Common.getBahasaConfig("Kuota Custom (Template)")));
					final MyDoublebox myDoublebox = new MyDoublebox(
							jsonObject.isNull(itemBiaya.getId().toString()) ? 0.0
									: jsonObject.getDouble(itemBiaya.getId().toString()));
					myDoublebox.setWidth("95%");
					myDoublebox.setParent(vboxItem);
					myDoublebox.addEventListener("onChange", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							jsonObject.put(itemBiaya.getId().toString(),
									myDoublebox.getValue() == null ? 0.0 : myDoublebox.getValue());
							settingBiayaDetail.setBiayas(jsonObject.toString());
							Common.refreshUpdate(settingBiayaDetail);
						}
					});

					// NOMINAL TAGIHAN AKTIF: lihat penjelasan lengkap di MahasiswaSettingRenderer.
					vboxItem.appendChild(new Label(ais.common.Common.getBahasaConfig("Tagihan Aktif")));
					Map<Long, Object[]> mapAktif = datasAktifFinal.get(itemBiaya.getId());
					if (mapAktif != null) {
						renderSatuItemTagihanAktif(vboxItem, kegiatanAktifFinal, detailKegiatansAktifFinal, mapAktif,
								refresh);
					} else {
						new Label(ais.common.Common.getBahasaConfig("Belum ada tagihan aktif")).setParent(vboxItem);
					}
				}

				// Kolom aksi rapi (pola MahasiswaAction): semua tombol dibungkus kebab popup (⋯)
				// via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten antar layar.
				final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
						new java.util.ArrayList<org.zkoss.zk.ui.Component>();
				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
				button.setTooltiptext("Hapus Data");
				button.addEventListener("onClick", new EventListener() {
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

											if (!bolehHapusSettingBiayaDetail(settingBiayaDetail)) {
												loadData(null);
												return;
											}
												Common.refreshDelete(settingBiayaDetail);

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
				aksiButtons.add(button);
				ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);

			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/DetailSettingBiayaAction.java:592");
			}

		}
	}

	/**
	 * Padanan {@link #resolveKegiatanMahasiswa} untuk {@link BiodataCalonMahasiswa} —
	 * lihat javadoc method tersebut untuk penjelasan lengkap (dipakai bersama oleh
	 * {@link CalonMahasiswaRenderer} dan {@link CalonMahasiswaSettingRenderer}).
	 */
	@SuppressWarnings("rawtypes")
	private Object[] resolveKegiatanCalonMahasiswa(BiodataCalonMahasiswa biodataCalonMahasiswa, Integer smt,
			boolean refresh, boolean rst) throws Exception {
		PembayaranUtil pembayaranUtilLokal = PembayaranUtil.getInstance();
		JenisKegiatan jenisKegiatan = settingBiaya.getJenisKegiatan();

		ArrayList<DetailBiaya> detailBiayas = new ArrayList<DetailBiaya>();
		if (jenisKegiatan.getId().equals(ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU.getId())) {
			Jurusan prodiLulus = biodataCalonMahasiswa.getProdiLulus();
			if (prodiLulus == null || prodiLulus.getId() == null) {
				Jurusan myjurusan1 = biodataCalonMahasiswa.getProdi1() == null ? biodataCalonMahasiswa.getProdi2()
						: biodataCalonMahasiswa.getProdi1();
				java.util.Collection<DetailBiaya> detailBiayas1 = PembayaranUtilHelper.getDetailBiayaCalonMahasiswa(
						biodataCalonMahasiswa, jenisKegiatan, myjurusan1, smt, refresh);

				detailBiayas.addAll(detailBiayas1);
			} else {
				java.util.Collection<DetailBiaya> detailBiayas1 = PembayaranUtilHelper.getDetailBiayaCalonMahasiswa(
						biodataCalonMahasiswa, jenisKegiatan, prodiLulus, smt, refresh);
				detailBiayas.addAll(detailBiayas1);
			}
		} else if (jenisKegiatan.getId().equals(ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId())) {
			Jurusan prodiLulus = biodataCalonMahasiswa.getProdiLulus();

			if (prodiLulus == null || prodiLulus.getId() == null) {
				Jurusan myjurusan1 = biodataCalonMahasiswa.getProdi1() == null ? biodataCalonMahasiswa.getProdi2()
						: biodataCalonMahasiswa.getProdi1();
				java.util.Collection<DetailBiaya> detailBiayas1 = PembayaranUtilHelper
						.getDetailBiayaCalonMahasiswa(biodataCalonMahasiswa, jenisKegiatan, myjurusan1, refresh);
				detailBiayas.addAll(detailBiayas1);
			} else {
				java.util.Collection<DetailBiaya> detailBiayas1 = PembayaranUtilHelper
						.getDetailBiayaCalonMahasiswa(biodataCalonMahasiswa, jenisKegiatan, prodiLulus, refresh);
				detailBiayas.addAll(detailBiayas1);
			}
		}

		Session session = HibernateUtil.currentNativeSession();
		int countPengaturanBulanan = PembayaranUtilHelper.countBulanan(session, biodataCalonMahasiswa,
				jenisKegiatan, smt, detailBiayas, refresh, true);

		Collection biayaBulanan = null;
		if (countPengaturanBulanan > 0) {
			biayaBulanan = pembayaranUtilLokal.getPengaturanPembayaranSemua(biodataCalonMahasiswa,
					HibernateUtil.currentNativeSession(), smt, jenisKegiatan, detailBiayas, refresh, true);
		}
		Object[] ooo = (biayaBulanan != null ? biayaBulanan.toArray() : detailBiayas.toArray());

		boolean ulang = true;
		ItemBiaya item = null;
		// Helper-helper di atas dapat menutup native session -> ambil ulang sebelum dipakai.
		session = HibernateUtil.currentNativeSession();
		final Kegiatan kegiatan = KegiatanHelper.checkKegiatanCalonMahasiswa(jenisKegiatan,
				biodataCalonMahasiswa, smt, tahunAkademik.getSelectedItem().getValue().toString(), ulang, rst,
				item, session);

		final Collection<DetailKegiatan> detailKegiatans = kegiatan == null || kegiatan.getId() == null ? null
				: kegiatan.ambilDetailKegiatan(refresh);

		Map<Long, Map<Long, Object[]>> datas = new HashMap<Long, Map<Long, Object[]>>();
		for (Object o : ooo) {
			DetailBiaya detailBiaya = null;
			PengaturanPembayaranBulanan pengaturanPembayaranBulanan = null;

			if (o instanceof DetailBiaya) {
				detailBiaya = (DetailBiaya) o;

				Map<Long, Object[]> map = datas.get(detailBiaya.getItemBiaya().getId());
				if (map == null) {
					map = new HashMap<Long, Object[]>();
					datas.put(detailBiaya.getItemBiaya().getId(), map);
				}
				map.put(detailBiaya.getId(), new Object[] { detailBiaya });
			} else if (o instanceof PengaturanPembayaranBulanan) {
				pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) o;
				detailBiaya = pengaturanPembayaranBulanan.getDetailBiaya();

				Map<Long, Object[]> map = datas.get(detailBiaya.getItemBiaya().getId());
				if (map == null) {
					map = new HashMap<Long, Object[]>();
					datas.put(detailBiaya.getItemBiaya().getId(), map);
				}
				map.put(pengaturanPembayaranBulanan.getId(), new Object[] { pengaturanPembayaranBulanan });
			}

		}
		return new Object[] { kegiatan, detailKegiatans, datas };
	}

	/**
	 * Padanan {@link MahasiswaRenderer} untuk daftar calon mahasiswa reguler (bukan mode
	 * "khusus per mahasiswa"): sumber baris {@link BiodataCalonMahasiswa} langsung, resolusi
	 * tagihan aktif lewat {@link #resolveKegiatanCalonMahasiswa}, kolom sama (biodata ringkas,
	 * tanggal tagihan, nominal per item biaya). Jurusan yang ditampilkan diprioritaskan dari
	 * {@code prodiLulus}, fallback ke {@code prodi1}.
	 *
	 * @see DetailSettingBiayaAction
	 */
	class CalonMahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		/** Bila true, paksa resolusi Kegiatan/tagihan aktif baca ulang dari DB. */
		private boolean refresh;
		/** Bila true, reset tagihan ke default billing saat resolusi. */
		private boolean rst;

		public CalonMahasiswaRenderer(boolean refresh, boolean rst) {
			this.refresh = refresh;
			this.rst = rst;
		}

		/**
		 * Render satu baris calon mahasiswa — lihat Javadoc kelas {@link CalonMahasiswaRenderer}.
		 *
		 * @param arg0 baris grid tujuan.
		 * @param data data baris, di-cast ke {@link BiodataCalonMahasiswa}.
		 */
		@SuppressWarnings("rawtypes")
		@Override
		public void render(final Row arg0, Object data) throws Exception {
			BiodataCalonMahasiswa biodataCalonMahasiswa = (BiodataCalonMahasiswa) data;

			Integer smt = Common.getSemester(biodataCalonMahasiswa.getTahun(),
					tahunAkademik.getSelectedItem().getValue().toString(),
					genapGanjil.getSelectedItem().getValue().toString(),
					biodataCalonMahasiswa.getPindahDariKampusLamaDiSemester(),
					biodataCalonMahasiswa.getSemesterMulai());

			Session session = HibernateUtil.currentNativeSession();
			try {
				Object[] resolusi = resolveKegiatanCalonMahasiswa(biodataCalonMahasiswa, smt, refresh, rst);
				final Kegiatan kegiatan = (Kegiatan) resolusi[0];
				@SuppressWarnings("unchecked")
				Collection<DetailKegiatan> detailKegiatans = (Collection<DetailKegiatan>) resolusi[1];
				@SuppressWarnings("unchecked")
				Map<Long, Map<Long, Object[]>> datas = (Map<Long, Map<Long, Object[]>>) resolusi[2];

				Hbox ahbox = new Hbox();
				ahbox.setParent(arg0);
				CommonMedia.tampilkanGambarKecil(biodataCalonMahasiswa).setParent(ahbox);
				Vbox a = new Vbox();
				a.setParent(ahbox);
				new Label(biodataCalonMahasiswa.getNoRegistrasi() + " / " + biodataCalonMahasiswa.getNama())
						.setParent(a);

				biodataCalonMahasiswa.tampilkanHp(a);
				biodataCalonMahasiswa.tampilkanEmail(a);

				a = new Vbox();
				a.setParent(ahbox);

				Jurusan jurusan = biodataCalonMahasiswa.getProdiLulus() == null ? biodataCalonMahasiswa.getProdi1()
						: biodataCalonMahasiswa.getProdiLulus();

				new Label("Angkatan:" + biodataCalonMahasiswa.getTahun() + "").setParent(a);
				new Label(jurusan == null ? "" : jurusan.getNama()).setParent(a);
				new Label(biodataCalonMahasiswa.getProgram()).setParent(a);
				new Label("Smt:" + smt + "").setParent(a);

				renderTanggalTagihan(arg0, kegiatan, detailKegiatans, datas, refresh, true);

				for (final ItemBiaya itemBiaya : selectedItemBiaya) {

					Map<Long, Object[]> mapUntukSettingIni = saringMapUntukSettingIni(datas.get(itemBiaya.getId()));

					if (mapUntukSettingIni != null) {
						Vbox vbox = new Vbox();
						vbox.setWidth("99%");
						vbox.setParent(arg0);
						renderSatuItemTagihanAktif(vbox, kegiatan, detailKegiatans, mapUntukSettingIni, refresh);
					} else {
						new Label("Tidak/belum ada").setParent(arg0);
					}
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/DetailSettingBiayaAction.java:802");
			}

			// session.disconnect();
			if (session.isOpen()) {session.disconnect();session.close();}
			HibernateUtil.closeSession();
		}
	}

	/**
	 * Muat ulang grid tanpa reset tagihan ke default.
	 *
	 * @param value bila instance {@link Boolean} {@code true}, paksa refresh resolusi tagihan
	 *              aktif dari DB (diteruskan sebagai parameter {@code refresh}); selain itu
	 *              dianggap {@code false}.
	 * @see #loadData(Object, boolean)
	 */
	public void loadData(Object value) {
		loadData(value, false);
	}

	/**
	 * Menyiapkan penghapusan binding mahasiswa/calon mahasiswa dari SettingBiaya.
	 *
	 * DetailBiaya yang belum pernah dipakai aman dihapus. Jika DetailBiaya sudah menjadi
	 * sumber tagihan/transaksi, baris SettingBiayaDetail harus tetap ada sebagai referensi
	 * historis; yang dilepas hanya mahasiswa/calon mahasiswanya. Dengan demikian mahasiswa
	 * hilang dari daftar khusus tanpa melanggar FK, tanpa mengubah template khusus menjadi
	 * template umum, dan tanpa menghapus tagihan maupun pembayaran lama.
	 */
	private boolean bolehHapusSettingBiayaDetail(SettingBiayaDetail settingBiayaDetail) throws Exception {
		if (settingBiayaDetail == null || settingBiayaDetail.getId() == null) {
			return true;
		}
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			@SuppressWarnings("unchecked")
			List<DetailBiaya> templates = session.createCriteria(DetailBiaya.class)
					.add(Restrictions.eq("settingBiayaDetail", settingBiayaDetail)).list();
			Transaction transaction = null;
			long jumlahDipakai = 0L;
			transaction = session.beginTransaction();
			try {
			for (DetailBiaya template : templates) {
				Number kegiatan = (Number) session.createCriteria(DetailKegiatan.class)
						.add(Restrictions.eq("detailBiaya", template))
						.setProjection(Projections.rowCount()).uniqueResult();
				Number cicilan = (Number) session.createCriteria(CicilanPembayaran.class)
						.add(Restrictions.eq("detailBiaya", template))
						.setProjection(Projections.rowCount()).uniqueResult();
				Number bulanan = (Number) session.createCriteria(PengaturanPembayaranBulanan.class)
						.add(Restrictions.eq("detailBiaya", template))
						.setProjection(Projections.rowCount()).uniqueResult();
				long dipakaiTemplate = (kegiatan == null ? 0L : kegiatan.longValue())
						+ (cicilan == null ? 0L : cicilan.longValue())
						+ (bulanan == null ? 0L : bulanan.longValue());
				jumlahDipakai += dipakaiTemplate;
				if (dipakaiTemplate == 0L) {
					session.delete(template);
				}
			}
				if (jumlahDipakai > 0L) {
					SettingBiayaDetail bindingHistoris = (SettingBiayaDetail) session.get(
							SettingBiayaDetail.class, settingBiayaDetail.getId());
					if (bindingHistoris != null) {
						bindingHistoris.setMahasiswa(null);
						bindingHistoris.setBiodataCalonMahasiswa(null);
						session.update(bindingHistoris);
					}
				}
				transaction.commit();
			} catch (Exception e) {
				if (transaction != null && transaction.isActive()) {
					transaction.rollback();
				}
				throw e;
			}
			if (jumlahDipakai > 0L) {
				System.out.println("[SETTING-BIAYA] Mahasiswa dilepas dari binding historis; "
						+ jumlahDipakai + " referensi tagihan/transaksi lama tetap dipertahankan.");
				// Binding tidak boleh dihapus karena masih direferensikan DetailBiaya. Nilai
				// false memberi tahu pemanggil bahwa proses sudah selesai tanpa refreshDelete.
				return false;
			}
			return true;
		} finally {
			if (session != null && session.isOpen()) {
				session.close();
			}
		}
	}

	/**
	 * Muat ulang isi grid sesuai halaman paging dan filter toolbar saat ini, memilih sumber data
	 * dan row renderer berdasarkan mode layar (lihat "Empat mode tampilan grid" di Javadoc kelas
	 * {@link DetailSettingBiayaAction}): {@link SettingBiayaDetail} (khusus calon/mahasiswa) via
	 * {@link CalonMahasiswaSettingRenderer}/{@link MahasiswaSettingRenderer}, atau
	 * {@link Mahasiswa}/{@link BiodataCalonMahasiswa} reguler via
	 * {@link MahasiswaRenderer}/{@link CalonMahasiswaRenderer}. Dijalankan lewat
	 * {@link Common#createDefaultTimer} (async, dengan indikator loading).
	 *
	 * @param value nilai instance {@link Boolean} menentukan flag {@code refresh} yang
	 *              diteruskan ke renderer (paksa baca ulang resolusi tagihan dari DB).
	 * @param reset flag {@code rst} yang diteruskan ke renderer — bila {@code true}, tagihan
	 *              aktif dikembalikan ke default billing saat resolusi (dipakai tombol Reset).
	 */
	@SuppressWarnings("unchecked")
	public void loadData(Object value, final boolean reset) {
		final boolean refresh = value != null && value instanceof Boolean ? ((Boolean) value) : false;
		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.initPaging15(initCriteria(false), paging);

				if (settingBiaya.getKhususBuatMahasiswaTertentu()
						&& (settingBiaya.getJenisKegiatan() != null
								&& ConstantValues.PENDAFTARAN_CALON_MAHASISWA != null
								&& settingBiaya.getJenisKegiatan().getId()
										.equals(ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId()))
						|| (settingBiaya.getJenisKegiatan() != null
								&& ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU != null
								&& settingBiaya.getJenisKegiatan().getId()
										.equals(ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU.getId()))) {
					List<SettingBiayaDetail> mahasiswas = ConstantValues
							.simpleList(
									initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE_15)
											.setFirstResult(Common.ROWS_COUNT_ON_PAGE_15
													* (paging == null ? 0 : paging.getActivePage())),
									SettingBiayaDetail.class);

					ListModel strset = new SimpleListModel(mahasiswas);

					grid.setRowRenderer(new CalonMahasiswaSettingRenderer(refresh, reset));
					grid.setModelCheckMobile(strset);

				} else if (modeDaftarMahasiswa()) {

					List<SettingBiayaDetail> mahasiswas = ConstantValues
							.simpleList(
									initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE_15)
											.setFirstResult(Common.ROWS_COUNT_ON_PAGE_15
													* (paging == null ? 0 : paging.getActivePage())),
									SettingBiayaDetail.class);

					ListModel strset = new SimpleListModel(mahasiswas);

					grid.setRowRenderer(new MahasiswaSettingRenderer(refresh, reset));
					grid.setModelCheckMobile(strset);

				}

				else if (mhs) {
					List<Mahasiswa> mahasiswas = ConstantValues.simpleList(
							initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE_15).setFirstResult(
									Common.ROWS_COUNT_ON_PAGE_15 * (paging == null ? 0 : paging.getActivePage())),
							Mahasiswa.class);

					ListModel strset = new SimpleListModel(mahasiswas);

					grid.setRowRenderer(new MahasiswaRenderer(refresh, reset));
					grid.setModelCheckMobile(strset);
				} else {
					List<BiodataCalonMahasiswa> mahasiswas = ConstantValues
							.simpleList(
									initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE_15)
											.setFirstResult(Common.ROWS_COUNT_ON_PAGE_15
													* (paging == null ? 0 : paging.getActivePage())),
									BiodataCalonMahasiswa.class);

					ListModel strset = new SimpleListModel(mahasiswas);

					grid.setRowRenderer(new CalonMahasiswaRenderer(refresh, reset));
					grid.setModelCheckMobile(strset);
				}
			}
		});
	}

	/**
	 * Bangun seluruh UI layar: toolbar filter (rentang angkatan/tahun, fakultas, jurusan,
	 * pencarian nama/NIM, TA/semester acuan untuk resolusi tagihan aktif), tombol khusus per
	 * mode (Ambil Mahasiswa/Calon Mahasiswa, Download/Upload template, Upload/Download Tagihan
	 * Aktif, Hapus Semua untuk mode "khusus"; Upload Tagihan + Refresh + Reset untuk mode
	 * reguler), kolom grid dinamis satu per {@link ItemBiaya} terpilih (di-load dari
	 * {@link DetailSettingBiaya} aktif milik {@link #settingBiaya}), lalu memanggil
	 * {@link #loadData(Object)} untuk mengisi baris pertama kali. Akses pembuatan Billing berada
	 * pada menu Action di baris induk Setting Biaya, bukan pada detail ini. Dipanggil dari listener
	 * {@code onOpen} di constructor, bukan langsung — memastikan layar sudah benar-benar
	 * ditampilkan sebelum UI berat ini dibangun.
	 */
	@SuppressWarnings("unchecked")
	public void display() {

		Session session = HibernateUtil.currentSession();
		selectedItemBiaya = ConstantValues.simpleList(session.createCriteria(DetailSettingBiaya.class)
				.setProjection(Projections.groupProperty("itemBiaya.id")).createAlias("itemBiaya", "itemBiaya")
				.add(Restrictions.or(Restrictions.isNull("itemBiaya.aktif"), Restrictions.eq("itemBiaya.aktif", true)))
				.add(Restrictions.eq("settingBiaya", settingBiaya)), ItemBiaya.class, false);

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(this);
		groupbox.appendChild(new MyCaptionStyled("Daftar tagihan " + (mhs ? "" : "calon") + " mahasiswa"));
		Toolbar toolbar = new Toolbar();
		toolbar.setParent(groupbox);

		searchtahun = new Combobox();
		searchtahun.setCols(2);

		searchtahunsd = new Combobox();
		searchtahunsd.setCols(2);

		searchtahun.setReadonly(true);
		searchtahunsd.setReadonly(true);

		int tahun = Calendar.getInstance().get(Calendar.YEAR);
		for (int i = (tahun - 25); i < (tahun + 25); i++) {
			Comboitem comboitem = new Comboitem(i + "");
			comboitem.setValue(i);
			searchtahun.appendChild(comboitem);
			comboitem = new Comboitem(i + "");
			comboitem.setValue(i);
			searchtahunsd.appendChild(comboitem);
		}

		if (modeDaftarMahasiswa()) {
			Common.selectComboItem(searchtahun, tahun - 10);
		} else {
			Common.selectComboItem(searchtahun, tahun - 2);
		}
		Common.selectComboItem(searchtahunsd, tahun);

		if (settingBiaya.getAngkatan() != null) {
			Common.selectComboItem(searchtahun, settingBiaya.getAngkatan(), true);
			Common.selectComboItem(searchtahunsd, settingBiaya.getAngkatan(), true);
			searchtahun.setDisabled(true);
			searchtahunsd.setDisabled(true);
		}

		toolbar.appendChild(new Label(Common.getBahasaConfig("Angkatan")));
		toolbar.appendChild(searchtahun);
		toolbar.appendChild(new Label(Common.getBahasaConfig("sd")));
		toolbar.appendChild(searchtahunsd);

		searchtahun.addEventListener(Events.ON_CHANGE, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		searchtahunsd.addEventListener(Events.ON_CHANGE, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		searchfakultas = new Combobox();
		searchjurusan = new Combobox();

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
		toolbar.appendChild(new Label(Common.getBahasaConfig("Fakultas")));
		toolbar.appendChild(searchfakultas);
		searchfakultas.setCols(5);
		searchfakultas.addEventListener(Events.ON_CHANGE, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		toolbar.appendChild(new Label(Common.getBahasaConfig("Jurusan")));
		toolbar.appendChild(searchjurusan);
		searchjurusan.setCols(5);
		searchjurusan.addEventListener(Events.ON_CHANGE, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		if (settingBiaya.getAngkatan() != null) {
			Common.selectComboItem(searchtahun, settingBiaya.getAngkatan());
			Common.selectComboItem(searchtahunsd, settingBiaya.getAngkatan());

			searchtahun.setDisabled(true);
			searchtahunsd.setDisabled(true);
		}

		if (settingBiaya.getJurusan() != null) {
			Common.selectComboItem(true, searchjurusan, settingBiaya.getJurusan());
			Common.selectComboItem(true, searchfakultas, settingBiaya.getJurusan().getFakultas());

			searchjurusan.setDisabled(true);
			searchfakultas.setDisabled(true);
		}

		toolbar.appendChild(
				new Label(mhs ? Common.getBahasaConfig("NIM/Nama") : Common.getBahasaConfig("No.Reg/Nama")));
		pencarian = new Textbox();
		pencarian.setCols(6);
		pencarian.setParent(toolbar);
		pencarian.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		// TA/Semester — DIPINDAH ke sini (dulu hanya dibuat di cabang "else" non-khusus)
		// agar tersedia di SEMUA mode, termasuk grid "khusus per mahasiswa": dipakai
		// resolveKegiatanMahasiswa/resolveKegiatanCalonMahasiswa untuk menentukan semester
		// saat menampilkan/mengubah "Nominal Tagihan Aktif" (DetailKegiatan) per mahasiswa.
		toolbar.appendChild(new ais.ui.util.MyLabelConfig("TA/Smt"));
		toolbar.appendChild(tahunAkademik = new Combobox());
		tahunAkademik.setCols(3);
		Common.generateTahunAjaran(tahunAkademik);

		genapGanjil = new Combobox();
		toolbar.appendChild(genapGanjil);
		{
			org.zkoss.zul.Comboitem comboitemGenap = new org.zkoss.zul.Comboitem();
			comboitemGenap.setLabel(Perkuliahan.GENAP);
			comboitemGenap.setValue(Perkuliahan.GENAP);
			genapGanjil.appendChild(comboitemGenap);
			org.zkoss.zul.Comboitem comboitemGanjil = new MyComboitemConfig();
			comboitemGanjil.setLabel(Perkuliahan.GANJIL);
			comboitemGanjil.setValue(Perkuliahan.GANJIL);
			genapGanjil.appendChild(comboitemGanjil);
		}

		Common.selectComboItem(genapGanjil, Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);
		genapGanjil.setReadonly(true);
		genapGanjil.setCols(2);

		tahunAkademik.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		genapGanjil.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		if (settingBiaya.getKhususBuatMahasiswaTertentu()
				&& (settingBiaya.getJenisKegiatan() != null && ConstantValues.PENDAFTARAN_CALON_MAHASISWA != null
						&& settingBiaya.getJenisKegiatan().getId()
								.equals(ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId()))
				|| (settingBiaya.getJenisKegiatan() != null && ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU != null
						&& settingBiaya.getJenisKegiatan().getId()
								.equals(ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU.getId()))) {

			MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Ambil Calon Mahasiswa",
					"/img/user_male_add.png");
			toolbar.appendChild(toolbarbutton);
			toolbarbutton.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					List<BiodataCalonMahasiswa> biodataCalonMahasiswas = ConstantValues.simpleList(
							HibernateUtil.currentSession().createCriteria(SettingBiayaDetail.class)
									.add(Restrictions.eq("settingBiaya", settingBiaya))
									.setProjection(Projections.groupProperty("biodataCalonMahasiswa.id")),
							BiodataCalonMahasiswa.class, false);

					AmbilDataBiodataCalonMahasiswaBanyak ambil = new AmbilDataBiodataCalonMahasiswaBanyak(
							biodataCalonMahasiswas);
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambil);
					ambil.setEventListener(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							List<BiodataCalonMahasiswa> biodataCalonMahasiswas = (List<BiodataCalonMahasiswa>) arg0
									.getData();
							if (biodataCalonMahasiswas != null && biodataCalonMahasiswas.size() != 0) {

								Session session = HibernateUtil.currentNativeSession();

								for (BiodataCalonMahasiswa biodataCalonMahasiswa : biodataCalonMahasiswas) {

									SettingBiayaDetail settingBiayaDetail = (SettingBiayaDetail) session
											.createCriteria(SettingBiayaDetail.class)
											.add(Restrictions.eq("biodataCalonMahasiswa", biodataCalonMahasiswa))
											.add(Restrictions.eq("settingBiaya", settingBiaya)).setMaxResults(1)
											.uniqueResult();

									if (settingBiayaDetail == null) {
										settingBiayaDetail = new SettingBiayaDetail();
										settingBiayaDetail.setSettingBiaya(settingBiaya);
										settingBiayaDetail.setBiodataCalonMahasiswa(biodataCalonMahasiswa);

										session.getTransaction().begin();
										session.save(settingBiayaDetail);
										session.getTransaction().commit();
									}

								}

								// session.disconnect();
								if (session.isOpen()) {session.disconnect();session.close();}
								HibernateUtil.closeSession();
							}

							loadData(null);
						}
					});
					ambil.setWidth("850px");
					ambil.setHeight("97%");
					ambil.setVisible(true);
					ambil.onModal();
				}
			});

			final String[] contents = new String[] { "biodataCalonMahasiswa.noRegistrasi", "biodataCalonMahasiswa.nama",
					"biodataCalonMahasiswa.gelombangPendaftaran.nama", "biodataCalonMahasiswa.tahun", "minSmt",
					"maxSmt" };

			List<String> columnHeadersAdding = new ArrayList<String>();
			for (ItemBiaya itemBiaya : selectedItemBiaya) {
				columnHeadersAdding.add(itemBiaya.getNama());
			}

			EventListener dataAdding = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Object[] objects = (Object[]) arg0.getData();
					SettingBiayaDetail settingBiayaDetail = (SettingBiayaDetail) objects[0];
					XSSFRow row = (XSSFRow) objects[2];
					XSSFCellStyle hlink_style = (XSSFCellStyle) objects[7];

					JSONObject jsonObject = new JSONObject(settingBiayaDetail.getBiayas());

					int size = 0;
					for (ItemBiaya itemBiaya : selectedItemBiaya) {

						Double nilai = jsonObject.isNull(itemBiaya.getId().toString()) ? 0.0
								: jsonObject.getDouble(itemBiaya.getId().toString());
						XSSFCell cell = row.createCell(contents.length + size);
						cell.setCellStyle(hlink_style);
						cell.setCellValue(nilai);
						size++;
					}

				}
			};

			MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(SettingBiayaDetail.class, this,
					"Download", "/img/print.png", columnHeadersAdding, dataAdding, contents);
			toolbar.appendChild(cetakToolbarbutton);

			MyToolbarbuttonConfig upload = new MyToolbarbuttonConfig("Upload " + Common.ukuranLabelFileUpload(),
					"/img/excel.png");
			upload.setUpload(Common.ukuranFileUpload());
			upload.addEventListener("onUpload", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					UploadEvent uploadEvent = (UploadEvent) event;
					Media media = uploadEvent.getMedia();
					if (!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))
						return;
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
								uploadDataCalonMahasiswa(file, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										loadData(arg0);
										Clients.clearBusy();
									}
								});
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

			// Upload "Tagihan Aktif" (DetailKegiatan) — entity-agnostic, membaca kolom ID
			// TAGIHAN/NOMINAL TAGIHAN dari file yang dihasilkan tombol Download di atas,
			// sehingga nominal tagihan yang SUDAH TERGENERATE bisa diubah manual massal
			// (bukan hanya template SettingBiayaDetail di atas).
			MyToolbarbuttonConfig uploadTagihanAktifCalon = KegiatanHelper.prosesUploadTagihan("Upload",
					"/img/excel.png", new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							loadData(arg0);
						}
					});
			toolbar.appendChild(uploadTagihanAktifCalon);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Hapus Semua", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.addEventListener("onClick", new EventListener() {
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
											List<SettingBiayaDetail> mahasiswas = ConstantValues
													.simpleList(initCriteria(true), SettingBiayaDetail.class);
											for (SettingBiayaDetail mahasiswa : mahasiswas) {
												if (bolehHapusSettingBiayaDetail(mahasiswa)) {
													Common.refreshDelete(mahasiswa);
												}
											}
											Common.createDefaultTimer(new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													loadData(null);
												}
											});
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

		} else if (modeDaftarMahasiswa()) {
			MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Ambil Mahasiswa",
					"/img/user_male_add.png");
			toolbar.appendChild(toolbarbutton);
			toolbarbutton.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					List<Mahasiswa> mahasiswas = ConstantValues.simpleList(HibernateUtil.currentSession()
							.createCriteria(SettingBiayaDetail.class).add(Restrictions.eq("settingBiaya", settingBiaya))
							.setProjection(Projections.groupProperty("mahasiswa.id")), Mahasiswa.class, false);

					AmbilDataMahasiswaBanyak ambil = new AmbilDataMahasiswaBanyak(mahasiswas);
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambil);
					ambil.setEventListener(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							List<Mahasiswa> mahasiswas = (List<Mahasiswa>) arg0.getData();
							if (mahasiswas != null && mahasiswas.size() != 0) {

								Session session = HibernateUtil.currentNativeSession();

								for (Mahasiswa mahasiswa : mahasiswas) {

									SettingBiayaDetail settingBiayaDetail = (SettingBiayaDetail)

									ConstantValues.simpleObject(session.createCriteria(SettingBiayaDetail.class)
											.add(Restrictions.eq("mahasiswa", mahasiswa))
											.add(Restrictions.eq("settingBiaya", settingBiaya)).setMaxResults(1),
											SettingBiayaDetail.class);

									if (settingBiayaDetail == null) {
										settingBiayaDetail = new SettingBiayaDetail();
										settingBiayaDetail.setSettingBiaya(settingBiaya);
										settingBiayaDetail.setMahasiswa(mahasiswa);

										session.getTransaction().begin();
										session.save(settingBiayaDetail);
										session.getTransaction().commit();
									}

								}

								// session.disconnect();
								if (session.isOpen()) {session.disconnect();session.close();}
								HibernateUtil.closeSession();
							}

							loadData(null);
						}
					});
					ambil.setWidth("850px");
					ambil.setHeight("97%");
					ambil.setVisible(true);
					ambil.onModal();
				}
			});

			final String[] contents = new String[] { "mahasiswa.nim", "mahasiswa.nama", "mahasiswa.jurusan.nama",
					"mahasiswa.tahunangkatan", "minSmt", "maxSmt" };

			List<String> columnHeadersAdding = new ArrayList<String>();
			for (ItemBiaya itemBiaya : selectedItemBiaya) {
				columnHeadersAdding.add(itemBiaya.getNama());
			}

			EventListener dataAdding = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Object[] objects = (Object[]) arg0.getData();
					SettingBiayaDetail settingBiayaDetail = (SettingBiayaDetail) objects[0];
					XSSFRow row = (XSSFRow) objects[2];
					XSSFCellStyle hlink_style = (XSSFCellStyle) objects[7];

					JSONObject jsonObject = new JSONObject(settingBiayaDetail.getBiayas());

					for (ItemBiaya itemBiaya : selectedItemBiaya) {

						Double nilai = jsonObject.isNull(itemBiaya.getId().toString()) ? 0.0
								: jsonObject.getDouble(itemBiaya.getId().toString());
						XSSFCell cell = row.createCell(contents.length);
						cell.setCellStyle(hlink_style);
						cell.setCellValue(nilai);
					}

				}
			};

			MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(SettingBiayaDetail.class, this,
					"Download", "/img/print.png", columnHeadersAdding, dataAdding, contents);
			toolbar.appendChild(cetakToolbarbutton);

			MyToolbarbuttonConfig upload = new MyToolbarbuttonConfig("Upload " + Common.ukuranLabelFileUpload(),
					"/img/excel.png");
			upload.setUpload(Common.ukuranFileUpload());
			upload.addEventListener("onUpload", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					UploadEvent uploadEvent = (UploadEvent) event;
					Media media = uploadEvent.getMedia();
					if (!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))
						return;
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
								uploadDataMahasiswa(file, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										loadData(arg0);
										Clients.clearBusy();
									}
								});
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

			// Upload "Tagihan Aktif" (DetailKegiatan) — entity-agnostic, membaca kolom ID
			// TAGIHAN/NOMINAL TAGIHAN dari file yang dihasilkan tombol Download di atas,
			// sehingga nominal tagihan yang SUDAH TERGENERATE bisa diubah manual massal
			// (bukan hanya template SettingBiayaDetail di atas).
			MyToolbarbuttonConfig uploadTagihanAktifMhs = KegiatanHelper.prosesUploadTagihan("Upload",
					"/img/excel.png", new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							loadData(arg0);
						}
					});
			toolbar.appendChild(uploadTagihanAktifMhs);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Hapus Semua", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.addEventListener("onClick", new EventListener() {
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
											List<SettingBiayaDetail> mahasiswas = ConstantValues
													.simpleList(initCriteria(true), SettingBiayaDetail.class);
											for (SettingBiayaDetail mahasiswa : mahasiswas) {
												if (bolehHapusSettingBiayaDetail(mahasiswa)) {
													Common.refreshDelete(mahasiswa);
												}
											}
											Common.createDefaultTimer(new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													loadData(null);
												}
											});
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
		} else {

			MyToolbarbuttonConfig prosesUlang = KegiatanHelper.prosesUploadTagihan("Upload", "/img/excel.png",
					new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							loadData(true);
						}
					});
			toolbar.appendChild(prosesUlang);

			MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
			cari.setParent(toolbar);
			cari.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					if (mhs) {
						List<Mahasiswa> mahasiswas = ConstantValues.simpleList(initCriteria(true), Mahasiswa.class);

						for (Mahasiswa mahasiswa : mahasiswas) {
							Session session = HibernateUtil.currentNativeSession();
							try {
								Integer smt = Common.getSemester(mahasiswa.getTahunangkatan(),
										tahunAkademik.getSelectedItem().getValue().toString(),
										genapGanjil.getSelectedItem().getValue().toString(),
										mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai());
								JenisKegiatan j = settingBiaya.getJenisKegiatan();
								boolean ulang = true;
								boolean rst = false;
								ItemBiaya item = null;
								KegiatanHelper.checkKegiatanMahasiswa(j, mahasiswa, smt,
										tahunAkademik.getSelectedItem().getValue().toString(), ulang, rst, item,
										session);
								// session.disconnect();
								if (session.isOpen()) {session.disconnect();session.close();}
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/DetailSettingBiayaAction.java:1438");
							}
							HibernateUtil.closeSession();
						}

					} else {
						List<BiodataCalonMahasiswa> mahasiswas = ConstantValues.simpleList(initCriteria(true),
								BiodataCalonMahasiswa.class);

						for (BiodataCalonMahasiswa biodataCalonMahasiswa : mahasiswas) {
							Session session = HibernateUtil.currentNativeSession();
							try {
								boolean ulang = true;
								ItemBiaya item = null;
								Integer smt = Common.getSemester(biodataCalonMahasiswa.getTahun(),
										tahunAkademik.getSelectedItem().getValue().toString(),
										genapGanjil.getSelectedItem().getValue().toString(),
										biodataCalonMahasiswa.getPindahDariKampusLamaDiSemester(),
										biodataCalonMahasiswa.getSemesterMulai());
								boolean rst = false;
								JenisKegiatan jenisKegiatan = settingBiaya.getJenisKegiatan();
								KegiatanHelper.checkKegiatanCalonMahasiswa(jenisKegiatan, biodataCalonMahasiswa, smt,
										tahunAkademik.getSelectedItem().getValue().toString(), ulang, rst, item,
										session);
								// session.disconnect();
								if (session.isOpen()) {session.disconnect();session.close();}
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/DetailSettingBiayaAction.java:1465");
							}

							HibernateUtil.closeSession();
						}

					}

					loadData(true);
				}
			});

			cari = new MyToolbarbuttonConfig("Reset", "/img/Business-Process-icon.png");
			cari.setParent(toolbar);
			cari.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					MyMessageboxConfig.show(
							"Apakah yakin ingin mengembalikan tagihan ini ke tagihan default dari billing pembayaran ?",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {

										if (mhs) {
											List<Mahasiswa> mahasiswas = ConstantValues.simpleList(initCriteria(true),
													Mahasiswa.class);

											for (Mahasiswa mahasiswa : mahasiswas) {
												Session session = HibernateUtil.currentNativeSession();
												try {
													Integer smt = Common.getSemester(mahasiswa.getTahunangkatan(),
															tahunAkademik.getSelectedItem().getValue().toString(),
															genapGanjil.getSelectedItem().getValue().toString(),
															mahasiswa.getPindahKeKampusIniMasukSemester(),
															mahasiswa.getSemesterMulai());
													JenisKegiatan j = settingBiaya.getJenisKegiatan();
													boolean ulang = true;
													boolean rst = true;
													ItemBiaya item = null;
													KegiatanHelper.checkKegiatanMahasiswa(j, mahasiswa, smt,
															tahunAkademik.getSelectedItem().getValue().toString(),
															ulang, rst, item, session);
													// session.disconnect();
													if (session.isOpen()) {session.disconnect();session.close();}
												} catch (Exception e) {
													e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/DetailSettingBiayaAction.java:1516");
												}
												HibernateUtil.closeSession();
											}

										} else {
											List<BiodataCalonMahasiswa> mahasiswas = ConstantValues
													.simpleList(initCriteria(true), BiodataCalonMahasiswa.class);

											for (BiodataCalonMahasiswa biodataCalonMahasiswa : mahasiswas) {
												Session session = HibernateUtil.currentNativeSession();
												try {
													boolean ulang = true;
													ItemBiaya item = null;
													Integer smt = Common.getSemester(biodataCalonMahasiswa.getTahun(),
															tahunAkademik.getSelectedItem().getValue().toString(),
															genapGanjil.getSelectedItem().getValue().toString(),
															biodataCalonMahasiswa.getPindahDariKampusLamaDiSemester(),
															biodataCalonMahasiswa.getSemesterMulai());
													boolean rst = true;
													JenisKegiatan jenisKegiatan = settingBiaya.getJenisKegiatan();
													KegiatanHelper.checkKegiatanCalonMahasiswa(jenisKegiatan,
															biodataCalonMahasiswa, smt,
															tahunAkademik.getSelectedItem().getValue().toString(),
															ulang, rst, item, session);
													// session.disconnect();
													if (session.isOpen()) {session.disconnect();session.close();}
												} catch (Exception e) {
													e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/DetailSettingBiayaAction.java:1544");
												}

												HibernateUtil.closeSession();
											}

										}

										loadData(true, true);

									}

								}
							});

				}
			});

		}

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(500);
		grid.getPagingChild().setMold("os");
		grid.setParent(groupbox);

		paging.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);

		if (selectedItemBiaya.size() >= 5) {
			column.setWidth("45%");
		}

		if (!modeDaftarMahasiswa()) {
			column.setLabel(mhs ? "Mahasiswa" : "Calon Mhs");
		} else {
			if ((settingBiaya.getJenisKegiatan() != null && ConstantValues.PENDAFTARAN_CALON_MAHASISWA != null
					&& settingBiaya.getJenisKegiatan().getId()
							.equals(ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId()))
					|| (settingBiaya.getJenisKegiatan() != null
							&& ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU != null
							&& settingBiaya.getJenisKegiatan().getId()
									.equals(ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU.getId()))) {
				column.setLabel("Calon Mhs");
			} else {
				column.setLabel("Mahasiswa");
			}
		}

		column = new MyColumnConfig("Tanggal Tagihan");
		column.setParent(columns);
		column.setWidth("14%");

		for (ItemBiaya itemBiaya : selectedItemBiaya) {
			column = new MyColumnConfig();
			column.setParent(columns);
			column.setWidth("10%");
			Vbox vbox = new Vbox();
			column.appendChild(vbox);
			vbox.appendChild(new Label(itemBiaya.getNama()));
			if (!modeDaftarMahasiswa()) {
				MyToolbarbuttonConfig prosesUlang = KegiatanHelper.prosesDownloadTagihan("Download", "/img/excel.png",
						tahunAkademik, genapGanjil, settingBiaya, this, itemBiaya);
				vbox.appendChild(prosesUlang);
			} else {
				// Download "Tagihan Aktif" (DetailKegiatan) khusus grid "khusus per mahasiswa" —
				// dipetakan lewat SettingBiayaDetail (initCriteria() layar ini di mode khusus),
				// dipakai berpasangan dengan KegiatanHelper.prosesUploadTagihan agar nominal
				// tagihan aktif bisa diubah manual satu-satu maupun via upload Excel.
				MyToolbarbuttonConfig prosesUlang = KegiatanHelper.prosesDownloadTagihanUntukSettingBiayaDetail(
						"Download", "/img/excel.png", tahunAkademik, genapGanjil,
						settingBiaya.getJenisKegiatan(), this, itemBiaya);
				vbox.appendChild(prosesUlang);
			}
		}

		if (modeDaftarMahasiswa()) {
			column = new MyColumnConfig("Hapus");
			column.setParent(columns);
			column.setWidth("8%");
		}

		loadData(null);
	}

	/**
	 * Proses upload Excel massal template {@link SettingBiayaDetail} (kuota custom per
	 * mahasiswa) — BUKAN tagihan aktif (untuk itu lihat {@code KegiatanHelper.prosesUploadTagihan}).
	 * Tiap baris: cari {@link Mahasiswa} (objek Excel, fallback nama+angkatan bila kolom NIM
	 * tak terbaca), validasi angkatan cocok dengan {@code settingBiaya.getAngkatan()} bila
	 * diisi, lalu simpan/update {@link SettingBiayaDetail} dengan nilai kuota tiap
	 * {@link ItemBiaya} terpilih dari kolom Excel ke-6 dst. (ditulis ke JSON {@code biayas}) dan
	 * {@code minSmt}/{@code maxSmt}. Dijalankan di background thread dengan transaksi per baris
	 * dan rollback eksplisit bila gagal; hasil dilaporkan lewat {@link ais.common.LaporanUpload}
	 * dan {@link ais.common.UploadReportHelper} (yang filenya otomatis di-download ke klien via
	 * {@link org.zkoss.zul.Filedownload#save} setelah proses selesai).
	 *
	 * @param file          file Excel (xlsx) hasil upload; kolom 0=NIM, 1=nama, 4=minSmt, 5=maxSmt,
	 *                      6+ = nilai kuota tiap item biaya sesuai urutan {@link #selectedItemBiaya}.
	 * @param eventListener dipanggil setelah proses selesai (lewat {@link ais.common.LaporanUpload#selesaikan}).
	 */
	public void uploadDataMahasiswa(final File file, final EventListener eventListener) throws Exception {

		final ais.common.LaporanUpload laporan = new ais.common.LaporanUpload(
				"Upload Setting Biaya Mahasiswa");
		laporan.setNamaBerkasSumber(file.getName());

		final Label label = new Label(ais.common.Common.getBahasaConfig("Proses upload data data .."));
		final ais.common.UploadReportHelper report = new ais.common.UploadReportHelper("Upload Tagihan Mahasiswa");
		final Label downloadPath = new Label("");
		Clients.showBusy(label.getValue());
		final Timer timer = new Timer(200);
		timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		timer.setRepeats(true);
		timer.addEventListener("onTimer", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Clients.showBusy(label.getValue());
				if (label.getValue().isEmpty()) {
					Clients.clearBusy();
					timer.detach();
					if (!downloadPath.getValue().isEmpty()) {
						try {
							org.zkoss.zul.Filedownload.save(new java.io.File(downloadPath.getValue()), "text/plain");
						} catch (Exception eDl) { ais.common.ErrorAuditUtil.record(eDl, "auto-audit(empty-catch) DetailSettingBiayaAction download laporan"); }
					}
					MyMessageboxConfig.showFormatCb(
						"Upload selesai.{V1}", "Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, eventListener,
						"\n" + report.getRingkasan());
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
						String nimBaris = "";
						try {

							nimBaris = Common.getSheetContentAsString(sheet, 0, i);
							Mahasiswa mahasiswa = (Mahasiswa) Common.getSheetContentAsObject(sheet, 0, i,
									Mahasiswa.class);
							String nama = Common.getSheetContentAsString(sheet, 1, i);
							if (mahasiswa == null) {
								mahasiswa = ConstantValues.ambilByNim(nimBaris);
							}

							if (mahasiswa != null && nama != null && !nama.trim().isEmpty()) {
								mahasiswa = (Mahasiswa) ConstantValues
										.simpleObject(session.createCriteria(Mahasiswa.class)
												.add(settingBiaya.getAngkatan() != null
														? Restrictions.eq("tahunangkatan", settingBiaya.getAngkatan())
														: Restrictions.sqlRestriction("true"))
												.add(Restrictions.ilike("nama", nama.trim(), MatchMode.ANYWHERE))
												.add(Restrictions.or(Restrictions.isNull("aktif"),
														Restrictions.eq("aktif", true)))
												.setMaxResults(1).addOrder(Order.desc("id")), Mahasiswa.class);
							}

							if (mahasiswa != null && mahasiswa.getId() != null
									&& (settingBiaya.getAngkatan() == null || (settingBiaya.getAngkatan() != null
											&& mahasiswa.getTahunangkatan() != null
											&& settingBiaya.getAngkatan().equals(mahasiswa.getTahunangkatan())))) {

								Double min = Common.getSheetContentAsDouble(sheet, 4, i);
								Double max = Common.getSheetContentAsDouble(sheet, 5, i);

								SettingBiayaDetail settingBiayaDetail = (SettingBiayaDetail) ConstantValues
										.simpleObject(session.createCriteria(SettingBiayaDetail.class)
												.add(Restrictions.eq("mahasiswa", mahasiswa))
												.add(Restrictions.eq("settingBiaya", settingBiaya)).setMaxResults(1),
												SettingBiayaDetail.class);

								if (settingBiayaDetail == null) {
									settingBiayaDetail = new SettingBiayaDetail();
									settingBiayaDetail.setSettingBiaya(settingBiaya);
									settingBiayaDetail.setMahasiswa(mahasiswa);
								}
								JSONObject jsonObject = new JSONObject(settingBiayaDetail.getBiayas());
								int index = 6;
								for (ItemBiaya itemBiaya : selectedItemBiaya) {
									String namaNilai = Common.getSheetContentAsString(sheet, index, i);
									if (namaNilai != null && Common.isNumber(namaNilai.trim())
											&& !namaNilai.trim().isEmpty()) {
										Double nilai = Double.parseDouble(namaNilai.trim());
										jsonObject.put(itemBiaya.getId().toString(), nilai);
									}
									index++;
								}
								if (min != null)
									settingBiayaDetail.setMinSmt(min.intValue());
								if (max != null)
									settingBiayaDetail.setMaxSmt(max.intValue());
								settingBiayaDetail.setBiayas(jsonObject.toString());
								session.getTransaction().begin();
								try {
									session.saveOrUpdate(settingBiayaDetail);
									session.getTransaction().commit();
								} catch (Exception eSimpan) {
									try { session.getTransaction().rollback(); } catch (Exception eRoll) { ais.common.ErrorAuditUtil.record(eRoll, "rollback-gagal-upload"); }
									throw eSimpan;
								}
								laporan.catatBerhasil(i, mahasiswa.getNim(), mahasiswa.getNama());
								report.sukses(i, mahasiswa.getNim(), "SettingBiayaDetail disimpan: " + mahasiswa.getNama());

								label.setValue("Upload data \"" + mahasiswa.getNim() + " - " + mahasiswa.getNama()
										+ "\" (" + Common.numberFormat.get().format(i * 100.0 / rowCount) + " %)");
							} else {
								laporan.catatDilewati(i, nimBaris, "Mahasiswa tidak ditemukan atau angkatan tidak sesuai");
								report.gagal(i, nimBaris, "Mahasiswa tidak ditemukan atau angkatan tidak sesuai", "Pastikan NIM/nama valid dan terdaftar.");
							}

						} catch (Exception e) {
							laporan.catatGagal(i, nimBaris, e);
							report.gagal(i, nimBaris, e, "Periksa data baris ini. Detail: " + e.getMessage());
							Common.tampilErrorJikaAdmin(e);
						}

					}
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/helper/DetailSettingBiayaAction.java:1772");
				}

				HibernateUtil.closeSession();

				try {
					java.io.File rptFile = report.simpanLaporan();
					downloadPath.setValue(rptFile.getAbsolutePath());
				} catch (Exception eReport) {
					ais.common.ErrorAuditUtil.record(eReport, "auto-audit(empty-catch) DetailSettingBiayaAction laporan upload");
				}
				label.setValue("");
							} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();
	}

	/**
	 * Padanan {@link #uploadDataMahasiswa} untuk {@link BiodataCalonMahasiswa}: cari calon
	 * mahasiswa lewat nomor registrasi (fallback nama), validasi tahun cocok dengan
	 * {@code settingBiaya.getAngkatan()} bila diisi, lalu simpan/update
	 * {@link SettingBiayaDetail} dengan kuota custom per item biaya dan rentang semester —
	 * lihat Javadoc {@link #uploadDataMahasiswa} untuk detail alur transaksi dan pelaporan.
	 *
	 * @param file          file Excel (xlsx) hasil upload; kolom 0=noRegistrasi, 1=nama, 4=minSmt,
	 *                      5=maxSmt, 6+ = nilai kuota tiap item biaya.
	 * @param eventListener dipanggil setelah proses selesai.
	 */
	public void uploadDataCalonMahasiswa(final File file, final EventListener eventListener) throws Exception {

		final ais.common.LaporanUpload laporan = new ais.common.LaporanUpload(
				"Upload Setting Biaya Calon Mahasiswa");
		laporan.setNamaBerkasSumber(file.getName());

		final Label label = new Label(ais.common.Common.getBahasaConfig("Proses upload data data .."));
		final ais.common.UploadReportHelper report = new ais.common.UploadReportHelper("Upload Tagihan Calon Mahasiswa");
		final Label downloadPath = new Label("");
		Clients.showBusy(label.getValue());
		final Timer timer = new Timer(200);
		timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		timer.setRepeats(true);
		timer.addEventListener("onTimer", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Clients.showBusy(label.getValue());
				if (label.getValue().isEmpty()) {
					Clients.clearBusy();
					timer.detach();
					if (!downloadPath.getValue().isEmpty()) {
						try {
							org.zkoss.zul.Filedownload.save(new java.io.File(downloadPath.getValue()), "text/plain");
						} catch (Exception eDl) { ais.common.ErrorAuditUtil.record(eDl, "auto-audit(empty-catch) DetailSettingBiayaAction download laporan"); }
					}
					MyMessageboxConfig.showFormatCb(
						"Upload selesai.{V1}", "Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, eventListener,
						"\n" + report.getRingkasan());
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
						String noRegistrasi = "";
						try {

							noRegistrasi = Common.getSheetContentAsString(sheet, 0, i);
							String nama = Common.getSheetContentAsString(sheet, 1, i);

							Double min = Common.getSheetContentAsDouble(sheet, 4, i);
							Double max = Common.getSheetContentAsDouble(sheet, 5, i);

							BiodataCalonMahasiswa biodataCalonMahasiswa = (BiodataCalonMahasiswa) ConstantValues
									.simpleObject(session.createCriteria(BiodataCalonMahasiswa.class)
											.add(Restrictions.or(Restrictions.isNull("aktif"),
													Restrictions.eq("aktif", true)))
											.add(settingBiaya.getAngkatan() != null
													? Restrictions.eq("tahun", settingBiaya.getAngkatan())
													: Restrictions.isNull("tahun"))
											.add(Restrictions.eq("noRegistrasi", noRegistrasi)).setMaxResults(1)
											.addOrder(Order.desc("id")), BiodataCalonMahasiswa.class);

							if (biodataCalonMahasiswa != null && nama != null && !nama.trim().isEmpty()) {
								biodataCalonMahasiswa = (BiodataCalonMahasiswa) ConstantValues.simpleObject(
										session.createCriteria(BiodataCalonMahasiswa.class)
												.add(Restrictions.or(Restrictions.isNull("aktif"),
														Restrictions.eq("aktif", true)))
												.add(settingBiaya.getAngkatan() != null
														? Restrictions.eq("tahun", settingBiaya.getAngkatan())
														: Restrictions.isNull("tahun"))
												.add(Restrictions.ilike("nama", nama.trim(), MatchMode.ANYWHERE))
												.setMaxResults(1).addOrder(Order.desc("id")),
										BiodataCalonMahasiswa.class);
							}

							if (biodataCalonMahasiswa != null && biodataCalonMahasiswa.getId() != null
									&& (settingBiaya.getAngkatan() == null || (settingBiaya.getAngkatan() != null
											&& biodataCalonMahasiswa.getTahun() != null
											&& settingBiaya.getAngkatan().equals(biodataCalonMahasiswa.getTahun())))) {

								SettingBiayaDetail settingBiayaDetail = (SettingBiayaDetail) ConstantValues
										.simpleObject(session.createCriteria(SettingBiayaDetail.class)
												.add(Restrictions.eq("biodataCalonMahasiswa", biodataCalonMahasiswa))
												.add(Restrictions.eq("settingBiaya", settingBiaya)).setMaxResults(1),
												SettingBiayaDetail.class);

								if (settingBiayaDetail == null) {
									settingBiayaDetail = new SettingBiayaDetail();
									settingBiayaDetail.setSettingBiaya(settingBiaya);
									settingBiayaDetail.setBiodataCalonMahasiswa(biodataCalonMahasiswa);
								}
								JSONObject jsonObject = new JSONObject(settingBiayaDetail.getBiayas());
								int index = 6;
								for (ItemBiaya itemBiaya : selectedItemBiaya) {
									String namaNilai = Common.getSheetContentAsString(sheet, index, i);
									if (namaNilai != null && Common.isNumber(namaNilai.trim())
											&& !namaNilai.trim().isEmpty()) {
										Double nilai = Double.parseDouble(namaNilai.trim());
										jsonObject.put(itemBiaya.getId().toString(), nilai);
									}
									index++;
								}
								if (min != null)
									settingBiayaDetail.setMinSmt(min.intValue());
								if (max != null)
									settingBiayaDetail.setMaxSmt(max.intValue());

								settingBiayaDetail.setBiayas(jsonObject.toString());
								session.getTransaction().begin();
								try {
									session.saveOrUpdate(settingBiayaDetail);
									session.getTransaction().commit();
								} catch (Exception eSimpan) {
									try { session.getTransaction().rollback(); } catch (Exception eRoll) { ais.common.ErrorAuditUtil.record(eRoll, "rollback-gagal-upload"); }
									throw eSimpan;
								}
								laporan.catatBerhasil(i, biodataCalonMahasiswa.getNoRegistrasi(), biodataCalonMahasiswa.getNama());
								report.sukses(i, biodataCalonMahasiswa.getNoRegistrasi(), "SettingBiayaDetail disimpan: " + biodataCalonMahasiswa.getNama());

								label.setValue("Upload data \"" + biodataCalonMahasiswa.getNoRegistrasi() + " - "
										+ biodataCalonMahasiswa.getNama() + "\" ("
										+ Common.numberFormat.get().format(i * 100.0 / rowCount) + " %)");
							} else {
								laporan.catatDilewati(i, noRegistrasi, "Calon mahasiswa tidak ditemukan atau tahun tidak sesuai");
								report.gagal(i, noRegistrasi, "Calon mahasiswa tidak ditemukan atau tahun tidak sesuai", "Pastikan nomor registrasi/nama valid dan terdaftar.");
							}

						} catch (Exception e) {
							laporan.catatGagal(i, noRegistrasi, e);
							report.gagal(i, noRegistrasi, e, "Periksa data baris ini. Detail: " + e.getMessage());
							Common.tampilErrorJikaAdmin(e);
						}

					}
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/helper/DetailSettingBiayaAction.java:1908");
				}

				HibernateUtil.closeSession();

				try {
					java.io.File rptFile = report.simpanLaporan();
					downloadPath.setValue(rptFile.getAbsolutePath());
				} catch (Exception eReport) {
					ais.common.ErrorAuditUtil.record(eReport, "auto-audit(empty-catch) DetailSettingBiayaAction laporan upload");
				}
				label.setValue("");
							} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();
	}

	/**
	 * Bangun query Hibernate sumber baris grid, bercabang empat menurut mode layar (lihat
	 * Javadoc kelas {@link DetailSettingBiayaAction}):
	 * <ol>
	 * <li>khusus calon mahasiswa: {@link SettingBiayaDetail} milik {@link #settingBiaya}
	 * dengan alias jurusan dari {@code prodi1} (jenis Pendaftaran Calon Mahasiswa) atau
	 * {@code prodiLulus} (jenis Pendaftaran Ulang), disaring jurusan/fakultas/rentang tahun/
	 * pencarian;</li>
	 * <li>khusus mahasiswa: {@link SettingBiayaDetail} beralias {@code mahasiswa.jurusan},
	 * disaring serupa;</li>
	 * <li>mode reguler mahasiswa ({@link #mhs}): {@link Mahasiswa} aktif disaring program,
	 * status awal, jenjang (via jurusan), jenis seleksi, gelombang pendaftaran dari
	 * {@link #settingBiaya}, plus filter toolbar (jurusan/fakultas/rentang angkatan/pencarian);</li>
	 * <li>mode reguler calon mahasiswa: {@link BiodataCalonMahasiswa} aktif dengan replikasi
	 * lengkap mesin pencocokan tagihan sesungguhnya ({@code GeneralValueObject.ambilSatuData},
	 * dipakai {@code PembayaranUtilHelper.getDetailBiayaCalonMahasiswa}) untuk jenis seleksi
	 * berjenjang (jenisSeleksiDipilih → jenisSeleksi → gelombangPendaftaran.jenisSeleksi), paket,
	 * dan gelombang pendaftaran — lihat komentar "PERBAIKAN" pada cabang ini untuk kronologi bug
	 * yang diperbaiki (daftar calon mahasiswa sebelumnya bisa menampilkan orang yang sebenarnya
	 * tidak memenuhi kriteria SettingBiaya yang sedang dilihat).</li>
	 * </ol>
	 *
	 * @param order bila {@code true}, tambahkan pengurutan (angkatan/tahun desc, NIM/no.registrasi asc).
	 * @return Criteria Hibernate siap dieksekusi/di-count untuk paging.
	 */
	@Override
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();

		Criteria criteria;

		if (settingBiaya.getKhususBuatMahasiswaTertentu()
				&& (settingBiaya.getJenisKegiatan() != null && ConstantValues.PENDAFTARAN_CALON_MAHASISWA != null
						&& settingBiaya.getJenisKegiatan().getId()
								.equals(ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId()))
				|| (settingBiaya.getJenisKegiatan() != null && ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU != null
						&& settingBiaya.getJenisKegiatan().getId()
								.equals(ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU.getId()))) {

			criteria = session.createCriteria(SettingBiayaDetail.class)
					.add(Restrictions.eq("settingBiaya", settingBiaya))
					.createAlias("biodataCalonMahasiswa", "biodataCalonMahasiswa")

					.createAlias(settingBiaya.getJenisKegiatan().getId()
							.equals(ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId()) ? "biodataCalonMahasiswa.prodi1"
									: "biodataCalonMahasiswa.prodiLulus",
							"jurusan", Criteria.LEFT_JOIN)

					.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
							|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
									: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

					.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
							|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
									: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", searchfakultas, false))

					.add(Restrictions.between("biodataCalonMahasiswa.tahun", searchtahun.getSelectedItem().getValue(),
							searchtahunsd.getSelectedItem().getValue()))

					.add(pencarian.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.or(
									Restrictions.ilike("biodataCalonMahasiswa.nama", pencarian.getValue().trim(),
											MatchMode.ANYWHERE),
									Restrictions.ilike("biodataCalonMahasiswa.noRegistrasi",
											pencarian.getValue().trim(), MatchMode.ANYWHERE)));

			if (order) {
				criteria.addOrder(Order.desc("biodataCalonMahasiswa.tahun"))
						.addOrder(Order.asc("biodataCalonMahasiswa.noRegistrasi"));
			}

		} else if (modeDaftarMahasiswa()) {

			criteria = session.createCriteria(SettingBiayaDetail.class)
					.add(Restrictions.eq("settingBiaya", settingBiaya)).createAlias("mahasiswa", "mahasiswa")

					.createAlias("mahasiswa.jurusan", "jurusan")

					.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
							|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
									: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

					.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
							|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
									: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", searchfakultas, false))

					.add(Restrictions.between("mahasiswa.tahunangkatan", searchtahun.getSelectedItem().getValue(),
							searchtahunsd.getSelectedItem().getValue()))

					.add(pencarian.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.or(
									Restrictions.ilike("mahasiswa.nama", pencarian.getValue().trim(),
											MatchMode.ANYWHERE),
									Restrictions.ilike("mahasiswa.nim", pencarian.getValue().trim(),
											MatchMode.ANYWHERE)));

			if (order) {
				criteria.addOrder(Order.desc("mahasiswa.tahunangkatan")).addOrder(Order.asc("mahasiswa.nim"));
			}

		}

		else if (mhs) {
			criteria = session.createCriteria(Mahasiswa.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

					.add(settingBiaya.getProgram() == null || settingBiaya.getProgram().isEmpty()
							? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("program", settingBiaya.getProgram()))

					.add(settingBiaya.getStatusAwalMahasiswa() == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("statusAwalMahasiswa", settingBiaya.getStatusAwalMahasiswa()))

					.createAlias("jurusan", "jurusan")

					.add(settingBiaya.getJenjang() == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("jurusan.jenjang", settingBiaya.getJenjang()))

					// PERBAIKAN sama seperti cabang BiodataCalonMahasiswa di bawah: sebelumnya
					// kriteria di sini tidak menyaring Jenis Seleksi/Gelombang Pendaftaran sama
					// sekali, padahal mesin pencocokan tagihan sesungguhnya mewajibkan field ini
					// cocok bila terisi di Setting Biaya. (Mahasiswa tidak punya field "paket".)
					.add(settingBiaya.getJenisSeleksi() == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("jenisSeleksi", settingBiaya.getJenisSeleksi()))

					.add(settingBiaya.getGelombangPendaftaran() == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("gelombangPendaftaran", settingBiaya.getGelombangPendaftaran()))

					.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
							|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
									: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

					.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
							|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
									: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", searchfakultas, false))

					.add(Restrictions.between("tahunangkatan", searchtahun.getSelectedItem().getValue(),
							searchtahunsd.getSelectedItem().getValue()))

					.add(pencarian.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.or(
									Restrictions.ilike("nama", pencarian.getValue().trim(), MatchMode.ANYWHERE),
									Restrictions.ilike("nim", pencarian.getValue().trim(), MatchMode.ANYWHERE)));

			if (order) {
				criteria.addOrder(Order.desc("tahunangkatan")).addOrder(Order.asc("nim"));
			}

		} else {
			criteria = session.createCriteria(BiodataCalonMahasiswa.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

					.add(settingBiaya.getProgram() == null || settingBiaya.getProgram().isEmpty()
							? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("program", settingBiaya.getProgram()))

					.add(settingBiaya.getStatusAwalMahasiswa() == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("statusAwalMahasiswa", settingBiaya.getStatusAwalMahasiswa()))

					.createAlias("prodi1", "prodi1", Criteria.LEFT_JOIN)
					.createAlias("prodiLulus", "prodiLulus", Criteria.LEFT_JOIN)

					.add(settingBiaya.getJenjang() == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("jenjang", settingBiaya.getJenjang()))

					// PERBAIKAN "calon mahasiswa jenis seleksi berbeda tetap muncul" (mis. mahasiswa
					// Reguler ikut muncul di daftar Setting Biaya RPL): kriteria ini SEBELUMNYA tidak
					// menyaring sama sekali berdasarkan Jenis Seleksi/Paket/Gelombang Pendaftaran,
					// padahal mesin pencocokan tagihan sesungguhnya (GeneralValueObject.ambilSatuData,
					// dipakai PembayaranUtilHelper.getDetailBiayaCalonMahasiswa) MEWAJIBKAN field-field
					// ini cocok bila terisi di Setting Biaya. Akibatnya daftar "Calon Mhs" di layar ini
					// bisa menampilkan mahasiswa yang sebenarnya TIDAK memenuhi kriteria Setting Biaya
					// yang sedang dilihat -- membingungkan staf (tampak seolah dobel/salah kelompok).
					// getJenisSeleksi() BiodataCalonMahasiswa berjenjang: jenisSeleksiDipilih (pilihan
					// sendiri saat daftar) -> jenisSeleksi (field mentah) -> gelombangPendaftaran.jenisSeleksi
					// (default gelombang) -- direplikasi di sini agar hasilnya konsisten dgn mesin tagihan.
					.createAlias("gelombangPendaftaran", "gelombangPendaftaranJs", Criteria.LEFT_JOIN)
					.add(settingBiaya.getJenisSeleksi() == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.or(
									Restrictions.eq("jenisSeleksiDipilih", settingBiaya.getJenisSeleksi()),
									Restrictions.and(Restrictions.isNull("jenisSeleksiDipilih"),
											Restrictions.or(
													Restrictions.eq("jenisSeleksi", settingBiaya.getJenisSeleksi()),
													Restrictions.and(Restrictions.isNull("jenisSeleksi"),
															Restrictions.eq("gelombangPendaftaranJs.jenisSeleksi",
																	settingBiaya.getJenisSeleksi()))))))

					.add(settingBiaya.getPaket() == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("paket", settingBiaya.getPaket()))

					.add(settingBiaya.getGelombangPendaftaran() == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("gelombangPendaftaran", settingBiaya.getGelombangPendaftaran()))

					.add(searchjurusan.getSelectedItem() == null
							|| searchjurusan.getSelectedItem().getValue() == null
									? Restrictions.sqlRestriction("1=1")
									: Restrictions.or(
											Restrictions.and(Restrictions.isNull("prodiLulus"),
													Restrictions.eq("prodi1",
															searchjurusan.getSelectedItem().getValue())),
											Restrictions.eq("prodiLulus", searchjurusan.getSelectedItem().getValue()))

					)

					.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
							? Restrictions.sqlRestriction("1=1")
							:

							Restrictions.or(
									Restrictions.and(Restrictions.isNull("prodiLulus.fakultas"),
											CommonSearchFilterHelper.eqSelectedWithId("prodi1.fakultas", searchfakultas, false)),
									CommonSearchFilterHelper.eqSelectedWithId("prodiLulus.fakultas", searchfakultas, false)))

					.add(Restrictions.between("tahun", searchtahun.getSelectedItem().getValue(),
							searchtahunsd.getSelectedItem().getValue()))

					.add(pencarian.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.or(
									Restrictions.ilike("nama", pencarian.getValue().trim(), MatchMode.ANYWHERE),
									Restrictions.or(
											Restrictions.ilike("noUjian", pencarian.getValue().trim(),
													MatchMode.ANYWHERE),
											Restrictions.ilike("noRegistrasi", pencarian.getValue().trim(),
													MatchMode.ANYWHERE))));

			if (order) {
				criteria.addOrder(Order.desc("tahun")).addOrder(Order.asc("noRegistrasi"));
			}
		}

		return criteria;

	}

}
