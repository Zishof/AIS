package ais.action.master.helper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.CicilanPembayaran;
import ais.database.model.DetailBiaya;
import ais.database.model.ItemBiaya;
import ais.database.model.Kegiatan;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyMessageboxConfig;

/**
 * Pusat penanganan CicilanPembayaran GANDA pada modul DaftarUlangMahasiswa
 * (Lama &amp; Baru). Tiga peran:
 *
 * <ol>
 * <li><b>Idempotency (sisi SAVE)</b> — {@link #adaKembarDiDb}: sebelum membuat
 * cicilan baru, cek apakah cicilan identik (item + bulan/ppb + nominal) sudah
 * ada di DB; bila ada, jangan buat record baru. Ini mencegah duplikat
 * benar-benar terbentuk, konsisten untuk tampilan maupun akuntansi.</li>
 * <li><b>Deteksi (sisi BACA)</b> — {@link #cariKembar}: dari daftar cicilan,
 * kembalikan baris REDUNDAN (semua kecuali yang paling awal/ID terkecil di tiap
 * grup identik). Tidak menyembunyikan apa pun, hanya menandai.</li>
 * <li><b>Pembersihan</b> — {@link #bersihkan}: hapus baris redundan lalu hitung
 * ulang total kegiatan (pola sama dengan PembayaranUtil batal-bayar).</li>
 * </ol>
 *
 * Kompatibel Java 1.6/1.7 (tanpa lambda/stream/diamond).
 */
public class CicilanDuplikatHelper {

	/**
	 * Kunci identitas pembayaran: item (detailBiaya, fallback itemBiaya) + bulan
	 * (pengaturanPembayaranBulanan) + nominal. Dua cicilan dengan kunci sama untuk
	 * kegiatan yang sama dianggap GANDA.
	 */
	private static String kunci(CicilanPembayaran c) {
		if (c == null) {
			return null;
		}
		Long detail = c.getDetailBiaya() == null ? null : c.getDetailBiaya().getId();
		Long item = c.getItemBiaya() == null ? null : c.getItemBiaya().getId();
		Long ppb = c.getPengaturanPembayaranBulanan() == null ? null
				: c.getPengaturanPembayaranBulanan().getId();
		Double nilai = c.getNilai();
		String itemKey = detail != null ? ("D" + detail) : (item != null ? ("I" + item) : "?");
		return itemKey + "|B" + (ppb == null ? "-" : ppb.toString()) + "|N" + (nilai == null ? "0" : nilai.toString());
	}

	/**
	 * Daftar cicilan REDUNDAN (duplikat) dari sebuah list. Yang dipertahankan adalah
	 * cicilan dengan ID terkecil (paling awal dibuat) di tiap grup identik.
	 */
	public static List<CicilanPembayaran> cariKembar(List<CicilanPembayaran> semua) {
		List<CicilanPembayaran> redundan = new ArrayList<CicilanPembayaran>();
		if (semua == null || semua.size() < 2) {
			return redundan;
		}
		List<CicilanPembayaran> urut = new ArrayList<CicilanPembayaran>(semua);
		Collections.sort(urut, new Comparator<CicilanPembayaran>() {
			@Override
			public int compare(CicilanPembayaran a, CicilanPembayaran b) {
				long ia = a == null || a.getId() == null ? Long.MAX_VALUE : a.getId().longValue();
				long ib = b == null || b.getId() == null ? Long.MAX_VALUE : b.getId().longValue();
				return ia < ib ? -1 : (ia > ib ? 1 : 0);
			}
		});
		Map<String, CicilanPembayaran> pertama = new LinkedHashMap<String, CicilanPembayaran>();
		for (CicilanPembayaran c : urut) {
			String k = kunci(c);
			if (k == null || k.startsWith("?")) {
				continue;
			}
			if (pertama.containsKey(k)) {
				redundan.add(c);
			} else {
				pertama.put(k, c);
			}
		}
		return redundan;
	}

	/**
	 * Versi sadar-tagihan. Hanya menandai baris ganda yang BENAR-BENAR membuat total
	 * pembayaran <b>melebihi tagihan</b>. Dua (atau lebih) pembayaran identik yang totalnya
	 * masih &le; tagihan — misalnya dua cicilan Rp5.000 untuk tagihan Rp10.000 — dianggap
	 * <b>sah</b> (cicilan/angsuran wajar) dan TIDAK ditandai.
	 *
	 * Baris yang ditandai dipilih dari ID terbaru, hanya sebatas nilai kelebihan bayar,
	 * sehingga pembersihan mengembalikan total persis ke angka tagihan tanpa membuat kurang bayar.
	 * Bila tagihan tidak diketahui (≤ 0), tidak ada yang ditandai (konservatif, hindari salah hapus).
	 */
	public static List<CicilanPembayaran> cariKembar(List<CicilanPembayaran> semua, Kegiatan kegiatan) {
		List<CicilanPembayaran> kandidat = cariKembar(semua);
		if (kandidat.isEmpty()) {
			return kandidat;
		}
		double tagihan = hitungTagihan(kegiatan);
		double dibayar = hitungTotalDibayar(semua);
		double kelebihan = dibayar - tagihan;
		// Tidak melebihi tagihan (atau tagihan tak diketahui) → pembayaran dianggap sah, jangan ditandai.
		if (tagihan <= 0.5 || kelebihan <= 0.5) {
			return new ArrayList<CicilanPembayaran>();
		}
		List<CicilanPembayaran> urutBaru = new ArrayList<CicilanPembayaran>(kandidat);
		Collections.sort(urutBaru, new Comparator<CicilanPembayaran>() {
			@Override
			public int compare(CicilanPembayaran a, CicilanPembayaran b) {
				long ia = a == null || a.getId() == null ? Long.MIN_VALUE : a.getId().longValue();
				long ib = b == null || b.getId() == null ? Long.MIN_VALUE : b.getId().longValue();
				return ia < ib ? 1 : (ia > ib ? -1 : 0); // ID terbaru lebih dulu
			}
		});
		List<CicilanPembayaran> hasil = new ArrayList<CicilanPembayaran>();
		double terkumpul = 0.0;
		for (CicilanPembayaran c : urutBaru) {
			double n = c == null || c.getNilai() == null ? 0.0 : c.getNilai().doubleValue();
			if (n <= 0.0) {
				continue;
			}
			// Hanya tandai duplikat sebatas kelebihan bayar; bila satu baris saja sudah melebihi
			// sisa kelebihan, jangan ditandai agar tidak membuat total jadi kurang dari tagihan.
			if (terkumpul + n <= kelebihan + 0.5) {
				hasil.add(c);
				terkumpul += n;
			}
		}
		return hasil;
	}

	private static double hitungTotalDibayar(List<CicilanPembayaran> semua) {
		double total = 0.0;
		if (semua != null) {
			for (CicilanPembayaran c : semua) {
				if (c != null && c.getNilai() != null) {
					total += c.getNilai().doubleValue();
				}
			}
		}
		return total;
	}

	private static double hitungTagihan(Kegiatan kegiatan) {
		try {
			if (kegiatan == null) {
				return 0.0;
			}
			Double t = kegiatan.hitungTagihan();
			return t == null ? 0.0 : t.doubleValue();
		} catch (Exception e) {
			return 0.0;
		}
	}

	/**
	 * Deteksi "pembayaran lebih dari sekali" PER ITEM. Untuk tiap item biaya (detailBiaya/itemBiaya,
	 * dan untuk bulanan: per bulan/pengaturanPembayaranBulanan) yang total pembayarannya MELEBIHI
	 * tagihan item itu DAN ada baris identik (item+bulan+nominal) yang berulang, kembalikan baris
	 * berlebih (dipilih dari ID terbaru) yang perlu dibersihkan agar dibayar == tagihan persis.
	 *
	 * Berbeda dengan {@link #cariKembar(List, Kegiatan)} yang memakai total kegiatan (bisa keliru bila
	 * ada item bernilai negatif spt beasiswa/potongan), method ini menghitung kelebihan PER ITEM dan
	 * mendukung nominal negatif. Bila tagihan item tak diketahui (0), item itu dilewati (aman).
	 * Hanya baris DUPLIKAT (selain yang paling awal) yang menjadi kandidat hapus.
	 */
	public static List<CicilanPembayaran> cariPembayaranBerulang(List<CicilanPembayaran> semua, Kegiatan kegiatan) {
		List<CicilanPembayaran> hasil = new ArrayList<CicilanPembayaran>();
		if (semua == null || semua.size() < 2 || kegiatan == null) {
			return hasil;
		}
		// Baris identik yang berulang (item+ppb+nominal), selain yang paling awal.
		java.util.Set<CicilanPembayaran> redundanSet = new java.util.HashSet<CicilanPembayaran>(cariKembar(semua));
		if (redundanSet.isEmpty()) {
			return hasil;
		}
		// Kelompokkan per item (detailBiaya/itemBiaya + bulan/ppb), abaikan nominal.
		LinkedHashMap<String, List<CicilanPembayaran>> perItem = new LinkedHashMap<String, List<CicilanPembayaran>>();
		for (CicilanPembayaran c : semua) {
			String ik = kunciItem(c);
			if (ik == null) {
				continue;
			}
			List<CicilanPembayaran> grup = perItem.get(ik);
			if (grup == null) {
				grup = new ArrayList<CicilanPembayaran>();
				perItem.put(ik, grup);
			}
			grup.add(c);
		}
		for (List<CicilanPembayaran> grup : perItem.values()) {
			double tagihan = hitungTagihanItem(kegiatan, grup);
			if (Math.abs(tagihan) <= 0.5) {
				continue; // tagihan item tak diketahui → jangan hapus (aman)
			}
			double dibayar = 0.0;
			for (CicilanPembayaran c : grup) {
				dibayar += c.getNilai() == null ? 0.0 : c.getNilai().doubleValue();
			}
			double kelebihan = dibayar - tagihan;
			// Hanya tandai bila pembayaran BENAR-BENAR MELEBIHI tagihan, searah tanda tagihan:
			// item positif → dibayar > tagihan; item negatif (mis. beasiswa) → dibayar < tagihan.
			// KURANG BAYAR (dibayar masih di bawah tagihan, kelebihan negatif pd item positif)
			// JANGAN ditandai — dulu pakai Math.abs sehingga kurang-bayar pun keliru terdeteksi.
			boolean adaKelebihan = (tagihan > 0 && kelebihan > 0.5) || (tagihan < 0 && kelebihan < -0.5);
			if (!adaKelebihan) {
				continue; // pas/kurang dari tagihan → cicilan/angsuran sah, jangan ditandai
			}
			// Baris duplikat di grup ini, urut ID terbaru lebih dulu.
			List<CicilanPembayaran> redGrup = new ArrayList<CicilanPembayaran>();
			for (CicilanPembayaran c : grup) {
				if (redundanSet.contains(c)) {
					redGrup.add(c);
				}
			}
			Collections.sort(redGrup, new Comparator<CicilanPembayaran>() {
				@Override
				public int compare(CicilanPembayaran a, CicilanPembayaran b) {
					long ia = a == null || a.getId() == null ? Long.MIN_VALUE : a.getId().longValue();
					long ib = b == null || b.getId() == null ? Long.MIN_VALUE : b.getId().longValue();
					return ia < ib ? 1 : (ia > ib ? -1 : 0);
				}
			});
			// Hapus duplikat sebatas nilai kelebihan (pakai nilai absolut agar item negatif ikut tertangani).
			double sisa = Math.abs(kelebihan);
			for (CicilanPembayaran c : redGrup) {
				double n = Math.abs(c.getNilai() == null ? 0.0 : c.getNilai().doubleValue());
				if (n <= 0.0) {
					continue;
				}
				if (n <= sisa + 0.5) {
					hasil.add(c);
					sisa -= n;
				}
				if (sisa <= 0.5) {
					break;
				}
			}
		}
		return hasil;
	}

	/** Kunci item (tanpa nominal): detailBiaya/itemBiaya + bulan/ppb. */
	private static String kunciItem(CicilanPembayaran c) {
		if (c == null) {
			return null;
		}
		Long detail = c.getDetailBiaya() == null ? null : c.getDetailBiaya().getId();
		Long item = c.getItemBiaya() == null ? null : c.getItemBiaya().getId();
		Long ppb = c.getPengaturanPembayaranBulanan() == null ? null : c.getPengaturanPembayaranBulanan().getId();
		String itemKey = detail != null ? ("D" + detail) : (item != null ? ("I" + item) : null);
		if (itemKey == null) {
			return null;
		}
		return itemKey + "|B" + (ppb == null ? "-" : ppb.toString());
	}

	/** Tagihan satu item (pakai detailBiaya dari salah satu cicilan di grup). 0 bila tak diketahui. */
	private static double hitungTagihanItem(Kegiatan kegiatan, List<CicilanPembayaran> grup) {
		try {
			for (CicilanPembayaran c : grup) {
				if (c != null && c.getDetailBiaya() != null) {
					Double t = Kegiatan.ambilJumlahTagihan(kegiatan, c.getDetailBiaya());
					return t == null ? 0.0 : t.doubleValue();
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return 0.0;
	}

	public static boolean adaKembar(List<CicilanPembayaran> semua) {
		return !cariKembar(semua).isEmpty();
	}

	/**
	 * Idempotency check sisi SAVE: apakah sudah ada CicilanPembayaran identik
	 * (kegiatan + item/detailBiaya + ppb + nominal) di DB. Fail-open: bila terjadi
	 * error pengecekan, kembalikan false agar pembayaran sah tidak ikut terblokir.
	 */
	public static boolean adaKembarDiDb(Session session, Kegiatan kegiatan, DetailBiaya detailBiaya,
			PengaturanPembayaranBulanan ppb, ItemBiaya itemBiaya, Double nilai) {
		try {
			if (session == null || kegiatan == null || kegiatan.getId() == null || nilai == null) {
				return false;
			}
			Criteria c = session.createCriteria(CicilanPembayaran.class)
					.add(Restrictions.eq("kegiatan", kegiatan))
					.add(Restrictions.eq("nilai", nilai));
			if (detailBiaya != null && detailBiaya.getId() != null) {
				c.add(Restrictions.eq("detailBiaya", detailBiaya));
			} else if (itemBiaya != null && itemBiaya.getId() != null) {
				c.add(Restrictions.eq("itemBiaya", itemBiaya));
			} else {
				return false;
			}
			if (ppb != null && ppb.getId() != null) {
				c.add(Restrictions.eq("pengaturanPembayaranBulanan", ppb));
			} else {
				c.add(Restrictions.isNull("pengaturanPembayaranBulanan"));
			}
			c.setProjection(Projections.rowCount());
			c.setMaxResults(1);
			Number n = (Number) c.uniqueResult();
			return n != null && n.longValue() > 0;
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return false;
		}
	}

	/**
	 * Hapus baris cicilan REDUNDAN untuk satu kegiatan, lalu hitung ulang total
	 * kegiatan dari sisa cicilan. Mengembalikan jumlah baris yang dihapus.
	 */
	@SuppressWarnings("unchecked")
	public static int bersihkan(Kegiatan kegiatan) {
		if (kegiatan == null || kegiatan.getId() == null) {
			return 0;
		}
		Session session = null;
		int dihapus = 0;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			Kegiatan keg = (Kegiatan) session.get(Kegiatan.class, kegiatan.getId());
			if (keg == null) {
				return 0;
			}
			List<CicilanPembayaran> semua = session.createCriteria(CicilanPembayaran.class)
					.add(Restrictions.eq("kegiatan", keg)).addOrder(Order.asc("id")).list();
			List<CicilanPembayaran> redundan = cariKembar(semua, keg);
			if (redundan.isEmpty()) {
				return 0;
			}
			Transaction tx = session.beginTransaction();
			for (CicilanPembayaran c : redundan) {
				Common.refreshDelete(session, c);
				dihapus++;
			}
			Number sisa = (Number) session.createCriteria(CicilanPembayaran.class)
					.add(Restrictions.eq("kegiatan", keg)).setProjection(Projections.sum("nilai")).uniqueResult();
			double total = sisa == null ? 0.0 : sisa.doubleValue();
			keg.setAmount(total);
			keg.setJumlahTelahDibayar(total);
			Common.refreshUpdate(session, keg);
			session.flush();
			tx.commit();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembersihan data cicilan pembayaran duplikat",
					e,
					new String[] {
							"Periksa kembali apakah data Kegiatan/tagihan yang bersangkutan tidak sedang diubah oleh proses pembayaran lain saat ini.",
							"Coba ulangi proses pembersihan duplikat beberapa saat lagi.",
							"Bila kegagalan berulang, laporkan ke Administrator/pengembang disertai tangkapan layar (screenshot) pesan ini, agar data cicilan tidak keliru saat direkonsiliasi."
					});
			try {
				if (session != null && session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/CicilanDuplikatHelper.java:376");
			}
			dihapus = 0;
		} finally {
			try {
				if (session != null && session.isOpen()) {
					session.close();
				}
			} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/CicilanDuplikatHelper.java:384");
			}
		}
		return dihapus;
	}

	private static String ringkas(CicilanPembayaran c) {
		String item = c.getItemBiaya() != null && c.getItemBiaya().getNama() != null ? c.getItemBiaya().getNama()
				: (c.getDetailBiaya() != null ? ("DetailBiaya #" + c.getDetailBiaya().getId()) : "-");
		String nominal = c.getNilai() == null ? "0" : Common.numberFormat.get().format(c.getNilai());
		return "Ke-" + (c.getKe() == null ? "-" : c.getKe()) + " | " + item + " | Rp " + nominal
				+ (c.getId() == null ? "" : (" | id:" + c.getId()));
	}

	/**
	 * Pasang alat penanganan duplikat ke panel (checkbox "Tampilkan data ganda" +
	 * tombol "Bersihkan Data Ganda"). Hanya muncul bila terdeteksi duplikat.
	 *
	 * @param onRefresh dipanggil setelah pembersihan sukses (mis. reload panel).
	 */
	public static void pasangAlatDuplikat(final Kegiatan kegiatan, final List<CicilanPembayaran> cicilanList,
			Component parent, final EventListener onRefresh) {
		try {
			final List<CicilanPembayaran> kembar = cariKembar(cicilanList, kegiatan);
			if (kembar.isEmpty() || parent == null) {
				return;
			}

			Vbox box = new Vbox();
			box.setWidth("100%");
			box.setStyle("border:1px solid #f59e0b;background:#fff7ed;border-radius:10px;"
					+ "padding:8px 10px;margin:6px 0;box-sizing:border-box;");
			box.setParent(parent);

			Label warn = new Label("⚠ Terdeteksi " + kembar.size()
					+ " baris pembayaran yang membuat total pembayaran MELEBIHI tagihan (item & nominal sama, "
					+ "kemungkinan tidak sengaja terbayar dua kali). Periksa lalu bersihkan bila benar duplikat.");
			warn.setStyle("color:#9a3412;font-weight:bold;font-size:12px;");
			warn.setParent(box);

			final Vbox detail = new Vbox();
			detail.setWidth("100%");
			detail.setVisible(false);
			detail.setStyle("margin-top:6px;");

			Hbox tools = new Hbox();
			tools.setAlign("center");
			tools.setSpacing("12px");
			tools.setStyle("margin-top:6px;");
			tools.setParent(box);

			final MyCheckboxConfig cb = new MyCheckboxConfig("Tampilkan data ganda");
			cb.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event e) throws Exception {
					Common.clear(detail);
					detail.setVisible(cb.isChecked());
					if (cb.isChecked()) {
						for (CicilanPembayaran c : kembar) {
							Label l = new Label(ringkas(c));
							l.setStyle("display:block;font-size:11px;color:#7c2d12;");
							l.setParent(detail);
						}
					}
				}
			});
			tools.appendChild(cb);

			MyButtonConfig btn = new MyButtonConfig("Bersihkan Data Ganda", "/img/delete.png");
			btn.setStyle("background:linear-gradient(135deg,#ef4444,#b91c1c);color:#ffffff;"
					+ "font-weight:bold;border-radius:8px;");
			btn.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event e) throws Exception {
					MyMessageboxConfig.show(
							"Akan menghapus " + kembar.size() + " baris pembayaran GANDA untuk mahasiswa ini "
									+ "(baris paling awal di tiap grup tetap dipertahankan).\n\n"
									+ "Tindakan ini MENGHAPUS data pembayaran dan tidak dapat dibatalkan. Lanjutkan?",
							"Konfirmasi Bersihkan Data Ganda", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {
								@Override
								public void onEvent(Event ev) throws Exception {
									if (Integer.parseInt(ev.getData().toString()) != MyMessageboxConfig.OK) {
										return;
									}
									int dihapus = bersihkan(kegiatan);
									MyMessageboxConfig.show(
											dihapus + " baris pembayaran ganda berhasil dihapus.",
											"Selesai", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
									if (onRefresh != null) {
										onRefresh.onEvent(null);
									}
								}
							});
				}
			});
			tools.appendChild(btn);

			detail.setParent(box);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Bersihkan pembayaran "lebih dari sekali" PER ITEM: hapus baris berlebih sehingga total
	 * pembayaran tiap item kembali PERSIS sama dengan tagihannya. Kembalikan jumlah baris dihapus.
	 */
	@SuppressWarnings("unchecked")
	public static int bersihkanPembayaranBerulang(Kegiatan kegiatan) {
		if (kegiatan == null || kegiatan.getId() == null) {
			return 0;
		}
		Session session = null;
		int dihapus = 0;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			Kegiatan keg = (Kegiatan) session.get(Kegiatan.class, kegiatan.getId());
			if (keg == null) {
				return 0;
			}
			List<CicilanPembayaran> semua = session.createCriteria(CicilanPembayaran.class)
					.add(Restrictions.eq("kegiatan", keg)).addOrder(Order.asc("id")).list();
			List<CicilanPembayaran> berlebih = cariPembayaranBerulang(semua, keg);
			if (berlebih.isEmpty()) {
				return 0;
			}
			Transaction tx = session.beginTransaction();
			for (CicilanPembayaran c : berlebih) {
				Common.refreshDelete(session, c);
				dihapus++;
			}
			Number sisa = (Number) session.createCriteria(CicilanPembayaran.class)
					.add(Restrictions.eq("kegiatan", keg)).setProjection(Projections.sum("nilai")).uniqueResult();
			double total = sisa == null ? 0.0 : sisa.doubleValue();
			keg.setAmount(total);
			keg.setJumlahTelahDibayar(total);
			Common.refreshUpdate(session, keg);
			session.flush();
			tx.commit();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembersihan data pembayaran cicilan yang berulang",
					e,
					new String[] {
							"Periksa kembali apakah data Kegiatan/tagihan yang bersangkutan tidak sedang diubah oleh proses pembayaran lain saat ini.",
							"Coba ulangi proses pembersihan beberapa saat lagi.",
							"Bila kegagalan berulang, laporkan ke Administrator/pengembang disertai tangkapan layar (screenshot) pesan ini, agar data cicilan tidak keliru saat direkonsiliasi."
					});
			try {
				if (session != null && session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/CicilanDuplikatHelper.java:530");
			}
			dihapus = 0;
		} finally {
			try {
				if (session != null && session.isOpen()) {
					session.close();
				}
			} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/CicilanDuplikatHelper.java:538");
			}
		}
		return dihapus;
	}

	/**
	 * Pasang tombol "Terdeteksi pembayaran lebih dari sekali" ke panel. Hanya muncul bila ada item
	 * yang dibayar melebihi tagihan akibat baris berulang. Saat diklik: bersihkan kelebihan per item
	 * sampai dibayar == tagihan persis, lalu refresh.
	 */
	public static void pasangAlatPembayaranBerulang(final Kegiatan kegiatan, final List<CicilanPembayaran> cicilanList,
			Component parent, final EventListener onRefresh) {
		try {
			final List<CicilanPembayaran> berlebih = cariPembayaranBerulang(cicilanList, kegiatan);
			if (berlebih.isEmpty() || parent == null) {
				return;
			}

			Vbox box = new Vbox();
			box.setWidth("100%");
			box.setStyle("border:1px solid #ef4444;background:#fef2f2;border-radius:10px;"
					+ "padding:8px 10px;margin:6px 0;box-sizing:border-box;");
			box.setParent(parent);

			Label warn = new Label("⚠ Terdeteksi pembayaran lebih dari sekali: ada " + berlebih.size()
					+ " baris yang membuat biaya MELEBIHI tagihan (item sama; untuk bulanan, di bulan yang sama). "
					+ "Klik tombol di bawah untuk membersihkan kelebihan hingga pembayaran pas dengan tagihan.");
			warn.setStyle("color:#991b1b;font-weight:bold;font-size:12px;");
			warn.setParent(box);

			Hbox tools = new Hbox();
			tools.setAlign("center");
			tools.setSpacing("12px");
			tools.setStyle("margin-top:6px;");
			tools.setParent(box);

			MyButtonConfig btn = new MyButtonConfig("Terdeteksi pembayaran lebih dari sekali", "/img/delete.png");
			btn.setStyle("background:linear-gradient(135deg,#ef4444,#b91c1c);color:#ffffff;"
					+ "font-weight:bold;border-radius:8px;");
			btn.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event e) throws Exception {
					MyMessageboxConfig.show(
							"Akan membersihkan " + berlebih.size() + " baris pembayaran berlebih sehingga tiap item "
									+ "kembali PERSIS sama dengan tagihannya (baris paling awal dipertahankan).\n\n"
									+ "Tindakan ini MENGHAPUS data pembayaran dan tidak dapat dibatalkan. Lanjutkan?",
							"Konfirmasi Bersihkan Pembayaran Berulang", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {
								@Override
								public void onEvent(Event ev) throws Exception {
									if (Integer.parseInt(ev.getData().toString()) != MyMessageboxConfig.OK) {
										return;
									}
									int dihapus = bersihkanPembayaranBerulang(kegiatan);
									MyMessageboxConfig.show(
											dihapus + " baris pembayaran berlebih berhasil dibersihkan. "
													+ "Pembayaran kini pas dengan tagihan.",
											"Selesai", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
									if (onRefresh != null) {
										onRefresh.onEvent(null);
									}
								}
							});
				}
			});
			tools.appendChild(btn);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}
}
