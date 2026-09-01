package ais.action.master.helper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Box;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.database.model.CicilanPembayaran;
import ais.database.model.DetailBiaya;
import ais.database.model.JenisKegiatan;
import ais.database.model.Kegiatan;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.SettingBiaya;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyWindow;

/**
 * Kumpulan method statis (utility class, tidak bisa di-instantiate) yang dipakai BERSAMA oleh
 * layar-layar pembayaran daftar ulang mahasiswa/calon mahasiswa (mis. action Lama dan Baru untuk
 * pembayaran {@link Kegiatan} lewat {@link CicilanPembayaran}/{@link DetailBiaya}/
 * {@link PengaturanPembayaranBulanan}) agar logika perhitungan nominal, anti-pembayaran-ganda,
 * dan pemilihan {@link SettingBiaya} yang benar TIDAK digandakan berbeda-beda di tiap action.
 *
 * <p><b>Kelompok tanggung jawab:</b></p>
 * <ul>
 * <li><b>Baca nominal dari tampilan</b> — {@link #ambilNominalDariLabel}, {@link
 * #hitungNilaiCicilanBelumTersimpanDariGrid}, {@link #hitungJumlahYangAkanDibayarDariTampilan}:
 * dipakai karena UI kadang hanya menyimpan angka sebagai teks berformat Rupiah di {@link Label},
 * sehingga nilai "yang benar-benar akan dibayar" harus direkonstruksi dari kombinasi label footer
 * dan baris cicilan yang belum tersimpan di grid.</li>
 * <li><b>Anti-pembayaran-ganda</b> — {@link #getBayarCooldownMs} (baca konfigurasi
 * {@code cooldown_pembayaran_ganda_detik}, default 300 detik) dan {@link #buildBayarSignature}
 * (signature kegiatan+item+nominal) dipakai bersama agar klik ganda tombol Bayar dalam rentang
 * cooldown dengan signature yang SAMA bisa dideteksi dan diblok oleh pemanggil.</li>
 * <li><b>Penentuan item yang masih perlu dibayar</b> — {@link #updateDetailBiayaUntukDibayar}
 * membandingkan nilai tagihan per {@link DetailBiaya} dengan total yang sudah dicicil.</li>
 * <li><b>Pemilihan {@link SettingBiaya} yang tepat</b> — {@link #pilihSettingBiayaSesuai} (dengan
 * helper privat {@link #resolveSettingBiaya}) memilih SettingBiaya yang benar-benar cocok
 * (jenis kegiatan + semester + tahun akademik, atau minimal jenis kegiatan) dari koleksi tagihan
 * terpilih, dipakai tombol "Ubah Tagihan" agar tidak membuka setting biaya yang salah.</li>
 * <li><b>UI bersama</b> — {@link #pasangRingkasanBayar} (kartu ringkasan Dibayar/Total Bayar
 * dengan terbilang) dan {@link #loadIframeToTabpanel} (muat sekali saja tab lazy-load).</li>
 * </ul>
 * <p>Semua method murni membaca/menghitung dari state ZK atau argumen yang diberikan; tidak ada
 * method di kelas ini yang membuka transaksi Hibernate sendiri (mutasi entitas didelegasikan ke
 * pemanggil), kecuali baca konfigurasi ({@link #getBayarCooldownMs}) yang menyentuh DB via
 * {@link Common#getKonfigurasi}.</p>
 */
public final class DaftarUlangPembayaranHelper {

	private DaftarUlangPembayaranHelper() {
	}

	/**
	 * Parse nominal Rupiah dari teks sebuah {@link Label} (mis. label footer "Dibayar"/"Total").
	 * Coba dulu dengan {@link Common#numberFormat} (format lokal standar aplikasi); bila gagal,
	 * jatuh ke fallback manual yang membuang prefix {@code "Rp"}, spasi, titik ribuan, lalu
	 * mengganti koma desimal dengan titik sebelum {@link Double#parseDouble}.
	 *
	 * @param label label sumber teks nominal; boleh {@code null} atau kosong.
	 * @return nominal hasil parse, atau {@code 0.0} bila label kosong/tidak bisa di-parse sama sekali.
	 */
	public static double ambilNominalDariLabel(Label label) {
		if (label == null || label.getValue() == null || label.getValue().trim().isEmpty()) {
			return 0.0;
		}
		try {
			return Common.numberFormat.get().parse(label.getValue().trim()).doubleValue();
		} catch (Exception e) {
			try {
				String nilai = label.getValue().replace("Rp", "").replace(" ", "").replace(".", "").replace(",", ".");
				return Double.parseDouble(nilai);
			} catch (Exception ex) {
				return 0.0;
			}
		}
	}

	/**
	 * Jumlahkan nominal cicilan yang SUDAH diinput di grid tapi BELUM tersimpan sebagai
	 * {@link CicilanPembayaran} (baris tanpa atribut {@code cicilanPembayaran}, atau atributnya
	 * ada tapi {@code getId() == null}) — dipakai untuk mengetahui berapa yang akan dibayar dari
	 * baris-baris baru yang sedang diedit user sebelum tombol Bayar ditekan. Baris yang tidak
	 * visible ({@code row.isVisible() == false}) dilewati.
	 *
	 * @param gridCicilan grid cicilan; baris diharapkan membawa attribute {@code cicilanPembayaran}
	 *                    dan {@code jumlahCicilan} (sebuah {@link MyDoublebox}).
	 * @return total nominal baris baru yang belum tersimpan, atau {@code 0.0} bila grid kosong/error.
	 */
	public static double hitungNilaiCicilanBelumTersimpanDariGrid(Grid gridCicilan) {
		double jumlah = 0.0;
		try {
			if (gridCicilan == null || gridCicilan.getRows() == null) {
				return 0.0;
			}
			List rows = gridCicilan.getRows().getChildren();
			for (Object object : rows) {
				if (!(object instanceof Row)) {
					continue;
				}
				Row row = (Row) object;
				if (row == null || !row.isVisible()) {
					continue;
				}
				CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) row.getAttribute("cicilanPembayaran");
				if (cicilanPembayaran != null && cicilanPembayaran.getId() != null) {
					continue;
				}
				MyDoublebox jumlahCicilan = (MyDoublebox) row.getAttribute("jumlahCicilan");
				if (jumlahCicilan != null && jumlahCicilan.getValue() != null) {
					jumlah += jumlahCicilan.getValue().doubleValue();
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return jumlah;
	}

	/**
	 * Tentukan nominal yang AKAN dibayar sekarang, dengan urutan prioritas sumber:
	 * <ol>
	 * <li>label footer "Dibayar" bila user sudah mengisi angka di sana secara eksplisit
	 * ({@link #ambilNominalDariLabel} &gt; 0.01);</li>
	 * <li>bila kosong, jumlah cicilan baru yang belum tersimpan di grid
	 * ({@link #hitungNilaiCicilanBelumTersimpanDariGrid} &gt; 0.01);</li>
	 * <li>bila keduanya kosong (berarti user belum menyentuh apa pun), selisih antara total
	 * seluruh baris tagihan ({@code footerTotal}) dengan total yang SUDAH tersimpan sebagai
	 * {@link CicilanPembayaran} — dipakai sebagai default "bayar sisa semuanya".</li>
	 * </ol>
	 *
	 * @param footerDibayar       label footer nominal yang diinput manual user.
	 * @param footerTotal         label footer total seluruh tagihan.
	 * @param gridCicilan         grid cicilan berisi baris baru yang belum tersimpan.
	 * @param cicilanPembayarans  cicilan yang sudah tersimpan di DB untuk kegiatan ini.
	 * @return nominal final yang akan dibayar menurut prioritas di atas.
	 */
	public static double hitungJumlahYangAkanDibayarDariTampilan(Label footerDibayar, Label footerTotal,
			Grid gridCicilan, List<CicilanPembayaran> cicilanPembayarans) {
		double jumlahBaru = ambilNominalDariLabel(footerDibayar);
		if (Math.abs(jumlahBaru) > 0.01) {
			return jumlahBaru;
		}
		jumlahBaru = hitungNilaiCicilanBelumTersimpanDariGrid(gridCicilan);
		if (Math.abs(jumlahBaru) > 0.01) {
			return jumlahBaru;
		}
		double totalSemuaBaris = ambilNominalDariLabel(footerTotal);
		double totalTersimpan = 0.0;
		if (cicilanPembayarans != null) {
			for (CicilanPembayaran cicilanPembayaran : cicilanPembayarans) {
				if (cicilanPembayaran != null && cicilanPembayaran.getId() != null
						&& cicilanPembayaran.getNilai() != null) {
					totalTersimpan += cicilanPembayaran.getNilai().doubleValue();
				}
			}
		}
		return totalSemuaBaris - totalTersimpan;
	}

	/**
	 * Bangun kartu ringkasan pembayaran yang rapi (baris "Dibayar" &amp; "Total Bayar" + terbilang)
	 * ke {@code parent}, lalu kembalikan {@link Box} tempat caller memasang tombol bayar
	 * (mis. lewat {@code menuBayar(box)}). Label nilai dikelola caller (di-setValue saat hitung ulang);
	 * di sini hanya diberi gaya &amp; ditata supaya seragam di kedua action.
	 */
	public static Box pasangRingkasanBayar(Component parent, MyLabelBoldAja footerDibayar,
			MyLabelBoldAja footerDibayarTerbilang, MyLabelBoldAja footerTotal, MyLabelBoldAja footerTotalTerbilang) {

		Vbox ringkasanBayar = new Vbox();
		ringkasanBayar.setWidth("99%");
		ringkasanBayar.setStyle("background:#f8fafc;border:1px solid #e2e8f0;border-radius:14px;"
				+ "padding:12px 16px;margin-top:8px;box-sizing:border-box;");
		ringkasanBayar.setParent(parent);

		// Ringkasan dalam bentuk tabel (grid) agar rapi: Keterangan | Nilai | Terbilang
		Grid grid = new Grid();
		grid.setWidth("100%");
		grid.setSclass("ais-ringkasan-bayar-grid");

		Columns columns = new Columns();
		columns.setParent(grid);
		Column colKeterangan = new Column();
		colKeterangan.setWidth("110px");
		colKeterangan.setParent(columns);
		Column colNilai = new Column();
		colNilai.setWidth("160px");
		colNilai.setParent(columns);
		Column colTerbilang = new Column();
		colTerbilang.setParent(columns);

		Rows barisRingkasan = new Rows();
		barisRingkasan.setParent(grid);

		// Baris "Dibayar"
		Row barisDibayar = new Row();
		barisDibayar.setParent(barisRingkasan);
		MyLabelBoldAja kapDibayar = new MyLabelBoldAja("Dibayar");
		kapDibayar.setStyle("color:#64748b;font-size:12px;");
		kapDibayar.setParent(barisDibayar);
		footerDibayar.setStyle("color:#0f172a;font-size:15px;font-weight:800;");
		footerDibayar.setParent(barisDibayar);
		Hbox terbilangDibayar = new Hbox();
		terbilangDibayar.setAlign("center");
		terbilangDibayar.setSpacing("3px");
		terbilangDibayar.setParent(barisDibayar);
		MyLabelBoldAja kurungD1 = new MyLabelBoldAja("(");
		kurungD1.setStyle("color:#94a3b8;font-size:11px;");
		kurungD1.setParent(terbilangDibayar);
		footerDibayarTerbilang.setStyle("color:#94a3b8;font-size:11px;");
		footerDibayarTerbilang.setParent(terbilangDibayar);
		MyLabelBoldAja kurungD2 = new MyLabelBoldAja(")");
		kurungD2.setStyle("color:#94a3b8;font-size:11px;");
		kurungD2.setParent(terbilangDibayar);

		// Baris "Total Bayar"
		Row barisTotal = new Row();
		barisTotal.setParent(barisRingkasan);
		MyLabelBoldAja kapTotal = new MyLabelBoldAja("Total Bayar");
		kapTotal.setStyle("color:#64748b;font-size:12px;");
		kapTotal.setParent(barisTotal);
		footerTotal.setStyle("color:#16a34a;font-size:18px;font-weight:900;");
		footerTotal.setParent(barisTotal);
		Hbox terbilangTotal = new Hbox();
		terbilangTotal.setAlign("center");
		terbilangTotal.setSpacing("3px");
		terbilangTotal.setParent(barisTotal);
		MyLabelBoldAja kurungT1 = new MyLabelBoldAja("(");
		kurungT1.setStyle("color:#94a3b8;font-size:11px;");
		kurungT1.setParent(terbilangTotal);
		footerTotalTerbilang.setStyle("color:#94a3b8;font-size:11px;");
		footerTotalTerbilang.setParent(terbilangTotal);
		MyLabelBoldAja kurungT2 = new MyLabelBoldAja(")");
		kurungT2.setStyle("color:#94a3b8;font-size:11px;");
		kurungT2.setParent(terbilangTotal);

		grid.setParent(ringkasanBayar);

		// Pemisah tipis antara ringkasan dan tombol
		new Html("<div style='height:1px;background:#e2e8f0;margin:10px 0;'></div>").setParent(ringkasanBayar);

		// Box tombol bayar (diisi caller via menuBayar(box)) — dibuat rata tengah.
		Box box = Common.isMobile() ? new Vbox() : new Hbox();
		if (box instanceof Hbox) {
			// Desktop: JANGAN paksa width 100%. Di ZK, Box dirender sebagai <table>; bila
			// width 100% sel-selnya ikut melebar sehingga tombol tampak terpencar ke kiri &
			// kanan. Pakai lebar mengikuti isi + margin auto + pack/align center agar
			// kelompok tombol benar-benar berada di TENGAH.
			((Hbox) box).setAlign("center");
			((Hbox) box).setPack("center");
			box.setStyle("margin-left:auto;margin-right:auto;");
		} else {
			box.setWidth("100%");
		}
		box.setParent(ringkasanBayar);
		return box;
	}

	/** Cooldown anti-pembayaran-ganda dalam ms. Konfig {@code cooldown_pembayaran_ganda_detik} (default 300 dtk). */
	public static long getBayarCooldownMs() {
		try {
			int detik = Integer
					.parseInt(Common.getKonfigurasi("cooldown_pembayaran_ganda_detik", "300").getNilai().trim());
			if (detik < 0) {
				detik = 0;
			}
			return detik * 1000L;
		} catch (Exception e) {
			return 300000L;
		}
	}

	/**
	 * Signature pembayaran = kegiatan + himpunan (item:nominal) yang sedang dibayar di {@code gridCicilan}.
	 * Dua pembayaran identik menghasilkan signature sama; pembayaran berbeda (item/nominal/angsuran beda)
	 * menghasilkan signature beda sehingga tidak diblok. Kembalikan null bila tidak ada baris yang dibayar.
	 */
	public static String buildBayarSignature(Kegiatan keg, Grid gridCicilan) {
		try {
			StringBuilder sb = new StringBuilder();
			sb.append(keg == null || keg.getId() == null ? "0" : keg.getId().toString());
			List<String> parts = new ArrayList<String>();
			if (gridCicilan != null && gridCicilan.getRows() != null) {
				for (Object o : gridCicilan.getRows().getChildren()) {
					if (!(o instanceof Row)) {
						continue;
					}
					Row row = (Row) o;
					MyDoublebox jc = (MyDoublebox) row.getAttribute("jumlahCicilan");
					if (jc == null || jc.getValue() == null || jc.getValue().intValue() == 0) {
						continue;
					}
					Combobox item = (Combobox) row.getAttribute("itemBiaya");
					Object iv = item == null || item.getSelectedItem() == null ? null
							: item.getSelectedItem().getValue();
					String itemKey = "?";
					if (iv instanceof PengaturanPembayaranBulanan) {
						itemKey = "B" + ((PengaturanPembayaranBulanan) iv).getId();
					} else if (iv instanceof DetailBiaya) {
						itemKey = "D" + ((DetailBiaya) iv).getId();
					}
					parts.add(itemKey + ":" + jc.getValue());
				}
			}
			if (parts.isEmpty()) {
				return null;
			}
			Collections.sort(parts);
			sb.append("|").append(parts.toString());
			return sb.toString();
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Muat halaman lain ke dalam sebuah Tabpanel (sekali saja — bila tabpanel masih kosong).
	 * Dipakai tab Pembelian/Diskon/Upload-Download di kedua action.
	 */
	public static void loadIframeToTabpanel(Tabpanel panel, String url) {
		if (panel != null && panel.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(panel);

			MyInclude iframe = new MyInclude(url);
			iframe.setHeight("100%");
			iframe.setWidth("100%");
			iframe.setParent(window);
		}
	}

	/**
	 * Tentukan daftar DetailBiaya yang MASIH perlu dibayar: item yang nilai tagihannya (dari label/doublebox
	 * di {@code gridss}) masih lebih besar dari total yang sudah dicicil. Bila belum ada cicilan sama sekali,
	 * kembalikan semua item ({@code semuaItemBiaya}). Logika identik untuk Lama &amp; Baru.
	 */
	public static List<DetailBiaya> updateDetailBiayaUntukDibayar(List<CicilanPembayaran> cicilanPembayarans,
			Grid gridss, Collection<DetailBiaya> semuaItemBiaya) {
		List<DetailBiaya> itemBiayasBaru = new ArrayList<DetailBiaya>();
		if (cicilanPembayarans != null && !cicilanPembayarans.isEmpty()) {
			Map<Long, Double> totalsYangSudahDicicil = new HashMap<Long, Double>();
			for (CicilanPembayaran cicilan : cicilanPembayarans) {
				if (cicilan.getItemBiaya() != null) {
					if (!totalsYangSudahDicicil.containsKey(cicilan.getItemBiaya().getId())) {
						totalsYangSudahDicicil.put(cicilan.getItemBiaya().getId(), cicilan.getNilai());
					} else {
						totalsYangSudahDicicil.put(cicilan.getItemBiaya().getId(),
								cicilan.getNilai() + totalsYangSudahDicicil.get(cicilan.getItemBiaya().getId()));
					}
				}
			}

			Rows rows = gridss == null ? null : (Rows) gridss.getRows();
			if (rows != null) {
				List<Row> myRows = rows.getChildren();
				for (Row rowBiaya : myRows) {
					try {
						DetailBiaya detailBiaya = (DetailBiaya) rowBiaya.getAttribute("myValue");
						// Baris tanpa atribut "myValue" (mis. baris header/pemisah) atau tanpa ItemBiaya
						// → lewati agar tidak NPE saat getNilaiBiayaBaru()/getItemBiaya().getId().
						if (detailBiaya == null || detailBiaya.getItemBiaya() == null) {
							continue;
						}
						Double biaya = detailBiaya.getNilaiBiayaBaru() == null ? detailBiaya.getNilaiBiaya()
								: detailBiaya.getNilaiBiayaBaru();
						try {
							Component component = (Component) rowBiaya.getAttribute("tag");
							if (component instanceof Label) {
								Label myLabel = (Label) component;
								biaya = Common.numberFormat.get().parse(myLabel.getValue()).doubleValue();
							} else if (component instanceof MyDoublebox
									&& detailBiaya.getItemBiaya().getNilaiBisaDiubah()) {
								MyDoublebox jumlaha = (MyDoublebox) component;
								biaya = jumlaha.getValue() == null ? 0.0 : jumlaha.getValue();
							}
						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
						}

						Double dibayar = totalsYangSudahDicicil.get(detailBiaya.getItemBiaya().getId());
						if (dibayar == null || (biaya.intValue() > (dibayar.intValue() + 1))) {
							itemBiayasBaru.add(detailBiaya);
						}
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
				}
			}
		} else if (semuaItemBiaya != null) {
			itemBiayasBaru.addAll(semuaItemBiaya);
		}
		return itemBiayasBaru;
	}

	/**
	 * Pilih {@link SettingBiaya} yang BENAR-BENAR sesuai dengan tagihan mahasiswa/calon
	 * yang sedang dipilih — bukan sekadar item terakhir pada daftar. Dipakai tombol
	 * "Ubah Tagihan" agar yang dibuka adalah setting biaya yang menghasilkan tagihan
	 * mahasiswa terpilih. Prioritas kecocokan:
	 * <ol>
	 * <li>jenis kegiatan + semester + tahun akademik sama,</li>
	 * <li>jenis kegiatan sama,</li>
	 * </ol>
	 * Mengembalikan {@code null} bila tidak ada setting yang cocok jenis kegiatannya;
	 * pemanggil dapat membuka layar filter detail_biaya_excel (yang juga sudah tersaring
	 * per profil mahasiswa) sehingga tidak pernah membuka setting yang salah.
	 * <p>
	 * Tiap elemen koleksi boleh berupa {@link DetailBiaya} maupun
	 * {@link PengaturanPembayaranBulanan} (kasus tagihan bulanan — diambil
	 * {@code getDetailBiaya()}-nya). Setting biaya di-resolve dari dua kemungkinan
	 * relasi: langsung lewat {@code DetailBiaya.getSettingBiaya()} atau via
	 * {@code DetailBiaya.getSettingBiayaDetail().getSettingBiaya()}.
	 *
	 * @param detailBiayas  koleksi DetailBiaya/PengaturanPembayaranBulanan milik
	 *                      mahasiswa+kegiatan yang sedang dipilih
	 * @param jenisKegiatan jenis pembayaran (kegiatan) yang sedang aktif
	 * @param kegiatan      kegiatan terpilih (sumber semester &amp; tahun akademik)
	 */
	public static SettingBiaya pilihSettingBiayaSesuai(Collection<?> detailBiayas, JenisKegiatan jenisKegiatan,
			Kegiatan kegiatan) {
		if (detailBiayas == null || detailBiayas.isEmpty()) {
			return null;
		}
		Long idJenisKegiatan = jenisKegiatan == null ? null : jenisKegiatan.getId();
		String semesterKegiatan = kegiatan == null || kegiatan.getSemster() == null ? null
				: String.valueOf(kegiatan.getSemster());
		String tahunKegiatan = kegiatan == null ? null : kegiatan.getTahunAkademik();

		SettingBiaya cocokPenuh = null;
		SettingBiaya cocokJenisKegiatan = null;
		for (Object o : detailBiayas) {
			SettingBiaya settingBiaya = resolveSettingBiaya(o);
			if (settingBiaya == null) {
				continue;
			}
			boolean jenisCocok = idJenisKegiatan != null && settingBiaya.getJenisKegiatan() != null
					&& idJenisKegiatan.equals(settingBiaya.getJenisKegiatan().getId());
			if (jenisCocok && cocokJenisKegiatan == null) {
				cocokJenisKegiatan = settingBiaya;
			}
			boolean semesterCocok = semesterKegiatan != null && semesterKegiatan.equals(settingBiaya.getSemester());
			boolean tahunCocok = tahunKegiatan != null && tahunKegiatan.equals(settingBiaya.getTahunAkademik());
			if (jenisCocok && semesterCocok && tahunCocok && cocokPenuh == null) {
				cocokPenuh = settingBiaya;
			}
		}
		return cocokPenuh != null ? cocokPenuh : cocokJenisKegiatan;
	}

	/**
	 * Ambil SettingBiaya dari sebuah item tagihan, baik {@link DetailBiaya} langsung
	 * maupun {@link PengaturanPembayaranBulanan} (lewat {@code getDetailBiaya()}).
	 * Relasi setting dicek dua arah: kolom {@code setting_biaya} langsung, lalu
	 * fallback ke {@code settingBiayaDetail.getSettingBiaya()}.
	 */
	private static SettingBiaya resolveSettingBiaya(Object o) {
		DetailBiaya detailBiaya = null;
		if (o instanceof DetailBiaya) {
			detailBiaya = (DetailBiaya) o;
		} else if (o instanceof PengaturanPembayaranBulanan) {
			detailBiaya = ((PengaturanPembayaranBulanan) o).getDetailBiaya();
		}
		if (detailBiaya == null) {
			return null;
		}
		SettingBiaya settingBiaya = detailBiaya.getSettingBiaya();
		if (settingBiaya == null && detailBiaya.getSettingBiayaDetail() != null) {
			settingBiaya = detailBiaya.getSettingBiayaDetail().getSettingBiaya();
		}
		return settingBiaya;
	}
}
