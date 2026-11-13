package com.gym.identity.local;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import com.gym.identity.api.SessionFailure;

@Component @Profile("local-dev")
public class LocalCredentialAuthenticator {
 private final BCryptPasswordEncoder encoder=new BCryptPasswordEncoder(12);
 private final String dummy=encoder.encode("dummy-constant-cost-no-account");
 private final byte[] key;
 private final SecureRandom random=new SecureRandom();
 public LocalCredentialAuthenticator(@Value("${gym.iam.hmac-key}") String hex){
  try{key=HexFormat.of().parseHex(hex);}catch(IllegalArgumentException e){throw new IllegalStateException("Invalid private HMAC key");}
  if(key.length<32)throw new IllegalStateException("HMAC key must contain at least 32 random bytes");
 }
 public String login(String value){
  if(value==null||!value.matches("[A-Za-z0-9._-]{3,64}"))throw new SessionFailure(422,"INVALID_INPUT");
  String name=value.toLowerCase(Locale.ROOT);
  if(!name.matches("[a-z0-9._-]{3,64}"))throw new SessionFailure(422,"INVALID_INPUT");return name;
 }
 public void password(String value){int size=value==null?0:value.getBytes(StandardCharsets.UTF_8).length;if(size<12||size>72)throw new SessionFailure(422,"INVALID_INPUT");}
 public String encode(char[] password){String value=new String(password);password(value);return encoder.encode(value);}
 public boolean verify(String proof,String hash){return encoder.matches(proof,hash==null?dummy:hash);}
 public String digest(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
 public String fingerprint(String... parts){try{var m=Mac.getInstance("HmacSHA256");m.init(new SecretKeySpec(key,"HmacSHA256"));for(String p:parts){byte[] b=p.getBytes(StandardCharsets.UTF_8);m.update(java.nio.ByteBuffer.allocate(4).putInt(b.length).array());m.update(b);}return HexFormat.of().formatHex(m.doFinal());}catch(GeneralSecurityException e){throw new IllegalStateException(e);}}
 public String token(){byte[] value=new byte[32];random.nextBytes(value);return Base64.getUrlEncoder().withoutPadding().encodeToString(value);}
}
