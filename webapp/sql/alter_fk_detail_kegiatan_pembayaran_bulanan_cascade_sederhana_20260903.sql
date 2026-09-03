ALTER TABLE detail_kegiatan
DROP CONSTRAINT fk59bea98af381abdb,
ADD CONSTRAINT fk59bea98af381abdb
    FOREIGN KEY (pengaturan_pembayaran_bulanan)
    REFERENCES pengaturan_pembayaran_bulanan (id)
    ON DELETE CASCADE;
