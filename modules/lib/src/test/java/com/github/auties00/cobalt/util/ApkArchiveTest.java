package com.github.auties00.cobalt.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("ApkArchive pure-JDK APK parser")
class ApkArchiveTest {

    @Test
    @DisplayName("parses UTF-8 AXML manifest package, versionName, entries, and v2 signing certificates")
    void parsesUtf8ManifestAndV2Certificates() throws IOException, CertificateException {
        var cert1 = new byte[]{0x30, (byte) 0x82, 0x01, 0x0a, 0x01, 0x02, 0x03};
        var cert2 = new byte[]{0x30, (byte) 0x82, 0x01, 0x0b, 0x04, 0x05};
        var manifest = buildAxmlManifest("com.whatsapp", "2.26.10.75", true);
        var dex = "dex\n035\0".getBytes(StandardCharsets.US_ASCII);
        var rawZip = buildZip(manifest, dex);
        var signedApk = injectApkSigBlockV2(rawZip, List.of(cert1, cert2));

        var archive = new ApkArchive(signedApk);
        var meta = archive.manifestMeta();
        assertEquals("com.whatsapp", meta.packageName());
        assertEquals("2.26.10.75", meta.versionName());
        assertArrayEquals(dex, archive.getEntry("classes.dex"));
        assertNull(archive.getEntry("does/not/exist"));

        var certs = archive.signingCertificates();
        assertEquals(2, certs.length);
        assertArrayEquals(cert1, certs[0]);
        assertArrayEquals(cert2, certs[1]);
    }

    @Test
    @DisplayName("parses UTF-16LE AXML manifest package and versionName")
    void parsesUtf16Manifest() throws IOException {
        var manifest = buildAxmlManifest("com.whatsapp.w4b", "2.26.11.12", false);
        var rawZip = buildZip(manifest, new byte[]{1, 2, 3});

        var archive = new ApkArchive(rawZip);
        var meta = archive.manifestMeta();
        assertEquals("com.whatsapp.w4b", meta.packageName());
        assertEquals("2.26.11.12", meta.versionName());
    }

    private static byte[] buildZip(byte[] manifest, byte[] classesDex) throws IOException {
        var out = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("AndroidManifest.xml"));
            zip.write(manifest);
            zip.closeEntry();

            zip.putNextEntry(new ZipEntry("classes.dex"));
            zip.write(classesDex);
            zip.closeEntry();
        }
        return out.toByteArray();
    }

    private static byte[] buildAxmlManifest(String packageName, String versionName, boolean utf8) throws IOException {
        var strings = List.of("manifest", "package", packageName, "versionName", versionName);

        var strData = new ByteArrayOutputStream();
        var offsets = new int[strings.size()];
        for (var i = 0; i < strings.size(); i++) {
            offsets[i] = strData.size();
            var s = strings.get(i);
            if (utf8) {
                var bytes = s.getBytes(StandardCharsets.UTF_8);
                strData.write(s.length());
                strData.write(bytes.length);
                strData.write(bytes);
                strData.write(0);
            } else {
                var bytes = s.getBytes(StandardCharsets.UTF_16LE);
                strData.write(s.length() & 0xff);
                strData.write((s.length() >>> 8) & 0xff);
                strData.write(bytes);
                strData.write(0);
                strData.write(0);
            }
        }
        while (strData.size() % 4 != 0) {
            strData.write(0);
        }

        var poolHeaderSize = 28;
        var offsetsSize = strings.size() * 4;
        var stringsStart = poolHeaderSize + offsetsSize;
        var poolBytes = strData.toByteArray();
        var poolChunkSize = stringsStart + poolBytes.length;

        var startElemHeaderSize = 16;
        var attrSize = 20;
        var attrCount = 2;
        var startElemChunkSize = startElemHeaderSize + 20 + attrCount * attrSize;

        var totalSize = 8 + poolChunkSize + startElemChunkSize;
        var buf = ByteBuffer.allocate(totalSize).order(ByteOrder.LITTLE_ENDIAN);

        // Root RES_XML_TYPE header
        buf.putShort((short) 0x0003);
        buf.putShort((short) 8);
        buf.putInt(totalSize);

        // String pool chunk
        buf.putShort((short) 0x0001);
        buf.putShort((short) poolHeaderSize);
        buf.putInt(poolChunkSize);
        buf.putInt(strings.size());
        buf.putInt(0);
        buf.putInt(utf8 ? (1 << 8) : 0);
        buf.putInt(stringsStart);
        buf.putInt(0);
        for (var off : offsets) {
            buf.putInt(off);
        }
        buf.put(poolBytes);

        // Start element chunk for <manifest package="..." versionName="...">
        buf.putShort((short) 0x0102);
        buf.putShort((short) startElemHeaderSize);
        buf.putInt(startElemChunkSize);
        buf.putInt(1); // lineNumber
        buf.putInt(-1); // comment
        buf.putInt(-1); // namespace
        buf.putInt(0); // name = "manifest"
        buf.putShort((short) 20); // attributeStart
        buf.putShort((short) attrSize); // attributeSize
        buf.putShort((short) attrCount); // attributeCount
        buf.putShort((short) 0); // idIndex
        buf.putShort((short) 0); // classIndex
        buf.putShort((short) 0); // styleIndex

        // Attribute 0: package
        buf.putInt(-1); // ns
        buf.putInt(1);  // name = "package"
        buf.putInt(2);  // rawValue = packageName
        buf.putShort((short) 8);
        buf.put((byte) 0);
        buf.put((byte) 0x03);
        buf.putInt(2);

        // Attribute 1: versionName
        buf.putInt(-1); // ns
        buf.putInt(3);  // name = "versionName"
        buf.putInt(4);  // rawValue = versionName
        buf.putShort((short) 8);
        buf.put((byte) 0);
        buf.put((byte) 0x03);
        buf.putInt(4);

        return buf.array();
    }

    private static byte[] injectApkSigBlockV2(byte[] zip, List<byte[]> certs) throws IOException {
        var eocdPos = -1;
        var zipBuf = ByteBuffer.wrap(zip).order(ByteOrder.LITTLE_ENDIAN);
        for (var i = zip.length - 22; i >= 0; i--) {
            if (zipBuf.getInt(i) == 0x06054b50) {
                eocdPos = i;
                break;
            }
        }
        var cdOffset = zipBuf.getInt(eocdPos + 16);

        // Build v2 signer value: signers -> signer -> signedData -> [empty digests][certs]
        var certsOut = new ByteArrayOutputStream();
        for (var cert : certs) {
            certsOut.write(intLe(cert.length));
            certsOut.write(cert);
        }
        var certsSeq = certsOut.toByteArray();

        var signedDataOut = new ByteArrayOutputStream();
        signedDataOut.write(intLe(0)); // empty digests
        signedDataOut.write(intLe(certsSeq.length));
        signedDataOut.write(certsSeq);
        var signedData = signedDataOut.toByteArray();

        var signerOut = new ByteArrayOutputStream();
        signerOut.write(intLe(signedData.length));
        signerOut.write(signedData);
        var signer = signerOut.toByteArray();

        var signersOut = new ByteArrayOutputStream();
        signersOut.write(intLe(signer.length));
        signersOut.write(signer);
        var signers = signersOut.toByteArray();

        var v2ValueOut = new ByteArrayOutputStream();
        v2ValueOut.write(intLe(signers.length));
        v2ValueOut.write(signers);
        var v2Value = v2ValueOut.toByteArray();

        var pairLen = 4L + v2Value.length;
        var blockSize = 8L + pairLen + 8L + 16L;
        var sigBlock = ByteBuffer.allocate((int) (8 + blockSize)).order(ByteOrder.LITTLE_ENDIAN);
        sigBlock.putLong(blockSize);
        sigBlock.putLong(pairLen);
        sigBlock.putInt(0x7109871a);
        sigBlock.put(v2Value);
        sigBlock.putLong(blockSize);
        sigBlock.put("APK Sig Block 42".getBytes(StandardCharsets.US_ASCII));
        var sigBytes = sigBlock.array();

        var out = new byte[zip.length + sigBytes.length];
        System.arraycopy(zip, 0, out, 0, cdOffset);
        System.arraycopy(sigBytes, 0, out, cdOffset, sigBytes.length);
        System.arraycopy(zip, cdOffset, out, cdOffset + sigBytes.length, zip.length - cdOffset);

        // Update EOCD central directory offset
        var outBuf = ByteBuffer.wrap(out).order(ByteOrder.LITTLE_ENDIAN);
        outBuf.putInt(eocdPos + sigBytes.length + 16, cdOffset + sigBytes.length);
        return out;
    }

    private static byte[] intLe(int value) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array();
    }
}
