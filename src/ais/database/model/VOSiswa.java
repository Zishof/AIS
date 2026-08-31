package ais.database.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;

import ais.common.BacaTulisUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.KelasLesSiswa;
import ais.database.model.sekolah.Siswa;

/**
 * Value object/proyeksi data untuk vo siswa. Tipe ini merangkum gabungan nilai yang dibutuhkan UI
 * atau laporan tanpa memperkenalkan entity persistence atau aturan transaksi baru.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GeneralValueObject}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code String dataJSON};
 * inisialisasi/lifecycle ({@code reInitHasilUjianMahasiswa()}); pembacaan/pencarian ({@code
 * getKelasLesDipilih()}, {@code ambilKelasLesSiswaId()}, {@code ambilKelasLesSiswa()}, {@code
 * ambilLokasiHasilUjianMahasiswa()}, {@code getUdahHasilUjianMahasiswa()}, {@code ambilHasilUjianMahasiswa()});
 * operasi domain lain ({@code tulisLokasiHasilUjianMahasiswa()}, {@code bersihkanLokasiHasilUjianMahasiswa()},
 * {@code populateHasilUjianMahasiswa()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface
 * yang disebut di atas.</p>
 * <p><b>Efek samping:</b> selain accessor state, operasi domain yang disebut di atas dapat membaca/mengubah
 * persistence, memicu lifecycle, atau membentuk komponen UI. Jangan menganggap model ini selalu murni;
 * panggil operasi tersebut melalui alur service dengan session, transaksi, dan otorisasi yang sesuai agar
 * perilakunya tidak disalin ke tempat lain.</p>
 *
 * @see GeneralValueObject
 */
public abstract class VOSiswa extends GeneralValueObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4136659196530916378L;

	public static String dataJSON = new JSONObject().toString();

	public abstract String getKelasLesDipilih();

	public List<Long> ambilKelasLesSiswaId() {

		List<Long> kelasLesSiswas = new ArrayList<Long>();

		for (String kode : StringUtils.split(getKelasLesDipilih(), ",")) {
			if (!kode.trim().isEmpty()) {
				Long id = !Common.isNumber(kode.trim()) ? -1L : Long.parseLong(kode.trim());
				if (id != null && !kelasLesSiswas.contains(id)) {
					kelasLesSiswas.add(id);
				}
			}
		}

		Collections.sort(kelasLesSiswas);
		return kelasLesSiswas;
	}

	public List<KelasLesSiswa> ambilKelasLesSiswa() {

		List<KelasLesSiswa> kelasLesSiswas = new ArrayList<KelasLesSiswa>();

		for (String kode : StringUtils.split(getKelasLesDipilih(), ",")) {
			if (!kode.trim().isEmpty()) {
				KelasLesSiswa kelasLesSiswa = (KelasLesSiswa) ConstantValues.simpleObject(HibernateUtil.currentSession()
						.createCriteria(KelasLesSiswa.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.eq("id", !Common.isNumber(kode.trim()) ? -1L : Long.parseLong(kode.trim())))
						.setMaxResults(1), KelasLesSiswa.class);
				if (kelasLesSiswa != null && !kelasLesSiswas.contains(kelasLesSiswa)) {
					kelasLesSiswas.add(kelasLesSiswa);
				}
			}
		}

		Collections.sort(kelasLesSiswas);
		return kelasLesSiswas;
	}

	public String ambilLokasiHasilUjianMahasiswa() {
		File file = Common.getFileLocation(this, "hasilUjianMahasiswa_" + getId().toString());
		try {
			// System.out.println(this + ", Baca file " + file);
			String data = ais.common.BacaTulisUtil.baca(file);
			return data == null || data.trim().isEmpty() ? VOMahasiswa.dataJSON : data;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOSiswa.java:78");
		}
		return VOMahasiswa.dataJSON;
	}

	public void tulisLokasiHasilUjianMahasiswa(String data) {
		File file = Common.getFileLocation(this, "hasilUjianMahasiswa_" + getId().toString());
		try {
			// System.out.println(this + ", Tulis file " + file);
			ais.common.BacaTulisUtil.tulis(file, data);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOSiswa.java:88");
		}
	}

	public void bersihkanLokasiHasilUjianMahasiswa() {
		File file = Common.getFileLocation(this, "hasilUjianMahasiswa_" + getId().toString());
		BacaTulisUtil.doHapus(file, "hasilUjianMahasiswa");

	}

	public void populateHasilUjianMahasiswa(HasilUjianMahasiswa hasilUjianMahasiswa) {
		try {
			JSONObject c = new JSONObject(ambilLokasiHasilUjianMahasiswa());
			hasilUjianMahasiswa.write();
			c.put(hasilUjianMahasiswa.getId().toString(), hasilUjianMahasiswa.getId().toString());
			tulisLokasiHasilUjianMahasiswa(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOSiswa.java:104");
		}
	}

	public boolean getUdahHasilUjianMahasiswa() {
		try {
			File file = Common.getFileLocation(this, this.getClass().getName() + "_udah_" + getId().toString());
			String data = ais.common.BacaTulisUtil.baca(file);
			System.out.println("data => " + data + ", id " + getId() + ", file " + file.getAbsolutePath());
			if (data == null || data.trim().isEmpty()) {
				ais.common.BacaTulisUtil.tulis(file, "true");
				return false;
			} else {
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VOSiswa.java:120");
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	public void reInitHasilUjianMahasiswa(Session session) {
		List<HasilUjianMahasiswa> hasilUjianMahasiswas = session.createCriteria(HasilUjianMahasiswa.class)
				.addOrder(Order.asc("id"))
				.add((this instanceof Siswa) ? Restrictions.eq("siswa", this) : Restrictions.eq("calonSiswa", this))
				.list();
		bersihkanLokasiHasilUjianMahasiswa();
		tulisLokasiHasilUjianMahasiswa(new JSONObject().toString());
		for (HasilUjianMahasiswa hasilUjianMahasiswa : hasilUjianMahasiswas) {
			masukkanData(HasilUjianMahasiswa.class, hasilUjianMahasiswa);
			populateHasilUjianMahasiswa(hasilUjianMahasiswa);
		}
		hasilUjianMahasiswas = null;
	}

	@SuppressWarnings("unchecked")
	public List<HasilUjianMahasiswa> ambilHasilUjianMahasiswa(Session session, boolean refresh) {
		if (!getUdahHasilUjianMahasiswa() || refresh) {
			reInitHasilUjianMahasiswa(session);
		}

		List<HasilUjianMahasiswa> hasilUjianMahasiswasa = new ArrayList<HasilUjianMahasiswa>();
		try {
			JSONObject c = new JSONObject(ambilLokasiHasilUjianMahasiswa());
			Iterator<String> keys = c.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				try {
					String s = c.getString(key);
					if (!s.trim().isEmpty()) {

						GeneralValueObject generalValueObject = ambilData(HasilUjianMahasiswa.class, key);
						if (generalValueObject != null) {
							HasilUjianMahasiswa hasilUjianMahasiswa = (HasilUjianMahasiswa) generalValueObject;
							if (hasilUjianMahasiswa != null && (this instanceof Siswa)) {
								hasilUjianMahasiswa.setSiswa((Siswa) this);
							} else if (hasilUjianMahasiswa != null && (this instanceof CalonSiswa)) {
								hasilUjianMahasiswa.setCalonSiswa((CalonSiswa) this);
							}
							hasilUjianMahasiswasa.add(hasilUjianMahasiswa);
						} else {

							Long hasilUjianMahasiswaId = Long.parseLong(key);
							if (session == null) {
								session = HibernateUtil.currentSession();
							}
							HasilUjianMahasiswa hasilUjianMahasiswa = (HasilUjianMahasiswa) session
									.createCriteria(HasilUjianMahasiswa.class)
									.add(Restrictions.idEq(hasilUjianMahasiswaId)).uniqueResult();
							masukkanData(HasilUjianMahasiswa.class, hasilUjianMahasiswa);
							if (hasilUjianMahasiswa != null && (this instanceof Siswa)) {
								hasilUjianMahasiswa.setSiswa((Siswa) this);
							} else if (hasilUjianMahasiswa != null && (this instanceof CalonSiswa)) {
								hasilUjianMahasiswa.setCalonSiswa((CalonSiswa) this);
							}
							hasilUjianMahasiswasa.add(hasilUjianMahasiswa);

						}
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOSiswa.java:184");

				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOSiswa.java:188");

		}
		return hasilUjianMahasiswasa;
	}

}
