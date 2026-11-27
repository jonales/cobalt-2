package com.github.auties00.cobalt.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Pure-JDK reader for Android APK archives stored in memory.
 *
 * <p>Extracts ZIP entries, parses the binary {@code AndroidManifest.xml} ({@code AXML}) header to read the
 * {@code package} and {@code versionName} attributes, and extracts X.509 signing certificates from the APK
 * Signature Scheme v2/v3/v3.1 block or, as a fallback, from JAR v1 {@code META-INF/*.RSA/*.DSA/*.EC}
 * signature blocks.
 */
public final class ApkArchive {
    /**
     * ZIP End of Central Directory record signature.
     */
    private static final int ZIP_EOCD_SIGNATURE = 0x06054b50;

    /**
     * Minimum length of a ZIP End of Central Directory record in bytes.
     */
    private static final int ZIP_EOCD_MIN_SIZE = 22;

    /**
     * Maximum length of a ZIP comment in bytes.
     */
    private static final int ZIP_MAX_COMMENT_LENGTH = 65535;

    /**
     * 16-byte trailing magic identifying an APK Signing Block.
     */
    private static final byte[] APK_SIG_BLOCK_MAGIC = "APK Sig Block 42".getBytes(StandardCharsets.US_ASCII);

    /**
     * APK Signing Block key ID for APK Signature Scheme v2.
     */
    private static final int APK_SIG_SCHEME_V2_ID = 0x7109871a;

    /**
     * APK Signing Block key ID for APK Signature Scheme v3.
     */
    private static final int APK_SIG_SCHEME_V3_ID = 0xf05368c0;

    /**
     * APK Signing Block key ID for APK Signature Scheme v3.1.
     */
    private static final int APK_SIG_SCHEME_V31_ID = 0x1b93ad61;

    /**
     * Binary Android XML chunk type for the root XML document.
     */
    private static final int RES_XML_TYPE = 0x0003;

    /**
     * Binary Android XML chunk type for the string pool.
     */
    private static final int RES_STRING_POOL_TYPE = 0x0001;

    /**
     * Binary Android XML chunk type for an element start tag.
     */
    private static final int RES_XML_START_ELEMENT_TYPE = 0x0102;

    /**
     * String pool header flag indicating UTF-8 encoded entries instead of UTF-16LE.
     */
    private static final int STRING_POOL_UTF8_FLAG = 1 << 8;

    /**
     * Android {@code Res_value} data type constant for string values.
     */
    private static final int RES_VALUE_TYPE_STRING = 0x03;

    /**
     * Raw APK archive bytes retained for random-access APK Signing Block parsing.
     */
    private final byte[] rawBytes;

    /**
     * Extracted ZIP entries keyed by entry name.
     */
    private final Map<String, byte[]> entries;

    /**
     * Parsed manifest metadata, populated lazily on first access.
     */
    private ManifestMeta manifestMeta;

    /**
     * Parsed {@code AndroidManifest.xml} metadata needed by Cobalt.
     *
     * @param packageName the {@code package} attribute of the {@code <manifest>} element, or {@code null}
     * @param versionName the {@code android:versionName} attribute of the {@code <manifest>} element, or {@code null}
     */
    public record ManifestMeta(String packageName, String versionName) {
    }

    /**
     * Parses an in-memory APK archive.
     *
     * @param apkBytes the raw APK file bytes
     * @throws IOException          if the ZIP stream is malformed
     * @throws NullPointerException if {@code apkBytes} is {@code null}
     */
    public ApkArchive(byte[] apkBytes) throws IOException {
        this.rawBytes = Objects.requireNonNull(apkBytes, "apkBytes");
        this.entries = readZipEntries(apkBytes);
    }

    /**
     * Reads all non-directory ZIP entries from the given archive bytes.
     *
     * @param apkBytes the raw APK bytes
     * @return map of entry paths to their uncompressed bytes
     * @throws IOException if the ZIP stream cannot be read
     */
    private static Map<String, byte[]> readZipEntries(byte[] apkBytes) throws IOException {
        var map = new HashMap<String, byte[]>();
        try (var zip = new ZipInputStream(new ByteArrayInputStream(apkBytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    map.put(entry.getName(), zip.readAllBytes());
                }
            }
        }
        return map;
    }

    /**
     * Returns the uncompressed bytes of the named ZIP entry, or {@code null} if absent.
     *
     * @param path the archive entry path
     * @return the entry bytes, or {@code null} when not present
     */
    public byte[] getEntry(String path) {
        return entries.get(path);
    }

    /**
     * Parses and returns the {@code package} and {@code versionName} attributes from {@code AndroidManifest.xml}.
     *
     * @return the parsed manifest metadata
     * @throws IOException if {@code AndroidManifest.xml} is missing or malformed
     */
    public ManifestMeta manifestMeta() throws IOException {
        if (manifestMeta == null) {
            var manifestBytes = entries.get("AndroidManifest.xml");
            if (manifestBytes == null) {
                throw new IOException("APK missing AndroidManifest.xml");
            }
            manifestMeta = parseManifest(manifestBytes);
        }
        return manifestMeta;
    }

    /**
     * Extracts the DER-encoded X.509 signing certificates from the APK.
     *
     * <p>Checks the APK Signature Scheme v2/v3/v3.1 Signing Block first and falls back to JAR v1 signature
     * blocks under {@code META-INF/} when no v2/v3 certificates are present.
     *
     * @return a two-dimensional array of X.509 DER certificate bytes
     * @throws IOException          if the archive structure is malformed
     * @throws CertificateException if a v1 certificate block cannot be decoded
     */
    public byte[][] signingCertificates() throws IOException, CertificateException {
        var v2Certs = extractApkSigBlockCertificates();
        if (!v2Certs.isEmpty()) {
            return v2Certs.toArray(byte[][]::new);
        }
        var v1Certs = extractJarV1Certificates();
        return v1Certs.toArray(byte[][]::new);
    }

    /**
     * Extracts X.509 DER certificates from an APK Signature Scheme v2, v3, or v3.1 block if present.
     *
     * @return extracted DER certificates, or an empty list if no APK Signing Block is present
     */
    private List<byte[]> extractApkSigBlockCertificates() {
        var cdOffset = findCentralDirectoryOffset(rawBytes);
        if (cdOffset < 32 || cdOffset > rawBytes.length) {
            return List.of();
        }
        var magicStart = cdOffset - APK_SIG_BLOCK_MAGIC.length;
        if (!Arrays.equals(rawBytes, magicStart, cdOffset, APK_SIG_BLOCK_MAGIC, 0, APK_SIG_BLOCK_MAGIC.length)) {
            return List.of();
        }
        var buffer = ByteBuffer.wrap(rawBytes).order(ByteOrder.LITTLE_ENDIAN);
        var footerSize = buffer.getLong(magicStart - 8);
        if (footerSize < 24 || footerSize > cdOffset - 8) {
            return List.of();
        }
        var blockStart = (int) (cdOffset - footerSize - 8);
        var headerSize = buffer.getLong(blockStart);
        if (headerSize != footerSize) {
            return List.of();
        }

        var v2Certs = new ArrayList<byte[]>();
        var v3Certs = new ArrayList<byte[]>();
        var pos = blockStart + 8;
        var limit = magicStart - 8;
        while (pos + 12 <= limit) {
            var pairLen = buffer.getLong(pos);
            pos += 8;
            if (pairLen < 4 || pos + pairLen > limit) {
                break;
            }
            var id = buffer.getInt(pos);
            var valueStart = pos + 4;
            var valueLen = (int) (pairLen - 4);
            if (id == APK_SIG_SCHEME_V2_ID) {
                parseSchemeSigners(buffer, valueStart, valueLen, v2Certs);
            } else if (id == APK_SIG_SCHEME_V3_ID || id == APK_SIG_SCHEME_V31_ID) {
                parseSchemeSigners(buffer, valueStart, valueLen, v3Certs);
            }
            pos += (int) pairLen;
        }
        return !v2Certs.isEmpty() ? v2Certs : v3Certs;
    }

    /**
     * Parses a length-prefixed APK Signature Scheme v2/v3 signer sequence and appends each signer's certificates.
     *
     * @param buffer     little-endian view of the raw APK bytes
     * @param valueStart start offset of the scheme value block
     * @param valueLen   length of the scheme value block
     * @param target     list receiving extracted DER certificates
     */
    private static void parseSchemeSigners(ByteBuffer buffer, int valueStart, int valueLen, List<byte[]> target) {
        var valueEnd = valueStart + valueLen;
        if (valueStart + 4 > valueEnd) {
            return;
        }
        var signersLen = buffer.getInt(valueStart);
        var pos = valueStart + 4;
        var signersEnd = Math.min(pos + signersLen, valueEnd);
        while (pos + 4 <= signersEnd) {
            var signerLen = buffer.getInt(pos);
            pos += 4;
            if (signerLen < 4 || pos + signerLen > signersEnd) {
                break;
            }
            var signerEnd = pos + signerLen;
            // First length-prefixed field in a signer is signedData.
            var signedDataLen = buffer.getInt(pos);
            var signedDataPos = pos + 4;
            var signedDataEnd = Math.min(signedDataPos + signedDataLen, signerEnd);
            if (signedDataLen >= 8 && signedDataPos + 4 <= signedDataEnd) {
                // Inside signedData: [digests][certificates][additionalAttributes]...
                var digestsLen = buffer.getInt(signedDataPos);
                var certsLenPos = signedDataPos + 4 + digestsLen;
                if (digestsLen >= 0 && certsLenPos + 4 <= signedDataEnd) {
                    var certsLen = buffer.getInt(certsLenPos);
                    var certPos = certsLenPos + 4;
                    var certsEnd = Math.min(certPos + certsLen, signedDataEnd);
                    while (certPos + 4 <= certsEnd) {
                        var certLen = buffer.getInt(certPos);
                        certPos += 4;
                        if (certLen <= 0 || certPos + certLen > certsEnd) {
                            break;
                        }
                        var certBytes = new byte[certLen];
                        buffer.get(certPos, certBytes);
                        target.add(certBytes);
                        certPos += certLen;
                    }
                }
            }
            pos = signerEnd;
        }
    }

    /**
     * Extracts X.509 DER certificates from JAR v1 signature block files under {@code META-INF/}.
     *
     * @return extracted DER certificates
     * @throws CertificateException if a PKCS#7 certificate block cannot be parsed
     */
    private List<byte[]> extractJarV1Certificates() throws CertificateException {
        var result = new ArrayList<byte[]>();
        var certFactory = CertificateFactory.getInstance("X.509");
        for (var entry : entries.entrySet()) {
            var upper = entry.getKey().toUpperCase(Locale.ROOT);
            if (!upper.startsWith("META-INF/") || upper.indexOf('/', 9) >= 0) {
                continue;
            }
            if (!upper.endsWith(".RSA") && !upper.endsWith(".DSA") && !upper.endsWith(".EC")) {
                continue;
            }
            var certs = certFactory.generateCertificates(new ByteArrayInputStream(entry.getValue()));
            for (Certificate cert : certs) {
                result.add(cert.getEncoded());
            }
        }
        return result;
    }

    /**
     * Locates the offset of the ZIP Central Directory from the End of Central Directory record.
     *
     * @param bytes raw ZIP bytes
     * @return Central Directory byte offset, or {@code -1} if EOCD is not found
     */
    private static int findCentralDirectoryOffset(byte[] bytes) {
        if (bytes.length < ZIP_EOCD_MIN_SIZE) {
            return -1;
        }
        var buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        var minOffset = Math.max(0, bytes.length - ZIP_EOCD_MIN_SIZE - ZIP_MAX_COMMENT_LENGTH);
        for (var offset = bytes.length - ZIP_EOCD_MIN_SIZE; offset >= minOffset; offset--) {
            if (buffer.getInt(offset) == ZIP_EOCD_SIGNATURE) {
                var commentLen = Short.toUnsignedInt(buffer.getShort(offset + 20));
                if (offset + ZIP_EOCD_MIN_SIZE + commentLen == bytes.length) {
                    return buffer.getInt(offset + 16);
                }
            }
        }
        return -1;
    }

    /**
     * Parses a binary Android XML ({@code AXML}) manifest to extract the {@code package} and {@code versionName}
     * attributes from the root {@code <manifest>} element.
     *
     * @param xmlBytes the binary {@code AndroidManifest.xml} bytes
     * @return the parsed manifest metadata
     * @throws IOException if the binary XML header or chunks are truncated or invalid
     */
    static ManifestMeta parseManifest(byte[] xmlBytes) throws IOException {
        if (xmlBytes.length < 8) {
            throw new IOException("AndroidManifest.xml is too short");
        }
        var buf = ByteBuffer.wrap(xmlBytes).order(ByteOrder.LITTLE_ENDIAN);
        var rootType = Short.toUnsignedInt(buf.getShort(0));
        if (rootType != RES_XML_TYPE) {
            throw new IOException("Unsupported AndroidManifest.xml chunk type: 0x" + Integer.toHexString(rootType));
        }
        var headerSize = Short.toUnsignedInt(buf.getShort(2));
        var totalSize = Math.min(buf.getInt(4), xmlBytes.length);

        String[] stringPool = null;
        var pos = headerSize;
        while (pos + 8 <= totalSize) {
            var chunkType = Short.toUnsignedInt(buf.getShort(pos));
            var chunkHeaderSize = Short.toUnsignedInt(buf.getShort(pos + 2));
            var chunkSize = buf.getInt(pos + 4);
            if (chunkSize < 8 || pos + chunkSize > totalSize) {
                break;
            }
            if (chunkType == RES_STRING_POOL_TYPE) {
                stringPool = parseStringPool(buf, pos, chunkHeaderSize, chunkSize);
            } else if (chunkType == RES_XML_START_ELEMENT_TYPE && stringPool != null) {
                var meta = parseManifestStartElement(buf, pos, chunkHeaderSize, chunkSize, stringPool);
                if (meta != null) {
                    return meta;
                }
            }
            pos += chunkSize;
        }
        throw new IOException("AndroidManifest.xml missing <manifest> element");
    }

    /**
     * Decodes a {@code ResStringPool} chunk in either UTF-8 or UTF-16LE format.
     *
     * @param buf             little-endian buffer over the AXML bytes
     * @param chunkStart      byte offset of the chunk header
     * @param chunkHeaderSize header size in bytes
     * @param chunkSize       total chunk size in bytes
     * @return array of decoded strings
     * @throws IOException if the string pool is malformed
     */
    private static String[] parseStringPool(ByteBuffer buf, int chunkStart, int chunkHeaderSize, int chunkSize) throws IOException {
        if (chunkHeaderSize < 28 || chunkStart + 28 > buf.limit()) {
            throw new IOException("Truncated AXML string pool header");
        }
        var stringCount = buf.getInt(chunkStart + 8);
        var flags = buf.getInt(chunkStart + 16);
        var stringsStart = buf.getInt(chunkStart + 20);
        var utf8 = (flags & STRING_POOL_UTF8_FLAG) != 0;

        var offsetsStart = chunkStart + chunkHeaderSize;
        var dataStart = chunkStart + stringsStart;
        var chunkEnd = chunkStart + chunkSize;
        var strings = new String[stringCount];

        for (var i = 0; i < stringCount; i++) {
            var relOffset = buf.getInt(offsetsStart + i * 4);
            var strPos = dataStart + relOffset;
            if (strPos < dataStart || strPos >= chunkEnd) {
                strings[i] = "";
                continue;
            }
            strings[i] = utf8 ? readUtf8String(buf, strPos, chunkEnd) : readUtf16String(buf, strPos, chunkEnd);
        }
        return strings;
    }

    /**
     * Reads one UTF-8 entry from an AXML string pool.
     *
     * @param buf   little-endian buffer
     * @param pos   entry start offset
     * @param limit chunk upper bound
     * @return the decoded string
     */
    private static String readUtf8String(ByteBuffer buf, int pos, int limit) {
        // First varint: UTF-16 character length (skipped).
        var p = skipUtf8Length(buf, pos);
        // Second varint: UTF-8 byte length.
        var first = Byte.toUnsignedInt(buf.get(p++));
        int byteLen;
        if ((first & 0x80) != 0) {
            byteLen = ((first & 0x7f) << 8) | Byte.toUnsignedInt(buf.get(p++));
        } else {
            byteLen = first;
        }
        var clamped = Math.max(0, Math.min(byteLen, limit - p));
        var data = new byte[clamped];
        buf.get(p, data);
        return new String(data, StandardCharsets.UTF_8);
    }

    /**
     * Advances past a 1- or 2-byte AXML UTF-8 length prefix.
     *
     * @param buf buffer to read from
     * @param pos start offset
     * @return offset immediately after the length prefix
     */
    private static int skipUtf8Length(ByteBuffer buf, int pos) {
        return (Byte.toUnsignedInt(buf.get(pos)) & 0x80) != 0 ? pos + 2 : pos + 1;
    }

    /**
     * Reads one UTF-16LE entry from an AXML string pool.
     *
     * @param buf   little-endian buffer
     * @param pos   entry start offset
     * @param limit chunk upper bound
     * @return the decoded string
     */
    private static String readUtf16String(ByteBuffer buf, int pos, int limit) {
        var first = Short.toUnsignedInt(buf.getShort(pos));
        var p = pos + 2;
        int charLen;
        if ((first & 0x8000) != 0) {
            charLen = ((first & 0x7fff) << 16) | Short.toUnsignedInt(buf.getShort(p));
            p += 2;
        } else {
            charLen = first;
        }
        var byteLen = Math.max(0, Math.min(charLen * 2, limit - p));
        var data = new byte[byteLen];
        buf.get(p, data);
        return new String(data, StandardCharsets.UTF_16LE);
    }

    /**
     * Inspects an AXML start-element chunk and returns {@link ManifestMeta} if it is the {@code <manifest>} tag.
     *
     * @param buf             little-endian buffer
     * @param chunkStart      byte offset of the chunk header
     * @param chunkHeaderSize header size in bytes
     * @param chunkSize       total chunk size in bytes
     * @param stringPool      decoded AXML string pool
     * @return parsed metadata for {@code <manifest>}, or {@code null} for any other element
     */
    private static ManifestMeta parseManifestStartElement(
            ByteBuffer buf,
            int chunkStart,
            int chunkHeaderSize,
            int chunkSize,
            String[] stringPool
    ) {
        var extStart = chunkStart + chunkHeaderSize;
        if (extStart + 20 > chunkStart + chunkSize) {
            return null;
        }
        var nameIdx = buf.getInt(extStart + 4);
        var elementName = stringAt(stringPool, nameIdx);
        if (!"manifest".equals(elementName)) {
            return null;
        }

        var attrStart = Short.toUnsignedInt(buf.getShort(extStart + 8));
        var attrSize = Short.toUnsignedInt(buf.getShort(extStart + 10));
        var attrCount = Short.toUnsignedInt(buf.getShort(extStart + 12));
        if (attrSize < 20) {
            return new ManifestMeta(null, null);
        }

        String packageName = null;
        String versionName = null;
        var base = extStart + attrStart;
        var chunkEnd = chunkStart + chunkSize;
        for (var i = 0; i < attrCount; i++) {
            var attrPos = base + i * attrSize;
            if (attrPos + 20 > chunkEnd) {
                break;
            }
            var attrName = stringAt(stringPool, buf.getInt(attrPos + 4));
            var rawValueIdx = buf.getInt(attrPos + 8);
            var valueType = Byte.toUnsignedInt(buf.get(attrPos + 15));
            var valueData = buf.getInt(attrPos + 16);

            String value = null;
            if (rawValueIdx >= 0 && rawValueIdx < stringPool.length) {
                value = stringPool[rawValueIdx];
            } else if (valueType == RES_VALUE_TYPE_STRING && valueData >= 0 && valueData < stringPool.length) {
                value = stringPool[valueData];
            }

            if ("package".equals(attrName)) {
                packageName = value;
            } else if ("versionName".equals(attrName)) {
                versionName = value;
            }
        }
        return new ManifestMeta(packageName, versionName);
    }

    /**
     * Safely resolves an index in the AXML string pool.
     *
     * @param pool  decoded string pool
     * @param index pool index (may be {@code -1})
     * @return the string at {@code index}, or {@code null} when out of bounds
     */
    private static String stringAt(String[] pool, int index) {
        return index >= 0 && index < pool.length ? pool[index] : null;
    }
}
