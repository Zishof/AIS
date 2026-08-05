package ais.common;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.Lokasi;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Helper untuk pembuatan kode/barcode otomatis.
 * Counter memakai AtomicLong agar aman dipakai banyak user/thread bersamaan.
 */
public class CommonGenerateHelper {

	private static final AtomicLong AUTO_GENERATOR_ID = new AtomicLong(0L);

	private CommonGenerateHelper() {
	}

	public static String getGeneratedBarCode(int digit) {
		try {
			Long lg = Long.parseLong(getGeneratedAngkaDigit(digit));
			return Long.toHexString(lg.longValue()).toUpperCase();
		} catch (Exception e) {
			return getGeneratedAngkaDigit(digit);
		}
	}

	public static String getGeneratedBarCode() {
		String str = String.valueOf(ais.ui.util.WaktuUtil.getCalendar().getTimeInMillis())
				+ String.valueOf(AUTO_GENERATOR_ID.incrementAndGet());
		try {
			Long lg = Long.parseLong(str);
			return Long.toHexString(lg.longValue()).toUpperCase();
		} catch (Exception e) {
			return str;
		}
	}

	public static String getGeneratedAngkaDigit(int digit) {
		if (digit <= 0) {
			digit = 1;
		}
		if (digit > 30) {
			digit = 30;
		}
		String str = "0000000000000000000000000000000000000000000"
				+ ais.ui.util.WaktuUtil.getCalendar().getTimeInMillis()
				+ String.valueOf(AUTO_GENERATOR_ID.incrementAndGet());
		return str.substring(str.length() - digit);
	}


    public static String generateCode(Class<?> class1, int panjang) {
        return generateCode(class1, panjang, "");
    }

    public static String generateCode(Class<?> class1, int panjang, String awalan) {
        return generateCode(class1, panjang, awalan, null);
    }

    public static String generateCode(Class<?> class1, int panjang, String awalan, Lokasi lokasi) {
        return generateCode(class1, panjang, awalan, lokasi, Long.valueOf(1L));
    }

    public static String generateCode(Class<?> class1, int panjang, String awalan, Lokasi lokasi, Long penambahan) {
        if (class1 == null) {
            return "";
        }
        if (panjang <= 0) {
            panjang = 1;
        }
        if (penambahan == null || penambahan.longValue() < 1L) {
            penambahan = Long.valueOf(1L);
        }

        long tambahan = penambahan.longValue();
        for (int i = 0; i < 50; i++) {
            Long max;
            if (lokasi == null) {
                max = (Long) HibernateUtil.currentSession().createCriteria(class1).setProjection(Projections.max("id"))
                        .uniqueResult();
            } else {
                max = generateMaxByLokasi(class1, lokasi);
            }
            if (max == null) {
                max = Long.valueOf(0L);
            }
            String kodeAngka = "00000000000000000000000000000" + (tambahan + max.longValue());
            String mykode = (awalan == null || awalan.trim().length() == 0 ? "" : awalan + "-")
                    + (lokasi == null || lokasi.getKode() == null || lokasi.getKode().trim().length() == 0 ? ""
                            : lokasi.getKode() + "-")
                    + kodeAngka.substring(kodeAngka.length() - panjang, kodeAngka.length());
            if (!isKodeExists(class1, mykode)) {
                return mykode;
            }
            tambahan++;
        }
        String kodeAngka = "00000000000000000000000000000" + System.currentTimeMillis();
        return (awalan == null || awalan.trim().length() == 0 ? "" : awalan + "-")
                + kodeAngka.substring(kodeAngka.length() - panjang, kodeAngka.length());
    }

    public static Long generateMaxByLokasi(Class<?> class1, Lokasi lokasi) {
        Session session = null;
        try {
            session = HibernateUtil.currentNativeSession();
            String strmax = (String) session.createCriteria(class1)
                    .add(lokasi == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("lokasi", lokasi))
                    .setProjection(Projections.max("kode")).uniqueResult();
            if (strmax == null) {
                strmax = "0";
            }
            String str;
            if (strmax.contains("-")) {
                String[] strSplit = strmax.trim().split("-");
                str = strSplit.length > 0 ? strSplit[strSplit.length - 1] : "0";
            } else {
                str = strmax;
            }
            try {
                return Long.valueOf(Long.parseLong(str.trim()));
            } catch (NumberFormatException e) {
                Common.tampilErrorJikaAdmin(e);
                return Long.valueOf(0L);
            }
        } finally {
            Common.closeNativeSessionQuietly(session);
        }
    }

    private static boolean isKodeExists(Class<?> class1, String kode) {
        Session session = null;
        try {
            session = HibernateUtil.currentNativeSession();
            Number count = (Number) session.createCriteria(class1).add(Restrictions.eq("kode", kode))
                    .setProjection(Projections.rowCount()).uniqueResult();
            return count != null && count.intValue() > 0;
        } finally {
            Common.closeNativeSessionQuietly(session);
        }
    }

}
