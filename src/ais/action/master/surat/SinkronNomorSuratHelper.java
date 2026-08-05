package ais.action.master.surat;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.surat.KlasifikasiSuratKeluar;
import ais.database.model.surat.NomorSurat;
import ais.database.model.surat.SuratKeluar;
import ais.ui.util.MyDiv;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * <h3>Sinkronkan Nomor Surat Keluar</h3>
 *
 * <p>Menata ulang penomoran ({@code kode}) seluruh surat pada satu {@link KlasifikasiSuratKeluar}
 * agar TIDAK ada nomor yang sama. Surat diurutkan berdasarkan tanggal (lalu id) lalu diberi nomor
 * urut ulang memakai skema {@link NomorSurat} milik klasifikasi — dengan menghormati aturan reset
 * penomoran (per tahun / per bulan) sehingga urutan tetap benar per periode.</p>
 *
 * <p>Dibuka dari tombol "Sinkronkan Nomor Surat" di {@code SuratKeluarAction}. Hanya admin. Semua
 * operasi berat dibungkus timer agar UI tidak beku. Java 1.7.</p>
 */
public final class SinkronNomorSuratHelper {

	private SinkronNomorSuratHelper() {
	}

	public static void buka(final EventListener onSelesai) throws InterruptedException {
		final MyWindow window = new MyWindow("Sinkronkan Nomor Surat", "normal", true);
		window.setWidth("560px");
		window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

		MyDiv root = new MyDiv();
		root.setStyle("padding:12px;");
		root.setParent(window);

		root.appendChild(new ais.ui.util.MyHtml(
				"<div style='color:#334155;margin-bottom:10px;'>Pilih klasifikasi surat, lalu klik <b>Proses</b>. "
						+ "Sistem akan menata ulang nomor surat pada klasifikasi tersebut agar tidak ada yang sama "
						+ "(urut berdasarkan tanggal, menghormati aturan reset penomoran).</div>"));

		Hbox pilih = new Hbox();
		pilih.setStyle("align-items:center;gap:8px;");
		pilih.setParent(root);
		pilih.appendChild(new Label("Klasifikasi:"));
		final Combobox combo = new Combobox();
		combo.setReadonly(true);
		combo.setWidth("330px");
		try {
			Common.insertCombo(combo, new String[] { "nama" }, "keterangan", KlasifikasiSuratKeluar.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) SinkronNomorSuratHelper.combo");
		}
		pilih.appendChild(combo);

		final Label hasil = new Label("");
		hasil.setStyle("display:block;margin-top:10px;color:#0f172a;");
		hasil.setParent(root);

		Hbox footer = new Hbox();
		footer.setStyle("margin-top:12px;gap:6px;");
		footer.setParent(root);

		final MyToolbarbuttonConfig proses = new MyToolbarbuttonConfig("Proses", "/img/save.gif");
		proses.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				if (combo.getSelectedItem() == null || combo.getSelectedItem().getValue() == null) {
					MyMessageboxConfig.show("Silakan pilih klasifikasi surat terlebih dahulu.", "Informasi",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return;
				}
				final KlasifikasiSuratKeluar klas = (KlasifikasiSuratKeluar) combo.getSelectedItem().getValue();
				MyMessageboxConfig.show(
						"Tata ulang nomor surat untuk klasifikasi \"" + klas.getNama()
								+ "\"? Penomoran surat yang sudah ada akan diperbarui.",
						"Konfirmasi", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {
							@Override
							public void onEvent(Event ev) throws Exception {
								if (Integer.parseInt(ev.getData().toString()) != MyMessageboxConfig.OK) {
									return;
								}
								proses.setDisabled(true);
								hasil.setValue("Memproses…");
								Common.createDefaultTimer(new EventListener() {
									@Override
									public void onEvent(Event t) throws Exception {
										String ringkas = sinkronkan(klas);
										hasil.setValue(ringkas);
										proses.setDisabled(false);
										if (onSelesai != null) {
											onSelesai.onEvent(new Event("onSelesai"));
										}
									}
								});
							}
						});
			}
		});
		proses.setParent(footer);

		MyToolbarbuttonConfig tutup = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
		tutup.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				window.detach();
			}
		});
		tutup.setParent(footer);

		window.setVisible(true);
		try {
			window.onModal();
		} catch (InterruptedException eModal) {
			ais.common.ErrorAuditUtil.record(eModal, "SinkronNomorSuratHelper.buka onModal");
		}
	}

	/**
	 * Tata ulang nomor surat satu klasifikasi. Kembalikan ringkasan hasil untuk ditampilkan.
	 */
	@SuppressWarnings("unchecked")
	public static String sinkronkan(KlasifikasiSuratKeluar klas) {
		try {
			if (klas == null || klas.getNomorSurat() == null) {
				return "Klasifikasi ini belum memiliki skema Nomor Surat.";
			}
			final NomorSurat ns = klas.getNomorSurat();
			final boolean indexUrut = Boolean.TRUE.equals(ns.getGunakanIndexUrut());
			final boolean resetBulan = Boolean.TRUE.equals(ns.getResetUrutanTiapBulan());
			final boolean resetTahun = Boolean.TRUE.equals(ns.getResetUrutanTiapTahun());

			Session session = HibernateUtil.currentSession();
			List<SuratKeluar> daftar = session.createCriteria(SuratKeluar.class)
					.add(Restrictions.eq("klasifikasiSuratKeluar", klas))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.addOrder(Order.asc("tanggal")).addOrder(Order.asc("id")).list();
			if (daftar == null || daftar.isEmpty()) {
				return "Tidak ada surat pada klasifikasi ini.";
			}

			Map<String, Integer> urutPerGrup = new HashMap<String, Integer>();
			int diproses = 0;
			int diperbarui = 0;
			int maksUrut = 0;
			for (SuratKeluar sk : daftar) {
				Date tgl = sk.getTanggal() == null ? ais.ui.util.WaktuUtil.getDate() : sk.getTanggal();
				Calendar c = Calendar.getInstance();
				c.setTime(tgl);
				int th = c.get(Calendar.YEAR);
				int bl = c.get(Calendar.MONTH) + 1;

				String grup;
				if (indexUrut) {
					grup = "ALL";
				} else if (resetBulan) {
					grup = th + "-" + bl;
				} else if (resetTahun) {
					grup = String.valueOf(th);
				} else {
					grup = "ALL";
				}
				Integer u = urutPerGrup.get(grup);
				int urut = u == null ? 1 : u + 1;
				urutPerGrup.put(grup, urut);
				if (urut > maksUrut) {
					maksUrut = urut;
				}

				String kodeBaru = formatKode(ns, klas, (long) urut, tgl, sk);

				boolean berubah = kodeBaru != null && !kodeBaru.equals(sk.getKode());
				sk.setTahun(th);
				sk.setBulan(bl);
				sk.setIndex((long) urut);
				if (kodeBaru != null && kodeBaru.length() > 0) {
					sk.setKode(kodeBaru);
				}
				Common.refreshUpdate(session, sk);
				diproses++;
				if (berubah) {
					diperbarui++;
				}
			}

			// Selaraskan counter untuk skema index-urut agar surat berikutnya tak bentrok.
			if (indexUrut) {
				try {
					ns.setNomorIndex((long) (maksUrut + 1));
					Common.refreshUpdate(session, ns);
				} catch (Exception e) {
					ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) SinkronNomorSuratHelper.counter");
				}
			}

			return "Selesai. " + diproses + " surat ditata ulang, " + diperbarui + " nomor diperbarui.";
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return "Terjadi kesalahan saat sinkronisasi. Silakan hubungi administrator.";
		}
	}

	private static String formatKode(NomorSurat ns, KlasifikasiSuratKeluar klas, Long urut, Date tgl, SuratKeluar sk) {
		String kode;
		SatuanKerja satker = null;
		try {
			satker = sk.getSatuanKerja();
		} catch (Exception e) {
			satker = null;
		}
		try {
			kode = satker != null ? ns.format(urut, tgl, satker) : ns.format(urut, tgl);
		} catch (Exception e) {
			kode = ns.format(urut, tgl);
		}
		try {
			kode = org.apache.commons.lang3.StringUtils.replaceIgnoreCase(kode, "KODE_KLASIFIKASI", klas.getKode());
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) SinkronNomorSuratHelper.formatKode");
		}
		return kode;
	}
}
