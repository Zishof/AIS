package ais.action.master.helper;


import ais.common.CommonSearchFilterHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.MatakuliahPrasyaratAction;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Kurikulum;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.PembagianKuotaPerkuliahanBerdasarkantahunAngkatan;
import ais.database.model.Perkuliahan;
import ais.database.model.Program;
import ais.database.model.Tbmuser;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Composer ZK untuk dialog "Ambil Data Perkuliahan" — layar pengambilan mata kuliah non-paket pada
 * pengisian KRS mahasiswa. Menampilkan grid perkuliahan yang tersedia (dengan filter fakultas/
 * jurusan/program/kelas/semester/tahapan/nama matakuliah), memberi tanda cek pada mata kuliah yang
 * dipilih pemakai, memvalidasi kapasitas kelas, prasyarat mata kuliah, dan batas SKS maksimum
 * berdasarkan IPK sebelum mengizinkan penyimpanan ke {@link ais.database.model.Detailperkuliahan}.
 *
 * <p>
 * Alur pakai: {@link #display} membangun seluruh UI (filter, grid, tombol Simpan/Batal) dan memuat
 * data lewat {@link #onSearchDefault}; setiap baris dirender oleh inner class
 * {@link MatakuliahRenderer} yang menampilkan checkbox pilih, info dosen/jadwal/ruangan, status
 * ketersediaan kursi, dan riwayat pengambilan sebelumnya (untuk kasus mengulang/menabung nilai).
 * Perubahan centang memicu {@link #updateStatus(org.zkoss.zul.Checkbox)} yang menghitung ulang total
 * SKS terpilih lewat {@link #hitungSksYangTelahDiambil()} dan menonaktifkan tombol Simpan bila
 * {@link #apakahMelebihiKetentuan()} bernilai {@code true} (SKS melebihi batas IPK mahasiswa).
 * Penyimpanan akhir dilakukan oleh {@link #save()}, yang membuat baris
 * {@link ais.database.model.Detailperkuliahan} baru per mata kuliah terpilih (mengecek prasyarat,
 * jam bentrok, dan kapasitas kelas sekali lagi sebelum commit) dan secara opsional langsung
 * menyetujuinya bila konfigurasi {@code saat_ambil_krs_langsung_disetujui} aktif.
 * </p>
 *
 * <p>
 * Paging data grid memakai {@code AmbilDataPagingHelper} (paging server-side), menggantikan mold
 * "paging" client-side lama yang dibatasi jumlah baris maksimum. Kelas ini menyimpan cache
 * proses-lokal ({@code matakuliahTelahDiambil}, {@code perkuliahanTelahDiambil}, peta riwayat) untuk
 * menghindari query berulang saat merender banyak baris grid dalam satu tampilan.
 * </p>
 */
public class AmbilDataPerkuliahanHelper {

	private String tahunAjaran;
	private Integer semester;
	private Mahasiswa mahasiswa;
	private MyGrid grid;

	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private Textbox namaMk;
	private Combobox searchfakultas = new Combobox();
	private Combobox jurusanCombobox = new Combobox();
	private Combobox programCombobox = new Combobox();
	private Combobox semesterBox;

	private Label sksYangdiambil;
	private Integer semesterPendek;

	private Boolean reload = false;

	private HashMap<Long, Perkuliahan> hashMap = new HashMap<Long, Perkuliahan>();
	private AmbilDataKelasBanbox kelas;
	private MyToolbarbuttonConfig buttonSimpan;

	private Integer tahapan;
	private Combobox tahapanBox;
	private Set<Long> matakuliahTelahDiambil = new HashSet<Long>();
	private Set<Long> perkuliahanTelahDiambil = new HashSet<Long>();
	private Map<Long, String> riwayatMatakuliah = new HashMap<Long, String>();
	private Map<Long, String> riwayatPerkuliahan = new HashMap<Long, String>();
	private boolean remedial;
	private Checkbox semuaSemester;

	/**
	 * @param semesterPendek status semester pendek (SP) yang dibatasi helper ini, atau {@code null}
	 *                        untuk pengambilan KRS reguler
	 * @param remedial        bila {@code true}, hanya menampilkan perkuliahan yang ditandai remedial
	 */
	public AmbilDataPerkuliahanHelper(Integer semesterPendek, boolean remedial) {
		this.semesterPendek = semesterPendek;
		this.remedial = remedial;

	}

	/**
	 * Row renderer grid hasil pencarian: menampilkan checkbox pilih (disembunyikan bila mata kuliah
	 * sudah pernah diambil dan kurikulumnya boleh diambil), SKS, dosen, jadwal/ruangan, prasyarat,
	 * kapasitas vs jumlah terisi, dan status ketersediaan/riwayat. Setiap tahap render dibungkus
	 * try/catch individual dengan log {@code System.out} berlabel langkah — pola defensif agar
	 * kegagalan satu baris (mis. relasi Hibernate lazy yang gagal dimuat) tidak menggagalkan seluruh
	 * grid, melainkan menampilkan baris darurat berisi pesan kegagalan.
	 */
	class MatakuliahRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row row, Object arg1) throws Exception {
			final Perkuliahan perkuliahan = (Perkuliahan) arg1;
			System.out.println("[MatakuliahRenderer.render] mulai id="
					+ (perkuliahan == null ? "null" : perkuliahan.getId()));
			try {
				renderInternal(row, perkuliahan);
			} catch (Exception ex) {
				System.out.println("[MatakuliahRenderer.render] EXCEPTION id="
						+ (perkuliahan == null ? "null" : perkuliahan.getId())
						+ " msg=" + ex.getMessage()
						+ " class=" + ex.getClass().getName());
				ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/helper/AmbilDataPerkuliahanHelper.java:115");
				// Tampilkan baris darurat agar grid tidak kosong
				try {
					String mk = "";
					try { mk = perkuliahan != null && perkuliahan.getMatakuliah() != null
							? perkuliahan.getMatakuliah().getNama() : "(?)"; } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataPerkuliahanHelper.java:120");}
					new Label("[" + mk + " — gagal render: " + ex.getClass().getSimpleName() + "]").setParent(row);
				} catch (Exception ignored2) { ais.common.ErrorAuditUtil.record(ignored2, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataPerkuliahanHelper.java:122");}
			}
			System.out.println("[MatakuliahRenderer.render] selesai id="
					+ (perkuliahan == null ? "null" : perkuliahan.getId()));
		}

		private void renderInternal(Row row, final Perkuliahan perkuliahan) throws Exception {
			if (perkuliahan == null) {
				System.out.println("[MatakuliahRenderer.renderInternal] perkuliahan null — skip");
				return;
			}

			System.out.println("[MatakuliahRenderer.renderInternal] step 1: getMatakuliah id=" + perkuliahan.getId());
			final Matakuliah matakuliah;
			try {
				matakuliah = perkuliahan.getMatakuliah();
			} catch (Exception ex) {
				System.out.println("[MatakuliahRenderer.renderInternal] GAGAL getMatakuliah: " + ex.getMessage());
				throw ex;
			}
			if (matakuliah == null) {
				System.out.println("[MatakuliahRenderer.renderInternal] matakuliah null — skip");
				return;
			}

			System.out.println("[MatakuliahRenderer.renderInternal] step 2: getKurikulum");
			boolean kirikulumBolehAmbil = false;
			try {
				kirikulumBolehAmbil = perkuliahan.getKurikulum() != null
						&& perkuliahan.getKurikulum().bolehAmbil(mahasiswa);
			} catch (Exception ex) {
				System.out.println("[MatakuliahRenderer.renderInternal] GAGAL getKurikulum: " + ex.getMessage());
			}

			System.out.println("[MatakuliahRenderer.renderInternal] step 3: currentSession");
			Session session;
			try {
				session = HibernateUtil.currentSession();
				System.out.println("[MatakuliahRenderer.renderInternal] session=" + (session == null ? "null" : (session.isOpen() ? "open" : "closed")));
			} catch (Exception ex) {
				System.out.println("[MatakuliahRenderer.renderInternal] GAGAL currentSession: " + ex.getMessage());
				throw ex;
			}

			System.out.println("[MatakuliahRenderer.renderInternal] step 4: getMerupakanRemedial");
			boolean jmlMk = false;
			try {
				jmlMk = perkuliahan.getMerupakanRemedial() ? false
						: (matakuliahTelahDiambil.contains(matakuliah.getId())
								|| perkuliahanTelahDiambil.contains(perkuliahan.getId()));
			} catch (Exception ex) {
				System.out.println("[MatakuliahRenderer.renderInternal] GAGAL jmlMk: " + ex.getMessage());
			}

			System.out.println("[MatakuliahRenderer.renderInternal] step 5: buat Checkbox");
			row.setValign("top");
			row.setAttribute("myValue", perkuliahan);

			String paralel = "";
			try {
				paralel = (perkuliahan.getMerupakan_paralel() != null && perkuliahan.getMerupakan_paralel())
						? " (Paralel) " : "";
			} catch (Exception ex) {
				System.out.println("[MatakuliahRenderer.renderInternal] GAGAL getMerupakan_paralel: " + ex.getMessage());
			}
			String labelCheckbox = matakuliah.getKode() + " - " + matakuliah.getNama() + paralel;

			final Checkbox checkbox = new Checkbox(labelCheckbox);
			row.setAttribute("checkbox", checkbox);

			boolean tampil = !jmlMk || !kirikulumBolehAmbil;
			checkbox.setVisible(tampil);
			if (!tampil) {
				new Label(labelCheckbox).setParent(row);
			} else {
				checkbox.setParent(row);
			}
			row.setAttribute("checkbox", checkbox);

			/*
			 * Centang hanya menunjukkan data yang benar-benar ada pada KRS yang sedang
			 * dibuka. perkuliahanTelahDiambil juga berisi riwayat semester lain, sehingga
			 * tidak boleh dipakai sebagai sumber status centang.
			 */
			final boolean jml = hashMap.containsKey(perkuliahan.getId());
			checkbox.setChecked(jml);

			System.out.println("[MatakuliahRenderer.renderInternal] step 6: kapasitas & jumlah masuk");
			Integer kapasitasKelas = 0;
			Integer jumlahUdahMasuk = 0;
			try {
				kapasitasKelas = perkuliahan.getKapasitasKelas();
				if (kapasitasKelas == null) { kapasitasKelas = 30; }
			} catch (Exception ex) {
				System.out.println("[MatakuliahRenderer.renderInternal] GAGAL getKapasitasKelas: " + ex.getMessage());
				kapasitasKelas = 30;
			}
			try {
				PembagianKuotaPerkuliahanBerdasarkantahunAngkatan pkpb = KrsUtilHelper
						.ambilPembagianKuotaPerkuliahanBerdasarkantahunAngkatan(session, perkuliahan,
								mahasiswa.getTahunangkatan(), reload);
				Number kuota = pkpb == null ? null : pkpb.getKuota();
				if (kuota != null) { kapasitasKelas = kuota.intValue(); }
			} catch (Exception ex) {
				System.out.println("[MatakuliahRenderer.renderInternal] GAGAL ambilPembagianKuota: " + ex.getMessage());
			}
			try {
				jumlahUdahMasuk = KrsUtilHelper.ambilJumlahDetailperkuliahan(session, perkuliahan, reload);
				if (jumlahUdahMasuk == null) { jumlahUdahMasuk = 0; }
			} catch (Exception ex) {
				System.out.println("[MatakuliahRenderer.renderInternal] GAGAL ambilJumlahDetailperkuliahan: " + ex.getMessage());
				jumlahUdahMasuk = 0;
			}

			if (!checkbox.isChecked()) {
				checkbox.setDisabled(jumlahUdahMasuk >= kapasitasKelas);
			}

			final EventListener checkboxEventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					if (!Common.checkMatakuliahPrasyarat(perkuliahan.getMatakuliah(), mahasiswa, semester)) {
						checkbox.setChecked(false);
						return;
					}

					if (!checkbox.isChecked() && jml) {
						MyMessageboxConfig.showFormat(
								"Mohon maaf, mata kuliah yang telah dipilih sebelumnya tidak dapat dibatalkan (di-uncheck) secara langsung. Langkah yang dapat dilakukan: (1) buka papan pengambilan {V1}; (2) hapus terlebih dahulu mata kuliah yang belum disetujui dari papan tersebut; (3) setelah terhapus, silakan lakukan pemilihan ulang bila diperlukan.",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION,
								Common.getBahasa("label_krs"));
						checkbox.setChecked(true);
						return;
					}
					if (checkbox.isChecked()) {
						hashMap.put(perkuliahan.getId(), perkuliahan);
					} else {
						hashMap.remove(perkuliahan.getId());
					}
					updateStatus(checkbox);
				}
			};

			checkbox.addEventListener(Events.ON_CHECK, checkboxEventListener);

			System.out.println("[MatakuliahRenderer.renderInternal] step 7: kolom SKS & dosen");
			new Label(matakuliah.getSks() + "").setParent(row);

			try {
				ais.action.master.helper.PerkuliahanUIHelper.displayDosenPerkuliahan(row, perkuliahan, true);
			} catch (Exception ex) {
				System.out.println("[MatakuliahRenderer.renderInternal] GAGAL displayDosenPerkuliahan: " + ex.getMessage());
				new Label("").setParent(row);
			}

			System.out.println("[MatakuliahRenderer.renderInternal] step 8: hari/jam/ruangan & prasyarat");
			Vbox vbox = new Vbox();
			vbox.setParent(row);
			try {
				ais.action.master.helper.PerkuliahanUIHelper.displayHariJamRuanganPerkuliahanUmum(vbox, perkuliahan);
			} catch (Exception ex) {
				System.out.println("[MatakuliahRenderer.renderInternal] GAGAL displayHariJamRuangan: " + ex.getMessage());
			}
			try {
				MatakuliahPrasyaratAction.tampilPrasyarat(vbox, matakuliah);
			} catch (Exception ex) {
				System.out.println("[MatakuliahRenderer.renderInternal] GAGAL tampilPrasyarat: " + ex.getMessage());
			}
			try {
				Kurikulum kurikulum = perkuliahan.getKurikulum();
				new Label(kurikulum == null ? "" : "Kurikulum : " + kurikulum.getNama()).setParent(vbox);
			} catch (Exception ex) {
				System.out.println("[MatakuliahRenderer.renderInternal] GAGAL label kurikulum: " + ex.getMessage());
				new Label("").setParent(vbox);
			}

			System.out.println("[MatakuliahRenderer.renderInternal] step 9: semester & tahap");
			try {
				new Label(perkuliahan.getSemester() == null ? "" : perkuliahan.getSemester() + "").setParent(row);
			} catch (Exception ex) {
				System.out.println("[MatakuliahRenderer.renderInternal] GAGAL getSemester: " + ex.getMessage());
				new Label("").setParent(row);
			}

			Integer tahap = 0;
			try {
				tahap = perkuliahan.getKurikulumPunyaMatakuliah() == null
						|| perkuliahan.getKurikulumPunyaMatakuliah().getTahap() == null ? 0
								: perkuliahan.getKurikulumPunyaMatakuliah().getTahap();
			} catch (Exception ex) {
				System.out.println("[MatakuliahRenderer.renderInternal] GAGAL getKurikulumPunyaMatakuliah: " + ex.getMessage());
			}
			new Label(tahap + "").setParent(row);

			System.out.println("[MatakuliahRenderer.renderInternal] step 10: kelas & keterangan");
			try {
				String keterangan = perkuliahan.getKeterangan();
				if (keterangan == null || keterangan.trim().isEmpty()) {
					new Label(perkuliahan.getKelas() == null ? "" : perkuliahan.getKelas()).setParent(row);
				} else {
					Vbox vb = new Vbox();
					vb.setParent(row);
					new Label(perkuliahan.getKelas() == null ? "" : perkuliahan.getKelas()).setParent(vb);
					new Label(keterangan).setParent(vb);
				}
			} catch (Exception ex) {
				System.out.println("[MatakuliahRenderer.renderInternal] GAGAL kelas/keterangan: " + ex.getMessage());
				new Label("").setParent(row);
			}

			System.out.println("[MatakuliahRenderer.renderInternal] step 11: kapasitas & status");
			final int kapFinal = kapasitasKelas == null ? 0 : kapasitasKelas;
			final int jmlFinal = jumlahUdahMasuk == null ? 0 : jumlahUdahMasuk;
			new Label(kapFinal + "/" + jmlFinal).setParent(row);

			String infoRiwayat = ambilInfoRiwayat(perkuliahan);
			boolean pernahDiambil = infoRiwayat != null && !infoRiwayat.trim().equals("");
			String statusText = jml ? buatInfoKrsAktif(perkuliahan)
					: (jmlMk || pernahDiambil) ? infoRiwayat
							: (jmlFinal < kapFinal) ? "Tersedia" : "Penuh";
			Label label = new Label(statusText);
			label.setParent(row);

			if (!kirikulumBolehAmbil) {
				label.setValue("Kurikulum ini Anda tidak boleh ambil");
				label.setStyle("font-weight:bold;color:red");
			} else if (jml) {
				label.setStyle("font-weight:bold;color:brown");
			} else if (jmlMk || pernahDiambil) {
				label.setStyle("font-weight:bold;color:green");
			} else if (jmlFinal < kapFinal) {
				label.setStyle("font-weight:bold;color:blue");
			} else {
				label.setStyle("font-weight:bold;color:red");
			}

			row.setValign("top");
			row.setAttribute("label_status", label);

			if (checkbox.isVisible() && hashMap.containsKey(perkuliahan.getId())) {
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						checkbox.setChecked(true);
						checkboxEventListener.onEvent(null);
					}
				});
			}
			System.out.println("[MatakuliahRenderer.renderInternal] step DONE id=" + perkuliahan.getId());
		}

	}

	@SuppressWarnings({ "unchecked" })
	private void updateStatus(Checkbox checkbox) throws Exception {
		if (checkbox != null) {
			boolean apakahMelebihiKetentuan = apakahMelebihiKetentuan();
			System.out.println("apakahMelebihiKetentuan => " + apakahMelebihiKetentuan);
			if (apakahMelebihiKetentuan) {
				buttonSimpan.setDisabled(true);
				checkbox.setChecked(false);
				hitungSksYangTelahDiambil();
				return;
			}
		}

		buttonSimpan.setDisabled(false);

		Session session = HibernateUtil.currentSession();
		Rows rows = grid.getRows();
		rows = grid.getRows();
		List<Row> list = rows.getChildren();
		Set<Long> longs = new HashSet<Long>();
		for (Perkuliahan perkuliahan : hashMap.values()) {
			longs.add(perkuliahan.getMatakuliah().getId());
		}
		for (Row row : list) {
			Checkbox c = (Checkbox) row.getAttribute("checkbox");
			if ((checkbox != null && c != null && c == checkbox) || (c != null && c.isChecked()))
				continue;

			Perkuliahan perkuliahan = (Perkuliahan) row.getAttribute("myValue");
			if (perkuliahan == null || perkuliahan.getMatakuliah() == null
					|| perkuliahan.getMatakuliah().getId() == null) {
				continue;
			}

			Integer jmlMk = longs.contains(perkuliahan.getMatakuliah().getId()) ? 1 : 0;
			if (jmlMk.equals(0)) {
				jmlMk = (matakuliahTelahDiambil.contains(perkuliahan.getMatakuliah().getId())
						|| perkuliahanTelahDiambil.contains(perkuliahan.getId())) ? 1 : 0;

			}

			if (perkuliahan.getMerupakanRemedial()) {
				jmlMk = 0;
			}

			Integer jumlahUdahMasuk = KrsUtilHelper.ambilJumlahDetailperkuliahan(session, perkuliahan, false);
			c.setDisabled(!jmlMk.equals(0));

			Integer kapasitasKelas = perkuliahan.getKapasitasKelas();
			PembagianKuotaPerkuliahanBerdasarkantahunAngkatan pembagianKuotaPerkuliahanBerdasarkantahunAngkatan = KrsUtilHelper
					.ambilPembagianKuotaPerkuliahanBerdasarkantahunAngkatan(session, perkuliahan,
							mahasiswa.getTahunangkatan(), reload);
			Number kuota = pembagianKuotaPerkuliahanBerdasarkantahunAngkatan == null ? null
					: pembagianKuotaPerkuliahanBerdasarkantahunAngkatan.getKuota();
			if (kuota != null) {
				kapasitasKelas = kuota.intValue();
			}

			try {
				Label label = (Label) row.getAttribute("label_status");
				String infoRiwayat = ambilInfoRiwayat(perkuliahan);
				boolean pernahDiambil = infoRiwayat != null && !infoRiwayat.trim().equals("");
				label.setValue(!jmlMk.equals(0) || pernahDiambil
						? infoRiwayat
						: jumlahUdahMasuk < (kapasitasKelas) ? "Tersedia" : "Penuh");

				if (!jmlMk.equals(0)) {
					c.setChecked(false);
					label.setStyle("font-weight:bold;color:green");
				} else if (pernahDiambil) {
					label.setStyle("font-weight:bold;color:green");
				} else if (jumlahUdahMasuk < (kapasitasKelas)) {
					label.setStyle("font-weight:bold;color:blue");
				} else {
					label.setStyle("font-weight:bold;color:red");
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataPerkuliahanHelper.java:443");
//				e.printStackTrace();
			}

		}
	}

	private String buatInfoKrsAktif(Perkuliahan perkuliahan) {
		String info = "Terpilih pada KRS";
		if (semester != null) {
			info += " semester " + semester;
		}
		if (tahunAjaran != null && !tahunAjaran.trim().equals("")) {
			info += ", TA " + tahunAjaran;
		}
		return info;
	}

	private String ambilInfoRiwayat(Perkuliahan perkuliahan) {
		if (perkuliahan == null) {
			return null;
		}
		String info = riwayatPerkuliahan.get(perkuliahan.getId());
		if ((info == null || info.trim().equals("")) && perkuliahan.getMatakuliah() != null) {
			info = riwayatMatakuliah.get(perkuliahan.getMatakuliah().getId());
		}
		return info;
	}

	private String buatInfoRiwayat(Detailperkuliahan detailperkuliahan) {
		String info = "Sudah diambil";
		Integer semesterRiwayat = detailperkuliahan.getSemester();
		if (semesterRiwayat != null) {
			info += " pada semester " + semesterRiwayat;
		}
		String tahunAkademikRiwayat = detailperkuliahan.getTahunAkademik();
		if ((tahunAkademikRiwayat == null || tahunAkademikRiwayat.trim().equals(""))
				&& detailperkuliahan.getPerkuliahan() != null) {
			tahunAkademikRiwayat = detailperkuliahan.getPerkuliahan().getTahunAjaran();
		}
		if (tahunAkademikRiwayat != null && !tahunAkademikRiwayat.trim().equals("")) {
			info += ", TA " + tahunAkademikRiwayat;
		}
		if (detailperkuliahan.getPerkuliahan() != null
				&& detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() != null) {
			info += " (semester pendek)";
		}
		return info;
	}

	private void simpanInfoRiwayat(Map<Long, String> riwayat, Long id, String info) {
		if (id == null || info == null || info.trim().equals("")) {
			return;
		}
		String infoLama = riwayat.get(id);
		if (infoLama == null || infoLama.trim().equals("")) {
			riwayat.put(id, info);
		} else if (infoLama.indexOf(info) < 0) {
			riwayat.put(id, infoLama + "; " + info);
		}
	}

	@SuppressWarnings({})
	private Integer hitungSksYangTelahDiambil() {

		Integer jumlah = KrsUtilHelper.hitungSksYangTelahDiambil(hashMap, mahasiswa, tahapan, semester, semesterPendek);
		if (this.sksYangdiambil != null) {
			this.sksYangdiambil.setValue(jumlah + " SKS");
		}

		return jumlah;
	}

	@SuppressWarnings({})
	private boolean apakahMelebihiKetentuan() throws Exception {
		return Common.checkPembatasanSKSBerdasarkanIP(mahasiswa, AmbilDataPerkuliahanHelper.this.semester,
				hitungSksYangTelahDiambil(), semesterPendek);
	}

	/**
	 * Menyimpan seluruh mata kuliah yang tercentang ({@code hashMap}) sebagai baris
	 * {@link ais.database.model.Detailperkuliahan} baru untuk mahasiswa yang sedang mengisi KRS.
	 * Membatalkan penyimpanan (mengembalikan {@code false}) bila total SKS melebihi ketentuan
	 * ({@link #apakahMelebihiKetentuan()}) atau bila konfigurasi
	 * {@code saat_pengambilan_krs_tidak_diperbolehkan_ada_jam_bentrok} aktif dan ditemukan jadwal
	 * bentrok. Setiap mata kuliah diproses dalam transaksi tersendiri; bila kapasitas kelas sudah
	 * penuh saat commit, mata kuliah tersebut dilewati dan pesan peringatan dikumpulkan untuk
	 * ditampilkan di akhir alih-alih menggagalkan seluruh proses simpan.
	 *
	 * @return {@code true} bila proses simpan (untuk mata kuliah yang berhasil) selesai dijalankan;
	 *         {@code false} bila dibatalkan sejak awal karena SKS berlebih atau jam bentrok
	 * @throws Exception diteruskan dari kegagalan pengecekan ketentuan SKS
	 */
	@SuppressWarnings({})
	public boolean save() throws Exception {

		if (apakahMelebihiKetentuan()) {
			return false;
		}

		Tbmuser tbmuser = Common.getCurrentUser();

		if (Common.bolehKonfigurasi("saat_pengambilan_krs_tidak_diperbolehkan_ada_jam_bentrok", Konfigurasi.TIDAK_AKTIF)) {

			List<Long> detailperkuliahansid = mahasiswa.ambilPerkuliahanDanParalel(semester, null);

			List<Detailperkuliahan> detailperkuliahans = new ArrayList<Detailperkuliahan>();
			for (Long detailperkuliahanid : detailperkuliahansid) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null && detailperkuliahan.getPerkuliahan() != null) {
					detailperkuliahans.add(detailperkuliahan);
				}
			}

			for (Perkuliahan perkuliahan : hashMap.values()) {
				Detailperkuliahan detailperkuliahan = new Detailperkuliahan(tbmuser, AmbilDataPerkuliahanHelper.class);
				detailperkuliahan.setNilaiHuruf("");
				detailperkuliahan.setTotalNilai(0.0);
				detailperkuliahan.setMahasiswa(mahasiswa);
				detailperkuliahan.setPerkuliahan(perkuliahan);
				detailperkuliahan.setSemester(AmbilDataPerkuliahanHelper.this.semester);
				detailperkuliahan.setTahap(tahapan);
				detailperkuliahans.add(detailperkuliahan);
			}
			if (!Common.checkJamBentrok(detailperkuliahans)) {
				return false;
			}
		}

		Set<Long> matakuliahs = new HashSet<Long>();
		String peringatanKapasitasRuangan = "";
		for (Perkuliahan perkuliahan : hashMap.values()) {

			if (matakuliahs.contains(perkuliahan.getMatakuliah().getId())) {
				continue;
			}
			matakuliahs.add(perkuliahan.getMatakuliah().getId());

			if (!Common.checkMatakuliahPrasyarat(perkuliahan.getMatakuliah(), mahasiswa, semester)) {
				continue;
			}

			Session mySession = HibernateUtil.currentNativeSession();
			try {
				int detailperkuliahanCount = KrsUtilHelper.ambilJumlahDetailperkuliahan(mySession, perkuliahan,
						mahasiswa, reload);
				if (detailperkuliahanCount == 0) {
					Integer jumlahUdahMasuk = KrsUtilHelper.ambilJumlahDetailperkuliahan(mySession, perkuliahan, false);
					Integer kapasitasKelas = perkuliahan.getKapasitasKelas();
					PembagianKuotaPerkuliahanBerdasarkantahunAngkatan pembagianKuotaPerkuliahanBerdasarkantahunAngkatan = KrsUtilHelper
							.ambilPembagianKuotaPerkuliahanBerdasarkantahunAngkatan(mySession, perkuliahan,
									mahasiswa.getTahunangkatan(), reload);
					Number kuota = pembagianKuotaPerkuliahanBerdasarkantahunAngkatan == null ? null
							: pembagianKuotaPerkuliahanBerdasarkantahunAngkatan.getKuota();
					if (kuota != null) {
						kapasitasKelas = kuota.intValue();
					}

					jumlahUdahMasuk++;
					if (jumlahUdahMasuk > (kapasitasKelas)) {
						peringatanKapasitasRuangan += Common.pesan(
								"Mohon maaf, kapasitas kelas untuk perkuliahan ini telah penuh. Kapasitas maksimal kelas tersebut adalah {V1} peserta, sedangkan penambahan Anda akan menjadikan jumlah peserta menjadi {V2}. Langkah yang dapat dilakukan: (1) pilih jadwal perkuliahan atau kelas lain yang masih tersedia; (2) hubungi bagian akademik untuk penambahan kuota kelas bila diperlukan.\n",
								(kapasitasKelas), jumlahUdahMasuk);
						HibernateUtil.closeSession();
						continue;
					}
					Detailperkuliahan detailperkuliahan = new Detailperkuliahan(tbmuser,
							AmbilDataPerkuliahanHelper.class);
					detailperkuliahan.setNilaiHuruf("");
					detailperkuliahan.setTotalNilai(0.0);
					detailperkuliahan.setMahasiswa(mahasiswa);
					detailperkuliahan.setPerkuliahan(perkuliahan);
					detailperkuliahan.setTahap(tahapan);
					detailperkuliahan.setSemester(AmbilDataPerkuliahanHelper.this.semester);
					if (Common.bolehKonfigurasi("saat_ambil_krs_langsung_disetujui", Konfigurasi.TIDAK_AKTIF)) {
						detailperkuliahan.setPersetujuan(Detailperkuliahan.DISETUJUI);
					}
					mySession.getTransaction().begin();
					KrsUtilHelper.simpanKrsJikaBelumAda(mySession, detailperkuliahan);
					mySession.getTransaction().commit();
				}
				// mySession.disconnect();
				if (mySession.isOpen()) {mySession.disconnect();mySession.close();}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
			HibernateUtil.closeSession();
		}

		if (!peringatanKapasitasRuangan.trim().equals("")) {
			MyMessageboxConfig.show(peringatanKapasitasRuangan, "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
		}
		return true;
	}

	/**
	 * Membangun dan menampilkan window modal "Ambil Data Perkuliahan": filter pencarian (fakultas,
	 * jurusan, nama matakuliah, program/kelas, semester/tahun ajaran, tahapan), label total SKS
	 * maksimum yang boleh diambil ({@code Common.getMinDanMaxIPK}), serta grid hasil pencarian.
	 * Sebelum grid dimuat, method ini menginisialisasi ulang seluruh state terpilih ({@code hashMap})
	 * dari daftar {@code detailperkuliahans} yang diberikan dan membangun peta riwayat pengambilan
	 * mata kuliah sebelumnya ({@code riwayatMatakuliah}/{@code riwayatPerkuliahan}) dari seluruh
	 * riwayat KRS mahasiswa.
	 *
	 * @param mahasiswa            mahasiswa yang sedang mengisi KRS
	 * @param tahunAjaran          tahun ajaran KRS yang sedang diisi
	 * @param semester             nomor semester KRS yang sedang diisi
	 * @param tahapan              tahapan KRS (bila fitur tahapan aktif), boleh {@code null}/0
	 * @param dataLoader           callback yang dipanggil ulang setelah simpan berhasil untuk memuat
	 *                             ulang layar pemanggil
	 * @param detailperkuliahans   id {@link ais.database.model.Detailperkuliahan} yang sudah terpilih
	 *                             sebelumnya (mis. draft KRS non-paket yang belum disetujui)
	 */
	public void display(final Mahasiswa mahasiswa, final String tahunAjaran, final Integer semester,
			final Integer tahapan, final DataLoader dataLoader, final List<Long> detailperkuliahans) {
		this.mahasiswa = mahasiswa;
		this.tahunAjaran = tahunAjaran;
		this.semester = semester;

		Double[] batas = Common.getMinDanMaxIPK(mahasiswa, semester, semesterPendek);
		final Integer maxsks = batas[0].intValue();

		this.tahapan = tahapan;
		hashMap = new HashMap<Long, Perkuliahan>();
		matakuliahTelahDiambil.clear();
		perkuliahanTelahDiambil.clear();
		riwayatMatakuliah.clear();
		riwayatPerkuliahan.clear();
		for (Long detailperkuliahanid : detailperkuliahans) {
			Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
					.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
			if (detailperkuliahan != null && detailperkuliahan.getPerkuliahan() != null) {
				hashMap.put(detailperkuliahan.getPerkuliahan().getId(), detailperkuliahan.getPerkuliahan());
			}
		}

		for (Long oid : mahasiswa.ambilDetailperkuliahan()) {
			Detailperkuliahan o = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
					oid.toString());
			if (o != null) {
				if (o.getPerkuliahan() != null) {
					perkuliahanTelahDiambil.add(o.getPerkuliahan().getId());
					String infoRiwayat = buatInfoRiwayat(o);
					simpanInfoRiwayat(riwayatPerkuliahan, o.getPerkuliahan().getId(), infoRiwayat);
					if (o.getPerkuliahan().getMatakuliah() != null) {
						simpanInfoRiwayat(riwayatMatakuliah,
								o.getPerkuliahan().getMatakuliah().getId(), infoRiwayat);
					}
					if (o.getSemester() != null && o.getSemester().equals(semester) &&

							(

							(semesterPendek == null && o.getPerkuliahan().getStatusSemesterPendek() == null)
									|| (semesterPendek != null && o.getPerkuliahan().getStatusSemesterPendek() != null)

							)

					) {
						if (o.getPerkuliahan().getMatakuliah() != null) {
							matakuliahTelahDiambil.add(o.getPerkuliahan().getMatakuliah().getId());
						}
					}
				}
			}
		}

		System.out.println("mahasiswa=>" + mahasiswa + ", semester=>" + semester + ", perkuliahanTelahDiambil=>"
				+ perkuliahanTelahDiambil + ", matakuliahTelahDiambil=>" + matakuliahTelahDiambil);

		final MyWindow window = new MyWindow();
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
		window.setTitle("Ambil Data Perkuliahan");
		window.setWidth("97%");
		window.setHeight("97%");

		try {

			Borderlayout borderlayout = new Borderlayout();
			borderlayout.setParent(window);
			Center center = new Center();
			center.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);

			boolean mobile = Common.isMobile();

			North north = new North();
			north.setParent(borderlayout);
			north.setBorder("none");
			ais.ui.util.ZkCompat.setFlex(north, false);
			// Pada mobile setiap kelompok filter ditampilkan per baris. Tinggi 90px
			// sebelumnya memadatkan enam baris ke area yang sangat kecil sehingga combo
			// sulit disentuh dan sebagian filter tersembunyi. Desktop tetap ringkas.
			if (mobile) {
				north.setHeight(tahapan != null && !tahapan.equals(0) ? "390px" : "340px");
			} else {
				north.setHeight(tahapan != null && !tahapan.equals(0) ? "120px" : "90px");
			}
		north.setAutoscroll(true);

			Grid searchgrid = new Grid();
			searchgrid.setWidth("100%");
			searchgrid.setParent(north);
			if (mobile) {
				searchgrid.setStyle("font-size:14px;");
			}

			if (mobile) {
				Columns columns = new Columns();
				columns.setParent(searchgrid);

				MyColumnConfig column = new MyColumnConfig();
				column.setParent(columns);
				column.setWidth("30%");

				column = new MyColumnConfig();
				column.setParent(columns);
			} else {
				Columns columns = new Columns();
				columns.setParent(searchgrid);

				MyColumnConfig column = new MyColumnConfig();
				column.setParent(columns);
				column.setWidth("8%");

				column = new MyColumnConfig();
				column.setParent(columns);

				column = new MyColumnConfig();
				column.setParent(columns);
				column.setWidth("8%");

				column = new MyColumnConfig();
				column.setParent(columns);

				column = new MyColumnConfig();
				column.setParent(columns);
				column.setWidth("8%");

				column = new MyColumnConfig();
				column.setParent(columns);
			}

			Rows rows = new Rows();
			rows.setParent(searchgrid);

			Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, jurusanCombobox);

			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));

			Hbox hbox = new Hbox();
			hbox.setParent(row);
			if (mobile) {
				hbox.setWidth("100%");
				hbox.setSpacing("6px");
			}
			Common.selectComboItem(true, searchfakultas, this.mahasiswa.getJurusan().getFakultas());
			hbox.appendChild(searchfakultas);
			searchfakultas.setCols(mobile ? 5 : 7);
			if (mobile) {
				searchfakultas.setWidth("48%");
				searchfakultas.setHeight("38px");
				searchfakultas.setStyle("font-size:14px;");
			}

			if (searchfakultas.getSelectedItem() != null && searchfakultas.getSelectedItem().getValue() != null) {
				Common.insertComboDanSemua(jurusanCombobox, new String[] { "nama", "kodeEpsbed" }, "jenjang",
						Jurusan.class, Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false));
			}

			Common.selectComboItem(true, jurusanCombobox, this.mahasiswa.getJurusan());
			hbox.appendChild(jurusanCombobox);
			jurusanCombobox.setCols(mobile ? 5 : 7);
			if (mobile) {
				jurusanCombobox.setWidth("48%");
				jurusanCombobox.setHeight("38px");
				jurusanCombobox.setStyle("font-size:14px;");
			}

			searchfakultas.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(arg0);

				}
			});

			jurusanCombobox.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(arg0);
				}
			});

			if (mobile) {
				row = new MyFormRow();
			}

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Matakuliah"));

			row.appendChild(namaMk = new Textbox());
			namaMk.setWidth(mobile ? "100%" : "90%");
			if (mobile) {
				namaMk.setHeight("38px");
				namaMk.setStyle("font-size:14px;");
			}

			namaMk.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(arg0);
				}
			});

			if (mobile) {
				row = new MyFormRow();
			}

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Telah mengambil"));
			row.appendChild(sksYangdiambil = new Label(hitungSksYangTelahDiambil() + " SKS"));

			if (mobile) {
				row = new MyFormRow();
			}

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Program / Kelas"));
			Common.insertComboDanSemua(programCombobox, "namaBaru", Program.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
			Session session = HibernateUtil.currentSession();
			Program programselected = (Program) session.createCriteria(Program.class)
					.add(Restrictions.eq("nama", this.mahasiswa.getProgram())).uniqueResult();
			Common.selectComboItem(programCombobox, programselected);

			hbox = new Hbox();
			hbox.setParent(row);
			if (mobile) {
				hbox.setWidth("100%");
				hbox.setSpacing("6px");
			}

			hbox.appendChild(programCombobox);
			programCombobox.setCols(mobile ? 5 : 7);
			if (mobile) {
				programCombobox.setWidth("48%");
				programCombobox.setHeight("38px");
				programCombobox.setStyle("font-size:14px;");
			}

			programCombobox.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(arg0);

				}
			});

			kelas = new AmbilDataKelasBanbox();
			String kls = "";
			if (Common.bolehKonfigurasi("Pada_saat_mengambil_KRS_otomatis_kelas_terisi_dengan_kelas_mahasiswa_dan_tidak_bisa_diubah", Konfigurasi.TIDAK_AKTIF)) {
				KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, semester, tahapan, semesterPendek,
						false, false);
				kls = krsMahasiswa.getKelas();
				kelas.setReadonly(true);
				kelas.setDisabled(true);
			} else if (Common.bolehKonfigurasi("Pada_saat_mengambil_KRS_otomatis_kelas_terisi_dengan_kelas_mahasiswa")) {
				KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, semester, tahapan, semesterPendek,
						false, false);
				kls = krsMahasiswa.getKelas();
				kelas.setReadonly(false);
			}

			if (mobile) {
				row = new MyFormRow();
			}

			hbox.appendChild(kelas);
			kelas.setValue(kls == null ? "" : kls.trim());
			kelas.setCols(mobile ? 5 : 7);
			if (mobile) {
				kelas.setWidth("48%");
				kelas.setHeight("38px");
				kelas.setStyle("font-size:14px;");
			}
			kelas.setEventListener(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(arg0);
				}
			});

			if (mobile) {
				row = new MyFormRow();
			}

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Semester / TA"));

			semuaSemester = new MyCheckboxConfig("Tampilkan semua smt");

			hbox = new Hbox();
			hbox.setParent(row);
			if (mobile) {
				hbox.setWidth("100%");
				hbox.setSpacing("6px");
			}

			hbox.appendChild(semesterBox = new Combobox());

			final EventListener eventListenerSemester = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					int maxSemesterPilihan = mahasiswa.getJurusan().getJenjang().getJumlahSemester();
					Common.clear(semesterBox);
					for (int i = 1; i <= maxSemesterPilihan; i++) {

						if (!semuaSemester.isChecked() && AmbilDataPerkuliahanHelper.this.semester != null) {
							if (AmbilDataPerkuliahanHelper.this.semester % 2 == 0 && i % 2 == 0) {
								org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
								comboitem.setLabel(i + "");
								comboitem.setValue(i);
								semesterBox.appendChild(comboitem);
							} else if (AmbilDataPerkuliahanHelper.this.semester % 2 == 1 && i % 2 == 1) {
								org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
								comboitem.setLabel(i + "");
								comboitem.setValue(i);
								semesterBox.appendChild(comboitem);
							}
						} else {
							org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
							comboitem.setLabel(i + "");
							comboitem.setValue(i);
							semesterBox.appendChild(comboitem);
						}

					}

					org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
					if (!semuaSemester.isChecked() && AmbilDataPerkuliahanHelper.this.semester != null) {
						if (AmbilDataPerkuliahanHelper.this.semester % 2 == 0) {
							comboitem.setLabel("Semua Genap");
							comboitem.setValue(null);
							semesterBox.appendChild(comboitem);
						} else {
							comboitem.setLabel("Semua Ganjil");
							comboitem.setValue(null);
							semesterBox.appendChild(comboitem);
						}
					} else {
						comboitem.setLabel("Semua");
						comboitem.setValue(null);
						semesterBox.appendChild(comboitem);
					}

					if (semesterPendek != null) {
						semesterBox.setSelectedItem(comboitem);
					} else if (tahapan == null || tahapan.equals(0)) {
						Common.selectComboItem(semesterBox, AmbilDataPerkuliahanHelper.this.semester);
					}
				}
			};

			semesterBox.setCols(mobile ? 5 : 7);
			semesterBox.setReadonly(true);
			if (mobile) {
				semesterBox.setWidth("38%");
				semesterBox.setHeight("38px");
				semesterBox.setStyle("font-size:14px;");
			}
			semesterBox.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(arg0);

				}
			});

			hbox.appendChild(new ais.ui.util.MyLabelConfig(this.tahunAjaran));

			hbox.appendChild(semuaSemester);

			semuaSemester.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					eventListenerSemester.onEvent(arg0);
					onSearchDefault(arg0);
				}
			});
			eventListenerSemester.onEvent(null);

			if (mobile) {
				row = new MyFormRow();
			}

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Maksimal SKS"));
			row.appendChild(new ais.ui.util.MyLabelConfig(Common.numberFormat.get().format(maxsks) + " SKS"));

			tahapanBox = new Combobox();
			if (tahapan != null && !tahapan.equals(0)) {
				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Tahapan"));

				row.appendChild(tahapanBox);
				for (int i = 1; i < 15; i++) {
					Comboitem comboitem = new org.zkoss.zul.Comboitem();
					comboitem.setLabel(i + "");
					comboitem.setValue(i);
					tahapanBox.appendChild(comboitem);
				}
				Common.selectComboItem(tahapanBox, tahapan);
				tahapanBox.setWidth(mobile ? "100%" : "90%");
				if (mobile) {
					tahapanBox.setHeight("38px");
					tahapanBox.setStyle("font-size:14px;");
				}

				tahapanBox.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						onSearchDefault(arg0);

					}
				});
			}

			Borderlayout myBorderlayout1 = new Borderlayout();
			myBorderlayout1.setParent(center);

			North north1 = new North();
			north1.setParent(myBorderlayout1);
			north1.setBorder("none");
			ais.ui.util.ZkCompat.setFlex(north1, true);

			Toolbar toolbar = new Toolbar();
			toolbar.setParent(north1);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					onSearchDefault(null);
				}
			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					reload = true;
					onSearchDefault(null);
				}
			});
			button.setParent(toolbar);

			Center myCenter1 = new Center();
			ais.ui.util.ZkCompat.setFlex(myCenter1, true);
			myCenter1.setParent(myBorderlayout1);

			grid = new MyGrid();// grid.setOddRowSclass("non-odd");
			grid.setWidth("100%");
			/* Paging server-side (AmbilDataPagingHelper) menggantikan mold "paging"
			 * client-side yang dibatasi MAX_RESULT_100. Grid di-parent langsung ke
			 * myCenter1 (Center ber-flex) agar Borderlayout dari pasangGridDanPaging
			 * memperoleh tinggi yang pasti — jika dipasang di dalam Row auto-height
			 * Borderlayout kolaps menjadi 0px sehingga area data tidak tampil. */
			pagingHelper.pasangOnPaging(new EventListener() {
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(null);
				}
			});
			pagingHelper.pasangGridDanPaging(myCenter1, grid);

			Columns columns = new Columns();

			columns.setParent(grid);

			MyColumnConfig column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Mata Kuliah");
			column.setWidth("20%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("SKS");
			column.setWidth("5%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel(Common.getBahasa("label_dosen"));
			column.setWidth(Common.bolehKonfigurasi("tampilkan_dosen_saat_ambil_krs") ? "17%" : "0%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Hari/Waktu/Ruang");
			column.setWidth("25%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Smt");
			column.setWidth("5%");

			if (ConstantValues.jumlahTahapan.isEmpty()) {
				ConstantValues.initJumlahTahapan();
			}

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Tahap");
			column.setWidth((ConstantValues.aktifkanTahapan
					&& ConstantValues.getJumlahTahapan(mahasiswa.getProgram(), mahasiswa.getJurusan()) > 2) ? "5%"
							: "0%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Kelas");
			column.setWidth("15%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Kap.");
			column.setWidth("5%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Status");
			column.setWidth("20%");

			South south = new South();
			ais.ui.util.ZkCompat.setFlex(south, true);
			south.setParent(borderlayout);

			toolbar = new Toolbar();
			toolbar.setParent(south);

			MyToolbarbuttonConfig buttonBatal = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
			buttonBatal.setTooltiptext("Tutup");
			buttonBatal.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					window.detach();
				}
			});
			buttonBatal.setParent(toolbar);

			buttonSimpan = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
			buttonSimpan.setTooltiptext("Simpan");
			buttonSimpan.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					save();

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							final Integer jumlah = KrsUtilHelper.hitungSksYangTelahDiambil(hashMap, mahasiswa, tahapan,
									semester, semesterPendek);

							int minimalSmtSyaratKrs = 2;
							try {
								minimalSmtSyaratKrs = Integer
										.parseInt(Common.getKonfigurasi("minimal_smt_syarat_krs", "2").getNilai());
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataPerkuliahanHelper.java:1072");
								// TODO: handle exception
							}

							int selisih = maxsks - jumlah;
							if (selisih < 0 && semester >= minimalSmtSyaratKrs) {
								MyMessageboxConfig.showFormatCb(
										"Mohon maaf, jumlah SKS yang diambil sebanyak {V1} SKS telah melebihi batas maksimal yang diizinkan. Mahasiswa dengan NIM {V2} atas nama {V3} hanya diperbolehkan mengambil paling banyak {V4} SKS. Langkah yang dapat dilakukan: (1) tekan OK untuk menyesuaikan pengambilan secara otomatis; (2) sistem akan menghapus mata kuliah yang melebihi ketentuan; (3) periksa kembali daftar mata kuliah setelah penyesuaian.",
										"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION,
										new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												Common.hapusMatakuliahYangMelebihiKetentuan(mahasiswa, semester,
														tahapan, semesterPendek, jumlah);

												Common.createDefaultTimer(new EventListener() {

													@Override
													public void onEvent(Event arg0) throws Exception {
														dataLoader.loadData(true);
														window.detach();
													}
												});
											}
									},
									jumlah, mahasiswa.getNim(), mahasiswa.getNama(), maxsks);

							} else {
								dataLoader.loadData(true);
								window.detach();
							}
						}
					});

				}
			});
			buttonSimpan.setParent(toolbar);

			window.setVisible(true);

			try {
				if (apakahMelebihiKetentuan()) {
					Common.freeze(window, true);
					buttonBatal.setDisabled(false);
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				Common.tampilErrorJikaAdmin(e);
			}

			window.onModal();
		} catch (Exception e) {
			try {
				window.onModal();
			} catch (Exception e1) {
				e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/helper/AmbilDataPerkuliahanHelper.java:1128");
			}
			Common.tampilErrorJikaAdmin(e);
		}

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (Common.bolehKonfigurasi("saat_ambil_krs_secara_default_hanya_pilih_smt_berjalan", Konfigurasi.TIDAK_AKTIF)) {
					semuaSemester.setVisible(false);
				}

				if (Common.bolehKonfigurasi("saat_ambil_krs_secara_default_pilih_semua_semester", Konfigurasi.TIDAK_AKTIF)) {
					semesterBox.setSelectedIndex(-1);
				}

				if (Common.bolehKonfigurasi("saat_ambil_krs_secara_default_pilih_semua_prodi", Konfigurasi.TIDAK_AKTIF)) {
					searchfakultas.setSelectedIndex(-1);
					jurusanCombobox.setSelectedIndex(-1);
				}

				if (Common.getKonfigurasi("saat_ambil_krs_boleh_mengambil_dari_prodi_lain", Konfigurasi.AKTIF)
						.getNilai().equals(Konfigurasi.TIDAK_AKTIF)) {
					searchfakultas.setDisabled(true);
					jurusanCombobox.setDisabled(true);
				} else {
					searchfakultas.setDisabled(false);
					jurusanCombobox.setDisabled(false);
				}

				onSearchDefault(null);
			}
		});
	}

	/**
	 * Membangun kriteria Hibernate pencarian {@link Perkuliahan} sesuai seluruh filter aktif di layar
	 * (kelas, semester pendek, fakultas/jurusan, program, semester/ganjil-genap, tahun ajaran, tahapan,
	 * nama/kode matakuliah), membatasi hanya perkuliahan aktif, tampil-saat-KRS, dan bukan kelas
	 * paralel.
	 *
	 * @param order bila {@code true}, menambahkan pengurutan berdasarkan urutan hari dalam seminggu
	 *              lalu waktu mulai
	 * @return kriteria siap dieksekusi untuk memuat daftar perkuliahan yang cocok
	 */
	public Criteria initCriteria(boolean order) {

		Program program = (Program) (programCombobox.getSelectedItem() == null ? null
				: programCombobox.getSelectedItem().getValue());

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

				.add(remedial ? Restrictions.eq("merupakanRemedial", true)
						: Restrictions.or(Restrictions.eq("merupakanRemedial", false),
								Restrictions.isNull("merupakanRemedial")))

				.add(Restrictions.or(Restrictions.isNull("tampilkanSaatPengambilanKrs"),
						Restrictions.eq("tampilkanSaatPengambilanKrs", true)));

		criteria.add(kelas.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
				: Restrictions.ilike("kelas", kelas.getValue().trim(), MatchMode.ANYWHERE))

				.add(semesterPendek == null ? Restrictions.isNull("statusSemesterPendek")
						: Restrictions.eq("statusSemesterPendek", semesterPendek))

				.createAlias("jurusan", "jurusan")

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", searchfakultas, false))

				.add(CommonSearchFilterHelper.eqSelectedWithId("jurusan", jurusanCombobox, false))

				.add(program == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("program", program.getNama()))

				.add(semesterBox.getSelectedItem() == null || semesterBox.getSelectedItem().getValue() == null ?

						(semesterBox.getSelectedItem() != null && semesterBox.getSelectedItem().getLabel() != null
								&& semesterBox.getSelectedItem().getLabel().equalsIgnoreCase("Semua Genap")
										? Restrictions.eq("ganjilGenap", Perkuliahan.GENAP)
										: semesterBox.getSelectedItem() != null
												&& semesterBox.getSelectedItem().getLabel() != null
												&& semesterBox.getSelectedItem().getLabel()
														.equalsIgnoreCase("Semua Ganjil")
														? Restrictions.eq("ganjilGenap", Perkuliahan.GANJIL)
																: Restrictions.sqlRestriction("1=1"))

						: Restrictions.eq("semester", semesterBox.getSelectedItem().getValue()))

				.add(Restrictions.eq("tahunAjaran", tahunAjaran))

				.add(Restrictions.or(Restrictions.eq("merupakan_paralel", false),
						Restrictions.isNull("merupakan_paralel")))

				.createAlias("matakuliah", "matakuliah")

				.createAlias("kurikulumPunyaMatakuliah", "kurikulumPunyaMatakuliah", Criteria.LEFT_JOIN)

				.add(tahapanBox.getSelectedItem() == null || tahapanBox.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("kurikulumPunyaMatakuliah.tahap", tahapanBox.getSelectedItem().getValue()))

				.add(namaMk.getText().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(
								Restrictions.ilike("matakuliah.kode", namaMk.getText().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("matakuliah.nama", namaMk.getText().trim(), MatchMode.ANYWHERE)));

		if (order)
			criteria.add(Restrictions.sqlRestriction(
					"1=1 order by case hari when 'Senin' then 1 when 'Selasa' then 2 when 'Rabu' then 3"
					+ " when 'Kamis' then 4 when 'Jumat' then 5 when 'Sabtu' then 6 when 'Minggu' then 7"
					+ " else 5 end, waktu_mulai_d"));

		return criteria;
	}

	/** Menjalankan ulang pencarian ({@link #initCriteria(boolean)}) lewat paging server-side dan me-render ulang grid perkuliahan dengan {@link MatakuliahRenderer}. */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		List<Perkuliahan> matakuliah = pagingHelper.cariDenganCriteria(
				initCriteria(true),
				Perkuliahan.class);

		System.out.println("[AmbilDataPerkuliahan.onSearchDefault]"
				+ " result=" + matakuliah.size()
				+ " totalSize=" + pagingHelper.getPaging().getTotalSize()
				+ " activePage=" + pagingHelper.getPaging().getActivePage()
				+ " hashMap=" + hashMap.size()
				+ " semester=" + semester
				+ " tahunAjaran=" + tahunAjaran);
		for (int _i = 0; _i < matakuliah.size(); _i++) {
			Perkuliahan _p = matakuliah.get(_i);
			System.out.println("  [" + _i + "] id=" + (_p == null ? "null" : _p.getId())
					+ " mk=" + (_p != null && _p.getMatakuliah() != null ? _p.getMatakuliah().getNama() : "null")
					+ " kelas=" + (_p != null ? _p.getKelas() : "-"));
		}

		ListModel strset = new SimpleListModel(matakuliah);

		grid.setRowRenderer(new MatakuliahRenderer());
		grid.setModelCheckMobile(strset);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				updateStatus(null);
				reload = false;
			}
		}, "Harap tunggu, sedang menyiapkan data...");

	}
}
