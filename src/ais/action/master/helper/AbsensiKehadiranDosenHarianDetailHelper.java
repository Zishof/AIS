package ais.action.master.helper;

import java.util.Calendar;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import ais.ui.util.MyCaptionStyled;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Perkuliahan;
import ais.database.model.StatuskehadiranKaryawanHarian;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;

/**
 * Panel detail (baris ekspansi ZK) yang ditampilkan saat baris {@link StatuskehadiranKaryawanHarian} milik
 * seorang dosen dibuka pada layar rekap absensi kehadiran karyawan harian. Menyajikan daftar jadwal
 * {@link Perkuliahan} yang menjadi tanggung jawab dosen tersebut sebagai dosen1 atau dosen2, pada hari, tahun
 * akademik, dan jenis semester yang dipilih pengguna — sehingga admin/HRD dapat memverifikasi apakah kehadiran
 * fisik pegawai pada tanggal itu sesuai dengan jadwal mengajarnya.
 *
 * <p>Hari pembanding dihitung dari {@code statuskehadiranKaryawanHarian.getTanggal()} (bukan dari combobox),
 * sedangkan tahun akademik dan jenis semester dipilih lewat toolbar combobox pada {@link #display()}. Setiap
 * baris jadwal dapat diekspansi lagi menjadi sub-{@link MyDetail} yang baru memanggil
 * {@link AktifitasPerkuliahanHelper#initDetail} saat dibuka, untuk menampilkan agenda/aktivitas perkuliahan pada
 * sesi tersebut (jumlah agenda yang ditampilkan diatur lewat konfigurasi
 * {@code tampilan_jumlah_agenda_perkuliahan}).</p>
 *
 * <p>Kolom "Jml Mhs" dihitung dari {@link Detailperkuliahan} yang berstatus
 * {@link Detailperkuliahan#DISETUJUI} dan belum mengundurkan diri ({@code ikutiPerkuliahan} null); untuk kelas
 * paralel, penghitungan dialihkan ke {@code perkuliahan.getPerkuliahan_paralel()} karena pendaftaran mahasiswa
 * kelas paralel dicatat pada kelas induknya, bukan pada baris paralel itu sendiri.</p>
 *
 * @see MyDetail
 * @see StatuskehadiranKaryawanHarian
 * @see Perkuliahan
 * @see AktifitasPerkuliahanHelper
 */
public class AbsensiKehadiranDosenHarianDetailHelper extends MyDetail {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8709253680548100690L;
	private StatuskehadiranKaryawanHarian statuskehadiranKaryawanHarian;
	//
	// private boolean edit = false;
	private MyGrid grid;
	private Combobox tahunAkademik;
	private Combobox jenisSemester;
	protected AktifitasPerkuliahanHelper aktifitasPerkuliahanHelper;

	/**
	 * Membuat detail absensi pengajaran dosen untuk satu baris {@link StatuskehadiranKaryawanHarian}. Menyiapkan
	 * {@link AktifitasPerkuliahanHelper} dalam mode hanya-baca (parameter ketiga {@code true}) dan mendaftarkan
	 * listener {@code onOpen} yang membersihkan komponen anak lalu memanggil {@link #display()} setiap kali baris
	 * detail ini dibuka pengguna (lazy render, mengikuti pola siklus hidup {@link MyDetail}).
	 *
	 * @param statuskehadiranKaryawanHarian baris status kehadiran harian pegawai (dosen) yang sedang dibuka
	 */
	public AbsensiKehadiranDosenHarianDetailHelper(StatuskehadiranKaryawanHarian statuskehadiranKaryawanHarian) {
		this.statuskehadiranKaryawanHarian = statuskehadiranKaryawanHarian;
		aktifitasPerkuliahanHelper = new AktifitasPerkuliahanHelper(null, null, true);

		// edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		addEventListener("onOpen", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(AbsensiKehadiranDosenHarianDetailHelper.this);
				if (isOpen())
					display();
			}
		});
	}

	/**
	 * Membangun UI panel: groupbox berjudul "Daftar absensi pengajaran dosen", toolbar dengan combobox Tahun
	 * Akademik ({@link Common#generateTahunAjaran}) dan Jenis Semester (Ganjil/Genap, default sesuai
	 * {@link Common#isNowSemensterGanjil()}), serta grid berpaging (100 baris/halaman) dengan kolom Waktu,
	 * Matakuliah, Dosen 1, Dosen 2, Ruangan, Kap, Smt, dan Jml Mhs. Perubahan salah satu combobox memicu
	 * pemuatan ulang lewat {@link #loadData(Object)}. Data awal langsung dimuat; kegagalan pada pemuatan pertama
	 * ditelan dan hanya ditampilkan ke admin lewat {@link Common#tampilErrorJikaAdmin(Exception)} agar pengguna
	 * biasa tidak melihat stack trace.
	 */
	public void display() {
		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(this);
		groupbox.appendChild(new MyCaptionStyled("Daftar absensi pengajaran dosen"));

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);
		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Tahun Akademik : ")));
		toolbar.appendChild(tahunAkademik = Common.generateTahunAjaran(tahunAkademik));

		tahunAkademik.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis Semester : ")));
		toolbar.appendChild(jenisSemester = new Combobox());

		MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		jenisSemester.appendChild(comboitem);

		comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		jenisSemester.appendChild(comboitem);

		Common.selectComboItem(jenisSemester, Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);

		jenisSemester.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(100);
		grid.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("40px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Waktu");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Matakuliah");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Dosen 1");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Dosen 2");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Ruangan");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kap");
		column.setWidth("7%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Smt");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jml Mhs");
		column.setWidth("7%");

		try {
			loadData(null);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
		}

	}

	/**
	 * Memuat ulang daftar jadwal {@link Perkuliahan} milik dosen pada baris {@code statuskehadiranKaryawanHarian}
	 * sesuai tahun akademik dan jenis semester yang sedang dipilih di combobox, lalu merender ulang baris-baris
	 * grid. Kembali tanpa efek (tidak menampilkan apa pun) bila salah satu combobox belum memilih nilai.
	 *
	 * <p>Hari pembanding didapat dari {@code statuskehadiranKaryawanHarian.getTanggal()} lewat
	 * {@link Common#haris}. Query Hibernate mengambil {@link Perkuliahan} yang aktif (atau kolom {@code aktif}
	 * null), hari serta tahun ajaran cocok, semester termasuk dalam {@link Common#ganjil} atau
	 * {@link Common#genap} sesuai pilihan, dan dosen1 ATAU dosen2 sama dengan dosen pada
	 * {@code statuskehadiranKaryawanHarian} — sehingga menemukan semua sesi yang dijadwalkan untuk dosen
	 * tersebut pada hari itu, terlepas dari posisinya sebagai dosen pengajar pertama atau kedua.</p>
	 *
	 * <p>Setiap baris hasil dirender dengan: sub-{@link MyDetail} yang baru membangun tampilan agenda
	 * perkuliahan ({@link AktifitasPerkuliahanHelper#initDetail}, jumlah agenda dari konfigurasi
	 * {@code tampilan_jumlah_agenda_perkuliahan}) saat baris tersebut dibuka pengguna — bukan saat grid pertama
	 * kali dimuat; label waktu (hari, jam mulai-selesai); tautan revisi ke matakuliah lewat
	 * {@link RevisiHelper#createNewRevisi} (menandai "(Paralel)" bila kelas paralel); nama dosen1/dosen2;
	 * kode ruangan; kapasitas kelas; semester+kelas; dan jumlah mahasiswa disetujui (subquery
	 * {@link Detailperkuliahan} berstatus {@link Detailperkuliahan#DISETUJUI} dan {@code ikutiPerkuliahan} null,
	 * dialihkan ke {@code perkuliahan.getPerkuliahan_paralel()} untuk kelas paralel karena pendaftaran mahasiswa
	 * dicatat pada kelas induknya).</p>
	 *
	 * @param object tidak digunakan; parameter dipertahankan agar cocok dengan signature listener {@code onChange}
	 *               pada combobox pemanggil
	 * @throws Exception diteruskan dari akses Hibernate/komponen ZK bila terjadi kegagalan
	 */
	@SuppressWarnings("unchecked")
	public void loadData(Object object) throws Exception {

		String tahunAkademik = (String) (this.tahunAkademik.getSelectedItem() == null
				|| this.tahunAkademik.getSelectedItem().getValue() == null ? null
						: this.tahunAkademik.getSelectedItem().getValue());

		String jenisSemester = (String) (this.jenisSemester.getSelectedItem() == null ? null
				: this.jenisSemester.getSelectedItem().getValue());

		if (tahunAkademik == null) {
			// MyMessageboxConfig.show("Tahun Akademik harus dipilih",
			// "Peringatan", MyMessageboxConfig.OK,
			// MyMessageboxConfig.EXCLAMATION);
			return;
		}
		if (jenisSemester == null) {
			// MyMessageboxConfig.show("Jenis Semester harus dipilih",
			// "Peringatan", MyMessageboxConfig.OK,
			// MyMessageboxConfig.EXCLAMATION);
			return;
		}

		Calendar hari = ais.ui.util.WaktuUtil.getCalendar();
		hari.setTime(statuskehadiranKaryawanHarian.getTanggal());

		String ahari = Common.haris[hari.get(Calendar.DAY_OF_WEEK) - 1];
		System.out.println("ahari = " + ahari);
		Session session = HibernateUtil.currentSession();

		List<Perkuliahan> perkuliahans = session.createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.eq("hari", ahari))
				.add(Restrictions.eq("tahunAjaran", tahunAkademik))
				.add(Restrictions.in("semester",
						jenisSemester.equals(Perkuliahan.GANJIL) ? Common.ganjil : Common.genap))
				.add(Restrictions.or(Restrictions.eq("dosen1", statuskehadiranKaryawanHarian.getDosen()),
						Restrictions.eq("dosen2", statuskehadiranKaryawanHarian.getDosen())))
				.list();

		Rows rows = grid.getRows() == null ? new Rows() : grid.getRows();
		grid.appendChild(rows);
		rows.setParent(grid);
		Common.clear(rows);

		for (final Perkuliahan perkuliahan : perkuliahans) {
			Row arg0 = new Row();
			arg0.setParent(rows);

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {
						ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
						groupbox.setStyle("min-height: 200px;");
						int banyak = 1;
						try {
							banyak = Integer.parseInt(
									Common.getKonfigurasi("tampilan_jumlah_agenda_perkuliahan", banyak + "").getNilai());
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/AbsensiKehadiranDosenHarianDetailHelper.java:221");
						}
						aktifitasPerkuliahanHelper.initDetail(perkuliahan, new DataLoader() {

							@Override
							public void loadData(Object value) {
								// TODO Auto-generated method stub

							}
						}, groupbox, 0, banyak);
						detail.appendChild(groupbox);
					}
				}
			});

			new Label((perkuliahan.getHari() == null ? "" : perkuliahan.getHari() + ", ")
					+ ((perkuliahan.getWaktuMulai() == null ? "" : perkuliahan.getWaktuMulai()) == null ? ""
							: (perkuliahan.getWaktuMulai() == null ? "" : perkuliahan.getWaktuMulai()))
					+ "-" + ((perkuliahan.getWaktuSelesai() == null ? "" : perkuliahan.getWaktuSelesai()) == null ? ""
							: (perkuliahan.getWaktuSelesai() == null ? ""
									: perkuliahan.getWaktuSelesai()))).setParent(arg0);

			RevisiHelper.createNewRevisi(Perkuliahan.class, perkuliahan,

					perkuliahan.getMatakuliah().getNama()
							+ (perkuliahan.getMerupakan_paralel() != null && perkuliahan.getMerupakan_paralel()
									? " (Paralel) " : ""))
					.setParent(arg0);

			new Label(perkuliahan.getDosen1() == null ? "" : perkuliahan.getDosen1().getNama()).setParent(arg0);
			new Label(perkuliahan.getDosen2() == null ? "" : perkuliahan.getDosen2().getNama()).setParent(arg0);

			new Label(perkuliahan.getRuang() == null ? "" : perkuliahan.getRuang().getKodeRuangan()).setParent(arg0);

			new Label((perkuliahan.getKapasitasKelas() == null ? ""
					: Common.numberFormat.get().format(perkuliahan.getKapasitasKelas()))).setParent(arg0);

			new Label(perkuliahan.getSemester() + (perkuliahan.getKelas() == null || perkuliahan.getKelas().equals("")
					? "" : " " + perkuliahan.getKelas())).setParent(arg0);

			Integer jmlDisetujui = ((Number) session.createCriteria(Detailperkuliahan.class)
					.add(Restrictions.isNull("ikutiPerkuliahan"))
					.add(perkuliahan.getMerupakan_paralel() == null || !perkuliahan.getMerupakan_paralel()
							? Restrictions.eq("perkuliahan", perkuliahan)
							: Restrictions.eq("perkuliahan", perkuliahan.getPerkuliahan_paralel()))
					.add(Restrictions.eq("persetujuan", Detailperkuliahan.DISETUJUI))
					.setProjection(Projections.rowCount()).uniqueResult()).intValue();

			new Label(Common.numberFormat.get().format(jmlDisetujui)).setParent(arg0);

		}

	}

}
