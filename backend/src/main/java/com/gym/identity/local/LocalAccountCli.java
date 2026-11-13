package com.gym.identity.local;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import com.gym.GymApplication;

/** Dedicated local-only CLI; password is never an argument or output. */
public final class LocalAccountCli {
 public static void main(String[] args){
  if(args.length<1||args.length>2||(args.length==2&&!"--password-stdin".equals(args[1]))){System.err.println("Usage: LocalAccountCli <login> [--password-stdin]");System.exit(2);}
  int exit=0;char[] password=null;
  try{
   if(args.length==2){
    // Explicit opt-in for redirected/private stdin; never echo the line.
    var reader=new InputStreamReader(System.in,StandardCharsets.UTF_8);var chars=new char[73];int size=0,c;
    while((c=reader.read())!=-1&&c!='\n'){if(c=='\r')continue;if(size==chars.length)throw new IllegalArgumentException();chars[size++]=(char)c;}
    password=Arrays.copyOf(chars,size);Arrays.fill(chars,'\0');
   }else{if(System.console()==null)throw new IllegalStateException();password=System.console().readPassword("Password: ");}
   try(var context=new SpringApplicationBuilder(GymApplication.class).profiles("local-dev").web(WebApplicationType.NONE)
    .properties("spring.main.banner-mode=off","logging.level.root=OFF").run()){
    context.getBean(LocalAccountProvisioner.class).create(args[0],password);System.out.println("Local account created (LOCAL_DEV; no staff/platform grants).");
   }
  }catch(Exception error){System.err.println("Local account creation rejected; verify private configuration, input and uniqueness.");exit=1;}
  finally{if(password!=null)Arrays.fill(password,'\0');}
  System.exit(exit);
 }
}
