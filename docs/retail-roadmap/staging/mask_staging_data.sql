BEGIN;

-- Jalankan hanya pada database staging. Guard ini sengaja menggagalkan database
-- yang namanya tidak mengandung staging/test agar produksi tidak termasking.
DO $$
BEGIN
  IF current_database() !~* '(staging|test)' THEN
    RAISE EXCEPTION 'Masking ditolak: database % bukan staging/test', current_database();
  END IF;
END $$;

-- Kolom berbeda antar instalasi; blok dinamis hanya mengubah kolom yang ada.
DO $$
DECLARE r record;
BEGIN
  FOR r IN
    SELECT table_schema, table_name, column_name
      FROM information_schema.columns
     WHERE table_schema IN ('public', 'akademik', 'koperasi')
       AND lower(column_name) IN ('email','emailayah','emailibu')
  LOOP
    EXECUTE format('UPDATE %I.%I SET %I = CASE WHEN %I IS NULL OR btrim(%I::text) = '''' THEN %I ELSE ''m-'' || substr(md5(ctid::text),1,12) || ''@x.invalid'' END',
      r.table_schema, r.table_name, r.column_name, r.column_name, r.column_name, r.column_name);
  END LOOP;
END $$;

DO $$
DECLARE r record;
BEGIN
  FOR r IN
    SELECT table_schema, table_name, column_name
      FROM information_schema.columns
     WHERE table_schema IN ('public', 'akademik', 'koperasi')
       AND lower(column_name) IN ('nohp','no_hp','telepon','telp','handphone')
  LOOP
    EXECUTE format('UPDATE %I.%I SET %I = CASE WHEN %I IS NULL OR btrim(%I::text) = '''' THEN %I ELSE ''0800'' || substr(md5(ctid::text),1,8) END',
      r.table_schema, r.table_name, r.column_name, r.column_name, r.column_name, r.column_name);
  END LOOP;
END $$;

-- Token/PIN/password tidak boleh dapat dipakai kembali. Tipe kolom dan trigger
-- lama beragam, sehingga hanya kolom teks yang ditimpa secara aman.
DO $$
DECLARE r record;
BEGIN
  FOR r IN
    SELECT table_schema, table_name, column_name
      FROM information_schema.columns
     WHERE table_schema IN ('public', 'akademik', 'koperasi')
       AND data_type IN ('character varying','character','text')
       AND lower(column_name) IN ('token','access_token','refresh_token','pin','password','passwd')
  LOOP
    EXECUTE format('UPDATE %I.%I SET %I = md5(random()::text || clock_timestamp()::text || ctid::text)',
      r.table_schema, r.table_name, r.column_name);
  END LOOP;
END $$;

COMMIT;

