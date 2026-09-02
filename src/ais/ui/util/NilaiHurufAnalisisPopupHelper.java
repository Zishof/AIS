package ais.ui.util;

import java.util.List;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Fakultas;
import ais.database.model.FormatNilai;
import ais.database.model.Jurusan;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.NilaiHuruf;
import ais.database.model.Perkuliahan;

public class NilaiHurufAnalisisPopupHelper {

	public static Label buatLabel(String value, Detailperkuliahan detailperkuliahan) {
		Label label = new Label(value == null ? "" : value);
		pasangLink(label, detailperkuliahan, null, null);
		return label;
	}

	public static void pasangLink(final Label label, final Detailperkuliahan detailperkuliahan) {
		pasangLink(label, detailperkuliahan, null, null);
	}

	public static void pasangLink(final Label label, final Detailperkuliahan detailperkuliahan,
			final Perkuliahan perkuliahanSumber, final List<FormatNilai> formatNilaisSumber) {
		if (label == null || detailperkuliahan == null) {
			return;
		}
		String sclass = label.getSclass() == null ? "" : label.getSclass().trim();
		if (!(" " + sclass + " ").contains(" ais-clickable-analysis-value ")) {
			label.setSclass((sclass.length() == 0 ? "" : sclass + " ") + "ais-clickable-analysis-value");
		}
		String style = label.getStyle() == null ? "" : label.getStyle();
		if (style.indexOf("cursor:pointer") < 0) {
			label.setStyle(style + (style.trim().length() == 0 || style.trim().endsWith(";") ? "" : ";")
					+ "cursor:pointer;");
		}
		label.setTooltiptext("Klik untuk membuka detail analisis nilai huruf");
		label.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				tampilkan(detailperkuliahan, perkuliahanSumber, formatNilaisSumber);
			}
		});
	}

	public static void tampilkan(Detailperkuliahan detailperkuliahan) throws Exception {
		tampilkan(detailperkuliahan, null, null);
	}

	public static void tampilkan(Detailperkuliahan detailperkuliahan, Perkuliahan perkuliahanSumber,
			List<FormatNilai> formatNilaisSumber) throws Exception {
		if (detailperkuliahan == null) {
			return;
		}
		Perkuliahan perkuliahan = perkuliahanSumber == null ? detailperkuliahan.getPerkuliahan()
				: perkuliahanSumber;
		List<FormatNilai> formatNilais = formatNilaisSumber;
		if (formatNilais == null && perkuliahan != null) {
			formatNilais = Common.getFormatNilais(perkuliahan);
		}
		MyWindow window = new MyWindow("Analisis Nilai Huruf", "normal", true);
		window.setWidth(Common.isMobile() ? "100%" : "680px");
		window.setHeight(Common.isMobile() ? "90%" : "620px");
		window.setContentStyle("overflow:auto;background:#f8fafc;padding:0;");
		window.appendChild(new Html(buatHtmlAnalisisNilaiHuruf(detailperkuliahan, perkuliahan, formatNilais)));
		if (window.getPage() == null && org.zkoss.zk.ui.Executions.getCurrent() != null
				&& org.zkoss.zk.ui.Executions.getCurrent().getDesktop() != null
				&& org.zkoss.zk.ui.Executions.getCurrent().getDesktop().getFirstPage() != null) {
			window.setPage(org.zkoss.zk.ui.Executions.getCurrent().getDesktop().getFirstPage());
		}
		window.doModal();
	}

	private static String buatHtmlAnalisisNilaiHuruf(Detailperkuliahan detailperkuliahan, Perkuliahan perkuliahan,
			List<FormatNilai> formatNilais) {
		StringBuilder html = new StringBuilder();
		boolean tampilSementara = perkuliahan != null && perkuliahan.getSembunyikanNilaiJikaBelumDiverifikasi()
				&& Detailperkuliahan.NOT_VERIFIED.equals(detailperkuliahan.getVerify());
		double total = tampilSementara ? nilaiAman(detailperkuliahan.getTotalNilaiSementara())
				: nilaiAman(detailperkuliahan.getTotalNilai());
		String hurufKunci = detailperkuliahan.getNilaiHurufKunci();
		String huruf = tampilSementara ? detailperkuliahan.getNilaiHurufSementara() : detailperkuliahan.getNilaiHuruf();
		NilaiHuruf aturanHuruf = ambilAturanNilaiHuruf(detailperkuliahan, total);
		NilaiHuruf targetBerikut = ambilAturanNilaiHurufBerikut(detailperkuliahan, total);

		html.append("<div style='font-family:Arial,sans-serif;color:#172033;font-size:13px;line-height:1.45;'>");
		html.append("<div style='background:#0b63ce;color:white;padding:14px 18px;'>");
		html.append("<div style='font-size:18px;font-weight:bold;'>Analisis Nilai Huruf</div>");
		html.append("<div style='font-size:12px;opacity:.92;'>Rincian ini membaca komponen nilai, bobot, verifikasi, kehadiran, dan tabel Nilai Huruf yang sama dengan perhitungan sistem.</div>");
		html.append("</div><div style='padding:16px 18px;'>");

		Mahasiswa mahasiswa = detailperkuliahan.getMahasiswa();
		html.append("<div style='background:white;border:1px solid #dbe5f0;border-radius:8px;padding:12px;margin-bottom:12px;'>");
		html.append("<div style='font-weight:bold;font-size:15px;margin-bottom:6px;'>")
				.append(teksAmanHtml(mahasiswa == null ? "Mahasiswa" : mahasiswa.getNim() + " - " + mahasiswa.getNama()))
				.append("</div>");
		html.append("<div>Nilai akhir: <b>").append(Common.numberFormat.get().format(total)).append("</b></div>");
		html.append("<div>Nilai huruf: <b>").append(teksAmanHtml(huruf == null || huruf.trim().isEmpty() ? "-" : huruf))
				.append("</b></div>");
		if (apakahPerkuliahanTerkunci(detailperkuliahan) && hurufKunci != null && !hurufKunci.trim().isEmpty()
				&& huruf != null && !hurufKunci.trim().equalsIgnoreCase(huruf.trim())) {
			html.append("<div style='color:#a16207;'>Snapshot huruf saat dikunci: <b>")
					.append(teksAmanHtml(hurufKunci)).append("</b>; tampilan dikoreksi mengikuti total menjadi <b>")
					.append(teksAmanHtml(huruf)).append("</b>.</div>");
		}
		if (aturanHuruf != null) {
			html.append("<div>Rentang huruf ini: <b>")
					.append(Common.numberFormat.get().format(aturanHuruf.getMulai())).append(" s.d ")
					.append(Common.numberFormat.get().format(aturanHuruf.getSampai())).append("</b>");
			if (aturanHuruf.getNilaiDiIPK() != null) {
				html.append(", IP: <b>").append(Common.numberFormat.get().format(aturanHuruf.getNilaiDiIPK()))
						.append("</b>");
			}
			html.append("</div>");
		}
		if (tampilSementara) {
			html.append("<div style='margin-top:6px;color:#a16207;'>Nilai yang dianalisis adalah nilai sementara karena nilai belum diverifikasi dan setting sembunyikan nilai belum verifikasi sedang aktif.</div>");
		}
		html.append("</div>");

		html.append(buatHtmlAnalisisPintar(detailperkuliahan, perkuliahan, formatNilais, total, huruf, hurufKunci,
				aturanHuruf, targetBerikut, tampilSementara));
		html.append(buatHtmlKomponenNilai(detailperkuliahan, perkuliahan, formatNilais, tampilSementara));

		String alasanNol = "";
		try {
			if (total < 0.01 && formatNilais != null) {
				alasanNol = detailperkuliahan.alasanNilaiJadiNol(true, formatNilais);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		html.append("<div style='background:white;border:1px solid #dbe5f0;border-radius:8px;padding:12px;margin-top:12px;'>");
		html.append("<div style='font-weight:bold;margin-bottom:6px;'>Kesimpulan</div>");
		if (alasanNol != null && !alasanNol.trim().isEmpty()) {
			html.append("<div style='color:#b91c1c;font-weight:bold;'>").append(teksAmanHtml(alasanNol)).append("</div>");
		} else if (aturanHuruf != null) {
			html.append("<div>Total <b>").append(Common.numberFormat.get().format(total)).append("</b> masuk rentang <b>")
					.append(teksAmanHtml(aturanHuruf.getNilaiHuruf())).append("</b>, sehingga sistem menampilkan nilai huruf tersebut.</div>");
		} else {
			html.append("<div>Nilai huruf belum ditemukan dari tabel konfigurasi Nilai Huruf. Periksa setting rentang nilai huruf untuk prodi/fakultas/tahun akademik ini.</div>");
		}
		if (targetBerikut != null && targetBerikut.getMulai() != null && targetBerikut.getNilaiHuruf() != null) {
			double kurang = targetBerikut.getMulai().doubleValue() - total;
			if (kurang > 0.0) {
				html.append("<div style='margin-top:6px;'>Untuk mencapai <b>")
						.append(teksAmanHtml(targetBerikut.getNilaiHuruf())).append("</b>, kurang sekitar <b>")
						.append(Common.numberFormat.get().format(kurang)).append("</b> poin dari batas bawah ")
						.append(Common.numberFormat.get().format(targetBerikut.getMulai())).append(".</div>");
			}
		}
		html.append("</div></div></div>");
		return html.toString();
	}

	private static String buatHtmlAnalisisPintar(Detailperkuliahan detailperkuliahan, Perkuliahan perkuliahan,
			List<FormatNilai> formatNilais, double total, String hurufTampil, String hurufKunci,
			NilaiHuruf aturanHuruf, NilaiHuruf targetBerikut, boolean tampilSementara) {
		StringBuilder html = new StringBuilder();
		String hurufSeharusnya = aturanHuruf == null ? "" : aturanHuruf.getNilaiHuruf();
		double persenHadir = 0.0;
		try {
			persenHadir = detailperkuliahan.hitungPersenKehadiran();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		double totalBobot = hitungTotalBobotEfektif(detailperkuliahan, perkuliahan, formatNilais, tampilSementara);
		FormatNilai bobotTerbesar = ambilFormatNilaiBobotTerbesar(detailperkuliahan, perkuliahan, formatNilais,
				tampilSementara);

		html.append("<div style='background:#f0f7ff;border:1px solid #bfdbfe;border-radius:8px;padding:12px;margin-bottom:12px;'>");
		html.append("<div style='font-weight:bold;margin-bottom:8px;color:#0b3b78;'>Analisis Pintar</div>");
		html.append("<ol style='margin:0;padding-left:20px;'>");

		boolean terkunci = apakahPerkuliahanTerkunci(detailperkuliahan);
		if (aturanHuruf == null) {
			html.append("<li><b>Rentang nilai huruf belum cocok.</b> Sistem tidak menemukan konfigurasi Nilai Huruf untuk total ")
					.append(Common.numberFormat.get().format(total))
					.append(". Ini biasanya karena setting Nilai Huruf prodi/fakultas/tahun akademik/jenis nilai belum lengkap.</li>");
		} else if (terkunci && hurufKunci != null && !hurufKunci.trim().isEmpty()
				&& !hurufKunci.trim().equalsIgnoreCase(hurufSeharusnya)) {
			html.append("<li><b>Nilai terkunci, tetapi snapshot huruf kunci sudah tidak sesuai.</b> Saat dikunci tersimpan <b>")
					.append(teksAmanHtml(hurufKunci)).append("</b>, sementara total ")
					.append(Common.numberFormat.get().format(total)).append(" sekarang masuk rentang <b>")
					.append(teksAmanHtml(hurufSeharusnya))
					.append("</b>. Sistem mengutamakan huruf sesuai total/rentang, bukan snapshot huruf lama.</li>");
		} else if (hurufTampil == null || !hurufTampil.trim().equalsIgnoreCase(hurufSeharusnya)) {
			html.append("<li><b>Ada indikasi huruf tersimpan tidak sinkron.</b> Berdasarkan total ")
					.append(Common.numberFormat.get().format(total)).append(", sistem membaca rentang <b>")
					.append(teksAmanHtml(hurufSeharusnya)).append("</b>, tetapi yang tampil <b>")
					.append(teksAmanHtml(hurufTampil)).append("</b>. Klik Hitung Ulang/Singkronkan Nilai agar nilai huruf tersimpan mengikuti rentang terbaru.</li>");
		} else {
			html.append("<li><b>Huruf sudah konsisten.</b> Total ")
					.append(Common.numberFormat.get().format(total)).append(" berada pada rentang <b>")
					.append(teksAmanHtml(hurufSeharusnya)).append("</b> yaitu ")
					.append(Common.numberFormat.get().format(aturanHuruf.getMulai())).append(" s.d ")
					.append(Common.numberFormat.get().format(aturanHuruf.getSampai())).append(".</li>");
		}

		if (tampilSementara) {
			html.append("<li><b>Nilai belum diverifikasi.</b> Analisis memakai nilai sementara, sehingga hasil akhir dapat berubah setelah verifikasi selesai.</li>");
		}
		if (perkuliahan != null && perkuliahan.getPersenKehadiranDinilai0() > 0.1) {
			html.append("<li>Kehadiran mahasiswa <b>").append(Common.numberFormat.get().format(persenHadir))
					.append("%</b>; batas minimal agar nilai tidak menjadi 0 adalah <b>")
					.append(Common.numberFormat.get().format(perkuliahan.getPersenKehadiranDinilai0()))
					.append("%</b>.</li>");
		}
		if (perkuliahan != null && perkuliahan.getJikaAdaNilai0TidakMenghitungNilaiAkhir()) {
			html.append("<li>Aturan <b>jika ada nilai 0 maka nilai akhir tidak dihitung</b> sedang aktif. Komponen bernilai 0 wajib diperiksa.</li>");
		} else if (perkuliahan != null && perkuliahan.getNilai_0_tidak_masuk_dalam_perhitungan_nilai_akhir()) {
			html.append("<li>Aturan <b>nilai 0 tidak masuk pembagi</b> sedang aktif. Bobot komponen bernilai 0 tidak ikut membentuk rata-rata akhir.</li>");
		}
		if (totalBobot < 99.9 || totalBobot > 100.1) {
			html.append("<li><b>Total bobot efektif ").append(Common.numberFormat.get().format(totalBobot))
					.append("%</b>. Jika tidak sesuai harapan, cek bobot Format Nilai karena perhitungan memakai bobot efektif ini.</li>");
		}
		if (targetBerikut != null && targetBerikut.getMulai() != null && bobotTerbesar != null && totalBobot > 0.0) {
			double kurangTotal = targetBerikut.getMulai().doubleValue() - total;
			double bobot = nilaiAman(bobotTerbesar.getPersen());
			double perluNaikKomponen = bobot <= 0.0 ? 0.0 : kurangTotal / (bobot / totalBobot);
			if (kurangTotal > 0.0 && perluNaikKomponen > 0.0) {
				html.append("<li>Jalur tercepat untuk naik ke <b>").append(teksAmanHtml(targetBerikut.getNilaiHuruf()))
						.append("</b>: komponen berbobot terbesar adalah <b>")
						.append(teksAmanHtml(bobotTerbesar.getNama())).append("</b> (")
						.append(Common.numberFormat.get().format(bobot)).append("%). Secara kasar perlu tambahan sekitar <b>")
						.append(Common.numberFormat.get().format(perluNaikKomponen))
						.append("</b> poin pada komponen itu, selama nilai maksimal komponen masih memungkinkan.</li>");
			}
		}
		html.append("</ol></div>");
		return html.toString();
	}

	private static String buatHtmlKomponenNilai(Detailperkuliahan detailperkuliahan, Perkuliahan perkuliahan,
			List<FormatNilai> formatNilais, boolean tampilSementara) {
		StringBuilder html = new StringBuilder();
		double totalBobot = 0.0;
		html.append("<div style='background:white;border:1px solid #dbe5f0;border-radius:8px;padding:12px;'>");
		html.append("<div style='font-weight:bold;margin-bottom:8px;'>Komponen Pembentuk Nilai</div>");
		html.append("<table style='width:100%;border-collapse:collapse;font-size:12px;'>");
		html.append("<tr style='background:#eef4fb;'><th style='text-align:left;padding:6px;border:1px solid #dbe5f0;'>Komponen</th><th style='text-align:right;padding:6px;border:1px solid #dbe5f0;'>Nilai</th><th style='text-align:right;padding:6px;border:1px solid #dbe5f0;'>Bobot</th><th style='text-align:right;padding:6px;border:1px solid #dbe5f0;'>Kontribusi</th><th style='text-align:center;padding:6px;border:1px solid #dbe5f0;'>Ver.</th></tr>");
		if (formatNilais != null) {
			for (FormatNilai formatNilai : formatNilais) {
				if (formatNilai == null || formatNilai.getPersen() == null || formatNilai.getPersen().doubleValue() < 0.01) {
					continue;
				}
				double nilai = tampilSementara ? nilaiAman(detailperkuliahan.retreiveDetailNilaiBelumVerify(formatNilai))
						: nilaiAman(detailperkuliahan.retreiveDetailNilai(formatNilai));
				if (!(perkuliahan != null && perkuliahan.getNilai_0_tidak_masuk_dalam_perhitungan_nilai_akhir()
						&& nilai < 0.01)) {
					totalBobot += nilaiAman(formatNilai.getPersen());
				}
			}
			for (FormatNilai formatNilai : formatNilais) {
				if (formatNilai == null || formatNilai.getPersen() == null || formatNilai.getPersen().doubleValue() < 0.01) {
					continue;
				}
				double nilai = tampilSementara ? nilaiAman(detailperkuliahan.retreiveDetailNilaiBelumVerify(formatNilai))
						: nilaiAman(detailperkuliahan.retreiveDetailNilai(formatNilai));
				double bobot = nilaiAman(formatNilai.getPersen());
				boolean bobotMasuk = !(perkuliahan != null
						&& perkuliahan.getNilai_0_tidak_masuk_dalam_perhitungan_nilai_akhir() && nilai < 0.01);
				double kontribusi = totalBobot > 0.0 && bobotMasuk ? nilai * (bobot / totalBobot) : 0.0;
				html.append("<tr><td style='padding:6px;border:1px solid #dbe5f0;'>")
						.append(teksAmanHtml(formatNilai.getNama())).append("</td><td style='text-align:right;padding:6px;border:1px solid #dbe5f0;'>")
						.append(Common.numberFormat.get().format(nilai)).append("</td><td style='text-align:right;padding:6px;border:1px solid #dbe5f0;'>")
						.append(Common.numberFormat.get().format(bobot)).append("%</td><td style='text-align:right;padding:6px;border:1px solid #dbe5f0;'>")
						.append(Common.numberFormat.get().format(kontribusi)).append("</td><td style='text-align:center;padding:6px;border:1px solid #dbe5f0;'>")
						.append(detailperkuliahan.retreiveDetailVerifikasiNilai(formatNilai) ? "Ya" : "Belum")
						.append("</td></tr>");
			}
		}
		html.append("</table>");
		if (formatNilais == null || formatNilais.isEmpty()) {
			html.append("<div style='color:#64748b;margin-top:8px;'>Belum ada komponen format nilai aktif yang dapat dianalisis.</div>");
		} else {
			html.append("<div style='margin-top:8px;color:#334155;'>Total bobot pembagi: <b>")
					.append(Common.numberFormat.get().format(totalBobot)).append("%</b>.</div>");
		}
		html.append("</div>");
		return html.toString();
	}

	private static boolean apakahPerkuliahanTerkunci(Detailperkuliahan detailperkuliahan) {
		try {
			Perkuliahan kuliah = detailperkuliahan == null ? null : detailperkuliahan.getPerkuliahan();
			return kuliah != null && kuliah.getDikunci() != null;
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return false;
		}
	}

	private static NilaiHuruf ambilAturanNilaiHuruf(Detailperkuliahan detailperkuliahan, double total) {
		try {
			Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null ? detailperkuliahan.getMatakuliahKonversi()
					: detailperkuliahan.getPerkuliahan().getMatakuliah();
			Mahasiswa mahasiswa = detailperkuliahan.getMahasiswa();
			Jurusan jurusan = mahasiswa == null ? null : mahasiswa.getJurusan();
			Fakultas fakultas = jurusan == null ? null : jurusan.getFakultas();
			return Common.getNilaiHuruf(Double.valueOf(total), mahasiswa == null ? null : mahasiswa.getTahunangkatan(),
					jurusan, fakultas, detailperkuliahan.getTahunAkademik(),
					detailperkuliahan.getPerkuliahan() == null ? null : detailperkuliahan.getPerkuliahan().getGanjilGenap(),
					matakuliah == null ? "" : matakuliah.getKode(),
					matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return null;
		}
	}

	private static NilaiHuruf ambilAturanNilaiHurufBerikut(Detailperkuliahan detailperkuliahan, double total) {
		NilaiHuruf kandidat = null;
		try {
			for (NilaiHuruf nilaiHuruf : ConstantValues.nilaiHurufs) {
				if (nilaiHuruf == null || nilaiHuruf.getMulai() == null || nilaiHuruf.getNilaiHuruf() == null
						|| nilaiHuruf.getMulai().doubleValue() <= total) {
					continue;
				}
				NilaiHuruf cocok = ambilAturanNilaiHuruf(detailperkuliahan, nilaiHuruf.getMulai().doubleValue());
				if (cocok == null || cocok.getNilaiHuruf() == null
						|| !cocok.getNilaiHuruf().equalsIgnoreCase(nilaiHuruf.getNilaiHuruf())) {
					continue;
				}
				if (kandidat == null || nilaiHuruf.getMulai().doubleValue() < kandidat.getMulai().doubleValue()) {
					kandidat = nilaiHuruf;
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return kandidat;
	}

	private static double hitungTotalBobotEfektif(Detailperkuliahan detailperkuliahan, Perkuliahan perkuliahan,
			List<FormatNilai> formatNilais, boolean tampilSementara) {
		double totalBobot = 0.0;
		if (formatNilais == null) {
			return totalBobot;
		}
		for (FormatNilai formatNilai : formatNilais) {
			if (formatNilai == null || formatNilai.getPersen() == null || formatNilai.getPersen().doubleValue() < 0.01) {
				continue;
			}
			double nilai = tampilSementara ? nilaiAman(detailperkuliahan.retreiveDetailNilaiBelumVerify(formatNilai))
					: nilaiAman(detailperkuliahan.retreiveDetailNilai(formatNilai));
			if (perkuliahan != null && perkuliahan.getNilai_0_tidak_masuk_dalam_perhitungan_nilai_akhir()
					&& nilai < 0.01) {
				continue;
			}
			totalBobot += nilaiAman(formatNilai.getPersen());
		}
		return totalBobot;
	}

	private static FormatNilai ambilFormatNilaiBobotTerbesar(Detailperkuliahan detailperkuliahan,
			Perkuliahan perkuliahan, List<FormatNilai> formatNilais, boolean tampilSementara) {
		FormatNilai kandidat = null;
		double bobotTerbesar = -1.0;
		if (formatNilais == null) {
			return null;
		}
		for (FormatNilai formatNilai : formatNilais) {
			if (formatNilai == null || formatNilai.getPersen() == null || formatNilai.getPersen().doubleValue() < 0.01) {
				continue;
			}
			double nilai = tampilSementara ? nilaiAman(detailperkuliahan.retreiveDetailNilaiBelumVerify(formatNilai))
					: nilaiAman(detailperkuliahan.retreiveDetailNilai(formatNilai));
			if (perkuliahan != null && perkuliahan.getNilai_0_tidak_masuk_dalam_perhitungan_nilai_akhir()
					&& nilai < 0.01) {
				continue;
			}
			if (formatNilai.getPersen().doubleValue() > bobotTerbesar) {
				bobotTerbesar = formatNilai.getPersen().doubleValue();
				kandidat = formatNilai;
			}
		}
		return kandidat;
	}

	private static double nilaiAman(Double nilai) {
		if (nilai == null || nilai.isNaN() || nilai.isInfinite()) {
			return 0.0;
		}
		return nilai.doubleValue();
	}

	private static String teksAmanHtml(String teks) {
		if (teks == null) {
			return "";
		}
		return teks.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}
}
