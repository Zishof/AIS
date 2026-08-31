package ais.common;

import java.util.List;

import org.hibernate.Session;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.sys.SessionsCtrl;
import org.zkoss.zkplus.util.ThreadLocalListener;
import org.zkoss.zul.Bandbox;
import org.zkoss.zul.Button;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Doublebox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;

import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.DetailLogLogin;
import ais.database.model.LogUserActifity;
import ais.ui.util.MyButtonConfig;

/**
 * Listener siklus-hidup event ZK yang didaftarkan secara global lewat {@code WEB-INF/zk.xml}
 * (bukan dipasang manual per-komponen), diturunkan dari
 * {@link org.zkoss.zkplus.util.ThreadLocalListener} milik framework ZK. Dengan pendaftaran
 * global ini, setiap event yang diproses ZK di seluruh aplikasi — pada thread yang sama dengan
 * pemrosesan request — melewati titik-titik hook yang di-override di kelas ini, menjadikannya
 * tempat sentral untuk dua kebutuhan lintas-halaman: (1) audit terpusat atas exception yang lolos
 * dari listener event ZK mana pun (lewat {@link #cleanup}), dan (2) pencatatan jejak aktivitas
 * pengguna pada komponen UI tertentu (lewat {@link #complete}, meski jalur pencatatan aktivitas
 * ini saat ini dinonaktifkan — lihat penjelasan di bawah).
 *
 * <h2>Event yang ditangani</h2>
 * <p>
 * {@link org.zkoss.zkplus.util.ThreadLocalListener} mendefinisikan sepasang hook seputar siklus
 * satu event ZK yang diproses pada satu thread: {@link #init} dipanggil di awal sebelum event
 * mulai diproses (menentukan apakah pemrosesan lanjut), {@link #prepare} sesaat sebelum listener
 * aplikasi (event handler {@code onClick}, {@code onChange}, dsb.) benar-benar dijalankan,
 * {@link #beforeResume}/{@link #afterResume}/{@link #abortResume} seputar resume thread yang
 * ditunda ZK (mis. saat menunggu I/O asinkron dalam satu siklus event), {@link #cleanup}
 * dipanggil di akhir untuk membersihkan resource thread-local <b>sekaligus menerima daftar
 * exception/error</b> yang terjadi selama pemrosesan event (parameter {@code errs}), dan
 * {@link #complete} dipanggil setelah satu siklus event selesai sepenuhnya. Hampir semua hook
 * selain {@link #cleanup} dan {@link #complete} pada kelas ini murni meneruskan ke implementasi
 * induk ({@code super}) tanpa logika tambahan — sisa badan method berupa komentar cetak debug
 * yang dinonaktifkan (peninggalan proses pengembangan awal).
 * </p>
 *
 * <h2>{@link #cleanup} — audit terpusat exception event ZK</h2>
 * <p>
 * Sebelum diperkaya, exception yang lolos dari event listener ZK 5 hanya membuat UI menampilkan
 * pesan generik "Unknown exception" tanpa jejak yang dapat ditelusuri administrator. Hook
 * {@link #cleanup} kini memeriksa parameter {@code errs} (daftar {@link Throwable}/objek error
 * yang diteruskan framework ZK), membangun konteks ringkas (nama event, kelas dan id komponen)
 * lewat {@link #buatKonteksErrorZk}, mencetak stack trace ke {@code System.err}, dan merekam
 * setiap kegagalan ke {@code ErrorAuditUtil#record} — sehingga kegagalan pada listener event ZK
 * mana pun di seluruh aplikasi otomatis tercatat di audit terpusat tanpa perlu instrumentasi
 * manual per-halaman. Kegagalan pada proses audit itu sendiri ditangkap terpisah agar TIDAK
 * pernah menutupi/menggantikan error asli milik ZK yang seharusnya tetap diproses oleh
 * {@code super.cleanup(...)}.
 * </p>
 *
 * <h2>{@link #complete} — pencatatan aktivitas pengguna (saat ini nonaktif)</h2>
 * <p>
 * Method ini berisi logika ekstensif untuk mencatat interaksi pengguna dengan berbagai jenis
 * komponen ZK (tombol, bandbox, combobox, textbox, doublebox, intbox, decimalbox, window) ke
 * entitas {@link LogUserActifity}, terkait dengan sesi login yang tersimpan pada atribut sesi ZK
 * {@code "detailLogLogin"} (bertipe {@link DetailLogLogin}). Namun, flag pengendali
 * {@code savingActivity} dihardcode {@code false} dan blok kode yang sebelumnya membaca nilainya
 * dari system property {@code savingActivity} sudah dikomentari, sehingga <b>seluruh logika
 * pencatatan aktivitas pada method ini efektif TIDAK PERNAH berjalan pada kondisi kode saat
 * ini</b> — badan method tetap dipertahankan lengkap kemungkinan untuk diaktifkan kembali di
 * kemudian hari. Terlepas dari status nonaktif tersebut, {@link StreamingHibernateUtil} tetap
 * ditutup di akhir setiap pemanggilan {@link #complete} sebagai bagian dari pembersihan resource
 * per-siklus-event.
 * </p>
 */
public class ZkossListener extends ThreadLocalListener {

	/**
	 * Hook siklus-hidup ZK yang dipanggil saat resume thread event dibatalkan. Tidak ada logika
	 * tambahan di kelas ini — hanya meneruskan ke {@code super.abortResume(comp, evt)}.
	 *
	 * @param comp komponen ZK terkait event
	 * @param evt   event ZK yang sedang diproses
	 */
	@Override
	public void abortResume(Component comp, Event evt) {
		// TODO Auto-generated method stub
		super.abortResume(comp, evt);

		// // System.out.println("abortResume => " + comp.getClass().getName() +
		// ", "
		// + evt.getName());
	}

	/**
	 * Hook siklus-hidup ZK yang dipanggil setelah thread event selesai di-resume. Tidak ada
	 * logika tambahan di kelas ini — hanya meneruskan ke {@code super.afterResume(comp, evt)}.
	 *
	 * @param comp komponen ZK terkait event
	 * @param evt   event ZK yang sedang diproses
	 */
	@Override
	public void afterResume(Component comp, Event evt) {
		// TODO Auto-generated method stub
		super.afterResume(comp, evt);
		// // System.out.println("afterResume => " + comp.getClass().getName() +
		// ", "
		// + evt.getName());
	}

	/**
	 * Hook siklus-hidup ZK yang dipanggil sesaat sebelum thread event di-resume. Tidak ada logika
	 * tambahan di kelas ini — hanya meneruskan ke {@code super.beforeResume(comp, evt)}.
	 *
	 * @param comp komponen ZK terkait event
	 * @param evt   event ZK yang sedang diproses
	 */
	@Override
	public void beforeResume(Component comp, Event evt) {
		// TODO Auto-generated method stub
		super.beforeResume(comp, evt);
		// // System.out.println("beforeResume => " + comp.getClass().getName()
		// + ", " + evt.getName());
	}

	/**
	 * Hook siklus-hidup ZK yang dipanggil di akhir pemrosesan satu event, menerima daftar
	 * exception/error yang terjadi selama pemrosesan lewat parameter {@code errs}. Method ini
	 * adalah inti dari audit exception terpusat kelas ini: setiap elemen {@code errs} yang berupa
	 * {@link Throwable} dicetak ke {@code System.err} dan direkam ke {@code ErrorAuditUtil#record}
	 * bersama konteks singkat (nama event + kelas/id komponen) dari {@link #buatKonteksErrorZk};
	 * elemen non-{@link Throwable} yang tidak {@code null} dibungkus jadi
	 * {@link RuntimeException} sebelum direkam. Kegagalan pada proses audit ini sendiri ditangkap
	 * terpisah dan hanya dicetak ke {@code System.err} agar TIDAK pernah menutupi error asli ZK;
	 * {@code super.cleanup(comp, evt, errs)} selalu dipanggil di blok {@code finally} agar
	 * perilaku bawaan ZK tetap berjalan apa pun hasil audit di atasnya.
	 *
	 * @param comp komponen ZK terkait event
	 * @param evt   event ZK yang sedang diproses
	 * @param errs  daftar exception/objek error yang terjadi selama pemrosesan event (tipe mentah
	 *              {@link List} mengikuti signature API {@code ThreadLocalListener}, ditandai
	 *              {@code @SuppressWarnings("rawtypes")}); boleh {@code null} atau kosong
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public void cleanup(Component comp, Event evt, List errs) {
		/*
		 * Hook ini didaftarkan global melalui WEB-INF/zk.xml. ZK 5 mengirim semua
		 * exception/error yang lolos dari event listener melalui parameter errs.
		 * Sebelumnya daftar tersebut diabaikan, sehingga UI hanya menampilkan
		 * "Unknown exception" tanpa jejak yang dapat dicari administrator.
		 */
		try {
			if (errs != null && !errs.isEmpty()) {
				String info = buatKonteksErrorZk(comp, evt);
				for (Object error : errs) {
					if (error instanceof Throwable) {
						Throwable throwable = (Throwable) error;
						try {
							throwable.printStackTrace(System.err);
						} catch (Throwable abaikanLogKonsol) {
						}
						ErrorAuditUtil.record(throwable, info);
					} else if (error != null) {
						ErrorAuditUtil.record(new RuntimeException(String.valueOf(error)), info);
					}
				}
			}
		} catch (Throwable gagalAudit) {
			// Listener audit tidak boleh menutupi error asli milik ZK.
			try {
				System.err.println("Gagal mencatat error event ZK: " + gagalAudit);
			} catch (Throwable abaikanLogKonsol) {
			}
		} finally {
			super.cleanup(comp, evt, errs);
		}
	}

	/**
	 * Membangun string konteks ringkas untuk sebuah error event ZK, berisi nama event dan
	 * kelas+id komponen terkait (bila tersedia), untuk dilampirkan pada entri audit di
	 * {@link #cleanup}. Seluruh pengambilan informasi dibungkus try-catch best-effort agar
	 * kegagalan membaca detail komponen tidak menggagalkan pencatatan stack trace asli.
	 *
	 * @param comp komponen ZK terkait event, boleh {@code null}
	 * @param evt   event ZK terkait, boleh {@code null}
	 * @return string deskriptif diawali {@code "ZK global event error"}, diikuti nama event dan
	 *         kelas/id komponen bila tersedia
	 */
	private String buatKonteksErrorZk(Component comp, Event evt) {
		StringBuilder info = new StringBuilder("ZK global event error");
		try {
			if (evt != null) {
				info.append(" | event=").append(evt.getName());
			}
			if (comp != null) {
				info.append(" | component=").append(comp.getClass().getName());
				String id = comp.getId();
				if (id != null && !id.trim().isEmpty()) {
					info.append("#").append(id.trim());
				}
			}
		} catch (Throwable abaikanKonteks) {
			// Konteks tambahan bersifat best effort; stack trace asli tetap dicatat.
		}
		return info.toString();
	}

	/**
	 * Hook siklus-hidup ZK yang dipanggil setelah satu event selesai diproses sepenuhnya.
	 * Berisi logika (saat ini tidak aktif — lihat Javadoc kelas) untuk mencatat interaksi
	 * pengguna terhadap komponen {@link Button}, {@link Bandbox}, {@link Combobox},
	 * {@link Textbox}, {@link Doublebox}, {@link Intbox}, {@link Decimalbox}, dan {@link Window}
	 * ke entitas {@link LogUserActifity}, memakai label komponen bertetangga (lewat
	 * {@link #getLabelSebelah}/{@link #cariLabel}) sebagai konteks tambahan. Di akhir, selalu
	 * menutup sesi {@link StreamingHibernateUtil} sebagai pembersihan resource per-siklus-event
	 * terlepas dari status aktif/nonaktifnya pencatatan aktivitas.
	 *
	 * @param comp komponen ZK yang menjadi sumber event
	 * @param evt   event ZK yang baru selesai diproses
	 */
	@Override
	public void complete(Component comp, Event evt) {
		// TODO Auto-generated method stub
		super.complete(comp, evt);
		// // System.out.println("complete => " + comp.getClass().getName() +
		// ", "
		// + evt.getName());

		boolean savingActivity = false;

		// if (System.getProperties().get("savingActivity") != null) {
		// try {
		// savingActivity = Boolean.parseBoolean((String) System
		// .getProperties().get("savingActivity"));
		// } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/ZkossListener.java:76");
		// Common.tampilErrorJikaAdmin(e);
		// }
		// }

		if (savingActivity) {
			String keterangan1 = "";
			String keterangan12 = "";
			String keterangan2 = "";
			String img = "";
			if (comp instanceof Button) {
				Button button = (Button) comp;
				keterangan1 = button.getTooltip() != null ? button.getTooltip().trim() : "";
				keterangan2 = button.getLabel() != null ? button.getLabel().trim() : "";
				img = button.getImage() != null ? button.getImage().trim() : "";

				try {
					DetailLogLogin logLogin = (DetailLogLogin) SessionsCtrl.getCurrent().getAttribute("detailLogLogin");
					// System.out.println("logLogin = " + logLogin);
					if (logLogin != null) {
						LogUserActifity actifity = new LogUserActifity();
						actifity.setDetailLogLogin(logLogin);
						actifity.setImg(img);
						actifity.setKeterangan(keterangan1);
						actifity.setKeterangan12(keterangan12);
						actifity.setKeterangan1(keterangan2);
						actifity.setEvent(evt.getName());

						Session session = ais.database.hibernate.HibernateUtil.currentNativeSession();
						session.getTransaction().begin();
						session.save(actifity);
						session.getTransaction().commit();
						// session.disconnect();
						if (session.isOpen()) {session.disconnect();session.close();}
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/ZkossListener.java:112");
				}
				ais.database.hibernate.HibernateUtil.closeSession();
			} else if (comp instanceof Bandbox) {
				Bandbox textbox = (Bandbox) comp;
				keterangan1 = "Pengguna membuka banbox";

				keterangan12 = getLabelSebelah(comp);
				keterangan2 = textbox.getValue() == null ? "" : textbox.getValue().toString();
				try {
					DetailLogLogin logLogin = (DetailLogLogin) SessionsCtrl.getCurrent().getAttribute("detailLogLogin");
					// System.out.println("logLogin = " + logLogin);
					if (logLogin != null) {
						LogUserActifity actifity = new LogUserActifity();
						actifity.setDetailLogLogin(logLogin);
						actifity.setImg(img);
						actifity.setKeterangan(keterangan1);
						actifity.setKeterangan12(keterangan12);
						actifity.setKeterangan1(keterangan2);
						actifity.setEvent(evt.getName());
						Session session = ais.database.hibernate.HibernateUtil.currentNativeSession();
						session.getTransaction().begin();
						session.save(actifity);
						session.getTransaction().commit();
						// session.disconnect();
						if (session.isOpen()) {session.disconnect();session.close();}
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/ZkossListener.java:140");
				}
				ais.database.hibernate.HibernateUtil.closeSession();
			} else if (comp instanceof Combobox) {
				Combobox combobox = (Combobox) comp;
				keterangan1 = "Ubah combo";

				keterangan12 = getLabelSebelah(comp);
				keterangan2 = combobox.getSelectedItem() == null ? "" : combobox.getSelectedItem().getLabel();
				try {
					DetailLogLogin logLogin = (DetailLogLogin) SessionsCtrl.getCurrent().getAttribute("detailLogLogin");
					// System.out.println("logLogin = " + logLogin);
					if (logLogin != null) {
						LogUserActifity actifity = new LogUserActifity();
						actifity.setDetailLogLogin(logLogin);
						actifity.setImg(img);
						actifity.setKeterangan(keterangan1);
						actifity.setKeterangan12(keterangan12);
						actifity.setKeterangan1(keterangan2);
						actifity.setEvent(evt.getName());
						Session session = ais.database.hibernate.HibernateUtil.currentNativeSession();
						session.getTransaction().begin();
						session.save(actifity);
						session.getTransaction().commit();
						// session.disconnect();
						if (session.isOpen()) {session.disconnect();session.close();}
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/ZkossListener.java:168");
				}
				ais.database.hibernate.HibernateUtil.closeSession();
			} else if (comp instanceof Textbox) {
				Textbox textbox = (Textbox) comp;
				keterangan1 = "Memasukkan nilai ke textbox";

				keterangan12 = getLabelSebelah(comp);
				keterangan2 = textbox.getValue();
				try {
					DetailLogLogin logLogin = (DetailLogLogin) SessionsCtrl.getCurrent().getAttribute("detailLogLogin");
					// System.out.println("logLogin = " + logLogin);
					if (logLogin != null) {
						LogUserActifity actifity = new LogUserActifity();
						actifity.setDetailLogLogin(logLogin);
						actifity.setImg(img);
						actifity.setKeterangan(keterangan1);
						actifity.setKeterangan12(keterangan12);
						actifity.setKeterangan1(keterangan2);
						actifity.setEvent(evt.getName());
						Session session = ais.database.hibernate.HibernateUtil.currentNativeSession();
						session.getTransaction().begin();
						session.save(actifity);
						session.getTransaction().commit();
						// session.disconnect();
						if (session.isOpen()) {session.disconnect();session.close();}
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/ZkossListener.java:196");
				}
				ais.database.hibernate.HibernateUtil.closeSession();
			} else if (comp instanceof Doublebox) {
				Doublebox textbox = (Doublebox) comp;
				keterangan1 = "Memasukkan nilai ke doublebox";

				keterangan12 = getLabelSebelah(comp);
				keterangan2 = textbox.getValue() == null ? "" : textbox.getValue().toString();
				try {
					DetailLogLogin logLogin = (DetailLogLogin) SessionsCtrl.getCurrent().getAttribute("detailLogLogin");
					// System.out.println("logLogin = " + logLogin);
					if (logLogin != null) {
						LogUserActifity actifity = new LogUserActifity();
						actifity.setDetailLogLogin(logLogin);
						actifity.setImg(img);
						actifity.setKeterangan(keterangan1);
						actifity.setKeterangan12(keterangan12);
						actifity.setKeterangan1(keterangan2);
						actifity.setEvent(evt.getName());
						Session session = ais.database.hibernate.HibernateUtil.currentNativeSession();
						session.getTransaction().begin();
						session.save(actifity);
						session.getTransaction().commit();
						// session.disconnect();
						if (session.isOpen()) {session.disconnect();session.close();}
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/ZkossListener.java:224");
				}
				ais.database.hibernate.HibernateUtil.closeSession();
			} else if (comp instanceof Intbox) {
				Intbox textbox = (Intbox) comp;
				keterangan1 = "Memasukkan nilai ke intbox";

				keterangan12 = getLabelSebelah(comp);
				keterangan2 = textbox.getValue() == null ? "" : textbox.getValue().toString();
				try {
					DetailLogLogin logLogin = (DetailLogLogin) SessionsCtrl.getCurrent().getAttribute("detailLogLogin");
					// System.out.println("logLogin = " + logLogin);
					if (logLogin != null) {
						LogUserActifity actifity = new LogUserActifity();
						actifity.setDetailLogLogin(logLogin);
						actifity.setImg(img);
						actifity.setKeterangan(keterangan1);
						actifity.setKeterangan12(keterangan12);
						actifity.setKeterangan1(keterangan2);
						actifity.setEvent(evt.getName());
						Session session = ais.database.hibernate.HibernateUtil.currentNativeSession();
						session.getTransaction().begin();
						session.save(actifity);
						session.getTransaction().commit();
						// session.disconnect();
						if (session.isOpen()) {session.disconnect();session.close();}
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/ZkossListener.java:252");
				}
				ais.database.hibernate.HibernateUtil.closeSession();
			} else if (comp instanceof Decimalbox) {
				Decimalbox textbox = (Decimalbox) comp;
				keterangan1 = "Memasukkan nilai ke decimalbox";

				keterangan12 = getLabelSebelah(comp);
				keterangan2 = textbox.getValue() == null ? "" : textbox.getValue().toString();
				try {
					DetailLogLogin logLogin = (DetailLogLogin) SessionsCtrl.getCurrent().getAttribute("detailLogLogin");
					// System.out.println("logLogin = " + logLogin);
					if (logLogin != null) {
						LogUserActifity actifity = new LogUserActifity();
						actifity.setDetailLogLogin(logLogin);
						actifity.setImg(img);
						actifity.setKeterangan(keterangan1);
						actifity.setKeterangan12(keterangan12);
						actifity.setKeterangan1(keterangan2);
						actifity.setEvent(evt.getName());
						Session session = ais.database.hibernate.HibernateUtil.currentNativeSession();
						session.getTransaction().begin();
						session.save(actifity);
						session.getTransaction().commit();
						// session.disconnect();
						if (session.isOpen()) {session.disconnect();session.close();}
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/ZkossListener.java:280");
				}
				ais.database.hibernate.HibernateUtil.closeSession();
			} else if (comp instanceof Window) {
				Window textbox = (Window) comp;
				keterangan1 = "Pengguna membuka window";
				keterangan2 = textbox.getTitle() == null ? "" : textbox.getTitle().toString();
				keterangan12 = cariLabel(textbox, "");
				if (keterangan12.length() >= 250) {
					keterangan12 = keterangan12.substring(0, 250);
				}
				try {
					DetailLogLogin logLogin = (DetailLogLogin) SessionsCtrl.getCurrent().getAttribute("detailLogLogin");
					// System.out.println("logLogin = " + logLogin);
					if (logLogin != null) {
						LogUserActifity actifity = new LogUserActifity();
						actifity.setDetailLogLogin(logLogin);
						actifity.setImg(img);
						actifity.setKeterangan(keterangan1);
						actifity.setKeterangan12(keterangan12);
						actifity.setKeterangan1(keterangan2);
						actifity.setEvent(evt.getName());
						Session session = ais.database.hibernate.HibernateUtil.currentNativeSession();
						session.getTransaction().begin();
						session.save(actifity);
						session.getTransaction().commit();
						// session.disconnect();
						if (session.isOpen()) {session.disconnect();session.close();}
						ais.database.hibernate.HibernateUtil.closeSession();
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		}

		StreamingHibernateUtil.getInstance().closeSession();
	}

	/**
	 * Menelusuri seluruh keturunan (rekursif, depth-first) sebuah komponen ZK untuk mengumpulkan
	 * nilai setiap {@link Label} yang ditemukan, digabung menjadi satu string dipisah koma dan
	 * dibungkus tanda kurung siku per-label (mis. {@code "[Nama], [Alamat]"}). Dipakai
	 * {@link #complete} untuk membangun deskripsi konten sebuah {@link Window} yang dibuka
	 * pengguna.
	 *
	 * @param component komponen akar yang ditelusuri keturunannya
	 * @param label     akumulator string label yang sudah terkumpul sejauh ini (dipakai sebagai
	 *                  parameter rekursi)
	 * @return string gabungan seluruh label yang ditemukan pada {@code component} dan
	 *         keturunannya
	 */
	@SuppressWarnings("unchecked")
	private String cariLabel(Component component, String label) {
		List<Component> components = component.getChildren();
		for (Component c : components) {
			// // System.out.println("c = " + c.getClass());
			if (c instanceof org.zkoss.zul.Label) {
				label += label.trim().equals("") ? "[" + ((Label) c).getValue() + "]"
						: ", [" + ((Label) c).getValue() + "]";
				// // System.out.println("label = " + label);
			} else {
				label = cariLabel(c, label);
			}
		}

		return label;
	}

	/**
	 * Mencari label deskriptif untuk sebuah komponen input dengan melihat komponen "tetangga" —
	 * saudara-saudaranya dalam satu {@link Row} yang sama (pola tabel/grid form ZK umum di AIS,
	 * mis. label kolom di sel sebelum/sesudah input). Mengumpulkan nilai dari saudara yang berupa
	 * {@link Label}, {@link Textbox}, {@link Doublebox}, {@link Decimalbox}, {@link Intbox}, atau
	 * gambar dari {@link MyButtonConfig}, digabung dipisah koma. Dipakai {@link #complete} untuk
	 * memberi konteks "kolom apa yang diubah pengguna" pada catatan aktivitas.
	 *
	 * @param component komponen input yang dicari label tetangganya; harus berada langsung di
	 *                  dalam sebuah {@link Row} agar pencarian menghasilkan sesuatu
	 * @return string gabungan nilai komponen tetangga dalam {@link Row} yang sama (kosong bila
	 *         parent bukan {@link Row}, atau bila terjadi kegagalan saat penelusuran — kegagalan
	 *         ditangani lewat {@link Common#tampilErrorJikaAdmin(Exception)})
	 */
	private String getLabelSebelah(Component component) {
		String label = "";
		try {
			if (component.getParent() instanceof Row) {
				Row row = (Row) component.getParent();

				int i = 0;
				for (Object o : row.getChildren()) {
					if (component != o) {
						if (o instanceof Label) {
							Label myLabel = (Label) o;
							if (i == 0) {
								label += myLabel.getValue();
							} else {
								label += ", " + myLabel.getValue();
							}
						} else if (o instanceof Textbox) {
							Textbox myLabel = (Textbox) o;
							if (i == 0) {
								label += myLabel.getValue();
							} else {
								label += ", " + myLabel.getValue();
							}
						} else if (o instanceof Doublebox) {
							Doublebox myLabel = (Doublebox) o;
							if (i == 0) {
								label += myLabel.getValue();
							} else {
								label += ", " + myLabel.getValue();
							}
						} else if (o instanceof Decimalbox) {
							Decimalbox myLabel = (Decimalbox) o;
							if (i == 0) {
								label += myLabel.getValue();
							} else {
								label += ", " + myLabel.getValue();
							}
						} else if (o instanceof Intbox) {
							Intbox myLabel = (Intbox) o;
							if (i == 0) {
								label += myLabel.getValue();
							} else {
								label += ", " + myLabel.getValue();
							}
						} else if (o instanceof MyButtonConfig) {
							MyButtonConfig myLabel = (MyButtonConfig) o;
							if (i == 0) {
								label += myLabel.getImage();
							} else {
								label += ", " + myLabel.getImage();
							}
						}
						if (!label.trim().equals("")) {
							i++;
						}
					}

				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return label;
	}

	/**
	 * Hook siklus-hidup ZK yang dipanggil di awal sebelum sebuah event mulai diproses; nilai
	 * kembaliannya menentukan apakah ZK melanjutkan pemrosesan event tersebut. Tidak ada logika
	 * tambahan di kelas ini — hanya meneruskan hasil {@code super.init(comp, evt)}.
	 *
	 * @param comp komponen ZK terkait event
	 * @param evt   event ZK yang akan diproses
	 * @return hasil dari {@code super.init(comp, evt)}
	 */
	@Override
	public boolean init(Component comp, Event evt) {
		// TODO Auto-generated method stub

		// // System.out.println("init => " + comp.getClass().getName() + ", "
		// + evt.getName());
		return super.init(comp, evt);
	}

	/**
	 * Hook siklus-hidup ZK yang dipanggil sesaat sebelum listener aplikasi (event handler
	 * {@code onClick}, {@code onChange}, dsb. yang didaftarkan pada komponen) benar-benar
	 * dijalankan. Tidak ada logika tambahan di kelas ini — hanya meneruskan ke
	 * {@code super.prepare(comp, evt)}.
	 *
	 * @param comp komponen ZK terkait event
	 * @param evt   event ZK yang akan segera diproses listener aplikasinya
	 */
	@Override
	public void prepare(Component comp, Event evt) {
		// TODO Auto-generated method stub
		super.prepare(comp, evt);
		// // System.out.println("prepare => " + comp.getClass().getName() +
		// ", "
		// + evt.getName());
	}

}
