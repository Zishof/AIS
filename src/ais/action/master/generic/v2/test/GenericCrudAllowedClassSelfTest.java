package ais.action.master.generic.v2.test;

import java.lang.reflect.Method;

import ais.action.master.generic.v2.GenericCrudAutoDefinitionFactory;

/**
 * Verifikasi allow-list kelas pada {@code GenericCrudAutoDefinitionFactory}.
 *
 * <p>Token pemblokir dicocokkan sebagai substring pada nama kelas, dan itu
 * disengaja supaya gagal ke sisi aman. Harganya sejumlah layar ikut terkunci
 * read-only tanpa alasan: {@code BankSoal} tertangkap token {@code bank}
 * padahal ia bank soal ujian, bukan rekening — sehingga menu Bank Soal pada
 * Sistem Informasi Akademik hanya bisa dibaca sementara layar ZK lamanya bisa
 * disunting penuh.</p>
 *
 * <p>Uji ini menjaga dua arah sekaligus, sebab yang berbahaya bukan hanya
 * kuncinya salah pasang melainkan juga kuncinya terlepas: kelas yang sudah
 * dibuka harus tetap terbuka, dan kelas yang memang sensitif harus tetap
 * terkunci. Pemeriksaannya memakai refleksi supaya tidak memerlukan basis data
 * — seluruh keputusan ini murni soal nama kelas.</p>
 */
@SuppressWarnings("rawtypes")
public final class GenericCrudAllowedClassSelfTest {
    private GenericCrudAllowedClassSelfTest() { }

    public static void main(String[] args) throws Exception {
        Method blocked = GenericCrudAutoDefinitionFactory.class
                .getDeclaredMethod("isBlockedClass", new Class[] { Class.class });
        blocked.setAccessible(true);

        // Dibuka: bank soal ujian, bukan rekening.
        String[] terbuka = {
            "ais.database.model.BankSoal",
            "ais.database.model.BankSoalDetail",
            "ais.database.model.KategoriBankSoal",
            "ais.database.model.PenjelasanBankSoal",
        };
        for (int i = 0; i < terbuka.length; i++) {
            Class type = muat(terbuka[i]);
            if (type == null) continue; // kelas tidak ada pada instalasi ini
            check(!isBlocked(blocked, type),
                    "Kelas " + terbuka[i] + " seharusnya tidak terkunci read-only.");
        }

        // Tetap terkunci: benar-benar sensitif, dan tidak boleh ikut terbuka.
        String[] terkunci = {
            "ais.database.model.Tbmuser",
            "ais.database.model.Roles",
            "ais.database.model.Menu",
            "ais.database.model.LogLogin",
        };
        for (int i = 0; i < terkunci.length; i++) {
            Class type = muat(terkunci[i]);
            if (type == null) continue;
            check(isBlocked(blocked, type),
                    "Kelas " + terkunci[i] + " harus tetap terkunci read-only.");
        }

        // Kelas yang namanya memuat "bank" tetapi bukan bank soal tetap terkunci;
        // allow-list bekerja pada nama utuh, bukan melonggarkan pencocokan.
        Class bank = muat("ais.database.model.Bank");
        if (bank != null) {
            check(isBlocked(blocked, bank),
                    "Entitas Bank harus tetap terkunci; allow-list tidak boleh melonggarkan token.");
        }

        System.out.println("PASS Generic CRUD allowed-class self-test");
    }

    private static boolean isBlocked(Method method, Class type) throws Exception {
        return ((Boolean) method.invoke(null, new Object[] { type })).booleanValue();
    }

    private static Class muat(String name) {
        try {
            return Class.forName(name);
        } catch (Throwable e) {
            return null;
        }
    }

    private static void check(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }
}
