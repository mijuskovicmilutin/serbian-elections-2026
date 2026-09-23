package rs.serbianelection2026.backend.survey.service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;
import rs.serbianelection2026.backend.common.exception.ServiceUnavailableException;

/**
 * Turns the network of a visitor into a keyed hash: {@code HMAC-SHA256(key, surveyId | network)}, where the network
 * is the IPv4 address or the /64 prefix of an IPv6 address. Without the key the hash cannot be reversed or
 * recomputed, and the address itself is never written anywhere.
 */
@Component
public class NetworkHasher {

    private static final String ALGORITHM = "HmacSHA256";

    private final SurveyProperties properties;

    public NetworkHasher(SurveyProperties properties) {
        this.properties = properties;
    }

    public boolean isConfigured() {
        return properties.getHashKey() != null && !properties.getHashKey().isBlank();
    }

    public String hash(long surveyId, String address) {
        if (!isConfigured()) {
            throw new ServiceUnavailableException("Survey submissions are not configured");
        }
        String network = network(address);
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(properties.getHashKey().getBytes(StandardCharsets.UTF_8), ALGORITHM));
            byte[] digest = mac.doFinal((surveyId + "|" + network).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HMAC-SHA256 is always available", e);
        }
    }

    /** IPv4 as it is, IPv6 reduced to its /64 prefix, IPv4-mapped IPv6 treated as the IPv4 address. */
    static String network(String address) {
        String value = address == null ? "" : address.trim();
        int zone = value.indexOf('%');
        if (zone >= 0) {
            value = value.substring(0, zone);
        }
        if (!ClientIpResolver.looksLikeIpLiteral(value)) {
            return "unrecognised";
        }
        try {
            byte[] bytes = InetAddress.getByName(value).getAddress(); // an IP literal: no DNS lookup happens
            if (bytes.length == 4) {
                return (bytes[0] & 0xff) + "." + (bytes[1] & 0xff) + "." + (bytes[2] & 0xff) + "." + (bytes[3] & 0xff);
            }
            return "v6:" + HexFormat.of().formatHex(bytes, 0, 8);
        } catch (UnknownHostException e) {
            return "unrecognised";
        }
    }
}
