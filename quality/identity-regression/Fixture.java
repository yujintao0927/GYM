import java.sql.*;
import java.util.*;
public class Fixture {
 public static void main(String[] args) throws Exception {
  String url=System.getenv("GYM_DB_URL");
  if(!"jdbc:mysql://localhost:3306/gym_dev_test_003b".equals(url))throw new IllegalArgumentException("target rejected");
  try(var c=DriverManager.getConnection(url,System.getenv("GYM_DB_USERNAME"),System.getenv("GYM_DB_PASSWORD"))){
   Scanner in=new Scanner(System.in);String op=in.nextLine(), name=in.nextLine();
   if(!name.matches("n2_[a-f0-9]{16}_[0-9]+"))throw new IllegalArgumentException("owner rejected");
   String sql=switch(op){
    case "closed" -> "update iam_account set status='CLOSED' where id in (select account_id from iam_local_credential where login_normalized=?)";
    case "epoch" -> "update iam_account set session_epoch=session_epoch+1 where id in (select account_id from iam_local_credential where login_normalized=?)";
    case "disabled" -> "update iam_local_credential set enabled=false where login_normalized=?";
    case "accessExpired" -> "update iam_session set access_expires_at=0 where account_id in (select account_id from iam_local_credential where login_normalized=?)";
    case "familyExpired" -> "update iam_refresh_family set absolute_expires_at=0 where session_id in (select id from iam_session where account_id in (select account_id from iam_local_credential where login_normalized=?))";
    case "invariants" -> "select count(*) from iam_refresh_node n join iam_refresh_family f on n.family_id=f.id join iam_session s on f.session_id=s.id join iam_local_credential l on s.account_id=l.account_id where l.login_normalized=? and (length(n.digest)<>64 or n.generation<0)";
    default -> throw new IllegalArgumentException("operation rejected");};
   try(var p=c.prepareStatement(sql)){p.setString(1,name);if(op.equals("invariants")){try(var r=p.executeQuery()){r.next();System.out.println(r.getInt(1));}}else System.out.println(p.executeUpdate());}
  }
 }
}
