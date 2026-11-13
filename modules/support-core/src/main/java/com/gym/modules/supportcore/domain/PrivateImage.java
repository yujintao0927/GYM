package com.gym.modules.supportcore.domain;
import com.gym.modules.supportcore.api.SupportFailure;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.ByteArrayInputStream;
public final class PrivateImage {
 private PrivateImage() {}
 public static void validate(String mediaType,byte[] bytes) {
  if(bytes==null||bytes.length==0||bytes.length>2_000_000||mediaType==null||!java.util.Set.of("image/png","image/jpeg").contains(mediaType))throw new SupportFailure(422,"INVALID_IMAGE");
  try(var input=new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
   var readers=ImageIO.getImageReaders(input);if(!readers.hasNext())throw new SupportFailure(422,"INVALID_IMAGE");
   var reader=readers.next();try {
    reader.setInput(input,true,true);String format=reader.getFormatName();
    if(!(mediaType.equals("image/png")&&format.equalsIgnoreCase("png")||mediaType.equals("image/jpeg")&&format.equalsIgnoreCase("jpeg")))throw new SupportFailure(422,"IMAGE_TYPE_MISMATCH");
    int width=reader.getWidth(0),height=reader.getHeight(0);
    if(width<1||height<1||(long)width*height>4_000_000)throw new SupportFailure(422,"IMAGE_TOO_LARGE");
    if(reader.read(0)==null)throw new SupportFailure(422,"INVALID_IMAGE");
   }finally {reader.dispose();}
  }catch(java.io.IOException|IllegalArgumentException e){throw new SupportFailure(422,"INVALID_IMAGE");}
 }
}
