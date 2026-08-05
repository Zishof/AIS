package ais.action.master.helper;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.DetailBiaya;
import ais.database.model.Detailperkuliahan;
import ais.database.model.GeneralValueObject;
import ais.database.model.ItemBiaya;
import ais.database.model.JenisKegiatan;
import ais.database.model.Kegiatan;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.Perkuliahan;

/**
 * Helper terpusat untuk menghitung nominal tagihan mahasiswa.
 *
 * Semua rumus yang sebelumnya tersebar di DetailBiaya.updateKeterangan(...) dan
 * PengaturanPembayaranBulanan.ambilNominalModifikasi(...) dipusatkan di class ini
 * agar perubahan rumus biaya cukup dilakukan di satu tempat.
 *
 * Aturan utama:
 * - TIDAK_ADA_PENGHITUNGAN selalu memakai nominal asli.
 * - DetailBiaya non-bulanan memakai nilaiBiaya sebagai harga dasar.
 * - PengaturanPembayaranBulanan memakai nominal bulanan sebagai harga dasar.
 * - Hasil 0 adalah nilai sah, bukan nilai kosong, sehingga tidak boleh dipaksa
 *   kembali ke nominal awal.
 */
public final class PembayaranNominalModifikasiHelper {

	private PembayaranNominalModifikasiHelper() {
	}

	private static Double safeDouble(Double value) {
		return value == null ? Double.valueOf(0.0) : value;
	}

	public static boolean isTanpaPenghitungan(ItemBiaya itemBiaya) {
		if (itemBiaya == null) {
			return true;
		}
		String penghitungan = itemBiaya.getPenghitungan();
		return penghitungan == null || penghitungan.trim().length() == 0
				|| ItemBiaya.TIDAK_ADA_PENGHITUNGAN.equals(penghitungan);
	}

	@SuppressWarnings("unchecked")
	public static void updateKeterangan(DetailBiaya detailBiaya, Mahasiswa mahasiswa, Integer semester) {

		if (detailBiaya == null) {
			return;
		}

		ItemBiaya itemBiaya = detailBiaya.getItemBiaya();
		if (itemBiaya == null) {
			detailBiaya.setNilaiBiayaBaru(safeDouble(detailBiaya.getNilaiBiaya()));
			return;
		}

		if (isTanpaPenghitungan(itemBiaya)) {
			detailBiaya.setNilaiBiayaBaru(safeDouble(detailBiaya.getNilaiBiaya()));
			return;
		}

		if (mahasiswa == null || mahasiswa.getId() == null || semester == null) {
			detailBiaya.setNilaiBiayaBaru(safeDouble(detailBiaya.getNilaiBiaya()));
			return;
		}

		JenisKegiatan jenisKegiatan = detailBiaya.getJenisKegiatan();

		if (mahasiswa != null && mahasiswa.getId() != null && detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getTerhubungKeNilaiTambahan() && detailBiaya.getItemBiaya().getParameterTambahan() != null) {
			String parameterTambahanInds = (String) HibernateUtil.currentSession()
					.createCriteria(BiodataMahasiswa.class).add(Restrictions.eq("mahasiswa", mahasiswa))
					.addOrder(Order.desc("id")).setMaxResults(1)
					.setProjection(Projections.property("parameterTambahanInds")).uniqueResult();
			if (parameterTambahanInds != null && !parameterTambahanInds.trim().isEmpty()) {
				String[] spl = parameterTambahanInds.split("\n");
				for (String d : spl) {
					String[] value = d.split("<=>");
					String lbl = value.length > 0 ? value[0].trim() : "";
					String val = value.length > 1 ? value[1].trim() : "";
					if (!val.isEmpty() && Common.isNumber(val)) {
						try {
							String[] labelParts = lbl.split("->");
							String parameterId = labelParts.length > 1 ? labelParts[1].trim() : "";
							if (parameterId.equalsIgnoreCase(detailBiaya.getItemBiaya().getParameterTambahan().getId().toString())) {
								detailBiaya.setNilaiBiayaBaru(Common.numberFormat.get().parse(val.trim()).doubleValue());
							}
						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
						}
					}
				}
			}
		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_SKS_MAHASISWA)) {

			Double harga = detailBiaya.getNilaiBiaya();

			boolean semua = false;
			Integer tahapan = null;
			Integer semesterPendek = null;
			boolean remedial = false;
			Integer persetujuan = null;
			Collection<Long> data = mahasiswa.ambilDetailperkuliahan(semester, tahapan, semesterPendek, remedial, semua,
					persetujuan);

			double jmlSKS = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : data) {
				Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
						detailperkuliahanid.toString());
				if (d != null && d.getPerkuliahan() != null) {
					Integer sks = d.getPerkuliahan().getMatakuliah().getSks();

					String dd = d.getPerkuliahan() == null
							? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
							: d.getPerkuliahan().getMatakuliah().getNama();
					daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

					jmlSKS += sks.doubleValue();
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x " + ((int) jmlSKS)
					+ " SKS, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jmlSKS * harga);

		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_SKS_MATAKULIAH_MENGULANG)) {

			Double harga = detailBiaya.getNilaiBiaya();

			boolean semua = false;
			Integer tahapan = null;
			Integer semesterPendek = null;
			boolean remedial = false;
			Integer persetujuan = null;
			Collection<Long> data = mahasiswa.ambilDetailperkuliahan(semester, tahapan, semesterPendek, remedial, semua,
					persetujuan);

			double jmlSKS = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : data) {
				Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
						detailperkuliahanid.toString());
				if (d != null && d.getPerkuliahan() != null && d.getPerkuliahan().getSemester() < semester) {
					Integer sks = d.getPerkuliahan().getMatakuliah().getSks();

					String dd = d.getPerkuliahan() == null
							? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
							: d.getPerkuliahan().getMatakuliah().getNama();
					daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

					jmlSKS += sks.doubleValue();
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x " + ((int) jmlSKS)
					+ " SKS, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jmlSKS * harga);

		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_SKS_MATAKULIAH_TIDAK_MENGULANG)) {

			Double harga = detailBiaya.getNilaiBiaya();

			boolean semua = false;
			Integer tahapan = null;
			Integer semesterPendek = null;
			boolean remedial = false;
			Integer persetujuan = null;
			Collection<Long> data = mahasiswa.ambilDetailperkuliahan(semester, tahapan, semesterPendek, remedial, semua,
					persetujuan);

			double jmlSKS = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : data) {
				Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
						detailperkuliahanid.toString());
				if (d != null && d.getPerkuliahan() != null && d.getPerkuliahan().getSemester().equals(semester)) {

					Integer sks = d.getPerkuliahan().getMatakuliah().getSks();

					String dd = d.getPerkuliahan() == null
							? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
							: d.getPerkuliahan().getMatakuliah().getNama();
					daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

					jmlSKS += sks.doubleValue();
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x " + ((int) jmlSKS)
					+ " SKS, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jmlSKS * harga);

		}

		else if (detailBiaya.getItemBiaya() != null && detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.HITUNG_TUNGGAKAN_SMT_LALU)
				&& semester > 1) {

			Kegiatan kegiatan = mahasiswa.ambilKegiatans((semester - 1), jenisKegiatan);
			if (kegiatan != null) {
				detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (Tungggakan semester " + (semester - 1) + " senilai Rp. "
						+ Common.numberFormat.get().format(kegiatan.getAmountTerhutang()) + ")");
				detailBiaya.setNilaiBiayaBaru(kegiatan.getAmountTerhutang());
				detailBiaya.setTunggakanLalu(kegiatan.getAmountTerhutang());
			} else {
				Collection<DetailBiaya> detailBiayas = PembayaranUtilHelper.getDetailBiayaMahasiswa(mahasiswa,
						(semester - 1), ConstantValues.PENDAFTARAN_MAHASISWA_LAMA, false);

				double totalTunggakan = 0.0;
				for (Object o : detailBiayas) {
					if (o instanceof DetailBiaya) {
						DetailBiaya biaya = (DetailBiaya) o;
						Double nilai = biaya.hitungTotalKegiatan(kegiatan);
						if (nilai > 0.01) {
							totalTunggakan += nilai;
						}
					}
				}
				detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (Tungggakan semester " + (semester - 1) + " senilai Rp. "
						+ Common.numberFormat.get().format(totalTunggakan) + ")");
				detailBiaya.setNilaiBiayaBaru(totalTunggakan);
				detailBiaya.setTunggakanLalu(totalTunggakan);
			}
		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_SKS_MK_KONVERSI)) {
			Double harga = detailBiaya.getNilaiBiaya();
			Collection<Long> data = KrsDetailHelper.ambilDetailperkuliahanKonversi(mahasiswa, semester);

			double SKSMatakuliahKonversi = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : data) {
				Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
						detailperkuliahanid.toString());
				if (d != null) {

					Integer sks = d.getMatakuliahKonversi().getSks();

					String dd = d.getPerkuliahan() == null
							? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
							: d.getPerkuliahan().getMatakuliah().getNama();
					daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

					SKSMatakuliahKonversi += sks.doubleValue();
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ ((int) SKSMatakuliahKonversi) + " SKS Konversi, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(SKSMatakuliahKonversi * harga);
		}

		else if (detailBiaya.getItemBiaya() != null && detailBiaya.getSemester() != null && mahasiswa != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_SATU_JIKA_LULUS_DISEMESTER_YANG_SAMA)) {

			boolean lulus = detailBiaya.getSemester().equals(mahasiswa.getSemesterLulus()) && mahasiswa.getSemesterLulus() != null
					&& ConstantValues.LULUS != null && mahasiswa.getStatusKeluar() != null
					&& mahasiswa.getStatusKeluar().getId().equals(ConstantValues.LULUS.getId());

			Double harga = detailBiaya.getNilaiBiaya();
			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + (lulus ? " (" + Common.numberFormat.get().format(harga)
					+ ") x 1 karena lulus di semester " + mahasiswa.getSemesterLulus() : ""));
			detailBiaya.setNilaiBiayaBaru((lulus ? 1 : 0) * harga);
		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_SATU_JIKA_AMBIL_MK_KONVERSI)) {
			Double harga = detailBiaya.getNilaiBiaya();
			Collection<Long> data = KrsDetailHelper.ambilDetailperkuliahanKonversi(mahasiswa, semester);

			String daftarMk = "";
			for (Long detailperkuliahanid : data) {
				Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
						detailperkuliahanid.toString());
				if (d != null) {

					Integer sks = d.getMatakuliahKonversi().getSks();

					String dd = d.getPerkuliahan() == null
							? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
							: d.getPerkuliahan().getMatakuliah().getNama();
					daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ (data.isEmpty() ? 0 : 1) + " Konversi, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru((data.isEmpty() ? 0 : 1) * harga);
		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_SATU_JIKA_AMBIL_MK_TERTENTU)
				&& !detailBiaya.getItemBiaya().getNamaMatakuliah().trim().isEmpty()) {
			Double harga = detailBiaya.getNilaiBiaya();
			String nama = detailBiaya.getItemBiaya().getNamaMatakuliah().trim();
			String[] spl = nama.split(";");

			Collection<Long> data = mahasiswa.ambilDetailperkuliahanMkTertentu(semester, spl);

			String daftarMk = "";
			for (Long detailperkuliahanid : data) {
				Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
						detailperkuliahanid.toString());
				if (d != null) {

					String dd = d.getPerkuliahan() == null
							? (d.getMatakuliahKonversi() == null ? ""
									: d.getMatakuliahKonversi().getNama() + "-" + d.getMatakuliahKonversi().getNama())
							: (d.getPerkuliahan().getMatakuliah().getNama() + "-"
									+ d.getPerkuliahan().getMatakuliah().getNama());
					daftarMk += daftarMk.isEmpty() ? dd : ", " + dd;
				}

			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ (data.isEmpty() ? 0 : 1) + " " + detailBiaya.getItemBiaya().getNamaMatakuliah() + ", sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru((data.isEmpty() ? 0 : 1) * harga);
		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_SATU_JIKA_AMBIL_MK_TERTENTU_DAN_SEMESTER_SEBELUMNYA)
				&& !detailBiaya.getItemBiaya().getNamaMatakuliah().trim().isEmpty()) {
			Double harga = detailBiaya.getNilaiBiaya();
			String nama = detailBiaya.getItemBiaya().getNamaMatakuliah().trim();
			String[] spl = nama.split(";");

			Collection<Long> data = mahasiswa.ambilDetailperkuliahanMkSdSmtTertentu(semester, spl);

			Map<String, Integer> daftarMk = new HashMap<String, Integer>();
			for (Long detailperkuliahanid : data) {
				Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
						detailperkuliahanid.toString());
				if (d != null) {

					String dd = d.getPerkuliahan() == null
							? (d.getMatakuliahKonversi() == null ? ""
									: d.getMatakuliahKonversi().getNama() + "-" + d.getMatakuliahKonversi().getNama())
							: (d.getPerkuliahan().getMatakuliah().getNama() + "-"
									+ d.getPerkuliahan().getMatakuliah().getNama());

					Integer c = daftarMk.get(dd.toLowerCase());
					if (c == null) {
						c = 1;
					} else {
						c++;
					}
					daftarMk.put(dd.toLowerCase(), c);
				}

			}

			System.out.println("daftarMk -> " + daftarMk);

			String mk = "";
			for (String m : daftarMk.keySet()) {
				Integer c = daftarMk.get(m.toLowerCase());
				if (c != null && c > 1) {
					mk += mk.isEmpty() ? m : ", " + m;
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ (mk.isEmpty() ? 0 : 1) + " " + detailBiaya.getItemBiaya().getNamaMatakuliah() + ", sbb : " + mk);
			detailBiaya.setNilaiBiayaBaru((mk.isEmpty() ? 0 : 1) * harga);
		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_MK_KONVERSI)) {
			Double harga = detailBiaya.getNilaiBiaya();
			Collection<Long> data = KrsDetailHelper.ambilDetailperkuliahanKonversi(mahasiswa, null);

			double SKSMatakuliahKonversi = data.size();
			String daftarMk = "";
			for (Long detailperkuliahanid : data) {
				Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
						detailperkuliahanid.toString());
				if (d != null) {

					String dd = d.getPerkuliahan() == null
							? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
							: d.getPerkuliahan().getMatakuliah().getNama();
					daftarMk += daftarMk.isEmpty() ? dd : "," + dd;
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ ((int) SKSMatakuliahKonversi) + " MK Konversi, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(SKSMatakuliahKonversi * harga);
		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_SKS_MK_PRAKTEK)) {

			Double harga = detailBiaya.getNilaiBiaya();

			boolean semua = false;
			Integer tahapan = null;
			Integer semesterPendek = null;
			boolean remedial = false;
			Integer persetujuan = null;
			Collection<Long> data = mahasiswa.ambilDetailperkuliahan(semester, tahapan, semesterPendek, remedial, semua,
					persetujuan);

			double jmlSKSPraktek = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : data) {
				Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
						detailperkuliahanid.toString());
				if (d != null && d.getPerkuliahan() != null) {
					Integer sks = d.getPerkuliahan().getMatakuliah().getSksPraktek();
					if (sks > 0) {
						String dd = d.getPerkuliahan() == null
								? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
								: d.getPerkuliahan().getMatakuliah().getNama();
						daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

						jmlSKSPraktek += sks.doubleValue();
					}
				}
			}

			data = KrsDetailHelper.ambilDetailperkuliahanKonversi(mahasiswa, semester);

			for (Long detailperkuliahanid : data) {
				Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
						detailperkuliahanid.toString());
				if (d != null) {
					Integer sks = d.getMatakuliahKonversi().getSksPraktek();
					if (sks > 0) {
						String dd = d.getPerkuliahan() == null
								? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
								: d.getPerkuliahan().getMatakuliah().getNama();
						daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

						jmlSKSPraktek += sks.doubleValue();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ ((int) jmlSKSPraktek) + " SKS, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jmlSKSPraktek * harga);

		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_SKS_MK_PRAKTEK_SP)) {

			Double harga = detailBiaya.getNilaiBiaya();

			boolean semua = false;
			Integer tahapan = null;
			Integer semesterPendek = Perkuliahan.SEMESTER_PENDEK;
			boolean remedial = false;
			Integer persetujuan = null;
			Collection<Long> data = mahasiswa.ambilDetailperkuliahan(semester, tahapan, semesterPendek, remedial, semua,
					persetujuan);

			double jmlSKSPraktek = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : data) {
				Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
						detailperkuliahanid.toString());
				if (d != null && d.getPerkuliahan() != null) {
					Integer sks = d.getPerkuliahan().getMatakuliah().getSksPraktek();
					if (sks > 0) {
						String dd = d.getPerkuliahan() == null
								? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
								: d.getPerkuliahan().getMatakuliah().getNama();
						daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

						jmlSKSPraktek += sks.doubleValue();
					}
				}
			}

			data = KrsDetailHelper.ambilDetailperkuliahanKonversi(mahasiswa, semester);

			for (Long detailperkuliahanid : data) {
				Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
						detailperkuliahanid.toString());
				if (d != null) {
					Integer sks = d.getMatakuliahKonversi().getSksPraktek();
					if (sks > 0) {
						String dd = d.getPerkuliahan() == null
								? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
								: d.getPerkuliahan().getMatakuliah().getNama();
						daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

						jmlSKSPraktek += sks.doubleValue();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ ((int) jmlSKSPraktek) + " SKS, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jmlSKSPraktek * harga);

		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_MK_SKS_DISKUSI_TEORI)) {

			Double harga = detailBiaya.getNilaiBiaya();

			boolean semua = false;
			Integer tahapan = null;
			Integer semesterPendek = null;
			boolean remedial = false;
			Integer persetujuan = null;
			Collection<Long> data = mahasiswa.ambilDetailperkuliahan(semester, tahapan, semesterPendek, remedial, semua,
					persetujuan);

			double jmlSKSDiskusi = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : data) {
				Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
						detailperkuliahanid.toString());
				if (d != null && d.getPerkuliahan() != null) {

					Integer sks = d.getPerkuliahan().getMatakuliah().getSksDiskusi();
					if (sks > 0) {
						String dd = d.getPerkuliahan() == null
								? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
								: d.getPerkuliahan().getMatakuliah().getNama();
						daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

						jmlSKSDiskusi += sks.doubleValue();
					}
				}
			}

			data = KrsDetailHelper.ambilDetailperkuliahanKonversi(mahasiswa, semester);

			for (Long detailperkuliahanid : data) {
				Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
						detailperkuliahanid.toString());
				if (d != null) {

					Integer sks = d.getMatakuliahKonversi().getSksDiskusi();
					if (sks > 0) {
						String dd = d.getPerkuliahan() == null
								? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
								: d.getPerkuliahan().getMatakuliah().getNama();
						daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

						jmlSKSDiskusi += sks.doubleValue();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ ((int) jmlSKSDiskusi) + " SKS, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jmlSKSDiskusi * harga);

		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_MK_SKS_DISKUSI_TEORI_SP)) {

			Double harga = detailBiaya.getNilaiBiaya();

			boolean semua = false;
			Integer tahapan = null;
			Integer semesterPendek = Perkuliahan.SEMESTER_PENDEK;
			boolean remedial = false;
			Integer persetujuan = null;
			Collection<Long> data = mahasiswa.ambilDetailperkuliahan(semester, tahapan, semesterPendek, remedial, semua,
					persetujuan);

			double jmlSKSDiskusi = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : data) {
				Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
						detailperkuliahanid.toString());
				if (d != null && d.getPerkuliahan() != null) {

					Integer sks = d.getPerkuliahan().getMatakuliah().getSksDiskusi();
					if (sks > 0) {
						String dd = d.getPerkuliahan() == null
								? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
								: d.getPerkuliahan().getMatakuliah().getNama();
						daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

						jmlSKSDiskusi += sks.doubleValue();
					}
				}
			}

			data = KrsDetailHelper.ambilDetailperkuliahanKonversi(mahasiswa, semester);

			for (Long detailperkuliahanid : data) {
				Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
						detailperkuliahanid.toString());
				if (d != null) {

					Integer sks = d.getMatakuliahKonversi().getSksDiskusi();
					if (sks > 0) {
						String dd = d.getPerkuliahan() == null
								? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
								: d.getPerkuliahan().getMatakuliah().getNama();
						daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

						jmlSKSDiskusi += sks.doubleValue();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ ((int) jmlSKSDiskusi) + " SKS, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jmlSKSDiskusi * harga);

		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_MK_SKS_SIMULASI)) {

			Double harga = detailBiaya.getNilaiBiaya();

			boolean semua = false;
			Integer tahapan = null;
			Integer semesterPendek = null;
			boolean remedial = false;
			Integer persetujuan = null;
			Collection<Long> data = mahasiswa.ambilDetailperkuliahan(semester, tahapan, semesterPendek, remedial, semua,
					persetujuan);

			double jmlSKSSimulasi = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : data) {
				Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
						detailperkuliahanid.toString());
				if (d != null && d.getPerkuliahan() != null) {

					Integer sks = d.getPerkuliahan().getMatakuliah().getSksSimulasi();
					if (sks > 0) {
						String dd = d.getPerkuliahan() == null
								? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
								: d.getPerkuliahan().getMatakuliah().getNama();
						daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

						jmlSKSSimulasi += sks.doubleValue();
					}
				}
			}

			data = KrsDetailHelper.ambilDetailperkuliahanKonversi(mahasiswa, semester);

			for (Long detailperkuliahanid : data) {
				Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
						detailperkuliahanid.toString());
				if (d != null) {

					Integer sks = d.getMatakuliahKonversi().getSksSimulasi();
					if (sks > 0) {
						String dd = d.getPerkuliahan() == null
								? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
								: d.getPerkuliahan().getMatakuliah().getNama();
						daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

						jmlSKSSimulasi += sks.doubleValue();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ ((int) jmlSKSSimulasi) + " SKS, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jmlSKSSimulasi * harga);

		}

		else if (detailBiaya.getItemBiaya() != null && detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_MK_UTS)) {

			Double harga = detailBiaya.getNilaiBiaya();

			Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, null, null);
			Double jumlahMatakuliah = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPerkuliahan() == null) {
						continue;
					}

					Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
							? detailperkuliahan.getMatakuliahKonversi()
							: detailperkuliahan.getPerkuliahan().getMatakuliah();
					if (matakuliah.getTerdapatUts()
							&& (detailperkuliahan.getPerkuliahan() == null
									|| detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() == null)
							&& (detailperkuliahan.getPerkuliahan() == null
									|| !detailperkuliahan.getPerkuliahan().getMerupakanRemedial())) {
						jumlahMatakuliah++;
						daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ (jumlahMatakuliah.intValue()) + " matakuliah, yaitu: " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jumlahMatakuliah * harga);

		}

		else if (detailBiaya.getItemBiaya() != null && detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_MK_UAS)) {

			Double harga = detailBiaya.getNilaiBiaya();

			Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, null, null);
			Double jumlahMatakuliah = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPerkuliahan() == null) {
						continue;
					}
					Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
							? detailperkuliahan.getMatakuliahKonversi()
							: detailperkuliahan.getPerkuliahan().getMatakuliah();
					if (matakuliah.getTerdapatUas()
							&& (detailperkuliahan.getPerkuliahan() == null
									|| detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() == null)
							&& (detailperkuliahan.getPerkuliahan() == null
									|| !detailperkuliahan.getPerkuliahan().getMerupakanRemedial())) {
						jumlahMatakuliah++;
						daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ (jumlahMatakuliah.intValue()) + " matakuliah, yaitu: " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jumlahMatakuliah * harga);

		}

		else if (detailBiaya.getItemBiaya() != null && detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_MK_UTS_SP)) {

			Double harga = detailBiaya.getNilaiBiaya();

			Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, null,
					Perkuliahan.SEMESTER_PENDEK);
			Double jumlahMatakuliah = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPerkuliahan() == null) {
						continue;
					}
					Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
							? detailperkuliahan.getMatakuliahKonversi()
							: detailperkuliahan.getPerkuliahan().getMatakuliah();
					if (matakuliah.getTerdapatUts() && detailperkuliahan.getPerkuliahan() != null
							&& !detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
						jumlahMatakuliah++;
						daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ jumlahMatakuliah.intValue() + " matakuliah, yaitu: " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jumlahMatakuliah * harga);

		}

		else if (detailBiaya.getItemBiaya() != null && detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_MK_UAS_SP)) {

			Double harga = detailBiaya.getNilaiBiaya();

			Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, null,
					Perkuliahan.SEMESTER_PENDEK);
			Double jumlahMatakuliah = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPerkuliahan() == null) {
						continue;
					}
					Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
							? detailperkuliahan.getMatakuliahKonversi()
							: detailperkuliahan.getPerkuliahan().getMatakuliah();
					if (matakuliah.getTerdapatUas() && detailperkuliahan.getPerkuliahan() != null
							&& !detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
						jumlahMatakuliah++;
						daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ jumlahMatakuliah.intValue() + " matakuliah, yaitu: " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jumlahMatakuliah * harga);

		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_MATAKULIAH_REMEDIAL)) {

			Double harga = detailBiaya.getNilaiBiaya();

			Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, null, null, true);
			Double jumlahMatakuliah = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPerkuliahan() == null) {
						continue;
					}
					Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
							? detailperkuliahan.getMatakuliahKonversi()
							: detailperkuliahan.getPerkuliahan().getMatakuliah();
					if (matakuliah != null && detailperkuliahan.getPerkuliahan() != null
							&& detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
						jumlahMatakuliah++;
						daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
					}
				}
			}
			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ jumlahMatakuliah.intValue() + " remedial, yaitu: " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jumlahMatakuliah * harga);

		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_MATAKULIAH_REMEDIAL_1_SKS)) {

			Double harga = detailBiaya.getNilaiBiaya();

			Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, null, null, true);
			Double jumlahMatakuliah = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPerkuliahan() == null) {
						continue;
					}
					Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
							? detailperkuliahan.getMatakuliahKonversi()
							: detailperkuliahan.getPerkuliahan().getMatakuliah();
					if (matakuliah != null && matakuliah.getSks().equals(1)
							&& detailperkuliahan.getPerkuliahan() != null
							&& detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
						jumlahMatakuliah++;
						daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ jumlahMatakuliah.intValue() + " remedial 1 SKS, yaitu: " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jumlahMatakuliah * harga);

		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_MATAKULIAH_REMEDIAL_2_SKS)) {

			Double harga = detailBiaya.getNilaiBiaya();

			Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, null, null, true);
			Double jumlahMatakuliah = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPerkuliahan() == null) {
						continue;
					}
					Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
							? detailperkuliahan.getMatakuliahKonversi()
							: detailperkuliahan.getPerkuliahan().getMatakuliah();
					if (matakuliah != null && matakuliah.getSks().equals(2)
							&& detailperkuliahan.getPerkuliahan() != null
							&& detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
						jumlahMatakuliah++;
						daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
					}
				}
			}
			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ jumlahMatakuliah.intValue() + " remedial 2 SKS, yaitu: " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jumlahMatakuliah * harga);

		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_MATAKULIAH_REMEDIAL_3_SKS)) {

			Double harga = detailBiaya.getNilaiBiaya();

			Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, null, null, true);
			Double jumlahMatakuliah = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPerkuliahan() == null) {
						continue;
					}
					Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
							? detailperkuliahan.getMatakuliahKonversi()
							: detailperkuliahan.getPerkuliahan().getMatakuliah();
					if (matakuliah != null && matakuliah.getSks().equals(3)
							&& detailperkuliahan.getPerkuliahan() != null
							&& detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
						jumlahMatakuliah++;
						daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ jumlahMatakuliah.intValue() + " remedial 3 SKS, yaitu: " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jumlahMatakuliah * harga);

		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_MATAKULIAH_REMEDIAL_4_SKS)) {

			Double harga = detailBiaya.getNilaiBiaya();

			Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, null, null, true);
			Double jumlahMatakuliah = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPerkuliahan() == null) {
						continue;
					}
					Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
							? detailperkuliahan.getMatakuliahKonversi()
							: detailperkuliahan.getPerkuliahan().getMatakuliah();
					if (matakuliah != null && matakuliah.getSks().equals(4)
							&& detailperkuliahan.getPerkuliahan() != null
							&& detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
						jumlahMatakuliah++;
						daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ jumlahMatakuliah.intValue() + " remedial 4 SKS, yaitu: " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jumlahMatakuliah * harga);

		}

		////////////////////////////////////////////////

		else if (detailBiaya.getItemBiaya() != null && detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_SKS_UTS)) {

			Double harga = detailBiaya.getNilaiBiaya();

			Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, null, null);
			Double jumlahMatakuliah = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPerkuliahan() == null) {
						continue;
					}
					Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
							? detailperkuliahan.getMatakuliahKonversi()
							: detailperkuliahan.getPerkuliahan().getMatakuliah();
					if (matakuliah.getTerdapatUts()
							&& (detailperkuliahan.getPerkuliahan() == null
									|| detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() == null)
							&& (detailperkuliahan.getPerkuliahan() == null
									|| !detailperkuliahan.getPerkuliahan().getMerupakanRemedial())) {
						jumlahMatakuliah += matakuliah.getSks();
						daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ jumlahMatakuliah.intValue() + " SKS, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jumlahMatakuliah * harga);

		}

		else if (detailBiaya.getItemBiaya() != null && detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_SKS_UAS)) {

			Double harga = detailBiaya.getNilaiBiaya();

			Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, null, null);
			Double jumlahMatakuliah = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPerkuliahan() == null) {
						continue;
					}
					Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
							? detailperkuliahan.getMatakuliahKonversi()
							: detailperkuliahan.getPerkuliahan().getMatakuliah();
					if (matakuliah.getTerdapatUas()
							&& (detailperkuliahan.getPerkuliahan() == null
									|| detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() == null)
							&& (detailperkuliahan.getPerkuliahan() == null
									|| !detailperkuliahan.getPerkuliahan().getMerupakanRemedial())) {
						jumlahMatakuliah += matakuliah.getSks();
						daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ jumlahMatakuliah.intValue() + " SKS, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jumlahMatakuliah * harga);

		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_SKS_UTS_SP)) {

			Double harga = detailBiaya.getNilaiBiaya();

			Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, null,
					Perkuliahan.SEMESTER_PENDEK);
			Double jumlahMatakuliah = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPerkuliahan() == null) {
						continue;
					}
					Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
							? detailperkuliahan.getMatakuliahKonversi()
							: detailperkuliahan.getPerkuliahan().getMatakuliah();
					if (matakuliah.getTerdapatUts() && detailperkuliahan.getPerkuliahan() != null
							&& detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() != null
							&& !detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
						jumlahMatakuliah += matakuliah.getSks();
						daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ jumlahMatakuliah.intValue() + " SKS, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jumlahMatakuliah * harga);

		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_SKS_UTS_REMEDIAL)) {

			Double harga = detailBiaya.getNilaiBiaya();

			Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, null, null, true, false,
					null);
			Double jumlahMatakuliah = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPerkuliahan() == null) {
						continue;
					}
					Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
							? detailperkuliahan.getMatakuliahKonversi()
							: detailperkuliahan.getPerkuliahan().getMatakuliah();
					if (matakuliah.getTerdapatUts() && detailperkuliahan.getPerkuliahan() != null
							&& detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
						jumlahMatakuliah += matakuliah.getSks();
						daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ jumlahMatakuliah.intValue() + " SKS, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jumlahMatakuliah * harga);

		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_SKS_UTS_REMEDIAL)) {

			Double harga = detailBiaya.getNilaiBiaya();

			Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, null, null, true, false,
					null);
			Double jumlahMatakuliah = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPerkuliahan() == null) {
						continue;
					}
					Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
							? detailperkuliahan.getMatakuliahKonversi()
							: detailperkuliahan.getPerkuliahan().getMatakuliah();
					if (matakuliah.getTerdapatUts() && detailperkuliahan.getPerkuliahan() != null
							&& detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
						jumlahMatakuliah += matakuliah.getSks();
						daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ jumlahMatakuliah.intValue() + " SKS, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jumlahMatakuliah * harga);

		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_SKS_UAS_SP)) {

			Double harga = detailBiaya.getNilaiBiaya();

			Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, null,
					Perkuliahan.SEMESTER_PENDEK);
			Double jumlahMatakuliah = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPerkuliahan() == null) {
						continue;
					}
					Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
							? detailperkuliahan.getMatakuliahKonversi()
							: detailperkuliahan.getPerkuliahan().getMatakuliah();
					if (matakuliah.getTerdapatUas() && detailperkuliahan.getPerkuliahan() != null
							&& detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() != null
							&& !detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
						jumlahMatakuliah += matakuliah.getSks();
						daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ jumlahMatakuliah.intValue() + " SKS, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jumlahMatakuliah * harga);

		}

		///////////////////////////////////////////////

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_MK_PRAKTEK)) {

			Double harga = detailBiaya.getNilaiBiaya();

			Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, null, null);

			String daftarMk = "";
			int jumlahMatakuliah = 0;
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPerkuliahan() == null) {
						continue;
					}
					Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
							? detailperkuliahan.getMatakuliahKonversi()
							: detailperkuliahan.getPerkuliahan().getMatakuliah();
					if (matakuliah.getMerupakanMkPraktek()) {
						jumlahMatakuliah++;
						daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ (jumlahMatakuliah) + " matakuliah, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jumlahMatakuliah * harga);

		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_MK_DISKUSI_TEORI)) {

			Double harga = detailBiaya.getNilaiBiaya();

			Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, null, null);

			String daftarMk = "";
			int jumlahMatakuliah = 0;
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPerkuliahan() == null) {
						continue;
					}
					Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
							? detailperkuliahan.getMatakuliahKonversi()
							: detailperkuliahan.getPerkuliahan().getMatakuliah();
					if (matakuliah.getMerupakanMkTeori()) {
						jumlahMatakuliah++;
						daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ (jumlahMatakuliah) + " matakuliah, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jumlahMatakuliah * harga);

		}

		else if (detailBiaya.getItemBiaya() != null && detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_MK)) {

			Double harga = detailBiaya.getNilaiBiaya();

			Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, null, null);
			Double jumlahMatakuliah = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPerkuliahan() == null) {
						continue;
					}
					Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
							? detailperkuliahan.getMatakuliahKonversi()
							: detailperkuliahan.getPerkuliahan().getMatakuliah();
					if ((detailperkuliahan.getPerkuliahan() == null
							|| detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() == null)
							&& (detailperkuliahan.getPerkuliahan() == null
									|| !detailperkuliahan.getPerkuliahan().getMerupakanRemedial())) {
						jumlahMatakuliah++;
						daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ jumlahMatakuliah.intValue() + " matakuliah, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jumlahMatakuliah * harga);

		}

		else if (detailBiaya.getItemBiaya() != null && detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_MK_SP)) {

			Double harga = detailBiaya.getNilaiBiaya();

			Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, null,
					Perkuliahan.SEMESTER_PENDEK);
			Double jumlahMatakuliah = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPerkuliahan() == null) {
						continue;
					}
					Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
							? detailperkuliahan.getMatakuliahKonversi()
							: detailperkuliahan.getPerkuliahan().getMatakuliah();
					if (detailperkuliahan.getPerkuliahan() != null
							&& detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() != null
							&& !detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
						jumlahMatakuliah++;
						daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ jumlahMatakuliah.intValue() + " matakuliah, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jumlahMatakuliah * harga);

		} else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_SATU_JIKA_AMBIL_MK_SP)) {

			Double harga = detailBiaya.getNilaiBiaya();

			Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, null,
					Perkuliahan.SEMESTER_PENDEK);
			Double jumlahMatakuliah = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPerkuliahan() == null) {
						continue;
					}
					Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
							? detailperkuliahan.getMatakuliahKonversi()
							: detailperkuliahan.getPerkuliahan().getMatakuliah();
					if (detailperkuliahan.getPerkuliahan() != null
							&& detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() != null
							&& !detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
						jumlahMatakuliah++;
						daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ (jumlahMatakuliah.intValue() > 0 ? 1 : 0) + " SP, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru((jumlahMatakuliah.intValue() > 0 ? 1 : 0) * harga);

		}

	
	}

	public static Double ambilNominalModifikasi(PengaturanPembayaranBulanan pengaturanPembayaranBulanan,
			Mahasiswa mahasiswa, Integer semester) {

		if (pengaturanPembayaranBulanan == null) {
			return Double.valueOf(0.0);
		}

		Double nominalModifikasi = pengaturanPembayaranBulanan.getNominal();
		if (nominalModifikasi == null) {
			nominalModifikasi = Double.valueOf(0.0);
		}

		DetailBiaya detailBiaya = pengaturanPembayaranBulanan.getDetailBiaya();
		ItemBiaya itemBiaya = detailBiaya == null ? null : detailBiaya.getItemBiaya();
		if (isTanpaPenghitungan(itemBiaya)) {
			return nominalModifikasi;
		}

		if (mahasiswa == null || semester == null) {
			return nominalModifikasi;
		}

		Integer tahapan = pengaturanPembayaranBulanan.hitungTahap(mahasiswa, semester);
		if (pengaturanPembayaranBulanan.getDetailBiaya() != null) {

			if (pengaturanPembayaranBulanan.getDikalikanDenganKondisiKhusus()) {

				if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_SKS_MAHASISWA)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					boolean semua = false;
					Integer semesterPendek = null;
					boolean remedial = false;
					Integer persetujuan = null;
					Collection<Long> data = mahasiswa.ambilDetailperkuliahan(semester, tahapan, semesterPendek,
							remedial, semua, persetujuan);

					double jmlSKS = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : data) {
						Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
								detailperkuliahanid.toString());
						if (d != null && d.getPerkuliahan() != null) {
							Integer sks = d.getPerkuliahan().getMatakuliah().getSks();

							String dd = d.getPerkuliahan() == null
									? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
									: d.getPerkuliahan().getMatakuliah().getNama();
							daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

							jmlSKS += sks.doubleValue();
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + ((int) jmlSKS) + " SKS, sbb : " + daftarMk);
					nominalModifikasi = (jmlSKS * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_SKS_MATAKULIAH_MENGULANG)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					boolean semua = false;
					Integer semesterPendek = null;
					boolean remedial = false;
					Integer persetujuan = null;
					Collection<Long> data = mahasiswa.ambilDetailperkuliahan(semester, tahapan, semesterPendek,
							remedial, semua, persetujuan);

					double jmlSKS = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : data) {
						Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
								detailperkuliahanid.toString());
						if (d != null && d.getPerkuliahan() != null && d.getPerkuliahan().getSemester() < semester) {
							Integer sks = d.getPerkuliahan().getMatakuliah().getSks();

							String dd = d.getPerkuliahan() == null
									? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
									: d.getPerkuliahan().getMatakuliah().getNama();
							daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

							jmlSKS += sks.doubleValue();
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + ((int) jmlSKS) + " SKS, sbb : " + daftarMk);
					nominalModifikasi = (jmlSKS * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_SKS_MATAKULIAH_TIDAK_MENGULANG)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					boolean semua = false;
					Integer semesterPendek = null;
					boolean remedial = false;
					Integer persetujuan = null;
					Collection<Long> data = mahasiswa.ambilDetailperkuliahan(semester, tahapan, semesterPendek,
							remedial, semua, persetujuan);

					double jmlSKS = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : data) {
						Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
								detailperkuliahanid.toString());
						if (d != null && d.getPerkuliahan() != null
								&& d.getPerkuliahan().getSemester().equals(semester)) {

							Integer sks = d.getPerkuliahan().getMatakuliah().getSks();

							String dd = d.getPerkuliahan() == null
									? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
									: d.getPerkuliahan().getMatakuliah().getNama();
							daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

							jmlSKS += sks.doubleValue();
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + (jmlSKS) + " SKS, sbb : " + daftarMk);
					nominalModifikasi = (jmlSKS * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getSemester() != null
						&& pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
								.equals(ItemBiaya.DIKALI_SATU_JIKA_LULUS_DISEMESTER_YANG_SAMA)) {

					boolean lulus = pengaturanPembayaranBulanan.getDetailBiaya().getSemester().equals(mahasiswa.getSemesterLulus())
							&& mahasiswa.getSemesterLulus() != null && ConstantValues.LULUS != null
							&& mahasiswa.getStatusKeluar() != null
							&& mahasiswa.getStatusKeluar().getId().equals(ConstantValues.LULUS.getId());

					Double harga = pengaturanPembayaranBulanan.getNominal();
					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama()
							+ (lulus ? " (" + Common.numberFormat.get().format(harga) + ") x 1 karena lulus di semester "
									+ mahasiswa.getSemesterLulus() : ""));
					nominalModifikasi = ((lulus ? 1 : 0) * harga);
				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_SATU_JIKA_AMBIL_MK_KONVERSI)) {
					Double harga = pengaturanPembayaranBulanan.getNominal();
					Collection<Long> data = KrsDetailHelper.ambilDetailperkuliahanKonversi(mahasiswa, semester);

					String daftarMk = "";
					for (Long detailperkuliahanid : data) {
						Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
								detailperkuliahanid.toString());
						if (d != null) {

							Integer sks = d.getMatakuliahKonversi().getSks();

							String dd = d.getPerkuliahan() == null
									? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
									: d.getPerkuliahan().getMatakuliah().getNama();
							daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + (data.isEmpty() ? 0 : 1) + " Konversi, sbb : " + daftarMk);
					nominalModifikasi = ((data.isEmpty() ? 0 : 1) * harga);
				} else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null
						&& pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
								.equals(ItemBiaya.DIKALI_SATU_JIKA_AMBIL_MK_TERTENTU)
						&& !pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNamaMatakuliah().trim().isEmpty()) {
					Double harga = pengaturanPembayaranBulanan.getNominal();

					String nama = pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNamaMatakuliah().trim();
					String[] spl = nama.split(";");

					Collection<Long> data = mahasiswa.ambilDetailperkuliahanMkTertentu(semester, spl);

					String daftarMk = "";
					for (Long detailperkuliahanid : data) {
						Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
								detailperkuliahanid.toString());
						if (d != null) {

							String dd = d.getPerkuliahan() == null
									? (d.getMatakuliahKonversi() == null ? ""
											: d.getMatakuliahKonversi().getNama() + "-"
													+ d.getMatakuliahKonversi().getNama())
									: (d.getPerkuliahan().getMatakuliah().getNama() + "-"
											+ d.getPerkuliahan().getMatakuliah().getNama());
							daftarMk += daftarMk.isEmpty() ? dd : ", " + dd;
						}

					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + (data.isEmpty() ? 0 : 1) + " "
							+ pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNamaMatakuliah() + ", sbb : " + daftarMk);
					nominalModifikasi = ((data.isEmpty() ? 0 : 1) * harga);
				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null
						&& pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
								.equals(ItemBiaya.DIKALI_SATU_JIKA_AMBIL_MK_TERTENTU_DAN_SEMESTER_SEBELUMNYA)
						&& !pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNamaMatakuliah().trim().isEmpty()) {
					Double harga = pengaturanPembayaranBulanan.getNominal();
					String nama = pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNamaMatakuliah().trim();
					String[] spl = nama.split(";");

					Collection<Long> data = mahasiswa.ambilDetailperkuliahanMkSdSmtTertentu(semester, spl);

					Map<String, Integer> daftarMk = new HashMap<String, Integer>();
					for (Long detailperkuliahanid : data) {
						Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
								detailperkuliahanid.toString());
						if (d != null) {

							String dd = d.getPerkuliahan() == null
									? (d.getMatakuliahKonversi() == null ? ""
											: d.getMatakuliahKonversi().getNama() + "-"
													+ d.getMatakuliahKonversi().getNama())
									: (d.getPerkuliahan().getMatakuliah().getNama() + "-"
											+ d.getPerkuliahan().getMatakuliah().getNama());

							Integer c = daftarMk.get(dd.toLowerCase());
							if (c == null) {
								c = 1;
							} else {
								c++;
							}
							daftarMk.put(dd.toLowerCase(), c);
						}

					}

					System.out.println("daftarMk -> " + daftarMk);

					String mk = "";
					for (String m : daftarMk.keySet()) {
						Integer c = daftarMk.get(m.toLowerCase());
						if (c != null && c > 1) {
							mk += mk.isEmpty() ? m : ", " + m;
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + (mk.isEmpty() ? 0 : 1) + " "
							+ pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNamaMatakuliah() + ", sbb : " + mk);

					nominalModifikasi = ((mk.isEmpty() ? 0 : 1) * harga);
				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_SATU_JIKA_AMBIL_MK_SP)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, tahapan,
							Perkuliahan.SEMESTER_PENDEK);
					Double jumlahMatakuliah = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {
							if (detailperkuliahan.getPerkuliahan() == null) {
								continue;
							}
							Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
									? detailperkuliahan.getMatakuliahKonversi()
									: detailperkuliahan.getPerkuliahan().getMatakuliah();
							if (detailperkuliahan.getPerkuliahan() != null
									&& detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() != null
									&& !detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
								jumlahMatakuliah++;
								daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + ((jumlahMatakuliah > 0 ? 1 : 0)) + " jumlah matakuliah, sbb : " + daftarMk);
					nominalModifikasi = ((jumlahMatakuliah > 0 ? 1 : 0) * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_SKS_MK_KONVERSI)) {
					Double harga = pengaturanPembayaranBulanan.getNominal();
					Collection<Long> data = KrsDetailHelper.ambilDetailperkuliahanKonversi(mahasiswa, semester);

					double SKSMatakuliahKonversi = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : data) {
						Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
								detailperkuliahanid.toString());
						if (d != null) {

							Integer sks = d.getMatakuliahKonversi().getSks();

							String dd = d.getPerkuliahan() == null
									? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
									: d.getPerkuliahan().getMatakuliah().getNama();
							daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

							SKSMatakuliahKonversi += sks.doubleValue();
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + ((int) SKSMatakuliahKonversi) + " SKS, sbb : " + daftarMk);
					nominalModifikasi = (SKSMatakuliahKonversi * harga);
				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_MK_KONVERSI)) {
					Double harga = pengaturanPembayaranBulanan.getNominal();
					Collection<Long> data = KrsDetailHelper.ambilDetailperkuliahanKonversi(mahasiswa, null);

					double SKSMatakuliahKonversi = data.size();
					String daftarMk = "";
					for (Long detailperkuliahanid : data) {
						Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
								detailperkuliahanid.toString());
						if (d != null) {

							String dd = d.getPerkuliahan() == null
									? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
									: d.getPerkuliahan().getMatakuliah().getNama();
							daftarMk += daftarMk.isEmpty() ? dd : "," + dd;
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + ((int) SKSMatakuliahKonversi) + " matakuliah, sbb : " + daftarMk);
					nominalModifikasi = (SKSMatakuliahKonversi * harga);
				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_SKS_MK_PRAKTEK)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					boolean semua = false;
					Integer semesterPendek = null;
					boolean remedial = false;
					Integer persetujuan = null;
					Collection<Long> data = mahasiswa.ambilDetailperkuliahan(semester, tahapan, semesterPendek,
							remedial, semua, persetujuan);

					double jmlSKSPraktek = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : data) {
						Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
								detailperkuliahanid.toString());
						if (d != null && d.getPerkuliahan() != null) {
							Integer sks = d.getPerkuliahan().getMatakuliah().getSksPraktek();
							if (sks > 0) {
								String dd = d.getPerkuliahan() == null
										? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
										: d.getPerkuliahan().getMatakuliah().getNama();
								daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

								jmlSKSPraktek += sks.doubleValue();
							}
						}
					}

					data = KrsDetailHelper.ambilDetailperkuliahanKonversi(mahasiswa, semester);

					for (Long detailperkuliahanid : data) {
						Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
								detailperkuliahanid.toString());
						if (d != null) {
							Integer sks = d.getMatakuliahKonversi().getSksPraktek();
							if (sks > 0) {
								String dd = d.getPerkuliahan() == null
										? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
										: d.getPerkuliahan().getMatakuliah().getNama();
								daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

								jmlSKSPraktek += sks.doubleValue();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + ((int) jmlSKSPraktek) + " SKS, sbb : " + daftarMk);
					nominalModifikasi = (jmlSKSPraktek * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_SKS_MK_PRAKTEK_SP)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					boolean semua = false;
					Integer semesterPendek = Perkuliahan.SEMESTER_PENDEK;
					boolean remedial = false;
					Integer persetujuan = null;
					Collection<Long> data = mahasiswa.ambilDetailperkuliahan(semester, tahapan, semesterPendek,
							remedial, semua, persetujuan);

					double jmlSKSPraktek = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : data) {
						Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
								detailperkuliahanid.toString());
						if (d != null && d.getPerkuliahan() != null) {
							Integer sks = d.getPerkuliahan().getMatakuliah().getSksPraktek();
							if (sks > 0) {
								String dd = d.getPerkuliahan() == null
										? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
										: d.getPerkuliahan().getMatakuliah().getNama();
								daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

								jmlSKSPraktek += sks.doubleValue();
							}
						}
					}

					data = KrsDetailHelper.ambilDetailperkuliahanKonversi(mahasiswa, semester);

					for (Long detailperkuliahanid : data) {
						Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
								detailperkuliahanid.toString());
						if (d != null) {
							Integer sks = d.getMatakuliahKonversi().getSksPraktek();
							if (sks > 0) {
								String dd = d.getPerkuliahan() == null
										? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
										: d.getPerkuliahan().getMatakuliah().getNama();
								daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

								jmlSKSPraktek += sks.doubleValue();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + ((int) jmlSKSPraktek) + " SKS, sbb : " + daftarMk);
					nominalModifikasi = (jmlSKSPraktek * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_MK_SKS_DISKUSI_TEORI)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					boolean semua = false;
					Integer semesterPendek = null;
					boolean remedial = false;
					Integer persetujuan = null;
					Collection<Long> data = mahasiswa.ambilDetailperkuliahan(semester, tahapan, semesterPendek,
							remedial, semua, persetujuan);

					double jmlSKSDiskusi = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : data) {
						Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
								detailperkuliahanid.toString());
						if (d != null && d.getPerkuliahan() != null) {

							Integer sks = d.getPerkuliahan().getMatakuliah().getSksDiskusi();
							if (sks > 0) {
								String dd = d.getPerkuliahan() == null
										? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
										: d.getPerkuliahan().getMatakuliah().getNama();
								daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

								jmlSKSDiskusi += sks.doubleValue();
							}
						}
					}

					data = KrsDetailHelper.ambilDetailperkuliahanKonversi(mahasiswa, semester);

					for (Long detailperkuliahanid : data) {
						Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
								detailperkuliahanid.toString());
						if (d != null) {

							Integer sks = d.getMatakuliahKonversi().getSksDiskusi();
							if (sks > 0) {
								String dd = d.getPerkuliahan() == null
										? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
										: d.getPerkuliahan().getMatakuliah().getNama();
								daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

								jmlSKSDiskusi += sks.doubleValue();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + ((int) jmlSKSDiskusi) + " SKS, sbb : " + daftarMk);
					nominalModifikasi = (jmlSKSDiskusi * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_MK_SKS_DISKUSI_TEORI_SP)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					boolean semua = false;
					Integer semesterPendek = Perkuliahan.SEMESTER_PENDEK;
					boolean remedial = false;
					Integer persetujuan = null;
					Collection<Long> data = mahasiswa.ambilDetailperkuliahan(semester, tahapan, semesterPendek,
							remedial, semua, persetujuan);

					double jmlSKSDiskusi = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : data) {
						Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
								detailperkuliahanid.toString());
						if (d != null && d.getPerkuliahan() != null) {

							Integer sks = d.getPerkuliahan().getMatakuliah().getSksDiskusi();
							if (sks > 0) {
								String dd = d.getPerkuliahan() == null
										? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
										: d.getPerkuliahan().getMatakuliah().getNama();
								daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

								jmlSKSDiskusi += sks.doubleValue();
							}
						}
					}

					data = KrsDetailHelper.ambilDetailperkuliahanKonversi(mahasiswa, semester);

					for (Long detailperkuliahanid : data) {
						Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
								detailperkuliahanid.toString());
						if (d != null) {

							Integer sks = d.getMatakuliahKonversi().getSksDiskusi();
							if (sks > 0) {
								String dd = d.getPerkuliahan() == null
										? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
										: d.getPerkuliahan().getMatakuliah().getNama();
								daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

								jmlSKSDiskusi += sks.doubleValue();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + ((int) jmlSKSDiskusi) + " SKS, sbb : " + daftarMk);
					nominalModifikasi = (jmlSKSDiskusi * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_MK_SKS_SIMULASI)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					boolean semua = false;
					Integer semesterPendek = null;
					boolean remedial = false;
					Integer persetujuan = null;
					Collection<Long> data = mahasiswa.ambilDetailperkuliahan(semester, tahapan, semesterPendek,
							remedial, semua, persetujuan);

					double jmlSKSSimulasi = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : data) {
						Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
								detailperkuliahanid.toString());
						if (d != null && d.getPerkuliahan() != null) {

							Integer sks = d.getPerkuliahan().getMatakuliah().getSksSimulasi();
							if (sks > 0) {
								String dd = d.getPerkuliahan() == null
										? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
										: d.getPerkuliahan().getMatakuliah().getNama();
								daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

								jmlSKSSimulasi += sks.doubleValue();
							}
						}
					}

					data = KrsDetailHelper.ambilDetailperkuliahanKonversi(mahasiswa, semester);

					for (Long detailperkuliahanid : data) {
						Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
								detailperkuliahanid.toString());
						if (d != null) {

							Integer sks = d.getMatakuliahKonversi().getSksSimulasi();
							if (sks > 0) {
								String dd = d.getPerkuliahan() == null
										? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
										: d.getPerkuliahan().getMatakuliah().getNama();
								daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

								jmlSKSSimulasi += sks.doubleValue();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + ((int) jmlSKSSimulasi) + " SKS, sbb : " + daftarMk);
					nominalModifikasi = (jmlSKSSimulasi * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null
						&& pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_MK_UTS)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					List<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, tahapan, null);
					Double jumlahMatakuliah = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {
							if (detailperkuliahan.getPerkuliahan() == null) {
								continue;
							}

							Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
									? detailperkuliahan.getMatakuliahKonversi()
									: detailperkuliahan.getPerkuliahan().getMatakuliah();
							if (matakuliah.getTerdapatUts()
									&& (detailperkuliahan.getPerkuliahan() == null
											|| detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() == null)
									&& (detailperkuliahan.getPerkuliahan() == null
											|| !detailperkuliahan.getPerkuliahan().getMerupakanRemedial())) {
								jumlahMatakuliah++;
								daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + (jumlahMatakuliah.intValue()) + " matakuliah, yaitu: " + daftarMk);
					nominalModifikasi = (jumlahMatakuliah * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null
						&& pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_MK_UAS)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					List<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, tahapan, null);
					Double jumlahMatakuliah = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {
							if (detailperkuliahan.getPerkuliahan() == null) {
								continue;
							}
							Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
									? detailperkuliahan.getMatakuliahKonversi()
									: detailperkuliahan.getPerkuliahan().getMatakuliah();
							if (matakuliah.getTerdapatUas()
									&& (detailperkuliahan.getPerkuliahan() == null
											|| detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() == null)
									&& (detailperkuliahan.getPerkuliahan() == null
											|| !detailperkuliahan.getPerkuliahan().getMerupakanRemedial())) {
								jumlahMatakuliah++;
								daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + (jumlahMatakuliah.intValue()) + " matakuliah, yaitu: " + daftarMk);
					nominalModifikasi = (jumlahMatakuliah * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_MK_UTS_SP)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					List<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, tahapan,
							Perkuliahan.SEMESTER_PENDEK);
					Double jumlahMatakuliah = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {
							if (detailperkuliahan.getPerkuliahan() == null) {
								continue;
							}
							Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
									? detailperkuliahan.getMatakuliahKonversi()
									: detailperkuliahan.getPerkuliahan().getMatakuliah();
							if (matakuliah.getTerdapatUts() && detailperkuliahan.getPerkuliahan() != null
									&& !detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
								jumlahMatakuliah++;
								daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + jumlahMatakuliah.intValue() + " matakuliah, yaitu: " + daftarMk);
					nominalModifikasi = (jumlahMatakuliah * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_MK_UAS_SP)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					List<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, tahapan,
							Perkuliahan.SEMESTER_PENDEK);
					Double jumlahMatakuliah = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {
							if (detailperkuliahan.getPerkuliahan() == null) {
								continue;
							}
							Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
									? detailperkuliahan.getMatakuliahKonversi()
									: detailperkuliahan.getPerkuliahan().getMatakuliah();
							if (matakuliah.getTerdapatUas() && detailperkuliahan.getPerkuliahan() != null
									&& !detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
								jumlahMatakuliah++;
								daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + jumlahMatakuliah.intValue() + " matakuliah, yaitu: " + daftarMk);
					nominalModifikasi = (jumlahMatakuliah * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_MATAKULIAH_REMEDIAL)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					List<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, tahapan, null, true);
					Double jumlahMatakuliah = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {
							if (detailperkuliahan.getPerkuliahan() == null) {
								continue;
							}
							Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
									? detailperkuliahan.getMatakuliahKonversi()
									: detailperkuliahan.getPerkuliahan().getMatakuliah();
							if (matakuliah != null && detailperkuliahan.getPerkuliahan() != null
									&& detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
								jumlahMatakuliah++;
								daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
							}
						}
					}
					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + jumlahMatakuliah.intValue() + " remedial, yaitu: " + daftarMk);
					nominalModifikasi = (jumlahMatakuliah * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_MATAKULIAH_REMEDIAL_1_SKS)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					List<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, tahapan, null, true);
					Double jumlahMatakuliah = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {
							if (detailperkuliahan.getPerkuliahan() == null) {
								continue;
							}
							Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
									? detailperkuliahan.getMatakuliahKonversi()
									: detailperkuliahan.getPerkuliahan().getMatakuliah();
							if (matakuliah != null && matakuliah.getSks().equals(1)
									&& detailperkuliahan.getPerkuliahan() != null
									&& detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
								jumlahMatakuliah++;
								daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + jumlahMatakuliah.intValue() + " remedial 1 SKS, yaitu: " + daftarMk);
					nominalModifikasi = (jumlahMatakuliah * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_MATAKULIAH_REMEDIAL_2_SKS)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					List<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, tahapan, null, true);
					Double jumlahMatakuliah = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {
							if (detailperkuliahan.getPerkuliahan() == null) {
								continue;
							}
							Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
									? detailperkuliahan.getMatakuliahKonversi()
									: detailperkuliahan.getPerkuliahan().getMatakuliah();
							if (matakuliah != null && matakuliah.getSks().equals(2)
									&& detailperkuliahan.getPerkuliahan() != null
									&& detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
								jumlahMatakuliah++;
								daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + jumlahMatakuliah.intValue() + " remedial 2 SKS, yaitu: " + daftarMk);
					nominalModifikasi = (jumlahMatakuliah * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_MATAKULIAH_REMEDIAL_3_SKS)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					List<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, tahapan, null, true);
					Double jumlahMatakuliah = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {
							if (detailperkuliahan.getPerkuliahan() == null) {
								continue;
							}
							Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
									? detailperkuliahan.getMatakuliahKonversi()
									: detailperkuliahan.getPerkuliahan().getMatakuliah();
							if (matakuliah != null && matakuliah.getSks().equals(3)
									&& detailperkuliahan.getPerkuliahan() != null
									&& detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
								jumlahMatakuliah++;
								daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + jumlahMatakuliah.intValue() + " remedial 3 SKS, yaitu: " + daftarMk);
					nominalModifikasi = (jumlahMatakuliah * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_MATAKULIAH_REMEDIAL_4_SKS)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					List<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, tahapan, null, true);
					Double jumlahMatakuliah = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {
							if (detailperkuliahan.getPerkuliahan() == null) {
								continue;
							}
							Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
									? detailperkuliahan.getMatakuliahKonversi()
									: detailperkuliahan.getPerkuliahan().getMatakuliah();
							if (matakuliah != null && matakuliah.getSks().equals(4)
									&& detailperkuliahan.getPerkuliahan() != null
									&& detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
								jumlahMatakuliah++;
								daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + jumlahMatakuliah.intValue() + " remedial 4 SKS, yaitu: " + daftarMk);
					nominalModifikasi = (jumlahMatakuliah * harga);

				}

				////////////////////////////////////////////////

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null
						&& pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_SKS_UTS)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					List<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, tahapan, null);
					Double jumlahMatakuliah = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {
							if (detailperkuliahan.getPerkuliahan() == null) {
								continue;
							}
							Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
									? detailperkuliahan.getMatakuliahKonversi()
									: detailperkuliahan.getPerkuliahan().getMatakuliah();
							if (matakuliah.getTerdapatUts()
									&& (detailperkuliahan.getPerkuliahan() == null
											|| detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() == null)
									&& (detailperkuliahan.getPerkuliahan() == null
											|| !detailperkuliahan.getPerkuliahan().getMerupakanRemedial())) {
								jumlahMatakuliah += matakuliah.getSks();
								daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + jumlahMatakuliah.intValue() + " SKS, sbb : " + daftarMk);
					nominalModifikasi = (jumlahMatakuliah * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null
						&& pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_SKS_UAS)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					List<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, tahapan, null);
					Double jumlahMatakuliah = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {
							if (detailperkuliahan.getPerkuliahan() == null) {
								continue;
							}
							Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
									? detailperkuliahan.getMatakuliahKonversi()
									: detailperkuliahan.getPerkuliahan().getMatakuliah();
							if (matakuliah.getTerdapatUas()
									&& (detailperkuliahan.getPerkuliahan() == null
											|| detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() == null)
									&& (detailperkuliahan.getPerkuliahan() == null
											|| !detailperkuliahan.getPerkuliahan().getMerupakanRemedial())) {
								jumlahMatakuliah += matakuliah.getSks();
								daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + jumlahMatakuliah.intValue() + " SKS, sbb : " + daftarMk);
					nominalModifikasi = (jumlahMatakuliah * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_SKS_UTS_SP)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					List<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, tahapan,
							Perkuliahan.SEMESTER_PENDEK);
					Double jumlahMatakuliah = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {
							if (detailperkuliahan.getPerkuliahan() == null) {
								continue;
							}
							Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
									? detailperkuliahan.getMatakuliahKonversi()
									: detailperkuliahan.getPerkuliahan().getMatakuliah();
							if (matakuliah.getTerdapatUts() && detailperkuliahan.getPerkuliahan() != null
									&& detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() != null
									&& !detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
								jumlahMatakuliah += matakuliah.getSks();
								daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + jumlahMatakuliah.intValue() + " SKS, sbb : " + daftarMk);
					nominalModifikasi = (jumlahMatakuliah * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_SKS_UAS_SP)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, tahapan,
							Perkuliahan.SEMESTER_PENDEK);
					Double jumlahMatakuliah = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {
							if (detailperkuliahan.getPerkuliahan() == null) {
								continue;
							}
							Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
									? detailperkuliahan.getMatakuliahKonversi()
									: detailperkuliahan.getPerkuliahan().getMatakuliah();
							if (matakuliah.getTerdapatUas() && detailperkuliahan.getPerkuliahan() != null
									&& detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() != null
									&& !detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
								jumlahMatakuliah += matakuliah.getSks();
								daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + jumlahMatakuliah.intValue() + " SKS, sbb : " + daftarMk);
					nominalModifikasi = (jumlahMatakuliah * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_SKS_UTS_REMEDIAL)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, null, null, true,
							false, null);
					Double jumlahMatakuliah = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {
							if (detailperkuliahan.getPerkuliahan() == null) {
								continue;
							}
							Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
									? detailperkuliahan.getMatakuliahKonversi()
									: detailperkuliahan.getPerkuliahan().getMatakuliah();
							if (matakuliah.getTerdapatUts() && detailperkuliahan.getPerkuliahan() != null
									&& detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
								jumlahMatakuliah += matakuliah.getSks();
								daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + jumlahMatakuliah.intValue() + " SKS, sbb : " + daftarMk);
					nominalModifikasi = (jumlahMatakuliah * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_SKS_UAS_REMDIAL)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, null, null, true,
							false, null);
					Double jumlahMatakuliah = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {
							if (detailperkuliahan.getPerkuliahan() == null) {
								continue;
							}
							Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
									? detailperkuliahan.getMatakuliahKonversi()
									: detailperkuliahan.getPerkuliahan().getMatakuliah();
							if (matakuliah.getTerdapatUas() && detailperkuliahan.getPerkuliahan() != null
									&& detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
								jumlahMatakuliah += matakuliah.getSks();
								daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + jumlahMatakuliah.intValue() + " SKS, sbb : " + daftarMk);
					nominalModifikasi = (jumlahMatakuliah * harga);

				}

				///////////////////////////////////////////////

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_MK_PRAKTEK)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, tahapan, null);

					String daftarMk = "";
					int jumlahMatakuliah = 0;
					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {
							if (detailperkuliahan.getPerkuliahan() == null) {
								continue;
							}
							Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
									? detailperkuliahan.getMatakuliahKonversi()
									: detailperkuliahan.getPerkuliahan().getMatakuliah();
							if (matakuliah.getMerupakanMkPraktek()) {
								jumlahMatakuliah++;
								daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
							}
						}
					}

					double jml = (double) jumlahMatakuliah;

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + (jumlahMatakuliah) + " matakuliah, sbb : " + daftarMk);
					nominalModifikasi = (jml * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_MK_DISKUSI_TEORI)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, tahapan, null);

					String daftarMk = "";
					int jumlahMatakuliah = 0;
					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {
							if (detailperkuliahan.getPerkuliahan() == null) {
								continue;
							}
							Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
									? detailperkuliahan.getMatakuliahKonversi()
									: detailperkuliahan.getPerkuliahan().getMatakuliah();
							if (matakuliah.getMerupakanMkTeori()) {
								jumlahMatakuliah++;
								daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + (jumlahMatakuliah) + " matakuliah, sbb : " + daftarMk);
					nominalModifikasi = (jumlahMatakuliah * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null
						&& pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_MK)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, tahapan, null);
					Double jumlahMatakuliah = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {
							if (detailperkuliahan.getPerkuliahan() == null) {
								continue;
							}
							Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
									? detailperkuliahan.getMatakuliahKonversi()
									: detailperkuliahan.getPerkuliahan().getMatakuliah();
							if ((detailperkuliahan.getPerkuliahan() == null
									|| detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() == null)
									&& (detailperkuliahan.getPerkuliahan() == null
											|| !detailperkuliahan.getPerkuliahan().getMerupakanRemedial())) {
								jumlahMatakuliah++;
								daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + jumlahMatakuliah.intValue() + " matakuliah, sbb : " + daftarMk);
					nominalModifikasi = (jumlahMatakuliah * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null
						&& pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_MK_SP)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, tahapan,
							Perkuliahan.SEMESTER_PENDEK);
					Double jumlahMatakuliah = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {
							if (detailperkuliahan.getPerkuliahan() == null) {
								continue;
							}
							Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
									? detailperkuliahan.getMatakuliahKonversi()
									: detailperkuliahan.getPerkuliahan().getMatakuliah();
							if (detailperkuliahan.getPerkuliahan() != null
									&& detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() != null
									&& !detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
								jumlahMatakuliah++;
								daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + jumlahMatakuliah.intValue() + " matakuliah, sbb : " + daftarMk);
					nominalModifikasi = (jumlahMatakuliah * harga);

				}
			}
		}

		return nominalModifikasi;
	
	}
}
