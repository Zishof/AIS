package ais.action.master.sekolah.helper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Html;
import org.zkoss.zul.Vbox;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.GelombangPendaftaranPsb;
import ais.database.model.sekolah.JenisBiayaSekolah;
import ais.database.model.sekolah.PengaturanBiaya;
import ais.database.model.sekolah.PengaturanBiayaItemBiaya;
import ais.database.model.sekolah.PengaturanBiayaPunyaSiswa;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Tagihan;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;

/**
 * Audit baca-saja untuk mencari penyebab tagihan sekolah tidak tampil. Filter
 * produksi PengaturanBiaya diuji berurutan dan juga dilewati satu per satu agar
 * petugas memperoleh penyebab yang dapat dibuktikan, bukan tebakan.
 */
public final class AnalisisTagihanSekolahHelper {

	private AnalisisTagihanSekolahHelper() {
	}

	private static class Tahap {
		String nama;
		String nilai;
		Criterion criterion;
		int jumlah;
		int jikaDilewati;
		boolean gagalPertama;

		Tahap(String nama, String nilai, Criterion criterion) {
			this.nama = nama;
			this.nilai = nilai;
			this.criterion = criterion;
		}
	}

	public static void buka(Siswa siswa, CalonSiswa calonSiswa, JenisBiayaSekolah jenisBiaya,
			Integer bulan, Integer tahun) throws Exception {
		if (siswa == null && calonSiswa == null) {
			MyMessageboxConfig.show("Pilih siswa atau calon siswa terlebih dahulu, lalu jalankan Analisis Data kembali.",
					"Analisis Data", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return;
		}
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			List<Tahap> tahap = susunTahap(session, siswa, calonSiswa, jenisBiaya, bulan, tahun);
			boolean sudahGagal = false;
			for (int i = 0; i < tahap.size(); i++) {
				Tahap t = tahap.get(i);
				t.jumlah = hitungPengaturan(session, tahap, i, false);
				t.gagalPertama = !sudahGagal && t.jumlah == 0;
				if (t.gagalPertama)
					sudahGagal = true;
			}
			int kandidat = tahap.isEmpty() ? 0 : tahap.get(tahap.size() - 1).jumlah;
			for (int i = 0; i < tahap.size(); i++)
				tahap.get(i).jikaDilewati = hitungPengaturan(session, tahap, i, true);

			List<Long> ids = ambilIdPengaturanCocok(session, tahap);
			int item = hitungItem(session, ids);
			int settingKhusus = hitungSettingKhusus(session, siswa, calonSiswa);
			int tagihan = hitungTagihan(session, siswa, calonSiswa, jenisBiaya, bulan, tahun);
			tampilkan(siswa, calonSiswa, jenisBiaya, bulan, tahun, tahap, kandidat, item,
					settingKhusus, tagihan);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "AnalisisTagihanSekolahHelper:buka");
			MyMessageboxConfig.show("Analisis belum dapat diselesaikan: " + aman(e.getMessage()), "Analisis Data",
					MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	@SuppressWarnings("unchecked")
	private static List<Tahap> susunTahap(Session session, Siswa siswa, CalonSiswa calonSiswa,
			JenisBiayaSekolah jenisBiaya, Integer bulan, Integer tahun) {
		List<Tahap> hasil = new ArrayList<Tahap>();
		hasil.add(new Tahap("Semua Pengaturan Biaya", "Tanpa kriteria", null));
		hasil.add(new Tahap("Setting aktif", "Aktif atau belum ditentukan",
				aktifAtauNull("aktif")));
		Conjunction pemilik = Restrictions.conjunction();
		if (siswa != null) {
			pemilik.add(Restrictions.eq("jenisBiayaSekolah.gunakanCalonSiswa", false));
			pemilik.add(Restrictions.eq("jenisBiayaSekolah.sekolah", siswa.getSekolah()));
		} else {
			pemilik.add(Restrictions.eq("jenisBiayaSekolah.gunakanCalonSiswa", true));
			pemilik.add(Restrictions.eq("jenisBiayaSekolah.sekolah", calonSiswa.getSekolah()));
		}
		hasil.add(new Tahap("Versi pembayaran dan sekolah",
				(siswa != null ? "Siswa" : "Calon siswa") + " / "
						+ nama(siswa != null ? siswa.getSekolah() : calonSiswa.getSekolah()), pemilik));
		hasil.add(new Tahap("Jenis biaya aktif", "Aktif atau belum ditentukan",
				aktifAtauNull("jenisBiayaSekolah.aktif")));
		if (jenisBiaya != null)
			hasil.add(new Tahap("Jenis biaya yang dipilih", nama(jenisBiaya),
					Restrictions.eq("jenisBiayaSekolah", jenisBiaya)));
		hasil.add(new Tahap("Angkatan dan kelas", "Angkatan "
				+ (siswa != null ? siswa.getTahunMasuk() : calonSiswa.getTahunMasuk()),
				kriteriaKelasAngkatan(siswa, calonSiswa)));
		hasil.add(new Tahap("Asrama", siswa == null ? "Bukan siswa asrama"
				: (siswa.ambilasrama().isEmpty() ? "Tidak tinggal di asrama" : "Asrama siswa"),
				kriteriaAsrama(siswa)));
		Object jurusan = siswa != null ? siswa.getPenjurusanSekolah() : calonSiswa.getPenjurusanSekolah();
		hasil.add(new Tahap("Penjurusan", nama(jurusan), jurusan == null ? Restrictions.sqlRestriction("1=1")
				: Restrictions.or(Restrictions.isNull("penjurusanSekolah"), Restrictions.eq("penjurusanSekolah", jurusan))));
		Object status = siswa != null ? siswa.getStatusAwalSiswa() : calonSiswa.getStatusAwalSiswa();
		Criterion statusC = status == null ? Restrictions.isNull("statusAwalSiswa")
				: Restrictions.or(Restrictions.isNull("statusAwalSiswa"), Restrictions.eq("statusAwalSiswa", status));
		hasil.add(new Tahap("Status awal", nama(status), Restrictions.or(
				Restrictions.eq("khususBuatSiswaTertentu", true), statusC)));
		hasil.add(new Tahap("Gelombang dan status penerimaan", nama(siswa != null
				? siswa.getGelombangPendaftaranPsb() : calonSiswa.getGelombangPendaftaranPsb()),
				kriteriaGelombang(calonSiswa, siswa)));
		Criteria relasi = session.createCriteria(PengaturanBiayaPunyaSiswa.class);
		relasi.add(siswa != null ? Restrictions.eq("siswa", siswa) : Restrictions.eq("calonSiswa", calonSiswa));
		List<Long> settingKhusus = relasi.setProjection(Projections.property("pengaturanBiaya.id")).list();
		Criterion khususC = Restrictions.or(Restrictions.isNull("khususBuatSiswaTertentu"),
				Restrictions.eq("khususBuatSiswaTertentu", false));
		if (settingKhusus != null && !settingKhusus.isEmpty())
			khususC = Restrictions.or(khususC, Restrictions.in("id", settingKhusus));
		hasil.add(new Tahap("Setting khusus siswa", settingKhusus == null || settingKhusus.isEmpty()
				? "Tidak memiliki setting khusus" : settingKhusus.size() + " setting khusus terhubung", khususC));
		if (bulan != null && tahun != null) {
			Integer periode = Integer.valueOf(tahun.intValue() * 100 + bulan.intValue());
			Criterion mulai = Restrictions.or(Restrictions.isNull("bulanMulai"), Restrictions.le("bulanMulai", periode));
			Criterion sampai = Restrictions.or(Restrictions.isNull("bulanSampai"), Restrictions.ge("bulanSampai", periode));
			hasil.add(new Tahap("Rentang bulan tagihan", String.valueOf(periode), Restrictions.and(mulai, sampai)));
		}
		return hasil;
	}

	private static Criterion kriteriaKelasAngkatan(Siswa siswa, CalonSiswa calonSiswa) {
		Integer angkatan = siswa != null ? siswa.getTahunMasuk() : calonSiswa.getTahunMasuk();
		List<Long> kelas = siswa != null ? siswa.ambilkelas() : new ArrayList<Long>();
		List<Long> kelasLes = calonSiswa != null ? calonSiswa.ambilKelasLesSiswaId() : new ArrayList<Long>();
		if (siswa != null)
			kelasLes.addAll(siswa.ambilkelasLes());
		Criterion angkatanC = Restrictions.or(Restrictions.eq("tahunAngkatan", 0),
				Restrictions.eq("tahunAngkatan", angkatan));
		Criterion lesC = kelasLes.isEmpty() ? Restrictions.isNull("kelasLesSiswa")
				: Restrictions.in("kelasLesSiswa.id", kelasLes);
		Criterion kelasC = kelas.isEmpty() ? Restrictions.isNull("kelasSiswa")
				: Restrictions.or(Restrictions.isNull("kelasSiswa"), Restrictions.in("kelasSiswa.id", kelas));
		return Restrictions.or(Restrictions.eq("khususBuatSiswaTertentu", true),
				Restrictions.and(Restrictions.or(lesC, kelasC), angkatanC));
	}

	private static Criterion kriteriaAsrama(Siswa siswa) {
		List<Long> asrama = siswa != null ? siswa.ambilasrama() : new ArrayList<Long>();
		Criterion umum = Restrictions.and(Restrictions.eq("tanpaAsrama", false), Restrictions.isNull("asramaSiswa"));
		Criterion khusus = asrama.isEmpty()
				? Restrictions.or(Restrictions.isNull("tanpaAsrama"), Restrictions.eq("tanpaAsrama", true))
				: Restrictions.in("asramaSiswa.id", asrama);
		return Restrictions.or(umum, khusus);
	}

	private static Criterion kriteriaGelombang(CalonSiswa calon, Siswa siswa) {
		Criterion c1;
		if (calon == null || calon.getGelombangPendaftaranPsb() == null) {
			c1 = Restrictions.sqlRestriction("1=1");
		} else {
			GelombangPendaftaranPsb g = calon.getGelombangPendaftaranPsb();
			boolean diterima = Boolean.TRUE.equals(calon.getTelahDiterima());
			boolean verifikasi = Boolean.TRUE.equals(calon.getTerverifikasi());
			if ((diterima && Boolean.TRUE.equals(g.getSesuaiKelasSaatDiterima()))
					|| (!diterima && Boolean.TRUE.equals(g.getSesuaiKelas()))) {
				c1 = Restrictions.sqlRestriction("1=1");
			} else {
				Set<Long> ids = new HashSet<Long>();
				if (g.getJenisBiayaSekolah() != null)
					ids.add(g.getJenisBiayaSekolah().getId());
				if (verifikasi && g.getJenisBiayaSekolahTerverifikasi() != null)
					ids.add(g.getJenisBiayaSekolahTerverifikasi().getId());
				if (diterima && g.getJenisBiayaSekolahLulus() != null)
					ids.add(g.getJenisBiayaSekolahLulus().getId());
				c1 = ids.isEmpty() ? Restrictions.sqlRestriction("1=0")
						: Restrictions.in("jenisBiayaSekolah.id", ids);
			}
		}
		GelombangPendaftaranPsb g = calon != null ? calon.getGelombangPendaftaranPsb()
				: siswa != null ? siswa.getGelombangPendaftaranPsb() : null;
		Criterion c2 = g != null && g.getJenisBiayaSekolah() == null && g.getJenisBiayaSekolahLulus() == null
				? Restrictions.eq("gelombangPendaftaranPsb", g) : Restrictions.sqlRestriction("1=0");
		return Restrictions.or(c1, c2);
	}

	private static Criterion aktifAtauNull(String properti) {
		return Restrictions.or(Restrictions.isNull(properti), Restrictions.eq(properti, true));
	}

	private static int hitungPengaturan(Session session, List<Tahap> tahap, int batas, boolean lewatiBatas) {
		Criteria c = session.createCriteria(PengaturanBiaya.class).createAlias("jenisBiayaSekolah", "jenisBiayaSekolah");
		int akhir = lewatiBatas ? tahap.size() - 1 : batas;
		for (int i = 0; i <= akhir && i < tahap.size(); i++)
			if (!(lewatiBatas && i == batas) && tahap.get(i).criterion != null)
				c.add(tahap.get(i).criterion);
		Number n = (Number) c.setProjection(Projections.rowCount()).uniqueResult();
		return n == null ? 0 : n.intValue();
	}

	@SuppressWarnings("unchecked")
	private static List<Long> ambilIdPengaturanCocok(Session session, List<Tahap> tahap) {
		Criteria c = session.createCriteria(PengaturanBiaya.class).createAlias("jenisBiayaSekolah", "jenisBiayaSekolah");
		for (Tahap t : tahap)
			if (t.criterion != null)
				c.add(t.criterion);
		return c.setProjection(Projections.id()).list();
	}

	private static int hitungItem(Session session, List<Long> ids) {
		if (ids == null || ids.isEmpty())
			return 0;
		Number n = (Number) session.createCriteria(PengaturanBiayaItemBiaya.class)
				.add(Restrictions.in("pengaturanBiaya.id", ids)).setProjection(Projections.rowCount()).uniqueResult();
		return n == null ? 0 : n.intValue();
	}

	private static int hitungSettingKhusus(Session session, Siswa siswa, CalonSiswa calon) {
		Criteria c = session.createCriteria(PengaturanBiayaPunyaSiswa.class);
		c.add(siswa != null ? Restrictions.eq("siswa", siswa) : Restrictions.eq("calonSiswa", calon));
		Number n = (Number) c.setProjection(Projections.rowCount()).uniqueResult();
		return n == null ? 0 : n.intValue();
	}

	private static int hitungTagihan(Session session, Siswa siswa, CalonSiswa calon, JenisBiayaSekolah jenis,
			Integer bulan, Integer tahun) {
		Criteria c = session.createCriteria(Tagihan.class);
		c.add(siswa != null ? Restrictions.eq("siswa", siswa) : Restrictions.eq("calonSiswa", calon));
		c.add(aktifAtauNull("aktif"));
		if (jenis != null) {
			c.createAlias("pengaturanBiaya", "pengaturanBiayaAnalisis");
			c.add(Restrictions.eq("pengaturanBiayaAnalisis.jenisBiayaSekolah", jenis));
		}
		if (bulan != null && tahun != null)
			c.add(Restrictions.or(Restrictions.isNull("tahunbulan"),
					Restrictions.le("tahunbulan", Integer.valueOf(tahun.intValue() * 100 + bulan.intValue()))));
		Number n = (Number) c.setProjection(Projections.rowCount()).uniqueResult();
		return n == null ? 0 : n.intValue();
	}

	private static void tampilkan(Siswa siswa, CalonSiswa calon, JenisBiayaSekolah jenis, Integer bulan,
			Integer tahun, List<Tahap> tahap, int kandidat, int item, int khusus, int tagihan) throws InterruptedException {
		MyWindow w = new MyWindow("Analisis Data Tagihan Siswa", "none", true);
		w.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		w.setWidth("950px");
		w.setHeight("84%");
		w.setSizable(true);
		Vbox isi = new Vbox();
		isi.setWidth("100%");
		isi.setHeight("100%");
		isi.setStyle("overflow:auto;padding:12px;box-sizing:border-box");
		isi.setParent(w);
		StringBuffer h = new StringBuffer();
		String identitas = siswa != null ? siswa.getNomorInduk() + " - " + siswa.getNamaSiswa()
				: calon.getNomorInduk() + " - " + calon.getNamaSiswa();
		h.append("<div style='font-family:Segoe UI,Arial;color:#1f2937'><div style='padding:10px 12px;background:#eff6ff;border-left:4px solid #2563eb'><b>")
				.append(esc(identitas)).append("</b><br>Versi: ").append(siswa != null ? "Siswa" : "Calon siswa")
				.append(" &nbsp;|&nbsp; Jenis biaya: ").append(esc(nama(jenis)))
				.append(" &nbsp;|&nbsp; Periode pemeriksaan: ").append(bulan == null ? "Semua" : bulan).append("/")
				.append(tahun == null ? "Semua" : tahun).append("</div>")
				.append("<p>Setiap kriteria dimasukkan berurutan. Kolom <b>Jika dilewati</b> menunjukkan jumlah setting ketika hanya kriteria pada baris itu yang tidak dipakai.</p>")
				.append("<table style='width:100%;border-collapse:collapse;font-size:12px'><tr style='background:#e5e7eb'><th style='border:1px solid #ccc;padding:6px'>No</th><th style='border:1px solid #ccc;padding:6px'>Kriteria</th><th style='border:1px solid #ccc;padding:6px'>Nilai siswa</th><th style='border:1px solid #ccc;padding:6px'>Tersisa</th><th style='border:1px solid #ccc;padding:6px'>Jika dilewati</th><th style='border:1px solid #ccc;padding:6px'>Status</th></tr>");
		for (int i = 0; i < tahap.size(); i++) {
			Tahap t = tahap.get(i);
			boolean terbukti = kandidat == 0 && t.jikaDilewati > 0;
			String status = terbukti ? "PENYEBAB TERBUKTI" : t.gagalPertama ? "TITIK GAGAL" : t.jumlah > 0 ? "Cocok" : "Tetap kosong";
			h.append("<tr style='background:").append(terbukti || t.gagalPertama ? "#fee2e2" : t.jumlah > 0 ? "#f0fdf4" : "#f9fafb").append("'><td style='border:1px solid #ccc;padding:6px;text-align:center'>")
					.append(i + 1).append("</td><td style='border:1px solid #ccc;padding:6px'><b>").append(esc(t.nama))
					.append("</b></td><td style='border:1px solid #ccc;padding:6px'>").append(esc(t.nilai))
					.append("</td><td style='border:1px solid #ccc;padding:6px;text-align:center'>").append(t.jumlah)
					.append("</td><td style='border:1px solid #ccc;padding:6px;text-align:center'>").append(t.criterion == null ? "-" : t.jikaDilewati)
					.append("</td><td style='border:1px solid #ccc;padding:6px;text-align:center'>").append(status).append("</td></tr>");
		}
		h.append("</table><div style='margin-top:10px;padding:10px;background:#f8fafc;border:1px solid #cbd5e1'>Setting cocok: <b>")
				.append(kandidat).append("</b> &nbsp;|&nbsp; Item biaya: <b>").append(item)
				.append("</b> &nbsp;|&nbsp; Setting khusus siswa: <b>").append(khusus)
				.append("</b> &nbsp;|&nbsp; Tagihan aktif sampai periode pilihan: <b>").append(tagihan).append("</b></div>")
				.append(kesimpulan(tahap, kandidat, item, tagihan))
				.append("<div style='margin-top:10px;padding:11px;background:#eff6ff;border-left:4px solid #2563eb'><b>Langkah perbaikan:</b><ol style='margin:6px 0 0 20px'>")
				.append("<li>Buka menu Pembayaran &gt; Pengaturan Biaya.</li><li>Buat atau ubah setting sesuai jenis biaya, sekolah, angkatan, kelas, jurusan, status awal, asrama, dan gelombang yang ditandai gagal.</li>")
				.append("<li>Tambahkan Item Biaya beserta nominalnya jika jumlah item masih nol.</li><li>Simpan, lalu jalankan Proses/Sinkronkan Tagihan dan klik Refresh.</li><li>Jalankan Analisis Data kembali sampai semua tahap cocok dan tagihan terbentuk.</li></ol></div>")
				.append("<div style='margin-top:8px;color:#7c2d12;font-size:11px'><b>Penting:</b> jangan mengubah data siswa hanya supaya cocok dengan pengaturan. Perbaiki data siswa hanya jika data induknya memang salah; selain itu buat varian Pengaturan Biaya yang sesuai.</div></div>");
		new Html(h.toString()).setParent(isi);
		w.onModal();
	}

	private static String kesimpulan(List<Tahap> tahap, int kandidat, int item, int tagihan) {
		String pesan;
		if (kandidat == 0) {
			Tahap sebab = null;
			for (Tahap t : tahap)
				if (t.gagalPertama || t.jikaDilewati > 0) { sebab = t; break; }
			pesan = sebab == null ? "Belum ada Pengaturan Biaya yang dapat dipakai."
					: "Tagihan berhenti pada kriteria <b>" + esc(sebab.nama) + "</b> dengan nilai <b>" + esc(sebab.nilai) + "</b>.";
		} else if (item == 0) {
			pesan = "Pengaturan Biaya sudah cocok, tetapi belum mempunyai Item Biaya. Tambahkan item dan nominalnya.";
		} else if (tagihan == 0) {
			pesan = "Pengaturan dan Item Biaya sudah cocok, tetapi baris Tagihan belum terbentuk untuk periode ini. Jalankan proses/sinkronisasi tagihan lalu Refresh.";
		} else {
			pesan = "Setting dan tagihan ditemukan. Bila layar masih kosong, periksa apakah tagihan sudah lunas, tidak aktif, di luar bulan berlaku, atau tidak dipilih oleh filter tampilan.";
		}
		return "<div style='margin-top:10px;padding:11px;background:#fff7ed;border-left:4px solid #f97316'><b>Kesimpulan:</b><br>" + pesan + "</div>";
	}

	private static String nama(Object o) {
		return o == null ? "Semua / belum diisi" : String.valueOf(o);
	}

	private static String aman(String s) {
		return s == null || s.trim().isEmpty() ? "kesalahan tidak memberikan rincian" : s;
	}

	private static String esc(String s) {
		if (s == null)
			return "-";
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
				.replace("'", "&#39;");
	}
}
