// RFC 6238 TOTP (SHA-1, 6 digits, 30 s) in plain JS for Maestro's `runScript` (GraalJS, no
// require/crypto available). Input: env TOTP_SECRET (base32). Output: output.totp.
//
//   - copyTextFrom:
//       id: profile_2fa_secret_text
//   - runScript:
//       file: ../../scripts/totp.js
//       env:
//         TOTP_SECRET: ${maestro.copiedText}
//   - inputText: ${output.totp}

function base32ToBytes(input) {
    var alphabet = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ234567';
    var clean = String(input).toUpperCase().replace(/[^A-Z2-7]/g, '');
    var bits = 0, value = 0, out = [];
    for (var i = 0; i < clean.length; i++) {
        value = (value << 5) | alphabet.indexOf(clean.charAt(i));
        bits += 5;
        if (bits >= 8) {
            out.push((value >>> (bits - 8)) & 0xff);
            bits -= 8;
        }
    }
    return out;
}

function sha1(bytes) {
    function rotl(n, s) { return (n << s) | (n >>> (32 - s)); }
    var h0 = 0x67452301, h1 = 0xefcdab89, h2 = 0x98badcfe, h3 = 0x10325476, h4 = 0xc3d2e1f0;
    var msg = bytes.slice();
    var bitLen = bytes.length * 8;
    msg.push(0x80);
    while (msg.length % 64 !== 56) msg.push(0);
    var hi = Math.floor(bitLen / 0x100000000), lo = bitLen >>> 0;
    msg.push((hi >>> 24) & 0xff, (hi >>> 16) & 0xff, (hi >>> 8) & 0xff, hi & 0xff);
    msg.push((lo >>> 24) & 0xff, (lo >>> 16) & 0xff, (lo >>> 8) & 0xff, lo & 0xff);
    var w = new Array(80);
    for (var off = 0; off < msg.length; off += 64) {
        for (var i = 0; i < 16; i++) {
            w[i] = (msg[off + 4 * i] << 24) | (msg[off + 4 * i + 1] << 16) | (msg[off + 4 * i + 2] << 8) | msg[off + 4 * i + 3];
        }
        for (i = 16; i < 80; i++) w[i] = rotl(w[i - 3] ^ w[i - 8] ^ w[i - 14] ^ w[i - 16], 1);
        var a = h0, b = h1, c = h2, d = h3, e = h4;
        for (i = 0; i < 80; i++) {
            var f, k;
            if (i < 20) { f = (b & c) | (~b & d); k = 0x5a827999; }
            else if (i < 40) { f = b ^ c ^ d; k = 0x6ed9eba1; }
            else if (i < 60) { f = (b & c) | (b & d) | (c & d); k = 0x8f1bbcdc; }
            else { f = b ^ c ^ d; k = 0xca62c1d6; }
            var t = (rotl(a, 5) + f + e + k + w[i]) | 0;
            e = d; d = c; c = rotl(b, 30); b = a; a = t;
        }
        h0 = (h0 + a) | 0; h1 = (h1 + b) | 0; h2 = (h2 + c) | 0; h3 = (h3 + d) | 0; h4 = (h4 + e) | 0;
    }
    var out = [];
    [h0, h1, h2, h3, h4].forEach(function (h) {
        out.push((h >>> 24) & 0xff, (h >>> 16) & 0xff, (h >>> 8) & 0xff, h & 0xff);
    });
    return out;
}

function hmacSha1(key, message) {
    if (key.length > 64) key = sha1(key);
    while (key.length < 64) key.push(0);
    var ipad = [], opad = [];
    for (var i = 0; i < 64; i++) { ipad.push(key[i] ^ 0x36); opad.push(key[i] ^ 0x5c); }
    return sha1(opad.concat(sha1(ipad.concat(message))));
}

function totp(secret) {
    var counter = Math.floor(Date.now() / 1000 / 30);
    var msg = [];
    for (var i = 7; i >= 0; i--) { msg[i] = counter & 0xff; counter = Math.floor(counter / 256); }
    var hash = hmacSha1(base32ToBytes(secret), msg);
    var offset = hash[hash.length - 1] & 0x0f;
    var code = ((hash[offset] & 0x7f) << 24) | (hash[offset + 1] << 16) | (hash[offset + 2] << 8) | hash[offset + 3];
    var str = String(code % 1000000);
    while (str.length < 6) str = '0' + str;
    return str;
}

output.totp = totp(TOTP_SECRET);
