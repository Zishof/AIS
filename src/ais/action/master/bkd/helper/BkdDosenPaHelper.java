package ais.action.master.bkd.helper;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zul.Label;

import ais.action.master.bkd.PenilaianAsesorAction;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.AsesemenPenilaian;
import ais.database.model.Asesor;
import ais.database.model.AsesorPegawai;
import ais.database.model.Dosen;
import ais.database.model.Jenjang;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.ParameterUmum;
import ais.database.model.Pegawai;
import ais.database.model.PenilaianAsesor;
import ais.database.model.PenunjangKinerjaDosen;
import ais.database.model.Perkuliahan;

/**
 * Helper modul BKD untuk mempopulasi ulang penilaian asesmen kegiatan <b>pembimbingan
 * akademik (PA)</b> dosen. Tiga overload {@code populate} membentuk rantai pemrosesan bertingkat:
 * varian pertama meng-iterasi seluruh jenjang pendidikan aktif; varian kedua, untuk satu jenjang,
 * memproses satu dosen tertentu atau (bila {@code pegawai} null) seluruh dosen PA yang membimbing
 * mahasiswa pada jenjang tersebut; varian ketiga menghitung SKS pembimbingan satu dosen berdasarkan
 * jumlah mahasiswa aktif bimbingannya (dibatasi maksimum SKS dari parameter umum konfigurasi),
 * menyimpan baris {@link AsesemenPenilaian}, lalu memicu {@link PenilaianAsesorAction#checkPenilaian}.
 */
public class BkdDosenPaHelper {

	/**
	 * Mempopulasi ulang penilaian PA untuk seluruh jenjang pendidikan aktif, meneruskan ke
	 * {@link #populate(Session, Pegawai, Jenjang, String, String, Label)} per jenjang.
	 *
	 * @param session       sesi Hibernate aktif
	 * @param pegawai       dosen PA yang diproses, atau {@code null} untuk semua dosen PA
	 * @param tahunAkademik tahun akademik penilaian
	 * @param semester      semester penilaian
	 * @param label         komponen label UI tempat progres ditampilkan
	 */
	@SuppressWarnings("unchecked")
	public static void populate(Session session, final Pegawai pegawai, String tahunAkademik, String semester,
			Label label) {
		List<Jenjang> jenjangs = HibernateUtil.currentSession().createCriteria(Jenjang.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();
		for (Jenjang jenjang : jenjangs) {
			BkdDosenPaHelper.populate(session, pegawai, jenjang, tahunAkademik, semester, label);
		}
	}

	/**
	 * Mempopulasi ulang penilaian PA pada {@code jenjang} tertentu: bila {@code pegawai} adalah
	 * seorang dosen, langsung diproses; bila {@code null}, seluruh dosen yang menjadi PA
	 * ({@code dosenPa}) mahasiswa aktif jenjang tersebut dicari lebih dulu lalu diproses satu per
	 * satu, dengan progres persentase ditampilkan ke {@code label}.
	 *
	 * @param session       sesi Hibernate aktif
	 * @param pegawai       dosen PA yang diproses, atau {@code null} untuk semua dosen PA jenjang ini
	 * @param jenjang       jenjang pendidikan yang diproses
	 * @param tahunAkademik tahun akademik penilaian
	 * @param semester      semester penilaian
	 * @param label         komponen label UI tempat progres ditampilkan, boleh {@code null}
	 */
	@SuppressWarnings("unchecked")
	public static void populate(Session session, final Pegawai pegawai, Jenjang jenjang, String tahunAkademik,
			String semester, Label label) {

		if (pegawai != null && pegawai.getDosen() != null) {
			BkdDosenPaHelper.populate(session, pegawai.getDosen(), jenjang, tahunAkademik, semester, label);
		} else {

			List<Long> dsns = session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setProjection(Projections.groupProperty("dosen"))
					.createAlias("jurusan", "jurusan").add(Restrictions.eq("jurusan.jenjang", jenjang))
					.add(Restrictions.isNotNull("dosen")).list();

			List<Dosen> dosens = session.createCriteria(Dosen.class)
					.add(dsns.isEmpty() ? Restrictions.sqlRestriction("false") : Restrictions.in("id", dsns)).list();

			System.out.println("jenjang => " + jenjang + ", banyak " + dosens.size());

			int i = 0;
			int rowCount = dosens.size();
			for (Dosen dosen : dosens) {
				if (label != null) {
					label.setValue("Memproses data pembimbing akademik \"" + dosen.getNama() + "\" ("
							+ Common.numberFormat.get().format(i * 100.0 / rowCount) + " %)");
				}
				i++;
				BkdDosenPaHelper.populate(session, dosen, jenjang, tahunAkademik, semester, label);
			}
		}
	}

	/**
	 * Menghitung dan menyimpan/memutakhirkan satu baris {@link AsesemenPenilaian} untuk pembimbingan
	 * akademik {@code dosen} pada {@code jenjang} tertentu. Mencari asesor aktif dosen ini terlebih
	 * dahulu (tanpa asesor, method tidak melakukan apa pun). Jumlah mahasiswa aktif bimbingan
	 * dihitung dari {@code KrsMahasiswa} pada tahun akademik+semester yang bersangkutan, dibatasi
	 * ke maksimum ({@code jumlah_sks_pembimbing_akademik_mahasiswa_<idJenjang>}, default 8) sebelum
	 * dikalikan tarif SKS per mahasiswa. Tidak melakukan apa pun bila tidak ada mahasiswa bimbingan
	 * aktif ({@code qtyBimbinganSkripis == 0}).
	 *
	 * @param session       sesi Hibernate aktif
	 * @param dosen         dosen pembimbing akademik; method langsung kembali tanpa efek bila
	 *                      {@code null} atau belum tertaut data pegawai
	 * @param jenjang       jenjang pendidikan mahasiswa bimbingan
	 * @param tahunAkademik tahun akademik penilaian
	 * @param semester      semester penilaian
	 * @param label         tidak dipakai langsung di method ini (parameter diteruskan dari pemanggil)
	 */
	@SuppressWarnings("unchecked")
	public static void populate(Session session, Dosen dosen, Jenjang jenjang, String tahunAkademik, String semester,
			Label label) {

		if (dosen == null || dosen.getPegawaiId() == null) {
			return;
		}

		List<Asesor> asesors = session.createCriteria(AsesorPegawai.class).createAlias("pegawai", "pegawai")
				.createAlias("asesor", "asesor")
				.add(Restrictions.or(Restrictions.isNull("asesor.aktif"), Restrictions.eq("asesor.aktif", true)))
				.add(Restrictions.eq("pegawai.dosen", dosen)).setProjection(Projections.groupProperty("asesor")).list();

		System.out.println("dosen => " + dosen + ", asesors => " + asesors);
		if (!asesors.isEmpty()) {

			String sql = "this_.mahasiswa in (select mahasiswa from history_status_mahasiswa where status_mahasiswa="
					+ ConstantValues.AKTIF.getId() + " and semester>=0 and tahunakademik = '" + tahunAkademik + "' and semester%2="
					+ (semester.equals(Perkuliahan.GANJIL) ? 1 : 0) + ")";

			List<String> nims = session.createCriteria(KrsMahasiswa.class).add(Restrictions.sqlRestriction(sql))
					.add(Restrictions.ge("semester", Integer.valueOf(0)))
					.add(Restrictions.eq("tahunAkademik", tahunAkademik))
					.add(Restrictions.sqlRestriction(
							"this_.semester % 2 = " + (semester.equals(Perkuliahan.GANJIL) ? "1" : "0")))
					.add(Restrictions.eq("dosenPa", dosen)).createAlias("mahasiswa", "mahasiswa")
					.add(Restrictions.eq("mahasiswa.jenjang", jenjang))
					.setProjection(Projections.groupProperty("mahasiswa.nim")).list();
			Integer qtyBimbinganSkripis = nims.size();
			if (qtyBimbinganSkripis > 0) {

				double jml = qtyBimbinganSkripis.doubleValue();

				Double sks = 0.0;

				String key = "jumlah_sks_pembimbing_akademik_mahasiswa";
				String newKey = key + "_" + jenjang.getId();

				ParameterUmum konfigurasi = Common.getParameterUmum(newKey, "0.0");

				double maks = 8.0;
				try {
					maks = Double.parseDouble(konfigurasi.getInfo1());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/bkd/helper/BkdDosenPaHelper.java:111");

				}

				if (jml > maks) {
					jml = maks;
				}

				try {
					sks = Double.parseDouble(konfigurasi.getNilai()) * jml;
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}

				String keterangan = dosen.getNama() + " sebagai pembimbing akademik sebanyak " + nims.size()
						+ " mahasiswa " + jenjang.getNama() + " yang ber-status aktif, yaitu : " + nims
						+ ". Jumlah total sks yang di dapat sebanyak " + Common.numberFormat.get().format(sks)
						+ ". Maksimal mahasiswa yang bisa dibimbing " + Common.numberFormat.get().format(maks) + ". ";

				// String ketTambahan = "";
				// for (Object[] m : mhs) {
				// ketTambahan += ketTambahan.isEmpty() ? m[0] + "-" + m[1] : ",
				// " +
				// m[0] + "-" + m[1];
				// }
				//
				// keterangan += ketTambahan;

				AsesemenPenilaian asesemenPenilaian = (AsesemenPenilaian) session
						.createCriteria(AsesemenPenilaian.class)
						.add(Restrictions.eq("pegawai.id", dosen.getPegawaiId()))
						.add(Restrictions.eq("spesifikasi", PenilaianAsesor.PEMBIMBING_AKADEMIK))
						.add(Restrictions.eq("jenjang", jenjang)).add(Restrictions.eq("tahunAkademik", tahunAkademik))
						.add(Restrictions.eq("semester", semester)).setMaxResults(1).uniqueResult();
				if (asesemenPenilaian == null) {
					asesemenPenilaian = new AsesemenPenilaian();
				}
				asesemenPenilaian.setBidang(PenunjangKinerjaDosen.PENDIDIKAN);
				asesemenPenilaian.setKeterangan(keterangan);
				asesemenPenilaian.setSpesifikasi(PenilaianAsesor.PEMBIMBING_AKADEMIK);
				asesemenPenilaian.setJenjang(jenjang);
				asesemenPenilaian.setTahunAkademik(tahunAkademik);
				asesemenPenilaian.setSemester(semester);
				asesemenPenilaian.setPegawai(new Pegawai(dosen));
				asesemenPenilaian.setSks(sks);

				session.getTransaction().begin();
				session.saveOrUpdate(asesemenPenilaian);
				session.getTransaction().commit();

				PenilaianAsesorAction.checkPenilaian(session, asesors, asesemenPenilaian);
			}
		}
	}

}
